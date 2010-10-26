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
package com.affymetrix.genometryImpl.quickload;

import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser;
import com.affymetrix.genometryImpl.parsers.AnnotsXmlParser.AnnotMapElt;
import com.affymetrix.genometryImpl.parsers.ChromInfoParser;
import com.affymetrix.genometryImpl.parsers.LiftParser;
import com.affymetrix.genometryImpl.util.Constants;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.ServerUtils;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 *
 * @version $Id: QuickLoadServerModel.java 6750 2010-08-25 17:35:57Z sgblanch $
 */
public final class QuickLoadServerModel {

	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	private static final Pattern tab_regex = Pattern.compile("\t");
	private final String root_url;
	private final List<String> genome_names = new ArrayList<String>();
	private final Map<String, String> genome_dir = new HashMap<String, String>();
	/** A set containing initialized genomes */
	private final Set<String> initialized = new HashSet<String>();
	// A map from String genome name to a Map of (typeName,fileName) on the server for that group
	private final Map<String, List<AnnotMapElt>> genome2annotsMap = new HashMap<String, List<AnnotMapElt>>();
	private static final Map<String, QuickLoadServerModel> url2quickload = new HashMap<String, QuickLoadServerModel>();
	private final String primary_url;
	private final GenericServer primaryServer;

	/**
	 * Initialize quickload server model for given url.
	 * @param url	server url.
	 */
	public QuickLoadServerModel(String url) {
		this(url, null, null);
	}

	private QuickLoadServerModel(String url, String pri_url, GenericServer priServer) {
		url = ServerUtils.formatURL(url, LoadUtils.ServerType.QuickLoad);

		if(pri_url != null)
			pri_url = ServerUtils.formatURL(pri_url, LoadUtils.ServerType.QuickLoad);
		
		root_url = url;
		primary_url = pri_url;
		primaryServer = priServer;

		Logger.getLogger(QuickLoadServerModel.class.getName()).log(Level.FINE, "( {0}, {1} )", new Object[] { root_url, primary_url });
		
		loadGenomeNames();
	}

	/**
	 * Initialize quickload server model for given url. Then add gnomes names provided in set.
	 * @param url	server url.
	 * @param primary_url	url of primary server. 
	 * @param genome_names	Set of genomes names to be added.
	 * @return an instance of QuickLoadServerModel
	 */
	public static synchronized QuickLoadServerModel getQLModelForURL(URL url, URL primary_url, GenericServer primaryServer) {

		String ql_http_root = url.toExternalForm();
		
		String primary_root = null;
		if(primary_url != null){
			primary_root = primary_url.toExternalForm();
		}

		QuickLoadServerModel ql_server = url2quickload.get(ql_http_root);
		if (ql_server == null) {
			ql_server = new QuickLoadServerModel(ql_http_root, primary_root, primaryServer);
			url2quickload.put(ql_http_root, ql_server);
			LocalUrlCacher.loadSynonyms(LOOKUP, ql_http_root + "synonyms.txt");
		}
		
		return ql_server;
		
	}
	
	/**
	 * Initialize quickload server model for given url.
	 */
	public static synchronized QuickLoadServerModel getQLModelForURL(URL url) {
		return getQLModelForURL(url, null, null);
	}
	
	private static boolean getCacheAnnots() {
		return true;
	}

	private String getRootUrl() {
		return root_url;
	}

	public List<String> getGenomeNames() {
		return genome_names;
	}

	private AnnotatedSeqGroup getSeqGroup(String genome_name) {
		return GenometryModel.getGenometryModel().addSeqGroup(LOOKUP.findMatchingSynonym(
				GenometryModel.getGenometryModel().getSeqGroupNames(), genome_name));
	}

	public List<AnnotMapElt> getAnnotsMap(String genomeName) {
		return this.genome2annotsMap.get(genomeName);
	}

