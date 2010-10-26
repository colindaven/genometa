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
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.datamodel.BaseCall;
import com.affymetrix.genoviz.datamodel.BaseCalls;
import com.affymetrix.genoviz.datamodel.NullBaseCalls;
import com.affymetrix.genoviz.datamodel.TraceI;
import java.awt.*;
import java.awt.geom.Point2D;
import com.affymetrix.genoviz.glyph.OutlineRectGlyph;
import com.affymetrix.genoviz.util.GeneralUtils;
import java.awt.geom.Rectangle2D;

/**
 * a glyph specialized for displaying called bases along a chromatogram.
 */
public class TraceBaseGlyph extends Glyph  {

	public static final int A = TraceGlyph.A;
	public static final int C = TraceGlyph.C;
	public static final int G = TraceGlyph.G;
	public static final int T = TraceGlyph.T;
	public static final int N = TraceGlyph.N;

	protected boolean showBase[] = { true, true, true, true, true };
	private BaseCalls base_calls;  // datamodel

	protected int dataCount, baseCount;

	private static final String baseString[] = { "A", "C", "G", "T", "-" };
	private static Color baseColor[] = {
		Color.green, Color.cyan, Color.yellow, Color.red,
		Color.white,    // unknown base color
		Color.lightGray // selection color
	};
	private static Color selColor = baseColor[baseColor.length-1];
	private static final Font fnt = new Font("Helvetica", Font.PLAIN, 12);
	private static final FontMetrics fntmet = GeneralUtils.getFontMetrics(fnt);
	private static final int fntWidth = fntmet.charWidth('C');
	//private static final int fntXOffset = fntWidth/2;
	private static final int fntHeight = fntmet.getHeight();
	private static final int letterBaseline = fntmet.getLeading() + fntmet.getAscent();

	private static final int letterHeight = fntHeight;
	//private static final int letterSpacer = 3;
	//private static final int tickHeight = 7;
	//private static final int tickSpacer = 2;

	//private static final int tickOffset = letterBaseline + letterSpacer;

	private static final int baseGlyphHeight = letterHeight;

	protected Point prevPixelPoint = new Point(0,0);
	protected Point currPixelPoint = new Point(0,0);
	protected Point basePixelPoint = new Point(0,0);
	protected Point selPixelPoint = new Point(0,0);

	protected Point2D.Double prevCoordPoint = new Point2D.Double(0,0);
	protected Point2D.Double currCoordPoint = new Point2D.Double(0,0);
	protected Point2D.Double baseCoordPoint = new Point2D.Double(0,0);
	protected Point2D.Double selCoordPoint = new Point2D.Double(0,0);

	protected Rectangle2D.Double labelCoords = new Rectangle2D.Double();
	protected Rectangle labelPixels = new Rectangle();

	protected OutlineRectGlyph sel_glyph;
	protected static Color sel_color = selColor;

	String bString = baseString[4]; // default to -
	Color currBaseColor = baseColor[4]; // default to white
	String numString;
	boolean showBases = true;

	public TraceBaseGlyph() {
		this.setDrawOrder(DRAW_CHILDREN_FIRST);
		setHeight( baseGlyphHeight );
		setBaseCalls( new NullBaseCalls() );
	}

	public TraceBaseGlyph(TraceI trace) {
		this();
		setTrace(trace);
	}

	public TraceBaseGlyph(BaseCalls base_calls) {
		this();
		setBaseCalls(base_calls);
	}

	public void setTrace(TraceI trace) {
		dataCount = trace.getTraceLength();
		baseCount = trace.getBaseCount();
		clearSelection();
	}

	public void setBaseCalls( BaseCalls theCalls ) {
		this.base_calls = theCalls;
		this.baseCount = theCalls.getBaseCount();
	}

	public BaseCalls getBaseCalls() {
		return base_calls;
	}

	protected void setHeight( int height ) {
		Rectangle2D.Double old_rect = getCoordBox();
		setCoords( old_rect.x, old_rect.y, old_rect.width, height );
	}
	public int getHeight() {
		return (int)(this.getCoordBox().height);
	}

