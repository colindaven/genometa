package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.util.prefs.Preferences;

public final class PluginInfo {
	public static final String NODE_PLUGINS = "plugins";
	private final String class_name;
	private final String plugin_name;
	private final boolean load;
	private final String display_name;

	public PluginInfo(String class_name, String plugin_name, boolean load) {
		this.class_name = class_name;		
		// If the plugin_name is null, it will try to set itself to the
		// class name, with the package name removed.
		if ((plugin_name == null || plugin_name.trim().length() == 0) && class_name != null) {
			int index = class_name.lastIndexOf('.');
			this.plugin_name = (index == 0) ? class_name : class_name.substring(index);
		} else {
			this.plugin_name = plugin_name;
		}
		this.load = load;
		this.display_name = this.plugin_name;
	}

	@Override
	public String toString() {
		return "IGB PluginInfo: " +
						"name = " + plugin_name + ", class = " + class_name + ", load = " + load;
	}

	public String getClassName() {
		return class_name;
	}

	public String getPluginName() {
		return plugin_name;
	}

	public boolean shouldLoad() {
		return load;
	}

	public String getDisplayName() {
		if (this.display_name == null || this.display_name.trim().length() == 0) {
			return this.plugin_name;
		} else {
			return this.display_name;
		}
	}

	public static Preferences getNodeForName(String name) {
		return PreferenceUtils.getTopNode().node(NODE_PLUGINS).node(name);
	}

	public static Object instantiatePlugin(String class_name) throws InstantiationException {
		Object plugin = null;
		try {
			plugin = Class.forName(class_name).newInstance();
		} catch (Throwable t) {
			// catches things like NoClassDefFoundError
			String msg = "Could not instantiate plugin\n" +
							"class name: '" + class_name + "'\n";
			InstantiationException e = new InstantiationException(msg);
			e.initCause(t);
			throw e;
		}
		return plugin;
	}
}
