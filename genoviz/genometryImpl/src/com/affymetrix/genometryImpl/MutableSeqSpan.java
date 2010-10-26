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


public interface MutableSeqSpan extends SeqSpan  {

	public void set(int start, int end, BioSeq seq);
	public void setCoords(int start, int end);
	public void setStart(int start);
	public void setEnd(int end);

	/** Extends the range of coordinates to include this range, but does not
	 *  change the orientation of this SeqSpan.
	 */
	//public void stretchSpan(int start, int end);
	public void setBioSeq(BioSeq seq);

	public void setStartDouble(double start);
	public void setEndDouble(double end);
	//public void setCoordsDouble(double start, double end);
	public void setDouble(double start, double end, BioSeq seq);
}
