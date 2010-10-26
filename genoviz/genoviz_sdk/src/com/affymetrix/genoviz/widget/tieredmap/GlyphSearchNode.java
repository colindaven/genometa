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

package com.affymetrix.genoviz.widget.tieredmap;

import com.affymetrix.genoviz.bioviews.GlyphI;
import java.util.Hashtable;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

public class GlyphSearchNode implements Cloneable {

	private final Hashtable<GlyphI,Object> glyphsInSearchTree = new Hashtable<GlyphI,Object>();

	private GlyphSearchNode l;
	private GlyphSearchNode r;
	private List<GlyphI> children;

	private long start;
	private long end;
	private long m;

	private boolean initialized = false;

	private static final long minimum_m = 32;

	private static final boolean debug = false;

	public GlyphSearchNode() { }

	private GlyphSearchNode(long start, long end) {
		this.initialized = true;
		this.start = start;
		this.end = end;
		this.m = (start + end) / 2;
		if (debug) {
			System.out.println(this);
		}
	}

	public Object clone() {
		try {
			Object c = super.clone();
			return c;
		}
		catch (CloneNotSupportedException cnse) {
		}
		return null;
	}

	protected void initialize(long a, long b) {
		if (debug) {
			System.out.println("initialize [" + a + "," + b + "]");
		}
		initialized = true;

		if (b < a) {
			throw new RuntimeException("Tried to initialize GlyphSearchTree with b < a: "
					+ b + ", " + a);
		}

		if (a >= 0) {
			int k = 0;
			while (b > 0) {
				if (debug) {
					System.out.println("Now b is: " + b);
				}
				b >>= 1;
				k++;
			}
			start = 0;
			m = (long) java.lang.Math.pow(2, k - 1);
			end = m * 2;
		}
		else {
			long absa = java.lang.Math.abs(a);
			int k = 0;
			while (absa > 0) {
				absa >>= 1;
				k++;
			}
			m = - (long) java.lang.Math.pow(2, k - 1);
			start = m * 2;
			end = 0;
		}
		if (debug) {
			System.out.println(" using [" + start + "," + m + "," + end + "]");
		}
	}


	protected void split(boolean right) {
		if (right) {
			GlyphSearchNode lprime = (GlyphSearchNode) this.clone();
			m = end;
			end += (end - start);
			GlyphSearchNode rprime = new GlyphSearchNode(m, end);
			children = null;
			l = lprime;
			if (debug) {
				System.out.println("New l is: " + l);
			}
			r = rprime;
		}
		else {
			GlyphSearchNode rprime = (GlyphSearchNode) this.clone();
			m = start;
			start -= (end - start);
			GlyphSearchNode lprime = new GlyphSearchNode(start, m);
			children = null;
			l = lprime;
			r = rprime;
		}
		if (debug) {
			System.out.println("split: [" + start + "," + m + "," + end + "]");
		}
	}

	protected void insert(GlyphI g) {
		if (debug) {
			System.out.println("insert");
		}
		Rectangle2D.Double cb = g.getCoordBox();
		double a = cb.x;
		double b = cb.x + cb.width;
		if (a > b) {
			double t = a;
			a = b;
			b = t;
		}

		if (m - start <= minimum_m || (a <= m && b > m) ) {
			if (null == children) {
				children = new ArrayList<GlyphI>();
			}
			if (debug) {
				System.out.println("Really Adding interval: [" + a + "," + b + "] to " + this);
			}
			children.add(g);
			glyphsInSearchTree.put(g, g);
		}
		else if (b <= m) {
			if (null == l) {
				l = new GlyphSearchNode(start, m);
			}
			if (debug) {
				System.out.println("Adding left interval: [" + a + "," + b + "] to " + this);
			}
			l.addGlyph(g);
		}
		else if (b > m) {
			if (null == r) {
				r = new GlyphSearchNode(m, end);
			}
			if (debug) {
				System.out.println("Adding right interval: [" + a + "," + b + "] to " + this);
			}
			r.addGlyph(g);
		}
		else {
			throw new RuntimeException("I couldn't figure out what to do with: " + g);
		}
	}

