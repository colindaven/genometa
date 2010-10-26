/**
 *   Copyright (c) 1998-2006 Affymetrix, Inc.
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
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.util.List;
import java.text.DecimalFormat;
import com.affymetrix.genoviz.util.GeneralUtils;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;

/**
 *  A glyph to display a vertical or horizontal numbered axis.
 */
public class AxisGlyph extends Glyph {

	/*
	 * We use the term "thickness" for the dimension orthogonal to orientation.
	 * We use the term "length" for the dimension along the orientation.
	 * This is to avoid confusion with the AWT terms "width" and "height"
	 * which always correspond to horizontal and vertical dimensions.
	 * So when the orientation is HORIZONTAL
	 * then "thickness" is height and "length" is width.
	 * When the orientation is VERTICAL
	 * then "thickness" is width and "length" is height.
	 */

	public boolean DEBUG_DRAW = false;

	/**
	 * HORIZONTAL or VERTICAL orientation.
	 * set only in constructors.
	 */
	protected int orient;

	/*
	 * Axisglyph can display itself with inverted coordinates in the HORIZONTAL direction.
	 * This was added to provide rudimentary ability to display a reverse-complemented sequence
	 * with a neomap.  However, the actual coordinate space of the NeoMap remains unchanged (which
	 * can admittedly make things a little confusing).
	 */

	protected boolean reversed = false;

	protected double label_scale = 1;

	private List<int[]> selected_regions;

	// default to true for backward compatability
	protected boolean hitable = true;

	/**
	 *  This method reverses the numbering of the axisglyph, although the
	 *  coordinate space of the underlying NeoMap remains the same.  This
	 *  makes it theoretically possible (if quite confusing) to display a
	 *  map that appears to have a start > end.
	 */
	public void setReversed ( boolean reversed ) {
		this.reversed = reversed;
	}

	protected Font label_font;


	// This DecimalFormat is used with the COMMA format.
	// It simply instructs java to insert commas between every three characters.
	DecimalFormat comma_format = new DecimalFormat("#,###.###");


	// A couple constants used only in the draw method.
	protected int subtick_size = 1;
	protected static final Rectangle2D.Double unitrect = new Rectangle2D.Double(0,0,1,1);
	// A couple of temporary rectangles used in the draw method
	private final Rectangle2D.Double select_coord = new Rectangle2D.Double();
	private final Rectangle select_pix = new Rectangle();
	private final Rectangle2D.Double scratchcoords = new Rectangle2D.Double();
	private final Rectangle scratchpixels = new Rectangle();

	/**
	 * Sets the font in which labels will be rendered.
	 *
	 * @param f the font to use
	 */
	protected final void internalSetFont(Font f) {
		this.label_font = f;
		FontMetrics fm = GeneralUtils.getFontMetrics(f);
		if (VERTICAL == this.orient) {
			labelThickness = fm.stringWidth("000000");
		}
		else {
			labelThickness = fm.getAscent();
		}
		setLabelPlacement(getLabelPlacement()); // to recalculate offsets.
	}

	/**
	 * Sets the font in which labels will be rendered.
	 *
	 * @param fnt a new matching font will be created
	 * and used internally.
	 */
	@Override
	public void setFont(Font fnt) {
		if (!fnt.equals(this.label_font)) {
			internalSetFont(new Font(fnt.getName(), fnt.getStyle(), fnt.getSize()));
		}
	}

	@Override
	public Font getFont() {
		return new Font
			(this.label_font.getName(),
			 this.label_font.getStyle(),
			 this.label_font.getSize());
	}

	/**
	 * Sets the color in which the axis is rendered.
	 *
	 * <p><em>Note that the super class, Glyph,
	 * sets the background color in setColor.
	 * We override it here to set the foreground color
	 * since that is the color used in our draw method.
	 * </em></p>
	 *
	 * @param color the color the axis should be.
	 * @deprecated use setForegroundColor().
	 */
	@Deprecated
		public void setColor(Color color)  {
			this.setForegroundColor( color );
		}

	/**
	 * @deprecated use getForegroundColor().
	 * @see #setColor
	 */
	@Deprecated
		public Color getColor()  {
			return this.getForegroundColor();
		}

	public static final int FULL = 0;
	public static final int ABBREV = FULL+1;
	public static final int COMMA = ABBREV+1;
	public static final int NO_LABELS = COMMA+1;
	protected int labelFormat = FULL;

