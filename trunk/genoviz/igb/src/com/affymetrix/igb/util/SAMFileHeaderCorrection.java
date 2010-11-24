package com.affymetrix.igb.util;

import java.net.URI;
import java.net.URISyntaxException;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.util.Iso8601Date;
import net.sf.samtools.util.StringLineReader;


/**
 * This class provides a SAMFileHeader correction. It reads the header given as
 * a string from SAMFileHeader.getTextHeader and try to correct various
 * mistakes done by older alignment-programs to harmonize better with the
 * SAM/BAM specification.
 *
 * @author stefan
 */
public class SAMFileHeaderCorrection {

	static public SAMFileHeader mFileHeader = new SAMFileHeader();


	public SAMFileHeaderCorrection() {}

	public static SAMFileHeader getCorrectedHeader(SAMFileHeader header) {
		String headerString = header.getTextHeader();
		StringLineReader lineReader = new StringLineReader(headerString);

		String nextLine;
		String[] lineSplits;

		while((nextLine = lineReader.readLine()) != null) {
			// is nextLine a header-line ?
			if(nextLine.startsWith("@")) {
				lineSplits = nextLine.split("\t");

				lineSplits = correctFields(lineSplits);

				if(lineSplits[0].equals("@HD")) {
					addHeader(lineSplits);
				}
				if(lineSplits[0].equals("@SQ")) {
					addSequence(lineSplits);
				}
				if(lineSplits[0].equals("@RG")) {
					addReadGroup(lineSplits);
				}
				if(lineSplits[0].equals("@PG")) {
					addProgramRecord(lineSplits);
				}
				if(lineSplits[0].equals("@CO")) {
					addComment(nextLine);
				}

			}
		}
		return mFileHeader;
	}

	static private void addHeader(String[] header) {

		String VN = "";	// *Version
		String SO = "";	// Sort order
		String GO = "";	// Group order

		for(int i = 0; i < header.length; i++) {
			if(header[i].startsWith("VN")) {
				VN = header[i].substring(3);
			}
			else if(header[i].startsWith("SO")) {
				SO = header[i].substring(3);
			}
			else if(header[i].startsWith("GO")) {
				GO = header[i].substring(3);
			}
		}

		mFileHeader.setAttribute("VN", VN);
		if(!SO.equals(""))
			mFileHeader.setAttribute("SO", SO);
		try {
			mFileHeader.setGroupOrder(SAMFileHeader.GroupOrder.valueOf(GO));
		} catch(IllegalArgumentException e) { /* do nothing: just ignore this group order */ }
	}

