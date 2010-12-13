package com.affymetrix.igb.featureloader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.parsers.ChpParser;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;

/**
 *
 * @author jnicol
 * @version $Id$
 */
public final class QuickLoad extends SymLoader {
	private final GenericVersion version;
	private SymLoader symL;	// parser factory

	public QuickLoad(GenericVersion version, String featureName, String organism_dir) {
		super(determineURI(version, featureName, organism_dir), featureName, null);
		this.version = version;
		this.symL = ServerUtils.determineLoader(extension, uri, featureName, version.group);
		this.isResidueLoader = (this.symL != null && this.symL.isResidueLoader);
	}

	public QuickLoad(GenericVersion version, URI uri) {
		super(uri, detemineFriendlyName(uri), null);
		this.version = version;
		this.symL = ServerUtils.determineLoader(extension, uri, featureName, version.group);
		this.isResidueLoader = (this.symL != null && this.symL.isResidueLoader);
	}

	@Override
	protected void init() {
		this.isInitialized = true;
	}

	/**
	 * Return possible strategies to load this URI.
	 * @return
	 */
	@Override
	public List<LoadStrategy> getLoadChoices() {
		// If we're using a symloader, return its load choices.
		if (this.symL != null) {
			return this.symL.getLoadChoices();
		}
		if (extension.endsWith(".bar") || extension.endsWith(".useq") || extension.endsWith(".bgr")
				||extension.endsWith(".chp")
				|| (extension.endsWith(".sin") || extension.endsWith(".egr") || extension.endsWith(".txt") || extension.endsWith("link.psl"))
				|| (extension.endsWith(".gff") || extension.endsWith(".gff3"))) {
			List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
			strategyList.add(LoadStrategy.NO_LOAD);
			strategyList.add(LoadStrategy.GENOME);
			return strategyList;
		}
		return super.getLoadChoices();
	}

	private static String detemineFriendlyName(URI uri){
		String unzippedName = GeneralUtils.getUnzippedName(uri.toString());
		String strippedName = unzippedName.substring(unzippedName.lastIndexOf("/") + 1);
		String friendlyName = strippedName.substring(0, strippedName.indexOf('.'));
		return friendlyName;
	}

	public static URI determineURI(GenericVersion version, String featureName, String organism_dir) {
		URI uri = null;

		if (version.gServer.URL == null || version.gServer.URL.length() == 0) {
			int httpIndex = featureName.toLowerCase().indexOf("http:");
			if (httpIndex > -1) {
				// Strip off initial characters up to and including http:
				// Sometimes this is necessary, as URLs can start with invalid "http:/"
				featureName = GeneralUtils.convertStreamNameToValidURLName(featureName);
				uri = URI.create(featureName);
			} else {
				uri = (new File(featureName)).toURI();
			}
		} else {
			uri = URI.create(
					version.gServer.URL
					+ ((organism_dir != null && !organism_dir.isEmpty()) ? organism_dir + "/"  : "")
					+ version.versionID + "/"
					+ determineFileName(version, featureName));
		}
		return uri;
	}

	private static String determineFileName(GenericVersion version, String featureName) {
		URL quickloadURL = null;
		try {
			quickloadURL = new URL((String) version.gServer.serverObj);
		} catch (MalformedURLException ex) {
			ex.printStackTrace();
			return "";
		}

		QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
		List<AnnotMapElt> annotsList = quickloadServer.getAnnotsMap(version.versionID);

		// Linear search, but over a very small list.
		for (AnnotMapElt annotMapElt : annotsList) {
			if (annotMapElt.title.equals(featureName)) {
				return annotMapElt.fileName;
			}
		}
		return "";
	}


	public boolean loadFeatures(final SeqSpan overlapSpan, final GenericFeature feature)
			throws OutOfMemoryError {

		Application.getSingleton().addNotLockedUpMsg("Loading feature " + feature.featureName + " on sequence " + overlapSpan.getBioSeq().getID());
		final SeqMapView gviewer = Application.getSingleton().getMapView();
		if (this.symL != null && this.symL.isResidueLoader) {
			return loadResiduesThread(feature, overlapSpan, gviewer);
		}
		return loadSymmetriesThread(feature, overlapSpan, gviewer);
	}

