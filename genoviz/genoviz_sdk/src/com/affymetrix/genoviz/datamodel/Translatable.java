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

package com.affymetrix.genoviz.datamodel;

/**
 * provides some constants for modeling the translation
 * of nucleotide sequences into peptide sequences.
 */
public interface Translatable {

	public static final int NUCLEOTIDES = 0;
	public static final int COMPLEMENT = 1;
	public static final int FRAME_ONE = 2;
	public static final int FRAME_TWO = 3;
	public static final int FRAME_THREE = 4;
	public static final int FRAME_NEG_ONE = 5;
	public static final int FRAME_NEG_TWO = 6;
	public static final int FRAME_NEG_THREE = 7;

	public static final int FORWARD_SPLICED_TRANSLATION = 8;
	public static final int REVERSE_SPLICED_TRANSLATION = 9;

	public static final int[] FRAME_MAPPING = { 0, 0, 0, 1, 2, -0, -1, -2 };

	public static final int ONE_LETTER_CODE = 100;
	public static final int THREE_LETTER_CODE = 101;

}
