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

import com.affymetrix.genometryImpl.symmetry.LeafSingletonSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.util.GeneralUtils;

/**
 *
 *  Parser for the Text repeat format (from UCSC sql tables, chr*_rmsk.sql).
 *
 *  <pre>
 *  bin smallint(5) unsigned NOT NULL default '0',
 *  swScore int(10) unsigned NOT NULL default 'b0',
 *  milliDiv int(10) unsigned NOT NULL default '0',
 *  milliDel int(10) unsigned NOT NULL default '0',
 *  milliIns int(10) unsigned NOT NULL default '0',
 *  genoName varchar(255) NOT NULL default '',
 *  genoStart int(10) unsigned NOT NULL default '0',
 *  genoEnd int(10) unsigned NOT NULL default '0',
 *  genoLeft int(11) NOT NULL default '0',     
 *  strand char(1) NOT NULL default '',
 *  repName varchar(255) NOT NULL default '',
 *  repClass varchar(255) NOT NULL default '',
 *  repFamily varchar(255) NOT NULL default '',
 *  repStart int(11) NOT NULL default '0',
 *  repEnd int(11) NOT NULL default '0',
 *  repLeft int(11) NOT NULL default '0',
 *  id char(1) NOT NULL default '',
 *
 *
 *  Frist pass at binary representation: 
 *  genome_version
 *  // TO BE ADDED number of different repeat types
 *  // TO BE ADDED for each repeat type {
 *  // TO BE ADDED     type id
 *  // TO BE ADDED     byte/short/int used to code for repeat type
 *  // TO BE ADDED  }
 *  number of seqs annotated
 *  for each seq  {
 *     seqid
 *     repeat_count
 *  }
 *  for each seq  {
 *     for each repeat (repeat_count)  {
 *        base_start
 *        base_end   [ and if base_start > base_end then on negative strand ]
 *        // TO BE ADDED byte/short/int? for repeat type
 *     }
 *  }
 *
 * </pre>
 */
public final class BrptParser {
	//    static String default_text_infile = "c:/data/ucsc/hg17/repeats/rmsk_all.txt";
	//    static String genome_version = "H_sapiens_Apr_2003";

	//  static Pattern line_regex = Pattern.compile("\t");
	static Pattern line_regex = Pattern.compile("\\s+");  // replaced single tab with one or more whitespace
	Map source_hash = new HashMap();
	Map type_hash = new HashMap();

	private static void outputBrptFormat(List<SeqSymmetry> parents, String genome_version, DataOutputStream dos)
			throws IOException {
		int pcount = parents.size();
		dos.writeUTF(genome_version);
		dos.writeInt(pcount);  // how many seqs there are
		for (int i = 0; i < pcount; i++) {
			SeqSymmetry parent = parents.get(i);
			BioSeq seq = parent.getSpanSeq(0);
			String seqid = seq.getID();
			int rpt_count = parent.getChildCount();
			dos.writeUTF(seqid);
			dos.writeInt(rpt_count);
		}

		for (int i = 0; i < pcount; i++) {
			SeqSymmetry parent = parents.get(i);
			int rpt_count = parent.getChildCount();
			for (int k = 0; k < rpt_count; k++) {
				LeafSingletonSymmetry rpt = (LeafSingletonSymmetry) parent.getChild(k);
				SeqSpan span = rpt.getSpan(0);
				int start = span.getStart();
				int end = span.getEnd();
				dos.writeInt(start);
				dos.writeInt(end);
			}
		}
	}

	private static List<SeqSymmetry> readTextFormat(BufferedReader br) throws IOException {
		//int weird_length_count = 0;
		Map<String,MutableSeqSymmetry> id2psym = new HashMap<String,MutableSeqSymmetry>();
		ArrayList<SeqSymmetry> parent_syms = new ArrayList<SeqSymmetry>();
		int repeat_count = 0;
		int pos_count = 0;
		int neg_count = 0;

		try {
			String line;
			while ((line = br.readLine()) != null) {
				String[] fields = line_regex.split(line);
				String seqid = fields[5].intern();
				BioSeq seq = null;
				MutableSeqSymmetry psym = id2psym.get(seqid);
				if (psym == null) {
					psym = new SimpleSymWithProps();
					seq = new BioSeq(seqid, seqid, 1000000000);
					psym.addSpan(new SimpleSeqSpan(0, 1000000000, seq));
					((SymWithProps) psym).setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
					id2psym.put(seqid, psym);
					parent_syms.add(psym);
				}
				else {
					seq = psym.getSpanSeq(0);
				}
				int min = Integer.parseInt(fields[6]);
				int max = Integer.parseInt(fields[7]);
				int start;
				int end;
				String strand = fields[9];
				if (strand.equals("-")) {  // on negative strand
					start = max;
					end = min;
					neg_count++;
				}
				else {  // else on positive strand
					start = min;
					end = max;
					pos_count++;
				}
				LeafSingletonSymmetry rpt_sym = new LeafSingletonSymmetry(start, end, seq);
				psym.addChild(rpt_sym);
				repeat_count++;
			}
		}
		finally {

		}
		System.out.println("repeat count: " + repeat_count);
		System.out.println("repeats on + strand: " + pos_count);
		System.out.println("repeats on - strand: " + neg_count);
		return parent_syms;
	}

