/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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
package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.View;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.util.Timer;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.*;
import java.awt.geom.Point2D;
import java.awt.font.TextAttribute;
import java.text.AttributedString;

/**
 *  An implementation of graphs for NeoMaps, capable of rendering graphs in a variety of styles
 *  Started with {@link com.affymetrix.genoviz.glyph.BasicGraphGlyph} and improved from there.
 *  ONLY MEANT FOR GRAPHS ON HORIZONTAL MAPS.
 */
public final class GraphGlyph extends Glyph {
	private static final boolean TIME_DRAWING = false;
	private static final boolean DEBUG = false;

	private static Font default_font = NeoConstants.default_plain_font;
	private static final Font axis_font = new Font("SansSerif", Font.PLAIN, 12);
	private static final NumberFormat nformat = new DecimalFormat();
	private static final int xpix_offset = 0;
	private static final Point zero_point = new Point(0, 0);
	private static final Point2D.Double coord = new Point2D.Double(0, 0);
	private static final Point curr_point = new Point(0, 0);
	private static final Point prev_point = new Point(0, 0);
	private static final Timer tim = new Timer();
	/**
	 *  point_max_ycoord is the max ycoord (in graph coords) of all points in graph.
	 *  This number is calculated in setPointCoords() directly fom ycoords, and cannot
	 *     be modified (except for resetting the points by calling setPointCoords() again)
	 */
	private float point_max_ycoord = Float.POSITIVE_INFINITY;
	private float point_min_ycoord = Float.NEGATIVE_INFINITY;
	// assumes sorted points, each x corresponding to y
	private final GraphSym graf;
	public static final int handle_width = 10;  // width of handle in pixels
	private static final int pointer_width = 10;
	private final Rectangle handle_pixbox = new Rectangle(); // caching rect for handle pixel bounds
	private final Rectangle pixel_hitbox = new Rectangle();  // caching rect for hit detection
	private final GraphState state;
	private final LinearTransform scratch_trans = new LinearTransform();

	// specified in coords_per_pixel

	// average # of points per entry in flat graph compression cache
	/*
	 *  may need to try a new approach to minimize switching graphics color
	 *  (testing is showing that graph draws ~5x _slower_ if have to alternate colors with
	 *    every graphics draw call...)
	 *  Therefore want to separate out for example the minmax bar drawing from the average line
	 *     drawing since they are different colors.  Could do this with two passes, but then
	 *     redoing a lot of summing, etc.
	 *    As an alternative, may try caching the average position for each pixel
	 *       (as doubles in graph coords, or maybe in pixel positions)
	 *
	 */
	private int[] pixel_avg_cache;
	private Color thresh_color;
	private static final int thresh_contig_height = 10;
	// in pixels, for calculating where to draw thresholded regions
	private static final int thresh_contig_yoffset = 2;
	private final Rectangle2D.Double thresh_coord_box = new Rectangle2D.Double();
	private ThreshGlyph thresh_glyph = new ThreshGlyph();
	private final Rectangle thresh_pix_box = new Rectangle();
	private static final double transition_scale = 500;


	private Color lighter;
	private Color darker;

	public float getXCoord(int i) {
		return graf.getGraphXCoord(i);
	}

	public float getYCoord(int i) {
		return graf.getGraphYCoord(i);
	}

	public boolean hasWidth() {
		return graf.hasWidth();
	}

	private int getWCoord(int i) {
		return graf.getGraphWidthCoord(i);
	}

	public int[] getWCoords() {
		return graf.getGraphWidthCoords();
	}

	/**
	 * Temporary helper method.
	 * 
	 */
	public float[] copyYCoords() {
		return graf.copyGraphYCoords();
	}

	public GraphGlyph(GraphSym graf, GraphState gstate) {
		super();
		state = gstate;

		setCoords(coordbox.x, state.getTierStyle().getY(), coordbox.width, state.getTierStyle().getHeight());

		Map<String,Object> map = graf.getProperties();
		boolean toInitialize = isUninitialized();
		
		setColorAndStyle(toInitialize, map);

		this.graf = graf;

		if (graf.getPointCount() == 0) {
			return;
		}

		checkVisibleBoundsY(toInitialize, map);

		/* Code below comes from old SmartGraphGlyph Constructor */
		setDrawOrder(Glyph.DRAW_SELF_FIRST);

		thresh_glyph.setVisibility(getShowThreshold());
		thresh_glyph.setSelectable(false);
		if (thresh_color != null) {
			thresh_glyph.setColor(thresh_color);
		}
		this.addChild(thresh_glyph);

		if (Float.isInfinite(getMinScoreThreshold()) && Float.isInfinite(getMaxScoreThreshold())) {
			setMinScoreThreshold(getVisibleMinY() + ((getVisibleMaxY() - getVisibleMinY()) / 2));
		}
		resetThreshLabel();
	}

	private void setColorAndStyle(boolean toInitialize, Map<String, Object> map) throws NumberFormatException {
		if (toInitialize && map != null) {
			Object value = map.get(ViewPropertyNames.INITIAL_COLOR);
			if (value != null) {
				setColor(Color.decode(value.toString()));
			} else {
				setColor(state.getTierStyle().getColor());
			}
			value = map.get(ViewPropertyNames.INITIAL_BACKGROUND);
			if (value != null) {
				setBackgroundColor(Color.decode(value.toString()));
			} else {
				setBackgroundColor(state.getTierStyle().getBackground());
			}
			value = map.get(ViewPropertyNames.INITIAL_GRAPH_STYLE);
			if (value != null) {
				setGraphStyle(GraphType.fromString(value.toString()));
			} else {
				setGraphStyle(state.getGraphStyle());
			}
		} else {
			setGraphStyle(state.getGraphStyle());
			setColor(state.getTierStyle().getColor());
		}
		//must call again to get it to properly render
		setColor(state.getTierStyle().getColor());
	}

	private boolean isUninitialized(){
		return isUninitializedMinY() ||
				isUninitializedMaxY();
	}

	private boolean isUninitializedMinY() {
		return getVisibleMinY() == Float.POSITIVE_INFINITY ||
				getVisibleMinY() == Float.NEGATIVE_INFINITY;
	}

	private boolean isUninitializedMaxY(){
		return getVisibleMaxY() == Float.POSITIVE_INFINITY ||
				getVisibleMaxY() == Float.NEGATIVE_INFINITY;
	}

