package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.comparator.BioSeqComparator;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.GFF3Parser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author hiralv
 */
public class GFF3 extends SymLoader{
	private static final boolean DEBUG = false;

	private static final Pattern line_regex = Pattern.compile("\\s+");
	private static final Pattern directive_version = Pattern.compile("##gff-version\\s+(.*)");
	
	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		//strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}

	public GFF3(URI uri, String featureName, AnnotatedSeqGroup group){
		super(uri, featureName, group);
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		if(buildIndex())
			super.init();
	}

	@Override
	public List<BioSeq> getChromosomeList(){
		init();
		List<BioSeq> chromosomeList = new ArrayList<BioSeq>(chrList.keySet());
		Collections.sort(chromosomeList,new BioSeqComparator());
		return chromosomeList;
	}

	@Override
	public List<? extends SeqSymmetry> getGenome() {
		init();
		List<BioSeq> allSeq = getChromosomeList();
		List<SeqSymmetry> retList = new ArrayList<SeqSymmetry>();
		for(BioSeq seq : allSeq){
			retList.addAll(getChromosome(seq));
		}
		return retList;
	}


	@Override
	public List<? extends SeqSymmetry> getChromosome(BioSeq seq) {
		init();
		return parse(seq,seq.getMin(),seq.getMax());
	}


	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan span) {
		init();
		return parse(span.getBioSeq(),span.getMin(),span.getMax());
	}

	private List<? extends SeqSymmetry> parse(BioSeq seq, int min, int max) {
		GFF3Parser parser = new GFF3Parser();
		InputStream istr = null;
		try {
			File file = chrList.get(seq);
			if (file == null) {
				Logger.getLogger(GFF3.class.getName()).log(Level.FINE, "Could not find chromosome {0}", seq.getID());
				return Collections.<SeqSymmetry>emptyList();
			}
			istr = new FileInputStream(file);
			return parser.parse(istr, featureName, group, true);
		}catch (Exception ex) {
			Logger.getLogger(BED.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(istr);
		}
		return Collections.<SeqSymmetry>emptyList();
	}


	@Override
	protected boolean parseLines(InputStream istr, Map<String, Integer> chrLength, Map<String, File> chrFiles) {
		BufferedReader br = null;
		BufferedWriter bw = null;

		Map<String, Boolean> chrTrack = new HashMap<String, Boolean>();
		Map<String, BufferedWriter> chrs = new HashMap<String, BufferedWriter>();
		String line, trackLine = null, seq_name = null;
		String[] fields;

		try {
			Thread thread = Thread.currentThread();
			br = new BufferedReader(new InputStreamReader(istr));
			while ((line = br.readLine()) != null && (!thread.isInterrupted())) {
				if(line.length() == 0)
					continue;

				char firstchar = line.charAt(0);
				if(firstchar == '#'){
					if (line.startsWith("##track name")) {
						chrTrack = new HashMap<String, Boolean>();
						trackLine = line;
					}
					continue;
				} else {
					if (DEBUG) {
						System.out.println(line);
					}
					fields = line_regex.split(line);

					if (fields.length < 5) {
						Logger.getLogger(GFF3.class.getName()).severe("Invalid GFF file");
						return false;
					}

					seq_name = fields[0]; // seq id field
					int end = Integer.parseInt(fields[4]);

					if (!chrs.containsKey(seq_name)) {
						addToLists(chrs, seq_name, chrFiles, chrLength, ".gff3");
					}
					bw = chrs.get(seq_name);
					if (!chrTrack.containsKey(seq_name)) {
						chrTrack.put(seq_name, true);

						if (trackLine != null) {
							bw.write(trackLine + "\n");
						}
					}
					bw.write(line + "\n");

					if (end > chrLength.get(seq_name)) {
						chrLength.put(seq_name, end);
					}

				}
			}

			return !thread.isInterrupted();
		} catch (IOException ex) {
			Logger.getLogger(BED.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			for (BufferedWriter b : chrs.values()) {
				GeneralUtils.safeClose(b);
			}
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(bw);
		}
		return false;
	}


	public static boolean isGFF3(URI uri){
		BufferedInputStream bis = null;
		BufferedReader br = null;
		String line;
		try {
			
			Thread thread = Thread.currentThread();
			bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
			br = new BufferedReader(new InputStreamReader(bis));

			while ((line = br.readLine()) != null && (!thread.isInterrupted())) {
				// The "#gff-version 3" pragma is *required* to be on the first line.
				// If it is not then it is not gff3
				if(!line.startsWith("#"))
					return false;

				if(processDirective(line)){
					return true;
				}
			}

		} catch (IOException ex) {
			Logger.getLogger(GFF3.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(bis);
		}

		return false;
	}

	private static boolean processDirective(String line) throws IOException {
		Matcher m = directive_version.matcher(line);
		if (m.matches()) {
			String vstr = m.group(1).trim();
			if ( "3".equals(vstr)) {
				return true;
			}
		} else {
			Logger.getLogger(GFF3.class.getName()).log(Level.WARNING, "Didn''t recognize directive: {0}", line);
		}
		return false;
	}

}
