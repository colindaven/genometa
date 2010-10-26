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

import java.awt.*;
import java.awt.event.PaintEvent;

public class NeoPaintEvent extends PaintEvent {

	Graphics2D g;

	/** event id is always PAINT */
	public NeoPaintEvent(Component source, Rectangle updateRect, Graphics2D g) {
		super(source, PaintEvent.PAINT, updateRect);
		this.g = g;
	}

	public Graphics2D getGraphics() {
		return g;
	}

}
