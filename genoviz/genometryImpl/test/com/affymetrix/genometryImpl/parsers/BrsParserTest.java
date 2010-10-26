package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.util.IndexingUtils;
import com.affymetrix.genometryImpl.util.IndexingUtils.IndexedSyms;
import com.affymetrix.genometryImpl.util.ServerUtils;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
/**
 *
 * @author jnicol
 */
public class BrsParserTest {
	private static GenometryModel gmodel = GenometryModel.getGenometryModel();
	String filename = "test/data/brs/refseq.brs";
	String versionString = "genomeVersion";
	AnnotatedSeqGroup genome = null;
	private List<SeqSymmetry> results = null;
	private BrsParser parser = new BrsParser();

	@Before
	public void setUp() {
		FileInputStream istr = null;
		try {
			istr = new FileInputStream(filename);
			BufferedInputStream bis = new BufferedInputStream(istr);
			DataInputStream dis = new DataInputStream(bis);
			genome = new AnnotatedSeqGroup("testGenome");
			results = parser.parse(dis, filename, genome);
		} catch (Exception ex) {
			Logger.getLogger(BrsParserTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		} finally {
			try {
				istr.close();
			} catch (IOException ex) {
				Logger.getLogger(BrsParserTest.class.getName()).log(Level.SEVERE, null, ex);
				fail();
			}
		}
	}

	@Test
	public void TestParseBrs() throws Exception {
		assertTrue(results != null);
		assertEquals(944, results.size());
	}


	/*@Test
	public void TestIndexing() {
		try {
			String testFileName = "test/data/brs/testOUT.brs";
			String seqid = "chr1";
			BioSeq seq = genome.getSeq(seqid);
			assertTrue(seq != null);
			assertEquals(267693137, seq.getLength());
			List<SeqSymmetry> sortedSyms = IndexingUtils.getSortedAnnotationsForChrom(
					results, seq, parser.getComparator(seq));
			assertEquals(726, sortedSyms.size());
			SeqSymmetry firstSym = sortedSyms.get(0);
			assertEquals(2150626, firstSym.getSpan(seq).getMin());
			assertEquals(2155593, firstSym.getSpan(seq).getMax());
			SeqSymmetry lastSym = sortedSyms.get(sortedSyms.size() - 1);
			assertEquals(267495490, lastSym.getSpan(seq).getMin());
			assertEquals(267693137, lastSym.getSpan(seq).getMax());
			FileOutputStream fos = null;
			fos = new FileOutputStream(testFileName);
			File testFile = new File(testFileName);
			IndexedSyms iSyms = new IndexedSyms(sortedSyms.size(), testFile, "test", (IndexWriter) parser);
			IndexingUtils.writeIndexedAnnotations(sortedSyms, seq, iSyms, fos);

			String overlap = "3000000:160000000";
			SeqSpan overlap_span = ServerUtils.getLocationSpan(seqid, overlap, genome);

			List newResults = ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,genome);
			assertEquals(337, newResults.size());

			overlap = "115000000:123000000";
			overlap_span = ServerUtils.getLocationSpan(seqid, overlap, genome);

			newResults = ServerUtils.getIndexedOverlappedSymmetries(overlap_span,iSyms,genome);
			assertEquals(6, newResults.size());

			if (testFile.exists()) {
				testFile.delete();
			}

		} catch (Exception ex) {
			Logger.getLogger(BgnParserTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}

	}*/
}
