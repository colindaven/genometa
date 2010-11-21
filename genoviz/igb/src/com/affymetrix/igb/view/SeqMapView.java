package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.glyph.RootGlyph;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.widget.Shadow;
import com.affymetrix.genoviz.awt.AdjustableJSlider;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.SceneI;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.LeafSingletonSymmetry;
import com.affymetrix.genometryImpl.symmetry.MutableSingletonSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.glyph.CharSeqGlyph;
import com.affymetrix.igb.glyph.GlyphEdgeMatcher;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.glyph.GraphSelectionManager;
import com.affymetrix.igb.glyph.PixelFloaterGlyph;
import com.affymetrix.igb.glyph.SmartRubberBand;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.SeqMapViewPopup;
import com.affymetrix.igb.tiers.TierArithmetic;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.tiers.TierLabelManager;
import com.affymetrix.igb.tiers.TransformTierGlyph;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.TooltipUtils;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.action.RefreshDataAction;
import com.affymetrix.igb.action.ShrinkWrapAction;
import com.affymetrix.igb.action.ToggleHairlineLabelAction;
import com.affymetrix.igb.glyph.CytobandGlyph;
import com.affymetrix.igb.tiers.AxisStyle;
import com.affymetrix.igb.tiers.MouseShortCut;
import com.affymetrix.igb.tiers.TierLabelGlyph;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.prefs.*;
import java.util.regex.Pattern;
import javax.swing.*;

/**
 *
 * @version $Id: SeqMapView.java 7051 2010-10-19 20:02:50Z hiralv $
 */
