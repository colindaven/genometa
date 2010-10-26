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

package com.affymetrix.igb.bookmarks;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.util.*;

import static com.affymetrix.igb.IGBConstants.UTF8;


/**
 *  Holds a bookmark, which is simply a name associated with a URL.
 */
public final class Bookmark implements Serializable {

  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String SEQID = "seqid";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String VERSION = "version";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String START = "start";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String END = "end";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String SELECTSTART = "selectstart";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String SELECTEND = "selectend";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark. */
  public static final String LOADRESIDUES  = "loadresidues";
  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark,
      this one can occur 0,1, or more times in the URL of a UnibrowControlServlet bookmark. */
  public static final String DATA_URL = "data_url";

  public static final String DAS2_QUERY_URL = "das2_query";
  public static final String DAS2_SERVER_URL = "das2_server";

  public static final String QUERY_URL = "query_url";
  public static final String SERVER_URL = "server_url";

  /** The name of one of the parameters in the URL of a UnibrowControlServlet bookmark,
      this optional paramater can be used to give the filetype extensions, such as ".gff" of
      each of the urls given with {@link #DATA_URL}.
      If these parameters are not used, then the filetype will be guessed based on the
      content type returned from the URLConnection, or from the file name in the URL.
      This parameter is optional, but if given there must be exactly one paramater
      for each of the {@link #DATA_URL} parameters given 
   */
  public static final String DATA_URL_FILE_EXTENSIONS = "data_url_file_extension";

  public static enum SYM{
	  FEATURE_URL	("feature_url_"),
	  METHOD		("sym_method_"),
	  YPOS			("sym_ypos_"),
	  YHEIGHT		("sym_yheight_"),
	  COL			("sym_col_"),
	  BG			("sym_bg_"),
	  NAME			("sym_name_");
	  
	  private String name;

	  SYM(String name){
			this.name = name;
		}

		@Override
		public String toString(){
			return name;
		}
	};

  public static enum GRAPH{
		FLOAT			("graph_float_"),
		SHOW_LABEL		("graph_show_label_"),
		SHOW_AXIS		("graph_show_axis_"),
		MINVIS			("graph_minvis_"),
		MAXVIS			("graph_maxvis_"),
		SCORE_THRESH	("graph_score_thresh_"),
		MAXGAP_THRESH	("graph_maxgap_thresh_"),
		MINRUN_THRESH	("graph_minrun_thresh_"),
		SHOW_THRESH		("graph_show_thresh_"),
		STYLE			("graph_style_"),
		THRESH_DIRECTION("graph_thresh_direction_"),
		HEATMAP			("graph_heatmap_"),
		COMBO			("graph_combo_");

		private String name;

		GRAPH(String name){
			this.name = name;
		}

		@Override
		public String toString(){
			return name;
		}
	};
	
  private static final boolean DEBUG = false;

  private String name;
  private URL url;

  public Bookmark(String name, String url) throws MalformedURLException {
    this.name = name;
    if (this.name == null || this.name.length() == 0) {
      this.name = "bookmark";
    }
    this.url = new URL(url);
  }

  /** Takes a URL and parses the query parameters into a map.
   *  All entries will be String arrays, as is expected by
   *  HttpServletRequest objects.
   *  Thus if the url is http://www.abc.com/page?x=3&z&y=4&y=5 then the
   *  resulting Map will have three String[] entries, for x={"3"} and z={""} and y={"4", "5"}.
   *  @return a Map, which can be empty.  All entries will be Strings.
   *  All keys and values will be decoded with {@link URLDecoder}.
   */
  public static Map<String,String[]> parseParameters(URL url) {
    Map<String,String[]> map = new LinkedHashMap<String,String[]>();
    String query = url.getQuery();
    if (query != null) {
      parseParametersFromQuery(map, query, true);
    }
    if (DEBUG) System.out.println("Finished parsing");
    return map;
  }

