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
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;

/**
 * A glyph that may transit a view.
 * It can be drawn "on top" of everything else in a glyph hierachy.
 * This can be useful, for example, to show a shadow of one widget on another,
 * or to have a glyph that can be dragged around a view with the mouse.
 *
 * <p> A map can be optimized
 * so that the view (without transients) can be quickly drawn as a background image.
 * Then just the transients need be drawn individually.
 * Not the rest of the glyphs.
 * This will make these transients easy to move around quickly.
 *
 * @see com.affymetrix.genoviz.widget.NeoMap#isTransientOptimized
 */
public class TransientGlyph extends Glyph {
	/*
	 * Calling this transient because the idea is to be able to move it quickly
	 * without having to worry about doing a full or damage-optimized redraw of
	 * the whole view/scene.
	 * (Note that if damage-optimized redrawing gets good enough, this may not
	 *   be needed...)
	 *
	 * A couple options here:
	 *  1. Do XOR drawing, keeping track of whether the previous draw of this
	 *      glyph needs to be erased
	 *  2. Double buffer everything else, and draw transients on screen graphic
	 *      after buffered image has been moved over
	 *
	 *  Problem with (1) is that keeping track of when previous draw needs to be
	 *     erased could become a serious pain, especially when trying to combine
	 *     with View drawing optimizations
	 *  Problem with (2) is that anything underneath the transient will disapear.
	 *     May also get more severe flicker...
	 *
	 *  Therefore trying a compromise:
	 *  Double buffer everything else, and draw transients on screen graphic,
	 *      but in XOR mode (with option to turn XOR mode off)
	 *
	 *  WARNING!  Putting check down in Glyph.java -- Transient glyphs that
	 *     are children of non-transient glyphs WILL NOT be drawn in the
	 *     standard drawTraversal.  The View will draw them "directly" to screen
	 *     after everything else (including scrolling optimizations, etc.).
	 */
	// or should it extend StretchContainerGlyph glyph???

	private boolean useXOR = true;

	public void drawTraversal(ViewI view) {
		Graphics g = view.getGraphics();
		if (useXOR) {
			g.setXORMode(view.getComponent().getBackground());
		}
		super.drawTraversal(view);
		if (useXOR) {
			g.setPaintMode();
		}
	}

	// trying to make sure transient draws will recurse down to children of
	//   transients -- may not be needed...
	// probably better to require that all children of a TransientGlyph NOT be
	//   TransientGlyphs themselves, this requirement will also help with
	//   figuring out XOR mode...
	public boolean drawTransients() {
		return true;
	}

	/**
	 * indicates whether or not children should be drawn in XOR mode.
	 * The default (if this method is never called) is true.
	 */
	public final void setUseXOR(boolean useXOR) {
		this.useXOR = useXOR;
	}

	public final boolean getUseXOR() {
		return useXOR;
	}

}
