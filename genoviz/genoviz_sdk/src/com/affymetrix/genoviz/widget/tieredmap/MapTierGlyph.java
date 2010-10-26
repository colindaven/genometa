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

package com.affymetrix.genoviz.widget.tieredmap;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.event.TierStateChangeEvent;
import com.affymetrix.genoviz.event.TierStateChangeListener;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


// as long as moveRelative and moveAbsolute are used for moving the
// tier, coords of MapTierGlyph should continue to encompass all of its
// children since it inherits from StretchContainerGlyph
//
// BUT, maintaining correct amount of spacer padding while children are
// moving around may be a problem.

// public class MapTierGlyph extends com.affymetrix.genoviz.glyph.StretchContainerGlyph {

/**
 *  MapTierGlyph is intended for use with {@link com.affymetrix.genoviz.widget.TieredNeoMap}.
 *  Each tier in the TieredNeoMap is implemented as a MapTierGlyph, which can have different
 *  states as indicated below.
 *  In a TieredNeoMap, MapTierGlyphs pack relative to each other but not to other glyphs added
 *  directly to the map.
 */
public class MapTierGlyph extends com.affymetrix.genoviz.bioviews.Glyph {

	private final Set<TierStateChangeListener> tierStateChangeListeners = new CopyOnWriteArraySet<TierStateChangeListener>();

	/**
	 *  If hidden, the MapTierGlyph height is 0 coords
	 *  Effectively the MapTierGlyph and all its children are not shown at
	 *  all
	 */
	public static final int HIDDEN = 100;

	/**
	 *  If collapsed, the MapTierGlyph uses the collapsed packer.
	 *  This defaults to a CollapsedTierPacker, in which children are all
	 *  placed centered along the tier's vertical midpoint, and the
	 *  height of the tier is expanded to be the height of the tallest
	 *  child plus spacer
	 */
	public static final int COLLAPSED = 101;

	/**
	 *  If expanded, the MapTierGlyph uses the expanded packer.
	 *  This defaults to an ExpandedTierPacker, which pack its children
	 *  with collision avoidance (very similar to SiblingCoordAvoid), and
	 *  expands the tier to include all children plus spacer padding
	 *  above and below children
	 */
	public static final int EXPANDED = 102;

	/**
	 *  If fixed_size, MapTierGlyph will stay whatever size it is told to
	 *     via standard Glyph calls (setCoords, etc.), and when the tier
	 *     is packed, packing will not recurse down within the tier.
	 *  Location will still change as it is packed relative to other
	 *     tiers, however
	 *  WARNING -- doesn't quite work this way yet.  Because MapTierGlyph
	 *     extends StretchContainerGlyph, if glyphs are added to the tier
	 *     that extend beyond the tier's current bounds, then the tier
	 *     will extend to include the glyph's bounds.  Therefore, for this
	 *     to work, when adding children you _must_ take into account the
	 *     current coords of the tier.  GAH 7-18-99
	 */
	public static final int FIXED_SIZE = 103;

	public static final int STRAND_INSENSITIVE = 300;
	public static final int FORWARD_STRAND = 301;
	public static final int REVERSE_STRAND = 302;



	/**
	 *  not used yet -- intended for bidirectional stacking of tiers, but
	 *  not sure when that will be implemented */
	public static final int GREATER = 200;
	public static final int LESSER = 201;

	protected int state = EXPANDED;
	protected int stateBeforeHidden = EXPANDED;
	protected int relative_position = GREATER;
	protected double spacer = 2;
	protected Color fill_color = null;
	protected Color outline_color = null;
	protected Color label_color = Color.black;
	protected String label = null;
	protected boolean showLabel = true;
	protected boolean hitable = false;
	protected boolean hideable = true;
	protected List<String> moreStrings;
	protected int label_spacing = -1;
	protected int strand = STRAND_INSENSITIVE;

	protected PackerI expand_packer = new ExpandedTierPacker();
	protected PackerI collapse_packer = new CollapsedTierPacker();

	public MapTierGlyph() {
		state = 0; // do this so that setState() will work.
		setState(FIXED_SIZE);
		setSpacer(spacer);
	}

	private GlyphSearchNode gsn = new GlyphSearchNode();
	private int last_removed_position = -1;

	public void addChild(GlyphI glyph, int position) {
		super.addChild(glyph, position);
		gsn.addGlyph(glyph);
	}

	public void addChild(GlyphI glyph)  {
		if ( last_removed_position != -1 ) {
			super.addChild(glyph, last_removed_position);
			last_removed_position = -1;
		}
		else super.addChild ( glyph );
		gsn.addGlyph(glyph);
	}

