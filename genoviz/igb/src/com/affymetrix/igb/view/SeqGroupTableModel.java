package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.featureloader.QuickLoad;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.util.List;
import javax.swing.table.AbstractTableModel;

final class SeqGroupTableModel extends AbstractTableModel {

	private final AnnotatedSeqGroup group;

	public SeqGroupTableModel(AnnotatedSeqGroup seq_group) {
		group = seq_group;
	}

	public int getRowCount() {
		return (group == null ? 0 : group.getSeqCount());
	}

	public int getColumnCount() {
		return 3;
	}

	public Object getValueAt(int row, int col) {
		if (group != null) {
			BioSeq seq = group.getSeq(row);
			if (col == 0) {
				return seq.getID();
			} else if (col == 1) {
				if (IGBConstants.GENOME_SEQ_ID.equals(seq.getID())) {
					return "";	// don't show the "whole genome" size, because it disagrees with the chromosome total
				}
				return Long.toString((long) seq.getLengthDouble());
			} else if (col == 2) {
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
			return "Length";
		} else if (col == 2) {
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
			case 2:
				return Integer.class;
			default:
				return super.getColumnClass(c);
		}
	} 
}
