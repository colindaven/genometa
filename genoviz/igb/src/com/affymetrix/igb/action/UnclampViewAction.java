package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: UnclampViewAction.java 5816 2010-04-29 14:28:29Z sgblanch $
 */
public class UnclampViewAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public UnclampViewAction() {
		super(BUNDLE.getString("unclamp"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_U);
	}

	public void actionPerformed(ActionEvent e) {
		IGB.getSingleton().getMapView().unclamp();
	}

}
