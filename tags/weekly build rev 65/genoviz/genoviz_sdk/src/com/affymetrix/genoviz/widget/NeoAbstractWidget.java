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

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.GeneralUtils;
import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.JScrollBar;

/**
 * Represents basic funcionality for all Widgets.
 * <p>
 * All widgets include notions of axis, establishing coordinate
 * space, indicating bounds, panning, zooming, selecting and placing
 * items at positions, glyph factories (for creating graphical
 * objects with common attributes), data adapters (for automating the
 * creation of graphical objects from data objects), dealing with
 * color, establishing window resize behavior, and managing event handlers.
 *
 * @version $Id: NeoAbstractWidget.java 7012 2010-10-12 15:10:24Z jnicol $
 */
public abstract class NeoAbstractWidget extends Container
	implements MouseListener, MouseMotionListener, KeyListener {
    /**
	 * No selection is done by the NeoWidget.
	 * A listener or superclass will still get all events and can react to them.
	 * @see #setSelectionEvent
	 */
	public static final int NO_SELECTION = 0;

	/**
	 * Selected on Event.MOUSE_DOWN.
	 * A listener or superclass will still get all events and can react to them.
	 * @see #setSelectionEvent
	 */
	public static final int ON_MOUSE_DOWN = 1;
	/**
	 * Selected on Event.MOUSE_UP.
	 * A listener or superclass will still get all events and can react to them.
	 * @see #setSelectionEvent
	 */
	public static final int ON_MOUSE_UP = 2;

	/**
	 * the index of the major (primary) axis,
	 * usually the horizontal X axis.
	 */
	public static final int PRIMARY_AXIS = 0;

	/**
	 * the index of the minor (secondary) axis,
	 * usually the vertical Y axis.
	 */
	public static final int SECONDARY_AXIS = 1;

	/**
	 * synonym for PRIMARY_AXIS.
	 *
	 * @see #PRIMARY_AXIS
	 */
	public static final int X = PRIMARY_AXIS;

	/**
	 * synonym for SECONDARY_AXIS
	 *
	 * @see #SECONDARY_AXIS
	 */
	public static final int Y = SECONDARY_AXIS;

	/**
	 * indicates that zooming should be focused
	 * on the top or left side of the widget.
	 * i.e. the uppermost edge or the leftmost edge
	 * of the viewable area remains fixed.
	 *
	 * @see #setZoomBehavior
	 */
	public static final int CONSTRAIN_START = 1;

	/**
	 * indicates that zooming should be focused
	 * on the center of the widget.
	 * i.e. the midpoint
	 * of the viewable area remains fixed.
	 *
	 * @see #setZoomBehavior
	 */
	public static final int CONSTRAIN_MIDDLE = 2;

	/**
	 * indicates that zooming should be focused
	 * on the bottom or right side of the widget.
	 * i.e. the bottom edge or the rightmost edge
	 * of the viewable area remains fixed.
	 *
	 * @see #setZoomBehavior
	 */
	public static final int CONSTRAIN_END = 3;

	/**
	 * indicates that zooming should be focused
	 * at a specified fixed coordinate.
	 *
	 * @see #setZoomBehavior
	 */
	public static final int CONSTRAIN_COORD = 4;

	/**
	 * indicates that the widget should be resized
	 * to fit within a container
	 * whenever that container is resized.
	 */
	public static final int FITWIDGET = 5;


	/**
	 * constrains high resolution zooming
	 * to integral values of pixels per coordinate.
	 * If the number of pixels per coordinate
	 * (<code>zoom_scale</code>) is greater than one,
	 * then <code>zoom_scale</code> is rounded to the nearest integer.
	 * <p>
	 * In NeoWidget,
	 * this is implemented only for zooming triggered by zoomer adjustables.
	 * Calling <a href="#zoom"><code>zoom()</code></a> directly has no effect.
	 *
	 * @see #INTEGRAL_COORDS
	 * @see #setScaleConstraint
	 * @see #zoom
	 * @see NeoWidget#setScaleConstraint
	 */
	public static final int INTEGRAL_PIXELS = 8;

	/**
	 * constrains low resolution zooming
	 * to integral values of coordinates per pixel.
	 * If the number of pixels per coordinate
	 * (<code>zoom_scale</code>) is less than one, then <code>zoom_scale</code>
	 * is modified such that <code>1/zoom_scale</code> (coords per pixel)
	 * is rounded to the nearest integer.
	 * <p>
	 * In NeoWidget,
	 * this is implemented only for zooming triggered by zoomer adjustables.
	 * Calling <a href="#zoom"><code>zoom()</code></a> directly has no effect.
	 *
	 * @see #INTEGRAL_PIXELS
	 * @see #setScaleConstraint
	 * @see #zoom
	 */
	public static final int INTEGRAL_COORDS = 9;

	/**
	 * constrain zooming to both
	 * INTEGRAL_PIXELS and INTEGRAL_COORDS.
	 *
	 * @see #INTEGRAL_PIXELS
	 * @see #INTEGRAL_COORDS
	 * @see #setScaleConstraint
	 * @see #zoom
	 */
	public static final int INTEGRAL_ALL = 10;

	/**
	 * indicates that a widget's bounds should be automatically expanded
	 * when an item is added beyond the widget's previous bounds.
	 *
	 * @see #NO_EXPAND
	 * @see #setExpansionBehavior
	 */
	public static final int EXPAND = 200;

	/**
	 * indicates that a widget's bounds
	 * should <em>not</em> be automatically expanded
	 * when an item is added beyond the widget's previous bounds.
	 *
	 * @see #EXPAND
	 * @see #setExpansionBehavior
	 */
	public static final int NO_EXPAND = 201;


	/**
	 * indicates that a component should be placed to the left
	 * within the widget.
	 */
	public static final int PLACEMENT_LEFT = 0;

	/**
	 * indicates that a component should be placed to the right
	 * within the widget.
	 */
	public static final int PLACEMENT_RIGHT = 1;

	/**
	 * indicates that a component should be placed at the top
	 * within the widget.
	 */
	public static final int PLACEMENT_TOP = 2;

	/**
	 * indicates that a component should be placed at the bottom
	 * within the widget.
	 */
	public static final int PLACEMENT_BOTTOM = 3;

	/**
	 * indicates that a component can be placed anywhere
	 * within the widget.
	 */
	public static final int PLACEMENT_NONE = 4;

	/**
	 * indicates that the coordinate scrolling increment should be
	 * automatically adjusted upon zooming
	 * so that the pixels scrolled remains constant.
	 *
	 * @see #setScrollIncrementBehavior
	 */
	public static final int AUTO_SCROLL_INCREMENT = 0;

	/**
	 * indicates that the coordinate scrolling increment should <em>not</em> be
	 * automatically adjusted upon zooming.
	 *
	 * @see #AUTO_SCROLL_INCREMENT
	 * @see #setScrollIncrementBehavior
	 */
	public static final int NO_AUTO_SCROLL_INCREMENT = 1;

	public static final int AUTO_SCROLL_HALF_PAGE = 2;

	protected Dimension pref_widg_size = new Dimension(1, 1);

	protected Set<MouseListener> mouse_listeners = new CopyOnWriteArraySet<MouseListener>();
	protected Set<MouseMotionListener> mouse_motion_listeners = new CopyOnWriteArraySet<MouseMotionListener>();
	protected Set<KeyListener> key_listeners = new CopyOnWriteArraySet<KeyListener>();

	protected Map<GlyphI,Object> glyph_hash = new HashMap<GlyphI,Object>();

	//TODO: This should maybe be Map<Object,List<GlyphI>>
	protected Map<Object,Object> model_hash = new HashMap<Object,Object>();

	protected boolean models_have_multiple_glyphs = false;

	private static final Hashtable<String,Color> colormap = GeneralUtils.getColorMap();

	protected int scroll_behavior[] = new int[2];

	// a list of selected glyphs
	protected List<GlyphI> selected = new ArrayList<GlyphI>();

	/**
	 * Places the component in the widget.
	 * Different widgets may want to restrict the placement possibilities.
	 *
	 * @param component the component to be placed.
	 * @param placement where to place it.
	 * Should be one of the PLACEMENT_* constants in this interface.
	 * @see #PLACEMENT_LEFT
	 * @see #PLACEMENT_RIGHT
	 * @see #PLACEMENT_TOP
	 * @see #PLACEMENT_BOTTOM
	 * @see #PLACEMENT_NONE
	 * @see #getPlacement
	 */
	public abstract void configureLayout(int component, int placement);

	/**
	 *  Returns true if any datamodels are represented by multiple glyphs.
	 *  WARNING: once one model is represented by multiple glyphs, this flag might only
	 *     be reset to false when clearWidget() is called
	 */
	public boolean hasMultiGlyphsPerModel() {
		return models_have_multiple_glyphs;
	}

	/**
	 * Associates an arbitrary datamodel object with a glyph.  Can be retrieved using
	 * <a href="#getDataModel">getDataModel</a>.  More than one <code>glyph</code>
	 * may be associated with one <code>datamodel</code>, but only one
	 * <code>datamodel</code> can be associated with a <code>glyph</code>.
	 *
	 * @param glyph       a GlyphI on the NeoAbstractWidget
	 * @param datamodel an arbitrary  Object
	 * @see #getDataModel
	 */
  @SuppressWarnings("unchecked")
  public void setDataModel(GlyphI glyph, Object datamodel) {
    // glyph to datamodel must be one-to-one
    // datamodel to glyph can be one-to-many

    glyph_hash.put(glyph, datamodel);
    glyph.setInfo(datamodel);

    // more than one glyph may be associated with the same datamodel!
    // therefore check and see if already a glyph associated with this datamodel
    // if so, create a List and add glyphs to it (or extend the pre-exisiting one)
    Object previous = model_hash.get(datamodel);
    if (previous == null) {
      model_hash.put(datamodel, glyph);
    }
    else {
      models_have_multiple_glyphs = true;
      if (previous instanceof List) {
        ((List<GlyphI>) previous).add(glyph);
      }
      else {
        List<GlyphI> glyphs = new ArrayList<GlyphI>();
        glyphs.add((GlyphI) previous);
        glyphs.add(glyph);
        model_hash.put(datamodel, glyphs);
      }
    }
  }

	/**
	 * Retrieve the datamodel associated with the glyph.  This facilitates
	 * efficient event handling by associating application-specific data to
	 * the visual glyphs.
	 *
	 * @param glyph a GlyphI on the widget
	 * @return the datamodel associated with <code>GlyphI</code>.
	 *
	 */
	public Object getDataModel(GlyphI glyph) {
		return glyph.getInfo();
	}

	/**
	 * Returns the first GlyphI found in the NeoAbstractWidget that is associated with
	 * the <code>datamodel</code>.  Typically, <code>datamodel</code> is
	 * an arbitrary datamodel that has been associated with one or more glyphs.
	 * If you know there is only one GlyphI associated with each datamodel, this
	 * method is more efficient than calling getItems(datamodel), which returns
	 * a List.
	 *
	 * @param datamodel an arbitrary object associated with one or
	 *   more glyphs.
	 * @return the GlyphI most recently associated with the datamodel
	 */
	@SuppressWarnings("unchecked")
	public <G extends GlyphI> G getItem(Object datamodel) {
		Object result = model_hash.get(datamodel);
		if (result instanceof GlyphI) {
			return (G)result;
		}
		if (result instanceof List && ((List)result).size() > 0) {
			List<G> vec = (List<G>)result;
			return vec.get(vec.size()-1);
		}
		return null;
	}

	/**
	 * Retrieves the <code>List</code> of glyphs associated with the
	 * <code>datamodel</code>.  Typically, <code>datamodel</code> is
	 * an arbitrary datamodel that has been associated with one or more glyphs.
	 *
	 * @param datamodel an arbitrary object associated with one or
	 *   more glyphs.
	 * @return the <code>List</code> of glyphs associated with <code>
	 *  datamodel</code>.
	 */
	@SuppressWarnings("unchecked")
	public <G extends GlyphI> List<G> getItems(Object datamodel) {
		Collections.singletonList(datamodel);
		Object result = model_hash.get(datamodel);
		if (result instanceof List) {
			return (List<G>) result;
		}
		List<G> vec = new ArrayList<G>();
		if (null != result) {
			vec.add((G) result);
		}
		return vec;
	}

	/**
	 * returns a list of all <code>Glyph</code>s at
	 *  <code>x,y</code> in this widget.
	 *
	 * @param x the double describing the X position
	 * @param y the double describing the Y position
	 *
	 * @return a <code>List</code> of <code>Glyph</code>s
	 * at <code>x,y</code>
	 */
	public abstract List<GlyphI> getItems(double x, double y, int location);

	/**
	 * Determines widget behavior along each axis if items are added beyond
	 * current bounds of the widget.  Valid values are EXPAND, in which case the
	 * widget's bounds are extended to encompass the new item's location, or
	 * NO_EXPAND, in which case the widget refuses to expand to encompass the
	 * new item
	 *
	 * @param axisid the axis ({@link #X} or {@link #Y}) to apply
	 *   the constraint.
	 * @param behavior the type of constraint to apply.  Valid
	 *  values are EXPAND and NO_EXPAND
	 *
	 * @see #EXPAND
	 * @see #NO_EXPAND
	 */
	public abstract void setExpansionBehavior(int axisid, int behavior);

	/**
	 * Gets the behavior set by setExpansionBehavior.
	 *
	 * @param axisid the axis (NeoAbstractWidget.X or NeoAbstractWidget.Y) whose expansion
	 *                   behavior is to be retrieved
	 *
	 * @see #setExpansionBehavior
	 */
	public abstract int getExpansionBehavior(int axisid);

	/**
	 * creates a named color
	 * and adds it to the widget's collection
	 * of named colors.
	 *
	 * @param name a unique identifier for the color.
	 * @param col  the <code>Color</code> to be associated with
	 *   <code>name</code>.
	 */
	public static void addColor(String name, Color col) {
		if (null == name) {
			throw new IllegalArgumentException("can't addColor without a name.");
		}
		if (null == col) {
			throw new IllegalArgumentException("can't add a null color.");
		}
		colormap.put(name, col);
	}

	/**
	 * gets the background color for a component widget
	 * within this widget.
	 *
	 * @param id identifies which component widget.
	 * @return the color assigned to the background.
	 */
	public abstract Color getBackground(int id);

	/**
	 * sets the background color for a component widget
	 * within this widget.
	 *
	 * @param id identifies the component widget to color.
	 * @param col is the color to assign to the background.
	 */
	public abstract void setBackground(int id, Color col);

	/**
	 * retrieves a named color.
	 *
	 * @param name the <code>String</code> label for a <code>Color</code>.
	 * @return the <code>Color</code> corresponding to <code>name</code>.
	 * @see #addColor
	 */
	public static Color getColor(String name) {
		if (null == name) {
			throw new IllegalArgumentException("can't getColor without a name.");
		}
		return colormap.get(name);
	}

	/**
	 * retrieves a color's name.
	 *
	 * @param theColor a <code>Color</code> to look for.
	 * @return a <code>String</code> label associated with a color.
	 * @see #addColor
	 */
	public static String getColorName(Color theColor) {
		if (null == theColor) {
			throw new IllegalArgumentException("can't get a name for a null color.");
		}
		Enumeration it = colormap.keys();
		while (it.hasMoreElements()) {
			String candidate = (String)it.nextElement();
			if (theColor.equals(colormap.get(candidate))) {
				return candidate;
			}
		}
		return null;
	}
	
	/**
	 * enumerates all the color names.
	 *
	 * @return an <code>Enumeration</code> of all color name <code>String</code>s
	 *   set by <code>addColor</code>
	 * @see #addColor
	 */
	public static Enumeration<String> getColorNames() {
		return colormap.keys();
	}

	/**
	 * returns the bounding rectangle of the glyph in coordinates
	 */
	public abstract Rectangle2D.Double getCoordBounds(GlyphI glyph);

	/**
	 * Updates the visual appearance of the widget.
	 * It is important to call this method
	 * to view any externally introduced changes
	 * in widget appearance
	 * since the last call to updateWidget().
	 */
	public abstract void updateWidget();

	/**
	 * Updates the visual appearance of the widget.
	 * This form allows you to force complete redrawing of the entire widget.
	 * <p>
	 * An implementation can use this as an interim measure
	 * or in place of smooth internal optimizations.
	 * Generally,
	 * updateWidget() with no arguments should have the same effect
	 * as updateWidget(true),
	 * but may be more efficient.
	 * updateWidget(false) should be equivalent to updateWidget().
	 *
	 * @param full_update indicates whether or not the entire widget
	 *                    should be redrawn.
	 */
	public abstract void updateWidget(boolean full_update);

	/**
	 * sets the visibility of <code>item</code> for this widget.
	 *
	 * @param glyph the GlyphI to modify visibility of.
	 * @param visible a boolean indicator of visibility.  if false,
	 *   then the GlyphI is not displayed.
	 */
	public abstract void setVisibility(GlyphI glyph, boolean visible);

	/**
	 * sets the visibility of all glyphs in List for this widget.
	 *
	 * @param glyphs List of GlyphIs to modify visibility;
	 * @param visible a boolean indicator of visibility.  if false,
	 *   then the GlyphI is not displayed.
	 */
	public abstract void setVisibility(List<GlyphI> glyphs, boolean visible);

	/**
	 * gets the visibility of an item in this widget.
	 *
	 * @param glyph the GlyphI whose visibility is queried
	 */
	public static boolean getVisibility(GlyphI glyph) {
		return glyph.isVisible();
	}


	/**
	 * modifies the position of <code>glyph</code> to be the
	 * new absolute position (<code>x,y</code>) specified in
	 * coordinate space (not pixels).
	 *
	 * @param glyph the GlyphI to move
	 * @param x the absolute double position along the X axis.
	 * @param y the absolute double position along the Y axis.
	 * @see #moveRelative
	 * @see NeoMap#addItem
	 */
	public void moveAbsolute(GlyphI glyph, double x, double y) {
		glyph.moveAbsolute(x, y);
	}

	/**
	 * Modifies the position of all <code>glyphs</code>  in List to be the
	 * new absolute position (<code>x,y</code>) specified in
	 * coordinate space (not pixels).
	 * @param glyphs the List of GlyphIs to move
	 * @param x the absolute double position along the X axis.
	 * @param y the absolute double position along the Y axis.
	 * @see #moveRelative
	 * @see NeoMap#addItem
	 */
	public void moveAbsolute(List<GlyphI> glyphs, double x, double y) {
		for (GlyphI glyph : glyphs) {
			moveAbsolute(glyph, x, y);
		}
	}

	/**
	 * update the position of <code>glyph</code> by <code>diffx</code>
	 * and <code>diffy</code> in the X and Y axes respectively,
	 * relative to the current position of <code>glyph</code>, where
	 * the current position of <code>glyph</code> is the coordinate of the
	 * top left coordinate of <code>glyph</code>'s bounding box.
	 * Offsets are specified in coordinate space (not pixels).
	 *
	 * @param glyph the GlyphI to move
	 * @param diffx the double relative offset along the X axis
	 * @param diffy the double relative offset along the Y axis
	 * @see #moveAbsolute
	 * @see NeoMap#addItem
	 */
	public void moveRelative(GlyphI glyph, double diffx, double diffy) {
		glyph.moveRelative(diffx, diffy);
	}

	/**
	 * update the position of all <code>glyphs</code> in List by
	 * <code>diffx</code> and <code>diffy</code> in the X and Y axes respectively,
	 * relative to the current position of <code>glyphs</code>, where
	 * the current position of a <code>glyph</code> is the coordinate of the
	 * top left corner of the <code>glyph</code>'s bounding box.
	 * Offsets are specified in coordinate space (not pixels).
	 *
	 * @param glyphs the List of GlyphIs to move
	 * @param x the double relative offset along the X axis
	 * @param y the double relative offset along the Y axis
	 * 
	 *
	 */
	public void moveRelative(List<GlyphI> glyphs, double x, double y) {
		for (GlyphI glyph : glyphs) {
			moveRelative(glyph, x, y);
		}
	}

	/**
	 * Modifies the way that scrolling is performed for an axis.
	 *
	 * @param id       identifies which axis (X or Y) is being queried.
	 * @param behavior AUTO_SCROLL_INCREMENT or NO_AUTO_SCROLL_INCREMENT
	 *
	 * @see #getScrollIncrementBehavior
	 */
	public void setScrollIncrementBehavior(int id, int behavior) {
		scroll_behavior[id] = behavior;
	}

	/**
	 * Use this to decide whether or not the scrolling increment
	 * is being automatically readjusted.
	 *
	 * @param id identifies which axis (X or Y) is being queried.
	 *
	 * @return a constant indicating the scroll behavior.  Valid values
	 *  are NeoAbstractWidget.AUTO_SCROLL_INCREMENT and
	 *  NeoAbstractWidget.NO_AUTO_SCROLL_INCREMENT
	 *
	 * @see #setScrollIncrementBehavior
	 */
	public int getScrollIncrementBehavior(int id) {
		return scroll_behavior[id];
	}


  /**
   * Indicates whether a given glyph is selected.
   * @param g The glyph to check for selected status.
   * @return <code>true</code> if the glyph is selected, else <code>false</code>.
   */
  public boolean isSelected(GlyphI g) {
    return selected.contains(g);
  }
  
	/**
	 * adds <code>glyph</code> to the list of selected glyphs for this
	 * widget.  Selected glyphs will be displayed differently than
	 * unselected glyphs, based on selection style
	 *
	 * Subclasses should implement this. Default does nothing.
	 * Implementations should add selections to the List 'selected',
	 * in addition to any other tasks specific to those classes.
	 *
	 * @param glyph a <code>GlyphI</code> to select
	 * @see #deselect
	 * @see #getSelected
	 */
	public abstract void select(GlyphI glyph);

	/**
	 * adds all glyphs in List <code>glyphs</code> to the list of
	 * selected glyphs for this widget.  Selected glyphs will be displayed
	 * differently than unselected glyphs, based on selection style
	 *
	 * @param glyphs a List of <code>GlyphIs</code> to select
	 * @see #deselect
	 * @see #getSelected
	 */
	public void select(List<GlyphI> glyphs) {
		if (glyphs == null) {
			return;
		}
		for (GlyphI glyph : glyphs) {
			select(glyph);
		}
	}

	/**
	 *  Clears all selections by actually calling {@link #deselect(GlyphI)}
	 *  on each one as well as removing them from the list of selections.
	 */
	public void clearSelected() {
		while (selected.size() > 0) {
			// selected.size() shrinks because deselect(glyph)
			//    calls selected.remove()
			Object gl = selected.get(0);
			if (gl == null) { selected.remove(0); }
			else {
				deselect((GlyphI)gl);
			}
		}
		selected.clear();
	}

	/**
	 * Removes <code>glyph</code> from the list of selected glyphs for this widget.
	 * Visually unselects glyph.
	 *
	 * @see #select
	 * @see #getSelected
	 */
	public abstract void deselect(GlyphI glyph);

	/**
	 * Removes all glyphs in List <code>glyphs</code> from the list of selected
	 * glyphs for this widget.  Visually unselects glyph.
	 *
	 * @see #select
	 * @see #getSelected
	 */
	public void deselect(List<GlyphI> glyphs) {
		// need to special case if glyphs argument is ref to same List as selected,
		//   since the deselect(Object) will cause shrinking of vec size as
		//   it is being looped through
		if (glyphs == null) {
			return;
		}
		if (glyphs == selected) {
			clearSelected();
		}
		for (int i=0; i<glyphs.size(); i++) {
			deselect(glyphs.get(i));
		}
	}

	/**
	 * retrieves all currently selected glyphs.
	 *
	 * @return a <code>List</code> of all selected GlyphIs
	 * @see #deselect
	 * @see #select
	 */
	public List<GlyphI> getSelected() {
		return selected;
	}

	/** Clears all graphs from the widget.
	 *  This default implementation simply removes all elements from the
	 *  list of selections.  (It does this without calling clearSelected(),
	 *  because it is faster to skip an explicit call to deselect(GlyphI)
	 *  for each Glyph.)
	 *  Subclasses should call this method during their own implementation.
	 *  Subclasses may choose to call clearSelected() before calling this
	 *  method if they require an explicit call to deselect(GlyphI) for
	 *  each Glyph.
	 */
	public void clearWidget() {
		selected.clear();
		// reset glyph_hash
		glyph_hash = new HashMap<GlyphI,Object>();

		// reset model_hash
		model_hash = new HashMap<Object,Object>();

		models_have_multiple_glyphs = false;
	}

	/**
	 * If this widget contains other widgets, returns the internal widget
	 *    at the given location.
	 *
	 * @param location where to find the component widget.
	 * @return the component widget.
	 */
	public abstract NeoAbstractWidget getWidget(int location);

	// implementing MouseListener interface and collecting mouse events
	public void mouseClicked(MouseEvent e) { heardMouseEvent(e); }
	public void mouseEntered(MouseEvent e) { heardMouseEvent(e); }
	public void mouseExited(MouseEvent e) { heardMouseEvent(e); }
	public void mousePressed(MouseEvent e) { heardMouseEvent(e); }
	public void mouseReleased(MouseEvent e) { heardMouseEvent(e); }

	// implementing MouseMotionListener interface and collecting mouse events
	public void mouseDragged(MouseEvent e) { heardMouseEvent(e); }
	public void mouseMoved(MouseEvent e) { heardMouseEvent(e); }

	public abstract void heardMouseEvent(MouseEvent evt);

	@Override
	public void addMouseListener(MouseListener l) {
		mouse_listeners.add(l);
	}

	@Override
	public void removeMouseListener(MouseListener l) {
		mouse_listeners.remove(l);
	}

	@Override
	public void addMouseMotionListener(MouseMotionListener l) {
		mouse_motion_listeners.add(l);
	}

	@Override
	public void removeMouseMotionListener(MouseMotionListener l) {
		mouse_motion_listeners.remove(l);
	}

	@Override
	public void addKeyListener(KeyListener l) {
		key_listeners.add(l);
	}

	@Override
	public void removeKeyListener(KeyListener l) {
		key_listeners.remove(l);
	}

	/**
	 * To be called when the object is no longer needed. Eliminate some references, as is necessary
	 * for garbage collection to occur.
	 */
	public void destroy() {
		key_listeners.clear();
		mouse_motion_listeners.clear();
		mouse_listeners.clear();
		glyph_hash.clear();
		model_hash.clear();
		selected.clear();
	}

	// Implementing KeyListener interface and collecting key events
	public void keyPressed(KeyEvent e) { heardKeyEvent(e); }
	public void keyReleased(KeyEvent e) { heardKeyEvent(e); }
	public void keyTyped(KeyEvent e) { heardKeyEvent(e); }

	public void heardKeyEvent(KeyEvent e) {
		int id = e.getID();
		if (key_listeners.size() > 0) {
			KeyEvent nevt =
				new KeyEvent(this, id, e.getWhen(), e.getModifiers(),
						e.getKeyCode(), e.getKeyChar());
			for (KeyListener kl : key_listeners) {
				if (id == KeyEvent.KEY_PRESSED) {
					kl.keyPressed(nevt);
				}
				else if (id == KeyEvent.KEY_RELEASED) {
					kl.keyReleased(nevt);
				}
				else if (id == KeyEvent.KEY_TYPED) {
					kl.keyTyped(nevt);
				}
			}
		}
	}

	/**
	 *  Reshapes the component.
	 *  Due to the way the Component class source code from Sun is written, it is this
	 *  method that we must override, not setBounds(), even though this method
	 *  is deprecated.
	 *  <p>
	 *  Users of this class should call setBounds(), but
	 *  when extending this class, override this, not setBounds().
	 *
	 *  @deprecated use {@link #setBounds(int,int,int,int)}.
	 */
	@Deprecated
	@Override
		public void reshape(int x, int y, int width, int height) {
			pref_widg_size.setSize(width, height);
			super.reshape(x, y, width, height);
		}

	@Override
	public Dimension getPreferredSize() {
		return pref_widg_size;
	}

	@Override
	public void setPreferredSize(Dimension d) {
		pref_widg_size = d;
	}

	@Override
	public void setCursor(Cursor cur) {
		for (Component comp : this.getComponents()) {
			comp.setCursor(cur);
		}
		super.setCursor(cur);
	}

	/**
	 * zoom this widget to a scale of <code>zoom_scale</code> along the
	 * <code>id</code>-th axis.
	 *
	 * @param id  indicates which axis to zoom.
	 *   valid values are NeoAbstractWidget.X or NeoAbstractWidget.Y.
	 *
	 * @param zoom_scale the double indicating the number of pixels
	 *   per coordinate
	 */
	public abstract void zoom(int id, double zoom_scale);

	/**
	 * sets the maximum allowable <code>zoom_scale</code> for this widget.
	 * For example, if at
	 * the highest resolution, you wish to display individual bases of
	 * a sequence, then set <code>max</code> to the width of a character
	 * of the desired font.
	 *
	 * @param axisid indicates which axis to apply this constraint.
	 *   valid values are {@link #X} or {@link #Y}.
	 *
	 * @param max  the double describing the maximum pixels per coordinate;
	 *   should generally be the maximum size (in pixels)
	 *   of a visual item.
	 *
	 * @see #zoom
	 * @see #getMaxZoom
	 */
	public abstract void setMaxZoom(int axisid, double max);

	/*
	 * sets the minimum allowable <code>zoom_scale</code> for this widget.
	 * For example, if at lowest resolution, you wish to ensure that at
	 * least one pixel is displayed per base and the coordinate system
	 * is set such that each base corresponds to a unit, then set
	 * <code>min</code> to 1.
	 *
	 * @param id  indicates which axis to apply this constraint.
	 *   valid values are NeoAbstractWidget.X or NeoAbstractWidget.Y.
	 *
	 * @param min  the double describing the minimum pixels per coordinate;
	 *   should generally be the minimum size (in pixels)
	 *   of a visual item.
	 *
	 * @see #zoom
	 * @see #getMinZoom
	 */
	public abstract void setMinZoom(int axisid, double min);

	/**
	 * returns the currently set maximum <code>zoom_scale</code>.
	 *
	 * @return the maximum number of pixels per coordinate.
	 * @see #setMaxZoom
	 */
	public abstract double getMaxZoom(int axisid);

	/**
	 * returns the currently set minimum <code>zoom_scale</code>.
	 *
	 * @return the minimum number of pixels per coordinate.
	 * @see #setMinZoom
	 */
	public abstract double getMinZoom(int axisid);

	/**
	 * determine where a component was placed.
	 *
	 * @param component the component to look for.
	 * @return indication of the placement.
	 * Should be one of the PLACEMENT_* constants in this interface.
	 * @see #configureLayout
	 */
	public abstract int getPlacement(int component);

	/**
	 * Adjusts this widget such that the <code>View</code>, scrollbars, etc., fit
	 * within the current bounds of the widget.
	 *
	 * @param xstretch the boolean determines whether stretchToFit
	 *   is applied along the X axis.
	 * @param ystretch the boolean determines whether stretchToFit
	 *   is applied along the Y axis.
	 */
	public abstract void stretchToFit(boolean xstretch, boolean ystretch);


	/**
	 * associates an adjustable component
	 * to control zooming along the specified axis.
	 *
	 * @param id identifies the axis of zooming.
	 *           Should be X or Y.
	 * @param adj an <code>Adjustable</code>
	 *            to be associated with the axis.
	 *            Typically this will be a scrollbar.
	 * @see #X
	 * @see #Y
	 */
	public abstract void setZoomer(int id, Adjustable adj);

	/**
	 * associates an adjustable component
	 * to control scrolling along the specified axis.
	 *
	 * @param id identifies the axis of scrolling.
	 *           Should be {@link #X} or {@link #Y}.
	 * @param adj an <code>Adjustable</code>
	 *            to be associated with the axis.
	 *            Typically this will be a scrollbar.
	 */
	public abstract void setScroller(int id, JScrollBar adj);

	/**
	 * scrolls this widget along the specified axis.
	 *
	 * @param id    identifies which axis to scroll.
	 *     valid values are NeoAbstractWidget.X or NeoAbstractWidget.Y.
	 * @param value  the double distance in coordinate space
	 *               to scroll.
	 * @see #X
	 * @see #Y
	 */
	public abstract void scroll(int id, double value);

	/**
	 * If this widget contains other widgets, returns the internal widget
	 *    that contains the given GlyphI.
	 *
	 * @param gl the glyph to search for
	 * @return the component widget.
	 */
	public abstract NeoAbstractWidget getWidget(GlyphI gl);

	/**
	 * constrains zooming along the given axis to the given constraint.
	 * You can focus horizontal zooming at the left edge, center or right edge.
	 * You can focus vertical zooming at the top, center, or bottom.
	 *
	 * @param axisid the axis (X or Y) to constrain.
	 * @param constraint the type desired.
	 *        Valid values are
	 *        CONSTRAIN_START, CONSTRAIN_MIDDLE, and CONSTRAIN_END.
	 *
	 * @see #X
	 * @see #Y
	 * @see #CONSTRAIN_START
	 * @see #CONSTRAIN_MIDDLE
	 * @see #CONSTRAIN_END
	 */
	public abstract void setZoomBehavior(int axisid, int constraint);

	/**
	 * constrains zooming along the given axis to the given point.
	 * This form of the setZoomBehavior method is used to constrain
	 * (or focus) zooming to a particular coordinate
	 * rather than CONSTRAIN_START, CONSTRAIN_MIDDLE, or CONSTRAIN_END.
	 *
	 * @param axisid the axis (X or Y) to constrain.
	 * @param constraint the type desired.
	 *        The only valid value is CONSTRAIN_COORD.
	 * @param coord the coordinate at which to focus zooming.
	 *
	 * @see #X
	 * @see #Y
	 * @see #CONSTRAIN_COORD
	 */
	public abstract void setZoomBehavior(int axisid, int constraint, double coord);

	/**
	 * Controls the scale values allowed during zooming.
	 *
	 * Scale constraints are currently only considered during
	 *    zooming with zoomer[] adjustables
	 *
	 * @param axisid     X or Y
	 * @param constraint INTEGRAL_PIXELS, INTEGRAL_COORDS, or INTEGRAL_ALL
	 *
	 * @see #INTEGRAL_PIXELS
	 * @see #INTEGRAL_COORDS
	 * @see #INTEGRAL_ALL
	 */
	public abstract void setScaleConstraint(int axisid, int constraint);


	/**
	 * turns rubber banding on and off.
	 * Configuration for rubberbanding
	 *  currently options are only to turn rubber banding on or off,
	 *  but anticipate having a longer signature for color, event mapping, etc.
	 *
	 * @param activate the boolean indicator.  if true, then rubber
	 *   banding is activated.
	 */
	public abstract void setRubberBandBehavior(boolean activate);

	/**
	 * specifies the manner in which selected items are visually displayed.
	 *
	 * @param behavior how selected Glyphs are visually differentiated
	 * from unselected Glyphs.
	 * Valid values are
	 * SceneI.SELECT_FILL,
	 * SeneI.SELECT_NONE,
	 * and
	 * SceneI.SELECT_OUTLINE.
	 *
	 * @see com.affymetrix.genoviz.bioviews.SceneI#SELECT_FILL
	 * @see com.affymetrix.genoviz.bioviews.SceneI#SELECT_NONE
	 * @see com.affymetrix.genoviz.bioviews.SceneI#SELECT_OUTLINE
	 */
	public abstract void setSelectionAppearance(int behavior);

	/**
	 * specifies the color in which selected items are visually displayed.
	 *
	 * @param color the color specification to use for selection.
	 */
	public abstract void setSelectionColor(Color color);

	/**
	 * returns the appearance set by setSelectionAppearance.
	 *
	 * @see #setSelectionAppearance
	 */
	public abstract int getSelectionAppearance();

	/**
	 * returns the Color set by setSelectionColor.
	 *
	 * @see #setSelectionColor
	 */
	public abstract Color getSelectionColor();

	/**
	 * determines whether or not sub-selection of glyphs is allowed.
	 *
	 * @param allowed <code>true</code> indicates that sub-selections
	 *   of glyphs are allowed.
	 */
	public abstract void setSubSelectionAllowed(boolean allowed);

	/**
	 * returns the current setting for sub-selection.
	 *
	 * @return <code>true</code> if sub-selection is currently allowed.
	 * @see #setSubSelectionAllowed
	 */
	public abstract boolean isSubSelectionAllowed();

	/**
	 * sets the pointing precision of the mouse.
	 *
	 * @param blur the number of pixels from the edge of glyphs.
	 * When the mouse is clicked this close to the glyph
	 * the glyph is selected.
	 */
	public abstract void setPixelFuzziness(int blur);

	/**
	 * gets the pointing precision of the mouse.
	 *
	 * @return the number of pixels around glyph bounds
	 * considered to be "within" the glyph.
	 * @see #setPixelFuzziness
	 */
	public abstract int getPixelFuzziness();

	/**
	 * removes the <code>glyph</code> from this widget
	 *
	 * @param glyph the GlyphI to remove
	 * @see NeoMap#addItem
	 */
	public abstract void removeItem(GlyphI glyph);

	/**
	 * Removes all GlyphI's in List from this widget
	 *
	 * @param glyphs the List of GlyphIs to remove
	 * @see NeoMap#addItem
	 */
	public abstract void removeItem(List<GlyphI> glyphs);

	/**
	 *  returns true if the glyph supports selection of a subregion
	 *  in addition to selection of the whole item
	 */
	public abstract boolean supportsSubSelection(GlyphI glyph);


	/**
	 * Make this glyph be drawn before all its siblings
	 * (a more drastic method is toFront(),
	 * which is only implemented for NeoMap)
	 */
	public abstract void toFrontOfSiblings(GlyphI glyph);

	/**
	 *  Make this glyph be drawn behind all its siblings
	 *  (a more drastic method is toBack(), which is only implemented for NeoMap)
	 */
	public abstract void toBackOfSiblings(GlyphI glyph);

	/**
	 * Determines automatic selection behavior.
	 * If <code>theEvent == ON_MOUSE_UP</code>
	 * then automatic selection occurs on the mouse up event.  Similarly,
	 * if <code>theEvent == ON_MOUSE_DOWN</code> then a automatic selection
	 * occurs on the mouse down event.  Automatic selection is disabled for the
	 * NeoWidget if <code>theEvent == NO_SELECTION</code>.
	 *
	 * @param theEvent
	 *        all NeoWidgets support NO_SELECTION, ON_MOUSE_DOWN, or ON_MOUSE_UP
	 *        some widgets support additional options
	 */
	public abstract void setSelectionEvent(int theEvent);


	/**
	 * Gets the selection method for automatic selection in the NeoWidget.
	 */
	public abstract int getSelectionEvent();
}
