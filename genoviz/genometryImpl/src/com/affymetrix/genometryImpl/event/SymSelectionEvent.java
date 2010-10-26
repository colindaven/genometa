package com.affymetrix.genometryImpl.event;

import com.affymetrix.genometryImpl.SeqSymmetry;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

public final class SymSelectionEvent extends EventObject {
	private final List<SeqSymmetry> selected_syms;
	private static final long serialVersionUID = 1L;

	/**
	 *  Constructs a SymSelectionEvent.
	 *  @param src The source of the event
	 *  @param syms a List of SeqSymmetry's.  Can be empty, but should not be null.
	 *   (If null, will default to {@link Collections#EMPTY_LIST}.)
	 */
	public SymSelectionEvent(Object src, List<SeqSymmetry> syms) {
		super(src);
		if (syms == null) {
			this.selected_syms = Collections.<SeqSymmetry>emptyList();
		} else {
			this.selected_syms = syms;
		}
	}

	/** @return a List of SeqSymmetry's.  May be empty, but will not be null.
	*/
	public List<SeqSymmetry> getSelectedSyms() {
		return selected_syms;
	}

}
