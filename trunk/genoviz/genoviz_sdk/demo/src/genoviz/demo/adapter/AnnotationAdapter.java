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

package genoviz.demo.adapter;

import java.util.*;
import com.affymetrix.genoviz.bioviews.*;
import genoviz.demo.datamodel.AnnotationI;

/**
 * an almost trivial example of a DataAdapter
 * most of the work is done by a MapGlyphFactory
 * the one thing this adapter knows is that the datamodel objects implement
 * the AnnotationI interface, and therefore can create the proper glyph and
 * add it to the map by:
 *     MapGlyphFactory.makeGlyph(AnnotationI.getStart(), AnnotationI.getEnd());
 */
public class AnnotationAdapter implements NeoDataAdapterI {

	protected MapGlyphFactory factory;

	/** need a Scene to set up MapGlyphFactory (which needs to know
	 *  which scene it is building glyphs for, in order to pack the
	 *   glyph relative to other glyphs [actually, should really need to
	 *   know the View that it should base its packing on, but for now
	 *   we consider the simpler case where it packs to the first view
	 *   of the scene])
	 */
	protected Scene scene;

	public AnnotationAdapter() {
		factory = new MapGlyphFactory();
	}

        public MapGlyphFactory getGlyphFactory()  { return factory; }

	public void setScene(Scene scene) {
		this.scene = scene;
		factory.setScene(scene);
	}

	public void configure(String options) {
		factory.configure(options);
	}

	@SuppressWarnings("unchecked")
	public void configure(Hashtable options)  {
		factory.configure(options);
	}

	public boolean accepts(Object obj) {
		return (obj instanceof AnnotationI);
	}

	// note that scene must be set, or factory will return null if packing
	//     is needed
	public GlyphI createGlyph(Object obj) {
		AnnotationI annot = (AnnotationI)obj;
		float start = (float)annot.getStart();
		float end = (float)annot.getEnd();

		GlyphI gl;
		if (start <= end) {
			gl = factory.makeGlyph(start, end+1.0f);
		}
		else {
			gl = factory.makeGlyph(start+1.0f, end);
		}
		return gl;
	}

}
