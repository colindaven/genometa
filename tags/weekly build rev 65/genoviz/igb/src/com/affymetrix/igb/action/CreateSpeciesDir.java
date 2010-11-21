package com.affymetrix.igb.action;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.List;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.das.DasSource;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;

import com.affymetrix.igb.view.load.GeneralLoadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.igb.featureloader.QuickLoad;
import com.affymetrix.igb.menuitem.FileTracker;

import static com.affymetrix.igb.IGBConstants.BUNDLE;


/**
 * A class to create quickload directory for selected species.
 * @author hiralv
 */
public class CreateSpeciesDir extends AbstractAction {

	private final FileTracker genome_dir_tracker;
	private final JFrame gviewerFrame;
	private static final String SELECT_SPECIES = BUNDLE.getString("speciesCap");
	
	public CreateSpeciesDir(JFrame gviewerFrame, FileTracker genome_dir_tracker){
		super(BUNDLE.getString("createGenome"));
		this.genome_dir_tracker = genome_dir_tracker;
		this.gviewerFrame = gviewerFrame;
	}
	
	public void actionPerformed(ActionEvent e) {
		String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();

		if(SELECT_SPECIES.equals(speciesName)){
			ErrorHandler.errorPanel("Please select a species first");
			return;
		}
		createDirForSpecies(genome_dir_tracker, gviewerFrame, speciesName);
	}

	private static void createDirForSpecies(FileTracker genome_dir_tracker, JFrame gviewerFrame, String speciesName){
		File currDir = genome_dir_tracker.getFile();
		if (currDir == null) {
			currDir = new File(System.getProperty("user.home"));
		}

		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		fc.setMultiSelectionEnabled(false);
		fc.setCurrentDirectory(currDir);
		fc.rescanCurrentDirectory();
		int option = fc.showDialog(gviewerFrame, "Select");
		final File location = fc.getSelectedFile();

		if (option != JFileChooser.APPROVE_OPTION || location == null) {
			return;
		}

		genome_dir_tracker.setFile(fc.getCurrentDirectory());

		createDirForSpecies(location.getAbsolutePath() + "/", speciesName);
	}
	
	private static void createDirForSpecies(String path, String speciesName){
		File speciesDir = GeneralUtils.makeDir(path+speciesName);

		List<String> gVersions = GeneralLoadUtils.getGenericVersions(speciesName);

		//Write Contents.txt
		FormatWriter(ContentWriter.class,speciesDir.getAbsolutePath(),gVersions.toArray());
		
		for(String gVersion : gVersions){
			getAllVisbileFeatures(speciesDir.getAbsolutePath(), gVersion);
		}
		
	}

	private static void getAllVisbileFeatures(String path, String version){

		//Create version directory
		GeneralUtils.makeDir(path + "/" +version);

		AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSeqGroup(version);
		List<GenericFeature> features = GeneralLoadUtils.getFeatures(group);
		List<String[][]> annots = new ArrayList<String[][]>();
		Set<BioSeq> chromInfo = new HashSet<BioSeq>();

		for (GenericFeature feature : features) {

			if (feature.isVisible()) {
				ServerType type = getServerType(feature);
				String urlString = getURLString(type, feature);
				String fileName = getFileName(urlString);
				File file = GeneralUtils.getFile(urlString, false);
				if(type == ServerType.LocalFiles){
					GeneralUtils.copyFileTo(file, fileName, path + "/" + version);
				}else
					GeneralUtils.moveFileTo(file, fileName, path + "/" + version);

				String[][] det = new String[2][2];
				det[0][0] = "name";
				det[0][1] = fileName;
				det[1][0] = "title";
				det[1][1] = feature.featureName;

				annots.add(det);

				if(feature.symL != null)
					chromInfo.addAll(feature.symL.getChromosomeList());
				
			}
		}

		//Write modChromInfo.txt file
		FormatWriter(ModChromWriter.class, path, chromInfo.toArray());

		
		//Writer annots.xml
		FormatWriter(AnnotWriter.class, path + "/" + version, annots.toArray());
		
	}

	private static String getFileName(String url){
		return url.substring(url.lastIndexOf("/")+1, url.length());
	}

	private static ServerType getServerType(GenericFeature feature){
		if (feature.gVersion.versionSourceObj instanceof Das2VersionedSource) {
			return ServerType.DAS2;
		} else if (feature.gVersion.versionSourceObj instanceof DasSource) {
			return ServerType.DAS;
		} else if (feature.gVersion.versionSourceObj instanceof QuickLoadServerModel) {
			return ServerType.QuickLoad;
		} else if (feature.gVersion.versionSourceObj == null){
			return ServerType.LocalFiles;
		}

		return null;
	}

	private static String getURLString(ServerType type, GenericFeature feature){
		
		switch(type){
			case QuickLoad:
				return QuickLoad.determineURI(feature.gVersion, feature.featureName, "").toString();
				
			case LocalFiles:
				return feature.symL.uri.toString();

			default:
				Logger.getLogger(CreateSpeciesDir.class.getName()).log(
					Level.INFO, "Cannot get feature {0} from Das/Das2. !!!",
					new Object[]{feature.featureName});
		}

		return "";
		
	}


	static interface IFormatWriter{
		public String getString(Object obj);
		public String getFileName();
		public String getStartTag();
		public String getEndTag();
	}

	static class ContentWriter implements IFormatWriter{
		
		public String getString(Object obj) {
			return (String)obj + "\n";
		}

		public String getFileName() {
			return Constants.contentsTxt;
		}

		public String getStartTag() { return "";}
		public String getEndTag() { return "";}
	}

	static class ModChromWriter implements IFormatWriter{

		public String getString(Object obj) {
			BioSeq seq = (BioSeq)obj;
			return seq.getID() + "\t" + seq.getLength() + "\n";
		}

		public String getFileName() {
			return Constants.modChromInfoTxt;
		}

		public String getStartTag() { return "";}
		public String getEndTag() { return "";}
	}

	static class AnnotWriter implements IFormatWriter {

		public String getString(Object obj) {
			String[][] det = (String[][])obj;
			String retString = "<file ";

			for(String[] col : det){
				retString += col[0] + " = '" + col[1] + "' ";
			}

			retString += "/> \n";

			return retString;
		}

		public String getFileName() {
			return Constants.annotsXml;
		}

		public String getStartTag() {
			return "<files> \n";
		}

		public String getEndTag() {
			return "</files> \n";
		}

	}

	private static boolean FormatWriter(Class <? extends IFormatWriter> c, String path, Object[] objects){
		File file = null;
		FileWriter fstream = null;
		BufferedWriter out = null;

		try {
			IFormatWriter writer = c.newInstance();
			file = new File(path + "/" + writer.getFileName());
			if (!file.exists()) {
				if (!file.createNewFile()) {
					return false;
				}
			}

			fstream = new FileWriter(file, false);
			out = new BufferedWriter(fstream);

			out.write(writer.getStartTag());
			for (Object object : objects) {
				out.write(writer.getString(object));
			}
			out.write(writer.getEndTag());
			
		} catch (Exception ex) {
			ex.printStackTrace();
			
		} finally {
			GeneralUtils.safeClose(out);
			GeneralUtils.safeClose(fstream);
		}

		return true;
	}

}
