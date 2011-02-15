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

package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.BioSeq;

/**
 * Implementations model a contiguous section of a {@link BioSeq}.
 */
public interface SeqSpan {
	public int getStart();
	public int getEnd();
	public void setStart(int start);
	public void setEnd(int end);
	public int getMin();
	public int getMax();
	public int getLength();
	public boolean isForward();
	public BioSeq getBioSeq();
	// boolean isReverse();
	/*
	   public int getStrand()
	   public static int FORWARD = 0;
	   public static int REVERSE = 1;
	   public static int BOTH = 2;
	   public static int UNKNOWN = 3;
	   */

	public double getStartDouble();
	public double getEndDouble();
	public double getMaxDouble();
	public double getMinDouble();
	public double getLengthDouble();
	public boolean isIntegral();
}

