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
package com.affymetrix.genoviz.widget;

import java.awt.event.*;
import java.awt.geom.Rectangle2D.Double;
import java.util.*;
import java.awt.geom.Point2D;

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.Glyph;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.datamodel.BaseCall;
import com.affymetrix.genoviz.datamodel.BaseCalls;
import com.affymetrix.genoviz.datamodel.BaseConfidence;
import com.affymetrix.genoviz.datamodel.Mapping;
import com.affymetrix.genoviz.datamodel.NullBaseCalls;
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.datamodel.Sequence;
import com.affymetrix.genoviz.datamodel.TraceI;
import com.affymetrix.genoviz.event.NeoBaseSelectEvent;
import com.affymetrix.genoviz.event.NeoBaseSelectListener;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.util.Selection;
import com.affymetrix.genoviz.widget.neotracer.AsymAxisGlyph;
import com.affymetrix.genoviz.widget.neotracer.TraceBaseGlyph;
import com.affymetrix.genoviz.widget.neotracer.TraceGlyph;
import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.JScrollBar;

/**
 * Implementers display a chromatogram from a sequencing machine.
 * The chromatogram is usualy visualized by four traces representing the four bases found in DNA.
 * Bases "called" from the chromatogram can be displayed along the chromatogram.
 * Initializing, selecting, highlighting, cropping, and interrogating are provided.
 *
 * <p> Example:
 * <pre>
 *   URL scfURL = new URL(this.getDocumentBase(), getParameter("scf_file")));
 *   Trace trace = new Trace(scfURL);
 *   NeoTracerI tracer = new NeoTracer();
 *   tracer.setTrace(trace);
 *   tracer.updateWidget(); // display
 *   tracer.centerAtBase(44);
 *   tracer.selectResidues(30,50);
 *   tracer.clearSelect();
 * </pre>
 *
 * <p> This javadoc explains the implementation specific features of this widget
 * concerning event handling and the java AWT.
 * In paticular, all genoviz implementations
 * of widget interfaces are subclassed from <code>Container</code>.
 *
 * <p> NeoTracer extends <code>java.awt.Container</code>,
 * and thus, inherits all of the AWT methods
 * of <code>java.awt.Container</code>,
 * and <code>Component</code>.
 * For example, a typical application might use the following
 * as part of initialization:
 * <pre>
 *   trace = new NeoTracer();
 *
 *   trace.setBackground(new Color(180, 250, 250));
 *   trace.resize(500, 250);
 * </pre>
 *
 * @version $Id: NeoTracer.java 6331 2010-07-02 15:48:14Z vbishnoi $
 */
