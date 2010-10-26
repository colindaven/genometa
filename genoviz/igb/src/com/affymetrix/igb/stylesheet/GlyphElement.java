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

package com.affymetrix.igb.stylesheet;

import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleDerivedSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.ArrowGlyph;
import com.affymetrix.genoviz.glyph.BridgeGlyph;
import com.affymetrix.genoviz.glyph.DirectedGlyph;
import com.affymetrix.genoviz.glyph.PointedGlyph;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.igb.glyph.EfficientLabelledGlyph;
import com.affymetrix.igb.glyph.EfficientLabelledLineGlyph;
import com.affymetrix.igb.glyph.EfficientLineContGlyph;
import com.affymetrix.igb.glyph.EfficientOutlineContGlyph;
import com.affymetrix.igb.glyph.EfficientOutlinedRectGlyph;
import com.affymetrix.igb.tiers.ExpandPacker;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.Color;
import java.util.*;

final class GlyphElement implements Cloneable, XmlAppender {
/*
<!ELEMENT GLYPH (PROPERTY*, GLYPH*, CHILDREN?)>
<!ATTLIST GLYPH
    type (box | filled_box | arrow | hidden | line | pointed | span | none) #REQUIRED
    position CDATA #IMPLIED
  >
*/
    
  static final String NAME = "GLYPH";
  static final String ATT_TYPE = "type";
  static final String ATT_POSITION = "position";
  
  static ExpandPacker expand_packer;
  static {
      expand_packer = new ExpandPacker();
      expand_packer.setParentSpacer(3);
      expand_packer.setStretchHorizontal(false);
  }
  
  static final String TYPE_BOX = "box";
  private static final String TYPE_FILLED_BOX = "filled_box";
  private static final String TYPE_LINE = "line";
  private static final String TYPE_ARROW = "arrow";
  private static final String TYPE_POINTED = "pointed";
  private static final String TYPE_SPAN = "span";

  private static final String TYPE_NONE = "none";
  private static final String TYPE_INVISIBLE = "hidden";

  private static final String[] knownTypes = new String[] {
    TYPE_BOX, TYPE_FILLED_BOX, TYPE_LINE, 
    TYPE_ARROW, TYPE_POINTED, TYPE_SPAN,
    TYPE_NONE,
    TYPE_INVISIBLE,
  };
  
  /**
   *  Indicates a color; the value shoule be a six-digit RRGGBB hex String.
   */
  private static final String PROP_KEY_COLOR = "color";
  
  /** Set to "true" (default) or "false" to indicate that the map.setInfo()
   *  should be called on the indicated glyph.
   *  Default is true.  False is useful when there are multiple
   *  glyphs representing the same symmetry.
   */
  private static final String PROP_KEY_INDEXED = "indexed";

  /** Whether the glyph is labeled or not.
   */
  private static final String PROP_KEY_LABELED = "labeled";

  /**
   *  Which property name to use to determine the label, if labeled is true.
   */
  private static final String PROP_KEY_LABEL_FIELD = "label_field";

  /** Set to "5to3" (default) or "3to5" to
   *  indicate the direction of directed glyphs, such as arrows.
   */
  private static final String PROP_KEY_DIRECTION = "direction";

  private static final String PROP_VALUE_DIRECTION_3to5 = "3to5";
  
  private static final Color default_color = Color.GREEN;
  
  PropertyMap propertyMap;
  private List<GlyphElement> enclosedGlyphElements = null;
  ChildrenElement childrenElement = null;
  private String position;
  private String type;

  private static final int glyph_height = 10;
  
  private final DerivedSeqSymmetry der; // used for transforming spans
  private final MutableSeqSpan derSpan; // used for transforming spans
  
	@Override
  public Object clone() throws CloneNotSupportedException {
    GlyphElement clone = (GlyphElement) super.clone();
    if (this.enclosedGlyphElements != null) {
      clone.enclosedGlyphElements = new ArrayList<GlyphElement>(enclosedGlyphElements.size());
	  for (GlyphElement ge : enclosedGlyphElements) {
        GlyphElement new_glyph_element = (GlyphElement) ge.clone();
        clone.enclosedGlyphElements.add(new_glyph_element);
      }
    }
    if (propertyMap != null) {
      clone.propertyMap = (PropertyMap) this.propertyMap.clone();
    }
    if (childrenElement != null) {
      clone.childrenElement = (ChildrenElement) this.childrenElement.clone();
    }
    
    return clone;
  }
  
  GlyphElement() {
    this.propertyMap = new PropertyMap();
    der = new SimpleDerivedSeqSymmetry();
    derSpan = new SimpleMutableSeqSpan();
    der.addSpan(derSpan);
  }

  void setPosition(String position) {
    this.position = position;
  }

  void setType(String type) {
    this.type = type;
  }
  