	/**
	 * Sets the label format.
	 * Format {@link #ABBREV} will replace trailing "000" with a "k"
	 * and trailing "000000" with an "M", etc. for "G", "T".
	 * Format {@link #COMMA} will put commas in the numbers,
	 * like "1,234,250".
	 *
	 * @param theFormat {@link #FULL} or {@link #ABBREV} or {@link #COMMA}.
	 */
	public void setLabelFormat(int theFormat) {
		if (theFormat != FULL && theFormat != ABBREV && theFormat != COMMA && theFormat != NO_LABELS) {
			throw new IllegalArgumentException(
					"Label format must be FULL, ABBREV, COMMA, or NO_LABELS.");
		}
		this.labelFormat = theFormat;
	}

	//TODO: use a Range object for the range
	public void selectRange ( int[] range ) {
		if ( range.length != 2 ) {
			System.err.println ( "AxisGlyph.selectRange got a int[] that was not of length 2.  Not selecting range." );
			return;
		}
		if ( selected_regions == null ) selected_regions = new ArrayList<int[]>();
		selected_regions.add( range );
	}

	public void deselectAll () {
		selected_regions.clear();
	}

	public int getLabelFormat() {
		return this.labelFormat;
	}

	protected int tickPlacement = ABOVE;

	/**
	 * Places the axis ticks relative to the center line.
	 *
	 * @param thePlacement ABOVE or BELOW for HORIZONTAL axes,
	 *                     RIGHT or LEFT for VERTICAL axes.
	 */
	public void setTickPlacement(int thePlacement) {
		switch (thePlacement) {
			case ABOVE:
				if (VERTICAL == this.orient)
					throw new IllegalArgumentException
						("Cannot place ticks above a VERTICAL axis.");
				subtick_size = 1;
				break;
			case RIGHT:
				if (HORIZONTAL == this.orient)
					throw new IllegalArgumentException
						("Cannot place ticks to the right of a HORIZONTAL axis.");
				subtick_size = 1;
				break;
			case BELOW:
				if (VERTICAL == this.orient)
					throw new IllegalArgumentException
						("Cannot place ticks below a VERTICAL axis.");
				subtick_size = -2;
				break;
			case LEFT:
				if (HORIZONTAL == this.orient)
					throw new IllegalArgumentException
						("Cannot place ticks to the left of a HORIZONTAL axis.");
				subtick_size = -2;
				break;
			case CENTER:
				subtick_size = -1;
				break;
			case NONE:
				subtick_size = 0;
				break;
			default:
				throw new IllegalArgumentException
					("Tick placement must be ABOVE, BELOW, RIGHT, or LEFT.");
		}
		this.tickPlacement = thePlacement;
	}

	public int getTickPlacement() {
		return this.tickPlacement;
	}


	private static final int MAJORTICKHEIGHT = 2;

	// gap between center_line and edge of labels.
	protected static final int labelGap = MAJORTICKHEIGHT+2;

	protected int labelPlacement = ABOVE;
	protected int labelShift = 5;
	protected int labelThickness;

	/**
	 * Places the axis labels relative to the center line and ticks.
	 *
	 * <p> Bug: When placed to the LEFT labels are still left justified.
	 * This leaves an unsightly gap with small numbers.
	 *
	 * @param thePlacement ABOVE or BELOW for HORIZONTAL axes,
	 *                     RIGHT or LEFT for VERTICAL axes.
	 */
	public void setLabelPlacement(int thePlacement) {
		switch (thePlacement) {
			case ABOVE:
				if (VERTICAL == this.orient)
					throw new IllegalArgumentException
						("Cannot place labels above a VERTICAL axis.");
				labelShift = labelGap;
				break;
			case RIGHT:
				if (HORIZONTAL == this.orient)
					throw new IllegalArgumentException
						("Cannot place labels to the right of a HORIZONTAL axis.");
				labelShift = labelGap;
				break;
			case BELOW:
				if (VERTICAL == this.orient)
					throw new IllegalArgumentException
						("Cannot place labels below a VERTICAL axis.");
				labelShift = -centerLineThickness - labelGap - labelThickness;
				break;
			case LEFT:
				if (HORIZONTAL == this.orient)
					throw new IllegalArgumentException
						("Cannot place labels to the left of a HORIZONTAL axis.");
				labelShift = -centerLineThickness - labelGap - labelThickness;
				break;
			default:
				throw new IllegalArgumentException
					("Label placement must be ABOVE, BELOW, RIGHT, or LEFT.");
		}
		this.labelPlacement = thePlacement;
	}

