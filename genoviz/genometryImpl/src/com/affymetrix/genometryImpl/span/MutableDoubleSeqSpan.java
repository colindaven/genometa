/**
 *   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;

public final class MutableDoubleSeqSpan implements MutableSeqSpan, Cloneable {
	private double start;
	private double end;
	private BioSeq seq;

	public MutableDoubleSeqSpan(double start, double end, BioSeq seq) {
		this.start = start;
		this.end = end;
		this.seq = seq;
	}


	public MutableDoubleSeqSpan()  {
		this(0, 0, null);
	}

	public void set(int start, int end, BioSeq seq) {
		this.start = (double)start;
		this.end = (double)end;
		this.seq = seq;
	}

	public void setCoords(int start, int end) {
		this.start = (double)start;
		this.end = (double)end;
	}

	public void setStart(int start) {
		this.start = (double)start;
	}

	public void setEnd(int end) {
		this.end = (double)end;
	}

	public void setBioSeq(BioSeq seq) {
		this.seq = seq;
	}


	public void setDouble(double start, double end, BioSeq seq) {
		this.start = start;
		this.end = end;
		this.seq = seq;
	}

	public void setStartDouble(double start) {
		this.start = start;
	}

	public void setEndDouble(double end) {
		this.end = end;
	}

	public boolean isIntegral() {
		return false;
	}

	@Override
	public Object clone() throws CloneNotSupportedException {
		return super.clone();
	}

	public int getStart() {
		return (int)start;
	}

	public int getEnd() {
		return (int)end;
	}

	public int getMin() {
		return (int)getMinDouble();
	}

	public int getMax() {
		return (int)getMaxDouble();
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
		double dl = getLengthDouble();
		if (dl > Integer.MAX_VALUE)  {
			return (Integer.MAX_VALUE - 1);
		}
		else  {
			return (int)dl;
		}
	}

	public BioSeq getBioSeq() {
		return seq;
	}

	public boolean isForward() {
		return (end >= start);
	}


	public double getMinDouble() {
		return (start < end ? start : end);
	}

	public double getMaxDouble() {
		return (end > start ? end : start);
	}

	public double getStartDouble() {
		return start;
	}

	public double getEndDouble() {
		return end;
	}

	public double getLengthDouble() {
		return (end > start ? end-start : start-end);
	}

}





