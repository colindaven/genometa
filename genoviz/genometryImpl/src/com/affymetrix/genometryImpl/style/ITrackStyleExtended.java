/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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
package com.affymetrix.genometryImpl.style;

import java.awt.Color;

public interface ITrackStyleExtended extends ITrackStyle {
	void setUrl(String url);
	String getUrl();

	void setColorByScore(boolean b);
	boolean getColorByScore();

	Color getScoreColor(float f);

	/** Controls a parameter of the GenericAnnotGlyphFactory. */
	void setGlyphDepth(int i);
	/** Returns a parameter useb by the GenericAnnotGlyphFactory. */
	int getGlyphDepth();

	/** Controls whether plus and minus strands will be drawn separately. */
	void setSeparate(boolean b);
	boolean getSeparate();

	/** Determines which data field in the symmetries will be used to pick the labels. */
	void setLabelField(String s);
	String getLabelField();
}
