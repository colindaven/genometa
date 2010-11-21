package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SimpleSymWithProps;
import com.affymetrix.genometryImpl.UcscPslSym;
import com.affymetrix.genometryImpl.parsers.IndexWriter;

import com.affymetrix.genometryImpl.parsers.PSLParser;
import com.affymetrix.genometryImpl.parsers.ProbeSetDisplayPlugin;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symloader.PSL;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jnicol
 */
public final class IndexingUtils {
	private static final boolean DEBUG = false;

	/**
	 * Used to index the symmetries for interval searches.
	 */
	public final static class IndexedSyms {
		final File file;
		public final int[] min;
		public final int[] max;
		private final BitSet forward;
		public final long[] filePos;
		private final String typeName;
		final IndexWriter iWriter;

		// for each sym, we have an array of ids generated from the group's id2symhash.
		// Each of these ids is in a byte array instead of a String to save memory
		private final byte[][][] id;

		public IndexedSyms(int resultSize, File file, String typeName, IndexWriter iWriter) {
			min = new int[resultSize];
			max = new int[resultSize];
			forward = new BitSet(resultSize);
			id = new byte[resultSize][][];
			filePos = new long[resultSize + 1];
			this.file = file;
			this.typeName = typeName;
			this.iWriter = iWriter;
		}

		private void setIDs(AnnotatedSeqGroup group, String symID, int i) {
			if (symID == null) {
				// no IDs
				this.id[i] = null;
				return;
			}
			// determine list of IDs for this symmetry index.
			Set<String> extraNames = group.getSymmetryIDs(symID.toLowerCase());
			List<String> ids = new ArrayList<String>(1 + (extraNames == null ? 0 : extraNames.size()));
			ids.add(symID);
			if (extraNames != null) {
				ids.addAll(extraNames);
			}

			int idSize = ids.size();
			this.id[i] = new byte[idSize][];
			for (int j=0;j<idSize;j++) {
				this.id[i][j] = ids.get(j).getBytes();
			}
		}
		
		private SimpleSymWithProps convertToSymWithProps(int i, BioSeq seq, String type) {
			SimpleSymWithProps sym = new SimpleSymWithProps();
			String id = this.id[i] == null ? "" : new String(this.id[i][0]);
			sym.setID(id);
			sym.setProperty("name", id);
			sym.setProperty("method",type);
			if (this.forward.get(i)) {
				sym.addSpan(new SimpleSeqSpan(this.min[i], this.max[i], seq));
			} else {
				sym.addSpan(new SimpleSeqSpan(this.max[i], this.min[i], seq));
			}
			return sym;
		}

	}

