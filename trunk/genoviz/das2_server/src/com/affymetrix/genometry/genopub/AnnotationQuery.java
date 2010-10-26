package com.affymetrix.genometry.genopub;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.hibernate.Query;
import org.hibernate.Session;


public class AnnotationQuery {

  
	// Criteria
	private String             scopeLevel = "";
	private Integer            idUserGroup;
	private Integer            idOrganism;
	private Integer            idGenomeVersion;
	private String             isVisibilityPublic = "Y";
	private String             isVisibilityMembers = "Y";
	private String             isVisibilityMembersAndCollabs = "Y";
	private String             isServerRefreshMode = "N";

    
	private StringBuffer        queryBuf;
	private boolean            addWhere = true;
	
	private static String      KEY_DELIM = "!!!!";
	
	private static final int ANNOTATION_GROUPING_LEVEL = 1;
	private static final int ANNOTATION_LEVEL = 2;
	
	
	private TreeMap<String, TreeMap<GenomeVersion, ?>> organismToVersion;
	private HashMap<String, TreeMap<String, ?>>        versionToRootGroupings;
	private HashMap<String, TreeMap<String, ?>>        groupingToGroupings;
	private HashMap<String, TreeMap<String, ?>>        groupingToAnnotations;
	
	private HashMap<String, List<Segment>>             versionToSegments;  
	
	private HashMap<String,  Organism>                 organismMap;
	private HashMap<String,  GenomeVersion>            genomeVersionNameMap;
	private HashMap<Integer, Annotation>               annotationMap;
	private HashMap<Integer, AnnotationGrouping>       annotationGroupingMap;
	
	@SuppressWarnings("unchecked")
	public static List<UnloadAnnotation> getUnloadedAnnotations(Session sess, GenoPubSecurity genoPubSecurity, GenomeVersion genomeVersion) throws Exception {
		StringBuffer queryBuf = new StringBuffer();
		queryBuf.append(" SELECT     u  ");
		queryBuf.append(" FROM       UnloadAnnotation as u  ");
		queryBuf.append(" WHERE      u.idGenomeVersion = " + genomeVersion.getIdGenomeVersion());
		
		if (genoPubSecurity != null && !genoPubSecurity.isAdminRole()) {
			queryBuf.append(" AND u.idUser = " + genoPubSecurity.getIdUser());		
		}

		queryBuf.append(" ORDER BY   u.idUnloadAnnotation");
		
		List<UnloadAnnotation> results = (List<UnloadAnnotation>)sess.createQuery(queryBuf.toString()).list();
		
		return results;
	}
	
	public AnnotationQuery() {
		if (scopeLevel == null || scopeLevel.equals("")) {
			scopeLevel = GenoPubSecurity.ALL_SCOPE_LEVEL;
		}		
	}
	
	public AnnotationQuery(HttpServletRequest req) {
		scopeLevel         = req.getParameter("scopeLevel");
		idUserGroup        = Util.getIntegerParameter(req, "idUserGroup");
		idOrganism         = Util.getIntegerParameter(req, "idOrganism");
		idGenomeVersion    = Util.getIntegerParameter(req, "idGenomeVersion");
		this.isVisibilityMembers = Util.getFlagParameter(req, "isVisibilityMembers");
		this.isVisibilityMembersAndCollabs = Util.getFlagParameter(req, "isVisibilityMembersAndCollabs");
		this.isVisibilityPublic = Util.getFlagParameter(req, "isVisibilityPublic");
		
		if (scopeLevel == null || scopeLevel.equals("")) {
			scopeLevel = GenoPubSecurity.ALL_SCOPE_LEVEL;
		}		
	}

