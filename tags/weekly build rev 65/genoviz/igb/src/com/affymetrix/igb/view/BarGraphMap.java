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
package com.affymetrix.igb.view;

import com.affymetrix.genoviz.awt.AdjustableJSlider;
import java.applet.*;
import java.awt.event.MouseEvent;
import java.net.MalformedURLException;
import java.util.*;

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.glyph.BasicGraphGlyph;
import com.affymetrix.genoviz.glyph.BasicImageGlyph;
import com.affymetrix.genoviz.glyph.LabelledRectGlyph;
import com.affymetrix.genoviz.widget.NeoWidget;
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
import java.awt.event.MouseListener;
import java.awt.geom.Point2D;
import java.net.URL;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

public class BarGraphMap extends JPanel implements ComponentListener, MouseListener{

	NeoMap map;
	AdjustableJSlider xzoomer;
	AdjustableJSlider yzoomer;
	Vector<LabelledRectGlyph> selected = new Vector<LabelledRectGlyph>();
	private UnibrowHairline hairline = null;

	public void init() {
		map = new NeoMap(true, true);  // no internal vertical scroller
		map.setZoomBehavior(NeoMap.X,  NeoMap.CONSTRAIN_COORD, 0);
		map.setMapOffset(-200, 50);
		map.setMapRange(0, 1000);
		map.addAxis(0);
		//map.stretchToFit(true, true);
		xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
		yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
		map.setZoomer(NeoMap.X, xzoomer);
		map.setZoomer(NeoMap.Y, yzoomer);
		map.addComponentListener(this);
		Container cpane = this;
		cpane.setLayout(new BorderLayout());
		cpane.add("Center", map);
		cpane.add("North", xzoomer);
		cpane.add("West", yzoomer);
		cpane.addComponentListener(this);
		map.addMouseListener(this);



		hairline = new UnibrowHairline(map);


		//double xcoords[] = {0, 100, 200, 300, 400, 500, 600, 700, 800, 900};
		//double ycoords[] = {0, -50, -25, 25, 100, 50, 175, -10, 50, 74};

		/*map.configure("-glyphtype BasicGraphGlyph -foreground pink -offset 0 "
				+ "-width 200 -packer null");
		BasicGraphGlyph sg = (BasicGraphGlyph) map.addItem(0, 900);
		sg.setPointCoords(xcoords, ycoords);
		sg.setBackgroundColor(Color.red);*/

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
		rg1.setCoords(11, 0, 10, -111);
	}

	
	public void componentHidden(ComponentEvent e) {

    }

    public void componentMoved(ComponentEvent e) {

    }

    public void componentResized(ComponentEvent e) {
		map.stretchToFit(false, false);
    }

    public void componentShown(ComponentEvent e) {


    }

	public void mouseClicked(MouseEvent e) {
		//throw new UnsupportedOperationException("Not supported yet.");
	}

	public void mousePressed(MouseEvent e) {
		//throw new UnsupportedOperationException("Not supported yet.");
	}

	public void mouseReleased(MouseEvent evt) {
		if (!(evt instanceof NeoMouseEvent)) {
			return;
		}
		NeoMouseEvent nevt = (NeoMouseEvent) evt;

		Point2D.Double zoom_point = new Point2D.Double(nevt.getCoordX(), nevt.getCoordY());
		List<GlyphI> hits = nevt.getItems();

		// DESELECT THE OLD GLYPHS
		Iterator<LabelledRectGlyph> it = selected.iterator();
		while( it.hasNext() ){
			it.next().setBackgroundColor(Color.RED);
		}
		selected.clear();
		
		Iterator<GlyphI> it2 = hits.iterator();
		// SELECT THE NEW GLYPHS
		while( it2.hasNext() ){
			GlyphI g = it2.next();
			if( g instanceof LabelledRectGlyph){
				selected.add((LabelledRectGlyph)g);
				((LabelledRectGlyph)g).setBackgroundColor(Color.MAGENTA);
			}
		}

		if (hairline != null) {
			hairline.setSpot((int)zoom_point.getX());
		}

		map.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD, (int)zoom_point.getX());

		map.updateWidget();
	}

	public void mouseEntered(MouseEvent e) {
		//throw new UnsupportedOperationException("Not supported yet.");
	}

	public void mouseExited(MouseEvent e) {
		//throw new UnsupportedOperationException("Not supported yet.");
	}
}