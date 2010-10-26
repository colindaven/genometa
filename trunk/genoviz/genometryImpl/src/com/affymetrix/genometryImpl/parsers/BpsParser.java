package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.*;
import java.util.*;

import com.affymetrix.genometryImpl.util.Timer;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.comparator.UcscPslComparator;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SeqSymmetryConverter;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class BpsParser implements AnnotationWriter, IndexWriter  {
	private static final UcscPslComparator comp = new UcscPslComparator();
	private static final List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("bps");
		pref_list.add("psl");
	}

	private static final int estimated_count = 80000;

	/** Reads binary PSL data from the given stream.  Note that this method <b>can</b>
	 *  be interrupted early by Thread.interrupt().  The input stream will always be closed
	 *  before exiting this method.
	 */
	public static List<UcscPslSym> parse(DataInputStream dis, String annot_type,
			AnnotatedSeqGroup query_group, AnnotatedSeqGroup target_group,
			boolean annot_query, boolean annot_target)
		throws IOException {

		// make temporary seq groups to avoid null pointers later
		if (query_group == null) {
			query_group = new AnnotatedSeqGroup("Query");
			query_group.setUseSynonyms(false);
		}
		if (target_group == null) {
			target_group = new AnnotatedSeqGroup("Target");
			target_group.setUseSynonyms(false);
		}

		Map<String,SeqSymmetry> target2sym = new HashMap<String,SeqSymmetry>(); // maps target chrom name to top-level symmetry
		Map<String,SeqSymmetry> query2sym = new HashMap<String,SeqSymmetry>(); // maps query chrom name to top-level symmetry
		List<UcscPslSym> results = new ArrayList<UcscPslSym>(estimated_count);
		int count = 0;

		try {
			Thread thread = Thread.currentThread();
			// Loop will usually be ended by EOFException, but
			// can also be interrupted by Thread.interrupt()
			while (! thread.isInterrupted()) {
				int matches = dis.readInt();
				int mismatches = dis.readInt();
				int repmatches = dis.readInt();
				int ncount = dis.readInt();
				int qNumInsert = dis.readInt();
				int qBaseInsert = dis.readInt();
				int tNumInsert = dis.readInt();
				int tBaseInsert = dis.readInt();
				boolean qforward = dis.readBoolean();
				String qname = dis.readUTF();
				int qsize = dis.readInt();
				int qmin = dis.readInt();
				int qmax = dis.readInt();

				BioSeq queryseq = query_group.getSeq(qname);
				if (queryseq == null)  {
					queryseq = query_group.addSeq(qname, qsize);
				}
				if (queryseq.getLength() < qsize) { queryseq.setLength(qsize); }

				String tname = dis.readUTF();
				int tsize = dis.readInt();
				int tmin = dis.readInt();
				int tmax = dis.readInt();


				BioSeq targetseq = target_group.getSeq(tname);
				if (targetseq == null) {
					targetseq = target_group.addSeq(tname, tsize);
				}
				if (targetseq.getLength() < tsize) { targetseq.setLength(tsize); }

				int blockcount = dis.readInt();
				int[] blockSizes = new int[blockcount];
				int[] qmins = new int[blockcount];
				int[] tmins = new int[blockcount];
				for (int i=0; i<blockcount; i++) {
					blockSizes[i] = dis.readInt();
				}
				for (int i=0; i<blockcount; i++) {
					qmins[i] = dis.readInt();
				}
				for (int i=0; i<blockcount; i++) {
					tmins[i] = dis.readInt();
				}
				count++;

				UcscPslSym sym =
					new UcscPslSym(annot_type, matches, mismatches, repmatches, ncount,
							qNumInsert, qBaseInsert, tNumInsert, tBaseInsert, qforward,
							queryseq, qmin, qmax, targetseq, tmin, tmax,
							blockcount, blockSizes, qmins, tmins);
				results.add(sym);


				if (annot_query) {
					SimpleSymWithProps query_parent_sym = (SimpleSymWithProps)query2sym.get(qname);
					if (query_parent_sym == null) {
						query_parent_sym = new SimpleSymWithProps();
						query_parent_sym.addSpan(new SimpleSeqSpan(0, queryseq.getLength(), queryseq));
						query_parent_sym.setProperty("method", annot_type);
						query_parent_sym.setProperty("preferred_formats", pref_list);
						query_parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
						queryseq.addAnnotation(query_parent_sym);
						query2sym.put(qname, query_parent_sym);
					}
					query_group.addToIndex(sym.getID(), sym);
					query_parent_sym.addChild(sym);
				}

				if (annot_target) {
					SimpleSymWithProps target_parent_sym = (SimpleSymWithProps)target2sym.get(tname);
					if (target_parent_sym == null) {
						target_parent_sym = new SimpleSymWithProps();
						target_parent_sym.addSpan(new SimpleSeqSpan(0, targetseq.getLength(), targetseq));
						target_parent_sym.setProperty("method", annot_type);
						target_parent_sym.setProperty("preferred_formats", pref_list);
						target_parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
						targetseq.addAnnotation(target_parent_sym);
						target2sym.put(tname, target_parent_sym);
					}
					target_parent_sym.addChild(sym);
					target_group.addToIndex(sym.getID(), sym);
				}
			}
		}
		catch (EOFException ex) {
		}
		finally {
			GeneralUtils.safeClose(dis);
		}

		if (count == 0) {
			Logger.getLogger(BpsParser.class.getName()).log(
							Level.INFO, "BPS total counts == 0 ???");
		}
		else {
			Collections.sort(results, comp);
		}
		return results;
	}


	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "binary PSL".
	 **/
	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
			String type, OutputStream outstream) {
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(outstream));
			for (SeqSymmetry sym : syms) {
				if (! (sym instanceof UcscPslSym)) {
					int spancount = sym.getSpanCount();
					if (spancount == 1) {
						sym = SeqSymmetryConverter.convertToPslSym(sym, type, seq);
					}
					else {
						BioSeq seq2 = SeqUtils.getOtherSeq(sym, seq);
						sym = SeqSymmetryConverter.convertToPslSym(sym, type, seq2, seq);
					}
				}
				this.writeSymmetry(sym,seq,dos);
			}
			dos.flush();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			return false;
		}
		finally {
			GeneralUtils.safeClose(dos);
		}
		return true;
	}

	public Comparator<UcscPslSym> getComparator(BioSeq seq) {
		return comp;
	}
	
	public void writeSymmetry(SeqSymmetry sym, BioSeq seq, OutputStream os) throws IOException {
		DataOutputStream dos = null;
		if (os instanceof DataOutputStream) {
			dos = (DataOutputStream)os;
		} else {
			dos = new DataOutputStream(os);
		}
		((UcscPslSym)sym).outputBpsFormat(dos);
	}

	public int getMin(SeqSymmetry sym, BioSeq seq) {
		return ((UcscPslSym)sym).getTargetMin();
	}

	public int getMax(SeqSymmetry sym, BioSeq seq) {
		return ((UcscPslSym)sym).getTargetMax();
	}
	public List<String> getFormatPrefList() {
		return BpsParser.pref_list;
	}
	public List<UcscPslSym> parse(DataInputStream dis, String annot_type, AnnotatedSeqGroup group) {
		try {
			return BpsParser.parse(dis, annot_type, null, group, false, false);
		} catch (IOException ex) {
			Logger.getLogger(BpsParser.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}
	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "binary PSL".
	 **/
	public String getMimeType() { return "binary/bps"; }


	public static void main(String[] args) throws IOException {
		if (args.length == 2) {
			String text_file = args[0];
			String bin_file = args[1];
			convertPslToBps(text_file, bin_file);
		} else {
			System.out.println("Usage:  java ... BpsParser <text infile> <binary outfile>");
			System.exit(1);
		}
	}


	private static void convertPslToBps(String psl_in, String bps_out)  {
		System.out.println("reading text psl file");
		List<UcscPslSym> psl_syms = readPslFile(psl_in);
		System.out.println("done reading text psl file, annot count = " + psl_syms.size());
		System.out.println("writing binary psl file");
		writeBinary(bps_out, psl_syms);
		System.out.println("done writing binary psl file");
	}


	private static List<UcscPslSym> readPslFile(String file_name) {
		Timer tim = new Timer();
		tim.start();

		List<UcscPslSym> results = null;
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		try {
			File fil = new File(file_name);
			long flength = fil.length();
			fis = new FileInputStream(fil);
			InputStream istr = null;
			byte[] bytebuf = new byte[(int) flength];
			bis = new BufferedInputStream(fis);
			bis.read(bytebuf);
			bis.close();
			ByteArrayInputStream bytestream = new ByteArrayInputStream(bytebuf);
			istr = bytestream;

			PSLParser parser = new PSLParser();
			// don't bother annotating the sequences, just get the list of syms
			results = parser.parse(istr, file_name, null, null, false, false);
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(fis);
		}
		Logger.getLogger(BpsParser.class.getName()).log(
							Level.INFO, "finished reading PSL file, time to read = {0}", (tim.read() / 1000f));
		return results;
	}

	private static void writeBinary(String file_name, List<UcscPslSym> syms)  {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try  {
			File outfile = new File(file_name);
			fos = new FileOutputStream(outfile);
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
			for (UcscPslSym psl : syms) {
				psl.outputBpsFormat(dos);
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(bos);
			GeneralUtils.safeClose(fos);
		}
	}

}

