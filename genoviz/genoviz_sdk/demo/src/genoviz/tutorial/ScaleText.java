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

package genoviz.tutorial;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.FontMetrics;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.SolidGlyph;

public class ScaleText  extends SolidGlyph {

	protected String text = "Put your text here!";
	protected int count = 9;
	protected static int MAX_SIZE = 24;
	@Override
	public void draw(ViewI view) {

		this.calcPixels(view);
		Rectangle bbox = this.getPixelBox();
		Graphics g = view.getGraphics();


		// create Font and FontMetrics objects to scale
		// text size against glyph width
		Font font = new Font(text, Font.PLAIN, count);
//		FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
		FontMetrics fm = g.getFontMetrics(font);

		// scale up if text is smaller than glyph size
		while (fm.stringWidth(text) < bbox.width && MAX_SIZE > count) {
			count++;
			font = new Font(text, Font.PLAIN,count);
//			fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
			fm = g.getFontMetrics(font);
		}
		// scale down if text is larger than glyph size
		while (fm.stringWidth(text) > bbox.width){
			count--;
			font = new Font(text, Font.PLAIN,count);
//			fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
			fm = g.getFontMetrics(font);
		}

		int space = (bbox.width - fm.stringWidth(text))/2;

		g.setFont(font);
		g.setColor( this.getBackgroundColor() );

		g.drawLine(bbox.x, bbox.y, bbox.x, bbox.y+bbox.height);
		g.drawLine(bbox.x+bbox.width, bbox.y, bbox.x+bbox.width, bbox.y+bbox.height);

		if (count < 8) {
			g.drawLine(bbox.x, bbox.y+(bbox.height/2),
					bbox.x+bbox.width, bbox.y+(bbox.height/2));
		}
		else {
			g.drawString(text,bbox.x+space, bbox.y+bbox.height-4);
		}
		super.draw(view);
	}

}
