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

package com.affymetrix.genoviz.util;

import java.awt.Font;
import java.awt.Scrollbar;

/**
 * some constants for use in multiple genoviz packages.
 */
public interface NeoConstants {

	public static boolean flush = true;
	public static boolean dispose = true;

	// might want to make all these constants final as well...

	/** same as java.awt.Scrollbar.HORIZONTAL */
	public static int HORIZONTAL = Scrollbar.HORIZONTAL;
	/** same as java.awt.Scrollbar.VERTICAL */
	public static int VERTICAL = Scrollbar.VERTICAL;

	public static int LEFT =  VERTICAL+1;
	public static int RIGHT = VERTICAL+2;
	public static int UP =    VERTICAL+3;
	public static int DOWN =  VERTICAL+4;
	public static int CENTER = VERTICAL+5;
	public static int NONE = VERTICAL+6;

	/** synonym for UP. */
	public static int ABOVE = UP;
	/** synonym for DOWN. */
	public static int BELOW = DOWN;

	/** synonym for LEFT. */
	public static int WEST = LEFT;
	/** synonym for RIGHT. */
	public static int EAST = RIGHT;
	/** synonym for ABOVE. */
	public static int NORTH = ABOVE;
	/** synonym for BELOW. */
	public static int SOUTH = BELOW;

	/** for flipping or reflecting things about a vertical axis. */
	public static int MIRROR_VERTICAL = VERTICAL+5;
	/** for flipping or reflecting things about a horizontal axis. */
	public static int MIRROR_HORIZONTAL = VERTICAL+6;

	public static int UNKNOWN = VERTICAL+6;

	public static Font default_bold_font = new Font("Courier", Font.BOLD, 12);
	public static Font default_plain_font = new Font("Courier", Font.PLAIN, 12);

}
