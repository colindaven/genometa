/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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
package genoviz.tutorial;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.MapGlyphFactory;
import com.affymetrix.genoviz.bioviews.SiblingCoordAvoid;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.LabelGlyph;
import com.affymetrix.genoviz.glyph.LabelledRectGlyph;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.parser.ContentParser;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import org.xml.sax.*;
import javax.xml.parsers.*;
import java.io.*;
import java.util.*;
import org.xml.sax.helpers.DefaultHandler;

/**
 *
 * @version $Id: MapHandler.java 5443 2010-03-05 19:58:52Z sgblanch $
 */
public class MapHandler extends DefaultHandler implements ContentParser {

	protected NeoMap map = new NeoMap(true, false);
	protected XMLReader xmlParser;
	private Hashtable<String, Integer> positions = new Hashtable<String, Integer>();
	private Hashtable<String, MapGlyphFactory> featureTypes = new Hashtable<String, MapGlyphFactory>();
	private Hashtable<String, Integer> featureLabels = new Hashtable<String, Integer>();
	private Hashtable<GlyphI, String> links = new Hashtable<GlyphI, String>();
	private Hashtable<GlyphI, String> targets = new Hashtable<GlyphI, String>();
	private MapGlyphFactory labelFactory, glyphFactory;
	private Hashtable<String, NeoMap> maps = new Hashtable<String, NeoMap>(7);
	private Stack<NeoMap> mapStack = new Stack<NeoMap>();

	public MapHandler() {
		super();

		this.positions.put("left", new Integer(LabelGlyph.LEFT));
		this.positions.put("right", new Integer(LabelGlyph.RIGHT));
		this.positions.put("above", new Integer(LabelGlyph.ABOVE));
		this.positions.put("below", new Integer(LabelGlyph.BELOW));
		this.positions.put("center", new Integer(LabelGlyph.CENTER));
	}

