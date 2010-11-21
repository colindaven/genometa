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

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.XMLUtils;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.w3c.dom.*;
import org.xml.sax.InputSource;

/**
 *  Loads an XML document using the igb_stylesheet_1.dtd.
 */
public final class XmlStylesheetParser {

  private Stylesheet stylesheet = new Stylesheet();
  private static Stylesheet system_stylesheet = null;
  private static Stylesheet user_stylesheet = null;

  // This resource should in the top-level igb source directory, or top level of jar file
  private static final String system_stylesheet_resource_name = "/igb_system_stylesheet.xml";
  private static final String default_user_stylesheet_resource_name = "/default_user_stylesheet.xml";

  /** Set the system stylesheet to null, so that the next call to getSystemStylesheet()
   *  will re-load it from storage.
   */
  public static synchronized void refreshSystemStylesheet() {
    system_stylesheet = null;
  }

  private static synchronized Stylesheet getSystemStylesheet() {
	  if (system_stylesheet == null) {
		  InputStream istr = null;
		  try {
			  Logger.getLogger(XmlStylesheetParser.class.getName()).log(Level.INFO,
				"Loading system stylesheet from resource:" + system_stylesheet_resource_name);
			  XmlStylesheetParser parser = new XmlStylesheetParser();
			  // If using class.getResource... use name beginning with "/"
			  istr = XmlStylesheetParser.class.getResourceAsStream(system_stylesheet_resource_name);
			  // If using getContextClassLoader... use name NOT beginning with "/"
			  system_stylesheet = parser.parse(istr);
		  } catch (Exception e) {
			  System.out.println("ERROR: Couldn't initialize system stylesheet.");
			  e.printStackTrace();
			  system_stylesheet = null;
		  } finally {
			  GeneralUtils.safeClose(istr);
		  }
		  if (system_stylesheet == null) {
			  system_stylesheet = new Stylesheet();
		  }
	  }
	  return system_stylesheet;
  }

  /** Set the user stylesheet to null, so that the next call to getSystemStylesheet()
   *  will re-load it from storage.
   */
  public static synchronized void refreshUserStylesheet() {
    system_stylesheet = null;
    user_stylesheet = null;
  }

 public static synchronized Stylesheet getUserStylesheet() {
	 if (user_stylesheet == null) {
		 InputStream istr = null;
		 try {
			 XmlStylesheetParser parser = new XmlStylesheetParser();
			 // If using class.getResource... use name beginning with "/"
			 istr = XmlStylesheetParser.class.getResourceAsStream(default_user_stylesheet_resource_name);

			 // Initialize the user stylesheet with the contents of the system stylesheet
			 parser.stylesheet = (Stylesheet) getSystemStylesheet().clone();

			 // then load the user stylesheet on top of that
			 Logger.getLogger(XmlStylesheetParser.class.getName()).log(Level.INFO,
				"Loading user stylesheet from resource: " + default_user_stylesheet_resource_name);

			 user_stylesheet = parser.parse(istr);

		 } catch (Exception e) {
			 System.out.println("ERROR: Couldn't initialize user stylesheet.");
			 e.printStackTrace();
			 user_stylesheet = null;
		 } finally {
			 GeneralUtils.safeClose(istr);
		 }
		 if (user_stylesheet == null) {
			 user_stylesheet = new Stylesheet();
		 }
	 }
	 return user_stylesheet;
 }

  private Stylesheet parse(InputStream istr) throws IOException {
    InputSource insrc = new InputSource(istr);
    parse(insrc);
    return stylesheet;
  }

	private Stylesheet parse(InputSource insource) throws IOException {
		try {
			Document prefsdoc = XMLUtils.nonValidatingFactory().newDocumentBuilder().parse(insource);

			processDocument(prefsdoc);
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception ex) {
			IOException ioe = new IOException("Error processing stylesheet file");
			ioe.initCause(ex);
			throw ioe;
		}
		return stylesheet;
	}

