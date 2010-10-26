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

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import java.awt.*;

/**
 *  Extends FillRectGlyph for showing threshold, so that it keeps a label showing threshold level 
 *  on far left.
 */
public final class ThreshGlyph extends FillRectGlyph {
  private String label;

  public void setLabel(String s) {
    this.label = s;
  }

  public String getLabel() { return label; }

	@Override
  public void draw(ViewI view) {
    super.draw(view);
    view.transformToPixels(coordbox, pixelbox);
    Graphics g = view.getGraphics();
    g.setColor(this.getBackgroundColor());
    int xpos = Math.max(pixelbox.x, 0);
    g.drawString(label, xpos+20, pixelbox.y-2);
  }
}
