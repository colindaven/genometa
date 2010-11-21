package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.util.MergeOptionChooser;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author jnicol
 */
public final class LoadURLAction extends AbstractAction {
	private static final long serialVersionUID = 1l;
	private static final MergeOptionChooser chooser = new MergeOptionChooser();
	private final JFrame gviewerFrame;
	private final Box mergeOptionBox = chooser.box;
	private JDialog dialog = null;

	public LoadURLAction(JFrame gviewerFrame) {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("openURL")),
					MenuUtil.getIcon("toolbarButtonGraphics/general/Open16.gif"));
		this.gviewerFrame = gviewerFrame;
	}

	public void actionPerformed(ActionEvent e) {
		loadURL();
	}

	private void loadURL() {
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		JOptionPane pane = new JOptionPane("Enter URL", JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION );
		pane.setWantsInput(true);

		chooser.refreshSpeciesList();
		dialog = pane.createDialog(gviewerFrame, BUNDLE.getString("openURL"));
		dialog.setModal(true);
		dialog.getContentPane().add(mergeOptionBox, BorderLayout.SOUTH);
		dialog.pack();
		dialog.setLocationRelativeTo(gviewerFrame);
		dialog.setVisible(true);

		String urlStr = (String)pane.getInputValue();
		if(urlStr == null || JOptionPane.UNINITIALIZED_VALUE.equals(urlStr)){
			return;
		}
		urlStr = urlStr.trim();
		URL url;
		URI uri;
		try {
			url = new URL(urlStr);
			uri = url.toURI();
		} catch (Exception ex) {
			// verify these are valid
			ErrorHandler.errorPanel("Invalid URL", "The URL " + urlStr + " is not valid.  Please enter a valid URL");
			return;
		}
			
		final AnnotatedSeqGroup loadGroup = gmodel.addSeqGroup((String)chooser.versionCB.getSelectedItem());

		final boolean mergeSelected = loadGroup == gmodel.getSelectedSeqGroup();

		LoadFileAction.openURI(uri, getFriendlyName(urlStr), mergeSelected, loadGroup, (String)chooser.speciesCB.getSelectedItem());
		
	}

	private static String getFriendlyName(String urlStr) {
		// strip off final "/" character, if it exists.
		if (urlStr.endsWith("/")) {
			urlStr = urlStr.substring(0,urlStr.length()-1);
		}

		//strip off all earlier slashes.
		urlStr = urlStr.substring(urlStr.lastIndexOf('/')+1);

		return urlStr;
	}
}
