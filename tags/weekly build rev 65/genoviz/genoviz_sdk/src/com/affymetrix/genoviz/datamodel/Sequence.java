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

import com.affymetrix.genoviz.event.SequenceEvent;
import com.affymetrix.genoviz.event.SequenceListener;
import java.util.ArrayList;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Models a biological sequence with known residues.
 * Optional info fields include name, identifier, and description
 */
public class Sequence implements EditableSequenceI {

	private static final boolean debug_exceptions = false;
	private String id;
	protected int start; // Points to first char.
	private int end; // Points to last char. Not beyond.
	// BUG: end = 0 for both empty string (length=0) and singleton (length=1).
	protected int length; // end - start + 1 (unless empty, then 0)
	private String description;
	private String name;

	private final List<Position> positions = new ArrayList<Position>();
	private final Set<SequenceListener> listeners = new CopyOnWriteArraySet<SequenceListener>();

	private StringBuffer residues;

	public Sequence () {
		this( null );
	}

	/**
	 * Constructs a sequence with the given identifier.
	 *
	 * @param id the identifier.
	 */
	public Sequence (String id) {
		this.start = 0;
		setID( id );
	}

	public Position createPosition( int theOffset ) {
		Position p = new SequencePosition();
		p.setOffset( theOffset );
		this.positions.add( p );
		return p;
	}

	/**
	 * Sets a unique identifier for this sequence.
	 *
	 * @param id the identifier.
	 */
	public void setID (String id) {
		this.id = id;
	}

	/**
	 * Returns the unique identifier for this sequence.
	 *
	 * @return the identifier set with setID.
	 */
	public String getID () {
		return id;
	}

	/** @return the sequence length. */
	public int getLength() {
		return length;
	}

	/**
	 * @return numbering for start and end of sequence as a Range.
	 */
	public Range getRange() {
		return new Range(this.start, this.end);
	}

	/**
	 * @return numbering for first residue in sequence.
	 */
	public int getStart() {
		return start;
	}

	/**
	 * sets the numbering for the first residue in sequence.
	 * Defaults to 0.
	 */
	public void setStart(int theStart) {
		if ( theStart < 0 )
			throw new IllegalArgumentException
				( "theStart cannot be negative" );
		this.start = theStart;
		this.end = this.start + this.length - 1;
	}

	/*
	   public void setLength(int length) {
	   this.length = length;
	   }
	   */

	/**
	 *  Set the sequence residues to the characters in the residues String
	 */
	public void setResidues ( String residues ) {
		if (residues != null) {
			this.residues = new StringBuffer(residues);
			length = residues.length();
			if (length == 0) {
				this.end = this.start;
			}
			else {
				this.end = this.start + this.length - 1;
			}
		}
	}

	/**
	 * sets the residues to those encoded in a StringBuffer.
	 *
	 * @param resBuf contains the encoded residues.
	 */
	public void setResidues ( StringBuffer resBuf ) {
		if (resBuf != null) {
			residues = resBuf;
		}
	}

	/**
	 * Append a new residue to the end of this sequence.
	 *
	 * @param new_residue residue to append.
	 */
	public void appendResidue(char new_residue) {
		residues.append(new_residue);
		length++;
	}

	/**
	 * Append new residues to the end of this sequence.
	 *
	 * @param new_residues to append.
	 */
	public void appendResidues(String new_residues) {
		residues.append(new_residues);
		length += new_residues.length();
		this.end = this.start + this.length;
	}

	/**
	 * Inserts residues before the residue at a given position.
	 *
	 * @param start points to the residue that will follow the insertion.
	 * @param new_residues the residues to insert.
	 */
	public void insertResidues(int start, String new_residues) {
		if (start >= residues.length()) {
			throw new IllegalArgumentException("try appending instead");
		}
		else {
			residues.insert(start, new_residues);
			length += new_residues.length();
		}
	}

	/**
	 * gets all the residues.
	 *
	 * @return a String of single-letter codes for the residues.
	 */
	public String getResidues () {

		if (residues == null) { return null; }
		return residues.toString();

		/*
		   An alternative design would be to store a String internally.
		   This internal String would be created by the first call to this method.
		   The String would be set to null every time the StringBuffer was changed.
		   This method would then create another snapshot whenever the String was null.
		   The advantage of this alternative would be (hopefully) fewer Strings being created.
		   The internal String would act as a kind of cache.

		   The advantage of the current implementation is simplicity.
		   If you believe the Java documentation,
		   the StringBuffer.toString() method uses a pointer
		   to the StringBuffer's internal String.
		   A new StringBuffer is created only when the StringBuffer is changed again.
		   So this alternative should be pretty similar to the one above.
		   Another advantage is that, if the caller discards the returned String,
		   then it can be garbage collected.
		   There is no internal cache carried when not needed.

		   -- Eric 1999-04-08
		   */

	}

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
	public String getResidues(int start, int end) {
		// CLH: This is counter-intuitive...
		//I'm changing it back to match the way substring operates,
		//so if you really want bases 1 through 50, do getResidues(1,51)
		//
		//returns substring of sequence residues,
		//  _inclusive_ of end point (unlike String.substring(beg,end))

		//    System.out.println(residues.length() + ", " + start + ", " + end);
		if (length == 0) { return ""; }
		char[] carray = new char[end - start];
		try  {
			residues.getChars(start, end, carray, 0);
		}
		catch (Exception e)  {
			System.out.println("exception in Sequence.getResidues(start, end)");
			System.out.println("start = " + start + ", end = " + end);

			if (debug_exceptions)  { e.printStackTrace(); }
			return null;
		}
		return new String(carray);
	}

