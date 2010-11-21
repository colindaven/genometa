package com.affymetrix.genometry.genopub;

import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class Organism {
    
    private Integer idOrganism;
    private String  name;
    private String  commonName;
    private String  binomialName;
    private String  NCBITaxID;
    private Integer sortOrder;
    private Set     genomeVersions;
    
    public Integer getIdOrganism() {
        return idOrganism;
    }
    public void setIdOrganism(Integer idOrganism) {
        this.idOrganism = idOrganism;
    }

    public Set getGenomeVersions() {
        return genomeVersions;
    }
    public void setGenomeVersions(Set genomeVersions) {
        this.genomeVersions = genomeVersions;
    }
    public String getCommonName() {
        return commonName;
    }
    public void setCommonName(String commonName) {
        this.commonName = commonName;
    }
    public Integer getSortOrder() {
        return sortOrder;
    }
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
    public String getBinomialName() {
        return binomialName;
    }
    public void setBinomialName(String binomialName) {
        this.binomialName = binomialName;
    }
    public String getNCBITaxID() {
        return NCBITaxID;
    }
    public void setNCBITaxID(String taxID) {
        NCBITaxID = taxID;
    }
	public String getName() {
    	return name;
    }
	public void setName(String name) {
    	this.name = name;
    }
    
	public Document getXML(GenoPubSecurity genoPubSecurity) {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Organism");
		
		root.addAttribute("label",        this.getBinomialName());
		root.addAttribute("idOrganism",   this.getIdOrganism().toString());
		root.addAttribute("name",         this.getName() != null ? this.getName() : "");				
		root.addAttribute("commonName",   this.getCommonName() != null ? this.getCommonName() : "");				
		root.addAttribute("binomialName", this.getBinomialName() != null ? this.getBinomialName() : "");				
		root.addAttribute("NCBITaxID",    this.getNCBITaxID() != null ? this.getNCBITaxID() : "");		
		root.addAttribute("canWrite",     genoPubSecurity.canWrite(this) ? "Y" : "N");

		return doc;
	}
    

}
