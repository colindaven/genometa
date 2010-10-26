package com.affymetrix.genometry.genopub;

import java.io.Serializable;
import java.sql.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import com.affymetrix.genometry.genopub.Annotation;
import com.affymetrix.genometry.genopub.AnnotationGrouping;

public class AnnotationGrouping implements Serializable {
    
    private Integer            idAnnotationGrouping;
    private String             name;
    private String             description;
    private Integer            idParentAnnotationGrouping;
    private AnnotationGrouping parentAnnotationGrouping;
    private Set                annotationGroupings;
    private Set                annotations;
    private Integer            idUserGroup;
    private Integer            idGenomeVersion;
    private String             createdBy;
    private Date               createDate;

    
    public Integer getIdAnnotationGrouping() {
        return idAnnotationGrouping;
    }
    public void setIdAnnotationGrouping(Integer idAnnotationGrouping) {
        this.idAnnotationGrouping = idAnnotationGrouping;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    public Set getAnnotationGroupings() {
        return annotationGroupings;
    }
    public void setAnnotationGroupings(Set annotationGroupings) {
        this.annotationGroupings = annotationGroupings;
    }
    public Integer getIdParentAnnotationGrouping() {
        return idParentAnnotationGrouping;
    }
    public void setIdParentAnnotationGrouping(Integer idParentAnnotationGrouping) {
        this.idParentAnnotationGrouping = idParentAnnotationGrouping;
    }
    public Set getAnnotations() {
        return annotations;
    }
    public void setAnnotations(Set annotations) {
        this.annotations = annotations;
    }
    public AnnotationGrouping getParentAnnotationGrouping() {
      return parentAnnotationGrouping;
    }
    public void setParentAnnotationGrouping(
        AnnotationGrouping parentAnnotationGrouping) {
      this.parentAnnotationGrouping = parentAnnotationGrouping;
    }


    public boolean hasVisibility(String codeVisibility) {
    	boolean hasVisibility = false;
    	for(Iterator<?> i = this.annotations.iterator(); i.hasNext();) {
    		Annotation a = Annotation.class.cast(i.next());
    		if (a.getCodeVisibility().equals(codeVisibility)) {
    			hasVisibility = true;
    			break;
    		}
    	}
    	return hasVisibility;
    }

    public Integer getIdGenomeVersion() {
      return idGenomeVersion;
    }
    public void setIdGenomeVersion(Integer idGenomeVersion) {
      this.idGenomeVersion = idGenomeVersion;
    }
    public Integer getIdUserGroup() {
      return idUserGroup;
    }
    public void setIdUserGroup(Integer idUserGroup) {
      this.idUserGroup = idUserGroup;
    } 

    /*
     * Get the type name (no genome version in path)
     */
    public String getQualifiedTypeName() {
    	if (getIdParentAnnotationGrouping() == null) {
    		return "";
    	}
    	
        String typeName = getName();
        typeName = recurseGetParentNameExcludingRoot(typeName);			 		
        return typeName;
      }

    /*
     * Get the fully qualifed type name (with genome version in path)
     */
    public String getQualifiedName() {
      String qualifiedName = getName();
      qualifiedName = recurseGetParentName(qualifiedName);			 		
      return qualifiedName;
    }

    private String recurseGetParentName(String qualifiedName) {
      AnnotationGrouping parent = this.getParentAnnotationGrouping();
      
      if (parent != null) {
        if (parent.getName() != null) {
          qualifiedName = parent.getName() + "/" + qualifiedName;

          qualifiedName = parent.recurseGetParentName(qualifiedName);
        }
      }
      return qualifiedName;
    }

    private String recurseGetParentNameExcludingRoot(String typeName) {
        AnnotationGrouping parent = this.getParentAnnotationGrouping();

       
        if (parent != null) {
          if (parent.getName() != null) {
        	  // Stop before the root annotation grouping
        	  if (parent.getIdParentAnnotationGrouping() != null) {
                  typeName = parent.getName() + "/" + typeName;

                  typeName = parent.recurseGetParentNameExcludingRoot(typeName);
        		  
        	  }
          }
        }
        return typeName;
      }

    

    public void recurseGetChildren(List<Object> descendents) {
    	for(Iterator<?> i = this.getAnnotationGroupings().iterator(); i.hasNext();) {        
    		AnnotationGrouping ag = AnnotationGrouping.class.cast(i.next());
    		descendents.add(ag);
    		ag.recurseGetChildren(descendents);
    	}
    	for(Iterator<?> i = this.getAnnotations().iterator(); i.hasNext();) {
    		Annotation a = Annotation.class.cast(i.next());
    		descendents.add(a);
    	}
    }
    
    public String getCreatedBy() {
      return createdBy;
    }
    public void setCreatedBy(String createdBy) {
      this.createdBy = createdBy;
    }
    public Date getCreateDate() {
      return createDate;
    }
    public void setCreateDate(Date createDate) {
      this.createDate = createDate;
    }

    public Document getXML(GenoPubSecurity genoPubSecurity, DictionaryHelper dictionaryHelper) {
      Document doc = DocumentHelper.createDocument();
      Element root = doc.addElement("AnnotationGrouping");

      GenomeVersion genomeVersion = dictionaryHelper.getGenomeVersion(this.getIdGenomeVersion());

      root.addAttribute("label", this.getName());	
      root.addAttribute("idAnnotationGrouping", this.getIdAnnotationGrouping().toString());	
      root.addAttribute("idGenomeVersion", genomeVersion.getIdGenomeVersion().toString());	
      root.addAttribute("genomeVersion", genomeVersion.getName());	
      root.addAttribute("name", this.getName().toString());	
      root.addAttribute("description", this.getDescription() != null ? this.getDescription() : "");	
      root.addAttribute("userGroup", dictionaryHelper.getUserGroupName(this.getIdUserGroup()));
      root.addAttribute("idUserGroup",this.getIdUserGroup() != null ? this.getIdUserGroup().toString() : "");
      root.addAttribute("createdBy", this.getCreatedBy() != null ? this.getCreatedBy() : "");
      root.addAttribute("createDate", this.getCreateDate() != null ? Util.formatDate(this.getCreateDate()) : "");

      root.addAttribute("canWrite",    genoPubSecurity.canWrite(this) ? "Y" : "N");

      return doc;
    }
}
