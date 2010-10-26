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

package com.affymetrix.genoviz.widget.neotracer;

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.datamodel.TraceI;
import com.affymetrix.genoviz.datamodel.TraceSample;
import java.awt.*;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class TraceGlyph extends Glyph  {
	public static final int A = 0;
	public static final int C = 1;
	public static final int G = 2;
	public static final int T = 3;
	public static final int N = 4;

	// one extra for N, to agree with TraceBaseGlyph, even though shouldn't be needed
	//  for TraceGlyph
	protected boolean showTrace[] = { true, true, true, true, true };
	protected TraceI trace;
	protected int dataCount, baseCount;
	protected double[] traceA, traceC, traceG, traceT;
	protected double[][] allTraces;

	protected static Color traceColor[] = {
		Color.green, Color.cyan, Color.yellow, Color.red,
		Color.white,    // unknown base color
		Color.gray, // selection color
	};

	protected static Color selColor = traceColor[traceColor.length-1];

	protected Point prevPixelPoint = new Point(0,0);
	protected Point currPixelPoint = new Point(0,0);
	protected Point basePixelPoint = new Point(0,0);

	protected Point2D.Double prevCoordPoint = new Point2D.Double(0,0);
	protected Point2D.Double currCoordPoint = new Point2D.Double(0,0);
	protected Point2D.Double baseCoordPoint = new Point2D.Double(0,0);

	/*
	   pixels_per_base is an _average_ over the whole trace
	   this is needed to determine a consistent number of
	   bases to add numbers to on the axis for a given resolution
	   Alternative (which was used at first) is to calculate this from the
	   actual number of bases shown in the view, but since this number can
	   change at a given scale with different translations, it can lead to
	   numbers flickering in and out as the user scrolls along the trace.
	   */
	protected double pixels_per_base, pixels_per_coord, coords_per_base;

	protected GlyphI sel_glyph;
	protected Color sel_color = selColor;

	public TraceGlyph() {
		this.setDrawOrder(DRAW_CHILDREN_FIRST);
	}

	public TraceGlyph(TraceI trace) {
		this();
		setTrace(trace);
	}

	public void setTrace(TraceI trace) {
		this.trace = trace;
		dataCount = trace.getTraceLength();
		baseCount = trace.getBaseCount();
		clearSelection();

		allTraces = new double[4][];
		allTraces[0] = traceA = new double[dataCount];
		allTraces[1] = traceC = new double[dataCount];
		allTraces[2] = traceG = new double[dataCount];
		allTraces[3] = traceT = new double[dataCount];

		TraceSample samp;
		for (int i=0; i<dataCount; i++) {
			samp = trace.sampleAt(i);
			traceA[i] = (double)samp.getSampleA();
			traceC[i] = (double)samp.getSampleC();
			traceG[i] = (double)samp.getSampleG();
			traceT[i] = (double)samp.getSampleT();
		}
	}

	@Override
	public void draw(ViewI view) {
		int beg, end;
		double[] traceArray;
		Graphics g = view.getGraphics();
		Rectangle2D.Double viewbox = view.getCoordBox();

		pixels_per_coord = (view.getTransform()).getScaleX();
		coords_per_base = (double)dataCount/(double)baseCount;
		pixels_per_base = pixels_per_coord * coords_per_base;

		// extending beg and end +/-1 so that connection to next point outside
		// view get drawn -- this is needed for optimized scrolling, otherwise get
		// vertical "holes" in the trace
		beg = (int)viewbox.x - 1;
		end = (int)(viewbox.x + viewbox.width) + 1;
		if (end < coordbox.x ) { end++; }
		if (end >= dataCount + coordbox.x) { end = dataCount + (int)coordbox.x - 1; }
		if (beg < coordbox.x ) beg = (int)coordbox.x;
		if (end > coordbox.x + dataCount - 1) end = (int)coordbox.x + dataCount - 1;

		for (int i=0; i<=3; i++) {
			if (!showTrace[i]) {
				continue;
			}
			traceArray = allTraces[i];
			g.setColor(traceColor[i]);
			prevCoordPoint.x = beg;
			prevCoordPoint.y = coordbox.height - traceArray[beg - (int)coordbox.x];
			prevPixelPoint = view.transformToPixels(prevCoordPoint, prevPixelPoint);
			// drawing curves along every point j for the given trace i
			for (int j=beg+1; j<=end; j++) {
				currCoordPoint.x = j;
				currCoordPoint.y = coordbox.height - traceArray[j - (int)coordbox.x];
				currPixelPoint =
					view.transformToPixels(currCoordPoint, currPixelPoint);

				// hack to get trace offset from bases & positions
				g.drawLine(prevPixelPoint.x, prevPixelPoint.y - 2,
						currPixelPoint.x, currPixelPoint.y - 2);

				prevPixelPoint.x = currPixelPoint.x;
				prevPixelPoint.y = currPixelPoint.y;
			}
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

	public void setTraceColors(Color[] colors) {
		traceColor = colors;
		this.setSelectionColor(colors[colors.length-1]);
	}

	public void setTraceColors(Color aColor, Color cColor,
			Color gColor, Color tColor, Color nColor) {
		traceColor[0] = aColor;
		traceColor[1] = cColor;
		traceColor[2] = gColor;
		traceColor[3] = tColor;
		traceColor[4] = nColor;
	}

	public void clearSelection() {
		if (sel_glyph != null) {
			removeChild(sel_glyph);
			sel_glyph = null;
		}
	}

	/**
	 * set the color to use to highlight selected areas of the trace.
	 */
	public void setSelectionColor( Color c ) {
		TraceGlyph.selColor = c;
		this.sel_color = c;
		if (sel_glyph != null) {
			sel_glyph.setColor(sel_color);
		}
	}

	public Color getSelectionColor() {
		return this.sel_color;
	}

	public void select(int base) {
		this.select(base, base);
	}

	public void deselect(int base) {
		this.deselect(base, base);
	}

	@Override
	public void select(double x, double y, double width, double height) {
		select(x, x+width);
	}

	public void select (double start, double end) {
		select((int)start, (int)end);
	}

	public void select(int start, int end) {
		if (sel_glyph == null) {
			sel_glyph = new FillRectGlyph();
			sel_glyph.setColor(sel_color);
			addChild(sel_glyph, 0);
		}
		Rectangle2D.Double cb = getCoordBox();
		sel_glyph.setCoords(start,cb.y,end-start + 1,cb.height);
	}


	public GlyphI getSelectionGlyph() {
		return sel_glyph;
	}

	//  selection is inclusive of begbase and endbase
	public void deselect(int begbase, int endbase) {

	}

	@Override
	public boolean supportsSubSelection() {
		return true;
	}

	@Override
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

	public void setVisibility(int traceID, boolean visible) {
		showTrace[traceID] = visible;
	}

	public boolean getVisibility(int traceID) {
		return showTrace[traceID];
	}

}