	@Override
	public void draw(ViewI view) {
		int beg, end;
		Graphics g = view.getGraphics();
		Rectangle2D.Double viewbox = view.getCoordBox();

		double pixels_per_coord = (view.getTransform()).getScaleX();
		double coords_per_base = (double)dataCount/(double)baseCount;
		double pixels_per_base = pixels_per_coord * coords_per_base;

		beg = (int)viewbox.x;
		end = (int)(viewbox.x + viewbox.width);
		if (end < coordbox.x ) { end++; }
		if (beg < 0) { beg = 0; }
		if ( beg < coordbox.x ) beg = (int)coordbox.x;

		char theBase;
		int baseID;

		double minview = viewbox.x;
		double maxview = viewbox.x + viewbox.width;
		// GAH 12-2-97
		// Need to expand minview and maxview so they'll encompass any piece of
		// a base string or number that falls within the view!
		// Since base and number draws are pixel based, can currently have a base
		// or number whose coordinate falls outside of viewbox but that should be
		// drawn anyway since the backconverted coordinates of its _pixel_ bounds
		// overlaps the view.
		//
		// This wasn't really a problem when doing full redraws with every
		// View.draw() call, but is a big problem when doing scrolling
		// optimizations
		//
		// Base character width will always be a subset of potential number label
		// width.  Therefore Expand by calculating coordinate width of number label
		// from pixel width found through fontmetrics (assumes at the moment
		// that number font and base font are same, and that this font
		// is monospaced

		// just assume maximum number width,
		// assuming traces will be < 10000 bases long
		labelPixels.width = fntWidth*4;
		labelPixels.height = fntHeight;
		view.transformToCoords(labelPixels, labelCoords);
		// Giving plenty of room, could definitely optimize this more
		minview -= labelCoords.width;
		maxview += labelCoords.width;

		boolean bases_within_view = false;

		// don't worry about setting default for firstbase/lastbase to nonsense --
		// if they aren't set in loop, they won't be used anyway
		//    firstbase = lastbase = Integer.MIN_VALUE;

		g.setFont(fnt);

		showBases = ( 10 < pixels_per_base );

		BaseCall calledBase;
		try {
			for (int i=0; i<baseCount; i++) {
				if ((calledBase = this.base_calls.getBaseCall(i)) != null) {
					baseCoordPoint.x = calledBase.getTracePoint() + coordbox.x;
					if ((baseCoordPoint.x >= minview) && (baseCoordPoint.x <= maxview))  {
						if (!bases_within_view) {
							bases_within_view = true;
						}

						if (showBases) {
							baseCoordPoint.y = coordbox.y;
							basePixelPoint =
								view.transformToPixels(baseCoordPoint, basePixelPoint);
							basePixelPoint.x -= fntWidth/2;

							theBase = calledBase.getBase();

							if      (theBase == 'A' || theBase == 'a')  { baseID = 0; }
							else if (theBase == 'C' || theBase == 'c')  { baseID = 1; }
							else if (theBase == 'G' || theBase == 'g')  { baseID = 2; }
							else if (theBase == 'T' || theBase == 't')  { baseID = 3; }
							else                      { baseID = 4; }
							if (!showBase[baseID]) {
								continue;
							}
							bString = baseString[baseID];
							currBaseColor = baseColor[baseID];

							g.setColor(currBaseColor);
							g.drawString( bString, basePixelPoint.x,
									basePixelPoint.y + letterBaseline );
						}
					}
				}
			}
		}
		catch ( ArrayIndexOutOfBoundsException e ) {
			//    System.err.println( "TraceBaseGlyph.draw: baseCount too big: " + baseCount );
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

	public void clearSelection() {
		if (sel_glyph != null) {
			removeChild(sel_glyph);
			sel_glyph = null;
		}
	}

	public void select(int base) {
		this.select(base, base);
	}

	/**
	 * @see #deselect(int,int)
	 */
	public void deselect(int base) {
		this.deselect(base, base);
	}

	/**
	 * highlights a portion of this glyph.
	 * @param x leftmost point.
	 * @param y ignored
	 * @param width
	 * @param height ignored
	 */
	@Override
	public void select(double x, double y, double width, double height) {
		select(x, x+width);
	}

	/**
	 * highlights a portion of this glyph.
	 */
	public void select (double start, double end) {
		select((int)start, (int)end);
	}

	/**
	 * highlights all the bases between start and end, inclusive.
	 * @param start index of first base to highlight.
	 * @param end index of last base to highlight.
	 */
	public void select(int start, int end) {
		if (sel_glyph == null) {
			sel_glyph = new OutlineRectGlyph();
			sel_glyph.setForegroundColor(sel_color);
			addChild(sel_glyph, 0);
		}
		Rectangle2D.Double cb = getCoordBox();
		// Why are we adding to cb.y? Why was 5 added to cb.y and cb.height? elb 1999-12-02
		sel_glyph.setCoords( start, cb.y, end-start + 1, cb.height );
	}

	/**
	 * does nothing.
	 * @see #clearSelection
	 */
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

	public void setVisibility(int baseID, boolean visible) {
		showBase[baseID] = visible;
	}

	public boolean getVisibility(int baseID) {
		return showBase[baseID];
	}

	public void setBaseColors(Color[] colors) {
		baseColor = colors;
		selColor = colors[colors.length-1];
		sel_color = selColor;
		if (sel_glyph != null)  {
			sel_glyph.setForegroundColor(sel_color);
		}
	}

}
