package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import java.util.Comparator;

/**
 *  Sorts SeqSymmetries based on {@link SeqSpan#getStart()}.
 *
 *  @see  SeqSymMinComparator
 */
public final class SeqSymStartComparator implements Comparator<SeqSymmetry> {
	private final boolean ascending;
	private final BioSeq seq;

	/** Constructor.
	 *  @param s  sequence to base the sorting on
	 *  @param b  true to sort ascending, false for descending
	 */
	public SeqSymStartComparator(BioSeq s, boolean b) {
		this.seq = s;
		this.ascending = b;
	}

	public int compare(SeqSymmetry sym1, SeqSymmetry sym2) {
		final SeqSpan span1 = sym1.getSpan(seq);
		final SeqSpan span2 = sym2.getSpan(seq);
		if (ascending) {
			return ((Integer) span1.getStart()).compareTo(span2.getStart());
		}
		return ((Integer) span2.getStart()).compareTo(span1.getStart());
	}
}
