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

import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import java.util.*;

import com.affymetrix.genometryImpl.util.SeqUtils;

public final class SeqSymSummarizer {

	public static GraphIntervalSym getSymmetrySummary(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id, Boolean isForward)  {
		int symcount = syms.size();
		List<SeqSpan> leaf_spans = new ArrayList<SeqSpan>(symcount);
		for (SeqSymmetry sym : syms) {
			SeqUtils.collectLeafSpans(sym, seq, isForward, leaf_spans);
		}
		if (leaf_spans.isEmpty()) {
			return null;
		} else {
			return getSpanSummary(leaf_spans, binary_depth, id);
		}
	}

	/**
	 *  Makes a summary graph of a set the spans of some SeqSymmetries on a given BioSeq.
	 *  Descends into parent's descendants, collecting all leaf symmetries and
	 *    creating a summary over the leafs.
	 *  Currently assumes that spans are integral.
	 *<pre>
	 *  Performance: ~ n log(n) ?   where n is number of spans in the syms
	 *      a.) collect leaf spans, ~linear scan (n)
	 *      b.) sort span starts and ends, ~(n)(log(n))
	 *      c.) get transitions, linear scan (n)
	 *</pre>
	 *  @param syms a List of SeqSymmetry's
	 *  @param seq the sequence you want the summary computed for
	 *  @param binary_depth passed through to {@link #getSpanSummary(List, boolean, String)}
	 */
	public static GraphIntervalSym getSymmetrySummary(List<SeqSymmetry> syms, BioSeq seq, boolean binary_depth, String id)  {
		int symcount = syms.size();
		List<SeqSpan> leaf_spans = new ArrayList<SeqSpan>(symcount);
		for (SeqSymmetry sym : syms) {
			SeqUtils.collectLeafSpans(sym, seq, leaf_spans);
		}
		if (leaf_spans.isEmpty()) {
			return null;
		} else {
			return getSpanSummary(leaf_spans, binary_depth, id);
		}
	}


	/**
	 *  GetSpanSummary.
	 *  General idea is that this will make getUnion(), getIntersection(), etc. easier and
	 *       more efficient.
	 *  @param spans a List of SeqSpan's all defined on the same BioSeq
	 *  @param binary_depth if false, then return a graph with full depth information
	 *                  if true, then return a graph with flattened / binary depth information,
	 *                  1 for covered, 0 for not covered
	 */
	private static GraphIntervalSym getSpanSummary(List<SeqSpan> spans, boolean binary_depth, String gid) {
		BioSeq seq = spans.get(0).getBioSeq();
		int span_num = spans.size();
		int[] starts = new int[span_num];
		int[] stops = new int[span_num];
		for (int i=0; i<span_num; i++) {
			SeqSpan span = spans.get(i);
			starts[i] = span.getMin();
			stops[i] = span.getMax();
		}
		Arrays.sort(starts);
		Arrays.sort(stops);
		int starts_index = 0;
		int stops_index = 0;
		int depth = 0;
		int max_depth = 0;
		// initializing capacity of sum_starts and sum_stops to max that could theoretically be
		//   needed, though likely won't fill it
		IntArrayList transition_xpos = new IntArrayList(span_num * 2);
		FloatArrayList transition_ypos = new FloatArrayList(span_num * 2);

		int prev_depth = 0;
		while ((starts_index < span_num) && (stops_index < span_num)) {
			// figure out whether next position is a start, stop, or both
			int next_start = starts[starts_index];
			int next_stop = stops[stops_index];
			int next_transition = Math.min(next_start, next_stop);
			// note that by design, if (next_start == next_stop), then both of the following
			//    conditionals will execute:
			if (next_start <= next_stop) {
				while ((starts_index < span_num) && (starts[starts_index] == next_start)) {
					depth++;
					starts_index++;
				}
			}
			if (next_start >= next_stop) {
				while ((stops_index < span_num) && (stops[stops_index] == next_stop)) {
					depth--;
					stops_index++;
				}
			}
			if (binary_depth) {
				if ((prev_depth <= 0) && (depth > 0)) {
					transition_xpos.add(next_transition);
					transition_ypos.add(1);
					prev_depth = 1;
				}
				else if ((prev_depth > 0) && (depth <= 0)) {
					transition_xpos.add(next_transition);
					transition_ypos.add(0);
					prev_depth = 0;
				}
			}
			else {
				transition_xpos.add(next_transition);
				transition_ypos.add(depth);
				max_depth = Math.max(depth, max_depth);
			}
		}
		// clean up last stops...
		//    don't need to worry about "last starts", all starts will be done before last stop...
		while (stops_index < span_num) {
			int next_stop = stops[stops_index];
			int next_transition = next_stop;
			while ((stops_index < span_num) && (stops[stops_index] == next_stop)) {
				depth--;
				stops_index++;
			}
			if (binary_depth) {
				if ((prev_depth <= 0) && (depth > 0)) {
					transition_xpos.add(next_transition);
					transition_ypos.add(1);
					prev_depth = 1;
				}
				else if ((prev_depth > 0) && (depth <= 0)) {
					transition_xpos.add(next_transition);
					transition_ypos.add(0);
					prev_depth = 0;
				}
			}
			else {
				transition_xpos.add(next_transition);
				transition_ypos.add(depth);
				max_depth = Math.max(depth, max_depth);
			}
		}
		transition_xpos.trimToSize();
		transition_ypos.trimToSize();
		int[] x_positions = transition_xpos.elements();
		int[] widths = new int[x_positions.length];
		for (int i=0; i<widths.length-1; i++) {
			widths[i] = x_positions[i+1] - x_positions[i];
		}
		widths[widths.length-1] = 1;

		// Originally, this returned a GraphSym with just x and y, but now has widths.
		// Since the x and y values are not changed, all old code that relies on them
		// does not need to change.
		String uid = AnnotatedSeqGroup.getUniqueGraphID(gid, seq);
		GraphIntervalSym gsym =
			new GraphIntervalSym(x_positions, widths, transition_ypos.elements(), uid, seq);
		return gsym;
	}


