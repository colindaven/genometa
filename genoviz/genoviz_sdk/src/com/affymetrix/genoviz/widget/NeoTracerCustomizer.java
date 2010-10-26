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

package com.affymetrix.genoviz.widget;

import java.awt.*;
import java.awt.event.ItemEvent;

public class NeoTracerCustomizer
	extends NeoWidgetICustomizer
{

	private NeoTracer neoTracer;

	public NeoTracerCustomizer() {

		super();

		includeSelection();

	}

	public void itemStateChanged(ItemEvent theEvent) {
		Object evtSource = theEvent.getSource();
		if (null == this.neoTracer) {
		}
		else if (evtSource == this.builtInSelectionChoice) {
			setSelectionEvent();
			neoTracer.updateWidget();
			return;
		}
		super.itemStateChanged(theEvent);
	}

	// Selection
	public void includeSelection() {
		Panel selectionPanel = new Panel();
		builtInSelectionChoice = new Choice();
		builtInSelectionChoice.addItemListener(this);
		selectionChoice = new Choice();
		selectionColorChoice = new Choice();
		selectionPanel.setLayout(valuePanelLayout);
		Label selectionLabel = new Label("Selection:", Label.RIGHT);
		add(selectionLabel);
		layout.setConstraints(selectionLabel, labelConstraints);
		layout.setConstraints(selectionPanel, valueConstraints);
		selectionPanel.add(builtInSelectionChoice);
		add(selectionPanel);
		valueConstraints.gridy++;
	}

	private void setSelectionEvent() {
		int method = NeoMap.NO_SELECTION;
		String s = this.builtInSelectionChoice.getSelectedItem();
		if (s.equals("On mouse down")) {
			method = NeoMap.ON_MOUSE_DOWN;
		}
		else if (s.equals("On mouse up")) {
			method = NeoMap.ON_MOUSE_UP;
		}
		neoTracer.setSelectionEvent(method);
		neoTracer.updateWidget();
	}

	// PropertyChangeListener Methods

	public void setObject(Object bean) {
		NeoTracer neoTracer;
		if (bean instanceof NeoTracer) {
			neoTracer = (NeoTracer)bean;
		}
		else {
			throw new IllegalArgumentException("need a NeoTracer");
		}
		super.setObject(neoTracer);

		// Background handled by superclass NeoWidgetICustomizer.

		// Built-in Selection
		if (null != builtInSelectionChoice) {
			String[] selectionMethods = { "Off",
				"On mouse down" };
			int current = neoTracer.getSelectionEvent();
			String currentChoice = "Off";
			switch (current) {
				case NeoMap.NO_SELECTION:
					currentChoice = "Off";
					break;
				case NeoMap.ON_MOUSE_DOWN:
					currentChoice = "On mouse down";
					break;
				case NeoMap.ON_MOUSE_UP:
					currentChoice = "On mouse up";
					break;
			}
			loadChoice(builtInSelectionChoice, selectionMethods, currentChoice);
		}

		this.neoTracer = neoTracer;
	}

}
