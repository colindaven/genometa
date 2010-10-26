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

import com.affymetrix.genoviz.datamodel.Sequence;
import com.affymetrix.genoviz.parser.ContentParser;
import com.affymetrix.genoviz.parser.FastaSequenceParser;
import com.affymetrix.genoviz.widget.NeoSeq;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.genoviz.awt.NeoPanel;

import java.applet.Applet;
import java.awt.BorderLayout;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;
import java.net.MalformedURLException;

public class ParseOne extends Applet {

	protected NeoAbstractWidget widget;
	protected ContentParser parser;
	protected NeoPanel pan;

	public ParseOne() {
		widget = new NeoSeq();
		parser = new FastaSequenceParser();
		this.setLayout(new BorderLayout());
		pan = new NeoPanel();
		pan.setLayout(new BorderLayout());
		pan.add("Center", (NeoSeq) widget);
		add("Center", pan);
	}

	public void loadSequence(String theReference)
		throws MalformedURLException, IOException {
		URL url = new URL(getDocumentBase(), theReference);
		InputStream input = url.openStream();
		Sequence seq = (Sequence) parser.importContent(input);
		((NeoSeq)widget).setSequence(seq);
	}

}
