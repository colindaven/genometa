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
import java.awt.*;
import java.awt.event.*;

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.glyph.AxisGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.Shadow;
import com.affymetrix.genoviz.widget.VisibleRange;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 *  Demonstrates using the vertical map option
 *  WARNING: currently many glyphs do not display correctly on vertical maps!
 *
 * @version $Id: VerticalMapDemo.java 6074 2010-06-04 18:49:01Z vbishnoi $
 */
public class VerticalMapDemo extends JApplet {
	JPanel panel1, panel2;

	static public void main(String[] args)
	{
		VerticalMapDemo demo = new VerticalMapDemo();
		demo.init();
		demo.start();
		JFrame window = new JFrame("Genoviz VerticalMap Demo");
		window.setContentPane(demo);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.pack();
		window.setVisible(true);
	}

	@Override
	public void init() {
		panel1 = testMap(NeoConstants.HORIZONTAL);
		panel2 = testMap(NeoConstants.VERTICAL);
		Container cpane = this.getContentPane();
		cpane.setLayout(new GridLayout(1,2,10,0));
		cpane.add(panel1);
		cpane.add(panel2);

	}

	public JPanel testMap(int orient) {
		final NeoMap map = new NeoMap(true,true,orient,new LinearTransform());
		final VisibleRange selectedRange = new VisibleRange();

		map.setSelectionEvent(NeoMap.ON_MOUSE_DOWN);
		map.setMapRange(0, 10000);
		map.setMapOffset(0, 100);
		AxisGlyph ax = map.addAxis(50);

		map.configure("-glyphtype FillRectGlyph -color green" +
				" -offset 40 -width 5");
		map.addItem(5000, 9000);
		map.addItem(6000, 7000);

		map.configure("-offset -40 -color blue");
		map.addItem(1000, 3000);
		map.addItem(2000, 4000);

		map.configure("-glyphtype PointedGlyph -color magenta" +
				" -offset -40 -width 5");
		map.addItem(4000, 1000);
		map.addItem(6000, 9000);

		AdjustableJSlider xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
		map.setZoomer(NeoMap.X, xzoomer);
		AdjustableJSlider yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
		map.setZoomer(NeoMap.Y, yzoomer);
		
		JPanel map_pan = new JPanel();
		//NeoPanel map_pan = new NeoPanel();
		map_pan.setLayout(new BorderLayout());
		map_pan.add("Center", map);
		map_pan.add("West", yzoomer);
		map_pan.add("North", xzoomer);

		Shadow hairline = new Shadow( map, orient );
		selectedRange.addListener( hairline );
		hairline.label.setFont(new Font("Courier", Font.PLAIN, 20));

		if (orient==NeoConstants.HORIZONTAL) {
			map.addMouseListener( new MouseAdapter() {
				@Override
				public void mouseReleased( MouseEvent e ) {
					selectedRange.setSpot( ((NeoMouseEvent)e).getCoordX() );
				}
			});
		}
		else {
			map.addMouseListener( new MouseAdapter() {
				@Override
				public void mouseReleased( MouseEvent e ) {
					selectedRange.setSpot( ((NeoMouseEvent)e).getCoordY() );
				}
			});
		}


		final int axisID = (orient==NeoConstants.HORIZONTAL) ? NeoMap.X : NeoMap.Y;
		NeoRangeListener zoomMidPointSetter = new NeoRangeListener() {
			public void rangeChanged( NeoRangeEvent e ) {
				double midPoint = ( e.getVisibleEnd() + e.getVisibleStart() ) / 2.0;
				map.setZoomBehavior(axisID, NeoMap.CONSTRAIN_COORD, midPoint );
				map.updateWidget();
			}
		};
		selectedRange.addListener( zoomMidPointSetter );
		
		return map_pan;
	}

}
