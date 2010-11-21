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

import java.awt.event.*;
import java.util.*;

import com.affymetrix.genoviz.awt.NeoCanvas;

import com.affymetrix.genoviz.bioviews.ExponentialTransform;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.NeoDataAdapterI;
import com.affymetrix.genoviz.bioviews.RubberBand;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.View;

import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoViewMouseEvent;
import com.affymetrix.genoviz.event.NeoRubberBandListener;
import com.affymetrix.genoviz.util.GeneralUtils;

import com.affymetrix.genoviz.glyph.RootGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.JScrollBar;
import javax.swing.JSlider;

/**
 * Abstract implementation of NeoAbstractWidget.
 * NeoWidget is abstract -- currently does not implement at least:<ul>
 * <li> addItem(obj, obj);
 * <li> configure(str);
 * <li> configure(hash);
 * <li> configure(obj, str);
 * <li> configure(obj, hash);
 * <li> removeItem(obj);
 * </ul>
 * @see NeoAbstractWidget
 * for more documentation on methods.
 *
 * @version $Id: NeoWidget.java 6749 2010-08-25 17:34:09Z jnicol $
 */
public abstract class NeoWidget extends NeoAbstractWidget
	implements AdjustmentListener {

	private static final boolean DEBUG_SCROLLER_VALUES = false;
	private static final boolean DEBUG_SCROLL = false;
	private static final boolean DEBUG_ZOOM = false;

	protected boolean checkZoomValue = true;
	protected boolean checkScrollValue = true;

	protected NeoCanvas canvas;

	protected Scene scene;

	protected View view;
	protected LinearTransform trans;

	protected int pixel_beg[] = new int[2];
	protected int pixel_end[] = new int[2];
	protected int pixel_size[] = new int[2];

	// pixelblur is the amount of pixel space leeway given when finding overlaps
	protected int pixelblur = 2;
	protected int min_rubberband_size = 2;

	protected int zoomer_value[] = new int[2];
	protected int scroller_value[] = new int[2];
	protected int prev_zoomer_value[] = new int[2];
	protected int prev_scroller_value[] = new int[2];
	protected ExponentialTransform zoomtrans[] = new ExponentialTransform[2];
	private LinearTransform scrolltrans[] = new LinearTransform[2];

	protected double zoomer_scale[] = new double[2];
	protected int zoom_behavior[] = new int[2];
	protected double zoom_coord[] = new double[2];

	protected int scale_constraint[] = new int[2];
	protected int reshape_constraint[] = { FITWIDGET, FITWIDGET };

	protected int expansion_behavior[] = { NO_EXPAND, NO_EXPAND };

	// constraints on transform scale
	protected boolean set_min_pix_per_coord[] = { false, false };
	protected boolean set_max_pix_per_coord[] = { false, false };
	protected double min_pixels_per_coord[] = new double[2];
	protected double max_pixels_per_coord[] = new double[2];
	// the equivalent values for max/min scrolling offset are obtained form
	//    the scene's coord box as local variables

	// caching transform values
	//    LinearTransform is:
	//      pixel_loc = (pixels_per_coord * coord_loc) + pixel_offset
	protected double pixels_per_coord[] = new double[2];
	protected double pixel_offset[] = new double[2];

	protected double coords_per_pixel[] = new double[2];

	protected JScrollBar scroller[] = new JScrollBar[2];
	protected Adjustable zoomer[] = new Adjustable[2];

	protected RubberBand rband;
	protected Rectangle2D.Double bandbox;
	protected boolean rbActivated = false;

	boolean use_neoscrollers = false;

	protected boolean hscroll_show, vscroll_show;

	protected static final String hscroll_default_loc = "South";
	protected static final String vscroll_default_loc = "East";
	protected String hscroll_loc = hscroll_default_loc;
	protected String vscroll_loc = vscroll_default_loc;

	protected Set<NeoRubberBandListener> rubberband_listeners = new CopyOnWriteArraySet<NeoRubberBandListener>();


	public NeoWidget() {

		this.setScrollIncrementBehavior(X,NO_AUTO_SCROLL_INCREMENT);
		this.setScrollIncrementBehavior(Y,NO_AUTO_SCROLL_INCREMENT);

		// start with default identity linear transform
		// should probably move this to NeoMap... GAH 12-8-97
		trans = new LinearTransform();

		// adding name for common default color -- should probably be in a static
		//  class initialization method
		addColor("nicePaleBlue", new Color(180, 250, 250));
	}

	/**
	 * Destructor that unlocks graphic resources, cuts links.
	 * Call only when the map is no longer being displayed.
	 */
	@Override
	public void destroy() {

		super.destroy();
		if (canvas != null) remove ( canvas );
		if (scroller != null) {
			if ( scroller[0] != null ) {
				scroller[0].removeAdjustmentListener(this);
			}
			if ( scroller[1] != null ) {
				scroller[1].removeAdjustmentListener(this);
			}
		}
		if (zoomer != null) {
			if ( zoomer[0] != null ) {
				zoomer[0].removeAdjustmentListener(this);
			}
			if ( zoomer[1] != null ) {
				zoomer[1].removeAdjustmentListener(this);
			}
		}
		scroller = null;
		zoomer = null;
		if (canvas != null) {
			if ( rband != null ) {
				canvas.removeMouseListener(rband);
				canvas.removeMouseMotionListener(rband);
			}
			canvas = null;
		}
		rband = null;
		view.destroy();
	}

	/* These are the methods that have NOT been moved from NeoMap
	 * into NeoWidget for one reason or another, yet are still
	 * spec'ed in the NeoAbstractWidget.
	 */
	public void stretchToFit(boolean xstretch, boolean ystretch) {
		System.out.println("NeoWidget.stretchToFit() not yet implemented");
	}



	public void setExpansionBehavior(int id, int behavior) {
		expansion_behavior[id] = behavior;
		int newid = 0;
		int newbehavior = 0;
		RootGlyph rglyph = (RootGlyph)scene.getGlyph();
		if (id == X) {
			newid = RootGlyph.X;
		}
		else if (id == Y) {
			newid = RootGlyph.Y;
		}
		if (behavior == EXPAND) {
			newbehavior = RootGlyph.EXPAND;
		}
		else if (behavior == NO_EXPAND) {
			newbehavior = RootGlyph.NO_EXPAND;
		}
		rglyph.setExpansionBehavior(newid, newbehavior);
	}

	public int getExpansionBehavior(int id) {
		return expansion_behavior[id];
	}

	public void setReshapeBehavior(int id, int behavior) {
		reshape_constraint[id] = behavior;
	}

	public int getReshapeBehavior(int id) {
		return reshape_constraint[id];
	}

	public Scene getScene() { return scene; }

	public NeoCanvas getNeoCanvas() { return canvas; }

	public View getView() { return view; }

	/**
	 * Ignores requests to configure the layout.
	 * This method should be overridden
	 * by subclasses supporting layout configuration.
	 *
	 * @see com.affymetrix.genoviz.widget.NeoAbstractWidget#configureLayout
	 */
	public void configureLayout(int component, int placement) {
	}

	/**
	 * placement is not supported.
	 * This method should be overridden
	 * by subclasses supporting placement options.
	 *
	 * @return PLACEMENT_NONE
	 * @see com.affymetrix.genoviz.widget.NeoAbstractWidget#getPlacement
	 */
	public int getPlacement(int component) {
		return PLACEMENT_NONE;
	}

	/**
	 *  Sets the bounds.
	 *  Unlike {@link #setBounds(int, int, int)}, this does NOT add
	 *  1 to the width.
	 *
	 */
	public void setFloatBounds(int id, double start, double end) {
		double size = end - start;
		Rectangle2D.Double sbox = scene.getCoordBox();
		if (id == X) {
			scene.setCoords(start, sbox.y, size, sbox.height);
		}
		else {
			scene.setCoords(sbox.x, start, sbox.width, size);
		}
		adjustScroller(id);
	}

	/**
	 *  Adds 1 to width when calculating coord box.
	 *  For example setting X bounds to 0, 74, actually sets coord box to
	 *  cbox.x = 0, cbox.width = 75
	 */
	public void setBounds(int id, int start, int end) {
		double size = end-start+1;
		Rectangle2D.Double sbox = scene.getCoordBox();
		if (id == X) {
			scene.setCoords(start, sbox.y, size, sbox.height);
		}
		else {
			scene.setCoords(sbox.x, start, sbox.width, size);
		}
		adjustScroller(id);
	}


	/**
	 * gets all the items in the widget that are visible.
	 * <p><em><strong>We will change this name soon.</strong></em>
	 * We should make this <code>getDisplayedItems()</code> instead,
	 * and reserve "visible" for <code>getVisibleItem(x, y)</code>
	 * which makes sure the item is not only drawn/displayed,
	 * but also visible (unobscured by other items).
	 *
	 * @return a list of the items.
	 */
	public List getVisibleItems() {
		// SHOULD THIS BE getViewBounds() INSTEAD!!??!!!
		//    WHICH GLYPHS DO WE REALLY WANT TO RETURN????
		Rectangle2D.Double coordrect = getCoordBounds();
		List<GlyphI> pickvect = new ArrayList<GlyphI>();
		scene.pickTraversal(coordrect, pickvect, view);
		return pickvect;
	}


	/**
	 *  retrieve a List of all drawn glyphs that overlap
	 *  the coordinate rectangle coordrect.
	 */
	public List<GlyphI> getItemsByCoord(Rectangle2D.Double coordrect) {
		List<GlyphI> pickvect = new ArrayList<GlyphI>();
		scene.pickTraversal(coordrect, pickvect, view);
		return pickvect;
	}

	/**
	 *  retrieve a List of all drawn glyphs that overlap
	 *  the pixel point x, y.
	 */
	public List<GlyphI> getItemsByPixel(int x, int y) {
		Rectangle pixrect = new Rectangle(x-this.pixelblur, y-this.pixelblur,
				2*this.pixelblur, 2*this.pixelblur);
		Rectangle2D.Double coordrect = new Rectangle2D.Double();
		coordrect = view.transformToCoords(pixrect, coordrect);
		return this.getItemsByCoord(coordrect);
	}

	/**
	 * gets the items that overlap a given rectangle of pixels.
	 *
	 * @param pixrect a rectangle in pixel space
	 * @return the overlapping glyphs
	 */
	public List<GlyphI> getItems(Rectangle pixrect) {
		// no pixelblur for region selection
		Rectangle2D.Double coordrect = new Rectangle2D.Double();
		coordrect = view.transformToCoords(pixrect, coordrect);
		return getItemsByCoord(coordrect);
	}

	/**
	 * gets the items overlapping a given point in coordinate space.
	 * A fuzz factor, in pixels, is added around the point.
	 *
	 * @return the overlapping glyphs
	 * @see com.affymetrix.genoviz.widget.NeoAbstractWidget#setPixelFuzziness
	 */
	public List<GlyphI> getItems(double x, double y) {
		Rectangle2D.Double coordrect = new Rectangle2D.Double(x, y, 1, 1);
		if (0 < pixelblur) {
			Rectangle pixrect = new Rectangle();
			pixrect = view.transformToPixels(coordrect, pixrect);
			pixrect.setBounds(pixrect.x-pixelblur, pixrect.y-pixelblur,
					pixrect.width+2*pixelblur, pixrect.height+2*pixelblur);
			coordrect = view.transformToCoords(pixrect, coordrect);
		}
		return getItemsByCoord(coordrect);
	}

	public List<GlyphI> getItems(double x, double y, int location) {
		return getItems(x,y);
	}


	public NeoAbstractWidget getWidget(int location) { return this; }

	public NeoAbstractWidget getWidget(GlyphI gl) {
		if (gl.getScene() == this.scene) { return this; }
		return null;
	}

	/**
	 * Updates the visual appearance of the widget.  It is important to call
	 * this method to view any externally introduced changes in widget
	 * appearance since last call to updateWidget()
	 */
	public void updateWidget() {
		updateWidget(false);
	}

	/**
	 * Updates the visual appearance of the widget
	 *    if (full_update) then force redrawing of entire widget
	 *  Once optimizations are working smoothly, this should never be needed.
	 *    For most visual updates, updateWidget() with no arguments should
	 *  have the same effect but will be more efficient
	 */
	public void updateWidget(boolean full_update) {
		if (canvas == null) return; // in case destroy() was called.
		if (full_update) {
			scene.maxDamage();
		}
		canvas.repaint();
	}



	public void setRubberBandBehavior(boolean activate) {
		if (canvas == null) return; // in case destroy() was called.
		if (activate && !rbActivated) {
			// rubberband listens to canvas for MOUSE_DOWN, MOUSE_DRAG, MOUSE_UP events
			canvas.addMouseListener(rband);
			canvas.addMouseMotionListener(rband);
			rbActivated = true;
			rband.clearRubberBand();
		}
		else if (!activate && rbActivated)  {
			canvas.removeMouseListener(rband);
			canvas.removeMouseMotionListener(rband);
			rbActivated = false;
			rband.clearRubberBand();
		}
	}

	public boolean getRubberBandBehavior() {
		return rbActivated;
	}

	public void addDataAdapter(NeoDataAdapterI adapter) {
		scene.addDataAdapter(adapter);
	}

	public GlyphI addData(Object model) {
		return scene.addData(model);
	}


	/** This is the only place <code>pixel_*</code> should change. */
	protected void setPixelBounds() {
		if (canvas == null) return; // in case destroy() was called.
		pixel_beg[X] = 0;
		pixel_beg[Y] = 0;
		pixel_size[X] = canvas.getSize().width;
		pixel_size[Y] = canvas.getSize().height;

		pixel_end[X] = pixel_size[X] - pixel_beg[X];
		pixel_end[Y] = pixel_size[Y] - pixel_beg[Y];

		Rectangle bbox = new Rectangle(pixel_beg[X], pixel_beg[Y],
				pixel_size[X], pixel_size[Y]);
		view.setPixelBox(bbox);
	}

	public Rectangle getPixelBounds(GlyphI gl) {
		return gl.getPixelBox(view);
	}

	public Rectangle2D.Double getCoordBounds() {
		return scene.getCoordBox();
	}

	public Rectangle2D.Double getViewBounds() {
		return view.getCoordBox();
	}

	public Rectangle2D.Double getCoordBounds(GlyphI gl) {
		return gl.getCoordBox();
	}

	public void setPixelFuzziness(int blur) {
		pixelblur = blur;
	}

	public int getPixelFuzziness() {
		return pixelblur;
	}


	public void setScroller(int id, JScrollBar adj) {
		if (adj == null) {
			throw new IllegalArgumentException("NeoWidget.setScroller() requires " +
					"an Adjustable argument, was passed a null instead");
		}
		// can only be one scroller for each range -- remove this from previous
		// scroller's listeners
		if (scroller[id] != null) {
			scroller[id].removeAdjustmentListener(this);
		}
		scroller[id] = adj;
		scrolltrans[id] = new LinearTransform();
		scroller[id].addAdjustmentListener(this);
	}

	public void setZoomer(int id, Adjustable adj) {
		if (adj == null) {
			throw new IllegalArgumentException("NeoWidget.setZoomer() requires " +
					"an Adjustable argument, was passed a null instead");
		}
		// can only be one zoomer for each range -- remove this from previous
		// zoomer's listeners
		if (zoomer[id] != null) {
			zoomer[id].removeAdjustmentListener(this);
		}
		zoomer[id] = adj;
		zoomer[id].setMinimum(0);		
		zoomer[id].setMaximum(200 + zoomer[id].getVisibleAmount());
		zoomer[id].setValue(0);

		// GAH 6-29-99
		// setting maxy of exponential tranform to (max - visible amount) to
		// compensate for the fact that in JDK1.1 and Swing Scrollbars,
		// the maximum for the value is really the scrollbar maximum minus
		// the visible amount (the thumb)
		zoomtrans[id] = new ExponentialTransform(min_pixels_per_coord[id],
				max_pixels_per_coord[id], zoomer[id].getMinimum(),
				zoomer[id].getMaximum()-zoomer[id].getVisibleAmount());
		zoomer[id].addAdjustmentListener(this);
	}

	// maybe should return coord it _actually_ scrolled to
	public void scroll(int id, double coord_value) {
		// double new_coord_value = 0;
		// double prev_pixel_value;
		// a boolean to enforce scroller adjustment if bumping up against
		//   map edges, otherwise sometimes the scrollers don't get adjusted
		//   in these cases
		boolean force_scroller_adjust = false;

		if (checkScrollValue) {
			// trying to constrain scrolling to stay inside coordinate bounds
			Rectangle2D.Double scene_coords = getCoordBounds();
			Rectangle2D.Double view_coords = view.getCoordBox();
			double min_coord, max_coord;
			if (id == X) {
				min_coord = scene_coords.x;
				max_coord = scene_coords.x + scene_coords.width - view_coords.width;
			}
			else {
				min_coord = scene_coords.y;
				max_coord = scene_coords.y + scene_coords.height - view_coords.height;
			}
			if (min_coord >= max_coord) {
				// this will iff view_coords >= scene_coords in id dimension, in
				// which case we are attempting to scroll something that is <=
				// the view -- therefore center it?

				double scene_center;
				if (id == X) {
					scene_center = scene_coords.x + (scene_coords.width/2);
					coord_value = scene_center - (view_coords.width/2);
					force_scroller_adjust = true;
				}
				else {
					scene_center = scene_coords.y + (scene_coords.height/2);
					coord_value = scene_center - (view_coords.height/2);
					force_scroller_adjust = true;
				}
			}

			else if (coord_value < min_coord) {
				coord_value = min_coord;
				force_scroller_adjust = true;
			}
			else if (coord_value > max_coord) {
				coord_value = max_coord;
				force_scroller_adjust = true;
			}
		}

		double pixel_value;
		if (id == X) {
			pixels_per_coord[id] = trans.getScaleX();
			pixel_value = coord_value * pixels_per_coord[id];
			trans.setTransform(trans.getScaleX(), 0, 0, trans.getScaleY(), -pixel_value, trans.getTranslateY());
		}
		else {
			pixels_per_coord[id] = trans.getScaleY();
			pixel_value = coord_value * pixels_per_coord[id];
			trans.setTransform(trans.getScaleX(), 0, 0, trans.getScaleY(), trans.getTranslateX(), -pixel_value);
			if (DEBUG_SCROLL) {
				System.out.println("Coord Value = " + coord_value +
						", Pixels/Coord = " + pixels_per_coord[id] +
						", " +
						((coord_value * pixels_per_coord[id])/ pixels_per_coord[id]) +
						", " + (pixel_value / pixels_per_coord[id]));
			}

		}

		if (force_scroller_adjust || coord_value != scroller_value[id]) {
			adjustScroller(id);
		}

		view.calcCoordBox();
		if (DEBUG_SCROLL) {
			System.out.println("Scrolling to: " + coord_value);
			System.out.println(trans);
			System.out.println(view.getCoordBox());
		}

	}


	public void adjustScroller(int id) {
    if (scroller[id] == null) { return; }

    boolean zoomerIsAdjusting = false;
    if (zoomer[id] instanceof JSlider) {
      zoomerIsAdjusting = ((JSlider) zoomer[id]).getValueIsAdjusting();
    }

    // GAH 1-15-2004
    // trying same trick as in adjustZoomer(), supressing event kickback changing
    //   original scroll value
    // not sure how necessary this suppression is with adjustScroller(), could just
    //   be a problem with adjustZoomer(), but trying for now -- need to watch
    //   for any side effects...
    scroller[id].removeAdjustmentListener(this);

    double coord_beg, coord_end, coord_size;
		Rectangle2D.Double scenebox = scene.getCoordBox();
    if (id == X)  {
      pixel_offset[id] = -1 * trans.getTranslateX();
      coord_beg = scenebox.x;
      coord_size = scenebox.width;
    }
    else {
      coord_beg = scenebox.y;
      coord_size = scenebox.height;
      pixel_offset[id] = -1 * trans.getTranslateY();

    }
    coord_end = coord_beg + coord_size;

    double coord_offset = pixel_offset[id] * coords_per_pixel[id];
    double visible_coords = coords_per_pixel[id] * pixel_size[id];

    /* If there would be more visible than the maximum coordinate size, max out
     *   the scrollbar.
     */
    if (coord_size < visible_coords) {
      if (DEBUG_SCROLLER_VALUES) {
        System.err.println("setting scroller values with " +
            "coord_size=" + coord_size +
            " < visible_coord=" + visible_coords);
      }
      scroller[id].getModel().setRangeProperties(
        scroller[id].getMinimum(), scroller[id].getMaximum() - scroller[id].getMinimum(),
        scroller[id].getMinimum(), scroller[id].getMaximum(), zoomerIsAdjusting);
    }
    else {
      if (DEBUG_SCROLLER_VALUES) {
        if (id == Y)  System.out.println(
            "Setting Y scroll for " +
            GeneralUtils.toObjectString(this) +
            ", value: " + (int)coord_offset +
            ", visible: " + (int)visible_coords +
            ", min: " + (int)coord_beg +
            ", max: " + (int)coord_end );
      }
      scroller[id].getModel().setRangeProperties(
        (int) coord_offset, (int) visible_coords,
        (int) coord_beg, (int) coord_end, zoomerIsAdjusting);
    }

    if (scroll_behavior[id] == AUTO_SCROLL_INCREMENT) {
      if (coords_per_pixel[id] > 1) {
        scroller[id].setUnitIncrement((int)coords_per_pixel[id]);
        scroller[id].setBlockIncrement((int)(5*coords_per_pixel[id]));
      }
      else {
        scroller[id].setUnitIncrement(1);
        scroller[id].setBlockIncrement(5);
      }
    }
    else if (scroll_behavior[id] == AUTO_SCROLL_HALF_PAGE)  {
      if (coords_per_pixel[id] > 1) {
        scroller[id].setUnitIncrement((int)(5*coords_per_pixel[id]));
        if (id == X) { scroller[id].setBlockIncrement((int)(getViewBounds().width/2)); }
        else if (id == Y)  { scroller[id].setBlockIncrement((int)(getViewBounds().height/2)); }
      }
      else {
        scroller[id].setUnitIncrement(1);
        scroller[id].setBlockIncrement(5);
      }
    }
    scroller[id].addAdjustmentListener(this);
  }

  public void adjustZoomer(int id) {
		if (zoomer[id] == null) { return; }
		if (pixels_per_coord[id] == zoomer_value[id]) { return; }
		// GAH 1-15-2004  trying to deal with problem where precisely setting zoom to Z
		//   via NeoWidget.zoom() call ends up _not_ precisely setting zoom, because
		//   NeoWidget.zoom() triggers adjustZoomer(), and the zoomer in turn sends an
		//   adjustment change event back to NeoWidget(), but because zoomer can only
		//   change in pixel increments, the change event forces _another_ zoom to
		//   some scale factor near Z but not quite Z...
		// Therefore try bracketing most of adjustZoomer() method with removing and then
		//   re-adding NeoWidget as listener to zoomer
		zoomer[id].removeAdjustmentListener(this);

		// otherwise need to do a zoomtrans[id].inverseTransform() to
		//  set correct zoomer_scroll_value[id]
		zoomer_scale[id] = pixels_per_coord[id];
		zoomer_value[id] = (int)zoomtrans[id].inverseTransform(id, zoomer_scale[id]);
		// catching bug where zoomer_value gets negative values for weird
		//   zoomtrans (like when zoom_max and zoom_min are equal)
		// zoomer_value is NOT scale --
		//   goes through zoomtrans transform to get scale
		if (zoomer_value[id] < 0)   { zoomer_value[id] = 0; }
		zoomer[id].setValue(zoomer_value[id]);

		zoomer[id].addAdjustmentListener(this);
	}

	public double getZoom(int axisid) {
		return pixels_per_coord[axisid];
	}

	public double getMinZoom(int axisid) {
		return min_pixels_per_coord[axisid];
	}

	public double getMaxZoom(int axisid) {
		return max_pixels_per_coord[axisid];
	}

	public void setMinZoom(int id, double min) {
		double prev_scale;
		boolean scale_at_min = false;
		if (id == X) { prev_scale = trans.getScaleX(); }
		else         { prev_scale = trans.getScaleY(); }
		if (prev_scale == min_pixels_per_coord[id]) {
			scale_at_min = true;
		}

		set_min_pix_per_coord[id] = true;
		min_pixels_per_coord[id] = min;
		// calling stretchToFit only for side effects...
		//   (like setting Adjustable appearance)
		stretchToFit(false, false);

		// testing scale adjustment if outside allowable range
		//   might want to push this down into stretchToFit() -- GAH 12/14/97
		double pix_per_coord;
		if (id == X) { pix_per_coord = trans.getScaleX(); }
		else         { pix_per_coord = trans.getScaleY(); }
		if (pix_per_coord < min || scale_at_min) {
			zoom(id, min);
		}

	}

	public void setMaxZoom(int id, double max) {

		// assuming that if scale is already at max zoom, want to
		//   change scale to keep it at max zoom
		double prev_scale;
		if (id == X) { prev_scale = trans.getScaleX(); }
		else         { prev_scale = trans.getScaleY(); }
		boolean scale_at_max = (prev_scale == max_pixels_per_coord[id]);

		set_max_pix_per_coord[id] = true;
		max_pixels_per_coord[id] = max;
		// calling stretchToFit() only for side effects...
		stretchToFit(false, false);

		// testing scale adjustment if outside allowable range
		//   might want to push this down into stretchToFit() -- GAH 12/14/97
		double pix_per_coord;
		if (id == X) { pix_per_coord = trans.getScaleX(); }
		else         { pix_per_coord = trans.getScaleY(); }
		if (pix_per_coord > max || scale_at_max) {
			zoom(id, max);
		}
	}


	public void setVisibility(GlyphI gl, boolean isVisible) {
		scene.setVisibility(gl, isVisible);
	}

	public void setVisibility(List glyphs, boolean isVisible) {
		for (int i=0; i<glyphs.size(); i++) {
			setVisibility((GlyphI)glyphs.get(i), isVisible);
		}
	}

	/****************************************/
	/** Methods for dealing with selecion **/
	/****************************************/

	/**
	 * adds an object to the selection.
	 * @see com.affymetrix.genoviz.widget.NeoAbstractWidget#select
	 */
	public void select(GlyphI g) {
		scene.select(g);
		if ( g.isSelected() && !selected.contains(g)) {
			selected.add(g);
		}
	}

	/**
	 * @see com.affymetrix.genoviz.widget.NeoMap#select
	 */
	public void select(List<GlyphI> glyphs, double x, double y,
			double width, double height) {
		for (GlyphI glyph : glyphs) {
			select(glyph, x, y, width, height);
		}
	}

	public void select(GlyphI g, double x, double y,
			double width, double height) {
		scene.select(g, x, y, width, height);
		if (g.isSelected() && ! selected.contains(g)) {
			selected.add(g);
		}
	}

	public boolean supportsSubSelection(GlyphI gl) {
		return gl.supportsSubSelection();
	}

	public void deselect(GlyphI g) {
		scene.deselect(g);
		selected.remove(g);
	}

	/**
	 * Scale constraints are currently only considered during
	 *    zooming with zoomer[] adjustables
	 */
	public void setScaleConstraint(int axisid, int constraint) {
		scale_constraint[axisid] = constraint;
	}

	public void setZoomBehavior(int axisid, int constraint) {
		switch (constraint) {
			case CONSTRAIN_START:
			case CONSTRAIN_MIDDLE:
			case CONSTRAIN_END:
				break;
			default:
				throw new IllegalArgumentException("Invalid constraint.");
		}
		zoom_behavior[axisid] = constraint;
	}
	public int getZoomBehavior(int axisid) {
		return zoom_behavior[axisid];
	}

	public double getZoomCoord(int axisid) {
		return zoom_coord[axisid];
	}

	public void setZoomBehavior(int axisid, int constraint, double coord) {
		if (CONSTRAIN_COORD != constraint)
			throw new IllegalArgumentException("Invalid constraint.");
		zoom_behavior[axisid] = constraint;
		zoom_coord[axisid] = coord;
	}


	public void adjustmentValueChanged(AdjustmentEvent evt) {
		//    System.out.println("adjustmentValueChanged to: " + evt.getValue());
		Adjustable source = evt.getAdjustable();
		//    System.out.println(source);
		if (source == zoomer[X] || source == zoomer[Y]) {
			int id;
			if (source == zoomer[X]) { id = X; }
			else { id = Y;  }
			zoomer_value[id] = source.getValue();
			if (zoomer_value[id] == prev_zoomer_value[id])  {
				return;
			}
			zoomer_scale[id] = zoomtrans[id].transform(id, zoomer_value[id]);
			if (scale_constraint[id] == INTEGRAL_PIXELS ||
					scale_constraint[id] == INTEGRAL_ALL) {
				if (zoomer_scale[id] >= 1)  {
					zoomer_scale[id] = (int)(zoomer_scale[id] +.0001);
				}
					}
			if (DEBUG_ZOOM)  {
				System.out.println("pixels_per_base = " + zoomer_scale[id] +
						",  coords_per_pixel[id] = " + 1/zoomer_scale[X]);
			}
			zoom(id, zoomer_scale[id]);
			updateWidget();
			prev_zoomer_value[id] = zoomer_value[id];
		}
		else if (source == scroller[X] || source == scroller[Y]) {
			int id;
			if (source == scroller[X]) { id = X; }
			else { id = Y; }

			scroller_value[id] = (int) scrolltrans[id].transform(id, source.getValue());

			if (DEBUG_SCROLL)  {
				System.out.println("Scrolling to: " + scroller_value[id] + ", " +
						source.getValue());
			}
			if (scroller_value[id] != prev_scroller_value[id]) {
				scroll(id, scroller_value[id]);
				updateWidget();
				prev_scroller_value[id] = scroller_value[id];
			}
		}
	}

	public void setScrollTransform(int id, LinearTransform trans) {
		scrolltrans[id] = trans;
	}

	public void setSelectionAppearance(int select_behavior) {
		scene.setSelectionAppearance( select_behavior );
	}
	public int getSelectionAppearance() {
		return scene.getSelectionAppearance();
	}
	public void setSelectionColor(Color col) {
		scene.setSelectionColor(col);
	}
	public Color getSelectionColor() {
		return scene.getSelectionColor();
	}

	/**
	 * this stub should be overridden by subclasses
	 * that allow sub selection.
	 */
	public void setSubSelectionAllowed(boolean allowed) {
	}
	/**
	 * this stub should be overridden by subclasses
	 * that allow sub selection.
	 */
	public boolean isSubSelectionAllowed() {
		return false;
	}



	public void zoom(int id, double zoom_scale) {
		/*
		   filtering out bogus zoom calls
		   should probably check for these internally in stretchToFit etc.,
		   before calling zoom() method, rather than here in the zoom() method
		   itself -- GAH 6-1-98
		   */
		if (Double.isInfinite(zoom_scale) ||
				Double.isNaN(zoom_scale)) {
			if (DEBUG_ZOOM)  {
				System.out.println("Weird zoom call, ignoring: id = " + id +
						", scale = " + zoom_scale);
			}
			return;
				}
		if (DEBUG_ZOOM)  {
			System.out.println("Zooming " + id + " to " + zoom_scale);
		}

		/*
		 * Want to make sure zoom_scale is within range set by max and min
		 * pixels per coord.  If not then throw an IllegalArgumentException
		 * BUT, may end up with zoom_scale just below min or just above max due
		 * to doubleing point imprecision / rounding errors.  Therefore if
		 * zoom_scale is below min or above max, but by only a small amount
		 * relative to the magnitude of zoom_scale, then set to exactly the
		 * max or min
		 */

		/*
		   This good and concientious checking breaks NeoSeq.

		   Therefore adding boolean to set to avoid this section in widgets
		   like NeoSeq -- GAH 6-1-98

		   partial solution to problem via silent death of weird calls (above)
		   could fully solve if switched to silent death of out-of-bounds calls
		   (commented out below) rather than throwing exceptions, but may
		   sometimes really want exception thrown if zoom bounds are exceeded.
		   Therefore leaving checkZoomValue boolean in for NeoSeq to toggle off

		   For now I'm putting in the silent death return -- GAH 6-4-98
		   */
		if (checkZoomValue) {
			if (zoom_scale > max_pixels_per_coord[id]) {
				double ratio_diff;
				if (0 == max_pixels_per_coord[id]) {
					ratio_diff = 0;
				}
				else {
					ratio_diff = Math.abs(1-zoom_scale/max_pixels_per_coord[id]);
				}
				if (ratio_diff < 0.0001) {
					zoom_scale = max_pixels_per_coord[id];
				}
				else {
					/*  changed to silent return to handle calls outside bounds
						(which seems to happen intermittently for different widgets
						on different platforms during scrolling, zooming,
						stretchToFit, and resize)
						*/
					if (DEBUG_ZOOM)  {
						System.err.println("attempted to zoom " + id + " to " +
								zoom_scale + " pixels per coord, but max allowed is " +
								max_pixels_per_coord[id]);
					}
					return;
					/*
					   throw new IllegalArgumentException("attempted to zoom to " +
					   zoom_scale + " pixels per coord, but max allowed is " +
					   max_pixels_per_coord[id]);
					   */
				}
			}
			else if (zoom_scale < min_pixels_per_coord[id]) {
				double ratio_diff;
				if (0 == min_pixels_per_coord[id]) {
					ratio_diff = 0;
				}
				else {
					ratio_diff = Math.abs(1-zoom_scale/min_pixels_per_coord[id]);
				}
				if (ratio_diff < 0.0001) {
					zoom_scale = min_pixels_per_coord[id];
				}
				else {
					/*
					   changed to silent return to handle calls outside bounds
					   (which seems to happen intermittently for different widgets
					   on different platforms during scrolling, zooming,
					   stretchToFit, and resize)
					   */
					if (DEBUG_ZOOM)  {
						System.err.println("attempted to zoom " + id + " to " +
								zoom_scale + " pixels per coord, but min allowed is " +
								min_pixels_per_coord[id]);
					}
					return;
				}
			}
		}

		view.calcCoordBox();
		double prev_pixels_per_coord = pixels_per_coord[id];
		pixels_per_coord[id] = zoom_scale;
		coords_per_pixel[id] = 1/zoom_scale;
		if (pixels_per_coord[id] == prev_pixels_per_coord) {
			return;
		}

		Rectangle2D.Double scenebox = scene.getCoordBox();
		double prev_coords_per_pixel = 1/prev_pixels_per_coord;
		double prev_pixel_offset;
		double coord_beg, coord_end, coord_size;
		double fixed_coord, fixed_pixel;

		if (id == X) {
			prev_pixel_offset = -1 * trans.getTranslateX();
			coord_beg = scenebox.x;
			coord_size = scenebox.width;
		}
		else {
			prev_pixel_offset = -1 * trans.getTranslateY();
			coord_beg = scenebox.y;
			coord_size = scenebox.height;
		}
		coord_end = coord_beg + coord_size;

		double prev_coord_offset = prev_pixel_offset * prev_coords_per_pixel;
		double prev_visible_coords = prev_coords_per_pixel * pixel_size[id];
		if (zoom_behavior[id] == CONSTRAIN_MIDDLE) {
			fixed_coord = prev_coord_offset + (prev_visible_coords / 2.0f);
		}
		else if (zoom_behavior[id] == CONSTRAIN_START)  {
			fixed_coord = prev_coord_offset;
		}
		else if (zoom_behavior[id] == CONSTRAIN_END) {
			fixed_coord = prev_coord_offset + prev_visible_coords;
		}
		else if (zoom_behavior[id] == CONSTRAIN_COORD) {
			fixed_coord = zoom_coord[id];
		}
		else {
			throw new IllegalArgumentException("zoom behavior must be on of "
					+ "CONSTRAIN_MIDDLE, CONSTRAIN_START, CONSTRAIN_END, or "
					+ "CONSTRAIN_COORD.");
		}

		double visible_coords = coords_per_pixel[id] * pixel_size[id];
		fixed_pixel = prev_pixels_per_coord * fixed_coord - prev_pixel_offset;
		double real_pix_offset = fixed_pixel - pixels_per_coord[id] * fixed_coord;
		double coord_offset = -real_pix_offset * coords_per_pixel[id];

		if ((scale_constraint[id] == INTEGRAL_PIXELS ||
					scale_constraint[id] == INTEGRAL_ALL) && zoom_scale >= 1) {
			coord_offset = (int)coord_offset;
					}
		
		double last_coord_displayed = coord_offset + visible_coords;

		if (!((coord_offset < coord_beg)  && (last_coord_displayed > coord_end))) {
			if (coord_offset < coord_beg) {
				coord_offset = coord_beg;
				last_coord_displayed = coord_offset + visible_coords;
			}
			if (last_coord_displayed > coord_end) {
				coord_offset = coord_end - visible_coords;
			}
		}

		if ((scale_constraint[id] == INTEGRAL_PIXELS ||
					scale_constraint[id] == INTEGRAL_ALL) && zoom_scale >= 1) {
			pixel_offset[id] =
				(int)(Math.ceil(coord_offset / coords_per_pixel[id]));
					}
		else {
			pixel_offset[id] = coord_offset / coords_per_pixel[id];
		}

		if (id == X) {
			trans.setTransform(pixels_per_coord[id], 0, 0, trans.getScaleY(), -pixel_offset[id], trans.getTranslateY());
		}
		else {
			trans.setTransform(trans.getScaleX(), 0, 0, pixels_per_coord[id], trans.getTranslateX(), -pixel_offset[id]);
		}
		if (zoom_scale != zoomer_scale[id]) {
			adjustZoomer(id);
		}
		adjustScroller(id);

		view.calcCoordBox();
		if (DEBUG_ZOOM) {
			if (id == Y) {
				System.out.println("zooming to: " + zoom_scale + ", coord offset = " +
						coord_offset);
			}
		}
	}

	public void toFrontOfSiblings(GlyphI glyph) {
		scene.toFrontOfSiblings(glyph);
	}

	public void toBackOfSiblings(GlyphI glyph) {
		scene.toBackOfSiblings(glyph);
	}

	/**
	 *  using isFullyWithinView() and isOnTop() to find out if
	 *  a glyph is unobscured on the screen
	 */
	public boolean isUnObscured(GlyphI gl) {
		return (isFullyWithinView(gl) && isOnTop(gl));
	}

	public boolean isOnTop(GlyphI gl) {
		if (!gl.isVisible()) { return false; }
		Rectangle2D.Double cbox = gl.getCoordBox();
		List<GlyphI> pickvect = new ArrayList<GlyphI>();
		getScene().pickTraversal(cbox, pickvect, getView());
		if (pickvect.isEmpty()) {
			// something very strange is going on if pickvect doesn't at
			//    least pick up the glyph itself...
			return false;
		}
		if (gl == pickvect.get(pickvect.size()-1)) {
			// if gl is last element in pickvect then it was drawn last,
			//   and therefore is on top and unobscured
			return true;
		}
		// otherwise there are other glyphs that hit its coordbox, and are
		//   being drawn after it, and therefore it is at least partially obscured
		return false;
	}

	public boolean isFullyWithinView(GlyphI gl) {

		// a) glyph.isVisible() == true
		if (!gl.isVisible())  { return false; }

		// b) the entire glyph is within the bounds of the region of
		//    the map drawn on screen iff union of view coord box and
		//    glyph coord box is same size as view coord box
		Rectangle2D.Double viewbox = getView().getCoordBox();
		Rectangle2D.Double glyphbox = gl.getCoordBox();
		if (!glyphbox.intersects(viewbox)) { return false; }
		Rectangle2D.Double unionbox = (Rectangle2D.Double)viewbox.createUnion(glyphbox);

		if (unionbox.equals(viewbox)) { return true; }
		return false;
	}

	public boolean isPartiallyWithinView(GlyphI gl) {
		// a) glyph.isVisible() == true
		if (!gl.isVisible())  { return false; }

		// b) part of the glyph is within the bounds of the region of
		//    the map drawn on screen iff view coord box hits glyph
		Rectangle2D.Double viewbox = getView().getCoordBox();
		return gl.hit(viewbox, this.getView());
	}


	/**
	 * Setting boolean for whether to restrict value
	 * in zoom(id, value) calls to be within range
	 * defined by calls to setMinZoom(id, min) and setMaxZoom(id, max).
	 * This is meant as temporary workaround for bugs in
	 * NeoSeq(), where these checks cause apps to hang
	 */
	protected void setCheckZoomValue(boolean b) {
		checkZoomValue = b;
	}

	/**
	 * Getting boolean for whether to restrict value
	 * in zoom(id, value) calls to be within range
	 * defined by calls to setMinZoom(id, min) and setMaxZoom(id, max).
	 */
	public boolean getCheckZoomValue() {
		return checkZoomValue;
	}


	/**
	 * Setting boolean for whether to restrict value in scroll(id, value)
	 * calls to stay within bounds of the map.
	 * This is meant as temporary workaround for bugs in
	 * NeoSeq(), where these checks cause problems
	 */
	protected void setCheckScrollValue(boolean b) {
		checkScrollValue = b;
	}

	/**
	 * Setting boolean for whether to restrict value in scroll(id, value)
	 * calls to stay within bounds of the map.
	 */
	public boolean getCheckScrollValue() {
		return checkScrollValue;
	}

	public void heardMouseEvent(MouseEvent evt) {
		if (! (evt instanceof NeoViewMouseEvent)) { return; }
		NeoViewMouseEvent e = (NeoViewMouseEvent)evt;

		// NeoViewMouseEvents return the _View_ they occurred on as the source,
		// rather than the Component like most AWTEvents
		Object source = e.getSource();

		if (source != view) { return; }
		int id = e.getID();

		NeoMouseEvent nevt =
			new NeoMouseEvent(e, this, NeoConstants.UNKNOWN, e.getCoordX(), e.getCoordY());
		// translating from NeoCanvas pixel location to
		//     widget pixel location
		Rectangle bnds = view.getComponent().getBounds();
		nevt.translatePoint(bnds.x, bnds.y);

		for (MouseListener ml : mouse_listeners) {
			if (id == MouseEvent.MOUSE_CLICKED) {
				ml.mouseClicked(nevt);
			} else if (id == MouseEvent.MOUSE_ENTERED) {
				ml.mouseEntered(nevt);
			} else if (id == MouseEvent.MOUSE_EXITED) {
				ml.mouseExited(nevt);
			} else if (id == MouseEvent.MOUSE_PRESSED) {
				ml.mousePressed(nevt);
			} else if (id == MouseEvent.MOUSE_RELEASED) {
				ml.mouseReleased(nevt);
			}
		}
		for (MouseMotionListener mml : mouse_motion_listeners) {
			if (id == MouseEvent.MOUSE_DRAGGED) {
				mml.mouseDragged(nevt);
			} else if (id == MouseEvent.MOUSE_MOVED) {
				mml.mouseMoved(nevt);
			}
		}
	}

	public void addRubberBandListener(NeoRubberBandListener l) {
		rubberband_listeners.add(l);
	}

	public void removeRubberBandListener(NeoRubberBandListener l) {
		rubberband_listeners.remove(l);
	}

}
