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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

/**
 * defines methods for converting between a stream of bytes
 * and a "data model".
 * The stream of bytes is generally going to (or coming from)
 * an external representation of the model.
 *
 * <p> The two methods should be inverses of each other.
 *
 * <p> The data model can be any object
 * so that this interface can be reused by parsers
 * for differnt pairs of external formats and data models.
 * A class implementing this interface will likely want
 * to have these methods delegate to more type-safe methods
 * the real work. e.g.
 * <p><pre>
 * public Object importContent(InputStream theInput) {
 *   return importSequenceContent(theInput);
 * }
 * protected Sequence importSequenceContent(InputStream theInput) {
 *   ...
 * }
 * </pre></p>
 *
 * <p> Note that this differs from Object serialization.
 * The external format can be a standard format like genbank.
 * A parser can be written for each of two different data models.
 * Each parser may be interested in a different subset of the data
 * in the input.
 *
 * @author Eric Blossom
 */
public interface ContentParser {

	/**
	 * reads a stream of bytes and parses it
	 * to build a data model.
	 *
	 * @param theInput the stream of bytes to parse.
	 * @return the data model.
	 */
	public Object importContent(InputStream theInput)
		throws IOException;

	/**
	 * builds an external representation of the data model
	 * and writes it to the stream of bytes.
	 *
	 * <p> This external representation should be parseable
	 * by <code>importContent()</code>.
	 *
	 * @param o the data model.
	 * @param theOutput the stream to write to.
	 * @see #importContent
	 */
	public void exportContent(OutputStream theOutput, Object o)
		throws IOException;

}
