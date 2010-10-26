package com.affymetrix.genometryImpl.util;

import java.io.IOException;
import java.util.Set;
import java.util.regex.Pattern;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.util.logging.Level;
import java.util.List;
import java.util.logging.Logger;
import com.affymetrix.genometryImpl.parsers.IndexWriter;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.UcscPslSym;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jnicol
 *
 * Verify that searches (locally and on server) return correct results.
 */
public class SearchUtilsTest {
	File f = null;
	AnnotatedSeqGroup group = GenometryModel.getGenometryModel().addSeqGroup("searchGroup");
	List<UcscPslSym> syms = null;
	Pattern regex = Pattern.compile(".*EG510482.*", Pattern.CASE_INSENSITIVE);
	IndexWriter iWriter = null;

	@Before
	public void setUp() {
		assertNotNull(group);
		
		DataInputStream dis = null;
		try {
			String filename = "test/data/psl/search.psl";
			// load in test file.
			f = new File(filename);
			assertTrue(f.exists());
			iWriter = new PSLParser();
			dis = new DataInputStream(new FileInputStream(f));
			syms = ((PSLParser)iWriter).parse(dis, "SearchTest", null, group, false, true);
			assertEquals(46, syms.size());
			assertEquals(5,group.getSeqCount());
			assertEquals("5", group.getSeq(0).getID());

		} catch (Exception ex) {
			Logger.getLogger(SearchUtilsTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		} finally {
			GeneralUtils.safeClose(dis);
		}
	}

	@Test
	public void testLocalSearch() {
		List<SeqSymmetry> foundSyms = SearchUtils.findLocalSyms(group, null, regex, false);
		assertEquals(46, foundSyms.size());
	}

	@Test
	public void testNonIndexedServerSearch() {
		Set<SeqSymmetry> foundSyms = SearchUtils.findNameInGenome(".*EG510482.*", group);
		assertEquals(0, foundSyms.size());
		
	}

	
	@Test
	public void testIndexedServerSearch() {
		try {
			Set<SeqSymmetry> foundSyms = null;
			foundSyms = IndexingUtils.findSymsByName(group, regex);
			assertEquals(0, foundSyms.size());

			// Need to index information
			AnnotatedSeqGroup tempGroup = AnnotatedSeqGroup.tempGenome(group);
			assertEquals(group.getSeqCount(), tempGroup.getSeqCount());
			
			List loadedSyms = ServerUtils.loadAnnotFile(f, "indexPSL", null, tempGroup, true);
			assertEquals(46, loadedSyms.size());

			/*IndexingUtils.determineIndexes(group, tempGroup, System.getProperty("user.dir"), f, loadedSyms, iWriter, "indexPSL", "indexPSL");
			
			foundSyms = IndexingUtils.findSymsByName(tempGroup, regex);
			assertEquals(0, foundSyms.size());*/

		} catch (Exception ex) {
			Logger.getLogger(SearchUtilsTest.class.getName()).log(Level.SEVERE, null, ex);
			fail();
		}

	}
}
