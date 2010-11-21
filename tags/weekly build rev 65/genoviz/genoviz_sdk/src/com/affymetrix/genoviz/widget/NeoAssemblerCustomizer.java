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
import java.awt.CheckboxGroup;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ItemEvent;

/**
 *
 * @version $Id: NeoAssemblerCustomizer.java 5082 2010-01-27 20:08:55Z anatara5 $
 */
public class NeoAssemblerCustomizer
	extends NeoWidgetICustomizer
{

	NeoAssembler assembler;

	Checkbox scrollingOpt = new Checkbox("optimized");
	Checkbox damageOpt = new Checkbox("optimized");
	Checkbox autoSort = new Checkbox("automatic");

	Checkbox labelsLeft = new Checkbox("left");
	Checkbox labelsRight = new Checkbox("right");
	Checkbox scrollersLeft = new Checkbox("left");
	Checkbox scrollersRight = new Checkbox("right");
	Checkbox axisTop = new Checkbox("top");
	Checkbox axisBottom = new Checkbox("bottom");
	Checkbox consensusTop = new Checkbox("top");
	Checkbox consensusBottom = new Checkbox("bottom");

	public NeoAssemblerCustomizer() {

		// Selection
		includeSelection();

		// Scrolling
		// test only.
		Panel scrollingPanel = new Panel();
		scrollingPanel.setLayout(valuePanelLayout);
		Label scrollingLabel = new Label("Scrolling:", Label.RIGHT);
		layout.setConstraints(scrollingLabel, labelConstraints);
		layout.setConstraints(scrollingPanel, valueConstraints);
		scrollingPanel.add(scrollingOpt);
		add(scrollingLabel); // testing only
		add(scrollingPanel); // testing only
		valueConstraints.gridy++;


		scrollingOpt.addItemListener(this);
		damageOpt.addItemListener(this);
		autoSort.addItemListener(this);

		// Labels Position
		Panel labelsPanel = new Panel();
		labelsPanel.setLayout(valuePanelLayout);
		Label labelsPositionLabel = new Label("Labels:", Label.RIGHT);
		add(labelsPositionLabel);
		layout.setConstraints(labelsPositionLabel, labelConstraints);
		layout.setConstraints(labelsPanel, valueConstraints);
		CheckboxGroup labelsPosistionGroup = new CheckboxGroup();
		labelsLeft.setCheckboxGroup(labelsPosistionGroup);
		labelsRight.setCheckboxGroup(labelsPosistionGroup);
		labelsPanel.add(labelsLeft);
		labelsPanel.add(labelsRight);
		add(labelsPanel);
		valueConstraints.gridy++;

		labelsLeft.addItemListener(this);
		labelsRight.addItemListener(this);
		scrollersLeft.addItemListener(this);
		scrollersRight.addItemListener(this);
		axisTop.addItemListener(this);
		axisBottom.addItemListener(this);
		consensusTop.addItemListener(this);
		consensusBottom.addItemListener(this);

		// Offset Scroller Position
		Panel scrollersPanel = new Panel();
		scrollersPanel.setLayout(valuePanelLayout);
		Label scrollersPositionLabel = new Label("Offset Scroller:", Label.RIGHT);
		add(scrollersPositionLabel);
		layout.setConstraints(scrollersPositionLabel, labelConstraints);
		layout.setConstraints(scrollersPanel, valueConstraints);
		CheckboxGroup scrollersPosistionGroup = new CheckboxGroup();
		scrollersLeft.setCheckboxGroup(scrollersPosistionGroup);
		scrollersRight.setCheckboxGroup(scrollersPosistionGroup);
		scrollersPanel.add(scrollersLeft);
		scrollersPanel.add(scrollersRight);
		add(scrollersPanel);
		valueConstraints.gridy++;

		// Axis Position
		Panel axisPanel = new Panel();
		axisPanel.setLayout(valuePanelLayout);
		Label axisPositionLabel = new Label("Sequence Scroller:", Label.RIGHT);
		add(axisPositionLabel);
		layout.setConstraints(axisPositionLabel, labelConstraints);
		layout.setConstraints(axisPanel, valueConstraints);
		CheckboxGroup axisPosistionGroup = new CheckboxGroup();
		axisTop.setCheckboxGroup(axisPosistionGroup);
		axisBottom.setCheckboxGroup(axisPosistionGroup);
		axisPanel.add(axisTop);
		axisPanel.add(axisBottom);
		add(axisPanel);
		valueConstraints.gridy++;

		// Consensus Position
		Panel consensusPanel = new Panel();
		consensusPanel.setLayout(valuePanelLayout);
		Label consensusPositionLabel = new Label("Consensus Sequence:", Label.RIGHT);
		add(consensusPositionLabel);
		layout.setConstraints(consensusPositionLabel, labelConstraints);
		layout.setConstraints(consensusPanel, valueConstraints);
		CheckboxGroup consensusPosistionGroup = new CheckboxGroup();
		consensusTop.setCheckboxGroup(consensusPosistionGroup);
		consensusBottom.setCheckboxGroup(consensusPosistionGroup);
		consensusPanel.add(consensusTop);
		consensusPanel.add(consensusBottom);
		add(consensusPanel);
		valueConstraints.gridy++;

		// Sort
		Panel sortPanel = new Panel();
		sortPanel.setLayout(valuePanelLayout);
		Label sortLabel = new Label("Sorting:", Label.RIGHT);
		add(sortLabel);
		layout.setConstraints(sortLabel, labelConstraints);
		layout.setConstraints(sortPanel, valueConstraints);
		sortPanel.add(autoSort);
		add(sortPanel);

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
		assembler.setSelectionEvent(method);
		assembler.updateWidget();
	}

	public void itemStateChanged(ItemEvent theEvent) {
		Object evtSource = theEvent.getSource();
		if (evtSource == this.labelsLeft) {
			assembler.configureLayout(NeoAssembler.LABELS, NeoAssembler.PLACEMENT_LEFT);
			return;
		}
		else if (evtSource == this.labelsRight) {
			assembler.configureLayout(NeoAssembler.LABELS, NeoAssembler.PLACEMENT_RIGHT);
			return;
		}
		else if (evtSource == this.scrollersLeft) {
			assembler.configureLayout(NeoAssembler.OFFSET_SCROLLER, NeoAssembler.PLACEMENT_LEFT);
			return;
		}
		else if (evtSource == this.scrollersRight) {
			assembler.configureLayout(NeoAssembler.OFFSET_SCROLLER, NeoAssembler.PLACEMENT_RIGHT);
			return;
		}
		else if (evtSource == this.scrollingOpt) {
			assembler.setScrollingOptimized(this.scrollingOpt.getState());
			assembler.setDamageOptimized(this.scrollingOpt.getState());
			return;
		}
		else if (evtSource == this.damageOpt) {
			assembler.setDamageOptimized(this.damageOpt.getState());
			return;
		}
		else if (evtSource == this.axisTop) {
			assembler.configureLayout(NeoAssembler.AXIS_SCROLLER, NeoAssembler.PLACEMENT_TOP);
			return;
		}
		else if (evtSource == this.axisBottom) {
			assembler.configureLayout(NeoAssembler.AXIS_SCROLLER, NeoAssembler.PLACEMENT_BOTTOM);
			return;
		}
		else if (evtSource == this.consensusTop) {
			assembler.configureLayout(NeoAssembler.CONSENSUS, NeoAssembler.PLACEMENT_TOP);
			return;
		}
		else if (evtSource == this.consensusBottom) {
			assembler.configureLayout(NeoAssembler.CONSENSUS, NeoAssembler.PLACEMENT_BOTTOM);
			return;
		}
		else if (evtSource == this.autoSort) {
			assembler.setAutoSort(this.autoSort.getState());
			if (assembler.getAutoSort()) {
				assembler.updateWidget();
			}
			return;
		}
		else if (evtSource == this.builtInSelectionChoice) {
			setSelectionEvent();
			return;
		}
		super.itemStateChanged(theEvent);
	}

	// PropertyChangeListener Methods

	public void setObject(Object bean) {
		NeoAssembler assembler;
		if (bean instanceof NeoAssembler) {
			assembler = (NeoAssembler)bean;
			super.setObject(assembler);
		}
		else {
			throw new IllegalArgumentException("need a NeoAssembler");
		}

		if (null != builtInSelectionChoice) {
			String[] selectionMethods = { "Off",
				"On mouse down" };
			int current = assembler.getSelectionEvent();
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

		int id = assembler.getPlacement(NeoAssembler.LABELS);
		this.labelsLeft.setState(NeoAssembler.PLACEMENT_LEFT == id);
		this.labelsRight.setState(NeoAssembler.PLACEMENT_RIGHT == id);
		id = assembler.getPlacement(NeoAssembler.OFFSET_SCROLLER);
		this.scrollersLeft.setState(NeoAssembler.PLACEMENT_LEFT == id);
		this.scrollersRight.setState(NeoAssembler.PLACEMENT_RIGHT == id);
		this.scrollingOpt.setState(assembler.isScrollingOptimized());
		this.damageOpt.setState(assembler.isDamageOptimized());
		id = assembler.getPlacement(NeoAssembler.AXIS_SCROLLER);
		this.axisTop.setState(NeoAssembler.PLACEMENT_TOP == id);
		this.axisBottom.setState(NeoAssembler.PLACEMENT_BOTTOM == id);
		this.autoSort.setState(assembler.getAutoSort());
		id = assembler.getPlacement(NeoAssembler.CONSENSUS);
		this.consensusTop.setState(NeoAssembler.PLACEMENT_TOP == id);
		this.consensusBottom.setState(NeoAssembler.PLACEMENT_BOTTOM == id);

		this.assembler = assembler;
	}


}
