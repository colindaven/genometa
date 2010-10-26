package com.affymetrix.genometry.genopub;

import java.util.Set;

import com.affymetrix.genometry.genopub.Owned;

public class AnalysisType implements Owned {
    
    private Integer idAnalysisType;
    private String  name;
    private String  isActive;
    private Integer idUser;
    
    public Integer getIdAnalysisType() {
        return idAnalysisType;
    }
    public void setIdAnalysisType(Integer idAnalysisType) {
        this.idAnalysisType = idAnalysisType;
    }    
    public String getIsActive() {
        return isActive;
    }
    public void setIsActive(String isActive) {
        this.isActive = isActive;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
	public Integer getIdUser() {
    	return idUser;
    }
	public void setIdUser(Integer idUser) {
    	this.idUser = idUser;
    }
	public boolean isOwner(Integer idUser) {
		if (this.getIdUser() != null && this.getIdUser().equals(idUser)) {
			return true;
		} else {
			return false;
		}
	}
	public boolean isUserGroup(Integer idUserGroup) {
		return false;
	}

}
