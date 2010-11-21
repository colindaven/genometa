package com.affymetrix.igb.genometry;

import affymetrix.calvin.data.ProbeSetQuantificationData;
import affymetrix.calvin.data.ProbeSetQuantificationDetectionData;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.IndexedSingletonSym;
import com.affymetrix.genometryImpl.IndexedSym;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.comparator.SeqSymMinComparator;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;

import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.comparator.QuantByIntIdComparator;
import com.affymetrix.genometryImpl.comparator.QuantDetectByIntIdComparator;
import com.affymetrix.genometryImpl.util.StringUtils;
import com.affymetrix.genometryImpl.IntId;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SingletonSymWithIntId;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.das2.Das2Region;
import com.affymetrix.genometryImpl.das2.Das2ServerInfo;
import com.affymetrix.genometryImpl.das2.Das2Type;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.ScoredContainerSym;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.util.*;

/**
 *  Want to automatically load location data for probesets on chip
 *
 *  Needed for:
 *      3' IVT Expression chips
 *      Exon chips and other expression that is non-3' IVT
 *      [Genotyping chips?  Not yet implemented]
 *
 *  Not necessary for tiling array chips
 *  Probably not necessary for sequencing chips (but those aren't supported yet)
 *
 *  Basic strategy is to retrieve probeset id and location data from the main public Affymetrix DAS/2 server,
 *     and match by id to associate locations with the probeset results.
 *
 *  Used for sequence annotations based on Affymetrix CHP file data when
 *  coords aren't included in the CHP files
 *
 *  Might get further optimizations by not extending from ScoreContainerSym,
 *     but for now want to leverage off use of ScoredContainerGlyphFactory to render as graphs
 *
 *  For every CHP file that needs coord resolution there should be a LazyChpSym for each sequence in the genome
 *
 * @version $Id: LazyChpSym.java 6866 2010-09-08 14:27:58Z jnicol $
 */
public final class LazyChpSym extends ScoredContainerSym {

  public static final String PROBESET_SERVER_NAME = "NetAffx";
  private final BioSeq aseq;


  /**
   *  list of probeset result data for probesets whose name/id can be
   *   represented as an integer
   *  list should be sorted by integer id
   */
  private final List int_entries;

  /** in Affy Fusion SDK this is called "CHP array type", for example "HuEx-1_0-st-v2" */
  private final String chp_array_type;


  /**
   *  Assumes entries_with_int_id is already sorted by int id
   */
  public LazyChpSym(BioSeq seq, String array_type, List entries_with_int_id) {
    this.aseq = seq;
    this.chp_array_type = array_type;
    this.int_entries = entries_with_int_id;
  }

  /**
   *  Pointer to set of scored results + ids (probably as a ScoreEntry (see ChpParser))

   *
   *
   *
   *  Coords & ids are retrieved on a per-seq basis via a DAS/2 server, preferably in an optimized binary format
   *  [server_root]/[genomeid]/features?segment=[seqid];
   */
  private boolean coords_loaded = false;

	@Override
  public int getChildCount() {
    if (! coords_loaded) { loadCoords(); }
    return super.getChildCount();
  }

	@Override
  public SeqSymmetry getChild(int index) {
    if (! coords_loaded) { loadCoords(); }
    return super.getChild(index);
  }

	@Override
  public float[] getChildScores(IndexedSym child, List scorelist) {
    if (! coords_loaded) { loadCoords(); }
    return super.getChildScores(child, scorelist);
  }

	@Override
  public float[] getChildScores(IndexedSym child) {
    if (! coords_loaded) { loadCoords(); }
    return super.getChildScores(child);
  }

	@Override
  public int getScoreCount() {
    if (! coords_loaded) { loadCoords(); }
    return super.getScoreCount();
  }

	@Override
  public float[] getScores(String name) {
    if (! coords_loaded) { loadCoords(); }
    return super.getScores(name);
  }

	@Override
  public float[] getScores(int index) {
    if (! coords_loaded) { loadCoords(); }
    return super.getScores(index);
  }

	@Override
  public String getScoreName(int index)  {
    if (! coords_loaded) { loadCoords(); }
    return super.getScoreName(index);
  }


