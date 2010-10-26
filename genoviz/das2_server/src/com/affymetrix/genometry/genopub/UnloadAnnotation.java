package com.affymetrix.genometry.genopub;

import java.io.Serializable;
import java.util.Date;

public class UnloadAnnotation implements Serializable {


    private Integer             idUnloadAnnotation;
	private String              typeName;
    private Integer             idUser;
    private Integer             idGenomeVersion;
    
	public Integer getIdUnloadAnnotation() {
    	return idUnloadAnnotation;
    }
	public void setIdUnloadAnnotation(Integer idUnloadAnnotation) {
    	this.idUnloadAnnotation = idUnloadAnnotation;
    }
	public String getTypeName() {
    	return typeName;
    }
	public void setTypeName(String typeName) {
    	this.typeName = typeName;
    }
	public Integer getIdUser() {
    	return idUser;
    }
	public void setIdUser(Integer idUser) {
    	this.idUser = idUser;
    }
	public Integer getIdGenomeVersion() {
    	return idGenomeVersion;
    }
	public void setIdGenomeVersion(Integer idGenomeVersion) {
    	this.idGenomeVersion = idGenomeVersion;
    }

}