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

package com.affymetrix.genoviz.util;

import com.affymetrix.genoviz.datamodel.Translatable;
import java.awt.Color;

/**
 * A collection of constants and static methods
 * useful in manipulating ascii representations of DNA sequences.
 */
public class DNAUtils implements Translatable  {

	private static final char[] dna_chars = {
		'A', 'C', 'G', 'T', 'N', 'M', 'R', 'W',
		'S', 'Y', 'K', 'V', 'H', 'D', 'B', 'X',
		' ',
		'a', 'c', 'g', 't', 'n', 'm', 'r', 'w',
		's', 'y', 'k', 'v', 'h', 'd', 'b', 'x' };

	/** Genetic Code in 1-character amino acid codes */

	protected static final String aa1[][][] = new String[16][16][16];

	/** Genetic Code in 3-character amino acid codes */

	protected static final String aa3[][][] = new String[16][16][16];

	/** number of "letters" that are valid in a string of nucleotide codes. */

	public static final int LETTERS = 17;

	/**
	 * ordinal numbers of nucleotides
	 * associated with each possible ascii character code.
	 * Unused characters are associated with the integer -1.
	 */
	protected static final int[] letter_to_id = new int[256];

	/** ascii character codes for each nucleotide (or set of nucleotides). */
	protected static final char[] id_to_letter = new char[LETTERS];

