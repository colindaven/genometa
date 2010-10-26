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

package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.TypedSym;
import com.affymetrix.genometryImpl.style.HeatMap;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Paint;
import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

/**
 *  A parser designed to parse data from cytoBand.txt files from UCSC.
 */
public final class CytobandParser implements AnnotationWriter  {

	static Pattern line_regex = Pattern.compile("\\t");

	static final HeatMap cyto_heat_map = HeatMap.makeLinearHeatmap("cytobands", Color.WHITE, Color.BLACK);
	//  static final Color cyto_acen_color = new Color(198,64,64); // dark red
	static final Color cyto_acen_color = new Color(198,96,96); // red
	static final Color cyto_stalk_color = new Color(128,128,160); // pale blue

	static public final Paint acen_paint = new GradientPaint(0, 0, Color.DARK_GRAY, 1, 1, Color.WHITE, true);
	static public final Paint stalk_paint = new GradientPaint(0, 1, Color.GRAY, 1, 0, Color.WHITE, true);

	/** This is the name that is used to identify the cytobands data.  It is not
	 *  intended to be displayed to the user.  It should be some String that they
	 *  are unlikely to want to use as the name of a tier.
	 */
	public static final String CYTOBAND_TIER_NAME = "__cytobands";
	public static final float GNEG_SCORE = 100.0f;
	public static final float GVAR_SCORE = 100.0f;
	public static final float ACEN_SCORE = 600.0f;
	public static final float STALK_SCORE = 500.0f;

	static List<String> pref_list = Arrays.asList("cyt");

	/**
	 *  if annotating seq, makes all parsed syms children of a containing SymWithProps and adds this single sym
	 *    to the seq (and if seq is a SmartAnnotBioSeq, it will in turn create a TypeContainerSym as parent of the
	 *    single SymWithProps)
	 */
	public List<SeqSymmetry> parse(InputStream dis, AnnotatedSeqGroup seq_group, boolean annotate_seq)
		throws IOException  {

		int band_alternator = 1; // toggles dark/light when band color is missing
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>(100);
		String line;
		Thread thread = Thread.currentThread();
		BufferedReader reader = new BufferedReader(new InputStreamReader(dis));
		Map<BioSeq,SeqSymmetry> seq2csym = new HashMap<BioSeq,SeqSymmetry>();
		while ((line = reader.readLine()) != null && (!thread.isInterrupted())) {
			if (line.charAt(0)=='#' || line.length() ==0) {  // skip comment lines
				continue;
			}
			String[] fields = line_regex.split(line);
			int field_count = fields.length;
			if (field_count < 4) {
				throw new IOException("Line has wrong number of data columns.");
			}

			String seq_name = fields[0]; // seq id field
			BioSeq seq = seq_group.getSeq(seq_name);
			if (seq == null) {
				seq = seq_group.addSeq(seq_name, 0);
			}

			int beg = Integer.parseInt(fields[1]);  // start field
			int end = Integer.parseInt(fields[2]);  // stop field
			String annot_name = new String(fields[3]);
			String band = null;
			if (field_count >= 5) {
				band = new String(fields[4]);
			} else {
				if (band_alternator > 0) {
					band = "gpos25";
				} else {
					band = "gpos75";
				}
				band_alternator = -band_alternator;
			}

			if (beg > seq.getLength()) {
				seq.setLength(beg);
			}
			if (end > seq.getLength()) {
				seq.setLength(end);
			}

			CytobandSym sym = new CytobandSym(beg, end, seq, annot_name, band);
			if (annotate_seq) {
				SimpleSymWithProps parent_sym = (SimpleSymWithProps) seq2csym.get(seq);
				if (parent_sym == null) {
					parent_sym = new SimpleSymWithProps();
					parent_sym.addSpan(new SimpleSeqSpan(0, seq.getLength(), seq));
					parent_sym.setProperty("method", CYTOBAND_TIER_NAME);
					parent_sym.setProperty("preferred_formats", pref_list);
					parent_sym.setProperty(SimpleSymWithProps.CONTAINER_PROP, Boolean.TRUE);
					seq2csym.put(seq, parent_sym);
					seq.addAnnotation(parent_sym);
				}
				parent_sym.addChild(sym);
				if (annot_name != null) {
					seq_group.addToIndex(annot_name, sym);
				}
			}
			results.add(sym);
		}
		return results;
	}

	public static final String BAND_STALK = "stalk";
	public static final String BAND_ACEN = "acen";