	/**
	 *  Returns the list of String typeNames that this QuickLoad server has
	 *  for the genome with the given name.
	 *  The list may (rarely) be empty, but never null.
	 */
	public List<String> getTypes(String genome_name) {
		genome_name = LOOKUP.findMatchingSynonym(genome_names, genome_name);
		if (!initialized.contains(genome_name)) {
			initGenome(genome_name);
		}
		if (getAnnotsMap(genome_name) == null) {
			return Collections.<String>emptyList();
		}
		List<String> typeNames = new ArrayList<String>();
		for (AnnotMapElt annotMapElt : getAnnotsMap(genome_name)) {
				typeNames.add(annotMapElt.title);
		}
		
		return typeNames;
	}

	public Map<String, String> getProps(String genomeName, String featureName) {
		Map<String, String> props = null;
		List<AnnotMapElt> annotList = getAnnotsMap(genomeName);
		if (annotList != null) {
			AnnotMapElt annotElt = AnnotMapElt.findTitleElt(featureName, annotList);
			if (annotElt != null) {
				return annotElt.props;
			}
		}
		return props;
	}

	private synchronized void initGenome(String genome_name) {
		Logger.getLogger(QuickLoadServerModel.class.getName()).log(
				Level.FINE, "initializing data for genome: {0}", genome_name);
		if (loadSeqInfo(genome_name) && loadAnnotationNames(genome_name)) {
			initialized.add(genome_name);
			return;
		}

		// Clear the type list if something went wrong.
		List<AnnotMapElt> annotList = getAnnotsMap(genome_name);
		if (annotList != null) {
			annotList.clear();
		}
	}

	/**
	 *  Determines the list of annotation files available in the genome directory.
	 *  Looks for ~genome_dir/annots.txt file which lists annotation files
	 *  available in same directory.  Returns true or false depending on
	 *  whether the file is successfully loaded.
	 *  You can retrieve the typeNames with {@link #getTypes(String)}
	 */
	private boolean loadAnnotationNames(String genome_name) {
		genome_name = LOOKUP.findMatchingSynonym(genome_names, genome_name);
		Logger.getLogger(QuickLoadServerModel.class.getName()).log(
				Level.FINE, "loading list of available annotations for genome: {0}", genome_name);

		// Make a new list of typeNames, in case this is being re-initialized
		// If this search fails, then we're just returning an empty map.
		List<AnnotMapElt> annotList = new ArrayList<AnnotMapElt>();
		genome2annotsMap.put(genome_name, annotList);

		InputStream istr = null;
		String filename = null;
		try{
			filename = getPath(genome_name, Constants.annotsXml);
			istr = getInputStream(filename, false, true);
			boolean annots_found = processAnnotsXml(istr, annotList);

			if(annots_found)
				return true;

			Logger.getLogger(QuickLoadServerModel.class.getName()).log(
					Level.FINE, "Couldn''t found annots.xml for {0}. Looking for annots.txt now.", genome_name);
			filename = getPath(genome_name, Constants.annotsTxt);
			istr = getInputStream(filename, getCacheAnnots(), false);

			return processAnnotsTxt(istr, annotList);

		}catch (Exception ex) {
			Logger.getLogger(QuickLoadServerModel.class.getName()).log(
					Level.FINE, "Couldn''t found either annots.xml or annots.txt for {0}", genome_name);
			System.out.println("Couldn't process file " + filename);
			ex.printStackTrace();
			return false;
		} finally {
			GeneralUtils.safeClose(istr);
		}
		
	}

	/**
	 * Process the annots.xml file (if it exists).
	 * This has friendly type names.
	 * @param filename
	 * @param annotList
	 * @return true or false
	 */
	private static boolean processAnnotsXml(InputStream istr, List<AnnotMapElt> annotList) {
			if (istr == null) {
				// Search failed.  That's fine, since there's a backup test for annots.txt.
				return false;
			}

			AnnotsXmlParser.parseAnnotsXml(istr, annotList);
			return true;
	}

