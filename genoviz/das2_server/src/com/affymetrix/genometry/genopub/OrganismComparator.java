package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

public class OrganismComparator implements Comparator<Organism>, Serializable {
	public int compare(Organism org1, Organism org2) {
		return org1.getBinomialName().compareTo(org2.getBinomialName());	
	}
}
