package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.geom.Rectangle2D.Double;
import com.affymetrix.genoviz.glyph.LabelGlyph;
import com.affymetrix.genoviz.widget.tieredmap.ExpandedTierPacker;
import java.util.*;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class ExpandPacker extends ExpandedTierPacker {

	/**
	 * constructs a packer that moves glyphs away from the horizontal axis.
	 */
	public ExpandPacker() {
		super(DOWN);
	}

	@Override
	public Rectangle pack(GlyphI parent, ViewI view) {		
		Rectangle2D.Double pbox = parent.getCoordBox();

		// resetting height of parent to just spacers
		parent.setCoords(pbox.x, 0, pbox.width, 2 * parent_spacer);

		List<GlyphI> sibs = parent.getChildren();
		if (sibs == null || sibs.isEmpty()) {
			return null;
		}

		double ymin = Float.MAX_VALUE;
		double ymax = Float.MIN_VALUE;
		double prev_xmax = Float.MIN_VALUE;

		// trying synchronization to ensure this method is threadsafe
		synchronized (sibs) {  // testing synchronizing on sibs vector...
			GlyphI[] sibarray = new GlyphI[sibs.size()];
			sibs.toArray(sibarray);
			sibs.clear(); // sets parent.getChildren() to empty Vector
			int sibs_size = sibarray.length;
			for (int i = 0; i < sibs_size; i++) {
				GlyphI child = sibarray[i];
				if (!(child instanceof LabelGlyph)) {
					pack(parent, child, view, true);
				}
				Rectangle2D.Double cbox = child.getCoordBox();
				// a quick hack to speed up packing when there are no (or few overlaps) --
				// keep track of max x coord of previous sibs --
				//   if prev_xmax < current glyph's min x, then there won't be any overlap,
				//   so can tell pack() to skip check against previous sibs
				sibs.add(child);  // add children back in one at a time
				ymin = Math.min(cbox.y, ymin);
				ymax = Math.max(cbox.y + cbox.height, ymax);
				prev_xmax = Math.max(cbox.x + cbox.width, prev_xmax);
				if (DEBUG_CHECKS) {
					System.out.println(child);
				}
			}
		}

		/*
		 *  now that child packing is done, need to ensure
		 *  that parent is expanded/shrunk vertically to just fit its
		 *  children, plus spacers above and below
		 *
		 *  maybe can get rid of this, since also handled for each child pack
		 *     in pack(parent, child, view);
		 *
		 */


		// move children so "top" edge (y) of top-most child (ymin) is "bottom" edge
		//    (y+height) of bottom-most (ymax) child is at

		for (GlyphI child : parent.getChildren()) {
			child.moveRelative(0, parent_spacer - ymin);
		}

		packParent(parent);

		return null;
	}

	void packParent(GlyphI parent) {
		List<GlyphI> sibs = parent.getChildren();
		Rectangle2D.Double pbox = parent.getCoordBox();
		Rectangle2D.Double newbox = new Rectangle2D.Double();
		Rectangle2D.Double tempbox = new Rectangle2D.Double();
		GlyphI child = sibs.get(0);
		newbox.setRect(pbox.x, child.getCoordBox().y, pbox.width, child.getCoordBox().height);
		int sibs_size = sibs.size();
		if (this.getStretchHorizontal()) {
			for (int i = 1; i < sibs_size; i++) {
				child = sibs.get(i);
				Rectangle2D.union(newbox, child.getCoordBox(), newbox);
			}
		} else {
			for (int i = 1; i < sibs_size; i++) {
				child = sibs.get(i);
				Rectangle2D.Double childbox = child.getCoordBox();
				tempbox.setRect(newbox.x, childbox.y, newbox.width, childbox.height);
				Rectangle2D.union(newbox, tempbox, newbox);
			}
		}
		newbox.y = newbox.y - parent_spacer;
		newbox.height = newbox.height + (2 * parent_spacer);

		if (parent instanceof TransformTierGlyph) {
			TransformTierGlyph transtier = (TransformTierGlyph) parent;
			LinearTransform.transform(transtier.getTransform(), newbox, newbox);
		}
		parent.setCoords(newbox.x, newbox.y, newbox.width, newbox.height);
	}

	@Override
	public Rectangle pack(GlyphI parent, GlyphI child, ViewI view) {
		return pack(parent, child, view, true);
	}

	/**
	 * packs a child.
	 * This adjusts the child's offset
	 * until it no longer reports hitting any of its siblings.
	 */
	private Rectangle pack(GlyphI parent, GlyphI child,
			ViewI view, boolean avoid_sibs) {
		Rectangle2D.Double childbox, siblingbox;
		Rectangle2D.Double pbox = parent.getCoordBox();
		childbox = child.getCoordBox();
		if (movetype == UP) {
			child.moveAbsolute(childbox.x,
					pbox.y + pbox.height - childbox.height - parent_spacer);
		} else {
			// assuming if movetype != UP then it is DOWN
			//    (ignoring LEFT, RIGHT, MIRROR_VERTICAL, etc. for now)
			child.moveAbsolute(childbox.x, pbox.y + parent_spacer);
		}
		childbox = child.getCoordBox();
		List<GlyphI> sibsinrange = null;
		boolean childMoved = true;
		List<GlyphI> sibs = parent.getChildren();
		if (sibs == null) {
			return null;
		}
		if (avoid_sibs) {
			sibsinrange = new ArrayList<GlyphI>();
			int sibs_size = sibs.size();
			for (int i = 0; i < sibs_size; i++) {
				GlyphI sibling = sibs.get(i);
				siblingbox = sibling.getCoordBox();
				if (!(siblingbox.x > (childbox.x + childbox.width)
						|| ((siblingbox.x + siblingbox.width) < childbox.x))) {
					sibsinrange.add(sibling);
				}
			}
			if (DEBUG_CHECKS) {
				System.out.println("sibs in range: " + sibsinrange.size());
			}

			this.before.x = childbox.x;
			this.before.y = childbox.y;
			this.before.width = childbox.width;
			this.before.height = childbox.height;
		} else {
			childMoved = false;
		}
		moveToAvoid(childMoved, sibsinrange, child, view);
		adjustTierBounds(child, parent, pbox, childbox);

		return null;
	}

	private void moveToAvoid(boolean childMoved, List<GlyphI> sibsinrange, GlyphI child, ViewI view) {
		while (childMoved) {
			childMoved = false;
			int sibsinrange_size = sibsinrange.size();
			for (int j = 0; j < sibsinrange_size; j++) {
				GlyphI sibling = sibsinrange.get(j);
				if (sibling == child) {
					continue;
				}
				Rectangle2D.Double siblingbox = sibling.getCoordBox();
				if (DEBUG_CHECKS) {
					System.out.println("checking against: " + sibling);
				}
				if (child.hit(siblingbox, view)) {
					if (DEBUG_CHECKS) {
						System.out.println("hit sib");
					}
					Rectangle2D.Double cb = child.getCoordBox();
					this.before.x = cb.x;
					this.before.y = cb.y;
					this.before.width = cb.width;
					this.before.height = cb.height;
					moveToAvoid(child, sibling, movetype);
					childMoved = childMoved || !before.equals(child.getCoordBox());
				}
			}
		}
	}

	private void adjustTierBounds(GlyphI child, GlyphI parent, Double pbox, Double childbox) {
		// adjusting tier bounds to encompass child (plus spacer)
		// maybe can get rid of this now?
		//   since also handled in pack(parent, view)
		childbox = child.getCoordBox();
		//     if first child, then shrink to fit...
		if (parent.getChildren().size() <= 1) {
			pbox.y = childbox.y - parent_spacer;
			pbox.height = childbox.height + 2 * parent_spacer;
		} else {
			if (pbox.y > (childbox.y - parent_spacer)) {
				double yend = pbox.y + pbox.height;
				pbox.y = childbox.y - parent_spacer;
				pbox.height = yend - pbox.y;
			}
			if ((pbox.y + pbox.height) < (childbox.y + childbox.height + parent_spacer)) {
				double yend = childbox.y + childbox.height + parent_spacer;
				pbox.height = yend - pbox.y;
			}
		}
	}
}