	@SuppressWarnings("unchecked")
	public Document getAnnotationDocument(Session sess, GenoPubSecurity genoPubSecurity) throws Exception {
	  // Run query to get annotation groupings, organized under
	  // organism and genome version
	  StringBuffer queryBuf = this.getAnnotationGroupingQuery(genoPubSecurity);    	
	  Logger.getLogger(this.getClass().getName()).fine("Annotation grouping query: " + queryBuf.toString());
	  Query query = sess.createQuery(queryBuf.toString());
	  List<Object[]> annotationGroupingRows = (List<Object[]>)query.list();

	  // Run query to get annotation grouping and annotations, organized under
	  // organism and genome version
	  queryBuf = this.getAnnotationQuery(genoPubSecurity);
	  Logger.getLogger(this.getClass().getName()).fine("Annotation query: " + queryBuf.toString());
	  query = sess.createQuery(queryBuf.toString());
	  List<Object[]> annotationRows = (List<Object[]>)query.list();

	  // Now run query to get the genome version segments
	  queryBuf = this.getSegmentQuery();
	  query = sess.createQuery(queryBuf.toString());
	  List<Segment> segmentRows = (List<Segment>) query.list();

	  // Create an XML document
	  Document doc = this.getAnnotationDocument(annotationGroupingRows, annotationRows, segmentRows, DictionaryHelper.getInstance(sess), genoPubSecurity);
	  return doc;
		
	}
	

	@SuppressWarnings("unchecked")
	public void runAnnotationQuery(Session sess, GenoPubSecurity genoPubSecurity, boolean isServerRefreshMode) throws Exception {
		this.isServerRefreshMode = isServerRefreshMode ? "Y" : "N";
		
		// Run query to get annotation groupings, organized under
		// organism and genome version
		StringBuffer queryBuf = this.getAnnotationGroupingQuery(genoPubSecurity);    	
		Logger.getLogger(this.getClass().getName()).fine("Annotation grouping query: " + queryBuf.toString());
    	Query query = sess.createQuery(queryBuf.toString());
		List<Object[]> annotationGroupingRows = (List<Object[]>)query.list();
		
		// Run query to get annotations, organized under annotation grouping,
		// organism, and genome version
		queryBuf = this.getAnnotationQuery(genoPubSecurity);    	
		Logger.getLogger(this.getClass().getName()).fine("Annotation query: " + queryBuf.toString());
    	query = sess.createQuery(queryBuf.toString());
		List<Object[]> annotationRows = (List<Object[]>)query.list();
		
		// Now run query to get the genome version segments
		queryBuf = this.getSegmentQuery();			
    	query = sess.createQuery(queryBuf.toString());
		List<Segment> segmentRows = (List<Segment>)query.list();
			
		this.hashAnnotations(annotationGroupingRows, annotationRows, segmentRows, DictionaryHelper.getInstance(sess));
		
	}
	
	private StringBuffer getAnnotationGroupingQuery(GenoPubSecurity genoPubSecurity) throws Exception {
		
		addWhere = true;
		queryBuf = new StringBuffer();

		queryBuf.append(" SELECT     org, ");
		queryBuf.append("            ver, ");
		queryBuf.append("            ag, ");
		queryBuf.append("            pag ");
		queryBuf.append(" FROM       Organism as org ");
		queryBuf.append(" JOIN       org.genomeVersions as ver ");
		queryBuf.append(" JOIN       ver.annotationGroupings as ag ");
		queryBuf.append(" LEFT JOIN  ag.parentAnnotationGrouping as pag ");
		

		addWhere = true;

		addCriteria(ANNOTATION_GROUPING_LEVEL);
		
		if (genoPubSecurity != null) {
			genoPubSecurity.appendAnnotationGroupingHQLSecurity(scopeLevel, queryBuf, "ag", addWhere);			
		}
		
		queryBuf.append(" ORDER BY org.name asc, ver.buildDate desc, ag.name asc ");

		return queryBuf;

	}

	private StringBuffer getAnnotationQuery(GenoPubSecurity genoPubSecurity) throws Exception {
		
		addWhere = true;
		queryBuf = new StringBuffer();

		queryBuf.append(" SELECT     org, ");
		queryBuf.append("            ver, ");
		queryBuf.append("            ag, ");
		queryBuf.append("            pag, ");
		queryBuf.append("            a  ");
		queryBuf.append(" FROM       Organism as org ");
		queryBuf.append(" JOIN       org.genomeVersions as ver ");
		queryBuf.append(" JOIN       ver.annotationGroupings as ag ");
		queryBuf.append(" LEFT JOIN  ag.parentAnnotationGrouping as pag ");
		queryBuf.append(" LEFT JOIN  ag.annotations as a ");
		

		addWhere = true;

		addCriteria(ANNOTATION_LEVEL);
		
		if (genoPubSecurity != null) {
			addWhere = genoPubSecurity.appendAnnotationHQLSecurity(scopeLevel, queryBuf, "a", "ag", addWhere);			
		}
		
		
		// If this is a server reload, get annotations not yet loaded
		if (this.isServerRefreshMode.equals("Y")) {
			this.AND();
			queryBuf.append("(");
			queryBuf.append(" a.isLoaded = 'N' ");

			if (!genoPubSecurity.isAdminRole()) {
				this.AND();
				queryBuf.append(" a.idUser = " + genoPubSecurity.getIdUser());		
			}

			queryBuf.append(")");
		}

		queryBuf.append(" ORDER BY org.name asc, ver.buildDate desc, ag.name asc, a.name asc ");

		return queryBuf;

	}


	
	private StringBuffer getSegmentQuery() throws Exception {
		addWhere = true;
		queryBuf = new StringBuffer();
		queryBuf.append(" SELECT     seg  ");
		queryBuf.append(" FROM       Segment as seg  ");
		queryBuf.append(" ORDER BY   seg.sortOrder");
		
		return queryBuf;

	}

