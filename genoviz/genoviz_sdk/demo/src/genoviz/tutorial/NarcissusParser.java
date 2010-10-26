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
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.glyph.LabelGlyph;
import com.affymetrix.genoviz.glyph.StringGlyph;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.parser.*;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoMap;

import java.awt.Adjustable;
import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.Reader;
import java.io.InputStreamReader;
import java.io.FileReader;
import java.net.URL;
import java.util.Hashtable;
import java.util.Stack;

/**
 * parses Narcissus format data for a NeoMap.
 */
public class NarcissusParser implements ContentParser {

	private NeoMap map;
	protected Hashtable positions = new Hashtable();
	protected MapGlyphFactory labelFactory;
	protected Hashtable featureTypes = new Hashtable();
	protected Adjustable zoomer;

	/**
	 * imports Narcissus format data from an internet.
	 *
	 * @param url the Uniform Resourse Locator pointing to the data.
	 * @return a map.
	 */

	public Object importContent(URL url) {
		Object results = null;
		try {
			InputStream istream = url.openStream();
			results = importContent(istream);
			istream.close();
		}
		catch(Exception ex) { 
			System.err.println(ex.getMessage());
		}
		return results;
	}

	/**
	 * constructs a parser
	 */
	@SuppressWarnings("unchecked")
	public NarcissusParser () {
		positions.put("left", new Integer(LabelGlyph.LEFT));
		positions.put("right", new Integer(LabelGlyph.RIGHT));
		positions.put("above", new Integer(LabelGlyph.ABOVE));
		positions.put("below", new Integer(LabelGlyph.BELOW));
	}

	/**
	 * parses data from a file.
	 *
	 * @param theFile
	 * @see #parseInputStream
	 */
	private void parseFile(File theFile) throws IOException {
		parseInputStream(new FileReader(theFile));
	}

	public Object importContent( Reader theInput ) throws IOException {
		parseInputStream( theInput );
		return this.map;
	}

	/**
	 * converts an <code>InputStream</code> to a <code>Reader</code>
	 * and then parses.
	 */
	public Object importContent( InputStream theInput )
		throws IOException {
		return importContent( new InputStreamReader( theInput ) );
	}

	/**
	 * parses an input stream in Narcissus format.
	 * Narcissus format consists of ASCII encoded text.
	 * Each line starts with a keyword, "{", or "}".
	 * Keywords are "FeatureType", "Glyph", "Offsets", and "Range".
	 * New keywords can be added via the "FeatureType" keyword.
	 * Braces are used to collect child glyphs under a parent glyph.
	 * Glyphs can be labeled
	 * by appending "labeled <var>side</var> "<var>text</var>""
	 * to the line.
	 * Where <var>side</var> can be "above", "below", "left", or "right".
	 * e.g.
	 *
	 * <pre>
	 *  range 0 100
	 *  offsets -100 100
	 *  featureType gene "-glyphtype LineContainerGlyph -color black"
	 *  featureType exon "-glyphtype FillRectGlyph -color blue"
	 *  gene 10 90 labeled below "A Gene"
	 *  {
	 *    exon 20 40
	 *    exon 50 70
	 *  }
	 * </pre>
	 *
	 * @param theStream from whence the data come.
	 */
	private void parseInputStream(Reader theStream) throws IOException {

		if (null != this.map) {
			System.out.println("clearing old widget");
			this.map.clearWidget();
			this.map.updateWidget();
		}
		this.map = new NeoMap();
		this.labelFactory = this.map.addFactory("-glyphtype LabelGlyph");
		this.map.setMapRange(1, 100);
		this.map.setMapOffset(-50, 50);
		this.map.addAxis(0);

		int lineNumber = 1;
		StreamTokenizer tokens = new StreamTokenizer(theStream);
		tokens.eolIsSignificant(true);
		int token;
		while (StreamTokenizer.TT_EOF != (token = tokens.nextToken())) {
			switch (token) {
				case StreamTokenizer.TT_WORD: // keyword
				case '{': // child glyphs coming.
				case '}': // no more children.
					tokens.pushBack();
					parseLine(lineNumber, tokens);
					break;
				case StreamTokenizer.TT_EOL: // reset
					while (StreamTokenizer.TT_EOF != token
							&& StreamTokenizer.TT_EOL != token)
					{
						token = tokens.nextToken();
						switch (token) {
							case StreamTokenizer.TT_NUMBER:
								System.err.print(" " + tokens.nval);
								break;
							case StreamTokenizer.TT_WORD:
								System.err.print(" " + tokens.sval);
								break;
							case '"':
								System.err.print(" \"" + tokens.sval + "\"");
								break;
							default:
								System.err.print(" " + (char)token);
								break;
							case StreamTokenizer.TT_EOL:
								System.err.println(" <END OF LINE>");
								break;
							case StreamTokenizer.TT_EOF:
								System.err.println(" <END OF FILE>");
						}
					}
					lineNumber++;
			}
		}
	}


