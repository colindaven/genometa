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
 * Models a biological sequence with known residues.
 * Optional info fields include name, identifier, and description
 */
public interface EditableSequenceI extends SequenceI {

	/**
	 * Append a new residue to the end of this sequence.
	 *
	 * @param new_residue residue to append.
	 */
	public void appendResidue(char new_residue);

	/**
	 * Inserts residues before the residue at a given position.
	 *
	 * @param start points to the residue that will follow the insertion.
	 * @param new_residues the residues to insert.
	 */
	public void insertResidues(int start, String new_residues);

	public void insertString( int offset, String str );

	public void remove( int offset, int length );

	public Position createPosition( int theOffset );

}
