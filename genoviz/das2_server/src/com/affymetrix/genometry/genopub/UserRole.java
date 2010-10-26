package com.affymetrix.genometry.genopub;

public class UserRole {
	
	private Integer idUserRole;
	private Integer idUser;
	private String  userName;
	private String  roleName;
	

	public Integer getIdUserRole() {
    	return idUserRole;
    }
	public void setIdUserRole(Integer idUserRole) {
    	this.idUserRole = idUserRole;
    }
	public Integer getIdUser() {
    	return idUser;
    }
	public void setIdUser(Integer idUser) {
    	this.idUser = idUser;
    }
	public String getRoleName() {
    	return roleName;
    }
	public void setRoleName(String roleName) {
    	this.roleName = roleName;
    }
	public String getUserName() {
    	return userName;
    }
	public void setUserName(String userName) {
    	this.userName = userName;
    }
	

}
