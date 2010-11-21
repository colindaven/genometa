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

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.datamodel.NASequence;
import com.affymetrix.genoviz.datamodel.Range;
import java.awt.*;
import java.awt.geom.Rectangle2D;

public class WrapSequence extends WrapGlyph {

	private static final boolean debug = false;

	protected SequenceI seq;

	protected int seq_start;
	protected int seq_end;

	protected Rectangle2D.Double visible_box;

	// the first and last visible sequence residue
	protected int first_visible_residue, last_visible_residue;

	private int lastPotentiallyVisibleResidue;

	protected WrapStripes stripe_glyph = new WrapStripes();
	protected WrapAnnot annot_glyph = new WrapAnnot();
	protected WrapAnnot font_annot_glyph = new WrapAnnot();
	protected WrapAnnot outline_annot_glyph = new WrapAnnot();
	protected WrapColors sel_glyph = null;

	public WrapSequence() {
		// shouldn't need any of this at the moment, since drawTraversal has
		//   been overriden to call drawTraversal on these glyphs directly, rather
		//   than go through the child list
		this.setDrawOrder(DRAW_CHILDREN_FIRST);
		this.addChild(stripe_glyph, 0);
		this.addChild(annot_glyph);
		this.addChild(font_annot_glyph);
		this.addChild(outline_annot_glyph);
	}

	@Override
	public void setSequence (SequenceI seq)  {
		super.setSequence(seq);
		this.seq = seq;

		Range seq_range = seq.getRange();
		seq_start = seq_range.beg;
		seq_end = seq_range.end;
	}

	/**
	 * Draws the sequence, wrapping into multiple lines.
	 */
	public void draw(ViewI view) {
		visible_box = ((View)view).calcCoordBox();

		first_visible_residue = (int)visible_box.y;

		if (residues_per_line < 1) {
			return;
		}

		last_visible_residue = useConstrain(residues_per_line, visible_box.y, visible_box.height);

		lastPotentiallyVisibleResidue = last_visible_residue;

		if (last_visible_residue > seq_end) {
			last_visible_residue = seq_end;
		}
		if (first_visible_residue < 0) {
			System.out.println("first_visible_residue = " + first_visible_residue);
		}
		drawResidues(first_visible_residue, last_visible_residue, view,
				seqfont, getBackgroundColor());
	}

	/**
	 * gets the number of residues we can show with the current size.
	 *
	 * @return number of residues.
	 */
	public int getResiduesPerScreen() {
		return lastPotentiallyVisibleResidue + 1 - first_visible_residue;
	}

	/**
	 *  draw Residues for all types that space has been allocated for in
	 *  getResidueHeight and getOffsets
	 */
	protected void drawResidues(
			int start, int end, ViewI view, Font fnt, Color col) {
		// allocateSpaceFor[] boolean array inherited from WrapGlyph
		drawResidues(start, end, view, fnt, col, allocateSpaceFor);
			}

	protected void drawResidueChars(
			Graphics g,
			String res_string,
			int xposition,
			int yposition,
			boolean isTranslation,
			int first_drawn_residue) {
		if (res_string == null) {
			return;
		}

		if (monospace && !isTranslation) {
			g.drawString(res_string, xposition, yposition);
			return;
		}

		// Loop over the res_string, drawing when appropriate

		int l = res_string.length();
		for (int i = 0; i < l; i++, xposition += font_width) {
			g.drawString(res_string.substring(i, i+1), xposition, yposition);
		}
			}

