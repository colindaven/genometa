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


import java.awt.Checkbox;
import java.awt.Choice;
import java.awt.Color;
import java.awt.GraphicsEnvironment;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;

public class NeoSeqCustomizer
	extends NeoWidgetICustomizer
{

	private NeoSeq neoSeq;

	Choice stripesOrientationChoice = new Choice();
	Choice stripeWidthChoice = new Choice();
	Choice stripe0ColorChoice = new Choice();
	Choice stripe1ColorChoice = new Choice();
	Choice fontNameChoice = new Choice();
	Choice fontSizeChoice = new Choice();
	Choice fontStyleChoice = new Choice();
	Choice fontColorChoice = new Choice();
	Color[] stripesColor;
	Choice scrollingIncrBehavior = new Choice();
	Choice spacingChoice = new Choice();
	private final Checkbox editable = new Checkbox();

	public NeoSeqCustomizer() {

		super();

		// Stripes
		Panel stripesPanel = new Panel();
		stripesPanel.setLayout(valuePanelLayout);
		Label stripesLabel = new Label("Stripes:", Label.RIGHT);
		add(stripesLabel);
		layout.setConstraints(stripesLabel, labelConstraints);
		layout.setConstraints(stripesPanel, valueConstraints);
		stripesPanel.add(stripesOrientationChoice);
		stripesOrientationChoice.addItemListener(this);
		stripesPanel.add(stripeWidthChoice);
		stripeWidthChoice.addItemListener(this);
		stripesPanel.add(stripe0ColorChoice);
		stripe0ColorChoice.addItemListener(this);
		stripesPanel.add(stripe1ColorChoice);
		stripe1ColorChoice.addItemListener(this);
		add(stripesPanel);
		valueConstraints.gridy++;

		// Font
		Panel fontPanel = new Panel();
		fontPanel.setLayout(valuePanelLayout);
		Label fontLabel = new Label("Font:", Label.RIGHT);
		add(fontLabel);
		layout.setConstraints(fontLabel, labelConstraints);
		layout.setConstraints(fontPanel, valueConstraints);
		fontPanel.add(fontNameChoice);
		fontNameChoice.addItemListener(this);
		fontPanel.add(fontSizeChoice);
		fontSizeChoice.addItemListener(this);
		fontPanel.add(fontColorChoice);
		fontColorChoice.addItemListener(this);
		add(fontPanel);
		valueConstraints.gridy++;

		// Scrolling
		Panel scrollingPanel = new Panel();
		scrollingPanel.setLayout(valuePanelLayout);
		Label scrollingLabel = new Label("Scrolling:", Label.RIGHT);
		add(scrollingLabel);
		layout.setConstraints(scrollingLabel, labelConstraints);
		layout.setConstraints(scrollingPanel, valueConstraints);
		scrollingPanel.add(scrollingIncrBehavior);
		scrollingIncrBehavior.addItemListener(this);
		add(scrollingPanel);
		valueConstraints.gridy++;

		// Line Spacing
		Panel spacingPanel = new Panel();
		spacingPanel.setLayout(valuePanelLayout);
		Label spacingLabel = new Label("Line Spacing:", Label.RIGHT);
		add(spacingLabel);
		layout.setConstraints(spacingLabel, labelConstraints);
		layout.setConstraints(spacingPanel, valueConstraints);
		spacingPanel.add(spacingChoice);
		spacingChoice.addItemListener(this);
		add(spacingPanel);
		valueConstraints.gridy++;

		// Editable
		Panel ePanel = new Panel();
		ePanel.setLayout(valuePanelLayout);
		Label eLabel = new Label("Editable:", Label.RIGHT);
		add(eLabel);
		layout.setConstraints(eLabel, labelConstraints);
		layout.setConstraints(ePanel, valueConstraints);
		ePanel.add(editable);
		editable.addItemListener(this);
		add(ePanel);
		valueConstraints.gridy++;

		// Selection
		includeSelection();

	}

	public void itemStateChanged(ItemEvent theEvent) {
		Object evtSource = theEvent.getSource();
		if (null == this.neoSeq) {
		}
		else if (evtSource == this.colorChoice) {
			Color c = NeoAbstractWidget.getColor(this.colorChoice.getSelectedItem());
			neoSeq.setResiduesBackground(c);
			neoSeq.setNumbersBackground(c);
			neoSeq.repaint();
			return;
		}
		else if (evtSource == this.scrollingIncrBehavior) {
			int i = this.scrollingIncrBehavior.getSelectedIndex();
			if (0 == i) {
				neoSeq.setScrollIncrementBehavior(NeoAbstractWidget.Y,
						NeoAbstractWidget.AUTO_SCROLL_INCREMENT);
			}
			else {
				neoSeq.setScrollIncrementBehavior(NeoAbstractWidget.Y,
						NeoAbstractWidget.NO_AUTO_SCROLL_INCREMENT);
				i = Integer.parseInt(this.scrollingIncrBehavior.getSelectedItem());
				neoSeq.setScrollIncrement(i);
			}
			return;
		}
		else if (evtSource == this.stripesOrientationChoice) {
			String s = ((Choice)evtSource).getSelectedItem();
			if (s.equalsIgnoreCase("Vertical")) {
				neoSeq.setStripeOrientation(NeoSeq.VERTICAL_STRIPES);
			}
			else if (s.equalsIgnoreCase("Horizontal")) {
				neoSeq.setStripeOrientation(NeoSeq.HORIZONTAL_STRIPES);
			}
			else if (s.equalsIgnoreCase("None")) {
				neoSeq.setStripeOrientation(NeoSeq.NO_STRIPES);
			}
			neoSeq.updateWidget();
			return;
		}
		else if (evtSource == this.stripeWidthChoice) {
			String s = ((Choice)evtSource).getSelectedItem();
			int size = Integer.parseInt(s);
			neoSeq.setStripeWidth(size);
			neoSeq.updateWidget();
			return;
		}
		else if (evtSource == this.fontNameChoice) {
			neoSeq.setFontName(((Choice)evtSource).getSelectedItem());
			neoSeq.updateWidget();
			return;
		}
		else if (evtSource == this.fontSizeChoice) {
			String s = ((Choice)evtSource).getSelectedItem();
			int size = Integer.parseInt(s);
			neoSeq.setFontSize(size);
			neoSeq.updateWidget();
			return;
		}
		else if (evtSource == this.fontColorChoice) {
			String cs = ((Choice)evtSource).getSelectedItem();
			Color c = NeoAbstractWidget.getColor(cs);
			neoSeq.setResidueFontColor(c);
			neoSeq.updateWidget();
			return;
		}
		else if (evtSource == this.stripe0ColorChoice) {
			stripesColor[0] = NeoAbstractWidget.getColor(this.stripe0ColorChoice.getSelectedItem());
			neoSeq.setStripeColors(stripesColor);
			neoSeq.updateWidget();
			return;
		}
		else if (evtSource == this.stripe1ColorChoice) {
			stripesColor[1] = NeoAbstractWidget.getColor(this.stripe1ColorChoice.getSelectedItem());
			neoSeq.setStripeColors(stripesColor);
			neoSeq.updateWidget();
			return;
		}
		else if (evtSource == this.spacingChoice) {
			neoSeq.setSpacing(Integer.parseInt(this.spacingChoice.getSelectedItem()));
			neoSeq.updateWidget();
			return;
		}
		else if (evtSource == this.editable) {
			neoSeq.setEditable(this.editable.getState());
			neoSeq.updateWidget();
			return;
		}
		super.itemStateChanged(theEvent);
	}

	// PropertyChangeListener Methods

	public void setObject(Object bean) {
		NeoSeq neoSeq;
		if (bean instanceof NeoSeq) {
			neoSeq = (NeoSeq)bean;
			super.setObject(neoSeq);
		}
		else {
			throw new IllegalArgumentException("need a NeoSeq");
		}

		// Background handled by superclass NeoWidgetICustomizer.

		// Stripes
		int stripesOrientation = neoSeq.getStripeOrientation();
		switch (stripesOrientation) {
			case NeoSeq.VERTICAL_STRIPES:
				stripesOrientationChoice.addItem("Vertical");
				stripesOrientationChoice.addItem("Horizontal");
				stripesOrientationChoice.addItem("None");
				break;
			case NeoSeq.HORIZONTAL_STRIPES:
				stripesOrientationChoice.addItem("Vertical");
				stripesOrientationChoice.addItem("None");
				break;
			case NeoSeq.NO_STRIPES:
				stripesOrientationChoice.addItem("None");
				stripesOrientationChoice.addItem("Vertical");
				break;
		}
		int dfltStripeWidth = neoSeq.getStripeWidth();
		int stripeWidth[] = {5, 10, 15, 20, 25};
		loadIntegerChoice(stripeWidthChoice, stripeWidth, dfltStripeWidth);
		stripesColor = neoSeq.getStripeColors();
		Color stripeColor = null;
		if (0 < stripesColor.length) {
			stripeColor = stripesColor[0];
		}
		String stripeColorName = "Transparent";
		if (null != stripeColor) {
			stripeColorName = NeoAbstractWidget.getColorName(stripeColor);
		}
		stripe0ColorChoice.addItem("Transparent");
		loadColorChoice(stripe0ColorChoice, stripeColorName);
		if (1 < stripesColor.length) {
			stripeColor = stripesColor[1];
		}
		stripeColorName = "Transparent";
		if (null != stripeColor) {
			stripeColorName = NeoAbstractWidget.getColorName(stripeColor);
		}
		stripe1ColorChoice.addItem("Transparent");
		loadColorChoice(stripe1ColorChoice, stripeColorName);

		// Font
		String fontName = neoSeq.getFont().getFamily();
		String[] fl = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
		loadChoice(fontNameChoice, fl, fontName);
		int fontSize[] = { 8, 10, 12, 14, 18, 24 };
		int defltFontSize = neoSeq.getFont().getSize();
		loadIntegerChoice(fontSizeChoice, fontSize, defltFontSize);
		Color fontColor = neoSeq.getResidueColor();
		loadColorChoice(fontColorChoice, fontColor);

		// Scrolling
		scrollingIncrBehavior.addItem("Autoincrement");
		int[] incr = { 1, 5, 10, 15, 20, 25 };
		loadIntegerChoice(scrollingIncrBehavior, incr);
		int id = neoSeq.getScrollIncrementBehavior(NeoAbstractWidget.X);
		if (NeoAbstractWidget.AUTO_SCROLL_INCREMENT == id) {
			scrollingIncrBehavior.select(0);
		}
		else {
			id = NeoAbstractWidget.AUTO_SCROLL_INCREMENT;
			scrollingIncrBehavior.select(1);
		}

		// Line Spacing
		int[] spacings = { 0, 5, 10, 15, 20 };
		int defltSpacing = neoSeq.getSpacing();
		loadIntegerChoice(spacingChoice, spacings, defltSpacing);

		this.editable.setState( neoSeq.isEditable() );

		this.neoSeq = neoSeq;
	}


}
