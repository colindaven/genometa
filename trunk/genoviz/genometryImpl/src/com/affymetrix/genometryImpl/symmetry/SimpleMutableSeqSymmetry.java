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

import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;

public class SimpleMutableSeqSymmetry extends SimpleSeqSymmetry implements MutableSeqSymmetry {

	public SimpleMutableSeqSymmetry() {
		super();
	}

	public void addSpan(SeqSpan span) {
		if (spans == null) {
			spans = new ArrayList<SeqSpan>();
		}
		spans.add(span);
	}

	public void removeSpan(SeqSpan span) {
		if (spans != null) {
			spans.remove(span);
		}
	}

	/*public void setSpan(int index, SeqSpan span) {
		if (spans == null) {
			spans = new ArrayList<SeqSpan>();
		}
		spans.set(index, span);
	}*/

	public void addChild(SeqSymmetry sym) {
		if (children == null) {
			children = new ArrayList<SeqSymmetry>();
		}
		children.add(sym);
	}

	public void removeChild(SeqSymmetry sym) {
		children.remove(sym);
	}

	public SeqSymmetry getChild(int index) {
		if ((children == null) || (index >= children.size())) { return null; }
		else { return children.get(index); }
	}

	public int getChildCount() {
		if (children == null) { return 0; }
		else { return children.size(); }
	}

	public void removeChildren() { children = null; }

	public void removeSpans() { spans = null; }

	public void clear() {
		removeChildren();
		removeSpans();
	}

}
