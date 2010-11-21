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

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.util.*;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class LineStretchContainerGlyph extends Glyph {

	public LineStretchContainerGlyph() {
		super();
	}


	/**
	 *  overriding addChild to force children to center on line
	 */
	@Override
	public void addChild(GlyphI glyph) {
		super.addChild(glyph);

		// if first child, then fit to it
		Rectangle2D.Double cbox = glyph.getCoordBox();
		if (getChildren() == null || getChildren().size() <= 1) {
			this.setCoords(cbox.x, cbox.y, cbox.width, cbox.height);
		}
		else {
			coordbox.add(cbox);
		}

		double ycenter = this.coordbox.y + this.coordbox.height/2;
		cbox.y = ycenter - cbox.height/2;
	}

	/**
	 *  overriding removeChild so that LineStretchContainer shrinks to fit
	 *  remaining children
	 */
	public void removeChild(GlyphI glyph) {
		super.removeChild(glyph);
		List child_glyphs = this.getChildren();
		if (child_glyphs == null || child_glyphs.size() <= 0) {
			// what should be done if no children left???
		}
		else {
			GlyphI child = (GlyphI)child_glyphs.get(0);
			Rectangle2D.Double childbox = child.getCoordBox();
			this.setCoords(childbox.x, childbox.y, childbox.width, childbox.height);
			for (int i=1; i<child_glyphs.size(); i++) {
				child = (GlyphI)child_glyphs.get(i);
				childbox = child.getCoordBox();
				coordbox.add(childbox);
			}
		}
	}

	public void draw(ViewI view) {
		view.transformToPixels(coordbox, pixelbox);
		if (pixelbox.width == 0) { pixelbox.width = 1; }
		if (pixelbox.height == 0) { pixelbox.height = 1; }
		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());
		g.fillRect(pixelbox.x, pixelbox.y+pixelbox.height/2, pixelbox.width, 1);
		super.draw(view);
	}


	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		calcPixels(view);
		return isVisible && pixel_hitbox.intersects(pixelbox);
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return isVisible && coord_hitbox.intersects(coordbox);
	}


	


}
