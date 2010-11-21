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

package com.affymetrix.igb.stylesheet;

import com.affymetrix.genometryImpl.SeqSymmetry;

/** Models an "ELSE" element.  Just like a "MATCH" element, except that
    it always evaluates to true.
 */
final class ElseElement extends MatchElement {
  static String NAME = "ELSE";
  
  /** Always returns true. */
  boolean matches(SeqSymmetry sym) {
    return true;
  }
}
