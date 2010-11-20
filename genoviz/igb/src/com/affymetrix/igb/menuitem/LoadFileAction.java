/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.menuitem;

import com.affymetrix.igb.util.JFileChooserWithOverwriteWarning;
import net.sf.samtools.SAMFileHeader;
import java.text.DecimalFormat;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.SeqGroupView;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.genometryImpl.util.MenuUtil;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

import net.sf.samtools.SAMFileReader;
import net.sf.samtools.SAMFileWriter;
import net.sf.samtools.SAMFileWriterFactory;
import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader.SortOrder;

import java.net.URI;
import java.text.MessageFormat;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.FishClonesParser;
import com.affymetrix.genometryImpl.parsers.SegmenterRptParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.UniFileFilter;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.symloader.SymLoaderInstNC;
import com.affymetrix.genoviz.util.FileDropHandler;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.featureloader.QuickLoad;
import com.affymetrix.igb.util.MergeOptionChooser;
import com.affymetrix.igb.util.ScriptFileLoader;
import com.affymetrix.igb.util.SAMFileHeaderCorrection;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @version $Id: LoadFileAction.java 6915 2010-09-16 15:05:44Z hiralv $
 */
public final class LoadFileAction extends AbstractAction {

	private final JFrame gviewerFrame;
	private final FileTracker load_dir_tracker;
	public static int unknown_group_count = 1;
	public static final String UNKNOWN_SPECIES_PREFIX = BUNDLE.getString("unknownGenome");
	public static final String UNKNOWN_VERSION_PREFIX = BUNDLE.getString("unknownVersion");
	private static final String SELECT_SPECIES = BUNDLE.getString("speciesCap");
	private static final String MERGE_MESSAGE = 
			"Must select a genome before loading a graph.  "
			+ "Graph data must be merged with already loaded genomic data.";
	private final TransferHandler fdh = new FileDropHandler(){

		@Override
		public void openFileAction(File f) {
			LoadFileAction.openFileAction(gviewerFrame,f);
		}

		@Override
		public void openURLAction(String url) {
			LoadFileAction.openURLAction(gviewerFrame,url);
		}
	};
	private static MergeOptionChooser chooser = null;
	/**
	 *  Constructor.
	 *  @param ft  a FileTracker used to keep track of directory to load from
	 */
	public LoadFileAction(JFrame gviewerFrame, FileTracker ft) {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("openFile")),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Open16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_O);

		this.gviewerFrame = gviewerFrame;
		load_dir_tracker = ft;
		this.gviewerFrame.setTransferHandler(fdh);
	}

	public void actionPerformed(ActionEvent e) {
		loadFile(load_dir_tracker, gviewerFrame);
	}
	

	private static MergeOptionChooser getFileChooser() {
		if (chooser != null) {
			return chooser;
		}

		chooser = new MergeOptionChooser();
		chooser.setMultiSelectionEnabled(true);
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"2bit"},
						".2bit Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"sam", "bam"}, "SAM & BAM Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"bed"}, "BED Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"bps", "bgn", "brs", "bsnp", "brpt", "bnib", "bp1", "bp2", "ead","useq"},
						"Binary Files"));
		chooser.addChoosableFileFilter(new UniFileFilter("cyt", "Cytobands"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"gff", "gtf", "gff3"},
						"GFF Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"fa", "fasta", "fas"},
						"FASTA Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"psl", "psl3", "pslx"},
						"PSL Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"das", "dasxml", "das2xml"},
						"DAS Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"gr", "bgr", "sgr", "bar", "chp", "wig"},
						"Graph Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"sin", "egr", "egr.txt"},
						"Scored Interval Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						"cnt", "Copy Number Files")); // ".cnt" files from CNAT
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"cnchp", "lohchp"}, "Copy Number CHP Files"));

		chooser.addChoosableFileFilter(new UniFileFilter(
						"var", "Genomic Variation Files")); // ".var" files (Toronto DB of genomic variations)
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{SegmenterRptParser.CN_REGION_FILE_EXT, SegmenterRptParser.LOH_REGION_FILE_EXT},
						"Regions Files")); // Genotype Console Segmenter
		chooser.addChoosableFileFilter(new UniFileFilter(
						FishClonesParser.FILE_EXT, "FishClones")); // ".fsh" files (fishClones.txt from UCSC)
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"map"}, "Scored Map Files"));
		chooser.addChoosableFileFilter(new UniFileFilter(
						new String[]{"igb"}, "IGB Script File"));

		Set<String> all_known_endings = new HashSet<String>();
		for (javax.swing.filechooser.FileFilter filter : chooser.getChoosableFileFilters()) {
			if (filter instanceof UniFileFilter) {
				UniFileFilter uff = (UniFileFilter) filter;
				uff.addCompressionEndings(GeneralUtils.compression_endings);
				all_known_endings.addAll(uff.getExtensions());
			}
		}
		UniFileFilter all_known_types = new UniFileFilter(
						all_known_endings.toArray(new String[all_known_endings.size()]),
						"Known Types");
		all_known_types.setExtensionListInDescription(false);
		all_known_types.addCompressionEndings(GeneralUtils.compression_endings);
		chooser.addChoosableFileFilter(all_known_types);
		chooser.setFileFilter(all_known_types);
		return chooser;
	}

	/** Load a file into the global singleton genometry model. */
	private static void loadFile(final FileTracker load_dir_tracker, final JFrame gviewerFrame) {

		GenometryModel gmodel = GenometryModel.getGenometryModel();
		final MergeOptionChooser fileChooser = getFileChooser();
		File currDir = load_dir_tracker.getFile();
		if (currDir == null) {
			currDir = new File(System.getProperty("user.home"));
		}
		fileChooser.setCurrentDirectory(currDir);
		fileChooser.rescanCurrentDirectory();

		int option = fileChooser.showOpenDialog(gviewerFrame);

		if (option != JFileChooser.APPROVE_OPTION) {
			return;
		}
		
		load_dir_tracker.setFile(fileChooser.getCurrentDirectory());

		final File[] fils = fileChooser.getSelectedFiles();
		
		final AnnotatedSeqGroup loadGroup = gmodel.addSeqGroup((String)fileChooser.versionCB.getSelectedItem());

		final boolean mergeSelected = loadGroup == gmodel.getSelectedSeqGroup();

		for(final File file : fils){
			URI uri = file.toURI();

			if (uri.toString().toLowerCase().endsWith(".sam")) {
				openSamFile(file, mergeSelected, loadGroup, (String)fileChooser.speciesCB.getSelectedItem(), gviewerFrame);
			} else if(uri.toString().toLowerCase().endsWith(".bam")) {
				openBamFile(file, mergeSelected, loadGroup, (String)fileChooser.speciesCB.getSelectedItem(), gviewerFrame);
			} else {
				openURI(uri, file.getName(), mergeSelected, loadGroup, (String)fileChooser.speciesCB.getSelectedItem());
			}
		}
	}

	/**
	 * Check first if the BAM-File is sorted (coordinate sort-order is needed). If so, open the BAM-File regular. If not, sort BAM-File.
	 *
	 * @param bamFile
	 * @param mergeSelected
	 * @param loadGroup
	 * @param speciesName
	 * @param gviewerFrame
	 */
	private static void openBamFile(File bamFile, final boolean mergeSelected, final AnnotatedSeqGroup loadGroup, final String speciesName, final JFrame gviewerFrame) {
		// While parsing: emit warnings but keep going if possible.
		SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
		final SAMFileReader reader = new SAMFileReader(bamFile);

		System.out.println(reader.getFileHeader().getSortOrder());

		if(reader.getFileHeader().getSortOrder() == SAMFileHeader.SortOrder.coordinate) {
			openURI(bamFile.toURI(), bamFile.getName(), mergeSelected, loadGroup, speciesName);
		} else {
			// bamFile is not coordinate sorted, lets do it now (if the user confirms...)
			//boolean userConfirmed = Application.confirmPanel("The BAM-File " + bamFile.getName() + " is not sorted on coordinates. Do you want to sort it now?");


			Object[] options = {"Sort File", "Open without sorting", "Cancel"};
			int dialogResult = JOptionPane.showOptionDialog(gviewerFrame, "The BAM-File seems to be not sorted by coordinates!\n"
					+ "If you know that the File is sorted by coordinates, you can try to open it without sorting.",
				"File not sorted", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

			switch(dialogResult) {
				case 0:
					JFileChooser fc = new JFileChooserWithOverwriteWarning();
					
					fc.setSelectedFile(bamFile);
					int fcResult = fc.showSaveDialog(gviewerFrame);
					if(fcResult == JFileChooser.APPROVE_OPTION) {
						final File newBamFile = fc.getSelectedFile();
						final String notLockedUpMsg = "Sorting " + bamFile.getName() + " to " + newBamFile.getName();

						SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
							@Override
							public Void doInBackground() {
								Application.getSingleton().addNotLockedUpMsg(notLockedUpMsg);

								reader.getFileHeader().setSortOrder(SortOrder.coordinate);
								SAMFileWriter writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(reader.getFileHeader(), false, newBamFile);

								Iterator<SAMRecord> iterator = reader.iterator();
								int i = 0;
								while (iterator.hasNext()) {
									writer.addAlignment(iterator.next());
									i++;
									if((i % 500000) == 0) {	// show progress in statusbar
										Application.getSingleton().setStatus(notLockedUpMsg + ": " + new DecimalFormat( ",###" ).format(i) + " Reads", false);
									}
								}

								// todo: print "writing to disc" to user
								reader.close();
								writer.close();

								return null;
							}

							@Override
							public void done() {
								Application.getSingleton().removeNotLockedUpMsg(notLockedUpMsg);
								openURI(newBamFile.toURI(), newBamFile.getName(), mergeSelected, loadGroup, speciesName);
							}
						};
						ThreadUtils.getPrimaryExecutor(new Object()).execute(worker);
					}
					break;
				case 1:
					// try to open the bam file, maybe just the header-tag is not set to coordinate
					openURI(bamFile.toURI(), bamFile.getName(), mergeSelected, loadGroup, speciesName);
					break;
			}
		}
	}

	/**
	 * 
	 * @param samFile
	 * @param mergeSelected
	 * @param loadGroup
	 * @param speciesName
	 * @param gviewerFrame
	 */
	private static void openSamFile(final File samFile, final boolean mergeSelected, final AnnotatedSeqGroup loadGroup, final String speciesName, final JFrame gviewerFrame) {
		// sort and transform sam-file to bam-file
		File bamFile = changeFileExtension(samFile, ".bam");
		boolean skipTransform = false;

		if(bamFile.exists()) {
			// if a bam file with same name as the sam file already exists, show confirmation dialog
			Object[] options = {"Open existing file", "Overwrite existing file", "Change name"};
			int n = JOptionPane.showOptionDialog(gviewerFrame, "A BAM-File with the same name of the SAM-File already exists!",
				"Existing BAM-File", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE, null, options, options[0]);

			switch(n) {
				case 0:			// open existing bam file without transforming
					skipTransform = true;
					break;
				case 1:	break;	// overwrite bam-file
				case 2:			// open filechooser for new filename
					JFileChooser fc = new JFileChooserWithOverwriteWarning();

					fc.setSelectedFile(bamFile);
					int fcResult = fc.showSaveDialog(gviewerFrame);
					if(fcResult == JFileChooser.APPROVE_OPTION) {
						bamFile = fc.getSelectedFile();
					} else {
						// abord opening
						JOptionPane.showMessageDialog(gviewerFrame, "Open the file has been aborted!", "Info", JOptionPane.INFORMATION_MESSAGE);
						return;
					}
					break;
				default:
					// abord opening
					JOptionPane.showMessageDialog(gviewerFrame, "Open the file has been aborted!", "Info", JOptionPane.INFORMATION_MESSAGE);
					return;
			}
		}
		
		if(skipTransform) {
			// open the existing bam file without doing anything
			openURI(bamFile.toURI(), bamFile.getName(), mergeSelected, loadGroup, speciesName);
		}else {
			final File inputFile = samFile;		// threading needs final variable
			final File outputFile = bamFile;	// threading needs final variable
			final String notLockedUpMsg = "Transforming " + inputFile.getName() + " to " + outputFile.getName();

			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				@Override
				public Void doInBackground() {
					Application.getSingleton().addNotLockedUpMsg(notLockedUpMsg);
					// While parsing: emit warnings but keep going if possible.
					SAMFileReader.setDefaultValidationStringency(SAMFileReader.ValidationStringency.LENIENT);
					SAMFileReader reader = new SAMFileReader(inputFile);

					SAMFileWriter writer;
					if(reader.getFileHeader().getSortOrder() == SortOrder.coordinate) {	// SAM is already sorted!
						writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(reader.getFileHeader(), true, outputFile);
					} else {
						reader.getFileHeader().setSortOrder(SortOrder.coordinate);
						writer = new SAMFileWriterFactory().makeSAMOrBAMWriter(reader.getFileHeader(), false, outputFile);
					}

					Iterator<SAMRecord> iterator = reader.iterator();
					int i = 0;
					while (iterator.hasNext()) {
						writer.addAlignment(iterator.next());
						i++;
						if((i % 500000) == 0) {	// show progress in statusbar
							Application.getSingleton().setStatus(notLockedUpMsg + ": " + new DecimalFormat( ",###" ).format(i) + " Reads", false);
						}
					}

					// todo: print "writing to disc" to user
					reader.close();
					writer.close();

					return null;
				}

				@Override
				public void done() {
					Application.getSingleton().removeNotLockedUpMsg(notLockedUpMsg);
					openURI(outputFile.toURI(), outputFile.getName(), mergeSelected, loadGroup, speciesName);
				}
			};
			ThreadUtils.getPrimaryExecutor(new Object()).execute(worker);
		}
	}

	/**
	 * Creates a new file and changes the extension to the given Param.
	 *
	 * @param inputFile File to create new one from
	 * @param newExtension Extension for the new file
	 * @return New file with changed extension
	 */
	private static File changeFileExtension(File inputFile, String newExtension) {
		String fileString = inputFile.getAbsolutePath();
		String newUriString = fileString.substring(0, fileString.length() - 4);
		if(newExtension.startsWith(".")) {
			newUriString = newUriString + newExtension;
		} else {
			newUriString = newUriString + "." + newExtension;
		}
		return new File(newUriString);
	}

	public static void openURI(URI uri, String fileName){
		AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		openURI(uri, fileName, true, group, group.getOrganism());
	}
	
	public static void openURI(URI uri, final String fileName, final boolean mergeSelected, final AnnotatedSeqGroup loadGroup, String speciesName) {
		if (uri.toString().toLowerCase().endsWith(".igb")) {
			// response file.  Do its actions and return.
			// Potential for an infinite loop here, of course.
			ScriptFileLoader.doActions(uri.toString());
			return;
		}

		GenericFeature gFeature = getFeature(uri, fileName, speciesName, loadGroup);

		if(gFeature == null)
			return;
		
		if (((QuickLoad)gFeature.symL).getSymLoader() instanceof SymLoaderInstNC) {
			loadAllFeatures(gFeature, loadGroup);
		} else if (gFeature.symL != null){
			addChromosomesForUnknownGroup(fileName, gFeature, loadGroup);
		}

		// force a refresh of this server
		ServerList.fireServerInitEvent(ServerList.getLocalFilesServer(), ServerStatus.Initialized, true, true);

		//Annotated Seq Group must be selected before feature table change call.
		GenometryModel.getGenometryModel().setSelectedSeqGroup(loadGroup);

		GeneralLoadView.getLoadView().createFeaturesTable();

		if(!mergeSelected){
			unknown_group_count++;
		}
	}

	public static GenericFeature getFeature(URI uri, String fileName, String speciesName, AnnotatedSeqGroup loadGroup){
		// Make sure this URI is not already used within the selectedGroup.  Otherwise there could be collisions in BioSeq.addAnnotations(type)
		boolean uniqueURI = true;
		for (GenericVersion version : loadGroup.getAllVersions()) {
			// See if symloader feature was created with the same uri.
			for (GenericFeature feature : version.getFeatures()) {
				if (feature.symL != null) {
					if (feature.symL.uri.equals(uri)) {
						uniqueURI = false;
						break;
					}
				}
			}
		}
		if (!uniqueURI) {
			ErrorHandler.errorPanel("Cannot add same feature",
					"The feature " + uri + " has already been added.");
			return null;
		}

		GenericVersion version = GeneralLoadUtils.getLocalFilesVersion(loadGroup, speciesName);

		// handle URL case.
		String uriString = uri.toString();
		int httpIndex = uriString.toLowerCase().indexOf("http:");
		if (httpIndex > -1) {
			// Strip off initial characters up to and including http:
			// Sometimes this is necessary, as URLs can start with invalid "http:/"
			uriString = GeneralUtils.convertStreamNameToValidURLName(uriString);
			uri = URI.create(uriString);
		}
		boolean autoload = PreferenceUtils.getBooleanParam(PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load);
		GenericFeature gFeature = new GenericFeature(fileName, null, version, new QuickLoad(version, uri), File.class, autoload);
		
		version.addFeature(gFeature);
		gFeature.setVisible(); // this should be automatically checked in the feature tree

		return gFeature;
	}

	private static void loadAllFeatures(final GenericFeature gFeature, final AnnotatedSeqGroup loadGroup){
		final String notLockedUpMsg = "Loading whole genome for " + gFeature.featureName;
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			@Override
			public Void doInBackground() {
				Application.getSingleton().addNotLockedUpMsg(notLockedUpMsg);

				gFeature.loadStrategy = LoadStrategy.GENOME;
				GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);

				return null;
			}

			@Override
			public void done() {
				Application.getSingleton().removeNotLockedUpMsg(notLockedUpMsg);
			}
		};

		ThreadUtils.getPrimaryExecutor(gFeature).execute(worker);
	}

	private static void addChromosomesForUnknownGroup(final String fileName, final GenericFeature gFeature, final AnnotatedSeqGroup loadGroup) {
		final String notLockedUpMsg = "Retrieving chromosomes for " + fileName;
		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			@Override
			public Void doInBackground() {
				Application.getSingleton().addNotLockedUpMsg(notLockedUpMsg);
				// Here we are reading the whole file in.  We have no choice, since the chromosomes in this file are unknown.
				for (BioSeq seq : gFeature.symL.getChromosomeList()) {
					loadGroup.addSeq(seq);
				}
				return null;
			}

			@Override
			public void done() {
				SeqGroupView.refreshTable();
				if (loadGroup.getSeqCount() > 0 && GenometryModel.getGenometryModel().getSelectedSeq() == null) {
					// select a chromosomes
					GenometryModel.getGenometryModel().setSelectedSeq(loadGroup.getSeq(0));
				}
				Application.getSingleton().removeNotLockedUpMsg(notLockedUpMsg);
			}
		};
		ThreadUtils.getPrimaryExecutor(gFeature).execute(worker);
	}


	private static void openURLAction(JFrame gviewerFrame,String url){
		try {
			URI uri = new URI(url.trim());
		
			if(!openURI(uri)){
				ErrorHandler.errorPanel(gviewerFrame, "FORMAT NOT RECOGNIZED", "Format not recognized for file: " + url, null);
			}
			
		} catch (URISyntaxException ex) {
			ex.printStackTrace();
			ErrorHandler.errorPanel(gviewerFrame, "INVALID URL", url + "\n Url provided is not valid: ", null);
		}
	}

	private static void openFileAction(JFrame gviewerFrame, File f){
		URI uri = f.toURI();
		if(!openURI(uri)){
			ErrorHandler.errorPanel(gviewerFrame, "FORMAT NOT RECOGNIZED", "Format not recognized for file: " + f.getName(), null);			
		}
	}

	private static boolean openURI(URI uri) {
		String unzippedName = GeneralUtils.getUnzippedName(uri.toString());
		String friendlyName = unzippedName.substring(unzippedName.lastIndexOf("/") + 1);

		if(!getFileChooser().accept(new File(friendlyName))){
			return false;
		}

		GenometryModel gmodel = GenometryModel.getGenometryModel();
		AnnotatedSeqGroup loadGroup = gmodel.getSelectedSeqGroup();
		boolean mergeSelected = loadGroup == null ? false :true;
		if (loadGroup == null) {
			loadGroup = gmodel.addSeqGroup(UNKNOWN_VERSION_PREFIX + " " + unknown_group_count);
		}

		String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();
		if(SELECT_SPECIES.equals(speciesName)){
			speciesName = UNKNOWN_SPECIES_PREFIX + " " + unknown_group_count;
		}
		openURI(uri, friendlyName, mergeSelected, loadGroup, speciesName);
		
		return true;
	}
}
