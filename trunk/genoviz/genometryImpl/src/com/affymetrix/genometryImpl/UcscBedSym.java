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

package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import java.io.*;
import java.util.*;


/**
 *  A SeqSymmetry (as well as SeqSpan) representation of UCSC BED format annotatations.
 *  <pre>
 *  From http://genome.ucsc.edu/goldenPath/help/customTrack.html#BED
 *  BED format provides a flexible way to define the data lines that are displayed
 *  in an annotation track. BED lines have three required fields and nine additional
 *  optional fields. The number of fields per line must be consistent throughout
 *  any single set of data in an annotation track.
 *
 * The first three required BED fields are:
 *    chrom - The name of the chromosome (e.g. chr3, chrY, chr2_random) or contig (e.g. ctgY1).
 *    chromStart - The starting position of the feature in the chromosome or contig.
 *         The first base in a chromosome is numbered 0.
 *    chromEnd - The ending position of the feature in the chromosome or contig. The chromEnd
 *         base is not included in the display of the feature. For example, the first 100 bases
 *         of a chromosome are defined as chromStart=0, chromEnd=100, and span the bases numbered 0-99.
 * The 9 additional optional BED fields are:
 *    name - Defines the name of the BED line. This label is displayed to the left of the BED line
 *        in the Genome Browser window when the track is open to full display mode.
 *    score - A score between 0 and 1000. If the track line useScore attribute is set to 1 for
 *        this annotation data set, the score value will determine the level of gray in which
 *        this feature is displayed (higher numbers = darker gray).
 *    strand - Defines the strand - either '+' or '-'.
 *    thickStart - The starting position at which the feature is drawn thickly (for example,
 *        the start codon in gene displays).
 *    thickEnd - The ending position at which the feature is drawn thickly (for example,
 *        the stop codon in gene displays).
 *    reserved - This should always be set to zero.
 *    blockCount - The number of blocks (exons) in the BED line.
 *    blockSizes - A comma-separated list of the block sizes. The number of items in this list
 *        should correspond to blockCount.
 *    blockStarts - A comma-separated list of block starts. All of the blockStart positions
 *        should be calculated relative to chromStart. The number of items in this list should
 *        correspond to blockCount.
 * WARNING -- relying on parser to modify blockStarts so they are in chrom/seq coordinates
 *            (NOT relative to chromStart)
 *
 * Example:
 *   Here's an example of an annotation track that uses a complete BED definition:
 *
 *  track name=pairedReads description="Clone Paired Reads" useScore=1
 *  chr22 1000 5000 cloneA 960 + 1000 5000 0 2 567,488, 0,3512
 *  chr22 2000 6000 cloneB 900 - 2000 6000 0 2 433,399, 0,3601
 * </pre>
 */
public final class UcscBedSym implements SeqSpan, SupportsCdsSpan, TypedSym, SymWithProps, Scored  {
	BioSeq seq; // "chrom"
	int txMin; // "chromStart"
	int txMax; // "chromEnd"
	String name; // "name"
	float score; // "score" // (if score == Float.NEGATIVE_INFINITY then score is not used)
	boolean forward; // "strand"
	int cdsMin = Integer.MIN_VALUE;  // "thickStart" (if = Integer.MIN_VALUE then cdsMin not used)
	int cdsMax = Integer.MIN_VALUE;  // "thickEnd" (if = Integer.MIN_VALUE then cdsMin not used)
	int[] blockMins; // "blockStarts" + "txMin"
	int[] blockMaxs; // "blockStarts" + "txMin" + "blockSizes"
	String type;
	Map<String,Object> props;
	boolean hasCdsSpan = false;

