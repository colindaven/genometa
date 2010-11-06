package com.affymetrix.igb.tiers;

import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genoviz.comparator.GlyphMinYComparator;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genoviz.bioviews.GlyphDragger;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.SceneI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.glyph.GraphGlyph;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 *
 * @version $Id: TierLabelManager.java 7044 2010-10-18 20:35:42Z hiralv $
 */
public final class TierLabelManager {

	private final AffyLabelledTierMap tiermap;
	private final AffyTieredMap labelmap;
	private final JPopupMenu popup;
	private final static int xoffset_pop = 10;
	private final static int yoffset_pop = 0;
	private final Set<PopupListener> popup_listeners = new CopyOnWriteArraySet<PopupListener>();
	private final Set<TrackSelectionListener> track_selection_listeners = new CopyOnWriteArraySet<TrackSelectionListener>();
	private final Comparator<GlyphI> tier_sorter = new GlyphMinYComparator();
	/**
	 *  Determines whether selecting a tier label of a tier that contains only
	 *  GraphGlyphs should cause the graphs in that tier to become selected.
	 */
	private boolean do_graph_selections = false;

	private final MouseListener mouse_listener = new MouseListener() {

		TierLabelGlyph dragging_label = null;

		public void mouseEntered(MouseEvent evt) {
		}

		public void mouseExited(MouseEvent evt) {
		}

		/** Tests whether the mouse event is due to the 3rd button.
		 *  (For the sake of Macintosh, considers Meta key and Control key as
		 *  simulation of 3rd button.)
		 */
		boolean isOurPopupTrigger(MouseEvent evt) {
			int mods = evt.getModifiers();
			return (evt.isMetaDown() || evt.isControlDown()
					|| ((mods & InputEvent.BUTTON3_MASK) != 0));
		}

		public void mouseClicked(MouseEvent evt) {
		}

		public void mousePressed(MouseEvent evt) {
			if (evt instanceof NeoMouseEvent && evt.getSource() == labelmap) {
				NeoMouseEvent nevt = (NeoMouseEvent) evt;
				List selected_glyphs = nevt.getItems();
				GlyphI topgl = null;
				if (!selected_glyphs.isEmpty()) {
					topgl = (GlyphI) selected_glyphs.get(selected_glyphs.size() - 1);
				}

				// Dispatch track selection event
				//doTrackSelection(topgl);

				// Normally, clicking will clear previons selections before selecting new things.
				// but we preserve the current selections if:
				//  1. shift or alt key is pressed, or
				//  2. the pop-up key is being pressed
				//     2a. on top of nothing
				//     2b. on top of something previously selected
				boolean preserve_selections = false;
				if (nevt.isAltDown() || nevt.isShiftDown()) {
					preserve_selections = true;
				} else if (topgl != null && isOurPopupTrigger(nevt)) {
					if (labelmap.getSelected().contains(topgl)) {
						preserve_selections = true;
					}
				}
				if (!preserve_selections) {
					labelmap.clearSelected();
				}
				List<GlyphI> selected = nevt.getItems();
				labelmap.select(selected);
				doGraphSelections();

				tiermap.updateWidget(); // make sure selections becomes visible
				if (isOurPopupTrigger(evt)) {
					doPopup(evt);
				} else if (selected.size() > 0) {
					// take glyph at end of selected, just in case there is more
					//    than one -- the last one should be on top...
					TierLabelGlyph gl = (TierLabelGlyph) selected.get(selected.size() - 1);
					labelmap.toFront(gl);
					dragLabel(gl, nevt);
				}
			}
		}

		// if a tier has been dragged, then try to sort out rearrangement of tiers
		//    in tiermap based on new positions of labels in labelmap
		public void mouseReleased(MouseEvent evt) {
			if (evt.getSource() == labelmap && dragging_label != null) {
				//sortTiers();
				rearrangeTiers();
				dragging_label = null;
			}
		}

		private void dragLabel(TierLabelGlyph gl, NeoMouseEvent nevt) {
			dragging_label = gl;
			GlyphDragger dragger = new GlyphDragger((NeoAbstractWidget) nevt.getSource());
			dragger.setUseCopy(false);
			dragger.startDrag(gl, nevt);
			dragger.setConstraint(NeoConstants.HORIZONTAL, true);
		}
	}; // end of mouse listener class

