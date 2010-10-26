package com.affymetrix.genoviz.parser;

import com.affymetrix.genoviz.datamodel.BaseConfidence;
import com.affymetrix.genoviz.datamodel.ReadConfidence;
import com.affymetrix.genoviz.util.GeneralUtils;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * parses output from FASTQ.
 * @author John Nicol
 */
public final class FASTQParser {

	/**
	 * constructs a ReadConfidence data model
	 *
	 * @param fastqURL
	 */
	public static ReadConfidence parseFiles(URL fastqURL) {
		InputStream fastqIn = null;
		BufferedReader fastqDataIn = null;

		try {
			fastqIn = fastqURL.openStream();
			fastqDataIn = new BufferedReader(new InputStreamReader(fastqIn));
		} catch (MalformedURLException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}

		ReadConfidence readConf = new ReadConfidence();

		try {
			readFile(fastqDataIn, readConf);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			GeneralUtils.safeClose(fastqIn);
			GeneralUtils.safeClose(fastqDataIn);
		}

		return readConf;

	}

	private static void readFile(BufferedReader fastqDataIn, ReadConfidence readConf) throws IOException {
		boolean FASTAline = true; // The line is either FASTA or confidence scores.
		boolean Illumina = false;	// is it in the Illumina/Solexa format, instead of the Phred format?
		String bases = null;
		String confidence = null;
		String fastqLine;
		while ((fastqLine = fastqDataIn.readLine()) != null) {
			// Skip comment lines -- but look for Illumina identifier
			if (fastqLine.charAt(0) == '@') {
				if (!Illumina && fastqLine.toLowerCase().startsWith("@hwusi-eas100r")) {
					// Illumina/Solexa format.
					Illumina = true;
					//System.out.println("In Illumina/Solexa format... assuming version 1.0");
				}
				continue;
			}
			if (fastqLine.charAt(0) == '+') {
				continue;
			}

			// Parse data
			if (FASTAline) {
				bases = fastqLine;
			} else {
				if (bases == null) {
					System.out.println("Couldn't find bases for confidence -- stopping parsing.");
					break;
				}
				if (bases.length() != fastqLine.length()) {
					System.out.println("Bases length was not equal to confidence length -- stopping parsing.");
					break;
				}
				addLineToReadConfidence(fastqLine, bases, confidence, readConf, Illumina);
				bases = null; // Sanity check
			}
			FASTAline = !FASTAline;
		}
	}

	private static void addLineToReadConfidence(String fastqLine, String bases, String confidence, ReadConfidence readConf, boolean Illumina) {
		confidence = fastqLine;
		int baseLength = bases.length(); // for performance
		for (int i = 0; i < baseLength; i++) {
			char base = bases.charAt(i);
			char conf = confidence.charAt(i);
			BaseConfidence baseConf = null;
			//  TODO: conversions for Illumina/Solexa.
			// See http://en.wikipedia.org/wiki/FASTQ_format and http://maq.sourceforge.net/fastq.shtml

			//if (Illumina) {
				// Illumina/Solexa format (assuming 1.0)

				// TODO: if it was 1.3, the conversion would be different.
			//} else {
				// Sanger/Phred format
				baseConf = new BaseConfidence(base, (int) conf);
			//}
			readConf.addBaseConfidence(baseConf);
		}
	}
}
