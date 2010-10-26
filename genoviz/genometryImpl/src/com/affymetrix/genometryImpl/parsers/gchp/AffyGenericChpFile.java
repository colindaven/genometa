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

import com.affymetrix.genometryImpl.GenometryModel;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/** A parser for the Affymetrix Generic CHP format. */
public final class AffyGenericChpFile {

	private int magic; // magic number.  Always 59.
	private int version; // version number.  Always 1.
	private int num_groups;
	private int group_0_pos;
	private Map<String,AffyChpParameter> parameterMap = new LinkedHashMap<String,AffyChpParameter>();
	private AffyGenericDataHeader header;
	List<AffyDataGroup> groups;
	private final File file;
	private final ChromLoadPolicy loadPolicy;

	/** Creates a new instance of AffyCnChpParser */
	private AffyGenericChpFile(File file, ChromLoadPolicy loadPolicy) {
		this.file = file;
		this.loadPolicy = loadPolicy;
	}

	/** Parses a string in UTF-16BE format, with the length specified first as an int. */
	static String parseWString(DataInputStream istr) throws IOException {
		int len = istr.readInt();
		byte bytes[] = new byte[len * 2];
		istr.readFully(bytes);
		return makeString(bytes, AffyDataType.UTF16);
	}

	/** Parses a string in UTF-8 format, with the length specified first as an int. */
	static CharSequence parseString(DataInputStream istr) throws IOException {
		int len = istr.readInt();
		byte bytes[] = new byte[len];
		istr.readFully(bytes);
		return makeString(bytes, AffyDataType.UTF8);
	}

	/** Parses the file.  Does not close the stream. 
	 *  @param file  The file that the input stream corresponds to, or null if it
	 *   does not come from a file.  If the file is not null, it may be possible
	 *   to defer loading some of the data until it is needed.
	 *  @param headerOnly if true, will read the complete header, but will not
	 *  read any data groups.
	 */
	public static AffyGenericChpFile parse(File file, ChromLoadPolicy loadPolicy, InputStream istr, boolean headerOnly) throws IOException  {

		AffyGenericChpFile chpFile = new AffyGenericChpFile(file, loadPolicy);

		if (file != null) {
			if (headerOnly) {
				Logger.getLogger(AffyGenericChpFile.class.getName()).log(
							Level.INFO, "Parsing header of file: {0}", file.getName());
			} else {
				Logger.getLogger(AffyGenericChpFile.class.getName()).log(
							Level.INFO, "Parsing file: {0}", file.getName());
			}
		}

		DataInputStream dis = new DataInputStream(istr);

		chpFile.magic = dis.readUnsignedByte();
		chpFile.version = dis.readUnsignedByte();

		if (chpFile.magic != 59) {
			throw new IOException("Error in chp file format: wrong magic number: " + chpFile.magic);
		}

		chpFile.num_groups = dis.readInt();
		chpFile.group_0_pos = dis.readInt(); // TODO: signed vs unsigned?

		chpFile.parameterMap = new LinkedHashMap<String,AffyChpParameter>();
		chpFile.header = AffyGenericDataHeader.readHeader(dis);

		chpFile.groups = new ArrayList<AffyDataGroup>(chpFile.num_groups);
		if (! headerOnly) {
			for (int i=0; i<chpFile.num_groups; i++) {
				AffyDataGroup group = AffyDataGroup.parse(chpFile, dis);
				chpFile.groups.add(group);
			}
		}

		return chpFile;
	}

	/** Creates a String from the given bytes, using the given Charset and
	 *  trimming off any trailing '\0' characters.
	 */
	static String makeString(byte[] bytes, Charset charset) {
		String s = null;
		try {
			//TODO: use new String(byte[], Charset) when we convert all to JDK 1.6
			s = new String(bytes, charset.name());
		} catch (UnsupportedEncodingException ex) {
			ex.printStackTrace();
			return "String could not be parsed: charset " + charset.name() + " not known";
		}
		int index = s.indexOf('\0');
		if (index >= 0) {
			s = new String(s.substring(0, index)); // new String() potentially saves memory
		}
		return s;
	}

	ChromLoadPolicy getLoadPolicy() {
		return loadPolicy;
	}

}
