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
package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.event.UrlLoaderThread;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.das2.Das2Capability;
import com.affymetrix.genometryImpl.das2.Das2Region;
import com.affymetrix.genometryImpl.das2.Das2ServerInfo;
import com.affymetrix.genometryImpl.das2.Das2Type;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.bookmarks.Bookmark.SYM;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.util.ScriptFileLoader;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.load.GeneralLoadView;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.swing.SwingWorker;

/**
 *  A way of allowing IGB to be controlled via hyperlinks.
 *  (This used to be an implementation of HttpServlet, but it isn't now.)
 * <pre>
 *  Can specify:
 *      genome version
 *      chromosome
 *      start of region in view
 *      end of region in view
 *  and bring up corect version, chromosome, and region with (at least)
 *      annotations that can be loaded via QuickLoaderView
 *  If the currently loaded genome doesn't match the one requested, might
 *      ask the user before switching.
 * 
 * @version $Id: UnibrowControlServlet.java 7007 2010-10-11 14:26:55Z hiralv $
 *</pre>
 */
public final class UnibrowControlServlet {

	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final Pattern query_splitter = Pattern.compile("[;\\&]");

	/** Convenience method for retrieving a String parameter from a parameter map
	 *  of an HttpServletRequest.
	 *  @param map Should be a Map, such as from {@link javax.servlet.ServletRequest#getParameterMap()},
	 *  where the only keys are String and String[] objects.
	 *  @param key Should be a key where you only want a single String object as result.
	 *  If the value in the map is a String[], only the first item in the array will
	 *  be returned.
	 */
	static String getStringParameter(Map map, String key) {
		Object o = map.get(key);
		if (o instanceof String) {
			return (String) o;
		} else if (o instanceof String[]) {
			return ((String[]) o)[0];
		} else if (o != null) {
			// This is a temporary case, for handling Integer objects holding start and end
			// in the old BookMarkAction.java class.  The new version of that class
			// puts everything into String[] objects, so this case can go away.
			return o.toString();
		}
		return null;
	}

