package com.affymetrix.genometryImpl.parsers.useq;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import com.affymetrix.genometryImpl.UcscBedSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SymWithProps;
import java.util.regex.*;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.parsers.useq.data.Position;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionData;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionScore;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionScoreData;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionScoreText;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionScoreTextData;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionText;
import com.affymetrix.genometryImpl.parsers.useq.data.PositionTextData;
import com.affymetrix.genometryImpl.parsers.useq.data.Region;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionData;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionScore;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionScoreData;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionScoreText;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionScoreTextData;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionText;
import com.affymetrix.genometryImpl.parsers.useq.data.RegionTextData;

/**For parsing binary USeq region data into GenViz display objects.
 * @author david.nix@hci.utah.edu*/
public final class USeqRegionParser  {

	private List<SeqSymmetry> symlist = new ArrayList<SeqSymmetry>();
	private String nameOfTrack = null;
	//private boolean indexNames = false;
	private AnnotatedSeqGroup group;
	private boolean addAnnotationsToSeq;
	private ArchiveInfo archiveInfo;
	private SliceInfo sliceInfo;
	public static final Pattern TAB = Pattern.compile("\\t");


	public List<SeqSymmetry> parse(InputStream istr, AnnotatedSeqGroup group, String stream_name, boolean addAnnotationsToSeq, ArchiveInfo ai) {		
		this.group = group;
		symlist = new ArrayList<SeqSymmetry>();
		nameOfTrack = stream_name;
		this.addAnnotationsToSeq = addAnnotationsToSeq;
		this.archiveInfo = ai;

		//open IO
		BufferedInputStream bis = null;
		ZipInputStream zis = null;

		if (istr instanceof ZipInputStream) zis = (ZipInputStream)istr;
		else {
			if (istr instanceof BufferedInputStream) bis = (BufferedInputStream)istr;
			else bis = new BufferedInputStream(istr);
			zis = new ZipInputStream(bis);
		}

		//parse it!
		parse(zis);

		return symlist;
	}

	private void parse(ZipInputStream zis)  {
		//open streams
		DataInputStream dis = new DataInputStream(zis);

		try {
			//make ArchiveInfo from first ZipEntry?
			if (archiveInfo == null){
				zis.getNextEntry();
				this.archiveInfo = new ArchiveInfo(zis, false);
			}
			
			//check that they are loading the data into the correct genome build
			String genomeVersion = archiveInfo.getVersionedGenome();
			if (group.getAllVersions().size() != 0 && group.isSynonymous(genomeVersion) == false){
				throw new IOException ("\nGenome versions differ! Cannot load this useq data from "+genomeVersion+" into the current genome in view. Navigate to the correct genome and reload or add a synonym.\n");
			}
			
			//for each entry parse, will contain all of the same kind of data so just parse first to find out data type 
			ZipEntry ze;
			while ((ze = zis.getNextEntry()) != null){
				//set SliceInfo
				sliceInfo = new SliceInfo (ze.getName());
				String dataType = sliceInfo.getBinaryType();
				//Region
				if (USeqUtilities.REGION.matcher(dataType).matches()) parseRegionData(dis);
				//RegionScore
				else if (USeqUtilities.REGION_SCORE.matcher(dataType).matches()) parseRegionScoreData(dis);
				//RegionText
				else if (USeqUtilities.REGION_TEXT.matcher(dataType).matches()) parseRegionTextData(dis);
				//RegionScoreText
				else if (USeqUtilities.REGION_SCORE_TEXT.matcher(dataType).matches()) parseRegionScoreTextData(dis);
				//Position
				else if(USeqUtilities.POSITION.matcher(dataType).matches()) parsePositionData(dis);
				//PositionScore
				else if(USeqUtilities.POSITION_SCORE.matcher(dataType).matches()) parsePositionScoreData(dis);
				//PositionText
				else if(USeqUtilities.POSITION_TEXT.matcher(dataType).matches()) parsePositionTextData(dis);
				//PositionScoreText
				else if(USeqUtilities.POSITION_SCORE_TEXT.matcher(dataType).matches()) parsePositionScoreTextData(dis);
				//unknown!
				else {
					throw new IOException ("Unknown USeq data type, '"+dataType+"', for parsing region data from  -> '"+ze.getName()+"' in "+nameOfTrack +"\n");
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			USeqUtilities.safeClose(dis);
			USeqUtilities.safeClose(zis);
		}
	}

	private void parsePositionData(DataInputStream dis){
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//make data
		PositionData pd = new PositionData (dis, sliceInfo);
		//add syms
		Position[] r = pd.getPositions();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = r[i].getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, start, start+1, null, score, forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start+1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getPosition()+1 > seq.getLength()) seq.setLength(r[r.length-1].getPosition()+1);
	}

	private void parsePositionScoreData(DataInputStream dis){
		PositionScoreData pd = new PositionScoreData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		PositionScore[] r = pd.getPositionScores();
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = r[i].getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, start, start+1, null, r[i].getScore(), forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start+1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getPosition()+1 > seq.getLength()) seq.setLength(r[r.length-1].getPosition()+1);
	}

