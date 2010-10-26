package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.general.SymLoader;
import java.net.URI;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

import com.affymetrix.genometryImpl.parsers.AnnotationWriter;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.Psl3Sym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SeqSymmetryConverter;
import com.affymetrix.genometryImpl.comparator.BioSeqComparator;
import com.affymetrix.genometryImpl.comparator.UcscPslComparator;
import com.affymetrix.genometryImpl.parsers.TrackLineParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author hiralv
 */
public class PSL extends SymLoader implements AnnotationWriter, IndexWriter {

	private static final UcscPslComparator comp = new UcscPslComparator();
	static final List<String> psl_pref_list = Arrays.asList("psl");
	static final List<String> link_psl_pref_list = Arrays.asList("link.psl", "bps", "psl");
	static final List<String> psl3_pref_list = Arrays.asList("psl3", "bps", "psl");
	boolean look_for_targets_in_query_group = false;
	boolean create_container_annot = false;
	boolean is_link_psl = false;
	public static final boolean DEBUG = false;
	final static Pattern line_regex = Pattern.compile("\t");
	final static Pattern comma_regex = Pattern.compile(",");
	final static Pattern tagval_regex = Pattern.compile("=");
	final static Pattern non_digit = Pattern.compile("[^0-9-]");
	final TrackLineParser track_line_parser = new TrackLineParser();
	String track_name_prefix = null;
	private static final String newLine = System.getProperty("line.separator");	// system-independent newline

