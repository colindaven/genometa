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

import com.affymetrix.genoviz.datamodel.Trace;
import com.affymetrix.genoviz.datamodel.CalledBase;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * parsers SCF format trace data.
 * It will load the first trace in the SCF file into something
 * implementing {@link com.affymetrix.genoviz.datamodel.TraceI}.
 *
 * @author Eric Blossom
 * @author Cyrus Harmon
 */
public class SCFTraceParser implements ContentParser {

	private static final boolean debug = false;

	protected int comments_offset;
	protected String version;
	protected int sample_size;
	protected int code_set;
	protected int private_size;
	protected int private_offset;
	protected int streampos = 0;

	protected int samples;
	protected int samples_offset;
	protected int bases;
	protected int bases_offset;
	protected int comments_size;

	/**
	 * Magic tag that SCF files use at the beginning of the file to
	 * indicate that this really is an SCF File ('.scf').
	 */
	private static final int scfMagic =
		((int)'.' << 24) + ((int)'s' << 16) + ((int)'c' << 8) + ((int)'f') ;

	/**
	 * imports the data from a stream of bytes.
	 * This is defined as returning an Object
	 * to conform to the ContentParser interface.
	 * However, it actually returns a {@link Trace}.
	 * To use it as such
	 * you will need to cast the returned Object back to a Trace.
	 *
	 * @param theInput from whence the data come.
	 * @return a representation of the trace.
	 */
	public Object importContent( InputStream theInput ) throws IOException {
		Trace trace = new Trace();
		try {
			DataInputStream dis = new DataInputStream(theInput);
			readHeader(dis, trace);
			readSamples(dis, trace);
			readBases(dis, trace);
		} catch (Exception e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return trace;
	}

	/**
	 * not yet implemented.
	 */
	public void exportContent( OutputStream theOutput, Object o )
		throws IOException {
	}

	protected boolean readHeader(DataInputStream scfDataIn, Trace trace) {
		boolean goodHeader = false;
		try {

			int magic = scfDataIn.readInt(); streampos += 4;
			if ( magic == scfMagic ) {
				goodHeader = true;

				samples = scfDataIn.readInt();
				streampos += 4;

				samples_offset = scfDataIn.readInt();
				streampos += 4;

				bases = scfDataIn.readInt();
				streampos += 4;

				trace.setLeftClip(scfDataIn.readInt());
				streampos += 4;

				trace.setRightClip(scfDataIn.readInt());
				streampos += 4;

				bases_offset = scfDataIn.readInt();
				streampos += 4;

				comments_size = scfDataIn.readInt();
				streampos += 4;

				comments_offset = scfDataIn.readInt();
				streampos += 4;

				StringBuffer vb = new StringBuffer();
				vb.append(String.valueOf((char)scfDataIn.readUnsignedByte()));
				streampos++;

				vb.append(String.valueOf((char)scfDataIn.readUnsignedByte()));
				streampos++;

				vb.append(String.valueOf((char)scfDataIn.readUnsignedByte()));
				streampos++;

				vb.append(String.valueOf((char)scfDataIn.readUnsignedByte()));
				streampos++;

				version = vb.toString();

				sample_size = scfDataIn.readInt();
				streampos += 4;

				code_set = scfDataIn.readInt();
				streampos += 4;

				private_size = scfDataIn.readInt();
				streampos += 4;

				private_offset = scfDataIn.readInt();
				streampos += 4;

				int[] spare = new int[18];
				for (int spareIndex = 0; spareIndex < 18; spareIndex++ ) {
					spare[spareIndex] = scfDataIn.readInt(); streampos += 4;
				}

				if (debug)  {
					System.out.println("Samples: " + samples);
					System.out.println("Samples offset: " + samples_offset);
					System.out.println("Bases: " + bases);
					System.out.println("Bases offset: " + bases_offset);
					System.out.println("Comments size: " + comments_size);
					System.out.println("Comments offset: " + comments_offset);
					System.out.println("SCF Version " + version);
					System.out.println("Sample size: " + sample_size);
					System.out.println("Code set: " + code_set);
					System.out.println("Private Size: " + private_size);
					System.out.println("Private offset: " + private_offset);
				}
			}
			else {
				System.out.println("Didn't get an scf file!");
			}
		}
		catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return goodHeader;
	}

	protected boolean readSamples(DataInputStream dis, Trace trace) {
		try {

			dis.skipBytes(samples_offset - streampos);
			streampos = samples_offset;

			if ( Float.valueOf(version).doubleValue() >= 2.0f ) {
				/* There are this.samples points */
				if ( sample_size == 1 ) {
					for (int sampleIndex = 0; sampleIndex < samples; sampleIndex++)  {
						int sample_A = dis.readUnsignedByte(); streampos += 1;
						int sample_C = dis.readUnsignedByte(); streampos += 1;
						int sample_G = dis.readUnsignedByte(); streampos += 1;
						int sample_T = dis.readUnsignedByte(); streampos += 1;
						trace.addSample(sample_A, sample_C, sample_G, sample_T);
					}
				}
				else if ( sample_size == 2 ) {
					for (int sampleIndex = 0; sampleIndex < samples; sampleIndex++) {
						int sample_A = dis.readUnsignedShort(); streampos += 2;
						int sample_C = dis.readUnsignedShort(); streampos += 2;
						int sample_G = dis.readUnsignedShort(); streampos += 2;
						int sample_T = dis.readUnsignedShort(); streampos += 2;
						trace.addSample(sample_A, sample_C, sample_G, sample_T);
					}
				}
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
		return true;
	}

	protected boolean readBases(DataInputStream dis, Trace trace) {

		try {

			dis.skipBytes(bases_offset - streampos);
			streampos = bases_offset;

			int peak_index;
			int prob_a;
			int prob_c;
			int prob_g;
			int prob_t;
			char base;
			CalledBase scfbase;

			for (int baseIndex = 0; baseIndex < bases; baseIndex++)  {
				peak_index = dis.readInt(); streampos += 4;
				prob_a = dis.readUnsignedByte(); streampos += 1;
				prob_c = dis.readUnsignedByte(); streampos += 1;
				prob_g = dis.readUnsignedByte(); streampos += 1;
				prob_t = dis.readUnsignedByte(); streampos += 1;
				base = (char) dis.readByte(); streampos += 1;

				if (debug)  {
					System.out.println("Char " + base + "  num = " +
							baseIndex + "  coord = " + peak_index );
				}

				dis.skip(3); streampos += 3;
				scfbase =
					new CalledBase(peak_index, prob_a, prob_c, prob_g, prob_t, base);
				trace.addBase(scfbase);
			}
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		return true;
	}


}
