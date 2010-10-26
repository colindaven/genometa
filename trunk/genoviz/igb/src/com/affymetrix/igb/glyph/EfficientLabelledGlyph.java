/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.util.GeneralUtils;

import java.awt.*;
import java.awt.geom.Rectangle2D;

public class EfficientLabelledGlyph extends EfficientSolidGlyph {

	private static final boolean OUTLINE_PIXELBOX = false;
	private static final boolean DEBUG_OPTIMIZED_FILL = false;
	static Rectangle2D.Double scratch_cbox = new Rectangle2D.Double();
	static final int max_char_ypix = 40; // maximum allowed pixel height of chars
	static final int max_char_xpix = 30; // maximum allowed pixel width of chars
	static final int min_char_ypix = 5;  // minimum allowed pixel height of chars
	static final int min_char_xpix = 4;  // minimum allowed pixel width of chars
	// ypix2fonts: index is char height in pixels, entry is Font that gives that char height (or smaller)
	static final Font[] ypix2fonts = new Font[max_char_ypix + 1];
	// xpix2fonts: index is char width in pixels, entry is Font that gives that char width (or smaller)
	static final Font[] xpix2fonts = new Font[max_char_xpix + 1];
	static final int pixel_separation = 1;
	protected boolean show_label = true;
	protected boolean toggle_by_width = true;
	protected boolean toggle_by_height = true;
	protected String label;
	protected int label_loc = NORTH;

	static {
		setBaseFont(new Font("Monospaced", Font.PLAIN, 1));
	}

	public static void setBaseFont(Font base_fnt) {
		int pntcount = 3;
		while (true) {
			// converting to float to trigger correct deriveFont() method...
			Font newfnt = base_fnt.deriveFont((float) (pntcount));
			FontMetrics fm = GeneralUtils.getFontMetrics(newfnt);
			int text_width = fm.stringWidth("G");
			int text_height = fm.getAscent();

			if (text_width > max_char_xpix || text_height > max_char_ypix) {
				break;
			}
			xpix2fonts[text_width] = newfnt;
			ypix2fonts[text_height] = newfnt;
			pntcount++;
		}
		Font smaller_font = null;
		for (int i = 0; i < xpix2fonts.length; i++) {
			if (xpix2fonts[i] != null) {
				smaller_font = xpix2fonts[i];
			} else {
				xpix2fonts[i] = smaller_font;
			}
		}
		smaller_font = null;
		for (int i = 0; i < ypix2fonts.length; i++) {
			if (ypix2fonts[i] != null) {
				smaller_font = ypix2fonts[i];
			} else {
				ypix2fonts[i] = smaller_font;
			}
		}
	}

	@Override
	public void drawTraversal(ViewI view) {
		Rectangle pixelbox = view.getScratchPixBox();
		view.transformToPixels(this.getCoordBox(), pixelbox);
		if (withinView(view) && isVisible) {
			if ((pixelbox.width <= 3)
					|| (pixelbox.height <= 3)) {
				// || (getChildCount() <=0)) {
				// still ends up drawing children for selected, but in general
				//    only a few glyphs are ever selected at the same time, so should be fine
				if (selected) {
					drawSelected(view);
				} else {
					fillDraw(view);
				}
			} else {
				super.drawTraversal(view);  // big enough to draw normal self and children
			}
		}
	}

	public void fillDraw(ViewI view) {
		super.draw(view);
		Rectangle pixelbox = view.getScratchPixBox();
		Graphics g = view.getGraphics();
		if (DEBUG_OPTIMIZED_FILL) {
			g.setColor(Color.white);
		} else {
			g.setColor(this.getBackgroundColor());
		}

		if (show_label) {
			Rectangle2D.Double cbox = this.getCoordBox();
			scratch_cbox.x = cbox.x;
			scratch_cbox.width = cbox.width;
			if (label_loc == NORTH) {
				scratch_cbox.y = cbox.y + cbox.height / 2;
				scratch_cbox.height = cbox.height / 2;
			} else if (label_loc == SOUTH) {
				scratch_cbox.y = cbox.y;
				scratch_cbox.height = cbox.height / 2;
			}
			view.transformToPixels(scratch_cbox, pixelbox);
		} else {
			view.transformToPixels(this.getCoordBox(), pixelbox);
		}

		fixAWTBigRectBug(view, pixelbox);
		if (pixelbox.width < 1) {
			pixelbox.width = 1;
		}
		if (pixelbox.height < 1) {
			pixelbox.height = 1;
		}
		g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
	}

	@Override
	public void draw(ViewI view) {
		super.draw(view);
		Rectangle pixelbox = view.getScratchPixBox();
		Graphics g = view.getGraphics();
		view.transformToPixels(this.getCoordBox(), pixelbox);
		int original_pix_width = pixelbox.width;
		if (pixelbox.width == 0) {
			pixelbox.width = 1;
		}
		if (pixelbox.height == 0) {
			pixelbox.height = 1;
		}

		Rectangle compbox = view.getComponentSizeRect();
		if ((pixelbox.x < compbox.x) ||
						((pixelbox.x + pixelbox.width) > (compbox.x + compbox.width))) {
			pixelbox = pixelbox.intersection(compbox);
		}
		if (OUTLINE_PIXELBOX) {
			g.setColor(Color.yellow);
			g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
		}
		// We use fillRect instead of drawLine, because it may be faster.
		g.setColor(getBackgroundColor());
		if (!show_label) {
			return;
		}

		if (label == null || label.length() == 0) {
			return;
		}

		int xpix_per_char = original_pix_width / label.length();
		int ypix_per_char = (pixelbox.height / 2 - pixel_separation);
		// only draw if there is enough width for smallest font
		if ((xpix_per_char >= min_char_xpix) && (ypix_per_char >= min_char_ypix)) {
			if (xpix_per_char > max_char_xpix) {
				xpix_per_char = max_char_xpix;
			}
			if (ypix_per_char > max_char_ypix) {
				ypix_per_char = max_char_ypix;
			}
			Graphics g2 = g;
			Font xmax_font = xpix2fonts[xpix_per_char];
			Font ymax_font = ypix2fonts[ypix_per_char];
			Font chosen_font = (xmax_font.getSize() < ymax_font.getSize()) ? xmax_font : ymax_font;
			g2.setFont(chosen_font);
			FontMetrics fm = g2.getFontMetrics();
			int text_width = fm.stringWidth(label);
			int text_height = fm.getAscent(); // trying to fudge a little (since ascent isn't quite what I want)

			if ((text_width <= pixelbox.width) &&
							(text_height <= (pixel_separation + pixelbox.height / 2))) {
				int xpos = pixelbox.x + (pixelbox.width / 2) - (text_width / 2);
				if (label_loc == NORTH) {
					g2.drawString(label, xpos,
									//                       pixelbox.y + text_height);
									pixelbox.y + pixelbox.height / 2 - pixel_separation - 2);
				} else if (label_loc == SOUTH) {
					g2.drawString(label, xpos,
									pixelbox.y + pixelbox.height / 2 + text_height + pixel_separation - 1);
				}
			}
		}
	}

	@Override
	public boolean hit(Rectangle2D.Double coord_hitbox, ViewI view) {
		return isVisible && coord_hitbox.intersects(this.getCoordBox());
	}

	public void setLabelLocation(int loc) {
		label_loc = loc;
	}

	public void setShowLabel(boolean b) {
		show_label = b;
	}

	public int getLabelLocation() {
		return label_loc;
	}

	public boolean getShowLabel() {
		return show_label;
	}

	public void setLabel(String str) {
		this.label = str;
	}

	public String getLabel() {
		return label;
	}
}
