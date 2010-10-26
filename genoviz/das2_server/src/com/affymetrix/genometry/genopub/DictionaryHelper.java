package com.affymetrix.genometry.genopub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;


import org.hibernate.Session;


public class DictionaryHelper {
    
    private static DictionaryHelper                     theDictionaryHelper   = new DictionaryHelper();
    
    private boolean                                     isLoaded = false;
	
    private final HashMap<Integer, AnalysisType>        analysisTypeMap  = new HashMap<Integer, AnalysisType>();
	private final List<AnalysisType>                    analysisTypeList = new ArrayList<AnalysisType>();
	
    private final HashMap<Integer, ExperimentMethod>    experimentMethodMap  = new HashMap<Integer, ExperimentMethod>();
	private final List<ExperimentMethod>                experimentMethodList = new ArrayList<ExperimentMethod>();

    private final HashMap<Integer, ExperimentPlatform>  experimentPlatformMap  = new HashMap<Integer, ExperimentPlatform>();
	private final List<ExperimentPlatform>              experimentPlatformList = new ArrayList<ExperimentPlatform>();

	private final HashMap<Integer, Organism>            organismMap  = new HashMap<Integer, Organism>();
	private final  List<Organism>                       organismList = new ArrayList<Organism>();

	private final HashMap<Integer, GenomeVersion>       genomeVersionMap  = new HashMap<Integer, GenomeVersion>();
	private final List<GenomeVersion>                   genomeVersionList = new ArrayList<GenomeVersion>();
	private final HashMap<Integer, List<GenomeVersion>> organismToGenomeVersionMap = new HashMap<Integer, List<GenomeVersion>>();

	private final HashMap<Integer, UserGroup>           groupMap  = new HashMap<Integer, UserGroup>();
	private final List<UserGroup>                       groupList = new ArrayList<UserGroup>();

	private final HashMap<Integer, User>                userMap  = new HashMap<Integer, User>();
	private final List<User>                            userList = new ArrayList<User>();

	private final List<Visibility>                      visibilityList = new ArrayList<Visibility>();

	public static DictionaryHelper getInstance(Session sess) {
		if (!theDictionaryHelper.isLoaded) {
			theDictionaryHelper.load(sess);
		}
		return theDictionaryHelper;
	}
	
