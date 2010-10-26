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

import java.util.*;
import java.awt.Color;

import com.affymetrix.genoviz.event.TierEvent;
import com.affymetrix.genoviz.event.TierEventListener;
import com.affymetrix.genoviz.widget.tieredmap.LegendGlyph;
import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;
import java.awt.geom.Rectangle2D;

/**
 * Tracks tiers on a {@link TieredNeoMap} and shows a legend.
 * Labelled glyphs corresponding to tiers on TieredNeoMap.
 * A faster alternative to {@link TieredLabelMap}.
 */
public class LegendMap extends NeoMap implements TierEventListener  {

	Color uniform_label_color = null;
	TieredNeoMap map_to_track;
	Hashtable<MapTierGlyph,LegendGlyph> tier_to_legend = new Hashtable<MapTierGlyph,LegendGlyph>();
	Hashtable<LegendGlyph,MapTierGlyph> legend_to_tier = new Hashtable<LegendGlyph,MapTierGlyph>();
	List<LegendGlyph> legend_glyphs = new ArrayList<LegendGlyph>();
	boolean debug_events = false;
	Color expanded_color = Color.white;
	Color collapsed_color = Color.black;

	public LegendMap(boolean hscroll_show, boolean vscroll_show, TieredNeoMap tnm) {
		super(hscroll_show, vscroll_show);
		this.map_to_track = tnm;
	}

	public LegendMap(TieredNeoMap tnm) {
		super();
		this.map_to_track = tnm;
	}

	public void clearWidget() {
		super.clearWidget();
		tier_to_legend = new Hashtable<MapTierGlyph,LegendGlyph>();
		legend_to_tier = new Hashtable<LegendGlyph,MapTierGlyph>();
		legend_glyphs = new ArrayList<LegendGlyph>();
	}

	public void setExpandedColor( Color expanded_color ) {
		this.expanded_color = expanded_color;
	}

	public void setCollapsedColor ( Color collapsed_color ) {
		this.collapsed_color = collapsed_color;
	}

	public Color getExpandedColor () {
		return expanded_color;
	}

	public Color getCollapsedColor () {
		return collapsed_color;
	}

	public TieredNeoMap getMapToTrack() {
		return map_to_track;
	}

	public LegendGlyph getLegendForTier(MapTierGlyph mtg) {
		return tier_to_legend.get(mtg);
	}

	public MapTierGlyph getTierForLegend(LegendGlyph lg) {
		return legend_to_tier.get(lg);
	}

	/* TierEventListener implementation */

	public void heardTierEvent(TierEvent evt) {
		if (evt.getSource() != map_to_track) {
			return;
		}

		MapTierGlyph mtg = evt.getTier();
		int type = evt.getType();

		if ( getReshapeBehavior(NeoAbstractWidget.Y) != map_to_track.getReshapeBehavior(NeoAbstractWidget.Y) )  {
			throw new RuntimeException ( "TieredLabelMap and LabelMap do not have the same reshape behavior." );
		}

		switch (type) {

			case TierEvent.ADD:
			case TierEvent.ADD_BOTTOM:
			case TierEvent.ADD_TOP:
				addLabelGlyph(mtg);
				break;

			case TierEvent.REMOVE:
				removeLabelGlyph(mtg);
				break;

			case TierEvent.REPACK:
			case TierEvent.REORDER:
			case TierEvent.STATE_CHANGED:
				moveLegendGlyphs();
				break;

			default:
				System.out.println("unrecognized TierEvent type: " + type);
				return;
		}
	}

	void removeLabelGlyph(MapTierGlyph mtg) {
		LegendGlyph lglyph = getLegendForTier(mtg);
		if (lglyph == null) { return; }
		removeItem(lglyph);
		legend_glyphs.remove(lglyph);
		tier_to_legend.remove(mtg);
		legend_to_tier.remove(lglyph);
	}

	/**
	 * Make a new MapTierGlyph as a label, appropriate to the given tier.
	 */
	LegendGlyph addLabelGlyph(MapTierGlyph mtg) {
		Color glyphColor = mtg.getLabelColor();
		if (glyphColor == null)
			glyphColor = Color.black;
		LegendGlyph lglyph = new LegendGlyph();
		lglyph.setForegroundColor(Color.black);
		if (mtg.getState() == MapTierGlyph.EXPANDED ) lglyph.setBackgroundColor ( expanded_color );
		else lglyph.setBackgroundColor ( collapsed_color );
		lglyph.setForegroundColor ( Color.black );
		lglyph.setPrimaryLabel(mtg.getLabel());
		lglyph.setCoords(0, 0, 100, 30);
		this.addItem(lglyph);
		legend_glyphs.add(lglyph);
		tier_to_legend.put(mtg, lglyph);
		legend_to_tier.put(lglyph, mtg);
		return lglyph;
	}

	/**
	 * If call to setUniformLabelColor() is made [prior to creation of tiers],
	 * then set all tier labels to a uniform color.
	 * (Rather than basing on colors of existing tiers in map that is posting tier events.)
	 */
	public void setUniformLabelColor(Color col) {
		uniform_label_color = col;
	}

	/**
	 * Return uniform label color, or null if label colors are non-uniform
	 */
	public Color getUniformLabelColor() {
		return uniform_label_color;
	}

	/**
	 * Given the monitored map, move legend glyphs to match corresponding tiers.
	 */
	void moveLegendGlyphs() {
		LegendGlyph lglyph;
		Rectangle2D.Double tierbox;
		Rectangle2D.Double mapbox = this.getCoordBounds();
		Rectangle2D.Double trackbox = map_to_track.getCoordBounds();

		for (int i=0; i<legend_glyphs.size(); i++) {
			lglyph = legend_glyphs.get(i);
			MapTierGlyph mtg = getTierForLegend(lglyph);

			if (mtg != null) {
				tierbox = mtg.getCoordBox();
				if ((! mtg.isVisible()) || tierbox.height == 0) {
					lglyph.setVisibility(false);
				}
				else {
					lglyph.setVisibility(true);
					lglyph.setCoords(mapbox.x, tierbox.y, mapbox.width, tierbox.height);
				}
				if (mtg.getState() == MapTierGlyph.EXPANDED ) lglyph.setBackgroundColor ( expanded_color );
				else lglyph.setBackgroundColor ( collapsed_color );
			}
		}

		// is this needed ???
		setMapOffset((int)trackbox.y, (int)(trackbox.y + trackbox.height));
		this.scroll(NeoMap.Y, trackbox.y);
	}

	/**
	 * Get the location of the given legend glyph in the map.
	 * @return 0..n, the location counting top-down,
	 * or -1 if the given tier is not on the map.
	 */
	public int indexOf (LegendGlyph lglyph) {
		return legend_glyphs.indexOf(lglyph);
	}

}
