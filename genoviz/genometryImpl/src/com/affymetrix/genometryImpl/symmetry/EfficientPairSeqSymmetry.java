package com.affymetrix.genometryImpl.symmetry;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import java.util.HashMap;
import java.util.Map;

public final class EfficientPairSeqSymmetry implements SeqSymmetry, SymWithProps {

	private static final int count = 2;
	private final int startA, startB, endA, endB;
	private final BioSeq seqA, seqB;
	private final String residues;

	public EfficientPairSeqSymmetry(int startA, int endA, BioSeq seqA, int startB, int endB, BioSeq seqB, String residues) {
		this.startA = startA;
		this.startB = startB;
		this.endA = endA;
		this.endB = endB;
		this.seqA = seqA;
		this.seqB = seqB;
		this.residues = residues;
	}

	public SeqSpan getSpan(BioSeq seq) {
		if (seqA == seq) {
			return new SimpleSeqSpan(startA, endA, seqA);
		} else if (seqB == seq) {
			return new SimpleSeqSpan(startB, endB, seqB);
		}
		return null;
	}

	public int getSpanCount() {
		return count;
	}

	public SeqSpan getSpan(int i) {
		if (i == 0) {
			return new SimpleSeqSpan(startA, endA, seqA);
		} else if (i == 1) {
			return new SimpleSeqSpan(startB, endB, seqB);
		} else {
			return null;
		}
	}

	public BioSeq getSpanSeq(int i) {
		if (i == 0) {
			return seqA;
		} else if (i == 1) {
			return seqB;
		} else {
			return null;
		}
	}

	public boolean getSpan(BioSeq seq, MutableSeqSpan span) {
		if (seqA == seq) {
			span.setStart(startA);
			span.setEnd(endA);
			span.setBioSeq(seqA);
			return true;
		} else if (seqB == seq) {
			span.setStart(startB);
			span.setEnd(endB);
			span.setBioSeq(seqB);
			return true;
		}
		return false;
	}

	public boolean getSpan(int index, MutableSeqSpan span) {
		if (index == 0) {
			span.setStart(startA);
			span.setEnd(endA);
			span.setBioSeq(seqA);
			return true;
		} else if (index == 1) {
			span.setStart(startB);
			span.setEnd(endB);
			span.setBioSeq(seqB);
			return true;
		}
		return false;
	}

	public int getChildCount() {
		return 0;
	}

	public SeqSymmetry getChild(int index) {
		return null;
	}

	public String getID() {
		return null;
	}

	public Map<String, Object> getProperties() {
		return cloneProperties();
	}

	public Map<String, Object> cloneProperties() {
		Map<String, Object> props = new HashMap<String, Object>();
		props.put("residues", residues);
		return props;
	}

	public Object getProperty(String key) {

		if ("residues".equalsIgnoreCase(key)) {
			return residues;
		}

		return null;
	}

	public boolean setProperty(String key, Object val) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
}
