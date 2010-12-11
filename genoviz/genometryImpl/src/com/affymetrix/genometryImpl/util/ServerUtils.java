package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.comparator.MatchToListComparator;
import com.affymetrix.genometryImpl.comparator.GenomeVersionDateComparator;
import com.affymetrix.genometryImpl.das2.SimpleDas2Type;
import com.affymetrix.genometryImpl.AnnotSecurity;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.SearchableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.das.DasServerInfo;
import com.affymetrix.genometryImpl.das2.Das2ServerInfo;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.parsers.LiftParser;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.parsers.ProbeSetDisplayPlugin;
import com.affymetrix.genometryImpl.parsers.useq.USeqUtilities;
import com.affymetrix.genometryImpl.symloader.*;
import com.affymetrix.genometryImpl.util.IndexingUtils.IndexedSyms;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Utils for DAS/2 and other servers.
 */
public abstract class ServerUtils {
	private static final String annots_filename = "annots.xml"; // potential originalFile for annots parsing
	private static final String graph_dir_suffix = ".graphs.seqs";
	private static final boolean SORT_SOURCES_BY_ORGANISM = true;
	private static final boolean SORT_VERSIONS_BY_DATE_CONVENTION = true;
	private static final Pattern interval_splitter = Pattern.compile(":");
	
	private static final String modChromInfo = "mod_chromInfo.txt";
	private static final String liftAll = "liftAll.lft";
	public static final List<String> BAR_FORMATS = new ArrayList<String>();

	static {
		BAR_FORMATS.add("bar");
	}

