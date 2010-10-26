package com.affymetrix.igb.general;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.parsers.FastaParser;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.genometryImpl.das.DasLoader;
import com.affymetrix.genometryImpl.das2.Das2VersionedSource;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.quickload.QuickLoadServerModel;
import com.affymetrix.genometryImpl.symloader.BNIB;
import com.affymetrix.genometryImpl.symloader.TwoBit;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.view.SeqMapView;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @version $Id: ResidueLoading.java 6863 2010-09-07 20:46:43Z jnicol $
 */
public final class ResidueLoading {

	enum FORMAT {
		RAW,
		BNIB,
		FASTA
	};

	enum QFORMAT{
		BNIB,
		TWOBIT,
		FA
	};

	/**
	 * Get residues from servers: DAS/2, Quickload, or DAS/1.
	 * Also gets partial residues.
	 * @param versionsWithChrom	-- list of servers that have this chromosome.
	 * @param genomeVersionName -- name of the genome.
	 * @param seq_name -- sequence (chromosome) name
	 * @param span	-- May be null.  If not, then it's used for partial loading.
	 * @return boolean
	 */
	// Most confusing thing here -- certain parsers update the composition, and certain ones do not.
	// DAS/1 and partial loading in DAS/2 do not update the composition, so it's done separately.
	public static boolean getResidues(
			Set<GenericVersion> versionsWithChrom, String genomeVersionName, String seq_name, int min, int max, BioSeq aseq, SeqSpan span) {

		boolean partial_load = (min > 0 || max < (aseq.getLength()-1));	// Are we only asking for part of the sequence?

		final SeqMapView gviewer = Application.getSingleton().getMapView();

		if (partial_load) {
			return loadPartial(versionsWithChrom, genomeVersionName, seq_name, min, max, aseq, span, gviewer);
		}

		return loadFull(versionsWithChrom, genomeVersionName, seq_name, min, max, aseq, gviewer);
	}


