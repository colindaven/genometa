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

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 *  A glyph that displays as a centered line and manipulates children to center on the
 *  same line.
 *
 *  LineContainerGlyph is very convenient for representing data that has a range but
 *  has multiple sub-ranges within it, such as genes which have a known intron/exon
 *  structure.
 */
public class LineContainerGlyph extends Glyph  {

	public void draw(ViewI view) {
		view.transformToPixels(coordbox, pixelbox);
		if (pixelbox.width == 0) { pixelbox.width = 1; }
		if (pixelbox.height == 0) { pixelbox.height = 1; }
		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());

		// temp fix for AWT drawing bug when rect gets too big.
		Rectangle compbox = view.getComponentSizeRect();
		pixelbox = pixelbox.intersection(compbox);

		// We use fillRect instead of drawLine, because it may be faster.
		g.fillRect(pixelbox.x, pixelbox.y+pixelbox.height/2, pixelbox.width, 1);

		super.draw(view);
	}

	/**
	 *  overriding addChild to force children to center on line
	 */
	public void addChild(GlyphI glyph) {
		// child.cbox.y is modified, but not child.cbox.height)
		// center the children of the LineContainerGlyph on the line
		Rectangle2D.Double cbox = glyph.getCoordBox();
		double ycenter = this.coordbox.y + this.coordbox.height/2;
		cbox.y = ycenter - cbox.height/2;
		super.addChild(glyph);
	}

	//
	//  Kind of twisted -- if child of this glyph is hit, then this glyph _wasn't_ hit
	//    only for hits though, not for intersection
	//    THIS BEHAVIOR COMMENTED OUT FOR NOW
	//
	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		calcPixels(view);
		return  isVisible && pixel_hitbox.intersects(pixelbox);
	}

	//
	//  Kind of twisted -- if child of this glyph is hit, then this glyph _wasn't_ hit
	//    only for hits though, not for intersection
	//    THIS BEHAVIOR COMMENTED OUT FOR NOW
	//
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return isVisible && coord_hitbox.intersects(coordbox);
	}

}
