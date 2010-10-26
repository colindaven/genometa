/**
 *   Copyright (c) 2001-2006 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.span;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.BioSeq;

public class SimpleSeqSpan implements SeqSpan, Cloneable {
	protected int start;
	protected int end;
	protected BioSeq seq;

	public SimpleSeqSpan(int start, int end, BioSeq seq) {
		this.start = start;
		this.end = end;
		this.seq = seq;
	}

	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	/** 
	 * Using a "real-number" coordinate representation, such that 
	 *   integer numbers fall <em>between</em> bases.  Thus a sequence span 
	 *   covering ACTG would now have for example start = 0, end = 4, with length = 4
	 *   (but still designated A = base 0
	 *                         C = base 1
	 *                         G = base 2
	 *                         T = base 3)
	 */
	public int getLength() {
		return (end > start ? end-start : start-end);
	}

	public BioSeq getBioSeq() {
		return seq;
	}

	public int getMin() {
		return (start < end ? start : end);
	}

	public int getMax() {
		return (end > start ? end : start);
	}

	public boolean isForward() {
		return (end >= start);
	}

	public double getStartDouble() {
		return (double)start;
	}

	public double getEndDouble() {
		return (double)end;
	}

	public double getMinDouble() {
		return (double)getMin();
	}

	public double getMaxDouble() {
		return (double)getMax();
	}

	public double getLengthDouble() {
		return (double)getLength();
	}

	public boolean isIntegral() { return true; }


}




