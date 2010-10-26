package com.affymetrix.igb.view.external;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.CookieHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.prefs.Preferences;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JTextPane;

/**
 * Subclass for UCSC view
 *
 * @author Ido M. Tamir
 */
public class UCSCView extends BrowserView {

	public static final String viewName = "UCSC";
	private static final String UCSCSETTINGSNODE = "ucscSettings";
	public static final String UCSCUSERID = "hguid";

	/**
	 *
	 * @param selector for selection foreground
	 */
	public UCSCView(JComboBox selector) {
		super(selector);
	}

	@Override
	public JDialog getViewHelper(Window window) {
		return new UCSCHelper(window, "Customize UCSC settings");
	}

	@Override
	public void initializeCookies() {
		final Preferences ucscSettingsNode = PreferenceUtils.getTopNode().node(UCSCSETTINGSNODE);
		String userId = ucscSettingsNode.get(UCSCUSERID, "");
		setCookie(UCSCUSERID, userId);
	}

	@Override
	public Image getImage(Loc loc, int pixWidth) {
		Map<String, String> cookies = new HashMap<String, String>();
		cookies.put(UCSCUSERID, getCookie(UCSCUSERID));
		return new UCSCLoader().getImage(loc, pixWidth, cookies).image;
	}

	@Override
	public String getViewName() {
		return viewName;
	}

	/**
	 * Panel for UCSC Settings: hguid selection
	 * Shows the UCSC plot for the current region.
	 *
	 **/
	public class UCSCHelper extends JDialog {

		private final JButton okButton = new JButton("submit");
		private final JButton ucscInfo = new JButton("UCSC info");
		private final JTextField userIdField = new JTextField(getCookie(UCSCUSERID), 15);

		public UCSCHelper(Window window, String string) {
			super(window, string);
			CookieHandler.setDefault(null);

			this.setLayout(new BorderLayout());
			final JTextPane pane = new JTextPane();
			pane.setContentType("text/html");

			String text = "<h1>Setting the UCSC user id</h1><p>Using the UCSC user id you can customize the UCSC Viewer settings with your browser.</p>";
			text += "<ol><li><p>Obtain your user id by clicking on the \"UCSC info\" button.</p><p>Or open <a href=\"http://genome.ucsc.edu/cgi-bin/cartDump\">http://genome.ucsc.edu/cgi-bin/cartDump</a> in your browser</p></li>";
			text += "<li>Then scroll down in the opened window and copy the value of hguid into the \"UCSC user id\" field.</li>";
			text += "<li>Click the submit button.</li>";
			text += "<li>Your IGB UCSC View is now synchronized with your browser track configuration.</br>";
			text += "The settings in your browser now change the view.</li></ol>";
			pane.setText(text);
			pane.setEditable(false);
			final JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			panel.add(ucscInfo);
			panel.add(Box.createHorizontalGlue());
			panel.add(Box.createHorizontalStrut(5));
			panel.add(new JLabel("UCSC user id (hguid):"));
			panel.add(Box.createHorizontalStrut(5));
			panel.add(userIdField);
			panel.add(Box.createHorizontalStrut(5));
			panel.add(Box.createHorizontalGlue());
			panel.add(okButton);

			okButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					String userId = userIdField.getText();
					setCookie(UCSCUSERID, userId);
					Preferences ucscSettingsNode = PreferenceUtils.getTopNode().node(UCSCSETTINGSNODE);
					ucscSettingsNode.put(UCSCUSERID, userId);
					dispose();
				}
			});
			okButton.setToolTipText("Set your UCSC id for the session");
			ucscInfo.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					GeneralUtils.browse("http://genome.ucsc.edu/cgi-bin/cartDump");
				}
			});
			ucscInfo.setToolTipText("<html>Opens the browser with the UCSC user info from cookie.</br>Type the number at the bottom of the screen</br>where it says \"hguid=...\" into the text box and click submit.</html>");
			getContentPane().add("Center", pane);
			getContentPane().add("South", panel);
		}
	}
}
