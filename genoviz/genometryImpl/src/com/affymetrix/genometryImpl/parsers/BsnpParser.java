/**
 *   Copyright (c) 2005-2007 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import com.affymetrix.genometryImpl.UcscGffSym;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.comparator.SeqSymMinComparator;
import com.affymetrix.genometryImpl.EfficientSnpSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.util.Timer;

/**
 *
 *  A class for parsing bsnp files.
 *
 *  <pre>
 *  Currently the type field is ignored (type info is not included in the output .bsnp file),
 *    because for all the snp files looked at so far from UCSC, the type for all entries has been "SNP"
 *
 *  Text SNP format (from UCSC sql tables, snpMap.sql):
 *
 *     bin smallint(5) unsigned NOT NULL default '0',
 *     chrom varchar(255) NOT NULL default '',
 *     chromStart int(10) unsigned NOT NULL default '0',
 *     chromEnd int(10) unsigned NOT NULL default '0',
 *     name varchar(255) NOT NULL default '',
 *     source enum('BAC_OVERLAP','MIXED','RANDOM','OTHER','Affy10K','Affy120K','unknown') NOT NULL default 'unknown',
 *     type enum('SNP','INDEL','SEGMENTAL','unknown') NOT NULL default 'unknown',
 *
 *  BSNP format:
 *
 *
 *   header string???
 *   genome organism / version / etc. ???
 *   seq_count  4-byte signed int
 *   source_count 4-byte signed int
 *   type_count 4-byte signed int
 *   [ id_constructor instructions ???]
 *   for each seq (seq_count)  {
 *      chromid    UTF-8 string
 *      for each (source)  {
 *         source_id  UTF-8 string
 *         for each (type)  {
 *            type_id  UTF-8 string
 *            snp_count  4-byte signed int
 *         }
 *      }
 *   }
 *   for each seq (seq_count)  {
 *      for each (source)  {
 *         for each (type)  {
 *            for each snp (snp_count)  {
 *               base_position 4-byte signed int
 *               numeric_id  4-byte signed int
 *            }
 *         }
 *      }
 *   }
 *
 *
 *  // first pass:
 *  genome_version
 *  number of seqs annotated
 *  for each seq  {
 *     seqid
 *     snp_count
 *  }
 *  for each seq  {
 *     for each snp (snp_count)  {
 *        base_position
 *     }
 *  }
 *
 *</pre>
 */
public abstract class BsnpParser {

	private static final Pattern line_regex = Pattern.compile("\\s+");  // replaced single tab with one or more whitespace

	private static void outputBsnpFormat(List<SeqSymmetry> parents, String genome_version, DataOutputStream dos)
		throws IOException {
			int pcount = parents.size();
			dos.writeUTF(genome_version);
			dos.writeInt(pcount);  // how many seqs there are
			for (int i=0; i<pcount; i++) {
				SeqSymmetry parent = parents.get(i);
				BioSeq seq = parent.getSpanSeq(0);
				String seqid = seq.getID();
				int snp_count = parent.getChildCount();
				dos.writeUTF(seqid);
				dos.writeInt(snp_count);
			}

			int total_snp_count = 0;
			for (int i=0; i<pcount; i++) {
				SeqSymmetry parent = parents.get(i);
				BioSeq seq = parent.getSpanSeq(0);
				int snp_count = parent.getChildCount();
				List<SeqSymmetry> snps = new ArrayList<SeqSymmetry>(snp_count);
				for (int k=0; k<snp_count; k++) {
					// need to make sure SNPs are written out in sorted order!
					snps.add(parent.getChild(k));
				}
				Collections.sort(snps, new SeqSymMinComparator(seq));
				for (int k=0; k<snp_count; k++) {
					EfficientSnpSym snp = (EfficientSnpSym)snps.get(k);
					int base_coord = snp.getSpan(0).getMin();
					dos.writeInt(base_coord);
					total_snp_count++;
				}
			}
			System.out.println("total snps output to bsnp file: " + total_snp_count);
	}

	/**
	 *  Reads a GFF document containing SNP data.
	 *  Assumes specific GFF variant used to represent SNPs on Affy genotyping chips:
	 *<pre>
#seqname        enzyme        probeset_id        start        end        score        strand        frame
chr1        XbaI        SNP_A-1507333        219135381        219135381        .        +        .
	 *</pre>
	 */
	private static List<SeqSymmetry> readGffFormat(InputStream istr, GenometryModel gmodel) throws IOException {
		AnnotatedSeqGroup seq_group = gmodel.addSeqGroup("Test Group");

		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();
		GFFParser gff_parser = new GFFParser();
		gff_parser.parse(istr, seq_group, true);
		int problem_count = 0;
		for (BioSeq aseq : seq_group.getSeqList()) {
			int acount = aseq.getAnnotationCount();
			String seqid = aseq.getID();
			System.out.println("seq = " + seqid + ", annots = " + acount);
			// for some reason having diffent enzymes in source column causes parent sym to be added as annotation multiple times!
			// therefore just taking first annotation
			// need to debug this eventually...
			if (acount >= 1) {
				SimpleSymWithProps new_psym = new SimpleSymWithProps();
				BioSeq seq = new BioSeq(seqid, aseq.getVersion(), 1000000000);
				new_psym.addSpan(new SimpleSeqSpan(0, 1000000000, seq));
				new_psym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
				for (int k = 0; k < acount; k++) {
					SeqSymmetry psym = aseq.getAnnotation(k);
					int child_count = psym.getChildCount();
					System.out.println("    child annots: " + child_count);

					for (int i = 0; i < child_count; i++) {
						UcscGffSym csym = (UcscGffSym) psym.getChild(i);
						int coord = csym.getSpan(0).getMin();
						csym.getFeatureType();  // because of quirk in how GFF files are constructed
						EfficientSnpSym snp_sym = new EfficientSnpSym(new_psym, coord);
						new_psym.addChild(snp_sym);
					}
				}
				results.add(new_psym);
			}
		}
		System.out.println("problems: " + problem_count);

		return results;
	}


