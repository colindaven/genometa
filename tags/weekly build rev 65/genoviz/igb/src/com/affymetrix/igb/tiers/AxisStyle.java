package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.awt.Color;

public final class AxisStyle {
	public static final String PREF_AXIS_COLOR = "Axis color";
	public static final String PREF_AXIS_BACKGROUND = "Axis background";
	public static final String PREF_AXIS_NAME = "Axis name";
	public static final Color default_axis_color = Color.BLACK;
	public static final Color default_axis_background = Color.WHITE;

	/** An un-collapsible, but hideable, instance. */
	public static final TrackStyle axis_annot_style = new TrackStyle() {

		{ // a non-static initializer block
			setHumanName("Coordinates");
		}

		@Override
		public boolean getSeparate() {
			return false;
		}

		@Override
		public boolean getCollapsed() {
			return false;
		}

		@Override
		public boolean getExpandable() {
			return false;
		}

		@Override
		public void setColor(Color c) {
			PreferenceUtils.putColor(PreferenceUtils.getTopNode(), PREF_AXIS_COLOR, c);
		}

		@Override
		public Color getColor() {
			return PreferenceUtils.getColor(PreferenceUtils.getTopNode(), PREF_AXIS_COLOR, default_axis_color);
		}

		@Override
		public void setBackground(Color c) {
			PreferenceUtils.putColor(PreferenceUtils.getTopNode(), PREF_AXIS_BACKGROUND, c);
		}

		@Override
		public Color getBackground() {
			return PreferenceUtils.getColor(PreferenceUtils.getTopNode(), PREF_AXIS_BACKGROUND, default_axis_background);
		}
	};
}
