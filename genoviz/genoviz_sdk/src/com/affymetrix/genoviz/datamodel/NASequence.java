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

import com.affymetrix.genoviz.util.DNAUtils;

/**
 * models a nucleotide sequence.
 */
public class NASequence extends Sequence implements Translatable, NASequenceI {

	protected boolean debug_exceptions = false;
	protected String revcomp = null;  // reverse complement string if needed
	protected String comp = null;     // complement string if needed
	protected String rev = null;      // reverse string if needed

	// for translations -- has eight elements to mirror Translatable constants
	protected String translations[] = new String[8];
	protected int trans_codetype = ONE_LETTER_CODE;
	protected String trans_prestring = " ";
	protected String trans_poststring = " ";

	public void setResidues ( String residues ) {
		// added check for null as per JM's request GAH 12-10-97
		if (residues != null) {
			// residues and length set in super.setResidues()
			super.setResidues(residues);
		}

		// if previously needed revcomp, comp, or rev, redo it
		// NO -- just null them, redoing them will be triggered when they're
		//     needed (first call to accessor method)
		if (revcomp != null) {
			revcomp = null;
		}
		if (comp != null) {
			comp = null;
		}
		if (rev != null) {
			rev = null;
		}
		for (int i=0; i<translations.length; i++) {
			translations[i] = null;
		}
	}

	public String getReverseComplement() {
		if (revcomp == null) {
			revcomp = DNAUtils.reverseComplement(this.getResidues());
		}
		return revcomp;
	}

	public String getComplement() {
		try {
			if (comp == null) {
				comp = DNAUtils.complement(this.getResidues());
			}
		}
		catch (Exception e) {
			System.out.println("exception in NASequence.getComplement()");
			if (debug_exceptions)  { e.printStackTrace(); }
			return null;
		}
		return comp;
	}

	public String getReverse() {
		if (rev == null) {
			rev = DNAUtils.reverse(this.getResidues());
		}
		return rev;
	}

	public void setTranslationStyle(int codetype) {
		if (codetype != ONE_LETTER_CODE && codetype != THREE_LETTER_CODE) {
			throw new IllegalArgumentException("codetype must be ONE_LETTER_CODE " +
					"or THREE_LETTER_CODE");
		}
		trans_codetype = codetype;

		// reset all translations to null -- they will be redone
		//   when next needed via accessor methods
		for (int i=0; i<translations.length; i++) {
			translations[i] = null;
		}
	}

	public String getTranslation(int frametype) {
		if (translations[frametype] == null) {
			String pre_string = null;
			String post_string = null;
			String initial_spacer = null;

			if (trans_codetype == ONE_LETTER_CODE) {
				// amino acid letter positioned at first base of codon (not centered)
				pre_string = ""; post_string = "  ";
			}
			else {
				pre_string = null; post_string = null;
			}

			if (frametype == FRAME_ONE || frametype == FRAME_TWO ||
					frametype == FRAME_THREE) {
				if (frametype == FRAME_ONE) { initial_spacer = ""; }
				else if (frametype == FRAME_TWO) { initial_spacer = " "; }
				else {  initial_spacer = "  "; }

				try {
					translations[frametype] =
						DNAUtils.translate(getResidues(), frametype, trans_codetype,
								initial_spacer, pre_string, post_string);
				}
				catch (Exception e) {
					System.out.println("exception in NASequence.getTranslation()");
					if (debug_exceptions)  { e.printStackTrace(); }
					return null;
				}
					}
			else if (frametype == FRAME_NEG_ONE || frametype == FRAME_NEG_TWO ||
					frametype == FRAME_NEG_THREE) {
				int rev_frametype;

				if (frametype == FRAME_NEG_ONE) { rev_frametype = FRAME_ONE; }
				else if (frametype == FRAME_NEG_TWO) { rev_frametype = FRAME_TWO; }
				else { rev_frametype = FRAME_THREE; }

				if (rev_frametype == FRAME_ONE) { initial_spacer = ""; }
				else if (rev_frametype == FRAME_TWO) { initial_spacer = " "; }
				else {  initial_spacer = "  "; }

				String revCompTrans = null;
				String compTrans = null;
				try {
					revCompTrans =
						DNAUtils.translate(getReverseComplement(), rev_frametype, trans_codetype,
								initial_spacer, pre_string, post_string);
					compTrans = DNAUtils.chunkReverse(revCompTrans,
							initial_spacer.length(), 3);
				}
				catch (Exception e) {
					System.out.println("exception in NASequence.getTranslation()");
					if (debug_exceptions)  { e.printStackTrace(); }
					return null;
				}

				translations[frametype] = compTrans;
					}
		}

		return translations[frametype];
	}

}
