package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.das.DasServerInfo;
import com.affymetrix.genometryImpl.das.DasSource;
import com.affymetrix.genometryImpl.das2.Das2ServerInfo;
import com.affymetrix.genometryImpl.das2.Das2Source;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingWorker;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A class to get meta data from all servers.
 * @author hiralv
 */
public class CacheScript extends Thread {

	private static final String dsn = "dsn.xml";
	private static final String temp = "temp";

	/** boolean to indicate should script continue to run if error occurs **/
	private static final boolean exitOnError = false;
	
	/** Local path where data is cached. **/
	private final String path;
	/** List of server to be cached. **/
	private final Set<GenericServer> server_list;

	/** For files too be looked up on server. **/
	private static final Set<String> quickloadFiles = new HashSet<String>();
	private static final Set<String> das2Files = new HashSet<String>();

	/** Add files to be looked up. **/
	static{
		quickloadFiles.add(Constants.annotsTxt);
		quickloadFiles.add(Constants.annotsXml);
		quickloadFiles.add(Constants.modChromInfoTxt);
		quickloadFiles.add(Constants.liftAllLft);
	}

	static{
		das2Files.add(Das2VersionedSource.TYPES_CAP_QUERY);
		das2Files.add(Das2VersionedSource.SEGMENTS_CAP_QUERY);
	}

	/** Default server list. **/
	private static final String defaultList =	" <servers> " +

//												" <server type='das2' name='NetAffx Das2' url='http://netaffxdas.affymetrix.com/das2/genome' />" +
//												" <server type='quickload' name='NetAffx Quickload' url='http://netaffxdas.affymetrix.com/quickload_data' />" +

//												" <server type='das2' name='Bioviz Das2' url='http://bioviz.org/das2/genome' />" +
//												" <server type='quickload' name='Bioviz Quickload' url='http://bioviz.org/quickload/' />" +

												" <server type='das' name='UCSC Das' url='http://genome.cse.ucsc.edu/cgi-bin/das/dsn' />" +

//												" <server type='quickload' name='HughesLab' url='http://hugheslab.ccbr.utoronto.ca/igb/' />" +


												" <server type='das' name='Ensembl' url='http://www.ensembl.org/das/dsn' enabled='false' />" +
//												" <server type='das2' name='UofUtahBioinfoCore' url='http://bioserver.hci.utah.edu:8080/DAS2DB/genome' enabled='false' />" +

												" </servers> ";

	public CacheScript(String path, Set<GenericServer> server_list){
		this.path = path;
		this.server_list = server_list;
	}

