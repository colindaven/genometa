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

import com.affymetrix.genoviz.glyph.TransientGlyph;
import com.affymetrix.genoviz.glyph.GlyphStyle;
import com.affymetrix.genoviz.glyph.GlyphStyleFactory;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Stack;
import java.util.List;

/**
 * The base class that implements the GlyphI interface. All other glyphs are
 * subclasses of Glyph.  See GlyphI for better documentation of methods.
 * Though it has no drawn appearance of its own, Glyph can also act as an
 * invisible container for other child glyphs.
 */
public abstract class Glyph implements GlyphI  {
	public static final int DRAW_SELF_FIRST = 0;
	public static final int DRAW_CHILDREN_FIRST = 1;

	private static final boolean DEBUG = false;
	protected static final Color default_bg_color = Color.black;
	protected static final Color default_fg_color = Color.black;
	protected static final GlyphStyleFactory stylefactory = new GlyphStyleFactory(); // might want to set default colors;

	protected Rectangle2D.Double coordbox;
	protected Scene scene;
	private   Rectangle2D.Double cb2= null; // used as a temporary variable
	protected Rectangle pixelbox;
	protected int min_pixels_width=1;
	protected int min_pixels_height=1;
	protected GlyphI parent;
	protected List<GlyphI> children;
	protected GlyphStyle style;

	protected boolean isVisible;
	private Object info;
	protected PackerI packer;
	protected boolean selected;
	protected int draw_order = DRAW_SELF_FIRST;

	protected boolean selectable = true;

	public Glyph()
	{
		coordbox = new Rectangle2D.Double();
		pixelbox = new Rectangle();
		min_pixels_width=1;
		min_pixels_height=1;
		isVisible = true;

		style = stylefactory.getStyle( default_fg_color, default_bg_color );
	}

	public boolean withinView(ViewI view)
	{
		return getPositiveCoordBox().intersects(view.getCoordBox());
	}

	/**
	 * Selecting a region of a glyph.
	 * This base class defaults to selecting the whole glyph.
	 * Subclasses can override this for a more appropriate implementation.
	 *
	 * @param x ignored
	 * @param y ignored
	 * @param width ignored
	 * @param height ignored
	 */
	public void select(double x, double y, double width, double height) {
		setSelected(true);
	}

	/**
	 *  Default is that glyph does not support subselection.
	 *  Override this to indicate support for subselection.
	 */
	public boolean supportsSubSelection() {
		return false;
	}

	/**
	 *  Default for getSelectedRegion() is to return bounding box for the
	 *     entire glyph
	 */
	public Rectangle2D.Double getSelectedRegion() {
		if (selected) { return getPositiveCoordBox(); }
		else { return null; }
	}

	public void setDrawOrder(int order) {
		if ((draw_order == DRAW_SELF_FIRST) ||
				(draw_order == DRAW_CHILDREN_FIRST))
			draw_order = order;
	}

	public int getDrawOrder() {
		return draw_order;
	}

	public void drawTraversal(ViewI view)  {
		if (DEBUG) {
			System.err.println("called Glyph.drawTraversal() on " + this);
		}
		if (draw_order == DRAW_SELF_FIRST) {
			if (isVisible && (withinView(view) || RectangleIntersectHack(view))) {
				if (selected) { drawSelected(view); }
				else { draw(view); }
				if (children != null) { drawChildren(view); }
			}
		}
		else if (draw_order == DRAW_CHILDREN_FIRST) {
			if (isVisible && (withinView(view) || RectangleIntersectHack(view))) {
				if (children != null)  { drawChildren(view); }
				if (selected) { drawSelected(view); }
				else { draw(view); }
			}
		}
		if (DEBUG) {
			System.err.println("leaving Glyph.drawTraversal()");
		}
	}

	/**
	 * Hack needed to make Hairline label glyph visible after removing bioviews.Rectangle2D.intersects.
	 * Specifically, this handles the case where a 0-width/0-height rectangle intersects another rectangle.
	 * Is this an intersection or not?  Genoviz says yes, Java SDK says no.
	 */
	private boolean RectangleIntersectHack(ViewI view) {
		Rectangle2D.Double r= this.getPositiveCoordBox();
		Rectangle2D.Double v = view.getCoordBox();
		if (r.width == 0 || r.height == 0) {
			return !((r.x + r.width <= v.x) ||
				(r.y + r.height <= v.y) ||
				(r.x >= v.x + v.width) ||
				(r.y >= v.y + v.height));
		}
		return false;
	}

