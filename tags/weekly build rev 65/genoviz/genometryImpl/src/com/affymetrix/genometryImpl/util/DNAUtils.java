/**
 *   Copyright (c) 1998-2007 Affymetrix, Inc.
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
 * A collection of constants and static methods
 * useful in manipulating ascii representations of DNA sequences.
 */
public final class DNAUtils {

	/**
	 * Determines the reverse complement of a sequence of nucleotides.
	 *
	 * @param s a string of nucleotide codes.
	 * @return the complementary codes in reverse order.
	 */
	public static String reverseComplement(String s) {
		if (s == null) { return null; }
		StringBuffer buf = new StringBuffer(s);
		reverseComplement(buf);
		return buf.toString();
	}

	/**
	 * Determines the reverse complement of a sequence of nucleotides "in-place".
	 *
	 * @param sb  Nucleotide codes.  The contents of this StringBuffer will be
	 *            replaced with the complementary codes in reverse order.
	 */
	public static void reverseComplement(StringBuffer sb) {
		if ( null == sb ) { return; }
		char c;
		for (int i=sb.length()-1, j=0; i>=j; i--, j++) {
			c = complementChar(sb.charAt(i));
			sb.setCharAt(i, complementChar(sb.charAt(j)));
			sb.setCharAt(j, c);
		}
	}

	private static char complementChar(char b) {

		switch (b) {
			case 'A': return 'T';
			case 'C': return 'G';
			case 'G': return 'C';
			case 'T': return 'A';
			case 'U': return 'A';
			case 'M': return 'K';
			case 'R': return 'Y';
			case 'Y': return 'R';
			case 'K': return 'M';
			case 'V': return 'B';
			case 'H': return 'D';
			case 'D': return 'H';
			case 'B': return 'V';
			case 'N': return 'X';
			case 'X': return 'X';
			case 'a': return 't';
			case 'c': return 'g';
			case 'g': return 'c';
			case 't': return 'a';
			case 'u': return 'a';
			case 'm': return 'k';
			case 'r': return 'y';
			case 'y': return 'r';
			case 'k': return 'm';
			case 'v': return 'b';
			case 'h': return 'd';
			case 'd': return 'h';
			case 'b': return 'v';
			case 'n': return 'x';
			case 'x': return 'x';
			default: return '-';
		}
	}
}
