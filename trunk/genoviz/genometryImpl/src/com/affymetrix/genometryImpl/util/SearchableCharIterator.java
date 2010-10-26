package com.affymetrix.genometryImpl.util;

/**
 *  to clean up code in SeqSearchView, OrfAnalyzer, etc., need an interface
 *  (so regex stuff, SeqCharGlyph, etc. can use it) that includes an
 *  indexOf() method (so SeqSearchView, etc. code can just
 *  wrap a String with a SearchableCharIterator and can cast objects
 *  to SearchableCharIterator rather than more specific class [such as GeneralBioSeq])
 */
public interface SearchableCharIterator {
	public String substring(int start, int end);
	public int indexOf(String searchstring, int offset);
	public int getLength();
}

