/**
 *   Copyright (c) 1998-2006 Affymetrix, Inc.
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

package com.affymetrix.genoviz.widget;

import com.affymetrix.genoviz.awt.AdjustableJSlider;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.NeoDataAdapterI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.SceneI;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.datamodel.Mapping;
import com.affymetrix.genoviz.datamodel.Sequence;
import com.affymetrix.genoviz.datamodel.Span;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.Hashtable;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.glyph.AbstractResiduesGlyph;
import com.affymetrix.genoviz.glyph.AlignedResiduesGlyph;
import com.affymetrix.genoviz.glyph.AlignmentGlyph;
import com.affymetrix.genoviz.glyph.ArrowGlyph;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.glyph.ResiduesGlyphI;
import com.affymetrix.genoviz.glyph.StretchContainerGlyph;
import com.affymetrix.genoviz.glyph.StringGlyph;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.util.ProteinUtils;
import com.affymetrix.genoviz.widget.neoassembler.AssemblyPacker;
import java.awt.AWTEventMulticaster;
import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.ItemSelectable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.JScrollBar;
import javax.swing.JSlider;

/**
 * This interface adds functionality for displaying an assembly
 * of multiple sequences.  Initialization, cropping, and alignment
 * modification are provided.<p>
 *
 * Example:<p> <pre>
 * NeoAssemblerI map = new NeoAssembler();
 * map.setResidueColor(Color.white);
 * map.setMatchColor(new Color(80, 80, 80));
 * map.setMisMatchColor(new Color(150, 150, 150));
 * map.setSelectionMethod(NeoMap.ON_MOUSE_DOWN);
 *
 * // Suppose we have an alignment like so:
 * //
 * //               1
 * //     0123456789012345
 * //       ACTG ACTGACT     &lt;&lt; Consensus
 * // Q:  AAACTTGACTGACT     &lt;&lt; a sequence with extra bases
 * // R:      TG ACT         &lt;&lt; a smaller sequence with gaps
 *
 * map.setRange(0, 15);
 *
 * // Indicate the complete consensus sequence
 * // and its start and end in the alignment coordinate system.
 * //  The first "A" aligns at position 2.
 * //  The last "T" aligns at position 13.
 * //  There may be gaps.  These are specified using addAlignedSpan().
 * Object cons_tag = map.setConsensus(2, 13, "ACTGACTGACT");
 *
 * // The sequence from 0..3 in the consensus sequence maps
 * // to positions 2..5 in the alignment coordinate system.
 * map.addAlignedSpan(cons_tag, 0, 3, 2, 5);
 *
 * // Similarly add the second span.
 * map.addAlignedSpan(cons_tag, 4, 10, 7, 13);
 *
 * // Now add the sequences that align to the consensus.
 * // The first sequence extends from 0 to 13.
 * // Gaps and residues can be delayed.
 * Object seq_tag = map.addSequence(0, 13);
 * map.setLabel(seq_tag, "Q");
 * map.setResidues(seq_tag, "AAACTTGACTGACT");
 * map.addAlignedSpan(seq_tag, 0, 13, 0, 13);
 *
 * // Finally, add the smaller sequence with two spans:
 * seq_tag = map.addSequence(4, 9);
 * map.setLabel(seq_tag, "R");
 * map.setResidues(seq_tag, "TGACT");
 * map.addAlignedSpan(seq_tag, 0, 1, 4, 5);
 * map.addAlignedSpan(seq_tag, 2, 4, 7, 9);
 *
 * map.updateWidget();
 *
 * </pre>
 *
 * <p> This javadoc explains the implementation specific features
 * of this widget concerning event handling and the java AWT.
 * In paticular, all genoviz implementations
 * of widget interfaces are subclassed
 * from <code>java.awt.Container</code>
 * and use the JDK 1.1 event handling model.
 *
 * <p> NeoAssembler extends <code>java.awt.Container</code>,
 * and thus, inherits all of the AWT methods
 * of <code>java.awt.Component</code>, and <code>Container</code>.
 * For example, a typical application might use the following as
 * part of initialization:
 *
 * <pre>
 * assembly = new NeoAssembler();
 *
 * assembly.setBackground(new Color(180, 250, 250));
 * assembly.setSize(500, 200);
 * </pre>
 *
 * @version $Id: NeoAssembler.java 6578 2010-08-03 15:33:07Z jnicol $
 */
