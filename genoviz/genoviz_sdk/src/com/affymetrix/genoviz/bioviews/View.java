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

import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D.Double;
import java.util.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import com.affymetrix.genoviz.awt.NeoCanvas;
import com.affymetrix.genoviz.event.NeoPaintEvent;
import com.affymetrix.genoviz.event.NeoPaintListener;
import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.event.NeoViewMouseEvent;
import com.affymetrix.genoviz.glyph.TransientGlyph;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * implementation of ViewI interface.
 * See ViewI for better documentation of methods.
 */
public class View implements ViewI, NeoPaintListener,
		MouseListener, MouseMotionListener, KeyListener {

	private final Rectangle scratch_pixelbox = new Rectangle();
	protected Rectangle pixelbox;

	// GAH 2006-03-28  for experimental multiscreen support
	// View may actually be a "subview" (for example when trying to farm single NeoMap out to multiple
	//    NeoMap children, or during scroll optimizations), in which case glyphs may want
	//    to know bounds, etc. of full virtual "parent" view.
	// If this is not a subview, then full_view = this view
	protected ViewI full_view = null;

	// View currently requires specific Scene implementation to
	//    handle optimization method calls
	protected Scene scene;
	protected LinearTransform transform;
	protected NeoCanvas component;
	protected Rectangle2D.Double coordbox;
	protected Graphics2D graphics;
	protected final Set<MouseListener> mouse_listeners = new CopyOnWriteArraySet<MouseListener>();
	protected final Set<MouseMotionListener> mouse_motion_listeners = new CopyOnWriteArraySet<MouseMotionListener>();
	protected final Set<KeyListener> key_listeners = new CopyOnWriteArraySet<KeyListener>();
	/**
	 *  Vector of viewbox listeners to be notified immediately _before_
	 *    view is drawn with changed bounding box
	 */
	protected final Set<NeoViewBoxListener> predraw_viewbox_listeners = new CopyOnWriteArraySet<NeoViewBoxListener>();
	/**
	 *  Vector of viewbox listeners to be notified immediately _after__
	 *    view is drawn with changed bounding box
	 */
	protected final Set<NeoViewBoxListener> viewbox_listeners = new CopyOnWriteArraySet<NeoViewBoxListener>();
	/** fields to help with optimizations **/
	protected boolean scrolling_optimized = false;
	protected boolean damage_optimized = false;
	// for now always use buffer-optimized draw (IF view is buffered)
	protected boolean buffer_optimized = true;
	protected boolean buffered = false;
	protected LinearTransform lastTransform;  // copy of transform from last draw
	// I think prevCalcCoordBox can be replaced with prevCoordBox -- GAH 8/4/97
	protected Rectangle2D.Double prevCalcCoordBox;
	protected Rectangle2D.Double prevCoordBox;
	protected Image bufferImage;
	protected Graphics bufferGraphics;
	protected Dimension bufferSize;
	private boolean firstScrollOptimizedDraw = true;
	private boolean firstDamageOptimizedDraw = true;
	private boolean firstBufferOptimizedDraw = true;
	protected final Rectangle damagePixelBox = new Rectangle();
	protected Dimension component_size;
	protected Rectangle component_bounds;
	protected Rectangle component_size_rect;
	protected int drawCount = 0;

	/*
	Views have a few boxes that aren't visible to the outside world
	These are to store temporary stuff, for instance to have a
	destination Rectangle2D.Double when doing transformations that actually
	map to pixel space
	 */
	private final Rectangle2D.Double scratch_coords;
	private final Point2D.Double scratch_coord;
	protected Rectangle scene_pixelbox;
	protected Rectangle2D.Double scene_coordbox;

	public View() {
		full_view = this;
		// transforms initialized to Identity transform
		transform = new LinearTransform();
		pixelbox = new Rectangle();

		setCoordBox(new Rectangle2D.Double());

		prevCoordBox = new Rectangle2D.Double();
		prevCalcCoordBox = new Rectangle2D.Double();
		scratch_coords = new Rectangle2D.Double();
		scratch_coord = new Point2D.Double(0, 0);
		scene_pixelbox = new Rectangle();
	}

	public View(SceneI scene) {
		this();
		if (!(scene instanceof Scene)) {
			throw new IllegalArgumentException("View implementation of ViewI " +
					"requires specific Scene implementation for " +
					"View(SceneI scene) constructor");
		}
		this.scene = (Scene) scene;
	}

	public void destroy() {
		mouse_listeners.clear();
		mouse_motion_listeners.clear();
		key_listeners.clear();
		predraw_viewbox_listeners.clear();
		viewbox_listeners.clear();
		if (bufferImage != null) {
			bufferImage.flush();
		}
		bufferImage = null;
		if (bufferGraphics != null) {
			bufferGraphics.dispose();
		}
		bufferGraphics = null;
		graphics = null;
		component = null;
	}

	public boolean isScrollingOptimized() {
		return scrolling_optimized;
	}

	public void setScrollingOptimized(boolean optimize) {
		if (optimize != scrolling_optimized) {
			firstScrollOptimizedDraw = true;
			scrolling_optimized = optimize;
		}
	}

	public boolean isDamageOptimized() {
		return damage_optimized;
	}

	public void setDamageOptimized(boolean optimize) {
		if (optimize != damage_optimized) {
			firstDamageOptimizedDraw = true;
			damage_optimized = optimize;
		}
	}

	public boolean isBufferOptimized() {
		return buffer_optimized;
	}

	public void setBufferOptimized(boolean optimize) {
		if (optimize != buffer_optimized) {
			firstBufferOptimizedDraw = true;
			buffer_optimized = optimize;
		}
	}

	public boolean isBuffered() {
		return buffered;
	}

	public void setBuffered(boolean b) {
		if (buffered != b) {
			firstBufferOptimizedDraw = true;
			buffered = b;
			if (!buffered) {
				bufferImage = null;
			}
		}
	}

	public void setTransform(LinearTransform transform) {
		this.transform = transform;
	}

	public LinearTransform getTransform() {
		return this.transform;
	}

	/**
	Maps src rectangle in coordinate space to dst rectangle in pixel
	(screen) space.
	Alters AND returns destination Rectangle, to follow 2D API
	transform convention.
	The view is the only object that knows about the mapping from
	coordinate spaces to pixel space, hence
	transformToPixels() and transformToCoords().
	 */
	public Rectangle transformToPixels(Rectangle2D.Double src, Rectangle dst) {
		LinearTransform.transform(transform, src, scratch_coords);

		// Make sure width and height don't overflow integer values!
		// shouldn't be necessary for x and y, since if they overflow (but width and height don't),
		// then nothing should be drawn anyway.
		if (scratch_coords.width > Integer.MAX_VALUE && scratch_coords.x < 0) {
			// normalize the width
			int norm = (int) (scratch_coords.width / Integer.MAX_VALUE);
			norm = Math.min(norm, (int)-(scratch_coords.x / Integer.MAX_VALUE));	// may be an edge case here

			// Bring scratch_coords.width to at most Integer.MAX_VALUE, assuming x is small enough.
			double dNorm = ((double)norm)*Integer.MAX_VALUE;
			scratch_coords.width -= dNorm;
			scratch_coords.x += dNorm;
		}
		if (scratch_coords.height > Integer.MAX_VALUE && scratch_coords.y < 0) {
			// normalize the height
			int norm = (int) (scratch_coords.height / Integer.MAX_VALUE);
			norm = Math.min(norm, (int)-(scratch_coords.y / Integer.MAX_VALUE));
			double dNorm = ((double)norm)*Integer.MAX_VALUE;

			// Bring scratch_coords.height to at most Integer.MAX_VALUE, assuming y is small enough.
			scratch_coords.height -= dNorm;
			scratch_coords.y += dNorm;
		}

		dst.x = (int) scratch_coords.x;
		dst.y = (int) scratch_coords.y;
		dst.width = (int) (scratch_coords.width);
		dst.height = (int) (scratch_coords.height);
		return dst;
	}

	public Point transformToPixels(Point2D.Double src, Point dst) {
		transform.transform(src, scratch_coord);
		dst.x = (int) scratch_coord.x;
		dst.y = (int) scratch_coord.y;
		return dst;
	}

	public boolean clipToPixelBox(Rectangle src, Rectangle dst) {
		// Written to avoid overflows
		int tempDstX = Math.max(src.x, pixelbox.x);
		int tempDstY = Math.max(src.y, pixelbox.y);
		int sx2 = dst.x - tempDstX + dst.width;
		int sy2 = dst.y - tempDstY + dst.height;
		int vx2 = pixelbox.x - tempDstX + pixelbox.width;
		int vy2 = pixelbox.y - tempDstY + pixelbox.height;
		dst.x = tempDstX;
		dst.y = tempDstY;
		dst.width = Math.min(sx2, vx2);
		dst.height = Math.min(sy2, vy2);
		return true;
	}

	/**
	Maps src rectangle in pixel (screen) space to dst rectangle in
	coord space
	alters AND returns destination Rectangle2D, to follow 2D API
	transform convention
	 */
	public Rectangle2D.Double transformToCoords(Rectangle src, Rectangle2D.Double dst) {
		scratch_coords.x = (double) src.x;
		scratch_coords.y = (double) src.y;
		scratch_coords.width = (double) src.width;
		scratch_coords.height = (double) src.height;
		return LinearTransform.inverseTransform(transform, scratch_coords, dst);
	}

	public Point2D transformToCoords(Point2D src, Point2D dst) {
		try {
			return transform.inverseTransform(src, dst);
		} catch (NoninvertibleTransformException ex) {
			Logger.getLogger(View.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		}
	}

	/**
	 *  draw this view
	 */
	public void draw() {
		boolean drawn = false;
		drawCount++;
		component_size = component.getSize();
		component_size_rect =
				new Rectangle(0, 0, component_size.width, component_size.height);

		component_bounds = component.getBounds();

		// calculate pixel bounding box of scene;
		scene_coordbox = scene.getCoordBox();
		scene_pixelbox = transformToPixels(scene_coordbox, scene_pixelbox);

		Graphics2D tempGraphics = null;
		if (isBuffered()) {
			if ((bufferImage == null) ||
					(component_size.width != bufferSize.width) ||
					(component_size.height != bufferSize.height)) {
				setComponent(component);
			}
			bufferGraphics = bufferImage.getGraphics();
			tempGraphics = getGraphics();
			setGraphics((Graphics2D) bufferGraphics);
		} else if (graphics == null) {
			getGraphics();
		}

		calcCoordBox();
		if (!(coordbox.equals(prevCoordBox))) {
			// need to change this to a more general ViewBoxChange event...
			if (predraw_viewbox_listeners.size() > 0) {
				Rectangle2D.Double newbox = new Rectangle2D.Double(coordbox.x, coordbox.y,
						coordbox.width, coordbox.height);
				NeoViewBoxChangeEvent nevt =
						new NeoViewBoxChangeEvent(this, newbox, true);
				for (NeoViewBoxListener listener : predraw_viewbox_listeners) {
					listener.viewBoxChanged(nevt);
				}
			}
		}


		// optimizedScrollDraw checks for damage, will return false and not
		// draw if there is any damage.  optimizedDamageDraw will not be called
		// if optimizedScrollDraw() is called.  Therefore will never call both
		// at the same time.
		if (!drawn && isScrollingOptimized() && lastTransform != null) {
			drawn = optimizedScrollDraw();
		}
		if (!drawn && isDamageOptimized()) {
			drawn = optimizedDamageDraw();
		}
		if (!drawn && isBuffered() && isBufferOptimized()) {
			drawn = optimizedBufferDraw();
		}
		if (!drawn) {
			normalDraw();
		}
		if (isBuffered()) {
			setGraphics(tempGraphics);
			graphics.drawImage(bufferImage, 0, 0, null);

			// should this really be here -- possible this could cause
			//   serious performance hit?
			bufferGraphics.dispose();
		}
		drawTransients();

		lastTransform = (LinearTransform) transform.clone();

		// WARNING!! Calling scene.clearDamage() here means that damage
		//   optimizations will most likely get screwed up when using
		//   multiple views of same scene! -- GAH 12/31/97
		scene.clearDamage();

		calcCoordBox();

		/*
		 *  If view's coordbox has changed (meaning the view has "moved" to
		 *  display a different region of the scene), then post a
		 *  ViewBoxChangeEvent to any listeners
		 */
		if (!(coordbox.equals(prevCoordBox))) {
			// need to change this to a more general ViewBoxChange event...
			if (viewbox_listeners.size() > 0) {
				Rectangle2D.Double newbox = new Rectangle2D.Double(coordbox.x, coordbox.y,
						coordbox.width, coordbox.height);
				NeoViewBoxChangeEvent nevt =
						new NeoViewBoxChangeEvent(this, newbox, false);

				for (NeoViewBoxListener l : viewbox_listeners) {
					l.viewBoxChanged(nevt);
				}
			}
		}

		prevCoordBox.setRect(coordbox.x, coordbox.y,
				coordbox.width, coordbox.height);

	}

	private void drawTransients() {
		/*
		 *  Drawing "transients" directly to screen
		 *  WARNING -- this hasn't been tested on un-buffered views, not sure
		 *      if it will work in such situations
		 */
		if (scene.hasTransients()) {
			for (TransientGlyph transglyph : scene.getTransients()) {
				transglyph.drawTraversal(this);
			}
		}
	}

	public boolean optimizedBufferDraw() {
		if (firstBufferOptimizedDraw) {
			firstBufferOptimizedDraw = false;
			return false;
		}
		if (!transform.equals(lastTransform)) {
			return false;
		}
		// If no damage, can just return true
		//    and draw() method will move old image over from view's image buffer
		if (scene.getDamageCoordBox() == null) {

			return true;
		}

		return false;
	}

	public void normalDraw() {
		transformToCoords(pixelbox, getCoordBox());

		// need to sort out whether background fill is needed or not  GAH 8/11/97
		// needed if NeoCanvas background fill is removed, and it currently is --
		//   GAH 11/30/97

		// clipping rect to eliminate any spillover outside of viewbox

		graphics.setColor(component.getBackground());

		graphics.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
		graphics.setColor(component.getForeground());
		scene.getGlyph().drawTraversal((ViewI) this);
	}

	protected boolean optimizedDamageDraw() {
		// 1. Make sure that:
		//    a. not first draw after turning on damage optimization
		//    b. transforms haven't changed
		if (firstDamageOptimizedDraw) {
			firstDamageOptimizedDraw = false;
			return false;
		}
		if (!transform.equals(lastTransform)) {
			return false;
		}

		// 2. If no damage, can just return true
		//    and draw() method will move old image over from view's image buffer
		if (scene.getDamageCoordBox() == null) {
			return true;
		}

		// 3. create a new view that only includes the damaged area
		//    transformToPixels(damageCoordBox, damagePixelBox);
		transformToPixels(scene.getDamageCoordBox(), damagePixelBox);

		// add one pixel spillover on each side, just to be safe
		// plus another pixel for rounding problems... GAH 12-10-97
		damagePixelBox.x -= 2;
		damagePixelBox.y -= 2;
		damagePixelBox.width += 4;
		damagePixelBox.height += 4;

		// ignore any damage outside of the visible pixelbox
		// if no damage within view, can just return true,
		// and draw() method will move old image over from view's image buffer
		if (!(damagePixelBox.intersects(pixelbox))) {
			return true;
		}

		// only need to deal with visible damage, so find intersection of
		// view's pixelbox and damage's pixelbox
		Rectangle visibleDamagePixelBox = damagePixelBox.intersection(pixelbox);

		// switching in smaller viewbox, etc. in this view for now.  Probably
		// would be cleaner to create a new view...

		Rectangle tempPixelBox = new Rectangle(pixelbox.x, pixelbox.y,
				pixelbox.width, pixelbox.height);
		setPixelBox(visibleDamagePixelBox);
		normalDraw();
		setPixelBox(tempPixelBox);
		return true;
	}

	protected boolean optimizedScrollDraw() {
		// 1. Make sure that:
		//    aa. not first draw after setting scrolling_optimize to true
		//    a. both transforms are linear
		//    b. only one translation has changed (not both X and Y)
		//    c. only the translation has changed (scales are the same)
		//    d. scale is integral (ONLY TEMPORARY, TILL TRANSLATION
		//            CONSTRAINTS ARE WORKED OUT)
		//    e. no damage in Scene since last draw

		//    aa. not first draw after setting scrolling_optimize to true
		if (firstScrollOptimizedDraw) {
			firstScrollOptimizedDraw = false;
			return false;
		}


		//    b. only one translation has changed (not both X and Y)
		boolean xunchanged, yunchanged;
		xunchanged = (transform.getTranslateX() == lastTransform.getTranslateX());
		yunchanged = (transform.getTranslateY() == lastTransform.getTranslateY());
		boolean transformChanged = (!(xunchanged && yunchanged));
		if (!(xunchanged || yunchanged)) {
			return false;
		}

		//    c. only the translation has changed (scales are the same)
		boolean scaleXchange, scaleYchange;
		scaleXchange = !(transform.getScaleX() == lastTransform.getScaleX());
		scaleYchange = !(transform.getScaleY() == lastTransform.getScaleY());
		transformChanged = transformChanged || scaleXchange || scaleYchange;
		if (scaleXchange || scaleYchange) {
			return false;
		}

		//    d. scale is integral (ONLY TEMPORARY, TILL TRANSLATION
		//            CONSTRAINTS ARE WORKED OUT)

		//  GAH 12-2-97
		//  Actually, integral scale only matters if there has been a change --
		//  if _no_ change, flag it and once all conditionals have been met,
		//  if no change should just return true since buffer image should
		//  work as is
		if (transformChanged) {
			double scale;
			// this assumes that check was already done to make sure only one
			// of x or y scroll had changed -- therefore
			// if transformChanged && x-scroll not changed, then
			//          y-scroll must have changed
			if (xunchanged) {
				scale = transform.getScaleY();
			} else {
				scale = transform.getScaleX();
			}
			if (!(((int) scale == scale) || ((int) (1 / scale) == (1 / scale)))) {
				return false;
			}


		}

		//    e. no damage in Scene since last draw
		if (scene.isDamaged()) {
			return false;
		}

		//  if no change should just return true since previous buffered image
		//  should work as is
		if (!transformChanged) {

			return true;
		}
		Double currCoordBox = calculateCurrentCoordbox();

		if (!(currCoordBox.intersects(prevCoordBox))) {
			return false;
		}

		// 3. find intersection of previous coord box and current coord box,
		//    and convert to pixels
		Rectangle2D.Double coordOverlap = (Rectangle2D.Double) coordbox.createIntersection(prevCoordBox);
		Rectangle prevOverlapPixBox = new Rectangle();
		Rectangle currOverlapPixBox = new Rectangle();
		setTransform(lastTransform);
		transformToPixels(coordOverlap, prevOverlapPixBox);
		setTransform(transform);
		transformToPixels(coordOverlap, currOverlapPixBox);

		// 4. copy (bit blt) shared pixels from previous position to
		//    new position
		Graphics g = getGraphics();
		g.copyArea(prevOverlapPixBox.x, prevOverlapPixBox.y,
				prevOverlapPixBox.width, prevOverlapPixBox.height,
				currOverlapPixBox.x - prevOverlapPixBox.x,
				currOverlapPixBox.y - prevOverlapPixBox.y);

		// 5. revise view to be portion of new coordbox that doesn't overlap
		//    old coordbox
		Rectangle2D.Double nolCoordBox = new Rectangle2D.Double();
		// already know that either xcoords are unchanged (c.x=p.x) OR
		//                          ycoords are unchanged (c.y=p.y)
		// see notes on determining non-overlapping rectangle...
		if (yunchanged) {
			nolCoordBox.y = currCoordBox.y;
			nolCoordBox.height = currCoordBox.height;
			if (currCoordBox.x >= prevCoordBox.x) {
				nolCoordBox.x = prevCoordBox.x + prevCoordBox.width;
				nolCoordBox.width = currCoordBox.x + currCoordBox.width -
						nolCoordBox.x;
			} else {
				nolCoordBox.x = currCoordBox.x;
				nolCoordBox.width = prevCoordBox.x - nolCoordBox.x;
			}
		} else if (xunchanged) {
			nolCoordBox.x = currCoordBox.x;
			nolCoordBox.width = currCoordBox.width;
			if (currCoordBox.y >= prevCoordBox.y) {
				nolCoordBox.y = prevCoordBox.y + prevCoordBox.height;
				nolCoordBox.height = currCoordBox.y + currCoordBox.height -
						nolCoordBox.y;
			} else {
				nolCoordBox.y = currCoordBox.y;
				nolCoordBox.height = prevCoordBox.y - nolCoordBox.y;
			}
		} else {
			System.out.println("Error in ScrollOptimizer!!!");
		}

		Rectangle temppixelbox =
				new Rectangle(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

		setCoordBox(nolCoordBox);
		transformToPixels(nolCoordBox, pixelbox);

		//  another attempt to get rid of "doogies"  GAH 2-20-99
		// problem appears to be caused by (int) casting in transformToPixels
		//    (instead of rounding -- though not clear if rounding would totally
		//     solve problem either) -- in particular, the area copy-shifted and
		//     the area actually redrawn are sometimes separated by a single pixel
		//     column/row, which doesn't change at all, leaving bits of previous
		//     draw in that column/row.  And once this has happened, it will be
		//     propogated in subsequent optimized scrolls, since this erroneous
		//     column/row will subsequently be treated as if it was correctly
		//     drawn, and will be bit-blitted just like everything else in
		//     when areas are copy-shifted
		//  Could try to fix this in transformToPixels, or come up with a more
		//     general solution for coord-to-pixel transforms.  However hopefully
		//     a more specific solution for optimized scrolling is to expand the
		//     edges of the area that actually gets redrawn by a pixel in every
		//     direction, thus compensating for any potential double/int "rounding"
		//     problems
		//  Could also try to change optimizedScrollDraw() so that it avoids the
		//     need to convert coords to pixels in the first place, but such a
		//     change would involve much more restructuring and testing.
		//  Therefore trying approach that requires fewest changes to existing
		//     code -- expand edges of area to be redrawn
		pixelbox.x -= 1;
		pixelbox.y -= 1;
		pixelbox.width += 2;
		pixelbox.height += 2;

		setPixelBox(pixelbox);

		// GAH 12-27-2000
		// should probably set the clip box here!
		// this would allow glyphs to not worry about drawing outside the
		//    view messing up when optimizing scrolling
		normalDraw();
		setPixelBox(temppixelbox);
		setCoordBox(currCoordBox);

		return true;
	}
	private Double calculateCurrentCoordbox() {
	// 2.calculate current coordbox, retrieve coordbox from previous paint
	//  could probably avoid this by cloning prevcoordbox from last coordbox,
	//   just like prevtransform... need to make sure cloning works though...
	// also make sure that current coord box and previous coord box
	//   overlap
	transformToCoords(pixelbox, coordbox);
	Rectangle2D.Double currCoordBox = coordbox;
	setTransform(lastTransform);
	transformToCoords(pixelbox, prevCalcCoordBox);
	prevCoordBox = prevCalcCoordBox;
	setTransform(transform);
	return currCoordBox;
	}
	public void setPixelBox(Rectangle pixelbox) {
		this.pixelbox = pixelbox;
	}

	public Rectangle getPixelBox() {
		return pixelbox;
	}

	public void setCoordBox(Rectangle2D.Double coordbox) {
		this.coordbox = coordbox;
	}

	public Rectangle2D.Double getCoordBox() {
		return this.coordbox;
	}

	public void setFullView(ViewI full_view) {
		this.full_view = full_view;
	}

	public ViewI getFullView() {
		return full_view;
	}

	public void setGraphics(Graphics2D g) {
		graphics = g;
	}

	public Graphics2D getGraphics() {
		if (graphics == null && component != null) {
			// Not sure if this is a good idea -- forcing this wreaks havoc
			//   on the updates... -- Gregg
			setGraphics((Graphics2D) component.getGraphics());
		}
		return graphics;
	}

	public void setComponent(NeoCanvas c) {
		component_size = c.getSize();
		if ((component == c) && (bufferImage != null)
				&& (bufferSize.width == component_size.width)
				&& (bufferSize.height == component_size.height)) {
			return;
		}

		bufferSize = component_size;
		component = c;
		firstBufferOptimizedDraw = true;
		if (bufferSize.width <= 0 || bufferSize.height <= 0) {
			return;
		}

		// GAH 12-3-98
		//  trying to minimize damage caused by JVM image memory leak
		//  see Java Developer's Connection, Bug ID = 4014323
		if (bufferImage != null) {
			bufferImage.flush();
		}
		if (bufferGraphics != null) {
			bufferGraphics.dispose();
		}

		bufferImage = component.createImage(bufferSize.width, bufferSize.height);
	}

	public NeoCanvas getComponent() {
		return component;
	}

	public SceneI getScene() {
		return scene;
	}

	public Rectangle2D.Double calcCoordBox() {
		return transformToCoords(pixelbox, coordbox);
	}

	//  Standard methods to implement the event source for event listeners
	public void addMouseListener(MouseListener l) {
		mouse_listeners.add(l);
	}

	public void removeMouseListener(MouseListener l) {
		mouse_listeners.remove(l);
	}

	public void addMouseMotionListener(MouseMotionListener l) {
		mouse_motion_listeners.add(l);
	}

	public void removeMouseMotionListener(MouseMotionListener l) {
		mouse_motion_listeners.remove(l);
	}

	public void addKeyListener(KeyListener l) {
		key_listeners.add(l);
	}

	public void removeKeyListener(KeyListener l) {
		key_listeners.remove(l);
	}

	public void addPostDrawViewListener(NeoViewBoxListener l) {
		viewbox_listeners.add(l);
	}

	public void removePostDrawViewListener(NeoViewBoxListener l) {
		viewbox_listeners.remove(l);
	}

	public void addPreDrawViewListener(NeoViewBoxListener l) {
		predraw_viewbox_listeners.add(l);
	}

	public void removePreDrawViewListener(NeoViewBoxListener l) {
		predraw_viewbox_listeners.remove(l);
	}

	public Dimension getComponentSize() {
		return component_size;
	}

	/**
	 * gets the size of the component.
	 *
	 * @return a Rectangle the same size as the bounds of the component
	 *         with an origin of (0, 0).
	 */
	public Rectangle getComponentSizeRect() {
		return component_size_rect;
	}

	/** implementing MouseListener interface and collecting mouse events */
	public void mouseClicked(MouseEvent e) {
		heardMouseEvent(e);
	}

	public void mouseEntered(MouseEvent e) {
		heardMouseEvent(e);
	}

	public void mouseExited(MouseEvent e) {
		heardMouseEvent(e);
	}

	public void mousePressed(MouseEvent e) {
		heardMouseEvent(e);
	}

	public void mouseReleased(MouseEvent e) {
		heardMouseEvent(e);
	}

	/** implementing MouseMotionListener interface and collecting mouse events */
	public void mouseDragged(MouseEvent e) {
		heardMouseEvent(e);
	}

	public void mouseMoved(MouseEvent e) {
		heardMouseEvent(e);
	}

	/** implementing KeyListener interface and collecting key events */
	public void keyPressed(KeyEvent e) {
		heardKeyEvent(e);
	}

	public void keyReleased(KeyEvent e) {
		heardKeyEvent(e);
	}

	public void keyTyped(KeyEvent e) {
		heardKeyEvent(e);
	}

	/** implementing NeoPaintListener interface and triggering draw */
	public void componentPainted(NeoPaintEvent evt) {
		setGraphics(evt.getGraphics());
		draw();
	}

	/** processing key events on view's component */
	public void heardKeyEvent(KeyEvent e) {
		if (e.getSource() != component) {
			return;
		}
		int id = e.getID();
		for (KeyListener kl : key_listeners) {
			if (id == KeyEvent.KEY_PRESSED) {
				kl.keyPressed(e);
			} else if (id == KeyEvent.KEY_RELEASED) {
				kl.keyReleased(e);
			} else if (id == KeyEvent.KEY_TYPED) {
				kl.keyTyped(e);
			}
		}
	}

	/** processing mouse events on view's component */
	public void heardMouseEvent(MouseEvent e) {
		if (e.getSource() != component) {
			return;
		}
		int id = e.getID();
		int x = e.getX();
		int y = e.getY();
		Rectangle pixrect = new Rectangle(x, y, 0, 0);
		Rectangle2D.Double coordrect = new Rectangle2D.Double();

		// Check for intersection with view's pixelbox
		// if (pixrect.intersects(getPixelBox())) {
		// commented out to allow events to be heard even if they are outside
		// the views pixelbox, for example to deal with mouse drags that
		// continue beyond the window

		if (transform == null) {
			return;
		}
		transformToCoords(pixrect, coordrect);
		NeoViewMouseEvent nevt =
				new NeoViewMouseEvent(e, this, coordrect.x, coordrect.y);

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

	/**
	 * If View is managing its own offscreen double buffering, will
	 * return this image, otherwise will return null.
	 */
	public Image getBufferedImage() {
		if (this.isBuffered()) {
			return bufferImage;
		} else {
			return null;
		}
	}

	public Rectangle getScratchPixBox() {
		return scratch_pixelbox;
	}


}