	/** Loads a bookmark.
	 *  @param parameters Must be a Map where the only values are String and String[]
	 *  objects.  For example, this could be the Map returned by
	 *  {@link javax.servlet.ServletRequest#getParameterMap()}.
	 */
	public static void goToBookmark(final Application uni, final Map<String, String[]> parameters) throws NumberFormatException {
		String batchFileStr = getStringParameter(parameters, IGBConstants.SCRIPTFILETAG);
		if (batchFileStr != null && batchFileStr.length() > 0) {
			ScriptFileLoader.doActions(batchFileStr);
			return;
		}

		String seqid = getStringParameter(parameters, Bookmark.SEQID);
		String version = getStringParameter(parameters, Bookmark.VERSION);
		String start_param = getStringParameter(parameters, Bookmark.START);
		String end_param = getStringParameter(parameters, Bookmark.END);
		String select_start_param = getStringParameter(parameters, Bookmark.SELECTSTART);
		String select_end_param = getStringParameter(parameters, Bookmark.SELECTEND);
		boolean loadResidue = Boolean.valueOf(getStringParameter(parameters, Bookmark.LOADRESIDUES));
		// For historical reasons, there are two ways of specifying graphs in a bookmark
		// Eventually, they should be treated more similarly, but for now some
		// differences remain
		// parameter "graph_file" can be handled by goToBookmark()
		//    Does not check whether the file was previously loaded
		//    Loads in GUI-friendly thread
		//    Must be a file name, not a generic URL
		// parameter "graph_source_url_0", "graph_source_url_1", ... is handled elsewhere
		//    Checks to avoid double-loading of files
		//    Loading can freeze the GUI
		//    Can be any URL, not just a file
		boolean has_properties = (parameters.get(SYM.FEATURE_URL+"0") != null);
		boolean loaddata = true;
		boolean loaddas2data = true;

		int values[] = parseValues(start_param, end_param, select_start_param, select_end_param);
		int start = values[0],
			end   = values[1],
			selstart = values[2],
			selend   = values[3];

		String[] server_urls = parameters.get(Bookmark.SERVER_URL);
		String[] query_urls = parameters.get(Bookmark.QUERY_URL);
		GenericServer[] gServers = null;

		if (server_urls == null || query_urls == null
				|| query_urls.length == 0 || server_urls.length != query_urls.length) {
			loaddata = false;
		} else {
			gServers = loadServers(server_urls);
		}

		String[] das2_query_urls = parameters.get(Bookmark.DAS2_QUERY_URL);
	    String[] das2_server_urls = parameters.get(Bookmark.DAS2_SERVER_URL);

		GenericServer[] gServers2 = null;

		if (das2_server_urls == null || das2_query_urls == null
				|| das2_query_urls.length == 0 || das2_server_urls.length != das2_query_urls.length) {
			loaddas2data = false;
		} else {
			gServers2 = loadServers(das2_server_urls);
		}

		final BioSeq seq = goToBookmark(uni, seqid, version, start, end);
		
		if (null == seq) {
			return; /* user cancelled the change of genome, or something like that */
		}
	
		if (loaddata) {
			GenericFeature[] gFeatures = loadData(gServers, query_urls, start, end);

			if (has_properties) {
				List<String> graph_urls = getGraphUrls(parameters);

				for (int i = 0; i < gFeatures.length; i++) {
					final GenericFeature feature = gFeatures[i];
					
					if (graph_urls.contains(feature.getURI().toString())) {
						ThreadUtils.getPrimaryExecutor(feature).execute(new Runnable() {

							public void run() {
								BookmarkController.applyProperties(seq, parameters, feature);
							}
						});
					}
				}
			}
		}

		if(loaddas2data){
			loadOldBookmarks(uni, gServers2, das2_query_urls, start, end);
		}

		//loadDataFromDas2(uni, das2_server_urls, das2_query_urls);
		//String[] data_urls = parameters.get(Bookmark.DATA_URL);
		//String[] url_file_extensions = parameters.get(Bookmark.DATA_URL_FILE_EXTENSIONS);
		//loadDataFromURLs(uni, data_urls, url_file_extensions, null);
		String selectParam = getStringParameter(parameters, "select");
		if (selectParam != null) {
			performSelection(selectParam);
		}

		if(loadResidue){
			loadResidues(start, end);
		}

	}

	 public static List<String> getGraphUrls(Map map){
		List<String> graph_paths = new ArrayList<String>();
		for (int i = 0; map.get(SYM.FEATURE_URL.toString() + i) != null; i++) {
			graph_paths.add(getStringParameter(map, SYM.FEATURE_URL.toString() + i));
		}
		return graph_paths;
	 }

	private static void loadResidues(int start, int end){
		AnnotatedSeqGroup seqGroup = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		BioSeq vseq = GenometryModel.getGenometryModel().getSelectedSeq();
		String genomeVersionName = seqGroup.getID();

		SeqSpan span = new SimpleMutableSeqSpan(start, end, vseq);
		final SwingWorker<Void, Void> worker = GeneralLoadView.getResidueWorker(genomeVersionName, vseq, span, true, true);

		ExecutorService vexec = Executors.newSingleThreadExecutor();
		vexec.execute(worker);
		vexec.shutdown();
	}

	/**
	 *  find Das2ServerInfo (or create if not already existing), based on das2_server_url
	 *       to add later?  If no
	 *  find Das2VersionedSource based on Das2ServerInfo and das2_query_url (search for version's FEATURE capability URL matching path of das2_query_url)
	 *  create Das2FeatureRequestSym
	 *  call processFeatureRequests(request_syms, update_display, thread_requests)
	 *       (which in turn call Das2ClientOptimizer.loadFeatures(request_sym))
	 */
	@Deprecated	
	private static void loadDataFromDas2(final Application uni, final String[] das2_server_urls, final String[] das2_query_urls) {
		if (das2_server_urls == null || das2_query_urls == null || das2_query_urls.length == 0 || das2_server_urls.length != das2_query_urls.length) {
			return;
		}
		List<String> opaque_requests = new ArrayList<String>();
		createDAS2andOpaqueRequests(das2_server_urls, das2_query_urls, opaque_requests);
		if (!opaque_requests.isEmpty()) {
			String[] data_urls = new String[opaque_requests.size()];
			for (int r = 0; r < opaque_requests.size(); r++) {
				data_urls[r] = opaque_requests.get(r);
			}
			loadDataFromURLs(uni, data_urls, null, null);
		}
	}

