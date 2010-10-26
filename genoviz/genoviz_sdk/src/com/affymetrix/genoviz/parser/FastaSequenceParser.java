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

import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.datamodel.Sequence;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses FASTA format data.
 * It will load the sequences in the fasta file
 * into a {@link Sequence} or a Vector of sequences.
 * stripping white space.
 * The sequence identifier, description, and residues are stored.
 * Commentary is discarded.
 *
 * <p> For example:
 * <pre>
 * &gt;seq1 description of sequence
 * ; commentary on the sequence
 *  acgtgactg agtcgtagc tgactgtgac
 *  tgacgtacg tacgtatgc tagctgagct
 * </pre>
 *
 * will be stored with sequence identifier "seq1",
 * description "description of sequence",
 * and residues "acgtgactgagtcgtagctgactgtgactgacgtacgtacgtatgctagctgagct".
 *
 * <p> A Sequence exported to fasta will have the sequence identifier and description
 * prefixed with '&gt;' on one line
 * followed by the residues wrapped into 60 character lines.
 *
 * @author Eric Blossom
 */
public class FastaSequenceParser implements ContentParser {

	/**
	 * imports the data from a stream of bytes.
	 * This is defined as returning an Object
	 * to conform to the ContentParser interface.
	 * However, it actually returns a {@link com.affymetrix.genoviz.datamodel.Sequence}.
	 * To use it as such
	 * you will need to cast the returned Object back to a Sequence.
	 *
	 * @param theInput from whence the data come.
	 * @return a {@link Sequence} or a Vector of them.
	 */
	public Object importContent( InputStream theInput ) throws IOException {
		BufferedReader in;
		in = new BufferedReader( new InputStreamReader( theInput ) );
		Object obj = null;
		try {
			obj = importContent(in);
		}
		finally {
			in.close();
		}
		return obj;
	}


	public Object importContent(BufferedReader in) throws IOException {
		StringBuffer out = new StringBuffer();
		List<SequenceI> v = new ArrayList<SequenceI>();
		String line;

		try {
			while ( null != ( line = in.readLine() )
					&& !line.startsWith( ">" ) ) {
				// Skip to header.
					}
			while ( null != line ) {
				SequenceI seq = new Sequence();
				if ( line.startsWith( ">" ) ) {
					if ( 1 < line.length() ) { // then there is an id.
						int p = line.indexOf( ' ' );
						if ( 0 < p ) { // then there is a description following the Id.
							seq.setID( line.substring( 1, p ) );
							seq.setDescription(line.substring( p ).trim());
						}
						else {
							seq.setID( line.substring(1).trim() );
						}
					}
				}
				while ( null != ( line = in.readLine() )
						&& !line.startsWith( ">" ) ) {
					char[] l = line.toCharArray();
					for ( int i = 0; i < l.length; i++ ) {
						if ( ';' == l[i] ) break; // the rest of the line is a comment.
						if ( ' ' < l[i] ) out.append( l[i] );
					}
						}
				seq.setResidues(out.toString());
				v.add( seq );
				out = new StringBuffer();
			}
		} finally {
			in.close();
		}
		if ( v.size() < 1 ) {
			return null;
		}
		if ( v.size() == 1 ) {
			return v.get( 0 );
		}
		return v;
	}

	/**
	 * Exports sequences to fasta format.
	 *
	 * @param theOutput where the fasta format data are written.
	 * @param o a {@link Sequence} or Vector of them.
	 */
	public void exportContent( OutputStream theOutput, Object o )
		throws IOException
	{
		PrintWriter pw = new PrintWriter(theOutput, true);
		if ( o instanceof Sequence ) {
			exportContent( pw, ( Sequence ) o );
		}
		else if ( o instanceof List ) {
			for (Object ob : (List)o) {
				exportContent( pw, (Sequence) ob );
				pw.println();
			}
		}
		else {
			pw.println(">" + o.toString());
		}
		pw.close();
	}

	/**
	 * Residues are wrapped into lines of this length.
	 */
	private final static int LINE_LENGTH = 60;

	/**
	 * Exports a Sequence to fasta format.
	 *
	 * @param pw where the fasta format data are written.
	 * @param theSequence to be exported.
	 */
	protected void exportContent( PrintWriter pw, Sequence theSequence ) {
		pw.println(">" + theSequence.getID() + " " + theSequence.getDescription());
		String r = theSequence.getResidues();
		int limit = r.length() - LINE_LENGTH;
		int i;
		for (i = 0; i < limit; i += LINE_LENGTH) {
			pw.println(r.substring(i, i+LINE_LENGTH));
		}
		pw.println(r.substring(i));
	}


}
