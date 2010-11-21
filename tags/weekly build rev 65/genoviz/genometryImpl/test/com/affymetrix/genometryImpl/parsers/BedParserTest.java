package com.affymetrix.genometryImpl.parsers;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.*;
import com.affymetrix.genometryImpl.symloader.BED;
import com.affymetrix.genometryImpl.util.ParserController;
import java.io.*;
import java.util.*;

public class BedParserTest {
	static GenometryModel gmodel = GenometryModel.getGenometryModel();

	public BedParserTest() {
	}

	@Before
		public void setUp() {
		}

	@After
		public void tearDown() {
		}

	@Test
		public void testParseFromFile() throws IOException {

			String filename = "test/data/bed/bed_01.bed";
			assertTrue(new File(filename).exists());

			InputStream istr = new FileInputStream(filename);
			DataInputStream dis = new DataInputStream(istr);
			assertNotNull(dis);

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");

			IndexWriter parser = ParserController.getIndexWriter(filename);
			assertNotNull(parser);
			List result = parser.parse(dis, filename, group);

			testFileResult(result);

			BED bed = new BED(new File(filename).toURI(), filename, group);
			result = bed.getGenome();
			testFileResult(result);
		}

	public void testFileResult(List result){
		assertEquals(6, result.size());

		UcscBedSym sym = (UcscBedSym) result.get(2);
		assertEquals(1, sym.getSpanCount());
		SeqSpan span = sym.getSpan(0);
		assertEquals(1790361, span.getMax());
		assertEquals(1789140, span.getMin());
		assertEquals(false, span.isForward());
		assertEquals(false, sym.hasCdsSpan());
		assertEquals(null, sym.getCdsSpan());
		assertEquals(2, sym.getChildCount());

		sym = (UcscBedSym) result.get(5);
		assertEquals(sym.hasCdsSpan(), true);
		SeqSpan cds = sym.getCdsSpan();
		assertEquals(1965425, cds.getMin());
		assertEquals(1965460, cds.getMax());
		assertEquals(new Float(0), ((Float) sym.getProperty("score")));
	}

	/**
	 * Test of parse method, of class com.affymetrix.igb.parsers.BedParser.
	 */
	@Test
		public void testParseFromString() throws Exception {

			String string = 
				"591	chr2L	901490	901662	CR31656-RA	0	-	901490	901662	0	1	172,	0,\n"+
				"595	chr2L	1432710	1432921	CR31927-RA	0	+	1432710	1432920	0	1	211,	0,\n"+

				// Next line is line "2": we'll specifically test that it was read correctly
				"598	chr2L	1789140	1790361	CR31930-RA	0	-	1789140	1789140	0	2	153,1010,	0,211,\n"+
				"598	chr2L	1792056	1793268	CR31931-RA	0	-	1792056	1792056	0	2	153,1014,	0,198,\n"+
				"599	chr2L	1938088	1938159	CR31667-RA	0	-	1938088	1938088	0	1	71,	0,\n"+

				// This last line has a CDS: we'll test that it was read correctly as well
				"599	chr2L	1965425	1965498	CR31942-RA	0	+	1965425	1965460	0	1	73,	0,\n"
				;

			InputStream istr = new ByteArrayInputStream(string.getBytes());
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			boolean annot_seq = true;
			String stream_name = "test_file";
			boolean create_container = true;
			BedParser instance = new BedParser();

			List<SeqSymmetry> result = instance.parse(istr, gmodel, group, annot_seq, stream_name, create_container);

			testStringResult(result);

			
			File tempFile = createFileFromString(string);
			
			BED bed = new BED(tempFile.toURI(), tempFile.getName(), group);
			result = bed.getGenome();
			testStringResult(result);
		}

	public void testStringResult(List result) {
		assertEquals(6, result.size());

		UcscBedSym sym = (UcscBedSym) result.get(2);
		assertEquals(1, sym.getSpanCount());
		SeqSpan span = sym.getSpan(0);
		assertEquals(1790361, span.getMax());
		assertEquals(1789140, span.getMin());
		assertEquals(false, span.isForward());
		assertEquals(false, sym.hasCdsSpan());
		assertEquals(null, sym.getCdsSpan());
		assertEquals(2, sym.getChildCount());

		sym = (UcscBedSym) result.get(5);
		assertEquals(sym.hasCdsSpan(), true);
		SeqSpan cds = sym.getCdsSpan();
		assertEquals(1965425, cds.getMin());
		assertEquals(1965460, cds.getMax());
		assertEquals(new Float(0), ((Float) sym.getProperty("score")));
	}
	