	protected void drawResidues(int start, int end, ViewI view,
			Font fnt, Color col, boolean showArray[]) {

		// if sequence doesn't exist, nothing to do here.
		if ((seq == null) || (seq.getLength() < 1))
			return;

		// if sequence isn't visible, don't need to draw anything
		if (end < first_visible_residue || start > last_visible_residue) {
			return;
		}

		int xposition, yposition, line_yposition;

		if (start < first_visible_residue) {
			start = first_visible_residue;
		}
		if (end > last_visible_residue) {
			end = last_visible_residue;
		}

		// *_line_num is line number (relative to visible number of lines, i.e.
		// first visible line is 0) where start/end base is located
		int start_line_num = ((start - first_visible_residue) / residues_per_line);
		int end_line_num =
			(int)(Math.ceil((end - first_visible_residue) / residues_per_line));
		int line_height = getResidueHeight();
		int offsets[] = getResidueOffsets();
		if (debug) {
			System.out.println("Start/End/StartLine/EndLine/FirstVis/LastVis: " +
					start + ", " + end + ", " +
					start_line_num + ", " + end_line_num + ", " +
					first_visible_residue + ", " + last_visible_residue);
		}

		line_yposition = start_line_num * line_height;

		int first_drawn_residue, last_drawn_residue;
		int line_beg_index, line_end_index;
		String res_string;

		Graphics g = view.getGraphics();
		g.setFont(fnt);
		g.setColor(col);

		int first_beg_index =
			first_visible_residue + start_line_num * residues_per_line;
		int last_beg_index =
			first_visible_residue + end_line_num * residues_per_line;

		for ( line_beg_index = first_beg_index;
				line_beg_index <= last_beg_index;
				line_beg_index += residues_per_line ) {

			line_end_index = line_beg_index + residues_per_line - 1;
			if (debug) {
				System.out.println("LineBegIndex/LineEndIndex/LastBegIndex: " +
						line_beg_index + ", " + line_end_index + ", " +
						last_beg_index);
			}
			if (start <= line_beg_index) {
				first_drawn_residue = line_beg_index;
				xposition = 0;
			}
			else {
				first_drawn_residue = start;
				xposition = (first_drawn_residue - line_beg_index) * font_width;
			}

			if (end >= line_end_index) {
				last_drawn_residue = line_end_index;
			}
			else {
				last_drawn_residue = end;
			}
			if (debug)  {
				System.out.println("End/LineEndIndex/LastDrawnResidue: " +
						end + ", " + line_end_index + ", " +
						last_drawn_residue);
			}

			if (first_drawn_residue > seq_end) {
				break;
			}
			if (last_drawn_residue > seq_end) {
				last_drawn_residue = seq_end;
			}

			if (debug)  {
				System.out.println("FirstDrawnResidue/LastDrawnResidue: " +
						first_drawn_residue + ", " + last_drawn_residue);
			}

			if (showArray[NUCLEOTIDES] && allocateSpaceFor[NUCLEOTIDES]) {
				yposition = line_yposition + offsets[NUCLEOTIDES];
				res_string = seq.getResidues(first_drawn_residue,
						last_drawn_residue+1);

				drawResidueChars(g, res_string, xposition, yposition,
						false, first_drawn_residue);
			}
			if (showArray[COMPLEMENT] && allocateSpaceFor[COMPLEMENT] &&
					seq instanceof NASequence) {
				yposition = line_yposition + offsets[COMPLEMENT];
				res_string = ((NASequence)seq).getComplement().substring(
						first_drawn_residue,
						last_drawn_residue+1);
				drawResidueChars(g, res_string, xposition, yposition,
						false, first_drawn_residue);
					}
			for (int j=FRAME_ONE; j<=FRAME_NEG_THREE; j++) {
				if (showArray[j] && allocateSpaceFor[j]) {
					yposition = line_yposition + offsets[j];

					String translation = ((NASequence)seq).getTranslation(j);
					if ((translation == null) || (translation.length() < 1))
						continue;

					res_string = translation.substring(first_drawn_residue,
							last_drawn_residue+1);
					drawResidueChars(g, res_string, xposition, yposition,
							true, first_drawn_residue);
				}
			}

			line_yposition += line_height;
				}
	}

