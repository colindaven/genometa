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

package com.affymetrix.genoviz.awt;

import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.Adjustable;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * a javax.swing.JSlider that also implements java.awt.Adjustable.
 * This is so that we can use sliders for NeoMap zoomers.
 */
public class AdjustableJSlider extends JSlider implements Adjustable {

	/**
	 * constructs a horizontal slider.
	 */
	public AdjustableJSlider() {
		super();
		setValue( getMinimum() );
	}

	/**
	 * constructs a slider in the given orientation.
	 * The slider will be constructed so that its minimum and maximum
	 * are at the same ends that a scrollbar's of the same orientation are.
	 * This is unlike vertical sliders which differ from vertical scrollbars.
	 * If you want a vertical slider with the minimum at the bottom,
	 * use the <code>setInverted( false )</code> method after construction.
	 * Also like scrollbars and unlike sliders,
	 * the value is initially set to the minimum, not the median.
	 *
	 * @param orientation HORIZONTAL or VERTICAL
	 * @see #setInverted
	 */
	public AdjustableJSlider( int orientation ) {
		super( orientation );
		setInverted ( Adjustable.VERTICAL == orientation );
		setValue( getMinimum() );
	}

	private int unitIncrement = 1;

	/**
	 * Not used.
	 * This is not useful for a slider
	 * but is needed to implement Adjustable.
	 * @return 1;
	 */
	public final int getUnitIncrement() {return unitIncrement;}

	/**
	 * Not used.
	 * This is not useful for a slider
	 * but is needed to implement Adjustable.
	 */
	public final void setUnitIncrement(int  v) {/*this.unitIncrement = v;*/}

	private int blockIncrement = 1;

	/**
	 * Not used.
	 * This is not useful for a slider
	 * but is needed to implement Adjustable.
	 * @return 1;
	 */
	public final int getBlockIncrement() {return blockIncrement;}

	/**
	 * Not used.
	 * This is not useful for a slider
	 * but is needed to implement Adjustable.
	 */
	public final void setBlockIncrement(int  v) {/*this.blockIncrement = v;*/}

	/**
	 * For compatibility with Adjustable.
	 * @return the extent.
	 */
	public final int getVisibleAmount() {return getExtent();}

	/**
	 * Sets the extent.
	 * For compatibility with Adjustable.
	 * @param v  Value to assign to extent.
	 */
	public final void setVisibleAmount(int  v) {setExtent(v);}


	private CopyOnWriteArraySet<AdjustmentListener> listeners = new CopyOnWriteArraySet<AdjustmentListener>();

	/**
	 * registers a listener for adjustment events.
	 */
	public void addAdjustmentListener(java.awt.event.AdjustmentListener l) {
		this.listeners.add( l );
	}

	/**
	 * cancels a listeners registration.
	 */
	public void removeAdjustmentListener(java.awt.event.AdjustmentListener l) {
		this.listeners.remove( l );
	}

	protected ChangeListener createChangeListener() {
		return new ChangeListener() {
			public void stateChanged( ChangeEvent e ) {
				fireAdjustmentEvent();
			}
		};
	}

	/**
	 * notifies all listeners of a value change.
	 * <p><strong>BUG</strong>: This always creates a TRACK type change
	 * even when min, max, or extent are actually what changed.
	 */
	private void fireAdjustmentEvent() {
		AdjustmentEvent e = new AdjustmentEvent
			( this, AdjustmentEvent.ADJUSTMENT_VALUE_CHANGED, AdjustmentEvent.TRACK, getValue() );
		for (AdjustmentListener l : this.listeners) {
			l.adjustmentValueChanged( e );
		}
	}

}
