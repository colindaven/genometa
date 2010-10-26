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

package com.affymetrix.genoviz.datamodel;

import java.net.*;

import com.affymetrix.genoviz.parser.SCFTraceParser;

/**
 * maps between positions in an assembly
 * to a trace
 * via a sequence.
 */
public class AlignTrace {

	/** locates an SCF trace on an internet. */
	URL traceURL;

	/**
	 * offset into the trace of the start for the sequence used
	 *     in the alignment.
	 */
	int seq_offset;

	/**
	 * length of sequence used in the alignment.
	 */
	int seq_length;

	/**
	 * true if Sequence is reverse complement relative to trace bases.
	 * (for univiewer, need to test for this even when traces are not pulled up,
	 * to determine correct direction to display for Sequence on the alignment.
	 *  in the univiewer files, all sequences are forward in the alignment, but
	 *  ones that have been flipped relative to their original traces should be
	 *  displayed as reverse complement (arrows pointing left))
	 */
	boolean seq_flipped;

	String seq_id, name;

	/**
	 * the trace produced by an automatic sequencing machine.
	 */
	TraceI trace;
	/**
	 * the Sequence based on this trace.
	 * this is _not_ the same as the trace residues --
	 * can be offset into the trace, and terminate before end of trace,
	 * and may be flipped around relative to the trace
	 */
	Sequence seq;

	/**
	 * constructs the map between the trace and the sequence.
	 */
	public AlignTrace(String seq_id, int seq_offset,
			int seq_length, boolean flipped, URL url) {
		this.seq_id = seq_id;
		this.seq_offset = seq_offset;
		this.seq_length = seq_length;
		this.seq_flipped = flipped;
		this.traceURL = url;
		name = this.traceURL.getFile();
		int tempint = name.lastIndexOf('/');
		if (tempint != -1) {
			name = name.substring(tempint+1);
		}
	}

	public void setSequence(Sequence seq) {
		this.seq = seq;
	}

	public String getID() {
		return seq_id;
	}

	/**
	 * Given a base position in the Sequence, returns
	 *       a base position in the trace.
	 */
	public int getSequencePosition(int trace_position) {
		int seq_position;
		seq_position = trace_position - seq_offset;
		return seq_position;
	}

	/**
	 * Given a base position in the trace, returns
	 *       a base position in the Sequence.
	 */
	public int getTracePosition(int seq_position) {
		int trace_position;
		trace_position = seq_position + seq_offset;
		return trace_position;
	}

	public URL getURL() {
		return this.traceURL;
	}

	public String getName() {
		return name;
	}

	/**
	 * gets the data model for the trace.
	 * If the trace data has not yet been loaded,
	 * it will be loaded first.
	 *
	 * @see #loadTrace
	 */
	public TraceI getTraceData() {
		if (trace == null) {
			loadTrace();
		}
		return trace;
	}

	public boolean isFlipped() {
		return seq_flipped;
	}

	/**
	 * reads and parses an SCF file from this AlignTrace's URL.
	 */
	public void loadTrace() {
		SCFTraceParser scfp = new SCFTraceParser();
		try {
			trace = (Trace) scfp.importContent(this.traceURL.openStream());
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		if (seq_flipped) {
			trace = trace.reverseComplement();
		}
	}

}
