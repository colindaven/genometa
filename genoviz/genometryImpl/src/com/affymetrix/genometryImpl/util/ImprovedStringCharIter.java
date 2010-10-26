package com.affymetrix.genometryImpl.util;

public final class ImprovedStringCharIter implements SearchableCharIterator {
	private final String src;

	public ImprovedStringCharIter(String src) {
		this.src = src;
	}

	public String substring(int start, int end)  {
		return src.substring(start, end);
	}

	public int indexOf(String searchString, int offset) {
		return src.indexOf(searchString, offset);
	}

	public int getLength() {
		return src.length();
	}
}

