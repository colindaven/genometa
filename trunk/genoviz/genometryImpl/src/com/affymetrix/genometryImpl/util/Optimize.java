package com.affymetrix.genometryImpl.util;

import java.util.ArrayList;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.IntervalSearchSym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.TypeContainerAnnot;

public abstract class Optimize {
	private static final boolean DEBUG = false;
	
	public static final void genome(AnnotatedSeqGroup genome) {
		/** third, replace top-level annotation SeqSymmetries with IntervalSearchSyms */
		for (BioSeq aseq : genome.getSeqList()) {
			Optimize.Seq(aseq);
		}
	}


	/*
	 *  After optimization, should end up with
	 *     a TypeContainerAnnot attached directly to the seq for each method/type of annotation
	 *     and a (probably single) child for each container which is an IntervalSearchSym that
	 *     holds annotations of the given type, and which has been optimized for range-based
	 *     queries to return its children
	 *    Another way to think of this is that there are two levels of annotation hierarchy
	 *       above what one would otherwise consider the "top level" annotations.
	 *       For example for transcript predictions there would be a four-level hiearchy:
	 *            single TypeContainerAnnot object A attached as annotation to seq
	 *            single IntervalSearchSym object B, child of A
	 *            multiple transcripts, children of B
	 *            multiple exons, children of transcripts
	 *
	 *
	 *  To Do:
	 *
	 *     check for multiple top-level annotations of the same type, and combine
	 *       if found so there is only one top-level annotation per type for each seq
	 *
	 */
	private static final void Seq(BioSeq aseq) {
		if (DEBUG) {
			System.out.println("optimizing seq = " + aseq.getID());
		}
		int annot_count = aseq.getAnnotationCount();
		if (DEBUG) {
			System.out.println("annotation count: " + annot_count);
		}
		for (int i = annot_count - 1; i >= 0; i--) {
			// annot should be a TypeContainerAnnot (if seq is a BioSeq)
			SeqSymmetry annot = aseq.getAnnotation(i);
			if (annot instanceof TypeContainerAnnot) {
				TypeContainerAnnot container = (TypeContainerAnnot) annot;
				Optimize.typeContainer(container, aseq);
			} else {
				System.out.println("problem in optimizeSeq(), found top-level sym that is not a TypeContainerAnnot: " +
						annot);
			}
		}
	}

	private static final void typeContainer(TypeContainerAnnot container, BioSeq aseq) {
		if (DEBUG) {
			System.out.println("optimizing type container: " + container.getProperty("method") +
					", depth = " + SeqUtils.getDepth(container));
		}
		String annot_type = container.getType();
		int child_count = container.getChildCount();
		ArrayList<SeqSymmetry> temp_annots = new ArrayList<SeqSymmetry>(child_count);

		// more efficient to remove from end of annotations...
		for (int i = child_count - 1; i >= 0; i--) {
			SeqSymmetry child = container.getChild(i);
			// if child is not IntervalSearchSym, copy to temp list in preparation for
			//    converting children to IntervalSearchSyms
			if (child instanceof IntervalSearchSym) {
				IntervalSearchSym search_sym = (IntervalSearchSym) child;
				if (!search_sym.getOptimizedForSearch()) {
					search_sym.initForSearching(aseq);
				}
			} else {
				temp_annots.add(child);
				// really want to do container.removeChild(i) here, but
				//   currently there is no removeChild(int) method for MutableSeqSymmetry and descendants
				container.removeChild(child);
			}
		}

		int temp_count = temp_annots.size();
		//    System.out.println("optimizing for: " + container.getType() + ", seq: " + aseq.getID() + ", count: " + temp_count);
		// iterate through all annotations from TypeContainerAnnot on this sequence that are not IntervalSearchSyms,
		//    convert them to IntervalSearchSyms.
		for (int i = temp_count - 1; i >= 0; i--) {
			SeqSymmetry annot_sym = temp_annots.get(i);
			IntervalSearchSym search_sym = new IntervalSearchSym(aseq, annot_sym);
			search_sym.setProperty("method", annot_type);
			search_sym.initForSearching(aseq);
			container.addChild(search_sym);
		}
		//    if (MAKE_LANDSCAPES) { makeLandscapes(aseq); }
		if (DEBUG) {
			System.out.println("finished optimizing container: " + container.getProperty("method") +
					", depth = " + SeqUtils.getDepth(container));
		}
	}

}
