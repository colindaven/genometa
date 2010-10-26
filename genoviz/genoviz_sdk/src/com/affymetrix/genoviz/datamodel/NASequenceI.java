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
 * models a nucleotide sequence.
 */
public interface NASequenceI extends SequenceI {

	public String getReverseComplement();

	public String getComplement();

	public String getReverse();

	public void setTranslationStyle(int codetype);

	public String getTranslation(int frametype);

}
