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

package com.affymetrix.genoviz.bioviews;

import java.util.*;
import java.awt.Color;
import com.affymetrix.genoviz.glyph.DirectedGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.util.GeneralUtils;

/**
 *  A factory used to conveniently generate glyphs on a widget.
 */
public class MapGlyphFactory implements NeoConstants  {

	protected int orient;  // orientation, default to HORIZONTAL
	private static final Hashtable<String,Color> colormap = GeneralUtils.getColorMap();
	protected String name;
	protected Scene scene;
	protected Color background_color, default_background, foreground_color,  default_foreground;
	protected double offset, default_offset, width, default_width;
	protected boolean mirror, default_mirror;

	/**
	 * TODO: There are problems with having packer as part of factory settings.
	 * Problems:
	 *   1. packing is relative to a particular view, whereas everything else
	 *      in the factory settings is not.
	 *   2. may want to use particular packer objects, rather than static
	 *      methods on the Packer class
	 * Because of these problems, may want to remove packer as a factory
	 *    option and put it somewhere else -- maybe packing should only
	 *    be allowed in tiers???
	 */
	protected PackerI packer, default_packer;

	protected Class glyphtype, default_glyphtype;
	protected String packerstring, glyphstring;
	protected Hashtable<String,String> default_options;

	/**
	 * creates a horizontally oriented factory.
	 */
	public MapGlyphFactory() {
		this(HORIZONTAL);
	}

	/**
	 * creates a factory.
	 *
	 * @param orientation
	 * must be HORIZONTAL or VERTICAL
	 */
	public MapGlyphFactory(int orientation) {

		orient = orientation;
		GlyphI tempglyph = new FillRectGlyph();
		glyphtype = default_glyphtype = tempglyph.getClass();
		background_color = default_background = Color.black;
		foreground_color = default_foreground = Color.white;
		width = default_width = 5;
		mirror = default_mirror = true;

		// should default be SiblingCoordAvoid or something else
		setPacker( new SiblingCoordAvoid() );

		offset = default_offset = 0;
	}

	public void setScene(Scene scene) {
		this.scene = scene;
	}

	/**
	 * Configure this factory by setting glyph options.
	 *
	 * @param options
	 *    An option String of the form "-option1 value1 -option2 value2..."
	 * @see #configure(Hashtable)
	 */
	public void configure(String options) {
		Hashtable<String,Object> options_hash = GeneralUtils.parseOptions(options);
		configure(options_hash);
	}

	/**
	 * Configure this factory by setting glyph options.
	 * The glyphs produced will have properties with the given values.
	 *
	 * @param options   A Hashtable of the form<BR>
	 * <table>
	 * <tr><td>option1</td><td>value1</td></tr>
	 * <tr><td>option2</td><td>value2</td></tr>
	 * </table>
	 * <p> Valid options are
	 * <table>
	 * <tr><td><code>-background_color</td><td><var>String</var></td>
	 * <td>a background_color name like "red" or "nicePaleBlue"</td></tr>
	 * <tr><td><code>-background</td><td><var>String</var></td>
	 * <td>same as "background_color"</td></tr>
	 * <tr><td><code>-foreground</td><td><var>String</var></td>
	 * <td>a background_color name like "red" or "nicePaleBlue"</td></tr>
	 * <tr><td><code>-glyphtype </td><td><var>String</var></td>
	 * <td>the name of a class implementing GlyphI</td></tr>
	 * <tr><td><code>-mirror</td><td><var>boolean</var></td>
	 * <td>When true the glyph will negate the offset if end &lt; beginning.</td></tr>
	 * <tr><td><code>-offset</td><td><var>int</var></td>
	 * <td>the distance from the primary axis</td></tr>
	 * <tr><td><code>-width</td><td><var>int</var></td>
	 * <td>the "thickness" of the glyph
	 * (orthogonal to the primary axis)</td></tr>
	 * </table>
	 */
	public void configure(Hashtable<String,Object> options) {
		configureTemp(options);
		default_background = background_color;
		default_width = width;
		default_offset = offset;
		default_glyphtype = glyphtype;
		default_packer = packer;
		default_mirror = mirror;
		default_foreground = foreground_color;
	}

	/**
	 * adds a named background_color to the GenoViz background_color map.
	 *
	 * @param name of the background_color
	 * @param col the Color
	 */
	static void addColor(String name, Color col) {
		colormap.put(name, col);
	}

	public Color getColor() {
		return this.background_color;
	}

	public Color getBackgroundColor () { return this.background_color; }

	public Color getForegroundColor () { return this.foreground_color; }

	protected void setTempColor(Color color) {
		this.background_color = color;
	}

	public void setColor(Color color) {
		setTempColor(color);
		this.default_background = color;
	}

	protected void setTempForegroundColor ( Color color ) {
		this.foreground_color = color;
	}

	public void setForegroundColor ( Color color ) {
		setTempForegroundColor ( color );
		this.default_foreground = color;
	}

	/**
	 * synonymous with {@link MapGlyphFactory#setColor(Color)}.
	 */
	public void setBackgroundColor ( Color color ) {
		setColor ( color );
	}

