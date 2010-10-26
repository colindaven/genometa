package com.affymetrix.genometryImpl.parsers.useq.apps;
import java.io.*;
import java.util.regex.*;
import java.util.zip.*;
import java.util.*;
import com.affymetrix.genometryImpl.parsers.useq.*;
import com.affymetrix.genometryImpl.parsers.useq.data.*;


/**Converts USeq binary archives to text 6 or 12 column bed format.*/
public class USeq2Text {

	//fields
	private File[] useqArchives;

	public USeq2Text(String[] args){
		processArgs(args);

		//for each zip archive
		for (int i=0; i< useqArchives.length; i++){
			System.out.println("Processing "+useqArchives[i].getName());
			printBed(useqArchives[i]);
		}
	}

	@SuppressWarnings("unchecked")
	public void printBed (File useqArchive){
		try {
			File bedFile = new File (useqArchive.getParentFile(), USeqUtilities.removeExtension(useqArchive.getName())+".bed");
			PrintWriter out = new PrintWriter (new FileWriter (bedFile));
			ZipFile zf = new ZipFile(useqArchive);
			Enumeration<ZipEntry> e = (Enumeration<ZipEntry>) zf.entries();

			//make an ArchiveInfo object on the first element in the zip archive
			ZipEntry ze = e.nextElement();
			if (ze.getName().equals(ArchiveInfo.ARCHIVE_README_NAME) == false) throw new IOException("The first zip entry -> "+ze.getName()+", is not the "+ArchiveInfo.ARCHIVE_README_NAME+"! Aborting.");
			ArchiveInfo ai = new ArchiveInfo(zf.getInputStream(ze), false);

			//write out ai info as comments
			ai.appendCommentedKeyValues(out);

			//load data slices
			while(e.hasMoreElements()) {
				ze = e.nextElement();
				//make a SliceInfo object
				SliceInfo si = new SliceInfo(ze.getName());
				DataInputStream dis = new DataInputStream( new BufferedInputStream(zf.getInputStream(ze)));
				String extension = si.getBinaryType();
				//call appropriate maker
				//Position
				if (USeqUtilities.POSITION.matcher(extension).matches()) new PositionData (dis, si).writeBed(out);
				//PositionScore
				else if (USeqUtilities.POSITION_SCORE.matcher(extension).matches()) new PositionScoreData (dis, si).writeBed(out);
				//PositionText
				else if (USeqUtilities.POSITION_TEXT.matcher(extension).matches()) new PositionTextData (dis, si).writeBed(out);
				//PositionScoreText
				else if (USeqUtilities.POSITION_SCORE_TEXT.matcher(extension).matches()) new PositionScoreTextData (dis, si).writeBed(out);
				//Region
				else if (USeqUtilities.REGION.matcher(extension).matches()) new RegionData (dis, si).writeBed(out);
				//RegionScore
				else if (USeqUtilities.REGION_SCORE.matcher(extension).matches()) new RegionScoreData (dis, si).writeBed(out);
				//RegionText
				else if (USeqUtilities.REGION_TEXT.matcher(extension).matches())  new RegionTextData (dis, si).writeBed(out);
				//RegionScoreText
				else if (USeqUtilities.REGION_SCORE_TEXT.matcher(extension).matches()) new RegionScoreTextData (dis, si).writeBed(out);
				else  throw new IOException("\nFailed to recognize the binary file extension! "+ze.getName());
				dis.close();
			}
			out.close();
		} catch (IOException e) {
			System.err.println("\nError, could not process binary archive!");
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		if (args.length ==0){
			printDocs();
			System.exit(0);
		}
		new USeq2Text(args);
	}

	/**This method will process each argument and assign new variables*/
	public void processArgs(String[] args){
		Pattern pat = Pattern.compile("-[a-z]");
		System.out.println("\nArguments: "+USeqUtilities.stringArrayToString(args, " ")+"\n");
		for (int i = 0; i<args.length; i++){
			String lcArg = args[i].toLowerCase();
			Matcher mat = pat.matcher(lcArg);
			if (mat.matches()){
				char test = args[i].charAt(1);
				try{
					switch (test){
					case 'f': useqArchives = USeqUtilities.extractFiles(new File(args[++i]), USeqUtilities.USEQ_EXTENSION_NO_PERIOD); break;
					case 'h': printDocs(); System.exit(0); break;
					default: USeqUtilities.printExit("\nProblem, unknown option! " + mat.group());
					}
				}
				catch (Exception e){
					USeqUtilities.printExit("\nSorry, something doesn't look right with this parameter: -"+test+"\n");
				}
			}
		}
		//pull files
		if (useqArchives == null || useqArchives.length == 0) USeqUtilities.printExit("\nCannot find any xxx."+USeqUtilities.USEQ_EXTENSION_NO_PERIOD+" USeq archives?\n");

	}	


	public static void printDocs(){
		System.out.println("\n" +
				"**************************************************************************************\n" +
				"**                                USeq 2 Text: Dec 2009                             **\n" +
				"**************************************************************************************\n" +
				"Converts USeq archives to six column, tab delimited, bed format: chrom, start, stop,\n" +
				"text, score, strand. Interbase coordinates.\n" +

				"\nOptions:\n"+
				"-f Full path file/directory containing xxx."+USeqUtilities.USEQ_EXTENSION_NO_PERIOD+" files.\n" +

				"\nExample: java -Xmx4G -jar pathTo/USeq/Apps/USeq2Text -f\n" +
				"      /AnalysisResults/USeqDataArchives/ \n\n" +

		"**************************************************************************************\n");

	}


}
