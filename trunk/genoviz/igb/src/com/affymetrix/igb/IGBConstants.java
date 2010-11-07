/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

package com.affymetrix.igb;

import com.affymetrix.genometryImpl.util.Constants;
import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *  Some global constants.
 */
public interface IGBConstants {
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("igb",new Locale(""));

	public static final String APP_NAME         = BUNDLE.getString("appName");
	public static final String APP_NAME_SHORT   = BUNDLE.getString("appNameShort");
	public static final String APP_VERSION      = BUNDLE.getString("appVersion");
	public static final String APP_VERSION_FULL = MessageFormat.format(
			BUNDLE.getString("appVersionFull"),
			APP_VERSION);

	public static final String USER_AGENT = MessageFormat.format(
			BUNDLE.getString("userAgent"),
			APP_NAME_SHORT,
			APP_VERSION_FULL,
			System.getProperty("os.name"),
			System.getProperty("os.version"),
			System.getProperty("os.arch"),
			Locale.getDefault().toString());

	public static final boolean DEBUG = false;

	public static final String DEFAULT_PREFS_API_RESOURCE = "/igb_default_APIprefs.xml";
	public static final String default_prefs_resource = "/igb_default_prefs.xml";

	public static final String UTF8 = "UTF-8";

	public final static String GENOME_SEQ_ID = Constants.GENOME_SEQ_ID;
	public final static String ENCODE_REGIONS_ID = "encode_regions";
	/**
	 * A potential parameter in either the URL of a bookmark, or a command-line option.  This allows a response file to be loaded, instead of anything else.
	 */
	public final static String SCRIPTFILETAG = "scriptfile";
}
