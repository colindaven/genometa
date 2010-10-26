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

import java.awt.Color;
import java.awt.Graphics;
import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *  A glyph that zigzags.
 *  Note that the background color is ignored!
 *  This glyph does not draw its background.
 */
public class SquiggleGlyph extends SolidGlyph  {
	// width of one half of squiggle turn;
	int preferred_halfsegment = 5;

	public void draw(ViewI view) {
		view.transformToPixels(coordbox, pixelbox);
		if (pixelbox.width == 0) { pixelbox.width = 1; }
		if (pixelbox.height == 0) { pixelbox.height = 1; }
		Graphics g = view.getGraphics();

		g.setColor(getBackgroundColor());
		if (pixelbox.width < 2*preferred_halfsegment || pixelbox.height < 3) {
			g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
		}
		else {
			int halfsegment = Math.min(pixelbox.height-1,preferred_halfsegment);
			halfsegment = Math.min(pixelbox.width/2, halfsegment);
			int half_segments = (pixelbox.width-1) / halfsegment;
			int full_segments = (pixelbox.width-1) / (halfsegment*2);
			int xleft = pixelbox.x;
			int xright;
			int ytop = pixelbox.y;
			int ybot = pixelbox.y+pixelbox.height-1;

			for (int i=0; i<full_segments; i++) {
				xright = xleft + halfsegment;
				g.drawLine(xleft, ybot, xright, ytop);
				xleft = xright;
				xright = xleft + halfsegment;
				g.drawLine(xleft, ytop, xright, ybot);
				xleft = xright;
			}
			boolean even_segments = (half_segments%2 == 0);

			// if even number of half segments, go up on final incomplete segment
			if (even_segments) {
				xright = pixelbox.x+pixelbox.width-1;
				int width = xright-xleft;
				if (width > 1) {
					g.drawLine(xleft, ybot, xright, ybot-((ybot-ytop)*width)/halfsegment);
				}
			}
			// else odd number of half segments, go down on final incomplete segment
			else {
				xright = xleft + halfsegment;
				g.drawLine(xleft, ybot, xright, ytop);
				xleft = xright;
				xright = pixelbox.x+pixelbox.width-1;
				int width = xright-xleft;
				if (width > 1) {
					g.drawLine(xleft, ytop, xright, ytop+((ybot-ytop)*width)/halfsegment);
				}
			}
		}
		super.draw(view);
	}

	/**
	 * @deprecated use {@link #setForegroundColor}.
	 */
	@Deprecated
		public void setColor( Color c ) {
			setForegroundColor( c );
		}

	/**
	 * @deprecated use {@link #getForegroundColor}.
	 */
	@Deprecated
		public Color getColor() {
			return getForegroundColor();
		}

}
