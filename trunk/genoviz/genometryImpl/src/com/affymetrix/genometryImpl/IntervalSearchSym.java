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

import com.affymetrix.genometryImpl.comparator.SeqSymMinComparator;
import java.util.*;

import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;

/**
 *  A symmetry that supports efficient retrieval of a subset of child
 *  symmetries that overlap a query interval on a BioSeq.
 *  Uses a binary search with bounded range scan algorithm
 *    (which for brevity I'm calling "interval array", since it's
 *      relationship to interval trees is somewhat analogous to
 *      the relationship between suffix arrays and suffix trees.
 *      thanks to Antonio for the suggesting the name...)
 */
public final class IntervalSearchSym extends SimpleSymWithProps
		implements SearchableSeqSymmetry {

	private static final boolean DEBUG = false;
	private boolean ready_for_searching = false;
	private BioSeq search_seq;
	//   for each entry in min sorted children list, the maximum max
	//     value up to (and including) that position
	//   currently using list of symmetries (max_sym_sofar),
	//   but could switch to array of ints (max_int_sofar)
	private List<SeqSymmetry> max_sym_sofar = null;
	private SeqSymMinComparator comp = null;

	/**
	 *  Constructor takes BioSeq argument for which
	 *  seq to optimize search for.
	 */
	public IntervalSearchSym(BioSeq seq) {
		this.search_seq = seq;
	}

	/**
	 *  Copies children of sym_to_copy into new IntervalSearchSym.
	 */
	public IntervalSearchSym(BioSeq seq, SeqSymmetry sym_to_copy) {
		this(seq);
		if (sym_to_copy instanceof SymWithProps) {
			this.setProperties(((SymWithProps) sym_to_copy).getProperties());
		}
		int child_count = sym_to_copy.getChildCount();
		for (int i = 0; i < child_count; i++) {
			addChild(sym_to_copy.getChild(i));
		}
	}

	/** Tells whether {@link #initForSearching(BioSeq)} has been run yet. */
	public boolean getOptimizedForSearch() {
		return ready_for_searching;
	}

	@Override
	public void addChild(SeqSymmetry child) {
		super.addChild(child);
		ready_for_searching = false;
	}

	public void initForSearching(BioSeq seq) {
		search_seq = seq;
		comp = new SeqSymMinComparator(search_seq);

		// make sure child symmetries are sorted by ascending min along search_seq
		// to avoid unecessary sort, first go through child list and see if it's
		//     already in ascending order -- if so, then no need to sort.
		//     (Calling Collections.sort() always results in making a temporary copy
		//      of an array, so best to avoid that when we can.)
		int child_count = this.getChildCount();
		boolean sorted = true;
		int prev_min = Integer.MIN_VALUE;
		for (int i = 0; i < child_count; i++) {
			SeqSymmetry child = getChild(i);
			if (child == null || child.getSpan(search_seq) == null) {
				continue;
			}
			int min = child.getSpan(search_seq).getMin();
			if (prev_min > min) {
				sorted = false;
				break;
			}
			prev_min = min;
		}
		if (!sorted) {
			if (DEBUG) {
				System.out.println("sorting sym children of IntervalSearchSym, seq = "
						+ search_seq.getID());
			}
			Collections.sort(this.getChildren(), comp);
			sorted = true;
		}

		determineMaxSymList(child_count);

		ready_for_searching = true;
	}

	private void determineMaxSymList(int child_count) {
		// as symmetries
		max_sym_sofar = new ArrayList<SeqSymmetry>(child_count);
		SeqSymmetry curMaxSym = this.getChild(0);
		for (int i = 0; i < child_count; i++) {
			SeqSymmetry child = this.getChild(i);
			if (child == null || child.getSpan(search_seq) == null) {
				continue;
			}
			int max = child.getSpan(search_seq).getMax();
			if (max > curMaxSym.getSpan(search_seq).getMax()) {
				curMaxSym = child;
			}
			max_sym_sofar.add(curMaxSym);
		}
	}

	/**
	 *  Gets overlapping children.
	 *  If there are no children that overlap/intersect qinterval,
	 *    the getOverlappingChildren() will return null.
	 */
	public List<SeqSymmetry> getOverlappingChildren(SeqSpan qinterval) {
		int child_count = getChildCount();
		if (child_count <= 0) {
			return null;
		}
		if (qinterval.getBioSeq() != search_seq) {
			ready_for_searching = false;
		}
		search_seq = qinterval.getBioSeq();
		int search_min = qinterval.getMin();
		int search_max = qinterval.getMax();
		if (DEBUG) {
			System.out.println("searching with interval: seqid = " + search_seq.getID()
					+ ", min = " + search_min
					+ ", max = " + search_max);
		}

		if (!ready_for_searching) {
			initForSearching(search_seq);
		}
		if (DEBUG) {
			System.out.println("done initing");
			System.out.println("IntervalSearchSym child count: " + child_count);
		}
		SeqSymmetry query_sym =
				new SingletonSeqSymmetry(search_min, search_max, search_seq);
		int beg_index = Collections.binarySearch(children, query_sym, comp);
		if (beg_index < 0) {
			beg_index = -beg_index - 1;
		}
		if (DEBUG) {
			System.out.println("done with binary search, beg_index = " + beg_index);
		}

		if (beg_index >= child_count) {
			beg_index = child_count - 1;
		}

		int cur_min = getChild(beg_index).getSpan(search_seq).getMin();
		beg_index = minBackTrack(beg_index, cur_min);
		int backtrack_max_index = maxBacktrack(beg_index, search_min);

		// overlap check required up to beg_index
		// and should have for just (min < search_max) search:
		//    iterate through children from beg_index+1 till first one with min >= max
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>(1000);
		checkBackwards(backtrack_max_index, beg_index, search_min, search_max, results);
		checkForwards(beg_index, child_count, search_max, results);

		return results;
	}

	private int minBackTrack(int beg_index, int cur_min) {
		// overlap check required up to beg_index
		// backtrack if previous mins are equivalent to this min,
		//   since binarySearch is not guaranteed to return lowest index of
		//   equal mins
		while (beg_index > 0) {
			int back_min = getChild(beg_index - 1).getSpan(search_seq).getMin();
			if (back_min == cur_min) {
				beg_index--;
			} else {
				break;
			}
		}
		if (DEBUG) {
			System.out.println("done with min backtracking, beg_index = " + beg_index);
		}
		return beg_index;
	}

	private int maxBacktrack(int beg_index, int search_min) {
		int backtrack_max_index = beg_index;
		while (backtrack_max_index > 0) {
			backtrack_max_index--;
			SeqSymmetry back_sym = max_sym_sofar.get(backtrack_max_index);
			if (back_sym.getSpan(search_seq).getMax() < search_min) {
				backtrack_max_index++;
				break;
			}
		}
		if (DEBUG) {
			System.out.println("done with max backtracking, backtrack_max_index = " + backtrack_max_index);
		} // and should have for just (min < search_max) search:
		//    iterate through children from beg_index+1 till first one with min >= max
		return backtrack_max_index;
	}

	private void checkBackwards(int backtrack_max_index, int beg_index, int search_min, int search_max, List<SeqSymmetry> results) {
		// just picking somewhat arbitrary size of 1000 for initial list, so that for most queries,
		//    (assuming most queries don't return > 1000 children) won't have to grow list's internal array
		for (int i = backtrack_max_index; i <= beg_index; i++) {
			SeqSymmetry sym = children.get(i);
			SeqSpan span = sym.getSpan(search_seq);
			if (span.getMax() > search_min && span.getMin() < search_max) {
				results.add(sym);
			}
		}
		if (DEBUG) {
			System.out.println("done with checking backwards, count = " + results.size());
		}
	}

	private void checkForwards(int beg_index, int child_count, int search_max, List<SeqSymmetry> results) {
		int cur_index = beg_index + 1;
		while (cur_index < child_count) {
			SeqSymmetry sym = children.get(cur_index);
			SeqSpan span = sym.getSpan(search_seq);
			if (DEBUG) {
				System.out.println(SeqUtils.getOtherSeq(sym, search_seq).getID() + ":" + SeqUtils.spanToString(span));
			}
			if (span.getMin() >= search_max) {
				break;
			}
			results.add(sym);
			cur_index++;
		}
		if (DEBUG) {
			System.out.println("done with checking forwards, count = " + results.size());
		}
	}
}
