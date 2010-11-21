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
 *  Event generated at specified intervals by NeoTimerEventClock in its
 *  own thread.
 *  Propogated to event listeners via JDK1.0-compatible delegation-based
 *  event handling (pseudo-1.1 event handling).
 */
public class NeoTimerEvent extends EventObject {

	protected int tick_count;
	protected Object arg;

	public NeoTimerEvent(Object source, Object arg, int tick_count) {
		super(source);
		this.arg = arg;
		this.tick_count = tick_count;
	}

	public int getTickCount() {
		return tick_count;
	}

	/**
	 *  An arbitrary object passed in when NeoTimerEvent was constructed.
	 */
	public Object getArg() {
		return arg;
	}

}
