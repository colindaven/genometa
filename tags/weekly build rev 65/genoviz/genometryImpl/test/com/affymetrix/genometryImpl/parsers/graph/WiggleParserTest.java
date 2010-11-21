/*
 * WiggleParserTest.java
 * JUnit based test
 *
 * Created on October 18, 2006, 2:36 PM
 */
package com.affymetrix.genometryImpl.parsers.graph;

import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphIntervalSym;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.style.GraphState;
import com.affymetrix.genometryImpl.symloader.Wiggle;
import java.io.*;
import java.util.*;

/**
 *
 * @author Ed Erwin
 */
public class WiggleParserTest {

	public WiggleParserTest() {
	}

	@Test
	public void testParse() throws Exception {
		String filename = "test/data/wiggle/wiggleExample.wig";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename);
		assertNotNull(istr);

		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

		WiggleParser parser = new WiggleParser();

		List<GraphSym> results = parser.parse(istr, seq_group, true, filename);

		assertEquals(3, results.size());

		GraphSym gr0 = results.get(0);

		BioSeq seq = gr0.getGraphSeq();

		// BED format
		assertTrue(gr0 instanceof GraphIntervalSym);
		assertEquals("chr19", gr0.getGraphSeq().getID());
		assertEquals(9, gr0.getPointCount());
		assertEquals(59302000, gr0.getSpan(seq).getMin());
		assertEquals(59304700, gr0.getSpan(seq).getMax());

		// variableStep format
		GraphSym gr1 = results.get(1);
		assertTrue(gr1 instanceof GraphIntervalSym);
		assertEquals(9, gr1.getChildCount());
		assertTrue(gr1.getChild(0) instanceof Scored);
		assertEquals(59304701 - 1, gr1.getSpan(seq).getMin());	// variableStep: 1-relative format
		assertEquals(59308021 - 1, gr1.getSpan(seq).getMax());	// variableStep: 1-relative foramt

		// fixedStep format
		GraphSym gr2 = results.get(2);
		assertTrue(gr2 instanceof GraphIntervalSym);
		assertEquals(10, gr2.getChildCount());
		assertEquals(59307401 - 1, gr2.getSpan(seq).getMin());			// fixedStep: 1-relative format
		assertEquals(59310301 - 1, gr2.getSpan(seq).getMax());			// fixedStep: 1-relative format
		assertEquals(300.0f, ((Scored) gr2.getChild(7)).getScore(), 0.00000001);

		assertEquals("Bed Format", gr0.getID());
		assertEquals("variableStep", gr1.getID());
		assertEquals("fixedStep", gr2.getID());

		GraphState state = gr1.getGraphState();
		assertEquals(0.0, state.getVisibleMinY(), 0.00001);
		assertEquals(25.0, state.getVisibleMaxY(), 0.00001);

