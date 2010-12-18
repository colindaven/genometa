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
    private javax.swing.JCheckBox show_all_tags_cb;
    private javax.swing.JButton up_button;

	private boolean _isBAM;

	public static final int DEFAULT_MAX_TOOLTIP_LENGTH = 25;
	public static final boolean DEFAULT_ENABLE_TOOLTIPS = true;
	public static final boolean DEFAULT_SHOW_ALL_TAGS = false;
	public static final int MAX_COUNT_OF_TOOLTIP_TAGS = 30;


	private String gff_tooltip_items[] = {
											"chromosome",
											"genome_name",
											"genome_species",
											"genome_strain",
											"db_xref",
											"end",
											"exon_number",
											"feature_type",
											"frame",
											"id",
											"length",
											"locus_tag",
											"method",
											"note",
											"product",
											"protein_id",
											"source",
											"start",
											"transl_table",
											"type"};
	private String bam_tooltip_items[] = {
											"baseQuality",
											"chromosome",
											"genome_name",
											"genome_species",
											"genome_strain",
											"cigar",
											"CL",
											"end",
											"forward",
											"id",
											"length",
											"MD",
											"method",
											"name",
											"NM",
											"residues",
											"score",
											"SEQ",
											"SM",
											"start",
											"type",
											"VN",
											"XA"};
	

	public TooltipEditorView(boolean isBAM) {
		super();

		_isBAM = isBAM;

		initComponents();

		fillCombobox();

		if(_isBAM) {
			this.setName("Tooltip Editor BAM");
			this.setToolTipText("Edit Tooltip BAM display");
		}
		else {
			this.setName("Tooltip Editor GFF");
			this.setToolTipText("Edit Tooltip GFF display");
		}
		this.setLayout(new BorderLayout());

		this.add( main_panel, BorderLayout.CENTER );

		model = new DefaultListModel();

		list.setModel(model);

		if(_isBAM) {
			try {
				PreferenceUtils.getTooltipEditorBAMPrefsNode().flush();
			} catch (Exception e) {
			}
			PreferenceUtils.getTooltipEditorBAMPrefsNode().addPreferenceChangeListener(this);
		}
		else {
			try {
				PreferenceUtils.getTooltipEditorGFFPrefsNode().flush();
			} catch (Exception e) {
			}
			PreferenceUtils.getTooltipEditorGFFPrefsNode().addPreferenceChangeListener(this);
		}

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
		max_length_ff.setEditable(false);
		enable_tooltips_cb.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				enableTooltips();
			}
		});
		show_all_tags_cb.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				showAllTags();
			}
		});
		updateGlobalTooltipSettings();
		updateList();
		validate();
	}

	public void refresh() {
		//JOptionPane.showMessageDialog(null, "refresh()");
		if(_isBAM) {
			if (PreferenceUtils.getTooltipEditorBAMPrefsNode().getBoolean("refresh", false)) {
				return;
			}
		}
		else {
			if (PreferenceUtils.getTooltipEditorGFFPrefsNode().getBoolean("refresh", false)) {
				return;
			}
		}
		
		//JOptionPane.showMessageDialog(null, "refreshing view");
		updateList();
	}

	public void updateGlobalTooltipSettings() {
		int max_length = PreferenceUtils.getTooltipEditorBAMPrefsNode().getInt("tooltip_length", DEFAULT_MAX_TOOLTIP_LENGTH);
		max_length_sl.setValue(max_length);
		max_length_ff.setText((new Integer(max_length)).toString());

		boolean enable_tooltip = PreferenceUtils.getTooltipEditorBAMPrefsNode().getBoolean("enable_tooltips", DEFAULT_ENABLE_TOOLTIPS);
		enable_tooltips_cb.setSelected(enable_tooltip);

		boolean show_all_tags = PreferenceUtils.getTooltipEditorBAMPrefsNode().getBoolean("show_all_tags", DEFAULT_SHOW_ALL_TAGS);
		show_all_tags_cb.setSelected(show_all_tags);
	}

	private void updateList() {
		//JOptionPane.showMessageDialog(null, "showShortcuts()");
		Preferences tooltip_editor_node = null;
		if(_isBAM) {
			tooltip_editor_node = PreferenceUtils.getTooltipEditorBAMPrefsNode();
		}
		else {
			tooltip_editor_node = PreferenceUtils.getTooltipEditorGFFPrefsNode();
		}
		String num;

		if ( tooltip_editor_node.get( "0", "dummy").equals("dummy") ) {
			setDefaults();
		}
		else {
			model.clear();
			for (int i = 0; i < MAX_COUNT_OF_TOOLTIP_TAGS; i++) {
				num = new String().valueOf(i);
				if (!(tooltip_editor_node.get(num, "dummy").equals("dummy"))) {
					model.addElement(tooltip_editor_node.get(num, "no value"));
				}
			}
		}
	}

	public void preferenceChange(PreferenceChangeEvent evt) {
		//JOptionPane.showMessageDialog(null, "preferenceChange()");
		if(_isBAM) {
			if (evt.getNode() != PreferenceUtils.getTooltipEditorBAMPrefsNode()) {
				return;
			}
			Boolean refresh = PreferenceUtils.getTooltipEditorBAMPrefsNode().getBoolean("refresh", true);
			if (refresh == true) {
				//JOptionPane.showMessageDialog(null, "refresh is true");
				refresh();
			}
		}
		else {
			if (evt.getNode() != PreferenceUtils.getTooltipEditorGFFPrefsNode()) {
				return;
			}
			Boolean refresh = PreferenceUtils.getTooltipEditorGFFPrefsNode().getBoolean("refresh", true);
			if (refresh == true) {
				//JOptionPane.showMessageDialog(null, "refresh is true");
				refresh();
			}
		}
		
	}

	public void addAction() {
		if (model.size() < MAX_COUNT_OF_TOOLTIP_TAGS) {
			model.addElement(element_cb.getSelectedItem());
			writeElement(model.size() - 1);
		} else {
			JOptionPane.showMessageDialog(null, "Maximum number of tooltip items reached");
		}
	}

	public void addBlankLineAction() {
		if (model.size() < MAX_COUNT_OF_TOOLTIP_TAGS) {
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
		PreferenceUtils.getTooltipEditorBAMPrefsNode().putInt("tooltip_length", length);
	}

	public void enableTooltips() {
		PreferenceUtils.getTooltipEditorBAMPrefsNode().putBoolean("enable_tooltips", enable_tooltips_cb.isSelected());
	}

	public void showAllTags() {
		PreferenceUtils.getTooltipEditorBAMPrefsNode().putBoolean("show_all_tags", show_all_tags_cb.isSelected());
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
		if(_isBAM) {
			for(int i = 0; i < bam_tooltip_items.length; i++) {
				element_cb.addItem(bam_tooltip_items[i].toLowerCase());
			}
		}
		else {
			for(int i = 0; i < gff_tooltip_items.length; i++) {
				element_cb.addItem(gff_tooltip_items[i].toLowerCase());
			}
		}
		element_cb.addItem("[----------]");
	}

	synchronized void writeAllElements() {
		//JOptionPane.showMessageDialog(null, "writeAllElements()");
		Preferences tooltip_editor_node = null;
		if(_isBAM) {
			tooltip_editor_node = PreferenceUtils.getTooltipEditorBAMPrefsNode();
		}
		else {
			tooltip_editor_node = PreferenceUtils.getTooltipEditorGFFPrefsNode();
		}
		tooltip_editor_node.putBoolean("refresh", false);
		for (int i = 0; i < MAX_COUNT_OF_TOOLTIP_TAGS; i++) {
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
		Preferences tooltip_editor_node = null;
		if(_isBAM) {
			tooltip_editor_node = PreferenceUtils.getTooltipEditorBAMPrefsNode();
		}
		else {
			tooltip_editor_node = PreferenceUtils.getTooltipEditorGFFPrefsNode();
		}
		String num = new String().valueOf(index);
		String item = new String();
		item = (String) model.get(index);
		//JOptionPane.showMessageDialog(null, item);
		tooltip_editor_node.put(num, item);
	}

	void setDefaults() {
		model.clear();
		if(_isBAM) {
			for(int i = 0; i < bam_tooltip_items.length; i++) {
				model.addElement(bam_tooltip_items[i].toLowerCase());
			}
		}
		else {
			for(int i = 0; i < gff_tooltip_items.length; i++) {
				model.addElement(gff_tooltip_items[i].toLowerCase());
			}
		}
		writeAllElements();

//		max_length_sl.setValue(DEFAULT_MAX_TOOLTIP_LENGTH);
//		max_length_ff.setText((new Integer(DEFAULT_MAX_TOOLTIP_LENGTH)).toString());
//		PreferenceUtils.getTooltipEditorBAMPrefsNode().putInt("tooltip_length", DEFAULT_MAX_TOOLTIP_LENGTH);
//
//		enable_tooltips_cb.setSelected(DEFAULT_ENABLE_TOOLTIPS);
//		PreferenceUtils.getTooltipEditorBAMPrefsNode().putBoolean("enable_tooltips", DEFAULT_ENABLE_TOOLTIPS);
//
//		show_all_tags_cb.setSelected(DEFAULT_SHOW_ALL_TAGS);
//		PreferenceUtils.getTooltipEditorBAMPrefsNode().putBoolean("show_all_tags", DEFAULT_SHOW_ALL_TAGS);
	}
	

    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        main_panel = new javax.swing.JPanel();
        settings_panel = new javax.swing.JPanel();
        max_length_desc_l = new javax.swing.JLabel();
        max_length_sl = new javax.swing.JSlider();
        max_length_ff = new javax.swing.JFormattedTextField();
        enable_tooltips_cb = new javax.swing.JCheckBox();
        show_all_tags_cb = new javax.swing.JCheckBox();
        editor_panel = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        list = new javax.swing.JList();
        add_button = new javax.swing.JButton();
        element_cb = new javax.swing.JComboBox();
        add_blank_line_button = new javax.swing.JButton();
        up_button = new javax.swing.JButton();
        down_button = new javax.swing.JButton();
        remove_button = new javax.swing.JButton();
        default_button = new javax.swing.JButton();

        settings_panel.setBorder(javax.swing.BorderFactory.createTitledBorder("Global tooltip settings"));

        max_length_desc_l.setText("Max. Length");

        max_length_sl.setMaximum(50);
        max_length_sl.setMinimum(10);
        max_length_sl.setValue(25);

        max_length_ff.setText("25");

        enable_tooltips_cb.setSelected(true);
        enable_tooltips_cb.setText("Enable tooltips");
        enable_tooltips_cb.setToolTipText("Enable tooltips");
        enable_tooltips_cb.setMargin(new java.awt.Insets(2, 6, 2, 2));

        show_all_tags_cb.setText("Show all available tags");

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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(settings_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(max_length_ff, javax.swing.GroupLayout.DEFAULT_SIZE, 216, Short.MAX_VALUE)
                    .addComponent(show_all_tags_cb))
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
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(settings_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(enable_tooltips_cb)
                    .addComponent(show_all_tags_cb))
                .addContainerGap())
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

        default_button.setText("Reset to defaults");

        javax.swing.GroupLayout editor_panelLayout = new javax.swing.GroupLayout(editor_panel);
        editor_panel.setLayout(editor_panelLayout);
        editor_panelLayout.setHorizontalGroup(
            editor_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(editor_panelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 312, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(editor_panelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(remove_button, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(down_button, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(up_button, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addComponent(add_blank_line_button, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, editor_panelLayout.createSequentialGroup()
                        .addComponent(add_button, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(element_cb, 0, 75, Short.MAX_VALUE))
                    .addComponent(default_button, javax.swing.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE))
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
                        .addComponent(remove_button)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 66, Short.MAX_VALUE)
                        .addComponent(default_button)))
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
                .addComponent(settings_panel, javax.swing.GroupLayout.DEFAULT_SIZE, 101, Short.MAX_VALUE)
                .addContainerGap())
        );

        settings_panel.getAccessibleContext().setAccessibleName("Global Tooltip Settings");

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
