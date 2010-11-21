package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ReportBugAction.java 5772 2010-04-26 19:51:15Z sgblanch $
 */
public class ReportBugAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ReportBugAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("reportABug")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_R);
	}

	public void actionPerformed(ActionEvent e) {
		GeneralUtils.browse("http://sourceforge.net/tracker/?group_id=129420&atid=714744");
	}
}
