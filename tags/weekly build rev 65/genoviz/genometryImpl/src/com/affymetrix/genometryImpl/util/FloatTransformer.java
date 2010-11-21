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

package com.affymetrix.genometryImpl.util;

/**
 *
 *  A simple interface for arbitrary transformation of float values.
 *  Primarily intended for transformation of GraphSym y values.
 *  Wanted to include method for inverting transform so can have a
 *     GraphSym transformed "in-place" via transform() calls, without using more memory,
 *     then untransformed via inverseTransform() calls
 *
 */
public interface FloatTransformer  {
	public float transform(float x);

	/**
	 *  inverseTranform() should be optional
	 * if isInvertible() == false, inverseTransform() should throw runtime exception?
	 */
	public float inverseTransform(float x);
	public boolean isInvertible();

	/*
	 * Logarithm base change: log_base_b(x) = log_base_a(x)/log_base_a(b)
	 * For example:
	 *     log10(x) = ln(x)/ln(10) = ln(x)/2.30258 = 0.4343 * ln(x)
	 *
	 *  use Math.ln(x) for ln(x)
	 *  use Math.exp(x) for e^x (inverse of ln(x))
	 *  use Math.pow(y, x) for y^x (inverse of log_base_y(x)
	 *
	 */
	public final class LogNatural implements FloatTransformer {
		static float LN1 = (float)Math.log(1); // should be 0...
		public float transform(float x) {
			// could pick any threshold > 0 to cut off low end at,
			// but thresholding at 1 for similarity to GTRANS
			return (x <= 1) ? LN1 : (float)Math.log(x);
		}
		public float inverseTransform(float y) {
			throw new RuntimeException("LogNatural.inverseTransform called, " +
					"but LogNatural is not an invertible function");
		}
		/** not invertible because values < 1 before transform cannot be recovered... */
		public boolean isInvertible()  { return false; }
	}


	public final class LogBase10 implements FloatTransformer {
		static double LN10 = Math.log(10);
		static float LOG10_1 = (float)(Math.log(1)/LN10);
		public float transform(float x) {
			// return (float)(Math.log(x)/LN10);
			return (x <= 1) ? LOG10_1 : (float)(Math.log(x)/LN10);
		}
		public float inverseTransform(float x) {
			throw new RuntimeException("LogBase10.inverseTransform called, " +
					"but LogBase10 is not an invertible function");
		}
		public boolean isInvertible()  { return false; }
	}


	public final class LogBase2 implements FloatTransformer {
		static double LN2 = Math.log(2);
		static float LOG2_1 = (float)(Math.log(1)/LN2);
		public float transform(float x) {
			return (x <= 1) ? LOG2_1 : (float)(Math.log(x)/LN2);
		}
		public float inverseTransform(float x) {
			throw new RuntimeException("LogBase2.inverseTransform called, " +
					"but LogBase2 is not an invertible function");
		}
		public boolean isInvertible()  { return false; }
	}

	public final class LogTransform implements FloatTransformer {
		double base;
		double LN_BASE;
		float LOG_1;
		public LogTransform(double base) {
			this.base = base;
			LN_BASE = Math.log(base);
			LOG_1 = (float)(Math.log(1)/LN_BASE);
		}
		public float transform(float x) {
			return (x <= 1) ? LOG_1 : (float)(Math.log(x)/LN_BASE);
		}
		public float inverseTransform(float x) {
			return (float)(Math.pow(base, x));
		}
		public boolean isInvertible() { return true; }
	}

	/**
	 *   Generalized replacement for LogNatural, LogBase2, LogBase10, etc.
	 *    transforms x to base raised to the x (base^x)
	 */
	public final class PowTransform implements FloatTransformer {
		double base;
		double LN_BASE;
		float LOG_1;
		public PowTransform(double base) {
			this.base = base;
			// if base == Math.E, then LN_BASE will be 1
			LN_BASE = Math.log(base);
			LOG_1 = (float)(Math.log(1)/LN_BASE);
		}
		public float transform(float x) {
			return (float)Math.pow(base, x);
		}
		public float inverseTransform(float x) {
			//    throw new RuntimeException("LogBase2.inverseTransform called, " +
			//                               "but LogBase2 is not an invertible function");
			return (x <= 1) ? LOG_1 : (float)(Math.log(x)/LN_BASE);
		}
		public boolean isInvertible() { return true; }
	}

	/**
	 *  alternative implementation of PowTransform.
	 *  since raising to a power is inverse of taking logarithm,
	 *     should be able to implement as inverse of LogTransform
	 *     (transform() calls LogTransform.inverseTransform(),
	 *      inverseTransform() calls LogTransform.transform())
	 */
	public final class InverseLogTransform implements FloatTransformer {
		LogTransform inner_trans;
		public InverseLogTransform(double base) {
			inner_trans = new LogTransform(base);
		}
		public float transform(float x) { return inner_trans.inverseTransform(x); }
		public float inverseTransform(float x) { return inner_trans.transform(x); }
		public boolean isInvertible() { return true; }
	}

	public final class IdentityTransform implements FloatTransformer {
		public IdentityTransform() {}
		public float transform(float x) { return x; }
		public float inverseTransform(float x) { return x; }
		public boolean isInvertible() { return true; }
	}
}
