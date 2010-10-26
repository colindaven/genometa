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
import java.util.*;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.datamodel.ReadConfidence;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.util.Selection;
import com.affymetrix.genoviz.widget.neoqualler.QualityBars;
import com.affymetrix.genoviz.widget.neoqualler.QualityBases;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.JScrollBar;

/**
 * NeoQualler is the <b>implementation</b> of NeoQuallerI.
 * Documentation for all interface methods can be found in the
 * documentation for NeoQuallerI.<p>
 *
 * This javadoc explains the implementation
 * specific features of this widget concerning event handling and the
 * java AWT.  In paticular, all genoviz implementations of widget
 * interfaces are subclassed from <code>Container</code> and use the
 * JDK 1.1 event handling model.
 *
 * <p> NeoAssembler extends <code>java.awt.Container</code>,
 * and thus, inherits all of the AWT methods of
 * <code>java.awt.Container</code>, and <code>Component</code>.
 * For example, a typical application might use the following as
 * part of initialization:
 *
 * <pre>
 *   qual = new NeoQualler();
 *
 *   qual.setBackground(new Color(180, 250, 250));
 *   qual.resize(500, 250);
 * </pre>
 *
 * @version $Id: NeoQualler.java 5210 2010-02-09 20:17:23Z sgblanch $
 */
