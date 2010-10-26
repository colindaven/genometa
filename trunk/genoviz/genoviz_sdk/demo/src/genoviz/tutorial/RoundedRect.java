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

package genoviz.tutorial;

import java.awt.Graphics;
import java.awt.Rectangle;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;

public class RoundedRect extends SolidGlyph {

	@Override
	public void draw(ViewI view) {
	  // calcPixels() uses view argument to calculate pixel box 
	  ///   of glyph based on coord box of glyph and view transform
	        this.calcPixels(view);  
		Rectangle bbox = this.getPixelBox();
		Graphics g = view.getGraphics();
		g.setColor( this.getBackgroundColor() );
		g.fillRoundRect(bbox.x, bbox.y, bbox.width, bbox.height,
				bbox.height, bbox.height);
		super.draw(view);
	}

}