	private Document getAnnotationDocument(List<Object[]> annotationGroupingRows, List<Object[]> annotationRows, List<Segment> segmentRows,  DictionaryHelper dictionaryHelper, GenoPubSecurity genoPubSecurity) throws Exception {
		
		// Organize results rows into hash tables
		hashAnnotations(annotationGroupingRows, annotationRows, segmentRows, dictionaryHelper);		
		

		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("Annotations");
		
				
		// Use hash to create XML Document.  Perform 2 passes so that organisms
		// with populated genome versions (annotations or sequences) appear first
		// in the list.

		fillOrganismNodes(root, dictionaryHelper, genoPubSecurity, true);
		fillOrganismNodes(root, dictionaryHelper, genoPubSecurity, false);

		return doc;
		
	}
	
	private void fillOrganismNodes(Element root, DictionaryHelper dictionaryHelper, GenoPubSecurity genoPubSecurity, boolean fillPopulatedOrganisms) throws Exception {
		Element organismNode = null;
		Element versionNode  = null;
		String[] tokens;
		for (String organismBinomialName : organismToVersion.keySet()) {
			TreeMap<GenomeVersion, ?> versionMap = organismToVersion.get(organismBinomialName);
			Organism organism = organismMap.get(organismBinomialName);
			
			boolean keep = false;
			boolean isPopulated = hasPopulatedGenomes(organism);
			if (fillPopulatedOrganisms) {
				keep = isPopulated;
			} else {
				keep = !isPopulated;
			}
			
			if (!keep) {
				continue;
			}
			
			organismNode = organism.getXML(genoPubSecurity).getRootElement();
			organismNode.addAttribute("isPopulated", isPopulated ? "Y" : "N");
			root.add(organismNode);
			
			// For each version, build up hierarchy
			if (versionMap != null) {
				for (GenomeVersion genomeVersion : versionMap.keySet()) {
					
					versionNode = genomeVersion.getXML(genoPubSecurity, null).getRootElement();
					
					// Indicate if the genome version has segments
					List<Segment> segments = this.getSegments(organism, genomeVersion.getName());
					versionNode.addAttribute("hasSegments", (segments != null && segments.size() > 0 ? "Y" : "N"));					
					
					// Attach the genome version node
					organismNode.add(versionNode);
					
					// For each root annotation grouping, recurse to create annotations
					// and groupings.
					TreeMap<String, ?> rootGroupings = versionToRootGroupings.get(genomeVersion.getName());
					fillGroupingNode(genomeVersion, versionNode, rootGroupings, genoPubSecurity, dictionaryHelper, false);
					
					// If selection criteria was applied to query, prune out nodes that don't 
					// have any content 
					if (this.hasAnnotationCriteria()) {
						if (!versionNode.hasContent()) {
							organismNode.remove(versionNode);					
						}
					}

				}
				
			}
			
			// If selection criteria was applied to query, prune out nodes that don't 
			// have any content 
			if (this.hasAnnotationCriteria()) {
				if (!organismNode.hasContent()) {
					root.remove(organismNode);					
				}
			}
			
		}
		
	}
	
