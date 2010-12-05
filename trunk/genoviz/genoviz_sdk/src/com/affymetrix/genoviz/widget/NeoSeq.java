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

import com.affymetrix.genoviz.awt.NeoCanvas;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.ConstrainLinearTrnsfm;
import com.affymetrix.genoviz.bioviews.DragMonitor;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.datamodel.EditableSequenceI;
import com.affymetrix.genoviz.datamodel.NASequence;
import com.affymetrix.genoviz.datamodel.Position;
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.datamodel.Sequence;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.datamodel.Translatable;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.util.*;
import com.affymetrix.genoviz.event.NeoDragEvent;
import com.affymetrix.genoviz.event.NeoDragListener;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.event.SequenceEvent;
import com.affymetrix.genoviz.event.SequenceListener;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.util.Selection;
import com.affymetrix.genoviz.widget.neoseq.AnnotationGlyph;
import com.affymetrix.genoviz.widget.neoseq.Caret;
import com.affymetrix.genoviz.widget.neoseq.WrapAnnot;
import com.affymetrix.genoviz.widget.neoseq.WrapColors;
import com.affymetrix.genoviz.widget.neoseq.WrapFontColors;
import com.affymetrix.genoviz.widget.neoseq.WrapNumbers;
import com.affymetrix.genoviz.widget.neoseq.WrapSequence;
import com.affymetrix.genoviz.widget.neoseq.WrapStripes;
import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Rectangle;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.swing.JScrollBar;

/**
 * Implementers can display a sequence of residues as letter codes.
 * Initializing, selecting, highlighting, cropping, and
 * interrogating are provided.<p>
 *
 * Example:<p>
 *
 * <pre>
 * String seq = "AAACGTGGGGGGGGGGGGGGGAAAAAAAAAAAAATTTTTTTTTTTTTTTTTT";
 * NeoSeqI seqview = new NeoSeq();
 * seqview.setSequence(seq);
 * seqview.setStripeColors( { new Color(255,255,255),
 *                            new Color(200,255,200),
 *                            new Color(200,200,200) } );
 * seqview.setStripeOrientation(NeoSeqI.VERTICAL_STRIPES);
 * seqview.setFontName("Courier");
 * seqview.setFontSize(10);
 * seqview.addAnnotation(5,25,Color.magenta);
 * seqview.addAnnotation(40,45,Color.yellow);
 * seqview.highlightResidues(3,30);
 *
 * </pre>
 *
 * <p> This javadoc explains the implementation specific features
 * of this widget concerning event handling and the java AWT.
 * In paticular, all genoviz implementations of widget interfaces
 * are subclassed from <code>Container</code>
 * and use the JDK 1.1 event handling model.
 *
 * <p> NeoSeq extends <code>java.awt.Container</code>
 * via <code>NeoContainerWidget</code>,
 * and thus, inherits all of the AWT methods
 * of <code>java.awt.Container</code>, and <code>Component</code>.
 * For example, a typical application might use the following
 * as part of initialization:
 *
 * <pre>
 *   NeoSeq seq = new NeoSeq();
 *   seq.setSize(500, 200);
 * </pre>
 *
 * @version $Id: NeoSeq.java 6578 2010-08-03 15:33:07Z jnicol $
 */
