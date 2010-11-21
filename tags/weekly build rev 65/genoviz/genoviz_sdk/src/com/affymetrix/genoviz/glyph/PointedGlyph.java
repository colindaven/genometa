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

import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.Graphics;

/**
 * A simple arrow-like glyph.
 * It fills its containing pixelbox with the exception of two corners.
 * These are the corners on the "end" coord of the primary axis.
 * It is suitable for depicting sequence features
 * that have a forward or reverse direction.
 */
public class PointedGlyph extends DirectedGlyph {

	public void draw(ViewI theView) {
		theView.transformToPixels(this.coordbox, this.pixelbox);
		if (this.pixelbox.width == 0) { this.pixelbox.width = 1; }
		if (this.pixelbox.height == 0) { this.pixelbox.height = 1; }
		Graphics g = theView.getGraphics();
		g.setColor(getBackgroundColor());
		int x[] = new int[6];
		int y[] = new int[6];
		int halfThickness = 1;
		if (HORIZONTAL == this.getOrientation() && this.isForward()) {
			halfThickness = (pixelbox.height-1)/2;
			x[0] = pixelbox.x;
			x[2] = pixelbox.x+pixelbox.width;
			x[1] = Math.max(x[0]+1, (x[2]-halfThickness));
			x[3] = x[1]-1;
			x[4] = x[0];
			y[0] = pixelbox.y;
			y[1] = y[0];
			y[2] = y[0] + halfThickness;
			y[3] = y[0] + pixelbox.height;
			y[4] = y[3];
		}
		else if (HORIZONTAL == this.getOrientation() && !this.isForward()) {
			halfThickness = (pixelbox.height-1)/2;
			x[0] = pixelbox.x;
			x[2] = x[0] + pixelbox.width;
			x[1] = Math.min(x[2]-1, x[0]+halfThickness);
			x[3] = x[2];
			x[4] = x[1]+1;
			y[1] = pixelbox.y;
			y[0] = y[1] + halfThickness;
			y[2] = y[1];
			y[3] = y[1] + pixelbox.height;
			y[4] = y[3];
		}
		else if (VERTICAL == this.getOrientation() && this.isForward()) {
			halfThickness = (pixelbox.width-1)/2;
			x[0] = pixelbox.x;
			x[1] = pixelbox.x+pixelbox.width;
			x[3] = x[0] + halfThickness;
			x[2] = x[1];
			x[4] = x[0];
			y[0] = pixelbox.y;
			y[1] = y[0];
			y[3] = y[0] + pixelbox.height;
			y[2] = Math.max(y[3]-halfThickness, y[0])-1;
			y[4] = y[2];
		}
		else if (VERTICAL == this.getOrientation() && !this.isForward()) {
			halfThickness = (pixelbox.width)/2;
			x[0] = pixelbox.x + pixelbox.width;
			x[1] = pixelbox.x;
			x[2] = x[1];
			x[4] = x[0];
			x[3] = x[1] + halfThickness;
			y[3] = pixelbox.y;
			y[0] = y[3]+pixelbox.height;
			y[1] = y[0];
			y[2] = Math.min(y[3]+halfThickness, y[0]);
			y[4] = y[2];
		}
		g.fillPolygon(x, y, 5);
	}

}
