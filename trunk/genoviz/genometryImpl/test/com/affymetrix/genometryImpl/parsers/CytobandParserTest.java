/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.genometryImpl.parsers;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.parsers.CytobandParser.Arm;
import com.affymetrix.genometryImpl.parsers.CytobandParser.CytobandSym;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
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
public class CytobandParserTest {

	public CytobandParserTest() {
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
	 * Test of parse method, of class CytobandParser.
	 */
	@Test
	public void testParse() throws Exception {
		String filename = "test/data/cyt/test1.cyt";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);

		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;

		CytobandParser instance = new CytobandParser();
		List<SeqSymmetry> result = instance.parse(istr, seq_group, annot_seq);
		assertEquals(7, result.size());
		CytobandSym sym = (CytobandSym) result.get(2);
		assertEquals("gpos25", sym.getBand());
		assertEquals(4300000, sym.getLength());
		assertEquals(39600000, sym.getMin());
		assertEquals(43900000, sym.getMax());
		assertEquals(0, sym.getChildCount());
		assertEquals(43900000, sym.getEnd());
		assertEquals(Arm.SHORT, sym.getArm());



	}

	/**
	 * Test of writeAnnotations method, of class CytobandParser.
	 */
	@Test
	public void testWriteAnnotations() throws FileNotFoundException, IOException {

		String string = "chr1\t39600000\t43900000\tp34.2\tgpos25\n" +
						"chr1\t43900000\t46500000\tp34.1\tgneg\n" +
						"chr1\t56200000\t58700000\tp32.2\tgpos50\n";


		InputStream istr = new ByteArrayInputStream(string.getBytes());
		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;

		CytobandParser instance = new CytobandParser();

		Collection<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();

		syms = instance.parse(istr, seq_group, annot_seq);


		BioSeq seq = seq_group.getSeq("chr1");
		String type = "test_type";
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();


		boolean result = instance.writeAnnotations(syms, seq, type, outstream);
		assertEquals(true, result);
		assertEquals(string, outstream.toString());



	}

	/**
	 * Test of writeCytobandFormat method, of class CytobandParser.
	 */
	@Test
	public void testWriteCytobandFormat() throws Exception {
		Writer out = new StringWriter();
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		String filename = "test/data/cyt/test1.cyt";
		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);
		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;
		CytobandParser instance = new CytobandParser();
		List<SeqSymmetry> result = instance.parse(istr, seq_group, annot_seq);
		BioSeq aseq = seq_group.getSeq(0);
		CytobandSym sym = (CytobandSym) result.get(2);
		CytobandParser.writeCytobandFormat(out, sym, aseq);
		assertEquals("chr1\t39600000\t43900000\tp34.2\tgpos25\n", out.toString());

	}

	/**
	 * Test of getMimeType method, of class CytobandParser.
	 */
	@Test
	public void testGetMimeType() {

		CytobandParser instance = new CytobandParser();
		assertEquals("txt/plain", instance.getMimeType());
	}
}