	public int getLabelPlacement() {
		return this.labelPlacement;
	}

	/**
	 * Creates an axis.
	 *
	 * @param orientation HORIZONTAL or VERTICAL
	 */
	public AxisGlyph(int orientation) {
		switch (orientation) {
			case VERTICAL:
				this.orient = orientation;
				setLabelPlacement(LEFT);
				break;
			case HORIZONTAL:
				this.orient = orientation;
				break;
			default:
				throw new IllegalArgumentException
					("Orientation must be HORIZONTAL or VERTICAL.");
		}
		internalSetFont(new Font("Helvetica", Font.BOLD, 12));
		setSelectable(false);
	}

	/**
	 * Creates a horizontal axis.
	 */
	public AxisGlyph() {
		this(HORIZONTAL);
	}

	@Override
	public void setCoords(double x, double y, double width, double height) {
		super.setCoords(x, y, width, height);
		setCenter();
	}

	@Override
	public void setCoordBox(Rectangle2D.Double coordbox) {
		super.setCoordBox(coordbox);
		setCenter();
	}

	double center_line;
	static int centerLineThickness = 2;
	private Rectangle2D.Double lastCoordBox = null;

	/**
	 * Centers the center_line within this axis' coordbox.
	 */
	protected void setCenter() {
		if (orient == VERTICAL) {
			center_line = coordbox.x + coordbox.width/2;
		}
		else {
			center_line = coordbox.y + coordbox.height/2;
		}
		this.lastCoordBox = null;
	}

	/**
	 * Places the center_line inside the coordbox.
	 * This should only be called when the coordbox has moved.
	 */
	private void placeCenter(ViewI theView) {

		if (null == lastCoordBox) { // then this is the first time we've done this.

			// Mark the original placement of our coord box.
			lastCoordBox = new Rectangle2D.Double
				(this.coordbox.x,
				 this.coordbox.y,
				 this.coordbox.width,
				 this.coordbox.height);

			// Center the center_line in the original coord box.
			Rectangle2D.Double centralLine = new Rectangle2D.Double(coordbox.x, coordbox.y, 0f, 0f);
			if (orient == VERTICAL) {
				center_line = coordbox.x + coordbox.width/2;
				centralLine.x = center_line;
				centralLine.height = coordbox.height;
			}
			else {
				center_line = coordbox.y + coordbox.height/2;
				centralLine.y = center_line;
				centralLine.width = coordbox.width;
			}
			theView.transformToPixels(coordbox, pixelbox);
			Rectangle centralBox = new Rectangle();
			theView.transformToPixels(centralLine, centralBox);


			// Adjust the pixel box to shrink wrap the axis.
			if (VERTICAL == this.orient) {
				centralBox.x -= MAJORTICKHEIGHT;
				centralBox.width = centerLineThickness + ( 2 * MAJORTICKHEIGHT);
				if (LEFT == this.labelPlacement) {
					centralBox.x -= labelThickness;
					centralBox.width += labelThickness;
				}
				else if (RIGHT == this.labelPlacement) {
					centralBox.width += labelThickness;
				}
			}
			else { // (HORIZONTAL == this.orient)
				centralBox.y -= MAJORTICKHEIGHT;
				centralBox.height = centerLineThickness + ( 2 * MAJORTICKHEIGHT);
				if (ABOVE == this.labelPlacement) {
					centralBox.y -= labelThickness;
					centralBox.height += labelThickness;
				}
				else if (BELOW == this.labelPlacement) {
					centralBox.height += labelThickness;
				}
			}

			Rectangle2D.Double temp_rect = new Rectangle2D.Double(coordbox.x, coordbox.y,
					coordbox.width, coordbox.height);
			// Readjust the coord box to match the new pixel box.
			theView.transformToCoords(centralBox, temp_rect);

			if (HORIZONTAL == orient) {
				coordbox.y = temp_rect.y;
				coordbox.height = temp_rect.height;
				// leave coordbox.x and coordbox.width alone
				// (temp_rect.width will be pretty close to coordbox.width, but round-off errors in
				// the transformations can result in problems that manifest as the right
				// edge of the axis not being drawn when the zoom level is very high)
			} else if (VERTICAL == orient) {
				coordbox.x = temp_rect.x;
				coordbox.width = temp_rect.width;
				// leave the y and height coords alone
			}
		}

		else {
			if (VERTICAL == this.orient) {
				double r = ( lastCoordBox.x - center_line ) / lastCoordBox.width;
				center_line = this.coordbox.x - ( r * this.coordbox.width );
			}
			else { // (HORIZONTAL == this.orient)
				double r = ( lastCoordBox.y - center_line ) / lastCoordBox.height;
				center_line = this.coordbox.y - ( r * this.coordbox.height );
			}
		}
		lastCoordBox.x = coordbox.x;
		lastCoordBox.y = coordbox.y;
		lastCoordBox.width = coordbox.width;
		lastCoordBox.height = coordbox.height;

	}

