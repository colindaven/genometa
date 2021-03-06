/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.util.DisplayUtils;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableRowSorter;

/**
 *
 * @author bene
 */
public final class SeqInfoView extends JComponent implements ListSelectionListener, GroupSelectionListener, SeqSelectionListener {

	private final static boolean DEBUG_EVENTS = false;
	private final static GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final JTable seqtable = new JTable();
	private static List <RowSorter.SortKey> sortKeys = new ArrayList<RowSorter.SortKey>();
	private BioSeq selected_seq = null;
	private AnnotatedSeqGroup previousGroup = null;
	private int previousSeqCount = 0;
	private final ListSelectionModel lsm;
	private TableRowSorter<SeqInfoTableModel> sorter;
	private String most_recent_seq_id = null;

	public SeqInfoView() {
		seqtable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		seqtable.setFillsViewportHeight(true);

		SeqInfoTableModel mod = new SeqInfoTableModel(null);
		seqtable.setModel(mod);	// Force immediate visibility of column headers (although there's no data).
		//BFTAG set column sizes
		changeColumnWidths();

		JScrollPane scroller = new JScrollPane(seqtable);
		scroller.setBorder(BorderFactory.createCompoundBorder(
				scroller.getBorder(),
				BorderFactory.createEmptyBorder(0, 2, 0, 2)));

		this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		this.add(Box.createRigidArea(new Dimension(0, 5)));
		this.add(scroller);

		this.setBorder(BorderFactory.createTitledBorder("Read Info"));
		gmodel.addGroupSelectionListener(this);
		gmodel.addSeqSelectionListener(this);
		lsm = seqtable.getSelectionModel();
		lsm.addListSelectionListener(this);
	}

	/**
	 * Refresh seqtable if more chromosomes are added, for example.
	 */
	public static void refreshTable() {
		seqtable.validate();
		seqtable.updateUI();
		seqtable.repaint();
		updateTableHeader();
	}

	public static void updateTableHeader() {
//		JTableHeader headers = seqtable.getTableHeader();
//		TableColumnModel model = headers.getColumnModel();
//
//		TableColumn col1 = model.getColumn(0);
//		col1.setHeaderValue("(" + seqtable.getRowCount() + ") Sequence(s)");

		changeColumnWidths();
	}

	private static void changeColumnWidths(){
		// Reads
		seqtable.getColumnModel().getColumn(0).setMinWidth(50);
		seqtable.getColumnModel().getColumn(0).setPreferredWidth(50);

		// "hide" ID
		seqtable.getColumnModel().getColumn(5).setMinWidth(0);
		seqtable.getColumnModel().getColumn(5).setMaxWidth(0);
		seqtable.getColumnModel().getColumn(5).setPreferredWidth(0);
		seqtable.getColumnModel().getColumn(5).setWidth(0);
//		try{
		// BFTAG
		// geht so nicht, da später auf Inhalt zugegriffen werden muss
		// über seqtable.getModel().Column funktioniert das nicht, da nach sortiertung
		// anderer Inhalt bei getValueAt() zurückgegeben wird
//			seqtable.removeColumn(seqtable.getColumnModel().getColumn(5));
//		}catch(Exception e){
//			// do nothing
//		}
	}

