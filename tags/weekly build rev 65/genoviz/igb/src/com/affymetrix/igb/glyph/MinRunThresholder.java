/**
*   Copyright (c) 2001-2006 Affymetrix, Inc.
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

package com.affymetrix.igb.glyph;

import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;


public final class MinRunThresholder extends JPanel
  implements ChangeListener, ActionListener, FocusListener  {

  private final List<GraphGlyph> graphs = new ArrayList<GraphGlyph>();
  private final NeoAbstractWidget widg;
  private JSlider tslider;
  private JTextField minrunTF;
  private static final int default_thresh_min = 0;
  private static final int default_thresh_max = 250;
  private static final int thresh_min = default_thresh_min;
  private int thresh_max = default_thresh_max;
  private int minrun_thresh = 0;

  private static final int max_chars = 9;
  private static final int max_pix_per_char = 6;
  private static final int tf_min_xpix = max_chars * max_pix_per_char;
  private static final int tf_max_xpix = tf_min_xpix + (2 * max_pix_per_char);
  private static final int tf_min_ypix = 20;
  private static final int tf_max_ypix = 25;

  static MinRunThresholder showFramedThresholder(GraphGlyph sgg, NeoAbstractWidget widg) {
    MinRunThresholder dthresher = new MinRunThresholder(sgg, widg);
    JFrame frm = new JFrame("Graph MinRun Threshold Control");
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", dthresher);
    //    frm.setSize(frm_width, frm_height);
    frm.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
	Window w = evt.getWindow();
	w.setVisible(false);
	w.dispose();
      }
    } );
    frm.pack();
    frm.setVisible(true);
    return dthresher;
  }

  public MinRunThresholder(GraphGlyph gl, NeoAbstractWidget w) {
    this(w);
    setGraph(gl);
  }

  public MinRunThresholder(NeoAbstractWidget w) {
    widg = w;

    tslider = new JSlider(JSlider.HORIZONTAL);
    tslider.setPreferredSize(new Dimension(400, 15));

    minrunTF = new JTextField(max_chars);
    minrunTF.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
    minrunTF.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    this.add(Box.createRigidArea(new Dimension(6,0)));
    this.add(new JLabel("Min Run > "));
    this.add(minrunTF);
    this.add(tslider);

    tslider.addChangeListener(this);
    minrunTF.addActionListener(this);
    
    minrunTF.addFocusListener(this);
  }

  public void setGraphs(List<GraphGlyph> newgraphs) {
    graphs.clear();
    tslider.removeChangeListener(this);
    minrunTF.removeActionListener(this);

    int first_min_run = 0;
    boolean all_have_same_min_run = false;
    boolean all_have_thresh_on = false;
    
    int gcount = newgraphs.size();
    if (gcount > 0) {
     int newthresh = 0;
			for (int i = 0; i < gcount; i++) {
				GraphGlyph gl = newgraphs.get(i);
				graphs.add(gl);
				int this_min_run = (int) gl.getMinRunThreshold();
	newthresh += this_min_run;
        if (i==0) {
          first_min_run = this_min_run;
          all_have_same_min_run = true;
          all_have_thresh_on = gl.getShowThreshold();
        } else {
          all_have_same_min_run = all_have_same_min_run && (this_min_run == first_min_run);
          all_have_thresh_on = all_have_thresh_on && gl.getShowThreshold();
        }
      }
      minrun_thresh = newthresh / gcount;
      tslider.setMinimum(thresh_min);
      tslider.setMaximum(thresh_max);
      tslider.setValue(minrun_thresh);
      if (all_have_same_min_run) {
        minrunTF.setText(Integer.toString(minrun_thresh));
      } else {
        minrunTF.setText("");
      }
    }

    tslider.addChangeListener(this);
    minrunTF.addActionListener(this);
    setEnabled(all_have_thresh_on);
  }


  public void setGraph(GraphGlyph gl) {
    List<GraphGlyph> newgraphs = new ArrayList<GraphGlyph>();
    newgraphs.add(gl);
    setGraphs(newgraphs);
  }
  
  public void setEnabled(boolean b) {
    super.setEnabled(b);
    tslider.setEnabled(b);
    minrunTF.setEnabled(b);
  }

  public void stateChanged(ChangeEvent evt) {
    if (graphs.size() <= 0) { return; }
    Object src = evt.getSource();
    if (src == tslider) {
      int current_thresh = tslider.getValue();
      if (current_thresh != minrun_thresh) {
	minrun_thresh = current_thresh;
	for (GraphGlyph sgg : graphs) {
	  sgg.setMinRunThreshold(minrun_thresh);
	}
	minrunTF.removeActionListener(this);
	minrunTF.setText(Integer.toString(minrun_thresh));
	minrunTF.addActionListener(this);
	widg.updateWidget();
      }
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    doAction(src);
  }
  
  public void doAction(Object src) {
    if (graphs.size() <= 0) { return; }
    if (src == minrunTF) try {
      int new_thresh = Integer.parseInt(minrunTF.getText());
      if (new_thresh != minrun_thresh) {
	boolean new_thresh_max = (new_thresh > thresh_max);
	//	if ((new_thresh < thresh_min) || (new_thresh > thresh_max)) {
	if (new_thresh < thresh_min) {
	  // new threshold outside of min/max possible, so keep current threshold instead
	  minrunTF.setText(Integer.toString(minrun_thresh));
	}
	else {
	  minrun_thresh = new_thresh;
		for (GraphGlyph sgg : graphs) {
	    sgg.setMinRunThreshold(minrun_thresh);
	  }
	  tslider.removeChangeListener(this);
	  if (new_thresh_max) {
	    thresh_max = minrun_thresh;
	    tslider.setMaximum(thresh_max);
	  }
	  else if (minrun_thresh <= default_thresh_max) {
	    thresh_max = default_thresh_max;
	    tslider.setMaximum(thresh_max);
	  }
          tslider.setValue(minrun_thresh);
	  tslider.addChangeListener(this);
	  widg.updateWidget();
	}
      }
    } catch (NumberFormatException nfe) {
      setGraphs(new ArrayList<GraphGlyph>(graphs));
    }
  }

  /*public void deleteGraph(GraphGlyph gl) {
    graphs.remove(gl);
    setGraphs(new ArrayList<SmartGraphGlyph>(graphs));
  }*/

  public void focusGained(FocusEvent e) {
  }

  public void focusLost(FocusEvent e) {
    Object src = e.getSource();
    if (src instanceof JTextField) {
      doAction(src);
    }
  }


}

