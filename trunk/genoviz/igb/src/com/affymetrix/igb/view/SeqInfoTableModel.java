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

	public AnnotatedSeqGroup getGroup() {
		return group;
	}

	public SeqInfoTableModel(AnnotatedSeqGroup seq_group) {
		group = seq_group;
	}

	public int getRowCount() {
		return (group == null ? 0 : group.getSeqCount());
	}

	public int getColumnCount() {
		return 5;
	}

	public Object getValueAt(int row, int col) {
		if (group != null) {
			BioSeq seq = group.getSeq(row);
			if (col == 0) {
				return GeneralLoadUtils.getNumberOfSymmetriesForSeq(seq);
			} else if (col == 1) {
				// evil hack to debug name field
				try {
					String workingString = seq.getID().substring( seq.getID().indexOf( "|NC_" ) + 1 );
					String refSeq = workingString.substring( 0, workingString.indexOf( "|" ) );
					String name = SynonymLookup.getDefaultLookup().getGenomeFromRefSeq( refSeq );
				return name;
				}
				catch ( Exception e )
				{
					return "";
				}
			} else if (col == 2) {
                            // evil hack to debug name field
				try {
				String workingString = seq.getID().substring( seq.getID().indexOf( "|NC_" ) + 1 );
				String refSeq = workingString.substring( 0, workingString.indexOf( "|" ) );
				String name = SynonymLookup.getDefaultLookup().getGenomeSpeciesFromRefSeq( refSeq );
				return name;
				}
				catch ( Exception e )
				{
					return "";
				}
				
			} else if (col == 3) {
				// evil hack to debug name field
				try {
					String workingString = seq.getID().substring( seq.getID().indexOf( "|NC_" ) + 1 );
					String refSeq = workingString.substring( 0, workingString.indexOf( "|" ) );
					String name = SynonymLookup.getDefaultLookup().getGenomeStrainFromRefSeq( refSeq );
					return name;
				}
				catch ( Exception e )
				{
					return "";
				}
			} else if (col == 4) {
				// evil hack to debug lineage field
				try {
				String workingString = seq.getID().substring( seq.getID().indexOf( "|NC_" ) + 1 );
				String refSeq = workingString.substring( 0, workingString.indexOf( "|" ) );
				String name = SynonymLookup.getDefaultLookup().getGenomeFromRefSeq( refSeq );
				String lineage = SynonymLookup.getDefaultLookup().getLineageNameFromGenera( name );
				return lineage;
				}
				catch ( Exception e )
				{
					return "";
				}
			}
		}
		return null;
	}

	@Override
	public String getColumnName(int col) {
		if (col == 0) {
			return "Reads";
		} else if (col == 1) {
			return "Genus";
		} else if (col == 2) {
			return "Species";
		} else if (col == 3) {
			return "Strain";
		} else if (col == 4) {
			return "Lineage";
		} else {
			return null;
		}
	}

	@Override
	public Class<?> getColumnClass(int c) {
		switch (c) {
			case 0:
				return Integer.class;
			default:
				return super.getColumnClass(c);
		}
	}
}