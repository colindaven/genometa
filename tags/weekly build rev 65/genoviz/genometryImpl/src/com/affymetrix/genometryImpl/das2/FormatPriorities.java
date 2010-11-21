package com.affymetrix.genometryImpl.das2;

import java.util.Map;

import com.affymetrix.genometryImpl.parsers.useq.USeqUtilities;


/**
 *  FormatPriorities is intended to help the IGB DAS/2 client figure out
 *  what format to retrieve annotations in, given the formats supported by the server for a particular
 *  versioned source (returned via the /types query).
 *
 *  May want to add some smarts in here on a per-type basis, so for example if a server can serve
 *   up either formatA or formatB for both type1 and type2, the client may stil prefer formatB for type1
 *   and formatA for type2.  For first cut not worrying about this, just giving the client a fixed
 *   format priorities that apply to all types
 *
 */
final public class FormatPriorities {

	/**
	 *  Different format types, prioritized
	 */
	private static final String[] ordered_formats = {
		"link.psl",
		"ead",
		"bp2",
		"brs",
		"bgn",
		"bps",
		"cyt",
		"useq",
		"bed",
		"psl",
		"gff",
		"bar",
		"bam"};

	public static String getFormat(Das2Type type) {
		if (type.getURI().toString().endsWith(".bar")) {  // temporary way to recognize graph "types"...
			return "bar";
		}
		if (type.getURI().toString().endsWith(USeqUtilities.USEQ_EXTENSION_NO_PERIOD)) {
			return USeqUtilities.USEQ_EXTENSION_NO_PERIOD;
		}
		if (type.getURI().toString().endsWith(".bed")) {
			return "bed";
		}
		Map<String,String> type_formats = type.getFormats();
		if (type_formats != null) {
			for (String format : ordered_formats) {
				if (type_formats.get(format) != null) {
					return format;
				}
			}
		}

		// return default_format;
		return null;
	}

}
