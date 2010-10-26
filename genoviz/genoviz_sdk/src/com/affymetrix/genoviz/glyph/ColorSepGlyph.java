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

package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;


/**
 * This glyph extends the standard glyph with a new draw traversal
 * that draws all the children of a certain color, one color at a time.
 *
 * @author Cyrus Harmon 6/25/97
 *
 * <p> Questions:<ol>
 *
 * <li> Is this worth doing?
 *    Look at the Quality glyph and you will notice a significant
 *    speed increase thanks to this new drawTraversal.
 *
 * <li> Does this affect drawing order?
 *    Yes. Does this matter? I'm not sure.
 *
 * <li> Is there a better way to do this?
 *    Maybe. Let me know if you find one
 *
 */
public class ColorSepGlyph extends Glyph  {

	protected Color colors[] = {};

	public Color[] getColorArray() {
		return colors;
	}

	public void setColorArray(Color[] ca) {
		colors = ca;
	}

	@Override
	protected void drawChildren(ViewI view) {
		if (children == null)  { return; }
		if (colors.length > 0) {
			for (int colorIndex = 0; colorIndex < colors.length; colorIndex++) {
				for (GlyphI child : children) {
					if (child.getColor() == colors[colorIndex]) {
						child.drawTraversal(view);
					}
				}
			}
		}
		else {
			super.drawChildren(view);
		}
	}

}