	public static DictionaryHelper reload(Session sess) {
	    theDictionaryHelper.analysisTypeMap.clear();
	    theDictionaryHelper.analysisTypeList.clear();
	    theDictionaryHelper.experimentMethodMap.clear();
	    theDictionaryHelper.experimentMethodList.clear();
	    theDictionaryHelper.experimentPlatformMap.clear();
	    theDictionaryHelper.experimentPlatformList.clear();
	    theDictionaryHelper.organismMap.clear();
	    theDictionaryHelper.organismList.clear();
	    theDictionaryHelper.genomeVersionMap.clear();
	    theDictionaryHelper.genomeVersionList.clear();
	    theDictionaryHelper.organismToGenomeVersionMap.clear();
	    theDictionaryHelper.groupMap.clear();
	    theDictionaryHelper.groupList.clear();
	    theDictionaryHelper.userMap.clear();
	    theDictionaryHelper.userList.clear();
	    theDictionaryHelper.visibilityList.clear();
	    
		theDictionaryHelper.load(sess);
		return theDictionaryHelper;
	}
	@SuppressWarnings("unchecked")
	private void load(Session sess) {
		List<AnalysisType> entries = (List<AnalysisType>) sess
		        .createQuery(
		                "SELECT d from AnalysisType d order by d.name")
		        .list();
		for (AnalysisType d : entries) {
			analysisTypeMap.put(d.getIdAnalysisType(), d);
			analysisTypeList.add(d);
		}
		
		List<ExperimentMethod> experimentMethods = (List<ExperimentMethod>) sess
		        .createQuery("SELECT d from ExperimentMethod d order by d.name")
		        .list();
		for (ExperimentMethod d : experimentMethods) {
			experimentMethodMap.put(d.getIdExperimentMethod(), d);
			experimentMethodList.add(d);
		}
		
		List<ExperimentPlatform> experimentPlatforms = (List<ExperimentPlatform>) sess
		        .createQuery("SELECT d from ExperimentPlatform d order by d.name")
		        .list();
		for (ExperimentPlatform d : experimentPlatforms) {
			experimentPlatformMap.put(d.getIdExperimentPlatform(), d);
			experimentPlatformList.add(d);
		}
		
		
		List<Visibility> visibilities = (List<Visibility>) sess
		        .createQuery("SELECT d from Visibility d order by d.name")
		        .list();
		for (Visibility d : visibilities) {
			visibilityList.add(d);
		}
		
		List<Organism> organisms = (List<Organism>) sess
        	.createQuery(
                "SELECT d from Organism d order by d.binomialName")
                .list();
		for (Organism d : organisms) {
			organismMap.put(d.getIdOrganism(), d);
			organismList.add(d);
		}
		
		List<GenomeVersion> genomeVersions = (List<GenomeVersion>) sess
        	.createQuery(
                "SELECT d from GenomeVersion d order by d.buildDate desc, d.name asc")
                .list();
		for (GenomeVersion d : genomeVersions) {
			genomeVersionMap.put(d.getIdGenomeVersion(), d);
			genomeVersionList.add(d);
			
			List<GenomeVersion> versions = organismToGenomeVersionMap.get(d.getIdOrganism());
			if (versions == null) {
				versions = new ArrayList<GenomeVersion>();
				organismToGenomeVersionMap.put(d.getIdOrganism(), versions);
			}
			versions.add(d);
		}
				
		List<UserGroup> groups = (List<UserGroup>) sess
        	.createQuery(
                "SELECT d from UserGroup d order by d.name")
                .list();
		for (UserGroup d : groups) {
			groupMap.put(d.getIdUserGroup(), d);
			groupList.add(d);
		}
		
		List<User> users = (List<User>) sess
    	.createQuery(
            "SELECT d from User d order by d.lastName, d.firstName, d.middleName")
            .list();
		for (User d : users) {
			userMap.put(d.getIdUser(), d);
			userList.add(d);
		}		
		
		isLoaded = true;

	}

