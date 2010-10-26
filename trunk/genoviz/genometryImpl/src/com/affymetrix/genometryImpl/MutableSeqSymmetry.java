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


public interface MutableSeqSymmetry extends SeqSymmetry  {

	public void addSpan(SeqSpan span);
	public void removeSpan(SeqSpan span);
	//public void setSpan(int index, SeqSpan span);

	public void addChild(SeqSymmetry sym);
	public void removeChild(SeqSymmetry sym);

	public void removeChildren();
	public void removeSpans();

	/**  clear _all_ fields...  */
	public void clear();

}
