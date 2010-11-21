/**
*   Copyright (c) 2007 Affymetrix, Inc.
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

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.RoundRectangle2D;

/**
 * A glyph that is drawn as a outlined rounded rectangle with a transparent
 * center in the shape of a rounded rectangle, through which the 
 * children can be seen.  Outside of the rounded rectangle, all children
 * are clipped (and thus invisible).  This can be used, for instance, as
 * a glyph representing one arm of a chromosome, with the cytobands
 * as children.
 */
public final class RoundRectMaskGlyph extends Glyph  {

  private static final BasicStroke stroke = new BasicStroke(2);
  private final RoundRectangle2D.Double rr2d = new RoundRectangle2D.Double();
  private Color fillColor = Color.WHITE;
  
  public RoundRectMaskGlyph(Color fillColor) {
    super();
    this.fillColor = fillColor;
    this.setDrawOrder(Glyph.DRAW_CHILDREN_FIRST);
  }
  
  RoundRectangle2D.Double getShapeInPixels(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this.getCoordBox(), pixelbox);

    // turning off fixAWTBigRectBug  because it is probably unnecessary and because
    // it causes the glyph to draw the round edges at the edges of the view, even when
    // zoomed in such that the edge of the view is not the edge of the coord space.
    //pixelbox = fixAWTBigRectBug(view, pixelbox);

    if (pixelbox.width < min_pixels_width) { pixelbox.width = min_pixels_width; }
    if (pixelbox.height < min_pixels_height) { pixelbox.height = min_pixels_height; }
    Graphics g = view.getGraphics();
    g.setColor(getBackgroundColor());

    //Point arcSize = view.transformToPixels(new Point2D(arcWidth, arcHeight), new Point());
    int arcSize = Math.min(pixelbox.width, pixelbox.height);
    rr2d.setRoundRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height,
      arcSize, arcSize);
    
    return rr2d;
  }

  /** Clears the region behind the glyph (a rectangle minus a rounded rectangle, 
   *  then draws the outline using a BasicStroke.  This is designed such that
   *  drawChildren() should be called before draw.
   */
  public void draw(ViewI view) {    
    RoundRectangle2D.Double s = getShapeInPixels(view);
    Area a = new Area(s.getFrame());
    a.subtract(new Area(s));

    Graphics2D g2 = view.getGraphics();
    g2.setColor(fillColor);
    g2.fill(a);

    g2.setColor(getColor());
    g2.setStroke(stroke);
    g2.draw(s);    
  }
}