	public void removeChild ( GlyphI glyph ) {
		last_removed_position = getChildren().indexOf(glyph);
		super.removeChild(glyph);
		gsn.removeGlyph(glyph);
	}

	/** Remove all children of the glyph */

	public void removeChildren () {
		List kids = this.getChildren();

		if (kids != null) {
			for (int i=0; i < kids.size(); i++)
				this.removeChild((GlyphI)kids.get(i));
		}
		gsn.removeChildren();
		// CLH: This is a hack. Instead of removing gsn,
		// I just assign a new one. Is this a massive leak???
		//
		// EEE: Yes, so I added the gsn.removeChildren() to help.
		gsn = new GlyphSearchNode();
	}

	public List<GlyphI> getOverlappingSibs(GlyphI child) {
		return gsn.getOverlaps(child);
	}

	/**
	 * @return a clone of moreStrings, used with TieredLabelMap.
	 */
  @SuppressWarnings("unchecked")
	public List<String> getMoreStrings() {
		if ( this.moreStrings == null ) {
			return new ArrayList<String>();
		}
		return new ArrayList<String>(this.moreStrings);
	}

	/**
	 * @param theStrings replaces the moreStrings vector.
	 * No clone is made.
	 * This may not be entirely safe
	 * and differs in strategy from the getMoreStrings method
	 * which does make a clone.
	 * @see #getMoreStrings
	 */
	public void setMoreStrings( List<String> theStrings ) {
		this.moreStrings = theStrings;
	}

	@Override
	protected void drawChildren(ViewI view) {
		if (children != null)  {
			GlyphI child;
			Rectangle compbox = view.getComponentSizeRect();
			pixelbox = pixelbox.intersection(compbox);
			Rectangle2D.Double cbox = new Rectangle2D.Double();
			view.transformToCoords(pixelbox, cbox);
			double a = cbox.x;
			double b = cbox.x + cbox.width;
			List children_in_range = gsn.getOverlappingGlyphs(a, b);
			int j_size = children_in_range.size();
			for (int j=0; j<j_size; j++) {
				child = (GlyphI) children_in_range.get(j);
				// TransientGlyphs are usually NOT drawn in standard drawTraversal
				if (drawTransients() || !(child instanceof TransientGlyph)) {
					child.drawTraversal(view);
				}
			}
		}
	}

	@Override
	public void draw(ViewI view) {

		view.transformToPixels(coordbox, pixelbox);
		pixelbox.width = Math.max ( pixelbox.width, min_pixels_width );
		pixelbox.height = Math.max ( pixelbox.height, min_pixels_height );

		Graphics g = view.getGraphics();
		// use view pixelbox instead of view's component's pixel box, so will play nice
		//   with drawing optimizations
		Rectangle vbox = view.getPixelBox();
		pixelbox = pixelbox.intersection(pixelbox);
		boolean bottomTier = (pixelbox.y+pixelbox.height==vbox.y+vbox.height);

		if (fill_color != null) {
			g.setColor(fill_color);
			g.fillRect(pixelbox.x, pixelbox.y,
					pixelbox.width, pixelbox.height);
		}
		if (outline_color != null) {
			g.setColor(outline_color);
			// The minus-one here is to make the right-hand
			// outline be drawn inside the pixelbox.  The default
			// behavior of g.fillRect and g.drawRect, when called
			// with the same arguments, is to draw the right and top
			// outline inside the box and the left and bottom lines
			// outside.  For most of the tiers, we let the bottom
			// outline be drawn outside the rectangle, so it will
			// get written-over by top pixel of the tier below, but
			// for the bottom tier, we must draw the bottom outline inside
			if (bottomTier) g.drawRect(pixelbox.x, pixelbox.y,
					pixelbox.width-1, pixelbox.height-1);
			else g.drawRect(pixelbox.x, pixelbox.y,
					pixelbox.width-1, pixelbox.height);
		}
		if (label_color != null && label != null && showLabel && pixelbox.height > 4) {
			// No font is readable at less than 5 pixels!
			FontMetrics fm = g.getFontMetrics();
			// 0.8 is a kludge, but getAscent() overestimates the amount
			// of space needed for normal capital letters; it includes
			// room for weirdly tall characters like '|' and accents.
			int fontSize = (int) (0.8*(fm.getAscent() + fm.getDescent()));
			int textYPos = pixelbox.y + (int) (0.8*fm.getAscent());
			int textXPos = 5;
			int bottom = pixelbox.y + pixelbox.height - fm.getDescent();
			if (outline_color != null) {
				textYPos += 2;
				bottom -= 1;
			}

			g.setColor(label_color);
			g.drawString(label, textXPos, textYPos);
			if ( moreStrings != null ) {
				if ( label_spacing == -1 ) label_spacing = fontSize + 2;
				for (int i = 0; i < moreStrings.size(); i++) {
					textYPos += label_spacing;
					if ( textYPos >= bottom ) break;
					g.drawString (moreStrings.get(i), textXPos, textYPos);
				}
			}
		}
		super.draw(view);
	}

