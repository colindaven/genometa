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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.awt.Graphics;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.ResiduePainter;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.geom.Rectangle2D;

/**
 *    Base class used in NeoAssembler
 *    to display spans of aligned residues
 *    with no gaps.
 *
 *<p> {@link com.affymetrix.genoviz.widget.NeoAssembler} uses
 *    an {@link com.affymetrix.genoviz.glyph.AlignmentGlyph}
 *    to display an aligned sequence with gaps,
 *    and AlignmentGlyph in turn uses subclasses
 *    of AlignedResiduesGlyphs,
 *    one for each continuous span of aligned residues
 *    (no gaps).
 *
 * @author Gregg Helt
 */
public class AlignedResiduesGlyph extends Glyph implements ResiduesGlyphI  {

	boolean monospace = false;

	/** indicates foreground (characters) used for color-coding, etc. */
	public static final int FOREGROUND = 100;

	/** indicates background (shaded rects) used for color-coding, etc. */
	public static final int BACKGROUND = 101;

	/** color-coding based on residue identity. */
	public static final int RESIDUE_BASED = 0;

	/** color-coding based on residue comparison to a consensus residue. */
	public static final int ALIGNMENT_BASED = 1;

	/** no color-coding, color is fixed constant. */
	public static final int FIXED_COLOR = 2;

	/**
	 * no color-coding, not drawn at all.
	 * This may be useful for font or background annotations
	 *   as children of an existing AlignedResiduesGlyph (??).
	 * Have to resolve interleaved draw ordering issues
	 *   for this to work though.
	 */
	public static final int NONE = 3;

	/**
	 * color-coding by an external {@link ResiduePainter}.
	 */
	public static final int CALCULATED = 4;

	/**
	 * color strategy of residue font color (foreground).
	 * Residue background rectangle color can be varied independently
	 * each is either:
	 *   RESIDUE_BASED, ALIGNMENT_BASED, FIXED_COLOR, NONE  (maybe CALCULATED ??)
	 * For now, only implementing choices for background, assuming
	 *   font is staying FIXED (and possible NONE)
	 * May want to include a third for outline color strategy, but this
	 *   really only makes sense as either fixed or none...
	 */
	protected int fg_color_strategy = FIXED_COLOR;
	protected int bg_color_strategy = NONE;

	protected ResiduePainter fg_painter;
	protected ResiduePainter bg_painter;

	/**
	 *  Color to use for background if residue letter is "unrecognized"
	 *  (doesn't map to a residue id)
	 */
	protected Color unknown_residue_background = null;

	/**
	 * matrix of colors to use if coloring scheme is ALIGNMENT_BASED.
	 */
	protected Color[][] fg_mapper_matrix;
	protected Color[][] bg_mapper_matrix;

	/**
	 * array of colors to use if coloring scheme is RESIDUE_BASED.
	 *   (could probably expand this into a color matrix so color matching
	 *   code could be same for RESIDUE or ALIGNMENT based coloring strategies.)
	 */
	protected Color[] fg_mapper_array;
	protected Color[] bg_mapper_array;

	/** fixed color to use if coloring strategy is FIXED_COLOR. */
	protected Color fg_color = Color.black;
	protected Color bg_color = null;

	protected Font residue_font;

	protected int font_width, font_height;

	protected String sequence;
	protected SequenceI consensus;
	protected String match_char_string;
	protected Character match_char;
	protected char[] residue_array;
	protected Color[] bg_color_array;
	protected Color[] fg_color_array;

	protected FillRectGlyph full_rect;

	protected boolean setSequence = false;
	protected boolean setConsensus = false;
	protected Rectangle2D.Double scratchrect;

	// not sure if these are needed anymore
	protected int parent_seq_beg, parent_seq_end;

	/**
	 * seq_beg and seq_end are the sequence start and end positions
	 * relative to the reference coordinate system.
	 * Not the consensus.
	 */
	int seq_beg, seq_end;

	public AlignedResiduesGlyph() {
		super();
		full_rect = new FillRectGlyph();
		full_rect.setPacker(null);
		scratchrect = new Rectangle2D.Double();
		setResidueFont(NeoConstants.default_bold_font);
	}