	/**
	 * Gets a residue at a given position.
	 *
	 * @param n index of the residue to retrieve.
	 * @return the character code for the residue
	 * or 0 is if n is out of bounds.
	 */
	public char getResidue (int n) {
		char c = '\000';
		try {
			c = residues.charAt( n - this.start );
		} catch ( RuntimeException e ) {
			//System.err.println( "getting residue " + n );
			//System.err.println( "start is at " + this.start );
			//System.err.println( "end is at " + this.end );
			//System.err.println( "length is " + this.length );
		}
		return c;
	}

	/**
	 * Gets a description of this sequence.
	 *
	 * @return the description set by setDescription.
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Describes this sequence.
	 *
	 * @param description to assign.
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Gets this sequence's name.
	 *
	 * @return the name assigned by setName().
	 */
	public String getName() {
		return name;
	}

	/**
	 * Names this sequence.
	 *
	 * @param name the name to assign.
	 */
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		String s = this.getClass().getName()
			+ ": length = " + this.length;
		if (null != this.id) s += ", id = " + this.id;
		if (null != this.name) s += ", name = " + this.name;
		if (null != this.description) s += ", " + this.description;
		return s;
	}


	/* Document Interface
	 * Here we implement a subset of Swing's Document interface.
	 * Perhaps someday, we can implement all of it
	 * and take greater advantage of Swing. -- Eric
	 */

	/**
	 * inserts a string into the sequence.
	 * c.f. Swing's Document interface.
	 *
	 * @param offset the number of characters to skip before insertion.
	 * @param str the characters to insert.
	 */
	public void insertString( int offset, String str/*, AttributeSet a*/ )
		/* throws BadLocationException */
	{
		if ( this.end + 1 < offset )
			throw new IllegalArgumentException
				( "Cannot insert beyond the end." );
		if ( this.end < offset ) {
			this.residues.append( str );
		}
		else {
			this.residues.insert( offset, str );
		}
		this.length += str.length();
		this.end += str.length();

		// Adjust positions.
		for ( Position p : positions ) {
			int o = p.getOffset();
			if ( offset <= o ) {
				o += str.length();
				p.setOffset( o );
			}
		}

		SequenceEvent e = new SequenceEvent
			( this,
			  SequenceEvent.EditType.INSERT,
			  offset,
			  str.length() );
		processSequenceEvent( e );

	}


	/**
	 * removes residues from the sequence.
	 * c.f. Swing's Document interface.
	 *
	 * @param offset the number of characters to skip before removal.
	 * @param length the number of characters to remove.
	 */
	public void remove( int offset, int length )
	{
		if ( this.end + 1 < offset + length )
			throw new IllegalArgumentException
				( "Cannot remove beyond the end." );
		char[] tail = new char[this.length - offset - length];
		if ( 0 < tail.length ) {
			this.residues.getChars( offset + length, this.length, tail, 0 );
		}
		this.residues.setLength( offset );
		this.residues.append( tail );
		this.length -= length;
		this.end -= length;
		if ( this.end < 0 ) this.end = 0;

		// Adjust positions.
		for ( Position p : positions ) {
			int o = p.getOffset();
			if ( offset < o ) {
				o -= length;
				p.setOffset( Math.max( o, offset ) );
			}
		}

		SequenceEvent e = new SequenceEvent
			( this,
			  SequenceEvent.EditType.REMOVE,
			  offset,
			  length );
		processSequenceEvent( e );
	}


	/* Sequence Event Source Methods */

	/**
	 * broadcasts a sequence event.
	 *
	 * @param theEvent to broadcast.
	 */
	protected void processSequenceEvent( SequenceEvent theEvent ) {
		if ( null != listeners ) {
			for (SequenceListener l : listeners) {
				l.sequenceChanged( theEvent );
					}
		}
	}

	/**
	 * adds a listener to the list of those listening to this sequence.
	 *
	 * @param l the listener to add.
	 */
	public void addSequenceListener( SequenceListener l ) {
		listeners.add( l );
	}

	/**
	 * removes a listener from the list of those listening to this sequence.
	 *
	 * @param l the listener who is no longer listening.
	 */
	public void removeSequenceListener( SequenceListener l ) {
		listeners.remove( l );
	}

}
