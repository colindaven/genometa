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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * parses ABI format data.
 * It will load the first trace in the ABI file into a {@link Trace}
 * stripping white space.
 *
 * @author Cyrus Harmon
 * @author Eric Blossom
 */
public class ABITraceParser implements ContentParser {

	protected List<ABIIndex> ABIindices;
	protected int max_numbyt;
	protected ABIIndex trace1_index;
	protected ABIIndex trace2_index;
	protected ABIIndex trace3_index;
	protected ABIIndex trace4_index;
	protected ABIIndex basemap_index;
	protected ABIIndex bases_index;
	protected ABIIndex baseposition_index;
	protected ABIIndex signal_index;
	protected ABIIndex spacing_index;
	protected ABIIndex pri_index;
	protected ABIIndex machine_index;
	protected ABIIndex dyeprimer_index;
	protected ABIIndex sample_index;
	protected ABIIndex thumbprint_index;

	protected List<Integer> A_list;
	protected List<Integer> C_list;
	protected List<Integer> G_vector;
	protected List<Integer> T_vector;

	protected int A_strength;
	protected int T_strength;
	protected int G_strength;
	protected int C_strength;

	protected int max_trace_value;

	protected int header_len = 0;
	protected int curpos = 0;

	/**
	 * the data model.
	 */
	protected Trace trace = new Trace();

	/**
	 * imports the data from a stream of bytes.
	 * This is defined as returning an Object
	 * to conform to the ContentParser interface.
	 * However, it actually returns a Trace.
	 * To use it as such
	 * you will need to cast the returned Object back to a Trace.
	 *
	 * @param theInput from whence the data come.
	 * @return a representation of the trace.
	 * @see com.affymetrix.genoviz.datamodel.Trace
	 * @see ContentParser
	 */
	public Object importContent( InputStream theInput ) throws IOException {

		ABIindices = new ArrayList<ABIIndex>(14);

		initTraceIndices();
		max_trace_value = 0;

		int bufsize = 65536;
		byte[] b = new byte[bufsize];
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		int len = 0;
		int totlen = 0;
		while (true) {
			len = theInput.read(b,0,bufsize);
			if (len > 0) {
				out.write(b, 0, len);
				totlen += len;
			}
			else {
				break;
			}
		}

		ByteArrayInputStream bis =
			new ByteArrayInputStream(out.toByteArray());
		DataInputStream dis = new DataInputStream (bis);

		readHeader(dis);
		readSamples(dis);
		readBases(dis);

		return this.trace;
	}

	/**
	 * not yet implemented.
	 */
	public void exportContent( OutputStream theOutput, Object o )
		throws IOException {
	}

	private void initTraceIndices () {
		trace1_index = new ABIIndex ("DATA", 9);
		ABIindices.add (trace1_index);
		trace2_index = new ABIIndex ("DATA", 10);
		ABIindices.add (trace2_index);
		trace3_index = new ABIIndex ("DATA", 11);
		ABIindices.add (trace3_index);
		trace4_index = new ABIIndex ("DATA", 12);
		ABIindices.add (trace4_index);
		basemap_index = new ABIIndex ("FWO_", 1);
		ABIindices.add (basemap_index);
		bases_index = new ABIIndex ("PBAS", 1);
		ABIindices.add (bases_index);
		baseposition_index = new ABIIndex ("PLOC", 1);
		ABIindices.add (baseposition_index);
		signal_index = new ABIIndex ("S/N%", 1);
		ABIindices.add (signal_index);
		spacing_index = new ABIIndex ("SPAC", 1);
		ABIindices.add (spacing_index);
		pri_index = new ABIIndex ("PPOS", 1);
		ABIindices.add (pri_index);
		machine_index = new ABIIndex ("MCHN", 1);
		ABIindices.add (machine_index);
		dyeprimer_index = new ABIIndex ("PDMF", 1);
		ABIindices.add (dyeprimer_index);
		sample_index = new ABIIndex ("SMPL", 1);
		ABIindices.add (sample_index);
		thumbprint_index =  new ABIIndex ("THUM", 1);
		ABIindices.add (thumbprint_index);
	}

