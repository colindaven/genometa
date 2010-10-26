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
import com.affymetrix.genometryImpl.GFF3Sym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.igb.view.SeqMapView;
import java.util.*;
import java.util.regex.Pattern;

public final class Stylesheet implements Cloneable, XmlAppender {
  LinkedHashMap<String, AssociationElement> meth2association = new LinkedHashMap<String, AssociationElement>();
  LinkedHashMap<Pattern, AssociationElement> regex2association = new LinkedHashMap<Pattern, AssociationElement>();
  LinkedHashMap<String, AssociationElement> type2association = new LinkedHashMap<String, AssociationElement>();
  LinkedHashMap<String,StyleElement> stylename2styleElement = new LinkedHashMap<String,StyleElement>();

  private static final String SYM_TO_STYLE_PROPERTY_KEY = Stylesheet.class.getName();

	@Override
  public Object clone() throws CloneNotSupportedException {
    Stylesheet clone = (Stylesheet) super.clone();
    clone.meth2association = new LinkedHashMap<String, AssociationElement>();
    clone.meth2association.putAll(meth2association);
    clone.regex2association = new LinkedHashMap<Pattern, AssociationElement>();
    clone.regex2association.putAll(regex2association);
    clone.type2association = new LinkedHashMap<String, AssociationElement>();
    clone.type2association.putAll(type2association);
    clone.stylename2styleElement = new LinkedHashMap<String,StyleElement>();
    clone.stylename2styleElement.putAll(stylename2styleElement);
    return clone;
  }


  StyleElement getStyleByName(String name) {
    return stylename2styleElement.get(name);
  }

  /** Creates a new style.  If one with the given name already exists, it will
   *  be obliterated and replaced by this new one.
   */
  StyleElement createStyle(String name, boolean add_to_index) {

    StyleElement se = getStyleByName(name);
    if (se == null) {
      se = new StyleElement();
      se.name = name;
    }

    if (add_to_index) {
      addToIndex(se);
    }

    return se;
  }

  void addToIndex(StyleElement se) {
    if (se.name != null && se.name.trim().length() > 0) {
      stylename2styleElement.put(se.name, se);
    }
  }


  /**
   *  Tries to find a styleElement for the given seq symmetry.
   *  First looks for a styleElement stored in sym.getProperty(SYM_TO_STYLE_PROPERTY_KEY).
   *  Second looks for a match by feature type (such as an ontology term).
   *  Third looks for a match by feature "method" (i.e. the tier name).
   */
  DrawableElement getDrawableForSym(SeqSymmetry sym) {
		DrawableElement drawable = null;
		if (sym instanceof SymWithProps) {
			SymWithProps proper = (SymWithProps) sym;
			Object o = proper.getProperty(SYM_TO_STYLE_PROPERTY_KEY);
			if (o instanceof DrawableElement) {
				drawable = (DrawableElement) o;
			}
		}
		if (drawable == null) {
			// TODO: not certain of the purpose in this.  Does removing this have any effect?
			/*if (sym instanceof Das2FeatureRequestSym) {
				Das2FeatureRequestSym d2r = (Das2FeatureRequestSym) sym;
				String type = d2r.getType();
				drawable = getAssociationForType(type);
			} else
			 */
			 if (sym instanceof GFF3Sym) {
				GFF3Sym gff = (GFF3Sym) sym;
				String type = gff.getFeatureType();
				drawable = getAssociationForType(type);
			}
		}
		if (drawable == null) {
			drawable = getAssociationForMethod(BioSeq.determineMethod(sym));
		}
		if (drawable == null) {
			drawable = getDefaultStyleElement();
		}
		return drawable;
	}


