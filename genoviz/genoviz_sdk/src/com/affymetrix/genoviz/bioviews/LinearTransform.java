package com.affymetrix.genoviz.bioviews;

import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class LinearTransform extends AffineTransform  {
	public LinearTransform() {
		super();
	}

	/**
	 * Transforms the coordinate on the axis indicated.
	 * If transform is being used in between a scene and a view,
	 * this would convert from scene coordinates to view/pixel coordinates.
	 * @param orientation
	 * @param in the coordinate
	 * @return transformed coord.
	 */
	public double transform(int orientation, double in) {
		double out = 0;
		if (orientation == NeoConstants.HORIZONTAL) {
			out = in * this.getScaleX() + this.getTranslateX();
		} else if (orientation == NeoConstants.VERTICAL) {
			out = in * this.getScaleY() + this.getTranslateY();
		}
		return out;
	}

	/**
	 * Transforms the source rectangle.
	 * @param src the Rectangle2D.Double to be transformed.
	 * @param dst ignored
	 * @return the Source rectangle transformed.
	 */
	public static Rectangle2D.Double transform(AffineTransform at, Rectangle2D.Double src, Rectangle2D.Double dst) {
		dst.x = src.x * at.getScaleX() + at.getTranslateX();
		dst.y = src.y * at.getScaleY() + at.getTranslateY();
		dst.width = src.width * at.getScaleX();
		dst.height = src.height * at.getScaleY();
		if (dst.height < 0) {
			dst.y += dst.height;
			dst.height = -dst.height;
		}
		if (dst.width < 0) {
			dst.x += dst.width;
			dst.width = -dst.width;
		}
		return dst;
	}

	/**
	 * Transforms the source rectangle inversely.
	 * @param src the Rectangle2D.Double to be transformed.
	 * @param dst ignored
	 * @return the source rectangle transformed.
	 */
	public static Rectangle2D.Double inverseTransform(AffineTransform t, Rectangle2D.Double src, Rectangle2D.Double dst) {
		dst.x = (src.x - t.getTranslateX()) / t.getScaleX();
		dst.y = (src.y - t.getTranslateY()) / t.getScaleY();
		dst.width = src.width / t.getScaleX();
		dst.height = src.height / t.getScaleY();

		if (dst.height < 0) {
			dst.y = dst.y + dst.height;
			dst.height = -dst.height;
		}
		if (dst.width < 0) {
			dst.x = dst.x + dst.width;
			dst.width = -dst.width;
		}
		return dst;
	}

	public boolean equals(LinearTransform lint) {
		if (lint == null) {
			return false;
		}
		return super.equals(lint);
	}

}
