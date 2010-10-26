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
 * Represents a location within a document.
 * c.f. Swing's javax.swing.text.Position interface.
 */
public interface Position {

	/**
	 * Gets the current position within the document.
	 *
	 * @return the number of characters
	 * between the start and the current position.
	 */
	public abstract int getOffset();
	public abstract void setOffset( int o );

	/**
	 * adds a listener interested in position changes.
	 */
	public abstract void addListener( PositionListener l );
	public abstract void removeListener( PositionListener l );

}