	/**
	 *  Note that this assumes adding based on <em>sequence</em>
	 *  so that it will <em>include</em> the end "--".
	 *  Thus, if x = 0, width = 2,
	 *  really creating a sequence annotation
	 *  with beg = 0, end = 1
	 */
	public void setCoords(double x, double y, double width, double height) {
		super.setCoords(x, y, width, height);
		full_rect.setCoords(x, y, width, height);
		seq_beg = (int)x;
		seq_end = (int)(x+width-1);
	}

	public void setResidues(String sequence) {
		this.sequence = sequence;
		residue_array = sequence.toCharArray();
		if (! (bg_color_strategy == NONE || bg_color_strategy == FIXED_COLOR)) {
			bg_color_array = new Color[residue_array.length];
		}
		if (! (fg_color_strategy == NONE || fg_color_strategy == FIXED_COLOR)) {
			fg_color_array = new Color[residue_array.length];
		}
		setSequence = true;
		// redo consensus comparison if have consensus
		if (consensus != null && bg_color_strategy == ALIGNMENT_BASED) {
			setReference(consensus);
			// redoColors() called in setReference...
		}
		else if (bg_color_strategy == RESIDUE_BASED) {
			redoColors();
		}
		makeMatchCharString();
	}

	public String getResidues() {
		return sequence;
	}

	public void setReference(SequenceI consensus) {
		this.consensus = consensus;
		setConsensus = true;
		makeMatchCharString();
		redoColors();
	}

	/** redo both background and foreground color arrays. */
	public void redoColors(int[] charMap) {
		redoColors(charMap, BACKGROUND);
		redoColors(charMap, FOREGROUND);
	}

	/**
	 * Sets the background colors of each residue.
	 * TODO: does this affect foreground colors?
	 * The colors will be set according to a strategy previously set.
	 *
	 * @param charMap is a mapping of all possible ascii characters to
	 *      residues id codes (unused characters = -1)
	 * @param type is either FOREGROUND (for font colors)
	 *      or BACKGROUND (for background rects)
	 */
	public void redoColors(int[] charMap, int type)  {

		int residue_to_id_map[] = charMap;
		char seq_char, cons_char;
		int seq_id, cons_id;
		int i, j;

		int color_strategy;
		Color[] color_array;
		Color[] mapper_array;
		Color[][] mapper_matrix;

		ResiduePainter painter;

		// Sanity check:

		if (residue_array == null ||
				bg_color_strategy == NONE ||
				bg_color_strategy == FIXED_COLOR) {
			return;
				}

		// Assign a slew of variables dependent on the coloring goal

		if (type == FOREGROUND) {
			color_strategy = fg_color_strategy;
			color_array    = fg_color_array;

			mapper_array  = fg_mapper_array;
			mapper_matrix = fg_mapper_matrix;

			painter = fg_painter;
		} else if (type == BACKGROUND) {
			color_strategy = bg_color_strategy;
			color_array    = bg_color_array;

			mapper_array  = bg_mapper_array;
			mapper_matrix = bg_mapper_matrix;

			painter = bg_painter;
		} else
			return;

		// Depending on the color strategy, fill up color_array

		if (color_strategy == RESIDUE_BASED) {
			if (mapper_array == null) {
				return;
			}

			for (j = 0, i = seq_beg; i <= seq_end; i++, j++) {
				try {
					seq_char = residue_array[j];
					seq_id   = residue_to_id_map[seq_char];
					color_array[j] = mapper_array[seq_id];
				} catch (ArrayIndexOutOfBoundsException e) {

					// if there's a problem, assume that last color in array is
					// for unknown residues

					color_array[j] = mapper_array[mapper_array.length-1];
				}
			}

		} else if (color_strategy == ALIGNMENT_BASED) {

			if (consensus == null || mapper_matrix == null) {
				return;
			}

			for (j = 0, i = seq_beg; i <= seq_end; i++, j++) {
				try {
					// Check inorder to prevent Array out of bounds exception
					// when the length of color array is small -hari 3/21/2000
					if (j >= residue_array.length)
						break;
					seq_char = residue_array[j];
					seq_id   = residue_to_id_map[seq_char];

					cons_char = consensus.getResidue(i);
					cons_id   = residue_to_id_map[cons_char];

					color_array[j] = mapper_matrix[seq_id][cons_id];
				}
				catch (Exception e) {

					// if there's a problem, assume that last color in matrix is
					// for unknown residues

					color_array[j] =
						mapper_matrix[mapper_matrix.length-1][mapper_matrix.length-1];

				}
			}

		} else if (color_strategy == CALCULATED && painter != null) {

			// This condition allows hook for color-coding via an external
			// ResiduePainter object

			String cons_string = (consensus == null ? null : consensus.getResidues());

			// Calculate the colors based on the glyph's info (aka
			// datamodel), if it's present.

			Object dataModel = this.getInfo();

			if (dataModel != null) {
				painter.calculateColors(dataModel, color_array, sequence,
						cons_string, 0, seq_beg);
			} else {
				painter.calculateColors(color_array, sequence,
						cons_string, 0, seq_beg);
			}
		}

	}

