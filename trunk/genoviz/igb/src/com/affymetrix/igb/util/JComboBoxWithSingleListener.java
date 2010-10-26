package com.affymetrix.igb.util;

import java.awt.event.ItemListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JComboBox;

/**
 * Only allow one listener to be added to the combo box.  Otherwise potentially many events can be fired with one change.
 */
public class JComboBoxWithSingleListener extends JComboBox {

	/**
	 * Default implementation of addListener permits the same class
	 * to be added as a listener multiple times, causing it to be
	 * notified of an event multiple times.
	 *
	 * This is a quick kludge to prevent a listener from being added
	 * multiple times.  Hopefully this can be removed once we
	 * sort out adding and removing ItemListeners.
	 */
	@Override
	public void addItemListener(ItemListener aListener) {
		for (ItemListener listener : this.getItemListeners()) {
			if (listener == aListener) {
				Logger.getLogger(this.getClass().getName()).log(Level.FINE, "Attempt to add duplicate ItemListener, ignoring");
				return;
			}
		}
		super.addItemListener(aListener);
	}
}
