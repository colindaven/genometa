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
import java.util.*;


/**
 *  A SeqSymmetry (as well as SeqSpan) representation of UCSC MySQL RefSeq annotations.
 *
 *
 *  from http://genome.cse.ucsc.edu/goldenPath/gbdDescriptions.html#RefFlat:
 *<pre>

 Gene Predictions and RefSeq Genes
 In alternative splicing situations each transcript has a row in this table.
 table refFlat
 "A gene prediction with additional geneName field."
 (
 string  geneName;   "Name of gene as it appears in genome browser."
 string  name;       "Name of gene"
 string  chrom;      "Chromosome name"
 char[1] strand;     "+ or - for strand"
 uint    txStart;    "Transcription start position"
 uint    txEnd;      "Transcription end position"
 uint    cdsStart;   "Coding region start"
 uint    cdsEnd;     "Coding region end"
 uint    exonCount;  "Number of exons"
 uint[exonCount] exonStarts; "Exon start positions"
 uint[exonCount] exonEnds;   "Exon end positions"
 )

 *  direct mapping of geneName, name, txStart, txEnd, cdsStart, cdsEnd, exonStarts, exonEnds
 *  (except for name changes, mostly to be clearer about locations -- start-->min, end-->max)
 * </pre>
 *
 */
public final class UcscGeneSym implements SeqSpan, SupportsCdsSpan, SymWithProps {

	String geneName;
	String name;
	int txMin;
	int txMax;
	int cdsMin;
	int cdsMax;
	boolean forward;
	int[] emins;
	int[] emaxs;
	BioSeq seq;
	String type;
	Map<String,Object> props;

	public UcscGeneSym(String type, String geneName, String name,
			BioSeq seq, boolean forward, int txMin, int txMax,
			int cdsMin, int cdsMax, int[] emins, int[] emaxs
			) {
		this.type = type;
		this.geneName = geneName;
		this.name = name;
		this.seq = seq;
		this.forward = forward;
		this.txMin = txMin;
		this.txMax = txMax;
		this.cdsMin = cdsMin;
		this.cdsMax = cdsMax;
		this.emins = emins;
		this.emaxs = emaxs;
			}

	public String getGeneName() { return geneName; }
	public String getName() { return name; }
	public String getType() { return type; }

	public boolean hasCdsSpan() {
		return (cdsMin >= 0 && cdsMax >= 0);
	}

	public SeqSpan getCdsSpan() {
		if (! hasCdsSpan())  { return null; }
		if (forward) { return new SimpleSeqSpan(cdsMin, cdsMax, seq); }
		else { return new SimpleSeqSpan(cdsMax, cdsMin, seq); }
	}

	/**
	 *  SeqSymmetry implementation.
	 */
	public String getID() { return name; }
	public SeqSpan getSpan(BioSeq bs) {
		if (bs.equals(this.seq)) {
			return this;
		}
		else { return null; }
	}

	public SeqSpan getSpan(int index) {
		if (index == 0) {
			return this;
		}
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
	public int getSpanCount() { return 1; }
	public BioSeq getSpanSeq(int index) {
		if (index == 0) { return seq; }
		else { return null; }
	}
	public int getChildCount() { return emins.length; };
	public SeqSymmetry getChild(int index) {
		if (forward) {
			return new SingletonSeqSymmetry(emins[index], emaxs[index], seq);
		}
		else {
			return new SingletonSeqSymmetry(emaxs[index], emins[index], seq);
		}
	}

	//  SeqSpan implementation
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

	public Map<String,Object> getProperties() {
		return cloneProperties();
	}

	public Map<String,Object> cloneProperties() {
		HashMap<String,Object> tprops = new HashMap<String,Object>();
		tprops.put("id", name);
		tprops.put("type", type);
		tprops.put("gene name", geneName);
		tprops.put("seq id", seq.getID());
		tprops.put("forward", forward);
		tprops.put("cds min", Integer.valueOf(cdsMin));
		tprops.put("cds max", Integer.valueOf(cdsMax));
		if (props != null) {
			tprops.putAll(props);
		}
		return tprops;
	}

	public Object getProperty(String key) {
		// test for standard gene sym  props
		if (key.equals("id")) { return name; }
		else if (key.equals("type")) { return getType(); }
		else if (key.equals("gene name") || key.equals("gene_name")) { return geneName; }
		else if (key.equals("seq id")) { return seq.getID(); }
		else if (key.equals("forward")) { return forward; }
		else if (key.equals("cds min")) { return Integer.valueOf(cdsMin); }
		else if (key.equals("cds max")) { return Integer.valueOf(cdsMax); }
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

	public void setStart(int start) {}
	public void setEnd(int end) {}

}