public class NeoTracer extends NeoContainerWidget
		implements Observer, NeoViewBoxListener {

	/**
	 * Orientation (Direction) for a trace.
	 * If orientation is FORWARD,
	 * then the trace is shown as originally loaded.
	 *
	 * @see #setDirection
	 */
	public static final int FORWARD = 1;
	/**
	 * Orientation (Direction) for a trace.
	 * If the orientation is REVERSE_COMPLEMENT,
	 * logical reverse complement is performed on the trace.
	 * End coord becomes beg coord and vice versa,
	 * reverse complement of bases are used, and trace colors are also
	 * complemented.
	 *
	 * @see #setDirection
	 */
	public static final int REVERSE_COMPLEMENT = 2;
	/**
	 * component identifier constant for the trace chromogram display
	 * @see #getItems
	 */
	public static final int TRACES = 7000;
	/**
	 * component identifier constant for the base letter display
	 * @see #getItems
	 */
	public static final int BASES = TRACES + 1;
	/**
	 * component identifier constant for the panning axis scroller
	 * @see #getItems
	 */
	public static final int AXIS_SCROLLER = TRACES + 1;
	/**
	 * component identifier constant for the zooming adjustable
	 * @see #getItems
	 */
	public static final int AXIS_ZOOMER = TRACES + 2;
	/**
	 * component identifier constant for the vertical scaling adjustable
	 * @see #getItems
	 */
	public static final int OFFSET_ZOOMER = TRACES + 3;
	/**
	 * component identifier constant for other components not part
	 * of the interface description.
	 * @see #getItems
	 * @deprecated NeoTraceI.UNKNOWN used to hide NeoConstants.UNKNOWN
	 */
	@Deprecated
	public static final int UNKNOWN = TRACES + 4;
	//  To allow multiple base calls, this can no longer be static final.
	private int base_map_pixel_height;
	protected NeoMap trace_map;
	protected NeoMap base_map;
	protected JScrollBar hscroll;
	protected Adjustable hzoom, vzoom;
	// locations for scrollbars, consensus, and labels
	protected int hscroll_loc = PLACEMENT_BOTTOM;
	protected int vzoom_loc = PLACEMENT_RIGHT;
	protected int hzoom_loc = PLACEMENT_TOP;
	protected int trace_loc = PLACEMENT_TOP;
	protected int base_loc = PLACEMENT_BOTTOM;
	private static final int PEAK_WIDTH = 10;
	protected TraceI trace;
	protected TraceGlyph trace_glyph;
	protected List<BaseCalls> base_calls_vector; // list of BaseCallss;
	protected List<TraceBaseGlyph> base_glyphs; // list of TraceBaseGlyphs
	private AsymAxisGlyph base_axis;
	private TraceBaseGlyph activeBaseCallsGlyph;
	// optional - for aligned bases
	private BaseCalls consensus; // reference string
	private final Set<NeoBaseSelectListener> base_listeners = new CopyOnWriteArraySet<NeoBaseSelectListener>();
	protected Glyph line_glyph;
	protected FillRectGlyph left_trim_glyph, right_trim_glyph;
	private static final Color default_trace_background = Color.black;
	private static final Color default_base_background = Color.black;
	private static final Color default_panel_background = Color.lightGray;
	protected Color trim_color = Color.lightGray;
	protected int sel_behavior = ON_MOUSE_DOWN;
	protected Selection sel_range;
	protected Selection traceSelection;
	protected int trace_length;
	protected int trace_height_max;
	protected int base_count;
	protected boolean optimize_scrolling = false;
	protected boolean optimize_damage = false;
	protected boolean forward = true;
	protected boolean hscroll_show, hzoom_show, vzoom_show;
	protected Range range;
	protected Set<NeoRangeListener> range_listeners = new CopyOnWriteArraySet<NeoRangeListener>();

	/**
	 * constructs a NeoTracer using all the built-in controls.
	 */
	public NeoTracer() {
		this(true, true, true);
	}

	/**
	 * Construct a new NeoTracer with the given Adjustables.
	 *
	 * @param scroller for scrolling horizontally
	 * @param hzoom for zooming horizontally
	 * @param vzoom for zooming vertically
	 * respectively.
	 */
	public NeoTracer(JScrollBar scroller, Adjustable hzoom, Adjustable vzoom) {
		this(true, true, true);
		setScroller(scroller);
		setHorizontalZoomer(hzoom);
		setVerticalZoomer(vzoom);
	}

	/**
	 * constructs a NeoTracer with some built in controls.
	 *
	 * @param hscroll_show indicates whether or not to use the built-in horizontal scroller.
	 * @param hzoom_show indicates whether or not to use the built-in horizontal zoomer.
	 * @param vzoom_show indicates whether or not to use the built-in vertical zoomer.
	 */
	public NeoTracer(boolean hscroll_show,
			boolean hzoom_show, boolean vzoom_show) {
		super();
		this.hscroll_show = hscroll_show;
		this.hzoom_show = hzoom_show;
		this.vzoom_show = vzoom_show;

		trace_map = new NeoMap(false, false);
		base_map = new NeoMap(false, false);

		base_calls_vector = new ArrayList<BaseCalls>();
		base_glyphs = new ArrayList<TraceBaseGlyph>();

		this.setBackground(default_panel_background);
		trace_map.setMapColor(default_trace_background);
		base_map.setMapColor(default_base_background);
		this.setLayout(null);

		if (hscroll_show) {
			hscroll = new JScrollBar(JScrollBar.HORIZONTAL);
			add((Component) hscroll);
			setRangeScroller(hscroll);
		}
		if (hzoom_show) {
			hzoom = new JScrollBar(JScrollBar.VERTICAL);
			add((Component) hzoom);
			setRangeZoomer(hzoom);
		}
		if (vzoom_show) {
			vzoom = new JScrollBar(JScrollBar.VERTICAL);
			add((Component) vzoom);
			setOffsetZoomer(vzoom);
		}

		add(trace_map);
		add(base_map);

		trace_map.setScaleConstraint(NeoMap.Y, NeoMap.INTEGRAL_COORDS);
		trace_map.setMapRange(0, 100);
		trace_map.setMapOffset(0, 100);
		trace_map.setReshapeBehavior(NeoMap.X, NeoConstants.NONE);
		trace_map.setReshapeBehavior(NeoMap.Y, FITWIDGET);
		trace_map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_END);
		trace_map.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_MIDDLE);

		base_map.setMapRange(0, 100);
		base_map.setMapOffset(0, 100);
		base_map.setReshapeBehavior(NeoMap.X, NeoConstants.NONE);
		base_map.setReshapeBehavior(NeoMap.Y, NeoMap.FITWIDGET);

		trace_map.addMouseListener(this);
		trace_map.addMouseMotionListener(this);
		trace_map.addKeyListener(this);

		// for transforming viewbox changes to NeoRangeEvents
		trace_map.addViewBoxListener(this);

		base_map.addMouseListener(this);
		base_map.addMouseMotionListener(this);
		base_map.addKeyListener(this);

		addWidget(trace_map);
		addWidget(base_map);

		setMinZoom(X, 1.0f);
		setMaxZoom(X, 10.0f);

		// calling stretch to fit to get side effects...
		stretchToFit(false, false);
		setRubberBandBehavior(false);

		// WARNING!  calling these before stretchToFit causes blank widget
		// to come up -- after that, scrolling or zooming causes traces to appear
		// and everything is ok
		setScrollingOptimized(optimize_scrolling);
		setDamageOptimized(optimize_damage);


		// Choosing a reasonable default scale to start at
		zoom(X, 3.0f);
		// Default to start at beginning of trace
		scroll(X, 0.0f);

		// Shouldn't need to call updateWidget() -- responsibility of application

		setSelection(new Selection());
		// For old times sake,
		// we define another selection
		// based on trace points
		// rather than residues.
		traceSelection = new Selection();
		addSelection(traceSelection);

	}

	/**
	 * a constructor for cloning.
	 */
	public NeoTracer(NeoTracer original) {
		this(); // First create a new one.
		setRoot(original); // Then copy or point to everything in the original.
	}

	protected void setRoot(NeoTracer root) {

		// We need to set up a derived view from  each individual NeoMap
		// within this widget based on corresponding NeoMap within the root.
		NeoMap root_trace_map = (NeoMap) root.getWidget(NeoTracer.TRACES);
		NeoMap root_base_map = (NeoMap) root.getWidget(NeoTracer.BASES);
		trace_map.setRoot(root_trace_map);
		base_map.setRoot(root_base_map);

		// Set various fields that need to be shared
		// between this widget and the root.

		// Object fields are being set here.
		// Since these fields are objects
		//   once they are assigned, unless reassigned they will continue to
		//   point to same object as corresponding fields in root.

		trace = root.getTrace();
		trace_glyph = root.trace_glyph;

		base_calls_vector = root.base_calls_vector;

		line_glyph = root.line_glyph;
		left_trim_glyph = root.left_trim_glyph;
		right_trim_glyph = root.right_trim_glyph;
		range = root.range;

		// more object fields to copy, these are inherited from NeoContainerWidget
		glyph_hash = root.glyph_hash;
		model_hash = root.model_hash;

		// What should happen here?
		setSelection(root.sel_range); // monitor the same selection as the root.
		// Or should we leave this alone.
		// If we comment out the above line,
		// then the clone will get a new Selection object,
		// created in the argumentless constructor.

		// Primitive types are being copied here.  This means that when setRoot()
		//   is called these will be synced with same fields in root, but after
		//   that they will act independently.  This is NOT the desired behavior.
		//   Therefore need to improve.  Possible options:
		// a.) Change all these field types from primitives to corresponding
		//       objects, then will act in sync since both root and this will
		//       continue pointing to same object
		// b.) put in check for "sibling" NeoAssemblers whenever these values
		//       change, and propogate change to each sibling

		sel_behavior = root.sel_behavior;

		trace_length = root.trace_length;
		trace_height_max = root.trace_height_max;
		base_count = root.base_count;

		optimize_scrolling = root.optimize_scrolling;
		optimize_damage = root.optimize_damage;

		// locations for scrollbars, consensus, and labels
		hscroll_loc = root.hscroll_loc;
		vzoom_loc = root.vzoom_loc;
		hzoom_loc = root.hzoom_loc;

		trace_loc = root.trace_loc;
		base_loc = root.base_loc;

		forward = root.forward;

		setTrimColor(root.getTrimColor());

	}

	public void setSelection(Selection theSelection) {
		if (null != sel_range) {
			sel_range.deleteObserver(this);
		}
		sel_range = theSelection;
		sel_range.addObserver(this);
	}

	public void addSelection(Selection theSelection) {
		theSelection.addObserver(this);
	}

	public void addSelectionObserver(Observer observer) {
		sel_range.addObserver(observer);
	}

	@Override
	public void setScrollingOptimized(boolean optimize) {
		this.optimize_scrolling = optimize;
		trace_map.setScrollingOptimized(optimize);
		base_map.setScrollingOptimized(optimize);
		if (optimize) {
			trace_map.setScaleConstraint(NeoMap.X, NeoMap.INTEGRAL_PIXELS);
			base_map.setScaleConstraint(NeoMap.X, NeoMap.INTEGRAL_PIXELS);
		} else {
			trace_map.setScaleConstraint(NeoMap.X, NeoConstants.NONE);
			base_map.setScaleConstraint(NeoMap.X, NeoConstants.NONE);
		}
		zoom(X, 1.0f);
	}

	@Override
	public void setDamageOptimized(boolean optimize) {
		this.optimize_damage = optimize;
		trace_map.setDamageOptimized(optimize);
		base_map.setDamageOptimized(optimize);
	}

	@Override
	public void scroll(int id, double coord_value) {
		if (id == X) {
			base_map.scroll(id, coord_value);
		}
		trace_map.scroll(id, coord_value);
	}

	@Override
	public void zoom(int id, double scale) {
		if (id == X) {
			base_map.zoom(id, scale);
		}
		trace_map.zoom(id, scale);
	}

	@Override
	public void setMinZoom(int id, double min) {
		if (id == X) {
			base_map.setMinZoom(id, min);
		}
		trace_map.setMinZoom(id, min);
	}

	@Override
	public void setMaxZoom(int id, double max) {
		if (id == X) {
			base_map.setMaxZoom(id, max);
		}
		trace_map.setMaxZoom(id, max);
	}

	/**
	 * Adjusts the bounds of the trace and sequence displayed.
	 * Allows clipping of the displayed sequence.
	 *
	 * @param start the integer indicating the starting sample position
	 * @param end  the integer indicating the final sample position.
	 */
	public void setRange(int start, int end) {
		this.range = new Range(start, end);
		trace_map.setMapRange(start, end);
		base_map.setMapRange(start, end);
		Rectangle2D.Double box = base_axis.getCoordBox();
		base_axis.setCoords(start, box.y, end - start, box.height);
	}

	/**
	 * @return the start of the range set by setRange.
	 */
	public int getRangeStart() {
		if (null == this.range) {
			return 0;
		}
		return this.range.beg;
	}

	/**
	 * @return the end of the range set by setRange.
	 */
	public int getRangeEnd() {
		if (null == this.range) {
			return 0;
		}
		return this.range.end;
	}

	public void configureLayout(int component, int placement) {
		if (component == AXIS_SCROLLER) {
			hscroll_loc = placement;
		} else if (component == AXIS_ZOOMER) {
			hzoom_loc = placement;
		} else if (component == OFFSET_ZOOMER) {
			vzoom_loc = placement;
		} else if (component == TRACES) {
			trace_loc = placement;
		} else if (component == BASES) {
			base_loc = placement;
		} else {
			throw new IllegalArgumentException(
					"can only configureLayout for AXIS_SCROLLER, AXIS_ZOOMER, "
					+ "OFFSET_ZOOMER, TRACES, or BASES.");
		}
		doLayout();
		// trying to fix paint issues when configuring layout
		Container parent = getParent();
		if (parent instanceof NeoPanel) {
			((NeoPanel) parent).forceBackgroundFill();
		}
		repaint();
	}

	public int getPlacement(int component) {
		if (component == AXIS_SCROLLER) {
			return hscroll_loc;
		} else if (component == AXIS_ZOOMER) {
			return hzoom_loc;
		} else if (component == OFFSET_ZOOMER) {
			return vzoom_loc;
		} else if (component == TRACES) {
			return trace_loc;
		} else if (component == BASES) {
			return base_loc;
		}
		throw new IllegalArgumentException(
				"can only getPlacement for AXIS_SCROLLER, AXIS_ZOOMER, "
				+ "OFFSET_ZOOMER, TRACES, or BASES.");
	}

	@Override
	public synchronized void doLayout() {

		Dimension dim = this.getSize();

		// The base_map_pixel_height is now being correctly set in addBaseGlyph.
		int base_height = base_map_pixel_height;
		int hz_size = 0, vz_size = 0, hs_size = 0;
		if (hscroll_show) {
			hs_size = ((Component) hscroll).getPreferredSize().height;
		}
		if (hzoom_show) {
			hz_size = ((Component) hzoom).getPreferredSize().width;
		}
		if (vzoom_show) {
			vz_size = ((Component) vzoom).getPreferredSize().width;
		}

		int trace_x = hz_size;
		int trace_y = 0;
		int trace_width = dim.width - hz_size - vz_size;
		int trace_height = dim.height - base_height - hs_size;

		int base_x = trace_x;
		int base_y = trace_height;
		int base_width = trace_width;

		trace_map.setBounds(trace_x, trace_y, trace_width, trace_height);
		base_map.setBounds(base_x, base_y, base_width, base_height);

		if (hscroll_show) {
			int hscroll_y = base_y + base_height;
			int hscroll_x = trace_x;
			int hscroll_width = trace_width;
			((Component) hscroll).setBounds(hscroll_x, hscroll_y, hscroll_width, hs_size);
		}

		if (vzoom_show) {
			int vzoom_x = trace_x + trace_width;
			int vzoom_y = 0;
			int vzoom_height = trace_height + base_height;
			((Component) vzoom).setBounds(vzoom_x, vzoom_y, vz_size, vzoom_height);
		}

		if (hzoom_show) {
			int hzoom_x = 0;
			int hzoom_y = 0;
			int hzoom_height = trace_height + base_height;
			((Component) hzoom).setBounds(hzoom_x, hzoom_y, hz_size, hzoom_height);
		}
	}

	public Range getVisiblePeakRange() {
		Rectangle2D.Double visible_box = trace_map.getView().calcCoordBox();
		return new Range((int) visible_box.x,
				(int) (visible_box.x + visible_box.width));
	}

	// this is a slow way to do this
	public Range getVisibleBaseRange() {
		Rectangle2D.Double visible_box = trace_map.getView().calcCoordBox();
		int visible_left = (int) (visible_box.x);
		int visible_right = (int) (visible_left + visible_box.width);
		Range r = new Range(-1, -1);
		BaseCall calledBase;
		int baseCoordPoint;

		if (getActiveBaseCalls() == null) {
			return r;
		}

		int baseCount = getBaseCount();

		if (getBaseCall(0).getTracePoint() > visible_left) {
			// left edge is past the edge of the view box, return -1
			r.beg = -99;
		}
		if (getBaseCall(baseCount - 1).getTracePoint() < visible_right) {
			// past the right edge
			r.end = -99;
		}

		for (int i = 0; i < baseCount; i++) {
			if ((calledBase = getBaseCall(i)) != null) {
				baseCoordPoint = calledBase.getTracePoint();
				if (r.beg == -1) { // looking for beginning
					if (baseCoordPoint >= visible_left) {
						r.beg = i;
						if (r.end == -99) { // already established end
							break;
						}
					}
				} else { // setting end
					if (baseCoordPoint >= (visible_right)) {
						break;
					} else {
						r.end = i;
					}
				}
			}
		}
		if (r.beg == -99) {
			r.beg = -1;
		}
		if (r.end == -99) {
			r.end = -1;
		}

		return r;
	}

	public void setChromatogram(TraceI theChromatogram) {
		setChromatogram(theChromatogram, 0);
	}

	/**
	 * Sets the chromatogram to display.
	 * @param theChromatogram ignore any base calls.
	 */
	public void setChromatogram(TraceI theChromatogram, int start) {

		this.trace = theChromatogram;
		trace_length = this.trace.getTraceLength();
		trace_height_max = this.trace.getMaxValue();

		trace_map.setMapRange(0, trace_length);
		// attempting to allow negative scrolling tss
		trace_map.setMapOffset(0, trace_height_max);

		if (trace_glyph == null) {
			trace_glyph = new TraceGlyph();
			trace_map.getScene().addGlyph(trace_glyph);
		}

		// might want to do
		//    trace_map.addItem(trace_map.getScene().getGlyph(), trace_glyph)
		// instead

		trace_glyph.setTrace(this.trace);
		trace_glyph.setCoords(start, 0, trace_length, trace_height_max);

		// Any initial zoom and scroll is currently being ovewritten
		//   by stretch to fit --

		base_map.setMapRange(0, trace_length);
		base_map.setMapOffset(0, base_map_pixel_height);

		line_glyph = new FillRectGlyph();
		//Changing the start of line_glyph from beginning of traces to start of the map
		line_glyph.setCoords(0, 1, trace_length, 1);
		line_glyph.setBackgroundColor(Color.white);
		base_map.getScene().addGlyph(line_glyph);
	}

	/**
	 * Sets the trace, starting at position 0.
	 *
	 * @see #setTrace(TraceI, int)
	 */
	public void setTrace(TraceI trace) {
		setTrace(trace, 0);
	}

	/**
	 * Allows for setting the trace starting at a particular coord point,
	 * which is useful for displaying several NeoTracers together in the
	 * same widget.
	 * <p>
	 * For backward compatibility,
	 * the trace may include a set of base calls.
	 * If so, those calls are removed from the trace
	 * and added directly to this object.
	 *
	 */
	public void setTrace(TraceI trace, int start) {
		setChromatogram(trace, start);
		if (0 < trace.getBaseCount()) { // deprecated storage of base calls has been done.
			// Move the base calls out of the trace
			// and add them directly.
			this.base_count = trace.getBaseCount();
			if (trace.getActiveBaseCalls() != null) {
				BaseCall[] b = trace.getActiveBaseCalls().getBaseCalls();
				base_map.removeItem((GlyphI) base_glyphs);
				base_calls_vector.clear();
				base_glyphs.clear();
				addBaseCalls(b, start);
			}
		}
	}

	/**
	 * @return the Trace set via setTrace.
	 *
	 * @see #setTrace
	 */
	public TraceI getTrace() {
		return this.trace;
	}

	/**
	 * replace the base calls below the axis.
	 * Before Tao tried to allow multiple sets of base calls,
	 * There was exactly one set of base calls.
	 * This method was used to replace those.
	 * For example, one could replace ABI calls with Phred calls.
	 *
	 * @param theCalls an array of new calls.
	 */
	public void replaceBaseCalls(BaseCall[] theCalls) {
		if (this.base_calls_vector.size() < 1) {
			addBaseCalls(theCalls, 0);
		} else {
			replaceBaseCalls(theCalls, 0);
		}
	}

	private void redrawRevComp(Double tbox, Double vbox) {
		double twidth = tbox.width;
		double tbeg = tbox.x;
		double tend = tbeg + twidth;
		double vwidth = vbox.width;
		double vbeg = vbox.x;
		double vend = vbeg + vwidth;
		// testing -1 here as well...
		double newbeg = tend - vend;
		if (newbeg < tbeg) {
			newbeg = tbeg;
		} else if ((newbeg + vwidth) > tend) {
			newbeg = tend - vbeg;
		}
		trace_map.scrollRange(newbeg);
		base_map.scrollRange(newbeg);
		// There seems to be a bug in scrolling.
		// Occasionally the scrollbar is not getting properly adjusted.
		// Forcing the adjustment here appears to fix the problem.
		// This could be a synchronization problem. -- need to synchronize
		//    NeoMap.scrollRange() ???    GAH 12-9-97
		base_map.adjustScroller(NeoMap.X);
		double beg;
		double end;
		double new_beg;
		double new_end;
		// If part of the trace is selected,
		// we need to reverse the selection.
		//
		//Now adding other observers, must fix bug TODO!! 5/11/99
		// Note that this introduces a bug
		// if there are other observers of sel_range.
		// In that case,
		// we should probably remove ourselves
		// as observers of sel_range.
		// We might then create a new Selection to observe.
		if (!sel_range.isEmpty()) {
			beg = sel_range.getStart();
			end = sel_range.getEnd();
			new_beg = (int) (getBaseCount() - end - 1);
			new_end = (int) (getBaseCount() - beg - 1);
			sel_range.setRange((int) new_beg, (int) new_end);
			sel_range.notifyObservers();
		}
		if (trace_glyph.getChildCount() > 0) {
			List<GlyphI> gchildren = trace_glyph.getChildren();
			GlyphI gchild;
			Rectangle2D.Double childbox;
			for (int i = 0; i < gchildren.size(); i++) {
				gchild = gchildren.get(i);
				// since selection glyph already dealt with, need to skip it
				//    should try eliminating special seleciton handling above and
				//    see if can just deal with it here -- GAH 12-9-97
				if (gchild == trace_glyph.getSelectionGlyph()) {
					continue;
				}
				childbox = gchild.getCoordBox();
				beg = childbox.x;
				end = childbox.x + childbox.width;
				// need a -1 for some reason???!!!...
				new_beg = (int) (tend - end - 1);
				gchild.setCoords(new_beg, childbox.y, childbox.width, childbox.height);
			}
		} // and the horizontal line separating the traces from the base calls. elb - 1999-12-07
	}

	// TODO: Add this to NeoTracerI?
	// Note: This has only been tested with bases_index 0. elb 1999-12-01
	/**
	 * replace the base calls below the axis.
	 * Before Tao tried to allow multiple sets of base calls,
	 * there was no second parameter to this method.
	 * There was exactly one set of base calls.
	 * This method was used to replace those.
	 * For example, one could replace ABI calls with Phred calls.
	 *
	 * @param theCalls an array of new calls.
	 * @param bases_index an index into all the sets of base calls.
	 */
	private void replaceBaseCalls(BaseCall[] theCalls, int bases_index) {
		try {
			BaseCalls bc = base_calls_vector.get(bases_index);
			bc.setBaseCalls(theCalls);
		} catch (ArrayIndexOutOfBoundsException e) {
		}
	}


	public void addBaseCalls(BaseCall[] residues, int start) {
		BaseCalls base_calls = new BaseCalls(residues);
		addBaseCalls(base_calls, start);
	}


	/**
	Allows for setting the base calls starting at a particular coord point,
	which is useful for displaying several NeoTracers together in the
	same widget.  Make sure that the trace you are displaying has the same
	start; you can do this by using setTrace ( TraceI trace, int start ),
	with a trace that has basecalls in it; that method will call this one.
	 */
	public void addBaseCalls(BaseCalls base_calls, int start) {
		base_calls.setTrace(trace);
		base_calls_vector.add(base_calls);

		// Create a new glyph for the base calls.
		TraceBaseGlyph base_glyph = new TraceBaseGlyph();
		base_glyph.setTrace(trace);
		base_glyph.setBaseCalls(base_calls);

		final int old_base_height = 4; // arbitrary number to keep highlighting rectangle from colliding with horizontal rule.
		int iBaseGlyphs = base_glyphs.size();
		for (int i = 0; i < iBaseGlyphs; i++) {
			TraceBaseGlyph old_base_glyph = base_glyphs.get(i);
//			old_base_height += old_base_glyph.getHeight() + 1;
		}

		final int last_glyph_height = base_glyph.getHeight();
		base_glyph.setCoords(start, old_base_height, trace_length, last_glyph_height);
		base_map_pixel_height = old_base_height + last_glyph_height;

		base_glyphs.add(base_glyph);

		// adjust the base_map to fit the multiple base_glyphs
		base_map.getScene().addGlyph(base_glyph);

		// Add axis if needed. -- need to be able to reset it.
		if (base_axis == null) {
			base_axis = new AsymAxisGlyph();
			base_map.getScene().addGlyph(base_axis);
		}

		int axis_height = base_axis.getHeight();
		base_axis.setCoords(0, base_map_pixel_height, trace_length, axis_height);
		base_map_pixel_height += axis_height;
		base_map.scrollOffset(40); // 40?? -- if this is constant it does not need to be reset
		base_map.setMapOffset(0, base_map_pixel_height);
		// Still need to deal with possiblity that new calls are different length
		// or starting point from already existing
		base_map.setMapRange(0, trace_length);

		setActiveBaseCalls(base_calls); // Make last added active.
	}

	/**
	 * clears all the BaseCalls's added with addBaseCalls.
	 */
	private void removeAllBaseCalls() {
		setActiveBaseCalls(new NullBaseCalls());
		// Not removing based axis because,
		// this is only called when about to replace the base calls with reverse complements.
		// To be correct,
		// we should remove all the base glyphs.
		int iBaseGlyphs = base_glyphs.size();
		for (int i = 0; i < iBaseGlyphs; i++) {
			TraceBaseGlyph baseGlyph = base_glyphs.get(i);
			base_map.getScene().removeGlyph(baseGlyph);
		}
		//base_glyphs.clear();
		base_calls_vector.clear();
	}

	/**
	 * selects which set of base calls is the active one.
	 */
	public void setActiveBaseCalls(BaseCalls bc) {
		if (null == bc || base_calls_vector.contains(bc)) {
			trace.setActiveBaseCalls(bc);
			for (int i = 0; i < base_glyphs.size(); i++) {
				TraceBaseGlyph base_glyph = base_glyphs.get(i);
				if (base_glyph.getBaseCalls() == trace.getActiveBaseCalls()) {
					this.activeBaseCallsGlyph = base_glyph;
				} else {
					base_glyph.clearSelection();
				}
			}
			base_axis.setBaseCalls(trace.getActiveBaseCalls());
		}
	}

	public final BaseCalls getActiveBaseCalls() {
		return trace.getActiveBaseCalls();
	}

	public void scrollRange(double value) {

		Range r = getVisiblePeakRange();
		if (value < r.beg) {
			if (value >= 0) {
				trace_map.scrollRange(value);
				base_map.scrollRange(value);
			}
		} else if (value > r.end) {
			if (value <= trace_length) {
				int delta = (int) value - r.end;
				int off = r.beg + delta;
				trace_map.scrollRange(off);
				base_map.scrollRange(off);
			}
		}
	}

	/**
	 * move indexed base to the center of the screen.
	 *
	 * @param baseNum index of base to be centered.
	 * @return false iff the baseNum could not be centered.
	 */
	public boolean centerAtBase(int baseNum) {
		// if baseNum is < 0, do nothing -- quick fix for negative indexes
		if (baseNum < 0) {
			return false;
		}
		// if baseNum is too big, set to last base in trace
		int base_count = getBaseCount();
		if (baseNum >= base_count) {
			//      baseNum = base_count ; // commenting this out for NeoMultiTracer -- could cause trouble
			return false;
		}

		double current_trace_point = (double) getBaseTracePoint(baseNum);

		Rectangle2D.Double viewbox = trace_map.getView().getCoordBox();
		double new_scroll_pos = current_trace_point - viewbox.width / 2;

		trace_map.scrollRange((int) new_scroll_pos);
		base_map.scrollRange((int) new_scroll_pos);
		return true;
	}

	/**
	 * move indexed base to a given point in pixel space.
	 * <p> The important equations are:<pre>
	 *  new_scroll_pos = old_scroll_pos + current_trace_point - new_trace_point
	 *  new_trace_point = old_scroll_pos + fraction_of_screen * trace_width
	 * </pre>
	 *  so <code>new_scroll_pos = current_trace_point - fraction_of_screen * trace_width</code>
	 *     -- for center at Base, <code>fraction_of_screen = 1/2</code>.
	 *
	 * @param baseNum point to the base to reposition.
	 * @param view_point is position to put it.
	 * @return true iff the repositioning succeeded.
	 */
	public boolean positionBase(int baseNum, int view_point) {
		// if baseNum is < 0, do nothing -- quick fix for negative indexes
		if (baseNum < 0) {
			return false;
		}
		// if baseNum is too big, set to last base in trace
		int base_count = getBaseCount();
		if (baseNum >= base_count) {
			//      baseNum = base_count ;
			return false;
		}

		double current_trace_point = (double) getBaseTracePoint(baseNum);

		// determine view_point as fraction of view width
		int view_width = trace_map.getView().getPixelBox().width;
		double view_fraction;
		if (view_point == -1) { // special code for right side
			view_fraction = 1;
		} else {
			view_fraction = (double) view_point / (double) view_width;
		}

		Rectangle2D.Double viewbox = trace_map.getView().getCoordBox();
		double new_scroll_pos = current_trace_point - (viewbox.width * view_fraction);

		trace_map.scrollRange((int) new_scroll_pos);
		base_map.scrollRange((int) new_scroll_pos);
		//System.out.println( "moving trace from " + current_trace_point +",  "+ new_scroll_pos );
		return true;
	}

	public int getBaseTracePoint(int base_index) {
		return getBaseCall(base_index).getTracePoint();
	}

	public int getBaseViewPoint(int base_index) {
		int trace_point = getBaseCall(base_index).getTracePoint();
		Point p = new Point();
		trace_map.getView().transformToPixels(new Point2D.Double(trace_point, 0), p);
		return p.x;
	}

	public int getLocation(NeoAbstractWidget widg) {
		if (widg == trace_map) {
			return TRACES;
		} else if (widg == base_map) {
			return BASES;
		}
		throw new IllegalArgumentException("unknown widget");
	}

	public NeoAbstractWidget getWidget(int location) {
		if (location == TRACES) {
			return trace_map;
		} else if (location == BASES) {
			return base_map;
		}
		throw new IllegalArgumentException(
				"only widgets to get are TRACES, or BASES.");
	}

	/**
	 * selects a region in both the trace area and the bases.
	 *
	 * @param start the first border, in trace space
	 * @param end   the second border, in trace space
	 */
	public void highlight(int start, int end) {
		highlightTrace(start, end);
		highlightBases(start, end);
	}

	/**
	 * highlights a region in the trace area.
	 *
	 * @param start the first border, in trace space
	 * @param end   the second border, in trace space
	 */
	public void highlightTrace(int start, int end) {
		trace_map.select(trace_glyph, start, end);
		trace_map.updateWidget();
	}

	/**
	 * highlights a contiguous set of bases.
	 *
	 * @param start the first border, in trace coords
	 * @param end   the second border, in trace coords
	 */
	public void highlightBases(int start, int end) {
		if (null != this.activeBaseCallsGlyph) {
			base_map.select(this.activeBaseCallsGlyph, start, end);
		} else {
			for (int i = 0; i < base_glyphs.size(); i++) {
				TraceBaseGlyph base_glyph = base_glyphs.get(i);
				base_map.select(base_glyph, start, end);
			}
		}
		base_map.updateWidget();
	}

	protected void traceMapStartHighlight(NeoMouseEvent evt) {
		if (null != getActiveBaseCalls()) {
			int base = getActiveBaseCalls().getBaseIndexAtTracePoint((int) evt.getCoordX());
			sel_range.setPoint(base);
			sel_range.notifyObservers();
		}
	}

	protected void traceMapExtendHighlight(NeoMouseEvent evt) {
		if (null != getActiveBaseCalls()) {
			int base = getActiveBaseCalls().getBaseIndexAtTracePoint((int) evt.getCoordX());
			sel_range.update(base);
			sel_range.notifyObservers();
		}
	}

	@Override
	public void heardMouseEvent(MouseEvent evt) {
		if (evt instanceof NeoMouseEvent) {
			NeoMouseEvent nevt = (NeoMouseEvent) evt;
			int id = nevt.getID();
			Object source = nevt.getSource();
			if (source == base_map) {
				if ((id == NeoMouseEvent.MOUSE_PRESSED && sel_behavior == ON_MOUSE_DOWN)
						|| (id == NeoMouseEvent.MOUSE_RELEASED && sel_behavior == ON_MOUSE_UP)) {
					// Switch which set of base calls is the active one.
					// Note this needs to be done before the selection stuff below.
					for (GlyphI glyph : nevt.getItems()) {
						if (glyph instanceof TraceBaseGlyph) {
							BaseCalls bc = ((TraceBaseGlyph) glyph).getBaseCalls();
							if (bc != getActiveBaseCalls()) {
								this.sel_range.clear();
								this.sel_range.notifyObservers();
								setActiveBaseCalls(bc);
							}
						}
					}
				}
			}
			if (source == trace_map || source == base_map) {
				if ((id == NeoMouseEvent.MOUSE_PRESSED && sel_behavior == ON_MOUSE_DOWN)
						|| (id == NeoMouseEvent.MOUSE_RELEASED && sel_behavior == ON_MOUSE_UP)) {
					if (nevt.isShiftDown() && (!sel_range.isEmpty())) {
						traceMapExtendHighlight(nevt);
					} else {
						traceMapStartHighlight(nevt);
					}
				} else if (id == NeoMouseEvent.MOUSE_DRAGGED
						&& sel_behavior == ON_MOUSE_DOWN) {
					traceMapExtendHighlight(nevt);
				}
			}
			if (source == base_map) {
				// Let everybody else know that a base has been selected.
				if (((id == NeoMouseEvent.MOUSE_PRESSED || id == NeoMouseEvent.MOUSE_DRAGGED) && sel_behavior == ON_MOUSE_DOWN)
						|| (id == NeoMouseEvent.MOUSE_RELEASED && sel_behavior == ON_MOUSE_UP)) {
					BaseCalls bc = getActiveBaseCalls();
					int index = bc.getBaseIndexAtTracePoint((int) nevt.getCoordX());
					sendBaseSelectedEvent(index);
				}
			}
		}
		super.heardMouseEvent(evt);
	}

	public int getSel_range_start() {
		return this.sel_range.getStart();
	}

	public int getSel_range_end() {
		return this.sel_range.getEnd();
	}

	/** Methods for dealing with selection **/
	/**
	 * gets a base call from the active set of base calls.
	 * For backward compatibility,
	 * if there are no active base calls,
	 * get it from the trace.
	 *
	 * @param theBaseIndex indicates which one.
	 * @return the indicated base call.
	 */
	private BaseCall getBaseCall(int theBaseIndex) {
		BaseCalls bc = getActiveBaseCalls();
		if (null == bc) { // for backward compatibility
			return this.trace.getActiveBaseCalls().getBaseCall(theBaseIndex);
		}
		return bc.getBaseCall(theBaseIndex);
	}

	private int getBaseCount() {
		BaseCalls bc = getActiveBaseCalls();
		if (null == bc) { // for backward compatibility
			return this.trace.getBaseCount();
		}
		return bc.getBaseCount();
	}
	private static final int BEFORE = -1;
	private static final int AT = 0;
	private static final int AFTER = 1;

	/**
	 * converts from "residue space" to "sample space".
	 *
	 * <p> This form assumes we are interested in the current set of base calls.
	 *
	 * @param theBaseIndex indicates which base.
	 * @param where indicates which sample point related to the base is wanted.
	 * It can be BEFORE, AFTER, or AT.
	 */
	private int samplePoint(int theBaseIndex, int where) {
		BaseCall bc = getBaseCall(theBaseIndex);
		int peak_at = bc.getTracePoint();
		switch (where) {
			case BEFORE:
				if (0 < theBaseIndex) {
					BaseCall bc2 = getBaseCall(theBaseIndex - 1);
					int peak_2 = bc2.getTracePoint();
					peak_at = (peak_at + peak_2) / 2;
				} else {
					peak_at = 0;
				}
				break;
			case AT:
				break;
			case AFTER:
				if (theBaseIndex + 1 < getBaseCount()) {
					BaseCall bc2 = getBaseCall(theBaseIndex + 1);
					int peak_2 = bc2.getTracePoint();
					peak_at = (peak_at + peak_2) / 2;
					peak_at--; // so that we do not overlap with next base.
				} else {
					peak_at += PEAK_WIDTH / 2;
				}
				break;
		}
		return peak_at;
	}

	public Range baseRange2TraceRange(BaseCalls bc, int basenum_start, int basenum_end) {
		BaseCall base_start = bc.getBaseCall(basenum_start);
		BaseCall base_end = bc.getBaseCall(basenum_end);

		int peak_start = base_start.getTracePoint();
		int peak_end = base_end.getTracePoint();

		int startAt, endAt;

		if (0 < basenum_start) {
			int pre_start_peak = bc.getBaseCall(basenum_start - 1).getTracePoint();
			startAt = (peak_start + pre_start_peak) / 2;
		} else {
			startAt = 0;
		}

		if (basenum_end + 1 < getBaseCount()) {
			int post_end_peak = bc.getBaseCall(basenum_end + 1).getTracePoint();
			endAt = (peak_end + post_end_peak) / 2;
			endAt--;
		} else {
			endAt = peak_end + PEAK_WIDTH / 2;
		}
		return new Range(startAt, endAt);
	}

	/**
	 * Selects a region in the widget.
	 *
	 * @param basenum_start the first base selected
	 * @param basenum_end   the last base selected
	 */
	public void selectResidues(int basenum_start, int basenum_end) {
		BaseCalls bc = getActiveBaseCalls();
		if (null == bc) {
			return; // There are no residues to select.
		}
		Range trace_range = baseRange2TraceRange(bc, basenum_start, basenum_end);
		highlight(trace_range.beg, trace_range.end);
	}

	public void setSelectionEvent(int theOption) {
		switch (theOption) {
			case ON_MOUSE_DOWN:
			case ON_MOUSE_UP:
			case NO_SELECTION:
				sel_behavior = theOption;
				break;
			default:
				throw new IllegalArgumentException(
						"SelectionEvent can only be NO_SELECTION, ON_MOUSE_DOWN, or "
						+ "ON_MOUSE_UP.");
		}
	}

	public int getSelectionEvent() {
		return this.sel_behavior;
	}

	/**
	 * Sets whether or not the trace and bases for a given dye are visible.
	 *
	 * @param traceID must be one of A, C, G, T, or N.
	 * @param visible  indicates visibility of <code>theDye</code>
	 */
	public void setVisibility(int traceID, boolean visible) {
		switch (traceID) {
			case TraceGlyph.A:
			case TraceGlyph.C:
			case TraceGlyph.G:
			case TraceGlyph.T:
				setTraceVisibility(traceID, visible);
				setBaseVisibility(traceID, visible);
				break;
			default:
				throw new IllegalArgumentException(
						"traceID must be one of A, C, G, or T");
		}
	}

	/**
	 * @return true if either [traceID] bases or trace are visible.
	 */
	public boolean getVisibility(int traceID) {
		return (getTraceVisibility(traceID) || getBaseVisibility(traceID));
	}

	public boolean getTraceVisibility(int traceID) {
		return trace_glyph.getVisibility(traceID);
	}

	public void setTraceVisibility(int traceID, boolean visible) {
		if (!(trace_glyph.getVisibility(traceID) == visible)) {
			trace_glyph.setVisibility(traceID, visible);
			trace_map.getScene().maxDamage();
		}
	}

	public final boolean getBaseVisibility(int baseID) {
		return base_glyphs!=null && !base_glyphs.isEmpty() && base_glyphs.get(0).getVisibility(baseID);
	}

	public void setBaseVisibility(int baseID, boolean visible) {
		if (!(getBaseVisibility(baseID) == visible)) {
			for (int i = 0; i < base_glyphs.size(); i++) {
				base_glyphs.get(i).setVisibility(baseID, visible);
			}
			base_map.getScene().maxDamage();
		}
	}

	/**
	 * Gets the color used for the background
	 * in the trimmed regions of the trace.
	 */
	public Color getTrimColor() {
		return trim_color;
	}

	/**
	 * Specify the background color for the trimmed portion of the trace.
	 *
	 * @param  col the Color to be used for the background in the trimmed
	 *  portion of the trace.
	 */
	public void setTrimColor(Color col) {
		if (col != this.trim_color) {
			this.trim_color = col;
			if (this.left_trim_glyph != null) {
				this.left_trim_glyph.setBackgroundColor(this.trim_color);
			}
			if (this.right_trim_glyph != null) {
				this.right_trim_glyph.setBackgroundColor(this.trim_color);
			}
		}
	}

	/**
	 * Highlights the left (5') end of the trace.
	 *
	 * @param left_trim_end the integer (in trace coordinates, not bases)
	 * specifying where to stop trimming the left end of the trace
	 */
	public void setLeftTrim(int left_trim_end) {
		Rectangle2D.Double coordbox = trace_glyph.getCoordBox();
		if (left_trim_glyph != null) {
			trace_glyph.removeChild(left_trim_glyph);
		}
		left_trim_glyph = new FillRectGlyph();
		left_trim_glyph.setBackgroundColor(trim_color);
		left_trim_glyph.setCoords(coordbox.x, coordbox.y,
				left_trim_end, coordbox.height);
		trace_glyph.addChild(left_trim_glyph);
	}

	/**
	 * Highlights the right (3') end of the trace.
	 *
	 * @param right_trim_start the integer (in trace coordinates, not bases)
	 * specifying where to start trimming the right end of the trace
	 */
	public void setRightTrim(int right_trim_start) {
		Rectangle2D.Double coordbox = trace_glyph.getCoordBox();
		if (right_trim_glyph != null) {
			trace_glyph.removeChild(right_trim_glyph);
		}
		right_trim_glyph = new FillRectGlyph();
		right_trim_glyph.setBackgroundColor(trim_color);
		right_trim_glyph.setCoords(right_trim_start, coordbox.y,
				coordbox.x + coordbox.width - right_trim_start,
				coordbox.height);
		trace_glyph.addChild(right_trim_glyph);
	}

	/**
	 * Sets the orientation of the trace.
	 */
	public void setDirection(int orientation) {
		switch (orientation) {
			case FORWARD:
				if (this.forward) {
					return;
				}
				this.forward = true;
				break;
			case REVERSE_COMPLEMENT:
				if (!this.forward) {
					return;
				}
				this.forward = false;
				break;
			default:
				throw new IllegalArgumentException(
						"orientation must be either FORWARD or REVERSE_COMPLEMENT.");
		}

		if (null == trace) {
			return;
		}

		trace = trace.reverseComplement();
		setChromatogram(trace);
		setTraceVisible();
		setBaseVisible();

		// want to move to "same" location as in previous orientation,
		// but since rev-comped from previous, need to calculate this
		Rectangle2D.Double tbox = trace_map.getScene().getCoordBox();
		Rectangle2D.Double vbox = trace_map.getView().getCoordBox();
		redrawRevComp(tbox, vbox);

		// This seems to be needed to avoid a gap being put in between the traces
		// and the horizontal line separating the traces from the base calls. elb - 1999-12-07
		doLayout();

	}

	private void setBaseVisible() {
		// Need to reverse each set of base calls.
		boolean aViz = getBaseVisibility(TraceGlyph.A);
		boolean cViz = getBaseVisibility(TraceGlyph.C);
		boolean gViz = getBaseVisibility(TraceGlyph.G);
		boolean tViz = getBaseVisibility(TraceGlyph.T);
		List<BaseCalls> newBaseCalls = new ArrayList<BaseCalls>();
		for (BaseCalls bc : base_calls_vector) {
			//			newBaseCalls.add(bc.reverseComplement());
			trace.setActiveBaseCalls(bc.reverseComplement());
		}
		// Remove the old.
		removeAllBaseCalls();
		// Add the new.
		this.addBaseCalls(trace.getActiveBaseCalls(), 0);
		// Switch the visibility of complimentary bases.
		setBaseVisibility(TraceGlyph.A, tViz);
		setBaseVisibility(TraceGlyph.T, aViz);
		setBaseVisibility(TraceGlyph.C, gViz);
		setBaseVisibility(TraceGlyph.G, cViz);
	}

	private void setTraceVisible() {
		// Switch the visibility of complimentary traces.
		setTraceVisibility(TraceGlyph.A, getTraceVisibility(TraceGlyph.A) ^ getTraceVisibility(TraceGlyph.T));
		setTraceVisibility(TraceGlyph.T, getTraceVisibility(TraceGlyph.A) ^ getTraceVisibility(TraceGlyph.T));
		setTraceVisibility(TraceGlyph.A, getTraceVisibility(TraceGlyph.A) ^ getTraceVisibility(TraceGlyph.T));
		setTraceVisibility(TraceGlyph.C, getTraceVisibility(TraceGlyph.C) ^ getTraceVisibility(TraceGlyph.G));
		setTraceVisibility(TraceGlyph.G, getTraceVisibility(TraceGlyph.C) ^ getTraceVisibility(TraceGlyph.G));
		setTraceVisibility(TraceGlyph.C, getTraceVisibility(TraceGlyph.C) ^ getTraceVisibility(TraceGlyph.G));
	}

	/**
	 * Returns the orientation of the trace
	 * relative to how it was originally loaded into NeoTracer.
	 *
	 * @see #setDirection
	 */
	public int getDirection() {
		if (!this.forward) {
			return REVERSE_COMPLEMENT;
		}
		return FORWARD;
	}

	public void setBackground(int id, Color col) {
		switch (id) {
			case TRACES:
				trace_map.setMapColor(col);
				break;
			case BASES:
				base_map.setMapColor(col);
				break;
			default:
				throw new IllegalArgumentException("NeoTracer.setBackground(id, "
						+ "color) currently only supports ids of TRACES or BASES");
		}
	}

	public Color getBackground(int id) {
		switch (id) {
			case TRACES:
				return trace_map.getMapColor();
			case BASES:
				return base_map.getMapColor();
		}
		throw new IllegalArgumentException("NeoTracer.getBackground(id) "
				+ "currently only supports ids of TRACES or BASES");
	}

	public void update(Observable theObserved, Object theArgument) {
		if (theObserved instanceof Selection) {
			update((Selection) theObserved);
		}
	}

	private void update(Selection theSelection) {
		if (theSelection == this.traceSelection) { // selecting trace points:
			// I dont think this is ever being called 5/12/99
			// System.err.println( "NeoTracer.update: trace" );
			highlight(theSelection.getStart(), theSelection.getEnd());
		} else { // selecting residues
			selectResidues(theSelection.getStart(), theSelection.getEnd());
		}
		updateWidget();
	}
	private int leftTrim, rightTrim;

	/**
	 * Highlights the portion of the trace
	 * corresponding to the first n bases called.
	 * This can be used to show that some base calls should be trimmed
	 * from the 5' end due to low quality.
	 *
	 * @param theBasesTrimmed how many
	 * @see #setLeftTrim
	 */
	public void setBasesTrimmedLeft(int theBasesTrimmed) {

		this.leftTrim = theBasesTrimmed;
		if (this.leftTrim < 1) {
			if (null != left_trim_glyph) {
				trace_glyph.removeChild(left_trim_glyph);
			}
			return;
		}

		int lastTrimmedBase = this.leftTrim - 1;
		int endAt = samplePoint(lastTrimmedBase, AFTER);

		endAt++; // This seems to be needed to match selection. Why?

		Rectangle2D.Double coordbox = trace_glyph.getCoordBox();
		if (left_trim_glyph != null) {
			trace_glyph.removeChild(left_trim_glyph);
		}
		left_trim_glyph = new FillRectGlyph();
		left_trim_glyph.setBackgroundColor(trim_color);
		left_trim_glyph.setCoords(coordbox.x, coordbox.y,
				endAt, coordbox.height);
		trace_glyph.addChild(left_trim_glyph);
		trace_map.toBack(left_trim_glyph);
	}

	public int getBasesTrimmedLeft() {
		return this.leftTrim;
	}

	/**
	 * Highlights the portion of the trace
	 * corresponding to the last n bases called.
	 * This can be used to show that some base calls should be trimmed
	 * from the 3' end due to low quality.
	 *
	 * @param theBasesTrimmed how many
	 * @see #setRightTrim
	 */
	public void setBasesTrimmedRight(int theBasesTrimmed) {

		this.rightTrim = theBasesTrimmed;
		if (this.rightTrim < 1) {
			if (null != right_trim_glyph) {
				trace_glyph.removeChild(right_trim_glyph);
			}
			return;
		}

		int firstTrimmedBase = getBaseCount() - this.rightTrim;
		int startAt = samplePoint(firstTrimmedBase, BEFORE);

		Rectangle2D.Double coordbox = trace_glyph.getCoordBox();
		if (right_trim_glyph != null) {
			trace_glyph.removeChild(right_trim_glyph);
		}
		right_trim_glyph = new FillRectGlyph();
		right_trim_glyph.setBackgroundColor(trim_color);
		right_trim_glyph.setCoords(
				startAt,
				coordbox.y,
				coordbox.x + coordbox.width - startAt,
				coordbox.height);
		trace_glyph.addChild(right_trim_glyph);
		trace_map.toBack(right_trim_glyph);
	}

	public int getBasesTrimmedRight() {
		return this.rightTrim;
	}

	public void setTraceColors(Color[] colors) {
		trace_glyph.setTraceColors(colors);
		for (int i = 0; i < base_glyphs.size(); i++) {
			base_glyphs.get(i).setBaseColors(colors);
		}
	}

	public void viewBoxChanged(NeoViewBoxChangeEvent evt) {
		if (evt.getSource() == trace_map) {
			if (range_listeners.size() > 0) {
				Range visRange = getVisibleBaseRange();
				NeoRangeEvent nevt = new NeoRangeEvent(this,
						visRange.beg, visRange.end);

				for (NeoRangeListener l : range_listeners) {
					l.rangeChanged(nevt);
				}
			}
		}
	}

	public void addRangeListener(NeoRangeListener l) {
		range_listeners.add(l);
	}

	public void removeRangeListener(NeoRangeListener l) {
		range_listeners.remove(l);
	}

	public void setRangeScroller(JScrollBar scroll) {
		trace_map.setRangeScroller(scroll);
		base_map.setRangeScroller(scroll);
	}

	public void setRangeZoomer(Adjustable scroll) {
		trace_map.setRangeZoomer(scroll);
		base_map.setRangeZoomer(scroll);
	}

	public void setOffsetZoomer(Adjustable scroll) {
		trace_map.setOffsetZoomer(scroll);
	}

	public void addBaseSelectListener(NeoBaseSelectListener l) {
		base_listeners.add(l);
	}

	public void removeBaseSelectListener(NeoBaseSelectListener l) {
		base_listeners.remove(l);
	}

	private void sendBaseSelectedEvent(int base_index) {
		NeoBaseSelectEvent event = new NeoBaseSelectEvent((Object) this, base_index);
		for (NeoBaseSelectListener nbsl : base_listeners) {
			nbsl.baseSelected(event);
		}
	}

	public int mapToAxisPos(int base_index) {
		return base_index + base_axis.getStartPos(); // needs to be smarter
	}

	public int mapFromAxisPos(int axis_pos) {
		return axis_pos - base_axis.getStartPos(); // needs to be smarter
	}

	/**
	 * Align one of the trace's set of base calls with an external consensus sequence.
	 * Only one set of base calls can be aligned with the consensus at any time.
	 */
	public void setConsensus(Sequence cons_bases, Mapping cons_aligner, Mapping active_aligner,
			BaseCalls active_base_calls) {
		// map the consensus to the active base calls
		if (active_base_calls != null) {
			setActiveBaseCalls(active_base_calls);
		} else {
			active_base_calls = getActiveBaseCalls();
		}
		consensus = new BaseCalls();
		List<Character> inserts = new ArrayList<Character>();
		int last_pos = 0;

		int calls_index = active_aligner.getMappedStart();

		// find the cons index that corresponds to the first calls_index;
		int cons_start, cons_index;

		// Search for good cons_index starting point.
		// We can't always make round trip from space transformations.
		while ((cons_index = cons_aligner.mapToMapped(active_aligner.mapToReference(calls_index)))
				== Integer.MIN_VALUE) {
			calls_index++;
		}
		// Save the starting consensus position for use in starting the axis numbers at the right first number.
		cons_start = cons_index;


		// Create the new consensus BaseCalls,
		// move all the bases in from cons_bases, and
		// use the position data from the active_base_calls.
		// Logic used to account for gaps.
		// Gaps in consensus are resolved as extra 'N's.
		// Gaps in the base_calls means the extra consesus calls need to be squeezed ( inserted ) in.

		// TODO -- need to build aligner as I go, from new cons_BaseCalls, back to the reference space
		//       - for numbering + selection information???
		while (calls_index <= active_aligner.getMappedEnd()) {
			int cons_ref = cons_aligner.mapToReference(cons_index);
			int calls_ref = active_aligner.mapToReference(calls_index);

			int position;
			char new_base;
			int conf = 100; // need to get confidence from consensus -- todo
			BaseConfidence new_base_obj;

			// consesus + calls aligned
			if (cons_ref == calls_ref) {
				new_base = cons_bases.getResidue(cons_index);
				position = getBaseCall(calls_index).getTracePoint();
				calls_index++;
				cons_index++;

				// insert the cashed inserts
				int iSize = inserts.size();
				int span = position - last_pos;// amount of space we have to stuff in inserts
				double spacing = span / (iSize + 1); // space inbetween edges
				for (int i = 0; i < iSize; i++) {
					char insert_base = inserts.get(i).charValue();
					int insert_pos = last_pos + (int) (spacing * (i + 1));
					new_base_obj = new BaseConfidence(insert_base, conf, insert_pos);
					consensus.addBase(new_base_obj);
				}
				inserts.clear(); // clear insert bases

				// insert the latest base
				new_base_obj = new BaseConfidence(new_base, conf, position);
				consensus.addBase(new_base_obj);
				last_pos = position;
			} // show gap in consensus
			else if (cons_ref > calls_ref) {
				new_base = 'N';
				position = getBaseCall(calls_index).getTracePoint();
				calls_index++;

				new_base_obj = new BaseConfidence(new_base, conf, position);
				consensus.addBase(new_base_obj);
				last_pos = position;
			} // fit extra consensus bases in as best as possible
			else { // cons_ref < calls_ref
				char insert_base = cons_bases.getResidue(cons_index);
				inserts.add(new Character(insert_base));
				cons_index++;
			}
		} // end while loop for base calls

		this.addBaseCalls(consensus, 0);
		// sync base numbers with consensus
		base_axis.setBaseCalls(consensus);
		base_axis.setStartPos(cons_start);
	}

	/**
	 * Make the maps big enough to allow scrolling off the side.
	 */
	public void padCoordBox() {
		// 1000 is arbitrary
		trace_map.setMapRange(-1000, trace_length + 2000);
		base_map.setMapRange(-1000, trace_length + 2000);
	}

	/**
	 * Make an <em>external</em>
	 * adjustable responsible for scrolling.
	 * The caller is responsible for adding the Adjustable to the user interface.
	 *
	 * @param axisid Either NeoAbstractWidget.X or NeoAbstractWidget.Y
	 */
	@Override
	public void setScroller(int axisid, JScrollBar adj) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid)) {
			throw new IllegalArgumentException(
					"Can set zoomer for X (" + NeoAbstractWidget.X
					+ ") or Y (" + NeoAbstractWidget.Y + ") axis. "
					+ "Not for " + axisid);
		}
		trace_map.setScroller(axisid, adj);
		if (axisid == NeoAbstractWidget.X) {
			base_map.setScroller(axisid, adj);
		}
	}

	/**
	 * Make an <em>external</em>
	 * adjustable responsible for zooming.
	 * The caller is responsible for adding the Adjustable to the user interface.
	 * Overriding NeoContainerWidget.setZoomer() to prevent zooming base_map
	 * in Y direction.
	 * @param axisid Either NeoAbstractWidget.X or NeoAbstractWidget.Y
	 */
	@Override
	public void setZoomer(int axisid, Adjustable adj) {
		if (!(NeoAbstractWidget.X == axisid || NeoAbstractWidget.Y == axisid)) {
			throw new IllegalArgumentException(
					"Can set zoomer for X (" + NeoAbstractWidget.X
					+ ") or Y (" + NeoAbstractWidget.Y + ") axis. "
					+ "Not for " + axisid);
		}
		trace_map.setZoomer(axisid, adj);
		if (axisid == NeoAbstractWidget.X) {
			base_map.setZoomer(axisid, adj);
		}
	}

	/**
	 * Get the <em>internal</em> Adjustable responsible for scrolling.
	 */
	public Adjustable getScroller() {
		return hscroll;
	}

	/**
	 * Make an <em>internal</em>
	 * adjustable responsible for horizontal scrolling.
	 * The given Adjustable will be added to this NeoTracer.
	 * The caller should not add it elsewhere in the user interface.
	 * If the given Adjustable isn't an instance of Component,
	 * the call will be ignored.
	 * @param scroller
	 */
	public void setScroller(JScrollBar scroller) {

		if (!(scroller instanceof Component)
				|| (scroller == null)
				|| !hscroll_show) {
			return;
		}

		remove((Component) hscroll);
		hscroll = scroller;
		add((Component) hscroll);

		setRangeScroller(hscroll);
	}

	/**
	 * Get the <em>internal</em> Adjustable responsible for horizontal zooming.
	 */
	public Adjustable getHorizontalZoomer() {
		return hzoom;
	}

	/**
	 * Make an <em>internal</em>
	 * Adjustable responsible for horizontal zooming.
	 * The given Adjustable will be added to this NeoTracer.
	 * The caller should not add it elsewhere in the user interface.
	 * If the given Adjustable is not an instance of Component,
	 * the call will be ignored.
	 * @see #setZoomer(int,Adjustable)
	 */
	public void setHorizontalZoomer(Adjustable zoomer) {
		if (!(zoomer instanceof Component) || (zoomer == null) || !hzoom_show) {
			return;
		}
		remove((Component) hzoom);
		hzoom = zoomer;
		add((Component) hzoom);
		setRangeZoomer(hzoom);
	}

	/**
	 * Get the <em>internal</em> Adjustable responsible for vertical zooming.
	 */
	public Adjustable getVerticalZoomer() {
		return vzoom;
	}

	/**
	 * Make an <em>internal</em>
	 * Adjustable responsible for vertical zooming.
	 * The Adjustable will be added to this NeoTracer.
	 * The caller should not add it elsewhere in the user interface.
	 * If the given Adjustable is not an instance of Component,
	 * the call will be ignored.
	 * @see #setZoomer(int,Adjustable)
	 */
	public void setVerticalZoomer(Adjustable zoomer) {
		if (!(zoomer instanceof Component) || (zoomer == null) || !vzoom_show) {
			return;
		}
		remove((Component) vzoom);
		vzoom = zoomer;
		add((Component) vzoom);
		setOffsetZoomer(vzoom);
	}
}
