/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.glyph.CharSeqGlyph;
import com.affymetrix.igb.util.ColorUtils;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.glyph.AlignedResidueGlyph;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.tiers.AxisStyle;
import com.affymetrix.igb.util.aligner.BowtieAlignerWrapper;
import com.affymetrix.igb.util.aligner.BwaAlignerWrapper;
import com.affymetrix.igb.view.OrfAnalyzer;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.UnibrowHairline;
import java.io.File;

/**
 *  A panel that shows the preferences for particular special URLs and file locations.
 */
public final class OptionsView extends IPrefEditorComponent implements ActionListener  {

  //final LocationEditPanel edit_panel1 = new LocationEditPanel();
  JButton clear_prefsB = new JButton("Reset all preferences to defaults");

  public OptionsView() {
    super();
    this.setName("Other Options");
	this.setToolTipText("Edit Miscellaneous Options");
    this.setLayout(new BorderLayout());

    JPanel main_box = new JPanel();
    main_box.setLayout(new BoxLayout(main_box,BoxLayout.Y_AXIS));
    main_box.setBorder(new javax.swing.border.EmptyBorder(5,5,5,5));

  
    JScrollPane scroll_pane = new JScrollPane(main_box);
    this.add(scroll_pane, BorderLayout.CENTER);

    
    Box misc_box = Box.createVerticalBox();
    

   
    misc_box.add(PreferenceUtils.createCheckBox("Ask before exiting", PreferenceUtils.getTopNode(),
      PreferenceUtils.ASK_BEFORE_EXITING, PreferenceUtils.default_ask_before_exiting));
    misc_box.add(PreferenceUtils.createCheckBox("Keep zoom stripe in view", PreferenceUtils.getTopNode(),
      UnibrowHairline.PREF_KEEP_HAIRLINE_IN_VIEW, UnibrowHairline.default_keep_hairline_in_view));
	misc_box.add(PreferenceUtils.createCheckBox("Enable Header-Correction on SAM/BAM-Files", PreferenceUtils.getTopNode(),
      LoadFileAction.PREF_HEADER_CORRECTION, LoadFileAction.default_pref_header_correction));
    misc_box.add(PreferenceUtils.createCheckBox("Enable sorting-dialog for unsorted BAM-Files", PreferenceUtils.getTopNode(),
      LoadFileAction.PREF_BAM_SORTING_DIALOG, LoadFileAction.default_pref_bam_sorting_dialog));


    misc_box.add(Box.createRigidArea(new Dimension(0,5)));

    misc_box.add(clear_prefsB);
    clear_prefsB.addActionListener(this);



    misc_box.add(Box.createRigidArea(new Dimension(0,5)));
	

    JPanel orf_box = new JPanel();
    orf_box.setLayout(new GridLayout(2,0));
    orf_box.setBorder(new javax.swing.border.TitledBorder("ORF Analyzer"));

	orf_box.add(addColorChooser("Stop Codon",OrfAnalyzer.PREF_STOP_CODON_COLOR, OrfAnalyzer.default_stop_codon_color));
	orf_box.add(addColorChooser("Dynamic ORF",OrfAnalyzer.PREF_DYNAMIC_ORF_COLOR, OrfAnalyzer.default_dynamic_orf_color));
	
	JPanel base_box = new JPanel();
    base_box.setLayout(new GridLayout(5,0));
    base_box.setBorder(new javax.swing.border.TitledBorder("Change Residue Colors"));

	base_box.add(addColorChooser("A", CharSeqGlyph.PREF_A_COLOR, CharSeqGlyph.default_A_color));
	base_box.add(addColorChooser("T", CharSeqGlyph.PREF_T_COLOR, CharSeqGlyph.default_T_color));
	base_box.add(addColorChooser("G", CharSeqGlyph.PREF_G_COLOR, CharSeqGlyph.default_G_color));
	base_box.add(addColorChooser("C", CharSeqGlyph.PREF_C_COLOR, CharSeqGlyph.default_C_color));
	base_box.add(addColorChooser("Other", CharSeqGlyph.PREF_OTHER_COLOR, CharSeqGlyph.default_other_color));


	String default_label_format = SeqMapView.VALUE_AXIS_LABEL_FORMAT_COMMA;
    String[] label_format_options = new String[] {SeqMapView.VALUE_AXIS_LABEL_FORMAT_FULL,
                                                  SeqMapView.VALUE_AXIS_LABEL_FORMAT_COMMA,
                                                  SeqMapView.VALUE_AXIS_LABEL_FORMAT_ABBREV};
    JComboBox axis_label_format_CB = PreferenceUtils.createComboBox(PreferenceUtils.getTopNode(), "Axis label format", label_format_options, default_label_format);

    JPanel axis_box = new JPanel();
    axis_box.setLayout(new GridLayout(3,0));
    axis_box.setBorder(new javax.swing.border.TitledBorder("Axis"));

	axis_box.add(addColorChooser("Foreground", AxisStyle.PREF_AXIS_COLOR, Color.BLACK));
	axis_box.add(addColorChooser("Background", AxisStyle.PREF_AXIS_BACKGROUND, Color.WHITE));
	axis_box.add(addToPanel("Number format", axis_label_format_CB));

	//MPTAG added
	JPanel dir_bar_box = new JPanel();
    dir_bar_box.setLayout(new GridLayout(4,2));
    dir_bar_box.setBorder(new javax.swing.border.TitledBorder("Direction Bar"));

    //JButton dir_bar_fw_color_button = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), AlignedResidueGlyph.dirBarFwColor, Color.RED);
    //dir_bar_box.add(new JLabel("Forward Direction: "));
    dir_bar_box.add(addColorChooser("Foreground Direction:", AlignedResidueGlyph.dirBarFwColor, Color.RED));

    //JButton dir_bar_rw_color_button = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), AlignedResidueGlyph.dirBarRwColor, Color.BLUE);
    //dir_bar_box.add(new JLabel("Reverse Direction: "));
    dir_bar_box.add(addColorChooser("Reverse Direction:", AlignedResidueGlyph.dirBarRwColor, Color.BLUE));

    dir_bar_box.add(new JLabel("Bar Location: "));
    JComboBox dir_bar_location_option = PreferenceUtils.createComboBox(PreferenceUtils.getTopNode(), 
			AlignedResidueGlyph.dirBarLocation, AlignedResidueGlyph.dirBarLocationValues, AlignedResidueGlyph.dirBarLocationValues[0]);
    dir_bar_box.add(dir_bar_location_option);
	dir_bar_box.add(new JLabel("Bar Style: "));
    JComboBox dir_bar_style_option = PreferenceUtils.createComboBox(PreferenceUtils.getTopNode(),
			AlignedResidueGlyph.dirBarStyle, AlignedResidueGlyph.dirBarStyleValues, AlignedResidueGlyph.dirBarStyleValues[0]);
    dir_bar_box.add(dir_bar_style_option);


    dir_bar_box.setAlignmentX(0.0f);
	//TODO ggf filechooser einbauen?
	JPanel aligner_opt_box = new JPanel();
	GridBagConstraints gbc = new GridBagConstraints();
    aligner_opt_box.setLayout(new GridBagLayout());
    aligner_opt_box.setBorder(new javax.swing.border.TitledBorder("Aligner options"));

    final JTextField aligner_opt_bowtie_location = new JTextField();
	//Try to set actual path to textfield
	aligner_opt_bowtie_location.setText(PreferenceUtils.getTopNode()
			.get(BowtieAlignerWrapper.BOWTIE_LOCATION_PREF, ""));
	JButton aligner_opt_bowtie_location_set = new JButton("Set");
	aligner_opt_bowtie_location_set.setToolTipText("Set the path to the bowtie aligner.");
	aligner_opt_bowtie_location_set.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(new File(aligner_opt_bowtie_location.getText()).exists()){
					BowtieAlignerWrapper.setBowtieExecutablePath(aligner_opt_bowtie_location.getText());
				}else{
					JOptionPane.showMessageDialog(PreferencesPanel.getSingleton().getFrame(), "The file you choosed does not exist",
							"File not found", JOptionPane.ERROR_MESSAGE);
				}
			} });
	gbc.fill = GridBagConstraints.BOTH;
	gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = .2;
    aligner_opt_box.add(new JLabel("bowtie location: "), gbc);
	gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = .6;
    aligner_opt_box.add(aligner_opt_bowtie_location, gbc);
	gbc.gridx = 2; gbc.gridy = 0; gbc.weightx = .2;
    aligner_opt_box.add(aligner_opt_bowtie_location_set, gbc);


    final JTextField aligner_opt_bwa_location = new JTextField();
	//Try to set actual path to textfield
	aligner_opt_bwa_location.setText(PreferenceUtils.getTopNode()
			.get(BwaAlignerWrapper.BWA_LOCATION_PREF, ""));
	JButton aligner_opt_bwa_location_set = new JButton("Set");
	aligner_opt_bwa_location_set.setToolTipText("Set the path to the bwa aligner");
	aligner_opt_bwa_location_set.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(new File(aligner_opt_bowtie_location.getText()).exists()){
					BwaAlignerWrapper.setBwaExecutablePath(aligner_opt_bwa_location.getText());
				}else{
					JOptionPane.showMessageDialog(PreferencesPanel.getSingleton().getFrame(), "The file you choosed does not exist",
							"File not found", JOptionPane.ERROR_MESSAGE);
				}
			} });
	gbc.fill = GridBagConstraints.BOTH;
	gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = .2;
    aligner_opt_box.add(new JLabel("bwa location: "), gbc);
	gbc.gridx = 1; gbc.gridy = 1; gbc.weightx = .6;
    aligner_opt_box.add(aligner_opt_bwa_location, gbc);
	gbc.gridx = 2; gbc.gridy = 1; gbc.weightx = .2;
    aligner_opt_box.add(aligner_opt_bwa_location_set, gbc);
    aligner_opt_box.setAlignmentX(0.0f);
	//MPTAG end

    axis_box.setAlignmentX(0.0f);
   
    orf_box.setAlignmentX(0.0f);
    misc_box.setAlignmentX(0.0f);
	base_box.setAlignmentX(0.0f);

   
    main_box.add(axis_box);
   
    main_box.add(orf_box);
	main_box.add(base_box);
	main_box.add(dir_bar_box);
	main_box.add(aligner_opt_box);
    main_box.add(Box.createRigidArea(new Dimension(0,5)));
    main_box.add(misc_box);

    validate();
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == clear_prefsB) {
      // The option pane used differs from the confirmDialog only in
		 // that "No" is the default choice.
		 String[] options = {"Yes", "No"};
		 if (JOptionPane.YES_OPTION == JOptionPane.showOptionDialog(
						 this, "Really reset all preferences to defaults?\n(this will also exit the application)", "Clear preferences?",
						 JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
						 options, options[1])) {

			 try {
				 PreferenceUtils.clearPreferences();
				 System.exit(0);
			 } catch (Exception e) {
				 ErrorHandler.errorPanel("ERROR", "Error clearing preferences", e);
			 }
		 }
    }
  }

  private static JPanel addColorChooser(String label_str, String pref_name, Color default_color) {
		JComponent component = ColorUtils.createColorComboBox(PreferenceUtils.getTopNode(), pref_name, default_color);
		return addToPanel(label_str, component);
	}

  private static JPanel addToPanel(String label_str, JComponent component) {
		JPanel panel = new JPanel();
		panel.setLayout(new GridLayout(1, 2));
		JPanel inner_panel = new JPanel();

		inner_panel.add(component);
		panel.add(new JLabel(label_str + ": "));
		panel.add(inner_panel);

		return panel;
	}

  public void refresh() {
  }

}