	public TierLabelManager(AffyLabelledTierMap map) {
		tiermap = map;
		popup = new JPopupMenu();

		labelmap = tiermap.getLabelMap();
		labelmap.addMouseListener(this.mouse_listener);

		labelmap.getScene().setSelectionAppearance(SceneI.SELECT_OUTLINE);
		labelmap.setPixelFuzziness(0); // there are no gaps between tiers, need no fuzziness
	}

	/** Returns a list of TierGlyph items representing the selected tiers. */
	List<TierGlyph> getSelectedTiers() {
		List<TierGlyph> selected_tiers = new ArrayList<TierGlyph>();

		for (TierLabelGlyph tlg : getSelectedTierLabels()) {
			// TierGlyph should be data model for tier label, access via label.getInfo()
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			selected_tiers.add(tier);
		}
		return selected_tiers;
	}

	/** Returns a list of selected TierLabelGlyph items. */
	@SuppressWarnings("unchecked")
	public List<TierLabelGlyph> getSelectedTierLabels() {
		// The below loop is unnecessary, but is done to fix generics compiler warnings.
		List<TierLabelGlyph> tlg = new ArrayList<TierLabelGlyph>(labelmap.getSelected().size());
		for (GlyphI g : labelmap.getSelected()) {
			if (g instanceof TierLabelGlyph) {
				tlg.add((TierLabelGlyph) g);
			}
		}
		return tlg;
	}

	public List<Map<String, Object>> getTierProperties() {

		List<Map<String, Object>> propList = new ArrayList<Map<String, Object>>();

		for (TierGlyph glyph : getSelectedTiers()) {
			Map<String, Object> props = getTierProperties(glyph);

			if(props != null)
				propList.add(props);
		}

		return propList;
	}

	public static Map<String, Object> getTierProperties(TierGlyph glyph) {
		GenericFeature feature = glyph.getAnnotStyle().getFeature();

		if (feature == null) {
			return null;
		}

		Map<String, Object> props = new HashMap<String, Object>();
		props.put("id", feature.featureName);
		props.put("description", feature.description());
		if (feature.friendlyURL != null) {
			props.put("feature url", feature.friendlyURL);
		}
		props.put("loadmode", feature.loadStrategy.toString());
		String server = feature.gVersion.gServer.serverName + " (" + feature.gVersion.gServer.serverType.name() + ")";
		props.put("server", server);
		props.put("server url", feature.gVersion.gServer.friendlyURL);

		return props;
	}

	/** Returns a list of all TierLabelGlyph items. */
	public List<TierLabelGlyph> getAllTierLabels() {
		return tiermap.getTierLabels();
	}

	/** Selects all non-hidden tiers. */
	void selectAllTiers() {
		for (TierLabelGlyph tierlabel : getAllTierLabels()) {
			if (tierlabel.getReferenceTier().getAnnotStyle().getShow()) {
				labelmap.select(tierlabel);
			}
		}
		doGraphSelections();
		//labelmap.updateWidget();
		tiermap.updateWidget(); // make sure selections becomes visible
	}

	/**
	 *  Determines whether selecting a tier label of a tier that contains only
	 *  GraphGlyphs should cause the graphs in that tier to become selected.
	 */
	public void setDoGraphSelections(boolean b) {
		do_graph_selections = b;
	}