	protected void drawChildren(ViewI view) {
		if (children != null)  {
			GlyphI child;
			int numChildren = children.size();
			for ( int i = 0; i < numChildren; i++ ) {
				child = children.get( i );
				// TransientGlyphs are usually NOT drawn in standard drawTraversal
				if (!(child instanceof TransientGlyph) || drawTransients()) {
					child.drawTraversal(view);
				}
			}
		}
	}

	public void draw(ViewI view)  {
		if (DEBUG) {
			Graphics2D g = view.getGraphics();
			//MPTAG changed
//			g.setColor(Color.red);
			view.transformToPixels(coordbox, pixelbox);
			g.drawRect(pixelbox.x+1, pixelbox.y+1,
					pixelbox.width-2, pixelbox.height-2);
		}
	}

	/**
	 * Drawing selected glyphs is currently very inefficient
	 * especially for <code>HIGHLIGHT</code>,
	 * because they are generally being drawn <em>twice</em>
	 * once as unselected, then painted over with selected color...
	 * <p>Needs to be fixed!
	 * But it's a performance enhancement not added feature or bug fix,
	 * so low priority for now  -- GAH 10-6-97
	 */
	public void drawSelected(ViewI view) {

		// WARNING -- calling scene directly here is a good way of
		//   testing whether scene has been set in all selected glyphs --
		//   otherwise get NullPointerExceptions (happens for example when forget
		//   to set scene on "non-child" glyphs associated with others, such as
		//   arrow glyph in AlignmentGlyph, or full_rect in AlignedDNAGlyph
		//    int selection_style = scene.getSelectionAppearance();

		int selection_style = view.getScene().getSelectionAppearance();

		if (selection_style == Scene.SELECT_OUTLINE) {
			drawSelectedOutline(view);
		}
		else if (selection_style == Scene.SELECT_FILL) {
			drawSelectedFill(view);
		}
		else if (selection_style == Scene.BACKGROUND_FILL) {
			drawSelectedBackground(view);
		}
		else if( selection_style == Scene.SELECT_REVERSE ) {
			drawSelectedReverse(view);
		}
		else if (selection_style == Scene.SELECT_NONE) {
			draw(view);
		}
	}

	protected void drawSelectedBackground(ViewI view) {
		Graphics2D g = view.getGraphics();
		g.setColor(view.getScene().getSelectionColor());
		view.transformToPixels(getPositiveCoordBox(), pixelbox);
		g.fillRect(pixelbox.x-3, pixelbox.y-3,
				pixelbox.width+6, pixelbox.height+6);
		draw(view);
	}

	protected void drawSelectedOutline(ViewI view) {
		draw(view);
		Graphics2D g = view.getGraphics();
		g.setColor(view.getScene().getSelectionColor());
		// see WARNING above (in drawSelected())
		//      g.setColor(scene.getSelectionColor());
		view.transformToPixels(getPositiveCoordBox(), pixelbox);
		g.drawRect(pixelbox.x-2, pixelbox.y-2,
				pixelbox.width+3, pixelbox.height+3);
	}

	protected void drawSelectedFill(ViewI view) {
		Color tempcolor = this.getBackgroundColor();
		this.setBackgroundColor(view.getScene().getSelectionColor());
		this.draw(view);
		this.setBackgroundColor(tempcolor);
	}

	protected void drawSelectedReverse( ViewI view ) {
		Color bg = this.getBackgroundColor();
		Color fg = this.getForegroundColor();
		this.setBackgroundColor( fg );
		this.setForegroundColor( bg );
		this.draw(view);
		this.setBackgroundColor( bg );
		this.setForegroundColor( fg );
	}

