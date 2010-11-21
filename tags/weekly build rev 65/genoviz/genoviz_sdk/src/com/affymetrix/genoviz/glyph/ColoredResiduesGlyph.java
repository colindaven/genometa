package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.util.GeneralUtils;
import java.awt.Color;
import java.awt.Graphics;

/**
 * Draws colored rectangles and corresponding character based upon nucleotide.
 * @see     com.affymetrix.genoviz.glyph.SequenceGlyph
 */
public class ColoredResiduesGlyph extends SequenceGlyph {

    public static final Color default_A_color = Color.GREEN;
    public static final Color default_T_color = Color.PINK;
    public static final Color default_G_color = Color.YELLOW;
    public static final Color default_C_color = Color.CYAN;

    /**
     * Draws colored rectangles and corresponding character based upon nucleotide.
     * @param   g               Graphic object for drawing.
     * @param   pixelsPerBase   Perpixel size based upon zooming level.
     * @param   residues        String residue of the nucleotides.
     * @param   seqBegIndex     Starting position of visible nucleotide.
     * @param   seqEndIndex     End position of visible nucleotide.
     * @param   pixelStart      Horizontal starting position from where string should be drawn.
     */
    @Override
    protected void drawHorizontalResidues(
            Graphics g,
            double pixelsPerBase,
            String residues,
            int seqBegIndex,
            int seqEndIndex,
            int pixelStart) {
        int baseline = (this.pixelbox.y + (this.pixelbox.height / 2)) + this.fontmet.getAscent() / 2 - 1;
        String str = residues.substring(seqBegIndex, seqEndIndex);

        if (this.font_width <= pixelsPerBase) {
            drawResidueRectangles(g, pixelsPerBase, str);
            drawResidueStrings(g, pixelsPerBase, str, pixelStart, baseline);
        }
    }

    /**
     * Draw colored rectangles based upon nucleotide.
     * @param   g               Graphic object for drawing.
     * @param   pixelsPerBase   Perpixel size based upon zooming level.
     * @param   str             Visible nucleotides string.
     */
    private void drawResidueRectangles(Graphics g, double pixelsPerBase, String str) {
        for (int j = 0; j < str.length(); j++) {
            if (str.charAt(j) == 'A') {
                g.setColor(default_A_color);
            } else if (str.charAt(j) == 'T') {
                g.setColor(default_T_color);
            } else if (str.charAt(j) == 'G') {
                g.setColor(default_G_color);
            } else if (str.charAt(j) == 'C') {
                g.setColor(default_C_color);
            }
            if (str.charAt(j) == 'A' || str.charAt(j) == 'T' || str.charAt(j) == 'G' || str.charAt(j) == 'C') {
                //We calculate the floor of the offset as we want the offset to stay to the extreme left as possible.
                int offset = (int) (j * pixelsPerBase);
                //ceiling is done to the width because we want the width to be as wide as possible to avoid losing pixels.
                g.fillRect(pixelbox.x + offset, pixelbox.y, (int) Math.ceil(pixelsPerBase), pixelbox.height);
            }
        }
    }

    /**
     * Draw character of the nucleotide.
     * @param   g               Graphic object for drawing.
     * @param   pixelsPerBase   Perpixel size based upon zooming level.
     * @param   str             Visible nucleotides string.
     * @param   pixelStart      Horizontal starting position from where string should be drawn.
     * @param   baseline        Vertical starting position where string should be drawn.
     * @see     com.affymetrix.genoviz.util.GeneralUtils
     */
    private void drawResidueStrings(Graphics g, double pixelsPerBase, String str, int pixelStart, int baseline) {
        g.setFont(getResidueFont());
        g.setColor(getForegroundColor());
        fontmet = GeneralUtils.getFontMetrics(getResidueFont());
        // Ample room to draw residue letters.
        for (int i = 0; i < str.length(); i++) {
            String c = String.valueOf(str.charAt(i));
            if (c != null) {
                g.drawString(c, pixelStart + (int) (i * pixelsPerBase), baseline);
            }
        }
    }
}
