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
import java.util.*;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.ITrackStyle;

/**
 *  A SeqSymmetry that can only accept children that are instances of
 *  {@link IndexedSym}.
 *  Assumes that ScoredContainerSym has only one SeqSpan
 */
public class ScoredContainerSym extends SimpleSymWithProps {
	private final Map<String,Object> name2scores = new HashMap<String,Object>();
	private final List<Object> scorevals = new ArrayList<Object>();
	private final List<String> scorenames = new ArrayList<String>();

	private static Map<String,GraphState> id2gstate = new HashMap<String,GraphState>();

	private static Map<String,ITrackStyle> id2combo_style_plus = new HashMap<String,ITrackStyle>();
	private static Map<String,ITrackStyle> id2combo_style_minus = new HashMap<String,ITrackStyle>();
	private static Map<String,ITrackStyle> id2combo_style_neutral = new HashMap<String,ITrackStyle>();

	/**
	 *  Adds scores.
	 *  Assumes all child syms have already been added, and span has already been set.
	 *  @param scores  a float array with the same length as the number of children.
	 */
	public void addScores(String name, float[] scores) {
		name2scores.put(name, scores);
		scorevals.add(scores);
		scorenames.add(name);
	}

	public int getScoreCount() { return scorevals.size(); }

	public float[] getScores(String name) {
		return (float[])name2scores.get(name);
	}

	public float[] getScores(int index) {
		return (float[])scorevals.get(index);
	}

	public String getScoreName(int index)  {
		return scorenames.get(index);
	}

	public float[] getChildScores(IndexedSym child, List scorelist) {
		float[] result = null;
		if (child.getParent() == this) {
			int score_index = child.getIndex();  // position in each score array for score for this child
			int scores_count = scorelist.size();
			result = new float[scores_count];
			for (int i=0; i<scores_count; i++) {
				float[] scores = (float[])scorelist.get(i);
				result[i] = scores[score_index];
			}
		}
		return result;
	}

	public float[] getChildScores(IndexedSym child) {
		return getChildScores(child, scorevals);
	}

	/**
	 *  Can only accept children that are instances of IndexedSym.
	 */
	@Override
	public void addChild(SeqSymmetry sym) {
		if (sym instanceof IndexedSym) {
			IndexedSym isym = (IndexedSym)sym;
			// calling super.getChildCount() instead of this.getChildCount()
			//   to avoid cyclic calls in LazyChpSym subclass
			int current_index = super.getChildCount();
			isym.setIndex(current_index);
			isym.setParent(this);
			super.addChild(isym);
		}
		else {
			System.err.println("ERROR: cannot add a child to ScoredContainerSym unless it is an IndexedSym");
		}
	}