  /** Takes the query parameter string from a URL and parses the parameters
   *  into a the given map.
   *  All entries will be String arrays, as is expected by
   *  HttpServletRequest objects.
   *  Thus if the query string is  x=3&z&y=4&y=5  then the
   *  resulting Map will have three String[] entries, for x={"3"} and z={""} and y={"4", "5"}.
   *  All entries will be Strings.
   *  @param use_url_decoding whether or not to apply {@link URLDecoder} to all keys and values.
   */
 public static void parseParametersFromQuery(Map<String, String[]> map, String query, boolean use_url_decoding) {
		if (query == null) {
			return;
		}
		StringTokenizer st = new StringTokenizer(query, "&");
		while (st.hasMoreTokens()) {
			String token = st.nextToken();
			int ind_1 = token.indexOf('=');

			String key, value;
			if (ind_1 > 0) {
				key = token.substring(0, ind_1);
				value = token.substring(ind_1 + 1);
			} else {
				key = token;
				value = "";
			}

			if (use_url_decoding) {
				try {
					key = URLDecoder.decode(key, UTF8);
					value = URLDecoder.decode(value, UTF8);
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}

			addToMap(map, key, value);

			if (DEBUG) {
				System.out.println("Bookmark.parseParameters: Key  ->  " + key + ",  value -> " + value);
			}
		}
	}
  
  /**
   *  Adds a key->value mapping to a map where the key will map to
   *  a String array.  If the key already has a String[] mapped to it,
   *  this method will increase the length of that array.  Otherwise it
   *  will create a new String[] of length 1.
   *  
   *  @param map  a Map.  It is good to use a LinkedHashMap, if you care
   *     about the order of the entries, but this is not required.
   *  @param key  a non-null, non-empty String.  If null or empty, it will not
   *     be added to the map. (Empty means "String.trim().length()==0" )
   *  @param value a String.  Null is ok.
   */
  static void addToMap(Map<String,String[]> map, String key, String value) {
    if (key == null || key.trim().length()==0) {
      return;
    }
    String[] array = map.get(key);
    if (array == null) {
      String[] new_array = new String[] {value};
      map.put(key, new_array);
    } else {
      String[] new_array = new String[array.length+1];
      System.arraycopy(array, 0, new_array, 0, array.length);
      new_array[new_array.length - 1] = value;
      map.put(key, new_array);
    }
  }
  
  /** Constructs a UnibrowControlServer Bookmark URL based on the properties
   *  in the Map.  All keys and values will be encoded with
   *  {@link URLEncoder}.  All values should be String[] arrays, but any that are not
   *  will be converted to a String by calling the toString() method of the object.
   *  (For String[] objects, each String gets appended individually as a
   *  key=value pair, with the same key name.)
   */
  public static String constructURL(Map<String,String[]> props) {
    return constructURL(SimpleBookmarkServer.DEFAULT_SERVLET_URL, props);
  }

  /** Constructs a GENERIC Bookmark URL based on the properties
   *  in the Map.  All keys and values will be encoded with
   *  {@link URLEncoder}.  All values should be String[] arrays, but any that are not
   *  will be converted to a String by calling the toString() method of the object.
   *  (For String[] objects, each String gets appended individually as a
   *  key=value pair, with the same key name.)
   *  @param url_base The beginning part of a url, like "http://www.xxx.com"
   *    or even "http://www.xxx.com?x=1&y=2".
   */
  public static String constructURL(String url_base, Map<String,String[]> props) {
    StringBuffer sb = new StringBuffer();
    sb.append(url_base);
    
    Iterator<String> iter = props.keySet().iterator();
    
    // The first key in props is usually the first tag in the URL query string,
    // but *not* if the url_base already contains a '?' character.
    boolean first_tag = (url_base.indexOf('?') < 0);

    while (iter.hasNext()) {
      // for all properties, add as tag-val parameter pair in URL
      String tag = iter.next();
      Object val = props.get(tag);
      if (first_tag) {sb.append('?');}
      else {sb.append('&');}
      appendTag(sb, tag, val);
      first_tag = false;
    }
    if (DEBUG) System.out.println("Constructed URL: "+sb);
    return sb.toString();
  }

  /** Appends a key-value pair to a StringBuffer in URL parameter format "key=value".
   *  All keys and values will be encoded with {@link URLEncoder}.  
   *  All value objects should be Strings or String[]s, but any that are not
   *  will be converted to a String by calling the toString() method of the object.
   *  For String[] objects, each String will get converted individually to a
   *  "key=value" pair, with the same key name. Example:  "key=value1&key=value2&key=value3".
   */
  private static void appendTag(StringBuffer sb, String key, Object o) {
    try {
      if (o instanceof String[]) {
        String[] values = (String[]) o;
        for (int i=0; i<values.length; i++) {
          if (i>0) {sb.append('&');}
          sb.append(URLEncoder.encode(key, UTF8));
          String val = values[i];
          if (val != null && val.length()>0) {
            sb.append('=');
            sb.append(URLEncoder.encode(values[i], UTF8));
          }
        }
      } else {
        sb.append(URLEncoder.encode(key, UTF8));
        if (o != null) {
          String value = o.toString();
          if (value.length()>0) {
            sb.append('=');
            sb.append(URLEncoder.encode(value, UTF8));
          }
        }
      }
    } catch (UnsupportedEncodingException e) {}
  }
  
  public Map<String,String[]> getParameters() {
    return parseParameters(url);
  }
  
  /** Returns true if the Path of the Url matches 
   *  {@link SimpleBookmarkServer#SERVLET_NAME} or
	 *  {@link SimpleBookmarkServer#SERVLET_NAME_OLD} and
   *  the Host is "localhost". 
   */
  public boolean isUnibrowControl() {
    String host = getURL().getHost();
    String path = getURL().getPath();
    return (("localhost".equals(host) || "127.0.0.1".equals(host)) 
      && (path.equals("/"+SimpleBookmarkServer.SERVLET_NAME) || path.equals("/"+SimpleBookmarkServer.SERVLET_NAME_OLD)));
  }

  @Override
  public String toString() {
    return "Bookmark: '"+this.name+"' -> '"+this.url.toExternalForm()+"'";
  }

  public String getName() {
    return this.name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public URL getURL() {
    return this.url;
  }

  void setURL(URL url) {
    this.url = url;
  }
}
