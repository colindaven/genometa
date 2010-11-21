/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package genoviz.demo.datamodel;

import java.util.*;
import com.affymetrix.genoviz.datamodel.*;

public class Assembly {
	/**
	 * The consensus mapping.
	 * Mappings contain both Sequence and Span info relative to the reference.
	 */
	Mapping consensus;

	/**
	 * A vector of Mappings corresponding to aligned sequences
	 */
	List aligns;
	List seqs;
	List traces;

	/**
	 * The length of the entire assembly
	 */
	int length;

	// a hash of sequence IDs to Sequences
	Hashtable seqhash;

	// a hash of sequence IDs to AlignTraces (if available)
	Hashtable tracehash;

	// a hash of sequence IDs to Mappings (alignments)
	Hashtable alignhash;

	public Assembly(Mapping consensus, Vector aligns, Vector seqs) {
		this(consensus, aligns, seqs, null);
	}

	@SuppressWarnings("unchecked")
	public Assembly(Mapping consensus, Vector aligns, Vector seqs, Vector traces) {
		this.consensus = consensus;
		this.aligns = aligns;
		this.seqs = seqs;
		this.traces = traces;

		// Usually the end of the last span in consensus will correspond to
		//     end of the assembly, but not always -- therefore go through
		//     aligns to find highest number start or end of span to determine
		//     length
		Mapping m;
		Span s;
		List<Span> v;
		int max = 0;
		for (int i=0; i<aligns.size(); i++) {
			m = (Mapping)aligns.elementAt(i);
			v = m.getSpans();
			for (int j=0; j < v.size(); j++) {
				s = v.get(j);
				if (s.ref_end >= s.ref_start) {
					if (s.ref_end > max) {
						max = s.ref_end;
					}
				}
				else {
					if (s.ref_start > max) {
						max = s.ref_start;
					}
				}
			}
		}
		this.length = max + 1;

		Mapping align;
		Sequence seq;
		AlignTrace trace;

		seqhash = new Hashtable();
		if (seqs != null) {
			for (int i=0; i<seqs.size(); i++) {
				seq = (Sequence)seqs.elementAt(i);
				seqhash.put(seq.getID(), seq);
			}
		}

		tracehash = new Hashtable();
		if (traces != null) {
			for (int i=0; i<traces.size(); i++) {
				trace = (AlignTrace)traces.elementAt(i);
				tracehash.put(trace.getID(), trace);
				trace.setSequence((Sequence)seqhash.get(trace.getID()));
			}
		}

		alignhash = new Hashtable();
		if (aligns != null) {
			for (int i=0; i<aligns.size(); i++) {
				align = (Mapping)aligns.elementAt(i);
				alignhash.put(align.getID(), align);
				align.setSequence((Sequence)seqhash.get(align.getID()));
			}
		}
	}

	public int getLength() {
		return length;
	}

	public Mapping getConsensus() {
		return consensus;
	}

	public List getAlignments() {
		return aligns;
	}
	public List getSequences() {
		return seqs;
	}
	public List getTraces() {
		return traces;
	}

	public Mapping getAlignment(String id) {
		return (Mapping)alignhash.get(id);
	}
	public Sequence getSequence(String id) {
		return (Sequence)seqhash.get(id);
	}
	public AlignTrace getTrace(String id) {
		return (AlignTrace)tracehash.get(id);
	}

}
