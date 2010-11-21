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

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.datamodel.Translatable;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.*;



/**
 * your basic Glyph that wraps around to a new "line" when it extends far enough.
 */
public abstract class WrapGlyph extends Glyph implements Translatable  {

	// alternative way of viewing translation -- only make room for one
	//   translation above and/or below, and only draw translation _annotations_
	// NOT YET IMPLEMENTED
	protected boolean showForwardTransAnnots = false;
	protected boolean showReverseTransAnnots = false;

	// boolean for showing as:
	//   NUCLEOTIDES, COMPLEMENT,
	//   FRAME_ONE, FRAME_TWO, FRAME_THREE,
	//   FRAME_NEG_ONE, FRAME_NEG_TWO, FRAME_NEG_THREE
	// GAH 12-10-98 started adding space for two more -- actual (spliced)
	//      forward and reverse translation
	//  protected boolean allocateSpaceFor[] = new boolean[8];
	protected boolean allocateSpaceFor[] = new boolean[10];

	// SPACERS:
	// trans_space, trans_nucs_space, nucs_space
	// these may be a little strange, since due to the way Font.getHeight()
	//    works, to get adjacent lines of upper-case letters right next to
	//    each other, the "space" between them might need to be negative
	// pre_spacer, post_spacer NOT YET IMPLEMENTED -- using line_spacing instead

	// spacer between "start" of line and any actual font drawing
	protected int pre_spacer;
	// spacer between last font drawing and actual end of line;
	protected int post_spacer;
	// spacer between same-strand translations, if more than one shown
	protected int trans_spacer = -4;
	// spacer between translations and nucleotides
	protected int trans_nucs_spacer = -1;
	// spacer between nucleotide strands, if both strands are shown
	protected int nucs_spacer = -5;

	protected Font seqfont;
	protected FontMetrics seqfontmetrics;
	protected int font_width, font_height;

	protected int residues_per_line;
	protected int residues_per_stripe = 10;
	protected int line_spacing = 0;
	protected int line_height;

	protected boolean monospace = false;

	public WrapGlyph () {
		setFont(NeoConstants.default_bold_font);

		allocateSpaceFor[NUCLEOTIDES] = true;
		allocateSpaceFor[COMPLEMENT] = false;
		allocateSpaceFor[FRAME_ONE] = false;
		allocateSpaceFor[FRAME_TWO] = false;
		allocateSpaceFor[FRAME_THREE] = false;
		allocateSpaceFor[FRAME_NEG_ONE] = false;
		allocateSpaceFor[FRAME_NEG_TWO] = false;
		allocateSpaceFor[FRAME_NEG_THREE] = false;
		allocateSpaceFor[FORWARD_SPLICED_TRANSLATION] = false;
		allocateSpaceFor[REVERSE_SPLICED_TRANSLATION] = false;

	}

	/**
	 * gets the height of a "line".
	 *
	 * @return the height of a line of residues in pixels.
	 */
	public int getResidueHeight() {
		line_height = calcLineHeight();
		return line_height;
	}

	private int calcLineHeight() {
		int height = line_spacing;
		int positive_frames = 0, negative_frames = 0;
		if (allocateSpaceFor[NUCLEOTIDES]) {
			height += font_height;
		}
		if (allocateSpaceFor[COMPLEMENT]) {
			height += font_height;
		}

		// if showing both nucleotide strands, add in spacer between them
		if (allocateSpaceFor[NUCLEOTIDES] && allocateSpaceFor[COMPLEMENT]) {
			height += nucs_spacer;
		}

		// if showing tranlations, add room for them
		for (int i=FRAME_ONE; i<=FRAME_THREE; i++) {
			if (allocateSpaceFor[i]) {
				height += font_height;
				positive_frames++;
			}
		}
		for (int i=FRAME_NEG_ONE; i<=FRAME_NEG_THREE; i++) {
			if (allocateSpaceFor[i]) {
				height += font_height;
				negative_frames++;
			}
		}

		// if positive/negative translations are shown, then add spacer between
		//   nucleotides and translations
		if (positive_frames > 0) {
			height += trans_nucs_spacer;
		}
		if (negative_frames > 0) {
			height += trans_nucs_spacer;
		}

		// if multiple positive/negative translation frames are shown, then
		//    add spacers between the frames
		if (positive_frames > 1) {
			height += (positive_frames-1)*trans_spacer;
		}
		if (negative_frames > 1) {
			height += (negative_frames-1)*trans_spacer;
		}

		return height;
	}

	public int[] getResidueOffsets() {
		int residue_offsets[] = calcResidueOffsets();
		return residue_offsets;
	}

