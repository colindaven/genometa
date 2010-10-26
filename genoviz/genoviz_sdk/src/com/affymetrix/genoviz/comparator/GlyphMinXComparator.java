package com.affymetrix.genoviz.comparator;

import java.util.Comparator;
import com.affymetrix.genoviz.bioviews.GlyphI;

public final class GlyphMinXComparator implements Comparator<GlyphI> {
	public int compare(GlyphI g1, GlyphI g2) {
		return Double.compare(g1.getCoordBox().x, g2.getCoordBox().x);
	}
}
