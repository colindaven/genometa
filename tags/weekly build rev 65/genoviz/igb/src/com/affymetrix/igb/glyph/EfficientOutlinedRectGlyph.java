/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.glyph;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *  A glyph that is drawn as a solid, outlined rectangle.
 */
public final class EfficientOutlinedRectGlyph extends EfficientOutlineContGlyph  {
  private Color bgcolor = Color.white;
  
	@Override
  public void draw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this.getCoordBox(), pixelbox);
    
    pixelbox = fixAWTBigRectBug(view, pixelbox);
    
    pixelbox.width = Math.max ( pixelbox.width, min_pixels_width );
    pixelbox.height = Math.max ( pixelbox.height, min_pixels_height );
    
    Graphics g = view.getGraphics();
    g.setColor(getColor());
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    if (pixelbox.width > 2 && pixelbox.height > 2) {
      g.setColor(bgcolor);
      g.fillRect(pixelbox.x+1, pixelbox.y+1, pixelbox.width-2, pixelbox.height-2);
    }

	//MPTAG added
	//addDirectionArrow(view);

    super.draw(view);
  }

	/**
	 * MPTAG
	 * Methode um an die Glyphe einen Richtungspfeil anzuhängen. Die Größe des Pfeils wird durch die
	 * skalierung des Views *3 bestimmt
	 * TODO ggf den Pfeil bei einem Pixel nicht mehr zeichnen
	 */
	private void addDirectionArrow( ViewI view){
		int numPoints = 3;
		int arrowWidth = 5;
		arrowWidth =  (int) (3 * (view.getTransform()).getScaleX());
		int[] xpts = new int[numPoints];
		int[] ypts = new int[numPoints];
		if(this.getDirection() == 1){
			//Pfeil nach Rechts
			xpts[0] = pixelbox.x+pixelbox.width; //Ecke oben an der Glyphe
			ypts[0] = pixelbox.y;
			xpts[1] = pixelbox.x+pixelbox.width; //Ecke unten an der Glyphe
			ypts[1] = pixelbox.y + pixelbox.height;
			xpts[2] = pixelbox.x+pixelbox.width + arrowWidth; //Spitze des Pfeils
			ypts[2] = pixelbox.y + (pixelbox.height / 2);
		}else if(this.getDirection() == 2){
			//Pfeil nach links
			xpts[0] = pixelbox.x; //Ecke oben an der Glyphe
			ypts[0] = pixelbox.y;
			xpts[1] = pixelbox.x; //Ecke unten an der Glyphe
			ypts[1] = pixelbox.y + pixelbox.height;
			xpts[2] = pixelbox.x - arrowWidth; //Spitze des Pfeils
			ypts[2] = pixelbox.y + (pixelbox.height / 2);
		}
		Graphics g = view .getGraphics();
		Color c = g.getColor();
		g.setColor(Color.RED);
		g.fillPolygon(xpts, ypts, numPoints);
		g.setColor(c);
	}
  
  /** Sets the outline color; the fill color is automatically calculated as  
   *  a darker shade. 
   */
	@Override
  public void setColor(Color c) {
    super.setColor(c);
    bgcolor = c.darker();
  }
}