public class NeoSeq extends NeoContainerWidget
	implements NeoDragListener, Observer, NeoViewBoxListener,
			   SequenceListener, Translatable
{
	/**
	 * component identifier constant for the residue letter display.
	 * @see #getItems
	 */
	public static final int RESIDUES = 9000;

	/**
	 * component identifier constant for the numeric position display.
	 * @see #getItems
	 */
	public static final int NUMBERS = RESIDUES + 1;

	/**
	 * component identifier constant for the axis scroller.
	 * @see #getItems
	 */
	public static final int AXIS_SCROLLER = RESIDUES + 2;

	/**
	 * component identifier constant for other components not part
	 * of the interface description.
	 * @see #getItems
	 *
	 * @deprecated NeoSeqI.UNKNOWN used to hide NeoConstants.UNKNOWN.
	 */
	@Deprecated
	public static final int UNKNOWN = RESIDUES + 3;


	/**
	 * constant to remove background striping of sequence display.
	 * @see #setStripeOrientation
	 */
	public static final int NO_STRIPES = WrapStripes.NONE;

	/**
	 * constant to add vertical striping of sequence display (default).
	 * @see #setStripeOrientation
	 */
	public static final int VERTICAL_STRIPES = WrapStripes.VERTICAL;

	/**
	 * constant to add horizontal striping of sequence display (default).
	 * @see #setStripeOrientation
	 */
	public static final int HORIZONTAL_STRIPES = WrapStripes.HORIZONTAL;

	private boolean editable = false;
	private GlyphI caret;
	private Position insertionPoint;

	private static final boolean do_layout = true;
	private static final boolean do_reshape = true;
	private static final boolean check_reshape = false;
	private static final boolean fit_check = true;
	private static final boolean DEBUG_STRETCH = false;

	private static final Color default_map_background = Color.lightGray;
	private static final Color default_panel_background = Color.lightGray;
	private static final Color default_number_background = Color.lightGray;
	private static final Color default_foreground = Color.black;

	protected boolean showAs[] = new boolean[8];

	protected int sel_behavior = ON_MOUSE_DOWN;

	protected Selection sel_range;

	protected JScrollBar offset_scroll;

	// locations for scrollbars, consensus, and labels
	protected int offset_scroll_loc = PLACEMENT_RIGHT;
	protected int residue_loc = PLACEMENT_RIGHT;
	protected int num_loc = PLACEMENT_LEFT;

	protected SequenceI seq = new NASequence();

	protected NeoMap residue_map;

	// want to use residue_canvas to set up DragMonitor
	protected NeoCanvas residue_canvas;
	protected DragMonitor residue_drag_monitor;
	protected boolean drag_scrolling_enabled = true;

	protected int residue_map_pixel_width;

	// default value for residue_multiple_constraint is 10
	protected int residue_multiple_constraint = 10;

	// default value for residue_stripe_width is 10
	protected int residue_stripe_width = 10;

	protected NeoMap num_map;
	protected int num_map_pixel_width;

	protected Font residue_font;

	protected int orientation = VERTICAL_STRIPES;

	protected Color background = Color.lightGray;

	protected int residues_per_line;
	protected int line_width;
	protected int ypixels_per_line;

	protected int seq_map_size;

	// preferredSize effectively overrides prefSize field in Component,
	// if setPreferredSize() method is ever called
	protected Dimension preferred_size = null;
	// a few extra pixels used to calculate preferred size, so there is
	// some space below the baseline of the last row
	protected int extra_height = 3;

	/*
	 *  If not zero,
	 *  this will be used to determine the preferred size
	 *  for layout managers.
	 */
	private int preferredWidthInResidues = 0;
	private int preferredHeightInLines = 0;

	protected boolean residues_selected = false;

	protected boolean widget_ready = false;
	protected WrapSequence residue_glyph;
	protected WrapNumbers num_glyph;

	protected int scroll_increment;
	private final ConstrainLinearTrnsfm sclt = new ConstrainLinearTrnsfm();

	protected Set<NeoRangeListener> range_listeners = new CopyOnWriteArraySet<NeoRangeListener>();

	public NeoSeq() {
		super();

		showAs[NUCLEOTIDES] = true;
		showAs[COMPLEMENT] = false;
		showAs[FRAME_ONE] = false;
		showAs[FRAME_TWO] = false;
		showAs[FRAME_THREE] = false;
		showAs[FRAME_NEG_ONE] = false;
		showAs[FRAME_NEG_TWO] = false;
		showAs[FRAME_NEG_THREE] = false;

		LinearTransform res_trans = new LinearTransform();
		LinearTransform num_trans = new LinearTransform();

		residue_map = new NeoMap(false, false, NeoConstants.HORIZONTAL, res_trans);
		num_map = new NeoMap(false, false, NeoConstants.HORIZONTAL, num_trans);

		residue_map.enableDragScrolling(false);
		residue_canvas = residue_map.getNeoCanvas();
		enableDragScrolling(drag_scrolling_enabled);

		residue_map.setCheckZoomValue(false);
		num_map.setCheckZoomValue(false);
		residue_map.setCheckScrollValue(false);
		num_map.setCheckScrollValue(false);

		residue_glyph = new WrapSequence();
		for (int i=0; i<showAs.length; i++) {
			residue_glyph.setShow(i, showAs[i]);
		}
		residue_glyph.setColor(default_foreground);
		residue_map.getScene().addGlyph(residue_glyph);
		residue_map.setDataModel(residue_glyph, this.seq);

		num_glyph = new WrapNumbers();
		for (int i=0; i<showAs.length; i++) {
			residue_glyph.setShow(i, showAs[i]);
		}
		num_glyph.setColor(default_foreground);
		num_map.getScene().addGlyph(num_glyph);
		num_map.setDataModel(num_glyph, seq);

		residue_font = NeoConstants.default_plain_font;
		setFont(residue_font);

		residue_map.setMapColor(default_map_background);
		num_map.setMapColor(default_number_background);
		setBackground(default_panel_background);

		this.setLayout(null);

		offset_scroll = new JScrollBar(JScrollBar.VERTICAL);
		this.setScroller (offset_scroll);

		add((Component)offset_scroll);
		add(residue_map);
		add(num_map);

		residue_map.setMapRange(0, 1000);
		residue_map.setMapOffset(0, 1000);
		residue_map.setReshapeBehavior(NeoMap.X, NeoConstants.NONE);
		residue_map.setReshapeBehavior(NeoMap.Y, NeoConstants.NONE);
		residue_map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_START);

		num_map.setMapRange(0, 1000);
		num_map.setMapOffset(0, 1000);
		num_map.setReshapeBehavior(NeoMap.X, NeoConstants.NONE);
		num_map.setReshapeBehavior(NeoMap.Y, NeoConstants.NONE);
		num_map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_START);

		residue_map.addMouseListener(this);
		residue_map.addMouseMotionListener(this);
		residue_map.addKeyListener(this);
		num_map.addMouseListener(this);
		num_map.addMouseMotionListener(this);
		num_map.addKeyListener(this);

		// adding NeoSeq as event listener on residue map's View
		// need this because residue_map is generating NeoRangeEvents,
		// but with wrong range (because "range" for NeoSeq is really Y coords)
		// Therefore need ignore residue_map NeoRangeEvents, and instead catch
		// NeoViewBoxChangeEvents from the residue_map's View, and convert them
		// to a correct NeoRangeEvent to send to any NeoSeq listeners
		residue_map.getView().addPostDrawViewListener(this);

		addWidget(residue_map);
		addWidget(num_map);

		residue_map.scrollOffset(0);
		num_map.scrollOffset(0);

		this.setScrollIncrementBehavior(Y, AUTO_SCROLL_INCREMENT);

		widget_ready = true;

		stretchToFit(false,false);
		setRubberBandBehavior(false);

		// added in attempt to fix bug # 157
		// had to make it one space long, else get range calculation errors...
		setResidues(" ");

		setSelection(new Selection());

		this.seq.addSequenceListener(this);

	}


	public void setEditable( boolean theAbility ) {
		if ( this.editable == theAbility )
			return; // It was already thus.

		this.editable = theAbility;

		if ( this.editable ) {
			if (seq instanceof EditableSequenceI) {
				EditableSequenceI eseq = (EditableSequenceI) seq;
				this.insertionPoint = eseq.createPosition(0);
				this.caret = residue_map.addItem (0, eseq.getLength(),
						"-glyphtype com.affymetrix.genoviz.widget.neoseq.Caret -color black" );
				this.caret.setColor(this.getResidueFontColor());
				this.residue_map.addItem( this.residue_glyph, this.caret );
				((Caret)this.caret).setResiduesPerLine(this.residues_per_line);
				((Caret)this.caret).setFill(Caret.OUTLINE);
				((Caret)this.caret).setPosition( this.insertionPoint );
				setCaretCoords();
			}
		}
		else { // get rid of the caret.
			this.residue_map.removeItem( this.caret );
			this.caret = null;
		}
		updateWidget();
	}

	@Override
	public void destroy() {
		residue_map.getView().removePostDrawViewListener ( this );
		this.seq.removeSequenceListener ( this );
		offset_scroll = null;
		super.destroy();
		range_listeners.clear();
	}

	boolean isEditable() {
		return this.editable;
	}

	public void setSelection(Selection theSelection) {
		if (null != sel_range) {
			sel_range.deleteObserver(this);
		}
		sel_range = theSelection;
		sel_range.addObserver(this);
	}

	/**
	 * Responds to cursor control keys.
	 * These keys do not generate KEY_TYPED events.
	 */
	@Override
	public void keyPressed(KeyEvent e) {
		if (null == this.seq)
			return;

		int rpl = getResiduesPerLine();
		Range vr = getVisibleRange();

		int rps = this.residue_glyph.getResiduesPerScreen();

		int last = this.seq.getLength() - 1;
		int p, q = 0;
		if (null != this.insertionPoint) {
			q = this.insertionPoint.getOffset();
		}

		switch ( e.getKeyCode() ) {
			case KeyEvent.VK_HOME:
				p = 0;
				if ( isEditable() )
					this.insertionPoint.setOffset( p );
				if ( 0 == ( KeyEvent.SHIFT_MASK & e.getModifiers() ) )
					clearSelection();
				else {
					if ( this.sel_range.isEmpty() ) {
						this.sel_range.setPoint( q );
					}
					residueMapExtendHighlight( p );
				}
				makeResidueVisible( 0 );
				updateWidget();
				break;
			case KeyEvent.VK_END:
				p = last;
				if ( 0 == ( KeyEvent.SHIFT_MASK & e.getModifiers() ) )
					clearSelection();
				else {
					if ( this.sel_range.isEmpty() ) {
						this.sel_range.setPoint( q );
					}
					residueMapExtendHighlight( p );
				}
				if ( isEditable() ) {
					p++;
					this.insertionPoint.setOffset( p );
				}
				makeResidueVisible( p );
				updateWidget();
				break;
			case KeyEvent.VK_UP:
				if ( isEditable() ) {
					p = this.insertionPoint.getOffset() - rpl;
					if ( 0 <= p ) {
						this.insertionPoint.setOffset( p );
						makeResidueVisible( p );
					}
					if ( 0 == ( KeyEvent.SHIFT_MASK & e.getModifiers() ) )
						clearSelection();
					else {
						if ( this.sel_range.isEmpty() ) {
							this.sel_range.setPoint( q );
						}
						residueMapExtendHighlight( p );
					}
				}
				else {
					p = Math.max( 0, vr.beg - 1 );
					makeResidueVisible( p );
				}
				updateWidget();
				break;
			case KeyEvent.VK_DOWN:
				if ( isEditable() ) {
					p = this.insertionPoint.getOffset() + rpl;
					p = Math.min( p, last + 1 );
					this.insertionPoint.setOffset( p );
					if ( 0 == ( KeyEvent.SHIFT_MASK & e.getModifiers() ) )
						clearSelection();
					else {
						if ( this.sel_range.isEmpty() ) {
							this.sel_range.setPoint( q );
						}
						residueMapExtendHighlight( p );
					}
				}
				else {
					p = Math.min( vr.end + 1, last );
				}
				makeResidueVisible( p );
				updateWidget();
				break;
			case KeyEvent.VK_RIGHT:
				if ( isEditable() ) {
					p = this.insertionPoint.getOffset();
					if ( p <= last ) {
						p++;
						this.insertionPoint.setOffset( p );
						makeResidueVisible( p );
						if ( 0 == ( KeyEvent.SHIFT_MASK & e.getModifiers() ) ) {
							clearSelection();
						}
						else {
							if ( this.sel_range.isEmpty() ) {
								this.sel_range.setPoint( q );
							}
							residueMapExtendHighlight( p );
						}
						updateWidget();
					}
				}
				break;
			case KeyEvent.VK_LEFT:
				if ( isEditable() ) {
					p = this.insertionPoint.getOffset();
					if ( 0 < p ) {
						p--;
						this.insertionPoint.setOffset( p );
						makeResidueVisible( p );
						if ( 0 == ( KeyEvent.SHIFT_MASK & e.getModifiers() ) )
							clearSelection();
						else {
							if ( this.sel_range.isEmpty() ) {
								this.sel_range.setPoint( q );
							}
							residueMapExtendHighlight( p );
						}
						updateWidget();
					}
				}
				break;
			case KeyEvent.VK_PAGE_UP:
				if ( isEditable() ) {
					p = this.insertionPoint.getOffset();
					p = Math.max( 0, p - rps );
					this.insertionPoint.setOffset( p );
					if ( 0 == ( KeyEvent.SHIFT_MASK & e.getModifiers() ) )
						clearSelection();
					else {
						if ( this.sel_range.isEmpty() ) {
							this.sel_range.setPoint( q );
						}
						residueMapExtendHighlight( p );
					}
				}
				scrollSequence( Math.max( 0, vr.beg - rps ) );
				updateWidget();
				break;
			case KeyEvent.VK_PAGE_DOWN:
				if ( isEditable() ) {
					p = this.insertionPoint.getOffset();
					p = Math.min( p + rps, last+1 );
					this.insertionPoint.setOffset( p );
					if ( 0 == ( KeyEvent.SHIFT_MASK & e.getModifiers() ) )
						clearSelection();
					else {
						if ( this.sel_range.isEmpty() ) {
							this.sel_range.setPoint( q );
						}
						residueMapExtendHighlight( p );
					}
				}
				makeResidueVisible( Math.min( vr.end + rps, last ) );
				// There is still a bug here in that it scrolls down a line
				// even when the last residue is already visible.
				updateWidget();
				break;
		}
	}

	/**
	 * Responds to ASCII keys being typed.
	 */
	@Override
	public void keyTyped(KeyEvent e) {
		if ( ! this.isEditable() )
			return;

		if (seq instanceof EditableSequenceI) {
			EditableSequenceI eseq = (EditableSequenceI) this.seq;

			int p = this.insertionPoint.getOffset();
			switch ( e.getKeyChar() ) {
				case KeyEvent.VK_BACK_SPACE:
					if ( sel_range.isEmpty() ) {
						if (0 < p) {
							p--;
							eseq.remove( p, 1 );
						}
					}
					else {
						eseq.remove
							( sel_range.getStart(), sel_range.getEnd() - sel_range.getStart() + 1 );
						clearSelection();
					}
					makeResidueVisible( this.insertionPoint.getOffset() );
					updateWidget();
					break;
				case KeyEvent.VK_DELETE:
					if ( p < eseq.getLength() ) {
						if ( sel_range.isEmpty() ) {
							eseq.remove( p, 1 );
						}
						else {
							eseq.remove
								( sel_range.getStart(), sel_range.getEnd() - sel_range.getStart() + 1 );
							clearSelection();
						}
						makeResidueVisible( this.insertionPoint.getOffset() );
						updateWidget();
					}
					break;
				case KeyEvent.VK_TAB:
				case KeyEvent.VK_ENTER:
					break;
				default:
					if ( ! sel_range.isEmpty() ) {
						eseq.remove
							( sel_range.getStart(), sel_range.getEnd() - sel_range.getStart() + 1 );
						clearSelection();
					}
					eseq.insertString( this.insertionPoint.getOffset(),
							String.valueOf( e.getKeyChar() ) );
					makeResidueVisible( this.insertionPoint.getOffset() );
					updateWidget();
			}
		}

	}

	protected WrapSequence getResidueGlyph() {
		return residue_map.<WrapSequence>getItem(seq);
	}

	protected WrapNumbers getNumGlyph() {
		return num_map.<WrapNumbers>getItem(seq);
	}

	public void configureLayout(int component, int placement) {
		if (component == AXIS_SCROLLER) {
			offset_scroll_loc = placement;
		}
		else if (component == RESIDUES) {
			residue_loc = placement;
		}
		else if (component == NUMBERS) {
			num_loc = placement;
		}
		else {
			throw new IllegalArgumentException(
					"can only configureLayout for an AXIS_SCROLLER, RESIDUES, or NUMBERS.");
		}
		doLayout();
		// trying to fix paint issues when configuring layout
		Container parent = getParent();
		if (parent instanceof NeoPanel) {
			((NeoPanel)parent).forceBackgroundFill();
		}
		repaint();
	}

	public int getPlacement(int component) {
		if (component == AXIS_SCROLLER)  { return offset_scroll_loc; }
		else if (component == RESIDUES)  { return residue_loc; }
		else if (component == NUMBERS)   { return num_loc; }
		throw new IllegalArgumentException(
				"can only getPlacement of an AXIS_SCROLLER, RESIDUES, or NUMBERS.");
	}

	@Override
	public void doLayout() {

		// Assume that we can bail if our offset_scroll isn't a Component.

		if (!(offset_scroll instanceof Component))
			return;

		Component offsetComp = (Component) offset_scroll;

		Dimension dim = this.getSize();
		int scroll_size = offsetComp.getPreferredSize().width;

		int offset_scroll_x = dim.width - scroll_size;
		int offset_scroll_y = 0;
		int offset_scroll_width = scroll_size;
		int offset_scroll_height = dim.height;

		int residue_x = 0;
		int residue_y = 0;
		int residue_width = 0;
		int residue_height = 0;

		int num_x = 0;
		int num_y = 0;
		int num_width = 0;
		int num_height = 0;

		if (num_loc == PLACEMENT_LEFT) {
			// Have to fix the residue_height first!
			num_width = num_map_pixel_width;

			// next set dimensions of residue_map
			residue_x = num_width;
			residue_y = 0;
			residue_width = dim.width - scroll_size - num_width;
			residue_height = dim.height;
			residue_map_pixel_width = residue_width;

			num_x = 0;
			num_y = 0;
			num_height = dim.height;
		}
		else if (num_loc == PLACEMENT_RIGHT) {
			// Have to fix the residue_height first!
			num_width = num_map_pixel_width;

			// next set dimensions of residue_map
			residue_x = 0;
			residue_y = 0;
			residue_width = dim.width - scroll_size - num_width;
			residue_height = dim.height;
			residue_map_pixel_width = residue_width;

			num_x = dim.width - scroll_size - num_width;
			num_y = 0;
			num_height = dim.height;
		}
		else if (num_loc == PLACEMENT_NONE) {
			// Have to fix the residue_height first!
			num_width = 0;

			// next set dimensions of residue_map
			residue_x = 0;
			residue_y = 0;
			residue_width = dim.width - scroll_size - num_width;
			residue_height = dim.height;
			residue_map_pixel_width = residue_width;

			num_x = dim.width - scroll_size;
			num_y = 0;
			num_height = dim.height;
		}

		// trying to debug NeoSeq hanging  GAH 11-21-98
		// as near as I can tell, the reshape calls to num_map and residue_map are
		//   forcing paint calls (which completely bypass repaint() and update())
		//   to the corresponding NeoCanvases.  I think this is coming out of the
		//   peers, since a NeoWidget.reshape() will invoke Component.reshape(),
		//   which will in turn call Component/CanvasPeer.reshape(), which I believe
		//   somehow invokes a PAINT event and calls (in jdk1.1)
		//   Component.dispatchEventImpl(paint_event), which calls Component.paint(g)
		//   directly in response to PaintEvent.PAINT events
		// Two big questions:
		//   is this reshape-induced, peer-mediated PAINT event propogation reliable
		//      across different JVMs, especially jdk1.0 vs jdk1.1 vs jdk1.x ???
		//   WHY does this cause NeoSeq to hang, when combined with updateWidget()
		//      calls?  I suspect synchronization deadlocks caused by synchronized
		//      blocks in standard AWT code somehow getting entangled with
		//      Neo code.  But I haven't been able to prove anything, beyond the fact
		//      that the hanging problem seen looks very much like one would expect
		//      with synchronization deadlocks.  I suppose there could be some other
		//      problem leading to infinite loop/recursion, but deadlock seems more
		//      likely
		//
		// For now, I am trying to reduce the possibility of this bug raising its
		//    ugly head via two modifications to NeoSeq:
		// 1. removed several updateWidget() calls that are redundant.  This assumes
		//      however that the reshape-->peer-->paint pathway is a trait shared
		//      by all JVMs, otherwise more external updateWidget() calls will be
		//      required to make sure the proper redraws occur!
		// 2. trying to reduce the number of map reshapes that are called via layout()
		//      toward this end I've added bounds checks as conditionals for map
		//      and scrollbar reshapes, so that if the bounds of the map have not
		//      changed, no reshape is called
		Rectangle bbox = residue_map.getBounds();
		bbox = num_map.getBounds();
		residue_map.setBounds(residue_x, residue_y, residue_width, residue_height);
		num_map.setBounds(num_x, num_y, num_width, num_height);

		bbox = offsetComp.getBounds();
		if (do_reshape && ((!check_reshape) ||
					bbox.x != offset_scroll_x || bbox.y != offset_scroll_y ||
					bbox.width != offset_scroll_width || bbox.height != offset_scroll_height)) {
			offsetComp.setBounds(offset_scroll_x, offset_scroll_y,
					offset_scroll_width, offset_scroll_height);
			offsetComp.setSize(offset_scroll_width, offset_scroll_height);
					}

		stretchToFit(false, false);
	}


	@Override
	public void stretchToFit(boolean b1, boolean b2) {

		int start_base = (int)num_map.getView().getCoordBox().y;

		if (DEBUG_STRETCH) {
			System.out.println("Beg of NeoSeq.stretchToFit, SeqBox = " +
					residue_map.getScene().getCoordBox() +
					"\n                           ViewBox = " +
					residue_map.getView().getCoordBox());
		}

		super.stretchToFit(b1,b2);


		if (fit_check) {
			int check_base = (int)num_map.getView().getCoordBox().y;
			if (start_base != check_base && residues_per_line != 0) {
				start_base = (check_base / residues_per_line) * residues_per_line;
			}
		}

		if (DEBUG_STRETCH) {
			System.out.println("Mid of NeoSeq.stretchToFit, SeqBox = " +
					residue_map.getScene().getCoordBox() +
					"\n                           ViewBox = " +
					residue_map.getView().getCoordBox());
		}

		setResiduesPerLine(residue_map_pixel_width / getResiduePixelWidth());
		ypixels_per_line = getResiduePixelHeight();

		seq_map_size = this.seq.getLength() + residues_per_line;

		residue_map.setMapOffset(0, seq_map_size + residues_per_line);

		residue_map.setMapRange(0, line_width);

		double offzoom = (double) ypixels_per_line / residues_per_line;
		residue_map.zoomOffset(offzoom);

		if(residue_glyph != null) {
			residue_map.zoomRange(getResiduePixelWidth());
			residue_map.scrollRange(0);
			residue_glyph.setCoords(0, 0, residues_per_line, seq_map_size);
		}

		// trying temp fix for bug that cuts off last line:
		// overcompensate by residues_per_line -- GAH 6-10-98

		num_map.setMapOffset(0, seq_map_size + residues_per_line);

		num_map.setMapRange(0, line_width);
		num_map.zoomOffset(offzoom);

		// Resizing so window is too small for any sequence to appear causes
		// viewbox y coord (and therefore start_base to go way big for some
		// reason... so compensating for this here
		if (start_base > this.seq.getLength() || start_base < 0) {
			start_base = 0;
		}

		residue_map.scrollOffset(start_base);
		num_map.scrollOffset(start_base);

		if (DEBUG_STRETCH) {
			System.out.println("residues_per_line = " + residues_per_line +
					"  seq_map_size = " + seq_map_size +
					"  line_width = " + line_width +
					"  offzoom = " + offzoom);
		}

		if(num_glyph != null) {
			num_glyph.setCoords(0, 0, residues_per_line, seq_map_size);
		}

		if (DEBUG_STRETCH) {
			System.out.println("End of NeoSeq.stretchToFit, SeqBox = " +
					residue_map.getScene().getCoordBox() +
					"\n                           ViewBox = " +
					residue_map.getView().getCoordBox());
		}

		// Make sure caret will be always be visible.
		// This should not need to be here. But it does as of 3/30/1999 - elb
		setCaretCoords();

	}

	/**
	 * Makes sure the caret is visible.
	 */
	private void setCaretCoords() {
		if ( null != this.caret ) {
			this.caret.setCoords ( 0, 0, residues_per_line, seq_map_size );
		}
	}

	@Override
	public void setBounds(int x, int y, int width, int height) {
		super.setBounds(x, y, width, height);
		doLayout();
		residue_map.setPixelBounds();
		num_map.setPixelBounds();
		stretchToFit(false, false);
		residue_map.getView().calcCoordBox();
	}

	public GlyphI addTextColorAnnotation(int start, int end, Color color) {
		return getResidueGlyph().addTextColorAnnotation(start, end, color);
	}

	/**
	 *  add an outline as an annotation along the sequence.
	 */
	public GlyphI addOutlineAnnotation(int start, int end, Color color) {
		return getResidueGlyph().addOutlineAnnotation(start, end, color);
	}

	/**
	 * add an annotation of a particular color on a specified sub-region.
	 *
	 * @param start  the integer starting coordinate of the annotation.
	 * @param end  the integer ending coordinate of the annotation.
	 * @param color the color of the annotation.
	 * @return       a tag to associate with the annotation for later reference
	 *
	 * @see NeoAbstractWidget#getColor
	 */
	public GlyphI addAnnotation(int start, int end, Color color) {
		return getResidueGlyph().addAnnotation(start, end, color);
	}

	/**
	 * gets the range of an annotation.
	 *
	 * @param annotation an item added with the addAnnotation method.
	 * @return the Range (start to end) of the annotation.
	 */
	public Range getAnnotationRange(GlyphI annotation) {
		Range r = new Range(0,0);
		if (annotation instanceof AnnotationGlyph) {
			r.beg = ((AnnotationGlyph) annotation).getStart();
			r.end = ((AnnotationGlyph) annotation).getEnd();
		}
		else {
			throw new IllegalArgumentException(
					"can only getAnnotationRange for an annotation.");
		}
		return r;
	}

	/**
	 * gets the start of an annotation.
	 *
	 * @param annotation an item added with the addAnnotation method.
	 * @return the start of the annotation.
	 */
	public int getAnnotationStart(GlyphI annotation) {
		int s = 0;
		if (annotation instanceof AnnotationGlyph) {
			s = ((AnnotationGlyph) annotation).getStart();
		}
		else {
			throw new IllegalArgumentException(
					"can only getAnnotationStart for an annotation.");
		}
		return s;
	}

	/**
	 * gets the end of an annotation.
	 *
	 * @param annotation an item added with the addAnnotation method.
	 * @return the end of the annotation.
	 */
	public int getAnnotationEnd(GlyphI annotation) {
		int e = 0;
		if (annotation instanceof AnnotationGlyph) {
			e = ((AnnotationGlyph) annotation).getEnd();
		}
		else {
			throw new IllegalArgumentException(
					"can only getAnnotationEnd for an annotation.");
		}
		return e;
	}

	/**
	 * remove an annotation from the sequence.
	 *
	 * @param gl  the tag associated with the annotation
	 */
	public void removeAnnotation(GlyphI gl) {
		getResidueGlyph().removeAnnotation(gl);
	}

	public void removeAnnotations(List<GlyphI> glyphs) {
		for (int i=0; i<glyphs.size(); i++) {
			removeAnnotation(glyphs.get(i));
		}
	}

	/**
	 * highlight the sequence residues
	 * between start and end and creates a selection.
	 *
	 * @param start  the integer starting coordinate of the highlight
	 * @param end  the integer ending coordinate of the highlight
	 *
	 * @see #getSelectedResidues
	 */
	// need to change this so selection doesn't compete with
	//  annotations -- selection should always be on top???
	public void highlightResidues(int start, int end) {
		residues_selected = true;
		getResidueGlyph().highlightResidues(start, end);
		updateWidget();
	}

	/**
	 * Removes highlighting of selected range of sequence.
	 */
	public void deselect() {
		residues_selected = false;
		WrapSequence wrapseq = getResidueGlyph();
		if (wrapseq != null) {
			wrapseq.unhighlight();
		}
	}

	/**
	 * Adds residues to the current sequence.
	 * @param residues a String of residues that are to be
	 *                    added to the sequence.
	 */
	public void appendResidues(String residues) {
		// if no previous sequence, then residues becomes full sequence
		if (seq == null) {
			setResidues(residues);
			return;
		}
		// want to remain in same place, need to remember start and restore
		//   after calling setResidues()
		int start_base = (int)num_map.getView().getCoordBox().y;
		seq.appendResidues(residues);
		setSequence(seq);
		residue_map.scrollOffset(start_base);
		num_map.scrollOffset(start_base);
		residue_map.adjustScroller(NeoMap.Y);
	}

	/**
	 * sets the residues in the NeoSeq.
	 *
	 * @param seq_string contains the residues.
	 * @deprecated Use setResidues(String residues) instead.
	 */
	@Deprecated
		public void setSequence(String seq_string) {
			setResidues(seq_string);
		}

	/**
	 * Sets the residues of the NeoSeq.
	 *
	 * @param residues   a String of residues
	 * @return the glyph representing the residues.
	 */
	public GlyphI setResidues(String residues) {
		if (residues == null) {
			throw new IllegalArgumentException("NeoSeq.setResidues() requires a " +
					"String argument, was passed null instead");
		}
		Sequence s = new NASequence();
		s.setResidues(residues);
		setSequence(s);
		return getResidueGlyph();
	}

	/**
	 * @return a string of Nucleotide or Amino Acid codes.
	 */
	public String getResidues() {
		String r = null;
		SequenceI s = getSequence();
		if (null != s) {
			r = s.getResidues();
		}
		return r;
	}

	public SequenceI getSequence() {
		return seq;
	}

	public void setFirstOrdinal (int first) {
		if (num_glyph == null)
			return;
		num_glyph.setFirstOrdinal (first);
	}

	/**
	 * sets the sequence
	 * with the first residue having the supplied ordinal number.
	 *
	 * @param first ordinal of the first residue.
	 */
	public void setSequence(SequenceI seq, int first) {
		setSequence(seq);
		setFirstOrdinal(first);
	}

	/**
	 * copies the given sequence into the internal sequence.
	 */
	public void setSequence(SequenceI seq) {
		// Should not accept a null Sequence
		if (seq == null) {
			throw new IllegalArgumentException("NeoSeq.setSequence() requires a " +
					"Sequence argument, was passed null instead");
		}

		this.seq.setResidues(seq.getResidues());

		// why is this needed twice???  GAH 11-21-98
		residue_map.scrollOffset(0);
		num_map.scrollOffset(0);

		getResidueGlyph().setSequence(this.seq);
		getNumGlyph().setSequence(this.seq);

		residue_map.scrollRange(0);
		residue_map.zoomRange(getResiduePixelWidth());
		num_map.scrollRange(0);

		stretchToFit(false,false);

		// why is this needed twice???  GAH 11-21-98
		residue_map.scrollOffset(0);
		num_map.scrollOffset(0);

		calcNumPixelWidth();

		// Make sure caret will be always be visible.
		if ( null != this.caret ) {
			this.caret.setCoords ( 0, 0, residues_per_line, seq_map_size );
		}

		updateWidget();
	}


	/**
	 * Scrolls so that a particular residue is visible.
	 * If it is already within range, then no scrolling occurs.
	 * The widget is scrolled just enough to make the residue visible,
	 * and no more.
	 *
	 * @param theResidueIndex points to the residue that must become visible.
	 */
	public void makeResidueVisible(int theResidueIndex) {
		Range r = getVisibleRange();
		if ( theResidueIndex < r.beg ) {
			scrollSequence( theResidueIndex );
		}
		else if ( r.end < theResidueIndex ) {
			int rpl = getResiduesPerLine();
			if (rpl > 0) {
				int rps = this.residue_glyph.getResiduesPerScreen() - 1;
				int top;
				for ( top = r.beg;
						top + rps < theResidueIndex;
						top += rpl
					);
				scrollSequence( top );
			}
		}
	}

	/**
	 * Scrolls so that the given residue is on the first visible line.
	 *
	 * @param value points to a residue that must be in the top visible line.
	 */
	public void scrollSequence(int value) {
		int cval = value - (value % scroll_increment);
		residue_map.scrollOffset(cval);
		num_map.scrollOffset(cval);
	}

	public int getLocation(NeoAbstractWidget widg) {
		if (widg == residue_map) {
			return RESIDUES;
		}
		else if (widg == num_map) {
			return NUMBERS;
		}
		throw new IllegalArgumentException(
				"widget is located at neither RESIDUES or NUMBERS.");
	}

	public NeoAbstractWidget getWidget(int location) {
		if (location == RESIDUES) {
			return residue_map;
		}
		else if (location == NUMBERS) {
			return num_map;
		}
		throw new IllegalArgumentException(
				"only gettable widgets here are RESIDUES and NUMBERS.");
	}

	protected void residueMapStartHighlight(NeoMouseEvent evt) {
		int residue = getCoordResidue(evt.getCoordX(),evt.getCoordY());
		sel_range.setPoint(residue);
		sel_range.setEmpty(true);
		sel_range.notifyObservers();
	}

	protected void residueMapExtendHighlight( NeoMouseEvent evt ) {
		int r = getCoordResidue( evt.getCoordX(),evt.getCoordY() );
		residueMapExtendHighlight( r );
	}

	private void residueMapExtendHighlight( int residue ) {
		this.sel_range.setEmpty(false);
		sel_range.update( residue );
		sel_range.notifyObservers();
	}

	@Override
	public void heardMouseEvent(MouseEvent evt) {
		if (!(evt instanceof NeoMouseEvent)) { return; }
		NeoMouseEvent e = (NeoMouseEvent)evt;
		int id = e.getID();

		// need to fix event source and coord source coming from NeoMap!
		// also need to add location property to NeoMouseEvent!

		if (e.getSource() == residue_map) {
			int x = e.getX();
			int y = e.getY();
			if ( this.isEditable() ) {
				if ( ON_MOUSE_DOWN == sel_behavior && NeoMouseEvent.MOUSE_PRESSED == e.getID() ||
						ON_MOUSE_UP == sel_behavior && NeoMouseEvent.MOUSE_RELEASED == e.getID() ||
						NeoMouseEvent.MOUSE_DRAGGED == e.getID() ) {
					int residue = getCoordResidue(e.getCoordX(),e.getCoordY());
					this.insertionPoint.setOffset( residue );
					updateWidget();
						}
			}
			if (sel_behavior != NO_SELECTION) {
				if ((id == NeoMouseEvent.MOUSE_PRESSED && sel_behavior == ON_MOUSE_DOWN) ||
						(id == NeoMouseEvent.MOUSE_RELEASED && sel_behavior == ON_MOUSE_UP)) {

					if (!residue_canvas.getBounds().contains(x, y)) {
						return;
					}
					if (e.isShiftDown() && (!sel_range.isEmpty())) {
						residueMapExtendHighlight(e);
					}
					else {
						residueMapStartHighlight(e);
					}
						}
				else if (id == NeoMouseEvent.MOUSE_DRAGGED && sel_behavior == ON_MOUSE_DOWN) {
					// checking to make sure drag is within bounds of canvas --
					//    DragMonitor will deal with drags to outside of canvas
					if (residue_canvas.getBounds().contains(x, y)) {
						residueMapExtendHighlight(e);
					}
				}
			}
		}
		super.heardMouseEvent(evt);
	}

	public Range getVisibleRange() {

		Rectangle2D.Double visible_box = residue_map.getView().calcCoordBox();

		int start = (int)(visible_box.y);
		int end = useConstrain(residues_per_line, visible_box.y, visible_box.height);

		if ( start < 0) {
			start = 0;
		}

		if (end > this.seq.getLength()) {
			end = this.seq.getLength();
		}

		// deal with Range problems when NeoSeq window is
		// shrunk so small that _no_ residues are visible
		// otherwise get end < start and Range throws an IllegalArgumentException
		// This has the undesired effect that when NeoSeq is too small to see
		// anything, it will to listeners for RangeChangeEvents etc. that
		// residue 0 is visible.  Should resolve this at some point, but for now
		// this is much preferable to throwing an Exception
		if (end < start) {
			end = start = 0;
		}

		Range r = new Range(start, end);
		return r;
	}

	/**
	 * returns the index of the residue in the sequence
	 * at the position <code>(xcoord, ycoord)</code> in the display.
	 *
	 * @param xcoord the horizontal offset (column) of the residue
	 * @param ycoord the vertical offset (row) of the residue
	 * @return  the integer index of the residue in the sequence
	 */
	public int getCoordResidue(double xcoord, double ycoord) {
		Rectangle2D.Double visible_box = residue_map.getView().calcCoordBox();

		if (xcoord < 0) {
			xcoord = 0;
		}

		if (xcoord > residues_per_line) {
			xcoord = residues_per_line;
		}

		int yoff = (int)(ycoord - visible_box.y);
		int residue =
			(int)(yoff - (yoff % residues_per_line) + xcoord + visible_box.y);

		Range r = getVisibleRange();

		if (r.end < residue && r.end <= this.seq.getLength()) {
			residue = r.end;
		}

		if (residue > this.seq.getLength()) {
			residue = this.seq.getLength();
			if ( this.isEditable() )
				residue++;
		}

		if ( residue < 0) {
			residue = 0;
		}
		return residue;
	}

	/**
	 * Clears the sequence range highlighting.
	 *
	 * <p> Note that this is different from clearSelected(),
	 * which is inherited from NeoContainerWidget
	 * and deselects all selected annotation glyphs.
	 *
	 * @see com.affymetrix.genoviz.widget.NeoContainerWidget#clearSelected
	 */
	public void clearSelection() {
		sel_range.clear();
		sel_range.notifyObservers();
		residues_selected = false;
	}

	protected void calcNumPixelWidth() {
		FontMetrics fontmet = GeneralUtils.getFontMetrics(residue_font);
		int font_width = fontmet.charWidth('C');

		int chars = (int) Math.log(Math.abs(this.seq.getLength()));
		if (chars < 1) {
			chars = 1;
		}
		num_map_pixel_width = font_width * chars;

		if (widget_ready) {
			if (do_layout) { doLayout(); }
			stretchToFit(false,false);
			updateWidget();
		}
	}

	/**
	 * get the Java AWT font of the sequence residues.
	 *
	 * @return  the java.awt.font used to display sequence residues
	 */
	@Override
	public Font getFont() {
		return residue_font;
	}

	/**
	 * set the display font of the sequence residues.  Proportionate or
	 * variable width fonts can both be specified.
	 *
	 * @param font the Java AWT font to be used to display
	 * sequence residues
	 */
	@Override
	public void setFont(Font font) {
		residue_font = font;

		getResidueGlyph().setFont(residue_font);
		getNumGlyph().setFont(residue_font);

		calcNumPixelWidth();
	}

	/**
	 * get the name of the display font of the sequence residues.
	 *
	 * @return the String name of the font to use
	 */
	public String getFontName() {
		return residue_font.getFamily();
	}

	/**
	 * set the display font of the sequence residues.  Proportionate or
	 * variable width fonts can both be specified.
	 *
	 * @param name the String name of the font to use
	 */
	public void setFontName(String name) {
		Font new_font = new Font(name, residue_font.getStyle(), residue_font.getSize());
		setFont(new_font);
	}

	/**
	 * get the size (in points) of the font used to display sequence residues.
	 *
	 * @return the integer point size of the font
	 */
	public int getFontSize() {
		return residue_font.getSize();
	}

	/**
	 * set the font size of the characters used to display the sequence residues.
	 *
	 * @param size the integer point size of the font
	 */
	public void setFontSize(int size) {
		Font new_font = new Font(residue_font.getFamily(), residue_font.getStyle(), size);
		setFont(new_font);
	}

	/**
	 * indicate the minimum grouping size with respect to line breaking.
	 * Lines will be wrapped such that the number of residues on a line
	 * is a multiple of <code>groupWidth</code>.
	 *
	 * @param i the integer minimum grouping size
	 */
	public void setResidueMultipleConstraint(int i) {
		if (i>0) {
			residue_multiple_constraint = i;
		}
		else {
			throw new IllegalArgumentException("can't constrain below 1.");
		}
	}

	/**
	 * retrieves the minimum grouping size with respect to line breaking.
	 *
	 * @see #setResidueMultipleConstraint
	 * @return the integer grouping size
	 */
	public int getResidueMultipleConstraint() {
		return residue_multiple_constraint;
	}

	/**
	 * set the width of the striping.
	 *
	 * @param i the integer width in pixels of the stripe
	 */
	public void setStripeWidth(int i) {
		if (i >= 0) {
			residue_stripe_width = i;
			getResidueGlyph().setStripeWidth(residue_stripe_width);
			updateWidget();
		}
		else {
			throw new IllegalArgumentException("stripes can't be narrower than 0.");
		}
	}

	/**
	 * returns the current width of striping.
	 *
	 * @return the integer width of striping.
	 */
	public int getStripeWidth() {
		return residue_stripe_width;
	}

	/**
	 * set the orientation of the striping in the display of the sequence residues.
	 * Striping is used as a background coloring of the residues to make viewing
	 * the sequence easier.
	 *
	 */

	public void setStripeOrientation(int i) {
		switch (i) {
			case WrapStripes.NONE:
			case WrapStripes.VERTICAL:
			case WrapStripes.HORIZONTAL:
				orientation = i;
				getResidueGlyph().setStripeOrientation(orientation);
				updateWidget();
				break;
			default:
				throw new IllegalArgumentException(
						"striping must be HORIZONTAL, VERTICAL, or NONE.");
		}
	}

	/**
	 * returns the current orientation of the striping.
	 *
	 * @return the constant orientation identifier,
	 * either NO_STRIPES, VERTICAL_STRIPES, or HORIZONTAL_STRIPES.
	 */
	public int getStripeOrientation() {
		return getResidueGlyph().getStripeOrientation();
	}

	public int getResiduesPerLine() {
		return residues_per_line;
	}

	protected void setResiduesPerLine(int residues) {
		line_width = residues;
		residues_per_line = residues;
		if (residue_multiple_constraint > 0) {
			residues_per_line -= residues_per_line % residue_multiple_constraint;
		}
		getResidueGlyph().setResiduesPerLine(residues_per_line);
		getNumGlyph().setResiduesPerLine(residues_per_line);
		if (scroll_behavior[Y] == AUTO_SCROLL_INCREMENT) {
			setScrollIncrement(residues_per_line);
		}
	}

	public void setScrollIncrement(int inc) {
		scroll_increment = inc;

		sclt.setConstrainValue(inc);
		
		sclt.setTransform(sclt.getScaleX(), 0, 0, sclt.getScaleY(), 0, 0);
		residue_map.setScrollTransform(NeoWidget.Y, sclt);
		num_map.setScrollTransform(NeoWidget.Y, sclt);
		offset_scroll.setBlockIncrement(inc);
		offset_scroll.setUnitIncrement(inc);
	}

	/**
	 * sets the number of pixels between each letter displayed.
	 *
	 * @param size the integer number of pixels between residues
	 */
	public void setSpacing(int size) {
		if (size < 0) {
			throw new IllegalArgumentException(
					"spacing cannot be set to less than 0.");
		}
		getResidueGlyph().setSpacing(size);
		getNumGlyph().setSpacing(size);
		stretchToFit(false,false);
		updateWidget();
	}
	public int getSpacing() {
		return getResidueGlyph().getSpacing();
	}

	/**
	 * sets the colors to use for striping.
	 * Two or more colors can be specified to alternate among.
	 *
	 * @param colors an array of Colors.  default is
	 *  { Color.white, Color.lightGray }
	 */
	public void setStripeColors(Color[] colors) {
		getResidueGlyph().setStripeColors(colors);
	}
	public Color[] getStripeColors() {
		return getResidueGlyph().getStripeColors();
	}

	/**
	 * Sets the Color of the numbers and residues in the NeoSeq.
	 * @param color the Color to be used for the residues
	 *                 and the numbers.
	 */
	public void setResidueColor(Color color) {
		getResidueGlyph().setColor(color);
		getNumGlyph().setColor(color);
		updateWidget();
	}

	/**
	 * Gets the color of the residues and the numbers.
	 * @return   the Color of the residues and the numbers.
	 */
	public Color getResidueColor() {
		return getResidueGlyph().getColor();
	}

	/**
	 * @return the visible residues
	 * or an empty string ("") if there are no residues.
	 */
	public String getVisibleResidues() {
		if (seq == null) { return ""; }
		Range r = getVisibleRange();
		return seq.getResidues(r.beg, r.end + 1);
	}

	/**
	 * @return the selected residues
	 * or an empty string ("") if there is no selection.
	 */
	public String getSelectedResidues() {
		if (residues_selected && seq != null) {
			return seq.getResidues(sel_range.getStart(), sel_range.getEnd()+1);
		}
		else {
			return "";
		}
	}

	/**
	 * @return the number of the first selected base
	 * or -1 if there is no selection.
	 */
	public int getSelectedStart() {
		if (residues_selected) {
			return sel_range.getStart();
		}
		else {
			return -1;
		}
	}

	/**
	 * @return the number of the last selected base
	 * or -1 if there is no selection.
	 */
	public int getSelectedEnd() {
		if (residues_selected) {
			return sel_range.getEnd();
		}
		else {
			return -1;
		}
	}

	@Override
	public void setSelectionColor(Color col) {
		super.setSelectionColor(col);
		getResidueGlyph().setHighlightColor(col);
	}

	public void setSelectionEvent(int theEvent) {
		residue_map.setSelectionEvent(theEvent);
		sel_behavior = theEvent;
	}

	public int getSelectionEvent() {
		return residue_map.getSelectionEvent();
	}

	/**
	 * Given the desired number of residues per line
	 * and the desired number of lines visible at once,
	 * getPreferredSize() will return the pixel dimension
	 * the NeoSeq needs to be resized to
	 * for the sequence to take up exactly the space it needs,
	 * given that font and other parameters are already set.
	 */
	public Dimension getPreferredSize(int desired_residues_per_line,
			int desired_visible_lines) {

		// Assume we can return null if offset_scroll isn't a Component

		if (!(offset_scroll instanceof Component))
			return null;

		// For x: add needed label width, scrollbar width, and pixel width of
		//        sequence map needed to exactly display desired residues_per_line
		int x = desired_residues_per_line * getResiduePixelWidth();
		x += num_map_pixel_width;
		x += ((Component)offset_scroll).getPreferredSize().width;

		// For y: just calculate how many pixels needed for visible_lines to
		//        exactly fit in sequence map
		int y = desired_visible_lines * getResiduePixelHeight();
		// Add a little extra height
		// so there is space below baseline of last row.
		y += extra_height;

		return new Dimension(x, y);
	}

	/**
	 * Sets the preferred size
	 * in terms of lines of residues
	 * rather than pixel dimentions.
	 */
	public void setPreferredSize(int desired_residues_per_line,
			int desired_visible_lines) {
		preferredWidthInResidues = desired_residues_per_line;
		preferredHeightInLines = desired_visible_lines;
		preferred_size = null;
	}

	/**
	 * Sets the preferred size
	 * in terms of pixel dimentions
	 * rather than lines of residues.
	 */
	@Override
	public void setPreferredSize(Dimension theSize) {
		preferred_size = theSize;
		preferredWidthInResidues = 0;
		preferredHeightInLines = 0;
	}

	@Override
	public Dimension getPreferredSize() {
		if (0 < preferredWidthInResidues * preferredHeightInLines) {
			return getPreferredSize(preferredWidthInResidues, preferredHeightInLines);
		}
		else if (preferred_size != null) {
			return preferred_size;
		}
		else {
			return super.getPreferredSize();
		}
	}

	public int getResiduePixelHeight() {
		return getResidueGlyph().getResidueHeight();
	}

	public int getResiduePixelWidth() {
		return getResidueGlyph().getResidueWidth();
	}

	/**
	 *  returns true if a range of residues is currently selected.
	 */
	public boolean residuesSelected() {
		return residues_selected;
	}

	public void setBackground(int id, Color col) {
		switch (id) {
			case RESIDUES: residue_map.setMapColor(col); break;
			case NUMBERS: num_map.setMapColor(col); break;
			default:
						  throw new IllegalArgumentException("NeoSeq.setBackground() can only " +
								  " accept an id of RESIDUES or NUMBERS");
		}
	}

	public Color getBackground(int id) {
		switch (id) {
			case RESIDUES: return residue_map.getMapColor();
			case NUMBERS: return num_map.getMapColor();
		}
		throw new IllegalArgumentException("NeoSeq.getBackground() can only " +
				" accept an id of RESIDUES or NUMBERS");
	}

	public void setResiduesBackground(Color col) {
		setBackground(RESIDUES, col);
	}

	public Color getResiduesBackground() {
		return getBackground(RESIDUES);
	}

	public void setNumbersBackground(Color col) {
		setBackground(NUMBERS, col);
	}

	public Color getNumbersBackground() {
		return getBackground(NUMBERS);
	}

	public Color getResidueFontColor() {
		return residue_glyph.getColor();
	}

	public void setResidueFontColor(Color col) {
		if ( null != this.caret ) {
			if ( Caret.OUTLINE == ((Caret)this.caret).getFill() ) {
				this.caret.setColor( col );
			}
		}
		residue_glyph.setColor(col);
	}

	public Color getNumberFontColor() {
		return num_glyph.getColor();
	}

	public void setNumberFontColor(Color col) {
		num_glyph.setColor(col);
	}

	/**
	 * Turns display options on or off.
	 *
	 * @param type is one of
	 *      NUCLEOTIDES, COMPLEMENT, FRAME_ONE, FRAME_TWO, FRAME_THREE,
	 *      FRAME_NEG_ONE, FRAME_NEG_TWO, FRAME_NEG_THREE.
	 * @param show true turns the option on. false turns it off.
	 */
	public void setShow(int type, boolean show) {
		if (showAs[type] == show)  { return; }
		showAs[type] = show;
		residue_glyph.setShow(type, show);
		num_glyph.setShow(type, show);
		// calling stretchToFit to force adjustment of vertical scrollbar
		stretchToFit(false,false);
	}
	public void setRevShow(int type, boolean show) {
		if (showAs[type] == show)  { return; }
		showAs[type] = show;
		residue_glyph.setShow(type, show);
		if(type == 1){
		num_glyph.setRevNumbering(show);
//		num_glyph.setShow(type, show);
		}
		// calling stretchToFit to force adjustment of vertical scrollbar
		stretchToFit(false,false);
	}

	/**
	 * gets the state of a display option.
	 *
	 * @param type is one of
	 *      NUCLEOTIDES, COMPLEMENT, FRAME_ONE, FRAME_TWO, FRAME_THREE,
	 *      FRAME_NEG_ONE, FRAME_NEG_TWO, FRAME_NEG_THREE.
	 * @return true if the option is turned on. false if it is off.
	 */
	public boolean getShow(int type) {
		return showAs[type];
	}


	/** Set whether or not the numbering should be displayed descending. */
	public void setRevNumbering (boolean revNums) {
		if (num_glyph == null)
			return;

		num_glyph.setRevNumbering(revNums);
	}

	/** Get whether or not the numbering will be displayed descending. */
	public boolean getRevNumbering () {
		if (num_glyph == null)
			return false;

		return num_glyph.getRevNumbering();
	}



	public void setTranslationStyle(int codetype) {
		if ( this.seq instanceof NASequence ) { // then it is translatable.
			// We should have a more specific interface to test for.
			((NASequence)seq).setTranslationStyle(codetype);
		}
		else {
			System.err.println( "Class " + this.seq.getClass().getName() + " has no translation style." );
			// Should we throw an exception here?
		}
	}

	//************************************************************

	// need to decide what "clearing" the sequence widget actually means
	@Override
	public void clearWidget() {
		clearAnnotations();
		super.clearWidget();

		residue_map.getScene().addGlyph(residue_glyph);
		residue_map.setDataModel(residue_glyph, seq);
		num_map.getScene().addGlyph(num_glyph);
		num_map.setDataModel(num_glyph, seq);

		setResidues("");
	}

	public void clearAnnotations() {
		removeAnnotations(getAnnotationItems(0, this.seq.getLength()));
	}

	/**
	 * Get annotation glyphs that overlap a sequence range.
	 * Note that this method filters out the glyph used for highlighting.
	 *
	 *  @param start   the start of the range to find overlaps
	 *  @param end     the end of the range to find overlaps
	 */
	public List<GlyphI> getAnnotationItems(int start, int end) {
		List<GlyphI> resultVec = new ArrayList<GlyphI>();
		Range sel_range = new Range(start, end);
		Range annot_range = new Range(0,0);

		List seqchildren, annotchildren;
		Object seqchild, annotchild;
		WrapAnnot wrap_annot;
		AnnotationGlyph annot_glyph;
		WrapSequence seq_glyph = getResidueGlyph();
		GlyphI sel_glyph = seq_glyph.getHighlightGlyph();

		seqchildren = getResidueGlyph().getChildren();
		// collect all the WrapAnnot children of the residue glyph
		for (int i=0; i<seqchildren.size(); i++) {
			seqchild = seqchildren.get(i);
			if (seqchild instanceof WrapAnnot) {
				wrap_annot = (WrapAnnot)seqchild;
				annotchildren = wrap_annot.getChildren();
				for (int j=0; j<annotchildren.size(); j++) {
					annotchild = annotchildren.get(j);
					if (annotchild instanceof AnnotationGlyph) {
						if (annotchild == sel_glyph) { continue; }
						annot_glyph = (AnnotationGlyph)annotchild;
						annot_range.beg = annot_glyph.getStart();
						annot_range.end = annot_glyph.getEnd();
						if (sel_range.overlaps(annot_range)) {
							resultVec.add(annot_glyph);
						}
					}
				}
			}
		}
		return resultVec;
	}

	/**
	 * Determines if a Glyph is fully visible and unobscured.
	 * i.e. there are no glyphs on top of this one.
	 *
	 * @param gl  The glyph that is either unobscured or not.
	 * @return  whether or not the glyph is unobscured.
	 */
	public boolean isUnObscured(GlyphI gl) {
		return (isFullyWithinView(gl) && isOnTop(gl));
	}

	/*
	 * For NeoSeq,
	 * we define isOnTop() to depend on what type of glyph is passed as the argument.
	 * WrapFontColors only need to be on top of other WrapFontColors.
	 * WrapColors only need to be on top of other WrapColors.
	 * any other type of glyph currently returns false.
	 */
	public boolean isOnTop(GlyphI glyph) {
		if (!(glyph instanceof AnnotationGlyph)) {
			return false;
		}
		AnnotationGlyph annot = (AnnotationGlyph)glyph;
		Range glyph_range = getAnnotationRange(annot);
		List pickvect = getAnnotationItems(glyph_range.beg, glyph_range.end);
		int start = pickvect.indexOf(annot)+1;
		if (start < 0) { // The glyph itself doesn't show up in pickvect.
			// Something very strange is going on.
			return false;
		}
		if (annot instanceof WrapFontColors) {
			for (int i=start; i<pickvect.size(); i++) {
				if (pickvect.get(i) instanceof WrapFontColors) {
					return false;
				}
			}
			return true;
		}
		else if (annot instanceof WrapColors) {
			for (int i=start; i<pickvect.size(); i++) {
				if (pickvect.get(i) instanceof WrapColors) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 *  Method for determining if a Glyph is fully visible.
	 *
	 *  @param gl The glyph that is either fully visible or not.
	 *  @return whether or not the glyph is fully visible.
	 */
	public boolean isFullyWithinView(GlyphI gl) {
		if ((gl instanceof AnnotationGlyph) && (getWidget(gl) == residue_map)) {
			Range vis_range = getVisibleRange();
			Range glyph_range = getAnnotationRange(gl);
			return glyph_range.within(vis_range);
		}
		else  {
			return false;
		}
	}

	/**
	 *  Method for determining if a Glyph is at least
	 *  partially visible.
	 *
	 *  @param gl The glyph that is either partially visible or not.
	 *  @return whether or not the glyph is at least partially visible.
	 */
	public boolean isPartiallyWithinView(GlyphI gl) {
		if ((gl instanceof AnnotationGlyph) && (getWidget(gl) == residue_map)) {
			Range vis_range = getVisibleRange();
			Range glyph_range = getAnnotationRange(gl);
			return glyph_range.overlaps(vis_range);
		}
		else  {
			return false;
		}
	}

	/**
	 *  Method that gets called when the NeoSeq is added as an
	 *  Obsorver to an Observable.
	 *
	 *  @see java.util.Observable
	 *  @see java.util.Observer
	 */
	public void update(Observable theObserved, Object theArgument) {
		if (theObserved instanceof Selection) {
			update((Selection)theObserved);
		}
	}

	/**
	 *  Private method that updates the selection
	 *  if the Observed object is a Selection
	 *
	 *  @see #update(Observable, Object)
	 *  @see java.util.Observable
	 *  @see java.util.Observer
	 */
	private void update(Selection theSelection) {
		int start = theSelection.getStart();
		int end = theSelection.getEnd();
		if ( theSelection.isEmpty() ) {
			highlightResidues(-1, -1);
			residues_selected = false;
		}
		else
			highlightResidues(start, end);

		/* It would be nice if we could automatically scroll this NeoSeq
		 * to ensure that the selection was visible.
		 * This is useful when the selection is being changed in another widget
		 * but this widget is "listening" to that selection.
		 * To this end we had the instruction:

		 scrollSequence((start + end) / 2);

		 * However, that interfered with automatic scrolling via a DragMonitor.
		 * So it is commented out until we can figure out how to reconcile
		 * these two conflicting demands.
		 */

	}

	/**
	 * Return the JScrollBar responsible for scrolling in the NeoSeqI.
	 */
	public Adjustable getScroller() {
		return offset_scroll;
	}

	/**
	 * Set the adjustable responsible for scrolling in the NeoSeqI.  If
	 * the given Adjustable isn't an instance of Component, the call
	 * will be ignored.
	 */
	public void setScroller(JScrollBar scroller) {

		if (!(scroller instanceof Component) || (scroller == null))
			return;

		remove((Component)offset_scroll);
		offset_scroll = scroller;
		add ((Component)scroller);

		residue_map.setOffsetScroller(offset_scroll);
		num_map.setOffsetScroller(offset_scroll);
	}

	/**
	 * method that listens for NeoDragEvents.
	 * This gets called only when dragging beyond the visible boundaries.
	 * It is used to scroll the widget by dragging to extend the selection.
	 *
	 * @param evt the NeoDragEvent to which the NeoSeq should respond.
	 *
	 * @see com.affymetrix.genoviz.event.NeoDragEvent
	 */
	public void heardDragEvent(NeoDragEvent evt) {
		Object src = evt.getSource();
		if (!drag_scrolling_enabled || (src != residue_drag_monitor)) { return; }
		int direction = evt.getDirection();
		if (direction != NeoConstants.NORTH && direction != NeoConstants.SOUTH) { return; }
		Rectangle2D.Double mbox = residue_map.getCoordBounds();
		Rectangle2D.Double vbox = residue_map.getViewBounds();
		int coord_to_scroll;

		if (direction == NeoConstants.NORTH) {
			coord_to_scroll = (int)(vbox.y - getResiduesPerLine());
			if (coord_to_scroll < mbox.y) {
				coord_to_scroll = (int)mbox.y;
			}

			scroll(Y, coord_to_scroll);
			int sel_cursor = sel_range.getStart() - getResiduesPerLine();

			if (sel_cursor < 0) sel_cursor = 0;

			sel_range.update(sel_cursor);
			sel_range.notifyObservers();
			updateWidget();
		}
		else if (direction == NeoConstants.SOUTH) {
			coord_to_scroll = (int)(vbox.y + getResiduesPerLine());
			if ((coord_to_scroll + vbox.height) > (mbox.height)) {
				return;
			}
			scroll(Y, coord_to_scroll);
			int sel_cursor = sel_range.getEnd() + getResiduesPerLine();

			if (sel_cursor > this.seq.getLength())
				sel_cursor = this.seq.getLength();

			sel_range.update(sel_cursor);
			sel_range.notifyObservers();

			updateWidget();
		}
	}

	/**
	 * enables/disbles scrolling of the NeoSeq when
	 * the user drags off the top or bottom of the NeoSeq.
	 *
	 * @param enable  whether drag scrolling should be on or off.
	 */
	public void enableDragScrolling(boolean enable) {
		drag_scrolling_enabled = enable;
		if (drag_scrolling_enabled) { // drag scrolling turned on
			if (residue_drag_monitor != null) {
				residue_drag_monitor.removeDragListener(this);
			}
			// DragMonitor constructor also adds itself as listener to canvas
			residue_drag_monitor = new DragMonitor(residue_canvas);
			residue_drag_monitor.addDragListener(this);
		}
		else {  // drag scrolling turned off
			if (residue_drag_monitor != null) {
				residue_drag_monitor.removeDragListener(this);
			}
			residue_drag_monitor = null;
		}
	}

	public void viewBoxChanged(NeoViewBoxChangeEvent evt) {
		if (range_listeners.size() > 0) {
			if (evt.getSource() == residue_map.getView()) {
				// ignore viewbox coordbox, just use the NeoSeq's visible range
				Range r = getVisibleRange();
				NeoRangeEvent nevt = new NeoRangeEvent(this, r.beg, r.end);

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

	/**
	 * trying to refresh the widget when the sequence has changed.
	 */
	private void refreshSequence(Sequence theSeq) {

		this.seq.setResidues(theSeq.getResidues());

		getResidueGlyph().setSequence(this.seq);
		getNumGlyph().setSequence(this.seq);

		residue_map.scrollRange(0);
		residue_map.zoomRange(getResiduePixelWidth());
		num_map.scrollRange(0);

		stretchToFit(false,false);

		// Why is it needed at all? It screws up editing. -- elb 3/16/99

		calcNumPixelWidth();

		updateWidget();
	}

	/**
	 * reacts to the sequence changing.
	 */
	public void sequenceChanged( SequenceEvent e ) {

		Sequence s = (Sequence)e.getSource();

		// This shouldn't change the sequence.
		refreshSequence(s);
		// why doesn't this display changed sequence?
		if ( null != this.caret ) {
			this.caret.setCoords ( 0, 0, residues_per_line, seq_map_size );
		}

	}

	private static int useConstrain(int residues_per_line, double y, double height) {
		return (int) (y + height - (height % residues_per_line) - 1);
	}

}
