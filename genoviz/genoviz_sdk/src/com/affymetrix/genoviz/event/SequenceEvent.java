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

package com.affymetrix.genoviz.event;

import com.affymetrix.genoviz.datamodel.SequenceI;

import java.util.EventObject;

/**
 * A change in a sequence.
 * c.f. Swing's DocumentEvent.
 */
public class SequenceEvent extends EventObject {

	private final EditType type;
	private final int offset;
	private final int length;

	public SequenceEvent( SequenceI theSource,
			EditType theType,
			int theOffset,
			int theLength ) {
		super( theSource );
		if ( theOffset < 0 )
			throw new IllegalArgumentException
				( "theOffset must not be negative." );
		if ( theLength < 0 )
			throw new IllegalArgumentException
				( "theLength must not be negative." );
		this.type= theType;
		this.offset = theOffset;
		this.length = theLength;
	}

	/**
	 * Gets the start of the change.
	 *
	 * @return the number of characters before the change.
	 */
	public int getOffset() {
		return this.offset;
	}

	/**
	 * Gets the length of the change.
	 *
	 * @return the length
	 */
	public int getLength() {
		return this.length;
	}

	/**
	 * Gets the Sequence that changed.
	 *
	 * @return the sequence
	 */
	public SequenceI getDocument() {
		return (SequenceI) super.getSource();
	}

	/**
	 * Gets the type of event.
	 *
	 * @return INSERT, REMOVE, or CHANGE
	 */
	public EditType getType() {
		return this.type;
	}


	/**
	 * Typesafe Enumeration of Edit Types.
	 */
	public static final class EditType {

		private final String typeString;

		private EditType(String s) {
			typeString = s;
		}

		public static final EditType INSERT = new EditType("INSERT");

		public static final EditType REMOVE = new EditType("REMOVE");

		public static final EditType CHANGE = new EditType("CHANGE");

		public String toString() {
			return typeString;
		}
	}


}
