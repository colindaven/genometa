/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.MenuUtil;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;

/**
 *
 * @author wieding-
 */
public class ExportOverviewDiagramToCsvAction extends AbstractAction {

	public ExportOverviewDiagramToCsvAction() {
		super(MessageFormat.format(
				"Export Diagram to CSV",
				"Export Diagram to CSV",
				MenuUtil.getIcon("toolbarButtonGraphics/development/Host16.gif")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_C);
	}

	public void actionPerformed(ActionEvent ae) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
