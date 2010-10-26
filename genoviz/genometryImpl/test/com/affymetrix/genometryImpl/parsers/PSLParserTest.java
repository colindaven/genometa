package com.affymetrix.genometryImpl.parsers;

import java.io.FileNotFoundException;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.comparator.UcscPslComparator;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.symloader.PSL;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class PSLParserTest {

	/**
	 * Test of writeAnnotations method, of class com.affymetrix.igb.parsers.PSLParser.
	 */
	@Test
	public void testWriteAnnotations() throws Exception {
		//System.out.println("writeAnnotations");

		String string =
				"70	1	0	0	0	0	2	165	+	EL049618	71	0	71	chr1	30432563	455031	455267	3	9,36,26,	0,9,45,	455031,455111,455241,\n"
				+ "71	0	0	0	0	0	2	176	+	EL049618	71	0	71	chr1	30432563	457618	457865	3	9,36,26,	0,9,45,	457618,457715,457839,\n";

		InputStream istr = new ByteArrayInputStream(string.getBytes());
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		boolean annot_query = true;
		String stream_name = "test_file";
		PSLParser instance = new PSLParser();

		List<UcscPslSym> syms = null;
		try {
			syms = instance.parse(istr, stream_name, group, group, annot_query, true);
		} catch (IOException ioe) {
			fail("Exception: " + ioe);
		}

		Collections.sort(syms, new UcscPslComparator());
		// Now we have read the data into "syms", so let's try writing it.

		BioSeq seq = group.getSeq("chr1");
		String type = "test_type";
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();

		boolean result = instance.writeAnnotations(syms, seq, type, outstream);
		assertEquals(true, result);
		assertEquals(string, outstream.toString());

		File file = createFileFromString(string);
		group = new AnnotatedSeqGroup("Test Group");
		PSL psl = new PSL(file.toURI(), stream_name, group, null, null,
				true, false, false);
		syms = psl.getGenome();
		seq = group.getSeq("chrl");
		outstream = new ByteArrayOutputStream();
		result = psl.writeAnnotations(syms, seq, type, outstream);
		assertEquals(true, result);
		assertEquals(string, outstream.toString());

	}

	/**
	 * Test of writeAnnotations method, of class com.affymetrix.genometryImpl.symloader.PSL
	 */
	public void testPslxFile() throws Exception {
		String pslxString = "117	2	0	0	1	2	1	1	+	query	121	0	121	target	120	0	120	3	57,36,26,	0,57,95,	0,58,94,		"
				+ "TTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTTT,AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA,CCCCCCCCCCCCCCCCCCCCCCCCCC,";
		String stream_name = "test_file";
		String type = "test_type";

		File file = createFileFromString(pslxString);
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		PSL psl = new PSL(file.toURI(), stream_name, group, null, null,
				true, false, false);
		List<UcscPslSym> syms = psl.getGenome();
		BioSeq seq = group.getSeq("target");
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		boolean result = psl.writeAnnotations(syms, seq, type, outstream);
		assertEquals(true, result);
		System.out.println(outstream.toString());
		assertEquals(pslxString, outstream.toString());
	}

	@Test
	public void testFiles() throws FileNotFoundException, IOException {
		File file = new File("test/data/psl/test1.psl");
		testFile(file);

		file = new File("test/data/pslx/test.pslx");
		testFile(file);
	}

	private void testFile(File file) throws FileNotFoundException, IOException {
		InputStream istr = GeneralUtils.getInputStream(file, new StringBuffer());
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		BioSeq seq = null;

		PSLParser instance = new PSLParser();
		List<UcscPslSym> syms = instance.parse(istr, file.getName(), null, group, true, true);
		Collections.sort(syms, new UcscPslComparator());
		
		PSL psl = new PSL(file.toURI(), file.getName(), group, null, null,
				true, true, false);
		List<BioSeq> seqs = psl.getChromosomeList();

		List<SeqSymmetry> syms1 = null;
		List<UcscPslSym> syms2 = null;

		for (int i = 0; i < seqs.size(); i++) {
			seq = seqs.get(i);
			syms1 = SymLoader.filterResultsByChromosome(syms, seq);
			syms2 = psl.getChromosome(seq);
			testSeqSymmetry(syms1, syms2);
		}


	}

	private void testSeqSymmetry(List<? extends SeqSymmetry> syms1, List<? extends SeqSymmetry> syms2) {
		assertEquals(syms1.size(), syms2.size());
		SeqSymmetry sym1, sym2;
		for (int i = 0; i < syms1.size(); i++) {
			sym1 = syms1.get(i);
			sym2 = syms2.get(i);

			assertEquals(sym1.getID(), sym2.getID());
			assertEquals(sym1.getChildCount(), sym2.getChildCount());
			assertEquals(sym1.getSpanCount(), sym2.getSpanCount());

			for (int j = 0; j < sym1.getSpanCount(); j++) {
				SeqSpan span1 = sym1.getSpan(j);
				SeqSpan span2 = sym2.getSpan(j);
				assertEquals(span1.getBioSeq().getID(), span1.getBioSeq().getID());
				assertEquals(span1.getMinDouble(), span2.getMinDouble(), 0);
				assertEquals(span1.getMaxDouble(), span2.getMaxDouble(), 0);
			}
		}
	}

	/**
	 * Test of getMimeType method.
	 */
	@Test
	public void testGetMimeType() {
		//System.out.println("getMimeType");

		PSLParser instance = new PSLParser();

		String result = instance.getMimeType();
		assertTrue("text/plain".equals(result));
	}

	public static File createFileFromString(String string) throws Exception {
		File tempFile = File.createTempFile("tempFile", ".psl");
		tempFile.deleteOnExit();
		BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile, true));
		bw.write(string);
		bw.close();
		return tempFile;
	}
}
