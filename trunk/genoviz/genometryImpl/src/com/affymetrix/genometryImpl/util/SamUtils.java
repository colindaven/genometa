package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.UcscBedSym;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;
import net.sf.samtools.Cigar;
import net.sf.samtools.CigarElement;
import net.sf.samtools.CigarOperator;

import net.sf.samtools.SAMRecord;
import net.sf.samtools.SAMFileHeader;
import net.sf.samtools.SAMProgramRecord;
import net.sf.samtools.SAMReadGroupRecord;
import net.sf.samtools.SAMRecord.SAMTagAndValue;
import net.sf.samtools.SAMSequenceDictionary;
import net.sf.samtools.SAMSequenceRecord;
import net.sf.samtools.util.SequenceUtil;

/**
 *
 * @author hiralv
 */
public abstract class SamUtils {

	public static final String CIGARPROP = "cigar";
	public static final String RESIDUESPROP = "residues";
	public static final String BASEQUALITYPROP = "baseQuality";

	/**
	 * Convert SAMRecord to SymWithProps.
	 * @param sr - SAMRecord
	 * @param seq - chromosome
	 * @param meth - method name
	 * @return SimpleSymWithProps
	 */
	public static SymWithProps convertSAMRecordToSymWithProps(SAMRecord sr, BioSeq seq, String featureName, String meth){
		SimpleSeqSpan span = null;
		int start = sr.getAlignmentStart() - 1; // convert to interbase
		int end = sr.getAlignmentEnd();
		if (!sr.getReadNegativeStrandFlag()) {
			span = new SimpleSeqSpan(start, end, seq);
		} else {
			span = new SimpleSeqSpan(end, start, seq);
		}

		List<SimpleSymWithProps> childs = getChildren(sr, seq, sr.getCigar(), sr.getReadString(), span.getLength());

		int blockMins[] = new int[childs.size()];
		int blockMaxs[] = new int[childs.size()];
		for (int i=0;i<childs.size();i++) {
			SymWithProps child = childs.get(i);
			blockMins[i] =  child.getSpan(0).getMin() + span.getMin();
			blockMaxs[i] =  blockMins[i] + child.getSpan(0).getLength();
		}

		if(childs.isEmpty()) {
			blockMins = new int[1];
			blockMins[0] = span.getStart();
			blockMaxs = new int[1];
			blockMaxs[0] = span.getEnd();
		}

		SymWithProps sym = new UcscBedSym(featureName, seq, start, end, sr.getReadName(), 0.0f, span.isForward(), 0, 0, blockMins, blockMaxs);
		sym.setProperty(BASEQUALITYPROP, sr.getBaseQualityString());
		sym.setProperty("id",sr.getReadName());
		for (SAMTagAndValue tv : sr.getAttributes()) {
			sym.setProperty(tv.tag, tv.value);
		}
		sym.setProperty(CIGARPROP, sr.getCigar());
		sym.setProperty(RESIDUESPROP, sr.getReadString());
		if (sr.getCigar() == null || sym.getProperty("MD") == null) {
			//sym.setProperty("residues", sr.getReadString());
		} else {
			// If both the MD and Cigar properties are set, don't need to specify residues.
			byte[] SEQ = SequenceUtil.makeReferenceFromAlignment(sr, false);
			sym.setProperty("SEQ", SEQ);
		}
		sym.setProperty("method", meth);

		getFileHeaderProperties(sr.getHeader(), sym);

		return sym;
	}