	private boolean hasPopulatedGenomes(Organism organism) {
		boolean isPopulated = false;
		
		TreeMap<GenomeVersion, ?> versionMap = organismToVersion.get(organism.getBinomialName());
		if (versionMap != null) {
			for (GenomeVersion genomeVersion : versionMap.keySet()) {
				if (isPopulated) {
					break;
				}
				List<Segment> segments = this.getSegments(organism, genomeVersion.getName());
				if (segments != null && segments.size() > 0) {
					isPopulated = true;
					break;
				}
				TreeMap<String, ?> rootGroupings = versionToRootGroupings.get(genomeVersion.getName());
				if (rootGroupings != null && rootGroupings.size() > 0) {
					for (String groupingKey : rootGroupings.keySet()) {
						
						//String[] tokens     = groupingKey.split(KEY_DELIM);
						//String groupingName          = tokens[0];
						

						TreeMap<String, ?> annotNameMap = groupingToAnnotations.get(groupingKey);
						if (annotNameMap != null && annotNameMap.size() > 0) {
							isPopulated = true;
						}
						
						TreeMap<String, ?> childGroupings = groupingToGroupings.get(groupingKey);
						if (childGroupings != null && childGroupings.size() > 0) {
							isPopulated = true;
						}
					}
				}
				
				
			}
		}
		return isPopulated;
	}
	

	
	private void hashAnnotations(List<Object[]> annotationGroupingRows, List<Object[]> annotationRows, List<Segment> segmentRows, DictionaryHelper dictionaryHelper) {

		organismToVersion        = new TreeMap<String, TreeMap<GenomeVersion, ?>>();
		versionToRootGroupings   = new HashMap<String, TreeMap<String, ?>>();
		groupingToGroupings      = new HashMap<String, TreeMap<String, ?>>();
		groupingToAnnotations    = new HashMap<String, TreeMap<String, ?>>();
		versionToSegments        = new HashMap<String, List<Segment>>();
		
		organismMap              = new HashMap<String, Organism>();
		genomeVersionNameMap     = new HashMap<String, GenomeVersion>();
		annotationGroupingMap    = new HashMap<Integer, AnnotationGrouping>();
		annotationMap            = new HashMap<Integer, Annotation>();
		
		// Prefill organism, genome version, and root annotation grouping
		// hash map with known entries
		// since those without annotations would otherwise not show up.
		for (Organism o : dictionaryHelper.getOrganisms()) {
			organismMap.put(o.getBinomialName(), o);

			// If we are filtering by organism, only include that one
			if (this.idOrganism != null) {
				if (!this.idOrganism.equals(o.getIdOrganism())) {
					continue;
				}
			}
			TreeMap<GenomeVersion, ?> versionMap = new TreeMap<GenomeVersion, Object>(new GenomeVersionComparator());
			organismToVersion.put(o.getBinomialName(), versionMap);
			if (dictionaryHelper.getGenomeVersions(o.getIdOrganism()) != null) {
				for(GenomeVersion v : dictionaryHelper.getGenomeVersions(o.getIdOrganism())) {

					genomeVersionNameMap.put(v.getName(), v);

					// If we are filtering by genome version, only include that one
					if (this.idGenomeVersion != null) {
						if (!this.idGenomeVersion.equals(v.getIdGenomeVersion())) {
							continue;
						}
					}

					versionMap.put(v, null);

					AnnotationGrouping rootGrouping = v.getRootAnnotationGrouping();

					if (rootGrouping != null) {
						String groupingKey       = rootGrouping.getName()  + KEY_DELIM + rootGrouping.getIdAnnotationGrouping();
						TreeMap<String, String> groupings = new TreeMap<String, String>();
						groupings.put(groupingKey, null);
						versionToRootGroupings.put(v.getName(), groupings);
					}

				}

			}
		}
		// Hash segments for each genome version
		if (segmentRows != null) {
			for (Segment segment : segmentRows) {
				if (segment == null) {
					continue;
				}
				GenomeVersion genomeVersion = dictionaryHelper.getGenomeVersion(segment.getIdGenomeVersion());
				if (genomeVersion == null) {
					System.out.println("Warning - Segment " + segment.getIdSegment() + " does not belong to a valid Genome Version");
					continue;
				}
				List<Segment> segments = versionToSegments.get(genomeVersion.getName());
				if (segments == null) {
					segments = new ArrayList<Segment>();
					versionToSegments.put(genomeVersion.getName(), segments);
				}
				segments.add(segment);
			}			
		}

		
		


		
		
		// Hash to create hierarchy:
		//   Organism
		//     Genome Version
		//       Annotation
		//       Annotation Grouping
		//          Annotation

		// Hash the annotation grouping->annotation.  We get
		// a row for each annotation grouping.  
		// 1. Hash the genome versions under the organism.
		// 2. Hash the root annotation groupings under the organism.
		//    (root annotations are under the root annotation grouping for
		//     the genome version.  We just hide this annotation grouping
		//     and show the annotations under the genome version node instead.
		// 3. Hash the annotation groupings under parent annotation grouping
		//    and the annotations under the parent annotation grouping.
		
		// First hash the annotation groupings
		for (Object[] row : annotationGroupingRows) {
			Organism organism                      = (Organism) row[0];
			GenomeVersion genomeVersion            = (GenomeVersion)  row[1];
			AnnotationGrouping annotGrouping       = (AnnotationGrouping) row[2];
			AnnotationGrouping parentAnnotGrouping = (AnnotationGrouping) row[3];
			
			// Hash genome versions for an organism
			TreeMap<GenomeVersion, ?> versionMap = organismToVersion.get(organism.getBinomialName());
			if (versionMap == null) {
				versionMap = new TreeMap<GenomeVersion, Object>(new GenomeVersionComparator());
				organismToVersion.put(organism.getBinomialName(), versionMap);
				organismMap.put(organism.getBinomialName(), organism);
			}
			if (genomeVersion != null) {
				versionMap.put(genomeVersion, null);
				genomeVersionNameMap.put(genomeVersion.getName(), genomeVersion);
			}

			
			// Hash root annotation groupings for a genome version
			String groupingKey       = annotGrouping.getName()  + KEY_DELIM + annotGrouping.getIdAnnotationGrouping();
			if (parentAnnotGrouping == null) {

				TreeMap<String, ?> groupingNameMap = versionToRootGroupings.get(genomeVersion.getName());
				if (groupingNameMap == null) {
					groupingNameMap = new TreeMap<String, String>();
					versionToRootGroupings.put(genomeVersion.getName(), groupingNameMap);
				}
				groupingNameMap.put(groupingKey, null);				
			} else {
				String parentGroupingKey = parentAnnotGrouping.getName() + KEY_DELIM + parentAnnotGrouping.getIdAnnotationGrouping();

				// Hash annotation grouping for a parent annotation grouping
				TreeMap<String, ?> childGroupingNameMap = groupingToGroupings.get(parentGroupingKey);
				if (childGroupingNameMap == null) {
					childGroupingNameMap = new TreeMap<String, String>();
					groupingToGroupings.put(parentGroupingKey, childGroupingNameMap);
				}
				childGroupingNameMap.put(groupingKey, null);				
			}
			annotationGroupingMap.put(annotGrouping.getIdAnnotationGrouping(), annotGrouping);				
		}
		
		// Now hash the annotation rows
		for (Object[] row : annotationRows) {
			Organism organism                      = (Organism) row[0];
			GenomeVersion genomeVersion            = (GenomeVersion)  row[1];
			AnnotationGrouping annotGrouping       = (AnnotationGrouping) row[2];
			AnnotationGrouping parentAnnotGrouping = (AnnotationGrouping) row[3];
			Annotation annot                       = (Annotation) row[4];
			
			// Load properties for annotations
			if (annot != null) {
				annot.loadProps(dictionaryHelper);				
			}
			
			// Hash genome versions for an organism
			TreeMap<GenomeVersion, ?> versionMap = organismToVersion.get(organism.getBinomialName());
			if (versionMap == null) {
				versionMap = new TreeMap<GenomeVersion, Object>(new GenomeVersionComparator());
				organismToVersion.put(organism.getBinomialName(), versionMap);
			}
			if (genomeVersion != null) {
				versionMap.put(genomeVersion, null);
			}
			
			if (annotGrouping != null) {
				String groupingKey       = annotGrouping.getName()  + KEY_DELIM + annotGrouping.getIdAnnotationGrouping();
				// Hash root annotation groupings for a genome version
				if (parentAnnotGrouping == null) {

					TreeMap<String, ?> groupingNameMap = versionToRootGroupings.get(genomeVersion.getName());
					if (groupingNameMap == null) {
						groupingNameMap = new TreeMap<String, String>();
						versionToRootGroupings.put(genomeVersion.getName(), groupingNameMap);
					}
					groupingNameMap.put(groupingKey, null);				
				} else {
					String parentGroupingKey = parentAnnotGrouping.getName() + KEY_DELIM + parentAnnotGrouping.getIdAnnotationGrouping();
					
					// Hash annotation grouping for a parent annotation grouping
					TreeMap<String, ?> childGroupingNameMap = groupingToGroupings.get(parentGroupingKey);
					if (childGroupingNameMap == null) {
						childGroupingNameMap = new TreeMap<String, String>();
						groupingToGroupings.put(parentGroupingKey, childGroupingNameMap);
					}
					childGroupingNameMap.put(groupingKey, null);				
				}
				annotationGroupingMap.put(annotGrouping.getIdAnnotationGrouping(), annotGrouping);				

				// Hash annotations for an annotation grouping
				if (annot != null) {
					TreeMap<String, ?> annotNameMap = groupingToAnnotations.get(groupingKey);
					if (annotNameMap == null) {
						annotNameMap = new TreeMap<String, String>();
						groupingToAnnotations.put(groupingKey, annotNameMap);
					}
					String annotKey       = annot.getName()  + KEY_DELIM + annot.getIdAnnotation();
					annotNameMap.put(annotKey, null);
					annotationMap.put(annot.getIdAnnotation(), annot);
				}			
			}
		}
		
	}
	
