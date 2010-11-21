package com.affymetrix.genometryImpl.parsers.useq;
import com.affymetrix.genometryImpl.parsers.useq.data.*;
import java.io.*;
import java.util.zip.*;
import java.util.*;

/**Class for parsing USeq binary files for DAS2 requests and writing the data to stream. A USeqArchive is created upon request for a USeq data file
 * this should be cached to speed up subsequent retrieval.
 * 
 * @author david.nix@hci.utah.edu*/
public class USeqArchive {

	private File zipFile;
	private ZipFile zipArchive;
	private ArchiveInfo archiveInfo;
	private ZipEntry archiveReadMeEntry;
	private HashMap<String, DataRange[]> chromStrandRegions = new HashMap<String, DataRange[]> ();
	//DAS2 does not support stranded requests at this time so leave false.
	private boolean maintainStrandedness = false;

	public USeqArchive (File zipFile) throws Exception{
		this.zipFile = zipFile;
		parseZipFile();
	}

	/**Fetches from the zip archive the files that intersect the unstranded range request and writes them to the stream.
	 * @return	false if no files found*/
	public boolean writeSlicesToStream (OutputStream outputStream, String chromosome, int beginningBP, int endingBP, boolean closeStream) {
		//fetch any overlapping entries
		ArrayList<ZipEntry> entries = fetchZipEntries(chromosome, beginningBP, endingBP);
		if (entries == null) return false;
		//add readme
		entries.add(0, archiveReadMeEntry);
		ZipOutputStream out = new ZipOutputStream(outputStream);
		DataOutputStream dos = new DataOutputStream(out);
		BufferedInputStream bis = null;
		try {
			int count;
			byte data[] = new byte[2048];
			int numEntries = entries.size();
			SliceInfo sliceInfo = null;
			//for each entry
			for (int i=0; i< numEntries; i++){
				//get input stream to read entry
				ZipEntry entry = entries.get(i);			
				bis = new BufferedInputStream (zipArchive.getInputStream(entry));
				//is this entirely contained or needing to be split?, skip first entry which is the readme file
				if (i!=0) sliceInfo = new SliceInfo(entry.getName());
				if (i == 0 || sliceInfo.isContainedBy(beginningBP, endingBP)){
					out.putNextEntry(entry);
					//read in and write out, wish there was a way of just copying it directly
					while ((count = bis.read(data, 0, 2048))!= -1)  out.write(data, 0, count);
					//close entry
					out.closeEntry();
				}
				//slice the slice
				else sliceAndWriteEntry(beginningBP, endingBP, sliceInfo, bis, out, dos);

				//close input entry input stream
				bis.close();
			}
			//close streams?
			if (closeStream) {				
				out.close();
				outputStream.close();
				dos.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
			USeqUtilities.safeClose(out);
			USeqUtilities.safeClose(outputStream);
			USeqUtilities.safeClose(bis);
			USeqUtilities.safeClose(dos);
			return false;
		}
		return true;
	}


	private void sliceAndWriteEntry(int beginningBP, int endingBP, SliceInfo sliceInfo, BufferedInputStream bis, ZipOutputStream out, DataOutputStream dos) {
		String dataType = sliceInfo.getBinaryType();
		DataInputStream dis = new DataInputStream(bis);
		try {
			//Position
			if (USeqUtilities.POSITION.matcher(dataType).matches()) {
				PositionData d = new PositionData(dis, sliceInfo);
				if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
			}
			//PositionScore
			else if (USeqUtilities.POSITION_SCORE.matcher(dataType).matches()) {
				PositionScoreData d = new PositionScoreData(dis, sliceInfo);
				if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
			}
			//PositionText
			else if (USeqUtilities.POSITION_TEXT.matcher(dataType).matches()) {
				PositionTextData d = new PositionTextData(dis, sliceInfo);
				if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
			}
			//PositionScoreText
			else if (USeqUtilities.POSITION_SCORE_TEXT.matcher(dataType).matches()) {
				PositionScoreTextData d = new PositionScoreTextData(dis, sliceInfo);
				if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
			}
			//Region
			else if (USeqUtilities.REGION.matcher(dataType).matches()) {
				RegionData d = new RegionData(dis, sliceInfo);
				if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
			}
			//RegionScore
			else if (USeqUtilities.REGION_SCORE.matcher(dataType).matches()) {
				RegionScoreData d = new RegionScoreData(dis, sliceInfo);
				if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
			}
			//RegionText
			else if (USeqUtilities.REGION_TEXT.matcher(dataType).matches()) {
				RegionTextData d = new RegionTextData(dis, sliceInfo);
				if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
			}
			//RegionScoreText
			else if (USeqUtilities.REGION_SCORE_TEXT.matcher(dataType).matches()) {
				RegionScoreTextData d = new RegionScoreTextData(dis, sliceInfo);
				if (d.trim(beginningBP, endingBP)) d.write(out, dos, true);
			}
			//unknown!
			else {
				throw new IOException ("Unknown USeq data type, '"+dataType+"', for slicing data from  -> '"+sliceInfo.getSliceName()+"\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
			USeqUtilities.safeClose(out);
			USeqUtilities.safeClose(bis);
		} finally {
			USeqUtilities.safeClose(dis);
		}
	}

	/**Fetches from the zip archive the files that intersect the unstranded range request and saves to a new zip archive.
	 * @return	Sliced zip archive or null if no files found*/
	public File writeSlicesToFile (File saveDirectory, String chromosome, int beginningBP, int endingBP) {
		//fetch any overlapping entries
		ArrayList<ZipEntry> entries = fetchZipEntries(chromosome, beginningBP, endingBP);
		if (entries == null) return null;
		//add readme
		entries.add(0, archiveReadMeEntry);
		//make new zip archive to hold slices
		File slicedZipArchive = new File (saveDirectory, "USeqDataSlice_"+createRandowWord(7)+"."+USeqUtilities.USEQ_EXTENSION_NO_PERIOD);
		ZipOutputStream out = null;
		BufferedInputStream is = null;
		try {
			out = new ZipOutputStream(new FileOutputStream(slicedZipArchive));
			int count;
			byte data[] = new byte[2048];
			int numEntries = entries.size();
			//for each entry
			for (int i=0; i< numEntries; i++){
				//get input stream to read entry
				ZipEntry entry = entries.get(i);
				out.putNextEntry(entry);
				is = new BufferedInputStream (zipArchive.getInputStream(entry));
				//read in and write out, wish there was a way of just copying it directly
				while ((count = is.read(data, 0, 2048))!= -1)  out.write(data, 0, count);
				//close streams
				out.closeEntry();
				is.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			USeqUtilities.safeClose(out);
			USeqUtilities.safeClose(is);
		}
		return slicedZipArchive;
	}
	
	
	/**Fetches the ZipEntries for a given range.  Returns null if none found or chromStrand not found. 
	 * Remember this list isn't stranded so must search entire set.*/
	public ArrayList<ZipEntry> fetchZipEntries (String chromStrand, int beginningBP, int endingBP){
		ArrayList<ZipEntry> al = new ArrayList<ZipEntry>();
		//fetch chromStrand
		DataRange[] dr = chromStrandRegions.get(chromStrand);
		if (dr == null) return null;
		for (int i=0; i< dr.length; i++){
			if (dr[i].intersects(beginningBP, endingBP)) {
				al.add(dr[i].zipEntry);
			}
		}
		if (al.size() == 0) return null;
		return al;
	}

	/**Loads the zip entries into the chromosomeStrand DataRange[] HashMap*/
	@SuppressWarnings("unchecked")
	private void parseZipFile() {
		InputStream is = null;
		try {
			//make ArchiveInfo, it's always the first entry
			if (USeqUtilities.USEQ_ARCHIVE.matcher(zipFile.getName()).matches() == false) throw new IOException("This file does not appear to be a USeq archive! "+zipFile);
			zipArchive = new ZipFile(zipFile);
			Enumeration e = zipArchive.entries();
			archiveReadMeEntry = (ZipEntry) e.nextElement();
			is = zipArchive.getInputStream(archiveReadMeEntry);
			archiveInfo = new ArchiveInfo(is, false);

			//load
			HashMap<String, ArrayList<DataRange>> map = new HashMap<String,ArrayList<DataRange>> ();

			while(e.hasMoreElements()) {
				ZipEntry zipEntry = (ZipEntry) e.nextElement();
				SliceInfo sliceInfo = new SliceInfo(zipEntry.getName());
				//get chromStrand and ranges
				String chromName;
				if (maintainStrandedness) chromName = sliceInfo.getChromosome()+sliceInfo.getStrand();
				else chromName = sliceInfo.getChromosome();
				//get/make ArrayList
				ArrayList<DataRange> al = map.get(chromName);
				if (al == null){
					al = new ArrayList<DataRange>();
					map.put(chromName, al);
				}
				al.add(new DataRange(zipEntry,sliceInfo.getFirstStartPosition(), sliceInfo.getLastStartPosition()));

			}
			//convert to arrays and sort
			Iterator<String> it = map.keySet().iterator();
			while (it.hasNext()){
				String chromName = it.next();
				ArrayList<DataRange> al = map.get(chromName);
				DataRange[] dr = new DataRange[al.size()];
				al.toArray(dr);
				Arrays.sort(dr);
				chromStrandRegions.put(chromName, dr);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		finally {
			USeqUtilities.safeClose(is);
		}
	}

	private class DataRange implements Comparable<DataRange>{
		ZipEntry zipEntry;
		int beginningBP;
		int endingBP;
		public DataRange (ZipEntry zipEntry, int beginningBP, int endingBP){
			this.zipEntry = zipEntry;
			this.beginningBP = beginningBP;
			this.endingBP = endingBP;
		}
		public boolean intersects (int start, int stop){
			if (stop <= beginningBP || start >= endingBP) return false;
			return true;
		}
		/**Sorts by beginningBP, smaller to larger.*/
		public int compareTo(DataRange other){
			if (beginningBP < other.beginningBP) return -1;
			if (beginningBP > other.beginningBP) return 1;
			return 0;
		}
	}

	//alphabet minus 28 abiguous characters
	public static String[] nonAmbiguousLetters = {"A","B","C","D","E","F","G","H","J","K","L","M","N",
		"P","Q","R","T","U","V","W","X","Y","3","4","6","7","8","9"};		

	/**Creates pseudorandom Strings derived from an alphabet of String[] using the
	 * java.util.Random class.  Indicate how long you want a particular word and
	 * the number of words.*/
	public static String[] createRandomWords(String[] alphabet,int lengthOfWord,int numberOfWords) {
		ArrayList<String> words = new ArrayList<String>();
		Random r = new Random();
		int len = alphabet.length;
		for (int i = 0; i < numberOfWords; i++) {
			StringBuffer w = new StringBuffer();
			for (int j = 0; j < lengthOfWord; j++) {
				w.append(alphabet[r.nextInt(len)]);
			}
			words.add(w.toString());
		}
		String[] w = new String[words.size()];
		words.toArray(w);
		return w;
	}

	/**Returns a random word using nonambiguous alphabet.  Don't use this method for creating more than one word!*/
	public static String createRandowWord(int lengthOfWord){
		return createRandomWords(nonAmbiguousLetters, lengthOfWord,1)[0];
	}

	public ArchiveInfo getArchiveInfo() {
		return archiveInfo;
	}
}
