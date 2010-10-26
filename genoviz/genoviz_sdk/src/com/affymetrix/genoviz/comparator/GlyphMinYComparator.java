package com.affymetrix.genoviz.comparator;

import com.affymetrix.genoviz.bioviews.GlyphI;
import java.util.Comparator;

public class GlyphMinYComparator implements Comparator<GlyphI> {
	public int compare(GlyphI g1, GlyphI g2) {
		return Double.compare(g1.getCoordBox().y, g2.getCoordBox().y);
	}
}