	/**
	 * Test of parseIntArray method, of class com.affymetrix.igb.parsers.BedParser.
	 */
	@Test
		public void testParseIntArray() {

			String int_array = "1,7,8,9,10";

			int[] expResult = new int[] {1,7,8,9,10};
			int[] result = BedParser.parseIntArray(int_array);
			for (int i=0; i<expResult.length; i++) {
				assertEquals(expResult[i], result[i]);
			}

			result = BED.parseIntArray(int_array);
			for (int i=0; i<expResult.length; i++) {
				assertEquals(expResult[i], result[i]);
			}
		}

	@Test
		public void testParseIntArrayWithWhitespace() {

			// the parser doesn't accept whitespace in the integer lists
			// (Maybe it should, but it isn't expected to need to do so.)
			String int_array = "1,7, 8,9,10";

			boolean passed = false;
			try {
				BedParser.parseIntArray(int_array);
			} catch (NumberFormatException nfe) {
				passed = true;
			}
			if (!passed) {
				fail("Expected exception was not thrown");
			}

			passed = false;
			try {
				BED.parseIntArray(int_array);
			} catch (NumberFormatException nfe) {
				passed = true;
			}
			if (!passed) {
				fail("Expected exception was not thrown");
			}
		}


	/**
	 * Test of makeBlockMins method, of class com.affymetrix.igb.parsers.BedParser.
	 */
	@Test
		public void testMakeBlockMins() {

			int min = 100;
			int[] blockStarts = new int[] {1,3,4,5,9};

			int[] expResult = new int[] {101,103,104,105,109};
			int[] result = BedParser.makeBlockMins(min, blockStarts);
			for (int i=0; i<expResult.length; i++) {
				assertEquals(expResult[i], result[i]);
			}

			result = BED.makeBlockMins(min, blockStarts);
			for (int i=0; i<expResult.length; i++) {
				assertEquals(expResult[i], result[i]);
			}
		}

	/**
	 * Test of makeBlockMaxs method, of class com.affymetrix.igb.parsers.BedParser.
	 */
	@Test
		public void testMakeBlockMaxs() {

			int[] blockMins =  new int[] {1,3,4,5,9};
			int[] blockSizes =  new int[] {1,3,4,5,9};

			int[] expResult =  new int[] {2,6,8,10,18};
			int[] result = BedParser.makeBlockMaxs(blockMins, blockSizes);
			for (int i=0; i<expResult.length; i++) {
				assertEquals(expResult[i], result[i]);
			}

			result = BED.makeBlockMaxs(blockMins, blockSizes);
			for (int i=0; i<expResult.length; i++) {
				assertEquals(expResult[i], result[i]);
			}
		}

	/**
	 * Test of writeSymmetry method, of class com.affymetrix.igb.parsers.BedParser.
	 */
	@Test
		public void testWriteBedFormat() throws Exception {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(baos);

			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			BioSeq seq = group.addSeq("chr12", 500000);
			SeqSpan span = new SimpleSeqSpan(500,800,seq);
			SeqSpan[] span_array = new SeqSpan[] {span};
			SimpleMutableSeqSymmetry sym = new SimpleMutableSeqSymmetry();
			for (SeqSpan span_in_array : span_array) {
				sym.addSpan(span_in_array);
			}

			BedParser.writeSymmetry(dos, sym, seq);
			assertEquals("chr12\t500\t800\n", baos.toString());

			baos = new ByteArrayOutputStream();
			dos = new DataOutputStream(baos);
			
			BED.writeSymmetry(dos, sym, seq);
			assertEquals("chr12\t500\t800\n", baos.toString());
		}