	@SuppressWarnings("unchecked")
  private void loadCoords() {
		coords_loaded = true;
		/**
		 *  Coords & ids are retrieved on a per-seq basis via a DAS/2 server, preferably in an optimized binary format
		 *      [server_root]/[genomeid]/features?segment=[seqid];
		 *  DAS/2 query is run through Das2ClientOptimizer, so only regions that haven't been retrieved yet are queried for
		 *  If features have already been retrieved for entire seq, then optimizer won't make any feature query calls
		 */
		GenericServer gServer = ServerList.getServer(PROBESET_SERVER_NAME);
		// server and vsource should already be checked before making this LazyChpSym, but checking again
		//     in case connection can no longer be established
		if (gServer == null) {
			ErrorHandler.errorPanel("Couldn't find server to retrieve location data for CHP file, server = " + PROBESET_SERVER_NAME);
			return;
		}
		Das2ServerInfo server = (Das2ServerInfo) gServer.serverObj;
		Das2VersionedSource vsource = server.getVersionedSource(aseq.getSeqGroup());
		if (vsource == null) {
			ErrorHandler.errorPanel("Couldn't find genome data on server for CHP file, genome = " + aseq.getSeqGroup().getID());
			return;
		}
		Das2Region das_segment = vsource.getSegment(aseq);
		if (das_segment == null) {
			ErrorHandler.errorPanel("Couldn't find sequence data on server for CHP file, seq = " + aseq.getID());
			return;
		}

		ProbeSetQuantificationDetectionData quant_detect = new ProbeSetQuantificationDetectionData();
		ProbeSetQuantificationData quant = new ProbeSetQuantificationData();
		QuantDetectByIntIdComparator quant_detect_comp = null;
		QuantByIntIdComparator quant_comp = null;
		if (int_entries != null && int_entries.size() > 0) {
			Object data = int_entries.get(0);
			if (data instanceof ProbeSetQuantificationDetectionData) {
				quant_detect_comp = new QuantDetectByIntIdComparator();
			} else if (data instanceof ProbeSetQuantificationData) {
				quant_comp = new QuantByIntIdComparator();
			}
		}
		Set<Das2Type> matched_types = determineMatchedTypes(chp_array_type,vsource);

		if (matched_types.isEmpty()) {
			// no DAS/2 type found for the CHP!
			System.out.println("****** WARNING: could not find location data for CHP array type: " + chp_array_type);
			return;
		}

		List<SeqSymmetry> symlist = new ArrayList<SeqSymmetry>(10000);
		List id_data_hits = new ArrayList(10000);
		List<SeqSymmetry> id_sym_hits = new ArrayList<SeqSymmetry>(10000);
		int id_hit_count = 0;
		int str_hit_count = 0;
	    int all_digit_not_int = 0;
		loadTypes(aseq, matched_types, symlist);

		// should the syms be sorted here??
		Collections.sort(symlist, new SeqSymMinComparator(aseq));

		// Iterate through probeset annotations, if possible do integer id binary search,
		//     otherwise do hash for string ID
		for (SeqSymmetry annot : symlist) {
			Object data = null;
			if (annot instanceof IntId) {
				// want to use integer id to avoid lots of String churn
				IntId isym = (IntId) annot;
				int nid = isym.getIntID();
				int index = -1;
				if (quant_detect_comp != null) {
					quant_detect.setId(nid);
					index = Collections.binarySearch(int_entries, quant_detect, quant_detect_comp);
				} else if (quant_comp != null) {
					quant.setId(nid);
					index = Collections.binarySearch(int_entries, quant, quant_comp);
				}
				if (index >= 0) {  // if index >= 0 then found entry at that index
					data = int_entries.get(index);
					id_hit_count++;
				}
			} else {  //  annot is not an IntId, try string ID
				String id = annot.getID();
				// try making id an integer and hashing to probeset_id2data
				// if not an integer, try id as string and hashing to probeset_name2data
				// [ what if can make it an integer, but no hit in probeset_id2data -- should also try probeset_name2data?
				//     NO, for now consider that a miss -- if id in CHP file _can_ be an integer,
				//     should have been converted in ChpParser to an Integer and populated in probeset_id2data ]
				if (id != null) {
					if (data == null && StringUtils.isAllDigits(id)) {
						// using a simple isAllDigits() method here, which will miss some
						//    want to avoid needing try/catch unless most likely can parse as integer
						try {
							int nid = Integer.parseInt(id);
							int index = -1;
							if (quant_detect_comp != null) {
								quant_detect.setId(nid);
								index = Collections.binarySearch(int_entries, quant_detect, quant_detect_comp);
							} else if (quant_comp != null) {
								quant.setId(nid);
								index = Collections.binarySearch(int_entries, quant, quant_comp);
							}
							if (index >= 0) {  // if index >= 0 then found entry at that index
								data = int_entries.get(index);
								id_hit_count++;
							}
						} catch (Exception ex) { // can't parse as an integer (even though all chars are digits)
							all_digit_not_int++;
						}
					}
				}
			}  // end non-IntId conditional
			if (data != null) {
				id_data_hits.add(data);
				id_sym_hits.add(annot);
			}
		}

		// now see what was found
		float[] quants = new float[id_hit_count];
		float[] pvals = new float[id_hit_count];
		boolean has_pvals = generateDataArrays(id_hit_count, id_data_hits, id_sym_hits, quants, pvals);
		this.addScores("score", quants);
		if (has_pvals) {
			this.addScores("pval", pvals);
		}
		System.out.println("Matching probeset integer IDs with CHP data, matches: " + id_hit_count);
		System.out.println("Matching non-integer string IDs with CHP data, matches: " + str_hit_count);
	}