	/**
	 *  Assumes all spans refer to same BioSeq
	 */
	public static List<SeqSpan> getMergedSpans(List<SeqSpan> spans) {
		GraphSym landscape = getSpanSummary(spans, true, "");
		return projectLandscapeSpans(landscape);
	}

	private static List<SeqSpan> projectLandscapeSpans(GraphSym landscape) {
		List<SeqSpan> spanlist = new ArrayList<SeqSpan>();
		BioSeq seq = landscape.getGraphSeq();
		int num_points = landscape.getPointCount();

		int current_region_start = 0;
		int current_region_end = 0;
		boolean in_region = false;
		for (int i=0; i<num_points; i++) {
			int xpos = landscape.getGraphXCoord(i);
			float ypos = landscape.getGraphYCoord(i);
			if (in_region) {
				if (ypos <= 0) { // reached end of region, make SeqSpan
					in_region = false;
					current_region_end = xpos;
					SeqSpan newspan = new SimpleSeqSpan(current_region_start, current_region_end, seq);
					spanlist.add(newspan);
				}
			} else {  // not already in_region
				if (ypos > 0) {
					in_region = true;
					current_region_start = xpos;
				}
			}
		}
		if (in_region) {  // last point was still in_region, so make a span to end?
			// pretty sure this won't happen, based on how getSymmetrySummary()/getSpanSummary() work
			System.err.println("still in a covered region at end of projectLandscapeSpans() loop!");
		}
		return spanlist;
	}


	private static SymWithProps projectLandscape(GraphSym landscape) {
		BioSeq seq = landscape.getGraphSeq();
		SimpleSymWithProps psym = new SimpleSymWithProps();
		int num_points = landscape.getPointCount();

		int current_region_start = 0;
		int current_region_end = 0;
		boolean in_region = false;
		for (int i=0; i<num_points; i++) {
			int xpos = landscape.getGraphXCoord(i);
			float ypos = landscape.getGraphYCoord(i);
			if (in_region) {
				if (ypos <= 0) { // reached end of region, make SeqSpan
					in_region = false;
					current_region_end = xpos;
					SeqSymmetry newsym =
						new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
					psym.addChild(newsym);
				}
			}
			else {  // not already in_region
				if (ypos > 0) {
					in_region = true;
					current_region_start = xpos;
				}
			}
		}
		if (in_region) {  // last point was still in_region, so make a span to end?
			// pretty sure this won't happen, based on how getSymmetrySummary()/getSpanSummary() work
			System.err.println("still in a covered region at end of projectLandscape() loop!");
		}

		if (psym.getChildCount() <= 0) {
			psym = null;
		}
		else {
			// landscape is already sorted, so should be able to derive parent min and max
			int pmin = psym.getChild(0).getSpan(seq).getMin();
			int pmax = psym.getChild(psym.getChildCount()-1).getSpan(seq).getMax();
			SeqSpan pspan = new SimpleSeqSpan(pmin, pmax, seq);
			psym.addSpan(pspan);
		}
		return psym;
	}

