/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.util.aligner.BowtieAlignerExecutor;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;

/**
 *
 * @author Elmo
 */
public class BowtieAlignerExecutionAction extends AbstractAction {

	public BowtieAlignerExecutionAction() {
		super(MessageFormat.format(
				"Run bowtie Aligner",
				"Run bowtie Aligner from an existing index",
				MenuUtil.getIcon("toolbarButtonGraphics/development/Host16.gif")));
//		this.putValue(MNEMONIC_KEY, KeyEvent.VK_C);
	}

	public void actionPerformed(ActionEvent e) {
		new BowtieAlignerExecutor();
	}



}
