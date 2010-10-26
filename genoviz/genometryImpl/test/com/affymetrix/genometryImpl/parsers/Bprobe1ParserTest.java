/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

import com.affymetrix.genometryImpl.EfficientProbesetSymA;

import com.affymetrix.genometryImpl.SeqSymmetry;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author auser
 */
//Bprobe1Parser parses and writes files in bp2 format
public class Bprobe1ParserTest {

	public Bprobe1ParserTest() {
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

	@Test
	//Tests the parse method
	public void testParseFromFile() throws Exception {



		String filename = "test/data/bp1/test.bp2";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("rn4");


		boolean annot_seq = true;
		String default_type = "test_type";

		boolean populate_id_hash = true;
		Bprobe1Parser parser = new Bprobe1Parser();
		List<SeqSymmetry> result = parser.parse(istr, group, annot_seq, default_type, populate_id_hash);
		istr.close();
		assertEquals(5, result.size());

		EfficientProbesetSymA sym1, sym2, sym3, sym4, sym5;

		sym1 = (EfficientProbesetSymA) result.get(0);
		assertEquals(25, sym1.getLength());
		assertEquals(187000341, sym1.getMin());
		assertEquals(187000366, sym1.getMax());
		assertEquals("RaGene-1_0-st:118032", sym1.getID());
		assertEquals(118032, sym1.getIntID());
		assertEquals(25, sym1.getProbeLength());
		assertEquals(1, sym1.getSpanCount());

		sym2 = (EfficientProbesetSymA) result.get(1);
		assertEquals(25, sym2.getLength());
		assertEquals(187000343, sym2.getMin());
		assertEquals(187000368, sym2.getMax());
		assertEquals("RaGene-1_0-st:874235", sym2.getID());

		sym3 = (EfficientProbesetSymA) result.get(2);
		assertEquals(25, sym3.getLength());
		assertEquals(187000372, sym3.getMin());
		assertEquals(187000397, sym3.getMax());
		assertEquals("RaGene-1_0-st:672767", sym3.getID());

		sym4 = (EfficientProbesetSymA) result.get(3);
		assertEquals(25, sym4.getLength());
		assertEquals(187000441, sym4.getMin());
		assertEquals(187000466, sym4.getMax());
		assertEquals("RaGene-1_0-st:964937", sym4.getID());


		sym5 = (EfficientProbesetSymA) result.get(4);
		assertEquals(25, sym5.getLength());
		assertEquals(187000456, sym5.getMin());
		assertEquals(187000481, sym5.getMax());
		assertEquals("RaGene-1_0-st:903927", sym5.getID());
		assertEquals(903927, sym5.getIntID());
		assertEquals(25, sym5.getProbeLength());
		assertEquals(1, sym5.getSpanCount());


	}
}