	/**
	 * Partial load, supported by DAS2 and DAS1.
	 * @param versionsWithChrom
	 * @param genomeVersionName
	 * @param seq_name
	 * @param min
	 * @param max
	 * @param aseq
	 * @param span
	 * @param gviewer
	 * @return
	 */
	private static boolean loadPartial(Set<GenericVersion> versionsWithChrom, String genomeVersionName, String seq_name, int min, int max, BioSeq aseq, SeqSpan span, final SeqMapView gviewer) {
		//Try to check if format data is available from Das2
		for (GenericVersion version : versionsWithChrom) {
			GenericServer server = version.gServer;
			if (server.serverType == ServerType.DAS2) {
				Das2VersionedSource das2version = (Das2VersionedSource) version.versionSourceObj;
				Set<String> format = das2version.getResidueFormat(seq_name);

				if(format == null || format.isEmpty())
					continue;
				
				String uri = null;
				if (format.contains("raw")) {
					uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.RAW);
				} else if (format.contains("fasta") || format.contains("fa")) {
					uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.FASTA);
				}

				String residues = GetPartialFASTADas2Residues(uri);
				if (residues != null) {
					// span is non-null, here
					BioSeq.addResiduesToComposition(aseq, residues, span);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}


			}
		}


		// If no format information is available then try all formats.
		// Try to load in raw format from DAS2 server.
		for (GenericVersion version : versionsWithChrom) {
			GenericServer server = version.gServer;
			if (server.serverType == ServerType.DAS2) {
				String uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.RAW);
				String residues = GetPartialFASTADas2Residues(uri);
				if (residues != null) {
					// span is non-null, here
					BioSeq.addResiduesToComposition(aseq, residues, span);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}

		// Try to load in fasta format from DAS2 server.
		for (GenericVersion version : versionsWithChrom) {
			GenericServer server = version.gServer;
			if (server.serverType == ServerType.DAS2) {
				String uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.FASTA);
				String residues = GetPartialFASTADas2Residues(uri);
				if (residues != null) {
					// span is non-null, here
					BioSeq.addResiduesToComposition(aseq, residues, span);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}

		//Try to load from Quickload server. Try in order bnib, 2bit and fasta.
		AnnotatedSeqGroup seq_group = aseq.getSeqGroup();
		for (GenericVersion version : versionsWithChrom) {
			GenericServer server = version.gServer;
			if (server.serverType == ServerType.QuickLoad) {
				String path = "";
				try {
					URL quickloadURL = new URL((String) server.serverObj);
					QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
					path = quickloadServer.getPath(version.versionName, seq_name);
				} catch (MalformedURLException ex) {
					Logger.getLogger(ResidueLoading.class.getName()).log(Level.SEVERE, null, ex);
				}

				String residues = GetQuickLoadResidues(seq_group, path, server.URL, span);
				if (residues != null) {
					BioSeq.addResiduesToComposition(aseq, residues, span);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}
		
		// Try to load via DAS/1 server.
		for (GenericVersion version : versionsWithChrom) {
			if (version.gServer.serverType == ServerType.DAS) {
				String residues = DasLoader.getDasResidues(version, seq_name, min, max);
				if (residues != null) {
					// Add to composition if we're doing a partial sequence
					// span is non-null, here
					BioSeq.addResiduesToComposition(aseq, residues, span);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}
		
		return false;
	}


	/**
	 * Full load, supported by DAS2, QuickLoad, and DAS1.
	 * Try to load in BNIB format before anything else.
	 * @param versionsWithChrom
	 * @param genomeVersionName
	 * @param seq_name
	 * @param min
	 * @param max
	 * @param aseq
	 * @param gviewer
	 * @param seq_group
	 * @return
	 */
	private static boolean loadFull(
			Set<GenericVersion> versionsWithChrom, String genomeVersionName, String seq_name, int min, int max, BioSeq aseq, final SeqMapView gviewer) {
		AnnotatedSeqGroup seq_group = aseq.getSeqGroup();

		//Try to check if format data is available from Das2
		for (GenericVersion version : versionsWithChrom) {
			GenericServer server = version.gServer;
			if (server.serverType == ServerType.DAS2) {
				Das2VersionedSource das2version = (Das2VersionedSource) version.versionSourceObj;
				Set<String> format = das2version.getResidueFormat(seq_name);

				if(format == null || format.isEmpty())
					continue;

				String uri = null;
				if (format.contains("bnib")) {
					uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.BNIB);
				}else if (format.contains("raw")) {
					uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.RAW);
				} else if (format.contains("fasta") || format.contains("fa")) {
					uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.FASTA);
				}

				if (LoadResiduesFromDAS2(seq_group, uri)) {
					BioSeq.addResiduesToComposition(aseq);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}


			}
		}

		// If no format information is available then try all formats.
		// Try to load in bnib format, as this format is more compactly represented internally.
		// Try loading from DAS/2.
		for (GenericVersion version : versionsWithChrom) {
			GenericServer server = version.gServer;
			if (server.serverType == ServerType.DAS2) {
				String uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.BNIB);
				if (LoadResiduesFromDAS2(seq_group, uri)) {
					BioSeq.addResiduesToComposition(aseq);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}
		//Try to load from Quickload server.
		for (GenericVersion version : versionsWithChrom) {
			GenericServer server = version.gServer;
			if (server.serverType == ServerType.QuickLoad) {
				String path = "";
				try {
					URL quickloadURL = new URL((String) server.serverObj);
					QuickLoadServerModel quickloadServer = QuickLoadServerModel.getQLModelForURL(quickloadURL);
					path = quickloadServer.getPath(version.versionName, seq_name);
				} catch (MalformedURLException ex) {
					Logger.getLogger(ResidueLoading.class.getName()).log(Level.SEVERE, null, ex);
				}

				if (GetQuickLoadResidues(seq_group, path, server.URL)) {
					BioSeq.addResiduesToComposition(aseq);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}

		// Try to load in RAW format from DAS2 server.
		for (GenericVersion version : versionsWithChrom) {
			GenericServer server = version.gServer;
			if (server.serverType == ServerType.DAS2) {
				String uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.RAW);
				String residues = LoadResiduesFromDAS2(uri);
				if (residues != null) {
					aseq.setResidues(residues);
					BioSeq.addResiduesToComposition(aseq);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}
		// Try to load in fasta format from DAS2 server.
		for (GenericVersion version : versionsWithChrom) {
			GenericServer server = version.gServer;
			if (server.serverType == ServerType.DAS2) {
				String uri = generateDas2URI(server.URL, genomeVersionName, seq_name, min, max, FORMAT.FASTA);
				if (LoadResiduesFromDAS2(seq_group, uri)) {
					BioSeq.addResiduesToComposition(aseq);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}
		
		// Try to load via DAS/1 server.
		for (GenericVersion version : versionsWithChrom) {
			if (version.gServer.serverType == ServerType.DAS) {
				String residues = DasLoader.getDasResidues(version, seq_name, min, max);
				if (residues != null) {
					aseq.setResidues(residues);
					BioSeq.addResiduesToComposition(aseq);
					gviewer.setAnnotatedSeq(aseq, true, true, true);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Get the partial residues from the specified QuickLoad server.
	 * @param seq_group
	 * @param path
	 * @param root_url
	 * @param span
	 * @return residue String.
	 */

	private static String GetQuickLoadResidues(AnnotatedSeqGroup seq_group, String path, String root_url, SeqSpan span){
		String common_url = root_url + path + ".";

		SymLoader symloader = determineLoader(common_url, seq_group);

		if(symloader != null )
			return symloader.getRegionResidues(span);
		
		return null;
	}

	private static SymLoader determineLoader(String common_url, AnnotatedSeqGroup seq_group){
		QFORMAT format = determineFormat(common_url);

		if(format == null)
			return null;

		URI uri = null;
		try {
			uri = new URI(generateQuickLoadURI(common_url, format));
		} catch (URISyntaxException ex) {
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.SEVERE, null, ex);
		}

		switch(format){
			case BNIB:
				return new BNIB(uri, seq_group);

			case TWOBIT:
				return new TwoBit(uri);

//			case FA:
//				return new Fasta(uri, seq_group);
		}

		return null;
	}

	private static QFORMAT determineFormat(String common_url){

		for(QFORMAT format : QFORMAT.values()){
			String url_path = generateQuickLoadURI(common_url,format);
			if(LocalUrlCacher.isValidURL(url_path)){
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE,
							"  Quickload location of bnib file: {0}", url_path);
				
				return format;
			}
		}
		
		return null;
	}
	/**
	 * Get the residues from the specified QuickLoad server.
	 * @param seq_group
	 * @param path
	 * @param root_url
	 * @return true or false
	 */
	private static boolean GetQuickLoadResidues(AnnotatedSeqGroup seq_group, String path, String root_url) {
		String url_path = root_url + path + ".bnib";

		Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE,
					"  Quickload location of bnib file: {0}", url_path);
		InputStream istr = null;
		try {
			istr = LocalUrlCacher.getInputStream(url_path, true);
			if (istr == null) {
				return false;
			}
			// NibbleResiduesParser handles creating a BufferedInputStream from the input stream
			NibbleResiduesParser.parse(istr, seq_group);
			return true;
		} catch (IOException ex) {
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.SEVERE, "Can not access sequence: " + url_path, ex);
		} finally {
			GeneralUtils.safeClose(istr);
		}
		return false;
	}

	// Generate URI (e.g., "http://www.bioviz.org/das2/genome/A_thaliana_TAIR8/chr1?range=0:1000")
	private static String generateDas2URI(String URL, String genomeVersionName,
			String segmentName, int min, int max, FORMAT Format) {
		Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "trying to load residues via DAS/2");
		String uri = URL + "/" + genomeVersionName + "/" + segmentName + "?format=";
		switch(Format)
		{
			case RAW:
				uri += "raw";
				break;

			case BNIB:
				uri += "bnib";
				break;

			case FASTA:
				uri += "fasta";
				break;
		}
		
		if (max > -1) {
			// ranged
			uri = uri + "&range=" + min + ":" + max;
		}

		Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "   request URI: {0}", uri);
		return uri;
	}
	
	// Generate URI (e.g., "http://www.bioviz.org/das2/genome/A_thaliana_TAIR8/chr1.bnib")
	private static String generateQuickLoadURI(String common_url, QFORMAT Format) {
		Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "trying to load residues via Quickload");
		switch(Format)
		{
			case BNIB:
				common_url += "bnib";
				break;

			case FA:
				common_url += "fa";
				break;

			case TWOBIT:
				common_url += "2bit";
				break;

		}

		return common_url;
	}

	// try loading via DAS/2 server that genome was originally modeled from
	private static boolean LoadResiduesFromDAS2(AnnotatedSeqGroup seq_group, String uri) {
		InputStream istr = null;
		Map<String, String> headers = new HashMap<String, String>();
		try {
			istr = LocalUrlCacher.getInputStream(uri, true, headers);
			String content_type = headers.get("content-type");
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE,
						"    response content-type: {0}", content_type);
			if (istr == null || content_type == null) {
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "  Improper response from DAS/2; aborting DAS/2 residues loading.");
				return false;
			}
			if (content_type.equals(NibbleResiduesParser.getMimeType())) {
				// check for bnib format
				// NibbleResiduesParser handles creating a BufferedInputStream from the input stream
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.INFO, "   response is in bnib format, parsing...");
				NibbleResiduesParser.parse(istr, seq_group);
				return true;
			}

			if (content_type.equals(FastaParser.getMimeType())) {
				// check for fasta format
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.INFO, "   response is in fasta format, parsing...");
				FastaParser.parseSingle(istr, seq_group);
				return true;
			}
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "   response is not in accepted format, aborting DAS/2 residues loading");
			return false;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(istr);
		}

		return false;
	}

	private static String LoadResiduesFromDAS2(String uri) {
		InputStream istr = null;
		BufferedReader buff = null;
		Map<String, String> headers = new HashMap<String, String>();
		try {
			istr = LocalUrlCacher.getInputStream(uri, true, headers);
			// System.out.println(headers);
			String content_type = headers.get("content-type");
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE,
						"    response content-type: {0}", content_type);
			if (istr == null || content_type == null) {
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "  Improper response from DAS/2; aborting DAS/2 residues loading.");
				return null;
			}
			if(content_type.equals("text/raw"))
			{
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.INFO, "   response is in raw format, parsing...");
				buff = new BufferedReader(new InputStreamReader(istr));
				return buff.readLine();
			}

			Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "   response is not in accepted format, aborting DAS/2 residues loading");
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(buff);
			GeneralUtils.safeClose(istr);
		}

		return null;
	}

	// try loading via DAS/2 server
	private static String GetPartialFASTADas2Residues(String uri) {
		InputStream istr = null;
		BufferedReader buff = null;
		Map<String, String> headers = new HashMap<String, String>();
		try {
			istr = LocalUrlCacher.getInputStream(uri, true, headers);
			// System.out.println(headers);
			String content_type = headers.get("content-type");
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE,
						"    response content-type: {0}", content_type);
			if (istr == null || content_type == null) {
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "  Didn't get a proper response from DAS/2; aborting DAS/2 residues loading.");
				return null;
			}

			if(content_type.equals("text/raw"))
			{
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.INFO, "   response is in raw format, parsing...");
				buff = new BufferedReader(new InputStreamReader(istr));
				return buff.readLine();
			}

			if (content_type.equals(FastaParser.getMimeType())) {
				// check for fasta format
				Logger.getLogger(ResidueLoading.class.getName()).log(Level.INFO, "   response is in fasta format, parsing...");
				return FastaParser.parseResidues(istr);
			}
			
			Logger.getLogger(ResidueLoading.class.getName()).log(Level.FINE, "   response is not in accepted format, aborting DAS/2 residues loading");
			return null;
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(buff);
			GeneralUtils.safeClose(istr);
		}

		return null;
	}

}