	private void parsePositionTextData(DataInputStream dis) {
		PositionTextData pd = new PositionTextData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		PositionText[] r = pd.getPositionTexts();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = r[i].getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, start, start+1, r[i].getText(), score, forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start+1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getPosition()+1 > seq.getLength()) seq.setLength(r[r.length-1].getPosition()+1);
	}

	private void parsePositionScoreTextData(DataInputStream dis) {
		PositionScoreTextData pd = new PositionScoreTextData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		PositionScoreText[] r = pd.getPositionScoreTexts();
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			int start = r[i].getPosition();
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, start, start+1, r[i].getText(), r[i].getScore(), forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{start}, new int[]{start+1});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameScoreText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getPosition()+1 > seq.getLength()) seq.setLength(r[r.length-1].getPosition()+1);
	}

	private void parseRegionData(DataInputStream dis) {
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//make data
		RegionData pd = new RegionData (dis, sliceInfo);
		//add syms
		Region[] r = pd.getRegions();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, r[i].getStart(), r[i].getStop(), null, score, forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{r[i].getStart()}, new int[]{r[i].getStop()});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getStop() > seq.getLength()) seq.setLength(r[r.length-1].getStop());
	}

	private void parseRegionScoreData(DataInputStream dis) {
		RegionScoreData pd = new RegionScoreData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		RegionScore[] r = pd.getRegionScores();
		for (int i=0; i< r.length; i++){
			//TODO: rewrite to use a zero child just props Sym see BedParser b.s., this is way inefficient!
			SymWithProps bedline_sym = new UcscBedSym(nameOfTrack, seq, r[i].getStart(), r[i].getStop(), null, r[i].getScore(), forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{r[i].getStart()}, new int[]{r[i].getStop()});
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getStop() > seq.getLength()) seq.setLength(r[r.length-1].getStop());
	}

	private void parseRegionTextData(DataInputStream dis) {
		RegionTextData pd = new RegionTextData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		RegionText[] r = pd.getRegionTexts();
		float score = Float.NEGATIVE_INFINITY; // Float.NEGATIVE_INFINITY signifies that score is not used
		for (int i=0; i< r.length; i++){
			SymWithProps bedline_sym;
			//check to see if this is a bed 12
			//bed12?
			String[] tokens = TAB.split(r[i].getText());
			//yes
			if (tokens.length == 7){
				int min = r[i].getStart();
				int max = r[i].getStop();
				String annot_name = tokens[0];
				// thickStart field
				int thick_min = Integer.parseInt(tokens[1]);
				// thickEnd field
				int thick_max = Integer.parseInt(tokens[2]);
				// itemRgb skip
				// blockCount
				int blockCount = Integer.parseInt(tokens[4]); 
				// blockSizes
				int[] blockSizes = BedParser.parseIntArray(tokens[5]);
				if (blockCount != blockSizes.length) {
					System.out.println("WARNING: block count does not agree with block sizes.  Ignoring " + annot_name + " on " + seq);
					return;
				}
				// blockStarts
				int[] blockStarts = BedParser.parseIntArray(tokens[6]); 
				if (blockCount != blockStarts.length) {
					System.out.println("WARNING: block size does not agree with block starts.  Ignoring " + annot_name + " on " + seq);
					return;
				}
				int[] blockMins = BedParser.makeBlockMins(min, blockStarts);
				int[] blockMaxs = BedParser.makeBlockMaxs(blockSizes, blockMins);
				bedline_sym = new UcscBedSym(nameOfTrack, seq, min, max, annot_name, score, forward, thick_min, thick_max, blockMins, blockMaxs);
			}
			//no
			else {
				bedline_sym = new UcscBedSym(nameOfTrack, seq, r[i].getStart(), r[i].getStop(), r[i].getText(), score, forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{r[i].getStart()}, new int[]{r[i].getStop()});
			}
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getStop() > seq.getLength()) seq.setLength(r[r.length-1].getStop());
	}

	private void parseRegionScoreTextData(DataInputStream dis) {
		RegionScoreTextData pd = new RegionScoreTextData (dis, sliceInfo);
		boolean forward = forward();
		BioSeq seq = getBioSeq();
		//add syms
		RegionScoreText[] r = pd.getRegionScoreTexts();
		for (int i=0; i< r.length; i++){
			SymWithProps bedline_sym;
			//check to see if this is a bed 12
			String[] tokens = TAB.split(r[i].getText());
			//yes
			if (tokens.length == 7){
				int min = r[i].getStart();
				int max = r[i].getStop();
				String annot_name = tokens[0];
				// thickStart field
				int thick_min = Integer.parseInt(tokens[1]);
				// thickEnd field
				int thick_max = Integer.parseInt(tokens[2]);
				// itemRgb skip
				// blockCount
				int blockCount = Integer.parseInt(tokens[4]); 
				// blockSizes
				int[] blockSizes = BedParser.parseIntArray(tokens[5]);
				if (blockCount != blockSizes.length) {
					System.out.println("WARNING: block count does not agree with block sizes.  Ignoring " + annot_name + " on " + seq);
					return;
				}
				// blockStarts
				int[] blockStarts = BedParser.parseIntArray(tokens[6]); 
				if (blockCount != blockStarts.length) {
					System.out.println("WARNING: block size does not agree with block starts.  Ignoring " + annot_name + " on " + seq);
					return;
				}
				int[] blockMins = BedParser.makeBlockMins(min, blockStarts);
				int[] blockMaxs = BedParser.makeBlockMaxs(blockSizes, blockMins);
				bedline_sym = new UcscBedSym(nameOfTrack, seq, min, max, annot_name, r[i].getScore(), forward, thick_min, thick_max, blockMins, blockMaxs);
			}
			//no
			else {
				bedline_sym = new UcscBedSym(nameOfTrack, seq, r[i].getStart(), r[i].getStop(), r[i].getText(), r[i].getScore(), forward, Integer.MIN_VALUE, Integer.MIN_VALUE, new int[]{r[i].getStart()}, new int[]{r[i].getStop()});
			}
			symlist.add(bedline_sym);
			if (addAnnotationsToSeq) seq.addAnnotation(bedline_sym);
			//if (indexNames) seq_group.addToIndex(nameScoreText, bedline_sym);
		}
		//set max
		if (r[r.length-1].getStop() > seq.getLength()) seq.setLength(r[r.length-1].getStop());
	}

	/*find BioSeq or make a new one*/
	private BioSeq getBioSeq(){
		String chromosome = sliceInfo.getChromosome();
		BioSeq seq = group.getSeq(chromosome);
		if (seq == null)  {
			seq = group.addSeq(chromosome, 0);
		}
		return seq;
	}
	/*Returns true if strand is + or .*/
	private boolean forward(){
		String strand = sliceInfo.getStrand();
		return strand.equals("+") || strand.equals(".");
	}



}