	/**
	 * redo colors with a default map.
	 * This base class just returns!
	 * Subclasses should call {@link #redoColors(int[])}.
	 */
	public void redoColors() {
	}

	public String getSequence() {
		return sequence;
	}

	/**
	 * This method sets a reference Character; the Glyph then draws
	 * any residues that match with the reference as that Character.
	 * Set to null to disable this feature.
	 */

	public void setMatchChar ( Character match_char ) {
		this.match_char = match_char;
		makeMatchCharString();
	}

	/**
	 * returns the character substitued for residues matching the
	 * reference.
	 */

	public Character getMatchChar ( Character match_char ) {
		return this.match_char;
	}

	public void draw(ViewI view) {
		
		Rectangle2D.Double coordclipbox = view.getCoordBox();
		Graphics g = view.getGraphics();
		double pixels_per_residue;
		int visible_ref_beg, visible_ref_end,
			visible_seq_beg, visible_seq_end, visible_seq_span,
			seq_beg_index, seq_end_index;
		visible_ref_beg = (int)coordclipbox.x;
		visible_ref_end =  (int)(coordclipbox.x + coordclipbox.width);

		// ******** determine first residue and last residue displayed ********
		visible_seq_beg = (seq_beg < visible_ref_beg) ? visible_ref_beg : seq_beg;
		visible_seq_end = (seq_end > visible_ref_end) ? visible_ref_end : seq_end;
		visible_seq_span = visible_seq_end - visible_seq_beg + 1;
		seq_beg_index = visible_seq_beg - seq_beg;
		seq_end_index = visible_seq_end - seq_beg;

		scratchrect.setRect(visible_seq_beg,  coordbox.y,
				visible_seq_span, coordbox.height);
		view.transformToPixels(scratchrect, pixelbox);
		pixels_per_residue = ((double)pixelbox.width)/scratchrect.width;
		int seq_pixel_offset = pixelbox.x;

		// ***** draw a normal rect if scale is < 1 pixel per residue ******
		// ****  or if sequence has not been set ****
		if ( pixels_per_residue < 1 || !setSequence ) {
			full_rect.setCoordBox ( this.getCoordBox() );
			full_rect.draw(view);
		}

		// ***** otherwise semantic zooming to show more detail *****
		else {
			int i, pixelstart, pixelwidth;
			double doublestart;

			if (selected && view.getScene().getSelectionAppearance() == Scene.SELECT_FILL) {
				g.setColor(view.getScene().getSelectionColor());
				g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
			}
			else if (bg_color_strategy == FIXED_COLOR) {
				g.setColor(this.getBackgroundColor());
				g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
			}
			// only draw background shading for each residue if:
			//   sequence has been set AND
			//   either the consensus has been set or coloring by residue identity
			else if (setSequence &&
					(bg_color_strategy == RESIDUE_BASED ||
					 (bg_color_strategy == ALIGNMENT_BASED && setConsensus) ||
					 (bg_color_strategy == CALCULATED && bg_painter != null))) {
				// ********* draw boxes for each visible residue if possible *******
				pixelwidth = (int)pixels_per_residue;
				//          pixelwidth = (int)Math.round(pixels_per_residue);
				doublestart = (double)seq_pixel_offset;
				Color last_color = null;
				int box_width;
				for (i = seq_beg_index; i <= seq_end_index; i++) {
					// Check inorder to prevent Array out of bounds exception
					// when the length of color array is small - hari 3/21/2000
					if (i+1 >= residue_array.length)
						break;
					box_width = pixelwidth;
					pixelstart = (int)doublestart;
					last_color = bg_color_array[i];

					// This little while loop greatly improves redraw speed in cases
					// where there are long stretches where background color for
					// adjacent positions is identical.
					while ((i < seq_end_index) &&
							(bg_color_array[i+1] == last_color)) {
						// Check inorder to prevent Array out of bounds exception
						//  when the length of color array is small - hari 3/21/2000
						if (i >= residue_array.length)
							break;
						i++;
						box_width += pixelwidth;
							}
					// changing strategy -- if the color is null, then don't draw
					//   _any_ background  GAH 7-25-98
					if (last_color != null) {
						g.setColor(last_color);
						g.fillRect(pixelstart, pixelbox.y,
								box_width, pixelbox.height);
					}
					doublestart += box_width;
				}
					 }

			/***** draw the sequence string for visible residues if possible ****
			 * Should the DNA string be drawn?
			 *  already tested for setSequence, but testing again for clarity
			 *  test for scale being natural number (integer double)
			 *    and for scale matching font size
			 */
			if (setSequence &&
					((double)((int)pixels_per_residue) == pixels_per_residue) &&
					((int)pixels_per_residue == font_width)) {
				doublestart = (double)seq_pixel_offset;
				pixelstart = (int)doublestart;
				int baseline = (pixelbox.y+(pixelbox.height/2)) + font_height/2;

				if (monospace && (fg_color_strategy == FIXED_COLOR))  {
					// this call to String.substring() is more efficient than it looks,
					// because if whole glyph is on screen, then substring will be same
					// as whole string, and String.substring() is optimized to then
					// return the whole String without creating a new one.  So only
					// new Strings being created are for glyphs on the edges of the map
					g.setFont(getResidueFont());
					g.setColor(getForegroundColor());

					// Check inorder to prevent Array out of bounds exception
					// when the length of color array is small - hari 3/21/2000
					if (seq_end_index+1 <= residue_array.length) {
						if (match_char_string == null)
							g.drawString(sequence.substring
									(Math.max ( 0, seq_beg_index),seq_end_index+1), pixelstart, baseline);
						else
							g.drawString(match_char_string.substring
									(Math.max ( 0, seq_beg_index),seq_end_index+1), pixelstart, baseline);
					}
				}
				else {
					// not using monospaced font, or using multiple colors for
					//   drawing residue characters
					// therefore need to draw each character _individually_,
					//   with correct position and color
					// Note that if standard Graphics implementation ever gets an
					//    _efficient_ drawChars(), should probably reimplement this loop
					//    using single chars and Graphics.drawChars() calls

					// if !monospaced but font color is fixed, need to draw each residue
					//     individually, but only need to set color once
					if (fg_color_strategy == FIXED_COLOR) {
						g.setFont(getResidueFont());
						String current_char;
						int xposition = pixelstart;
						g.setColor(fg_color);
						for (i = Math.max  (0, seq_beg_index); i <= seq_end_index; i++) {
							current_char = sequence.substring(i, i+1);
							g.drawString(current_char, xposition, baseline);
							xposition += font_width;
						}
					}

					// font color is not fixed, so need to draw each residue
					//    individually with its own color setting
					else {
						g.setFont(getResidueFont());
						Color last_color;
						String current_char;
						int xposition = pixelstart;
						for (i = seq_beg_index; i <= seq_end_index; i++) {
							last_color = fg_color_array[i];
							if (last_color != null) {
								g.setColor(last_color);
								current_char = sequence.substring(i, i+1);
								g.drawString(current_char, xposition, baseline);
								xposition += font_width;
							}
						}
					}  // end (fg_color_strategy != FIXED_COLOR)

				}  // end (!monospaced)
					}  // end (draw residues characters?)
		}  // (semantic zooming to show more detail)
	}


