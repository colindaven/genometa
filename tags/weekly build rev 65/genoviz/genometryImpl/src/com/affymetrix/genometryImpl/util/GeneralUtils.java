package com.affymetrix.genometryImpl.util;

import com.affymetrix.genometryImpl.general.GenericFeature;
import java.awt.Desktop;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;
import javax.swing.ImageIcon;
import net.sf.image4j.codec.ico.ICODecoder;
import net.sf.image4j.codec.ico.ICOImage;

public final class GeneralUtils {
	public static final String UTF8 = "UTF-8";

	/**
	 * Safely close a Closeable object.  If it doesn't exist, return.
	 */
	public static <S extends Closeable> void safeClose(S s) {
		if (s == null) {
			return;
		}
		try {
			s.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/** A list of all the compression-type file endings that this
	 *  object knows how to decompress.
	 *  This list is all lower-case, but should be treated as case-insensitive.
	 */
	public static final String[] compression_endings =
	{".z", ".gzip", ".gz", ".zip"};


	/** Returns the file name with all {@link #compression_endings} stripped-off. */
	public static String stripEndings(String name) {
		for (int i=0; i<compression_endings.length; i++) {
			String ending = compression_endings[i].toLowerCase();
			if (name.toLowerCase().endsWith(ending)) {
				String stripped_name = name.substring(0, name.lastIndexOf('.'));
				return stripEndings(stripped_name);
			}
		}
		return name;
	}

	/** Returns a BufferedInputStream, possibly wrapped by a
	 *  GZIPInputStream, or ZipInputStream,
	 *  as appropriate based on the name of the given file.
	 *  @param f a file
	 *  @param sb a StringBuffer used to pass back the name of the file
	 *            with the compression endings (like ".zip") removed,
	 *            and converted to lower case.
	 */
	public static InputStream getInputStream(File f, StringBuffer sb) throws
		FileNotFoundException, IOException {

			String infile_name = f.getName();
			BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f));
			InputStream isr = unzipStream(bis, infile_name, sb);
			return isr;
		}

	/**
	 *  Takes a named input stream and returns another one which is
	 *  an instance of GZIPInputStream or ZipInputStream if the given name
	 *  ends with one of the {@link #compression_endings} (case insensitive).
	 *  (If the stream name does not have one of those endings, the original
	 *  InputStream is simply returned unchanged.)
	 *  The name with the compression ending stripped off (and converted to lower case) is
	 *  returned in the value of stripped_name.
	 */
	public static InputStream unzipStream(InputStream istr, String stream_name,
			StringBuffer stripped_name)
			throws IOException {
		String lc_stream_name = stream_name.toLowerCase();
		if (lc_stream_name.endsWith(".gz") || lc_stream_name.endsWith(".gzip") ||
				lc_stream_name.endsWith(".z")) {
			GZIPInputStream gzstr = new GZIPInputStream(istr);
			String new_name = stream_name.substring(0, stream_name.lastIndexOf('.'));
			return unzipStream(gzstr, new_name, stripped_name);
		} else if (stream_name.endsWith(".zip")) {
			ZipInputStream zstr = new ZipInputStream(istr);
			zstr.getNextEntry();
			String new_name = stream_name.substring(0, stream_name.lastIndexOf('.'));
			return unzipStream(zstr, new_name, stripped_name);
		}
		stripped_name.append(stream_name);
		return istr;
	}

	public static String getUnzippedName(String stream_name) {
		String lc_stream_name = stream_name.toLowerCase();
		if (lc_stream_name.endsWith(".gz") || lc_stream_name.endsWith(".gzip") ||
				lc_stream_name.endsWith(".z")) {
			return stream_name.substring(0, stream_name.lastIndexOf('.'));
		} else if (stream_name.endsWith(".zip")) {
			return stream_name.substring(0, stream_name.lastIndexOf('.'));
		}
		return stream_name;
	}

