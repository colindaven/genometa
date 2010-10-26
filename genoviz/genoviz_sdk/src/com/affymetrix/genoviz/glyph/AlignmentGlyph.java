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
package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.bioviews.Scene;
import com.affymetrix.genoviz.datamodel.Mapping;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.datamodel.Sequence;
import com.affymetrix.genoviz.datamodel.NASequence;
import com.affymetrix.genoviz.util.NeoConstants;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Used in NeoAssembler to display gapped sequence alignments.
 * @author Gregg Helt
 */
public class AlignmentGlyph extends AbstractResiduesGlyph
		implements Comparable<AlignmentGlyph> {

	public boolean debugdraw = false;
	protected boolean showGapGlyph = false;
	String res_string = null;
	// efficient build of gapped residues string
	StringBuffer align_buffer;
	boolean align_modified = false;
	AlignedResiduesGlyph uniChild;
	boolean first_span_added = true;
	// is alignment being displayed at minimum zoom?
	// (currently widget must _explicitly_ tell glyph _before_ drawing that it
	//  is being drawn at widget's mimumum zoom
	//    boolean at_min_zoom = false;
	// toggle for whether to force switch from per-base representation to
	// arrows when shown at minimum zoom, regardless of size of
	// alignment and assembly
	//    boolean force_min_arrow = false;
	public static int setResiduesCount;

	/*
	 * children of an AlignmentGlyph are
	 *  AlignedUngappedResidue glyphs -- in other words, a
	 *   aligned sequence with gaps is composed of smaller
	 *   aligned ungapped sequences, interspersed with insertions
	 *     (deletions are just unused residues in the sequence??? --
	 *      well, right now there are no deletions
	 * can also have other child glyphs, and includes support for
	 *    delayed loading of residues for ResiduesGlyphI children
	 */
	private int lastResidueEnd = -1;
	private SequenceI seq;
	private SequenceI reference;
	private Mapping seqToRefMap;
	// These are now inherited from AbstractResiduesGlyph!  Leaving them in
	//   overshadows inherited fields, and causes problems with compareTo()
	//   (and probably other methods too)  GAH 1-9-98
	//  int seq_beg, seq_end;
	// in addition to holding all child glyphs in getChildren(),
	// keeping separate lists for aligned span children and
	// unaligned span children (otherwise there's no real way to
	// distinguish them)
	protected List<AlignedResiduesGlyph> unaligned_spans = new ArrayList<AlignedResiduesGlyph>();
	protected List<AlignedResiduesGlyph> aligned_spans = new ArrayList<AlignedResiduesGlyph>();
	private final ArrowGlyph arrow = new ArrowGlyph();
	/** should the arrow ever be drawn? */
	protected boolean drawArrow = true;
	/** should the arrow always be drawn? */
	protected boolean alwaysDrawArrow = false;
	/** should the unaligned spans (usually trimmed edges) always be drawn? */
	protected boolean alwaysDrawUnalignedSpans = false;
	protected boolean forward = true;
	public static final int UNKNOWN_RESIDUES = 0;
	public static final int NA_RESIDUES = 1;
	public static final int AA_RESIDUES = 2;
	/** color-coding based on residue identity */
	public static final int RESIDUE_BASED =
			AlignedResiduesGlyph.RESIDUE_BASED;
	/** color-coding based on residue comparison to a consensus residue */
	public static final int ALIGNMENT_BASED =
			AlignedResiduesGlyph.ALIGNMENT_BASED;
	/** no color-coding, color is fixed constant */
	public static final int FIXED_COLOR =
			AlignedResiduesGlyph.FIXED_COLOR;
	private int residueType = NA_RESIDUES;
	private boolean complementIfReversed = true;
	private Character match_char = null;

	/**
	 * constructs a glyph for nucleic acid residues.
	 */
	public AlignmentGlyph() {
		this(NA_RESIDUES);
	}

	public AlignmentGlyph(int residueType, int length) {
		this(residueType);
	}

	public AlignmentGlyph(int residueType) {
		this.residueType = residueType;
		setResidueFont(NeoConstants.default_bold_font);
		setDrawOrder(AlignmentGlyph.DRAW_CHILDREN_FIRST);
	}

	@Override
	public void setCoords(double x, double y, double width, double height) {
		super.setCoords(x, y, width, height);
		arrow.setCoords(x, y, width, height);
		arrow.setForward(forward);

		List<GlyphI> subGlyphs = arrow.getChildren();
		if (null != subGlyphs) {
			for (int k = 0; k < subGlyphs.size(); k++) {
				SolidGlyph sg = (SolidGlyph) (subGlyphs.get(k));
				Rectangle2D.Double rect = sg.getCoordBox();
				sg.setCoords(rect.x, y, rect.width, height);
			}
		}
	}

	public void showArrow(boolean b) {
		drawArrow = b;
	}

	/**
	 * indicates whether to draw arrows.
	 * even if higher resolution features are also being drawn.
	 */
	public void setAlwaysDrawArrow(boolean bool) {
		alwaysDrawArrow = bool;
	}

	public boolean getAlwaysDrawArrow(boolean bool) {
		return alwaysDrawArrow;
	}

	/**
	 *  toggle for whether to always draw unaligned spans.
	 * (presumably as edges of alignment),
	 * regardless of whether higher resolution features are also being drawn.
	 */
	public void setAlwaysDrawUnalignedSpans(boolean bool) {
		alwaysDrawUnalignedSpans = bool;
	}

	public boolean getAlwaysDrawUnalignedSpans(boolean bool) {
		return alwaysDrawUnalignedSpans;
	}

	public void setResidues(String residues) {
		SequenceI newseq = new Sequence();
		newseq.setResidues(residues);
		setSequence(newseq);
	}

	public String getResidues() {
		if (null == getSequence()) {
			return null;
		}
		return getSequence().getResidues();
	}

	public void setSequence(SequenceI seq) {
		this.seq = seq;

		if (children == null) {
			return;
		}
		int max = children.size();
		AlignedResiduesGlyph child;
		int seqstart, seqend;
		for (int i = 0; i < max; i++) {
			if (children.get(i) instanceof AlignedResiduesGlyph) {
				child = (AlignedResiduesGlyph) children.get(i);
				seqstart = child.getParentSeqStart();
				seqend = child.getParentSeqEnd();
				setChildResidues(child, seqstart, seqend);
				child.setMatchChar(match_char);
			}
		}
		// expanding damage to ensure this glyph is redrawn if
		//    view is using damage optimizations
		scene.expandDamage(this);
	}

	public void setReference(Sequence reference) {
		if (children == null) {
			return;
		}
		this.reference = reference;
		int max = children.size();
		AlignedResiduesGlyph child;
		for (int i = 0; i < max; i++) {
			if (children.get(i) instanceof AlignedResiduesGlyph) {
				child = (AlignedResiduesGlyph) children.get(i);
				child.setReference(reference);
				child.setMatchChar(match_char);
			}
		}
	}

	public void setMatchChar(Character match_char) {
		this.match_char = match_char;
	}

	public Character getMatchChar() {
		return this.match_char;
	}

	public SequenceI getSequence() {
		return seq;
	}

	/**
	 * Note that this assumes adding based on sequence,
	 * so that it will <em>include</em> the end.
	 * Thus if start = 0, end = 1,
	 * we are really creating a sequence annotation
	 * that starts at 0 and is 2 map units long.
	 */
	public GlyphI addUngappedAlignment(int seqstart, int seqend,
			int refstart, int refend) {
		return addAlignedSpan(seqstart, seqend, refstart, refend);
	}

	public AlignedResiduesGlyph addAlignedSpan(int seqstart, int seqend,
			int refstart, int refend) {

		AlignedResiduesGlyph glyph = null;

		if (residueType == NA_RESIDUES) {
			glyph = new AlignedDNAGlyph();
		} else if (residueType == AA_RESIDUES) {
			glyph = new AlignedProteinGlyph();
		} else {
			glyph = new AlignedResiduesGlyph();
		}
		aligned_spans.add(glyph);
		// This assumes that refstart <= refend always!!!

		addResidueGlyphChild(glyph, seqstart, seqend, refstart, refend);

		// adding glyph to show breaks in arrow glyphs that are large enough to view when zoomed out.
		if (lastResidueEnd == -1) {
			lastResidueEnd = refend + 1;
		} else {
			if (refstart - lastResidueEnd > 0) {
				addResidueGapGlyph(lastResidueEnd, refstart - 1);
			}
			lastResidueEnd = refend + 1;
		}


		if (reference != null) {
			glyph.setReference(reference);
		}

		glyph.setForegroundColor(this.getForegroundColor());
		glyph.setBackgroundColor(this.getBackgroundColor());
		glyph.setMatchChar(match_char);
		return glyph;
	}

	public AlignedResiduesGlyph addUnalignedSpan(int seqstart, int seqend,
			int refstart, int refend) {
		AlignedResiduesGlyph glyph;
		if (residueType == NA_RESIDUES) {
			glyph = new AlignedDNAGlyph();
		} else if (residueType == AA_RESIDUES) {
			glyph = new AlignedProteinGlyph();
		} else {
			glyph = new AlignedResiduesGlyph();
		}
		unaligned_spans.add(glyph);
		glyph.setBackgroundColorStrategy(AlignedResiduesGlyph.FIXED_COLOR);

		addResidueGlyphChild(glyph, seqstart, seqend, refstart, refend);

		if (lastResidueEnd == -1) {
			lastResidueEnd = refend + 1;
		} else {
			if (refstart - lastResidueEnd > 0) {
				addResidueGapGlyph(lastResidueEnd, refstart - 1);
			}
			lastResidueEnd = refend + 1;
		}

		glyph.setMatchChar(match_char);
		return glyph;
	}

	public void setBackgroundColorStrategy(int strategy) {
		List<AlignedResiduesGlyph> glyphs = getAlignedSpans();
		for (AlignedResiduesGlyph arglyph : glyphs) {
			arglyph.setBackgroundColorStrategy(strategy);
		}
	}

	public void setBackgroundColorArray(Color[] col_array) {
		if (null != children) {
			for (GlyphI o : children) {
				if (o instanceof AlignedResiduesGlyph) {
					((AlignedResiduesGlyph) o).setBackgroundColorArray(col_array);
					((AlignedResiduesGlyph) o).redoColors();
				}
			}
		}
	}

	public void setBackgroundColorMatrix(Color[][] col_matrix) {
		if (null != children) {
			for (GlyphI o : children) {
				if (o instanceof AlignedResiduesGlyph) {
					((AlignedResiduesGlyph) o).setBackgroundColorMatrix(col_matrix);
					((AlignedResiduesGlyph) o).redoColors();
				}
			}
		}
	}

	public void setForegroundColorStrategy(int strategy) {
		List<AlignedResiduesGlyph> glyphs = getAlignedSpans();
		for (AlignedResiduesGlyph arglyph : glyphs) {
			arglyph.setForegroundColorStrategy(strategy);
		}
	}

	public void setForegroundColorMatrix(Color[][] col_matrix) {
		if (null != children) {
			for (GlyphI o : children) {
				if (o instanceof AlignedResiduesGlyph) {
					((AlignedResiduesGlyph) o).setForegroundColorMatrix(col_matrix);
					((AlignedResiduesGlyph) o).redoColors();
				}
			}
		}
	}

	public List<AlignedResiduesGlyph> getAlignedSpans() {
		return aligned_spans;
	}

	public List<AlignedResiduesGlyph> getUnalignedSpans() {
		return unaligned_spans;
	}

	@Override
	public void removeChild(GlyphI glyph) {
		aligned_spans.remove(glyph);
		unaligned_spans.remove(glyph);
		super.removeChild(glyph);
	}

	// used to add glyphs representing gaps between sequences that are rendered
	// on top of the arrow glyphs when zoomed out.  (PS 1.24.00)
	public void addResidueGapGlyph(int refstart, int refend) {
		if (showGapGlyph) {
			GlyphI child = new GapGlyph();
			arrow.addChild(child);
			//System.out.println("adding gap glyph");
			child.setCoords((double) (refstart), coordbox.y,
					(double) (refend - refstart), coordbox.height);
			child.setColor(new Color(180, 250, 250));
		}
	}

	public void setShowGapGlyph(boolean state) {
		showGapGlyph = state;
	}

	// where most of the time in setting up gapped spans is spent
	public void addResidueGlyphChild(ResiduesGlyphI child,
			int seqstart, int seqend,
			int refstart, int refend) {
		child.setParentSeqStart(seqstart);
		child.setParentSeqEnd(seqend);
		child.setResidueFont(this.getResidueFont());
		addChild(child);

		// This assumes that refstart <= refend always!!!
		child.setCoords((double) refstart, coordbox.y,
				(double) (refend - refstart + 1), coordbox.height);
		// expand if this grows alignment
		expandIfNeeded(child);
		setChildResidues(child, seqstart, seqend);
	}

	protected void setChildResidues(ResiduesGlyphI child,
			int seqstart, int seqend) {

		if (seq == null) {
			return;
		}
		if (this.isForward()) {
			if (seqstart < 0 || seqend >= seq.getLength()) {
				// would throw IllegalArgumentException here, but then could screw
				//     up adding of rest of spans -- returning silently for now
				// throw new IllegalArgumentException("attempt to reference " +
				//    "non-existent sequence position: " + seqstart + " to " +
				//    seqend + " in " + seq.getName());
				return;

			}
			setResiduesCount++;

			child.setResidues(seq.getResidues().substring(seqstart, seqend + 1));
		} else {  // seqstart > seqend, use reverse complement
			if (seq instanceof NASequence) {
				seqstart = seq.getLength() - 1 - seqstart;
				seqend = seq.getLength() - 1 - seqend;
				if (seqstart < 0 || seqend >= seq.getLength()) {
					// would throw IllegalArgumentException here, but then could screw
					//     up adding of rest of spans -- returning silently for now
					// throw new IllegalArgumentException("attempt to reference " +
					//    "non-existent sequence position: " + seqstart + " to " +
					//    seqend + " in " + seq.getName());
					return;
				}
				if (complementIfReversed) {
					child.setResidues(
							((NASequence) seq).getReverseComplement().substring(seqstart, seqend + 1));
				} else {
					child.setResidues(seq.getResidues(seqstart, seqend + 1));
				}
			} else {
				if (seqend < 0 || seqstart >= seq.getLength()) {
					return;
				}
				if (complementIfReversed) {
					child.setResidues(DNAUtils.reverseComplement(seq.getResidues().substring(seqend, seqstart + 1)));
				} else {
					child.setResidues(seq.getResidues().substring(seqstart, seqend + 1));
				}
			}
		}
	}

	public void setComplementIfReversed(boolean complementIfReversed) {
		this.complementIfReversed = complementIfReversed;
	}

	public boolean getComplementIfReversed(boolean complementIfReversed) {
		return complementIfReversed;
	}

	/**
	 * overriding drawTraversal to provide for semantic zooming.
	 * <em>Note</em>:
	 * drawTraversal does <em>not</em> call AlignmentGlyph.draw().
	 * Rather, it calls arrow.draw() and/or children.drawTraversal() as appropriate.
	 * Therefore need to deal with drawing selection here rather than in draw().
	 */
	@Override
	public void drawTraversal(ViewI view) {
		if (isVisible && coordbox.intersects(view.getCoordBox())) {
			if (debugdraw) {
				System.out.println("now in AlignmentGlyph.drawTraversal(): " + this);
			}
			view.transformToPixels(coordbox, pixelbox);
			double pixels_per_base = pixelbox.width / coordbox.width;
			// if resolution is < 1 pixel/base, just draw as an arrow
			// or if it has no children
			//      if (pixels_per_base < 1 || children == null || children.size() <= 0) {
			// if (pixels_per_base < 1 || children == null || children.size() <= 0) {
			// GAH 4-28-99 modified to draw only arrows if pixels_per_base is not not integral
			if (pixels_per_base < 1 || children == null || children.size() <= 0
					|| ((pixels_per_base - (int) pixels_per_base) != 0)) {
				if (drawArrow) {
					arrow.drawTraversal(view);
				}
				// assuming unaligned spans are NOT transient
				// (should be valid assumption)
				if (alwaysDrawUnalignedSpans) {
					List<AlignedResiduesGlyph> spans = getUnalignedSpans();
					for (int i = 0; i < spans.size(); i++) {
						(spans.get(i)).drawTraversal(view);
					}
				}
				// special-casing outline selection when at low resolution
				if (selected && view.getScene().getSelectionAppearance() == Scene.SELECT_OUTLINE) {
					drawSelectedOutline(view);
				}
				return;
			} else if (alwaysDrawArrow) {
				arrow.drawTraversal(view);
				if (selected && view.getScene().getSelectionAppearance() == Scene.SELECT_OUTLINE) {
					drawSelectedOutline(view);
				}
			}
			// otherwise draw the children (ungapped alignment glyphs)
			super.drawTraversal(view);
		}
	}

	/**
	 * @param otherseq is another sequence to be compared with this one.
	 * @return &lt; 0 if this sequence starts before the other,
	 * or if they start at same position but, this is shorter than the other;
	 * 0 if both are of the same size and location;
	 * &gt; 0 otherwise.
	 */
	public int compareTo(AlignmentGlyph otherseq) {
		if (seq_beg != otherseq.seq_beg) {
			return ((Integer) seq_beg).compareTo(otherseq.seq_beg);
		}
		return ((Integer) seq_end).compareTo(otherseq.seq_end);
	}

	@Override
	public boolean hit(Rectangle pixel_hitbox, ViewI view) {
		calcPixels(view);
		return isVisible && pixel_hitbox.intersects(pixelbox);
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
		return isVisible && coord_hitbox.intersects(coordbox);
	}

	@Override
	public void setSelected(boolean selected) {
		super.setSelected(selected);
		arrow.setSelected(selected);
		if (children != null) {
			int size = children.size();
			for (int i = 0; i < size; i++) {
				children.get(i).setSelected(selected);
			}
		}
	}

	public void setForward(boolean forward) {
		this.forward = forward;
		if (arrow != null) {
			arrow.setForward(forward);
		}
	}

	public boolean isForward() {
		return this.forward;
	}

	public void setBackgroundColor(Color c) {
		super.setBackgroundColor(c);
		arrow.setBackgroundColor(c);
		List<AlignedResiduesGlyph> vec = getAlignedSpans();
		for (GlyphI gl : vec) {
			gl.setColor(c);
		}
	}

	@Override
	public void setForegroundColor(Color c) {
		super.setForegroundColor(c);
		if (null != children) {
			for (GlyphI o : children) {
				if (o instanceof AlignedResiduesGlyph) {
					((AlignedResiduesGlyph) o).setForegroundColor(c);
				}
			}
		}
	}

	public void setMapping(Mapping m) {
		seqToRefMap = m;
	}

	public Mapping getMapping() {
		return seqToRefMap;
	}

	/**
	 * Need to override setScene()
	 * to make sure arrowglyph gets its scene set properly.
	 */
	@Override
	public void setScene(Scene s) {
		super.setScene(s);
		arrow.setScene(s);
	}

	/**
	 * expands the AlignmentGlyph if child extends the alignment.
	 */
	protected void expandIfNeeded(GlyphI child) {
		Rectangle2D.Double childbox = child.getCoordBox();
		double oldend = coordbox.x + coordbox.width;
		double newend = childbox.x + childbox.width;
		if (childbox.x < coordbox.x || newend > oldend) {
			double newx = Math.min(childbox.x, coordbox.x);
			double newwidth = Math.max(oldend, newend) - newx;
			setCoords(newx, coordbox.y, newwidth, coordbox.height);
		}
	}

	/*
	 * WARNING: We stub out these methods
	 * just to satisfy ResiduesGlyphI interface.
	 */
	/** @exception IllegalArgumentException. */
	public void setParentSeqStart(int beg) {
		throw new IllegalArgumentException("AlignmentGlyph.setParentSeqStart() "
				+ "should not be called -- only exists to satisfy ResiduesGlyphI "
				+ "interface");
	}

	/** @exception IllegalArgumentException. */
	public void setParentSeqEnd(int end) {
		throw new IllegalArgumentException("AlignmentGlyph.setParentSeqEnd() "
				+ "should not be called -- only exists to satisfy ResiduesGlyphI "
				+ "interface");
	}

	/** @exception IllegalArgumentException. */
	public int getParentSeqStart() {
		throw new IllegalArgumentException("AlignmentGlyph.getParentSeqStart() "
				+ "should not be called -- only exists to satisfy ResiduesGlyphI "
				+ "interface");
	}

	/** @exception IllegalArgumentException. */
	public int getParentSeqEnd() {
		throw new IllegalArgumentException("AlignmentGlyph.getParentSeqEnd() "
				+ "should not be called -- only exists to satisfy ResiduesGlyphI "
				+ "interface");
	}
}