	public void setSelected(boolean selected) {
		super.setSelected(selected);
		full_rect.setSelected(selected);
	}

	public void setParentSeqStart(int beg)  {
		parent_seq_beg = beg;
	}

	public void setParentSeqEnd(int end)  {
		parent_seq_end = end;
	}

	public int getParentSeqStart() {
		return parent_seq_beg;
	}

	public int getParentSeqEnd() {
		return parent_seq_end;
	}

	/**
	 * sets the font for displaying residues
	 * and notes the height &amp; width
	 * of a "typical" (upper case) character.
	 *
	 * @param f the font to use for residues.
	 */
	public void setResidueFont(Font f) {
		residue_font = f;
		FontMetrics fontmet = GeneralUtils.getFontMetrics(residue_font);
		font_width = fontmet.charWidth('G');
		font_height = fontmet.getAscent(); // Descent not needed for capital letters
		monospace = GeneralUtils.isReallyMonospaced(residue_font,
				DNAUtils.getAllowedDNACharacters());
		if (!monospace) {
			font_width = GeneralUtils.getMaxCharWidth(residue_font,
					DNAUtils.getAllowedDNACharacters());
		}
	}

	public Font getResidueFont() { return residue_font; }
	public void setColor(Color col) {
		super.setBackgroundColor(col);
		full_rect.setBackgroundColor(col);
	}

