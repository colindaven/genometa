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

import java.awt.Color;

import com.affymetrix.genoviz.util.DNAUtils;

/**
 * Used in NeoAssembler to display spans of aligned DNA with no gaps.
 *
 * NeoAssembler uses an AlignmentGlyph to display aligned nucleotides with gaps, and
 * AlignmentGlyph in turn uses AlignedDNAGlyphs, one for each continuous span
 * of aligned nucleotides (no gaps).
 */
public class AlignedDNAGlyph extends AlignedResiduesGlyph  {

	static Color identity_color_matrix[][] = new Color[DNAUtils.LETTERS][DNAUtils.LETTERS];

	static {
		DNAUtils.fillColorMatrix (identity_color_matrix,
				Color.black, Color.blue, Color.pink, null);
	}

	public AlignedDNAGlyph() {

		bg_color_strategy = ALIGNMENT_BASED;
		fg_color = Color.lightGray;

		setBackgroundColorMatrix(identity_color_matrix);

	}

	/** Redo colors with a default map. */
	public void redoColors()  {
		redoColors(DNAUtils.getNACharToIdMap());
	}

}
