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

import java.io.PrintStream;

public final class AffyChpColumnType {
	String name;
	AffyDataType type;
	int size;

	public AffyChpColumnType(String name, byte type, int size) {
		this.name = name;
		this.type = AffyDataType.getType((int) type);
		this.size = size;
	}

	@Override
		public String toString() {
			return this.getClass().getName() + ": " + name + ", " + type + ", " + size;
		}

	void dump(PrintStream str) {
		str.println(this.toString());
	}
}