	/**
	 *  Creates a GraphIntervalSym.
	 *  Assumes all child syms have already been added, and span has already been set.
	 *  Resultant graph sym has x, width, and y data points for each child sym.
	 *
	 *  The returned GraphSym will have this property set by default:
	 *  <ol>
	 *  <li>GraphSym.PROP_GRAPH_STRAND will be the correct strand Integer.
	 *  </ol>
	 *
	 * @return a GraphSym or null if there was an error condition
	 */
	public final GraphIntervalSym makeGraphSym(String name, AnnotatedSeqGroup seq_group)  {
		float[] scores = getScores(name);
		SeqSpan pspan = this.getSpan(0);
		if (scores == null) {
			System.err.println("ScoreContainerSym.makeGraphSym() called, but no scores found for: " + name);
			return null;
		}
		if (pspan == null) {
			System.err.println("ScoreContainerSym.makeGraphSym() called, but has no span yet");
			return null;
		}
		BioSeq aseq = pspan.getBioSeq();
		int score_count = scores.length;
		int[] xcoords = new int[score_count];
		int[] wcoords = new int[score_count];
		float[] ycoords = new float[score_count];
		for (int i=0; i<score_count; i++) {
			IndexedSym isym = (IndexedSym) this.getChild(i);
			if (isym.getIndex() != i) {
				System.err.println("problem in ScoredContainerSym.makeGraphSym(), " +
						"child.getIndex() not same as child's index in parent child list: " +
						isym.getIndex() + ", " + i);
			}
			SeqSpan cspan = isym.getSpan(aseq);
			xcoords[i] = cspan.getMin();
			wcoords[i] = cspan.getLength();
			ycoords[i] = scores[i];
		}

		if (xcoords.length == 0) {
			return null;
		}

		String id = getGraphID(seq_group, name, '.');
		GraphIntervalSym gsym = new GraphIntervalSym(xcoords, wcoords, ycoords, id, aseq);
		gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_BOTH);
		return gsym;
	}

	/**
	 *  Make a GraphSym, but only with scores for scored intervals in the specified orientation.
	 *  @param orientation  true for forward strand intervals.
	 *  @see #makeGraphSym(String,AnnotatedSeqGroup)
	 */
	public final GraphIntervalSym makeGraphSym(String name, boolean orientation, AnnotatedSeqGroup seq_group) {
		float[] scores = getScores(name);
		SeqSpan pspan = this.getSpan(0);
		if (scores == null) {
			System.err.println("ScoreContainerSym.makeGraphSym() called, but no scores found for: " + name);
			return null;
		}
		if (pspan == null) {
			System.err.println("ScoreContainerSym.makeGraphSym() called, but has no span yet");
			return null;
		}
		BioSeq aseq = pspan.getBioSeq();
		int score_count = scores.length;

		IntArrayList xlist = new IntArrayList(score_count);
		IntArrayList wlist = new IntArrayList(score_count);
		FloatArrayList ylist = new FloatArrayList(score_count);
		for (int i=0; i<score_count; i++) {
			IndexedSym isym = (IndexedSym)this.getChild(i);
			if (isym.getIndex() != i) {
				System.err.println("problem in ScoredContainerSym.makeGraphSym(), " +
						"child.getIndex() not same as child's index in parent child list: " +
						isym.getIndex() + ", " + i);
			}
			SeqSpan cspan = isym.getSpan(aseq);
			if (cspan.isForward() == orientation) {
				xlist.add(cspan.getMin());
				wlist.add(cspan.getLength());
				ylist.add(scores[i]);
			}
		}

		if (xlist.size() == 0) {
			return null;
		}
		String id;
		if (orientation) {
			id = getGraphID(seq_group, name, '+');
		} else {
			id = getGraphID(seq_group, name, '-');
		}
		int[] xcoords = Arrays.copyOf(xlist.elements(), xlist.size());
		xlist = null;
		int[] wcoords = Arrays.copyOf(wlist.elements(), wlist.size());
		wlist = null;
		float[] ycoords = Arrays.copyOf(ylist.elements(), ylist.size());
		ylist = null;
		GraphIntervalSym gsym = new GraphIntervalSym(xcoords, wcoords, ycoords, id, aseq);
		if (orientation) {
			gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_PLUS);
		} else {
			gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_MINUS);
		}
		return gsym;
	}


	// Returns the unique graph ID associated with the score name;
	// this score will map to this same graph ID for all other
	// ScoredContainerSym's that have the same ID, even if they
	// are on other BioSeq's.
	public final String getGraphID(AnnotatedSeqGroup seq_group, String score_name, char strand) {
		// I'm just assuming that this combination will be unique
		String id = getID() + ":" + strand + ":" + score_name;
		if (id2gstate.get(id) == null) {
			GraphState gs = initializeGraphState(id, score_name, strand);
			id2gstate.put(id, gs);
		}
		return id;
	}

	private GraphState initializeGraphState(String id, String score_name, char strand) {
		GraphState gs = DefaultStateProvider.getGlobalStateProvider().getGraphState(id);
		gs.setFloatGraph(false);
		gs.setGraphStyle(GraphType.HEAT_MAP);
		gs.getTierStyle().setHumanName(score_name);
		gs.setComboStyle(getContainerStyle(strand));
		return gs;
	}


	private ITrackStyle getContainerStyle(char strand) {
		// There are separate combo style items for +, - and +/-.
		// They are shared by all scores with the same ID on different seqs.
		// They do not need a "+" or "-" as part of their name, because the glyph
		// factory will do that.
		ITrackStyle style;
		String name = (String) this.getProperty("method");
		if (name == null) {
			name = "Scores";
		} else {
			name = "Scores " + name;
		}

		if (strand == '+') {
			style = id2combo_style_plus.get(getID());
			if (style == null) {
				style = newComboStyle(name);
				id2combo_style_plus.put(getID(), style);
			}
		} else if (strand == '-') {
			style = id2combo_style_minus.get(getID());
			if (style == null) {
				style = newComboStyle(name);
				id2combo_style_minus.put(getID(), style);
			}
		} else {
			style = id2combo_style_neutral.get(getID());
			if (style == null) {
				style = newComboStyle(name);
				id2combo_style_neutral.put(getID(), style);
			}
		}
		return style;
	}

	private static ITrackStyle newComboStyle(String name) {
		ITrackStyle style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(name);
		style.setGraphTier(true);
		style.setExpandable(true);
		style.setCollapsed(false);
		return style;
	}
}
