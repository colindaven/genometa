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

package com.affymetrix.genometryImpl.util;

/**
 * A little utility class to help monitor performance.
 */
public final class Timer {
	private long start_time, read_time;

	/**
	 * Notes a start time.
	 */
	public void start() {
		start_time = System.currentTimeMillis();
	}

	/**
	 * @return the time since the start method was called.
	 */
	public long read() {
		read_time = System.currentTimeMillis();
		return read_time - start_time;
	}

	/**
	 * Prints the time since the start method was called.
	 */
	public void print() {
		System.out.println("Elapsed time since start: " + (double)this.read()/1000f);
	}
}
