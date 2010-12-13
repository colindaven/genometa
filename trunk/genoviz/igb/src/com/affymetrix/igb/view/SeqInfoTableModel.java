/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author bene
 */
public class SeqInfoTableModel extends AbstractTableModel {

	private final AnnotatedSeqGroup group;

	public SeqInfoTableModel(AnnotatedSeqGroup seq_group) {
		group = seq_group;
	}

	public int getRowCount() {
		return (group == null ? 0 : group.getSeqCount());
	}

	public int getColumnCount() {
		return 2;
	}

	public Object getValueAt(int row, int col) {
		if (group != null) {
			BioSeq seq = group.getSeq(row);
			if (col == 0) {
				return seq.getID();
			} else if (col == 1) {
				return GeneralLoadUtils.getNumberOfSymmetriesForSeq(seq);
			}
		}
		return null;
	}

	@Override
	public String getColumnName(int col) {
		if (col == 0) {
			return "("+ getRowCount() +") Sequence(s)";
		} else if (col == 1) {
			return "Reads";
		} else {
			return null;
		}
	}

	@Override
	public Class<?> getColumnClass(int c) {
		switch (c) {
			case 1:
				return Integer.class;
			default:
				return super.getColumnClass(c);
		}
	}
}