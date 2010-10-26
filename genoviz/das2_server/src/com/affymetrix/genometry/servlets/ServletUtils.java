package com.affymetrix.genometry.servlets;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.parsers.FastaParser;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.parsers.TwoBitParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

final class ServletUtils {

	static void retrieveRAW(
			List<String> ranges, SeqSpan span, String sequence_directory, String seqname, HttpServletResponse response, HttpServletRequest request)
			throws IOException {

		String file_name = sequence_directory + seqname + ".bnib";
		File seqfile = new File(file_name);
		FileInputStream fis = null;

		if (seqfile.exists()) {
			response.setContentType("text/raw"); // set text type
			try {
				fis = new FileInputStream(seqfile);
				if (!ranges.isEmpty()) {
					int spanStart = 0, spanEnd = 0;
					spanStart = span.getStart();
					spanEnd = span.getEnd();
					NibbleResiduesParser.parse(fis, spanStart, spanEnd, response.getOutputStream());
				} else {
					NibbleResiduesParser.parse(fis, response.getOutputStream());
				}
			} finally {
				GeneralUtils.safeClose(fis);
			}
			return;
		}

		file_name = sequence_directory + seqname + ".2bit";
		seqfile = new File(file_name);
		if (seqfile.exists()) {
			response.setContentType("text/raw"); // set text type

			if (!ranges.isEmpty()) {
				int spanStart = 0, spanEnd = 0;
				spanStart = span.getStart();
				spanEnd = span.getEnd();
				TwoBitParser.parse(seqfile.toURI(), spanStart, spanEnd, response.getOutputStream());
			} else {
				TwoBitParser.parse(seqfile.toURI(), response.getOutputStream());
			}
			return;
		}

		PrintWriter pw = response.getWriter();
		pw.println("File not found: " + file_name);
		pw.println("This DAS/2 server cannot currently handle request:    ");
		pw.println(request.getRequestURL().toString());

	}

	static void retrieveBNIB(
			String sequence_directory, String seqname, HttpServletResponse response, HttpServletRequest request) throws IOException {

		String file_name = sequence_directory + seqname + ".bnib";
		File seqfile = new File(file_name);

		if (seqfile.exists()) {
			response.setContentType(NibbleResiduesParser.getMimeType()); // set bnib format mime type
			BufferedInputStream in = null;
			BufferedOutputStream out = null;
			try {
				in = new BufferedInputStream(new FileInputStream(seqfile));
				out = new BufferedOutputStream(response.getOutputStream());
				int c;

				while ((c = in.read()) != -1) {
					out.write(c);
				}

			} finally {
				GeneralUtils.safeClose(in);
				GeneralUtils.safeClose(out);
			}
			return;
		}

		PrintWriter pw = response.getWriter();
		pw.println("File not found: " + file_name);
		pw.println("This DAS/2 server cannot currently handle request:    ");
		pw.println(request.getRequestURL().toString());
	}


	/**
	 *Retrieve sequence from FASTA file.  Please note restrictions in FASTA parser for DAS/2 serving.
	 * @param ranges
	 * @param span
	 * @param sequence_directory
	 * @param organism_name
	 * @param seqname
	 * @param format
	 * @param response
	 * @param request
	 * @throws java.io.IOException
	 * @deprecated
	 */
	@Deprecated
	static void retrieveFASTA(
			List<String> ranges, SeqSpan span, String sequence_directory, String organism_name, String seqname, HttpServletResponse response, HttpServletRequest request)
	throws IOException {
		String file_name = sequence_directory + seqname + ".fa";
		File seqfile = new File(file_name);
		if (!seqfile.exists()) {
			System.out.println("seq request mapping to nonexistent file: " + file_name);
			PrintWriter pw = response.getWriter();
			pw.println("File not found: " + file_name);
			pw.println("This DAS/2 server cannot currently handle request:    ");
			pw.println(request.getRequestURL().toString());
			return;
		}

		// Determine spanStart and spanEnd.  If it's an unranged query, then just make SpanEnd no larger than the filesize.
		int spanStart = 0, spanEnd = 0;
		if (ranges.isEmpty()) {
			if (seqfile.length() > (long) Integer.MAX_VALUE) {
				spanEnd = Integer.MAX_VALUE;
			} else {
				spanEnd = (int) seqfile.length();
			}
		} else {
			spanStart = span.getStart();
			spanEnd = span.getEnd();
		}

		response.setContentType(FastaParser.getMimeType());
		byte[] buf = FastaParser.readFASTA(seqfile, spanStart, spanEnd);
		byte[] header = FastaParser.generateNewHeader(seqname, organism_name, spanStart, spanEnd);
		OutputFormattedFasta(buf, header, response.getOutputStream());
	}

	// Write a formatted fasta file out to the ServletOutputStream.
	private static void OutputFormattedFasta(byte[] buf, byte[] header, ServletOutputStream sos)
	throws IOException, IOException, IllegalArgumentException {
		if (buf == null) {
			return;
		}

		DataOutputStream dos = new DataOutputStream(sos);
		try {
			dos.write(header, 0, header.length);

			byte[] newlineBuf = new byte[1];
			newlineBuf[0] = '\n';

			// Write out Fasta sequence, adding a newline after every LINELENGTH characters.
			int lines = buf.length / FastaParser.LINELENGTH;
			for (int i = 0; i < lines; i++) {
				dos.write(buf, i * FastaParser.LINELENGTH, FastaParser.LINELENGTH);
				dos.write(newlineBuf, 0, 1);
			}
			if (buf.length % FastaParser.LINELENGTH > 0) {
				// Write remainder of last line out to buffer
				dos.write(buf, lines * FastaParser.LINELENGTH, buf.length % FastaParser.LINELENGTH);
				dos.write(newlineBuf, 0, 1);
			}
		} finally {
			GeneralUtils.safeClose(dos);
		}
	}
}