	/**
	 *  Finds the Union of a List of SeqSymmetries.
	 *  This will merge not only overlapping syms but also abutting syms (where symA.getMax() == symB.getMin())
	 */
	public static SeqSymmetry getUnion(List<SeqSymmetry> syms, BioSeq seq)  {
		//    MutableSeqSymmetry psym = new SimpleSymWithProps();
		// first get the landscape as a GraphSym
		GraphSym landscape = getSymmetrySummary(syms, seq, true, "");
		// now just flatten it
		if (landscape != null) {
			return projectLandscape(landscape);
		} else {
			return null;
		}
	}



	/**
	 *  Finds the Intersection of a List of SeqSymmetries.
	 */
	public static SeqSymmetry getIntersection(List<SeqSymmetry> symsA, List<SeqSymmetry> symsB, BioSeq seq)  {
		MutableSeqSymmetry psym = new SimpleSymWithProps();
		SeqSymmetry unionA = getUnion(symsA, seq);
		SeqSymmetry unionB = getUnion(symsB, seq);
		List<SeqSymmetry> symsAB = new ArrayList<SeqSymmetry>();
		symsAB.add(unionA);
		symsAB.add(unionB);
		GraphSym combo_graph = getSymmetrySummary(symsAB, seq, false, "");
		// combo_graph should now be landscape where:
		//    no coverage ==> depth = 0;
		//    A not B     ==> depth = 1;
		//    B not A     ==> depth = 1;
		//    A && B      ==> depth = 2;

		// so any regions with depth == 2 are intersection
		int num_points = combo_graph.getPointCount();

		int current_region_start = 0;
		int current_region_end = 0;
		boolean in_region = false;
		for (int i=0; i<num_points; i++) {
			int xpos = combo_graph.getGraphXCoord(i);
			float ypos = combo_graph.getGraphYCoord(i);
			if (in_region) {
				if (ypos < 2) { // reached end of intersection region, make SeqSpan
					in_region = false;
					current_region_end = xpos;
					SeqSymmetry newsym =
						new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
					psym.addChild(newsym);
				}
			}
			else {  // not already in_region
				if (ypos >= 2) {
					in_region = true;
					current_region_start = xpos;
				}
			}
		}
		if (in_region) {  // last point was still in_region, so make a span to end?
			// pretty sure this won't happen, based on how getSymmetrySummary()/getSpanSummary() work
			System.err.println("still in a covered region at end of getUnion() loop!");
		}

		if (psym.getChildCount() <= 0) {
			psym = null;
		}
		else {
			// landscape is already sorted, so should be able to derive parent min and max
			int pmin = psym.getChild(0).getSpan(seq).getMin();
			int pmax = psym.getChild(psym.getChildCount()-1).getSpan(seq).getMax();
			SeqSpan pspan = new SimpleSeqSpan(pmin, pmax, seq);
			psym.addSpan(pspan);
		}
		return psym;
	}