	public GenomeVersion getGenomeVersion(String genomeVersionName) {
		return genomeVersionNameMap.get(genomeVersionName);
	}
	
	public Map<String, GenomeVersion> getGenomeVersionNameMap() {
		return genomeVersionNameMap;
	}
	

	public Set<Organism> getOrganisms() {
		TreeSet<Organism> organisms = new TreeSet<Organism>(new OrganismComparator());
		for(String organismBinomialName: organismToVersion.keySet()) {
			Organism organism = organismMap.get(organismBinomialName);
			organisms.add(organism);
		}
		return organisms;
	}
	
	public Set<String> getVersionNames(Organism organism) {
		Set<String> versionNames = new TreeSet<String>();
		
		TreeMap<GenomeVersion, ?> versionMap = organismToVersion.get(organism.getBinomialName());
		if (versionMap != null) {
			for(GenomeVersion version : versionMap.keySet()) {
				versionNames.add(version.getName());
			}
		}
		
		return versionNames;
	}
	
	public List<Segment> getSegments(Organism organism, String genomeVersionName) {
		List<Segment> segments = null;
		TreeMap<GenomeVersion, ?> versionMap = organismToVersion.get(organism.getBinomialName());
			
		// For each version...
		if (versionMap != null) {
			for (GenomeVersion genomeVersion: versionMap.keySet()) {
				
				if (genomeVersion.getName().equals(genomeVersionName)) {
					segments = versionToSegments.get(genomeVersion.getName());
					break;
				}
			}
		}
		return segments;		
	}
	
