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

package com.affymetrix.genoviz.widget.neoassembler;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.AlignmentGlyph;
import java.util.*;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class AssemblyPacker implements PackerI {

	protected int spacing;

	public AssemblyPacker() {
		spacing = 1;
	}

	public void setSpacing(int spacing) {
		this.spacing = spacing;
	}

	public int getSpacing() {
		return spacing;
	}

	public Rectangle pack( GlyphI assembly, GlyphI seq, ViewI view) {
		if (seq instanceof AlignmentGlyph) {
			return pack(assembly, (AlignmentGlyph)seq, view);
		}
		return null;
	}

	public Rectangle pack(GlyphI assembly, AlignmentGlyph seq, ViewI view) {
		if (assembly == null || seq == null || view == null) {
			// **** throw an exception here? ****
			return null;
		}
		List alignments = assembly.getChildren();
		if (alignments == null) { return null; }
		int position = alignments.size();
		if (seq.getParent() == assembly) {
			position--;
		}
		pack(assembly, seq, position);
		return null;
	}


	protected void pack(GlyphI assembly, AlignmentGlyph seq, int position) {
		Rectangle2D.Double assemblyBox = assembly.getCoordBox();
		Rectangle2D.Double seqBox = seq.getCoordBox();
		double offset = assemblyBox.y + (position * (seqBox.height + spacing));
		if (seq.isForward()) {
			seq.setCoords(seqBox.x, offset, seqBox.width, seqBox.height);
		}
		else {
			seq.setCoords(seqBox.x+seqBox.width, offset,
					-seqBox.width, seqBox.height);
		}
		// this should probably be handled by assemblyBox.propogateStretch()
		// or similar method... GAH 12-6-97
		assemblyBox.add(seq.getCoordBox());
	}

	// this forces a stretch of the assembly to hold all alignments!
	// Also assumes that alignments have already been sorted in
	//     whatever order you prefer, with first --> last in assembly
	//     child vector mapping to top --> bottom in display
	public Rectangle pack(GlyphI assembly, ViewI view) {
		if (assembly == null || view == null) { return null; }
		List alignments = assembly.getChildren();
		Rectangle2D.Double prevbox = assembly.getCoordBox();
		assembly.setCoords(prevbox.x, prevbox.y, prevbox.width, 0);

		if (alignments == null) { return null; }
		AlignmentGlyph align;
		for (int i=0; i<alignments.size(); i++) {
			align = (AlignmentGlyph)alignments.get(i);
			pack(assembly, align, i);

		}
		return null;
	}


}
