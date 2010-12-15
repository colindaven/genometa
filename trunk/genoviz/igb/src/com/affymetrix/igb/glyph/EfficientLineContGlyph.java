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

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.List;

/**
 *  A glyph that displays as a centered line and manipulates children to center on the
 *  same line.
 *
 *  Very convenient for representing data that has a range but
 *  has multiple sub-ranges within it, such as genes which have a known intron/exon
 *  structure.
 *
 *  This is a new version of ImprovedLineContGlyph,
 *     subclassed from EfficientGlyph instead of Glyph,
 *     and renamed EfficientLineContGlyph.
 *
 *  Optimized to just draw a filled rect if glyph is small, and skip drawing children
 *
 */
public final class EfficientLineContGlyph extends EfficientSolidGlyph  {
  private static final boolean DEBUG_OPTIMIZED_FILL = false;
  private boolean move_children = true;

  private boolean isStackedGlyph = false;

  @Override
  public void drawTraversal(ViewI view) {
		Rectangle pixelbox = view.getScratchPixBox();

//			if(coordbox.height > GenericAnnotGlyphFactory.GLYPH_HEIGHT){
//				coordbox.height = GenericAnnotGlyphFactory.GLYPH_HEIGHT;
//				this.adjustChildren();//MPTAG added
//			}
		view.transformToPixels(this.getCoordBox(), pixelbox);
		if (withinView(view) && isVisible) {
			if (pixelbox.width <= 3 || pixelbox.height <= 3) {//MPTAG hier wird entschieden wieviel gezeichnet wird.// orig 3
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
    Graphics2D g = view.getGraphics();
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

	//MPTAG hier ggf statt dem Rechteck ein Bild zeichnen
	g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
	if(!isStackedGlyph)
		g.drawImage(TextureCache.getInstance().getImage(pixelbox.width, pixelbox.height),
				pixelbox.x, pixelbox.y, null);


    super.draw(view);
  }

  @Override
  public void draw(ViewI view) {
    Rectangle pixelbox = view.getScratchPixBox();
    view.transformToPixels(this.getCoordBox(), pixelbox);
    if (pixelbox.width == 0) { pixelbox.width = 1; }
    if (pixelbox.height == 0) { pixelbox.height = 1; }
    Graphics g = view.getGraphics();
    g.setColor(getBackgroundColor());

    pixelbox = fixAWTBigRectBug(view, pixelbox);

    // We use fillRect instead of drawLine, because it may be faster.
    g.fillRect(pixelbox.x, pixelbox.y+pixelbox.height/2, pixelbox.width, 1);

    super.draw(view);
  }

  /**
   *  If {@link #isMoveChildren()}, forces children to center on line.
   */
  @Override
  public void addChild(GlyphI glyph) {
    if (isMoveChildren()) {
      double child_height = adjustChild(glyph);
      super.addChild(glyph);
      if (child_height > this.getCoordBox().height) {
        this.getCoordBox().height = child_height;
        adjustChildren();
      }
    } else {
      super.addChild(glyph);
    }
  }


  @Override
  public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
    return isVisible && coord_hitbox.intersects(this.getCoordBox());
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

  protected double adjustChild(GlyphI child) {
    if (isMoveChildren()) {
      // child.cbox.y is modified, but not child.cbox.height)
      // center the children of the LineContainerGlyph on the line
      final Rectangle2D.Double cbox = child.getCoordBox();
      final double ycenter = this.getCoordBox().y + this.getCoordBox().height/2;
      // use moveAbsolute or moveRelative to make sure children of "child" glyph also get moved
      child.moveRelative(0, ycenter - cbox.height/2 - cbox.y);
      return cbox.height;
    } else {
      return this.getCoordBox().height;
    }
  }

  protected void adjustChildren() {
    double max_height = 0.0;
    if (isMoveChildren()) {
      List<GlyphI> childlist = this.getChildren();
      if (childlist != null) {
        for (GlyphI child :childlist) {
          double child_height = adjustChild(child);
          max_height = Math.max(max_height, child_height);
        }
      }
    }
    if (max_height > this.getCoordBox().height) {
      this.getCoordBox().height = max_height;
      adjustChildren(); // have to adjust children again after a height change.
    }
  }

  @Override
  public void pack(ViewI view) {
    if ( isMoveChildren()) {
      this.adjustChildren();
      // Maybe now need to adjust size of total glyph to take into account
      // any expansion of the children ?
    } else {
      super.pack(view);
    }
  }


	public void setIsStackedGlyph(boolean b){
		isStackedGlyph = b;
	}

}
