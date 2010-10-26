package com.affymetrix.genometryImpl.das2;

import java.util.List;
import java.util.Map;

public final class SimpleDas2Type {
	
	private String             name;
	private List<String>       formats;
	private Map<String,Object> props;

	public SimpleDas2Type(String name, List<String> formats,
            Map<String, Object> props) {

	    this.name = name;
	    this.formats = formats;
	    this.props = props;
    }
	
	public String getName() {
    	return name;
    }
	public void setName(String name) {
    	this.name = name;
    }
	public List<String> getFormats() {
    	return formats;
    }
	public void setFormats(List<String> formats) {
    	this.formats = formats;
    }
	public Map<String, Object> getProps() {
    	return props;
    }
	public void setProps(Map<String, Object> props) {
    	this.props = props;
    }
	
	

}
