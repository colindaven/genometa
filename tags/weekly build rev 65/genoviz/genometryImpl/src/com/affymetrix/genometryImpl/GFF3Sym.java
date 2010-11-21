/**
 *   Copyright (c) 2006-2007 Affymetrix, Inc.
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
package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.parsers.GFF3Parser;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.*;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 *  A sym to efficiently store GFF version 3 annotations.
 *
 *  See http://song.sourceforge.net/gff3.shtml
 *
 * @version $Id: GFF3Sym.java 6687 2010-08-17 19:06:44Z hiralv $
 */
public final class GFF3Sym extends SimpleSymWithProps implements Scored, SupportsCdsSpan, Cloneable {
	private String id;
	private static boolean multipleCdsWarning = false;

	public static final char UNKNOWN_FRAME = UcscGffSym.UNKNOWN_FRAME;
	public static final String UNKNOWN_SOURCE = ".";

	// Assuming that these feature types are not case-sensitive
	public static final String FEATURE_TYPE_GENE = "gene";
	public static final String FEATURE_TYPE_MRNA = "mrna";
	public static final String FEATURE_TYPE_EXON = "exon";
	public static final String FEATURE_TYPE_CDS = "cds";
	public static final String FEATURE_TYPE_CHROMOSOME = "chromosome";

	// Assuming that these ontology types are not case-sensitive
	public static final String SOFA_GENE = "SO:0000704";
	public static final String SOFA_MRNA = "SO:0000234";
	public static final String SOFA_EXON = "SO:0000147";
	public static final String SOFA_CDS = "SO:0000316";

	private static final Pattern equalsP = Pattern.compile("=");
	private static final Pattern commaP = Pattern.compile(",");

	private static final String[] EMPTY_RESULT = new String[0];

	private static final List<String> bad_prop_names = Arrays.asList(new String[] {
			"feature_type", "type", "score", "frame"
			});

	private String source;
	private String method;
	public String feature_type;
	private final float score;
	private final char frame;
	private final String attributes;

	
	/**
	 * Constructor.
	 * The coordinates should be given exactly as they appear in a GFF file.
	 * In principle, the first coordinate is supposed to be less than the second one,
	 * but in practice this isn't always followed, so this constructor will correct
	 * those errors and will also convert from base-1 to interbase-0 coordinates.
	 * @param source
	 * @param feature_type
	 * @param score
	 * @param frame
	 * @param attributes Attributes, formatted in GFF3 style.
	 */
	public GFF3Sym(String source, String feature_type,
			float score, char frame, String attributes) {
		super();

		if (! UNKNOWN_SOURCE.equals(source)) {
			this.source = source;
		} else {
			this.source = UNKNOWN_SOURCE;  // Basically equivalent to this.source = source.intern()
		}
		this.method = null;
		this.feature_type = feature_type;
		this.score = score;
		this.frame = frame;
		this.attributes = attributes;

		// in GFF3, the property "ID" is intended to have meaning only inside the file itself.
		// the property "Name" is more like what we think of as an ID in Genometry
		String[] possible_names = getGFF3PropertyFromAttributes(GFF3Parser.GFF3_NAME, attributes);
		if (possible_names.length > 0) {
			this.id = possible_names[0];
		} else {
			this.id = null;
		}
	}

	/**
	 * Return the ID.  Unknown if the original contract allows null to be
	 * returned, but this class and users of this class assume it can
	 * return null.
	 *
	 * @return ID or null
	 */
	@Override
		public String getID() {
			// This is overridden because we only want to check the value of this.id,
			// we do NOT want to check for a property named "id".  This is because GFF3
			// has a very different notion of what an ID is.  In GFF3 and "ID", in upper case,
			// only has meaning while processing the file and should be ignored later.
			if (this.id == null) {
				return null;
			} else {
				return this.id.toString();
			}
		}
	public String getSource()  { return source; }
	public String getFeatureType()  { return feature_type; }
	public float getScore()  { return score; }
	public char getFrame()  { return frame; }
	public String getAttributes() { return attributes; }

	@Override
	public Object getProperty(String name) {
		if (name.equals("source") && source != null) { return source; }
		else if (name.equals("method")) { return method; }
		else if (name.equals("feature_type") || name.equals("type")) { return feature_type; }
		else if (name.equals("score") && score != UNKNOWN_SCORE) { return new Float(score); }
		else if (name.equals("frame") && frame != UNKNOWN_FRAME) { return Character.valueOf(frame); }
		else if (name.equals("id")) {
			return getID();
		}
		String[] temp = getGFF3PropertyFromAttributes(name, attributes);
		if (temp.length == 0) {
			return null;
		} else if (temp.length == 1) {
			return temp[0];
		} else {
			return temp;
		}
	}