	private boolean readHeader (DataInputStream in) throws IOException {

		if (!verifyFileID (in)) {
			System.err.println ("doesn't seem to be a valid ABI trace file");
			return false;
		}

		// Read index block size
		int index_block_size = readIndexSize (in);

		// Read the size (in number of entries) of the index.
		int index_size = readABIint( in, 18 );

		// Read block index offset
		int index_block_offset = readABIint( in, 26 );

		int index_offset = index_block_offset;
		String label;
		int serial_number;
		int index_count, index_number;
		ABIIndex index;
		int datType;
		boolean index_found;

		// Don't skip bytes! Read them into a buffer
		index_count = 0;
		int index_entries_read = 0;
		while ( index_count < ABIindices.size() && index_entries_read++ < index_size ) {

			in.skip (index_offset - curpos);
			curpos += index_offset - curpos;

			index_offset += index_block_size;

			label = readABIlabel (in, "label");

			serial_number = readABIint (in, "serial_number");

			index_number = 0;
			index_found = false;

			index = ABIindices.get (index_number);
			while ( !index_found && index_number < ABIindices.size() ) {
				if (serial_number == index.serial_number
						&& (label.equals (index.label))
						&& !index.occur) {
					index_count++;
					index_found = true;
					index.occur = true;
					datType = readABIshort (in, "datType");
					index.sizwrd = readABIshort (in, "index.sizwrd");
					index.numwrd = readABIint (in, "index.numwrd");
					index.numbyt = readABIint (in, "index.numbyt");
					if (index.numbyt <= 4 && datType != 18) {
						for (int j = 0; j < index.numbyt; j++) {
							index.offset = ((index.offset << 8)
									| in.readUnsignedByte ());
							curpos++;
						}
					}
					else {
						index.offset = readABIint (in, "index.offset");
					}
					if (index.numbyt > max_numbyt) {
						max_numbyt = index.numbyt;
					}
						}
				else {
					index_number++;
					if ( index_number < ABIindices.size() ) {
						index = ABIindices.get (index_number);
					}
				}
			}
		}

		return true;
	}

	private int readIndexSize (DataInputStream in) throws IOException {
		int index_block_size = 0;

		// Read block index size
		in.skip (12);
		curpos += 12;
		index_block_size = readABIshort (in, "index_block_size");

		return index_block_size;
	}

	private int readABIshort (DataInputStream in, String msg) throws IOException {
		int short_value = 0;

		short_value = in.readUnsignedShort ();
		curpos += 2;

		return short_value;
	}


	/**
	 * read an int from a specified stream.
	 *
	 * @param in stream to read from.
	 * @param msg unused.
	 */
	private int readABIint (DataInputStream in, String msg) throws IOException {
		int int_value = 0;

		int_value = (in.readUnsignedByte () & 0xff);
		int_value = ((int_value << 8) | (in.readUnsignedByte () & 0xff));
		int_value = ((int_value << 8) | (in.readUnsignedByte () & 0xff));
		int_value = ((int_value << 8) | (in.readUnsignedByte () & 0xff));
		curpos += 4;

		return int_value;
	}

	/**
	 * read an int from a specified stream at a specified offset.
	 *
	 * @param in stream from which to read.
	 * @param theOffset in the stream.
	 */
	private int readABIint( DataInputStream in, int theOffset ) throws IOException {
		if ( this.curpos < theOffset ) {
			in.skip( theOffset - curpos );
			this.curpos = theOffset;
		}
		if ( this.curpos == theOffset ) {
			return readABIint( in, "offset_read" );
		}
		throw new IOException( "Can't go backwards." );
	}

	private String readABIlabel (DataInputStream in, String msg) throws IOException {
		byte[] byte_value = new byte[4];
		String string_value = "";

		in.read(byte_value, 0, 4);
		curpos += 4;
		string_value = (new String (byte_value));

		return string_value;
	}

	/**
	 * verifies that the file is an ABI format file.
	 * Such a file starts with a signature of the characters "ABIF".
	 * On a Macintosh there may be a 128 byte header before the signature.
	 * This checks for the signature at byte 0 and (if needed) at byte 128.
	 * @return true iff the correct signature is found.
	 */
	private boolean verifyFileID (DataInputStream in) throws IOException {
		String read_id = "";

		read_id = readABIlabel (in, "read_id");
		if (read_id.equals ("ABIF")) {
			header_len = 0;
			return true;
		} else {
			in.skip (124);
			curpos = 0;
			header_len = 128;
			read_id = readABIlabel (in, "read_id");
			if (read_id.equals ("ABIF")) {
				return true;
			}
		}

		return false;
	}

