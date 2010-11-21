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

package com.affymetrix.genoviz.awt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.LayoutManager;
import java.awt.Panel;

/**
 * A heavyweight panel
 * that overrides update() and paint()
 * to avoid flicker.
 * It ensures that no background is drawn
 * (except occasionally in peers in some JVMs).
 * Lightweight components can thus be placed in a NeoBlankPanel
 * to prevent flickering when lightweights are painting.
 * This is needed because lightweight painting forces painting
 * of heavyweight (containment) ancestors.
 */
public class NeoPanel extends Panel {

	private boolean needs_background_paint = false;

	public NeoPanel() {
		super();
	}

	public NeoPanel(LayoutManager lm) {
		super(lm);
	}

	/**
	 * creates a NeoPanel with a new BorderLayout
	 * and adds the given Component to the "Center".
	 */
	public NeoPanel(Component comp) {
		super(new BorderLayout());
		this.add(comp, BorderLayout.CENTER);
	}

	/**
	 * updates without clearing.
	 */
	public void update(Graphics g) {
		paint(g);
	}

	/**
	 * same as super.paint, but with background repainted if requested.
	 * Note that the forced clearing of the Panel will happen only once
	 * for each call to forceBackgroundFill.
	 * After the Panel has been cleared the forced clearing is turned off.
	 *
	 * @see #forceBackgroundFill
	 */
	public void paint(Graphics g) {
		if (needs_background_paint) {
			needs_background_paint = false;
			Dimension dim = this.getSize();
			g.setClip(0, 0, dim.width, dim.height);
			g.setColor(this.getBackground());
			g.fillRect(0, 0, dim.width, dim.height);
		}
		super.paint(g);
	}


	/**
	 * Force a background fill when next paint of NeoPanel occurs.
	 * This is currently needed for cases when internal widgets are 
	 * moved around within a NeoPanel (for instance when calling 
	 * NeoAssembler.configureLayout().  Tried to do this with 
	 * Component events and ComponentListener, but they didn't give
	 * enough control over combining multiple component events into 
	 * single background fills (multiple paints appear to be getting 
	 * forced within the peers (without calls to paint()!)
	 */
	public void forceBackgroundFill() {
		needs_background_paint = true;
	}


}
