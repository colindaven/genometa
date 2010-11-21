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
package com.affymetrix.igb.glyph;

import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.ScoredContainerSym;
import com.affymetrix.genometryImpl.IndexedSym;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;

import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;

public final class ScoredContainerGlyphFactory implements MapViewGlyphFactoryI {

	private static final boolean DEBUG = false;
	private static final boolean separate_by_strand = true;

	/** Does nothing. */
	public void init(Map options) {
	}

	public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
		boolean attach_graphs = PreferenceUtils.getBooleanParam(ScoredIntervalParser.PREF_ATTACH_GRAPHS,
				ScoredIntervalParser.default_attach_graphs);
		if (sym instanceof ScoredContainerSym) {
			ScoredContainerSym container = (ScoredContainerSym) sym;
			if (DEBUG) {
				System.out.println("&&&&& in ScoredContainerGlyphFactory, attach graphs: " + attach_graphs);
			}
			// first draw the little rectangle that will go in an annotation tier
			// and be used to select regions for the pivot view
			TrackView.getAnnotationGlyphFactory().createGlyph(sym, smv);

			// then draw the graphs
			if (attach_graphs) {
				displayGraphs(container, smv, false);
			}
		} else {
			System.err.println("ScoredContainerGlyphFactory.createGlyph() called, but symmetry "
					+ "passed in is NOT a ScoredContainerSym: " + sym);
		}
		if (DEBUG) {
			System.out.println("&&&&& exiting ScoredContainerGlyphFactory");
		}
	}

	private static void displayGraphs(ScoredContainerSym original_container, SeqMapView smv, boolean update_map) {
		BioSeq aseq = smv.getAnnotatedSeq();
		if (original_container.getSpan(aseq) == null) {
			return;
		}
		GraphIntervalSym[] the_graph_syms = determineGraphSyms(smv, aseq, original_container);

		for (GraphIntervalSym gis : the_graph_syms) {
			displayGraphSym(gis, smv);
		}

		if (update_map) {
			AffyTieredMap map = smv.getSeqMap();
			map.packTiers(false, true, false);
			map.stretchToFit(false, false);
			map.updateWidget();
		}
	}

	private static GraphIntervalSym[] determineGraphSyms(SeqMapView smv, BioSeq aseq, ScoredContainerSym original_container) {
		BioSeq vseq = smv.getViewSeq();
		AnnotatedSeqGroup seq_group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		if (aseq != vseq) {
			DerivedSeqSymmetry derived_sym = SeqUtils.copyToDerived(original_container);
			SeqUtils.transformSymmetry(derived_sym, smv.getTransformPath());
			return makeGraphsFromDerived(derived_sym, seq_group, vseq);
		}
		// aseq == vseq, so no transformation needed
		return makeGraphs(original_container, seq_group);
	}

	private static GraphIntervalSym[] makeGraphs(ScoredContainerSym container, AnnotatedSeqGroup seq_group) {
		int score_count = container.getScoreCount();
		List<GraphIntervalSym> results = null;
		if (separate_by_strand) {
			results = new ArrayList<GraphIntervalSym>(score_count * 2);
		} else {
			results = new ArrayList<GraphIntervalSym>(score_count);
		}

		for (int i = 0; i < score_count; i++) {
			String score_name = container.getScoreName(i);
			if (separate_by_strand) {
				GraphIntervalSym forward_gsym = container.makeGraphSym(score_name, true, seq_group);
				if (forward_gsym != null) {
					results.add(forward_gsym);
				}
				GraphIntervalSym reverse_gsym = container.makeGraphSym(score_name, false, seq_group);
				if (reverse_gsym != null) {
					results.add(reverse_gsym);
				}
			} else {
				GraphIntervalSym gsym = container.makeGraphSym(score_name, seq_group);
				if (gsym != null) {
					results.add(gsym);
				}
			}
		}
		return results.toArray(new GraphIntervalSym[results.size()]);
	}

	private static GraphIntervalSym[] makeGraphsFromDerived(DerivedSeqSymmetry derived_parent_sym,
			AnnotatedSeqGroup seq_group, BioSeq seq) {
		ScoredContainerSym original_container = (ScoredContainerSym) derived_parent_sym.getOriginalSymmetry();

		int score_count = original_container.getScoreCount();
		List<GraphIntervalSym> results = null;
		if (separate_by_strand) {
			results = new ArrayList<GraphIntervalSym>(score_count * 2);
		} else {
			results = new ArrayList<GraphIntervalSym>(score_count);
		}

		for (int i = 0; i < score_count; i++) {
			String score_name = original_container.getScoreName(i);
			if (separate_by_strand) {
				GraphIntervalSym forward_gsym = makeGraphSymFromDerived(derived_parent_sym, score_name, seq_group, seq, '+');
				if (forward_gsym != null) {
					results.add(forward_gsym);
				}
				GraphIntervalSym reverse_gsym = makeGraphSymFromDerived(derived_parent_sym, score_name, seq_group, seq, '-');
				if (reverse_gsym != null) {
					results.add(reverse_gsym);
				}
			} else {
				GraphIntervalSym gsym = makeGraphSymFromDerived(derived_parent_sym, score_name, seq_group, seq, '.');
				if (gsym != null) {
					results.add(gsym);
				}
			}
		}

		return results.toArray(new GraphIntervalSym[results.size()]);
	}

	// strands should be one of '+', '-' or '.'
	// name -- should be a score name in the original ScoredContainerSym
	private static GraphIntervalSym makeGraphSymFromDerived(DerivedSeqSymmetry derived_parent, String name,
			AnnotatedSeqGroup seq_group, BioSeq seq, final char strands) {
		ScoredContainerSym original_container = (ScoredContainerSym) derived_parent.getOriginalSymmetry();

		float[] original_scores = original_container.getScores(name);

		// Simply knowing the correct graph ID is the key to getting the correct
		// graph state, with the accompanying tier style and tier combo style.
		String id = original_container.getGraphID(seq_group, name, strands);

		if (original_scores == null) {
			System.err.println("ScoreContainerSym.makeGraphSym() called, but no scores found for: " + name);
			return null;
		}

		int derived_child_count = derived_parent.getChildCount();
		IntArrayList xcoords = new IntArrayList(derived_child_count);
		IntArrayList wcoords = new IntArrayList(derived_child_count);
		FloatArrayList ycoords = new FloatArrayList(derived_child_count);

		for (int i = 0; i < derived_child_count; i++) {
			Object child = derived_parent.getChild(i);
			if (child instanceof DerivedSeqSymmetry) {
				DerivedSeqSymmetry derived_child = (DerivedSeqSymmetry) derived_parent.getChild(i);
				SeqSpan cspan = derived_child.getSpan(seq);
				if (cspan != null) {
					if (strands == '.' || (strands == '+' && cspan.isForward())
							|| (strands == '-' && !cspan.isForward())) {
						xcoords.add(cspan.getMin());
						wcoords.add(cspan.getLength());
						IndexedSym original_child = (IndexedSym) derived_child.getOriginalSymmetry();
						// the index of this child in the original parent symmetry.
						// it is very possible that original_index==i in all cases,
						// but I'm not sure of that yet
						int original_index = original_child.getIndex();
						ycoords.add(original_scores[original_index]);
					}
				}
			}
		}
		xcoords.trimToSize();
		wcoords.trimToSize();
		ycoords.trimToSize();
		GraphIntervalSym gsym = null;
		if (!xcoords.isEmpty()) {
			gsym = new GraphIntervalSym(xcoords.elements(),
					wcoords.elements(), ycoords.elements(), id, seq);
			if (strands == '-') {
				gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_MINUS);
			} else if (strands == '+') {
				gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_PLUS);
			} else {
				gsym.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_BOTH);
			}
		}
		return gsym;
	}

	private static void displayGraphSym(GraphIntervalSym graf, SeqMapView smv) {
		GraphGlyph graph_glyph = new GraphGlyph(graf, graf.getGraphState());
		GraphState gstate = graph_glyph.getGraphState();
		ITrackStyle tier_style = gstate.getTierStyle();
		tier_style.setHumanName(graf.getGraphName());

		AffyTieredMap map = smv.getSeqMap();
		Rectangle2D.Double cbox = map.getCoordBounds();
		graph_glyph.setCoords(cbox.x, tier_style.getY(), cbox.width, tier_style.getHeight());
		map.setDataModelFromOriginalSym(graph_glyph, graf); // has side-effect of graph_glyph.setInfo(graf)
		// Allow floating glyphs ONLY when combo style is null.
		// (Combo graphs cannot yet float.)
		if (gstate.getComboStyle() == null && gstate.getFloatGraph()) {
			graph_glyph.setCoords(cbox.x, tier_style.getY(), cbox.width, tier_style.getHeight());
			GraphGlyphUtils.checkPixelBounds(graph_glyph, map);
			smv.getPixelFloaterGlyph().addChild(graph_glyph);
		} else {
			if (gstate.getComboStyle() != null) {
				tier_style = gstate.getComboStyle();
			}
			TierGlyph.Direction tier_direction = TierGlyph.Direction.FORWARD;
			if (GraphSym.GRAPH_STRAND_MINUS.equals(graf.getProperty(GraphSym.PROP_GRAPH_STRAND))) {
				tier_direction = TierGlyph.Direction.REVERSE;
			}
			TierGlyph tglyph = TrackView.getGraphTrack(map, tier_style, tier_direction);
			tglyph.addChild(graph_glyph);
			tglyph.pack(map.getView());
		}
	}
}
