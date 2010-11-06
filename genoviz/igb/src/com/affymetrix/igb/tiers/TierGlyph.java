package com.affymetrix.igb.tiers;

import com.affymetrix.genoviz.comparator.GlyphMinXComparator;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.bioviews.PackerI;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import com.affymetrix.genoviz.widget.tieredmap.PaddedPackerI;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.*;
import java.awt.geom.Rectangle2D;

/*
 MPTAG
 * Die KLasse zur Darstellung der einzelnen Gylphen.
 */
/**
 *  TierGlyph is intended for use with AffyTieredMap.
 *  Each tier in the TieredNeoMap is implemented as a TierGlyph, which can have different
 *  states as indicated below.
 *  In a AffyTieredMap, TierGlyphs pack relative to each other but not to other glyphs added
 *  directly to the map.
 *
 */
public class TierGlyph extends SolidGlyph {
	// extending solid glyph to inherit hit methods (though end up setting as not hitable by default...)

	private boolean sorted = true;
	private boolean ready_for_searching = false;
	private static final Comparator<GlyphI> child_sorter = new GlyphMinXComparator();
	protected Direction direction = Direction.NONE;
	/** glyphs to be drawn in the "middleground" --
	 *    in front of the solid background, but behind the child glyphs
	 *    For example, to indicate how much of the xcoord range has been covered by feature retrieval attempts
	 */
	private final List<GlyphI> middle_glyphs = new ArrayList<GlyphI>();

	public static enum TierState {

		HIDDEN, COLLAPSED, EXPANDED, FIXED_COORD_HEIGHT
	};

	public static enum Direction {

		FORWARD, NONE, REVERSE, BOTH, AXIS
	};
	/** A property for the IAnnotStyle.getTransientPropertyMap().  If set to
	 *  Boolean.TRUE, the tier will draw a label next to where the handle
	 *  would be.
	 *  Note: You probably do NOT want the TierGlyph to draw a label and for the
	 *  included GraphGlyph to also draw a label.
	 */
	public static final String SHOW_TIER_LABELS_PROPERTY = "Show Track Labels";
	/** A property for the IAnnotStyle.getTransientPropertyMap().  If set to
	 *  Boolean.TRUE, the tier will draw a handle on the left side.
	 *  Note: You probably do NOT want the TierGlyph to draw a handle and for the
	 *  included GraphGlyph to also draw a handle.
	 */
	public static final String SHOW_TIER_HANDLES_PROPERTY = "Show Track Handles";
	private double spacer = 2;

	/*
	 * other_fill_color is derived from fill_color whenever setFillColor() is called.
	 * if there are any "middle" glyphs, then background is drawn with other_fill_color and
	 *    middle glyphs are drawn with fill_color
	 * if no "middle" glyphs, then background is drawn with fill_color
	 */
	private Color other_fill_color = null;
	private String label = null;
	private static final Font default_font = new Font("Monospaced", Font.PLAIN, 12);
	private FasterExpandPacker expand_packer = new FasterExpandPacker();
	private CollapsePacker collapse_packer = new CollapsePacker();
	private List<GlyphI> max_child_sofar = null;
	private static final int handle_width = 10;  // width of handle in pixels
	private ITrackStyle style;
	
	public TierGlyph(ITrackStyle style) {
		setHitable(false);
		setSpacer(spacer);
		setStyle(style);
	}

	public final void setStyle(ITrackStyle style) {
		this.style = style;

		// most tier glyphs ignore their foreground color, but AffyTieredLabelMap copies
		// the fg color to the TierLabel glyph, which does pay attention to that color.
		setForegroundColor(style.getColor());
		setFillColor(style.getBackground());

		if (style.getCollapsed()) {
			setPacker(collapse_packer);
		} else {
			setPacker(expand_packer);
		}
		setVisibility(!style.getShow());
		setMaxExpandDepth(style.getMaxDepth());
		setLabel(style.getHumanName());
	}

	public ITrackStyle getAnnotStyle() {
		return style;
	}

	/**
	 *  Adds "middleground" glyphs, which are drawn in front of the background but
	 *    behind all "real" child glyphs.
	 *  These are generally not considered children of
	 *    the glyph.  The TierGlyph will render these glyphs, but they can't be selected since they
	 *    are not considered children in pickTraversal() method.
	 *  The only way to remove these is via removeAllChildren() method,
	 *    there is currently no external access to them.
	 */
	public final void addMiddleGlyph(GlyphI gl) {
		middle_glyphs.add(gl);
	}

