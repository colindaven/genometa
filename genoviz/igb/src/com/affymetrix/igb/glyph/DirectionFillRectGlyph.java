/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.LabelledRectGlyph;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D.Double;
/**
 *
 * @author Elmo
 */
public class DirectionFillRectGlyph extends LabelledRectGlyph {

	private boolean isStackedGlyph = false;

	@Override
	public void draw(ViewI view) {
		//MPTAG changed
		super.draw(view);

		//MPTAG kopiert aus superkalsse
		view.transformToPixels(coordbox, pixelbox);
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
		Graphics2D g = view.getGraphics();
//		g.setColor(Color.BLUE);
//		g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
		if(!isStackedGlyph)
			g.drawImage(TextureCache.getInstance().getImage(pixelbox.width, pixelbox.height),
				pixelbox.x, pixelbox.y, null);
	}

	public void setIsStackedGlyph(boolean b){
		isStackedGlyph = b;
	}
}
