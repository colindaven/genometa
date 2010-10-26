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

import java.io.*;
import java.nio.*;

public final class AffyChpParameter {
	String name;
	byte[] valueBytes;
	AffyDataType type;

	static AffyChpParameter parse(DataInputStream dis) throws IOException {
		AffyChpParameter param = new AffyChpParameter();
		param.name = AffyGenericChpFile.parseWString(dis);
		param.valueBytes = parseValueString(dis);
		param.type = AffyDataType.getType(AffyGenericChpFile.parseWString(dis));
		return param;
	}

	/** Parses a byte array specified as length (int) followed by bytes.
	 *  In practice, length seems to always be 16.
	 */
	private static byte[] parseValueString(DataInputStream istr) throws IOException {
		int len = istr.readInt();
		byte bytes[] = new byte[len];
		istr.readFully(bytes);
		return bytes;
	}

	@Override
		public String toString() {
			return "Parameter:  Name: " + name + " type: " + type.affyMimeType + " value: " + getValue();
		}

	Object getValue() {
		Object result = valueBytes;

		ByteBuffer bb = ByteBuffer.wrap(valueBytes);
		bb.order(ByteOrder.BIG_ENDIAN);

		switch (type) {
			case INT8:
				result = Integer.valueOf((int) (char) valueBytes[0]); break; // untested
			case UINT8:
				result = Integer.valueOf((int) (char) valueBytes[0]); break; // untested (seems to be wrong)
			case INT16:
				result = Short.valueOf(bb.getShort()); break; // untested
			case UINT16:
				result = Integer.valueOf((int) (char) bb.getShort()); break; // untested
			case INT32:
				result = Integer.valueOf(bb.getInt(0)); break; //OK
			case UINT32:
				throw new RuntimeException("Can't do type UINT32");
			case FLOAT:
				result = new Float(bb.getFloat(0)); break; //OK
			case TEXT_ASCII:
				result = AffyGenericChpFile.makeString(valueBytes, AffyDataType.UTF8); break;//OK
			case TEXT_UTF16BE:
				result = AffyGenericChpFile.makeString(valueBytes, AffyDataType.UTF16); break;//OK
			default:
				result = valueBytes; break;
		}

		return result;
	}

	void dump(PrintStream str) {
		str.println(this.toString() + "<" + this.getValue().getClass().getName() + ">");
	}

}
