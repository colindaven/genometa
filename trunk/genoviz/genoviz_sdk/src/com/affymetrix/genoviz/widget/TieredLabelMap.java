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

import java.awt.Color;
import java.awt.Font;
import com.affymetrix.genoviz.event.TierEvent;
import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;
import java.util.List;


/**
 * A tiered map containing just labels.
 * Each tier contains a label for a corresponding tier
 * in another tiered map.
 * A listener will typically keep the two maps in sync.
 */
public class TieredLabelMap extends AbstractTieredMap {

	private Color uniform_label_color = null;
	private Color uniform_background_color = Color.gray;
	private Color uniform_outline_color = Color.black;
	private Font uniform_label_font = null;
	private boolean subSelectionAllowed = false;

	public TieredLabelMap (boolean hscroll_show, boolean vscroll_show) {
		super(hscroll_show, vscroll_show);
	}

	public TieredLabelMap() {
		super();
	}

	/* TierEventListener implementation */

	public void heardTierEvent(TierEvent evt) {

		// We only care if this came from a TieredNeoMap
		if (!(evt.getSource() instanceof TieredNeoMap))
			return;

		// Distill info from the event

		TieredNeoMap tnm = (TieredNeoMap) evt.getSource();
		MapTierGlyph mtg = evt.getTier();
		int type         = evt.getType();

		if ( getReshapeBehavior(NeoAbstractWidget.Y) != tnm.getReshapeBehavior(NeoAbstractWidget.Y) )
			throw new RuntimeException( "TieredLabelMap and LabelMap do not have the same reshape behavior." );

		if (debug_events)  {
			String tierlabel = "null";
			if (mtg != null) { tierlabel = mtg.getLabel(); }
			System.out.println(name + " heardTierEvent():  type = " +
					evt.getTypeString() + ", tier = " + tierlabel);
		}
		switch (type) {

			case TierEvent.ADD:
			case TierEvent.ADD_BOTTOM:
				this.addTier (makeLabelTierFor (mtg), false);
				this.packToMatch (tnm, true, true);
				break;

			case TierEvent.ADD_TOP:
				this.addTier (makeLabelTierFor (mtg), true);
				this.packToMatch (tnm, true, true);
				break;

			case TierEvent.REMOVE:
				this.removeCorrTier(tnm, mtg);
				break;

			case TierEvent.REPACK:
				this.packToMatch (tnm, evt.getFullRepack(), evt.getStretchMap());
				break;

			case TierEvent.REORDER:
				this.moveTiers (tnm, evt.getMoveLocs());
				break;

			case TierEvent.STATE_CHANGED:
				this.adjustState (tnm, mtg);
				break;

			default:
				return;
		}
	}

	private void addTier(MapTierGlyph mtg, boolean ontop) {
		if (mtg == null)
			return;

		this.addItem(mtg);

		if (ontop) {
			tiers.add(0, mtg);
		}
		else {
			tiers.add(mtg);
		}

		this.packTiers(true, true);
		this.updateWidget();

		// Listen to the tier for state change events,

		mtg.addTierStateChangeListener (this);
	}

	/**
	 * Make a new MapTierGlyph as a label, appropriate to the given tier.
	 */
	private MapTierGlyph makeLabelTierFor (MapTierGlyph mtg) {

		if (mtg == null)
			return null;

		Color mtgLabelColor = mtg.getLabelColor();

		if (mtgLabelColor == null)
			mtgLabelColor = Color.black;

		MapTierGlyph labelMTG = new MapTierGlyph();
		if (uniform_label_font != null) labelMTG.setFont(uniform_label_font);
		labelMTG.setHitable(true);
		labelMTG.setSpacer(5);
		labelMTG.setState(mtg.getState());
		labelMTG.setHideable(mtg.isHideable());
		labelMTG.setOutlineColor(uniform_outline_color);
		labelMTG.setBackgroundColor(uniform_background_color);
		labelMTG.setLabelSpacing ( mtg.getLabelSpacing() );
		if (uniform_label_color == null) {
			labelMTG.setLabelColor(mtgLabelColor != Color.gray ?
					mtgLabelColor : Color.black);
		}
		else {
			labelMTG.setLabelColor(uniform_label_color);
		}
		labelMTG.setLabel(mtg.getLabel());
		labelMTG.setCoords(0, 0, 100, 30);
		labelMTG.setSelectable ( isSubSelectionAllowed() );

		List<String> v = mtg.getMoreStrings();
		if ( v != null ) labelMTG.setMoreStrings( v );

		return labelMTG;
	}

	/**
	 * If call to setUniformLabelFont is made [prior to creation of tiers],
	 * then set all tier labels to the same Font.
	 */
	public void setUniformLabelFont(Font font) {
		this.uniform_label_font = font;
	}

	/**
	 * If call to setUniformLabelColor() is made [prior to creation of tiers],
	 * then all tier labels will be set to this uniform color.
	 * (Rather than basing on colors of existing tiers in map that is posting tier events.)
	 */
	public void setUniformLabelColor(Color col) {
		uniform_label_color = col;
	}

	/**
	 * @return uniform label color, or null if label colors are non-uniform.
	 */
	public Color getUniformLabelColor() {
		return uniform_label_color;
	}

	/**
	 * If call to setUniformBackgroundColor() is made [prior to creation of tiers],
	 * then all LabelTier backgrounds will be set to this uniform color.
	 * (Default is gray.)
	 * LabelTiers never automatically match their background colors based on TierEvents.
	 */
	public void setUniformBackgroundColor(Color col) {
		uniform_background_color = col;
	}

	/**
	 * @return uniform background color.
	 */
	public Color getUniformBackgroundColor() {
		return uniform_background_color;
	}

	/**
	 * If call to setUniformOutlineColor() is made [prior to creation of tiers],
	 * then all tier outlines will be set to this uniform color.
	 * (Default is black.)
	 * LabelTiers never automatically match their outline colors based on TierEvents.
	 */
	public void setUniformOutlineColor(Color col) {
		uniform_outline_color = col;
	}

	/**
	 * @return uniform outline color, or null if label colors are non-uniform.
	 */
	public Color getUniformOutlineColor() {
		return uniform_outline_color;
	}

	public void packTiers(boolean full_repack, boolean stretch_map)  {

		super.packTiers(full_repack, stretch_map);
	}

	/** Returns true if slection of individual tiers is allowed */
	public boolean isSubSelectionAllowed() {
		return subSelectionAllowed;
	}

	/** Sets whether slection of individual tiers is allowed */
	public void setSubSelectionAllowed(boolean allowed) {
		subSelectionAllowed = allowed;
	}

}
