package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.*;

/**
 * A glyph used to display a text string.
 * Not to be confused with {@link LabelGlyph}
 * (which is used to label other glyphs with text).
 *
 * <p> There are currently some placement problems with StringGlyph.
 * If placement needs to be anything other than {@link NeoConstants#CENTER},
 * {@link NeoConstants#LEFT}, or {@link NeoConstants#WEST},
 * a LabelGlyph may be a better choice.
 */
public class StringGlyph extends SolidGlyph implements NeoConstants  {

	private static final boolean DEBUG_PIXELBOX = false;
	final static Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

	private Rectangle debug_rect;

	private String str;
	private Font fnt = DEFAULT_FONT;
	private int placement;
	private boolean show_background = false;


	@Override
	public String toString() {
		return ("StringGlyph: string: \""+str+"\"  +coordbox: "+coordbox);
	}

	public StringGlyph (String str) {
		this();
		this.str = str;
	}

	public StringGlyph () {
		placement = CENTER;
		if (DEBUG_PIXELBOX) {
			debug_rect = new Rectangle();
		}
	}

	public void setString (String str) {
		this.str = str;
	}
	public String getString () {
		return str;
	}

	public void setShowBackground(boolean show) {
		show_background = show;
	}
	public boolean getShowBackground() {
		return show_background;
	}

	@Override
	public void draw(ViewI view) {
		Graphics g = view.getGraphics();
		g.setPaintMode();
		if ( null != fnt ) {
			g.setFont(fnt);
		}
		FontMetrics fm = g.getFontMetrics();
		int text_width = 0;
		if ( null != str ) {
			text_width = fm.stringWidth(str);
		}
		// int text_height = fm.getAscent() + fm.getDescent();
		int text_height = fm.getAscent();
		int blank_width = fm.charWidth ('z')*2;

		view.transformToPixels(coordbox, pixelbox);
		if (DEBUG_PIXELBOX) {
			debug_rect.setBounds(pixelbox.x, pixelbox.y,
					pixelbox.width, pixelbox.height);
		}
		if (placement == LEFT) {
		}
		else if (placement == RIGHT) {
			pixelbox.x += pixelbox.width + blank_width;
		}
		else {
			pixelbox.x += pixelbox.width/2 - text_width/2;
		}
		if (placement == ABOVE) {
		}
		else if (placement == BELOW) {
			pixelbox.y += pixelbox.height;
		}
		else {
			pixelbox.y += pixelbox.height/2 + text_height/2;
		}
		pixelbox.width = text_width;
		pixelbox.height = text_height+1; // +1 for an extra pixel below the text
		// so letters like 'g' still have at
		// least one pixel below them

		if( getShowBackground() ) { // show background
			Color bgc = getBackgroundColor();
			if ( null != bgc ) {
				g.setColor( getBackgroundColor() );
				g.fillRect( pixelbox.x, pixelbox.y - pixelbox.height,
						pixelbox.width, pixelbox.height);
			}
		}


		if ( null != str ) {
			// display string
			g.setColor( getForegroundColor() );
			// define adjust such that: ascent-adjust = descent+adjust
			// (But see comment above about the extra -1 pixel)
			//			int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0) -1;
			// changed from -1 to +2 so descent is overhanging (rather than ascent)
			int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0) + 2;
			g.drawString (str, pixelbox.x, pixelbox.y -pixelbox.height/2 + adjust);
		}

		if (DEBUG_PIXELBOX) {
			// testing pixbox...
			g.setColor(Color.red);
			g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
			g.setColor(Color.yellow);
			g.drawRect(debug_rect.x, debug_rect.y,
					debug_rect.width, debug_rect.height);
		}
		super.draw(view);
	}

	/** Sets the font.  If you attemt to set the font to null, it will set itself
	 *  to a default font.
	 */
	@Override
	public void setFont(Font f) {
		if (f==null) {
			this.fnt = DEFAULT_FONT;
		} else {
			this.fnt = f;
		}
	}

	@Override
	public Font getFont() {
		return this.fnt;
	}

	public void setPlacement(int placement) {
		this.placement = placement;
	}

	public int getPlacement() {
		return placement;
	}

	/**
	 * @deprecated use {@link #setForegroundColor}.
	 * Also see {@link #setBackgroundColor}.
	 */
	@Deprecated
		public void setColor( Color c ) {
			setForegroundColor( c );
		}

	/**
	 * @deprecated use {@link #getForegroundColor}.
	 * Also see {@link #setBackgroundColor}.
	 */
	@Deprecated
		public Color getColor() {
			return getForegroundColor();
		}

}
