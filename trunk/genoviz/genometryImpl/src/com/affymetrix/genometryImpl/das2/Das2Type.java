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

import java.util.*;
import java.net.*;
import com.affymetrix.genometryImpl.general.GenericFeature;

public final class Das2Type {

	private final Das2VersionedSource versioned_source;
	private final URI type_uri;
	private final String name;
	private final Map<String, String> props;
	private final Map<String, String> formats; // formats is a map of format names ("bed", "psl", etc.) to mime-type Strings
	private GenericFeature feature;

	public Das2Type(Das2VersionedSource version, URI type_uri, String name,
			Map<String, String> formats, Map<String, String> props) {
		this.versioned_source = version;
		this.type_uri = type_uri;
		this.formats = formats;
		this.props = props;
		this.name = name;
	}

	public Das2VersionedSource getVersionedSource() {
		return versioned_source;
	}

	public URI getURI() {
		return type_uri;
	}

	public String getName() {
		return name;
	}

	@Override
	public String toString() {
		if (getName() == null) {
			return getURI().toString();
		} else {
			return getName();
		}
	}

	public Map<String, String> getProps() {
		return props;
	}

	Map<String, String> getFormats() {
		return formats;
	}

	public void setFeature(GenericFeature f) {
		this.feature = f;
	}
	public GenericFeature getFeature() {
		return this.feature;
	}
}
