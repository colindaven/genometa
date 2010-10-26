/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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

import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import com.affymetrix.genometryImpl.symmetry.MutableSingletonSeqSymmetry;
import com.affymetrix.genometryImpl.UcscGffSym;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.comparator.SeqSymStartComparator;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.util.GeneralUtils;

/**
 *  GFF parser.  For GFF Version 3, you can use the class GFF3Parser directly, or
 *  this parser will call it for you behind the scenes.
 * <pre>
 *  Trying to parse three different forms of GFF:
 *    GFF Version 1.0
 *    GFF Version 2.0
 *    GTF Version ?
 *
 *  GFF format is tab-delimited fields:
 *   <seqname> <source> <feature> <start> <end> <score> <strand> <frame> [attribute] [comment]
 *    where <field> indicates required field, and [field] indicates optional field
 *
 *  for now, assuming that in attribute field their are no backslashed quotes
 *     (in other words, don't see something like "depends on what \"is\" means" )
 *  also, assuming that in attribute field the only ";" characters are for separating
 *     the tag-value sets
 *
 *
 *  GTF is Affy/Neo format, and is GFF v2 with certain restrictions:
 *    ("xyz" line means a line whose feature field == "xyz")
 *    1. All "gene" lines must have a tag-value attribute with tag == "gene_id"
 *        and single value
 *    2. All lines except "gene" lines must have tag-value attributes
 *       with tag == "gene_id" and tag == "transcript_id", each with a single value
 *    3. For each unique "transcript_id" value in tag-value attribtues
 *       over the whole GFF file, there must be a "prim_trans" line
 *
 *  Eventually want to support feature "grouping", and distinguish several special cases:
 *    GFF 1.0, where attributes field (if present) should be a single free-text entry,
 *        which indicates a group to cluster features by
 *    GFF 2.0, where one has prior knowledge of which tag-value entry in attributes
 *        field to use to cluster features by
 *    GTF, where features should be clustered by the value for attribute tag
 *        "transcript_id", and aforementioned restrictions apply
 *  if none of these apply, then don't group features at all
 *  When building genometry models based on GFF, feature "grouping" corresponds to
 *      making some symmetries (the ones to be grouped) into children of other symmetries
 *
// if loading as new BioSeq, use source id to specify ID of new BioSeq
// if merging to previous BioSeq, use source id to check for identity with BioSeq
 *
 *  for GTF,
 *  still need to deal with CDS and CDS_insert!!!
 *
 *  CDS_insert means there's bases missing in the genome
 *     for GTF CDS_insert, score field is length of extra bases in transcript/CDS that are
 *     missing from genome
 *  For this kinda stuff, going to need a specific GTF parser that understands
 *     semantics of some of the GFF types (exon, CDS, CDS_insert, etc.) and
 *     can build appropriate genometry models
 *</pre>
 *
 * @version $Id: GFFParser.java 6652 2010-08-12 17:51:00Z jnicol $
 */
public final class GFFParser implements AnnotationWriter  {
	public static final int VERSION_UNKNOWN = 0;
	public static final int GFF1 = 1;
	public static final int GFF2 = 2;
	public static final int GFF3 = 3;
	public static final int GTF = 201;

	public static final String GFF3_ID = "ID";
	public static final String GFF3_PARENT = "Parent";

	static List pref_list = Arrays.asList("gff");

	int gff_version = 0;

	private static final boolean DEBUG = false;
	boolean DEBUG_GROUPING = false;
	boolean USE_FILTER = true;
	boolean USE_GROUPING = true;

	//override the source in the GFF line and use default source
	boolean useDefaultSource = false;
	boolean use_standard_filters = false;
	boolean gff_base1 = true;

	// should only be one tab between each field, but just in case,
	//    allowing for possible multi-tabs
	static final Pattern line_regex = Pattern.compile("\\t+");

	// Note that this simple rule for breaking the string at semicolons doesn't
	// allow for the possibility that some tag's values might contain semicolons
	// inside quotes
	static final Pattern att_regex = Pattern.compile(";");


	// According to http://www.sanger.ac.uk/Software/formats/GFF/GFF_Spec.shtml
	// all tags must match ([A-Za-z][A-Za-z0-9_]*)
	// but we have relaxed this rule (probably inadvertently) to just ([\\w]+)
	// (thus allowing the identifier to start with '_' or a number.)
	static final Pattern tag_regex = Pattern.compile("^\\s*([\\w]+)\\s*");

