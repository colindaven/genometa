/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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
package com.affymetrix.igb.glyph;

import com.affymetrix.genoviz.bioviews.GlyphI;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.DerivedSeqSymmetry;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SupportsCdsSpan;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.UcscBedSym;
import com.affymetrix.genometryImpl.UcscGeneSym;
import com.affymetrix.genometryImpl.symloader.BAM;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.parsers.TrackLineParser;
import com.affymetrix.genoviz.glyph.ArrowGlyph;

import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.view.SeqMapView;

import java.awt.Color;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;
//MPTAG Added for changes

/**
 *
 * @version $Id: GenericAnnotGlyphFactory.java 6892 2010-09-13 15:58:00Z hiralv $
 */
public final class GenericAnnotGlyphFactory implements MapViewGlyphFactoryI {

	private static final boolean DEBUG = false;
	/** Set to true if the we can assume the container SeqSymmetry being passed
	 *  to addLeafsToTier has all its leaf nodes at the same depth from the top.
	 */
	private static Class default_eparent_class = (new EfficientLineContGlyph()).getClass();//MPTAG Glyphen die verwendet werden
	private static Class default_echild_class = (new DirectionFillRectGlyph()).getClass();//MPTAG Glyphen die verwendet werden
//	private static Class default_echild_class = (new ArrowGlyph()).getClass();//MPTAG Glyphen die verwendet werden
	private static Class default_elabelled_parent_class = (new EfficientLabelledLineGlyph()).getClass();//MPTAG Glyphen die verwendet werden
	// BFTAG changed
	public static final int DEFAULT_THICK_HEIGHT = 3;
	public static final int DEFAULT_THIN_HEIGHT = 2;
	private SeqMapView gviewer;
	private Class parent_glyph_class;
	private Class child_glyph_class;
	private final Class parent_labelled_glyph_class;

	//MPTAG added
	private static Image texture;
	public static final double GLYPH_HEIGHT = 2.;

	public GenericAnnotGlyphFactory() {
		//MPTAG added
		try{
			File f = new File("./igb/resources/Schattierung.png");
			if(!f.exists())
				throw new RuntimeException("Failed loading Image"+ f.getAbsolutePath());
			texture = ImageIO.read(f);
	//			System.getProperty("user.dir")+System.getProperty("path.seperator")+"testimage.jpg");
		}catch (IOException e){
			System.out.println(e);
		}
		parent_glyph_class = default_eparent_class;
		child_glyph_class = default_echild_class;
		parent_labelled_glyph_class = default_elabelled_parent_class;
	}

	public void init(Map options) {
		if (DEBUG) {
			System.out.println("     @@@@@@@@@@@@@     in GenericAnnotGlyphFactory.init(), props: " + options);
		}

		String parent_glyph_name = (String) options.get("parent_glyph");
		if (parent_glyph_name != null) {
			try {
				parent_glyph_class = Class.forName(parent_glyph_name);
			} catch (Exception ex) {
				System.err.println();
				System.err.println("WARNING: Class for parent glyph not found: " + parent_glyph_name);
				System.err.println();
				parent_glyph_class = default_eparent_class;
			}
		}
		String child_glyph_name = (String) options.get("child_glyph");
		if (child_glyph_name != null) {
			try {
				child_glyph_class = Class.forName(child_glyph_name);
			} catch (Exception ex) {
				System.err.println();
				System.err.println("WARNING: Class for child glyph not found: " + child_glyph_name);
				System.err.println();
				child_glyph_class = default_echild_class;
			}
		}
	}

	public void createGlyph(SeqSymmetry sym, SeqMapView smv) {//MPTAG
		gviewer = smv;

		String meth = BioSeq.determineMethod(sym);

		if (meth != null) {
			ITrackStyleExtended style = DefaultStateProvider.getGlobalStateProvider().getAnnotStyle(meth);
			int glyph_depth = style.getGlyphDepth();
			TierGlyph[] tiers = smv.getTiers(false, style);
			tiers[0].setInfo(sym);
			tiers[1].setInfo(sym);
			if (style.getSeparate()) {
				addLeafsToTier(sym, tiers[0], tiers[1], glyph_depth);
			} else {
				// use only one tier
				addLeafsToTier(sym, tiers[0], tiers[0], glyph_depth);
			}
		} else {  // keep recursing down into child syms if parent sym has no "method" property
			int childCount = sym.getChildCount();
			for (int i = 0; i < childCount; i++) {
				SeqSymmetry childSym = sym.getChild(i);
				createGlyph(childSym, gviewer);
			}
		}
	}

