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
import java.awt.Color;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;

/**
 * Customizer for a NeoMap for use with NeoMap as a Java bean.
 *
 * @version $Id: NeoMapCustomizer.java 4940 2010-01-06 21:10:36Z hiralv $
 */
public class NeoMapCustomizer
	extends NeoWidgetCustomizer
{

	NeoMap map;
	Choice mapColorChoice;

	public NeoMapCustomizer() {
		super();
		//includeMapColorEditor();
	}

	public void includeSelection() {
		super.includeSelection();
		selectionPanel.add(builtInSelectionChoice);
	}

	public void includeMapColorEditor() {
		Panel mapColorPanel = new Panel();
		mapColorChoice = new Choice();
		mapColorChoice.addItemListener(this);
		mapColorPanel.setLayout(valuePanelLayout);
		Label mapColorLabel = new Label("Map Color:", Label.RIGHT);
		add(mapColorLabel);
		layout.setConstraints(mapColorLabel, labelConstraints);
		layout.setConstraints(mapColorPanel, valueConstraints);
		mapColorPanel.add(mapColorChoice);
		add(mapColorPanel);
		Label descLabel = new Label("Background color of the map", Label.LEFT);
		layout.setConstraints(descLabel, descConstraints);
		add(descLabel);
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
		map.setSelectionEvent(method);
		map.updateWidget();
	}

	public void itemStateChanged(ItemEvent theEvent) {
		if (null == this.map) {
		}
		else if (theEvent.getSource() == this.builtInSelectionChoice) {
			setSelectionEvent();
			return;
		}
		else if (theEvent.getSource() == mapColorChoice) {
			Color c = map.getColor(this.mapColorChoice.getSelectedItem());
			map.setMapColor(c);
			map.updateWidget();
			return;
		}
		super.itemStateChanged(theEvent);
	}

	// PropertyChangeListener Methods

	public void setObject(Object bean) {
		NeoMap map;
		if (bean instanceof NeoMap) {
			map = (NeoMap)bean;
			super.setObject(map);
		}
		else {
			throw new IllegalArgumentException("need a NeoMap");
		}
		// Background
		if (null != mapColorChoice) {
			loadColorChoice(mapColorChoice, map.getMapColor());
		}
		if (null != builtInSelectionChoice) {
			String[] selectionMethods = { "Off",
				"On mouse down",
				"On mouse up" };
			int current = map.getSelectionEvent();
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

		int reshapeBehavior = map.getReshapeBehavior(NeoMap.X);
		this.reshapingBehaviorX.setState(NeoMap.FITWIDGET == reshapeBehavior);
		reshapeBehavior = map.getReshapeBehavior(NeoMap.Y);
		this.reshapingBehaviorY.setState(NeoMap.FITWIDGET == reshapeBehavior);

		this.map = (NeoMap)bean;
	}

}