	static {
		int A = 0;
		int C = 1;
		int G = 2;
		int T = 3;
		int N = 4;
		int M = 5;
		int R = 6;
		int W = 7;
		int S = 8;
		int Y = 9;
		int K = 10;
		int V = 11;
		int H = 12;
		int D = 13;
		int B = 14;
		int X = 15;

		// Three-letter codes:

		// Initialize all genetic code entries to unknown:

		for (int i=0; i<16; i++) {
			for (int j=0; j<16; j++) {
				for (int k=0; k<16; k++) {
					aa3[i][j][k] = "???";
				}
			}
		}

		// Organized by amino acid:

		aa3[G][C][A] = "Ala";  // GCA, A, Ala, Alanine
		aa3[G][C][C] = "Ala";  // GCC, A, Ala, Alanine
		aa3[G][C][G] = "Ala";  // GCG, A, Ala, Alanine
		aa3[G][C][T] = "Ala";  // GCT, A, Ala, Alanine
		aa3[G][C][N] = "Ala";
		aa3[G][C][M] = "Ala";
		aa3[G][C][R] = "Ala";
		aa3[G][C][W] = "Ala";
		aa3[G][C][S] = "Ala";
		aa3[G][C][Y] = "Ala";
		aa3[G][C][K] = "Ala";
		aa3[G][C][V] = "Ala";
		aa3[G][C][H] = "Ala";
		aa3[G][C][D] = "Ala";
		aa3[G][C][B] = "Ala";
		aa3[G][C][X] = "Ala";


		aa3[T][G][C] = "Cys";  // TGC, C, Cys, Cysteine
		aa3[T][G][T] = "Cys";  // TGT, C, Cys, Cysteine
		aa3[T][G][Y] = "Cys";


		aa3[G][A][C] = "Asp";  // GAC, D, Asp, Aspartate
		aa3[G][A][T] = "Asp";  // GAT, D, Asp, Aspartate
		aa3[G][A][Y] = "Asp";


		aa3[G][A][A] = "Glu";  // GAA, E, Glu, Glutamate
		aa3[G][A][G] = "Glu";  // GAG, E, Glu, Glutamate
		aa3[G][A][R] = "Glu";


		aa3[T][T][C] = "Phe";  // TTC, F, Phe, Phenylalanine
		aa3[T][T][T] = "Phe";  // TTT, F, Phe, Phenylalanine
		aa3[T][T][Y] = "Phe";


		aa3[G][G][A] = "Gly";  // GGA, G, Gly, Glycine
		aa3[G][G][C] = "Gly";  // GGC, G, Gly, Glycine
		aa3[G][G][G] = "Gly";  // GGG, G, Gly, Glycine
		aa3[G][G][T] = "Gly";  // GGT, G, Gly, Glycine
		aa3[G][G][N] = "Gly";
		aa3[G][G][M] = "Gly";
		aa3[G][G][R] = "Gly";
		aa3[G][G][W] = "Gly";
		aa3[G][G][S] = "Gly";
		aa3[G][G][Y] = "Gly";
		aa3[G][G][K] = "Gly";
		aa3[G][G][V] = "Gly";
		aa3[G][G][H] = "Gly";
		aa3[G][G][D] = "Gly";
		aa3[G][G][B] = "Gly";
		aa3[G][G][X] = "Gly";


		aa3[C][A][C] = "His";  // CAC, H, His, Histidine
		aa3[C][A][T] = "His";  // CAT, H, His, Histidine
		aa3[C][A][Y] = "His";


		aa3[A][T][A] = "Ile";  // ATA, I, Ile, Isoleucine
		aa3[A][T][C] = "Ile";  // ATC, I, Ile, Isoleucine
		aa3[A][T][T] = "Ile";  // ATT, I, Ile, Isoleucine
		aa3[A][T][M] = "Ile";
		aa3[A][T][Y] = "Ile";
		aa3[A][T][W] = "Ile";


		aa3[A][A][A] = "Lys";  // AAA, K, Lys, Lysine
		aa3[A][A][G] = "Lys";  // AAG, K, Lys, Lysine
		aa3[A][A][R] = "Lys";


		aa3[C][T][A] = "Leu";  // CTA, L, Leu, Leucine
		aa3[C][T][C] = "Leu";  // CTC, L, Leu, Leucine
		aa3[C][T][G] = "Leu";  // CTG, L, Leu, Leucine
		aa3[C][T][T] = "Leu";  // CTT, L, Leu, Leucine
		aa3[C][T][N] = "Leu";
		aa3[C][T][M] = "Leu";
		aa3[C][T][R] = "Leu";
		aa3[C][T][W] = "Leu";
		aa3[C][T][S] = "Leu";
		aa3[C][T][Y] = "Leu";
		aa3[C][T][K] = "Leu";
		aa3[C][T][V] = "Leu";
		aa3[C][T][H] = "Leu";
		aa3[C][T][D] = "Leu";
		aa3[C][T][B] = "Leu";
		aa3[C][T][X] = "Leu";

		aa3[T][T][A] = "Leu";  // TTA, L, Leu, Leucine
		aa3[T][T][G] = "Leu";  // TTG, L, Leu, Leucine
		aa3[T][T][R] = "Leu";

		aa3[Y][T][A] = "Leu";
		aa3[Y][T][C] = "Leu";
		aa3[Y][T][G] = "Leu";
		aa3[Y][T][T] = "Leu";
		aa3[Y][T][N] = "Leu";
		aa3[Y][T][M] = "Leu";
		aa3[Y][T][R] = "Leu";
		aa3[Y][T][W] = "Leu";
		aa3[Y][T][S] = "Leu";
		aa3[Y][T][Y] = "Leu";
		aa3[Y][T][K] = "Leu";
		aa3[Y][T][V] = "Leu";
		aa3[Y][T][H] = "Leu";
		aa3[Y][T][D] = "Leu";
		aa3[Y][T][B] = "Leu";
		aa3[Y][T][X] = "Leu";


		aa3[A][T][G] = "Met";  // ATG, M, Met, Methionine


		aa3[A][A][C] = "Asn";  // AAC, N, Asn, Asparagine
		aa3[A][A][T] = "Asn";  // AAT, N, Asn, Asparagine
		aa3[A][A][Y] = "Asn";


		aa3[C][C][A] = "Pro";  // CCA, P, Pro, Proline
		aa3[C][C][C] = "Pro";  // CCC, P, Pro, Proline
		aa3[C][C][G] = "Pro";  // CCG, P, Pro, Proline
		aa3[C][C][T] = "Pro";  // CCT, P, Pro, Proline
		aa3[C][C][N] = "Pro";
		aa3[C][C][M] = "Pro";
		aa3[C][C][R] = "Pro";
		aa3[C][C][W] = "Pro";
		aa3[C][C][S] = "Pro";
		aa3[C][C][Y] = "Pro";
		aa3[C][C][K] = "Pro";
		aa3[C][C][V] = "Pro";
		aa3[C][C][H] = "Pro";
		aa3[C][C][D] = "Pro";
		aa3[C][C][B] = "Pro";
		aa3[C][C][X] = "Pro";


		aa3[C][A][A] = "Gln";  // CAA, Q, Gln, Glutamine
		aa3[C][A][G] = "Gln";  // CAG, Q, Gln, Glutamine
		aa3[C][A][R] = "Gln";


		aa3[C][G][A] = "Arg";  // CGA, R, Arg, Arginine
		aa3[C][G][C] = "Arg";  // CGC, R, Arg, Arginine
		aa3[C][G][G] = "Arg";  // CGG, R, Arg, Arginine
		aa3[C][G][T] = "Arg";  // CGT, R, Arg, Arginine
		aa3[C][G][N] = "Arg";
		aa3[C][G][M] = "Arg";
		aa3[C][G][R] = "Arg";
		aa3[C][G][W] = "Arg";
		aa3[C][G][S] = "Arg";
		aa3[C][G][Y] = "Arg";
		aa3[C][G][K] = "Arg";
		aa3[C][G][V] = "Arg";
		aa3[C][G][H] = "Arg";
		aa3[C][G][D] = "Arg";
		aa3[C][G][B] = "Arg";
		aa3[C][G][X] = "Arg";

		aa3[A][G][A] = "Arg";  // AGA, R, Arg, Arginine
		aa3[A][G][G] = "Arg";  // AGG, R, Arg, Arginine
		aa3[A][G][R] = "Arg";

		aa3[M][G][A] = "Arg";
		aa3[M][G][C] = "Arg";
		aa3[M][G][G] = "Arg";
		aa3[M][G][T] = "Arg";
		aa3[M][G][N] = "Arg";
		aa3[M][G][M] = "Arg";
		aa3[M][G][R] = "Arg";
		aa3[M][G][W] = "Arg";
		aa3[M][G][S] = "Arg";
		aa3[M][G][Y] = "Arg";
		aa3[M][G][K] = "Arg";
		aa3[M][G][V] = "Arg";
		aa3[M][G][H] = "Arg";
		aa3[M][G][D] = "Arg";
		aa3[M][G][B] = "Arg";
		aa3[M][G][X] = "Arg";


		aa3[A][G][C] = "Ser";  // AGC, S, Ser, Serine
		aa3[A][G][T] = "Ser";  // AGT, S, Ser, Serine
		aa3[A][G][Y] = "Ser";

		aa3[T][C][A] = "Ser";  // TCA, S, Ser, Serine
		aa3[T][C][C] = "Ser";  // TCC, S, Ser, Serine
		aa3[T][C][G] = "Ser";  // TCG, S, Ser, Serine
		aa3[T][C][T] = "Ser";  // TCT, S, Ser, Serine
		aa3[T][C][N] = "Ser";
		aa3[T][C][M] = "Ser";
		aa3[T][C][R] = "Ser";
		aa3[T][C][W] = "Ser";
		aa3[T][C][S] = "Ser";
		aa3[T][C][Y] = "Ser";
		aa3[T][C][K] = "Ser";
		aa3[T][C][V] = "Ser";
		aa3[T][C][H] = "Ser";
		aa3[T][C][D] = "Ser";
		aa3[T][C][B] = "Ser";
		aa3[T][C][X] = "Ser";

		aa3[W][G][C] = "Ser";
		aa3[W][G][T] = "Ser";
		aa3[W][G][Y] = "Ser";

		aa3[W][C][A] = "Ser";
		aa3[W][C][C] = "Ser";
		aa3[W][C][G] = "Ser";
		aa3[W][C][T] = "Ser";
		aa3[W][C][N] = "Ser";
		aa3[W][C][M] = "Ser";
		aa3[W][C][R] = "Ser";
		aa3[W][C][W] = "Ser";
		aa3[W][C][S] = "Ser";
		aa3[W][C][Y] = "Ser";
		aa3[W][C][K] = "Ser";
		aa3[W][C][V] = "Ser";
		aa3[W][C][H] = "Ser";
		aa3[W][C][D] = "Ser";
		aa3[W][C][B] = "Ser";
		aa3[W][C][X] = "Ser";


		aa3[A][C][A] = "Thr";  // ACA, T, Thr, Threonine
		aa3[A][C][C] = "Thr";  // ACC, T, Thr, Threonine
		aa3[A][C][G] = "Thr";  // ACG, T, Thr, Threonine
		aa3[A][C][T] = "Thr";  // ACT, T, Thr, Threonine
		aa3[A][C][N] = "Thr";
		aa3[A][C][M] = "Thr";
		aa3[A][C][R] = "Thr";
		aa3[A][C][W] = "Thr";
		aa3[A][C][S] = "Thr";
		aa3[A][C][Y] = "Thr";
		aa3[A][C][K] = "Thr";
		aa3[A][C][V] = "Thr";
		aa3[A][C][H] = "Thr";
		aa3[A][C][D] = "Thr";
		aa3[A][C][B] = "Thr";
		aa3[A][C][X] = "Thr";


		aa3[G][T][A] = "Val";  // GTA, V, Val, Valine
		aa3[G][T][C] = "Val";  // GTC, V, Val, Valine
		aa3[G][T][G] = "Val";  // GTG, V, Val, Valine
		aa3[G][T][T] = "Val";  // GTT, V, Val, Valine
		aa3[G][T][N] = "Val";
		aa3[G][T][M] = "Val";
		aa3[G][T][R] = "Val";
		aa3[G][T][W] = "Val";
		aa3[G][T][S] = "Val";
		aa3[G][T][Y] = "Val";
		aa3[G][T][K] = "Val";
		aa3[G][T][V] = "Val";
		aa3[G][T][H] = "Val";
		aa3[G][T][D] = "Val";
		aa3[G][T][B] = "Val";
		aa3[G][T][X] = "Val";

		aa3[T][G][G] = "Trp";  // TGG, W, Trp, Tryptophan

		aa3[T][A][C] = "Tyr";  // TAC, Y, Tyr, Tyrosine
		aa3[T][A][T] = "Tyr";  // TAT, Y, Tyr, Tyrosine
		aa3[T][A][Y] = "Tyr";

		aa3[T][A][A] = "***";  // TAA, *, ***, Stop
		aa3[T][A][G] = "***";  // TAG, *, ***, Stop
		aa3[T][A][R] = "***";

		aa3[T][G][A] = "***";  // TGA, *, ***, Stop
		aa3[T][R][A] = "***";

		// One-letter codes:

		// Initialize all genetic code entries to unknown:

		for (int i=0; i<16; i++) {
			for (int j=0; j<16; j++) {
				for (int k=0; k<16; k++) {
					aa1[i][j][k] = "?";
				}
			}
		}

		// Organized by amino acid:

		aa1[G][C][A] = "A";  // GCA, A, Ala, Alanine
		aa1[G][C][C] = "A";  // GCC, A, Ala, Alanine
		aa1[G][C][G] = "A";  // GCG, A, Ala, Alanine
		aa1[G][C][T] = "A";  // GCT, A, Ala, Alanine
		aa1[G][C][N] = "A";
		aa1[G][C][M] = "A";
		aa1[G][C][R] = "A";
		aa1[G][C][W] = "A";
		aa1[G][C][S] = "A";
		aa1[G][C][Y] = "A";
		aa1[G][C][K] = "A";
		aa1[G][C][V] = "A";
		aa1[G][C][H] = "A";
		aa1[G][C][D] = "A";
		aa1[G][C][B] = "A";
		aa1[G][C][X] = "A";


		aa1[T][G][C] = "C";  // TGC, C, Cys, Cysteine
		aa1[T][G][T] = "C";  // TGT, C, Cys, Cysteine
		aa1[T][G][Y] = "C";


		aa1[G][A][C] = "D";  // GAC, D, Asp, Aspartate
		aa1[G][A][T] = "D";  // GAT, D, Asp, Aspartate
		aa1[G][A][Y] = "D";


		aa1[G][A][A] = "E";  // GAA, E, Glu, Glutamate
		aa1[G][A][G] = "E";  // GAG, E, Glu, Glutamate
		aa1[G][A][R] = "E";


		aa1[T][T][C] = "F";  // TTC, F, Phe, Phenylalanine
		aa1[T][T][T] = "F";  // TTT, F, Phe, Phenylalanine
		aa1[T][T][Y] = "F";


		aa1[G][G][A] = "G";  // GGA, G, Gly, Glycine
		aa1[G][G][C] = "G";  // GGC, G, Gly, Glycine
		aa1[G][G][G] = "G";  // GGG, G, Gly, Glycine
		aa1[G][G][T] = "G";  // GGT, G, Gly, Glycine
		aa1[G][G][N] = "G";
		aa1[G][G][M] = "G";
		aa1[G][G][R] = "G";
		aa1[G][G][W] = "G";
		aa1[G][G][S] = "G";
		aa1[G][G][Y] = "G";
		aa1[G][G][K] = "G";
		aa1[G][G][V] = "G";
		aa1[G][G][H] = "G";
		aa1[G][G][D] = "G";
		aa1[G][G][B] = "G";
		aa1[G][G][X] = "G";


		aa1[C][A][C] = "H";  // CAC, H, His, Histidine
		aa1[C][A][T] = "H";  // CAT, H, His, Histidine
		aa1[C][A][Y] = "H";


		aa1[A][T][A] = "I";  // ATA, I, Ile, Isoleucine
		aa1[A][T][C] = "I";  // ATC, I, Ile, Isoleucine
		aa1[A][T][T] = "I";  // ATT, I, Ile, Isoleucine
		aa1[A][T][M] = "I";
		aa1[A][T][Y] = "I";
		aa1[A][T][W] = "I";


		aa1[A][A][A] = "K";  // AAA, K, Lys, Lysine
		aa1[A][A][G] = "K";  // AAG, K, Lys, Lysine
		aa1[A][A][R] = "K";


		aa1[C][T][A] = "L";  // CTA, L, Leu, Leucine
		aa1[C][T][C] = "L";  // CTC, L, Leu, Leucine
		aa1[C][T][G] = "L";  // CTG, L, Leu, Leucine
		aa1[C][T][T] = "L";  // CTT, L, Leu, Leucine
		aa1[C][T][N] = "L";
		aa1[C][T][M] = "L";
		aa1[C][T][R] = "L";
		aa1[C][T][W] = "L";
		aa1[C][T][S] = "L";
		aa1[C][T][Y] = "L";
		aa1[C][T][K] = "L";
		aa1[C][T][V] = "L";
		aa1[C][T][H] = "L";
		aa1[C][T][D] = "L";
		aa1[C][T][B] = "L";
		aa1[C][T][X] = "L";

		aa1[T][T][A] = "L";  // TTA, L, Leu, Leucine
		aa1[T][T][G] = "L";  // TTG, L, Leu, Leucine
		aa1[T][T][R] = "L";

		aa1[Y][T][A] = "L";
		aa1[Y][T][C] = "L";
		aa1[Y][T][G] = "L";
		aa1[Y][T][T] = "L";
		aa1[Y][T][N] = "L";
		aa1[Y][T][M] = "L";
		aa1[Y][T][R] = "L";
		aa1[Y][T][W] = "L";
		aa1[Y][T][S] = "L";
		aa1[Y][T][Y] = "L";
		aa1[Y][T][K] = "L";
		aa1[Y][T][V] = "L";
		aa1[Y][T][H] = "L";
		aa1[Y][T][D] = "L";
		aa1[Y][T][B] = "L";
		aa1[Y][T][X] = "L";


		aa1[A][T][G] = "M";  // ATG, M, Met, Methionine


		aa1[A][A][C] = "N";  // AAC, N, Asn, Asparagine
		aa1[A][A][T] = "N";  // AAT, N, Asn, Asparagine
		aa1[A][A][Y] = "N";


		aa1[C][C][A] = "P";  // CCA, P, Pro, Proline
		aa1[C][C][C] = "P";  // CCC, P, Pro, Proline
		aa1[C][C][G] = "P";  // CCG, P, Pro, Proline
		aa1[C][C][T] = "P";  // CCT, P, Pro, Proline
		aa1[C][C][N] = "P";
		aa1[C][C][M] = "P";
		aa1[C][C][R] = "P";
		aa1[C][C][W] = "P";
		aa1[C][C][S] = "P";
		aa1[C][C][Y] = "P";
		aa1[C][C][K] = "P";
		aa1[C][C][V] = "P";
		aa1[C][C][H] = "P";
		aa1[C][C][D] = "P";
		aa1[C][C][B] = "P";
		aa1[C][C][X] = "P";


		aa1[C][A][A] = "Q";  // CAA, Q, Gln, Glutamine
		aa1[C][A][G] = "Q";  // CAG, Q, Gln, Glutamine
		aa1[C][A][R] = "Q";


		aa1[C][G][A] = "R";  // CGA, R, Arg, Arginine
		aa1[C][G][C] = "R";  // CGC, R, Arg, Arginine
		aa1[C][G][G] = "R";  // CGG, R, Arg, Arginine
		aa1[C][G][T] = "R";  // CGT, R, Arg, Arginine
		aa1[C][G][N] = "R";
		aa1[C][G][M] = "R";
		aa1[C][G][R] = "R";
		aa1[C][G][W] = "R";
		aa1[C][G][S] = "R";
		aa1[C][G][Y] = "R";
		aa1[C][G][K] = "R";
		aa1[C][G][V] = "R";
		aa1[C][G][H] = "R";
		aa1[C][G][D] = "R";
		aa1[C][G][B] = "R";
		aa1[C][G][X] = "R";

		aa1[A][G][A] = "R";  // AGA, R, Arg, Arginine
		aa1[A][G][G] = "R";  // AGG, R, Arg, Arginine
		aa1[A][G][R] = "R";

		aa1[M][G][A] = "R";
		aa1[M][G][C] = "R";
		aa1[M][G][G] = "R";
		aa1[M][G][T] = "R";
		aa1[M][G][N] = "R";
		aa1[M][G][M] = "R";
		aa1[M][G][R] = "R";
		aa1[M][G][W] = "R";
		aa1[M][G][S] = "R";
		aa1[M][G][Y] = "R";
		aa1[M][G][K] = "R";
		aa1[M][G][V] = "R";
		aa1[M][G][H] = "R";
		aa1[M][G][D] = "R";
		aa1[M][G][B] = "R";
		aa1[M][G][X] = "R";


		aa1[A][G][C] = "S";  // AGC, S, Ser, Serine
		aa1[A][G][T] = "S";  // AGT, S, Ser, Serine
		aa1[A][G][Y] = "S";

		aa1[T][C][A] = "S";  // TCA, S, Ser, Serine
		aa1[T][C][C] = "S";  // TCC, S, Ser, Serine
		aa1[T][C][G] = "S";  // TCG, S, Ser, Serine
		aa1[T][C][T] = "S";  // TCT, S, Ser, Serine
		aa1[T][C][N] = "S";
		aa1[T][C][M] = "S";
		aa1[T][C][R] = "S";
		aa1[T][C][W] = "S";
		aa1[T][C][S] = "S";
		aa1[T][C][Y] = "S";
		aa1[T][C][K] = "S";
		aa1[T][C][V] = "S";
		aa1[T][C][H] = "S";
		aa1[T][C][D] = "S";
		aa1[T][C][B] = "S";
		aa1[T][C][X] = "S";

		aa1[W][G][C] = "S";
		aa1[W][G][T] = "S";
		aa1[W][G][Y] = "S";

		aa1[W][C][A] = "S";
		aa1[W][C][C] = "S";
		aa1[W][C][G] = "S";
		aa1[W][C][T] = "S";
		aa1[W][C][N] = "S";
		aa1[W][C][M] = "S";
		aa1[W][C][R] = "S";
		aa1[W][C][W] = "S";
		aa1[W][C][S] = "S";
		aa1[W][C][Y] = "S";
		aa1[W][C][K] = "S";
		aa1[W][C][V] = "S";
		aa1[W][C][H] = "S";
		aa1[W][C][D] = "S";
		aa1[W][C][B] = "S";
		aa1[W][C][X] = "S";


		aa1[A][C][A] = "T";  // ACA, T, Thr, Threonine
		aa1[A][C][C] = "T";  // ACC, T, Thr, Threonine
		aa1[A][C][G] = "T";  // ACG, T, Thr, Threonine
		aa1[A][C][T] = "T";  // ACT, T, Thr, Threonine
		aa1[A][C][N] = "T";
		aa1[A][C][M] = "T";
		aa1[A][C][R] = "T";
		aa1[A][C][W] = "T";
		aa1[A][C][S] = "T";
		aa1[A][C][Y] = "T";
		aa1[A][C][K] = "T";
		aa1[A][C][V] = "T";
		aa1[A][C][H] = "T";
		aa1[A][C][D] = "T";
		aa1[A][C][B] = "T";
		aa1[A][C][X] = "T";


		aa1[G][T][A] = "V";  // GTA, V, Val, Valine
		aa1[G][T][C] = "V";  // GTC, V, Val, Valine
		aa1[G][T][G] = "V";  // GTG, V, Val, Valine
		aa1[G][T][T] = "V";  // GTT, V, Val, Valine
		aa1[G][T][N] = "V";
		aa1[G][T][M] = "V";
		aa1[G][T][R] = "V";
		aa1[G][T][W] = "V";
		aa1[G][T][S] = "V";
		aa1[G][T][Y] = "V";
		aa1[G][T][K] = "V";
		aa1[G][T][V] = "V";
		aa1[G][T][H] = "V";
		aa1[G][T][D] = "V";
		aa1[G][T][B] = "V";
		aa1[G][T][X] = "V";

		aa1[T][G][G] = "W";  // TGG, W, Trp, Tryptophan

		aa1[T][A][C] = "Y";  // TAC, Y, Tyr, Tyrosine
		aa1[T][A][T] = "Y";  // TAT, Y, Tyr, Tyrosine
		aa1[T][A][Y] = "Y";

		aa1[T][A][A] = "*";  // TAA, *, ***, Stop
		aa1[T][A][G] = "*";  // TAG, *, ***, Stop
		aa1[T][A][R] = "*";

		aa1[T][G][A] = "*";  // TGA, *, ***, Stop
		aa1[T][R][A] = "*";

		// Now initialize the mapping arrays

		for (int i=0; i<letter_to_id.length; i++) {
			letter_to_id[i] = -1;
		}

		letter_to_id['A'] = 0;
		letter_to_id['C'] = 1;
		letter_to_id['G'] = 2;
		letter_to_id['T'] = 3;
		letter_to_id['N'] = 4;
		letter_to_id['M'] = 5;
		letter_to_id['R'] = 6;
		letter_to_id['W'] = 7;
		letter_to_id['S'] = 8;
		letter_to_id['Y'] = 9;
		letter_to_id['K'] = 10;
		letter_to_id['V'] = 11;
		letter_to_id['H'] = 12;
		letter_to_id['D'] = 13;
		letter_to_id['B'] = 14;
		letter_to_id['X'] = 15;

		letter_to_id['a'] = 0;
		letter_to_id['c'] = 1;
		letter_to_id['g'] = 2;
		letter_to_id['t'] = 3;
		letter_to_id['n'] = 4;
		letter_to_id['m'] = 5;
		letter_to_id['r'] = 6;
		letter_to_id['w'] = 7;
		letter_to_id['s'] = 8;
		letter_to_id['y'] = 9;
		letter_to_id['k'] = 10;
		letter_to_id['v'] = 11;
		letter_to_id['h'] = 12;
		letter_to_id['d'] = 13;
		letter_to_id['b'] = 14;
		letter_to_id['x'] = 15;

		letter_to_id['*'] = 16;
		letter_to_id[' '] = 16;
		letter_to_id['-'] = 16;

		id_to_letter[0] = 'A';
		id_to_letter[1] = 'C';
		id_to_letter[2] = 'G';
		id_to_letter[3] = 'T';
		id_to_letter[4] = 'N';
		id_to_letter[5] = 'M';
		id_to_letter[6] = 'R';
		id_to_letter[7] = 'W';
		id_to_letter[8] = 'S';
		id_to_letter[9] = 'Y';
		id_to_letter[10] = 'K';
		id_to_letter[11] = 'V';
		id_to_letter[12] = 'H';
		id_to_letter[13] = 'D';
		id_to_letter[14] = 'B';
		id_to_letter[15] = 'X';

		id_to_letter[16] = '*';
	}

