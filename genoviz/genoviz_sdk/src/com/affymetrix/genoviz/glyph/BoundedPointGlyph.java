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

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *  A useful glyph for representing a point feature with bounded uncertainty.
 */
public class BoundedPointGlyph extends SolidGlyph  {

	public void draw(ViewI view) {
		view.transformToPixels(coordbox, pixelbox);
		if (pixelbox.width == 0) { pixelbox.width = 1; }
		if (pixelbox.height == 0) { pixelbox.height = 1; }
		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());
		g.fillRect(pixelbox.x, pixelbox.y+pixelbox.height/2, pixelbox.width, 1);
		g.fillRect(pixelbox.x+pixelbox.width/2-1, pixelbox.y, 3, pixelbox.height);
		g.fillRect(pixelbox.x, pixelbox.y, 1, pixelbox.height);
		g.fillRect(pixelbox.x+pixelbox.width, pixelbox.y, 1, pixelbox.height);
		super.draw(view);
	}

}
