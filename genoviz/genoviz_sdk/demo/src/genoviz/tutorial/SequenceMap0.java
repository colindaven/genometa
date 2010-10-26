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

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.widget.NeoSeq;

import java.awt.Frame;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.StreamTokenizer;

/**
 *
 * @version $Id: SequenceMap0.java 4917 2010-01-04 22:37:23Z hiralv $
 */
public class SequenceMap0 extends SimpleMap4 {

	protected NeoSeq seq = new NeoSeq();
	protected Frame seqFrame = null;
	protected NeoPanel pan = new NeoPanel();

	@Override
	public String getAppletInfo() {
		return "Simple Sequence Map - genoviz Software, Inc.";
	}

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
			else if (keyword.equalsIgnoreCase("Sequence")) {
				String s = parseSequence(theTokens);
				seq.setResidues(s);
				if (null == seqFrame) {
					seqFrame = new Frame("sequence");
					pan.setLayout(new BorderLayout());
					pan.add("Center", (Component)seq);
					seqFrame.add("Center", pan);
					seqFrame.setSize(300, 200);
					seqFrame.show();
				}
			}
			else {
				MapGlyphFactory fac = (MapGlyphFactory)this.featureTypes.get(keyword);
				if (null == fac) { // not a keyword.
					System.err.println("\"" + keyword + "\" is not a keyword.");
					return;
				}
				else { // a feature type
					int[] r = null;
					r = parseDirectedRange(theTokens);
					this.map.addItem(fac, r[0], r[1]);
				}
			}
		}
	}
       */

	@Override
	public void start() {
		super.start();
		if (null != seqFrame) {
			seqFrame.setVisible(true); //seqFrame.show();
		}
	}
	@Override
	public void stop() {
		super.stop();
		if (null != seqFrame) {
			seqFrame.setVisible(false);
		}
	}

	protected String parseSequence(StreamTokenizer theTokens)
		throws IOException
	{
		int token;
		theTokens.eolIsSignificant(false);
		StringBuffer sb = new StringBuffer("");
		while (StreamTokenizer.TT_WORD == (token = theTokens.nextToken()))
		{
			sb.append(theTokens.sval);
		}
		return sb.toString();
	}

	public static void main (String argv[]) {
		SequenceMap0 me = new SequenceMap0();
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
		f.setBounds( 20, 40, 300, 250 );
		f.setVisible(true);//f.show();
	}

}
