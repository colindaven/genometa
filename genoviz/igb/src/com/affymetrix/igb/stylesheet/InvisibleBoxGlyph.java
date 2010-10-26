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

package com.affymetrix.igb.stylesheet;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import com.affymetrix.igb.glyph.EfficientOutlineContGlyph;
import java.awt.Graphics;
import java.awt.Rectangle;

public final class InvisibleBoxGlyph extends EfficientOutlineContGlyph {

  public void draw(ViewI view) {
    // When the pixelbox is normal size, draw nothing.
    // The children will be drawn, though, due to drawTraversal()
  }

  public void fillDraw(ViewI view) {
    // This is what gets drawn when the pixelbox of this glyph is too small.
    // Draw all the children as tiny boxes.

    if (children != null)  {
      GlyphI child;
      int numChildren = getChildCount();
      for ( int i = 0; i < numChildren; i++ ) {
	child = children.get( i );
	// TransientGlyphs are usually NOT drawn in standard drawTraversal
	if (!(child instanceof TransientGlyph) || drawTransients()) {
          Rectangle pixelbox = view.getScratchPixBox();
          view.transformToPixels(child.getCoordBox(), pixelbox);
          Graphics g = view.getGraphics();
          g.setColor(child.getColor());

          if (pixelbox.width < 1) { pixelbox.width = 1; }
          if (pixelbox.height < 1) { pixelbox.height = 1; }
          // draw the box
          g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);          
	}
      }
    }
  }
}