		assertEquals(59310301 - 1, seq.getLength());	// fixedStep was 1-relative format.

	}

	//Test to see if one file is created.
	@Test
	public void testWiggle1() throws IOException {
		String filename = "test/data/wiggle/wiggleExample.wig";
		assertTrue(new File(filename).exists());

		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
		Wiggle wiggle = new Wiggle(new File(filename).toURI(), filename, seq_group);
		BioSeq aseq = seq_group.addSeq("chr19", 59310300);

		List<GraphSym> results = wiggle.getGenome();

		assertEquals(3, results.size());

		GraphSym gr0 = results.get(0);

		BioSeq seq = gr0.getGraphSeq();

		// BED format
		assertTrue(gr0 instanceof GraphIntervalSym);
		assertEquals("chr19", gr0.getGraphSeq().getID());
		assertEquals(9, gr0.getPointCount());
		assertEquals(59302000, gr0.getSpan(seq).getMin());
		assertEquals(59304700, gr0.getSpan(seq).getMax());

		// variableStep format
		GraphSym gr1 = results.get(1);
		assertTrue(gr1 instanceof GraphIntervalSym);
		assertEquals(9, gr1.getChildCount());
		assertTrue(gr1.getChild(0) instanceof Scored);
		assertEquals(59304701 - 1, gr1.getSpan(seq).getMin());	// variableStep: 1-relative format
		assertEquals(59308021 - 1, gr1.getSpan(seq).getMax());	// variableStep: 1-relative foramt

		// fixedStep format
		GraphSym gr2 = results.get(2);
		assertTrue(gr2 instanceof GraphIntervalSym);
		assertEquals(10, gr2.getChildCount());
		assertEquals(59307401 - 1, gr2.getSpan(seq).getMin());			// fixedStep: 1-relative format
		assertEquals(59310301 - 1, gr2.getSpan(seq).getMax());			// fixedStep: 1-relative format
		assertEquals(300.0f, ((Scored) gr2.getChild(7)).getScore(), 0.00000001);

		assertEquals(AnnotatedSeqGroup.getUniqueGraphTrackID(filename, "Bed Format"), gr0.getID());
		assertEquals(AnnotatedSeqGroup.getUniqueGraphTrackID(filename, "variableStep"), gr1.getID());
		assertEquals(AnnotatedSeqGroup.getUniqueGraphTrackID(filename, "fixedStep"), gr2.getID());

		GraphState state = gr1.getGraphState();
		assertEquals(0.0, state.getVisibleMinY(), 0.00001);
		assertEquals(25.0, state.getVisibleMaxY(), 0.00001);

		assertEquals(59310301 - 1, seq.getLength());	// fixedStep was 1-relative format.

		results = wiggle.getChromosome(aseq);
		assertEquals(3, results.size());

		gr0 = results.get(0);

		assertTrue(gr0 instanceof GraphIntervalSym);
		assertEquals("chr19", gr0.getGraphSeq().getID());
		assertEquals(9, gr0.getPointCount());
		assertEquals(59302000, gr0.getSpan(seq).getMin());
		assertEquals(59304700, gr0.getSpan(seq).getMax());

		gr1 = results.get(1);
		assertTrue(gr1 instanceof GraphIntervalSym);
		assertEquals(9, gr1.getChildCount());
		assertTrue(gr1.getChild(0) instanceof Scored);
		assertEquals(59304701 - 1, gr1.getSpan(seq).getMin());	// variableStep: 1-relative format
		assertEquals(59308021 - 1, gr1.getSpan(seq).getMax());	// variableStep: 1-relative foramt

		// fixedStep format
		gr2 = results.get(2);
		assertTrue(gr2 instanceof GraphIntervalSym);
		assertEquals(10, gr2.getChildCount());
		assertEquals(59307401 - 1, gr2.getSpan(seq).getMin());			// fixedStep: 1-relative format
		assertEquals(59310301 - 1, gr2.getSpan(seq).getMax());			// fixedStep: 1-relative format
		assertEquals(300.0f, ((Scored) gr2.getChild(7)).getScore(), 0.00000001);

		assertEquals(AnnotatedSeqGroup.getUniqueGraphTrackID(filename, "Bed Format"), gr0.getID());
		assertEquals(AnnotatedSeqGroup.getUniqueGraphTrackID(filename, "variableStep"), gr1.getID());
		assertEquals(AnnotatedSeqGroup.getUniqueGraphTrackID(filename, "fixedStep"), gr2.getID());

		List<BioSeq> allSeqs = wiggle.getChromosomeList();
		assertEquals(allSeqs.size(),1);

		seq = allSeqs.get(0);
		assertEquals(seq.getID(),"chr19");
	}

	//Test to see if multiple files are created.
	@Test
	public void testWiggle2() throws IOException{
		String filename = "test/data/wiggle/wiggleExample2.wig";
		assertTrue(new File(filename).exists());

		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
		Wiggle wiggle = new Wiggle(new File(filename).toURI(), filename, seq_group);

		List<GraphSym> results = wiggle.getGenome();

		testResults2(filename, results, true);
	}

	public boolean testResults2(String filename, List<GraphSym> results, boolean checkId){
		
		assertEquals(3, results.size());

		GraphSym gr0 = results.get(0);

		BioSeq seq = gr0.getGraphSeq();

		// BED format
		assertTrue(gr0 instanceof GraphIntervalSym);
		assertEquals("chr19", gr0.getGraphSeq().getID());
		assertEquals(9, gr0.getPointCount());
		assertEquals(59302000, gr0.getSpan(seq).getMin());
		assertEquals(59304700, gr0.getSpan(seq).getMax());

		// variableStep format
		GraphSym gr1 = results.get(1);
		seq = gr1.getGraphSeq();
		assertTrue(gr1 instanceof GraphIntervalSym);
		assertEquals("chr20", gr1.getGraphSeq().getID());
		assertEquals(9, gr1.getChildCount());
		assertTrue(gr1.getChild(0) instanceof Scored);
		assertEquals(59304701 - 1, gr1.getSpan(seq).getMin());	// variableStep: 1-relative format
		assertEquals(59308021 - 1, gr1.getSpan(seq).getMax());	// variableStep: 1-relative foramt

		// fixedStep format
		GraphSym gr2 = results.get(2);
		seq = gr2.getGraphSeq();
		assertTrue(gr2 instanceof GraphIntervalSym);
		assertEquals("chr21", gr2.getGraphSeq().getID());
		assertEquals(10, gr2.getChildCount());
		assertEquals(59307401 - 1, gr2.getSpan(seq).getMin());			// fixedStep: 1-relative format
		assertEquals(59310301 - 1, gr2.getSpan(seq).getMax());			// fixedStep: 1-relative format
		assertEquals(300.0f, ((Scored) gr2.getChild(7)).getScore(), 0.00000001);

		if(checkId){
			assertEquals(AnnotatedSeqGroup.getUniqueGraphTrackID(filename, "Bed Format"), gr0.getID());
		assertEquals(AnnotatedSeqGroup.getUniqueGraphTrackID(filename, "variableStep"), gr1.getID());
		assertEquals(AnnotatedSeqGroup.getUniqueGraphTrackID(filename, "fixedStep"), gr2.getID());
		}

		return true;
	}
	
	public void testWriteBarFormat() throws IOException {
		String filename = "test/data/wiggle/wiggleExample.wig";
		assertTrue(new File(filename).exists());

		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
		Wiggle wiggle = new Wiggle(new File(filename).toURI(), filename, seq_group);

		List<GraphSym> results = wiggle.getGenome();

		GraphSym gr0 = results.get(0);

		ByteArrayOutputStream outstream = new ByteArrayOutputStream();

		boolean written = Wiggle.writeBarFormat(results, seq_group.getID(), outstream);
		assertTrue(written);

		GenometryModel gmodel = GenometryModel.getGenometryModel();

		InputStream istr = new ByteArrayInputStream(outstream.toByteArray());
		results = BarParser.parse(istr,gmodel,seq_group,null,0,Integer.MAX_VALUE,"chr19",true);

		GraphSym gr1 = results.get(0);
		assertEquals(gr0.getGraphSeq().getID(),gr1.getGraphSeq().getID());
		assertEquals(gr0.getGraphSeq().getMin(),gr1.getGraphSeq().getMin());
		assertEquals(gr0.getGraphSeq().getMax(),gr1.getGraphSeq().getMax());
	}

	@Test
	public void testWriteAnnotation() throws Exception {
		String filename = "test/data/wiggle/wiggleExample2.wig";
		assertTrue(new File(filename).exists());

		AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");
		Wiggle wiggle = new Wiggle(new File(filename).toURI(), filename, seq_group);

		List<GraphSym> results = wiggle.getGenome();

		testResults2(filename, results, true);

		ByteArrayOutputStream outstream = new ByteArrayOutputStream();

		wiggle.writeAnnotations(results, null, null, outstream);

		File outfile = createFileFromString(outstream.toString());
		Wiggle outwiggle = new Wiggle(outfile.toURI(), outfile.getName(), seq_group);
		List<GraphSym> outresults = outwiggle.getGenome();

		testResults2(filename, outresults, false);

		File testFile = new File("test/data/wiggle/testFile.wig");

		FileInputStream fin = new FileInputStream(testFile);
		byte fileContent[] = new byte[(int)testFile.length()];
		fin.read(fileContent);
		String strFileContent = new String(fileContent);
		fin.close();
		
		assertEquals(strFileContent,outstream.toString());
	}

	public File createFileFromString(String string) throws Exception{
		File tempFile = File.createTempFile("tempFile", ".wig");
		tempFile.deleteOnExit();
		BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile, true));
		bw.write(string);
		bw.close();
		return tempFile;
	}
	/*
	@Test
	public void testWriteGraphs() {
	fail("test not implemented");
	}
	 */
}
