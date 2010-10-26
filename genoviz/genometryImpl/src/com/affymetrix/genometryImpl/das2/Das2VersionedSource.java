/**
 *   Copyright (c) 2001-2005 Affymetrix, Inc.
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
package com.affymetrix.genometryImpl.das2;

import java.io.*;
import java.net.*;
import java.util.*;
import org.w3c.dom.*;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.XMLUtils;
import com.affymetrix.genometryImpl.parsers.Das2FeatureSaxParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.parsers.BedParser;
import java.util.logging.Level;
import java.util.logging.Logger;
import static com.affymetrix.genometryImpl.util.Constants.UTF8;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.ServerUtils;

public final class Das2VersionedSource {
    private static final boolean URL_ENCODE_QUERY = true;
    public static final String SEGMENTS_CAP_QUERY = "segments";
    public static final String TYPES_CAP_QUERY = "types";
    public static final String FEATURES_CAP_QUERY = "features";
	private static final String XML = ".xml";
    static String ID = Das2FeatureSaxParser.ID;
    static String URID = Das2FeatureSaxParser.URID;
    static String SEGMENT = Das2FeatureSaxParser.SEGMENT;
    static String NAME = Das2FeatureSaxParser.NAME;
    static String TITLE = Das2FeatureSaxParser.TITLE;
    static GenometryModel gmodel = GenometryModel.getGenometryModel();
    private final URI version_uri;
    private final URI coords_uri;
	private final URI primary_uri; // Cached primary server.
	private final GenericServer primaryServer;
    private final Das2Source source;
    private final String name;
    private final Map<String,Das2Capability> capabilities = new HashMap<String,Das2Capability>();
    private final Map<String,Das2Region> regions = new LinkedHashMap<String,Das2Region>();
    private AnnotatedSeqGroup genome = null;
    private final Map<String,Das2Type> types = new LinkedHashMap<String,Das2Type>();
	private final Map<String,List<Das2Type>> residue2types = new LinkedHashMap<String,List<Das2Type>>();
    private boolean regions_initialized = false;
    private boolean types_initialized = false;
    private String types_filter = null;


    public Das2VersionedSource(Das2Source das_source, URI vers_uri, URI coords_uri, String name,
            String href, String description, boolean init, URI pri_uri, GenericServer primaryServer) {
        this.name = name;
        this.coords_uri = coords_uri;
        version_uri = vers_uri;
		this.primaryServer = primaryServer;

		primary_uri = (pri_uri == null) ? null : URI.create(pri_uri.toString() + name + "/");
		
        source = das_source;
        if (init) {
            initSegments();
            initTypes(null);
        }
    }

    public String getID() {
        return version_uri.toString();
    }

    public String getName() {
        return name;
    }

		@Override
    public String toString() {
        return getName();
    }

    public Das2Source getSource() {
        return source;
    }

    void addCapability(Das2Capability cap) {
        capabilities.put(cap.getType(), cap);
		Das2Capability.getCapabilityMap().put(cap.getRootURI().toString(), this);
    }

    public Das2Capability getCapability(String type) {
        return capabilities.get(type);
    }

	public AnnotatedSeqGroup getGenome() {
		if (genome != null) {
			return genome;
		}
		// trying to use name for group id first, if no name then use full URI
		// This won't work in every situation!  Really need to resolve issues between VersionedSource URI ids and group ids
		String groupid = this.getName();
		if (groupid == null) {
			groupid = this.getID();
		}
		genome = gmodel.getSeqGroup(groupid);  // gets existing seq group if possible
		if (genome == null && coords_uri != null) { // try coordinates
			genome = gmodel.getSeqGroup(coords_uri.toString());
		}
		if (genome == null) {
			// add new seq group -- if has global coordinates uri, then use that
			//   otherwise, use groupid (version source name or URI)
			if (coords_uri == null) {
				genome = new Das2SeqGroup(this, groupid);
			} else {
				// for now only use coords URI for group if version has no name (just ID), otherwise use name
				if (this.getName() == null) {
					genome = new Das2SeqGroup(this, coords_uri.toString());
				} else {
					genome = new Das2SeqGroup(this, groupid);
				}
			}
			gmodel.addSeqGroup(genome);
		}
		return genome;
	}

    public synchronized Map<String,Das2Region> getSegments() {
        if (!regions_initialized) {
            initSegments();
        }
        return regions;
    }

    /**
     *  assumes there is only one region for each seq
     *    may want to change this to return a list of regions instead
     **/
    public Das2Region getSegment(BioSeq seq) {
		for (Das2Region region : getSegments().values()) {
            BioSeq region_seq = region.getAnnotatedSeq();
            if (region_seq == seq) {
                return region;
            }
        }
        return null;
    }

    private synchronized void addType(Das2Type type) {
		boolean isResidueFormat = false;
		for(String format : type.getFormats().keySet()){
			if(ServerUtils.isResidueFile(format)){
				isResidueFormat = true;
				break;
			}
		}

		if(isResidueFormat){
			String tname = type.getName();
			List<Das2Type> prevlist = residue2types.get(tname);
			if (prevlist == null) {
				prevlist = new ArrayList<Das2Type>();
				residue2types.put(tname, prevlist);
			}
			prevlist.add(type);
		}
		else{
			types.put(type.getURI().toString(), type);
		}
    }

    public synchronized Map<String,Das2Type> getTypes() {
        if (!types_initialized || types_filter != null) {
            initTypes(null);
        }
        return types;
    }

	public synchronized Set<String> getResidueFormat(String name) {
		List<Das2Type> Localtypes = residue2types.get(name);
		if(Localtypes == null || Localtypes.isEmpty()){
			return Collections.<String>emptySet();
		}

		Set<String> formats = new HashSet<String>();
		for(Das2Type type : Localtypes){
			for(String format : type.getFormats().keySet())
				formats.add(format.toLowerCase());
		}

		return formats;
	}

    /**
	 * Get regions from server.
	 */
	private synchronized void initSegments() {
		Das2Capability segcap = getCapability(SEGMENTS_CAP_QUERY);
		String region_request = segcap.getRootURI().toString();
		try {
			Logger.getLogger(Das2ServerInfo.class.getName()).log(
				Level.FINE, "Das2 Segments Request: {0}", region_request);
			// don't cache this!  If the file is corrupted, this can hose the IGB instance until the cache and preferences are cleared.
			InputStream response = getInputStream(SEGMENTS_CAP_QUERY, LocalUrlCacher.getPreferredCacheUsage(), false, null, "Das2 Segments Request");

			Document doc = XMLUtils.getDocument(response);
			NodeList regionlist = doc.getElementsByTagName("SEGMENT");
			getRegionList(regionlist, region_request);
		} catch (Exception ex) {
			ex.printStackTrace();
			LocalUrlCacher.invalidateCacheFile(region_request);
			// TODO
			//ErrorHandler.errorPanel("Error initializing DAS2 region points for\n" + region_request, ex);
		}
		//TODO should regions_initialized be true if an exception occurred?
		regions_initialized = true;
	}


	private void getRegionList(NodeList regionlist, String region_request) throws NumberFormatException {
		Logger.getLogger(Das2ServerInfo.class.getName()).log(
					Level.FINE, "segments: {0}", regionlist.getLength());
		int regionLength = regionlist.getLength();
		for (int i = 0; i < regionLength; i++) {
			Element reg = (Element) regionlist.item(i);
			String region_id = reg.getAttribute(URID);
			if (region_id.length() == 0) {
				region_id = reg.getAttribute(ID);
			}
			// GAH 10-24-2007  temporary hack to weed out bad seqs that are somehow
			//   getting added to segments response from Affy DAS/2 server
			if ((region_id.indexOf("|") >= 0) || (region_id.charAt(region_id.length() - 1) == '.')) {
				Logger.getLogger(Das2ServerInfo.class.getName()).log(
					Level.WARNING, "@@@@@@@@@@@@@ caught bad seq id: {0}", region_id);
				continue;
			}
			URI region_uri = Das2ServerInfo.getBaseURI(region_request, reg).resolve(region_id);
			// GAH _TEMPORARY_ hack to strip down region_id
			// Need to move to full URI resolution very soon!
			if (Das2FeatureSaxParser.DO_SEQID_HACK) {
				region_id = Das2FeatureSaxParser.doSeqIdHack(region_id);
			}
			String lengthstr = reg.getAttribute("length");
			String region_name = reg.getAttribute(NAME);
			if (region_name.length() == 0) {
				region_name = reg.getAttribute(TITLE);
			}
			String region_info_url = reg.getAttribute("doc_href");
			int length = Integer.parseInt(lengthstr);
			Das2Region region = new Das2Region(this, region_uri, region_name, region_info_url, length);
			regions.put(region.getID(), region);
		}
	}


    /**
	 * get annotation types from das2 server
     *  loading of parents disabled, getParents currently does nothing
     */
    private synchronized void initTypes(String filter) {
        this.types_filter = filter;
		this.types.clear();

        // how should xml:base be handled?
        //example of type request:  http://das.biopackages.net/das/assay/mouse/6/type?ontology=MA
		Das2Capability segcap = getCapability(TYPES_CAP_QUERY);
		String types_request = segcap.getRootURI().toString();
		InputStream response = null;

		try {

			Map<String, String> headers = new LinkedHashMap<String, String>();

			//set in header a sessionId for types authentication?
			//Also, if there is a sessionId then should ignore cache so user can get hidden types
			String sessionId = source.getServerInfo().getSessionId();
			if (sessionId != null) {
				headers.put("sessionId", sessionId);
				//if sessionID then connected so ignore cache
				response = getInputStream(TYPES_CAP_QUERY, LocalUrlCacher.IGNORE_CACHE, false, headers, "Das2 Types Request");
			} else {
				response = getInputStream(TYPES_CAP_QUERY, LocalUrlCacher.getPreferredCacheUsage(), true, headers, "Das2 Types Request");
			}
			if (response == null) {
				Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Types request {0} was not reachable.", types_request);
				return;
			}
			Document doc = XMLUtils.getDocument(response);
			NodeList typelist = doc.getElementsByTagName("TYPE");
			getTypeList(typelist, types_request);

		} catch (Exception ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
			LocalUrlCacher.invalidateCacheFile(types_request);
		} finally {
			GeneralUtils.safeClose(response);
		}
        //TODO should types_initialized be true after an exception?
        types_initialized = true;
    }


	private void getTypeList(NodeList typelist, String types_request) {
		for (int i = 0; i < typelist.getLength(); i++) {
			Element typenode = (Element) typelist.item(i);
			String typeid = typenode.getAttribute(URID); // Gets the ID value
			if (typeid.length() == 0) {
				typeid = typenode.getAttribute(ID);
			}
			// GAH Temporary hack to deal with typeids that are not legal URIs
			//    unfortunately this can mess up XML Base resolution when the id is an absolute URI
			//    (because URI-encoding will replace any colons, but those are used by URI resolution...)
			//    real fix needs to be on server(s), not client!!
			//	typeid = URLEncoder.encode(typeid, "UTF-8");
			//	typeid = "./" + typeid;
			//        String typeid = typenode.getAttribute("ontology");                            // Gets the ID value
			//FIXME: quick hack to get the type IDs to be kind of right (for now)
			// temporary workaround for getting type ending, rather than full URI
			//	if (typeid.startsWith("./")) { typeid = typeid.substring(2); }
			// if these characters are one the beginning, take off the 1st 2 characters...
			//FIXME: quick hack to get the type IDs to be kind of right (for now)

			String type_name = typenode.getAttribute(NAME);
			if (type_name.length() == 0) {
				type_name = typenode.getAttribute(TITLE);
			}
			NodeList flist = typenode.getElementsByTagName("FORMAT");
			LinkedHashMap<String,String> formats = new LinkedHashMap<String,String>();
			HashMap<String,String> props = new HashMap<String,String>();
			for (int k = 0; k < flist.getLength(); k++) {
				Element fnode = (Element) flist.item(k);
				String formatid = fnode.getAttribute(NAME);
				if (formatid == null) {
					formatid = fnode.getAttribute(ID);
				}
				String mimetype = fnode.getAttribute("mimetype");
				if (mimetype == null || mimetype.equals("")) {
					mimetype = "unknown";
				}
				formats.put(formatid, mimetype);
			}
			NodeList plist = typenode.getElementsByTagName("PROP");
			for (int k = 0; k < plist.getLength(); k++) {
				Element pnode = (Element) plist.item(k);
				String key = pnode.getAttribute("key");
				String val = pnode.getAttribute("value");
				props.put(key, val);
			}
			// If one of the typeid's is not a valid URI, then skip it, but allow
			// other typeid's to get through.
			URI type_uri = null;
			try {
				type_uri = Das2ServerInfo.getBaseURI(types_request, typenode).resolve(typeid);
			} catch (Exception e) {
				Logger.getLogger(Das2ServerInfo.class.getName()).log(
					Level.WARNING, "Error in typeid, skipping: {0}\nUsually caused by an improper character in the URI.", typeid);
			}
			if (type_uri != null) {
				Das2Type type = new Das2Type(this, type_uri, type_name, formats, props);
				// parents field is null for now -- remove at some point?
				this.addType(type);
			}
		}
	}


    /**
     *  Use the name feature filter in DAS/2 to retrieve features by name or id.
     */
    public synchronized List<SeqSymmetry> getFeaturesByName(String name, AnnotatedSeqGroup group, BioSeq chrFilter) {
		InputStream istr = null;
		DataInputStream dis = null;
		try {
			String feature_query = determineFeatureQuery(getCapability(FEATURES_CAP_QUERY),name, chrFilter);
			URL query_url = new URL(feature_query);
			URLConnection query_con = query_url.openConnection();
			query_con.setConnectTimeout(LocalUrlCacher.CONNECT_TIMEOUT);
			query_con.setReadTimeout(LocalUrlCacher.READ_TIMEOUT);
			istr = query_con.getInputStream();
			dis = new DataInputStream(istr);

			// temporary group needed to avoid side effects (remote SeqSymmetries added to the genome)
			AnnotatedSeqGroup tempGroup = AnnotatedSeqGroup.tempGenome(group);
			BedParser parser = new BedParser();
			List<SeqSymmetry> feats = parser.parse(dis, feature_query, tempGroup);
			Logger.getLogger(Das2VersionedSource.class.getName()).log(
					Level.FINE, "parsed query results, annot count = {0}", feats.size());
			return feats;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(istr);
		}
		return null;
	}

	private static String determineFeatureQuery(Das2Capability featcap, String name, BioSeq chrFilter) throws UnsupportedEncodingException {
		String request_root = featcap.getRootURI().toString();
		String nameglob = name;
		if (URL_ENCODE_QUERY) {
			nameglob = URLEncoder.encode(nameglob, UTF8);
		}
		String chrFilterStr = chrFilter == null ? "?" : "?segment=" + URLEncoder.encode(chrFilter.getID(), UTF8) + ";";
		String feature_query = request_root + chrFilterStr + "name=" + nameglob + ";format=bed";
		Logger.getLogger(Das2ServerInfo.class.getName()).log(Level.FINE, "feature query: {0}", feature_query);
		return feature_query;
	}

	private InputStream getInputStream(String query_type, int cache_opt, boolean write_to_cache, Map<String, String> headers, String log_string) throws MalformedURLException, IOException {
		String load_url = getRegionString(query_type);
		InputStream istr = LocalUrlCacher.getInputStream(load_url, cache_opt, write_to_cache, headers);

		/** Check to see if trying to load from primary server but primary server is not responding **/
		if(istr == null && isLoadingFromPrimary()){
			LocalUrlCacher.invalidateCacheFile(load_url);
			Logger.getLogger(Das2ServerInfo.class.getName()).log(
					Level.WARNING, "Primary Server :{0} is not responding. So disabling it for this session.", primaryServer.serverName);
			primaryServer.setServerStatus(ServerStatus.NotResponding);

			load_url = getRegionString(query_type);
			istr = LocalUrlCacher.getInputStream(load_url, cache_opt, write_to_cache, headers);
		}

		Logger.getLogger(Das2ServerInfo.class.getName()).log(
				Level.INFO, "{0} : {1}", new Object[]{log_string, load_url});

		return istr;
	}

	private boolean isLoadingFromPrimary(){
		return (primary_uri != null && primaryServer != null && !primaryServer.getServerStatus().equals(ServerStatus.NotResponding));
	}

	/**
	 * If primary uri is null then load data from actual server.
	 * @param type	Required das2capability type.
	 * @return	Returns region string.
	 */
	private String getRegionString(String type){
		if(!isLoadingFromPrimary()){
			Das2Capability segcap = getCapability(type);
			return segcap.getRootURI().toString();
		}

		return primary_uri.toString() + type + XML;
	}
}
