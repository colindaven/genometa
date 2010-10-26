package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ClearGraphsAction.java 5789 2010-04-27 19:47:03Z sgblanch $
 */
public class ClearGraphsAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ClearGraphsAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("clearGraphs")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_L);
	}

	public void actionPerformed(ActionEvent e) {
		if (IGB.confirmPanel("Really clear graphs?")) {
			IGB.getSingleton().getMapView().clearGraphs();
		}
	}
}
