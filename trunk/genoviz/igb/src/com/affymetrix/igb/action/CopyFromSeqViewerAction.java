package com.affymetrix.igb.action;

import com.affymetrix.igb.view.SequenceViewer;
import com.affymetrix.genometryImpl.util.MenuUtil;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: CopyResiduesAction.java 7155 2010-11-10 20:54:57Z vbishnoi $
 */
public class CopyFromSeqViewerAction extends AbstractAction {

	private static final long serialVersionUID = 1l;
	SequenceViewer sv;
	public CopyFromSeqViewerAction(SequenceViewer sv) {
		super(BUNDLE.getString("copySelectedResiduesToClipboard"),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Copy16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_C);
		this.sv=sv;
	}

	public void actionPerformed(ActionEvent e) {
		sv.copyAction();
	}
}