	/**
	 * determines the complement of a sequence of nucleotides.
	 *
	 * @param s a string of nucleotide codes.
	 * @return the complementary codes.
	 */
	public static String complement(String s) {
		if (s == null)  { return null; }
		StringBuffer buf = new StringBuffer(s);
		DNAUtils.complementBuffer(buf);
		return buf.toString();
	}

	/**
	 * determines the reverse complement of a sequence of nucleotides.
	 *
	 * @param s a string of nucleotide codes.
	 * @return the complementary codes in reverse order.
	 */
	public static String reverseComplement(String s) {
		if (s == null) { return null; }
		StringBuffer buf = new StringBuffer(s.length());
		for (int i=s.length()-1; i>=0; i--) {
			buf.append(s.charAt(i));
		}
		complementBuffer(buf);
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

	/**
	 * Gets the reverse complement of a sequence of nucleotides.
	 *
	 * @param sb  nucleotide codes.
	 * @return the complementary codes in reverse order in a new String.
	 */
	public static String getReverseComplement(CharSequence sb) {
		if ( null == sb ) { return null; }
		int length = sb.length();
		char[] sbRC = new char[length];

		for (int i=0, j=length-1; j>= 0; i++, j--) {
			sbRC[i] = complementChar(sb.charAt(j));
		}

		return new String(sbRC);
	}
	/**
	 * determines the reverse of a sequence of nucleotides.
	 *
	 * @param s a string of nucleotide codes.
	 * @return the codes in reverse order.
	 */
	public static String reverse(String s) {
		if (s == null) { return null; }
		StringBuilder buf = new StringBuilder(s);
		for (int i=s.length()-1; i>=0; i--) {
			buf.append(s.charAt(i));
		}
		return buf.toString();
	}

	/**
	 * determines the reverse of a part of a sequence of nucleotides.
	 *
	 * @param s a string of nucleotide codes.
	 * @param offset the number of characters to skip
	 *               at the beginning of s.
	 * @param chunk_size the number of characters in the portion
	 *                   to be reversed
	 * @return the codes of the specified chunk, in reverse order.
	 */
	public static String chunkReverse(String s, int offset, int chunk_size) {
		if (s == null) { return null; }
		int reverse_offset = (s.length()-offset) % chunk_size;
		StringBuilder buf = new StringBuilder(s.length());
		for (int i = 0; i < reverse_offset; i++) {
			buf.append(' ');
		}
		int max = s.length() - reverse_offset - chunk_size;
		String chunk;
		for (int i = max; i >= 0; i -= chunk_size) {
			chunk = s.substring(i, i+chunk_size);
			buf.append(chunk);
		}
		int end_spaces = s.length() - buf.length();
		for (int i = 0; i < end_spaces; i++) {
			buf.append(' ');
		}
		return buf.toString();
	}

	public static char complementChar(char b) {

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
		}
		return '-';
	}


