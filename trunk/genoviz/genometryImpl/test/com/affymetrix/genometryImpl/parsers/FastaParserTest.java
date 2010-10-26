package com.affymetrix.genometryImpl.parsers;

import org.junit.Test;
import static org.junit.Assert.*;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.*;
import java.util.List;


public class FastaParserTest {

	public FastaParserTest() {
	}

	@Test
		public void testGenerateNewHeader() throws Exception {
			String chrom_name = "chrC";
			String genome_name = "A_thaliana_TAIR8";

			byte[] result = FastaParser.generateNewHeader(chrom_name, genome_name, 0, 1000);

			assertNotNull(result);

			String expected_string = "chrC range:0-1,000 interbase genome:A_thaliana_TAIR8\n";
			byte[] expected_result = new byte[expected_string.length() +1];
			expected_result[0]='>';
			for (int i=0;i<expected_string.length();i++)
				expected_result[i+1] = (byte)expected_string.charAt(i);

			/*for (int i=0;i<result.length;i++)
			  System.out.print(":" + (char)result[i]);
			  System.out.println();
			  for (int i=0;i<expected_result.length;i++)
			  System.out.print(":" + (char)result[i]);
			  System.out.println();*/

			assertEquals(expected_result.length,result.length);
			for (int i=0;i<expected_result.length;i++) {
				assertEquals((char)expected_result[i],(char)result[i]);
			}
		}

	@Test
		public void testParseAll() throws Exception {
			String filename_1 = "test/data/fasta/FASTA_chrQ.fasta";
			assertTrue(new File(filename_1).exists());
			InputStream istr_1 = new FileInputStream(filename_1);
			assertNotNull(istr_1);

			String filename_2 = "test/data/fasta/FASTA_small_genome.fasta";
			assertTrue(new File(filename_2).exists());
			InputStream istr_2 = new FileInputStream(filename_2);
			assertNotNull(istr_2);

			AnnotatedSeqGroup seq_group = new AnnotatedSeqGroup("test");

			//FastaParser instance = new FastaParser();

			List<BioSeq> seqs = FastaParser.parseAll(istr_1, seq_group);

			assertEquals(1, seqs.size());
			assertEquals(1, seq_group.getSeqCount());

			BioSeq seq = seqs.get(0);
			assertEquals("chrQ", seq.getID());

			seqs = FastaParser.parseAll(istr_2, seq_group);

			assertEquals(3, seqs.size());
			assertEquals(4, seq_group.getSeqCount());

			seq = seqs.get(0);
			assertEquals("gi|5524211|gb|AAD44166.1| cytochrome b [Elephas maximus maximus]", seq.getID());

			seq = seqs.get(1);
			assertEquals("SEQUENCE_1", seq.getID());

			seq = seqs.get(2);
			assertEquals("SEQUENCE_2", seq.getID());
			assertEquals("SATV", seq.getResidues(0,4));
		}

	@Test
		public void testParse() throws Exception {
			String filename = "test/data/fasta/FASTA_chrQ.fasta";
			assertTrue(new File(filename).exists());

			InputStream istr = new FileInputStream(filename);
			assertNotNull(istr);

			BioSeq result = FastaParser.parse(istr);

			assertEquals("chrQ", result.getID());
			assertEquals(33, result.getLength());
			assertEquals("AAAAAAAAAAACCCCCCCCCGGGGGGGGGTTTT", result.getResidues());
			assertEquals("AACCC", result.getResidues(9,9+5));
			assertEquals("GGGTT", result.getResidues(9+5,9));
		}


	@Test
		public void testReadFASTA() throws Exception {
			String filename = "test/data/fasta/FASTA_obey_70.fasta";
			assertTrue(new File(filename).exists());

			char [] expected_fasta = null;
			byte[] fasta = null;
			expected_fasta = "LCLYTHIGRN".toCharArray();
			testFASTASegment(filename, fasta, expected_fasta, 0, 10);

			expected_fasta = "VITNLFSAIPYIGTNLVEWI".toCharArray();
			testFASTASegment(filename, fasta, expected_fasta, 53, 73);

			expected_fasta = "L".toCharArray();
			testFASTASegment(filename, fasta, expected_fasta, 0, 1);

			expected_fasta = null;
			testFASTASegment(filename, fasta, expected_fasta, 3, 3);

			expected_fasta = "LMPFLH".toCharArray();
			testFASTASegment(filename, fasta, expected_fasta, 211, 217);

			expected_fasta = "IENY".toCharArray();
			testFASTASegment(filename, fasta, expected_fasta, 280, 284);

			testFASTASegment(filename, fasta, expected_fasta, 280, 290);

			fasta = FastaParser.readFASTA(new File(filename), 290, 291);
			assertNull(fasta);
		}


	@Test
		public void testReadBadFASTA_1() throws Exception {
			String filename = "test/data/fasta/FASTA_not_obey_70.fasta";
			assertTrue(new File(filename).exists());

			char [] expected_fasta = null;
			byte[] fasta = null;
			expected_fasta = "LCLYTHIGRN".toCharArray();
			try {
				testFASTASegment(filename, fasta, expected_fasta, 10, 0);
			}
			catch(java.lang.IllegalArgumentException ex) {
				return;
			}
			fail("Should throw an IllegalArgumentException");     
		}

