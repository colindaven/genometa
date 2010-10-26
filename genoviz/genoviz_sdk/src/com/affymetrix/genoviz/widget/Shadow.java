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

package com.affymetrix.genoviz.widget;

import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import com.affymetrix.genoviz.glyph.StringGlyph;

import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.FontMetrics;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.geom.Rectangle2D;

/**
 * A shadow cast on a map to help line things up.
 * It can be used to monitor the visible range on another map.
 * It can also be used to indicate a zoom focus.
 *
 * @version $Id: Shadow.java 6305 2010-06-30 14:57:09Z hiralv $
 */
public class Shadow implements NeoRangeListener, NeoViewBoxListener {

	private final TransientGlyph tg = new TransientGlyph();
	private final FillRectGlyph vGlyph = new FillRectGlyph();

	private NeoMap map;
	private final static double topMargin = 0.0;  // margins between viewable area and the shadow
	private final static double bottomMargin = 0.0;
	private int orientation;
	private boolean labeled=false;  // should we draw a label?
	private boolean labelForReverse = false;
	private long residueCount;
	private static final java.text.DecimalFormat dform = new java.text.DecimalFormat("#,###.##");
	private final Rectangle2D.Double px = new Rectangle2D.Double();  // used for coordinate conversions
	private final Rectangle tx = new Rectangle(); // used for label text width and height
	private int current_range_st = 0;
	private int current_range_en = 0;

	/**
	 * When labeled=true, access the label's Glyph through this variable
	 * for setting the label font, text color, etc.
	 * Note that if useXOR is true, the color shown will actually be a
	 * non-obvious function of the color you specify.
	 *
	 * @see #setLabeled
	 * @see #setUseXOR
	 */
	public final StringGlyph label=new StringGlyph("");

	// This rectangle hides the area where the label and the vGlyph intersect
	private final FillRectGlyph extraRect = new FillRectGlyph();

	/**
	 * Creates a light gray Shadow on the given (HORIZONTAL) map.
	 *
	 * @param destination the map on which the shadow is cast.
	 */
	public Shadow( NeoMap destination ) {
		this( destination, NeoConstants.HORIZONTAL );
	}

	/**
	 * Creates a light gray Shadow on the given map.
	 *
	 * @param destination the map on which the shadow is cast.
	 * @param theOrientation of the map (HORIZONTAL or VERTICAL).
	 */
	public Shadow( NeoMap destination, int theOrientation ) {
		this( destination, theOrientation, Color.lightGray );
	}

	/**
	 * Creates a Shadow on the given map.
	 * It would be better if the orientation could be inferred
	 * from the orientation of the map.
	 * But, alas, there is not yet a way to get the map's orientation.
	 *
	 * @param destination the map on which the shadow is cast.
	 * @param theOrientation of the map (HORIZONTAL or VERTICAL).
	 * @param theColor of the visible glyph.
	 * @see NeoConstants#HORIZONTAL
	 * @see NeoConstants#VERTICAL
	 */
	public Shadow( NeoMap destination, int theOrientation, Color theColor ) {
		resetShadow(destination, theOrientation, theColor);
	}

	public void resetShadow(NeoMap destination, int theOrientation, Color theColor)  {
		if (Shadow.topMargin + Shadow.bottomMargin >= 1.0) {
			System.out.println("Margins too large." );
		}

		this.map = destination;

		// Start out with the TransientGlyph the full size of the map!
		Rectangle2D.Double sbox = this.map.getScene().getCoordBox();
		this.tg.setCoords( sbox.x, sbox.y, sbox.width, sbox.height );
		this.map.getScene().addGlyph( this.tg );

		// Set up a visible glyph.
		double ourX, ourY, ourWidth, ourHeight;
		int[] offset = map.getVisibleOffset();
		switch ( theOrientation ) {
			case NeoConstants.HORIZONTAL:
				ourX = sbox.x;
				ourWidth = 1;
				ourY = sbox.y + ( sbox.height * Shadow.topMargin );
				ourHeight = sbox.height * (1.0 - Shadow.topMargin - Shadow.bottomMargin );

				break;
			case NeoConstants.VERTICAL:
				ourY = sbox.y;
				ourHeight = 1;
				ourX = sbox.x + ( sbox.width * Shadow.topMargin );
				ourWidth = sbox.width * (1.0 - Shadow.topMargin - Shadow.bottomMargin );

				break;
			default:
				throw new IllegalArgumentException( "The orientation must be HORIZONTAL or VERTICAL." );
		}
		this.orientation = theOrientation;


		this.vGlyph.setCoords( ourX, ourY, ourWidth, ourHeight );
		if ( null == theColor ) {
			this.vGlyph.setBackgroundColor( Color.lightGray );
		}
		else {
			this.vGlyph.setBackgroundColor( theColor );
		}

		// Add the visible glyph and extra glyph as children of the transient glyph.

		this.map.addItem( this.tg, vGlyph );
		this.tg.setSelectable(false);
		this.setSelectable(false);
		this.vGlyph.setHitable(false);
		this.label.setHitable(false);
		this.extraRect.setHitable(false);
		this.extraRect.setSelectable(false);
		setLabeled(false);
		if (map.getView() != null) {
			map.getView().addPreDrawViewListener(this);
		}
		else this.map.addViewBoxListener( this );
	}