  private void processDocument(Document prefsdoc) throws IOException {

    Element top_element = prefsdoc.getDocumentElement();
    String topname = top_element.getTagName();
    if (! (topname.equalsIgnoreCase("igb_stylesheet"))) {
      throw new IOException("Can't parse file: Initial Element is not <IGB_STYLESHEET>.");
    }
    NodeList children = top_element.getChildNodes();

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element)child;

          if (name.equalsIgnoreCase("import")) {
            processImport(el);
          }
          else if (name.equalsIgnoreCase("styles")) {
            processStyles(el);
          }
          else if (name.equalsIgnoreCase("associations")) {
            processAssociations(el);
          }
          else {
            cantParse(el);
          }
      }
    }
  }

  private static void cantParse(Element n) {
    System.out.println("WARNING: Stylesheet: Cannot parse element: " + n.getNodeName());
  }

  private static void cantParse(Element n, String msg) {
    System.out.println("WARNING: Stylesheet: Cannot parse element: " + n.getNodeName());
    System.out.println("        " + msg);
  }

  private static void notImplemented(String s) {
    System.out.println("WARNING: Stylesheet: Not yet implemented: " + s);
  }

  private static boolean isBlank(String s) {
    return (s == null || s.trim().length() == 0);
  }

  private static void processImport(Element el) throws IOException {
    notImplemented("<IMPORT>");
  }

  private void processAssociations(Element associations) throws IOException {

    NodeList children = associations.getChildNodes();

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;

        AssociationElement associationElement = null;

        if (name.equalsIgnoreCase(AssociationElement.TYPE_ASSOCIATION)) {
          String type = el.getAttribute(AssociationElement.ATT_TYPE);
          String style = el.getAttribute(AssociationElement.ATT_STYLE);
          if (isBlank(type) || isBlank(style)) {
            throw new IOException("ERROR in stylesheet: missing method or style in METHOD_ASSOCIATION");
          }
          associationElement = AssociationElement.getTypeAssocation(type, style);
          stylesheet.type2association.put(type, associationElement);
        }
        else if (name.equalsIgnoreCase(AssociationElement.METHOD_ASSOCIATION)) {
          String method = el.getAttribute(AssociationElement.ATT_METHOD);
          String style = el.getAttribute(AssociationElement.ATT_STYLE);
          if (isBlank(method) || isBlank(style)) {
            throw new IOException("ERROR in stylesheet: missing method or style in METHOD_ASSOCIATION");
          }
          associationElement = AssociationElement.getMethodAssocation(method, style);
          stylesheet.meth2association.put(method, associationElement);
        }
        else if (name.equalsIgnoreCase(AssociationElement.METHOD_REGEX_ASSOCIATION)) {
          String regex = el.getAttribute(AssociationElement.ATT_REGEX);
          String style = el.getAttribute(AssociationElement.ATT_STYLE);
          if (isBlank(regex) || isBlank(style)) {
            throw new IOException("ERROR in stylesheet: missing method or style in METHOD_ASSOCIATION");
          }
          try {
            Pattern pattern = Pattern.compile(regex);
            associationElement = AssociationElement.getMethodRegexAssocation(regex, style);
            stylesheet.regex2association.put(pattern, associationElement);
          } catch (PatternSyntaxException pse) {
            IOException ioe = new IOException("ERROR in stylesheet: Regular Expression not valid: '" +
                regex + "'");
            ioe.initCause(pse);
            throw ioe;
          }
        }
        else {
          cantParse(el);
        }

        //Now read the properties maps
        NodeList grand_children = child.getChildNodes();
        for (int j=0; j<grand_children.getLength(); j++) {
          Node grand_child = grand_children.item(j);
          if (grand_child instanceof Element) {
            if (grand_child.getNodeName().equalsIgnoreCase(PropertyMap.PROP_ELEMENT_NAME)) {
              processProperty((Element) grand_child, associationElement.propertyMap);
            } else {
              cantParse(el);
            }
          }
        }
      }
    }
  }

  private void processStyles(Element stylesNode) throws IOException {
    NodeList children = stylesNode.getChildNodes();

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;

        if (name.equalsIgnoreCase(StyleElement.NAME) || name.equalsIgnoreCase(Stylesheet.WrappedStyleElement.NAME)) {
          processStyle(el, true);
        }
      }
    }
  }

  private StyleElement processStyle(Element styleel, boolean top_level) throws IOException {

    // node name should be STYLE, COPY_STYLE or USE_STYLE
    String node_name = styleel.getNodeName();


    StyleElement se = null;
    if (StyleElement.NAME.equalsIgnoreCase(node_name)) {
      String styleName = styleel.getAttribute(StyleElement.ATT_NAME);
      se = stylesheet.createStyle(styleName, top_level);
      se.childContainer = styleel.getAttribute(StyleElement.ATT_CONTAINER);

    } else if (Stylesheet.WrappedStyleElement.NAME.equalsIgnoreCase(node_name)) {
      String styleName = styleel.getAttribute(StyleElement.ATT_NAME);
      if (styleName==null || styleName.trim().length()==0) {
        throw new IOException("Can't have a USE_STYLE element with no name");
      }

      se = stylesheet.getWrappedStyle(styleName);
      // Not certain this will work
      se.childContainer = styleel.getAttribute(StyleElement.ATT_CONTAINER);

      return se; // do not do any other processing on a USE_STYLE element
    } else {
      cantParse(styleel);
    }

    if (se == null) {
      cantParse(styleel);
    }

    if (top_level) {
      if (isBlank(se.getName())) {
        System.out.println("WARNING: Stylesheet: All top-level styles must have a name!");
      } else {
        stylesheet.addToIndex(se);
      }
    }

    NodeList children = styleel.getChildNodes();


    // there can be multiple <PROPERTY> children
    // There should only be one child <GLYPH> OR one or more <MATCH> and <ELSE> elements
    // <COPY_STYLE> is not supposed to have <PROPERTIES>, but it is allowed to here

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;

        if (name.equalsIgnoreCase(GlyphElement.NAME)) {
          GlyphElement ge2 = processGlyph(el);
          se.setGlyphElement(ge2);
        } else if (name.equalsIgnoreCase(PropertyMap.PROP_ELEMENT_NAME)) {
          processProperty(el, se.propertyMap);
        } else if (name.equalsIgnoreCase(MatchElement.NAME) || name.equalsIgnoreCase(ElseElement.NAME)) {
          MatchElement me = processMatchElement(el);
          se.addMatchElement(me);
        } else {
          cantParse(el);
        }
      }
    }

    return se;
  }

  private GlyphElement processGlyph(Element glyphel) throws IOException {
    GlyphElement ge = new GlyphElement();

    String type = glyphel.getAttribute(GlyphElement.ATT_TYPE);
    if (GlyphElement.knownGlyphType(type)) {
      ge.setType(type);
    } else {
      System.out.println("STYLESHEET WARNING: <GLYPH type='" + type + "'> not understood");
      ge.setType(GlyphElement.TYPE_BOX);
    }

    String position = glyphel.getAttribute(GlyphElement.ATT_POSITION);
    ge.setPosition(position);

    NodeList children = glyphel.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;

        if (name.equalsIgnoreCase(GlyphElement.NAME)) {
          GlyphElement ge2 = processGlyph(el);
          ge.addGlyphElement(ge2);
        } else if (name.equalsIgnoreCase(ChildrenElement.NAME)) {
          ChildrenElement ce = processChildrenElement(el);
          ge.setChildrenElement(ce);
        } else if (name.equalsIgnoreCase(PropertyMap.PROP_ELEMENT_NAME)) {
          processProperty(el, ge.propertyMap);
        } else {
          cantParse(el);
        }
      }
    }

    return ge;
  }

  private ChildrenElement processChildrenElement(Element childel) throws IOException {
    ChildrenElement ce = new ChildrenElement();

    String position = childel.getAttribute(ChildrenElement.ATT_POSITIONS);
    ce.setPosition(position);
    String container = childel.getAttribute(ChildrenElement.ATT_CONTAINER);
    ce.setChildContainer(container);

    NodeList children = childel.getChildNodes();
    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;

        if (name.equalsIgnoreCase(StyleElement.NAME) || name.equalsIgnoreCase(Stylesheet.WrappedStyleElement.NAME)) {
          StyleElement se = processStyle(el, false);
          ce.setStyleElement(se);
        } else if (name.equalsIgnoreCase(MatchElement.NAME) || name.equalsIgnoreCase(ElseElement.NAME)) {
          MatchElement me = processMatchElement(el);
          ce.addMatchElement(me);
        } else if (name.equalsIgnoreCase(PropertyMap.PROP_ELEMENT_NAME)) {
          processProperty(el, ce.propertyMap);
        } else {
          cantParse(el);
        }
      }
    }
    return ce;

  }

  private MatchElement processMatchElement(Element matchel) throws IOException {
    MatchElement me;

    if (MatchElement.NAME.equalsIgnoreCase(matchel.getNodeName())) {
      me = new MatchElement();
      String type = matchel.getAttribute(MatchElement.ATT_TEST);
      String param = matchel.getAttribute(MatchElement.ATT_PARAM);
      if (! isBlank(type)) {
        if (! MatchElement.knownTestType(type)) {
          cantParse(matchel, "Unknown test type, test='" + type + "'");
        }

        me.match_test = type;
        if (! isBlank(param)) {
          me.match_param = param;

          if (MatchElement.MATCH_BY_METHOD_REGEX.equals(type)) {
            try {
              me.match_regex = Pattern.compile(param);
            } catch (PatternSyntaxException pse) {
              IOException ioe = new IOException("ERROR in stylesheet: Regular Expression not valid: '" +
                  param + "'");
              ioe.initCause(pse);
              throw ioe;
            }
          }
        }
      }

    } else if (ElseElement.NAME.equalsIgnoreCase(matchel.getNodeName())) {
      // an "ELSE" element is just like MATCH,
      //  except that it always matches as true
      me = new ElseElement();
    } else {
      cantParse(matchel);
      me = new ElseElement(); // treat it like an ELSE element
    }

    NodeList children = matchel.getChildNodes();

    for (int i=0; i<children.getLength(); i++) {
      Node child = children.item(i);
      String name = child.getNodeName();
      if (child instanceof Element) {
        Element el = (Element) child;

        if (name.equalsIgnoreCase(StyleElement.NAME) || name.equalsIgnoreCase(Stylesheet.WrappedStyleElement.NAME)) {
          StyleElement se = processStyle(el, false);
          me.setStyle(se);
        } else if (name.equalsIgnoreCase(MatchElement.NAME) || name.equalsIgnoreCase(ElseElement.NAME)) {
          MatchElement me2 = processMatchElement(el);
          me.subMatchList.add(me2);
        } else if (name.equalsIgnoreCase(PropertyMap.PROP_ELEMENT_NAME)) {
          processProperty(el, me.propertyMap);
        } else {
          cantParse(el);
        }
      }
    }
    return me;
  }

  private void processProperty(Element properElement, PropertyMap propertied)
  throws IOException {
    String key = properElement.getAttribute(PropertyMap.PROP_ATT_KEY);
    String value = properElement.getAttribute(PropertyMap.PROP_ATT_VALUE);
    if (key == null) {
       throw new IOException("ERROR: key or value of <PROPERTY> is null");
    }
    propertied.setProperty(key, value);
  }

  private static String escapeXML(String s) {
    if (s==null) {
      return "";
    } else {
      return s.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt")
      .replaceAll("\"", "&quot;").replaceAll("'", "&apos;");
    }
  }


  static void appendAttribute(StringBuffer sb, String name, String value) {
    if (value != null && value.trim().length() > 0) {
      sb.append(" ").append(name).append("='").append(escapeXML(value)).append("'");
    }
  }
}