	// a regular expression to find values for tag-value entries
	// values are either
	//   (1): quote-delimited free text (and this code makes the further
	//          simplifying assumption that there are no backslashed quotes
	//          and no semicolons in the free text)
	//   (2): non-whitespace that doesn't start with a quote (or a whitespace)
	static final Pattern value_regex = Pattern.compile(
			"\\s*\"([^\"]*)\""    /* pattern 1 */
			+ "|"                   /* or */
			+ "\\s*([^\"\\s]\\S*)"  /* pattern 2 */
			);

	static final Pattern gff3_tagval_splitter = Pattern.compile("=");
	static final Pattern gff3_multival_splitter = Pattern.compile(",");

	// a hash used to filter
	//  Hashtable fail_filter_hash = new Hashtable();
	Map<String,String> fail_filter_hash = null;
	Map<String,String> pass_filter_hash = null;

	Map<String,Object> gff3_id_hash = new HashMap<String,Object>();

	/*
	 *  tag to group features on
	 */
	String group_tag = null;
	String group_id_field_name = null;
	String id_tag = null;

	TrackLineParser track_line_parser = new TrackLineParser();

	/** Whether to convert group_id field value to lower case.
	 *  Beginning with source forge version 1.5 of this file, we started always
	 *  doing this, but the reason has been forgotten.  Unless we can remember
	 *  why case-insensitivity (i.e. forcing lower case) is needed, let's set
	 *  this to false.  We could allow a new flag in the GFF header to toggle
	 *  this on or off.
	 *  <p>
	 *  SourceForge issue ID:
	 *  <a href="https://sourceforge.net/tracker/index.php?func=detail&aid=1143530&group_id=129420&atid=714744">1143530</a>
	 *  </p>
	 */
	boolean GROUP_ID_TO_LOWER_CASE = false;

	// When grouping, do you want to use the first item encountered as the parent of the group?
	boolean use_first_one_as_group = false;

	public GFFParser() {
		this(true);
	}

	/**
	 * Constructor.
	 * @param coords_are_base1  whether it is necessary to convert from base-1
	 *     numbering to interbase-0 numbering, to agree with genometry.
	 */
	public GFFParser(boolean coords_are_base1) {
		gff_base1 = coords_are_base1;
	}

	/**
	 *  Adds a filter to the fail_filter_hash.
	 *  Like {@link #addFeatureFilter(String, boolean)} with pass_filter=false.
	 */
	public void addFeatureFilter(String feature_type)  {
		addFeatureFilter(feature_type, false);
	}

	/**
	 *  Allows you to specify the entries you want to accept while parsing, or
	 *    the ones you want to reject.
	 *  When filtering:
	 *      1.  if there are any entries in pass_filter, then _only_ features with type entries
	 *          in the pass_filter_hash will pass through the filter;
	 *      2.  if there are any entries in fail_filter, then _only_ features that do _not_ have
	 *          entries in the fail_filter will pass through the filter.
	 *
	 *  @param pass_filter  if true then add to the pass_filter_hash;
	 *    if false then add to the fail_filter_hash
	 */
	public void addFeatureFilter(String feature_type, boolean pass_filter) {
		if (pass_filter) {
			if (pass_filter_hash == null) { pass_filter_hash = new HashMap<String,String>(); }
			pass_filter_hash.put(feature_type, feature_type);
		}
		else {
			if (fail_filter_hash == null) { fail_filter_hash = new HashMap<String,String>(); }
			fail_filter_hash.put(feature_type, feature_type);
		}
	}

	/**
	 *  Removes a filter from the fail_filter_hash.
	 *  Like {@link #removeFeatureFilter(String, boolean)} with pass_filter=false.
	 */
	public void removeFeatureFilter(String feature_type) {
		removeFeatureFilter(feature_type, false);
	}


	/**
	 *  Remove a filter that had been added with {@link #addFeatureFilter(String, boolean)}.
	 *  @param pass_filter if true then remove from pass_filter_hash;
	 *                if false then remove from fail_filter_hash
	 */
	public void removeFeatureFilter(String feature_type, boolean pass_filter) {
		if (pass_filter) {
			if (pass_filter_hash != null) {
				pass_filter_hash.remove(feature_type);
				if (pass_filter_hash.size() == 0) { pass_filter_hash = null; }
			}
		}
		else {
			if (fail_filter_hash != null) {
				fail_filter_hash.remove(feature_type);
				if (fail_filter_hash.size() == 0) { fail_filter_hash = null; }
			}
		}
	}

	/**
	 *  Removes all filtering.  Removes all "pass" filters and all "reject" filters.
	 *  Has no effect on any grouping tag set with {@link #setGroupTag(String)}.
	 */
	public void resetFilters() {
		pass_filter_hash = null;
		fail_filter_hash = null;
	}

	/**
	 *  Sets which tag to use to create groups.  Most commonly, this will
	 *  be set to "transcript_id" to group all entries from the same transcript.
	 *  Can be set to null if no grouping is desired.
	 */
	public void setGroupTag(String tag) {
		group_tag = tag;
	}

