/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometryImpl.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author sgblanch
 */
public class SpeciesLookupTest {

	private static final String c_brenneri = "Caenorhabditis brenneri";
	private static final String m_musculus = "Mus musculus";
	private static final String p_pygmaeus_abelii = "Pongo pygmaeus abelii";

    public SpeciesLookupTest() {
    }

	@BeforeClass
	public static void setUpClass() throws Exception {
	}

	@AfterClass
	public static void tearDownClass() throws Exception {
	}

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

	/**
	 * Test of getSpeciesName method, of class SpeciesLookup.
	 *
	 * @throws IOException
	 */
	@Test
	public void testGetSpeciesName() throws IOException {
		String filename = "test/data/speciesLookup/species.txt";
		assertTrue(new File(filename).exists());

		InputStream istr = new FileInputStream(filename); 
		assertNotNull(istr);

		SpeciesLookup.load(istr);

		String version = "C_brenneri_Aug_2009";
		String result = SpeciesLookup.getSpeciesName(version, true);
		assertEquals(c_brenneri, result);

		version = "caePb9";
		result = SpeciesLookup.getSpeciesName(version, true);
		assertEquals(c_brenneri, result);

		version = "c_Brenneri_Aug_2009";
		result = SpeciesLookup.getSpeciesName(version, true);
		assertEquals(version, result);
		result = SpeciesLookup.getSpeciesName(version, false);
		assertEquals(c_brenneri, result);

		version = "caepb9";
		result = SpeciesLookup.getSpeciesName(version, true);
		assertEquals(version, result);
		result = SpeciesLookup.getSpeciesName(version, false);
		assertEquals(c_brenneri, result);

		version = "mm9";
		result = SpeciesLookup.getSpeciesName(version, true);
		assertEquals(m_musculus, result);

		version = "MM9";
		result = SpeciesLookup.getSpeciesName(version, true);
		assertEquals(version, result);
		result = SpeciesLookup.getSpeciesName(version, false);
		assertEquals(m_musculus, result);

		version = "ponAbe9";
		result = SpeciesLookup.getSpeciesName(version, true);
		assertEquals(p_pygmaeus_abelii, result);

		version = "ponabe9";
		result = SpeciesLookup.getSpeciesName(version, true);
		assertEquals(version, result);
		result = SpeciesLookup.getSpeciesName(version, false);
		assertEquals(p_pygmaeus_abelii, result);
	}

}