	public void setForegroundColor(Color col) { fg_color = col; }
	public Color getForegroundColor() { return fg_color; }
	public void setBackgroundColor(Color col) { bg_color = col; }
	public Color getBackgroundColor() { return bg_color; }
	public void setBackgroundColorArray(Color[] col_array) {
		bg_mapper_array = col_array;
	}
	public void setForegroundColorArray(Color[] col_array) {
		fg_mapper_array = col_array;
	}
	public void setBackgroundColorMatrix(Color[][] col_matrix) {
		bg_mapper_matrix = col_matrix;
	}
	public void setForegroundColorMatrix(Color[][] col_matrix) {
		fg_mapper_matrix = col_matrix;
	}

	public void setBackgroundColorStrategy(int strategy) {
		bg_color_strategy = strategy;
		if ((bg_color_array == null) && (residue_array != null) &&
				(! (bg_color_strategy == NONE || bg_color_strategy == FIXED_COLOR)))  {
			bg_color_array = new Color[residue_array.length];
				}
		else if (bg_color_strategy == NONE || bg_color_strategy == FIXED_COLOR) {
			bg_color_array = null;
		}
		redoColors();
	}

	public void setForegroundColorStrategy(int strategy) {
		fg_color_strategy = strategy;
		if ((fg_color_array == null) && (residue_array != null) &&
				(! (fg_color_strategy == NONE || fg_color_strategy == FIXED_COLOR)))  {
			fg_color_array = new Color[residue_array.length];
				}
		else if (fg_color_strategy == NONE || fg_color_strategy == FIXED_COLOR) {
			fg_color_array = null;
		}
		redoColors();
	}

	public void setBackgroundPainter(ResiduePainter rp) {
		bg_painter = rp;
	}

	public void setForegroundPainter(ResiduePainter rp) {
		fg_painter = rp;
	}

	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		calcPixels(view);
		return isVisible && pixel_hitbox.intersects(pixelbox);
	}

	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return isVisible && coord_hitbox.intersects(coordbox);
	}

	public void setUnknownResidueBackground(Color col) {
		unknown_residue_background = col;
	}
	public Color getUnknownResidueBackground() {
		return unknown_residue_background;
	}


	private void makeMatchCharString() {
		if ( match_char == null || sequence == null || consensus == null ) {
			match_char_string = null;
			return;
		}
		char match = match_char.charValue();
		StringBuffer sb = new StringBuffer ( sequence.length() );
		for ( int i = 0; i < sequence.length(); i++ ) {
			if ( sequence.charAt(i) == consensus.getResidue(i +  seq_beg) ) {
				sb.append ( match );
			}
			else {
				sb.append ( sequence.charAt(i) );
			}
		}
		match_char_string = sb.toString();
	}

}
