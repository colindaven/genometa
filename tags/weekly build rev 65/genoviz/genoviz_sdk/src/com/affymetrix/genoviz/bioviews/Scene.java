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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.*;

import com.affymetrix.genoviz.glyph.RootGlyph;
import com.affymetrix.genoviz.glyph.TransientGlyph;


/**
 * implementation of SceneI interface.
 * See SceneI for better documentation of methods.
 */
public class Scene implements SceneI  {
	private static final boolean DEBUG = false;

	protected GlyphI eveGlyph;
	protected List<ViewI> views;
	protected Color select_color;
	protected int select_style;

	/**
	 * damage flag to indicate if something has changed in the scene that
	 * should force a full draw on the next Widget draw call
	 */
	protected boolean damaged = false;
	protected Rectangle2D.Double damageCoordBox;
	private final Rectangle2D scratchCoordBox;

	/**
	 * Vector of transient glyphs that are "layered" on top of views
	 * after all other glyphs have been drawn.
	 */
	protected List<TransientGlyph> transients;

	protected List<NeoDataAdapterI> adapters;

	public Scene ()  {
		/*
		 * eveGlyph is a RootGlyph glyph rather than a base glyph.
		 * This should allow the scene to respond better to dynamic
		 * addition of data.  This means the only place the bounding coords of the
		 * scene are stored is in the eveGlyph, so Scene.coordbox has been
		 * removed, and all use of coordbox has been replaced by calls to
		 * eveGlyph.getCoordBox() -- GAH 12/6/97
		 */
		eveGlyph = new RootGlyph();
		eveGlyph.setScene(this);
		eveGlyph.setCoords(0,0,1,1);
		views = new ArrayList<ViewI>();
		select_color = Color.red;
		select_style = SELECT_FILL;
		scratchCoordBox = new Rectangle2D.Double();
	}

	/**
	 * Sets the coordinate bounds for the Scene.
	 * Glyphs outside of these bounds will not be visible.
	 * @param x X coordinate for top left
	 * @param y Y coordinate for top right
	 * @param w Width of coordinate box
	 * @param h Height of coordinate box
	 */
	public void setCoords(double x, double y, double w, double h) {
		eveGlyph.setCoords(x,y,w,h);
		maxDamage();
	}

	/**
	 * Sets the root glyph of this scene.
	 * Use addGlyph() to add another glyph to the scene.
	 * @param glyph the new RootGlyph
	 */
	public void setGlyph(GlyphI glyph) {
		if (glyph == null) {
			eveGlyph = new RootGlyph();
			return;
		}
		eveGlyph = glyph;
		eveGlyph.setScene(this);
		maxDamage();
	}

	/**
	 * Returns the root glyph of this scene.
	 */
	public GlyphI getGlyph()  {
		return eveGlyph;
	}

	/**
	 * Adds another glyph to the scene.
	 */
	public void addGlyph(GlyphI glyph) {
		if (glyph != null) {
			getGlyph().addChild(glyph);

			// transients should be added both to the root glyph (for pick
			//     traversal, etc.) and to the transients list (for actual drawing),
			//     but shouldn't be considered in damage expansion...
			if (glyph instanceof TransientGlyph) {
				addTransient((TransientGlyph)glyph);
			}
			else {
				expandDamage(glyph);
			}
		}
	}

	/**
	 * adds a glyph.
	 * @param glyph to add.
	 * @param i where to add it.
	 */
	public void addGlyph(GlyphI glyph, int i) {
		if (glyph != null) {
			getGlyph().addChild(glyph,i);
			expandDamage(glyph);
		}
	}

	/**
	 * Adds a view representing this scene.
	 */
	public void addView(ViewI view)  {
		views.add(view);
	}

	/**
	 * Removes a view that had been representing the scene.
	 */
	public void removeView(ViewI view)  {
		views.remove(view);
	}

	/**
	 * Returns a vector of the views that are currently representing the scene.
	 */
	public List<ViewI> getViews()  {
		return views;
	}


