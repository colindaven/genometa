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
 * This empty BaseCalls can be used instead of null.
 * Only the empty constructor is available.
 * Setter methods do nothing.
 * <p> c.f. Woolf, Bobby, <cite>Null Object</cite>
 * in Martin, et. al. <cite>Pattern Languages of Program Design 3</cite>,
 * Addison Wesley, 1998.
 */
public final class NullBaseCalls extends BaseCalls {

	public NullBaseCalls() {
		super();
	}

	/** does nothing. */
	public void setAligner( Mapping aligner ) {
	}

	/** does nothing. */
	public void setTrace( TraceI trace ) {
	}

	/** does nothing. */
	public void setBaseCalls( BaseCall[] theCalls ){
	}

}
