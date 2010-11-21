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

import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.glyph.LabelGlyph;
import com.affymetrix.genoviz.bioviews.MapGlyphFactory;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.Hashtable;

public class SimpleMap2 extends SimpleMap0 {

	Hashtable positions = new Hashtable();
	MapGlyphFactory labelFactory;

	@SuppressWarnings("unchecked")
	public SimpleMap2() {
		positions.put("left", new Integer(LabelGlyph.LEFT));
		positions.put("right", new Integer(LabelGlyph.RIGHT));
		positions.put("above", new Integer(LabelGlyph.ABOVE));
		positions.put("below", new Integer(LabelGlyph.BELOW));
		labelFactory = map.addFactory("-glyphtype com.affymetrix.genoviz.glyph.LabelGlyph");
	}

	@Override
	protected void parseLine(int theLineNumber, StreamTokenizer theTokens)
		throws IOException
	{
		int token = theTokens.nextToken();
		if (StreamTokenizer.TT_WORD == token) { // We have the keyword.
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
				Object item = this.map.addItem(r[0], r[1]);
				parseLabel(theTokens, item);
			}
			else { // not a keyword.
				System.err.println("\"" + keyword + "\" is not a keyword.");
				return;
			}
		}
	}

	protected boolean parseLabel(StreamTokenizer theTokens, Object theLabeledGlyph)
		throws IOException
	{
		int token = theTokens.nextToken();
		switch (token) {
			default:
			case StreamTokenizer.TT_EOL:
				theTokens.pushBack();
				return false;
			case StreamTokenizer.TT_WORD:
				if (theTokens.sval.equalsIgnoreCase("labeled")) {
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
					LabelGlyph label = (LabelGlyph)map.addItem(labelFactory, 0, 0);
					label.setText(labelText);
					label.setLabeledGlyph((Glyph)theLabeledGlyph);
					label.setPlacement(location);
				}
				else {
					System.err.println("expected \"labeled\". Got \""
							+ theTokens.sval + "\".");
					theTokens.pushBack();
					return false;
				}
				break;
		}
		return true;
	}

	public static void main (String argv[]) {
		SimpleMap0 me = new SimpleMap2();
		Frame f = new Frame("GenoViz");
		f.add("Center", me);
		me.addFileMenuItems(f);

		f.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				Window w = (Window) e.getSource();
				w.dispose();
			}
			@Override
			public void windowClosed( WindowEvent e ) {
				System.exit( 0 );
			}
		} );

		f.pack();
		f.setBounds(20, 40, 400, 500);
		f.setVisible(true);//f.show();
	}

}
