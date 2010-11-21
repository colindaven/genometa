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

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.igb.view.SeqMapView;
import java.util.*;


public interface MapViewGlyphFactoryI  {
  public void init(Map options);
  public void createGlyph(SeqSymmetry sym, SeqMapView smv);
}