	public int[] calcResidueOffsets() {
		// fixed ordering for now:
		//     FRAME_THREE
		//     FRAME_TWO
		//     FRAME_ONE
		//     NUCLEOTIDES
		//     COMPLEMENT
		//     FRAME_NEG_ONE
		//     FRAME_NEG_TWO
		//     FRAME_NEG_THREE

		int sum = line_spacing;
		int offsets[] = new int[8];
		int positive_frames = 0, negative_frames = 0, nucleotides = 0;
		if (allocateSpaceFor[FRAME_THREE]) {
			sum += font_height;
			offsets[FRAME_THREE] = sum;
			positive_frames++;
		}
		if (allocateSpaceFor[FRAME_TWO]) {
			if (positive_frames > 0) { sum += trans_spacer; }
			sum += font_height;
			offsets[FRAME_TWO] = sum;
			positive_frames++;
		}
		if (allocateSpaceFor[FRAME_ONE]) {
			if (positive_frames > 0) { sum += trans_spacer; }
			sum += font_height;
			offsets[FRAME_ONE] = sum;
			positive_frames++;
		}
		if ((positive_frames > 0) &&
				(allocateSpaceFor[NUCLEOTIDES] || allocateSpaceFor[COMPLEMENT])) {
			sum += trans_nucs_spacer;
				}
		if (allocateSpaceFor[NUCLEOTIDES]) {
			sum += font_height;
			offsets[NUCLEOTIDES] = sum;
			nucleotides++;
		}
		if (allocateSpaceFor[COMPLEMENT]) {
			if (nucleotides > 0) { sum += nucs_spacer; }
			sum += font_height;
			offsets[COMPLEMENT] = sum;
			nucleotides++;
		}
		if ((nucleotides > 0) &&
				(allocateSpaceFor[FRAME_NEG_ONE] ||
				 allocateSpaceFor[FRAME_NEG_TWO] || allocateSpaceFor[FRAME_NEG_THREE])) {
			sum += trans_nucs_spacer;
				 }
		if (allocateSpaceFor[FRAME_NEG_ONE]) {
			sum += font_height;
			offsets[FRAME_NEG_ONE] = sum;
			negative_frames++;
		}
		if (allocateSpaceFor[FRAME_NEG_TWO]) {
			if (negative_frames > 0) { sum += trans_spacer; }
			sum += font_height;
			offsets[FRAME_NEG_TWO] = sum;
			negative_frames++;
		}
		if (allocateSpaceFor[FRAME_NEG_THREE]) {
			if (negative_frames > 0) { sum += trans_spacer; }
			sum += font_height;
			offsets[FRAME_NEG_THREE] = sum;
			negative_frames++;
		}
		return offsets;
	}

	public int getResidueWidth() {
		return font_width;
	}

	public void setSequence(SequenceI seq) {
		if (children != null)  {
			for (GlyphI child : children) {
				if (child instanceof WrapGlyph) {
					((WrapGlyph)child).setSequence(seq);
				}
			}
		}
	}

	public void setResiduesPerLine(int residues_per_line) {
		if (children != null)  {
			for (GlyphI child : children) {
				if (child instanceof WrapGlyph) {
					((WrapGlyph)child).setResiduesPerLine(residues_per_line);
				}
			}
		}
		this.residues_per_line = residues_per_line;
	}

	@Override
	public abstract void draw(ViewI view);

	@Override
	public Font getFont() {
		return seqfont;
	}

	@Override
	public void setFont(Font seqfont) {
		this.seqfont = seqfont;
		seqfontmetrics = GeneralUtils.getFontMetrics(seqfont);
		font_width = seqfontmetrics.charWidth('A');
		font_width = (seqfontmetrics.charWidth('C') > font_width)
			? seqfontmetrics.charWidth('C') : font_width;
		font_width = (seqfontmetrics.charWidth('G') > font_width)
			? seqfontmetrics.charWidth('G') : font_width;
		font_width = (seqfontmetrics.charWidth('T') > font_width)
			? seqfontmetrics.charWidth('T') : font_width;

		font_height = seqfontmetrics.getHeight();
		if (
				(font_width == seqfontmetrics.charWidth('A'))
				&& (font_width == seqfontmetrics.charWidth('C'))
				&& (font_width == seqfontmetrics.charWidth('G'))
				&& (font_width == seqfontmetrics.charWidth('T'))
				&& (font_width == seqfontmetrics.charWidth(' ')) ) {
			monospace = true;
				}
		else {
			monospace = false;
		}
	}

	public void setFontSize(int size) {
		setFont(new Font(seqfont.getFamily(), seqfont.getStyle(), size));
	}

	public int getFontSize() {
		return seqfont.getSize();
	}

	public void setSpacing(int spacing) {
		line_spacing = spacing;
	}

	public int getSpacing() {
		return line_spacing;
	}

	public void setShow(int type, boolean show) {
		allocateSpaceFor[type] = show;
		// do recalcs here???
	}

	public boolean getShow(int type) {
		return allocateSpaceFor[type];
	}

}
