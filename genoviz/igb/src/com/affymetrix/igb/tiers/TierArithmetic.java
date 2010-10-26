package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;

import com.affymetrix.genometryImpl.SeqSymSummarizer;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.view.SeqMapView;

/**
 *  A PopupListener that adds the ability to create "union", "intersection", etc.,
 *  tiers based on selected annotation tiers.  Is not used on graph tiers.
 */
public final class TierArithmetic implements TierLabelManager.PopupListener {
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private final SeqMapView gviewer;
	private final TierLabelManager handler;
	private final JMenu combineMenu = new JMenu("Combine Selected Tracks");
	private final JMenuItem intersectMI = new JMenuItem("Intersect");
	private final JMenuItem unionMI = new JMenuItem("Union");
	private final JMenuItem a_not_b_MI = new JMenuItem("A not B");
	private final JMenuItem b_not_a_MI = new JMenuItem("B not A");
	private final JMenuItem xorMI = new JMenuItem("Xor");
	private final JMenuItem notMI = new JMenuItem("Not");

	private final ActionListener action_listener = new ActionListener() {

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();

			if (src == intersectMI) {
				addIntersectTier();
			} else if (src == unionMI) {
				addUnionTier();
			} else if (src == a_not_b_MI) {
				addExclusiveTier(true);
			} else if (src == b_not_a_MI) {
				addExclusiveTier(false);
			} else if (src == xorMI) {
				addXorTier();
			} else if (src == notMI) {
				addNotTier();
			}
		}
	};

	public TierArithmetic(TierLabelManager handler, SeqMapView gviewer) {
		this.handler = handler;
		this.gviewer = gviewer;

		intersectMI.addActionListener(this.action_listener);
		unionMI.addActionListener(this.action_listener);
		a_not_b_MI.addActionListener(this.action_listener);
		b_not_a_MI.addActionListener(this.action_listener);
		xorMI.addActionListener(this.action_listener);
		notMI.addActionListener(this.action_listener);

		combineMenu.add(intersectMI);
		combineMenu.add(unionMI);
		combineMenu.add(a_not_b_MI);
		combineMenu.add(b_not_a_MI);
		combineMenu.add(xorMI);
		combineMenu.add(notMI);
	}

	private void addUnionTier() {
		List<TierGlyph> selected = handler.getSelectedTiers();
		if (!selected.isEmpty()) {
			addUnionTier(selected);
		} else {
			ErrorHandler.errorPanel("Must select one or more annotation tracks for union");
		}
	}

	private void addExclusiveTier(boolean exclusiveA) {
		List<TierGlyph> selected = handler.getSelectedTiers();
		if (selected.size() == 2) {
			TierGlyph tierA = selected.get(0);
			TierGlyph tierB = selected.get(1);
			addExclusiveTier(tierA, tierB, exclusiveA);
		} else {
			ErrorHandler.errorPanel("Must select two and only two tracks for union");
		}
	}

	private void addXorTier() {
		List<TierGlyph> selected = handler.getSelectedTiers();
		if (selected.size() == 2) {
			TierGlyph tierA = selected.get(0);
			TierGlyph tierB = selected.get(1);
			addXorTier(tierA, tierB);
		} else {
			ErrorHandler.errorPanel("Must select two and only two tracks for XOR(A,B)");
		}
	}

	private void addNotTier() {
		List<TierGlyph> selected = handler.getSelectedTiers();
		if (selected.size() == 1) {
			TierGlyph tierA = selected.get(0);
			addNotTier(tierA);
		} else {
			ErrorHandler.errorPanel("Must select one and only one track for NOT(A)");
		}
	}

	private void addIntersectTier() {
		List<TierGlyph> selected = handler.getSelectedTiers();
		if (selected.size() == 2) {
			TierGlyph tierA = selected.get(0);
			TierGlyph tierB = selected.get(1);
			addIntersectTier(tierA, tierB);
		} else {
			ErrorHandler.errorPanel("Must select two and only two tracks for intersection");
		}
	}

	private void addNotTier(TierGlyph tierA) {
		BioSeq aseq = gmodel.getSelectedSeq();
		List<SeqSymmetry> listA = findChildSyms(tierA);
		if (listA.isEmpty()) {
			ErrorHandler.errorPanel("Illegal Operation",
					"Cannot perform this operation on this track.");
			return;
		}
		SeqSymmetry inverse_sym = SeqSymSummarizer.getNot(listA, aseq);
		if (inverse_sym != null) {
			String method = "not: " + tierA.getLabel();
			makeNonPersistentStyle((SymWithProps) inverse_sym, method);
			aseq.addAnnotation(inverse_sym);
			gviewer.setAnnotatedSeq(aseq, true, true);
		}
	}

	private void addExclusiveTier(TierGlyph tierA, TierGlyph tierB, boolean exclusiveA) {
		BioSeq aseq = gmodel.getSelectedSeq();
		List<SeqSymmetry> listA = findChildSyms(tierA);
		List<SeqSymmetry> listB = findChildSyms(tierB);
		
		SeqSymmetry exclusive_sym;
		if (exclusiveA) {
			exclusive_sym = SeqSymSummarizer.getExclusive(listA, listB, aseq);
		} else {
			exclusive_sym = SeqSymSummarizer.getExclusive(listB, listA, aseq);
		}
		if (exclusive_sym != null) {
			String method;
			if (exclusiveA) {
				method = "A not B:" + tierA.getLabel() + ", " + tierB.getLabel();
			} else {
				method = "B not A:" + tierB.getLabel() + ", " + tierA.getLabel();
			}
			addStyleAndAnnotation(exclusive_sym, method, aseq);
		}
	}

	private void addXorTier(TierGlyph tierA, TierGlyph tierB) {
		BioSeq aseq = gmodel.getSelectedSeq();
		List<SeqSymmetry> listA = findChildSyms(tierA);
		List<SeqSymmetry> listB = findChildSyms(tierB);

		SeqSymmetry xor_sym = SeqSymSummarizer.getXor(listA, listB, aseq);
		if (xor_sym != null) {
			String method = "xor: " + tierA.getLabel() + ", " + tierB.getLabel();
			addStyleAndAnnotation(xor_sym, method, aseq);
		}
	}

	private void addUnionTier(List<TierGlyph> tiers) {
		BioSeq aseq = gmodel.getSelectedSeq();
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
		for (TierGlyph tier : tiers) {	
			findChildSyms(tier, syms);
		}
		SeqSymmetry union_sym = SeqSymSummarizer.getUnion(syms, aseq);
		if (union_sym != null) {
			StringBuilder meth = new StringBuilder();
			meth.append("union: ");
			for (TierGlyph tier : tiers) {
				meth.append(tier.getLabel()).append(", ");
			}
			addStyleAndAnnotation(union_sym, meth.toString(), aseq);
		}
	}

	private void addIntersectTier(TierGlyph tierA, TierGlyph tierB) {
		BioSeq aseq = gmodel.getSelectedSeq();
		List<SeqSymmetry> listA = findChildSyms(tierA);
		List<SeqSymmetry> listB = findChildSyms(tierB);
		SeqSymmetry intersect_sym = SeqSymSummarizer.getIntersection(listA, listB, aseq);

		if (intersect_sym != null) {
			String method = "intersect: " + tierA.getLabel() + ", " + tierB.getLabel();
			addStyleAndAnnotation(intersect_sym, method, aseq);
		}
	}
	
	private static List<SeqSymmetry> findChildSyms(TierGlyph tiers) {
		List<SeqSymmetry> list = new ArrayList<SeqSymmetry>();
		findChildSyms(tiers, list);
		return list;
	}
	private static void findChildSyms(TierGlyph tiers, List<SeqSymmetry> list) {
		for (GlyphI child : tiers.getChildren()) {
			SeqSymmetry csym = (SeqSymmetry) child.getInfo();
			if (csym != null) {
				list.add(csym);
			}
		}
	}
	
	private void addStyleAndAnnotation(SeqSymmetry sym, String method, BioSeq aseq) {
		makeNonPersistentStyle((SymWithProps) sym, method);
		aseq.addAnnotation(sym);
		gviewer.setAnnotatedSeq(aseq, true, true);
	}

	public void popupNotify(JPopupMenu popup, TierLabelManager handler) {
		if (handler != this.handler) {
			throw new RuntimeException("");
		}
		List<TierLabelGlyph> labels = handler.getSelectedTierLabels();
		int num_selected = labels.size();
		boolean all_are_annotations = areAllAnnotations(labels);

		intersectMI.setEnabled(all_are_annotations && num_selected == 2);
		unionMI.setEnabled(all_are_annotations && num_selected > 0);
		a_not_b_MI.setEnabled(all_are_annotations && num_selected == 2);
		b_not_a_MI.setEnabled(all_are_annotations && num_selected == 2);
		notMI.setEnabled(all_are_annotations && num_selected == 1);
		xorMI.setEnabled(all_are_annotations && num_selected == 2);
		combineMenu.setEnabled(all_are_annotations && num_selected > 0);

		popup.add(combineMenu);
	}

	private static boolean areAllAnnotations(List<TierLabelGlyph> labels) {
		for (TierLabelGlyph tlg : labels) {
			if (tlg.getReferenceTier().getAnnotStyle().isGraphTier()) {
				return false;
			}
		}
		return true;
	}

	private static TrackStyle makeNonPersistentStyle(SymWithProps sym, String human_name) {
		// Needs a unique name so that if any later tier is produced with the same
		// human name, it will not automatically get the same color, etc.
		String unique_name = TrackStyle.getUniqueName(human_name);
		sym.setProperty("method", unique_name);
		TrackStyle style = TrackStyle.getInstance(unique_name, false);
		style.setHumanName(human_name);
		style.setGlyphDepth(1);
		style.setSeparate(false); // there are not separate (+) and (-) strands
		style.setCustomizable(false); // the user can change the color, but not much else is meaningful
		return style;
	}
}