	public void setIdTag(String tag) {
		id_tag = tag;
	}

	boolean use_track_lines = true;
	
	

	public List<? extends SeqSymmetry> parse(InputStream istr, AnnotatedSeqGroup seq_group, boolean create_container_annot)
		throws IOException {
		return this.parse(istr, ".", seq_group, create_container_annot);
	}

	public List<? extends SeqSymmetry> parse(InputStream istr, String default_source, AnnotatedSeqGroup seq_group, boolean create_container_annot)
		throws IOException {
		return this.parse(istr, default_source, seq_group, create_container_annot, true);
	}

	public List<? extends SeqSymmetry> parse(InputStream istr, String default_source, AnnotatedSeqGroup seq_group,
			boolean create_container_annot, boolean annotate_seq)
		throws IOException {
		if (DEBUG) {
			System.out.println("starting GFF parse, create_container_annot: " + create_container_annot);
		}
		int line_count = 0;
		int sym_count = 0;
		int group_count = 0;
		number_of_duplicate_warnings = 0;

		Map<BioSeq,Map<String,SimpleSymWithProps>> seq2meths = new HashMap<BioSeq,Map<String,SimpleSymWithProps>>(); // see getContainer()
		Map<String,SingletonSymWithProps> group_hash = new HashMap<String,SingletonSymWithProps>();
		gff3_id_hash = new HashMap<String,Object>();
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>();

		// By default, no hierarchical grouping; turned on with a directive
		use_hierarchy = false;
		hierarchy_levels.clear(); // clear until an ##IGB-hierarchy directive is found
		int current_h_level = 0;
		UcscGffSym[] hier_parents = null;
		BufferedReader br = new BufferedReader(new InputStreamReader(istr));
		String line = null;
		String track_name = null;

		try {
			Thread thread = Thread.currentThread();
			while ((! thread.isInterrupted()) && ((line = br.readLine()) != null)) {
				if (line == null) { continue; }
				if (line.startsWith("##")) {
					processDirective(line);
					if (gff_version == 3) {
						if (line_count > 0) {
							throw new IOException("You can only use the '##gff-version' parameter at the beginning of the file");
						} else {
							// The "#gff-version 3" pragma is *required* to be on the first line.
							GFF3Parser gff3_parser = new GFF3Parser();
							return gff3_parser.parse(br, default_source, seq_group, annotate_seq);
						}
					} else {
						continue;
					}
				}
				if (line.startsWith("#")) { continue; }
				if (line.startsWith("track")) {
					track_line_parser.parseTrackLine(line);
					TrackLineParser.createTrackStyle(track_line_parser.getCurrentTrackHash(), default_source);
					track_name = track_line_parser.getCurrentTrackHash().get(TrackLineParser.NAME);
					continue;
				}
				String fields[] = line_regex.split(line);

				if (fields != null && fields.length >= 8) {
					line_count++;
					if (DEBUG && (line_count % 10000) == 0) { System.out.println("" + line_count + " lines processed"); }
					String feature_type = fields[2].intern();

					// if feature_type is present in fail_filter_hash, skip this line
					if (USE_FILTER && (fail_filter_hash != null)  && (fail_filter_hash.get(feature_type) != null)) { continue; }
					// if feature_type is _not_ present in pass_filter_hash, skip this line
					if (USE_FILTER && (pass_filter_hash != null)  && (pass_filter_hash.get(feature_type) == null)) { continue; }

					String seq_name = fields[0].intern();
					String source = fields[1].intern();
					if (useDefaultSource || ".".equals(source)) {
						source = default_source;
					}

					int coord_a = Integer.parseInt(fields[3]);
					int coord_b = Integer.parseInt(fields[4]);
					String score_str = fields[5];
					String strand_str = fields[6].intern();
					String frame_str = fields[7].intern();
					String last_field = null;
					if (fields.length>=9) { last_field = new String(fields[8]); } // creating a new String saves memory
					// last_field is "group" in GFF1 or "attributes" in GFF2 and GFF3

					float score = UcscGffSym.UNKNOWN_SCORE;
					if (! score_str.equals(".")) { score = Float.parseFloat(score_str); }

					BioSeq seq = seq_group.getSeq(seq_name);
					if (seq == null) {       	  
						seq = seq_group.addSeq(seq_name, 0);
					}

					if (gff_version == GFF3) {
						// temporary hack to make GFF3 look like GFF1
						last_field = hackGff3GroupId(last_field);
					}
					UcscGffSym sym = new UcscGffSym(seq, source, feature_type, coord_a, coord_b,
							score, strand_str.charAt(0), frame_str.charAt(0),
							last_field, gff_base1);

					if (use_track_lines && track_name != null) {
						sym.setProperty("method", track_name);
					} else {
						sym.setProperty("method", source);
					}


					int max = sym.getMax();
					if (max > seq.getLength()) { seq.setLength(max); }

					// add syms to a results List during parsing,
					// then add group syms to BioSeq after entire parse is done.

					if (use_hierarchy) {
						useHierarchy(hier_parents, feature_type, current_h_level, line, sym, results);
					}
					else if (USE_GROUPING)  {
						group_count = useGrouping(sym, results, group_hash, source, track_name, group_count, seq_group);
					}
					else {
						// if not grouping, then simply add feature directly to results List
						results.add(sym);
					}
					sym_count++;
				}
			}
		} finally {
			GeneralUtils.safeClose(br);
		}
		hierarchy_levels.clear();
		
		addSymstoSeq(results, create_container_annot, seq2meths, annotate_seq);
		

		System.out.println("lines: " + line_count + " syms:" + sym_count + " groups:" + group_count + " results:" + results.size());
		return results;
		}


