package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

 public class AnnotationGroupingComparator implements Comparator<AnnotationGrouping>, Serializable {
    public int compare(AnnotationGrouping ag1, AnnotationGrouping ag2) {
      return ag1.getIdAnnotationGrouping().compareTo(ag2.getIdAnnotationGrouping());
      
    }
  }