	private boolean loadSymmetriesThread(
			final GenericFeature feature, final SeqSpan overlapSpan, final SeqMapView gviewer)
			throws OutOfMemoryError {

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			public Void doInBackground() {
				try {
					if (QuickLoad.this.extension.endsWith(".chp")) {
						// special-case chp files, due to their LazyChpSym DAS/2 loading
						QuickLoad.this.getGenome();
						gviewer.setAnnotatedSeq(overlapSpan.getBioSeq(), true, true);
						return null;
					}

					loadAndAddSymmetries(feature, overlapSpan, gviewer);
					TrackView.updateDependentData();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}

			@Override
			public void done() {
				try {
					BioSeq aseq = GenometryModel.getGenometryModel().getSelectedSeq();
					if (overlapSpan != null && aseq != null) {
						gviewer.setAnnotatedSeq(aseq, true, true);
					} else if (GenometryModel.getGenometryModel().getSelectedSeq() == null && QuickLoad.this.version.group != null) {
						// This can happen when loading a brand-new genome
						GenometryModel.getGenometryModel().setSelectedSeq(QuickLoad.this.version.group.getSeq(0));
					}

					SeqGroupView.refreshTable();
					//BFTAG added : fix view-height after re-adjusting max expand
					Application.getSingleton().getMapView().setAnnotatedSeq(Application.getSingleton().getMapView().getAnnotatedSeq(), true, true, true);
				} catch (Exception ex) {
					Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
					Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature.featureName + " on sequence " + overlapSpan.getBioSeq().getID());
				}
			}
		};
		ThreadUtils.getPrimaryExecutor(feature).execute(worker);
		return true;
	}


	/**
	 * For unoptimized file formats load symmetries and add them.
	 * @param feature
	 * @return
	 */
	public boolean loadAllSymmetriesThread(final GenericFeature feature){
		Application.getSingleton().addNotLockedUpMsg("Loading feature " + feature.featureName);
		final SeqMapView gviewer = Application.getSingleton().getMapView();

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			public Void doInBackground() {
				try {
					loadAndAddAllSymmetries(feature);
					TrackView.updateDependentData();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}

			@Override
			public void done() {
				try {
					BioSeq aseq = GenometryModel.getGenometryModel().getSelectedSeq();
					if (aseq != null) {
						gviewer.setAnnotatedSeq(aseq, true, true);
					} else if (GenometryModel.getGenometryModel().getSelectedSeq() == null && QuickLoad.this.version.group != null) {
						// This can happen when loading a brand-new genome
						GenometryModel.getGenometryModel().setSelectedSeq(QuickLoad.this.version.group.getSeq(0));
					}

					SeqGroupView.refreshTable();
				} catch (Exception ex) {
					Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
					Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature.featureName);
				}
			}
		};
		ThreadUtils.getPrimaryExecutor(feature).execute(worker);

		return true;
	}

	/**
	 * For optimized file format load symmetries and add them.
	 * @param feature
	 * @param span
	 * @throws IOException
	 * @throws OutOfMemoryError
	 */
	private void loadAndAddSymmetries(GenericFeature feature, final SeqSpan span, final SeqMapView gviewer)
			throws IOException, OutOfMemoryError {

		if (this.symL != null && !this.symL.getChromosomeList().contains(span.getBioSeq())) {
			return;
		}

		List<? extends SeqSymmetry> results;
		boolean setStyle = false;

		// short-circuit if there's a failure... which may not even be signaled in the code
		if (!this.isInitialized) {
			this.init();
		}

		results = this.getRegion(span);
		if (results != null) {
			setStyle = addSymmtries(span, results, feature);
		}
		feature.addLoadedSpanRequest(span); // this span is now considered loaded.

		if(setStyle){
			setStyle(feature);
		}

		/*Zoom in, because of memory overhead not the complet sequence is loaded.
		  The zoom is not 100% correct, its zoom just nearly into the loaded part*/
		if (results.size() < span.getEnd()) {
			gviewer.zoomTo(0, (results.size() / 2));
		}
	}

	private boolean loadAndAddAllSymmetries(final GenericFeature feature)
			throws OutOfMemoryError {

		// short-circuit if there's a failure... which may not even be signaled in the code
		if (!this.isInitialized) {
			this.init();
		}

		List<? extends SeqSymmetry> results = this.getGenome();
		Map<BioSeq, List<SeqSymmetry>> seq_syms = SymLoader.splitResultsBySeqs(results);
		SeqSpan span = null;
		BioSeq seq = null;

		for (Entry<BioSeq, List<SeqSymmetry>> seq_sym : seq_syms.entrySet()) {
			seq = seq_sym.getKey();
			span = new SimpleSeqSpan(seq.getMin(), seq.getMax() - 1, seq);
			addSymmtries(span, seq_sym.getValue(), feature);
			feature.addLoadedSpanRequest(span); // this span is now considered loaded.
		}

		setStyle(feature);

		return true;
	}

	
	private void setStyle(GenericFeature feature) {
		// TODO - not necessarily unique, since the same file can be loaded to multiple tracks for different organisms
		ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(this.uri.toString(), featureName, feature.featureProps);
		style.setFeature(feature);

		// TODO - probably not necessary
		style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(featureName, featureName, feature.featureProps);
		style.setFeature(feature);
		
	}


	private static boolean addSymmtries(final SeqSpan span, List<? extends SeqSymmetry> results, GenericFeature feature) {
		boolean setStyle = false;
		results = ServerUtils.filterForOverlappingSymmetries(span, results);
		for (Map.Entry<String, List<SeqSymmetry>> entry : SymLoader.splitResultsByTracks(results).entrySet()) {
			if (entry.getValue().isEmpty()) {
				continue;
			}
			SymLoader.filterAndAddAnnotations(entry.getValue(), span, feature.getURI(), feature);
			setStyle = true;
			// Some format do not annotate. So it might not have method name. e.g bgn
			if (entry.getKey() != null) {
				feature.addMethod(entry.getKey());
			}
		}
		return setStyle;
	}
	
	private boolean loadResiduesThread(final GenericFeature feature, final SeqSpan span, final SeqMapView gviewer) {
		SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {

			public String doInBackground() {
				try {
					String results = QuickLoad.this.getRegionResidues(span);
					if (results != null && !results.isEmpty()) {
						// TODO: make this more general.  Since we can't currently optimize all residue requests,
						// we are simply considering the span loaded if it loads the entire chromosome
						if (span.getMin() <= span.getBioSeq().getMin() && span.getMax() >= span.getBioSeq().getMax()) {
							feature.addLoadedSpanRequest(span);	// this span is now considered loaded.
						}
					}
					return results;
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}
			@Override
			public void done() {
				try {
					final String results = get();
					if (results != null && !results.isEmpty()) {
						BioSeq.addResiduesToComposition(span.getBioSeq(), results, span);
						gviewer.setAnnotatedSeq(span.getBioSeq(), true, true);
					}
				} catch (Exception ex) {
					Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
				} finally {
					Application.getSingleton().removeNotLockedUpMsg("Loading feature " + feature.featureName);
				}
			}
		};

		ThreadUtils.getPrimaryExecutor(this.version.gServer).execute(worker);
		return true;
	}

	/**
	 * Get list of chromosomes used in the file/uri.
	 * Especially useful when loading a file into an "unknown" genome
	 * @return List of chromosomes
	 */
	@Override
	public List<BioSeq> getChromosomeList() {
		if (this.symL != null) {
			return this.symL.getChromosomeList();
		}
		return super.getChromosomeList();
	}


	/**
	 * Only used for non-symloader files.
	 * @return
	 */
	@Override
	public List<? extends SeqSymmetry> getGenome() {
		try {
			if (this.extension.endsWith(".chp")) {
				// special-case CHP files. ChpParser only has
				//    a parse() method that takes the file name
				// (ChpParser uses Affymetrix Fusion SDK for actual file parsing)
				File f = LocalUrlCacher.convertURIToFile(this.uri);
				return ChpParser.parse(f.getAbsolutePath(), true);
			}
			BufferedInputStream bis = null;
			try {
				// This will also unzip the stream if necessary
				bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(this.uri);
				return SymLoader.parse(this.extension, this.uri, bis, this.version.group, this.featureName, null);
			} catch (FileNotFoundException ex) {
				Logger.getLogger(QuickLoad.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(bis);
			}
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan span) {
		if (this.symL != null) {
			return this.symL.getRegion(span);
		}
		return super.getRegion(span);
	}

	@Override
	public String getRegionResidues(SeqSpan span) {
		if (this.symL != null && this.symL.isResidueLoader) {
			return this.symL.getRegionResidues(span);
		}
		Logger.getLogger(QuickLoad.class.getName()).log(
				Level.SEVERE, "Residue loading was called with a non-residue format.");
		return "";
	}

	public SymLoader getSymLoader(){
		return symL;
	}
}
