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

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import java.io.*;
import java.util.regex.*;

import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.Timer;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LiftParser {

	private static final Pattern re_tab = Pattern.compile("\t");
	private static final Pattern re_name = Pattern.compile("/");
	private static final int CHROM_START = 0;
	private static final int COMBO_NAME = 1;
	private static final int MATCH_LENGTH = 2;
	private static final int CHROM_NAME = 3;
	private static final int CHROM_LENGTH = 4;
	private static final int CONTIG_NAME_SUBFIELD = 1;

	private static final boolean SET_COMPOSITION = true;

	public static AnnotatedSeqGroup loadChroms(String file_name, GenometryModel gmodel, String genome_version)
		throws IOException {
		Logger.getLogger(LiftParser.class.getName()).log(
							Level.FINE, "trying to load lift file: {0}", file_name);
		FileInputStream fistr = null;
		AnnotatedSeqGroup result = null;
		try {
			File fil = new File(file_name);
			fistr = new FileInputStream(fil);
			result = LiftParser.parse(fistr, gmodel, genome_version);
		}
		finally {
			GeneralUtils.safeClose(fistr);
		}
		return result;
	}


	/**
	 *  Reads lift-format from the input stream.
	 *  @return  A Map with chromosome ids as keys, and SmartAnnotBioSeqs representing
	 *     chromosomes in the lift file as values.
	 */
	public static AnnotatedSeqGroup parse(InputStream istr, GenometryModel gmodel, String genome_version) throws IOException {
		return parse(istr, gmodel, genome_version, true);
	}

	/**
	 *  Reads lift-format from the input stream and creates a new AnnotatedSeqGroup.
	 *  The new AnnotatedSeqGroup will be inserted into the GenometryModel.
	 *  @return an AnnotatedSeqGroup containing SmartAnnotBioSeqs representing
	 *     chromosomes in the lift file.
	 */
	public static AnnotatedSeqGroup parse(InputStream istr, GenometryModel gmodel, String genome_version, boolean annotate_seq)
		throws IOException {
		Logger.getLogger(LiftParser.class.getName()).log(
							Level.FINE,"parsing in lift file");
		Timer tim = new Timer();
		tim.start();
		int contig_count = 0;
		int chrom_count = 0;
		AnnotatedSeqGroup seq_group = gmodel.addSeqGroup(genome_version);

		BufferedReader br = new BufferedReader(new InputStreamReader(istr));

		try {
			String line;
			Thread thread = Thread.currentThread();
			while ((line = br.readLine()) != null && (!thread.isInterrupted())) {
				if ((line.length() == 0) || line.equals("") || line.startsWith("#")) {
					continue;
				}
				String fields[] = re_tab.split(line);
				int chrom_start = Integer.parseInt(fields[CHROM_START]);
				int match_length = Integer.parseInt(fields[MATCH_LENGTH]);
				String chrom_name = fields[CHROM_NAME];
				int chrom_length = Integer.parseInt(fields[CHROM_LENGTH]);

				String tempname = fields[COMBO_NAME];
				String splitname[] = re_name.split(tempname);
				String contig_name = splitname[CONTIG_NAME_SUBFIELD];
				// experimenting with constructing virtual sequences by using chromosomes as contigs
				BioSeq contig = seq_group.getSeq(contig_name);
				if (contig == null) {
					contig = new BioSeq(contig_name, "", match_length);
				}

				contig_count++;
				BioSeq chrom = seq_group.getSeq(chrom_name);
				if (chrom == null) {
					chrom_count++;
					chrom = seq_group.addSeq(chrom_name, chrom_length);
					chrom.setVersion(genome_version);
				}

				MutableSeqSymmetry comp = (MutableSeqSymmetry) chrom.getComposition();
				if (comp == null) {
					comp = new SimpleSymWithProps();
					((SimpleSymWithProps) comp).setProperty("method", "contigs");
					if (SET_COMPOSITION) {
						chrom.setComposition(comp);
					}
					if (annotate_seq) {
						chrom.addAnnotation(comp);
					}
				}
				SimpleSymWithProps csym = new SimpleSymWithProps();
				csym.addSpan(new SimpleSeqSpan(chrom_start, (chrom_start + match_length), chrom));
				csym.addSpan(new SimpleSeqSpan(0, match_length, contig));
				csym.setProperty("method", "contig");
				csym.setProperty("id", contig.getID());
				comp.addChild(csym);
			}
		} catch (EOFException ex) {
			Logger.getLogger(LiftParser.class.getName()).log(
							Level.FINE,"reached end of lift file");
		}

		for (BioSeq chrom : seq_group.getSeqList()) {
		
			MutableSeqSymmetry comp = (MutableSeqSymmetry) chrom.getComposition();
			if (comp != null && SET_COMPOSITION) {
				SeqSpan chromspan = SeqUtils.getChildBounds(comp, chrom);
				comp.addSpan(chromspan);
			}
		}
		Logger.getLogger(LiftParser.class.getName()).log(
							Level.INFO, "chroms loaded, load time = {0}", tim.read() / 1000f);
		Logger.getLogger(LiftParser.class.getName()).log(
							Level.INFO, "contig count: {0}", contig_count);
		Logger.getLogger(LiftParser.class.getName()).log(
							Level.INFO, "chrom count: {0}", chrom_count);
		return seq_group;
	}

}
