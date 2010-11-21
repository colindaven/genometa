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
import com.affymetrix.genoviz.glyph.SolidGlyph;
import java.awt.geom.Rectangle2D;
import java.util.*;

public abstract class FlyweightPointGlyph extends SolidGlyph  {
  protected GlyphI template_glyph;
  protected int[] xcoords;
  protected int flylength = 1;
  
  public FlyweightPointGlyph(GlyphI gl, int[] xarray, int flength) {
    template_glyph = gl;
    xcoords = xarray;
    flylength = flength;
    Rectangle2D.Double tbox = template_glyph.getCoordBox();
    int xmin = xarray[0];
    int xmax = xarray[xarray.length-1];
    int xlength = xmax - xmin + flength;  // extend to include flength
    this.setCoords((double)xmin, tbox.y, (double)xlength, tbox.height);
  }

	@Override
  public void drawTraversal(ViewI view)  {
    super.drawTraversal(view);
    if (xcoords != null) {
      drawFlyweights(view);
    }
  }

  public void drawFlyweights(ViewI view) {
    Rectangle2D.Double cbox = this.getCoordBox();
    template_glyph.setCoords(cbox.x, cbox.y, flylength, cbox.height);
    if (xcoords != null) {
      Rectangle2D.Double tbox = template_glyph.getCoordBox();
      int flycount = xcoords.length;
      for (int i=0; i<flycount; i++) {
	tbox.x = xcoords[i];
	//	tbox.width = flylength;
	if (template_glyph.withinView(view)) {
	  template_glyph.draw(view);
	}
      }
    }
  }

  /**
   *  Reifying flyweight glyphs as needed in pickTraversal.
   */
	@Override
  public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList,
                            ViewI view)  {
    super.pickTraversal(pickRect, pickList, view);
    if (isVisible && intersects(pickRect, view))  {
      if (xcoords != null) {
	Rectangle2D.Double tbox = template_glyph.getCoordBox();
	int flycount = xcoords.length;
	for (int i=0; i<flycount; i++) {
	  tbox.x = xcoords[i];
	  if (template_glyph.hit(pickRect, view)) {
	    try {
	      GlyphI reified_glyph = (GlyphI)template_glyph.getClass().newInstance();
	      reified_glyph.setColor(template_glyph.getColor());
	      reified_glyph.setCoords(tbox.x, tbox.width, tbox.y, tbox.height);
	      pickList.add(reified_glyph);
	    }
	    catch (Exception ex) {
	      ex.printStackTrace();
	    }
	  }
	}
      }
    }
  }


}
