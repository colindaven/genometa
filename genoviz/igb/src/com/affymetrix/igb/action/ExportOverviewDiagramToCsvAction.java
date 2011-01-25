/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.view.BarGraphMap;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.JFileChooser;

/**
 *
 * @author wieding-
 */
public class ExportOverviewDiagramToCsvAction extends AbstractAction {

	public ExportOverviewDiagramToCsvAction(Component par, BarGraphMap bgm) {
		super(MessageFormat.format(
				"Export Data as CSV",
				"Export Data as CSV",
				MenuUtil.getIcon("toolbarButtonGraphics/development/Host16.gif")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_E);
		this.parent = par;
		this.bgm = bgm;
	}

	public void actionPerformed(ActionEvent ae) {
		JFileChooser fc = new JFileChooser();

		int retval = fc.showSaveDialog(this.parent);
		if (retval == JFileChooser.APPROVE_OPTION) {
			bgm.writeCsvFile(fc.getSelectedFile().getAbsolutePath());
		}
	}
	private BarGraphMap bgm;
	private Component parent;
}