	private void checkVisibleBoundsY(boolean toInitialize, Map<String,Object> map) {
		boolean rangeInit = false;
		if (toInitialize && map != null) {
			Object value = map.get(ViewPropertyNames.INITIAL_MAX_Y);
			if (value != null) {
				point_max_ycoord = Float.parseFloat(value.toString());
				rangeInit = true;
			}
			value = map.get(ViewPropertyNames.INITIAL_MIN_Y);
			if (value != null) {
				point_min_ycoord = Float.parseFloat(value.toString());
				rangeInit = true;
			}
		}
		if (!rangeInit) {
			float[] range = graf.getVisibleYRange();
			if (point_max_ycoord == Float.POSITIVE_INFINITY) {
				point_max_ycoord = range[1];
			}
			if (point_min_ycoord == Float.NEGATIVE_INFINITY) {
				point_min_ycoord = range[0];
			}
		}

		if (point_max_ycoord <= point_min_ycoord) {
			point_min_ycoord = point_max_ycoord - 1;
		}

		if(isUninitializedMinY()){
			setVisibleMinY(point_min_ycoord);
		}

		if(isUninitializedMaxY() && point_max_ycoord > 0){
			setVisibleMaxY(point_max_ycoord);
		}
		
	}


	public GraphState getGraphState() {
		return state;
	}

	private void oldDraw(ViewI view, GraphType graph_style) {
		if (TIME_DRAWING) {
			tim.start();
		}
		view.transformToPixels(coordbox, pixelbox);

		Graphics g = view.getGraphics();
		
		if (getShowGraph() && graf != null && graf.getPointCount() > 0) {
			DrawTheGraph(view, g, graph_style);
		}

		// drawing the "handle", which is the only part of the graph that recognizes hits
		// not a normal "child", so if it is hit then graph is considered to be hit...
		drawHandle(view);
		
		if (getShowAxis()) {
			drawAxisLabel(view);
		}

		// drawing outline around bounding box
		if (getShowBounds()) {
			g.setColor(Color.green);
			g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width - 1, pixelbox.height - 1);
		}

