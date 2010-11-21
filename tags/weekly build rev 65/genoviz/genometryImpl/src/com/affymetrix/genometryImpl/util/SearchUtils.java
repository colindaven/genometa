package com.affymetrix.genometryImpl.util;

import java.util.*;
import java.util.regex.*;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SymWithProps;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jnicol
 */
public final class SearchUtils {
	/**
	 * Due to disagreements between group ID search and BioSeq ID search, do both and combine their results
	 * @param group
	 * @param chrFilter
	 * @param regex
	 * @return
	 */
	public static List<SeqSymmetry> findLocalSyms(AnnotatedSeqGroup group, BioSeq chrFilter, Pattern regex, boolean search_props) {

		Set<SeqSymmetry> syms = null;
		if(search_props)
			syms = new HashSet<SeqSymmetry>(group.findInSymProp(regex));
		else
			syms = new HashSet<SeqSymmetry>(group.findSyms(regex));

		List<BioSeq> chrs;
		if (chrFilter != null) {
			chrs = new ArrayList<BioSeq>();
			chrs.add(chrFilter);
		} else {
			chrs = group.getSeqList();
		}
		Matcher match = regex.matcher("");
		SymWithProps sym = null;
		for (BioSeq chr : chrs) {
			int annotCount = chr.getAnnotationCount();
			for (int i=0;i<annotCount;i++) {
				sym = (SymWithProps)chr.getAnnotation(i);
				findIDsInSym(syms, sym, match);
			}
		}
		return new ArrayList<SeqSymmetry>(syms);
	}

	/**
	 * Recursively search for symmetries that match regex.
	 * @param syms
	 * @param sym
	 * @param match
	 */
	private static void findIDsInSym(Set<SeqSymmetry> syms, SeqSymmetry sym, Matcher match) {
		if (sym == null) {
			return;
		}
		if (sym.getID() != null && match.reset(sym.getID()).matches()) {
			syms.add(sym);	// ID matches
			// If parent matches, then don't list children
			return;
		} else if (sym instanceof SymWithProps) {
			String method = BioSeq.determineMethod(sym);
			if (method != null && match.reset(method).matches()) {
				syms.add(sym);	// method matches
				// If parent matches, then don't list children
				return;
			}
		}
		int childCount = sym.getChildCount();
		for (int i = 0; i < childCount; i++) {
			findIDsInSym(syms, sym.getChild(i), match);
		}
	}



	public static Set<SeqSymmetry> findNameInGenome(String name, AnnotatedSeqGroup genome) {
		//int resultLimit = 1000000;

		boolean glob_start = name.startsWith("*");
		boolean glob_end = name.endsWith("*");

		Set<SeqSymmetry> result = null;
		Pattern name_pattern = null;
		String name_regex = name;
		if (glob_start || glob_end) {
			//name_regex = name.toLowerCase();
			if (glob_start) {
				// do replacement of first "*" with ".*" ?
				name_regex = ".*" + name_regex.substring(1);
			}
			if (glob_end) {
				// do replacement of last "*" with ".*" ?
				name_regex = name_regex.substring(0, name_regex.length() - 1) + ".*";
			}

		} else {
			// ABC -- field exactly matches "ABC"
			name_regex = "^" + name.toLowerCase() + "$";
			//result = genome.findSyms(name);
		}
		Logger.getLogger(SearchUtils.class.getName()).log(Level.INFO,
				"name arg: {0},  regex to use for pattern-matching: {1}", new Object[]{name, name_regex});

		name_pattern = Pattern.compile(name_regex, Pattern.CASE_INSENSITIVE);
		result = genome.findSyms(name_pattern);

		Logger.getLogger(SearchUtils.class.getName()).log(Level.INFO,
				"non-indexed regex matches: {0}", result.size());

		Set<SeqSymmetry> result2 = IndexingUtils.findSymsByName(genome, name_pattern);
		Logger.getLogger(SearchUtils.class.getName()).log(Level.INFO,
				"indexed regex matches: {0}", result2.size());

		result.addAll(result2);

		return result;
	}

}
