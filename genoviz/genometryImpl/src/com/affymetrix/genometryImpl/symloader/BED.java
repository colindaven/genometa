package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.Scored;
import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import com.affymetrix.genometryImpl.UcscBedSym;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.comparator.BioSeqComparator;
import com.affymetrix.genometryImpl.comparator.SeqSymMinComparator;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.TrackLineParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import java.net.URI;
import java.util.Map.Entry;

/**
 *
 * @author hiralv
 */
public class BED extends SymLoader{

	// Used later to allow bed files to be output as a supported format in the DAS/2 types query.
	private static final List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("bed");
	}

	private static final boolean DEBUG = false;
	private static final Pattern line_regex = Pattern.compile("\\s+");
	private static final Pattern comma_regex = Pattern.compile(",");

	private final List<SeqSymmetry> symlist = new ArrayList<SeqSymmetry>();
	private final Map<BioSeq,Map<String,SeqSymmetry>> seq2types = new HashMap<BioSeq,Map<String,SeqSymmetry>>();
	private boolean annotate_seq = true;
	private boolean create_container_annot = false;
	private String default_type = null;

	private final TrackLineParser track_line_parser = new TrackLineParser();

	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}

	public BED(URI uri, String featureName, AnnotatedSeqGroup group){
		super(uri, featureName, group);
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
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
	public List<SeqSymmetry> getGenome() {
		init();
		List<BioSeq> allSeq = getChromosomeList();
		List<SeqSymmetry> retList = new ArrayList<SeqSymmetry>();
		for(BioSeq seq : allSeq){
			retList.addAll(getChromosome(seq));
		}
		return retList;
	}


	@Override
	public List<SeqSymmetry> getChromosome(BioSeq seq) {
		init();
		return parse(seq,seq.getMin(),seq.getMax());
	}


	@Override
	public List<SeqSymmetry> getRegion(SeqSpan span) {
		init();
		return parse(span.getBioSeq(),span.getMin(),span.getMax());
	}

	private List<SeqSymmetry> parse(BioSeq seq, int min, int max){
		InputStream istr = null;
		try {
			File file = chrList.get(seq);
			boolean isSorted = chrSort.get(seq) == true;
			if (file == null) {
				Logger.getLogger(BED.class.getName()).log(Level.FINE, "Could not find chromosome {0}", seq.getID());
				return Collections.<SeqSymmetry>emptyList();
			}
			istr = new FileInputStream(file);
			return parse(istr, GenometryModel.getGenometryModel(), this.group, false, this.featureName, false, isSorted, min, max);
		}catch (Exception ex) {
			Logger.getLogger(BED.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(istr);
		}
		return Collections.<SeqSymmetry>emptyList();
	}

	private List<SeqSymmetry> parse(InputStream istr,  GenometryModel gmodel,
			AnnotatedSeqGroup group, boolean annot_seq,
			String stream_name, boolean create_container, boolean isSorted, int min, int max)
		throws IOException {
		if (DEBUG) {
			System.out.println("BED parser called, annotate seq: " + annot_seq +
					", create_container_annot: " + create_container);
		}
		/*
		 *  seq2types is hash for making container syms (if create_container_annot == true)
		 *  each entry in hash is: BioSeq ==> type2psym hash
		 *     Each type2csym is hash where each entry is "type" ==> container_sym
		 *  so two-step process to find container sym for a particular type on a particular seq:
		 *    Map type2csym = (Map)seq2types.get(seq);
		 *    MutableSeqSymmetry container_sym = (MutableSeqSymmetry)type2csym.get(type);
		 */
		seq2types.clear();
		symlist.clear();
		annotate_seq = annot_seq;
		this.create_container_annot = create_container;
		default_type = this.featureName;

		if (stream_name.endsWith(".bed")) {
			default_type = stream_name.substring(0, stream_name.lastIndexOf(".bed"));
		}
		BufferedInputStream bis;
		if (istr instanceof BufferedInputStream) {
			bis = (BufferedInputStream)istr;
		}
		else {
			bis = new BufferedInputStream(istr);
		}
		DataInputStream dis = new DataInputStream(bis);
		parse(dis, gmodel, group, default_type, isSorted, min, max);
		System.out.println("BED annot count: " + symlist.size());
		return symlist;
	}

	private void parse(DataInputStream dis, GenometryModel gmodel, AnnotatedSeqGroup seq_group, String default_type, boolean isSorted, int min, int max)
		throws IOException  {
		if (DEBUG) {
			System.out.println("called BedParser.parseWithEvents()");
		}
		String line;
		String type = default_type;
		boolean use_item_rgb = false;

		Thread thread = Thread.currentThread();
		BufferedReader reader = new BufferedReader(new InputStreamReader(dis));
		while ((line = reader.readLine()) != null && (! thread.isInterrupted())) {
			if (line.length() == 0) {
				continue;
			}
			char firstChar = line.charAt(0);
			if (firstChar == '#') {  // skip comment lines
				continue;
			}
			else if (firstChar == 't' && line.startsWith("track")) {
				track_line_parser.parseTrackLine(line);
				TrackLineParser.createTrackStyle(track_line_parser.getCurrentTrackHash(), default_type);
				type = track_line_parser.getCurrentTrackHash().get(TrackLineParser.NAME);
				String item_rgb_string = track_line_parser.getCurrentTrackHash().get(TrackLineParser.ITEM_RGB);
				use_item_rgb = "on".equalsIgnoreCase(item_rgb_string);
				continue;
			}
			else if (firstChar == 'b' && line.startsWith("browser")) {
				// currently take no action for browser lines
			}
			else {
				if (DEBUG) {
					System.out.println(line);
				}
				if(!parseLine(line, seq_group, gmodel, type, use_item_rgb, min, max) && isSorted){
					break;
				}
			}
		}
	}


	private boolean parseLine(String line, AnnotatedSeqGroup seq_group, GenometryModel gmodel, String type, boolean use_item_rgb, int minimum, int maximum)
			throws NumberFormatException, IOException {
		String[] fields = line_regex.split(line);
		int field_count = fields.length;
		if (field_count < 3) {
			return true;
		}

		String seq_name = null;
		String annot_name = null;
		int min;
		int max;
		String itemRgb = "";
		int thick_min = Integer.MIN_VALUE; // Integer.MIN_VALUE signifies that thick_min is not used
		int thick_max = Integer.MIN_VALUE; // Integer.MIN_VALUE signifies that thick_max is not used
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		boolean forward;
		int[] blockSizes = null;
		int[] blockStarts = null;
		int[] blockMins = null;
		int[] blockMaxs = null;
		boolean includes_bin_field = field_count > 6 && (fields[6].startsWith("+") || fields[6].startsWith("-") || fields[6].startsWith("."));
		int findex = 0;
		if (includes_bin_field) {
			findex++;
		}
		seq_name = fields[findex++]; // seq id field
		
		int beg = Integer.parseInt(fields[findex++]); // start field
		int end = Integer.parseInt(fields[findex++]); // stop field
		if (field_count >= 4) {
			annot_name = parseName(fields[findex++]);
		}
		if (field_count >= 5) {
			score = parseScore(fields[findex++]);
		} // score field
		if (field_count >= 6) {
			forward = !(fields[findex++].equals("-"));
		} else {
			forward = (beg <= end);
		}
		min = Math.min(beg, end);
		max = Math.max(beg, end);

		if(!checkRange(min,max,minimum,maximum)){
			if(max > maximum) 
				return false;
			
			return true;
		}
		
		BioSeq seq = seq_group.getSeq(seq_name);
		if ((seq == null) && (seq_name.indexOf(';') > -1)) {
			// if no seq found, try and split up seq_name by ";", in case it is in format
			//    "seqid;genome_version"
			String seqid = seq_name.substring(0, seq_name.indexOf(';'));
			String version = seq_name.substring(seq_name.indexOf(';') + 1);
			//            System.out.println("    seq = " + seqid + ", version = " + version);
			if ((gmodel.getSeqGroup(version) == seq_group) || seq_group.getID().equals(version)) {
				// for format [chrom_name];[genome_version]
				seq = seq_group.getSeq(seqid);
				if (seq != null) {
					seq_name = seqid;
				}
			} else if ((gmodel.getSeqGroup(seqid) == seq_group) || seq_group.getID().equals(seqid)) {
				// for format [genome_version];[chrom_name]
				String temp = seqid;
				seqid = version;
				version = temp;
				seq = seq_group.getSeq(seqid);
				if (seq != null) {
					seq_name = seqid;
				}
			}
		}
		if (seq == null) {
			//System.out.println("seq not recognized, creating new seq: " + seq_name);
			seq = seq_group.addSeq(seq_name, 0);
		}

		
		if (field_count >= 8) {
			thick_min = Integer.parseInt(fields[findex++]); // thickStart field
			thick_max = Integer.parseInt(fields[findex++]); // thickEnd field
		}
		if (field_count >= 9) {
			itemRgb = fields[findex++];
		} else {
			findex++;
		}
		if (field_count >= 12) {
			int blockCount = Integer.parseInt(fields[findex++]); // blockCount field
			blockSizes = parseIntArray(fields[findex++]); // blockSizes field
			if (blockCount != blockSizes.length) {
				System.out.println("WARNING: block count does not agree with block sizes.  Ignoring " + annot_name + " on " + seq_name);
				return true;
			}
			blockStarts = parseIntArray(fields[findex++]); // blockStarts field
			if (blockCount != blockStarts.length) {
				System.out.println("WARNING: block size does not agree with block starts.  Ignoring " + annot_name + " on " + seq_name);
				return true;
			}
			blockMins = makeBlockMins(min, blockStarts);
			blockMaxs = makeBlockMaxs(blockSizes, blockMins);
		} else {
			/*
			 * if no child blocks, make a single child block the same size as the parent
			 * Very Inefficient, ideally wouldn't do this
			 * But currently need this because of GenericAnnotGlyphFactory use of annotation depth to
			 *     determine at what level to connect glyphs -- if just leave blockMins/blockMaxs null (no children),
			 *     then factory will create a line container glyph to draw line connecting all the bed annots
			 * Maybe a way around this is to adjust depth preference based on overall depth (1 or 2) of bed file?
			 */
			blockMins = new int[1];
			blockMins[0] = min;
			blockMaxs = new int[1];
			blockMaxs[0] = max;
		}
		if (max > seq.getLength()) {
			seq.setLength(max);
		}
		if (DEBUG) {
			System.out.println("fields: " + field_count + ", type = " + type + ", seq = " + seq_name + ", min = " + min + ", max = " + max + ", name = " + annot_name + ", score = " + score + ", forward = " + forward + ", thickmin = " + thick_min + ", thickmax = " + thick_max);
			if (blockMins != null) {
				int count = blockMins.length;
				if (blockSizes != null && blockStarts != null && blockMins != null && blockMaxs != null) {
					for (int i = 0; i < count; i++) {
						System.out.println("   " + i + ": blockSize = " + blockSizes[i] + ", blockStart = " + blockStarts[i] + ", blockMin = " + blockMins[i] + ", blockMax = " + blockMaxs[i]);
					}
				}
			}
		}
		SymWithProps bedline_sym = null;
		bedline_sym = new UcscBedSym(type, seq, min, max, annot_name, score, forward, thick_min, thick_max, blockMins, blockMaxs);
		if (use_item_rgb && itemRgb != null) {
			java.awt.Color c = null;
			try {
				c = TrackLineParser.reformatColor(itemRgb);
			} catch (Exception e) {
				throw new IOException("Could not parse a color from String '" + itemRgb + "'");
			}
			if (c != null) {
				bedline_sym.setProperty(TrackLineParser.ITEM_RGB, c);
			}
		}
		symlist.add(bedline_sym);
		if (annotate_seq) {
			this.annotationParsed(bedline_sym);
		}
		if (annot_name != null) {
			seq_group.addToIndex(annot_name, bedline_sym);
		}

		return true;
	}


	/** Converts the data in the score field, if present, to a floating-point number. */
	private static float parseScore(String s) {
		return Float.parseFloat(s);
	}

	/**
	 * Parses the name field from the file.  Gene names are allowed to be non-unique.
	 * @param s
	 * @return annot_name
	 */
	private static String parseName(String s) {
		String annot_name = new String(s); // create a new String so the entire input line doesn't get preserved
		return annot_name;
	}

	private void annotationParsed(SeqSymmetry bedline_sym) {
		BioSeq seq = bedline_sym.getSpan(0).getBioSeq();
		if (create_container_annot) {
			String type = track_line_parser.getCurrentTrackHash().get(TrackLineParser.NAME);
			if (type == null) {
				type = default_type;
			}
			Map<String, SeqSymmetry> type2csym = seq2types.get(seq);
			if (type2csym == null) {
				type2csym = new HashMap<String, SeqSymmetry>();
				seq2types.put(seq, type2csym);
			}
			SimpleSymWithProps parent_sym = (SimpleSymWithProps) type2csym.get(type);
			if (parent_sym == null) {
				parent_sym = new SimpleSymWithProps();
				parent_sym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
				parent_sym.setProperty("method", type);
				parent_sym.setProperty("preferred_formats", pref_list);   // Used to indicate to DAS/2 server to support the formats in the pref_list.
				parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
				seq.addAnnotation(parent_sym);
				type2csym.put(type, parent_sym);
			}
			parent_sym.addChild(bedline_sym);
		} else {
			seq.addAnnotation(bedline_sym);
		}
	}


	public static int[] parseIntArray(String int_array) {
		String[] intstrings = comma_regex.split(int_array);
		int count = intstrings.length;
		int[] results = new int[count];
		for (int i=0; i<count; i++) {
			int val = Integer.parseInt(intstrings[i]);
			results[i] = val;
		}
		return results;
	}

	/**
	 *  Converting blockStarts to blockMins.
	 *  @param blockStarts  in coords relative to min of annotation
	 *  @return blockMins in coords relative to sequence that annotation is "on"
	 */
	public static int[] makeBlockMins(int min, int[] blockStarts) {
		int count = blockStarts.length;
		int[] blockMins = new int[count];
		for (int i=0; i<count; i++) {
			blockMins[i] = blockStarts[i] + min;
		}
		return blockMins;
	}

	public static int[] makeBlockMaxs(int[] blockMins, int[] blockSizes)  {
		int count = blockMins.length;
		int[] blockMaxs = new int[count];
		for (int i=0; i<count; i++) {
			blockMaxs[i] = blockMins[i] + blockSizes[i];
		}
		return blockMaxs;
	}


	public static void writeBedFormat(DataOutputStream out, List<SeqSymmetry> syms, BioSeq seq)
		throws IOException  {
		for (SeqSymmetry sym : syms) {
			writeSymmetry(out, sym, seq);
		}
	}

	/**
	 *  Writes bed file format.
	 *  WARNING. This currently assumes that each child symmetry contains
	 *     a span on the seq given as an argument.
	 */
	public static void writeSymmetry(DataOutputStream out, SeqSymmetry sym, BioSeq seq)
		throws IOException {
		if (DEBUG) {
			System.out.println("writing sym: " + sym);
		}
		SeqSpan span = sym.getSpan(seq);
		if (span == null) {
			return;
		}

		if (sym instanceof UcscBedSym) {
			UcscBedSym bedsym = (UcscBedSym) sym;
			if (seq == bedsym.getBioSeq()) {
				bedsym.outputBedFormat(out);
			}
			return;
		}

		SymWithProps propsym = null;
		if (sym instanceof SymWithProps) {
			propsym = (SymWithProps) sym;
		}

		writeOutFile(out, seq, span, sym, propsym);
	}


	private static void writeOutFile(DataOutputStream out, BioSeq seq, SeqSpan span, SeqSymmetry sym, SymWithProps propsym) throws IOException {
		out.write(seq.getID().getBytes());
		out.write('\t');
		int min = span.getMin();
		int max = span.getMax();
		out.write(Integer.toString(min).getBytes());
		out.write('\t');
		out.write(Integer.toString(max).getBytes());
		int childcount = sym.getChildCount();
		if ((!span.isForward()) || (childcount > 0) || (propsym != null)) {
			out.write('\t');
			if (propsym != null) {
				if (propsym.getProperty("name") != null) {
					out.write(((String) propsym.getProperty("name")).getBytes());
				} else if (propsym.getProperty("id") != null) {
					out.write(((String) propsym.getProperty("id")).getBytes());
				}
			}
			out.write('\t');
			if ((propsym != null) && (propsym.getProperty("score") != null)) {
				out.write(propsym.getProperty("score").toString().getBytes());
			} else if (sym instanceof Scored) {
				out.write(Float.toString(((Scored) sym).getScore()).getBytes());
			} else {
				out.write('0');
			}
			out.write('\t');
			if (span.isForward()) {
				out.write('+');
			} else {
				out.write('-');
			}
			if (childcount > 0) {
				writeOutChildren(out, propsym, min, max, childcount, sym, seq);
			}
		}
		out.write('\n');
	}



	private static void writeOutChildren(DataOutputStream out, SymWithProps propsym, int min, int max, int childcount, SeqSymmetry sym, BioSeq seq) throws IOException {
		out.write('\t');
		if ((propsym != null) && (propsym.getProperty("cds min") != null)) {
			out.write(propsym.getProperty("cds min").toString().getBytes());
		} else {
			out.write(Integer.toString(min).getBytes());
		}
		out.write('\t');
		if ((propsym != null) && (propsym.getProperty("cds max") != null)) {
			out.write(propsym.getProperty("cds max").toString().getBytes());
		} else {
			out.write(Integer.toString(max).getBytes());
		}
		out.write('\t');
		out.write('0');
		out.write('\t');
		out.write(Integer.toString(childcount).getBytes());
		out.write('\t');
		int[] blockSizes = new int[childcount];
		int[] blockStarts = new int[childcount];
		for (int i = 0; i < childcount; i++) {
			SeqSymmetry csym = sym.getChild(i);
			SeqSpan cspan = csym.getSpan(seq);
			blockSizes[i] = cspan.getLength();
			blockStarts[i] = cspan.getMin() - min;
		}
		for (int i = 0; i < childcount; i++) {
			out.write(Integer.toString(blockSizes[i]).getBytes());
			out.write(',');
		}
		out.write('\t');
		for (int i = 0; i < childcount; i++) {
			out.write(Integer.toString(blockStarts[i]).getBytes());
			out.write(',');
		}
	}



	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "BED" format.
	 **/
	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
			String type, OutputStream outstream) {
		if (DEBUG){
			System.out.println("in BedParser.writeAnnotations()");
		}
		DataOutputStream dos = null;
		try {
			dos = new DataOutputStream(new BufferedOutputStream(outstream));
			for (SeqSymmetry sym : syms) {
				writeSymmetry(dos, sym, seq);
			}
			dos.flush();
			return true;
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		return false;
	}

	public void writeSymmetry(SeqSymmetry sym, BioSeq seq, OutputStream os) throws IOException {
		DataOutputStream dos = null;
		if (os instanceof DataOutputStream) {
			dos = (DataOutputStream) os;
		} else {
			dos = new DataOutputStream(os);
		}
		BED.writeSymmetry(dos, sym, seq);
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

	@Override
	public List<String> getFormatPrefList() {
		return pref_list;
	}


	/** Returns "text/bed". */
	public String getMimeType() {
		return "text/bed";
	}

	private static boolean checkRange(int start, int end, int min, int max){

		//getChromosome && getRegion
		if(end < min || start > max){
			return false;
		}

		return true;
	}
	
	@Override
	protected boolean parseLines(InputStream istr, Map<String, Integer> chrLength, Map<String, File> chrFiles) {
		BufferedReader br = null;
		BufferedWriter bw = null;

		Map<String, Boolean> chrTrack = new HashMap<String, Boolean>();
		Map<String, BufferedWriter> chrs = new HashMap<String, BufferedWriter>();
		String line, trackLine = null, seq_name = null;
		String[] fields;

		try {
			Thread thread = Thread.currentThread();
			br = new BufferedReader(new InputStreamReader(istr));
			while ((line = br.readLine()) != null && (!thread.isInterrupted())) {

				if (line.length() == 0) {
					continue;
				}
				char firstChar = line.charAt(0);
				if (firstChar == '#') {  // skip comment lines
					continue;
				} else if (firstChar == 't' && line.startsWith("track")) {
					chrTrack = new HashMap<String, Boolean>();
					trackLine = line;
					continue;
				} else if (firstChar == 'b' && line.startsWith("browser")) {
					// currently take no action for browser lines
				} else {
					if (DEBUG) {
						System.out.println(line);
					}
					fields = line_regex.split(line);

					if (fields.length < 3) {
						Logger.getLogger(BED.class.getName()).severe("Invalid BED file");
						return false;
					}

					boolean includes_bin_field = false;

					if(fields.length > 6){
						firstChar = fields[6].charAt(0);
						includes_bin_field = (firstChar == '+' || firstChar == '-' || firstChar == '.');
					}

					int findex = 0;
					if (includes_bin_field) {
						findex++;
					}

					seq_name = fields[findex++]; // seq id field
					int beg = Integer.parseInt(fields[findex++]); // start field;
					int end = Integer.parseInt(fields[findex++]); // stop field
					int max = Math.max(beg, end);

					if (!chrs.containsKey(seq_name)) {
						addToLists(chrs, seq_name, chrFiles, chrLength, ".bed");
					}
					bw = chrs.get(seq_name);
					if (!chrTrack.containsKey(seq_name)) {
						chrTrack.put(seq_name, true);

						if(trackLine != null)
							bw.write(trackLine + "\n");
					}
					bw.write(line + "\n");

					if (max > chrLength.get(seq_name))
						chrLength.put(seq_name, max);

				}
			}

			return !thread.isInterrupted();

		} catch (IOException ex) {
			Logger.getLogger(BED.class.getName()).log(Level.SEVERE, null, ex);
		}finally{
			for (BufferedWriter b : chrs.values()) {
				GeneralUtils.safeClose(b);
			}
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(bw);
		}
		
		return false;
	}


	@Override
	protected void createResults(Map<String, Integer> chrLength, Map<String, File> chrFiles){
		GenometryModel gmodel = GenometryModel.getGenometryModel();
		for(Entry<String, Integer> bioseq : chrLength.entrySet()){
			String seq_name = bioseq.getKey();
			BioSeq seq = group.getSeq(seq_name);
			if ((seq == null) && (seq_name.indexOf(';') > -1)) {
				// if no seq found, try and split up seq_name by ";", in case it is in format
				//    "seqid;genome_version"
				String seqid = seq_name.substring(0, seq_name.indexOf(';'));
				String version = seq_name.substring(seq_name.indexOf(';') + 1);
				//            System.out.println("    seq = " + seqid + ", version = " + version);
				if ((gmodel.getSeqGroup(version) == group) || group.getID().equals(version)) {
					// for format [chrom_name];[genome_version]
					seq = group.getSeq(seqid);
					if (seq != null) {
						seq_name = seqid;
					}
				} else if ((gmodel.getSeqGroup(seqid) == group) || group.getID().equals(seqid)) {
					// for format [genome_version];[chrom_name]
					String temp = seqid;
					seqid = version;
					version = temp;
					seq = group.getSeq(seqid);
					if (seq != null) {
						seq_name = seqid;
					}
				}
			}
			if (seq == null) {
				//System.out.println("seq not recognized, creating new seq: " + seq_name);
				seq = group.addSeq(seq_name, bioseq.getValue());
			}
			
			chrList.put(seq, chrFiles.get(seq_name));
		}
	}

}
