package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.ParserController;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.ExportSlicedViewAction;
import com.affymetrix.igb.action.RefreshDataAction;
import com.affymetrix.igb.bookmarks.UnibrowControlServlet;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.view.MapRangeBox;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.Component;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import org.freehep.util.export.ExportFileType;

/**
 *
 * @author jnicol
 * Parse actions from IGB response file.
 */
public class ScriptFileLoader {
	private static String splitter = "\\s";

	private static enum ExportMode {
		MAIN ("mainView"),
		MAINWITHLABELS ("mainViewWithLabels"),
		SLICEDWITHLABELS ("slicedViewWithLabels"),
		WHOLEFRAME ("wholeFrame");

		private String name;

		ExportMode(String name) {
			this.name = name;
		}

		@Override
		public String toString() {
			return name;
		}
	};


	public static String getScriptFileStr(String[] args) {
		if (args == null) {
			return null;
		}
		for (int i=0;i<args.length;i++) {
			if (args[i].equalsIgnoreCase("-" + IGBConstants.SCRIPTFILETAG)) {
				if (i+1 < args.length) {
					return args[i+1];
				} else {
					Logger.getLogger(ScriptFileLoader.class.getName()).severe("File was not specified.");
					return null;
				}
			}
		}
		return null;
	}

	/**
	 * Done in a thread to avoid GUI lockup.
	 * @param batchFileStr
	 */
	public static void doActions(final String batchFileStr) {
		Executor vexec = Executors.newSingleThreadExecutor();
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				protected Void doInBackground() throws Exception {
					if (batchFileStr == null || batchFileStr.length() == 0) {
						Logger.getLogger(ScriptFileLoader.class.getName()).log(
								Level.SEVERE, "Couldn''t find response file: {0}", batchFileStr);
						return null;
					}
					// A response file was requested.  Run response file parser, and ignore any other parameters.
					File f = new File(batchFileStr);
					if (!f.exists()) {
						URI uri = URI.create(batchFileStr);
						if (uri == null) {
							Logger.getLogger(ScriptFileLoader.class.getName()).log(
									Level.SEVERE, "Not a valid script file: {0}", batchFileStr);
							return null;
						}
						f = LocalUrlCacher.convertURIToFile(uri);
					}
					if (f == null || !f.exists()) {
						Logger.getLogger(ScriptFileLoader.class.getName()).log(
								Level.SEVERE, "Couldn''t find response file: {0}", batchFileStr);
						return null;
					}

					ScriptFileLoader.doActions(f);
					return null;
				}
			};

