/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.parsers;

/**
 *  Parses tab-delimited output "fishClones.txt" files from UCSC.
 *  The file extension should be {@link #FILE_EXT}.
 */
public final class FishClonesParser extends TabDelimitedParser {
	public static final String FILE_EXT = "fsh";
	public static final String FISH_CLONES_METHOD = "fishClones";

	public FishClonesParser(boolean addToIndex) {
		super(-1, 0, 1, 2, -1, -1, -1, 3, false, false, addToIndex);
	}
}
