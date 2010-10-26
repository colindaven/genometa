/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */
package com.affymetrix.igb.parsers;

import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.igb.IGBConstants;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import org.w3c.dom.*;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;

import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.igb.view.PluginInfo;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.glyph.MapViewGlyphFactoryI;
import javax.xml.parsers.ParserConfigurationException;
import org.xml.sax.SAXException;

/**
 *Class for parsing preferences for IGB.
 *<pre>
Description of <annotation_style> element format

---------------------------------------------------------------------

One form of type attribute is required, possible attribute names are:
annot_type="val"
annot_type_starts_with="val"
annot_type_ends_with="val"
annot_type_regex="val"
There can also be any combination of these type attributes.

So the simplest <annotation_style> example possible would be:
<annotation_style annot_type="xyz" />

Although note that the above example is not very useful without using some
of the options discussed below.  Because if an annotation is loaded that has no
<annotation_style> entry in the prefs file(s), then a default annotation style
is automatically assigned, which has the same effect as the above
<annotation_style> entry...

All other attributes and sub-elements are optional, and are either
unnecessary or automatically assigned a default if not present.

Optionally specifying the factory class:
The "factory" attribute specifies a class to be instantiated
as the glyph factory to "glyphify" annotations of the given annotation type.
Other attributes of an <annotation_style> element are passed to the factory
in an initialization step as a Map with key/value pairs of form
{ attribute_name ==> attribute_value }, and it is up to the the specific
factory implementation to decide what to do with this information.  This
means that there are no restrictions on the attribute names in the
<annotation_style> element, as different factories may recognize different
attributes.
Example:
<annotation_style annot_type="abc" factory="com.affymetrix.igb.glyph.GenericGraphGlyphFactory" />

The usual default factory is the GenericAnnotGlyphFactory.
Attributes that GenericAnnotGlyphFactory recognizes currently include:

"child glyph": This attribute specifies what glyph to use to render
the (visible) leaf spans of the annotation
"parent_glyph": This attribute specifies what glyph to use to connect
the child glyphs
Example:
<annotation_style annot_type="test2"
parent_glyph="com.affymetrix.igb.glyph.EfficientOutlineContGlyph"
child_glyph="com.affymetrix.igb.glyph.EfficientFillRectGlyph"  />
 *</pre>
 * 
 * @version $Id: XmlPrefsParser.java 6803 2010-08-31 15:45:14Z hiralv $
 */
public final class XmlPrefsParser {

	private static final Class<?> default_factory_class =
			com.affymetrix.igb.glyph.GenericAnnotGlyphFactory.class;
	private static final Set<PluginInfo> plugins = new LinkedHashSet<PluginInfo>();

	private XmlPrefsParser() {
	}

	public static void parse(InputStream istr) throws IOException {
		InputSource insource = new InputSource(istr);

		try {
			Document prefsdoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(insource);
			processDocument(prefsdoc);
		} catch (ParserConfigurationException ex) {
			throw new IOException(ex);
		} catch (SAXException ex) {
			throw new IOException(ex);
		}
	}

	public static Set<PluginInfo> getPlugins() {
		return Collections.<PluginInfo>unmodifiableSet(plugins);
	}

