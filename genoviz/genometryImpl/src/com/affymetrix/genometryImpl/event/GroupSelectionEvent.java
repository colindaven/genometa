package com.affymetrix.genometryImpl.event;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

public final class GroupSelectionEvent extends EventObject {
	private List<AnnotatedSeqGroup> selected_groups;
	private AnnotatedSeqGroup primary_selection = null;
	private static final long serialVersionUID = 1L;

	/**
	 *  Constructor.
	 * @param src The source of the event
	 * @param groups  a List of AnnotatedSeqGroup's that have been selected.
	 *   (If null, will default to {@link Collections#EMPTY_LIST}.)
	 */
	public GroupSelectionEvent(Object src, List<AnnotatedSeqGroup> groups) {
		super(src);
		this.selected_groups = groups;
		this.primary_selection = null;
		if (selected_groups == null) {
			selected_groups = Collections.<AnnotatedSeqGroup>emptyList();
		} else if (! selected_groups.isEmpty()) {
			primary_selection = groups.get(0);
		}
	}

	/** Gets the first entry in the list {@link #getSelectedGroup()}.
	 *  @return an AnnotatedSeqGroup or null.
	 */
	public AnnotatedSeqGroup getSelectedGroup() {
		return primary_selection;
	}

	@Override
		public String toString() {
			return "GroupSelectionEvent: group count: " + selected_groups.size() +
				" first group: '" + (primary_selection == null ? "null" : primary_selection.getID()) +
				"' source: " + this.getSource();
		}
}
