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
import com.affymetrix.genoviz.datamodel.*;
import com.affymetrix.genoviz.glyph.AlignmentGlyph;
import com.affymetrix.genoviz.widget.NeoAssembler;
import genoviz.demo.datamodel.Assembly;

public class AssemblyAdapter implements NeoDataAdapterI {

	protected NeoAssembler map;

	/**
	 * Need a constructor that takes a NeoAssembler as a constructor
	 * this is really just a usability convenience
	 */
	public AssemblyAdapter(NeoAssembler map) {
		this.map = map;
	}

	public void configure(String options) {

	}

	public void configure(Hashtable options)  {

	}

	public boolean accepts(Object obj) {
		return (obj instanceof Assembly);
	}

	/**
	 * For NeoAssemblyAdapter, multiple glyphs on multiple "inner"
	 * scenes are being created.  Therefore createGlyph returns
	 * null, and the addition of glyphs to the assembly is
	 * considered a side effect
	 */
	public GlyphI createGlyph(Object obj) {
		if (!(obj instanceof Assembly)) {
			return null;
		}
		Assembly assem = (Assembly)obj;

		// temp variables to help extract assembly info from data models
		List aligns;
		Mapping align;
		List spans;
		Span sp;
		GlyphI seqtag, labeltag;
		String name;
		int start, end;
		AlignTrace trace;

		//--------- setting up consensus map info ---------
		Mapping consensus = assem.getConsensus();
		spans = consensus.getSpans();
		start = ((Span)spans.get(0)).ref_start;
		end = ((Span)spans.get(spans.size()-1)).ref_end;
		seqtag = map.setConsensus(start, end,
				consensus.getSequence().getResidues());
		map.setDataModel(seqtag, consensus);
		for (int j=0; j<spans.size(); j++) {
			sp = (Span)spans.get(j);
			map.addAlignedSpan(seqtag, sp.seq_start, sp.seq_end,
					sp.ref_start, sp.ref_end);
		}

		//----------- setting up alignment map info ------------
		aligns = assem.getAlignments();
		for (int i=1; i<aligns.size(); i++) {
			align = (Mapping)aligns.get(i);
			spans = align.getSpans();
			start = ((Span)spans.get(0)).ref_start;
			end = ((Span)spans.get(spans.size()-1)).ref_end;
			if (!align.isForward()) {
				int temp = start;
				start = end;
				end = temp;
			}
			name = align.getSequence().getName();
			seqtag = map.addSequence(start, end);
			labeltag = map.setLabel(seqtag, name);
			map.setResidues(seqtag, align.getSequence().getResidues());

			trace = assem.getTrace(align.getID());
			if (trace != null) {
				if (trace.isFlipped()) {
					((AlignmentGlyph)seqtag).setForward(false);
				}
			}
			map.setDataModel(seqtag, align);
			map.setDataModel(labeltag, align);
			for (int j=0; j<spans.size(); j++) {
				sp = (Span)spans.get(j);
				map.addAlignedSpan(seqtag, sp.seq_start, sp.seq_end,
						sp.ref_start, sp.ref_end);
			}
		}
		return null;
	}

	/**
	 * need to maintain setScene for backward compatibility with
	 * NeoDataAdapterI interface
	 */
	public void setScene(Scene scene) {
		return;
	}

}
