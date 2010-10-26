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

import java.nio.charset.Charset;

public enum AffyDataType {
	// Note: the order these are listed in is important for getType(int) to work

	INT8("text/x-calvin-integer-8"),
		UINT8("text/x-calvin-unsigned-integer-8"),
		INT16("text/x-calvin-integer-16"),
		UINT16("text/x-calvin-unsigned-integer-16"),
		INT32("text/x-calvin-integer-32"),
		UINT32("text/x-calvin-unsigned-integer-32"),
		FLOAT("text/x-calvin-float"),
		TEXT_ASCII("text/ascii"), //TODO: Undocumented
		TEXT_UTF16BE("text/plain"),
		DOUBLE("text/x-calvin-double"), //TODO: Undocumented mime type // documentation says order is DOUBLE then String then WSTRING
		;

	static final Charset UTF16 = Charset.forName("utf-16be");
	static final Charset UTF8 = Charset.forName("utf-8");

	String affyMimeType;

	/** Contructor to be used only inside this class. */
	AffyDataType(String mimeType) {
		this.affyMimeType = mimeType;
	}

	public static AffyDataType getType(String mimeType) {
		for (AffyDataType type : values()) {
			if (type.affyMimeType.equals(mimeType)) {
				return type;
			}
		}

		throw new RuntimeException("Unknown type: '" + mimeType + "'");
	}

	public static AffyDataType getType(int i) {
		return AffyDataType.values()[i];
	}

	//  public static void main(String[] args) {
	//    for (int i=0; i<=9; i++) {
	//      System.out.println("Type: " + i + ": " + AffyDataType.getType(i));
	//    }
	//  }
}