	// filename of indexed annotations.
	static String indexedFileName(String dataRoot, File file, String annot_name, AnnotatedSeqGroup genome, BioSeq seq) {
		String retVal = indexedDirName(dataRoot, genome, seq) + "/";
		
		// Change the file path to use forward slash (for Windows OS)
		String fullPath = file.getPath().replace("\\", "/");
		
		String fullDirName = dataRoot + genomeDirName(genome);
		if (!fullDirName.endsWith("/")) {
			fullDirName += "/";
		}
		
		if (fullPath.indexOf(fullDirName) >= 0) {
			String shortenedPath = fullPath.replace(fullDirName, "");
			return retVal + shortenedPath;			
		} else {
			return retVal + annot_name + "_indexed";
		}
	}
	static String indexedDirName(String dataRoot, AnnotatedSeqGroup genome, BioSeq seq) {
		return indexedGenomeDirName(dataRoot, genome) + "/" + seq.getID();
	}
	static String genomeDirName(AnnotatedSeqGroup genome) {
		return genome.getOrganism() + "/" + genome.getID();
	}
	static String indexedGenomeDirName(String dataRoot, AnnotatedSeqGroup genome) {
		String optimizedDirectory = dataRoot + ".indexed";
		return optimizedDirectory + "/" + genomeDirName(genome);
	}


	
	/**
	 *
	 * @param originalGenome
	 * @param tempGenome
	 * @param dataRoot
	 * @param file
	 * @param loadedSyms
	 * @param iWriter
	 * @param typeName
	 * @param returnTypeName
	 * @throws java.io.IOException
	 */
	public static void determineIndexes(
			AnnotatedSeqGroup originalGenome, AnnotatedSeqGroup tempGenome,
			String dataRoot, File file, List loadedSyms, IndexWriter iWriter, String typeName, String returnTypeName) throws IOException {

		for (BioSeq originalSeq : originalGenome.getSeqList()) {
			BioSeq tempSeq = tempGenome.getSeq(originalSeq.getID());
			if (tempSeq == null) {
				continue;	// ignore; this is a seq that was added during parsing.
			}

			Logger.getLogger(IndexingUtils.class.getName()).log(Level.INFO,
					"Determining indexes for {0}, {1}", new Object[]{tempGenome.getID(), tempSeq.getID()});

			// Sort symmetries for this specific chromosome.
			List<SeqSymmetry> sortedSyms =
					IndexingUtils.getSortedAnnotationsForChrom(loadedSyms, tempSeq, iWriter.getComparator(tempSeq));
			if (sortedSyms.isEmpty()) {
				Logger.getLogger(IndexingUtils.class.getName()).log(Level.WARNING,
						"No annotations found for file: {0} on chromosome:{1}", new Object[]{file.getName(), tempSeq.getID()});
				continue;
			}

			String indexedAnnotationsFileName = IndexingUtils.indexedFileName(dataRoot, file, typeName, tempGenome, tempSeq);
			String dirName = indexedAnnotationsFileName.substring(0,indexedAnnotationsFileName.lastIndexOf("/"));
			ServerUtils.createDirIfNecessary(dirName);

			File indexedAnnotationsFile = new File(indexedAnnotationsFileName);
			indexedAnnotationsFile.deleteOnExit();

			IndexedSyms iSyms = new IndexedSyms(sortedSyms.size(), indexedAnnotationsFile, typeName, iWriter);

			// add indexed symmetries to the chromosome (used by types request)
			originalSeq.addIndexedSyms(returnTypeName, iSyms);

			// Write the annotations out to a file.
			IndexingUtils.writeIndexedAnnotations(sortedSyms, tempSeq, tempGenome, iSyms, indexedAnnotationsFileName);
		}
	}

	
	/**
	 * Find symmetries that have IDs or titles matching regex.  Return no more than resultLimit symmetries.
	 * @param genome
	 * @param regex
	 * @return list of Seq symmetries
	 */
	public static Set<SeqSymmetry> findSymsByName(AnnotatedSeqGroup genome, Pattern regex) {
		final Matcher matcher = regex.matcher("");
		Set<SeqSymmetry> results = new HashSet<SeqSymmetry>(100000);

		int resultCount = 0;

		// label for breaking out of loop
		SEARCHSYMS:
		for (BioSeq seq : genome.getSeqList()) {
			for (String type : seq.getIndexedTypeList()) {
				IndexedSyms iSyms = seq.getIndexedSym(type);
				if (iSyms == null) {
					continue;
				}
				if (findSymByName(iSyms, matcher, seq, type, results, resultCount)) {
					break SEARCHSYMS;
				}
			}
		}

		return results;
	}

	private static boolean findSymByName(
			IndexedSyms iSyms, final Matcher matcher, BioSeq seq, String type, Set<SeqSymmetry> results, int resultCount) {
		int symSize = iSyms.min.length;
		for (int i = 0; i < symSize; i++) {
			// test against various IDs
			byte[][] ids = iSyms.id[i];
			boolean foundID = false;
			int idLength = (ids == null) ? 0 : ids.length;
			for (int j=0;j<idLength;j++) {
				String id = new String(ids[j]);
				matcher.reset(id);
				if (matcher.matches()) {
					foundID = true;
					break;
				}
			}
			if (!foundID) {
				continue;
			}

			// found a match
			SimpleSymWithProps sym = iSyms.convertToSymWithProps(i, seq, type);
			results.add(sym);
			resultCount++;
			/*if (resultCount == resultLimit) {
				return true;
			}*/
		}
		return false;
	}



