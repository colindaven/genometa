/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.SimpleSymWithProps;

/**
 *  Attempts to parse "generic" tab delimited data.
 *  For example excel spreadsheets (in tab format).
 *  Which columns to use for start, end, etc. are specified in the constructor.
 */
public class TabDelimitedParser {
	private final int chromosome_col;
	private final int start_col;
	private final int end_col;     // should need only end_col or length_col, not both
	private final int length_col;  // should need only end_col or length_col, not both
	private final int strand_col;  // column to use for determining strand
	private final int group_col;  // column to use for grouping features...
	private final int type_col;   // column to use for setting feature type

	private final boolean annotateSeq; // whether to add annotation id's to the index on the seq group

	private int id_col;

	// if makeProps, then each column (other than start, end, length, group) will become a
	//    property in the SymWithProps that is generated
	private boolean make_props = true;

	private boolean use_length = false;
	private boolean use_group = false;
	private boolean use_type = false;
	private boolean use_strand = false;
	private boolean has_header = false;
	private boolean has_id = false;

	private static final Pattern line_splitter = Pattern.compile("\t");

	/**
	 *  Constructor.
	 *  Each argument tells which column to find a particular item in.
	 *  -1 for any arg indicates that it is not present in the table.
	 *  @param props Whether to use the column names to construct properties.
	 *    Only valid if "header" is true.
	 *  @param header Whether there is a header line containing column names for properties
	 *  @param addToIndex Whether to add the ID of each item to the index on the
	 *    AnnotatedSeqGroup
	 */
	public TabDelimitedParser(int type, int chromosome, int start, int end, int length,
			int strand, int group, int id, boolean props, boolean header, boolean annotateSeq) {

		if (chromosome < 0) {
			throw new IllegalArgumentException("Chromosome column number must be 0 or greater.");
		}

		chromosome_col = chromosome;
		start_col = start;
		end_col = end;
		length_col = length;
		group_col = group;
		type_col = type;
		strand_col = strand;
		id_col = id;
		this.annotateSeq = annotateSeq;

		has_header = header;
		use_length = (length >= 0);
		use_group = (group >= 0);
		use_type = (type >=0);
		use_strand = (strand >= 0);
		has_id = (id >= 0);

		this.make_props = props;
	}


	/**
	 *  Parses data into the given AnnotatedSeqGroup.
	 *  @param istr The source of the data
	 *  @param default_type  The name to use for the "type" or "method" if the
	 *    "type" column parameter in the constructor was -1.
	 *  @param seq_group  The AnnotatedSeqGroup on which to add the data.
	 */
	public List<SeqSymmetry> parse(InputStream istr, String default_type, AnnotatedSeqGroup seq_group) {

		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		Map<String,SeqSymmetry> group_hash = new HashMap<String,SeqSymmetry>();
		MutableSeqSpan union_span = new SimpleMutableSeqSpan();
		List<String> col_names = null;
		try {
			InputStreamReader asr = new InputStreamReader(istr);
			BufferedReader br = new BufferedReader(asr);
			String line;
			if (has_header) {
				line = br.readLine();
				String[] cols = line_splitter.split(line);
				col_names = new ArrayList<String>(cols.length);
				for (int i=0; i<cols.length; i++) {
					col_names.add(cols[i]);
				}
			}
			while ((line = br.readLine()) != null && (!Thread.currentThread().isInterrupted())) {
				String[] cols = line_splitter.split(line);
				if (cols.length <= 0) { continue; }

				int start = Integer.parseInt(cols[start_col]);
				int end;
				if (use_length) {
					int length = Integer.parseInt(cols[length_col]);
					if (use_strand) {
						String strand = cols[strand_col];
						if (strand.equals("-")) {
							end = start - length;
						}
						else {
							end = start + length;
						}
					} else {
						end = start + length;
					}
				} else {
					end = Integer.parseInt(cols[end_col]);
				}
				String type = default_type;
				if (use_type) {
					type = cols[type_col];
				}

				String id = null;
				if (has_id) {
					id = cols[id_col];
				}

				String chromName = cols[chromosome_col];
				BioSeq seq = seq_group.getSeq(chromName);
				if (seq == null) {
					seq = seq_group.addSeq(chromName, 0);
				}

				if (seq.getLength() < end) {
					seq.setLength(end);
				}
				if (seq.getLength() < start) {
					seq.setLength(start);
				}

				SingletonSymWithProps child = new SingletonSymWithProps(start, end, seq);
				child.setProperty("method", type);
				if (id == null) {
					id = type + " " + seq.getID() + ":" + start + "-" + end;
				}
				child.setProperty("id", id);
				if (make_props) {
					for (int i=0; i<cols.length && i<col_names.size(); i++) {
						String name = col_names.get(i);
						String val = cols[i];
						child.setProperty(name, val);
					}
				}

				if (use_group) {
					String group = cols[group_col];
					SimpleSymWithProps parent = (SimpleSymWithProps) group_hash.get(group);
					if (parent == null) {
						parent = new SimpleSymWithProps();
						SimpleMutableSeqSpan span = new SimpleMutableSeqSpan(start, end, seq);
						parent.addSpan(span);
						parent.setProperty("method", type);
						if (id == null) {
							id = type + " " + span.getBioSeq().getID() + ":" + span.getStart() + "-" + span.getEnd();
						}

						parent.setProperty("id", id);
						group_hash.put(group, parent);
						// or maybe should add all parents to a grandparent, and add _grandparent_ to aseq???
						results.add(parent);
						if (annotateSeq) {
							seq.addAnnotation(parent);
							seq_group.addToIndex(parent.getID(), parent);
						}
					} else {
						MutableSeqSpan pspan = (MutableSeqSpan) parent.getSpan(seq);
						SeqUtils.encompass(pspan, (SeqSpan) child, union_span);
						pspan.set(union_span.getStart(), union_span.getEnd(), seq);
					}
					parent.addChild(child);
				} else {
					results.add(child);
					if (annotateSeq) {
						seq.addAnnotation(child);
						seq_group.addToIndex(child.getID(), child);
					}
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return results;
	}
}
