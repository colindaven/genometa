package com.affymetrix.igb.action;


import com.affymetrix.igb.view.SequenceViewer;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ExitAction.java 5804 2010-04-28 18:54:46Z sgblanch $
 */
public class ExportFastaSequenceAction extends AbstractAction {
	private static final long serialVersionUID = 1l;
	SequenceViewer sv;
	public ExportFastaSequenceAction(SequenceViewer sv) {
		super(BUNDLE.getString("fastasequence"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_S);
		this.sv=sv;
	}

	public void actionPerformed(ActionEvent e) {
		sv.exportSequenceFasta();
	}
}
