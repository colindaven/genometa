package com.affymetrix.genometry.genopub;


public class ExperimentPlatform implements Owned {
    
    private Integer idExperimentPlatform;
    private String  name;
    private String  isActive;
    private Integer idUser;
    
 
    public Integer getIdExperimentPlatform() {
        return idExperimentPlatform;
    }
    public void setIdExperimentPlatform(Integer idExperimentPlatform) {
        this.idExperimentPlatform = idExperimentPlatform;
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