			vexec.execute(worker);
	}

	/**
	 * Read and execute the actions from a file.
	 * @param bis
	 */
	private static void doActions(File f) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(f));
			ScriptFileLoader.doActions(br);
		} catch (FileNotFoundException ex) {
			Logger.getLogger(ScriptFileLoader.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(br);
		}
	}


	/**
	 * Read and execute the actions from the stream.
	 * @param bis
	 */
	private static void doActions(BufferedReader br) {
		try {
			String line = null;
			while ((line = br.readLine()) != null) {
				//Ignore comments.
				if(line.startsWith("#"))
					continue;

				try {
					IGB.getSingleton().addNotLockedUpMsg("Executing script line: " + line);
					Logger.getLogger(ScriptFileLoader.class.getName()).log(
							Level.INFO, "line: {0}", line);
					doSingleAction(line);
					Thread.sleep(1000);	// user actions don't happen instantaneously, so give a short sleep time between batch actions.
				} finally {
					IGB.getSingleton().removeNotLockedUpMsg("Executing script line: " + line);
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(ScriptFileLoader.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void doSingleAction(String line) {
		String[] fields = line.split(splitter);
		String action = fields[0].toLowerCase();
		if (action.equals("genome") && fields.length >= 2) {
			// go to genome
			goToGenome(join(fields,1));
			return;
		}
		if (action.equals("goto") && fields.length >= 2) {
			// go to region
			goToRegion(join(fields,1));
			return;
		}
		if (action.equals("load")) {
			// Allowing multiple files to be specified, split by commas
			String[] loadFiles = join(fields,1).split(",");
			for (int i=0;i<loadFiles.length;i++) {
				if (i > 0) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException ex) {
						Logger.getLogger(ScriptFileLoader.class.getName()).log(Level.SEVERE, null, ex);
					}
				}
				loadFile(loadFiles[i]);
			}
			return;
		}
		if (action.equals("loadfromserver")) {
			if (fields.length >= 2) {
				loadData(fields[1], join(fields,2));
				return;
			}
		}
		if (action.equals("loadmode")) {
			if (fields.length >=2) {
				loadMode(fields[1], join(fields,2));
			}
		}
		if (action.equals("print")) {
			if (fields.length == 1) {
				try {
					Application.getSingleton().getMapView().getSeqMap().print(0, true);
				} catch (Exception ex) {
					ErrorHandler.errorPanel("Problem trying to print.", ex);
				}
				return;
			}
		}
		if (action.equals("refresh")) {
			RefreshDataAction.getAction().actionPerformed(null);
		}
		if (action.equals("select") && fields.length>=2) {
			UnibrowControlServlet.performSelection(join(fields,1));
		}
		if (action.equals("sleep") && fields.length == 2) {
			try {
				int sleepTime = Integer.parseInt(fields[1]);
				Thread.sleep(sleepTime);
			} catch (Exception ex) {
				Logger.getLogger(ScriptFileLoader.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		if (action.startsWith("snapshot")) {
			// determine the export mode
			action = action.substring(8,action.length());
			ExportMode exportMode = ExportMode.WHOLEFRAME;
			if (action.length() == 0 || action.equalsIgnoreCase(ExportMode.WHOLEFRAME.toString())) {
				exportMode = ExportMode.WHOLEFRAME;
			} else if (action.equalsIgnoreCase(ExportMode.MAIN.toString())) {
				exportMode = ExportMode.MAIN;
			} else if (action.equalsIgnoreCase(ExportMode.MAINWITHLABELS.toString())) {
				exportMode = ExportMode.MAINWITHLABELS;
			} else if (action.equalsIgnoreCase(ExportMode.SLICEDWITHLABELS.toString())) {
				exportMode = ExportMode.SLICEDWITHLABELS;
			}

			// determine the file name, and export.
			if (fields.length >= 1) {
				snapShot(exportMode,new File(join(fields,1)));	// second field and possibly others are a single filename
			} else {
				// base filename upon organism and timestamp
				String id = GenometryModel.getGenometryModel().getSelectedSeqGroup() == null ? "default" :
					GenometryModel.getGenometryModel().getSelectedSeqGroup().getID();
				snapShot(exportMode,new File(id + System.currentTimeMillis() + ".png"));
			}
		}
	}

	/**
	 * Take a snapshot, i.e., export to a file.
	 * @param f
	 */
	private static void snapShot(ExportMode exportMode, File f) {
		Logger.getLogger(ScriptFileLoader.class.getName()).log(
				Level.INFO, "Exporting file {0} in mode: {1}", new Object[]{f.getName(), exportMode.toString()});
		String extension = ParserController.getExtension(f.getName().toLowerCase());
		if (extension.length() == 0) {
			Logger.getLogger(ScriptFileLoader.class.getName()).log(
					Level.SEVERE, "no file extension given for file", f.getName());
			return;
		}
		extension = extension.substring(1, extension.length());
		List efts = ExportFileType.getExportFileTypes(extension);
		if (efts.isEmpty()) {
			Logger.getLogger(ScriptFileLoader.class.getName()).log(
					Level.SEVERE, "image file extension {0} is not supported", extension);
			return;
		}

		try {
			ExportFileType eft = (ExportFileType) efts.get(0);
			Component c = null;
			switch(exportMode) {
				case WHOLEFRAME:
					c = IGB.getSingleton().getFrame();
					break;
				case MAIN:
					c = IGB.getSingleton().getMapView().getSeqMap().getNeoCanvas();
					break;
				case MAINWITHLABELS:
					c = IGB.getSingleton().getMapView().getSeqMap();
					break;
				case SLICEDWITHLABELS:
					c = ExportSlicedViewAction.determineSlicedComponent();
					break;
			}
			if (!ComponentWriter.exportComponent(f, c, eft)) {
				Logger.getLogger(ScriptFileLoader.class.getName()).log(
						Level.SEVERE, "Unknown error in outputting file {0}", f.getName());
			}
		} catch (Exception ex) {
			Logger.getLogger(ScriptFileLoader.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	private static void goToGenome(String genomeVersion) {
		AnnotatedSeqGroup group = UnibrowControlServlet.determineAndSetGroup(genomeVersion);
		if (group == null) {
			return;
		}
		for (int i=0;i<100;i++) {
			// sleep until versions are initialized
			for (GenericVersion version: group.getEnabledVersions()) {
				if (version.isInitialized() && group == GenometryModel.getGenometryModel().getSelectedSeqGroup()) {
					continue;
				}
				try {
					Thread.sleep(300); // not finished initializing versions
				} catch (InterruptedException ex) {
					Logger.getLogger(ScriptFileLoader.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}

	private static void goToRegion(String region) {
		MapRangeBox.setRange(Application.getSingleton().getMapView(), region);
	}

	private static void loadData(String serverURIorName, String feature_url) {
		GenericServer server = UnibrowControlServlet.loadServer(serverURIorName);
		GenericFeature feature = UnibrowControlServlet.getFeature(server, feature_url);

		if (feature != null) {
			feature.setVisible();
			GenericFeature.setPreferredLoadStrategy(feature, LoadStrategy.VISIBLE);
		}

		GeneralLoadView.getLoadView().createFeaturesTable();
	}

	private static void loadFile(String fileName) {
		File f = new File(fileName.trim());
		LoadFileAction.openURI(f.toURI(), f.getName());
	}

	private static void loadMode(String loadMode, String featureURIStr) {
		
		URI featureURI = null;
		File featureFile = new File(featureURIStr.trim());
		if (featureFile.exists()) {
			featureURI = featureFile.toURI();
		} else {
			featureURI = URI.create(featureURIStr.trim());
		}
		LoadStrategy s = LoadStrategy.NO_LOAD;
		if (loadMode.equalsIgnoreCase("no_load")) {
			s = LoadStrategy.NO_LOAD;
		} else if (loadMode.equalsIgnoreCase("region_in_view") || loadMode.equalsIgnoreCase("visible")) {
			s = LoadStrategy.VISIBLE;
		} else if (loadMode.equalsIgnoreCase("chromosome")) {
			s = LoadStrategy.CHROMOSOME;
		} else if (loadMode.equalsIgnoreCase("genome")) {
			s = LoadStrategy.GENOME;
		}

		// First try to look up for feature in current group.
		AnnotatedSeqGroup seqGroup = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		GenericFeature feature = null;

		// If feature is not found in current group then look up all groups.
		if(seqGroup != null)
			feature = findFeatureInGroup(seqGroup, featureURI);

		if(feature == null){
			for(AnnotatedSeqGroup group : GenometryModel.getGenometryModel().getSeqGroups().values()){
				feature = findFeatureInGroup(group, featureURI);
				if(feature != null)
					break;
			}
		}
		
		if (feature != null) {
			GenericFeature.setPreferredLoadStrategy(feature, s);
		} else {
			Logger.getLogger(ScriptFileLoader.class.getName()).log(
					Level.SEVERE, "Couldn''t find feature :{0}", featureURIStr);
		}
	}

	private static GenericFeature findFeatureInGroup(AnnotatedSeqGroup seqGroup, URI featureURI){
		GenericFeature feature = null;
		for(GenericVersion version : seqGroup.getEnabledVersions()){
			feature = GeneralUtils.findFeatureWithURI(version.getFeatures(), featureURI);
			if(feature != null)
				break;
		}

		return feature;
	}

	/**
	 * Join fields from startField to end of fields.
	 * @param fields
	 * @param startField
	 * @return
	 */
	private static String join(String[] fields, int startField) {
		StringBuilder buffer = new StringBuilder("");
		for(int i=startField;i<fields.length;i++) {
			buffer.append(fields[i]);
		}
		return buffer.toString();
	}

}
