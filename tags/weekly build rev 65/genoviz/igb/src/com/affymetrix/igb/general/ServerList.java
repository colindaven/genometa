package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 *
 * @version $Id: ServerList.java 7009 2010-10-11 18:10:14Z hiralv $
 */
public final class ServerList {
	private final static Map<String, GenericServer> url2server = new LinkedHashMap<String, GenericServer>();
	private final static Set<GenericServerInitListener> server_init_listeners = new CopyOnWriteArraySet<GenericServerInitListener>();
	private final static GenericServer localFilesServer = new GenericServer("Local Files","",ServerType.LocalFiles,true,null);

	public static Set<GenericServer> getEnabledServers() {
		Set<GenericServer> serverList = new HashSet<GenericServer>();
		for (GenericServer gServer : getAllServers()) {
			if (gServer.isEnabled() && gServer.getServerStatus() != ServerStatus.NotResponding) {
				serverList.add(gServer);
			}
		}
		return serverList;
	}

	public static Set<GenericServer> getInitializedServers() {
		Set<GenericServer> serverList = new HashSet<GenericServer>();
		for (GenericServer gServer : ServerList.getEnabledServers()) {
			if (gServer.getServerStatus() == ServerStatus.Initialized) {
				serverList.add(gServer);
			}
		}
		return serverList;
	}

	public static GenericServer getLocalFilesServer() {
		return localFilesServer;
	}

	public static boolean areAllServersInited() {
		for (GenericServer gServer : ServerList.getAllServers()) {
			if (!gServer.isEnabled()) {
				continue;
			}
			if (gServer.getServerStatus() == ServerStatus.NotInitialized) {
				return false;
			}
		}
		return true;
	}

	public static synchronized Collection<GenericServer> getAllServers() {
		return url2server.values();
	}

	public static synchronized Collection<GenericServer> getAllServersExceptCached(){
		Collection<GenericServer> servers = getAllServers();
		GenericServer server = getPrimaryServer();
		if(server != null)
			servers.remove(server);
		return servers;
	}

	/**
	 *  Given an URLorName string which should be the resolvable root URL
	 *  (but may optionally be the server name)
	 *  Return the GenericServer object.  (This could be non-unique if passed a name.)
	 *
	 * @param URLorName
	 * @return gserver or server
	 */
	public static GenericServer getServer(String URLorName) {
		GenericServer server = url2server.get(URLorName);
		if (server == null) {
			for (GenericServer gServer : getAllServers()) {
				if (gServer.serverName.equals(URLorName)) {
					return gServer;
				}
			}
		}
		return server;
	}

	/**
	 *
	 * @param serverType
	 * @param name
	 * @param url
	 * @param enabled
	 * @param isPrimary
	 * @return GenericServer
	 */
	public static GenericServer addServer(ServerType serverType, String name, String url, boolean enabled, boolean primary) {
		url = ServerUtils.formatURL(url, serverType);
		GenericServer server = url2server.get(url);
		Object info;

		if (server == null) {
			info = ServerUtils.getServerInfo(serverType, url, name);

			if (info != null) {
				server = new GenericServer(name, url, serverType, enabled, info, primary);

				if (server != null) {
					url2server.put(url, server);
				}
			}
			addServerToPrefs(server);
		}
		
		return server;
	}

	/**
	 *
	 * @param serverType
	 * @param name
	 * @param url
	 * @param enabled
	 * @return GenericServer
	 */
	public static GenericServer addServer(ServerType serverType, String name, String url, boolean enabled) {
		return addServer(serverType, name, url, enabled, false);
	}

	public static GenericServer addServer(Preferences node) {
		GenericServer server = url2server.get(GeneralUtils.URLDecode(node.name()));
		String url;
		String name;
		ServerType serverType;
		Object info;

		if (server == null) {
			url = GeneralUtils.URLDecode(node.name());
			name = node.get("name", "Unknown");
			serverType = ServerType.valueOf(node.get("type", ServerType.LocalFiles.name()));
			url = ServerUtils.formatURL(url, serverType);
			info = ServerUtils.getServerInfo(serverType, url, name);

			if (info != null) {
				server = new GenericServer(node, info);

				if (server != null) {
					url2server.put(url, server);
				}
			}
		}
		
		return server;
	}

	/**
	 * Remove a server.
	 *
	 * @param url
	 */
	public static void removeServer(String url) {
		GenericServer server = url2server.get(url);
		url2server.remove(url);
		server.setEnabled(false);
		fireServerInitEvent(server, ServerStatus.NotResponding);	// remove it from our lists.
	}

