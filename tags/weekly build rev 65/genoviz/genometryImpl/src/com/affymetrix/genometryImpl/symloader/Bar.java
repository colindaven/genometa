package com.affymetrix.genometryImpl.symloader;

import java.io.*;
import java.util.*;

import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.graph.BarParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Bar extends SymLoader {

	private File f = null;

	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
		strategyList.add(LoadStrategy.GENOME);
	}

	public Bar(URI uri, String featureName, AnnotatedSeqGroup group) {
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
		super.init();
		f = LocalUrlCacher.convertURIToFile(uri);
	}

	@Override
	public List<GraphSym> getGenome() {
		BufferedInputStream bis = null;
		try {
			init();
			bis = new BufferedInputStream(new FileInputStream(f));
			return BarParser.parse(bis, GenometryModel.getGenometryModel(), group, null, 0, Integer.MAX_VALUE, featureName, true);
		} catch (Exception ex) {
			Logger.getLogger(Bar.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(bis);
		}
		return null;
	}

	@Override
	public List<GraphSym> getChromosome(BioSeq seq) {
		BufferedInputStream bis = null;
		try {
			init();
			bis = new BufferedInputStream(new FileInputStream(f));
			return BarParser.parse(bis, GenometryModel.getGenometryModel(), group, seq, 0, seq.getMax() + 1, featureName, true);
		} catch (Exception ex) {
			Logger.getLogger(Bar.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(bis);
		}
		return null;
	}

	@Override
	public List<GraphSym> getRegion(SeqSpan span) {
		BufferedInputStream bis = null;
		try {
			init();
			bis = new BufferedInputStream(new FileInputStream(f));
			return BarParser.parse(bis, GenometryModel.getGenometryModel(), group, span.getBioSeq(), span.getMin(), span.getMax(), featureName, true);
		} catch (Exception ex) {
			Logger.getLogger(Bar.class.getName()).log(Level.SEVERE, null, ex);
		} finally {
			GeneralUtils.safeClose(bis);
		}
		return null;
	}
}
