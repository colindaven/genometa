package com.affymetrix.igb.view.external;

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
 * View for ENSEMBL annotation
 *
 * @author Ido M. Tamir
 */
public class EnsemblView extends BrowserView {

	public static final String viewName = "Ensembl";
	private static final String ENSEMBLSETTINGS = "ensemblSettings";
	public static final String ENSEMBLSESSION = "ENSEMBL_WWW_SESSION";
	public static final String ENSEMBLWIDTH = "ENSEMBL_WIDTH";

	/**
	 *
	 * @param selector selects foreground
	 */
	public EnsemblView(JComboBox selector) {
		super(selector);
	}

	@Override
	public JDialog getViewHelper(Window window) {
		return new ENSEMBLHelper(window, "Customize Ensembl settings");
	}

	@Override
	public void initializeCookies() {
		final Preferences ucscSettingsNode = PreferenceUtils.getTopNode().node(ENSEMBLSETTINGS);
		String userId = ucscSettingsNode.get(ENSEMBLSESSION, "");
		setCookie(ENSEMBLSESSION, userId);
	}

	@Override
	public Image getImage(Loc loc, int pixWidth) {
		Map<String, String> cookies = new HashMap<String, String>();
		cookies.put(ENSEMBLSESSION, getCookie(ENSEMBLSESSION));
		cookies.put(ENSEMBLWIDTH, Integer.toString(pixWidth));
		return new ENSEMBLoader().getImage(loc, pixWidth, cookies).image;
	}

	@Override
	public String getViewName() {
		return viewName;
	}

	/**
	 * Panel for ENSEMBL Settings: cookie selection
	 *
	 *
	 **/
	class ENSEMBLHelper extends JDialog {

		private final JButton okButton = new JButton("submit");
		private final JTextField userIdField = new JTextField(getCookie(ENSEMBLSESSION), 50);

		public ENSEMBLHelper(Window window, String string) {
			super(window, string);
			CookieHandler.setDefault(null);

			this.setLayout(new BorderLayout());
			final JTextPane pane = new JTextPane();
			pane.setContentType("text/html");

			String text = "<h1>Setting the ENSEMBL cookie</h1><p>With the ENSEMBL cookie value you can synchronize the Viewer settings with your browser.</p>";
			text += "<p>ENSEMBL puts a cookie into your browser called ENSEMBL_WWW_SESSION.</p><p>You have to put its value into the textfield.</p>";
			pane.setText(text);
			pane.setEditable(false);
			final JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));
			panel.add(Box.createHorizontalGlue());
			panel.add(Box.createHorizontalStrut(5));
			panel.add(new JLabel("ENSEMBL cookie: (" + ENSEMBLSESSION + "):"));
			panel.add(Box.createHorizontalStrut(5));
			panel.add(userIdField);
			panel.add(Box.createHorizontalStrut(5));
			panel.add(Box.createHorizontalGlue());
			panel.add(okButton);

			okButton.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent e) {
					String userId = userIdField.getText();
					setCookie(ENSEMBLSESSION, userId);
					Preferences ucscSettingsNode = PreferenceUtils.getTopNode().node(ENSEMBLSESSION);
					ucscSettingsNode.put(ENSEMBLSESSION, userId);
					dispose();
				}
			});
			okButton.setToolTipText("Set your ENSEMBL id for the session");

			getContentPane().add("Center", pane);
			getContentPane().add("South", panel);
		}
	}
}
