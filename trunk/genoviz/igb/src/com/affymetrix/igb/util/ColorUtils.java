package com.affymetrix.igb.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JColorChooser;
import javax.swing.JComponent;

import com.affymetrix.genometryImpl.util.PreferenceUtils;

import com.jidesoft.combobox.ColorComboBox;

/**
 *
 * @version $Id: ColorUtils.java 7018 2010-10-12 19:48:32Z hiralv $
 */
public class ColorUtils {

	private static JColorChooser singleton = new JColorChooser();
	
	/**
	 *  Creates a Color chooser combo box associated with a Color preference.
	 *  Will initialize itself with the value of the given
	 *  preference and will update itself, via a PreferenceChangeListener,
	 *  if the preference value changes.
	 * 
	 * @param node
	 * @param pref_name 
	 * @param default_val 
	 * @return
	 */
	public static JComponent createColorComboBox(final Preferences node,
			final String pref_name, final Color default_val){
		Color initial_color = PreferenceUtils.getColor(node, pref_name, default_val);
		final ColorComboBox combobox = new ColorComboBox();
		combobox.setSelectedColor(initial_color);
		combobox.setColorValueVisible(false);
		combobox.setCrossBackGroundStyle(false);
		combobox.setButtonVisible(false);
		combobox.setStretchToFit(true);
		combobox.setMaximumSize(new Dimension(150,20));
		combobox.addItemListener(new ItemListener() {

			public void itemStateChanged(ItemEvent e) {
				Color c = combobox.getSelectedColor();
				if (c != null) {
					PreferenceUtils.putColor(node, pref_name, c);
				}else{
					combobox.setSelectedColor(PreferenceUtils.getColor(node, pref_name, default_val));
				}
			}
		});

		node.addPreferenceChangeListener(new PreferenceChangeListener() {

			public void preferenceChange(PreferenceChangeEvent evt) {
				if (evt.getNode().equals(node) && evt.getKey().equals(pref_name)) {
					Color c = PreferenceUtils.getColor(node, pref_name, default_val);
					combobox.setSelectedColor(c);
					combobox.repaint();
				}
			}
		});
		

		return combobox;
	}
}
