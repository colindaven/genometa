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

import java.awt.Color;

/**
 * Implementers set an array of colors
 * appropriate for painting a string of residues.
 */
public interface ResiduePainter {

	/**
	 * sets colors appropriate for a particular data model.
	 *
	 * @param dataModel to refer to when deciding which colors to use.
	 * @param color_array where the colors are set.
	 * @param residue_string the string of residues that might want coloring.
	 * @param consensus_string another string of residues for possible comparison.
	 * @param residue_start position of the first residue to be colored.
	 * @param consensus_start position in the consensus string corresponding to residue_start in the residue_string.
	 */
	public abstract void calculateColors
		( Object dataModel,
		  Color[] color_array,
		  String residue_string,
		  String consensus_string,
		  int residue_start,
		  int consensus_start );

	/**
	 * sets colors independent of a particular data model.
	 *
	 * @param color_array where the colors are set.
	 * @param residue_string the string of residues that might want coloring.
	 * @param consensus_string another string of residues for possible comparison.
	 * @param residue_start position of the first residue to be colored.
	 * @param consensus_start position in the consensus string corresponding to residue_start in the residue_string.
	 */
	public abstract void calculateColors
		( Color[] color_array,
		  String residue_string,
		  String consensus_string,
		  int residue_start,
		  int consensus_start );

}
