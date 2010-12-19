/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.affymetrix.igb.util.csv;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author wieding-
 *
 * Format according to:
 * rfc4180: Common Format and MIME Type for Comma-Separated Values (CSV) Files
 */
public class CsvFile {

	private FileOutputStream ofs;
	private BufferedOutputStream os;
	private final String LINEEND = "\r\n";
	private final String RECORD_DELIMITER = ",";

	public CsvFile(String file) {
		try {
			ofs = new FileOutputStream(file);
		} catch (FileNotFoundException ex) {
			Logger.getLogger(CsvFile.class.getName()).log(Level.SEVERE, "Could not open csv file '" + file + "'.", ex);
		}

		os = new BufferedOutputStream(ofs);
	}

	public void writeLine(Object[] cols) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < cols.length; ++i) {
			sb.append(cols[i].toString());

			if (i < cols.length - 1) {
				sb.append(';');
			}
		}

		this.writeLine(sb.toString());
	}

	public void close() {
		try {
			os.close();
			ofs.close();
		} catch (IOException ex) {
			Logger.getLogger(CsvFile.class.getName()).log(Level.SEVERE, "Error while closing the csv file.", ex);
		}
	}

	private void writeLine(String s) {
		try {
			os.write(s.getBytes());
			os.write(LINEEND.getBytes());
		} catch (IOException ex) {
			Logger.getLogger(CsvFile.class.getName()).log(Level.SEVERE, "Could not write to CSV file.", ex);
		}
	}
}
