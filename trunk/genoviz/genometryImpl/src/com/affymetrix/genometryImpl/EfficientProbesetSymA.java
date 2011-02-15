/**
 *   Copyright (c) 2005-2007 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.symmetry.LeafSingletonSymmetry;
import java.util.*;

/**
 *   EfficientProbesetSymA is an efficient representation of probesets that meet
 *   certain criteria.
 *   <pre>
 *       a) all probes are same length
 *       b) all probes align to a contiguous genome interval (no split probes)
 *       c) probeset ids can be represented numerically (with an optional prefix string)
 *       d) all probes within a probeset are on same strand
 *       e) probeset must have at least one probe
 *   </pre>
 *
 *   Assumption is that this sym will be child of a sym that handles type, etc.
 */
public final class EfficientProbesetSymA implements SeqSpan, SymWithProps, IntId {
	SharedProbesetInfo info;
	boolean forward;
	int nid;
	int[] child_mins;

	/**
	 * Constructor.
	 * @param  info to provide seq, probe_length, id_prefix (and props?)
	 * @param cmins an array of the minima of the probe positions, this should
	 *   be sorted in ascending order (but will be automatically sorted by this
	 *   routine if this is not the case.  This means that the ordering
	 *   of the elements in the array you pass in may be altered as a side-effect.)
	 * @param forward  true for forward strand
	 * @param nid  an integer to be used as the ID
	 */
	//  public EfficientProbesetSymA(SharedProbesetInfo info, int[] cmins, int probe_length, boolean forward,
	public EfficientProbesetSymA(SharedProbesetInfo info, int[] cmins, boolean forward, int nid) {
		this.info = info;
		this.child_mins = cmins;
		this.forward = forward;
		this.nid = nid;
		java.util.Arrays.sort(this.child_mins);
	}

	/** implementing ParentOfLeafSpan interface */
	public MutableSeqSpan getChildSpan(int child_index, BioSeq aseq, MutableSeqSpan result_span) {
		if ((child_index >= child_mins.length) ||
				(aseq != getBioSeq()) ||
				(result_span == null)) {
			return null;
				}
		if (forward) { result_span.set(child_mins[child_index], child_mins[child_index] + getProbeLength(), aseq); }
		else { result_span.set(child_mins[child_index] + getProbeLength(), child_mins[child_index], aseq); }
		return result_span;
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

	public int getChildCount() { return child_mins.length; }

	public SeqSymmetry getChild(int index) {
		if (index >= getChildCount()) { return null; }
		else  {
			int start, end;
			if (forward) {
				start = child_mins[index];
				end = start + getProbeLength();
			}
			else {
				end = child_mins[index];
				start = end + getProbeLength();
			}
			return new LeafSingletonSymmetry(start, end, this.getBioSeq());
		}
	}

	public int getIntID() { return nid; } // implementing IntId interface
	public int getProbeLength() { return info.getProbeLength(); }
	public String getIDPrefix() { return info.getIDPrefix(); }

	/** The integer id converted to String representation. */
	public String getID() {
		String rootid = Integer.toString(getIntID());
		if (getIDPrefix() == null) {
			return rootid;
		}
		else {
			return (getIDPrefix() + rootid);
		}
	}

	/* SeqSpan implementation */

	public int getStart() {
		// assumes child_mins has been sorted in ascending order
		if (forward)  { return child_mins[0]; }
		else { return (child_mins[child_mins.length-1] + getProbeLength()); }
	}

	public int getEnd() {
		// assumes child_mins has been sorted in ascending order
		if (forward) { return (child_mins[child_mins.length-1] + getProbeLength()); }
		else { return child_mins[0]; }
	}

	public int getMin() {
		// assumes child_mins has been sorted in ascending order
		return child_mins[0];
	}

	public int getMax() {
		// assumes child_mins has been sorted in ascending order
		return (child_mins[child_mins.length-1] + getProbeLength());
	}

	public void setStart(int start) {}

	public void setEnd(int end) {}

	public int getLength() { return (getMax() - getMin()); }
	public boolean isForward() { return forward; }
	public BioSeq getBioSeq() { return info.getBioSeq(); }
	public double getStartDouble() { return (double)getStart(); }
	public double getEndDouble() { return (double)getEnd(); }
	public double getMinDouble() { return (double)getMin(); }
	public double getMaxDouble() { return (double)getMax(); }
	public double getLengthDouble() { return (double)getLength(); }
	public boolean isIntegral() { return true; }

	/**
	 *  WARNING: The implementation of the Propertied (SymWithProps) interface in this class
	 *  is incomplete and is very likely to change or be removed in future implementations.
	 *  Returns a new Map instance with only two values:
	 *  "method" maps to "HuEx-1_0-st-Probes"; and "id" maps to the value of getID().
	 */
	public Map<String,Object> getProperties() {
		HashMap<String,Object> properties = new HashMap<String,Object>(1);
		Map<String,Object> shared_props = info.getProps();
		if (shared_props != null && shared_props.get("method") != null) {
			properties.put("method", (String)shared_props.get("method"));
		}
		properties.put("id", "" + this.getID());
		return properties;
	}

	/** Has no effect, and returns false. */
	public boolean setProperty(String key, Object val) {
		return false;
	}

	/** See getProperties(). */
	public Object getProperty(String key) {
		Map shared_props = info.getProps();
		if ("method".equals(key) && shared_props != null) { return (String)shared_props.get("method"); }
		if ("id".equals(key)) return this.getID();
		else return null;
	}

	/** Returns a clone of the Map from getProperties(). */
	public Map<String,Object> cloneProperties() {
		return getProperties();
	}

}
