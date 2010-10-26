package com.affymetrix.genometryImpl.comparator;

import java.util.Comparator;
import affymetrix.calvin.data.ProbeSetQuantificationData;

/**
 *  Assumes objects to compare are both ProbeSetQuantificationData objects, and 
 *     they have their integer IDs set
 */
public final class QuantByIntIdComparator implements Comparator<ProbeSetQuantificationData> {
  public int compare(ProbeSetQuantificationData dataA, ProbeSetQuantificationData dataB) {
    int idA = dataA.getId();
    int idB = dataB.getId();
    return Integer.valueOf(idA).compareTo(idB);
  }
}
