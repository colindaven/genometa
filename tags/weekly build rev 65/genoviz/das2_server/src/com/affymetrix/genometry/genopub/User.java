package com.affymetrix.genometry.genopub;

import java.io.Serializable;
import java.util.Set;

public class User implements Serializable {
	
	public static final String    MASKED_PASSWORD = "XXXX";
	
	private Integer idUser;
	private String  firstName;
	private String  lastName;
	private String  middleName;
	private String  userName;
	private String  password;
	private String  email;
	private String  institute;
	private Set     memberUserGroups;
	private Set     collaboratingUserGroups;
	private Set     managingUserGroups;
	private Set     roles;
	
	public Integer getIdUser() {
    	return idUser;
    }
	public void setIdUser(Integer idUser) {
    	this.idUser = idUser;
    }
	public String getFirstName() {
    	return firstName;
    }
	public void setFirstName(String firstName) {
    	this.firstName = firstName;
    }
	public String getLastName() {
    	return lastName;
    }
	public void setLastName(String lastName) {
    	this.lastName = lastName;
    }
	public String getMiddleName() {
    	return middleName;
    }
	public void setMiddleName(String middleName) {
    	this.middleName = middleName;
    }
	public String getName() {
		StringBuffer name = new StringBuffer();
		if (this.getLastName() != null && !this.getLastName().equals("")) {
			name.append(this.getLastName());
		}
		if (this.getFirstName() != null && !this.getFirstName().equals("")) {
			if (name.length() > 0) {
				name.append(", ");
			}
			name.append(this.getFirstName());
		}
		if (this.getMiddleName() != null && !this.getMiddleName().equals("")) {
			if (name.length() > 0) {
				name.append(" ");
			}
			name.append(this.getMiddleName());
		}
		return name.toString();
	}
	public String getUserDisplayName() {
		StringBuffer name = new StringBuffer();
		if (this.getFirstName() != null && !this.getFirstName().equals("")) {
			name.append(this.getFirstName());
		}
		if (this.getMiddleName() != null && !this.getMiddleName().equals("")) {
			if (name.length() > 0) {
				name.append(" ");
			}
			name.append(this.getMiddleName());
		}
		if (this.getLastName() != null && !this.getLastName().equals("")) {
			if (name.length() > 0) {
				name.append(" ");
			}
			name.append(this.getLastName());
		}
		return name.toString();
	}
	public String getUserName() {
    	return userName;
    }
	public void setUserName(String userName) {
    	this.userName = userName;
    }
	public String getPassword() {
    	return password;
    }
	public void setPassword(String password) {
    	this.password = password;
    }
	public String getPasswordDisplay() {
		if (this.password != null && !this.password.equals("")) {
			return MASKED_PASSWORD;      
		} else {
			return "";
		}
	}
	public Set getMemberUserGroups() {
    	return memberUserGroups;
    }
	public void setMemberUserGroups(Set memberUserGroups) {
    	this.memberUserGroups = memberUserGroups;
    }
	public Set getCollaboratingUserGroups() {
    	return collaboratingUserGroups;
    }
	public void setCollaboratingUserGroups(Set collaboratingUserGroups) {
    	this.collaboratingUserGroups = collaboratingUserGroups;
    }
	public Set getManagingUserGroups() {
    	return managingUserGroups;
    }
	public void setManagingUserGroups(Set managingUserGroups) {
    	this.managingUserGroups = managingUserGroups;
    }
	public Set getRoles() {
    	return roles;
    }
	public void setRoles(Set roles) {
    	this.roles = roles;
    }
	public String getEmail() {
    	return email;
    }
	public void setEmail(String email) {
    	this.email = email;
    }
	public String getInstitute() {
    	return institute;
    }
	public void setInstitute(String institute) {
    	this.institute = institute;
    }
	

}
