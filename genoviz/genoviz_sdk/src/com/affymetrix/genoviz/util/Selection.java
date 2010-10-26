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

package com.affymetrix.genoviz.util;

import java.util.Observable;

/**
 * models a selection of a range of integers.
 */
public class Selection extends Observable {

	private int sel_orig;
	private int sel_start;
	private int sel_end;

	private static final int ORIENTATION_NONE = 0;
	private static final int ORIENTATION_POINT = 1;
	private static final int ORIENTATION_FORWARD = 2;
	private static final int ORIENTATION_REVERSE = 3;

	private int sel_orientation = ORIENTATION_NONE;

	/**
	 * creates a new, empty selection.
	 */
	public Selection() {
		sel_orientation = ORIENTATION_NONE;
	}

	/**
	 * @return true iff no integers are selected.
	 */
	public final boolean isEmpty() {
		return (ORIENTATION_NONE == this.sel_orientation);
	}
	/**
	 * sets this range's emptiness.
	 * Unlike <code>clear()</code>,
	 * <code>setEmpty( true )</code> will reinstate the previous selection.
	 */
	public void setEmpty( boolean empty ) {
		int orientation = sel_orientation;
		if ( empty ) {
			orientation = ORIENTATION_NONE;
		}
		else {
			if (sel_start < sel_end) {
				orientation = ORIENTATION_FORWARD;
			}
			else if (sel_end < sel_start) {
				orientation = ORIENTATION_REVERSE;
			}
			else {
				orientation = ORIENTATION_POINT;
			}
		}
		if ( this.sel_orientation != orientation ) {
			this.sel_orientation = orientation;
			setChanged();
		}
	}

	/**
	 * @return the smallest integer selected.
	 */
	public int getStart() {
		return sel_start;
	}

	/**
	 * @return the largest integer selected.
	 */
	public int getEnd() {
		return sel_end;
	}

	/**
	 * clears the selection.
	 *
	 * <em>Note:
	 * getStart() and getEnd() will return 0 after a call here.
	 * That is like having zero selected.
	 * Of course, that is the current situation before
	 * update() or setRange() are called.
	 * So you must call isEmpty()
	 * to be sure that zero is really selected.</em>
	 */
	public void clear() {
		if (this.isEmpty())
			return; // It was already thus.
		setChanged();
		sel_orientation = ORIENTATION_NONE;
		sel_orig = sel_start = sel_end = 0;
	}

	/**
	 * selectes a single integer.
	 * It is equivalent to setRange(point, point).
	 *
	 * @param point the integer to select.
	 */
	public void setPoint(int point) {
		if (sel_orientation == ORIENTATION_POINT
				&& point == sel_orig
				&& !isEmpty())
			return; // It was already thus.
		setRange(point, point);
	}

	/**
	 * sets the selection range from start to end inclusive.
	 *
	 * @param start the first selected integer.
	 * @param end the last selected integer.
	 * @see #update
	 */
	public void setRange(int start, int end) {

		if ( start <= end && start == sel_start && end == sel_end && !isEmpty() )
			return; // It was already thus.
		if ( end < start && end == sel_start && start == sel_end && !isEmpty() )
			return; // It was already thus.

		setChanged();

		sel_orig = start;
		sel_start = start;
		sel_end = end;

		if ( sel_start < sel_end ) {
			sel_orientation = ORIENTATION_FORWARD;
		}
		else if ( sel_start == sel_end ) {
			sel_orientation = ORIENTATION_POINT;
		}
		else {
			// swap start and end to match update()
			sel_end = start;
			sel_start = end;
			sel_orientation = ORIENTATION_REVERSE;
		}

	}

	/**
	 * updates the selection
	 * moving whichever end was not the origin
	 * to the given value.
	 *
	 * <p> Note that if the update value is less than the origin
	 * the origin will no longer be included in the selection.
	 * c.f. <code>setRange</code>.
	 * If <code>end</code> &lt; <code>start</code> then
	 * the origin will be set to <code>start</code> and included.
	 *
	 * @param value the new end (or start) point.
	 * @see #setRange
	 */
	public void update(int value) {

		setChanged();

		if ( this.isEmpty() ) {
			sel_orig = value;
		}

		if ( sel_orig < value ) {
			sel_start = sel_orig;
			sel_end = value;
			sel_orientation = ORIENTATION_FORWARD;
		}
		else if ( sel_orig == value ) {
			sel_start = value;
			sel_end = value;
			sel_orientation = ORIENTATION_POINT;
		}
		else {
			sel_start = value;
			sel_end = sel_orig-1;
			sel_orientation = ORIENTATION_REVERSE;
		}

	}

}
