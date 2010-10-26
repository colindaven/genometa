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


import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.bioviews.SceneI;
import java.beans.Customizer;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;

/**
 * This is a superclass of all the genoviz beans customizers.
 */
public class NeoWidgetICustomizer
	extends Panel
	implements Customizer, ItemListener {

	protected NeoAbstractWidget widgeti;
	protected GridBagLayout layout;
	protected GridBagConstraints labelConstraints = new GridBagConstraints();
	protected GridBagConstraints valueConstraints = new GridBagConstraints();
	protected GridBagConstraints descConstraints =  new GridBagConstraints();
	protected FlowLayout valuePanelLayout = new FlowLayout(FlowLayout.LEFT);

	protected Choice colorChoice;
	protected Choice foregroundColorChoice;
	protected Choice fuzzinessChoice;
	protected Checkbox scrollingIncrBehavior = new Checkbox("auto-increment");
	protected Choice selectionChoice;
	protected Choice selectionColorChoice;
	protected Choice builtInSelectionChoice;
	protected Checkbox bandingBehavior = new Checkbox("active");
	protected Choice zoomingXChoice = new Choice();
	protected Choice zoomingYChoice = new Choice();
	protected Checkbox reshapingBehaviorX;
	protected Checkbox reshapingBehaviorY;

	public NeoWidgetICustomizer() {

		this.layout = new GridBagLayout();
		this.setLayout(this.layout);

		labelConstraints.fill = GridBagConstraints.HORIZONTAL;
		labelConstraints.anchor = GridBagConstraints.EAST;
		labelConstraints.gridx = 0;

		valueConstraints.gridy = 0;
		//valueConstraints.anchor = GridBagConstraints.CENTER;
		valueConstraints.anchor = GridBagConstraints.WEST;
		//valueConstraints.gridwidth = GridBagConstraints.REMAINDER;
		
		descConstraints.anchor = GridBagConstraints.WEST;
		descConstraints.gridwidth = GridBagConstraints.REMAINDER;

		includeBackgroundEditor();

		scrollingIncrBehavior.addItemListener(this);
		bandingBehavior.addItemListener(this);
		zoomingXChoice.addItemListener(this);
		zoomingYChoice.addItemListener(this);

	}

	public void includeBackgroundEditor() {
		Panel backgroundColorPanel = new Panel();
		colorChoice = new Choice();
		colorChoice.addItemListener(this);
		backgroundColorPanel.setLayout(valuePanelLayout);
		Label backgroundColorLabel = new Label("background:", Label.RIGHT);
		add(backgroundColorLabel);
		layout.setConstraints(backgroundColorLabel, labelConstraints);
		layout.setConstraints(backgroundColorPanel, valueConstraints);
		backgroundColorPanel.add(colorChoice);
		add(backgroundColorPanel);
		Label descLabel = new Label("Background color of the map", Label.LEFT);
		layout.setConstraints(descLabel, descConstraints);
		add(descLabel);
		valueConstraints.gridy++;
	}

	public void includeForegroundEditor() {
		Panel foregroundColorPanel = new Panel();
		foregroundColorChoice = new Choice();
		foregroundColorChoice.addItemListener(this);
		foregroundColorPanel.setLayout(valuePanelLayout);
		Label foregroundColorLabel = new Label("foreground:", Label.RIGHT);
		add(foregroundColorLabel);
		layout.setConstraints(foregroundColorLabel, labelConstraints);
		layout.setConstraints(foregroundColorPanel, valueConstraints);
		foregroundColorPanel.add(foregroundColorChoice);
		add(foregroundColorPanel);
		Label descLabel = new Label("Foreground color of the map", Label.LEFT);
		layout.setConstraints(descLabel, descConstraints);
		add(descLabel);
		valueConstraints.gridy++;
	}

	public void includeFuzzinessEditor() {
		Panel fuzzinessPanel = new Panel();
		fuzzinessChoice = new Choice();
		fuzzinessChoice.addItemListener(this);
		fuzzinessPanel.setLayout(valuePanelLayout);
		Label fuzzinessLabel = new Label("pointer precision:", Label.RIGHT);
		add(fuzzinessLabel);
		layout.setConstraints(fuzzinessLabel, labelConstraints);
		layout.setConstraints(fuzzinessPanel, valueConstraints);
		fuzzinessPanel.add(fuzzinessChoice);
		add(fuzzinessPanel);
		Label descLabel = new Label("Precision of pointer in the map", Label.LEFT);
		layout.setConstraints(descLabel, descConstraints);
		add(descLabel);
		valueConstraints.gridy++;
	}

	protected Panel selectionPanel;

	// Selection
	public void includeSelection() {
		selectionPanel = new Panel();
		builtInSelectionChoice = new Choice();
		builtInSelectionChoice.addItemListener(this);
		selectionChoice = new Choice();
		selectionChoice.addItemListener(this);
		selectionColorChoice = new Choice();
		selectionColorChoice.addItemListener(this);
		selectionPanel.setLayout(valuePanelLayout);
		Label selectionLabel = new Label("Selection:", Label.RIGHT);
		add(selectionLabel);
		layout.setConstraints(selectionLabel, labelConstraints);
		layout.setConstraints(selectionPanel, valueConstraints);
		selectionPanel.add(selectionChoice);
		selectionPanel.add(selectionColorChoice);
		add(selectionPanel);
		Label descLabel = new Label("Appearance, color and event for selected glyph", Label.LEFT);
		layout.setConstraints(descLabel, descConstraints);
		add(descLabel);
		valueConstraints.gridy++;
	}

	// Pointer Precision (pixel fuzziness)

	public void itemStateChanged(ItemEvent theEvent) {
		Object evtSource = theEvent.getSource();
		if (null == widgeti) {
		}
		else if (evtSource == this.colorChoice) {
			Color c = widgeti.getColor(this.colorChoice.getSelectedItem());
			Color prevColor = widgeti.getBackground();
			widgeti.setBackground(c);
			((Component)widgeti).repaint();
			this.secretary.firePropertyChange("Background", prevColor, c);
		}
		else if (evtSource == this.foregroundColorChoice) {
			Color c = widgeti.getColor(this.foregroundColorChoice.getSelectedItem());
			widgeti.setForeground(c);
			widgeti.updateWidget();
			this.secretary.firePropertyChange("Foreground", null, c);
		}
		else if (evtSource == this.fuzzinessChoice) {
			widgeti.setPixelFuzziness(Integer.parseInt(this.fuzzinessChoice.getSelectedItem()));
			this.secretary.firePropertyChange("PixelFuzziness", null,
					this.fuzzinessChoice.getSelectedItem());
			return;
		}
		else if (evtSource == this.scrollingIncrBehavior) {
			if(this.scrollingIncrBehavior.getState()) {
				widgeti.setScrollIncrementBehavior(NeoAbstractWidget.X,
						NeoAbstractWidget.AUTO_SCROLL_INCREMENT);
			}
			else {
				widgeti.setScrollIncrementBehavior(NeoAbstractWidget.X,
						NeoAbstractWidget.NO_AUTO_SCROLL_INCREMENT);
			}
			return;
		}
		else if (evtSource == this.selectionChoice
				||  evtSource == this.selectionColorChoice) {
			setSelectionAppearance();
				}
		else if (evtSource == this.bandingBehavior) {
			widgeti.setRubberBandBehavior(this.bandingBehavior.getState());
			widgeti.updateWidget();
			this.secretary.firePropertyChange("RubberBandBehavior",
					new Boolean(!this.bandingBehavior.getState()),
					new Boolean(this.bandingBehavior.getState()));
		}
		else if (evtSource == this.zoomingXChoice) {
			setScaleConstraint( NeoAbstractWidget.X, this.zoomingXChoice.getSelectedItem());
		}
		else if (evtSource == this.zoomingYChoice) {
			setScaleConstraint( NeoAbstractWidget.Y, this.zoomingYChoice.getSelectedItem());
		}
	}

	private void setSelectionAppearance() {
		int behavior = SceneI.SELECT_FILL;
		String s = this.selectionChoice.getSelectedItem();
		if (s.equals("Outlined")) {
			behavior = Scene.SELECT_OUTLINE;
		}
		else if (s.equals("Colored")) {
			behavior = Scene.SELECT_FILL;
		}
		else if (s.equals("Reversed")) {
			behavior = Scene.SELECT_REVERSE;
		}
		else if (s.equals("None")) {
			behavior = Scene.SELECT_NONE;
		}
		widgeti.setSelectionAppearance(behavior);
		this.secretary.firePropertyChange("SelectionAppearance",
				null, new Integer(behavior));
		Color color = widgeti.getColor(this.selectionColorChoice.getSelectedItem());
		widgeti.setSelectionColor(color);
		this.secretary.firePropertyChange("SelectionColor", null, color);
		widgeti.updateWidget();
	}

	private void setScaleConstraint(int theAxis, String theChoice) {
		if (theChoice.equalsIgnoreCase("Top")
				|| theChoice.equalsIgnoreCase("Left")) {
			widgeti.setZoomBehavior(theAxis, NeoAbstractWidget.CONSTRAIN_START);
				}
		else if (theChoice.equalsIgnoreCase("Center")
				|| theChoice.equalsIgnoreCase("Middle")) {
			widgeti.setZoomBehavior(theAxis, NeoAbstractWidget.CONSTRAIN_MIDDLE);
				}
		else if (theChoice.equalsIgnoreCase("Bottom")
				|| theChoice.equalsIgnoreCase("Right")) {
			widgeti.setZoomBehavior(theAxis, NeoAbstractWidget.CONSTRAIN_END);
				}
	}

	// PropertyChangeListener Methods

	protected PropertyChangeSupport secretary = new PropertyChangeSupport(this);
	public void addPropertyChangeListener(PropertyChangeListener listener){
		secretary.addPropertyChangeListener(listener);
	}
	public void removePropertyChangeListener(PropertyChangeListener listener){
		secretary.removePropertyChangeListener(listener);
	}
	public void setObject(Object bean) {
		if (bean instanceof NeoAbstractWidget) {
			this.widgeti = (NeoAbstractWidget)bean;
		}
		else {
			throw new IllegalArgumentException("need a NeoAbstractWidget");
		}

		// Background
		if (null != colorChoice) {
			loadColorChoice(colorChoice, widgeti.getBackground());
		}

		// Foreground
		if (null != foregroundColorChoice) {
			loadColorChoice(foregroundColorChoice, widgeti.getForeground());
		}

		// Fuzziness
		if (null != fuzzinessChoice) {
			int choices[] = { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 };
			loadIntegerChoice(fuzzinessChoice, choices, widgeti.getPixelFuzziness());
		}

		// Selection
		if (null != selectionColorChoice) {
			int selectionBehavior = widgeti.getSelectionAppearance();
			switch (selectionBehavior) {
				case Scene.SELECT_FILL:
					selectionChoice.addItem("Colored");
					selectionChoice.addItem("Outlined");
					//selectionChoice.addItem("Reversed");
					selectionChoice.addItem("None");
					break;
				case Scene.SELECT_OUTLINE:
					selectionChoice.addItem("Outlined");
					selectionChoice.addItem("Colored");
					//selectionChoice.addItem("Reversed");
					selectionChoice.addItem("None");
					break;
				case Scene.SELECT_NONE:
					selectionChoice.addItem("None");
					selectionChoice.addItem("Outlined");
					selectionChoice.addItem("Colored");
					//selectionChoice.addItem("Reversed");
					break;
				case Scene.SELECT_REVERSE:
					selectionChoice.addItem("Reversed");
					selectionChoice.addItem("Colored");
					selectionChoice.addItem("Outlined");
					selectionChoice.addItem("None");
					break;
				default:
					selectionChoice.addItem("Colored");
					selectionChoice.addItem("Outlined");
					//selectionChoice.addItem("Reversed");
					selectionChoice.addItem("None");
					break;
			}
			loadColorChoice(selectionColorChoice, widgeti.getSelectionColor());
		}

	}


	protected void loadColorChoice(Choice theChoice, Color theDefaultColor) {
		if (null == theDefaultColor) {
			loadColorChoice(theChoice, (String)null);
		}
		else {
			loadColorChoice(theChoice, widgeti.getColorName(theDefaultColor));
		}
	}

	protected void loadColorChoice(Choice theChoice, String theDefaultColor) {
		loadChoice(theChoice, widgeti.getColorNames());
		if (null != theDefaultColor) {
			theChoice.select(theDefaultColor);
		}
	}

	protected void loadChoice(Choice theChoice, Enumeration theChoices) {
		while (theChoices.hasMoreElements()) {
			theChoice.addItem(""+theChoices.nextElement());
		}
	}

	protected void loadChoice(Choice theChoice, Object[] theChoices, Object theDefault) {
		for (int i = 0; i < theChoices.length; i++) {
			theChoice.addItem(""+theChoices[i]);
		}
		if (null != theDefault) {
			theChoice.select(""+theDefault);
		}
	}

	protected void loadIntegerChoice(Choice theChoice, int[] theChoices) {
		for (int i = 0; i < theChoices.length; i++) {
			theChoice.addItem(""+theChoices[i]);
		}
	}

	protected void loadIntegerChoice(Choice theChoice, int[] theChoices, int theDefault) {
		int i;
		for (i = 0; i < theChoices.length && theChoices[i] <= theDefault; i++) {
			theChoice.addItem(""+theChoices[i]);
		}
		if (i < theChoices.length && theChoices[i] < theDefault) {
			theChoice.addItem(""+theDefault);
		}
		for ( ; i < theChoices.length; i++) {
			theChoice.addItem(""+theChoices[i]);
		}
		if (theChoices[--i] < theDefault) {
			theChoice.addItem(""+theDefault);
		}
		theChoice.select(""+theDefault);
	}


}
