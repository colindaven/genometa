package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genometryImpl.util.ImprovedStringCharIter;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.OutlineRectGlyph;
import com.affymetrix.genoviz.glyph.LabelledRectGlyph;
import com.affymetrix.igb.IGBConstants;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;


/**
 * CharSeqGlyph differs from SequenceGlyph in that it can take either a 
 * String as residues (via sg.setResidues()) or a {@link SearchableCharIterator}
 * as a provider of residues (via sg.setResiduesProvider()) .
 * This allows one to glyphify large biological sequences while maintaining a more 
 * compressed representation of the sequences residues.
 *
 * A glyph that shows a sequence of residues.
 * At low resolution (small scale) as a solid background rectangle
 * and at high resolution overlays the residue letters.
 *
 */
public final class CharSeqGlyph extends SequenceGlyph
		 {
	private SearchableCharIterator chariter;
	private int residue_length = 0;
	private static final Font mono_default_font = new Font("Monospaced", Font.BOLD, 12);

	// default to true for backward compatability
	private boolean hitable = true;
	public static final String PREF_A_COLOR = "Adenine color";
	public static final String PREF_T_COLOR = "Thymine color";
	public static final String PREF_G_COLOR = "Guanine color";
	public static final String PREF_C_COLOR = "Cytosine color";
	public static final String PREF_OTHER_COLOR = "Other color";
	public static final Color default_A_color = new Color(151, 255, 179);
	public static final Color default_T_color = new Color(102, 211, 255);
	public static final Color default_G_color = new Color(255, 210, 0);
	public static final Color default_C_color = new Color(255, 176, 102);
	public static final Color default_other_color = Color.LIGHT_GRAY;

	private static final ColorHelper helper = new ColorHelper();
	private static final class ColorHelper implements PreferenceChangeListener {
		private static final Map<String, Color> DEFAULT_COLORS;
		private final Color[] colors;

		static {
			Map<String, Color> defaultColors = new LinkedHashMap<String, Color>();

			defaultColors.put(PREF_A_COLOR, default_A_color);
			defaultColors.put(PREF_T_COLOR, default_T_color);
			defaultColors.put(PREF_G_COLOR, default_G_color);
			defaultColors.put(PREF_C_COLOR, default_C_color);
			defaultColors.put(PREF_OTHER_COLOR, default_other_color);

			DEFAULT_COLORS = Collections.<String, Color>unmodifiableMap(defaultColors);
		}

		ColorHelper() {
			int i = 0;
			colors = new Color[5];
			PreferenceUtils.getTopNode().addPreferenceChangeListener(this);
			for (Map.Entry<String, Color> entry : DEFAULT_COLORS.entrySet()) {
				colors[i] = PreferenceUtils.getColor(PreferenceUtils.getTopNode(), entry.getKey(), entry.getValue());
				i++;
			}
		}

		public void preferenceChange(PreferenceChangeEvent evt) {
			int i = 0;
			for (Map.Entry<String, Color> entry : DEFAULT_COLORS.entrySet()) {
				if (entry.getKey().equals(evt.getKey())) {
					colors[i] = PreferenceUtils.getColor(PreferenceUtils.getTopNode(), entry.getKey(), entry.getValue());
					break;
				}
				i++;
			}
		}
	}

	public CharSeqGlyph() {
		super();
		setResidueFont(mono_default_font);
	}

	@Override
	public void setResidues(String residues) {
		setResiduesProvider(new ImprovedStringCharIter(residues), residues.length());
	}

	@Override
	public String getResidues() {
		return null;
	}

	public void setResiduesProvider(SearchableCharIterator iter, int seqlength) {
		chariter = iter;
		residue_length = seqlength;
	}

	public SearchableCharIterator getResiduesProvider() {
		return chariter;
	}

	// Essentially the same as SequenceGlyph.drawHorizontal
	@Override
	public void draw(ViewI view) {
		Rectangle2D.Double coordclipbox = view.getCoordBox();
		int visible_ref_beg, visible_seq_beg, seq_beg_index;
		visible_ref_beg = (int) coordclipbox.x;

		// determine first base displayed
		visible_seq_beg = Math.max(seq_beg, visible_ref_beg);
		seq_beg_index = visible_seq_beg - seq_beg;

		if (null != chariter && seq_beg_index <= residue_length) {
			double pixel_width_per_base = ( view.getTransform()).getScaleX();
			// ***** background already drawn in drawTraversal(), so just return if
			// ***** scale is < 1 pixel per base
			if (pixel_width_per_base < 1) {
				return;
			}
			int visible_ref_end = (int) (coordclipbox.x + coordclipbox.width);
			// adding 1 to visible ref_end to make sure base is drawn if only
			// part of it is visible
			visible_ref_end = visible_ref_end + 1;
			int visible_seq_end = Math.min(seq_end, visible_ref_end);
			int visible_seq_span = visible_seq_end - visible_seq_beg;
			// ***** otherwise semantic zooming to show more detail *****
			if (visible_seq_span > 0) {
				Rectangle2D.Double scratchrect = new Rectangle2D.Double(visible_seq_beg, coordbox.y,
						visible_seq_span, coordbox.height);
				view.transformToPixels(scratchrect, pixelbox);
				int seq_end_index = visible_seq_end - seq_beg;
				if (seq_end_index > residue_length) {
					seq_end_index = residue_length;
				}
				if (Math.abs((long) seq_end_index - (long) seq_beg_index) > 100000) {
					// something's gone wrong.  Ignore.
					Logger.getLogger(CharSeqGlyph.class.getName()).log(Level.FINE, "Invalid string: {0},{1}", new Object[]{seq_beg_index, seq_end_index});
					return;
				}
				int seq_pixel_offset = pixelbox.x;
				String str = chariter.substring(seq_beg_index, seq_end_index);
				Graphics g = view.getGraphics();
				drawHorizontalResidues(g, pixel_width_per_base, str, seq_beg_index, seq_end_index, seq_pixel_offset);
			}
		}
		super.draw(view);
	}

	/**
	 * Draw the sequence string for visible bases if possible.
	 *
	 * <p> We are showing letters regardless of the height constraints on the glyph.
	 */
	@Override
	protected void drawHorizontalResidues(Graphics g,
			double pixelsPerBase,
			String residueStr,
			int seqBegIndex,
			int seqEndIndex,
			int pixelStart) {
		char[] charArray = residueStr.toCharArray();
		drawResidueRectangles(g, pixelsPerBase, charArray, pixelbox.x, pixelbox.y, pixelbox.height);
		drawResidueStrings(g, pixelsPerBase, charArray, pixelStart);
	}

	private static void drawResidueRectangles(Graphics g, double pixelsPerBase, char[] charArray, int x, int y, int height) {
		int intPixelsPerBase = (int) Math.ceil(pixelsPerBase);
		for (int j = 0; j < charArray.length; j++) {
			g.setColor(determineResidueColor(charArray[j]));

			//Create a colored rectangle.
			//We calculate the floor of the offset as we want the offset to stay to the extreme left as possible.
			int offset = (int) (j * pixelsPerBase);
			//ceiling is done to the width because we want the width to be as wide as possible to avoid losing pixels.
			g.fillRect(x + offset, y, intPixelsPerBase, height);
		}
	}

	private static Color determineResidueColor(char charAt) {
		switch (charAt) {
			case 'A':
			case 'a':
				return helper.colors[0];
			case 'T':
			case 't':
				return helper.colors[1];
			case 'G':
			case 'g':
				return helper.colors[2];
			case 'C':
			case 'c':
				return helper.colors[3];
			default:
				return helper.colors[4];
		}
	}

	private void drawResidueStrings(Graphics g, double pixelsPerBase, char[] charArray, int pixelStart) {
		if (this.font_width <= pixelsPerBase) {
			// Ample room to draw residue letters.
			g.setFont(getResidueFont());
			g.setColor(getForegroundColor());
			int baseline = (this.pixelbox.y + (this.pixelbox.height / 2)) + this.fontmet.getAscent() / 2 - 1;
			for (int i = 0; i < charArray.length; i++) {
				g.drawChars(charArray, i, 1, pixelStart + (int) (i * pixelsPerBase), baseline);
			}
		}
	}

	
	public void setHitable(boolean h) {
		this.hitable = h;
	}

	@Override
	public boolean isHitable() {
		return hitable;
	}

	@Override
	public boolean hit(Rectangle pixel_hitbox, ViewI view) {
		if (isVisible && isHitable()) {
			calcPixels(view);
			return pixel_hitbox.intersects(pixelbox);
		} else {
			return false;
		}
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
		return isVisible && isHitable() && coord_hitbox.intersects(coordbox);
	}


	public static CharSeqGlyph initSeqGlyph(BioSeq viewSeq, Color axis_fg, AxisGlyph axis) {
		CharSeqGlyph seq_glyph = new CharSeqGlyph();
		seq_glyph.setForegroundColor(axis_fg);
		seq_glyph.setShowBackground(false);
		seq_glyph.setHitable(false);
		seq_glyph.setDrawOrder(Glyph.DRAW_CHILDREN_FIRST);
		seq_glyph.setCoords(viewSeq.getMin(), 0, viewSeq.getLengthDouble(), 10);
		seq_glyph.setResiduesProvider(viewSeq, viewSeq.getLength());
		SeqSymmetry compsym = viewSeq.getComposition();
		if (compsym != null) {
			showFillRect(viewSeq, seq_glyph, compsym, axis);
		}
		return seq_glyph;
	}

	private static void showFillRect(BioSeq viewSeq, CharSeqGlyph seqGlyph, SeqSymmetry compsym, AxisGlyph axis) {
		int compcount = compsym.getChildCount();
		// create a color, c3, in between the foreground and background colors
		Color c1 = axis.getForegroundColor();
		Color c2 = axis.getBackgroundColor();
		Color c3 = new Color((c1.getRed() + 2 * c2.getRed()) / 3, (c1.getGreen() + 2 * c2.getGreen()) / 3, (c1.getBlue() + 2 * c2.getBlue()) / 3);
		for (int i = 0; i < compcount; i++) {
			// Make glyphs for contigs
			SeqSymmetry childsym = compsym.getChild(i);
			SeqSpan childspan = childsym.getSpan(viewSeq);
			SeqSpan ospan = SeqUtils.getOtherSpan(childsym, childspan);
			GlyphI cgl;
			if (ospan.getBioSeq().isComplete(ospan.getMin(), ospan.getMax())) {
				cgl = new FillRectGlyph();
				cgl.setColor(c3);
			} else {
				if (viewSeq.getID().equals(IGBConstants.GENOME_SEQ_ID)) {
					// hide axis numbering
					axis.setLabelFormat(AxisGlyph.NO_LABELS);
					cgl = new LabelledRectGlyph();
					String text = ospan.getBioSeq().getID();
					if (text.toLowerCase().startsWith("chr")) {
						text = text.substring(3);
					}
					((LabelledRectGlyph) cgl).setText(text);
					cgl.setColor(axis.getForegroundColor());
				} else if (viewSeq.getID().equals(IGBConstants.ENCODE_REGIONS_ID)) {
					cgl = new LabelledRectGlyph();
					String text = childsym.getID();
					if (text != null) {
						((LabelledRectGlyph) cgl).setText(text);
					}
					cgl.setColor(axis.getForegroundColor());
				} else {
					cgl = new OutlineRectGlyph();
					cgl.setColor(axis.getForegroundColor());
				}
			}
			cgl.setCoords(childspan.getMinDouble(), 0, childspan.getLengthDouble(), 10);
			// also note that "Load residues in view" produces additional
			// contig-like glyphs that can partially hide these glyphs.
			seqGlyph.addChild(cgl);
		}
	}

}
