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

import com.affymetrix.genoviz.util.ProteinUtils;


import java.awt.Color;

/**
 *  Used in NeoAssembler to display spans of aligned protein with no gaps.
 *
 *  <p> NeoAssembler uses an AlignmentGlyph to display aligned amino acids with gaps, and
 *  AlignmentGlyph in turn uses AlignedProteinGlyphs, one for each continuous span
 *  of aligned amino acids (no gaps).
 */
public class AlignedProteinGlyph extends AlignedResiduesGlyph  {

	static Color[][] identity_color_matrix =
		new Color[ProteinUtils.LETTERS][ProteinUtils.LETTERS];

	static {
		for (int i=0; i<identity_color_matrix.length; i++) {
			for (int j=0;j<identity_color_matrix.length; j++) {
				if (i == j) {
					identity_color_matrix[i][j] = Color.black;
				}
				else {
					identity_color_matrix[i][j] = Color.blue;
				}
			}
		}
	}

	static Color b[] = {
		new Color (0x80, 0x40, 0xff),
		new Color (0x40, 0xff, 0x80),
		new Color (0xff, 0x60, 0x60),
		new Color (0x20, 0xff, 0xff),
		new Color (0xff, 0xff, 0x20)
	};

	static Color col_array[] = {
		b[0], b[0], b[0], b[0],
		b[1], b[1], b[1], b[1],
		b[2], b[2], b[2], b[2],
		b[3], b[3], b[3], b[3],
		b[4], b[4], b[4], b[4]
	};

	public AlignedProteinGlyph() {

		bg_color_strategy = ALIGNMENT_BASED;
		fg_color = Color.lightGray;

		setBackgroundColorMatrix(identity_color_matrix);
		setBackgroundColorArray(col_array);

	}

	/** Redo colors with a default map. */
	public void redoColors()  {
		redoColors(ProteinUtils.getAACharToIdMap());
	}

}
