package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRubberBandEvent;
import com.affymetrix.genoviz.event.NeoRubberBandListener;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.action.AutoScrollAction;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierGlyph;
import java.awt.Rectangle;
import java.awt.event.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.*;
import javax.swing.ToolTipManager;

/**
 *  A MouseListener for the SeqMapView.
 *
 *  This handles selection by clicking, section by rubber-banding, and the
 *  decision about when to pop-up a menu.
 *
 *  It was necessary to deviate somewhat from "best-practice" standards about
 *  how to check for the pop-up trigger and whether things happen on
 *  mousePressed() or mouseReleased() and detection of "right" mouse clicks.
 *  This is because the GenoViz SDK RubberBand interferes with some possibilities.
 *
 *  For example, we always show the popup during mouseReleased(), never
 *  mousePressed(), because that would interfere with the rubber band.
 *  For Windows users, this is the normal behavior anyway.  For Mac and Linux
 *  users, it is not standard, but should be fine.
 */
final class SeqMapViewMouseListener implements MouseListener, MouseMotionListener, NeoRubberBandListener, PropertyView.PropertyListener {

	// This flag determines whether selection events are processed on
	//  mousePressed() or mouseReleased().
	//
	// Users normally expect something to happen on mousePressed(), but
	// if updateWidget() is done in mousePressed(), it can occasionally make
	// the rubber band draw oddly.
	//
	// A solution is to move all mouse event processing into mouseReleased(),
	// as was done in earlier versions of IGB.  But since most applications
	// respond to mousePressed(), users expect something to happen then.
	//
	// A better solution would be to fix the rubber band drawing routines
	// so that they respond properly after updateWidget()
	//
	// The program should work perfectly fine with this flag true or false,
	// the rubber band simply looks odd sometimes (particularly with a fast drag)
	// if this flag is true.
	private static final boolean SELECT_ON_MOUSE_PRESSED = false;
	private final SeqMapView smv;
	private final AffyTieredMap map;
	private transient MouseEvent rubber_band_start = null;
	private int num_last_selections = 0;
	private int no_of_prop_being_displayed = 0;
	private final int reshowdelay = ToolTipManager.sharedInstance().getReshowDelay();
	private final int initialdelay = ToolTipManager.sharedInstance().getInitialDelay();
	private final int dismissdelay = ToolTipManager.sharedInstance().getDismissDelay();

	SeqMapViewMouseListener(SeqMapView smv) {
		this.smv = smv;
		this.map = smv.seqmap;
	}

	public void mouseEntered(MouseEvent evt) {
		ToolTipManager.sharedInstance().setDismissDelay(1000000);
		ToolTipManager.sharedInstance().setReshowDelay(0);
		ToolTipManager.sharedInstance().setInitialDelay(0);
	}

	public void mouseExited(MouseEvent evt) {
		ToolTipManager.sharedInstance().setDismissDelay(dismissdelay);
		ToolTipManager.sharedInstance().setReshowDelay(reshowdelay);
		ToolTipManager.sharedInstance().setInitialDelay(initialdelay);
	}

	public void mouseClicked(MouseEvent evt) {
		// reset rubber_band_start here?
	}

	public void mousePressed(MouseEvent evt) {
		if (map instanceof AffyLabelledTierMap) {
			((AffyLabelledTierMap) map).getLabelMap().clearSelected();
		}

		// turn OFF autoscroll in mousePressed()
		if (AutoScrollAction.getAction().map_auto_scroller != null) {
			AutoScrollAction.getAction().toggleAutoScroll();
		}

		// process selections in mousePressed() or mouseReleased()
		if (SELECT_ON_MOUSE_PRESSED) {
			processSelections(evt, true);
		}
	}

	public void mouseReleased(MouseEvent evt) {
		num_last_selections = map.getSelected().size();

		// process selections in mousePressed() or mouseReleased()
		if (!SELECT_ON_MOUSE_PRESSED) {
			// if rubber-banding is going on, don't post selections now,
			// because that will be handled in rubberBandChanged().
			// Still need to call processSelections, though, to set
			// the zoom point and to select the items under the current mouse point.
			processSelections(evt, rubber_band_start == null);
		}

		//  do popup in mouseReleased(), never in mousePressed(),
		//  so it doesn't interfere with rubber band
		if (isOurPopupTrigger(evt)) {
			smv.showPopup((NeoMouseEvent) evt);
		}

		// if the GraphSelectionManager is also trying to control popup menus,
		// then there needs to be code here to prevent both this and that from
		// trying to do a popup at the same time.  But it is tricky.  So for
		// now we let ONLY this class trigger the pop-up.
	}

