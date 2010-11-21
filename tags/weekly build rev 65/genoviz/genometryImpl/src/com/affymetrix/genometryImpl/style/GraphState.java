/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.style;

/**
 *  Encapsulates information needed to restore the visual appearance of
 *    a graph stored at a URL.
 */
public final class GraphState {

		//TODO: Make this an enum
	public static final int THRESHOLD_DIRECTION_GREATER = 1;
	public static final int THRESHOLD_DIRECTION_BETWEEN = 0;
	public static final int THRESHOLD_DIRECTION_LESS = -1;

	private GraphType graph_style = GraphType.MINMAXAVG;

	/**
	 *  The minimum score the ycoord of a point must be >= in order to be counted
	 *     as passing threshold.
	 */
	private float min_score_threshold = Float.NEGATIVE_INFINITY;  // NEGATIVE_INFINITY means hasn't been set yet

	/**
	 *  The maximum score the ycoord of a point must be <= in order to be counted
	 *    as passing threshold
	 */
	private float max_score_threshold = Float.POSITIVE_INFINITY;  // POSITIVE_INFINITY means hasn't been set yet

	private int threshold_direction = THRESHOLD_DIRECTION_GREATER;


	/**
	 *  The maximum distance (in xcooords) allowed between points that
	 *     exceed the min_score_threshold in order for region between points to be painted
	 */
	private int max_gap_threshold = 100;  // max_gap

	/**
	 *  The minumum length (in xcoords) that must be covered by a region that
	 *    meets min_score and max_distance thresholds in order for region between
	 *    points to be painted
	 */
	private int min_run_threshold = 30;

	/**  how much to shift xcoords of span start when painting spans that pass thresholds */
	private double span_start_shift = 12;  // currently should be 12 for transcriptome graphs
	/**  how much to shift xcoords of span end when painting spans that pass thresholds */
	private double span_end_shift = 13;  // currently should be 13 for transcriptome graphs

	/**
	 *  visible_max_ycoord is the max ycoord (in graph coords) that is visible (rendered)
	 *  this number is set to point_max_ycoord in setPointCoords(), but can be modified via
	 *     calls to setVisibleMaxY, and the visual effect is to threhsold the graph drawing
	 *     so that any points above visible_max_ycoord render as visible_max_ycoord
	 */
	private float graph_visible_min = Float.NEGATIVE_INFINITY;  // NEGATIVE_INFINITY means hasn't been set yet
	private float graph_visible_max = Float.POSITIVE_INFINITY;  // POSITIVE_INFINITY means hasn't been set yet

	// if float_graph, then graph should float above annotations in tiers
	// if !float_graph, then graph should be in its own tier
	private boolean float_graph = false;
	private boolean show_threshold = false;
	private boolean show_axis = false;
	private boolean show_graph = true;
	private boolean show_bounds = false;
	private boolean show_label = true;

	private HeatMap heat_map;
	private final ITrackStyle tier_style;
	private ITrackStyle combo_tier_style = null;

	public static final float default_graph_height = 100.0f;


	/** Convert a graph type name such as "line", "bar", "dot", etc., to an Integer,
	 *  or null if no such style.
	 */
	public static GraphType getStyleNumber(String style_name) {
		return GraphType.fromString(style_name);
	}

	/** Creates a new GraphState.  Uses the global state provider to generate
	 *  an initial IAnnotStyle for the tier_style, but you can replace that style if
	 *  necessary.
	 */
	public GraphState(String id) {
		super();

		StateProvider provider = DefaultStateProvider.getGlobalStateProvider();
		tier_style = provider.getAnnotStyle(id);
		tier_style.setGraphTier(true);

		// Graph Tiers with a single graph in them are not collapsible/expandible
		tier_style.setExpandable(false);
		tier_style.setHeight(default_graph_height);

		setFloatGraph(false);
	}