	/**
	 * Draws all views on all canvases.
	 */
	public void draw()  {
		for (ViewI view : views) {
			view.draw();
		}
		clearDamage();
	}

	// NOT USED ANYMORE?  11-17-97
	/**
	 * Draws all the glyphs in one view.
	 */
	public void draw(ViewI v) {
		v.draw();
	}

	/**
	 * Draw one canvas.
	 */
	public void draw(Component c, Graphics2D g)  {
		for (ViewI view : views) {
			if (view.getComponent() == c)  {
				view.setGraphics (g);
				view.draw();
			}
		}
		// This will cause problems when trying to do damage control across
		// views on multiple canvases!!!  11-17-97
		// should probably switch to a long damage_counter for both View
		// and Scene, so View can compare to its own counter and decide based
		// on that what to do, and Scene can in turn check View counters to
		// decide when it can zero out the damage again
		clearDamage();
	}

	public Rectangle2D.Double getCoordBox() {
		return eveGlyph.getCoordBox();
	}

	// should really move data adapters out to widgets...
	public void addDataAdapter(NeoDataAdapterI adapter) {
		if (adapters == null) {
			adapters = new ArrayList<NeoDataAdapterI>();
		}
		adapters.add(adapter);
		adapter.setScene(this);
	}

	// should really move data adapters out to widgets...
	public void removeDataAdapter(NeoDataAdapterI adapter) {
		if (adapters == null)  return;
		adapters.remove(adapter);
	}

	// should really move data adapters out to widgets...
	public GlyphI addData(Object obj) {
		if (adapters == null) {
			return null;
		}
		NeoDataAdapterI da;
		GlyphI glyph;
		for (int i=0; i<adapters.size(); i++) {
			da = adapters.get(i);
			if (da.accepts(obj)) {
				glyph = da.createGlyph(obj);
				if (glyph != null) {
					addGlyph(glyph);
				}
				return glyph;
			}
		}
		return null;
	}

	public void pickTraversal(Rectangle2D.Double coordrect, List<GlyphI> pickvect,
			ViewI view) {
		eveGlyph.pickTraversal(coordrect, pickvect, view);
	}

	/**
	 * Sets visibility for a particular glyph in the scene.
	 * @param glyph the glyph to set.
	 * @param isVisible whether or not the glyph is visible.
	 */
	public void setVisibility(GlyphI glyph, boolean isVisible) {
		glyph.setVisibility(isVisible);
		expandDamage(glyph);
	}

	/*
	 * Alternatively, damage expansion on selection/removal/visibility
	 * changes should be handled by the glyphs themselves -- if go that
	 * route, should add a Scene field to base glyph
	 */

	/**
	 * Selects a glyph.
	 * @param gl The glyph to select
	 */
	public void select(GlyphI gl) {
		if (gl != null) {
			gl.setSelected(true);
			expandDamage(gl);
		}
	}