	/**
	 * Fix several potential problems in URL names.
	 * @param streamName
	 * @return
	 */
	public static String convertStreamNameToValidURLName(String streamName) {
		int httpIndex = streamName.indexOf("http:");
		if (httpIndex > -1) {
			streamName = streamName.substring(httpIndex + 5);	// strip off initial characters including http:
		}

		// strip off initial "/" characters.  There may be one, or multiple.
		int streamNameLen = streamName.length();
		for (int i=0;i<streamNameLen;i++) {
			if (streamName.startsWith("/")) {
				streamName = streamName.substring(1);
			}
		}

		// strip off final "/" character, if it exists.
		if (streamName.endsWith("/")) {
			streamName = streamName.substring(0,streamName.length()-1);
		}
		return "http://" + streamName;

	}

	/**
	 * @param istr	- input stream
	 * @param streamName
	 * @return File with data from stream.
	 */
	public static File convertStreamToFile(InputStream istr, String streamName) {
		// Output the InputStream to a temporary file, and read that as a FileInputStream.
		OutputStream out = null;
		FileInputStream fis = null;
		try {
			String unzippedStreamName = stripEndings(streamName);
			String extension = ParserController.getExtension(unzippedStreamName);
			File f = File.createTempFile(
					unzippedStreamName,extension);
			f.deleteOnExit();	// This is only a temporary file!  Delete when the app exits.
			out = new FileOutputStream(f);
			int read = 0;
			byte[] bytes = new byte[1024];
			while ((read = istr.read(bytes)) != -1) {
				out.write(bytes, 0, read);
			}
			return f;
		}  catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
		finally {
			GeneralUtils.safeClose(out);
			GeneralUtils.safeClose(fis);
		}
	}



	/**
	 * Get a favicon from the URL.
	 * @param iconString
	 * @return null
	 */
	public static ImageIcon determineFriendlyIcon(String iconString) {
		// Step 1. getting IconURL
		URL iconURL = null;
		try {
			iconURL = new URL(iconString);
		} catch (Exception ex) {
			// Ignore an exception here, since this is only for making a pretty UI.
		}
		if (iconURL == null) {
			return null;
		}

		// Step 2. loading the icon and find a proper icon
		BufferedImage icon = null;
		URLConnection conn = null;
		List<ICOImage> icoImages = null;
		try {
			conn = iconURL.openConnection();
			conn.setConnectTimeout(5000);	// only wait a few seconds, since this isn't critical
			conn.setReadTimeout(5000);		// only wait a few seconds, since this isn't critical
			conn.connect();
			if (conn.getInputStream() == null) {
				return null;
			}
			icoImages = ICODecoder.readExt(conn.getInputStream());
		}
		catch (Exception ex) {
			return null;
		}
		
		if (icoImages == null) {
			return null;
		}
		int maxColorDepth = 0;
		for (ICOImage icoImage : icoImages) {
			int colorDepth = icoImage.getColourDepth();
			int width = icoImage.getWidth();
			if (width == 16 && maxColorDepth < colorDepth) {
				icon = icoImage.getImage();
				maxColorDepth = colorDepth;
			}
		}
		if (icon == null && !icoImages.isEmpty()) {
			icon = icoImages.get(0).getImage();
		}

		// step 3. create the imageIcon instance
		ImageIcon friendlyIcon = null;
		try {
			if (icon != null) {
				friendlyIcon = new ImageIcon(icon);
			}
		} catch (Exception ex) {
			// Ignore an exception here, since this is only for making a pretty UI.
		}
		return friendlyIcon;
	}

	public static String URLEncode(String s) {
		try {
			return URLEncoder.encode(s, UTF8);
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(GeneralUtils.class.getName()).log(Level.SEVERE, null, ex);
			throw new IllegalArgumentException(ex);
		}
	}

	public static String URLDecode(String s) {
		try {
			return URLDecoder.decode(s, UTF8);
		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(GeneralUtils.class.getName()).log(Level.SEVERE, null, ex);
			throw new IllegalArgumentException(ex);
		}
	}

