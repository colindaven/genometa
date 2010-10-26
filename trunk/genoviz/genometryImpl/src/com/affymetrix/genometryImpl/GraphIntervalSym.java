package com.affymetrix.genometryImpl;

/**
 *  A SeqSymmetry for holding graph for graphs that have y values that apply to
 *  intervals.  So instead of (x,y) there is (x_start, x_width, y).
 */
public final class GraphIntervalSym extends GraphSym {
	public GraphIntervalSym(int[] x, int[] width, float[] y, String id, BioSeq seq) {
		super(x,width,y,id,seq);
	}

	@Override
	public int getChildCount() {
		return this.getPointCount();
	}

	/**
	 *  Constructs a temporary SeqSymmetry to represent the graph value of a single span.
	 *  The returned SeqSymmetry will implement the {@link Scored} interface.
	 */
	@Override
	public SeqSymmetry getChild(int index) {
		return new ScoredSingletonSym(
				this.getGraphXCoord(index),
				this.getGraphXCoord(index)+ getGraphWidthCoord(index),
				this.getGraphSeq(),
				this.getGraphYCoord(index));
	}
}
