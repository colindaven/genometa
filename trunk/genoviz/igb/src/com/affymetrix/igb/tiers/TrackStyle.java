package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.style.HeatMap;
import java.awt.Color;
import java.util.*;
import java.util.prefs.*;
import java.util.regex.Pattern;
import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.stylesheet.XmlStylesheetParser;
import com.affymetrix.igb.stylesheet.AssociationElement;
import com.affymetrix.igb.stylesheet.Stylesheet;
import com.affymetrix.igb.stylesheet.PropertyMap;

/**
 *
 *  When setting up a TrackStyle, want to prioritize:
 *
 *  A) Start with default instance (from system stylesheet?)
 *
 *  B) Modify with user-set default parameters from default Preferences node
 *
 *  C) Modify with method-matching parameters from system stylesheet
 *
 *  D) Modify with user-set method parameters from Preferences nodes
 *
 *  Not sure yet where stylesheets from DAS/2 servers fits in yet -- between B/C or between C/D ?
 */
public class TrackStyle implements ITrackStyleExtended {

	private static Preferences tiers_root_node = PreferenceUtils.getTopNode().node("tiers");
	// A pattern that matches two or more slash "/" characters.
	// A preference node name can't contain two slashes, nor end with a slash.
	private static final Pattern multiple_slashes = Pattern.compile("/{2,}");
	private static final String NAME_OF_DEFAULT_INSTANCE = "* DEFAULT *";
	// The String constants named PREF_* are for use in the persistent preferences
	// They are not displayed to users, and should never change
	private static final String PREF_SEPARATE = "Separate Tiers";
	private static final String PREF_COLLAPSED = "Collapsed";
	private static final String PREF_MAX_DEPTH = "Max Depth";
	private static final String PREF_COLOR = "Color";
	private static final String PREF_BACKGROUND = "Background";
	private static final String PREF_HUMAN_NAME = "Human Name";
	private static final String PREF_LABEL_FIELD = "Label Field";
	private static final String PREF_GLYPH_DEPTH = "Glyph Depth";
	private static final String PREF_HEIGHT = "Height"; // height per glyph? // linear transform value?
	private static final String PREF_FORWARD_COLOR = "Forward Color";//MPTAG added for saving forward Color
	private static final String PREF_REVERSE_COLOR = "Reverse Color";//MPTAG added for saving reverse Color
	private static final boolean default_show = true;
	private static final boolean default_separate = true; //MPTAG changed from true
	private static final boolean default_collapsed = false;
	private static final boolean default_expandable = true;
	private static final int default_max_depth = 10;
//	private static final Color default_color = Color.CYAN;
//	private static final Color default_background = Color.BLACK;
	private static final Color default_color = new Color(73, 87, 116);
	private static final Color default_background = new Color(245, 245, 230);
	private static final Color default_forward_color = Color.RED;//MPTAG added for saving forward Color
	private static final Color default_reverse_color = Color.BLUE;//MPTAG added for saving reverse Color
	private static final String default_label_field = "";
	private static final int default_glyph_depth = 2;
	private static final double default_height = 20.0;
	private static final double default_y = 0.0;
	public static final boolean DEBUG = false;
	public static final boolean DEBUG_NODE_PUTS = false;
	// whether to create and use a java Preferences node object for this instance
	private boolean is_persistent = true;
	private boolean show = default_show;
	private boolean separate = default_separate;
	private boolean collapsed = default_collapsed;
	private boolean expandable = default_expandable;
	private int max_depth = default_max_depth;
	private Color color = default_color;
	private Color background = default_background;
	private Color forward_color = default_forward_color;//MPTAG added for saving forward Color
	private Color reverse_color = default_reverse_color;//MPTAG added for saving reverse Color
	private String label_field = default_label_field;
	private int glyph_depth = default_glyph_depth;
	private double height = default_height;
	private double y = default_y;
	private String url = null;
	private boolean color_by_score = false;
	private HeatMap custom_heatmap = null;
	private String unique_name;
	private String human_name;
	final private String method_name;
	private Preferences node;
	private static final Map<String, TrackStyle> static_map = new LinkedHashMap<String, TrackStyle>();
	private static TrackStyle default_instance = null;
	private boolean is_graph = false;
	private Map<String, Object> transient_properties;
	private boolean customizable = true;
	private GenericFeature feature = null;

	public static TrackStyle getInstance(String name, String human_name, Map<String, String> props) {
		return getInstance(name, human_name, true, true, props);
	}

	public static TrackStyle getInstance(String name, String human_name) {
		return getInstance(name, human_name, true, true, null);
	}