	@SuppressWarnings("unchecked")
	public Document getXML(GenoPubSecurity genoPubSecurity) {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Dictionaries");

		//
		// AnalysisType
		//
		// Create two hierarchies - one for dropdowns 
		// and another used for dictionary editing.
		//
		Element dictEdit = root.addElement("Dictionary");
		dictEdit.addAttribute("dictionaryName", "AnalysisType");
		dictEdit.addAttribute("dictionaryDisplayName", "Analysis Type");
		dictEdit .addAttribute("label", "Analysis Types");
		
		Element dict = root.addElement("AnalysisTypes");
		this.makeBlankNode(dict, "AnalysisType");
		
		for (AnalysisType d : analysisTypeList) {
			Element dictEntry = dictEdit.addElement("DictionaryEntry");
			dictEntry.addAttribute("dictionaryName", "AnalysisType");
			dictEntry.addAttribute("dictionaryDisplayName", "Analysis Type");
			dictEntry.addAttribute("id",       d.getIdAnalysisType().toString());
			dictEntry.addAttribute("name",     d.getName());
			dictEntry.addAttribute("label",    d.getName());
			dictEntry.addAttribute("isActive", d.getIsActive());
			dictEntry.addAttribute("type",     "DictionaryEntry");
			dictEntry.addAttribute("canWrite", genoPubSecurity.canWrite(d) ? "Y" : "N");
			dictEntry.addAttribute("idUser",   d.getIdUser() != null ? d.getIdUser().toString() : "");
			dictEntry.addAttribute("owner",    this.getUserFullName(d.getIdUser()));
			
			Element de = (Element)dictEntry.clone();
			de.setName("AnalysisType");
			dict.add(de);
		}
		

		//
		// ExperimentMethods
		//
		// Create two hierarchies - one for dropdowns 
		// and another used for dictionary editing.
		//
		dictEdit = root.addElement("Dictionary");
		dictEdit.addAttribute("dictionaryName", "ExperimentMethod");
		dictEdit.addAttribute("dictionaryDisplayName", "Experiment Method");
		dictEdit.addAttribute("label", "Experiment Methods");

		dict = root.addElement("ExperimentMethods");
		this.makeBlankNode(dict, "ExperimentMethod");
		
		for (ExperimentMethod d : experimentMethodList) {
			Element dictEntry = dictEdit.addElement("DictionaryEntry");
			dictEntry.addAttribute("dictionaryName", "ExperimentMethod");
			dictEntry.addAttribute("dictionaryDisplayName", "Experiment Method");
			dictEntry.addAttribute("id",       d.getIdExperimentMethod().toString());
			dictEntry.addAttribute("name",     d.getName());
			dictEntry.addAttribute("label",    d.getName());
			dictEntry.addAttribute("isActive", d.getIsActive());
			dictEntry.addAttribute("type",     "DictionaryEntry");
			dictEntry.addAttribute("canWrite", genoPubSecurity.canWrite(d) ? "Y" : "N");
			dictEntry.addAttribute("idUser",   d.getIdUser() != null ? d.getIdUser().toString() : "");
			dictEntry.addAttribute("owner",    this.getUserFullName(d.getIdUser()));
			
			Element de = (Element)dictEntry.clone();
			de.setName("ExperimentMethod");
			dict.add(de);
		}
		

		//
		// ExperimentPlatforms
		//
		// Create two hierarchies - one for dropdowns 
		// and another used for dictionary editing.
		//

		dictEdit = root.addElement("Dictionary");
		dictEdit.addAttribute("dictionaryName", "ExperimentPlatform");
		dictEdit.addAttribute("dictionaryDisplayName", "Experiment Platform");
		dictEdit.addAttribute("label", "Experiment Platforms");

		dict = root.addElement("ExperimentPlatforms");
		this.makeBlankNode(dict, "ExperimentPlatform");
		
		for (ExperimentPlatform d : experimentPlatformList) {
			Element dictEntry = dictEdit.addElement("DictionaryEntry");
			dictEntry.addAttribute("dictionaryName", "ExperimentPlatform");
			dictEntry.addAttribute("dictionaryDisplayName", "Experiment Platform");
			dictEntry.addAttribute("id",       d.getIdExperimentPlatform().toString());
			dictEntry.addAttribute("name",     d.getName());
			dictEntry.addAttribute("label",    d.getName());
			dictEntry.addAttribute("isActive", d.getIsActive());
			dictEntry.addAttribute("type",     "DictionaryEntry");
			dictEntry.addAttribute("canWrite", genoPubSecurity.canWrite(d) ? "Y" : "N");
			dictEntry.addAttribute("idUser",   d.getIdUser() != null ? d.getIdUser().toString() : "");
			dictEntry.addAttribute("owner",    this.getUserFullName(d.getIdUser()));
			
			Element de = (Element)dictEntry.clone();
			de.setName("ExperimentPlatform");
			dict.add(de);
		}
		
		
		//
		// Visibility
		//
		dict = root.addElement("Visibilities");
		makeBlankNode(dict, "Visibility");
		for (Visibility d : visibilityList) {
			Element dictEntry = dict.addElement("Visibility");
			dictEntry.addAttribute("id",       d.getCodeVisibility());
			dictEntry.addAttribute("name",     d.getName());
		}

		//
		// Genome versions
		//
		dict = root.addElement("GenomeVersions");
		makeBlankNode(dict, "GenomeVersion", "name", "Genome version...");
		for (GenomeVersion d : genomeVersionList) {
			Element dictEntry = dict.addElement("GenomeVersion");
			dictEntry.addAttribute("id",         d.getIdGenomeVersion().toString());
			dictEntry.addAttribute("name",       d.getName());
			dictEntry.addAttribute("idOrganism", d.getIdOrganism().toString());
		}

		dict = root.addElement("Organisms");
		makeBlankNode(dict, "Organism", "binomialName", "Species...");
		for (Organism d : organismList) {
			Element dictEntry = dict.addElement("Organism");
			dictEntry.addAttribute("id",   d.getIdOrganism().toString());
			dictEntry.addAttribute("name", d.getName());
			dictEntry.addAttribute("binomialName", d.getBinomialName());
			dictEntry.addAttribute("commonName", d.getCommonName());
			
			makeBlankNode(dictEntry, "GenomeVersion", "name", "Genome version...");
			if (this.getGenomeVersions(d.getIdOrganism()) != null) {
				for (GenomeVersion gv : this.getGenomeVersions(d.getIdOrganism())) {
					Element de = dictEntry.addElement("GenomeVersion");
					de.addAttribute("id",         gv.getIdGenomeVersion().toString());
					de.addAttribute("name",       gv.getName());
					de.addAttribute("idOrganism", gv.getIdOrganism().toString());
				}
				
			}
			
		}
		
		
		//
		// Security groups
		//
		dict = root.addElement("UserGroups");
		Element blank = makeBlankNode(dict, "UserGroup", "promptedName", "User group...");
		blank.addAttribute("isPartOf", "N");
		for (UserGroup d : groupList) {
			Element dictEntry = dict.addElement("UserGroup");
			dictEntry.addAttribute("id",         d.getIdUserGroup().toString());
			dictEntry.addAttribute("name",       d.getName());
			dictEntry.addAttribute("promptedName",  d.getName());
			
			dictEntry.addAttribute("isPartOf",         genoPubSecurity.isAdminRole() || genoPubSecurity.belongsToGroup(d) ? "Y" : "N");
			dictEntry.addAttribute("isMemberOf",       genoPubSecurity.isAdminRole() || genoPubSecurity.isMember(d) ? "Y" : "N");
			dictEntry.addAttribute("isManagerOf",      genoPubSecurity.isAdminRole() || genoPubSecurity.isManager(d) ? "Y" : "N");
			dictEntry.addAttribute("isCollaboratorOf", genoPubSecurity.isAdminRole() || genoPubSecurity.isCollaborator(d) ? "Y" : "N");

			Element membersNode = dictEntry.addElement("Members");
			makeBlankNode(membersNode, "User");
			for (User member : (Set<User>)d.getMembers()) {
				Element memberNode = membersNode.addElement("User");
				memberNode.addAttribute("id",         member.getIdUser().toString());
				memberNode.addAttribute("name",       member.getName());
			}

			Element collaboratorsNode = dictEntry.addElement("Collaborators");
			makeBlankNode(collaboratorsNode, "User");
			for (User member : (Set<User>)d.getCollaborators()) {
				Element memberNode = collaboratorsNode.addElement("User");
				memberNode.addAttribute("id",         member.getIdUser().toString());
				memberNode.addAttribute("name",       member.getName());
			}

			Element managersNode = dictEntry.addElement("Managers");
			makeBlankNode(managersNode, "User");
			for (User member : (Set<User>)d.getManagers()) {
				Element memberNode = managersNode.addElement("User");
				memberNode.addAttribute("id",         member.getIdUser().toString());
				memberNode.addAttribute("name",       member.getName());
			}

		}

		// 
		// Users
		//
		dict = root.addElement("Users");
		makeBlankNode(dict, "User");
		for (User d : userList) {
			Element dictEntry = dict.addElement("User");
			dictEntry.addAttribute("id",         d.getIdUser().toString());
			dictEntry.addAttribute("name",       d.getName());
		}
		
		return doc;
	}
	