	/**
	 * determines the complement of a sequence of nucleotides.
	 *
	 * @param buf a string of nucleotide codes
	 *            each of which is replaced
	 *            with its complementary code.
	 * @see #complement
	 */
	protected static void complementBuffer(StringBuffer buf) {
		char base;
		for (int i=0; i<buf.length(); i++) {
			base = buf.charAt(i);
			buf.setCharAt(i, complementChar(base));
		}
	}

	/**
	 * gets a representation of the genetic code.
	 * The three dimensions of the array returned correspond
	 * to the three nucleotides in a codon.
	 * Each dimension ranges from 0 to 4
	 * representing bases A, C, G, T, and N respectively.
	 * Prefer the constants A, C, G, T, and N to the integers
	 * when subscripting the array.
	 *
	 * @return the genetic code
	 *         expressed in three-character amino acid codes.
	 */
	public static String[][][] getGeneticCodeThree() {
		return aa3;
	}

	/**
	 * gets a representation of the genetic code.
	 * The three dimensions of the array returned correspond
	 * to the three nucleotides in a codon.
	 * Each dimension ranges from 0 to 4
	 * representing bases A, C, G, T, and N respectively.
	 * Prefer the constants A, C, G, T, and N to the integers
	 * when subscripting the array.
	 *
	 * @return the genetic code
	 *         expressed in one-character amino acid codes.
	 */
	public static String[][][] getGeneticCodeOne() {
		return aa1;
	}