	private void initForSearching() {
		int child_count = getChildCount();
		if (child_count > 0) {
			sortChildren(true);  // forcing sort
			//    sortChildren(false); // not forcing sort (relying on sorted field instead...)

			// now construct the max list, which is:
			//   for each entry in min sorted children list, the maximum max
			//     value up to (and including) that position
			// could do max list as int array or as symmetry list, for now doing symmetry list
			max_child_sofar = new ArrayList<GlyphI>(child_count);
			GlyphI curMaxChild = getChild(0);
			Rectangle2D.Double curbox = curMaxChild.getCoordBox();
			double max = curbox.x + curbox.width;
			for (int i = 0; i < child_count; i++) {
				GlyphI child = this.getChild(i);
				curbox = child.getCoordBox();
				double newmax = curbox.x + curbox.width;
				if (newmax > max) {
					curMaxChild = child;
					max = newmax;
				}
				max_child_sofar.add(curMaxChild);
			}
		} else {
			max_child_sofar = null;
		}

		ready_for_searching = true;
	}

	@Override
	public void addChild(GlyphI glyph, int position) {
		throw new RuntimeException("TierGlyph.addChild(glyph, position) not allowed, "
				+ "use TierGlyph.addChild(glyph) instead");
	}

	// overriding addChild() to keep track of whether children are sorted
	//    by ascending min
	@Override
	public void addChild(GlyphI glyph) {
		int count = this.getChildCount();
		if (count <= 0) {
			sorted = true;
		} else if (glyph.getCoordBox().x < this.getChild(count - 1).getCoordBox().x) {
			sorted = false;
		}
		super.addChild(glyph);
//		com.affymetrix.igb.IGB.MPTAGprintClass("TierGlyph.addChild()",glyph);
	}

	/**
	 *  return a list of all children _prior_ to query_index in child list that
	 *    overlap (along x) the child at query_index.
	 *  assumes that child list is already sorted by ascending child.getCoordBox().x
	 *      and that max_child_sofar list is also populated
	 *      (via TierGlyph.initForSearching() call)
	 */
	public final List<GlyphI> getPriorOverlaps(int query_index) {
		if ((!ready_for_searching) || (!sorted)) {
			throw new RuntimeException("must call TierGlyph.initForSearching() before "
					+ "calling TierGlyph.getPriorOverlaps");
		}
		int child_count = getChildCount();
		if (child_count <= 1) {
			return null;
		}

		double query_min = getChild(query_index).getCoordBox().x;
		int cur_index = query_index;

		while (cur_index > 0) {
			cur_index--;
			GlyphI cur_max_glyph = max_child_sofar.get(cur_index);
			Rectangle2D.Double rect = cur_max_glyph.getCoordBox();
			double cur_max = rect.x + rect.width;
			if (cur_max < query_min) {
				cur_index++;
				break;
			}
		}
		if (cur_index == query_index) {
			return null;
		}

		ArrayList<GlyphI> result = new ArrayList<GlyphI>();
		for (int i = cur_index; i < query_index; i++) {
			GlyphI child = getChild(i);
			Rectangle2D.Double rect = child.getCoordBox();
			double max = rect.x + rect.width;
			if (max >= query_min) {
				result.add(child);
			}
		}
		return result;
	}

	private void sortChildren(boolean force) {
		int child_count = this.getChildCount();
		if (((!sorted) || force) && (child_count > 0)) {
			// make sure child symmetries are sorted by ascending min along search_seq
			// to avoid unecessary sort, first go through child list and see if it's
			//     already in ascending order -- if so, then no need to sort
			//     (not sure if this is necessary -- Collections.sort() may already
			//        be optimized to catch this case)
			sorted = true;
			//      int prev_min = Integer.MIN_VALUE;
			double prev_min = Double.NEGATIVE_INFINITY;
			for (int i = 0; i < child_count; i++) {
				GlyphI child = getChild(i);
				double min = child.getCoordBox().x;
				if (prev_min > min) {
					sorted = false;
					break;
				}
				prev_min = min;
			}
			if (!sorted) {
				Collections.sort(children, child_sorter);
			}
		}
		sorted = true;
	}

	public final void setLabel(String str) {
		label = str;
	}

	public final String getLabel() {
		return label;
	}

