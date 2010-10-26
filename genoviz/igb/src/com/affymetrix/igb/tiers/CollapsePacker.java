package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.Rectangle;
import java.util.List;
import com.affymetrix.genoviz.widget.tieredmap.CollapsedTierPacker;
import java.awt.geom.Rectangle2D;

public final class CollapsePacker extends CollapsedTierPacker {

	@Override
	public Rectangle pack(GlyphI parent, ViewI view) {
		List<GlyphI> children = parent.getChildren();

		if (children != null) {
			for (GlyphI child : children) {
				maxHeight = Math.max(maxHeight, child.getCoordBox().height);
			}
		}

		adjustHeight(parent);
		moveAllChildren(parent);

		// trying to transform according to tier's internal transform
		//   (since packing is done based on tier's children)
		if (parent instanceof TransformTierGlyph) {
			Rectangle2D.Double newbox = new Rectangle2D.Double();
			newbox.setRect(parent.getCoordBox());
			TransformTierGlyph transtier = (TransformTierGlyph) parent;
			LinearTransform tier_transform = transtier.getTransform();
			LinearTransform.transform(tier_transform, newbox, newbox);
			parent.setCoords(newbox.x, newbox.y, newbox.width, newbox.height);
		}

		return null;
	}
}


