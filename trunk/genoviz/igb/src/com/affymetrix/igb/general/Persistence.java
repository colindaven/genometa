package com.affymetrix.igb.general;

import java.util.prefs.Preferences;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.SeqMapView;

public final class Persistence {
	private final static boolean DEBUG = false;

	private final static String GENOME_ID = "GENOME_ID";  // full genome version ID if gets MD5-compressed in node creation
	private final static String SEQ_ID = "SEQ_ID";  // full seq ID if gets MD5-compressed in node creation
	private final static String SELECTED_GENOME_PREF = "SELECTED_GENOME_PREF";
	private final static String SELECTED_SEQ_PREF = "SELECTED_SEQ_PREF";
	private final static String SEQ_MIN_PREF = "SEQ_MIN_PREF";
	private final static String SEQ_MAX_PREF = "SEQ_MAX_PREF";
	private final static GenometryModel gmodel = GenometryModel.getGenometryModel();

	/**
	 *  Saves information on current group
	 *  Using Preferences node: [igb_root_pref]/genomes/[group_id]
	 *  Using PreferenceUtils to convert node names if they are too long
	 *  tagvals:
	 *      GENOME_ID
	 *      SEQ_ID
	 *      SELECTED_GENOME_PREF
	 *      SELECTED_SEQ_PREF
	 */
	public static void saveCurrentView(SeqMapView gviewer) {
		AnnotatedSeqGroup group = gmodel.getSelectedSeqGroup();
		if (gmodel.getSelectedSeq() != null) {
			BioSeq seq = gmodel.getSelectedSeq();
			saveGroupSelection(group);
			saveSeqSelection(seq);
			saveSeqVisibleSpan(gviewer);
		}
	}

	public static void saveGroupSelection(AnnotatedSeqGroup group) {
		Preferences genomes_node = PreferenceUtils.getGenomesNode();
		if (genomes_node == null || group == null) {
			return;
		}
		genomes_node.put(SELECTED_GENOME_PREF, group.getID());

		Preferences group_node = PreferenceUtils.getSubnode(genomes_node, group.getID(), true);
		//  encodes id via MD5 if too long, also remove forward slashes ("/")
		group_node.put(GENOME_ID, group.getID());  // preserve actual ID, no MD5 encoding, no slash removal

	}

	/**
	 * Restore selection of group.
	 * @return the restored group which is an AnnotatedSeqGroup.
	 */
	public static AnnotatedSeqGroup restoreGroupSelection() {
		Preferences genomes_node = PreferenceUtils.getGenomesNode();
		String group_id = genomes_node.get(SELECTED_GENOME_PREF, "");
		if (group_id == null || group_id.length() == 0) {
			return null;
		}
		if (DEBUG) {
			System.out.println("Attempting to restore group:" + group_id);
		}
		return gmodel.getSeqGroup(group_id);
	}

	/**
	 *  Save information on which seq is currently being viewed
	 *  Using Preferences node: [igb_root_pref]/genomes/[group_id], {SELECTED_SEQ_PREF ==> seq_id }
	 *  Using PreferenceUtils to convert node names if they are too long
	 */
	public static void saveSeqSelection(BioSeq seq) {
		if (seq == null) {
			return;
		}

		AnnotatedSeqGroup current_group = seq.getSeqGroup();
		if (current_group == null) {
			return;
		}
		Preferences genomes_node = PreferenceUtils.getGenomesNode();
		Preferences group_node = PreferenceUtils.getSubnode(genomes_node, current_group.getID(), true);
		//  encodes id via MD5 if too long, removes slashes rather than make deeply nested node hierarchy
		group_node.put(SELECTED_SEQ_PREF, seq.getID());
	}

	/**
	 * Restore the selected chromosome.
	 * @param group
	 * @return restore the selected chromosome which is a BioSeq
	 */
	public static BioSeq restoreSeqSelection(AnnotatedSeqGroup group) {
		Preferences genomes_node = PreferenceUtils.getGenomesNode();
		Preferences group_node = PreferenceUtils.getSubnode(genomes_node, group.getID(), true);
		//  encodes id via MD5 if too long, removes slashes rather than make deeply nested node hierarchy
		String seq_id = group_node.get(SELECTED_SEQ_PREF, "");
		if (seq_id == null || seq_id.length() == 0) {
			return null;
		}
		
		BioSeq seq = group.getSeq(seq_id);
		if (DEBUG) {
		System.out.println("Persistence: seq_id is "+ seq_id + ". seq is " + seq);
		}
		// if selected or default seq can't be found, use first seq in group
		if (seq == null && group.getSeqCount() > 0) {
			seq = group.getSeq(0);
			if (DEBUG) {
			System.out.println("Persistence: seq is now " + seq);
			}
		}
		return seq;
	}

	/**
	 *  Saving visible span info for currently viewed seq
	 *  Uses Preferences node: [igb_root_pref]/genomes/[group_id]/seqs/[seq_id]
	 *                                {SEQ_MIN_PREF ==> viewspan.getMin() }
	 *                                {SEQ_MAX_PREF ==> viewspan.getMax() }
	 *                                {ID ==> seq_id }
	 *  Using PreferenceUtils to convert node names if they are too long
	 */
	public static void saveSeqVisibleSpan(SeqMapView gviewer) {
		SeqSpan visible_span = gviewer.getVisibleSpan();
		if (visible_span != null) {
			BioSeq seq = visible_span.getBioSeq();
			if (seq != null) {
				AnnotatedSeqGroup group = seq.getSeqGroup();
				Preferences genomes_node = PreferenceUtils.getGenomesNode();
				Preferences group_node = PreferenceUtils.getSubnode(genomes_node, group.getID(), true);  //  encodes id via MD5 if too long
				Preferences seqs_node = PreferenceUtils.getSubnode(group_node, "seqs");
				Preferences seq_node = PreferenceUtils.getSubnode(seqs_node, seq.getID(), true);  //  encodes id via MD5 if too long
				seq_node.put(SEQ_ID, seq.getID());   // in case node name is MD5 encoded
				seq_node.putInt(SEQ_MIN_PREF, visible_span.getMin());
				seq_node.putInt(SEQ_MAX_PREF, visible_span.getMax());
			}
		}
	}

	/**
	 *  Assumes that correct seq has already been set in gviewer (usually due to gviewr bein a SeqSelectionListener on gmodel)
	 */
	public static SeqSpan restoreSeqVisibleSpan(SeqMapView gviewer) {
		BioSeq seq = gviewer.getViewSeq();
		if (seq == null) {
			return null;
		}

		AnnotatedSeqGroup group = seq.getSeqGroup();
		Preferences genomes_node = PreferenceUtils.getGenomesNode();
		Preferences group_node = PreferenceUtils.getSubnode(genomes_node, group.getID(), true);  //  encodes id via MD5 if too long
		Preferences seqs_node = PreferenceUtils.getSubnode(group_node, "seqs");
		Preferences seq_node = PreferenceUtils.getSubnode(seqs_node, seq.getID(), true);  //  encodes id via MD5 if too long
		int seq_min = seq_node.getInt(SEQ_MIN_PREF, 0);
		int seq_max = seq_node.getInt(SEQ_MAX_PREF, seq.getLength());
		SeqSpan span = new SimpleSeqSpan(seq_min, seq_max, seq);
		gviewer.zoomTo(span);
		return span;
	}
}

