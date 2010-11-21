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

import com.affymetrix.genoviz.util.NeoConstants;
import java.util.EventObject;

public class NeoDragEvent extends EventObject {

	protected int direction;

	/**
	 * @param source - thing being dragged.
	 * @param direction -
	 * {@link NeoConstants#NORTH},
	 * {@link NeoConstants#SOUTH},
	 * {@link NeoConstants#EAST},
	 * {@link NeoConstants#WEST}, or
	 * {@link NeoConstants#NONE}.
	 * (Probably shouldn't see NONE.
	 * But DragMonitor falls back on it
	 * if it can't figure out anything else.)
	 */
	public NeoDragEvent(Object source, int direction) {
		super(source);
		switch ( direction ) {
			case NeoConstants.NORTH:
			case NeoConstants.SOUTH:
			case NeoConstants.EAST:
			case NeoConstants.WEST:
			case NeoConstants.NONE:
				this.direction = direction;
				break;
			default:
				throw new IllegalArgumentException(
						"Direction must be NORTH, SOUTH, EAST, WEST, or NONE." );
		}
	}

	public int getDirection() {
		return direction;
	}

}
