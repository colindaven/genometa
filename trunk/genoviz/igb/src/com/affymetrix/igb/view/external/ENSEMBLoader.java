package com.affymetrix.igb.view.external;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

/**
 * Helper class for getting genomic images from ENSEMBL
 *
 * @author Ido M. Tamir
 */
class ENSEMBLoader extends BrowserLoader {

	private static final Map<String, EnsemblURL> dbmap = new HashMap<String, EnsemblURL>();
	

	static {
		dbmap.put("mm9", new EnsemblURL("http://may2010.archive.ensembl.org/Mus_musculus"));
		dbmap.put("hg19",new EnsemblURL("http://may2010.archive.ensembl.org/Homo_sapiens"));
		dbmap.put("dm3", new EnsemblURL("http://may2010.archive.ensembl.org/Drosophila_melanogaster"));
	}

	public static String getUrlForView(Loc loc, int pixWidth) {
		if (!dbmap.containsKey(loc.db)) {
			return "Error: could not transpose the genome " + loc.db + " for ENSEMBL";
		}
		if( loc.length() >= 100000){
			return "Error: the selected region is too large (>100000)";
		}
		String chr = loc.chr.replaceAll("chr", "");
		String url = dbmap.get(loc.db).url + "/Location/View?r=" + chr + ":" + (loc.start+1) + "-" + loc.end; //ensembl = 1 based
		Logger.getLogger(ENSEMBLoader.class.getName()).log(Level.FINE, "url was : " + url);
		return url;
	}
	

	/**
	 *
	 * @param query comes from the IGB as UCSC query string
	 * @param pixWidth
	 * @param cookies
	 * @return
	 */
	@Override
	public ImageError getImage(Loc loc, int pixWidth, Map<String, String> cookies) {
		String url = "";
		try{
			url = getUrlForView(loc, pixWidth);
		}
		catch(Exception e){
			url = "Error: Could not translate UCSC query for ENSEMBL: " + loc;
		}
		if(url.startsWith("http")){
			String cookie = EnsemblView.ENSEMBLWIDTH + "=" + cookies.get(EnsemblView.ENSEMBLWIDTH);
			String session = cookies.get(EnsemblView.ENSEMBLSESSION);
			if(session != null && !session.equals("")){
				cookie += ";" + EnsemblView.ENSEMBLSESSION + "=" + cookies.get(EnsemblView.ENSEMBLSESSION);
			}
			url = getImageUrl(url, cookie, new ENSEMBLURLFinder());
			if (url.startsWith("http")) {
				try {
					return new ImageError(ImageIO.read(new URL(url)),"");
				} catch (IOException e) {
					Logger.getLogger(BrowserView.class.getName()).log(Level.FINE, "url was : " + url, e);
				}
			}
		}
		return new ImageError(createErrorImage(url, pixWidth), "Error: " + url);
	}
}

/**
 * Extracts the image url from the returned html page.
 * ENSEMBL likes to change the ids of the elments quite often
 * e.g. sep2009 id = "BottomViewPanel" -> may2010 id ="contigviewbottom"
 *
 * the panelPattern could be part of the ensemblurl and passed into the constructor to be
 * more flexible and allow other ensembl versions
 *
 */
class ENSEMBLURLFinder implements URLFinder {
	private final static Pattern panelPattern = Pattern.compile("id=\"contigviewbottom\"");
	private final static Pattern imagePattern = Pattern.compile("img-tmp(.*png)");

	public String findUrl(BufferedReader reader, URL redirectedURL) throws IOException {
		String inputLine = "";
		boolean panel = false;
		while ((inputLine = reader.readLine()) != null) {
			if (!panel) {
				Matcher m = panelPattern.matcher(inputLine);
				panel = m.find();
			} else {
				Matcher m = imagePattern.matcher(inputLine);
				if (m.find()) {
					Logger.getLogger(UCSCLoader.class.getName()).log(Level.FINE, "found fileName " + inputLine);
					String fileName = m.group(1);
					return "http://"+redirectedURL.getHost() + "/img-tmp" + fileName;
				}
			}
		}
		return "Error: could not find image URL in page " + redirectedURL.toExternalForm();
	}
}

class EnsemblURL {
	final String url;
	EnsemblURL(String url){
		this.url = url;
	}
}

