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

import com.affymetrix.genoviz.bioviews.MapGlyphFactory;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.util.Hashtable;

public class SimpleMap4 extends SimpleMap3 {

	protected Hashtable featureTypes = new Hashtable();

	/*
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
					Object item = this.map.addItem(fac, r[0], r[1]);
					parseLabel(theTokens, item);
				}
			}
		}
	}
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

	public static void main (String argv[]) {
		SimpleMap4 me = new SimpleMap4();
		Frame f = new Frame("GenoViz");
		f.add("Center", me);
		// me.addFileMenuItems(f);

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
