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

import cern.colt.list.ByteArrayList;
import cern.colt.list.DoubleArrayList;
import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.ShortArrayList;
import java.io.*;
import java.util.*;

public final class AffyChpColumnData {
	String name;
	AffyDataType type;
	int size;
	Object theData = null;
	ByteArrayList dataByte = null;
	DoubleArrayList dataDouble = null;
	IntArrayList dataInt = null;
	FloatArrayList dataFloat = null;
	ShortArrayList dataShort = null;
	ArrayList<CharSequence> dataString = null;
	AffySingleChromData singleChromData;

	AffyChpColumnData(AffySingleChromData singleChromData, String name, AffyDataType type, int size) {
		this.singleChromData = singleChromData;
		this.name = name;
		this.type = type;
		this.size = size;
		switch (this.type) {
			case INT8:
				theData = dataByte = new ByteArrayList(); break;
			case UINT8:
				theData = dataShort = new ShortArrayList(); break;
			case INT16:
				theData = dataShort = new ShortArrayList(); break;
			case UINT16:
				theData = dataInt = new IntArrayList(); break;
			case INT32:
				theData = dataInt = new IntArrayList(); break;
			case UINT32:
				theData = dataInt = new IntArrayList(); break; //TODO: really need to use unsigned int
			case FLOAT:
				theData = dataFloat = new FloatArrayList(); break;
			case DOUBLE:
				theData = dataDouble = new DoubleArrayList(); break;
			case TEXT_ASCII:
				theData = dataString = new ArrayList<CharSequence>(); break;
			case TEXT_UTF16BE:
				theData = dataString = new ArrayList<CharSequence>(); break;
			default:
				throw new RuntimeException("Can't parse that type: " + type);
		}
	}

	void addData(DataInputStream dis) throws IOException {
		switch (this.type) {
			case INT8:
				dataByte.add(dis.readByte()); break;
			case UINT8:
				dataShort.add((short) dis.readUnsignedByte()); break;
			case INT16:
				dataShort.add(dis.readShort()); break;
			case UINT16:
				dataInt.add(dis.readUnsignedShort()); break;
			case INT32:
				dataInt.add(dis.readInt()); break;
			case UINT32:
				dataInt.add(dis.readInt()); break; //TODO: really need to use unsigned int
			case FLOAT:
				dataFloat.add(dis.readFloat()); break;
			case DOUBLE:
				dataDouble.add(dis.readDouble()); break;
			case TEXT_ASCII:
				dataString.add(AffyGenericChpFile.parseString(dis)); break;
			case TEXT_UTF16BE:
				dataString.add(AffyGenericChpFile.parseWString(dis)); break;
			default:
				throw new RuntimeException("Can't parse that type: " + type);
		}
	}

	/** Gets the number of bytes required to store one data entry. */
	int getByteLength() {
		return size;
	}

	/**
	 *  Returns the data as an instance of FloatList, IntList, ShortArrayList,
	 *  ByteList, DoubleArrayList, ArrayList<CharSequence>, or null.
	 */
	Object getData() {
		return theData;
	}

	@Override
		public String toString() {
			String s = this.getClass().getName() +
				": " + name + ", " + type + ", " + size + ", " + theData.getClass().getName() + ":  ";
			for (int i =0; i<5; i++) {
				switch (this.type) {
					case INT8:
						s = s + dataByte.get(i) + ", "; break;
					case UINT8:
						s = s + dataInt.get(i) + ", "; break;
					case INT16:
						s = s + dataShort.get(i) + ", "; break;
					case UINT16:
						s = s + dataInt.get(i) + ", "; break;
					case INT32:
						s = s + dataInt.get(i) + ", "; break;
					case UINT32:
						s = s + dataInt.get(i) + ", "; break; //TOODO: unit32
					case FLOAT:
						s = s + dataFloat.get(i) + ", "; break;
					case DOUBLE:
						s = s + dataDouble.get(i) + ", "; break;
					case TEXT_ASCII:
						s = s + dataString.get(i) + ", "; break;
					case TEXT_UTF16BE:
						s = s + dataString.get(i) + ", "; break;
					default:
						throw new RuntimeException("Can't parse that type: " + type);
				}
			}
			s += " ...";
			return s;
		}

	void dump(PrintStream str) {
		str.println(this.toString());
	}
}
