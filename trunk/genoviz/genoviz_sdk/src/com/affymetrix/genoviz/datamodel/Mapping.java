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

import java.util.List;
import java.util.ArrayList;

/**
 * models a mapping from an aligned sequence into a reference coordinate space.
 * The aligned sequence can be thought of as the domain or source of the mapping.
 * The reference space can be thought of as the range or destination of the mapping.
 * When used for multiple sequence alignments,
 * the reference space is generally the consenus sequence.
 *
 * e.g. <pre>
 * range: -------------------------------------------------
 *            start: |                    end: |
 * domain:           ---------------------------
 * </pre>
 *
 * <p> The start and end of the mapping need not be the beginning and end of the domain.
 *
 * e.g. <pre>
 * range: -------------------------------------------------
 *               start: |            end: |
 * domain:           ---------------------------
 * </pre>
 *
 *
 * <p> The aligned sequence (domain) can be broken into spans.
 *
 * e.g. <pre>
 * range: -------------------------------------------------
 *             start: |                    end: |
 * domain:           ----------   -----------------
 * </pre>
 */
public class Mapping {

	protected String id;
	protected boolean direction;
	protected List<Span> spans = new ArrayList<Span>();

	/** The mapping's domain. */
	protected Sequence seq;
	/* I think the sequence should associated with the Mapping + not vise-verse -tao */

	/** minimum and maximum of the mapping's range. */
	protected int ref_start, ref_end;

	/** minimum and maximum of the mapping's domain. */
	private int map_start, map_end;  // added to allow the mapped range to start somewhere other than zero.

	protected boolean ref_range_set = false;

	public Mapping () {
		super();
	}

	/**
	 * Construct an identified mapping.
	 * @param id
	 */
	public Mapping (String id) {
		this.id = id;
	}

	/**
	 * Construct a mapping with the given start and end positions relative
	 * to the reference coordinates.
	 *
	 * @param start minimum of the mapping's range.
	 * @param end maximum of the mapping's range.
	 */
	public Mapping(int start, int end) {
		super();
		ref_start = start;
		ref_end = end;
		ref_range_set = true;
		// defaults
		map_start = 0;
		map_end = Math.abs( ref_end - ref_start );
	}

	/**
	 * @return a String representing the mapping.
	 */
	public String toString() {
		return ("Mapping: seqid = " + id + ", ref_start= " + getStart() + ", ref_end = " + getEnd() +
				", map_start " + getMappedStart() + ", map_end " + getMappedEnd() + ", direction = " + direction +
				",  spans = " + spans.size());
	}

	/**
	 * sets this mapping's sequence.
	 */
	public void setSequence(Sequence seq) {
		this.seq = seq;
	}

	/**
	 * gets this mapping's sequence.
	 */
	public Sequence getSequence() {
		return seq;
	}

	/**
	 *  Sets the id (a unique identifier for this mapping).
	 */
	public void setID ( String id ) {
		this.id = id ;
	}

	/**
	 * Returns the id (a unique identifier for this mapping).
	 */
	public String getID () {
		return id;
	}

	/**
	 *  Set sequence orientation relative to reference.
	 *  true = same direction (forward), false = different direction (reverse)
	 */
	public void setDirection ( boolean direction ) {
		this.direction = direction;
	}

	/**
	 *  Return sequence orientation relative to reference.
	 *  true = same direction (forward), false = different direction (reverse)
	 */
	public boolean getDirection () {
		return direction;
	}

	/**
	 *  Return whether sequence orientation is same as reference
	 *  true = same direction (forward), false = different direction (reverse)
	 */
	public boolean isForward() {
		return direction;
	}

	/**
	 * Add an aligned Span to the mapping
	 *
	 * @see Span
	 */
	public void addSpan(Span span) {
		if (spans.size() == 0 && (!ref_range_set)) {
			ref_start = span.ref_start;
			ref_end = span.ref_end;
			map_start = span.seq_start;
			map_end = span.seq_end;

		}
		else {
			if (span.ref_start < ref_start) { ref_start = span.ref_start; }
			if (span.ref_end > ref_end) { ref_end = span.ref_end; }
			if (span.seq_start < map_start) { map_start = span.seq_start; }
			if (span.seq_end > map_end) { map_end = span.seq_end; }
		}
		spans.add(span);
		setDirection(span.seq_start <= span.seq_end);
	}

	/**
	 *  return start of mapping (relative to reference).
	 */
	public int getStart() {
		return ref_start;
	}

	/**
	 * return end of mapping (relative to reference).
	 */
	public int getEnd() {
		return ref_end;
	}

	/**
	 * return a particular span.
	 */
	public Span getSpan(int index) {
		return spans.get(index);
	}

	/**
	 * return a Vector of all spans in the mapping.
	 */
	public List<Span> getSpans() {
		return spans;
	}

	/** @deprecated  use {@link #mapToMapped(int)}. */
	@Deprecated
		public int mapToSequence(int ref_pos) {
			return mapToMapped( ref_pos );
		}

	/**
	 * maps a reference position to a mapped position.
	 *
	 * @param ref_pos the reference position
	 * @return the sequence position that maps to the given reference position,
	 *         or {@link Integer#MIN_VALUE} if there is none.
	 */
	public int mapToMapped(int ref_pos) {
		int seqpos = Integer.MIN_VALUE;
		if (ref_pos < ref_start || ref_pos > ref_end) {
			return seqpos;
		}
		int i;
		Span sp;
		int numspans = spans.size();
		for (i=0; i<numspans; i++) {
			sp = spans.get(i);
			if (ref_pos >= sp.ref_start && ref_pos <= sp.ref_end) {
				seqpos = sp.seq_start + (ref_pos - sp.ref_start);
				return seqpos;
			}
		}
		return seqpos;
	}


	/**
	 * maps a sequence position to a reference position.
	 *
	 * @param map_pos the sequence position
	 * @return the reference position that maps to the given sequence position,
	 *         or {@link Integer#MIN_VALUE} if there is none.
	 */
	public int mapToReference(int map_pos) {
		int i;
		int ref_pos = Integer.MIN_VALUE;
		if (map_pos < map_start || map_pos > map_end) {
			return ref_pos;
		}

		Span sp;
		int numspans = spans.size();
		for (i=0; i<numspans; i++) {
			sp = spans.get(i);
			if (map_pos >= sp.seq_start && map_pos <= sp.seq_end) {
				ref_pos = sp.ref_start + (map_pos - sp.seq_start);
				return ref_pos;
			}
		}
		return ref_pos;
	}



	public int getMappedStart() {
		return map_start;
	}

	public int getMappedEnd(){
		return map_end;
	}

	public void setMappedRange( int start, int end ) {
		map_start = start;
		map_end = end;
	}

	// some renamed fuctions to conform to more generic names
	public int getReferenceStart() {
		return getStart();
	}

	public int getReferenceEnd(){
		return getEnd();
	}

}