	// overriding pack to ensure that tier is always the full width of the scene
	@Override
	public void pack(ViewI view) {
		initForSearching();
		super.pack(view);
		Rectangle2D.Double mbox = scene.getCoordBox();
		Rectangle2D.Double cbox = this.getCoordBox();

		if (shouldDrawLabel()) {
			// Add extra space to make room for the label.

			// Although the space SHOULD be computed based on font metrics, etc,
			// that doesn't really work any better than a fixed coord value
			this.setCoords(mbox.x, cbox.y - 6, mbox.width, cbox.height + 6);
		} else {
			this.setCoords(mbox.x, cbox.y, mbox.width, cbox.height);
		}
	}

	/**
	 *  Overridden to allow background shading by a collection of non-child
	 *    "middleground" glyphs.  These are rendered after the solid background but before
	 *    all of the children (which could be considered the "foreground").
	 */
	@Override
	public void draw(ViewI view) {
		view.transformToPixels(coordbox, pixelbox);
	
		pixelbox.width = Math.max(pixelbox.width, min_pixels_width);
		pixelbox.height = Math.max(pixelbox.height, min_pixels_height);

		Graphics g = view.getGraphics();
		Rectangle vbox = view.getPixelBox();
		pixelbox = pixelbox.intersection(vbox);

		if (middle_glyphs.isEmpty()) { // no middle glyphs, so use fill color to fill entire tier
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
			}
		} else {
			if (style.getBackground() != null) {
				g.setColor(style.getBackground());
				g.fillRect(pixelbox.x, pixelbox.y, 2 * pixelbox.width, pixelbox.height);
			}
			
			// cycle through "middleground" glyphs,
			//   make sure their coord box y and height are set to same as TierGlyph,
			//   then call mglyph.draw(view)
			// TODO: This will draw middle glyphs on the Whole Genome, which appears to cause problems due to coordinates vs. pixels
			// See bug 3032785
			if(other_fill_color != null){
				for (GlyphI mglyph : middle_glyphs) {
					Rectangle2D.Double mbox = mglyph.getCoordBox();
					mbox.setRect(mbox.x, coordbox.y, mbox.width, coordbox.height);
					mglyph.setColor(other_fill_color);
					mglyph.drawTraversal(view);
				}
			}
		}

		if (!style.isGraphTier()) {
			// graph tiers take care of drawing their own handles and labels.
			if (shouldDrawLabel()) {
				drawLabelLeft(view);
			}
			if (Boolean.TRUE.equals(style.getTransientPropertyMap().get(SHOW_TIER_HANDLES_PROPERTY))) {
				drawHandle(view);
			}
		}