	/**
	 *  Constructs a SeqSymmetry optimized for BED-file format.
	 *  This object is optimized for the case where all optional columns in the
	 *  bed file are used.  If you are using only the first few columns, it would
	 *  be more efficient to use a different SeqSymmetry object.
	 *  @param cdsMin the start of the CDS region, "thinEnd", or Integer.MIN_VALUE.
	 *         If cdsMin = Integer.MIN_VALUE or cdsMin = cdsMax, then there is no CDS.
	 *  @param cdsMax the end of the CDS region, "thickEnd", or Integer.MIN_VALUE.
	 *  @param score an optional score, or Float.NEGATIVE_INFINITY to indicate no score.
	 */
	public UcscBedSym(String type, BioSeq seq, int txMin, int txMax, String name, float score,
			boolean forward, int cdsMin, int cdsMax, int[] blockMins, int[] blockMaxs) {
		this.type = type;
		this.seq = seq;  // replace chrom name-string with reference to chrom BioSeq
		this.txMin = txMin;
		this.txMax = txMax;
		this.name = name;
		this.score = score;
		this.forward = forward;
		this.cdsMin = cdsMin;
		this.cdsMax = cdsMax;
		hasCdsSpan = ((cdsMin != Integer.MIN_VALUE) && (cdsMax != Integer.MIN_VALUE) && (cdsMin != cdsMax));

		this.blockMins = blockMins;
		this.blockMaxs = blockMaxs;
	}

	public String getName() { return name; }
	public String getType() { return type; }

	/**
	 *  Returns true if the cds was specified in the constructor with valid values.
	 *  If cdsMin = cdsMax = Integer.MIN_VALUE, or if cdsMin = cdsMax, then there is no CDS.
	 *
	 */
	public boolean hasCdsSpan() { return hasCdsSpan; }
	public SeqSpan getCdsSpan() {
		if (! hasCdsSpan()) { return null; }
		if (forward) { return new SimpleSeqSpan(cdsMin, cdsMax, seq); }
		else { return new SimpleSeqSpan(cdsMax, cdsMin, seq); }
	}

	public String getID() { return name; }
	public SeqSpan getSpan(BioSeq bs) {
		if (bs.equals(this.seq)) { return this; }
		else { return null; }
	}

	public SeqSpan getSpan(int index) {
		if (index == 0) { return this; }
		else { return null; }
	}

	public boolean getSpan(BioSeq bs, MutableSeqSpan span) {
		if (bs.equals(this.seq)) {
			if (forward) {
				span.set(txMin, txMax, seq);
			}
			else {
				span.set(txMax, txMin, seq);
			}
			return true;
		}
		else { return false; }
	}

	public boolean getSpan(int index, MutableSeqSpan span) {
		if (index == 0) {
			if (forward) {
				span.set(txMin, txMax, seq);
			}
			else {
				span.set(txMax, txMin, seq);
			}
			return true;
		}
		else { return false; }
	}

	/** Always returns 1. */
	public int getSpanCount() { return 1; }

	/** Returns null if index is not 1. */
	public BioSeq getSpanSeq(int index) {
		if (index == 0) { return seq; }
		else { return null; }
	}

	public int getChildCount() {
		if (blockMins == null)  { return 0; }
		else  { return blockMins.length; }
	}

	public SeqSymmetry getChild(int index) {
		if (blockMins == null || (blockMins.length <= index)) { return null; }
		if (forward) {
			// blockMins are in seq coordinates, NOT relative to txMin
			//    (transforming blockStarts in BED format to blockMins in seq coordinates
			//       is handled by BedParser)
			//      return new SingletonSeqSymmetry(blockMins[index],
			//      				      blockMins[index] + blockSizes[index], seq);
			return new BedChildSingletonSeqSym(blockMins[index], blockMaxs[index], seq);
		}
		else {
			return new BedChildSingletonSeqSym(blockMaxs[index], blockMins[index], seq);
		}
	}

	class BedChildSingletonSeqSym extends SingletonSeqSymmetry implements SymWithProps, Scored {
		public BedChildSingletonSeqSym(int start, int end, BioSeq seq) {
			super(start, end, seq);
		}

		// For the web links to be constructed properly, this class must implement getID(),
		// or must NOT implement SymWithProps.
		public String getID() {return UcscBedSym.this.getID();}
		public Map<String,Object> getProperties() {return UcscBedSym.this.getProperties();}
		public Map<String,Object> cloneProperties() {return UcscBedSym.this.cloneProperties();}
		public Object getProperty(String key) {return UcscBedSym.this.getProperty(key);}
		public boolean setProperty(String key, Object val) {return UcscBedSym.this.setProperty(key, val);}
		public float getScore() {return UcscBedSym.this.getScore(); }
	}