public class SeqMapView extends JPanel
				implements SymSelectionListener, SeqSelectionListener, GroupSelectionListener {

	private static final boolean DEBUG_TIERS = false;

	protected boolean subselectSequence = true;  // try to visually select range along seq glyph based on rubberbanding
	boolean show_edge_matches = true;
	protected boolean coord_shift = false;
	private boolean hairline_is_labeled = true;
	private boolean show_prop_tooltip = true;
	private final Set<ContextualPopupListener> popup_listeners = new CopyOnWriteArraySet<ContextualPopupListener>();
	/**
	 *  maximum number of query glyphs for edge matcher.
	 *  any more than this and won't attempt to edge match
	 *  (edge matching is currently very inefficient with large numbers of glyphs --
	 *   something like O(N * M), where N is number of query glyphs and
	 *   M is total number of glyphs to try and match against query glyphs
	 *   [or possibly O(N^2 * M) ???] )
	 */
	private static final int max_for_matching = 500;
	/** boolean for setting map range to min and max bounds of
	AnnotatedBioSeq's annotations */
	private boolean shrinkWrapMapBounds = false;

	protected AffyTieredMap seqmap;
	private UnibrowHairline hairline = null;
	protected BioSeq aseq;
	/**
	 *  a virtual sequence that maps the BioSeq aseq to the map coordinates.
	 *  if the mapping is identity, then:
	 *     vseq == aseq OR
	 *     vseq.getComposition().getSpan(aseq) = SeqSpan(0, aseq.getLength(), aseq)
	 *  if the mapping is reverse complement, then:
	 *     vseq.getComposition().getSpan(aseq) = SeqSpan(aseq.getLength(), 0, aseq);
	 *
	 */
	protected BioSeq viewseq;
	// mapping of annotated seq to virtual "view" seq
	protected MutableSeqSymmetry seq2viewSym;
	protected SeqSymmetry[] transform_path;
	private static final String PREF_AXIS_LABEL_FORMAT = "Axis label format";
	/** One of the acceptable values of {@link #PREF_AXIS_LABEL_FORMAT}. */
	public static final String VALUE_AXIS_LABEL_FORMAT_COMMA = "COMMA";
	/** One of the acceptable values of {@link #PREF_AXIS_LABEL_FORMAT}. */
	public static final String VALUE_AXIS_LABEL_FORMAT_FULL = "FULL";
	/** One of the acceptable values of {@link #PREF_AXIS_LABEL_FORMAT}. */
	public static final String VALUE_AXIS_LABEL_FORMAT_ABBREV = "ABBREV";
	/** One of the acceptable values of {@link #PREF_AXIS_LABEL_FORMAT}. */
	public static final String VALUE_AXIS_LABEL_FORMAT_NO_LABELS = "NO_LABELS";
	public static final String PREF_EDGE_MATCH_COLOR = "Edge match color";
	public static final String PREF_EDGE_MATCH_FUZZY_COLOR = "Edge match fuzzy color";
	/** Name of a boolean preference for whether the hairline lable should be on. */
	public static final String PREF_HAIRLINE_LABELED = "Zoom Stripe Label On";
	/** Name of a boolean preference for whether the horizontal zoom slider is above the map. */
	private static final String PREF_X_ZOOMER_ABOVE = "Horizontal Zoomer Above Map";
	/** Name of a boolean preference for whether the vertical zoom slider is left of the map. */
	private static final String PREF_Y_ZOOMER_LEFT = "Vertical Zoomer Left of Map";
	/** Name of a boolean preference for whether to show properties in tooltip. */
	public static final String PREF_SHOW_TOOLTIP = "Show properties in tooltip";

	public static final Color default_edge_match_color = Color.WHITE;
	public static final Color default_edge_match_fuzzy_color = new Color(200, 200, 200); // light gray
	private static final boolean default_x_zoomer_above = true;
	private static final boolean default_y_zoomer_left = true;

	private final PixelFloaterGlyph pixel_floater_glyph = new PixelFloaterGlyph();
	private final GlyphEdgeMatcher edge_matcher;
	private final JPopupMenu sym_popup = new JPopupMenu();
	private JLabel sym_info;
	// A fake menu item, prevents null pointer exceptions in actionPerformed()
	// for menu items whose real definitions are commented-out in the code
	private static final JMenuItem empty_menu_item = new JMenuItem("");
	JMenuItem zoomtoMI = empty_menu_item;
	JMenuItem centerMI = empty_menu_item;
	JMenuItem selectParentMI = empty_menu_item;
	JMenuItem slicendiceMI = empty_menu_item;
	// for right-click on background
	private final SeqMapViewActionListener action_listener;
	private final SeqMapViewMouseListener mouse_listener;
	private CharSeqGlyph seq_glyph = null;
	private SeqSymmetry seq_selected_sym = null;  // symmetry representing selected region of sequence
	private final List<GlyphI> match_glyphs = new ArrayList<GlyphI>();
	protected TierLabelManager tier_manager;
	protected JComponent xzoombox;
	protected JComponent yzoombox;
	protected MapRangeBox map_range_box;
	public static final Font axisFont = NeoConstants.default_bold_font;
	boolean report_hairline_position_in_status_bar = false;
	boolean report_status_in_status_bar = true;
	private SeqSymmetry sym_used_for_title = null;

	private final static int xoffset_pop = 10;
	private final static int yoffset_pop = 0;
	private final Set<SeqMapRefreshed> seqmap_refresh_list = new CopyOnWriteArraySet<SeqMapRefreshed>();
	
	private TransformTierGlyph axis_tier;

	// This preference change listener can reset some things, like whether
	// the axis uses comma format or not, in response to changes in the stored
	// preferences.  Changes to axis, and other tier, colors are not so simple,
	// in part because of the need to coordinate with the label glyphs.
	private final PreferenceChangeListener pref_change_listener = new PreferenceChangeListener() {

		public void preferenceChange(PreferenceChangeEvent pce) {
			if (getAxisTier() == null) {
				return;
			}

			if (!pce.getNode().equals(PreferenceUtils.getTopNode())) {
				return;
			}

			TransformTierGlyph axis_tier = getAxisTier();

			if (pce.getKey().equals(PREF_AXIS_LABEL_FORMAT)) {
				AxisGlyph ag = null;
				for (GlyphI child : axis_tier.getChildren()) {
					if (child instanceof AxisGlyph) {
						ag = (AxisGlyph) child;
					}
				}
				if (ag != null) {
					setAxisFormatFromPrefs(ag);
				}
				seqmap.updateWidget();
			} else if (pce.getKey().equals(PREF_EDGE_MATCH_COLOR) || pce.getKey().equals(PREF_EDGE_MATCH_FUZZY_COLOR)) {
				if (show_edge_matches) {
					doEdgeMatching(seqmap.getSelected(), true);
				}
			} else if (pce.getKey().equals(PREF_X_ZOOMER_ABOVE)) {
				boolean b = PreferenceUtils.getBooleanParam(PREF_X_ZOOMER_ABOVE, default_x_zoomer_above);
				SeqMapView.this.remove(xzoombox);
				if (b) {
					SeqMapView.this.add(BorderLayout.NORTH, xzoombox);
				} else {
					SeqMapView.this.add(BorderLayout.SOUTH, xzoombox);
				}
				SeqMapView.this.invalidate();
			} else if (pce.getKey().equals(PREF_Y_ZOOMER_LEFT)) {
				boolean b = PreferenceUtils.getBooleanParam(PREF_Y_ZOOMER_LEFT, default_y_zoomer_left);
				SeqMapView.this.remove(yzoombox);
				if (b) {
					SeqMapView.this.add(BorderLayout.WEST, yzoombox);
				} else {
					SeqMapView.this.add(BorderLayout.EAST, yzoombox);
				}
				SeqMapView.this.invalidate();
			}
		}
	};

	public SeqMapView(boolean add_popups) {
		super();

		seqmap = createAffyTieredMap();
		
		seqmap.setReshapeBehavior(NeoAbstractWidget.X, NeoConstants.NONE);
		seqmap.setReshapeBehavior(NeoAbstractWidget.Y, NeoConstants.NONE);

		seqmap.addComponentListener(new SeqMapViewComponentListener());

		// the MapColor MUST be a very dark color or else the hairline (which is
		// drawn with XOR) will not be visible!
		seqmap.setMapColor(Color.BLACK);

		edge_matcher = GlyphEdgeMatcher.getSingleton();

		action_listener = new SeqMapViewActionListener(this);
		mouse_listener = new SeqMapViewMouseListener(this);

		seqmap.getNeoCanvas().setDoubleBuffered(false);

		seqmap.setScrollIncrementBehavior(AffyTieredMap.X, AffyTieredMap.AUTO_SCROLL_HALF_PAGE);

		Adjustable xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
		((JSlider)xzoomer).setToolTipText("Horizontal zoom");
		Adjustable yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
		((JSlider)yzoomer).setToolTipText("Vertical zoom");

		seqmap.setZoomer(NeoMap.X, xzoomer);
		seqmap.setZoomer(NeoMap.Y, yzoomer);


		tier_manager = new TierLabelManager((AffyLabelledTierMap) seqmap);
		SeqMapViewPopup popup = new SeqMapViewPopup(tier_manager,this);
		MouseShortCut msc = new MouseShortCut(popup);
		
		tier_manager.setDoGraphSelections(true);
		if (add_popups) {
			//NOTE: popup listeners are called in reverse of the order that they are added
			// Must use separate instances of GraphSelectioManager if we want to use
			// one as a ContextualPopupListener AND one as a TierLabelHandler.PopupListener
			//tier_manager.addPopupListener(new GraphSelectionManager(this));
			tier_manager.addPopupListener(new TierArithmetic(tier_manager, this));
			//TODO: tier_manager.addPopupListener(new CurationPopup(tier_manager, this));
			tier_manager.addPopupListener(popup);
		}

		// Listener for track selection events.  We will use this to populate 'Selection Info'
		// grid with properties of the Type.
		TierLabelManager.TrackSelectionListener track_selection_listener = new TierLabelManager.TrackSelectionListener() {

			public void trackSelectionNotify(GlyphI topLevelGlyph, TierLabelManager handler) {
				// TODO:  Find properties of selected track and show in 'Selection Info' tab.
			}
		};
		tier_manager.addTrackSelectionListener(track_selection_listener);


		seqmap.setSelectionAppearance(SceneI.SELECT_OUTLINE);
		seqmap.addMouseListener(mouse_listener);
		seqmap.addMouseListener(msc);
		seqmap.addMouseMotionListener(mouse_listener);
		((AffyLabelledTierMap)seqmap).getLabelMap().addMouseMotionListener(mouse_listener);
		//((AffyLabelledTierMap)seqmap).getLabelMap().addMouseListener(msc); //Enable mouse short cut here.

		tier_manager.setDoGraphSelections(true);

		// A "Smart" rubber band is necessary becaus we don't want our attempts
		// to drag the graph handles to also cause rubber-banding
		SmartRubberBand srb = new SmartRubberBand(seqmap);
		seqmap.setRubberBand(srb);
		seqmap.addRubberBandListener(mouse_listener);
		srb.setColor(new Color(100, 100, 255));

		GraphSelectionManager graph_manager = new GraphSelectionManager(this);
		seqmap.addMouseListener(graph_manager);
		this.addPopupListener(graph_manager);

		setupPopups();
		this.setLayout(new BorderLayout());

		map_range_box = new MapRangeBox(this);
		xzoombox = Box.createHorizontalBox();
		xzoombox.add(map_range_box.range_box);

		xzoombox.add(Box.createRigidArea(new Dimension(6, 0)));
		xzoombox.add((Component) xzoomer);

		JButton refresh_button = new JButton(RefreshDataAction.getAction());
		refresh_button.setText("");
		xzoombox.add(refresh_button);

		boolean x_above = PreferenceUtils.getBooleanParam(PREF_X_ZOOMER_ABOVE, default_x_zoomer_above);
		JPanel pan = new JPanel(new BorderLayout());
		pan.add("Center", xzoombox);
		if (x_above) {
			this.add(BorderLayout.NORTH, pan);
		} else {
			this.add(BorderLayout.SOUTH, pan);
		}

		yzoombox = Box.createVerticalBox();
		yzoombox.add((Component) yzoomer);
		boolean y_left = PreferenceUtils.getBooleanParam(PREF_Y_ZOOMER_LEFT, default_y_zoomer_left);
		if (y_left) {
			this.add(BorderLayout.WEST, yzoombox);
		} else {
			this.add(BorderLayout.EAST, yzoombox);
		}


		this.add(BorderLayout.CENTER, seqmap);

		LinkControl link_control = new LinkControl();
		this.addPopupListener(link_control);

		TrackView.getAnnotationGlyphFactory().setStylesheet(XmlStylesheetParser.getUserStylesheet());

		PreferenceUtils.getTopNode().addPreferenceChangeListener(pref_change_listener);
	}

	public final class SeqMapViewComponentListener extends ComponentAdapter {
		// update graphs and annotations when the map is resized.

		@Override
		public void componentResized(ComponentEvent e) {
			SwingUtilities.invokeLater(new Runnable() {

				public void run() {
					List<GlyphI> graphs = collectGraphs();
					for (int i = 0; i < graphs.size(); i++) {
						GraphGlyphUtils.checkPixelBounds((GraphGlyph) graphs.get(i), getSeqMap());
					}
					getSeqMap().stretchToFit(false, true);
					getSeqMap().updateWidget();

				}
			});
		}
	};

	/** Creates an instance to be used as the SeqMap.  Set-up of listeners and such
	 *  will be done in init()
	 */
	private static AffyTieredMap createAffyTieredMap() {
		AffyTieredMap resultSeqMap = new AffyLabelledTierMap(true, true);
		resultSeqMap.enableDragScrolling(true);
		NeoMap label_map = ((AffyLabelledTierMap) resultSeqMap).getLabelMap();
		label_map.setSelectionAppearance(SceneI.SELECT_OUTLINE);
		label_map.setReshapeBehavior(NeoAbstractWidget.Y, NeoConstants.NONE);
		return resultSeqMap;
	}

	public final TierLabelManager getTierManager() {
		return tier_manager;
	}

	private void setupPopups() {
		sym_info = new JLabel("");
		sym_info.setEnabled(false); // makes the text look different (usually lighter)

		centerMI = setUpMenuItem(sym_popup, "Center at zoom stripe");

		zoomtoMI = setUpMenuItem(sym_popup, "Zoom to selected");
		zoomtoMI.setIcon(MenuUtil.getIcon("toolbarButtonGraphics/general/Zoom16.gif"));

		selectParentMI = setUpMenuItem(sym_popup, "Select parent");
	}

	public final TransformTierGlyph getAxisTier() {
		return axis_tier;
	}

	/** Sets the axis label format from the value in the persistent preferences. */
	public static void setAxisFormatFromPrefs(AxisGlyph axis) {
		// It might be good to move this to AffyTieredMap
		String axis_format = PreferenceUtils.getTopNode().get(PREF_AXIS_LABEL_FORMAT, VALUE_AXIS_LABEL_FORMAT_COMMA);
		if (VALUE_AXIS_LABEL_FORMAT_COMMA.equalsIgnoreCase(axis_format)) {
			axis.setLabelFormat(AxisGlyph.COMMA);
		} else if (VALUE_AXIS_LABEL_FORMAT_FULL.equalsIgnoreCase(axis_format)) {
			axis.setLabelFormat(AxisGlyph.FULL);
		} else if (VALUE_AXIS_LABEL_FORMAT_NO_LABELS.equalsIgnoreCase(axis_format)) {
			axis.setLabelFormat(AxisGlyph.NO_LABELS);
		} else {
			axis.setLabelFormat(AxisGlyph.ABBREV);
		}
	}

	public void clear() {
		seqmap.clearWidget();
		aseq = null;
		this.viewseq = null;
		clearSelection();
		TrackView.clear();
		match_glyphs.clear();
		seqmap.updateWidget();
	}

	/**
	 *  Clears the graphs, and reclaims some memory.
	 */
	public final void clearGraphs() {
		if (aseq != null) {
			if (IGBConstants.GENOME_SEQ_ID.equals(aseq.getID())) {
				// clear graphs for all sequences in the genome
				for (BioSeq seq : aseq.getSeqGroup().getSeqList()) {
					removeGraphsFromSeq(seq);
				}
			}
			removeGraphsFromSeq(aseq);
		} else {
			System.err.println("Please select a chromosome!");
		}

		//Make sure the graph is un-selected in the genometry model, to allow GC
		GenometryModel.getGenometryModel().clearSelectedSymmetries(this);
		setAnnotatedSeq(aseq, false, true);
	}

	private static void removeGraphsFromSeq(BioSeq mseq) {
		int acount = mseq.getAnnotationCount();
		for (int i = acount - 1; i >= 0; i--) {
			SeqSymmetry annot = mseq.getAnnotation(i);
			if (annot instanceof GraphSym) {
				mseq.removeAnnotation(annot); // This also removes from the AnnotatedSeqGroup.
			}
		}
	}

	/** Sets the sequence; if null, has the same effect as calling clear(). */
	public final void setAnnotatedSeq(BioSeq seq) {
		setAnnotatedSeq(seq, false, (seq == this.aseq) && (seq != null));
		// if the seq is not changing, try to preserve current view
	}

	/**
	 *   Sets the sequence.  If null, has the same effect as calling clear().
	 *   @param preserve_selection  if true, then try and keep same selections
	 *   @param preserve_view  if true, then try and keep same scroll and zoom / scale and offset in
	 *       // both x and y direction.
	 *       [GAH: temporarily changed to preserve scale in only the x direction]
	 */
	public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view) {
		setAnnotatedSeq(seq, preserve_selection, preserve_view, false);
	}

	//   want to optimize for several situations:
	//       a) merging newly loaded data with existing data (adding more annotations to
	//           existing BioSeq) -- would like to avoid recreation and repacking
	//           of already glyphified annotations
	//       b) reverse complementing existing BioSeq
	//       c) coord shifting existing BioSeq
	//   in all these cases:
	//       "new" BioSeq == old BioSeq
	//       existing glyphs could be reused (in (b) they'd have to be "flipped")
	//       should preserve selection
	//       should preserve view (x/y scale/offset) (in (b) would preserve "flipped" view)
	//   only some of the above optimization/preservation are implemented yet
	//   WARNING: currently graphs are not properly displayed when reverse complementing,
	//               need to "genometrize" them
	//            currently sequence is not properly displayed when reverse complementing
	//
	public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view_x, boolean preserve_view_y) {
		Application.getSingleton().getFrame().setTitle(getTitleBar(seq));

		if (seq == null) {
			clear();
			return;
		}

		boolean same_seq = (seq == this.aseq);

		match_glyphs.clear();
		List<SeqSymmetry> old_selections = Collections.<SeqSymmetry>emptyList();
		double old_zoom_spot_x = seqmap.getZoomCoord(AffyTieredMap.X);
		double old_zoom_spot_y = seqmap.getZoomCoord(AffyTieredMap.Y);

		if (same_seq) {
			// Gather information about what is currently selected, so can restore it later
			if (preserve_selection) {
				old_selections = getSelectedSyms();
			} else {
				old_selections = Collections.<SeqSymmetry>emptyList();
			}
		}

		// stash annotation tiers for proper state restoration after resetting for same seq
		//    (but presumably added / deleted / modified annotations...)
		List<TierGlyph> cur_tiers = new ArrayList<TierGlyph>(seqmap.getTiers());
		int axis_index = Math.max(0, cur_tiers.indexOf(axis_tier));	// if not found, set to 0
		List<TierGlyph> temp_tiers = copyMapTierGlyphs(cur_tiers, axis_index);

		seqmap.clearWidget();
		seqmap.clearSelected(); // may already be done by map.clearWidget()

		pixel_floater_glyph.removeAllChildren();
		pixel_floater_glyph.setParent(null);

		seqmap.addItem(pixel_floater_glyph);

		// Synchronized to keep aseq from getting set to null
		synchronized (this) {
			aseq = seq;

			// if shifting coords, then seq2viewSym and viewseq are already taken care of,
			//   but reset coord_shift to false...
			if (coord_shift) {
				// map range will probably change after this if SHRINK_WRAP_MAP_BOUNDS is set to true...
				coord_shift = false;
			} else {
				this.viewseq = seq;
				seq2viewSym = null;
				transform_path = null;
			}

			seqmap.setMapRange(viewseq.getMin(), viewseq.getMax());
			addGlyphs(temp_tiers, axis_index);
		}

		seqmap.repack();

		if (same_seq && preserve_selection) {
			// reselect glyph(s) based on selected sym(s);
			// Unfortunately, some previously selected syms will not be directly
			// associatable with new glyphs, so not all selections can be preserved
			Iterator<SeqSymmetry> iter = old_selections.iterator();
			while (iter.hasNext()) {
				SeqSymmetry old_selected_sym = iter.next();

				GlyphI gl = seqmap.<GlyphI>getItem(old_selected_sym);
				if (gl != null) {
					seqmap.select(gl);
				}
			}
			setZoomSpotX(old_zoom_spot_x);
			setZoomSpotY(old_zoom_spot_y);
		} else {
			// do selection based on what the genometry model thinks is selected
			List<SeqSymmetry> symlist = GenometryModel.getGenometryModel().getSelectedSymmetries(seq);
			select(symlist, false, false, false);

			setStatus(getSelectionTitle(seqmap.getSelected()));
		}

		if (show_edge_matches) {
			doEdgeMatching(seqmap.getSelected(), false);
		}

		if (shrinkWrapMapBounds) {
			shrinkWrap();
		}
		
		seqmap.toFront(axis_tier);

		// restore floating layers to front of map
		for (GlyphI layer_glyph : getFloatingLayers(seqmap.getScene().getGlyph())) {
			seqmap.toFront(layer_glyph);
		}

		// Ignore preserve_view if seq has changed
		if ((preserve_view_x || preserve_view_y) && same_seq) {
			seqmap.stretchToFit(!preserve_view_x, !preserve_view_y);

			/** Possible bug : When all strands are hidden.
			 * tier label and tiers do appear at same position.
			**/
			// NOTE: Below call to stretchToFit is not redundancy. It is there
			//       to solve above mentioned bug.
			
			seqmap.stretchToFit(!preserve_view_x, !preserve_view_y);
		} else {
			seqmap.stretchToFit(true, true);
			
			/** Possible bug : Below both ranges are different
			*	System.out.println("SeqMapRange "+seqmap.getMapRange()[1]);
			*	System.out.println("VisibleRange "+seqmap.getVisibleRange()[1]);
			**/
			// NOTE: Below call to stretchToFit is not redundancy. It is there
			//       to solve a bug (ID: 2912651 -- tier map and tiers off-kilter)
			seqmap.stretchToFit(true, true);
			zoomToSelections();
			int[] range = seqmap.getVisibleRange();
			setZoomSpotX(0.5 * (range[0] + range[1]));
		}

		for(SeqMapRefreshed smr : seqmap_refresh_list){
			smr.refresh();
		}
		
		seqmap.updateWidget();

		//A Temporary hack to solve problem when a 'genome' is selected
		if(IGBConstants.GENOME_SEQ_ID.equals((seq.getID()))){
			seqmap.scroll(NeoMap.X, seqmap.getScroller(NeoMap.X).getMinimum());
		}
	}


	// copying map tiers to separate list to avoid problems when removing tiers
	//   (and thus modifying map.getTiers() list -- could probably deal with this
	//    via iterators, but feels safer this way...)
	private List<TierGlyph> copyMapTierGlyphs(List<TierGlyph> cur_tiers, int axis_index) {
		List<TierGlyph> temp_tiers = new ArrayList<TierGlyph>();
		for (int i = 0; i < cur_tiers.size(); i++) {
			if (i == axis_index) {
				continue;
			}
			TierGlyph tg = cur_tiers.get(i);
			tg.removeAllChildren();
			temp_tiers.add(tg);
			if (DEBUG_TIERS) {
				System.out.println("removing tier from map: " + tg.getLabel());
			}
			seqmap.removeTier(tg);
		}
		return temp_tiers;
	}

	private void addGlyphs(List<TierGlyph> temp_tiers, int axis_index) {
		// The hairline needs to be among the first glyphs added,
		// to keep it from interfering with selection of other glyphs.
		if (hairline != null) {
			hairline.destroy();
		}
		hairline = new UnibrowHairline(seqmap);
		hairline.getShadow().setLabeled(hairline_is_labeled);
		addPreviousTierGlyphs(seqmap, temp_tiers);
		axis_tier = addAxisTier(axis_index);
		addAnnotationTracks();
		hideEmptyTierGlyphs(seqmap.getTiers());
	}


	private static void addPreviousTierGlyphs(AffyTieredMap seqmap, List<TierGlyph> temp_tiers) {
		// add back in previous annotation tiers (with all children removed)
		if (temp_tiers != null) {
			for (int i = 0; i < temp_tiers.size(); i++) {
				TierGlyph tg = temp_tiers.get(i);
				if (DEBUG_TIERS) {
					System.out.println("adding back tier: " + tg.getLabel() + ", scene = " + tg.getScene());
				}
				if (tg.getAnnotStyle() != null) {
					tg.setStyle(tg.getAnnotStyle());
				}
				seqmap.addTier(tg, false);
			}
			temp_tiers.clear(); // redundant hint to garbage collection
		}
	}


	/** Set up a tier with fixed pixel height and place axis in it. */
	private TransformTierGlyph addAxisTier(int tier_index) {
		TransformTierGlyph resultAxisTier = new TransformTierGlyph(AxisStyle.axis_annot_style);
		resultAxisTier.setFixedPixHeight(45);
		resultAxisTier.setDirection(TierGlyph.Direction.AXIS);
		AxisGlyph axis = seqmap.addAxis(0);
		axis.setHitable(false);
		axis.setFont(axisFont);

		Color axis_bg = AxisStyle.axis_annot_style.getBackground();
		Color axis_fg = AxisStyle.axis_annot_style.getColor();

		axis.setBackgroundColor(axis_bg);
		resultAxisTier.setBackgroundColor(axis_bg);
		resultAxisTier.setFillColor(axis_bg);
		axis.setForegroundColor(axis_fg);
		resultAxisTier.setForegroundColor(axis_fg);
		setAxisFormatFromPrefs(axis);

		GlyphI cytoband_glyph = CytobandGlyph.makeCytobandGlyph(getAnnotatedSeq(), resultAxisTier, this);
		if (cytoband_glyph != null) {
			resultAxisTier.addChild(cytoband_glyph);
			resultAxisTier.setFixedPixHeight(resultAxisTier.getFixedPixHeight() + (int) cytoband_glyph.getCoordBox().height);
		}

		resultAxisTier.addChild(axis);

		// it is important to set the colors before adding the tier
		// to the map, else the label tier colors won't match
		if (seqmap.getTiers().size() >= tier_index) {
			seqmap.addTier(resultAxisTier, tier_index);
		} else {
			seqmap.addTier(resultAxisTier, false);
		}

		seq_glyph = CharSeqGlyph.initSeqGlyph(viewseq, axis_fg, axis);

		resultAxisTier.addChild(seq_glyph);

		return resultAxisTier;
	}

	private void shrinkWrap() {
		/*
		 *  Shrink wrapping is a little more complicated than one might expect, but it
		 *   needs to take into account the mapping of the annotated sequence to the
		 *   view (although currently assumes this mapping doesn't do any rearrangements, etc.)
		 *   (alternative, to ensure that _arbitrary_ genometry mapping can be accounted for,
		 *    is to base annotation bounds on map glyphs, but then have to go into tiers to
		 *    get children bounds, and filter out stuff like axis and DNA glyphs, etc...)
		 */
		SeqSpan annot_bounds = SeqUtils.getAnnotationBounds(aseq);
		if (annot_bounds != null) {
			// transform to view
			MutableSeqSymmetry sym = new SimpleMutableSeqSymmetry();
			sym.addSpan(annot_bounds);
			if (aseq != viewseq) {
				SeqUtils.transformSymmetry(sym, transform_path);
			}
			SeqSpan view_bounds = sym.getSpan(viewseq);
			seqmap.setMapRange(view_bounds.getMin(), view_bounds.getMax());
		}
	}

	private static String getTitleBar(BioSeq seq) {
		StringBuilder title = new StringBuilder(128);
		if (seq != null) {
			if (title.length() > 0) {
				title.append(" - ");
			}
			String seqid = seq.getID().trim();
			Pattern pattern = Pattern.compile("chr([0-9XYM]*)");
			if (pattern.matcher(seqid).matches()) {
				seqid = seqid.replace("chr", "Chromosome ");
			}

			title.append(seqid);
			String version_info = getVersionInfo(seq);
			if (version_info != null) {
				title.append("  (").append(version_info).append(')');
			}
		}
		if (title.length() > 0) {
			title.append(" - ");
		}
		title.append(IGBConstants.APP_NAME).append(" ").append(IGBConstants.APP_VERSION);
		return title.toString();
	}

	private static String getVersionInfo(BioSeq seq) {
		if (seq == null) {
			return null;
		}
		String version_info = null;
		if (seq.getSeqGroup() != null) {
			AnnotatedSeqGroup group = seq.getSeqGroup();
			if (group.getDescription() != null) {
				version_info = group.getDescription();
			} else {
				version_info = group.getID();
			}
		}
		if (version_info == null) {
			version_info = seq.getVersion();
		}
		if ("hg17".equals(version_info)) {
			version_info = "hg17 = NCBI35";
		} else if ("hg18".equals(version_info)) {
			version_info = "hg18 = NCBI36";
		}
		return version_info;
	}

	/**
	 *  Returns all floating layers _except_ grid layer (which is supposed to stay
	 *  behind everything else).
	 */
	private static List<GlyphI> getFloatingLayers(GlyphI root_glyph) {
		List<GlyphI> layers = new ArrayList<GlyphI>();
		int gcount = root_glyph.getChildCount();
		for (int i = 0; i < gcount; i++) {
			GlyphI cgl = root_glyph.getChild(i);
			if (cgl instanceof PixelFloaterGlyph) {
				layers.add(cgl);
			}
		}
		return layers;
	}

	private static void hideEmptyTierGlyphs(List<TierGlyph> tiers) {
		for (TierGlyph tg : tiers) {
			if (tg.getChildCount() == 0) {
				tg.setVisibility(false);
			}
		}
	}

	
	private void addAnnotationTracks() {
		TrackView.addTracks(this, aseq);

		if (aseq.getComposition() != null) {
			handleCompositionSequence();
		}
	}

	// muck with aseq, seq2viewsym, transform_path to trick addAnnotationTiers(),
	//   addLeafsToTier(), addToTier(), etc. into mapping from composition sequences
	private void handleCompositionSequence() {
		BioSeq cached_aseq = aseq;
		MutableSeqSymmetry cached_seq2viewSym = seq2viewSym;
		SeqSymmetry[] cached_path = transform_path;
		SeqSymmetry comp = aseq.getComposition();
		// assuming a two-level deep composition hierarchy for now...
		//   need to make more recursive at some point...
		//   (or does recursive call to addAnnotationTiers already give us full recursion?!!)
		int scount = comp.getChildCount();
		for (int i = 0; i < scount; i++) {
			SeqSymmetry csym = comp.getChild(i);
			// return seq in a symmetry span that _doesn't_ match aseq
			BioSeq cseq = SeqUtils.getOtherSeq(csym, cached_aseq);
			if (cseq != null) {
				aseq = cseq;
				if (cached_seq2viewSym == null) {
					transform_path = new SeqSymmetry[1];
					transform_path[0] = csym;
				} else {
					transform_path = new SeqSymmetry[2];
					transform_path[0] = csym;
					transform_path[1] = cached_seq2viewSym;
				}
				addAnnotationTracks();
			}
		}
		// restore aseq and seq2viewsym afterwards...
		aseq = cached_aseq;
		seq2viewSym = cached_seq2viewSym;
		transform_path = cached_path;
	}




	public final BioSeq getAnnotatedSeq() {
		return aseq;
	}

	/**
	 *  Gets the view seq.
	 *  Note: {@link #getViewSeq()} and {@link #getAnnotatedSeq()} may return
	 *  different BioSeq's !
	 *  This allows for reverse complement, coord shifting, seq slicing, etc.
	 *  Returns BioSeq that is the SeqMapView's _view_ onto the
	 *     BioSeq returned by getAnnotatedSeq()
	 *  @see #getTransformPath()
	 */
	public final BioSeq getViewSeq() {
		return viewseq;
	}

	/**
	 *  Returns the series of transformations that can be used to map
	 *  a SeqSymmetry from {@link #getAnnotatedSeq()} to
	 *  {@link #getViewSeq()}.
	 */
	public final SeqSymmetry[] getTransformPath() {
		return transform_path;
	}

	/** Returns a transformed copy of the given symmetry based on
	 *  {@link #getTransformPath()}.  If no transform is necessary, simply
	 *  returns the original symmetry.
	 */

	public final SeqSymmetry transformForViewSeq(SeqSymmetry insym, BioSeq seq_to_compare) {
		if (seq_to_compare != getViewSeq()) {
			MutableSeqSymmetry tempsym = SeqUtils.copyToDerived(insym);
			SeqUtils.transformSymmetry(tempsym, getTransformPath());
			return tempsym;
		}
		return insym;
	}

	public final AffyTieredMap getSeqMap() {
		return seqmap;
	}

	public final void selectAllGraphs() {
		List<GlyphI> glyphlist = collectGraphs();
		// convert graph glyphs to GraphSyms via glyphsToSyms

		// Bring them all into the visual area
		for (GlyphI gl : glyphlist) {
			GraphGlyphUtils.checkPixelBounds((GraphGlyph) gl, getSeqMap());
		}

		select(glyphsToSyms(glyphlist), false, true, true);
	}

	final void select(List<SeqSymmetry> sym_list) {
		select(sym_list, false, false, true);
	}

	private void select(List<SeqSymmetry> sym_list, boolean add_to_previous,
					boolean call_listeners, boolean update_widget) {
		if (!add_to_previous) {
			clearSelection();
		}

		for (SeqSymmetry sym : sym_list) {
			// currently assuming 1-to-1 mapping of sym to glyph
			GlyphI gl = seqmap.<GlyphI>getItem(sym);
			if (gl != null) {
				seqmap.select(gl);
			}
		}
		if (update_widget) {
			seqmap.updateWidget();
		}
		if (call_listeners) {
			postSelections();
		}
	}

	protected final void clearSelection() {
		sym_used_for_title = null;
		seqmap.clearSelected();
		setSelectedRegion(null, false);
		//  clear match_glyphs?
	}

	/**
	 *  Figures out which symmetries are currently selected and then calls
	 *  {@link GenometryModel#setSelectedSymmetries(List, Object)}.
	 */
	final void postSelections() {
		// Note that seq_selected_sym (the selected residues) is not included in selected_syms
		GenometryModel.getGenometryModel().setSelectedSymmetries(getSelectedSyms(), this);
	}


	// assumes that region_sym contains a span with span.getBioSeq() ==  current seq (aseq)
	public final void setSelectedRegion(SeqSymmetry region_sym, boolean update_widget) {
		seq_selected_sym = region_sym;
		// Note: SUBSELECT_SEQUENCE might possibly be set to false in the AltSpliceView
		if (subselectSequence && seq_glyph != null) {
			if (region_sym == null) {
				seq_glyph.setSelected(false);
			} else {
				SeqSpan seq_region = seq_selected_sym.getSpan(aseq);
				// corrected for interbase coords
				seq_glyph.select(seq_region.getMin(), seq_region.getMax()-1);
				setStatus(SeqUtils.spanToString(seq_region));
			}
			if (update_widget) {
				seqmap.updateWidget();
			}
		}
	}

	/**
	 * Copies residues of selection to clipboard
	 * If a region of sequence is selected, should copy genomic residues
	 * If an annotation is selected, should the residues of the leaf nodes of the annotation, spliced together
	 */
	public final boolean copySelectedResidues() {
		boolean success = false;
		SeqSymmetry residues_sym = null;
		Clipboard clipboard = this.getToolkit().getSystemClipboard();
		String from = "";

		if (seq_selected_sym != null) {
			residues_sym = seq_selected_sym;
			from = " from selected region";
		} else {
			List<SeqSymmetry> syms = getSelectedSyms();
			if (syms.size() == 1) {
				residues_sym = syms.get(0);
				from = " from selected item";
			}
		}

		if (residues_sym == null) {
			ErrorHandler.errorPanel("Can't copy to clipboard",
							"No selection or multiple selections.  Select a single item before copying its residues to clipboard.");
		} else {
			String residues = SeqUtils.determineSelectedResidues(residues_sym, aseq);
			if (residues != null) {
				if (SeqUtils.areResiduesComplete(residues)) {
					/*
					 *  WARNING
					 *  This bit of code *looks* unnecessary, but is needed because
					 *    StringSelection is buggy (at least with jdk1.3):
					 *    making a StringSelection with a String that has been derived from another
					 *    String via substring() ends up starting from the beginning of the _original_
					 *    String (maybe because of the way derived and original Strings do char-array sharing)
					 * THEREFORE, need to make a String with its _own_ internal char array that starts with
					 *   the 0th character...
					 */
					StringBuffer hackbuf = new StringBuffer(residues);
					String hackstr = new String(hackbuf);
					StringSelection data = new StringSelection(hackstr);
					clipboard.setContents(data, null);
					String message = "Copied " + hackstr.length() + " residues" + from + " to clipboard";
					setStatus(message);
					success = true;
				} else {
					ErrorHandler.errorPanel("Missing Sequence Residues",
							"Don't have all the needed residues, can't copy to clipboard.\n"
							+ "Please load sequence residues for this region.");
				}
			}
		}
		if (!success) {
			// null out clipboard if unsuccessful (otherwise might get fooled into thinking
			//   the copy operation worked...)
			// GAH 12-16-2003
			// for some reason, can't null out clipboard with [null] or [new StringSelection("")],
			//   have to put in at least one character -- just putting in a space for now
			clipboard.setContents(new StringSelection(" "), null);
		}
		return success;
	}


	/**
	 *  Determines which SeqSymmetry's are selected by looking at which Glyph's
	 *  are currently selected.  The list will not include the selected sequence
	 *  region, if any.  Use getSelectedRegion() for that.
	 *  @return a List of SeqSymmetry objects, possibly empty.
	 */
	private List<SeqSymmetry> getSelectedSyms() {
		return glyphsToSyms(seqmap.getSelected());
	}

	/**
	 * Given a list of glyphs, returns a list of syms that those
	 *  glyphs represent.
	 */
	public static List<SeqSymmetry> glyphsToSyms(List<GlyphI> glyphs) {
		Set<SeqSymmetry> symSet = new LinkedHashSet<SeqSymmetry>(glyphs.size());	// use LinkedHashSet to preserve order
		for (GlyphI gl : glyphs) {
			if (gl.getInfo() instanceof SeqSymmetry) {
				symSet.add((SeqSymmetry)gl.getInfo());
			}
		}
		return new ArrayList<SeqSymmetry>(symSet);
	}

	public final void zoomTo(SeqSpan span) {
		BioSeq zseq = span.getBioSeq();
		if (zseq != null && zseq != this.getAnnotatedSeq()) {
			GenometryModel.getGenometryModel().setSelectedSeq(zseq);
		}
		zoomTo(span.getMin(), span.getMax());
	}

	public final void zoomTo(double smin, double smax) {
		double coord_width = smax - smin;
		double pixel_width = seqmap.getView().getPixelBox().width;
		double pixels_per_coord = pixel_width / coord_width; // can be Infinity, but the Math.min() takes care of that
		pixels_per_coord = Math.min(pixels_per_coord, seqmap.getMaxZoom(NeoAbstractWidget.X));
		seqmap.zoom(NeoAbstractWidget.X, pixels_per_coord);
		seqmap.scroll(NeoAbstractWidget.X, smin);
		seqmap.setZoomBehavior(AffyTieredMap.X, AffyTieredMap.CONSTRAIN_COORD, (smin + smax) / 2);
		seqmap.updateWidget();
	}

	/** Zoom to a region including all the currently selected Glyphs. */
	public final void zoomToSelections() {
		List<GlyphI> selections = seqmap.getSelected();
		if (selections.size() > 0) {
			zoomToRectangle(getRegionForGlyphs(selections));
		} else if (seq_selected_sym != null) {
			SeqSpan span = getViewSeqSpan(seq_selected_sym);
			zoomTo(span);
		}
//		else{
//			zoomToRectangle(seqmap.getCoordBounds()); //Enable double click to zoom out here.
//		}
	}

	/**
	 * Center at the hairline.
	 */
	public final void centerAtHairline() {
		if (this.hairline == null) {
			return;
		}
		double pos = this.hairline.getSpot();
		Rectangle2D.Double vbox = this.getSeqMap().getViewBounds();
		double map_start = pos - vbox.width / 2;

		this.getSeqMap().scroll(NeoMap.X, map_start);
		this.setZoomSpotX(pos);
		this.getSeqMap().updateWidget();
	}

	/** Returns a rectangle containing all the current selections.
	 *  @return null if the vector of glyphs is empty
	 */
	private static Rectangle2D.Double getRegionForGlyphs(List<GlyphI> glyphs) {
		if (glyphs.isEmpty()) {
			return null;
		}
			Rectangle2D.Double rect = new Rectangle2D.Double();
			GlyphI g0 = glyphs.get(0);
			rect.setRect(g0.getCoordBox());
			for (GlyphI g : glyphs) {
				rect.add(g.getCoordBox());
			}
			return rect;
	}

	/**
	 *  Zoom to include (and slightly exceed) a given rectangular region in coordbox coords.
	 */
	private void zoomToRectangle(Rectangle2D.Double rect) {
		if (rect != null) {
			double desired_width = Math.min(rect.width * 1.1f, aseq.getLength() * 1.0f);
			seqmap.zoom(NeoAbstractWidget.X, Math.min(
							seqmap.getView().getPixelBox().width / desired_width,
							seqmap.getMaxZoom(NeoAbstractWidget.X)));
			seqmap.scroll(NeoAbstractWidget.X, -(seqmap.getVisibleRange()[0]));
			seqmap.scroll(NeoAbstractWidget.X, (rect.x - rect.width * 0.05));
			double map_center = rect.x + rect.width / 2 - seqmap.getViewBounds().width / 2;
			seqmap.scroll(NeoAbstractWidget.X, map_center);	// Center at hairline
			seqmap.setZoomBehavior(AffyTieredMap.X, AffyTieredMap.CONSTRAIN_COORD, (rect.x + rect.width / 2));
			seqmap.setZoomBehavior(AffyTieredMap.Y, AffyTieredMap.CONSTRAIN_COORD, (rect.y + rect.height / 2));
			seqmap.updateWidget();
		}
	}

	public final void unclamp() {
		if (viewseq != null) {
			int min = viewseq.getMin();
			int max = viewseq.getMax();
			seqmap.setMapRange(min, max);
		}
		seqmap.stretchToFit(false, false);
		seqmap.updateWidget();
	}

	public final void clampToView() {
		Rectangle2D.Double vbox = seqmap.getViewBounds();
		seqmap.setMapRange((int) (vbox.x), (int) (vbox.x + vbox.width));
		seqmap.stretchToFit(false, false); // to adjust scrollers and zoomers
		seqmap.updateWidget();
	}

	/**
	 * Do edge matching.  If query_glyphs is empty, clear all edges.
	 * @param query_glyphs
	 * @param update_map
	 */
	public final void doEdgeMatching(List<GlyphI> query_glyphs, boolean update_map) {
		// Clear previous edges
		if (match_glyphs != null && match_glyphs.size() > 0) {
			seqmap.removeItem(match_glyphs);  // remove all match glyphs in match_glyphs vector
		}

		int qcount = query_glyphs.size();
		int match_query_count = query_glyphs.size();
		for (int i = 0; i < qcount && match_query_count <= max_for_matching; i++) {
			match_query_count += query_glyphs.get(i).getChildCount();
		}

		if (match_query_count <= max_for_matching) {
			match_glyphs.clear();
			ArrayList<GlyphI> target_glyphs = new ArrayList<GlyphI>();
			target_glyphs.add(seqmap.getScene().getGlyph());
			double fuzz = getEdgeMatcher().getFuzziness();
			if (fuzz == 0.0) {
				Color edge_match_color = PreferenceUtils.getColor(PreferenceUtils.getTopNode(), PREF_EDGE_MATCH_COLOR, default_edge_match_color);
				getEdgeMatcher().setColor(edge_match_color);
			} else {
				Color edge_match_fuzzy_color = PreferenceUtils.getColor(PreferenceUtils.getTopNode(), PREF_EDGE_MATCH_FUZZY_COLOR, default_edge_match_fuzzy_color);
				getEdgeMatcher().setColor(edge_match_fuzzy_color);
			}
			getEdgeMatcher().matchEdges(seqmap, query_glyphs, target_glyphs, match_glyphs);
		} else {
			setStatus("Skipping edge matching; too many items selected.");
		}

		if (update_map) {
			seqmap.updateWidget();
		}
	}

	public final boolean getEdgeMatching() {
		return show_edge_matches;
	}

	public final void setEdgeMatching(boolean b) {
		show_edge_matches = b;
		if (show_edge_matches) {
			doEdgeMatching(seqmap.getSelected(), true);
		} else {
			doEdgeMatching(new ArrayList<GlyphI>(0), true);
		}
	}

	public final void adjustEdgeMatching(int bases) {
		getEdgeMatcher().setFuzziness(bases);
		if (show_edge_matches) {
			doEdgeMatching(seqmap.getSelected(), true);
		}
	}

	/**
	 *  return a SeqSpan representing the visible bounds of the view seq
	 */
	public final SeqSpan getVisibleSpan() {
		Rectangle2D.Double vbox = seqmap.getView().getCoordBox();
		SeqSpan vspan = new SimpleSeqSpan((int) vbox.x,
						(int) (vbox.x + vbox.width),
						viewseq);
		return vspan;
	}

	public final GlyphEdgeMatcher getEdgeMatcher() {
		return edge_matcher;
	}

	public final void setShrinkWrap(boolean b) {
		shrinkWrapMapBounds = b;
		setAnnotatedSeq(aseq);
		ShrinkWrapAction.getAction().putValue(Action.SELECTED_KEY, b);
	}

	public final boolean getShrinkWrap() {
		return shrinkWrapMapBounds;
	}

	/**
	 *  SymSelectionListener interface
	 */
	public void symSelectionChanged(SymSelectionEvent evt) {
		Object src = evt.getSource();

		// ignore self-generated xym selection -- already handled internally
		if (src == this) {
			String title = getSelectionTitle(seqmap.getSelected());
			setStatus(title);
		} // ignore sym selection originating from AltSpliceView, don't want to change internal selection based on this
		else if ((src instanceof AltSpliceView) || (src instanceof SeqMapView)) {
			// catching SeqMapView as source of event because currently sym selection events actually originating
			//    from AltSpliceView have their source set to the AltSpliceView's internal SeqMapView...
		} else {
			List<SeqSymmetry> symlist = evt.getSelectedSyms();
			// select:
			//   add_to_previous ==> false
			//   call_listeners ==> false
			//   update_widget ==>  false   (zoomToSelections() will make an updateWidget() call...)
			select(symlist, false, true, false);
			// Zoom to selections, unless the selection was caused by the TierLabelManager
			// (which sets the selection source as the AffyTieredMap, i.e. getSeqMap())
			if (src != getSeqMap() && src != getTierManager()) {
				zoomToSelections();
			}
			String title = getSelectionTitle(seqmap.getSelected());
			setStatus(title);
		}
	}

	/** Sets the hairline position and zoom center to the given spot. Does not call map.updateWidget() */
	public final void setZoomSpotX(double x) {
		int intx = (int) x;
		if (hairline != null) {
			hairline.setSpot(intx);
		}
		seqmap.setZoomBehavior(AffyTieredMap.X, AffyTieredMap.CONSTRAIN_COORD, intx);
	}

	/** Sets the hairline position to the given spot. Does not call map.updateWidget() */
	public final void setZoomSpotY(double y) {
		seqmap.setZoomBehavior(AffyTieredMap.Y, AffyTieredMap.CONSTRAIN_COORD, y);
	}

	/**
	 * Toggles the hairline between labeled/unlabeled and returns true
	 * if it ends up labeled.
	 *
	 * @return true if hairline is labeled
	 */
	public final boolean toggleHairlineLabel() {
		hairline_is_labeled = !hairline_is_labeled;
		if (hairline != null) {
			Shadow s = hairline.getShadow();
			s.setLabeled(hairline_is_labeled);
			seqmap.updateWidget();
		}
		if (ToggleHairlineLabelAction.getAction() != null) {
			ToggleHairlineLabelAction.getAction().putValue(Action.SELECTED_KEY, hairline_is_labeled);
		}
		return hairline_is_labeled;
	}

	public final boolean isHairlineLabeled() {
		return hairline_is_labeled;
	}

	private JMenuItem setUpMenuItem(JPopupMenu menu, String action_command) {
		return setUpMenuItem((Container) menu, action_command, action_listener);
	}

	final SeqMapViewMouseListener getMouseListener(){
		return mouse_listener;
	}
	
	/**
	 *  Adds a new menu item and sets-up an accelerator key based
	 *  on user prefs.  The accelerator key is registered directly
	 *  to the SeqMapView *and* on the JMenuItem itself: this does
	 *  not seem to cause a conflict.
	 *  @param menu if not null, the new JMenuItem will be added
	 *  to the given Container (perhaps a JMenu or JPopupMenu).
	 *  Use null if you don't want that to happen.
	 */
	public final JMenuItem setUpMenuItem(Container menu, String action_command,
					ActionListener al) {
		JMenuItem mi = new JMenuItem(action_command);
		// Setting accelerator via the MenuUtil.addAccelerator makes it also
		// work when the pop-up menu isn't visible.
		KeyStroke ks = MenuUtil.addAccelerator((JComponent) this,
						al, action_command);
		if (ks != null) {
			// Make the accelerator be visible in the menu item.
			mi.setAccelerator(ks);
		}
		mi.addActionListener(al);
		if (menu != null) {
			menu.add(mi);
		}
		return mi;
	}

	/** Select the parents of the current selections */
	final void selectParents() {
		if (seqmap.getSelected().isEmpty()) {
			ErrorHandler.errorPanel("Nothing selected");
			return;
		}

		boolean top_level = seqmap.getSelected().size() > 1;
		// copy selections to a new list before starting, because list of selections will be modified
		List<GlyphI> all_selections = new ArrayList<GlyphI>(seqmap.getSelected());
		Iterator<GlyphI> iter = all_selections.iterator();
		while (iter.hasNext()) {
			GlyphI child = iter.next();
			GlyphI pglyph = getParent(child, top_level);
			if (pglyph != child) {
				seqmap.deselect(child);
				seqmap.select(pglyph);
			}
		}

		if (show_edge_matches) {
			doEdgeMatching(seqmap.getSelected(), false);
		}
		seqmap.updateWidget();
		postSelections();
	}

	/** Find the top-most parent glyphs of the given glyphs.
	 *  @param childGlyphs a list of GlyphI objects, typically the selected glyphs
	 *  @return a list where each child is replaced by its top-most parent, if it
	 *  has a parent, or else the child itself is included in the list
	 */
	static List<GlyphI> getParents(List<GlyphI> childGlyphs) {
		// linked hash set keeps parents in same order as child list so that comparison
		// like childList.equals(parentList) can be used.
		Set<GlyphI> results = new LinkedHashSet<GlyphI>(childGlyphs.size());
		for (GlyphI child : childGlyphs) {
			GlyphI pglyph = getParent(child, true);
			results.add(pglyph);
		}
		return new ArrayList<GlyphI>(results);
	}

	/** Get the parent, or top-level parent, of a glyph, with certain restrictions.
	 *  Will not return a TierGlyph or RootGlyph or a glyph that isn't hitable, but
	 *  will return the original GlyphI instead.
	 *  @param top_level if true, will recurse up to the top-level parent, with
	 *  certain restrictions: recursion will stop before reaching a TierGlyph
	 */
	private static GlyphI getParent(GlyphI g, boolean top_level) {
		GlyphI pglyph = g.getParent();
		// the test for isHitable will automatically exclude seq_glyph
		if (pglyph != null && pglyph.isHitable() && !(pglyph instanceof TierGlyph) && !(pglyph instanceof RootGlyph)) {
			if (top_level) {
				GlyphI t = pglyph;
				while (t != null && t.isHitable() && !(t instanceof TierGlyph) && !(t instanceof RootGlyph)) {
					pglyph = t;
					t = t.getParent();
				}
			}
			return pglyph;
		}
		return g;
	}

	private void setStatus(String title) {
		if (!report_status_in_status_bar) {
			return;
		}
		Application.getSingleton().setStatus(title, false);
	}

	// Compare the code here with SymTableView.selectionChanged()
	// The logic about finding the ID from instances of DerivedSeqSymmetry
	// should be similar in both places, or else users could get confused.
	private String getSelectionTitle(List<GlyphI> selected_glyphs) {
		String id = null;
		if (selected_glyphs.isEmpty()) {
			id = "";
			sym_used_for_title = null;
		} else {
			if (selected_glyphs.size() == 1) {
				GlyphI topgl = selected_glyphs.get(0);
				Object info = topgl.getInfo();
				SeqSymmetry sym = null;
				if (info instanceof SeqSymmetry) {
					sym = (SeqSymmetry) info;
				}
				if (sym instanceof MutableSingletonSeqSymmetry) {
					id = ((LeafSingletonSymmetry) sym).getID();
					sym_used_for_title = sym;
				}
				if (id == null && sym instanceof SymWithProps) {
					id = (String) ((SymWithProps) sym).getProperty("id");
					sym_used_for_title = sym;
				}
				if (id == null && sym instanceof DerivedSeqSymmetry) {
					SeqSymmetry original = ((DerivedSeqSymmetry) sym).getOriginalSymmetry();
					if (original instanceof MutableSingletonSeqSymmetry) {
						id = ((LeafSingletonSymmetry) original).getID();
						sym_used_for_title = original;
					} else if (original instanceof SymWithProps) {
						id = (String) ((SymWithProps) original).getProperty("id");
						sym_used_for_title = original;
					}
				}
				if (id == null && topgl instanceof GraphGlyph) {
					GraphGlyph gg = (GraphGlyph) topgl;
					if (gg.getLabel() != null) {
						id = "Graph: " + gg.getLabel();
					} else {
						id = "Graph Selected";
					}
					sym_used_for_title = null;
				}
				if (id == null) {
					// If ID of item is null, check recursively for parent ID, or parent of that...
					GlyphI pglyph = topgl.getParent();
					if (pglyph != null && !(pglyph instanceof TierGlyph) && !(pglyph instanceof RootGlyph)) {
						// Add one ">" symbol for each level of getParent()
						sym_used_for_title = null; // may be re-set in the recursive call
						id = "> " + getSelectionTitle(Arrays.asList(pglyph));
					} else {
						id = "Unknown Selection";
						sym_used_for_title = null;
					}
				}
			} else {
				sym_used_for_title = null;
				id = "" + selected_glyphs.size() + " Selections";
			}
		}
		if (id == null) {
			id = "";
			sym_used_for_title = null;
		}
		return id;
	}

	final void showPopup(NeoMouseEvent nevt) {
		sym_popup.setVisible(false); // in case already showing
		sym_popup.removeAll();

		preparePopup(sym_popup);

		if (sym_popup.getComponentCount() > 0) {

			if (nevt == null) {
				// this might happen from pressing the Windows context menu key
				sym_popup.show(seqmap, 15, 15);
				return;
			}

			// if resultSeqMap is a MultiWindowTierMap, then using resultSeqMap as Component target arg to popup.show()
			//  won't work, since its component is never actually rendered -- so checking here
			/// to use appropriate target Component and pixel position
			EventObject oevt = nevt.getOriginalEvent();
			if ((oevt != null) && (oevt.getSource() instanceof Component)) {
				Component target = (Component) oevt.getSource();
				if (oevt instanceof MouseEvent) {
					MouseEvent mevt = (MouseEvent) oevt;
					sym_popup.show(target, mevt.getX() + xoffset_pop, mevt.getY() + yoffset_pop);
				} else {
					sym_popup.show(target, nevt.getX() + xoffset_pop, nevt.getY() + yoffset_pop);
				}
			} else {
				sym_popup.show(seqmap, nevt.getX() + xoffset_pop, nevt.getY() + yoffset_pop);
			}
		}
		// For garbage collection, it would be nice to add a listener that
		// could call sym_popup.removeAll() when the popup is removed from view.

		/* Force a repaint of the JPopupMenu.  This is a work-around for an
		 * Apple JVM Bug (verified on 10.5.8, Java Update 5).  Affected systems
		 * will display a stale copy of the JPopupMenu if the current number of
		 * menu items is equal to the previous number of menu items.
		 *
		 * The repaint must occur after the menu has been drawn:  it appears to
		 * skip the repaint if isVisible is false.  (another optimisation?)
		 */
		sym_popup.repaint();
	}


	/** Prepares the given popup menu to be shown.  The popup menu should have
	 *  items added to it by this method.  Display of the popup menu will be
	 *  handled by showPopup(), which calls this method.
	 */
	private void preparePopup(JPopupMenu popup) {
		List<GlyphI> selected_glyphs = seqmap.getSelected();

		setPopupMenuTitle(sym_info, selected_glyphs);

		popup.add(sym_info);
		if (!selected_glyphs.isEmpty()) {
			popup.add(zoomtoMI);
		}
		popup.add(centerMI);
		List<SeqSymmetry> selected_syms = getSelectedSyms();
		if (!selected_syms.isEmpty()) {
			popup.add(selectParentMI);
		}

		for (ContextualPopupListener listener : popup_listeners) {
			listener.popupNotify(popup, selected_syms, sym_used_for_title);
		}
	}


	// sets the text on the JLabel based on the current selection
	private void setPopupMenuTitle(JLabel label, List<GlyphI> selected_glyphs) {
		String title = "";
		if (selected_glyphs.size() == 1 && selected_glyphs.get(0) instanceof GraphGlyph) {
			GraphGlyph gg = (GraphGlyph) selected_glyphs.get(0);
			title = gg.getLabel();
		} else {
			title = getSelectionTitle(selected_glyphs);
		}
		// limit the popup title to 30 characters because big popup-menus don't work well
		if (title != null && title.length() > 30) {
			title = title.substring(0, 30) + " ...";
		}
		label.setText(title);
	}

	private void addPopupListener(ContextualPopupListener listener) {
		popup_listeners.add(listener);
	}

	/** Recurse through glyphs and collect those that are instances of GraphGlyph. */
	final List<GlyphI> collectGraphs() {
		List<GlyphI> graphs = new ArrayList<GlyphI>();
		GlyphI root = seqmap.getScene().getGlyph();
		collectGraphs(root, graphs);
		return graphs;
	}

	/** Recurse through glyph hierarchy and collect graphs. */
	private static void collectGraphs(GlyphI gl, List<GlyphI> graphs) {
		int max = gl.getChildCount();
		for (int i = 0; i < max; i++) {
			GlyphI child = gl.getChild(i);
			if (child instanceof GraphGlyph) {
				graphs.add((GraphGlyph) child);
			}
			if (child.getChildCount() > 0) {
				collectGraphs(child, graphs);
			}
		}
	}

	public final GlyphI getPixelFloaterGlyph() {
		PixelFloaterGlyph floater = pixel_floater_glyph;
		Rectangle2D.Double cbox = getSeqMap().getCoordBounds();
		floater.setCoords(cbox.x, 0, cbox.width, 0);

		return floater;
	}

	/**
	 *  Returns a forward and reverse tier for the given method, creating them if they don't
	 *  already exist.
	 *  Generally called by the Glyph Factory.
	 *  Note that this can create empty tiers.  But if the tiers are not filled with
	 *  something, they will later be removed automatically. 
	 *  @param meth  The tier annot; it will be treated as case-insensitive.
	 *  @param next_to_axis Do you want the Tier as close to the axis as possible?
	 *  @param style  a non-null instance of IAnnotStyle; tier label and other properties
	 *   are determined by the IAnnotStyle.
	 *  @return an array of two (not necessarily distinct) tiers, one forward and one reverse.
	 *    The array may instead contain two copies of one mixed-direction tier;
	 *    in this case place glyphs for both forward and revers items into it.
	 */
	public TierGlyph[] getTiers(boolean next_to_axis, ITrackStyleExtended style) {
		return TrackView.getTiers(this, next_to_axis, style, true);
	}



	public void groupSelectionChanged(GroupSelectionEvent evt) {
		AnnotatedSeqGroup current_group = null;
		AnnotatedSeqGroup new_group = evt.getSelectedGroup();
		if (aseq != null) {
			current_group = aseq.getSeqGroup();
		}

		if (Application.DEBUG_EVENTS) {
			System.out.println("SeqMapView received seqGroupSelected() call: " + ((new_group != null) ? new_group.getID() : "null"));
		}

		if ((new_group != current_group) && (current_group != null)) {
			clear();
		}
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (Application.DEBUG_EVENTS) {
			System.out.println("SeqMapView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
		}
		final BioSeq newseq = evt.getSelectedSeq();
		// Don't worry if newseq is null, setAnnotatedSeq can handle that
		// (It can also handle the case where newseq is same as old seq.)

		// trying out not calling setAnnotatedSeq() unless seq that is selected is actually different than previous seq being viewed
		// Maybe should change GenometryModel.setSelectedSeq() to only fire if seq changes...

		// reverted to calling setAnnotatedSeq regardless of whether newly selected seq is same as previously selected seq,
		//    because often need to trigger repacking / rendering anyway
		setAnnotatedSeq(newseq);
	}

	/** Get the span of the symmetry that is on the seq being viewed. */
	public final SeqSpan getViewSeqSpan(SeqSymmetry sym) {
		return sym.getSpan(viewseq);
	}

	/**
	 * Sets tool tip from given glyphs.
	 * @param glyphs
	 */
	public final void setToolTip(List<GlyphI> glyphs){
		if(!show_prop_tooltip) {
			return;
		}

		((AffyLabelledTierMap)seqmap).setToolTip(null);

		if(glyphs.isEmpty())
			return;

		List<SeqSymmetry> sym = SeqMapView.glyphsToSyms(glyphs);

		if (!sym.isEmpty()) {
			String[][] properties = PropertyView.getPropertiesRow(sym.get(0), this);
			// String tooltip = convertPropsToString(properties);
			String tooltip = convertPropsToEditorTooltip(properties);
			((AffyLabelledTierMap) seqmap).setToolTip(tooltip);
		} else if(glyphs.get(0) instanceof TierLabelGlyph){
			Map<String, Object> properties = TierLabelManager.getTierProperties(((TierLabelGlyph) glyphs.get(0)).getReferenceTier());
			String tooltip = convertPropsToString(properties);
			((AffyLabelledTierMap) seqmap).getLabelMap().setToolTip(tooltip);
		}
	}

	/**
	 * Sets tool tip from graph glyph.
	 * @param glyph
	 */
	public final void setToolTip(int x, GraphGlyph glyph){
		if(!show_prop_tooltip) {
			return;
		}

		((AffyLabelledTierMap)seqmap).setToolTip(null);

		List<GlyphI> glyphs = new ArrayList<GlyphI>();
		glyphs.add(glyph);
		List<SeqSymmetry> sym = SeqMapView.glyphsToSyms(glyphs);

		if (!sym.isEmpty()) {
			String[][] properties = PropertyView.getGraphPropertiesRowColumn((GraphSym)sym.get(0), x, this);
			String tooltip = convertPropsToString(properties);
			((AffyLabelledTierMap) seqmap).setToolTip(tooltip);
		}
	}

	private static String convertPropsToString(Map<String, Object> properties){
		if(properties == null)
			return null;

		StringBuilder props = new StringBuilder();
		String value = null;
		props.append("<html>");
		for(Entry<String, Object> prop : properties.entrySet()){
			props.append("<b>");
			props.append(prop.getKey());
			props.append(" : </b>");
			if(prop.getValue() != null){
				value = prop.getValue().toString();
				int vallen = value.length();
				props.append(value.substring(0, Math.min(40, vallen)));
				if(vallen > 40) {
					props.append(" ...");
				}
			}
			props.append("<br>");
		}
		props.append("</html>");

		return props.toString();
	}

	/**
	 * Constructs tooltip from preferences
	 * @param properties
	 * @return
	 */
	private static String convertPropsToEditorTooltip(String[][] properties){
		// JOptionPane.showMessageDialog(null, "convertPropsToEditorTooltip()");
		StringBuilder props = new StringBuilder();
		String value;
		HashMap<String,String> property_map = new HashMap<String,String>();

		// convert String array to Map
		for(int i = 0; i < properties.length; i++) {
			property_map.put(properties[i][0], properties[i][1]);
		}

		props.append("<html>");
		for(int i = 0; i < 20; i++) {
				String index = String.valueOf(i);
				String item = PreferenceUtils.getTooltipEditorPrefsNode().get(index, "dummy");
				Boolean available = true;

				if ( item.equals("[----------]") ) {
					props.append("<br>");
				}
				else if ( item.equals("dummy") ) {

				}
				else {
					value = property_map.get(item);

					if ( value == null ) {
						value = "tag not available";
						available = false;
					}

					if(value.length() > 25) {
						value = value.substring(0, TooltipUtils.MAX_TOOLTIP_LENGTH) + " ...";
					}

					if (!available) props.append("<font color=\"#555555\">");

					props.append("<b>");
					props.append(item);
					props.append(": </b>");
					props.append(value);
					props.append("<br>");

					if (!available) props.append("</font>");
				}

		}
		props.append("</html>");

		return props.toString();
	}


	/**
	 * Converts given properties into string.
	 * @param properties
	 * @return
	 */
	private static String convertPropsToString(String[][] properties){
		StringBuilder props = new StringBuilder();
		String value = null;
		
		props.append("<html>");
		for(int i=0; i<properties.length; i++){
			props.append("<b>");
			props.append(properties[i][0]);
			props.append(" : </b>");
			if((value = properties[i][1]) != null){
				int vallen = value.length();
				props.append(value.substring(0, Math.min(25, vallen)));
				if(vallen > 30) {
					props.append(" ...");
				}
			}
			props.append("<br>");
		}
		props.append("</html>");

		return props.toString();
	}

	public boolean togglePropertiesTooltip(){
		show_prop_tooltip =  !show_prop_tooltip;
		((AffyLabelledTierMap)seqmap).setToolTip(null);
		return show_prop_tooltip;
	}

	public final boolean shouldShowPropTooltip(){
		return show_prop_tooltip;
	}

	final void addToRefreshList(SeqMapRefreshed smr){
		seqmap_refresh_list.add(smr);
	}
	
	public static interface SeqMapRefreshed{
		public void refresh();
	}
}
