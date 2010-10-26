package com.affymetrix.genometryImpl.util;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

/**
 * A class to test the SynonymLookup class.
 * Specifially tests the effect of setting the flags "case_sensitive" and "strip_random".
 *
 * @version $Id: SynonymLookupTest.java 3690 2009-04-10 17:40:44Z jnicol $
 */
public class SynonymLookupTest {
	static SynonymLookup sl;


	public SynonymLookupTest() {
	}


	@Before
		public void setUp() {
			sl = new SynonymLookup();


			sl.addSynonyms(new String[] {"1", "chr1", "CHR1"}); // Add upper and lower case by hand in this test
			sl.addSynonyms(new String[] {"1", "one"});
			// NOTE that "one" and "chr1" are NOW currently considered synonyms 
			// even though they are listed on separate lines.  (This was not true prior to IGB 4.46)

			sl.addSynonyms(new String[] {"chr2", "", "2"});
			sl.addSynonyms(new String[] {"chr3", "", null, "3", "chrIII"});
			sl.addSynonyms(new String[] {"chr3_random", "3_random"});
			sl.addSynonyms(new String[] {"chrM", "M", "chrMT", "mitochondria", "mitochondrion"});
			sl.addSynonyms(new String[] {"R_norvegicus_Jan_2003", "Rat_Jan_2003", "Rat jan2003", "rn2", "rn:Jan_2003", "Rn:Jan_2003"});
			sl.addSynonyms(new String[] {"H_sapiens_May_2004", "Human_May_2004", "hs.NCBIv35", "hg17", "human_ncbi35", "ncbi.v35", "hs:May_2004", "Hs:May_2004", "hs:NCBIv35", "Hs:NCBIv35", "ensembl1834", "chado/chado-Hsa-17", "human/17", "Hs;NCBIv35", "Hs:NCBI35"});
		}

	@Test
		public void testAddSynonym() {
			Collection<String> synonymSet;
			List<String> a = new ArrayList<String>();

			boolean cs = true;

			sl.addSynonyms(new String[] {"a", "b", "c"});
			sl.addSynonyms(new String[] {"d", "e", "f"});
			sl.addSynonyms(new String[] {"g", "a", "d"});

			a.add("a");
			a.add("b");
			a.add("c");
			a.add("d");
			a.add("e");
			a.add("f");
			a.add("g");

			//System.out.println("running testAddSynonym");

			for (String synonym : a) {
				//System.out.println("synonym:    " + synonym);
				synonymSet = sl.getSynonyms(synonym, cs);
				//System.out.println("synonymSet: " + synonymSet);
				assertEquals("synonymSet is the wrong size for synonym " + synonym + ".",7, synonymSet.size());
				for (String current : a) {
					assertTrue("Could not find synonym " + current + "in list.", synonymSet.contains(current));
				}
			}
		}

	@Test
		public void testCaseInsensitiveLookup() {
			List<String> a = new ArrayList<String>();

			sl.addSynonyms(new String[] {"aa", "bb", "cc"});
			sl.addSynonyms(new String[] {"AA", "dd", "ee"});

			a.add("aa");
			a.add("AA");
			a.add("bb");
			a.add("cc");
			a.add("dd");
			a.add("ee");

			//System.out.println("running testCaseInsensitiveLookup");

			caseInsensitiveLookupHelper("aA", a);
			caseInsensitiveLookupHelper("aa", a);
			caseInsensitiveLookupHelper("AA", a);
		}

	private void caseInsensitiveLookupHelper(String test, List<String> expected) {
		Collection<String> synonymSet = sl.getSynonyms(test, false);

		//System.out.println("synonym:    " + test);
		//System.out.println("synonymSet: " + synonymSet);
		assertEquals("Size of synonymSet does not match size expected", expected.size(), synonymSet.size());
		for (String synonym : expected) {
			assertTrue("Could not find synonym " + synonym + " in list.", synonymSet.contains(synonym));
		}
	}

	/** Some tests that don't depend on whether case-sensitive or not. 
	 *  This test is called as a helper in some other tests.
	 */
	public static void helper1(SynonymLookup sl, boolean cs, boolean sr) {
		assertTrue(sl.isSynonym("chr1", "1", cs, sr));
		assertTrue(sl.isSynonym("chr1", "CHR1", cs, sr));
		assertTrue(sl.isSynonym("1", "chr1", cs, sr));
		assertTrue(sl.isSynonym("1", "CHR1", cs, sr));
		assertTrue(sl.isSynonym("CHR1", "chr1", cs, sr));
		assertTrue(sl.isSynonym("CHR1", "1", cs, sr));

		// This tests transitivity: that two things defined on separate lines ARE considered synonyms
		assertTrue(sl.isSynonym("CHR1", "one", cs, sr));

		assertFalse(sl.isSynonym("1", "chr2", cs, sr));

		assertTrue(sl.isSynonym("chr2", "2", cs, sr));
		assertTrue(sl.isSynonym("2", "chr2", cs, sr));

		Collection<String> list1 = sl.getSynonyms("chrMT", cs);
		Collection<String> list2 = sl.getSynonyms("chrM", cs);
		assertEquals(list1.size(), list2.size());    

		// This tests that the null and empty strings were ignored in the input
		// (If they were not ignored, then "chr2" == "" == "chr3" due to transitivity)
		assertFalse(sl.isSynonym("chr2", "chr3", cs, sr));

		// The elements in list1 and list2 should be the same, other than ordering
		Set<String> set1 = new HashSet<String>(list1);
		Set<String> set2 = new HashSet<String>(list2);
		assertEquals(set1, set2);    
	}

