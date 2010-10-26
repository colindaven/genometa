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
import java.awt.*;
import com.affymetrix.genoviz.bioviews.*;
import genoviz.demo.datamodel.ScoredAnnotationI;

/**
 * a data adapter that colors glyphs based on the score of an annotation
 * like AnnotationAdapter, most of the work is done by a MapGlyphFactory 
 * MapGlyphFactory is not necessarily well suited for this purpose, but 
 *    they are adequate
 */
public class ScoredAnnotAdapter implements NeoDataAdapterI {

	protected MapGlyphFactory factory;
	protected Color col;

	/** need a Scene to set up MapGlyphFactory (which needs to know 
	 *  which scene it is building glyphs for, in order to pack the 
	 *   glyph relative to other glyphs [actually, should really need to 
	 *   know the View that it should base its packing on, but for now 
	 *   we consider the simpler case where it packs to the first view 
	 *   of the scene])
	 */
	protected Scene scene;

	public ScoredAnnotAdapter() {
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
		return (obj instanceof ScoredAnnotationI);
	}

	// note that scene must be set, or factory will return null if packing 
	//     is needed
	public GlyphI createGlyph(Object obj) {
		ScoredAnnotationI annot = (ScoredAnnotationI)obj;
		float start = (float)annot.getStart();
		float end = (float)annot.getEnd();

		int grayscale = (int)(annot.getScore()*255);
		grayscale = grayscale%256;
		if (grayscale < 0) { 
			grayscale = -grayscale;
		}
		col = new Color(grayscale, grayscale, grayscale);
		// Alternatively, set color using HSB color model
		//    double grayscale = annot.getScore()/256;
		//    col = Color.getHSBColor(0.9f, 1.0f, (float)grayscale);
		factory.setColor(col);
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