	public static TrackStyle getInstance(String unique_name, boolean persistent) {
		return getInstance(unique_name, null, persistent, false, null);
	}

	private static TrackStyle getInstance(String unique_name, String human_name, boolean persistent, boolean force_human_name, Map<String, String> props){
		TrackStyle style = static_map.get(unique_name.toLowerCase());
		if (style == null) {
			if (DEBUG) {
				System.out.println("    (((((((   in AnnotStyle.getInstance() creating AnnotStyle for name: " + unique_name);
			}
			// apply any default stylesheet stuff
			TrackStyle template = getDefaultInstance();
			// at this point template should already have all modifications to default applied from stylesheets and preferences nodes (A & B)
			// apply any stylesheet stuff...
			style = new TrackStyle(unique_name, persistent, template, props);
			static_map.put(unique_name.toLowerCase(), style);

			if(force_human_name) {
				style.human_name = human_name;
			}
		}
		return style;
	}

	/** Returns all (persistent and temporary) instances of AnnotStyle. */
	public static List<TrackStyle> getAllLoadedInstances() {
		return new ArrayList<TrackStyle>(static_map.values());
	}

	/** If there is no AnnotStyle with the given name, just returns the given name;
	 * else modifies the name such that there are no instances that are currently
	 * using it.
	 */
	public static String getUniqueName(String name) {
		String result = name.toLowerCase();
		while (static_map.get(result) != null) {
			result = name.toLowerCase() + "." + System.currentTimeMillis();
		}
		return result;
	}

	protected TrackStyle() {
		method_name = null;
	}

	/** Creates an instance associated with a case-insensitive form of the unique name.
	 *
	 *   When setting up an AnnotStyle, want to prioritize:
	 *
	 *  A) Start with default instance (from system stylesheet?)
	 *  B) Modify with user-set default parameters from default Preferences node
	 *  C) Modify with method-matching parameters from system stylesheet
	 *  D) Modify with user-set method parameters from Preferences nodes
	 *
	 *  Not sure yet where stylesheets from DAS/2 servers fits in yet -- between B/C or between C/D ?
	 */
	private TrackStyle(String name, boolean is_persistent, TrackStyle template, Map<String, String> properties) {
		this.method_name = name;
		this.human_name = name; // this is the default human name, and is not lower case
		this.unique_name = name.toLowerCase();
		this.is_persistent = is_persistent;

		if (is_persistent) {
			if (unique_name.endsWith("/")) {
				unique_name = unique_name.substring(0, unique_name.length() - 1);
			}
			unique_name = multiple_slashes.matcher(unique_name).replaceAll("/");
			// transforming to shortened but unique name if name exceeds Preferences.MAX_NAME_LENGTH
			//   is now handled within PreferenceUtils.getSubnod() call
		}

		if (template != null) {
			// calling initFromTemplate should take care of A) and B)
			initFromTemplate(template);
		}

		// GAH eliminated hard-coded default settings for glyph_depth, can now set in stylesheet
		//    applyHardCodedDefaults();

		// now need to add use of stylesheet settings via AssociationElements, etc.
		Stylesheet stylesheet = XmlStylesheetParser.getUserStylesheet();
		AssociationElement assel = stylesheet.getAssociationForType(name);
		if (assel == null) {
			assel = stylesheet.getAssociationForMethod(name);
		}
		if (assel != null) {
			PropertyMap props = assel.getPropertyMap();
			if (props != null) {
				initFromPropertyMap(props);
			}
		}
		if(properties != null){
			initFromPropertyMap(properties);
		}
		
		if (is_persistent) {
			try {
				node = PreferenceUtils.getSubnode(tiers_root_node, this.unique_name);
			} catch (Exception e) {
				// if there is a problem creating the node, continue with a non-persistent style.
				e.printStackTrace();
				node = null;
				is_persistent = false;
			}
			if (node != null) {
				initFromNode(node);
			}
		} else {
			node = null;
		}
	}

	// Copies properties from the given node, using the currently-loaded values as defaults.
	// generally call initFromTemplate before this.
	// Make sure to set human_name to some default before calling this.
	// Properties set this way do NOT get put in persistent storage.
	private void initFromNode(Preferences node) {
		if (DEBUG) {
			System.out.println("    ----------- called AnnotStyle.initFromNode() for: " + unique_name);
		}
		human_name = node.get(PREF_HUMAN_NAME, this.human_name);

		separate = node.getBoolean(PREF_SEPARATE, this.getSeparate());
		collapsed = node.getBoolean(PREF_COLLAPSED, this.getCollapsed());
		max_depth = node.getInt(PREF_MAX_DEPTH, this.getMaxDepth());
		color = PreferenceUtils.getColor(node, PREF_COLOR, this.getColor());
		background = PreferenceUtils.getColor(node, PREF_BACKGROUND, this.getBackground());

		label_field = node.get(PREF_LABEL_FIELD, this.getLabelField());
		glyph_depth = node.getInt(PREF_GLYPH_DEPTH, this.getGlyphDepth());
	}

