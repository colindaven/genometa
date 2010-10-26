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
import com.affymetrix.genoviz.glyph.*;
import genoviz.demo.datamodel.SequenceAnnotationI;

public class SequenceAnnotAdapter implements NeoDataAdapterI {
	protected MapGlyphFactory factory;
	protected Scene scene;

	public SequenceAnnotAdapter() {
		factory = new MapGlyphFactory();
		factory.configure("-glyphtype SequenceGlyph -width 16");
	}

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
		return (obj instanceof SequenceAnnotationI);
	}

	// note that scene must be set, or factory will return null if packing 
	//     is needed
	public GlyphI createGlyph(Object obj) {
		if (!(obj instanceof SequenceAnnotationI)) {
			// should call error handler here
			return null;
		}
		SequenceAnnotationI annot = (SequenceAnnotationI)obj;
		float start = (float)annot.getStart();
		float end = (float)annot.getEnd();
		String residues = annot.getResidues();

		GlyphI gl;
		if (start <= end) {
			gl = factory.makeGlyph(start, end+1.0f);
		}
		else {
			gl = factory.makeGlyph(start+1.0f, end);
		}
		((SequenceGlyph)gl).setResidues(residues);
		return gl;
	}

}