	public void setLabelSpacing ( int pixels ) {
		this.label_spacing = pixels;
	}

	public int getLabelSpacing() {
		return label_spacing;
	}

	public void addLineToLabel ( String s ) {
		if ( moreStrings == null ) moreStrings = new ArrayList<String>();
		moreStrings.add( s );
	}

	public PackerI getExpandedPacker() {
		return expand_packer;
	}

	public PackerI getCollapsedPacker() {
		return collapse_packer;
	}

	public void setExpandedPacker(PackerI packer) {
		this.expand_packer = packer;
		setSpacer(getSpacer());
	}

	public void setCollapsedPacker(PackerI packer) {
		this.collapse_packer = packer;
		setSpacer(getSpacer());
	}

	public int getState() {
		return state;
	}

	public int getStrand() {
		return strand;
	}

	public void setStrand(int theStrand) {
		if (strand == FORWARD_STRAND ||
				strand == REVERSE_STRAND ||
				strand == STRAND_INSENSITIVE) this.strand = theStrand;
		else throw new IllegalArgumentException(
				"Bad argument to setStrand: "+theStrand);
	}

	/**
	 * Sets the state of the tier.  Acceptable values are:
	 * <ul>
	 * <li> {@link #EXPANDED}: children are packed away from each other vertically,
	 *                         and the glyph expands to the size necessary to display them
	 * <li> {@link #COLLAPSED}: children are not packed,
	 *                          but rather collapsed into a tier the height of the tallest glyph.
	 * <li> {@link #HIDDEN}: the glyph has a coordbox of zero, takes up no space and children are not visible.
	 * <li> {@link #FIXED_SIZE}:  the size of the tier is set to the size of the MapTierGlyph's coordbox height
	 * </ul>
	 * @see #restoreState()
	 */
	public void setState(int newstate) {
		// terminate any pingponging if state is already same
		if (state == newstate) return;

		if (newstate==HIDDEN && state != HIDDEN)
			stateBeforeHidden = state; // used by restoreState();

		if (newstate==COLLAPSED || newstate==EXPANDED || newstate==HIDDEN)
			state = newstate;
		else state = FIXED_SIZE;

		if (state == EXPANDED) {
			setPacker(expand_packer);
			setVisibility(true);
		}
		else if (state == COLLAPSED) {
			setPacker(collapse_packer);
			setVisibility(true);
		}
		else if (state == HIDDEN) {
			setPacker(null);
			setVisibility(false);
		}
		else {  // including state == FIXED
			state = FIXED_SIZE;
			setPacker(null);
			setVisibility(true);
		}

		// And tell the tier's listeners about it
		TierStateChangeEvent te = new TierStateChangeEvent (this, state);
		this.notifyTierStateChangeListeners (te);
	}

	/**
	 * Restore a hidden glyph to its previous state.
	 * If the <code>state=={@link #HIDDEN}</code>,
	 * restore the glyph to the state it was in before it was hidden.
	 * Otherwise, do nothing.
	 */
	public void restoreState() {
		if (state==HIDDEN) setState(stateBeforeHidden);
	}

	@Override
	public boolean hit (Rectangle pixel_hitbox, ViewI view) {
		calcPixels(view);

		if (!isVisible() || !this.hitable)
			return false;

		return pixel_hitbox.intersects (this.getPixelBox());
	}

	@Override
	public boolean hit (Rectangle2D.Double coord_hitbox, ViewI view) {
		return isVisible && isHitable() && coord_hitbox.intersects (this.getCoordBox());
	}


	public void setSpacer(double spacer) {
		this.spacer = spacer;
		if (collapse_packer instanceof PaddedPackerI) {
			((PaddedPackerI)collapse_packer).setParentSpacer(spacer);
		}
		if (expand_packer instanceof PaddedPackerI) {
			((PaddedPackerI)expand_packer).setParentSpacer(spacer);
		}
	}

	public double getSpacer() {
		return spacer;
	}

	/** Sets the color used to draw the outline.
	 *  @param color   A color, or null if no outline is desired.
	 */
	public void setOutlineColor(Color color) {
		outline_color = color;
	}

