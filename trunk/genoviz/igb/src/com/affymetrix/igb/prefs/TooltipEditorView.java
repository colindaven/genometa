/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.*;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.*;
import javax.swing.*;
import java.awt.event.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 *
 * @author Markus
 */
public final class TooltipEditorView extends IPrefEditorComponent implements PreferenceChangeListener {

	private final DefaultListModel model;
	/*
	private final JList list = new JList();
	private final JButton add_button = new JButton("Add Element");
	private final JButton remove_button = new JButton("Remove Element");
	private final JButton up_button = new JButton("Up");
	private final JButton down_button = new JButton("Down");
	private final JButton default_button = new JButton("Set Defaults");
	private final JComboBox element_cb = new JComboBox();
	*/

	
	private javax.swing.JButton add_blank_line_button;
    private javax.swing.JButton add_button;
    private javax.swing.JButton default_button;
    private javax.swing.JButton down_button;
    private javax.swing.JPanel editor_panel;
    private javax.swing.JComboBox element_cb;
    private javax.swing.JCheckBox enable_tooltips_cb;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JList list;
    private javax.swing.JPanel main_panel;
    private javax.swing.JLabel max_length_desc_l;
    private javax.swing.JFormattedTextField max_length_ff;
    private javax.swing.JSlider max_length_sl;
    private javax.swing.JButton remove_button;
    private javax.swing.JPanel settings_panel;
    private javax.swing.JButton up_button;

	public static final int DEFAULT_MAX_TOOLTIP_LENGTH = 25;
	public static final boolean DEFAULT_ENABLE_TOOLTIPS = true;
	

