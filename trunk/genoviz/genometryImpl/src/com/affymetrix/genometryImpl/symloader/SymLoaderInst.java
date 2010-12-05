package com.affymetrix.genometryImpl.symloader;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.comparator.BioSeqComparator;
import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.SeqSpan;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author hiralv
 */
public class SymLoaderInst extends SymLoader{

	private final List<BioSeq> chromosomeList = new ArrayList<BioSeq>();

	public SymLoaderInst(URI uri, String featureName, AnnotatedSeqGroup group){
		super(uri, featureName, group);
	}

	@Override
	public void init(){
		if(this.isInitialized){
			return;
		}
		super.init();
		
		chromosomeList.addAll(SymLoader.getChromosomes(uri, extension, featureName));
		Collections.sort(chromosomeList,new BioSeqComparator());
	}

	@Override
	public List<BioSeq> getChromosomeList(){		
		init();
		return chromosomeList;
	}

	@Override
	 public List<? extends SeqSymmetry> getGenome() {
		init();
		return super.getGenome();
	 }

	@Override
	public List<? extends SeqSymmetry> getChromosome(BioSeq seq) {
		init();
		return super.getChromosome(seq);
	}

	@Override
	public List<? extends SeqSymmetry> getRegion(SeqSpan overlapSpan) {
		init();
		return super.getRegion(overlapSpan);
    }
}
