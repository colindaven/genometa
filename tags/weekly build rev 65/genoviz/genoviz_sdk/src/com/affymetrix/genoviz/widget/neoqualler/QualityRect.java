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

package com.affymetrix.genoviz.widget.neoqualler;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class QualityRect extends Glyph
{
	@Override
	public void draw(ViewI view) {
		view.transformToPixels(coordbox, pixelbox);
		// shrinks slightly to differentiate from possible neighbors

		Graphics g = view.getGraphics();

		if (pixelbox.height == 0) { pixelbox.height = 1; }

		if (pixelbox.width >=1 ) {
			if (pixelbox.width > 2) {
				pixelbox.x += 1;
				pixelbox.width -=2;
			}
			else if (pixelbox.width == 2) {
				pixelbox.x++;
				pixelbox.width--;
			}
			g.setColor(getBackgroundColor());
			g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
		}
		else {
			g.setColor(getBackgroundColor());
			g.fillRect(pixelbox.x, pixelbox.y, 2, 2);
		}
		super.draw(view);
	}

	@Override
	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		calcPixels(view);
		return  pixel_hitbox.intersects(pixelbox);
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return coord_hitbox.intersects(coordbox);
	}

}
