package com.affymetrix.genometryImpl.symloader;

/**
 * Genbank parser
 * Adapted from Apollo Genbank parser
 */

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenbankSym;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.comparator.BioSeqComparator;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;

import java.net.*;
import java.util.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Genbank extends SymLoader {

	private static final String NONAME = "no name";	// gene with no name

	/**
	 *  This hash table contains the GENBANK start of line keywords (LOCUS,
	 *  DEFINITION, FEATURES and the EMBL start of lines that they correspond to)
	 **/
	private static final Map<String, String> genbank_hash = new HashMap<String, String>();

	static {
		genbank_hash.put("LOCUS", "ID");
		genbank_hash.put("DEFINITION", "DE");
		genbank_hash.put("ACCESSION", "AC");
		genbank_hash.put("NID", "NID");
		genbank_hash.put("VERSION", "DT");
		genbank_hash.put("KEYWORDS", "KW");
		genbank_hash.put("SOURCE", "OS");
		genbank_hash.put("ORGANISM", "OC");
		genbank_hash.put("REFERENCE", "RP");
		genbank_hash.put("AUTHORS", "RA");
		genbank_hash.put("TITLE", "RT");
		genbank_hash.put("JOURNAL", "RL");
		genbank_hash.put("PUBMED", "RK");
		genbank_hash.put("MEDLINE", "RK");
		genbank_hash.put("COMMENT", "CC");
		genbank_hash.put("FEATURES", "FH");
		genbank_hash.put("source", "FT");
		genbank_hash.put("BASE", "SQ");
		genbank_hash.put("ORIGIN", "SQ");
	}
	/** There are absolutely no mandatory qualifiers for an
	mRNA feature so that makes finding an appropriate value
	to use as an ID/Name a bit problematic. Do the best
	we can and if that fails then make something up. This
	method however, returns null if there are no ID-able
	qualifiers found.
	Optional qualifiers
	/allele="text" (and in gene)
	/citation=[number] (and in gene)
	/db_xref="<database>:<identifier>"  (and in gene)
	/evidence=<evidence_value>              (and in gene)
	/function="text" (and in gene)
	/gene="text" (and in gene)
	/label=feature_label (and in gene)
	/locus_tag="text" (single token) (and in gene)
	/map="text" (and in gene)
	/note="text" (and in gene)
	/operon="text" (and in gene)
	/product="text" (and in gene)
	/pseudo (and in gene)
	/standard_name="text"
	/usedin=accnum:feature_label
	/codon_start=int
	 */
	private static final List<String> transcript_id_tags = new ArrayList<String>();
	private static final List<String> annot_id_tags = new ArrayList<String>();
	private static final List<String> annot_name_tags = new ArrayList<String>();

	static {
		transcript_id_tags.add("product");
		transcript_id_tags.add("transcript_id");
		transcript_id_tags.add("protein_id");
		transcript_id_tags.add("codon_start"); // ?
	}

	static {
		annot_id_tags.add("locus_tag");
		annot_id_tags.add("transposon");
	}

	static {
		annot_name_tags.add("gene");
		annot_name_tags.add("standard_name");
		annot_name_tags.add("allele");
		annot_name_tags.add("label");
		annot_name_tags.add("note");
	}
	private static final Map<String, String> featureTag_hash = new HashMap<String, String>();

	static {
		featureTag_hash.put("map", "cyto_range");
		featureTag_hash.put("RBS", "ribosomal_binding_site");
		featureTag_hash.put("transposon", "transposable_element");
		featureTag_hash.put("misc_RNA", "ncRNA");
	}
	/**
	 *  The tag for the end of entry line: "//"
	 **/
	private final static int BEGINNING_OF_ENTRY = 0;
	private final static String BEGINNING_OF_ENTRY_STRING = "ID";
	/**
	 *  The tag for the end of entry line: "//"
	 **/
	private final static int END_OF_ENTRY = 1;
	private final static String END_OF_ENTRY_STRING = "//";
	/**
	 *  The tag for the start of sequence line
	 **/
	private final static int SEQUENCE = 2;
	private final static String SEQUENCE_STRING = "SQ";
	/**
	 *  The tag for EMBL feature table lines
	 **/
	private final static int FEATURE = 3;
	private final static String FEATURE_STRING = "FT";
	/**
	 *  The tag for EMBL feature header lines (FH ...)
	 **/
	private final static int FEATURE_HEADER = 4;
	private final static String FEATURE_HEADER_STRING = "FH";
	/**
	 *  The tag for EMBL definition lines
	 **/
	private final static int DEFINITION = 5;
	private final static String DEFINITION_STRING = "DE";
	/**
	 *  The tag for EMBL accession lines
	 **/
	private final static int ACCESSION = 6;
	private final static String ACCESSION_STRING = "AC";
	/**
	 *  The tag for EMBL organism of source lines
	 **/
	private final static int ORGANISM = 7;
	private final static String ORGANISM_STRING = "OS";
	/**
	 *  The tag for EMBL cross-references
	 **/
	private final static int REF_KEY = 8;
	private final static String REF_KEY_STRING = "RK";
	/**
	 *  The tag used for unidentified input. Which is kept, but not
	 *  distinguished
	 **/
	private final static int MISC = 9;
	/**
	 *  The column of the output where we should start writing the location.
	 *  e.g. for EMBL the 's' in source is the 5th character in the line
	FT   source
	 *  e.g. for Genbank the 'O' in On is the 12th character in the line
	COMMENT     On Sep 18, 2002 this sequence version replaced gi:10727164.
	 **/
	private final static int EMBL_CONTENT_OFFSET = 5;
	private final static int GENBANK_CONTENT_OFFSET = 12;
	// -----------------------------------------------------------------------
	// Instance variables
	// -----------------------------------------------------------------------
	/**
	 * The column to use for the content of a row file
	 */
	private int content_offset;
	private int line_number = 0;
	private String current_line;
	private String current_content;
	private String current_locus = "";
	private int current_line_type;
	private BioSeq currentSeq = null;




	public Genbank(URI uri, String featureName, AnnotatedSeqGroup seq_group) {
		super(uri, featureName, seq_group);
	}


	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}

	/**
	 * Return possible strategies to load this URI.
	 * @return
	 */
	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}


	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		super.init();

		// TODO: Hack to get chromosomes, which is currently done by parsing the entire file.
		List<GenbankSym> results = parse(null, Integer.MIN_VALUE, Integer.MAX_VALUE);
		for (GenbankSym sym : results) {
			BioSeq seq = sym.getBioSeq();
			if (sym.getMax() > seq.getMax()) {
				seq.setLength(sym.getMax());
			}
			if (!chrList.containsKey(seq)) {
				chrList.put(seq, new File(uri));	// Doesn't matter if the file exists or not.
			}
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
	public List<GenbankSym> getGenome() {
		return parse(null, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public List<GenbankSym> getChromosome(BioSeq seq) {
		return parse(seq, Integer.MIN_VALUE, Integer.MAX_VALUE);
	}

	@Override
	public List<GenbankSym> getRegion(SeqSpan span) {
		return parse(span.getBioSeq(), span.getMin(), span.getMax());
	}
	
	/**
	 * Return a list of symmetries for the given chromosome range
	 * @param seq
	 * @return
	 */
	public List<GenbankSym> parse(BioSeq seq, int min, int max) {
		BufferedInputStream bis = null;
		BufferedReader br = null;
		try {
			bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
			br = new BufferedReader(new InputStreamReader(bis));
			return parse(br, seq, min, max);
		}catch (Exception ex) {
			Logger.getLogger(Genbank.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(br);
		}
		return Collections.<GenbankSym>emptyList();
	}

	public List<GenbankSym> parse(BufferedReader input, BioSeq seq, int min, int max) {
		line_number = 0;
		current_line_type = -1;
		
		Map<String,GenbankSym> id2sym = new HashMap<String,GenbankSym>(1000);

		String first_line = null;
		getCurrentInput(input);
		while (current_line != null && first_line == null) {
			int index = current_line.indexOf("LOCUS       ");
			if (index < 0) {
				index = current_line.indexOf("ID   ");
			}
			if (index >= 0) {
				first_line = current_line.substring(index);
				current_line = first_line;
				current_line_type = getLineType(current_line);
				current_content = getRestOfLine(current_line, content_offset);
				current_locus = current_content.trim();
			} else {
				getCurrentInput(input);
			}
		}

		if (first_line == null) {
			Logger.getLogger(Genbank.class.getName()).log(
					Level.SEVERE, "GenBank read failed");
			return Collections.<GenbankSym>emptyList();
		}
		
		//if (beginEntry() != null) {
		readFeature(input, id2sym, seq, min, max);
		//}
		return new ArrayList<GenbankSym>(id2sym.values());
	}

	// loop until there are no more lines in the file or we hit the start of
	// the next feature or the start of the next line group
	private void readFeature(BufferedReader input, Map<String,GenbankSym> id2sym, BioSeq seq, int min, int max) {
		boolean done = false;
		//GenbankSequence seq = (GenbankSequence) curation.getRefSequence();
		getCurrentInput(input);

		while (current_line != null && !done) {
			switch (current_line_type) {
				case END_OF_ENTRY:
					done = true;
					break;
				case SEQUENCE:
					ignoreSequence(input);
					break;
				case FEATURE_HEADER:
				case FEATURE:
					readSingleFeature(input, id2sym, seq, min, max);
					break;
				case DEFINITION:
					getCurrentInput(input);
					break;
				case ACCESSION:
					getCurrentInput(input);
					break;
				case ORGANISM:
					//seq.setOrganism(current_content);
					getCurrentInput(input);
					break;
				case REF_KEY:
					getCurrentInput(input);
					break;
				case MISC:
					getCurrentInput(input);
					break;
				default:
					Logger.getLogger(Genbank.class.getName()).log(
							Level.WARNING, 
							"not handling line type \"{0}\" (current content = {1})",
							new Object[]{current_line_type, current_content});
					getCurrentInput(input);
			}
		}
		if (current_line_type != END_OF_ENTRY) {
			Logger.getLogger(Genbank.class.getName()).log(
					Level.WARNING, "didn't find end of record (//) but saving annotations anyway.");
		}
	}

	/**
	Sets up all the current values, the line itself, the
	line type, the offset, and the content */
	private void getCurrentInput(BufferedReader input) {
		line_number++;
		current_line = getCurrentLine(input, line_number);
		if (current_line == null) {
			return;
		}
		current_line_type = getLineType(current_line);
		current_content = getRestOfLine(current_line, content_offset);
	}


	private static String getCurrentLine(BufferedReader input, int line_number) {
		String current_line = readLineFromInput(input, line_number);
		if (current_line == null) {
			return null;
		}
		boolean within_html = true;
		while (within_html) {
			int html1 = current_line.indexOf("<");
			int html2 = current_line.indexOf(">") + 1;
			within_html = (html1 >= 0 && html2 > 0 && html2 > html1);
			// Is this REALLY html, or is it < or > indicating incomplete 5' or 3' end?
			// (don't want to get rid of those!!)
			within_html = within_html && (current_line.indexOf("   gene   ") < 0) && (current_line.indexOf("   mRNA   ") < 0);
			if (within_html) {
				String prefix = current_line.substring(0, html1);
				String suffix = html2 < current_line.length() ? current_line.substring(html2) : "";
				current_line = prefix + suffix;
			}
		}
		current_line = replaceInString(current_line, "&gt;", ">");
		current_line = replaceInString(current_line, "&lt;", "<");
		return current_line;
	}


	private static String readLineFromInput(BufferedReader input, int line_number) {
		try {
			return (input.readLine());
		} catch (Exception e) {
			Logger.getLogger(Genbank.class.getName()).log(
							Level.SEVERE,"Unable to read line " + line_number, e);
			return null;
		}
	}

	private static String replaceInString(String current_line,
			String get_out,
			String put_in) {
		boolean seeking = true;
		while (seeking) {
			int sgml = current_line.indexOf(get_out);
			seeking = sgml >= 0;
			if (seeking) {
				String prefix = current_line.substring(0, sgml);
				int index = sgml + get_out.length();
				String suffix = (index < current_line.length()
						? current_line.substring(index) : "");
				current_line = prefix + put_in + suffix;
			}
		}
		return current_line;
	}

	/**
	 *  Return the embl line type of the line contained in the argument String.
	 */
	public int getLineType(String line) {
		String line_key = null;
		if (line.length() >= 2
				&& (line.charAt(0) == '/' || Character.isLetter(line.charAt(0)))
				&& (line.charAt(1) == '/' || Character.isLetter(line.charAt(1)))
				&& (line.length() == 2
				|| line.length() == 3 && line.endsWith(" ")
				|| line.length() == 4 && line.endsWith("  ")
				|| line.length() >= 5 && line.substring(2, 5).equals("   "))) {
			line_key = line.substring(0, 2);
			content_offset = EMBL_CONTENT_OFFSET;
		} else if (line.length() > 0) {
			if (Character.isLetter(line.charAt(0))) {
				int first_space = line.indexOf(' ');
				if (first_space == -1) {
					line_key = genbank_hash.get(line);
				} else {
					String first_word = line.substring(0, first_space);
					line_key = genbank_hash.get(first_word);
				}
				if (line_key != null) {
					content_offset = GENBANK_CONTENT_OFFSET;
				}
			} else if (GENBANK_CONTENT_OFFSET < line.length()) {
				String sub_key = line.substring(0, GENBANK_CONTENT_OFFSET).trim();
				// Don't use the subkeys, stick with the main key
				//        if (!(sub_key.equals("")))
				//          current_first_word = sub_key;
				line_key = genbank_hash.get(sub_key);
			}
		}

		if (line_key != null) {
			//current_line_key = line_key;
			if (line_key.startsWith(BEGINNING_OF_ENTRY_STRING)) {
				return BEGINNING_OF_ENTRY;
			}
			if (line_key.startsWith(ORGANISM_STRING)) {
				return ORGANISM;
			}
			if (line_key.startsWith(END_OF_ENTRY_STRING)) {
				return END_OF_ENTRY;
			}
			if (line_key.startsWith(SEQUENCE_STRING)) {
				return SEQUENCE;
			}
			if (line_key.startsWith(FEATURE_HEADER_STRING)) {
				return FEATURE_HEADER;
			}
			if (line_key.startsWith(FEATURE_STRING)) {
				return FEATURE;
			}
			if (line_key.startsWith(DEFINITION_STRING)) {
				return DEFINITION;
			}
			if (line_key.startsWith(ACCESSION_STRING)) {
				return ACCESSION;
			} else {
				return MISC;
			}
		}
		// default is whatever was last parsed in
		return current_line_type;
	}

	/**
	 *  Returns a String containing the contents of the line with the initial
	 *  type string (number of letters dependent upon whether or not it is
	 *  genbank or embl format) and trailing white space removed.
	 */
	private static String getRestOfLine(String line, int content_offset) {
		if (line.length() > content_offset) {
			return line.substring(content_offset).trim();
		}
		return "";
	}

	private void readSingleFeature(BufferedReader input, Map<String,GenbankSym> id2sym, BioSeq seq, int min, int max) {
		// first get past the header
		while (current_line != null && current_line_type != FEATURE) {
			getCurrentInput(input);
		}

		// now actually read in the features
		GenbankSym annotation = null;
		while (current_line != null && current_line_type == FEATURE) {
			GenbankFeature current_feature = new GenbankFeature();
			String key = current_feature.getFeatureType(current_line);
			getCurrentInput(input);
			while (current_line != null
					&& current_line_type == FEATURE
					&& current_feature.addToFeature(current_line)) {
				getCurrentInput(input);
			}
			
			if (key == null) {
				Logger.getLogger(Genbank.class.getName()).log(
						Level.SEVERE, "GenBank read error: no key in line {0}", current_line);
				continue;
			}
			if (key.equals("source")) {
				Map<String, List<String>> tagValues = current_feature.getTagValues();
				for (String tag : tagValues.keySet()) {
					String value = current_feature.getValue(tag);
					if (value != null && !value.equals("")) {
						if (tag.equals("chromosome")) {
							BioSeq newSeq = this.group.getSeq(value);
							if (newSeq == null) {
								newSeq = new BioSeq(value, "", 1000);
								this.group.addSeq(newSeq);
							}
							currentSeq = newSeq;
						} else if (tag.equals("organism")) {
							//   seq.setOrganism (value);
						}
					}
				}
				continue;
			}

			if (seq != null && currentSeq != seq) {
				// only parse elements on the specified seq
				continue;
			}

			if (key.equals("gene")
					|| // Some GenBank records seem to use locus_tag instead of gene
					// for the gene name/id
					key.equals("locus_tag")) {
				annotation = buildAnnotation(currentSeq, current_locus, current_feature, id2sym, min, max);
				continue;
			}
			if (annotation == null) {
				// skipping this annotation, which is outside the min/max range
				continue;
			}
			if (key.equals("mRNA")
					|| key.equals("rRNA")
					|| key.equals("tRNA")
					|| key.equals("scRNA")
					|| key.equals("snRNA")
					|| key.equals("snoRNA")) {
				String value = current_feature.getValue("gene");
				if (value != null && !value.equals("")) {
					annotation.setProperty("name",value);
				}
				value = current_feature.getValue("locus_tag");
				if (value != null && !value.equals("")) {
					annotation.setID(value);
				}
				// What are the spans associated with this key?
				List<int[]> locs = current_feature.getLocation();
				if (locs == null || locs.isEmpty()) {
				Logger.getLogger(Genbank.class.getName()).log(
						Level.WARNING, "no location for key {0} for {1}", new Object[]{key, current_feature.toString()});
				} else {
					for (int[] loc : locs) {
						annotation.addBlock(loc[0], loc[1]);
					}
				}
			} else if (key.equals("CDS")) {
				// What are the spans associated with this key?
				List<int[]> locs = current_feature.getLocation();
				if (locs == null || locs.isEmpty()) {
				Logger.getLogger(Genbank.class.getName()).log(
						Level.WARNING, "no location for key {0} for {1}", new Object[]{key, current_feature.toString()});
				} else {
					for (int[] loc : locs) {
						annotation.addCDSBlock(loc[0], loc[1]);
					}
				}
			} else {
				Logger.getLogger(Genbank.class.getName()).log(
						Level.WARNING,
						"ignoring {0} features; next line is {1}",
						new Object[]{key, line_number});
			}
		}
	}

	/**
	 *  This method will read raw sequence from the stream, and ignore it.
	 **/
	private void ignoreSequence(BufferedReader input) {
		getCurrentInput(input);
		while (current_line != null && current_line.length() > 0 && current_line_type == SEQUENCE) {
			getCurrentInput(input);
		}
	}

	private static GenbankSym buildAnnotation(BioSeq seq, String type, GenbankFeature pub_feat, Map<String,GenbankSym> id2sym, int min, int max) {
		String id = getAnnotationId(pub_feat);
		if (id == null || id.equals("")) {
			id = pub_feat.getValue("protein_id");
			if (id == null || id.equals("")) {
				Logger.getLogger(Genbank.class.getName()).log(
						Level.WARNING, "no id for {0}", pub_feat.toString());
				id = NONAME;
				// TODO: increment this
			}
		}

		String name = getAnnotationName(pub_feat);
		if (name == null || name.equals("")) {
			name = id;
		}

		// see if a parent sym already exists
		GenbankSym annotation = id2sym.get(id);
		if (annotation == null) {
			List<int[]> locs = pub_feat.getLocation();
			if (locs == null || locs.isEmpty()) {
				Logger.getLogger(Genbank.class.getName()).log(
						Level.WARNING, "no location for {0}", pub_feat.toString());
				annotation = new GenbankSym(type, seq, 0, 0, name);
			}
			int loc0 = locs.get(0)[0];
			int loc1 = locs.get(0)[1];
			if (loc0 >= loc1) {
				// forward
				if (min >= loc1 || max <= loc0) {
					return null;
				}
			}
			if (loc1 < loc0) {
				// reverse
				if (min >= loc0 || max <= loc1) {
					return null;
				}
			}

			annotation = new GenbankSym(type, seq, locs.get(0)[0], locs.get(0)[1], name);
			id2sym.put(id, annotation);
		}
		setDescription(annotation, pub_feat);
		if (!pub_feat.getValue("pseudo").equals("")) {
			annotation.setProperty("pseudogene", "true");
		}
		return annotation;
	}

	private static String getFeatureId(GenbankFeature pub_feat, List<String> tags) {
		String id = "";
		for (int i = 0; i < tags.size() && (id == null || id.equals("")); i++) {
			String tag = tags.get(i);
			id = pub_feat.getValue(tag);
		}
		return id;
	}

	private static String getAnnotationId(GenbankFeature pub_feat) {
		String id = getFeatureId(pub_feat, annot_id_tags);
		if (id == null || id.equals("")) {
			id = getFeatureId(pub_feat, annot_name_tags);
		}
		return id;
	}

	private static String getAnnotationName(GenbankFeature pub_feat) {
		return getFeatureId(pub_feat, annot_name_tags);
	}

	private static void setDescription(
                              GenbankSym seq,
                              GenbankFeature pub_feat) {
    Map<String,List<String>> tagValues = pub_feat.getTagValues();
	for (String tag : tagValues.keySet()) {
      String value = pub_feat.getValue(tag);
      if (value != null && !value.equals("")) {
       if (tag.equals("chromosome")) {
       //   curation.setChromosome(value);
		  }
       else if (tag.equals("organism")) {
       //   seq.setOrganism (value);
	   }
        else {
		   seq.setProperty(tag, value);
          //seq.setDescription (seq.getDescription() + " " + value);
          //seq.addProperty(tag, value);
          //logger.debug("Added property to sequence: " + tag + "=" + value);
        }
      }
    }
    //addDbXref (seq, pub_feat.getDbXrefs());
  }
}


/** A value class for the different types of Data input */
final class GenbankFeature {

  // -----------------------------------------------------------------------
  // Class/static variables
  // -----------------------------------------------------------------------

  private static final int key_offset = 5;

  // -----------------------------------------------------------------------
  // Instance variables
  // -----------------------------------------------------------------------

  private String type;
  // this is a hash into vectors to allow for cases where
  // there is more than one value for an individual tag
  private Map<String,List<String>> tagValues = new HashMap<String,List<String>>(3);
  private StringBuffer location;
  private String active_tag = null;
  private List<int[]> locs = null;

  private boolean initialized = false;
  //private boolean missing5prime = false;
  private boolean extend3prime = false;
  private boolean extend5prime = false;

  // private constructor - cant be made outside class
  protected GenbankFeature() {
  }

  protected String getFeatureType(String current_line) {
    String str = current_line.substring(key_offset);
    int index = str.indexOf(' ');
    if (index > 0) {
      this.location = new StringBuffer();
      this.type = str.substring(0, index);
      this.location.append(str.substring(index).trim());
    }
    return this.type;
  }

  protected boolean addToFeature(String current_line) {
    if (forSameFeature(current_line)) {
      String str = current_line.substring(key_offset).trim();
      // if (str.indexOf('/') < 0 && active_tag == null){
      if (str.charAt(0) != '/' && active_tag == null) {
        this.location.append(str);
      }
      else {
        setTagValue(str);
      }
      return true;
    }
    else {
      return false;
    }
  }

  private static boolean forSameFeature(String current_line) {
    return (current_line.charAt(key_offset) == ' ');
  }

  private List<String> getValues(String tag) {
    if (!initialized) {
      initialized = true;
      initSynonyms();
      initLocations();
    }
    return tagValues.get(tag);
  }

  protected Map<String,List<String>> getTagValues() {
    return tagValues;
  }

  protected String getValue(String tag) {
    /* there were 2 options here, either just get the first
       value or get all of them concatenated together. going
       with the latter so nothing is lost */
    StringBuilder val = new StringBuilder();
    List<String> all_vals = getValues(tag);
    if (all_vals != null) {
      int val_count = all_vals.size();
      for (int i = 0; i < val_count; i++) {
        val.append(all_vals.get(i));
        if (i < val_count-1)
          val.append(" ");
      }
    }
    return val.toString();
  }

  private void initSynonyms() {
    List<String> syns = null;
    String note = getValue("note");
    int index = (note != null ? note.indexOf("synonyms:") : -1);
    if (index >= 0) {
      syns = new ArrayList<String>(1);
      //String prefix = note.substring(0, index);
      String syns_str = note.substring(index + "synonyms:".length()).trim();
      String syn;
      //String suffix = "";
      int end = syns_str.indexOf(';');
      if (end > 0) {
        syns_str = syns_str.substring(0, end);
        index = note.indexOf(';', index) + 1;
        //if (index < note.length())
        //  suffix = note.substring(index);
      }
      while (syns_str.length() > 0) {
        end = syns_str.indexOf(',');
        if (end > 0) {
          syn = syns_str.substring(0, end);
          syns_str = (++end < syns_str.length() ?
                      syns_str.substring(end) : "");
        } else {
          syn = syns_str;
          syns_str = "";
        }
        syns.add(syn);
      }
     /* Vector note_vec = (Vector) tagValues.get("note");
      note_vec.removeElementAt(0);
      note_vec.addElement(prefix + suffix);*/
    }
    if (syns != null)
      tagValues.put("synonyms", syns);
  }

  private void setTagValue(String content) {
    String tag;
    String value;
    //String db_tag = null;
    //String db_value = null;
    //String synonyms = null;

    if (content.charAt(0) == '/') {
      int index = content.indexOf("=");
      if (index >= 0) {
        tag = content.substring(1, index);
        value = content.substring(index + "=".length());
        value = stripQuotes(value);

        index = value.indexOf(':');
        if (index > 0 &&
            !tag.equals("note") && !value.substring(0, index).contains(" ") &&
            !tag.equals("gene") &&
            !tag.equals("method") &&
            !tag.equals("date") &&
            !tag.endsWith("synonym") &&
            !tag.equals("product") &&
            !tag.equals("prot_desc")) {
         // if (!tag.equals("db_xref")) {
            // Why do we want to do this?  I see lots of cases where we *don't*
            // want to, but I don't see when we'd ever want to.  --NH
            tag = value.substring(0, index);
            String tmp = value.substring(index + ":".length());
            value = tmp;
         /* } else {  // db_xref
            db_tag = value.substring(0, index);
            db_value = value.substring(index + ":".length());
          }*/
        }
        active_tag = tag;
      } else if (content.charAt(0) == '/') {
        tag = content.substring(1);
        value = "true";
      } else {
        tag = active_tag;
        value = content;
      }
    } else {
      tag = active_tag;
      value = content;
      value = stripQuotes(value);
    }

    if (!value.equalsIgnoreCase("unknown")) {
		  List<String> current_vec = tagValues.get(tag);
		  if (current_vec == null) {
			  current_vec = new ArrayList<String>();
			  tagValues.put(tag, current_vec);
		  }
		  /* else if (content.charAt(0) != '/' || tag.equals("note")) {
		  // This is an extension, not a second occurrence, of a tag
		  int i = current_vec.size() - 1;
		  String current_value = (String) current_vec.get(i);
		  current_vec.removeElementAt(i);
		  value = current_value + " " + value;
		  }*/
		  if (!value.equals("") && !value.equals(".")) {
			  current_vec.add(value);
		  }
	  }
  }

	@Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append(type).append(" at ").append(location.toString()).append("\n");
	for (String tag : tagValues.keySet()) {
      if (!tag.equals("translation") &&
          !tag.equals("method") &&
          !tag.equals("db_xref")) {
		  buf.append("\t").append(tag).append(" = ").append(getValue(tag)).append("\n");
	  }
    }
    return buf.toString();
  }

	static String stripQuotes(String value) {
    if (value.length() == 0)
      return value;

    if (value.charAt(0) == '\"')
      value = value.substring(1);

    if (value.length() >= 1 && value.charAt(value.length() - 1) == '\"')
      return value.substring(0, value.length() - 1);

	return value;
  }

  protected List<int[]> getLocation() {
    if (!initialized) {
      initialized = true;
      initSynonyms();
      initLocations();
    }
    return locs;
  }

  protected void initLocations() {
    locs = new ArrayList<int[]>();
    parseLocations(this.location.toString(), locs);
  }

  /**
     location operators ::= complement, join, order
     locations ::=
     basenumber^basenumber (insertion between these)
     basenumber..basenumber  (span)
     basenumber.basenumber (single base)
     basenumber (single base)
     adding < and > now too
     entryname:basenumber..basenumber (indirect references)
     *
     *
     **/
  private void parseLocations(String location_str, List<int[]> locs) {
    if (location_str != null && !location_str.equals("")) {
      String operation_str = null;
      int index_start = 0;
      int index_end = 0;
      if (location_str.startsWith("complement(")) {
        List<int[]> comp_vect = new ArrayList<int[]>();
        index_start = "complement(".length();
        index_end = indexOfClosingParen(location_str, index_start);
        operation_str = substringLocation(location_str,
                                          index_start, index_end);
        parseLocations(operation_str, comp_vect);
        int cmp_count = comp_vect.size();
        for (int i = cmp_count - 1; i >= 0; i--) {
          int [] span = comp_vect.get(i);
          comp_vect.remove(span);
          int tmp = span[0];
          span[0] = span[1];
          span[1] = tmp;
          locs.add(span);
        }
        if (extend3prime) {
          extend5prime = true;
          extend3prime = false;
        } else if (extend5prime) {
          extend5prime = false;
          extend3prime = true;
        }
      }
      else if (location_str.startsWith("join(")) {
        index_start = "join(".length();
        index_end = indexOfClosingParen(location_str, index_start);
        operation_str = substringLocation(location_str,
                                          index_start, index_end);
        parseLocations(operation_str, locs);
      }
      else if (location_str.startsWith("order(")) {
        index_start = "order(".length();
        index_end = indexOfClosingParen(location_str, index_start);
        operation_str = substringLocation(location_str,
                                          index_start, index_end);
        parseLocations(operation_str, locs);
      }
      else if (!Character.isDigit(location_str.charAt(0))) {
        if (location_str.charAt(0) == '<') {
          extend5prime = true;
          parseLocations(location_str.substring(1), locs);
        }
        else if (location_str.indexOf(':') > 0) {
          // 6/21/04: Sometimes the EMBL file has a source like
          // FT   source          AJ009736:1..7411
          // Why should this make us refuse to parse the file?  Let's just
          // strip off the part before the : (and hope for the best).
          parseLocations(location_str.substring(location_str.indexOf(':')+1), locs);
        }
        else
          parseLocations(location_str.substring(1), locs);
      }
      else {
        index_start = 0;
        index_end = indexOfNextNonDigit(location_str, index_start);
        String pos_str = "";
        try {
          pos_str = substringLocation(location_str,
                                      index_start, index_end);
          int low = Integer.parseInt(pos_str);
          int high;
          boolean no_high = ((index_end < location_str.length() &&
                              location_str.charAt(index_end) == ',') ||
                             index_end >= location_str.length());
          if (no_high) {
            high = low;
          }
          else {
            index_start = ++index_end;
            if (location_str.charAt(index_start) == '>') {
              extend3prime = true;
              index_start++;
            }
            if (Character.isDigit(location_str.charAt(index_start))) {
              // This is a point location
              index_end = indexOfNextNonDigit(location_str, index_start);
              pos_str = substringLocation(location_str,
                                          index_start, index_end);
              high = Integer.parseInt(pos_str);
              if (high != low) {
                low = low + ((high - low + 1) / 2);
                high = low + 1;
              }
              else
                high++;
            } else {
              index_start++;
              if (location_str.charAt(index_start) == '>') {
                extend3prime = true;
                index_start++;
              }
              index_end = indexOfNextNonDigit(location_str, index_start);
              pos_str = substringLocation(location_str,
                                          index_start, index_end);
              high = Integer.parseInt(pos_str);
            }
          }
          int [] pos = new int [2];
          pos[0] = low;
          pos[1] = high;
          locs.add(pos);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      int index_comma = (index_end < location_str.length() ?
			 location_str.indexOf(",", index_end) : -1);
      int next_locs = (index_comma >= 0 ?
		       index_comma + 1 : 0);
      if (next_locs > 0 && next_locs < location_str.length())
        parseLocations(location_str.substring(next_locs), locs);
    }
  }

  private static int indexOfNextNonDigit(String location_str, int index_start) {
    int index_end = index_start;
    while (index_end < location_str.length() &&
           Character.isDigit(location_str.charAt(index_end)))
      index_end++;
    return index_end;
  }

  private static String substringLocation(String location_str,
                                   int index_start, int index_end) {
    try {
      return (index_end < location_str.length() ?
              location_str.substring(index_start, index_end) :
              (index_start < location_str.length() ?
               location_str.substring(index_start) : location_str));
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private static int indexOfClosingParen(String location_str, int index_start) {
    int index_end = index_start;
    int open_paren_count = 1;
    while (index_end < location_str.length() &&
           open_paren_count != 0) {
      if (location_str.charAt(index_end) == ')')
        open_paren_count--;
      else if (location_str.charAt(index_end) == '(')
        open_paren_count++;
      if (open_paren_count != 0)
        index_end++;
    }
    return index_end;
  }
}
