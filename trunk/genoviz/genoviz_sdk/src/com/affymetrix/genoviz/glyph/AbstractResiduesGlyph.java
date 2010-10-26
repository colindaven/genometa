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

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.*;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.geom.Rectangle2D;

/**
 *  An abstract base class for several different biological sequence glyphs.
 */
public abstract class AbstractResiduesGlyph extends Glyph implements ResiduesGlyphI {
	// made abstract 6-24-98 to make explicit that should not be used directly --
	//   use a subclass instead
	private static final Color default_residue_color = Color.black;
	protected FontMetrics fontmet;
	protected int font_width, font_ascent, font_height;
	Glyph sel_glyph;

	/**
	 * seq_beg and seq_end are the sequence start and end positions
	 * relative to the reference coordinate system.
	 */
	protected int seq_beg, seq_end;
	protected int orient = HORIZONTAL;

	/**
	 * creates a horizontal glyph.
	 */
	public AbstractResiduesGlyph() {
		this(HORIZONTAL);
	}

	/**
	 * creates an abstract residues glyph.
	 *
	 * @param orientation should be HORIZONTAL or VERTICAL
	 */
	public AbstractResiduesGlyph(int orientation) {
		orient = orientation;
		//setResidueColor(default_residue_color);
		setResidueFont( NeoConstants.default_bold_font );
		setForegroundColor( default_residue_color );
	}

	public void setResidueFont(Font fnt) {
		fontmet = GeneralUtils.getFontMetrics(fnt);
		// change font
		this.style = stylefactory.getStyle( style.getForegroundColor(), style.getBackgroundColor(), fnt );

		font_width = fontmet.charWidth('C');
		font_width = Math.max(font_width, fontmet.charWidth('A'));
		font_width = Math.max(font_width, fontmet.charWidth('C'));
		font_width = Math.max(font_width, fontmet.charWidth('T'));
		/*  Temporary font diagnostics
			System.out.println("SETTING FONT: " + fnt);
			System.out.println("    acgt font widths: " + 
			fontmet.charWidth('a') + ", " +
			fontmet.charWidth('c') + ", " +
			fontmet.charWidth('g') + ", " + 
			fontmet.charWidth('t'));
			System.out.println("    ACGT font widths: " + 
			fontmet.charWidth('A') + ", " +
			fontmet.charWidth('C') + ", " +
			fontmet.charWidth('G') + ", " + 
			fontmet.charWidth('T'));
			System.out.println("FONT WIDTH: " + font_width);
			*/

		font_height = fontmet.getAscent();
		font_ascent = fontmet.getAscent();
		if (children != null) {
			Object child;
			for (int i=0; i<children.size(); i++) {
				child = children.get(i);
				if (child instanceof ResiduesGlyphI)
					((ResiduesGlyphI)child).setResidueFont( fnt );
			}
		}
	}

	public Font getResidueFont() { return style.getFont(); }
	public int getFontWidth() { return font_width; }
	public int getFontHeight() { return font_height; }
	public int getFontAscent() { return font_ascent; }

	public boolean supportsSubSelection() {
		return true;
	}

	public Rectangle2D.Double getSelectedRegion() {
		if (sel_glyph == null) {
			if (selected) {
				return this.getCoordBox();
			}
			else {
				return null;
			}
		}
		return sel_glyph.getCoordBox();
	}

	/**
	 * Calls super.setCoords and resets the reference space.
	 * Also resets the coords for all the children.
	 */
	public void setCoords(double x, double y, double width, double height) {
		super.setCoords(x, y, width, height);
		if (orient == HORIZONTAL) {
			seq_beg = (int)(coordbox.x);
			seq_end = (int)(coordbox.x + coordbox.width);
		} else if (orient == VERTICAL) {
			seq_beg = (int)(coordbox.y);
			seq_end = (int)(coordbox.y + coordbox.height);
		}
		if (children != null) {
			int i;
			GlyphI child;
			Rectangle2D.Double childbox;
			for (i=0; i<children.size(); i++) {
				child = children.get(i);
				childbox = child.getCoordBox();
				child.setCoords(childbox.x, y, childbox.width, height);
			}
		}
		if (sel_glyph != null) {
			Rectangle2D.Double selbox = sel_glyph.getCoordBox();
			sel_glyph.setCoords(selbox.x, y, selbox.width, height);
		}
	}

	/**
	 * This turns around and calls setCoords.
	 */
	public void setCoordBox( Rectangle2D.Double theBox ) {
		setCoords( theBox.x, theBox.y, theBox.width, theBox.height );
	}

	/**
	 * Overriding glyph.select(x,y,width,height) to ignore y & height.
	 * Just use x start and end (x+width).
	 * Should probably go in a LinearGlyph superclass...
	 */
	public void select(double x, double y, double width, double height) {
		if (orient == HORIZONTAL) {
			select(x, x+width);
		} else if (orient == VERTICAL) {
			select(y, y+height);
		}
	}

	/**
	 * @see #select(int, int)
	 */
	public void select (double start, double end) {
		select((int)start, (int)end);
	}

	/**
	 * Selects a range of residues.
	 *
	 * @param start the first residue to be selected.
	 * @param end the last residue to be selected.
	 */
	public void select(int start, int end) {
		selected = true;
		if (end >= start) { end += 1; }
		else { start += 1; }
		if (sel_glyph == null) {
			sel_glyph = new OutlineRectGlyph();
		}
		if (orient == HORIZONTAL) {
			if (start <= end) {
				if (start < coordbox.x) {
					start = (int)coordbox.x; }
				if (end > (coordbox.x + coordbox.width)) {
					end = (int)(coordbox.x + coordbox.width); }
			}
			else {
				if (end < coordbox.x) {
					end = (int)coordbox.x; }
				if (start > (coordbox.x + coordbox.width)) {
					start = (int)(coordbox.x + coordbox.width); }
			}
			sel_glyph.setCoords(start, coordbox.y, end-start, coordbox.height);
		} else if (orient == VERTICAL) {
			if (start <= end) {
				if (start < coordbox.y) {
					start = (int)coordbox.y; }
				if (end > (coordbox.y + coordbox.height)) {
					end = (int)(coordbox.y + coordbox.height); }
			}
			else {
				if (end < coordbox.y) {
					end = (int)coordbox.y; }
				if (start > (coordbox.y + coordbox.height)) {
					start = (int)(coordbox.y + coordbox.height); }
			}
			sel_glyph.setCoords(coordbox.x, start, coordbox.width, end-start);
		}
	}

	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		if ( ! isSelected() ) {
			sel_glyph = null;
		}
	}

	@Override
	protected void drawSelectedOutline(ViewI view) {
		if (sel_glyph != null)  {
			draw(view);
			sel_glyph.setForegroundColor(view.getScene().getSelectionColor());
			sel_glyph.drawTraversal(view);
		}
		else {
			super.drawSelectedOutline(view);
		}
	}

}