	private static List<SeqSymmetry> readTextFormat(BufferedReader br) {
		int snp_count = 0;
		int weird_length_count = 0;
		Map<String,SeqSymmetry> id2psym = new HashMap<String,SeqSymmetry>();
		List<SeqSymmetry> parent_syms = new ArrayList<SeqSymmetry>();
		try {
			String line;
			while ((line = br.readLine()) != null) {
				String[] fields = line_regex.split(line);
				String seqid = fields[1].intern();
				MutableSeqSymmetry psym = (MutableSeqSymmetry)id2psym.get(seqid);
				if (psym == null) {
					psym = new SimpleSymWithProps();
					BioSeq seq = new BioSeq(seqid, seqid, 1000000000);
					psym.addSpan(new SimpleSeqSpan(0, 1000000000, seq));
					((SymWithProps) psym).setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
					id2psym.put(seqid, psym);
					parent_syms.add(psym);
				}
				int min = Integer.parseInt(fields[2]);
				int max = Integer.parseInt(fields[3]);
				int length = (max - min);
				if (length != 1) {
					System.out.println("length != 1: " + line);
					weird_length_count++;
				}
				EfficientSnpSym snp_sym = new EfficientSnpSym(psym, min);
				psym.addChild(snp_sym);
				snp_count++;
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		System.out.println("snp count: " + snp_count);
		System.out.println("weird length count: " + weird_length_count);
		return parent_syms;
	}

	public static List<SeqSymmetry> parse(InputStream istr, String annot_type, AnnotatedSeqGroup seq_group, boolean annot_seq)
		throws IOException {
		Timer tim = new Timer();
		tim.start();
		List<SeqSymmetry> snp_syms = null;

		BufferedInputStream bis;
		if (istr instanceof BufferedInputStream) { bis = (BufferedInputStream)istr; }
		else { bis = new BufferedInputStream(istr); }
		DataInputStream dis = new DataInputStream(bis);
		dis.readUTF();
		int seq_count = dis.readInt();
		int[] snp_counts = new int[seq_count];
		String[] seqids = new String[seq_count];
		BioSeq[] seqs = new BioSeq[seq_count];
		int total_snp_count = 0;
		for (int i=0; i<seq_count; i++) {
			String seqid = dis.readUTF();
			seqids[i] = seqid;
			BioSeq aseq = seq_group.getSeq(seqid);
			if (aseq == null) {
				aseq = seq_group.addSeq(seqid, 0);
			}
			seqs[i] = aseq;
			snp_counts[i] = dis.readInt();
			total_snp_count += snp_counts[i];
		}
		snp_syms = new ArrayList<SeqSymmetry>(total_snp_count);
		for (int i=0; i<seq_count; i++) {
			BioSeq aseq = seqs[i];
			int snp_count = snp_counts[i];
			SimpleSymWithProps psym = new SimpleSymWithProps();
			psym.setProperty("type", annot_type);
			psym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
			if (aseq != null) { psym.addSpan(new SimpleSeqSpan(0, aseq.getLength(), aseq)); }
			else { 
				psym.addSpan(new SimpleSeqSpan(0, 1000000000, null)); 
			}
			if (annot_seq && (aseq != null))  {
				aseq.addAnnotation(psym);
			}
			int base_coord = 0;
			for (int k=0; k<snp_count; k++) {
				base_coord = dis.readInt();
				EfficientSnpSym snp = new EfficientSnpSym(psym, base_coord);
				psym.addChild(snp);
				snp_syms.add(snp);
			}
			// I'm assuming the snp coords are sorted from min to max, thus the last coord is the max
			if (aseq != null && aseq.getLength() < base_coord) {
				aseq.setLength(base_coord);
			}
		}

		tim.print();
		return snp_syms;
	}

	public static void main(String[] args) {
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		try {
			if (args.length >= 2) {
				String genome_version = args[0];
				String text_infile = args[1];
				String bin_outfile;

				if (args.length >= 3) {
					bin_outfile = args[2];
				} else if (text_infile.endsWith(".txt")
						|| text_infile.endsWith(".gff")) {
					bin_outfile = text_infile.substring(0, text_infile.length() - 4) + ".bsnp";
				} else {
					bin_outfile = text_infile + ".bsnp";
				}
				File ifil = new File(text_infile);
				List<SeqSymmetry> parent_syms = new ArrayList<SeqSymmetry>();
				if (text_infile.endsWith(".txt")) {
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ifil)));
					System.out.println("reading in text data from: " + text_infile);
					parent_syms = BsnpParser.readTextFormat(br);
					br.close();
				} else if (text_infile.endsWith(".gff")) {
					InputStream istr = new FileInputStream(ifil);
					System.out.println("reading in gff data from: " + text_infile);
					parent_syms = BsnpParser.readGffFormat(istr, gmodel);
					istr.close();
				}

				File ofil = new File(bin_outfile);
				DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(ofil)));
				System.out.println("outputting binary data to: " + bin_outfile);
				BsnpParser.outputBsnpFormat(parent_syms, genome_version, dos);
				dos.close();
				System.out.println("finished converting text data to binary .bsnp format");
			} else {
				System.out.println("Usage:  java ... BsnpParser <genome_version> <text infile> [<binary outfile>]");
				System.exit(1);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	// Annotationwriter implementation
	//  public boolean writeAnnotations(Collection syms, BioSeq seq,
	//                                  String type, OutputStream outstream) {
	//  }
	// public String getMimeType()  { return "binary/bsnp"; }

}
