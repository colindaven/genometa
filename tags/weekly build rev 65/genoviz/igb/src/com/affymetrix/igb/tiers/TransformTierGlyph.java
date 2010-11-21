package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.bioviews.ViewI;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.*;


/**
 *  TransformTierGlyph.
 *  only use transform for operations on children.
 *    coordinates of the tier itself are maintained in coordinate system
 *    of the incoming view...
 *
 *  currently assuming no modifications to tier_transform, etc. are made between
 *     call to modifyView(view) and call to restoreView(view);
 *
 *  Note that if the tier has any "middleground" glyphs, 
 *     these are _not_ considered children, so transform does not apply to them
 *
 */
public final class TransformTierGlyph extends TierGlyph {
  private int fixedPixHeight = 1;

  private final LinearTransform tier_transform = new LinearTransform();

  private LinearTransform modified_view_transform = new LinearTransform();
  private final Rectangle2D.Double modified_view_coordbox = new Rectangle2D.Double();

  private LinearTransform incoming_view_transform;
  private Rectangle2D.Double incoming_view_coordbox;

  // for caching in pickTraversal() methods
  private final Rectangle2D.Double internal_pickRect = new Rectangle2D.Double();
  
  public TransformTierGlyph(ITrackStyle style)  {
    super(style);
  }

  public LinearTransform getTransform() {
    return tier_transform;
  }

	@Override
  public void drawChildren(ViewI view) {

    // MODIFY VIEW
    incoming_view_transform = view.getTransform();
    incoming_view_coordbox = view.getCoordBox();

    // figure out draw transform by combining tier transform with view transform
    // should allow for arbitrarily deep nesting of transforms too, since cumulative
    //     transform is set to be view transform, and view transform is restored after draw...

    AffineTransform trans2D = new AffineTransform();
    trans2D.translate(0.0, incoming_view_transform.getTranslateY());
    trans2D.scale(1.0, incoming_view_transform.getScaleY());
    trans2D.translate(1.0, tier_transform.getTranslateY());
    trans2D.scale(1.0, tier_transform.getScaleY());

    modified_view_transform = new LinearTransform();
	modified_view_transform.setTransform(
			incoming_view_transform.getScaleX(),0,0,trans2D.getScaleY(),
			incoming_view_transform.getTranslateX(),trans2D.getTranslateY());
    view.setTransform(modified_view_transform);

    // need to set view coordbox based on nested transformation
    //   (for methods like withinView(), etc.)
    view.transformToCoords(view.getPixelBox(), modified_view_coordbox);
    view.setCoordBox(modified_view_coordbox);

    // CALL NORMAL DRAWCHILDREN(), BUT WITH MODIFIED VIEW
    super.drawChildren(view);

    // RESTORE ORIGINAL VIEW
    view.setTransform(incoming_view_transform);
    view.setCoordBox(incoming_view_coordbox);
  }

  public void fitToPixelHeight(ViewI view) {
    // use view transform to determine how much "more" scaling must be
    //       done within tier to keep its
    LinearTransform view_transform = view.getTransform();
    double yscale = 0.0d;
    if ( 0.0d != coordbox.height ) {
      yscale = (double)fixedPixHeight / coordbox.height;
    }
    yscale = yscale / view_transform.getScaleY();
    tier_transform.setTransform(tier_transform.getScaleX(),0,0,tier_transform.getScaleY() * yscale,tier_transform.getTranslateX(),tier_transform.getTranslateY());
    coordbox.height = coordbox.height * yscale;
  }


  //
  // need to redo pickTraversal, etc. to take account of transform also...
  //
	@Override
  public void pickTraversal(Rectangle2D.Double pickRect, List<GlyphI> pickList,
                            ViewI view)  {

		if (!isVisible || !intersects(pickRect,view)) {
			return;
		}
		if (hit(pickRect, view)) {
			if (!pickList.contains(this)) {
				pickList.add(this);
			}
		}

		if (children != null) {
			// modify pickRect on the way in
			//   (transform from view coords to local (tier) coords)
			//    [ an inverse transform? ]
			LinearTransform.inverseTransform(tier_transform, pickRect, internal_pickRect);

			for (GlyphI child : children) {
				child.pickTraversal(internal_pickRect, pickList, view);
			}
		}
	}


  // don't move children! just change tier's transform offset
	@Override
  public void moveRelative(double diffx, double diffy) {
    coordbox.x += diffx;
    coordbox.y += diffy;
   tier_transform.setTransform(tier_transform.getScaleX(), 0, 0, tier_transform.getScaleY(), tier_transform.getTranslateX(), tier_transform.getTranslateY() + diffy);
  }

  public void setFixedPixHeight(int pix_height) {
    fixedPixHeight = pix_height;
  }

  public int getFixedPixHeight() {
    return fixedPixHeight;
  }

}