	private void doGraphSelections() {
		if (!do_graph_selections) {
			return;
		}

		GenometryModel gmodel = GenometryModel.getGenometryModel();
		Set<SeqSymmetry> symmetries = new LinkedHashSet<SeqSymmetry>();
		symmetries.addAll(gmodel.getSelectedSymmetries(gmodel.getSelectedSeq()));

		for (TierLabelGlyph tierlabel : getAllTierLabels()) {
			TierGlyph tg = tierlabel.getReferenceTier();
			int child_count = tg.getChildCount();
			if (child_count > 0 && tg.getChild(0) instanceof GraphGlyph) {
				// It would be nice if we could assume that a tier contains only
				// GraphGlyph's or only non-GraphGlyph's, but that is not true.
				//
				// When graph thresholding is turned on, there can be one or
				// two other EfficientFillRectGlyphs that are a child of the tier glyph
				// but are not instances of GraphGlyph.  They can be ignored.
				// (I would like to change them to be children of the GraphGlyph, but
				// haven't done it yet.)

				// Assume that if first child is a GraphGlyph, then so are all others
				for (int i = 0; i < child_count; i++) {
					GlyphI ob = tg.getChild(i);
					if (!(ob instanceof GraphGlyph)) {
						// ignore the glyphs that are not GraphGlyph's
						continue;
					}
					SeqSymmetry sym = (SeqSymmetry) ob.getInfo();
					// sym will be a GraphSym, but we don't need to cast it
					if (tierlabel.isSelected()) {
						symmetries.add(sym);
					} else if (symmetries.contains(sym)) {
						symmetries.remove(sym);
					}
				}
			}
		}

		gmodel.setSelectedSymmetries(new ArrayList<SeqSymmetry>(symmetries), this);
	}

