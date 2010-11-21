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

/**
 * models a range of integers.
 * It is a set of all integers between and including two integers,
 * a beginning and an end.
 */
public class Range implements Cloneable {

	/** the first (least) integer in the range. */
	public int beg;
	/** the last (greatest) integer in the range. */
	public int end;

	/**
	 * constructs a range of integers.
	 *
	 * @param beg the first (least) integer in the range.
	 * @param end the last (greatest) integer in the range.
	 */
	public Range(int beg, int end) {
		this.beg  = beg;
		this.end = end;
		if (end < beg) {
			throw new IllegalArgumentException("Error in setting up Range. "
					+ "End must be >= beg, beg = " + beg + ", end = " + end);
		}
	}

	/**
	 * Returns a new Range Object with the same
	 * properties as this one.
	 *
	 * @return  new Range Object
	 */
	public Object clone() {
		return new Range(beg, end);
	}

	/**
	 * indicates whether or not two ranges overlap (intersect).
	 *
	 * @param that is another range that might overlap this one.
	 * @return whether or not that overlaps this.
	 */
	public boolean overlaps(Range that) {
		return !(this.end < that.beg  || this.beg > that.end);
	}

	/**
	 * indicates whether or not another range is contained in
	 * (is a subset of) this one.
	 *
	 * @param that is another range that might contain this one.
	 * @return whether or not that contains this.
	 */
	public boolean within(Range that) {
		return (this.beg >= that.beg && this.end <= that.end);
	}

	/**
	 * unites two ranges.
	 * Note that this is not the set theoretic union of the ranges
	 * unless they overlap or abutt.
	 * Such a union would not be a range.
	 * This returns the smallest range
	 * that contains the set theoretic union.
	 *
	 * @param that is another range to unite with this one.
	 * @return the smallest range that contains both this and that.
	 */
	public Range union(Range that) {
		Range result = new Range(0, 0);
		result.beg = (this.beg <= that.beg) ? this.beg : that.beg;
		result.end = (this.end >= that.end) ? this.end : that.end;
		return result;
	}

	/**
	 * creates a new range representing the intersection of two ranges.
	 * <em>Bug:</em> If the two ranges do not overlap,
	 * this will return an invalid range,
	 * whose end is less than its beginning.
	 *
	 * @param that is another range to intersect with this one.
	 * @return the intersection of the this and that.
	 * @see #overlaps
	 */
	public Range intersection(Range that) {
		Range result = new Range(0, 0);
		result.beg = (this.beg > that.beg) ? this.beg : that.beg;
		result.end = (this.end < that.end) ? this.end : that.end;
		return result;
	}

	/** @return a string representing this range. */
	public String toString() {
		return "Range: " + beg + " " + end;
	}

}