	private Element makeBlankNode(Element parentNode, String name) {
		Element node = parentNode.addElement(name);
		node.addAttribute("id",   "");
		node.addAttribute("name", "");
		return node;
	}
	
	private Element makeBlankNode(Element parentNode, String name, String displayAttributeName, String display) {
		Element node = parentNode.addElement(name);
		node.addAttribute("id",   "");
		node.addAttribute(displayAttributeName, display);
		
		if (!displayAttributeName.equals("name")) {
			node.addAttribute("name", "");			
		}
		return node;
	}

	public String getAnalysisType(Integer id) {
		if (id == null) {
			return "";
		}
		AnalysisType d = analysisTypeMap.get(id);
		if (d != null) {
			return d.getName();
		} else {
			return "";
		}
	}
	
	public String getExperimentMethod(Integer id) {
		if (id == null) {
			return "";
		}
		ExperimentMethod d = experimentMethodMap.get(id);
		if (d != null) {
			return d.getName();
		} else {
			return "";
		}
	}
	
	public String getExperimentPlatform(Integer id) {
		if (id == null) {
			return "";
		}
		ExperimentPlatform d = experimentPlatformMap.get(id);
		if (d != null) {
			return d.getName();
		} else {
			return "";
		}
	}
	
	public List<Organism> getOrganisms() {
		return this.organismList;
	}
	
