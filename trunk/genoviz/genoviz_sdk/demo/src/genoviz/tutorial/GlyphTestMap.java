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
import com.affymetrix.genoviz.widget.NeoMap;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

public class GlyphTestMap extends JApplet implements ActionListener {

  NeoMap map;
  JButton clear;
  JButton add;
  JTextField start_field;
  JTextField end_field;
  JComboBox glyph_field;
  JComboBox color_field;

  String[] glyphtypes = {
    "com.affymetrix.genoviz.glyph.FillRectGlyph",
    "com.affymetrix.genoviz.glyph.OutlineRectGlyph",
    "com.affymetrix.genoviz.glyph.FillOvalGlyph", 
    "com.affymetrix.genoviz.glyph.ArrowGlyph"
  };

  String[] colors = { "black", "red", "green", "blue", "yellow" };

  public GlyphTestMap () {
    // false args to constructor turns off internal scrollbars
    map = new NeoMap(false, false);
    //    map.setMapColor(Color.PINK);
    // make sure map expands to encompass all glyph x coords
    map.setExpansionBehavior(NeoMap.X, NeoMap.EXPAND);
    // make sure map expands to encompass all glyph y coords
    map.setExpansionBehavior(NeoMap.Y, NeoMap.EXPAND);
    map.setMapRange(0, 500);
    map.setMapOffset(-20, 100);
    map.addAxis(0);
    this.getContentPane().add("Center", map);
    JPanel pan = new JPanel();
    start_field = new JTextField(5);
    end_field = new JTextField(5);
    start_field.setText("100");
    end_field.setText("200");
    glyph_field = new JComboBox(glyphtypes);
    color_field = new JComboBox(colors);
    add = new JButton("Add Glyph");
    add.addActionListener(this);
    clear = new JButton("Clear Map");
    clear.addActionListener(this);

    pan.add(new JLabel("start"));
    pan.add(start_field);
    pan.add(new JLabel("end"));
    pan.add(end_field);
    pan.add(glyph_field);
    pan.add(color_field);
    pan.add(add);
    pan.add(clear);
    this.getContentPane().add("North", pan);
  }


  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == clear)  {
      map.clearWidget();
      map.addAxis(0);
      map.updateWidget();
    }
    else if (src == add)  {
      String glyph_name = (String)glyph_field.getSelectedItem();
      String color_name = (String)color_field.getSelectedItem();
      try {
	int start = Integer.parseInt(start_field.getText());
	int end = Integer.parseInt(end_field.getText());
	Class glyph_class = Class.forName(glyph_name);
	Color col = NeoMap.getColor(color_name);
	MapGlyphFactory fac = map.getFactory();
	fac.setGlyphtype(glyph_class);
	fac.setBackgroundColor(col);
	fac.setForegroundColor(col);
	map.addItem(start, end);
	map.stretchToFit();  
	map.updateWidget();
      } catch (ClassNotFoundException ex) {
	System.err.println("could not find class: " + glyph_name);
      } catch (NumberFormatException ex)  {
	System.err.println("could not parse numeric text field");
      }
    }
  }


  public static void main (String argv[]) {
    GlyphTestMap me = new GlyphTestMap();
    JFrame frm = new JFrame("GenoViz SimpleMap Tutorial");
    frm.add("Center", me);
    frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frm.pack();
    frm.setBounds( 20, 40, 900, 400 );
    frm.setVisible(true);
  }

}