	/**
	 * Runs caching script for given set of server list.
	 */
	@Override
	public void run() {
		for (final GenericServer gServer : server_list) {

			final Timer ser_tim = new Timer();
			ExecutorService vexec = Executors.newSingleThreadExecutor();
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

				protected Void doInBackground() {
					ser_tim.start();
					if(processServer(gServer, path))
						copyDirectoryFor(path,gServer.serverName);
					return null;
				}

				@Override
				public void done() {
					Logger.getLogger(CacheScript.class.getName()).log(Level.INFO, "Time required to cache " + gServer.serverName + " :" + (ser_tim.read() / 1000f), ser_tim);
				}
			};
			vexec.execute(worker);
			vexec.shutdown();
		}
	}

	/**
	 * Create serverMapping.txt and adds server name and corresponding directory to it.
	 * @param server_list
	 */
	public void writeServerMapping(){
		FileOutputStream fos = null;
		PrintStream out = null;
		try {
			File mapping = new File(path + "/" + Constants.serverMapping);
			mapping.createNewFile();
			fos = new FileOutputStream(mapping);
			out = new PrintStream(fos);
			for (final GenericServer gServer : server_list) {
				out.println(gServer.URL + "\t" + gServer.serverName);
			}
		} catch (IOException ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(fos);
			GeneralUtils.safeClose(out);
		}
	}

	/**
	 * Creates directory of server name.
	 * Determines the server type and process it accordingly.
	 * @param gServer	GenericServer to be processed.
	 */
	private static boolean processServer(GenericServer gServer, String path){
		Logger.getLogger(CacheScript.class.getName()).log(Level.FINE, "Caching {0} at path {1}", new Object[]{gServer.serverName, path});

		String serverCachePath = path+gServer.serverName+temp;
		GeneralUtils.makeDir(serverCachePath);

		switch(gServer.serverType){
			case QuickLoad:
				return processQuickLoad(gServer, serverCachePath);
				

			case DAS2:
				return processDas2Server(gServer, serverCachePath);
				

			case DAS:
				return processDasServer(gServer, serverCachePath);
		}

		return false;
	}

	/**
	 * Gets files for all genomes from Quickload server and copies it to appropriate directory.
	 * @param gServer	GenericServer from where mapping are fetched.
	 * @param serverCachePath	Local path where fetched files are stored.
	 * @return
	 */
	private static boolean processQuickLoad(GenericServer gServer, String serverCachePath){
		File file = GeneralUtils.getFile(gServer.URL+Constants.contentsTxt, false);

		String quickloadStr = null;
		quickloadStr = (String) gServer.serverObj;
		
		QuickLoadServerModel quickloadServer = new QuickLoadServerModel(quickloadStr);

		List<String> genome_names = quickloadServer.getGenomeNames();
		if(!GeneralUtils.moveFileTo(file,Constants.contentsTxt,serverCachePath))
			return false;
		
		for(String genome_name : genome_names){
			if(!getAllFiles(gServer,genome_name,serverCachePath)){
				Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, "Could not find all files for {0} !!!", gServer.serverName);
				return false;
			}
		}

		return true;
	}

	/**
	 * Gets files for all genomes from Das2 server and copies it to appropriate directory.
	 * @param gServer	GenericServer from where mapping are fetched.
	 * @param serverCachePath	Local path where fetched files are stored.
	 * @return
	 */
	private static boolean processDas2Server(GenericServer gServer, String serverCachePath){
		File file = GeneralUtils.getFile(gServer.URL, false);
		if(!GeneralUtils.moveFileTo(file, Constants.GENOME_SEQ_ID+ Constants.xml_ext, serverCachePath))
			return false;
		
		Das2ServerInfo serverInfo = (Das2ServerInfo) gServer.serverObj;
		Map<String,Das2Source> sources = serverInfo.getSources();
		
		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.WARNING,"Couldn't find species for server: ",gServer);
			return false;
		}

		for (Das2Source source : sources.values()) {
			// Das/2 has versioned sources.  Get each version.
			for (Das2VersionedSource versionSource : source.getVersions().values()) {
				if(!getAllFiles(gServer,versionSource.getName(),serverCachePath)){
					Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, "Could not find all files for {0} !!!", gServer.serverName);
					return false;
				}
			}
		}

		return true;
	}

	/**
	 * Returns true if file may not exist else false.
	 * @param fileName
	 * @return
	 */
	private static boolean getFileAvailability(String fileName){
		if(fileName.equals(Constants.annotsTxt) || fileName.equals(Constants.annotsXml) || fileName.equals(Constants.liftAllLft))
			return true;

		return false;
	}

	/**
	 * Gets files for a genome and copies it to it's directory.
	 * @param servertype	Server type to determine which set of files to be used.
	 * @param server_path	Server path from where mapping is to be copied.
	 * @param local_path	Local path from where mapping is to saved.
	 */
	private static boolean getAllFiles(GenericServer gServer, String genome_name, String local_path){
		File file;
		Set<String> files = null;

		switch(gServer.serverType){
			case QuickLoad:
				files = quickloadFiles;
				break;

			case DAS2:
				files = das2Files;
				break;

			default:
				return false;
		}

		String server_path = gServer.URL + "/" + genome_name;
		local_path += "/" + genome_name;
		GeneralUtils.makeDir(local_path);
		boolean fileMayNotExist;
		for(String fileName : files){
			fileMayNotExist = getFileAvailability(fileName);

			file = GeneralUtils.getFile(server_path+"/"+fileName, fileMayNotExist);

			if(gServer.serverType.equals(ServerType.DAS2))
				fileName += Constants.xml_ext;

			if((file == null && !fileMayNotExist))
				return false;

			if(!GeneralUtils.moveFileTo(file,fileName,local_path))
				return false;
		}

		return true;
	}
	
	/**
	 * Gets files for all genomes from Das server and copies it to appropriate directory.
	 * @param gServer	GenericServer from where mapping are fetched.
	 * @param serverCachePath	Local path where fetched files are stored.
	 * @return
	 */
	private static boolean processDasServer(GenericServer gServer, String serverCachePath){
		File file = GeneralUtils.getFile(gServer.URL, false);
		if(!GeneralUtils.moveFileTo(file,dsn,serverCachePath))
			return false;
		
		DasServerInfo server = (DasServerInfo) gServer.serverObj;
		Map<String, DasSource> sources = server.getDataSources();

		if (sources == null || sources.values() == null || sources.values().isEmpty()) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.WARNING,"Couldn't find species for server: ",gServer);
			return false;
		}

		for (DasSource source : sources.values()) {
			
			if(!getAllDasFiles(source.getID(),source.getServerURL(), source.getMasterURL(), serverCachePath)){
				Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, "Could not find all files for {0} !!!", gServer.serverName);
				return false;
			}

			for(String src : source.getSources()){
				if(!getAllDasFiles(src,source.getServerURL(), source.getMasterURL(), serverCachePath)){
					Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, "Could not find all files for {0} !!!", gServer.serverName);
					return false;
				}
			}

		}

		return true;
	}

	/**
	 * Gets files for a genome and copies it to it's directory.
	 * @param server_path	Server path from where mapping is to be copied.
	 * @param local_path	Local path from where mapping is to saved.
	 */
	private static boolean getAllDasFiles(String id, URL server, URL master, String local_path){
		local_path += "/" + id;
		GeneralUtils.makeDir(local_path);

		File file;
		final Map<String, String> DasFilePath = new HashMap<String, String>();

		String entry_point = getPath(master.getPath(),master, DasSource.ENTRY_POINTS);
		
		String types = getPath(id,server,DasSource.TYPES);

		DasFilePath.put(entry_point, DasSource.ENTRY_POINTS + Constants.xml_ext);
		DasFilePath.put(types, DasSource.TYPES + Constants.xml_ext);

		for(Entry<String, String> fileDet : DasFilePath.entrySet()){
			file = GeneralUtils.getFile(fileDet.getKey(), false);

			if((file == null || !GeneralUtils.moveFileTo(file,fileDet.getValue(),local_path)) && exitOnError)
				return false;

		}

		return true;
	}

	/**
	 * Returns server path for a mapping on Das server.
	 * @param id	Genome id
	 * @param server	Server url.
	 * @param mapping	File name.
	 * @return
	 */
	private static String getPath(String id, URL server, String file){
		try {
			URL server_path = new URL(server, id + "/" + file);
			return server_path.toExternalForm();
		} catch (MalformedURLException ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		}
		return null;
	}

	/**
	 * Parses xml mapping of server list.
	 * @param istr
	 * @return	Returns a list of generic server.
	 */
	private static Set<GenericServer> parseServerList(InputStream istr) throws Exception {
		Set<GenericServer> serverList = new HashSet<GenericServer>();

		Document list = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(istr);
		Element top_element = list.getDocumentElement();
		String topname = top_element.getTagName();
		if (!(topname.equalsIgnoreCase("servers"))) {
			System.err.println("not a server list file -- can't parse!");
		}
		NodeList children = top_element.getChildNodes();
		Node child;
		String name;
		Element el = null;

		for (int i = 0; i < children.getLength(); i++) {
			child = children.item(i);
			name = child.getNodeName();
			if (child instanceof Element) {
				el = (Element) child;
				if (name.equalsIgnoreCase("server")) {
					ServerType server_type = getServerType(el.getAttribute("type"));
					String server_name = el.getAttribute("name").replaceAll("\\W","");
					String server_url = el.getAttribute("url");
					String en = el.getAttribute("enabled");
					Boolean enabled = en == null || en.isEmpty() ? true : Boolean.valueOf(en);

					String serverURL = ServerUtils.formatURL(server_url, server_type);
					Object serverInfo = ServerUtils.getServerInfo(server_type, serverURL, server_name);
					GenericServer server = new GenericServer(server_name, serverURL, server_type, enabled, serverInfo);
					serverList.add(server);
				}
			}
		}

		return serverList;
	}

	/**
	 * Returns server type.
	 * @param type	Type name.
	 * @return
	 */
	private static ServerType getServerType(String type) {
		for (ServerType t : ServerType.values()) {
			if (type.equalsIgnoreCase(t.toString())) {
				return t;
			}
		}
		return null;
	}

	/**
	 * Recursively copies data from source to destination.
	 * @param source	Source directory.
	 * @param dest		Destination directory.
	 */
	private static void copyRecursively(File source, File dest){
		for(File file: source.listFiles()){
			if(file.isDirectory()){
				copyRecursively(file,GeneralUtils.makeDir(dest.getPath()+ "/" +file.getName()));
			}else{
				GeneralUtils.moveFileTo(file,file.getName(),dest.getPath());
			}
		}
	}

	/**
	 * Recursively copies directory data for given server name.
	 * @param servername	Name of the server.
	 */
	private static void copyDirectoryFor(String path, String servername){
		File temp_dir = new File(path + servername + temp);

		String perm_path = path + servername;
		GeneralUtils.makeDir(perm_path);

		File perm_dir = new File(perm_path);

		copyRecursively(temp_dir,perm_dir);
	}

	static public void main(String[] args){
		InputStream istr = null;
		try {
			istr = new ByteArrayInputStream(defaultList.getBytes());
			String path = "/";

			if(args.length >= 1)
				path = args[0];
			
			Set<GenericServer> server_list = parseServerList(istr);
			CacheScript script = new CacheScript(path,server_list);
			script.start();
			script.writeServerMapping();
		} catch (Exception ex) {
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(istr);
		}
				
	}

}
