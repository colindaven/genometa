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

import com.affymetrix.genometryImpl.symmetry.LeafSingletonSymmetry;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;

import java.util.*;

/**
 * A symmetry containing only a single span.
 * In other words, it has a "breadth" of one.
 */
public class SingletonSeqSymmetry extends LeafSingletonSymmetry  implements SeqSymmetry {

	protected List<SeqSymmetry> children;

	public SingletonSeqSymmetry(SeqSpan span) {
		super(span);
	}

	public SingletonSeqSymmetry(int start, int end, BioSeq seq) {
		super(start, end, seq);
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

}