	/**
	 * Process the annots.txt file (if it exists).
	 * @param filename
	 * @param annotList
	 * @return true or false
	 */
	private static boolean processAnnotsTxt(InputStream istr, List<AnnotMapElt> annotList) {
		BufferedReader br = null;
		try {
			if (istr == null) {
				// Search failed.  getInputStream has already logged warnings about this.
				return false;
			}
			br = new BufferedReader(new InputStreamReader(istr));
			String line;
			while ((line = br.readLine()) != null) {
				String[] fields = tab_regex.split(line);
				if (fields.length >= 1) {
					String annot_file_name = fields[0];
					if (annot_file_name == null || annot_file_name.length() == 0) {
						continue;
					}
					String friendlyName = LoadUtils.stripFilenameExtensions(annot_file_name);
					AnnotMapElt annotMapElt = new AnnotMapElt(annot_file_name, friendlyName);
					annotList.add(annotMapElt);
				}
			}
			return true;
		} catch (Exception ex) {
			return false;
		} finally {
			GeneralUtils.safeClose(br);
		}
	}

	private boolean loadSeqInfo(String genome_name) {
		String liftAll = Constants.liftAllLft;
		String modChromInfo = Constants.modChromInfoTxt;
		genome_name = LOOKUP.findMatchingSynonym(genome_names, genome_name);
		boolean success = false;
		InputStream lift_stream = null;
		InputStream cinfo_stream = null;
		try {
			String lift_path = getPath(genome_name, liftAll);
			try {
				// don't warn about this file, since we'll look for modChromInfo file
				lift_stream = getInputStream(lift_path, getCacheAnnots(), true);
			} catch (Exception ex) {
				// exception can be ignored, since we'll look for modChromInfo file.
				Logger.getLogger(QuickLoadServerModel.class.getName()).log(
						Level.FINE, "couldn''t find {0}, looking instead for {1}", new Object[]{liftAll, modChromInfo});
				lift_stream = null;
			}
			if (lift_stream == null) {
				String cinfo_path = getPath(genome_name, modChromInfo);
				try {
					cinfo_stream = getInputStream(cinfo_path, getCacheAnnots(), false);
				} catch (Exception ex) {
					System.err.println("ERROR: could find neither " + lift_path + " nor " + cinfo_path);
					ex.printStackTrace();
					cinfo_stream = null;
				}
			}

			boolean annot_contigs = false;
			if (lift_stream != null) {
				LiftParser.parse(lift_stream, GenometryModel.getGenometryModel(), genome_name, annot_contigs);
				success = true;
			} else if (cinfo_stream != null) {
				ChromInfoParser.parse(cinfo_stream, GenometryModel.getGenometryModel(), genome_name);
				success = true;
			}
		} catch (Exception ex) {
			ErrorHandler.errorPanel("ERROR", "Error loading data for genome '" + genome_name + "'", ex);
		} finally {
			GeneralUtils.safeClose(lift_stream);
			GeneralUtils.safeClose(cinfo_stream);
		}
		return success;
	}

