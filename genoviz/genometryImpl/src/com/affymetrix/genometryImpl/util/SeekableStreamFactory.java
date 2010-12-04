package com.affymetrix.genometryImpl.util;

import java.io.IOException;
import java.net.URI;
import net.sf.samtools.util.SeekableFileStream;
import net.sf.samtools.util.SeekableStream;

public class SeekableStreamFactory {

	public static SeekableStream getStreamFor(URI path) throws IOException {


		SeekableStream is = null;
		if (path.getScheme().toLowerCase().startsWith("http:") || path.getScheme().toLowerCase().startsWith("https:")) {
			is = new SeekableHTTPStream(path.toURL());
		} else if (path.getScheme().toLowerCase().startsWith("ftp:")) {
			//is = new SeekableFTPStream(new URL(path));
		} else {
			is = new SeekableFileStream(LocalUrlCacher.convertURIToFile(path));
		}
		return is;

	}
}
