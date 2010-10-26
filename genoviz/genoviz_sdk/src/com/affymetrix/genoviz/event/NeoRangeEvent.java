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


/**
 *  Event generated when visible span of a Neo* widget primary axis changes.
 *  Propogated to event listeners via JDK1.0-compatible delegation-based
 *  event handling
 *  (pseudo-1.1 event handling)
 *
 *  Not currently implemented for NeoAssembler.
 */
public class NeoRangeEvent extends EventObject  {

	protected double span_start;
	protected double span_end;

	/**
	 * Constructs an event with the specified target component,
	 * event type, and argument.
	 * @param source the Neo widget that generated the event.
	 */
	public NeoRangeEvent(Object source, double span_start, double span_end) {
		super(source);
		this.span_start = span_start;
		this.span_end = span_end;
	}

	public double getVisibleStart() {
		return span_start;
	}

	public double getVisibleEnd() {
		return span_end;
	}

	public String toString() {
		return "NeoRangeEvent: " + span_start + " " + span_end;
	}

}
