/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.prefs;

import java.util.*;
import java.util.prefs.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.awt.BorderLayout;

/**
 *  A panel that shows the preferences mapping between KeyStroke's and Actions. 
 */
public final class KeyStrokesView extends IPrefEditorComponent implements ListSelectionListener,
  PreferenceChangeListener {

  private final JTable table = new JTable();
  private final static String[] col_headings = {"Action", "Key Stroke"};
  private final DefaultTableModel model;
  private final ListSelectionModel lsm;
  private final TableRowSorter<DefaultTableModel> sorter;
  KeyStrokeEditPanel edit_panel = null;

  public KeyStrokesView() {
    super();
    this.setName("Shortcuts");
	this.setToolTipText("Edit Locations");
    this.setLayout(new BorderLayout());

    JScrollPane scroll_pane = new JScrollPane(table);
    this.add(scroll_pane, BorderLayout.CENTER);

    model = new DefaultTableModel() {
			@Override
      public boolean isCellEditable(int row, int column) {return false;}
            @Override
      public Class getColumnClass(int column) {
        return String.class;
      }
    };
    model.setDataVector(new Object[0][0], col_headings);

    lsm = table.getSelectionModel();
    lsm.addListSelectionListener(this);
    lsm.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

	sorter = new TableRowSorter<DefaultTableModel>(model);

    table.setModel(model);
    table.setRowSorter(sorter);
    table.setRowSelectionAllowed(true);
    table.setEnabled( true );

    edit_panel = new KeyStrokeEditPanel();
    edit_panel.setEnabled(false);
    this.add("South", edit_panel);

    try {PreferenceUtils.getKeystrokesNode().flush();} catch (Exception e) {}
    PreferenceUtils.getKeystrokesNode().addPreferenceChangeListener(this);

    showShortcuts();
    validate();
  }

  private static Object[][] buildRows(Preferences node) {
    Collection<String> keys = PreferenceUtils.getKeystrokesNodeNames();
	Object[][] rows;

	synchronized (keys) {
		int num_rows = keys.size();
		int num_cols = 2;
		rows = new Object[num_rows][num_cols];
		Iterator iter = keys.iterator();
		for (int i=0; iter.hasNext(); i++) {
			String key = (String) iter.next();
			rows[i][0] = key;
			rows[i][1] = node.get(key, "");
		}
	}
    return rows;
  }

  /** Re-populates the table with the shortcut data. */
  private void showShortcuts() {
    Object[][] rows = null;
    rows = buildRows(PreferenceUtils.getKeystrokesNode());
    model.setDataVector(rows, col_headings);
  }

  public void refresh() {
    showShortcuts();
  }

  /** This is called when the user selects a row of the table;
   */
  public void valueChanged(ListSelectionEvent evt) {
    if (evt.getSource()==lsm && ! evt.getValueIsAdjusting()) {
      int srow = table.getSelectedRow();
      if (srow >= 0) {
        String id = (String) table.getModel().getValueAt(srow, 0);
        editKeystroke(id);
      } else {
        edit_panel.setPreferenceKey(null, null, null);
      }
    }
  }
  
  private void editKeystroke(String id) {
    edit_panel.setPreferenceKey(PreferenceUtils.getKeystrokesNode(), id, "");
  }

  public void preferenceChange(PreferenceChangeEvent evt) {
    if (evt.getNode() != PreferenceUtils.getKeystrokesNode()) {
      return;
    }
    // Each time a keystroke preference is changed, update the
    // whole table.  Inelegant, but works.
    refresh();
  }  

  /*public void destroy() {
    removeAll();
    if (lsm != null) {lsm.removeListSelectionListener(this);}
    PrefenceUtils.getKeystrokesNode().removePreferenceChangeListener(this);
  }*/
}
