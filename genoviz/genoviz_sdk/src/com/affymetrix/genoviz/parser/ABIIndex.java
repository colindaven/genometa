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

package com.affymetrix.genoviz.parser;

/**
 * models an ABI trace index entry.
 * This is meant for use only by the {@link ABITraceParser}.
 *
 * @author Susana Lewis
 * @author Eric Blossom
 */
public class ABIIndex
{
	int offset = 0;
	boolean occur = false;
	int sizwrd;
	int numbyt = 0;
	int numwrd = 0;
	String label;
	int serial_number;

	ABIIndex (String label, int serial_number) {
		this.label = label;
		this.serial_number = serial_number;
	}

	public String toString () {
		StringBuffer s = new StringBuffer();
		s.append ("index.label:\t" + label);
		s.append ("index.serial_number:\t" + serial_number);
		if (occur) {
			s.append ("index.offset:\t" + offset);
			s.append ("index.sizwrd:\t" + sizwrd);
			s.append ("index.numbyt:\t" + numbyt);
			s.append ("index.numwrd:\t" + numwrd);
		}
		return new String(s);
	}

}
