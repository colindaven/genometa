package com.affymetrix.igb.view.external;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;

/**
 * Parses a html page for the image URL
 *
 * @author Ido M. Tamir
 */
public interface URLFinder {

	public String findUrl(BufferedReader reader, URL url) throws IOException;
}