	public static List<SeqSymmetry> parse(InputStream istr, String annot_type, AnnotatedSeqGroup seq_group, boolean annot_seq)
		throws IOException {
		System.out.println("parsing brpt file");
		List<SeqSymmetry> rpt_syms = null;
		BufferedInputStream bis = null;
		DataInputStream dis = null;
		try {
			if (istr instanceof BufferedInputStream) { bis = (BufferedInputStream)istr; }
			else { bis = new BufferedInputStream(istr); }
			dis = new DataInputStream(bis);
			String genome_version = dis.readUTF();
			int seq_count = dis.readInt();
			int[] rpt_counts = new int[seq_count];
			String[] seqids = new String[seq_count];
			BioSeq[] seqs = new BioSeq[seq_count];
			System.out.println("genome version: " + genome_version);
			System.out.println("seqs: " + seq_count);
			int total_rpt_count = 0;
			for (int i=0; i<seq_count; i++) {
				String seqid = dis.readUTF();
				seqids[i] = seqid;
				BioSeq aseq = seq_group.getSeq(seqid);
				if (aseq == null) {
					aseq = seq_group.addSeq(seqid, 0);
				}
				seqs[i] = aseq;
				rpt_counts[i] = dis.readInt();
				total_rpt_count += rpt_counts[i];
			}
			System.out.println("total rpts: " + total_rpt_count);
			rpt_syms = new ArrayList<SeqSymmetry>(total_rpt_count);
			for (int i=0; i<seq_count; i++) {
				BioSeq aseq = seqs[i];

				int rpt_count = rpt_counts[i];
				System.out.println("seqid: " + seqids[i] + ", rpts: " + rpt_counts[i]);
				SimpleSymWithProps psym = new SimpleSymWithProps();
				psym.setProperty("type", annot_type);
				psym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
				psym.addSpan(new SimpleSeqSpan(0, 1000000000, aseq));
				if (annot_seq && (aseq != null))  {
					aseq.addAnnotation(psym);
				}
				for (int k=0; k<rpt_count; k++) {
					int start = dis.readInt();
					int end = dis.readInt();
					LeafSingletonSymmetry rpt = new LeafSingletonSymmetry(start, end, aseq);
					psym.addChild(rpt);
					rpt_syms.add(rpt);
				}
			}
		}
		finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(dis);
		}
		return rpt_syms;
	}

	static boolean TEST_BINARY_PARSE = false;
	public static void main(String[] args) {
		try {
			if (TEST_BINARY_PARSE) {
				String binfile = args[0];
				System.out.println("parsing in rpt data from .brpt file: " + binfile);
				File ifil = new File(binfile);
				InputStream istr = new FileInputStream(ifil);
				GenometryModel gmodel = GenometryModel.getGenometryModel();
				AnnotatedSeqGroup seq_group = gmodel.addSeqGroup("Test Group");
				parse(istr, "rpt", seq_group, true);
				System.out.println("finished parsing in rpt data from .brpt file");
			}
			else {
				if (args.length >= 2) {
					String genome_version = args[0];
					String text_infile = args[1];
					String bin_outfile;
					if (args.length >= 3) {
						bin_outfile = args[2];
					}
					else if (text_infile.endsWith(".txt")) {
						bin_outfile = text_infile.substring(0, text_infile.length()-4)+ ".brpt";
					}
					else {
						bin_outfile = text_infile + ".brpt";
					}

					File ifil = new File(text_infile);
					BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(ifil)));
					System.out.println("reading in text data from: " + text_infile);
					List<SeqSymmetry> parent_syms = readTextFormat(br);
					File ofil = new File(bin_outfile);
					DataOutputStream dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(ofil)));
					System.out.println("outputing binary data to: " + bin_outfile);
					outputBrptFormat(parent_syms, genome_version, dos);
					System.out.println("finished converting text data to binary .brpt format");
				}
				else {
					System.out.println("Usage:  java ... BsnpParser <genome_version> <text infile> [<binary outfile>]");
					System.exit(1);
				}
			}
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	// Annotationwriter implementation
	//  public boolean writeAnnotations(Collection syms, BioSeq seq,
	//                                  String type, OutputStream outstream) {
	//  }
	// public String getMimeType()  { return "binary/brpt"; }

}