	/**
	 * Returns the color used to draw the outline,
	 * or null if there is no outline.
	 */
	public Color getOutlineColor() {
		return outline_color;
	}

	/** Equivalent to {@link #setBackgroundColor(Color)}. */
	public void setFillColor(Color col) {
		fill_color = col;
		if (col != null) super.setBackgroundColor(col);
	}

	/** Equivalent to {@link #getBackgroundColor()}. */
	public Color getFillColor() {
		return fill_color;
	}

	/** Equivalent to {@link #setFillColor(Color)}. */
	@Override
	public void setBackgroundColor(Color color)  {
		setFillColor(color);
	}

	/** Equivalent to {@link #getFillColor()}. */
	@Override
	public Color getBackgroundColor()  {
		return getFillColor();
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	/**
	 * Set the color used to draw the label.
	 * Equivalent to {@link #setLabelColor(Color)}.
	 * @see #setLabel(String)
	 */
	@Override
	public void setForegroundColor(Color color)  {
		setLabelColor(color);
	}

	/**
	 * Returns the color used to draw the label.
	 * Equivalent to {@link #getLabelColor}.
	 * @see #setLabel(String)
	 */
	@Override
	public Color getForegroundColor()  {
		return getLabelColor();
	}

	/**
	 * Set the color used to draw the label.
	 * @see #setLabel(String)
	 */
	public void setLabelColor(Color color) {
		label_color = color;
		if (label_color != null) super.setForegroundColor(color);
	}

	/**
	 * Returns the color used to draw the label.
	 *
	 * @see #setLabel(String)
	 */
	public Color getLabelColor() {
		return label_color;
	}

	/** Set whether or not the tier should be hit by the mouse. */
	public void setHitable(boolean h) {
		hitable = h;
	}

	/** Get whether or not the tier should be hit by the mouse. */
	@Override
	public boolean isHitable() {
		return hitable;
	}

	/**
	 * Set whether or not the tier wants to allow itself to be hidden.
	 * The state of this flag has no effect
	 * on whether or not setState(HIDDEN) will work.
	 */
	public void setHideable(boolean h) {
		this.hideable = h;
	}

	/**
	 * Get whether or not the tier wants to allow itself to be hidden.
	 * The state of this flag has no effect on whether setState(HIDDEN)
	 * will work or not.
	 */
	public boolean isHideable() {
		return this.hideable;
	}

	/** Set whether or not the tier's label should be displayed. */
	public void setShowLabel(boolean s) {
		showLabel = s;
	}

	/** Get whether or not the tier's label should be displayed. */
	public boolean isShowLabel() {
		return showLabel;
	}

	/**
	 * for use in cleaning up references to facilitate gc'ing.
	 */
	public void removeAllTierStateChangeListeners() {
		tierStateChangeListeners.clear();
	}

	/** Add a TierStateChangeListener to the audience. */
	public void addTierStateChangeListener(TierStateChangeListener tl) {
		tierStateChangeListeners.add(tl);
	}

	/** Remove a TierStateChangeListener from the audience. */
	public void removeTierStateChangeListener(TierStateChangeListener tl) {
		tierStateChangeListeners.remove(tl);
	}

	/** Tell all listeners of a TierStateChangeEvent. */
	public void notifyTierStateChangeListeners(TierStateChangeEvent evt) {
		for (TierStateChangeListener tl : tierStateChangeListeners) {
			tl.heardTierStateChangeEvent(evt);
		}
	}

	/**
	 * Returns a string representing the state of this object.
	 * @see #setState
	 */
	public String getStateString() {
		return getStateString(getState());
	}

	/**
	 * Converts the given state constant into a human-readable string.
	 * @see #setState
	 */
	public final static String getStateString(int astate) {
		if (astate == HIDDEN) { return "HIDDEN"; }
		else if (astate == COLLAPSED) { return "COLLAPSED"; }
		else if (astate == EXPANDED) { return "EXPANDED"; }
		else if (astate == FIXED_SIZE) { return "FIXED_SIZE"; }
		else { return "UNKNOWN"; }
	}

	/**
	 * Converts the given strand constant into a human-readable string.
	 * @see #setStrand
	 */
	public final static String getStrandString(int strand) {
		if (strand == FORWARD_STRAND) { return "FORWARD_STRAND"; }
		else if (strand == REVERSE_STRAND) { return "REVERSE_STRAND"; }
		else if (strand == STRAND_INSENSITIVE) { return "STRAND_INSENSITIVE"; }
		else { return "INVALID STRAND"; }
	}

	@Override
	public final String toString() { return label; }

}
