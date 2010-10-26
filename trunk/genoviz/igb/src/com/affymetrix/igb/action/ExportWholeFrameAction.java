package com.affymetrix.igb.action;

import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.util.ComponentWriter;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ExportWholeFrameAction.java 5916 2010-05-10 20:57:27Z sgblanch $
 */
public class ExportWholeFrameAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public ExportWholeFrameAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("wholeFrame")));
	}

	public void actionPerformed(ActionEvent e) {
		try {
			ComponentWriter.showExportDialog(IGB.getSingleton().getFrame());
		} catch (Exception ex) {
			ErrorHandler.errorPanel("Problem during output.", ex);
		}
	}
}
