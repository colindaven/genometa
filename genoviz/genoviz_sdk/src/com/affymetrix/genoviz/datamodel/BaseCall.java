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
 * An interface to data modeling a base call from a trace.
 * Implementors can be used in the NeoTracer to display base calls
 * below a trace.
 */
public interface BaseCall {

	/**
	 * @return a character code for the called base.
	 */
	public char getBase();

	/**
	 * a pointer to the sample point in the trace where this base was called.
	 */
	public int getTracePoint();

	/**
	 * @param trace_length is needed for reversing position
	 * @return new BaseCall with complemented base, and reverse position
	 */
	public BaseCall reverseComplement( int trace_length );

}
