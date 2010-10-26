package com.affymetrix.genometryImpl.parsers.graph;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.ScoredContainerSym;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author auser
 */
public class ScoredIntervalParserTest {

	public ScoredIntervalParserTest() {
	}

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void testParseFromFile() throws IOException {
		String filename = "test/data/egr/test1.egr";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);
		String stream_name = "chr1";
		AnnotatedSeqGroup seq_group = GenometryModel.getGenometryModel().addSeqGroup("Test Seq Group");

		try {
			ScoredIntervalParser tester = new ScoredIntervalParser();
			tester.parse(istr, stream_name, seq_group, true);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		//System.out.println("done testing ScoredIntervalParser");
		String unique_container_name = AnnotatedSeqGroup.getUniqueGraphID(stream_name, seq_group);
		assertEquals("chr1.1", unique_container_name);
	}

	@Test
	public void testMakeNewSeq() {

		AnnotatedSeqGroup seq_group = GenometryModel.getGenometryModel().addSeqGroup("Test Seq Group");
		String seqid = "chr1";

		BioSeq aseq = seq_group.getSeq(seqid);
		ScoredIntervalParser ins = new ScoredIntervalParser();

		aseq = seq_group.addSeq(seqid, 0); // hmm, should a default size be set?
		assertEquals(100208700, aseq.getLength());
		assertEquals("Test Seq Group", aseq.getVersion());
		assertEquals("chr1", aseq.getID());
	}

	@Test
	public void testwriteEgrFormat() throws IOException {
		String string = "# genome_version = H_sapiens_Mar_2006\n" +
				"# score0 = NormDiff\n" +
				"chr1	10015038	10016039	.	25.0\n" +
				"chr1	100004630	100005175	.	6.0\n" +
				"chr1	100087772	100088683	.	13.0\n" +
				"chr1	100207533	100208700	.	230.0\n";

		InputStream istr = new ByteArrayInputStream(string.getBytes());
		AnnotatedSeqGroup seq_group = GenometryModel.getGenometryModel().addSeqGroup("Test Seq Group");
		String stream_name = "chr1";
		ScoredIntervalParser tester = new ScoredIntervalParser();
		tester.parse(istr, stream_name, seq_group, true);
		assertEquals(1, seq_group.getSeqCount());
		BioSeq aseq = seq_group.getSeq(0);
		assertEquals("chr1", aseq.getID());
		ScoredContainerSym symI = (ScoredContainerSym) aseq.getAnnotation(0);
		assertEquals("chr1", symI.getID());
		assertEquals(2, aseq.getAnnotationCount());
		GraphIntervalSym result = symI.makeGraphSym("NormDiff", seq_group);
		assertEquals(4, result.getChildCount());
		String genome_version = "H_sapiens_Mar_2006";
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		Boolean out = ScoredIntervalParser.writeEgrFormat(result, genome_version, outstream);
		assertEquals(true, out);
		assertEquals(string, outstream.toString());
	}
}

