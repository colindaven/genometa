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

package com.affymetrix.genoviz.widget.neoseq;

import com.affymetrix.genoviz.bioviews.ViewI;


public class WrapFontColors extends AnnotationGlyph {
	// max types to allocate space for
	// currently forward/reverse strand plus six possible translations = 8
	private static final int maxDrawTypes = 8;

	boolean drawTypes[];

	protected WrapSequence wrap_seq;

	public void setColorSpan(int start, int end) {
		annot_start = start;
		annot_end = end;
	}

	/**
	 *  Draw relies on WrapSequence.drawResidues() to do the work.
	 *  Tried doing the calculations within this class similar to WrapColors,
	 *  but font y-positions ended up occasionally being off by a pixel.
	 *  Relying on WrapSequence (parent of parent) to do the work may be a
	 *  little twisted, but it has the advantage that any changes to how
	 *  string positions are calculated for drawing are localized to the same
	 *  piece of code.
	 */
	public void draw(ViewI view) {
		// if no drawTypes are set, then draw as _all_ types that space
		//    has been allocated for in the WrapSequence
		// doing it this way mainly to save memory in default case where all
		//    types with allocated space are supposed to be drawn
		if (drawTypes == null) {
			wrap_seq.drawResidues(annot_start, annot_end, view,
					wrap_seq.getFont(), getBackgroundColor());
		}
		else {
			wrap_seq.drawResidues(annot_start, annot_end, view,
					wrap_seq.getFont(), getBackgroundColor(), drawTypes);
		}
	}

	public void setWrapSequence(WrapSequence wrap_seq) {
		this.wrap_seq = wrap_seq;
	}

	public WrapSequence getWrapSequence() {
		return wrap_seq;
	}

	public boolean getDrawnAs(int type) {
		return (drawTypes == null ? true : drawTypes[type]);
	}

	public void setDrawnAs(int type, boolean draw) {
		if (drawTypes == null) {
			drawTypes = new boolean[maxDrawTypes];
			for (int i=0; i<maxDrawTypes; i++) {
				drawTypes[i] = false;
			}
		}
		drawTypes[type] = draw;
	}

}
