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

package com.affymetrix.genoviz.event;

import com.affymetrix.genoviz.bioviews.GlyphI;
import java.util.EventObject;


/**
 *  This does NOT have any relation to NeoDragEvent
 *  Apologies for the confusing similarity of names
 *
 *  NeoGlyphDragEvent is meant to keep NeoGlyphDragListeners posted
 *     about glyphs being dragged around a NeoWidget via a GlyphDragger
 *
 *  whereas NeoDragEvent is meant to keep NeoDragListeners posted about
 *    mouse drag gestures that start in a NeoWidget and extend beyond
 *    its borders, usually implying the NeoWidget should be "scrolled"
 *    in some manner
 *
 *  WARNING -- if this class is changed to inherit from AWTEvent, need to
 *     take out id field, else will shadow AWTEvent id field...
 */
public class NeoGlyphDragEvent extends EventObject {
	GlyphI dragged_glyph;
	public final static int DRAG_STARTED = 0;
	public final static int DRAG_IN_PROGRESS = 1;
	public final static int DRAG_ENDED = 2;
	//  Rectangle2D.Double prev_glyph_coords = new Rectangle2D();
	// EventObject original_event;

	/**
	 *  the id of drag event, one of:
	 *    DRAG_STARTED
	 *    DRAG_IN_PROGRESS
	 *    DRAG_ENDED
	 */
	int id;

	/**
	 * Create one.
	 * @param source is the component (usually a NeoMap) that the drag started on.
	 * @param glyph is the glyph being dragged.
	 *
	 * <p> Three types of NeoGlyphDragEvents:<ol>
	 * <li> DRAG_STARTED
	 * <li> DRAG_IN_PROGRESS
	 * <li> DRAG_ENDED
	 * </ol></p>
	 *
	 * <p> All types of GlyphDragEvents are meant to be thrown
	 * <em>after</em> the drag has occurred,
	 * (meaning the glyphs coords have changed to reflect the drag event)
	 * but <em>before</em> the display has been redrawn
	 * to reflect the changed glyph coords.
	 *
	 * <p> So to get new location of glyph, just ask for its coords.
	 * Currently old location of glyph is not available...
	 *
	 * <p> Also, if dragging a copy,
	 * the call to GlyphDragListeners will occur <em>before</em> the copy is removed.
	 *
	 * <p> [ If you want to drag multiple glyphs, throw them into some sort of glyph
	 *    that stretches around its children (like StretchContainerGlyph?) ]
	 */
	public NeoGlyphDragEvent(Object source, int id, GlyphI glyph) {
		super(source);
		this.id = id;
		dragged_glyph = glyph;
	}

	public int getID() {
		return id;
	}

	public GlyphI getGlyph() {
		return dragged_glyph;
	}

	/**
	 * Experimenting with recycling events to improve efficiency.
	 */
	public void recycle(Object source, int id, GlyphI glyph)  {
		this.source = source;
		this.id = id;
		dragged_glyph = glyph;
	}
}
