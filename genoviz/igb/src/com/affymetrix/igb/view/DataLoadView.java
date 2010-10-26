/**
 *   Copyright (c) 2005-2007 Affymetrix, Inc.
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

import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.awt.BorderLayout;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;

public final class DataLoadView extends JComponent {
	private final SeqGroupView group_view;
	public static int TAB_DATALOAD_PREFS = -1;

	public DataLoadView() {
		this.setLayout(new BorderLayout());

		JPanel main_panel = new JPanel();

		this.add(main_panel);
		this.setBorder(BorderFactory.createEtchedBorder());

		main_panel.setLayout(new BorderLayout());

		group_view = new SeqGroupView();

		JSplitPane jPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, GeneralLoadView.getLoadView(), group_view);
		jPane.setResizeWeight(0.9);
		main_panel.add("Center", jPane);

		final PreferencesPanel pp = PreferencesPanel.getSingleton();
		TAB_DATALOAD_PREFS = pp.addPrefEditorComponent(new DataLoadPrefsView());
	}

	public void tableChanged() {
		GeneralLoadView.getLoadView().createFeaturesTable();
	}
}
