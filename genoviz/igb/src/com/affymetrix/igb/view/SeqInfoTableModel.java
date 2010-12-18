/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.SynonymLookup;
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
		return 6;
	}

	public Object getValueAt(int row, int col) {
		if (group != null) {
			BioSeq seq = group.getSeq(row);
			if (col == 0) {
				return seq.getID();
			} else if (col == 1) {
				return GeneralLoadUtils.getNumberOfSymmetriesForSeq(seq);
			} else if (col == 2) {
				// evil hack to debug name field
				String workingString = seq.getID().substring( seq.getID().indexOf( "|NC_" ) + 1 );
				String refSeq = workingString.substring( 0, workingString.indexOf( "|" ) );
				String name = SynonymLookup.getDefaultLookup().getGenomeFromRefSeq( refSeq );
				return name;
			} else if (col == 3) {
				// evil hack to debug name field
				String workingString = seq.getID().substring( seq.getID().indexOf( "|NC_" ) + 1 );
				String refSeq = workingString.substring( 0, workingString.indexOf( "|" ) );
				String name = SynonymLookup.getDefaultLookup().getGenomeStrainFromRefSeq( refSeq );
				return name;
			} else if (col == 4) {
				// evil hack to debug name field
				String workingString = seq.getID().substring( seq.getID().indexOf( "|NC_" ) + 1 );
				String refSeq = workingString.substring( 0, workingString.indexOf( "|" ) );
				String name = SynonymLookup.getDefaultLookup().getGenomeSpeciesFromRefSeq( refSeq );
				return name;
			} else if (col == 5) {
				// evil hack to debug lineage field
				String workingString = seq.getID().substring( seq.getID().indexOf( "|NC_" ) + 1 );
				String refSeq = workingString.substring( 0, workingString.indexOf( "|" ) );
				String name = SynonymLookup.getDefaultLookup().getGenomeFromRefSeq( refSeq );
				String lineage = SynonymLookup.getDefaultLookup().getLineageNameFromGenera( name );
				return lineage;
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
		} else if (col == 2) {
			return "Name";
		} else if (col == 3) {
			return "Strain";
		} else if (col == 4) {
			return "Species";
		} else if (col == 5) {
			return "Lineage";
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