	public void removeGlyph ( GlyphI g ) {
		Rectangle2D.Double cb = g.getCoordBox();
		double a = cb.x;
		double b = cb.x + cb.width;
		if (a > b) {
			double t = a;
			a = b;
			b = t;
		}
		if ( ( a <= m && m < b ) || m <= minimum_m ) {
			if ( children != null ) children.remove ( g );
			glyphsInSearchTree.remove(g);
		}
		else if (a > m) {
			if ( r != null )  r.removeGlyph ( g );
		}
		else if (b < m) {
			if ( l != null ) l.removeGlyph ( g );
		}
	}

	public void addGlyph(GlyphI g) {

		Object o = glyphsInSearchTree.get(g);

		// Only add the glyph if not already in search tree!
		// CURRENTLY WE DON'T HANDLE REMOVING OR MOVING GLYPHS!!!

		if (null == o) {
			Rectangle2D.Double cb = g.getCoordBox();
			double a = cb.x;
			double b = cb.x + cb.width;
			if (a > b) {
				double t = a;
				a = b;
				b = t;
			}

			if (!initialized) {
				initialize((long)a, (long)b);
				addGlyph(g);
			}
			else if (b > end) {
				split(true);
				addGlyph(g);
			}
			else if (a < start) {
				split(false);
				addGlyph(g);
			}
			else {
				insert(g);
			}
		}
	}

	public List<GlyphI> getOverlaps(GlyphI g) {
		List<GlyphI> o = new ArrayList<GlyphI>();
		getOverlaps(g, o);
		return o;
	}

	private void getOverlaps(GlyphI i, List<GlyphI> o) {
		Rectangle2D.Double gbox = i.getCoordBox();
		double a = gbox.x;
		double b = gbox.x + gbox.width;
		if (a > b) {
			double t = a;
			a = b;
			b = t;
		}

		if (debug) {
			System.out.println("Checking overlaps [" + a + "," + b + "]: " + this);
		}
		if (null != children) {
			for (GlyphI c : children) {
				if (i != c) {
					Rectangle2D.Double cbox = c.getCoordBox();
					if (debug) {
						System.out.println("cbox[x: " + cbox.x + ", x + width: " + ( cbox.x + cbox.width) + "]" );
					}
					if (! ( ((cbox.x + cbox.width) < a) || (cbox.x > b)) ) {
						o.add(c);
					}
				}
			}
		}
		if (a <= m) {
			if (null != l) {
				l.getOverlaps(i, o);
			}
			else {
				if (debug) {
					System.out.println("l is null");
				}
			}
		}
		if (b >= m) {
			if (null != r) {
				r.getOverlaps(i, o);
			}
			else {
				if (debug) {
					System.out.println("r is null");
				}
			}
		}
	}

	public List getOverlappingGlyphs(double a, double b) {
		List<GlyphI> o = new ArrayList<GlyphI>();
		getOverlappingGlyphs(a, b, o);
		return o;
	}

	private void getOverlappingGlyphs(double a, double b, List<GlyphI> o) {
		if (a > b) {
			double t = a;
			a = b;
			b = t;
		}

		if (null != children) {
			for (GlyphI c : children) {
				Rectangle2D.Double cbox = c.getCoordBox();
				if (! ( ((cbox.x + cbox.width) < a) || (cbox.x > b)) ) {
					o.add(c);
				}
			}
		}
		if (a <= m) {
			if (null != l) {
				l.getOverlappingGlyphs(a, b, o);
			}
			else {
				if (debug) {
					System.out.println("l is null");
				}
			}
		}
		if (b >= m) {
			if (null != r) {
				r.getOverlappingGlyphs(a, b, o);
			}
			else {
				if (debug) {
					System.out.println("r is null");
				}
			}
		}
	}

	/**
	 * Free-up memory by removing references to Glyphs.
	 * Also calls this method on its own left- and
	 * right-hand neighbors, leading to a cascade of
	 * child-removal.  This clears-up a lot of the
	 * loitering objects in AnnotationStation. Although
	 * there may be possible instances when one wouldn't
	 * want to call removeChildren on the left- and right-hand
	 * neighbors, I can't think of any.
	 */
	public void removeChildren() {
		if (children != null) {
			children.clear();
			children = null;
		}
		glyphsInSearchTree.clear();
		if (l != null) { l.removeChildren(); l=null;}
		if (r != null) { r.removeChildren(); r=null;}
	}

	public String toString() {
		return "GlyphSearchNode[start: " + start + " end: " + end
			+ " mid: " + m + " " + super.toString() + "]";
	}

}
