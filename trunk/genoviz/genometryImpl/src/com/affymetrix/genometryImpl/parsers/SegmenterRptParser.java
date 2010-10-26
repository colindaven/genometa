package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.SingletonSymWithProps;
import com.affymetrix.genometryImpl.SymWithProps;
import java.io.*;
import java.util.*;
import java.util.regex.*;

/**
 *  Parses tab-delimited output data from the Copy Number "Segmenter".
 *  The file extension is either {@link #CN_REGION_FILE_EXT} or
 *  {@link #LOH_REGION_FILE_EXT}.
 *
 *  (This was based on the TabDelimitedParser, but then specialized.)
 */
public final class SegmenterRptParser {
	public static final String CN_REGION_FILE_EXT = "cn_segments";
	public static final String LOH_REGION_FILE_EXT = "loh_segments";

	/** The name of a property key used to store a Map<String,String> containing
	 *  the file's header.  Outside classes rarely need to use this; call
	 *  {@link #getHeaderValue(String, SymWithProps)} instead.
	 */
	private static final String HEADER_PROP_KEY_NAME = "SegmenterFileHeader";

	private int chromosome_col;
	private int start_col;
	private int end_col;     // should need only end_col or length_col, not both
	private int length_col;  // should need only end_col or length_col, not both
	private int strand_col;  // column to use for determining strand
	private int cn_change_col;  // column to use for determining cn change type ("dup", "del", etc.)
	private int file_col;  // column to use for sample file name


	// if makeProps, then each column (other than start, end, length, group) will become a
	//    property in the SymWithProps that is generated
	private final boolean make_props;

	private final boolean use_length;
	private final boolean use_strand;
	private final boolean has_column_names_header_line;

	private static final Pattern line_splitter = Pattern.compile("\t");


	// Sample header
	//#MinimumMarkersInSegment=5
	//#MinimumGenomicSegmentSize=100
	//#MaximumOverlapOfCNVWithSNP=100
	//#State=CN
	//#Arrayset=GenomeWideSNP_6
	//#genome=hg18
	//#gender=Female
	//#gender_threshold_test=In Bounds
	//#madPairDiff=0.15253

	//Possible values as below

	//#gender=Female, Male, Unknown
	//#gender_threshold_test=In Bounds, Out of Bounds



	private static final Pattern headerPattern = Pattern.compile("^#(.*)=(.*)$");
	private final Map<String,String> headerMap = new HashMap<String,String>();

	private static final List<String> integerColumnNames = Arrays.asList(
			"Copy Number",
			"Size(kb)",
			"#Markers", "Avg_DistBetweenMarkers(kb)",
			"Start_Linear_Pos", "End_Linear_Position"
			);

	public static boolean isCnRegionsFilename(String s) {
		return (s != null && s.toLowerCase().endsWith("." + CN_REGION_FILE_EXT.toLowerCase()));
	}

	public static boolean isLohRegionsFilename(String s) {
		return (s != null && s.toLowerCase().endsWith("." + LOH_REGION_FILE_EXT.toLowerCase()));
	}

	public SegmenterRptParser() {
		this(true, true);
	}



	public SegmenterRptParser(boolean props, boolean addToIndex) {

		//Sample	Copy Number State	Loss/Gain	Chr
		//Cytoband_Start_Pos	Cytoband_End_Pos	Size(kb)	#Markers	
		//Avg_DistBetweenMarkers(kb)	%CNV_Overlap	
		//Start_Linear_Pos	End_Linear_Position	Start_Marker	End_Marker	
		//CNV_Annotation
		this.file_col = 0;
		this.chromosome_col = 3;
		this.start_col = 10;
		this.end_col = 11;
		this.cn_change_col = 2;

		this.length_col = -1;
		this.strand_col = -1;

		this.has_column_names_header_line = true;

		this.use_length = (this.length_col >= 0);
		this.use_strand = (this.strand_col >= 0);

		this.make_props = props;
	}