	public List<QualifiedAnnotation> getQualifiedAnnotations(Organism organism, String genomeVersionName) {
		List<QualifiedAnnotation> qualifiedAnnotations = new ArrayList<QualifiedAnnotation>();
		
				
		TreeMap<GenomeVersion, ?> versionMap = organismToVersion.get(organism.getBinomialName());
			
		// For each version...
		if (versionMap != null) {
			for (GenomeVersion genomeVersion : versionMap.keySet()) {
				
				if (genomeVersion.getName().equals(genomeVersionName)) {
					// For each root annotation grouping, recurse annotation grouping
					// hierarchy to get leaf annotations.
					TreeMap<String, ?> rootGroupingNameMap = versionToRootGroupings.get(genomeVersion.getName());
					String qualifiedName = "";
					getQualifiedAnnotation(rootGroupingNameMap, qualifiedAnnotations, qualifiedName, false);
					
				}
				
			}
			
		}
		return qualifiedAnnotations;		
	}
	
	private void getQualifiedAnnotation(TreeMap<String, ?> theGroupings, List<QualifiedAnnotation> qualifiedAnnotations, String typePrefix, boolean showGroupingLevel) {
		if (theGroupings != null) {
			for (String groupingKey : theGroupings.keySet()) {
				String[] tokens     = groupingKey.split(KEY_DELIM);
				String groupingName          = tokens[0];
				
				// For each annotation....
				TreeMap<String, ?> annotNameMap = groupingToAnnotations.get(groupingKey);
				if (annotNameMap != null) {
					// For each annotation...
					for (String annotNameKey : annotNameMap.keySet()) { 
						String[] tokens1    = annotNameKey.split(KEY_DELIM);
						Integer idAnnotation = new Integer(tokens1[1]);
													
						Annotation annot = annotationMap.get(idAnnotation);
						
						
						String fullTypePrefix = concatenateTypePrefix(typePrefix, groupingName, showGroupingLevel);
						if (fullTypePrefix != null && fullTypePrefix.length() > 0) {
							fullTypePrefix += "/";
						}
						
						qualifiedAnnotations.add(new QualifiedAnnotation(annot, 
								fullTypePrefix + annot.getName(), 
								fullTypePrefix + annot.getName() ));
					}						
				}
											
				// Recurse for each annotation grouping (under a grouping)
				TreeMap<String, ?> childGroupings = groupingToGroupings.get(groupingKey);
				if (childGroupings != null) {
					getQualifiedAnnotation(childGroupings, qualifiedAnnotations, concatenateTypePrefix(typePrefix, groupingName, showGroupingLevel), true);					
				}
			}					
		}
	}
	