	/**
	 * sets the coords
	 * to make sure axis spans the whole map
	 * in the direction it is oriented.
	 *
	 * <p> A NeoMap keeps a list of its axes
	 * added via the NeoMap's addAxis method.
	 * Every time the NeoMap's range changes this method is called
	 * for each of its axes.
	 */

	public void rangeChanged() {
		if (DEBUG_DRAW) System.err.println("Parental Coords: "+parent.getCoordBox());
		if (VERTICAL == this.orient) {
			coordbox.y = parent.getCoordBox().y;
			coordbox.height = parent.getCoordBox().height;
		}
		else {
			coordbox.x = parent.getCoordBox().x;
			coordbox.width = parent.getCoordBox().width;
		}
	}


	@Override
	public void draw(ViewI view) {
		int axis_loc;
		LinearTransform cumulative;
		int axis_length;

		FontMetrics fm=null;
		if (orient == VERTICAL && LEFT == this.labelPlacement) {
			fm = view.getGraphics().getFontMetrics();
		}


		if (DEBUG_DRAW) { System.err.println("called draw() on " + this); }
		if (DEBUG_DRAW) { System.err.println("Coords: " + getCoordBox()); }

		// Packers do not seem to be calling setCoord method.
		// So we need to do this in case a packer has moved the axis.
		if (null == lastCoordBox || !this.coordbox.equals(lastCoordBox)) {
			placeCenter(view);
		}

		// We don't need to do this if the axis is never moved
		// as it was when it was invisible to packers
		// by dint of having no intersects or hit methods.

		view.transformToPixels(coordbox, pixelbox);
		if (DEBUG_DRAW) { System.err.println("Pixels: " + pixelbox); }
		if (DEBUG_DRAW) { System.err.println("Transform: " + view.getTransform());}

		Rectangle2D.Double scenebox = scene.getCoordBox();
		double scene_start, scene_end;
		if (orient == VERTICAL) {
			scene_start = scenebox.y;
			scene_end = scenebox.y + scenebox.height;
			scratchcoords.x = center_line;
			scratchcoords.width = 0;
			scratchcoords.y = coordbox.y;
			scratchcoords.height = coordbox.height;
		}
		else {
			scene_start = scenebox.x;
			scene_end = scenebox.x + scenebox.width;
			scratchcoords.y = center_line;
			scratchcoords.height = 0;
			scratchcoords.x = coordbox.x;
			scratchcoords.width = coordbox.width;
		}
		view.transformToPixels(scratchcoords, scratchpixels);
		cumulative = view.getTransform();

		Rectangle clipbox = view.getPixelBox();
		Graphics g = view.getGraphics();
		Font savefont = g.getFont();
		if (savefont != label_font) {
			g.setFont(label_font);
		}

		LinearTransform.transform(cumulative, unitrect, scratchcoords);
		double pixels_per_unit = (orient == VERTICAL) ?
			scratchcoords.height :
			scratchcoords.width;

		// if make it this far but scale is weird, return without drawing
		if (pixels_per_unit == 0 || Double.isNaN(pixels_per_unit) ||
				Double.isInfinite(pixels_per_unit)) {
			return;
				}

		int axis_start;   // start to draw axis at (in canvas coordinates)
		int axis_width;     // width to draw axis (in canvas coordinates)

		double units_per_pixel = 1/pixels_per_unit;

		int clip_start, clip_width;
		if (orient == VERTICAL) {
			axis_loc = scratchpixels.x;
			axis_start = pixelbox.y;
			axis_width = pixelbox.height;
			clip_start = clipbox.y;
			clip_width = clipbox.height;
		}
		else {
			axis_loc = scratchpixels.y;
			axis_start = pixelbox.x;
			axis_width = pixelbox.width;
			clip_start = clipbox.x;
			clip_width = clipbox.width;
		}

		// TODO - axis_start was negative due to overflow.
		axis_start = Math.max(axis_start, clip_start);
		axis_width = Math.min(axis_width, clip_width);

		axis_width++;
		
		g.setColor( getForegroundColor() );

		// Draw the base line.

		int center_line_start = axis_loc - centerLineThickness/2;

		if (orient == VERTICAL)  {
			g.fillRect(center_line_start, axis_start, centerLineThickness,axis_width);
		}
		else {
			g.fillRect(axis_start, center_line_start, axis_width, centerLineThickness);
			setColorForSelections2(g, view, center_line_start);
		}

		if (DEBUG_DRAW) {
			System.err.println("Calculating tick increment" +
					", units_per_pixel = " + units_per_pixel +
					", pixels_per_unit = " + pixels_per_unit);
		}
		// space between tickmarks (in map coordinates)
		double tick_increment = tickIncrement(units_per_pixel, pixels_per_unit);
		if (DEBUG_DRAW) System.err.println("tick increment = " + tick_increment);

		// Calculate map_loc and max_map.

		double map_loc;
		double max_map;    // max tickmark to draw (in map coordinates)

		if (orient == VERTICAL) {
			if (pixelbox.y < clipbox.y) {
				map_loc = (((int)(view.transformToCoords(clipbox, scratchcoords).y /
								tick_increment)) * tick_increment);
			}
			else  {
				map_loc = view.transformToCoords(pixelbox, scratchcoords).y;
			}
			if (pixelbox.y+pixelbox.height > clipbox.y+clipbox.height)  {
				view.transformToCoords(clipbox, scratchcoords);
			}
			else  {
				view.transformToCoords(pixelbox, scratchcoords);		
			}
			max_map = scratchcoords.y + scratchcoords.height;
		}
		else {
			if (pixelbox.x < clipbox.x)  {
				map_loc = (((int)(view.transformToCoords(clipbox, scratchcoords).x /
								tick_increment)) * tick_increment);
			}
			else  {
				map_loc = view.transformToCoords(pixelbox, scratchcoords).x;
			}

			if (pixelbox.x+pixelbox.width > clipbox.x+clipbox.width)  {
				view.transformToCoords(clipbox, scratchcoords);
			}
			else  {
				view.transformToCoords(pixelbox, scratchcoords);	
			}
			max_map = scratchcoords.x + scratchcoords.width;
		}

		if (DEBUG_DRAW) System.err.println("map_loc " + map_loc + ", max " + max_map);

		double subtick_increment = tick_increment/10;
		double subtick_loc, rev_subtick_loc;
		// need to do tick_loc for those maps that don't start
		// at convenient tick_increments
		double tick_loc = tick_increment * Math.ceil(map_loc/tick_increment);

		// for reversed map, start by drawing from the right side
		// use view's coordbox -- we were having problems with the
		// coordbox.width not being accurate for reversed axes.
		double rev_tick_const = (view.getScene().getCoordBox().x + view.getScene().getCoordBox().width);

		// This computation finds the location of the right-most tickmark so
		// we can start drawing ticks from that location when the axis is reversed.
		// Starting from the right-most edge of the coordbox as was previously done
		// resulted in a big performance drain.  EEE - Sept 2000
		double rev_tick_loc = rev_tick_const- tick_increment *
			Math.ceil((rev_tick_const-max_map)/tick_increment);

		// making sure first tick_loc is offscreen to ensure that all visible
		// subticks between it and first visible tick_loc get drawn
		// fixes missing subtick problem -- GAH 12/14/97

		tick_loc -= tick_increment;
		rev_tick_loc += tick_increment;

		subtick_loc = tick_loc;
		rev_subtick_loc = rev_tick_loc;
		double tick_scaled_loc, tick_scaled_increment, rev_tick_scaled_loc;
		if (orient == VERTICAL) {
			scratchcoords.y = (reversed ? rev_tick_loc : tick_loc);
			scratchcoords.height = tick_increment;
			LinearTransform.transform(cumulative, scratchcoords, scratchcoords);
			tick_scaled_loc = scratchcoords.y;
			tick_scaled_increment = scratchcoords.height;
			rev_tick_scaled_loc = scratchcoords.y;
		}
		else {
			scratchcoords.x = (reversed ? rev_tick_loc : tick_loc);
			scratchcoords.width = tick_increment;
			LinearTransform.transform(cumulative, scratchcoords, scratchcoords);
			tick_scaled_loc = scratchcoords.x;
			tick_scaled_increment = scratchcoords.width;
			rev_tick_scaled_loc = scratchcoords.x;
		}
		drawTicks(tick_loc, max_map, tick_increment, tick_scaled_loc, tick_scaled_increment, g, view, scene_start, scene_end, fm, center_line_start, rev_tick_loc, rev_tick_scaled_loc, clipbox, rev_tick_const);
		
		drawSubTicks(subtick_loc, subtick_increment, cumulative, rev_subtick_loc, max_map, g, view, scene_start, scene_end, center_line_start, clipbox);

		if (savefont != label_font) {
			g.setFont(savefont);
		}

		super.draw(view);
		if (DEBUG_DRAW) { System.err.println("leaving draw() for " + this); }
		DEBUG_DRAW = false;

	}


