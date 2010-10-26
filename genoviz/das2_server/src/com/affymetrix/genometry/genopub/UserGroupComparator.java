package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

 public class UserGroupComparator implements Comparator<UserGroup>, Serializable {
    public int compare(UserGroup g1, UserGroup g2) {
      return g1.getIdUserGroup().compareTo(g2.getIdUserGroup());
    }
  }