	public void mouseDragged(MouseEvent evt) {

	}

	public void mouseMoved(MouseEvent evt) {
		if (!(evt instanceof NeoMouseEvent) || !smv.shouldShowPropTooltip()) {
			return;
		}
		
		NeoMouseEvent nevt = (NeoMouseEvent) evt;
		List<GlyphI> glyphs = nevt.getItems();
		if(!glyphs.isEmpty()) {
			smv.setToolTip(glyphs);
			return;
		}
		
		// Do we intersect any graph glyphs?
		List<GlyphI> glyphlist = smv.collectGraphs();
		Point2D pbox = evt.getPoint();
		for (GlyphI glyph : glyphlist) {
			GraphGlyph graf = (GraphGlyph) glyph;
			if (graf.getPixelBox().contains(pbox)) {
				Point2D cbox = new Point2D.Double();
				map.getView().transformToCoords(pbox, cbox);

				// Now we have the hairline (cbox.getX()).
				smv.setToolTip((int)cbox.getX(), graf);
				return;
			}
		}
		
		smv.setToolTip(glyphs);	// empty tooltip
	}

	private void processSelections(MouseEvent evt, boolean post_selections) {

		if (!(evt instanceof NeoMouseEvent)) {
			return;
		}
		NeoMouseEvent nevt = (NeoMouseEvent) evt;

		Point2D.Double zoom_point = new Point2D.Double(nevt.getCoordX(), nevt.getCoordY());
		List<GlyphI> hits = nevt.getItems();
		int hcount = hits.size();

		GlyphI topgl = null;
		if (!nevt.getItems().isEmpty()) {
			topgl = nevt.getItems().get(nevt.getItems().size() - 1);
			topgl = zoomCorrectedGlyphChoice(topgl, zoom_point);
		}

		// If drag began in the axis tier, then do NOT do normal selection stuff,
		// because we are selecting sequence instead.
		// (This only really matters when SELECT_ON_MOUSE_PRESSED is false.
		//  If SELECT_ON_MOUSE_PRESSED is true, topgl will already be null
		//  because a drag can only start when you begin the drag on blank space.)
		if (startedInAxisTier()) {
			topgl = null;
		}

		// Normally, clicking will clear previons selections before selecting new things.
		// but we preserve the current selections if:
		//  shift (Add To) or alt (Toggle) or pop-up (button 3) is being pressed
		boolean preserve_selections =
				(isAddToSelectionEvent(nevt) || isToggleSelectionEvent(nevt) || isOurPopupTrigger(nevt));

		// Special case:  if pop-up button is pressed on top of a single item and
		// that item is not already selected, then do not preserve selections
		if (topgl != null && isOurPopupTrigger(nevt)) {
			if (isAddToSelectionEvent(nevt)) {
				// This particular special-special case is really splitting hairs....
				// It would be ok to get rid of it.
				preserve_selections = true;
			} else if (!map.getSelected().contains(topgl)) {
				// This is the important special case.  Needs to be kept.
				preserve_selections = false;
			}
		}

		if (!preserve_selections) {
			smv.clearSelection(); // Note that this also clears the selected sequence region
		}

		// seems no longer needed
		//map.removeItem(match_glyphs);  // remove all match glyphs in match_glyphs
		List<GraphGlyph> graphs = new ArrayList<GraphGlyph>();
		for (int i = 0; i < hcount; i++) {
			Object obj = hits.get(i);
			if (obj instanceof GraphGlyph) {
				graphs.add((GraphGlyph) obj);
			}
		}
		int gcount = graphs.size();

		if (topgl != null) {
			boolean toggle_event = isToggleSelectionEvent(evt);
			//      if (toggle_event && map.getSelected().contains(topgl)) {
			if (toggle_event && topgl.isSelected()) {
				map.deselect(topgl);
			} else {
				map.select(topgl);
			}
			for (int i = 0; i < gcount; i++) {
				GraphGlyph gl = graphs.get(i);
				if (gl != topgl) {  // if gl == topgl, already handled above...
					if (toggle_event && gl.isSelected()) {
						map.deselect(gl);
					} else {
						map.select(gl);
					}
				}
			}
		}

		boolean nothing_changed = (preserve_selections && (topgl == null));
		boolean selections_changed = !nothing_changed;

		if (smv.show_edge_matches && selections_changed) {
			smv.doEdgeMatching(map.getSelected(), false);
		}
		smv.setZoomSpotX(zoom_point.getX());
		smv.setZoomSpotY(zoom_point.getY());

		map.updateWidget();

		if (selections_changed && post_selections) {
			smv.postSelections();
		}
	}


