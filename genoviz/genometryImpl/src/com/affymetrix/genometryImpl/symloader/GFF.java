package com.affymetrix.genometryImpl.symloader;

import java.net.URI;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;

/**
 *
 * @author hiralv
 */
public class GFF extends SymLoaderInstNC{

	public GFF(URI uri, String featureName, AnnotatedSeqGroup group){
		super(uri, featureName, group);
	}
}