	// Copies selected properties from a PropertyMap into this object, but does NOT persist
	// these copied values -- if values were persisted, then if PropertyMap changed between sessions,
	//      older values would override newer values since persisted nodes take precedence
	//    (only want to persists when user sets preferences in GUI)
	private void initFromPropertyMap(PropertyMap props) {

		if (DEBUG) {
			System.out.println("    +++++ initializing AnnotStyle from PropertyMap: " + unique_name);
			System.out.println("             props: " + props);
		}
		Color col = props.getColor("color");
		if (col == null) {
			col = props.getColor("foreground");
		}
		if (col != null) {
			color = col;
		}
		Color bgcol = props.getColor("background");
		if (bgcol != null) {
			background = bgcol;
		}

		String gdepth_string = (String) props.getProperty("glyph_depth");
		if (gdepth_string != null) {
			int prev_glyph_depth = glyph_depth;
			try {
				glyph_depth = Integer.parseInt(gdepth_string);
			} catch (Exception ex) {
				glyph_depth = prev_glyph_depth;
			}
		}
		String labfield = (String) props.getProperty("label_field");
		if (labfield != null) {
			label_field = labfield;
		}

		String mdepth_string = (String) props.getProperty("max_depth");
		if (mdepth_string != null) {
			int prev_max_depth = max_depth;
			try {
				max_depth = Integer.parseInt(mdepth_string);
			} catch (Exception ex) {
				max_depth = prev_max_depth;
			}
		}

		String sepstring = (String) props.getProperty("separate");
		if (sepstring != null) {
			if (sepstring.equalsIgnoreCase("false")) {
				separate = false;
			} else if (sepstring.equalsIgnoreCase("true")) {
				separate = true;
			}
		}
		String showstring = (String) props.getProperty("show");
		if (showstring != null) {
			if (showstring.equalsIgnoreCase("false")) {
				show = false;
			} else if (showstring.equalsIgnoreCase("true")) {
				show = true;
			}
		}
		String collapstring = (String) props.getProperty("collapsed");
		if (collapstring != null) {
			if (collapstring.equalsIgnoreCase("false")) {
				collapsed = false;
			} else if (collapstring.equalsIgnoreCase("true")) {
				collapsed = true;
			}
		}
		if (DEBUG) {
			System.out.println("    +++++++  done initializing from PropertyMap");
		}
		// height???
	}

	private void initFromPropertyMap(Map<String,String> props){
		String labfield = props.get("label_field");
		if (labfield != null && !"".equals(labfield)) {
			label_field = labfield;
		}
	}

	// Copies properties from the template into this object, but does NOT persist
	// these copied values.
	// human_name and factory_instance are not modified
	private void initFromTemplate(TrackStyle template) {
		separate = template.getSeparate();
		show = template.getShow();
		collapsed = template.getCollapsed();
		max_depth = template.getMaxDepth();  // max stacking of annotations
		color = template.getColor();
		background = template.getBackground();
		label_field = template.getLabelField();
		glyph_depth = template.getGlyphDepth();  // depth of visible glyph tree
	}

	// Returns the preferences node, or null if this is a non-persistent instance.
	private Preferences getNode() {
		return this.node;
	}

	/* Gets an instance that can be used for holding
	 *  default values.  The default instance is used as a template in creating
	 *  new instances.  (Although not ALL properties of the default instance are used
	 *  in this way.)
	 */
	public static TrackStyle getDefaultInstance() {
		if (default_instance == null) {
			// Use a temporary variable here to avoid possible synchronization problems.
			TrackStyle instance = new TrackStyle(NAME_OF_DEFAULT_INSTANCE, true, null, null);
			instance.setHumanName("");
			instance.setShow(true);
			default_instance = instance;
			// Note that name will become lower-case
			static_map.put(default_instance.unique_name, default_instance);
		}
		return default_instance;
	}

	public String getUniqueName() {
		return unique_name;
	}

	public String getMethodName() {
		return method_name;
	}

	/** Gets a name that may be shorter and more user-friendly than the unique name.
	 *  The human-readable name may contain upper- and lower-case characters.
	 *  The default is equivalent to the unique name.
	 */
	public String getHumanName() {
		if (human_name == null || human_name.trim().length() == 0) {
			human_name = unique_name;
		}
		return this.human_name;
	}