	// Still need to deal with Y!!!!  GAH 12-10-97
	public void select(Object obj, double x, double y,
			double width, double height) {
		if (obj == null || !(obj instanceof GlyphI))  {
			// **** throw exception here??? ****
			return;
		}

		GlyphI gl = (GlyphI)obj;
		if (!gl.supportsSubSelection()) {
			select(gl);
			return;
		}

		if (DEBUG) {
			System.out.println("supports sub selection, expanding damage: " +
					x + ", " + y + ", " + width + ", " + height);
		}
		Rectangle2D.Double prev_selbox = gl.getSelectedRegion();
		if (prev_selbox == null)  {
			if (DEBUG) {
				System.out.println("in Scene.select(), prev_selbox is null");
			}
			gl.select(x, y, width, height);
			expandDamage(gl, x, y, width, height);
		}
		else {
			scratchCoordBox.setRect(prev_selbox.x, prev_selbox.y,
					prev_selbox.width, prev_selbox.height);
			if (DEBUG) {
				System.out.println("in Scene.select(), prev_selbox NOT null, " +
						"calling select(x,y,w,h) on " + obj);
			}
			gl.select(x, y, width, height);
			Rectangle2D.Double curr_selbox = gl.getSelectedRegion();
			Rectangle2D union_selbox =
				curr_selbox.createUnion(scratchCoordBox);
			Rectangle2D common_selbox =
				curr_selbox.createIntersection(scratchCoordBox);
			Rectangle2D.Double damage_selbox =
				new Rectangle2D.Double(union_selbox.getX(), union_selbox.getY(),
						union_selbox.getWidth(), union_selbox.getHeight());

			if (DEBUG) {
				System.out.println("PrevBox:   " + prev_selbox);
				System.out.println("CurrBox:   " + curr_selbox);
				System.out.println("UnionBox:  " + union_selbox);
				System.out.println("InterBox:  " + common_selbox);
			}

			// +1/-1 adjustments made to draw over previous selection edge

			if (union_selbox.getY() == common_selbox.getY() &&
					union_selbox.getHeight() == common_selbox.getHeight()) {

				// both x-start and x-end are the same,
				// therefore prev and current coord boxes are identical
				// therefore don't need to expand damage at all?
				if (union_selbox.getX() == common_selbox.getX() &&
						union_selbox.getWidth() == common_selbox.getWidth()) {
					if (DEBUG) {
						System.out.println("***** selection not changed, no damage *****");
					}
					return;
						}

				// x-start of selection hasn't moved
				else if (union_selbox.getX() == common_selbox.getX()) {
					if (DEBUG) {
						System.out.println("***** x-start of selection hasn't moved ****");
					}
					damage_selbox.x = common_selbox.getX() + common_selbox.getWidth();
					damage_selbox.width =
						union_selbox.getWidth() - common_selbox.getWidth();
				}

				// x-end of selection hasn't moved
				else if ((union_selbox.getX() + union_selbox.getWidth()) ==
						(common_selbox.getX() + common_selbox.getWidth())) {
					damage_selbox.x = union_selbox.getX();
					damage_selbox.width = common_selbox.getX() - union_selbox.getX();
					if (DEBUG) {
						System.out.println("***** x-end of selection hasn't moved ****");
						System.out.println("Union:  " + union_selbox);
						System.out.println("Inter:  " + common_selbox);
						System.out.println("Damage: " + damage_selbox);
					}

						}
					}
			if (DEBUG) {
				System.out.println("DamageBox: " + damage_selbox);
			}
			expandDamage(gl, damage_selbox.x, damage_selbox.y,
					damage_selbox.width, damage_selbox.height);
		}

	}

	/**
	 * Deselects a glyph.
	 * @param gl the glyph to be deselected.
	 */
	public void deselect(GlyphI gl) {
		if (gl != null) {
			gl.setSelected(false);
			expandDamage(gl);
		}
	}

	/**
	 * Removes a glyph from the scene.
	 * @param gl the glyph to be removed.
	 */
	public void removeGlyph(GlyphI gl) {
		expandDamage(gl);
		// special case: if gl is the top-level glyph, set to null
		if (gl == this.getGlyph()) {
			setGlyph(null);
		}
		// otherwise remove reference to gl in parent's children
		else {
			GlyphI parent = gl.getParent();
			parent.removeChild(gl);
		}
		if (gl instanceof TransientGlyph) {
			removeTransient((TransientGlyph)gl);
		}
	}

	public int getSelectionStyle() {
		return getSelectionAppearance();
	}

	/**
	 * Sets the selection appearance.
	 * @param id the appearance state.
	 * Choose from SELECT_NONE, SELECT_OUTLINE,
	 * SELECT_FILL, BACKGROUND_FILL, SELECT_REVERSE, HIGHLIGHT.
	 */
	public void setSelectionAppearance(int id) {
		select_style = id;
	}

	/**
	 * @return the selection appearance set with {@link #setSelectionAppearance(int)}.
	 */
	public int getSelectionAppearance() {
		return select_style;
	}

	public void setSelectionColor(Color col) {
		select_color = col;
	}

	public Color getSelectionColor() {
		return select_color;
	}

