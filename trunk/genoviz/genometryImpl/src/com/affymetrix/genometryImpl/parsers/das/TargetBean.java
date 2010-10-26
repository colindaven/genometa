package com.affymetrix.genometryImpl.parsers.das;

/**
 *
 * @author sgblanch
 * @version $Id: TargetBean.java 5343 2010-02-25 01:39:42Z sgblanch $
 */
class TargetBean {
	private String id, name;
	private int start, stop;

	TargetBean() { this.clear(); }

	void setID(String id) { this.id = id.intern(); }

	String getID() { return this.id; }

	void setName(String name) { this.name = name.intern(); }

	String getName() { return this.name; }

	void setStart(String start) { this.start = Integer.parseInt(start) - 1; }

	int getStart() { return this.start; }

	void setStop(String stop) { this.stop = Integer.parseInt(stop); }

	int getStop() { return this.stop; }

	void clear() {
		id = "";
		name = "";
		start = 0;
		stop = 0;
	}
}
