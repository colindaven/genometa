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

package com.affymetrix.genoviz.bioviews;

import java.util.Hashtable;

/**
 * a factory for producing glyphs affected by a data model.
 * Generaly,
 * objects implementing this interface will have a MapGlyphFactory
 * behind the scenes.
 * Hence,
 * it is an adapter between that factory
 * and the object receiving the manufactured glyphs.
 *
 * @see MapGlyphFactory
 */
public interface NeoDataAdapterI {

	/**
	 * configures the factory to produce characteristic glyphs.
	 * The glyph's options are set to match those in the Hashtable
	 * <p> This will typically be delegated to the MapGlyphFactory.
	 * However,
	 * this is also a good place to add options
	 * not handled by MapGlyphFactory.
	 *
	 * @see MapGlyphFactory#configure(Hashtable)
	 */
	public void configure(Hashtable options);

	/**
	 * configures the factory to produce characteristic glyphs.
	 * The glyph's options are set to match those in the String.
	 * @see MapGlyphFactory#configure(String)
	 */
	public void configure(String options);

	/**
	 * creates a glyph possibly changing settings
	 * after consulting with the data model
	 *
	 * @param obj the data model
	 * @return the glyph created
	 */
	public GlyphI createGlyph(Object obj);

	public void setScene(Scene s);

	/**
	 * declares whether or not a type of data model can be used
	 * to create glyphs.
	 *
	 * @param obj a candidate data model
	 * @return true iff the implementor's createGlyph method
	 * can accept ojects of this type.
	 */
	public boolean accepts(Object obj);

}
