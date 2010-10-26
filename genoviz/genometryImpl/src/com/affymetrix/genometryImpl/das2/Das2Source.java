/**
*   Copyright (c) 2001-2005 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.genometryImpl.das2;

import java.net.*;
import java.util.*;


/**
 *
 * started with com.affymetrix.igb.das.DasSource and modified
 */
public final class Das2Source {

  private final URI source_uri;
  private final String name;
  private final Map<String,Das2VersionedSource> versions = new LinkedHashMap<String,Das2VersionedSource>();
 
  private final Das2ServerInfo server;


  public Das2Source(Das2ServerInfo source_server, URI src_uri, String source_name) {
    //    System.out.println("source: name = " + source_name + ", uri = " + src_uri);
    source_uri = src_uri;
    server = source_server;
    name = source_name;
  }

  public String getID() { return source_uri.toString(); }
  public String getName() { return name; }
  @Override
  public String toString() { return getName(); }

  Das2ServerInfo getServerInfo() { return server; }

  /** returns Map of version ID to Das2VersionedSource */
  public synchronized Map<String,Das2VersionedSource> getVersions() {
    return versions;
  }

  synchronized void addVersion(Das2VersionedSource version) {
    versions.put(version.getID(), version);
  }


}
