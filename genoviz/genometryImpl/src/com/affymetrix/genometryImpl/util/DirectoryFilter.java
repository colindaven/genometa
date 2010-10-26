package com.affymetrix.genometryImpl.util;

import java.io.File;
import java.io.FileFilter;

/**
 *
 * A FileFilter that only includes sub directories in directory listings.
 *
 * @see java.io.FileFilter
 * @author sgblanch
 * @version $Id: DirectoryFilter.java 4058 2009-07-29 14:28:31Z sgblanch $
 */
public final class DirectoryFilter implements FileFilter {
	private final FileFilter filter;

	public DirectoryFilter() { filter = null; }

	/**
	 * Constructor that allows chaining of file filters
	 *
	 * @param filter the FileFilter to add to filter chain
	 */
	public DirectoryFilter(FileFilter filter) { this.filter = filter; }

	public boolean accept(File pathname) {
		return pathname.isDirectory() && (filter == null || filter.accept(pathname));
	}
}
