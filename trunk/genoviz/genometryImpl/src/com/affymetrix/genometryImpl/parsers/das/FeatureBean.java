package com.affymetrix.genometryImpl.parsers.das;

import com.affymetrix.genometryImpl.Scored;
import com.affymetrix.genometryImpl.parsers.das.DASFeatureParser.Orientation;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author sgblanch
 * @version $Id: FeatureBean.java 5343 2010-02-25 01:39:42Z sgblanch $
 */
class FeatureBean {

	private String id;
	private String label;
	private String typeID;
	private String typeCategory;
	private String typeLabel;
	private boolean typeReference;
	private String methodID;
	private String methodLabel;
	private int start;
	private int end;
	private float score;
	private Orientation orientation;
	private char phase;
	private final List<String> notes = new ArrayList<String>(2);
	private final List<LinkBean> links = new ArrayList<LinkBean>(2);
	private final List<TargetBean> targets = new ArrayList<TargetBean>(2);
	private final List<GroupBean> groups = new ArrayList<GroupBean>(2);

	FeatureBean() { this.clear(); }

	void setID(String id) { this.id = id.intern(); }

	String getID() { return this.id; }

	void setLabel(String label) { this.label = label.intern(); }

	String getLabel() { return this.label; }

	void setTypeID(String typeID) { this.typeID = typeID.intern(); }

	String getTypeID() { return this.typeID; }

	void setTypeCategory(String typeCategory) { this.typeCategory = typeCategory.intern(); }

	String getTypeCategory() { return this.typeCategory; }

	void setTypeLabel(String typeLabel) { this.typeLabel = typeLabel.intern(); }

	String getTypeLabel() { return this.typeLabel; }

	void setTypeReference(String typeReference) {
		this.typeReference = typeReference.equals("yes") ? true : false;
	}

	boolean isTypeReference() { return this.typeReference; }

	void setMethodID(String methodID) { this.methodID = methodID.intern(); }

	String getMethodID() { return this.methodID; }

	void setMethodLabel(String methodLabel) { this.methodLabel = methodLabel.intern(); }

	String getMethodLabel() { return this.methodLabel; }

	void setStart(String start) {
		this.start = Integer.parseInt(start) - 1;
	}

	int getStart() { return this.start; }

	public void setEnd(String end) {
		this.end = Integer.parseInt(end);
	}

	int getEnd() { return this.end; }

	void setScore(String score) {
		this.score = score.equals("-") ? Scored.UNKNOWN_SCORE : Float.parseFloat(score);
	}

	float getScore() { return this.score; }

	void setOrientation(String orientation) {
		if (orientation.equals("+")) {
			this.orientation = Orientation.FORWARD;
		} else if (orientation.equals("-")) {
			this.orientation = Orientation.REVERSE;
		} else {
			this.orientation = Orientation.UNKNOWN;
		}
	}

	Orientation getOrientation() { return this.orientation; }

	void setPhase(String phase) {
		this.phase = phase.isEmpty() ? '-' : phase.charAt(0);
	}

	char getPhase() { return this.phase; }

	void addNote(String note) { this.notes.add(note.intern()); }

	List<String> getNotes() {
		return Collections.<String>unmodifiableList(this.notes);
	}

	void addLink(LinkBean link) { this.links.add(link); }

	List<LinkBean> getLinks() {
		return Collections.<LinkBean>unmodifiableList(this.links);
	}
	
	void addTarget(TargetBean target) { this.targets.add(target); }

	List<TargetBean> getTargets() {
		return Collections.<TargetBean>unmodifiableList(this.targets);
	}

	void addGroup(GroupBean group) { this.groups.add(group); }

	List<GroupBean> getGroups() {
		return Collections.<GroupBean>unmodifiableList(this.groups);
	}

	void clear() {
		id = "";
		label = "";
		typeID = "";
		typeCategory = "";
		typeLabel = "";
		typeReference = false;
		methodID = "";
		methodLabel = "";
		start = 0;
		end = 0;
		score = Scored.UNKNOWN_SCORE;
		orientation = Orientation.UNKNOWN;
		phase = '-';
		notes.clear();
		links.clear();
		targets.clear();
		groups.clear();
	}
}
