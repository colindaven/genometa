/*
 * GFF3SymTest.java
 * JUnit based test
 *
 * Created on August 24, 2006, 10:28 AM
 */
package com.affymetrix.genometryImpl;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class GFF3SymTest {

	public GFF3SymTest() {
	}

	@Before
		public void setUp() {
		}

	@After
		public void tearDown() {
		}

	/*
	   @Test
	   public void testGetSource() {
	   fail("test not implemented");
	   }

	   @Test
	   public void testGetFeatureType() {
	   fail("test not implemented");
	   }

	   @Test
	   public void testGetScore() {
	   fail("test not implemented");
	   }

	   @Test
	   public void testGetFrame() {
	   fail("test not implemented");
	   }

	   @Test
	   public void testGetAttributes() {
	   fail("test not implemented");
	   }

	   @Test
	   public void testGetProperty() {
	   fail("test not implemented");
	   }

	   @Test
	   public void testSetProperty() {
	   fail("test not implemented");
	   }

	   @Test
	   public void testGetProperties() {
	   fail("test not implemented");
	   }

	   @Test
	   public void testCloneProperties() {
	   fail("test not implemented");
	   }
	   */

	@Test
		public void testGetIdFromGFF3Attributes() {
			assertEquals("test1", GFF3Sym.getIdFromGFF3Attributes("test1;test2=foo;ID=test1"));
			assertEquals("test2", GFF3Sym.getIdFromGFF3Attributes("ID=test2"));
			assertEquals("test3", GFF3Sym.getIdFromGFF3Attributes("test1;test2=foo;ID=test3;"));
			assertEquals("test4", GFF3Sym.getIdFromGFF3Attributes("test1;;test2=foo;ID=test4;;;"));
			assertEquals("test5", GFF3Sym.getIdFromGFF3Attributes("test1;test2=foo;ID=test5;animals=cow,dog,rat"));
			assertEquals(null, GFF3Sym.getIdFromGFF3Attributes("test1;test2=foo;NotTheID=this is not the id;animals=cow,dog,rat"));
			assertEquals(null, GFF3Sym.getIdFromGFF3Attributes("test1;test2=foo;animals=cow,dog,rat"));
			assertEquals("This has&some special$characters", GFF3Sym.getIdFromGFF3Attributes("test1;ID=This%20has%26some+special%24characters;foo=bar"));
			String example = "ID=NC_000964.2:yaaA:unknown_transcript_1;Parent=NC_000964.2:yaaA;locus_tag=BSU00030;function=unknown;transl_table=11;product=hypothetical%20protein;protein_id=NP_387884.1;db_xref=GOA:P05650;db_xref=UniProtKB%2FSwiss-Prot:P05650;db_xref=GI:16077071;db_xref=GeneID:939444;exon_number=1";
			assertEquals("NC_000964.2:yaaA:unknown_transcript_1", GFF3Sym.getIdFromGFF3Attributes(example)); 
		}

	/*
	   @Test
	   public void testAddAllAttributesFromGFF3() {
	   fail("test not implemented");
	   }

	   @Test
	   public void testGetGFF3PropertyFromAttributes() {
	   fail("test not implemented");
	   }
	   */
}