	/**
	 * Load server preferences from the Java preferences subsystem.
	 */
	public static void loadServerPrefs() {
		ServerType serverType;
		Preferences node;

		try {
			for (String serverURL : PreferenceUtils.getServersNode().childrenNames()) {
				node = PreferenceUtils.getServersNode().node(serverURL);
				serverType = ServerType.valueOf(node.get("type", ServerType.LocalFiles.name()));

				if (serverType == ServerType.LocalFiles) {
					continue;
				}

				addServer(node);
			}
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Update the old-style preference nodes to the newer format.  This is now
	 * called by the PrefsLoader when checking/updating the preferences version.
	 */
	public static void updateServerPrefs() {
		GenericServer server;

		for (ServerType type : ServerType.values()) {
			try {
				if (PreferenceUtils.getServersNode().nodeExists(type.toString())) {
					Preferences prefServers = PreferenceUtils.getServersNode().node(type.toString());
					String name, login, password;
					boolean enabled;
					for (String url : prefServers.keys()) {
						name        = prefServers.get(url, "Unknown");
						login       = prefServers.node("login").get(url, "");
						password    = prefServers.node("password").get(url, "");
						enabled     = Boolean.parseBoolean(prefServers.node("enabled").get(url, "true"));


						server = addServerToPrefs(GeneralUtils.URLDecode(url), name, type);
						server.setLogin(login);
						server.setEncryptedPassword(password);
						server.setEnabled(enabled);
					}
					prefServers.removeNode();
				}
			} catch (BackingStoreException ex) {
				Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
	}

	public static void updateServerURLsInPrefs() {
		Preferences servers = PreferenceUtils.getServersNode();
		Preferences currentServer;
		String normalizedURL;
		String decodedURL;
		
		try {
			for (String encodedURL : servers.childrenNames()) {
				try {
				currentServer = servers.node(encodedURL);
				decodedURL = GeneralUtils.URLDecode(encodedURL);
				String serverType = currentServer.get("type", "Unknown");
				if (serverType.equals("Unknown")) {
					Logger.getLogger(ServerList.class.getName()).log(
							Level.WARNING, "server URL: {0} could not be determined; ignoring.\nPreferences may be corrupted; clear preferences.", decodedURL);
					continue;
				}
				
				normalizedURL = ServerUtils.formatURL(decodedURL, ServerType.valueOf(serverType));

				if (!decodedURL.equals(normalizedURL)) {
					Logger.getLogger(ServerList.class.getName()).log(Level.FINE, "upgrading server URL: ''{0}'' in preferences", decodedURL);
					Preferences normalizedServer = servers.node(GeneralUtils.URLEncode(normalizedURL));
					for (String key : currentServer.keys()) {
						normalizedServer.put(key, currentServer.get(key, ""));
					}
					currentServer.removeNode();
				}
				} catch (Exception ex) {
					// Allow preferences loading to continue if an exception is encountered.
					Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
					continue;
				}
			}
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Add or update a server in the preferences subsystem.  This only modifies
	 * the preferences nodes, it does not affect any other part of the application.
	 *
	 * @param url URL of this server.
	 * @param name name of this server.
	 * @param type type of this server.
	 * @return an anemic GenericServer object whose sole purpose is to aid in setting of additional preferences
	 */
	private static GenericServer addServerToPrefs(String url, String name, ServerType type) {
		url = ServerUtils.formatURL(url, type);
		Preferences node = PreferenceUtils.getServersNode().node(GeneralUtils.URLEncode(ServerUtils.formatURL(url, type)));

		node.put("name",  name);
		node.put("type", type.toString());

		return new GenericServer(node, null);
	}

	/**
	 * Add or update a server in the preferences subsystem.  This only modifies
	 * the preferences nodes, it does not affect any other part of the application.
	 *
	 * @param server GenericServer object of the server to add or update.
	 */
	public static void addServerToPrefs(GenericServer server) {
		addServerToPrefs(server.URL, server.serverName, server.serverType);
	}

	/**
	 * Remove a server from the preferences subsystem.  This only modifies the
	 * preference nodes, it does not affect any other part of the application.
	 *
	 * @param url  URL of the server to remove
	 */
	public static void removeServerFromPrefs(String url) {
		try {
			PreferenceUtils.getServersNode().node(GeneralUtils.URLEncode(url)).removeNode();
		} catch (BackingStoreException ex) {
			Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
		}
	}


	/**
	 * Get server from ServerList that matches the URL.
	 * @param u
	 * @return server
	 * @throws URISyntaxException
	 */
	public static GenericServer getServer(URL u) throws URISyntaxException {
		URI a = u.toURI();
		URI b;
		for (String url : url2server.keySet()) {
			try {
				b = new URI(url);
				if (!b.relativize(a).equals(a)) {
					return url2server.get(url);
				}
			} catch (URISyntaxException ex) {
				Logger.getLogger(ServerList.class.getName()).log(Level.SEVERE, null, ex);
			}
		}
		throw new IllegalArgumentException("URL " + u.toString() + " is not a valid server.");
	}

	public static void addServerInitListener(GenericServerInitListener listener) {
		server_init_listeners.add(listener);
	}

	public static void fireServerInitEvent(GenericServer server, ServerStatus status) {
		fireServerInitEvent(server, status, false, true);
	}

	public static void fireServerInitEvent(GenericServer server, ServerStatus status, boolean removedManually) {
		fireServerInitEvent(server, status, false, removedManually);
	}

	public static void fireServerInitEvent(GenericServer server, ServerStatus status, boolean forceUpdate, boolean removedManually) {
		if (status == ServerStatus.NotResponding) {
			GeneralLoadUtils.removeServer(server);

			if(!removedManually)
				ErrorHandler.errorPanel(server.serverName, "Server " + server.serverName + " is not responding. Disabling it for this session.");
			
			if (server.serverType != ServerType.LocalFiles) {
				Application.getSingleton().removeNotLockedUpMsg("Loading server " + server + " (" + server.serverType.toString() + ")");
			}
		}

		if (forceUpdate || server.getServerStatus() != status) {
			server.setServerStatus(status);
			GenericServerInitEvent evt = new GenericServerInitEvent(server);
			for (GenericServerInitListener listener : server_init_listeners) {
				listener.genericServerInit(evt);
			}
		}
	}

	/**
	 * Gets the primary server if present else returns null.
	 * @return
	 */
	public static GenericServer getPrimaryServer(){
		for(GenericServer server : getEnabledServers()){
			if(server.isPrimary())
				return server;
		}
		return null;
	}
}