	/**
	 * When labeled=true, a text label is added to the bottom of the Shadow
	 * containing a number indicating the location of the Shadow along
	 * the primary axis.
	 * Note that the range and offset of the map being shadowed must
	 * have been initialized before this method is called.
	 */
	public void setLabeled(boolean labeled) {
		if (this.labeled == labeled) return; // already in the requested state.

		this.labeled = labeled;
		if (labeled) {
			extraRect.setBackgroundColor(vGlyph.getBackgroundColor());
			extraRect.setSelectable(false);
			label.setBackgroundColor(vGlyph.getBackgroundColor());
			label.setForegroundColor ( Color.black );
			label.setShowBackground(true);
			label.setPlacement(NeoConstants.CENTER);
			label.setSelectable(false);

			this.setRange(current_range_st, current_range_en);

			map.addItem( this.tg, extraRect);
			map.addItem( this.tg, label);
		}
		else {
			map.removeItem(extraRect);
			map.removeItem(label);
		}
	}

	public final boolean isLabeled() {
		return this.labeled;
	}

	/** Sets the font that will be used to draw the label, if the label is drawn. */
	public void setFont(Font f) {
		label.setFont(f);
	}

	private boolean isRR = false;
	/**
	 * lets the shadow know that the range being monitored is an integer range,
	 * as opposed to a real range.
	 * This is helpful for some ranges that operate on the borders of respectability
	 * like the NeoSeq.
	 */
	public void setResidueRange( boolean isResidueRange ) {
		if ( isResidueRange == this.isRR ) return; // It was already thus.
		this.isRR = isResidueRange;
	}

	public final boolean isResidueRange() {
		return this.isRR;
	}

	public void clearFromMap() {
		this.map.removeItem( this.tg );
	}


	public void setRange(int st, int en) {
		if (null == map.getView().getGraphics()) return;

		if ( null != this.vGlyph ) {
			int x, y, width, height;
			Rectangle2D.Double sbox = this.map.getCoordBounds( this.vGlyph );

			if (labeled) {
				// We must set the label text before calculating its width and height

				if (this.labelForReverse) {
					int revLabel = (int)((this.residueCount - st)+1);
					label.setString(dform.format(revLabel));
				}
				else {
					label.setString(dform.format(st));
				}

				View view = map.getView();
				Graphics g = view.getGraphics();
				Font font = label.getFont();
				if (font != null) g.setFont(font);
				FontMetrics fm = g.getFontMetrics();
				tx.width  = fm.stringWidth(label.getString());
				tx.height = fm.getAscent();

				// This means something like:  px = transform(tx)
				view.transformToCoords(tx, px);
			}

			int[] offset = map.getVisibleOffset();
			switch ( this.orientation ) {
				case NeoConstants.HORIZONTAL:
					x = st;
					width = en - x;
					y = (int) sbox.y;
					height = (int) sbox.height;
					if (labeled) {
						// The extraRect should be offset by (width*0.5, px.height*0.5) from the label
						extraRect.setCoords(x, offset[1] - px.height*1.5, 1, px.height);
						label.setCoords(x+width*0.5, offset[1] - px.height*1.0, 0,0);
					}
					break;
				case NeoConstants.VERTICAL:
					y = st;
					height = en - y;
					x = (int) sbox.x;
					width = (int) sbox.width;
					if (labeled) {
						// The extraRect should be offset by (px.width*0.5,height*0.5) from the label
						extraRect.setCoords(offset[1] - px.width*1.25,y, px.width, 1);
						label.setCoords(offset[1] - px.width*0.75,y+0.5*height, 0,0);
					}
					break;
				default:
					throw new IllegalStateException( "The orientation must be HORIZONTAL or VERTICAL." );
			}

			if ( this.isRR ) width++;

			if ( null != this.tg )  {
				this.tg.setCoords( x, y, width, height );
			}
			this.vGlyph.setCoords(x,y,width,height);

		}

		current_range_st = st;
		current_range_en = en;
	}

