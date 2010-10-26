package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

 public class SegmentComparator implements Comparator<Segment>, Serializable {
    public int compare(Segment s1, Segment s2) {
      return s1.getIdSegment().compareTo(s2.getIdSegment());
    }
  }
