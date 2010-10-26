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
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.datamodel.Range;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.IOException;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.Enumeration;
import java.util.Vector;

public class SimpleMap0 extends Applet implements ActionListener {

	protected NeoMap map;
	protected NeoPanel pan;

	public SimpleMap0 () {
		/*
		   These (false, false) arguments prevent the automatic addition of scrollbars.
		   As you will see in SimpleMap3.java, you can explicitly add any
		   java.awt.Adjustable as a scroller after you have created the NeoMap.
		   */
		map = new NeoMap(false, false);
		map.setMapRange(1, 100);
		map.setMapOffset(-50, 50);
		map.addAxis(0);
		setLayout(new BorderLayout());
		pan = new NeoPanel();
		pan.setLayout(new BorderLayout());
		pan.add("Center", map);
		add("Center", pan);
	}

	@Override
	public String getAppletInfo() {
		return("Simple Map Demo - genoviz Software, Inc.");
	}

	@Override
	public void init() {
		super.init();
		String s = getParameter("config");
		if (null != s) {
			parseInputString(s);
		}
	}

	@SuppressWarnings("unchecked")
	protected void addFileMenuItems(Frame theFrame) {
		int FILE = 0; // assuming the File menu is the first one.
		Menu fileMenu;
		MenuBar menuBar = theFrame.getMenuBar();
		if ( null == menuBar ) {
			menuBar = new MenuBar();
			fileMenu = new Menu( "File" );
			menuBar.add( fileMenu );
			theFrame.setMenuBar( menuBar );
		}
		fileMenu = menuBar.getMenu(FILE);
		Vector items = new Vector();
		if (null != fileMenu) {
			// Remove and save the items already on the menu.
			while (0 < fileMenu.getItemCount()) {
				items.addElement(fileMenu.getItem(0));
				fileMenu.remove(0);
			}
			// Add an Open item.
			MenuItem openMenuItem = new MenuItem("Open");
			openMenuItem.addActionListener(this);
			fileMenu.add(openMenuItem);
			// Replace the other items.
			Enumeration enm = items.elements();
			while (enm.hasMoreElements()) {
				Object o = enm.nextElement();
				fileMenu.add((MenuItem)o);
			}
		}
		else {
			System.err.println( "couldn't get file menu." );
		}
	}


	public void actionPerformed(ActionEvent theEvent) {
		Container f = this.getParent();
		while (null != f && !(f instanceof Frame)) {
			f = f.getParent();
		}
		if (null != f) {
			FileDialog dialog = new FileDialog((Frame)f,
					"Open File", FileDialog.LOAD);
			dialog.pack();
			dialog.setVisible(true);//dialog.show();
			if (null != dialog.getFile()) { // not cancelled
				this.map.clearWidget();
				this.map.addAxis(0);
				try {
					parseFile(new File(dialog.getDirectory(), dialog.getFile()));
				}
				catch (IOException e) {
				}
				this.map.updateWidget();
			}
		}
	}

	private void parseFile(File theFile) throws IOException {
		parseInput(new FileReader(theFile));
	}

	public void parseInputString(String theString) {
		this.map.clearWidget();
		this.map.addAxis(0);
		try {
			parseInput(new StringReader(theString));
		}
		catch (IOException e) {
		}
		this.map.updateWidget();
	}

	private void parseInput(Reader theReader) throws IOException {
		int lineNumber = 1;
		StreamTokenizer tokens = new StreamTokenizer(theReader);
		tokens.eolIsSignificant(true);
		int token;
		while (StreamTokenizer.TT_EOF != (token = tokens.nextToken())) {
			switch (token) {
				case StreamTokenizer.TT_WORD: // keyword
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

	protected void parseLine(int theLineNumber, StreamTokenizer theTokens)
		throws IOException
	{
		int token = theTokens.nextToken();
		if (StreamTokenizer.TT_WORD == token) { // We have the keyword.
			String keyword = theTokens.sval;
			if (keyword.equalsIgnoreCase("range")) {
				Range r = parseRange(theTokens);
				this.map.setMapRange(r.beg, r.end);
			}
			else if (keyword.equalsIgnoreCase("offsets")) {
				Range r = parseRange(theTokens);
				this.map.setMapOffset(r.beg, r.end);
			}
			else if (keyword.equalsIgnoreCase("glyph")) {
				int[] r = null;
				r = parseDirectedRange(theTokens);
				String configuration = parseString(theTokens);
				this.map.configure(configuration);
				this.map.addItem(r[0], r[1]);
			}
			else { // not a keyword.
				System.err.println("\"" + keyword + "\" is not a keyword.");
				return;
			}
		}
	}

	/**
	 * parses a pair of integers
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
	 * parses a pair of integers
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
		int[] r = new int[2];
		int token = theTokens.nextToken();
		if (StreamTokenizer.TT_NUMBER == token) {
			r[0] = (int) theTokens.nval;
			token = theTokens.nextToken();
			if (StreamTokenizer.TT_NUMBER == token) {
				r[1] = (int) theTokens.nval;
				return r;
			}
		}
		return null;
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
				System.err.println("expected a string");
				return "";
		}
	}


	public static void main (String argv[]) {
		SimpleMap0 me = new SimpleMap0();
		Frame f = new Frame("GenoViz");
		f.add("Center", me);

		f.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
				Window w = (Window) e.getSource();
				w.dispose();
			}
			public void windowClosed( WindowEvent e ) {
				System.exit( 0 );
			}
		} );
		me.addFileMenuItems(f);
		f.pack();
		f.setBounds( 20, 40, 400, 500 );
		f.setVisible(true);//f.show();
	}

}
