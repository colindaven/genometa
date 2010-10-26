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

import com.affymetrix.genoviz.awt.AdjustableJSlider;

import java.awt.*;
import java.awt.event.*;
import javax.swing.JScrollBar;

public class SimpleMap3 extends SimpleMap1 {

	AdjustableJSlider zoomer;
      JScrollBar scroller;

	public SimpleMap3() {
		zoomer = new AdjustableJSlider(JScrollBar.VERTICAL);
            zoomer.setBackground(Color.green);
            zoomer.setForeground(Color.red);
		add("West", zoomer);
		map.setRangeZoomer(zoomer);
		scroller = new JScrollBar(JScrollBar.HORIZONTAL);
		add("South", scroller);
		map.setRangeScroller(scroller);
	}

	public static void main (String argv[]) {
		SimpleMap3 me = new SimpleMap3();
		Frame f = new Frame("GenoViz");
		f.add(me, BorderLayout.CENTER);
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
		f.setBounds(20, 40, 900, 400);
		f.setVisible(true);//f.show();
	}

}
