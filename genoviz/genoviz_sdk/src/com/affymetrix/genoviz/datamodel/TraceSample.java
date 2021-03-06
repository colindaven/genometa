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
 * models a single point at which a sample is taken of a sequencer's output.
 */
public class TraceSample {

	protected int sample_A;
	protected int sample_C;
	protected int sample_G;
	protected int sample_T;

	public TraceSample ( int sample_A, int sample_C, int sample_G, int sample_T ) {

		this.sample_A = sample_A;
		this.sample_C = sample_C;
		this.sample_G = sample_G;
		this.sample_T = sample_T;

		return;
	}

	public int getSampleA () { return sample_A; }
	public int getSampleC () { return sample_C; }
	public int getSampleG () { return sample_G; }
	public int getSampleT () { return sample_T; }

	/**
	 * @return the complement of this trace sample.
	 * In other words, returns a trace sample with Ts and As switched,
	 * and Gs and Cs switched.
	 */
	public TraceSample complement() {
		return new TraceSample(sample_T, sample_G, sample_C, sample_A);
	}

}
