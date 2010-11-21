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

package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import java.util.*;


/**
 *  Holds a reference to a "parent" symmetry and an index within it.
 *  All requests for properties or scores return those of the parent, and
 *  will throw exceptions if the parent is null.
 */
public final class IndexedSingletonSym extends SingletonSeqSymmetry implements IndexedSym, SymWithProps {
	private int index_in_parent = -1;
	private ScoredContainerSym parent = null;
	private String id = null;

	/** Constructor. Be sure to also call {@link #setParent} and {@link #setIndex}.
	*/
	public IndexedSingletonSym(int start, int end, BioSeq seq)  {
		super(start, end, seq);
	}

	public void setParent(ScoredContainerSym par) { parent = par; }
	public void setIndex(int index) { index_in_parent = index; }
	public ScoredContainerSym getParent() { return parent; }
	public int getIndex() { return index_in_parent; }
	public void setID(String symid) { id = symid; }
	public String getID() { return id; }

	public Map<String,Object> getProperties() {
		Map<String,Object> props;
		if (id != null) {
			props = cloneProperties();
		}
		else {
			props = parent.getProperties();
		}
		return props;
	}

	public Map<String,Object> cloneProperties() {
		Map<String,Object> props = parent.cloneProperties();
		if (id != null) {
			props.put("id", id);
		}
		return props;
	}

	public Object getProperty(String key) {
		if (key.equals("id")) { return id; }
		else { return parent.getProperty(key); }
	}

	/** IndexedSingletonSym does not support setting properties, so this will
	 *  return false.
	 */
	public boolean setProperty(String key, Object val) {
		if (key.equals("id")) {
			setID((String)val);
			return true;
		}
		else  {
			System.err.println("IndexedSingletonSym does not support setting properties, except for id");
			return false;
		}
	}
}
