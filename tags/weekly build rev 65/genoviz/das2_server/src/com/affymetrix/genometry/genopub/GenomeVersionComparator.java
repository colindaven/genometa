package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

public class GenomeVersionComparator implements Comparator<GenomeVersion>, Serializable {
	public int compare(GenomeVersion v1, GenomeVersion v2) {
		if (v1.getBuildDate() != null && v2.getBuildDate() != null) {
			return v2.getBuildDate().compareTo(v1.getBuildDate());
		} else if (v1.getBuildDate() != null) {
			return 1;
		} else if (v2.getBuildDate() != null) {
			return 2;
		} else {
			return v1.getName().compareTo(v2.getName());
		}

	}
}
