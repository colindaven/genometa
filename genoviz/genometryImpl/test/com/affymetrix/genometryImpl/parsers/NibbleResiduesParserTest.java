package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.util.NibbleIterator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jnicol1
 */
public class NibbleResiduesParserTest {
	public NibbleResiduesParserTest() {
	}
	@Test
	public void testNibbleToBinaryAndBack() {
		String testString = createTestString();

		byte[] nibble_array = NibbleIterator.stringToNibbles(testString, 0, testString.length());
		String newString = NibbleIterator.nibblesToString(nibble_array, 0, testString.length());
		assertEquals(testString.length(), newString.length());

		for (int i = 0; i < testString.length(); i++) {
			char testChar = testString.charAt(i);
			char newChar = newString.charAt(i);
			assertEquals(testChar, newChar);
		}
	}

	/**
	 * Create string with all 256 possible 2-residue combinations... AA, AC, ..., AU, ..., UA, UC, ..., UU
	 * @return string
	 */
	private String createTestString() {
		StringBuffer testBuffer = new StringBuffer(256*2);
		char[] nibble2char = {'A', 'C', 'G', 'T', 'N', 'M', 'R', 'W', 'S', 'Y', 'K', 'V', 'H', 'D', 'B', 'U'};
		for (int i = 0; i < 16; i++) {
			for (int j = 0; j < 16; j++) {
				String tempString = String.valueOf(nibble2char[i]) + String.valueOf(nibble2char[j]);
				assertEquals(2, tempString.length());
				testBuffer.append(tempString);
			}
		}
		String testString = testBuffer.toString();
		assertEquals(256*2, testString.length());
		return testString;
	}
}
