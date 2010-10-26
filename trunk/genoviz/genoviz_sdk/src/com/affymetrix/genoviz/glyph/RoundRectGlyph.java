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
 * A glyph that is drawn as a solid rectangle with rounded edges.
 */
public class RoundRectGlyph extends SolidGlyph  {

	int arcWidth = 3;
	int arcHeight = 3;
	int changeWidth = 0;

	public void setArcWidth(int w) {
		arcWidth = w;
	}

	public void setArcHeight(int h) {
		arcHeight = h;
	}

	/**
	 * Sets a width at which glyphs change from drawing as
	 * RoundRectGlyphs and switch to FillRectGlyphs.  If you
	 * have many RoundRectGlyphs in your NeoMap, allows for
	 * reduced computation involved in computing all of those
	 * rounded corners.
	 * Default value is 0.
	 */
	public void setChangeWidth ( int width ) {
		changeWidth = width;
	}

	public void draw(ViewI view) {
		view.transformToPixels(coordbox, pixelbox);
		if (pixelbox.width == 0) { pixelbox.width = 1; }
		if (pixelbox.height == 0) { pixelbox.height = 1; }
		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());

		// temp fix for AWT drawing bug when rect gets too big -- GAH 2/6/98
		Rectangle compbox = view.getComponentSizeRect();
		pixelbox = pixelbox.intersection(compbox);
		if ( pixelbox.width < changeWidth )
			g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width,
					pixelbox.height);
		else
			g.fillRoundRect(pixelbox.x, pixelbox.y, pixelbox.width,
					pixelbox.height,arcWidth,arcHeight);
		super.draw(view);
	}

}
