/**
 *   Copyright (c) 2006 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl;

/**
 *  A very simple interface to indicate something that has a score.
 *  For SeqSymmetry's with a score, we may want to display the score somehow.
 */
public interface Scored {
	public static final float UNKNOWN_SCORE = Float.NEGATIVE_INFINITY;

	public float getScore();
}
