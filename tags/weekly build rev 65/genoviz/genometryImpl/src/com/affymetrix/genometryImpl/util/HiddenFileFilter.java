package com.affymetrix.genometryImpl.util;

import java.io.File;
import java.io.FileFilter;

/**
 *
 * A FileFilter that excludes hidden files from directory listings.
 * 
 * @see java.io.FileFilter
 * 
 * @author sgblanch
 * @version $Id: HiddenFileFilter.java 4058 2009-07-29 14:28:31Z sgblanch $
 */
public final class HiddenFileFilter implements FileFilter {
	private final FileFilter filter;

	public HiddenFileFilter() { filter = null; }

	/**
	 * Constructor that allows chaining of file filters
	 *
	 * @param filter the FileFilter to add to filter chain
	 */
	public HiddenFileFilter(FileFilter filter) { this.filter = filter; }

	public boolean accept(File pathname) {
		return !pathname.isHidden() && (filter == null || filter.accept(pathname));
	}
}