	@Deprecated
	private static void createDAS2andOpaqueRequests(
			final String[] das2_server_urls, final String[] das2_query_urls, List<String> opaque_requests) {
		for (int i = 0; i < das2_server_urls.length; i++) {
			String das2_server_url = GeneralUtils.URLDecode(das2_server_urls[i]);
			String das2_query_url = GeneralUtils.URLDecode(das2_query_urls[i]);
			String cap_url = null;
			String seg_uri = null;
			String type_uri = null;
			String overstr = null;
			String format = null;
			boolean use_optimizer = true;
			int qindex = das2_query_url.indexOf('?');
			if (qindex > -1) {
				cap_url = das2_query_url.substring(0, qindex);
				String query = das2_query_url.substring(qindex + 1);
				String[] query_array = query_splitter.split(query);
				for (int k = -0; k < query_array.length; k++) {
					String tagval = query_array[k];
					int eqindex = tagval.indexOf('=');
					String tag = tagval.substring(0, eqindex);
					String val = tagval.substring(eqindex + 1);
					if (tag.equals("format") && (format == null)) {
						format = val;
					} else if (tag.equals("type") && (type_uri == null)) {
						type_uri = val;
					} else if (tag.equals("segment") && (seg_uri == null)) {
						seg_uri = val;
					} else if (tag.equals("overlaps") && (overstr == null)) {
						overstr = val;
					} else {
						use_optimizer = false;
						break;
					}
				}
				if (type_uri == null || seg_uri == null || overstr == null) {
					use_optimizer = false;
				}
			} else {
				use_optimizer = false;
			}
			//
			// only using optimizer if query has 1 segment, 1 overlaps, 1 type, 0 or 1 format, no other params
			// otherwise treat like any other opaque data url via loadDataFromURLs call
			//
			if (!use_optimizer) {
				opaque_requests.add(das2_query_url);
				continue;
			}

			try {
				GenericServer gServer = ServerList.getServer(das2_server_url);
				if (gServer == null) {
					gServer = ServerList.addServer(ServerType.DAS2, das2_server_url, das2_server_url, true);
				} else if (!gServer.isEnabled()) {
					gServer.setEnabled(true);
					GeneralLoadUtils.discoverServer(gServer);
					// enable the server.
					// TODO - this will be saved in preferences as enabled, although it shouldn't.
				}
				Das2ServerInfo server = (Das2ServerInfo) gServer.serverObj;
				server.getSources(); // forcing initialization of server sources, versioned sources, version sources capabilities
				Das2VersionedSource version = Das2Capability.getCapabilityMap().get(cap_url);
				if (version == null) {
					Logger.getLogger(UnibrowControlServlet.class.getName()).log(Level.SEVERE, "Couldn''t find version in url: {0}", cap_url);
					continue;
				}
				Das2Type dtype = version.getTypes().get(type_uri);
				if (dtype == null) {
					Logger.getLogger(UnibrowControlServlet.class.getName()).log(Level.SEVERE, "Couldn''t find type: {0} in server: {1}", new Object[]{type_uri, das2_server_url});
					continue;
				}
				Das2Region segment = version.getSegments().get(seg_uri);
				if (segment == null) {
					Logger.getLogger(UnibrowControlServlet.class.getName()).log(Level.SEVERE, "Couldn''t find segment: {0} in server: {1}", new Object[]{seg_uri, das2_server_url});
					continue;
				}
				String[] minmax = overstr.split(":");
				int min = Integer.parseInt(minmax[0]);
				int max = Integer.parseInt(minmax[1]);
				SeqSpan overlap = new SimpleSeqSpan(min, max, segment.getAnnotatedSeq());

				GenericFeature feature = dtype.getFeature();
				if (feature != null) {
					feature.setVisible();
					GeneralLoadView.getLoadView().createFeaturesTable();
					GeneralLoadUtils.loadAndDisplaySpan(overlap, feature);
				} else {
					Logger.getLogger(GeneralUtils.class.getName()).log(
							Level.SEVERE, "Couldn't find feature for bookmark URL: {0}", dtype.getURI());
				}
			} catch (Exception ex) {
				// something went wrong with deconstructing DAS/2 query URL, so just add URL to list of opaque requests
				ex.printStackTrace();
				use_optimizer = false;
				opaque_requests.add(das2_query_url);
			}
		}
	}

