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

import com.affymetrix.genometryImpl.symmetry.LeafTrioSeqSymmetry;
import java.io.*;
import java.util.*;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.SeqUtils;

public final class Psl3Sym extends UcscPslSym {
	static int OTHER_INDEX = 2;
	BioSeq otherseq;
	boolean same_other_orientation;  // orientation of "other" spans relative to _query_
	int omin;
	int omax;
	int[] omins;
	boolean overlapping_other_coords = false;

	public Psl3Sym(String type,
			int matches,
			int mismatches,
			int repmatches,
			int ncount,
			int qNumInsert,
			int qBaseInsert,
			int tNumInsert,
			int tBaseInsert,
			boolean same_target_orientation,
			boolean same_other_orientation,
			BioSeq queryseq,
			int qmin,
			int qmax,
			BioSeq targetseq,
			int tmin,
			int tmax,
			BioSeq otherseq,
			int omin,
			int omax,
			int blockcount,  // now ignored, uses blockSizes.length
			int[] blockSizes,
			int[] qmins,
			int[] tmins,
			int[] omins
				) {
					super(type, matches, mismatches, repmatches, ncount, qNumInsert, qBaseInsert,
							tNumInsert, tBaseInsert, same_target_orientation, queryseq, qmin, qmax,
							targetseq, tmin, tmax,
							blockcount, blockSizes, qmins, tmins);
					this.otherseq = otherseq;
					this.omin = omin;
					this.omax = omax;
					this.same_other_orientation = same_other_orientation;
					this.omins = omins;

					//  need to do a check here for whether other coords overlap..
				}

	/** Always returns 3. */
	public int getSpanCount() { return 3; }

	public SeqSpan getSpan(BioSeq bs) {
		if (bs.equals(otherseq)) {
			SeqSpan span = null;
			if (same_other_orientation) {  span = new SimpleSeqSpan(omin, omax, otherseq); }
			else { span = new SimpleSeqSpan(omax, omin, otherseq); }
			return span;
		}
		else { return super.getSpan(bs); }
	}

	public boolean getSpan(BioSeq bs, MutableSeqSpan span) {
		if (bs.equals(otherseq)) {
			if (same_other_orientation) { span.set(omin, omax, otherseq); }
			else { span.set(omax, omin, otherseq); }
			return true;
		}
		else return super.getSpan(bs, span);
	}

	public boolean getSpan(int index, MutableSeqSpan span) {
		if (index == OTHER_INDEX) {
			if (same_other_orientation)  { span.set(omin, omax, otherseq); }
			else { span.set(omax, omin, otherseq); }
			return true;
		}
		else { return super.getSpan(index, span); }
	}

	public SeqSpan getSpan(int index) {
		if (index == OTHER_INDEX) {
			SeqSpan span = null;
			if (same_other_orientation) { span = new SimpleSeqSpan(omin, omax, otherseq); }
			else { span = new SimpleSeqSpan(omax, omin, otherseq); }
			return span;
		}
		else {
			return super.getSpan(index);
		}
	}

	public BioSeq getSpanSeq(int index) {
		if (index == OTHER_INDEX) { return otherseq; }
		else { return super.getSpanSeq(index); }
	}

	public SeqSymmetry getChild(int i) {
		int t1, t2, o1, o2;
		if (same_orientation) {
			t1 = tmins[i];
			t2 = tmins[i]+blockSizes[i];
		}
		else {
			t1 = tmins[i]+blockSizes[i];
			t2 = tmins[i];
		}
		if (same_other_orientation) {
			o1 = omins[i];
			o2 = omins[i]+blockSizes[i];
		}
		else {
			o1 = omins[i]+blockSizes[i];
			o2 = omins[i];
		}
		return new LeafTrioSeqSymmetry(qmins[i], qmins[i]+blockSizes[i], queryseq,
				t1, t2, targetseq,
				o1, o2, otherseq);
	}

	public List<SeqSymmetry> getOverlappingChildren(SeqSpan input_span) {
		if (input_span.getBioSeq() == otherseq) {
			return SeqUtils.getOverlappingChildren(this, input_span);
		}
		else {
			return super.getOverlappingChildren(input_span);
		}
	}

	public BioSeq getOtherSeq() { return otherseq; }
	public int getOtherMin() { return omin; }
	public int getOtherMax() { return omax; }
	public boolean getSameOtherOrientation() { return same_other_orientation; }

	public Map<String,Object> cloneProperties() {
		Map<String,Object> tprops = super.cloneProperties();
		tprops.put("other seq", getOtherSeq().getID());
		tprops.put("same other orientation", getSameOtherOrientation());
		return tprops;
	}

	public void outputPsl3Format(DataOutputStream out) throws IOException {
		outputStandardPsl(out, false);
		if (same_other_orientation) { out.write('+'); }
		else { out.write('-'); }
		out.write('\t');
		out.write(otherseq.getID().getBytes());
		out.write('\t');
		out.write(Integer.toString(otherseq.getLength()).getBytes());
		out.write('\t');
		out.write(Integer.toString(omin).getBytes());
		out.write('\t');
		out.write(Integer.toString(omax).getBytes());
		out.write('\t');
		int blockcount = this.getChildCount();
		for (int i=0; i<blockcount; i++) {
			out.write(Integer.toString(omins[i]).getBytes());
			out.write(',');
		}
		out.write('\t');
		outputPropTagVals(out);
		out.write('\n');
	}
}
