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

package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

// VariationID	Landmark	Chr	Start	End	VariationType
// LocusID	LocusChr	LocusStart	LocusEnd
// Reference	PubMedID	Method/platform	Gain	Loss	TotalGainLossInv

// The LocusID is not stable from one release to the next, so we ignore it

/**
 *  A parser designed to parse genomic variants data from http://projects.tcag.ca/variation/
 */
public final class VarParser {

	private static final String GENOMIC_VARIANTS = "Genomic Variants";

	private static final Pattern line_regex = Pattern.compile("\\t");
	
	public static List<SingletonSymWithProps> parse(InputStream dis, AnnotatedSeqGroup seq_group)
		throws IOException  {

		String line;
		List<SingletonSymWithProps> results = new ArrayList<SingletonSymWithProps>();
		Thread thread = Thread.currentThread();
		BufferedReader reader = new BufferedReader(new InputStreamReader(dis));

		line = reader.readLine();
		String[] column_names = null;
		column_names = line_regex.split(line);

		if (column_names == null) {
			throw new IOException("Column names were missing or malformed");
		}

		int line_count = 1;
		String[] fields;
		while ((line = reader.readLine()) != null && (! thread.isInterrupted())) {
			line_count++;

			fields = line_regex.split(line);
			int field_count = fields.length;

			if (field_count > column_names.length) {
				throw new IOException("Line " + line_count + " has wrong number of data columns: " + field_count);
			}

			String variationId = fields[0];
			String seqid = fields[2];
			int start = Integer.parseInt(fields[3]);
			int end = Integer.parseInt(fields[4]);

			BioSeq aseq = seq_group.getSeq(seqid);
			if (aseq == null) { aseq = seq_group.addSeq(seqid, end); }
			if (start > aseq.getLength()) { aseq.setLength(start); }
			if (end > aseq.getLength()) { aseq.setLength(end); }

			SingletonSymWithProps var = new SingletonSymWithProps(variationId, start, end, aseq);
			var.setProperty("id", variationId);
			var.setProperty("method", GENOMIC_VARIANTS);
			var.setProperty(column_names[1], fields[1]);
			for (int c=5; c < fields.length; c++) {
				String propName = column_names[c];
				if (! propName.startsWith("Locus")) {
					var.setProperty(propName, fields[c]);
				}
			}
			results.add(var);
		}
		return results;
	}
}
