package com.affymetrix.igb.util;

import java.util.List;

import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.util.Iso8601Date;
import net.sf.samtools.util.StringLineReader;


public class SAMFileHeaderCorrection {
	
	static public SAMFileHeader mFileHeader = new SAMFileHeader();
	

	public SAMFileHeaderCorrection() {}
	
	public static SAMFileHeader correctHeader(String headerString) {
		StringLineReader lineReader = new StringLineReader(headerString);
		
		String nextLine;
		String[] lineSplits;
		
		while((nextLine = lineReader.readLine()) != null) {
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

		String VN = "";
		String SO = "";
		String GO = "";
		
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
		mFileHeader.setAttribute("SO", SO);
		//mFileHeader.setGroupOrder(SAMFileHeader.GroupOrder.valueOf(GO));
	}
	
	static private void addSequence(String[] sequence) {
		
		String SN = "";	// Sequence name
		int LN = 0;	// Sequence length
		String AS = "";	// Assembly identifier
		String M5 = "";	// MD5 checksum
		String UR = "";	// URI
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
				UR = sequence[i].substring(3);
			}
			else if(sequence[i].startsWith("SP")) {
				SP = sequence[i].substring(3);
			}
		}
		
		SAMSequenceRecord record = new SAMSequenceRecord(SN, LN);
		record.setAssembly(AS);
		//record.setAttribute("M5", M5);
		//record.setAttribute("UR", UR);
		record.setSpecies(SP);
		
		mFileHeader.addSequence(new SAMSequenceRecord(SN, LN));	
	} 
	
	static private void addReadGroup(String[] readGroup) {
		
		String ID = "";	// Read group identifier
		String SM = "";	// Sample
		String LB = "";	// Library
		String DS = "";	// Description
		String PU = "";	// Platform unit
		int PI = 0;	// Predicted median insert size
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
		record.setLibrary(LB);
		record.setDescription(DS);
		record.setPlatformUnit(PU);
		record.setPredictedMedianInsertSize(PI);
		record.setSequencingCenter(CN);
		record.setRunDate(new Iso8601Date(DT));
		record.setPlatform(PL);
		
		mFileHeader.addReadGroup(record);
	}
	
	static private void addProgramRecord(String[] programRecord) {
		
		String ID = "";	// Program name
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
		record.setProgramVersion(VN);
		record.setCommandLine(CL);
		
		mFileHeader.addProgramRecord(record);		
	}
	
	static private void addComment(String commentLine) {
		mFileHeader.addComment(commentLine.substring(3));
	}
	
	

	private static String[] correctFields(String[] fields) {
		// ignoring type-field at position fields[0]
		for(int i = 1; i < fields.length; i++) {
			
			// test and correct the separator of tag and data
			if(fields[i].charAt(2) != ':') {
				fields[i] = fields[i].substring(0, 2) + ":" + fields[i].substring(3);
			}
		}
		return fields;
	}
	
}
