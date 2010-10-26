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

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.igb.view.SeqMapView;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class MatchElement implements DrawableElement {

//<!ELEMENT MATCH (PROPERTY*, ((MATCH+,ELSE?) | (STYLE|USE_STYLE)))>
//<!ATTLIST MATCH
//    test CDATA #REQUIRED
//    param CDATA #REQUIRED
//>
  
  static String NAME = "MATCH";

  static String ATT_TEST = "test";
  static String ATT_PARAM = "param";
  
  static String MATCH_BY_EXACT_TYPE = "type";
  static String MATCH_BY_EXACT_METHOD = "method";
  static String MATCH_BY_METHOD_REGEX = "method_regex";

  /** A test where of whether the item has or doesn't have children;
   *  the param should be set to "true" (default) or "false".
   */
  static String TEST_HAS_CHILDREN = "has_children";
  
  static String[] knownTypes = new String[] {
    MATCH_BY_EXACT_TYPE, MATCH_BY_EXACT_METHOD, MATCH_BY_METHOD_REGEX,
    TEST_HAS_CHILDREN,
  };
  
  StyleElement styleElement;
  PropertyMap propertyMap;
  List<MatchElement> subMatchList = new ArrayList<MatchElement>();
  
  String match_test = null;
  String match_param = null;
  Pattern match_regex = null;
    
	@Override
  public Object clone() throws CloneNotSupportedException {
    MatchElement clone = (MatchElement) super.clone();
    clone.styleElement = (StyleElement) styleElement.clone();
    clone.subMatchList = new ArrayList<MatchElement>(subMatchList.size());
		for (MatchElement me : subMatchList) {
      MatchElement new_me = (MatchElement) me.clone();
      clone.subMatchList.add(new_me);
    }
    if (propertyMap != null) {
      clone.propertyMap = (PropertyMap) this.propertyMap.clone();
    }
    return clone;
  }

  MatchElement() {
    this.propertyMap = new PropertyMap();
  }
  
  /**
   *  NOT IMPLEMENTED.
   *  If this MatchElement, or one of its children, matcheds the given symmetry,
   *  then a glyph will be created and returned.  Otherwise, will return null.
   */
  public GlyphI symToGlyph(SeqMapView gviewer, SeqSymmetry sym, GlyphI gl, 
      Stylesheet stylesheet, PropertyMap context) {
    PropertyMap oldContext = propertyMap.getContext();
    propertyMap.setContext(context);
    
    // If a sub_matcher matches, use it to make the glyph, 
    // otherwise if this matches, make it ourselves,
    // otherwise, if no match, return null
    
    GlyphI result = null;
    
    if (subMatchList != null) {
			for (MatchElement me : subMatchList) {
        result = me.symToGlyph(gviewer, sym, gl, stylesheet, propertyMap);
      }
    }

    // If none of the sub-matches matched, then check whether this matches,
    // and if so, draw it ourselves
    else if (styleElement != null) {
      if (matches(sym)) {
        result = styleElement.symToGlyph(gviewer, sym, gl, stylesheet, propertyMap);
      }
    }

    propertyMap.setContext(oldContext);
    return result;
  }

  static boolean knownTestType(String type) {
    for (int i=0; i<knownTypes.length; i++) {
      if (type.equals(knownTypes[i])) { return true; }
    }
    return false;
  }

  boolean matches(SeqSymmetry sym) {
    boolean result = false;
    
    if (match_param == null) {
      return false;
    }
    
    if (TEST_HAS_CHILDREN.equals(match_test)) {
      boolean hasChildren = (sym.getChildCount() == 0);
      if ("false".equalsIgnoreCase(match_param)) {
        return ! hasChildren;
      } else {
        return hasChildren;
      }
    }
    
    if (MATCH_BY_EXACT_TYPE.equals(match_test)) {
      if (sym instanceof SymWithProps && 
          match_param.equals( ((SymWithProps) sym).getProperty("type") ))  {
        result = true;
      }
    }
    
    else if (MATCH_BY_EXACT_METHOD.equals(match_test)) {
      if (match_param.equals( BioSeq.determineMethod(sym) ))  {
        result = true;
      }
    }

    else if (MATCH_BY_METHOD_REGEX.equals(match_test) && match_regex != null) {
      if (match_regex.matcher(BioSeq.determineMethod(sym)).matches())  {
        result = true;
      }
    }
    
    return result;
  }
  
  StyleElement getStyle() {
    return this.styleElement;
  }

  void setStyle(StyleElement style) {
    this.styleElement = style;
  }

  List<MatchElement> getSubMatchList() {
    return this.subMatchList;
  }
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    return appendXML(indent, sb, NAME);
  }

  StringBuffer appendXML(String indent, StringBuffer sb, String name) {
    sb.append(indent).append("<").append(NAME).append(' ');
    XmlStylesheetParser.appendAttribute(sb, ATT_TEST, match_test);
    XmlStylesheetParser.appendAttribute(sb, ATT_PARAM, match_param);
    sb.append(">\n");

    if (this.propertyMap != null) {
      propertyMap.appendXML(indent + "  ", sb);
    }

		for (MatchElement kid : this.getSubMatchList()) {
     kid.appendXML(indent + "  ", sb);
    }
    
    if (styleElement != null) {
      styleElement.appendXML(indent + "  ", sb);
    }

    sb.append(indent).append("</").append(name).append(">\n");
    return sb;
  }
}
