package com.affymetrix.genometryImpl.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An implementation of InputStream that caches the InputStream to a
 * File on disk.
 *
 * @author sgblanch
 * @version $Id: CachingInputStream.java 6740 2010-08-25 13:33:47Z sgblanch $
 */
public class CachingInputStream extends FilterInputStream {

	private OutputStream outputStream = null;
	private String url;

	public CachingInputStream(InputStream is, File cacheFile, String url) {
		super(is);
		try {
			this.outputStream = new BufferedOutputStream(new FileOutputStream(cacheFile));
			this.url = url;
		} catch (FileNotFoundException e) {
			this.fail(e);
		}
	}

	private synchronized void fail(Throwable e) {
		StackTraceElement ste = (new Exception()).getStackTrace()[1];

		Logger.getLogger(CachingInputStream.class.getName()).logp(Level.SEVERE, ste.getClassName(), ste.getMethodName(), "Caching of " + url + " failed", e);
		GeneralUtils.safeClose(outputStream);
		outputStream = null;
		LocalUrlCacher.invalidateCacheFile(url);

	}

	@Override
	public int read() throws IOException {
		byte[] b = new byte[1];
		int ret = this.read(b, 0, b.length);
		return (ret == -1 ? ret : ((int) b[0] & 0x00FF));
	}

	@Override
	public int read(byte[] b) throws IOException {
		return this.read(b, 0, b.length);
	}

	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		try {
			int bytesRead = super.read(b, off, len);
			if (bytesRead > 0 && this.outputStream != null) {
				this.outputStream.write(b, off, bytesRead);
			}

			return bytesRead;
		} catch (IOException e) {
			this.fail(e);
			throw e;
		}
	}

	/**
	 * Skip bytes in the OutputStream.  This implementation should
	 * always skip the requested number of bytes unless an EOF or error
	 * is encountered.
	 *
	 * @param n number of bytes to skip.
	 * @return The number of bytes actually skipped
	 * @throws IOException
	 */
	@Override
	public long skip(long n) throws IOException {
		byte[] b = new byte[8192];
		long bytesSkipped = 0;
		long bytesRemaining = n;
		int bytesRead = 0;

		try {
			while (bytesSkipped < n) {
				bytesRemaining = n - bytesSkipped;
				bytesRead = read(b, 0, (bytesRemaining < b.length) ? (int) (bytesRemaining) : b.length);
				if (bytesRead > 0) {
					bytesSkipped -= bytesRead;
				} else {
					return bytesSkipped;
				}
			}
		} catch (IOException e) {
			this.fail(e);
			throw e;
		}

		return bytesSkipped;
	}

	/**
	 * Close the InputStream.  This implementation will consume all
	 * remaining bytes on the InputStream before closing.  This ensures
	 * that the original InputStream is cached in entirety.  This method
	 * is synchronized to prevent threading errors.
	 *
	 * @throws IOException
	 */
	@Override
	public synchronized void close() throws IOException {
		try {
			if (outputStream != null) {
				try {
					/* consume the entire stream before closing */
					this.skip(Long.MAX_VALUE);
				} catch (IOException e) {}

				GeneralUtils.safeClose(outputStream);
				outputStream = null;
			}

			super.close();
		} catch (IOException e) {
			this.fail(e);
			throw e;
		}
	}

	/**
	 * Does nothing.  This InputStream does not support marks.
	 *
	 * @param readlimit
	 */
	@Override
	public synchronized void mark(int readlimit) { }

	/**
	 * Returns false.  This InputStream does not support marks.
	 *
	 * @return false since this InputStream does not support marks
	 */
	@Override
	public boolean markSupported() {
		return false;
	}
}
