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

package com.affymetrix.genoviz.glyph;

import java.awt.Graphics;
import java.awt.Rectangle;

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 * A glyph that is drawn as a solid rectangle to represent a break in
 * an alignment.
 */
public class GapGlyph extends SolidGlyph  {

	public void draw(ViewI view) {
		view.transformToPixels(coordbox, pixelbox);
		if (pixelbox.width < mSizeThreshold) { pixelbox.width = mSizeThreshold; }
		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());

		Rectangle compbox = view.getComponentSizeRect();
		pixelbox = pixelbox.intersection(compbox);

		// draw the box
		// Note: the cost of the cull test should be miniscule compared with
		// the cost of what follows when it tests positive.
		if(pixelbox.width + pixelbox.height > mSizeThreshold)
			g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
		super.draw(view);
	}

	/**
	 * Facility to skip drawing of rect glyphs that are smaller than a
	 * given threshold which is the sum of their width and height.
	 * Therefore the value 0 (the default) will draw all GapGlyphs.
	 */
	public static int getCullThreshold() { return mSizeThreshold; }
	public static void setCullThreshold(int threshold) {
		mSizeThreshold = threshold;
	}
	private static int mSizeThreshold = 0;
}
