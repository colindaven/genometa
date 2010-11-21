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

/**
 * Different types of glyphs for sequence annotation can extend this.
 * Such glyphs will inherit start and end read-only properties.
 */
public abstract class AnnotationGlyph extends WrapGlyph {

	protected int annot_start = 0;
	protected int annot_end = 0;

	public int getStart() {
		return annot_start;
	}

	public int getEnd() {
		return annot_end;
	}

	public void setStart(int start) {
		annot_start = start;
	}

	public void setEnd(int end) {
		annot_end = end;
	}


	@Override
	public String toString() {
		return "Annotation " + super.toString();
	}


}