	private void useHierarchy(UcscGffSym[] hier_parents, String feature_type, int current_h_level, String line, UcscGffSym sym, List<SeqSymmetry> results) throws RuntimeException {
		if (hier_parents == null) {
			hier_parents = new UcscGffSym[hierarchy_levels.size()];
		}
		Integer new_h_level_int = hierarchy_levels.get(feature_type);
		if (new_h_level_int == null) {
			throw new RuntimeException("Hierarchy exception: unknown feature type: " + feature_type);
		}
		int new_h_level = new_h_level_int.intValue();
		if (new_h_level - current_h_level > 1) {
			throw new RuntimeException("Hierarchy exception: skipped a level: " + current_h_level + " -> " + new_h_level + ":\n" + line + "\n");
		}
		String id_field = hierarchy_id_fields.get(feature_type);
		if (id_field != null) {
			String group_id = determineGroupId(sym, id_field);
			if (group_id != null) {
				sym.setProperty("id", group_id);
			}
		}
		hier_parents[new_h_level] = sym; // It is a potential parent of the lower-level sym
		if (new_h_level == 0) {
			results.add(sym);
		} else {
			UcscGffSym the_parent = hier_parents[new_h_level - 1];
			if (the_parent == null) {
				throw new RuntimeException("Hierarchy exception: no parent");
			}
			the_parent.addChild(sym);
		}
		current_h_level = new_h_level;
	}


	private int useGrouping(UcscGffSym sym, List<SeqSymmetry> results, Map<String, SingletonSymWithProps> group_hash, String source, String track_name, int group_count, AnnotatedSeqGroup seq_group) {
		String group_id = null;
		if (sym.isGFF1()) {
			group_id = sym.getGroup();
		} else if (group_tag != null) {
			group_id = determineGroupId(sym, group_tag);
		}
		if (group_id == null) {
			results.add(sym); // just add it directly
		} else {
			if (DEBUG_GROUPING) {
				System.out.println(group_id);
			}
			SingletonSymWithProps groupsym = group_hash.get(group_id);
			if (groupsym == null) {
				if (use_first_one_as_group) {
					// Take the first entry found with a given group_id and use it
					// as the parent symmetry for all members of the group
					// (For example, a "transcript" line with transcript_id=3 might
					//  be followed by several "exon" lines with transcript_id=3.
					//  The "transcript" line should be used as the group symmetry in this case.)
					groupsym = sym;
				} else {
					// Make a brand-new symmetry to hold all syms with a given group_id
					groupsym = new SingletonSymWithProps(sym.getStart(), sym.getEnd(), sym.getBioSeq());
					groupsym.addChild(sym);
					// Setting the "group" property might be needed if you plan to use the
					// outputGFF() method.  Otherwise it is probably not necessary since "id" is set to group id below
					groupsym.setProperty("group", group_id);
					groupsym.setProperty("source", source);
					if (this.use_track_lines && track_name != null) {
						groupsym.setProperty("method", track_name);
					} else {
						groupsym.setProperty("method", source);
					}
				}
				group_count++;
				// If one field, like "probeset_id" was chosen as the group_id_field_name,
				// then make the contents of that field be the "id" of the group symmetry
				// and also index it in the IGB id-to-symmetry hash
				String index_id = null;
				if (group_id_field_name != null) {
					index_id = (String) sym.getProperty(group_id_field_name);
				}
				if (index_id != null) {
					groupsym.setProperty("id", index_id);
					if (seq_group != null) {
						seq_group.addToIndex(index_id, groupsym);
					}
				} else {
					groupsym.setProperty("id", group_id);
					if (seq_group != null) {
						seq_group.addToIndex(group_id, groupsym);
					}
				}
				group_hash.put(group_id, groupsym);
				results.add(groupsym);
			} else {
				groupsym.addChild(sym);
			}
		}
		return group_count;
	}


