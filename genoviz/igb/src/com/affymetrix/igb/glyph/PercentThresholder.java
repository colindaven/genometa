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
import java.awt.Window;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;


public final class PercentThresholder extends JPanel
  implements ChangeListener, ActionListener  {

  private final NeoAbstractWidget widg;
  private final JSlider min_percent_slider;
  private final JSlider max_percent_slider;
  private final JTextField min_perT;
  private final JTextField max_perT;
  private final JCheckBox syncCB;
  private boolean sync_min_max;

  // info2pscores is a hash of GraphGlyphs' data model
  //   (usually a GraphSym if using genometry) to float[] arrays, each of length
  //   (sliders_per_percent * total_percent), and each value v at index i is
  //   value at which (i * sliders_per_percent) percent of the y values in the graph
  //   are below v
  // assuming abs_min_percent = 0, abs_max_percent = 100, so total_percent = 100
  // Using glyph's data model instead of glyph itself because GraphGlyph may get
  //    recreated from data model, but still want new GraphGlyph to hash to same
  //    cached percent-to-score array
  //TODO:
  // WARNING!  this caching currently causes a persistent reference to
  //    a data model (usually a GraphSym) for _every_ graph that is ever
  //    selected.  For times when many graphs are looked at and discarded, this
  //    will quickly eat up memory that could otherwise be freed.  NEED TO
  //    FIX THIS!  But also need to balance between memory concerns and the
  //    desire to avoid recalculation of percent-to-score array (which requires a
  //    sort) every time a graph is selected...
  private final Map<Object,float[]> info2pscores = new HashMap<Object,float[]>();
  private final List<GraphGlyph> graphs = new ArrayList<GraphGlyph>();

  /**
   *  Now trying to map slider values to percentages, such that each slider
   *  unit = 0.1 percent (or in other words slider units per percent = 10)
   */
  private static final float sliders_per_percent = 10.0f;
  private static final float percents_per_slider = 1.0f / sliders_per_percent;
  private static final float abs_min_percent = 0.0f;
  private static final float abs_max_percent = 100.0f;
  private float prev_min;
  private float prev_max;
  private static final float slider_label_offset = 50.0f;

  static PercentThresholder showFramedThresholder(GraphGlyph sgg, NeoAbstractWidget widg) {
    //    PercentThresholder thresher = new PercentThresholder(sgg, widg);
    PercentThresholder thresher = new PercentThresholder(widg);
    List<GraphGlyph> glist = new ArrayList<GraphGlyph>();
    glist.add(sgg);
    thresher.setGraphs(glist);
    JFrame frm = new JFrame("Graph Percentile Adjuster");
    Container cpane = frm.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", thresher);
    frm.addWindowListener( new WindowAdapter() {
			@Override
      public void windowClosing(WindowEvent evt) {
	Window w = evt.getWindow();
	w.setVisible(false);
	w.dispose();
      }
    } );
    //    frm.setSize(frm_width, frm_height);
    frm.pack();
		frm.setVisible(true);
    return thresher;
  }

  public PercentThresholder(NeoAbstractWidget w) {
    super();
    widg = w;
    float current_max_percent = 100;
    float current_min_percent = 0;
    prev_max = current_max_percent;
    prev_min = current_min_percent;

    min_perT = new JTextField(6);
    max_perT = new JTextField(6);
    min_perT.setText(Float.toString(current_min_percent));
    max_perT.setText(Float.toString(current_max_percent));

    min_percent_slider =
      new JSlider(JSlider.HORIZONTAL,
		  (int)(abs_min_percent * sliders_per_percent),
		  (int)(abs_max_percent * sliders_per_percent),
		  (int)(current_min_percent * sliders_per_percent));
    max_percent_slider =
      new JSlider(JSlider.HORIZONTAL,
		  (int)(abs_min_percent * sliders_per_percent),
		  (int)(abs_max_percent * sliders_per_percent),
		  (int)(current_max_percent * sliders_per_percent));
    min_percent_slider.addChangeListener(this);
    min_percent_slider.setPreferredSize(new Dimension(600, 15));

    max_percent_slider.setMinorTickSpacing(10);
    max_percent_slider.setMajorTickSpacing((int)slider_label_offset);
    max_percent_slider.addChangeListener(this);
    max_percent_slider.setPreferredSize(new Dimension(600, 15));

    Hashtable<Integer, JLabel> decimal_labels = new Hashtable<Integer, JLabel>();

    for (float f=0.0f; f<=1000.0f; f+=slider_label_offset) {
      Integer slideval = new Integer((int)f);
      int labelval = (int)(f/sliders_per_percent);
      decimal_labels.put(slideval, new JLabel(Integer.toString(labelval)));
    }
    max_percent_slider.setLabelTable(decimal_labels);

    this.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    JPanel labP = new JPanel();
    labP.setLayout(new BoxLayout(labP, BoxLayout.Y_AXIS));
    JPanel textP = new JPanel();
    textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS));
    JPanel slideP = new JPanel();
    slideP.setLayout(new BoxLayout(slideP, BoxLayout.Y_AXIS));

    labP.add(new JLabel("Min:"));
    labP.add(new JLabel("Max:"));
    textP.add(min_perT);
    textP.add(max_perT);
    slideP.add(min_percent_slider);
    slideP.add(max_percent_slider);

    this.add(labP);
    this.add(textP);
    this.add(slideP);

    syncCB = new JCheckBox("Sync Min/Max");

    min_perT.addActionListener(this);
    max_perT.addActionListener(this);
    syncCB.addActionListener(this);
  }

  private void setGraphs(List<GraphGlyph> newgraphs) {
    graphs.clear();
		for (GraphGlyph gl : newgraphs) {
      Object info = gl.getInfo();
      if (info == null) { System.err.println("Graph has no info! " + gl); }
      float[] p2score = info2pscores.get(info);
      if (p2score == null) {
	p2score = calcPercents2Scores(gl);
	info2pscores.put(info, p2score);
      }
      graphs.add(gl);
    }
  }

  public float[] calcPercents2Scores(GraphGlyph sgg) {
    int num_percents = (int)(abs_max_percent * sliders_per_percent + 1);
    System.out.println("num_percents: " + num_percents);
    float[] ordered_scores = sgg.copyYCoords();
    System.out.println("score array copied");
    Arrays.sort(ordered_scores);
    System.out.println("scores sorted");
    float[] percent2score = new float[num_percents];

    float scores_per_percent = ordered_scores.length / 100.0f;
    for (float percent = 0.0f; percent <= abs_max_percent; percent += percents_per_slider) {
      int score_index = (int)(percent * scores_per_percent);
      if (score_index >= ordered_scores.length) { score_index = ordered_scores.length -1; }
      percent2score[Math.round(percent * sliders_per_percent)] = ordered_scores[score_index];
    }
    // just making sure max 100% is really 100%...
    percent2score[percent2score.length - 1] = ordered_scores[ordered_scores.length - 1];
    return percent2score;
  }

  public void stateChanged(ChangeEvent evt) {
    Object src = evt.getSource();
    float max_val = (max_percent_slider.getValue()/sliders_per_percent);
    float min_val = (min_percent_slider.getValue()/sliders_per_percent);;

    if (src == max_percent_slider) {
      if (max_val <= prev_min) {
	prev_max = prev_min+1;
	max_percent_slider.setValue((int)(prev_max * sliders_per_percent));
	max_percent_slider.updateUI();
      }
      else if (max_val != prev_max) {
	float max_percent = max_val;
	setVisibleMaxPercent(max_percent);
	if (sync_min_max) {
	  float min_percent = 100 - max_percent;
	  setVisibleMinPercent(min_percent);
	  prev_min = min_percent;
	  min_percent_slider.setValue((int)(min_percent * sliders_per_percent));
	}
	prev_max = max_val;
	widg.updateWidget();
      }
      max_perT.setText(Float.toString(prev_max));
    }
    else if (src == min_percent_slider) {
      if (min_val >= prev_max) {
	prev_min = prev_max-1;
	min_percent_slider.setValue((int)(prev_min * sliders_per_percent));
	min_percent_slider.updateUI();
      }
      else if (min_val != prev_min) {
	float min_percent = min_val;
	setVisibleMinPercent(min_percent);
	if (sync_min_max) {
	  float max_percent = 100 - min_percent;
	  setVisibleMaxPercent(max_percent);
	  prev_max = max_percent;
	  max_percent_slider.setValue((int)(max_percent * sliders_per_percent));
	}

	prev_min = min_val;
	widg.updateWidget();
      }
      min_perT.setText(Float.toString(prev_min));
    }
  }

  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();

    if (src == min_perT || src == max_perT || src == syncCB) {
      try {
	float minval = Float.parseFloat(min_perT.getText());
	float maxval = Float.parseFloat(max_perT.getText());
	if (minval != prev_min && src == min_perT) {
	  System.out.println("received evt on min_perT: " + evt);
	  System.out.println("new min val: " + minval);
	  if (minval > prev_max) {
	    minval = prev_max-1;
	    min_perT.setText(Float.toString(minval));
	  }
	  prev_min = minval;
	  min_percent_slider.setValue((int)(minval * sliders_per_percent));
	}
	else if (maxval != prev_max && src == max_perT) {
	  System.out.println("received evt on max_perT: " + evt);
	  System.out.println("new min val: " + maxval);
	  if (maxval < prev_min) {
	    maxval = prev_min+1;
	    max_perT.setText(Float.toString(maxval));
	  }
	  prev_max = maxval;
	  max_percent_slider.setValue((int)(maxval * sliders_per_percent));
	}
	else if (src == syncCB) {
	  System.out.println("received evt on lockbox: " + evt);
	  sync_min_max = syncCB.isSelected();
	  System.out.println("lockbox selected: " + sync_min_max);
	}
	setVisibleMaxPercent(maxval);
	setVisibleMinPercent(minval);
      }
      catch (Exception ex) {
	ex.printStackTrace();
      }
      widg.updateWidget();
    }
  }

  /**
   *   Set visible min Y to the specified value for all graphs under control
   *   of PercentThresholder (but doesn't force an updateWidget()).
   *   Argument is _value_, not percentage.
   */
  public void setVisibleMinPercent(float percent) {
		for (GraphGlyph gl : graphs) {
      Object info = gl.getInfo();
      float[] percent2score = info2pscores.get(info);
      float min_score = percent2score[Math.round(percent * sliders_per_percent)];
      gl.setVisibleMinY(min_score);
    }
  }


  /**
   *   set visible max Y to the specified value for all graphs under control
   *   of PercentThresholder (but doesn't force an updateWidget()).
   *   Argument is _value_, not percentage.
   */
  public void setVisibleMaxPercent(float percent) {
		for (GraphGlyph gl : graphs) {
      Object info = gl.getInfo();
      float[] percent2score = info2pscores.get(info);
      float max_score = percent2score[Math.round(percent * sliders_per_percent)];
      gl.setVisibleMaxY(max_score);
    }
  }

}