	@Test
		public void testReadBadFASTA_2() throws Exception {
			String filename = "test/data/fasta/FASTA_not_obey_70.fasta";
			assertTrue(new File(filename).exists());

			char [] expected_fasta = null;
			byte[] fasta = null;
			expected_fasta = "LCLYTHIGRN".toCharArray();
			try {
				testFASTASegment(filename, fasta, expected_fasta, 10, 100);
			}
			catch(java.lang.AssertionError ex) {
				//System.out.println(ex.toString());
				return;
			}
			fail("Should throw an AssertionError Exception");
		}

	@Test
		public void testSkipFastaHeader() throws Exception {
			String filename = "test/data/fasta/chrC.fasta";
			DataInputStream dis = new DataInputStream(new FileInputStream(filename));
			BufferedInputStream bis = new BufferedInputStream(dis);
			byte[] fasta = FastaParser.skipFASTAHeader(filename, bis);

			//System.out.print("TEST: header is ");
			assertNotNull(fasta);
			/*for (int i =0;i<fasta.length;i++)
				System.out.print((char)fasta[i]);
			System.out.println("");*/
		}


	/**
	 * Test of a certain case I saw when running a query.
	 * This was due to Java not implementing a proper skip() method.
	 */
	@Test
		public void testChrCfailure() throws Exception {
			String filename = "test/data/fasta/chrC.fasta";
			int start=8020,end=8021;
			//System.out.println("ChrCfailure: Testing " + filename + " from [" + start + ":" + end + "]");
			assertTrue(new File(filename).exists());

			byte[] fasta = FastaParser.readFASTA(new File(filename), start, end);
			/*for (int i=0;i<fasta.length;i++)
				System.out.print((char)fasta[i]);
			System.out.println();*/
		}

	/*
	 * Test beginning value out of range.
	 */
	@Test
		public void testChrCfailure2() throws Exception {
			String filename = "test/data/fasta/chrC.fasta";
			int start=200000,end=200001; // file size < 200000.
			//System.out.println("Testing " + filename + " from [" + start + ":" + end + "]");
			assertTrue(new File(filename).exists());

			try {
				FastaParser.readFASTA(new File(filename), start, end);
			}
			catch(java.lang.IllegalArgumentException ex) {
				return;
			}
			fail("Should throw an IllegalArgumentException");

		}

	/**
	 * Test of a certain case I saw when running a query.
	 */
	@Test
		public void testChrC_OK() throws Exception {
			String filename = "test/data/fasta/chrC.fasta";

			char[] expected_fasta = "ATGG".toCharArray();
			byte[] fasta = null;
			testFASTASegment(filename, fasta, expected_fasta, 0, 4);

			expected_fasta = "CCCC".toCharArray();
			testFASTASegment(filename, fasta, expected_fasta, 75, 79);

			expected_fasta = "TACG".toCharArray(); // line 3
			testFASTASegment(filename, fasta, expected_fasta, 79, 83);

			expected_fasta = "CAAA".toCharArray(); // line 4
			testFASTASegment(filename, fasta, expected_fasta, 158, 162);

			expected_fasta = "TCTTT".toCharArray(); // line 103
			testFASTASegment(filename, fasta, expected_fasta, 7979, 7984);

			//expected_fasta = "TTTTTTTCATTTT".toCharArray(); // line 103
			//testFASTASegment(filename, fasta, expected_fasta, 8009, 8022);

			expected_fasta = "CATTTT".toCharArray(); // line 103
			testFASTASegment(filename, fasta, expected_fasta, 8016, 8022);

			expected_fasta = "TT".toCharArray(); // line 103
			testFASTASegment(filename, fasta, expected_fasta, 8020, 8022);
		}

	private void testFASTASegment(String filename, byte[] fasta, char[] expected_fasta, int start, int end) throws IOException {
		//System.out.println("Testing " + filename + " from [" + start + ":" + end + "]");

		DataInputStream dis = new DataInputStream(new FileInputStream(filename));
		BufferedInputStream bis = new BufferedInputStream(dis);
		byte[] header = FastaParser.skipFASTAHeader(filename, bis);
		//assertNotNull(header);
		int header_len = (header == null ? 0 : header.length);

		fasta = FastaParser.readFASTA(new File(filename), start, end);

		if (expected_fasta == null) {
			assertNull(fasta);
			return;
		}

		assertNotNull(fasta);

		//System.out.println("expected, header, actual " + expected_fasta.length + ":" + header_len + ":" + fasta.length);

		assertTrue(end - start >= fasta.length - header_len);

		/*System.out.print("actual:");
		for (int i=header_len;i<fasta.length;i++)
			System.out.print((char)fasta[i]);
		System.out.println();*/

		assertTrue(filename + ": Expected expected_fasta.length (" + expected_fasta.length + ") >= fasta.length (" + fasta.length + ") - header_len (" + header_len + ")", expected_fasta.length >= fasta.length - header_len);

		//System.out.print("testing against expected:");
		for (int i = 0; i < fasta.length - header_len; i++) {
			//System.out.print(expected_fasta[i]);
			assertEquals(expected_fasta[i], (char) fasta[i + header_len]);
		}
		//System.out.println();
	}
}
