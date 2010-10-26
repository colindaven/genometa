package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ExitAction.java 5804 2010-04-28 18:54:46Z sgblanch $
 */
public class ExitAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ExitAction() {
		super(BUNDLE.getString("exit"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_X);
	}

	public void actionPerformed(ActionEvent e) {
		Toolkit.getDefaultToolkit().getSystemEventQueue().postEvent(
				new WindowEvent(
					IGB.getSingleton().getFrame(),
					WindowEvent.WINDOW_CLOSING));
	}
}
