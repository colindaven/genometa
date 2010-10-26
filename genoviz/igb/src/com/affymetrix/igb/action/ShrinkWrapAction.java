package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ShrinkWrapAction.java 5900 2010-05-10 14:33:25Z sgblanch $
 */
public class ShrinkWrapAction extends AbstractAction {
	private static final long serialVersionUID = 1;
	private static final ShrinkWrapAction ACTION = new ShrinkWrapAction();

	private ShrinkWrapAction() {
		super(BUNDLE.getString("toggleShrinkWrapping"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_S);
		this.putValue(SELECTED_KEY, IGB.getSingleton().getMapView().getShrinkWrap());
	}

	public static ShrinkWrapAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		SeqMapView map_view = IGB.getSingleton().getMapView();
		map_view.setShrinkWrap(!map_view.getShrinkWrap());
	}

}
