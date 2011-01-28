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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.text.DecimalFormat;

import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.view.GraphAdjusterView;
import java.text.ParseException;

public final class GraphVisibleBoundsSetter extends JPanel
				implements ChangeListener, ActionListener, FocusListener {

	private static DecimalFormat val_format;
	private static DecimalFormat per_format;
	private NeoAbstractWidget widg;
	private JSlider min_percent_slider;
	private JSlider max_percent_slider;
	private JSlider min_val_slider;
	private JSlider max_val_slider;
	private JTextField min_perT;
	private JTextField max_perT;
	private JTextField min_valT;
	private JTextField max_valT;
	private final JRadioButton by_valRB = new JRadioButton("By Value (together)");
	private final JRadioButton by_percentileRB = new JRadioButton("By Percentile (individually)");
	private final JPanel valP = new JPanel();  // for adjust-by-value controls
	private final JPanel perP = new JPanel();  // for adjust-by-percent controls
	private final static int max_chars = 8;
	private final static int max_pix_per_char = 6;
	private static final int tf_min_xpix = max_chars * max_pix_per_char;
	private static final int tf_max_xpix = tf_min_xpix + (2 * max_pix_per_char);
	private final static int tf_min_ypix = 10;
	private final static int tf_max_ypix = 25;


	static {
		val_format = new DecimalFormat();
		val_format.setMinimumIntegerDigits(1);
		val_format.setMinimumFractionDigits(0);
		val_format.setMaximumFractionDigits(1);
		val_format.setGroupingUsed(false);
		per_format = new DecimalFormat();
		per_format.setMinimumIntegerDigits(1);
		per_format.setMinimumFractionDigits(0);
		per_format.setMaximumFractionDigits(1);
		per_format.setPositiveSuffix("%");
		per_format.setGroupingUsed(false);
	}

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

	/*
	 *  Now trying to map slider values to percentages, such that each slider
	 *  unit = 0.1 percent (or in other words slider units per percent = 10)
	 */
	private static final float sliders_per_percent = 10.0f;
	private static final float abs_min_percent = 0.0f;
	private static final float abs_max_percent = 100.0f;
	private float prev_min_per = 0;
	private float prev_max_per = 100;
	private final static int total_val_sliders = 1000;
	private float sliders_per_val; // slider units per yval unit
	//private float vals_per_slider; // yval units per slider unit
	//private float abs_min_val;
	//private float abs_max_val;
	private float prev_min_val;
	private float prev_max_val;
	private static final float per_offset = 0.1f;
	private static final float val_offset = 0.1f;
	//private Dimension slider_sizepref = new Dimension(600, 15);
	//private Dimension textbox_sizepref = new Dimension(400, 15);
	//private boolean set_slider_sizepref = false;
	//private boolean set_textbox_sizepref = false;
	private static final boolean show_min_and_max = false;
	private boolean includePercentileControls = true;

	static GraphVisibleBoundsSetter showFramedThresholder(GraphGlyph sgg, NeoAbstractWidget widg) {
		//    GraphVisibleBoundsSetter thresher = new GraphVisibleBoundsSetter(sgg, widg);
		GraphVisibleBoundsSetter thresher = new GraphVisibleBoundsSetter(widg);
		List<GraphGlyph> glist = new ArrayList<GraphGlyph>();
		glist.add(sgg);
		thresher.setGraphs(glist);
		JFrame frm = new JFrame("Graph Percentile Adjuster");
		Container cpane = frm.getContentPane();
		cpane.setLayout(new BorderLayout());
		cpane.add("Center", thresher);
		frm.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent evt) {
				Window w = evt.getWindow();
				w.setVisible(false);
				w.dispose();
			}
		});
		//    frm.setSize(frm_width, frm_height);
		frm.pack();
		frm.setVisible(true);
		return thresher;
	}

	public GraphVisibleBoundsSetter(NeoAbstractWidget w) {
		this(w, true);
	}

	private GraphVisibleBoundsSetter(NeoAbstractWidget w, boolean includePercentileControls) {
		super();
		this.includePercentileControls = includePercentileControls;

		widg = w;

		min_valT = new JTextField(max_chars);
		max_valT = new JTextField(max_chars);
		min_perT = new JTextField(max_chars);
		max_perT = new JTextField(max_chars);

		min_perT.setText(per_format.format(prev_min_per));
		max_perT.setText(per_format.format(prev_max_per));
		min_percent_slider =
						new JSlider(JSlider.HORIZONTAL,
						(int) (abs_min_percent * sliders_per_percent),
						(int) (abs_max_percent * sliders_per_percent),
						(int) (prev_min_per * sliders_per_percent));
		max_percent_slider =
						new JSlider(JSlider.HORIZONTAL,
						(int) (abs_min_percent * sliders_per_percent),
						(int) (abs_max_percent * sliders_per_percent),
						(int) (prev_max_per * sliders_per_percent));
		min_val_slider = new JSlider(JSlider.HORIZONTAL);
		max_val_slider = new JSlider(JSlider.HORIZONTAL);

		min_valT.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
		max_valT.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
		min_perT.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
		max_perT.setMinimumSize(new Dimension(tf_min_xpix, tf_min_ypix));
		min_valT.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));
		max_valT.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));
		min_perT.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));
		max_perT.setMaximumSize(new Dimension(tf_max_xpix, tf_max_ypix));
		valP.setLayout(new BoxLayout(valP, BoxLayout.X_AXIS));
		perP.setLayout(new BoxLayout(perP, BoxLayout.X_AXIS));
		//valP.setBorder(new TitledBorder("By Value"));
		//perP.setBorder(new TitledBorder("By Percentile"));

		JPanel labP2 = new JPanel();
		JPanel textP2 = new JPanel();
		JPanel slideP2 = new JPanel();
		labP2.setLayout(new BoxLayout(labP2, BoxLayout.Y_AXIS));
		textP2.setLayout(new BoxLayout(textP2, BoxLayout.Y_AXIS));
		slideP2.setLayout(new BoxLayout(slideP2, BoxLayout.Y_AXIS));
		labP2.add(new JLabel("Min: "));
		labP2.add(new JLabel("Max: "));
		textP2.add(min_valT);
		textP2.add(max_valT);
		slideP2.add(min_val_slider);
		slideP2.add(max_val_slider);
		valP.add(labP2);
		valP.add(textP2);
		valP.add(slideP2);

		JPanel labP = new JPanel();
		JPanel textP = new JPanel();
		JPanel slideP = new JPanel();
		labP.setLayout(new BoxLayout(labP, BoxLayout.Y_AXIS));
		textP.setLayout(new BoxLayout(textP, BoxLayout.Y_AXIS));
		slideP.setLayout(new BoxLayout(slideP, BoxLayout.Y_AXIS));
		labP.add(new JLabel("Min: "));
		labP.add(new JLabel("Max: "));
		textP.add(min_perT);
		textP.add(max_perT);
		slideP.add(min_percent_slider);
		slideP.add(max_percent_slider);
		perP.add(labP);
		perP.add(textP);
		perP.add(slideP);

		Box by_val_box = Box.createHorizontalBox();
		ButtonGroup by_val_group = new ButtonGroup();
		by_val_group.add(by_valRB);
		by_val_group.add(by_percentileRB);
		by_valRB.setSelected(true);
		by_percentileRB.setSelected(false);
		by_valRB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				switchView(false);
			}
		});
		by_percentileRB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				switchView(true);
			}
		});
		by_val_box.add(by_valRB);
		by_val_box.add(by_percentileRB);
		by_val_box.add(Box.createHorizontalGlue());

		this.setBorder(new TitledBorder("Y-Axis Scale"));
		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		if (includePercentileControls) {
			this.add(by_val_box);
			this.add(Box.createRigidArea(new Dimension(5, 5)));
			this.add(valP);
			this.add(perP);
		} else {
			this.add(valP);
		}
		valP.setVisible(true);
		perP.setVisible(false);

		turnOnListening();
	}

	/**
	 * Show either the value-based sliders or the percent-based sliders.
	 * @param b  if true, show the percent-based sliders.
	 */
	public void switchView(boolean b) {
		valP.setVisible(!b);
		perP.setVisible(b);
	}

	/**
	 *  Set the set of graphs to the given List of GraphGlyph objects.
	 */
	public void setGraphs(List newgraphs) {
		turnOffListening();
		graphs.clear();
		int gcount = newgraphs.size();
		for (int i = 0; i < gcount; i++) {
			GraphGlyph gl = (GraphGlyph) newgraphs.get(i);
			graphs.add(gl);
		}

		if (includePercentileControls) {
			initPercents();
		}
		initValues();
		setEnabled(!graphs.isEmpty());
		turnOnListening();
	}

	@Override
	public void setEnabled(boolean b) {
		super.setEnabled(b);
		min_valT.setEnabled(b);
		max_valT.setEnabled(b);
		min_perT.setEnabled(b);
		max_perT.setEnabled(b);
		min_percent_slider.setEnabled(b);
		max_percent_slider.setEnabled(b);
		min_val_slider.setEnabled(b);
		max_val_slider.setEnabled(b);
	}

	

	private void initValues() {
		float min_of_mins = Float.POSITIVE_INFINITY;
		float max_of_maxes = Float.NEGATIVE_INFINITY;
		float avg_of_mins = 0;
		float avg_of_maxes = 0;

		float min_of_vismins = Float.POSITIVE_INFINITY;
		float max_of_vismins = Float.NEGATIVE_INFINITY;
		float min_of_vismaxes = Float.POSITIVE_INFINITY;
		float max_of_vismaxes = Float.NEGATIVE_INFINITY;
		float avg_of_vismins = 0;
		float avg_of_vismaxes = 0;
		int gcount = graphs.size();

		if (gcount == 0) {
			min_of_mins = 0;
			max_of_maxes = 0;
			avg_of_mins = avg_of_maxes = 0;
			min_of_vismins = max_of_vismins = 0;
			min_of_vismaxes = max_of_vismaxes = 0;
			avg_of_vismins = avg_of_vismaxes = 0;

		} else {

			for (int i = 0; i < gcount; i++) {
				GraphGlyph gl = graphs.get(i);
				float min = gl.getGraphMinY();
				float max = gl.getGraphMaxY();
				float vismin = gl.getVisibleMinY();
				float vismax = gl.getVisibleMaxY();

				min_of_mins = Math.min(min_of_mins, min);
				max_of_maxes = Math.max(max_of_maxes, max);

				min_of_vismins = Math.min(min_of_vismins, vismin);
				max_of_vismins = Math.max(max_of_vismins, vismin);
				max_of_vismaxes = Math.max(max_of_vismaxes, vismax);
				min_of_vismaxes = Math.min(min_of_vismaxes, vismax);

				avg_of_mins += min;
				avg_of_maxes += max;
				avg_of_vismins += vismin;
				avg_of_vismaxes += vismax;
			}

			avg_of_mins = avg_of_mins / gcount;
			avg_of_maxes = avg_of_maxes / gcount;
			avg_of_vismins = avg_of_vismins / gcount;
			avg_of_vismaxes = avg_of_vismaxes / gcount;
		}

		sliders_per_val = (total_val_sliders) / (max_of_maxes - min_of_mins);

		if (min_of_vismins == max_of_vismins) {
			min_valT.setText(val_format.format(min_of_vismins));
		} else {
			if (show_min_and_max) {
				min_valT.setText(val_format.format(min_of_vismins) +
								" : " + val_format.format(max_of_vismins));
			} else {
				min_valT.setText("");
			}
		}
		if (min_of_vismaxes == max_of_vismaxes) {
			max_valT.setText(val_format.format(max_of_vismaxes));
		} else {
			if (show_min_and_max) {
				max_valT.setText(val_format.format(min_of_vismaxes) +
								" : " + val_format.format(max_of_vismaxes));
			} else {
				max_valT.setText("");
			}
		}

		min_val_slider.setMinimum((int) (min_of_mins * sliders_per_val));
		min_val_slider.setMaximum((int) (max_of_maxes * sliders_per_val));
		min_val_slider.setValue((int) (avg_of_vismins * sliders_per_val));

		max_val_slider.setMinimum((int) (min_of_mins * sliders_per_val));
		max_val_slider.setMaximum((int) (max_of_maxes * sliders_per_val));
		max_val_slider.setValue((int) (avg_of_vismaxes * sliders_per_val));

		prev_min_val = avg_of_vismins;
		prev_max_val = avg_of_vismaxes;
	}

	// assumes listening has already been turned off
	private void initPercents() {
		float min_of_vismins = Float.POSITIVE_INFINITY;
		float max_of_vismins = Float.NEGATIVE_INFINITY;
		float min_of_vismaxes = Float.POSITIVE_INFINITY;
		float max_of_vismaxes = Float.NEGATIVE_INFINITY;
		float avg_of_vismins = 0;
		float avg_of_vismaxes = 0;

		int gcount = graphs.size();
		if (gcount == 0) {
			min_of_vismins = max_of_vismins = 0;
			min_of_vismaxes = max_of_vismaxes = 0;
			avg_of_vismins = avg_of_vismaxes = 0;
		} else {
			for (int i = 0; i < gcount; i++) {
				GraphGlyph gl = graphs.get(i);
				float vismin_val = gl.getVisibleMinY();
				float vismax_val = gl.getVisibleMaxY();
				float vismin_per = getPercentForValue(gl, vismin_val);
				float vismax_per = getPercentForValue(gl, vismax_val);

				min_of_vismins = Math.min(min_of_vismins, vismin_per);
				max_of_vismins = Math.max(max_of_vismins, vismin_per);
				max_of_vismaxes = Math.max(max_of_vismaxes, vismax_per);
				min_of_vismaxes = Math.min(min_of_vismaxes, vismax_per);

				avg_of_vismins += vismin_per;
				avg_of_vismaxes += vismax_per;
			}
		}

		avg_of_vismins = avg_of_vismins / gcount;
		avg_of_vismaxes = avg_of_vismaxes / gcount;

		if (min_of_vismins == max_of_vismins) {
			min_perT.setText(per_format.format(min_of_vismins));
		} else {
			if (show_min_and_max) {
				min_perT.setText(per_format.format(min_of_vismins) + " : " + per_format.format(max_of_vismins));
			} else {
				min_perT.setText("");
			}
		}
		if (min_of_vismaxes == max_of_vismaxes) {
			max_perT.setText(per_format.format(max_of_vismaxes));
		} else {
			if (show_min_and_max) {
				max_perT.setText(per_format.format(min_of_vismaxes) + " : " + per_format.format(max_of_vismaxes));
			} else {
				max_perT.setText("");
			}
		}

		min_percent_slider.setValue((int) (avg_of_vismins * sliders_per_percent));
		max_percent_slider.setValue((int) (avg_of_vismaxes * sliders_per_percent));

		prev_min_per = avg_of_vismins;
		prev_max_per = avg_of_vismaxes;
	}

	private void setSlider(final JSlider slider, final int value) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				//turnOffListening();
				slider.setValue(value);
				slider.updateUI();
			//turnOnListening();
			}
		});
	}

	public void stateChanged(ChangeEvent evt) {
		if (graphs.size() <= 0) {
			return;
		}
		Object src = evt.getSource();

		if (src == max_percent_slider) {
			if (max_percent_slider.getValue() < min_percent_slider.getValue() + 1) {
				setSlider(max_percent_slider, min_percent_slider.getValue() + 1);
				return;
			}
			setVisibleMaxPercent(max_percent_slider.getValue() / sliders_per_percent);
		} else if (src == min_percent_slider) {
			if (min_percent_slider.getValue() > max_percent_slider.getValue() - 1) {
				setSlider(min_percent_slider, max_percent_slider.getValue() - 1);
				return;
			}
			setVisibleMinPercent(min_percent_slider.getValue() / sliders_per_percent);
		} else if (src == max_val_slider) {
			if (max_val_slider.getValue() < min_val_slider.getValue() + 1) {
				setSlider(max_val_slider, min_val_slider.getValue() + 1);
				return;
			}
			setVisibleMaxValue(max_val_slider.getValue() / sliders_per_val);
		} else if (src == min_val_slider) {
			if (min_val_slider.getValue() > max_val_slider.getValue() - 1) {
				setSlider(min_val_slider, max_val_slider.getValue() - 1);
				return;
			}
			setVisibleMinValue(min_val_slider.getValue() / sliders_per_val);
		}
	}

	/** When a JTextField gains focus, do nothing special. */
	public void focusGained(FocusEvent e) {
	}

	/** When a JTextField loses focus, process its value. */
	public void focusLost(FocusEvent e) {
		Object src = e.getSource();
		if (src instanceof JTextField) {
			doAction(src);
		}
	}

	public void actionPerformed(ActionEvent evt) {
		doAction(evt.getSource());
	}

	private void doAction(Object src) {
		if (graphs.size() <= 0) {
			return;
		}

		if (src == min_valT) {
			try {
				float minval = GraphAdjusterView.numberParser.parse(min_valT.getText()).floatValue();
				if (minval > prev_max_val - val_offset) {
					minval = prev_max_val - val_offset;
				}
				// do not enforce an absolute minimum in the text field.
				// let the user enter any value to get the desired scaling.
				// this flexibility was requested on SourceForge.
				//else if (minval < abs_min_val) { minval = abs_min_val; }
				setVisibleMinValue(minval);
			} catch (ParseException ex) {
				min_valT.setText(val_format.format(prev_min_val));
			}
		} else if (src == max_valT) {
			try {
				float maxval = GraphAdjusterView.numberParser.parse(max_valT.getText()).floatValue();
				if (maxval < prev_min_val + val_offset) {
					maxval = prev_min_val + val_offset;
				}
				// do not enforce an absolute maximum in the text field.
				// let the user enter any value to get the desired scaling
				//else if (maxval > abs_max_val) { maxval = abs_max_val; }
				setVisibleMaxValue(maxval);
			} catch (ParseException ex) {
				max_valT.setText(val_format.format(prev_max_val));
			}
		} else if (src == min_perT) {
			try {
				float min_per = GraphAdjusterView.parsePercent(min_perT.getText());
				if (min_per < 0) {
					min_per = 0;
				} else if (min_per > prev_max_per - per_offset) {
					min_per = prev_max_per - per_offset;
				}
				setVisibleMinPercent(min_per);  // resets min_perT text also
			} catch (ParseException ex) {
				min_perT.setText(per_format.format(prev_min_per));
			}
		} else if (src == max_perT) {
			try {
				float max_per = GraphAdjusterView.parsePercent(max_perT.getText());
				if (max_per < prev_min_per + per_offset) {
					max_per = prev_min_per + per_offset;
				} else if (max_per > 100) {
					max_per = 100;
				}
				setVisibleMaxPercent(max_per);  // resets max_perT text also
			} catch (ParseException ex) {
				max_perT.setText(per_format.format(prev_max_per));
			}
		}
	}

	private void setVisibleMinValue(float val) {
		int gcount = graphs.size();
		if (gcount > 0 && (val != prev_min_val)) {
			turnOffListening();

			float min_of_mins = Float.POSITIVE_INFINITY;
			float max_of_mins = Float.NEGATIVE_INFINITY;
			float avg_of_mins = 0;
			// set values
			for (int i = 0; i < gcount; i++) {
				GraphGlyph gl = graphs.get(i);
				float min_per = getPercentForValue(gl, val);
				min_of_mins = Math.min(min_per, min_of_mins);
				max_of_mins = Math.max(min_per, max_of_mins);
				avg_of_mins += min_per;
				gl.setVisibleMinY(val);
			}
			avg_of_mins = avg_of_mins / gcount;
			if (widg != null) {
				widg.updateWidget();
			}

			// set values
			min_valT.setText(val_format.format(val));
			min_val_slider.setValue((int) (val * sliders_per_val));

			// then set percentages
			if (min_of_mins == max_of_mins) {
				min_perT.setText(per_format.format(min_of_mins));
			} else {
				if (show_min_and_max) {
					min_perT.setText(per_format.format(min_of_mins) + " : " + per_format.format(max_of_mins));
				} else {
					min_perT.setText("");
				}
			}
			min_percent_slider.setValue((int) (avg_of_mins * sliders_per_percent));

			prev_min_val = val;
			//      prev_min_per = avg_of_mins; ???
			turnOnListening();
		}
	}

	private void setVisibleMaxValue(float val) {
		int gcount = graphs.size();
		if (gcount > 0 && (val != prev_max_val)) {
			turnOffListening();

			float min_of_maxes = Float.POSITIVE_INFINITY;
			float max_of_maxes = Float.NEGATIVE_INFINITY;
			float avg_of_maxes = 0;
			for (int i = 0; i < gcount; i++) {
				GraphGlyph gl = graphs.get(i);
				float max_per = getPercentForValue(gl, val);
				min_of_maxes = Math.min(max_per, min_of_maxes);
				max_of_maxes = Math.max(max_per, max_of_maxes);
				avg_of_maxes += max_per;
				gl.setVisibleMaxY(val);
			}
			avg_of_maxes = avg_of_maxes / gcount;
			if (widg != null) {
				widg.updateWidget();
			}

			max_valT.setText(val_format.format(val));
			max_val_slider.setValue((int) (val * sliders_per_val));

			if (min_of_maxes == max_of_maxes) {
				max_perT.setText(per_format.format(min_of_maxes));
			} else {
				if (show_min_and_max) {
					max_perT.setText(per_format.format(min_of_maxes) + " : " + per_format.format(max_of_maxes));
				} else {
					max_perT.setText("");
				}
			}
			max_percent_slider.setValue((int) (avg_of_maxes * sliders_per_percent));

			prev_max_val = val;
			//      prev_max_per = avg_of_maxes; ???
			turnOnListening();
		}
	}

	/**
	 *   Set visible min Y to the specified percent value for all graphs under control
	 *   of GraphVisibleBoundsSetter, adjusts the controls, and updates the widget.
	 */
	private void setVisibleMinPercent(float percent) {
		//    System.out.println("setting min percent: " + percent + ", previous: " + prev_min_per);
		int gcount = graphs.size();
		if (gcount > 0 /*&& (percent != prev_min_per) */) {
			turnOffListening();

			if (percent > prev_max_per - per_offset) {
				percent = prev_max_per - per_offset;
			}
			if (percent < 0) {
				percent = 0;
			}

			float min_of_mins = Float.POSITIVE_INFINITY;
			float max_of_mins = Float.NEGATIVE_INFINITY;
			float avg_of_mins = 0;
			// set percentages
			for (int i = 0; i < gcount; i++) {
				GraphGlyph gl = graphs.get(i);
				float min_val = getValueForPercent(gl, percent);
				min_of_mins = Math.min(min_val, min_of_mins);
				max_of_mins = Math.max(min_val, max_of_mins);
				avg_of_mins += min_val;
				gl.setVisibleMinY(min_val);
			}
			avg_of_mins = avg_of_mins / gcount;
			if (widg != null) {
				widg.updateWidget();
			}

			// set percents
			min_perT.setText(per_format.format(percent));
			min_percent_slider.setValue((int) (percent * sliders_per_percent));

			// then set values
			if (min_of_mins == max_of_mins) {
				min_valT.setText(val_format.format(min_of_mins));
			} else {
				if (show_min_and_max) {
					min_valT.setText(val_format.format(min_of_mins) + " : " + val_format.format(max_of_mins));
				} else {
					min_valT.setText("");
				}
			}
			min_val_slider.setValue((int) (avg_of_mins * sliders_per_val));

			prev_min_per = percent;
			//      prev_min_val = avg_of_mins; ???
			turnOnListening();
		}
	}

	/**
	 *   Set visible max Y to the specified percent value for all graphs under control
	 *   of GraphVisibleBoundsSetter, adjusts the controls, and updates the widget.
	 */
	private void setVisibleMaxPercent(float percent) {
		int gcount = graphs.size();

		if (gcount > 0 /*&& (percent != prev_max_per)*/) {
			turnOffListening();

			if (percent < prev_min_per + per_offset) {
				percent = prev_min_per + per_offset;
			}
			if (percent > 100) {
				percent = 100;
			}

			max_perT.setText(per_format.format(percent));
			max_percent_slider.setValue((int) (percent * sliders_per_percent));

			float min_of_maxes = Float.POSITIVE_INFINITY;
			float max_of_maxes = Float.NEGATIVE_INFINITY;
			float avg_of_maxes = 0;
			for (int i = 0; i < gcount; i++) {
				GraphGlyph gl = graphs.get(i);
				float max_val = getValueForPercent(gl, percent);
				min_of_maxes = Math.min(max_val, min_of_maxes);
				max_of_maxes = Math.max(max_val, max_of_maxes);
				avg_of_maxes += max_val;
				gl.setVisibleMaxY(max_val);
			}
			avg_of_maxes = avg_of_maxes / gcount;
			if (widg != null) {
				widg.updateWidget();
			}

			if (min_of_maxes == max_of_maxes) {
				max_valT.setText(val_format.format(min_of_maxes));
			} else {
				if (show_min_and_max) {
					max_valT.setText(val_format.format(min_of_maxes) + " : " + val_format.format(max_of_maxes));
				} else {
					max_valT.setText("");
				}
			}
			max_val_slider.setValue((int) (avg_of_maxes * sliders_per_val));

			prev_max_per = percent;

			turnOnListening();
		}
	}

	/**
	 *  Gets the percents2scores array for the given graph, creating the array
	 *  if necessary.
	 */
	private float[] getPercents2Scores(GraphGlyph gl) {
		Object info = gl.getInfo();
		if (info == null) {
			System.err.println("Graph has no info! " + gl);
		}
		float[] p2score = info2pscores.get(info);

		if (p2score == null) {
			float[] ycoords = gl.copyYCoords();
			p2score = GraphSymUtils.calcPercents2Scores(ycoords, sliders_per_percent);
			info2pscores.put(info, p2score);
		}
		return p2score;
	}

	float getValueForPercent(GraphGlyph gl, float percent) {
		float[] percent2score = getPercents2Scores(gl);
		int index = Math.round(percent * sliders_per_percent);

		// I have actually seen a case where index was calculated as -1,
		// and an exception was thrown. That is why I added this check. (Ed)
		if (index < 0) {
			index = 0;
		} else if (index >= percent2score.length) {
			index = percent2score.length - 1;
		}

		return percent2score[index];
	}

	float getPercentForValue(GraphGlyph gl, float value) {
		float percent = Float.NEGATIVE_INFINITY;
		float[] percent2score = getPercents2Scores(gl);
		// do a binary search through percent2score array to find percent bin closest to value
		int index = Arrays.binarySearch(percent2score, value);
		if (index < 0) {
			index = -index - 2;
		}
		if (index >= percent2score.length) {
			percent = 100;
		} else if (value >= gl.getGraphMaxY()) {
			percent = 100;
		} else if (index < 0) {
			percent = 0;
		} else if (value <= gl.getGraphMinY()) {
			percent = 0;
		} else {
			percent = index / sliders_per_percent;
		}

		return percent;
	}

	private void turnOffListening() {
		min_percent_slider.removeChangeListener(this);
		max_percent_slider.removeChangeListener(this);
		min_val_slider.removeChangeListener(this);
		max_val_slider.removeChangeListener(this);
		min_perT.removeActionListener(this);
		max_perT.removeActionListener(this);
		min_valT.removeActionListener(this);
		max_valT.removeActionListener(this);
		min_perT.removeFocusListener(this);
		max_perT.removeFocusListener(this);
		min_valT.removeFocusListener(this);
		max_valT.removeFocusListener(this);
	}

	private void turnOnListening() {
		min_percent_slider.addChangeListener(this);
		max_percent_slider.addChangeListener(this);
		min_val_slider.addChangeListener(this);
		max_val_slider.addChangeListener(this);
		min_perT.addActionListener(this);
		max_perT.addActionListener(this);
		min_valT.addActionListener(this);
		max_valT.addActionListener(this);
		min_perT.addFocusListener(this);
		max_perT.addFocusListener(this);
		min_valT.addFocusListener(this);
		max_valT.addFocusListener(this);
	}

}