	private static float parseScore(String s) {
		if ("gneg".equals(s)) {
			return GNEG_SCORE;
		}
		if ("gvar".equals(s)) {
			return GVAR_SCORE;
		}
		if (BAND_ACEN.equals(s)) {
			return ACEN_SCORE;
		}
		if (BAND_STALK.equals(s)) {
			return STALK_SCORE;
		}
		if (s.startsWith("gpos")) {
			// "gpos50" == 500
			// "gpos100" == 1000
			// etc.
			float pos = 1000.0f;
			try {
				pos = 10.0f * Integer.parseInt(s.substring(4));
			} catch (NumberFormatException nfe) {
				pos = 1000.0f;
			}
			return pos;
		}
		return 0.0f;
	}

	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
			String type, OutputStream outstream) {
		System.out.println("in CytobandParser.writeAnnotations()");
		boolean success = true;
		try {
			Writer bw = new BufferedWriter(new OutputStreamWriter(outstream));
			Iterator<? extends SeqSymmetry> iterator = syms.iterator();
			while (iterator.hasNext()) {
				SeqSymmetry sym = (SeqSymmetry)iterator.next();
				writeCytobandFormat(bw, sym, seq);
			}
			bw.flush();
		}
		catch (Exception ex) {
			ex.printStackTrace();
			success = false;
		}
		return success;
	}

	public static void writeCytobandFormat(Writer out, SeqSymmetry sym, BioSeq seq)
		throws IOException  {
		if (sym instanceof CytobandSym) {
			CytobandSym cytosym = (CytobandSym)sym;
			SeqSpan span = cytosym.getSpan(seq);
			if (span != null) {
				out.write(seq.getID());
				out.write('\t');
				int min = span.getMin();
				int max = span.getMax();
				out.write(Integer.toString(min));
				out.write('\t');
				out.write(Integer.toString(max));
				out.write('\t');
				out.write(cytosym.getID());
				out.write('\t');
				out.write(cytosym.getBand());
				out.write('\n');
			}
		}
	}

	public String getMimeType()  { return "txt/plain"; }

	public static enum Arm {SHORT, LONG, UNKNOWN};

	public static final class CytobandSym extends SingletonSymWithProps implements Scored, TypedSym {
		String band;

		public CytobandSym(int start, int end, BioSeq seq, String name, String band) {
			super(start, end, seq);
			this.band = band;
			this.id = name;
		}

		public float getScore() {
			return parseScore(band);
		}

		public String getBand() {
			return band;
		}    

		public Arm getArm() {
			if (id.charAt(0) == 'p') {
				return Arm.SHORT;
			}
			if (id.charAt(0) == 'q') {
				return Arm.LONG;
			}
			return Arm.UNKNOWN;
		}

		public Color getColor() {
			if (BAND_ACEN.equals(band)) {
				return cyto_acen_color;
			}
			if (BAND_STALK.equals(band)) {
				return cyto_stalk_color;
			}
			float score = parseScore(band);
			return cyto_heat_map.getColors()[(int) (255 * 0.001 * score)];
		}

		/** Retrieves a color that will contrast well with {@link #getColor()}. */
		public Color getTextColor() {
			Color col = getColor();
			int intensity = col.getRed() + col.getGreen() + col.getBlue();
			if (intensity > 255+128) { return Color.BLACK; }
			else {
				return Color.WHITE;
			}
		}

		@Override
			public boolean setProperty(String name, Object val) {
				if ("band".equals(name)) {
					band = name;
				}
				return super.setProperty(name, val);
			}

		@Override
			public Object getProperty(String name) {
				if ("id".equals(name)) {
					return getID();
				}
				if ("method".equals(name)) {
					return CYTOBAND_TIER_NAME;
				}
				if ("band".equals(name)) {
					return band;
				}
				return super.getProperty(name);
			}

		@Override
			public Map<String,Object> cloneProperties() {
				Map<String,Object> props = super.cloneProperties();
				if (props == null) {
					props = new HashMap<String,Object>(4);
				}
				if (id != null) {
					props.put("id", id);
				}
				props.put("band", band);
				return props;
			}

		public String getType() {
			return CYTOBAND_TIER_NAME;
		}
	}

	public static int determineCentromerePoint(List<CytobandSym> bands) {
		for (int i = 0; i < bands.size() - 1; i++) {
			if (bands.get(i).getArm() != bands.get(i + 1).getArm()) {
				return i;
			}
		}
		return -1;
	}

	public static List<CytobandSym> generateBands(SeqSymmetry cyto_container) {
		List<CytobandSym> bands = new ArrayList<CytobandSym>();
		for (int q = 0; q < cyto_container.getChildCount(); q++) {
			SeqSymmetry child = cyto_container.getChild(q);
			if (child instanceof CytobandSym) {
				bands.add((CytobandSym) child);
			} else if (child != null) {
				for (int subindex = 0; subindex < child.getChildCount(); subindex++) {
					SeqSymmetry grandchild = child.getChild(subindex);
					if (grandchild instanceof CytobandSym) {
						bands.add((CytobandSym) grandchild);
					}
				}
			}
		}
		return bands;
	}
}
