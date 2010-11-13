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
package genoviz.demo;

import com.affymetrix.genoviz.awt.AdjustableJSlider;
import java.applet.*;
import java.net.MalformedURLException;
import java.util.*;

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.glyph.*;
import com.affymetrix.igb.glyph.EfficientLabelledGlyph;
import com.affymetrix.genoviz.widget.NeoWidget;
import com.affymetrix.igb.glyph.EfficientLabelledGlyph;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.net.URL;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class GraphDemo extends JApplet implements ComponentListener{

	NeoMap map;
	AdjustableJSlider xzoomer;
	AdjustableJSlider yzoomer;
	Vector selected = new Vector();

	@Override
	public void init() {
		map = new NeoMap(true, true);  // no internal vertical scroller
		map.setMapOffset(-200, 200);
		map.setMapRange(0, 1000);
		map.addAxis(0);
		//map.stretchToFit(true, true);
		xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
		yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
		map.setZoomer(NeoMap.X, xzoomer);
		map.setZoomer(NeoMap.Y, yzoomer);
//		map.addComponentListener(this);
		Container cpane = this.getContentPane();
		cpane.setLayout(new BorderLayout());
		cpane.add("Center", map);
		cpane.add("North", xzoomer);
		cpane.add("West", yzoomer);
		cpane.addComponentListener(this);


		double xcoords[] = {0, 100, 200, 300, 400, 500, 600, 700, 800, 900};
		double ycoords[] = {0, 50, -25, 25, 100, 50, 175, -10, 50, 74};

		map.configure("-glyphtype BasicGraphGlyph -foreground black -offset 0 "
				+ "-width 200 -packer null");
		BasicGraphGlyph sg = (BasicGraphGlyph) map.addItem(0, 900);
		sg.setPointCoords(xcoords, ycoords);
		sg.setBackgroundColor(Color.red);

		Image img1 = this.getImage(getCodeBase(), "./images/red-ball.gif");
		this.prepareImage(img1, this);

		Image img2 = this.getImage(getCodeBase(), "./images/alphahelix.gif");
		this.prepareImage(img2, this);

		map.configure("-glyphtype com.affymetrix.genoviz.glyph.LabelledRectGlyph "
				+ " ");

		// Put a an alpha helix image (or images, if tiled) from 600 to 700.
		LabelledRectGlyph rg;
		rg = (LabelledRectGlyph) map.addItem(0, 200 );
		rg.setColor(Color.RED);
		rg.setText("NC 1212");
		rg.setCoords(0, 0, 10, -100);

		LabelledRectGlyph rg1;
		rg1 = (LabelledRectGlyph) map.addItem(0, 200 );
		rg1.setColor(Color.RED);
		rg1.setText("NC 1212");
		rg1.setCoords(10, 0, 10, -111);
		

		/*map.configure("-glyphtype com.affymetrix.genoviz.glyph.LabelGlyph "
				+ " -offset 50 -width 10");

		LabelGlyph lGlyph = (LabelGlyph) map.addItem( 400, 700 );
		lGlyph.setText("Species XY-Unbekannt");
		lGlyph.setLabeledGlyph(rg);
		lGlyph.setToggleByWidth(true);*/

		// Put a red ball at 600.
		map.configure("-offset 0");
		/*ig = (BasicImageGlyph) map.addItem(600, 601);
		ig.setImage(img1, this);*/

		// Put a logo near the bottom center of the map.
		/*Image img3 = this.getImage(getCodeBase(), "./images/affymetrix_logo.gif");
		map.configure("-offset 140");
		ig = (BasicImageGlyph)map.addItem(0, 999);
		ig.setImage(img3, this);*/
	}

	@Override
	public URL getCodeBase() {
		if (isApplication) {
			return this.getClass().getResource("/");
		}
		return super.getCodeBase();
	}

	@Override
	public AppletContext getAppletContext() {
		if (isApplication) {
			return null;
		}
		return super.getAppletContext();
	}

	@Override
	public URL getDocumentBase() {
		if (isApplication) {
			return getCodeBase();
		}
		return super.getDocumentBase();
	}

	@Override
	public Image getImage(URL filebaseurl, String filename) {
		if (isApplication) {
			try {
				String filepath = filebaseurl.toString();
				filepath += filename.substring(2, filename.length());
				return Toolkit.getDefaultToolkit().getImage(new URL(filepath));
			} catch (MalformedURLException ex) {
				ex.printStackTrace();
			}
		}
		return super.getImage(filebaseurl, filename);
	}
	public void componentHidden(ComponentEvent e) {

    }

    public void componentMoved(ComponentEvent e) {

    }

    public void componentResized(ComponentEvent e) {
//	if (e.getSource() == canvas) {
					map.stretchToFit(false, false);
//					map.updateWidget();
//				}
    }

    public void componentShown(ComponentEvent e) {


    }

	static Boolean isApplication = false;

	static public void main(String[] args) {
		isApplication = true;
		GraphDemo me = new GraphDemo();
		me.init();
		JFrame frm = new JFrame("GenoViz Graph Demo");
		frm.getContentPane().add("Center", me);
		frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frm.pack();
		frm.setBounds(20, 40, 900, 400);
		frm.setVisible(true);
	}
}