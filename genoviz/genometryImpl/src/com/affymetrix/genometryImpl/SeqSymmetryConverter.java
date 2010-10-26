/**
 *   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;

/**
 *  Routines to convert generic SeqSymmetrys to PSL SeqSymmetrys.
 */
public final class SeqSymmetryConverter {

	/**
	 *  Converts any SeqSymmetry defined on three BioSeq's into a Psl3Sym.
	 *  Unknown values in the PSL will be set to -1:
	 *  matches, mismatches, repmatches, ncount, gNumInsert, qBaseInsert, tNumInsert, tBaseInsert.
	 *  @param type  The PSL type parameter
	 */
	/*
	   public static Psl3Sym convertToTrioPslSym(SeqSymmetry sym, String type,
	   BioSeq queryseq, BioSeq targetseq, BioSeq otherseq) {
	   int child_count = sym.getChildCount();
	   SeqSpan qspan = sym.getSpan(queryseq);
	   SeqSpan tspan = sym.getSpan(targetseq);
	   SeqSpan ospan = sym.getSpan(otherseq);

	// qforward is true iff both spans are same orientation  (therefore = NOT ( XOR (tforward, qforward)))
	boolean target_forward = (! (tspan.isForward() ^ qspan.isForward()));
	boolean other_forward = (! (ospan.isForward() ^ qspan.isForward()));
	int[] blockSizes = new int[child_count];
	int[] tmins = new int[child_count];
	int[] qmins = new int[child_count];
	int[] omins = new int[child_count];
	for (int i=0; i<child_count; i++) {
	SeqSymmetry child = sym.getChild(i);
	SeqSpan child_qspan = child.getSpan(queryseq);
	SeqSpan child_tspan = child.getSpan(targetseq);
	SeqSpan child_ospan = child.getSpan(otherseq);
	blockSizes[i] = child_tspan.getLength();
	tmins[i] = child_tspan.getMin();
	qmins[i] = child_qspan.getMin();
	omins[i] = child_ospan.getMin();
	}
	Psl3Sym trisym = new Psl3Sym(type, -1, -1, -1, -1,
	-1, -1, -1, -1,
	target_forward, other_forward,
	queryseq, qspan.getMin(), qspan.getMax(),
	targetseq, tspan.getMin(), tspan.getMax(),
	otherseq, ospan.getMin(), ospan.getMax(),
	child_count, blockSizes, qmins, tmins, omins);
	return trisym;
	}*/

	/**
	 *  Converts any SeqSymmetry into a UcscPslSym.
	 *  @param targetseq the target sequence; calculate query seq
	 *  assuming no gaps in query.
	 *  Unknown values in the PSL will be set to -1:
	 *  matches, mismatches, repmatches, ncount, gNumInsert, qBaseInsert, tNumInsert, tBaseInsert.
	 *  @param type  The PSL type parameter
	 */
	public static UcscPslSym convertToPslSym(SeqSymmetry sym, String type,
			BioSeq targetseq)  {
		int child_count = sym.getChildCount();
		SeqSpan tspan = sym.getSpan(targetseq);
		boolean forward = tspan.isForward();
		String qname = null;
		if (sym instanceof SymWithProps) {
			SymWithProps psym = (SymWithProps)sym;
			qname = (String)psym.getProperty("group");
		}
		if (qname == null) { qname = sym.getID(); }
		if (qname == null) { qname = "unknown"; }
		//    SeqSpan tspan = sym.getSpan(targetseq);

		int curlength = 0;
		int[] blockSizes = new int[child_count];
		int[] qmins = new int[child_count];
		int[] tmins = new int[child_count];
		for (int i=0; i<child_count; i++) {
			SeqSymmetry child = sym.getChild(i);
			SeqSpan child_tspan = child.getSpan(targetseq);
			blockSizes[i] = child_tspan.getLength();
			tmins[i] = child_tspan.getMin();
			qmins[i] = curlength;
			curlength += child_tspan.getLength();
		}

		BioSeq queryseq = new BioSeq(qname, qname, curlength);
		SeqSpan qspan = new SimpleSeqSpan(0, curlength, queryseq);

		UcscPslSym pslsym = new UcscPslSym(type, -1, -1, -1, -1,
				-1, -1, -1, -1, forward,
				queryseq, qspan.getMin(), qspan.getMax(),
				targetseq, tspan.getMin(), tspan.getMax(),
				child_count, blockSizes, qmins, tmins);
		return pslsym;
	}

	/**
	 *  Converts any SeqSymmetry into a UcscPslSym.
	 *  Unknown values in the PSL will be set to -1:
	 *  matches, mismatches, repmatches, ncount, gNumInsert, qBaseInsert, tNumInsert, tBaseInsert.
	 *  @param targetseq indicates span in symmetry to be used as "target" span for UcscPslSym
	 *  @param queryseq indicates span in symmetry to be used as "query" span for UcscPslSym
	 */
	public static UcscPslSym convertToPslSym(SeqSymmetry sym, String type,
			BioSeq queryseq, BioSeq targetseq) {
		int child_count = sym.getChildCount();
		SeqSpan qspan = sym.getSpan(queryseq);
		SeqSpan tspan = sym.getSpan(targetseq);

		// qforward is true iff both spans are same orientation  (therefore = NOT ( XOR (tforward, qforward)))
		boolean forward = (! (tspan.isForward() ^ qspan.isForward()));
		int[] blockSizes = new int[child_count];
		int[] tmins = new int[child_count];
		int[] qmins = new int[child_count];
		for (int i=0; i<child_count; i++) {
			SeqSymmetry child = sym.getChild(i);
			SeqSpan child_qspan = child.getSpan(queryseq);
			SeqSpan child_tspan = child.getSpan(targetseq);
			blockSizes[i] = child_tspan.getLength();
			tmins[i] = child_tspan.getMin();
			// Throwing a null pointer exception with AnnotMapper during 
			// hg16-hg17 mapping used for exon array. --steve chervitz
			if(child_qspan != null) {
				qmins[i] = child_qspan.getMin();
			}
		}
		UcscPslSym pslsym = new UcscPslSym(type, -1, -1, -1, -1,
				-1, -1, -1, -1, forward,
				queryseq, qspan.getMin(), qspan.getMax(),
				targetseq, tspan.getMin(), tspan.getMax(),
				child_count, blockSizes, qmins, tmins);
		return pslsym;
	}

}
