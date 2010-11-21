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
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.datamodel.BaseConfidence;
import com.affymetrix.genoviz.datamodel.ReadConfidence;
import com.affymetrix.genoviz.glyph.OutlineRectGlyph;
import com.affymetrix.genoviz.util.GeneralUtils;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public class QualityBases extends Glyph  {
	protected ReadConfidence read_conf;

	private static final String baseString[] = { "A", "C", "G", "T", "-" };
	private static final Color baseColor[] = { Color.green, Color.cyan,
		Color.yellow, Color.red, Color.white };
	private static final Color numColor = Color.lightGray;
	private static final Font fnt = new Font("Helvetica", Font.BOLD, 12);
	private static final FontMetrics fntmet = GeneralUtils.getFontMetrics(fnt);
	//private static final int fntWidth = fntmet.charWidth('C');
	//private static final int fntXOffset = fntWidth/2;
	private static final int fntHeight = fntmet.getHeight();

	private static final int topSpacer = 1;
	private static final int letterHeight = fntHeight;
	private static final int letterSpacer = 3;
	private static final int tickHeight = 7;
	private static final int tickSpacer = 2;
	private static final int numHeight = fntHeight;
	private static final int numSpacer = 4;

	//private static final int letterOffset = topSpacer;
	private static final int letterBaseline = topSpacer + letterHeight;
	private static final int tickOffset = letterBaseline + letterSpacer;
	private static final int numOffset = tickOffset + tickHeight + tickSpacer;
	private static final int numBaseline = numOffset + numHeight;

	public static final int baseGlyphHeight = (
			topSpacer +
			letterHeight +
			letterSpacer +
			tickHeight +
			tickSpacer +
			numHeight +
			numSpacer
			);

	protected Point prevPixelPoint = new Point(0,0);
	protected Point currPixelPoint = new Point(0,0);
	protected Point basePixelPoint = new Point(0,0);
	protected Point selPixelPoint = new Point(0,0);

	protected Point2D.Double prevCoordPoint = new Point2D.Double(0,0);
	protected Point2D.Double currCoordPoint = new Point2D.Double(0,0);
	protected Point2D.Double baseCoordPoint = new Point2D.Double(0,0);
	protected Point2D.Double selCoordPoint = new Point2D.Double(0,0);

	protected int read_length;
	protected String bString = baseString[4]; // default to -
	protected Color currBaseColor = baseColor[4]; // default to white
	protected String numString;
	protected boolean showBases = true;
	protected BaseConfidence calledBase;
	protected BaseConfidence nextBase;

	protected GlyphI sel_glyph;
	private static final Color sel_color = Color.white;

	public QualityBases(ReadConfidence read_conf) {
		this.setDrawOrder(DRAW_CHILDREN_FIRST);
		setReadConfidence(read_conf);
	}

	public void setReadConfidence(ReadConfidence read_conf) {
		this.read_conf = read_conf;
		read_length = read_conf.getReadLength();
		clearSelection();
	}

	@Override
	public void draw(ViewI view) {
		int beg, end, i;
		Graphics g = view.getGraphics();
		Rectangle2D.Double viewbox = view.getCoordBox();

		beg = (int)viewbox.x;
		end = (int)(viewbox.x + viewbox.width);
		if (end < coordbox.x ) { end++; }
		if (end >= read_length) { end = read_length - 1; }
		if (beg < 0) { beg = 0; }

		char theBase;
		double minview = viewbox.x;
		double maxview = viewbox.x + viewbox.width;
		boolean firstdisplayed = true;
		int firstbase, lastbase;
		firstbase = lastbase = Integer.MIN_VALUE;
		g.setFont(fnt);

		double avgBasesDrawn = viewbox.width;
		int increment;
		if  (avgBasesDrawn < 20) { increment = 2; }
		else if  (avgBasesDrawn < 50) { increment = 5; }
		else if  (avgBasesDrawn < 100) { increment = 10; }
		else if  (avgBasesDrawn < 300) { increment = 20; }
		else { increment = 50; }

		if ( increment > 10 ) {
			showBases = false;
		}
		else {
			showBases = true;
		}

		Rectangle2D.Double visible_box = ((View)view).calcCoordBox();
		for (i=(int)visible_box.x; i<(int)(visible_box.x+visible_box.width); i++) {
			if (i>= read_length) {
				break;
			}
			if ((calledBase = read_conf.getBaseConfidenceAt(i)) != null) {
				baseCoordPoint.x = i;
				if ((baseCoordPoint.x >= minview) && (baseCoordPoint.x <= maxview))  {
					if (firstdisplayed) {
						firstbase = i;
						firstdisplayed = false;
					}
					lastbase = i;
					baseCoordPoint.y = coordbox.y;
					basePixelPoint =
						view.transformToPixels(baseCoordPoint, basePixelPoint);
					theBase = calledBase.getBase();

					if (theBase == 'A' || theBase == 'a') {
						bString = baseString[0];
						currBaseColor = baseColor[0];
					}
					else if (theBase == 'C' || theBase == 'c') {
						bString = baseString[1];
						currBaseColor = baseColor[1];
					}
					else if (theBase == 'G' || theBase == 'g') {
						bString = baseString[2];
						currBaseColor = baseColor[2];
					}
					else if (theBase == 'T' || theBase == 't') {
						bString = baseString[3];
						currBaseColor = baseColor[3];
					}
					else {
						bString = baseString[4];
						currBaseColor = baseColor[4];
					}

					if (showBases) {
						g.setColor(currBaseColor);
						g.drawString( bString, basePixelPoint.x,
								basePixelPoint.y + letterBaseline );
					}
				}
			}
		}

		// Drawing base numbers along the axis

		if (firstbase%increment != 0) {
			firstbase = firstbase + increment - firstbase%increment;
		}
		g.setColor(numColor);
		for (i=firstbase; i<=lastbase; i+=increment) {
			baseCoordPoint.x = i;
			baseCoordPoint.y = coordbox.y;
			basePixelPoint =
				view.transformToPixels(baseCoordPoint, basePixelPoint);

			// a little embellishment (need to get rid of hardwiring though)
			g.drawLine( basePixelPoint.x+3,
					basePixelPoint.y + tickOffset,
					basePixelPoint.x+3,
					basePixelPoint.y + tickOffset + tickHeight) ;

			g.drawString( String.valueOf(i), basePixelPoint.x,
					basePixelPoint.y + numBaseline );
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

	public void deselect(int base) {
		this.deselect(base, base);
	}

	//  selection is inclusive of start and end
	public void select(int start, int end) {
		if (sel_glyph == null) {
			sel_glyph = new OutlineRectGlyph();
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