	public List<GenomeVersion> getGenomeVersions(Integer idOrganism) {
		return this.organismToGenomeVersionMap.get(idOrganism);
	}
	
	public String getUserFullName(Integer idUser) {
		User user = userMap.get(idUser);
		if (user != null) {
			return user.getName();
		} else {
			return "";
		}
	}
	public String getUserEmail(Integer idUser) {
		User user = userMap.get(idUser);
		if (user != null) {
			return user.getEmail();
		} else {
			return null;
		}
	}
	public String getUserInstitute(Integer idUser) {
		User user = userMap.get(idUser);
		if (user != null) {
			return user.getInstitute();
		} else {
			return null;
		}
	}
	
	public String getOrganismName(Integer idOrganism) {
		Organism organism = organismMap.get(idOrganism);
		if (organism != null) {
			return organism.getName();
		} else {
			return "";
		}
	}
	public String getOrganismName(GenomeVersion genomeVersion) {
		if (genomeVersion != null && genomeVersion.getIdOrganism() != null) {
			Organism organism = organismMap.get(genomeVersion.getIdOrganism());
			if (organism != null) {
				return organism.getName();
			} else {
				return "";
			}			
		} else {
			return "";
		}
	}
	public String getOrganismBinomialName(Integer idOrganism) {
		Organism organism = organismMap.get(idOrganism);
		if (organism != null) {
			return organism.getBinomialName();
		} else {
			return "";
		}
	}
	public String getOrganismBinomialName(GenomeVersion genomeVersion) {
		if (genomeVersion != null && genomeVersion.getIdOrganism() != null) {
			Organism organism = organismMap.get(genomeVersion.getIdOrganism());
			if (organism != null) {
				return organism.getBinomialName();
			} else {
				return "";
			}			
		} else {
			return "";
		}
	}
	public String getGenomeVersionName(Integer idGenomeVersion) {
		GenomeVersion genomeVersion = genomeVersionMap.get(idGenomeVersion);
		if (genomeVersion != null) {
			return genomeVersion.getName();
		} else {
			return "";
		}
	}
	public GenomeVersion getGenomeVersion(Integer idGenomeVersion) {
		GenomeVersion genomeVersion = genomeVersionMap.get(idGenomeVersion);
		return genomeVersion;
	}	
	
	public String getUserGroupName(Integer idUserGroup) {
		UserGroup group = groupMap.get(idUserGroup);
		if (group != null) {
			return group.getName();
		} else {
			return "";
		}
	}
	public String getUserGroupContact(Integer idUserGroup) {
		UserGroup group = groupMap.get(idUserGroup);
		if (group != null) {
			return group.getContact();
		} else {
			return null;
		}
	}
	public String getUserGroupEmail(Integer idUserGroup) {
		UserGroup group = groupMap.get(idUserGroup);
		if (group != null) {
			return group.getEmail();
		} else {
			return null;
		}
	}
	public String getUserGroupInstitute(Integer idUserGroup) {
		UserGroup group = groupMap.get(idUserGroup);
		if (group != null) {
			return group.getInstitute();
		} else {
			return null;
		}
	}

}
