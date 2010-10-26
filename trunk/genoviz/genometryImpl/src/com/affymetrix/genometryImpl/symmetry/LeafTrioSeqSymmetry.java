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

package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;

/** A SeqSymmetry with exactly three SeqSpan's. */
public final class LeafTrioSeqSymmetry implements SeqSymmetry {
	private int startA, startB, endA, endB, startC, endC;
	private BioSeq seqA, seqB, seqC;

	public LeafTrioSeqSymmetry(int startA, int endA, BioSeq seqA, 
			int startB, int endB, BioSeq seqB, 
			int startC, int endC, BioSeq seqC) {
		this.startA = startA;
		this.startB = startB;
		this.startC = startC;
		this.endA = endA;
		this.endB = endB;
		this.endC = endC;
		this.seqA = seqA;
		this.seqB = seqB;
		this.seqC = seqC;
	}

	public SeqSpan getSpan(BioSeq seq) {
		if (seqA == seq) { return new SimpleSeqSpan(startA, endA, seqA); }
		else if (seqB == seq) { return new SimpleSeqSpan(startB, endB, seqB); }
		else if (seqC == seq) { return new SimpleSeqSpan(startC, endC, seqC); }
		return null;
	}

	public int getSpanCount() {
		return 3;
	}

	public SeqSpan getSpan(int i) {
		if (i == 0) { return new SimpleSeqSpan(startA, endA, seqA); }
		else if (i == 1) { return new SimpleSeqSpan(startB, endB, seqB);  }
		else if (i == 2) { return new SimpleSeqSpan(startC, endC, seqC);  }
		else { return null; }
	}

	public BioSeq getSpanSeq(int i) {
		if (i == 0) { return seqA; }
		else if (i == 1) { return seqB;  }
		else if (i == 2) { return seqC;  }
		else { return null; }
	}

	public boolean getSpan(BioSeq seq, MutableSeqSpan span) {
		if (seqA == seq) {  span.set(startA, endA, seqA); }
		else if (seqB == seq) { span.set(startB, endB, seqB); }
		else if (seqC == seq) { span.set(startC, endC, seqC); }
		else { return false; }
		return true;
	}

	public boolean getSpan(int index, MutableSeqSpan span) {
		if (index == 0) { span.set(startA, endA, seqA); }
		else if (index == 1) { span.set(startB, endB, seqB); }
		else if (index == 2) { span.set(startC, endC, seqC); }
		else { return false; }
		return true;
	}

	public int getChildCount() { return 0; }

	public SeqSymmetry getChild(int index) { return null; }

	public String getID() { return null; }
}