	/**
	 * Draw the major tick marks including labels.
	 * @param tick_loc
	 * @param max_map
	 * @param tick_increment
	 * @param tick_scaled_loc
	 * @param tick_scaled_increment
	 * @param g
	 * @param view
	 * @param scene_start
	 * @param scene_end
	 * @param fm
	 * @param center_line_start
	 * @param rev_tick_loc
	 * @param rev_tick_scaled_loc
	 * @param clipbox
	 * @param rev_tick_const
	 */
	private void drawTicks(double tick_loc, double max_map, double tick_increment, double tick_scaled_loc, double tick_scaled_increment, Graphics g, ViewI view, double scene_start, double scene_end, FontMetrics fm, int center_line_start, double rev_tick_loc, double rev_tick_scaled_loc, Rectangle clipbox, double rev_tick_const) {
		if (!reversed) {
			drawForwardTick(tick_loc, max_map, tick_increment, tick_scaled_loc, tick_scaled_increment, g, view, scene_start, scene_end, fm, center_line_start);
		} else {
			//reversed axis, major axis ticks and numbering
			drawReverseTick(rev_tick_loc, tick_increment, rev_tick_scaled_loc, tick_scaled_increment, clipbox, rev_tick_const, g, view, scene_start, scene_end, fm, center_line_start);
		}
	}