	/**
	 * parses a label.
	 */
	protected boolean parseLabel(
			StreamTokenizer theTokens, Object theLabeledGlyph)
		throws IOException
	{
		int token = theTokens.nextToken();
		switch (token) {
			default:
			case StreamTokenizer.TT_EOL:
				theTokens.pushBack();
				return false;
			case StreamTokenizer.TT_WORD:
				String action = theTokens.sval.toLowerCase();

				if (theTokens.sval.equalsIgnoreCase("labeled") ||
						theTokens.sval.equalsIgnoreCase("labelled")) {
					int location = LabelGlyph.LEFT;
					token = theTokens.nextToken();
					switch (token) {
						default:
							theTokens.pushBack();
							return false;
						case '"':
							theTokens.pushBack();
							break;
						case StreamTokenizer.TT_WORD:
							Object p = positions.get(theTokens.sval.toLowerCase());
							if (null == p) {
								theTokens.pushBack();
							}
							else {
								location = ((Integer)p).intValue();
							}
							break;
					}
					String labelText = parseString(theTokens);
					this.map.configure("-glyphtype LabelGlyph");
					LabelGlyph label = (LabelGlyph)this.map.addItem(0, 0);
					label.setText(labelText);
					label.setLabeledGlyph((GlyphI)theLabeledGlyph);
					label.setPlacement(location);
						}
				else if (theTokens.sval.equalsIgnoreCase("contains")) {

					String text = parseString(theTokens);
					if (theLabeledGlyph instanceof StringGlyph) {
						((StringGlyph)theLabeledGlyph).setString(text);
						((StringGlyph)theLabeledGlyph).setPlacement( NeoConstants.CENTER );
						((StringGlyph)theLabeledGlyph).setForegroundColor( Color.red );
					}
					else if (theLabeledGlyph instanceof SequenceGlyph) {
						((SequenceGlyph)theLabeledGlyph).setResidues(text);
						((SequenceGlyph)theLabeledGlyph).setOrientation(NeoConstants.HORIZONTAL);
						((SequenceGlyph)theLabeledGlyph).setForegroundColor( Color.green );
						((SequenceGlyph)theLabeledGlyph).setBackgroundColor( Color.blue );
					}
					else {
						System.err.println( "Cannot use contains modifier on non-string glyph" );
					}
				}
				else {
					System.err.println("expected \"labeled\" or \"contains\". Got \""
							+ theTokens.sval + "\".");
					theTokens.pushBack();
					return false;
				}
				break;
		}
		return true;
	}


	/**
	 * parses a pair if integers
	 * representing the beginning and end
	 * of a range of integers.
	 *
	 * @return a Range
	 * @see com.affymetrix.genoviz.datamodel.Range
	 */
	protected Range parseRange(StreamTokenizer theTokens)
		throws IOException
	{
		int begin, end;
		int token = theTokens.nextToken();
		if (StreamTokenizer.TT_NUMBER == token) {
			begin = (int) theTokens.nval;
			token = theTokens.nextToken();
			if (StreamTokenizer.TT_NUMBER == token) {
				end = (int) theTokens.nval;
				return new Range(begin, end);
			}
		}
		return null;
	}

