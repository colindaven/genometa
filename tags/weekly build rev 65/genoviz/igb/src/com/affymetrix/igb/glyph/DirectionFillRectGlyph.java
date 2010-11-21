/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.LabelledRectGlyph;
/**
 *
 * @author Elmo
 */
public class DirectionFillRectGlyph extends LabelledRectGlyph {

	@Override
	public void setText(String s){
		System.out.println(s);
		super.setText(s);
	}

	@Override
	public String getText() {
		return super.getText();
	}

	@Override
	public void draw(ViewI view) {
		super.draw(view);
	}

	@Override
	public void drawTraversal(ViewI view) {
		
		super.drawTraversal(view);
	}
}
