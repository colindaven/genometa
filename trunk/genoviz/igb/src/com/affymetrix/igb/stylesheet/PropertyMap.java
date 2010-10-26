/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.igb.stylesheet;

import java.awt.Color;
import java.util.*;

/**
 *  A cascading implementation of the Java Map class that also implements
 *  the Genometry Propertied interface.
 *  Will store properties in its own Map.  Will look for properties
 *  first in its own Map, then in its parent Map, then the parent's parent, etc.
 *  All keys and values must be String's.
 */
public final class PropertyMap extends HashMap<String, Object> implements Map<String, Object>, Cloneable, XmlAppender {

  private PropertyMap parentProperties;

  private final ArrayList<PropertyMap> ancestors = new ArrayList<PropertyMap>(100);


  private static int color_warnings = 0; // count the number of warnings about colors
  // Printing millions of such warnings would cause a big slowdown.


  static final String PROP_ELEMENT_NAME = "PROPERTY";
  static final String PROP_ATT_KEY = "key";
  static final String PROP_ATT_VALUE = "value";
  
  /** Checks whether this item's parent, or grandparent, etc. is
   *  identical to the possible_ancestor. This helps prevent infinite loops
   *  that could arise during processing recursive <STYLE> invocations.
   */
  private boolean hasAncestor(PropertyMap possible_ancestor) {
    PropertyMap p2 = this;
    while (p2 != null) {
      if (p2 == possible_ancestor) {
        return true;
      }
      p2 = p2.parentProperties;
    }
    return false;
  }
  
  void setContext(PropertyMap context) {
    if (context == null) {
      this.parentProperties = null;
    } else {
      
      if (context.hasAncestor(this)) {
        throw new RuntimeException("BAD CONTEXT: Current already present in context");
      }
      
      // Cloning prevents context.hasAncestor(this) from ever being true.
      // There may be a simpler way....
      this.parentProperties = (PropertyMap) context.clone();
    }
  }
  
  PropertyMap getContext() {
    return this.parentProperties;
  }
  
	@Override
  public Object get(Object key) {
    return this.getProperty((String) key);
  }
    
  public Object getProperty(String key) {
    Object o = super.get(key);
    
    //WARNING: the simple, obvious way of implementing recursion would have the
    // possibility of infinite recursion which is avoided here (I hope!).
    if (o == null) {
      ancestors.clear();
      o = this.getProperty(key, 0, ancestors);
      ancestors.clear();
    }
    
    return o;
  }
    
  private Object getProperty(String key, int recur, List<PropertyMap> ancestors) {

    if (ancestors.contains(this)) {
      System.out.println("WARNING: Caught an infinite loop!");
      return null;
    }

    if (recur == 100) {
      throw new RuntimeException("Recursion too deep.");
    }
    
    Object o = super.get(key);
    
    if (o == null && parentProperties != null) {
      ancestors.add(this);
      o = this.parentProperties.getProperty(key, recur+1, ancestors);
    }

    return o;
  }

  public void setProperty(String key, Object val) {
    super.put(key, val);
  }
    
  public Color getColor(String key) {
        
    Color c = null;
    Object o = getProperty(key);
    if ("".equals(o)) {
      // setting the value of color to "" means that you want to ignore the
      // color settings in any inherited context and revert to the default.
      return null;
    } else if (o instanceof Color) {
      c = (Color) o;
    } else if (o instanceof String) {
      try {
        c = Color.decode("0x"+o);
      } catch (Exception e) {
        c = null;
        if (++color_warnings < 100) {
          System.out.println("WARNING: could not parse color '"+o+"'");
        }
      }
    }

    PropertyMap pm = this;
    if (c != null) {
      // replace the color string with a Color object for speed in next call.
      // But be careful to do thiw only where the key->color entry was found
      // in THIS map, not a parent or child map.
      while (pm != null && pm != this) {
        if (pm.get(key) != null) {
          pm.put(key, c);
          pm = null; // to end the loop
        } else {
          pm = pm.parentProperties;
        }
      }
    }

    return c;
  }
  
	@Override
  public Object clone() {
    PropertyMap clone = (PropertyMap) super.clone();
    // It does not seem necessary to clone the parent properties,
    // but I can revisit this later if necessary
    //clone.parentProperties = (PropertyMap) this.parentProperties.clone();
    clone.parentProperties = this.parentProperties;
    return clone;
  }
  
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    Iterator<String> iter = this.keySet().iterator();
    while (iter.hasNext()) {
     String key = iter.next();
     Object value = this.getProperty(key);
     sb.append(indent).append('<').append(PROP_ELEMENT_NAME);
     XmlStylesheetParser.appendAttribute(sb, PROP_ATT_KEY, key);
     XmlStylesheetParser.appendAttribute(sb, PROP_ATT_VALUE, "" + value);
     sb.append("/>\n");
    }
    return sb;
  }

}
