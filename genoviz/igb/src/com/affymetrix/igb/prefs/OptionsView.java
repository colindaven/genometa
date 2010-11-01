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
import com.affymetrix.igb.tiers.AxisStyle;
import com.affymetrix.igb.view.OrfAnalyzer;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.UnibrowHairline;

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

    

    misc_box.add(Box.createRigidArea(new Dimension(0,5)));

    misc_box.add(clear_prefsB);
    clear_prefsB.addActionListener(this);



    misc_box.add(Box.createRigidArea(new Dimension(0,5)));
	

    JPanel orf_box = new JPanel();
    orf_box.setLayout(new GridLayout(2,2));
    orf_box.setBorder(new javax.swing.border.TitledBorder("ORF Analyzer"));

    JButton stop_codon_color = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), OrfAnalyzer.PREF_STOP_CODON_COLOR, OrfAnalyzer.default_stop_codon_color);
    orf_box.add(new JLabel("Stop Codon: "));
    orf_box.add(stop_codon_color);
    JButton dynamic_orf_color = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), OrfAnalyzer.PREF_DYNAMIC_ORF_COLOR, OrfAnalyzer.default_dynamic_orf_color);
    orf_box.add(new JLabel("Dynamic ORF: "));
    orf_box.add(dynamic_orf_color);
	
	JPanel base_box = new JPanel();
    base_box.setLayout(new GridLayout(5,2));
    base_box.setBorder(new javax.swing.border.TitledBorder("Change Residue Colors"));

    JButton A_color = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), CharSeqGlyph.PREF_A_COLOR, CharSeqGlyph.default_A_color);
	base_box.add(new JLabel("A: "));
	base_box.add(A_color);
	JButton T_color = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), CharSeqGlyph.PREF_T_COLOR, CharSeqGlyph.default_T_color);
	base_box.add(new JLabel("T: "));
	base_box.add(T_color);
	JButton G_color = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), CharSeqGlyph.PREF_G_COLOR, CharSeqGlyph.default_G_color);
	base_box.add(new JLabel("G: "));
	base_box.add(G_color);
	JButton C_color = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), CharSeqGlyph.PREF_C_COLOR, CharSeqGlyph.default_C_color);
    base_box.add(new JLabel("C: "));
    base_box.add(C_color);
	JButton OTHER_color = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), CharSeqGlyph.PREF_OTHER_COLOR, CharSeqGlyph.default_other_color);
    base_box.add(new JLabel("Other: "));
    base_box.add(OTHER_color);
    


    JPanel axis_box = new JPanel();
    axis_box.setLayout(new GridLayout(3,2));
    axis_box.setBorder(new javax.swing.border.TitledBorder("Axis"));

    JButton axis_color_button2 = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), AxisStyle.PREF_AXIS_COLOR, Color.BLACK);
    axis_box.add(new JLabel("Foreground: "));
    axis_box.add(axis_color_button2);

    JButton axis_back_color = ColorUtils.createColorButton(PreferenceUtils.getTopNode(), AxisStyle.PREF_AXIS_BACKGROUND, Color.WHITE);
    axis_box.add(new JLabel("Background: "));
    axis_box.add(axis_back_color);

    axis_box.add(new JLabel("Number format: "));
    String default_label_format = SeqMapView.VALUE_AXIS_LABEL_FORMAT_COMMA;
    String[] label_format_options = new String[] {SeqMapView.VALUE_AXIS_LABEL_FORMAT_FULL,
                                                  SeqMapView.VALUE_AXIS_LABEL_FORMAT_COMMA,
                                                  SeqMapView.VALUE_AXIS_LABEL_FORMAT_ABBREV};
    JComboBox axis_label_format_CB = PreferenceUtils.createComboBox(PreferenceUtils.getTopNode(), "Axis label format", label_format_options, default_label_format);
    axis_box.add(axis_label_format_CB);

    axis_box.setAlignmentX(0.0f);
   
    orf_box.setAlignmentX(0.0f);
    misc_box.setAlignmentX(0.0f);
	base_box.setAlignmentX(0.0f);

   
    main_box.add(axis_box);
   
    main_box.add(orf_box);
	main_box.add(base_box);
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

  public void refresh() {
  }

}