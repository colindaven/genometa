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

import com.affymetrix.genoviz.glyph.StringGlyph;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class SimpleMap1 extends GlyphTestMap {

  protected void parseInput(Reader input)  {
    String line;
    MapGlyphFactory fac = map.getFactory();
    try  {
      BufferedReader binput = new BufferedReader(input);
      while ((line = binput.readLine()) != null)  {
	String[] fields = line.split("\\s+");
	if (fields.length >= 3)  {
	  String type = fields[0];
	  int start = Integer.parseInt(fields[1]);
	  int end = Integer.parseInt(fields[2]);
	  if (type.equals("foo"))  { 
	    fac.setGlyphtype(com.affymetrix.genoviz.glyph.FillRectGlyph.class);
	    fac.setBackgroundColor(Color.blue);
	  }
	  else if (type.equals("bar"))  {
	    fac.setGlyphtype(com.affymetrix.genoviz.glyph.OutlineRectGlyph.class);
	    fac.setForegroundColor(Color.red);
	    fac.setBackgroundColor(Color.green);
	  }
	  else if (type.equals("baz"))  {
	    fac.setGlyphtype(com.affymetrix.genoviz.glyph.ArrowGlyph.class);
	    fac.setBackgroundColor(Color.green);
	  }
	  else {
	    fac.setGlyphtype(com.affymetrix.genoviz.glyph.FillRectGlyph.class);
	    fac.setBackgroundColor(Color.black);
	  }
	  GlyphI glyph_to_label = map.addItem(start, end);
	  // fac.setGlyphtype(com.affymetrix.genoviz.glyph.LabelGlyph.class);
	  //	  LabelGlyph label = (LabelGlyph)map.addItem(0,0);
	  StringGlyph label = new StringGlyph();
	  label.setString(type);
	  label.setForegroundColor(Color.red);
	  label.setBackgroundColor(Color.gray);
//	  label.setLabelledGlyph(glyph_to_label);
//	  label.setPlacement(LabelGlyph.BELOW);
	  map.addItem(label);
	}
      }
      map.repack();
      map.updateWidget();
    }
    catch (IOException ex)  {
      System.err.println("problem with parsing input, possibly syntax error?");
      ex.printStackTrace();
    }
  } 


  public static void main (String argv[]) {
    SimpleMap1 me = new SimpleMap1();
    JFrame frm = new JFrame("GenoViz SimpleMap Tutorial");
    frm.add("Center", me);
    frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//    me.addFileMenuItems(frm);
    frm.pack();
    frm.setBounds(20, 40, 900, 400);
    frm.setVisible(true);
  }

 
}