	private static void processDocument(Document prefsdoc) {
		Element top_element = prefsdoc.getDocumentElement();
		String topname = top_element.getTagName();
		if (!(topname.equalsIgnoreCase("prefs"))) {
			System.err.println("not a prefs file -- can't parse in prefs!");
		}
		NodeList children = top_element.getChildNodes();
		Node child;
		String name;
		Element el;

		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			name = child.getNodeName();
			if (child instanceof Element) {
				el = (Element) child;
				if (name.equalsIgnoreCase("annotation_style")) {
					processAnnotStyle(el);
				} else if (name.equalsIgnoreCase("annotation_url")) {
					processLinkUrl(el);
				} else if (name.equalsIgnoreCase("plugin")) {
					processPlugin(el);
				} else if (name.equalsIgnoreCase("server")) {
					processServer(el);
				}
			}
		}
	}

	private static void processServer(Element el) {
		ServerType server_type = getServerType(el.getAttribute("type"));
		String server_name = el.getAttribute("name");
		String server_url = el.getAttribute("url");
		String en = el.getAttribute("enabled");
		Boolean enabled = en == null || en.isEmpty() ? true : Boolean.valueOf(en);
		String pr = el.getAttribute("primary");
		Boolean primary = pr == null || pr.isEmpty() ? false : Boolean.valueOf(pr);
		if (IGBConstants.DEBUG) {
			System.out.println("XmlPrefsParser adding " + server_type + " server: " + server_name + ",  " + server_url + ", enabled: " + enabled);
		}
		ServerList.addServer(server_type, server_name, server_url, enabled, primary);
	}
	
	private static ServerType getServerType(String type) {
		for (ServerType t : ServerType.values()) {
			if (type.equalsIgnoreCase(t.toString())) {
				return t;
			}
		}
		return ServerType.LocalFiles;
	}

	private static void processPlugin(Element el) {
		String loadstr = el.getAttribute("load");
		String plugin_name = el.getAttribute("name");
		String class_name = el.getAttribute("class");
		//String description = el.getAttribute("description");
		//String info_url = el.getAttribute("info_url");
		boolean load = (loadstr == null ? true : (!loadstr.equalsIgnoreCase("false")));
		if (plugin_name != null && class_name != null) {
			System.out.println("plugin, name = " + plugin_name + ", class = " + class_name);
			PluginInfo pinfo = new PluginInfo(class_name, plugin_name, load);
			plugins.add(pinfo);
		}
	}

	/**
	 *  Sets up a regular-expression matching between a method name or id and a url,
	 *  which can be used, for example, in SeqMapView to "get more info" about
	 *  an item.
	 *  For example:
	 *  <p>
	 *  <code>&gt;annotation_url annot_type_regex="google" match_case="false" url="http://www.google.com/search?q=$$" /&lt;</code>
	 * <code>&gt;annotation_url annot_id_regex="^AT*" match_case="false" url="http://www.google.com/search?q=$$" /&lt;</code>
	 *  <p>
	 *  Note that the url can contain "$$" which will later be substituted with the
	 *  "id" of the annotation to form a link.
	 *  By default, match is case-insensitive;  use match_case="true" if you want
	 *  to require an exact match.
	 */
	private static void processLinkUrl(Element el) {
		Map<String, String> attmap = XmlPrefsParser.getAttributeMap(el);
		String url = attmap.get("url");
		if (url == null || url.trim().length() == 0) {
			System.out.println("ERROR: Empty data in preferences file for an 'annotation_url':" + el.toString());
			return;
		}

		WebLink.RegexType type_regex = WebLink.RegexType.TYPE;
		String annot_regex_string = attmap.get("annot_type_regex");
		if (annot_regex_string == null || annot_regex_string.trim().length() == 0) {
			type_regex = WebLink.RegexType.ID;
			annot_regex_string = attmap.get("annot_id_regex");
		}
		if (annot_regex_string == null || annot_regex_string.trim().length() == 0) {
			System.out.println("ERROR: Empty data in preferences file for an 'annotation_url':" + el.toString());
			return;
		}

		String name = attmap.get("name");
		String species = attmap.get("species");
		try {
			WebLink link = new WebLink();
			link.setRegexType(type_regex);
			link.setName(name);
			link.setUrl(url);
			link.setSpeciesName(species);
			if ("false".equalsIgnoreCase(attmap.get("match_case"))) {
				link.setRegex("(?-i)" + annot_regex_string);
			} else {
				link.setRegex(annot_regex_string);
			}

			WebLink.addWebLink(link);
		} catch (PatternSyntaxException pse) {
			System.out.println("ERROR: Regular expression syntax error in preferences\n" + pse.getMessage());
		}
	}

	private static void processAnnotStyle(Element el) {
		Class<?> factory_class = default_factory_class;
		Map<String, String> attmap = XmlPrefsParser.getAttributeMap(el);

		// annotation_style element _must_ have and annot_type attribute
		// planning to relax this at some point to allow for element to have one (and only one) of:
		//     annot_type, annot_type_starts_with, annot_type_ends_with, annot_type_regex...
		if (attmap.get("factory") != null) {
			String factory_name = null;
			try {
				factory_name = attmap.get("factory");
				factory_class = Class.forName(factory_name);
			} catch (ClassNotFoundException ex) {
				System.out.println("ERROR: Class '" + factory_name + "' specified in the preferences file can not be found");
				factory_class = default_factory_class;
			}
		}
		try {
			MapViewGlyphFactoryI factory = (MapViewGlyphFactoryI) factory_class.newInstance();
			factory.init(attmap);
		} catch (InstantiationException ex) {
			System.out.println("ERROR: Could not instantiate a glyph factory while processing preferences file: " + factory_class);
		} catch (IllegalAccessException ex) {
			System.out.println("ERROR: Could not instantiate a glyph factory while processing preferences file: " + factory_class);
		} catch (ClassCastException ex) {
			System.out.println("ERROR: Could not instantiate a glyph factory while processing preferences file: " + factory_class + " is not an instance of MapViewGlyphFactoryI");
		} catch (Exception ex) {
			System.out.println("ERROR: Exception while parsing preferences: " + ex.toString());
		}
	}

	private static Map<String, String> getAttributeMap(Element el) {
		HashMap<String, String> amap = new HashMap<String, String>();
		NamedNodeMap atts = el.getAttributes();
		int attcount = atts.getLength();
		for (int i = 0; i < attcount; i++) {
			Attr att = (Attr) atts.item(i);
			String tag = att.getName();
			String val = att.getValue();
			amap.put(tag, val);
		}
		return amap;
	}
}