	public void setHumanName(String human_name) {
		this.human_name = human_name;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setHumanName(): " + human_name);
			}
			getNode().put(PREF_HUMAN_NAME, human_name);
		}
	}

	/** Whether the tier is shown or hidden. */
	public boolean getShow() {
		return show;
	}

	/** Sets whether the tier is shown or hidden; this is a non-persistent setting. */
	public void setShow(boolean b) {
		this.show = b;
	}

	/** Whether PLUS and MINUS strand should be in separate tiers. */
	public boolean getSeparate() {
		return separate;
	}

	public void setSeparate(boolean b) {
		this.separate = b;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setSeparate(): " + human_name + ", " + b);
			}
			getNode().putBoolean(PREF_SEPARATE, b);
		}
	}

	public final boolean getCustomizable() {
		return customizable;
	}

	/** Whether tier is collapsed. */
	public boolean getCollapsed() {
		return collapsed;
	}

	public void setCollapsed(boolean b) {
		this.collapsed = b;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setCollapsed(): " + human_name + ", " + b);
			}
			getNode().putBoolean(PREF_COLLAPSED, b);
		}
	}

	/** Maximum number of rows of annotations for this tier. */
	public int getMaxDepth() {
		return max_depth;
	}

	/** Set the maximum number of rows of annotations for this tier.
	 *  Any attempt to set this less than zero will
	 *  fail, the value will be truncated to fit the range.
	 *  @param max a non-negative number.
	 */
	public void setMaxDepth(int max) {
		if (max < 0) {
			max = 0;
		}
		this.max_depth = max;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setMaxDepth(): " + human_name + ", " + max);
			}
			getNode().putInt(PREF_MAX_DEPTH, max);
		}
	}

	/** The color of annotations in the tier. */
	public Color getColor() {
		return color;
	}

	public void setColor(Color c) {
		if (c != this.color) {
			custom_heatmap = null;
			// get rid of old heatmap, force it to be re-created when needed
		}
		this.color = c;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setColor(): " + human_name + ", " + c);
			}
			PreferenceUtils.putColor(getNode(), PREF_COLOR, c);
		}
	}

	/** The color of the tier Background. */
	public Color getBackground() {
		return background;
	}

	public void setBackground(Color c) {
		if (c != this.background) {
			custom_heatmap = null;
			// get rid of old heatmap, force it to be re-created when needed
		}
		this.background = c;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setBackground(): " + human_name + ", " + c);
			}
			PreferenceUtils.putColor(getNode(), PREF_BACKGROUND, c);
		}
	}

	/** Returns the field name from which the glyph labels should be taken.
	 *  This will never return null, but will return "" instead.
	 */
	public String getLabelField() {
		return label_field;
	}

	public void setLabelField(String l) {
		if (l == null || l.trim().length() == 0) {
			l = "";
		}
		label_field = l;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setLabelField(): " + human_name + ", " + l);
			}
			getNode().put(PREF_LABEL_FIELD, l);
		}
	}

	public int getGlyphDepth() {
		return glyph_depth;
	}

	public void setGlyphDepth(int i) {
		if (glyph_depth != i) {
			glyph_depth = i;
			if (getNode() != null) {
				if (DEBUG_NODE_PUTS) {
					System.out.println("   %%%%% node.put() in AnnotStyle.setGlyphDepth(): " + human_name + ", " + i);
				}
				getNode().putInt(PREF_GLYPH_DEPTH, i);
			}
		}
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double h) {
		height = h;
		if (getNode() != null) {
			if (DEBUG_NODE_PUTS) {
				System.out.println("   %%%%% node.put() in AnnotStyle.setHeight(): " + human_name + ", " + h);
			}
			// BFTAG welche Höhe wird hier beeinflusst? Setzen von einem festen Wert ändert nichts
			// an letztendlicher Glyphenhöhe ?!
			getNode().putDouble(PREF_HEIGHT, h);
		}
	}

	/** could be used to remember tier positions. */
	public void setY(double y) {
		this.y = y;
	}

	/** could be used to remember tier positions. */
	public double getY() {
		return y;
	}

	/** A non-persistent property.  Usually set by UCSC browser "track" lines. */
	public void setUrl(String url) {
		this.url = url;
	}

	/** A non-persistent property.  Usually set by UCSC browser "track" lines. Can return null. */
	public String getUrl() {
		return this.url;
	}

	public final boolean getPersistent() {
		return (is_persistent && getNode() != null);
	}

	public boolean getExpandable() {
		return expandable;
	}

	public void setExpandable(boolean b) {
		// currently there is no need to make this property persistent.
		// there is rarly any reason to change it from the defualt value for
		// annotation tiers, only for graph tiers, which don't use this class
		expandable = b;
	}

	/** Returns false by default.  This class is only intended for annotation tiers,
	 *  not graph tiers.
	 */
	public boolean isGraphTier() {
		return is_graph;
	}

	/** Avoid setting to anything but false.  This class is only intended for annotation tiers,
	 *  not graph tiers.
	 */
	public void setGraphTier(boolean b) {
		is_graph = b;
	}

	public Map<String, Object> getTransientPropertyMap() {
		if (transient_properties == null) {
			transient_properties = new HashMap<String, Object>();
		}
		return transient_properties;
	}

	/**
	 *  Indicates whether the scores of the annotations should be marked by colors.
	 */
	public void setColorByScore(boolean b) {
		color_by_score = b;
	}

	/**
	 *  Indicates whether the scores of the annotations should be marked by colors.
	 */
	public boolean getColorByScore() {
		return color_by_score;
	}

	/**
	 *  Returns a color that can be used to indicate a score between 1 and 1000.
	 *  This will return a color even if getColorByScore() is false.
	 */
	public Color getScoreColor(float score) {
		final float min = 1.0f; // min and max might become variables later...
		final float max = 1000.0f;

		if (score < min) {
			score = min;
		} else if (score > max) {
			score = max;
		}

		final float range = max - min;
		int index = (int) ((score / range) * 255);

		return getCustomHeatMap().getColors()[index];
	}

	/**
	 *  Returns a HeatMap that interpolates between colors based on
	 *  getColor() and getBackgroundColor().  The color at the low
	 *  end of the HeatMap will be slightly different from the background
	 *  color so that it can be distinguished from it.
	 *  This will return a HeatMap even if getColorByScore() is false.
	 */
	private HeatMap getCustomHeatMap() {
		if (custom_heatmap == null) {
			// Bottom color is not quite same as background, so it remains visible
			Color bottom_color = HeatMap.interpolateColor(getBackground(), getColor(), 0.20f);
			custom_heatmap = HeatMap.makeLinearHeatmap("Custom", bottom_color, getColor());
		}
		return custom_heatmap;
	}

	public void copyPropertiesFrom(ITrackStyle g) {
		setColor(g.getColor());
		setShow(g.getShow());
		setHumanName(g.getHumanName());
		setBackground(g.getBackground());
		setCollapsed(g.getCollapsed());
		setMaxDepth(g.getMaxDepth());
		setHeight(g.getHeight());
		setY(g.getY());
		setExpandable(g.getExpandable());
		setFeature(g.getFeature());

		if (g instanceof ITrackStyleExtended) {
			ITrackStyleExtended as = (ITrackStyleExtended) g;
			setColorByScore(as.getColorByScore());
			setGlyphDepth(as.getGlyphDepth());
			setLabelField(as.getLabelField());
			setSeparate(as.getSeparate());
		}
		if (g instanceof TrackStyle) {
			TrackStyle as = (TrackStyle) g;
			setCustomizable(as.getCustomizable());
		}

		getTransientPropertyMap().putAll(g.getTransientPropertyMap());
	}

	/** Whether this style should be customizable in a preferences panel.
	 *  Sometimes there are temporary styles created where some of the options
	 *  simply don't make sense and shouldn't be shown to the user in the
	 *  customization panel.
	 */
	public final void setCustomizable(boolean b) {
		// Another option instead of a single set/getCustomizable flag would be
		// to have a bunch of individual flags: getSeparable(), getHumanNamable(),
		// getHasMaxDepth(), etc....
		customizable = b;
	}

	@Override
	public String toString() {
		String s = "AnnotStyle: (" + Integer.toHexString(this.hashCode()) + ")"
				+ " '" + unique_name + "' ('" + human_name + "') "
				+ " persistent: " + is_persistent
				+ " color: " + getColor()
				+ " bg: " + getBackground();
		return s;
	}

	public void setFeature(GenericFeature f) {
		this.feature = f;
	}

	public GenericFeature getFeature() {
		return this.feature;
	}

	/*
	 * MPTAG added Getter and Setter for forward and reverse colors
	 */
	public Color getForwardColor() {
		return forward_color;
	}

	public void setForwardColor(Color forward_color) {
		this.forward_color = forward_color;
	}

	public Color getReverseColor() {
		return reverse_color;
	}

	public void setReverseColor(Color reverse_color) {
		this.reverse_color = reverse_color;
	}


}
