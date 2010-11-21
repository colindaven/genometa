package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.SeqSymmetry;
import java.io.Serializable;
import java.util.Comparator;

/**
 *  Sorts SeqSymmetries based on lexicographic ordering of IDs 
 */
public final class SeqSymIdComparator implements Comparator<SeqSymmetry>, Serializable {
	public static final long serialVersionUID = 1l;

	public int compare(SeqSymmetry sym1, SeqSymmetry sym2) {
		return compareStrings(sym1.getID(), sym2.getID());
	}

	private static int compareStrings(String id1, String id2) {
		if (id1 == null || id2 == null) {
			return compareNullIDs(id1, id2);
		}
		return id1.compareTo(id2);
	}
	
	public static int compareNullIDs(String id1, String id2) {
		if (id1 == null) {
			if (id2 == null) {
				return 0;
			}
			return 1;
		}
		return -1;
	}
}
