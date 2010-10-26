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
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.tiers.ExpandPacker;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.view.SeqMapView;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

final class ChildrenElement implements Cloneable, XmlAppender {

/*
<!ELEMENT CHILDREN (PROPERTY*, ((MATCH+,ELSE?) | (STYLE|USE_STYLE)))>
<!ATTLIST CHILDREN
    container CDATA #IMPLIED
    child_positions CDATA #IMPLIED
  >
*/
  
static String NAME = "CHILDREN";
  
static String ATT_CONTAINER = "container";
static String ATT_POSITIONS = "child_positions";
  
  private String childContainer = ".";
  private String childPositions; // becomes default position for children glyphs if they don't override it
  private List<MatchElement> matchElements;
  StyleElement styleElement;
  
  PropertyMap propertyMap;
  
  static ExpandPacker expand_packer;
  
  static {
      expand_packer = new ExpandPacker();
      expand_packer.setParentSpacer(3);
      expand_packer.setStretchHorizontal(false);
  }
  
	@Override
  public Object clone() throws CloneNotSupportedException {
    ChildrenElement clone = (ChildrenElement) super.clone();
    if (styleElement != null) {
      clone.styleElement = (StyleElement) styleElement.clone();
    }
    if (matchElements != null) {
      clone.matchElements = new ArrayList<MatchElement>(matchElements.size());
			for (MatchElement me : matchElements) {
        MatchElement new_me = (MatchElement) me.clone();
        clone.matchElements.add(new_me);
      }
    }
    if (propertyMap != null) {
      clone.propertyMap = (PropertyMap) this.propertyMap.clone();
    }
    return clone;
  }
  
  ChildrenElement() {
    this.propertyMap = new PropertyMap();
  }
  
  /** Draws the children symmetries as glyphs. 
   *  @param insym the parent sym
   *  @param gl the glyph corresponding to the parent sym.  By default, children
   *   symmetries are drawn as glyphs inside this parent glyph, but that can
   *   change depending on the setting of {@link #childContainer}.
   */
  void childSymsToGlyphs(SeqMapView gviewer, SeqSymmetry insym, GlyphI gl, 
      Stylesheet stylesheet, PropertyMap context) {
    
    int childCount = insym.getChildCount();
    if (childCount > 0) {
      for (int i=0; i<childCount; i++) {
        SeqSymmetry childsym = insym.getChild(i);
        this.childSymToGlyph(gviewer, childsym, gl, stylesheet, context);
      }
    }
     
    // packing will be handled in the containing <GLYPH> element
  }

  /** Draws a single child from the <CHILDREN> element.  Generally called
   *  only from inside this class.
   */
  private GlyphI childSymToGlyph(SeqMapView gviewer, SeqSymmetry childsym,
      GlyphI container_glyph, Stylesheet stylesheet, PropertyMap context) {
    GlyphI result = null;

    PropertyMap oldContext = propertyMap.getContext();
    this.propertyMap.setContext(context);

    if (matchElements != null && ! matchElements.isEmpty()) {
      Iterator iter = matchElements.iterator();
      while (iter.hasNext() && result == null) {
        MatchElement matchElement = (MatchElement) iter.next();
        
        // If the match element matches, it will return a glyph, otherwise will return null
        GlyphI match_glyph = matchElement.symToGlyph(gviewer, childsym, 
            container_glyph, stylesheet, propertyMap);
        if (match_glyph != null) {
          result = match_glyph;
        }
      }
    }
    
    // If there were no match elements matched, use the given <STYLE> element
    else {
      DrawableElement drawable = styleElement;
      if (drawable == null) {
        // NOTE: The current DTD requires that a <STYLE> or <USE_STYLE> be specified,
        // but I've experimented with the possibility of leaving it blank, in which case
        // ask the stylesheet for an appropriate style.
        drawable = stylesheet.getDrawableForSym(childsym);
      }
      if (drawable != null) {
        result = drawable.symToGlyph(gviewer, childsym, container_glyph, stylesheet, propertyMap);
      } else {
        SeqUtils.printSymmetry(childsym);
      }
    }

    this.propertyMap.setContext(oldContext);
    return result;
  }
  
  static GlyphI findContainer(GlyphI gl, String container) {
    GlyphI container_glyph = gl;
    
    if (container == null || "".equals(container)) {
      container_glyph = gl;
    } else if (".".equals(container)) {
      container_glyph = gl;
    } else if ("..".equals(container)) {
      container_glyph = parent(gl);
    } else if ("../..".equals(container)) {
      container_glyph = parent(parent(gl));
    } else if ("../../..".equals(container)) {
      container_glyph = parent(parent(parent(gl)));
    } else if ("../../../..".equals(container)) {
      container_glyph = parent(parent(parent(parent(gl))));
      
      /// TODO: handle arbitrary nesting levels
      
    } else if ("/".equals(container)) {
      container_glyph = gl;
      while (!( container_glyph instanceof TierGlyph)) {
        container_glyph = parent(container_glyph);
      }
    }
    return container_glyph;
  }  
  
  private static GlyphI parent(GlyphI gl) {
    if (gl instanceof TierGlyph) {
      return gl;
    } else {
      return gl.getParent();
    }
  }

  void setStyleElement(StyleElement styleElement) {
    this.styleElement = styleElement;
  }
  
  void addMatchElement(MatchElement me) {
    if (matchElements == null) {
      matchElements = new ArrayList<MatchElement>();
    }
    matchElements.add(me);
  }
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    sb.append(indent).append('<').append(NAME);
    XmlStylesheetParser.appendAttribute(sb, ATT_CONTAINER, childContainer);
    XmlStylesheetParser.appendAttribute(sb, ATT_POSITIONS, childPositions);
    sb.append(">\n");

    if (this.propertyMap != null) {
      propertyMap.appendXML(indent + "  ", sb);
    }
    
    if (matchElements != null) {
      Iterator iter = matchElements.iterator();
      while (iter.hasNext()) {
       MatchElement kid = (MatchElement) iter.next();
       kid.appendXML(indent + "  ", sb);
      }
    }
    
    if (styleElement != null) {
      styleElement.appendXML(indent + "  ", sb);
    }

    sb.append(indent).append("</").append(NAME).append(">\n");
    return sb;
  }

  void setChildContainer(String child_container) {
    this.childContainer = child_container;
  }

  void setPosition(String position) {
    this.childPositions = position;
  }
}