	/**
	 * gets a translation into amino acids of a string of nucleotides.
	 *
	 * @param s represents the string of nucleotides.
	 * @param frametype FRAME_ONE, FRAME_TWO, or FRAME_THREE.
	 *                  For reverse strand frames,
	 *                  translate the reverse complement.
	 *                  Then reverse that result.
	 * @param codetype ONE_LETTER_CODE, or THREE_LETTER_CODE
	 *                 indicating how many letters should encode each amino acid.
	 * @return a representation of the amino acid sequence
	 *         encoded by the given nucleotide sequence.
	 */
	public static String translate(String s, int frametype, int codetype) {
		return translate(s, frametype, codetype, null, null, null);
	}

	/**
	 * gets a translation into amino acids of a string of nucleotides.
	 *
	 * @param s represents the string of nucleotides.
	 * @param frametype FRAME_ONE, FRAME_TWO, or FRAME_THREE.
	 *                  For reverse strand frames,
	 *                  translate the reverse complement.
	 *                  Then reverse that result.
	 * @param codetype ONE_LETTER_CODE, or THREE_LETTER_CODE
	 *                 indicating how many letters should encode each amino acid.
	 * @param initial_string what goes at front of entire translation
	 * @param pre_string what goes before every amino acid
	 * @param post_string what goes after every amino acid
	 * @return a representation of the amino acid sequence
	 *         encoded by the given nucleotide sequence.
	 */
	public static String translate(String s, int frametype, int codetype,
			String initial_string,
			String pre_string, String post_string) {
		String result = null;
		if (codetype == ONE_LETTER_CODE || codetype == 1) {
			result =
				translate(s, frametype, getGeneticCodeOne(),
						initial_string, pre_string, post_string);
		}
		else if (codetype == THREE_LETTER_CODE || codetype == 3) {
			result =
				translate(s, frametype, getGeneticCodeThree(),
						initial_string, pre_string, post_string);
		}
		return result;
	}

