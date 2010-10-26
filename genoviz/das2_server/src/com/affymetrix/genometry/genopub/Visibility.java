package com.affymetrix.genometry.genopub;

public class Visibility {

	public static final String		MEMBERS = "MEM";
	public static final String		MEMBERS_AND_COLLABORATORS = "MEMCOL";
	public static final String		PUBLIC = "PUBLIC";
	
	private String codeVisibility;
	private String name;
	
	public String getCodeVisibility() {
    	return codeVisibility;
    }
	public void setCodeVisibility(String codeVisibility) {
    	this.codeVisibility = codeVisibility;
    }
	public String getName() {
    	return name;
    }
	public void setName(String name) {
    	this.name = name;
    }
	
	public static String getDisplay(String codeVisibility) {
		if (codeVisibility.equals(MEMBERS)) {
			return "Members";
		} else if (codeVisibility.equals(MEMBERS_AND_COLLABORATORS)){
			return "Members and Collaborators";
		} else if (codeVisibility.equals(PUBLIC)) {
			return "Public";
		} else {
			return "";
		}
	}
	
	

}
