package com.affymetrix.genometry.genopub;


public class Segment {
    
    private Integer idSegment;
    private Integer length;
    private String  name;
    private Integer idGenomeVersion;
    private Integer sortOrder;
    
    public Integer getIdSegment() {
        return idSegment;
    }
    public void setIdSegment(Integer idSegment) {
        this.idSegment = idSegment;
    }
    public Integer getLength() {
        return length;
    }
    public void setLength(Integer length) {
        this.length = length;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
	public Integer getIdGenomeVersion() {
    	return idGenomeVersion;
    }
	public void setIdGenomeVersion(Integer idGenomeVersion) {
    	this.idGenomeVersion = idGenomeVersion;
    }

}
