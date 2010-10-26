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

public final class HeatMap {
	public enum StandardHeatMap {
		BLACK_WHITE("Black/White", Color.BLACK, Color.WHITE),
		VIOLET("Violet", Color.BLACK, new Color(255, 0, 255)),
		BLUE_YELLOW("Blue/Yellow", Color.BLUE, Color.YELLOW),
		BLUE_YELLOW_2("Blue/Yellow 2", new Color(0, 0, 128), new Color(255, 255, 0)),
		RED_BLACK_GREEN("Red/Black/Green", null, null) {
			@Override
			protected HeatMap create(String name, Color c1, Color c2) {
				Color[] colors = new Color[bins];
				for (int bin = 0; bin < bins; bin++) {
					colors[bin] = new Color(Math.max(255 - 2*bin, 0), Math.min(Math.max(2 * (bin-128), 0), 255), 0);
				}
				return new HeatMap(name, colors);

			}
		},
		RAINBOW("Rainbow", null, null) {
			@Override
			protected HeatMap create(String name, Color c1, Color c2) {
				Color[] colors = new Color[bins];
				for (int bin = 0; bin < bins; bin++) {
					colors[bin] = new Color(Color.HSBtoRGB(0.66f*(1.0f*bin)/bins, 0.8f, 1.0f));
				}
				return new HeatMap(name, colors);
			}
		},
		RED_GRAY_BLUE("Red/Gray/Blue", null, null) {
			@Override
			protected HeatMap create(String name, Color c1, Color c2) {
				Color c;
				Color[] colors = new Color[bins];
				for (int bin = 0; bin < bins; bin++) {
					c = new Color(Color.HSBtoRGB(0.66f*(1.0f*bin)/bins, 0.8f, 1.0f));
					int g = (192 * c.getGreen()) / 256;
					colors[bin] = new Color(Math.max(c.getRed(), g), g, Math.max(c.getBlue(), g));
				}
				return new HeatMap(name, colors);
			}
		},
		TRANSPARENT_BW("Transparent B/W", new Color(0, 0, 0, 128), new Color(255, 255, 255, 128)),
		TRANSPARENT_RED("Transparent Red", new Color(0, 0, 0, 128), new Color(255, 0, 0, 128)),
		TRANSPARENT_GREEN("Transparent Green", new Color(0, 0, 0, 128), new Color(0, 255, 0, 128)),
		TRANSPARENT_BLUE("Transparent Blue", new Color(0, 0, 0, 128), new Color(0, 0, 255, 128));

		private static final int bins = 256;
		private final HeatMap heatmap;

		private StandardHeatMap(String name, Color c1, Color c2) {
			this.heatmap = create(name, c1, c2);
		}

		protected HeatMap create(String name, Color c1, Color c2) {
			return HeatMap.makeLinearHeatmap(name, c1, c2);
		}

		public HeatMap getHeatMap() { return heatmap; }

		@Override
		public String toString() { return heatmap.getName(); }
	}

	public static final String PREF_HEATMAP_NAME = "Default Heatmap";
	public static final StandardHeatMap def_heatmap_name = StandardHeatMap.BLUE_YELLOW;

	private final String name;
	private final Color[] colors;

	private HeatMap(String name, Color[] colors) {
		this.name = name;
		this.colors = colors;
	}

	public String getName() {
		return name;
	}

	public Color[] getColors() {
		return colors;
	}

	/** Gets the color at the given index value.
	 * @param heatmap_index an integer in the range 0 to 255.  If the specified
	 *  index is outside this range, the color corresponding to index 0 or 255
	 *  will be returned.
	 */
	public Color getColor(int heatmap_index) {
		if (heatmap_index < 0) {
			return colors[0];
		} else if (heatmap_index > 255) {
			return colors[255];
		} else {
			return colors[heatmap_index];
		}
	}

	/**
	 *  Returns one of the standard pre-defined heat maps using the names in
	 *  StandardHeatMap.
	 * <br />
	 * Will throw NullPointerException if name is null or
	 * IllegalArgumentException if a StandardHeatMap for the requested
	 * name does not exist.
	 *
	 * @param name the name of the StandardHeatMap to return
	 * @return the HeatMap for the requested StandHeatMap
	 */
	public static HeatMap getStandardHeatMap(String name) {
		try {
			return StandardHeatMap.valueOf(name).getHeatMap();
		} catch (IllegalArgumentException e) {
			for (StandardHeatMap s : StandardHeatMap.values()) {
				if (s.toString().equals(name)) {
					return s.getHeatMap();
				}
			}
			throw e;
		}
	}

	/** Make a HeatMap that interpolates linearly between the two given colors. */
	public static HeatMap makeLinearHeatmap(String name, Color low, Color high) {
		Color[] colors = new Color[256];
		HeatMap heat_map = new HeatMap(name, colors);

		for (int i=0; i<256; i++) {
			float x = (i*1.0f)/255.0f;
			colors[i] = interpolateColor(low, high, x);
		}

		return heat_map;
	}


	/**
	 *  Creates a new color inbetween c1 and c2.
	 *  @param x  The fraction of the new color (0.00 to 1.00) that
	 *  should be based on color c2, the rest is based on c1.
	 */
	public static Color interpolateColor(Color c1, Color c2, float x) {
		if (x <= 0.0f) {
			return c1;
		} else if (x >= 1.0f) {
			return c2;
		} else {
			int r = (int) ((1.0f - x) * c1.getRed() + x * c2.getRed());
			int g = (int) ((1.0f - x) * c1.getGreen() + x * c2.getGreen());
			int b = (int) ((1.0f - x) * c1.getBlue() + x * c2.getBlue());
			int a = (int) ((1.0f - x) * c1.getAlpha() + x * c2.getAlpha());

			return new Color(r, g, b, a);
		}
	}

	public static String[] getStandardNames() {
			int length = StandardHeatMap.values().length;
			String[] names = new String[length];
			HeatMap.StandardHeatMap[] shm = StandardHeatMap.values();
			for (int i=0; i<length; i++) {
				names[i] = shm[i].toString();
			}
			return names;
		}
}
