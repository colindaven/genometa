/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl;

/**
 *  Top-level annots attached to a BioSeq.
 */
public final class TypeContainerAnnot extends SimpleSymWithProps implements TypedSym  {
	String type;

	public TypeContainerAnnot(String type) {
		super();
		this.type = type;
		this.setProperty("method", type);
		this.setProperty(CONTAINER_PROP, Boolean.TRUE);
	}

	public String getType()  { return type; }
}
