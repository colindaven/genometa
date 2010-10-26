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
import com.affymetrix.genoviz.util.DNAUtils;

/**
 * a called base and the quality of the call.
 */
public class BaseConfidence implements BaseCall {

	/** letter code for the base called. */
	protected char base;
	/** the confidence of the call. */
	protected int conf;
	/** the point in the trace where this base was called. */
	private int point;

	/**
	 * constructs a base confidence with the given base and confidence
	 * at the given point.
	 */
	public BaseConfidence( char base, int conf, int point ) {
		this.base = base;
		this.conf = conf;
		this.point = point;
	}

	/**
	 * constructs a base confidence with the given base and confidence.
	 */
	public BaseConfidence ( char base, int conf ) {
		this( Character.toUpperCase( base ), conf, 0 );
	}

	public void setBase(char theBase) {
		this.base = theBase;
	}
	public char getBase() {
		return this.base;
	}

	public void setConfidence(int theConfidence) {
		this.conf = theConfidence;
	}
	public int getConfidence() {
		return conf;
	}

	public int getTracePoint() {
		return this.point;
	}

	/**
	 * Returns the complement of this called base.
	 * Requires the number of sample points in the trace,
	 *    since the complement's peak index will be num_samples - this.peak
	 */
	public BaseCall reverseComplement ( int trace_length ) {
		char rev_base = DNAUtils.complementChar(base);
		int rev_peak = trace_length - point - 1;

		return new BaseConfidence(rev_base, conf, rev_peak);
	}

	public String toString() {
		return "BaseCall[base: " + base + " conf: +" + conf + " point: " + point;
	}

}
