package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.parsers.XmlPrefsParser;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;

/**
 *
 * @author jnicol1
 * @version $Id$
 */
public abstract class PrefsLoader {

	private static final int CURRENT_PREF_VERSION = 2;
	private static boolean prefsLoaded = false;
	private static final String user_dir = System.getProperty("user.dir");
	private static final String user_home = System.getProperty("user.home");
	/**
	 *  We no longer distribute a file called "igb_prefs.xml".
	 *  Instead there is a default prefs file hidden inside the igb.jar file, and
	 *  this is augmented by a web-based prefs file.
	 *  But, we still will load a file called "igb_prefs.xml" if it exists in
	 *  the user's home directory, since they may have put some personal modifications
	 *  there.
	 */
	private static final String DEFAULT_PREFS_FILENAME = "igb_prefs.xml";
	static String default_user_prefs_files =
			(new File(user_home, DEFAULT_PREFS_FILENAME)).getAbsolutePath()
			+ ";"
			+ (new File(user_dir, DEFAULT_PREFS_FILENAME)).getAbsolutePath();

	/**
	 *  Returns IGB prefs hash
	 *  If prefs haven't been loaded yet, will force loading of prefs
	 */
	public static void loadIGBPrefs(String[] main_args) {
		checkPrefsVersion();

		if (prefsLoaded) {
			return;
		}

		String def_prefs_url = get_default_prefs_url(main_args);
		String[] prefs_list = get_prefs_list(main_args);

		LoadDefaultPrefsFromJar();
		LoadDefaultAPIPrefsFromJar();
		LoadWebPrefs(def_prefs_url);
		LoadFileOrURLPrefs(prefs_list);
		ServerList.loadServerPrefs();

		prefsLoaded = true;
	}

	private static void LoadDefaultPrefsFromJar() {
		/**  first load default prefs from jar (with XmlPrefsParser)*/
		InputStream default_prefs_stream = null;
		try {
			default_prefs_stream = IGB.class.getResourceAsStream(IGBConstants.default_prefs_resource);
			if (default_prefs_stream == null) {
				System.out.println("no default preferences found at " + IGBConstants.default_prefs_resource + ", skipping...");
				return;
			}
			System.out.println("loading default prefs from: " + IGBConstants.default_prefs_resource);
			XmlPrefsParser.parse(default_prefs_stream);
		} catch (IOException ex) {
			System.out.println("Problem parsing prefs from: " + IGBConstants.default_prefs_resource);
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(default_prefs_stream);
		}
	}

	private static void LoadDefaultAPIPrefsFromJar() {
		// Return if there are not already Preferences defined.  (Since we define keystroke shortcuts, this is a reasonable test.)
		try {
			if ((PreferenceUtils.getTopNode()).nodeExists("keystrokes")) {
				return;
			}
		} catch (BackingStoreException ex) {
		}

		InputStream default_prefs_stream = null;
		/**  load default prefs from jar (with Preferences API).  This will be the standard method soon.*/
		try {
			default_prefs_stream = IGB.class.getResourceAsStream(IGBConstants.DEFAULT_PREFS_API_RESOURCE);
			System.out.println("loading default User preferences from: " + IGBConstants.DEFAULT_PREFS_API_RESOURCE);
			Preferences.importPreferences(default_prefs_stream);
			//prefs_parser.parse(default_prefs_stream, "", prefs_hash);
		} catch (Exception ex) {
			System.out.println("Problem parsing prefs from: " + IGBConstants.DEFAULT_PREFS_API_RESOURCE);
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(default_prefs_stream);
		}
	}

	private static void LoadWebPrefs(String def_prefs_url) {
		// If a particular web prefs file was specified, then load it.
		// Otherwise try to load the web-based-default prefs file. (But
		// only load it if it is cached, then later update the cache on
		// a background thread.)
		if (def_prefs_url != null) {
			//	loadDefaultWebBasedPrefs(prefs_parser, prefs_hash);
			//} else {
			LoadPreferencesFromURL(def_prefs_url);
		}
	}

	private static void LoadPreferencesFromURL(String prefs_url) {
		InputStream prefs_url_stream = null;
		try {
			prefs_url_stream = LocalUrlCacher.getInputStream(prefs_url);
			System.out.println("loading prefs from url: " + prefs_url);
			XmlPrefsParser.parse(prefs_url_stream);
		} catch (IOException ex) {
			System.out.println("Problem parsing prefs from url: " + prefs_url);
			System.out.println("Caused by: " + ex.toString());
		} finally {
			GeneralUtils.safeClose(prefs_url_stream);
		}
	}