	public static List<SimpleSymWithProps> getChildren(SAMRecord sr, BioSeq seq, Cigar cigar, String residues, int spanLength) {
		List<SimpleSymWithProps> results = new ArrayList<SimpleSymWithProps>();
		if (cigar == null || cigar.numCigarElements() == 0) {
			return results;
		}
		int currentChildStart = 0;
		int currentChildEnd = 0;
		int celLength = 0;

		for (CigarElement cel : cigar.getCigarElements()) {
			try {
				celLength = cel.getLength();
				if (cel.getOperator() == CigarOperator.DELETION) {
					currentChildStart = currentChildEnd + celLength;
					currentChildEnd = currentChildStart;
				} else if (cel.getOperator() == CigarOperator.INSERTION) {
					// TODO -- allow possibility that INSERTION is terminator, not M
					// print insertion
					currentChildStart = currentChildEnd;
					currentChildEnd = currentChildStart;
				} else if (cel.getOperator() == CigarOperator.M) {
					// print matches
					currentChildEnd += celLength;
					SimpleSymWithProps ss = new SimpleSymWithProps();
					if (!sr.getReadNegativeStrandFlag()) {
						ss.addSpan(new SimpleSeqSpan(currentChildStart, currentChildEnd, seq));
					}
					else {
						ss.addSpan(new SimpleSeqSpan(currentChildEnd, currentChildStart, seq));
					}
					results.add(ss);
				} else if (cel.getOperator() == CigarOperator.N) {
					currentChildStart = currentChildEnd + celLength;
					currentChildEnd = currentChildStart;
				} else if (cel.getOperator() == CigarOperator.PADDING) {
					// TODO -- allow possibility that PADDING is terminator, not M
					// print matches
					currentChildEnd += celLength;
				} else if (cel.getOperator() == CigarOperator.SOFT_CLIP) {
					// skip over soft clip
				} else if (cel.getOperator() == CigarOperator.HARD_CLIP) {
					// hard clip can be ignored
				}
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		}

		return results;
	}

	public static void getFileHeaderProperties(SAMFileHeader hr, SymWithProps sym) {
		if (hr == null) {
			return;
		}
		//Sequence Dictionary
		SAMSequenceDictionary ssd = hr.getSequenceDictionary();
		for (SAMSequenceRecord ssr : ssd.getSequences()) {
			if (ssr.getAssembly() != null) {
				sym.setProperty("genomeAssembly", ssr.getAssembly());
			}
			if (ssr.getSpecies() != null) {
				sym.setProperty("species  ", ssr.getSpecies());
			}
		}
		//Read Group
		for (SAMReadGroupRecord srgr : hr.getReadGroups()) {
			for (Entry<String, Object> en : srgr.getAttributes()) {
				if (en.getValue() instanceof String) {
					sym.setProperty(en.getKey(), en.getValue());
				}
			}
		}
		//Program
		for (SAMProgramRecord spr : hr.getProgramRecords()) {
			for (Entry<String, Object> en : spr.getAttributes()) {
				if (en.getValue() instanceof String) {
					sym.setProperty(en.getKey(), en.getValue());
				}
			}
		}
	}

	/**
	 * Rewrite the residue string, based upon cigar information
	 * @param cigarObj
	 * @param residues
	 * @param spanLength
	 * @return
	 */
	public static String interpretCigar(Object cigarObj, String residues, int startPos, int spanLength) {
		Cigar cigar = (Cigar)cigarObj;
		if (cigar == null || cigar.numCigarElements() == 0) {
			return residues;
		}
		StringBuilder sb = new StringBuilder(spanLength);
		int currentPos = 0;
		for (CigarElement cel : cigar.getCigarElements()) {
			try {
				int celLength = cel.getLength();
				if (cel.getOperator() == CigarOperator.DELETION) {
					//currentPos += celLength;	// skip over deletion
				} else if (cel.getOperator() == CigarOperator.INSERTION) {
					currentPos += celLength;	// print insertion
				} else if (cel.getOperator() == CigarOperator.M) {
					if (currentPos >= startPos) {
						sb.append(residues.substring(currentPos, currentPos + celLength));
					}
					currentPos += celLength;	// print matches
				} else if (cel.getOperator() == CigarOperator.N) {
					// ignore skips
				} else if (cel.getOperator() == CigarOperator.PADDING) {
					char[] tempArr = new char[celLength];
					Arrays.fill(tempArr, '*');		// print padding as '*'
					sb.append(tempArr);
					currentPos += celLength;
				} else if (cel.getOperator() == CigarOperator.SOFT_CLIP) {
					currentPos += celLength;	// skip over soft clip
				} else if (cel.getOperator() == CigarOperator.HARD_CLIP) {
					continue;				// hard clip can be ignored
				}
				if (currentPos - startPos >= spanLength) {
					break;
				}
			} catch (Exception ex) {
				ex.printStackTrace();
				if (spanLength - currentPos - startPos > 0) {
					char[] tempArr = new char[spanLength - currentPos - startPos];
					Arrays.fill(tempArr, '.');
					sb.append(tempArr);
				}
			}
		}

		return sb.toString().intern();
	}

}
