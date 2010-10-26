/**
 *   Copyright (c) 2005 Affymetrix, Inc.
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


public final class EfficientSnpSym implements SeqSymmetry, SeqSpan {
	/**
	 *  from parent can derive id construction, annot type, annot source, etc.
	 */
	SeqSymmetry parent;  // may need to make a SnpParentSym for this...
	int base_coord;
	int numeric_id;

	public EfficientSnpSym(SeqSymmetry sym_parent, int coord) {
		parent = sym_parent;
		base_coord = coord;
	}

	/* SeqSymmetry implementation */

	public SeqSpan getSpan(BioSeq bs) {
		if (this.getBioSeq() == bs) { return this; }
		else { return null; }
	}

	public int getSpanCount() { return 1; }

	public SeqSpan getSpan(int i) {
		if (i == 0) { return this; }
		else { return null; }
	}

	public BioSeq getSpanSeq(int i) {
		if (i == 0) { return this.getBioSeq(); }
		else { return null; }
	}

	public boolean getSpan(BioSeq bs, MutableSeqSpan span) {
		if (this.getBioSeq() == bs) {
			span.setStart(this.getStart());
			span.setEnd(this.getEnd());
			span.setBioSeq(this.getBioSeq());
			return true;
		}
		return false;
	}

	public boolean getSpan(int index, MutableSeqSpan span) {
		if (index == 0) {
			span.setStart(this.getStart());
			span.setEnd(this.getEnd());
			span.setBioSeq(this.getBioSeq());
			return true;
		}
		return false;
	}

	public int getChildCount() { return 0; }
	public SeqSymmetry getChild(int index) { return null; }

	public String getID() {
		return Integer.toString(numeric_id);
	}

	/* SeqSpan implementation */

	public int getStart() { return base_coord; }
	public int getEnd() { return (base_coord + 1); }
	public int getMin() { return base_coord; }
	public int getMax() { return (base_coord + 1); }
	public int getLength() { return 1; }
	public boolean isForward() { return true; }
	public BioSeq getBioSeq() { return parent.getSpanSeq(0); }
	public double getStartDouble() { return (double)getStart(); }
	public double getEndDouble() { return (double)getEnd(); }
	public double getMinDouble() { return (double)getMin(); }
	public double getMaxDouble() { return (double)getMax(); }
	public double getLengthDouble() { return (double)getLength(); }
	public boolean isIntegral() { return true; }


}
