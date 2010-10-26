package com.affymetrix.genometryImpl.parsers;

import java.io.*;
import java.util.*;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.SymWithProps;

public final class ProbeSetDisplayPlugin implements AnnotationWriter {
	public static final String CONSENSUS_TYPE = "netaffx consensus";
	static final String PROBESET_TYPE = "netaffx probesets";
	static final String CROSSHYB_TYPE = "netaffx crosshyb";
	static final String POLY_A_STACKS_TYPE = "netaffx poly_a_stacks";
	static final String POLY_A_SITES_TYPE = "netaffx poly_a_sites";


	/** Converts array_name + " " + CONSENSUS_TYPE into array_name.
	 *  Example:  "HC_G110 netaffx consensus" becomes "HC_G110".
	 *  @return null if the argument doesn't end with " " + CONSENSUS_TYPE
	 */
	public static String getArrayNameFromRequestedType(String requested_type) {
		String result = null;
		int index = requested_type.indexOf(" "+CONSENSUS_TYPE);
		if (index > 0) {
			result = requested_type.substring(0, index);
		}
		return result;
	}


	/**
	 *  Basic technique:
	 *  1. Determine bounds on genome seq "D" as range query to get _all_
	 *        consensus2genome symmetries within those bounds on "D".
	 *  2. For each consensus2genome returned by range query, get the
	 *        consensus seqs that those consensus2genome point to.
	 *  3. For each consensus seq, get all of its annotations, which are
	 *        probeset2consensus and xhyb2consensus features.  Note
	 *        that if a consensus maps to the genome multiple times,
	 *        we should not list that consensus's probe sets or
	 *        cross-hybridized probe sets multiple times.
	 *  4. Return all consensus2genomes, probeset2consensus, xhyb2consensus
	 *        features via response.

	 *
	 *  Step 1 handled by normal DAS/2 feature query for consensus2genome annotations 
	 *  Step 2, 3, 4 handled as output issue...
	 *
	 */
	public static void collectAndWriteAnnotations(Collection<? extends SeqSymmetry> consensus_syms, boolean writeConsensus, BioSeq genome_seq, String array_name,
			OutputStream outstream) {

		String array_name_prefix = "";
		if (array_name != null && array_name.trim().length() >= 0) {
			array_name_prefix = array_name + " ";
		}
		String requested_type = array_name_prefix + CONSENSUS_TYPE;
		String probeset_type = array_name_prefix + PROBESET_TYPE;
		String poly_a_sites_type = array_name_prefix + POLY_A_SITES_TYPE;
		String poly_a_stacks_type = array_name_prefix + POLY_A_STACKS_TYPE;

		Set<SeqSymmetry> probesets = new HashSet<SeqSymmetry>();
		Set<SeqSymmetry> crossHybProbes = new HashSet<SeqSymmetry>();
		Set<SeqSymmetry> polyASites = new HashSet<SeqSymmetry>();
		Set<SeqSymmetry> polyAStacks = new HashSet<SeqSymmetry>();
		findProbeSets(consensus_syms, genome_seq, probeset_type, probesets, crossHybProbes, poly_a_sites_type, polyASites, poly_a_stacks_type, polyAStacks);

		PSLParser psl_parser = new PSLParser();

		if (writeConsensus) {
			writePSLTrack(psl_parser, consensus_syms, genome_seq, requested_type,    "Consensus Sequences", outstream);
		}
		writePSLTrack(psl_parser, probesets,      genome_seq, probeset_type,     "Probe Sets", outstream);
		writePSLTrack(psl_parser, polyASites,     genome_seq, poly_a_sites_type, "Poly-A Sites", outstream);
		writePSLTrack(psl_parser, polyAStacks,    genome_seq, poly_a_stacks_type, "Poly-A Stacks", outstream);
		writePSLTrack(psl_parser, crossHybProbes, genome_seq, CROSSHYB_TYPE,      "Cross-Hybridized Probes", outstream);
	}