	/** Run some tests with case-sensitive set to true. */
	@Test
		public void testCaseSensitive() {
			boolean cs = true;
			boolean sr = false;
			helper1(sl, cs, sr);

			// These tests are all false when case-sensitive is true
			assertFalse(sl.isSynonym("CHR2", "2", cs, sr));
			assertFalse(sl.isSynonym("CHR2", "chr2", cs, sr));
			assertFalse(sl.isSynonym("2", "CHR2", cs, sr));
			assertFalse(sl.isSynonym("chr2", "CHR2", cs, sr));

			// There is no list of synonyms for "chrm" if case sensitive is true.
			Collection<String> list3 = sl.getSynonyms("chrm", cs);
			assertTrue(list3.isEmpty());
		}

	/** Run some tests with case-sensitive set to false. */
	@Test
		public void testCaseInsensitive() {
			boolean cs = false;
			boolean sr = false;
			helper1(sl, cs, sr);

			// These tests are all true when case-sensitive is true
			assertTrue(sl.isSynonym("CHR2", "2", cs, sr));
			assertTrue(sl.isSynonym("CHR2", "chr2", cs, sr));
			assertTrue(sl.isSynonym("2", "CHR2", cs, sr));
			assertTrue(sl.isSynonym("chr2", "CHR2", cs, sr));

			// There *is* a list of synonyms for "chrm" if case sensitive is false.
			// and it should match the list for "CHRmT"
			Collection<String> list3 = sl.getSynonyms("chrm", cs);
			Collection<String> list4 = sl.getSynonyms("CHRmT", cs);
			assertTrue(list3 != null);
			assertTrue(list4 != null);

			// The elements in list3 and list4 should be the same, other than ordering
			Set<String> set3 = new HashSet<String>(list3);
			Set<String> set4 = new HashSet<String>(list4);
			assertEquals(set3, set4);

			// even with case-sensitive set to false, the list will not actually contain "chrm", but rather "chrM"
			// since that is how we put it in the list.
			assertTrue(list3.contains("chrM"));
			assertFalse(list3.contains("chrm"));
		}

	/** Tests what happens when SynonymLookup.stripRandom is set to true.  */
	@Test
		public void testStripRandom() {
			boolean cs = true;
			boolean sr = true;
			helper1(sl, cs, sr);

			assertTrue(sl.isSynonym("1_random", "chr1_random", cs, sr));
			assertTrue(sl.isSynonym("1_random", "one_random", cs, sr));
			assertTrue(sl.isSynonym("chrMT_random", "M_random", cs, sr));

			// "random" chromosomes are NOT the same as non-random ones
			assertFalse(sl.isSynonym("1_random", "chr1", cs, sr));
			assertFalse(sl.isSynonym("2", "chr2_random", cs, sr));

			// True because "CHR1", "chr1" and "1" were all explicity given as input
			assertTrue(sl.isSynonym("1_random", "CHR1_random", cs, sr));
			assertTrue(sl.isSynonym("1_random", "chr1_random", cs, sr));
			assertTrue(sl.isSynonym("chr1_random", "CHR1_random", cs, sr));

			// False due to case-sensitivity: we know nothing about "CHR2"
			assertFalse(sl.isSynonym("2_random", "CHR2_random", cs, sr));
			assertFalse(sl.isSynonym("chr2_random", "CHR2_random", cs, sr));

			assertFalse(sl.isSynonym("1_ranDom", "chr1_Random", cs, sr));


			// When case-sensitive is false, it should now know that 2_random == CHR2_random == chr2_random
			cs = false;
			assertTrue(sl.isSynonym("2_random", "CHR2_random", cs, sr));
			assertTrue(sl.isSynonym("chr2_random", "CHR2_random", cs, sr));

			assertTrue(sl.isSynonym("CHRMT_RaNdOm", "M_random", cs, sr));
			assertFalse(sl.isSynonym("CHRMT_RANDOM", "CHRM", cs, sr));

			// This is false because we didn't teach it about "MT" (we taught it "chrMT" not "MT")
			assertFalse(sl.isSynonym("CHRMT_RANDOM", "MT_random", cs, sr));
		}
}