	public static void parseChromosomeData(File genome_directory, String genome_version) throws IOException {
		File chrom_info_file = new File(genome_directory, modChromInfo);
		if (chrom_info_file.exists()) {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.INFO,
					"parsing {0} for: {1}", new Object[]{modChromInfo,genome_version});
			InputStream chromstream = new FileInputStream(chrom_info_file);
			try {
				ChromInfoParser.parse(chromstream, GenometryModel.getGenometryModel(), genome_version);
			} finally {
				GeneralUtils.safeClose(chromstream);
			}
		} else {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.INFO,
					"couldn't find {0} for: {1}", new Object[]{modChromInfo,genome_version});
			Logger.getLogger(ServerUtils.class.getName()).log(Level.INFO,
					"looking for {0} instead", liftAll);
			File lift_file = new File(genome_directory, "liftAll.lft");
			if (lift_file.exists()) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.INFO,
						"parsing {0} for: {1}", new Object[]{liftAll,genome_version});
				InputStream liftstream = new FileInputStream(lift_file);
				try {
					LiftParser.parse(liftstream, GenometryModel.getGenometryModel(), genome_version);
				} finally {
					GeneralUtils.safeClose(liftstream);
				}
			} else {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.SEVERE,
						"couldn't find {0} or {1} for genome!!! {2}", new Object[]{modChromInfo, liftAll, genome_version});
			}
		}
	}

	/**Loads a originalFile's lines into a hash first column is the key, second the value.
	 * Skips blank lines and those starting with a '#'
	 * @return null if an exception in thrown
	 * */
	public static HashMap<String, String> loadFileIntoHashMap(File file) {
		BufferedReader in = null;
		HashMap<String, String> names = new HashMap<String, String>();
		try {
			in = new BufferedReader(new FileReader(file));
			String line;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				String[] keyValue = line.split("\\s+");
				if (keyValue.length < 2) {
					continue;
				}
				names.put(keyValue[0], keyValue[1]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			GeneralUtils.safeClose(in);
		}
		return names;
	}

	/**
	 * Load synonyms from file into lookup.
	 * @param symfile
	 * @param lookup
	 */
	public static void loadSynonyms(File synfile, SynonymLookup lookup) {
		if (synfile.exists()) {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.INFO,
					"Synonym file {0} found, loading synonyms", synfile.getName());
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(synfile);
				lookup.loadSynonyms(fis);
			} catch (Exception ex) {
				ex.printStackTrace();
			} finally {
				GeneralUtils.safeClose(fis);
			}
		} else {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.INFO,
					"Synonym file {0} not found, therefore not using synonyms", synfile.getName());
		}
	}

	/** sorts genomes and versions within genomes
	 *  sort genomes based on "organism_order.txt" config originalFile if present
	 * @param organisms
	 * @param org_order_filename
	 */
	public static void sortGenomes(Map<String, List<AnnotatedSeqGroup>> organisms, String org_order_filename) {
		// sort genomes based on "organism_order.txt" config originalFile if present
		// get Map.Entry for organism, sort based on order in organism_order.txt,
		//    put in order in new LinkedHashMap(), then replace as organisms field
		File org_order_file = new File(org_order_filename);
		if (SORT_SOURCES_BY_ORGANISM && org_order_file.exists()) {
			Comparator<String> org_comp = new MatchToListComparator(org_order_filename);
			List<String> orglist = new ArrayList<String>(organisms.keySet());
			Collections.sort(orglist, org_comp);
			Map<String, List<AnnotatedSeqGroup>> sorted_organisms = new LinkedHashMap<String, List<AnnotatedSeqGroup>>();
			for (String org : orglist) {
				sorted_organisms.put(org, organisms.get(org));
			}
			organisms = sorted_organisms;
		}
		if (SORT_VERSIONS_BY_DATE_CONVENTION) {
			Comparator<AnnotatedSeqGroup> date_comp = new GenomeVersionDateComparator();
			for (List<AnnotatedSeqGroup> versions : organisms.values()) {
				Collections.sort(versions, date_comp);
			}
		}
	}

	/**
	 * Load annotations from root of genome directory.
	 * @param genomeDir
	 * @param genome
	 * @param graph_name2dir
	 * @param graph_name2file
	 * @param dataRoot
	 */
	public static void loadAnnots(
			File genomeDir,
			AnnotatedSeqGroup genome,
			Map<AnnotatedSeqGroup, List<AnnotMapElt>> annots_map,
			Map<String, String> graph_name2dir,
			Map<String, String> graph_name2file,
			String dataRoot) throws IOException {
		if (genomeDir.isDirectory()) {
			ServerUtils.loadAnnotsFromDir(
					genomeDir.getName(), genome, genomeDir, "", annots_map, graph_name2dir, graph_name2file, dataRoot);
		} else {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING, ""
					+ "{0} is not a directory.  Skipping.", genomeDir.getAbsolutePath());
		}
	}

	/**
	 * if ".seqs" suffix, then handle as graphs
	 * otherwise recursively call on each child files;
	 * @param type_name
	 * @param genome
	 * @param current_file
	 * @param new_type_prefix
	 * @param graph_name2dir
	 * @param graph_name2file
	 * @param dataRoot
	 */
	private static void loadAnnotsFromDir(
			String type_name,
			AnnotatedSeqGroup genome,
			File current_file,
			String new_type_prefix,
			Map<AnnotatedSeqGroup,List<AnnotMapElt>> annots_map,
			Map<String, String> graph_name2dir,
			Map<String, String> graph_name2file,
			String dataRoot) throws IOException {
		File annot = new File(current_file, annots_filename);
		if (annot.exists()) {
			FileInputStream istr = null;
			try {
				istr = new FileInputStream(annot);

				List<AnnotMapElt> annotList = annots_map.get(genome);
				if (annotList == null) {
					annotList = new ArrayList<AnnotMapElt>();
					annots_map.put(genome, annotList);
				}
				AnnotsXmlParser.parseAnnotsXml(istr, annotList);
			} catch (FileNotFoundException ex) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(istr);
			}
		}


		if (type_name.endsWith(graph_dir_suffix)) {
			// each originalFile in directory is same annotation type, but for a single originalSeq?
			// assuming bar files for now, each with starting with originalSeq id?
			String graph_name = type_name.substring(0, type_name.length() - graph_dir_suffix.length());
			Logger.getLogger(ServerUtils.class.getName()).log(Level.FINE,
					"@@@ adding graph directory to types: {0}, path: {1}", new Object[]{graph_name, current_file.getPath()});
			graph_name2dir.put(graph_name, current_file.getPath());
			genome.addType(graph_name, null);
		} else {
			File[] child_files = current_file.listFiles(new HiddenFileFilter());
			Arrays.sort(child_files);
			for (File child_file : child_files) {
				loadAnnotsFromFile(child_file, genome, new_type_prefix, annots_map, graph_name2dir, graph_name2file, dataRoot);
			}
		}
	}


	/**
	 * see if can parse as annotation originalFile.
	 * @param current_file
	 * @param genome
	 * @param type_prefix
	 * @param graph_name2dir
	 * @param graph_name2file
	 * @param dataRoot
	 */
	private static void loadAnnotsFromFile(File current_file, AnnotatedSeqGroup genome, String type_prefix,
			Map<AnnotatedSeqGroup,List<AnnotMapElt>> annots_map,
			Map<String, String> graph_name2dir,
			Map<String, String> graph_name2file,
			String dataRoot) throws IOException {
		String file_name = current_file.getName();
		String type_name = type_prefix + file_name;
		
		// if current originalFile is directory, then descend down into child files
		if (current_file.isDirectory()) {
			String new_type_prefix = type_name + "/";
			loadAnnotsFromDir(
					type_name, genome, current_file, new_type_prefix, annots_map, graph_name2dir, graph_name2file, dataRoot);
			return;
		}

		if (isSequenceFile(current_file)
				|| isGraph(current_file, type_name, graph_name2file, genome)
				|| isAnnotsFile(current_file)) {
			return;
		}

		if(isSymLoader(current_file)){
			String stream_name = GeneralUtils.getUnzippedName(current_file.getName());
			String extension = ParserController.getExtension(stream_name);
			List<AnnotMapElt> annotList = annots_map.get(genome);
			String annotTypeName = ParserController.getAnnotType(annotList, current_file.getName(), extension, type_name);
			genome.addType(annotTypeName, null);
			for (BioSeq originalSeq : genome.getSeqList()) {
				SymLoader symloader = determineLoader(extension, current_file.toURI(), type_name, genome);
				originalSeq.addSymLoader(annotTypeName, symloader);
			}
			return;
		}
		
		if (!annots_map.isEmpty() && annots_map.containsKey(genome)) {
			if (AnnotMapElt.findFileNameElt(file_name, annots_map.get(genome)) == null) {
				// we have loaded in an annots.xml originalFile, but yet this originalFile is not in it and should be ignored.
				Logger.getLogger(ServerUtils.class.getName()).log(Level.INFO,
						"Ignoring file {0} which was not found in annots.xml", file_name);
				return;
			}
		}

		// current originalFile is not a directory, so try and recognize as annotation file
		indexOrLoadFile(dataRoot, current_file, type_name, annots_map, genome, null);
	}

	private static boolean isSequenceFile(File current_file) {
		return (current_file.getName().equals("mod_chromInfo.txt") || current_file.getName().equals("liftAll.lft"));
	}

	public static boolean isResidueFile(String format){
		return (format.equalsIgnoreCase("bnib") || format.equalsIgnoreCase("fa") ||
				format.equalsIgnoreCase("2bit"));
	}

	private static boolean isAnnotsFile(File current_file) {
		return current_file.getName().equals("annots.xml");
	}


	private static boolean isGraph(File current_file, String type_name, Map<String, String> graph_name2file, AnnotatedSeqGroup genome) {
		String file_name = current_file.getName();
		if (file_name.endsWith(".bar") || USeqUtilities.USEQ_ARCHIVE.matcher(file_name).matches()) {
			//if a bar or useq archive don't load just add
			// special casing so bar files are seen in types request, but not parsed in on startup
			//    (because using graph slicing so don't have to pull all bar originalFile graphs into memory)
			String file_path = current_file.getPath();
			Logger.getLogger(ServerUtils.class.getName()).log(Level.FINE,
					"@@@ adding graph file to types: {0}, path: {1}", new Object[]{type_name, file_path});
			graph_name2file.put(type_name, file_path);
			genome.addType(type_name, null);
			return true;
		}
		return false;
	}

	private static boolean isSymLoader(File current_file){
		String stream_name = GeneralUtils.getUnzippedName(current_file.getName());
		String extension = ParserController.getExtension(stream_name);
		extension = extension.substring(extension.indexOf('.') + 1);
		
		return (extension.endsWith("bam") || isResidueFile(extension));
	}

	/**
	 *   If current_file is directory:
	 *       if ".seqs" suffix, then handle as graphs
	 *       otherwise recursively call on each child files;
	 *   if not directory, see if can parse as annotation file.
	 *   if type prefix is null, then at top level of genome directory, so make type_prefix = "" when recursing down
	 */
	public static void loadGenoPubAnnotsFromFile(String dataroot,
			File current_file,
			AnnotatedSeqGroup genome,
			Map<AnnotatedSeqGroup,List<AnnotMapElt>> annots_map,
			String type_prefix,
			Integer annot_id,
			Map<String,String> graph_name2file) throws FileNotFoundException, IOException {

		if (isGenoPubSequenceFile(current_file)
				|| isGenoPubGraph(current_file, type_prefix, graph_name2file, genome, annot_id)) {
			return;
		}

		// current originalFile is not a directory, so try and recognize as annotation file
		indexOrLoadFile(dataroot, current_file, type_prefix, annots_map, genome, annot_id);
	}


	public static void loadGenoPubAnnotFromDir(String type_name,
			String file_path,
			AnnotatedSeqGroup genome,
			File current_file,
			Integer annot_id,
			Map<String,String> graph_name2dir) {
		// each file in directory is same annotation type, but for a single seq?
		// assuming bar files for now, each with starting with seq id?
		//  String graph_name = file_name.substring(0, file_name.length() - graph_dir_suffix.length());
		Logger.getLogger(ServerUtils.class.getName()).log(Level.FINE,
				"@@@ adding graph directory to types: {0}, path: {1}", new Object[]{type_name, file_path});
		graph_name2dir.put(type_name, file_path);
		genome.addType(type_name, annot_id);

	}

	public static void unloadGenoPubAnnot(String type_name,
			AnnotatedSeqGroup genome,
			Map<String,String> graph_name2dir) {
		
		
		if (graph_name2dir.containsKey(type_name)) {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.FINE,
					"@@@ removing graph directory to types: {0}", type_name);
			graph_name2dir.remove(type_name);
			
		}  else {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.FINE,
					"@@@ removing annotation {0}", type_name);
			List<BioSeq> seqList = genome.getSeqList();
			for (BioSeq aseq : seqList) {
				SymWithProps tannot = aseq.getAnnotation(type_name);			
				if (tannot != null) {
					aseq.unloadAnnotation(tannot);
					tannot = null;
				} else {
					IndexedSyms iSyms = aseq.getIndexedSym(type_name);
					if (iSyms != null) {
						if (!aseq.removeIndexedSym(type_name)) {
							Logger.getLogger(ServerUtils.class.getName()).log(
									Level.WARNING, "Unable to remove indexed annotation {0}", type_name);
						}
						iSyms = null;
					}
				}
			}
		}
		genome.removeType(type_name);

	}
	


	private static boolean isGenoPubSequenceFile(File current_file) {
		return (current_file.getName().equals("mod_chromInfo.txt") || current_file.getName().equals("liftAll.lft"));
	}

	private static boolean isGenoPubGraph(File current_file, String type_prefix, Map<String, String> graph_name2file, AnnotatedSeqGroup genome, Integer annot_id) {
		String file_name = current_file.getName();
		if (file_name.endsWith(".bar") || USeqUtilities.USEQ_ARCHIVE.matcher(file_name).matches()) {
			String file_path = current_file.getPath();
			// special casing so bar files are seen in types request, but not parsed in on startup
			//    (because using graph slicing so don't have to pull all bar file graphs into memory)
			Logger.getLogger(ServerUtils.class.getName()).log(Level.FINE,
					"@@@ adding graph file to types: {0}, path: {1}", new Object[]{type_prefix, file_path});
			graph_name2file.put(type_prefix, file_path);
			genome.addType(type_prefix, annot_id);
			return true;
		}
		return false;
	}


	/**
	 * Index the file, if possible or load the file.
	 * @param dataRoot -- root of data directory
	 * @param file -- file to load or index
	 * @param annot_name
	 * @param annots_map
	 * @param genome
	 * @param annot_id
	 * @throws java.io.FileNotFoundException
	 * @throws java.io.IOException
	 */
	private static void indexOrLoadFile(
			String dataRoot,
			File file,
			String annot_name,
			Map<AnnotatedSeqGroup,List<AnnotMapElt>> annots_map,
			AnnotatedSeqGroup genome,
			Integer annot_id)
			throws FileNotFoundException, IOException {

		String stream_name = GeneralUtils.getUnzippedName(file.getName());
		IndexWriter iWriter = ParserController.getIndexWriter(stream_name);

		List<AnnotMapElt> annotList = annots_map.get(genome);

		String extension = ParserController.getExtension(stream_name);	// .psl, .bed, et cetera
		
		// If the annotation id was passed in, the server is running in genopub 
		// mode, so use the annotation name; otherwise, the server is running in 
		// classic mode,so use the file directory path and the file name to 
		// formulate the name.
		String annotTypeName = ParserController.getAnnotType(annotList, file.getName(), extension, annot_name);

		genome.addType(annotTypeName, annot_id);

		AnnotatedSeqGroup tempGenome = AnnotatedSeqGroup.tempGenome(genome);

		if (iWriter == null) {	
			loadAnnotFile(file, annotTypeName, annotList, genome, false);
			getAddedChroms(genome, tempGenome, false);
			getAlteredChroms(genome, tempGenome, false);
			// Not yet indexable
			return;
		}

		List loadedSyms = loadAnnotFile(file, annotTypeName, annotList, tempGenome, true);
		getAddedChroms(tempGenome, genome, true);
		getAlteredChroms(tempGenome, genome, true);

		String returnTypeName = annotTypeName;
		if (stream_name.endsWith(".link.psl")) {
			// Nasty hack necessary to add "netaffx consensus" to type names returned by GetGenomeType
			returnTypeName = annotTypeName + " " + ProbeSetDisplayPlugin.CONSENSUS_TYPE;
		}

		ServerUtils.createDirIfNecessary(IndexingUtils.indexedGenomeDirName(dataRoot, genome));

		IndexingUtils.determineIndexes(genome,
				tempGenome, dataRoot, file, loadedSyms, iWriter, annotTypeName, returnTypeName);
	}	

	public static boolean createDirIfNecessary(String dirName) {
		// Make sure the appropriate .indexed/species/version/chr directory exists.
		// If not, create it.
		File newFile = new File(dirName);
		if (!newFile.exists()) {
			if (!new File(dirName).mkdirs()) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.SEVERE,
						"Couldn''t create directory: {0}", dirName);
				return false;
			} else {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.FINE,
						"Created new directory: {0}", dirName);
			}
		}
		return true;
	}

	/**
	 * Load an annotations file (indexed or non-indexed), and return the symmetries.
	 * @param current_file
	 * @param type_name
	 * @param annotList
	 * @param genome
	 * @param isIndexed
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static List loadAnnotFile(File current_file, String type_name, List<AnnotMapElt> annotList, AnnotatedSeqGroup genome, boolean isIndexed) throws FileNotFoundException, IOException {
		String stream_name = GeneralUtils.getUnzippedName(current_file.getName());
		InputStream istr = null;
		try {
			istr = GeneralUtils.getInputStream(current_file, new StringBuffer());
			if (!isIndexed) {
				return ParserController.parse(istr, annotList, stream_name, genome, type_name);
			}
			return ParserController.parseIndexed(istr, annotList, stream_name, genome, type_name);
		} finally {
			GeneralUtils.safeClose(istr);
		}
	}


	/**
	 *  Differs from Das2FeatureSaxParser.getLocationSpan():
	 *     Won't add unrecognized seqids or null groups
	 *     If rng is null or "", will set to span to [0, originalSeq.getLength()]
	 */
	public static SeqSpan getLocationSpan(String seqid, String rng, AnnotatedSeqGroup group) {
		if (seqid == null || group == null) {
			return null;
		}
		BioSeq seq = group.getSeq(seqid);
		if (seq == null) {
			return null;
		}
		int min;
		int max;
		boolean forward = true;
		if (rng == null) {
			min = 0;
			max = seq.getLength();
		} else {
			try {
				String[] subfields = interval_splitter.split(rng);
				min = Integer.parseInt(subfields[0]);
				max = Integer.parseInt(subfields[1]);
				if (subfields.length >= 3) {  // in DAS/2 strandedness is not allowed for range query params, but accepting it here
					if (subfields[2].equals("-1")) {
						forward = false;
					}
				}
			} catch (Exception ex) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.INFO,
						"Problem parsing a query parameter range filter: {0}", rng);
				return null;
			}
		}
		SeqSpan span;
		if (forward) {
			span = new SimpleSeqSpan(min, max, seq);
		} else {
			span = new SimpleSeqSpan(max, min, seq);
		}
		return span;
	}

	/**
	 * Retrieve symmetries meeting query constraints.
	 * @param overlap_span
	 * @param query_type - annotation "type", which is feature name.
	 * @param inside_span
	 * @return
	 */
	public static List<SeqSymmetry> getIntersectedSymmetries(SeqSpan overlap_span, String query_type, SeqSpan inside_span) {
		List<SeqSymmetry> result =
				ServerUtils.getOverlappedSymmetries(overlap_span, query_type);
		if (result == null) {
			result = Collections.<SeqSymmetry>emptyList();
		}
		if (inside_span != null) {
			result = ServerUtils.specifiedInsideSpan(inside_span, result);
		}
		return result;
	}

	/**
	 *
	 *  Currently assumes:
	 *    query_span's originalSeq is a BioSeq (which implies top-level annots are TypeContainerAnnots)
	 *    only one IntervalSearchSym child for each TypeContainerAnnot
	 *  Should expand soon so loadedSyms can be returned from multiple IntervalSearchSyms children
	 *      of the TypeContainerAnnot
	 */
	public static List<SeqSymmetry> getOverlappedSymmetries(SeqSpan query_span, String annot_type) {
		BioSeq seq = query_span.getBioSeq();
		SymWithProps container = seq.getAnnotation(annot_type);
		if (container != null) {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.FINE,
					"non-indexed request for {0}", annot_type);
			int annot_count = container.getChildCount();
			for (int i = 0; i < annot_count; i++) {
				SeqSymmetry sym = container.getChild(i);
				if (sym instanceof SearchableSeqSymmetry) {
					SearchableSeqSymmetry target_sym = (SearchableSeqSymmetry) sym;
					return target_sym.getOverlappingChildren(query_span);
				}
			}
		} else {
			// Couldn't find it.  See if it's been indexed.
			Logger.getLogger(ServerUtils.class.getName()).log(Level.FINE,
					"indexed request for {0}", annot_type);
			IndexedSyms iSyms = seq.getIndexedSym(annot_type);
			if (iSyms != null) {
				return getIndexedOverlappedSymmetries(
						query_span,
						iSyms,
						annot_type,
						seq.getSeqGroup());
			}
		}
		return Collections.<SeqSymmetry>emptyList();
	}

	// if an inside_span specified, then filter out intersected symmetries based on this:
	//    don't return symmetries with a min < inside_span.min() or max > inside_span.max()  (even if they overlap query interval)
	public static <SymExtended extends SeqSymmetry> List<SymExtended> specifiedInsideSpan(
			SeqSpan inside_span, List<SymExtended> result) {
		int inside_min = inside_span.getMin();
		int inside_max = inside_span.getMax();
		BioSeq iseq = inside_span.getBioSeq();
		MutableSeqSpan testspan = new SimpleMutableSeqSpan();
		List<SymExtended> orig_result = result;
		result = new ArrayList<SymExtended>(orig_result.size());
		for (SymExtended sym : orig_result) {
			// fill in testspan with span values for sym (on aseq)
			sym.getSpan(iseq, testspan);
			if ((testspan.getMin() >= inside_min) && (testspan.getMax() <= inside_max)) {
				result.add(sym);
			}
		}
		Logger.getLogger(ServerUtils.class.getName()).log(Level.FINE,
				"  overlapping annotations that passed inside_span constraints: {0}", result.size());
		return result;
	}

	
	/**
	 * Get the list of symmetries.
	 * @param overlap_span
	 * @param iSyms
	 * @param annot_type
	 * @param group
	 * @return list of indexed overlapped symmetries
	 */
	public static List<SeqSymmetry> getIndexedOverlappedSymmetries(
			SeqSpan overlap_span,
			IndexedSyms iSyms,
			String annot_type,
			AnnotatedSeqGroup group) {

		List<? extends SeqSymmetry> symList = getIndexedSymmetries(overlap_span,iSyms,annot_type,group);

		// We need to filter this list to only return overlaps.
		// Due to the way indexing is implemented, there may have been additional symmetries outside of the specified interval.
		// This violates the DAS/2 specification, but more importantly, IGB gets confused.
		return filterForOverlappingSymmetries(overlap_span,symList);
	}


	/**
	 * Return only the symmetries that have some overlap with this span.
	 * Chromosome is not an issue; everything returned is on the same chromosome.
	 * @param overlapSpan
	 * @param symList
	 * @return list of overlapping seq symmetries
	 */
	public static List<SeqSymmetry> filterForOverlappingSymmetries(SeqSpan overlapSpan, List<? extends SeqSymmetry> symList) {
		List<SeqSymmetry> newList = new ArrayList<SeqSymmetry>(symList.size());
		for (SeqSymmetry sym : symList) {
			if (sym instanceof UcscPslSym) {
				UcscPslSym uSym = (UcscPslSym)sym;
				SeqSpan span = uSym.getSpan(uSym.getTargetSeq());
				if (!SeqUtils.overlap(span, overlapSpan)) {
					continue;
				}
				newList.add(sym);
				continue;
			}
			if (isOverlapping(sym, overlapSpan)) {
				newList.add(sym);
			}
		}
		return newList;
	}


	private static boolean isOverlapping(SeqSymmetry sym, SeqSpan overlapSpan) {
		int spanCount = sym.getSpanCount();
		for (int i = 0; i < spanCount; i++) {
			SeqSpan span = sym.getSpan(i);
			if (span != null && SeqUtils.overlap(span, overlapSpan)) {
				return true;
			}
		}
		return false;
	}



	/**
	 * Get the list of symmetries
	 * @param overlap_span
	 * @param iSyms
	 * @param annot_type
	 * @param group
	 * @return list of indexed seq symmetries
	 */
	private static List<? extends SeqSymmetry> getIndexedSymmetries(
			SeqSpan overlap_span,
			IndexedSyms iSyms,
			String annot_type,
			AnnotatedSeqGroup group) {

		InputStream newIstr = null;
		DataInputStream dis = null;
		try {
			int[] overlapRange = new int[2];
			int[] outputRange = new int[2];
			overlapRange[0] = overlap_span.getMin();
			overlapRange[1] = overlap_span.getMax();
			IndexingUtils.findMaxOverlap(overlapRange, outputRange, iSyms.min, iSyms.max);
			int minPos = outputRange[0];
			// We add 1 to the maxPos index.
			// Since filePos is recorded at the *beginning* of each line, this allows us to read the last element.
			int maxPos = outputRange[1] + 1;

			if (minPos >= maxPos) {
				// Nothing found, or invalid values passed in.
				return Collections.<SeqSymmetry>emptyList();
			}
			byte[] bytes = IndexingUtils.readBytesFromFile(
					iSyms.file, iSyms.filePos[minPos], (int) (iSyms.filePos[maxPos] - iSyms.filePos[minPos]));

			if ((iSyms.iWriter instanceof PSLParser || iSyms.iWriter instanceof PSL) && iSyms.file.getName().endsWith(".link.psl")) {
				String indexesFileName = iSyms.file.getAbsolutePath();
				newIstr = IndexingUtils.readAdditionalLinkPSLIndex(indexesFileName, annot_type, bytes);
			} else {
				newIstr = new ByteArrayInputStream(bytes);
			}
			dis = new DataInputStream(newIstr);

			return iSyms.iWriter.parse(dis, annot_type, group);
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		finally {
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(newIstr);
		}
	}


	


	// Print out the genomes
	public static void printGenomes(Map<String, List<AnnotatedSeqGroup>> organisms) {
		for (Map.Entry<String, List<AnnotatedSeqGroup>> ent : organisms.entrySet()) {
			String org = ent.getKey();
			Logger.getLogger(ServerUtils.class.getName()).log(Level.INFO, "Organism: {0}", org);
			for (AnnotatedSeqGroup version : ent.getValue()) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.INFO, 
						"    Genome version: {0}, organism: {1}, seq count: {2}",
						new Object[]{version.getID(), version.getOrganism(), version.getSeqCount()});
			}
		}
	}

	/**
	 *  Gets the list of types of annotations for a given genome version.
	 *  Assuming top-level annotations hold type info in property "method" or "meth".
	 *  @return a Map where keys are feature type Strings and values are 
	 *    instances of SimpleDas2Type, which contains a list graph formats and
	 *    a map of properties.
	 *
	 *  may want to cache this info (per versioned source) at some point...
	 */
	public static Map<String, SimpleDas2Type> getAnnotationTypes(
					String data_root,
					AnnotatedSeqGroup genome,
					AnnotSecurity annotSecurity) {
		List<BioSeq> seqList = genome.getSeqList();
		Map<String,SimpleDas2Type> genome_types = new LinkedHashMap<String,SimpleDas2Type>();
		for (BioSeq aseq : seqList) {
			for (String type : aseq.getTypeList()) {
				if (genome_types.get(type) != null) {
					continue;
				}
				List<String> flist = Collections.<String>emptyList();
				SymWithProps tannot = aseq.getAnnotation(type);
				SymWithProps first_child = (SymWithProps) tannot.getChild(0);
				if (first_child != null) {
					List formats = (List)first_child.getProperty("preferred_formats");
					if (formats != null) {
						flist = new ArrayList<String>(formats.size());
						for (Object o : formats) {
							flist.add((String) o);
						}
					}
				}
				
		        if (annotSecurity == null || isAuthorized(genome, annotSecurity, type)) {
					genome_types.put(type, new SimpleDas2Type(type, flist, getProperties(genome, annotSecurity, type)));
		        }
			}
			for (String type : aseq.getIndexedTypeList()) {
				if (genome_types.get(type) != null) {
					continue;
				}
				IndexedSyms iSyms = aseq.getIndexedSym(type);
				List<String> flist = new ArrayList<String>();
				flist.addAll(iSyms.iWriter.getFormatPrefList());
				
		        if (annotSecurity == null || isAuthorized(genome, annotSecurity, type)) {
					genome_types.put(type, new SimpleDas2Type(type, flist, getProperties(genome, annotSecurity, type)));
		        }

			}
		}
		return genome_types;
	}

	/**
	 * Add symloader types to map.
	 * @param genome
	 * @param types_hash
	 */
	public static void getSymloaderTypes(AnnotatedSeqGroup genome, AnnotSecurity annotSecurity, Map<String, SimpleDas2Type> genome_types) {
		for(BioSeq aseq : genome.getSeqList()){
			for(String type: aseq.getSymloaderList()){
				SymLoader sym = aseq.getSymLoader(type);
				if(genome_types.containsKey(type))
					return;

				if (annotSecurity == null || isAuthorized(genome, annotSecurity, type)) {
					genome_types.put(type, new SimpleDas2Type(type, sym.getFormatPrefList(), getProperties(genome, annotSecurity, type)));
		        }
			}
		}
	}

	/**
	 * Add graph types to the map.
	 * @param data_root
	 * @param genome
	 * @param annotSecurity
	 * @param genome_types
	 */
	public static void getGraphTypes(
		String data_root, AnnotatedSeqGroup genome, AnnotSecurity annotSecurity, Map<String, SimpleDas2Type> genome_types) {
		for (String type : genome.getTypeList()) {
			if (genome_types.containsKey(type) || !isAuthorized(genome, annotSecurity, type)) {
				continue;
			}

			if (annotSecurity == null) {
				// DAS2 "classic"
				if (USeqUtilities.USEQ_ARCHIVE.matcher(type).matches()) {
					genome_types.put(type, new SimpleDas2Type(genome.getID(), USeqUtilities.USEQ_FORMATS, getProperties(genome, annotSecurity, type)));
				} else {
					genome_types.put(type, new SimpleDas2Type(genome.getID(), BAR_FORMATS, getProperties(genome, annotSecurity, type)));
				}
				continue;
			}
			// GenoPub
			if (annotSecurity.isBarGraphData(data_root, genome.getID(), type, genome.getAnnotationId(type))) {
				genome_types.put(type, new SimpleDas2Type(genome.getID(), BAR_FORMATS, getProperties(genome, annotSecurity, type)));
			} else if (annotSecurity.isUseqGraphData(data_root, genome.getID(), type, genome.getAnnotationId(type))) {
				genome_types.put(type, new SimpleDas2Type(genome.getID(), USeqUtilities.USEQ_FORMATS, getProperties(genome, annotSecurity, type)));
			} else {
				Logger.getLogger(ServerUtils.class.getName()).log(
						Level.WARNING, "Non-graph annotation {0} encountered, but does not match known entry.  This annotation will not show in the types request.", type);
			}
		}
	}

	private static boolean isAuthorized(AnnotatedSeqGroup group, AnnotSecurity annotSecurity, String type) {
		boolean isAuthorized = annotSecurity == null || annotSecurity.isAuthorized(group.getID(), type, group.getAnnotationId(type));
		Logger.getLogger(AnnotatedSeqGroup.class.getName()).log(Level.FINE,
				"{0} Annotation {1} ID={2}", new Object[]{isAuthorized ? "Showing  " : "Blocking ", type, group.getAnnotationId(type)});
		return isAuthorized;
	}

	private static Map<String, Object> getProperties(AnnotatedSeqGroup group, AnnotSecurity annotSecurity, String type) {
		return annotSecurity == null ? null : annotSecurity.getProperties(group.getID(), type, group.getAnnotationId(type));
	}

	private static void getAddedChroms(AnnotatedSeqGroup newGenome, AnnotatedSeqGroup oldGenome, boolean isIgnored) {
		if (oldGenome.getSeqCount() == newGenome.getSeqCount()) {
			return;
		}

		Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
				"found {0} chromosomes instead of {1}",
				new Object[]{newGenome.getSeqCount(), oldGenome.getSeqCount()});
		if (isIgnored) {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
					"Due to indexing, this was ignored.");
		} else {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
					"The genome has been altered.");
		}

		// output the altered seq
		Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
				"Extra chromosomes : ");
		for (BioSeq seq : newGenome.getSeqList()) {
			BioSeq genomeSeq = oldGenome.getSeq(seq.getID());
			if (genomeSeq == null) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,seq.getID());
			}
		}
	}


	private static void getAlteredChroms(AnnotatedSeqGroup newGenome, AnnotatedSeqGroup oldGenome, boolean isIgnored) {
		List<String> alteredChromStrings = new ArrayList<String>();
		for (BioSeq seq : newGenome.getSeqList()) {
			BioSeq genomeSeq = oldGenome.getSeq(seq.getID());
			if (genomeSeq != null && genomeSeq.getLength() != seq.getLength()) {
				alteredChromStrings.add(
						seq.getID() + ":" + seq.getLength() + "(was " + genomeSeq.getLength() + ") ");
			}
		}

		if (alteredChromStrings.size() > 0) {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
					"altered chromosomes found for genome {0}. ", oldGenome.getID());
			if (isIgnored) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
						"Indexing; this may cause problems.");
			} else {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
						"The genome has been altered.");
			}
			// output the altered seq
			Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,"Altered chromosomes : ");
			for (String alteredChromString : alteredChromStrings) {
				Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
						alteredChromString);
			}
		}
	}

	/**
	 * Initialize the server.
	 *
	 * @param serverType
	 * @param url
	 * @param name
	 * @return initialized server
	 */
	public static Object getServerInfo(ServerType serverType, String url, String name) {
		Object info = null;

		try {
			if (serverType == ServerType.QuickLoad) {
				info = ServerUtils.formatURL(url, serverType);
			} else if (serverType == ServerType.DAS) {
				info = new DasServerInfo(url);
			} else if (serverType == ServerType.DAS2) {
				info = new Das2ServerInfo(url, name, false);
			}
		} catch (URISyntaxException e) {
			Logger.getLogger(ServerUtils.class.getName()).log(Level.WARNING,
					"Could not initialize {0} server with address: {1}", new Object[]{serverType, url});
			e.printStackTrace(System.out);
		}
		return info;
	}

	/**
	 * Format a URL based on the ServerType's requirements.
	 *
	 * @param url URL to format
	 * @param type type of server the URL represents
	 * @return formatted URL
	 */
	public static String formatURL(String url, ServerType type) {
		try {
			/* remove .. and // from URL */
			url = new URI(url).normalize().toASCIIString();
		} catch (URISyntaxException ex) {
			String message = "Unable to parse URL: '" + url + "'";
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, message, ex);
			throw new IllegalArgumentException(message, ex);
		}
		switch (type) {
			case DAS:
			case DAS2:
				while (url.endsWith("/")) {
					url = url.substring(0, url.length()-1);
				}
				return url;
			case QuickLoad:
				return url.endsWith("/") ? url : url + "/";
			default:
				return url;
		}
	}

	/**
	 * Determine the appropriate loader.
	 * @return
	 */
	public static SymLoader determineLoader(String extension, URI uri, String featureName, AnnotatedSeqGroup group) {
		// residue loaders
		extension = extension.substring(extension.indexOf('.') + 1);	// strip off first .
		if (extension.equals("bnib")) {
			return new BNIB(uri, group);
		}
		if (extension.equals("fa") || extension.equals("fas") || extension.equals("fasta")) {
			return new Fasta(uri, group);
		}
		if (extension.equals("2bit")) {
			return new TwoBit(uri);
		}

		// symmetry loaders
		if (extension.equals("bam")) {
			return new BAM(uri, featureName, group);
		}
		/*if (extension.equals("bar")) {
			return new Bar(uri, featureName, group);
		}*/
		if (extension.equals("bed")) {
			return new BED(uri, featureName, group);
		}
		if (extension.equals("gb")) {
			return new Genbank(uri, featureName, group);
		}
		if (extension.equals("gr")) {
			return new Gr(uri, featureName, group);
		}
		if (extension.equals("sgr")) {
			return new Sgr(uri, featureName, group);
		}
		// commented out until the USeq class is updated
//		if (extension.equals("useq")) {
//			return new USeq(uri, featureName, group);
//		}
		if (extension.equals("wig")) {
			return new Wiggle(uri, featureName, group);
		}
		if(extension.equals("link.psl")) {
			PSL psl = new PSL(uri, featureName, group, null, null,
				false, false, false);
			psl.setIsLinkPsl(true);
			psl.enableSharedQueryTarget(true);
			return psl;
		}
		if(extension.equals("psl") || extension.equals("psl3") || extension.equals("pslx")) {
			PSL psl = new PSL(uri, featureName, group, null, null,
				false, false, false);
			psl.enableSharedQueryTarget(true);
			return psl;
		}
		if(extension.equals("bgn") || extension.equals("bp1") || extension.equals("bp2") ||
				extension.equals("bps") || extension.equals("brs") ||
				extension.equals("cnt") || extension.equals("cyt")) {
			return new SymLoaderInst(uri, featureName, group);
		}
		if((extension.equals("sin") || extension.equals("egr")) ||
				extension.equals("bgr") || extension.equals("useq")){
			return new SymLoaderInstNC(uri, featureName, group);
		}if((extension.equals("gff3")) || extension.endsWith("gff") ||
				extension.equals("gtf")){
			//Determine if a file with extension gff is actually gff3
			if(GFF3.isGFF3(uri))
				return new GFF3(uri, featureName, group);
			
			return new SymLoaderInstNC(uri, featureName, group);
		}

		return null;
	}

}