	/**
	 * gets a translation into amino acids of a string of nucleotides.
	 *
	 * @param s represents the string of nucleotides.
	 * @param frametype FRAME_ONE, FRAME_TWO, or FRAME_THREE.
	 *                  For reverse strand frames,
	 *                  translate the reverse complement.
	 *                  Then reverse that result.
	 * @param genetic_code the result of one of the getGeneticCode methods
	 *                     of this class.
	 * @param initial_string what goes at front of entire translation
	 * @param pre_string what goes before every amino acid
	 * @param post_string what goes after every amino acid
	 * @return a representation of the amino acid sequence
	 *         encoded by the given nucleotide sequence.
	 * @see #getGeneticCodeOne
	 * @see #getGeneticCodeThree
	 */
	// currently only translates in +1, +2, +3
	// for -1, -2, -3: translate reverse complement, then reverse result
	// initial_string is what goes at front of entire translation
	// pre_string is what goes before every amino acid
	// post_string is what goes after every amino acid
	public static String translate(String s, int frametype,
			String[][][] genetic_code,
			String initial_string,
			String pre_string, String post_string) {

		// Checking to see if the string is null --HARI 4/23/2000
		if (s == null || s.length() == 0) {
			System.err.println("ERROR: Sequence string passed to DNAUtils.translate() is empty");
			return null;
		}

		int frame = FRAME_MAPPING[frametype];
		int length = s.length();
		byte[] basenums = new byte[length];

		for (int i=0; i<length; i++) {
			switch (s.charAt(i)) {
				case 'A':
				case 'a':
					basenums[i] = 0;
					break;
				case 'C':
				case 'c':
					basenums[i] = 1;
					break;
				case 'G':
				case 'g':
					basenums[i] = 2;
					break;
				case 'T':
				case 't':
					basenums[i] = 3;
					break;
				case 'N':
				case 'n':
					basenums[i] = 4;
					break;
				case 'M':
				case 'm':
					basenums[i] = 5;
					break;
				case 'R':
				case 'r':
					basenums[i] = 6;
					break;
				case 'W':
				case 'w':
					basenums[i] = 7;
					break;
				case 'S':
				case 's':
					basenums[i] = 8;
					break;
				case 'Y':
				case 'y':
					basenums[i] = 9;
					break;
				case 'K':
				case 'k':
					basenums[i] = 10;
					break;
				case 'V':
				case 'v':
					basenums[i] = 11;
					break;
				case 'H':
				case 'h':
					basenums[i] = 12;
					break;
				case 'D':
				case 'd':
					basenums[i] = 13;
					break;
				case 'B':
				case 'b':
					basenums[i] = 14;
					break;
				case 'X':
				case 'x':
					basenums[i] = 15;
					break;

					/* Bug fix:
					   The default case was basenums[i]=16.
					   This causes ArrayOutOfBounds exception when accessing the aa1 array.
					   For eg: (In the 'for loop below)
					   genetic_code[basenums[i]][basenums[i+1]][basenums[i+2]];
					   Exception is thrown when trying to access genetic_code[16][16][16].
					   Range of array aa1/genetic_code is 0 thru 15.
					   Changing the default case to 15 to fix this bug. This should not affect the
					   translation as 'X' represents A/C/G or T.
					   translate method because codon XXX does not translate to any aa residue in
					   the genetic code.  -- HARI 4/24/2000
					   */
				default:
					basenums[i] = 15;
			}
		}

		String residue;
		int residue_charsize = genetic_code[0][0][0].length();
		if (pre_string != null) { residue_charsize += pre_string.length(); }
		if (post_string != null) { residue_charsize += post_string.length(); }

		StringBuilder amino_acids = new StringBuilder(length);

		if (initial_string != null ) amino_acids.append(initial_string);
		// checking for no spaces, can build non-spaced faster by avoiding
		//     amino_acids.append("") calls
		int extra_bases = (length - Math.abs(frame)) % 3;
		if (pre_string == null && post_string == null) {
			for (int i = frame; i < length-2; i += 3) {
				residue = genetic_code[basenums[i]][basenums[i+1]][basenums[i+2]];
				amino_acids.append(residue);
			}

			for (int i = 0; i < extra_bases; i++) {
				amino_acids.append(" ");
			}
		}
		else {
			if (pre_string == null) { pre_string = ""; }
			if (post_string == null) { post_string = ""; }
			for (int i = frame; i< length-2; i+=3) {
				residue = genetic_code[basenums[i]][basenums[i+1]][basenums[i+2]];
				amino_acids.append(pre_string);
				amino_acids.append(residue);
				amino_acids.append(post_string);
			}
			for (int i = 0; i < extra_bases; i++) {
				amino_acids.append(" ");
			}
		}
		return amino_acids.toString();
	}

