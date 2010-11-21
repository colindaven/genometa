/**
 *   Copyright (c) 2006 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;

public final class ScoredSingletonSym extends SingletonSeqSymmetry implements Scored {
	float score;

	public ScoredSingletonSym(int start, int end, BioSeq seq, float score) {
		super(start, end, seq);
		this.score = score;
	}

	public float getScore() {
		return score;
	}
}
