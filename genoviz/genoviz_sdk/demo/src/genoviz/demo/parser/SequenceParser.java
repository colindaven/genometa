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

package genoviz.demo.parser;

import java.io.*;
import java.net.*;
import java.util.*;
import com.affymetrix.genoviz.datamodel.Sequence;

/**
 * parses "Helt Format" sequence data.
 * Each line is of the form:
 * id name residues
 * Note that the id fields in this file are meant to correspond
 * to ids in a matching alignment data file.
 * @see AlignmentParser
 */
public class SequenceParser {

	/**
	 * gets and parses data from somewhere on the net.
	 *
	 * @param url locates the data on an internet.
	 * @return a Vector of Sequences.
	 * @see com.affymetrix.genoviz.datamodel.Sequence
	 */
	public static Vector getSequences(URL url) {
		Vector results = null;
		try {
			InputStream istream = url.openStream();
			results = SequenceParser.getSequences(
					new BufferedReader(new InputStreamReader(istream)));
			istream.close();
		}
		catch(IOException ex) {
			System.out.print( "SequenceParser: could not get sequence. " );
			System.out.println(ex.getMessage());
		}
		return results;
	}

	/**
	 * parsers the data coming from a stream of characters.
	 *
	 * @param chars the BufferedReader holding the character stream
	 * @return a Vector of Sequences.
	 * @see com.affymetrix.genoviz.datamodel.Sequence
	 */
	@SuppressWarnings("unchecked")
	public static Vector getSequences (BufferedReader chars) throws IOException {
		Vector results = new Vector();
		StreamTokenizer toks = new StreamTokenizer(chars);
		String line, id, name, residues;
		Sequence seq;
		toks.ordinaryChars('0', '9');
		toks.wordChars('!', '~');
		toks.eolIsSignificant(true);
		id = "";
		name = "";
		residues = "";
		int tok;
		while (StreamTokenizer.TT_EOF != (tok = toks.nextToken())) {
			if (StreamTokenizer.TT_WORD == tok) {
				id = toks.sval;
				tok = toks.nextToken();
				if (StreamTokenizer.TT_WORD == tok) {
					name = toks.sval;
					tok = toks.nextToken();
					if (StreamTokenizer.TT_WORD == tok) {
						residues = toks.sval;
						seq = new Sequence();
						seq.setID(id);
						seq.setName(name);
						seq.setResidues(residues);
						results.addElement(seq);

						//System.out.println("Just added seq: " + seq.getID() + " "
						//                   + seq.getName() + " "
						//                   + seq.getResidues().substring(0,5));
					}
				}
			}
		}
		//    System.out.println("read whole sequence file");
		return results;
	}

}
