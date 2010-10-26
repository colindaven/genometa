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

package com.affymetrix.genoviz.util;

import java.awt.Color;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * A genoviz Color Map to which one can add named colors.
 * This allows all objects to share the same named colors.
 * It is a singleton.
 * Use the getColorMap method
 * to get a pointer to it.
 * The constructor is private.
 */
public final class NeoColorMap extends Hashtable<String,Color> {

	private static NeoColorMap map = new NeoColorMap();

	/**
	 * constructs a NeoColorMap.
	 * The map is filled in with the 13 colors defined by AWT,
	 * and a nice pale blue.
	 */
	private NeoColorMap() {
		super();
		put("black", Color.black);
		put("blue", Color.blue);
		put("cyan", Color.cyan);
		put("darkGray", Color.darkGray);
		put("gray", Color.gray);
		put("green", Color.green);
		put("lightGray", Color.lightGray);
		put("magenta", Color.magenta);
		put("nicePaleBlue", new Color(180, 250, 250));
		put("orange", Color.orange);
		put("pink", Color.pink);
		put("red", Color.red);
		put("white", Color.white);
		put("yellow", Color.yellow);
	}

	/**
	 * gets the color map.
	 *
	 * @return the NeoColorMap
	 */
	public static NeoColorMap getColorMap() {
		return map;
	}

	/**
	 * adds a named color to the map.
	 * This is a type safe way to <code>put( theName, theColor )</code>.
	 * Prefer this to the Hashtable method.
	 *
	 * @param theName
	 * @param theColor
	 */
	public void addColor(String theName, Color theColor) {
		if (null == theName) {
			throw new IllegalArgumentException("Can't addColor without a name.");
		}
		if (null == theColor) {
			throw new IllegalArgumentException("Can't add a null color.");
		}
		map.put(theName, theColor);
	}

	/**
	 * Gets a named color from the map.
	 * This is a type safe way to <code>get( theName )</code>.
	 * Prefer this to the Hashtable method.
	 *
	 * @param theName
	 * @return the color
	 */
	public Color getColor(String theName) {
		if (null == theName) {
			throw new IllegalArgumentException("Can't getColor without a name.");
		}
		return map.get(theName);
	}

	/**
	 * gets the name of a color in the map.
	 *
	 * @param theColor
	 * @return the name in the map for that color.
	 */
	public String getColorName(Color theColor) {
		if (null == theColor) {
			throw new IllegalArgumentException("Can't get a name for a null color.");
		}
		Enumeration it = map.keys();
		while (it.hasMoreElements()) {
			String candidate = (String)it.nextElement();
			if (theColor.equals(map.get(candidate))) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * gets all the names.
	 * This is equivalent to Hashtable's <code>keys()</code> method.
	 *
	 * @return an Enumeration of the names.
	 * Each name is a <code>java.lang.String</code>.
	 */
	public Enumeration getColorNames() {
		return map.keys();
	}

}
