package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.util.StringUtils;
import java.awt.*;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.tiers.TierGlyph.Direction;

/**
 * A glyph used to display a label for a TierGlyph.
 */
public final class TierLabelGlyph extends SolidGlyph implements NeoConstants {
	private int position;

	@Override
	public String toString() {
		return ("TierLabelGlyph: label: \"" + getLabelString() + "\"  +coordbox: " + coordbox);
	}

	/**
	 *  Constructor.
	 *  @param reference_tier the tier in the main part of the AffyLabelledTierMap,
	 *    must not be null
	 */
	TierLabelGlyph(TierGlyph reference_tier, int position) {
		this.setInfo(reference_tier);
		setPosition(position);
	}

	void setPosition(int position){
		this.position = position;
	}

	int getPosition(){
		return position;
	}
	
	/** Overridden such that the info must be of type TierGlyph.  It is used
	 *  to store the reference tier that will be returned by getReferenceTier().
	 */
	@Override
	public void setInfo(Object o) {
		if (o == null) {
			throw new IllegalArgumentException("Null input parameter to setInfo() method in TierLabelGlyph found.");
		}
		if (!(o instanceof TierGlyph)) {
			String msg = "Invalid type " + o.getClass().getName() + " found in input parameter ";
			msg += "for setInfo() method in TierLabelGlyph.  Type TierGlyph required.";
			throw new IllegalArgumentException(msg);
		}
		super.setInfo(o);
	}

	/** Returns the reference tier from the main map in AffyLabelledTierMap.
	 *  Equivalent to value returned by getInfo().  Will not be null.
	 */
	public TierGlyph getReferenceTier() {
		return (TierGlyph) getInfo();
	}

	private static String getDirectionString(TierGlyph tg) {
		return getDirectionSymbol(tg.direction);
	}

	public static String getDirectionSymbol(Direction direction){
		switch (direction) {
			case FORWARD:
				return " (+)";
			case REVERSE:
				return " (-)";
			case BOTH:
				return " (+/-)";
			default: // DIRECTION_NONE
				return "";
		}
	}
	
	/**
	 * Returns the label of the reference tier, or some default string if there isn't one.
	 * @return string
	 */
	private String getLabelString() {
		TierGlyph reference_tier = getReferenceTier();
		if (reference_tier.getLabel() == null) {
			return ".......";
		}
		String direction_str = getDirectionString(reference_tier);
		return reference_tier.getLabel() + direction_str;
	}

	@Override
	public void draw(ViewI view) {
		TierGlyph reftier = this.getReferenceTier();
		Color fgcolor = reftier.getForegroundColor();
		Color bgcolor = reftier.getFillColor();

		Graphics g = view.getGraphics();
		g.setPaintMode();

		Rectangle pixelbox = new Rectangle();
		view.transformToPixels(coordbox, pixelbox);

		if (bgcolor != null) {
			g.setColor(bgcolor);
			g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
		}
		g.setColor(fgcolor);
		g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width - 1, pixelbox.height - 1);
		g.drawRect(pixelbox.x + 1, pixelbox.y + 1, pixelbox.width - 3, pixelbox.height - 3);

		drawLabel(g, view.getPixelBox(), pixelbox);

		super.draw(view);
	}

	private void drawLabel(Graphics g, Rectangle boundingPixelBox, Rectangle pixelbox) {
		// assumes that pixelbox coordinates are already computed

		String label = getLabelString();
		// this was for test:
		// label = "hey_this_is_going_to_be-a-long-text-to-test.the.behaviour";
		//label = "abc DEfgHIj  klMn		OPqRstUv  w xyz.  Antidisestablishmentarianism.  The quick brown fox jumps over a lazy dog.";

		FontMetrics fm = g.getFontMetrics();
		//int text_height = fm.getAscent() + fm.getDescent();
		int text_height = fm.getHeight();

		// Lower bound of visible glyph
		int lowerY = Math.max(pixelbox.y, boundingPixelBox.y);

		// Upper bound of visible glyph
		int upperY = Math.min(
				pixelbox.y + pixelbox.height,
				boundingPixelBox.y + boundingPixelBox.height);

		int text_width = fm.stringWidth(label);
		if (text_width > pixelbox.width) {
			drawWrappedLabel(label, fm, g, lowerY, upperY, text_height, pixelbox);
		} else {
			// if glyph's pixelbox wider than text, then center text
			pixelbox.x += pixelbox.width / 2 - text_width / 2;
			g.drawString(label, pixelbox.x, (lowerY + upperY + text_height) / 2);
		}
	}


	private static void drawWrappedLabel(String label, FontMetrics fm, Graphics g, int lowerY, int upperY, int text_height, Rectangle pixelbox) {
		int pbBuffer_x = 3;
		int maxLines = (upperY - lowerY) / text_height;
		if(maxLines == 0)  { return; }
		String[] lines = StringUtils.wrap(label, fm, pixelbox.width - pbBuffer_x, maxLines);
		pixelbox.x += pbBuffer_x;
		int height =  (upperY + lowerY - text_height*(lines.length - 2)) / 2;
		for (String line : lines) {
			//Remark: the "height-3" parameter in the drawString function is a fine-tune to center vertically.
			g.drawString(line, pixelbox.x, height-3);
			height += text_height;
		}
	}

	/** Draws the outline in a way that looks good for tiers.  With other glyphs,
	 *  the outline is usually drawn a pixel or two larger than the glyph.
	 *  With TierGlyphs, it is better to draw the outline inside of or contiguous
	 *  with the glyph's borders.
	 **/
	@Override
	protected void drawSelectedOutline(ViewI view) {
		draw(view);

		Graphics g = view.getGraphics();
		g.setColor(view.getScene().getSelectionColor());
		Rectangle pixelbox = new Rectangle();
		view.transformToPixels(getPositiveCoordBox(), pixelbox);
		g.drawRect(pixelbox.x, pixelbox.y,
				pixelbox.width - 1, pixelbox.height - 1);

		g.drawRect(pixelbox.x + 1, pixelbox.y + 1,
				pixelbox.width - 3, pixelbox.height - 3);
	}
}
