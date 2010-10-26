/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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

import java.util.*;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSpan;

public abstract class SimpleSeqSymmetry implements SeqSymmetry {

	protected List<SeqSpan> spans;
	protected List<SeqSymmetry> children = null;

	public SeqSpan getSpan(BioSeq seq) {
		if (spans == null) {
			return null;
		}
		for (SeqSpan span : spans) {
			if (span.getBioSeq() == seq) {
				return span;
			}
		}
		return null;
	}

	public int getSpanCount() {
		if (spans == null) { return 0; }
		else  { return spans.size(); }
	}

	public SeqSpan getSpan(int i) {
		return spans.get(i);
	}

	public BioSeq getSpanSeq(int i) {
		SeqSpan sp = getSpan(i);
		if (null != sp) { return sp.getBioSeq(); }
		return null;
	}

	public boolean getSpan(int index, MutableSeqSpan span) {
		SeqSpan vspan = spans.get(index);
		span.set(vspan.getStart(), vspan.getEnd(), vspan.getBioSeq());
		return true;
	}

	public boolean getSpan(BioSeq seq, MutableSeqSpan span) {
		if (spans == null) {
			return false;
		}
		for (SeqSpan vspan : spans) {
			if (vspan.getBioSeq() == seq) {
				span.set(vspan.getStart(), vspan.getEnd(), vspan.getBioSeq());
				return true;
			}
		}
		return false;
	}

	public int getChildCount() {
		if (null != children)
			return children.size();
		else
			return 0;
	}

	public SeqSymmetry getChild(int index) {
		if (null != children)
			return children.get(index);
		else
			return null;
	}

	public String getID() { return null; }

	/** Allows subclasses direct access to the children list. */
	protected List<SeqSymmetry> getChildren() {
		return children;
	}

}

