/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.awt.*;
import java.util.prefs.*;
import javax.swing.*;
import java.awt.event.*;

/**
 *
 * @author Markus
 */
public final class TooltipEditorView extends IPrefEditorComponent implements PreferenceChangeListener {

	private final JList list = new JList();
	private final DefaultListModel model;
	private final JButton add_button = new JButton("Add Element");
	private final JButton remove_button = new JButton("Remove Element");
	private final JButton up_button = new JButton("Up");
	private final JButton down_button = new JButton("Down");
	private final JButton default_button = new JButton("Set Defaults");
	private final JComboBox element_cb = new JComboBox();

	public TooltipEditorView() {
		super();

		fillCombobox();

		this.setName("Tooltip Editor");
		this.setToolTipText("Edit Tooltip display");
		this.setLayout(new BorderLayout());

		element_cb.setMinimumSize(new Dimension(300,50));
		element_cb.setMaximumSize(new Dimension(300,50));

		element_cb.setAlignmentX(LEFT_ALIGNMENT);

		//Box button_box = new Box(BoxLayout.Y_AXIS);
		Box button_box = Box.createVerticalBox();
		button_box.add(element_cb);
		button_box.add(Box.createVerticalStrut(25));
		button_box.add(add_button);
		button_box.add(remove_button);
		button_box.add(up_button);
		button_box.add(down_button);
		button_box.add(Box.createVerticalStrut(25));
		button_box.add(default_button);
		button_box.add( Box.createVerticalGlue() );

		JScrollPane scroll_pane = new JScrollPane(list);
		this.add(scroll_pane, BorderLayout.CENTER);
		this.add(button_box, BorderLayout.EAST);

		model = new DefaultListModel();

		list.setModel(model);

		try {
			PreferenceUtils.getTooltipPrefsNode().flush();
		} catch (Exception e) {
		}
		PreferenceUtils.getTooltipPrefsNode().addPreferenceChangeListener(this);

		add_button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				addAction();
			}
		});
		up_button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				upAction();
			}
		});
		down_button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				downAction();
			}
		});
		remove_button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				removeAction();
			}
		});
		default_button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				setDefaults();
			}
		});

		updateList();
		validate();
	}

	public void refresh() {
		//JOptionPane.showMessageDialog(null, "refresh()");
		if (PreferenceUtils.getTooltipPrefsNode().getBoolean("refresh", false)) {
			return;
		}
		//JOptionPane.showMessageDialog(null, "refreshing view");
		updateList();
	}

	private void updateList() {
		//JOptionPane.showMessageDialog(null, "showShortcuts()");
		Preferences tooltip_editor_node = PreferenceUtils.getTooltipPrefsNode();
		String num;

		if ( tooltip_editor_node.get( "0", "dummy").equals("dummy") ) {
			setDefaults();
		}
 else
		{

		model.clear();
		for (int i = 0; i < 20; i++) {
			num = new String().valueOf(i);
			if (!(tooltip_editor_node.get(num, "dummy").equals("dummy"))) {
				model.addElement(tooltip_editor_node.get(num, "no value"));
			}
		}
		}
	}

	public void preferenceChange(PreferenceChangeEvent evt) {
		//JOptionPane.showMessageDialog(null, "preferenceChange()");
		if (evt.getNode() != PreferenceUtils.getTooltipPrefsNode()) {
			return;
		}
		Boolean refresh = PreferenceUtils.getTooltipPrefsNode().getBoolean("refresh", true);
		if (refresh == true) {
			//JOptionPane.showMessageDialog(null, "refresh is true");
			refresh();
		}
	}

	public void addAction() {
		if (model.size() < 20) {
			model.addElement(element_cb.getSelectedItem());
			writeElement(model.size() - 1);
		} else {
			JOptionPane.showMessageDialog(null, "Maximum number of tooltip items reached");
		}
	}

	public void upAction() {
		//JOptionPane.showMessageDialog(null, "upAction()");
		int index = list.getSelectedIndex();
		if (index == -1) {
			JOptionPane.showMessageDialog(null, "Select something to move.");
		} else if (index > 0) {
			String temp = (String) model.remove(index);
			model.add(index - 1, temp);
			list.setSelectedIndex(index - 1);
		}
		writeElement(index - 1);
		writeElement(index);
	}

	public void downAction() {
		//JOptionPane.showMessageDialog(null, "downAction()");
		int index = list.getSelectedIndex();
		if (index == -1) {
			JOptionPane.showMessageDialog(null, "Select something to move.");
		} else if (index < model.size() - 1) {
			String temp = (String) model.remove(index);
			model.add(index + 1, temp);
			list.setSelectedIndex(index + 1);
		}
		writeElement(index + 1);
		writeElement(index);
	}

	public void removeAction() {
		int index = list.getSelectedIndex();
		if (index == -1) {
			JOptionPane.showMessageDialog(null, "Select something to remove.");
		} else if (model.size() == 1) {
			JOptionPane.showMessageDialog(null, "Can't remove last item.");
		} else {
			model.remove(index);
			list.setSelectedIndex(0);
		}
		writeAllElements();
	}

	public void fillCombobox() {
		element_cb.addItem("name");
		element_cb.addItem("id");
		element_cb.addItem("chromosome");
		element_cb.addItem("start");
		element_cb.addItem("end");
		element_cb.addItem("wrong item");
		element_cb.addItem("blank line");
	}

	synchronized void writeAllElements() {
		//JOptionPane.showMessageDialog(null, "writeAllElements()");
		Preferences tooltip_editor_node = PreferenceUtils.getTooltipPrefsNode();
		tooltip_editor_node.putBoolean("refresh", false);
		for (int i = 0; i < 20; i++) {
			String num = new String().valueOf(i);
			String item = new String();
			if (i < model.size()) {
				item = (String) model.get(i);
			} else {
				item = "dummy";
			}
			tooltip_editor_node.put(num, item);
		}
		tooltip_editor_node.putBoolean("refresh", true);
	}

	void writeElement(int index) {
		Preferences tooltip_editor_node = PreferenceUtils.getTooltipPrefsNode();
		String num = new String().valueOf(index);
		String item = new String();
		item = (String) model.get(index);
		//JOptionPane.showMessageDialog(null, item);
		tooltip_editor_node.put(num, item);
	}

	void setDefaults()
	{
		model.clear();
		model.addElement("name");
		model.addElement("id");
		model.addElement("chromosome");
		model.addElement("start");
		model.addElement("end");
		model.addElement("length");
		model.addElement("type");
		model.addElement("residues");
		model.addElement("VN");
		model.addElement("score");
		model.addElement("SEQ");
		model.addElement("SM");
		model.addElement("baseQuality");
		model.addElement("cigar");
		model.addElement("XA");
		model.addElement("forward");
		model.addElement("NM");
		model.addElement("method");
		model.addElement("MD");
		model.addElement("CL");
		writeAllElements();
	}
}
