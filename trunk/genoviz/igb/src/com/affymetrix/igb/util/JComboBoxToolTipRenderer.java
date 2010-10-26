/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util;

import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JList;
import javax.swing.plaf.basic.BasicComboBoxRenderer;

/**
 * Allows tooltips for individual entries in combo box.
 * @author jnicol
 */
public final class JComboBoxToolTipRenderer extends BasicComboBoxRenderer {

	private final Map<Object,String> toolTipMap = new HashMap<Object,String>();

	public void setToolTipEntry(Object text, String toolTip) {
		toolTipMap.put(text, toolTip);
	}

	@Override
	public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus) {
		if (isSelected) {
			setBackground(list.getSelectionBackground());
			setForeground(list.getSelectionForeground());
			if (value != null && toolTipMap.containsKey(value)) {
				list.setToolTipText(toolTipMap.get(value));
			} else {
				list.setToolTipText("");
			}
		} else {
			setBackground(list.getBackground());
			setForeground(list.getForeground());
		}
		setFont(list.getFont());
		setText((value == null) ? "" : value.toString());
		return this;
	}
}
