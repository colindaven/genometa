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

/** A parser for the Affymetrix Generic CHP files containing copy number data. */
package com.affymetrix.genometryImpl.parsers.gchp;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AffyCnChpParser {
	public List<SeqSymmetry> parse(
			File file, InputStream istr, String stream_name, AnnotatedSeqGroup seq_group, boolean annotateSeq)
			throws IOException {

		Logger.getLogger(AffyCnChpParser.class.getName()).log(
							Level.FINE, "Parsing with {0}: {1}", new Object[]{this.getClass().getName(), stream_name});
		ChromLoadPolicy loadPolicy = ChromLoadPolicy.getLoadAllPolicy();
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		try {
			AffyGenericChpFile chpFile = AffyGenericChpFile.parse(file, loadPolicy, istr, false);

			AffyDataGroup group = chpFile.groups.get(0);
			AffyDataSet dataSet = group.getDataSets().get(0);

			for (String seq_name : dataSet.getChromosomeNames()) {
				// Make sure that all the seq's mentioned in the header are
				// present in the SeqGroup.  Adds them if necessary.
				getSeq(seq_group, seq_name);
			}            

			for (AffySingleChromData data : dataSet.getSingleChromData()) {
				BioSeq seq = getSeq(seq_group, data.displayName);
				List<SeqSymmetry> syms = data.makeGraphs(seq);
				if (annotateSeq) {
					for (SeqSymmetry sym : syms) {
						seq.addAnnotation(sym);
					}
				}
				results.addAll(syms);
			}
		} catch (Exception e) {
			if (! (e instanceof IOException)) {
				IOException ioe = new IOException("IOException for file: " + stream_name);
				e.printStackTrace();
				ioe.initCause(e);
				throw ioe;
			}
		}
		return results;
	}

	private BioSeq getSeq(AnnotatedSeqGroup seq_group, String seqid) {
		BioSeq aseq = seq_group.getSeq(seqid);
		if (aseq == null) {
			aseq = seq_group.addSeq(seqid, 1);
		}
		return aseq;
	} 
}
