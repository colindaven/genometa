package com.affymetrix.genometryImpl.util;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author sgblanch
 * @version $Id: XMLUtils.java 5674 2010-04-12 19:14:03Z jnicol $
 */
public class XMLUtils {

	private XMLUtils() { }

	/**
	 * Create a new DocumentBuilder factory with validation disabled.
	 * The parser returned is not specifically set-up for DAS, and can be
	 * used in any case where you want a non-validating parser.
	 *
	 * @return a non-validating DocumentBuilderFactory
	 */
	public static DocumentBuilderFactory nonValidatingFactory() {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setValidating(false);
		setFeature(factory,"http://xml.org/sax/features/validation");
		setFeature(factory,"http://apache.org/xml/features/validation/dynamic");
		setFeature(factory,"http://apache.org/xml/features/nonvalidating/load-external-dtd");
		return factory;
	}

	private static void setFeature(DocumentBuilderFactory factory, String feature) {
		try {
			factory.setFeature(feature, false);
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Opens an XML document, using {@link #nonValidatingFactory()}.
	 *
	 * @param url
	 * @return the opened XML doc
	 * @throws ParserConfigurationException
	 * @throws MalformedURLException
	 * @throws SAXException
	 * @throws IOException 
	 */
	public static Document getDocument(String url) throws ParserConfigurationException, MalformedURLException, SAXException, IOException {
		Document doc = null;
		URL request_url = new URL(url);
		URLConnection request_con = request_url.openConnection();
		
		request_con.setConnectTimeout(LocalUrlCacher.CONNECT_TIMEOUT);
		request_con.setReadTimeout(LocalUrlCacher.READ_TIMEOUT);
		doc = getDocument(request_con);

		return doc;
	}

	/**
	 * Opens an XML document, using {@link #nonValidatingFactory()}.
	 *
	 * @param request_con
	 * @return doc
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Document getDocument(URLConnection request_con) throws ParserConfigurationException, SAXException, IOException {
		InputStream result_stream = null;
		Document doc = null;

		try {
		  result_stream = new BufferedInputStream(request_con.getInputStream());
		  doc = getDocument(result_stream);
		} finally {
			GeneralUtils.safeClose(result_stream);
		}

		return doc;
	}

	/**
	 * Opens an XML document, using {@link #nonValidatingFactory()}.
	 *
	 * @param str
	 * @return Document
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException 
	 */
	public static Document getDocument(InputStream str) throws ParserConfigurationException, SAXException, IOException {
		return nonValidatingFactory().newDocumentBuilder().parse(str);
	}
}