	public void maxDamage() {
		expandDamage(eveGlyph);
	}

	/**
	 * Expands damaged area to include glyph's coordbox.
	 * Should this be protected???
	 */
	public void expandDamage(GlyphI glyph) {
		if (glyph == null) {
			return;
		}
		damaged = true;
		Rectangle2D.Double gcoords = glyph.getCoordBox();

		if (damageCoordBox == null) {
			damageCoordBox = new Rectangle2D.Double();
			damageCoordBox.setRect(gcoords);
		}
		else {
			damageCoordBox.add(gcoords);
		}
	}

	/*
	 * Just a pass-through to expandDamage(x, y, width, height) for now,
	 * but may want to use glyph-specific info in later versions, for example
	 * to optimize for a selection that is expanding/contracting
	 *
	 * Alternatively, damage expansion on selection should be handled by
	 * the glyphs themselves -- if go that route, should add a Scene field
	 * to base glyph
	 */
	public void expandDamage(GlyphI glyph, double x, double y,
			double width, double height) {
		expandDamage(x, y, width, height);
	}

	public void expandDamage(double x, double y, double width, double height) {
		damaged = true;
		if (width < 0) {
			x = x + width;
			width = -width;
		}
		if (height < 0) {
			y = y + height;
			height = -height;
		}

		if (damageCoordBox == null) {
			damageCoordBox = new Rectangle2D.Double(x, y, width, height);
		}
		else {
			damageCoordBox.add(x, y);
			damageCoordBox.add(x+width, y+height);
		}
	}

	public void clearDamage() {
		damaged = false;
		damageCoordBox = null;
	}

	public boolean isDamaged() {
		return damaged;
	}

	public Rectangle2D.Double getDamageCoordBox() {
		return damageCoordBox;
	}

	protected boolean hasTransients() {
		return (transients != null && transients.size() > 0);
	}

	protected void addTransient(TransientGlyph tg) {
		if (transients == null) {
			transients = new ArrayList<TransientGlyph>();
		}
		transients.add(tg);
	}

	protected void removeTransient(TransientGlyph tg) {
		if (transients == null) { return; }
		transients.remove(tg);
	}

	/**
	 * Clears out the list of transient glyphs.
	 * This allows for a more complete clearing of a NeoMap.
	 * @see com.affymetrix.genoviz.widget.NeoMap#clearWidget()
	 */
	public void removeAllTransients() {
		if ( null != transients ) {
			transients.clear();
		}
	}

	protected List<TransientGlyph> getTransients() {
		return transients;
	}

	/**
	 * Make glyph gl be drawn behind all its sibling glyphs.
	 */
	public static void toBackOfSiblings(GlyphI gl) {
		GlyphI parent = gl.getParent();
		if (parent != null) {
			parent.removeChild(gl);
			parent.addChild(gl, 0);
		}
	}

	/**
	 * Make this glyph be drawn in front of all its sibling glyphs.
	 * (with the exception that it will not be drawn in front of transient glyphs)
	 */
	public static void toFrontOfSiblings(GlyphI gl) {
		GlyphI parent = gl.getParent();
		if (parent != null) {
			parent.removeChild(gl);
			parent.addChild(gl);
		}
	}

	/**
	 * Make glyph gl be drawn behind all other glyphs.
	 * (before all other glyphs)
	 */
	public static void toBack(GlyphI gl) {
		GlyphI child = gl;
		GlyphI parent = gl.getParent();
		while (parent != null) {
			toBackOfSiblings(child);
			child = parent;
			parent = child.getParent();
		}
	}

	/**
	 * Make this glyph be drawn in front.
	 * (after all other glyphs)
	 * Except, will not be drawn in front of transient glyphs.
	 */
	public static void toFront(GlyphI gl) {
		GlyphI child = gl;
		GlyphI parent = child.getParent();
		while (parent != null) {  // maybe also check for parent != child ???
			toFrontOfSiblings(child);
			child = parent;
			parent = child.getParent();
		}
	}

}
