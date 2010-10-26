package com.affymetrix.igb.action;

import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.tiers.AffyLabelledTierMap;
import com.affymetrix.igb.util.ComponentWriter;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ExportLabelledMainViewAction.java 5916 2010-05-10 20:57:27Z sgblanch $
 */
public class ExportLabelledMainViewAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ExportLabelledMainViewAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("mainViewWithLabels")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_L);
	}

	public void actionPerformed(ActionEvent e) {
		try {
			AffyLabelledTierMap tm = (AffyLabelledTierMap) IGB.getSingleton().getMapView().getSeqMap();
			ComponentWriter.showExportDialog(tm.getSplitPane());
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}
}
