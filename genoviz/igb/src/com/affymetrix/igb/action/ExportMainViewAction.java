package com.affymetrix.igb.action;

import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.ComponentWriter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ExportMainViewAction.java 5916 2010-05-10 20:57:27Z sgblanch $
 */
public class ExportMainViewAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ExportMainViewAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("mainView")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_M);
	}

	public void actionPerformed(ActionEvent e) {
		try {
			ComponentWriter.showExportDialog(IGB.getSingleton().getMapView().getSeqMap().getNeoCanvas());
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}
}
