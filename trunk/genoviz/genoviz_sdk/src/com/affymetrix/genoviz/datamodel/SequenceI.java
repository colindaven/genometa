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

import com.affymetrix.genoviz.event.SequenceListener;

/**
 * Models a biological sequence with known residues.
 * Optional info fields include name, identifier, and description
 */
public interface SequenceI {

	/**
	 * Sets a unique identifier for this sequence.
	 *
	 * @param id the identifier.
	 */
	public void setID (String id);

	/**
	 * Returns the unique identifier for this sequence.
	 *
	 * @return the identifier set with setID.
	 */
	public String getID ();

	/** @return the sequence length. */
	public int getLength();

	/**
	 * @return numbering for start and end of sequence as a Range.
	 */
	public Range getRange();

	/**
	 * @return numbering for first residue in sequence.
	 */
	public int getStart();

	/**
	 * sets the numbering for the first residue in sequence.
	 * Defaults to 0.
	 */
	public void setStart(int theStart);

	/**
	 *  Set the sequence residues to the characters in the residues String
	 */
	public void setResidues ( String residues );

	/**
	 * sets the residues to those encoded in a StringBuffer.
	 *
	 * @param resBuf contains the encoded residues.
	 */
	public void setResidues ( StringBuffer resBuf );

	/**
	 * Append new residues to the end of this sequence.
	 *
	 * @param new_residues to append.
	 */
	public void appendResidues(String new_residues);

	/**
	 * gets all the residues.
	 *
	 * @return a String of single-letter codes for the residues.
	 */
	public String getResidues ();

	/**
	 * Gets a String representing the residues
	 * numbered from start to end-1
	 *
	 * Note that this method <em>excludes</em> the residue at <em>end</em>.
	 * This is so that it agrees with analogous methods in String and StringBuffer.
	 *
	 * @param start index of the first residue to retrieve.
	 * @param end index of the residue after the last residue to retrieve.
	 */
	public String getResidues(int start, int end);

	/**
	 * Gets a residue at a given position.
	 *
	 * @param n index of the residue to retrieve.
	 * @return the character code for the residue
	 * or 0 is if n is out of bounds.
	 */
	public char getResidue (int n);

	/**
	 * Gets a description of this sequence.
	 *
	 * @return the description set by setDescription.
	 */
	public String getDescription();

	/**
	 * Describes this sequence.
	 *
	 * @param description to assign.
	 */
	public void setDescription(String description);

	/**
	 * Gets this sequence's name.
	 *
	 * @return the name assigned by setName().
	 */
	public String getName();

	/**
	 * Names this sequence.
	 *
	 * @param name the name to assign.
	 */
	public void setName(String name);

	/* Sequence Event Source Methods */

	/**
	 * adds a listener to the list of those listening to this sequence.
	 *
	 * @param l the listener to add.
	 */
	public void addSequenceListener( SequenceListener l );

	/**
	 * removes a listener from the list of those listening to this sequence.
	 *
	 * @param l the listener who is no longer listening.
	 */
	public void removeSequenceListener( SequenceListener l );

}