	void parseHeaderLine(String line) {
		Matcher m = headerPattern.matcher(line);
		if (m.matches()) {
			String key = m.group(1);
			String val = m.group(2);
			headerMap.put(key, val);
		}
	}

	public void parse(InputStream istr, String default_type, AnnotatedSeqGroup seq_group) {

		List<String> col_names = null;

		try {
			InputStreamReader asr = new InputStreamReader(istr);
			BufferedReader br = new BufferedReader(asr);


			// skip any comment lines
			String line = br.readLine();
			while (line != null && (line.startsWith("#") || line.startsWith("[")) && (!Thread.currentThread().isInterrupted())) {
				parseHeaderLine(line);
				line = br.readLine();
			}

			if (line == null) {
				return;
			}

			// read header
			if (has_column_names_header_line) {
				String[] cols = line_splitter.split(line);
				col_names = new ArrayList<String>(cols.length);
				for (int i=0; i<cols.length && (!Thread.currentThread().isInterrupted()); i++) {
					col_names.add(cols[i]);
				}
			}

			// skip any other comment lines
			line = br.readLine();
			while (line != null && (line.startsWith("#") || line.startsWith("[")) && Thread.currentThread().isInterrupted()) {
				line = br.readLine();
			}

			// read data
			while (line != null && (!Thread.currentThread().isInterrupted())) {

				String[] cols = line_splitter.split(line);
				if (cols.length <= 0) { continue; }

				int start = Integer.parseInt(cols[start_col]);
				int end;
				if (use_length) {
					int length = Integer.parseInt(cols[length_col]);
					if (use_strand) {
						String strand = cols[strand_col];
						//	    boolean revstrand = strand.equals("-");
						if (strand.equals("-")) {
							end = start - length;
						}
						else {
							end = start + length;
						}
					} else {
						end = start + length;
					}
				} else {
					end = Integer.parseInt(cols[end_col]);
				}

				String chromName = cols[chromosome_col];
				if (! chromName.startsWith("chr")) {
					// Add prefix "chr" to chromosome name.  This is important to make sure there are not
					// some chromosomes named "1" and some others named "chr1".
					chromName = "chr" + chromName;
				}
				BioSeq seq = seq_group.getSeq(chromName);
				if (seq == null) {
					seq = seq_group.addSeq(chromName, 0);
				}

				if (seq.getLength() < end) {
					seq.setLength(end);
				}
				if (seq.getLength() < start) {
					seq.setLength(start);
				}

				String type = cols[file_col];
				if (type == null || type.trim().length() == 0) {
					type = default_type;
				}

				String change_type = cols[cn_change_col];
				String THE_METHOD = default_type; // should be file name with proper extension

				SingletonSymWithProps sym = new SingletonSymWithProps(start, end, seq);
				sym.setProperty("method", THE_METHOD); // typically the value of the "file" column
				String id = change_type + " " + seq.getID() + ":" + start + "-" + end;
				sym.setProperty("id", id);

				if (make_props) {
					for (int i=0; i<cols.length && i<col_names.size() && (!Thread.currentThread().isInterrupted()); i++) {
						String name = col_names.get(i);
						String stringVal = cols[i];
						if (integerColumnNames.contains(name)) {
							try {
								Long intVal = Long.parseLong(stringVal);
								sym.setProperty(name, intVal);
							} catch (Exception e) {
								sym.setProperty(name, stringVal);
							}
						} else {
							sym.setProperty(name, stringVal);
						}
					}
				}

				sym.setProperty(HEADER_PROP_KEY_NAME, headerMap);

				seq.addAnnotation(sym);
				seq_group.addToIndex(id, sym);

				line = br.readLine();
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	public static String getHeaderValue(String key, SymWithProps sym) {
		@SuppressWarnings("unchecked")
		Map<String,String> map = (Map<String,String>) sym.getProperty(HEADER_PROP_KEY_NAME);
		if (map != null) {
			return map.get(key);
		}
		return null;
	}
}
