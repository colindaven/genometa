/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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
package com.affymetrix.genometryImpl;

import java.util.*;

public final class SharedProbesetInfo {
	BioSeq seq;
	int probe_length;
	String id_prefix;
	Map<String,Object> props;

	public SharedProbesetInfo(BioSeq seq, int probe_length, String id_prefix, Map<String,Object> props) {
		this.seq = seq;
		this.probe_length = probe_length;
		this.id_prefix = id_prefix;
		this.props = props;
	}

	public BioSeq getBioSeq() { return seq; }
	public int getProbeLength() { return probe_length; }
	public String getIDPrefix() { return id_prefix; }
	public Map<String,Object> getProps() { return props; }

}
