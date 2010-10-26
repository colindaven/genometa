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
 * models an alignment of a span along one sequence (the subject)
 * with a span of equal length along another sequence (the reference)
 */
public class Span {

	/** index of the first residue in the subject sequence. */
	public int seq_start;
	/** index of the last residue in the subject sequence. */
	public int seq_end;
	/** index of the first residue in the reference sequence. */
	public int ref_start;
	/** index of the last residue in the reference sequence. */
	public int ref_end;

	/**
	 * constructs a Span.
	 *
	 * @param a index of the first residue in the subject sequence.
	 * @param b index of the last residue in the subject sequence.
	 * @param c index of the first residue in the reference sequence.
	 * @param d index of the last residue in the reference sequence.
	 */
	public Span ( int a, int b, int c, int d ) {
		seq_start = a;
		seq_end = b;
		ref_start = c;
		ref_end = d;
		return;
	}

	/** @return a string representation of the Span. */
	public String toString() {
		return "Span:  seq_start = " + seq_start + ", seq_end = " + seq_end +
			", ref_start = " + ref_start + ", ref_end = " + ref_end;
	}

}