	private synchronized void loadGenomeNames() {
		String contentsTxt = Constants.contentsTxt;
		InputStream istr = null;
		InputStreamReader ireader = null;
		BufferedReader br = null;

		try {
			try {
				istr = getInputStream(contentsTxt, getCacheAnnots(), false);
			} catch (Exception e) {
				System.out.println("ERROR: Couldn't open '" + getLoadURL() + contentsTxt + "\n:  " + e.toString());
				istr = null; // dealt with below
			}

			if (istr == null) {
				System.out.println("Could not load QuickLoad contents from\n" + getLoadURL() + contentsTxt);
				return;
			}

			ireader = new InputStreamReader(istr);
			br = new BufferedReader(ireader);
			String line;
			while ((line = br.readLine()) != null) {
				if ((line.length() == 0) || line.startsWith("#")) {
					continue;
				}
				AnnotatedSeqGroup group = null;
				String[] fields = tab_regex.split(line);

				if (fields.length >= 3) {
					Set<String> synonyms = SynonymLookup.getDefaultLookup().getSynonyms(fields[2]);
					if(synonyms == null || synonyms.isEmpty()){
						Logger.getLogger(QuickLoadServerModel.class.getName()).log(
								Level.WARNING,"Couldn't find synomym for {0}. "
								+ "So skipping it.",new Object[]{fields[2]});
						continue;
					}
				}
				
				String genome_name = "";
				if (fields.length >= 1) {
					genome_name = fields[0];
					genome_name = genome_name.trim();
					if (genome_name.length() == 0) {
						System.out.println("Found blank QuickLoad genome -- skipping");
						continue;
					}
					group = this.getSeqGroup(genome_name);  // returns existing group if found, otherwise creates a new group
					genome_names.add(genome_name);
				}
				// if quickload server has description, and group is new or doesn't yet have description, add description to group
				if ((fields.length >= 2) && (group.getDescription() == null)) {
					group.setDescription(fields[1]);
				}

				if (fields.length >= 3) {
					genome_dir.put(genome_name, fields[2]);
				}else
					genome_dir.put(genome_name, "");
			}

		} catch (Exception ex) {
			ErrorHandler.errorPanel("ERROR", "Error loading genome names", ex);
		} finally {
			GeneralUtils.safeClose(istr);
			GeneralUtils.safeClose(ireader);
			GeneralUtils.safeClose(br);
		}
	}

	public String getOrganismDir(String version){
		return genome_dir.get(version);
	}

	/**
	 * Translate a file in a genome to a relative path under this
	 * QuickLoad directory.  If file is null or an empty string, this
	 * will return the path to the version directory.
	 *
	 * @param genome_name name of this genome version
	 * @param file the file in question.  Can be null
	 * @return path to the file or version directory if file is null
	 */
	public String getPath(String genome_name, String file) {
		StringBuilder builder = new StringBuilder();
		String organism = getOrganismDir(genome_name);

		if (organism != null && !organism.isEmpty()) {
			builder.append(organism);
			builder.append("/");
		}

		builder.append(LOOKUP.findMatchingSynonym(genome_names, genome_name));
		builder.append("/");

		if (file != null && !file.isEmpty()) {
			builder.append(file);
		}

		return builder.toString();
	}

	private InputStream getInputStream(String append_url, boolean write_to_cache, boolean fileMayNotExist) throws IOException{
		String load_url = getLoadURL() + append_url;
		InputStream istr = LocalUrlCacher.getInputStream(load_url, write_to_cache, null, fileMayNotExist);

		/** Check to see if trying to load from primary server but primary server is not responding **/
		if(istr == null && isLoadingFromPrimary() && !fileMayNotExist){

			Logger.getLogger(QuickLoadServerModel.class.getName()).log(
					Level.WARNING, "Primary Server :{0} is not responding. So disabling it for this session.", primaryServer.serverName);
			primaryServer.setServerStatus(ServerStatus.NotResponding);

			load_url = getLoadURL() + append_url;
			istr = LocalUrlCacher.getInputStream(load_url, write_to_cache, null, fileMayNotExist);
		}

		Logger.getLogger(QuickLoadServerModel.class.getName()).log(
				Level.FINE, "Load URL: {0}", load_url);
		return istr;
	}

	private boolean isLoadingFromPrimary(){
		return (primary_url != null && primaryServer != null && !primaryServer.getServerStatus().equals(ServerStatus.NotResponding));
	}

	private String getLoadURL(){
		if (!isLoadingFromPrimary()) {
			return root_url;
		}
			
		return primary_url;
	}
	
	@Override
	public String toString() {
		return "QuickLoadServerModel: url='" + getRootUrl() + "'";
	}
}