	/**
	 *  Overridden such that certain properties will be stored more efficiently.
	 *  Setting certain properties this way is not supported:
	 *  these include "attributes", "score" and "frame".
	 */
	@Override
	public boolean setProperty(String name, Object val) {
		String lc_name = name.toLowerCase();
		if (name.equals("id")) {
			if (val instanceof String) {
				id = (String) val;
				return true;
			}
			else {
				//id = null;
				return false;
			}
		}
		if (name.equals("source")) {
			if (val instanceof String) {
				source = (String) val;
				return true;
			}
			else {
				//source = null;
				return false;
			}
		}
		if (name.equals("method")) {
			if (val instanceof String) {
				method = (String) val;
				return true;
			}
			else {
				//method = null;
				return false;
			}
		}
		else if (bad_prop_names.contains(lc_name)) {
			// May need to handle these later, but it is unlikely to be an issue
			throw new IllegalArgumentException("Currently can't modify property '" + name +"' via setProperty");
		}

		return super.setProperty(name, val);
	}

	@Override
	public Map<String,Object> getProperties() {
		return cloneProperties();
	}

	@Override
	public Map<String,Object> cloneProperties() {
		Map<String,Object> tprops = super.cloneProperties();
		if (tprops == null) {
			tprops = new HashMap<String,Object>();
		}
		if (getID() != null) {
			tprops.put("id", getID());
		}
		if (source != null) {
			tprops.put("source", source);
		}
		if (method != null) {
			tprops.put("method", method);
		}
		if (feature_type != null) {
			tprops.put("feature_type", feature_type);
			tprops.put("type", feature_type);
		}
		if (score != UNKNOWN_SCORE) {
			tprops.put("score", new Float(getScore()));
		}
		if (frame != UNKNOWN_FRAME) {
			tprops.put("frame", Character.valueOf(frame));
		}
		addAllAttributesFromGFF3(tprops, attributes);

		return tprops;
	}

	/** Returns the property GFF3Parser.GFF3_ID from the attributes.
	 *  This will be a single String or null.  This ID is intended to be used
	 *  during processing of the GFF3 file, and has no meaning outside the file.
	 */
	public static String getIdFromGFF3Attributes(String attributes) {
		String[] possible_ids = getGFF3PropertyFromAttributes(GFF3Parser.GFF3_ID, attributes);
		if (possible_ids.length == 0) {
			return null;
		} else {
			return possible_ids[0];
		}
	}

	private static void addAllAttributesFromGFF3(Map<String,Object> m, String attributes) {
		if (attributes == null) {
			return;
		}

		String[] tag_vals = attributes.split(";");

		for (int i=0; i<tag_vals.length; i++) {
			if ("".equals(tag_vals[i])) {
				continue;
			}
			String[] tag_and_vals = equalsP.split(tag_vals[i], 2);
			if (tag_and_vals.length == 2) {
				String[] vals = commaP.split(tag_and_vals[1]);
				for (int j=0; j<vals.length; j++) {
					vals[j] = GeneralUtils.URLDecode(vals[j]);
				}
				if (vals.length == 1) { // put a single String
					m.put(tag_and_vals[0], vals[0]);
				} else { // put a String array
					m.put(tag_and_vals[0], vals);
				}
			}
		}
	}

	/** Returns a non-null String[]. */
	public static String[] getGFF3PropertyFromAttributes(String prop_name, String attributes) {
		if (attributes == null) {
			return EMPTY_RESULT;
		}
		String[] tag_vals = attributes.split(";");
		String prop_with_equals = prop_name + "=";
		String val = null;

		for (int i=0; i<tag_vals.length; i++) {
			if (tag_vals[i].startsWith(prop_with_equals)) {
				val = tag_vals[i].substring(prop_with_equals.length());
				break;
			}
		}
		if (val == null) {
			return EMPTY_RESULT;
		}
		String[] results = val.split(",");
		for (int i=0; i<results.length; i++) {
			results[i] = GeneralUtils.URLDecode(results[i]);
		}
		return results;
	}

	/**
	 *  Converts feature types that IGB understands into one of the constant strings:
	 *  {@link #FEATURE_TYPE_GENE}, etc.  Invalid ones, are simply interned.
	 */
	public static String normalizeFeatureType(String s) {

		if (FEATURE_TYPE_GENE.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_GENE;
		}
		if (FEATURE_TYPE_EXON.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_EXON;
		}
		if (FEATURE_TYPE_MRNA.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_MRNA;
		}
		if (FEATURE_TYPE_CDS.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_CDS;
		}
		if (FEATURE_TYPE_CHROMOSOME.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_CHROMOSOME;
		}

		if (SOFA_GENE.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_GENE;
		}
		if (SOFA_EXON.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_EXON;
		}
		if (SOFA_MRNA.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_MRNA;
		}
		if (SOFA_CDS.equalsIgnoreCase(s)) {
			return FEATURE_TYPE_CDS;
		}

		return s.intern();
	}