	public static void browse(String s) {
		try {
			URI u = new URI(s);
		
			if("file".equalsIgnoreCase(u.getScheme())){
				Desktop.getDesktop().open(new File(u));
				return;
			}
			
			Desktop.getDesktop().browse(u);
		} catch (IOException ex) {
			Logger.getLogger(GeneralUtils.class.getName()).log(Level.SEVERE, null, ex);
		} catch (URISyntaxException ex) {
			Logger.getLogger(GeneralUtils.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * Write contents of file to output stream.
	 * @param f
	 * @param dos
	 */
	public static void writeFileToStream(File f, OutputStream dos) {
		FileInputStream is = null;
		try {
			is = new FileInputStream(f);
			byte[] buffer = new byte[4096]; // tweaking this number may increase performance
			int len;
			while ((len = is.read(buffer)) != -1) {
				dos.write(buffer, 0, len);
			}
		} catch (Exception ex) {
			Logger.getLogger(GeneralUtils.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(is);

		}
	}

	public static GenericFeature findFeatureWithURI(Collection<GenericFeature> features, URI uri) {
		if(uri == null || features.isEmpty())
			return null;
		
		for (GenericFeature feature : features) {
			if(uri.equals(feature.getURI()))
				return feature;
		}
		
		return null;	// couldn't find it
	}

	/**
	 * Moves mapping to the given path and renames it to filename.
	 * @param mapping	File to be moved.
	 * @param fileName	File name to be given to moved mapping.
	 * @param path	Path to where mapping is moved.
	 * @return
	 */
	public static boolean moveFileTo(File file, String fileName, String path){
		File newLocation = new File(path+ "/" +fileName);
		boolean sucess = file.renameTo(newLocation);

		if(!sucess){
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, "Could not find move file {0} to {1} !!!", new Object[]{fileName,path});
		}

		return sucess;
	}

	/**
	 * Copies mapping to the given path.
	 * @param mapping	File to be copied
	 * @param fileName	File name to be given to copied mapping.
	 * @param path	Path to where mapping is copied.
	 * @return
	 */
	public static boolean copyFileTo(File file, String fileName, String path){
		try {
			File newLocation = new File(path + "/" + fileName);
			if (!newLocation.createNewFile()) {
				Logger.getLogger(GeneralUtils.class.getName()).log(
						Level.SEVERE, "Could not find copy file from {0} to {1} !!!",
						new Object[]{fileName, path});
				return false;
			}

			GeneralUtils.unzipFile(file, newLocation);

			return true;
		} catch (IOException ex) {
			Logger.getLogger(GeneralUtils.class.getName()).log(Level.SEVERE, null, ex);
		}
		return false;
	}
	
	/**
	 * Creates directory for the given path.
	 * @param path	Path where directory is to be created.
	 * @return
	 */
	public static File makeDir(String path){
		File dir = new File(path);
		if(!dir.exists()){
			dir.mkdir();
		}
		return dir;
	}

	/**
	 * Returns mapping for give path.
	 * @param path	File path.
	 * @param fileMayNotExist
	 * @return
	 */
	public static File getFile(String path, boolean fileMayNotExist){
		File file = null;
		try{
			file = LocalUrlCacher.convertURIToFile(URI.create(path),fileMayNotExist);
		}catch(Exception ex){
			ex.printStackTrace();
		}

		if(file == null && !fileMayNotExist){
			Logger.getLogger(CacheScript.class.getName()).log(Level.SEVERE, "Invalid path : {0} !!!", path);
		}

		return file;
	}

	public static void unzipFile(File f, File f2) throws IOException {
		// File must be unzipped!
		InputStream is = null;
		OutputStream out = null;
		try {
			// This will also unzip the stream if necessary
			is = GeneralUtils.getInputStream(f, new StringBuffer());
			out = new FileOutputStream(f2);
			byte[] buf = new byte[1024];
			int len;
			while ((len = is.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			GeneralUtils.safeClose(is);
			GeneralUtils.safeClose(out);
		}
	}
}
