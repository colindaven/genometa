package com.affymetrix.genometry.genopub;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.hibernate.Session;
import org.hibernate.Transaction;

import com.affymetrix.genometryImpl.parsers.useq.USeqArchive;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.oreilly.servlet.multipart.FilePart;
import com.oreilly.servlet.multipart.MultipartParser;
import com.oreilly.servlet.multipart.ParamPart;
import com.oreilly.servlet.multipart.Part;


public class GenoPubServlet extends HttpServlet {

	public static final String GENOPUB_WEBAPP_NAME  = "genopub";

	private static final String GENOPUB_HTML_WRAPPER = "GenoPub.html";
	private static final String REALM                = "Das2";

	private static final int ERROR_CODE_OTHER                     = 901;
	private static final int ERROR_CODE_UNSUPPORTED_FILE_TYPE     = 902;
	private static final int ERROR_CODE_INCORRECT_FILENAME        = 903;
	private static final int ERROR_CODE_INSUFFICIENT_PERMISSIONS  = 904;
	private static final int ERROR_CODE_FILE_TOO_BIG              = 905;
	
	private static final String SESSION_DOWNLOAD_KEYS              = "genopubDownloadKeys";

	public static final String SECURITY_REQUEST                   = "security";
	public static final String DICTIONARIES_REQUEST               = "dictionaries";
	public static final String ANNOTATIONS_REQUEST                = "annotations";
	public static final String ANNOTATION_REQUEST                 = "annotation";
	public static final String ORGANISM_ADD_REQUEST               = "organismAdd";
	public static final String ORGANISM_UPDATE_REQUEST            = "organismUpdate";
	public static final String ORGANISM_DELETE_REQUEST            = "organismDelete";
	public static final String GENOME_VERSION_REQUEST             = "genomeVersion";
	public static final String GENOME_VERSION_ADD_REQUEST         = "genomeVersionAdd";
	public static final String GENOME_VERSION_UPDATE_REQUEST      = "genomeVersionUpdate";
	public static final String GENOME_VERSION_DELETE_REQUEST      = "genomeVersionDelete";
	public static final String SEGMENT_IMPORT_REQUEST             = "segmentImport";
	public static final String SEQUENCE_FORM_UPLOAD_URL_REQUEST   = "sequenceUploadURL";
	public static final String SEQUENCE_UPLOAD_FILES_REQUEST      = "sequenceUploadFiles"; 
	public static final String ANNOTATION_GROUPING_ADD_REQUEST    = "annotationGroupingAdd";
	public static final String ANNOTATION_GROUPING_UPDATE_REQUEST = "annotationGroupingUpdate";
	public static final String ANNOTATION_GROUPING_MOVE_REQUEST   = "annotationGroupingMove";
	public static final String ANNOTATION_GROUPING_DELETE_REQUEST = "annotationGroupingDelete";
	public static final String ANNOTATION_ADD_REQUEST             = "annotationAdd";
	public static final String ANNOTATION_UPDATE_REQUEST          = "annotationUpdate";
	public static final String ANNOTATION_DUPLICATE_REQUEST       = "annotationDuplicate";
	public static final String ANNOTATION_DELETE_REQUEST          = "annotationDelete";
	public static final String ANNOTATION_UNLINK_REQUEST          = "annotationUnlink";
	public static final String ANNOTATION_MOVE_REQUEST            = "annotationMove";
	public static final String ANNOTATION_INFO_REQUEST            = "annotationInfo";
	public static final String ANNOTATION_FORM_UPLOAD_URL_REQUEST = "annotationUploadURL";
	public static final String ANNOTATION_UPLOAD_FILES_REQUEST    = "annotationUploadFiles"; 
	public static final String ANNOTATION_ESTIMATE_DOWNLOAD_SIZE_REQUEST  = "annotationEstimateDownloadSize"; 
	public static final String ANNOTATION_DOWNLOAD_FILES_REQUEST  = "annotationDownloadFiles"; 
	public static final String USERS_AND_GROUPS_REQUEST           = "usersAndGroups"; 
	public static final String USER_ADD_REQUEST                   = "userAdd";
	public static final String USER_PASSWORD_REQUEST              = "userPassword"; 
	public static final String USER_UPDATE_REQUEST                = "userUpdate"; 
	public static final String USER_DELETE_REQUEST                = "userDelete"; 
	public static final String GROUP_ADD_REQUEST                  = "groupAdd";
	public static final String GROUP_UPDATE_REQUEST               = "groupUpdate"; 
	public static final String GROUP_DELETE_REQUEST               = "groupDelete"; 
	public static final String DICTIONARY_ADD_REQUEST             = "dictionaryAdd";
	public static final String DICTIONARY_UPDATE_REQUEST          = "dictionaryUpdate"; 
	public static final String DICTIONARY_DELETE_REQUEST          = "dictionaryDelete"; 
	public static final String VERIFY_RELOAD_REQUEST              = "verifyReload";

	private GenoPubSecurity genoPubSecurity = null;

	private String genometry_genopub_dir;

	public void init() throws ServletException {
		if (getGenoPubDir() == false) {
			Logger.getLogger(this.getClass().getName()).severe("FAILED to init() GenoPubServlet, aborting!");
			throw new ServletException("FAILED " + this.getClass().getName() + ".init(), aborting!");
		}
		
	}

	protected void doPost(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		handleRequest(req, res);
	}

