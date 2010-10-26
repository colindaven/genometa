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
 *  a simple ResiduePainter
 *  just ignores consensus,
 *  and colors all the residues white except that it colors
 *  AG and GT blue/green (potential splice sites?)
 *  (not that I'm recommending this as a good way of displaying splice
 *   sites -- I just needed an example that didn't require a more
 *   sophisticated datamodel external to the widgets)
 */
public class ResiduePainterExample implements ResiduePainter {
	Color bg_color = Color.white;
	Color ag_color = Color.blue;
	Color gt_color = Color.green;
	Color agt_color = Color.cyan;

	public void calculateColors(Color[] color_array,
			String residue_string, String consensus_string,
			int residue_start, int consensus_start) {
		int cursor = residue_start;
		int position;
		for (int i=0; i<color_array.length; i++) {
			color_array[i] = bg_color;
		}
		while (((position = residue_string.indexOf("AG", cursor)) != -1)  &&
				(color_array.length > position+1)) {
			color_array[position] = ag_color;
			color_array[position + 1] = ag_color;
			cursor = position+1;
				}
		cursor = residue_start;
		while ((position = residue_string.indexOf("GT", cursor)) != -1  &&
				(color_array.length > position+1)) {
			color_array[position] = gt_color;
			color_array[position + 1] = gt_color;
			cursor = position+1;
				}
		cursor = residue_start;
		while ((position = residue_string.indexOf("AGT", cursor)) != -1  &&
				(color_array.length > position+2)) {
			color_array[position + 1] = agt_color;
			cursor = position+1;
				}
	}


	public void calculateColors(Object dataModel, Color[] color_array,
			String residue_string, String consensus_string,
			int residue_start, int consensus_start) {

		this.calculateColors(color_array, residue_string, consensus_string,
				residue_start, consensus_start);
	}

}
