package com.affymetrix.genoviz.util;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;


/**
 * General utilities for use by other classes as static methods.
 */
public final class GeneralUtils  {

	private static Hashtable<String,Color> colormap;
	private static final BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
    private static final Graphics g = GraphicsEnvironment.getLocalGraphicsEnvironment().createGraphics(img);

	/**
	 * toObjectString(obj) is a debugging tool
	 * A reimplementation of Object.toString() method, to route around any
	 * overriding of toString() in subclasses if desired
	 */
	public static String toObjectString(Object obj) {
		if (obj == null) { return "null"; }

		// GAH 12-3-98
		// uh-oh, no toHexString in jdk1.0.0? (maybe it showed up in 1.0.2?)
		// try just using hashCode instead...
		// return (obj.getClass().getName() + "@" + obj.hashCode());

		// assume that everyone is using jdk1.0.2 or higher
		return (obj.getClass().getName() + "@" +
				Integer.toHexString(obj.hashCode()));
	}

	/** a "global" color map of color names to Color objects */
	public static Hashtable<String,Color> getColorMap() {
		if (colormap == null) {
			colormap = initColorMap();
		}
		return colormap;
	}

	/**
	 * creates a Hastable from a string of options.
	 * The options are name, value pairs of tokens.
	 * e.g. "color blue size 12".
	 * Do not use punctuation or equals signs.
	 *
	 * @param option_string string of name, value pairs to be parsed.
	 * @return option values indexed by name.
	 */
	public static Hashtable<String,Object> parseOptions ( String option_string ) {
		Hashtable<String,Object> hash = new Hashtable<String,Object>(15);
		String key, value;
		StringTokenizer tokens = new StringTokenizer(option_string);
		while (tokens.hasMoreElements()) {
			key = tokens.nextToken();
			try {
				value = tokens.nextToken();
				hash.put(key,value);
			} catch (NoSuchElementException e) {
				System.err.println("ERROR: options must come in name, value pairs");
				System.err.println("--> " + option_string);
			}
		}
		return hash;
	}

	/**
	 * Combines hash1 and hash2 so that any keys that are in both hash1
	 * and hash2 are taken from hash1.
	 */
	public static Hashtable<String,Object> CombineOptions( Hashtable<String,Object> hash1, Hashtable<String,Object> hash2 )  {
		// for efficiency, deal first with special case where hashes are identical
		if (hash1 == hash2) {
			return hash1;
		}
		Hashtable<String,Object> return_hash = new Hashtable<String,Object>(hash2);
		return_hash.putAll(hash1);
		return return_hash;
	}

	/**
	 * creates a color map of the 13 colors defined by AWT.
	 *
	 * @return the AWT colors indexed by name.
	 */
	protected static Hashtable<String,Color> initColorMap() {
		Hashtable<String,Color> cm = new Hashtable<String,Color>();
		cm.put("black", Color.black);
		cm.put("blue", Color.blue);
		cm.put("cyan", Color.cyan);
		cm.put("darkGray", Color.darkGray);
		cm.put("gray", Color.gray);
		cm.put("green", Color.green);
		cm.put("lightGray", Color.lightGray);
		cm.put("magenta", Color.magenta);
		cm.put("orange", Color.orange);
		cm.put("pink", Color.pink);
		cm.put("red", Color.red);
		cm.put("white", Color.white);
		cm.put("yellow", Color.yellow);
		return cm;
	}

  /**
   * A simple way to get a FontMetrics object without calling the
   * deprecated method Toolkit.getFontMetrics(Font).
   * Use Graphics.getFontMetrics() instead whenever possible, but this
   * will work just as well as Toolkit.getFontMetrics(Font).
   */
  public static FontMetrics getFontMetrics(Font fnt) {
    return g.getFontMetrics(fnt);
  }

	/**
	 *  determines whether for the given character set a font is monospaced
	 *    (characters are all of same pixel width)
	 *
	 *  trying to deal with bugs in some AWT implementations,
	 *  where fonts which are <em>supposed</em> to be monospaced
	 *  turn out to not quite be.
	 *
	 *  If the font claims to be monospaced, then this method checks
	 *  the pixel width of all the characters in chars array, and returns
	 *  false if any of them are not equivalent to each other
	 */
	public static boolean isReallyMonospaced(Font fnt, char[] chars) {
		String name = fnt.getName().toLowerCase();
		if (!name.contains("Monospaced")) {
			return false;
		}
		FontMetrics fontmet = getFontMetrics(fnt);
		int width = fontmet.charWidth(chars[0]);
		for (int i = 0; i < chars.length; i++) {
			if (fontmet.charWidth(chars[i]) != width) {
				return false;
			}
		}
		return true;
	}

	/**
	 *  Find the maximum character pixel width for a given set of
	 *  characters over a variety of fonts
	 */
	public static int getMaxCharWidth(Font[] fnts, char[] chars) {
		int width = 0;
		for (int i=0; i<fnts.length; i++) {
			width = Math.max(width, getMaxCharWidth(fnts[i], chars));
		}
		return width;
	}

	/**
	 *  Find the maximum character pixel width for a give set of
	 *  characters using a particular Font
	 */
	public static int getMaxCharWidth(Font fnt, char[] chars) {
		FontMetrics fontmet = getFontMetrics(fnt);
		int width = 0;
		for (int i=0; i<chars.length; i++) {
			width = Math.max(width, fontmet.charWidth(chars[i]));
		}
		return width;
	}

	/**
	 * Safely close a Closeable object.  If it doesn't exist, return.
	 */
	public static <S extends Closeable> void safeClose(S s) {
		if (s == null)
			return;
		try {
			s.close();
		}
		catch(Exception ex) {
			ex.printStackTrace();
		}
	}

}
