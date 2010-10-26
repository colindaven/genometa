package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.SeqSymmetry;
import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * @author jnicol
 */
/**
 *  Sorts SeqSymmetries based on lexicographic ordering of reversed IDs
 */
public final class SeqSymReverseIdComparator implements Comparator<SeqSymmetry>, Serializable {
	public static final long serialVersionUID = 1l;

	public int compare(SeqSymmetry sym1, SeqSymmetry sym2) {
		return compareReverseStrings(sym1.getID(), sym2.getID());
	}

	private static int compareReverseStrings(String id1, String id2) {
		if (id1 == null || id2 == null) {
			return SeqSymIdComparator.compareNullIDs(id1, id2);
		}
		// reverse id1.
		StringBuffer IDbuffer = new StringBuffer(id1);
		IDbuffer = IDbuffer.reverse();
		String tempID1 = IDbuffer.toString();
		// reverse id2.
		IDbuffer = new StringBuffer(id2);
		IDbuffer = IDbuffer.reverse();
		String tempID2 = IDbuffer.toString();
		
		return tempID1.compareTo(tempID2);
	}
}

