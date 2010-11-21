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

package com.affymetrix.genometryImpl.parsers.gchp;

import java.io.*;
import java.util.*;

public final class AffyGenericDataHeader {

	public CharSequence data_type_guid;
	public CharSequence unique_id;
	public String date_time;
	public String locale;
	public Map<String,AffyChpParameter> paramMap = new LinkedHashMap<String,AffyChpParameter>();
	public List<AffyGenericDataHeader> children = new ArrayList<AffyGenericDataHeader>();

	/** Creates a new instance of GenericDataHeader */
	protected AffyGenericDataHeader() {
	}

	/** Prints the header to the given stream. */
	public void dump(PrintStream str) {
		str.println(this.getClass().getName());
		str.println("guid: " + data_type_guid);
		str.println("unique: " + unique_id);
		str.println("date: " + date_time);
		str.println("locale: " + locale);

		for (AffyChpParameter param : paramMap.values()) {
			str.println(param.toString());
		}

		for (AffyGenericDataHeader header : children) {
			str.println("----- child header ------");
			header.dump(str);
		}
	}

	public static AffyGenericDataHeader readHeader(DataInputStream dis) throws IOException {
		AffyGenericDataHeader header = new AffyGenericDataHeader();

		header.data_type_guid = AffyGenericChpFile.parseString(dis);
		header.unique_id = AffyGenericChpFile.parseString(dis);
		header.date_time = AffyGenericChpFile.parseWString(dis);
		header.locale = AffyGenericChpFile.parseWString(dis); // specification is unclear on the format

		int param_count = dis.readInt();

		for (int i=0; i<param_count; i++) {
			AffyChpParameter p = AffyChpParameter.parse(dis);
			header.paramMap.put(p.name, p);
		}


		int moreParamMaps = dis.readInt();

		for (int j=0; j<moreParamMaps && j<1; j++) {
			AffyGenericDataHeader childHeader = readHeader(dis);
			header.children.add(childHeader);
		}
		return header;
	}

}
