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
import java.awt.Polygon;
import java.awt.Rectangle;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.util.NeoConstants;

/**
 * An arrow glyph.
 */
public class ArrowGlyph extends DirectedGlyph  {

	private int x[];
	private int y[];
	private final Rectangle stem = new Rectangle();
	private static final int MAX_STEM_WIDTH = 25;
	private int stemWidth, headX, headY;
	private final Polygon poly;

	protected boolean fillArrowHead = true;
	protected boolean fillStem = true;

	public ArrowGlyph() {
		x = new int[3];
		y = new int[3];
		poly = new Polygon(x, y, 3);
		setStemWidth(5);
	}

	/**
	 * Sets the stem width of the arrow.
	 * This also sets arrow head's height & width.
	 * Stem width is forced to be odd.
	 * @param theWidth of the arrow's stem in pixelspace
	 */
	private void setStemWidth(int theWidth) {
		this.stemWidth = Math.max( Math.min( theWidth, MAX_STEM_WIDTH ), 1 );
		if ( 0 == this.stemWidth % 2 ) this.stemWidth--;
		this.headY = 2*this.stemWidth+4;
		this.headX = this.headY/2;
	}

	public void draw(ViewI view) {
		double hold_y = coordbox.y;

		coordbox.y = coordbox.y + (coordbox.height / 2);
		view.transformToPixels(coordbox, pixelbox);
		int offset_center = pixelbox.y;
		coordbox.y = hold_y;
		view.transformToPixels(coordbox, pixelbox);

		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());


		/* Note that the arrow glyph seems to point in the direction
		 * opposite to what might be expected.
		 * This is for backward compatibility.
		 * We don't want to break the NeoAssembler, in particular.
		 * Perhaps this should be corrected at some point.
		 * When that time comes, EAST and WEST should be switched,
		 * as should be SOUTH and NORTH.
		 */
		switch ( this.getDirection() ) {
			case EAST:  // forward strand
				this.stem.x = pixelbox.x;
				this.stem.y = offset_center - stemWidth/2;
				this.stem.width = pixelbox.width - headX;
				this.stem.height = stemWidth;
				drawArrowHead (g, pixelbox.x + pixelbox.width,
						pixelbox.x + pixelbox.width - headX,
						offset_center);
				break;
			case WEST:
				this.stem.x = pixelbox.x + headX;
				this.stem.y = offset_center - stemWidth/2;
				this.stem.width = pixelbox.width - headX;
				this.stem.height = stemWidth;
				drawArrowHead (g, pixelbox.x,
						pixelbox.x + headX,
						offset_center);
				break;
			case SOUTH:  // forward strand
				this.stem.x = pixelbox.x + ( pixelbox.width - stemWidth ) / 2;
				this.stem.y = pixelbox.y;
				this.stem.width = stemWidth;
				this.stem.height = pixelbox.height - headX;
				drawArrowHead (g, pixelbox.y + pixelbox.height,
						pixelbox.y + pixelbox.height - headX,
						pixelbox.x + pixelbox.width/2 );
				break;
			case NORTH:  // reverse strand
				this.stem.x = pixelbox.x + ( pixelbox.width - stemWidth ) / 2;
				this.stem.y = pixelbox.y + headX;
				this.stem.width = stemWidth;
				this.stem.height = pixelbox.height - headX;
				drawArrowHead (g, pixelbox.y,
						pixelbox.y + headX,
						pixelbox.x + pixelbox.width/2 );
				break;
			default:
		}
		g.fillRect( this.stem.x, this.stem.y,
				this.stem.width, this.stem.height);

		super.draw(view);
	}

	/**
	 * draws the triangle that forms the head of the arrow.
	 * Note that the "x"s in the parameter names hark back
	 * to a time when arrows did not work on vertical maps.
	 */
	private void drawArrowHead(Graphics g, int tip_x, int flat_x, int tip_center) {
		x = poly.xpoints;
		y = poly.ypoints;
		switch ( this.getOrientation() ) {
			case NeoConstants.HORIZONTAL:
				x[0] = flat_x;
				y[0] = tip_center - headY/2;
				x[1] = tip_x;
				y[1] = tip_center;
				x[2] = flat_x;
				y[2] = tip_center + headY/2;
				break;
			case NeoConstants.VERTICAL:
				y[0] = flat_x;
				x[0] = tip_center - headY/2;
				y[1] = tip_x;
				x[1] = tip_center;
				y[2] = flat_x;
				x[2] = tip_center + headY/2;
				break;
		}
		if (fillArrowHead) {
			g.fillPolygon(x, y, 3);
		}
		else {
			g.drawPolygon(x, y, 3);
		}
	}

}
