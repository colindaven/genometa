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

package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.parsers.GFFParser;
import java.util.*;
import java.util.regex.*;

/**
 *  A sym to efficiently store GFF 1.0 annotations.
 *  See http://genome.ucsc.edu/goldenPath/help/customTrack.html#GTF
 */
public final class UcscGffSym extends SingletonSymWithProps implements Scored {

	public static final char UNKNOWN_FRAME = '.';

	/**
	 *  A pattern used to test whether this is GFF1 or GFF2.
	 *  If the pattern matches, then Matcher.group(1) will contain the GFF1 ID.
	 *  <p>
	 *  The pattern that matches a single string of non-whitespace characters,
	 *  either (1) all by themselves, or (2) follwed by any amount of whitespace
	 *  and a "#" and any other text.  Case (2) allows for comments.
	 *  </p>
	 *
	 *<pre>
	 *  Examples:
	 *    "AFX382 # here is a comment"  matches.
	 *    "AFX382" matches
	 *    "AFX382  " matches (note that the extra white space is ok)
	 *    "group_id "foo" ; transcript_id "bar""  does NOT match
	 *    Gotchas:
	 *      "AFX382# this is a comment" matches, and the ID does not include the "#" character
	 *      "AFX382#" matches, and the ID does include the "#" character
	 *</pre>
	 */
	public static final Pattern gff1_regex = Pattern.compile("^(\\S+)\\s*($|#.*)");

	// old, wrong pattern, required a tab before the comment
	//public static final Pattern gff1_regex = Pattern.compile("^(\\S+)($|\\t#)");

	String source;
	String method;
	String feature_type;
	float score;
	char frame;
	String group;
	boolean is_gff1;

	/**
	 * Constructor.
	 * The coordinates should be given exactly as they appear in a GFF file.
	 * In principle, the first coordinate is supposed to be less than the second one,
	 * but in practice this isn't always followed, so this constructor will correct
	 * those errors and will also convert from base-1 to interbase-0 coordinates.
	 * @param a  The coordinate in column 4 of the GFF file.
	 * @param b  The coordinate in column 5 of the GFF file.
	 * @param convert_base Whether to convert from base-1 to interbase-0
	 *   numbering; this IS necessary with typical GFF files.
	 */
	public UcscGffSym(BioSeq seq, String source, String feature_type, int a, int b,
			float score, char strand, char frame, String group_field,
			boolean convert_base) {
		super(0, 0, seq);

		// GFF spec says coord_A <= coord_B, but this is not always obeyed
		int max = Math.max(a, b);
		int min = Math.min(a, b);
		if (convert_base) { // convert from base-1 numbering to interbase-0 numbering
			min--;
		}

		if (strand == '-') {
			setCoords(max, min);
		} else {
			setCoords(min, max);
		}

		this.source = source;
		this.feature_type = feature_type;
		this.score = score;
		this.frame = frame;
		if (group_field==null || group_field.startsWith("#")) {
			this.group = null;
			this.is_gff1 = true;
		} else {
			Matcher gff1_matcher = gff1_regex.matcher(group_field);
			if (gff1_matcher.matches()) {
				this.group = new String(gff1_matcher.group(1)); // creating a new String can save Memory
				this.is_gff1 = true;
			} else {
				this.group = group_field;
				this.is_gff1 = false;
			}
		}
	}

	public String getSource()  { return source; }
	public String getFeatureType()  { return feature_type; }
	public float getScore()  { return score; }
	public char getFrame()  { return frame; }

	/** Returns null for GFF2 or the group field for GFF1. */
	public String getGroup()  {
		if (is_gff1) return group;
		else return null;
	}

	public boolean isGFF1() {
		return is_gff1;
	}

	public Object getProperty(String name) {
		if (name.equals("method")) { return method; }
		else if (name.equals("source")) { return source; }
		else if (name.equals("feature_type") || name.equals("type")) { return feature_type; }
		else if (name.equals("score") && score != UNKNOWN_SCORE) { return new Float(score); }
		else if (name.equals("frame") && frame != UNKNOWN_FRAME) { return Character.valueOf(frame); }
		else if (name.equals("group")) { return getGroup(); }
		else if (name.equals("id")) { return getID(); }
		else if (is_gff1) {
			return super.getProperty(name);
		} else {
			// for GFF2, parse the attributes field and return the property found in that
			Map<String,Object> m = cloneProperties();
			return m.get(name);
		}
	}

	/**
	 *  Overriden such that certain properties will be stored more efficiently.
	 *  Setting certain properties this way is not supported:
	 *  these include "group", "score" and "frame".
	 */
	public boolean setProperty(String name, Object val) {
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
		if (name.equals("feature_type") || name.equals("type")) {
			if (val instanceof String) {
				feature_type = (String) val;
				return true;
			}
			else {
				//feature_type = null;
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
				//source = null;
				return false;
			}
		}
		else if (name.equals("group")) {
			// Not supported
			throw new IllegalArgumentException("Currently can't modify 'group' via setProperty");
		}
		else if (name.equals("score") || name.equals("frame")) {
			// May need to handle these later, but it is unlikely to be an issue
			throw new IllegalArgumentException("Currently can't modify 'score' or 'frame' via setProperty");
		}

		return super.setProperty(name, val);
	}

	public Map<String,Object> getProperties() {
		return cloneProperties();
	}

	public Map<String,Object> cloneProperties() {
		Map<String,Object> tprops = super.cloneProperties();
		if (tprops == null) {
			tprops = new HashMap<String,Object>();
		}
		if (getID() != null) {
			tprops.put("id", getID());
		}
		if (source != null) { tprops.put("source", source); }
		if (method != null) { tprops.put("method", method); }
		if (feature_type != null) { tprops.put("feature_type", feature_type); }
		if (feature_type != null) { tprops.put("type", feature_type); }
		if (score != UNKNOWN_SCORE) {
			tprops.put("score", new Float(getScore()));
		}
		if (frame != UNKNOWN_FRAME) {
			tprops.put("frame", Character.valueOf(frame));
		}
		if (is_gff1) {
			if (group != null) {tprops.put("group", group);}
		} else {
			if (group != null) GFFParser.processAttributes(tprops, group);
		}

		return tprops;
	}

}
