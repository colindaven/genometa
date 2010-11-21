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
import com.affymetrix.genoviz.datamodel.*;

/**
 * parses "Helt Format" sequence alignment data.
 * Each line is of the form:
 * <pre>
 * id name { 0, 1 } seqstart seqend refstart refend [ sstart send rstart rend ... ]
 * </pre>
 * These fields are respectively:
 * <dl>
 * <dt> id:       <dd>an identifying string for a sequence
 * <dt> name:     <dd>a name for the sequence
 * <dt> {0,1}     <dd>an indication of which strand
 *                  ( 0 for forward, 1 for reverse ).
 * <dt> seqstart: <dd>the start of a "span" on the sequence.
 * <dt> seqend:   <dd>the end of a "span" on the sequence.
 * <dt> refstart: <dd>the point in the reference space
 *                  ( generally a position in a consensus sequence )
 *                    that corresponds to seqstart ( for lining up ).
 * <dt> refend: <dd>the point in the reference space
 *                  that corresponds to seqend
 * </dl>
 * <p>
 * Note that the id fields in this file are meant to correspond
 * to ids in a matching sequence data file.
 * @see SequenceParser
 * <p>
 * The aligment data is collected into a set of Mappings.
 * Each Mapping refers to a Sequence
 * and a collection of Spans of that sequence.
 * Each Span models a contiguous portion of a sequence
 * that corresponds to a contiguous portion of some reference space.
 * Often, that reference space is a consensus sequence.
 * @see com.affymetrix.genoviz.datamodel
 */
public class AlignmentParser {

	// Methods to get alignments
	// without needing to know hash of sequence ids to seq,
	// so the the Mapping's Sequence can be set later.

	/**
	 * gets the alignment data from the net.
	 *
	 * @param url from whence the data come.
	 * @return a collection of Mappings
	 * @see com.affymetrix.genoviz.datamodel.Mapping
	 */
	public static Vector getAlignments(URL url)  {
		return getAlignments(url, null);
	}

	/**
	 * gets the alignment data by parsing a stream of characters.
	 *
	 * @param chars the BufferedReader holding the character stream
	 * @return a collection of Mappings
	 * @see com.affymetrix.genoviz.datamodel.Mapping
	 */
	public static Vector getAlignments(BufferedReader chars) {
		Vector results = null;
		try {
			results = getAlignments(chars, null);
		}
		catch(IOException ex) {
			System.out.print( "AlignmentParser: could not get data. " );
			System.out.println(ex.getMessage());
		}
		return results;
	}


	/**
	 * gets the alignment data from the net.
	 *
	 * @param url from whence the data come.
	 * @param seqhash a collection of matching sequence data
	 *                keyed by the same ids as are in the alignment data
	 *                to be parsed.
	 * @return a collection of Mappings
	 * @see com.affymetrix.genoviz.datamodel.Mapping
	 */
	public static Vector getAlignments(URL url, Hashtable seqhash) {
		Vector results = null;
		try {
			BufferedReader chars = new BufferedReader(new InputStreamReader(url.openStream()));
			results = AlignmentParser.getAlignments(chars, seqhash);
			chars.close();
		}
		catch(IOException ex) {
			System.out.print( "AlignmentParser: could not get data. " );
			System.out.println(ex.getMessage());
		}
		return results;
	}

	/**
	 * gets the alignment data by parsing a stream of bytes.
	 *
	 * @param chars the BufferedReader holding the character stream
	 * @param seqhash a collection of matching sequence data
	 *                keyed by the same ids as are in the alignment data
	 *                to be parsed.
	 * @return a collection of Mappings
	 * @see com.affymetrix.genoviz.datamodel.Mapping
	 */
	@SuppressWarnings("unchecked")
	public static Vector getAlignments(BufferedReader chars, Hashtable seqhash) throws IOException {
		Vector results = new Vector();
		StringTokenizer toks;
		String line, id, name;
		int seq_start, seq_end, ref_start, ref_end;
		boolean direction;
		Mapping align;
		Span sp;
		line = chars.readLine();
		// readLine() should return null at EOF, so loop until this happens
		while (line != null) {
			toks = new StringTokenizer(line);
			// check if tokenizer has any elements -- if not, its a blank line, 
			//     so skip
			if (toks.hasMoreElements())  {
				id = toks.nextToken();
				// name is currently not used in Mapping, but still being parsed
				name = toks.nextToken();
				direction = (Integer.parseInt(toks.nextToken()) > 0);
				align = new Mapping();
				align.setID(id);
				align.setDirection(direction);
				if (seqhash != null) {
					align.setSequence((Sequence)seqhash.get(id));
				}
				while (toks.hasMoreElements()) {
					seq_start = Integer.parseInt(toks.nextToken());
					seq_end = Integer.parseInt(toks.nextToken());
					ref_start = Integer.parseInt(toks.nextToken());
					ref_end = Integer.parseInt(toks.nextToken());
					sp = new Span(seq_start,seq_end,ref_start,ref_end); 
					align.addSpan(sp);
				}
				results.addElement(align);
			}
			line = chars.readLine();
		}
		return results;
	}

}