public class NeoQualler extends NeoContainerWidget
	implements Observer, NeoViewBoxListener  {
	/**
	 * component identifier constant for the quality score histogram display.
	 * @see #getItems
	 */
	public static final int BARS = 8000;

	/**
	 * component identifier constant for the sequence base (letter) display.
	 * @see #getItems
	 */
	public static final int BASES = BARS + 1;

	/**
	 * component identifier constant for the panning axis scroller.
	 * @see #getItems
	 */
	public static final int AXIS_SCROLLER = BARS + 1;

	/**
	 * component identifier constant for the zooming adjustable.
	 * @see #getItems
	 */
	public static final int AXIS_ZOOMER = BARS + 2;

	/**
	 * component identifier constant for the vertical scaling adjustable.
	 * @see #getItems
	 */
	public static final int OFFSET_ZOOMER = BARS + 3;

	/**
	 * component identifier constant for other components not part
	 * of the interface description.
	 * @see #getItems
	 * @deprecated NeoQualler.UNKNOWN used to hide NeoConstants.UNKNOWN
	 */
	@Deprecated
	public static final int UNKNOWN = BARS + 4;

	private static final Color default_bar_background = Color.black;
	private static final Color default_base_background = Color.black;
	private static final Color default_panel_background = Color.lightGray;


	protected static final int base_map_pixel_height = QualityBases.baseGlyphHeight;

	protected NeoMap bar_map;
	protected NeoMap base_map;

	protected JScrollBar hscroll;
	protected JScrollBar hzoom;

	protected JScrollBar vzoom;

	// locations for scrollbars, consensus, and labels
	protected int hscroll_loc = PLACEMENT_BOTTOM;
	protected int vzoom_loc = PLACEMENT_RIGHT;
	protected int hzoom_loc = PLACEMENT_TOP;

	protected int bar_loc = PLACEMENT_TOP;
	protected int base_loc = PLACEMENT_BOTTOM;

	protected ReadConfidence read_conf;
	protected int read_length;
	protected int read_height_max;

	protected QualityBars bar_glyph;
	protected QualityBases base_glyph;
	protected GlyphI line_glyph;

	protected Color background = Color.black;

	protected int sel_behavior = ON_MOUSE_DOWN;
	protected Selection sel_range;

	protected Range range;
	protected Set<NeoRangeListener> range_listeners = new CopyOnWriteArraySet<NeoRangeListener>();

	public NeoQualler() {
		super();

		bar_map = new NeoMap(false, false);
		base_map = new NeoMap(false, false);

		hscroll = new JScrollBar(JScrollBar.HORIZONTAL);
		hzoom = new JScrollBar(JScrollBar.VERTICAL);

		vzoom = new JScrollBar(JScrollBar.VERTICAL);

		setBackground(default_panel_background);
		bar_map.setMapColor(default_bar_background);
		base_map.setMapColor(default_base_background);

		this.setLayout(null);
		add(hscroll);
		add(hzoom);
		add(vzoom);
		add(bar_map);
		add(base_map);

		bar_map.setRangeScroller(hscroll);
		bar_map.setRangeZoomer(hzoom);
		bar_map.setOffsetZoomer(vzoom);
		bar_map.setScaleConstraint(NeoMap.Y, NeoMap.INTEGRAL_COORDS);
		bar_map.setMapRange(0, 100);
		bar_map.setMapOffset(0, 100);
		bar_map.setReshapeBehavior(bar_map.X, NeoConstants.NONE);
		bar_map.setReshapeBehavior(bar_map.Y, FITWIDGET);
		bar_map.setZoomBehavior(bar_map.Y, bar_map.CONSTRAIN_END);
		bar_map.setZoomBehavior(bar_map.X, bar_map.CONSTRAIN_MIDDLE);

		base_map.setRangeScroller(hscroll);
		base_map.setRangeZoomer(hzoom);

		base_map.setMapRange(0, 100);
		base_map.setMapOffset(0, 100);
		base_map.setReshapeBehavior(base_map.X, NeoConstants.NONE);
		base_map.setReshapeBehavior(base_map.Y, base_map.FITWIDGET);

		bar_map.setMaxZoom(bar_map.X, 12);
		base_map.setMaxZoom(base_map.X, 12);

		base_map.zoomRange(2);
		bar_map.zoomRange(2);

		bar_map.addMouseListener(this);
		bar_map.addMouseMotionListener(this);
		bar_map.addKeyListener(this);

		// for transforming viewbox changes to NeoRangeEvents
		bar_map.addViewBoxListener(this);

		base_map.addMouseListener(this);
		base_map.addMouseMotionListener(this);
		base_map.addKeyListener(this);

		addWidget(bar_map);
		addWidget(base_map);

		updateWidget();

		stretchToFit(false,false);
		setRubberBandBehavior(false);

		setSelection(new Selection());

	}

	/**
	 * a constructor for cloning.
	 */
	public NeoQualler(NeoQualler original) {
		this(); // First create a new one.
		setRoot(original); // Then copy or point to everything in the original.
	}

	protected void setRoot(NeoQualler root) {

		// We need to set up a derived view from  each individual NeoMap
		// within this widget based on corresponding NeoMap within the root.
		NeoMap root_bar_map = (NeoMap)root.getWidget(NeoQualler.BARS);
		NeoMap root_base_map = (NeoMap)root.getWidget(NeoQualler.BASES);
		bar_map.setRoot(root_bar_map);
		base_map.setRoot(root_base_map);

		// Set various fields that need to be shared
		// between this widget and the root.

		// Object fields are being set here.
		// Since these fields are objects
		//   once they are assigned, unless reassigned they will continue to
		//   point to same object as corresponding fields in root.

		read_conf = root.read_conf;

		bar_glyph = root.bar_glyph;
		base_glyph = root.base_glyph;
		line_glyph = root.line_glyph;
		range = root.range;

		setBasesTrimmedLeft(root.getBasesTrimmedLeft());
		setBasesTrimmedRight(root.getBasesTrimmedRight());

		setSelection(root.sel_range); // monitor the same selection as the root.

		// more object fields to copy, these are inherited from NeoContainerWidget
		glyph_hash = root.glyph_hash;
		model_hash = root.model_hash;
		selected = root.getSelected();

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

		read_length = root.read_length;
		read_height_max = root.read_height_max;

		// locations for scrollbars, consensus, and labels
		hscroll_loc = root.hscroll_loc;
		vzoom_loc = root.vzoom_loc;
		hzoom_loc = root.hzoom_loc;

		bar_loc = root.bar_loc;
		base_loc = root.base_loc;
	}

	public void setSelection(Selection theSelection) {
		if (null != sel_range) {
			sel_range.deleteObserver(this);
		}
		sel_range = theSelection;
		sel_range.addObserver(this);
	}

	/**
	 * adjusts the bounds of the quality and sequence displayed.
	 * allows clipping of the displayed sequence.
	 * range parameters are in the coordinate system of the sequence,
	 * i.e. (10,20) is the range between position 10 and 20 in the sequence,
	 * inclusive.
	 *
	 * @param start the integer indicating the starting base position
	 * @param end the integer indicating the final base position.
	 */
	public void setRange(int start, int end) {
		this.range = new Range(start, end);
		bar_map.setMapRange(start, end);
		base_map.setMapRange(start, end);
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
		}
		else if (component == AXIS_ZOOMER) {
			hzoom_loc = placement;
		}
		else if (component == OFFSET_ZOOMER) {
			vzoom_loc = placement;
		}
		else if (component == BARS) {
			bar_loc = placement;
		}
		else if (component == BASES) {
			base_loc = placement;
		}
		else {
			throw new IllegalArgumentException(
					"cannot configureLayout for an unknown component.");
		}
		doLayout();
		Container parent = getParent();
		if (parent instanceof NeoPanel) {
			((NeoPanel)parent).forceBackgroundFill();
		}
		repaint();
	}

	public int getPlacement(int component) {
		if (component == AXIS_SCROLLER)  { return hscroll_loc; }
		else if (component == AXIS_ZOOMER)  { return hzoom_loc; }
		else if (component == OFFSET_ZOOMER) { return vzoom_loc; }
		else if (component == BARS)  { return bar_loc; }
		else if (component == BASES)   { return base_loc; }
		throw new IllegalArgumentException(
				"cannot getPlacement for an unknown component.");
	}

	public synchronized void doLayout() {

		Dimension dim = this.getSize();

		// Have to fix the base_height first!
		int base_height = base_map_pixel_height;
		int scroll_size = hscroll.getPreferredSize().height;

		int bar_x = scroll_size;
		int bar_y = 0;
		int bar_width = dim.width - scroll_size - scroll_size ;
		int bar_height = dim.height - base_height - scroll_size;

		int base_x = scroll_size;
		int base_y = bar_height;
		int base_width = bar_width;

		int hscroll_y = base_y + base_height ;
		int hscroll_x = bar_x;
		int hscroll_width = bar_width;

		int vzoom_x = bar_x + bar_width;
		int vzoom_y = 0;
		int vzoom_height = bar_height + base_height;

		int hzoom_x = 0;
		int hzoom_y = 0;
		int hzoom_height = bar_height + base_height;

		bar_map.setBounds(bar_x, bar_y, bar_width, bar_height);

		base_map.setBounds(base_x, base_y, base_width, base_height);

		hscroll.setBounds(hscroll_x, hscroll_y, hscroll_width, scroll_size);
		hscroll.setSize(bar_width, scroll_size);

		vzoom.setBounds(vzoom_x, vzoom_y, scroll_size, vzoom_height);
		vzoom.setSize(scroll_size, vzoom_height);

		hzoom.setBounds(hzoom_x, hzoom_y, scroll_size, hzoom_height);
		hzoom.setSize(scroll_size, hzoom_height);
	}

	/**
	 * Associates a set of confidence scores with this widget.
	 * ReadConfidence contains the sequence bases and a quality score per base.
	 *
	 * @param read_conf the ReadConfidence containing the sequence
	 *                  and quality scores.
	 *
	 * @see ReadConfidence
	 */
	public void setReadConfidence(ReadConfidence read_conf) {

		this.read_conf = read_conf;
		read_length = read_conf.getReadLength();
		read_height_max = read_conf.getMaxValue();

		bar_map.setMapRange(0,read_length);
		bar_map.setMapOffset(0,read_height_max);

		bar_glyph = new QualityBars(read_conf);
		bar_map.getScene().addGlyph(bar_glyph);
		bar_glyph.setCoords(0,0,read_length,read_height_max);
		bar_map.zoomOffset(.2f);
		bar_map.scrollOffset(read_height_max);

		base_map.setMapRange(0,read_length);
		base_map.setMapOffset(0,base_map_pixel_height);

		base_glyph = new QualityBases(read_conf);
		base_map.getScene().addGlyph(base_glyph);
		base_glyph.setCoords(0,0,read_length,base_map_pixel_height);
		base_map.scrollOffset(0);

		line_glyph = new FillRectGlyph();
		line_glyph.setCoords(0,1,read_length,1);
		line_glyph.setColor(Color.white);
		base_map.getScene().addGlyph(line_glyph);

		updateWidget();
	}

	public void scrollRange(double value) {
		bar_map.scrollRange(value);
		base_map.scrollRange(value);
	}

	public void zoomRange(double value) {
		bar_map.zoomRange(value);
		base_map.zoomRange(value);
	}

	/**
	 * adjusts the display
	 * so that the base at <code>baseNum</code> is centered.
	 *
	 * @param baseNum the integer index into the sequence of the base
	 *                to center the display.
	 */
	public void centerAtBase(int baseNum) {
		// if baseNum is too big, set to last base in read
		if (baseNum > read_length) {
			baseNum = read_length ;
		}
		double xcenter = baseNum;
		Rectangle2D.Double viewbox = bar_map.getView().getCoordBox();
		double xstart = xcenter - viewbox.width/2;

		scrollRange((int)xstart);
		updateWidget();
	}

	public int getLocation(NeoAbstractWidget widg) {
		if (widg == bar_map) { return BARS; }
		else if (widg == base_map) { return BASES; }
		throw new IllegalArgumentException("unknown widget");
	}

	public NeoAbstractWidget getWidget(int location) {
		if (location == BARS) { return bar_map; }
		else if (location == BASES) { return base_map; }
		throw new IllegalArgumentException("can only get BARS or BASES.");
	}

	public Range getVisibleRange() {
		Rectangle2D.Double box = bar_map.getView().calcCoordBox();
		return new Range((int)box.x, (int)(box.x + box.width));
	}

	/**
	 * selects a region of bases.
	 *
	 * @param start the integer index of the starting base of the sequence
	 * @param end   the integer index of the final base of the sequence
	 */
	public void selectBases(int start, int end) {
		highlightBars(start,end);
		highlightBases(start,end);

		// Make sure the at least part of the selection is visible.
		Range r = getVisibleRange();
		if (end < r.beg || r.end < start) {
			if (end < r.beg) {
				scrollRange(start);
			}
			else {
				scrollRange(end);
			}
		}

	}

	/**
	 * highlights a region of the quality display.
	 *
	 * @param start the integer starting base position
	 * @param end   the integer ending base position
	 *
	 */
	public void highlightBars(int start, int end) {
		bar_glyph.select(start,end);
		updateWidget();
	}

	/**
	 * highlights a region of bases.
	 *
	 * @param start the integer index of the starting base of the sequence
	 * @param end   the integer index of the final base of the sequence
	 */
	public void highlightBases(int start, int end) {
		base_glyph.select(start,end);
		updateWidget();
	}


	protected void barMapStartHighlight(NeoMouseEvent evt) {
		for (Object item : evt.getItems()) {
			int base = (int)evt.getCoordX();
			sel_range.setPoint(base);
			sel_range.notifyObservers();
		}
	}

	protected void barMapExtendHighlight(NeoMouseEvent evt) {
		for (Object item : evt.getItems()) {
			int base = (int)evt.getCoordX();
			sel_range.update(base);
			sel_range.notifyObservers();
		}
	}

	public void heardMouseEvent(MouseEvent evt) {
		if (evt instanceof NeoMouseEvent) {
			NeoMouseEvent nevt = (NeoMouseEvent)evt;
			Object source = nevt.getSource();
			if (source == bar_map) {
				int id = nevt.getID();
				if (sel_behavior != NO_SELECTION) {
					if ((id == nevt.MOUSE_PRESSED && sel_behavior == ON_MOUSE_DOWN) ||
							(id == nevt.MOUSE_RELEASED && sel_behavior == ON_MOUSE_UP)) {
						if (nevt.isShiftDown() && (!sel_range.isEmpty())) {
							barMapExtendHighlight(nevt);
						}
						else {
							barMapStartHighlight(nevt);
						}
							}
					else if (id == nevt.MOUSE_DRAGGED &&
							sel_behavior == ON_MOUSE_DOWN) {
						barMapExtendHighlight(nevt);
							}
				}
			}			
		}
		super.heardMouseEvent(evt);
	}

	/**
	 * Clears the bases selected in calls to selectBases.
	 *
	 * @see #selectBases
	 */
	public void clearSelection() {
		bar_glyph.clearSelection();
		base_glyph.clearSelection();
	}

	/* Methods for Dealing with Selection */
	public void select(GlyphI gl) {
		throw new RuntimeException("select not yet implemented.");
	}
	public void select(Vector vec) {
		throw new RuntimeException("select not yet implemented.");
	}

	public void deselect(GlyphI gl) {
		throw new RuntimeException("deselect not yet implemented.");
	}
	public void deselect(Vector vec) {
		throw new RuntimeException("deselect not yet implemented.");
	}

	public List<GlyphI> getSelected() {
		return selected;
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
						"SelectionEvent can only be NO_SELECTION, ON_MOUSE_DOWN, " +
						"or ON_MOUSE_UP.");
		}
	}
	public int getSelectionEvent() {
		return this.sel_behavior;
	}

	public void setBackground(int id, Color col) {
		switch (id) {
			case BARS: bar_map.setMapColor(col); break;
			case BASES: base_map.setMapColor(col); break;
			default:
						throw new IllegalArgumentException("NeoQualler.setBackground(id, " +
								"color) currently only supports ids of " +
								"BARS or BASES");
		}
	}
	public Color getBackground(int id) {
		switch (id) {
			case BARS: return bar_map.getMapColor();
			case BASES: return base_map.getMapColor();
		}
		throw new IllegalArgumentException("NeoQualler.getBackground(id) " +
				"currently only supports ids of " +
				"BARS or BASES");
	}

	public void setBarsBackground(Color theColor) {
		setBackground(BARS, theColor);
	}
	public Color getBarsBackground() {
		return getBackground(BARS);
	}
	public void setBasesBackground(Color theColor) {
		setBackground(BASES, theColor);
	}
	public Color getBasesBackground() {
		return getBackground(BASES);
	}

	public void update(Observable theObserved, Object theArgument) {
		if (theObserved instanceof Selection) {
			update((Selection)theObserved);
		}
	}

	private void update(Selection theSelection) {
		selectBases(theSelection.getStart(), theSelection.getEnd());
	}

	protected Color trim_color = Color.lightGray;
	protected GlyphI left_trim_glyph, right_trim_glyph;
	private int leftTrim, rightTrim;
	public void setTrimColor(Color col) {
		if (col != trim_color) {
			trim_color = col;
			if (left_trim_glyph != null) {
				left_trim_glyph.setColor(trim_color);
			}
			if (right_trim_glyph != null) {
				right_trim_glyph.setColor(trim_color);
			}
		}
	}

	/**
	 * highlights the first n bases called.
	 * This can be used to show that some base calls should be trimmed
	 * from the 5' end due to low quality.
	 *
	 * @param theBasesTrimmed how many
	 */
	public void setBasesTrimmedLeft(int theBasesTrimmed) {
		this.leftTrim = theBasesTrimmed;
		Rectangle2D.Double coordbox = bar_glyph.getCoordBox();
		if (left_trim_glyph != null) {
			bar_glyph.removeChild(left_trim_glyph);
		}
		left_trim_glyph = new FillRectGlyph();
		left_trim_glyph.setColor(trim_color);
		left_trim_glyph.setCoords(coordbox.x, coordbox.y,
				theBasesTrimmed, coordbox.height);
		bar_glyph.addChild(left_trim_glyph);
		bar_map.toBack(left_trim_glyph);
	}

	public int getBasesTrimmedLeft() {
		return this.leftTrim;
	}

	/**
	 * highlights last n bases called.
	 * This can be used to show that some base calls should be trimmed
	 * from the 3' end due to low quality.
	 *
	 * @param theBasesTrimmed how many
	 */
	public void setBasesTrimmedRight(int theBasesTrimmed) {
		this.rightTrim = theBasesTrimmed;
		Rectangle2D.Double coordbox = bar_glyph.getCoordBox();
		if (right_trim_glyph != null) {
			bar_glyph.removeChild(right_trim_glyph);
		}
		right_trim_glyph = new FillRectGlyph();
		right_trim_glyph.setColor(trim_color);
		right_trim_glyph.setCoords(
				coordbox.x + coordbox.width - theBasesTrimmed,
				coordbox.y,
				theBasesTrimmed,
				coordbox.height);
		bar_glyph.addChild(right_trim_glyph);
		bar_map.toBack(right_trim_glyph);
	}

	public int getBasesTrimmedRight() {
		return this.rightTrim;
	}

	public void viewBoxChanged(NeoViewBoxChangeEvent evt) {
		if (range_listeners.size() > 0) {
			if (evt.getSource() == bar_map) {
				Rectangle2D.Double vbox = evt.getCoordBox();
				NeoRangeEvent nevt = new NeoRangeEvent(this,
						vbox.x, vbox.x + vbox.width);
				for (NeoRangeListener rl : range_listeners) {
					rl.rangeChanged(nevt);
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

}