	private String concatenateTypePrefix(String typePrefix, String groupingName, boolean showGroupingLevel) {
		if (showGroupingLevel) {
			if (typePrefix == null || typePrefix.equals("")) {
				return groupingName;
			} else {
				return typePrefix + "/" + groupingName;
			}
		} else {
			return typePrefix != null ? typePrefix : "";
		}
	}


	
	private void fillGroupingNode(GenomeVersion genomeVersion, Element parentNode, TreeMap<String, ?> theGroupings, GenoPubSecurity genoPubSecurity, DictionaryHelper dictionaryHelper, boolean showGroupingLevel) throws Exception {
		if (theGroupings != null) {
			for (String groupingKey : theGroupings.keySet()) {
				String[] tokens     = groupingKey.split(KEY_DELIM);
				String groupingName          = tokens[0];
				Integer idAnnotationGrouping = new Integer(tokens[1]);
				
				Element groupingNode = null;
				AnnotationGrouping annotGrouping = null;
				if (showGroupingLevel) {
					annotGrouping = annotationGroupingMap.get(idAnnotationGrouping);
					
				    groupingNode = annotGrouping.getXML(genoPubSecurity, dictionaryHelper).getRootElement();
				    parentNode.add(groupingNode);
					

				} else {
					groupingNode = parentNode;					
				}
				
				
				// For each annotation
				TreeMap<String, ?> annotNameMap = groupingToAnnotations.get(groupingKey);
				if (annotNameMap != null && annotNameMap.size() > 0) {
					groupingNode.addAttribute("annotationCount", String.valueOf(annotNameMap.size()));
					// For each annotation...
					for (String annotNameKey : annotNameMap.keySet()) { 
						String[] tokens1    = annotNameKey.split(KEY_DELIM);
						Integer idAnnotation = Integer.valueOf(tokens1[1]);
						
						Annotation annot = annotationMap.get(idAnnotation);
						
						Element annotNode = annot.getXML(genoPubSecurity, dictionaryHelper, null).getRootElement();
						annotNode.addAttribute("idAnnotationGrouping", annotGrouping != null ? annotGrouping.getIdAnnotationGrouping().toString() : "");	
						
						groupingNode.add(annotNode);

					}						
				}
											
				// Recurse for each annotation grouping (under a grouping)
				TreeMap<String, ?> childGroupings = groupingToGroupings.get(groupingKey);
				fillGroupingNode(genomeVersion, groupingNode, childGroupings, genoPubSecurity, dictionaryHelper, true);
				
				// Prune out other group's folders that don't have
				// any content (no authorized annotations in its folder
				// or its descendent's folder).
				if (!groupingNode.hasContent()) {
					String folderIdUserGroup = groupingNode.attributeValue("idUserGroup");
					boolean prune = true;
					// Public folders
					if (groupingNode.getName().equals("AnnotationGrouping") &&
						(folderIdUserGroup == null || folderIdUserGroup.equals(""))) {
						// If we are not filtering by user group,
						// always show empty public empty folders
						if (this.idUserGroup == null) {
							prune = false;
						} else {
							// If we are filtering by user group, prune
							// empty public folders.
							prune = true;
						}
					}
					// User group folders
					else if (groupingNode.getName().equals("AnnotationGrouping") &&
							    folderIdUserGroup != null && 
							    !folderIdUserGroup.equals("") &&
							  	(genoPubSecurity.belongsToGroup(new Integer(folderIdUserGroup)))) {
						if (this.idUserGroup == null) {
							// If we are not filtering by user group, then always
							// show my group's empty folders
							prune = false;							
						} else {
							// If we are filtering by user group, prune any empty
							// folders not specifically for the group being filtered
							if (this.idUserGroup.equals(new Integer(folderIdUserGroup))) {
								prune = false;								
							} else {
								prune = true;
							}
							
						}
					}	else {
						// Prune empty folders for other groups
						prune = true;
					}
					if (prune) {
						parentNode.remove(groupingNode);					
					}
				}
			}					
		}

	}

   
	

