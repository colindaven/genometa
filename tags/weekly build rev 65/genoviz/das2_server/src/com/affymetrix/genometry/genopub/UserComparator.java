package com.affymetrix.genometry.genopub;


import java.io.Serializable;
import java.util.Comparator;

 public class UserComparator implements Comparator<User>, Serializable {
    public int compare(User u1, User u2) {
      return u1.getIdUser().compareTo(u2.getIdUser());
    }
  }
