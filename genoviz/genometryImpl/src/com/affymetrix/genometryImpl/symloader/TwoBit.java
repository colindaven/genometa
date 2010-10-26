package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.parsers.TwoBitParser;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author jnicol
 */
public class TwoBit extends SymLoader {

	private static final List<String> pref_list = new ArrayList<String>();
	static {
		pref_list.add("raw");
		pref_list.add("2bit");
	}

	private static final List<LoadStrategy> strategyList = new ArrayList<LoadStrategy>();
	static {
		// BAM files are generally large, so only allow loading visible data.
		strategyList.add(LoadStrategy.NO_LOAD);
		strategyList.add(LoadStrategy.VISIBLE);
		strategyList.add(LoadStrategy.CHROMOSOME);
	}
	
	public TwoBit(URI uri) {
		super(uri, "", null);
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
	public List<BioSeq> getChromosomeList(){
		//init();
		return Collections.<BioSeq>emptyList();
	}

	@Override
	public String getRegionResidues(SeqSpan span) {
		init();

		ByteArrayOutputStream outStream = null;
		try {
			outStream = new ByteArrayOutputStream();
			TwoBitParser.parse(uri, span.getStart(), span.getEnd(), outStream);
			byte[] bytes = outStream.toByteArray();
			return new String(bytes);
		} catch (Exception ex) {
			Logger.getLogger(TwoBit.class.getName()).log(Level.SEVERE, null, ex);
			return null;
		} finally {
			GeneralUtils.safeClose(outStream);
		}
	}

	@Override
	public List<String> getFormatPrefList() {
		return pref_list;
	}
}
