package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

public class AnnotationComparator implements Comparator<Annotation>, Serializable {
	public int compare(Annotation a1, Annotation a2) {
		return a1.getIdAnnotation().compareTo(a2.getIdAnnotation());
	}
}
