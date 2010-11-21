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

package com.affymetrix.genoviz.bioviews;

import java.awt.Rectangle;

/**
 * Interface for intelligent layout of glyphs in a scene,
 * given a particular view.
 */
public interface PackerI {

	/**
	 * packs a given glyph within its parent.
	 * This is typically called for each child of a glyph
	 * in the other pack method of this interface.
	 *
	 * @param parent_glyph
	 * @param child_glyph
	 * @param view in which to pack glyphs.
	 */
	public Rectangle pack(GlyphI parent_glyph,
			GlyphI child_glyph, ViewI view);

	/**
	 * packs all the children of a glyph in a view.
	 *
	 * @param parent_glyph the glyph to pack
	 * @param view
	 */
	public Rectangle pack(GlyphI parent_glyph, ViewI view);

}