		super.draw(view);
	}
	
	private boolean shouldDrawLabel() {
		// graph tiers take care of drawing their own handles and labels.
		return (!style.isGraphTier() && Boolean.TRUE.equals(style.getTransientPropertyMap().get(SHOW_TIER_LABELS_PROPERTY)));
	}


	private void drawLabelLeft(ViewI view) {
		if (getLabel() == null) {
			return;
		}
		Rectangle hpix = calcHandlePix(view);
		if (hpix != null) {
			Graphics g = view.getGraphics();
			g.setFont(default_font);
			FontMetrics fm = g.getFontMetrics();
			g.setColor(this.getColor());
			g.drawString(getLabel(), (hpix.x + hpix.width + 1), (hpix.y + fm.getMaxAscent() - 1));
		}
	}

	private Rectangle calcHandlePix(ViewI view) {
		// could cache pixelbox of handle, but then will have problems if try to
		//    have multiple views on same scene / glyph hierarchy
		// therefore reconstructing handle pixel bounds here... (although reusing same object to
		//    cut down on object creation)

		// if full view differs from current view, and current view doesn't left align with full view,
		//   don't draw handle (only want handle at left side of full view)
		if (view.getFullView().getCoordBox().x != view.getCoordBox().x) {
			return null;
		}
		view.transformToPixels(coordbox, pixelbox);
		Rectangle view_pixbox = view.getPixelBox();
		int xbeg = Math.max(view_pixbox.x, pixelbox.x);
		Graphics g = view.getGraphics();
		g.setFont(default_font);

		Rectangle handle_pixbox = new Rectangle();
		FontMetrics fm = g.getFontMetrics();
		int h = Math.min(fm.getMaxAscent(), pixelbox.height);
		handle_pixbox.setBounds(xbeg, pixelbox.y, handle_width, h);
		return handle_pixbox;
	}

	private void drawHandle(ViewI view) {
		Rectangle hpix = calcHandlePix(view);
		if (hpix != null) {
			Graphics g = view.getGraphics();
			Color c = new Color(style.getColor().getRed(), style.getColor().getGreen(), style.getColor().getBlue(), 64);
			g.setColor(c);
			g.fillRect(hpix.x, hpix.y, hpix.width, hpix.height);
			g.drawRect(hpix.x, hpix.y, hpix.width, hpix.height);
		}
	}

	/**
	 *  Remove all children of the glyph, including those added with
	 *  addMiddleGlyph(GlyphI).
	 */
	@Override
	public void removeAllChildren() {
		super.removeAllChildren();
		// also remove all middleground glyphs
		// this is currently the only place where middleground glyphs are treated as if they were children
		//   maybe should rename this method clear() or something like that...
		// only reference to middle glyphs should be in this.middle_glyphs, so should be able to GC them by
		//     clearing middle_glyphs.  These glyphs never have setScene() called on them,
		//     so it is not necessary to call setScene(null) on them.
		middle_glyphs.clear();
	}

	public final TierState getState() {
		if (!isVisible()) {
			return TierState.HIDDEN;
		}
		if (packer == expand_packer) {
			return TierState.EXPANDED;
		}
		if (packer == collapse_packer) {
			return TierState.COLLAPSED;
		}
		return TierState.FIXED_COORD_HEIGHT;
	}

	/** Sets the expand packer.  Note that you are responsible for setting
	 *  any properties of the packer, such as those based on the AnnotStyle.
	 */
	public final void setExpandedPacker(FasterExpandPacker packer) {
		this.expand_packer = packer;
		setSpacer(getSpacer());
		setStyle(getAnnotStyle()); // make sure the correct packer is used, and that its properties are set
	}

	public final void setCollapsedPacker(CollapsePacker packer) {
		this.collapse_packer = packer;
		setSpacer(getSpacer());
		setStyle(getAnnotStyle()); // make sure the correct packer is used, and that its properties are set
	}

	private void setSpacer(double spacer) {
		this.spacer = spacer;
		((PaddedPackerI) collapse_packer).setParentSpacer(spacer);
		((PaddedPackerI) expand_packer).setParentSpacer(spacer);
	}

	private double getSpacer() {
		return spacer;
	}

	/** Sets the color used to fill the tier background, or null if no color
	 *  @param col  A color, or null if no background color is desired.
	 */
	public final void setFillColor(Color col) {
		if (style.getBackground() != col) {
			style.setBackground(col);
		}

		// Now set the "middleground" color based on the fill color
		if (col == null) {
			other_fill_color = Color.DARK_GRAY;
		} else {
			int intensity = col.getRed() + col.getGreen() + col.getBlue();
			if (intensity == 0) {
				other_fill_color = Color.darkGray;
			} else if (intensity > (255 + 127)) {
				other_fill_color = col.darker();
			} else {
				other_fill_color = col.brighter();
			}
		}
	}

	// very, very deprecated
	@Override
	public Color getColor() {
		return getForegroundColor();
	}

	// very, very deprecated
	@Override
	public void setColor(Color c) {
		setForegroundColor(c);
	}

	/** Returns the color used to draw the tier background, or null
	if there is no background. */
	public final Color getFillColor() {
		return style.getBackground();
	}

	@Override
	public void setForegroundColor(Color color) {
		if (style.getColor() != color) {
			style.setColor(color);
		}
	}

	@Override
	public Color getForegroundColor() {
		return style.getColor();
	}

	@Override
	public void setBackgroundColor(Color color) {
		setFillColor(color);
	}

	@Override
	public Color getBackgroundColor() {
		return getFillColor();
	}

	public final Direction getDirection() {
		return direction;
	}

	/**
	 *  Sets direction.  Must be one of DIRECTION_FORWARD, DIRECTION_REVERSE,
	 *  DIRECTION_BOTH or DIRECTION_NONE.
	 */
	public final void setDirection(Direction d) {
		this.direction = d;
	}

	/** Changes the maximum depth of the expanded packer.
	 *  This does not call pack() afterwards.
	 */
	public final void setMaxExpandDepth(int max) {
		expand_packer.setMaxSlots(max);
	}

	/** Not implemented.  Will behave the same as drawSelectedOutline(ViewI). */
	@Override
	protected void drawSelectedFill(ViewI view) {
		this.drawSelectedOutline(view);
	}

	/** Not implemented.  Will behave the same as drawSelectedOutline(ViewI). */
	@Override
	protected void drawSelectedReverse(ViewI view) {
		this.drawSelectedOutline(view);
	}

}
