package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.SeqSpan;
import java.io.Serializable;
import java.util.Comparator;

/**
 *  Orders SeqSpan objects first by {@link SeqSpan#getMin()},
 *  then by {@link SeqSpan#getMax()}, regardless of the span orientations.
 */
public final class SeqSpanComparator implements Comparator<SeqSpan>, Serializable {
	public static final long serialVersionUID = 1l;

	public int compare(SeqSpan span1, SeqSpan span2) {
		return compareSpans(span1, span2);
	}

	static int compareSpans(SeqSpan span1, SeqSpan span2) {
		final int min1 = span1.getMin();
		final int min2 = span2.getMin();
		if (min1 != min2) {
			return ((Integer) min1).compareTo(min2);
		}
		// secondary sort by max
		return ((Integer) span1.getMax()).compareTo(span2.getMax());
	}
}