    private boolean hasAnnotationCriteria() {
    	if (idUserGroup != null ||
    	    hasVisibilityCriteria()) {
    		return true;
    	} else {
    		return false;
    	}
    }
    
    
    private boolean hasVisibilityCriteria() {
    	if (this.isVisibilityMembers.equals("Y") && this.isVisibilityMembersAndCollabs.equals("Y") && this.isVisibilityPublic.equals("Y")) {
    		return false;
    	} else if (this.isVisibilityMembers.equals("N") && this.isVisibilityMembersAndCollabs.equals("N") && this.isVisibilityPublic.equals("N")) {
    		return false;
    	} else {
    		return true;
    	}
    }
  

	private void addCriteria(int joinLevel) {

		// Search by organism
		if (idOrganism != null) {
			this.AND();
			queryBuf.append(" org.idOrganism = ");
			queryBuf.append(idOrganism);
		}
		// Search by genome version
		if (idGenomeVersion != null) {
			this.AND();
			queryBuf.append(" ver.idGenomeVersion = ");
			queryBuf.append(idGenomeVersion);
		}
		// Search for annotations and annotation groups for a particular group
		if (idUserGroup != null) {
			this.AND();
			queryBuf.append("(");
			if (joinLevel == ANNOTATION_LEVEL) {
				queryBuf.append(" a.idUserGroup = " + this.idUserGroup);
			} else if (joinLevel == ANNOTATION_GROUPING_LEVEL) {
				queryBuf.append(" ag.idUserGroup = " + this.idUserGroup);
				queryBuf.append(" OR ");
				queryBuf.append(" ag.idUserGroup is NULL");
			}
			queryBuf.append(")");
		}
		// Filter by annotation's visibility
		if (joinLevel == ANNOTATION_LEVEL) {
			if (hasVisibilityCriteria()) {
				this.AND();
				int count = 0;
				queryBuf.append(" a.codeVisibility in (");
				if (this.isVisibilityMembers.equals("Y")) {
					queryBuf.append("'" + Visibility.MEMBERS + "'");
					count++;
				}
				if (this.isVisibilityMembersAndCollabs.equals("Y")) {
					if (count > 0) {
						queryBuf.append(", ");
					}
					queryBuf.append("'" + Visibility.MEMBERS_AND_COLLABORATORS + "'");
					count++;
				}
				if (this.isVisibilityPublic.equals("Y")) {
					if (count > 0) {
						queryBuf.append(", ");
					}
					queryBuf.append("'" + Visibility.PUBLIC + "'");
					count++;
				}
				queryBuf.append(")");
			}
			
		}
		
		


	}

  

  
	protected boolean AND() {
		if (addWhere) {
			queryBuf.append(" WHERE ");
			addWhere = false;
		} else {
			queryBuf.append(" AND ");
		}
		return addWhere;
	}

	public String getIsVisibilityPublic() {
    	return isVisibilityPublic;
    }

	public void setIsVisibilityPublic(String isVisibilityPublic) {
    	this.isVisibilityPublic = isVisibilityPublic;
    }

	public String getIsVisibilityMembers() {
    	return isVisibilityMembers;
    }

	public void setIsVisibilityMembers(String isVisibilityMembers) {
    	this.isVisibilityMembers = isVisibilityMembers;
    }

	public String getIsVisibilityMembersAndCollabs() {
    	return isVisibilityMembersAndCollabs;
    }

	public void setIsVisibilityMembersAndCollabs(
            String isVisibilityMembersAndCollabs) {
    	this.isVisibilityMembersAndCollabs = isVisibilityMembersAndCollabs;
    }

  
}
