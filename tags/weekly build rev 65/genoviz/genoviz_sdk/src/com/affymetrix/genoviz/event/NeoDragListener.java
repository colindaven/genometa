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

import java.util.EventListener;

/**
 * Implementers can monitor NeoDragEvents.
 * Classes that need to listen for NeoDragEvents
 * must implement this interface and register with the
 * appropriate DragMonitor.
 *
 * @see com.affymetrix.genoviz.bioviews.DragMonitor
 */
public interface NeoDragListener extends EventListener {
	public void heardDragEvent(NeoDragEvent evt);
}
