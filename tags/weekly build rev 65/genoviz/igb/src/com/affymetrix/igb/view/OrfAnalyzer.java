/**
 *   Copyright (c) 2001-2005 Affymetrix, Inc.
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

import cern.colt.list.IntArrayList;
import com.affymetrix.genometryImpl.SeqSpan;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import com.affymetrix.igb.glyph.FlyPointLinkerGlyph;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.style.DefaultTrackStyle;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.tiers.TransformTierGlyph;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.util.ErrorHandler;

/**
 *  OrfAnalyzer2 is used on the virtual sequence being viewed in AltSpliceView.  It does
 *  not incorporate content stat graphs (GC, dicodon, etc.).
 */
public final class OrfAnalyzer extends JComponent
		implements ChangeListener, ActionListener {

	public static final String PREF_STOP_CODON_COLOR = "stop codon";
	public static final String PREF_DYNAMIC_ORF_COLOR = "dynamic orf";
	public static final Color default_stop_codon_color = new Color(200, 150, 150);
	public static final Color default_dynamic_orf_color = new Color(100, 200, 100);
	// GAH 8-23-2004
	// As IGB is currently configured, smv should be set to the internal SeqMapView of the AltSpliceView...
	private final SeqMapView smv;
	private JSlider orf_thresh_slider;
	private JCheckBox showCB;
	private JLabel orf_threshL;
	private JLabel orfL;
	private TransformTierGlyph fortier;
	private TransformTierGlyph revtier;
	private int current_orf_thresh = 300;
	private static final int orf_thresh_min = 10;
	private static final int orf_thresh_max = 500;
	private static final int max_analysis_span = 1000000;
	private boolean show_orfs;
	private final List<FlyPointLinkerGlyph> orf_holders = new ArrayList<FlyPointLinkerGlyph>();
	private static final String[] stop_codons = {"TAA", "TAG", "TGA", "TTA", "CTA", "TCA"};

	public OrfAnalyzer(SeqMapView view) {
		super();
		smv = view;
		init();
	}

	public void init() {

		orfL = new JLabel("Min ORF Length:", SwingConstants.CENTER);
		orf_threshL = new JLabel(Integer.toString(current_orf_thresh), SwingConstants.CENTER);
		showCB = new JCheckBox("Analyze ORFs");
		orf_thresh_slider = new JSlider(JSlider.HORIZONTAL, orf_thresh_min, orf_thresh_max, current_orf_thresh);
		JPanel pan1 = new JPanel();
		pan1.setLayout(new FlowLayout());
		pan1.add(orfL);
		pan1.add(orf_threshL);

		this.setLayout(new FlowLayout());
		this.add(showCB);
		this.add(pan1);
		this.add(orf_thresh_slider);

		showCB.addActionListener(this);
		orf_thresh_slider.addChangeListener(this);
	}

	public void stateChanged(ChangeEvent evt) {
		Object src = evt.getSource();
		if (src == orf_thresh_slider) {
			current_orf_thresh = orf_thresh_slider.getValue();
			for (FlyPointLinkerGlyph fw : orf_holders) {
				fw.setMinThreshold(current_orf_thresh);
			}
			AffyTieredMap map = smv.getSeqMap();
			map.updateWidget();
			orf_threshL.setText(Integer.toString(current_orf_thresh));
		}
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (src == showCB) {
			show_orfs = ((JCheckBox) src).isSelected();
			if (show_orfs) {
				redoOrfs();
			} else {
				removeTiersAndCleanup();
				adjustMap();
			}
		}
	}

	void redoOrfs() {
		if (smv == null) {
			return;
		}
		BioSeq vseq = smv.getViewSeq();
		if (vseq == null) {
			return;
		}
		removeTiersAndCleanup();
		if (!show_orfs) {
			return;
		}
		
		if (!(vseq.isComplete())) {
			ErrorHandler.errorPanel("Cannot perform ORF analysis: must first load residues for sequence");
			show_orfs = false;
			showCB.setSelected(false);
			return;
		}

		SeqSpan vspan = smv.getVisibleSpan();
		int span_start = vspan.getMin();
		int span_end = vspan.getMax();
		if (span_start < 0 || span_end < span_start) {
			ErrorHandler.errorPanel("Cannot perform ORF analysis: first select a sliced region");
			show_orfs = false;
			showCB.setSelected(false);
			return;
		}

		fortier = new TransformTierGlyph(new DefaultTrackStyle());
		fortier.setLabel("Stop Codons");
		fortier.setFixedPixHeight(25);
		fortier.setFillColor(Color.darkGray);
		fortier.setDirection(TierGlyph.Direction.FORWARD);

		AffyTieredMap map = smv.getSeqMap();
		map.addTier(fortier, true);  // put forward tier above axis

		revtier = new TransformTierGlyph(new DefaultTrackStyle());
		revtier.setLabel("Stop Codons");
		revtier.setFixedPixHeight(25);
		revtier.setFillColor(Color.darkGray);
		revtier.setDirection(TierGlyph.Direction.REVERSE);
		map.addTier(revtier, false);  // put reverse tier below axis

		Color pointcol = PreferenceUtils.getColor(PreferenceUtils.getTopNode(), PREF_STOP_CODON_COLOR, default_stop_codon_color);
		Color linkcol = PreferenceUtils.getColor(PreferenceUtils.getTopNode(), PREF_DYNAMIC_ORF_COLOR, default_dynamic_orf_color);

		int span_mid = (int) (0.5f * span_start + 0.5f * span_end);

		span_start = span_mid - (max_analysis_span / 2);
		span_start = Math.max(0, span_start);	// shouldn't have negative start
		span_start -= span_start % 3;
		span_end = span_mid + (max_analysis_span / 2);
		span_end -= span_end % 3;

		int residue_offset = vseq.getMin();
		IntArrayList[] frame_lists = buildFrameLists(span_start, residue_offset, vseq, span_end);

		for (int frame = 0; frame < 6; frame++) {
			boolean forward = frame <= 2;
			IntArrayList xpos_vec = frame_lists[frame];
			int[] xpos = xpos_vec.elements();
			// must sort positions!  because positions were added to IntList for each type of
			//   stop codon before other types, positions in IntList will _not_ be in
			//   ascending order (though for a particular type, e.g. "TAA", they will be)
			Arrays.sort(xpos);

			GlyphI point_template = new FillRectGlyph();
			point_template.setColor(pointcol);
			point_template.setCoords(residue_offset, 0, vseq.getLength(), 10);

			TierGlyph tier = forward ? fortier : revtier;
			GlyphI orf_glyph = null;
			if (xpos.length > 0) {
				GlyphI link_template = new FillRectGlyph();
				link_template.setColor(linkcol);
				link_template.setCoords(0, 0, 0, 4);  // only height is retained

				FlyPointLinkerGlyph fw = new FlyPointLinkerGlyph(point_template, link_template, xpos, 3,
						span_start, span_end);
				fw.setHitable(false);
				orf_glyph = fw;
				fw.setMinThreshold(current_orf_thresh);
				orf_holders.add(fw);
			} else {
				orf_glyph = new FillRectGlyph() {

					@Override
					public boolean isHitable() {
						return false;
					}
				};
				orf_glyph.setColor(tier.getFillColor());
			}
			// Make orf_glyph as long as vseq; otherwise, two or more could pack into one line.
			// The underlying symmetry may be shorter, so "zoom to selected" won't work.
			// But the glyph not hittable (for other reasons), so this isn't an issue.
			orf_glyph.setCoords(residue_offset, 0, vseq.getLength(), point_template.getCoordBox().height);
			tier.addChild(orf_glyph);
		}
		adjustMap();
	}

	private static IntArrayList[] buildFrameLists(int span_start, int residue_offset, BioSeq vseq, int span_end) {
		int span_length = span_end - span_start;
		IntArrayList[] frame_lists = new IntArrayList[6];
		for (int i = 0; i < 6; i++) {
			// estimating number of stop codons, then padding by 20%
			frame_lists[i] = new IntArrayList((int) ((span_length / 64) / 1.2));
		}
		for (int i = 0; i < stop_codons.length; i++) {
			boolean forward_codon = i <= 2;
			String codon = stop_codons[i];
			int seq_index = span_start;
			int res_index = span_start - residue_offset;
			res_index = caseInsensitiveIndexOfHack(vseq, codon, res_index);
			// need to factor in possible offset of residues string from start of
			//    sequence (for example, when sequence is a CompNegSeq)
			while (res_index >= 0 && (seq_index < span_end)) {
				int frame;
				// need to factor in possible offset of residues string from start of
				//    sequence (for example, when sequence is a CompNegSeq)
				seq_index = res_index + residue_offset;
				if (forward_codon) {
					frame = res_index % 3;	 // forward frames = (0, 1, 2)
				}
				else {
					frame = 3 + (res_index % 3); // reverse frames = (3, 4, 5)
				}

				frame_lists[frame].add(seq_index);
				res_index = caseInsensitiveIndexOfHack(vseq, codon, res_index+1);
			}
		}
		return frame_lists;
	}

	private static int caseInsensitiveIndexOfHack(BioSeq vseq, String codon, int resIndex) {
		int temp_index = vseq.indexOf(codon, resIndex);
		if (temp_index == -1) {
			temp_index = vseq.indexOf(codon.toLowerCase(), resIndex);	// hack for case-insensitivity
		}
		return temp_index;
	}

	private void removeTiersAndCleanup() {
		AffyTieredMap map = smv.getSeqMap();
		if (fortier != null) {
			map.removeTier(fortier);
			fortier = null;
		}
		if (revtier != null) {
			map.removeTier(revtier);
			revtier = null;
		}
		orf_holders.clear();
	}

	private void adjustMap() {
		AffyTieredMap tiermap = smv.getSeqMap();
		tiermap.repack();
		tiermap.stretchToFit(false, true);
		tiermap.updateWidget();
	}
}
