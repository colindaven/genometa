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
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSpan;

public class SimpleMutableSeqSpan extends SimpleSeqSpan implements MutableSeqSpan {

	public SimpleMutableSeqSpan(int start, int end, BioSeq seq) {
		super(start, end, seq);
	}

	public SimpleMutableSeqSpan(SeqSpan span) {
		this(span.getStart(), span.getEnd(), span.getBioSeq());
	}

	public SimpleMutableSeqSpan()  {
		this(0, 0, null);
	}

	public void set(int start, int end, BioSeq seq) {
		this.start = start;
		this.end = end;
		this.seq = seq;
	}

	public void setCoords(int start, int end) {
		this.start = start;
		this.end = end;
	}

	public void setStart(int start) {
		this.start = start;
	}

	public void setEnd(int end) {
		this.end = end;
	}

	public void setBioSeq(BioSeq seq) {
		this.seq = seq;
	}

	public void setDouble(double start, double end, BioSeq seq) {
		this.start = (int)start;
		this.end = (int)end;
		this.seq = seq;
	}

	/*public void setCoordsDouble(double start, double end) {
		this.start = (int)start;
		this.end = (int)end;
	}*/

	public void setStartDouble(double start) {
		this.start = (int)start;
	}

	public void setEndDouble(double end) {
		this.end = (int)end;
	}
}





