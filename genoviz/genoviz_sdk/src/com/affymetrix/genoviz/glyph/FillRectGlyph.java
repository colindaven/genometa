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
 * A glyph that is drawn as a solid rectangle.
 */
public class FillRectGlyph extends SolidGlyph  {

	public void draw(ViewI view) {
		view.transformToPixels(coordbox, pixelbox);

		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());

		// temp fix for AWT drawing bug when rect gets too big -- GAH 2/6/98
		Rectangle compbox = view.getComponentSizeRect();
		pixelbox = pixelbox.intersection(compbox);

		// If the coordbox was specified with negative width or height,
		// convert pixelbox to equivalent one with positive width and height.
		// Constrain abs(width) or abs(height) by min_pixels.
		// Here I'm relying on the fact that min_pixels is positive.
		if (coordbox.width < 0) {
			pixelbox.width = -Math.min(pixelbox.width, -min_pixels_width);
			pixelbox.x -= pixelbox.width;
		}
		else pixelbox.width = Math.max ( pixelbox.width, min_pixels_width );
		if (coordbox.height < 0) {
			pixelbox.height = -Math.min(pixelbox.height, -min_pixels_height);
			pixelbox.y -= pixelbox.height;
		}
		else pixelbox.height = Math.max ( pixelbox.height, min_pixels_height );

		// draw the box
		g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

		super.draw(view);
	}
}
