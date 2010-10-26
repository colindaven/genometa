/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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

import java.io.*;
import java.util.*;
import java.util.regex.*;
import java.text.*;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;

/**
 *  Parses a fasta-formatted file.
 *  The default parse() method only loads the first sequence in a fasta file.
 *  If there are multiple sequences in the file, ignores the rest.
 *  The parseAll() method will load all sequences listed in the file.
 */
public final class FastaParser {
	private static final Pattern header_regex = Pattern.compile("^\\s*>(.+)");
	public static final int LINELENGTH=79;
	private static final boolean DEBUG=false;


	/**
	 * Parses an input stream which can contain one or more sequences in FASTA format.
	 * Will merge the sequences with the given group.
	 * (When necessary, new sequences will be added to the existing group; otherwise
	 * sequence data will be stored in the existing, synonymous BioSeq objects.)
	 * Returns the List of sequences that were read from the file, which will be
	 * a subset of the sequences in the group.
	 */
	public static List<BioSeq> parseAll(InputStream istr, AnnotatedSeqGroup group) throws IOException {
		List<BioSeq> seqlist = new ArrayList<BioSeq>();
		BufferedReader br = null;
		Matcher matcher = header_regex.matcher("");
		try {
			br = new BufferedReader(new InputStreamReader(istr));
			String header = br.readLine();
			while (br.ready() && (!Thread.currentThread().isInterrupted())) {  // loop through lines till find a header line
				if (header == null) {
					continue;
				}  // skip null lines
				matcher.reset(header);
				boolean matched = matcher.matches();

				if (!matched) {
					continue;
				}
				String seqid = matcher.group(1);

				StringBuffer buf = new StringBuffer();
				while (br.ready() && (!Thread.currentThread().isInterrupted())) {
					String line = br.readLine();
					if (line == null || line.length() == 0) {
						continue;
					}  // skip null and empty lines

					if (line.charAt(0) == ';') {
						continue;
					} // skip comment lines

					// break if hit header for another sequence --
					if (line.startsWith(">")) {
						header = line;
						break;
					}

					buf.append(line);
				}

				// Didn't use .toString() here because of a memory bug in Java
				// (See "stringbuffer memory java" for more details.)
				String residues = new String(buf);
				buf.setLength(0);
				buf = null; // immediately allow the gc to use this memory
				residues = residues.trim();

				BioSeq seq = group.getSeq(seqid);
				if (seq == null && seqid.indexOf(' ') > 0) {
					// It's possible that the header has additional info past the chromosome name.  If so, remove and try again.
					String name = seqid.substring(0, seqid.indexOf(' '));
					seq = group.getSeq(name);
				}
				if (seq == null) {
					seq = group.addSeq(seqid, residues.length());
				}
				seq.setResidues(residues);

				seqlist.add(seq);
				if (DEBUG) {
				System.out.println("length of sequence: " + residues.length());
				}
			}
		} finally {
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(istr);
		}
		return seqlist;
	}


	public static BioSeq parseSingle(InputStream istr, AnnotatedSeqGroup group) throws IOException {
		List <BioSeq> bioList = parseAll(istr,group);
		if (bioList == null)
			return null;
		return bioList.get(0);
	}

	// Basically the same as parseAll, except that it returns the residues and doesn't change the AnnotatedSeqGroup
	public static String parseResidues(InputStream istr) throws IOException {
		BufferedReader br = null;
		Matcher matcher = header_regex.matcher("");
		String result = null;
		try {
			br = new BufferedReader(new InputStreamReader(istr));
			String header = br.readLine();
			while (br.ready()) {  // loop through lines till find a header line
				if (header == null) {
					continue;
				}  // skip null lines
				matcher.reset(header);
				boolean matched = matcher.matches();

				if (!matched) {
					continue;
				}
				StringBuffer buf = new StringBuffer();
				while (br.ready()) {
					String line = br.readLine();
					if (line == null || line.length() == 0) {
						continue;
					}  // skip null and empty lines

					if (line.charAt(0) == ';') {
						continue;
					} // skip comment lines

					// break if hit header for another sequence --
					if (line.startsWith(">")) {
						header = line;
						break;
					}

					buf.append(line);
				}

				// Didn't use .toString() here because of a memory bug in Java
				// (See "stringbuffer memory java" for more details.)
				result = new String(buf);
				buf.setLength(0);
				buf = null; // immediately allow the gc to use this memory

				result = result.trim();
			}
		} finally {
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(istr);
			
		}
		return result;
	}

