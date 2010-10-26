package org.bioviz.protannot;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.MutableSeqSpan;
import com.affymetrix.genometryImpl.MutableSeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.TypeContainerAnnot;
import com.affymetrix.genometryImpl.comparator.SeqSymStartComparator;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.SeqUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * Reads xml file to convert it into a Genometry.
 */

final class Xml2GenometryParser {

    private static final boolean DEBUG = false;
    private Map<String,BioSeq> mrna_hash;
    private Map<String,BioSeq> prot_hash;
    // instance variables needed during the parse
    private List<int[]> transCheckExons;	// used to sanity-check exon translation
	private static final String end_codon = "Z";

	public static final String STARTSTR = "start";
	public static final String ENDSTR = "end";
	public static final String TYPESTR = "type";
	public static final String NAMESTR = "name";
	public static final String EXONSTR = "exon";
	public static final String IDSTR = "id";
	public static final String RESIDUESSTR = "residues";
	public static final String MRNASTR = "mrna";
	public static final String STRANDSTR = "strand";
	public static final String CDSSTR = "cds";
	public static final String METHODSTR = "method";
	public static final String AA_START = "aa_start";
	public static final String AA_END = "aa_end";
	public static final String AA_LENGTH = "aa_length";

	/**
	 * Create a new BioSeq and add annotations to it.
	 * @param doc
	 * @return
	 * @throws Exception
	 */
    BioSeq parse(Document doc) throws Exception{
		mrna_hash = new HashMap<String,BioSeq>();
		prot_hash = new HashMap<String,BioSeq>();

        try {
            BioSeq ret_genomic = processDocument(doc);

			return ret_genomic;

        } catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
        }
    }

    /**
    <dnaseq>
    <genesearch>
    <gene>
    <primarytranscript>
    <mrna>
    <exon />
    <exon />
    <cds>
    <cdsseg />
    <cdsseg />
    </cds>
    </mrna>
    </primarytranscript>
    </gene>
    </genesearch>
    </dnaseq>
     */
    /**
     * Takes in Document object to parse it and convert into BioSeq.
     * @param   seqdoc  Document object name
     * @return          Returns BioSeq of given document object.
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
	private BioSeq processDocument(Document seqdoc) {
       Element top_element = seqdoc.getDocumentElement();
		String name = top_element.getTagName();
		if (!name.equalsIgnoreCase("dnaseq")) {
			return null;
		}
		if (DEBUG) {
			System.err.println("processing dna seq");
		}
		String version = "";
		try {
			version = top_element.getAttribute("version");
		} catch (Exception e) {
			// ignore exception
		}
		String seq = "genome";
		try {
			seq = top_element.getAttribute("seq");
		} catch (Exception e) {
			// ignore exception
		}

		BioSeq chrom = buildChromosome(top_element, seq, version);

		processDNASeq(chrom, top_element);

		return chrom;
    }

    private static BioSeq buildChromosome(Element top_element, String seq, String version)
            throws DOMException {
        BioSeq chrom = null;
        NodeList children = top_element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String cname = child.getNodeName();
            if (cname != null && cname.equalsIgnoreCase(RESIDUESSTR)) {
                Text resnode = (Text) child.getFirstChild();
                String residues = resnode.getData();
                chrom = new BioSeq(seq, version, residues.length());
                chrom.setResidues(residues);
            }
        }
        return chrom;
    }

    /**
     * Process dna in BioSeq for each child node of element provided.
     * @param   genomic
     * @param   elem        Node in genomic for which dna is to be processed
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private void processDNASeq(BioSeq genomic, Element elem) {
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (name != null) {
                if (name.equalsIgnoreCase("genesearch")) {
                    processGeneSearch(genomic, (Element) child);
                } else {
                    if (name.equalsIgnoreCase(MRNASTR)) {
                        processMRNA(genomic, (Element) child);
                    }
                }
            }
        }

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (name != null && name.equalsIgnoreCase("aaseq")) {
                processProtein(prot_hash, (Element) child);
            }
        }
    }

    /**
     Process protein in BioSeq for each child node of element provided.
     * @param   elem        Node for which protein is to be processed
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private static void processProtein(Map<String,BioSeq> prot_hash, Element elem) {
        String pid = elem.getAttribute(IDSTR);
        BioSeq protein = prot_hash.get(pid);
        if (protein == null) {
            System.err.println("Error: no bioseq matching id: " + pid
                    + ". Skipping it.");
            return;
        }
        if (DEBUG) {
            System.err.println("aaseq: id = " + pid + ",  " + protein);
        }

        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (name != null && name.equalsIgnoreCase("simsearch")) {
                processSimSearch(protein, (Element) child);
            }
        }
    }

    /**
     *
     * @param   query_seq
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private static void processSimSearch(BioSeq query_seq, Element elem) {
        NodeList children = elem.getChildNodes();
        String method = elem.getAttribute(METHODSTR);
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (name != null && name.equalsIgnoreCase("simhit")) {
                processSimHit(query_seq, (Element) child, method);
            }
        }
    }

    /**
     *
     * @param   query_seq
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.MutableSeqSpan
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.SymWithProps
     * @see     com.affymetrix.genometryImpl.TypeContainerAnnot
     * @see     com.affymetrix.genometryImpl.util.SeqUtils
     */
    private static void processSimHit(BioSeq query_seq, Element elem, String method) {
        // method can never be null -- if it is, the XML is wrong
        TypeContainerAnnot hitSym = new TypeContainerAnnot(method);
        addDescriptors(elem, hitSym);

        String hit_name = elem.getAttribute(NAMESTR);
        String hit_descr = elem.getAttribute("desc");

        if (hit_name != null && hit_name.length() > 0) {
            hitSym.setProperty(NAMESTR, hit_name);
        }
        if (hit_descr != null && hit_descr.length() > 0) {
            hitSym.setProperty("descr", hit_descr);
        }

        SeqSpan hitSpan = null;
        NodeList children = elem.getChildNodes();
        int num_spans = 0, aa_start = Integer.MAX_VALUE, aa_end = Integer.MIN_VALUE;
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element chelem = (Element) child;
                if (name.equalsIgnoreCase("simspan")) {
                    SeqSymmetry spanSym = processSimSpan(query_seq, chelem);
                    ((SymWithProps) spanSym).setProperty(METHODSTR, method);
                    hitSym.addChild(spanSym);
                    SeqSpan spanSpan = spanSym.getSpan(query_seq);
                    if (hitSpan == null) {
                        hitSpan = new SimpleMutableSeqSpan(spanSpan.getMin(), spanSpan.getMax(), query_seq); 
                    } else {
                        SeqUtils.encompass(hitSpan, spanSpan, (MutableSeqSpan) hitSpan);
                    }
                    //hitSym.setProperty(TYPESTR, "hitspan");
					int start = Integer.valueOf(((SymWithProps)spanSym).getProperty(AA_START).toString());
					int end = Integer.valueOf(((SymWithProps)spanSym).getProperty(AA_END).toString());
					aa_start = Math.min(aa_start, start);
					aa_end = Math.max(aa_end, end);
                    num_spans++;
                }
            }
        }
        String prop =  (Integer.valueOf(num_spans)).toString();
        hitSym.setProperty("num_spans", prop);
        hitSym.setProperty(TYPESTR, "simHit");
		hitSym.setProperty(AA_START, String.valueOf(aa_start));
		hitSym.setProperty(AA_END, String.valueOf(aa_end));
		hitSym.setProperty(AA_LENGTH, String.valueOf(aa_end - aa_start));
        hitSym.addSpan(hitSpan);
        hitSym.setID("");
        query_seq.addAnnotation(hitSym);
    }

    /**
     * Adds description from elem to sym.
     * @param   elem    Source from which description is to added.
     * @param   sym     Target to which description is added.
     * @see     com.affymetrix.genometryImpl.SimpleSymWithProps
     */
    private static void addDescriptors(Element elem, SimpleSymWithProps sym) {

        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String name = child.getNodeName();
            if (child instanceof Element) {
                Element chelem = (Element) child;
                if (name.equalsIgnoreCase("descriptor")) {
                    String desc_name = chelem.getAttribute(TYPESTR);
                    Text tnode = (Text) chelem.getFirstChild();
                    if (tnode != null) {
                        String desc_text = tnode.getData();
                        sym.setProperty(desc_name, desc_text);
                    }
                }
            }
        }
        Object test = sym.getProperty("domain_pos");
        if (test != null) {
            sym.setProperty(NAMESTR, test);
        }
    }

    /**
     *
     * @param   query_seq
     * @param   elem
     * @return  SeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SimpleSymWithProps
     */
    private static SeqSymmetry processSimSpan(BioSeq query_seq, Element elem) {
        int start = Integer.parseInt(elem.getAttribute("query_start"));
        int end;
        //  need to standardize on which tag to use!
        try {
            end = Integer.parseInt(elem.getAttribute("query_end"));
        } catch (Exception ex) {
            end = Integer.parseInt(elem.getAttribute("query_stop"));
        }

        SimpleSymWithProps spanSym = new SimpleSymWithProps();
        addDescriptors(elem, spanSym);
        String prop = (Integer.valueOf(start)).toString();
        spanSym.setProperty(AA_START, prop);
        prop = (Integer.valueOf(end)).toString();
        spanSym.setProperty(AA_END, prop);
        prop = (Integer.valueOf(end - start)).toString();
        spanSym.setProperty(AA_LENGTH, prop);
		//Multiplying start and end by 3. Because three letters forms one amino acid.
        SeqSpan qspan = new SimpleSeqSpan((start*3)+query_seq.getMin(), (end*3)+query_seq.getMin(), query_seq);
        spanSym.addSpan(qspan);
        return spanSym;
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private void processGeneSearch(BioSeq genomic, Element elem) {
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (null != nodename && nodename.equalsIgnoreCase("gene")) {
                processGene(genomic, (Element) child);
            }
        }
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private void processGene(BioSeq genomic, Element elem) {

        if (DEBUG) {
			int start = Integer.parseInt(elem.getAttribute(STARTSTR));
			int end = Integer.parseInt(elem.getAttribute(ENDSTR));
            System.err.println("gene:  start = " + start + "  end = " + end);
        }

        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (null != nodename && nodename.equalsIgnoreCase("primarytranscript")) {
                processTranscript(genomic, (Element) child);
            }
        }
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     */
    private void processTranscript(BioSeq genomic, Element elem) {
        if (DEBUG) {
			int start = Integer.parseInt(elem.getAttribute(STARTSTR));
			int end = Integer.parseInt(elem.getAttribute(ENDSTR));
            System.err.println("transcript:  start = " + start + "  end = " + end);
        }
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (null != nodename && nodename.equalsIgnoreCase(MRNASTR)) {
                processMRNA(genomic, (Element) child);
            }
        }
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SimpleSymWithProps
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.SymWithProps
     * @see     com.affymetrix.genometryImpl.TypeContainerAnnot
     * @see     com.affymetrix.genometryImpl.util.SeqUtils
     */
    private void processMRNA(BioSeq genomic, Element elem) {
        int start = Integer.parseInt(elem.getAttribute(STARTSTR));
        int end = Integer.parseInt(elem.getAttribute(ENDSTR));

        if (DEBUG) {
            System.err.println("mrna:  start = " + start + "  end = " + end);
        }
        NodeList children = elem.getChildNodes();
        SeqSpan span = new SimpleSeqSpan(start, end, genomic);

        TypeContainerAnnot m2gSym = new TypeContainerAnnot(elem.getAttribute("method"));
        m2gSym.addSpan(span);
        addDescriptors(elem, m2gSym);
        m2gSym.setProperty(TYPESTR, "mRNA");
        boolean forward = (span.isForward());


		transCheckExons = new ArrayList<int[]>();
        List<SeqSymmetry> exon_list = new ArrayList<SeqSymmetry>();
        List<Node> exon_insert_list = new ArrayList<Node>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodename = child.getNodeName();
            if (nodename != null) {
                if (nodename.equalsIgnoreCase(EXONSTR)) {
                    SymWithProps exSym = processExon(genomic, (Element) child);
                    exSym.setProperty(TYPESTR, EXONSTR);
                    exon_list.add(exSym);
                } else if (nodename.equalsIgnoreCase("exon_insert")) {
                    exon_insert_list.add(child);
                }
            }
        }

        // need to sort exon inserts...
        //    5' to 3' along transcript.  Otherwise, trying to insert a 5'
        //    after a 3' has been inserted ill mess up coordinates of 3'.
        // assuming for now that exon inserts are ordered in the XML

        // sorting exons, so that later position calculations are accurate

        Collections.sort(exon_list, new SeqSymStartComparator( genomic, forward));
        for (SeqSymmetry esym : exon_list) {
            m2gSym.addChild(esym);
        }

		BioSeq mrna = addSpans(m2gSym, genomic, exon_insert_list, start);
		
		String protein_id = determineProteinID(children);

		String amino_acid = getAminoAcid(m2gSym);
		processCDS(children, genomic, m2gSym, mrna, protein_id, amino_acid);

        m2gSym.setID("");
        genomic.addAnnotation(m2gSym);
        mrna.addAnnotation(m2gSym);
    }

	private static String getAminoAcid(TypeContainerAnnot m2gSym){
		String residue = (String) m2gSym.getProperty("protein sequence");

		if(residue == null)
			return "";
		else
			residue += end_codon;

		return residue;
	}

	private static String determineProteinID(NodeList children) throws DOMException {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String nodename = child.getNodeName();
			if (nodename != null && nodename.equalsIgnoreCase("descriptor")) {
				Element el = (Element) child;
				String type = el.getAttribute(TYPESTR);
				if (type != null && type.equalsIgnoreCase("protein_product_id")) {
					Text tnode = (Text) el.getFirstChild();
					return tnode.getData();
				}
			}
		}
		return null;
	}


	private BioSeq addSpans(TypeContainerAnnot m2gSym, BioSeq genomic, List exon_insert_list, int start)
			throws NumberFormatException {
		int exoncount = m2gSym.getChildCount();
		int mrnalength = determinemRNALength(exoncount, m2gSym, genomic, exon_insert_list);
		int end = 0;
		String mrna_id = MRNASTR;
		BioSeq mrna = new BioSeq(mrna_id, null, mrnalength);
		mrna.setBounds(start, start+mrnalength);
		mrna_hash.put(mrna_id, mrna);
		SeqSpan mrna_span = new SimpleSeqSpan(mrna.getMin(), mrna.getMax(), mrna); 
		m2gSym.addSpan(mrna_span);
		for (int i = 0; i < exoncount; i++) {
			SimpleSymWithProps esym = (SimpleSymWithProps) m2gSym.getChild(i);
			SeqSpan gspan = esym.getSpan(genomic);
			end = start + gspan.getLength();
			List<Element> hit_inserts = new ArrayList<Element>();
			end = determineOverlappingExons(exon_insert_list, gspan, hit_inserts, end);
			SeqSpan tspan = new SimpleSeqSpan(start, end, mrna);
			esym.addSpan(tspan);
			if (!hit_inserts.isEmpty()) {
				processExonInsert((MutableSeqSymmetry) esym, hit_inserts, genomic, mrna);
			}
			start = end;
		}
		return mrna;
	}

	/**
	 * check each exon_insert, figure out which (if any) exons it overlaps
	 * @param exon_insert_list
	 * @param gspan
	 * @param hit_inserts
	 * @param end
	 * @return
	 * @throws NumberFormatException
	 */
	private static int determineOverlappingExons(List exon_insert_list, SeqSpan gspan, List<Element> hit_inserts, int end) throws NumberFormatException {
		for (int insert_index = 0; insert_index < exon_insert_list.size(); insert_index++) {
			Element iel = (Element) exon_insert_list.get(insert_index);
			int istart = Integer.parseInt(iel.getAttribute("insert_at"));
			int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
			if (SeqUtils.contains(gspan, (SeqSpan) iel)) {
				// need to add children to this exon symmetry to indicate an insertion
				//   (or possibly deletion?) of bases in the transcript relative to the genomic
				//	    processExonInsert(esym, istart, ilength);
				System.err.println("insert: insertion_start = " + istart + ", length = " + ilength);
				// remove this exon_insert from list to consider in future passes
				//    need to also decrement the insert_index to make sure removal doesn't cause
				//    next exon_insert to not be considered...
				exon_insert_list.remove(insert_index);
				hit_inserts.add(iel);
				insert_index--;
				end += ilength;
			}
		}
		return end;
	}

	private static int determinemRNALength(int exoncount, TypeContainerAnnot m2gSym, BioSeq genomic, List exon_insert_list) throws NumberFormatException {
		int mrnalength = 0;
		for (int i = 0; i < exoncount; i++) {
			SeqSymmetry esym = m2gSym.getChild(i);
			SeqSpan gspan = esym.getSpan(genomic);
			mrnalength += gspan.getLength();
		}
		for (int i = 0; i < exon_insert_list.size(); i++) {
			Element iel = (Element) exon_insert_list.get(i);
			int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
			mrnalength += ilength;
		}
		return mrnalength;
	}


    /**
     *
     * @param   exonSym
     * @param   hit_inserts
     * @param   genomic
     * @param   mrna
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SeqSpan
     */
    private static void processExonInsert(MutableSeqSymmetry exonSym, List<Element> hit_inserts,
            BioSeq genomic, BioSeq mrna) {
        // assumes that hit_inserts are in order 5' to 3' along transcript
        // assumes that each exon_insert in hit_inserts actually is contained in the exon
        // assumes that the genomic and transcript spans of the exon are already
        //       part of the exonSym and that the transcript span already correctly takes into account
        //       the additional bases introduced by the exon inserts

        //   map from genomic coords over to transcript coords to figure out where to "split" the
        //       exonSym into children

        SeqSpan egSpan = exonSym.getSpan(genomic);
        SeqSpan etSpan = exonSym.getSpan(mrna);

        int genStart = egSpan.getStart();
        int transStart = etSpan.getStart();

        for (int insert_index = 0; insert_index < hit_inserts.size(); insert_index++) {
            Element iel = hit_inserts.get(insert_index);
            int istart = Integer.parseInt(iel.getAttribute("insert_at"));
            int ilength = Integer.parseInt(iel.getAttribute("insert_length"));
            int genLength = Math.abs(istart - genStart);
            int transEnd = transStart + genLength;

            // split out exon seg between last insert (or start of exon) and current insert
            //   [unless start of exon and the insert is actually at exact beginning of exon]
            if (istart != genStart) {
                MutableSeqSymmetry segSym = new SimpleMutableSeqSymmetry();
                SeqSpan gSpan = new SimpleSeqSpan(genStart, istart, genomic);  // start of insert is end of exon seg
                SeqSpan tSpan = new SimpleSeqSpan(transStart, transEnd, mrna);
                segSym.addSpan(gSpan);
                segSym.addSpan(tSpan);
                exonSym.addChild(segSym);
            }
            // now add exon seg for the current insert
            transStart = transEnd;
            transEnd += ilength;
            SeqSpan insert_tspan = new SimpleSeqSpan(transStart, transEnd, mrna);
            SeqSpan insert_gspan = new SimpleSeqSpan(istart, istart, genomic);
            MutableSeqSymmetry isegSym = new SimpleMutableSeqSymmetry();
            isegSym.addSpan(insert_tspan);
            // experimenting with adding a zero-length placeholder for exon insert relative to genomic
            isegSym.addSpan(insert_gspan);
            exonSym.addChild(isegSym);

            // set current genomic start point for next loop to location of current insert
            genStart = istart;
            transStart = transEnd;

        }

        // if last insert is not _exactly_ at end of exon, then need to add last exon seg
        //   after finished looping through inserts
        if (genStart != egSpan.getEnd()) {
            SeqSpan gSpan = new SimpleSeqSpan(genStart, egSpan.getEnd(), genomic);
            SeqSpan tSpan = new SimpleSeqSpan(transStart, etSpan.getEnd(), mrna);
            MutableSeqSymmetry endSym = new SimpleMutableSeqSymmetry();
            endSym.addSpan(gSpan);
            endSym.addSpan(tSpan);
            exonSym.addChild(endSym);
        }
    }

    /**
     *
     * @param   genomic
     * @param   elem
     * @return  SymWithProps
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SimpleSymWithProps
     * @see     com.affymetrix.genometryImpl.SymWithProps
     */
    private SymWithProps processExon(BioSeq genomic, Element elem) {
         // should not be any nodes underneath exon tags (at least in current pseudo-DTD
        //  GAH 10-6-2001
        int start = Integer.parseInt(elem.getAttribute(STARTSTR));
        int end = Integer.parseInt(elem.getAttribute(ENDSTR));

        transCheckExons.add(new int[]{start,end});

        SeqSpan span = new SimpleSeqSpan(start, end, genomic);
        SimpleSymWithProps exonsym = new SimpleSymWithProps();
        addDescriptors(elem, exonsym);
        exonsym.setProperty(STARTSTR, elem.getAttribute(STARTSTR));
        exonsym.setProperty(ENDSTR, elem.getAttribute(ENDSTR));
		exonsym.setProperty("length", String.valueOf(end - start));
        exonsym.addSpan(span);
        return exonsym;
    }

	private void processCDS(NodeList children, BioSeq genomic, TypeContainerAnnot m2gSym, BioSeq mrna, String protein_id, String amino_acid) {
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			String nodename = child.getNodeName();
			if (nodename != null && nodename.equalsIgnoreCase(CDSSTR)) {
					processCDS(genomic, (Element) child, m2gSym, mrna, protein_id, amino_acid);
			}
		}
	}


    /**
     *
     * @param   genomic
     * @param   elem
     * @param   m2gSym
     * @param   mrna
     * @param   protein_id
     * @see     com.affymetrix.genometryImpl.BioSeq
     * @see     com.affymetrix.genometryImpl.SimpleSymWithProps
     * @see     com.affymetrix.genometryImpl.MutableSeqSymmetry
     * @see     com.affymetrix.genometryImpl.SeqSpan
     * @see     com.affymetrix.genometryImpl.SeqSymmetry
     * @see     com.affymetrix.genometryImpl.TypeContainerAnnot
     * @see     com.affymetrix.genometryImpl.util.SeqUtils
     */
    private void processCDS(BioSeq genomic, Element elem, SimpleSymWithProps m2gSym,
            BioSeq mrna, String protein_id, String amino_acid) {

        String attr = elem.getAttribute("transstart");
        if (attr == null || attr.length() == 0) {
            attr = elem.getAttribute(STARTSTR);
        }
        int start = Integer.parseInt(attr);

		// transstop indicates last base of actual translation
        attr = elem.getAttribute("transstop");
        if (attr == null || attr.length() == 0) {
            attr = elem.getAttribute(ENDSTR);
        }
        int end = Integer.parseInt(attr);

        checkTranslationLength(transCheckExons,start,end);

        // could just do this as a single seq span (start, end, seq), but then would end up recreating
        //   the cds segments, which will get ignored afterwards...
        SeqSpan gstart_point = new SimpleSeqSpan(start, start, genomic);
        SeqSpan gend_point = new SimpleSeqSpan(end, end, genomic);
        SimpleSymWithProps result = new SimpleSymWithProps();
        result.addSpan(gstart_point);
        SeqSymmetry[] m2gPath = new SeqSymmetry[]{m2gSym};
        SeqUtils.transformSymmetry((MutableSeqSymmetry) result, m2gPath);
        SeqSpan mstart_point = result.getSpan(mrna);

		if(mstart_point == null) {
			throw new NullPointerException("Conflict with start and end in processCDS.");
		}

        result = new SimpleSymWithProps();

        result.addSpan(gend_point);
        SeqUtils.transformSymmetry((MutableSeqSymmetry) result, m2gPath);
        SeqSpan mend_point = result.getSpan(mrna);

		if(mend_point == null) {
			throw new NullPointerException("Conflict with start and end in processCDS.");
		}

        TypeContainerAnnot m2pSym = new TypeContainerAnnot(elem.getAttribute(METHODSTR));

        SeqSpan mspan = new SimpleSeqSpan(mstart_point.getStart(), mend_point.getEnd(), mrna);
        BioSeq protein = new BioSeq(protein_id, null, mspan.getLength());
		protein.setResidues(processAminoAcid(amino_acid));
		protein.setBounds(mspan.getMin(), mspan.getMin() + mspan.getLength());

        prot_hash.put(protein_id, protein);
        SeqSpan pspan = new SimpleSeqSpan(protein.getMin(), protein.getMax(), protein); 
        if (DEBUG) {
            System.err.println("protein: length = " + pspan.getLength());
        }
        m2pSym.addSpan(mspan);
        m2pSym.addSpan(pspan);

        m2pSym.setID("");
        protein.addAnnotation(m2pSym);
        mrna.addAnnotation(m2pSym);

        // Use genometry manipulations to map cds start/end on genome to cds start/end on transcript
        //    (so that cds becomes mrna2protein symmetry on mrna (and on protein...)

    }

	/**
	 * Create String with amino acids, left-justified with spaces versus nucleotides
	 * @param residue - String of amino acids
	 * @return - left-justified String
	 */
	private static String processAminoAcid(String residue){
		if(residue.isEmpty())
			return residue;
		
		char[] amino_acid = new char[residue.length()*3];
		for(int i=0; i < amino_acid.length; i++ ){
			if(i % 3 == 0){
				amino_acid[i] = residue.charAt(i/3);
			}else
				amino_acid[i] = ' ';
		}
		return String.valueOf(amino_acid);
	}

	/**
	 * Sanity check on length of translations (the total should be divisible by 3).
	 * @param transCheckExons
	 * @param start
	 * @param end
	 */
	private static void checkTranslationLength(List<int[]> transCheckExons, int start ,int end){

        int length = 0;
        for(int[] exon : transCheckExons){
            int exon_start = exon[0];
            int exon_end = exon[1];

			//int old_length = length;
            if(exon_start >= start && exon_end <= end){
				// exon completely in translated region
                length += exon_end - exon_start;
            } else if(exon_start <= start && exon_end >= start){
				// translation start is past beginning of exon
                length += exon_end - start;
            } else if(exon_start <= end && exon_end >= end){
				// translation end is before ending of exon
                length += end - exon_start;
            }
        }

        if(length % 3 != 0)
            System.out.println("WARNING:  Translation length is " + length + " and remainder modulo 3 is " + length % 3);
    }
}
