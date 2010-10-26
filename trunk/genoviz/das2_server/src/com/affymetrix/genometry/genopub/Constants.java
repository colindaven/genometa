package com.affymetrix.genometry.genopub;

public class Constants {
	public static final String GENOMETRY_MODE         = "genometry_mode";

	public static final String GENOMETRY_MODE_GENOPUB = "genopub";
	public static final String GENOMETRY_MODE_CLASSIC = "classic";

	public static final String GENOMETRY_SERVER_DIR_CLASSIC  = "genometry_server_dir";
	public static final String GENOMETRY_SERVER_DIR_GENOPUB  = "genometry_server_dir_genopub";

	public static final String SEQUENCE_DIR_PREFIX    = "SEQ";

	public static final int MAXIMUM_NUMBER_TEXT_FILE_LINES = 10000;

	static final String[] ANNOTATION_FILE_EXTENSIONS = new String[] 
	                                                               {
		".bar",
		".bed",
		".bgn",
		".bgr",
		".bps",
		".bp1",
		".bp2",
		".brs",
		".cyt",
		".ead",
		".gff", 
		".gtf",
		".psl",
		".useq",
		".bulkUpload"
	                                                               };

	static final String[] FILE_EXTENSIONS_TO_CHECK_SIZE_BEFORE_UPLOADING = new String[] {
		".bed", 
		".bgn", 
		".gff", 
		".gtf", 
		".psl", 
	};

	static final String[] SEQUENCE_FILE_EXTENSIONS = new String[] 
	                                                             {
		".bnib", 
		".fasta",
	                                                             };



}