  public AssociationElement getAssociationForMethod(String meth){
    if (meth == null) {
      return null;
    }
    AssociationElement association = null;
    // First try to match styleElement based on exact name match
    association = meth2association.get(meth);
    // Then try to match styleElement from regular expressions
    if (association == null) {
      List<Pattern> keyset = new ArrayList<Pattern>(regex2association.keySet());

      // Look for a matching pattern, going backwards, so that the
      // patterns from the last preferences read take precedence over the
      // first ones read (such as the default prefs).  Within a single
      // file, the last matching pattern will trump any earlier ones.
      for (int j=keyset.size()-1 ; j >= 0 && association == null; j--) {
        Pattern regex = keyset.get(j);
        if (regex.matcher(meth).find()) {
          association = regex2association.get(regex);
          // Put the stylename in meth2stylename to speed things up next time through.
          meth2association.put(meth, association);
        }
      }
    }
    return association;
  }

  public AssociationElement getAssociationForType(String type){
    return type2association.get(type);
  }

  private StyleElement default_style;

  StyleElement getDefaultStyleElement() {
    if (default_style == null) {
      // Create a default style that is just boxes inside boxes...
      default_style = new StyleElement();
      default_style.name = "<system-default-style>";
      default_style.glyphElement = new GlyphElement();
      default_style.glyphElement.setType(GlyphElement.TYPE_BOX);
      default_style.glyphElement.childrenElement = new ChildrenElement();
      default_style.glyphElement.childrenElement.styleElement = default_style;
    }
    return default_style;
  }

  public StringBuffer appendXML(String indent, StringBuffer sb) {
    sb.append("<?xml version=\"1.0\"?>\n");

    sb.append("<!DOCTYPE STYLESHEET SYSTEM \"igb_stylesheet_1.dtd\">\n");

    sb.append("\n");
    sb.append(indent).append("<STYLESHEET>\n");

    sb.append("\n");
    sb.append(indent).append("<STYLES>");
    Iterator iter = this.stylename2styleElement.values().iterator();
    while (iter.hasNext()) {
      sb.append("\n");
      StyleElement styleElement = (StyleElement) iter.next();
      styleElement.appendXML(indent + "  ", sb);
    }
    sb.append("\n").append(indent).append("</STYLES>\n");

    sb.append("\n");
    sb.append(indent).append("<ASSOCIATIONS>\n");

    List<AssociationElement> associations = new ArrayList<AssociationElement>();
    associations.addAll(meth2association.values());
    associations.addAll(regex2association.values());
    associations.addAll(type2association.values());

    Iterator a_iter = associations.iterator();
    while (a_iter.hasNext()) {
      AssociationElement ae = (AssociationElement) a_iter.next();
      ae.appendXML(indent+"  ",sb);
    }

    sb.append(indent).append("</ASSOCIATIONS>\n");
    sb.append("\n");

    sb.append(indent).append("</STYLESHEET>\n");
    sb.append("\n");
    return sb;
  }

  StyleElement getWrappedStyle(String name) {
    StyleElement se = new WrappedStyleElement(name);
    return se;
  }

  final static class WrappedStyleElement extends StyleElement {
    public static String NAME = "USE_STYLE";

    private WrappedStyleElement(String name) {
      super();
      this.name = name;
    }

    private StyleElement getReferredStyle(Stylesheet ss) {
      StyleElement se = ss.getStyleByName(name);
      if (se == null) {
        se = ss.getDefaultStyleElement();
      }
      return se;
    }
        @Override
    public GlyphI symToGlyph(SeqMapView gviewer, SeqSymmetry sym, GlyphI container,
        Stylesheet stylesheet, PropertyMap context) {
      StyleElement referredStyle = getReferredStyle(stylesheet);

      GlyphI containerGlyph = ChildrenElement.findContainer(container, this.childContainer);

      return referredStyle.symToGlyph(gviewer, sym, containerGlyph, stylesheet, context);
    }

        @Override
    public StringBuffer appendXML(String indent, StringBuffer sb) {
      sb.append(indent).append('<').append(NAME);
      XmlStylesheetParser.appendAttribute(sb, ATT_NAME, name);
      sb.append("\"/>\n");
      return sb;
    }
  }
}
