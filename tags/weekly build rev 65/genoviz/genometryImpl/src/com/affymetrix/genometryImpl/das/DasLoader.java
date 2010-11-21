/**
 *   Copyright (c) 2001-2006 Affymetrix, Inc.
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
package com.affymetrix.genometryImpl.das;

import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.QueryBuilder;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.XMLUtils;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;



/**
 * A class to help load and parse documents from a DAS server.
 */
public abstract class DasLoader {

	private static final Pattern white_space = Pattern.compile("\\s+");

	/**
	 *  Get residues for a given region.
	 *  min and max are specified in genometry coords (interbase-0),
	 *  and since DAS is base-1, inside this method min/max get modified to
	 *  (min+1)/max before passing to DAS server
	 *
	 * @param version
	 * @param seqid
	 * @param min 
	 * @param max
	 * @return a string of residues from the DAS server or null
	 */
	public static String getDasResidues(GenericVersion version, String seqid, int min, int max) {
		Set<String> segments = ((DasSource)version.versionSourceObj).getEntryPoints();
		String segment = SynonymLookup.getDefaultLookup().findMatchingSynonym(segments, seqid);
		URI request;
		InputStream result_stream = null;
		String residues = null;

		try {
			request = URI.create(version.gServer.URL);
			URL url = new URL(request.toURL(), version.versionID + "/dna?");
			QueryBuilder builder = new QueryBuilder(url.toExternalForm());

			builder.add("segment", segment + ":" + (min + 1) + "," + max);
			request = builder.build();
			result_stream = LocalUrlCacher.getInputStream(request.toString());
			residues = parseDasResidues(new BufferedInputStream(result_stream));
		} catch (MalformedURLException ex) {
			Logger.getLogger(DasLoader.class.getName()).log(Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(DasLoader.class.getName()).log(Level.SEVERE, null, ex);
		} catch (ParserConfigurationException ex) {
			Logger.getLogger(DasLoader.class.getName()).log(Level.SEVERE, null, ex);
		} catch (SAXException ex) {
			Logger.getLogger(DasLoader.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(result_stream);
		}
		return residues;
	}

	private static String parseDasResidues(InputStream das_dna_result)
			throws IOException, SAXException, ParserConfigurationException {
		InputSource isrc = new InputSource(das_dna_result);

		Document doc = XMLUtils.nonValidatingFactory().newDocumentBuilder().parse(isrc);
		Element top_element = doc.getDocumentElement();
		NodeList top_children = top_element.getChildNodes();

		for (int i = 0; i < top_children.getLength(); i++) {
			Node top_child = top_children.item(i);
			String cname = top_child.getNodeName();
			if (cname == null || !cname.equalsIgnoreCase("sequence")) {
				continue;
			}
			NodeList seq_children = top_child.getChildNodes();
			for (int k = 0; k < seq_children.getLength(); k++) {
				Node seq_child = seq_children.item(k);
				if (seq_child == null || !seq_child.getNodeName().equalsIgnoreCase("DNA")) {
					continue;
				}
				NodeList dna_children = seq_child.getChildNodes();
				for (int m = 0; m < dna_children.getLength(); m++) {
					Node dna_child = dna_children.item(m);
					if (dna_child instanceof org.w3c.dom.Text) {
						String residues = ((Text) dna_child).getData();
						Matcher matcher = white_space.matcher("");
						residues = matcher.reset(residues).replaceAll("");
						return residues;
					}
				}
			}
		}
		return null;
	}
}
