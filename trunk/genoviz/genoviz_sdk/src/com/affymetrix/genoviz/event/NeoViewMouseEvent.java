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
import java.util.List;
import com.affymetrix.genoviz.bioviews.View;

/**
 * a NeoMouseEvent whose source is a {@link View} rather than a Component.
 */
public class NeoViewMouseEvent extends NeoMouseEvent {

	protected View view;

	public NeoViewMouseEvent(MouseEvent ome, View view,
			double xcoord, double ycoord) {
		super(ome, (Component)ome.getSource(), UNKNOWN, xcoord, ycoord);
		this.view = view;
	}

	/**
	 * overriding getSource() to return the NeoViewMouseEvent's view,
	 * <em>not</em> the component source of the original MouseEvent.
	 * This contortion is needed because NeoMouseEvent and MouseEvent
	 * constructors require a Component, but View is not a component.
	 */
	@Override
	public Object getSource() {
		return view;
	}

	public View getView()  {
		return view;
	}

	@Override
	public List<GlyphI> getItems() {
		return null;
	}

}
