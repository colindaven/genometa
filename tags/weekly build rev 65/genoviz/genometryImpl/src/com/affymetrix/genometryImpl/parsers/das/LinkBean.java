package com.affymetrix.genometryImpl.parsers.das;

/**
 *
 * @author sgblanch
 * @version $Id: LinkBean.java 5343 2010-02-25 01:39:42Z sgblanch $
 */
class LinkBean {
	private String url;
	private String title;

	LinkBean() { this.clear(); }

	void setURL(String url) { this.url = url.intern(); }

	String getURL() { return this.url; }

	void setTitle(String title) { this.title = title.intern(); }

	public String getTitle() { return this.title; }

	void clear() {
		this.url = "";
		this.title = "";
	}
}
