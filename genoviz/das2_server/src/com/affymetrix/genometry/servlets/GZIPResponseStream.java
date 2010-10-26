/*
 * Copyright 2003 Jayson Falkner (jayson@jspinsider.com)
 * This code is from "Servlets and JavaServer pages; the J2EE Web Tier",
 * http://www.jspbook.com. You may freely use the code both commercially
 * and non-commercially. If you like the code, please pick up a copy of
 * the book and help support the authors, development of more free code,
 * and the JSP/Servlet/J2EE community.
 */
package com.affymetrix.genometry.servlets;

import java.io.*;
import java.util.zip.GZIPOutputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;


public final class GZIPResponseStream extends ServletOutputStream {
	private ByteArrayOutputStream baos = null;
	private GZIPOutputStream gzipstream = null;
	private boolean closed = false;
	private HttpServletResponse response = null;
	private ServletOutputStream output = null;

	public GZIPResponseStream(HttpServletResponse response) throws IOException {
		super();
		closed = false;
		this.response = response;
		this.output = response.getOutputStream();
		baos = new ByteArrayOutputStream();
		gzipstream = new GZIPOutputStream(baos);
	}

	@Override
	public void close() throws IOException {
		// Unnecessary to throw an exception in this case.  
		// Note that other OutputStream classes do not throw an exception when a stream is closed multiple times.
		/*if (closed) {
			throw new IOException("This output stream has already been closed");
		}*/
		if (closed) {
			return;
		}
		gzipstream.finish();

		byte[] bytes = baos.toByteArray();


		response.addHeader("Content-Length", 
				Integer.toString(bytes.length)); 
		response.addHeader("Content-Encoding", "gzip");
		output.write(bytes);
		output.flush();
		output.close();
		closed = true;
	}

	@Override
	public void flush() throws IOException {
		if (closed) {
			throw new IOException("Cannot flush a closed output stream");
		}
		gzipstream.flush();
	}

	public void write(int b) throws IOException {
		if (closed) {
			throw new IOException("Cannot write to a closed output stream");
		}
		gzipstream.write((byte)b);
	}

	@Override
	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException {
		if (closed) {
			throw new IOException("Cannot write to a closed output stream");
		}
		gzipstream.write(b, off, len);
	}

	public boolean closed() {
		return (this.closed);
	}

	public void reset() {
		//noop
	}
}
