package com.affymetrix.igb.glyph;

import java.awt.geom.Rectangle2D;
import java.util.Map;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.CollapsePacker;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;

public final class GenericGraphGlyphFactory implements MapViewGlyphFactoryI {

	private boolean check_same_seq = true;
	/** Name of a parameter for the init() method.  Set to Boolean.TRUE or Boolean.FALSE.
	 *  Determines whether the glyph factory will try to determine whether the GraphSym
	 *  that it is drawing is defined on the currently-displayed bioseq.
	 *  In some cases, you may want to intentionally display a graph on a seq that
	 *  has a different ID without checking to see if the IDs match.
	 */
	private static final String CHECK_SAME_SEQ_OPTION = "Check Same Seq";

	/** Name of a parameter for the init() method.  Set to an instance of Double.
	 *  Controls a parameter of the GraphGlyph.
	 *  @see GraphGlyph#setTransitionScale(double)
	 */
	/** Allows you to set the parameter CHECK_SAME_SEQ_OPTION. */
	public void init(Map options) {
		Boolean ccs = (Boolean) options.get(CHECK_SAME_SEQ_OPTION);
		if (ccs != null) {
			check_same_seq = ccs.booleanValue();
		}
	}

	public void createGlyph(SeqSymmetry sym, SeqMapView smv) {
		if (sym instanceof GraphSym) {
			displayGraph((GraphSym) sym, smv, check_same_seq);
		} else {
			System.err.println("GenericGraphGlyphFactory.createGlyph() called, but symmetry "
					+ "passed in is NOT a GraphSym: " + sym);
		}
	}

	/**
	 *  Makes a GraphGlyph to represent the input GraphSym,
	 *     and either adds it as a floating graph to the SeqMapView or adds it
	 *     in a tier, depending on getGraphState().getGraphStyle().getFloatGraph()
	 *     and getGraphState().getComboStyle().
	 *  All graphs that share the same tier style or the same combo tier style,
	 *     will go in the same tier.  Graphs with a non-null combo tier style
	 *     will go into an attached tier, never a floating glyph.
	 *  Also adds to the SeqMapView's GraphState-to-TierGlyph hash if needed.
	 */
	private static GraphGlyph displayGraph(GraphSym graf, SeqMapView smv, boolean check_same_seq) {
		BioSeq aseq = smv.getAnnotatedSeq();
		BioSeq vseq = smv.getViewSeq();
		BioSeq graph_seq = graf.getGraphSeq();
		boolean isGenome = false;

		if (check_same_seq && graph_seq != aseq) {
			// may need to modify to handle case where GraphGlyph's seq is one of seqs in aseq's composition...
			return null;
		}

		// GAH 2006-03-26
		//    want to add code here to handle situation where a "virtual" seq is being display on SeqMapView,
		//       and it is composed of GraphSym's from multiple annotated seqs, but they're really from the
		//       same data source (or they're the "same" data on different chromosomes for example)
		//       In this case want these displayed as a single graph

		//   match these up based on identical graph names / ids, then:
		//    Approach 1)
		//       build a CompositeGraphSym on the virtual seq
		//       make a single GraphGlyph
		//    Approach 2)
		//       create a new CompositeGraphGlyph subclass (or do I already have this?)
		//       make multiple GraphGlyphs
		//    Approach 3)
		//       ???


		GraphSym newgraf = graf;
		if (check_same_seq && graph_seq != vseq) {
			if (vseq != null && "genome".equals(vseq.getID())) {
				//TODO: Fix bug 1856102 "Genome View Bug" here. See Gregg's comments above.
				isGenome = true;
			}
			// The new graph doesn't need a new GraphState or a new ID.
			// Changing any graph properties will thus apply to the original graph.
			SeqSymmetry mapping_sym = smv.transformForViewSeq(graf, graph_seq);
			newgraf = GraphSymUtils.transformGraphSym(graf, mapping_sym);
		}
		if (newgraf == null || newgraf.getPointCount() == 0) {
			return null;
		}

		String graph_name = newgraf.getGraphName();
		if (graph_name == null) {
			// this probably never actually happens
			graph_name = "Graph #" + System.currentTimeMillis();
			newgraf.setGraphName(graph_name);
		}

		return displayGraphSym(newgraf, graf, smv, isGenome);
	}

	/**
	 * Almost exactly the same as ScoredContainerGlyphFactory.displayGraphSym.
	 * @param newgraf
	 * @param graf
	 * @param cbox
	 * @param map
	 * @param smv
	 * @param update_map
	 * @return graph glyph
	 */
	private static GraphGlyph displayGraphSym(GraphSym newgraf, GraphSym graf, SeqMapView smv, boolean isGenome) {
		GraphState gstate = graf.getGraphState();
		GraphGlyph graph_glyph = new GraphGlyph(newgraf, gstate);
		ITrackStyle tier_style = gstate.getTierStyle();
		tier_style.setHumanName(newgraf.getGraphName());
		tier_style.setCollapsed(isGenome);

		AffyTieredMap map = smv.getSeqMap();
		Rectangle2D.Double cbox = map.getCoordBounds();
		graph_glyph.setCoords(cbox.x, tier_style.getY(), cbox.width, tier_style.getHeight());
		map.setDataModelFromOriginalSym(graph_glyph, graf); // has side-effect of graph_glyph.setInfo(graf)
		// Allow floating glyphs ONLY when combo style is null.
		// (Combo graphs cannot yet float.)
		if (gstate.getComboStyle() == null && gstate.getFloatGraph()) {
			GraphGlyphUtils.checkPixelBounds(graph_glyph, map);
			smv.getPixelFloaterGlyph().addChild(graph_glyph);
		} else {
			if (gstate.getComboStyle() != null) {
				tier_style = gstate.getComboStyle();
			}
			TierGlyph.Direction direction = TierGlyph.Direction.NONE;
			if (GraphSym.GRAPH_STRAND_MINUS.equals(graf.getProperty(GraphSym.PROP_GRAPH_STRAND))) {
				direction = TierGlyph.Direction.REVERSE;
			}
			TierGlyph tglyph = TrackView.getGraphTrack(map, tier_style, direction);
			if (isGenome && !(tglyph.getPacker() instanceof CollapsePacker)) {
				CollapsePacker cp = new CollapsePacker();
				cp.setParentSpacer(0); // fill tier to the top and bottom edges
				cp.setAlignment(CollapsePacker.ALIGN_CENTER);
				tglyph.setPacker(cp);
			}
			tglyph.addChild(graph_glyph);
			tglyph.pack(map.getView());
		}
		return graph_glyph;
	}
}