	private static void LoadFileOrURLPrefs(String[] prefs_list) {
		if (prefs_list == null || prefs_list.length == 0) {
			return;
		}

		for (String fileOrURL : prefs_list) {
			InputStream strm = null;
			try {
				System.out.flush();


				File fil = new File(fileOrURL);
				if (fil.exists()) {
					System.out.println("loading user prefs from: " + fileOrURL);
					strm = new FileInputStream(fil);
					XmlPrefsParser.parse(strm);
				} else if (fileOrURL.startsWith("http:")) {
					System.out.println("loading user prefs from: " + fileOrURL);
					LoadPreferencesFromURL(fileOrURL);
				} else if (fileOrURL.startsWith("file:")) {
					fil = new File(new URI(fileOrURL));
					System.out.println("loading user prefs from: " + fileOrURL);
					strm = new FileInputStream(fil);
					XmlPrefsParser.parse(strm);
				} else {
					/*
					 * Non-existant files like $HOME/igb_prefs.xml and
					 * $PWD/igb_prefs.xml fall to here so do not print an error
					 */
					//System.out.println("Unknown prefs source: " + fileOrURL);
				}
			} catch (URISyntaxException ex) {
				System.out.flush();
				System.out.println("Problem parsing prefs from: " + fileOrURL);
				System.out.println(ex.toString());
			} catch (IOException ex) {
				System.out.flush();
				System.out.println("Problem parsing prefs from: " + fileOrURL);
				System.out.println(ex.toString());
			} finally {
				GeneralUtils.safeClose(strm);
			}
		}
	}

	/**
	 * Parse the command line arguments.  Find out what prefs file to use.
	 * Return the name of the file as a String, or null if not invoked with
	 * -prefs option.
	 */
	private static String[] get_prefs_list(String[] args) {
		String files = IGB.get_arg("-prefs", args);
		if (files == null) {
			files = default_user_prefs_files;
		}
		StringTokenizer st = new StringTokenizer(files, ";");
		Set<String> result = new HashSet<String>();
		result.add(st.nextToken());
		while (st.hasMoreTokens()) {
			result.add(st.nextToken());
		}
		return result.toArray(new String[result.size()]);
	}

	private static String get_default_prefs_url(String[] args) {
		String def_prefs_url = IGB.get_arg("-default_prefs_url", args);
		return def_prefs_url;
	}

	/**
	 * Checks the version of the preferences file.  This function is also
	 * responsible for updating older preference files to the current version.
	 */
	@SuppressWarnings("fallthrough")
	private static void checkPrefsVersion() {
		int version = PreferenceUtils.getTopNode().getInt("version", 0);

		switch (version) {
			case 0:
				Logger.getLogger(PrefsLoader.class.getName()).log(Level.FINE, "Upgrading unversioned preferences to version 1");
				ServerList.updateServerPrefs();
			/* continue */
			case 1:
				Logger.getLogger(PrefsLoader.class.getName()).log(Level.FINE, "Upgrading preferences version 1 to version 2");

				ServerList.updateServerURLsInPrefs();

				/* continue */

				/* add future version checks here */

				/* this always should occur in version n-1 */
				version = 2; /* change this number to current prefs version */
				PreferenceUtils.getTopNode().putInt("version", version);
				break;
			default:
			/* do nothing */

		}

		/*
		 * Check if the version of the preferences is not the 'correct' version.
		 * The check is done this way because code above this point should have
		 * upgraded the preferences file to the 'correct' version.  This check
		 * will catch any future preference versions as well as any mistakes in
		 * the upgrade code that runs before it.
		 */
		if (version != CURRENT_PREF_VERSION) {
			Object[] options = {"Quit IGB", "Delete and Continue"};
			int n = JOptionPane.showOptionDialog(null,
					"The preferences file is newer than this version of IGB.  Do you\n"
					+ "wish to delete preferences and continue?",
					"Preferences Too New",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null,
					options,
					options[0]);

			System.err.println("pressed: " + n);
			if (n == 0 || n == JOptionPane.CLOSED_OPTION) {
				System.err.println("Quitting");
				System.exit(0);
			} else {
				try {
					System.err.println("Deleting");
					PreferenceUtils.getTopNode().removeNode();
				} catch (BackingStoreException ex) {
					Logger.getLogger(PrefsLoader.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		}
	}
}
