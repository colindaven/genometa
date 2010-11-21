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

/**
 *  A glyph that expands when children are added, so that its bounding box is
 *  inclusive of the preferred bounding boxes of its children.
 */
public class StretchContainerGlyph extends Glyph {

	@Override
	public void addChild(GlyphI child)  {
		super.addChild(child);
		propagateStretch(child);
	}

	public void propagateStretch(GlyphI child) {
		coordbox.add(child.getCoordBox());

		if (this.parent instanceof StretchContainerGlyph) {
			((StretchContainerGlyph)this.parent).propagateStretch(this);
		}
	}

	@Override
	public void setCoords(double x, double y, double width, double height)  {
		super.setCoords(x, y, width, height);
	}


}
