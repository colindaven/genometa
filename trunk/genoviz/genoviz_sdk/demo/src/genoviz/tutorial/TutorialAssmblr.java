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

import java.io.*;
import com.affymetrix.genoviz.widget.NeoAssembler;
import javax.swing.JApplet;


// public class TutorialAssmblr extends Applet {
public class TutorialAssmblr extends JApplet {

	NeoAssembler assmblr;

	@Override
	public void init() {
	    NeoAssembler.use_neo_scroll = false;
	    NeoAssembler.use_neo_zoom = false;
		assmblr = new NeoAssembler();
		assmblr.setAutoSort(false);
		this.getContentPane().add("Center", assmblr);
		String param;
		//		param = getParameter("assembly");
		//		if (param != null) {
		//			parseInputString(param);
		//		}
	}

	public void parseInputString(String theSource) {
		this.assmblr.clearWidget();
		try {
			parseInput(new BufferedReader(new StringReader(theSource)));
		}
		catch (Exception e) {
		    e.printStackTrace();
		}
		assmblr.stretchToFit(true, false);
		assmblr.zoom(NeoAssembler.Y, 1.0);
		assmblr.scroll(NeoAssembler.Y, 0);
		assmblr.updateWidget();
	}

    public void parseInput(Reader in) throws Exception {
	BufferedReader buf = new BufferedReader(in);
	boolean first_seq = true;
	String line;
	StringBuffer params = new StringBuffer();
	int offset = 0;
	int numlines;
	boolean forward = true;

	while ((line = buf.readLine()) != null) {
	    System.out.println(line);
	    String[] fields = line.split(":*\\s+");
	    if (fields.length >= 2)  {
		String name = fields[0];
		String gapped_residues = fields[1];
		if (first_seq) {
		    assmblr.setGappedConsensus(name, gapped_residues, offset, forward);
		    first_seq = false;
		}
		else {
		    assmblr.addGappedSequence(name, gapped_residues, offset, forward);
		}
		System.out.println("added: " + name);
	    }
	}
	in.close();
    }

}