	private static void loadOldBookmarks(final Application uni, GenericServer[] gServers, String[] das2_query_urls, int start, int end){
		List<String> opaque_requests = new ArrayList<String>();
		for (int i = 0; i < das2_query_urls.length; i++) {
			String das2_query_url = GeneralUtils.URLDecode(das2_query_urls[i]);
			String seg_uri = null;
			String type_uri = null;
			String overstr = null;
			String format = null;
			boolean use_optimizer = true;
			int qindex = das2_query_url.indexOf('?');
			if (qindex > -1) {
				String query = das2_query_url.substring(qindex + 1);
				String[] query_array = query_splitter.split(query);
				for (int k = -0; k < query_array.length; k++) {
					String tagval = query_array[k];
					int eqindex = tagval.indexOf('=');
					String tag = tagval.substring(0, eqindex);
					String val = tagval.substring(eqindex + 1);
					if (tag.equals("format") && (format == null)) {
						format = val;
					} else if (tag.equals("type") && (type_uri == null)) {
						type_uri = val;
					} else if (tag.equals("segment") && (seg_uri == null)) {
						seg_uri = val;
					} else if (tag.equals("overlaps") && (overstr == null)) {
						overstr = val;
					} else {
						use_optimizer = false;
						break;
					}
				}
				if (type_uri == null || seg_uri == null || overstr == null) {
					use_optimizer = false;
				}
			} else {
				use_optimizer = false;
			}
			//
			// only using optimizer if query has 1 segment, 1 overlaps, 1 type, 0 or 1 format, no other params
			// otherwise treat like any other opaque data url via loadDataFromURLs call
			//
			if (!use_optimizer) {
				opaque_requests.add(das2_query_url);
				continue;
			}

			loadData(gServers[i], type_uri, start, end);
		}

		if (!opaque_requests.isEmpty()) {
			String[] data_urls = new String[opaque_requests.size()];
			for (int r = 0; r < opaque_requests.size(); r++) {
				data_urls[r] = opaque_requests.get(r);
			}
			loadDataFromURLs(uni, data_urls, null, null);
		}
	}
	
	private static GenericFeature[] loadData(final GenericServer[] gServers, final String[] query_urls, int start, int end){
		GenericFeature[] gFeatures = new GenericFeature[query_urls.length];
		for (int i = 0; i < query_urls.length; i++) {
			gFeatures[i] = loadData(gServers[i], query_urls[i], start, end);
		}
		
		GeneralLoadView.getLoadView().refreshTreeView();
		GeneralLoadView.getLoadView().createFeaturesTable();

		return gFeatures;
	}

	private static GenericFeature loadData(final GenericServer gServer, final String query_url, int start, int end){
		BioSeq seq = GenometryModel.getGenometryModel().getSelectedSeq();

		if (gServer == null) {
			return null;
		}

		GenericFeature feature = getFeature(gServer, query_url);

		if (feature != null) {
			feature.setVisible();
			SeqSpan overlap = new SimpleSeqSpan(start, end, seq);
			if(!GenericFeature.setPreferredLoadStrategy(feature, LoadStrategy.VISIBLE)){
				overlap = new SimpleSeqSpan(seq.getMin(), seq.getMax(), seq);
			}
			GeneralLoadUtils.loadAndDisplaySpan(overlap, feature);
		} else {
			Logger.getLogger(GeneralUtils.class.getName()).log(
					Level.SEVERE, "Couldn't find feature for bookmark url {0}", query_url);
		}

		return feature;
	}
	
