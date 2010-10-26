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

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.util.*;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

/**
 * A glyph used to label other glyphs.
 * It draws a text string positioned relative to the labeled glyph.
 */
public class LabelGlyph extends Glyph implements NeoConstants  {
	// If TOGGLE_BY_WIDTH, then will only display when there is enough space
	// (calculated by pixel width) along its labelled glyph.
	boolean TOGGLE_BY_WIDTH = false;
	private static final boolean DEBUG_PIXELBOX = false;
	private static final boolean DEBUG_HIT = true;

	protected int placement = ABOVE;
	protected String text;
	protected Font fnt;
	protected String font_name;
	protected int font_size;
	protected int font_style;

	protected boolean show_outline = false;
	protected boolean show_background = false;

	protected Color outline_color = Color.yellow;

	// NOTE the coordbox of the label is actually the coordbox of the
	//      glyph being labeled
	// BUT  the pixelbox of the label is the pixel bounds of the
	//      label's drawn aspect (not including any connector to the
	//      labeled glyph)
	// ALSO need to keep track of several other
	//      coord and pixel boxes:
	//      enclosing_coords, enclosing_pix, label_coords, labeled_pix

	// enclosing coord box contains both the label glyph's coord box and the
	//    coord box of the glyph it is labeling
	public Rectangle2D.Double enclosing_coords;

	// enclosing pixel box contains both the label glyph's pixel box and the
	//    pixel box of the glyph it is labeling
	public Rectangle enclosing_pix;

	// label_coords is the coord box for just the label's drawn aspect
	//   (not including any connector the the labeled glyph...)
	public Rectangle2D.Double label_coords;

	// labeled_pix is the pixel box for the glyph that LabelGlyph is
	//   labeling
	public Rectangle labeled_pix;

	// the Glyph that is being labeled -- null means label is "on its own"
	protected GlyphI labeled;

	// keeping track of previous view to make sure previous coord/pixel
	//    calculations are relevant to current view
	//    (really needs to keep track of Transform -- maybe make a
	//     view.equivalent(view2) or view.unchanged(view2) method???)
	protected ViewI prev_view = null;

	// distance away from glyph being labeled that glyph should be placed
	// (currently ignored for placement of CENTER or NONE)
	int pixel_separation;

	// y position of text baseline (for drawString(str, x, y) since y argument
	//   needs to be text baseline rather than top of text
	int text_baseline;

	public LabelGlyph (String str) {
		this();
		this.setText(str);
	}

	public LabelGlyph () {
		setFont( NeoConstants.default_plain_font );
		setFontExtras();
		placement = ABOVE;
		labeled_pix = new Rectangle();
		enclosing_pix = new Rectangle();
		enclosing_coords = new Rectangle2D.Double();
		label_coords = new Rectangle2D.Double();
		pixel_separation = 3;

	}


	public void draw(ViewI view) {
		if (this.getFont() == null || this.text == null) { return; }
		Graphics g = view.getGraphics();
		g.setFont(this.getFont());

		calcPixels(view);

		// if TOGGLE_BY_WIDTH, only show if LabelGlyph's pixelbox is <= pixels of glyph being labelled
		if ((! TOGGLE_BY_WIDTH) || (pixelbox.width <= labeled_pix.width)) {
			if (show_background) {
				g.setColor(this.getBackgroundColor());
				g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
			}
			g.setColor( this.getTextColor() );
			// +2 for offsetting text from outline/background
			g.drawString (text, pixelbox.x+2, this.text_baseline);
		}


		// note that union creates a new Rectangle -- may want to try
		// doing union calculations here instead to avoid object creation...
		enclosing_pix = pixelbox.union(labeled_pix);
		enclosing_coords = view.transformToCoords(enclosing_pix, enclosing_coords);
		label_coords = view.transformToCoords(pixelbox, label_coords);
		view.transformToCoords(pixelbox, coordbox);

		if (DEBUG_PIXELBOX) {
			g.setColor(Color.red);
			g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
		}
		if (show_outline) {
			g.setColor( this.getOutlineColor() );
			g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
		}

		// The only reason I can find for calling super's draw method
		// is to draw a rectangle around the pixelbox.
		// This recalculates the pixel box by calling View.transformToPixels
		// without calling calcPixels().
		// Since the coord box is set to that of the labeled glyph
		// the above strategy resets the pixel box to that of the labeled glyph.
		// I don't want that.
		// So I put the call to super.draw in an if (debug) block
		// and call calcPixels again (when debugging)
		// so as to correct the pixel box.
		if (DEBUG_PIXELBOX || DEBUG_HIT) {
			super.draw(view);
			calcPixels(view);
		}
		prev_view = view;
	}


