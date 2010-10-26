/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.SeqSymmetry;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.event.SymSelectionEvent;
import com.affymetrix.genometryImpl.event.SymSelectionListener;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.TierLabelManager;

public class AltSpliceView extends JComponent
				implements ActionListener, ComponentListener, ItemListener,
				SymSelectionListener, SeqSelectionListener,
				TierLabelManager.PopupListener {

	private final AltSpliceSeqMapView spliced_view;
	private final OrfAnalyzer orf_analyzer;
	private final JTextField buffer_sizeTF;
	private final JCheckBox slice_by_selectionCB;
	private List<SeqSymmetry> last_selected_syms = new ArrayList<SeqSymmetry>();
	private BioSeq last_seq_changed = null;
	private boolean pending_sequence_change = false;
	private boolean pending_selection_change = false;
	private boolean slice_by_selection_on = true;

	public AltSpliceView() {
		this.setLayout(new BorderLayout());
		spliced_view = new AltSpliceSeqMapView(false);
		spliced_view.subselectSequence = false;
		orf_analyzer = new OrfAnalyzer(spliced_view);
		buffer_sizeTF = new JTextField(4);
		buffer_sizeTF.setText("" + spliced_view.getSliceBuffer());
		slice_by_selectionCB = new JCheckBox("Slice By Selection", true);

		JPanel buf_adjustP = new JPanel(new FlowLayout());
		buf_adjustP.add(new JLabel("Slice Buffer: "));
		buf_adjustP.add(buffer_sizeTF);

		JPanel pan1 = new JPanel(new GridLayout(1, 2));

		pan1.add(slice_by_selectionCB);
		pan1.add(buf_adjustP);
		JPanel options_panel = new JPanel(new BorderLayout());

		options_panel.add("West", pan1);
		options_panel.add("East", orf_analyzer);
		JSplitPane splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitpane.setResizeWeight(1);  // allocate as much space as possible to top panel
		splitpane.setDividerSize(8);
		splitpane.setTopComponent(spliced_view);
		splitpane.setBottomComponent(options_panel);
		this.add("Center", splitpane);

		this.addComponentListener(this);
		buffer_sizeTF.addActionListener(this);
		slice_by_selectionCB.addItemListener(this);

		GenometryModel.getGenometryModel().addSeqSelectionListener(this);
		GenometryModel.getGenometryModel().addSymSelectionListener(this);

		TierLabelManager tlman = spliced_view.getTierManager();
		if (tlman != null) {
			tlman.addPopupListener(this);
		}
	}

	/**
	 *  This method is notified when selected symmetries change.
	 *  It usually triggers a re-computation of the sliced symmetries to draw.
	 *  If no selected syms, then don't change.
	 *  Any Graphs in the selected symmetries will be ignored
	 *  (because graphs currently span entire sequence and slicing on them can
	 *  use too much memory).
	 */
	public void symSelectionChanged(SymSelectionEvent evt) {
		if (Application.DEBUG_EVENTS) {
			System.out.println("AltSpliceView received selection changed event");
		}
		Object src = evt.getSource();
		// ignore if symmetry selection originated from this AltSpliceView -- don't want to
		//   reslice based on internal selection!
		if ((src != this) && (src != spliced_view)) {
			// catching spliced_view as source of event because currently sym selection events actually originating
			//    from AltSpliceView have their source set to the AltSpliceView's internal SeqMapView...
			last_selected_syms = evt.getSelectedSyms();
			last_selected_syms = removeGraphs(last_selected_syms);
			if (last_selected_syms.size() > 0) {
				if (!this.isShowing()) {
					pending_selection_change = true;
				} else if (slice_by_selection_on) {
					this.sliceAndDice(last_selected_syms);
					pending_selection_change = false;
				} else {
					spliced_view.select(last_selected_syms);
					pending_selection_change = false;
				}
			}
		}
	}

	/**
	 * takes a list of SeqSymmetries and removes any GraphSyms from it.
	 * @param syms
	 * @return
	 */
	private static List<SeqSymmetry> removeGraphs(List<SeqSymmetry> syms) {
		List<SeqSymmetry> v = new ArrayList<SeqSymmetry>(syms.size());
		for (SeqSymmetry sym : syms) {
			if (!(sym instanceof GraphSym)) {
				v.add(sym);
			}
		}
		return v;
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if (Application.DEBUG_EVENTS) {
			System.out.println("AltSpliceView received SeqSelectionEvent, selected seq: " + evt.getSelectedSeq());
		}
		BioSeq newseq = GenometryModel.getGenometryModel().getSelectedSeq();
		if (last_seq_changed != newseq) {
			last_seq_changed = newseq;
			if (this.isShowing() && slice_by_selection_on) {
				spliced_view.setAnnotatedSeq(last_seq_changed);
				pending_sequence_change = false;
			} else {
				pending_sequence_change = true;
			}
		}
	}

	private void setSliceBySelection(boolean b) {
		slice_by_selection_on = b;
	}

	private void setSliceBuffer(int buf_size) {
		spliced_view.setSliceBuffer(buf_size);
		orf_analyzer.redoOrfs();
	}

	private void sliceAndDice(List<SeqSymmetry> syms) {
		if (syms.size() > 0) {
			spliced_view.sliceAndDice(syms);
			orf_analyzer.redoOrfs();
		}
	}

	// ComponentListener implementation
	public void componentResized(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentShown(ComponentEvent e) {
		if (pending_sequence_change && slice_by_selection_on) {
			spliced_view.setAnnotatedSeq(last_seq_changed);
			pending_sequence_change = false;
		}
		if (pending_selection_change) {
			if (slice_by_selection_on) {
				this.sliceAndDice(last_selected_syms);
			} else {
				spliced_view.select(last_selected_syms);
			}
			pending_selection_change = false;
		}
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (src == buffer_sizeTF) {
			String str = buffer_sizeTF.getText();
			if (str != null) {
				int new_buf_size = Integer.parseInt(str);
				this.setSliceBuffer(new_buf_size);
			}
		}
	}

	public void itemStateChanged(ItemEvent evt) {
		Object src = evt.getSource();
		if (src == slice_by_selectionCB) {
			setSliceBySelection(evt.getStateChange() == ItemEvent.SELECTED);
		}
	}

	public void popupNotify(JPopupMenu popup, final TierLabelManager handler) {
		if (handler != spliced_view.getTierManager()) {
			return;
		}

		Action hide_action = new AbstractAction("Hide Tier") {

			public void actionPerformed(ActionEvent e) {
				spliced_view.doEdgeMatching(Collections.<GlyphI>emptyList(), false);
				handler.hideTiers(handler.getSelectedTierLabels(), false, true);
			}
		};

		Action restore_all_action = new AbstractAction("Show All") {

			public void actionPerformed(ActionEvent e) {
				// undo all edge-matching, because packing will behave badly otherwise.
				spliced_view.doEdgeMatching(Collections.<GlyphI>emptyList(), false);
				handler.showTiers(handler.getAllTierLabels(), true, true);
			}
		};

		hide_action.setEnabled(!handler.getSelectedTierLabels().isEmpty());
		restore_all_action.setEnabled(true);

		if (popup.getComponentCount() > 0) {
			popup.add(new JSeparator());
		}
		popup.add(hide_action);
		popup.add(restore_all_action);
	}

	public SeqMapView getSplicedView() {
		return spliced_view;
	}
}