	public double getOffset() {
		return this.offset;
	}

	public void setOffset(double offset) {
		this.offset = offset;
	}

	public boolean getMirror() {
		return this.mirror;
	}

	public void setMirror(boolean mirror) {
		this.mirror = mirror;
	}

	public double getWidth() {
		return this.width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public Class getGlyphtype() {
		return this.glyphtype;
	}

	public void setGlyphtype(Class glyphtype) {
		this.glyphtype = glyphtype;
	}

	public PackerI getPacker() {
		return packer;
	}

	protected void setTempPacker(PackerI packer) {
		this.packer = packer;

		if ( packer instanceof AbstractCoordPacker ) {
			if (this.orient == VERTICAL) {
				((AbstractCoordPacker)packer).setMoveType(MIRROR_HORIZONTAL);
			}
			else {
				((AbstractCoordPacker)packer).setMoveType(MIRROR_VERTICAL);
			}
		}

	}

	public void setPacker(PackerI packer) {
		setTempPacker(packer);
		this.default_packer = packer;
	}

	protected void configureTemp(Hashtable<String,Object> options) {
		if (options == null) {
			return;
		}
		Object tempobj;
		String tempstr;
		

		// checking for new background_color and setting if present
		tempobj = options.get("-color");
		if (tempobj != null) {
			if (tempobj instanceof Color) {
				setTempColor((Color)tempobj);
			}
			else if (tempobj instanceof String) {
				tempobj = colormap.get((String)tempobj);
				if (tempobj != null) {
					setTempColor((Color)tempobj);
				}
			}
		}

		tempobj = options.get("-background");
		if (tempobj != null) {
			if (tempobj instanceof Color) {
				setTempColor((Color)tempobj);
			}
			else if (tempobj instanceof String) {
				tempobj = colormap.get((String)tempobj);
				if (tempobj != null) {
					setTempColor((Color)tempobj);
				}
			}
		}
		tempobj = options.get("-foreground");
		if (tempobj != null) {
			if (tempobj instanceof Color) {
				setTempForegroundColor((Color)tempobj);
			}
			else if (tempobj instanceof String) {
				tempobj = colormap.get((String)tempobj);
				if (tempobj != null) {
					setTempForegroundColor((Color)tempobj);
				}
			}
		}

		// checking for new offset and setting if present
		tempobj = options.get("-offset");
		if (tempobj != null) {
			if (tempobj instanceof String) {
				tempstr = (String)tempobj;
				setOffset(Integer.parseInt(tempstr));
			}
			else if (tempobj instanceof Integer) {
				setOffset(((Integer)tempobj).intValue());
			}
		}

		tempobj = options.get("-mirror");
		if (tempobj != null) {
			if (tempobj instanceof String) {
				tempstr = (String)tempobj;
				setMirror((new Boolean(tempstr)).booleanValue());
			}
			else if (tempobj instanceof Boolean) {
				setMirror(((Boolean)tempobj).booleanValue());
			}
		}

		// checking for new width and setting if present
		tempobj = options.get("-width");
		if (tempobj != null) {
			if (tempobj instanceof String) {
				tempstr = (String)tempobj;
				setWidth(Integer.parseInt(tempstr));
			}
			else if (tempobj instanceof Integer) {
				setWidth(((Integer)tempobj).intValue());
			}
		}

		// checking for new glyph type and setting if present
		tempobj = options.get("-glyphtype");
		if (tempobj != null) {
			if (tempobj instanceof String) {
				tempstr = (String)tempobj;
				
				try {
					setGlyphtype(Class.forName(tempstr));
				}
				catch (ClassNotFoundException ex) {
					if (!tempstr.startsWith("com.")) {
						try {
							tempstr = ("com.affymetrix.genoviz.glyph." + tempstr);
							setGlyphtype(Class.forName(tempstr));
							
							options.put("-glyphtype", tempstr);
						}
						catch (ClassNotFoundException ex2) {
							
							System.out.println(ex2.toString());
							ex2.printStackTrace();
						}
					} else {
						
					}
				}
			} else if (tempobj instanceof Class) {
				setGlyphtype((Class)tempobj);
			}
		}

		// checking for new packer and setting if present
		tempobj = options.get("-packer");
		if (tempobj != null) {
			if (tempobj instanceof String) {
				tempstr = (String)tempobj;
				if (tempstr.equals("null")) {
					setTempPacker(null);
				}
				else {
					try {
						Class tempclass = Class.forName(tempstr);
						Object temppacker = tempclass.newInstance();
						if (temppacker instanceof PackerI) {
							setTempPacker((PackerI)temppacker);
						}
					}
					catch (Exception ex) {
						System.out.println(ex.toString());
						ex.printStackTrace();
					}
				}
			}
			else if (tempobj instanceof Class) {
				try {
					Object temppacker = ((Class)tempobj).newInstance();
					if (temppacker instanceof PackerI) {
						setTempPacker((PackerI)temppacker);
					}
				}
				catch (Exception ex) {
					System.out.println(ex.toString());
					ex.printStackTrace();
				}
			}
		}

	}

	/**
	 * produces a glyph.
	 * The glyph is built according to the current configuration.
	 *
	 * <p><em>The scene must be set if there is a packer.
	 * If the scene is null,
	 * an IllegalStateException will be thrown.</em>
	 *
	 * @param beg where the glyph should begin
	 * @param end where the glyph should end
	 * @return the new glyph or null if it could not be manufactured.
	 * @see #configure(Hashtable)
	 */
	public GlyphI makeGlyph(double beg, double end) {

		if ( null != packer && null == scene ) {
			throw new IllegalStateException
				( "MapGlyphFactory cannot make a glyph when the scene is null and the packer is not." );
		}

		GlyphI item = null;

		try {
			item = (GlyphI)glyphtype.newInstance();

			if ( item instanceof DirectedGlyph ) {
				((DirectedGlyph)item).setOrientation( this.orient );
			}

			double actual_offset, actual_width;
			if (orient == VERTICAL) {
				if (mirror && beg<=end) {
					actual_width = -width;
					actual_offset = -offset;
				} else {
					actual_offset = offset;
					actual_width = width;
				}
				item.setCoords(actual_offset, beg, actual_width, end-beg);
			} else {
				if (mirror && beg>end) {
					actual_offset = -offset;
					actual_width = -width;
				} else {
					actual_offset = offset;
					actual_width = width;
				}
				item.setCoords(beg, actual_offset, end-beg, actual_width);
			}

			item.setBackgroundColor(background_color);
			item.setForegroundColor(foreground_color);

			if (packer != null) {
				packer.pack(scene.getGlyph(), item,
						scene.getViews().get(0));
			}

		} catch( InstantiationException ie ) {
			System.err.println(ie.getMessage());
			ie.printStackTrace();
		} catch( IllegalAccessException ae ) {
			System.err.println(ae.getMessage());
			ae.printStackTrace();
		}
		return item;
	}

	/**
	 * makes a new glyph with a different configuration.
	 * The options are in effect only for the production of one glyph.
	 * The factory's settings are unchanged.
	 *
	 * @param beg where the glyph should begin
	 * @param end where the glyph should end
	 * @param options
	 *    An option String of the form "-option1 value1 -option2 value2..."
	 * @return the new glyph or null if it could not be manufactured.
	 * @see #makeGlyph(double, double)
	 * @see #configure(String)
	 */
	public GlyphI makeGlyph(double beg, double end, String options) {
		Hashtable<String,Object> options_hash = GeneralUtils.parseOptions(options);
		return makeGlyph(beg, end, options_hash);
	}


	/**
	 * makes a new glyph with a different configuration.
	 * The options are in effect only for the production of one glyph.
	 * The factory's settings are unchanged.
	 *
	 * @param beg where the glyph should begin
	 * @param end where the glyph should end
	 * @param options
	 *    A Hashtable of the options
	 * @return the new glyph or null if it could not be manufactured.
	 * @see #makeGlyph(double, double)
	 * @see #configure(Hashtable)
	 */
	public GlyphI makeGlyph(double beg, double end, Hashtable<String,Object> options) {
		// set configuration fields based on options hash
		configureTemp(options);
		// make the glyph, based on configuration fields
		GlyphI gl = makeGlyph(beg, end);

		// restore configure fields to their previous values
		background_color = default_background;
		foreground_color = default_foreground;
		width = default_width;
		offset = default_offset;
		glyphtype = default_glyphtype;
		packer = default_packer;
		mirror = default_mirror;
		return gl;
	}

	/**
	 * makes a glyph and adds it to this factory's scene.
	 * Otherwise, this is just like makeGlyph().
	 * @see #makeGlyph(double, double)
	 */
	public GlyphI makeItem(double beg, double end) {
		GlyphI gl = this.makeGlyph(beg, end);
		scene.addGlyph(gl);
		return gl;
	}

	/**
	 * @see #makeItem(double, double)
	 */
	public GlyphI makeItem(int beg, int end, String options) {
		return makeItem((double)beg, (double)end, options);
	}
	public GlyphI makeItem(double beg, double end, String options) {
		Hashtable<String,Object> options_hash = GeneralUtils.parseOptions(options);
		return makeItem(beg, end, options_hash);
	}

	/**
	 * @see #makeItem(double, double)
	 */
	public GlyphI makeItem(int beg, int end, Hashtable<String,Object> options)  {
		return makeItem((double)beg, (double)end, options);
	}

	/**
	 * @see #makeItem(double, double)
	 */
	public GlyphI makeItem(double beg, double end, Hashtable<String,Object> options)  {
		// set configuration fields based on options hash
		configureTemp(options);
		// make the glyph, based on configuration fields
		GlyphI gl = makeItem(beg, end);

		// restore configure fields to their previous values
		background_color = default_background;
		foreground_color = default_foreground;
		width = default_width;
		offset = default_offset;
		glyphtype = default_glyphtype;
		packer = default_packer;
		mirror = default_mirror;
		return gl;
	}

}