	private void addSymstoSeq(List<SeqSymmetry> results, boolean create_container_annot, Map<BioSeq, Map<String, SimpleSymWithProps>> seq2meths, boolean annotate_seq) {
		// Loop through the results List and add all Sym's to the BioSeq
		Iterator iter = results.iterator();
		while (iter.hasNext()) {
			SingletonSymWithProps sym = (SingletonSymWithProps) iter.next();
			BioSeq seq = sym.getBioSeq();
			if (USE_GROUPING && sym.getChildCount() > 0) {
				// stretch sym to bounds of all children
				SeqSpan pspan = SeqUtils.getChildBounds(sym, seq);
				// SeqSpan pspan = SeqUtils.getLeafBounds(sym, seq);  // alternative that does full recursion...
				sym.setCoords(pspan.getStart(), pspan.getEnd());
				resortChildren((MutableSingletonSeqSymmetry) sym, seq);
			}
			if (create_container_annot) {
				String meth = (String) sym.getProperty("method");
				SimpleSymWithProps parent_sym = getContainer(seq2meths, seq, meth, annotate_seq);
				parent_sym.addChild(sym);
			} else {
				if (annotate_seq) {
					seq.addAnnotation(sym);
				}
			}
		}
	}


		/**
		 *  Retrieves (and/or creates) a container symmetry based on the BioSeq
		 *    and the method.
		 *  When a new container is created, it is also added to the BioSeq.
		 *  Each entry in seq2meths maps a BioSeq to a Map called "meth2csym".
		 *  Each meth2csym is hash where each entry maps a "method/source" to a container Symmetry.
		 *  It is a two-step process to find container sym for a particular meth on a particular seq:
		 *    Map meth2csym = (Map)seq2meths.get(seq);
		 *    MutableSeqSymmetry container_sym = (MutableSeqSymmetry)meth2csym.get(meth);
		 */
		static SimpleSymWithProps getContainer(Map<BioSeq,Map<String,SimpleSymWithProps>> seq2meths,
				BioSeq seq, String meth, boolean annotate_seq) {

			Map<String,SimpleSymWithProps> meth2csym = seq2meths.get(seq);
			if (meth2csym == null) {
				meth2csym = new HashMap<String,SimpleSymWithProps>();
				seq2meths.put(seq, meth2csym);
			}
			SimpleSymWithProps parent_sym = meth2csym.get(meth);
			if (parent_sym == null) {
				parent_sym = new SimpleSymWithProps();
				parent_sym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
				parent_sym.setProperty("method", meth);
				parent_sym.setProperty("preferred_formats", pref_list);
				parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
				if (annotate_seq) {
					seq.addAnnotation(parent_sym);
				}
				meth2csym.put(meth, parent_sym);
			}
			return parent_sym;
		}

		/**
		 *  Resorts child syms of a mutable symmetry in either ascending order if
		 *   sym's span on sortseq is forward, or descending if sym's span on sortseq is reverse,
		 *   based on child sym's span's start position on BioSeq sortseq.
		 */
		public static void resortChildren(MutableSeqSymmetry psym, BioSeq sortseq)  {
			SeqSpan pspan = psym.getSpan(sortseq);
			boolean ascending = pspan.isForward();
			//    System.out.println("sortseq: " + sortseq.getID() + ", child list: " + child_count);
			//    System.out.println("sortseq: " + sortseq.getID());
			//    SeqUtils.printSymmetry(psym);
			int child_count = psym.getChildCount();
			if (child_count > 0) {
				List<SeqSymmetry> child_list = new ArrayList<SeqSymmetry>(child_count);
				for (int i=0; i<child_count; i++) {
					SeqSymmetry csym = psym.getChild(i);
					if (csym.getSpan(sortseq) != null) {
						child_list.add(psym.getChild(i));
					}
				}
				psym.removeChildren();
				Comparator<SeqSymmetry> comp = new SeqSymStartComparator(sortseq, ascending);
				Collections.sort(child_list, comp);
				for (SeqSymmetry child : child_list) {		
					psym.addChild(child);
				}
			}
		}

		static final Pattern directive_version = Pattern.compile("##gff-version\\s+(.*)");
		static final Pattern directive_filter = Pattern.compile("##IGB-filter-(include |exclude |clear)(.*)");
		static final Pattern directive_hierarchy = Pattern.compile("##IGB-filter-hierarchy (.*)");
		static final Pattern directive_group_by = Pattern.compile("##IGB-group-by (.*)");
		static final Pattern directive_group_from_first = Pattern.compile("##IGB-group-properties-from-first-member (true|false)");
		static final Pattern directive_index_field = Pattern.compile("##IGB-group-id-field (.*)");