	private final AnnotatedSeqGroup query_group;
	private final AnnotatedSeqGroup target_group;
	private final AnnotatedSeqGroup other_group;
	private final boolean annotate_query;
	private final boolean annotate_target;
	private final boolean annotate_other;

	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}
	
	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	public PSL(URI uri, String featureName, AnnotatedSeqGroup target_group,
			AnnotatedSeqGroup query_group, AnnotatedSeqGroup other_group,
			boolean annotate_query, boolean annotate_target,
			boolean annotate_other){
		super(uri, featureName, target_group);
		this.target_group = target_group;
		this.query_group = query_group;
		this.other_group = other_group;
		this.annotate_query = annotate_query;
		this.annotate_target = annotate_target;
		this.annotate_other = annotate_other;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		
		if(buildIndex()){
			sortCreatedFiles();
			super.init();
		}
		
	}

	@Override
	public List<BioSeq> getChromosomeList(){
		init();
		List<BioSeq> chromosomeList = new ArrayList<BioSeq>(chrList.keySet());
		Collections.sort(chromosomeList,new BioSeqComparator());
		return chromosomeList;
	}

	@Override
	public List<UcscPslSym> getGenome() {
		init();
		List<BioSeq> allSeq = getChromosomeList();
		List<UcscPslSym> retList = new ArrayList<UcscPslSym>();
		for(BioSeq seq : allSeq){
			retList.addAll(getChromosome(seq));
		}
		return retList;
	}


	@Override
	public List<UcscPslSym> getChromosome(BioSeq seq) {
		init();
		return parse(seq, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}


	@Override
	public List<UcscPslSym> getRegion(SeqSpan span) {
		init();
		return parse(span.getBioSeq(), span.getMin(), span.getMax());
	}

	@Override
	protected boolean parseLines(InputStream istr, Map<String, Integer> chrLength, Map<String, File> chrFiles){

		BufferedWriter bw = null;
		BufferedReader br = null;
		Map<String, Boolean> chrTrack = new HashMap<String, Boolean>();
		Map<String, BufferedWriter> chrs = new HashMap<String, BufferedWriter>();
		Map<String, Set<String>> queryTarget = new HashMap<String, Set<String>>();
		String trackLine = null;
	
		if (DEBUG) {
			System.out.println("in PSL.parse(), create_container_annot: " + create_container_annot);
		}

		int line_count = 0, length = 0;
		String line = null, target_seq_id, query_seq_id;
		String[] fields = null;
		boolean in_bottom_of_link_psl = false;
		
		Thread thread = Thread.currentThread();
		try {
			br = new BufferedReader(new InputStreamReader(istr));
			
			while ((line = br.readLine()) != null && (!thread.isInterrupted())) {
				line_count++;
				// Ignore psl header lines
				if(line.trim().length() == 0)
					continue;
				char firstchar = line.charAt(0);

				if (firstchar == '#' ||
						(firstchar == 'm' && line.startsWith("match\t")) ||
						(firstchar == '-' && line.startsWith("-------"))) {
					continue;
				}

				if (firstchar == 't' && line.startsWith("track")) {
					if (is_link_psl) {
						Map<String,String> track_props = track_line_parser.parseTrackLine(line, track_name_prefix);
						String track_name = track_props.get(TrackLineParser.NAME);
						if (track_name != null && track_name.endsWith("probesets")) {
							in_bottom_of_link_psl = true;
						}
					}
					chrTrack = new HashMap<String, Boolean>();
					trackLine = line;

					if(in_bottom_of_link_psl){
						for(String seq_id : chrs.keySet()){
							bw = chrs.get(seq_id);
							bw.write(trackLine + "\n");
							bw.flush();
						}
					}
					continue;
				}
				
				fields = line_regex.split(line);
				// filtering out header lines (and any other line that doesn't start with a first field of all digits)
				String field0 = fields[0];
				boolean non_digits_present = non_digit.matcher(field0).find(0);
				if (non_digits_present) {
					continue;
				}

				int findex = 0;

				findex = skipExtraBinField(findex, fields);

				findex += 9;
				query_seq_id = fields[findex];

				findex += 4;
				target_seq_id = fields[findex];

				findex += 1;
				length = Integer.valueOf(fields[findex]);


				if(in_bottom_of_link_psl){
					Set<String> seq_ids = queryTarget.get(target_seq_id);
					
					if(seq_ids == null){
						Logger.getLogger(PSL.class.getName()).log(Level.INFO, 
								"Ignoring orphan target sequence {0} at line {1} "
								+ "in feature {2}",
								new Object[]{target_seq_id, line_count, featureName});
						continue;
					}

					for(String seq_id : seq_ids){
						bw = chrs.get(seq_id);
						bw.write(line + "\n");
					}
					continue;
				}
				
				addToQueryTarget(queryTarget, query_seq_id, target_seq_id);

				// Ignoring chromosome seqs after last track line. It also ignores
				// orphan chromosome seqs.
				if (!chrs.containsKey(target_seq_id)) {
					addToLists(chrs, target_seq_id, chrFiles, chrLength, is_link_psl ? ".link.psl" : ".psl");
				}

				bw = chrs.get(target_seq_id);
		
				if (!chrTrack.containsKey(target_seq_id) && trackLine != null) {
					chrTrack.put(target_seq_id, true);
					bw.write(trackLine + "\n");
				}

				bw.write(line + "\n");
				if (length > chrLength.get(target_seq_id)) {
					chrLength.put(target_seq_id, length);
				}
			}

			return !thread.isInterrupted();
		} catch (Exception e) {
			StringBuilder sb = new StringBuilder();
			sb.append("Error parsing PSL file\n");
			sb.append("line count: ").append(line_count).append("\n");
		} finally {
			for (BufferedWriter b : chrs.values()) {
				try {
					b.flush();
				} catch (IOException ex) {
					Logger.getLogger(PSL.class.getName()).log(Level.SEVERE, null, ex);
				}
				GeneralUtils.safeClose(b);
			}
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(bw);
		}
		return false;
	}

	private static void addToQueryTarget(Map<String, Set<String>> queryTarget, String query_seq_id, String target_seq_id){

		if(!queryTarget.containsKey(query_seq_id)){
			queryTarget.put(query_seq_id, new HashSet<String>());
		}

		Set<String> set = queryTarget.get(query_seq_id);
		set.add(target_seq_id);
	}

	public void enableSharedQueryTarget(boolean b) {
		look_for_targets_in_query_group = b;
	}

	public void setCreateContainerAnnot(boolean b) {
		create_container_annot = b;
	}

	/**
	 *  Whether or not to add new seqs from the file to the target AnnotatedSeqGroup.
	 *  Normally false; set this to true for "link.psl" files.
	 */
	public void setIsLinkPsl(boolean b) {
		is_link_psl = b;
	}

	public List<UcscPslSym> parse(InputStream istr, String annot_type,
			AnnotatedSeqGroup query_group, AnnotatedSeqGroup target_group,
			boolean annotate_query, boolean annotate_target) throws IOException {
		return parse(istr, Integer.MIN_VALUE, Integer.MAX_VALUE, annot_type,
				query_group, target_group, null, annotate_query, annotate_target,
				false);
	}

	private List<UcscPslSym> parse(BioSeq seq, int min, int max){
		InputStream istr = null;
		try {
			File file = chrList.get(seq);
			if (file == null) {
				Logger.getLogger(Wiggle.class.getName()).log(Level.FINE, "Could not find chromosome {0}", seq.getID());
				return Collections.<UcscPslSym>emptyList();
			}
			istr = new FileInputStream(file);
			return parse(istr, min, max, featureName, query_group, target_group,
					other_group, annotate_query, annotate_target, annotate_other);

		} catch (FileNotFoundException ex) {
			Logger.getLogger(PSL.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(istr);
		}
		return Collections.<UcscPslSym>emptyList();
	}

	/**
	 *  Parse.
	 *  The most common parameters are:
	 *     annotate_query = false;
	 *     annotate_target = true;
	 *     annotate_other = false.
	 *
	 *  @param istr             An input stream
	 *  @param annot_type       The method name for the annotation to load from the file, if the track line is missing;
	 *                          if there is a track line in the file, the name from the track line will be used instead.
	 *  @param query_group      An AnnotatedSeqGroup (or null) to look for query SeqSymmetries in and add SeqSymmetries to.
	 *                          Null is ok; this will cause a temporary AnnotatedSeqGroup to be created.
	 *  @param target_group     An AnnotatedSeqGroup (or null) to look for target SeqSymmetries in and add SeqSymmetries to.
	 *  @param other_group      An AnnotatedSeqGroup (or null) to look for other SeqSymmetries in (in PSL3 format) and add SeqSymmetries to.
	 *                          This parameter is ignored if the file is not in psl3 format.
	 *  @param annotate_query   if true, then alignment SeqSymmetries are added to query seq as annotations
	 *  @param annotate_target  if true, then alignment SeqSymmetries are added to target seq as annotations
	 *  @param annotate_other   if true, then alignment SeqSymmetries (in PSL3 format files) are added to other seq as annotations
	 *
	 */
	private List<UcscPslSym> parse(InputStream istr, int min, int max, String annot_type,
			AnnotatedSeqGroup query_group, AnnotatedSeqGroup target_group, AnnotatedSeqGroup other_group,
			boolean annotate_query, boolean annotate_target, boolean annotate_other){

		if (DEBUG) {
			System.out.println("in PSL.parse(), create_container_annot: " + create_container_annot);
		}
		List<UcscPslSym> results = new ArrayList<UcscPslSym>();

		// Make temporary seq groups for any unspecified group.
		// These temporary groups do not require synonym matching, because they should
		// only refer to sequences from a single file.
		if (query_group == null) {
			query_group = new AnnotatedSeqGroup("Query");
			query_group.setUseSynonyms(false);
		}
		if (target_group == null) {
			target_group = new AnnotatedSeqGroup("Target");
			target_group.setUseSynonyms(false);
		}
		if (other_group == null) {
			other_group = new AnnotatedSeqGroup("Other");
			other_group.setUseSynonyms(false);
		}

		boolean in_bottom_of_link_psl = false;

		// the three xxx2types Maps accommodate using create_container_annot and psl with track lines.
		Map<BioSeq, Map<String, SimpleSymWithProps>> target2types = new HashMap<BioSeq, Map<String, SimpleSymWithProps>>();
		Map<BioSeq, Map<String, SimpleSymWithProps>> query2types = new HashMap<BioSeq, Map<String, SimpleSymWithProps>>();
		Map<BioSeq, Map<String, SimpleSymWithProps>> other2types = new HashMap<BioSeq, Map<String, SimpleSymWithProps>>();

		int line_count = 0;
		BufferedReader br = null;
		String line = null;
		int childcount = 0;
		int total_annot_count = 0;
		int total_child_count = 0;
		String[] block_size_array = null;
		Thread thread = Thread.currentThread();
		try {
			br = new BufferedReader(new InputStreamReader(istr));
			while ((line = br.readLine()) != null && (!thread.isInterrupted())) {
				line_count++;
				// Ignore psl header lines
				if(line.trim().length() == 0)
					continue;
				char firstchar = line.charAt(0);

				if (firstchar == '#' ||
						(firstchar == 'm' && line.startsWith("match\t")) ||
						(firstchar == '-' && line.startsWith("-------"))) {
					continue;
				}

				if (firstchar == 't' && line.startsWith("track")) {
					// Always parse the track line, but
					// only set the AnnotStyle properties from it
					// if this is NOT a ".link.psl" file.
					if (is_link_psl) {
						Map<String,String> track_props = track_line_parser.parseTrackLine(line, track_name_prefix);
						String track_name = track_props.get(TrackLineParser.NAME);
						if (track_name != null && track_name.endsWith("probesets")) {
							in_bottom_of_link_psl = true;
						}
					} else {
						track_line_parser.parseTrackLine(line, track_name_prefix);
						TrackLineParser.createTrackStyle(track_line_parser.getCurrentTrackHash(), annot_type);
					}
					// You can later get the track properties with getCurrentTrackHash();
					continue;
				}
				String[] fields = line_regex.split(line);
				// filtering out header lines (and any other line that doesn't start with a first field of all digits)
				String field0 = fields[0];
				boolean non_digits_present = non_digit.matcher(field0).find(0);
				if (non_digits_present) {
					continue;
				}

				int findex = 0;

				findex = skipExtraBinField(findex, fields);

				int match = Integer.parseInt(fields[findex++]);
				int mismatch = Integer.parseInt(fields[findex++]);
				int repmatch = Integer.parseInt(fields[findex++]);
				int n_count = Integer.parseInt(fields[findex++]);
				int q_gap_count = Integer.parseInt(fields[findex++]);
				int q_gap_bases = Integer.parseInt(fields[findex++]);
				int t_gap_count = Integer.parseInt(fields[findex++]);
				int t_gap_bases = Integer.parseInt(fields[findex++]);
				String strandstring = fields[findex++];
				boolean same_orientation = true;
				boolean qforward = true;
				boolean tforward = true;
				if (strandstring.length() == 1) {
					same_orientation = strandstring.equals("+");
					qforward = (strandstring.charAt(0) == '+');
					tforward = true;
				} else if (strandstring.length() == 2) {
					// need to deal with cases (as mentioned in PSL docs) where
					//    strand field is "++", "+-", "-+", "--"
					//  (where first char indicates strand of query, and second is strand for ? [target??]
					//  for now, just call strand based on them being different,
					//   so "++" OR "--" ==> forward
					//      "+-" OR "-+" ==> reverse
					// current implentation assumes "++", "--", "+-", "-+" are the only possibilities
					same_orientation = (strandstring.equals("++") || strandstring.equals("--"));
					qforward = (strandstring.charAt(0) == '+');
					tforward = (strandstring.charAt(1) == '+');
				} else {
					System.err.println("strand field longer than two characters! ==> " + strandstring);
				}

				String qname = fields[findex++];
				int qsize = Integer.parseInt(fields[findex++]);
				int qmin = Integer.parseInt(fields[findex++]);
				int qmax = Integer.parseInt(fields[findex++]);
				String tname = fields[findex++];
				int tsize = Integer.parseInt(fields[findex++]);
				int tmin = Integer.parseInt(fields[findex++]);
				int tmax = Integer.parseInt(fields[findex++]);
				int maximum = Math.max(tmin, tmax);
				int minimum = Math.min(tmin, tmax);

				if((maximum < min || minimum > max) && !is_link_psl)
					continue;
				
				int blockcount = Integer.parseInt(fields[findex++]);

				block_size_array = comma_regex.split(fields[findex++]);
				String[] q_start_array = comma_regex.split(fields[findex++]);
				String[] t_start_array = comma_regex.split(fields[findex++]);
				childcount = block_size_array.length;

				// skipping entries that have problems with block_size_array
				if ((block_size_array.length == 0) ||
						(block_size_array[0] == null) ||
						(block_size_array[0].length() == 0)) {
					System.err.println("PSL found problem with blockSizes list, skipping this line: ");
					System.err.println(line);
					continue;
				}
				if (blockcount != block_size_array.length) {
					System.err.println("PSL found disagreement over number of blocks, skipping this line: ");
					System.err.println(line);
					continue;
				}

				// Main method to determine the symmetry
				UcscPslSym sym = determineSym(
						query_group, qname, qsize, target_group, tname, in_bottom_of_link_psl, tsize, qforward, tforward, block_size_array, q_start_array, t_start_array, annot_type, fields, findex, childcount, other_group, match, mismatch, repmatch, n_count, q_gap_count, q_gap_bases, t_gap_count, t_gap_bases, same_orientation, qmin, qmax, tmin, tmax, blockcount, annotate_other, other2types, annotate_query, query2types, annotate_target, target2types);
		
				total_annot_count++;
				total_child_count += sym.getChildCount();
				results.add(sym);
				if (DEBUG) {
					if (total_annot_count % 5000 == 0) {
						System.out.println("current annot count: " + total_annot_count);
					}
				}
			}
		} catch (Exception e) {
			StringBuilder sb = new StringBuilder();
			sb.append("Error parsing PSL file\n");
			sb.append("line count: ").append(line_count).append("\n");
			sb.append("child count: ").append(childcount).append("\n");
			if (block_size_array != null && block_size_array.length != 0) {
				sb.append("block_size first element: **").append(block_size_array[0]).append("**\n");
			}

		} finally {
			GeneralUtils.safeClose(br);
		}
		if (DEBUG) {
			System.out.println("finished parsing PSL file, annot count: " + total_annot_count +
					", child count: " + total_child_count);
		}

		return results;
	}

	private static int skipExtraBinField(int findex, String[] fields) {
		/*
		 *  includes_bin_field is so PSL can serve double duty:
		 *  1. for standard PSL files (includes_bin_field = false)
		 *  2. for UCSC PSL-like dump from database, where format has extra ushort field at beginning
		 *       that is used to speed up indexing in db (includes_bin_field = true)
		 */
		//        if (line_count < 3) { System.out.println("# of fields: " + fields.length); }
		// trying to determine if there's an extra bin field at beginning of PSL line...
		//   for normal PSL, orientation field is
		
		if(fields.length > 9){
			char firstchar = fields[9].charAt(0);
			if (firstchar == '+' || firstchar == '-') {
				findex++;
			} // skip bin field at beginning if present
		}
		
		return findex;
	}

	private static BioSeq determineSeq(AnnotatedSeqGroup query_group, String qname, int qsize) {
		BioSeq qseq = query_group.getSeq(qname);
		if (qseq == null) {
			// Doing a new String() here gives a > 4X reduction in
			//    memory requirements!  Possible reason: Regex machinery when it splits a String into
			//    an array of Strings may use same underlying character array, so essentially
			//    end up holding a pointer to a character array containing the whole input file ???
			//
			qseq = query_group.addSeq(new String(qname), qsize);
		}
		if (qseq.getLength() < qsize) {
			qseq.setLength(qsize);
		}
		return qseq;
	}

	private UcscPslSym determineSym(
			AnnotatedSeqGroup query_group, String qname, int qsize, 
			AnnotatedSeqGroup target_group, String tname, boolean in_bottom_of_link_psl,
			int tsize, boolean qforward, boolean tforward, String[] block_size_array,
			String[] q_start_array, String[] t_start_array, String annot_type,
			String[] fields, int findex, int childcount, AnnotatedSeqGroup other_group,
			int match, int mismatch, int repmatch, int n_count, int q_gap_count,
			int q_gap_bases, int t_gap_count, int t_gap_bases, boolean same_orientation,
			int qmin, int qmax, int tmin, int tmax, int blockcount, boolean annotate_other,
			Map<BioSeq, Map<String, SimpleSymWithProps>> other2types, boolean annotate_query,
			Map<BioSeq, Map<String, SimpleSymWithProps>> query2types, boolean annotate_target,
			Map<BioSeq, Map<String, SimpleSymWithProps>> target2types)
			throws NumberFormatException {
		BioSeq qseq = determineSeq(query_group, qname, qsize);
		BioSeq tseq = target_group.getSeq(tname);
		boolean shared_query_target = false;
		if (tseq == null) {
			if (look_for_targets_in_query_group && (query_group.getSeq(tname) != null)) {
				tseq = query_group.getSeq(tname);
				shared_query_target = true;
			} else {
				if (look_for_targets_in_query_group && is_link_psl) {
					// If we are in the bottom section of a ".link.psl" file,
					// then add sequences only to the query sequence, never the target sequence.
					if (in_bottom_of_link_psl) {
						tseq = query_group.addSeq(new String(tname), qsize);
					} else {
						tseq = target_group.addSeq(new String(tname), qsize);
					}
				} else {
					tseq = target_group.addSeq(new String(tname), qsize);
				}
			}
		}
		if (tseq.getLength() < tsize) {
			tseq.setLength(tsize);
		}
		List<Object> child_arrays = calcChildren(qseq, tseq, qforward, tforward, block_size_array, q_start_array, t_start_array);
		int[] blocksizes = (int[]) child_arrays.get(0);
		int[] qmins = (int[]) child_arrays.get(1);
		int[] tmins = (int[]) child_arrays.get(2);
		String type = track_line_parser.getCurrentTrackHash().get(TrackLineParser.NAME);
		if (type == null) {
			type = annot_type;
		}
		UcscPslSym sym = null;
		// a "+" or "-" in first field after tmins indicates that it's a Psl3 format
		boolean is_psl3 = fields.length > findex && (fields[findex].equals("+") || fields[findex].equals("-"));
		// trying to handle parsing of extended PSL format for three sequence alignment
		//     (putting into a Psl3Sym)
		// extra fields (immediately after tmins), based on Psl3Sym.outputPsl3Format:
		// same_other_orientation  otherseq_id  otherseq_length  other_min other_max omins
		//    (but omins doesn't have weirdness that qmins/tmins does when orientation = "-")
		if (is_psl3) {
			String otherstrand_string = fields[findex++];
			boolean other_same_orientation = otherstrand_string.equals("+");
			String oname = fields[findex++];
			int osize = Integer.parseInt(fields[findex++]);
			int omin = Integer.parseInt(fields[findex++]);
			int omax = Integer.parseInt(fields[findex++]);
			String[] o_min_array = comma_regex.split(fields[findex++]);
			int[] omins = new int[childcount];
			for (int i = 0; i < childcount; i++) {
				omins[i] = Integer.parseInt(o_min_array[i]);
			}
			BioSeq oseq = determineSeq(other_group,oname,osize);

			sym = new Psl3Sym(type, match, mismatch, repmatch, n_count, q_gap_count, q_gap_bases, t_gap_count, t_gap_bases, same_orientation, other_same_orientation, qseq, qmin, qmax, tseq, tmin, tmax, oseq, omin, omax, blockcount, blocksizes, qmins, tmins, omins);
			annotate(annotate_other, create_container_annot, is_link_psl, other2types, oseq, type, sym, is_psl3, other_group);
		} else {
			boolean is_pslx = false;
			String[] query_res_arr = null;
			String[] target_res_arr = null;
			if (fields.length >= findex+2) {
				// see if there are two extra fields with residues for each block
				query_res_arr = comma_regex.split(fields[findex++]);
				target_res_arr = comma_regex.split(fields[findex++]);
			}
			sym = new UcscPslSym(type, match, mismatch, repmatch, n_count, q_gap_count, q_gap_bases,
					t_gap_count, t_gap_bases, same_orientation,
					qseq, qmin, qmax,
					tseq, tmin, tmax, target_res_arr,
					blockcount, blocksizes, qmins, tmins);
		}

		findExtraTagValues(fields, findex, sym);

		annotate(annotate_query, create_container_annot, is_link_psl, query2types, qseq, type, sym, is_psl3, query_group);
		annotateTarget(annotate_target || (shared_query_target && is_link_psl), create_container_annot, is_link_psl, target2types, tseq, type, sym, is_psl3, in_bottom_of_link_psl, target_group);

		return sym;
	}

	// looking for extra tag-value fields at end of line
	private static void findExtraTagValues(String[] fields, int findex, UcscPslSym sym) {
		if (fields.length > findex) {
			for (int i = findex; i < fields.length; i++) {
				String field = fields[i];
				String[] tagval = tagval_regex.split(field);
				if (tagval.length >= 2) {
					String tag = tagval[0];
					String val = tagval[1];
					sym.setProperty(tag, val);
				}
			}
		}
	}

	private static void annotate(
			boolean annotate, boolean create_container_annot, boolean is_link_psl, Map<BioSeq, Map<String, SimpleSymWithProps>> str2types, BioSeq seq, String type, UcscPslSym sym, boolean is_psl3, AnnotatedSeqGroup annGroup) {
		if (annotate) {
			if (create_container_annot) {
				createContainerAnnot(str2types, seq, type, sym, is_psl3, is_link_psl);
			} else {
				seq.addAnnotation(sym);
			}
			annGroup.addToIndex(sym.getID(), sym);
		}
	}

	private static void annotateTarget(
			boolean annotate, boolean create_container_annot, boolean is_link_psl, Map<BioSeq, Map<String, SimpleSymWithProps>> str2types, BioSeq seq, String type, UcscPslSym sym, boolean is_psl3, boolean in_bottom_of_link_psl, AnnotatedSeqGroup annGroup) {
		if (annotate) {
			// force annotation of target if query and target are shared and file is ".link.psl" format
			if (create_container_annot) {
				createContainerAnnot(str2types, seq, type, sym, is_psl3, is_link_psl);
			} else {
				seq.addAnnotation(sym);
			}
			if (!in_bottom_of_link_psl) {
				annGroup.addToIndex(sym.getID(), sym);
			}
		}
	}

	private static void createContainerAnnot(
			Map<BioSeq, Map<String, SimpleSymWithProps>> seq2types, BioSeq seq, String type, SeqSymmetry sym, boolean is_psl3, boolean is_link) {
		//    If using a container sym, need to first hash (seq2types) from
		//    seq to another hash (type2csym) of types to container sym
		//    System.out.println("in createContainerAnnot, type: " + type);
		Map<String, SimpleSymWithProps> type2csym = seq2types.get(seq);
		if (type2csym == null) {
			type2csym = new HashMap<String, SimpleSymWithProps>();
			seq2types.put(seq, type2csym);
		}
		SimpleSymWithProps parent_sym = type2csym.get(type);
		if (parent_sym == null) {
			parent_sym = new SimpleSymWithProps();
			parent_sym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
			parent_sym.setProperty("method", type);
			if (is_link) {
				parent_sym.setProperty("preferred_formats", link_psl_pref_list);
			} else if (is_psl3) {
				parent_sym.setProperty("preferred_formats", psl3_pref_list);
			} else {
				parent_sym.setProperty("preferred_formats", psl_pref_list);
			}
			parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
			seq.addAnnotation(parent_sym);
			type2csym.put(type, parent_sym);
		}
		parent_sym.addChild(sym);
	}

	private static List<Object> calcChildren(BioSeq qseq, BioSeq tseq, boolean qforward, boolean tforward,
			String[] blocksize_strings,
			String[] qstart_strings, String[] tstart_strings) {
		int childCount = blocksize_strings.length;
		if (qstart_strings.length != childCount || tstart_strings.length != childCount) {
			System.out.println("array counts for block sizes, q starts, and t starts don't agree, " +
					"skipping children");
			return null;
		}
		int[] blocksizes = new int[childCount];
		int[] qmins = new int[childCount];
		int[] tmins = new int[childCount];

		if (childCount > 0) {
			int qseq_length = qseq.getLength();
			int tseq_length = tseq.getLength();

			if (qforward && tforward) { // query = forward, target = forward
				for (int i = 0; i < childCount; i++) {
					int match_length = Integer.parseInt(blocksize_strings[i]);
					int qstart = Integer.parseInt(qstart_strings[i]);
					int tstart = Integer.parseInt(tstart_strings[i]);
					blocksizes[i] = match_length;
					qmins[i] = qstart;
					tmins[i] = tstart;
				}
			} else if ((!qforward) && (tforward)) { // query = reverse, target = forward
				for (int i = 0; i < childCount; i++) {
					int string_index = childCount - i - 1;
					int match_length = Integer.parseInt(blocksize_strings[string_index]);
					int qstart = qseq_length - Integer.parseInt(qstart_strings[string_index]);
					int tstart = Integer.parseInt(tstart_strings[string_index]);
					int qend = qstart - match_length;
					blocksizes[i] = match_length;
					qmins[i] = qend;
					tmins[i] = tstart;
				}
			} else if ((qforward) && (!tforward)) {  // query = forward, target = reverse
				for (int i = 0; i < childCount; i++) {
					int match_length = Integer.parseInt(blocksize_strings[i]);
					int qstart = Integer.parseInt(qstart_strings[i]);
					int tstart = tseq_length - Integer.parseInt(tstart_strings[i]);
					int tend = tstart - match_length;
					blocksizes[i] = match_length;
					qmins[i] = qstart;
					tmins[i] = tend;
				}
			} else { // query = reverse, target = reverse
				for (int i = 0; i < childCount; i++) {
					int string_index = childCount - i - 1;
					int match_length = Integer.parseInt(blocksize_strings[string_index]);
					int qstart = qseq_length - Integer.parseInt(qstart_strings[string_index]);
					int tstart = tseq_length - Integer.parseInt(tstart_strings[string_index]);
					int qend = qstart - match_length;
					int tend = tstart - match_length;
					blocksizes[i] = match_length;
					qmins[i] = qend;
					tmins[i] = tend;
				}
			}
		}
		List<Object> results = new ArrayList<Object>(3);
		results.add(blocksizes);
		results.add(qmins);
		results.add(tmins);
		return results;
	}

	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "PSL" format
	 **/
	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
			String type, OutputStream outstream) {
		return writeAnnotations(syms, seq, false, type, null, outstream);
	}

	/**
	 *  This version of the method is able to write out track lines
	 **/
	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
			boolean writeTrackLines, String type,
			String description, OutputStream outstream) {

		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(outstream));

			if (writeTrackLines) {
				dos.write(trackLine(type,description).getBytes());
			}

			for (SeqSymmetry sym : syms) {

				if(Thread.currentThread().isInterrupted())
					break;

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
		return true;
	}


	public static String trackLine(String type, String description) {
		String trackLine = "track";
		if (type != null) {
			trackLine += " name=\"" + type + "\"";
		}
		if (description != null) {
			trackLine += " description=\"" + description + "\"";
		}
		trackLine += newLine;
		return trackLine;
	}


	public Comparator<UcscPslSym> getComparator(BioSeq seq) {
		return comp;
	}

	public void writeSymmetry(SeqSymmetry sym, BioSeq seq, OutputStream os) throws IOException {
		DataOutputStream dos = null;
		if (os instanceof DataOutputStream) {
			dos = (DataOutputStream) os;
		} else {
			dos = new DataOutputStream(os);
		}
		((UcscPslSym) sym).outputPslFormat(dos);
	}

	public int getMin(SeqSymmetry sym, BioSeq seq) {
		return ((UcscPslSym) sym).getTargetMin();
	}

	public int getMax(SeqSymmetry sym, BioSeq seq) {
		return ((UcscPslSym) sym).getTargetMax();
	}

	@Override
	public List<String> getFormatPrefList() {
		if (is_link_psl) {
			return PSL.link_psl_pref_list;
		}
		return PSL.psl_pref_list;
	}

	public List<UcscPslSym> parse(DataInputStream dis, String annot_type, AnnotatedSeqGroup group) {
		return parse(dis, Integer.MIN_VALUE, Integer.MAX_VALUE, annot_type, null, group, null, false, false, false);
	}

	public void setTrackNamePrefix(String prefix) {
		track_name_prefix = prefix;
	}

	public String getTrackNamePrefix() {
		return track_name_prefix;
	}

	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "PSL" format
	 **/
	public String getMimeType() {
		return "text/plain";
	}

}