	protected void doGet(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {
		handleRequest(req, res);
	}

	private void handleRequest(HttpServletRequest req, HttpServletResponse res)
	throws ServletException, IOException {

		try {

			// Get the GenoPubSecurity		
			genoPubSecurity = GenoPubSecurity.class.cast(req.getSession().getAttribute(GenoPubSecurity.SESSION_KEY));
			if (genoPubSecurity == null) {
				Session sess = HibernateUtil.getSessionFactory().openSession();

				genoPubSecurity = new GenoPubSecurity(sess, 
						req.getUserPrincipal().getName(), 
						true,
						req.isUserInRole(GenoPubSecurity.ADMIN_ROLE),
						req.isUserInRole(GenoPubSecurity.GUEST_ROLE));
				req.getSession().setAttribute(GenoPubSecurity.SESSION_KEY, genoPubSecurity);
			}

			// Handle the request
			if (req.getPathInfo() == null) {
				this.handleFlexRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.SECURITY_REQUEST)) {
				this.handleSecurityRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.DICTIONARIES_REQUEST)) {
				this.handleDictionaryRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATIONS_REQUEST)) {
				this.handleAnnotationsRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_REQUEST)) {
				this.handleAnnotationRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ORGANISM_ADD_REQUEST)) {
				this.handleOrganismAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ORGANISM_UPDATE_REQUEST)) {
				this.handleOrganismUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ORGANISM_DELETE_REQUEST)) {
				this.handleOrganismDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GENOME_VERSION_REQUEST)) {
				this.handleGenomeVersionRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GENOME_VERSION_ADD_REQUEST)) {
				this.handleGenomeVersionAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GENOME_VERSION_UPDATE_REQUEST)) {
				this.handleGenomeVersionUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GENOME_VERSION_DELETE_REQUEST)) {
				this.handleGenomeVersionDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.SEGMENT_IMPORT_REQUEST)) {
				this.handleSegmentImportRequest(req, res);
			}  else if (req.getPathInfo().endsWith(this.SEQUENCE_FORM_UPLOAD_URL_REQUEST)) {
				this.handleSequenceFormUploadURLRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.SEQUENCE_UPLOAD_FILES_REQUEST)) {
				this.handleSequenceUploadRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_GROUPING_ADD_REQUEST)) {
				this.handleAnnotationGroupingAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_GROUPING_UPDATE_REQUEST)) {
				this.handleAnnotationGroupingUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_GROUPING_MOVE_REQUEST)) {
				this.handleAnnotationGroupingMoveRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_GROUPING_DELETE_REQUEST)) {
				this.handleAnnotationGroupingDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_ADD_REQUEST)) {
				this.handleAnnotationAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_UPDATE_REQUEST)) {
				this.handleAnnotationUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_DUPLICATE_REQUEST)) {
				this.handleAnnotationDuplicateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_DELETE_REQUEST)) {
				this.handleAnnotationDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_UNLINK_REQUEST)) {
				this.handleAnnotationUnlinkRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_MOVE_REQUEST)) {
				this.handleAnnotationMoveRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_INFO_REQUEST)) {
				this.handleAnnotationInfoRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_FORM_UPLOAD_URL_REQUEST)) {
				this.handleAnnotationFormUploadURLRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_UPLOAD_FILES_REQUEST)) {
				this.handleAnnotationUploadRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_DOWNLOAD_FILES_REQUEST)) {
				this.handleAnnotationDownloadRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.ANNOTATION_ESTIMATE_DOWNLOAD_SIZE_REQUEST)) {
				this.handleAnnotationEstimateDownloadSizeRequest(req, res);
			}  else if (req.getPathInfo().endsWith(this.USERS_AND_GROUPS_REQUEST)) {
				this.handleUsersAndGroupsRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.USER_ADD_REQUEST)) {
				this.handleUserAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.USER_UPDATE_REQUEST)) {
				this.handleUserUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.USER_PASSWORD_REQUEST)) {
				this.handleUserPasswordRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.USER_DELETE_REQUEST)) {
				this.handleUserDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GROUP_ADD_REQUEST)) {
				this.handleGroupAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GROUP_UPDATE_REQUEST)) {
				this.handleGroupUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.GROUP_DELETE_REQUEST)) {
				this.handleGroupDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.DICTIONARY_ADD_REQUEST)) {
				this.handleDictionaryAddRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.DICTIONARY_UPDATE_REQUEST)) {
				this.handleDictionaryUpdateRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.DICTIONARY_DELETE_REQUEST)) {
				this.handleDictionaryDeleteRequest(req, res);
			} else if (req.getPathInfo().endsWith(this.VERIFY_RELOAD_REQUEST)) {
				this.handleVerifyReloadRequest(req, res);
			} else {
				throw new Exception("Unknown GenoPub request " + req.getPathInfo());
			}

			res.setHeader("Cache-Control", "max-age=0, must-revalidate");

			return;

		} catch (Exception e) {
			e.printStackTrace();		
			this.reportError(res, e.toString());
		}
	}

	private void handleFlexRequest(HttpServletRequest request, HttpServletResponse res) throws IOException {
		Session sess = null;

		try {

			// If idAnnotation was provided, make sure the user has permission
			// to read this annotation.
			if (request.getParameter("idAnnotation") != null && !request.getParameter("idAnnotation").equals("")) {
				sess = HibernateUtil.getSessionFactory().openSession();
				Integer idAnnotation = new Integer(request.getParameter("idAnnotation"));
				Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));

				if (!genoPubSecurity.canRead(annotation)) {
					throw new InsufficientPermissionException("Insufficient permission to access this annotation");
				}
			}

			// Now stream the HTML wrapper to the response.  This HTML
			// invokes the GenoPub swf.
			res.setContentType("text/html");
			res.getOutputStream().println(getFlexHTMLWrapper(request));
			res.setHeader("Cache-Control", "max-age=0, must-revalidate");

		}  catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());

		} catch (Exception e) {
			e.printStackTrace();
			this.reportError(res, e.toString());

		} finally {

			if (sess != null) {
				sess.close();
			}
		} 


	}

	private void handleSecurityRequest(HttpServletRequest request, HttpServletResponse res) throws Exception{
		XMLWriter writer = new XMLWriter(res.getOutputStream(),
				OutputFormat.createCompactFormat());
		writer.write(genoPubSecurity.getXML());

	}

	private void handleDictionaryRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			Document doc = DictionaryHelper.reload(sess).getXML(genoPubSecurity);

			XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat.createCompactFormat());
			writer.write(doc);
		}  catch (Exception e) {
			e.printStackTrace();
			this.reportError(res, e.toString());

		} finally {

			if (sess != null) {
				sess.close();
			}
		}
	}


	private void handleAnnotationsRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Document doc = null;
		Session sess = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();

			AnnotationQuery annotationQuery = new AnnotationQuery(request);
			doc = annotationQuery.getAnnotationDocument(sess, genoPubSecurity);

			XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat.createCompactFormat());
			writer.write(doc);
		} catch (Exception e) {
			e.printStackTrace();
			this.reportError(res, e.toString());

		} finally {

			if (sess != null) {
				sess.close();
			}
		}


	}


	private void handleAnnotationRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();

			if (request.getParameter("idAnnotation") == null || request.getParameter("idAnnotation").equals("")) {
				throw new Exception("idAnnotation request to get Annotation");
			}
			Integer idAnnotation = new Integer(request.getParameter("idAnnotation"));

			Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));

			if (!genoPubSecurity.canRead(annotation)) {
				throw new InsufficientPermissionException("Insufficient permission to access this annotation");
			}

			Document doc = annotation.getXML(this.genoPubSecurity, DictionaryHelper.getInstance(sess), genometry_genopub_dir);

			XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat.createCompactFormat());
			writer.write(doc);

		}  catch (InsufficientPermissionException e) {

			this.reportError(res, e.getMessage());

		} catch (Exception e) {			

			e.printStackTrace();
			this.reportError(res, e.toString());

		} finally {

			if (sess != null) {
				sess.close();
			}
		} 



	}

	private void handleOrganismAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			// Only admins can add organisms
			if (!this.genoPubSecurity.isAdminRole()) {
				throw new InsufficientPermissionException("Insufficient permission to add organism.");
			}

			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new InvalidNameException("Please enter an organism DAS2 name.");
			}
			if (request.getParameter("binomialName") == null || request.getParameter("binomialName").equals("")) {
				throw new InvalidNameException("Please enter an organism binomial name.");
			}
			if (request.getParameter("commonName") == null || request.getParameter("commonName").equals("")) {
				throw new InvalidNameException("Please enter an organism common name.");
			}

			// Make sure that the DAS2 name has no spaces or special characters
			if (request.getParameter("name").indexOf(" ") >= 0) {
				throw new InvalidNameException("The organism DAS2 name cannot have spaces.");
			}
			Pattern pattern = Pattern.compile("\\W");
			Matcher matcher = pattern.matcher(request.getParameter("name"));
			if (matcher.find()) {
				throw new InvalidNameException("The organism DAS2 name cannot have special characters.");
			}


			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			Organism organism = new Organism();

			organism.setName(request.getParameter("name"));
			organism.setCommonName(request.getParameter("commonName"));
			organism.setBinomialName(request.getParameter("binomialName"));

			sess.save(organism);

			tx.commit();

			DictionaryHelper.reload(sess);

			this.reportSuccess(res, "idOrganism", organism.getIdOrganism());


		}  catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		}  catch (InvalidNameException e) {
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} catch (Exception e) {			
			e.printStackTrace();
			this.reportError(res, e.toString());			
			if (tx != null) {
				tx.rollback();
			}
		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}


	private void handleOrganismUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {

			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			Organism organism = Organism.class.cast(sess.load(Organism.class, Util.getIntegerParameter(request, "idOrganism")));

			// Check write permissions
			if (!this.genoPubSecurity.canWrite(organism)) {
				throw new InsufficientPermissionException("Insufficient permission to update organism.");
			}


			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new InvalidNameException("Please enter an organism DAS2 name.");
			}
			if (request.getParameter("binomialName") == null || request.getParameter("binomialName").equals("")) {
				throw new InvalidNameException("Please enter an organism binomial name.");
			}
			if (request.getParameter("commonName") == null || request.getParameter("commonName").equals("")) {
				throw new InvalidNameException("Please enter an organism common name.");
			}

			// Make sure that the DAS2 name has no spaces or special characters
			if (request.getParameter("name").indexOf(" ") >= 0) {
				throw new InvalidNameException("The organism DAS2 name cannot have spaces.");
			}
			Pattern pattern = Pattern.compile("\\W");
			Matcher matcher = pattern.matcher(request.getParameter("name"));
			if (matcher.find()) {
				throw new InvalidNameException("The organism DAS2 name cannot have special characters.");
			}


			organism.setName(request.getParameter("name"));
			organism.setCommonName(request.getParameter("commonName"));
			organism.setBinomialName(request.getParameter("binomialName"));
			organism.setNCBITaxID(request.getParameter("NCBITaxID"));

			sess.flush();

			tx.commit();

			DictionaryHelper.reload(sess);

			this.reportSuccess(res, "idOrganism", organism.getIdOrganism());


		}  catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		}  catch (InvalidNameException e) {
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.reportError(res, e.toString());
			if (tx != null) {
				tx.rollback();
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}


	private void handleOrganismDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();


			Integer idOrganism = Util.getIntegerParameter(request, "idOrganism");
			Organism organism = Organism.class.cast(sess.load(Organism.class, idOrganism));


			// Check write permissions
			if (!this.genoPubSecurity.canWrite(organism)) {
				throw new InsufficientPermissionException("Insufficient permission to update organism.");
			}


			sess.delete(organism);

			tx.commit();

			DictionaryHelper.reload(sess);

			this.reportSuccess(res);


		} catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());
			if (tx != null) {
				tx.rollback();
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}

	
	private void handleGenomeVersionRequest(HttpServletRequest request, HttpServletResponse res) {
		Session sess = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();

			if (request.getParameter("idGenomeVersion") == null || request.getParameter("idGenomeVersion").equals("")) {
				throw new Exception("idGenomeVersion request to get Genome Version");
			}


			Integer idGenomeVersion = new Integer(request.getParameter("idGenomeVersion"));

			GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));

			Document doc = gv.getXML(genoPubSecurity, this.genometry_genopub_dir);

			XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat.createCompactFormat());
			writer.write(doc);

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

		} finally {

			if (sess != null) {
				sess.close();

			}
		}
	}


	private void handleGenomeVersionAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			// Only admins can add genome versions
			if (!this.genoPubSecurity.isAdminRole()) {
				throw new InsufficientPermissionException("Insufficient permissions to add genome version.");
			}


			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new InvalidNameException("Please enter the genome version name.");
			}
			// Make sure that the DAS2 name has no spaces or special characters
			if (request.getParameter("name").indexOf(" ") >= 0) {
				throw new InvalidNameException("The genome version DAS2 name cannot have spaces.");
			}
			Pattern pattern = Pattern.compile("\\W");
			Matcher matcher = pattern.matcher(request.getParameter("name"));
			if (matcher.find()) {
				throw new InvalidNameException("The genome version DAS2 name cannot have special characters.");
			}

			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			GenomeVersion genomeVersion = new GenomeVersion();

			Integer idOrganism = Util.getIntegerParameter(request, "idOrganism");
			
			genomeVersion.setIdOrganism(idOrganism);
			genomeVersion.setName(request.getParameter("name"));
			genomeVersion.setBuildDate(Util.getDateParameter(request, "buildDate"));
			sess.save(genomeVersion);

			// Now add a root annotation grouping
			AnnotationGrouping annotationGrouping = new AnnotationGrouping();
			annotationGrouping.setName(genomeVersion.getName());
			annotationGrouping.setIdGenomeVersion(genomeVersion.getIdGenomeVersion());
			annotationGrouping.setIdParentAnnotationGrouping(null);
			sess.save(annotationGrouping);

			Set<AnnotationGrouping>  annotationGroupingsToKeep= new TreeSet<AnnotationGrouping>(new AnnotationGroupingComparator());
			annotationGroupingsToKeep.add(annotationGrouping);
			genomeVersion.setAnnotationGroupings(annotationGroupingsToKeep);


			tx.commit();

			DictionaryHelper.reload(sess);

			this.reportSuccess(res, "idGenomeVersion", genomeVersion.getIdGenomeVersion());


		} catch (InvalidNameException e) {
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());
			if (tx != null) {
				tx.rollback();
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}

	private void handleGenomeVersionUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			GenomeVersion genomeVersion = GenomeVersion.class.cast(sess.load(GenomeVersion.class, Util.getIntegerParameter(request, "idGenomeVersion")));

			// Make sure the user can write this genome version
			if (!this.genoPubSecurity.canWrite(genomeVersion)) {
				throw new InsufficientPermissionException("Insufficient permision to write genome version.");
			}



			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new InvalidNameException("Please enter the genome version name.");
			}
			// Make sure that the DAS2 name has no spaces or special characters
			if (request.getParameter("name").indexOf(" ") >= 0) {
				throw new InvalidNameException("The genome version DAS2 name cannot have spaces.");
			}
			Pattern pattern = Pattern.compile("\\W");
			Matcher matcher = pattern.matcher(request.getParameter("name"));
			if (matcher.find()) {
				throw new InvalidNameException("The genome version DAS2 name cannot have special characters.");
			}

			// If the genomeversion name has changed, change to root annotation grouping
			// name
			if (!request.getParameter("name").equals(genomeVersion.getName())) {
				AnnotationGrouping ag = genomeVersion.getRootAnnotationGrouping();
				ag.setName(request.getParameter("name"));
				ag.setDescription(request.getParameter("name"));
			}

			genomeVersion.setIdOrganism(Util.getIntegerParameter(request, "idOrganism"));
			genomeVersion.setName(request.getParameter("name"));
			genomeVersion.setBuildDate(Util.getDateParameter(request, "buildDate"));
			genomeVersion.setCoordURI(request.getParameter("coordURI"));
			genomeVersion.setCoordVersion(request.getParameter("coordVersion"));
			genomeVersion.setCoordSource(request.getParameter("coordSource"));
			genomeVersion.setCoordTestRange(request.getParameter("coordTestRange"));
			genomeVersion.setCoordAuthority(request.getParameter("coordAuthority"));


			// Delete segments		
			StringReader reader = new StringReader(request.getParameter("segmentsXML"));
			SAXReader sax = new SAXReader();
			Document segmentsDoc = sax.read(reader);
			for (Iterator<?> i = genomeVersion.getSegments().iterator(); i.hasNext();) {
				Segment segment = Segment.class.cast(i.next());
				boolean found = false;
				for(Iterator<?> i1 = segmentsDoc.getRootElement().elementIterator(); i1.hasNext();) {
					Element segmentNode = (Element)i1.next();
					String idSegment = segmentNode.attributeValue("idSegment");
					if (idSegment != null && !idSegment.equals("")) {
						if (segment.getIdSegment().equals(new Integer(idSegment))) {
							found = true;
							break;
						}
					}										
				}
				if (!found) {
					sess.delete(segment);
				}
			} 
			sess.flush();

			// Add segments
			for(Iterator<?> i = segmentsDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element segmentNode = (Element)i.next();

				String idSegment = segmentNode.attributeValue("idSegment");
				String len = segmentNode.attributeValue("length");
				len = len.replace(",", "");
				String sortOrder = segmentNode.attributeValue("sortOrder");

				Segment s = null;
				if (idSegment != null && !idSegment.equals("")) {
					s = Segment.class.cast(sess.load(Segment.class, new Integer(idSegment)));

					s.setName(segmentNode.attributeValue("name"));
					s.setLength(len != null && !len.equals("") ? new Integer(len) : null);
					s.setSortOrder(sortOrder != null && !sortOrder.equals("") ? new Integer(sortOrder) : null);
					s.setIdGenomeVersion(genomeVersion.getIdGenomeVersion());


				} else {
					s = new Segment();		

					s.setName(segmentNode.attributeValue("name"));
					s.setLength(len != null && !len.equals("") ? new Integer(len) : null);
					s.setSortOrder(sortOrder != null && !sortOrder.equals("") ? new Integer(sortOrder) : null);
					s.setIdGenomeVersion(genomeVersion.getIdGenomeVersion());

					sess.save(s);
					sess.flush();
				}

			}    
			sess.flush();


			// Remove sequence files
			reader = new StringReader(request.getParameter("sequenceFilesToRemoveXML"));
			sax = new SAXReader();
			Document filesDoc = sax.read(reader);
			for(Iterator<?> i = filesDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element fileNode = (Element)i.next();
				File file = new File(fileNode.attributeValue("url"));
				if (!file.delete()) {
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Unable to delete sequence file " + file.getName() + " for genome version " + genomeVersion.getName());
				}
			}            



			tx.commit();

			DictionaryHelper.reload(sess);

			this.reportSuccess(res, "idGenomeVersion", genomeVersion.getIdGenomeVersion());



		} catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} catch (InvalidNameException e) {
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.reportError(res, e.toString());
			if (tx != null) {
				tx.rollback();
			}
		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}



	private void handleGenomeVersionDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			// Find the genome version
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			GenomeVersion genomeVersion = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));

			// Make sure the user can write this genome version
			if (!this.genoPubSecurity.canWrite(genomeVersion)) {
				throw new InsufficientPermissionException("Insufficient permision to delete genome version.");
			}

			// Delete the root annotation grouping
			AnnotationGrouping ag = genomeVersion.getRootAnnotationGrouping();
			if (ag != null) {
				// Make sure the root annotation grouping has no children
				if (ag.getAnnotationGroupings().size() > 0 || ag.getAnnotations().size() > 0) {
					throw new Exception("The annotations for" + genomeVersion.getName() + " must be deleted first.");
				}
				sess.delete(ag);
			}

			// Delete segments
			for (Iterator<?> i = genomeVersion.getSegments().iterator(); i.hasNext();) {
				Segment segment = Segment.class.cast(i.next());
				sess.delete(segment);
			}

			// Delete aliases
			for (Iterator<?> i = genomeVersion.getAliases().iterator(); i.hasNext();) {
				GenomeVersionAlias alias = GenomeVersionAlias.class.cast(i.next());
				sess.delete(alias);
			}

			sess.flush();

			// remove sequence files
			genomeVersion.removeSequenceFiles(genometry_genopub_dir);


			// Now delete the genome version
			sess.refresh(genomeVersion);
			sess.delete(genomeVersion);

			tx.commit();

			DictionaryHelper.reload(sess);

			this.reportSuccess(res);


		} catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}


	private void handleSegmentImportRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();


			String chromosomeInfo = request.getParameter("chromosomeInfo");
			String line;
			int count = 1;
			if (chromosomeInfo != null && !chromosomeInfo.equals("")) {
				Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
				GenomeVersion genomeVersion = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));

				// Make sure the user can write this genome version
				if (!this.genoPubSecurity.canWrite(genomeVersion)) {
					throw new InsufficientPermissionException("Insufficient permision to update the genome version.");
				}

				BufferedReader reader = new BufferedReader(new StringReader(chromosomeInfo));
				while ((line = reader.readLine()) != null) {	
					if ( (line.length() == 0) || line.equals("") || line.startsWith("#"))  { 
						continue; 
					}

					String name = null;
					String len = null;
					try {
						String[] tokens = line.split("\\s+", 2);
						name = tokens[0];
						len = tokens[1];					  
					} catch (Exception e) {
						String message = "Segment info did not import due to incorrect format.  Please enter the chromsome name, then whitespace (spaces or tabs), then chromosome length.";
						reportError(res, message);
						return;
					}

					Segment s = new Segment();		

					s.setName(name);
					s.setLength(len != null && !len.equals("") ? new Integer(len.replaceAll("[^0-9]", "")) : null);
					s.setSortOrder(Integer.valueOf(count));
					s.setIdGenomeVersion(genomeVersion.getIdGenomeVersion());

					sess.save(s);

					count++;
				}
				sess.flush();
			}


			tx.commit();

			DictionaryHelper.reload(sess);

			this.reportSuccess(res, "idGenomeVersion", new Integer(request.getParameter("idGenomeVersion")));


		}  catch (InsufficientPermissionException e) {			
			reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} catch (Exception e) {
			e.printStackTrace();
			reportError(res, "Segment info did not import. " + e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		} finally {
			if (sess != null) {
				sess.close();
			}
		}

	}

	private void handleSequenceFormUploadURLRequest(HttpServletRequest req, HttpServletResponse res) {
		try {

			//
			// COMMENTED OUT CODE: 
			//    String baseURL =  "http"+ (isLocalHost ? "://" : "s://") + req.getServerName() + req.getContextPath();
			//
			// To fix upload problem (missing session in upload servlet for FireFox, Safari), encode session in URL
			// for upload servlet.  Also, use non-secure (http: rather than https:) when making http request; 
			// otherwise, existing session is not accessible to upload servlet.
			//
			//

			String baseURL =  "http"+  "://"  + req.getServerName() + ":" + req.getLocalPort() + req.getContextPath();
			String URL = baseURL + "/" +  GENOPUB_WEBAPP_NAME + "/" +  this.SEQUENCE_UPLOAD_FILES_REQUEST;
			// Encode session id in URL so that session maintains for upload servlet when called from
			// Flex upload component inside FireFox, Safari
			URL += ";jsessionid=" + req.getRequestedSessionId();

			// Get the valid file extensions
			StringBuffer fileExtensions = new StringBuffer();
			for (int x=0; x < Constants.SEQUENCE_FILE_EXTENSIONS.length; x++) {
				if (fileExtensions.length() > 0) {
					fileExtensions.append(";");
				}
				fileExtensions.append("*" + Constants.SEQUENCE_FILE_EXTENSIONS[x]);
			}

			res.setContentType("application/xml");
			res.getOutputStream().println("<UploadURL url='" + URL + "'" + " fileExtensions='" + fileExtensions.toString() + "'" + "/>");

		} catch (Exception e) {
			System.out.println("An error has occured in GenoPubServlet - " + e.toString());
		}		
	}

	private void handleSequenceUploadRequest(HttpServletRequest req, HttpServletResponse res) {

		Session sess = null;
		Integer idGenomeVersion = null;

		GenomeVersion genomeVersion = null;

		String fileName = null;


		try {
			sess = HibernateUtil.getSessionFactory().openSession();


			res.setDateHeader("Expires", -1);
			res.setDateHeader("Last-Modified", System.currentTimeMillis());
			res.setHeader("Pragma", "");
			res.setHeader("Cache-Control", "");


			res.setCharacterEncoding("UTF-8");


			MultipartParser mp = new MultipartParser(req, Integer.MAX_VALUE); 
			Part part;
			while ((part = mp.readNextPart()) != null) {
				String name = part.getName();
				if (part.isParam()) {
					// it's a parameter part
					ParamPart paramPart = (ParamPart) part;
					String value = paramPart.getStringValue();
					if (name.equals("idGenomeVersion")) {
						idGenomeVersion = new Integer(String.class.cast(value));
					} 
				}

				if (idGenomeVersion != null) {
					break;
				}

			}


			if (idGenomeVersion != null) {
				genomeVersion = (GenomeVersion)sess.get(GenomeVersion.class, idGenomeVersion);
			} 
			if (genomeVersion != null) {
				if (this.genoPubSecurity.canWrite(genomeVersion)) {
					
					// Make sure that the data root dir exists
					if (!new File(genometry_genopub_dir).exists()) {
						boolean success = (new File(genometry_genopub_dir)).mkdir();
						if (!success) {
							throw new Exception("Unable to create directory " + genometry_genopub_dir);      
						}
					}

					String sequenceDir = genomeVersion.getSequenceDirectory(genometry_genopub_dir);

					// Create sequence directory if it doesn't exist
					if (!new File(sequenceDir).exists()) {
						boolean success = (new File(sequenceDir)).mkdir();
						if (!success) {
							throw new Exception("Unable to create directory " + sequenceDir);      
						}      
					}

					while ((part = mp.readNextPart()) != null) {        
						if (part.isFile()) {
							// it's a file part
							FilePart filePart = (FilePart) part;
							fileName = filePart.getFileName();
							if (fileName != null) {

								// Is the fileName valid?
								if (!Util.isValidSequenceFileType(fileName)) {
									throw new UnsupportedFileTypeException("Bypassing upload of sequence files for  " + genomeVersion.getName() + " for file" + fileName + ". Unsupported file extension");
								}

								// Write the file
								long size = filePart.writeTo(new File(sequenceDir));

							}
							else { 
							}
						}
					}
					sess.flush();

					this.reportSuccess(res, "idGenomeVersion", genomeVersion.getIdGenomeVersion());


				} else {
					throw new InsufficientPermissionException("Bypassing upload of sequence files for  " + genomeVersion.getName() + " due to insufficient permissions.");
				}
			} else {
				throw new Exception("No genome version provided for sequence files");
			}




		} catch (InsufficientPermissionException e) {
			Logger.getLogger(this.getClass().getName()).warning(e.getMessage());
			this.reportError(res, e.getMessage(),this.ERROR_CODE_UNSUPPORTED_FILE_TYPE);
		} catch (UnsupportedFileTypeException e) {
			Logger.getLogger(this.getClass().getName()).warning(e.getMessage());
			this.reportError(res, e.getMessage(), ERROR_CODE_UNSUPPORTED_FILE_TYPE);
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).warning(e.toString());
			e.printStackTrace();
			this.reportError(res, e.toString(), ERROR_CODE_OTHER);
		} finally {
			if (sess != null) {
				sess.close();
			}
		}


	}


	private void handleAnnotationGroupingAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {


			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter the annotation folder name.");
			}

			if (genoPubSecurity.isGuestRole()) {
				throw new InsufficientPermissionException("Insufficient permissions to add a folder.");
			}

			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			AnnotationGrouping annotationGrouping = new AnnotationGrouping();

			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			Integer idParentAnnotationGrouping = Util.getIntegerParameter(request, "idParentAnnotationGrouping");
			Integer idUserGroup = Util.getIntegerParameter(request, "idUserGroup");
			// If this is a root annotation grouping, find the default root annotation
			// grouping for the genome version.
			AnnotationGrouping parentAnnotationGrouping = null;
			if (idParentAnnotationGrouping == null) {
				GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));
				parentAnnotationGrouping = gv.getRootAnnotationGrouping();
				if (parentAnnotationGrouping == null) {
					throw new Exception("Cannot find root annotation grouping for " + gv.getName());
				}
				idParentAnnotationGrouping = parentAnnotationGrouping.getIdAnnotationGrouping(); 
			} else {
				parentAnnotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idParentAnnotationGrouping));
			}

			// If parent annotation grouping is owned by a user group, this
			// child annotation grouping must be as well.
			if (parentAnnotationGrouping.getIdUserGroup() != null) {

				if (idUserGroup == null ||
						!parentAnnotationGrouping.getIdUserGroup().equals(idUserGroup)) {
					throw new Exception("Folder '" + request.getParameter("name") + "' must belong to user group '" + 
							DictionaryHelper.getInstance(sess).getUserGroupName(parentAnnotationGrouping.getIdUserGroup()) + "'");

				}
			} 


			
			// Make sure that name doesn't have forward slashes (/).
			String name = request.getParameter("name");
			if (name.contains("/")) {
				throw new InvalidNameException("The folder name cannnot contain any / characters.");
			}

			annotationGrouping.setName(name);
			annotationGrouping.setIdGenomeVersion(idGenomeVersion);
			annotationGrouping.setIdParentAnnotationGrouping(idParentAnnotationGrouping);

			annotationGrouping.setIdUserGroup(Util.getIntegerParameter(request, "idUserGroup"));				

			annotationGrouping.setCreateDate(new java.sql.Date(System.currentTimeMillis()));
			annotationGrouping.setCreatedBy(this.genoPubSecurity.getUserName());


			sess.save(annotationGrouping);

			tx.commit();


			this.reportSuccess(res, "idAnnotationGrouping", annotationGrouping.getIdAnnotationGrouping());


		} catch (InsufficientPermissionException e) {			
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		}  catch (InvalidNameException e) {			
			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
		}  catch (Exception e) {			
			e.printStackTrace();
			this.reportError(res, e.toString());
			if (tx != null) {
				tx.rollback();
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}

	private void handleAnnotationGroupingUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter the annotation folder name.");
			}


			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			AnnotationGrouping annotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, Util.getIntegerParameter(request, "idAnnotationGrouping")));


			// Make sure the user can write this annotation grouping
			if (!this.genoPubSecurity.canWrite(annotationGrouping)) {
				throw new InsufficientPermissionException("Insufficient permision to write annotation folder.");
			}

			// If parent annotation grouping is owned by a user group, this
			// child annotation grouping must be as well.
			Integer idUserGroup = Util.getIntegerParameter(request, "idUserGroup");
			if (annotationGrouping.getParentAnnotationGrouping() != null &&
					annotationGrouping.getParentAnnotationGrouping().getIdUserGroup() != null) {

				if (idUserGroup == null ||
						!annotationGrouping.getParentAnnotationGrouping().getIdUserGroup().equals(idUserGroup)) {
					throw new Exception("Folder '" + request.getParameter("name") + "' must belong to user group '" + 
							DictionaryHelper.getInstance(sess).getUserGroupName(annotationGrouping.getParentAnnotationGrouping().getIdUserGroup()) + "'");
				}
			} 
			
			
			// Make sure that name doesn't have forward slashes (/).
			String name = request.getParameter("name");
			if (name.contains("/")) {
				throw new InvalidNameException("The folder name cannnot contain any / characters.");
			}

			annotationGrouping.setName(name);
			annotationGrouping.setDescription(request.getParameter("description"));
			annotationGrouping.setIdUserGroup(idUserGroup);

			sess.save(annotationGrouping);

			tx.commit();


			this.reportSuccess(res, "idAnnotationGrouping", annotationGrouping.getIdAnnotationGrouping());


		}  catch (InsufficientPermissionException e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		}   catch (InvalidNameException e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}


	private void handleAnnotationGroupingMoveRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			Integer idAnnotationGrouping = Util.getIntegerParameter(request, "idAnnotationGrouping");
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			Integer idParentAnnotationGrouping = Util.getIntegerParameter(request, "idParentAnnotationGrouping");
			String  isMove = Util.getFlagParameter(request, "isMove");

			AnnotationGrouping annotationGrouping = null;
			GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));

			// Get the annotation grouping this annotation grouping should be moved to.
			AnnotationGrouping parentAnnotationGrouping = null;
			if (idParentAnnotationGrouping == null) {
				// If this is a root annotation, find the default root annotation
				// grouping for the genome version.
				parentAnnotationGrouping = gv.getRootAnnotationGrouping();
				if (parentAnnotationGrouping == null) {
					throw new Exception("Cannot find root annotation grouping for " + gv.getName());
				}
			} else {
				// Otherwise, find the annotation grouping passed in as a request parameter.
				parentAnnotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idParentAnnotationGrouping));
			}





			// If this is a copy instead of a move,
			// clone the annotation grouping, leaving the existing one as-is.
			if (isMove.equals("Y")) {
				annotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));

				// Make sure the user can write this annotation grouping
				if (!this.genoPubSecurity.canWrite(annotationGrouping)) {
					throw new InsufficientPermissionException("Insufficient permision to move this annotation folder.");
				}
			} else {
				AnnotationGrouping ag = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
				annotationGrouping = new AnnotationGrouping();
				annotationGrouping.setName(ag.getName());
				annotationGrouping.setDescription(ag.getDescription());
				annotationGrouping.setIdGenomeVersion(ag.getIdGenomeVersion());
				annotationGrouping.setIdUserGroup(ag.getIdUserGroup());				

				Set<Annotation> annotationsToKeep = new TreeSet<Annotation>(new AnnotationComparator());
				for(Iterator<?> i = ag.getAnnotations().iterator(); i.hasNext();) {
					Annotation a = Annotation.class.cast(i.next());
					annotationsToKeep.add(a);
				}
				annotationGrouping.setAnnotations(annotationsToKeep);
				sess.save(annotationGrouping);
			}

			// The move/copy is disallowed if the parent annotation grouping belongs to a 
			// different genome version
			if (!parentAnnotationGrouping.getIdGenomeVersion().equals(annotationGrouping.getIdGenomeVersion())) {
				throw new Exception("Annotation folder '" + annotationGrouping.getName() + 
				"' cannot be moved to a different genome version");
			}

			// The move/copy is disallowed if the from and to annotation grouping are the
			// same
			if (parentAnnotationGrouping.getIdAnnotationGrouping().equals(idAnnotationGrouping)) {
				throw new Exception("Move/copy operation to same annotation folder is not allowed.");
			}

			// Set the parent annotation grouping
			annotationGrouping.setIdParentAnnotationGrouping(parentAnnotationGrouping.getIdAnnotationGrouping());


			tx.commit();


			this.reportSuccess(res, "idAnnotationGrouping", annotationGrouping.getIdAnnotationGrouping());


		} catch (InsufficientPermissionException e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}


	private void handleAnnotationGroupingDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			Integer idAnnotationGrouping = Util.getIntegerParameter(request, "idAnnotationGrouping");
			AnnotationGrouping annotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));

			List<Object> descendents = new ArrayList<Object>();
			descendents.add(annotationGrouping);
			annotationGrouping.recurseGetChildren(descendents);


			// Make sure the user can write this annotation grouping and all of its
			// descendent annotations and annotation groupings
			for(Iterator<?> i = descendents.iterator(); i.hasNext();) {
				Object descendent = i.next();
				if (!this.genoPubSecurity.canWrite(descendent)) {
					if (descendent.equals(annotationGrouping)) {
						throw new InsufficientPermissionException("Insufficient permision to delete this annotation folder.");	            
					} else if (descendent instanceof AnnotationGrouping){
						AnnotationGrouping ag = (AnnotationGrouping)descendent;
						throw new InsufficientPermissionException("Insufficent permission to delete child folder '" + ag.getName() + "'.");
					} else if (descendent instanceof Annotation){
						Annotation a = (Annotation)descendent;
						throw new InsufficientPermissionException("Insufficent permission to delete child annotation '" + a.getName() + "'.");
					}
				}
			}

			// Make sure we are not trying to delete an annotation that also exists in
			// another folder (that will not be deleted.)
			for(Iterator<?> i = descendents.iterator(); i.hasNext();) {
				Object descendent = i.next();
				if (descendent instanceof Annotation) {
					Annotation a = (Annotation)descendent;
					if (a.getAnnotationGroupings().size() > 1) {
						for(Iterator<?> i1 = a.getAnnotationGroupings().iterator(); i1.hasNext();) {
							AnnotationGrouping ag = (AnnotationGrouping)i1.next();
							boolean inDeleteList = false;
							for(Iterator<?> i2 = descendents.iterator(); i2.hasNext();) {
								Object d = i2.next();
								if (d instanceof AnnotationGrouping) {
									AnnotationGrouping agToDelete = (AnnotationGrouping)d;
									if (agToDelete.getIdAnnotationGrouping().equals(ag.getIdAnnotationGrouping())) {
										inDeleteList = true;
										break;
									}
								}
							}
							if (!inDeleteList) {
								throw new InsufficientPermissionException("Cannot remove contents of folder '" + annotationGrouping.getName() + 
										"' because annotation '" + a.getName() + 
										"' exists in folder '" + 
										ag.getName() + 
								"'.  Please remove this annotation first.");
							}
						}
					}
				}
			}

			// Now delete all of the contents of the annotation grouping and then the
			// annotation grouping itself.  By traversing the list from the
			// in reverse, we are sure to delete the children before the parent
			// folder.
			for(int i = descendents.size() - 1; i >= 0; i--) {
				Object descendent = descendents.get(i);

				// Remove annotation file(s)
				if (descendent instanceof Annotation) {
					Annotation a = (Annotation)descendent;	          
					a.removeFiles(genometry_genopub_dir);              
				} 

				// Delete the object from db
				sess.delete(descendent);          
			}

			tx.commit();

			this.reportSuccess(res);

		} catch (InsufficientPermissionException e) {

			this.reportError(res, e.getMessage());
			if (tx != null) {
				tx.rollback();
			}

		} catch (Exception e) {

			this.reportError(res, e.toString());
			if (tx != null) {
				tx.rollback();
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}



	private void handleAnnotationAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;


		try {
			if (genoPubSecurity.isGuestRole()) {
				throw new InsufficientPermissionException("Insufficient permissions to add an annotation.");
			}

			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter an annotation name.");
			}
			if (request.getParameter("codeVisibility") == null || request.getParameter("codeVisibility").equals("")) {
				throw new Exception("Please select the visibility for this annotation.");
			}
			if (!request.getParameter("codeVisibility").equals(Visibility.PUBLIC)) {
				if (Util.getIntegerParameter(request, "idUserGroup") == null) {
					throw new Exception("For private annotations, the group must be specified.");
				}
			}

			String name = request.getParameter("name");
			
			// Make sure that name doesn't have forward slashes (/).
			if (name.contains("/")) {
				throw new InvalidNameException("The annotation name cannnot contain any / characters.");
			}
			
			
			String codeVisibility = request.getParameter("codeVisibility");
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			Integer idAnnotationGrouping = Util.getIntegerParameter(request, "idAnnotationGrouping");
			Integer idUserGroup = Util.getIntegerParameter(request, "idUserGroup");			

			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();


			// Create a new annotation
			Annotation annotation = createNewAnnotation(sess, name, codeVisibility, idGenomeVersion, idAnnotationGrouping, idUserGroup);


			sess.flush();



			tx.commit();


			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotation", annotation.getIdAnnotation().toString());
			root.addAttribute("idGenomeVersion", idGenomeVersion.toString());
			root.addAttribute("idAnnotationGrouping", idAnnotationGrouping != null ? idAnnotationGrouping.toString() : "");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
					OutputFormat.createCompactFormat());
			writer.write(doc);


		}  catch (InsufficientPermissionException e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		}  catch (InvalidNameException e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}


	}


	private Annotation createNewAnnotation(Session sess, String name, String codeVisibility, Integer idGenomeVersion, Integer idAnnotationGrouping,  Integer idUserGroup) throws Exception {
		Annotation annotation = new Annotation();


		annotation.setName(name);
		annotation.setIdGenomeVersion(idGenomeVersion);
		annotation.setCodeVisibility(codeVisibility);
		annotation.setIdUserGroup(idUserGroup);
		annotation.setIsLoaded("N");

		// Only set ownership if this is not an admin
		if (!genoPubSecurity.isAdminRole()) {
			annotation.setIdUser(genoPubSecurity.getIdUser());				
		}

		annotation.setCreateDate(new java.sql.Date(System.currentTimeMillis()));
		annotation.setCreatedBy(this.genoPubSecurity.getUserName());

		sess.save(annotation);
		sess.flush();

		// Get the annotation grouping this annotation is in.
		AnnotationGrouping ag = null;
		if (idAnnotationGrouping == null) {
			// If this is a root annotation, find the default root annotation
			// grouping for the genome version.
			GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));
			ag = gv.getRootAnnotationGrouping();
			if (ag == null) {
				throw new Exception("Cannot find root annotation grouping for " + gv.getName());
			}
		} else {
			// Otherwise, find the annotation grouping passed in as a request parameter.
			ag = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
		}

		// Add the annotation to the annotation grouping
		Set<Annotation> newAnnotations = new TreeSet<Annotation>(new AnnotationComparator());
		for(Iterator<?>i = ag.getAnnotations().iterator(); i.hasNext();) {
			Annotation a = Annotation.class.cast(i.next());
			newAnnotations.add(a);
		}
		newAnnotations.add(annotation);
		ag.setAnnotations(newAnnotations);


		// Assign a file directory name
		annotation.setFileName("A" + annotation.getIdAnnotation());

		return annotation;

	}


	private void handleAnnotationUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, Util.getIntegerParameter(request, "idAnnotation")));

			// Make sure the user can write this annotation 
			if (!this.genoPubSecurity.canWrite(annotation)) {
				throw new InsufficientPermissionException("Insufficient permision to write annotation.");
			}

			// Make sure that the required fields are filled in
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter an annotation name.");
			}
			if (request.getParameter("codeVisibility") == null || request.getParameter("codeVisibility").equals("")) {
				throw new Exception("Please select the visibility for this annotation.");
			}
			if (!request.getParameter("codeVisibility").equals(Visibility.PUBLIC)) {
				if (Util.getIntegerParameter(request, "idUserGroup") == null) {
					throw new Exception("For private annotations, the group must be specified.");
				}
			}
			
			
			// Make sure that name doesn't have forward slashes (/).
			String name = request.getParameter("name");
			if (name.contains("/")) {
				throw new InvalidNameException("The annotation name cannnot contain any / characters.");
			}

			annotation.setName(name);
			annotation.setDescription(request.getParameter("description"));
			annotation.setSummary(request.getParameter("summary"));
			annotation.setIdAnalysisType(Util.getIntegerParameter(request, "idAnalysisType"));
			annotation.setIdExperimentPlatform(Util.getIntegerParameter(request, "idExperimentPlatform"));
			annotation.setIdExperimentMethod(Util.getIntegerParameter(request, "idExperimentMethod"));
			annotation.setCodeVisibility(request.getParameter("codeVisibility"));
			annotation.setIdUserGroup(Util.getIntegerParameter(request, "idUserGroup"));
			annotation.setIdUser(Util.getIntegerParameter(request, "idUser"));

			// Remove annotation files
			StringReader reader = new StringReader(request.getParameter("filesToRemoveXML"));
			SAXReader sax = new SAXReader();
			Document filesDoc = sax.read(reader);
			for(Iterator<?> i = filesDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element fileNode = (Element)i.next();
				File file = new File(fileNode.attributeValue("url"));
				if (!file.delete()) {
					Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Unable remove annotation file " + file.getName() + " for annotation " + annotation.getName());
				}
			}            

			sess.save(annotation);

			tx.commit();


			this.reportSuccess(res, "idAnnotation", annotation.getIdAnnotation());


		} catch (InsufficientPermissionException e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (InvalidNameException e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}


	private void handleAnnotationDuplicateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;
		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			// Make sure that the required fields are filled in
			if (request.getParameter("idAnnotation") == null || request.getParameter("idAnnotation").equals("")) {
				throw new Exception("idAnnotation required.");
			}


			Annotation sourceAnnot = Annotation.class.cast(sess.load(Annotation.class, Util.getIntegerParameter(request, "idAnnotation")));

			// Make sure the user can write this annotation 
			if (!this.genoPubSecurity.canWrite(sourceAnnot)) {
				throw new InsufficientPermissionException("Insufficient permision to write annotation.");
			}

			Annotation dup = new Annotation();

			dup.setName(sourceAnnot.getName() + "_copy");
			dup.setDescription(sourceAnnot.getDescription());
			dup.setSummary(sourceAnnot.getSummary());
			dup.setIdAnalysisType(sourceAnnot.getIdAnalysisType());
			dup.setIdExperimentPlatform(sourceAnnot.getIdExperimentPlatform());
			dup.setIdExperimentMethod(sourceAnnot.getIdExperimentMethod());
			dup.setCodeVisibility(sourceAnnot.getCodeVisibility());
			dup.setIdUserGroup(sourceAnnot.getIdUserGroup());
			dup.setIdUser(sourceAnnot.getIdUser());
			dup.setIdGenomeVersion(sourceAnnot.getIdGenomeVersion());
			dup.setIsLoaded("N");
			dup.setCreateDate(new java.sql.Date(System.currentTimeMillis()));
			dup.setCreatedBy(this.genoPubSecurity.getUserName());


			sess.save(dup);


			// Get the annotation grouping this annotation is in.
			AnnotationGrouping ag = null;
			if (Util.getIntegerParameter(request, "idAnnotationGrouping") == null) {
				// If this is a root annotation, find the default root annotation
				// grouping for the genome version.
				GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, sourceAnnot.getIdGenomeVersion()));
				ag = gv.getRootAnnotationGrouping();
				if (ag == null) {
					throw new Exception("Cannot find root annotation grouping for " + gv.getName());
				}
			} else {				
				// Otherwise, find the annotation grouping passed in as a request parameter.
				ag = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, Util.getIntegerParameter(request, "idAnnotationGrouping")));
			}

			// Add the annotation to the annotation grouping
			Set<Annotation> newAnnotations = new TreeSet<Annotation>(new AnnotationComparator());
			for(Iterator<?> i = ag.getAnnotations().iterator(); i.hasNext();) {	
				Annotation a = Annotation.class.cast(i.next());
				newAnnotations.add(a);
			}
			newAnnotations.add(dup);
			ag.setAnnotations(newAnnotations);


			// Assign a file directory name
			dup.setFileName("A" + dup.getIdAnnotation());

			tx.commit();


			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotation", dup.getIdAnnotation().toString());
			if ( Util.getIntegerParameter(request, "idAnnotationGrouping") != null) {
				root.addAttribute("idAnnotationGrouping", Util.getIntegerParameter(request, "idAnnotationGrouping").toString());				
			} else {
				root.addAttribute("idAnnotationGrouping", "");
			}

			XMLWriter writer = new XMLWriter(res.getOutputStream(),
					OutputFormat.createCompactFormat());
			writer.write(doc);


		} catch (InsufficientPermissionException e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}


	@SuppressWarnings("unchecked")
    private void handleAnnotationDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			Integer idAnnotation = Util.getIntegerParameter(request, "idAnnotation");
			Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));

			// Make sure the user can write this annotation 
			if (!this.genoPubSecurity.canWrite(annotation)) {
				throw new InsufficientPermissionException("Insufficient permision to delete annotation.");
			}

			// insert annotation reload entry which will cause
			// das/2 type to be unloaded on next 'das2 reload' request
			// Note:  If annotation is under more than one folder, there
			// can be multiple das/2 types for one annotation.
			for(AnnotationGrouping ag : (Set<AnnotationGrouping>)annotation.getAnnotationGroupings()) {
				String path = ag.getQualifiedTypeName();
				if (path.length() > 0) {
					path += "/";
				}
				String typeName = path + annotation.getName();
		
				UnloadAnnotation unload = new UnloadAnnotation();
				unload.setTypeName(typeName);
				unload.setIdUser(this.genoPubSecurity.getIdUser());
				unload.setIdGenomeVersion(annotation.getIdGenomeVersion());
				
				sess.save(unload);
			}
		

			// remove annotation files
			annotation.removeFiles(genometry_genopub_dir);

			// delete database object
			sess.delete(annotation);
			
			sess.flush();
			

			tx.commit();


			this.reportSuccess(res);

		}  catch (InsufficientPermissionException e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}


	private void handleAnnotationUnlinkRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			Integer idAnnotation = Util.getIntegerParameter(request, "idAnnotation");
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			Integer idAnnotationGrouping = Util.getIntegerParameter(request, "idAnnotationGrouping");

			Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));
			GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));

			// Make sure the user can write this annotation 
			if (!this.genoPubSecurity.canWrite(annotation)) {
				throw new InsufficientPermissionException("Insufficient permision to unlink annotation.");
			}


			// Get the annotation grouping this annotation should be removed from.
			AnnotationGrouping annotationGrouping = null;
			if (idAnnotationGrouping == null) {
				// If this is a root annotation, find the default root annotation
				// grouping for the genome version.
				annotationGrouping = gv.getRootAnnotationGrouping();
				if (annotationGrouping == null) {
					throw new Exception("Cannot find root annotation grouping for " + gv.getName());
				}
			} else {
				// Otherwise, find the annotation grouping passed in as a request parameter.
				annotationGrouping = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
			}
			
			// Create a pending unload of the annotation
			String typeName = annotationGrouping.getQualifiedTypeName() + "/" + annotation.getName();
			UnloadAnnotation unload = new UnloadAnnotation();
			unload.setTypeName(typeName);
			unload.setIdUser(this.genoPubSecurity.getIdUser());
			sess.save(unload);
			

			// Remove the annotation grouping the annotation was in
			// by adding back the annotations to the annotation grouping, 
			// excluding the annotation to be removed
			Set<Annotation> annotationsToKeep = new TreeSet<Annotation>(new AnnotationComparator());
			for(Iterator<?>i = annotationGrouping.getAnnotations().iterator(); i.hasNext();) {
				Annotation a = Annotation.class.cast(i.next());
				if (a.getIdAnnotation().equals(annotation.getIdAnnotation())) {
					continue;
				}
				annotationsToKeep.add(a);

			}
			annotationGrouping.setAnnotations(annotationsToKeep);



			tx.commit();

			// Send back XML attributes showing remaining references to annotation groupings
			sess.refresh(annotation);
			StringBuffer remainingAnnotationGroupings = new StringBuffer();
			int agCount = 0;
			for (Iterator<?> i1 = annotation.getAnnotationGroupings().iterator(); i1.hasNext();) {
				AnnotationGrouping ag = AnnotationGrouping.class.cast(i1.next());
				if (remainingAnnotationGroupings.length() > 0) {
					remainingAnnotationGroupings.append(",\n");					
				}
				remainingAnnotationGroupings.append("    '" + ag.getName() + "'");
				agCount++;

			}


			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotation", annotation.getIdAnnotation().toString());
			root.addAttribute("name", annotation.getName());
			root.addAttribute("numberRemainingAnnotationGroupings", Integer.valueOf(agCount).toString());
			root.addAttribute("remainingAnnotationGroupings", remainingAnnotationGroupings.toString());
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
					OutputFormat.createCompactFormat());
			writer.write(doc);


		} catch (InsufficientPermissionException e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}

	private void handleAnnotationMoveRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			Integer idAnnotation = Util.getIntegerParameter(request, "idAnnotation");
			Integer idGenomeVersion = Util.getIntegerParameter(request, "idGenomeVersion");
			Integer idAnnotationGrouping = Util.getIntegerParameter(request, "idAnnotationGrouping");
			Integer idAnnotationGroupingOld = Util.getIntegerParameter(request, "idAnnotationGroupingOld");
			String  isMove = Util.getFlagParameter(request, "isMove");

			Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));
			GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, idGenomeVersion));

			// Make sure the user can write this annotation 
			if (isMove.equals("Y")) {
				if (!this.genoPubSecurity.canWrite(annotation)) {
					throw new InsufficientPermissionException("Insufficient permision to unlink annotation.");
				}
			}

			// Get the annotation grouping this annotation should be moved to.
			AnnotationGrouping annotationGroupingNew = null;
			if (idAnnotationGrouping == null) {
				// If this is a root annotation, find the default root annotation
				// grouping for the genome version.
				annotationGroupingNew = gv.getRootAnnotationGrouping();
				if (annotationGroupingNew == null) {
					throw new Exception("Cannot find root annotation grouping for " + gv.getName());
				}
			} else {
				// Otherwise, find the annotation grouping passed in as a request parameter.
				annotationGroupingNew = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
			}



			// The move/copy is disallowed if the parent annotation grouping belongs to a 
			// different genome version
			if (!annotationGroupingNew.getIdGenomeVersion().equals(annotation.getIdGenomeVersion())) {
				throw new Exception("Annotation '" + annotation.getName() + 
				"' cannot be moved to a different genome version");
			}

			// The move/copy is disallowed if the from and to annotation grouping are the
			// same
			if (idAnnotationGroupingOld != null) {
				if (annotationGroupingNew.getIdAnnotationGrouping().equals(idAnnotationGroupingOld)) {
					throw new Exception("Move/copy operation to same annotation folder is not allowed.");
				}				
			} else {
				if (idAnnotationGrouping == null) {
					throw new Exception("Move/copy operation to same folder is not allowed.");
				}
			}


			//
			// Add the annotation to the annotation grouping
			//
			Set<Annotation> newAnnotations = new TreeSet<Annotation>(new AnnotationComparator());
			for(Iterator<?> i = annotationGroupingNew.getAnnotations().iterator(); i.hasNext();) {
				Annotation a = Annotation.class.cast(i.next());
				newAnnotations.add(a);
			}
			newAnnotations.add(annotation);
			annotationGroupingNew.setAnnotations(newAnnotations);



			// If this is a move instead of a copy,
			// get the annotation grouping this annotation should be removed from.
			if (isMove.equals("Y")) {
				AnnotationGrouping annotationGroupingOld = null;
				if (idAnnotationGroupingOld == null) {
					// If this is a root annotation, find the default root annotation
					// grouping for the genome version.
					annotationGroupingOld = gv.getRootAnnotationGrouping();
					if (annotationGroupingOld == null) {
						throw new Exception("Cannot find root annotation grouping for " + gv.getName());
					}
				} else {
					// Otherwise, find the annotation grouping passed in as a request parameter.
					annotationGroupingOld = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGroupingOld));
				}

				//
				// Remove the annotation grouping the annotation was in
				// by adding back the annotations to the annotation grouping, 
				// excluding the annotation that has moved
				Set<Annotation> annotationsToKeep = new TreeSet<Annotation>(new AnnotationComparator());
				for(Iterator<?> i1 = annotationGroupingOld.getAnnotations().iterator(); i1.hasNext();) {
					Annotation a = Annotation.class.cast(i1.next());
					if (a.getIdAnnotation().equals(annotation.getIdAnnotation())) {
						continue;
					}
					annotationsToKeep.add(a);
				}
				annotationGroupingOld.setAnnotations(annotationsToKeep);

			}



			tx.commit();


			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("idAnnotation", annotation.getIdAnnotation().toString());
			root.addAttribute("idGenomeVersion", idGenomeVersion.toString());
			root.addAttribute("idAnnotationGrouping", idAnnotationGrouping != null ? idAnnotationGrouping.toString() : "");
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
					OutputFormat.createCompactFormat());
			writer.write(doc);


		}  catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}

	private void handleAnnotationInfoRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		org.dom4j.io.OutputFormat format = org.dom4j.io.OutputFormat.createPrettyPrint();
		org.dom4j.io.HTMLWriter writer = null;


		try {
			sess = HibernateUtil.getSessionFactory().openSession();

			Integer idAnnotation = Util.getIntegerParameter(request, "idAnnotation");

			DictionaryHelper dh = DictionaryHelper.getInstance(sess);

			Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));

			if (!this.genoPubSecurity.canRead(annotation)) {
				throw new Exception("Insufficient permissions to access information on this annotation.");				
			}

			res.setContentType("text/html");
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("HTML");

			Element head = root.addElement("HEAD");
			Element link = head.addElement("link");
			link.addAttribute("rel", "stylesheet");
			link.addAttribute("type", "text/css");

			String baseURL = "";
			StringBuffer fullPath = request.getRequestURL();
			String extraPath = request.getServletPath() + request.getPathInfo();
			int pos = fullPath.lastIndexOf(extraPath);
			if (pos > 0) {
				baseURL = fullPath.substring(0, pos);
			}

			link.addAttribute("href", baseURL + "/info.css");

			Element body = root.addElement("BODY");


			Element center = body.addElement("CENTER");
			Element h1   = center.addElement("H1");
			h1.addText("DAS2 Annotation");

			Element h2   = body.addElement("H2");
			h2.addText(annotation.getName());

			Element table = body.addElement("TABLE");

			Element row   = table.addElement("TR");
			row.addElement("TD").addText("Summary").addAttribute("CLASS", "label");
			row.addElement("TD").addCDATA(annotation.getSummary() != null && !annotation.getSummary().equals("") ? annotation.getSummary() : "&nbsp;");

			row   = table.addElement("TR");			
			row.addElement("TD").addText("Description").addAttribute("CLASS", "label");
			if (annotation.getDescription() == null || annotation.getDescription().equals("")) {
				row.addElement("TD").addCDATA("&nbsp;");
			} else {
				String description = annotation.getDescription().replaceAll("\\n", "<br>");
				description = annotation.getDescription().replaceAll("\\r", "<br>");
				row.addElement("TD").addCDATA(description);				
			}

			row   = table.addElement("TR");			
			row.addElement("TD").addText("Experiment platform").addAttribute("CLASS", "label");
			row.addElement("TD").addCDATA(annotation.getIdExperimentPlatform() != null ? dh.getExperimentPlatform(annotation.getIdExperimentPlatform()) : "&nbsp;");

			row   = table.addElement("TR");			
			row.addElement("TD").addText("Experiment method").addAttribute("CLASS", "label");
			row.addElement("TD").addCDATA(annotation.getIdExperimentMethod() != null ? dh.getExperimentMethod(annotation.getIdExperimentMethod()) : "&nbsp;");

			row   = table.addElement("TR");			
			row.addElement("TD").addText("Analysis type").addAttribute("CLASS", "label");
			row.addElement("TD").addCDATA(annotation.getIdAnalysisType() != null ? dh.getAnalysisType(annotation.getIdAnalysisType()) : "&nbsp;");

			row   = table.addElement("TR");			
			row.addElement("TD").addText("Owner").addAttribute("CLASS", "label");
			row.addElement("TD").addCDATA(annotation.getIdUser() != null ? dh.getUserFullName(annotation.getIdUser()) : "&nbsp;");

			row   = table.addElement("TR");			
			row.addElement("TD").addText("Owner email").addAttribute("CLASS", "label");
			String userEmail = dh.getUserEmail(annotation.getIdUser());
			row.addElement("TD").addCDATA(userEmail != null ? userEmail : "&nbsp;");

			row   = table.addElement("TR");			
			row.addElement("TD").addText("Owner institute").addAttribute("CLASS", "label");
			String userInstitute = dh.getUserInstitute(annotation.getIdUser());
			row.addElement("TD").addCDATA(userInstitute != null ? userInstitute : "&nbsp;");

			row   = table.addElement("TR");			
			row.addElement("TD").addText("User Group").addAttribute("CLASS", "label");
			row.addElement("TD").addCDATA(annotation.getIdUserGroup() != null ? dh.getUserGroupName(annotation.getIdUserGroup()) : "&nbsp;");

			row   = table.addElement("TR");			 
			row.addElement("TD").addText("User Group contact").addAttribute("CLASS", "label");
			String groupContact = dh.getUserGroupContact(annotation.getIdUserGroup());
			row.addElement("TD").addCDATA(groupContact != null ? groupContact : "&nbsp;");

			row   = table.addElement("TR");			
			row.addElement("TD").addText("User Group email").addAttribute("CLASS", "label");
			String groupEmail = dh.getUserGroupEmail(annotation.getIdUserGroup());
			row.addElement("TD").addCDATA(groupEmail != null ? groupEmail : "&nbsp;");

			row   = table.addElement("TR");			
			row.addElement("TD").addText("User Group institute").addAttribute("CLASS", "label");
			String groupInstitute = dh.getUserGroupInstitute(annotation.getIdUserGroup());
			row.addElement("TD").addCDATA(groupInstitute != null ? groupInstitute : "&nbsp;");

			row   = table.addElement("TR");			
			row.addElement("TD").addText("Visibility").addAttribute("CLASS", "label");
			row.addElement("TD").addCDATA(annotation.getCodeVisibility() != null && !annotation.getCodeVisibility().equals("") ? Visibility.getDisplay(annotation.getCodeVisibility()) : "&nbsp;");


			String publishedBy = "&nbsp;";
			if (annotation.getCreatedBy() != null && !annotation.getCreatedBy().equals("")) {
				publishedBy = annotation.getCreatedBy();

				if (annotation.getCreateDate() != null) {
					publishedBy += " " + Util.formatDate(annotation.getCreateDate());
				}
			} else {
				if (annotation.getCreateDate() != null) {
					publishedBy = " " + Util.formatDate(annotation.getCreateDate());
				}
			}
			row   = table.addElement("TR");			
			row.addElement("TD").addText("Published by").addAttribute("CLASS", "label");
			row.addElement("TD").addCDATA(publishedBy);


			writer = new org.dom4j.io.HTMLWriter(res.getWriter(), format);	        	
			writer.write(doc);
			writer.flush();
			writer.close();

		} catch (Exception e) {

			if (writer != null) {
				writer.close();
			}

			e.printStackTrace();
			Document doc = DocumentHelper.createDocument();

			res.setContentType("text/html");
			Element root = doc.addElement("HTML");

			Element head = root.addElement("HEAD");
			Element link = head.addElement("link");
			link.addAttribute("rel", "stylesheet");
			link.addAttribute("type", "text/css");
			Element body = root.addElement("BODY");
			body.addText(e.toString());

			XMLWriter w = new org.dom4j.io.HTMLWriter(res.getWriter(), format);

			w.write(doc);
			w.close();

		} finally {
			if (writer != null) {
				writer.close();
			}

			if (sess != null) {
				sess.close();
			}
		}

	}


	private void handleAnnotationFormUploadURLRequest(HttpServletRequest req, HttpServletResponse res) {
		try {

			//
			// COMMENTED OUT CODE: 
			//    String baseURL =  "http"+ (isLocalHost ? "://" : "s://") + req.getServerName() + req.getContextPath();
			//
			// To fix upload problem (missing session in upload servlet for FireFox, Safari), encode session in URL
			// for upload servlet.  Also, use non-secure (http: rather than https:) when making http request; 
			// otherwise, existing session is not accessible to upload servlet.
			//
			//

			String baseURL =  "http"+  "://"  + req.getServerName() + ":" + req.getLocalPort() + req.getContextPath();
			String URL = baseURL + "/" +  GENOPUB_WEBAPP_NAME + "/" +  this.ANNOTATION_UPLOAD_FILES_REQUEST;
			// Encode session id in URL so that session maintains for upload servlet when called from
			// Flex upload component inside FireFox, Safari
			URL += ";jsessionid=" + req.getRequestedSessionId();

			// Get the valid file extensions
			StringBuffer fileExtensions = new StringBuffer();
			for (int x=0; x < Constants.ANNOTATION_FILE_EXTENSIONS.length; x++) {
				if (fileExtensions.length() > 0) {
					fileExtensions.append(";");
				}
				fileExtensions.append("*" + Constants.ANNOTATION_FILE_EXTENSIONS[x]);
			}


			res.setContentType("application/xml");
			res.getOutputStream().println("<UploadURL url='" + URL + "'" + " fileExtensions='" + fileExtensions.toString() + "'" + "/>");

		} catch (Exception e) {
			System.out.println("An error has occured in GenoPubServlet handleAnnotationFormUploadURLRequest - " + e.toString());
		}		
	}


	private void handleAnnotationUploadRequest(HttpServletRequest req, HttpServletResponse res) {

		Session sess = null;
		Integer idAnnotation = null;

		Annotation annotation = null;

		String annotationName = null;
		String codeVisibility = null;
		Integer idGenomeVersion = null;
		Integer idAnnotationGrouping = null;
		Integer idUserGroup = null;

		String fileName = null;
		Transaction tx = null;
		StringBuffer bypassedFiles = new StringBuffer();
		File tempBulkUploadFile = null;

		try {

			if (genoPubSecurity.isGuestRole()) {
				throw new Exception("Insufficient permissions to upload data.");
			}

			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			DictionaryHelper dh = DictionaryHelper.getInstance(sess);

			res.setDateHeader("Expires", -1);
			res.setDateHeader("Last-Modified", System.currentTimeMillis());
			res.setHeader("Pragma", "");
			res.setHeader("Cache-Control", "");
			res.setCharacterEncoding("UTF-8");

			MultipartParser mp = new MultipartParser(req, Integer.MAX_VALUE); 
			Part part;
			while ((part = mp.readNextPart()) != null) {
				String name = part.getName();
				if (part.isParam()) {
					// it's a parameter part
					ParamPart paramPart = (ParamPart) part;
					String value = paramPart.getStringValue();
					if (name.equals("idAnnotation")) {
						idAnnotation = new Integer(String.class.cast(value));
					} else if (name.equals("name")) {
						annotationName = value;
					} else if (name.equals("codeVisibility")) {
						codeVisibility = value;
					} else if (name.equals("idGenomeVersion")) {
						idGenomeVersion = new Integer(value);
					} else if (name.equals("idAnnotationGrouping")) {
						if (value != null && !value.equals("")) {
							idAnnotationGrouping = new Integer(value);
						}
					} else if (name.equals("idUserGroup")) {
						if (value != null && !value.equals("")) {
							idUserGroup = new Integer(value);
						}
					}
				}

				if (idAnnotation != null) {
					break;
				} else if (annotationName != null && codeVisibility != null && idGenomeVersion != null && idAnnotationGrouping != null && idUserGroup != null) {
					break;
				}

			}

			

			if (idAnnotation != null) {				
				annotation = (Annotation)sess.get(Annotation.class, idAnnotation);
			} else {
				// If idAnnotation wasn't sent in as parameter, we are adding
				// annotation as part of the upload
				annotation = createNewAnnotation(sess, annotationName, codeVisibility, idGenomeVersion, idAnnotationGrouping.intValue() == -99 ? null : idAnnotationGrouping, idUserGroup.intValue() == -99 ? null : idUserGroup);
				sess.flush();				
			}

			if (annotation != null) {
				if (this.genoPubSecurity.canWrite(annotation)) {
					SimpleDateFormat formatter = new SimpleDateFormat("yyyy");

					// Make sure that the genometry server dir exists
					if (!new File(genometry_genopub_dir).exists()) {
						boolean success = (new File(genometry_genopub_dir)).mkdir();
						if (!success) {
							throw new Exception("Unable to create directory " + genometry_genopub_dir);      
						}
					}

					String annotationFileDir = annotation.getDirectory(genometry_genopub_dir);

					// Create annotation directory if it doesn't exist
					if (!new File(annotationFileDir).exists()) {
						boolean success = (new File(annotationFileDir)).mkdir();
						if (!success) {
							throw new Exception("Unable to create directory " + annotationFileDir);      
						}      
					}

					while ((part = mp.readNextPart()) != null) {        
						if (part.isFile()) {
							// it's a file part
							FilePart filePart = (FilePart) part;
							fileName = filePart.getFileName();
							
							//is it a bulk upload? 
							if (fileName.endsWith("bulkUpload")) {
								//write temp file
								tempBulkUploadFile = new File (genometry_genopub_dir, "TempFileDeleteMe_"+USeqArchive.createRandowWord(6));
								filePart.writeTo(tempBulkUploadFile); 
								//make new annotations based on current annotation with modifications from the 1.ablk text file
								AnnotationGrouping ag = getDefaultAnnotationGrouping(annotation, sess, idAnnotationGrouping);							
								uploadBulkAnnotations(sess, tempBulkUploadFile, annotation, ag, res);
								if (tempBulkUploadFile.exists()) { 
									if (!tempBulkUploadFile.delete()) {
										Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Unable to delete file " + tempBulkUploadFile.getName() + " during bulk upload.");
									}
									break;
								}
							}

							// Is this a valid file extension?
							if (!Util.isValidAnnotationFileType(fileName)) {
								String message = "Bypassing upload of annotation file  " + fileName + " for annotation " + annotation.getName() + ".  Unsupported file extension.";    	    					
								throw new UnsupportedFileTypeException(message);
							}

							// If this is a bar file, does the file name match a known segment name?
							if (fileName.toUpperCase().endsWith(".BAR")) {
								GenomeVersion genomeVersion = GenomeVersion.class.cast(sess.load(GenomeVersion.class, annotation.getIdGenomeVersion()));
								if (!Util.fileHasSegmentName(fileName, genomeVersion)) {
									String message = "Bypassing upload of annotation file  " + fileName + " for annotation " + annotation.getName() + ".  File name is invalid because it does not start with a valid segment name.";    	    					
									throw new IncorrectFileNameException(message);
								}
							}
							
							if (fileName != null) {
								// the part actually contained a file
								File file = new File (annotationFileDir, fileName);
								long size = filePart.writeTo(file);
								//check size of text files
								if (Util.tooManyLines(file)){
									if (!file.delete()) {
										Logger.getLogger(this.getClass().getName()).log(Level.WARNING, "Unable to delete file " + file.getName() + " during bulk upload.");
									}
									throw new FileTooBigException("Aborting upload, text formatted annotation file '" + annotation.getName() + " exceeds the maximum allowed size ("+
											Constants.MAXIMUM_NUMBER_TEXT_FILE_LINES+" lines). Convert to xxx.useq (see http://useq.sourceforge.net/useqArchiveFormat.html) or other binary form.");
								}
							}

						}
					}
					sess.flush();
				} else {
					throw new InsufficientPermissionException("Bypassing upload of annotation " + annotation.getName() + " due to insufficient permissions.");
				}
			}


			tx.commit();

			this.reportSuccess(res, "idAnnotation", annotation.getIdAnnotation());


		} catch (InsufficientPermissionException e) {
			Logger.getLogger(this.getClass().getName()).warning(e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
			this.reportError(res, e.getMessage(), this.ERROR_CODE_INSUFFICIENT_PERMISSIONS);
		}  catch (IncorrectFileNameException e) {
			Logger.getLogger(this.getClass().getName()).warning(e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
			this.reportError(res, e.getMessage(), ERROR_CODE_INCORRECT_FILENAME);
		}  catch (FileTooBigException e) {
			Logger.getLogger(this.getClass().getName()).warning(e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
			this.reportError(res, e.getMessage(), ERROR_CODE_FILE_TOO_BIG);
		} catch (UnsupportedFileTypeException e) {
			Logger.getLogger(this.getClass().getName()).warning(e.getMessage());
			if (tx != null) {
				tx.rollback();
			}
			this.reportError(res, e.getMessage(), ERROR_CODE_UNSUPPORTED_FILE_TYPE);
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).warning(e.toString());
			if (tx != null) {
				tx.rollback();
			}
			e.printStackTrace();
			this.reportError(res, e.toString(), ERROR_CODE_OTHER);
		} finally {
			if (tempBulkUploadFile != null && tempBulkUploadFile.exists()) tempBulkUploadFile.delete();
			if (sess != null) {
				sess.close();
			}
		}


	}

	/**Reads in a tab delimited file (name, fullPathFileName, summary, description) describing new Annotations to be created using a sourceAnnotation as a template.
	 * @author davidnix*/
	private void uploadBulkAnnotations(Session sess, File spreadSheet, Annotation sourceAnnotation, AnnotationGrouping defaultAnnotationGrouping, HttpServletResponse res) 
	   throws IOException, InsufficientPermissionException {

		BufferedReader in = new BufferedReader (new FileReader(spreadSheet));
		String line;
		Pattern tab = Pattern.compile("([^\\t]+)\\t([^\\t]+)\\t([^\\t]+)\\t(.+)", Pattern.DOTALL);
		
		//for each line create a new annotation
		while ((line = in.readLine()) != null){
			line = line.trim();
			if (line.length() == 0 || line.startsWith("#") || line.startsWith("Name")) {
				continue;
			}
			
			//parse name, fileName, summary, description
			Matcher mat = tab.matcher(line);
			if (mat.matches() == false) { 
				throw new IOException("Unable to parse the required fields from this line -> " + line+"  Aborting bulk upload.");
			}
			String name = mat.group(1).trim();
			if (name.length()==0) {
				throw new IOException("Failed to parse an annotation name from this line -> " + line+"  Aborting bulk upload.");
			}
			File dataFile = new File (mat.group(2).trim());
			if (dataFile.canRead() == false) {
				throw new IOException("Cannot read or find the File in line -> " + line+", looking for "+dataFile+" . Aborting bulk uploading.");
			}
			String summary = mat.group(3).trim();
			String description = mat.group(4).trim();
			
			// If the annotation name is preceded by a directory structure, parse
			// out actual name and create/find the annotation groupings represented
			// the the directory structure embedded in the name;
			String annotationName = "";
			AnnotationGrouping ag = null;
			if (name.lastIndexOf("/") >= 0) {
				annotationName = name.substring(name.lastIndexOf("/") + 1);
				ag = getSpecifiedAnnotationGrouping(sess, defaultAnnotationGrouping, name.substring(0, name.lastIndexOf("/")));
			} else {
				annotationName = name;
				ag = defaultAnnotationGrouping;
			}
			
			
			//make new annotation cloning current annotation
			addNewAnnotation(sess, sourceAnnotation, annotationName, summary, description, dataFile, ag, res);
			
		}
		in.close();
		
	}
	
	/**Fetches the AnnotationGrouping from a particular request. For bulk uploading.
	 * @author davidnix*/
	private AnnotationGrouping getDefaultAnnotationGrouping(Annotation sourceAnnot, Session sess, Integer idAnnotationGrouping) throws Exception{		
		// Get the annotation grouping this annotation is in.
		AnnotationGrouping ag = null;
		if (idAnnotationGrouping == null || idAnnotationGrouping.intValue() == -99) {
			// If this is a root annotation, find the default root annotation
			// grouping for the genome version.
			GenomeVersion gv = GenomeVersion.class.cast(sess.load(GenomeVersion.class, sourceAnnot.getIdGenomeVersion()));
			ag = gv.getRootAnnotationGrouping();
			if (ag == null) {
				throw new Exception("Cannot find root annotation grouping for " + gv.getName());
			}
		} else {
			// Otherwise, find the annotation grouping passed in as a request parameter.			
			ag = AnnotationGrouping.class.cast(sess.load(AnnotationGrouping.class, idAnnotationGrouping));
		}
		return ag;
	}

	/*
	 * Get the annotation grouping (off of the base annotation grouping) specified as a directory structure 
	 * in the annotation name.  If annotation groupings do not exist, create them.  
	 */
	private AnnotationGrouping getSpecifiedAnnotationGrouping(Session sess, AnnotationGrouping annotationGroupingBase, String name){
		AnnotationGrouping agNext = annotationGroupingBase;
		
		String[] tokens = name.split("/");
		AnnotationGrouping agCurrent = annotationGroupingBase;
		for (int x = 0; x < tokens.length; x++) {
			String agName = tokens[x];
			agNext = null;
			for (Iterator<?> i = agCurrent.getAnnotationGroupings().iterator(); i.hasNext();) {
				AnnotationGrouping ag = AnnotationGrouping.class.cast(i.next());
				if (ag.getName().equalsIgnoreCase(agName)) {
					agNext = ag;
					break;
				}
			}
			
			if (agNext == null) {
				agNext = new AnnotationGrouping();
				agNext.setName(agName);
				agNext.setIdParentAnnotationGrouping(agCurrent.getIdAnnotationGrouping());
				agNext.setIdGenomeVersion(agCurrent.getIdGenomeVersion());
				agNext.setIdUserGroup(agCurrent.getIdUserGroup());
				sess.save(agNext);
				sess.flush();	
				sess.refresh(agNext);
				sess.refresh(agCurrent);
			}
			agCurrent = agNext;
			
			
		}
		
		return agNext;		
	}
	
	
	/**Adds an new Annotation cloning in part the source annotation. For bulk uploading.
	 * @author davidnix*/
	private void addNewAnnotation(Session sess, Annotation sourceAnnot, String name, String summary, String description, File dataFile, AnnotationGrouping ag, HttpServletResponse res) 
	    throws IOException, InsufficientPermissionException {		


		// Make sure the user can write this annotation 
		if (!this.genoPubSecurity.canWrite(sourceAnnot)) {
			throw new InsufficientPermissionException("Insufficient permision to write annotation.");
		}

		Annotation dup = new Annotation();

		dup.setName(name);
		if (description.length()!=0) {
			dup.setDescription(description);
		}
		else {
			dup.setDescription(sourceAnnot.getDescription());
		}
		if (summary.length()!=0) {
			dup.setSummary(summary);
		}
		else {
			dup.setSummary(sourceAnnot.getSummary());
		}
		dup.setIdAnalysisType(sourceAnnot.getIdAnalysisType());
		dup.setIdExperimentPlatform(sourceAnnot.getIdExperimentPlatform());
		dup.setIdExperimentMethod(sourceAnnot.getIdExperimentMethod());
		dup.setCodeVisibility(sourceAnnot.getCodeVisibility());
		dup.setIdUserGroup(sourceAnnot.getIdUserGroup());
		dup.setIdUser(sourceAnnot.getIdUser());
		dup.setIdGenomeVersion(sourceAnnot.getIdGenomeVersion());

		sourceAnnot.setCreateDate(new java.sql.Date(System.currentTimeMillis()));
		sourceAnnot.setCreatedBy(this.genoPubSecurity.getUserName());

		sess.save(dup);

		// Add the annotation to the annotation grouping
		Set<Annotation> newAnnotations = new TreeSet<Annotation>(new AnnotationComparator());
		for(Iterator<?> i = ag.getAnnotations().iterator(); i.hasNext();) {
			Annotation a = Annotation.class.cast(i.next());
			newAnnotations.add(a);
		}
		newAnnotations.add(dup);
		ag.setAnnotations(newAnnotations);

		sess.flush();

		// Create a file directory and move in the data file
		dup.setFileName("A" + dup.getIdAnnotation());
		File dir = new File (genometry_genopub_dir, dup.getFileName());
		if (!dir.mkdir()) {
			throw new IOException("Failed to move the dataFile '" + dataFile + "' to its archive location.  Rename failed . Aborting bulk uploading.");
		}
		File moved = new File (dir, dataFile.getName());
		if (dataFile.renameTo(moved) == false) {
			throw new IOException("Failed to move the dataFile '" +dataFile + "' to its archive location  '" + moved +"' . Aborting bulk uploading.");
		}
			
		
	}

	

	private void handleAnnotationEstimateDownloadSizeRequest(HttpServletRequest req, HttpServletResponse res) {
		Session sess = null;

		// Get the request parameter with the keys;
		String keys = req.getParameter("keys");
		
		try {
			sess = HibernateUtil.getSessionFactory().openSession();

	        long estimatedDownloadSize = 0;
			
			String[] keyTokens = keys.split(":");
			for(int x = 0; x < keyTokens.length; x++) {
				String key = keyTokens[x];
				
				String[] idTokens = key.split(",");
				if (idTokens.length != 2) {
					throw new Exception("Invalid parameter format " + key + " encountered. Expected 99,99 for idAnnotation and idAnnotationGrouping");
				}
				Integer idAnnotation = new Integer(idTokens[0]);
				
				Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));
				for (File file : annotation.getFiles(this.genometry_genopub_dir)) {
					double compressionRatio = 1;
					if (file.getName().toUpperCase().endsWith("BAR")) {
						compressionRatio = 3;
					} else if (file.getName().toUpperCase().endsWith("BED")) {
						compressionRatio = 2.5;
					} else if (file.getName().toUpperCase().endsWith("GFF")) {
						compressionRatio = 3;
					} else if (file.getName().toUpperCase().endsWith("BRS")) {
						compressionRatio = 4;
					} else if (file.getName().toUpperCase().endsWith("BGN")) {
						compressionRatio = 3;
					} else if (file.getName().toUpperCase().endsWith("BGR")) {
						compressionRatio = 3;
					} else if (file.getName().toUpperCase().endsWith("BP1")) {
						compressionRatio = 3;
					} else if (file.getName().toUpperCase().endsWith("BP2")) {
						compressionRatio = 3;
					} else if (file.getName().toUpperCase().endsWith("CYT")) {
						compressionRatio = 3;
					} else if (file.getName().toUpperCase().endsWith("GTF")) {
						compressionRatio = 3;
					} else if (file.getName().toUpperCase().endsWith("PSL")) {
						compressionRatio = 3;
					} else if (file.getName().toUpperCase().endsWith("USEQ")) {
						compressionRatio = 1;
					} else if (file.getName().toUpperCase().endsWith("BNIB")) {
						compressionRatio = 2;
					}  else if (file.getName().toUpperCase().endsWith("FASTA")) {
						compressionRatio = 2;
					}       
					estimatedDownloadSize += new BigDecimal(file.length() / compressionRatio).longValue();
				}
			}

			// Store download keys in session b/c Flex FileReference cannnot
			// handle long request parameter
			req.getSession().setAttribute(SESSION_DOWNLOAD_KEYS, keys);
			
			this.reportSuccess(res, "size", Long.valueOf(estimatedDownloadSize).toString());
		} catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).warning(e.toString());
			e.printStackTrace();
			this.reportError(res, e.toString(), ERROR_CODE_OTHER);
		} finally {
			if (sess != null) {
				sess.close();
			}
		}
	}
	


	private void handleAnnotationDownloadRequest(HttpServletRequest req, HttpServletResponse res) {
		Session sess = null;

		// Get the download keys stored in session when download size estimated.  
		// Can't use request parameter here do to Flex FileReference url properties
		// size restriction.
		String keys = (String)req.getSession().getAttribute(SESSION_DOWNLOAD_KEYS);
		
		// Now empty out the session attribute
		req.getSession().setAttribute(SESSION_DOWNLOAD_KEYS, "");
	    
	    // Get the parameter that tells us if we are handling a large download.
		ArchiveHelper archiveHelper = new ArchiveHelper();
		if (req.getParameter("mode") != null && !req.getParameter("mode").equals("")) {
	      archiveHelper.setMode(req.getParameter("mode"));
	    }
		
		try {
			if (keys == null || keys.equals("")) {
				throw new Exception("Cannot perform download due to empty keys parameter.");
			}
			sess = HibernateUtil.getSessionFactory().openSession();
		        
			res.setContentType("application/x-download");
		    res.setHeader("Content-Disposition", "attachment;filename=genopub_annotations.zip");
		    res.setHeader("Cache-Control", "max-age=0, must-revalidate");
		    
	        // Open the archive output stream
	        archiveHelper.setTempDir("./");
	        TarArchiveOutputStream tarOut = null;
	        ZipOutputStream zipOut = null;
	        if (archiveHelper.isZipMode()) {
	          zipOut = new ZipOutputStream(res.getOutputStream());
	        } else {
	          tarOut = new TarArchiveOutputStream(res.getOutputStream());
	        }
	        
	        long totalArchiveSize = 0;
			
			String[] keyTokens = keys.split(":");
			for(int x = 0; x < keyTokens.length; x++) {
				String key = keyTokens[x];
				
				String[] idTokens = key.split(",");
				if (idTokens.length != 2) {
					throw new Exception("Invalid parameter format " + key + " encountered. Expected 99,99 for idAnnotation and idAnnotationGrouping");
				}
				Integer idAnnotation = new Integer(idTokens[0]);
				Integer idAnnotationGrouping = new Integer(idTokens[1]);
				
				Annotation annotation = Annotation.class.cast(sess.load(Annotation.class, idAnnotation));
				
				if (!this.genoPubSecurity.canRead(annotation)) {
					throw new InsufficientPermissionException("Insufficient permission to read/download annotation.");
				}
				
				AnnotationGrouping annotationGrouping = null;
				if (idAnnotationGrouping.intValue() == -99) {
					DictionaryHelper dh = DictionaryHelper.getInstance(sess);
					GenomeVersion gv = dh.getGenomeVersion(annotation.getIdGenomeVersion());
					annotationGrouping = gv.getRootAnnotationGrouping();
				} else {
					for(Iterator<?>i = annotation.getAnnotationGroupings().iterator(); i.hasNext();) {
						AnnotationGrouping ag = AnnotationGrouping.class.cast(i.next());
						if (ag.getIdAnnotationGrouping().equals(idAnnotationGrouping)) {
							annotationGrouping = ag;
							break;
							
						}
					}
					
				}
				if (annotationGrouping == null) {
					throw new Exception("Unable to find annotation grouping " + idAnnotationGrouping);
				}
				
				String path = annotationGrouping.getQualifiedName() + "/" + annotation.getName() + "/";
				
				
				for (File file : annotation.getFiles(this.genometry_genopub_dir)) {
					String zipEntryName = path + file.getName();
					archiveHelper.setArchiveEntryName(zipEntryName);
		            
		            // If we are using tar, compress the file first using
		            // zip.  If we are zipping the file, just open
		            // it to read.            
		            InputStream in = archiveHelper.getInputStreamToArchive(file.getAbsolutePath(), zipEntryName);
		            

		            // Add an entry to the archive 
		            // (The file name starts after the year subdirectory)
		            ZipEntry zipEntry = null;
		            if (archiveHelper.isZipMode()) {
		              // Add ZIP entry 
		              zipEntry = new ZipEntry(archiveHelper.getArchiveEntryName());
		              zipOut.putNextEntry(zipEntry);              
		            } else {
		              // Add a TAR archive entry
		              TarArchiveEntry entry = new TarArchiveEntry(archiveHelper.getArchiveEntryName());
		              entry.setSize(archiveHelper.getArchiveFileSize());
		              tarOut.putArchiveEntry(entry);
		            }
		            

		            // Transfer bytes from the file to the archive file
		            OutputStream out = null;
		            if (archiveHelper.isZipMode()) {
		              out = zipOut;
		            } else {
		              out = tarOut;
		            }
		            int size = archiveHelper.transferBytes(in, out);
		            totalArchiveSize += size;

		            if (archiveHelper.isZipMode()) {
		              zipOut.closeEntry();              
		              totalArchiveSize += zipEntry.getCompressedSize();
		            } else {
		              tarOut.closeArchiveEntry();
		              totalArchiveSize += archiveHelper.getArchiveFileSize();
		            }
		            
		            // Remove temporary files
		            archiveHelper.removeTemporaryFile();
				
				}
				

				
			}
	        
	        if (archiveHelper.isZipMode()) {
	          zipOut.finish();
	          zipOut.flush();          
	        } else {
	          tarOut.close();
	          tarOut.flush();
	        }

			
		} catch (InsufficientPermissionException e) {
			Logger.getLogger(this.getClass().getName()).warning(e.getMessage());
			this.reportError(res, e.getMessage(), this.ERROR_CODE_INSUFFICIENT_PERMISSIONS);
		}  catch (Exception e) {
			Logger.getLogger(this.getClass().getName()).warning(e.toString());
			e.printStackTrace();
			this.reportError(res, e.toString(), ERROR_CODE_OTHER);
		} finally {
			if (sess != null) {
				sess.close();
			}
		}
	}


	private void handleUsersAndGroupsRequest(HttpServletRequest request, HttpServletResponse res) {
		Session sess = null;

		Document doc = DocumentHelper.createDocument();
		Element root = doc.addElement("UsersAndGroups");

		try {
			sess = HibernateUtil.getSessionFactory().openSession();

			// Get group members
			StringBuffer query = new StringBuffer();
			query.append("SELECT      gr, ");
			query.append("            mem   ");
			query.append("FROM        UserGroup as gr   ");
			query.append("LEFT JOIN   gr.members as mem ");
			query.append("ORDER BY    gr.name, mem.lastName, mem.firstName ");

			List<?> rows = sess.createQuery(query.toString()).list();

			String groupNamePrev = "";
			Element groupNode = null;
			Element membersNode = null;
			Element collabsNode = null;
			Element managersNode = null;
			Element userNode = null;
			HashMap<Integer, Element> groupNodeMap = new HashMap<Integer, Element>();

			for (Iterator<?> i = rows.iterator(); i.hasNext();) {
				Object[] row = Object[].class.cast(i.next());
				UserGroup group = (UserGroup)row[0];
				User user = (User)row[1];

				// Only show groups this user managers
				if (!this.genoPubSecurity.isManager(group)) {
					continue;
				}

				if (!group.getName().equals(groupNamePrev)) {
					groupNode = root.addElement("UserGroup");
					groupNode.addAttribute("label", group.getName());
					groupNode.addAttribute("name", group.getName());					
					groupNode.addAttribute("contact", group.getContact() != null ? group.getContact() : "");					
					groupNode.addAttribute("email", group.getEmail() != null ? group.getEmail() : "");					
					groupNode.addAttribute("institute", group.getInstitute() != null ? group.getInstitute() : "");					
					groupNode.addAttribute("idUserGroup", group.getIdUserGroup().toString());
					groupNode.addAttribute("canWrite", this.genoPubSecurity.canWrite(group) ? "Y" : "N");
					groupNodeMap.put(group.getIdUserGroup(), groupNode);					
					membersNode = null;
				}

				if (user != null) {
					if (membersNode == null) {
						membersNode = groupNode.addElement("members");
					}
					userNode = membersNode.addElement("User");
					userNode.addAttribute("label", user.getLastName() + ", " + user.getFirstName());
					userNode.addAttribute("name",  user.getLastName() + ", " + user.getFirstName());
					userNode.addAttribute("idUser", user.getIdUser().toString());
					userNode.addAttribute("type", "Member");					
				}


				groupNamePrev = group.getName();
			}

			// Get group collaborators
			query = new StringBuffer();
			query.append("SELECT      gr, ");
			query.append("            col   ");
			query.append("FROM        UserGroup as gr   ");
			query.append("JOIN   gr.collaborators as col ");
			query.append("ORDER BY    gr.name, col.lastName, col.firstName ");

			rows = sess.createQuery(query.toString()).list();
			for (Iterator<?> i = rows.iterator(); i.hasNext();) {
				Object[] row = Object[].class.cast(i.next());
				
				UserGroup group = (UserGroup)row[0];
				User user = (User)row[1];

				// Only show groups this user managers
				if (!this.genoPubSecurity.isManager(group)) {
					continue;
				}

				groupNode = groupNodeMap.get(group.getIdUserGroup());

				collabsNode = groupNode.element("collaborators");				
				if (collabsNode == null) {
					collabsNode = groupNode.addElement("collaborators");
				}
				userNode = collabsNode.addElement("User");
				userNode.addAttribute("label", user.getLastName() + ", " + user.getFirstName());
				userNode.addAttribute("name",  user.getLastName() + ", " + user.getFirstName());
				userNode.addAttribute("idUser", user.getIdUser().toString());
				userNode.addAttribute("type", "Collaborator");					
			}

			// Get group managers
			query = new StringBuffer();
			query.append("SELECT      gr, ");
			query.append("            mgr   ");
			query.append("FROM        UserGroup as gr   ");
			query.append("JOIN   gr.managers as mgr ");
			query.append("ORDER BY    gr.name, mgr.lastName, mgr.firstName ");

			rows = sess.createQuery(query.toString()).list();
			for (Iterator<?> i = rows.iterator(); i.hasNext();) {
				Object[] row = Object[].class.cast(i.next());
				
				UserGroup group = (UserGroup)row[0];
				User user = (User)row[1];
				groupNode = groupNodeMap.get(group.getIdUserGroup());

				// Only show groups this user managers
				if (!this.genoPubSecurity.isManager(group)) {
					continue;
				}

				managersNode = groupNode.element("managers");				
				if (managersNode == null) {
					managersNode = groupNode.addElement("managers");
				}

				userNode = managersNode.addElement("User");
				userNode.addAttribute("label", user.getName());
				userNode.addAttribute("name",  user.getName());
				userNode.addAttribute("idUser", user.getIdUser().toString());
				userNode.addAttribute("type", "Manager");					
			}

			// Get All Users
			query = new StringBuffer();
			query.append("SELECT      user ");
			query.append("FROM        User as user   ");
			query.append("ORDER BY    user.lastName, user.firstName ");

			List<?> users = sess.createQuery(query.toString()).list();
			for (Iterator<?> i = users.iterator(); i.hasNext();) {
				User user = User.class.cast(i.next());
				userNode = root.addElement("User");
				userNode.addAttribute("label", user.getName());
				userNode.addAttribute("name",  user.getName());
				userNode.addAttribute("idUser", user.getIdUser().toString());
				userNode.addAttribute("firstName",  user.getFirstName() != null ? user.getFirstName() : "");
				userNode.addAttribute("lastName",  user.getLastName() != null ? user.getLastName() : "");
				userNode.addAttribute("middleName",  user.getMiddleName() != null ? user.getMiddleName() : "");
				userNode.addAttribute("email", user.getEmail() != null ? user.getEmail() : "");					
				userNode.addAttribute("institute", user.getInstitute() != null ? user.getInstitute() : "");					
				userNode.addAttribute("userName",  user.getUserName() != null ? user.getUserName() : "");
				userNode.addAttribute("canWrite", this.genoPubSecurity.canWrite(user) ? "Y" : "N");

				if (this.genoPubSecurity.canWrite(user)) {
					userNode.addAttribute("passwordDisplay",  user.getPasswordDisplay() != null ? user.getPasswordDisplay() : "");

					for(Iterator<?> i1 = user.getRoles().iterator(); i1.hasNext();) {
						UserRole role = UserRole.class.cast(i1.next());
						userNode.addAttribute("role", role.getRoleName());
					}

					StringBuffer memberGroups = new StringBuffer();
					for(Iterator<?> i1 = user.getMemberUserGroups().iterator(); i1.hasNext();) {
						UserGroup memberGroup = UserGroup.class.cast(i1.next());
						if (memberGroups.length() > 0) {
							memberGroups.append(", ");
						}
						memberGroups.append(memberGroup.getName());
					}
					userNode.addAttribute("memberGroups", memberGroups.length() > 0 ? memberGroups.toString() : "(none)");

					StringBuffer collaboratorGroups = new StringBuffer();
					for(Iterator<?> i1 = user.getCollaboratingUserGroups().iterator(); i1.hasNext();) {
						UserGroup colGroup = UserGroup.class.cast(i1.next());
						if (collaboratorGroups.length() > 0) {
							collaboratorGroups.append(", ");
						}
						collaboratorGroups.append(colGroup.getName());
					}
					userNode.addAttribute("collaboratorGroups", collaboratorGroups.length() > 0 ? collaboratorGroups.toString() : "(none)");

					StringBuffer managerGroups = new StringBuffer();
					for(Iterator<?> i1 = user.getManagingUserGroups().iterator(); i1.hasNext();) {
						UserGroup mgrGroup = UserGroup.class.cast(i1.next());
						if (managerGroups.length() > 0) {
							managerGroups.append(", ");
						}
						managerGroups.append(mgrGroup.getName());
					}
					userNode.addAttribute("managerGroups", managerGroups.length() > 0 ? managerGroups.toString() : "(none)");

				}
			}

			XMLWriter writer = new XMLWriter(res.getOutputStream(), OutputFormat.createCompactFormat());
			writer.write(doc);

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}




	private void handleUserAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			// Only admins can add users
			if (!this.genoPubSecurity.isAdminRole()) {
				throw new InsufficientPermissionException("Insufficient permissions to add users.");
			}

			// Make sure that the required fields are filled in
			if ((request.getParameter("firstName") == null || request.getParameter("firstName").equals("")) &&
					(request.getParameter("lastName") == null || request.getParameter("lastName").equals(""))) {
				throw new Exception("Please enter first or last name.");
			}
			if (request.getParameter("userName") == null || request.getParameter("userName").equals("")) {
				throw new Exception("Please enter the user name.");
			}

			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			// Make sure this user name doesn't exist
			List<?> users = sess.createQuery("SELECT u.userName from User u where u.userName = '" + request.getParameter("userName") + "'").list();
			if (users.size() > 0) {
				throw new Exception("The user name " + request.getParameter("userName") + " is already taken.  Please enter a unique user name.");
			}

			User user = new User();

			user.setFirstName(request.getParameter("firstName"));
			user.setMiddleName(request.getParameter("middleName"));
			user.setLastName(request.getParameter("lastName"));
			user.setUserName(request.getParameter("userName"));

			sess.save(user);

			sess.flush();

			// Default user to das2user role
			UserRole role = new UserRole();
			role.setRoleName(GenoPubSecurity.USER_ROLE);
			role.setUserName(user.getUserName());
			role.setIdUser(user.getIdUser());
			sess.save(role);
			sess.flush();


			tx.commit();

			this.reportSuccess(res, "idUser", user.getIdUser());


		} catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}



	private void handleUserDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx =  sess.beginTransaction();

			User user = User.class.cast(sess.load(User.class, Util.getIntegerParameter(request, "idUser")));

			// Check write permissions
			if (!this.genoPubSecurity.canWrite(user)) {
				throw new InsufficientPermissionException("Insufficient permissions to delete user.");
			}

			for (Iterator<?> i = user.getRoles().iterator(); i.hasNext();) {
				UserRole role = UserRole.class.cast(i.next());
				sess.delete(role);
			}
			sess.flush();

			sess.refresh(user);



			sess.delete(user);

			sess.flush();

			tx.commit();


			this.reportSuccess(res);


		} catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}




	private void handleUserUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			User user = User.class.cast(sess.load(User.class, Util.getIntegerParameter(request, "idUser")));

			// Check write permissions
			if (!this.genoPubSecurity.canWrite(user)) {
				throw new InsufficientPermissionException("Insufficient permissions to write user.");
			}

			// Make sure that the required fields are filled in
			if ((request.getParameter("firstName") == null || request.getParameter("firstName").equals("")) &&
					(request.getParameter("lastName") == null || request.getParameter("lastName").equals(""))) {
				throw new Exception("Please enter first or last name.");
			}
			if (request.getParameter("userName") == null || request.getParameter("userName").equals("")) {
				throw new Exception("Please enter the user name.");
			}
			if (request.getParameter("role") == null || request.getParameter("role").equals("")) {
				throw new Exception("Please select a role (admin, user, guest).");
			}


			// Get rid of existing roles if the user name has changed
			boolean userNameChanged = false;
			if (!user.getUserName().equals(request.getParameter("userName"))) {
				userNameChanged = true;
			}
			if (userNameChanged) {
				// Make sure this user name doesn't exist
				List<?> users = sess.createQuery("SELECT u.userName from User u where u.userName = '" + request.getParameter("userName") + "'").list();
				if (users.size() > 0) {
					throw new Exception("The user name " + request.getParameter("userName") + " is already taken.  Please enter a unique user name.");
				}

				for (Iterator<?> i = user.getRoles().iterator(); i.hasNext();) {
					UserRole role = UserRole.class.cast(i.next());
					sess.delete(role);						
					sess.flush();
				}
			}


			// Set the fields to the values from the screen
			user.setFirstName(request.getParameter("firstName"));
			user.setMiddleName(request.getParameter("middleName"));
			user.setLastName(request.getParameter("lastName"));
			user.setUserName(request.getParameter("userName"));
			user.setEmail(request.getParameter("email"));
			user.setInstitute(request.getParameter("institute"));

			// Encrypt the password
			if (!request.getParameter("password").equals(User.MASKED_PASSWORD)) {
				String pw = user.getUserName() + ":" + REALM + ":" + request.getParameter("password");
				try {
					String digestedPassword = getDigestedPassword(pw);
					user.setPassword(digestedPassword);       			    
				} catch (Exception e) {
					e.printStackTrace();
					Logger.getLogger(this.getClass().getName()).severe("Unabled to get digested password " + e.toString());
				}
			}

			// Flush here so that if user name changes, the user row is
			// updated before trying to insert a new role
			sess.flush();

			// Set existing user roles
			if (user.getRoles() != null && !userNameChanged) {
				for (Iterator<?> i = user.getRoles().iterator(); i.hasNext();) {
					UserRole role = UserRole.class.cast(i.next());
					
					role.setRoleName(request.getParameter("role"));
					role.setUserName(user.getUserName());
				}
			} else {
				// New create a new user role
				UserRole role = new UserRole();
				role.setRoleName(request.getParameter("role"));
				role.setUserName(user.getUserName());
				role.setIdUser(user.getIdUser());
				sess.save(role);
			}


			sess.flush();

			tx.commit();

			this.reportSuccess(res, "idUser", user.getIdUser());


		} catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}


	private void handleUserPasswordRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			if (this.genoPubSecurity.isGuestRole()) {
				throw new InsufficientPermissionException("Cannot change guest account password");
			}

			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			User user = User.class.cast(sess.load(User.class, this.genoPubSecurity.getIdUser()));

			// Encrypt the password
			if (!request.getParameter("password").equals(User.MASKED_PASSWORD) && !request.getParameter("password").equals("")) {
				String pw = user.getUserName() + ":" + REALM + ":" + request.getParameter("password");
				try {
					String digestedPassword = getDigestedPassword(pw);
					user.setPassword(digestedPassword);                 
				} catch (Exception e) {
					e.printStackTrace();
					Logger.getLogger(this.getClass().getName()).severe("Unabled to get digested password " + e.toString());
				}		
			}

			// Flush here so that if user name changes, the user row is
			// updated before trying to insert a new role
			sess.flush();

			tx.commit();

			this.reportSuccess(res, "idUser", user.getIdUser());


		} catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}



	private void handleGroupAddRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {

			// Only admins can add groups
			if (!this.genoPubSecurity.isAdminRole()) {
				throw new InsufficientPermissionException("Insufficient permissions to add groups.");
			}
			// Make sure required fields are filled in.
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter the group name.");
			}


			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			UserGroup group = new UserGroup();

			group.setName(request.getParameter("name"));

			sess.save(group);

			sess.flush();

			tx.commit();

			this.reportSuccess(res, "idUserGroup", group.getIdUserGroup());


		} catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}


	}


	private void handleGroupDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			UserGroup group = UserGroup.class.cast(sess.load(UserGroup.class, Util.getIntegerParameter(request, "idUserGroup")));

			// Check write permissions
			if (!this.genoPubSecurity.canWrite(group)) {
				throw new InsufficientPermissionException("Insufficient permissions to delete group.");
			}

			sess.delete(group);

			sess.flush();


			tx.commit();


			this.reportSuccess(res);


		} catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}




	private void handleGroupUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			UserGroup group = UserGroup.class.cast(sess.load(UserGroup.class, Util.getIntegerParameter(request, "idUserGroup")));

			// Check write permissions
			if (!this.genoPubSecurity.canWrite(group)) {
				throw new InsufficientPermissionException("Insufficient permissions to write group.");
			}

			// Make sure required fields are filled in.
			if (request.getParameter("name") == null || request.getParameter("name").equals("")) {
				throw new Exception("Please enter the group name.");
			}

			group.setName(request.getParameter("name"));
			group.setContact(request.getParameter("contact"));
			group.setEmail(request.getParameter("email"));
			group.setInstitute(request.getParameter("institute"));


			// Add members
			StringReader reader = new StringReader(request.getParameter("membersXML"));
			SAXReader sax = new SAXReader();
			Document membersDoc = sax.read(reader);
			TreeSet<User> members = new TreeSet<User>(new UserComparator());
			for(Iterator<?> i = membersDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element memberNode = (Element)i.next();
				Integer idUser = new Integer(memberNode.attributeValue("idUser"));
				User member = User.class.cast(sess.get(User.class, idUser));
				members.add(member);
			}    
			group.setMembers(members);

			// Add collaborators
			reader = new StringReader(request.getParameter("collaboratorsXML"));
			sax = new SAXReader();
			Document collabsDoc = sax.read(reader);
			TreeSet<User> collaborators = new TreeSet<User>(new UserComparator());
			for(Iterator<?> i = collabsDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element collabNode = (Element)i.next();
				Integer idUser = new Integer(collabNode.attributeValue("idUser"));
				User collab = User.class.cast(sess.get(User.class, idUser));
				collaborators.add(collab);
			}    
			group.setCollaborators(collaborators);

			// Add managers
			reader = new StringReader(request.getParameter("managersXML"));
			sax = new SAXReader();
			Document managersDoc = sax.read(reader);
			TreeSet<User> managers = new TreeSet<User>(new UserComparator());
			for(Iterator<?> i = managersDoc.getRootElement().elementIterator(); i.hasNext();) {
				Element mgrNode = (Element)i.next();
				Integer idUser = new Integer(mgrNode.attributeValue("idUser"));
				User mgr = User.class.cast(sess.get(User.class, idUser));
				managers.add(mgr);
			}    
			group.setManagers(managers);


			sess.flush();

			tx.commit();

			this.reportSuccess(res, "idUserGroup", group.getIdUserGroup());


		}  catch (InsufficientPermissionException e) {
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}





	private void handleDictionaryAddRequest(HttpServletRequest request, HttpServletResponse res)  {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			String dictionaryName = request.getParameter("dictionaryName");
			Integer id = null;

			if (dictionaryName.equals("AnalysisType")) {
				AnalysisType dict = new AnalysisType();
				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));
				dict.setIdUser(this.genoPubSecurity.isAdminRole() ? null : this.genoPubSecurity.getIdUser());
				sess.save(dict);
				id = dict.getIdAnalysisType();
			} else if (dictionaryName.equals("ExperimentMethod")) {
				ExperimentMethod dict = new ExperimentMethod();
				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));
				dict.setIdUser(this.genoPubSecurity.isAdminRole() ? null : this.genoPubSecurity.getIdUser());
				sess.save(dict);
				id = dict.getIdExperimentMethod();
			} else if (dictionaryName.equals("ExperimentPlatform")) {
				ExperimentPlatform dict = new ExperimentPlatform();
				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));
				dict.setIdUser(this.genoPubSecurity.isAdminRole() ? null : this.genoPubSecurity.getIdUser());
				sess.save(dict);
				id = dict.getIdExperimentPlatform();
			} 

			sess.flush();

			tx.commit();

			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			root.addAttribute("id", id != null ? id.toString() : "");
			root.addAttribute("dictionaryName", dictionaryName);
			XMLWriter writer = new XMLWriter(res.getOutputStream(),
					OutputFormat.createCompactFormat());
			writer.write(doc);


		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}


	}



	private void handleDictionaryDeleteRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();

			String dictionaryName = request.getParameter("dictionaryName");
			Integer id = Util.getIntegerParameter(request, "id");

			if (dictionaryName.equals("AnalysisType")) {
				AnalysisType dict = AnalysisType.class.cast(sess.load(AnalysisType.class, id));
				// Check write permissions
				if (!this.genoPubSecurity.canWrite(dict)) {
					throw new Exception("Insufficient permissions to delete dictionary entry.");
				}
				sess.delete(dict);

			} else if (dictionaryName.equals("ExperimentMethod")) {
				ExperimentMethod dict = ExperimentMethod.class.cast(sess.load(ExperimentMethod.class, id));
				// Check write permissions
				if (!this.genoPubSecurity.canWrite(dict)) {
					throw new Exception("Insufficient permissions to delete dictionary entry.");
				}
				sess.delete(dict);

			} else if (dictionaryName.equals("ExperimentPlatform")) {
				ExperimentPlatform dict = ExperimentPlatform.class.cast(sess.load(ExperimentPlatform.class, id));
				// Check write permissions
				if (!this.genoPubSecurity.canWrite(dict)) {
					throw new Exception("Insufficient permissions to delete dictionary entry.");
				}
				sess.delete(dict);

			} 

			sess.flush();


			tx.commit();

			this.reportSuccess(res);


		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}


	}


	private void handleDictionaryUpdateRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess = null;
		Transaction tx = null;

		try {
			sess = HibernateUtil.getSessionFactory().openSession();
			tx = sess.beginTransaction();


			String dictionaryName = request.getParameter("dictionaryName");
			Integer id = Util.getIntegerParameter(request, "id");

			if (dictionaryName.equals("AnalysisType")) {
				AnalysisType dict = AnalysisType.class.cast(sess.load(AnalysisType.class, id));
				// Check write permissions
				if (!this.genoPubSecurity.canWrite(dict)) {
					throw new InsufficientPermissionException("Insufficient permissions to write dictionary entry.");
				}

				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));
				if (this.genoPubSecurity.isAdminRole()) {
					dict.setIdUser(Util.getIntegerParameter(request, "idUser"));
				}

			} else if (dictionaryName.equals("ExperimentMethod")) {
				ExperimentMethod dict = ExperimentMethod.class.cast(sess.load(ExperimentMethod.class, id));
				// Check write permissions
				if (!this.genoPubSecurity.canWrite(dict)) {
					throw new Exception("Insufficient permissions to write dictionary entry.");
				}

				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));
				if (this.genoPubSecurity.isAdminRole()) {
					dict.setIdUser(Util.getIntegerParameter(request, "idUser"));
				}

			} else if (dictionaryName.equals("ExperimentPlatform")) {
				ExperimentPlatform dict = ExperimentPlatform.class.cast(sess.load(ExperimentPlatform.class, id));
				// Check write permissions
				if (!this.genoPubSecurity.canWrite(dict)) {
					throw new InsufficientPermissionException("Insufficient permissions to write dictionary entry.");
				}

				dict.setName(request.getParameter("name"));
				dict.setIsActive(Util.getFlagParameter(request, "isActive"));				
				if (this.genoPubSecurity.isAdminRole()) {
					dict.setIdUser(Util.getIntegerParameter(request, "idUser"));
				}
			} 

			sess.flush();

			tx.commit();

			this.reportSuccess(res, "id", id);


		} catch (InsufficientPermissionException e) {

			this.reportError(res, e.getMessage());

			if (tx != null) {
				tx.rollback();				
			}

		} catch (Exception e) {

			e.printStackTrace();
			this.reportError(res, e.toString());

			if (tx != null) {
				tx.rollback();				
			}

		} finally {

			if (sess != null) {
				sess.close();
			}
		}

	}

	private void handleVerifyReloadRequest(HttpServletRequest request, HttpServletResponse res) throws Exception {
		Session sess  = null;

		StringBuffer invalidGenomeVersions = new StringBuffer();
		StringBuffer emptyAnnotations = new StringBuffer();
		int loadCount = 0;
		int unloadCount = 0;
		try {
			sess  = HibernateUtil.getSessionFactory().openSession();

			AnnotationQuery annotationQuery = new AnnotationQuery();
			annotationQuery.runAnnotationQuery(sess, this.genoPubSecurity, true);
			for (Organism organism : annotationQuery.getOrganisms()) {
				for (String genomeVersionName : annotationQuery.getVersionNames(organism)) {

					GenomeVersion gv = annotationQuery.getGenomeVersion(genomeVersionName);

					List<Segment> segments = annotationQuery.getSegments(organism, genomeVersionName);  
					// Make sure that genome versions with annotations or sequence have at least
					// one segment.
					if (annotationQuery.getQualifiedAnnotations(organism, genomeVersionName).size() > 0 || gv.hasSequence(this.genometry_genopub_dir)) {
						if (segments == null || segments.size() == 0) {
							if (invalidGenomeVersions.length() > 0) {
								invalidGenomeVersions.append(", ");
							}
							invalidGenomeVersions.append(genomeVersionName);
						}
					}
					// Keep track of how many annotations have missing files
					for(Iterator i = annotationQuery.getQualifiedAnnotations(organism, genomeVersionName).iterator(); i.hasNext();) {
						QualifiedAnnotation qa = (QualifiedAnnotation)i.next();
						
						if (qa.getAnnotation().getFileCount(this.genometry_genopub_dir) == 0) {
							if (emptyAnnotations.length() > 0) {
								emptyAnnotations.append("\n");
							}
							emptyAnnotations.append(gv.getName() + ":  ");
							break;
						}
					}
					boolean firstAnnot = true;
					for(Iterator i = annotationQuery.getQualifiedAnnotations(organism, genomeVersionName).iterator(); i.hasNext();) {
						QualifiedAnnotation qa = (QualifiedAnnotation)i.next();
						if (qa.getAnnotation().getFileCount(this.genometry_genopub_dir) == 0) {
							if (firstAnnot) {
								firstAnnot = false;
							} else {
								if (emptyAnnotations.length() > 0) {
									emptyAnnotations.append(", ");
								}								
							}
							emptyAnnotations.append(qa.getAnnotation().getName());
						} else {
							loadCount++; 
						}
					}
					List<UnloadAnnotation> unloadAnnotations = AnnotationQuery.getUnloadedAnnotations(sess, genoPubSecurity, gv);
					unloadCount = unloadCount + unloadAnnotations.size();

				}
			}
			
			
			StringBuffer confirmMessage = new StringBuffer();

			if (loadCount > 0 || unloadCount > 0) {
				if (loadCount > 0) {
					confirmMessage.append(loadCount + " annotation(s) and ready to load to DAS/2.\n\n");
				}
				if (unloadCount > 0) {
					confirmMessage.append(unloadCount + " annotation(s) ready to unload from DAS/2.\n\n");
				} 
				confirmMessage.append("Do you wish to continue?\n\n");					
			} else {
				confirmMessage.append("No annotations are queued for reload.  Do you wish to continue?\n\n");
			}
			
			StringBuffer message = new StringBuffer();
			if (invalidGenomeVersions.length() > 0 || emptyAnnotations.length() > 0) {
			
				if (invalidGenomeVersions.length() > 0) {
					message.append("Annotations and sequence for the following genome versions will be bypassed due to missing segment information:\n" + 
							invalidGenomeVersions.toString() +  
					".\n\n");			
				}
				if (emptyAnnotations.length() > 0) {
					message.append("The following empty annotations will be bypassed:\n" + 
							emptyAnnotations.toString() +  
					".\n\n");			
				}
				message.append(confirmMessage.toString());
				this.reportError(res, message.toString()); 

			} else {				
				this.reportSuccess(res, confirmMessage.toString());
			}
		} catch (Exception e) {
			e.printStackTrace();
			this.reportError(res, e.toString());

		} finally {

			if (sess != null) {
				sess.close();
			}
		}
	}


	private String getFlexHTMLWrapper(HttpServletRequest request) {
		StringBuffer buf = new StringBuffer();
		BufferedReader input = null;
		try {
			String fileName = getServletContext().getRealPath("/");
			fileName += "/" + this.GENOPUB_HTML_WRAPPER;
			FileReader fileReader = new FileReader(fileName);
			input = new BufferedReader(fileReader);
		} catch (FileNotFoundException ex) {
			System.out.println(ex.toString());
		}
		if (input != null) {
			try {
				String line = null;
				String flashVarsLine = null;
				while ((line = input.readLine()) != null) {
					// If we encounter the Flash invocation line,
					// add in the FlashVars if the request parameter idAnnotation
					// was provided.  This will allow us to launch GenoPub
					// and bring up a particular annotation.
					if (line.contains("src") && line.contains("GenoPub")) {
						if (request.getParameter("idAnnotation") != null) {
							flashVarsLine =   "\"FlashVars\", \"idAnnotation=" + request.getParameter("idAnnotation") + "\",";
						}
					}
					buf.append(line);
					buf.append(System.getProperty("line.separator"));
					if (flashVarsLine != null) {
						buf.append(flashVarsLine);
						buf.append(System.getProperty("line.separator"));
						flashVarsLine = null;
					}
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
				try {
					input.close();
				} catch (IOException e) {
				}
			}

		}
		return buf.toString();
	}

	private final boolean getGenoPubDir() {
		// attempt to get properties from servlet context
		ServletContext context = getServletContext();
		genometry_genopub_dir = context.getInitParameter(Constants.GENOMETRY_SERVER_DIR_GENOPUB);

		// Make sure we have the parameter
		if (genometry_genopub_dir == null || genometry_genopub_dir.equals("")) {
			Logger.getLogger(this.getClass().getName()).severe("Unable to find parameter " + Constants.GENOMETRY_SERVER_DIR_GENOPUB);
			return false;
		}

		// Make sure that the genometry server dir exists
		if (!new File(genometry_genopub_dir).exists()) {
			boolean success = (new File(genometry_genopub_dir)).mkdir();
			if (!success) {
				Logger.getLogger(this.getClass().getName()).severe("Unable to create directory " + genometry_genopub_dir);
				return false;
			}
		}

		if (genometry_genopub_dir != null && !genometry_genopub_dir.endsWith("/")) {
			genometry_genopub_dir += "/";			
		}


		Logger.getLogger(this.getClass().getName()).fine("genometry_genopub_dir = " + genometry_genopub_dir);

		return true;
	}

	/**Loads a file's lines into a hash first column is the key, second the value.
	 * Skips blank lines and those starting with a '#'
	 * @return null if an exception in thrown
	 * */
	private static final HashMap<String, String> loadFileIntoHashMap(File file) {
		BufferedReader in = null;
		HashMap<String, String> names = null;
		try {
			names = new HashMap<String, String>();
			in = new BufferedReader(new FileReader(file));
			String line;
			String[] keyValue;
			while ((line = in.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				keyValue = line.split("\\s+");
				if (keyValue.length < 2) {
					continue;
				}
				names.put(keyValue[0], keyValue[1]);
			}            
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			GeneralUtils.safeClose(in);
		}
		return names;
	}



	private String getDigestedPassword(String password) throws NoSuchAlgorithmException{
		byte[] defaultBytes = password.getBytes();
		MessageDigest algorithm = MessageDigest.getInstance("MD5");
		algorithm.reset();
		algorithm.update(defaultBytes);
		byte messageDigest[] = algorithm.digest();

		StringBuffer hexString = new StringBuffer(messageDigest.length * 2);
		for (int i=0; i < messageDigest.length;i++)
		{
			int value1 = (messageDigest[i] >> 4);
			value1 &= 0x0f;
			if (value1 >= 10)
			{
				hexString.append(((char) (value1 - 10 + 'a')));
			}
			else
			{
				hexString.append(((char) (value1 + '0')));
			}
			int value2 = (messageDigest[i] & 0x0f);
			value2 &= 0x0f;
			if (value2 >= 10)
			{
				hexString.append(((char) (value2 - 10 + 'a')));
			}
			else
			{
				hexString.append(((char) (value2 + '0')));
			}
		}
		String digestedPassword = hexString.toString();    
		return digestedPassword;
	}

	private void reportError(HttpServletResponse response, String message) {
		reportError(response, message, null);

	}
	private void reportError(HttpServletResponse response, String message, Integer statusCode) {
		try {
			if (statusCode != null) {
				response.setStatus(statusCode.intValue());
			}
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("Error");
			root.addAttribute("message", message);
			XMLWriter writer = new XMLWriter(response.getOutputStream(), OutputFormat.createCompactFormat());
			writer.write(doc);	    
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	private void reportSuccess(HttpServletResponse response) {
		this.reportSuccess(response, null, null);
	}
	
	
	private void reportSuccess(HttpServletResponse response, String message) {
		try {
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			if (message != null) {
				root.addAttribute("message", message);
			}
			XMLWriter writer = new XMLWriter(response.getOutputStream(), OutputFormat.createCompactFormat());
			writer.write(doc);
		} catch (Exception e) {
			e.printStackTrace();

		}
		
	}



	private void reportSuccess(HttpServletResponse response, String attributeName, Object id) {
		try {
			Document doc = DocumentHelper.createDocument();
			Element root = doc.addElement("SUCCESS");
			if (id != null && attributeName != null) {
				root.addAttribute(attributeName, id.toString());
			}
			XMLWriter writer = new XMLWriter(response.getOutputStream(), OutputFormat.createCompactFormat());
			writer.write(doc);
		} catch (Exception e) {
			e.printStackTrace();

		}
	}

}