		boolean use_hierarchy = false;
		Map<String,Integer> hierarchy_levels = new HashMap<String,Integer>(); // Map of String to Integer
		Map<String,String> hierarchy_id_fields = new HashMap<String,String>(); // Map of String to String

		/**
		 *  Process directive lines in the input, which are lines beginning with "##".
		 *  Directives that are not understood are treated as comments.
		 *  Directives that are understood include "##IGB-filter-include x y z",
		 *  "##IGB-filter-exclude a b c", "##IGB-filter-clear", "##IGB-group-by x",
		 *  and "##IGB-hierarchy 3 exon <exon_id>".
		 */
		void processDirective(String line) throws IOException {
			Matcher m = directive_version.matcher(line);
			if (m.matches()) {
				String vstr = m.group(1).trim();
				try {
					int vers = (int)(Float.parseFloat(vstr));
					if (DEBUG) {
						System.out.println("parsing GFF, setting version to: " + vers);
					}
					setGffVersion(vers);
				}
				catch (Exception ex) {
					System.err.println("could not parse \"##gff-version\" pragma line: " + line);
					ex.printStackTrace();
				}
				return;
			}
			m = directive_filter.matcher(line);
			if (m.matches()) {
				resetFilters();
				String[] feature_types = m.group(2).split(" ");
				for (int i=0;i<feature_types.length; i++) {
					String feature_type = feature_types[i].trim();
					if (feature_type.length() > 0) {
						addFeatureFilter(feature_type, "include ".equals(m.group(1)));
					}
				}
				return;
			}

			m = directive_group_by.matcher(line);
			if (m.matches()) {
				String group = m.group(1).trim();
				if (group.length() > 0) {
					setGroupTag(group);
				} else {
					setGroupTag(null);
				}
				return;
			}

			m = directive_group_from_first.matcher(line);
			if (m.matches()) {
				String true_false = m.group(1).trim();
				use_first_one_as_group = "true".equals(true_false);
				return;
			}

			m = directive_index_field.matcher(line);
			if (m.matches()) {
				group_id_field_name = m.group(1).trim();
				return;
			}

			m = directive_hierarchy.matcher(line);
			if (m.matches()) {
				if (! use_hierarchy) {
					// If this is the first time the tag is seen, reset the filters
					resetFilters();
				}
				String hierarchy_string = m.group(1).trim();

				// Patern: repetition of:  [spaces]Integer[spaces]Name[spaces]<ID_field_name>
				// The ID field is optional.
				// Example:  2 psr  3 probeset <probeset_name> 4 probe <probe_id>
				Pattern p = Pattern.compile("\\s*([0-9]+)\\s*(\\S*)(\\s*<(\\S*)>)?");

				Matcher mm = p.matcher(hierarchy_string);
				while (mm.find()) {
					String level_string = mm.group(1);
					String feature_type = mm.group(2);
					Integer level = Integer.valueOf(level_string);
					hierarchy_levels.put(feature_type, level);
					addFeatureFilter(feature_type, true); // include only the items mentioned in the hierarchy

					if (DEBUG) {
					System.out.println("  Hierarchical parsing level: "+feature_type+" -> "+level);
					}
					String id_field = mm.group(4);
					if (id_field != null) {hierarchy_id_fields.put(feature_type, id_field);}
				}
				if (hierarchy_levels.isEmpty()) {
					throw new IOException("The '##IGB-filter-hierarchy' directive could not be parsed");
				} else {
					use_hierarchy = true;
				}
				return;
			}


			// Issue warnings about directives that aren't understood only for "##IGB-" directives.
			if (line.startsWith("##IGB")) {
				System.out.println("WARNING: GFF/GTF processing directive not understood: '"+line+"'");
			}
		}


