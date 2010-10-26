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

package com.affymetrix.genoviz.bioviews;
import java.awt.geom.Rectangle2D;
import java.util.List;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Rectangle;

/**
 *      GlyphI, along with SceneI and ViewI,
 *      is one of the three fundamental interfaces
 *      that form the core
 *      of Affymetrix' inner 2D structured graphics architecture.
 *
 * <p>  In the field of data visualization
 *      the term glyph has previously been used
 *      to refer to many different things.
 *      Closest in concept to GlyphI are the glyphs used for a case study
 *      in <cite>Design Patterns</cite>,
 *      p.38 (Gamma, Helm, Johnson &amp; Vlissides).
 *
 * <p>  Glyphs have three fundamental properties.
 *      They know:
 * <ol>
 * <li> how to draw themselves
 * <li> what region of coordinate space they occupy
 * <li> who their parent glyph and child glyphs are
 * </ol>
 *
 * <p>  A few other rules:
 * <ul>
 * <li> Glyphs can contain zero or more other glyphs as children.
 * <li> A glyph can only have zero or one parent.
 * <li> A glyph cannot contain itself as its own child.
 * </ul>
 *
 * @see com.affymetrix.genoviz.bioviews.SceneI
 * @see com.affymetrix.genoviz.bioviews.ViewI
 */
public interface GlyphI extends NeoConstants  {
	/**
	 * Draws the glyph and all its children.
	 * This can be implemented by recursively calling drawTraversal
	 * for each child.
	 *
	 * @param view a view upon which the glyph draws itself.
	 */
	public void drawTraversal(ViewI view);

	/**
	 * Draws the glyph.
	 *
	 * @param view a view upon which the glyph draws itself.
	 */
	public void draw(ViewI view);


	// A Glyph knows its parent and children.

	/**
	 * Sets the glyph's parent.
	 *
	 * @param parent contains the glyph as a child.
	 */
	public void setParent(GlyphI parent);

	/**
	 * Gets the glyph's parent.
	 *
	 * @return the glyph containing this one.
	 */
	public GlyphI getParent();

	/**
	 * Adds a glyph to this glyph's collection of children.
	 *
	 * @param child the glyph to add
	 */
	public void addChild(GlyphI child);

	/**
	 * Adds a glyph to this glyph's collection of children
	 * at a particular position.
	 * Other children at that position or further back
	 * are pushed back one.
	 *
	 * @param glyph the glyph to add
	 * @param position the position in which to put the new child.
	 */
	public void addChild(GlyphI glyph, int position);

	/**
	 * Removes a glyph from this glyph's collection of children.
	 *
	 * @param child the glyph to remove.
	 */
	public void removeChild(GlyphI child);

	/**
	 *  Remove all children of this glyph.
	 */
	public void removeAllChildren();

	/**
	 * Gets the entire collection of this glyph's children.
	 *
	 * @return a List containing the children.
	 */
	public List<GlyphI> getChildren();

	/**
	 *  Returns number of child glyph's this glyph has.
	 */
	public int getChildCount();

	/**
	 *  Returns Nth child glyph, where N is designated by the index argument.
	 */
	public GlyphI getChild(int index);


	// Parent gives Glyph its position in absolute coordinate space.
	// Using a view's transform, Glyph can calculate its pixel box.
	/**
	 * Gets the glyph's containing box in coordinate space.
	 *
	 * @return the CoordBox.
	 */
	public Rectangle2D.Double getCoordBox();

	/**
	 * Sets the glyph's containing box in coordinate space.
	 *
	 * @param coordbox the box.
	 */
	public void setCoordBox(Rectangle2D.Double coordbox);

	/**
	 * Sets the glyph's containing box in coordinate space.
	 *
	 * @param x the left side of the box.
	 * @param y the top of the box.
	 * @param width of the box.
	 * @param height of the box.
	 */
	public void setCoords(double x, double y, double width, double height);

	/**
	 * Recalculates the glyph's containing box in pixel space,
	 * stores the result in the glyph,
	 * and returns the result.
	 *
	 * @param view the view containing a transform.
	 * @return the pixelbox.
	 */
	public Rectangle getPixelBox(ViewI view);


	// A Glyph can advise its parent of its minimum recommended size.