	private void drawForwardTick(double tick_loc, double max_map, double tick_increment, double tick_scaled_loc, double tick_scaled_increment, Graphics g, ViewI view, double scene_start, double scene_end, FontMetrics fm, int center_line_start) {
		for (; tick_loc <= max_map; tick_loc += tick_increment, tick_scaled_loc += tick_scaled_increment) {
			int canvas_loc = (int) tick_scaled_loc;
			setColorForSelections(g, view, canvas_loc);
			String label = "";
			if (labelFormat != NO_LABELS) {
				label = stringRepresentation(tick_loc, tick_increment);
			}
			drawTickLoc(tick_loc, scene_start, scene_end, fm, label, g, center_line_start, canvas_loc);
		}
	}

	private void drawReverseTick(double rev_tick_loc, double tick_increment, double rev_tick_scaled_loc, double tick_scaled_increment, Rectangle clipbox, double rev_tick_const, Graphics g, ViewI view, double scene_start, double scene_end, FontMetrics fm, int center_line_start) {
		//reversed axis, major axis ticks and numbering
		for (; rev_tick_loc > 0; rev_tick_loc -= tick_increment, rev_tick_scaled_loc -= tick_scaled_increment) {
			int canvas_loc = (int) rev_tick_scaled_loc;
			// Don't draw things which are out of the clipbox
			// This fixes an enormous performance drain.  EEE-Sept 2000
			if (canvas_loc < clipbox.x) {
				break;
			}
			if (canvas_loc > clipbox.x + clipbox.width) {
				continue;
			}
			setColorForSelections(g, view, canvas_loc);
			String label = "";
			if (labelFormat != NO_LABELS) {
				double rev_tick_value = rev_tick_const - rev_tick_loc;
				label = stringRepresentation(rev_tick_value, tick_increment);
			}
			drawTickLoc(rev_tick_loc, scene_start, scene_end, fm, label, g, center_line_start, canvas_loc);
		}
	}

	
	/**
	 * Draw the minor tick marks.
	 * @param subtick_loc
	 * @param subtick_increment
	 * @param cumulative
	 * @param rev_subtick_loc
	 * @param max_map
	 * @param g
	 * @param view
	 * @param scene_start
	 * @param scene_end
	 * @param center_line_start
	 * @param clipbox
	 */
	private void drawSubTicks(double subtick_loc, double subtick_increment, LinearTransform cumulative, double rev_subtick_loc, double max_map, Graphics g, ViewI view, double scene_start, double scene_end, int center_line_start, Rectangle clipbox) {
		double subtick_scaled_increment;
		double subtick_scaled_loc;
		if (orient == VERTICAL) {
			scratchcoords.y = subtick_loc;
			scratchcoords.height = subtick_increment;
			LinearTransform.transform(cumulative, scratchcoords, scratchcoords);
			subtick_scaled_loc = scratchcoords.y;
			subtick_scaled_increment = scratchcoords.height;
		} else {
			//horizontal map
			scratchcoords.x = (reversed ? rev_subtick_loc : subtick_loc);
			scratchcoords.width = subtick_increment;
			LinearTransform.transform(cumulative, scratchcoords, scratchcoords);
			subtick_scaled_loc = scratchcoords.x;
			subtick_scaled_increment = scratchcoords.width;
		}
		if (!reversed) {
			drawForwardSubTick(subtick_loc, max_map, subtick_increment, subtick_scaled_loc, g, view, scene_start, scene_end, center_line_start, subtick_scaled_increment);
		} else {
			//reversed map
			drawReverseSubTick(rev_subtick_loc, subtick_increment, subtick_scaled_loc, subtick_scaled_increment, clipbox, g, view, scene_start, scene_end, center_line_start);
		}
	}