	private boolean readSamples (DataInputStream in) throws IOException {
		boolean success = false;
		A_list = new ArrayList<Integer>();
		C_list = new ArrayList<Integer>();
		G_vector = new ArrayList<Integer>();
		T_vector = new ArrayList<Integer>();
		char trace1_base = (char) ((basemap_index.offset >> 24) & 0xff);
		char trace2_base = (char) ((basemap_index.offset >> 16) & 0xff);
		char trace3_base = (char) ((basemap_index.offset >> 8) & 0xff);
		char trace4_base = (char) ((basemap_index.offset) & 0xff);
		List<Integer> trace1_list;
		List<Integer> trace2_list;
		List<Integer> trace3_list;
		List<Integer> trace4_list;

		trace1_list = getBaseList( trace1_base );
		trace2_list = getBaseList( trace2_base );
		trace3_list = getBaseList( trace3_base );
		trace4_list = getBaseList( trace4_base );

		// Read signal strength
		readSignalStrengths (in,
				trace1_base, trace2_base,
				trace3_base, trace4_base);
		int trace1_strength = getBaseStrength ( trace1_base );
		int trace2_strength = getBaseStrength ( trace2_base );
		int trace3_strength = getBaseStrength ( trace3_base );
		int trace4_strength = getBaseStrength ( trace4_base );

		success = readTrace (in, trace1_index, trace1_list, trace1_strength);
		success &= readTrace (in, trace2_index, trace2_list, trace2_strength);
		success &= readTrace (in, trace3_index, trace3_list, trace3_strength);
		success &= readTrace (in, trace4_index, trace4_list, trace4_strength);

		if (success) {
			for (int i = 0;
					(i < A_list.size ()
					 && i < T_vector.size ()
					 && i < G_vector.size ()
					 && i < C_list.size ());
					i++) {
				trace.addSample(A_list.get(i).intValue(),
						C_list.get(i).intValue(),
						G_vector.get(i).intValue(),
						T_vector.get(i).intValue());
					}
		}

		return success;
	}

	private boolean readTrace (DataInputStream in,
			ABIIndex trace_index,
			List<Integer> trace_vector,
			int trace_signal) throws IOException {

		boolean success = false;
		int sample;

		if ( trace_index.occur ) {
			seek(in, trace_index.offset);

			for (int i = 0; i < trace_index.numwrd; i++) {
				sample = in.readUnsignedByte ();
				curpos ++;
				sample = (sample << 8) | in.readUnsignedByte ();
				curpos ++;
				if (sample > max_trace_value) {
					max_trace_value = sample;
				}
				trace_vector.add(sample);
			}
			success = true;
		}
		return success;
	}

	private List<Integer> getBaseList (char base) {
		List<Integer> correct_vector = null;

		if (base == 'A')
			correct_vector = A_list;
		else if (base == 'C')
			correct_vector = C_list;
		else if (base == 'G')
			correct_vector = G_vector;
		else if (base == 'T')
			correct_vector = T_vector;

		return correct_vector;
	}

	private void readSignalStrengths (DataInputStream in,
			char base1, char base2,
			char base3, char base4) throws IOException {
		int sig1_strength;
		int sig2_strength;
		int sig3_strength;
		int sig4_strength;

		seek(in, signal_index.offset);

		sig1_strength = readABIshort (in, "sig1_strength");
		sig2_strength = readABIshort (in, "sig2_strength");
		sig3_strength = readABIshort (in, "sig3_strength");
		sig4_strength = readABIshort (in, "sig4_strength");

		setBaseStrength (sig1_strength, base1);
		setBaseStrength (sig2_strength, base2);
		setBaseStrength (sig3_strength, base3);
		setBaseStrength (sig4_strength, base4);
	}

	private void setBaseStrength (int signal_strength, char base) {

		if (base == 'A')
			A_strength = signal_strength;
		else if (base == 'C')
			C_strength = signal_strength;
		else if (base == 'G')
			G_strength = signal_strength;
		else if (base == 'T')
			T_strength = signal_strength;
	}

	private int getBaseStrength (char base) {
		int correct_strength = 0;

		if (base == 'A')
			correct_strength = A_strength ;
		else if (base == 'C')
			correct_strength = C_strength ;
		else if (base == 'G')
			correct_strength = G_strength ;
		else if (base == 'T')
			correct_strength = T_strength ;

		return correct_strength;
	}

	private boolean readBases (DataInputStream in) throws IOException {
		boolean success = false;
		int base;
		Integer base_position;
		

		if ( null != bases_index && bases_index.occur ) {
			List<Integer> base_positions = new ArrayList<Integer>(baseposition_index.numwrd);
			seek(in, baseposition_index.offset);
			readBasePositions (in, base_positions);
			seek(in, bases_index.offset);

			for (int i = 0; i < bases_index.numbyt; i++) {
				base = in.readUnsignedByte ();
				curpos++;
				base_position = base_positions.get (i);
				CalledBase trace_base =
					new CalledBase(base_position.intValue(), 0, 0, 0, 0, (char) base);
				trace.addBase(trace_base);
			}

			success = true;
		}
		return success;
	}

	private void readBasePositions (DataInputStream in, List<Integer> base_positions)
		throws IOException {
		int byte1, byte2;
		int base_position = 0;

		for (int i = 0; i < baseposition_index.numwrd; i++) {
			byte1 = (in.readUnsignedByte ());
			curpos++;
			byte2 = in.readUnsignedByte ();
			curpos++;
			base_position = (byte1 << 8) | byte2;

			base_positions.add(base_position);
		}
	}

	private synchronized void seek (DataInputStream dis, int position)
		throws IOException {
		int delta = position - curpos;
		if (delta >= 0) {
			dis.skip(delta);
			curpos += delta;
		} else {
			dis.reset();
			dis.skip(header_len + position);
			curpos = position;
		}
	}

}
