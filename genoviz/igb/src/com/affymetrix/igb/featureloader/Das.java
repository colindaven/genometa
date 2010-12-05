package com.affymetrix.igb.featureloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.das.DasSource;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.parsers.das.DASFeatureParser;
import com.affymetrix.genometryImpl.parsers.das.DASSymmetry;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.QueryBuilder;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.TrackView;
import java.util.List;
import java.util.Set;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import javax.xml.stream.XMLStreamException;


/**
 * Class to aid in loading features from DAS servers.
 * <p />
 * This class will load features for requested regions, skipping any sub-regions
 * which have already been loaded.
 *
 * @author sgblanch
 */
public final class Das {
	/** Private constructor to prevent instantiation. */
	private Das() { }

	/**
	 * Load annotations from a DAS server.
	 *
	 * @param gFeature the generic feature that is to be loaded from the server.
	 * @param spans List of spans containing the ranges for which you want annotations.
	 * @return true if data was loaded
	 */
	public static boolean loadFeatures(final List<SeqSpan> spans, final GenericFeature gFeature) {
		Application.getSingleton().addNotLockedUpMsg("Loading feature " + gFeature.featureName);

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			public Void doInBackground() {
				BioSeq current_seq = spans.get(0).getBioSeq();
				Set<String> segments = ((DasSource) gFeature.gVersion.versionSourceObj).getEntryPoints();
				String segment = SynonymLookup.getDefaultLookup().findMatchingSynonym(segments, current_seq.getID());

				QueryBuilder builder = new QueryBuilder(gFeature.typeObj.toString());
				builder.add("segment", segment);
				for (SeqSpan span : spans) {
					builder.add("segment", segment + ":" + (span.getMin() + 1) + "," + span.getMax());
				}

				URI uri = builder.build();

				ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(uri.toString(), gFeature.featureName);
				style.setFeature(gFeature);

				// TODO - probably not necessary
				style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(gFeature.featureName, gFeature.featureName);
				style.setFeature(gFeature);

				Collection<DASSymmetry> dassyms = parseData(uri);
				// Special case : When a feature make more than one Track, set feature for each track.
				if(dassyms != null){
					for(DASSymmetry sym : dassyms){
						gFeature.addMethod(sym.getType());
					}
				}
				TrackView.updateDependentData();
				return null;
			}

			@Override
			public void done() {
				try {
					Application.getSingleton().getMapView().setAnnotatedSeq(GenometryModel.getGenometryModel().getSelectedSeq(), true, true);
				} finally {
					Application.getSingleton().removeNotLockedUpMsg("Loading feature " + gFeature.featureName);
				}
			}
		};
		ThreadUtils.getPrimaryExecutor(gFeature).execute(worker);

		return true;
	}

	/**
	 *  Opens a binary data stream from the given uri and adds the resulting
	 *  data.
	 */
	private static Collection<DASSymmetry> parseData(URI uri) {
		Map<String, List<String>> respHeaders = new HashMap<String, List<String>>();
		InputStream stream = null;
		List<String> list;
		String content_type = "content/unknown";
		int content_length = -1;

		try {
			stream = LocalUrlCacher.getInputStream(uri.toURL(), true, null, respHeaders);
			list = respHeaders.get("Content-Type");
			if (list != null && !list.isEmpty()) {
				content_type = list.get(0);
			}

			list = respHeaders.get("Content-Length");
			if (list != null && !list.isEmpty()) {
				try {
					content_length = Integer.parseInt(list.get(0));
				} catch (NumberFormatException ex) {
					content_length = -1;
				}
			}

			if (content_length == 0) { // Note: length == -1 means "length unknown"
				Logger.getLogger(Das.class.getName()).log(Level.WARNING, "{0} returned no data.", uri);
				return null;
			}

			if (content_type.startsWith("text/plain")
					|| content_type.startsWith("text/html")
					|| content_type.startsWith("text/xml")) {
				// Note that some http servers will return "text/html" even when that is untrue.
				// we could try testing whether the filename extension is a recognized extension, like ".psl"
				// and if so passing to LoadFileAction.load(.. feat_request_con.getInputStream() ..)
				AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
				DASFeatureParser das_parser = new DASFeatureParser();

				BufferedInputStream bis = null;
				try {
					bis = new BufferedInputStream(stream);
					return das_parser.parse(bis, group);
				} catch (XMLStreamException ex) {
					Logger.getLogger(Das.class.getName()).log(Level.SEVERE, "Unable to parse DAS response", ex);
				} finally {
					GeneralUtils.safeClose(bis);
				}
			} else {
				Logger.getLogger(Das.class.getName()).log(Level.WARNING, "Declared data type {0} cannot be processed", content_type);
			}
		} catch (Exception ex) {
			Logger.getLogger(Das.class.getName()).log(Level.SEVERE, "Exception encountered: no data returned for url " + uri, ex);
		} finally {
			GeneralUtils.safeClose(stream);
		}

		return null;
	}
}