		if (getShowLabel()) {
			drawLabel(view);
		}
		if (TIME_DRAWING) {
			System.out.println("graph draw time: " + tim.read());
		}
	}

	private void DrawTheGraph(ViewI view, Graphics g, GraphType graph_style) {
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getTranslateY();
		Rectangle2D.Double view_coordbox = view.getCoordBox();
		double xmin = view_coordbox.x;
		double xmax = view_coordbox.x + view_coordbox.width;

		float yzero = determineYZero();
		coord.y = offset - ((yzero - getVisibleMinY()) * yscale);
		view.transformToPixels(coord, zero_point);

		if (yzero == 0) {
			// zero_point within min/max, so draw
			g.setColor(Color.gray);
			g.drawLine(pixelbox.x, zero_point.y, pixelbox.x + pixelbox.width, zero_point.y);
		}

		g.setColor(this.getColor());

		// set up prev_point before starting loop
		coord.x = graf.getMinXCoord();
		float prev_ytemp = graf.getFirstYCoord();
		coord.y = offset - ((prev_ytemp - getVisibleMinY()) * yscale);
		view.transformToPixels(coord, prev_point);

		Point max_x_plus_width = new Point(Integer.MIN_VALUE, Integer.MIN_VALUE);
		int draw_beg_index = graf.determineBegIndex(xmin);
		int draw_end_index = graf.determineEndIndex(xmax, draw_beg_index);
		
		g.translate(xpix_offset, 0);

		RenderingHints original_render_hints = null;
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			original_render_hints = g2.getRenderingHints();
			Map<Object, Object> my_render_hints = new HashMap<Object, Object>();
			my_render_hints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
			g2.addRenderingHints(my_render_hints);
		}

		Point curr_x_plus_width = new Point(0, 0);
		bigDrawLoop(draw_beg_index, draw_end_index, offset, yscale, view, curr_x_plus_width, graph_style, g, max_x_plus_width);

		g.translate(-xpix_offset, 0);
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D) g;
			if (original_render_hints != null) {
				g2.setRenderingHints(original_render_hints);
			}
		}

	}


	private void bigDrawLoop(
			int draw_beg_index, int draw_end_index, double offset, double yscale, ViewI view, Point curr_x_plus_width,
			GraphType graph_style, Graphics g, Point max_x_plus_width) {
		for (int i = draw_beg_index; i <= draw_end_index; i++) {
			// flipping about yaxis... should probably make this optional
			// also offsetting to place within glyph bounds
			int xtemp = graf.getGraphXCoord(i);
			coord.x = xtemp;
			float ytemp = graf.getGraphYCoord(i);
			if (Double.isNaN(ytemp) || Double.isInfinite(ytemp)) {
				return;
			}
			// flattening any points > getVisibleMaxY() or < getVisibleMinY()...
			ytemp = Math.min(ytemp, getVisibleMaxY());
			ytemp = Math.max(ytemp, getVisibleMinY());

			coord.y = offset - ((ytemp - getVisibleMinY()) * yscale);
			view.transformToPixels(coord, curr_point);
			if (graf.hasWidth()) {
				Point2D.Double x_plus_width2D = new Point2D.Double(0, 0);
				x_plus_width2D.x = xtemp + this.getWCoord(i);
				x_plus_width2D.y = coord.y;
				view.transformToPixels(x_plus_width2D, curr_x_plus_width);
			}
			if (graph_style == GraphType.LINE_GRAPH) {
				if (!graf.hasWidth()) {
					g.drawLine(prev_point.x, prev_point.y, curr_point.x, curr_point.y);
				} else {
					// Draw a line representing the width: (x,y) to (x + width,y)
					g.drawLine(curr_point.x, curr_point.y, curr_x_plus_width.x, curr_x_plus_width.y);
					// Usually draw a line from (xA + widthA,yA) to next (xB,yB), but when there
					// are overlapping spans, only do this from the largest previous (x+width) value
					// to an xA that is larger than that.
					if (curr_point.x >= max_x_plus_width.x && max_x_plus_width.x != Integer.MIN_VALUE) {
						g.drawLine(max_x_plus_width.x, max_x_plus_width.y, curr_point.x, curr_point.y);
					}
					if (curr_x_plus_width.x >= max_x_plus_width.x) {
						max_x_plus_width.x = curr_x_plus_width.x; // xB + widthB
						max_x_plus_width.y = curr_x_plus_width.y; // yB
					}
				}
			} else if (graph_style == GraphType.BAR_GRAPH) {
				int ymin_pixel = Math.min(curr_point.y, zero_point.y);
				int yheight_pixel = Math.abs(curr_point.y - zero_point.y);
				yheight_pixel = Math.max(1, yheight_pixel);
				if (!graf.hasWidth()) {
					g.drawLine(curr_point.x, ymin_pixel, curr_point.x, ymin_pixel + yheight_pixel);
				} else {
					final int width = Math.max(1, curr_x_plus_width.x - curr_point.x);
					g.drawRect(curr_point.x, ymin_pixel, width, yheight_pixel);
				}
			} else if (graph_style == GraphType.DOT_GRAPH) {
				if (!graf.hasWidth()) {
					g.drawLine(curr_point.x, curr_point.y, curr_point.x, curr_point.y); // point
				} else {
					g.drawLine(curr_point.x, curr_point.y, curr_x_plus_width.x, curr_point.y);
				}
			} else if (graph_style == GraphType.STAIRSTEP_GRAPH) {
				int stairwidth = curr_point.x - prev_point.x;
				if (stairwidth >= 0 && stairwidth <= 10000 && (i == 0 || graf.getGraphYCoord(i - 1) != 0)) {
					// skip drawing if width > 10000... (fix for linux problem?)
					// draw the same regardless of whether wcoords == null
					drawRectOrLine(g, prev_point.x,
							Math.min(zero_point.y, prev_point.y),
							Math.max(1, stairwidth),
							Math.max(1, Math.abs(prev_point.y - zero_point.y)));
				}
				// If this is the very last point, special rules apply
				if (i == draw_end_index) {
					stairwidth = (!graf.hasWidth()) ? 1 : curr_x_plus_width.x - curr_point.x;
					drawRectOrLine(g, curr_point.x,
							Math.min(zero_point.y, curr_point.y),
							Math.max(1, stairwidth),
							Math.max(1, Math.abs(curr_point.y - zero_point.y)));
				}
			} else if (graph_style == GraphType.HEAT_MAP) {
				double heatmap_scaling = (double) (state.getHeatMap().getColors().length - 1) / (getVisibleMaxY() - getVisibleMinY());
				int heatmap_index = (int) (heatmap_scaling * (ytemp - getVisibleMinY()));
				if (heatmap_index < 0) {
					heatmap_index = 0;
				} else if (heatmap_index > 255) {
					heatmap_index = 255;
				}
				g.setColor(state.getHeatMap().getColor(heatmap_index));
				drawRectOrLine(g, curr_point.x, pixelbox.y, Math.max(1, curr_x_plus_width.x - curr_point.x), pixelbox.height + 1);
			}
			prev_point.x = curr_point.x;
			prev_point.y = curr_point.y;
		}
	}

	private void drawLabel(ViewI view) {
		Rectangle hpix = calcHandlePix(view);
		Graphics g = view.getGraphics();
		g.setColor(this.getColor());
		g.setFont(default_font);
		FontMetrics fm = g.getFontMetrics();
		g.drawString(getLabel(), (hpix.x + hpix.width + 1), (hpix.y + fm.getMaxAscent() - 1));
	}

	private void drawHandle(ViewI view) {
		Rectangle hpix = calcHandlePix(view);
		if (hpix != null) {
			Graphics g = view.getGraphics();
			g.setColor(this.getColor());
			drawRectOrLine(g,hpix.x, hpix.y, hpix.width, hpix.height);
		}
	}

	private void drawAxisLabel(ViewI view) {
		if (GraphState.isHeatMapStyle(getGraphStyle())) {
			return;
		}

		Rectangle hpix = calcHandlePix(view);

		Graphics g = view.getGraphics();
		g.setColor(this.getColor());
		g.setFont(axis_font);
		FontMetrics fm = g.getFontMetrics();
		int font_height = fm.getHeight();
		double last_pixel = Double.NaN; // the y-value at which the last tick String was drawn

		Double[] tick_coords = determineYTickCoords();
		double[] tick_pixels = convertToPixels(view, tick_coords);
		for (int i = 0; i < tick_pixels.length; i++) {
			double mark_ypix = tick_pixels[i];
			drawRectOrLine(g,hpix.x, (int) mark_ypix, hpix.width + 8, 1);
			// Always draw the lowest tick value, and indicate the others only
			// if there is enough room between them that the text won't overlap
			if (Double.isNaN(last_pixel) || Math.abs(mark_ypix - last_pixel) > font_height) {
				AttributedString minString = new AttributedString(nformat.format(tick_coords[i]));
				minString.addAttribute(TextAttribute.BACKGROUND, state.getTierStyle().getBackground());
				minString.addAttribute(TextAttribute.FOREGROUND, lighter);
				minString.addAttribute(TextAttribute.FONT, axis_font);
				g.drawString(minString.getIterator(), hpix.x + 15, (int) mark_ypix + fm.getDescent());
				last_pixel = mark_ypix;
			}
		}

	}

	/** Creates an array of about 4 to 10 coord values evenly spaced between 
	 * {@link #getVisibleMinY()} and {@link #getVisibleMaxY()}. 
	 */
	private Double[] determineYTickCoords() {
		float min = getVisibleMinY();
		float max = getVisibleMaxY();
		return determineYTickCoords(min, max);
	}

	/** Creates an array of about 4 to 10 coord values evenly spaced between min and max. */
	private static Double[] determineYTickCoords(double min, double max) {
		double range = max - min;
		double interval = Math.pow(10, Math.floor(Math.log10(range)));
		double start = Math.floor(min / interval) * interval;

		List<Double> coords = new ArrayList<Double>(10);
		for (double d = start; d <= max; d += interval) {
			if (d >= min && d <= max) {
				coords.add(d);
			}
		}

		// If there are not at least 4 ticks, then
		if (coords.size() < 4) { // try original interval divided by 2
			coords.clear();
			interval = interval / 2;
			start = Math.floor(min / interval) * interval;
			for (double d = start; d <= max; d += interval) {
				if (d >= min && d <= max) {
					coords.add(d);
				}
			}
		}

		// If there are not at least 4 ticks, then
		if (coords.size() < 4) { // take original interval divided by 5
			coords.clear();
			interval = (2 * interval) / 5;
			start = Math.floor(min / interval) * interval;
			for (double d = start; d <= max; d += interval) {
				if (d >= min && d <= max) {
					coords.add(d);
				}
			}
		}

		return coords.toArray(new Double[coords.size()]);
	}

	/** Calculate tick pixel positions based on tick coord positions. */
	private double[] convertToPixels(ViewI view, Double[] y_coords) {
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double yoffset = scratch_trans.getTranslateY();

		double[] y_pixels = new double[y_coords.length];
		for (int i = 0; i < y_coords.length; i++) {
			double tickY = y_coords[i];

			coord.y = yoffset - ((tickY - getVisibleMinY()) * yscale);
			view.transformToPixels(coord, curr_point);
			y_pixels[i] = curr_point.y;
		}
		return y_pixels;
	}

	/** Draws the outline in a way that looks good for tiers.  With other glyphs,
	 *  the outline is usually drawn a pixel or two larger than the glyph.
	 *  With TierGlyphs, it is better to draw the outline inside of or contiguous
	 *  with the glyph borders.
	 *  This method assumes the tiers are horizontal.
	 *  The left and right border are taken from the view's pixel box,
	 *  the top and bottom border are from the coord box.
	 **/
	@Override
	protected void drawSelectedOutline(ViewI view) {
		draw(view);
		Rectangle view_pixbox = view.getPixelBox();
		Graphics g = view.getGraphics();
		Color sel_color = view.getScene().getSelectionColor();
		g.setColor(sel_color);
		view.transformToPixels(getPositiveCoordBox(), pixelbox);

		// only outline the handle, not the whole graph
		g.drawRect(view_pixbox.x, pixelbox.y,
				handle_width - 1, pixelbox.height - 1);
		g.drawRect(view_pixbox.x + 1, pixelbox.y + 1,
				handle_width - 3, pixelbox.height - 3);

		// also draw a little pointing triangle to make the selection stand-out more
		int[] xs = {view_pixbox.x + handle_width,
			view_pixbox.x + handle_width + pointer_width,
			view_pixbox.x + handle_width};
		int[] ys = {pixelbox.y,
			pixelbox.y + (int) (0.5 * (pixelbox.height - 1)),
			pixelbox.y + pixelbox.height - 1};
		Color c = new Color(sel_color.getRed(), sel_color.getGreen(), sel_color.getBlue(), 128);
		g.setColor(c);
		g.fillPolygon(xs, ys, 3);
	}

	@Override
	public void moveRelative(double xdelta, double ydelta) {
		super.moveRelative(xdelta, ydelta);
		state.getTierStyle().setHeight(coordbox.height);
		state.getTierStyle().setY(coordbox.y);
		if (xdelta != 0.0f) {
			graf.moveX(xdelta);
		}
	}

	@Override
	public void setCoords(double newx, double newy, double newwidth, double newheight) {
		super.setCoords(newx, newy, newwidth, newheight);
		state.getTierStyle().setHeight(newheight);
		state.getTierStyle().setY(newy);
	}

	/**
	 *  Designed to work in combination with pickTraversal().
	 *  If called outside of pickTraversal(), may get the wrong answer
	 *      since won't currently take account of nested transforms, etc.
	 */
	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
		// within bounds of graph ?
		if (isVisible() && coord_hitbox.intersects(coordbox)) {
			// overlapping handle ?  (need to do this one in pixel space?)
			view.transformToPixels(coord_hitbox, pixel_hitbox);
			Rectangle hpix = calcHandlePix(view);
			if (hpix.intersects(pixel_hitbox)) {
				return true;
			}
		}
		return false;
	}

	private Rectangle calcHandlePix(ViewI view) {
		// could cache pixelbox of handle, but then will have problems if try to
		//    have multiple views on same scene / glyph hierarchy
		// therefore reconstructing handle pixel bounds here... (although reusing same object to
		//    cut down on object creation)

		// if full view differs from current view, and current view doesn't left align with full view,
		//   don't draw handle (only want handle at left side of full view)
		if (view.getFullView().getCoordBox().x != view.getCoordBox().x) {
			return null;
		}
		view.transformToPixels(coordbox, pixelbox);
		Rectangle view_pixbox = view.getPixelBox();
		int xbeg = Math.max(view_pixbox.x, pixelbox.x);
		Graphics g = view.getGraphics();
		g.setFont(default_font);
		handle_pixbox.setBounds(xbeg, pixelbox.y, handle_width, pixelbox.height);

		return handle_pixbox;
	}

	/**
	 *  getGraphMaxY() returns max ycoord (in graph coords) of all points in graph.
	 *  This number is calculated in setPointCoords() directly from ycoords, and cannot
	 *     be modified (except for resetting the points by calling setPointCoords() again)
	 */
	float getGraphMinY() {
		return point_min_ycoord;
	}

	float getGraphMaxY() {
		return point_max_ycoord;
	}

	/**
	 *  getVisibleMaxY() returns max ycoord (in graph coords) that is visible (rendered).
	 *  This number can be modified via calls to setVisibleMaxY, and the visual effect is
	 *     to threshold the graph drawing so that any points above max_ycoord render as max_ycoord
	 */
	public float getVisibleMaxY() {
		return state.getVisibleMaxY();
	}

	public float getVisibleMinY() {
		return state.getVisibleMinY();
	}

	/**
	 *  If want to override default setting of y min and max (based on setPointCoords()),
	 *    then must call setMaxY() and setMinY() _after_ call to setPointCoords() --
	 *    any subsequent call to setPointCoords will again reset y min and max.
	 */
	public void setVisibleMaxY(float ymax) {
		state.setVisibleMaxY(ymax);
	}

	/**
	 *  If want to override default setting of y min and max (based on setPointCoords()),
	 *    then must call setMaxY() and setMinY() _after_ call to setPointCoords() --
	 *    any subsequent call to setPointCoords will again reset y min and max.
	 */
	public void setVisibleMinY(float ymin) {
		state.setVisibleMinY(ymin);
	}

	@Override
	public void setColor(Color c) {
		setBackgroundColor(c);
		setForegroundColor(c);
		state.getTierStyle().setColor(c);
	}

	public String getLabel() {
		String lab = state.getTierStyle().getHumanName();
		// If it has a combo style and that is collapsed, then only use the label
		// from the combo style.  Otherwise use the individual tier style.
		if (state.getComboStyle() != null && state.getComboStyle().getCollapsed()) {
			lab = state.getComboStyle().getHumanName();
		}
		if (lab == null) {
			// if no label was set, try using ID
			Object mod = this.getInfo();
			if (mod instanceof SeqSymmetry) {
				lab = ((SeqSymmetry) mod).getID();
			}
			if (lab == null) {
				lab = state.getTierStyle().getUniqueName();
			}
		}

		lab += " (" + nformat.format(state.getVisibleMinY()) + ", " + nformat.format(state.getVisibleMaxY()) + ")";

		return lab;
	}

	public boolean getShowGraph() {
		return state.getShowGraph();
	}

	public boolean getShowBounds() {
		return state.getShowBounds();
	}

	public boolean getShowLabel() {
		return state.getShowLabel();
	}

	public boolean getShowAxis() {
		return state.getShowAxis();
	}

	public void setShowAxis(boolean b) {
		state.setShowAxis(b);
	}

	public void setShowGraph(boolean show) {
		state.setShowGraph(show);
	}

	void setShowBounds(boolean show) {
		state.setShowBounds(show);
	}

	public void setShowLabel(boolean show) {
		state.setShowLabel(show);
	}

	

	@Override
	public void setBackgroundColor(Color col) {
		super.setBackgroundColor(col);
		lighter = col.brighter();
		darker = col.darker();
		thresh_color = darker.darker();
		if (thresh_glyph != null) {
			thresh_glyph.setColor(thresh_color);
		}
	}

	public void setGraphStyle(GraphType type) {
		state.setGraphStyle(type);
		if (type == GraphType.HEAT_MAP) {
			setHeatMap(state.getHeatMap());
		}
	}

	public GraphType getGraphStyle() {
		return state.getGraphStyle();
	}

	public int[] getXCoords() {
		return graf.getGraphXCoords();
	}

	public int getPointCount() {
		return graf.getPointCount();
	}

	public void setHeatMap(HeatMap hmap) {
		state.setHeatMap(hmap);
	}

	public HeatMap getHeatMap() {
		return state.getHeatMap();
	}

	@Override
	public void getChildTransform(ViewI view, LinearTransform trans) {
		double external_yscale = trans.getScaleY();
		double external_offset = trans.getTranslateY();
		double internal_yscale = coordbox.height / (getVisibleMaxY() - getVisibleMinY());
		double internal_offset = coordbox.y + coordbox.height;
		double new_yscale = internal_yscale * external_yscale * -1;
		double new_yoffset =
				(external_yscale * internal_offset) +
				(external_yscale * internal_yscale * getVisibleMinY()) +
				external_offset;
		trans.setTransform(trans.getScaleX(),0,0,new_yscale,trans.getTranslateX(),new_yoffset);
	}

	private double getUpperYCoordInset(ViewI view) {
		double top_ycoord_inset = 0;
		if (getShowLabel()) {
			Graphics g = view.getGraphics();
			g.setFont(default_font);
			FontMetrics fm = g.getFontMetrics();
			Rectangle label_pix_box = new Rectangle(0,fm.getAscent() + fm.getDescent());
			Rectangle2D.Double label_coord_box = new Rectangle2D.Double();
			view.transformToCoords(label_pix_box, label_coord_box);
			top_ycoord_inset = label_coord_box.height;
		}
		return top_ycoord_inset;
	}

	/**
	 *  Same as GraphGlyph.getInternalLinearTransform(), except
	 *  also calculates a bottom y offset for showing thresholded
	 *  regions, if showThresholdedRegions() == true.
	 */
	private double getLowerYCoordInset(ViewI view) {
		/* This original super to this function had had its return value
		 * changed from 0 to 5 by GAH 3-21-2005.  bottom_ycoord_inset
		 * is set to five to mirror the original call to super */
		double bottom_ycoord_inset = 5;
		if (getShowThreshold()) {
			thresh_pix_box.height = thresh_contig_height + thresh_contig_yoffset;
			view.transformToCoords(thresh_pix_box, thresh_coord_box);
			bottom_ycoord_inset += thresh_coord_box.height;
		}
		return bottom_ycoord_inset;
	}

	private void getInternalLinearTransform(ViewI view, LinearTransform lt) {
		double top_ycoord_inset = getUpperYCoordInset(view);
		double bottom_ycoord_inset = getLowerYCoordInset(view);

		double num = getVisibleMaxY() - getVisibleMinY();
		if (num <= 0) {
			num = 0.1;
		} // if scale is 0 or negative, set to a small default instead

		double yscale = (coordbox.height - top_ycoord_inset - bottom_ycoord_inset) / num;
		double yoffset = coordbox.y + coordbox.height - bottom_ycoord_inset;
		lt.setTransform(lt.getScaleX(),0,0,yscale,lt.getTranslateX(),yoffset);
	}


	private void drawSmart(ViewI view) {
		if (getGraphStyle() == GraphType.MINMAXAVG) {
			// could size cache to just the view's pixelbox, but then may end up creating a
			//   new int array every time the pixelbox changes (which with view damage or
			//   scrolling optimizations turned on could be often)
			int comp_ysize = ((View) view).getComponentSize().width;
			// could check for exact match with comp_ysize, but allowing larger comp size here
			//    may be good for multiple maps that share the same scene, so that new int array
			//    isn't created every time paint switches from mapA to mapB -- the array will
			//    be reused and be the length of the component with greatest width...
			if ((pixel_avg_cache == null) || (pixel_avg_cache.length < comp_ysize)) {
				pixel_avg_cache = new int[comp_ysize];
			}
			Arrays.fill(pixel_avg_cache, 0, comp_ysize - 1, Integer.MIN_VALUE);
		}

		if (TIME_DRAWING) {
			tim.start();
		}
		view.transformToPixels(coordbox, pixelbox);
		
		if (getShowGraph()) {
			drawGraph(view);
		}

		drawHandle(view);
		
		if (getShowAxis()) {
			drawAxisLabel(view);
		}
		// drawing outline around bounding box
		if (getShowBounds()) {
			Graphics g = view.getGraphics();
			g.setColor(Color.green);
			g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width - 1, pixelbox.height - 1);
		}
		if (getShowLabel()) {
			drawLabel(view);
		}
		if (TIME_DRAWING) {
			long drawtime = tim.read();
			System.out.println("graph draw time: " + drawtime);
		}
	}

	private void drawGraph(ViewI view) {
		if (this.getPointCount() == 0) {
			return;
		}
		GraphType graph_style = getGraphStyle();
		view.transformToPixels(coordbox, pixelbox);
		Graphics g = view.getGraphics();
		double coords_per_pixel = 1.0F / ( view.getTransform()).getScaleX();
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getTranslateY();
		
		// plot_top_ypixel and plot_bottom_ypixel are replacements for pixelbox.y and pbox_yheight in many
		//   (but not all) calculations, they take into account an internal transform to shrink the graph rendering
		//   if necessary to allow space for the graph label and thresholded regions
		// plot_top_ypixel is "top" y pixel position allocated to plot rendering
		// plot_bottom_ypixel is "bottom" y pixel position allocated to plot rendering
		// since y pixel addressing in Graphics is numbered in increasing order from top,
		//         plot_top_ypixel < plot_bottom_ypixel
		//    this is a little confusing because it means as graph values decrease, pixel position increases
		//    a better way to think of this is:
		//        plot_top_ypixel = pixel position of graph.getVisibleMaxY()
		//        plot_bottom_ypixel = pixel position of graph.getVisibleMinY();
		coord.y = offset - ((getVisibleMaxY() - getVisibleMinY()) * yscale);
		Point scratch_point = new Point(0, 0);
		view.transformToPixels(coord, scratch_point);
		int plot_top_ypixel = scratch_point.y;
		// replaces pixelbox.y
		coord.y = offset;
		view.transformToPixels(coord, scratch_point);
		int plot_bottom_ypixel = scratch_point.y;
		// replaces pbox_yheight
		
		float yzero = determineYZero();
		coord.y = offset - ((yzero - getVisibleMinY()) * yscale);
		view.transformToPixels(coord, zero_point);
		if (graph_style == GraphType.MINMAXAVG || graph_style == GraphType.LINE_GRAPH) {
			if (yzero == 0) {
				g.setColor(Color.gray);
				g.drawLine(pixelbox.x, zero_point.y, pixelbox.width, zero_point.y);
			}
			if (graph_style == GraphType.MINMAXAVG) {
				g.setColor(darker);
			} else {
				g.setColor(getBackgroundColor());
			}
		}
		DrawPoints(offset, yscale, view, graph_style, g, plot_bottom_ypixel, plot_top_ypixel);
		if (graph_style == GraphType.MINMAXAVG) {
			DrawAvgLine(g, coords_per_pixel);
		}
	}

	private void DrawPoints(double offset, double yscale, ViewI view, GraphType graph_style, Graphics g, int plot_bottom_ypixel, int plot_top_ypixel) {
		Rectangle2D.Double view_coordbox = view.getCoordBox();
		double xmin = view_coordbox.x;
		double xmax = view_coordbox.x + view_coordbox.width;

		int draw_beg_index = graf.determineBegIndex(xmin);
		coord.x = graf.getGraphXCoord(draw_beg_index);
		coord.y = offset - ((graf.getGraphYCoord(draw_beg_index) - getVisibleMinY()) * yscale);

		view.transformToPixels(coord, prev_point);
		int ymin_pixel = prev_point.y;
		int ymax_pixel = prev_point.y;
		int ysum = prev_point.y;
		int points_in_pixel = 1;
		if (graph_style == GraphType.MINMAXAVG) {
			g.setColor(darker);
		}
		if (graph_style == GraphType.LINE_GRAPH) {
			g.setColor(getBackgroundColor());
		}

		int draw_end_index = graf.determineEndIndex(xmax, draw_beg_index);
		for (int i = draw_beg_index; i <= draw_end_index; i++) {
			int xtemp = graf.getGraphXCoord(i);
			float ytemp = graf.getGraphYCoord(i);
			// flattening any points > getVisibleMaxY() or < getVisibleMinY()...
			ytemp = Math.min(ytemp, getVisibleMaxY());
			ytemp = Math.max(ytemp, getVisibleMinY());

			coord.x = xtemp;
			coord.y = offset - ((ytemp - getVisibleMinY()) * yscale);
			view.transformToPixels(coord, curr_point);

			if (prev_point.x == curr_point.x) {
				ymin_pixel = Math.min(ymin_pixel, curr_point.y);
				ymax_pixel = Math.max(ymax_pixel, curr_point.y);
				ysum += curr_point.y;
				points_in_pixel++;
			} else {
				// draw previous pixel position
				drawSingleRect(
						ymin_pixel, plot_bottom_ypixel, plot_top_ypixel, ymax_pixel, graph_style, g, ysum, points_in_pixel, i);

				ymin_pixel = curr_point.y;
				ymax_pixel = curr_point.y;
				ysum = curr_point.y;
				points_in_pixel = 1;
			}
			prev_point.x = curr_point.x;
			prev_point.y = curr_point.y;
		}
		/* draw last pixel position */
		drawSingleRect(ymin_pixel, plot_bottom_ypixel, plot_top_ypixel, ymax_pixel, graph_style, g, ysum, points_in_pixel, draw_end_index);
	}


	private void drawSingleRect(
			int ymin_pixel, int plot_bottom_ypixel, int plot_top_ypixel, int ymax_pixel, GraphType graph_style, Graphics g, int ysum, int points_in_pixel, int i) {
		int ystart = Math.max(Math.min(ymin_pixel, plot_bottom_ypixel), plot_top_ypixel);
		int yend = Math.min(Math.max(ymax_pixel, plot_top_ypixel), plot_bottom_ypixel);
		if (graph_style == GraphType.HEAT_MAP) {
			double heatmap_scaling = 1;
			if (state.getHeatMap() != null) {
				Color[] heatmap_colors = state.getHeatMap().getColors();
				// scale based on pixel position, not cooord position, since most calculations below are in pixels
				heatmap_scaling = (double) (heatmap_colors.length - 1) / (-plot_top_ypixel + plot_bottom_ypixel);
			}
			g.setColor(state.getHeatMap().getColor((int) (heatmap_scaling * (plot_bottom_ypixel - ystart))));
			drawRectOrLine(g, prev_point.x, plot_top_ypixel, 1, plot_bottom_ypixel - plot_top_ypixel);
			return;
		}

		if (graph_style == GraphType.MINMAXAVG) {
			// cache for drawing later
			if (prev_point.x > 0 && prev_point.x < pixel_avg_cache.length) {
				int yavg_pixel = ysum / points_in_pixel;
				pixel_avg_cache[prev_point.x] = Math.min(Math.max(yavg_pixel, plot_top_ypixel), plot_bottom_ypixel);
			}
		}

		drawRectOrLine(g, prev_point.x, ystart, 1, yend - ystart);

		if (graph_style == GraphType.LINE_GRAPH && i > 0) {
			int y1 = Math.min(Math.max(prev_point.y, plot_top_ypixel), plot_bottom_ypixel);
			int y2 = Math.min(Math.max(curr_point.y, plot_top_ypixel), plot_bottom_ypixel);
			g.drawLine(prev_point.x, y1, curr_point.x, y2);
		}
	}


	private void DrawAvgLine(Graphics g, double coords_per_pixel) {
		g.setColor(lighter);
		int prev_index = 0;
		// find the first pixel position that has a real value in pixel_cache
		while ((prev_index < pixel_avg_cache.length) && (pixel_avg_cache[prev_index] == Integer.MIN_VALUE)) {
			prev_index++;
		}	
		for (int i = prev_index + 1; i < pixel_avg_cache.length; i++) {
			// successfully found a real value in pixel cache
			int yval = pixel_avg_cache[i];
			if (yval == Integer.MIN_VALUE) {
				continue;
			}
			if (pixel_avg_cache[i - 1] == Integer.MIN_VALUE && coords_per_pixel > 30) {
				g.drawLine(i, yval, i, yval);
			} else {
				// last pixel had at least one datapoint, so connect with line
				g.drawLine(prev_index, pixel_avg_cache[prev_index], i, yval);
			}
			prev_index = i;
		}
	}


	/**
	 * Draws thresholded regions.
	 * Current set up so that if regions_parent != null, then instead of drawing to view,
	 * populate regions_parent with child SeqSymmetries for each region that passes threshold,
	 */
	void drawThresholdedRegions(ViewI view, MutableSeqSymmetry region_holder, BioSeq aseq) {
		double max_gap_threshold = getMaxGapThreshold();
		double min_run_threshold = getMinRunThreshold();
		double span_start_shift = getThreshStartShift();
		double span_end_shift = getThreshEndShift();
		int thresh_direction = getThresholdDirection();
		float min_score_threshold = Float.NEGATIVE_INFINITY;
		float max_score_threshold = Float.POSITIVE_INFINITY;
		if (thresh_direction == GraphState.THRESHOLD_DIRECTION_GREATER) {
			min_score_threshold = getMinScoreThreshold();
		} else if (thresh_direction == GraphState.THRESHOLD_DIRECTION_LESS) {
			max_score_threshold = getMaxScoreThreshold();
		} else if (thresh_direction == GraphState.THRESHOLD_DIRECTION_BETWEEN) {
			min_score_threshold = getMinScoreThreshold();
			max_score_threshold = getMaxScoreThreshold();
		}
		// if neither min or max score thresholds have been set, assume that only using
		//     min score threshold and set so it is in the middle of visible score range
		if (Float.isInfinite(min_score_threshold) && Float.isInfinite(max_score_threshold)) {
			setMinScoreThreshold(getVisibleMinY() + ((getVisibleMaxY() - getVisibleMinY()) / 2));
			min_score_threshold = getMinScoreThreshold();
			max_score_threshold = Float.POSITIVE_INFINITY;
		}
		
		int draw_beg_index = 0;
		int draw_end_index;
		boolean make_syms = (region_holder != null) && (aseq != null);
		if (make_syms) {
			draw_end_index = this.getPointCount() - 1;
		} else {
			Rectangle2D.Double view_coordbox = view.getCoordBox();
			double xmin = view_coordbox.x;
			double xmax = view_coordbox.x + view_coordbox.width;
			draw_beg_index = graf.determineBegIndex(xmin);
			draw_end_index = graf.determineEndIndex(xmax, draw_beg_index);
		}
		double thresh_ycoord;
		double thresh_score;
		if (!Float.isInfinite(min_score_threshold)) {
			thresh_score = min_score_threshold;
		} else if (!Float.isInfinite(max_score_threshold)) {
			thresh_score = max_score_threshold;
		} else {
			System.out.println("in SmartGraphGlyph.drawThresholdedRegions(), problem with setting up threshold line!");
			thresh_score = (getVisibleMinY() + (getVisibleMaxY() / 2));
		}
		thresh_glyph.setVisibility(thresh_score >= getVisibleMinY() && thresh_score <= getVisibleMaxY());
		thresh_ycoord = getCoordValue(view, (float) thresh_score);
		thresh_glyph.setCoords(coordbox.x, thresh_ycoord, coordbox.width, 1);
		Graphics g = view.getGraphics();
		g.setColor(lighter);

		int pass_thresh_start = 0;
		int pass_thresh_end = 0;
		boolean pass_threshold_mode = false;
		int min_index = 0;
		int max_index = this.getPointCount() - 1;
		// need to widen range searched to include previous and next points out of view that
		//   pass threshold (unless distance to view is > max_gap_threshold
		int new_beg = draw_beg_index;
		int minX = graf.getGraphXCoord(draw_beg_index);
		// GAH 2006-02-16 changed to <= max_gap instead of <, to better mirror Affy tiling array pipeline
		while ((new_beg > min_index) && ((minX - graf.getGraphXCoord(new_beg)) <= max_gap_threshold)) {
			new_beg--;
		}

		draw_beg_index = new_beg;
		int new_end = draw_end_index;
		boolean draw_previous = false;
		int maxX = graf.getGraphXCoord(draw_end_index);
		// GAH 2006-02-16 changed to <= max_gap instead of <, to better mirror Affy tiling array pipeline
		while ((new_end < max_index) && ((graf.getGraphXCoord(new_end) - maxX) <= max_gap_threshold)) {
			new_end++;
		}
		draw_end_index = new_end;
		if (draw_end_index >= this.getPointCount()) {
			draw_end_index = this.getPointCount() - 1;
		}
		// eight possible states:
		//
		//     pass_threshold_mode    [y >= min_score_threshold]   [x-pass_thresh_end <= max_dis_thresh]
		//
		//  prune previous region and draw when:
		//      true, false, false
		//      true, true, false
		for (int i = draw_beg_index; i <= draw_end_index; i++) {
			int x = graf.getGraphXCoord(i);
			int w = graf.hasWidth() ? graf.getGraphWidthCoord(i) : 0;
			double y = graf.getGraphYCoord(i);
			// GAH 2006-02-16 changed to > min_score instead of >= min_score, to better mirror Affy tiling array pipeline
			boolean pass_score_thresh = ((y > min_score_threshold) && (y <= max_score_threshold));
			boolean passes_max_gap = ((x - pass_thresh_end) <= max_gap_threshold);
			if (pass_threshold_mode) {
				if (!passes_max_gap) {
					draw_previous = true;
				} else if (pass_score_thresh) {
					pass_thresh_end = x + w;
				}
			} else {
				if (pass_score_thresh) {
					// switch into pass_threshold_mode
					// don't need to worry about distance thresh here
					pass_thresh_start = x;
					pass_thresh_end = x + w;
					pass_threshold_mode = true;
				}
			}
			if (draw_previous) {
				drawPrevious(pass_thresh_start, span_start_shift, pass_thresh_end, span_end_shift, min_run_threshold, view, make_syms, aseq, region_holder, g);
				draw_previous = false;
				pass_threshold_mode = pass_score_thresh;
				if (pass_score_thresh) {
					// current point passes threshold test, start new region scan
					pass_thresh_start = x;
					pass_thresh_end = x + w;
				}
			}
		}
		// clean up by doing a draw if exited loop while still in pass_threshold_mode
		if (pass_threshold_mode && (pass_thresh_end != pass_thresh_start)) {
			drawPrevious2(pass_thresh_start, span_start_shift, pass_thresh_end, span_end_shift, min_run_threshold, view, make_syms, aseq, region_holder, g);
		}
	}


	private void drawPrevious(int pass_thresh_start, double span_start_shift, int pass_thresh_end, double span_end_shift, double min_run_threshold, ViewI view, boolean make_syms, BioSeq aseq, MutableSeqSymmetry region_holder, Graphics g) {
		double draw_min = pass_thresh_start + span_start_shift;
		double draw_max = pass_thresh_end + span_end_shift;
		// make sure that length of region is > min_run_threshold
		// GAH 2006-02-16 changed to > min_run instead of >=, to better mirror Affy tiling array pipeline
		if (draw_max - draw_min > min_run_threshold) {
			// make sure aren't drawing single points
			coord.x = draw_min;
			view.transformToPixels(coord, prev_point);
			coord.x = draw_max;
			view.transformToPixels(coord, curr_point);
			if (make_syms) {
				SeqSymmetry sym = new SingletonSeqSymmetry((int) draw_min, (int) draw_max, aseq);
				region_holder.addChild(sym);
			} else {
				drawRectOrLine(g, prev_point.x, pixelbox.y + pixelbox.height - thresh_contig_height, curr_point.x - prev_point.x + 1, thresh_contig_height);
			}
		}
	}



	private void drawPrevious2(int pass_thresh_start, double span_start_shift, int pass_thresh_end, double span_end_shift, double min_run_threshold, ViewI view, boolean make_syms, BioSeq aseq, MutableSeqSymmetry region_holder, Graphics g) {
		double draw_min = pass_thresh_start + span_start_shift;
		double draw_max = pass_thresh_end + span_end_shift;
		// make sure that length of region is > min_run_threshold
		// GAH 2006-02-16 changed to > min_run instead of >=, to better mirror Affy tiling array pipeline
		if (draw_max - draw_min > min_run_threshold) {
			// make sure aren't drawing single points
			coord.x = draw_min;
			view.transformToPixels(coord, prev_point);
			coord.x = draw_max;
			view.transformToPixels(coord, curr_point);
			if (make_syms) {
				SeqSymmetry sym = new SingletonSeqSymmetry(pass_thresh_start, pass_thresh_end, aseq);
				region_holder.addChild(sym);
			} else {
				drawRectOrLine(g, prev_point.x, pixelbox.y + pixelbox.height - thresh_contig_height, curr_point.x - prev_point.x + 1, thresh_contig_height);
			}
		}
	}


	/**
	 * Fill rect or draw line, depending upon width
	 * (Much faster than simply filling a rect, if the width or height is 1)
	 * @param g
	 * @param x
	 * @param y
	 * @param width
	 * @param height
	 */
	private static void drawRectOrLine(Graphics g, int x, int y, int width, int height) {
		if (width <= 0 || height <= 0) {
			return;
		}
		if (width == 1 || height == 1) {
			g.drawLine(x, y, x+width-1, y+height-1);
		} else {
			g.fillRect(x, y, width, height);
		}
	}


	/**
	 * Retrieve the map y coord corresponding to a given graph yvalue.
	 */
	private double getCoordValue(ViewI view, float graph_value) {
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getTranslateY();
		float coord_value = (float) (offset - ((graph_value - getVisibleMinY()) * yscale));
		return coord_value;
	}

	/**
	 * Retrieve the graph yvalue corresponding to a given ycoord.
	 */
	float getGraphValue(ViewI view, double coord_value) {
		getInternalLinearTransform(view, scratch_trans);
		double yscale = scratch_trans.getScaleY();
		double offset = scratch_trans.getTranslateY();
		float graph_value = (float) ((offset - coord_value) / yscale) + getVisibleMinY();
		return graph_value;
	}

	public double getMaxGapThreshold() {
		return state.getMaxGapThreshold();
	}

	public float getMaxScoreThreshold() {
		return state.getMaxScoreThreshold();
	}

	public double getMinRunThreshold() {
		return state.getMinRunThreshold();
	}

	public float getMinScoreThreshold() {
		return state.getMinScoreThreshold();
	}

	public boolean getShowThreshold() {
		return state.getShowThreshold();
	}

	double getThreshEndShift() {
		return state.getThreshEndShift();
	}

	double getThreshStartShift() {
		return state.getThreshStartShift();
	}

	public int getThresholdDirection() {
		return state.getThresholdDirection();
	}

	private void resetThreshLabel() {
		float min_thresh = getMinScoreThreshold();
		float max_thresh = getMaxScoreThreshold();
		int direction = state.getThresholdDirection();
		if (direction == GraphState.THRESHOLD_DIRECTION_BETWEEN) {
			thresh_glyph.setLabel(GraphGlyph.nformat.format(min_thresh) + " -- " + GraphGlyph.nformat.format(max_thresh));
		} else if (direction == GraphState.THRESHOLD_DIRECTION_GREATER) {
			thresh_glyph.setLabel(">= " + GraphGlyph.nformat.format(min_thresh));
		} else if (direction == GraphState.THRESHOLD_DIRECTION_LESS) {
			thresh_glyph.setLabel("<= " + GraphGlyph.nformat.format(max_thresh));
		}
	}

	void setMaxGapThreshold(int thresh) {
		state.setMaxGapThreshold(thresh);
	}

	void setMaxScoreThreshold(float thresh) {
		state.setMaxScoreThreshold(thresh);
		resetThreshLabel();
	}

	void setMinRunThreshold(int thresh) {
		state.setMinRunThreshold(thresh);
	}

	void setMinScoreThreshold(float thresh) {
		state.setMinScoreThreshold(thresh);
		resetThreshLabel();
	}

	void setShowThreshold(boolean show) {
		state.setShowThreshold(show);
		thresh_glyph.setVisibility(show);
	}

	void setThreshEndShift(double d) {
		state.setThreshEndShift(d);
	}

	void setThreshStartShift(double d) {
		state.setThreshStartShift(d);
	}

	void setThresholdDirection(int d) {
		state.setThresholdDirection(d);
		resetThreshLabel();
	}

	@Override
	public void draw(ViewI view) {
		if (DEBUG) {
			System.out.println("called GraphGlyph.draw(), coords = " + coordbox);
		}
		GraphType graph_style = getGraphStyle();
		// GAH 9-13-2002
		// hack to get thresholding to work -- thresh line child glyph keeps getting removed
		//   as a child of graph... (must be something in SeqMapView.setAnnotatedSeq()...
		if (this.getChildCount() == 0) {
			if (thresh_glyph == null) {
				thresh_glyph = new ThreshGlyph();
				thresh_glyph.setSelectable(false);
				thresh_glyph.setColor(thresh_color);
			}
			this.addChild(thresh_glyph);
		}
		if (graph_style == GraphType.MINMAXAVG || graph_style == GraphType.LINE_GRAPH || graph_style == GraphType.HEAT_MAP) {
			double xpixels_per_coord = ( view.getTransform()).getScaleX();
			double xcoords_per_pixel = 1 / xpixels_per_coord;
			if ((xcoords_per_pixel < transition_scale)) {
				if (graph_style == GraphType.MINMAXAVG) {
					this.oldDraw(view, GraphType.BAR_GRAPH);
				} else {
					this.oldDraw(view, graph_style);
				}
			} else {
				drawSmart(view);
			}
		} else {
			// Not one of the special styles, so default to regular GraphGlyph.draw method.
			this.oldDraw(view, graph_style);
		}
		if (getShowThreshold()) {
			drawThresholdedRegions(view, null, null);
		} else {
			thresh_glyph.setVisibility(false);
		}
	}

	private float determineYZero() {
		if (getVisibleMinY() > 0) {
			return getVisibleMinY();
		}
		return Math.min(0, getVisibleMaxY());
	}
}