	private static void findProbeSets(Collection<? extends SeqSymmetry> consensus_syms, BioSeq genome_seq, String probeset_type, Set<SeqSymmetry> probesets, Set<SeqSymmetry> crossHybProbes, String poly_a_sites_type, Set<SeqSymmetry> polyASites, String poly_a_stacks_type, Set<SeqSymmetry> polyAStacks) {
		for (SeqSymmetry current_c2g : consensus_syms) {
			
			if(Thread.currentThread().isInterrupted())
				break;

			// 2. For each consensus sequence symmetry, get the
			//    corresponding consensus BioSeq
			BioSeq aseq = SeqUtils.getOtherSeq(current_c2g, genome_seq);
			if (aseq == null) {
				continue;
			}
			int maxm = aseq.getAnnotationCount();
			for (int m = 0; m < maxm; m++) {
				SeqSymmetry container = aseq.getAnnotation(m);
				for (int cindex = 0; cindex < container.getChildCount(); cindex++) {
					// 3. For each consensus seq, get all of its annotations and collate
					// them by type
					SeqSymmetry cons_annot = container.getChild(cindex);
					if (cons_annot instanceof SymWithProps) {
						findProbeSet(cons_annot, probeset_type, probesets, crossHybProbes, poly_a_sites_type, polyASites, poly_a_stacks_type, polyAStacks);
					}
				}
			}
		}
	}


	private static void findProbeSet(SeqSymmetry cons_annot, String probeset_type, Set<SeqSymmetry> probesets, Set<SeqSymmetry> crossHybProbes, String poly_a_sites_type, Set<SeqSymmetry> polyASites, String poly_a_stacks_type, Set<SeqSymmetry> polyAStacks) {
		Set<SeqSymmetry> probesetsFound = null;
		SymWithProps cons_sym = (SymWithProps) cons_annot;
		String type = (String) cons_sym.getProperty("method");
		if (type.equalsIgnoreCase(probeset_type)) {
			probesetsFound = probesets;
		} else if (type.equalsIgnoreCase(CROSSHYB_TYPE)) {
			probesetsFound = crossHybProbes;
		} else if (type.equalsIgnoreCase(poly_a_sites_type)) {
			probesetsFound = polyASites;
		} else if (type.equalsIgnoreCase(poly_a_stacks_type)) {
			probesetsFound = polyAStacks;
		} else {
			System.out.println("findProbeSet: ERROR: couldn't find type:---" + type +"---");
			System.out.println("types are:" + probeset_type +","+CROSSHYB_TYPE+","+poly_a_sites_type+","+poly_a_stacks_type+",");
			return;
		}
		int childCount = cons_sym.getChildCount();
		for (int n = 0; n < childCount; n++) {
			SeqSymmetry cons_child = cons_annot.getChild(n);
			// assume for now all annotations of consensus seqs are wanted
			//   (they're either probeset2consensus or xhyb2consensus)
			probesetsFound.add(cons_child);
		}
	}



	public static void writePSLTrack(PSLParser parser, Collection<? extends SeqSymmetry> syms, BioSeq seq, String type,
			String description, OutputStream outstream) {
		if (! syms.isEmpty()) {
			parser.writeAnnotations(syms, seq, true, type, description, outstream);
		}
	}


	// implementation of AnnotationWriter    
	public String getMimeType() {
		return "text/plain"; // Don't really know what to put here!
	}

	// implementation of AnnotationWriter    
	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq, String type, OutputStream outstream) throws IOException {
		String array_name = getArrayNameFromRequestedType(type);
		System.out.println("in ProbesetDisplayPlugin.writeAnnotations(), array_name: " + array_name);
		if (array_name == null || seq == null) {
			return false;
		}
		collectAndWriteAnnotations(syms, true, seq, array_name, outstream);

		return true;
	}

}