	/** Copy all the properties, except ID and label, of the given state into this state. */
	public void copyProperties(GraphState ostate) {
		setGraphStyle(ostate.getGraphStyle());
		setVisibleMinY(ostate.getVisibleMinY());
		setVisibleMaxY(ostate.getVisibleMaxY());
		setFloatGraph(ostate.getFloatGraph());
		setShowThreshold(ostate.getShowThreshold());
		setShowAxis(ostate.getShowAxis());
		setShowGraph(ostate.getShowGraph());
		setShowBounds(ostate.getShowBounds());
		setShowLabel(ostate.getShowLabel());
		setThresholdDirection(ostate.getThresholdDirection());
		setMinScoreThreshold(ostate.getMinScoreThreshold());
		setMaxScoreThreshold(ostate.getMaxScoreThreshold());
		setMaxGapThreshold(ostate.getMaxGapThreshold());
		setMinRunThreshold(ostate.getMinRunThreshold());
		setThreshStartShift(ostate.getThreshStartShift());
		setThreshEndShift(ostate.getThreshEndShift());
		setHeatMap(ostate.getHeatMap());

		getTierStyle().copyPropertiesFrom(ostate.getTierStyle());
	}

	public final GraphType getGraphStyle() { return graph_style; }
	public HeatMap getHeatMap() {
		if (heat_map == null) {
			heat_map = HeatMap.StandardHeatMap.BLACK_WHITE.getHeatMap();
		}
		return heat_map;
	}
	public final float getVisibleMinY() { return graph_visible_min; }
	public final float getVisibleMaxY() { return graph_visible_max; }

	public final boolean getFloatGraph() { return  float_graph; }
	public final boolean getShowThreshold() { return show_threshold; }
	public final boolean getShowAxis() { return show_axis; }

	public final boolean getShowGraph() { return show_graph; }
	public final boolean getShowBounds() { return show_bounds; }
	public final boolean getShowLabel() { return show_label; }

	public float getMinScoreThreshold() { return min_score_threshold; }
	public float getMaxScoreThreshold() { return max_score_threshold; }

	public int getMaxGapThreshold() { return max_gap_threshold; }
	public int getMinRunThreshold() { return min_run_threshold; }
	public double getThreshStartShift() { return span_start_shift; }
	public double getThreshEndShift() { return span_end_shift; }

	/**
	 *  Returns either {@link #THRESHOLD_DIRECTION_GREATER} or
	 *  {@link #THRESHOLD_DIRECTION_LESS} or {@link #THRESHOLD_DIRECTION_BETWEEN}.
	 */
	public int getThresholdDirection() { return threshold_direction; }

	public final void setFloatGraph(boolean b) { float_graph = b; }
	public final void setGraphStyle(GraphType style) { graph_style = style;}
	public final void setHeatMap(HeatMap hmap) { heat_map = hmap;}
	public final void setVisibleMinY(float vminy) { graph_visible_min = vminy;}  // check
	public final void setVisibleMaxY(float vmaxy) { graph_visible_max = vmaxy;}  // check
	public final void setShowThreshold(boolean b) { show_threshold = b;} // check
	public final void setShowAxis(boolean b) { show_axis = b;} // check
	public final void setShowGraph(boolean b) { show_graph = b;}    // check
	public final void setShowBounds(boolean b) { show_bounds = b;}  // check
	public final void setShowLabel(boolean b) { show_label = b;}    // check

	public void setMinScoreThreshold(float thresh) { min_score_threshold = thresh;}
	public void setMaxScoreThreshold(float thresh) { max_score_threshold = thresh;}
	public void setMaxGapThreshold(int thresh) { max_gap_threshold = thresh;}
	public void setMinRunThreshold(int thresh) { min_run_threshold = thresh;}
	public void setThreshStartShift(double d) { span_start_shift = d;}
	public void setThreshEndShift(double d) { span_end_shift = d;}

	/**
	 *  Set to either {@link #THRESHOLD_DIRECTION_GREATER} or
	 *  {@link #THRESHOLD_DIRECTION_LESS} or {@link #THRESHOLD_DIRECTION_BETWEEN}.
	 */
	public void setThresholdDirection(int d) {
		if (d != THRESHOLD_DIRECTION_GREATER && d != THRESHOLD_DIRECTION_LESS
				&& d != THRESHOLD_DIRECTION_BETWEEN) {
			throw new IllegalArgumentException();
				}
		threshold_direction = d;
	}

	public ITrackStyle getTierStyle() {
		return tier_style;
	}

	public ITrackStyle getComboStyle() {
		return combo_tier_style;
	}

	public void setComboStyle(ITrackStyle s) {
		this.combo_tier_style = s;
	}

	float[] gridLineYValues = null;

	public void setGridLinesYValues(float[] f) {
		gridLineYValues = f;
	}

	public float[] getGridLinesYValues() {
		return gridLineYValues;
	}

	public static boolean isHeatMapStyle(GraphType graph_style) {
		return (graph_style == GraphType.HEAT_MAP);
	}
}