	private static int getDepth(SeqSymmetry sym) {
		int depth = 1;
		SeqSymmetry current = sym;
		while (current.getChildCount() != 0) {
			current = current.getChild(0);
			depth++;
		}
		return depth;
	}

	private void addLeafsToTier(SeqSymmetry sym,
			TierGlyph ftier, TierGlyph rtier,
			int desired_leaf_depth) {
		int depth = getDepth(sym);
		if (depth > desired_leaf_depth || sym instanceof TypeContainerAnnot) {
			int childCount = sym.getChildCount();
			for (int i = 0; i < childCount; i++) {
				addLeafsToTier(sym.getChild(i), ftier, rtier, desired_leaf_depth);
			}
		} else {  // depth == desired_leaf_depth
			addToTier(sym, ftier, rtier, (depth >= 2));
		}
	}

	/**
	 *  @param parent_and_child  Whether to draw this sym as a parent and
	 *    also draw its children, or to just draw the sym itself
	 *   (using the child glyph style).  If this is set to true, then
	 *    the symmetry must have a depth of at least 2.
	 */
	private void addToTier(SeqSymmetry insym,
			TierGlyph forward_tier,
			TierGlyph reverse_tier,
			boolean parent_and_child) {
		try {
			AffyTieredMap map = gviewer.getSeqMap(); //MPTAG enthält LabelGlyphs, die die Linke beschriftung beinhalten
			BioSeq annotseq = gviewer.getAnnotatedSeq();
			BioSeq coordseq = gviewer.getViewSeq();
			SeqSymmetry sym = insym;

			if (annotseq != coordseq) {
				sym = gviewer.transformForViewSeq(insym, annotseq);
			}

			SeqSpan pspan = gviewer.getViewSeqSpan(sym);
			if (pspan == null || pspan.getLength() == 0) {
				return;
			}  // if no span corresponding to seq, then return;

			TierGlyph the_tier = pspan.isForward() ? forward_tier : reverse_tier;

			// I hate having to do this cast to IAnnotStyleExtended.  But how can I avoid it?
			ITrackStyleExtended the_style = (ITrackStyleExtended) the_tier.getAnnotStyle();

			the_tier.addChild(determinePGlyph(
					parent_and_child, insym, the_style, the_tier, pspan, map, sym, annotseq, coordseq));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private GlyphI determinePGlyph(
			boolean parent_and_child, SeqSymmetry insym,
			ITrackStyleExtended the_style, TierGlyph the_tier, SeqSpan pspan,
			AffyTieredMap map, SeqSymmetry sym,
			BioSeq annotseq, BioSeq coordseq)
			throws InstantiationException, IllegalAccessException {//MPTAG Hier sollte die Richtungsglypen gesetzt werden
		GlyphI pglyph = null;
		if (parent_and_child && insym.getChildCount() > 0) {
			pglyph = determineGlyph(parent_glyph_class, parent_labelled_glyph_class, the_style, insym, the_tier, pspan, map, sym);
			// call out to handle rendering to indicate if any of the children of the
			//    original annotation are completely outside the view
			addChildren(insym, sym, the_style, annotseq, pglyph, map, coordseq);
			handleAlignedResidueGlyphs(insym, the_style, annotseq, pglyph);//MPTAG added Style mit übergeben
		} else {
			// depth !>= 2, so depth <= 1, so _no_ parent, use child glyph instead...
			pglyph = determineGlyph(child_glyph_class, parent_labelled_glyph_class, the_style, insym, the_tier, pspan, map, sym);
		}
		return pglyph;
	}

	private static GlyphI determineGlyph(
			Class glyphClass, Class labelledGlyphClass,
			ITrackStyleExtended the_style, SeqSymmetry insym, TierGlyph the_tier,
			SeqSpan pspan, AffyTieredMap map, SeqSymmetry sym)
			throws IllegalAccessException, InstantiationException {
		GlyphI pglyph = null;
		// Note: Setting parent height (pheight) larger than the child height (cheight)
		// allows the user to select both the parent and the child as separate entities
		// in order to look at the properties associated with them.  Otherwise, the method
		// EfficientGlyph.pickTraversal() will only allow one to be chosen.
//		double pheight = DEFAULT_THICK_HEIGHT + 0.0001;
		double pheight = GLYPH_HEIGHT;//MPTAG
		String label_field = the_style.getLabelField();
		boolean use_label = label_field != null && (label_field.trim().length() > 0);
		if (use_label) {
			EfficientLabelledGlyph lglyph = (EfficientLabelledGlyph) labelledGlyphClass.newInstance();
			Object property = getTheProperty(insym, label_field);
			String label = (property == null) ? "" : property.toString();
			if (the_tier.getDirection() == TierGlyph.Direction.REVERSE) {
				lglyph.setLabelLocation(GlyphI.SOUTH);
			} else {
				lglyph.setLabelLocation(GlyphI.NORTH);
			}
			lglyph.setLabel(label);
			pheight = 2 * pheight;
			pglyph = lglyph;
		} else {
			pglyph = (GlyphI) glyphClass.newInstance();
		}
		pglyph.setCoords(pspan.getMin(), 0, pspan.getLength(), pheight);
		pglyph.setColor(getSymColor(insym, the_style));
		map.setDataModelFromOriginalSym(pglyph, sym);
		return pglyph;
	}

	private static Object getTheProperty(SeqSymmetry sym, String prop) {
		if (prop == null || (prop.trim().length() == 0)) {
			return null;
		}
		SeqSymmetry original = getMostOriginalSymmetry(sym);

		if (original instanceof SymWithProps) {
			return ((SymWithProps) original).getProperty(prop);
		}
		return null;
	}

	private static SeqSymmetry getMostOriginalSymmetry(SeqSymmetry sym) {
		if (sym instanceof DerivedSeqSymmetry) {
			return getMostOriginalSymmetry(((DerivedSeqSymmetry) sym).getOriginalSymmetry());
		}
		return sym;
	}

	private void addChildren(
			SeqSymmetry insym, SeqSymmetry sym, ITrackStyleExtended the_style, BioSeq annotseq,
			GlyphI pglyph, AffyTieredMap map, BioSeq coordseq)
			throws InstantiationException, IllegalAccessException {
		SeqSpan cdsSpan = null;
		SeqSymmetry cds_sym = null;
		boolean same_seq = annotseq == coordseq;
		if ((insym instanceof SupportsCdsSpan) && ((SupportsCdsSpan) insym).hasCdsSpan()) {
			cdsSpan = ((SupportsCdsSpan) insym).getCdsSpan();
			MutableSeqSymmetry tempsym = new SimpleMutableSeqSymmetry();
			tempsym.addSpan(new SimpleMutableSeqSpan(cdsSpan));
			if (!same_seq) {
				SeqUtils.transformSymmetry(tempsym, gviewer.getTransformPath());
				cdsSpan = gviewer.getViewSeqSpan(tempsym);
			}
			cds_sym = tempsym;
		}
		// call out to handle rendering to indicate if any of the children of the
		//    orginal annotation are completely outside the view

		int childCount = sym.getChildCount();
		List<SeqSymmetry> outside_children = new ArrayList<SeqSymmetry>();
		for (int i = 0; i < childCount; i++) {
			SeqSymmetry child = sym.getChild(i);
			SeqSpan cspan = gviewer.getViewSeqSpan(child);
			if (cspan == null) {
				// if no span for view, then child is either to left or right of view
				outside_children.add(child); // collecting children outside of view to handle later
			} else {
				GlyphI cglyph;
				if (cspan.getLength() == 0) {
					cglyph = new DeletionGlyph();
				} else {
					cglyph = (GlyphI) child_glyph_class.newInstance();
					//Hier wird die Kindglyphe erzeugt wenn sie sichtbar ist
					//Diese ist aber noch nicht die Glyphe mit ATGC Drauf
				}

				Color child_color = getSymColor(child, the_style);
				//MPTAG added versuchen aus der Elternglyphe herauszubekommen ob diese schon eine Richtung hat
//				try{
//				if(getDirectionOfGlyph(insym) > 0){
//					if(getDirectionOfGlyph(insym) == 1){
//						child_color = ((TrackStyle)the_style).getForwardColor();
//					}else if(getDirectionOfGlyph(insym) == 2){
//						child_color = ((TrackStyle)the_style).getReverseColor();
//					}
//				}
//				}catch (Exception e){
//					System.out.println("Exception bei Zuweisung der Kindfarbe in GenericAnnotationGlyphFactory.addChildren()");
//				}
				double cheight = handleCDSSpan(cdsSpan, cspan, cds_sym, child, annotseq, same_seq, child_color, pglyph, map);
				cglyph.setCoords(cspan.getMin(), 0, cspan.getLength(), cheight);
				cglyph.setColor(child_color);
				pglyph.addChild(cglyph);
				map.setDataModelFromOriginalSym(cglyph, child);
			}
		}
		// call out to handle rendering to indicate if any of the children of the
		//    orginal annotation are completely outside the view
//		DeletionGlyph.handleEdgeRendering(outside_children, pglyph, annotseq, coordseq, 0.0, DEFAULT_THIN_HEIGHT);//MPTAG
		DeletionGlyph.handleEdgeRendering(outside_children, pglyph, annotseq, coordseq, 0.0, GLYPH_HEIGHT);
	}

	/*
	 * Wird mehrfach aufgerufen. Für jeweis die unterscheidlichen Zoomstufen
	 */
	private static Color getSymColor(SeqSymmetry insym, ITrackStyleExtended style) {//MPTAG heir ggf farbe für Richtung setzten
		boolean use_score_colors = style.getColorByScore();
		boolean use_item_rgb = "on".equalsIgnoreCase((String) style.getTransientPropertyMap().get(TrackLineParser.ITEM_RGB));

		if (!(use_score_colors || use_item_rgb)) {
			//return style.getColor();
			if(getDirectionOfGlyph(insym)>0){
				if(getDirectionOfGlyph(insym) == 1){
					if(style instanceof TrackStyle){
						return ((TrackStyle)style).getForwardColor();//MPTAG TODO globale Farbe für richtung holen
					}else{
						return Color.RED;
					}
				}else if(getDirectionOfGlyph(insym) == 2){
					if(style instanceof TrackStyle){
						return ((TrackStyle)style).getReverseColor();//MPTAG TODO globale Farbe für richtung holen
					}else{
						return Color.BLUE;
					}
				}
			}else{
				return style.getColor();
			}
		}

		SeqSymmetry sym = insym;
		if (insym instanceof DerivedSeqSymmetry) {
			sym = (SymWithProps) getMostOriginalSymmetry(insym);
		}

		if (use_item_rgb && sym instanceof SymWithProps) {
			Color cc = (Color) ((SymWithProps) sym).getProperty(TrackLineParser.ITEM_RGB);
			if (cc != null) {
				return cc;
			}
		}
		if (use_score_colors && sym instanceof Scored) {
			float score = ((Scored) sym).getScore();
			if (score != Float.NEGATIVE_INFINITY && score > 0.0f) {
				return style.getScoreColor(score);
			}
		}

		return style.getColor();
	}

	private double handleCDSSpan(
			SeqSpan cdsSpan, SeqSpan cspan, SeqSymmetry cds_sym,
			SeqSymmetry child, BioSeq annotseq, boolean same_seq,
			Color child_color, GlyphI pglyph, AffyTieredMap map)
			throws IllegalAccessException, InstantiationException {
		if (cdsSpan == null || SeqUtils.contains(cdsSpan, cspan)) {
//			return DEFAULT_THICK_HEIGHT;//MPTAG
			return GLYPH_HEIGHT;
		}
		if (SeqUtils.overlap(cdsSpan, cspan)) {
			SeqSymmetry cds_sym_2 = SeqUtils.intersection(cds_sym, child, annotseq);
			if (!same_seq) {
				cds_sym_2 = gviewer.transformForViewSeq(cds_sym_2, annotseq);
			}
			SeqSpan cds_span = gviewer.getViewSeqSpan(cds_sym_2);
			if (cds_span != null) {
				GlyphI cds_glyph;
				if (cspan.getLength() == 0) {
					cds_glyph = new DeletionGlyph();
				} else {
					System.out.println("Added glyph: "+ child_glyph_class.getCanonicalName());
					cds_glyph = (GlyphI) child_glyph_class.newInstance();
				}
//				cds_glyph.setCoords(cds_span.getMin(), 0, cds_span.getLength(), DEFAULT_THICK_HEIGHT);
				cds_glyph.setCoords(cds_span.getMin(), 0, cds_span.getLength(), GLYPH_HEIGHT);
				cds_glyph.setColor(child_color); // CDS same color as exon
				pglyph.addChild(cds_glyph);
				map.setDataModelFromOriginalSym(cds_glyph, cds_sym_2);
			}
		}
//		return DEFAULT_THIN_HEIGHT;
		return GLYPH_HEIGHT;
	}

	/**
	 * @param sym
	 * @param annotseq
	 * @param pglyph Die Vaterglyphe, für deren Kinder die Residues gesetzt werden.
	 */
	private static void handleAlignedResidueGlyphs(SeqSymmetry sym, ITrackStyleExtended the_style, BioSeq annotseq, GlyphI pglyph) {//MPTAG changed ITrackstyle hinzugefügt
		if (!(sym instanceof SymWithProps)) {
			return;
		}

		boolean handleCigar = sym instanceof UcscBedSym;

		// We are in an aligned residue glyph.
		int childCount = sym.getChildCount();
		if (childCount > 0) {
			int startPos = 0;
			for (int i = 0; i < childCount; i++) {
				startPos = setResidues(sym.getChild(i),	the_style, annotseq, pglyph, startPos, handleCigar, true);//MPTAG changed ITrackstyle hinzugefügt
			}
		} else {
			setResidues(sym, the_style, annotseq, pglyph, 0, false, false);//MPTAG changed ITrackstyle hinzugefügt
			// Note that pglyph is replaced here.
			// don't need to process cigar, since entire residue string is used
		}
	}

	/**
	 * Determine and set the appropriate residues for this element.
	 * @param sym
	 * @param annotseq
	 * @param pglyph
	 * @param startPos - starting position of the current child in the residues string
	 * @param handleCigar - indicates whether we need to process the cigar element.
	 * @return
	 *///MPTAG Hier wird aufgerufen das der Text auf die Glyphe gesetzt werden soll
	//MPTAG changed ITrackstyle hinzugefügt
	private static int setResidues(SeqSymmetry sym, ITrackStyleExtended the_style, BioSeq annotseq, GlyphI pglyph, int startPos, boolean handleCigar, boolean isChild) {
		SeqSpan span = sym.getSpan(annotseq);
		if (span == null) {
			return startPos;
		}

		if (!(sym instanceof SymWithProps)) {
			return startPos;
		}

		Object residues = ((SymWithProps) sym).getProperty(BAM.RESIDUESPROP);

		if (residues == null) {
			return startPos;
		}

		AlignedResidueGlyph csg = null;
		if (residues != null) {
			String residueStr = residues.toString();
			if (handleCigar) { //MPTAG Wenn BED Format verwendet wird
				Object cigar = ((SymWithProps) sym).getProperty(BAM.CIGARPROP);
				residueStr = BAM.interpretCigar(cigar, residueStr, startPos, span.getLength());
				startPos += residueStr.length();
			}
			csg = new AlignedResidueGlyph();
			//MPTAG added
			csg.setDirection(getDirectionOfGlyph(sym));
			csg.setResidues(residueStr);
			if (annotseq.getResidues(span.getStart(), span.getEnd()) != null) {
				if (handleCigar) {//MPTAG Wenn BED Format verwendet wird
					csg.setResidueMask(annotseq.getResidues(span.getMin(), span.getMin() + residueStr.length()));
//					System.out.println("Residue set WITH BED");
				} else {
					csg.setResidueMask(annotseq.getResidues(span.getMin(), span.getMax()));
//					System.out.println("Residue set without BED");
				}
			}
			csg.setHitable(false);
			csg.setCoords(span.getMin(), 0, span.getLengthDouble(), pglyph.getCoordBox().height);
			if (isChild) {
				pglyph.addChild(csg);
			} else {
				pglyph = csg;	// dispense with extra glyph, which is just overwritten when drawing.
			}

			// SEQ array has unexpected behavior;  commenting out for now.
			/*if (((SymWithProps) sym).getProperty("SEQ") != null) {
			byte[] seqArr = (byte[]) ((SymWithProps) sym).getProperty("SEQ");
			for (int i = 0; i < seqArr.length; i++) {
			System.out.print((char) seqArr[i]);
			}
			System.out.println();
			csg.setResidueMask(seqArr);
			}*/
		}

		return startPos;
	}

	/**
	 * MPTAG
	 * Methode zum Finden der Richtung der Glyphe. Arbeitet zZt. nur auf Glypen die eine
	 * SymWithProps haben bei der eine Proerty "forward" als boolean hinterlegt ist.
	 * @param sym Die Sym aus der Versucht wird die Daten für die Richtung zu extrahieren.
	 * @return Eine Richtung als Short, 0=None, 1=forward, 2=reverse
	 */
	private static short getDirectionOfGlyph(SeqSymmetry insym){
		SeqSymmetry sym = insym;
		if (insym instanceof DerivedSeqSymmetry) {
			sym = (SymWithProps) getMostOriginalSymmetry(insym);
		}
		if(sym instanceof SymWithProps){
			if(((SymWithProps)sym).getProperty("forward")!= null){
				if(((Boolean)((SymWithProps)sym).getProperty("forward"))==true){
					return 1;
				}else{
					return 2;
				}
			}
		}
		//Sehr besonderer Fall, den ich aber schon mehrfach gesehen habe.
		// UcscGeneSym hat ein feld forward, welches direkt abgefreagt werden kann
		if(insym instanceof UcscGeneSym){
			if(((UcscGeneSym) insym).isForward()){
				return 1;
			}else{
				return 2;
			}
		}
		return 0;
	}

	//MPTAG added
	public static Image getTexture(){
		return texture;
	}
}
