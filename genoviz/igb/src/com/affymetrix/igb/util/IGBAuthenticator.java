package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.StringUtils;
import com.affymetrix.igb.general.ServerList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;

/**
 * An Authenticator class for IGB.  It is designed to make it easier for a
 * user to authenticate to a server as well as letting a user use a server
 * anonymously.
 *
 * TODO:
 *  - detect when a login fails
 *  - detect difference between optional and required authentication
 *  - use this class to authenticate old-style genoviz DAS2 login
 *  - integrate this class with Server Preferences
 *  - transition away from using guest:guest for authentication
 *
 * @author sgblanch
 * @version $Id: IGBAuthenticator.java 5852 2010-05-04 18:22:56Z jnicol $
 */
public class IGBAuthenticator extends Authenticator {
	private static enum AuthType { ASK, ANONYMOUS, AUTHENTICATE };

	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle("igb");
	
	private static final String[] OPTIONS = { BUNDLE.getString("login"), BUNDLE.getString("cancel") };
	private static final String GUEST = "guest";
	private static final String PREF_AUTH_TYPE = "authentication type";
	private static final String PREF_REMEMBER = "remember authentication";

	private final JFrame parent;

	public IGBAuthenticator(JFrame parent) {
		this.parent = parent;
	}

	/**
	 * Constructs the dialog that is presented to the user when IGB recieves an
	 * authentication request from a server.
	 */
	private static JPanel buildDialog(
			final JPanel messageContainer,
			final JRadioButton anon,
			final JRadioButton auth,
			final JLabel server,
			final JTextField username,
			final JPasswordField password,
			final JCheckBox remember,
			final boolean authOptional) {
		JPanel dialog = new JPanel();
		JLabel s = new JLabel(BUNDLE.getString("server"));
		final JLabel u = new JLabel(BUNDLE.getString("username"));
		final JLabel p = new JLabel(BUNDLE.getString("password"));
		ButtonGroup group = new ButtonGroup();
		GroupLayout layout = new GroupLayout(dialog);

		dialog.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.linkSize(SwingConstants.HORIZONTAL, s, u, p);

		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(messageContainer)
				.addComponent(anon)
				.addComponent(auth)
				.addGroup(layout.createSequentialGroup()
					.addComponent(s)
					.addComponent(server))
				.addGroup(layout.createSequentialGroup()
					.addComponent(u)
					.addComponent(username))
				.addGroup(layout.createSequentialGroup()
					.addComponent(p)
					.addComponent(password))
				.addComponent(remember));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(messageContainer)
				.addComponent(anon)
				.addComponent(auth)
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(s)
					.addComponent(server))
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(u)
					.addComponent(username))
				.addGroup(layout.createParallelGroup(Alignment.BASELINE)
					.addComponent(p)
					.addComponent(password))
				.addComponent(remember));

		ActionListener radioListener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				u.setEnabled(auth.isSelected());
				p.setEnabled(auth.isSelected());
				username.setEnabled(auth.isSelected());
				password.setEnabled(auth.isSelected());

				if (anon.isSelected()) {
					remember.setText(BUNDLE.getString("alwaysAnonymous"));
				} else {
					remember.setText(BUNDLE.getString("savePassword"));
				}
			}
		};

		group.add(anon);
		group.add(auth);
		anon.addActionListener(radioListener);
		auth.addActionListener(radioListener);
		anon.setSelected(authOptional);
		auth.setSelected(!authOptional);
		radioListener.actionPerformed(null);

		return dialog;
	}

	/**
	 * Request credentials to authenticate to the server.  First consults the
	 * preferences and then prompts the user.
	 * 
	 * @return a PasswordAuthentication to use against the server
	 */
	@Override
	public PasswordAuthentication getPasswordAuthentication() {
		String url = this.getRequestingURL().toString();
		Preferences serverNode = null;
		AuthType authType = AuthType.ASK;
		String userFromPrefs = "";
		String passFromPrefs = "";
		GenericServer serverObject = null;

		try {
			serverObject = ServerList.getServer(this.getRequestingURL());
		} catch (URISyntaxException ex) {
			Logger.getLogger(IGBAuthenticator.class.getName()).log(Level.SEVERE, "Problem translating URL '" + this.getRequestingURL().toString() + "' to server", ex);
		} catch (IllegalArgumentException ex) {
			Logger.getLogger(IGBAuthenticator.class.getName()).log(Level.WARNING, "URL " +  this.getRequestingURL() + " was not in server list.");
		}

		if (serverObject != null) {
			url = serverObject.URL;
			serverNode = PreferenceUtils.getServersNode().node(GeneralUtils.URLEncode(url));
			authType = AuthType.valueOf(serverNode.get(PREF_AUTH_TYPE, AuthType.ASK.toString()));
			if (serverObject.getLogin() != null) {
				userFromPrefs = serverObject.getLogin();
			}
			if (serverObject.getPassword() != null) {
				passFromPrefs = serverObject.getPassword();
			}
		}

		

		if (authType == AuthType.AUTHENTICATE && !userFromPrefs.equals("") && !passFromPrefs.equals("")) {
			return new PasswordAuthentication(userFromPrefs, passFromPrefs.toCharArray());
		} else if (authType == AuthType.ANONYMOUS) {
			return doAnonymous();
		} else {
			return displayDialog(parent, serverNode, serverObject, url);
		}
	}

	/**
	 * Returns 'anonymous' credentials for authenticating against a genopub
	 * server.
	 *
	 * @return a PasswordAuthentication with the username and password set to 'guest'
	 */
	private static PasswordAuthentication doAnonymous() {
		return new PasswordAuthentication(GUEST, GUEST.toCharArray());
	}

	/**
	 * Prompt the user on how to authenticate to the server.
	 * 
	 * @param serverNode
	 * @param serverObject
	 * @param url
	 * @return Password authentication to the user
	 */
	private static PasswordAuthentication displayDialog(final JFrame parent, final Preferences serverNode, final GenericServer serverObject, final String url) {
		boolean authOptional = serverObject != null && serverObject.serverType == ServerType.DAS2;
		JPanel messageContainer = serverObject == null ? new JPanel() : setMessage(serverObject.serverName, authOptional);
		JLabel server = new JLabel();
		JTextField     username = new JTextField();
		JPasswordField password = new JPasswordField();
		JRadioButton anon = new JRadioButton(BUNDLE.getString("useAnonymousLogin"));
		JRadioButton auth = new JRadioButton(BUNDLE.getString("authToServer"));
		JCheckBox remember = new JCheckBox();
		JPanel dialog = buildDialog(messageContainer, anon, auth, server, username, password, remember, authOptional);

		server.setText(url);
		anon.setSelected(authOptional);
		anon.setEnabled(authOptional);
		auth.setSelected(!authOptional);
		remember.setEnabled(serverObject != null && serverNode != null && serverNode.parent().getBoolean(PREF_REMEMBER, true));

		int result = JOptionPane.showOptionDialog(parent, dialog, null, OK_CANCEL_OPTION, PLAIN_MESSAGE, null, OPTIONS, OPTIONS[0]);

		if (result == OK_OPTION) {
			if (remember.isSelected()) {
				savePreferences(
						serverNode,
						serverObject,
						username.getText(),
						password.getPassword(),
						anon.isSelected(),
						remember.isSelected());
			}

			if (auth.isSelected()) {
				return new PasswordAuthentication(username.getText(), password.getPassword());
			} else {
				return doAnonymous();
			}
		}

		/* User cancelled or quit login prompt */
		/*
		 * We really want to return null here, but there is a bug in
		 * Das2ServerInfo: getSources() will call initialize() every time
		 * if the login() fails.  Currently, this occurs 4 times on startup.
		 */
		return authOptional ? doAnonymous() : null;
	}

	/**
	 * Formats and word wraps the message of the authentication dialog.
	 * 
	 * @param serverObject friendly name of the server that requested authentication
	 * @return a JPanel containing the message
	 */
	private static JPanel setMessage(String serverName, boolean authOptional) {
		JPanel messageContainer = new JPanel();
		/* instantiante current simply to steal FontMetrics from it */
		JLabel current = new JLabel();
		String[] message = StringUtils.wrap(
				MessageFormat.format(
					BUNDLE.getString(authOptional ?  "authOptional" : "authRequired"),
					serverName),
				current.getFontMetrics(current.getFont()),
				500);

		messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));

		for (String line : message) {
			current = new JLabel(line);
			messageContainer.add(current);
		}

		return messageContainer;
	}

	/**
	 * Writes the user's choices out to the preferences.
	 * 
	 * @param serverNode the preferences node for this server
	 * @param serverObject the GenericServer object for this server
	 */
	private static void savePreferences(
			Preferences serverNode,
			GenericServer serverObject,
			String username,
			char[] password,
			boolean anon,
			boolean remember) {

		if (serverNode == null || serverObject == null) {
			return;
		}
		
		AuthType authType = anon ? AuthType.ANONYMOUS : AuthType.AUTHENTICATE;
		serverNode.put(PREF_AUTH_TYPE, authType.toString());
		serverNode.parent().putBoolean(PREF_REMEMBER, remember);
		if (authType == authType.AUTHENTICATE) {
			serverObject.setLogin(username);
			serverObject.setPassword(new String(password));
		}
	}

	public static void resetAuth(String url) {
		Preferences serverNode = PreferenceUtils.getServersNode().node(GeneralUtils.URLEncode(url));
		serverNode.put(PREF_AUTH_TYPE, AuthType.ASK.toString());
	}
}
