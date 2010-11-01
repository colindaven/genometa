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

import java.util.EventObject;
import java.awt.geom.Rectangle2D;

/**
 * A view was redrawn and its visible bounds have changed.
 * This event is generated by the GenoViz structured graphics architecture.
 * It is used internally by genoviz widgets to generate {@link NeoRangeEvent}s.
 * NeoViewBoxChangeEvents are only generated
 * when a view is redrawn and the visible bounds have changed since the last draw.
 */
public class NeoViewBoxChangeEvent extends EventObject  {

	public final static int ADJUSTMENT = 30000;

	protected Rectangle2D.Double currentCoordBox;
	protected int id;
	boolean predraw;

	/**
	 * Constructs an event with the specified arguments
	 * @param source the Neo widget that generated the event
	 * @param currentCoordBox the bounding box of the new visible coordinate
	 *    area of the widget
	 */
	public NeoViewBoxChangeEvent(Object source, Rectangle2D.Double currentCoordBox) {
		this(source, currentCoordBox, false);
	}

	public NeoViewBoxChangeEvent(Object source, Rectangle2D.Double currentCoordBox,
			boolean predraw) {
		super(source);
		id = ADJUSTMENT;
		this.currentCoordBox = currentCoordBox;
		this.predraw = predraw;
	}

	/**
	 * @return the bounding box of the new visible coordinate area.
	 */
	public Rectangle2D.Double getCoordBox() {
		return currentCoordBox;
	}

	/**
	 * #return the id of the event (currently only ADJUSTMENT is supported).
	 */
	public int getID() { return id; }

	public boolean isPreDraw() {
		return predraw;
	}

}