package com.affymetrix.genometryImpl.event;

import java.util.*;
import com.affymetrix.genometryImpl.BioSeq;

public final class SeqSelectionEvent extends EventObject {
	private final List<BioSeq> selected_seqs;
	private BioSeq primary_selection = null;
	private static final long serialVersionUID = 1L;

	/**
	 *  Constructor.
	 *  @param src The source of the event.
	 *  @param seqs a List of AnnotatedBioSeq's that have been selected.
	 *   (If null, will default to {@link Collections#EMPTY_LIST}.)
	 */
	public SeqSelectionEvent(Object src, List<BioSeq> seqs) {
		super(src);
		if (seqs == null) {
			this.selected_seqs = Collections.<BioSeq>emptyList();
		} else {
			this.selected_seqs = seqs;
			if (!selected_seqs.isEmpty()) {
				primary_selection = selected_seqs.get(0);
			}
		}
	}

	/** Gets the first entry in the list {@link #getSelectedSeq()}.
	 *  @return a BioSeq or null.
	 */
	public BioSeq getSelectedSeq() {
		return primary_selection;
	}

	@Override
		public String toString() {
			return "SeqSelectionEvent: seq count: " + selected_seqs.size() +
				" first seq: '" + (primary_selection == null ? "null" : primary_selection.getID()) +
				"' source: " + this.getSource();
		}
}
