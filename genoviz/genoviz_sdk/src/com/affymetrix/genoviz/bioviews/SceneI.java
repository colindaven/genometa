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

package com.affymetrix.genoviz.bioviews;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 * A SceneI is an abstract two dimensional space with x and y coordinates
 * specified as doubleing point numbers.  The scene contains a rooted
 * hierarchy of GlyphI objects which have been placed onto the scene at
 * specific coordinates.  The scene also can have multiple views that look
 * onto the scene and manage the visual representation of a scene on
 * an AWT component.
 *
 * <p> SceneI, along with ViewI and GlyphI, is one of the three fundamental
 * interfaces comprising Affymetrix' inner 2D structured graphics
 * architecture.
 */
public interface SceneI {

	/**
	 * do not distinguish selected glyphs
	 * from non-selected glyphs.
	 * @see #setSelectionAppearance
	 * @see #getSelectionAppearance
	 */
	public static final int SELECT_NONE = 100;

	/**
	 * distinguish selected glyph
	 * from non-selected glyphs
	 * by outlining them with selection color
	 * @see #setSelectionAppearance
	 * @see #getSelectionAppearance
	 * @see #setSelectionColor
	 */
	public static final int SELECT_OUTLINE = 101;

	/**
	 * distinguish selected glyph
	 * from non-selected glyphs
	 * by filling them with selection color.
	 * @see #setSelectionAppearance
	 * @see #getSelectionAppearance
	 * @see #setSelectionColor
	 */
	public static final int SELECT_FILL = 102;

	/**
	 * distinguish selected glyph
	 * from non-selected glyphs
	 * by filling rectangle behind them with selection color.
	 * @see #setSelectionAppearance
	 * @see #getSelectionAppearance
	 * @see #setSelectionColor
	 */
	public static final int BACKGROUND_FILL = 103;

	/**
	 * distinguish selected glyph
	 * from non-selected glyphs
	 * by reversing forground and background colors.
	 * @see #setSelectionAppearance
	 * @see #getSelectionAppearance
	 * @see #setSelectionColor
	 */
	public static final int SELECT_REVERSE = 104;

	/**
	 *  returns the selection style to apply to glyphs within this scene
	 *  possible return values: NONE, OUTLINE, FILL, HIGHLIGHT
	 */
	public void setSelectionAppearance(int id);

	/**
	 *  returns the selection appearance  to apply to glyphs within this scene
	 *  possible return values: NONE, OUTLINE, FILL, HIGHLIGHT
	 */
	public int getSelectionAppearance();

	// needed in interface for Glyph implementation
	// (could remove by specifying Scene in glyph...)
	/**
	 * return color for selected glyphs within this scene
	 */
	public Color getSelectionColor();

	/**
	 *  return color for selected glyphs within this scene
	 */
	public void setSelectionColor(Color col);

	/**
	 *  Add a view onto the scene
	 */
	public void addView(ViewI view);

	/**
	 *  Remove a view from the scene
	 */
	public void removeView(ViewI view);

	/**
	 *  Return a Vector of all views onto the scene
	 */
	public List<ViewI> getViews();

	/**
	 *  Draw all the views of this scene
	 */
	public void draw();  // draw all views on all canvases

	/**
	 *  Draw a particular view of this scene
	 */
	public void draw(ViewI v);  // draw one view

	/**
	 *  Draw all the views of this scene that use Component c.
	 */
	public void draw(Component c, Graphics2D g); // draw one canvas
	//  (maybe this should be implementation, not interface?)

	/**
	 *  Add a glyph to the scene
	 */
	public void addGlyph(GlyphI glyph);

	/**
	 *  Insert a glyph into the top level of the glyph hierarchy at position i
	 */
	public void addGlyph(GlyphI glyph, int i);

	/**
	 *  return the coordinate bounds of the entire scene
	 */
	public Rectangle2D.Double getCoordBox();

}
