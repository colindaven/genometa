package com.affymetrix.genometryImpl.parsers;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.IndexingUtils;
import com.affymetrix.genometryImpl.util.IndexingUtils.IndexedSyms;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author jnicol
 */
public class BpsParserTest {

	/**
	 * Verify that converting to a Bps file always works the same.
	 * (This doesn't mean it's correct, just that its behavior hasn't changed.)
	 */
	@Test
	public void testConvertToBps() {
		InputStream istr = null;
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		try {
			String filename = "test/data/psl/test1.psl";
			assertTrue(new File(filename).exists());
			istr = new FileInputStream(filename);
			assertNotNull(istr);
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			boolean annot_seq = true;
			String stream_name = "test_file";

			PSLParser parser = new PSLParser();
			List<UcscPslSym> syms = parser.parse(istr, stream_name, group, group, annot_seq, true);

			BpsParser instance2 = new BpsParser();
			boolean writeResult = instance2.writeAnnotations(syms, null, "", outstream);
			assertEquals(true, writeResult);


		} catch (Exception ex) {
			Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				istr.close();
			} catch (IOException ex) {
				Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

		try {
			String filename = "test/data/bps/test1.bps";
			assertTrue(new File(filename).exists());
			istr = new FileInputStream(filename);
			assertNotNull(istr);

			BufferedInputStream bis = new BufferedInputStream(istr);
			ByteArrayOutputStream buf = new ByteArrayOutputStream();
			int result = bis.read();
			while (result != -1) {
				byte b = (byte) result;
				buf.write(b);
				result = bis.read();
			}

			assertEquals(outstream.toString(), buf.toString());
			
		} catch (Exception ex) {
			Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			try {
				istr.close();
			} catch (IOException ex) {
				Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
			}
		}

	}
	

	/**
	 * Test indexing code.
	 */
	@Test
	public void testIndexing() {
		String string =
		"70	1	0	0	0	0	2	165	+	EL049618	71	0	71	chr1	30432563	455031	455267	3	9,36,26,	0,9,45,	455031,455111,455241,\n" +
		"71	0	0	0	0	0	2	176	+	EL049618	71	0	71	chr1	30432563	457618	457865	3	9,36,26,	0,9,45,	457618,457715,457839,\n" +
		"70	1	0	0	0	0	2	165	+	EL049618	71	0	71	chr2	30432563	455031	455267	3	9,36,26,	0,9,45,	455031,455111,455241,\n" +
		"71	0	0	0	0	0	2	176	+	EL049500	71	0	71	chr1	30432563	457617	457864	3	9,36,26,	0,9,45,	457617,457714,457838,\n"
				;


		InputStream istr = new ByteArrayInputStream(string.getBytes());
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;

		List<UcscPslSym> syms = null;
		try {
			PSLParser parser = new PSLParser();
			syms = parser.parse(istr, "stream_test", group, group, annot_seq, true);
		} catch (IOException ex) {
			Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
		}

		assertEquals(4, syms.size());	// precisely 4 symmetries.

		BioSeq seq = group.getSeq("chr1");

		BpsParser bps = new BpsParser();
			Comparator<UcscPslSym> USCCCompare = bps.getComparator(seq);
		List<SeqSymmetry> sortedSyms = IndexingUtils.getSortedAnnotationsForChrom(
				syms, seq, USCCCompare);

		assertEquals(3,sortedSyms.size());	// precisely 3 symmetries on chr1.

		assertEquals(457617, ((UcscPslSym)sortedSyms.get(1)).getTargetMin());	// the middle symmetry (after sorting) should have a start coord of 457617.
		
	}


	/**
	 * Test indexing code.
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void testIndexing2() {

		try {
			String filename = "test/data/bps/test1.bps";
			String testFileName = "test/data/bps/testNEW.bps";
			assertTrue(new File(filename).exists());
			
			AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
			
			List<UcscPslSym> syms = null;
			syms = bpsParse(filename, "stream_test", group);

			BioSeq seq = group.getSeq("chr1");
		
			IndexWriter iWriter = new BpsParser();
			List<SeqSymmetry> sortedSyms = IndexingUtils.getSortedAnnotationsForChrom(
					syms, seq, iWriter.getComparator(seq));

			assertEquals(15,sortedSyms.size());

			File testFile = new File(testFileName);
			IndexedSyms iSyms = new IndexedSyms(sortedSyms.size(), testFile, "test", iWriter);
			IndexingUtils.writeIndexedAnnotations(sortedSyms, seq, group, iSyms, testFileName);

			assertEquals(iSyms.min.length, iSyms.max.length);
			assertEquals(iSyms.min.length + 1, iSyms.filePos.length);
			assertEquals(sortedSyms.size(), iSyms.min.length);

			testOutputIndexedSymmetries(iSyms.min,iSyms.max,iSyms.filePos);

			if (testFile.exists()) {
				testFile.delete();
			}
		} catch (Exception ex) {
			Logger.getLogger(BpsParserTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}
	}


	public static List<UcscPslSym> bpsParse(String file_name, String annot_type, AnnotatedSeqGroup seq_group)
			throws IOException {
		Logger.getLogger(BpsParser.class.getName()).log(
							Level.INFO, "loading file: {0}", file_name);
		List<UcscPslSym> results = null;
		FileInputStream fis = null;
		DataInputStream dis = null;
		BufferedInputStream bis = null;
		try {
			File fil = new File(file_name);
			long flength = fil.length();
			fis = new FileInputStream(fil);
			bis = new BufferedInputStream(fis);

			byte[] bytebuf = new byte[(int) flength];
			bis.read(bytebuf);

			ByteArrayInputStream bytestream = new ByteArrayInputStream(bytebuf);
			dis = new DataInputStream(bytestream);
			results = BpsParser.parse(dis, annot_type, (AnnotatedSeqGroup) null, seq_group, false, true);
		} finally {
			GeneralUtils.safeClose(bis);
			GeneralUtils.safeClose(dis);
			GeneralUtils.safeClose(fis);
		}
		return results;
	}


	private void testOutputIndexedSymmetries(int [] min, int [] max, long [] pos){
		int [] overlapRange = new int[2];
		int [] outputRange = new int[2];
		overlapRange[0] = 2455539;
		overlapRange[1] = 2455600;
		IndexingUtils.findMaxOverlap(overlapRange, outputRange, min, max);
		
		int minPos = outputRange[0];
		//System.out.println("val: " + min[minPos] + " " + max[minPos] + " " + pos[minPos]);

		minPos = outputRange[1];
		//System.out.println("position: " + minPos);
	}
	


	/**
	 * Test of getMimeType method.
	 */
	@Test
	public void testGetMimeType() {
		BpsParser instance = new BpsParser();

		String result = instance.getMimeType();
		assertTrue("binary/bps".equals(result));
	}
}
