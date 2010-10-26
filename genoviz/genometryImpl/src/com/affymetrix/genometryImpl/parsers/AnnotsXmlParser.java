package com.affymetrix.genometryImpl.parsers;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * This class is specifically for parsing the annots.xml file used by IGB and the DAS/2 server.
 */
public abstract class AnnotsXmlParser {

	/**
	 * @param istr - stream of annots file
	 */
	public static final void parseAnnotsXml(InputStream istr, List<AnnotMapElt> annotList) {
		try {
			DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
			Document doc = docBuilder.parse(istr);
			doc.getDocumentElement().normalize();

			NodeList listOfFiles = doc.getElementsByTagName("file");

			int length = listOfFiles.getLength();
			for (int s = 0; s < length && (!Thread.currentThread().isInterrupted()); s++) {
				Node fileNode = listOfFiles.item(s);
				if (fileNode.getNodeType() != Node.ELEMENT_NODE) {
					continue;
				}
				Element fileElement = (Element) fileNode;
				String filename = fileElement.getAttribute("name");
				String title = fileElement.getAttribute("title");
				String desc = fileElement.getAttribute("description");   // not currently used
				String friendlyURL = fileElement.getAttribute("url");
				String serverURL = fileElement.getAttribute("serverURL");
				String load_hint = fileElement.getAttribute("load_hint");
				String label_field = fileElement.getAttribute("label_field");
				
				if (filename != null) {
					AnnotMapElt annotMapElt = new AnnotMapElt(filename, title, desc, friendlyURL, serverURL, load_hint, label_field);
					annotList.add(annotMapElt);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static class AnnotMapElt {
		public String fileName;
		public String title;
		public String serverURL;
		public Map<String,String> props = new HashMap<String,String>();

		public AnnotMapElt(String fileName, String title) {
			this(fileName, title, "", "", "", "", "");
		}

		public AnnotMapElt(String fileName, String title, String description, String URL, String serverURL, String load_hint, String label_field) {
			// filename's case is important, since we may be loading this file locally (in QuickLoad).
			this.fileName = fileName;
			this.title = (title == null ? "" : title);
			this.serverURL = (serverURL == null ? "" : serverURL);
			this.props.put("description", description);
			this.props.put("url", URL);
			this.props.put("load_hint", load_hint);
			this.props.put("label_field", label_field);
		}

		public static AnnotMapElt findFileNameElt(String fileName, List<AnnotMapElt> annotList) {
			for (AnnotMapElt annotMapElt : annotList) {
				if (annotMapElt.fileName.equalsIgnoreCase(fileName)) {
					return annotMapElt;
				}
			}
			return null;
		}
		public static AnnotMapElt findTitleElt(String title, List<AnnotMapElt> annotList) {
			for (AnnotMapElt annotMapElt : annotList) {
				if (annotMapElt.title.equalsIgnoreCase(title)) {
					return annotMapElt;
				}
			}
			return null;
		}
	}
}