  void addGlyphElement(GlyphElement ge) {
    if (enclosedGlyphElements == null) {
      enclosedGlyphElements = new ArrayList<GlyphElement>();
    }
    enclosedGlyphElements.add(ge);
  }
  
  void setChildrenElement(ChildrenElement c) {
    this.childrenElement = c;
  }
  
	static boolean knownGlyphType(String type) {
		for (String knownType : knownTypes) {
			if (type.equals(knownType)) {
				return true;
			}
		}
		return false;
	}

  private GlyphI makeGlyph(String type) {

    GlyphI gl = null;
    if (TYPE_NONE.equals(type)) {
      gl = null;
    } else if (TYPE_BOX.equals(type)) {
      gl = new EfficientOutlineContGlyph();
    } else if (TYPE_FILLED_BOX.equals(type)) {
      gl = new EfficientOutlinedRectGlyph();
    } else if (TYPE_POINTED.equals(type)) {
      gl = new PointedGlyph();
    } else if (TYPE_LINE.equals(type)) {
      if ("true".equals(propertyMap.getProperty(PROP_KEY_LABELED))) {
        gl = new EfficientLabelledLineGlyph();
      } else {
        gl = new EfficientLineContGlyph();
      }
    } else if (TYPE_ARROW.equals(type)) {
      gl = new ArrowGlyph();
    } else if (TYPE_SPAN.equals(type)) {
      gl = new BridgeGlyph();
    } else if (TYPE_INVISIBLE.equals(type)) {
      gl = new InvisibleBoxGlyph();
    } else {
      // this will be caught by knownGlyphType() method
      System.out.println("GLYPH Type Not Known: " + type);
    }
    return gl;
  }
  
	GlyphI symToGlyph(SeqMapView gviewer, SeqSymmetry insym, GlyphI parent_glyph,
			Stylesheet stylesheet, PropertyMap context) {

		if (insym == null) {
			return null;
		}

		// NOTE: some of the glyphs below are very picky about the order various
		// properties are set in relative to the adding of children and packing.
		// So do lots of testing if you re-arrange any of this.

		PropertyMap oldContext = propertyMap.getContext();
		propertyMap.setContext(context);

		GlyphI gl = null;
		if (knownGlyphType(type)) {
			gl = handleKnownGlyph(context, gviewer, insym, parent_glyph, gl, stylesheet);
		}

		propertyMap.setContext(oldContext);
		return gl;
	}

	private GlyphI handleKnownGlyph(PropertyMap context, SeqMapView gviewer, SeqSymmetry insym, GlyphI parent_glyph, GlyphI gl, Stylesheet stylesheet) {
		TierGlyph tier_glyph = (TierGlyph) context.getProperty(TierGlyph.class.getName());
		SeqSpan span = transformForViewSeq(gviewer, insym);
		if (span == null || (span.getLength() == 0 && parent_glyph instanceof TierGlyph)) {
			// NOTE: important not to simply call "return null" before
			// taking care of restoring the context.
			// TODO: In future we must take into account the possibility
			// that an item may have children which map to the current seq
			// even though the parent does not.  Thus we need to loop over
			// the children even when the span for the current item is null.
			//
			// Would be nice if a SeqSymmetry could report whether all its children
			// are or are not enclosed in its bounds, then we would know which
			// syms we really have to worry about that for.
			return null;
		}

		gl = makeGlyph(type);
		if (gl != null) {
			gl.setCoords(span.getMin(), 0, span.getLength(), glyph_height);
			if (gl instanceof EfficientLabelledGlyph) {
				configureLabel((EfficientLabelledGlyph) gl, insym, tier_glyph);
			}
			gl.setColor(findColor(propertyMap));
			addToParent(parent_glyph, gl);
			indexGlyph(propertyMap, gviewer, gl, insym);
		} // but if no glyph was drawn, use the parent glyph
		GlyphI container = gl;
		if (gl == null) {
			container = parent_glyph;
		} // These re-draw the same sym, not the children
		drawEnclosedGlyphs(gviewer, container, insym, stylesheet);
		if (childrenElement != null) {
			// Always use "insym" rather than "transformed_sym" for children.
			// The transformed_sym may not have the same number of levels of nesting.
			childrenElement.childSymsToGlyphs(gviewer, insym, container, stylesheet, propertyMap);
		}
		packGlyph(gviewer, container);
		// Setting the direction of a directed glyph must come after
		// adding the children to it.  Not sure why.
		if (gl instanceof DirectedGlyph) {
			((DirectedGlyph) gl).setForward(false);
			if (PROP_VALUE_DIRECTION_3to5.equalsIgnoreCase((String) propertyMap.getProperty(PROP_KEY_DIRECTION))) {
				((DirectedGlyph) gl).setForward(!span.isForward());
			} else {
				((DirectedGlyph) gl).setForward(span.isForward());
			}
		}
		return gl;
	}

