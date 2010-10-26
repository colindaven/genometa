package org.bioviz.protannot;

import com.affymetrix.genometryImpl.util.DNAUtils;
import java.io.IOException;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import java.io.BufferedInputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Internal to the application, convert to a "positive strand" format.
 * @author jnicol
 */
final class NormalizeXmlStrand {
	private boolean isNegativeStrand = false;
	private boolean isStrandSet = false;
	Document doc = null;
	private static final String JAXP_SCHEMA_SOURCE = "http://java.sun.com/xml/jaxp/properties/schemaSource";
	private static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	private static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	private static final String schemaSource = "protannot.xsd";
	 /**
     *Initialize dbFactory and dBuilder
     */
   NormalizeXmlStrand(BufferedInputStream bistr) throws ParserConfigurationException, SAXException, IOException {
		URL url = ProtAnnotMain.class.getResource(schemaSource);
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		dbFactory.setNamespaceAware(true);
		dbFactory.setValidating(true);
		dbFactory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA); // use LANGUAGE here instead of SOURCE
		dbFactory.setAttribute(JAXP_SCHEMA_SOURCE, url.toExternalForm());

		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		dBuilder.setErrorHandler(new SimpleErrorHandler());

		Document seqdoc = dBuilder.parse(bistr);
		doc = processDocument(seqdoc);
	}

	static void outputXMLToScreen(Document doc) {
		Transformer transformer;
		try {
			transformer = TransformerFactory.newInstance().newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			//initialize StreamResult with File object to save to file
			StreamResult result = new StreamResult(new StringWriter());
			DOMSource source = new DOMSource(doc);
			transformer.transform(source, result);
			String xmlString = result.getWriter().toString();
			System.out.println(xmlString);
		} catch (Exception ex) {
			Logger.getLogger(NormalizeXmlStrand.class.getName()).log(Level.SEVERE, null, ex);
		}

	}
	
	/**
     * Transforms sequence coordinates.
	 * Normalizes all coordinates respective to the sequence's start coordinates.
	 * If strand is negative, flips all coordinates and reverse-complements the sequence.

     * @param   seqdoc  Document object name
     * @return          Returns BioSeq of given document object.
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private Document processDocument(Document seqdoc) {
		Element top_element = seqdoc.getDocumentElement();
		NodeList children = top_element.getChildNodes();
		if (!top_element.getTagName().equalsIgnoreCase("dnaseq")) {
			return null;
		}

		// get residues and normalize their attributes
		int residuesStart = 0;
		String residues = "";
		Node residuesChildNode = null;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String name = child.getNodeName();
			if (name == null || !name.equalsIgnoreCase(Xml2GenometryParser.RESIDUESSTR)) {
				continue;
			}
			Element residuesNode = (Element) child;
			residuesChildNode = residuesNode.getFirstChild();
			Text resnode = (Text) residuesChildNode;
			residues = resnode.getData();
			try {
				residuesStart = Integer.parseInt((residuesNode).getAttribute(Xml2GenometryParser.STARTSTR));
				residuesNode.setAttribute(Xml2GenometryParser.STARTSTR, Integer.toString(0)); // normalize start of residues to 0

				try {
					int residuesEnd = Integer.parseInt((residuesNode).getAttribute(Xml2GenometryParser.ENDSTR));
					residuesNode.setAttribute(Xml2GenometryParser.ENDSTR, Integer.toString(residuesEnd - residuesStart));
					// normalize end of residues, if end exists

				} catch (Exception ex) {
					// Ignore exceptions here, since residue end may not be defined.
					Logger.getLogger(NormalizeXmlStrand.class.getName()).log(Level.WARNING, "Residue end not defined", ex);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		normalizemRNA(children, residuesStart, residues, residuesChildNode);

		return seqdoc;

    }


	private void normalizemRNA(NodeList children, int residuesStart, String residues, Node residuesChildNode)
			throws DOMException, NumberFormatException {
		// Get strand of mRNA.  Normalize attributes
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String name = child.getNodeName();
			if (name == null || !name.equalsIgnoreCase(Xml2GenometryParser.MRNASTR)) {
				continue;
			}
			Element childElem = (Element) child;
			int start = Integer.parseInt(childElem.getAttribute(Xml2GenometryParser.STARTSTR));
			int end = Integer.parseInt(childElem.getAttribute(Xml2GenometryParser.ENDSTR));
			start = start - residuesStart;
			end = end - residuesStart;

			try {
				String strand = childElem.getAttribute(Xml2GenometryParser.STRANDSTR);
				isNegativeStrand = "-".equals(strand);
				if (isNegativeStrand) {
					int newEnd = residues.length() - start;
					start = residues.length() - end;
					end = newEnd;
					if (!isStrandSet) {
						residues = DNAUtils.reverseComplement(residues);
						residuesChildNode.setNodeValue(residues);
						isStrandSet = true;
					}
					childElem.setAttribute(Xml2GenometryParser.STRANDSTR, "+"); // Normalizing to positive strand
				}
			} catch (Exception e) {
				System.out.println("No strand attribute found");
			}
			childElem.setAttribute(Xml2GenometryParser.STARTSTR, Integer.toString(start));
			childElem.setAttribute(Xml2GenometryParser.ENDSTR, Integer.toString(end));

			normalizeNodes(Xml2GenometryParser.EXONSTR,childElem.getChildNodes(), residuesStart, residues);
			normalizeNodes(Xml2GenometryParser.CDSSTR,childElem.getChildNodes(), residuesStart, residues);
		}
	}

	private void normalizeNodes(String nodeName, NodeList children, int residuesStart, String residues)
			throws DOMException, NumberFormatException {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String name = child.getNodeName();
			if (name == null || !name.equalsIgnoreCase(nodeName)) {
				continue;
			}
			Element childElem = (Element) child;
			int start = Integer.parseInt(childElem.getAttribute(Xml2GenometryParser.STARTSTR));
			int end = Integer.parseInt(childElem.getAttribute(Xml2GenometryParser.ENDSTR));
			start = start - residuesStart;
			end = end - residuesStart;
			if (isNegativeStrand) {
				int newEnd = residues.length() - start;
				start = residues.length() - end;
				end = newEnd;
			}
			childElem.setAttribute(Xml2GenometryParser.STARTSTR, Integer.toString(start));
			childElem.setAttribute(Xml2GenometryParser.ENDSTR, Integer.toString(end));
		}
	}

	private static class SimpleErrorHandler implements ErrorHandler {
    public void warning(SAXParseException e) throws SAXException {
        System.out.println("Line " + e.getLineNumber() + ": " + e.getMessage());
    }

    public void error(SAXParseException e) throws SAXException {
        System.err.println("Line " + e.getLineNumber() + ": " + e.getMessage());
    }

    public void fatalError(SAXParseException e) throws SAXException {
        System.err.println("Line " + e.getLineNumber() + ": " + e.getMessage());
    }
}


}
