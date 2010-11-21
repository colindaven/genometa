/**
 *   Copyright (c) 2001-2006 Affymetrix, Inc.
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


import com.affymetrix.genometryImpl.util.Timer;
import com.affymetrix.genometryImpl.util.NibbleIterator;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SeekableBufferedStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.util.SeekableStream;

public final class NibbleResiduesParser {
	private static int BUFSIZE = 65536;	// buffer for outputting

	/**
	 *  Parses an input stream.
	 *  The sequence will get added to the existing NibbleBioSeq found by
	 *  seq_group.getSeq(name), where the name comes from data in the file.
	 *  If such a NibbleBioSeq doesn't exist, it will be created, but if a
	 *  BioSeq does exist that is not of the type NibbleBioSeq, an exception
	 *  will be thrown.
	 */
	public static BioSeq parse(InputStream istr, AnnotatedSeqGroup seq_group) throws IOException {
		return parse(istr,seq_group,0,Integer.MAX_VALUE);
	}

	//Returns bnib format
	public static BioSeq parse(InputStream istr, AnnotatedSeqGroup seq_group, int start, int end) throws IOException
	{
		BioSeq result_seq = null;
		InputStream bis = null;
		DataInputStream dis = null;
		try {
			Timer tim = new Timer();
			tim.start();

			//If istr is instance of Seekablestream then use SeekableBufferedStream else use BufferedInputStream.
			if (istr instanceof SeekableStream) {
				bis = new SeekableBufferedStream((SeekableStream) istr);
			} else if (istr instanceof BufferedInputStream) {
				bis = (BufferedInputStream) istr;
			} else {
				bis = new BufferedInputStream(istr);
			}
			dis = new DataInputStream(bis);

			
			String name = dis.readUTF();
			dis.readUTF();
			int total_residues = dis.readInt();

			if(start < end)
			{
				start = Math.max(0, start);
				start = Math.min(total_residues, start);

				end = Math.max(0, end);
				end = Math.min(total_residues, end);
			}
			else
			{
				start = 0;
				end = 0;
			}

			int num_residues = end - start;
			num_residues = Math.max(0, num_residues);

			BioSeq existing_seq = seq_group.getSeq(name);
			if (existing_seq != null) {
				result_seq = existing_seq;
			} else {
				result_seq = seq_group.addSeq(name, num_residues);
			}

			int bytes_to_skip = start / 2;
			while (bytes_to_skip > 0) {
				int skipped = (int) bis.skip(bytes_to_skip);
				if (skipped < 0) {
					break;
				} // EOF reached
				bytes_to_skip -= skipped;
			}

			Logger.getLogger(NibbleResiduesParser.class.getName()).log(
					Level.INFO, "Chromosome: {0} : residues: {1}", new Object[]{result_seq, num_residues});
			SetResiduesIterator(start, end, dis, result_seq);
		}
		finally {
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(bis);
		}
		return result_seq;
	}

	
	//Returns raw format
	public static boolean parse(InputStream istr, OutputStream output) throws FileNotFoundException, IOException
	{
		return parse(istr,new AnnotatedSeqGroup("No_Data"), output);
	}

	//Returns raw format
	public static boolean parse(InputStream istr, AnnotatedSeqGroup seq_group, OutputStream output) throws IOException
	{
		return parse(istr,seq_group, 0, Integer.MAX_VALUE, output);
	}

	//Returns raw format
	public static boolean parse(InputStream istr, int start, int end, OutputStream output) throws FileNotFoundException, IOException
	{
		return parse(istr,new AnnotatedSeqGroup("No_Data"), start, end, output);
	}
	
	//Returns raw format
	public static boolean parse(InputStream istr, AnnotatedSeqGroup seq_group, int start, int end, OutputStream output) throws IOException
	{
		BioSeq seq = parse(istr,seq_group,start,end);
		return writeAnnotations(seq,start,end,output);
	}

	/**
	 * Determine the chromosome referenced by the BNIB stream.
	 * @param istr
	 * @param seq_group
	 * @return
	 */
	public static BioSeq determineChromosome(InputStream istr, AnnotatedSeqGroup seq_group) {
		BioSeq result_seq = null;
		BufferedInputStream bis = null;
		DataInputStream dis = null;
		try {
			if (istr instanceof BufferedInputStream) {
				bis = (BufferedInputStream) istr;
			} else {
				bis = new BufferedInputStream(istr);
			}
			dis = new DataInputStream(bis);
			String name = dis.readUTF();
			dis.readUTF();
			int total_residues = dis.readInt();
			if (seq_group.getSeq(name) != null) {
				return seq_group.getSeq(name);
			}
			result_seq = new BioSeq(name, seq_group.getID(), total_residues);
		} catch (Exception ex) {
			Logger.getLogger(NibbleResiduesParser.class.getName()).log(Level.SEVERE, null, ex);
		}
		return result_seq;
	}

	public static byte[] readBNIB(File seqfile) throws FileNotFoundException, IOException {
		DataInputStream dis = null;
		try {
			dis = new DataInputStream(new FileInputStream(seqfile));
			int filesize = (int) seqfile.length();
			byte[] buf = new byte[filesize];
			dis.readFully(buf);
			return buf;
		}
		finally {
			GeneralUtils.safeClose(dis);
		}
	}

	private static void SetResiduesIterator(int start, int end, DataInputStream dis, BioSeq result_seq) throws IOException {

		int num_residues = end - start;
		int extra = Math.max(end%2, num_residues%2);
		byte[] nibble_array = new byte[num_residues / 2 + extra];
		int first = start%2;
		int last = first + num_residues;

		dis.readFully(nibble_array);	// this only reads nibble_array.len bytes, per spec.

		if (result_seq.getMin() < start || result_seq.getMax() > end) {
			// We're only asking for a partial result -- used on the server.
			// if we're skipping an even number of residues, there's no problem.  We've already skipped the first and last byte segments.
			// However, if we're skipping an odd number of residues, then we have to re-shift every residue 4 bits.
			// We do this in a naive way -- convert the nibbles to a string, then convert back from the string.
			// This potentially can use a lot of memory, if a large, odd-start, query is made.
			if (first == 1) {
				String temp = NibbleIterator.nibblesToString(nibble_array, first, last);
				nibble_array = NibbleIterator.stringToNibbles(temp, 0, temp.length());
			}
		}
		NibbleIterator residues_provider = new NibbleIterator(nibble_array, num_residues);
		result_seq.setResiduesProvider(residues_provider);
	}

	public static void writeBinaryFile(String file_name, String seqname, String seqversion,
			String residues) throws IOException {

		// Note: We need to support case because many groups, including
		// UCSC use case to indicate when a base is in a repeat region
		//   AL - 10/1/08
		//
		// binary DNA residues format
		// header:
		//   UTF8-encoded sequence name
		//   UTF8-encoded sequence version
		//   length of sequence as 4-byte int
		//   residues encoded as "nibbles" (4 bits per residue ==> 2 bases / byte)
		//      4-bit residue encoding maps to 16 possible IUPAC codes:
		//     [ A, C, G, T, N, M, R, W, S, Y, K, V, H, D, B, X ]

		FileOutputStream fos = null;
		DataOutputStream dos = null;
		BufferedOutputStream bos = null;

		try {
			File fil = new File(file_name);
			fos = new FileOutputStream(fil);
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
			dos.writeUTF(seqname);
			dos.writeUTF(seqversion);
			dos.writeInt(residues.length());
			System.out.println("creating nibble array");
			byte[] nibble_array = NibbleIterator.stringToNibbles(residues, 0, residues.length());

			System.out.println("done creating nibble array, now writing nibble array out");
			// hit problem with trying to output full nibble_array at once for large
			// (>100 Mb) sequences.  I would have thought the BufferedOutputStream would
			// handle this, but maybe it just passes through calls that write large amounts...
			//
			// anyway, trying to work around this by looping over the array and writing
			// smaller chunks
			int chunk_length = 65536;
			if (nibble_array.length > chunk_length) {
				int bytepos = 0;
				while (bytepos < (nibble_array.length - chunk_length)) {
					dos.write(nibble_array, bytepos, chunk_length);
					bytepos += chunk_length;
				}
				dos.write(nibble_array, bytepos, nibble_array.length - bytepos);
			}
			else {
				dos.write(nibble_array);
			}
			System.out.println("done writing out nibble file");
		}
		finally {
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(bos);
			GeneralUtils.safeClose(fos);
		}
	}

	/**
	 * Write raw characters out to stream.
	 * @param seq	- chromosome we're currently on
	 * @param start - start of raw sequence
	 * @param end - end of raw sequence
	 * @param outstream - stream we're writing to
	 * @return - success or failure
	 */
	private static boolean writeAnnotations(BioSeq seq, int start, int end, OutputStream outstream)
	{
		if (seq.getResiduesProvider() == null) {
			return false;
		}
		// sanity checks
		start = Math.max(0, start);
		end = Math.max(end, start);
		end = Math.min(end, start+seq.getResiduesProvider().getLength());

		DataOutputStream dos = null;
		try
		{
			dos = new DataOutputStream(new BufferedOutputStream(outstream));

			// Only keep BUFSIZE characters in memory at one time
			for (int i=0;i<(end-start) && (!Thread.currentThread().isInterrupted());i+=BUFSIZE) {
				String outString = seq.getResidues(i, Math.min(i+BUFSIZE, (end-start)));
				dos.writeBytes(outString);
			}
			dos.flush();
			return true;
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
	}

	public static String getMimeType() {
		return "binary/bnib";
	}
	
	private static void writeFastaToFile(File bnibFile){
		InputStream in = null;
		FileOutputStream fos = null;
		try {
			in = new FileInputStream(bnibFile);
			BioSeq seq = NibbleResiduesParser.parse(in, new AnnotatedSeqGroup(bnibFile.toString()));
			String name = bnibFile.getName().replace(".bnib", ".fasta");
			File outFile = new File (bnibFile.getParentFile(), name);

			// We avoid string manipulation here; important for large chromosomes
			fos = new FileOutputStream(outFile);
			fos.write('>');
			fos.write(seq.getID().getBytes());
			fos.write('\n');
			NibbleResiduesParser.writeAnnotations(seq, 0, seq.getLength(), fos);
			fos.write('\n');
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			GeneralUtils.safeClose(fos);
			GeneralUtils.safeClose(in);
		}
	}

	/** Reads a FASTA file and outputs a binary sequence file.
	 *  @param args  sequence name, input file, output file, sequence version
	 */
	public static void main(String[] args) { 
		try {
			String infile_name = null;
			String outfile_name = null;
			String seq_version = null;
			String seq_name = null;
			if (args.length == 1) {
				writeFastaToFile(new File(args[0]));
				return;
			}
			if (args.length >= 4) {
				seq_name = args[0];
				infile_name = args[1];
				outfile_name = args[2];
				seq_version = args[3];
			}
			else {
				System.err.println("Usage: java -cp <exe_filename> com.affymetrix.genometryImpl.parsers.NibbleResiduesParser [seq_name] [in_file] [out_file] [seq_version]. Alternatively, provide just a xxx.bnib file to convert it to xxx.fasta");
				System.exit(1);
			}
			File fil = new File(infile_name);
			StringBuffer sb = new StringBuffer();
			InputStream isr = GeneralUtils.getInputStream(fil, sb);

			// if file is gzipped or zipped, then fil.length() will not really be the max_seq_length,
			//   but that's okay because the parse() method only uses it as a suggestion
			BioSeq seq = FastaParser.parse(isr, null, (int)(fil.length()));
			int seqlength = seq.getResidues().length();
			System.out.println("length: " + seqlength);

			writeBinaryFile(outfile_name, seq_name, seq_version, seq.getResidues());
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}

}

