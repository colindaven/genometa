package com.affymetrix.igb.parsers;


import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class ChpParserTest {

	@Before
	public void setUp() {
	}

	@After
	public void tearDown() {
	}

	@Test
	public void TestParseFromFile() throws IOException, Exception {
		 // unzip this file.
		String zippedFileStr= ("../genometryImpl/test/data/chp/TisMap_Brain_01_v1_WTGene1.rma-gene-default.chp.gz");

		File f = new File(zippedFileStr);
		assertTrue(f.exists());
		File f2 = File.createTempFile(f.getName(), ".chp");
		f2.deleteOnExit();
		assertTrue(f2.exists());
		unzipFile(f, f2);
		
		AnnotatedSeqGroup group = new AnnotatedSeqGroup("test");
		BioSeq seq = new BioSeq("chr1","test version", 100);
		group.addSeq(seq);
		GenometryModel.getGenometryModel().setSelectedSeqGroup(group);
		//List<? extends SeqSymmetry> results = ChpParser.parse(f2.getAbsolutePath(), true);
		//	assertEquals(1, results.size());
	}

	private static void unzipFile(File f, File f2) throws IOException {
		// File must be unzipped!
		InputStream is = null;
		OutputStream out = null;
		try {
			// This will also unzip the stream if necessary
			is = GeneralUtils.getInputStream(f, new StringBuffer());
			out = new FileOutputStream(f2);
			byte[] buf = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			GeneralUtils.safeClose(is);
			GeneralUtils.safeClose(out);
		}
	}
}
