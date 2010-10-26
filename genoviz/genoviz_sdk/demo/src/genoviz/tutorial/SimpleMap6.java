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
import com.affymetrix.genoviz.widget.NeoAbstractWidget;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.awt.Component;
import java.io.*;

public class SimpleMap6 extends Applet {


	protected MapHandler parser;
	protected NeoPanel pan;

	@Override
	public void init() {

		parser = new MapHandler();

		this.setLayout( new BorderLayout() );
		pan = new NeoPanel();
		pan.setLayout( new BorderLayout() );
		add( "Center", pan );
		String param;
		param = getParameter( "config" );
		if ( null != param ) {
			parseInputString( param );
		}
	}

	public void parseInputString(String theString) {

		this.pan.removeAll();
		this.pan.invalidate();
		try {
			NeoAbstractWidget widget =
				(NeoAbstractWidget) parser.importContent( new StringReader( theString ) );
			this.pan.add( "Center", (Component) widget );
			this.pan.validate();
			widget.updateWidget();
		}
		catch ( IOException e ) {
			System.err.println( "Couldn't parse the string." );
			System.err.println( "  " + e.getMessage() );
		}
	}

}