	private static GenericServer[] loadServers(String[] server_urls){
		GenericServer[] gServers = new GenericServer[server_urls.length];

		for (int i = 0; i < server_urls.length; i++) {
			String server_url = server_urls[i];
			gServers[i] = loadServer(server_url);
		}
		
		return gServers;
	}

	public static GenericFeature getFeature(GenericServer gServer, String feature_url){
		AnnotatedSeqGroup seqGroup = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		GenericFeature feature = null;

		URI uri = URI.create(feature_url);
		GenericVersion gVersion = seqGroup.getVersionOfServer(gServer);
		if (gVersion == null && gServer.serverType != ServerType.LocalFiles) {
			Logger.getLogger(UnibrowControlServlet.class.getName()).log(
				Level.SEVERE, "Couldn''t find version {0} in server {1}",
				new Object[]{seqGroup.getID(), gServer.serverName});
			return null;
		}

		if(gVersion != null)
			feature = GeneralUtils.findFeatureWithURI(gVersion.getFeatures(), uri);

		if(feature == null && gServer.serverType == ServerType.LocalFiles){
			// For local file check if feature already exists.

			// If feature doesn't not exist then add it.
			String fileName = feature_url.substring(feature_url.lastIndexOf('/') + 1, feature_url.length());
			feature = LoadFileAction.getFeature(uri, fileName, seqGroup.getOrganism(), seqGroup);

		}

		return feature;
	}
	/**
	 * Finds server from server url and enables it, if found disabled.
	 * @param server_url	Server url string.
	 * @return	Returns GenericServer if found else null.
	 */
	public static GenericServer loadServer(String server_url){
		GenericServer gServer = ServerList.getServer(server_url);
		if (gServer == null) {
			Logger.getLogger(UnibrowControlServlet.class.getName()).log(
					Level.SEVERE, "Couldn''t find server {0}. Creating a local server.", server_url);

			gServer = ServerList.getLocalFilesServer();
			
		} else if (!gServer.isEnabled()) {
			// enable the server for this session only
			gServer.enableForSession();
			GeneralLoadUtils.discoverServer(gServer);
		}
		return gServer;
	}

	private static void loadDataFromURLs(final Application uni, final String[] data_urls, final String[] extensions, final String[] tier_names) {
		try {
			if (data_urls != null && data_urls.length != 0) {
				URL[] urls = new URL[data_urls.length];
				for (int i = 0; i < data_urls.length; i++) {
					urls[i] = new URL(data_urls[i]);
				}
				final UrlLoaderThread t = new UrlLoaderThread(uni.getMapView(), urls, extensions, tier_names);
				t.runEventually();
				t.join();
			}
		} catch (MalformedURLException e) {
			ErrorHandler.errorPanel("Error loading bookmark\nData URL malformed\n", e);
		} catch (InterruptedException ex) {
		}
	}

	private static int[] parseValues(String start_param, String end_param,
					String select_start_param, String select_end_param)
					throws NumberFormatException {

		int start = 0;
		int end = Integer.MAX_VALUE;
		if (start_param == null || start_param.equals("")) {
			System.err.println("No start value found in the bookmark URL");
		} else {
			start = Integer.parseInt(start_param);
		}
		if (end_param == null || end_param.equals("")) {
			System.err.println("No end value found in the bookmark URL");
		} else {
			end = Integer.parseInt(end_param);
		}
		int selstart = -1;
		int selend = -1;
		if (select_start_param != null && select_end_param != null && select_start_param.length() > 0 && select_end_param.length() > 0) {
			selstart = Integer.parseInt(select_start_param);
			selend = Integer.parseInt(select_end_param);
		}
		return new int[]{start, end, selstart, selend};
	}

