package com.affymetrix.genometryImpl.util;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import net.sf.samtools.util.SeekableStream;
import net.sf.samtools.util.SeekableFileStream;

public final class LocalUrlCacher {

	private static final String cache_content_root = PreferenceUtils.getAppDataDirectory() + "cache/";
	private static final String cache_header_root = cache_content_root + "headers/";
	private static final String HTTP_STATUS_HEADER = "HTTP_STATUS";
	private static boolean DEBUG_CONNECTION = false;
	private static boolean CACHE_FILE_URLS = false;
	public static final int IGNORE_CACHE = 100;
	public static final int ONLY_CACHE = 101;
	public static final int NORMAL_CACHE = 102;

	public static enum CacheUsage {
		Normal(NORMAL_CACHE),
		Disabled(IGNORE_CACHE),
		Offline(ONLY_CACHE);

		public final int usage;
		
		CacheUsage(int usage) {
			this.usage = usage;
		}
	};

	public static enum CacheOption {
		IGNORE,
		ONLY,
		NORMAL;
	}

	// the "quickload" part of the constant value is there for historical reasons
	public static final String PREF_CACHE_USAGE = "quickload_cache_usage";
	public static final int CACHE_USAGE_DEFAULT = LocalUrlCacher.NORMAL_CACHE;
	public static final String URL_NOT_REACHABLE = "URL_NOT_REACHABLE";

	public static final int CONNECT_TIMEOUT = 20000;	// If you can't connect in 20 seconds, fail.
	public static final int READ_TIMEOUT = 60000;		// If you can't read any data in 1 minute, fail.

	private static enum CacheType { FILE, CACHED, STALE_CACHE, NOT_CACHED, UNREACHABLE};

	private static boolean offline = false;

	/** Sets the cacher to off-line mode, in which case only cached data will
	 *  be used, will never try to get data from the web.
	 *
	 * @param b
	 */
	public static void setOffLine(boolean b) {
		offline = b;
	}

	/** Returns the value of the off-line flag.
	 *
	 * @return true if offline
	 */
	public static boolean getOffLine() {
		return offline;
	}

	/** Determines whether the given URL string represents a file URL. */
	private static boolean isFile(String url) {
		if (url == null || url.length() < 5) {
			return false;
		}
		return (url.substring(0, 5).compareToIgnoreCase("file:") == 0);
	}

	public static boolean isFile(URI uri){
		if(uri.getScheme() == null || uri.getScheme().length() == 0 || uri.getScheme().equalsIgnoreCase("file"))
			return true;

		return false;
	}

	public static SeekableStream getSeekableStream(URI uri) throws FileNotFoundException, MalformedURLException{
		if (LocalUrlCacher.isFile(uri)) {
			File f = new File(uri.getPath());
			return new SeekableFileStream(f);
		}
		return new SeekableHTTPStream(uri.toURL());
	}

	public static InputStream getInputStream(URL url) throws IOException {
		return getInputStream(url.toString(), getPreferredCacheUsage(), true, null, null, false);
	}

	public static InputStream getInputStream(URL url, boolean write_to_cache, Map<String, String> rqstHeaders, Map<String, List<String>> respHeaders) throws IOException {
		return getInputStream(url.toString(), getPreferredCacheUsage(), write_to_cache, rqstHeaders, respHeaders, false);
	}

