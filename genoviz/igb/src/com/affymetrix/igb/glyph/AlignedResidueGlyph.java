package com.affymetrix.igb.glyph;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genometryImpl.util.ImprovedStringCharIter;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.glyph.AbstractResiduesGlyph;
import java.util.BitSet;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

/**
 *
 * @author jnicol
 *
 * A glyph that shows a sequence of aligned residues.
 * At low resolution (small scale) as a solid background rectangle
 * and at high resolution overlays the residue letters.
 *
 * Residues can be masked out if they agree with a reference sequence.
 *
 */
public final class AlignedResidueGlyph extends AbstractResiduesGlyph
		 {//MPTAG Hier werden auf die Glyphen ATCG gezeichnet
	private SearchableCharIterator chariter;
	private int residue_length = 0;
	private final BitSet residueMask = new BitSet();
	private static final Font mono_default_font = new Font("Monospaced", Font.BOLD, 12);

	// default to true for backward compatability
	private boolean hitable = true;
	public boolean packerClip = false;	// if we're in an overlapped glyph (top of packer), don't draw residues -- for performance

	//MPTAG added
	private boolean isForward = true;

	private static final ColorHelper helper = new ColorHelper();

	public void setParentSeqStart(int beg) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public void setParentSeqEnd(int end) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getParentSeqStart() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	public int getParentSeqEnd() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	private static final class ColorHelper implements PreferenceChangeListener {
		private static final Map<String, Color> DEFAULT_COLORS;
		private final Color[] colors;

		static {
			Map<String, Color> defaultColors = new LinkedHashMap<String, Color>();

			defaultColors.put(CharSeqGlyph.PREF_A_COLOR, CharSeqGlyph.default_A_color);
			defaultColors.put(CharSeqGlyph.PREF_T_COLOR, CharSeqGlyph.default_T_color);
			defaultColors.put(CharSeqGlyph.PREF_G_COLOR, CharSeqGlyph.default_G_color);
			defaultColors.put(CharSeqGlyph.PREF_C_COLOR, CharSeqGlyph.default_C_color);
			defaultColors.put(CharSeqGlyph.PREF_OTHER_COLOR, CharSeqGlyph.default_other_color);

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

	public AlignedResidueGlyph() {
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

	/**
	 * If this is set, we will only display residues that disagree with the residue mask.
	 * This is useful for BAM visualization.
	 * @param residues
	 */
	public void setResidueMask(String residues) {
		if (residues != null && chariter != null) {
			int minResLen = Math.min(residues.length(), residue_length);
			char[] residuesArr = residues.toLowerCase().toCharArray();
			char[] displayResArr = chariter.substring(0, minResLen).toLowerCase().toCharArray();
			
			// determine which residues disagree with the reference sequence
			for(int i=0;i<minResLen;i++) {
				residueMask.set(i, displayResArr[i] != residuesArr[i]);
			}
			if (residueMask.isEmpty()) {
				// Save space and time if all residues match the reference sequence.
				residue_length = 0;
				chariter = null;
			}
		}
	}

	public void setResidueMask(byte[] SEQ) {
		char[] seqArr = new String(SEQ).toLowerCase().toCharArray();
		char[] displayResArr = chariter.substring(0, Math.min(seqArr.length, residue_length)).toLowerCase().toCharArray();
		boolean setRes = false;
		for (int i = 0; i < displayResArr.length; i++) {
			setRes = (SEQ[i] != '=') && (displayResArr[i] != seqArr[i]);
			residueMask.set(i, setRes);
		}
		if (residueMask.isEmpty()) {
			// Save space and time if all residues match the reference sequence.
			residue_length = 0;
			chariter = null;
		}
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
		if (packerClip || chariter == null) {
			return;	// don't draw residues
		}
		Rectangle2D.Double coordclipbox = view.getCoordBox();
		int visible_ref_beg, visible_seq_beg, seq_beg_index;
		visible_ref_beg = (int) coordclipbox.x;

		// determine first base displayed
		visible_seq_beg = Math.max(seq_beg, visible_ref_beg);
		seq_beg_index = visible_seq_beg - seq_beg;

		if (seq_beg_index > residue_length) {
			return;	// no residues to draw
		}
		
		double pixel_width_per_base = (view.getTransform()).getScaleX();
		if (residueMask.isEmpty() && pixel_width_per_base < 1) {
			return;	// If we're drawing all the residues, return if there's less than one pixel per base
		}
		if (pixel_width_per_base < 0.2) {
			return;	// If we're masking the residues, draw up to 5 residues at one pixel.
		}
		
		int visible_ref_end = (int) (coordclipbox.x + coordclipbox.width);
		// adding 1 to visible ref_end to make sure base is drawn if only
		// part of it is visible
		visible_ref_end = visible_ref_end + 1;
		int visible_seq_end = Math.min(seq_end, visible_ref_end);
		int visible_seq_span = visible_seq_end - visible_seq_beg;
		if (visible_seq_span > 0) {
			// ***** semantic zooming to show more detail *****
			Rectangle2D.Double scratchrect = new Rectangle2D.Double(visible_seq_beg, coordbox.y,
					visible_seq_span, coordbox.height);
			view.transformToPixels(scratchrect, pixelbox);
			int seq_end_index = visible_seq_end - seq_beg;
			if (seq_end_index > residue_length) {
				seq_end_index = residue_length;
			}
			if (Math.abs((long) seq_end_index - (long) seq_beg_index) > 100000) {
				// something's gone wrong.  Ignore.
				Logger.getLogger(CharSeqGlyph.class.getName()).fine("Invalid string: " + seq_beg_index + "," + seq_end_index);
				return;
			}
			int seq_pixel_offset = pixelbox.x;
			String str = chariter.substring(seq_beg_index, seq_end_index);
			Graphics g = view.getGraphics();
			drawHorizontalResidues(g, pixel_width_per_base, str, seq_beg_index, seq_end_index, seq_pixel_offset);
		}
		addDirectionArrow(view.getGraphics(), view);
	}

	/**
	 * Draw the sequence string for visible bases if possible.
	 *
	 * <p> We are showing letters regardless of the height constraints on the glyph.
	 */
	private void drawHorizontalResidues(Graphics g,
			double pixelsPerBase,
			String residueStr,
			int seqBegIndex,
			int seqEndIndex,
			int pixelStart) {
		char[] charArray = residueStr.toCharArray();
		drawResidueRectangles(g, pixelsPerBase, charArray, residueMask.get(seqBegIndex,seqEndIndex), pixelbox.x, pixelbox.y, pixelbox.height);
		drawResidueStrings(g, pixelsPerBase, charArray, residueMask.get(seqBegIndex,seqEndIndex), pixelStart);
		//MPTAG Prüft für jeden Buchstaben ob er passt. Reicht da nicht einmaliges Prüfen ?
	}

	private static void drawResidueRectangles(
			Graphics g, double pixelsPerBase, char[] charArray, BitSet residueMask, int x, int y, int height) {
		int intPixelsPerBase = (int) Math.ceil(pixelsPerBase);
		for (int j = 0; j < charArray.length; j++) {
			if (!residueMask.get(j)) {
				continue;	// skip drawing of this residue
			}
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

	private void drawResidueStrings(
			Graphics g, double pixelsPerBase, char[] charArray, BitSet residueMask, int pixelStart) {
		if (this.font_width <= pixelsPerBase) {
			// Ample room to draw residue letters.
			g.setFont(getResidueFont());
			g.setColor(getForegroundColor());
			int baseline = (this.pixelbox.y + (this.pixelbox.height / 2)) + this.fontmet.getAscent() / 2 - 1;
			for (int i = 0; i < charArray.length; i++) {
				if (!residueMask.get(i)) {
					continue;	// skip drawing of this residue
				}
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

	/**
	 * MPTAG
	 * Methode um an die Glyphe einen Richtungspfeil anzuhängen
	 *
	 */
	private void addDirectionArrow(Graphics g, ViewI view){
		int numPoints = 3;
		int arrowWidth = 5;
		arrowWidth =  (int) (3 * (view.getTransform()).getScaleX());
		int[] xpts = new int[numPoints];
		int[] ypts = new int[numPoints];
		if(isForward){
			//Pfeil nach Rechts
			xpts[0] = pixelbox.x+pixelbox.width; //Ecke oben an der Glyphe
			ypts[0] = pixelbox.y;
			xpts[1] = pixelbox.x+pixelbox.width; //Ecke unten an der Glyphe
			ypts[1] = pixelbox.y + pixelbox.height;
			xpts[2] = pixelbox.x+pixelbox.width + arrowWidth; //Spitze des Pfeils
			ypts[2] = pixelbox.y + (pixelbox.height / 2);
		}else{
			//Pfeil nach links
			xpts[0] = pixelbox.x; //Ecke oben an der Glyphe
			ypts[0] = pixelbox.y;
			xpts[1] = pixelbox.x; //Ecke unten an der Glyphe
			ypts[1] = pixelbox.y + pixelbox.height;
			xpts[2] = pixelbox.x - arrowWidth; //Spitze des Pfeils
			ypts[2] = pixelbox.y + (pixelbox.height / 2);
		}
		Color c = g.getColor();
		g.setColor(Color.RED);
		g.fillPolygon(xpts, ypts, numPoints);
		g.setColor(c);
	}

	/**
	 * MPTAG
	 * Methode zum setzen der Richtung der Glyphe um den Pfeil hinzuzufügen
	 * @param isForward true wenn das Parenttier forward ist, false wenn nicht
	 */
	public void setDirection(boolean isForward){
		this.isForward = isForward;
	}
}
