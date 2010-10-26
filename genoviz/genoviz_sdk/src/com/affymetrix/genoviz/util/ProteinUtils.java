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

package com.affymetrix.genoviz.util;

/**
 * a collection of constants and static methods
 * useful in manipulating ascii representations of amino acids sequences.
 */
public class ProteinUtils {

	/** number of "letters" that are valid in a string of amino acid codes. */
	public static final int LETTERS = 26;

	/**
	 * ordinal numbers of amino acids
	 * associated with each possible ascii character code.
	 * Unused characters are associated with the integer -1.
	 */
	protected static int[] letter_to_id = new int[256];
	/** ascii character codes for each amino acid (or set of amino acids). */
	protected static char[] id_to_letter = new char[LETTERS];

	static {
		for (int i=0; i<letter_to_id.length; i++) {
			letter_to_id[i] = -1;
		}
		letter_to_id['A'] = 0;   // Alanine
		letter_to_id['R'] = 1;   // Arginine
		letter_to_id['N'] = 2;   // Asparagine
		letter_to_id['D'] = 3;   // Aspartate
		letter_to_id['C'] = 4;   // Cysteine
		letter_to_id['Q'] = 5;   // Glutamine
		letter_to_id['E'] = 6;   // Glutamate
		letter_to_id['G'] = 7;   // Glycine
		letter_to_id['H'] = 8;   // Histidine
		letter_to_id['I'] = 9;   // Isoleucine
		letter_to_id['L'] = 10;  // Leucine
		letter_to_id['K'] = 11;  // Lysine
		letter_to_id['M'] = 12;  // Methionine
		letter_to_id['F'] = 13;  // Phenylalanine
		letter_to_id['P'] = 14;  // Proline
		letter_to_id['S'] = 15;  // Serine
		letter_to_id['T'] = 16;  // Threonine
		letter_to_id['W'] = 17;  // Tryptophan
		letter_to_id['Y'] = 18;  // Tyrosine
		letter_to_id['V'] = 19;  // Valine
		letter_to_id['B'] = 20;  // ???
		letter_to_id['Z'] = 21;  // ???
		letter_to_id['X'] = 22;  // unknown??
		letter_to_id['*'] = 23;  // gap? unknown? stop?
		letter_to_id['-'] = 24;  // gap / unknown
		letter_to_id[' '] = 25;  // gap / unknown

		letter_to_id['a'] = 0;   // Alanine
		letter_to_id['r'] = 1;   // Arginine
		letter_to_id['n'] = 2;   // Asparagine
		letter_to_id['d'] = 3;   // Aspartate
		letter_to_id['c'] = 4;   // Cysteine
		letter_to_id['q'] = 5;   // Glutamine
		letter_to_id['e'] = 6;   // Glutamate
		letter_to_id['g'] = 7;   // Glycine
		letter_to_id['h'] = 8;   // Histidine
		letter_to_id['i'] = 9;   // Isoleucine
		letter_to_id['l'] = 10;  // Leucine
		letter_to_id['k'] = 11;  // Lysine
		letter_to_id['m'] = 12;  // Methionine
		letter_to_id['f'] = 13;  // Phenylalanine
		letter_to_id['p'] = 14;  // Proline
		letter_to_id['s'] = 15;  // Serine
		letter_to_id['t'] = 16;  // Threonine
		letter_to_id['w'] = 17;  // Tryptophan
		letter_to_id['y'] = 18;  // Tyrosine
		letter_to_id['v'] = 19;  // Valine
		letter_to_id['b'] = 20;  // ???
		letter_to_id['z'] = 21;  // ???
		letter_to_id['x'] = 22;  // unknown??

		id_to_letter[0] = 'A';   // Alanine
		id_to_letter[1] = 'R';   // Arginine
		id_to_letter[2] = 'N';   // Asparagine
		id_to_letter[3] = 'D';   // Aspartate
		id_to_letter[4] = 'C';   // Cysteine
		id_to_letter[5] = 'Q';   // Glutamine
		id_to_letter[6] = 'E';   // Glutamate
		id_to_letter[7] = 'G';   // Glycine
		id_to_letter[8] = 'H';   // Histidine
		id_to_letter[9] = 'I';   // Isoleucine
		id_to_letter[10] = 'L';  // Leucine
		id_to_letter[11] = 'K';  // Lysine
		id_to_letter[12] = 'M';  // Methionine
		id_to_letter[13] = 'F';  // Phenylalanine
		id_to_letter[14] = 'P';  // Proline
		id_to_letter[15] = 'S';  // Serine
		id_to_letter[16] = 'T';  // Threonine
		id_to_letter[17] = 'W';  // Tryptophan
		id_to_letter[18] = 'Y';  // Tyrosine
		id_to_letter[19] = 'V';  // Valine
		id_to_letter[20] = 'B';  // ???
		id_to_letter[21] = 'Z';  // ???
		id_to_letter[22] = 'X';  // unknown??
		id_to_letter[23] = '*';  // gap? unknown?
		id_to_letter[24] = '-';  // gap / unknown
		id_to_letter[25] = ' ';  // gap / unknown
	}

	/**
	 * gets an index into an array of codes for amino acids.
	 *
	 * @param residue_letter letter representation of an amino acid.
	 * @return ordinal of amino acid letter code.
	 * @see #getResidueChar
	 */
	public static int getResidueID(char residue_letter) {
		return letter_to_id[residue_letter];
	}

	/**
	 * gets a amino acid code.
	 *
	 * @param residue_id ordinal of amino acid letter code.
	 * @return letter representation of an amino acid.
	 * @see #getResidueChar
	 */
	public static char getResidueChar(int residue_id) {
		return id_to_letter[residue_id];
	}

	/**
	 * gets a map from letters to numbers
	 * each representing an amino acid.
	 */
	public static int[] getAACharToIdMap() {
		return letter_to_id;
	}

	/**
	 * gets a map from numbers to letters
	 * each representing an amino acid.
	 */
	public static char[] getAAIdToCharMap() {
		return id_to_letter;
	}

}
