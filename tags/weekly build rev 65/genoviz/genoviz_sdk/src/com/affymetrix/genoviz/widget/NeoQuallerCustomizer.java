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


import java.awt.Choice;
import java.awt.Label;
import java.awt.Panel; // Just for main
import java.awt.event.ItemEvent;

/**
 *
 * @version $Id: NeoQuallerCustomizer.java 3887 2009-06-23 23:07:30Z sgblanch $
 */
public class NeoQuallerCustomizer
	extends NeoWidgetICustomizer
{

	private NeoQualler neoQualler;

	public NeoQuallerCustomizer() {

		super();

		includeSelection();

	}

	public void itemStateChanged(ItemEvent theEvent) {
		Object evtSource = theEvent.getSource();
		if (null == this.neoQualler) {
		}
		else if (evtSource == this.builtInSelectionChoice) {
			setSelectionEvent();
			neoQualler.updateWidget();
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
		neoQualler.setSelectionEvent(method);
		neoQualler.updateWidget();
	}

	// PropertyChangeListener Methods

	public void setObject(Object bean) {
		NeoQualler neoQualler;
		if (bean instanceof NeoQualler) {
			neoQualler = (NeoQualler)bean;
		}
		else {
			throw new IllegalArgumentException("need a NeoQualler");
		}
		super.setObject(neoQualler);

		// Background handled by superclass NeoWidgetICustomizer.

		// Built-in Selection
		if (null != builtInSelectionChoice) {
			String[] selectionMethods = { "Off",
				"On mouse down" };
			int current = neoQualler.getSelectionEvent();
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

		this.neoQualler = neoQualler;
	}


}
