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
package com.affymetrix.genometryImpl.parsers.gchp;

import java.util.ArrayList;
import java.util.List;


/**
 *  Specifies which chromosomes to load during file or stream parsing.
 */
public abstract class ChromLoadPolicy {

	public ChromLoadPolicy() {}

	public abstract boolean shouldLoadChrom(String chromName);

	static final ChromLoadPolicy LOAD_ALL = new ChromLoadPolicy() {
		@Override
			public boolean shouldLoadChrom(String chromName) {
				return true;
			}
	};

	static final ChromLoadPolicy LOAD_NOTHING = new ChromLoadPolicy() {
		@Override
			public boolean shouldLoadChrom(String chromName) {
				return false;
			}
	};

	public static ChromLoadPolicy getLoadAllPolicy() {
		return LOAD_ALL;
	}

	public static ChromLoadPolicy getLoadNothingPolicy() {
		return LOAD_NOTHING;
	}

	public static ChromLoadPolicy getLoadListedChromosomesPolicy(List<String> list) {
		final List<String> chromList = new ArrayList<String>(list);
		return new ChromLoadPolicy() {
			@Override 
				public boolean shouldLoadChrom(String chromName) {
					return chromList.contains(chromName);
				}
		};
	}
}