	public TooltipEditorView() {
		super();

		initComponents();

		fillCombobox();

		this.setName("Tooltip Editor");
		this.setToolTipText("Edit Tooltip display");
		this.setLayout(new BorderLayout());

		this.add( main_panel, BorderLayout.CENTER );

		model = new DefaultListModel();

		list.setModel(model);

		try {
			PreferenceUtils.getTooltipEditorPrefsNode().flush();
		} catch (Exception e) {
		}
		PreferenceUtils.getTooltipEditorPrefsNode().addPreferenceChangeListener(this);

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
		add_blank_line_button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {
				addBlankLineAction();
			}
		});
		max_length_sl.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				lengthSliderChange();
			}
		});
		int max_length = PreferenceUtils.getTooltipEditorPrefsNode().getInt("tooltip_length", DEFAULT_MAX_TOOLTIP_LENGTH);
		max_length_sl.setValue(max_length);
		max_length_ff.setText((new Integer(max_length)).toString());
		max_length_ff.setEditable(false);
		enable_tooltips_cb.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				enableTooltips();
			}
		});
		boolean enable_tooltip = PreferenceUtils.getTooltipEditorPrefsNode().getBoolean("enable_tooltips", DEFAULT_ENABLE_TOOLTIPS);
		enable_tooltips_cb.setSelected(enable_tooltip);

		updateList();
		validate();
	}

	public void refresh() {
		//JOptionPane.showMessageDialog(null, "refresh()");
		if (PreferenceUtils.getTooltipEditorPrefsNode().getBoolean("refresh", false)) {
			return;
		}
		//JOptionPane.showMessageDialog(null, "refreshing view");
		updateList();
	}

	private void updateList() {
		//JOptionPane.showMessageDialog(null, "showShortcuts()");
		Preferences tooltip_editor_node = PreferenceUtils.getTooltipEditorPrefsNode();
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
		if (evt.getNode() != PreferenceUtils.getTooltipEditorPrefsNode()) {
			return;
		}
		Boolean refresh = PreferenceUtils.getTooltipEditorPrefsNode().getBoolean("refresh", true);
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

	public void addBlankLineAction() {
		if (model.size() < 20) {
			model.addElement("[----------]");
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

	public void lengthSliderChange() {
		int length = max_length_sl.getValue();
		max_length_ff.setText((new Integer(length)).toString());
		PreferenceUtils.getTooltipEditorPrefsNode().putInt("tooltip_length", length);
	}

	public void enableTooltips() {
		PreferenceUtils.getTooltipEditorPrefsNode().putBoolean("enable_tooltips", enable_tooltips_cb.isSelected());
		SeqMapView.SHOW_PROP_TOOLTIP = enable_tooltips_cb.isSelected();
	}

	public void removeAction() {
		int index = list.getSelectedIndex();
		if (index == -1) {
			JOptionPane.showMessageDialog(null, "Select something to remove.");
		} else if (model.size() == 1) {
			JOptionPane.showMessageDialog(null, "Can't remove last item.");
		} else {
			model.remove(index);
			if(index > 0) {
				list.setSelectedIndex(index-1);
			}
			else {
				list.setSelectedIndex(0);
			}
		}
		writeAllElements();
	}

	public void fillCombobox() {
		element_cb.addItem("name");
		element_cb.addItem("id");
		element_cb.addItem("chromosome");
		element_cb.addItem("start");
		element_cb.addItem("end");
		element_cb.addItem("length");
		element_cb.addItem("type");
		element_cb.addItem("residues");
		element_cb.addItem("VN");
		element_cb.addItem("score");
		element_cb.addItem("SEQ");
		element_cb.addItem("SM");
		element_cb.addItem("baseQuality");
		element_cb.addItem("cigar");
		element_cb.addItem("XA");
		element_cb.addItem("forward");
		element_cb.addItem("NM");
		element_cb.addItem("method");
		element_cb.addItem("MD");
		element_cb.addItem("CL");
		element_cb.addItem("miep");
	}

	synchronized void writeAllElements() {
		//JOptionPane.showMessageDialog(null, "writeAllElements()");
		Preferences tooltip_editor_node = PreferenceUtils.getTooltipEditorPrefsNode();
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
		Preferences tooltip_editor_node = PreferenceUtils.getTooltipEditorPrefsNode();
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

		max_length_sl.setValue(DEFAULT_MAX_TOOLTIP_LENGTH);
		max_length_ff.setText((new Integer(DEFAULT_MAX_TOOLTIP_LENGTH)).toString());
		PreferenceUtils.getTooltipEditorPrefsNode().putInt("tooltip_length", DEFAULT_MAX_TOOLTIP_LENGTH);

		enable_tooltips_cb.setSelected(DEFAULT_ENABLE_TOOLTIPS);
		PreferenceUtils.getTooltipEditorPrefsNode().putBoolean("enable_tooltips", DEFAULT_ENABLE_TOOLTIPS);
	}
 
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        main_panel = new javax.swing.JPanel();
        settings_panel = new javax.swing.JPanel();
        max_length_desc_l = new javax.swing.JLabel();
        max_length_sl = new javax.swing.JSlider();
        default_button = new javax.swing.JButton();
        max_length_ff = new javax.swing.JFormattedTextField();
        enable_tooltips_cb = new javax.swing.JCheckBox();
        editor_panel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list = new javax.swing.JList();
        add_button = new javax.swing.JButton();
        element_cb = new javax.swing.JComboBox();
        add_blank_line_button = new javax.swing.JButton();
        up_button = new javax.swing.JButton();
        down_button = new javax.swing.JButton();
        remove_button = new javax.swing.JButton();

        settings_panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Settings"));

        max_length_desc_l.setText("Max. Length");

        max_length_sl.setMaximum(50);
        max_length_sl.setMinimum(10);
        max_length_sl.setValue(25);

        default_button.setText("Reset to defaults");

        max_length_ff.setText("25");

        enable_tooltips_cb.setSelected(true);
        enable_tooltips_cb.setText("Enable tooltips");
        enable_tooltips_cb.setToolTipText("Enable tooltips");
        enable_tooltips_cb.setMargin(new java.awt.Insets(2, 6, 2, 2));

        javax.swing.GroupLayout settings_panelLayout = new javax.swing.GroupLayout(settings_panel);
        settings_panel.setLayout(settings_panelLayout);
        settings_panelLayout.setHorizontalGroup(
            settings_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settings_panelLayout.createSequentialGroup()
                .addGroup(settings_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(settings_panelLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(max_length_desc_l)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(max_length_sl, javax.swing.GroupLayout.PREFERRED_SIZE, 228, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(enable_tooltips_cb))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(settings_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(default_button, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE)
                    .addComponent(max_length_ff, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 220, Short.MAX_VALUE))
                .addContainerGap())
        );
        settings_panelLayout.setVerticalGroup(
            settings_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(settings_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(settings_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(max_length_ff, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(max_length_sl, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(max_length_desc_l))
                .addGroup(settings_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(settings_panelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(default_button)
                        .addContainerGap())
                    .addGroup(settings_panelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(enable_tooltips_cb)
                        .addContainerGap())))
        );

        editor_panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Editor"));

        list.setBackground(new java.awt.Color(255, 255, 204));
        list.setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
        list.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(list);

        add_button.setText("Add item:");

        add_blank_line_button.setText("Add Blank Line");

        up_button.setText("Move up");

        down_button.setText("Move down");

        remove_button.setText("Remove item");

        javax.swing.GroupLayout editor_panelLayout = new javax.swing.GroupLayout(editor_panel);
        editor_panel.setLayout(editor_panelLayout);
        editor_panelLayout.setHorizontalGroup(
            editor_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editor_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(editor_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(remove_button, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(down_button, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(up_button, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(add_blank_line_button, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addGroup(editor_panelLayout.createSequentialGroup()
                        .addComponent(add_button, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(element_cb, 0, 75, Short.MAX_VALUE)))
                .addContainerGap())
        );
        editor_panelLayout.setVerticalGroup(
            editor_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editor_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(editor_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 252, Short.MAX_VALUE)
                    .addGroup(editor_panelLayout.createSequentialGroup()
                        .addGroup(editor_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(element_cb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(add_button))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(add_blank_line_button)
                        .addGap(18, 18, 18)
                        .addComponent(up_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(down_button)
                        .addGap(18, 18, 18)
                        .addComponent(remove_button)))
                .addContainerGap())
        );

        add_button.getAccessibleContext().setAccessibleName("add_button");
        add_blank_line_button.getAccessibleContext().setAccessibleName("add_blank_line_button");

        javax.swing.GroupLayout main_panelLayout = new javax.swing.GroupLayout(main_panel);
        main_panel.setLayout(main_panelLayout);
        main_panelLayout.setHorizontalGroup(
            main_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, main_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(main_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(settings_panel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(editor_panel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        main_panelLayout.setVerticalGroup(
            main_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(main_panelLayout.createSequentialGroup()
                .addComponent(editor_panel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(settings_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(main_panel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>

}
