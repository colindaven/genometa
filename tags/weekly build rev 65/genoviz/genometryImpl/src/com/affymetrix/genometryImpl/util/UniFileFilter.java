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

package com.affymetrix.genometryImpl.util;

import javax.swing.filechooser.FileFilter;
import java.util.*;
import java.io.*;

/**
 *  A file filter which makes it easy to set-up a JFileChooser that
 *  can accept files only with particular filename endings, and can also
 *  understand compression type endings.
 *  UniFileFilter uff = new UniFileFilter(new String[] {"gff", "gtf"}, "GFF Files"));
 *  uff.addCompressionEndings(new String[] {".gz", ".zip"});
 */
public final class UniFileFilter extends FileFilter {


	private HashSet<String> filters = null;
	private String description = null;
	private String fullDescription = null;
	private boolean useExtensionsInDescription = true;

	public UniFileFilter() {
		this.filters = new LinkedHashSet<String>();
	}

	public UniFileFilter(String extension) {
		this(extension,null);
	}

	public UniFileFilter(String extension, String description) {
		this();
		if (extension!=null) {
			addExtension(extension);
		}
		if (description!=null) {
			setDescription(description);
		}
	}

	/**
	 *  This is the full constructor.
	 *  <pre>
	 *  Example 1:
	 *  UniFileFilter uff = new UniFileFilter(new String[] {"gff", "gtf"}, "GFF Files"));
	 *  uff.addCompressionEndings(new String[] {".gz", ".zip"});
	 *
	 *  Example 2:
	 *  UniFileFilter uff = new UniFileFilter(new String[] {"sin", "egr", "egr.txt"}, "Scored Interval Files"));
	 *  uff.addCompressionEndings(new String[] {".gz", ".zip"});
	 *  </pre>
	 */
	public UniFileFilter(String[] filters, String description) {
		this();
		for (int i = 0; i < filters.length; i++) {
			// add filters one by one
			addExtension(filters[i]);
		}
		if (description!=null) {
			setDescription(description);
		}
	}

	public boolean accept(File f) {
		if(f != null) {
			if(f.isDirectory()) {
				return true;
			}

			// We used to use getExtension(f) and check whether filters.contains(extension)
			// but getExtension(f) can't return compound extensions like ".egr.txt"

			String base_name = stripCompressionEndings(f.getName().toLowerCase());
			Iterator iter = filters.iterator();
			while (iter.hasNext()) {
				String ending = "." + (String) iter.next();
				if (base_name.endsWith(ending)) {
					return true;
				}
			}
		}
		return false;
	}

	List<String> compression_endings = new ArrayList<String>(4);

	/** Adds a file extension that will be considered to represent
	 *  compression types that the filter should
	 *  accept.  Endings, such as ".z" or ".gz",  are considered case-insensitive.
	 */
	public void addCompressionEnding(String ending) {
		compression_endings.add(ending.toLowerCase());
	}

	/** Calls {@link #addCompressionEnding(String)} for each item in the list. */
	public void addCompressionEndings(String[] endings) {
		for (int i=0; i<endings.length; i++) {
			addCompressionEnding(endings[i]);
		}
	}

	// Removes all compression file-type endings
	// Supply argument in lower case
	private String stripCompressionEndings(String name) {
		if (compression_endings != null) {
			for (String ending : compression_endings) {
				name = stripCompressionEnding(name, ending);
			}
		}
		return name;
	}

	// Removes a compression file-type endings. If the ending isn't present,
	// the given filename is returned intact
	// Should supply both arguments in lower case
	private static String stripCompressionEnding(String name, String ending) {
		if (name.endsWith(ending)) {
			int index = name.lastIndexOf(ending);
			return name.substring(0,index);
		} else {
			return name;
		}
	}

	/** Returns the file extension remaining after all compression endings have
	 *  been removed from the name.  The returned file extension is lowercase.
	 *  In the following examples,
	 *  assume that setCompressionEnding(new String[] {".gz"}) has
	 *  been called:
	 *<ol>
	 *<li> "foo.bar" returns "bar".
	 *<li> "foo.BAR.gz" returns "bar".
	 *<li> "foo.egr.txt" returns "txt". (NOT "egr.txt" as you might wish)
	 *<li> "foo" returns null.
	 *<li> "foo.gz" returns null.
	 *</ol>
	 */
	/*public String getExtension(File f) {
	  if(f != null) {
	  String filename = f.getName().toLowerCase();
	  filename = stripCompressionEndings(filename);
	  int i = filename.lastIndexOf('.');
	  if(i>0 && i<filename.length()-1) {
	  return filename.substring(i+1).toLowerCase();
	  }
	  }
	  return null;
	  }*/


	public void addExtension(String extension) {
		if(filters == null) {
			filters = new LinkedHashSet<String>(5);
		}
		filters.add(extension.toLowerCase());
		fullDescription = null;
	}

	/** Returns an unmodifiable Set of the extensions added with
	 * {@link #addExtension(String)}.
	 */
	public Set<String> getExtensions() {
		return Collections.<String>unmodifiableSet(filters);
	}

	public String getDescription() {
		if(fullDescription == null) {
			if(description == null || isExtensionListInDescription()) {
				fullDescription = description==null ? "(" : description + " (";
				// build the description from the extension list
				Iterator extensions = filters.iterator();
				if(extensions.hasNext()) {
					fullDescription += "*." + (String) extensions.next();
					while (extensions.hasNext()) {
						fullDescription += ", *." + (String) extensions.next();
					}
				}
				fullDescription += ")";
			} else {
				fullDescription = description;
			}
		}
		return fullDescription;
	}

	/** Sets a short description for the file filter.  Example "Text files".
	 *  The list of file endings may be added automatically.
	 *  @see #setExtensionListInDescription(boolean)
	 */
	public void setDescription(String description) {
		this.description = description;
		fullDescription = null;
	}

	/** Set whether the description of the file type should have the list of
	 *  file type extensions added to it automatically.  Default is true.
	 *  For example  "Text files (*.txt, *.rtf)".
	 */
	public void setExtensionListInDescription(boolean b) {
		useExtensionsInDescription = b;
		fullDescription = null;
	}

	/** Returns the value set in {@link #setExtensionListInDescription(boolean)}.
	*/
	public boolean isExtensionListInDescription() {
		return useExtensionsInDescription;
	}

}