	/**
	 * Sets the minimum size the glyph can assume
	 * in pixels.
	 *
	 * @param d the smallest height and width it needs.
	 */
	public void setMinimumPixelBounds(Dimension d);

	/**
	 *     Determines if a given rectangle "touches" this glyph
	 *     in coordinate space
	 *     in the given view.
	 *
	 * <p> The glyph may consider some or all of itself intangible.
	 *     For example, a container glyph should always return false.
	 *
	 * @param hitbox the rectangle in question.
	 * @param view the view in which they might overlap.
	 * @return true iff rect intersects this glyph's pixelbox within view
	 */
	public boolean hit(Rectangle2D.Double hitbox, ViewI view);

	/**
	 * Determines if a given rectangle intersects this glyph
	 * in coordinate space in the given view.
	 *
	 * @param rect the rectangle in question.
	 * @param view the view in which they might overlap.
	 */
	public boolean intersects(Rectangle2D.Double rect, ViewI view);

	/**
	 * Sets the selectability of the glyph.
	 *
	 * @param selectability
	 */
	public void setSelectable(boolean selectability);

	/**
	 * Indicates whether or not the glyph can be selected.
	 */
	public boolean isSelectable();

	/**
	 * Whether or not this glyph is hitable.
	 */
	public boolean isHitable();

	/**
	 * Sets whether or not the glyph is selected.
	 * <p>
	 * Selected glyphs should be visually distinguishable
	 * from unselected glyphs.
	 * <p>
	 * Widgets can have different <em>selection styles</em>.
	 * Choosing a selection style for a widget may change how
	 * glyphs are "highlighted", or shown to be selected.
	 * Selection style is set with {@link com.affymetrix.genoviz.widget.NeoAbstractWidget#setSelectionAppearance}.
	 * Note that a NeoWidget can have only one selection style
	 * at a time.
	 * <p>
	 * Note that if the glyph supports subselection,
	 * then calling <code>setSelected(true)</code> should select the whole glyph,
	 * and <code>setSelected(false)</code> should deselect the whole glyph.
	 * Also, after deselection,
	 * glyphs need not remember which subregions were previously selected.
	 * e.g. the {@link Glyph} class in this package does not remember.
	 * <p>
	 * This should be a noop when <code>setSelectable(false)</code> has been called.
	 *
	 * @param selected true iff the glyph is to be selected.
	 */
	public void setSelected(boolean selected);

	/**
	 * Selects a subregion of the glyph
	 * in coordinate space.
	 *
	 * @param x the left side of the subregion.
	 * This is relative to the overall coordinate space,
	 * not relative to the glyph.
	 * @param y the top of the subregion.
	 * This is relative to the overall coordinate space,
	 * not relative to the glyph.
	 * @param width of the subregion.
	 * @param height of the subregion.
	 */
	public void select(double x, double y, double width, double height);

	/**
	 * Indicates whether or not subselection is supported.
	 *
	 * @return true if subselection is supported.
	 */
	public boolean supportsSubSelection();

	/**
	 * Gets the region of the glyph that is selected.
	 * If the glyph does not support subselection,
	 * then this should return
	 * either the glyph's entire coordinate bounding box
	 * when selected or null when not selected
	 *
	 * @return the selected region.
	 */
	public Rectangle2D.Double getSelectedRegion();

	/**
	 * Indicates whether or not the glyph is selected.
	 *
	 * @return selection state.
	 */
	public boolean isSelected();

	/**
	 * Searches children for hits and add them to the pick vector.
	 * @param pickvec modified by this routine to return the results
	 */
	public void pickTraversal(Rectangle2D.Double pickrect, List<GlyphI> pickvec, ViewI view);


	/**
	 * Makes a glyph visible or invisible.
	 *
	 * @param isVisible make the glyph visible iff this is true.
	 */
	public void setVisibility(boolean isVisible);

	/**
	 * Finds out whether or not a glyph is visible.
	 *
	 * @return true iff the glyph is visible.
	 */
	public boolean isVisible();


	/**
	 * Associates information with the glyph.
	 * <p>
	 * This information Object is not necessarily meant
	 * to be used by the glyph itself.
	 * It could be used to associate some sort of data or model
	 * with the glyph.
	 *
	 * @param info A caller may store anything it wishes in here.
	 * @see #getInfo
	 */
	public void setInfo(Object info);

