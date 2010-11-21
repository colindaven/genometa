/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
 *    
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.  
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genoviz.parser;

import com.affymetrix.genoviz.datamodel.BaseConfidence;
import com.affymetrix.genoviz.datamodel.ReadConfidence;
import java.net.*;
import java.io.*;
import java.util.StringTokenizer;

/**
 * parses output from phred.
 *
 * <p> The bases called are expected to be in a fasta format file.
 * It will look something like this:
 * <pre>
 * &gt;LD34.15    861      0    861  ABI
 * GGTGGCTGGCTANAGTCATGTATCCACGGGCTGCAGGCTTCCCGCAAACG
 * TATACTGTCGGAAACAGTTTTTTCATTAACACAAAAAAAGGCATCCAGAG
 * CATTTGCTGTAGCACAAAAACATCGAATTTCCAACGCTACCAGGTAAACC
 * AGTCGCCGCCGAACCGCCGGAAAAACGGGCTTTCCAGTTCGTCTTTAGCG
 * </pre>
 * <p> The quality scores are expected to be in another file
 * whose format looks like this:
 * <pre>
 * &gt;LD34.15    861      0    861  ABI
 * 4 4 4 4 4 6 6 4 4 6 18 4 4 4 6 6 4 6 6 7 7 13 10 7 
 * 7 9 9 17 21 31 33 36 33 33 21 20 10 15 16 11 8 8 9 
 * 21 16 26 24 26 16 16 6 6 6 6 6 14 17 21 19 13 10 18 
 * 16 21 21 31 22 27 27 32 45 34 34 34 36 34 37 40 37 
 * </pre>
 *
 * <p> The base calls and quality scores must correspond.
 * The number of calls and quality scores in the files must match.
 *
 * @author Cyrus Harmon
 * @author Eric Blossom
 */
public class PhredParser
{

	BufferedReader fastaDataIn;
	BufferedReader qualDataIn;

	/**
	 * constructs a ReadConfidence data model
	 * from phred output parsed
	 * from over an internet.
	 *
	 * @param fastaURL whence the fasta format sequence data come.
	 * @param qualURL whence the corresponding quality scores come.
	 */
	public ReadConfidence parseFiles ( URL fastaURL, URL qualURL ) {

		try {

			InputStream fastain = fastaURL.openStream();
			fastaDataIn = new BufferedReader( new InputStreamReader( fastain ) );

			InputStream qualin = qualURL.openStream();
			qualDataIn = new BufferedReader( new InputStreamReader( qualin ) );

		} catch (MalformedURLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		String fastaLine = new String();
		String qualLine = new String();
		StringTokenizer fastaST;
		StringTokenizer qualST;
		BaseConfidence baseConf;
		ReadConfidence readConf = new ReadConfidence();

		try {
			// First skip the fasta ">" tag lines
			do {
				fastaLine = fastaDataIn.readLine();
			} while ( fastaLine.startsWith ( ">" ) );
			do {
				qualLine = qualDataIn.readLine();
			} while ( qualLine.startsWith ( ">" ) );

			qualST = new StringTokenizer( qualLine, " \n", false );
			do {
				fastaST = new StringTokenizer( fastaLine, "acgtnACGTN", true );
				do {
					char base = fastaST.nextToken().charAt( 0 );
					if ( ! qualST.hasMoreTokens() ) {
						qualLine = qualDataIn.readLine();
						qualST = new StringTokenizer( qualLine, " \n", false );
					}
					int conf = Integer.parseInt( qualST.nextToken() );
					baseConf = new BaseConfidence( base, conf );
					readConf.addBaseConfidence( baseConf );
				} while ( fastaST.hasMoreTokens() ) ;

				fastaLine = fastaDataIn.readLine();
			} while ( (fastaLine != null)
					&& ! fastaLine.startsWith ( ">" )
					&& ! qualLine.startsWith ( ">" ) );
		}
		catch ( IOException e ) {
			System.out.println( e.getMessage() );
			e.printStackTrace();
		}

		return readConf;

	}

}