	/**
	 * calculates a containing rectangle
	 * in pixel space.
	 * This rectangle is stored
	 * in the instance variable <code>pixelbox</code>.
	 *
	 * @param theView into pixel space
	 * @see #calcPixels
	 * @see GlyphI#getPixelBox
	 */
	public void calcPixels(ViewI theView) {

		FontMetrics fm;
		if ( null == theView.getGraphics() ) {
			fm = GeneralUtils.getFontMetrics(getFont());
		}
		else {
			fm = theView.getGraphics().getFontMetrics();
		}

		int text_width = (null==this.text)?0:fm.stringWidth(this.text);
		int text_height = (null==this.text)?0:fm.getAscent();

		if (null == this.labeled) {
			theView.transformToPixels(this.coordbox, this.pixelbox);
		} else {
			theView.transformToPixels(this.labeled.getCoordBox(), this.pixelbox);
		}
		labeled_pix.setBounds(pixelbox.x, pixelbox.y,
				pixelbox.width, pixelbox.height);
		if (placement == LEFT) {
			pixelbox.x = pixelbox.x - text_width - pixel_separation;
		}
		else if (placement == RIGHT) {
			pixelbox.x = pixelbox.x + pixelbox.width + pixel_separation;
		}
		else {
			pixelbox.x = pixelbox.x + pixelbox.width/2 - text_width/2;
		}
		if (placement == ABOVE) {
			//      pixelbox.y = pixelbox.y - pixel_separation;
			this.text_baseline = pixelbox.y - pixel_separation;
		}
		else if (placement == BELOW) {
			this.text_baseline = pixelbox.y + pixelbox.height +
				text_height + pixel_separation;
		}
		else {
			this.text_baseline = pixelbox.y + pixelbox.height/2 + text_height/2;
		}
		pixelbox.width = text_width;
		pixelbox.height = text_height;
		pixelbox.y = this.text_baseline - text_height;

		// -2/+4 for offsetting outline/background from text position
		pixelbox.x -= 2;
		pixelbox.width += 4;
	}

