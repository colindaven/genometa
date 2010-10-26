package com.affymetrix.genometry.genopub;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.sql.Date;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;


public class GenomeVersion implements Serializable {

  private Integer  idGenomeVersion;
  private String   name;
  private Date     buildDate;
  private String   coordURI;
  private String   coordAuthority;
  private String   coordVersion;
  private String   coordSource;
  private String   coordTestRange;
  private Set      segments;
  private Set      aliases;
  private Integer  idOrganism;
  private Set      annotationGroupings;
  private Set      annotations;

  public Integer getIdGenomeVersion() {
    return idGenomeVersion;
  }
  public void setIdGenomeVersion(Integer idGenomeVersion) {
    this.idGenomeVersion = idGenomeVersion;
  }
  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  public Date getBuildDate() {
    return buildDate;
  }
  public void setBuildDate(Date buildDate) {
    this.buildDate = buildDate;
  }
  public String getCoordURI() {
    return coordURI;
  }
  public void setCoordURI(String coordURI) {
    this.coordURI = coordURI;
  }
  public String getCoordAuthority() {
    return coordAuthority;
  }
  public void setCoordAuthority(String coordAuthority) {
    this.coordAuthority = coordAuthority;
  }
  public String getCoordVersion() {
    return coordVersion;
  }
  public void setCoordVersion(String coordVersion) {
    this.coordVersion = coordVersion;
  }
  public String getCoordSource() {
    return coordSource;
  }
  public void setCoordSource(String coordSource) {
    this.coordSource = coordSource;
  }
  public String getCoordTestRange() {
    return coordTestRange;
  }
  public void setCoordTestRange(String coordTestRange) {
    this.coordTestRange = coordTestRange;
  }
  public Set getSegments() {
    return segments;
  }
  public void setSegments(Set segments) {
    this.segments = segments;
  }
  public Set getAliases() {
    return aliases;
  }
  public void setAliases(Set aliases) {
    this.aliases = aliases;
  }
  public Set getAnnotationGroupings() {
    return annotationGroupings;
  }
  public void setAnnotationGroupings(Set annotationGroupings) {
    this.annotationGroupings = annotationGroupings;
  }

  @SuppressWarnings("unchecked")
  private List getRootAnnotationGroupings() {
    ArrayList rootGroupings = new ArrayList();
    for (AnnotationGrouping annotationGrouping : (Set<AnnotationGrouping>) this
        .getAnnotationGroupings()) {
      if (annotationGrouping.getIdParentAnnotationGrouping() == null) {
        rootGroupings.add(annotationGrouping);
      }
    }
    return rootGroupings;
  }

  public AnnotationGrouping getRootAnnotationGrouping() {
    List rootGroupings = this.getRootAnnotationGroupings();
    if (rootGroupings.size() > 0) {
      return AnnotationGrouping.class.cast(rootGroupings.get(0));
    } else {
      return null;
    }
  }

  public Integer getIdOrganism() {
    return idOrganism;
  }
  public void setIdOrganism(Integer idOrganism) {
    this.idOrganism = idOrganism;
  }
  public Set getAnnotations() {
    return annotations;
  }
  public void setAnnotations(Set annotations) {
    this.annotations = annotations;
  }

	
	@SuppressWarnings("unchecked")
	public Document getXML(GenoPubSecurity genoPubSecurity, String data_root) {
		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("GenomeVersion");
		
		root.addAttribute("label", this.getName());				
		root.addAttribute("idGenomeVersion",this.getIdGenomeVersion().toString());				
		root.addAttribute("name",           this.getName());				
		root.addAttribute("buildDate",      this.getBuildDate() != null ? Util.formatDate(this.getBuildDate()) : "");				
		root.addAttribute("idOrganism",     this.getIdOrganism().toString());				
		root.addAttribute("coordURI",       this.getCoordURI() != null ? this.getCoordURI().toString() : "");	
		root.addAttribute("coordVersion",   this.getCoordVersion() != null ? this.getCoordVersion().toString() : "");	
		root.addAttribute("coordSource",    this.getCoordSource() != null ? this.getCoordSource().toString() : "");	
		root.addAttribute("coordTestRange", this.getCoordTestRange() != null ? this.getCoordTestRange().toString() : "");	
		root.addAttribute("coordAuthority", this.getCoordAuthority() != null ? this.getCoordAuthority().toString() : "");	
		
		// Only show the sequence files and segments for genome version detail 
		// (if data_root provided).
		if (data_root != null) {

		  // Sequence files
		  Element filesNode = root.addElement("SequenceFiles");

		  String filePath = getSequenceDirectory(data_root);
		  File fd = new File(filePath);
		  if (fd.exists()) {


		    Element fileNode = filesNode.addElement("Dir");
		    fileNode.addAttribute("name", getSequenceFileName());
		    fileNode.addAttribute("url", filePath);
		    appendSequenceFileXML(filePath, fileNode, null);	    	
		  }

		  // Segments
		  Element segmentsNode = root.addElement("Segments");
		  for (Segment segment : (Set<Segment>)this.getSegments()) {
		    Element sNode = segmentsNode.addElement("Segment");
		    sNode.addAttribute("idSegment", segment.getIdSegment().toString());
		    sNode.addAttribute("name", segment.getName());

		    sNode.addAttribute("length", segment.getLength() != null ? NumberFormat.getInstance().format(segment.getLength()) : "");
		    sNode.addAttribute("sortOrder", segment.getSortOrder() != null ? segment.getSortOrder().toString() : "");
		  }
		}


		root.addAttribute("canRead", genoPubSecurity.canRead(this) ? "Y" : "N");
		root.addAttribute("canWrite", genoPubSecurity.canWrite(this) ? "Y" : "N");

		return doc;
	}
	