	private static void loadTypes(BioSeq aseq, Set<Das2Type> matched_types, List<SeqSymmetry> symlist) {
		for (Das2Type das_type : matched_types) {
			// Set the human name on the tier to the short type name, not the long URL ID
			DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(das_type.getURI().toString()).setHumanName(das_type.getName());
			SeqSpan whole_span = new SimpleSeqSpan(0, aseq.getLength(), aseq);

			// if already retrieved chp_array_type coord annotations for this whole sequence (for example
			//   due to a previously loaded CHP file with same "array_type", then optimizer
			//   will figure this out and not make any queries --
			//   so if load multiple chps of same array type, actual feature query to DAS/2 server only happens once (per seq)
			// optimizer should also figure out (based on Das2Type info) an optimized format to load data with
			//   (for example "bp2" for
			LoadStrategy ls = das_type.getFeature().loadStrategy;
			if (ls == LoadStrategy.NO_LOAD || ls == LoadStrategy.VISIBLE) {
				ls = LoadStrategy.CHROMOSOME;
			}
			GeneralLoadUtils.loadAndDisplaySpan(whole_span, das_type.getFeature());

			TypeContainerAnnot container = (TypeContainerAnnot) aseq.getAnnotation(das_type.getURI().toString()); // should be a TypeContainerAnnot

			// collect probeset annotations for given chp type
			//     (probesets should be at 2rd level down in annotation hierarchy)
			for (int i = 0; i < container.getChildCount(); i++) {
				SeqSymmetry sym = container.getChild(i);
				addIdSyms(sym, symlist);
			}
		}
	}

	private boolean generateDataArrays(int id_hit_count, List id_data_hits, List<SeqSymmetry> id_sym_hits, float[] quants, float[] pvals) {
		boolean has_pvals = false;
		for (int i = 0; i < id_hit_count; i++) {
			Object data = id_data_hits.get(i);
			SeqSymmetry sym = id_sym_hits.get(i);
			SeqSpan span = sym.getSpan(0);
			IndexedSingletonSym isym = new IndexedSingletonSym(span.getStart(), span.getEnd(), span.getBioSeq());
			this.addChild(isym);
			if (data instanceof ProbeSetQuantificationData) {
				ProbeSetQuantificationData pdata = (ProbeSetQuantificationData) data;
				quants[i] = pdata.getQuantification();
				pvals[i] = 0;
			} else if (data instanceof ProbeSetQuantificationDetectionData) {
				ProbeSetQuantificationDetectionData pdata = (ProbeSetQuantificationDetectionData) data;
				quants[i] = pdata.getQuantification();
				pvals[i] = pdata.getPValue();
				has_pvals = true;
			} else {
				quants[i] = 0;
				pvals[i] = 0;
			}
		}
		return has_pvals;
	}

	private static Set<Das2Type> determineMatchedTypes(String chp_array_type, Das2VersionedSource vsource) {
		SynonymLookup lookup = SynonymLookup.getDefaultLookup();
		Set<Das2Type> matched_types = new HashSet<Das2Type>();
		Collection<String> chp_array_syns = lookup.getSynonyms(chp_array_type);
		if (chp_array_syns == null) {
			chp_array_syns = new ArrayList<String>();
			chp_array_syns.add(chp_array_type);
		}
		for (String synonym : chp_array_syns) {
			String lcsyn = synonym.toLowerCase();
			for (Das2Type type : vsource.getTypes().values()) {
				//  fix problem with name matching when name has a path prefix...
				String name = type.getName();
				String tname = name;
				int sindex = name.lastIndexOf("/");
				if (sindex >= 0) { tname = name.substring(sindex+1); }
				if (tname.startsWith(synonym) || tname.startsWith(lcsyn)) {
					matched_types.add(type);
				}
			}
		}
		return matched_types;
	}

	/**
	 *  syms should be one of:
	 *     EfficientProbesetSymA (for exon array probesets)
	 *     SingletonSymWithIntId (for exon array transcript_clusters, exon_clusters, PSRs)
	 *     ??? (for gene chips)
	 *     ??? (for genotyping chips)
	 */
	private static void addIdSyms(SeqSymmetry sym, List<SeqSymmetry> symlist) {
		if (sym instanceof IntId) {
			symlist.add(sym);
		} else if (sym.getID() != null) {
			symlist.add(sym);
		}
		// if SingletonSymWithIntId, recursively descend through children and add those with IDs
		if ((sym.getChildCount() > 0) && (sym instanceof SingletonSymWithIntId)) {
			for (int i = 0; i < sym.getChildCount(); i++) {
				SeqSymmetry child = sym.getChild(i);
				addIdSyms(child, symlist);
			}
		}
	}

}