	/**
	 * gets an index into an array of codes for nucleotides.
	 *
	 * @param residue_letter letter representation of a nucleotide.
	 * @return ordinal of nucleotide letter code.
	 * @see #getResidueChar
	 */
	public static int getResidueID(char residue_letter) {
		return letter_to_id[residue_letter];
	}

	/**
	 * gets a nucleotide code.
	 *
	 * @param residue_id ordinal of nucleotide letter code.
	 * @return letter representation of a nucleotide.
	 * @see #getResidueChar
	 */
	public static char getResidueChar(int residue_id) {
		return id_to_letter[residue_id];
	}

	/**
	 * gets a map from letters to numbers
	 * each representing nucleotides.
	 */
	public static int[] getNACharToIdMap() {
		return letter_to_id;
	}

	/**
	 * gets a map from numbers to letters
	 * each representing nucleotides.
	 */
	public static char[] getNAIdToCharMap() {
		return id_to_letter;
	}

	/**
	 * gets the set of valid character codes for nucleotides.
	 * @return an array of all allowed characters codes.
	 */
	public static char[] getAllowedDNACharacters() {
		return dna_chars;
	}

	/**
	 * Fill a color matrix with settings for the given match, mismatch,
	 * and semi_match.  The color matrix must be at least LETTERS sized
	 * in both dimensions.
	 */