	public String getSequenceDirectory(String data_root) {
		return data_root  + getSequenceFileName();
	}
	
	
	public  String getSequenceFileName() {
	  return Constants.SEQUENCE_DIR_PREFIX + this.getIdGenomeVersion();
	}
	
	public boolean hasSequence(String data_root) throws IOException {

	  boolean hasSequence = false;
    String filePath = getSequenceDirectory(data_root);
    File dir = new File(filePath);

    if (dir.exists()) {
      // Delete the files in the directory
      String[] childFileNames = dir.list();
      if (childFileNames != null && childFileNames.length > 0) {
        hasSequence = true;
      }
    }
    
    return hasSequence;
  }

	public void removeSequenceFiles(String data_root) throws IOException {

	  String filePath = getSequenceDirectory(data_root);
	  File dir = new File(filePath);

	  if (dir.exists()) {
	    // Delete the files in the directory
	    String[] childFileNames = dir.list();
	    if (childFileNames != null) {
	      for (int x = 0; x < childFileNames.length; x++) {
	        String fileName = filePath + "/" + childFileNames[x];
	        File f = new File(fileName);
	        boolean success = f.delete();
	        if (!success) {
				Logger.getLogger(GenomeVersion.class.getName()).log(Level.WARNING, "Unable to delete file " + fileName);
	        }
	      }

	    }

	    // Delete the annotation directory
	    boolean success = dir.delete();
	    if (!success) {
			Logger.getLogger(GenomeVersion.class.getName()).log(Level.WARNING, "Unable to delete directory " + filePath);	    	
	    }
	  }
	}

	public static void appendSequenceFileXML(String filePath, Element parentNode, String subDirName) {
		File fd = new File(filePath);

		if (fd.isDirectory()) {
			String[] fileList = fd.list();
			for (int x = 0; x < fileList.length; x++) {
				String fileName = filePath + "/" + fileList[x];
				File f1 = new File(fileName);

				// Show the subdirectory in the name if we are not at the main folder level
				String displayName = "";
				if (subDirName != null) {
					displayName = subDirName + "/" + fileList[x];
				} else {
					displayName = f1.getName();
				}

				if (f1.isDirectory()) {
					Element fileNode = parentNode.addElement("Dir");
					fileNode.addAttribute("name", displayName);
					fileNode.addAttribute("url", fileName);
					appendSequenceFileXML(fileName, fileNode,
					        subDirName != null ? subDirName + "/"
					                + f1.getName() : f1.getName());
				} else {
					Element fileNode = parentNode.addElement("File");

					long kb = Util.getKilobytes(f1.length());
					String kilobytes = kb + " kb";
					
					fileNode.addAttribute("name", displayName);
					fileNode.addAttribute("url", fileName);
					fileNode.addAttribute("size", kilobytes);
					fileNode.addAttribute("lastModified", Util.formatDate(new java.sql.Date(f1.lastModified())));

				}
			}
		}
	}


}
