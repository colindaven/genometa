package com.affymetrix.genoviz.bioviews;

import com.affymetrix.genoviz.util.NeoConstants;

/**
 *  A transform used internally by NeoSeq, should not be used directly.
 */
public final class ConstrainLinearTrnsfm extends LinearTransform {

	private double constrain_value;

	public ConstrainLinearTrnsfm() {
		constrain_value = 1;
	}

	public void setConstrainValue(double cv) {
		constrain_value = cv;
	}

	public double getConstrainValue() {
		return constrain_value;
	}

	@Override
	public double transform(int orientation, double in) {
		double out = 0;
		if (orientation == NeoConstants.HORIZONTAL) {
			out = in * this.getScaleX();
		} else if (orientation == NeoConstants.VERTICAL) {
			out = in * this.getScaleY();
		}

		out = out - (out % constrain_value);
	
		if (orientation == NeoConstants.HORIZONTAL) {
			out += this.getTranslateX();
		} else if (orientation == NeoConstants.VERTICAL) {
			out += this.getTranslateY();
		}

		return out;
	}

	@Override
	public boolean equals(LinearTransform Tx) {
		return (Tx instanceof ConstrainLinearTrnsfm) &&
				super.equals(Tx) &&
				(constrain_value == ((ConstrainLinearTrnsfm)Tx).getConstrainValue());
	}
}