	// SeqSpan implementation
	public int getStart() { return (forward ? txMin : txMax); }
	public int getEnd() { return (forward ? txMax : txMin); }
	public int getMin() { return txMin; }
	public int getMax() { return txMax; }
	public int getLength() { return (txMax - txMin); }
	public boolean isForward() { return forward; }
	public BioSeq getBioSeq() { return seq; }
	public double getStartDouble() { return (double)getStart(); }
	public double getEndDouble() { return (double)getEnd(); }
	public double getMaxDouble() { return (double)getMax(); }
	public double getMinDouble() { return (double)getMin(); }
	public double getLengthDouble() { return (double)getLength(); }
	public boolean isIntegral() { return true; }

	public float getScore() { return score; }

	public Map<String,Object> getProperties() {
		return cloneProperties();
	}

	public Map<String,Object> cloneProperties() {
		HashMap<String,Object> tprops = new HashMap<String,Object>();
		tprops.put("id", name);
		tprops.put("type", type);
		tprops.put("name", name);
		tprops.put("seq id", seq.getID());
		tprops.put("forward", forward);
		if (hasCdsSpan) {
			tprops.put("cds min", Integer.valueOf(cdsMin));
			tprops.put("cds max", Integer.valueOf(cdsMax));
		} if (score != Float.NEGATIVE_INFINITY) {
			tprops.put("score", new Float(score));
		}
		if (props != null) {
			tprops.putAll(props);
		}
		return tprops;
	}

	public Object getProperty(String key) {
		// test for standard gene sym  props
		if (key.equals("id")) { return name; }
		else if (key.equals("type")) { return getType(); }
		else if (key.equals("method"))  { return getType(); }
		else if (key.equals("name")) { return name; }
		else if (key.equals("seq id")) { return seq.getID(); }
		else if (key.equals("forward")) { return forward; }
		else if (hasCdsSpan && key.equals("cds min")) { return Integer.valueOf(cdsMin); }
		else if (hasCdsSpan && key.equals("cds max")) { return Integer.valueOf(cdsMax); }
		else if (key.equals("score") && (score != Float.NEGATIVE_INFINITY)) { return new Float(score); }
		else if (props != null)  {
			return props.get(key);
		}
		else  { return null; }
	}

	public boolean setProperty(String name, Object val) {
		if (props == null) {
			props = new Hashtable<String,Object>();
		}
		props.put(name, val);
		return true;
	}

	public void outputBedFormat(DataOutputStream out) throws IOException  {
		out.write(seq.getID().getBytes());
		out.write('\t');
		out.write(Integer.toString(txMin).getBytes());
		out.write('\t');
		out.write(Integer.toString(txMax).getBytes());
		// only first three fields are required

		// only keep going if has name
		if (name != null) {
			out.write('\t');
			out.write(name.getBytes());
			// only keep going if has score field
			if (score > Float.NEGATIVE_INFINITY) {
				out.write('\t');
				if (score == 0) {
					out.write('0');
				} else {
					out.write(Float.toString(score).getBytes());
				}
				out.write('\t');
				if (forward) { out.write('+'); }
				else { out.write('-'); }
				// only keep going if has thickstart/thickend
				if (cdsMin > Integer.MIN_VALUE &&
						cdsMax > Integer.MIN_VALUE)  {
					out.write('\t');
					out.write(Integer.toString(cdsMin).getBytes());
					out.write('\t');
					out.write(Integer.toString(cdsMax).getBytes());
					// only keep going if has blockcount/blockSizes/blockStarts
					int child_count = this.getChildCount();
					if (child_count > 0) {
						out.write('\t');
						// writing out extra "reserved" field, which currently should always be 0
						out.write('0');
						out.write('\t');
						out.write(Integer.toString(child_count).getBytes());
						out.write('\t');
						// writing blocksizes
						for (int i=0; i<child_count; i++) {
							out.write(Integer.toString(blockMaxs[i]-blockMins[i]).getBytes());
							out.write(',');
						}
						out.write('\t');
						// writing blockstarts
						for (int i=0; i<child_count; i++) {
							out.write(Integer.toString(blockMins[i]-txMin).getBytes());
							out.write(',');
						}
					}
						}
			}
		}
		out.write('\n');

	}


	public void setStart(int start) {	}

	public void setEnd(int end) {	}


}


