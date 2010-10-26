package com.affymetrix.genoviz.widget.tieredmap;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.bioviews.AbstractCoordPacker;
import com.affymetrix.genoviz.util.NeoConstants;
import java.util.*;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class ExpandedTierPacker extends AbstractCoordPacker implements PaddedPackerI {
	private boolean STRETCH_HORIZONTAL = true;
	/**
	 * Parent_spacer is <em>not</em> the same as AbstractCoordPacker.spacing.
	 * Spacing is between each child.
	 * parent_spacer is padding added to parent above and below the
	 * extent of all the children.
	 */
	protected double parent_spacer;

	/**
	 * Constructs a packer that moves glyphs away from the horizontal axis.
	 */
	public ExpandedTierPacker() {
		this(NeoConstants.DOWN);
	}

	/**
	 * Constructs a packer with a given direction to move glyphs.
	 *
	 * @param movetype indicates which direction the glyph_to_move should move.
	 * @see #setMoveType
	 */
	public ExpandedTierPacker(int movetype) {
		setMoveType(movetype);
		spacing = 2;
	}


	public int getMoveType() {
		return movetype;
	}

	public void setParentSpacer(double spacer) {
		this.parent_spacer = spacer;
	}

	public double getParentSpacer() {
		return parent_spacer;
	}

	public void setStretchHorizontal(boolean b) {
		STRETCH_HORIZONTAL = b;
	}

	public boolean getStretchHorizontal() {
		return STRETCH_HORIZONTAL;
	}

	@Override
	public Rectangle pack(GlyphI parent, ViewI view) {
		List<GlyphI> sibs;
		GlyphI child;

		sibs = parent.getChildren();
		if (sibs == null) {
			return null;
		}

		Rectangle2D.Double pbox = parent.getCoordBox();

		// resetting height of parent to just spacers
		parent.setCoords(pbox.x, pbox.y, pbox.width, 2 * parent_spacer);

		// trying to fix an old packing bug...
		// this may also speed things up a bit...
		// BUT, this is probably NOT THREADSAFE!!!
		// (if for example another thread was adding / removing glyphs from the parent...)
		//
		// might be more threadsafe to make a new pack(parent, child, view, vector) method
		//   that can take a vector argument to use as children to check against, rather than
		//   always checking against all the children of parent?  And then make a new ArrayList
		//   and keep adding to it rather than removing all the children like current solution
		//      [but then what to do about using glyph searchnodes?]
		//
		// an easier (but probably less efficient) way to make this threadsafe is to
		//    synchronize on the children glyph Vector (sibs), so that no other thread
		//    can muck with it while children are being removed then added back in this array
		//    However, this may become even less efficient depending on what the performance
		//    hit is for sorting the glyphs into GlyphSearchNodes as they are being added
		//    back...
		//
		// trying synchronization to ensure this method is threadsafe
		synchronized (sibs) {  // testing synchronizing on sibs vector...
			GlyphI[] sibarray = new GlyphI[sibs.size()];
			sibs.toArray(sibarray);
			sibs.clear(); // sets parent.getChildren() to empty Vector
			int sibs_size = sibarray.length;
			for (int i = 0; i < sibs_size; i++) {
				child = sibarray[i];
				sibs.add(child);  // add children back in one at a time
				pack(parent, child, view);
				if (DEBUG_CHECKS) {
					System.out.println(child);
				}
			}
		}
		packParent(parent);

		return null;
	}

	private void packParent(GlyphI parent) {
		/*
		 * Now that child packing is done, need to ensure
		 * that parent is expanded/shrunk vertically to just fit its
		 * children, plus spacers above and below.
		 */
		List<GlyphI> sibs = parent.getChildren();
		Rectangle2D.Double pbox = parent.getCoordBox();
		if (sibs == null || sibs.size() <= 0) {
			parent.setCoords(pbox.x, pbox.y, pbox.width, parent_spacer);
			return;
		}
		Rectangle2D.Double newbox = new Rectangle2D.Double();
		Rectangle2D.Double tempbox = new Rectangle2D.Double();
		GlyphI child = sibs.get(0);
		newbox.setRect(pbox.x, child.getCoordBox().y, pbox.width, child.getCoordBox().height);
		int sibs_size = sibs.size();
		if (STRETCH_HORIZONTAL) {
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
		parent.setCoords(newbox.x, newbox.y, newbox.width, newbox.height);
	}

	/**
	 * Packs a child.
	 * This adjusts the child's offset
	 * until it no longer reports hitting any of its siblings.
	 */
	public Rectangle pack(GlyphI parent, GlyphI child, ViewI view) {
		Rectangle2D.Double childbox, siblingbox;
		Rectangle2D.Double pbox = parent.getCoordBox();
		childbox = child.getCoordBox();
		if (movetype == NeoConstants.UP) {
			child.moveAbsolute(childbox.x,
					pbox.y + pbox.height - childbox.height - parent_spacer);
		} else {
			// assuming if movetype != UP then it is DOWN
			//    (ignoring LEFT, RIGHT, MIRROR_VERTICAL, etc. for now)
			child.moveAbsolute(childbox.x, pbox.y + parent_spacer);
		}
		childbox = child.getCoordBox();

		List<GlyphI> sibs = parent.getChildren();
		if (sibs == null) {
			return null;
		}

		List<GlyphI> sibsinrange = new ArrayList<GlyphI>();
		for (GlyphI sibling : sibs) {
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
		boolean childMoved = true;
		while (childMoved) {
			childMoved = false;
			for (GlyphI sibling : sibsinrange) {
				if (sibling == child) {
					continue;
				}
				siblingbox = sibling.getCoordBox();
				if (DEBUG_CHECKS) {
					System.out.println("checking against: " + sibling);
				}
				if (child.hit(siblingbox, view)) {
					if (DEBUG_CHECKS) {
						System.out.println("hit sib");
					}
					if (child instanceof com.affymetrix.genoviz.glyph.LabelGlyph) {
						/* LabelGlyphs cannot be so easily moved as other glyphs.
						 * They will immediately snap back to the glyph they are labeling.
						 * This can cause an infinite loop here.
						 * What's worse is that the "snapping back" may happen outside the loop.
						 * Hence the checking with "before" done below may not always work
						 * for LabelGlyphs.
						 * Someday, we might try changing the LabelGlyph's orientation
						 * to its labeled glyph.
						 * i.e. move it to the other side or inside its labeled glyph.
						 */
					} else {
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

		// adjusting tier bounds to encompass child (plus spacer)
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

		return null;
	}
}
