/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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
package com.affymetrix.igb.view;

import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.tiers.AffyTieredMap;

import java.awt.Adjustable;
import java.awt.event.*;

import javax.swing.*;

final class SeqMapViewActionListener implements ActionListener {

	private final static String ZOOM_OUT_FULLY = "ZOOM_OUT_FULLY";
	private final static String ZOOM_OUT_X = "ZOOM_OUT_X";
	private final static String ZOOM_IN_X = "ZOOM_IN_X";
	private final static String ZOOM_OUT_Y = "ZOOM_OUT_Y";
	private final static String ZOOM_IN_Y = "ZOOM_IN_Y";
	private final static String SCROLL_UP = "SCROLL_UP";
	private final static String SCROLL_DOWN = "SCROLL_DOWN";
	private final static String SCROLL_LEFT = "SCROLL_LEFT";
	private final static String SCROLL_RIGHT = "SCROLL_RIGHT";
	private final static String ZOOM_TO_SELECTED = "Zoom to selected";
	private final static String[] commands = {ZOOM_OUT_FULLY,
		ZOOM_OUT_X, ZOOM_IN_X, ZOOM_OUT_Y, ZOOM_IN_Y,
		SCROLL_UP, SCROLL_DOWN, SCROLL_RIGHT, SCROLL_LEFT};
	private final Action zoom_out_fully_action;
	private final Action zoom_out_x_action;
	private final Action zoom_in_x_action;
	private final Action zoom_out_y_action;
	private final Action zoom_in_y_action;
	private final Action zoom_to_selected_action;
	private final AffyTieredMap seqmap;
	private final SeqMapView gviewer;

	SeqMapViewActionListener(SeqMapView gviewer) {

		this.gviewer = gviewer;
		seqmap = gviewer.seqmap;

		for (String command : commands) {
			MenuUtil.addAccelerator((JComponent) gviewer, this, command);
		}
		zoom_out_x_action = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_OUT_X);
			}
		};
		zoom_in_x_action = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_IN_X);
			}
		};
		zoom_out_y_action = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_OUT_Y);
			}
		};
		zoom_in_y_action = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_IN_Y);
			}
		};
		zoom_out_fully_action = new AbstractAction() {

			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_OUT_FULLY);
			}
		};

		Icon icon0 = MenuUtil.getIcon("toolbarButtonGraphics/general/Zoom16.gif");
		zoom_to_selected_action = new AbstractAction(ZOOM_TO_SELECTED, icon0) {

			public void actionPerformed(ActionEvent e) {
				doAction(ZOOM_TO_SELECTED);
			}
		};

		Icon icon1 = MenuUtil.getIcon("toolbarButtonGraphics/general/ZoomOut16.gif");
		zoom_out_y_action.putValue(Action.NAME, "Zoom out vertically");
		zoom_out_y_action.putValue(Action.SMALL_ICON, icon1);
		zoom_out_x_action.putValue(Action.NAME, "Zoom out horizontally");
		zoom_out_x_action.putValue(Action.SMALL_ICON, icon1);

		Icon icon2 = MenuUtil.getIcon("toolbarButtonGraphics/general/ZoomIn16.gif");
		zoom_in_y_action.putValue(Action.NAME, "Zoom in vertically");
		zoom_in_y_action.putValue(Action.SMALL_ICON, icon2);
		zoom_in_x_action.putValue(Action.NAME, "Zoom in horizontally");
		zoom_in_x_action.putValue(Action.SMALL_ICON, icon2);

		Icon icon3 = MenuUtil.getIcon("toolbarButtonGraphics/navigation/Home16.gif");
		zoom_out_fully_action.putValue(Action.SHORT_DESCRIPTION, "Zoom out fully");
		zoom_out_fully_action.putValue(Action.NAME, "Home Position");
		zoom_out_fully_action.putValue(Action.SMALL_ICON, icon3);
	}

	public void actionPerformed(ActionEvent evt) {
		String command = evt.getActionCommand();
		//System.out.println("SeqMapView received action event "+command);
		doAction(command);
	}

	private void doAction(String command) {
//    if (command.equals(gviewer.zoomtoMI.getText())) {
//      gviewer.zoomToSelections();
//    }
		if (command.equals(gviewer.selectParentMI.getText())) {
			gviewer.selectParents();

		} else if (command.equals(gviewer.centerMI.getText())) {
			gviewer.centerAtHairline();
		} else if (command.equals(gviewer.viewinSequenceViewer.getText())) {
			gviewer.openSequenceViewer();
		} else if (command.equals(ZOOM_OUT_FULLY)) {
			Adjustable adj = seqmap.getZoomer(NeoMap.X);
			adj.setValue(adj.getMinimum());
			adj = seqmap.getZoomer(NeoMap.Y);
			adj.setValue(adj.getMinimum());
			//map.updateWidget();
		} else if (command.equals(ZOOM_OUT_X)) {
			Adjustable adj = seqmap.getZoomer(NeoMap.X);
			adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
			//map.updateWidget();
		} else if (command.equals(ZOOM_IN_X)) {
			Adjustable adj = seqmap.getZoomer(NeoMap.X);
			adj.setValue(adj.getValue() + (adj.getMaximum() - adj.getMinimum()) / 20);
			//map.updateWidget();
		} else if (command.equals(ZOOM_OUT_Y)) {
			Adjustable adj = seqmap.getZoomer(NeoMap.Y);
			adj.setValue(adj.getValue() - (adj.getMaximum() - adj.getMinimum()) / 20);
			//map.updateWidget();
		} else if (command.equals(ZOOM_IN_Y)) {
			Adjustable adj = seqmap.getZoomer(NeoMap.Y);
			adj.setValue(adj.getValue() + (adj.getMaximum() - adj.getMinimum()) / 20);
			//map.updateWidget();
		} else if (command.equals(SCROLL_LEFT)) {
			int[] visible = seqmap.getVisibleRange();
			seqmap.scroll(NeoAbstractWidget.X, visible[0] + (visible[1] - visible[0]) / 10);
			seqmap.updateWidget();
		} else if (command.equals(SCROLL_RIGHT)) {
			int[] visible = seqmap.getVisibleRange();
			seqmap.scroll(NeoAbstractWidget.X, visible[0] - (visible[1] - visible[0]) / 10);
			seqmap.updateWidget();
		} else if (command.equals(SCROLL_UP)) {
			int[] visible = seqmap.getVisibleOffset();
			seqmap.scroll(NeoAbstractWidget.Y, visible[0] + (visible[1] - visible[0]) / 10);
			seqmap.updateWidget();
		} else if (command.equals(SCROLL_DOWN)) {
			int[] visible = seqmap.getVisibleOffset();
			seqmap.scroll(NeoAbstractWidget.Y, visible[0] - (visible[1] - visible[0]) / 10);
			seqmap.updateWidget();
		}
	}
}
