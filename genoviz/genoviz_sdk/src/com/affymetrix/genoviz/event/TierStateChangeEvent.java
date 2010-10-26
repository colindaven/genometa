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

import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;

import java.util.EventObject;

public class TierStateChangeEvent extends EventObject {

	protected int state;
	protected MapTierGlyph source;

	/**
	 * Make a new tier state change event for the given parameters.
	 */
	public TierStateChangeEvent(MapTierGlyph src, int state) {
		super(src);

		source = src;
		this.state = state;
	}

	/**
	 * Get the state for which this event exists.
	 */
	public int getState() {
		return state;
	}
}