	/**
	 * Create a file of annotations, and index its entries.
	 * @param syms -- a sorted list of annotations (on one chromosome)
	 * @param seq -- the chromosome
	 * @param group -- the group (used to determine IDs for each sym)
	 * @param iSyms
	 * @param indexesFileName
	 * @throws java.io.IOException
	 */
	public static void writeIndexedAnnotations(
			List<SeqSymmetry> syms,
			BioSeq seq,
			AnnotatedSeqGroup group,
			IndexedSyms iSyms,
			String indexesFileName) throws IOException {
		if (DEBUG) {
			System.out.println("in IndexingUtils.writeIndexedAnnotations()");
		}

		createIndexArray(iSyms, syms, seq, group);
		writeIndex(iSyms, indexesFileName, syms, seq);
	}

	/**
	 * Determine file positions and create iSyms array.
	 * @param iSyms
	 * @param syms
	 * @param seq
	 * @throws IOException
	 */
	private static void createIndexArray(
			IndexedSyms iSyms,
			List<SeqSymmetry> syms,
			BioSeq seq,
			AnnotatedSeqGroup group) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int index = 0;
		long currentFilePos = 0;
		IndexWriter iWriter = iSyms.iWriter;
		iSyms.filePos[0] = 0;
		for (SeqSymmetry sym : syms) {
			// Determine symmetry's byte size
			iWriter.writeSymmetry(sym, seq, baos);
			baos.flush();
			byte[] buf = baos.toByteArray();
			baos.reset();

			// add to iSyms, and advance index.
			iSyms.setIDs(group, sym.getID(), index);
			iSyms.min[index] = iWriter.getMin(sym, seq);
			iSyms.max[index] = iWriter.getMax(sym, seq);
			iSyms.forward.set(index,sym.getSpan(seq).isForward());
			currentFilePos += buf.length;
			index++;
			iSyms.filePos[index] = currentFilePos;
			
		}
		GeneralUtils.safeClose(baos);
	}


	/**
	 * Write out indexes to files (for one chromosome).
	 *
	 * @param iSyms
	 * @param indexesFileName -- file
	 * @param syms -- symmetries to write out
	 * @param seq -- chromosome
	 * @throws IOException
	 */
	private static void writeIndex(
			IndexedSyms iSyms,
			String indexesFileName,
			List<SeqSymmetry> syms,
			BioSeq seq) throws IOException {
		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try {
			fos = new FileOutputStream(indexesFileName);
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
			IndexWriter iSymWriter = iSyms.iWriter;
			for (SeqSymmetry sym : syms) {
				iSymWriter.writeSymmetry(sym, seq, dos);	// write out interval<->symmetries
			}
			if ((iSymWriter instanceof PSLParser || iSymWriter instanceof PSL) && indexesFileName.toLowerCase().endsWith(".link.psl")) {
				writeAdditionalLinkPSLIndex(indexesFileName, syms, seq, iSyms.typeName);
			}
		} finally {
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(bos);
			GeneralUtils.safeClose(fos);
		}
	}


	/**
	 * if it's a link.psl file, there is special-casing.
	 * We need to write out the remainder of the annotations as a special file ("...link2.psl")
	 *
	 * @param indexesFileName
	 * @param syms
	 * @param seq
	 * @param typeName
	 * @throws FileNotFoundException
	 */
	private static void writeAdditionalLinkPSLIndex(
			String indexesFileName, List<SeqSymmetry> syms, BioSeq seq, String typeName) throws FileNotFoundException {
		if (DEBUG) {
			System.out.println("in IndexingUtils.writeAdditionalLinkPSLIndex()");
		}

		FileOutputStream fos = null;
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		String secondIndexesFileName = indexesFileName.substring(0, indexesFileName.lastIndexOf(".link.psl"));
		secondIndexesFileName += ".link2.psl";
		try {
			fos = new FileOutputStream(secondIndexesFileName);
			bos = new BufferedOutputStream(fos);
			dos = new DataOutputStream(bos);
			// Write everything but the consensus sequence.
			ProbeSetDisplayPlugin.collectAndWriteAnnotations(syms, false, seq, typeName, dos);
		} finally {
			GeneralUtils.safeClose(fos);
			GeneralUtils.safeClose(dos);
			GeneralUtils.safeClose(bos);
		}
	}

	/**
	 * Returns annotations for specific chromosome, sorted by comparator.
	 * Class cannot be generic, since symmetries could be UcscPslSyms or SeqSymmetries.
	 * @param syms - original list of annotations
	 * @param seq - specific chromosome
	 * @param comp - comparator
	 * @return - sorted list of annotations
	 */
	@SuppressWarnings("unchecked")
	public static List<SeqSymmetry> getSortedAnnotationsForChrom(List syms, BioSeq seq, Comparator comp) {
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>(10000);
		int symSize = syms.size();
		for (int i = 0; i < symSize; i++) {
			SeqSymmetry sym = (SeqSymmetry) syms.get(i);
			if (sym instanceof UcscPslSym) {
				// add the lines specifically with Target seq == seq.
				if (((UcscPslSym)sym).getTargetSeq() == seq) {
					results.add(sym);
				}
				continue;
			}
			// sym is instance of SeqSymmetry.
			if (sym.getSpan(seq) != null) {
				// add the lines specifically with seq.
				results.add(sym);
			}
		}

		Collections.sort(results, comp);

		return results;
	}


	/**
	 * Get "length" bytes starting at filePosStart
	 * @param file
	 * @param filePosStart
	 * @param length
	 * @return byte array 
	 */
	public static byte[] readBytesFromFile(File file, long filePosStart, int length) {
		byte[] contentsOnly = null;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
			if (file.length() < length) {
				System.out.println("WARNING: filesize " + file.length() + " was less than argument " + length);
				length = (int)file.length();
			}
			FileChannel fc = fis.getChannel();
			MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, filePosStart, length);
			contentsOnly = new byte[length];
			mbb.get(contentsOnly);
		} catch (IOException ex) {
			Logger.getLogger(IndexingUtils.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(fis);
		}
		return contentsOnly;
	}

	/**
	 * special case for link.psl files
	 * we need to append the track name, and the probesets
	 * @param indexesFileName
	 * @param annot_type
	 * @param bytes1 - consensus symmetries in a byte array
	 * @return Byte array of the input stream
	 * @throws IOException
	 */
	static ByteArrayInputStream readAdditionalLinkPSLIndex(
			String indexesFileName, String annot_type, byte[] bytes1) throws IOException {
		String secondIndexesFileName = indexesFileName.substring(0, indexesFileName.lastIndexOf(".link.psl"));
		secondIndexesFileName += ".link2.psl";

		File secondIndexesFile = new File(secondIndexesFileName);
		int bytes2Len = (int) secondIndexesFile.length();
		byte[] bytes0 = PSLParser.trackLine(annot_type, "Consensus Sequences").getBytes();
		// Determine overall length
		int bytes0Len = bytes0.length;
		int bytes1Len = bytes1.length;
		byte[] combinedByteArr = new byte[bytes0Len + bytes1Len + bytes2Len];

		// Copy in arrays.
		// copy 0th byte array (trackLine)
		System.arraycopy(bytes0, 0, combinedByteArr, 0, bytes0Len);
		bytes0 = null;	// now unused

		// copy 1st byte array (consensus syms)
		System.arraycopy(bytes1, 0, combinedByteArr, bytes0Len, bytes1Len);
		bytes1 = null;	// now unused

		// copy 2nd byte array (probeset syms)
		byte[] bytes2 = IndexingUtils.readBytesFromFile(secondIndexesFile, 0, bytes2Len);

		System.arraycopy(bytes2, 0, combinedByteArr, bytes0Len + bytes1Len, bytes2Len);
		bytes2 = null;	// now unused

		return new ByteArrayInputStream(combinedByteArr);
	}

	
	/**
	 * Find the maximum overlap given a range.
	 * @param overlapRange -- an array of length 2, with a start and end coordinate.
	 * @param outputRange -- an outputted array of length 2, with a start position (from min[] array) and an end position (from min[] array).
	 * @param min -- array of SORTED min points.
	 * @param max -- array of max points.
	 */
	public static void findMaxOverlap(int [] overlapRange, int [] outputRange, int [] min, int [] max) {
		// Find the first element with min at least equal to our start.
		int minStart = findMinimaGreaterOrEqual(min, overlapRange[0]);

		// Correct this estimate by backtracking to find any max values where start <= max.
		// (Otherwise, we will miss half-in intervals that have min < start, but start <= max.)
		int correctedMinStart = backtrackForHalfInIntervals(minStart, max, overlapRange[0]);

		outputRange[0] = correctedMinStart;

		// Find the last element with start(min) at most equal to our overlap end.
		// Since min is always <= max, this gives us a correct bound on our return values.
		int maxEnd = findMaximaLessOrEqual(min, overlapRange[1]);
		outputRange[1] = maxEnd;
	}


	/**
	 * Find minimum index of min[] array that is >= start range.
	 * @param min
	 * @param elt
	 * @return tempPos
	 */
	private static int findMinimaGreaterOrEqual(int[] min, int elt) {
		int tempPos = Arrays.binarySearch(min, elt);
		if (tempPos >= 0) {
			tempPos = backTrack(min, tempPos);
		} else {
			// This means the start element was not found in the array.  Translate back to "insertion point", which is:
			//the index of the first element greater than the key, or min.length, if all elements in the list are less than the specified key.
			tempPos = (-(tempPos + 1));
			// Don't go past array limit.
			tempPos = Math.min(min.length - 1, tempPos);
		}
		return tempPos;
	}

	/**
	 * Find maximum index of min[] array that is <= end range.
	 * @param min
	 * @param elt
	 * @return tempPos
	 */
	private static int findMaximaLessOrEqual(int[] min, int elt) {
		int tempPos = Arrays.binarySearch(min, elt);
		if (tempPos >= 0) {
			tempPos = forwardtrack(min, tempPos);
		} else {
			// This means the end element was not found in the array.  Translate back to "insertion point", which is:
			//the index of the first element greater than the key, or min.length, if all elements in the list are less than the specified key.
			tempPos = (-(tempPos + 1));
			// But here, we want to go the last element < the key.
			if (tempPos > 0) {
				tempPos--;
			}
			// Don't go past array limit (this case is probably impossible)
			tempPos = Math.min(min.length - 1, tempPos);
		}
		return tempPos;
	}

	/**
	 * backtrack if necessary
	 * (since binarySearch is not guaranteed to return lowest index of equal elements)
	 * @param arr
	 * @param pos
	 * @return lowest index of equal elements
	 */
	public static int backTrack(int[] arr, int pos) {
		while (pos > 0) {
			if (arr[pos - 1] == arr[pos]) {
				pos--;
			} else {
				break;
			}
		}
		return pos;
	}

	/**
	 * forward-track if necessary
	 * (since binarySearch is not guaranteed to return highest index of equal elements)
	 * @param arr
	 * @param pos
	 * @return highest index of equal elements
	 */
	public static int forwardtrack(int[] arr, int pos) {
		while (pos < arr.length - 1) {
			if (arr[pos + 1] == arr[pos]) {
				pos++;
			} else {
				break;
			}
		}
		return pos;
	}

	/**
	 * Backtrack to find any max values where start <= max <= end.
	 * @param minStart
	 * @param max
	 * @param overlapStart
	 * @return minVal
	 */
	private static int backtrackForHalfInIntervals(int minStart, int[] max, int overlapStart) {
		int minVal = minStart;
		for (int i=minStart-1;i>=0;i--) {
			if (max[i] >= overlapStart) {
				minVal = i;
			}
		}
		return minVal;
	}

	/**
	 * Index a graph.
	 * @param graphName
	 * @param pointCount
	 * @param x
	 * @param y
	 * @param w
	 * @return File
	 */
	public static File createIndexedFile(
			String graphName, int pointCount, int[] x, float[] y, int[] w) {
		File bufVal = null;
		DataOutputStream dos = null;
		try {
			// create indexed file.
			
			if (graphName.length() < 3) {
				graphName += "___";
				// fix for Java error with short names
			}
			bufVal = File.createTempFile(URLEncoder.encode(graphName, "UTF-8"), "idx");
			bufVal.deleteOnExit(); // Delete this file when shutting down.
			dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(bufVal)));
			for (int i = 0; i < pointCount; i++) {
				dos.writeInt(x[i]);
				dos.writeFloat(y[i]);
				dos.writeInt(w == null ? 1 : w[i]); // width of 1 is a single point.
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(dos);
		}
		return bufVal;
	}

}
