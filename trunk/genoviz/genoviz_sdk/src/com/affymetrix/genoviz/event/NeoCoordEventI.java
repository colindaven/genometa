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
import java.awt.geom.Point2D;
import java.util.EventObject;
import java.util.List;

/**
 * An interface implemented by some events to incorporate widget info
 * into the event. This includes widget coordinates, the location within the widget,
 * and the glyphs positioned under the event.
 */
public interface NeoCoordEventI {

	/**
	 * get the x coordinate of the event, in
	 * widget coordinate units (_not_ pixels).
	 */
	public double getCoordX();

	/**
	 * get the y coordinate of the event, in
	 * widget coordinate units (_not_ pixels).
	 */
	public double getCoordY();

	/**
	 * get the coordinates of the event as a Point2D, in
	 * widget coordinate units (_not_ pixels).
	 */
	public Point2D.Double getPoint2D();

	/**
	 * get the original event that this NeoCoordEventI is based
	 * on (usually a standard AWTEvent).
	 */
	public EventObject getOriginalEvent();

	/**
	 * @return the event type
	 */
	public int getID();

	/**
	 * if the widget has internal structure, returns the internal
	 * widget location of the event.
	 */
	public int getLocation();

	/**
	 * @return a Vector of GlyphIs whose coord bounds contain the
	 * coord location of the event.
	 */
	public List<GlyphI> getItems();

}
