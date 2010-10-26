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

import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class WrapColors extends AnnotationGlyph {

	protected Point colorPixelPoint1 = new Point(0,0);
	protected Point colorPixelPoint2 = new Point(0,0);

	/**
	 * colorCoordPoint1.x = offset of start position of annot into row of bases
	 * colorCoordPoint1.y = first base of row that start of annot is in
	 */
	protected Point2D.Double colorCoordPoint1 = new Point2D.Double(0,0);

	/**
	 * colorCoordPoint2.x = offset of end position of annot into row of bases
	 * colorCoordPoint2.y = last base of row that start of annot is in
	 */
	protected Point2D.Double colorCoordPoint2 = new Point2D.Double(0,0);

	protected Rectangle2D.Double visible_box;

	protected int y_offset_fudge = 3;

	public static final int OUTLINE = 0;
	public static final int SOLID = 1;
	public static final int SELECTED = 2;

	protected int fill = SOLID;

	private static final boolean debug = false;

	public void setFill(int fill) {
		this.fill = fill;
	}

	public int getFill() {
		return fill;
	}

	public void setColorSpan(int start, int end) {
		annot_start = start;
		annot_end = end;
	}

	/** Overriden to prevent
	 * an outline box from being drawn around the entire glyph.
	 * Note this is only the case with Glyph.drawSelectedOutline,
	 * not drawSelectedFill.
	 */
	@Override
	protected void drawSelectedOutline(ViewI view) {
		draw(view);
	}

	public void draw(ViewI view) {

		Graphics g = view.getGraphics();

		visible_box = ((View)view).calcCoordBox();

		g.setColor(getBackgroundColor());

		int line_index;
		int first_residue;
		int last_residue;

		int first_visible_residue = (int)visible_box.y;

		if (residues_per_line < 1) {
			return; // Avoid infinite loop below.
		}
		int last_visible_residue = useConstrain(residues_per_line, visible_box.y, visible_box.height);

		for ( line_index = first_visible_residue;
				line_index < last_visible_residue;
				line_index += residues_per_line )
		{
			first_residue = line_index;
			last_residue = first_residue + residues_per_line - 1;

			if (annot_start <= first_residue) {
				colorCoordPoint1.x = 0;
			}
			else {
				colorCoordPoint1.x = annot_start - first_residue;
			}

			if (annot_end >= last_residue) {
				colorCoordPoint2.x = residues_per_line;
			}
			else {
				colorCoordPoint2.x = residues_per_line - (last_residue - annot_end);
			}

			if ((colorCoordPoint1.x >= 0) &&
					(colorCoordPoint2.x - colorCoordPoint1.x > 0)) {

				colorCoordPoint1.y = line_index;
				colorCoordPoint2.y = colorCoordPoint1.y + residues_per_line;

				colorPixelPoint1 =
					view.transformToPixels(colorCoordPoint1, colorPixelPoint1);

				colorPixelPoint2 =
					view.transformToPixels(colorCoordPoint2, colorPixelPoint2);

				if (debug) {
					System.out.println(" " + colorCoordPoint1.y +
							", " + colorCoordPoint2.y + " : " +
							colorPixelPoint1.y + ", " +
							colorPixelPoint2.y);
				}

				if (selected) {
					if (view.getScene().getSelectionAppearance() == Scene.SELECT_OUTLINE ||
							fill == OUTLINE) {
						// Forcing outline if fill == OUTLINE,
						//  regardless of selection style.
						g.drawRect(colorPixelPoint1.x,
								colorPixelPoint1.y + y_offset_fudge,
								colorPixelPoint2.x - colorPixelPoint1.x,
								colorPixelPoint2.y - colorPixelPoint1.y);
							}
					else if (view.getScene().getSelectionAppearance() == Scene.SELECT_FILL) {
						g.fillRect(colorPixelPoint1.x,
								colorPixelPoint1.y + y_offset_fudge,
								colorPixelPoint2.x - colorPixelPoint1.x,
								colorPixelPoint2.y - colorPixelPoint1.y);
					}
				}
				else {

					if (fill == OUTLINE) {
						g.drawRect(colorPixelPoint1.x,
								colorPixelPoint1.y + y_offset_fudge,
								colorPixelPoint2.x - colorPixelPoint1.x,
								colorPixelPoint2.y - colorPixelPoint1.y);
					}
					if (fill == SOLID) {
						g.fillRect(colorPixelPoint1.x,
								colorPixelPoint1.y + y_offset_fudge,
								colorPixelPoint2.x - colorPixelPoint1.x,
								colorPixelPoint2.y - colorPixelPoint1.y);
					}

				}

					}
		}
	}

	private static int useConstrain(int residues_per_line, double y, double height) {
		return (int) (y + height - (height % residues_per_line) - 1);
	}

}
