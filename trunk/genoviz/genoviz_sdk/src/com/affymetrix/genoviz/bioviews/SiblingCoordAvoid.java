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

import com.affymetrix.genoviz.glyph.StretchContainerGlyph;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * This packer makes sure siblings do not overlap.
 * i.e. it makes sure all the direct children of the parent do not overlap.
 * This does not try to recursively pack each child.
 * <p> Note that this packer ignores the coordFuzziness property.
 */
public class SiblingCoordAvoid extends AbstractCoordPacker {

	/**
	 * packs a child.
	 * This adjusts the child's offset
	 * until it no longer reports hitting any of its siblings.
	 */
	public Rectangle pack(GlyphI parent,
			GlyphI child, ViewI view) {
		Rectangle2D.Double childbox, siblingbox;
		childbox = child.getCoordBox();
		List<GlyphI> children = parent.getChildren();
		if (children == null) { return null; }

		List<GlyphI> sibsinrange = new ArrayList<GlyphI>();
		for (GlyphI sibling : children) {
			siblingbox = sibling.getCoordBox();
			if (!(siblingbox.x > (childbox.x + childbox.width)
					|| ((siblingbox.x + siblingbox.width) < childbox.x))) {
				sibsinrange.add(sibling);
			}
		}

		this.before.x = childbox.x;
		this.before.y = childbox.y;
		this.before.width = childbox.width;
		this.before.height = childbox.height;
		boolean childMoved = true;
		while (childMoved) {
			childMoved = false;
			for (GlyphI sibling : sibsinrange) {
				if (sibling == child) { continue; }
				siblingbox = sibling.getCoordBox();
				if (child.hit(siblingbox, view) ) {
					if ( child instanceof com.affymetrix.genoviz.glyph.LabelGlyph ) {
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
					}
					else {
						Rectangle2D.Double cb = child.getCoordBox();
						this.before.x = cb.x;
						this.before.y = cb.y;
						this.before.width = cb.width;
						this.before.height = cb.height;
						moveToAvoid(child, sibling, movetype);
						childMoved = childMoved || ! before.equals(child.getCoordBox());
					}
				}
			}
		}

		if (parent instanceof StretchContainerGlyph) {
			((StretchContainerGlyph)parent).propagateStretch(child);
		}

		return null;
	}

}