	/**
	 *  Parse an input stream, creating a single new BioSeq.
	 *  @param istr an InputStream that will be read and then closed
	 */
	public static BioSeq parse(InputStream istr) throws IOException {
		return FastaParser.parse(istr, null, -1);
	}

	// to help eliminate memory spike (by dynamic reallocation of memory in StringBuffer -- don't ask...)
	// give upper limit to sequence length, based on file size -- this will be an overestimate (due to
	//   white space, name header, etc.), but probably no more than 10% greater than actual size, which
	//   is a lot better than aforementioned memory spike, which can temporarily double the amount of
	//   memory needed
	public static BioSeq parse(InputStream istr, BioSeq aseq,
			int max_seq_length) {
		return FastaParser.oldparse(istr, aseq, max_seq_length);
	}

	/**
	 *  Old parsing method.
	 *  trying to optimize for case where number of residues is known, so can
	 *  pre-allocate length of StringBuffer's internal char array, and then use
	 *  the StringBuffer.toString() method to get residues without accidentally
	 *  caching an array bigger than needed (see comments in method for more details...)
	 *  @param istr an InputStream that will be read and then closed
	 *  @param aseq Usually null, but can be an existing seq that you want to load the
	 *   residues into.  If not null, then the sequence in the file must have a name
	 *   that is synonymous with aseq.
	 */
	private static BioSeq oldparse(InputStream istr, BioSeq aseq,
			int max_seq_length) {
		boolean use_buffer_directly = false;
		boolean fixed_length_buffer = false;
		if (max_seq_length > 0) {
			fixed_length_buffer = true;
			use_buffer_directly = true;
		}
		else {
			fixed_length_buffer = false;
			use_buffer_directly = false;
		}
		System.out.println("using buffer directly: " + use_buffer_directly);
		System.out.println("using fixed length buffer: " + fixed_length_buffer);

		com.affymetrix.genometryImpl.util.Timer tim = new com.affymetrix.genometryImpl.util.Timer();
		tim.start();
		BioSeq seq = aseq;
		String seqid = "unknown";
		// maybe guesstimate size of buffer needed based on file size???
		StringBuffer buf;
		if (fixed_length_buffer) {
			buf = new StringBuffer(max_seq_length);
		}
		else {
			buf = new StringBuffer();
		}

		Matcher matcher = header_regex.matcher("");
		int line_count = 0;
		BufferedReader br = null;
		try {
			//      System.out.println("trying to read");
			br = new BufferedReader(new InputStreamReader(istr));
			while (br.ready() && (!Thread.currentThread().isInterrupted())) {  // loop through lines till find a header line
				String header = br.readLine();
				if (header == null) { continue; }  // skip null lines
				matcher.reset(header);
				boolean matched = matcher.matches();
				if (matched) {
					seqid = matcher.group(1);
					break;
				}
			}
			while (br.ready() && (!Thread.currentThread().isInterrupted())) {
				String line = br.readLine();
				if (line == null || line.length()==0) { continue; }  // skip null and empty lines

				if (line.startsWith(";")) { continue; } // lines beginning with ";" are comments
				// see http://en.wikipedia.org/wiki/Fasta_format

				// end loop if hit header for another sequence --
				//   currently only parsing first sequence in fasta file
				if (line.startsWith(">")) {
					break;
				}

				buf.append(line);
				line_count++;
			}

		}
		catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(istr);
		}

		// GAH 6-26-2002
		// contortions to try and minimize length of String's internal char array, because:
		//   if just do res = buf.toString(), or res = new String(buf),
		//     then String will point to StringBuffer's
		//     current internal array, which may be up to twice as big as actually needed to
		//     hold residues (see String(StringBuffer) constructor, and how StringBuffer
		//     stretches its internal array in StringBuffer.expandCapacity()).  And even if
		//     StringBuffer changes later, it's the StringBuffer that creates a new array -- String
		//     still points to the old array
		//   this does take more time (and potentially more peak memory), since have to first
		//     copy buffer's characters to char array, and then the String(char[]) constructor will
		//     copy the charray's characters to its own internal array.  BUT, will decrease
		//     long-term memory usage
		String residues = null;
		if (use_buffer_directly) {
			residues = new String(buf);
		}
		else {
			// trying new strategy with String(String) constructor
			// looks weird, but not as convoluted as other technique
			//  use String(String) constructor, whose side effect is to trim new String's
			//  internal char array -- then can garbage collect temp_residues later,
			//  and hopefully will save space...
			String temp_residues = new String(buf);
			residues = new String(temp_residues);
			temp_residues = null;
		
			System.out.println("done constructing residues via array");
			buf = null;
		}
		
