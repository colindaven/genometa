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
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.*;
import java.awt.event.ItemEvent;

/**
 * Customizer for a NeoWidget for use with NeoWidget as a Java bean.
 *
 * @version $Id: NeoWidgetCustomizer.java 5126 2010-02-02 17:54:49Z anatara5 $
 */
public class NeoWidgetCustomizer
	extends NeoWidgetICustomizer
{

	protected NeoWidget widget;

	public NeoWidgetCustomizer() {

		super.includeFuzzinessEditor();

		// Scrolling
		Panel scrollingPanel = new Panel();
		scrollingPanel.setLayout(valuePanelLayout);
		Label scrollingLabel = new Label("Scrolling:", Label.RIGHT);
		add(scrollingLabel);
		layout.setConstraints(scrollingLabel, labelConstraints);
		layout.setConstraints(scrollingPanel, valueConstraints);
		scrollingPanel.add(scrollingIncrBehavior);
		add(scrollingPanel);
		Label descScrollingLabel = new Label("Don't know what Scrolling Do", Label.LEFT);
		layout.setConstraints(descScrollingLabel, descConstraints);
		add(descScrollingLabel);
		valueConstraints.gridy++;

		// Rubber Band
		Panel bandingPanel = new Panel();
		bandingPanel.setLayout(valuePanelLayout);
		Label bandingLabel = new Label("Rubber Band:", Label.RIGHT);
		add(bandingLabel);
		layout.setConstraints(bandingLabel, labelConstraints);
		layout.setConstraints(bandingPanel, valueConstraints);
		bandingPanel.add(bandingBehavior);
		add(bandingPanel);
		Label descBandingLabel = new Label("Allows to select multiple glyphs by dragging.", Label.LEFT);
		layout.setConstraints(descBandingLabel, descConstraints);
		add(descBandingLabel);
		valueConstraints.gridy++;

		// Zoom Behavior
		Panel zoomingPanel = new Panel();
		zoomingPanel.setLayout(valuePanelLayout);
		Label zoomingLabel = new Label("Zoom from:", Label.RIGHT);
		add(zoomingLabel);
		layout.setConstraints(zoomingLabel, labelConstraints);
		layout.setConstraints(zoomingPanel, valueConstraints);
		zoomingPanel.add(zoomingXChoice);
		zoomingPanel.add(zoomingYChoice);
		add(zoomingPanel);
		Label descZoomLabel = new Label("Horizontal and Vertical focus point for zooming", Label.LEFT);
		layout.setConstraints(descZoomLabel, descConstraints);
		add(descZoomLabel);
		valueConstraints.gridy++;

		includeReshapeBehavior();

		// Pointer Precision (pixel fuzziness)

		// Selection
		includeSelection();

	}

	// Reshape Behavior
	public void includeReshapeBehavior() {
		Panel reshapingPanel = new Panel();
		reshapingBehaviorX = new Checkbox("Fit X");
		reshapingBehaviorX.addItemListener(this);
		reshapingBehaviorY = new Checkbox("Fit Y");
		reshapingBehaviorY.addItemListener(this);
		reshapingPanel.setLayout(valuePanelLayout);
		Label reshapingLabel = new Label("Reshape:", Label.RIGHT);
		add(reshapingLabel);
		layout.setConstraints(reshapingLabel, labelConstraints);
		layout.setConstraints(reshapingPanel, valueConstraints);
		reshapingPanel.add(reshapingBehaviorX);
		reshapingPanel.add(reshapingBehaviorY);
		add(reshapingPanel);
		Label descLabel = new Label("Fit the map in available area", Label.LEFT);
		layout.setConstraints(descLabel, descConstraints);
		add(descLabel);
		valueConstraints.gridy++;
	}

	public void itemStateChanged(ItemEvent theEvent) {
		Object evtSource = theEvent.getSource();
		if (null == widget) {
		}
		else if (evtSource == this.scrollingIncrBehavior) {
			if(this.scrollingIncrBehavior.getState()) {
				widget.setScrollIncrementBehavior(NeoWidget.X,
						NeoWidget.AUTO_SCROLL_INCREMENT);
			}
			else {
				widget.setScrollIncrementBehavior(NeoWidget.X,
						NeoWidget.NO_AUTO_SCROLL_INCREMENT);
			}
			return;
		}
		else if (evtSource == this.selectionChoice
				||  evtSource == this.selectionColorChoice) {
			setSelectionAppearance();
			return;
				}
		else if (evtSource == this.bandingBehavior) {
			widget.setRubberBandBehavior(this.bandingBehavior.getState());
			widget.updateWidget();
			return;
		}
		else if (evtSource == this.zoomingXChoice) {
			setScaleConstraint( NeoAbstractWidget.X, this.zoomingXChoice.getSelectedItem());
			return;
		}
		else if (evtSource == this.zoomingYChoice) {
			setScaleConstraint( NeoAbstractWidget.Y, this.zoomingYChoice.getSelectedItem());
			return;
		}
		else if (evtSource == this.reshapingBehaviorX) {
			if (((Checkbox)evtSource).getState()) {
				widget.setReshapeBehavior(NeoAbstractWidget.X, NeoAbstractWidget.FITWIDGET);
				widget.updateWidget();
				widget.setSize(widget.getSize());
			}
			else {
				widget.setReshapeBehavior(NeoAbstractWidget.X, NeoConstants.NONE);
			}
			return;
		}
		else if (evtSource == this.reshapingBehaviorY) {
			if (((Checkbox)evtSource).getState()) {
				widget.setReshapeBehavior(NeoAbstractWidget.Y, NeoAbstractWidget.FITWIDGET);
				widget.updateWidget();
				widget.setSize(widget.getSize());
			}
			else {
				widget.setReshapeBehavior(NeoAbstractWidget.Y, NeoConstants.NONE);
			}
			return;
		}
		super.itemStateChanged(theEvent);
	}

	private void setSelectionAppearance() {
		int behavior = SceneI.SELECT_FILL;
		String s = this.selectionChoice.getSelectedItem();
		if (s.equals("Outlined")) {
			behavior = Scene.SELECT_OUTLINE;
		}
		else if (s.equals("Filled")) {
			behavior = Scene.SELECT_FILL;
		}
		else if (s.equals("None")) {
			behavior = Scene.SELECT_NONE;
		}
		else if( s.equals("Reversed") ) {
			behavior = Scene.SELECT_REVERSE;
		}

		widget.setSelectionAppearance(behavior);
		Color color = widget.getColor(this.selectionColorChoice.getSelectedItem());
		widget.setSelectionColor(color);
		widget.updateWidget();
	}

	private void setScaleConstraint(int theAxis, String theChoice) {
		if (theChoice.equalsIgnoreCase("Top")
				|| theChoice.equalsIgnoreCase("Left")) {
			widget.setZoomBehavior(theAxis, NeoAbstractWidget.CONSTRAIN_START);
				}
		else if (theChoice.equalsIgnoreCase("Center")
				|| theChoice.equalsIgnoreCase("Middle")) {
			widget.setZoomBehavior(theAxis, NeoAbstractWidget.CONSTRAIN_MIDDLE);
				}
		else if (theChoice.equalsIgnoreCase("Bottom")
				|| theChoice.equalsIgnoreCase("Right")) {
			widget.setZoomBehavior(theAxis, NeoAbstractWidget.CONSTRAIN_END);
				}
	}

	// PropertyChangeListener Methods

	public void setObject(Object bean) {
		NeoWidget widget;
		if (bean instanceof NeoWidget) {
			widget = (NeoWidget)bean;
			super.setObject(widget);
		}
		else {
			throw new IllegalArgumentException("need a NeoWidget");
		}

		// Scrolling
		int id = widget.getScrollIncrementBehavior(NeoAbstractWidget.X);
		scrollingIncrBehavior.setState(NeoWidget.AUTO_SCROLL_INCREMENT == id);

		// Selection is set in super.setObject().

		// Rubberband
		this.bandingBehavior.setState(widget.getRubberBandBehavior());

		// Zoom
		int zoomBehavior = widget.getZoomBehavior(NeoAbstractWidget.X);
		switch (zoomBehavior) {
			case NeoAbstractWidget.CONSTRAIN_START:
				zoomingXChoice.addItem("Left");
				zoomingXChoice.addItem("Center");
				zoomingXChoice.addItem("Right");
				break;
			case NeoAbstractWidget.CONSTRAIN_MIDDLE:
				zoomingXChoice.addItem("Center");
				zoomingXChoice.addItem("Left");
				zoomingXChoice.addItem("Right");
				break;
			case NeoAbstractWidget.CONSTRAIN_END:
				zoomingXChoice.addItem("Right");
				zoomingXChoice.addItem("Center");
				zoomingXChoice.addItem("Left");
				break;
			default:
				zoomingXChoice.addItem("Center");
				zoomingXChoice.addItem("Left");
				zoomingXChoice.addItem("Right");
				break;
		}
		zoomBehavior = widget.getZoomBehavior(NeoAbstractWidget.Y);
		switch (zoomBehavior) {
			case NeoAbstractWidget.CONSTRAIN_START:
				zoomingYChoice.addItem("Top");
				zoomingYChoice.addItem("Middle");
				zoomingYChoice.addItem("Bottom");
				break;
			case NeoAbstractWidget.CONSTRAIN_MIDDLE:
				zoomingYChoice.addItem("Middle");
				zoomingYChoice.addItem("Top");
				zoomingYChoice.addItem("Bottom");
				break;
			case NeoAbstractWidget.CONSTRAIN_END:
				zoomingYChoice.addItem("Bottom");
				zoomingYChoice.addItem("Middle");
				zoomingYChoice.addItem("Top");
				break;
			default:
				zoomingYChoice.addItem("Middle");
				zoomingYChoice.addItem("Top");
				zoomingYChoice.addItem("Bottom");
				break;
		}

		this.widget = widget;
	}


}
