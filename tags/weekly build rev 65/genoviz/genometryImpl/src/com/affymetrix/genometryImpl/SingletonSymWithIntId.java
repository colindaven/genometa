/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.symmetry.MutableSingletonSeqSymmetry;


public final class SingletonSymWithIntId extends MutableSingletonSeqSymmetry implements IntId {
	int nid;
	String id_prefix;

	public SingletonSymWithIntId(int start, int end, BioSeq seq, String id_prefix, int nid) {
		super(start, end, seq);
		this.id_prefix = id_prefix;
		this.nid = nid;
	}

	public String getID() {
		String rootid = Integer.toString(getIntID());
		if (id_prefix == null) {
			return rootid;
		}
		else {
			return (id_prefix + rootid);
		}
	}

	public int getIntID() {
		return nid;
	}
}