	public static SeqSymmetry getXor(List<SeqSymmetry> symsA, List<SeqSymmetry> symsB, BioSeq seq) {
		MutableSeqSymmetry psym = new SimpleSymWithProps();
		SeqSymmetry unionA = getUnion(symsA, seq);
		SeqSymmetry unionB = getUnion(symsB, seq);
		List<SeqSymmetry> symsAB = new ArrayList<SeqSymmetry>();
		symsAB.add(unionA);
		symsAB.add(unionB);
		GraphSym combo_graph = getSymmetrySummary(symsAB, seq, false, "");
		// combo_graph should now be landscape where:
		//    no coverage ==> depth = 0;
		//    A not B     ==> depth = 1;
		//    B not A     ==> depth = 1;
		//    A && B      ==> depth = 2;

		// so any regions with depth == 1 are XOR regions
		int num_points = combo_graph.getPointCount();

		int current_region_start = 0;
		int current_region_end = 0;
		boolean in_region = false;
		for (int i=0; i<num_points; i++) {
			int xpos = combo_graph.getGraphXCoord(i);
			float ypos = combo_graph.getGraphYCoord(i);
			if (in_region) {
				if (ypos < 1 || ypos > 1) { // reached end of xor region, make SeqSpan
					in_region = false;
					current_region_end = xpos;
					SeqSymmetry newsym =
						new SingletonSeqSymmetry(current_region_start, current_region_end, seq);
					psym.addChild(newsym);
				}
			}
			else {  // not already in_region
				if (ypos == 1) {
					in_region = true;
					current_region_start = xpos;
				}
			}
		}
		if (in_region) {  // last point was still in_region, so make a span to end?
			// pretty sure this won't happen, based on how getSymmetrySummary()/getSpanSummary() work
			System.err.println("still in a covered region at end of getUnion() loop!");
		}

		if (psym.getChildCount() <= 0) {
			psym = null;
		}
		else {
			// landscape is already sorted, so should be able to derive parent min and max
			int pmin = psym.getChild(0).getSpan(seq).getMin();
			int pmax = psym.getChild(psym.getChildCount()-1).getSpan(seq).getMax();
			SeqSpan pspan = new SimpleSeqSpan(pmin, pmax, seq);
			psym.addSpan(pspan);
		}
		return psym;
	}

	/**
	 *  Like a one-sided xor,
	 *  creates a SeqSymmetry that contains children for regions covered by syms in symsA that
	 *     are not covered by syms in symsB.
	 */
	public static SeqSymmetry getExclusive(List<SeqSymmetry> symsA, List<SeqSymmetry> symsB, BioSeq seq) {
		SeqSymmetry xorSym = getXor(symsA, symsB, seq);
		//  if no spans for xor, then won't be any for one-sided xor either, so return null;
		if (xorSym == null)  { return null; }
		List<SeqSymmetry> xorList = new ArrayList<SeqSymmetry>();
		xorList.add(xorSym);
		SeqSymmetry a_not_b = getIntersection(symsA, xorList, seq);
		return a_not_b;
	}

	public static SeqSymmetry getNot(List<SeqSymmetry> syms, BioSeq seq) {
		return getNot(syms, seq, true);
	}

	private static SeqSymmetry getNot(List<SeqSymmetry> syms, BioSeq seq, boolean include_ends) {
		SeqSymmetry union = getUnion(syms, seq);
		int spanCount = union.getChildCount();

		// rest of this is pretty much pulled directly from SeqUtils.inverse()
		if (! include_ends )  {
			if (spanCount <= 1) {  return null; }  // no gaps, no resulting inversion
		}
		MutableSeqSymmetry invertedSym = new SimpleSymWithProps();
		if (include_ends) {
			if (spanCount < 1) {
				// no spans, so just return sym of whole range of seq
				invertedSym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
				return invertedSym;
			}
			else {
				SeqSpan firstSpan = union.getChild(0).getSpan(seq);
				if (firstSpan.getMin() > 0) {
					SeqSymmetry beforeSym = new SingletonSeqSymmetry(0, firstSpan.getMin(), seq);
					invertedSym.addChild(beforeSym);
				}
			}
		}
		for (int i=0; i<spanCount-1; i++) {
			SeqSpan preSpan = union.getChild(i).getSpan(seq);
			SeqSpan postSpan = union.getChild(i+1).getSpan(seq);
			SeqSymmetry gapSym =
				new SingletonSeqSymmetry(preSpan.getMax(), postSpan.getMin(), seq);
			invertedSym.addChild(gapSym);
		}
		if (include_ends) {
			SeqSpan lastSpan = union.getChild(spanCount-1).getSpan(seq);
			if (lastSpan.getMax() < seq.getLength()) {
				SeqSymmetry afterSym = new SingletonSeqSymmetry(lastSpan.getMax(), seq.getLength(), seq);
				invertedSym.addChild(afterSym);
			}
		}
		if (include_ends) {
			invertedSym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
		}
		else {
			int min = union.getChild(0).getSpan(seq).getMax();
			int max = union.getChild(spanCount-1).getSpan(seq).getMin();
			invertedSym.addSpan(new SimpleSeqSpan(min, max, seq));
		}
		return invertedSym;
	}

}
