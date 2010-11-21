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

package com.affymetrix.genoviz.glyph;

import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.Font;

import com.affymetrix.genoviz.bioviews.ViewI;

/**
 * Adds an internal label string to solid rectangle glyph.
 */
public class LabelledRectGlyph extends FillRectGlyph {
	private String text;

	public void setText(String str) {
		this.text = str;
	}
	public String getText() {
		return this.text;
	}

	// CLH: This is the constant that the glyph uses to decide
	//      if it should even bother checking to see if the label
	//      will fit. Below this threshold it just assumest that
	//      it will not fit.
	public static final int min_width_needed_for_text = 32;

	@Override
	public void draw(ViewI view) {
		super.draw( view );
		if( getText() != null ) {
			Graphics g = view.getGraphics();

			// CLH: Added a check to make sure there is at least _some_ room
			// before we start getting setting the font and checking metrics.
			// No need to do this on a 1 px wide rectangle!

			if (pixelbox.width >= min_width_needed_for_text) {
				Font savefont = g.getFont();
				Font f2 = this.getFont();
				if (f2 != savefont) {
					g.setFont(f2);
				} else {
					// If they are equal, there's no need to restore the font
					// down below.
					savefont = null;
				}
				FontMetrics fm = g.getFontMetrics();
				int text_width = fm.stringWidth(this.text);

				int midline = pixelbox.y + pixelbox.height / 2;

				if(text_width <= pixelbox.width ) {
					int mid = pixelbox.x + ( pixelbox.width / 2 ) - ( text_width / 2 );
					// define adjust such that: ascent-adjust = descent+adjust
					int adjust = (int) ((fm.getAscent()-fm.getDescent())/2.0);
					g.setColor(this.getForegroundColor());
					g.drawString(this.text, mid, midline + adjust );
				}
				if (null != savefont) {
					g.setFont(savefont);
				}
			}
		}
	}


}
