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
 * models a single base called by a base-calling or assembly program,
 * like phred and phrap,
 * given a trace from a sequencer.
 */
public class CalledBase implements BaseCall {

	private int peak_index;
	protected int prob_A;
	protected int prob_C;
	protected int prob_G;
	protected int prob_T;
	private char base;

	/**
	 * constructs a CalledBase.
	 * The scores give some indication of the likelyhood
	 * that the base called is really one particular base.
	 * Typically the highest score will be the one that corresponds
	 * to the base actually called.
	 *
	 * @param peak_index where the peak is found in the trace.
	 * @param prob_A adenine score.
	 * @param prob_C cytosine score.
	 * @param prob_G guanine score.
	 * @param prob_T thymine score.
	 * @param base the base called.
	 */
	public CalledBase ( int peak_index,
			int prob_A, int prob_C, int prob_G, int prob_T,
			char base ) {

		this.peak_index = peak_index;
		this.prob_A = prob_A;
		this.prob_C = prob_C;
		this.prob_G = prob_G;
		this.prob_T = prob_T;
		this.base = base;
		return;
	}

	/**
	 * @return the index into the trace where the peak is found.
	 * @deprecated use getTracePoint()
	 */
	@Deprecated
		public final int getPeakIndex () { return getTracePoint(); }
	public final int getTracePoint() { return peak_index; }

	/** @return adenine score. */
	public final int getProbA () { return prob_A; }
	/** @return cytosine score. */
	public final int getProbC () { return prob_C; }
	/** @return guanine score. */
	public final int getProvG () { return prob_G; }
	/** @return thymine score. */
	public final int getProbT () { return prob_T; }
	/** @return the base called. */
	public final char getBase() { return base; }

	/**
	 * produces a complement of this called base.
	 * This includes the probabilities being complemented
	 * and the trace point being complemented (reversed).
	 *
	 * @param trace_length the number of sample points in the trace.
	 * The complement's peak index will be trace_length - this.peak.
	 * @return a new base call which is the complement of this called base.
	 */
	public BaseCall reverseComplement( int trace_length ) {
		char rev_base = DNAUtils.complementChar(base);
		int rev_peak = trace_length - peak_index - 1;

		int p = prob_A;
		prob_A = prob_T;
		prob_T = p;
		p = prob_C;
		prob_C = prob_G;
		prob_G = p;

		return new CalledBase(rev_peak, prob_T, prob_G,
				prob_C, prob_A, rev_base);
	}

	/**
	 * @return a string representing this CalledBase.
	 */
	public String toString() {
		return ("CalledBase: " + base + " at " + peak_index);
	}

}