	public static void fillColorMatrix (Color[][] color_matrix,
			Color match,
			Color mismatch,
			Color semi_match,
			Color unknown) {

		// The matrix given us must not be smaller than LETTERS

		if ((color_matrix.length < LETTERS) ||
				(color_matrix[0].length < LETTERS))
			return;

		// Default everything to semi_match,

		for (int i=0; i<color_matrix.length; i++) {
			for (int j=0;j<color_matrix.length; j++) {
				color_matrix[i][j] = semi_match;
			}
		}

		// Assign the matches,

		color_matrix[letter_to_id['A']][letter_to_id['A']] = match;
		color_matrix[letter_to_id['G']][letter_to_id['G']] = match;
		color_matrix[letter_to_id['C']][letter_to_id['C']] = match;
		color_matrix[letter_to_id['T']][letter_to_id['T']] = match;

		// Assign the mis-matches

		// A

		color_matrix[letter_to_id['A']][letter_to_id['G']] = mismatch;
		color_matrix[letter_to_id['A']][letter_to_id['C']] = mismatch;
		color_matrix[letter_to_id['A']][letter_to_id['T']] = mismatch;

		color_matrix[letter_to_id['A']][letter_to_id['S']] = mismatch;
		color_matrix[letter_to_id['S']][letter_to_id['A']] = mismatch;

		color_matrix[letter_to_id['A']][letter_to_id['Y']] = mismatch;
		color_matrix[letter_to_id['Y']][letter_to_id['A']] = mismatch;

		color_matrix[letter_to_id['A']][letter_to_id['K']] = mismatch;
		color_matrix[letter_to_id['K']][letter_to_id['A']] = mismatch;

		color_matrix[letter_to_id['A']][letter_to_id['B']] = mismatch;
		color_matrix[letter_to_id['B']][letter_to_id['A']] = mismatch;

		// C

		color_matrix[letter_to_id['C']][letter_to_id['A']] = mismatch;
		color_matrix[letter_to_id['C']][letter_to_id['G']] = mismatch;
		color_matrix[letter_to_id['C']][letter_to_id['T']] = mismatch;

		color_matrix[letter_to_id['C']][letter_to_id['R']] = mismatch;
		color_matrix[letter_to_id['R']][letter_to_id['C']] = mismatch;

		color_matrix[letter_to_id['C']][letter_to_id['W']] = mismatch;
		color_matrix[letter_to_id['W']][letter_to_id['C']] = mismatch;

		color_matrix[letter_to_id['C']][letter_to_id['K']] = mismatch;
		color_matrix[letter_to_id['K']][letter_to_id['C']] = mismatch;

		color_matrix[letter_to_id['C']][letter_to_id['D']] = mismatch;
		color_matrix[letter_to_id['D']][letter_to_id['C']] = mismatch;

		// G

		color_matrix[letter_to_id['G']][letter_to_id['A']] = mismatch;
		color_matrix[letter_to_id['G']][letter_to_id['C']] = mismatch;
		color_matrix[letter_to_id['G']][letter_to_id['T']] = mismatch;

		color_matrix[letter_to_id['G']][letter_to_id['M']] = mismatch;
		color_matrix[letter_to_id['M']][letter_to_id['G']] = mismatch;

		color_matrix[letter_to_id['G']][letter_to_id['W']] = mismatch;
		color_matrix[letter_to_id['W']][letter_to_id['G']] = mismatch;

		color_matrix[letter_to_id['G']][letter_to_id['Y']] = mismatch;
		color_matrix[letter_to_id['Y']][letter_to_id['G']] = mismatch;

		color_matrix[letter_to_id['G']][letter_to_id['H']] = mismatch;
		color_matrix[letter_to_id['H']][letter_to_id['G']] = mismatch;

		// T

		color_matrix[letter_to_id['T']][letter_to_id['A']] = mismatch;
		color_matrix[letter_to_id['T']][letter_to_id['G']] = mismatch;
		color_matrix[letter_to_id['T']][letter_to_id['C']] = mismatch;

		color_matrix[letter_to_id['T']][letter_to_id['M']] = mismatch;
		color_matrix[letter_to_id['M']][letter_to_id['T']] = mismatch;

		color_matrix[letter_to_id['T']][letter_to_id['R']] = mismatch;
		color_matrix[letter_to_id['R']][letter_to_id['T']] = mismatch;

		color_matrix[letter_to_id['T']][letter_to_id['S']] = mismatch;
		color_matrix[letter_to_id['S']][letter_to_id['T']] = mismatch;

		color_matrix[letter_to_id['T']][letter_to_id['V']] = mismatch;
		color_matrix[letter_to_id['V']][letter_to_id['T']] = mismatch;

		// Dual ones:

		color_matrix[letter_to_id['M']][letter_to_id['K']] = mismatch;
		color_matrix[letter_to_id['K']][letter_to_id['M']] = mismatch;

		color_matrix[letter_to_id['R']][letter_to_id['Y']] = mismatch;
		color_matrix[letter_to_id['Y']][letter_to_id['R']] = mismatch;

		color_matrix[letter_to_id['S']][letter_to_id['W']] = mismatch;
		color_matrix[letter_to_id['W']][letter_to_id['S']] = mismatch;

		// Mismatch a space to anything,

		for (int i=0; i<color_matrix.length; i++) {
			color_matrix[i][letter_to_id[' ']] = mismatch;
			color_matrix[letter_to_id[' ']][i] = mismatch;
		}

		// Use the unknown color, if the matrix is big enough to hold it,

		if (color_matrix.length == (LETTERS + 1)) {
			color_matrix[color_matrix.length-1][color_matrix.length-1] = unknown;
		}


	}

}
