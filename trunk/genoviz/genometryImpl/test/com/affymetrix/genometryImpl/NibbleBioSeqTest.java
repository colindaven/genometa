package com.affymetrix.genometryImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.util.NibbleIterator;

public class NibbleBioSeqTest {
	static GenometryModel gmodel = GenometryModel.getGenometryModel();

	public NibbleBioSeqTest() {
	}

	@Before
		public void setUp() {
		}

	@After
		public void tearDown() {
		}

	/**
	 * Verify that, if you read in a nibble string, and then write it out, it's the same.
	 */
	@Test
	public void testStringToNibblesAndBack() {
		String test_string = "ACTGAAACCCTTTGGGNNNATATGCGC";

		byte[] test_array = NibbleIterator.stringToNibbles(test_string, 0, test_string.length());
		BioSeq nibseq = new BioSeq(null, null, test_string.length());
		NibbleIterator nibber = new NibbleIterator(test_array, test_string.length());
		assertEquals(test_string, nibber.substring(0, test_string.length()));


		nibseq.setResiduesProvider(nibber);
		String result_string = NibbleIterator.nibblesToString(test_array, 0, test_string.length());
		assertEquals(test_string, result_string);
	}

	@Test
	public void testSubstring() {
		String test_string = "ACTGAAACCCTTTGGGNNNATATGCGC";

		byte[] test_array = NibbleIterator.stringToNibbles(test_string, 0, test_string.length());
		NibbleIterator nibber = new NibbleIterator(test_array, test_string.length());

		// Testing that substring is implemented properly
		//System.out.println(test_string.substring(5, 10) + " : " + nibber.substring(5, 10));
		assertEquals(test_string.substring(5, 10), nibber.substring(5, 10));
	}
}