	@Override
	public String toString() {
		return "GFF3Sym: ID = '" + getProperty(GFF3Parser.GFF3_ID) + "'  type=" + feature_type
			+ " children=" + getChildCount();
	}

	public boolean hasCdsSpan() {
		for(SeqSymmetry child : children) {
			if (isCdsSym(child)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isCdsSym(SeqSymmetry sym) {
		return sym instanceof GFF3Sym
				&& ((GFF3Sym) sym).getFeatureType().equals(GFF3Sym.FEATURE_TYPE_CDS);
	}

	/**
	 * TODO: this does not take into account multiple CDS for a single mRNA nor
	 *       does it make use of the 5' and 3' UTR or multiple CDS regions on a
	 *       single mRNA.
	 *
	 * TODO: Most of this should be precomputed in the addChild() or something
	 *       so we do not need to compute it every time it is requested.
	 *
	 * @return A single SeqSpan covering the CDS region.
	 */
	public SeqSpan getCdsSpan() {
		/* This can be null but Maps can store null keys */
		String gff3ID;
		Map<String,MutableSeqSpan> cdsSpans = new LinkedHashMap<String,MutableSeqSpan>();
		MutableSeqSpan span = null;
		
		for(SeqSymmetry child : children) {
			if (isCdsSym(child)) {
				gff3ID = getIdFromGFF3Attributes(((GFF3Sym)child).getAttributes());
				for(int i = 0; i < child.getSpanCount(); i++) {
					span = cdsSpans.get(gff3ID);
					if (span == null) {
						span = new SimpleMutableSeqSpan(child.getSpan(i));
						cdsSpans.put(gff3ID, span);
					} else {
						SeqUtils.encompass(child.getSpan(i), span, span);
					}
				}
			}
		}

		if (cdsSpans.isEmpty()) {
			throw new IllegalArgumentException("This Symmetry does not have a CDS");
		} else if (cdsSpans.size() > 1){
			Logger.getLogger(
					this.getClass().getName()).log(Level.WARNING,
					"Multiple CDS spans detected.  Skipping remaining CDS spans.  (found {0} spans for {1})",
					new Object[] {Integer.valueOf(cdsSpans.size()),
					getIdFromGFF3Attributes(attributes)});

			if (!multipleCdsWarning) {
				multipleCdsWarning = !multipleCdsWarning;
				SwingUtilities.invokeLater(new Runnable() {
					public void run() {
						/* TODO: This should use StringUtils.wrap() */
						JOptionPane.showMessageDialog(null,
								"Multiple CDS regions for a shared parent have been\ndetected in a GFF3 file.  Only the first CDS region\nencountered will be displayed.  This is a known\nlimitation of the GFF3 parser.",
								"Multiple CDS Regions Detected",
								JOptionPane.WARNING_MESSAGE);
					}
				});
			}
		}

		return cdsSpans.entrySet().iterator().next().getValue();
	}

	/*
	 *Returns Map of id to list of symmetries of cds spans.
	 */
	public Map<String, List<SeqSymmetry>> getCdsSpans() {
		String gff3ID;
		Map<String, List<SeqSymmetry>> cdsSpans = new LinkedHashMap<String, List<SeqSymmetry>>();
		MutableSeqSpan span = null;

		for (SeqSymmetry child : children) {
			if (isCdsSym(child)) {
				gff3ID = getIdFromGFF3Attributes(((GFF3Sym) child).getAttributes());
				for (int i = 0; i < child.getSpanCount(); i++) {
					List<SeqSymmetry> list = cdsSpans.get(gff3ID);
					if (list == null) {
						list = new ArrayList<SeqSymmetry>();
						cdsSpans.put(gff3ID, list);
						list.add(child);
					}
				}
			}
		}
		return cdsSpans;
	}

	/**
	 * Removes all cds symmetries.
	 */
	public void removeCdsSpans(){
		List<SeqSymmetry> remove_list = new ArrayList<SeqSymmetry>();
		for(SeqSymmetry child : children) {
			if (isCdsSym(child)) {
				remove_list.add(child);
			}
		}
		children.removeAll(remove_list);
	}

	@Override
	public Object clone() {
		GFF3Sym dup = new GFF3Sym(this.source, this.feature_type, this.score, this.frame, this.attributes);
		if (children != null) {
			for (SeqSymmetry child : children) {
				dup.addChild(child);
			}
		}
		if (spans != null) {
			for (SeqSpan span : spans) {
				dup.addSpan(span);
			}
		}
		dup.props = this.cloneProperties();
		dup.method = this.method;
		
		return dup;
	}
}
