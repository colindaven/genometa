/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometryImpl.parsers;


//import com.affymetrix.genometry.BioSeq;
//import com.affymetrix.genometry.SeqSpan;
//import com.affymetrix.genometry.SeqSymmetry;
//import com.affymetrix.genometry.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 *
 * @author auser
 */
public class Das2FeatureSaxParserTest {
	public static final String test_file_name_1 = "test/data/das2/test2.das2xml";
    public Das2FeatureSaxParserTest() {
    }

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }
		/**
	 * Tests the parsing of the <LINK> elements
	 */

	@Test
		public void testLinks() throws FileNotFoundException, SAXException {
			assertTrue(new File(test_file_name_1).exists());
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			boolean annot_seq =true;
			Das2FeatureSaxParser ins=new Das2FeatureSaxParser();
			String uri=Das2FeatureSaxParser.TYPEURI;

            List<SeqSymmetry> results = null;
			try {
				InputStream istr = new FileInputStream(test_file_name_1);
				assertNotNull(istr);
				InputSource isrc = new InputSource(istr);
				assertNotNull(isrc);
			    results = ins.parse(isrc,uri,group,annot_seq);
			    assertNotNull(results);
			} catch (IOException ioe) {
				ioe.printStackTrace();
				fail("Failed due to Exception: " + ioe.toString());
			}
	         
	}


		
		@Test
		public void testgetRangeString() throws Exception {
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			BioSeq seq = group.addSeq("chr1", 30432563);
			SeqSpan span = new SimpleSeqSpan(500,800,seq);
			boolean indicate_strand = true;
			String results =Das2FeatureSaxParser.getRangeString(span, indicate_strand);
			assertEquals("500:800:1",results);

		
		}

}
