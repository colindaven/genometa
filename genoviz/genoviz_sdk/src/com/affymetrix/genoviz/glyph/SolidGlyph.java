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

import java.awt.Rectangle;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.geom.Rectangle2D;

/**
 * A convenient base class for glyphs that are "solid", meaning any event within
 * the coordinate bounds of the glyph is considered to hit the glyph.
 *
 * <p> Mainly a convenience so other Glyph's don't have to implement hit
 * methods if they are willing to stick with simple hits.
 */
public class SolidGlyph extends Glyph {

	boolean hitable = true;

	/**
	 * @return whether or not this glyph is hitable
	 * @see #setHitable
	 */
	public boolean isHitable() {
		return hitable;
	}

	/**
	 * Sets if the glyph is hitable.
	 * Most glyphs will probably be hitable,
	 * and the default value is true.
	 * Making a glyph not hitable keeps it from being selectable, and from being
	 * in the NeoMouseEvent.getItems() vector.
	 */
	public void setHitable(boolean hitable) {
		this.hitable = hitable;
	}

	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		calcPixels(view);
		return hitable && isVisible() && pixel_hitbox.intersects(getPixelBox());
	}

	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return hitable && isVisible() && coord_hitbox.intersects(getPositiveCoordBox());
	}

}
