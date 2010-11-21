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

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * RootGlyph should not be used directly.
 * It is for internal use by the NeoWidgets.
 *
 * RootGlyph is used internally by NeoWidgets
 * as the root glyph of the widget's (and scene's) glyph hierarchy.
 */
public class RootGlyph extends StretchContainerGlyph {

	public static final int X = 0;
	public static final int Y = 1;
	public static final int EXPAND = 3;
	public static final int NO_EXPAND = 4;

	protected int expansion_behavior[] = { NO_EXPAND, NO_EXPAND };
	//  protected Rectangle2D.Double testbox = new Rectangle2D();
	protected boolean show_outline = false;

	public void setExpansionBehavior(int axisid, int behavior) {
		expansion_behavior[axisid] = behavior;
	}

	public int getExpansionBehavior(int axisid) {
		return expansion_behavior[axisid];
	}

	public void propagateStretch(GlyphI child) {
		if (expansion_behavior[X] == EXPAND && expansion_behavior[Y] == EXPAND) {
			super.propagateStretch(child);
			return;
		}
		Rectangle2D.Double childbox = child.getCoordBox();
		if (expansion_behavior[X] == EXPAND) {
			double xbeg = Math.min(childbox.x, coordbox.x);
			double xend = Math.max(childbox.x + childbox.width,
					coordbox.x + coordbox.width);
			coordbox.x = xbeg;
			coordbox.width = xend - xbeg;
		}
		else if (expansion_behavior[Y] == EXPAND) {
			double ybeg = Math.min(childbox.y, coordbox.y);
			double yend = Math.max(childbox.y + childbox.height,
					coordbox.y + coordbox.height);
			coordbox.y = ybeg;
			coordbox.height = yend - ybeg;
		}
		else {
			// System.err.println("in rootglyph, shouldn't reach this branch!");
		}

	}

	public void drawTraversal(ViewI view) {
		super.drawTraversal(view);
		if (show_outline) {
			view.transformToPixels(coordbox, pixelbox);
			Graphics g= view.getGraphics();
			g.setColor(Color.green);
			g.drawRect(pixelbox.x+2, pixelbox.y+2,
					pixelbox.width-4, pixelbox.height-4);
		}
	}

	/**
	 * calculates the pixel box
	 * and delegates the rest to the super class.
	 *
	 * @param view into the scene of which this is the root glyph
	 */
	public void draw(ViewI view) {

		/* The reason this is done is so that pickTraversalByPixel will work.
		 * Otherwise, the root glyph's pixel box is always empty.
		 * Hence, since all glyphs are children of the root glyph,
		 * no glyphs can get hit. -- Eric 1998-12-12
		 */
		view.transformToPixels(coordbox, pixelbox);
		super.draw(view);
	}


	public void setShowOutline(boolean show_outline) {
		this.show_outline = show_outline;
	}

	public boolean getShowOutline() {
		return show_outline;
	}


}
