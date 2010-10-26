/**
 *   Copyright (c) 2001-2004 Affymetrix, Inc.
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
 *  a symmetry derived from another "original" symmetry.
 *  It maintains a reference back to that original symmetry.
 */
public interface DerivedSeqSymmetry extends MutableSeqSymmetry {
	public SeqSymmetry getOriginalSymmetry();
	public void setOriginalSymmetry(SeqSymmetry sym);
}
