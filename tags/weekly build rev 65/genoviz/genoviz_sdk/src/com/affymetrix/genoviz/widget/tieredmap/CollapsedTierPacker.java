package com.affymetrix.genoviz.widget.tieredmap;

import com.affymetrix.genoviz.bioviews.AbstractCoordPacker;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.util.*;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;

public class CollapsedTierPacker extends AbstractCoordPacker implements PaddedPackerI {

	public static final int ALIGN_TOP = 1000;
	public static final int ALIGN_BOTTOM = 1001;
	public static final int ALIGN_CENTER = 1002;
	protected int alignment = ALIGN_CENTER;
	protected double maxHeight = 0;
	protected double parent_spacer = 2;

	public Rectangle pack(GlyphI parent, GlyphI child, ViewI view) {
		double height = child.getCoordBox().height;
		if (height > maxHeight) {
			maxHeight = height;
			// need to repack siblings to reflect new max height!
			adjustHeight(parent);
			moveAllChildren(parent);
		} else {
			// max height hasn't changed, just move specified child glyph
			moveOneChild(parent, child);
		}
		return null;
	}

	@Override
	public Rectangle pack(GlyphI parent, ViewI view) {
		List<GlyphI> children = parent.getChildren();
		if (children == null) {
			return null;
		}
		for (GlyphI child : children) {
			maxHeight = Math.max(maxHeight, child.getCoordBox().height);
		}
		adjustHeight(parent);
		moveAllChildren(parent);
		return null;
	}

	protected void adjustHeight(GlyphI parent) {
		parent.getCoordBox().height = maxHeight + (2 * parent_spacer);
	}

	protected void moveOneChild(GlyphI parent, GlyphI child) {
		Rectangle2D.Double pbox = parent.getCoordBox();
		Rectangle2D.Double cbox = child.getCoordBox();

		if (alignment == ALIGN_TOP) {
			child.moveAbsolute(cbox.x, pbox.y + parent_spacer);
		} else if (alignment == ALIGN_BOTTOM) {
			child.moveAbsolute(cbox.x, pbox.y + pbox.height - parent_spacer - cbox.height);
		} else if (alignment == ALIGN_CENTER) {
			double parent_height = maxHeight + (2 * parent_spacer);
			child.moveAbsolute(cbox.x, pbox.y + parent_height / 2 - cbox.height / 2);
		}
	}

	protected void moveAllChildren(GlyphI parent) {
		List<GlyphI> children = parent.getChildren();
		if (children == null) {
			return;
		}
		for (GlyphI child : children) {
			moveOneChild(parent, child);
		}
	}

	public void setParentSpacer(double spacer) {
		this.parent_spacer = spacer;
	}

	public double getParentSpacer() {
		return parent_spacer;
	}

	public void setAlignment(int val) {
		alignment = val;
	}
}
