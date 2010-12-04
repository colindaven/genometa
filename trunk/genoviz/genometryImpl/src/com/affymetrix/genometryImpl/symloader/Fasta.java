package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author jnicol
 */
public class Fasta extends SymLoader {
	private static final List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("fa");
	}

	private static final Pattern header_regex = 
			Pattern.compile("^\\s*>\\s*(.+)");
	private final Set<BioSeq> chrSet = new HashSet<BioSeq>();

	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
	}

	public Fasta(URI uri, AnnotatedSeqGroup group) {
		super(uri, "", group);
		this.isResidueLoader = true;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		if(initChromosomes())
			super.init();
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	@Override
	public List<BioSeq> getChromosomeList(){
		init();
		return new ArrayList<BioSeq>(chrSet);
	}

	/**
	 * Get seqids and lengths for all chromosomes.
	 */
	private boolean initChromosomes() {
		BufferedInputStream bis = null;
		BufferedReader br = null;
		Matcher matcher = header_regex.matcher("");
		try {
			bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
			br = new BufferedReader(new InputStreamReader(bis));
			String header = br.readLine();;
			while (br.ready() && (!Thread.currentThread().isInterrupted())) {  // loop through lines till find a header line
				if (header == null) {
					continue;
				}  
				matcher.reset(header);

				if (!matcher.matches()) {
					continue;
				}
				String seqid = matcher.group(1).split(" ")[0];	//get rid of spaces
				BioSeq seq = group.getSeq(seqid);
				int count = 0;
				header = null;	// reset for next header
				String line = null;
				char firstChar;
				while (br.ready() && (!Thread.currentThread().isInterrupted())) {
					line = br.readLine();
					if (line == null){
						break;
					}
					if(line.length() == 0) {
						continue;
					}  // skip null and empty lines

					firstChar = line.charAt(0);
					if (firstChar == ';') {
						continue;
					} // skip comment lines

					// break if hit header for another sequence --
					if (firstChar == '>') {
						header = line;
						break;
					}
					count += line.trim().length();
				}
				if (seq == null) {
					chrSet.add(new BioSeq(seqid, "", count));
				} else {
					group.addSeq(seqid, count);
					chrSet.add(seq);
				}
			}

			return !Thread.currentThread().isInterrupted();
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(bis);
		}

		return false;
	}

	@Override
	public String getRegionResidues(SeqSpan span) {
		init();
		BufferedInputStream bis = null;
		BufferedReader br = null;
		int count = 0;
		String residues = "";
		Matcher matcher = header_regex.matcher("");
		try {
			bis = LocalUrlCacher.convertURIToBufferedUnzippedStream(uri);
			br = new BufferedReader(new InputStreamReader(bis));
			String header = br.readLine();
			while (br.ready() && (!Thread.currentThread().isInterrupted())) {  // loop through lines till find a header line
				if (header == null) {
					break;
				}  
				matcher.reset(header);

				if (!matcher.matches()) {
					continue;
				}
				String seqid = matcher.group(1).split(" ")[0];	// get rid of spaces
				BioSeq seq = group.getSeq(seqid);
				boolean seqMatch = (seq != null && seq == span.getBioSeq());
				header = null;	// reset for next header

				StringBuffer buf = new StringBuffer();
				String line = null;
				char firstChar;
				while (br.ready() && (!Thread.currentThread().isInterrupted())) {
					line = br.readLine();
					if (line == null){
						break;
					}
					
					if(line.length() == 0) {
						continue;
					}  // skip null and empty lines

					firstChar = line.charAt(0);
					if (firstChar == ';') {
						continue;
					} // skip comment lines

					// break if hit header for another sequence --
					if (firstChar == '>') {
						header = line;
						break;
					}
					if (seqMatch) {
						line = line.trim();
						if (count + line.length() <= span.getMin()) {
							// skip lines
							count += line.length();
							continue;
						}
						if (count > span.getMax()) {
							break; // should never happen
						}
						if (count < span.getMin()) {
							// skip beginning characters
							line = line.substring(span.getMin() - count);
							count = span.getMin();
						}
						if (count + line.length() >= span.getMax()) {
							// skip ending characters
							line = line.substring(0, count + line.length() - span.getMax());
						}
						buf.append(line);
					}
				}

				// Didn't use .toString() here because of a memory bug in Java
				// (See "stringbuffer memory java" for more details.)
				residues = new String(buf);
				buf.setLength(0);
				buf = null; // immediately allow the gc to use this memory
				residues = residues.trim();
				if (seqMatch) {
					break;
				}
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			GeneralUtils.safeClose(br);
			GeneralUtils.safeClose(bis);
		}
		return residues;
	}

	@Override
	public List<String> getFormatPrefList() {
		return pref_list;
	}
}
