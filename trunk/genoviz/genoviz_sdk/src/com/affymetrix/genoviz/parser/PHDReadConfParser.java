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

import com.affymetrix.genoviz.datamodel.ReadConfidence;
import com.affymetrix.genoviz.datamodel.BaseConfidence;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

import java.util.StringTokenizer;

/**
 * converts between a PHD format file and a {@link ReadConfidence} data model.
 *
 * @author Eric Blossom
 */
public class PHDReadConfParser implements ContentParser {

	/**
	 * loads the first sequence in the PHD file into a ReadConfidence.
	 *
	 * @param theInput from whence the data come.
	 * @return a {@link ReadConfidence}.
	 */
	public Object importContent( InputStream theInput ) throws IOException {
		BufferedReader in;
		ReadConfidence conf = new ReadConfidence();

		in = new BufferedReader( new InputStreamReader( theInput ) );
		try {
			String line;
			while ( null != ( line = in.readLine() )
					&& !line.startsWith( "BEGIN_DNA" ) ) {
				// Skip header.
					}
			while ( null != ( line = in.readLine() )
					&& !line.startsWith( "END_DNA" ) ) {
				BaseConfidence bc = importContent(line);
				conf.addBaseConfidence( bc );
					}
		} finally {
			in.close();
		}
		return conf;
	}

	/**
	 * parses a single line
	 * building a BaseConfidence.
	 */
	private BaseConfidence importContent(String theLine) {
		BaseConfidence bc = null;
		StringTokenizer line = new StringTokenizer(theLine);
		if (line.hasMoreTokens()) {
			char base = line.nextToken().charAt(0);
			if (line.hasMoreTokens()) {
				int conf = Integer.parseInt(line.nextToken());
				if (line.hasMoreTokens()) {
					int point = Integer.parseInt(line.nextToken());
					bc = new BaseConfidence(base, conf, point);
				}
				else {
					bc = new BaseConfidence(base, conf);
				}
			}
		}
		return bc;
	}

	/**
	 * is not yet implemented.
	 */
	public void exportContent( OutputStream theOutput, Object o )
		throws IOException {
	}

}
