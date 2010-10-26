package com.affymetrix.genometry.genopub;

import java.io.Serializable;


public class QualifiedAnnotation implements Serializable {
	private Annotation annotation;
	private String     typePrefix;
	private String     resourceName;
	
	public QualifiedAnnotation(Annotation annotation, String typePrefix, String resourceName) {
	    super();
	    this.annotation = annotation;
	    this.typePrefix = typePrefix;
	    this.resourceName = resourceName;
    }
	public Annotation getAnnotation() {
    	return annotation;
    }
	public void setAnnotation(Annotation annotation) {
    	this.annotation = annotation;
    }
	public String getTypePrefix() {
    	return typePrefix;
    }
	public void setTypePrefix(String typePrefix) {
    	this.typePrefix = typePrefix;
    }
	public String getResourceName() {
    	return resourceName;
    }
	public void setResourceName(String resourceName) {
    	this.resourceName = resourceName;
    }
}