	public static InputStream getInputStream(String url) throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), true, null, null, false);
	}

	public static InputStream getInputStream(String url, boolean write_to_cache, Map<String,String> rqstHeaders)
					throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), write_to_cache, rqstHeaders, null, false);
	}
	public static InputStream getInputStream(String url, boolean write_to_cache, Map<String,String> rqstHeaders, boolean fileMayNotExist)
					throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), write_to_cache, rqstHeaders, null, fileMayNotExist);
	}

	public static InputStream getInputStream(String url, boolean write_to_cache)
					throws IOException {
		return getInputStream(url, getPreferredCacheUsage(), write_to_cache, null, null, false);
	}

	public static InputStream getInputStream(String url, int cache_option, boolean write_to_cache, Map<String,String> rqstHeaders)
					throws IOException {
		return getInputStream(url, cache_option, write_to_cache, rqstHeaders, null, false);
	}

	/**
	 * @param url URL to load.
	 * @param cache_option caching option (should be enum)
	 * @param write_to_cache Write to cache.
	 * @param rqstHeaders a Map which when getInputStream() returns will be populated with any headers returned from the url
	 *      Each entry will be either: { header name ==> header value }
	 *        OR if multiple headers have same name, then value of entry will be a List of the header values:
	 *                                 { header name ==> [header value 1, header value 2, ...] }
	 * headers will get cleared of any entries it had before getting passed as arg
	 * @param fileMayNotExist Don't warn if file doesn't exist.
	 * @return input stream from the loaded url
	 * @throws java.io.IOException
	 */
	private static InputStream getInputStream(
			String url, int cache_option, boolean write_to_cache, Map<String,String> rqstHeaders, Map<String, List<String>> respHeaders, boolean fileMayNotExist)
					throws IOException {
		//look to see if a sessionId is present in the headers
		String sessionId = null;
		if (rqstHeaders != null) {
			if (rqstHeaders.containsKey("sessionId")) {
				sessionId = rqstHeaders.get("sessionId");
			}
			//clear headers
			rqstHeaders.clear();
		}
		
		// if url is a file url, and not caching files, then just directly return stream
		if ((!CACHE_FILE_URLS) && isFile(url)) {
			//Application.getSingleton().logInfo("URL is file url, so not caching: " + furl);
			return getUncachedFileStream(url, sessionId, fileMayNotExist);
		}


		// if NORMAL_CACHE:
		//   if not in cache, then return input stream from http connection
		//   if in cache, then check URL content has changed via GET with if-modified-since header based
		//        on modification date of cached file
		//      if content is returned, check last-modified header just to be sure (some servers might ignore
		//        if-modified-since header?)
		InputStream result_stream = null;
		File cache_file = getCacheFile(cache_content_root, url);
		File header_cache_file = getCacheFile(cache_header_root, url);
		long local_timestamp = -1;
		
		// special-case when one cache file exists, but the other doesn't or is zero-length. Shouldn't happen, really.
		if (cache_file.exists() && (!header_cache_file.exists() || header_cache_file.length() == 0)) {
			cache_file.delete();
		} else if ((!cache_file.exists() || cache_file.length() == 0) && header_cache_file.exists()) {
			header_cache_file.delete();
		}
		
		if ((offline || cache_option != IGNORE_CACHE) && cache_file.exists() && header_cache_file.exists()) {
			local_timestamp = cache_file.lastModified();
		}
		URLConnection conn = null;
		long remote_timestamp = 0;
		boolean url_reachable = false;
		int http_status = -1;

		if (offline) {
			// ignore whatever option was specified when we are offline, only the
			// cache is available.
			cache_option = ONLY_CACHE;
		}

		// if offline or if cache_option == ONLY_CACHE, then don't even try to retrieve from url
		if (cache_option != ONLY_CACHE) {
			try {
				conn = connectToUrl(url, sessionId, local_timestamp);

				if (DEBUG_CONNECTION) {
					reportHeaders(conn);
				}

				if (respHeaders != null) {
					respHeaders.putAll(conn.getHeaderFields());
				}

				remote_timestamp = conn.getLastModified();

				if (conn instanceof HttpURLConnection) {
					HttpURLConnection hcon = (HttpURLConnection) conn;
					http_status = hcon.getResponseCode();
					
					// Status codes:
					//     1xx Informational
					//     2xx Success
					//     3xx Redirection
					//     4xx Client Error
					//     5xx Server Error
					//  So only consider URL reachable if 2xx or 3xx (not quite sure what to do yet with redirection)
					url_reachable = ((http_status >= 200) && (http_status < 400));
				}else {
					// Assuming it to be FtpURLConnection.
					url_reachable = true;
					remote_timestamp = conn.getIfModifiedSince();
				}

			} catch (IOException ioe) {
				url_reachable = false;
			} catch (Exception e) {
				e.printStackTrace();
				url_reachable = false;
			}
			if (!url_reachable) {
				if (!fileMayNotExist) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING,
							"URL not reachable, status code = {0}: {1}", new Object[]{http_status, url});
				}
				if (rqstHeaders != null) {
					rqstHeaders.put("LocalUrlCacher", URL_NOT_REACHABLE);
				}
				if (!cache_file.exists()) {
					if (!fileMayNotExist) {
						Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING,
								"URL {0} is not reachable, and is not cached!", url);
					}
					return null;
				}
			}
		}
		
		// found cached data
		if (local_timestamp != -1) {
			result_stream = TryToRetrieveFromCache(url_reachable, http_status, cache_file, remote_timestamp, local_timestamp, url, cache_option);
			if (rqstHeaders != null) {
				retrieveHeadersFromCache(rqstHeaders, header_cache_file);
			}
		}

		// Need to get data from URL, because no cache hit, or stale, or cache_option set to IGNORE_CACHE...
		if (result_stream == null && url_reachable && (cache_option != ONLY_CACHE)) {
			result_stream = RetrieveFromURL(conn, rqstHeaders, write_to_cache, cache_file, header_cache_file);
		}

		if (rqstHeaders != null && DEBUG_CONNECTION) {
			reportHeaders(url, rqstHeaders);
		}
		if (result_stream == null) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING,
					"couldn''t get content for: {0}", url);
		}
		return result_stream;
	}


	private static URLConnection connectToUrl(String url, String sessionId, long local_timestamp) throws MalformedURLException, IOException {
		URL theurl = new URL(url);
		URLConnection conn = theurl.openConnection();
		conn.setConnectTimeout(CONNECT_TIMEOUT);
		conn.setReadTimeout(READ_TIMEOUT);
		conn.setRequestProperty("Accept-Encoding", "gzip");
		//set sessionId?
		if (sessionId != null) {
			conn.setRequestProperty("Cookie", sessionId);
		}
		if (local_timestamp != -1) {
			conn.setIfModifiedSince(local_timestamp);
		} //    because some method calls on URLConnection like those below don't always throw errors
		//    when connection can't be opened -- which would end up allowing url_reachable to be set to true
		///   even when there's no connection
		conn.connect();
		return conn;
	}


	private static InputStream getUncachedFileStream(String url, String sessionId, boolean fileMayNotExist) throws MalformedURLException, IOException {
		URL furl = new URL(url);
		URLConnection huc = furl.openConnection();
		huc.setConnectTimeout(CONNECT_TIMEOUT);
		huc.setReadTimeout(READ_TIMEOUT);
		//set sessionId
		if (sessionId != null) {
			huc.setRequestProperty("Cookie", sessionId);
		}
		InputStream fstr = null;
		try {
			fstr = huc.getInputStream();
		} catch (FileNotFoundException ex) {
			if (fileMayNotExist) {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
						"Couldn''t find file {0}, but it''s optional.", url);
				return null; // We don't care if the file doesn't exist.
			}
		}
		return fstr;
	}


	/** Returns the local File object for the given URL;
	 *  you must check File.exists() to determine if the file exists in the cache.
	 *
	 * For long URLs, the file may be contained in additional subdirectories of the
	 *    the cache root directory in order to ensure that each path segment is
	 *    within the file name limits of the OS
	 *  If additional subdirectories are needed, getCacheFileForURL automatically creates
	 *     these directories
	 *  The File object returned is created by getCacheFileForURL, but the actual on-disk file is not created --
	 *     that is up to other methods in LocalUrlCacher
	 */
	private static File getCacheFile(String root, String url) {
		File fil = new File(root);
		if (!fil.exists()) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO, "Creating new cache directory: {0}", fil.getAbsolutePath());
			if (!fil.mkdirs()) {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.SEVERE, "Could not create directory: {0}", fil.toString());
			}
		}
		String encoded_url = UrlToFileName.encode(url);
		String cache_file_name = root + encoded_url;
		// Need to make sure that full path of file is < 255 characters to ensure
		//    cross-platform compatibility (some OS allow any length, some only restrict file name
		//    length (last path segment), but there are some that restrict full path to <= 255 characters
		if (cache_file_name.length() > 255) {
			cache_file_name = root + UrlToFileName.toMd5(encoded_url);
		}
		File cache_file = new File(cache_file_name);
		return cache_file;
	}

	/**
	 * Invalidate cache file so it will be rebuilt if needed.
	 * @param url
	 */
	public static void invalidateCacheFile(String url) {
		File cache_file = getCacheFile(cache_content_root, url);
		if (cache_file.exists()) {
			if (!cache_file.delete()) {
				cache_file.deleteOnExit();	// something went wrong.  Try to delete it later
			}
		}

		File header_cache_file = getCacheFile(cache_header_root, url);
		if (header_cache_file.exists()) {
			if (!header_cache_file.delete()) {
				header_cache_file.deleteOnExit();	// something went wrong.  Try to delete it later
			}
		}
	}

	private static InputStream TryToRetrieveFromCache(
			boolean url_reachable, int http_status, File cache_file, long remote_timestamp, long local_timestamp,
			String url, int cache_option)
			throws IOException, FileNotFoundException {
		if (url_reachable) {
			//  has a timestamp and response contents not modified since local cached copy last modified, so use local
			if (http_status == HttpURLConnection.HTTP_NOT_MODIFIED) {
				if (DEBUG_CONNECTION) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
							"Received HTTP_NOT_MODIFIED status for URL, using cache: {0}", cache_file);
				}		
			} else if (remote_timestamp > 0 && remote_timestamp <= local_timestamp) {
				if (DEBUG_CONNECTION) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
							"Cache exists and is more recent, using cache: {0}", cache_file);
				}
			} else {
				if (DEBUG_CONNECTION) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
							"cached file exists, but URL is more recent, so reloading cache: {0}", url);
				}
				return null;
			}
		} else {
			// url is not reachable
			if (cache_option != ONLY_CACHE) {
				if (DEBUG_CONNECTION) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING,
							"Remote URL not reachable: {0}", url);
				}
			}
			if (DEBUG_CONNECTION) {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
						"Loading cached file for URL: {0}", url);
			}
			
		}
		return new BufferedInputStream(new FileInputStream(cache_file));
	}

	private static void retrieveHeadersFromCache(Map<String, String> rqstHeaders, File header_cache_file) throws IOException {
		// using cached content, so should also use cached headers
		//   eventually want to improve so headers get updated if server is accessed and url is reachable
		BufferedInputStream hbis = null;
		try {
			hbis = new BufferedInputStream(new FileInputStream(header_cache_file));
			Properties headerprops = new Properties();
			headerprops.load(hbis);
			for (String propKey : headerprops.stringPropertyNames()) {
				rqstHeaders.put(propKey, headerprops.getProperty(propKey));
			}
		} finally {
			GeneralUtils.safeClose(hbis);
		}
	}

	/**
	 * Retrieve a page from a URL, optionally storing it in the cache.
	 *
	 * @param conn
	 * @param headers
	 * @param write_to_cache
	 * @param cache_file
	 * @param header_cache_file
	 * @return
	 * @throws IOException
	 */
	private static InputStream RetrieveFromURL(
			URLConnection conn, Map<String,String> headers, boolean write_to_cache, File cache_file, File header_cache_file) throws IOException {
		final InputStream connstr;
		String contentEncoding = conn.getHeaderField("Content-Encoding");
		boolean isGZipped = contentEncoding == null ? false : "gzip".equalsIgnoreCase(contentEncoding);
		if (isGZipped) {
			// unknown content length, stick with -1
			connstr = new GZIPInputStream(conn.getInputStream());
			if (DEBUG_CONNECTION) {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.FINE,
						"gzipped stream, so ignoring reported content length of {0}", conn.getContentLength());
			}
		} else {
			connstr = conn.getInputStream();
		}

		if (write_to_cache) {
			writeHeadersToCache(header_cache_file, populateHeaderProperties(conn, headers));
			return new CachingInputStream(connstr, cache_file, conn.getURL().toExternalForm());
		} else {
			return connstr;
		}
	}


	// populating header Properties (for persisting) and header input Map
	private static Properties populateHeaderProperties(URLConnection conn, Map<String, String> headers) {
		Map<String, List<String>> headermap = conn.getHeaderFields();
		Properties headerprops = new Properties();
		for (Map.Entry<String, List<String>> ent : headermap.entrySet()) {
			String key = ent.getKey();
			// making all header names lower-case
			List<String> vals = ent.getValue();
			if (vals.isEmpty()) {
				continue;
			}
			String val = vals.get(0);
			if (key == null) {
				key = HTTP_STATUS_HEADER;
			} // HTTP status code line has a null key, change so can be stored
			key = key.toLowerCase();
			headerprops.setProperty(key, val);
			if (headers != null) {
				headers.put(key, val);
			}
		}
		return headerprops;
	}

	private static void reportHeaders(String url, Map<String, String> headers) {
		Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
				"   HEADERS for URL: {0}", url);
		for (Map.Entry<String, String> ent : headers.entrySet()) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
					"   key: {0}, val: {1}", new Object[]{ent.getKey(), ent.getValue()});
		}
	}


	/**
	 *  Forces flushing of entire cache.
	 *  Simply removes all cached files.
	 */
	public static void clearCache() {
		Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO, "Clearing cache");
		DeleteFilesInDirectory(cache_header_root);
		DeleteFilesInDirectory(cache_content_root);
	}

	private static void DeleteFilesInDirectory(String filename) {
		File dir = new File(filename);
		if (dir.exists()) {
			for (File fil : dir.listFiles()) {
				fil.delete();
			}
		}
	}

	/** Returns the location of the root directory of the cache.
	 * @return 
	 */
	public static String getCacheRoot() {
		return cache_content_root;
	}

	/** Returns the current value of the persistent user preference PREF_CACHE_USAGE.
	 *
	 * @return the preferred cache usage
	 */
	public static int getPreferredCacheUsage() {
		return PreferenceUtils.getIntParam(PREF_CACHE_USAGE, CACHE_USAGE_DEFAULT);
	}

	public static void setPreferredCacheUsage(int usage) {
		Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO, "Setting Caching mode to {0}", getCacheUsage(usage));
		PreferenceUtils.saveIntParam(LocalUrlCacher.PREF_CACHE_USAGE, usage);
	}

	public static void reportHeaders(URLConnection query_con) {
		try {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
					"URL: {0}", query_con.getURL().toString());
			int hindex = 0;
			while (true) {
				String val = query_con.getHeaderField(hindex);
				String key = query_con.getHeaderFieldKey(hindex);
				if (val == null && key == null) {
					break;
				}
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
						"   header:   key = {0}, val = {1}", new Object[]{key, val});
				hindex++;
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static void loadSynonyms(SynonymLookup lookup, String synonym_loc) {
		InputStream syn_stream = null;
		try {
			// Don't cache.  Don't warn user if the synonyms file doesn't exist.
			syn_stream = LocalUrlCacher.getInputStream(synonym_loc, getPreferredCacheUsage(), false, null, null, true);
			if (syn_stream == null) {
				return;
			}
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
					"Synonyms found at: {0}", synonym_loc);
			lookup.loadSynonyms(syn_stream);
		} catch (IOException ioe) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING, "Unable to load synonyms from '" + synonym_loc + "'", ioe);
		} finally {
			GeneralUtils.safeClose(syn_stream);
		}
	}

	private static void writeHeadersToCache(File header_cache_file, Properties headerprops) throws IOException {
		// cache headers also -- in [cache_dir]/headers ?
		if (DEBUG_CONNECTION) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.INFO,
					"writing headers to cache: {0}", header_cache_file.getPath());
		}
		BufferedOutputStream hbos = null;
		try {
			hbos = new BufferedOutputStream(new FileOutputStream(header_cache_file));
			headerprops.store(hbos, null);
		} finally {
			GeneralUtils.safeClose(hbos);
		}
	}

	public static CacheUsage getCacheUsage(int usage) {
		for(CacheUsage u : CacheUsage.values()) {
			if(u.usage == usage) { return u; }
		}

		return null;
	}

	public static File convertURIToFile(URI uri) {
		return convertURIToFile(uri, false);
	}

	public static File convertURIToFile(URI uri, boolean fileMayNotExist) {
		if (uri.getScheme() == null) {
			// attempt to find a local file
		}
		if (isFile(uri)) {
			File f = new File(uri);
			if (!GeneralUtils.getUnzippedName(f.getName()).equalsIgnoreCase(f.getName())) {
				try {
					File f2 = File.createTempFile(f.getName(), null);
					f2.deleteOnExit();	// This is only a temporary file!  Delete on exit.
					GeneralUtils.unzipFile(f, f2);
					return f2;
				} catch (IOException ex) {
					Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.SEVERE, null, ex);
					return null;
				}
			}
			return f;
		}
		String scheme = uri.getScheme().toLowerCase();
		if (scheme.startsWith("http") || scheme.startsWith("ftp")) {
			InputStream istr = null;
			try {
				String uriStr = uri.toString();
				istr = LocalUrlCacher.getInputStream(uriStr, false, null, fileMayNotExist);

				if(istr == null)
					return null;
				
				StringBuffer stripped_name = new StringBuffer();
				InputStream str = GeneralUtils.unzipStream(istr, uriStr, stripped_name);
				String stream_name = stripped_name.toString();
				if (str instanceof BufferedInputStream) {
					str = (BufferedInputStream) str;
				} else {
					str = new BufferedInputStream(str);
				}
				return GeneralUtils.convertStreamToFile(str, stream_name.substring(stream_name.lastIndexOf("/")));
			} catch (IOException ex) {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.SEVERE, null, ex);
			} finally {
				GeneralUtils.safeClose(istr);
			}
		}
		Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.SEVERE,
				"URL scheme: {0} not recognized", scheme);
		return null;
	}

	/**
	 * Get stream associated with this uri.  Don't unzip here.
	 * @param uri
	 * @return
	 */
	public static BufferedInputStream convertURIToBufferedUnzippedStream(URI uri) {
		String scheme = uri.getScheme().toLowerCase();
		InputStream is = null;
		try {
			if (scheme.length() == 0 || scheme.equals("file")) {
				is = new FileInputStream(new File(uri));
			} else if (scheme.startsWith("http") || scheme.startsWith("ftp")) {
				is = LocalUrlCacher.getInputStream(uri.toString());
			} else {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.SEVERE,
						"URL scheme: {0} not recognized", scheme);
				return null;
			}

			StringBuffer stripped_name = new StringBuffer();
			InputStream str = GeneralUtils.unzipStream(is, uri.toString(), stripped_name);
			if (str instanceof BufferedInputStream) {
				return (BufferedInputStream) str;
			}
			return new BufferedInputStream(str);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	/**
	 * Get stream associated with this uri.  Don't unzip here.
	 * @param uri
	 * @return
	 */
	public static BufferedInputStream convertURIToBufferedStream(URI uri) {
		String scheme = uri.getScheme().toLowerCase();
		InputStream is = null;
		try {
			if (scheme.length() == 0 || scheme.equals("file")) {
				is = new FileInputStream(new File(uri));
			} else if (scheme.startsWith("http") || scheme.startsWith("ftp")) {
				is = LocalUrlCacher.getInputStream(uri.toString());
			} else {
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.SEVERE,
						"URL scheme: {0} not recognized", scheme);
				return null;
			}

			if (is instanceof BufferedInputStream) {
				return (BufferedInputStream) is;
			}
			return new BufferedInputStream(is);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return null;
	}

	public static boolean isValidURL(String url){
		URI uri = null;

		try {
			uri = new URI(url);
			return isValidURI(uri);
		} catch (URISyntaxException ex) {
			Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING, null, "Invalid url :" + url);
		}
		
		return false;
	}
	
	public static boolean isValidURI(URI uri){
		
		String scheme = uri.getScheme().toLowerCase();
		if (scheme.length() == 0 || scheme.equals("file")) {
			File f = new File(uri);
			if(f != null && f.exists()){
				return true;
			}
		}

		if (scheme.startsWith("http") || scheme.startsWith("ftp")) {
			InputStream istr = null;
			URLConnection conn = null;
			try {

				conn = uri.toURL().openConnection();
				conn.setConnectTimeout(CONNECT_TIMEOUT);
				conn.setReadTimeout(READ_TIMEOUT);
				istr = conn.getInputStream();

				if(istr != null)
					return true;

				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING, "Invalid uri :{0}", uri.toString());
			}catch(Exception ex){
				Logger.getLogger(LocalUrlCacher.class.getName()).log(Level.WARNING, "Invalid uri :{0}", uri.toString());
			}finally{ 
				GeneralUtils.safeClose(istr);
			}
		}
		
		return false;
	}
}