		System.out.println("id: " + seqid);
		if (seq == null) {
			seq = new BioSeq(seqid, seqid, residues.length());
			seq.setResidues(residues);
		}
		else {  // try to merge with existing seq
			if (SynonymLookup.getDefaultLookup().isSynonym(seq.getID(), seqid)) {
				seq.setResidues(residues);
			}
			else {
				System.out.println("*****  ABORTING MERGE, sequence ids don't match: " +
						"old seq id = " + seq.getID() + ", new seq id = " + seqid);
			}
		}
		System.out.println("time to execute: " + tim.read()/1000f);
		System.out.println("done loading fasta file");
		System.out.println("length of sequence: " + residues.length());
		return seq;
	}

	

	/**Read FASTA sequence from specified file for DAS/2 serving.
	 * We assume a header of less than 500 characters, terminated by a newline.
	 * We assume exactly one sequence in the FASTA file.
	 * We assume no comment lines.
	 * We assume exactly LINELENGTH nucleotides per line (until the last line), with a carriage return following each line.
	 * Sequence range is specified in interbase format. (See http://www.biodas.org/documents/das2/das2_get.html.)
	 *
	 * @param seqfile
	 * @param begin_sequence
	 * @param end_sequence
	 * @return FASTA sequence byte[]
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 * @throws java.lang.IllegalArgumentException
	 */
	public static byte[] readFASTA(File seqfile, int begin_sequence, int end_sequence)
			throws FileNotFoundException, IOException, IllegalArgumentException {

			if (begin_sequence < 0)
				throw new java.lang.IllegalArgumentException("beginning sequence:" + begin_sequence + " was negative.");
			if (end_sequence < begin_sequence)
				throw new java.lang.IllegalArgumentException("range " + begin_sequence + ":" + end_sequence + " was negative.");

			if (!seqfile.exists())
				throw new java.io.FileNotFoundException("Couldn't find file " + seqfile.toString());

			if (begin_sequence > seqfile.length())
				throw new java.lang.IllegalArgumentException("beginning sequence:" + begin_sequence + " larger than file size:" + (int)seqfile.length());

			// Sanity check on huge range... can't be larger than the overall file size.
			if (seqfile.length() <= (long)Integer.MAX_VALUE) {
				end_sequence = Math.min(end_sequence, (int)seqfile.length());
			}

			if (begin_sequence == end_sequence) {
				return null;
			}

			byte[] buf = null;
			DataInputStream dis = new DataInputStream(new FileInputStream(seqfile));
			BufferedInputStream bis = new BufferedInputStream(dis);

			try {
				// Skip to the location past the header.
				byte[] header = skipFASTAHeader(seqfile.getName(), bis);
				int header_len = (header == null ? 0 : header.length);


				bis.reset();
				long skip_status = BlockUntilSkipped(bis, header_len);
				if (skip_status != header_len) {
					System.out.println("skipped header past EOF");
					return buf;
				}
				// begin_sequence of 0 is first nucleotide.

				// Skip to location of begin_sequence.  Since there are (LINELENGTH + 1) characters per line, that should be:
				// floor(begin_sequence / LINELENGTH) * (LINELENGTH+1) + begin_sequence % LINELENGTH.
				int full_lines_to_skip = (begin_sequence / LINELENGTH);
				int chars_to_skip = (LINELENGTH+1) * full_lines_to_skip;
				int line_location = begin_sequence % LINELENGTH;

				// skip all the full lines
				skip_status = BlockUntilSkipped(bis, chars_to_skip);
				if (skip_status != chars_to_skip) {
					System.out.println("skipped lines past EOF");
					return buf;
				}

				// skip the additional nucleotides in the line
				skip_status = BlockUntilSkipped(bis, line_location);
				if (skip_status != line_location) {
					System.out.println(line_location + "," + skip_status + ": skipped nucleotides past EOF");
					return buf;
				}

				int nucleotides_len = end_sequence - begin_sequence;
				buf = new byte[nucleotides_len];

				for (int i=0;i<nucleotides_len;) {
					if (line_location == LINELENGTH) {
						// skipping the newline
						byte[] x = new byte[1];
						int nucleotides_read = bis.read(x,0,1);
						if (nucleotides_read < 1) {
							// end of file hit.  quit parsing.
							System.out.println("Unexpected End of File at newline!");
							return trimBuffer(buf);
						}
						if (nucleotides_read == 1 && x[0] == '\n') {
							// expected
							line_location=0;
							continue;
						}

						// What is this character doing here?
						throw new java.lang.AssertionError("Unexpected char at end of line: " + (char)x[0] + "\nPlease verify that the FASTA file satisfies DAS/2 format assumptions.");
					}

					// Read several characters if possible
					int nucleotides_left_on_this_line = Math.min(LINELENGTH - line_location, nucleotides_len - i);
					int nucleotides_read = bis.read(buf, i, nucleotides_left_on_this_line);
					if (nucleotides_read == -1)
						return trimBuffer(buf);
					i+= nucleotides_read;
					line_location += nucleotides_read;

					if (nucleotides_read != nucleotides_left_on_this_line) {
						// end of file hit.  quit parsing.
						System.out.println("Unexpected EOF: i,nucleotides_read" + i + " " + nucleotides_read);

						return trimBuffer(buf);
					}
				}

				return trimBuffer(buf);
			}
			finally {
				GeneralUtils.safeClose(bis);
				GeneralUtils.safeClose(dis);
			}
		}


		// Generate header of form:
		// >[seqname] range:[start]-[end] interbase genome:[genome]
		// e.g.,
		// >ChrC range:0-1000 interbase genome:A_thaliana_TAIR8
		public static byte[] generateNewHeader(String chrom_name, String genome_name, int start, int end) {
			String header = 
				">" +
				chrom_name + 
				" range:" + 
				NumberFormat.getIntegerInstance().format(start) +
				"-" +
				NumberFormat.getIntegerInstance().format(end) +
				" interbase genome:" +
				genome_name +
				"\n";

			byte[] result = new byte[header.length()];

			for (int i=0;i<header.length();i++)
				result[i] = (byte)header.charAt(i);

			return result;
		}


		private static byte[] trimBuffer(byte[] buf) {
			int i=buf.length;

			// Find last valid residue
			while (i>=0 && (buf[i-1] == '\n' || buf[i-1] == 0)) {
				i--;
			}
			if (i == 0)
				return null;

			// Copy over shortened buffer

			byte[] buf2 = new byte[i];
			System.arraycopy(buf, 0, buf2, 0, i);
			buf = null;

			return buf2;
		}

		// Parse off the header (if it exists).
		public static byte[] skipFASTAHeader(String filename, BufferedInputStream bis)
			throws IOException, UnsupportedEncodingException {
			// The header is less than 500 bytes, and if it exists, the header begins with a ">" and ends with a newline.
			byte[] header = new byte[500];

			bis.mark(500);
			
			int bytes_to_read = header.length;
			int begin = 0;
			while(bytes_to_read > 0){
				int bytesRead = bis.read(header,begin,bytes_to_read);

				if(bytesRead < 0)
					break;

				begin = bytesRead;
				bytes_to_read -= bytesRead;
			}
			if (header[0] == '>') {
				// We found a header
				// Parse until we're done with header.
				for (int i=1;i<500;i++) {
					if (header[i] == '\n') {
						byte[] header2 = new byte[i+1];
						System.arraycopy(header, 0, header2, 0, i+1);
						return header2;
					}
				}

				// There was no newline?
				throw new java.io.UnsupportedEncodingException("file " + filename + " header does not match expected FASTA format.");
			} 
			//System.out.println("Didn't start with >");
			// no header.
			return null;
		}

			// Turns out you can't trust Java to skip to a location in a file.
			// (See http://java.sun.com/j2se/1.5.0/docs/api/java/io/InputStream.html#skip(long) .)
			// This wrapper method will block until the appropriate bytes are skipped.
			private static long BlockUntilSkipped(BufferedInputStream bis, int line_location) throws IOException {
				long skip_status = 0;
				for (skip_status = bis.skip(line_location); skip_status < line_location && bis.available() > 0; skip_status += bis.skip(line_location - skip_status)) {
					//System.out.println("total skip:" + skip_status);
				}
				return skip_status;
			}


			public static String getMimeType()  { return "text/fasta"; } 
		}