	/**
	 * importContent parses an xml formated stream and creates
	 * and returns a NeoMap object that contains the objects
	 * specified by the stream.
	 *
	 * @param theInput 
	 * @return
	 * @throws IOException
	 */
	public Object importContent(Reader theInput) throws IOException {
		if (null == xmlParser) {
			try {
				xmlParser = SAXParserFactory.newInstance().newSAXParser().getXMLReader();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		try {
			xmlParser.setContentHandler(this);
			map.clearWidget();
			xmlParser.parse(new InputSource(theInput));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return map;
	}

	/**
	 * converts an <code>InputStream</code> to a <code>Reader</code>
	 * and then parses.
	 *
	 * @throws IOException
	 */
	public Object importContent(InputStream theInput) throws IOException {
		return importContent(new InputStreamReader(theInput));
	}

	/**
	 * exportContent needed to implement ContentParser
	 * Here the signature is empty because we are only interested
	 * in providing the importContent functionality associated
	 * with ContentParser.
	 *
	 * @throws IOException
	 */
	public void exportContent(OutputStream theOutput,
			Object o) throws IOException {
	}

	/**
	 * sets the widget (in our case a NeoMap).
	 *
	 * @param theWidget 
	 */
	public void setWidget(NeoAbstractWidget theWidget) {
		System.out.println("setting widget");

		if (theWidget instanceof NeoMap) {
			System.out.println("it's a map");

			this.map = (NeoMap) theWidget;
			this.labelFactory = this.map.addFactory("-glyphtype LabelGlyph");
		}
	}

	/**
	 * Adds an alias for the "map" tag.
	 * You can call this multiple times
	 * thereby directing glyphs to multiple maps.
	 * 
	 * @param theName a unique name.
	 * @param theMap on which glyphs are to be placed.
	 */
	public void addMap(String theName, NeoMap theMap) {
		System.out.println("adding map type " + theName);
		this.maps.put(theName, theMap);
	}

	/**
	 * Handle the start of the document.
	 * @see org.xml.sax.DocumentHandler#startDocument
	 */
	@Override
	public void startDocument() {
		System.out.println("Start document");
	}

	/**
	 * Handle the end of the document.
	 * @see org.xml.sax.DocumentHandler#endDocument
	 */
	@Override
	public void endDocument() {
		System.out.println("End document");
	}
	private Stack<GlyphI> parents = new Stack<GlyphI>();

	/**
	 * Handle the start of an element.
	 * @param uri 
	 * @param localName
	 * @param name
	 * @param attributes
	 * @see org.xml.sax.DocumentHandler#startElement
	 */
	@Override
	public void startElement(String uri, String localName, String name, Attributes attributes) {
		GlyphI lastItem = null;
		LabelGlyph label = null;
		NeoMap nextMap = null;

		System.out.println("start:  " + name);

		// First look to see if this has been defined earlier.
		MapGlyphFactory fac = this.featureTypes.get(name);

		if (null != fac) { // It has.
			String v;
			int glyphBegin = 0, glyphEnd = 1;

			v = attributes.getValue("start");
			if (null != v) {
				glyphBegin = Integer.parseInt(v);
			}

			v = attributes.getValue("end");
			if (null != v) {
				glyphEnd = Integer.parseInt(v);
			}

			v = attributes.getValue("config");
			if (null != v) {
				lastItem = this.map.addItem(fac, glyphBegin, glyphEnd, v);
			} else {
				lastItem = this.map.addItem(fac, glyphBegin, glyphEnd);
			}

			v = attributes.getValue("name");
			if (null != v) {
				if (lastItem instanceof LabelledRectGlyph) {
					((LabelledRectGlyph) lastItem).setText(v);
				} else {
					label = (LabelGlyph) this.map.addItem(this.labelFactory, 0, 1);

					label.setText(v);
					label.setLabeledGlyph(lastItem);

					Object p = getLabelPosition(attributes);

					if (null == p) {
						p = featureLabels.get(name);
					}

					if (null != p && p instanceof Integer) {
						int i = ((Integer) p).intValue();

						label.setPlacement(i);
					}
				}
			}

			v = attributes.getValue("href");
			if (null != v) {
				links.put(lastItem, v);
			}

			v = attributes.getValue("target");
			if (null != v) {
				targets.put(lastItem, v);
			}
		} else if (name.equals("featureType")) {  // defining a new glyph style
			String v;

			v = attributes.getValue("name");
			if (null != v) {
				String fName = v;

				v = attributes.getValue("config");

				Integer p = getLabelPosition(attributes);

				defineGlyphStyle(this.map, fName, v, p);
			}
		} else if (name.equals("axis")) {
			String v;
			int axisOffset = 0;

			v = attributes.getValue("offset");
			if (null != v) {
				axisOffset = Integer.parseInt(v);
			}

			this.map.addAxis(axisOffset);
		} else if (null != (nextMap = this.maps.get(name))) {
			System.out.println("got map alias");

			if (null != this.map) {
				System.out.println("pushing map");
				this.mapStack.push(this.map);
			}

			setWidget(nextMap);

			String v;
			int mapMin = 0, mapMax = 100;
			int mapTop = -100, mapBottom = 100;

			v = attributes.getValue("min");
			if (null != v) {
				System.out.println("min set to " + v);

				mapMin = Integer.parseInt(v);
			}

			v = attributes.getValue("max");
			if (null != v) {
				System.out.println("max set to " + v);
				mapMax = Integer.parseInt(v);
			}

			this.map.setMapRange(mapMin, mapMax);

			v = attributes.getValue("top");
			if (null != v) {
				mapTop = Integer.parseInt(v);
			}

			v = attributes.getValue("bottom");
			if (null != v) {
				mapBottom = Integer.parseInt(v);
			}

			v = attributes.getValue("color");
			if (null != v) {
				com.affymetrix.genoviz.util.NeoColorMap cm =
						com.affymetrix.genoviz.util.NeoColorMap.getColorMap();

				this.map.setBackground(cm.getColor(v));
			}

			this.map.setMapOffset(mapTop, mapBottom);

			v = attributes.getValue("config");
			if (null != v) {
				// This may not be needed because of glyph factory below.
				this.map.configure(v);
			} else {
				v = "";
			}

			this.glyphFactory = this.map.addFactory(v);
			this.featureTypes.put("glyph", glyphFactory);

			Integer p = getLabelPosition(attributes);
			if (null != p) {
				this.featureLabels.put("glyph", p);
			}
		} else { // Not a recognized tag.

			// Push a null on the stack so we dont get parental relationships confused.
			System.out.println("unknown element: \"" + name + "\"");
			this.parents.push(null);
		}

		if (!parents.empty() && null != lastItem) {
			Object o = this.parents.peek();

			if (null != o && o instanceof GlyphI) {
				GlyphI g = (GlyphI) o;

				this.map.addItem(g, lastItem);

				if (null != label) {
					this.map.addItem(g, label);
				}
			}
		}

		if (null != lastItem) {
			this.parents.push(lastItem);
		}
	}

	/**
	 * @param theGlyph
	 * @return a URL if an href attribute was specified for this glyph.
	 * null otherwise.
	 */
	public String getHRef(GlyphI theGlyph) {
		Object o = links.get(theGlyph);

		if (null == o) {
			return null;
		}

		return (String) o;
	}

	/**
	 * @param theGlyph 
	 * @return a target if one was specified for this glyph.
	 * null otherwise.
	 */
	public String getTarget(GlyphI theGlyph) {
		Object o = targets.get(theGlyph);

		if (null == o) {
			return null;
		}

		return (String) o;
	}

	/**
	 * defines a glyph style.
	 * 
	 * @param theMap 
	 * @param name of the style
	 * @param config string to pass to the glyph factory
	 * @param labelPlacement LEFT, RIGHT, ABOVE, BELOW, or CENTER
	 */
	public void defineGlyphStyle(NeoMap theMap, String name, String config,
			Integer labelPlacement) {
		if (null == name) {
			throw new NullPointerException("Need a name to define a glyph style");
		}

		MapGlyphFactory f;

		if (null == config) {
			f = theMap.addFactory("");
		} else {
			f = theMap.addFactory(config);
		}

		this.featureTypes.put(name, f);

		if (null != labelPlacement) {
			this.featureLabels.put(name, labelPlacement);
		}
	}

	/**
	 * Handle the end of an element.
	 * @param uri 
	 * @param localName 
	 * @param name
	 * @see org.xml.sax.DocumentHandler#endElement
	 */
	@Override
	public void endElement(String uri, String localName, String name) {
		System.out.println("end:  " + name);

		if (!parents.empty()) {
			GlyphI p = this.parents.pop();

			if (name.equals("chromosome")) {  // temporary
				p.setPacker(new SiblingCoordAvoid());
				ViewI view = this.map.getView();
			}
		}

		if (!mapStack.empty() && null != maps.get(name)) {
			this.map = mapStack.pop();
		}
	}

	/**
	 *
	 * @param ch
	 * @param start
	 * @param length
	 */
	@Override
	public void characters(char ch[], int start, int length) {
		System.out.println("characters:  \"" + escape(ch, start, length) + '"');

		GlyphI lastItem = null;

		try {
			lastItem = this.parents.peek();

			if (lastItem instanceof SequenceGlyph) {
				((SequenceGlyph) lastItem).setResidues(new String(ch, start,
						length));
			}
		} catch (EmptyStackException e) {
			System.out.println("No parents. " + e.getMessage());
		}
	}

	/**
	 * Handle ignorable whitespace.
	 * @param ch
	 * @param start
	 * @param length 
	 * @see org.xml.sax.DocumentHandler#ignorableWhitespace
	 */
	@Override
	public void ignorableWhitespace(char ch[], int start, int length) {
		System.out.println("Ignorable whitespace:  \"" + escape(ch, start, length) + '"');
	}

	/**
	 * Handle a processing instruction.
	 * @param target
	 * @param data 
	 * @see org.xml.sax.DocumentHandler#processingInstruction
	 */
	@Override
	public void processingInstruction(String target, String data) {
		System.out.println("Processing Instruction:  " + target + ' '
				+ escape(data.toCharArray(), 0, data.length()));
	}

	private Integer getLabelPosition(Attributes attributes) {
		Integer p = null;
		String v = attributes.getValue("labeled");

		if (null == v) {  // Allow English as well as American spelling.
			v = attributes.getValue("labelled");
		}

		if (null != v) {
			p = this.positions.get(v.toLowerCase());

			if (null == p) {
				System.err.println("Invalid label positioning " + v);
			}
		}

		return p;
	}

	/**
	 * Escape a string for printing.
	 */
	String escape(char ch[], int offset, int length) {
		StringBuffer out = new StringBuffer();

		for (int i = 0; i < length; i++) {
			switch (ch[offset + i]) {

				case '\\':
					out.append("\\\\");
					break;

				case '\n':
					out.append("\\n");
					break;

				case '\t':
					out.append("\\t");
					break;

				case '\r':
					out.append("\\r");
					break;

				case '\f':
					out.append("\\f");
					break;

				case '"':
					out.append("\\");
					break;

				default:
					out.append(ch[offset + i]);
					break;
			}
		}

		return out.toString();
	}
}