	/** Gets all the GraphGlyph objects inside the given list of TierLabelGlyph's. */
	public static List<GraphGlyph> getContainedGraphs(List<TierLabelGlyph> tier_label_glyphs) {
		List<GraphGlyph> result = new ArrayList<GraphGlyph>();
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			result.addAll(getContainedGraphs(tlg));
		}
		return result;
	}

	/** Gets all the GraphGlyph objects inside the given TierLabelGlyph. */
	private static List<GraphGlyph> getContainedGraphs(TierLabelGlyph tlg) {
		List<GraphGlyph> result = new ArrayList<GraphGlyph>();
		TierGlyph tier = (TierGlyph) tlg.getInfo();
		int child_count = tier.getChildCount();
		if (child_count > 0 && tier.getChild(0) instanceof GraphGlyph) {
			for (int j = 0; j < child_count; j++) {
				result.add((GraphGlyph) tier.getChild(j));
			}
		}
		return result;
	}

	/** Restores multiple hidden tiers and then repacks.
	 *  @param tier_labels  a List of GlyphI objects for each of which getInfo() returns a TierGlyph.
	 *  @param full_repack  Whether to do a full repack
	 *  @param fit_y  Whether to change the zoom to fit all the tiers in the view
	 *  @see #repackTheTiers(boolean, boolean)
	 */
	public void showTiers(List<TierLabelGlyph> tier_labels, boolean full_repack, boolean fit_y) {
		for (TierLabelGlyph g : tier_labels) {
			if (g.getInfo() instanceof TierGlyph) {
				TierGlyph tier = (TierGlyph) g.getInfo();
				tier.getAnnotStyle().setShow(true);
			}
		}

		repackTheTiers(full_repack, fit_y);
	}

	/** Hides multiple tiers and then repacks.
	 *  @param tier_labels  a List of GlyphI objects for each of which getInfo() returns a TierGlyph.
	 *  @param fit_y  Whether to change the zoom to fit all the tiers in the view
	 */
	public void hideTiers(List<TierLabelGlyph> tier_labels, boolean full_repack, boolean fit_y) {
		for (TierLabelGlyph g : tier_labels) {
			if (g.getInfo() instanceof TierGlyph) {
				TierGlyph tier = (TierGlyph) g.getInfo();
				tier.getAnnotStyle().setShow(false);
			}
		}

		repackTheTiers(full_repack, fit_y);
	}

	/**
	 * Collapse or expand tiers.
	 * @param tier_labels
	 * @param collapsed - boolean indicating whether to collapse or expand tiers.
	 */
	void setTiersCollapsed(List<TierLabelGlyph> tier_labels, boolean collapsed) {
		for (TierLabelGlyph tlg : tier_labels) {
			setTierCollapsed(tlg, collapsed);
		}
		repackTheTiers(true, true);
	}

	/**
	 * Collapse or expand tier.
	 * @param tlg
	 * @param collapsed - boolean indicating whether to collapse or expand tiers.
	 */
	 void setTierCollapsed(TierLabelGlyph tlg, boolean collapsed) {
		ITrackStyle style = tlg.getReferenceTier().getAnnotStyle();
		if (style.getExpandable()) {
			style.setCollapsed(collapsed);
			// When collapsing, make them all be the same height as the tier.
			// (this is for simplicity in figuring out how to draw things.)
			if (collapsed) {
				List<GraphGlyph> graphs = getContainedGraphs(tlg);
				double tier_height = style.getHeight();
				for (GraphGlyph graph : graphs) {
					graph.getGraphState().getTierStyle().setHeight(tier_height);
				}
			}
			for (ViewI v : tlg.getReferenceTier().getScene().getViews()) {
				tlg.getReferenceTier().pack(v);
			}
		}
	}

	public void toggleTierCollapsed(List<TierLabelGlyph> tier_glyphs){
		for(TierLabelGlyph glyph : tier_glyphs){
			ITrackStyle style = glyph.getReferenceTier().getAnnotStyle();
			setTierCollapsed(glyph, !style.getCollapsed());
		}
		repackTheTiers(true, true);
	}
	
	/**
	 * Rearrange tiers in case mouse is dragged.
	 */
	void rearrangeTiers(){
		List<TierLabelGlyph> label_glyphs = tiermap.getTierLabels();
		Collections.sort(label_glyphs, tier_sorter);

		List<TierGlyph> tiers = tiermap.getTiers();
		tiers.clear();
		for (TierLabelGlyph label : label_glyphs) {
			TierGlyph tier = (TierGlyph) label.getInfo();
			tiers.add(tier);
		}
		//MPTAG Changed
		//updatePositions();
		// then repack of course (tiermap repack also redoes labelmap glyph coords...)
		tiermap.packTiers(updatePositions(), true, false);
		tiermap.updateWidget();
	}

	/**
	 * Sortiert nur die Labels um, jedoch nciht die Tiers selbst
	 * @return Wenn ein Label einen neuen Packer gesetzt bekommen hat muss neu gezeichnet werden. das kann im draw geschehen.
	 *  true wenn neu gezeichnet werden soll, false wenn nicht
	 */
	private boolean updatePositions(){
		//MPTAG added
		int coordIdx=-1;
		boolean hasToBeRepacked = false;
		List<TierLabelGlyph> label_glyphs = tiermap.getTierLabels();
		for(int i=0; i<label_glyphs.size(); i++){
			if(label_glyphs.get(i).getInfo() instanceof TransformTierGlyph){
				//MPTAG bisher habe ich nichts eindeutigeres gefunden was darauf hin deutet das es die Achsengylphe ist
				coordIdx = i;
			}
			label_glyphs.get(i).setPosition(i);
		}
		//Für alle über dem CoordIdx die Packerdirection auf Up setzen, für alle darunter auf down
		for (int i= 0; i < label_glyphs.size(); i++) {
			if(i < coordIdx){
				FasterExpandPacker ep = ((TierGlyph) label_glyphs.get(i).getInfo()).getExpandPacker();
				if(ep.getMoveType() != ExpandPacker.UP){
					ep.setMoveType(ExpandPacker.UP);
					((TierGlyph) label_glyphs.get(i).getInfo()).setExpandedPacker(ep);
					hasToBeRepacked = true;
				}
			}else if(i > coordIdx){
				FasterExpandPacker ep = ((TierGlyph) label_glyphs.get(i).getInfo()).getExpandPacker();
				if(ep.getMoveType() != ExpandPacker.DOWN){
					ep.setMoveType(ExpandPacker.DOWN);
					((TierGlyph) label_glyphs.get(i).getInfo()).setExpandedPacker(ep);
					hasToBeRepacked = true;
				}
			}
		}
		System.out.println("Achsenindex ist: "+ coordIdx+ " hasToBeRepacked? "+ hasToBeRepacked);
		return hasToBeRepacked;
	}

	/**
	 *  Sorts all tiers and then calls packTiers() and updateWidget().
	 */
	void sortTiers() {
		List<TierLabelGlyph> label_glyphs = tiermap.getTierLabels();
		Collections.sort(label_glyphs, new Comparator<TierLabelGlyph>(){

			public int compare(TierLabelGlyph g1, TierLabelGlyph g2) {
				return Double.compare(g1.getPosition(), g2.getPosition());
			}
		});
		
		// then repack of course (tiermap repack also redoes labelmap glyph coords...)
		tiermap.packTiers(false, true, false);
		tiermap.updateWidget();
	}

	/**
	 *  Repacks tiers.  Should be called after hiding or showing tiers or
	 *  changing their heights.
	 */
	void repackTheTiers(boolean full_repack, boolean stretch_vertically) {
		tiermap.repackTheTiers(full_repack, stretch_vertically);
	}

	public void addPopupListener(PopupListener p) {
		popup_listeners.add(p);
	}

	/** Removes all elements from the popup, then notifies all {@link TierLabelManager.PopupListener}
	 *  objects (which may add items to the menu), then displays the popup
	 *  (if it isn't empty).
	 */
	private void doPopup(MouseEvent e) {
		popup.removeAll();

		setPopuptitle();
		
		for (PopupListener pl : popup_listeners) {
			pl.popupNotify(popup, this);
		}

		if (popup.getComponentCount() > 0) {
			popup.show(labelmap, e.getX() + xoffset_pop, e.getY() + yoffset_pop);
		}
	}

	/**
	 * Sets title for popup.
	 * Sets feature name as title if available else shows number of selection.
	 */
	private void setPopuptitle(){
		List<TierGlyph> tiers = getSelectedTiers();

		if(tiers.isEmpty())
			return;

		String label = null;
		if(tiers.size() == 1 && tiers.get(0).getAnnotStyle().getFeature() != null)
			label = tiers.get(0).getAnnotStyle().getFeature().featureName;
		else
			label = tiers.size() + " Selections";

		if (label != null && label.length() > 30) {
			label = label.substring(0, 30) + " ...";
		}

		if(label != null && label.length() > 0){
			JLabel label_name = new JLabel(label);
			label_name.setEnabled(false); // makes the text look different (usually lighter)
			popup.add(label_name);
		}
	}

	/** An interface that lets listeners modify the popup menu before it is shown. */
	public interface PopupListener {

		/** Called before the {@link TierLabelManager} popup menu is displayed.
		 *  The listener may add elements to the popup menu before it gets displayed.
		 */
		public void popupNotify(JPopupMenu popup, TierLabelManager handler);
	}

	public void addTrackSelectionListener(TrackSelectionListener l) {
		track_selection_listeners.add(l);
	}

	public void doTrackSelection(GlyphI topLevelGlyph) {
		for (TrackSelectionListener l : track_selection_listeners) {
			l.trackSelectionNotify(topLevelGlyph, this);
		}
	}

	/** An interface that to listener for track selection events. */
	public interface TrackSelectionListener {

		public void trackSelectionNotify(GlyphI topLevelGlyph, TierLabelManager handler);
	}
}
