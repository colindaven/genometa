package com.affymetrix.genometryImpl.parsers.graph;

import com.affymetrix.genometryImpl.parsers.graph.SgrParser;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.symloader.Sgr;

import java.io.*;
import java.util.*;

public class SgrParserTest {

	@Test
	public void testParseFromFile() throws IOException {

		String filename = "test/data/sgr/test1.sgr";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;
		String stream_name = "test_file";
		boolean ensure_unique_id = true;

		List<GraphSym> results = SgrParser.parse(istr, stream_name, group, annot_seq, ensure_unique_id);

		assertEquals(1, results.size());
		GraphSym gr0 = results.get(0);

		assertEquals("16", gr0.getGraphSeq().getID());
		assertEquals(10, gr0.getPointCount());
		assertEquals(-0.0447924, gr0.getGraphYCoord(2), 0.01);
		assertEquals(0.275948, gr0.getGraphYCoord(3), 0.01);
		assertEquals(948028, gr0.getGraphXCoord(3));
	}

	@Test
	/**
	 * Make sure this writes out the same format it reads in.
	 */
	public void testWriteFormat() throws IOException {

		String string =
						"16	948025	0.128646\n" +
						"16	948026	0.363933\n";
		InputStream istr = new ByteArrayInputStream(string.getBytes());

		AnnotatedSeqGroup group = new AnnotatedSeqGroup("Test Group");
		boolean annot_seq = true;
		String stream_name = "test_file";
		boolean ensure_unique_id = true;

		List<GraphSym> results = SgrParser.parse(istr, stream_name, group, annot_seq, ensure_unique_id);

		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		
		SgrParser.writeSgrFormat(results.get(0), outstream);

		assertEquals(string, outstream.toString());
	}

	@Test
	public void testSgr() throws IOException{
		String filename = "test/data/sgr/test4.sgr";
		assertTrue(new File(filename).exists());
		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

		Sgr sgr = new Sgr(new File(filename).toURI(), filename, seq_group);

		List<GraphSym> results = sgr.getGenome();

		assertEquals(4, results.size());
		GraphSym gr0 = results.get(0);

		assertEquals("16", gr0.getGraphSeq().getID());
		assertEquals(5, gr0.getPointCount());
		assertEquals(-0.0447924, gr0.getGraphYCoord(2), 0.01);
		assertEquals(0.275948, gr0.getGraphYCoord(3), 0.01);
		assertEquals(948028, gr0.getGraphXCoord(3));

		GraphSym gr1 = results.get(1);

		assertEquals("17", gr1.getGraphSeq().getID());
		assertEquals(4, gr1.getPointCount());
		assertEquals(0.8384833, gr1.getGraphYCoord(1), 0.01);
		assertEquals(0.4523419, gr1.getGraphYCoord(2), 0.01);
		assertEquals(948030, gr1.getGraphXCoord(2));


		GraphSym gr2 = results.get(2);

		assertEquals("18", gr2.getGraphSeq().getID());
		assertEquals(2, gr2.getPointCount());
		assertEquals(0.9203930, gr2.getGraphYCoord(1), 0.01);
		assertEquals(0.2789456, gr2.getGraphYCoord(0), 0.01);
		assertEquals(948033, gr2.getGraphXCoord(0));

		GraphSym gr3 = results.get(3);

		assertEquals("19", gr3.getGraphSeq().getID());
		assertEquals(8, gr3.getPointCount());
		assertEquals(-0.0447924, gr3.getGraphYCoord(2), 0.01);
		assertEquals(0.275948, gr3.getGraphYCoord(3), 0.01);
		assertEquals(948028, gr3.getGraphXCoord(3));
	}

	@Test
	public void testWriteAnnotation() throws Exception {
		String string =
						"16	948025	0.128646\n" +
						"16	948026	0.363933\n";

		File file = createFileFromString(string);
		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
		Sgr sgr = new Sgr(file.toURI(), file.getName(), seq_group);
		List<GraphSym> results = sgr.getGenome();

		ByteArrayOutputStream outstream = new ByteArrayOutputStream();

		sgr.writeAnnotations(results, null, null, outstream);

		assertEquals(string, outstream.toString());
	}

	public File createFileFromString(String string) throws Exception{
		File tempFile = File.createTempFile("tempFile", ".sgr");
		tempFile.deleteOnExit();
		BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile, true));
		bw.write(string);
		bw.close();
		return tempFile;
	}
}