	@Override
	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		calcPixels(view);
		return isVisible && pixel_hitbox.intersects(pixelbox);
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return isVisible && coord_hitbox.intersects(coordbox);
	}

	public int getStartVisible() {
		return 0;
	}

	public int getEndVisible() {
		return 0;
	}

	public Range getVisible() {
		return new Range(0,0);
	}

	public SequenceI getSequence() {
		return seq;
	}

	public String getVisibleResidues() {
		return getSequence().getResidues(0,0);
	}

	public String getSelectedResidues() {
		return getSequence().getResidues(0,0);
	}

	@Override
	public void setCoords(double x, double y, double width, double height) {
		stripe_glyph.setCoords(x, y, width, height);
		annot_glyph.setCoords(x, y, width, height);
		font_annot_glyph.setCoords(x, y, width, height);
		outline_annot_glyph.setCoords(x, y, width, height);
		super.setCoords(x, y, width, height);
	}

	@Override
	public void setCoordBox(Rectangle2D.Double coordbox)   {
		stripe_glyph.setCoordBox(coordbox);
		annot_glyph.setCoordBox(coordbox);
		font_annot_glyph.setCoordBox(coordbox);
		outline_annot_glyph.setCoordBox(coordbox);
		super.setCoordBox(coordbox);
	}

	public void setStripeWidth(int i) {
		stripe_glyph.setStripeWidth(i);
	}
	public void setStripeOrientation(int i) {
		stripe_glyph.setStripeOrientation(i);
	}

	public int getStripeOrientation() {
		return stripe_glyph.getStripeOrientation();
	}

	public void setStripeColors(Color[] colors) {
		stripe_glyph.setStripeColors(colors);
	}
	public Color[] getStripeColors() {
		return stripe_glyph.getStripeColors();
	}


	public GlyphI addTextColorAnnotation(int start, int end, Color color) {
		WrapFontColors color_glyph = new WrapFontColors();
		color_glyph.setSequence(seq);
		color_glyph.setColor(color);
		color_glyph.setColorSpan(start,end);
		color_glyph.setCoordBox(coordbox);
		color_glyph.setWrapSequence(this);
		font_annot_glyph.addChild(color_glyph);
		return color_glyph;
	}

	public GlyphI addOutlineAnnotation(int start, int end, Color color) {
		WrapColors color_glyph = new WrapColors();
		color_glyph.setSequence(seq);
		color_glyph.setColor(color);
		color_glyph.setColorSpan(start,end);
		color_glyph.setCoordBox(coordbox);
		color_glyph.setFill(WrapColors.OUTLINE);
		outline_annot_glyph.addChild(color_glyph);
		return color_glyph;
	}

	public GlyphI addAnnotation(int start, int end, Color color) {
		WrapColors color_glyph = new WrapColors();
		color_glyph.setSequence(seq);
		color_glyph.setColor(color);
		color_glyph.setColorSpan(start,end);
		color_glyph.setCoordBox(coordbox);
		annot_glyph.addChild(color_glyph);
		return color_glyph;
	}

	public void clearAnnotations() {
		removeChild(annot_glyph);
		removeChild(font_annot_glyph);
		removeChild(outline_annot_glyph);

		annot_glyph = new WrapAnnot();
		font_annot_glyph = new WrapAnnot();
		outline_annot_glyph = new WrapAnnot();

		addChild(annot_glyph);
		addChild(font_annot_glyph);
		addChild(outline_annot_glyph);

		sel_glyph = null;
	}

	public void removeAnnotation(GlyphI color_glyph) {
		annot_glyph.removeChild(color_glyph);
		font_annot_glyph.removeChild(color_glyph);
		outline_annot_glyph.removeChild(color_glyph);
	}

	protected Color sel_color = Color.red;

	public void setHighlightColor(Color color) {
		sel_color = color;
		if (null != sel_glyph) {
			sel_glyph.setColor(sel_color);
		}
	}

	// need to change this so selection doesn't compete with
	//  annotations -- selection should always be on top???
	public void highlightResidues(int start, int end) {
		if (sel_glyph == null) {
			sel_glyph = new WrapColors();
			sel_glyph.setSequence(seq);
			sel_glyph.setColor(sel_color);
			sel_glyph.setCoordBox(coordbox);
			sel_glyph.setFill(WrapColors.SELECTED);

			// shouldn't need this at the moment, since drawTraversal has been
			// overriden to call drawTraversal on these glyphs directly, rather
			//   than go through the child list
			annot_glyph.addChild(sel_glyph);
		}
		sel_glyph.setColorSpan(start,end);
		sel_glyph.setSelected(true);
	}

	public void unhighlight() {
		if (annot_glyph != null && sel_glyph != null) {
			annot_glyph.removeChild(sel_glyph);
		}
		sel_glyph = null;
	}

	public WrapColors getHighlightGlyph() {
		return sel_glyph;
	}

	/**
	 * draws all the children and this glyph as well.
	 * Some children need to be drawn before the residues
	 * and some after.
	 */
	@Override
	public void drawTraversal(ViewI view)  {
		if (isVisible && coordbox.intersects(view.getCoordBox())) {

			stripe_glyph.drawTraversal(view);

			annot_glyph.drawTraversal(view);

			if (sel_glyph != null) {
				sel_glyph.drawTraversal(view);
			}

			draw(view);

			font_annot_glyph.drawTraversal(view);

			outline_annot_glyph.drawTraversal(view);

			/* Draw all the children not already drawn above. */
			for (GlyphI o : getChildren()) {
				if ( o != this.stripe_glyph &&
						o != this.annot_glyph &&
						o != this.sel_glyph &&
						o != this.font_annot_glyph &&
						o != this.outline_annot_glyph ) {
					o.drawTraversal(view);
						}
				}

		}
	}

	private static int useConstrain(int residues_per_line, double y, double height) {
		return (int) (y + height - (height % residues_per_line) - 1);
	}

}
