package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.ColorIcon;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;

/**
 *
 * @version $Id: ColorUtils.java 7018 2010-10-12 19:48:32Z hiralv $
 */
public class ColorUtils {

	private static JColorChooser singleton = new JColorChooser();
	
	/**
	 *  Creates a JButton associated with a Color preference.
	 *  Will initialize itself with the value of the given
	 *  preference and will update itself, via a PreferenceChangeListener,
	 *  if the preference value changes.
	 * 
	 * @param node
	 * @param pref_name 
	 * @param default_val 
	 * @return
	 */
	public static JButton createColorButton(final Preferences node,
			final String pref_name, final Color default_val) {

		Color initial_color = PreferenceUtils.getColor(node, pref_name, default_val);
		final ColorIcon icon = new ColorIcon(11, initial_color);
		final String panel_title = "Choose a color";

		final ActionListener ok = new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				Color c = singleton.getColor();
				if (c != null) {
					PreferenceUtils.putColor(node, pref_name, c);
				}
			}
		};
		
		final JButton button = new JButton(icon);
		button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent ae) {

				JDialog dialog = JColorChooser.createDialog(button, panel_title, true, singleton, ok, null);
				singleton.setColor(PreferenceUtils.getColor(node, pref_name, default_val));

				dialog.setVisible(true);
			}
		});

		node.addPreferenceChangeListener(new PreferenceChangeListener() {

			public void preferenceChange(PreferenceChangeEvent evt) {
				if (evt.getNode().equals(node) && evt.getKey().equals(pref_name)) {
					Color c = PreferenceUtils.getColor(node, pref_name, default_val);
					icon.setColor(c);
					button.repaint();
				}
			}
		});
		return button;
	}
}
