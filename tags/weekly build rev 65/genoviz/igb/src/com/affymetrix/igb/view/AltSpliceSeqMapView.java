package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.symmetry.SimplePairSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.tiers.TierGlyph;
import java.awt.Component;
import java.util.List;

final class AltSpliceSeqMapView extends SeqMapView {
	private static final long serialVersionUID = 1l;
	private boolean slicing_in_effect = false;
	/**
	 *  number of bases that slicer tries to buffer on each side of every span it is using to guide slicing
	 */
	private int slice_buffer = 100;
	/**
	 *  current symmetry used to determine slicing
	 */
	private SeqSymmetry slice_symmetry;
	private Thread slice_thread = null;

	AltSpliceSeqMapView(boolean b) {
		super(b);
		if (tier_manager != null) {
			tier_manager.setDoGraphSelections(false);
		}
		report_hairline_position_in_status_bar = false;
		report_status_in_status_bar = false;
	}

	@Override
	public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view) {
		if (coord_shift) {
			// ignore the preserve_view parameter, always pretend it is false in the splice view
			super.setAnnotatedSeq(seq, preserve_selection, false);
		} else {
			this.clear();
			this.aseq = seq;
			this.viewseq = seq;
		}
	}

	// override so that the tier glyph in the splice view has a different
	// AnnotStyle from the TierGlyph in the main view.  (So that if we hide it,
	// it won't become hidden in the main view.)
	@Override
	public TierGlyph[] getTiers(boolean next_to_axis, ITrackStyleExtended style) {
		// Create a temporary style with all the properties of the given style.
		// Thus any changes to this copy in the slice view
		// will not affect the original in the main view
		TrackStyle style_copy = new TrackStyle() {
		};
		style_copy.copyPropertiesFrom(style);
		TierGlyph[] glyphs = super.getTiers(next_to_axis, style_copy);
		// super.getTiers() may have created a brand new tier, in which case
		// the style is already set to "style_copy", or it may have re-used
		// a tier, in which case it may still have an old copy of the style
		// associated with it.  Reset the style to be certain.
		glyphs[0].setStyle(style_copy);
		glyphs[1].setStyle(style_copy);
		return glyphs;
	}

	protected final void setSliceBuffer(int bases) {
		slice_buffer = bases;
		if (slicing_in_effect) {
			stopSlicingThread();

			slice_thread = new Thread() {

				@Override
				public void run() {
					enableSeqMap(false);
					sliceAndDiceNow(slice_symmetry);
					enableSeqMap(true);
				}
			};
			slice_thread.start();
		}
	}

	final int getSliceBuffer() {
		return slice_buffer;
	}

	public final void sliceAndDice(final List<SeqSymmetry> syms) {
		stopSlicingThread();

		slice_thread = new Thread() {

			@Override
			public void run() {
				enableSeqMap(false);
				SimpleSymWithProps unionSym = new SimpleSymWithProps();
				SeqUtils.union(syms, unionSym, aseq);
				sliceAndDiceNow(unionSym);
				enableSeqMap(true);
			}
		};

		slice_thread.start();
	}

	// disables the sliced view while the slicing thread works
	private void enableSeqMap(boolean b) {
		seqmap.setVisible(b);
		if (map_range_box != null) {
			if (!b) {
				map_range_box.range_box.setText("Working...");
			}
		}
		Component[] comps = xzoombox.getComponents();
		for (int i = 0; i < comps.length; i++) {
			comps[i].setEnabled(b);
		}
		comps = yzoombox.getComponents();
		for (int i = 0; i < comps.length; i++) {
			comps[i].setEnabled(b);
		}
	}

	@SuppressWarnings("deprecation")
	private void stopSlicingThread() {
		if (slice_thread != null
				&& slice_thread != Thread.currentThread()
				&& slice_thread.isAlive()) {
			slice_thread.stop(); // TODO: Deprecated, but seems OK here.  Maybe fix later.
			slice_thread = null;
			enableSeqMap(true);
		}
	}

	/**
	 *  Performs a genometry-based slice-and-dice.
	 *  Assumes that symmetry children are ordered by child.getSpan(aseq).getMin().
	 */
	private void sliceAndDiceNow(SeqSymmetry sym) {
		int childCount = (sym == null) ? 0 : sym.getChildCount();
		if (childCount == 0) {
			return;
		}

		this.viewseq = new BioSeq("view_seq", "", aseq.getLength());
		slice_symmetry = sym;
		coord_shift = true;

		if (seq2viewSym == null) {
			seq2viewSym = new SimpleMutableSeqSymmetry();
		} else {
			seq2viewSym.clear();
		}

		// rebuild seq2viewSym as a symmetry mapping slices of aseq to abut next to each other
		//    mapped to viewseq
		int prev_max = 0;
		int slice_offset = 0;
		MutableSeqSpan prev_seq_slice = null;
		MutableSeqSpan prev_view_slice = null;
		for (int i = 0; i < childCount; i++) {
			SeqSymmetry child = sym.getChild(i);
			SeqSpan exact_span = child.getSpan(aseq);
			if (exact_span == null) {
				continue;
			}  // skip any children that don't have a span in aseq
			int next_min;
			if (i == (childCount - 1)) {
				next_min = aseq.getLength();
			} else {
				next_min = sym.getChild(i + 1).getSpan(aseq).getMin();
			}

			int slice_min = Math.max(prev_max, (exact_span.getMin() - slice_buffer));
			int slice_max = Math.min(next_min, (exact_span.getMax() + slice_buffer));
			MutableSeqSpan seq_slice_span = new SimpleMutableSeqSpan(slice_min, slice_max, aseq);

			int slice_length = seq_slice_span.getLength();
			MutableSeqSpan view_slice_span =
							new SimpleMutableSeqSpan(slice_offset, slice_offset + slice_length, viewseq);

			if (prev_seq_slice != null && SeqUtils.looseOverlap(prev_seq_slice, seq_slice_span)) {
				// if new seq slice span abuts the old one, then just
				// lengthen existing spans (seq and view) rather than adding new ones
				SeqUtils.encompass(prev_seq_slice, seq_slice_span, prev_seq_slice);
				SeqUtils.encompass(prev_view_slice, view_slice_span, prev_view_slice);
			} else {
				addIntronTransforms(prev_seq_slice, seq_slice_span, view_slice_span);

				SeqSymmetry slice_sym = new SimplePairSeqSymmetry(seq_slice_span, view_slice_span);
				seq2viewSym.addChild(slice_sym);

				prev_seq_slice = seq_slice_span;
				prev_view_slice = view_slice_span;
			}
			slice_offset += slice_length;
			prev_max = slice_max;
		}

		SeqSpan seq_span = SeqUtils.getChildBounds(seq2viewSym, aseq);
		SeqSpan view_span = SeqUtils.getChildBounds(seq2viewSym, viewseq);
		seq2viewSym.addSpan(seq_span);
		seq2viewSym.addSpan(view_span);

		viewseq.setComposition(seq2viewSym);
		viewseq.setBounds(view_span.getMin(), view_span.getMax());
		transform_path = new SeqSymmetry[1];
		transform_path[0] = seq2viewSym;
		slicing_in_effect = true;

		setAnnotatedSeq(aseq);
	}

	/**
	 * Add spans to the transformation sym that will cause all
	 * "intron" spans in the regions of aseq between exons chosen for slicing
	 * to be transformed into zero-length spans.
	 * This allows glyph factories to find "deleted" exons
	 * and draw them (if desired) without requiring messy calculations in
	 * the glyph factories.
	 * @param prev_seq_slice
	 * @param seq_slice_span
	 * @param view_slice_span
	 */
	private void addIntronTransforms(MutableSeqSpan prev_seq_slice, MutableSeqSpan seq_slice_span, MutableSeqSpan view_slice_span) {
		if (prev_seq_slice != null) {
			SeqSpan intron_region_span = new SimpleSeqSpan(prev_seq_slice.getMax(), seq_slice_span.getMin(), aseq);
			SeqSpan zero_length_span = new SimpleSeqSpan(view_slice_span.getMin(), view_slice_span.getMin(), viewseq);
			// SimplePairSeqSymmetry is better than EfficientPairSeqSymmetry here,
			// since there will be frequent calls to getSpan(BioSeq)
			seq2viewSym.addChild(new SimplePairSeqSymmetry(intron_region_span, zero_length_span));
		}
	}

	@Override
	public void setAnnotatedSeq(BioSeq seq, boolean preserve_selection, boolean preserve_view_x, boolean preserve_view_y) {
		stopSlicingThread();
		super.setAnnotatedSeq(seq, preserve_selection, preserve_view_x, preserve_view_y);
	}

	@Override
	public void clear() {
		stopSlicingThread();
		super.clear();
	}
}