public class NeoAssembler extends NeoContainerWidget
	implements NeoViewBoxListener,
			   ItemSelectable
{

	/**
	 * Old code assumed that its arguments implemented Comparable, which was only true for AlignmentGlyph.
	 * This is a hack to work around that.
	 */
	private static final Comparator<GlyphI> comparatorHack = new Comparator<GlyphI>() {

					public int compare(GlyphI arg0, GlyphI arg1) {
						if (arg0 instanceof AlignmentGlyph && arg1 instanceof AlignmentGlyph) {
							return ((AlignmentGlyph)arg0).compareTo((AlignmentGlyph)arg1);
						} else {
							throw new IllegalArgumentException();
						}
					}
	};

	public static final int UNKNOWN_ASSEMBLY = 0;
	public static final int NA_ASSEMBLY = 1;
	public static final int AA_ASSEMBLY = 2;

	public static final int SELECT_ALIGNMENTS = NO_SELECTION + 1;
	public static final int SELECT_RESIDUES = NO_SELECTION + 2;

	/**
	 * component identifier constant for the labels of the individual
	 * sequences of the assembly.
	 * @see #getItems
	 */
	public static final int LABELS = 6000;

	/**
	 * component identifier constant for the consensus sequence.
	 * @see #getItems
	 */
	public static final int CONSENSUS = LABELS + 1;

	/**
	 * component identifier constant for the set of alignment sequences.
	 * @see #getItems
	 */
	public static final int ALIGNMENTS = LABELS + 2;

	/**
	 * component identifier constant for the horizontal panning axis scroller
	 * @see #getItems
	 */
	public static final int AXIS_SCROLLER = LABELS + 3;

	/**
	 * component identifier constant for the vertical panning axis scroller
	 * @see #getItems
	 */
	public static final int OFFSET_SCROLLER = LABELS + 4;

	/**
	 * component identifier constant for other components not part
	 * of the interface description.
	 * @see #getItems
	 * @deprecated NeoAssembler.UNKNOWN used to hide NeoConstants.UNKNOWN
	 */
	@Deprecated
	public static final int UNKNOWN = LABELS + 5;

	/**
	 * component identifier constant for the consensus label.
	 * @see #getItems
	 */
	public static final int CONSENSUS_LABEL = LABELS + 6;
	
	public static boolean use_neo_scroll = false;
	public static boolean use_neo_zoom = false;

	protected int font_color_strategy = AlignedResiduesGlyph.FIXED_COLOR;
	protected int rect_color_strategy = AlignedResiduesGlyph.ALIGNMENT_BASED;

	protected boolean complementIfReversed = true;
	protected boolean show_axis = true;
	protected boolean apply_color_retro = true;
	protected boolean colors_affect_cons = true;
        protected boolean internal_zoomer = true;
	protected boolean fontControlsMaxZoom = true;
	protected boolean fontControlsGlyphHeight = true;
	protected String consensus_name = null;

	protected NeoMap alignmap, labelmap, consmap, conslabelmap;
	protected Scene align_scene, label_scene, cons_scene;
	// currently no vertical zooming, so no need for internal vzoom
	protected JScrollBar hscroll, vscroll;
  protected Adjustable hzoom;

	protected StretchContainerGlyph cglyph;

	protected AssemblyPacker apacker;
	protected List<GlyphI> align_glyphs;
	protected List<NeoDataAdapterI> adapters;
	boolean optimize_scrolling = false;
	boolean optimize_damage = false;
	boolean use_label_arrows = true;

	protected boolean auto_sort = true;
	protected boolean all_sorted = false;

	// hash of alignment glyphs to label glyphs on label map
	protected Hashtable<GlyphI,StringGlyph> labelhash = new Hashtable<GlyphI,StringGlyph>();
	// hash of alignment glyphs to arrow glyphs on label map
	protected Hashtable<GlyphI,ArrowGlyph> arrowhash = new Hashtable<GlyphI,ArrowGlyph>();
	// hash of alignment glyphs to alignment Mappings
	//  Hashtable alignhash = new Hashtable();

	// locations for scrollbars, consensus, and labels
	protected int vscroll_loc, hscroll_loc, cons_loc, label_loc;
        protected int scroll_size, zoom_size, cons_height, label_width;
	protected int align_offset, align_glyph_height, align_offset_scale, label_font_height;
	protected int align_coord_spacing, align_pixel_spacing, align_spacing;
	protected int label_string_inset = 2;
	protected int label_string_width = 80;
	protected int label_arrow_width = 7;
	protected int reverse_arrow_inset = 20;
	protected int forward_arrow_inset = 10;

	protected int align_num;

	protected int axis_offset = 18;
	protected int cons_offset = axis_offset + 5;
	protected Sequence ref_seq, cons_seq;
	protected Mapping cons_align;
	protected boolean cons_start_received = false, cons_end_received = false;
	protected boolean consensus_complete = false;
	protected int cons_start, cons_end;
	protected int range_start, range_end;
	protected AlignmentGlyph cons_glyph;
	protected AxisGlyph axis_glyph;

	// currently selected glyph (for doing SELECT_RESIDUES subselection)
	protected GlyphI sel_glyph;


	private static final Color default_map_background = new Color(180, 250, 250);
	private static final Color default_panel_background = Color.lightGray;
	private static final Color default_label_background = Color.lightGray;

	protected Color label_color = Color.black;
	protected Color residue_color = Color.white;

	protected Color match_rect_color = Color.darkGray;
	protected Color mismatch_rect_color = Color.gray;
	// color for unknown residues background
	// (defaults to null, meaning no background color)
	protected Color unknown_rect_color = null;
	protected Color match_font_color = null;
	protected Color mismatch_font_color = null;
	protected Color unknown_font_color = null;

	protected Color[][] bg_color_matrix;
	protected Color[][] fg_color_matrix;

	protected Color unaligned_font_color = Color.lightGray;
	protected Color unaligned_rect_color = new Color(220, 220, 220);
	protected Color background_col = Color.lightGray;

    protected Font label_font = NeoConstants.default_bold_font;
	protected Font residue_font = NeoConstants.default_bold_font;

	// toggle for automatically applying new color settings
	// to previously added glyphs -- NOT YET IMPLEMENTED
	//   protected boolean grandfatherColorBehavior;

	// default setRect behavior for NeoAssembler is for alignments and consensus
	//   to stretch to fit in X, and everything to remain constant in Y
	protected int reshape_constraint[] = { FITWIDGET, NeoConstants.NONE };
	protected int zoom_behavior[] = { CONSTRAIN_MIDDLE, CONSTRAIN_MIDDLE };

	private int selection_behavior = SELECT_ALIGNMENTS;

	private int assemblyType = NA_ASSEMBLY;

	protected Set<NeoRangeListener> range_listeners = new CopyOnWriteArraySet<NeoRangeListener>();

	protected Character match_char = null;

	/**
	 * @param internal_zoomer indecates whether or not the internal zooming
	 * scrollbar is to be included.
	 */
	public NeoAssembler(boolean internal_zoomer) {

		this(NA_ASSEMBLY, internal_zoomer);
	}

	/**
	 * Constructs a NeoAssembler with an internal zoomer.
	 */
	public NeoAssembler() {
		this(NA_ASSEMBLY, true);
	}

	/**
	 * Constructs a NeoAssembler with an internal zoomer.
	 *
	 * @param assemblyType {@link #NA_ASSEMBLY} or {@link #AA_ASSEMBLY}.
	 */
	public NeoAssembler(int assemblyType) {
		this(assemblyType, true);
	}

	/**
	 * Constructs a NeoAssembler of the specified "type".
	 *
	 * @param assemblyType {@link #NA_ASSEMBLY} or {@link #AA_ASSEMBLY}.
	 * @param internal_zoomer indecates whether or not the internal zooming
	 * scrollbar is to be included.
	 */
	public NeoAssembler(int assemblyType, boolean internal_zoomer) {
		super();
		this.assemblyType = assemblyType;
		this.internal_zoomer = internal_zoomer;
		if (assemblyType == AA_ASSEMBLY) {
			use_label_arrows = false;
			// adding extra column to hold unknown residue background color
			bg_color_matrix =
				new Color[ProteinUtils.LETTERS+1][ProteinUtils.LETTERS+1];
			fg_color_matrix =
				new Color[ProteinUtils.LETTERS+1][ProteinUtils.LETTERS+1];
		}
		else {
			use_label_arrows = true;
			// adding extra column to hold unknown residue background color
			bg_color_matrix = new Color[DNAUtils.LETTERS+1][DNAUtils.LETTERS+1];
			fg_color_matrix = new Color[DNAUtils.LETTERS+1][DNAUtils.LETTERS+1];
		}
		adjustColorMatrix(match_rect_color, mismatch_rect_color,
				unknown_rect_color, false,
				bg_color_matrix);

		hscroll_loc = PLACEMENT_BOTTOM;
		vscroll_loc = PLACEMENT_LEFT;
		cons_loc = PLACEMENT_TOP;
		label_loc = PLACEMENT_LEFT;
		scroll_size = 16;
		zoom_size = 20;
		cons_height = 40;
		label_width = 100;
		align_offset = 0;
		FontMetrics fontmet = GeneralUtils.getFontMetrics(residue_font);
		//int font_width = fontmet.charWidth('C');
		align_glyph_height = fontmet.getAscent()+2;
		align_offset_scale = 1;
		align_pixel_spacing = 1;
		align_spacing = align_pixel_spacing;
		align_num = 0;
		align_glyphs = new ArrayList<GlyphI>();

		alignmap = new NeoMap(false, false);
		labelmap = new NeoMap(false, false);
		consmap = new NeoMap(false, false);
		conslabelmap = new NeoMap(false, false);

		// zeroing out pixel fuzziness -- this prevents accidental selection of
		// multiple aligned sequences or labels
		// labels and sequences are big enough there doesn't need to be any
		// selection fuzziness anyway
		// GAH  12-10-97
		alignmap.setPixelFuzziness(0);
		labelmap.setPixelFuzziness(0);
		consmap.setPixelFuzziness(0);
		conslabelmap.setPixelFuzziness(0);

		addWidget(alignmap);
		addWidget(labelmap);
		addWidget(consmap);
		addWidget(conslabelmap);

		hscroll = new JScrollBar(JScrollBar.HORIZONTAL);
		vscroll = new JScrollBar(JScrollBar.VERTICAL);
		if (internal_zoomer)  {
			hzoom = new AdjustableJSlider(JSlider.HORIZONTAL);
		}

		setBackground(default_panel_background);

		alignmap.setMapColor(default_map_background);
		consmap.setMapColor(default_map_background);
		labelmap.setMapColor(Color.lightGray); //tss default_label_background);
		conslabelmap.setMapColor(default_label_background);

		this.setLayout(null);
		add(alignmap);
		add(labelmap);
		add(consmap);
		add((Component)hscroll);
		add((Component)vscroll);
		add(conslabelmap);
		if (internal_zoomer)  { add((Component)hzoom); }

		/*
		 *  If using NeoMap's internal scrollbars
		 *  then for some reason, need to explicitly resize NeoMap so that
		 *  each axis has room for scrollbar width + a bit (scrollbar width = 16),
		 *  or else get IllegalArgmentExceptions (must be a NeoScrollbar bug)
		 *  don't need to resize for NeoAssembler -- not using NeoMap
		 *  internal scrollbars
		 *    alignmap.resize(20,20);
		 *    labelmap.resize(100, 20);
		 *    consmap.resize(20, 50);
		 */
		// currently in NeoMap, need to set the map range and offset or else
		// nothing will be seen -- need to make this default to dynamic map
		// extension...

		setUpAlignMap();
		setUpConsensusMap();
		setUpLabelMap(labelmap);
		label_scene = labelmap.getScene();
		setUpLabelMap(conslabelmap);

		setHorizontalScroller (hscroll);
		setVerticalScroller   (vscroll);
		setInternalZoomer     (hzoom);

		alignmap.addMouseListener(this);
		consmap.addMouseListener(this);
		labelmap.addMouseListener(this);
		conslabelmap.addMouseListener(this);

		alignmap.addMouseMotionListener(this);
		consmap.addMouseMotionListener(this);
		labelmap.addMouseMotionListener(this);
		conslabelmap.addMouseMotionListener(this);

		alignmap.addKeyListener(this);
		consmap.addKeyListener(this);
		labelmap.addKeyListener(this);
		conslabelmap.addKeyListener(this);

		// for transforming viewbox changes to NeoRangeEvents
		alignmap.addViewBoxListener(this);

		// To increase the vertical scroll increment from 1 pixel to the size of font height HARI 2/3/2000
		label_font_height = GeneralUtils.getFontMetrics(label_font).getHeight();
		vscroll.setUnitIncrement(label_font_height);

		/**
		 * To enable scrolling such that the last sequence is placed right below the consensus.
		 * The component listener is registered so that the map offset could be increased.
		 * This enables scrolling past the current position.
		 */
		this.addComponentListener(new ComponentAdapter(){
			@Override
			public void componentResized(ComponentEvent e) {
				int [] visible_offset = labelmap.getVisibleOffset();
				double scale = (alignmap.getView().getTransform()).getScaleY();
				int font_height = GeneralUtils.getFontMetrics(residue_font).getHeight();
				int visible_map_height = visible_offset[1] - visible_offset[0];
				//visible_map_height is in coords
				visible_map_height = (int)(visible_map_height-(font_height/scale));
				//font_height is substracted from the visible_map_height to prevent scrolling past the last seq.
				labelmap.setMapOffset(0,(getSeqHeight() + visible_map_height));
				alignmap.setMapOffset(0,(getSeqHeight() + visible_map_height));
			}
		});
	}

	/**
	 * Constructs a copy of the given NeoAssembler.
	 *
	 * @param root the original NeoAssembler to copy.
	 */
	public NeoAssembler(NeoAssembler root) {
		this();  // Set up a normal NeoAssembler.
		setRoot(root);  // copy the original.
	}

	/**
	 * @return the height that aligned sequences occupy (in coords).
	 */
	public int getSeqHeight()
	{
		int number_of_aligned_sequences = align_glyphs.size();
		double scale = (alignmap.getView().getTransform()).getScaleY();
		double aligned_sequences_height = ((number_of_aligned_sequences * (label_font_height/scale)));
		return ((int)aligned_sequences_height);
	}

	protected void setRoot(NeoAssembler root) {

		this.use_label_arrows = root.use_label_arrows;
		this.assemblyType = root.assemblyType;

		// Need to set up a derived view from each individual NeoMap
		// within this NeoAssembler based on corresponding NeoMap within root
		// NeoAssembler.
		NeoMap root_alignmap = (NeoMap)root.getWidget(NeoAssembler.ALIGNMENTS);
		NeoMap root_consmap =  (NeoMap)root.getWidget(NeoAssembler.CONSENSUS);
		NeoMap root_labelmap =  (NeoMap)root.getWidget(NeoAssembler.LABELS);
		NeoMap root_conslabelmap =
			(NeoMap)root.getWidget(NeoAssembler.CONSENSUS_LABEL);
		alignmap.setRoot(root_alignmap);
		consmap.setRoot(root_consmap);
		labelmap.setRoot(root_labelmap);
		conslabelmap.setRoot(root_conslabelmap);

		// Set various fields that need to be shared between
		// this NeoAssembler and the root

		// Object fields are being set here.  Since these fields are objects
		//   once they are assigned, unless reassigned they will continue to
		//   point to same object as corresponding fields in root
		align_scene = root.align_scene;
		label_scene = root.label_scene;
		cons_scene = root.cons_scene;
		cglyph = root.cglyph;
		apacker = root.apacker;
		align_glyphs = root.align_glyphs;
		adapters = root.adapters;
		labelhash = root.labelhash;
		arrowhash = root.arrowhash;
		ref_seq = root.ref_seq;
		cons_seq = root.cons_seq;
		cons_align = root.cons_align;
		cons_glyph = root.cons_glyph;
		// more object fields to copy, these are inherited from NeoContainerWidget
		glyph_hash = root.glyph_hash;
		model_hash = root.model_hash;
		selected = root.getSelected();

		// Primitive types are being copied here.  This means that when setRoot()
		//   is called these will be synced with same fields in root, but after
		//   that they will act independently.  This is NOT the desired behavior.
		//   Therefore need to improve.  Possible options:
		// a.) Change all these field types from primitives to corresponding
		//       objects, then will act in sync since both root and this will
		//       continue pointing to same object
		// b.) put in check for "sibling" NeoAssemblers whenever these values
		//       change, and propogate change to each sibling
		optimize_scrolling = root.optimize_scrolling;
		optimize_damage = root.optimize_damage;
		auto_sort = root.auto_sort;
		all_sorted = root.all_sorted;
		align_num  = root.align_num;
		range_start = root.range_start;
		range_end = root.range_end;
		cons_start = root.cons_start;
		cons_end = root.cons_end;
		cons_start_received = root.cons_start_received;
		cons_end_received = root.cons_end_received;
		consensus_complete = root.consensus_complete;

	}

	protected void setUpAlignMap() {
		align_scene = alignmap.getScene();

		alignmap.setScaleConstraint(NeoMap.X, NeoMap.INTEGRAL_PIXELS);

		alignmap.setMapRange(0, 0);
		alignmap.stretchToFit();
		alignmap.setRubberBandBehavior(false);

		cglyph = new StretchContainerGlyph();
		cglyph.setCoords(0, align_offset, 0, 0);
		align_scene.addGlyph(cglyph);
		apacker = new AssemblyPacker();
		apacker.setSpacing(align_spacing);
		cglyph.setPacker(apacker);

		alignmap.setReshapeBehavior(NeoMap.X, reshape_constraint[X]);
		alignmap.setReshapeBehavior(NeoMap.Y, reshape_constraint[Y]);
		alignmap.zoomOffset(align_offset_scale);
		alignmap.scrollOffset(0);
	}

	protected void setUpConsensusMap() {
		cons_scene = consmap.getScene();

		consmap.setScaleConstraint(NeoMap.X, NeoMap.INTEGRAL_PIXELS);

		consmap.setMapRange(0, 0);
		consmap.setMapOffset(0, cons_height);

		consmap.stretchToFit();
		consmap.setRubberBandBehavior(false);
		axis_glyph = consmap.addAxis(axis_offset);

		axis_glyph.setVisibility(false);
		consmap.setReshapeBehavior(NeoMap.X, reshape_constraint[X]);

		consmap.setReshapeBehavior(NeoMap.Y, NeoConstants.NONE);
		consmap.zoomOffset(align_offset_scale);
		consmap.scrollOffset(0);

		cons_start_received = false;
		cons_end_received = false;
		consensus_complete = false;
	}

	protected void setUpLabelMap(NeoMap lmap) {

		// -1 to account for setBounds +1  8-25-98 GAH
		lmap.setMapRange(0, label_width-1);
		lmap.setMapOffset(0, 0);
		lmap.setRubberBandBehavior(false);

		lmap.setReshapeBehavior(NeoMap.Y, reshape_constraint[Y]);
		lmap.zoomOffset(align_offset_scale);
		lmap.scrollOffset(0);
	}

	/**
	 * Makes sure that the range of the map takes into account orientation.
	 */
	protected boolean checkRange(int start, int end) {
		int orientedStart = Math.min(start, end);
		int orientedEnd = Math.max(start, end);
		if (orientedStart < range_start || orientedEnd > range_end) {
			if (orientedStart < range_start) { range_start = orientedStart; }
			if (orientedEnd > range_end) { range_end = orientedEnd; }
			setRange(range_start, range_end);
			return true;
		}
		return false;
	}

	/**
	 * adjusts the bounds of the assembly displayed.  in practice
	 * this should be set to range from 0 to one past the length
	 * of the assembly.
	 *
	 * @param start the integer indicating the starting residue position
	 * @param end   the integer indicating the final residue position.
	 */
	public void setRange(int start, int end) {
		range_start = start;
		range_end = end+1;
		alignmap.setMapRange(range_start, range_end);
		consmap.setMapRange(range_start, range_end);
		consmap.stretchToFit(false, false);
		alignmap.stretchToFit(false, false);
		//Rectangle2D.Double alignbox = alignmap.getScene().getCoordBox();
		Rectangle2D.Double viewbox = alignmap.getView().getCoordBox();
		if ((viewbox.x < range_start) && (viewbox.x+viewbox.width > range_end)) {
			// do nothing if alignments won't stretch to edge of NeoAssembler???
		}
		else if (viewbox.x < range_start) {
			scrollRange(range_start);
		}
		else if (viewbox.x+viewbox.width > range_end) {
			scrollRange(range_end);
		}

	}

	/**
	 * Not implemented yet.
	 */
	protected Glyph addAlignment(Mapping align) {
		return null;
	}

	/**
	 * Sets a character used to mask residues in aligned sequences
	 * that match the consensus.  Makes parts of the alignment that
	 * don't match stand out more.  Set to null to disable this feature.
	 */
	public void setMatchChar ( Character match_char ) {
		this.match_char = match_char;
	}

	/**
	 * @see NeoAssembler#setMatchChar
	 */
	public Character getMatchChar () {
		return this.match_char;
	}

	/**
	 * add a consensus sequence letters starting at position
	 * <code>start</code> and ending at <code>end</code> in the alignment
	 * coordinate system.
	 *
	 * @param start the starting position in the alignment coordinate system.
	 * @param end  the final position in the alignment coordinate system.
	 *
	 * @return a <code>Glyph</code> of the consensus sequence.
	 * Use <code>addAlignedSpan</code> to indicate the alignment of
	 * the consensus sequence to the alignment coordinate system.
	 */
	public GlyphI setConsensus(int start, int end, String residues) {
		// hiding axis till have a first consensus or sequence  GAH 4-14-99
		axis_glyph.setVisibility(true);

		checkRange(start, end);
		cons_seq = new Sequence();
		cons_seq.setResidues(residues);

		if (cons_glyph != null) {
			consmap.removeItem(cons_glyph);
			consensus_complete = false;
			cons_start_received = false;
			cons_end_received = false;
		}

		int length = residues.length();

		// currently NOT using AlignmentGlyph unichild option for consensus --
		//   causes problems because of special calls to
		//   consensus.setResidues, etc. in NeoAssembler
		if (NA_ASSEMBLY == assemblyType) {
			cons_glyph = new AlignmentGlyph(AlignmentGlyph.NA_RESIDUES, length);
		}
		else if (AA_ASSEMBLY == assemblyType) {
			cons_glyph = new AlignmentGlyph(AlignmentGlyph.AA_RESIDUES, length);
		}
		else {
			System.out.println("!!! no assembly type !!!");
			cons_glyph = new AlignmentGlyph();
		}

		cons_glyph.setResidueFont(residue_font);
		cons_glyph.setForegroundColor(residue_color);
		cons_glyph.setComplementIfReversed ( complementIfReversed );
		cons_glyph.setForward(true);
		cons_glyph.showArrow(true);
		cons_glyph.setCoords(start, cons_offset, end-start, align_glyph_height);
		cons_glyph.setSequence(cons_seq);
		cons_scene.addGlyph(cons_glyph);

		cons_start = start;
		cons_end = end;
		cons_align = new Mapping(start, end);

		cons_align.setSequence(cons_seq);
		cons_glyph.setMapping(cons_align);

		return cons_glyph;
	}

	public void setComplementIfReversed ( boolean flag ) {
		this.complementIfReversed = flag;
	}

	/**
	 *
	 * add the sequence letters to be associated with <code>seq_tag</code>.
	 * <code>setResidues</code> allows application controlled delayed loading
	 * of sequence data.
	 *
	 * @param seq_tag an Object (GlyphI) to associate a string of letters.
	 * Instances of seq_tag are returned from <code>addSequence</code>.
	 *
	 * @param residues a String of letters
	 *
	 * @return the (possibly new) <code>Glyph</code> for the sequence.
	 *
	 * @see #addSequence
	 */
	public GlyphI setResidues(GlyphI seq_tag, String residues) {
		AlignmentGlyph aglyph = (AlignmentGlyph)seq_tag;
		Sequence seq = new Sequence();
		seq.setResidues(residues);
		aglyph.setSequence(seq);

		Mapping m = aglyph.getMapping();
		m.setSequence(seq);
		aglyph.setMapping(m);
		if (ref_seq != null) {
			aglyph.setReference(ref_seq);
		}

		return aglyph;
	}

	/**
	 * adds a span to a sequence object.  Each aligned sequence has one
	 * or more ungapped spans.  <code>addAlignedSpan</code> maps the
	 * subsequence from <code>seqstart</code> to <code>seqend</code> to
	 * the alignment coordinate system from <code>alignstart</code> to
	 * <code>alignend</code>.
	 *
	 * @param seq_tag    an Object (GlyphI) to which a new span is added.
	 *                   instances of seq_tag are returned from
	 *                   <code>addSequence</code>.
	 * @param seqstart   the starting residue position in the sequence of this
	 *                   span.
	 * @param seqend     the final residue position in the sequence of this span.
	 * @param alignstart the starting position in the alignment coordinate
	 *                   system of the first residue of this span.
	 * @param alignend   the final position in the alignment coordinate
	 *                   system of the last residue of this span.
	 *
	 * @return the <code>Glyph</code> of the span.
	 *
	 */
	public GlyphI addAlignedSpan(GlyphI seq_tag, int seqstart, int seqend,
			int alignstart, int alignend) {
		AlignedResiduesGlyph gl;
		if (!(seq_tag instanceof AlignmentGlyph)) {
			throw new IllegalArgumentException("can only handle AlignmentGlyph.");
		}
		// special case when adding spans to the consensus
		if (seq_tag == cons_glyph) {
			if (consensus_complete) {
				throw new RuntimeException
					("**** trying to add span to already completed consensus ****");
			}
			if (alignstart == cons_start) {
				cons_start_received = true;
			}
			if (alignend == cons_end) {
				cons_end_received = true;
			}
			cons_align.addSpan(new Span(seqstart, seqend, alignstart, alignend));

			if (cons_start_received && cons_end_received) {
				consensus_complete = true;
				ref_seq = createReference(cons_align);
				cons_glyph.setReference(ref_seq);
			}

			else {
			}

			AlignmentGlyph aglyph = (AlignmentGlyph)seq_tag;
			aglyph.getMapping().addSpan(
					new Span(seqstart, seqend, alignstart, alignend));
			gl = (AlignedResiduesGlyph)aglyph.addUngappedAlignment(
					seqstart, seqend, alignstart, alignend);

			gl.setBackgroundColorMatrix(bg_color_matrix);

			if (ref_seq != null) {
				gl.setReference(ref_seq);
			}
			gl.setMatchChar ( match_char );
			// if all of consensus received, then set reference for any
			// aligned sequence already added
			if (consensus_complete && ref_seq != null) {
				AlignmentGlyph seq_glyph;
				for (int i=0; i<align_glyphs.size(); i++) {
					seq_glyph = (AlignmentGlyph)align_glyphs.get(i);
					seq_glyph.setReference(ref_seq);
				}
			}
		}

		else {
			AlignmentGlyph aglyph = (AlignmentGlyph)seq_tag;
			aglyph.getMapping().addSpan(
					new Span(seqstart, seqend, alignstart, alignend));
			gl =
				(AlignedResiduesGlyph)aglyph.addUngappedAlignment(seqstart, seqend,
						alignstart, alignend);

			gl.setBackgroundColorMatrix(bg_color_matrix);
			if (ref_seq != null) {
				gl.setReference(ref_seq);
			}
			gl.setMatchChar ( match_char );
		}
		return gl;
	}

	public GlyphI addUnalignedSpan(GlyphI seq_tag,
			int seqstart, int seqend,
			int refstart, int refend) {
		AlignmentGlyph aglyph = (AlignmentGlyph)seq_tag;
		AlignedResiduesGlyph glyph = aglyph.addUnalignedSpan(seqstart, seqend,
				refstart, refend);
		glyph.setColor(unaligned_rect_color);
		glyph.setBackgroundColor(unaligned_rect_color);
		glyph.setForegroundColor(unaligned_font_color);
		return glyph;
	}

	public GlyphI setGappedConsensus(String name, String gapped_residues,
			int start, boolean orientation) {
		int end = start + gapped_residues.length()-1;
		AlignmentGlyph align_glyph =
			(AlignmentGlyph)setConsensus(start, end, gapped_residues);
		addAlignedSpan(align_glyph, start, end, start, end);
		setLabel(align_glyph, name);
		align_glyph.setForward(orientation);
		return align_glyph;
	}

	public GlyphI addGappedSequence(String name, String gapped_residues,
			int start, boolean orientation) {
		int end = start + gapped_residues.length()-1;
		AlignmentGlyph align_glyph = (AlignmentGlyph)addSequence(start, end);
		setResidues(align_glyph, gapped_residues);
		addAlignedSpan(align_glyph, start, end, start, end);
		setLabel(align_glyph, name);
		align_glyph.setForward(orientation);
		return align_glyph;
	}

	/**
	 * adds a new sequence to the alignment
	 * that extends from <code>start</code> to <code>end</code> in the
	 * alignment coordinate system.  There may be one or more gaps between
	 * these bounds as set by <code>addAlignedSpan</code>.
	 *
	 * @param start the starting position in the alignment coordinate
	 *   system of the first residue of this sequence.
	 * @param end  the final position in the alignment coordinate
	 *   system of the last residue of this sequence.
	 *
	 * @return the <code>Glyph</code> for the sequence.
	 *
	 * @see #addAlignedSpan
	 * @see #setResidues
	 * @see #setLabel
	 */
	public GlyphI addSequence(int start, int end) {

		// hiding axis till have a first consensus or sequence  GAH 4-14-99
		axis_glyph.setVisibility(true);

		checkRange(start, end);
		AlignmentGlyph aglyph;

		int length;
		if (start < end) { length = end-start+1; }
		else { length = start-end+1; }
		if (NA_ASSEMBLY == assemblyType) {
			aglyph = new AlignmentGlyph(AlignmentGlyph.NA_RESIDUES, length);
		}
		else if (AA_ASSEMBLY == assemblyType) {
			aglyph = new AlignmentGlyph(AlignmentGlyph.AA_RESIDUES, length);
		}
		else {
			System.out.println("!!! no assembly type !!!");
			aglyph = new AlignmentGlyph();
		}
		aglyph.setComplementIfReversed ( complementIfReversed );
		aglyph.setResidueFont(residue_font);
		aglyph.setForegroundColor(residue_color);
		aglyph.setBackgroundColorMatrix(bg_color_matrix);

		boolean forward = (start <= end) ? true : false;
		aglyph.setForward(forward);
		aglyph.showArrow(true);

		Mapping m = new Mapping(start, end);
		aglyph.setMapping(m);

		aglyph.setCoords(start, align_offset, end-start+1, align_glyph_height);

		if (auto_sort) {
			// If all previous alignment glyphs are already sorted,
			// can do easy and fast sort.
			if (all_sorted) {
				cglyph.addChild(aglyph,
						getSortedPosition(aglyph, cglyph.getChildren()));
			}
			// Otherwise, use QuickSorter.
			else {
				cglyph.addChild(aglyph);
				align_glyphs = cglyph.getChildren();
				Collections.sort(align_glyphs, comparatorHack);
				//QuickSorter.sort(cglyph.getChildren());
				all_sorted = true;
			}
		}
		else {
			cglyph.addChild(aglyph);
			all_sorted = false;
		}

		// getChildren() does not make a copy, but passes a reference
		// therefore cglyph.addChild() calls, etc., also affect align_glyphs,
		// and align_glyphs.add() calls add to cglyph children
		align_glyphs = cglyph.getChildren();

		if (auto_sort) {
			pack();
		}
		else {
			// if auto sort not turned on, rather than repacking whole assembly
			//    can just pack this new alignment glyph below all the
			//    previous ones packed
			apacker.pack(cglyph, aglyph, alignmap.getView());
			Rectangle2D.Double cbox = cglyph.getCoordBox();
			alignmap.setMapOffset((int)cbox.y, (int)(cbox.y + cbox.height));
			labelmap.setMapOffset((int)cbox.y, (int)(cbox.y + cbox.height));
		}
		return aglyph;
	}


	/**
	 * gets the label of an aligned sequence.
	 * @param seq_tag the alignment labeled.
	 * @return a String containing the label.
	 */
	public String getLabel(GlyphI seq_tag) {
		String label = null;
		AlignmentGlyph aglyph = (AlignmentGlyph)seq_tag;
		if (aglyph == cons_glyph) {
			label = consensus_name;
		}
		else {
			label = labelhash.get(aglyph).getString();
		}
		return label;
	}

	/**
	 * associates a string label with an aligned sequence.  The label is
	 * displayed adjacent to the sequence alignment display.
	 *
	 * @param seq_tag  the <code>GlyphI</code> representing the
	 *   aligned sequence returned from
	 *   <code>addSequence</code>
	 * @param name the <code>String</code> label to display for
	 *   <code>gl</code>
	 * @return GlyphI
	 * @see #addSequence
	 */
	public GlyphI setLabel(GlyphI seq_tag, String name) {
		AlignmentGlyph aglyph = (AlignmentGlyph)seq_tag;

		//-------- Adding label --------

		StringGlyph sglyph = new StringGlyph( name );
		sglyph.setPlacement(NeoConstants.LEFT);
		sglyph.setFont(label_font);
		sglyph.setForegroundColor(label_color);

		if (aglyph == cons_glyph) {
			consensus_name = name;
			sglyph.setCoords(label_string_inset, 0, label_string_width, align_glyph_height);
			conslabelmap.getScene().addGlyph(sglyph);
			return sglyph;
		}

		sglyph.setCoords(label_string_inset, align_offset,
				label_string_width, align_glyph_height);

		if (use_label_arrows) {
			ArrowGlyph arrowglyph = new ArrowGlyph();
			boolean aforward;
			aforward = aglyph.isForward();
			if (aforward) {
				arrowglyph.setCoords(label_width - forward_arrow_inset, align_offset,
						label_arrow_width, align_glyph_height);
			}
			else {
				arrowglyph.setCoords(label_width - forward_arrow_inset, align_offset,
						label_arrow_width, align_glyph_height);
			}
			arrowglyph.setForward(aforward);
			label_scene.addGlyph(arrowglyph);
			arrowhash.put(aglyph, arrowglyph);
		}
		label_scene.addGlyph(sglyph);
		labelhash.put(aglyph, sglyph);
		moveLabels();
		return sglyph;
	}

	/**
	 * makes sure that the labels are all matched up properly
	 * with their corresponding sequences.
	 *
	 * <p> In the future this may well become deprecated or protected.
	 * This is mostly useful for compensating for problems with glyph alignment.
	 */
	public void moveLabels() {
		align_glyphs = cglyph.getChildren();
		if (align_glyphs == null) { return; }
		Rectangle2D.Double acoords;
		AlignmentGlyph align;
		ArrowGlyph arrow;
		GlyphI label;
		for (int i=0; i<align_glyphs.size(); i++) {
			align = (AlignmentGlyph)align_glyphs.get(i);
			label = (GlyphI)labelhash.get(align);

			acoords = align.getCoordBox();
			if (label != null) {
				label.setCoords(label.getCoordBox().x, acoords.y,
						label.getCoordBox().width, acoords.height);
			}
			if (use_label_arrows) {
				arrow = arrowhash.get(align);
				if (arrow != null) {
					arrow.setCoords(arrow.getCoordBox().x, acoords.y,
							arrow.getCoordBox().width, acoords.height);
					arrow.setForward(align.isForward());
				}
			}
		}
	}

	/**
	 * positions the given component of this Widget.
	 * Axis can be top or bottom.
	 * Consensus can be top or bottom.
	 * Label and offset scroller can be left or right.
	 *
	 * @param component one of AXIS_SCROLLER, OFFSET_SCROLLER,
	 *                         CONSENSUS, or LABELS.
	 * @param placement one of PLACEMENT_TOP, PLACEMENT_BOTTOM,
	 *                         PLACEMENT_LEFT, or PLACEMENT_RIGHT.
	 */
	public void configureLayout(int component, int placement) {
	    //	    System.out.println("in NeoAssembler.configureLayout()");
		if (component == AXIS_SCROLLER) {
			hscroll_loc = placement;
		}
		else if (component == OFFSET_SCROLLER) {
			vscroll_loc = placement;
		}
		else if (component == CONSENSUS) {
			cons_loc = placement;
		}
		else if (component == LABELS) {
			label_loc = placement;
		}
		else {
			throw new IllegalArgumentException("unknown component");
		}
		doLayout();
		// trying to fix paint issues when configuring layout
		Container parent = getParent();
		if (parent instanceof NeoPanel) {
			((NeoPanel)parent).forceBackgroundFill();
		}
		repaint();
	}

	public int getPlacement(int component) {
		if (component == AXIS_SCROLLER)  { return hscroll_loc; }
		else if (component == OFFSET_SCROLLER) { return vscroll_loc; }
		else if (component == CONSENSUS)  { return cons_loc; }
		else if (component == LABELS)   { return label_loc; }
		else {
			throw new IllegalArgumentException("unknown component");
		}
	}

	/**
	 * Sets the bounds of this component.
	 * This is for backward compatibility with Java 1.0
	 * but is also needed with some 1.1 VMs
	 * because some layout managers still use this method.
	 *
	 * <p> Note that we must still call the deprecated
	 * super.setRect to avoid an infinite loop
	 * due to a bug in Sun's classes.
	 *
	 * @deprecated use setBounds (but override setRect).
	 */
	@Deprecated
		public synchronized void reshape(int x, int y, int width, int height) {
			super.reshape(x, y, width, height);
			alignmap.adjustScroller(NeoMap.Y);
		}

	@Override
	public synchronized void doLayout() {
		// this assumes that NeoAssembler has no insets
		//   but I'm pretty sure the base Panel class never has insets...

		// Layout Rules:
		// scrollbar width, label map width, and consensus map height
		//   are fixed (for the purposes of layout);
		// horizontal scrollbar can be placed top or bottom
		// vertical scrollbar can be placed left or right
		// consensus map can be placed top or bottom
		// label map can be placed left or right
		// scrollbars are always on outer edges, regardless of consensus
		//       and label placement
		// alignment map takes remaining rectangle
		// zoombar takes the corner with the most space (in row with
		//     consensus map, in column with labelmap)
		// alignment map height = label map height = vertical scroll length
		// alignment map width = consensus map width = horizontal scroll length
		// alignment map ybeg = label map ybeg = vertical scroll ybeg
		// alignment map xbeg = consensus map xbeg = horizontal scroll xbeg
		Dimension dim = this.getSize();
		int cons_y=0, label_x=0;
		int align_x=0, align_y=0, align_height=0, align_width=0;
		int hscroll_y=0, vscroll_x=0;
		// corner that zoombar fits in
		int corner_x=0, corner_y=0, corner_width=0, corner_height=0;
		align_height = dim.height - cons_height - scroll_size;
		align_width = dim.width - label_width - scroll_size;

		if (vscroll_loc == PLACEMENT_LEFT && label_loc == PLACEMENT_LEFT) {
			vscroll_x = 0;
			label_x = scroll_size;
			align_x = scroll_size + label_width;
			corner_x = 0;
			corner_width = scroll_size + label_width;
		}
		else if (vscroll_loc == PLACEMENT_LEFT && label_loc == PLACEMENT_RIGHT) {
			vscroll_x = 0;
			label_x = dim.width - label_width;
			align_x = scroll_size;
			corner_x = label_x;
			corner_width = label_width;
		}
		else if (vscroll_loc == PLACEMENT_RIGHT && label_loc == PLACEMENT_LEFT) {
			vscroll_x = dim.width - scroll_size;
			label_x = 0;
			align_x = label_width;
			corner_x = 0;
			corner_width = label_width;
		}
		else if (vscroll_loc == PLACEMENT_RIGHT && label_loc == PLACEMENT_RIGHT) {
			vscroll_x = dim.width - scroll_size;
			label_x = dim.width - scroll_size - label_width;
			align_x = 0;
			corner_x = label_x;
			corner_width = label_width + scroll_size;
		}

		if (hscroll_loc == PLACEMENT_TOP && cons_loc == PLACEMENT_TOP) {
			hscroll_y = 0;
			cons_y = scroll_size;
			align_y = scroll_size + cons_height;
			corner_y = 0;
			corner_height = scroll_size + cons_height;
		}
		else if (hscroll_loc == PLACEMENT_TOP && cons_loc == PLACEMENT_BOTTOM) {
			hscroll_y = 0;
			cons_y = dim.height - cons_height;
			align_y = scroll_size;
			corner_y = cons_y;
			corner_height = cons_height;
		}
		else if (hscroll_loc == PLACEMENT_BOTTOM && cons_loc == PLACEMENT_TOP) {
			hscroll_y = dim.height - scroll_size;
			cons_y = 0;
			align_y = cons_height;
			corner_y = 0;
			corner_height = cons_height;
		}
		else if (hscroll_loc == PLACEMENT_BOTTOM && cons_loc == PLACEMENT_BOTTOM) {
			hscroll_y = dim.height - scroll_size;
			cons_y = dim.height - scroll_size - cons_height;
			align_y = 0;
			corner_y = cons_y;
			corner_height = cons_height + scroll_size;
		}

		labelmap.setBounds(label_x, align_y, label_width, align_height);
		consmap.setBounds(align_x, cons_y, align_width, cons_height);
		alignmap.setBounds(align_x, align_y, align_width, align_height);

		((Component)hscroll).setBounds(align_x, hscroll_y, align_width, scroll_size);
		((Component)hscroll).setSize(align_width, scroll_size);
		((Component)vscroll).setBounds(vscroll_x, align_y, scroll_size, align_height);
		((Component)vscroll).setSize(scroll_size, align_height);

		if (internal_zoomer) {
		    ((Component)hzoom).setBounds(corner_x, corner_y,
						 corner_width, zoom_size);
		    ((Component)hzoom).setSize(corner_width, zoom_size);
		}
		conslabelmap.setBounds(corner_x,
				corner_y + corner_height - zoom_size,
				corner_width, zoom_size);

		// scale perpendicular to axis is always 1
		consmap.zoomOffset(1.0f);
		alignmap.zoomOffset(1.0f);
		labelmap.zoomOffset(1.0f);

	}


	protected Sequence createReference(Mapping consensus) {
		String seqstring = consensus.getSequence().getResidues();
		int refstart, refend, reflength;
		refstart = consensus.getStart();
		refend = consensus.getEnd();
		checkRange(refstart, refend);
		reflength = refend + 1;
		StringBuffer refbuf = new StringBuffer(reflength);
		int i;
		for (i=0; i<reflength; i++) {
			refbuf.append('*');
		}
		List<Span> spans = consensus.getSpans();
		if (spans != null) {
			int seqindex, refindex;
			for (Span s : spans) {
				refindex = s.ref_start;
				if (seqstring != null) {
					for (seqindex = s.seq_start;
									seqindex < seqstring.length() && seqindex <= s.seq_end;
									seqindex++) {
						try {
							refbuf.setCharAt(refindex, seqstring.charAt(seqindex));
						} catch (StringIndexOutOfBoundsException e) {
							refbuf.setCharAt(refindex, '*');
						}
						refindex++;
					}
				}
			}
		}
		Sequence ref = new Sequence();
		ref.setResidues(refbuf.toString());
		return ref;
	}

	/**
	 * Assuming an already sorted vector of Comparable objects,
	 * can add another Comparable by finding the vector position
	 * where it sorts to, and inserting it
	 *
	 * Should move this method and QuickSorter, InsertionSort classes
	 *  to a Sorter util class
	 */
	protected int getSortedPosition(Comparable<AlignmentGlyph> elem, List<GlyphI> vec) {
		if (vec == null) {
			return 0;
		}
		int max = vec.size();
		for (int i=0; i<max; i++) {
			if (vec.get(i) instanceof AlignmentGlyph && elem.compareTo((AlignmentGlyph)vec.get(i)) < 0) {
				return i;
			}
		}
		return max;
	}

	public int getLocation(NeoAbstractWidget widg) {
		if (widg == labelmap) { return LABELS; }
		else if (widg == alignmap) { return ALIGNMENTS; }
		else if (widg == consmap) { return CONSENSUS; }
		else if (widg == conslabelmap) { return CONSENSUS_LABEL; }
		throw new IllegalArgumentException("unknown widget");
	}

	public NeoAbstractWidget getWidget(int location) {
		if (location == LABELS) { return labelmap; }
		else if (location == ALIGNMENTS) { return alignmap; }
		else if (location == CONSENSUS) { return consmap; }
		else if (location == CONSENSUS_LABEL) { return conslabelmap; }
		throw new IllegalArgumentException("unknown location");
	}

	/**
	 * This implements the selection method portion of NeoMap.
	 * That portion of NeoMap might move to NeoAbstractWidget
	 * or to a new interface of its own.
	 * @see NeoMap
	 */
	public void setSelectionEvent(int theEvent) {
		alignmap.setSelectionEvent(theEvent);
		consmap.setSelectionEvent(theEvent);
		labelmap.setSelectionEvent(theEvent);
	}
	public int getSelectionEvent() {
		return alignmap.getSelectionEvent();
	}

	public void setSelectionBehavior(int behavior) {
		if (SELECT_ALIGNMENTS == behavior) {
			setSelectionAppearance(SceneI.SELECT_FILL);
			selection_behavior = behavior;
		}
		else if (SELECT_RESIDUES == behavior) {
			setSelectionAppearance(SceneI.SELECT_OUTLINE);
			selection_behavior = behavior;
		}
		else {
			throw new
				IllegalArgumentException("selection behavior must be "
						+ "SELECT_ALIGNMENTS or SELECT_RESIDUES");
		}
	}

	public int getSelectionBehavior() {
		return this.selection_behavior;
	}


	// ItemSelectable Implementation

	private ItemListener listener = null;

	public void addItemListener( ItemListener l ) {
		this.listener = AWTEventMulticaster.add( this.listener, l );
	}

	public void removeItemListener( ItemListener l ) {
		this.listener = AWTEventMulticaster.remove( this.listener, l );
	}

	protected void fireItemEvent( ItemEvent e ) {
		if ( null != this.listener ) {
			this.listener.itemStateChanged( e );
		}
	}

	public Object[] getSelectedObjects() {
		List<GlyphI> v = this.labelmap.getSelected();
		if ( null == v || v.size() < 1 ) {
			return null;
		}
		Object[] o = new Object[v.size()];
		for ( int i = 0; i < o.length; i ++ ) {
			Object g = v.get( i );
			if ( g instanceof StringGlyph ) {
				StringGlyph sg = ( StringGlyph ) g;
				o[i] = sg.getString();
			}
			else {
				o[i] = g.toString();
			}
		}
		return o;
	}

	/**
	 * selects a label.
	 *
	 * @param offset the label is at.
	 */
	private void selectLabel( double offset ) {
		List<GlyphI> v = this.labelmap.getItems( 10, offset );
		for ( int i = 0; i < v.size(); i++ ) {
			GlyphI o = v.get( i );
			if ( o instanceof StringGlyph ) {
				StringGlyph g = ( StringGlyph ) o;
				this.labelmap.select( g );
				String s = g.getString();
				fireItemEvent( new ItemEvent( this,
							ItemEvent.ITEM_STATE_CHANGED,
							s,
							ItemEvent.SELECTED ) );
			}
		}
	}

	int select_start, select_end;

	@Override
	public void heardMouseEvent(MouseEvent evt) {
		int id = evt.getID();
		Object source = evt.getSource();
		if (evt instanceof NeoMouseEvent) {
			NeoMouseEvent nevt = (NeoMouseEvent)evt;
			if (SELECT_ALIGNMENTS == selection_behavior) {
				if ((id == MouseEvent.MOUSE_PRESSED &&
							alignmap.getSelectionEvent() == ON_MOUSE_DOWN) ||
						(id == MouseEvent.MOUSE_RELEASED &&
						 alignmap.getSelectionEvent() == ON_MOUSE_UP)) {

					if (source == alignmap) {
						consmap.deselect(consmap.getSelected());
						labelmap.deselect(labelmap.getSelected());
						consmap.updateWidget();
						labelmap.updateWidget();
					}
					else if (source == consmap) {
						alignmap.deselect(alignmap.getSelected());
						labelmap.deselect(labelmap.getSelected());
						alignmap.updateWidget();
						labelmap.updateWidget();
					}
					else if (source == labelmap) {
						consmap.deselect(consmap.getSelected());
						alignmap.deselect(alignmap.getSelected());
						consmap.updateWidget();
						alignmap.updateWidget();
					}
						 }

				List<GlyphI> a = new ArrayList<GlyphI>(alignmap.getSelected());
				// a must be a clone! getSelected() returns the real thing.
				a.addAll(consmap.getSelected());

				this.selected = a;
			}

			else if (SELECT_RESIDUES == selection_behavior) {
				if ((id == MouseEvent.MOUSE_PRESSED &&
							alignmap.getSelectionEvent() == ON_MOUSE_DOWN) ||
						(id == MouseEvent.MOUSE_RELEASED &&
						 alignmap.getSelectionEvent() == ON_MOUSE_UP)) {
					if (source == labelmap) {
						consmap.deselect(consmap.getSelected());
						alignmap.deselect(alignmap.getSelected());
						consmap.updateWidget();
						alignmap.updateWidget();
						selectLabel( nevt.getCoordY() );
						this.labelmap.updateWidget();
					}
					else if (source == consmap || source == alignmap) {
						NeoMap selmap = (NeoMap)source;
						labelmap.deselect(labelmap.getSelected());
						labelmap.updateWidget();

						// currently need to clear auto-selection in alignmap and consmap
						//    (keep track of subselection with select_start / select_end)
						alignmap.deselect(alignmap.getSelected());
						consmap.deselect(consmap.getSelected());
						List<GlyphI> items = selmap.getItems(nevt.getCoordX(),
								nevt.getCoordY());
						sel_glyph = null;
						for (GlyphI item : items) {
							if (item instanceof AbstractResiduesGlyph) {
								sel_glyph = item;
								// CoordX returns reference coord
								select_start = (int)nevt.getCoordX();
								select_end = select_start;
								selmap.select(item, select_start, select_end);
								if( selmap == consmap ) {
									selectBaseRangeOnAllResidues( select_start, select_end );
								}
								else {
									selectBaseRangeOnConsensus( select_start, select_end);
								}
								break;
							}
						}
						alignmap.updateWidget();
						consmap.updateWidget();
						if ( source == alignmap ) {
							selectLabel( nevt.getCoordY() );
							this.labelmap.updateWidget();
						}
					}
						 }
				else if (id == MouseEvent.MOUSE_DRAGGED
						&& NeoMap.NO_SELECTION != alignmap.getSelectionEvent()) {
					if (source == alignmap || source == consmap) {
						NeoMap selmap = (source == alignmap) ? alignmap : consmap;
						if (select_end != (int)nevt.getCoordX() && sel_glyph != null) {
							select_end = (int)nevt.getCoordX();
							selmap.select(sel_glyph, select_start, select_end);

							if( selmap == consmap ) {
								selectBaseRangeOnAllResidues( select_start, select_end );
								alignmap.updateWidget();
							}
							else {
								selectBaseRangeOnConsensus( select_start, select_end);
								consmap.updateWidget();
							}
							selmap.updateWidget();
						}
					}
						}
				else {
					// System.err.println("MouseEvent not caught");
				}

			}
			else {
				// System.err.println("No selection method specified");
			}
		}
		super.heardMouseEvent(evt);
	}

	public void select(GlyphI gl, int start, int end) {
		Scene sc = gl.getScene();
		NeoMap map = getMap(sc);
		if (map == null) { return; }
		map.select(gl, start, end);
		if (!selected.contains(gl)) {
			selected.add(gl);
		}
	}

	@Override
	public void clearSelected() {
		for (int i=0; i<selected.size(); i++) {
			GlyphI gl = selected.get(i);
			gl.getScene().deselect(gl);
		}
		selected.clear();
	}

	@Override
	public List<GlyphI> getSelected() {
		return selected;
	}
	//-------------------------------------------

	public void updateMap(boolean alignments, boolean consensus, boolean labels) {
		if (alignments)  alignmap.updateWidget();
		if (consensus)   consmap.updateWidget();
		if (labels)      labelmap.updateWidget();
	}

	public void updateMap(int id) {
		if (id == ALIGNMENTS) {
			alignmap.updateWidget();
		}
		else if (id == CONSENSUS) {
			consmap.updateWidget();
		}
		else if (id == LABELS) {
			labelmap.updateWidget();
		}
		else {
			throw new IllegalArgumentException(
					"can only update ALIGNMENTS, CONSENSUS, or LABELS");
		}
	}

	public void addDataAdapter(NeoDataAdapterI adapter) {
		if (adapter == null) {
			throw new NullPointerException("cannot add a null NeoDataAdapterI.");
		}
		if (adapters == null) {
			adapters = new ArrayList<NeoDataAdapterI>();
		}
		adapters.add(adapter);
	}

	public Object addData(Object obj) {
		if (obj == null) {
			throw new NullPointerException("cannot add a null Object.");
		}
		NeoDataAdapterI da;
		GlyphI glyph;
		if (adapters == null) {
			throw new NullPointerException("cannot addData if adapters is null.");
		}
		for (int i=0; i<adapters.size(); i++) {
			da = adapters.get(i);
			if (da.accepts(obj)) {
				glyph = da.createGlyph(obj);
				return glyph;
			}
		}
		throw new RuntimeException("no adapters accept " + obj);
	}

	@Override
	public void setScrollingOptimized(boolean optimize_scrolling) {
		if (this.optimize_scrolling != optimize_scrolling) {
			this.optimize_scrolling = optimize_scrolling;
			alignmap.setScrollingOptimized(optimize_scrolling);
			labelmap.setScrollingOptimized(optimize_scrolling);
			consmap.setScrollingOptimized(optimize_scrolling);
		}
	}
	public boolean isScrollingOptimized() {
		return this.optimize_scrolling;
	}

	@Override
	public void setDamageOptimized(boolean optimize_damage) {
		if (this.optimize_damage != optimize_damage) {
			this.optimize_damage = optimize_damage;
			alignmap.setDamageOptimized(optimize_damage);
			labelmap.setDamageOptimized(optimize_damage);
			consmap.setDamageOptimized(optimize_damage);
		}
	}
	public boolean isDamageOptimized() {
		return this.optimize_damage;
	}

	public void scrollOffset(double value) {
		// shoudln't have to adjust labelmap
		// labelmap will also be scrolled because alignmap will adjust vscroller,
		//    which will notify labelmap
		// WHY isn't this working?  Probably due to NeoScrollbar not notifying
		//    on value change
		alignmap.scrollOffset(value);
		labelmap.scrollOffset(value);
	}

	public void scrollRange(double value) {
		// shouldn't have to adjust consmap
		// consmap will also be scrolled because alignmap will adjust hscroller,
		//    which will notify consmap
		// WHY isn't this working?  Probably due to NeoScrollbar not notifying
		//    on value change
		alignmap.scrollRange(value);
		consmap.scrollRange(value);
	}

	/**
	 * returns a <code>boolean</code> indicating whether or not alignments
	 * are automatically sorted.
	 *
	 * @return <code>true</code> if alignments are automatically sorted.
	 */
	public boolean getAutoSort() {
		return auto_sort;
	}

	/**
	 * Sets whether or not alignments are to be sorted automatically.
	 *
	 * @param sort <code>true</code> indicates that alignments
	 *   should be automatically sorted.
	 */
	public void setAutoSort(boolean sort) {
		auto_sort = sort;
		if (auto_sort) {
			if (all_sorted == false) {
				if (cglyph.getChildren() != null) {
					//QuickSorter.sort(cglyph.getChildren());
					Collections.sort(cglyph.getChildren(), comparatorHack);
					pack();
					all_sorted = true;
				}
			}
		}
	}

	/**
	 *  Pack (or repack) the aligned sequences
	 */
	public void pack()  {
		if (cglyph == null || alignmap == null)  {
			return;
		}
		ViewI pack_view = alignmap.getView();
		List align_glyphs = cglyph.getChildren();
		if (pack_view == null || align_glyphs == null) {
			return;
		}
		cglyph.pack(pack_view);

		Rectangle2D.Double cbox = cglyph.getCoordBox();
		alignmap.setMapOffset((int)cbox.y, (int)(cbox.y + cbox.height));
		labelmap.setMapOffset((int)cbox.y, (int)(cbox.y + cbox.height));
		moveLabels();
	}

	public Object addItem(int start, int end) {
		return alignmap.addItem(start, end);
	}

	public Object addItem(int start, int end, String options) {
		return alignmap.addItem(start, end, options);
	}

	public NeoMap getMap(Scene s) {
		if (s == align_scene) { return alignmap; }
		else if (s == cons_scene) { return consmap; }
		else if (s == label_scene) { return labelmap; }
		throw new IllegalArgumentException("unknown scene");
	}


	public void resetUnalignedColors() {
		List parents = getAlignmentGlyphs();
		AlignmentGlyph parent;
		List<AlignedResiduesGlyph> unaligned_spans;
		for (int i=0; i<parents.size(); i++) {
			parent = (AlignmentGlyph)parents.get(i);
			unaligned_spans = parent.getUnalignedSpans();
			for (AlignedResiduesGlyph arglyph : unaligned_spans) {
				arglyph.setBackgroundColor(unaligned_rect_color);
				arglyph.setForegroundColor(unaligned_font_color);
			}
		}
	}



	// should probably be in a NeoResidueI interface?
	// since it is also needed in NeoSeq, NeoMap, NeoQualler, NeoTracer
	// at least for NeoAssembler, should throw error if set to anything
	// other than Courier
	/**
	 * sets the font to be used for displaying residue letters.
	 *
	 * @param fnt  the Font to use for residue letters
	 * @see #getResidueFont
	 */
	public void setResidueFont(Font fnt) {
		residue_font = fnt;
		FontMetrics fontmet = GeneralUtils.getFontMetrics(residue_font);
		if (cons_glyph != null)  { cons_glyph.setResidueFont(residue_font); }
		Object child;
		ResiduesGlyphI rglyph;
		if (cglyph != null) {
			List aligns = cglyph.getChildren();
			if (aligns != null) {
				for (int i=0; i<aligns.size(); i++) {
					child = aligns.get(i);
					if (child instanceof ResiduesGlyphI) {
						rglyph = (ResiduesGlyphI)child;
						rglyph.setResidueFont(residue_font);
					}
				}
			}
		}
		if (fontControlsMaxZoom) {
			int font_width =
				GeneralUtils.getMaxCharWidth(fnt, DNAUtils.getAllowedDNACharacters());
			setMaxZoom(NeoAbstractWidget.X, font_width);
		}

		if (fontControlsGlyphHeight) {
			align_glyph_height = fontmet.getAscent() + 2;
			Rectangle2D.Double glyphbox;
			if (cons_glyph != null)  {
				glyphbox = cons_glyph.getCoordBox();
				cons_glyph.setCoords(glyphbox.x, glyphbox.y,
						glyphbox.width, align_glyph_height);
			}
			AlignmentGlyph seq_glyph;
			if (cglyph != null) {
				List aligns = cglyph.getChildren();
				if (aligns != null) {
					for (int i=0; i<aligns.size(); i++) {
						child = aligns.get(i);
						if (child instanceof AlignmentGlyph) {
							seq_glyph = (AlignmentGlyph)child;
							glyphbox = seq_glyph.getCoordBox();
							seq_glyph.setCoords(glyphbox.x, glyphbox.y,
									glyphbox.width, align_glyph_height);
							List child2 = seq_glyph.getChildren();

							for (int k=0; k<child2.size(); k++){
								GlyphI glyph = (GlyphI)child2.get(k);
								glyphbox = glyph.getCoordBox();
								glyph.setCoords(glyphbox.x, glyphbox.y,
										glyphbox.width, align_glyph_height);
							}
						}
					}
				}
			}
			pack();

			Rectangle2D.Double cbox = cglyph.getCoordBox();
			alignmap.setMapOffset((int)cbox.y, (int)(cbox.y + cbox.height));
			labelmap.setMapOffset((int)cbox.y, (int)(cbox.y + cbox.height));
			alignmap.adjustScroller(alignmap.Y);
		}

	}

	/**
	 * returns the currently set font used for display residue letters.
	 *
	 * @return the <code>Font</code> currently set.
	 * @see #setResidueFont
	 */
	public Font getResidueFont() {
		return residue_font;
	}


	/**
	 *  Zoom affects alignments in both dimensions
	 *  labels are only affected by zoom along Y
	 *  consensus (and axis) are only affected by zoom along X
	 */
	public void zoom(int id, double zoom_scale) {
		if (id == X) {
			alignmap.zoom(id, zoom_scale);
			consmap.zoom(id, zoom_scale);
		}
		else if (id == Y)  {
			alignmap.zoom(id, zoom_scale);
			labelmap.zoom(id, zoom_scale);
		}
		else {
			throw new IllegalArgumentException("NeoAssembler.zoom() id argument " +
					"must be either NeoAssembler.X or NeoAssembler.Y");
		}
	}

	/**
	 *  Scrolling affects alignments in both dimensions
	 *  labels are only affected by scrolling along Y
	 *  consensus (and axis) are only affected by scrolling along X
	 */
	@Override
	public void scroll(int id, double value) {
		if (id == X) {
			alignmap.scroll(id, value);
			consmap.scroll(id, value);
		}
		else if (id == Y)  {
			alignmap.scroll(id, value);
			labelmap.scroll(id, value);
		}
		else {
			throw new IllegalArgumentException("NeoAssembler.zoom() id argument " +
					"must be either NeoAssembler.X or NeoAssembler.Y");
		}
	}

	@Override
	public void setMinZoom(int axisid, double scale) {
		throw new IllegalArgumentException("NeoAssembler.setMinZoom() " +
				"not yet supported");
	}

	@Override
	public void setMaxZoom(int axisid, double scale) {
		if (axisid == NeoAbstractWidget.X) {
			consmap.setMaxZoom(axisid, scale);
			alignmap.setMaxZoom(axisid, scale);
			alignmap.adjustZoomer(axisid);
		}
		else {
			throw new IllegalArgumentException("NeoAssembler.setMaxZoom() " +
					"will only adjust max zoom along NeoAbstractWidget.X");
		}
	}

	@Override
	public void removeItem(GlyphI item) {
		if (item instanceof AlignmentGlyph) {
			AlignmentGlyph itemglyph = (AlignmentGlyph)item;
			GlyphI lglyph = (GlyphI)labelhash.get(itemglyph);
			GlyphI aglyph = (GlyphI)arrowhash.get(itemglyph);
			if (lglyph != null) {
				labelhash.remove(itemglyph);
				removeItem(lglyph);
			}
			if (aglyph != null) {
				arrowhash.remove(itemglyph);
				removeItem(aglyph);
			}
		}
		super.removeItem(item);
	}

	/**
	 * Call NeoContainerWidget.clearWidget()
	 * to reset inner widget's scenes, etc.
	 * Note that this does not remove any data adapters.
	 */
	@Override
	public void clearWidget() {
		super.clearWidget();
		setUpAlignMap();
		setUpConsensusMap();
		setUpLabelMap(labelmap);
		label_scene = labelmap.getScene();
		setUpLabelMap(conslabelmap);

		setHorizontalScroller (hscroll);
		setVerticalScroller   (vscroll);
		setInternalZoomer     (hzoom);

		setRange(0, 0);

		align_glyphs.clear();
		labelhash.clear();
		arrowhash.clear();
		ref_seq = null;
		cons_seq = null;
		cons_align = null;
		cons_glyph = null;

	}

	/**
	 * Alignments are affected by zoom behavior along both X and Y dimensions.
	 * Labels are only affected along Y dimension.
	 * Consensus and axis are only affected along X dimension.
	 * @param id should be {@link #X} or {@link #Y}.
	 * @param behavior {@link #CONSTRAIN_START},
	 *                 {@link #CONSTRAIN_MIDDLE}, or
	 *                 {@link #CONSTRAIN_END}.
	 * @see NeoAbstractWidget
	 */
	@Override
	public void setZoomBehavior(int id, int behavior) {
		zoom_behavior[id] = behavior;
		alignmap.setZoomBehavior(id, behavior);
		if (id == X) {
			consmap.setZoomBehavior(id, behavior);
		}
		else if (id == Y) {
			labelmap.setZoomBehavior(id, behavior);
		}
	}

	@Override
	public void setZoomBehavior(int id, int behavior, double coord) {
		zoom_behavior[id] = behavior;
		alignmap.setZoomBehavior(id, behavior, coord);
		if (id == X) {
			consmap.setZoomBehavior(id, behavior, coord);
		}
		else if (id == Y) {
			labelmap.setZoomBehavior(id, behavior, coord);
		}
	}

	public int getZoomBehavior(int id) {
		return zoom_behavior[id];
	}

	/**
	 * Alignments are affected by setRect behavior along both X and Y dimensions.
	 * Labels are only affected along Y dimension.
	 * Consensus and axis are only affected along X dimension.
	 * @param id should be {@link #X} or {@link #Y}.
	 * @param behavior {@link #FITWIDGET} or {@link #FITWIDGET};
	 * @see NeoWidget
	 */
	public void setReshapeBehavior(int id, int behavior) {
		reshape_constraint[id] = behavior;
		alignmap.setReshapeBehavior(id, behavior);
		if (id == X) {
			consmap.setReshapeBehavior(id, behavior);
		}
		else if (id == Y) {
			labelmap.setReshapeBehavior(id, behavior);
		}
	}

	public int getReshapeBehavior(int id) {
		return reshape_constraint[id];
	}

	public void setBackground(int id, Color col) {
		switch (id) {
			case ALIGNMENTS: alignmap.setMapColor(col); break;
			case CONSENSUS: consmap.setMapColor(col); break;
			case LABELS: labelmap.setMapColor(col); break;
			case CONSENSUS_LABEL: conslabelmap.setMapColor(col); break;
			default:
								  throw new IllegalArgumentException("NeoAssembler.setBackground(id, " +
										  "color) currently only supports ids of " +
										  "ALIGNMENTS, CONSENSUS, or LABELS");
		}
	}

	public Color getBackground(int id) {
		switch (id) {
			case ALIGNMENTS: return alignmap.getMapColor();
			case CONSENSUS: return consmap.getMapColor();
			case LABELS: return labelmap.getMapColor();
			case CONSENSUS_LABEL: return conslabelmap.getMapColor();
		}
		throw new IllegalArgumentException("NeoAssembler.getBackground(id) " +
				"currently only supports ids of " +
				"ALIGNMENTS, CONSENSUS, or LABELS");
	}

	public void setAlignmentsBackground(Color theColor) {
		setBackground(ALIGNMENTS, theColor);
	}
	public Color getAlignmentsBackground() {
		return getBackground(ALIGNMENTS);
	}
	public void setConsensusBackground(Color theColor) {
		setBackground(CONSENSUS, theColor);
	}
	public Color getConsensusBackground() {
		return getBackground(CONSENSUS);
	}
	public void setLabelsBackground(Color theColor) {
		setBackground(LABELS, theColor);
	}
	public Color getLabelsBackground() {
		return getBackground(LABELS);
	}
	public void setConsensusLabelBackground(Color theColor) {
		setBackground(CONSENSUS_LABEL, theColor);
	}
	public Color getConsensusLabelBackground() {
		return getBackground(CONSENSUS_LABEL);
	}
	/**
	 * @return the glyph for the consensus sequence.
	 */
	public GlyphI getConsensusGlyph() {
		return cons_glyph;
	}

	/**
	 * Set an EXTERNAL zoomer
	 * for either the X (horizontal) or Y (vertical) dimension.
	 * @param id {@link #X} or {@link #Y}.
	 * @param adj The adjustable to use.
	 */

	@Override
	public void setZoomer(int id, Adjustable adj) {
		if (adj == null) {
			throw new IllegalArgumentException("NeoAssembler.setZoomer() requires "
					+ "an Adjustable argument, was passed a null instead");
		}
		if (id == X)  {
			alignmap.setZoomer(id, adj);
			consmap.setZoomer(id, adj);
		}
		else if (id == Y) {
			alignmap.setZoomer(id, adj);
			labelmap.setZoomer(id, adj);
		}
		else {
			throw new IllegalArgumentException("NeoAssembler.setZoomer() id "
					+ "argument must be NeoAssembler.X or NeoAssembler.Y");
		}
	}

	/**
	 * Set an <em>INTERNAL</em> horizontal zoomer.
	 * If the given Adjustable is not an instance of Component,
	 * the call will be ignored.
	 */
	public void setInternalZoomer (Adjustable adjustable) {

		if (!(adjustable instanceof Component) || (adjustable == null))
			return;

		remove ((Component)hzoom);
		hzoom = adjustable;
		add    ((Component)hzoom);

		alignmap.setZoomer(X, hzoom);
		consmap.setZoomer (X, hzoom);
	}

	/**
	 * Get the <em>internal</em> horizontal zoomer.
	 */
	public Adjustable getInternalZoomer () {
		return hzoom;
	}

	/**
	 * Determines whether or not the labels also have arrows
	 * attached to show direction of sequences they label.
	 * This will ONLY affect sequences added after this call.
	 */
	public void setUseLabelArrows(boolean use_label_arrows) {
		this.use_label_arrows = use_label_arrows;
	}

	/**
	 * Determines whether or not the labels also have arrows
	 * attached to show direction of sequences they label.
	 */
	public boolean getUseLabelArrows() {
		return use_label_arrows;
	}

	/**
	 * Set the pixel width allocated to labels.
	 * If not called, default is 100 pixels
	 * It is recommended that you set this
	 * prior to adding any aligned sequences to the NeoAssembler
	 */
	public void setLabelWidth(int label_width) {
		this.label_width = label_width;
		labelmap.setBounds(NeoMap.X, 0, label_width-1);
		doLayout();
	}

	/**
	 * @return the pixel width allocated to labels.
	 */
	public int getLabelWidth() {
		return label_width;
	}

	/** Adjusts <em>background</em> color matrix. */
	public void adjustColorMatrix(Color match, Color mismatch,
			boolean apply_retro) {
		adjustColorMatrix(match, mismatch, unknown_rect_color, apply_retro);
	}

	/** Adjusts <em>background</em> color matrix. */
	public void adjustColorMatrix(Color match, Color mismatch, Color unknown,
			boolean apply_retro) {
		adjustColorMatrix(match, mismatch, unknown, apply_retro, bg_color_matrix);
	}

	/** Adjust either bg_color_matrix or fg_color_matrix. */
	public void adjustColorMatrix(Color match, Color mismatch, Color unknown,
			boolean apply_retro, Color[][] color_matrix) {
		// could actually do to length-1 (since matrix is +1 larger to allow for
		//    unknown -- which is inefficient here, but saves a lot of memory
		//    by not requiring separate ref for each AlignedResiduesGlyph
		for (int i=0; i<color_matrix.length; i++) {
			for (int j=0;j<color_matrix.length; j++) {
				if (i == j) {
					color_matrix[i][j] = match;
				}
				else {
					color_matrix[i][j] = mismatch;
				}
			}
		}

		// null out space in seq to anything in consensus
		int[] charToId = DNAUtils.getNACharToIdMap();
		for (int i=0; i<color_matrix.length; i++) {
			color_matrix[charToId[' ']][i] = null;
		}

		// deal with unknown residue color
		color_matrix[color_matrix.length-1][color_matrix.length-1] = unknown;

		if (apply_retro) {
			// actually this should all automatically apply, since glyphs already
			//   point to bg_color_matrix -- but leaves open the possibility of
			//   specifying different matrices for different alignment glyphs,
			//   if switch to making a new matrix with each call to
			//   adjustColorMatrix()

			List<GlyphI> align_glyphs = this.getAlignmentGlyphs();
			AlignmentGlyph gar;
			if (color_matrix == bg_color_matrix) {
				for (int i=0; i<align_glyphs.size(); i++) {
					gar = (AlignmentGlyph) align_glyphs.get(i);
					gar.setBackgroundColorMatrix(color_matrix);
				}
				if (colors_affect_cons && cons_glyph != null) {
					cons_glyph.setBackgroundColorMatrix(color_matrix);
				}
			}
			else if (color_matrix == fg_color_matrix) {

				for (int i=0; i<align_glyphs.size(); i++) {
					gar = (AlignmentGlyph)align_glyphs.get(i);
					gar.setForegroundColorStrategy(AlignmentGlyph.ALIGNMENT_BASED);
					gar.setForegroundColorMatrix(color_matrix);
				}
				if (colors_affect_cons && cons_glyph != null) {
					cons_glyph.setForegroundColorStrategy(AlignmentGlyph.ALIGNMENT_BASED);
					cons_glyph.setForegroundColorMatrix(color_matrix);
				}
			}
		}
	}

	public List<GlyphI> getAlignmentGlyphs() {
		return align_glyphs;
	}

	public Rectangle2D.Double getCoordBounds() {
		return alignmap.getCoordBounds();
	}

	public void setConsensusHeight(int cons_height) {
		this.cons_height = cons_height;
		doLayout();
	}

	public void showAxis(boolean show) {
		if (show_axis == show) { return; }
		show_axis = show;
		if (!show_axis) {
			cons_offset = 5;
		}
		else {
			cons_offset = axis_offset + 5;
		}
		consmap.removeItem(axis_glyph);
	}

	public GlyphI getAxis () { return axis_glyph; }

	/**
	 * returns the currently set color for displaying residues matching
	 * the consensus sequence.
	 *
	 * @return the <code>Color</code> of matching residues
	 * @see #setMatchColor
	 * @deprecated
	 */
	@Deprecated
	public Color getMatchColor() {
		return getMatchRectColor();
	}

	/**
	 * returns the currently set color for displaying residues
	 * <b>not</b> matching the consensus sequence.
	 *
	 * @return the <code>Color</code> of mismatching residues
	 * @see #setMisMatchColor
	 * @deprecated
	 */
	@Deprecated
	public Color getMisMatchColor() {
		return getMisMatchRectColor();
	}

	@Deprecated
	public Color getUnrecognizedColor() {
		return getUnrecognizedRectColor();
	}

	/**
	 * returns the currently set default color for displaying residue letters.
	 *
	 * @return the <code>Color</code> for displaying residues
	 * @see #setResidueColor
	 * @deprecated
	 */
	@Deprecated
	public Color getResidueColor() {
		return getResidueFontColor();
	}

	/**
	 * sets the color to be used for displaying residues matching
	 * the consensus sequence.
	 *
	 * @param col  the <code>Color</code> for matching residues
	 * @see #getMatchColor
	 * @deprecated
	 */
	@Deprecated
	public void setMatchColor(Color col) {
		setMatchFontColor(col);
	}

	/**
	 * sets the color to be used for displaying residues <b>not</b> matching
	 * the consensus sequence.
	 *
	 * @param col  the <code>Color</code> for mismatching residues
	 * @see #getMisMatchColor
	 * @deprecated
	 */
	@Deprecated
	public void setMisMatchColor(Color col) {
		setMisMatchRectColor(col);
	}

	@Deprecated
	public void setUnrecognizedColor(Color col) {
		setUnrecognizedRectColor(col);
	}

	/**
	 * sets the default color for displaying residue letters.
	 *
	 * @param col  the <code>Color</code> to display residues
	 * @see #getResidueColor
	 * @deprecated
	 */
	@Deprecated
	public void setResidueColor(Color col) {
		setResidueFontColor(col);
	}

	@Deprecated
	public void setUnalignedBackgroundColor(Color col) {
		setUnalignedRectColor(col);
	}

	@Deprecated
	public void setUnalignedResidueColor(Color col) {
		setUnalignedFontColor(col);
	}


	public Color getMatchRectColor() {
		return match_rect_color;
	}
	public Color getMisMatchRectColor() {
		return mismatch_rect_color;
	}
	public Color getUnrecognizedRectColor() {
		return unknown_rect_color;
	}
	public Color getResidueFontColor() {
		return residue_color;
	}

	/**
	 * returns the currently set color for displaying the label for each
	 * aligned sequence.
	 *
	 * @return the current label <code>Color</code>
	 * @see #setLabelColor
	 */
	public Color getLabelColor() {
		return label_color;
	}
	
	public Color getMatchFontColor() {
		return match_font_color;
	}
	public Color getMisMatchFontColor() {
		return mismatch_font_color;
	}
	public Color getUnrecognizedFontColor() {
		return unknown_font_color;
	}
	public Color getUnalignedFontColor() {
		return unaligned_font_color;
	}
	public Color getUnalignedRectColor() {
		return unaligned_rect_color;
	}

	public void setMatchFontColor(Color col) {
		if (match_font_color != col) {
			match_font_color = col;
			adjustColorMatrix(match_font_color, mismatch_font_color,
					unknown_font_color, apply_color_retro,
					fg_color_matrix);
		}
	}

	public void setMisMatchFontColor(Color col) {
		if (mismatch_font_color != col) {
			mismatch_font_color = col;
			adjustColorMatrix(match_font_color, mismatch_font_color,
					unknown_font_color, apply_color_retro,
					fg_color_matrix);
		}
	}

	public void setUnrecognizedFontColor(Color col) {
		if (unknown_font_color != col) {
			unknown_font_color = col;
			adjustColorMatrix(match_font_color, mismatch_font_color,
					unknown_font_color, apply_color_retro,
					fg_color_matrix);
		}
	}


	public void setMatchRectColor(Color col) {
		if (match_rect_color != col) {
			match_rect_color = col;
			adjustColorMatrix(match_rect_color, mismatch_rect_color,
					unknown_font_color, apply_color_retro,
					bg_color_matrix);
		}
	}
	public void setMisMatchRectColor(Color col) {
		if (mismatch_rect_color != col) {
			mismatch_rect_color = col;
			adjustColorMatrix(match_rect_color, mismatch_rect_color,
					unknown_font_color, apply_color_retro,
					bg_color_matrix);
		}
	}
	public void setUnrecognizedRectColor(Color col) {
		if (unknown_rect_color != col) {
			unknown_rect_color = col;
			adjustColorMatrix(match_rect_color, mismatch_rect_color,
					unknown_font_color, apply_color_retro, bg_color_matrix);
		}
	}


	public void setUnalignedRectColor(Color col) {
		unaligned_rect_color = col;
		if (apply_color_retro) {
			resetUnalignedColors();
		}
	}

	public void setUnalignedFontColor(Color col) {
		unaligned_font_color = col;
		if (apply_color_retro) {
			resetUnalignedColors();
		}
	}


	/**
	 * sets the color for displaying the label for each aligned sequence.
	 *
	 * @param col  the label <code>Color</code>
	 * @see #getLabelColor
	 */
	public void setLabelColor(Color col) {
		label_color = col;
	}

	/**
	 * Setting residue font color sets font color strategy
	 * @param col
	 */
	public void setResidueFontColor(Color col) {
		font_color_strategy = AlignedResiduesGlyph.FIXED_COLOR;
		residue_color = col;
		if (apply_color_retro) {
			List<GlyphI> align_glyphs = this.getAlignmentGlyphs();
			AlignmentGlyph gar;
			for (int i=0; i<align_glyphs.size(); i++) {
				gar = (AlignmentGlyph) align_glyphs.get(i);
				gar.setForegroundColor(residue_color);
			}
			if (colors_affect_cons && cons_glyph != null)  {
				cons_glyph.setForegroundColor(residue_color);
			}
		}
	}

	public void viewBoxChanged(NeoViewBoxChangeEvent evt) {
		if (evt.getSource() == alignmap) {
			if (range_listeners.size() > 0)  {
				Rectangle2D.Double vbox = evt.getCoordBox();
				NeoRangeEvent nevt = new NeoRangeEvent(this,
						vbox.x, vbox.x + vbox.width);
				for (NeoRangeListener rl : range_listeners) {
					rl.rangeChanged(nevt);
				}
			}
		}
	}

	/**
	 * allows another object to listen for NeoRangeEvents
	 * generated by implementers.
	 *
	 * @param l the listener.
	 */
	public void addRangeListener(NeoRangeListener l) {
		range_listeners.add(l);
	}

	/**
	 * allows another object to stop listening for NeoRangeEvents
	 * generated by implementers.
	 *
	 * @param l the listener.
	 */
	public void removeRangeListener(NeoRangeListener l) {
		range_listeners.remove(l);
	}

	public void selectBaseRangeOnConsensus( int x_coord_start, int x_coord_end) {
		if( x_coord_start > x_coord_end ) { // if the end is before the start, swap them
			int temp = x_coord_start;
			x_coord_start = x_coord_end;
			x_coord_end = temp;
		}

		int width = x_coord_end - x_coord_start;
		int end = x_coord_end;
		int start = x_coord_start;

		Rectangle2D.Double rect = alignmap.getCoordBounds();
		Rectangle2D.Double new_rect = new Rectangle2D.Double( x_coord_start, rect.y, width, rect.height );

		View map_view = alignmap.getView();
		for(GlyphI residue: align_glyphs) {
			if (residue.isSelected()){
				Rectangle2D.Double rec = residue.getCoordBox();
				if (rec.x > x_coord_start) start = (int)rec.x;
				if ( (rec.x + rec.width) < x_coord_end) end = (int)(rec.x + rec.width);
			}
		}
		if( cons_glyph.intersects( new_rect, map_view ) ) {
			consmap.select( cons_glyph,start, end );
		}
		else if( cons_glyph.isSelected() ) {
			consmap.deselect( cons_glyph );
		}
	}


	/**
	 * Select a given base on all the residues.
	 * Shows how they line up with the consensus.
	 */
	public void selectBaseRangeOnAllResidues( int x_coord_start, int x_coord_end ) {

		if( x_coord_start > x_coord_end ) { // if the end is before the start, swap them
			int temp = x_coord_start;
			x_coord_start = x_coord_end;
			x_coord_end = temp;
		}

		int width = x_coord_end - x_coord_start;
		Rectangle2D.Double rect = alignmap.getCoordBounds();
		Rectangle2D.Double new_rect = new Rectangle2D.Double( x_coord_start, rect.y, width, rect.height );

		View map_view = alignmap.getView();
		for (GlyphI residue : align_glyphs) {
			if( residue.intersects( new_rect, map_view ) ) {
				alignmap.select( residue, x_coord_start, x_coord_end );
			}
			else if( residue.isSelected() ) {
				alignmap.deselect( residue );
			}
		}
	}

	/** Get the Adjustable responsible for horizontal scrolling. */
	public Adjustable getHorizontalScroller () {
		return hscroll;
	}

	/**
	 * Set the scroller responsible for horizontal scrolling.
	 * If the given Adjustable is not an instance of Component,
	 * the call will be ignored.
	 */
	public void setHorizontalScroller(JScrollBar scroller) {

		if (!(scroller instanceof Component) || (scroller == null))
			return;

		remove ((Component)hscroll);
		hscroll = scroller;
		add    ((Component)hscroll);

		alignmap.setRangeScroller(hscroll);
		consmap.setRangeScroller (hscroll);
	}

	/** Get the Adjustable responsible for vertical scrolling. */
	public Adjustable getVerticalScroller () {
		return vscroll;
	}

	/**
	 * Set the scroller responsible for vertical scrolling.
	 * If the given Adjustable is not an instance of Component,
	 * the call will be ignored.
	 */
	public void setVerticalScroller(JScrollBar scroller) {

		if (!(scroller instanceof Component) || (scroller == null))
			return;

		remove ((Component)vscroll);
		vscroll = scroller;
		add    ((Component)vscroll);

		alignmap.setOffsetScroller(vscroll);
		labelmap.setOffsetScroller(vscroll);
	}

}
