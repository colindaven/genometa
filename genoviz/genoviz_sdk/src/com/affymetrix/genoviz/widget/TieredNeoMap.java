/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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

import com.affymetrix.genoviz.event.TierEvent;
import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;



/**
 * A map that is separated into horizontal divisions, or tiers.
 * These tiers presumably each hold glyphs that represent a different category of data.
 * For example, a TieredNeoMap would be useful in displaying several kinds of features
 * along a sequence. There would be one tier for the axis, and then a tier for each other
 * type of feature. Some of the the advantages over a standard NeoMap are:
 * <ul>
 * <li> The tiers will expand as necessary to hold as many glyphs as have been added as children.
 *      There is no need to allocate a region of set size for a feature type.
 * <li> Each tier can have several states, implemented by different packers.
 *      See <a href="tieredmap/MapTierGlyph.html">MapTierGlyph</a> for details.
 * <li> It is possible to have a legend on the left or right of the map using TieredLabelMap.
 * </ul>
 * <p> Each tier is implemented as a MapTierGlyph,
 * which is a subclass of Glyph that does not draw itself,
 * but packs away from other MapTierGlyphs to define the tier regions.
 * Glyphs that belong in a tier are added as a child to the appropriate MapTierGlyph.
 * Other Glyphs that are added to the map without a parent will not be packed.
 */
public class TieredNeoMap extends AbstractTieredMap {

	/**
	 * @param hscroll_show     If true, TieredNeoMap uses its own horizontal scrollbar
	 * @param vscroll_show     If true, TieredNeoMap uses its own vertival scrollbar
	 *
	 */

	public TieredNeoMap(boolean hscroll_show, boolean vscroll_show) {
		super(hscroll_show, vscroll_show);
	}

	public TieredNeoMap() {
		super();
	}

	/** Add the given tier to the map, building top-down. */
	public void addTier(MapTierGlyph mtg) {
		addTier(mtg, false);
	}


	/**
	 * Repacks the tiers relative to each other.
	 * @param full_repack determines if a complete repack of all tiers is forced
	 */
	public void packTiers(boolean full_repack, boolean stretch_map)  {
		if (full_repack) {
		}
		super.packTiers(full_repack, stretch_map);
	}

	/**
	 * Add the given tier to the map.
	 * @param mtg the MapTierGlyph being added.
	 * @param ontop determines whether tier goes above or below existing tiers
	 */
	public void addTier(MapTierGlyph mtg, boolean ontop) {
		if (mtg == null)
			return;
		int evtid;
		this.addItem(mtg);
		// Use packTiers() rather than repack(), 'cause repack would
		// generate a TierEvent and any label maps will be out of sync
		// without the TierEvent.ADD below

		if (ontop) {
			tiers.add(0, mtg);
			evtid = TierEvent.ADD_TOP;
		}
		else {
			tiers.add(mtg);
			evtid = TierEvent.ADD_BOTTOM;
		}

		mtg.addTierStateChangeListener (this);
		TierEvent te = new TierEvent (this, evtid, mtg);
		this.notifyTierEventListeners(te);
	}

	/**
	 * TierEventListener implementation.
	 * This is used primarily for communication with a TieredLabelMap.
	 */
	public void heardTierEvent(TierEvent evt) {
		// We only care if this came from a TieredLabelMap
		if (!(evt.getSource() instanceof TieredLabelMap))
			return;

		// Distill info from the event

		TieredLabelMap tlm = (TieredLabelMap) evt.getSource();
		MapTierGlyph mtg = evt.getTier();
		int type = evt.getType();
		if (debug_events) {
			System.out.println(name + " heardTierEvent(): " +
					mtg.getLabel() + ", " + evt.getTypeString());
		}

		switch (type) {
			case TierEvent.ADD:
			case TierEvent.ADD_BOTTOM:
			case TierEvent.ADD_TOP:
				// Added a label?  Ignore it.
				break;

			case TierEvent.REMOVE:
				this.removeCorrTier(tlm, mtg);
				this.packToMatch(tlm, false, evt.getStretchMap() );
				this.updateWidget();
				break;

			case TierEvent.REPACK:
				this.packToMatch (tlm, evt.getFullRepack(), evt.getStretchMap() );
				this.updateWidget();
				break;

			case TierEvent.REORDER:
				this.moveTiers (tlm, evt.getMoveLocs());
				this.packToMatch(tlm,  evt.getFullRepack(), evt.getStretchMap() );
				this.updateWidget();
				break;

			case TierEvent.STATE_CHANGED:
				this.adjustState (tlm, mtg);
				this.packToMatch(tlm,  evt.getFullRepack(), evt.getStretchMap() );
				this.updateWidget();
				break;
			default:
				return;
		}
	}

}
