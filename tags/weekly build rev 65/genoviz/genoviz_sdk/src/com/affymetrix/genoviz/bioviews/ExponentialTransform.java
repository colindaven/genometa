/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.bioviews;


/**
 * A transform used internally by some NeoWidgets to handle zooming, should
 *    not be used directly.
 *
 * ExponentialTransform is the start of replacing application handling
 *    of things such as zooming a map with scrollbars that can take
 *    both transforms and listeners and do the right thing
 *    Right now it only does about half the work involved in
 *    the "gradual deceleration" of the zoom scrollbar
 */
public final class ExponentialTransform {
	private final double lxmin, ratio;

	// for zoomer transform, x is transformed to y
	public ExponentialTransform(double xmin, double xmax, double ymin, double ymax) {
		double lxmax = Math.log(xmax);
		lxmin = Math.log(xmin);
		ratio = (lxmax-lxmin)/(ymax-ymin);
	}

	public double transform(int orientation, double in) {
		double out = Math.exp(in*ratio + lxmin);
		/*
		 *  Fix for zooming -- for cases where y _should_ be 7, but ends up
		 *  being 6.9999998 or thereabouts because of errors in Math.exp()
		 */
		if ( Math.abs(out) > .1) {
			double outround = Math.round(out);
			if (Math.abs(out-outround) < 0.0001) {
				out = outround;
			}
		}
		return out;
	}

	public double inverseTransform(int orientation, double in) {
		return (Math.log(in)-lxmin) / ratio;
	}

}