	/**
	 * Gets information associated with the glyph.
	 *
	 * @return anything previously set with setInfo
	 * null otherwise.
	 * @see #setInfo
	 */
	public Object getInfo();

	/**
	 * Associates this glyph with layout instructions.
	 * <p>
	 * The packer of a glyph determines
	 * how this glyph and all its children will be laid out
	 * when the GlyphI.pack() method is used.
	 */
	public void setPacker(PackerI p);
	/*
	 * Theoretically the packer of a glyph determines how this
	 * glyph and all its children will be laid out in calls to GlyphI.pack().
	 *
	 * In practice this is often superceded by
	 * factories handling packing.  Root glyphs, Container glyphs, and Tier Glyphs
	 * do often have their packers set.
	 */

	/**
	 * Finds out how a glyph is to be layed out.
	 *
	 * @return the packer containing layout instructions for the glyph.
	 * @see #setPacker
	 */
	public PackerI getPacker();

	/**
	 * Lays out the glyph's children.
	 * <p>
	 * It should do nothing when the packer is null.
	 * e.g. if setPacker() was never called.
	 *
	 * @param view the view in which to pack the glyph.
	 */
	public void pack(ViewI view);


	public void setForegroundColor(Color color);
	public Color getForegroundColor();
	public void setBackgroundColor(Color color);
	public Color getBackgroundColor();

	/**
	 * Sets the background color of the glyph.
	 *
	 * @param color
	 */
	public void setColor(Color color);

	/**
	 * Gets the background color of the glyph.
	 *
	 * @return the color set with setColor()
	 * or the default color (could be null)
	 * if setColor was never called.
	 * @see #setColor
	 */
	public Color getColor();


	/**
	 * Moves the glyph to a new position.
	 *
	 * @param x the new position of the glyph's left side
	 * in coordinate space.
	 * @param y the new position of the top of the glyph
	 * in coordinate space.
	 */
	public void moveAbsolute(double x, double y);

	/**
	 * Moves the glyph a specified distance.
	 *
	 * @param diffx the horizontal distance to move the glyph
	 * in coordinate space.
	 * @param diffy the vertical distance to move the glyph
	 * in coordinate space.
	 */
	public void moveRelative(double diffx, double diffy);


	/**
	 * Lets the glyph know what scene it is in.
	 * A glyph can be on only one scene.
	 *
	 * @param s the scene in which the glyph appears.
	 */
	public void setScene(Scene s);
	/*
	 * Sets the scene for the glyph.  An internal method used by NeoWidgets.
	 * Each NeoWidget can have multiple scenes (and NeoWidgets can share
	 * scenes if widget cloning is used).  A glyph can appear on only one
	 * scene.  Scenes know what glyphs are on them, so you might wonder why
	 * the glyph needs to know about the scene.  Main reason the glyph
	 * needs to know about the scene is during optimized damage propogation
	 * for efficient drawing.
	 */

	/**
	 * Finds out which scene contains the glyph.
	 *
	 * @return the Scene in which the GlyphI appears.
	 * @see #setScene
	 */
	public Scene getScene();

	/**
	 *  set trans to global transform for this glyph (based on
	 *    getChildTransform() of parent)
	 */
	public boolean getGlobalTransform(ViewI view, LinearTransform trans);

	/**
	 *  Given the input transform, modify to "relative" transform that should
	 *     be applied to glyph's children.
	 *  Usually this is same as the input transform, but some glyph's
	 *     implement nested transforms or other manipulations that
	 *     may modify transform that needs to be applied to glyph.
	 */
	public void getChildTransform(ViewI view, LinearTransform trans);

	/**
	 *  Given a view, manipulate trans so that it is the global
	 *     transform needed to map this glyph's children's coords
	 *     to pixel coords.
	 *  Usually this will be the same as the view's transform,
	 *     but some glyph's implement nested transforms or other manipulations
	 *     that may modify transform that needs to be applied to glyph.
	 */
	public boolean getGlobalChildTransform(ViewI view, LinearTransform trans);

	public boolean withinView(ViewI view);
}
