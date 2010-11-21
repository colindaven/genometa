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

package genoviz.demo;

import java.awt.Color;
import com.affymetrix.genoviz.bioviews.*;

/**
 * another simple ResiduePainter.
 * It ignores consensus and any data model.
 * It just color codes ATG (and AUG).
 * It also doesn't bother with color-coding anything else.
 * So any other residues should retain their previous coloring.
 */
public class StartCodonPainter implements ResiduePainter {

	private Color startColor;

	/**
	 * @param theColor for the start codons.
	 */
	public StartCodonPainter( Color theColor ) {
		this.startColor = theColor;
	}

	/**
	 * constructs a painter that paints start codons red.
	 */
	public StartCodonPainter() {
		this( Color.red );
	}

	/**
	 * sets the start codons' color.
	 * @param consensus_string ignored.
	 * @param consensus_start ignored.
	 */
	public void calculateColors(Color[] color_array,
			String residue_string,
			String consensus_string,
			int residue_start,
			int consensus_start) {
		int i;
		String residues = residue_string.toUpperCase().replace( 'U', 'T' );
		while ( -1 < ( i = residues.indexOf( "ATG", residue_start ) ) ) {
			if ( i < color_array.length )
				color_array[i++] = this.startColor;
			if ( i < color_array.length )
				color_array[i++] = this.startColor;
			if ( i < color_array.length )
				color_array[i++] = this.startColor;
			residue_start = i;
		}
	}

	/**
	 * sets the start codons' color.
	 * @param dataModel ignored.
	 * @param consensus_string ignored.
	 * @param consensus_start ignored.
	 */
	public void calculateColors(Object dataModel,
			Color[] color_array,
			String residue_string,
			String consensus_string,
			int residue_start,
			int consensus_start) {

		//TODO: handle the dataModel properly. Not sure what it's for.
		this.calculateColors(color_array, residue_string, consensus_string,
				residue_start, consensus_start);
	}

}