	/**
	 * parses a pair if integers
	 * representing the beginning and end
	 * of a directed range of integers.
	 * The range is "directed"
	 * in that the beginning may be greater than the end.
	 * This is useful for cofiguring directed glyphs
	 * like the ArrowGlyph.
	 *
	 * @return an array of two integers
	 */
	protected int[] parseDirectedRange(StreamTokenizer theTokens)
		throws IOException
	{
		int[] r = { 0, 1 };
		int token = theTokens.nextToken();
		if (StreamTokenizer.TT_NUMBER == token) {
			r[0] = (int) theTokens.nval;
			token = theTokens.nextToken();
			if (StreamTokenizer.TT_NUMBER == token) {
				r[1] = (int) theTokens.nval;
			}
			else {
				r[1] = r[0] + 1;
			}
		}
		return r;
	}

	protected String parseString(StreamTokenizer theTokens)
		throws IOException
	{
		int token = theTokens.nextToken();
		switch (token) {
			case StreamTokenizer.TT_WORD:
			case '"':
				return theTokens.sval;
			default:
				System.err.println("expected a string not "+ token +" " + theTokens.sval );
				return "";
		}
	}

	private Stack parents = new Stack();
	GlyphI lastItem = null;

	@SuppressWarnings("unchecked")
	protected void parseLine(int theLineNumber, StreamTokenizer theTokens)
		throws IOException
	{
		int token = theTokens.nextToken();
		switch (token) {
			case '{':
				parents.push(lastItem);
				break;
			case '}':
				lastItem = (GlyphI) parents.pop();
				break;
			case StreamTokenizer.TT_WORD: // We have a keyword.
				String keyword = theTokens.sval;
				if (keyword.equalsIgnoreCase("range")) {
					Range r = null;
					r = parseRange(theTokens);
					this.map.setMapRange(r.beg, r.end);
				}
				else if (keyword.equalsIgnoreCase("offsets")) {
					Range r = null;
					r = parseRange(theTokens);
					this.map.setMapOffset(r.beg, r.end);
				}
				else if (keyword.equalsIgnoreCase("glyph")) {
					int[] r = null;
					r = parseDirectedRange(theTokens);
					String configuration = parseString(theTokens);
					this.map.configure(configuration);
					lastItem = this.map.addItem(r[0], r[1]);
					if (!parents.empty()) {
						lastItem = this.map.addItem( (GlyphI)parents.peek(), lastItem );
					}
					parseLabel(theTokens, lastItem);
				}
				else if (keyword.equalsIgnoreCase("FeatureType")) {
					parseFeatureType(theTokens);
				}
				else {
					MapGlyphFactory fac = (MapGlyphFactory)featureTypes.get(keyword);
					if (null == fac) { // not a keyword.
						System.err.println("\"" + keyword + "\" is not a keyword.");
						return;
					}
					else { // a feature type
						int[] r = null;
						r = parseDirectedRange(theTokens);
						lastItem = this.map.addItem(fac, r[0], r[1]);
						if (!parents.empty()) {
							lastItem = this.map.addItem( (GlyphI)parents.peek(), lastItem );
						}
						parseLabel(theTokens, lastItem);
					}
				}
		}
	}

	/**
	 * parses a feature type and adds it to a list of such types.
	 * Once added, the feature type becomes a keyword.
	 */
	@SuppressWarnings("unchecked")
	protected void parseFeatureType(StreamTokenizer theTokens)
		throws IOException
	{
		int token = theTokens.nextToken();
		switch (token) {
			case StreamTokenizer.TT_WORD:
				String name = new String(theTokens.sval);
				token = theTokens.nextToken();
				switch (token) {
					case '"':
					case StreamTokenizer.TT_WORD:
						String config = new String(theTokens.sval);
						MapGlyphFactory f = this.map.addFactory(config);
						featureTypes.put(name, f);
						break;
					default:
						theTokens.pushBack();
				}
				break;
			default:
				theTokens.pushBack();
		}
	}

	public void exportContent(java.io.OutputStream s, java.lang.Object o) {
	}

	/**
	 * for testing.
	 */
	public static void main(String argv[]) {
		ContentParser p = new NarcissusParser();
		try {
			String s = "http://roma/~eric/dna.fasta";
			if (0 < argv.length) {
				s = argv[0];
			}
			java.net.URL url = new java.net.URL(s);
			InputStream istream = url.openStream();
			Object o = p.importContent(istream);
			if (null != o) {
				p.exportContent(System.out, o);
			}
			else {
				System.out.println( "nothing?" );
			}
		}
		catch ( Exception e ) {
			e.printStackTrace();
		}
	}
	/* */

}
