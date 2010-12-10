/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.glyph.fhh;

import com.affymetrix.genoviz.bioviews.LinearTransform;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import com.affymetrix.genoviz.bioviews.ViewI;

/**
 *
 * @author Burim
 */
public class AxisGlyph extends com.affymetrix.genoviz.glyph.AxisGlyph{




	public AxisGlyph() {
		super();
	}



	public AxisGlyph(int orient) {
		super(orient);
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
		scratchpixels.x = (int)scratchcoords.x;
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

		//super.draw(view);
		if (DEBUG_DRAW) { System.err.println("leaving draw() for " + this); }
		DEBUG_DRAW = false;
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
	protected String stringRepresentation(double theNumber, double theIncrement) {
		double double_label = theNumber / this.label_scale;
		int int_label;
		// This fix should be faster than checking the string
		if (theIncrement < 2) {
			// temp fix for Java doubleing-point to string conversion problems,
			// needs to be made more general at some point
			int_label = (int)(double_label * 1000 + 0.5);
			double_label = ((double)int_label)/1000;
			return String.valueOf(Math.abs(double_label));
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
				return comma_format.format(Math.abs(int_label));
			} else if (COMMA == this.labelFormat) {
				return comma_format.format(Math.abs(int_label));
			}
			else if (this.labelFormat == FULL)  {
				String str = Integer.toString(Math.abs(int_label));
				if (str.endsWith("000")) {
					str = str.substring(0, str.length()-3) + "kb";
				}
				return str;
			}
			return String.valueOf(Math.abs(int_label));
		}
	}

	@Override
	public boolean withinView(ViewI view) {
		return true;
	}



}