	private void drawForwardSubTick(double subtick_loc, double max_map, double subtick_increment, double subtick_scaled_loc, Graphics g, ViewI view, double scene_start, double scene_end, int center_line_start, double subtick_scaled_increment) {
		for (; subtick_loc <= max_map; subtick_loc += subtick_increment) {
			int canvas_loc = (int) (subtick_scaled_loc + 0.5f);
			setColorForSelections(g, view, canvas_loc);
			drawSubTickLoc(subtick_loc, scene_start, scene_end, g, center_line_start, canvas_loc);
			subtick_scaled_loc += subtick_scaled_increment;
		}
	}


	private void drawReverseSubTick(double rev_subtick_loc, double subtick_increment, double subtick_scaled_loc, double subtick_scaled_increment, Rectangle clipbox, Graphics g, ViewI view, double scene_start, double scene_end, int center_line_start) {
		//reversed map
		for (; rev_subtick_loc >= 0; rev_subtick_loc -= subtick_increment, subtick_scaled_loc -= subtick_scaled_increment) {
			int canvas_loc = (int) (subtick_scaled_loc + 0.5f);
			// Don't draw things which are out of the clipbox
			// This fixes an enormous performance drain. EEE-Sept 2000
			if (canvas_loc < clipbox.x) {
				break;
			}
			if (canvas_loc > clipbox.x + clipbox.width) {
				continue;
			}
			setColorForSelections(g, view, canvas_loc);
			drawSubTickLoc(rev_subtick_loc, scene_start, scene_end, g, center_line_start, canvas_loc);
		}
	}


	private void setColorForSelections(Graphics g, ViewI view, int canvas_loc) {
		if (selected_regions != null) {
			g.setColor(getForegroundColor());
			for (int[] select_range : selected_regions) {
				select_coord.x = select_range[0];
				select_coord.width = select_range[1] - select_range[0];
				view.transformToPixels(select_coord, select_pix);
				if (canvas_loc > select_pix.x && canvas_loc < (select_pix.x + select_pix.width)) {
					g.setColor(getBackgroundColor());
				}
			}
		}
	}


	private void setColorForSelections2(Graphics g, ViewI view, int center_line_start) {
		// Drawing selected major axis ticks and labels in red if selected
		if (selected_regions != null) {
			g.setColor(getBackgroundColor());
			for (int[] select_range : selected_regions) {
				select_coord.x = select_range[0];
				select_coord.width = select_range[1] - select_range[0];
				view.transformToPixels(select_coord, select_pix);
				g.fillRect(select_pix.x, center_line_start, select_pix.width, centerLineThickness);
			}
			g.setColor(getForegroundColor());
		}
	}



	private void drawTickLoc(double tick_loc, double scene_start, double scene_end, FontMetrics fm, String label, Graphics g, int center_line_start, int canvas_loc) {
		if (tick_loc >= scene_start && tick_loc <= scene_end) {
			if (orient == VERTICAL) {
				if (labelFormat != NO_LABELS) {
					if (LEFT == this.labelPlacement) {
						int x = fm.stringWidth(label);
						g.drawString(label, center_line_start - labelGap - x, canvas_loc);
					} else {
						g.drawString(label, center_line_start + labelShift, canvas_loc);
					}
				}
				g.fillRect(center_line_start - 2, canvas_loc, centerLineThickness + 4, 2);
			} else {
				if (labelFormat != NO_LABELS) {
					g.drawString(label, canvas_loc, center_line_start - labelShift);
				}
				g.fillRect(canvas_loc, center_line_start - 2, 2, centerLineThickness + 4);
			}
		}
	}