	public void groupSelectionChanged(GroupSelectionEvent evt) {
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		if (SeqInfoView.DEBUG_EVENTS) {
			System.out.println("SeqInfoView received groupSelectionChanged() event");
			if (group == null) {
				System.out.println("  group is null");
			} else {
				System.out.println("  group: " + group.getID());
				System.out.println("  seq count: " + group.getSeqCount());
			}
		}
		if (previousGroup == group) {
			if (group == null) {
				return;
			}
			warnAboutNewlyAddedChromosomes(previousSeqCount, group);
		}

		previousGroup = group;
		previousSeqCount = group == null ? 0 : group.getSeqCount();


		SeqInfoTableModel mod = new SeqInfoTableModel(group);

		sorter = new TableRowSorter<SeqInfoTableModel>(mod) {

			@Override
			public Comparator<?> getComparator(int column) {
				switch (column) {
//					case 0:
//						return String.CASE_INSENSITIVE_ORDER;
					case 0:
						return new Comparator()
						{
							public int compare(Object a, Object b)
							{
								return ((Integer)a).compareTo((Integer)b);
							}
						};
					case 1:
						return String.CASE_INSENSITIVE_ORDER;
					case 2:
						return String.CASE_INSENSITIVE_ORDER;
					case 3:
						return String.CASE_INSENSITIVE_ORDER;
				}
				return null;
			}
		};

		// BFTAG sort by read-counter descending
		sortKeys.clear();
		sortKeys.add(new RowSorter.SortKey(0, SortOrder.DESCENDING));
		sorter.setSortKeys(sortKeys);

		selected_seq = null;
		seqtable.setModel(mod);
		seqtable.setRowSorter(sorter);

		refreshTable();

		if (group != null && most_recent_seq_id != null) {
			// When changing genomes, try to keep the same chromosome selected when possible
			BioSeq aseq = group.getSeq(most_recent_seq_id);
			if (aseq != null) {
				gmodel.setSelectedSeq(aseq);
			}
		}
	}

	private static void warnAboutNewlyAddedChromosomes(int previousSeqCount, AnnotatedSeqGroup group) {
		if (previousSeqCount > group.getSeqCount()) {
			System.out.println("WARNING: chromosomes have been added");
			if (previousSeqCount < group.getSeqCount()) {
				System.out.print("New chromosomes:");
				for (int i = previousSeqCount; i < group.getSeqCount(); i++) {
					System.out.print(" " + group.getSeq(i).getID());
				}
				System.out.println();
			}
		}
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (SeqInfoView.DEBUG_EVENTS) {
			System.out.println("SeqInfoView received seqSelectionChanged() event: seq is " + evt.getSelectedSeq());
		}
		synchronized (seqtable) {  // or should synchronize on lsm?
			lsm.removeListSelectionListener(this);
			selected_seq = evt.getSelectedSeq();
			if (selected_seq == null) {
				seqtable.clearSelection();
			} else {
				most_recent_seq_id = selected_seq.getID();

				int rowCount = seqtable.getRowCount();
				for (int i = 0; i < rowCount; i++) {
					// should be able to use == here instead of equals(), because table's model really returns seq.getID()
					if (most_recent_seq_id == seqtable.getValueAt(i, 5)) { //BFTAG col 5 enthält die ID
						if (seqtable.getSelectedRow() != i) {
							seqtable.setRowSelectionInterval(i, i);
							scrollTableLater(seqtable, i);
						}
						break;
					}
				}
			}
			lsm.addListSelectionListener(this);
		}
	}

	// Scroll the table such that the selected row is visible
	void scrollTableLater(final JTable table, final int i) {
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				// Check the row count first since this is multi-threaded
				if (table.getRowCount() >= i) {
					DisplayUtils.scrollToVisible(table, i, 0);
				}
			}
		});
	}

	public void valueChanged(ListSelectionEvent evt) {
		Object src = evt.getSource();
		if ((src == lsm) && (!evt.getValueIsAdjusting())) { // ignore extra messages
			if (SeqInfoView.DEBUG_EVENTS) {
				System.out.println("SeqInfoView received valueChanged() ListSelectionEvent");
			}
			int srow = seqtable.getSelectedRow();
			if (srow >= 0) {
				String seq_name = (String) seqtable.getValueAt(srow, 5); //BFTAG col 5 enthält die ID
				selected_seq = gmodel.getSelectedSeqGroup().getSeq(seq_name);
				if (selected_seq != gmodel.getSelectedSeq()) {
					gmodel.setSelectedSeq(selected_seq);
				}
			}
		}
	}

	@Override
	public Dimension getMinimumSize() {
		return new Dimension(220, 50);
	}

	@Override
	public Dimension getPreferredSize() {
		return new Dimension(220, 50);
	}
}