	/**
	 * Adjusts the shadow to match the range in the given event.
	 *
	 * @param evt the range change
	 */
	public void rangeChanged( NeoRangeEvent evt ) {
		int st = (int) evt.getVisibleStart();
		int en = (int) evt.getVisibleEnd();
		setRange(st, en);
	}

	/**
	 * Setting the shadow appropriately to display the coordinates for reverse strand
	 *
	 * @param residueCount the size of the sequence displayed
	 */
	public void shadowForReverse(long residueCount) {
		this.labelForReverse = true;
		this.residueCount = residueCount;
	}


	/**
	 * makes sure the shadow maintains its size relative to the map.
	 */
	public void viewBoxChanged(NeoViewBoxChangeEvent e) {
		Object source = e.getSource();
		View view = map.getView();
		if ( source == this.map || source == view) {
			Rectangle2D.Double sbox = this.map.getScene().getCoordBox();
			Rectangle2D.Double vgbox = vGlyph.getCoordBox();

			if (labeled) {
				int[] offset = map.getVisibleOffset();
				Graphics g = view.getGraphics();
				Font font = label.getFont();
				if (font != null) g.setFont(font);
				FontMetrics fm = g.getFontMetrics();
				tx.width  = fm.stringWidth(label.getString());
				tx.height = fm.getAscent();

				// This means something like:  px = transform(tx)
				view.transformToCoords(tx, px);

				switch ( this.orientation ) {
					case NeoConstants.HORIZONTAL:
						extraRect.setCoords(vgbox.x, offset[1] - px.height*1.5,
								vgbox.width, px.height);
						label.setCoords(vgbox.x+0.5*vgbox.width, offset[1] - px.height*1.0, 0,0);
						break;
					case NeoConstants.VERTICAL:
						extraRect.setCoords(offset[1] - px.width*1.25,
								vgbox.y, px.width,1);
						label.setCoords(offset[1] - px.width*0.75, vgbox.y+0.5*vgbox.height, 0,0);
						break;
				}
			}

			double height_scale = 1.0-topMargin-bottomMargin;
			switch ( this.orientation ) {
				case NeoConstants.HORIZONTAL:
					tg.setCoords( vgbox.x, sbox.y + ( sbox.height * topMargin ),
							vgbox.width, sbox.height * height_scale );
					vGlyph.setCoords( vgbox.x, sbox.y + ( sbox.height * topMargin ),
							vgbox.width, sbox.height * height_scale );
					break;
				case NeoConstants.VERTICAL:
					tg.setCoords( sbox.x + ( sbox.width * topMargin ), vgbox.y,
							sbox.width, vgbox.height * height_scale );
					vGlyph.setCoords( sbox.x + ( sbox.width * topMargin ), vgbox.y,
							sbox.width, vgbox.height * height_scale );
					break;
				default:
					throw new IllegalStateException( "The orientation must be HORIZONTAL or VERTICAL." );
			}

		}
		else {
			System.err.println( "Shadow: viewBoxChanged: Whoah. Where'd this come from? " + e );
		}
	}

	/**
	 * Allow or disallow selecting the shadow.
	 * (This has a side-effect of also making the shadow "hitable" or not.)
	 *
	 * @param selectable whether to allow selection
	 */
	public final void setSelectable(boolean selectable) {
		this.vGlyph.setSelectable(selectable);
		this.vGlyph.setHitable(selectable);
	}

	/**
	 * Set whether or not you want the Shadow drawn onto the
	 * other glyphs in the map using XOR.  (true by default.)
	 */
	public final void setUseXOR(boolean useXOR) {
		this.tg.setUseXOR(useXOR);
	}

	/**
	 * If true, the Shadow is drawn onto the map using XOR.
	 * If false, the Shadow overwrites onto the map.
	 */
	public final boolean getUseXOR() {
		return this.tg.getUseXOR();
	}

	public final void setShowHairline(boolean bool){
		this.tg.setVisibility(bool);
	}

	public void destroy() {
		if ( map != null ) {
			map.removeViewBoxListener(this);
			if ( map.getView() != null )
				map.getView().removePreDrawViewListener(this);
			map = null;
		}
	}
}