	public boolean intersects(Rectangle2D.Double rect, ViewI view) {
		this.calcPixels(view);
		this.coordbox = view.transformToCoords(this.pixelbox, this.coordbox);
		return super.intersects(rect, view);
	}

	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		calcPixels(view);
		coordbox = view.transformToCoords(pixelbox, coordbox);
		return coord_hitbox.intersects(coordbox);
	}


	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		if (view != prev_view) {
			calcPixels(view);
		}
		return pixel_hitbox.intersects(pixelbox);
	}


	/**
	 * sets the text that appears in this glyph.
	 *
	 * @param str the text
	 */
	public void setText(String str) {
		this.text = str;
	}

	/**
	 * gets the text that appears in this glyph.
	 *
	 * @return the text
	 */
	public String getText() {
		return this.text;
	}


	public void setFont(Font f) {
		this.style = stylefactory.getStyle( style.getForegroundColor(), style.getBackgroundColor(), f );
		setFontExtras();
	}

	public Font getFont() {
		return this.style.getFont();
	}

	//  since fnt is referenced in setFont(fnt), not copied, it could be
	//  changed from outside.  Therefore whenever font extras
	//  (name, size, style) are going to be referred to, it is a good idea to
	//  call setFontExtras() first;
	// NOTE that new font IS created in setFont() with no arguments though...
	//   might want to set up an external_font boolean so can have it both
	//   ways...
	protected void setFontExtras() {
		Font fnt = getFont();
		font_name = fnt.getFamily();
		font_size = fnt.getSize();
		font_style = fnt.getStyle();
	}

	public void setFontName(String name) {
		setFontExtras();
		font_name = name;
		setFont();
	}

	public void setFontSize(int size) {
		setFontExtras();
		font_size = size;
		setFont();
	}

	public void setFontStyle(int style) {
		setFontExtras();
		font_style = style;
		setFont();
	}

	protected void setFont() {
		setFont( new Font(font_name, font_style, font_size) );
	}

	/**
	 * places this label
	 * relative to the labeled glyph.
	 *
	 * @param placement LEFT, RIGHT, ABOVE, BELOW, or CENTER
	 */
	public void setPlacement(int placement) {
		switch (placement) {
			case LEFT:
			case RIGHT:
			case ABOVE:
			case BELOW:
			case CENTER:
				this.placement = placement;
				break;
			default:
				throw new IllegalArgumentException
					("must be LEFT, RIGHT, ABOVE, BELOW, or CENTER");
		}
	}

	/**
	 * gets this label's placement
	 * relative to the labeled glyph.
	 *
	 * @return LEFT, RIGHT, ABOVE, BELOW, or CENTER
	 */
	public int getPlacement() {
		return this.placement;
	}

	public void setPixelSeparation(int pix) {
		pixel_separation = pix;
	}

	public int getPixelSeparation() {
		return pixel_separation;
	}

	/**
	 * associates this label with a labeled glyph.
	 * Only one glyph can be labeled at a time.
	 * Multiple labels can label the same glyph.
	 *
	 * @param lg the labeled glyph
	 */
	public void setLabeledGlyph(GlyphI lg) {

		labeled = lg;

		/* Here we set this.coordbox to that of the labeled glyph.
		 * This is a reasonable default.
		 */
		Rectangle2D.Double lgbox = lg.getCoordBox();

		/* We favor setCoords() over setCoordBox for speed.
		 * Also, setting to lg's coordbox causes moving problems,
		 * since both lg and this will try to do moveRelative() calls
		 * affecting the _same_ coordbox.
		 */
		setCoords(lgbox.x, lgbox.y, lgbox.width, lgbox.height);

		prev_view = null;
	}

	public GlyphI getLabeledGlyph() {
		return(labeled);
	}

	public void setShowOutline(boolean show) {
		show_outline = show;
	}

	public boolean getShowOutline() {
		return show_outline;
	}

	public void setShowBackground(boolean show) {
		show_background = show;
	}

	public boolean getShowBackground() {
		return show_background;
	}

	public void setOutlineColor(Color col) {
		outline_color = col;
	}

	public void setTextColor(Color col) {
		setForegroundColor( col );
	}

	public Color getOutlineColor() {
		return outline_color;
	}

	public Color getTextColor() {
		return getForegroundColor();
	}

	/**
	 * We override the superclass's version of this method
	 * so that we can add both the labeling (this) and labeled glyph
	 * to the pickList.
	 */
	public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList,
			ViewI view)  {
		if (isVisible && intersects(pickRect, view))  {
			if (hit(pickRect, view))  {
				pickList.add(this);
				if (null != this.labeled && !pickList.contains(this.labeled)) {
					pickList.add(this.labeled);
				}
			}
			if (children != null)  {
				GlyphI child;
				int childnum = children.size();
				for (int i=0; i<childnum; i++) {
					child = children.get(i);
					child.pickTraversal(pickRect, pickList, view);
				}
			}
		}
	}

	public void setToggleByWidth(boolean b) {
		TOGGLE_BY_WIDTH = b;
	}

	public boolean getToggleByWidth() {
		return TOGGLE_BY_WIDTH;
	}
}
