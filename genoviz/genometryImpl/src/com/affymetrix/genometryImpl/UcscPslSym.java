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

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.EfficientPairSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;

import java.util.*;
import java.io.*;

/**
 *  A SeqSymmetry optimized for holding data from PSL files.
 *  Target span is at span index {@link #QUERY_INDEX} = 0.
 *  Query span is at span {@link #TARGET_INDEX} = 1.
 */
public class UcscPslSym
	implements TypedSym, SearchableSeqSymmetry, SymWithProps {

	public static final int QUERY_INDEX = 0;
	public static final int TARGET_INDEX = 1;

	private String type;
	private int matches;
	private int mismatches;
	private int repmatches; // should be derivable w/o residues
	private int ncount;
	private int qNumInsert;  // should be derivable w/o residues
	private int qBaseInsert; // should be derivable w/o residues
	private int tNumInsert;  // should be derivable w/o residues
	private int tBaseInsert; // should be derivable w/o residues
	protected boolean same_orientation;
	private boolean overlapping_query_coords = false;

	protected BioSeq queryseq;
	private int qmin;
	private int qmax;
	protected BioSeq targetseq;
	private int tmin;
	private int tmax;
	private String[] target_res_arr;
	protected int[] blockSizes;
	protected int[] qmins;
	protected int[] tmins;
	private Map<String,Object> props;

	public UcscPslSym(String type,
			int matches,
			int mismatches,
			int repmatches,
			int ncount,
			int qNumInsert,
			int qBaseInsert,
			int tNumInsert,
			int tBaseInsert,
			boolean same_orientation,
			BioSeq queryseq,
			int qmin,
			int qmax,
			BioSeq targetseq,
			int tmin,
			int tmax,
			int blockcount,  // now ignored, uses blockSizes.length
			int[] blockSizes,
			int[] qmins,
			int[] tmins
			) {
		this(type, matches, mismatches, repmatches, ncount, 
				qNumInsert, qBaseInsert, tNumInsert, tBaseInsert,
				same_orientation, queryseq, qmin, qmax,
				targetseq, tmin, tmax, null, blockcount, blockSizes, qmins, tmins);
	}
	/**
	 *  @param blockcount  ignored, uses blockSizes.length
	 */
	public UcscPslSym(String type,
			int matches,
			int mismatches,
			int repmatches,
			int ncount,
			int qNumInsert,
			int qBaseInsert,
			int tNumInsert,
			int tBaseInsert,
			boolean same_orientation,
			BioSeq queryseq,
			int qmin,
			int qmax,
			BioSeq targetseq,
			int tmin,
			int tmax,
			String[] target_res_arr,
			int blockcount,  // now ignored, uses blockSizes.length
			int[] blockSizes,
			int[] qmins,
			int[] tmins
			) {

		this.type = type;
		this.matches = matches;
		this.mismatches = mismatches;
		this.repmatches = repmatches;
		this.ncount = ncount;
		this.qNumInsert = qNumInsert;
		this.qBaseInsert = qBaseInsert;
		this.tNumInsert = tNumInsert;
		this.tBaseInsert = tBaseInsert;
		this.same_orientation = same_orientation;
		this.queryseq = queryseq;
		this.qmin = qmin;
		this.qmax = qmax;
		this.targetseq = targetseq;
		this.tmin = tmin;
		this.tmax = tmax;
		this.target_res_arr = target_res_arr;
		this.blockSizes = blockSizes;
		this.qmins = qmins;
		this.tmins = tmins;

		// calculating whether any of the query coords overlap (assumes query coords are sorted in ascending order)
		// (should probably also do this for target coords???)
		int count = qmins.length-1;
		int prevmin = 0;
		for (int i=0; i<count; i++) {
			if ((qmins[i] < prevmin) || ((qmins[i] + blockSizes[i]) > qmins[i+1])) {
				overlapping_query_coords = true;
				break;
			}
			prevmin = qmins[i];
		}

	}

	private static String getResidue(String[] target_res_arr){
		if(target_res_arr == null)
			return "";

		StringBuilder builder = new StringBuilder(2000);

		for(int i=0; i<target_res_arr.length; i++){
			builder.append(target_res_arr[i]);
		}

		return builder.toString();
	}
	
	public String getType() { return type; }

	/**
	 *  Returns the queryseq id.
	 */
	public String getID() { return queryseq.getID(); }

	/** Always returns 2. */
	public int getSpanCount() { return 2; }

	public SeqSpan getSpan(BioSeq bs) {
		SeqSpan span = null;
		if (bs.equals(targetseq)) {
			if (same_orientation) { span = new SimpleSeqSpan(tmin, tmax, targetseq); }
			else { span = new SimpleSeqSpan(tmax, tmin, targetseq); }
		}
		else if (bs.equals(queryseq)) {
			span = new SimpleSeqSpan(qmin, qmax, queryseq);
		}
		return span;
	}

	public boolean getSpan(BioSeq bs, MutableSeqSpan span) {
		if (bs.equals(targetseq)) {
			if (same_orientation) { span.set(tmin, tmax, targetseq); }
			else { span.set(tmax, tmin, targetseq); }
			return true;
		}
		else if (bs.equals(queryseq)) {
			span.set(qmin, qmax, queryseq);
		}
		return false;
	}

	public boolean getSpan(int index, MutableSeqSpan span) {
		if (index == QUERY_INDEX) {
			span.set(qmin, qmax, queryseq);
		}
		else if (index == TARGET_INDEX) {
			if (same_orientation) { span.set(tmin, tmax, targetseq); }
			else { span.set(tmax, tmin, targetseq); }
		}
		return false;
	}

	public SeqSpan getSpan(int index) {
		SeqSpan span = null;
		if (index == QUERY_INDEX) {
			span = new SimpleSeqSpan(qmin, qmax, queryseq);
		}
		else if (index == TARGET_INDEX) {
			if (same_orientation) { span = new SimpleSeqSpan(tmin, tmax, targetseq); }
			else { span = new SimpleSeqSpan(tmax, tmin, targetseq); }
		}
		return span;
	}

	public BioSeq getSpanSeq(int index) {
		if (index == QUERY_INDEX) { return queryseq; }
		else if (index == TARGET_INDEX) { return targetseq; }
		return null;
	}

	public int getChildCount() { return blockSizes.length; }

	public SeqSymmetry getChild(int i) {
		if (same_orientation) {
			return new EfficientPairSeqSymmetry(qmins[i], qmins[i]+blockSizes[i], queryseq,
						tmins[i], tmins[i]+blockSizes[i], targetseq, getChildResidue(i));
		}
		else {
			return new EfficientPairSeqSymmetry(qmins[i], qmins[i]+blockSizes[i], queryseq, 
					tmins[i]+blockSizes[i], tmins[i], targetseq, getChildResidue(i));
		}
	}

	private String getChildResidue(int i){

		if(target_res_arr != null && target_res_arr.length > i){
			return target_res_arr[i];
		}

		return "";
	}

	// SearchableSeqSymmetry interface
	public List<SeqSymmetry> getOverlappingChildren(SeqSpan input_span) {
		final boolean debug = false;
		List<SeqSymmetry> results = null;
		if (input_span.getBioSeq() != this.getQuerySeq()) {
			results =  SeqUtils.getOverlappingChildren(this, input_span);
			if (debug) System.out.println("input span != query seq, doing normal SeqUtils.getOverlappingChildren() call");
		}
		else if (overlapping_query_coords) {
			results =  SeqUtils.getOverlappingChildren(this, input_span);
			// or maybe do a smarter binary search with constrained scan???
			if (debug) System.out.println("query children overlap, doing normal SeqUtils.getOverlappingChildren() call");
		}
		else {
			//      System.out.println("trying to do a binary search on qmins");
			// do a simple binary search on qmins to find first qmin with coord >= input_span.getMin()
			// then scan till qmin coord >= input_span.getMax();
			// collect all qmins in between and use as basis for creating syms...
			int input_min = input_span.getMin();
			int input_max = input_span.getMax();
			int beg_index = Arrays.binarySearch(qmins, input_min);
			if (debug) {
				System.out.println("map symmetry:");
				SeqUtils.printSymmetry(this);
			}
			if (debug) { System.out.println("initial beg_index: " + beg_index); }
			if (beg_index < 0)  { beg_index = -beg_index -1; }
			//      if (beg_index < 0)  { beg_index = (-beg_index -1) -1; }
			else {
				// backtrack beg_index in case hit duplicates???
				// well, only way this can happen when (! overlapping_query_coords) is when have weird
				//   entries where blockSize is zero...
				while ((beg_index > 0) && (qmins[beg_index-1] == qmins[beg_index])) { beg_index--; }
			}
			while ((beg_index > 0) && ((qmins[beg_index-1] + blockSizes[beg_index-1]) > input_min)) {
				beg_index--;
			}
			if (debug) {
				System.out.println("binary search, final beg_index: " + beg_index);
				System.out.println("binary search, child count: " + this.getChildCount());
			}

			if ((beg_index < qmins.length) && (qmins[beg_index] < input_max)) {
				results = new ArrayList<SeqSymmetry>();
				int index = beg_index;
				// now scan forward till qmin[index] >= input_max and collect list of symmetries
				while ((index < qmins.length) && (qmins[index] < input_max)) {
					results.add(this.getChild(index));
					index++;
				}
			}
		}
		return results;
	}

	public int getMatches() { return matches; }
	public void setMatches(int count) { matches = count; }
	public int getMisMatches() { return mismatches; }
	public int getRepMatches() { return repmatches; }
	public int getNCount() { return ncount; }
	public int getQueryNumInserts() { return qNumInsert; }
	public int getQueryBaseInserts() { return qBaseInsert; }
	public int getTargetNumInserts() { return tNumInsert; }
	public int getTargetBaseInserts() { return tBaseInsert; }
	public boolean getSameOrientation() { return same_orientation; }
	public BioSeq getQuerySeq() { return queryseq; }
	public int getQueryMin() { return qmin; }
	public int getQueryMax() { return qmax; }
	public BioSeq getTargetSeq() { return targetseq; }
	public int getTargetMin() { return tmin; }
	public int getTargetMax() { return tmax; }

	public Map<String,Object> getProperties() {
		return cloneProperties();
	}

	public Map<String,Object> cloneProperties() {
		HashMap<String,Object> tprops = new HashMap<String,Object>();

		tprops.put("id", getQuerySeq().getID());
		tprops.put("type", "Pairwise Alignment");
		tprops.put("same orientation", getSameOrientation());
		tprops.put("# matches", Integer.valueOf(getMatches()));
		tprops.put("query length", Integer.valueOf(queryseq.getLength()));
		tprops.put("# query inserts", Integer.valueOf(getQueryNumInserts()));
		tprops.put("# query bases inserted", Integer.valueOf(getQueryBaseInserts()));
		tprops.put("# target inserts", Integer.valueOf(getTargetNumInserts()));
		tprops.put("# target bases inserted", Integer.valueOf(getTargetBaseInserts()));
		//tprops.put("query seq", getQuerySeq().getID());
		//tprops.put("target seq", getTargetSeq().getID());
		if (props != null) {
			tprops.putAll(props);
		}

		if(target_res_arr != null){
			tprops.put("residues", getResidue(target_res_arr));
		}
		
		return tprops;
	}

	public Object getProperty(String key) {
		if (key.equals("id")) {  return getQuerySeq().getID(); }
		else if (key.equals("method")) {  return getType(); }
		else if (key.equals("type")) {  return "Pairwise Alignment"; }
		else if (key.equals("same orientation")) { return getSameOrientation()?"true":"false"; }
		else if (key.equals("# matches")) { return Integer.toString(getMatches()); }
		else if (key.equals("query length")) { return Integer.toString(queryseq.getLength()); }
		else if (key.equals("# query inserts")) { return Integer.toString(getQueryNumInserts()); }
		else if (key.equals("# query bases inserted")) { return Integer.toString(getQueryBaseInserts()); }
		else if (key.equals("# target inserts")) { return Integer.toString(getTargetNumInserts()); }
		else if (key.equals("# target bases inserted")) { return Integer.toString(getTargetBaseInserts()); }
		else if (key.equals("residues") && target_res_arr != null) { return getResidue(target_res_arr); }
		//else if (key.equals("query seq")) { return getQuerySeq().getID(); }
		//else if (key.equals("target seq")) { return  getTargetSeq().getID(); }

		// then try to match with any extras
		else if (props != null) {
			return props.get(key);
		}
		else { return null; }
	}

	public boolean setProperty(String name, Object val) {
		if (props == null) {
			props = new Hashtable<String,Object>();
		}
		props.put(name, val);
		return true;
	}

	/**
	 *  Writes a line of PSL to a writer, including property tag values.
	 */
	public final void outputPslFormat(DataOutputStream out) throws IOException  {
		outputStandardPsl(out, false);
		outputPropTagVals(out);
		out.write('\n');
	}

	/**
	 *  Writes a line of PSL to a writer, NOT including property tag values.
	 *  @param include_newline  whether to add a newline at the end.
	 */
	protected void outputStandardPsl(DataOutputStream out, boolean include_newline)  throws IOException {
		out.write(Integer.toString(matches).getBytes());
		out.write('\t');
		out.write(Integer.toString(mismatches).getBytes());
		out.write('\t');
		out.write(Integer.toString(repmatches).getBytes());
		out.write('\t');
		out.write(Integer.toString(ncount).getBytes());
		out.write('\t');
		out.write(Integer.toString(qNumInsert).getBytes());
		out.write('\t');
		out.write(Integer.toString(qBaseInsert).getBytes());
		out.write('\t');
		out.write(Integer.toString(tNumInsert).getBytes());
		out.write('\t');
		out.write(Integer.toString(tBaseInsert).getBytes());
		out.write('\t');
		if (same_orientation) { out.write('+'); }
		else { out.write('-'); }
		out.write('\t');
		out.write(queryseq.getID().getBytes());
		out.write('\t');
		out.write(Integer.toString(queryseq.getLength()).getBytes());
		out.write('\t');
		out.write(Integer.toString(qmin).getBytes());
		out.write('\t');
		out.write(Integer.toString(qmax).getBytes());
		out.write('\t');
		out.write(targetseq.getID().getBytes());
		out.write('\t');
		out.write(Integer.toString(targetseq.getLength()).getBytes());
		out.write('\t');
		out.write(Integer.toString(tmin).getBytes());
		out.write('\t');
		out.write(Integer.toString(tmax).getBytes());
		out.write('\t');
		int blockcount = this.getChildCount();
		out.write(Integer.toString(blockcount).getBytes());
		out.write('\t');
		for (int i=0; i<blockcount; i++) {
			out.write(Integer.toString(blockSizes[i]).getBytes());
			out.write(',');
		}
		out.write('\t');

		for (int i=0; i<blockcount; i++) {
			if (same_orientation) {
				out.write(Integer.toString(qmins[i]).getBytes());
			}
			else {
				// dealing with reverse issue
				int mod_qmin = queryseq.getLength() - qmins[i] - blockSizes[i];
				out.write(Integer.toString(mod_qmin).getBytes());
			}
			out.write(',');
		}
		out.write('\t');
		for (int i=0; i<blockcount; i++) {
			out.write(Integer.toString(tmins[i]).getBytes());
			out.write(',');
		}

//		if (target_res_arr != null) {
//			out.write('\t');	//Tab for source residue array. No need to write it ???
//			out.write('\t');
//
//			for (int i=0; i<target_res_arr.length; i++) {
//				out.write(target_res_arr[i].getBytes());
//				out.write(',');
//			}
//		}
		
		if (include_newline) {
			out.write('\n');
		}
	}

	protected void outputPropTagVals(DataOutputStream out)  throws IOException {
		if (props != null) {
			Iterator<Map.Entry<String,Object>> iter = props.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String,Object> entry = iter.next();
				out.write((entry.getKey()).getBytes());
				out.write('=');
				out.write(entry.getValue().toString().getBytes());
				out.write('\t');
			}
		}
	}

	public final void outputBpsFormat(DataOutputStream dos) throws IOException  {
		dos.writeInt(matches);
		dos.writeInt(mismatches);
		dos.writeInt(repmatches);
		dos.writeInt(ncount);
		dos.writeInt(qNumInsert);
		dos.writeInt(qBaseInsert);
		dos.writeInt(tNumInsert);
		dos.writeInt(tBaseInsert);
		dos.writeBoolean(same_orientation);
		dos.writeUTF(queryseq.getID());
		dos.writeInt(queryseq.getLength());
		dos.writeInt(qmin);
		dos.writeInt(qmax);
		dos.writeUTF(targetseq.getID());
		dos.writeInt(targetseq.getLength());
		dos.writeInt(tmin);
		dos.writeInt(tmax);
		int blockcount = this.getChildCount();
		dos.writeInt(blockcount);
		for (int i=0; i<blockcount; i++) {
			dos.writeInt(blockSizes[i]);
		}
		for (int i=0; i<blockcount; i++) {
			dos.writeInt(qmins[i]);
		}
		for (int i=0; i<blockcount; i++) {
			dos.writeInt(tmins[i]);
		}
	}
}
