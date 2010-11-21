package com.affymetrix.igb.glyph;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import java.awt.Graphics;
import java.awt.Rectangle;

  /** A small x-shaped glyph that can be used to indicate a deleted exon
   * in the slice view.
   */
public final class DeletionGlyph extends SolidGlyph {
  

  /** Draws a small "X". */
  @Override
  public void draw(ViewI view) {
    Rectangle pbox = view.getScratchPixBox();
    view.transformToPixels(this.coordbox, pbox);
    Graphics g = view.getGraphics();

    //pixelbox.width = Math.max( pixelbox.width, min_pixels_width );
    pbox.height = Math.max( pbox.height, min_pixels_height );

    final int half_height = pbox.height/2;
    final int h = Math.min(half_height, 4);

    final int x1 = pbox.x - h;
    final int x2 = pbox.x + h;

    final int y1 = pbox.y + half_height - h;
    final int y2 = pbox.y + half_height + h;

    g.setColor(getBackgroundColor()); // this is the tier foreground color

    g.drawLine(x1, y1, x2, y2);
    g.drawLine(x1, y2, x2, y1);

    super.draw(view);
  }

  /** Overridden to always return false. */
  @Override
    public boolean isHitable() {
    return false;
  }


  /**
   * Static method to use DeletionGlyphs to indicate cases where a parent sym/annotation's children
   *      are outside the bounds of the current view, but due to transformations
   *      the information about which edge is closest to is not in the sym itself, but
   *      can be found by comparison with sym that view is based on
   *
   * @param outside_children children syms that are outside edges of view defined by coordseq
   * @param pglyph parent glyph
   * @param annotseq original annotated seq
   * @param coordseq seq for rendering view (could be same as annotated seq or different)
   * @param deletion_y y coord location for deletion glyph
   * @param deletion_height y coord height for deletion glyph
   */
  public static void handleEdgeRendering(java.util.List<SeqSymmetry> outside_children, GlyphI pglyph,
					 BioSeq annotseq, BioSeq coordseq, 
					 double deletion_y, double deletion_height)  {
      /** GAH 2009-02-23 BUG FIX for issues 2390626, 1832822
	 *  found splice view deletion rendering bug when annotation is on negative strand
	 *  Problem was that previous code was assuming that if any children were out of the slice view, then either:
	 *     first child is out on left (5') side of view
	 *     last child is out on right (3') side of view
	 *     or both
	 *  But ordering of children within a parent cannot be assumed
	 *  So instead, now checking bounds of child's original coords relative to composition coords of entire slice view
	 */
    boolean already_right_extended = false;
    boolean already_left_extended = false;
    // some sanity checks
    if (annotseq == coordseq)  { return; }
    if (coordseq == null) { return; }

    // symmetry representing composition of view seq from slices of annnoted seqs
    SeqSymmetry viewsym = coordseq.getComposition();
    SeqSpan viewedges = viewsym.getSpan(annotseq);

    for (SeqSymmetry child : outside_children)  {  
      SeqSpan original_child_span = child.getSpan(annotseq);
      if (original_child_span == null)  { continue; }  // shouldn't happen, but just in case, ignore this child

      // if no other children have already triggered leftward parent extension,
      //   and child span is left of entire view, then extend parent to LEFT
      if (!already_left_extended && 
	  original_child_span.getMax() <= viewedges.getMin())  { 
	already_left_extended = true;
	pglyph.getCoordBox().width += pglyph.getCoordBox().x;
	pglyph.getCoordBox().x = 0;
	DeletionGlyph boundary_glyph = new DeletionGlyph();
	boundary_glyph.setCoords(0.0, deletion_y, 1.0, deletion_height);
	boundary_glyph.setColor(pglyph.getColor());
	pglyph.addChild(boundary_glyph);
      }
	  
      // if no other children have already triggered rightward parent extension,
      //   and child span is right of entire view, then extend parent to RIGHT
      else if (!already_right_extended && 
	       original_child_span.getMin() >= viewedges.getMax())  {
	already_right_extended = true;
	pglyph.getCoordBox().width = coordseq.getLength() - pglyph.getCoordBox().x;
	DeletionGlyph boundary_glyph = new DeletionGlyph();
	boundary_glyph.setCoords(coordseq.getLength()-0.5, deletion_y, 1.0, deletion_height);
	boundary_glyph.setColor(pglyph.getColor());
	pglyph.addChild(boundary_glyph);
      }

    } 
  }

}
