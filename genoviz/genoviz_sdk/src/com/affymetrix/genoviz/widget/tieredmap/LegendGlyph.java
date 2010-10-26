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

package com.affymetrix.genoviz.widget.tieredmap;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.OutlineRectGlyph;
import java.awt.*;


public class LegendGlyph extends OutlineRectGlyph {

	String primary_label;

	public void setPrimaryLabel(String str) {
		this.primary_label = str;
	}

	/**
	  Background color is text color.
	  Foreground color is outline color.
	  */

	@Override
	public void draw(ViewI view) {
		Graphics g = view.getGraphics();
		g.setColor(getBackgroundColor());
		int padding = g.getFont().getSize() + 2;
		view.transformToPixels(coordbox, pixelbox);
		g.drawString(primary_label, 5, pixelbox.y + padding);
		super.draw(view);
	}

}
