/*
 * Das1FeatureSaxParserTest.java
 * JUnit based test
 *
 * Created on October 6, 2006, 3:19 PM
 */
package com.affymetrix.genometryImpl.parsers.das;

import com.affymetrix.genometryImpl.SeqSymmetry;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.util.*;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Ed Erwin
 * @version $Id: DASFeatureParserTest.java 5979 2010-05-21 16:42:02Z jnicol $
 */
public class DASFeatureParserTest {

	public static final String test_file_name_1 = "test/data/das1/das1-sample-hg18.dasxml";
	public static final String test_file_name_2 = "test/data/das1/das1-sample-hg10.dasxml";

	public DASFeatureParserTest() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	/**
	 * Tests the parsing of the <LINK> elements
	 *
	 * @throws FileNotFoundException
	 * @throws XMLStreamException
	 */
	@Test
	public void testLinks() throws FileNotFoundException, XMLStreamException {
		InputStream istr = null;
		assertTrue(new File(test_file_name_1).exists());

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		DASFeatureParser parser = new DASFeatureParser();
		parser.setAnnotateSeq(false);

		Collection<DASSymmetry> results = null;
		try {
			istr = new FileInputStream(test_file_name_1);
			assertNotNull(istr);

			results = parser.parse(istr, group);
		} finally {
			GeneralUtils.safeClose(istr);
		}

		assertEquals(32, results.size());

		SeqSymmetry newSym = null;
		for (SeqSymmetry sym: results) {
			if (sym.getID().equals("235371_at.chr3.73089142")) {
				newSym = sym;
			}
		}
		assertNotNull(newSym);
		assertTrue("Result is not a DASSymmetry", newSym instanceof DASSymmetry);
		DASSymmetry sym = (DASSymmetry) newSym;

		String link = (String) sym.getProperty("link");
		String linkName = (String) sym.getProperty("link_name");
		assertEquals("http://genome.ucsc.edu/cgi-bin/hgTracks?position=chr3:73089142-73107313&db=hg18", link);
		assertEquals("Link to UCSC Browser", linkName);
		assertEquals("affyU133Plus2", sym.getType());
		assertEquals("235371_at.chr3.73089142", sym.getProperty("id"));
		assertEquals(4, sym.getChildCount());
	}

	/**
	 * Tests the parsing of a simple example file
	 *
	 * @throws FileNotFoundException 
	 * @throws XMLStreamException
	 */
	@Test
	public void testParse() throws FileNotFoundException, XMLStreamException {
		InputStream istr = null;
		assertTrue(new File(test_file_name_2).exists());

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		DASFeatureParser parser = new DASFeatureParser();
		parser.setAnnotateSeq(false);

		Collection<DASSymmetry> results = null;
		try {
			istr = new FileInputStream(test_file_name_2);
			assertNotNull(istr);

			results = parser.parse(istr, group);
		} finally {
			GeneralUtils.safeClose(istr);
		}

		assertEquals(1, results.size());

		DASSymmetry sym = results.iterator().next();

		assertEquals(1, sym.getSpanCount());
		assertEquals(46, sym.getSpan(0).getLength());
		assertEquals("Em:D87024.C22.12.chr22.20012405", sym.getID());
		assertEquals("sanger22_type", sym.getType());

		assertEquals("Em:D87024.C22.12.chr22.20012405", sym.getProperty("label"));

		Object oLink     = sym.getProperty("link");
		Object oLinkName = sym.getProperty("link_name");
		String link = null;
		String linkName = null;

		if (oLink instanceof String) {
			link = (String) oLink;
		} else {
			fail("Link was not a string");
		}
		if (oLinkName instanceof String) {
			linkName = (String) oLinkName;
		} else {
			fail("Link name was not a string");
		}

		assertEquals("http://genome.ucsc.edu/cgi-bin/hgTracks?position=chr22:20012405-20012900&db=hg10", link);
		assertEquals("Link to UCSC Browser", linkName);
	}
}