	/** Checks whether the mouse event is something that we consider to be
	 *  a pop-up trigger.  (This has nothing to do with MouseEvent.isPopupTrigger()).
	 *  Checks for isMetaDown() and isControlDown() to try and
	 *  catch right-click simulation for one-button mouse operation on Mac OS X.
	 */
	private static boolean isOurPopupTrigger(MouseEvent evt) {
		if (evt == null) {
			return false;
		}
		if (isToggleSelectionEvent(evt)) {
			return false;
		}
		return evt.isControlDown() || evt.isMetaDown() || ((evt.getModifiers() & InputEvent.BUTTON3_MASK) != 0);
	}

	/** Checks whether this the sort of mouse click that should preserve
	and add to existing selections.  */
	private static boolean isAddToSelectionEvent(MouseEvent evt) {
		return (evt != null && (evt.isShiftDown()));
	}

	/** Checks whether this the sort of mouse click that should toggle selections. */
	private static boolean isToggleSelectionEvent(MouseEvent evt) {
		//Make sure this does not conflict with pop-up trigger
		boolean b = (evt != null && evt.isControlDown() && evt.isShiftDown());
		return (b);
	}

	public void rubberBandChanged(NeoRubberBandEvent evt) {
		/*
		 * Note that because using SmartRubberBand, rubber banding will only happen
		 *   (and NeoRubberBandEvents will only be received) when the orginal mouse press to
		 *    start the rubber band doesn't land on a hitable glyph
		 */

		if (isOurPopupTrigger(evt)) {
			return;
			// This doesn't stop the rubber band from being drawn, because you would
			// have to do that inside the SmartRubberBand itself.  But if you don't
			// have this return statement here, it is possible for the selections
			// reported in the pop-up menu to differ from what appears to be selected
			// visually.  This is because the mouseReleased event can get processed
			// before the selection happens here through the rubber-band methods
		}

		if (evt.getID() == NeoRubberBandEvent.BAND_START) {
			rubber_band_start = evt;
		}
		if (evt.getID() == NeoRubberBandEvent.BAND_END) {
			Rectangle2D.Double cbox = new Rectangle2D.Double();
			Rectangle pbox = evt.getPixelBox();
			map.getView().transformToCoords(pbox, cbox);

			// setZoomSpot is best if done before updateWidget
			smv.setZoomSpotX(cbox.x + cbox.width);
			smv.setZoomSpotY(cbox.y + cbox.height);

			if (startedInAxisTier()) {
				// started in axis tier: user is trying to select sequence residues

				if (pbox.width >= 2 && pbox.height >= 2) {
					int seq_select_start = (int) cbox.x;
					// add 1 for interbase.  But don't go past end of sequence.
					int seq_select_end = Math.min(smv.getAnnotatedSeq().getLength(), (int) (cbox.x + cbox.width + 1));

					SeqSymmetry new_region = new SingletonSeqSymmetry(seq_select_start, seq_select_end, smv.getAnnotatedSeq());
					smv.setSelectedRegion(new_region, true);
				} else {
					// This is optional: clear selected region if drag is very small distance
					smv.setSelectedRegion(null, true);
				}

			} else {
				// started outside axis tier: user is trying to select glyphs
				doTheSelection(map.getItemsByCoord(cbox), rubber_band_start);
			}

			rubber_band_start = null; // for garbage collection
		}
	}

	// did the most recent drag start in the axis tier?
	private boolean startedInAxisTier() {
		TierGlyph axis_tier = smv.getAxisTier();
		boolean started_in_axis_tier = (rubber_band_start != null)
				&& (axis_tier != null)
				&& axis_tier.inside(rubber_band_start.getX(), rubber_band_start.getY());
		return started_in_axis_tier;
	}

