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

import java.awt.Color;
import java.awt.Font;

/**
 * A Glyph Style is associated with each glyph,
 * keeping its background color, foreground color and Font.
 * In combination with the {@link GlyphStyleFactory},
 * it uses the Flyweight pattern.
 * Only one object exists for each used combination of colors and fonts.
 *
 * <p> There is no empty constructor because GlyphStyle is immutable.
 * It has no set accessors.
 * The get accessors return pointers.
 * However, the properties are each immutable objects themselves.
 * Hence, the properties of a GlyphStyle cannot be changed.
 */
public class GlyphStyle {

	private final Color background_color;
	private final Color foreground_color;
	private final Font fnt;

	public GlyphStyle( Color fg, Color gb, Font fnt ) {
		this.background_color = gb;
		this.foreground_color = fg;
		this.fnt = fnt;
		if ( fg == null || gb == null || fnt == null )
			throw new NullPointerException ( "Can't make GlyphStyle with null constructor argument." );
	}

	public Color getBackgroundColor () {
		return this.background_color;
	}

	public Color getForegroundColor () {
		return this.foreground_color;
	}

	public Font getFont() {
		return this.fnt;
	}

	@Override
	public boolean equals( Object obj ) {
		if( obj instanceof GlyphStyle ) {
			return equals( (GlyphStyle) obj );
		}
		else {
			return false;
		}
	}

	public boolean equals( GlyphStyle theStyle ) {
		if ( theStyle == this ) return true;
		if ( null == theStyle ) return false;
		return ( this.getFont().equals( theStyle.getFont() ) &&
				this.getForegroundColor().equals( theStyle.getForegroundColor() ) &&
				this.getBackgroundColor().equals( theStyle.getBackgroundColor() ) );
	}

	@Override
	public int hashCode() {
		return getForegroundColor().hashCode() + getBackgroundColor().hashCode() + getFont().hashCode();
	}

}
