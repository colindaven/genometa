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
import com.affymetrix.genoviz.datamodel.Position;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * a caret or cursor for editing a NeoSeq.
 */
public class Caret extends WrapGlyph {

	private Position position;

	private Point colorPixelPoint1 = new Point(0,0);
	private Point colorPixelPoint2 = new Point(0,0);

	/**
	 * x = offset of start position of annot into row of bases
	 * y = first base of row that start of annot is in
	 */
	private final Point2D.Double colorCoordPoint1 = new Point2D.Double(0,0);

	/**
	 * x = offset of end position of annot into row of bases
	 * y = last base of row that start of annot is in
	 */
	private final Point2D.Double colorCoordPoint2 = new Point2D.Double(0,0);

	private Rectangle2D.Double visible_box;

	private static final int y_offset_fudge = 3;

	/** Caret is drawn outlining the current character. */
	public static final int OUTLINE = 0;
	/** Caret is drawn as the current character with forground and background reversed. */
	public static final int SOLID = 1;
	/** Caret is drawn underlining the current character. */
	public static final int UNDERLINE = 2;
	public static final int SELECTED = 10;

	private int fill = SOLID;

	private static final boolean debug = false;

	public Caret() {
		super();
		this.setSelectable( false );
	}

	/**
	 * The fill property determines how this caret is drawn.
	 *
	 * @param f one of {@link #SOLID}, {@link #UNDERLINE}, or {@link #OUTLINE}.
	 */
	public void setFill( int f ) {
		switch ( f ) {
			case OUTLINE:
			case SOLID:
			case UNDERLINE:
				this.fill = f;
				break;
			default:
				throw new IllegalArgumentException
					("Fill must be either SOLID, UNDERLINE, or OUTLINE.");
		}
	}

	public int getFill() {
		return this.fill;
	}

	/**
	 * The position of the caret in "residue space".
	 * The caret should point to the leading edge of the residue
	 * whose index is the parameter.
	 *
	 * @param p postion of the residue being pointed before.
	 */
	public final void setPosition( Position p ) {
		this.position = p;
	}

	public final Position getPosition() {
		return this.position;
	}

	public void draw(ViewI view) {

		if ( null == this.position ) {
			System.err.println("Cannot draw a Caret when the position is null.");
			return;
		}

		/* for the moment use the old notation */
		int annot_start = this.position.getOffset();
		int annot_end = annot_start;

		Graphics g = view.getGraphics();

		visible_box = ((View)view).calcCoordBox();

		g.setColor( getBackgroundColor() );

		int line_index;
		int first_residue;
		int last_residue;

		int first_visible_residue = (int)visible_box.y;

		if ( residues_per_line < 1 ) {
			System.err.println( "" + residues_per_line + " residues per line?" );
			return;
		}
		int last_visible_residue =
			useConstrain(residues_per_line, visible_box.y, visible_box.height);

		for (line_index = first_visible_residue;
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

					switch ( fill ) {
						case OUTLINE:
							g.drawRect(colorPixelPoint1.x,
									colorPixelPoint1.y + y_offset_fudge,
									colorPixelPoint2.x - colorPixelPoint1.x,
									colorPixelPoint2.y - colorPixelPoint1.y);
							break;
						case SOLID:
							g.setXORMode( Color.white );
							g.fillRect(colorPixelPoint1.x,
									colorPixelPoint1.y + y_offset_fudge,
									colorPixelPoint2.x - colorPixelPoint1.x,
									colorPixelPoint2.y - colorPixelPoint1.y);
							g.setPaintMode();
							break;
						case UNDERLINE:
							g.fillRect(colorPixelPoint1.x,
									colorPixelPoint2.y + y_offset_fudge,
									colorPixelPoint2.x - colorPixelPoint1.x,
									2);
							break;
					}

				}

					}
		}
	}

	private static int useConstrain(int residues_per_line, double y, double height) {
		return (int) (y + height - (height % residues_per_line) - 1);
	}
}
