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

import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import com.affymetrix.genoviz.datamodel.SequenceI;
import java.awt.geom.Rectangle2D;

public class WrapNumbers extends WrapGlyph {

	protected int seqLength;
	protected int firstOrd = 0;
	protected boolean revNums = false;

	protected Rectangle2D.Double visible_box;

	/** specifies the gap between the numbers and the residues to the right. */
	private static final int RIGHT_MARGIN = 6;

	@Override
	public void setSequence (SequenceI seq) {
		seqLength = seq.getLength();
	}

	/** Set whether or not numbering should be displayed descending */
	public void setRevNumbering (boolean rn) {
		revNums = rn;
	}

	/** Get whether or not the numbering will be displayed descending */
	public boolean getRevNumbering () {
		return revNums;
	}

	/** Set from where the numbering begins */
	public void setFirstOrdinal (int firstOrd) {
		this.firstOrd = firstOrd;
	}

	/** Get from where the numbering begins */
	public int getFirstOrdinal () {
		return firstOrd;
	}

	public void draw (ViewI view) {
		Graphics g = view.getGraphics();
		drawNumbers(view, g);
	}

	public void drawNumbers (ViewI view, Graphics g )  {

		int seqEnd = firstOrd + seqLength;

		visible_box = ((View)view).calcCoordBox();

		Rectangle pixelBox = new Rectangle(0, 0);
		pixelBox = ((View)view).transformToPixels(visible_box, pixelBox);

		// Figure out where to start counting,

		int first_residue_line = (int)visible_box.y;

		if (!revNums)
			first_residue_line += firstOrd;

		if (residues_per_line < 1) {
			return;
		}
		int last_residue_line =
			useConstrain(residues_per_line, visible_box.y, visible_box.height);

		if (last_residue_line > seqEnd) {
			last_residue_line = seqEnd;
		}

		int residue_height = getResidueHeight();
		int offsets[] = getResidueOffsets();

		int line_yposition = 0;

		int ycounter = line_yposition + offsets[NUCLEOTIDES];

		g.setFont(seqfont);
		g.setColor(getBackgroundColor());

		FontMetrics fm = g.getFontMetrics(seqfont);

		int residue_index;
		for (residue_index = first_residue_line;
				residue_index <= last_residue_line;
				residue_index += residues_per_line)  {
			if (residue_index > seqEnd) {
				break;
			}
			String ordinal;

			if (revNums)
				ordinal = Integer.toString(seqEnd - residue_index -1);
			else
				ordinal = Integer.toString(residue_index);

			int x = pixelBox.width - RIGHT_MARGIN - fm.stringWidth(ordinal);
			g.drawString(ordinal, x, ycounter);
			ycounter += residue_height;
				}

	}

	private static int useConstrain(int residues_per_line, double y, double height) {
		return (int) (y + height - (height % residues_per_line) - 1);
	}

}
