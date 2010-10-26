package com.affymetrix.genometryImpl.parsers;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.UcscGeneSym;
import com.affymetrix.genometryImpl.SupportsCdsSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.comparator.SeqSymMinComparator;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.Timer;

/**
 *  Just like refFlat table format, except no geneName field (just name field).
 */
public final class BgnParser implements AnnotationWriter, IndexWriter {

	private static final boolean DEBUG = false;
	private static List<String> pref_list = new ArrayList<String>();

	static {
		pref_list.add("bgn");
	}
	private static final Pattern line_regex = Pattern.compile("\t");
	private static final Pattern emin_regex = Pattern.compile(",");
	private static final Pattern emax_regex = Pattern.compile(",");

	//static String default_annot_type = "genepred";
	//  static String default_annot_type = "refflat-test";
	//static String user_dir = System.getProperty("user.dir");
	// mod_chromInfo.txt is same as chromInfo.txt, except entries have been arranged so
	//   that all random, etc. bits are at bottom
	// .bin1:
	//         name UTF8
	//        chrom UTF8
	//       strand UTF8
	//      txStart int
	//        txEnd int
	//     cdsStart int
	//       cdsEnd int
	//    exoncount int
	//   exonStarts int[exoncount]
	//     exonEnds int[exoncount]
	//
	public List<SeqSymmetry> parse(DataInputStream dis, String annot_type, AnnotatedSeqGroup group) {
		try {
			return this.parse(dis, annot_type, group, false);
		} catch (IOException ex) {
			Logger.getLogger(BgnParser.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}


	/**
	 *
	 * @param istr
	 * @param annot_type
	 * @param seq_group
	 * @param annotate_seq
	 * @return List of seq symmetries
	 * @throws java.io.IOException
	 */
	public List<SeqSymmetry> parse(InputStream istr, String annot_type,
			AnnotatedSeqGroup seq_group, boolean annotate_seq) throws IOException {
		if (seq_group == null) {
			throw new IllegalArgumentException("BgnParser called with seq_group null.");
		}
		Timer tim = new Timer();
		tim.start();

		// annots is list of top-level parent syms (max 1 per seq in seq_group) that get
		//    added as annotations to the annotated BioSeqs -- their children
		//    are then actual transcript annotations
		List<SeqSymmetry> annots = new ArrayList<SeqSymmetry>();
		// results is list actual transcript annotations
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>(15000);
		// chrom2sym is temporary hash to put top-level parent syms in to map
		//     seq id to top-level symmetry, prior to adding these parent syms
		//     to the actual annotated seqs
		Map<String, SeqSymmetry> chrom2sym = new HashMap<String, SeqSymmetry>(); // maps chrom name to top-level symmetry

		int total_exon_count = 0;
		int count = 0;
		BufferedInputStream bis = null;
		DataInputStream dis = null;
		boolean reached_EOF = false;

		try {
			bis = new BufferedInputStream(istr);
			dis = new DataInputStream(bis);

			Thread thread = Thread.currentThread();
			while (!thread.isInterrupted()) {
				String name = dis.readUTF();
				String chrom_name = dis.readUTF();
				String strand = dis.readUTF();
				boolean forward = (strand.equals("+") || (strand.equals("++")));
				int tmin = dis.readInt();
				int tmax = dis.readInt();
				int cmin = dis.readInt();
				int cmax = dis.readInt();
				int ecount = dis.readInt();
				int[] emins = new int[ecount];
				int[] emaxs = new int[ecount];
				for (int i = 0; i < ecount; i++) {
					emins[i] = dis.readInt();
				}
				for (int i = 0; i < ecount; i++) {
					emaxs[i] = dis.readInt();
				}

				BioSeq chromseq = seq_group.getSeq(chrom_name);

				if (chromseq == null) {
					chromseq = seq_group.addSeq(chrom_name, 0);
				}

				UcscGeneSym sym = new UcscGeneSym(annot_type, name, name, chromseq, forward,
						tmin, tmax, cmin, cmax, emins, emaxs);

				if (seq_group != null) {
					seq_group.addToIndex(name, sym);
				}
				results.add(sym);

				if (tmax > chromseq.getLength()) {
					chromseq.setLength(tmax);
				}

				if (annotate_seq) {
					SimpleSymWithProps parent_sym = (SimpleSymWithProps) chrom2sym.get(chrom_name);
					if (parent_sym == null) {
						parent_sym = new SimpleSymWithProps();
						parent_sym.addSpan(new SimpleSeqSpan(0, chromseq.getLength(), chromseq));
						parent_sym.setProperty("method", annot_type);
						//              System.out.println("method: " + annot_type);
						parent_sym.setProperty("preferred_formats", pref_list);
						parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
						annots.add(parent_sym);
						chrom2sym.put(chrom_name, parent_sym);
					}
					//TODO: Make sure parent_sym is long enough to encompass all its children
					parent_sym.addChild(sym);
				}
				total_exon_count += ecount;
				count++;
			}
		} catch (EOFException ex) {
			// System.out.println("end of file reached, file successfully loaded");
			reached_EOF = true;
		} catch (IOException ioe) {
			throw ioe;
		} catch (Exception ex) {
			String message = "Problem processing BGN file";
			String m1 = ex.getMessage();
			if (m1 != null && m1.length() > 0) {
				message += ": " + m1;
			}
			IOException ioe = new IOException(message);
			ioe.initCause(ex);
			throw ioe;
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(dis);
		}

		if (annotate_seq) {
			for (SeqSymmetry annot : annots) {
				BioSeq chromseq = annot.getSpan(0).getBioSeq();
				chromseq.addAnnotation(annot);
			}
		}
		if (DEBUG) {
			System.out.println("bgn file load time: " + tim.read() / 1000f);
			System.out.println("transcript count = " + count);
			System.out.println("exon count = " + total_exon_count);
			if (count > 0) {
				System.out.println("average exons / transcript = " +
						((double) total_exon_count / (double) count));
			}
		}
		if (!reached_EOF) {
			System.out.println("File loading was terminated early.");
		}
		return results;
	}

	/**
	 *  Writes a single SeqSymmetry to the output stream in BGN format.
	 *  If the SeqSymmetry implements SupportsCdsSpan, then the CDS
	 *  span information will be written.  If not, then the BGN format is
	 *  probably not the best format to use, but since that can still be useful,
	 *  this routine will treat the entire span as the CDS.
	 */
	public void writeSymmetry(SeqSymmetry gsym, BioSeq targetSeq, OutputStream os) throws IOException {
		SeqSpan tspan = gsym.getSpan(0);
		SeqSpan cspan;
		String name;
		if (gsym instanceof UcscGeneSym) {
			UcscGeneSym ugs = (UcscGeneSym) gsym;
			cspan = ugs.getCdsSpan();
			name = ugs.getName();
		} else if (gsym instanceof SupportsCdsSpan) {
			cspan = ((SupportsCdsSpan) gsym).getCdsSpan();
			name = gsym.getID();
		} else {
			cspan = tspan;
			name = gsym.getID();
		}
		BioSeq seq = tspan.getBioSeq();
		DataOutputStream dos = null;
		if (os instanceof DataOutputStream) {
			dos = (DataOutputStream) os;
		} else {
			dos = new DataOutputStream(os);
		}
		dos.writeUTF(name);
		dos.writeUTF(seq.getID());
		if (tspan.isForward()) {
			dos.writeUTF("+");
		} else {
			dos.writeUTF("-");
		}
		dos.writeInt(tspan.getMin());
		dos.writeInt(tspan.getMax());
		dos.writeInt(cspan.getMin());
		dos.writeInt(cspan.getMax());
		dos.writeInt(gsym.getChildCount());
		int childcount = gsym.getChildCount();
		for (int k = 0; k < childcount; k++) {
			SeqSpan child = gsym.getChild(k).getSpan(seq);
			dos.writeInt(child.getMin());
		}
		for (int k = 0; k < childcount; k++) {
			SeqSpan child = gsym.getChild(k).getSpan(seq);
			dos.writeInt(child.getMax());
		}
	}

	/**
	 *  Writes a list of annotations to a file in BGN format.
	 *  @param annots  a List of SeqSymmetry objects, preferably implementing SupportsCdsSpan
	 */
	public void writeBinary(String file_name, List<SeqSymmetry> annots) throws IOException {
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File(file_name))));
			for (SeqSymmetry gsym : annots) {
				writeSymmetry(gsym, null, dos);
			}
		} finally {
			GeneralUtils.safeClose(dos);
		}
	}

	public void convertTextToBinary(String text_file, String bin_file, AnnotatedSeqGroup seq_group) {
		System.out.println("loading file: " + text_file);
		int count = 0;
		long flength = 0;
		int max_tlength = Integer.MIN_VALUE;
		int max_exons = Integer.MIN_VALUE;
		int max_spliced_length = Integer.MIN_VALUE;
		int total_exon_count = 0;
		int biguns = 0;
		int big_spliced = 0;

		Timer tim = new Timer();
		tim.start();
		FileInputStream fis = null;
		DataOutputStream dos = null;
		BufferedOutputStream bos = null;
		BufferedReader br = null;
		try {
			File fil = new File(text_file);
			flength = fil.length();
			fis = new FileInputStream(fil);

			br = new BufferedReader(new InputStreamReader(fis));

			File outfile = new File(bin_file);
			FileOutputStream fos = new FileOutputStream(outfile);
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);

			writeLines(
					br, count, seq_group, dos, biguns, total_exon_count, max_exons, max_tlength, tim, flength, max_spliced_length, big_spliced);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(fis);
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(bos);
		}

	}

	private void writeLines(BufferedReader br, int count, AnnotatedSeqGroup seq_group, DataOutputStream dos, int biguns, int total_exon_count, int max_exons, int max_tlength, Timer tim, long flength, int max_spliced_length, int big_spliced) throws NumberFormatException, IOException {
		String line = null;
		while ((line = br.readLine()) != null) {
			count++;
			String[] fields = line_regex.split(line);
			String name = fields[0];
			String chrom = fields[1];
			if (seq_group != null && seq_group.getSeq(chrom) == null) {
				System.out.println("sequence not recognized, ignoring: " + chrom);
				continue;
			}
			String strand = fields[2];
			String txStart = fields[3]; // min base of transcript on genome
			String txEnd = fields[4]; // max base of transcript on genome
			String cdsStart = fields[5]; // min base of CDS on genome
			String cdsEnd = fields[6]; // max base of CDS on genome
			String exonCount = fields[7]; // number of exons
			String exonStarts = fields[8];
			String exonEnds = fields[9];
			int tmin = Integer.parseInt(txStart);
			int tmax = Integer.parseInt(txEnd);
			int tlength = tmax - tmin;
			int cmin = Integer.parseInt(cdsStart);
			int cmax = Integer.parseInt(cdsEnd);
			int ecount = Integer.parseInt(exonCount);
			String[] emins = emin_regex.split(exonStarts);
			String[] emaxs = emax_regex.split(exonEnds);
			dos.writeUTF(name);
			dos.writeUTF(chrom);
			dos.writeUTF(strand);
			dos.writeInt(tmin);
			dos.writeInt(tmax);
			dos.writeInt(cmin);
			dos.writeInt(cmax);
			dos.writeInt(ecount);
			if (ecount != emins.length || ecount != emaxs.length) {
				System.out.println("EXON COUNTS DON'T MATCH UP FOR " + name + " !!!");
			} else {
				for (int i = 0; i < ecount; i++) {
					Integer.parseInt(emins[i]);
				}
				for (int i = 0; i < ecount; i++) {
					int emax = Integer.parseInt(emaxs[i]);
					dos.writeInt(emax);
				}
			}
			if (tlength >= 500000) {
				biguns++;
			}
			total_exon_count += ecount;
			max_exons = Math.max(max_exons, ecount);
			max_tlength = Math.max(max_tlength, tlength);
		}
		if (DEBUG) {
			System.out.println("load time: " + tim.read() / 1000f);
			System.out.println("line count = " + count);
			System.out.println("file length = " + flength);
			System.out.println("max genomic transcript length: " + max_tlength);
			System.out.println("max exons in single transcript: " + max_exons);
			System.out.println("total exons: " + total_exon_count);
			System.out.println("max spliced transcript length: " + max_spliced_length);
			System.out.println("spliced transcripts > 65000: " + big_spliced);
		}
	}

	/** For testing. */
	/*
	public static void main(String[] args) {
	String text_file = null;
	String bin_file = null;
	if (args.length == 2) {
	text_file = args[0];
	bin_file = args[1];
	} else {
	System.out.println("Usage:  java ... BgnParser <text infile> <binary outfile>");
	System.exit(1);
	}
	BgnParser test = new BgnParser();
	//    test.readTextTest(text_file, null);
	test.convertTextToBinary(text_file, bin_file, null);
	}*/
	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "binary UCSC gene" (.bgn)
	 **/
	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
			String type, OutputStream outstream) {
		System.out.println("in BgnParser.writeAnnotations()");
		try {
			DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(outstream));
			for (SeqSymmetry sym : syms) {
				if (!(sym instanceof UcscGeneSym)) {
					System.err.println("trying to output non-UcscGeneSym as UcscGeneSym!");
				}
				writeSymmetry((UcscGeneSym) sym, null, dos);
			}
			dos.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		return true;
	}

	public Comparator<SeqSymmetry> getComparator(BioSeq seq) {
		return new SeqSymMinComparator(seq);
	}

	public int getMin(SeqSymmetry sym, BioSeq seq) {
		SeqSpan span = sym.getSpan(seq);
		return span.getMin();
	}

	public int getMax(SeqSymmetry sym, BioSeq seq) {
		SeqSpan span = sym.getSpan(seq);
		return span.getMax();
	}

	public List<String> getFormatPrefList() {
		return BgnParser.pref_list;
	}

	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "binary UCSC gene".
	 **/
	public String getMimeType() {
		return "binary/bgn";
	}
}