		/**
		 *  Parse GFF attributes field into a properties on the Map. Each entry is
		 *  key = attribute tag, value = attribute values, with following restrictions:
		 *    if single value for a key, then hash.get(key) = value
		 *    if no value for a key, then hash.get(key) = key
		 *    if multiple values for a key, then hash.get(key) = List vec,
		 *         and each value is an element in vec
		 */
		public static void processAttributes(Map<String,Object> m, String attributes) {
			List<String> vals = new ArrayList<String>();
			String[] attarray = att_regex.split(attributes);
			for (int i=0; i<attarray.length; i++) {
				String att = attarray[i];
				Matcher tag_matcher = tag_regex.matcher(att);
				if (tag_matcher.find()) {
					String tag = tag_matcher.group(1);

					int index = tag_matcher.end(1);
					Matcher value_matcher = value_regex.matcher(att);
					boolean matches = value_matcher.find(index);
					while (matches) {

						String group1 = value_matcher.group(1);
						String group2 = value_matcher.group(2);
						if (group1 != null) {
							vals.add(group1);
						}
						else {
							vals.add(group2);
						}
						matches = value_matcher.find();
					}
					// common case where there's only one value for a tag,
					//  so hash the tag to that value
					if (vals.size() == 1) {
						Object the_object = vals.get(0);
						m.put(tag, the_object);
						vals.clear();
					}
					// rare case -- if no value for the tag, hash the tag to itself...
					else if (vals.size() == 0) {
						m.put(tag, tag);
						vals.clear();
					}
					// not-so-common case where there's multiple values for a tag,
					//   so hash the tag to the List/List of all the values,
					//   and make a new List for next tag-value entry
					else {
						m.put(tag, vals);
						vals = new ArrayList<String>();
					}
				}
			}  // end attribute processing
		}

		/**
		 *  Sets the parser to some standard settings that filter-out "intron" and
		 *  "transcript" lines, among other things, and groups by "transcript_id".
		 */
		public void setUseStandardFilters(boolean b) {
			// "standard" filters may depend on whether file is GFF1, GFF2, GTF, or GFF3
			addFeatureFilter("intron");
			addFeatureFilter("splice3");
			addFeatureFilter("splice5");
			addFeatureFilter("splice_donor");
			addFeatureFilter("splice_acceptor");
			addFeatureFilter("prim_trans");
			addFeatureFilter("transcript");
			addFeatureFilter("gene");
			addFeatureFilter("cluster");
			addFeatureFilter("psr");
			addFeatureFilter("link");
			addFeatureFilter("chromosome");

			if (DEBUG) {
			System.out.println("group tag: transcript_id");
			}
			setGroupTag("transcript_id");
		}

		public void setGffVersion(int version) {
			// is use_standard_filters, then reset filters whenever gff version is set
			gff_version = version;
			if (gff_version != 3) {
				setUseStandardFilters(use_standard_filters);
			}
		}

		static final Integer TWO = Integer.valueOf(2);

		int number_of_duplicate_warnings = 0;

		public String hackGff3GroupId(String atts) {
			String groupid = null;
			String featid = null;
			String[] tagvals = att_regex.split(atts);
			for (int i=0; i<tagvals.length; i++) {
				String tagval = tagvals[i];
				String[] tv = gff3_tagval_splitter.split(tagval);
				String tag = tv[0];
				String val = tv[1];
				//      String vals = gff3_multival_splitter.split(val);
				if (tag.equals(GFF3_PARENT)) {
					groupid = val;
				}
				else if (tag.equals(GFF3_ID)) {
					featid = val;
					Object obj = gff3_id_hash.get(featid);
					if (obj == null) {
						gff3_id_hash.put(featid, featid);
					}
					else {
						if (obj instanceof String) {
							gff3_id_hash.put(featid, TWO);
							featid = featid + "_1";
						}
						else if (obj instanceof Integer) {
							Integer iobj = (Integer)obj;
							int fcount = iobj.intValue();
							gff3_id_hash.put(featid, Integer.valueOf(fcount+1));
							featid = featid + "_" + iobj.toString();
						}
						if (number_of_duplicate_warnings++ <= 10) {
							System.out.println("duplicate feature id, new id: " + featid);
							if (number_of_duplicate_warnings == 10) {
								System.out.println("(Suppressing further warnings about duplicate ids");
							}
						}
					}
				}
			}
			if (groupid == null) {
				return featid;
			}
			else  {
				return groupid;
			}
		}

		public String determineGroupId(SymWithProps sym, String group_tag) {
			String group_id = null;
			if ((group_tag != null)) {
				if (gff_version == GFF3) {
					System.out.println("shouldn't get here, GFF3 should have been transformed to look like GFF1");
				}
				else {
					Object value = sym.getProperty(group_tag);
					if (value != null) {
						if (value instanceof String) {
							group_id = (String) value;
							if (GROUP_ID_TO_LOWER_CASE) { group_id = group_id.toLowerCase(); }
						}
						else if (value instanceof Number) {
							group_id = "" + value;
						}
						else if (value instanceof Character) {
							group_id = "" + value;
						}
						else if (value instanceof List) {  // such as a Vector
							// If there are multiple values for the group_tag, then take first one as String value
							List valist = (List)value;
							if ((valist.size() > 0) && (valist.get(0) instanceof String)) {
								group_id = (String) valist.get(0);
								if (GROUP_ID_TO_LOWER_CASE) { group_id = group_id.toLowerCase(); }
							}
						}
					}
				}
			}
			return group_id;
		}

