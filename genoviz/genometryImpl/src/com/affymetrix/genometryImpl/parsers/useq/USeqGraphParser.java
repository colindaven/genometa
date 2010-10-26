package com.affymetrix.genometryImpl.parsers.useq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionData;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionScoreData;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.affymetrix.genometryImpl.util.SynonymLookup;

/**For parsing USeq graph data into GenoViz graph objects
 * 
 * @author david.nix@hci.utah.edu*/
public class USeqGraphParser {

	private GenometryModel gmodel;
	private String stream_name;
	private static final float defaultFloatValue = 1f;
	private ArchiveInfo archiveInfo;
	
	/** Parse a USeq zip archive stream returning a List of graphs for IGB. Returns null if something goes wrong.
	 * Only works with Position or PositionScore data, others should use the USeqRegionParser. 
	 * @param archiveInfo can be null, include if already read from the stream.*/
	@SuppressWarnings("unchecked")
	public List<GraphSym> parseGraphSyms(InputStream istr, GenometryModel gmodel, String stream_name, ArchiveInfo archiveInfo) {		
		this.gmodel = gmodel;
		this.stream_name = stream_name;
		this.archiveInfo = archiveInfo;

		BufferedInputStream bis = null;
		ZipInputStream zis = null;
		List<GraphSym> graphs = new ArrayList<GraphSym>();

		//open streams
		if (istr instanceof ZipInputStream) zis = (ZipInputStream)istr;
		else {
			if (istr instanceof BufferedInputStream) bis = (BufferedInputStream)istr;
			else bis = new BufferedInputStream(istr);
			zis = new ZipInputStream(bis);
		}
		DataInputStream dis = new DataInputStream(zis); 

		try {
			//make ArchiveInfo from first ZipEntry
			if (this.archiveInfo == null){
				zis.getNextEntry();
				this.archiveInfo = new ArchiveInfo(zis, false);
				archiveInfo = this.archiveInfo;
			}
			
			//check that they are loading the data into the correct genome build
			String genomeVersion = archiveInfo.getVersionedGenome();
			AnnotatedSeqGroup asg = gmodel.getSelectedSeqGroup();
			if (asg!= null && asg.isSynonymous(genomeVersion) == false){
				throw new IOException ("\nGenome versions differ! Cannot load this useq data from "+genomeVersion+" into the current genome in view. Navigate to the correct genome and reload or add a synonym.\n");
			}

			//for each entry build appropriate arrays, may contain multiple stranded chromosome slices so first build and hash them. 
			ZipEntry ze;
			ArrayList al = new ArrayList();
			HashMap<String, ArrayList> chromData = new HashMap<String, ArrayList>();
			String chromStrand;
			SliceInfo si = null;
			while ((ze = zis.getNextEntry()) != null){
				//make SliceInfo
				si = new SliceInfo(ze.getName());	
				//PositionData, just positions, no values
				if (USeqUtilities.POSITION.matcher(si.getBinaryType()).matches()) {
					PositionData pd = new PositionData (dis, si);
					chromStrand = si.getChromosome()+si.getStrand();				
					al = chromData.get(chromStrand);
					if (al == null){
						al = new ArrayList();
						chromData.put(chromStrand, al);
					}
					al.add(pd);
				}
				//PositionData, just positions, no values
				else if (USeqUtilities.POSITION_SCORE.matcher(si.getBinaryType()).matches()) {
					PositionScoreData pd = new PositionScoreData (dis, si);
					chromStrand = si.getChromosome()+si.getStrand();			
					al = chromData.get(chromStrand);
					if (al == null){
						al = new ArrayList();
						chromData.put(chromStrand, al);
					}
					al.add(pd);
				}
				else throw new IOException ("\nIncorrect file type for graph generation -> "+si.getBinaryType()+" . Aborting USeq graph loading.\n");
			}

			//merge each chrom strand dataset and make graphs, note all of the BinaryTypes in the archive are assumed to be the same (e.g. either Position or PositionScore)
			if (USeqUtilities.POSITION.matcher(si.getBinaryType()).matches()) {
				Iterator<String> it = chromData.keySet().iterator();
				while (it.hasNext()){
					chromStrand = it.next();
					al = chromData.get(chromStrand);
					//merge data
					PositionData merged = PositionData.merge(al);
					//pull values
					int xcoords[] = merged.getBasePositions();
					float ycoords[] = new float[xcoords.length];
					Arrays.fill(ycoords, defaultFloatValue);
					//make GraphSym and add to List
					GraphSym graf = makeGraph(merged.getSliceInfo(), xcoords, ycoords);
					graphs.add(graf);
				}
			}
			else if (USeqUtilities.POSITION_SCORE.matcher(si.getBinaryType()).matches()) {
				Iterator<String> it = chromData.keySet().iterator();
				while (it.hasNext()){
					chromStrand = it.next();
					al = chromData.get(chromStrand);
					//merge data
					PositionScoreData merged = PositionScoreData.merge(al);
					//pull values
					int xcoords[] = merged.getBasePositions();
					float ycoords[] = merged.getBaseScores();
					//make GraphSym and add to List
					GraphSym graf = makeGraph(merged.getSliceInfo(), xcoords, ycoords);
					graphs.add(graf);
				}
			}
			else {
				throw new IOException ("USeq graph parsing for "+si.getBinaryType()+" is not implemented.");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			USeqUtilities.safeClose(bis);
			USeqUtilities.safeClose(dis);
			USeqUtilities.safeClose(zis);
		}
		return graphs;
	}

	private GraphSym makeGraph(SliceInfo sliceInfo, int[] xcoords, float[] ycoords){
		//get versionedGenome
		AnnotatedSeqGroup versionGenomeASG = getSeqGroup(archiveInfo.getVersionedGenome(), gmodel);
		//get chromosome
		BioSeq chromosomeBS = determineSeq(versionGenomeASG, sliceInfo.getChromosome(), archiveInfo.getVersionedGenome());
		checkSeqLength(chromosomeBS, xcoords);
		//get strand
		String strand = sliceInfo.getStrand();
		//make GraphSym
		GraphSym graf = new GraphSym(xcoords, ycoords, stream_name, chromosomeBS);
		//add properties
		copyProps(graf, archiveInfo.getKeyValues());
		//set strand
		if (strand.equals(".") ) graf.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_BOTH);						
		else if (strand.equals("+")) graf.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_PLUS);
		else if (strand.equals("-")) graf.setProperty(GraphSym.PROP_GRAPH_STRAND, GraphSym.GRAPH_STRAND_MINUS);	
		//check if an initialGraphStyle has been set, hate to hard code this 
		if (graf.getProperties().containsKey("initialGraphStyle") == false) graf.getProperties().put("initialGraphStyle", "Bar");
		return graf;
	}

	/**Sets new length if it exceeds the existing BioSeq length.*/
	public static void checkSeqLength(BioSeq seq, int[] xcoords) {
		int xcount = xcoords.length;
		if (xcoords[xcount-1] > seq.getLength()) seq.setLength(xcoords[xcount-1]);
	}
	/**Adds all tag values to GraphSym tag values.*/
	public static void copyProps(GraphSym graf, HashMap<String,String> tagvals) {
		Iterator<String> iter = tagvals.keySet().iterator();
		while (iter.hasNext()) {
			String tag = iter.next();
			String val = tagvals.get(tag);
			graf.setProperty(tag, val);
		}
	}

	public static BioSeq determineSeq(AnnotatedSeqGroup seq_group, String chromosome, String versionedGenome) {
		// try to get standard AnnotatedSeqGroup seq id (chromosome) resolution first
		BioSeq seq = seq_group.getSeq(chromosome);

		// if standard AnnotatedSeqGroup seq id resolution doesn't work, look through synonyms
		if (seq == null) {
			SynonymLookup lookup = SynonymLookup.getDefaultLookup();
			//TODO: Convert this to the standard way of getting synomous sequences,
			// but we may have to check for extra bar-specific synonyms involving seq group and version
			for (BioSeq testseq : seq_group.getSeqList()) {
				// testing both seq id and version id (if version id is available)
				if (lookup.isSynonym(testseq.getID(), chromosome)) {
					seq = testseq;
					break;
				}
			}
		}
		//if still null them make new
		if (seq == null)  seq = seq_group.addSeq(chromosome, 1000);
		return seq;
	}

	/** Try an match with an existing versionedGenome AnnotatedSeqGroup if it can't be found, create a new one using the versionedGenome */
	public static AnnotatedSeqGroup getSeqGroup(String versionedGenome, GenometryModel gmodel) {
		AnnotatedSeqGroup group = null;
		//attempt to find versionedGenome:versionedGenome
		group = gmodel.getSeqGroup(versionedGenome + ":" + versionedGenome);
		//attempt to find versionedGenome
		if (group == null)  group = gmodel.getSeqGroup(versionedGenome);
		// if no group found, create a new one using versionedGenome
		if (group == null)   group = gmodel.addSeqGroup(versionedGenome);
		//add group
		if (gmodel.getSelectedSeqGroup() != group) {
			// This is necessary to make sure new groups get added to the DataLoadView.
			// maybe need a SeqGroupModifiedEvent class instead.
			gmodel.setSelectedSeqGroup(group);
		}
		return group;
	}
}