	/**
	 * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.BedParser.
	 */
	@Test
		public void testWriteAnnotations() throws Exception {

			String string = 
				"chr2L	901490	901662	CR31656-RA	0	-	901490	901662	0	1	172,	0,\n"+
				"chr2L	1432710	1432921	CR31927-RA	0	+	1432710	1432920	0	1	211,	0,\n"+
				"chr2L	1789140	1790361	CR31930-RA	0	-	1789140	1789140	0	2	153,1010,	0,211,\n"+
				"chr2L	1792056	1793268	CR31931-RA	0	-	1792056	1792056	0	2	153,1014,	0,198,\n"+
				"chr2L	1938088	1938159	CR31667-RA	0	-	1938088	1938088	0	1	71,	0,\n"+
				"chr2L	1965425	1965498	CR31942-RA	0	+	1965425	1965460	0	1	73,	0,\n"
				;

			InputStream istr = new ByteArrayInputStream(string.getBytes());
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			boolean annot_seq = true;
			String stream_name = "test_file";
			boolean create_container = true;
			BedParser instance = new BedParser();

			Collection<SeqSymmetry> syms = null;
			try {
				syms = instance.parse(istr,gmodel,group,annot_seq,stream_name,create_container);
			} catch (IOException ioe) {
				fail("Exception: " + ioe);
			}

			BioSeq seq = group.getSeq("chr2L");
			
			testWrite(syms,seq,string);

			File file = createFileFromString(string);
			
			BED bed = new BED(file.toURI(), file.getName(), group);
			syms = bed.getGenome();

			testWrite(syms,seq,string);
		}

	/**
	 * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.BedParser.
	 * Validate that if the genes have the same name, then loading and writing them out
	 * gives the same information.
	 */
	@Test
		public void testWriteAnnotations2() throws Exception {

			String string =
				"chr1	455031	455267	EL049618	0	+	455031	455267	0	3	9,36,26,	0,80,210,\n"+
        "chr1	457618	457865	EL049618	0	+	457618	457865	0	3	9,36,26,	0,97,221,\n"
				;

			InputStream istr = new ByteArrayInputStream(string.getBytes());
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			boolean annot_seq = true;
			String stream_name = "test_file";
			boolean create_container = true;
			BedParser instance = new BedParser();

			Collection<SeqSymmetry> syms = null;
			try {
				syms = instance.parse(istr,gmodel,group,annot_seq,stream_name,create_container);
			} catch (IOException ioe) {
				fail("Exception: " + ioe);
			}

			// Now we have read the data into "syms", so let's try writing it.

			BioSeq seq = group.getSeq("chr1");

			testWrite(syms,seq,string);

			File file = createFileFromString(string);

			BED bed = new BED(file.toURI(), file.getName(), group);
			syms = bed.getGenome();

			testWrite(syms,seq,string);
		}

	public void testWrite(Collection<SeqSymmetry> syms, BioSeq seq, String expResult){
		// Now we have read the data into "syms", so let's try writing it.
			BedParser instance = new BedParser();

			String type = "test_type";
			ByteArrayOutputStream outstream = new ByteArrayOutputStream();

			boolean result = instance.writeAnnotations(syms, seq, type, outstream);
			assertEquals(true, result);
			assertEquals(expResult, outstream.toString());
	}

	/**
	 * Test of getMimeType method, of class com.affymetrix.igb.parsers.BedParser.
	 */
	@Test
		public void testGetMimeType() {

			BedParser instance = new BedParser();

			String result = instance.getMimeType();
			assertTrue("text/plain".equals(result) || "text/bed".equals(result));
		}

	public File createFileFromString(String string) throws Exception{
		File tempFile = File.createTempFile("tempFile", ".bed");
		tempFile.deleteOnExit();
		BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile, true));
		bw.write(string);
		bw.close();
		return tempFile;
	}

	@Test
	public void testBEDParseFromFile() throws IOException {
		String filename = "test/data/bed/bed_02.bed";
		assertTrue(new File(filename).exists());
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		BioSeq seq = group.addSeq("chr2L", 1965498);

		BED bed = new BED(new File(filename).toURI(), filename, group);

		List<BioSeq> allSeq = bed.getChromosomeList();
		assertEquals(4, allSeq.size());

		List result = bed.getChromosome(seq);
		assertEquals(6, result.size());

		UcscBedSym sym = (UcscBedSym) result.get(2);
		assertEquals(1, sym.getSpanCount());
		SeqSpan span = sym.getSpan(0);
		assertEquals(1790361, span.getMax());
		assertEquals(1789140, span.getMin());
		assertEquals(false, span.isForward());
		assertEquals(false, sym.hasCdsSpan());
		assertEquals(null, sym.getCdsSpan());
		assertEquals(2, sym.getChildCount());

		sym = (UcscBedSym) result.get(5);
		assertEquals(sym.hasCdsSpan(), true);
		SeqSpan cds = sym.getCdsSpan();
		assertEquals(1965425, cds.getMin());
		assertEquals(1965460, cds.getMax());
		assertEquals(new Float(0), ((Float) sym.getProperty("score")));
	}

}
