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

import java.awt.Font;
import com.affymetrix.genoviz.bioviews.GlyphI;

public interface ResiduesGlyphI extends GlyphI {

	// sets the residues for this glyph (unrelated to parent)
	public void setResidues(String residues);
	public String getResidues();

	public void setResidueFont(Font fnt);
	public Font getResidueFont();

	// helper methods so parent can figure out what residue string to pass
	//   in setResidues()
	public void setParentSeqStart(int beg);
	public void setParentSeqEnd(int end);
	public int getParentSeqStart();
	public int getParentSeqEnd();
}