	static private void addSequence(String[] sequence) {

		String SN = "";	// *Sequence name
		int LN = 0;	// *Sequence length
		String AS = "";	// Assembly identifier
		String M5 = "";	// MD5 checksum
		URI UR = null;	// URI
		String SP = "";	// Species

		for(int i = 0; i < sequence.length; i++) {
			if(sequence[i].startsWith("SN")) {
				SN = sequence[i].substring(3);
			}
			else if(sequence[i].startsWith("LN")) {
				LN = Integer.parseInt(sequence[i].substring(3));
			}
			else if(sequence[i].startsWith("AS")) {
				AS = sequence[i].substring(3);
			}
			else if(sequence[i].startsWith("M5")) {
				M5 = sequence[i].substring(3);
			}
			else if(sequence[i].startsWith("UR")) {
				try {
					UR = new URI(sequence[i].substring(3));
					if (UR.getScheme() == null) {
						UR = new URI("file", UR.getUserInfo(), UR.getHost(), UR.getPort(), UR.getPath(), UR.getQuery(), UR.getFragment());
			        }
				} catch (URISyntaxException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			else if(sequence[i].startsWith("SP")) {
				SP = sequence[i].substring(3);
			}
		}

		SAMSequenceRecord record = new SAMSequenceRecord(SN, LN);
		if(!AS.equals(""))
			record.setAssembly(AS);
		if(!M5.equals(""))
			record.setAttribute("M5", M5);
		if(UR != null)
			record.setAttribute("UR", UR);
		if(!SP.equals(""))
			record.setSpecies(SP);

		mFileHeader.addSequence(record);
	}

	static private void addReadGroup(String[] readGroup) {

		String ID = "";	// *Read group identifier
		String SM = "";	// *Sample
		String LB = "";	// Library
		String DS = "";	// Description
		String PU = "";	// Platform unit
		int PI = 0;	// Predicted median insert size
		boolean PIIsSet = false;
		String CN = "";	// Sequence center name
		String DT = "";	// Date
		String PL = "";	// Platform

		for(int i = 0; i < readGroup.length; i++) {
			if(readGroup[i].startsWith("ID")) {
				ID = readGroup[i].substring(3);
			}
			else if(readGroup[i].startsWith("SM")) {
				SM = readGroup[i].substring(3);
			}
			else if(readGroup[i].startsWith("LB")) {
				LB = readGroup[i].substring(3);
			}
			else if(readGroup[i].startsWith("DS")) {
				DS = readGroup[i].substring(3);
			}
			else if(readGroup[i].startsWith("PU")) {
				PU = readGroup[i].substring(3);
			}
			else if(readGroup[i].startsWith("PI")) {
				PI = Integer.parseInt(readGroup[i].substring(3));
				PIIsSet = true;
			}
			else if(readGroup[i].startsWith("CN")) {
				CN = readGroup[i].substring(3);
			}
			else if(readGroup[i].startsWith("DT")) {
				DT = readGroup[i].substring(3);
			}
			else if(readGroup[i].startsWith("PL")) {
				PL = readGroup[i].substring(3);
			}
		}
		SAMReadGroupRecord record = new SAMReadGroupRecord(ID);
		record.setSample(SM);
		if(!LB.equals(""))
			record.setLibrary(LB);
		if(!DS.equals(""))
			record.setDescription(DS);
		if(!PU.equals(""))
			record.setPlatformUnit(PU);
		if(PIIsSet == true)
			record.setPredictedMedianInsertSize(PI);
		if(!CN.equals(""))
			record.setSequencingCenter(CN);
		if(!DT.equals(""))
			record.setRunDate(new Iso8601Date(DT));
		if(!PL.equals(""))
			record.setPlatform(PL);

		mFileHeader.addReadGroup(record);
	}

	static private void addProgramRecord(String[] programRecord) {

		String ID = "";	// *Program name
		String VN = "";	// Program version
		String CL = "";	// Command line

		for(int i = 0; i < programRecord.length; i++) {
			if(programRecord[i].startsWith("ID")) {
				ID = programRecord[i].substring(3);
			}
			else if(programRecord[i].startsWith("VN")) {
				VN = programRecord[i].substring(3);
			}
			else if(programRecord[i].startsWith("CL")) {
				CL = programRecord[i].substring(3);
			}
		}
		SAMProgramRecord record = new SAMProgramRecord(ID);
		if(!VN.equals(""))
			record.setProgramVersion(VN);
		if(!CL.equals(""))
			record.setCommandLine(CL);

		mFileHeader.addProgramRecord(record);
	}

	static private void addComment(String commentLine) {
		mFileHeader.addComment(commentLine.substring(3));
	}



	private static String[] correctFields(String[] fields) {
		// ignoring type-field at position fields[0]
		for(int i = 1; i < fields.length; i++) {

			fields[i] = correctSeperator(fields[i]);

			if(fields[0].startsWith("@HD")) {
				fields[i] = correctHDVersion(fields[i]);
			}
			if(fields[0].startsWith("@SQ")) {
				fields[i] = correctSNSpaces(fields[i]);
			}
		}

		return fields;
	}

	/**
	 * Test and correct the separator of a tag and its data.
	 */
	private static String correctSeperator(String field) {
		if(field.charAt(2) != ':') {
			field = field.substring(0, 2) + ":" + field.substring(3);
		}
		return field;
	}

	/**
	 * Test and correct if HD Version is in format "1.0" instead of maybe "1".
	 */
	private static String correctHDVersion(String field) {
		if(field.startsWith("VN")) {
			if(!field.contains(".")) {
				field = field + ".0";
			}
		}
		return field;
	}

	/**
	 * Test and correct if there are illegal spaces in @SQ SN.
	 */
	private static String correctSNSpaces(String field) {
		if(field.contains(" ")) {
			field = field.substring(0, field.indexOf(' '));
		}
		return field;
	}
}