  private void addToParent(GlyphI parent, GlyphI child) {
    parent.addChild(child);
    //TODO: use position
    // One way to do it: make a special glyph interface StyledGlyphI where
    // all implementations of that interface know how to position themselves
    // inside their parents
  }

  private void packGlyph(SeqMapView gviewer, GlyphI container) {
    if (container != null) {
      if (! (container instanceof TierGlyph)) {
        // packing with labeled glyphs doesn't work right, so skip it.
        container.setPacker(expand_packer);
        container.pack(gviewer.getSeqMap().getView());
      }
    }
  }
  
  private void drawEnclosedGlyphs(SeqMapView gviewer, GlyphI container, SeqSymmetry insym, Stylesheet stylesheet) {
    if (enclosedGlyphElements != null) {
      // inside the parent, not inside the glyph.
      for (GlyphElement kid : enclosedGlyphElements) {
        kid.symToGlyph(gviewer, insym, container, stylesheet, this.propertyMap);
      }
    }
  }
  
  private static Color findColor(PropertyMap pm) {
    Color color = pm.getColor(PROP_KEY_COLOR);
    if (color == null || "".equals(color.toString())) {
      ITrackStyleExtended style = (ITrackStyleExtended) pm.get(ITrackStyleExtended.class.getName());
      if (style != null) {
        color = style.getColor();
      }
    }
    if (color == null) {
      color = default_color;
    }
    return color;
  }

  private void configureLabel(EfficientLabelledGlyph lgl, SeqSymmetry insym, TierGlyph tier_glyph) {
	  String the_label = determineLabel(insym);

    // go ahead and set the height big enough for a label, even if it is null,
    // because (1) we want to keep constant heights with other labeled glyphs, and
    // (2) instances of LabelledGlyph expect that.
    lgl.getCoordBox().height *= 2;
    if (the_label != null) {
      lgl.setLabel(the_label);
      if (tier_glyph.getDirection() == TierGlyph.Direction.REVERSE) {
        lgl.setLabelLocation(GlyphI.SOUTH);
      } else {
        lgl.setLabelLocation(GlyphI.NORTH);
      }
    }
  }


	private String determineLabel(SeqSymmetry insym) {
		String the_label = null;
		if (insym instanceof SymWithProps) {
			String label_property_name = (String) this.propertyMap.getProperty(PROP_KEY_LABEL_FIELD);
			if (null == label_property_name) {
				the_label = insym.getID();
			} else {
				the_label = (String) ((SymWithProps) insym).getProperty(label_property_name);
				if (the_label == null) {
					the_label = label_property_name + "=???";
				}
			}
		} else {
			the_label = insym.getID();
		}
		return the_label;
	}
  
  
  private static void indexGlyph(PropertyMap pm, SeqMapView gviewer, GlyphI gl, SeqSymmetry insym) {
    if (! "false".equals(pm.getProperty(PROP_KEY_INDEXED))) {
      // This will call GlyphI.setInfo() as a side-effect.
      gviewer.getSeqMap().setDataModelFromOriginalSym(gl, insym);
    } else {
      // Even if we don't add the glyph to the map's data model,
      // it is still important to call GlyphI.setInfo() so that slicing will work.
      if (insym instanceof DerivedSeqSymmetry)  {
        gl.setInfo(((DerivedSeqSymmetry) insym).getOriginalSymmetry());
      } else {
        gl.setInfo(insym);
      }
    }
  }
   
  	/** An efficient method to transform a single span. */
	private SeqSpan transformForViewSeq(SeqMapView gviewer, SeqSymmetry insym) {
		der.clear();
		// copy the span into derSpan
		insym.getSpan(gviewer.getAnnotatedSeq(), derSpan);
		der.addSpan(derSpan);
		
		if (gviewer.getAnnotatedSeq() != gviewer.getViewSeq()) {
			SeqUtils.transformSymmetry(der, gviewer.getTransformPath());
		}
		return gviewer.getViewSeqSpan(der);
	}
  
  public StringBuffer appendXML(String indent, StringBuffer sb) {
    sb.append(indent).append('<').append(NAME);
    XmlStylesheetParser.appendAttribute(sb, ATT_TYPE, type);
    XmlStylesheetParser.appendAttribute(sb, ATT_POSITION, position);
    sb.append(">\n");
    if (this.propertyMap != null) {
      propertyMap.appendXML(indent + "  ", sb);
    }
    
    if (this.enclosedGlyphElements != null) {
		for (GlyphElement kid : enclosedGlyphElements) {
       kid.appendXML(indent + "  ", sb);
      }
    }
    
    if (childrenElement != null) {
      childrenElement.appendXML(indent + "  ", sb);
    }

    sb.append(indent).append("</").append(NAME).append(">\n");
    return sb;
  }
}