	/** Loads the sequence and goes to the specified location.
	 *  If version doesn't match the currently-loaded version,
	 *  asks the user if it is ok to proceed.
	 *  NOTE:  This schedules events on the AWT event queue.  If you want
	 *  to make sure that everything has finished before you do something
	 *  else, then you have to schedule that something else to occur
	 *  on the AWT event queue.
	 *  @param graph_files it is ok for this parameter to be null.
	 *  @return true indicates that the action succeeded
	 */
	private static BioSeq goToBookmark(final Application uni, final String seqid, final String version, int start, int end) {
		final AnnotatedSeqGroup book_group = determineAndSetGroup(version);
		if (book_group == null) {
			ErrorHandler.errorPanel("Bookmark genome version seq group '" + version + "' not found.\n" +
							"You may need to choose a different server.");
			return null; // cancel
		}

		final BioSeq book_seq = determineSeq(seqid, book_group);
		if (book_seq == null) {
			ErrorHandler.errorPanel("No seqid", "The bookmark did not specify a valid seqid: specified '" + seqid + "'");
			return null;
		} else {
			// gmodel.setSelectedSeq() should trigger a gviewer.setAnnotatedSeq() since
			//     gviewer is registered as a SeqSelectionListener on gmodel
			if (book_seq != gmodel.getSelectedSeq()) {
				gmodel.setSelectedSeq(book_seq);
			}
		}
		setRegion(uni.getMapView(), start, end, book_seq);
		
		return book_seq;
	}

	public static AnnotatedSeqGroup determineAndSetGroup(final String version) {
		final AnnotatedSeqGroup group;
		if (version == null || "unknown".equals(version) || version.trim().equals("")) {
			group = gmodel.getSelectedSeqGroup();
		} else {
			group = gmodel.getSeqGroup(version);
		}
		if (group != null && !group.equals(gmodel.getSelectedSeqGroup())) {
			GeneralLoadView.initVersion(version);
			gmodel.setSelectedSeqGroup(group);
		}
		return group;
	}


	public static BioSeq determineSeq(String seqid, AnnotatedSeqGroup group) {
		// hopefully setting gmodel's selected seq group above triggered population of seqs
		//   for group if not already populated
		BioSeq book_seq;
		if (seqid == null || "unknown".equals(seqid) || seqid.trim().length() == 0) {
			book_seq = gmodel.getSelectedSeq();
			if (book_seq == null && gmodel.getSelectedSeqGroup().getSeqCount() > 0) {
				book_seq = gmodel.getSelectedSeqGroup().getSeq(0);
			}
		} else {
			book_seq = group.getSeq(seqid);
		}
		return book_seq;
	}

	public static void setRegion(SeqMapView gviewer, int start, int end, BioSeq book_seq) {
		if (start >= 0 && end > 0 && end != Integer.MAX_VALUE) {
			final SeqSpan view_span = new SimpleSeqSpan(start, end, book_seq);
			gviewer.zoomTo(view_span);
			final double middle = (start + end) / 2.0;
			gviewer.setZoomSpotX(middle);
		}
	}

	/**
	 * This handles the "select" API parameter.  The "select" parameter can be followed by one
	 * or more comma separated IDs in the form: &select=<id_1>,<id_2>,...,<id_n>
	 * Example:  "&select=EPN1,U2AF2,ZNF524"
	 * Each ID that exists in IGB's ID to symmetry hash will be selected, even if the symmetries
	 * lie on different sequences.
	 * @param selectParam The select parameter passed in through the API
	 */
	public static void performSelection(String selectParam) {

		if (selectParam == null || selectParam.length() == 0) {
			return;
		}

		// split the parameter by commas
		String[] ids = selectParam.split(",");

		if (ids.length == 0) {
			return;
		}

		AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		List<SeqSymmetry> sym_list = new ArrayList<SeqSymmetry>(ids.length);
		for (String id : ids) {
			sym_list.addAll(group.findSyms(id));
		}

		GenometryModel.getGenometryModel().setSelectedSymmetriesAndSeq(sym_list, UnibrowControlServlet.class);
	}
}