	private void drawSubTickLoc(double subtick_loc, double scene_start, double scene_end, Graphics g, int center_line_start, int canvas_loc) {
		if (subtick_loc >= scene_start && subtick_loc <= scene_end) {
			// this should put a tick subtick_size pixels tall tick above
			// the line, nothing below it
			if (orient == VERTICAL) {
				g.drawLine(center_line_start - subtick_size, canvas_loc, center_line_start, canvas_loc);
			} else {
				g.drawLine(canvas_loc, center_line_start - subtick_size, canvas_loc, center_line_start);
			}
		}

	}

	/**
	 * Represents a doubleing point number as a string.
	 * Output depends on the format set in {@link #setLabelFormat(int)}.
	 * <p>
	 * Gregg added this to deal with an annoying tendency
	 * of the <code>String.valueOf()</code> method in some JVM's
	 * to add extraneous precision.
	 * For example,
	 * <code>String.valueOf(1f)</code> returns "1.0".
	 * Not the desired "1".
	 *
	 * @param theNumber to convert
	 * @return a String representing the number.
	 */
	private String stringRepresentation(double theNumber, double theIncrement) {
		double double_label = theNumber / this.label_scale;
		int int_label;
		// This fix should be faster than checking the string
		if (theIncrement < 2) {
			// temp fix for Java doubleing-point to string conversion problems,
			// needs to be made more general at some point
			int_label = (int)(double_label * 1000 + 0.5);
			double_label = ((double)int_label)/1000;
			return String.valueOf(double_label);
		}
		else {
			int_label = (int)Math.round(double_label);
			if (ABBREV == this.labelFormat) {
				if (0 == int_label % 1000 && 0 != int_label) {
					int_label /= 1000;
					if (0 == int_label % 1000) {
						int_label /= 1000;
						if ( 0 == int_label % 1000) {
							int_label /= 1000;
							if ( 0 == int_label % 1000) {
								return comma_format.format(int_label) + "T";
							}
							return comma_format.format(int_label) + "G";
						}
						return comma_format.format(int_label) + "M";
					}
					return comma_format.format(int_label) + "k";
				}
				return comma_format.format(int_label);
			} else if (COMMA == this.labelFormat) {
				return comma_format.format(int_label);
			}
			else if (this.labelFormat == FULL)  {
				String str = Integer.toString(int_label);
				if (str.endsWith("000")) {
					str = str.substring(0, str.length()-3) + "kb";
				}
				return str;
			}
			return String.valueOf(int_label);
		}
	}


	private final double tickIncrement(double theUnitsPerPixel, double thePixelsPerUnit) {
		double result = 1;
		double increment = 1;
		double remainder;
		if (theUnitsPerPixel < 1)  {
			remainder = thePixelsPerUnit;
			while (remainder >= 10)  {
				remainder /= 10;
				increment *= 10;
				if (DEBUG_DRAW) System.err.println(" " + remainder + ", " + increment);
			}
			if (remainder >= 2)  {
				remainder /= 2;
				increment *= 2;
				if (remainder >= 2.5)  {
					remainder /= 2.5;
					increment *= 2.5;
				}
			}
			result = (100/increment);
		}
		else  {
			remainder = theUnitsPerPixel;

			// The COMMA format requires 25% more space to accomodate "," characters
			// The ABBREV format is hard to predict, so give it extra space as well
			if (labelFormat != FULL) { remainder *= 1.25; }

			while (remainder >= 10)  {
				remainder /= 10;
				increment *= 10;
				if (DEBUG_DRAW) System.err.println(" " + remainder + ", " + increment);
			}
			if (remainder >= 2.5)  {
				remainder /= 2.5;
				increment *= 2.5;
				if (remainder >= 2)  {
					remainder /= 2;
					increment *= 2;
				}
			}
			result = (increment * 200);
		}

		return result;
	}

	public void setHitable(boolean h) {
		this.hitable = h;
	}

	@Override
	public boolean isHitable() { return hitable; }

	@Override
	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		return isHitable() && pixel_hitbox.intersects(pixelbox);
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return isHitable() && coord_hitbox.intersects(coordbox);
	}
}
