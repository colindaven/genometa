/*
 * GFF3ParserTest.java
 * JUnit based test
 *
 */
package com.affymetrix.genometryImpl.parsers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GFF3Sym;
import com.affymetrix.genometryImpl.symloader.GFF3;
import java.io.*;
import java.util.*;

/**
 * Tests of class 
 */
public class GFF3ParserTest {

	public GFF3ParserTest() {
	}

	@Before
		public void setUp() {
		}

	@After
		public void tearDown() {
		}

	/**
	 * Test of parse method using a canonical example.
	 */
	@Test
		public void testParseCanonical() throws Exception {
			//System.out.println("parse");

			String filename = "test/data/gff3/GFF3_canonical_example.gff3";
			assertTrue(new File(filename).exists());

			InputStream istr = new FileInputStream(filename);
			assertNotNull(istr);

			AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
			assertNotNull(seq_group);

			GFFParser instance = new GFFParser(); // the parser should be able to recognized
			// that this is GFF3 and create an instance of GFF3Parser to do the actual parsing.


			List expResult = null;
			List result = instance.parse(istr, seq_group, true);

			testResults(result);

			GFF3 gff3 = new GFF3(new File(filename).toURI(),new File(filename).getName(),seq_group);
			testResults(gff3.getGenome());
			
			// Replacing test with above test. hiralv 08-16-10
			
//			GFF3Sym mRNA1 = (GFF3Sym) gene.getChild(1);
//			GFF3Sym mRNA2 = (GFF3Sym) gene.getChild(2);
//			GFF3Sym mRNA3 = (GFF3Sym) gene.getChild(3);
//
//			assertEquals("EDEN.1", mRNA1.getProperty(GFF3Parser.GFF3_NAME));
//
//			assertEquals(4+1, mRNA1.getChildCount()); // 4 exons, 1 CDS
//			assertEquals(3+1, mRNA2.getChildCount()); // 3 exons, 1 CDS
//			assertEquals(4+2, mRNA3.getChildCount()); // 4 exons, 2 CDS
//
//			GFF3Sym exon1 = (GFF3Sym) mRNA1.getChild(0);
//			assertEquals(GFF3Sym.FEATURE_TYPE_EXON, exon1.getFeatureType());
//
//			GFF3Sym cds_group1 = (GFF3Sym) mRNA1.getChild(4);
//			assertEquals(GFF3Sym.FEATURE_TYPE_CDS, cds_group1.getFeatureType());
//			assertEquals(cds_group1.getSpanCount(), 4);

			istr.close();
		}

	public void testResults(List result){


			// Making result size 2, since now we are counting
			// "TF_binding_site" too. : hiralv 08-16-10
			assertEquals(2, result.size());


			SeqSymmetry gene = (SeqSymmetry) result.get(0);
			assertEquals(999, gene.getSpan(0).getStart());
			assertEquals(9000, gene.getSpan(0).getEnd());

			assertEquals(4, gene.getChildCount());
			// TODO: test child 0

			for(int i=0; i<gene.getChildCount(); i++){
				GFF3Sym mRNA = (GFF3Sym) gene.getChild(i);

				if("EDEN.1".equals(mRNA.getProperty(GFF3Parser.GFF3_NAME))){
					assertEquals(4+1, mRNA.getChildCount()); // 4 exons, 1 CDS

					GFF3Sym exon1 = (GFF3Sym) mRNA.getChild(0);
					assertEquals(GFF3Sym.FEATURE_TYPE_EXON, exon1.getFeatureType());

					GFF3Sym cds_group1 = (GFF3Sym) mRNA.getChild(4);
					assertEquals(GFF3Sym.FEATURE_TYPE_CDS, cds_group1.getFeatureType());
					assertEquals(cds_group1.getSpanCount(), 4);

				}else if ("EDEN.2".equals(mRNA.getProperty(GFF3Parser.GFF3_NAME))) {
					assertEquals(3+1, mRNA.getChildCount()); // 3 exons, 1 CDS
				}else if ("EDEN.3".equals(mRNA.getProperty(GFF3Parser.GFF3_NAME))) {
					assertEquals(4+1, mRNA.getChildCount()); // 4 exons, 1 CDS
				}
			}

	}
	@Test
		public void testParseErrors() throws IOException {
			//System.out.println("parse");

			String filename = "test/data/gff3/GFF3_with_errors.gff3";
			assertTrue(new File(filename).exists());

			InputStream istr = new FileInputStream(filename);
			assertNotNull(istr);

			AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

			GFFParser instance = new GFFParser(); // the parser should be able to recognized
			// that this is GFF3 and create an instance of GFF3Parser to do the actual parsing.


			List expResult = null;
			List result = instance.parse(istr, seq_group, true);

			// Changing result size to 2 from 1, since now we are counting
			// "TF_binding_site" too. : hiralv 08-16-10
			assertEquals(2, result.size());
		}

	/**
	 * Test of processDirective method, of class com.affymetrix.igb.parsers.GFF3Parser.
	 */
	@Test
		public void testProcessDirective() throws Exception {
			//System.out.println("processDirective");

			GFF3Parser instance = new GFF3Parser();

			instance.processDirective("##gff-version 3");

			// Setting to gff-version 2 should throw an exception
			Exception e = null;
			try {
				instance.processDirective("##gff-version 2");
			} catch (IOException ioe) {
				e = ioe;
			}
			assertNotNull(e);

		}
}
