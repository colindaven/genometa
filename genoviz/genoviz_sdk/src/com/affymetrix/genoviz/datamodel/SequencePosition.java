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

package com.affymetrix.genoviz.datamodel;

import java.util.concurrent.CopyOnWriteArraySet;

/**
 * tracks position within a sequence.
 */
public class SequencePosition implements Position {

	private int offset = 0;

	public int getOffset() {
		return this.offset;
	}

	/**
	 * sets the position.
	 *
	 * @param theValue the number of characters before the position.
	 */
	public void setOffset(int theValue) {
		if ( theValue == this.offset ) { // it was already thus.
			return;
		}
		if ( theValue < 0 ) {
			throw new IllegalArgumentException
				( "Offsets must not be negative." );
		}
		this.offset = theValue;
		notifyListeners();
	}

	private final CopyOnWriteArraySet<PositionListener> listeners = new CopyOnWriteArraySet<PositionListener>();
	public void addListener( PositionListener l ) {
		this.listeners.add( l );
	}
	public void removeListener( PositionListener l ) {
		this.listeners.remove( l );
	}
	private void notifyListeners() {
		for (PositionListener l : this.listeners) {
			l.positionChanged( this, this.offset );
		}
	}

	/**
	 * for debugging
	 */
	public String toString() {
		return String.valueOf( this.offset );
	}

}
