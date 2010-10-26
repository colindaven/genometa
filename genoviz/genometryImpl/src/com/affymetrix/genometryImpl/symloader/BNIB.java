package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.NibbleResiduesParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.sf.samtools.util.SeekableStream;

/**
 *
 * @author jnicol
 */
public class BNIB extends SymLoader {
	private static final List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("raw");
		pref_list.add("bnib");
	}

	private List<BioSeq> chrList = null;	// at most one element here, so it can be an ArrayList

	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
	}

	public BNIB(URI uri, AnnotatedSeqGroup group) {
		super(uri, "", group);
		this.isResidueLoader = true;
	}

	@Override
	public void init() {
		if (this.isInitialized) {
			return;
		}
		super.init();
	}

	@Override
	public List<LoadStrategy> getLoadChoices() {
		return strategyList;
	}

	@Override
	public List<BioSeq> getChromosomeList() {
		if (this.chrList != null) {
			return this.chrList;
		}

		init();
		chrList = new ArrayList<BioSeq>(1);
		SeekableStream sis = null;
		try {
			sis = LocalUrlCacher.getSeekableStream(uri);
			BioSeq seq = NibbleResiduesParser.determineChromosome(sis, group);
			if (seq != null) {
				chrList.add(seq);
			}
		} catch (Exception ex) {
			Logger.getLogger(TwoBit.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(sis);
		}
		return chrList;
	}

	@Override
	public String getRegionResidues(SeqSpan span) {
		init();

		SeekableStream sis = null;
		ByteArrayOutputStream outStream = null;
		try {
			outStream = new ByteArrayOutputStream();
			sis = LocalUrlCacher.getSeekableStream(uri);
			NibbleResiduesParser.parse(sis, span.getStart(), span.getEnd(), outStream);
			byte[] bytes = outStream.toByteArray();
			return new String(bytes);
		} catch (Exception ex) {
			Logger.getLogger(TwoBit.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		} finally {
			GeneralUtils.safeClose(outStream);
			GeneralUtils.safeClose(sis);
		}
	}

	@Override
	public List<String> getFormatPrefList() {
		return pref_list;
	}
}
