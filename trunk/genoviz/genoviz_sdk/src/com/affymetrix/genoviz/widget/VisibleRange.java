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

import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Keeps track of a visible range.
 * Using it to implement a hairline.
 * {@link NeoRangeListener}s can listen for range changes.
 */
public class VisibleRange implements Cloneable {

	private double beginning;
	private double end;
	private Set<NeoRangeListener> listeners = new CopyOnWriteArraySet<NeoRangeListener>();
	private boolean changed = false;
	private boolean reversed = false;

	public VisibleRange() {
	}

	public VisibleRange(double spot) {
		setSpot(spot);
	}

	public VisibleRange(double beg, double end) {
		setBeginning(beg);
		setEnd(end);
	}

	public VisibleRange(double beg, double end, boolean reversed) {
		setBeginning(beg);
		setEnd(end);
		setReversed(reversed);
	}

	/**
	 * creates a copy of a VisibleRange.
	 * @return a copy of this object,
	 * but with a new (empty) set of listeners.
	 */
	public Object clone() {
		Object o = null;
		try {
			o = super.clone();
			VisibleRange vr = ( VisibleRange ) o;
			vr.listeners = new CopyOnWriteArraySet<NeoRangeListener>();
			vr.changed = false;
		} catch ( CloneNotSupportedException e ) {
		}
		return o;
	}

	public boolean debug = false;
	public void debug() { debug = true; }

	/**
	 * sets the visible range to include the integral interval around a point.
	 * This can be useful for selecting the part of a map around a single residue in a sequence.
	 * It also automatically notifies all listeners (if changed).
	 *
	 * @param thePlace a number in the interval.
	 * i.e. the interval <code>(n, n+1)</code> is selected where <code>n &gt;= thePlace &lt;= n+1</code>.
	 */
	public void setSpot( double thePlace ) {
		if ( debug ) new RuntimeException ("setSpot in VisibleRange: " + (int)thePlace).printStackTrace();
		double before = Math.floor( thePlace );
		setBeginning( before );
		double after = Math.ceil( thePlace );
		if ( before == after ) after++;
		setEnd( after );
		notifyListeners();
	}

	/*
	 * calls notifyListeners(true);
	 */

	public void notifyListeners() { notifyListeners(true); }

	/**
	 * Notify all the listeners if this has changed since the last notice,
	 * or if the boolean argument is true.
	 * Make sure that no listeners call this method,
	 * or else you'll end up with an infinite loop, and that would suck.
	 * @param checkIfChanged if this is false, there is no check
	 * to see if there has been a notification since the last change to the value.
	 */
	public void notifyListeners( boolean checkIfChanged ) {
		if ( this.changed |! checkIfChanged ) {
			this.changed = false; // Needs to be done before notifying listeners to avoid possible loops.
			NeoRangeEvent evt = new NeoRangeEvent( this, this.beginning, this.end );
			for (NeoRangeListener l : this.listeners) {
				l.rangeChanged( evt );
			}
		}
	}

	public final void setBeginning( double thePlace ) {
		if ( this.beginning != thePlace ) {
			this.beginning = thePlace;
			this.changed = true;
		}
	}
	public final double getBeginning() {
		return this.beginning;
	}

	public final void setEnd( double thePlace ) {
		if ( this.end != thePlace ) {
			this.end = thePlace;
			this.changed = true;
		}
	}
	public final double getEnd() {
		return this.end;
	}

	/**
	 * setReversed and isReversed are used to track reversed maps, maps whose
	 *   coordinates are the reverse of the datamodel coordinates.
	 */
	public final void setReversed( boolean reversed ) {
		if (this.reversed != reversed) {
			this.reversed = reversed;
			this.changed = true;
		}
	}
	public final boolean isReversed() {
		return this.reversed;
	}

	public void addListener( NeoRangeListener theListener ) {
		this.listeners.add( theListener );
		NeoRangeEvent evt = new NeoRangeEvent( this, this.beginning, this.end) ;
		theListener.rangeChanged(evt);
	}

	public void removeListener( NeoRangeListener theListener ) {
		if ( null != this.listeners && null != theListener ) {
			this.listeners.remove( theListener );
		}
	}

	@Override
	public String toString() {
		return this.getClass().getName() + "[" + getBeginning() + ", " + getEnd() + "]";
	}

}
