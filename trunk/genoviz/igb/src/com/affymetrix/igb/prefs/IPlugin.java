/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.prefs;


/**
 *  Any plugin for IGB can *optionally* extend this interface.
 *  For any plugin that extends this interface, you can expect that
 *  {@link #putPluginProperty(Object, Object)} will be called for
 *  several key properties during construction of the plugin.
 */
public interface IPlugin {

  /**
   *  Returns a String that can be used as the name of the component.
   *  Preferably a very short string, since it may be used as the name of
   *  a tab in a tab pane.
   *  @return a non-null, short String identifier.
   */
  public String getName();
  
  /**
   *  Use this to set special values in the plugin.
   *  You can expect that the following will be set
   *  during creation of the plugin:
   *  <ul>
   *  <li> {@link #TEXT_KEY_APP}: an instance of {@link com.affymetrix.igb.Application}
   *  <li> {@link #TEXT_KEY_SEQ_MAP_VIEW}: an instance of {@link com.affymetrix.igb.view.SeqMapView}
   *  </ul>
   *
   *  Note that many types of plugin don't need to know those values.  
   *  Implementations of this class are free to ignore values passed in or
   *  store them in a hashtable or in any other way desired.
   *
   *  Note that you can un-set a property by setting its value to null.  In order
   *  to aid garbage collection, the values of "IGB" and "SeqMapView" in particular
   *  should be set to null when destroying one of these plugins.
   */
  public void putPluginProperty(Object key, Object value);
  
  /**
   *  Returns anything that was explicitly set with {@link #putPluginProperty(Object, Object)},
   *  or that the object itself decides to return.  There are certain special
   *  keys that can be used by convention to get values that are useful for
   *  IGB plugins.  In all cases, it is acceptable for the return value to be null.
   *  Special text keys:
   *  <ul>
   *   <li> {@link #TEXT_KEY_ICON}: null or an {@link javax.swing.ImageIcon} icon that could be used in a tab pane or a Frame.
   *   <li> {@link #TEXT_KEY_HTML_HELP}: null or help text in HTML format.
   *   <li> {@link #TEXT_KEY_DESCRIPTION}: null or textual descriptive text.
   *   <li> {@link #TEXT_KEY_INFO_URL}: null or a String containing a URL to find more information.
   *   <li> {@link #TEXT_KEY_APP}: null or an instance of {@link com.affymetrix.igb.Application}.
   *   <li> {@link #TEXT_KEY_SEQ_MAP_VIEW}: null or an instance of {@link com.affymetrix.igb.view.SeqMapView}.
   *  </ul>
   *
   */
  public Object getPluginProperty(Object o);
    
  /**
   *  This method might be called when the plugin is no longer needed.
   *  It can be used to free any resources to aid garbage collection, etc.
   */
  public void destroy();
    
  /**
   *  Key for holding an instance of {@link com.affymetrix.igb.Application}.
   */
  public static final String TEXT_KEY_APP = "Application";

  /**
   *  Key for holding an instance of {@link com.affymetrix.igb.view.SeqMapView}.
   */
  public static final String TEXT_KEY_SEQ_MAP_VIEW = "SeqMapView";
  
  /**
   *  Key for help text explaining the function of this plugin.
   *  If no help is available, should return null rather than an empty String.
   */
  public static final String TEXT_KEY_HTML_HELP = "HTML help";
  
  /**  
   *  Key for a String representing a URL where the user can find more information.
   */
  public static final String TEXT_KEY_INFO_URL = "info URL";
 
  /**
   *  Key for holding an Icon.
   */
  public static final String TEXT_KEY_ICON = "icon";
  
  /**
   *  Key for holding a String text description.
   */
  public static final String TEXT_KEY_DESCRIPTION = "description";
  
}
