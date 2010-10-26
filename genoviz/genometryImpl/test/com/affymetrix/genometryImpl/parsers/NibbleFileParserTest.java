
package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author hiralv
 */
public class NibbleFileParserTest {

	String input_string = "ACTGGGTCTCAGTACTAGGAATTCCGTCATAGCTAAA";
	String infile_name = "test/data/bnib/BNIB_chrQ2.bnib";
	File infile = new File(infile_name);
	StringBuffer sb;
	InputStream isr;
	int total_residues = input_string.length();

	@Before
	public void setup() throws Exception
	{
		assertTrue(infile.exists());
	}

	@Test
	public void testOriginal() throws Exception
	{
		sb = new StringBuffer();
		isr = GeneralUtils.getInputStream(infile, sb);
		BioSeq seq= NibbleResiduesParser.parse(isr, new AnnotatedSeqGroup("Test"));
		assertEquals(seq.getResidues(),input_string);
	}

	@Test
	public void testCases() throws Exception
	{
		testCase(0,input_string.length());  //From begining to end

		testCase(4,18);						// even, even
		testCase(6,7);						// even, odd
		testCase(1,4);						// odd , even
		testCase(1,5);						// odd , odd
		testCase(-1,3);						// Start out of range
		testCase(11,total_residues+4);		// End out of range
		testCase(-5,total_residues+5);      // Start and end out of range

		testCase(2,22);
		testCase(6,7);
		testCase(1,2);
		testCase(5,15);
		testCase(-9,9);
		testCase(1,total_residues+9);
		testCase(-15,total_residues+15);

		testCase(0,2);
		testCase(0,1);
		testCase(1,2);
		testCase(1,3);
		testCase(-11,15);
		testCase(10,total_residues+9);
		testCase(-11,total_residues+15);

		testCase(0,0);						// 0 length
		testCase(1,0);						// Negative length
		testCase(total_residues,0);
		testCase(total_residues,-4);
		testCase(total_residues,total_residues);
		testCase(total_residues,total_residues+1);
		testCase(total_residues+1,total_residues);
		testCase(total_residues+1,total_residues+1);
		testCase(total_residues+1,total_residues+5);
		testCase(Integer.MIN_VALUE,Integer.MAX_VALUE);
	}

	public void testCase(int start, int end) throws Exception
	{
		sb = new StringBuffer();
		isr = GeneralUtils.getInputStream(infile, sb);
		ByteArrayOutputStream outstream = new ByteArrayOutputStream();
		boolean result = NibbleResiduesParser.parse(isr, new AnnotatedSeqGroup("Test"),start,end,outstream);

		if (start < end) {
			start = Math.max(0, start);
			start = Math.min(total_residues, start);

			end = Math.max(0, end);
			end = Math.min(total_residues, end);
		}
		else
		{
			start = 0;
			end = 0;
		}
		
		assertTrue(result);
		assertEquals(input_string.substring(start, end),outstream.toString());
		//System.out.println(input_string.substring(start, end) + "==" +outstream.toString());
		outstream.close();
	}


}