		/**
		 *  Assumes that the sym being output is of depth = 2 (which UcscPslSyms are).
		 */
		public static void outputGffFormat(SymWithProps psym, BioSeq seq, Writer wr)
			throws IOException {
			//  public static void outputGffFormat(UcscPslSym psym, BioSeq seq, Writer wr) throws IOException  {
			int childcount = psym.getChildCount();
			String meth = (String)psym.getProperty("source");
			if (meth == null) { meth = (String)psym.getProperty("type"); }
			//    String id = (String)psym.getProperty("id");
			String group = (String)psym.getProperty("group");
			if (group == null) { group = psym.getID(); }
			if (group == null) { group = (String)psym.getProperty("id"); }

			for (int i=0; i<childcount; i++) {
				SeqSymmetry csym = psym.getChild(i);
				SeqSpan span = csym.getSpan(seq);

				// GFF ==> seqname source feature start end score strand frame group
				wr.write(seq.getID()); // seqname
				wr.write('\t');

				// source
				if (meth != null)  { wr.write(meth); }
				else { wr.write("unknown_source"); }
				wr.write('\t');

				String child_type = null;
				SymWithProps cwp = null;
				if (csym instanceof SymWithProps) {
					cwp = (SymWithProps)csym;
					child_type = (String)cwp.getProperty("type");
				}
				if (child_type != null) { wr.write(child_type); }
				else  { wr.write("unknown_feature_type"); }
				wr.write('\t');

				wr.write(Integer.toString(span.getMin()+1)); wr.write('\t');  // start
				wr.write(Integer.toString(span.getMax())); wr.write('\t');  // end

				// score
				Object score = null;
				if (cwp != null) { score = cwp.getProperty("score"); }
				if (score != null) { wr.write(score.toString()); }
				else { wr.write("."); }
				wr.write('\t');

				// strand
				if (span.isForward()) { wr.write("+"); } else { wr.write("-"); }
				wr.write('\t');

				// frame
				Object frame = null;
				if (cwp != null)  { frame = cwp.getProperty("frame"); }
				if (frame != null) { wr.write(frame.toString()); }
				else {  wr.write('.'); }
				wr.write('\t');  // frame

				// group
				//      if (id != null) { wr.write(id); }
				if (group != null) { wr.write(group); }

				wr.write('\n');
			}
			}

	// assumes that seqid for outputting in GFF format is id of sym's first span's BioSeq
	// currently type is ignored
	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, String type, OutputStream outstream) {
		boolean success = true;
		int count = 0;
		if (DEBUG) {
			System.out.println("in GFFParser.writeAnnotations()");
		}
		try {
			Writer bw = new BufferedWriter(new OutputStreamWriter(outstream));
			Iterator iterator = syms.iterator();
			while (iterator.hasNext()) {
				count++;
				if (DEBUG) {
					if (count % 1000 == 0) {
						System.out.println("output count: " + count);
					}
				}
				SeqSymmetry sym = (SeqSymmetry) iterator.next();
				SeqSpan span = sym.getSpan(0);
				BioSeq seq = span.getBioSeq();
				if (sym instanceof SymWithProps) {
					outputGffFormat((SymWithProps) sym, seq, bw);
				} else {
					System.err.println("sym is not instance of SymWithProps");
				}
			}
			bw.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
			success = false;
		}
		System.out.println("total line count: " + count);
		return success;
	}

	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "GFF" format.
	 *  @param type  currently ignored
	 **/
	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
			String type, OutputStream outstream) {
		boolean success = true;
		if (DEBUG) {
			System.out.println("in GFFParser.writeAnnotations()");
		}
		try {
			Writer bw = new BufferedWriter(new OutputStreamWriter(outstream));
			Iterator iterator = syms.iterator();
			while (iterator.hasNext()) {
				SeqSymmetry sym = (SeqSymmetry) iterator.next();
				if (sym instanceof SymWithProps) {
					outputGffFormat((SymWithProps) sym, seq, bw);
				} else {
					System.err.println("sym is not instance of SymWithProps");
				}
			}
			bw.flush();
		} catch (Exception ex) {
			ex.printStackTrace();
			success = false;
		}
		return success;
	}

	/**
	 *  Implementing AnnotationWriter interface to write out annotations
	 *    to an output stream as "GFF" format.
	 **/
	public String getMimeType() {
		return "text/plain";
	}

	public void setUseDefaultSource(boolean useDefaultSource) {
		this.useDefaultSource = useDefaultSource;
	}

	public void setUseTrackLines(boolean use_track_lines) {
    	this.use_track_lines = use_track_lines;
    }
}
