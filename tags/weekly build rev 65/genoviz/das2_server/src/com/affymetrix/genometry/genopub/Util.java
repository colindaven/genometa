package com.affymetrix.genometry.genopub;

import javax.servlet.http.HttpServletRequest;

import java.io.*;
import java.sql.Date;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;

public class Util {

	private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy");

	
	private static final double    KB = Math.pow(2, 10);
	
	public static Integer getIntegerParameter(HttpServletRequest req, String parameterName) {
		if (req.getParameter(parameterName) != null && !req.getParameter(parameterName).equals("")) {
			return new Integer(req.getParameter(parameterName));
		} else{
			return null;
		}
	}
	
	public static Date getDateParameter(HttpServletRequest req, String parameterName) {
		if (req.getParameter(parameterName) != null && !req.getParameter(parameterName).equals("")) {
			try {
				return parseDate(req.getParameter(parameterName));				
			} catch (ParseException e) {
				return null;
			}
		} else{
			return null;
		}
	}
	
	public static String getFlagParameter(HttpServletRequest req, String parameterName) {
		if (req.getParameter(parameterName) != null && !req.getParameter(parameterName).equals("")) {
			return req.getParameter(parameterName);
		} else{
			return "Y";
		}
	}
	
	public static boolean fileHasSegmentName(String fileName, GenomeVersion genomeVersion) {
		// For now, just skip this check if segments haven't beens specified
		// for this genome version
		if (genomeVersion.getSegments() == null || genomeVersion.getSegments().size() == 0) {
			return true;
		}
		
		boolean isValid = false;
		for (Iterator i = genomeVersion.getSegments().iterator(); i.hasNext();) {
			Segment segment = (Segment)i.next();
			String fileParts[] = fileName.split("\\.");
			if (fileParts.length == 2) {
				if (fileParts[0].equalsIgnoreCase(segment.getName())) {
					isValid = true;
					break;
				}
			}
		}
		return isValid;
	}
	
	public static boolean isValidAnnotationFileType(String fileName) {		
		boolean isValid = false;
		for (int x=0; x < Constants.ANNOTATION_FILE_EXTENSIONS.length; x++) {
			if (fileName.toUpperCase().endsWith(Constants.ANNOTATION_FILE_EXTENSIONS[x].toUpperCase())) {
				isValid = true;
				break;
			}
		}
		return isValid;
	}
	
	public static boolean isValidSequenceFileType(String fileName) {		
		boolean isValid = false;
		for (int x=0; x < Constants.SEQUENCE_FILE_EXTENSIONS.length; x++) {
			if (fileName.toUpperCase().endsWith(Constants.SEQUENCE_FILE_EXTENSIONS[x].toUpperCase())) {
				isValid = true;
				break;
			}
		}
		return isValid;
	}
	
	
	public static String formatDate(Date date) {
		return dateFormat.format(date);
	}
	
	public static Date parseDate(String date) throws ParseException {
		return new Date(dateFormat.parse(date).getTime());
	}
    
	public static long getKilobytes(long bytes) {
		long kb =  Math.round(bytes / KB);
		if (kb == 0) {
			kb = 1;
		}
		return kb;
	}
	
	public static String removeHTMLTags(String buf) {
		if (buf  != null) {
			buf = buf.replaceAll("<(.|\n)+?>", " ");
			buf = Util.escapeHTML(buf);
		}
		return buf;
	}
	
	public static String escapeHTML(String buf) {
		if (buf != null) {
			buf = buf.replaceAll("&", "&amp;");
			buf = buf.replaceAll("<", "&lt;");
			buf = buf.replaceAll(">", "&gt;");			
			buf = buf.replaceAll("\"", "'");			
		}
		
		return buf;
	}

	public static boolean tooManyLines(File file) throws IOException{
		String lcName = file.getName().toLowerCase();
		// is it a text file to check
		for (int i=0; i< Constants.FILE_EXTENSIONS_TO_CHECK_SIZE_BEFORE_UPLOADING.length; i++){
			if (lcName.endsWith(Constants.FILE_EXTENSIONS_TO_CHECK_SIZE_BEFORE_UPLOADING[i])){
				int counter = 0;
				BufferedReader in = new BufferedReader (new FileReader(file));
				while (in.readLine() != null){
					if (counter > Constants.MAXIMUM_NUMBER_TEXT_FILE_LINES) {
						in.close();
						return true;
					}
					else counter++;
				}
				in.close();
				return false;
			}
		}
		return false;
	}
}