	public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList,
			ViewI view)  {
		if (isVisible && intersects(pickRect, view))  {
			if (DEBUG)  {
				System.out.println("intersects");
			}
			if (hit(pickRect, view))  {
				if (!pickList.contains(this)) {
					pickList.add(this);
				}
				if (DEBUG)   {
					System.out.println("Hit " + this);
				}
			}
			if (children != null)  {
				GlyphI child;
				int childnum = children.size();
				for ( int i = 0; i < childnum; i++ ) {
					child = children.get( i );
					child.pickTraversal( pickRect, pickList, view );
				}
			}
		}
	}


	/**
	 * Detects whether or not this glyph is "hit"
	 * by a rectangle of pixel space within a view.
	 *
	 *<p> Note that this base implementation always returns false.
	 *    This is because container glyphs must return false.
	 *    They can intersect other rectangles.
	 *    But, they cannot be "hit".
	 *    Glyphs that extend this class and are not container glyphs
	 *    should override this method.
	 *
	 * @param pixel_hitbox ignored
	 * @param view ignored
	 * @return false
	 */
	public boolean hit(Rectangle pixel_hitbox, ViewI view)  {
		return false;
	}

	/**
	 * Detects whether or not this glyph is "hit"
	 * by a rectangle of coordinate space within a view.
	 *
	 *<p> Note that this base implementation always returns false.
	 *    This is because container glyphs must return false.
	 *    They can intersect other rectangles.
	 *    But, they cannot be "hit".
	 *    Glyphs that extend this class and are not container glyphs
	 *    should override this method.
	 *
	 * @param coord_hitbox ignored
	 * @param view ignored
	 * @return false
	 */
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view)  {
		return false;
	}

	/** Default implementation of method from GlyphI, always returns false
	 *  unless overridden in sub-class.
	 */
	public boolean isHitable() { return false; }

	public boolean intersects(Rectangle rect)  {
		return isVisible && rect.intersects(pixelbox);
	}

	public boolean intersects(Rectangle2D.Double rect, ViewI view)  {
		return isVisible && rect.intersects(getPositiveCoordBox());
	}

	public boolean inside(int x, int y)  {
		return isVisible && this.pixelbox.contains(x,y);
	}

	/**
	 *  Adds a child glyph.
	 *  Because the pickTraversal() method calls itself
	 *  recursively on its children, a glyph cannot be a
	 *  child of itself.
	 *  @throws IllegalArgumentException if you try to add a glyph as a child
	 *    of itself.
	 */
	public void addChild(GlyphI glyph, int position) {
		if (this==glyph) throw new IllegalArgumentException(
				"Illegal to add a Glyph as a child of itself!");
		GlyphI prev_parent = glyph.getParent();
		if (prev_parent != null) {
			prev_parent.removeChild(glyph);
		}
		if (children == null)  {
			children = new ArrayList<GlyphI>();
		}
		if (position == children.size()) {
			children.add(glyph);
		}
		else  {
			children.add(position, glyph);
		}
		// setParent() also calls setScene()
		glyph.setParent(this);
	}

	/** Adds the child to this object's list of children.
	 *  Note:  there is nothing preventing you from
	 *  adding the same child multiple times, although
	 *  that would probably be a bad thing to do.
	 */
	public void addChild(GlyphI glyph)  {
		GlyphI prev_parent = glyph.getParent();
		if (prev_parent != null) {
			prev_parent.removeChild(glyph);
		}
		if (children == null)  {
			children = new ArrayList<GlyphI>();
		}
		children.add(glyph);
		glyph.setParent(this);
	}

	/** Removes the child from this object's list of children,
	 *  and sets its parent to null (for improved garbage collection).
	 *  Note:  if the same child was added multiple times,
	 *  this will only remove one of the references to it and
	 *  will not set the parent to null.
	 *  Probably {@link #addChild(GlyphI)} should be re-written
	 *  to disallow that in the first place.
	 */
	public void removeChild(GlyphI glyph)  {
		if (children != null) {
			children.remove(glyph);
			if (children.size() == 0) { children = null; }
		}
		// null out the scene if glyph is removed
		glyph.setScene(null);
	}

	public void removeAllChildren() {
		if (children != null)  {
			for (int i=0; i<children.size(); i++) {
				children.get(i).setScene(null);
			}
		}
		children = null;
	}

	public int getChildCount() {
		if (children == null) { return 0; }
		else { return children.size(); }
	}

	public GlyphI getChild(int index) {
		return children.get(index);
	}

	public List<GlyphI> getChildren()  {
		return children;
	}

	public void setParent(GlyphI glyph)  {
		parent = glyph;
		if (glyph != null) {setScene(glyph.getScene());}
		else {setScene(null);}
	}

	public GlyphI getParent()  {
		return parent;
	}

	public void calcPixels (ViewI view)  {
		pixelbox = view.transformToPixels (coordbox, pixelbox);
	}

	public Rectangle getPixelBox()  {
		return pixelbox;
	}

	public Rectangle getPixelBox(ViewI view)  {
		pixelbox = view.transformToPixels (coordbox, pixelbox);
		return pixelbox;
	}

	/** Sets the minimum size in pixels. If d.width or d.height is negative,
	  this uses their absolute value instead. */
	public void setMinimumPixelBounds(Dimension d)   {
		// to save a miniscule amount of memory per Glyph, this is saved as
		// two integers rather than one Dimension object.
		min_pixels_width  = Math.abs(d.width);
		min_pixels_height = Math.abs(d.height);
	}

	/**
	 * Sets the coordinates of the Glyph.
	 * Follow AWT args convention: x, y, width, height.
	 * This will convert rectangles of a negative width and/or height
	 * to an equivalent rectangle with positive width and height.
	 */
	public void setCoords(double x, double y, double width, double height)  {
		if (width < 0) {
			x = x + width;
			width = -width;
		}
		if (height < 0) {
			y = y + height;
			height = -height;
		}
		coordbox.setRect(x, y, width, height);
	}

	public Rectangle2D.Double getCoordBox()   {
		return coordbox;
	}

	/** Returns the coordbox,
	 *  but converts rectangles with negative width or height
	 *  to an equivalent one with positive width and height.
	 */
	// TODO: remove this method.  Coordbox should always be positive anyway,
	// but setCoordbox() allows any coordbox to be used.
	protected final Rectangle2D.Double getPositiveCoordBox() {
		if (coordbox.width>=0 && coordbox.height>=0) {
			return coordbox;
		}

		if (cb2==null) {
			cb2 = new Rectangle2D.Double();}

		if (coordbox.width<0) {
			System.err.println("*********** WARNING: Found a negative width coord box. **********");
			cb2.x = coordbox.x+coordbox.width;
			cb2.width = -coordbox.width;
		}
		else {
			if ( Double.isNaN( coordbox.width ) ) {
				System.err.println( "******** WARNING: Coordbox width is not a number! How did this happen? *****" );
				coordbox.width = 0; // for now. To what should it be set?
			}
			cb2.x = coordbox.x;
			cb2.width = coordbox.width;
		}

		if (coordbox.height<0) {
			System.err.println("*********** WARNING: Found a negative height coord box. **********");
			cb2.y = coordbox.y+coordbox.height;
			cb2.height = -coordbox.height;
		}
		else {
			if ( Double.isNaN( coordbox.height ) ) {
				System.err.println( "******** WARNING: Coordbox height is not a number! How did this happen? *****" );
				coordbox.height = 0; // for now. To what should it be set?
			}
			cb2.y = coordbox.y;
			cb2.height = coordbox.height;
		}

		return cb2;
	}


	/**
	 * Replaces the coord box.
	 * Note that this does not make the assurances of setCoords().
	 * @see #setCoords
	 */
	public void setCoordBox(Rectangle2D.Double coordbox)   {
		this.coordbox = coordbox;
	}

	public void setForegroundColor(Color color)  {
		this.style = stylefactory.getStyle( color, style.getBackgroundColor(), style.getFont() );
	}

	public Color getForegroundColor()  {
		return this.style.getForegroundColor();
	}

	public void setBackgroundColor(Color color)  {
		this.style = stylefactory.getStyle( style.getForegroundColor(), color, style.getFont() );
	}

	public Color getBackgroundColor()  {
		return this.style.getBackgroundColor();
	}

	/** Semi-deprecated. Use {@link #setBackgroundColor(Color)}. */
	public void setColor(Color color)  {
		this.setBackgroundColor( color );
	}

	/** Semi-deprecated. Use {@link #getBackgroundColor}. */
	public Color getColor()  {
		return this.getBackgroundColor();
	}

	public void setFont(Font f) {
		this.style = stylefactory.getStyle( style.getForegroundColor(), style.getBackgroundColor(), f );
	}

	public Font getFont() {
		return this.style.getFont();
	}

	public void setInfo(Object info)  {
		this.info = info;
	}

	public Object getInfo()  {
		return info;
	}

	public void setVisibility(boolean isVisible)  {
		this.isVisible = isVisible;
	}

	public boolean isVisible()  {
		return isVisible;
	}

	public void setPacker(PackerI packer)  {
		this.packer = packer;
	}

	public PackerI getPacker()  {
		return packer;
	}

	public void pack(ViewI view) {
		if (packer == null) { return; }
		packer.pack(this, view);
	}

	public void moveRelative(double diffx, double diffy) {
		coordbox.x += diffx;
		coordbox.y += diffy;
		if (children != null) {
			int numchildren = children.size();
			for (int i=0; i<numchildren; i++) {
				children.get(i).moveRelative(diffx, diffy);
			}
		}
	}

	public void moveAbsolute(double x, double y) {
		double diffx = x - coordbox.x;
		double diffy = y - coordbox.y;
		this.moveRelative(diffx, diffy);
	}

	public void setScene(Scene s) {
		scene = s;
		if (children != null) {
			int size = children.size();
			for (int i=0; i<size; i++) {
				children.get(i).setScene(s);
			}
		}
	}

	public Scene getScene() {
		return scene;
	}


	/**
	 * Sets the selectability of the glyph.
	 *
	 * @param selectability
	 */
	public void setSelectable(boolean selectability) {
		if (!selectability) setSelected(false);
		this.selectable = selectability;
	}

	/**
	 * Indicates whether or not the glyph can be selected.
	 */
	public boolean isSelectable() {
		return this.selectable;
	}

	/**
	 * Selects the glyph if it is selectable.
	 * If it is not then this does nothing.
	 *
	 * @param selected true if the glyph is to be selected,
	 * false otherwise.
	 */
	public void setSelected(boolean selected) {
		if (this.selectable) this.selected = selected;
	}

	/**
	 * Indicates whether or not the glyph has been selected.
	 */
	public final boolean isSelected() {
		return selected;
	}


	public boolean drawTransients() {
		return false;
	}

	/**
	 *  Set trans to global transform for this glyph.
	 *  (Based on getChildTransform() of parent.)
	 */
	public boolean getGlobalTransform(ViewI view, LinearTransform trans) {
		trans.setTransform(view.getTransform());
		return getParent().getGlobalChildTransform(view, trans);
	}

	/** Default implementation does nothing. */
	public void getChildTransform(ViewI view, LinearTransform trans) {
	}

	public boolean getGlobalChildTransform(ViewI view, LinearTransform trans) {
		Stack<GlyphI> glstack = new Stack<GlyphI>();
		GlyphI rootgl = ((Scene)view.getScene()).getGlyph();
		GlyphI gl = this;
		glstack.push(gl);
		while (gl != rootgl) {
			gl = gl.getParent();
			// if get a null parent before getting root glyph, then fail and return
			if (parent == null) { return false; }
			glstack.push(gl);
		}
		trans.setTransform(view.getTransform());
		while (! (glstack.empty())) {
			gl = glstack.pop();
			gl.getChildTransform(view, trans);
		}
		return true;
	}

	/**
	 * (May not be necessary in current versions of Java.)
	 * Fixes a bug that can happen with AWT when drawing rectangles bigger than about 32000 pixels, by
	 *  trimming the pixelbox of a large rectangle to the region that intersects the view.
	 */
	public static Rectangle fixAWTBigRectBug(ViewI view, Rectangle pixelbox) {
		if (pixelbox.width >= 1024) {
			Rectangle compbox = view.getComponentSizeRect();
			pixelbox = pixelbox.intersection(compbox);
		}
		return pixelbox;
	}
}
