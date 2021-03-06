package com.affymetrix.igb.action;

import com.affymetrix.igb.view.SequenceViewer;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.igb.IGB;
import com.affymetrix.genometryImpl.util.MenuUtil;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: CopyResiduesAction.java 6324 2010-07-01 20:11:30Z hiralv $
 */
public class CopyResiduesAction extends AbstractAction {

	private static final long serialVersionUID = 1l;

	public CopyResiduesAction() {
		super(BUNDLE.getString("copySelectedResiduesToClipboard"),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Copy16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_C);
	}

	public void actionPerformed(ActionEvent e) {
		SeqSymmetry residues_sym = IGB.getSingleton().getMapView().copySelectedResidues(true);
		if (residues_sym != null) {
			SequenceViewer neoSeqDemo = new SequenceViewer();
			neoSeqDemo.tempChange(residues_sym);
		}
	}
}
