/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.glyph.fhh;

import com.affymetrix.genoviz.event.NeoViewBoxChangeEvent;
import com.affymetrix.genoviz.event.NeoViewBoxListener;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.Shadow;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.awt.Font;
import java.util.prefs.*;

public final class UnibrowHairline {

  public static final String PREF_KEEP_HAIRLINE_IN_VIEW = "Keep zoom stripe in view";
  public static final boolean default_keep_hairline_in_view = true;

  static final Font DEFAULT_FONT = new Font("SansSerif", Font.PLAIN, 12);

  // It is common practice to use a VisibleRange with the Shadow as
  // a NeoRangeListener on it, but that seems overly complex for this case.
  //private VisibleRange visible_range;

  private Shadow hairline;
  private NeoMap map;
  //private MouseListener mouse_listener;

  // The NeoViewBoxListener makes sure that the hairline will
  // stay inside the borders of the map, no matter where the user
  // scrolls or zooms the map.
  // (We could use a NeoRangeListener on the NeoMap instead of
  // a pre-draw NeoViewBoxListener on the View.)
  private final NeoViewBoxListener pre_draw_listener;

  // the current location of the hairline
  private double focus = 1;

  private boolean keep_hairline_in_view = true;

  private int _pixelOffset = 0;

  PreferenceChangeListener pcl;

  public UnibrowHairline(NeoMap the_map) {
    if (the_map == null) throw new IllegalArgumentException();

    map = the_map;

    pre_draw_listener = new NeoViewBoxListener() {
      public void viewBoxChanged( NeoViewBoxChangeEvent e ) {
        if (keep_hairline_in_view == true) {
          double start = e.getCoordBox().x;
          double end = e.getCoordBox().width + start;
          if (focus < start) {setSpot(start);}
          else if (focus > end) {setSpot(end);}
        }
		double coordBarOffset = _pixelOffset / map.getView().getTransform().getScaleX();
		hairline.setRange((int) (focus + (int)coordBarOffset), (int) (focus + (int)coordBarOffset) + 1);
      }
    };

    hairline = new Shadow(map);
    hairline.setUseXOR(true);
    hairline.setSelectable(false);
    hairline.setLabeled(false);
    // hairline.setFont(DEFAULT_FONT);
    setKeepHairlineInView(PreferenceUtils.getBooleanParam(PREF_KEEP_HAIRLINE_IN_VIEW, default_keep_hairline_in_view));

    map.getView().addPreDrawViewListener(pre_draw_listener);

    pcl = new PreferenceChangeListener() {
      public void preferenceChange(PreferenceChangeEvent pce) {
        if (! pce.getNode().equals(PreferenceUtils.getTopNode())) {
          return;
        }
        if (pce.getKey().equals(PREF_KEEP_HAIRLINE_IN_VIEW)) {
          setKeepHairlineInView(PreferenceUtils.getBooleanParam(PREF_KEEP_HAIRLINE_IN_VIEW, default_keep_hairline_in_view));
        }
      }
    };

    PreferenceUtils.getTopNode().addPreferenceChangeListener(pcl);
  }

  /** Sets the flag determining whether the hairline is constrained
   *  to remain inside the visible map boundaries.
   *  If b is null, the current value of the flag is not changed.
   */
  public void setKeepHairlineInView(boolean b) {
    keep_hairline_in_view = b;
  }

  /** Sets the location of the hairline.  This is the only supported
   *  way to move the hairline.  Does *NOT* call map.updateWidget() and
   *  but you will probably want to do that after calling this method.
   */
  public void setSpot(double spot) {

    focus = spot;
    //visible_range.setSpot(focus);
    // instead of using the visible_range, directly call hairline.setRange()
	double coordBarOffset = _pixelOffset / map.getView().getTransform().getScaleX();

	if (focus < coordBarOffset) {
		focus = 0;
	} else{
		focus -= Math.round(coordBarOffset);
	}

	//double spot_line = focus + Math.round(coordBarOffset);
	hairline.setRange((int) (focus + coordBarOffset), (int) (focus + coordBarOffset + 1));
    map.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD, focus);
    //map.updateWidget();
  }

  /** Returns the current location of the hairline. */
  public double getSpot() {
    return focus;
  }

  /** Returns the actual shadow glyph. */
  public Shadow getShadow() {
    return hairline;
  }

  /** Call this method to get rid of circular references, to make
   *  garbage collection easier.
   */
  public void destroy() {
    if (map != null && pre_draw_listener != null) {
      map.getView().removePreDrawViewListener(pre_draw_listener);
    }
    if (hairline != null) { hairline.destroy(); }
    hairline = null;
    map = null;
    if (pcl != null) {PreferenceUtils.getTopNode().removePreferenceChangeListener(pcl);}
    pcl = null;
  }

	/**
	 * @return the _pixelOffset
	 */ public int getPixelOffset() {
		return _pixelOffset;
	}

	/**
	 * @param pixelOffset the _pixelOffset to set
	 */ public void setPixelOffset(int pixelOffset) {
		this._pixelOffset = pixelOffset;
	}
}
