package com.affymetrix.genometryImpl.das2;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Das2Capability {
  private final String type;
  private final URI root_uri;
  private static final Map<String,Das2VersionedSource> cap2version = new LinkedHashMap<String,Das2VersionedSource>();

  Das2Capability(String cap_type, URI cap_root) {
    type = cap_type;
    root_uri = cap_root;
  }

  public static Map<String,Das2VersionedSource> getCapabilityMap() { return cap2version; }
  String getType() { return type; }
  public URI getRootURI() { return root_uri; }

}
