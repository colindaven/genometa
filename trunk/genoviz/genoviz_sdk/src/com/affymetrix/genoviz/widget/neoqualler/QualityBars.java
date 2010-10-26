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

package com.affymetrix.genoviz.widget.neoqualler;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.datamodel.ReadConfidence;
import com.affymetrix.genoviz.glyph.ColorSepGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import java.awt.*;

import java.awt.geom.Rectangle2D;

public class QualityBars extends Glyph  {

	protected ReadConfidence read_conf;
	protected int read_length;
	protected char[] baseArray;
	protected int[] qualArray;
	protected int maxValue;

	protected ColorSepGlyph qual_glyph;

	protected GlyphI sel_glyph;
	private static final Color sel_color = Color.gray;

	protected static Color qualColor[] = {
		Color.green,
		Color.cyan,
		Color.yellow,
		Color.red,
		Color.white };

	private static final char baseString[] = { 'A', 'C', 'G', 'T', '-' };

	public QualityBars(ReadConfidence read_conf) {

		this.read_conf = read_conf;
		baseArray = read_conf.getBaseArray();
		qualArray = read_conf.getQualArray();
		read_length = read_conf.getReadLength();
		maxValue = read_conf.getMaxValue();

		double glyph_width = read_length;
		double glyph_height = maxValue + 10 - (maxValue + 1) % 10;
		this.setCoords(0, 0, glyph_width, glyph_height);

		qual_glyph = new ColorSepGlyph();
		qual_glyph.setColorArray(qualColor);
		qual_glyph.setCoords(0, 0, glyph_width, glyph_height);
		this.addChild(qual_glyph);

		addQualityBars();
	}

	public void addQualityBars() {
		Glyph glyph;
		double height = getCoordBox().height;

		for (int i=0; i < read_length ; i++) {
			glyph = new QualityRect();
			if (null != baseArray) {
				glyph.setBackgroundColor(getQualityColor(baseArray[i]));
			}
			else {
				glyph.setBackgroundColor(getQualityColor(' '));
			}
			glyph.setCoords(i, height - qualArray[i], 1, qualArray[i]);
			qual_glyph.addChild(glyph);
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

	public void setQualityColors(Color[] colors) {
		qualColor = colors;
	}

	public void setQualityColors(Color aColor, Color cColor,
			Color gColor, Color tColor, Color nColor) {
		qualColor[0] = aColor;
		qualColor[1] = cColor;
		qualColor[2] = gColor;
		qualColor[3] = tColor;
		qualColor[4] = nColor;
	}

	public Color getQualityColor(char base) {
		for (int i=0; i<baseString.length ;i++) {
			if ( Character.toUpperCase(base) == baseString[i] ){
				return qualColor[i];
			}
		}
		return Color.white;
	}

	public void clearSelection() {
		if (sel_glyph != null) {
			removeChild(sel_glyph);
			sel_glyph = null;
		}
	}

	public void select(int base) {
		this.select(base, base);
	}

	public void deselect(int base) {
		this.deselect(base, base);
	}

	//  selection is inclusive of start and end
	public void select(int start, int end) {
		if (sel_glyph == null) {
			sel_glyph = new FillRectGlyph();
			sel_glyph.setColor(sel_color);
			addChild(sel_glyph, 0);
		}
		Rectangle2D.Double cb = getCoordBox();
		sel_glyph.setCoords(start,cb.y,end-start + 1,cb.height);
	}

	//  selection is inclusive of begbase and endbase
	public void deselect(int begbase, int endbase) {
	}

}
