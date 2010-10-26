package com.affymetrix.genometryImpl.util;

import java.awt.Canvas;
import java.awt.Font;
import java.awt.FontMetrics;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sgblanch
 * @version $Id: StringUtilsTest.java 4596 2009-10-26 13:38:14Z jnicol $
 */
public final class StringUtilsTest {
	private Canvas canvas;
	private static final String loremIpsum =
				"Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut " +
				"labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco " +
				"laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in " +
				"voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat " +
				"non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.";

    public StringUtilsTest() {
    }

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

    @Before
    public void setUp() {
		this.canvas = new Canvas();
    }

    @After
    public void tearDown() {
		this.canvas = null;
    }

	/**
	 * Test of wrap method, of class StringUtils.
	 */
	@Test
	public void testWrap_3args() {
		String toWrap = loremIpsum;
		FontMetrics metrics = this.canvas.getFontMetrics(new Font("sansserif", Font.PLAIN, 12));
		int pixels;
		String[] result;
		
		/*************************************/

		/* Normal case */
		pixels = 300;
		result = StringUtils.wrap(toWrap, metrics, pixels);

		verifyWrap(result, metrics, pixels);
		assertTrue("Wrapped text does not appear to be complete", result[result.length - 1].endsWith("laborum. "));

		/*************************************/

		/* Extreme test.  Most words should be wider than 50 pixels. */
		pixels = 50;
		result = StringUtils.wrap(toWrap, metrics, pixels);
		
		verifyWrap(result, metrics, pixels);
		assertTrue("Wrapped text does not appear to be complete", result[result.length - 1].endsWith("laborum. "));
	}

	/**
	 * Test of wrap method, of class StringUtils.
	 */
	@Test
	public void testWrap_4args() {
		String toWrap = loremIpsum;
		FontMetrics metrics = this.canvas.getFontMetrics(new Font("sansserif", Font.PLAIN, 12));
		int pixels = 300;
		int maxLines;
		String[] result;
		
		/*************************************/

		/* Normal case */
		maxLines = 3;
		result = StringUtils.wrap(toWrap, metrics, pixels, maxLines);

		assertEquals("Wrong number of lines in wrapped text", maxLines, result.length);
		verifyWrap(result, metrics, pixels);

		/* Check for the ellipsis */
		String lastLine = result[result.length - 1];
		assertEquals("Wrapped text does not end with ellipsis (\u2026)",'\u2026', lastLine.charAt(lastLine.length() - 1));

		/*************************************/

		/* One line is a special case in the code */
		maxLines = 1;
		result = StringUtils.wrap(toWrap, metrics, pixels, maxLines);

		assertEquals("Wrong number of lines in wrapped text", maxLines, result.length);
		verifyWrap(result, metrics, pixels);

		/* Check for the ellipsis */
		lastLine = result[result.length - 1];
		assertEquals("Wrapped text does not end with ellipsis (\u2026)",'\u2026', lastLine.charAt(lastLine.length() - 1));
	}

	/**
	 * Verify word-by-word that every word in the original text exists in the
	 * wrapped text.  Will break on the ellipsis (\u2026) character as that
	 * is used when the text was truncated to a certain number of lines.  This
	 * function will also verify that every line containing a space is less than
	 * or equal to the wrap width.
	 *
	 * @param result Array of strings, one per line of wrapped text
	 * @param metrics Font metrics used to calculate widths
	 * @param pixels Number of pixels to wrap text to.
	 */
	private static void verifyWrap(String[] result, FontMetrics metrics, int pixels) {
		String current;
		int width;
		int i = 0;
		String[] words = loremIpsum.split("\\s+");
		for (String line : result) {
			/* Check that line lengths are not greater than maximum */
			if (line.endsWith(" ")) {
				current = line.substring(0, line.length() - 1);
			} else {
				current = line;
			}
			width = metrics.stringWidth(current);
			assertFalse("Current line is larger than wrap width ", current.contains(" ") && width > pixels);

			/* check that wrapped text contains every word */
			for (String word : line.split("\\s+")) {
				if ("\u2026".equals(word)) {
					break;
				}
				assertEquals("Found incorrect word in text", words[i], word);
				i++;
			}
		}
	}
}
