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
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.EventObject;
import java.util.List;

import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import java.awt.geom.Point2D;

/**
 * A mouse event occurring over a NeoWidget.
 * (Actually anything implementing {@link NeoAbstractWidget}.)
 * NeoMouseEvent provides methods for querying
 * the widget coordinates and widget location of a mouse event.
 * Also for retrieving glyphs under the event's coordinates.
 */
public class NeoMouseEvent extends MouseEvent implements NeoCoordEventI {

	protected static final int UNKNOWN = com.affymetrix.genoviz.util.NeoConstants.UNKNOWN;
	protected EventObject original_event;
	protected double xcoord;
	protected double ycoord;
	protected int location = UNKNOWN;
	protected List<GlyphI> cached_items = null;

	/**
	 * Constructs an event with the specified original event, source component,
	 * widget location, and widget x/y coords.
	 * @param ome the original MouseEvent that this NeoMouseEvent is based on
	 * @param source the Neo widget that generated the event
	 * @param location id of the widget location the event occurred over, if
	 *        the widget has internal structure
	 */
	public NeoMouseEvent(MouseEvent ome, Component source, int location,
			double xcoord, double ycoord) {
		this(ome, source, xcoord, ycoord);
		this.location = location;
	}

	/**
	 * Constructs an event with the specified original event, source component,
	 * widget x/y coords, and unknown location.
	 * @param ome the original MouseEvent that this NeoMouseEvent is based on
	 * @param source the Neo widget that generated the event
	 */
	public NeoMouseEvent(MouseEvent ome, Component source,
			double xcoord, double ycoord) {
		super(source, ome.getID(), ome.getWhen(),
				ome.getModifiers(), ome.getX(), ome.getY(),
				ome.getClickCount(), ome.isPopupTrigger());
		this.original_event = ome;
		this.xcoord = xcoord;
		this.ycoord = ycoord;
	}

	/**
	 * points to in which part of a NeoContainerWidget the event occurred.
	 * @return the internal widget location of the event or UNKNOWN
	 * if the widget has no internal structure.
	 */
	public int getLocation() {
		return location;
	}

	/**
	 * @return the x coordinate of the event
	 * in widget coordinate units (<em>not</em> pixels).
	 */
	public double getCoordX() {
		return xcoord;
	}

	/**
	 * @return the y coordinate of the event
	 * in widget coordinate units (<em>not</em> pixels).
	 */
	public double getCoordY() {
		return ycoord;
	}

	/**
	 * @return the coordinates of the event as a Point2D
	 * in widget coordinate units (<em>not</em> pixels).
	 */
	public Point2D.Double getPoint2D() {
		return new Point2D.Double(getCoordX(), getCoordY());
	}

	/**
	 * @return the original event on which this NeoCoordEventI is based
	 * (usually a standard AWTEvent).
	 */
	public EventObject getOriginalEvent() {
		return original_event;
	}

	/**
	 * @return a List of GlyphIs
	 * whose coord bounds contain the coord location of the event.
	 */
	public List<GlyphI> getItems() {
		if (cached_items == null) {
			Object src = getSource();
			if (! (src instanceof NeoAbstractWidget)) { return null; }
			cached_items = ((NeoAbstractWidget)src).getItems(getCoordX(), getCoordY(), getLocation());
		}
		return cached_items;
	}

	/**
	 * for debugging.
	 */
	@Override
	public String toString() {
		String s = "NeoMouseEvent: at ( " + xcoord + ", " + ycoord + " )";
		if ( UNKNOWN != this.location ) {
			s += " in location " + this.location;
		}
		s += " originally: " + original_event.toString();

		return s;
	}

}
