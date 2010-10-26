/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 *  A new version of OutlineRectGlyph, 
 *     subclassed from EfficientGlyph instead of Glyph.
 */
public class EfficientOutlineContGlyph extends EfficientSolidGlyph  {
  private static final boolean DEBUG_OPTIMIZED_FILL = false;
  private boolean move_children = true;
  private Color fill_color = null;

	@Override
	public void drawTraversal(ViewI view) {
		Rectangle pixelbox = view.getScratchPixBox();
		view.transformToPixels(this.getCoordBox(), pixelbox);
		if (isVisible && withinView(view)) {
			if (pixelbox.width <= 3 || pixelbox.height <= 3) {
				// still ends up drawing children for selected, but in general
				//    only a few glyphs are ever selected at the same time, so should be fine
				if (selected) {
					drawSelected(view);
				} else {
					fillDraw(view);
				}
			} else {
				super.drawTraversal(view);  // big enough to draw children
			}
		}
	}

  public void fillDraw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this.getCoordBox(), pixelbox);
    Graphics g = view.getGraphics();
    if (DEBUG_OPTIMIZED_FILL) {
      g.setColor(Color.white);
    }
    else {
      g.setColor(this.getBackgroundColor());
    }
    
    pixelbox = fixAWTBigRectBug(view, pixelbox);

    if (pixelbox.width < 1) { pixelbox.width = 1; }
    if (pixelbox.height < 1) { pixelbox.height = 1; }
    // draw the box
    g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

    super.draw(view);
  }
    
	@Override
  public void draw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this.getCoordBox(), pixelbox);
    if (pixelbox.width == 0) { pixelbox.width = 1; }
    if (pixelbox.height == 0) { pixelbox.height = 1; }

    pixelbox = fixAWTBigRectBug(view, pixelbox);

    Graphics g = view.getGraphics();
    if (getFillColor() != null) {
      g.setColor(getFillColor());
      g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
    }
    g.setColor(getBackgroundColor());
    g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

    super.draw(view);
  }

  /**
   *  Overridden to force children to center on line
   */
	@Override
  public void addChild(GlyphI glyph) {
    if (isMoveChildren()) {
      // center the child vertically in the parent
      // child.cbox.y is modified, but not child.cbox.height)
      Rectangle2D.Double cbox = glyph.getCoordBox();
      double ycenter = this.getCoordBox().y + this.getCoordBox().height/2;
      cbox.y = ycenter - cbox.height/2;
    }
    super.addChild(glyph);
  }

	@Override
  public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
    return isVisible && coord_hitbox.intersects(this.getCoordBox());
  }

  /** Sets the fill color. Use null if you do not want the rectangle filled. 
   *  Default is null.
   */
  public void setFillColor(Color c) {
    this.fill_color = c;
  }
  
  /** Gets the fill color. Default is null. 
   *  @return a color or null.
   */
  public Color getFillColor() {
    return this.fill_color;
  }

  /**
   * If true, {@link #addChild(GlyphI)} will automatically center the child vertically.
   * Default is true.
   */
  public boolean isMoveChildren() {
    return this.move_children;
  }  

  /**
   * Set whether {@link #addChild(GlyphI)} will automatically center the child vertically.
   */
  public void setMoveChildren(boolean move_children) {
    this.move_children = move_children;
  }
}
