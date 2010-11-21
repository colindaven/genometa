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

package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.util.NeoConstants;

/**
 * A glyph that has direction.
 * It can be directed "forward" or "reverse".
 * The motivation is to have glyphs that point
 * toward the 3' or 5' ends of a nucleotide sequence
 * or away from or toward the start of a peptide sequence
 * respectively.
 *
 * <p> They may also be pointed toward or away from the primary axis.
 * Since the view is not oriented,
 * the factory that produces these glyphs needs
 * to set the glyph's orientation
 * based on the orientation of the factory.
 *
 * <p> The intent is for directed glyphs to rotate themselves by 180 degrees
 * when isForward is false.
 *
 * <p> Take a look at the subclasses to see examples
 * of how to draw directed glyphs.
 */
public abstract class DirectedGlyph extends SolidGlyph {

	private int orientation = NeoConstants.HORIZONTAL;

	public void setOrientation( int theOrientation ) {
		switch ( theOrientation ) {
			case NeoConstants.HORIZONTAL:
			case NeoConstants.VERTICAL:
				this.orientation = theOrientation;
				break;
			default:
				throw new IllegalArgumentException
					( "Glyph orientation must be HORIZONTAL or VERTICAL." );
		}
	}

	public int getOrientation() {
		return this.orientation;
	}

	private boolean forward = true;

	public final void setForward( boolean forward ) {
		this.forward = forward;
	}

	public final boolean isForward() {
		return this.forward;
	}

	/**
	 * possible direction
	 * @see #getDirection
	 */
	protected static final int EAST = 1, SOUTH = 2, WEST = 4, NORTH = 8;

	/**
	 * gets the direction of the glyph.
	 */
	public int getDirection() {
		if ( this.isForward() && NeoConstants.HORIZONTAL == this.getOrientation() ) {
			return EAST;
		}
		if ( this.isForward() && NeoConstants.VERTICAL == this.getOrientation() ) {
			return SOUTH;
		}
		if ( !this.isForward() && NeoConstants.HORIZONTAL == this.getOrientation() ) {
			return WEST;
		}
		if ( !this.isForward() && NeoConstants.VERTICAL == this.getOrientation() ) {
			return NORTH;
		}
		throw new IllegalStateException
			( "A directed glyph has no direction." );
	}

	/**
	 * sets the coordinates in "coord" space.
	 *
	 * <p> Here we also set the direction to forward or reverse
	 * depending on the orientation and whether the "length"
	 * of the glyph is negative.
	 */
	public void setCoords(double x, double y, double width, double height)  {
		super.setCoords(x, y, width, height);
		switch ( this.getOrientation() ) {
			case NeoConstants.HORIZONTAL:
				setForward( 0 <= width );
				break;
			case NeoConstants.VERTICAL:
				setForward( 0 <= height );
				break;
		}
	}

}