	// This is called ONLY at the end of a rubber-band drag.
	private void doTheSelection(List<GlyphI> glyphs, MouseEvent evt) {
		boolean something_changed = true;

		// Remove any children of the axis tier (like contigs) from the selections.
		// Selecting contigs is something you usually do not want to do.  It is
		// much more likely that if someone dragged across the axis, they want to
		// select glyphs in tiers above and below but not IN the axis.
		ListIterator<GlyphI> li = glyphs.listIterator();
		while (li.hasNext()) {
			GlyphI g = li.next();
			if (isInAxisTier(g)) {
				li.remove();
			}
		}
		// Now correct for the fact that we might be zoomed way-out.  In that case
		// select only the parent glyphs (RNA's), not all the little children (Exons).
		Point2D.Double zoom_point = new Point2D.Double(0, 0); // dummy variable, value not used
		List<GlyphI> corrected = new ArrayList<GlyphI>(glyphs.size());
		for (int i = 0; i < glyphs.size(); i++) {
			GlyphI g = glyphs.get(i);
			GlyphI zc = zoomCorrectedGlyphChoice(g, zoom_point);
			if (!corrected.contains(zc)) {
				corrected.add(zc);
			}
		}
		glyphs = corrected;

		glyphs = new ArrayList<GlyphI>(SeqMapView.getParents(glyphs));

		if (isToggleSelectionEvent(evt)) {
			if (glyphs.isEmpty()) {
				something_changed = false;
			}
			toggleSelections(map, glyphs);
		} else if (isAddToSelectionEvent(evt)) {
			if (glyphs.isEmpty()) {
				something_changed = false;
			}
			map.select(glyphs);
		} else {
			if (glyphs.isEmpty() && num_last_selections == 0
					&& no_of_prop_being_displayed == 0) {
				something_changed = false;
			} else {
				something_changed = true;
				smv.clearSelection();
				map.select(glyphs);
			}
		}
		if (smv.show_edge_matches && something_changed) {
			smv.doEdgeMatching(map.getSelected(), false);
		}

		map.updateWidget();

		if (something_changed) {
			smv.postSelections();
		}
	}

	/**
	 *  Tries to determine the glyph you really wanted to choose based on the
	 *  one you clicked on.  Usually this will be the glyph you clicked on,
	 *  but when the zoom level is such that the glyph is very small, this
	 *  assumes you probably wanted to pick the parent glyph rather than
	 *  one of its children.
	 *
	 *  @param topgl a Glyph
	 *  @param zoom_point  the location where you clicked; if the returned glyph
	 *   is different from the given glyph, the returned zoom_point will be
	 *   at the center of that returned glyph, otherwise it will be unmodified.
	 *   This parameter should not be supplied as null.
	 *  @return a Glyph, and also modifies the value of zoom_point
	 */
	private GlyphI zoomCorrectedGlyphChoice(GlyphI topgl, Point2D.Double zoom_point) {
		if (topgl == null) {
			return null;
		}
		// trying to do smarter selection of parent (for example, transcript)
		//     versus child (for example, exon)
		// calculate pixel width of topgl, if <= 2, and it has no children,
		//   and parent glyphs has pixel width <= 10, then select parent instead of child..
		Rectangle pbox = new Rectangle();
		Rectangle2D.Double cbox = topgl.getCoordBox();
		map.getView().transformToPixels(cbox, pbox);

		if (pbox.width <= 2) {
			// if the selection is very small, move the x_coord to the center
			// of the selection so we can zoom-in on it.
			zoom_point.x = cbox.x + cbox.width / 2;
			zoom_point.y = cbox.y + cbox.height / 2;

			if ((topgl.getChildCount() == 0) && (topgl.getParent() != null)) {
				// Watch for null parents:
				// The reified Glyphs of the FlyweightPointGlyph made by OrfAnalyzer2 can have no parent
				cbox = topgl.getParent().getCoordBox();
				map.getView().transformToPixels(cbox, pbox);
				if (pbox.width <= 10) {
					topgl = topgl.getParent();
					if (pbox.width <= 2) { // Note: this pbox has new values than those tested above
						// if the selection is very small, move the x_coord to the center
						// of the selection so we can zoom-in on it.
						zoom_point.x = cbox.x + cbox.width / 2;
						zoom_point.y = cbox.y + cbox.height / 2;
					}
				}
			}
		}

		return topgl;
	}

	private boolean isInAxisTier(GlyphI g) {
		TierGlyph axis_tier = smv.getAxisTier();
		if (axis_tier == null) {
			return false;
		}

		GlyphI p = g;
		while (p != null) {
			if (p == axis_tier) {
				return true;
			}
			p = p.getParent();
		}
		return false;
	}


	private static void toggleSelections(NeoMap map, Collection<GlyphI> glyphs) {
		List<GlyphI> current_selections = map.getSelected();
		Iterator<GlyphI> iter = glyphs.iterator();
		while (iter.hasNext()) {
			GlyphI g = iter.next();
			if (current_selections.contains(g)) {
				map.deselect(g);
			} else {
				map.select(g);
			}
		}
	}

	public void propertyDisplayed(int prop_displayed) {
		no_of_prop_being_displayed = prop_displayed;
	}
}
