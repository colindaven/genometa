/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.text.NumberFormat;
import java.text.DecimalFormat;
import java.util.*;

public final class MinScoreThresholder extends JPanel
  implements ChangeListener, ActionListener  {

  private static final NumberFormat nformat = new DecimalFormat();
  private final List<GraphGlyph> graphs = new ArrayList<GraphGlyph>();
  private final NeoAbstractWidget widg;
  private final JSlider tslider;
  private final JTextField minscoreTF;
  private float thresh_min = -500;
  private float thresh_max = 500;
  private float minscore_thresh = 0;
  private float sliders_per_score = 0;

  static MinScoreThresholder showFramedThresholder(GraphGlyph sgg, NeoAbstractWidget widg) {
    MinScoreThresholder dthresher = new MinScoreThresholder(sgg, widg);
    JFrame frm = new JFrame("Graph Score Threshold Control");
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", dthresher);
    frm.addWindowListener( new WindowAdapter() {
			@Override
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

  public MinScoreThresholder(GraphGlyph gl, NeoAbstractWidget w) {
    this(w);
    setGraph(gl);
  }

  public MinScoreThresholder(NeoAbstractWidget w) {
    widg = w;

    tslider = new JSlider(JSlider.HORIZONTAL);
    tslider.setPreferredSize(new Dimension(400, 15));

    minscoreTF = new JTextField(5);
    JPanel valpan = new JPanel();
    valpan.setLayout(new GridLayout(1, 2));
    valpan.add(new JLabel("Score"));
    valpan.add(minscoreTF);

    this.setLayout(new BorderLayout());
    this.add("Center", tslider);
    this.add("West", valpan);

    tslider.addChangeListener(this);
    minscoreTF.addActionListener(this);
  }

  public void setGraphs(List<GraphGlyph> newgraphs) {
    graphs.clear();

    tslider.removeChangeListener(this);
    minscoreTF.removeActionListener(this);
    float thresh_min = Float.POSITIVE_INFINITY;
    float thresh_max = Float.NEGATIVE_INFINITY;
    int gcount = newgraphs.size();
    //    System.out.println("in MinScoreThresholder.setGraphs(), count = " + gcount);
    //    float minscore_thresh = 0;
    float newthresh = 0;
		for (GraphGlyph gl : newgraphs) {
      graphs.add(gl);
      thresh_min = Math.min(thresh_min, gl.getGraphMinY());
      thresh_max = Math.max(thresh_max, gl.getGraphMaxY());
      newthresh += gl.getMinScoreThreshold();
    }

    // set default thresh to average thresh of selected graphs
    minscore_thresh = newthresh / (float)gcount;
    //    System.out.println("thresh min: " + thresh_min);
    //    System.out.println("thresh max: " + thresh_max);
    //    System.out.println("thresh cur avg: " + minscore_thresh);

    float score_range = thresh_max - thresh_min;
    sliders_per_score = 1000.0f/score_range;

    tslider.setMinimum(0);
    tslider.setMaximum(1000);

    tslider.setValue( calcSliderVal(minscore_thresh));
    minscoreTF.setText(nformat.format(minscore_thresh));

    tslider.addChangeListener(this);
    minscoreTF.addActionListener(this);
  }



  public void setGraph(GraphGlyph gl) {
    graphs.clear();
    graphs.add(gl);
    //    this.sgg = gl;

    tslider.removeChangeListener(this);
    minscoreTF.removeActionListener(this);

    thresh_min = gl.getGraphMinY();
    thresh_max = gl.getGraphMaxY();
    minscore_thresh = gl.getMinScoreThreshold();

    float score_range = thresh_max - thresh_min;
    sliders_per_score = 1000.0f/score_range;

    tslider.setMinimum(0);
    tslider.setMaximum(1000);

    tslider.setValue( calcSliderVal(minscore_thresh));
    minscoreTF.setText(nformat.format(minscore_thresh));

    tslider.addChangeListener(this);
    minscoreTF.addActionListener(this);
  }

  public float calcScore(int slider_val) {
    return (((float)slider_val/sliders_per_score) + thresh_min);
  }

  public int calcSliderVal(float thresh_score) {
    return Math.round((thresh_score - thresh_min) * sliders_per_score);
  }


  public void stateChanged(ChangeEvent evt) {
    Object src = evt.getSource();
    if (src == tslider) {
      //      float current_thresh = tslider.getValue() / sliders_per_score;
      float current_thresh = calcScore(tslider.getValue());
      if (current_thresh != minscore_thresh) {
	minscore_thresh = current_thresh;
	for (GraphGlyph sgg : graphs) {
	  sgg.setMinScoreThreshold(minscore_thresh);
	}
	minscoreTF.removeActionListener(this);
	minscoreTF.setText(nformat.format(minscore_thresh));
	minscoreTF.addActionListener(this);
	widg.updateWidget();
      }
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == minscoreTF) {
      float new_thresh = Float.parseFloat(minscoreTF.getText());
      if (new_thresh != minscore_thresh) {
	if ((new_thresh < thresh_min) || (new_thresh > thresh_max)) {
	  minscoreTF.setText(nformat.format(minscore_thresh));
	}
	else {
	  minscore_thresh = new_thresh;
		for (GraphGlyph sgg : graphs) {
	    sgg.setMinScoreThreshold(minscore_thresh);
	  }
	  tslider.removeChangeListener(this);
	  tslider.setValue(calcSliderVal(minscore_thresh));
	  tslider.addChangeListener(this);
	  widg.updateWidget();
	}
      }
    }
  }


}
