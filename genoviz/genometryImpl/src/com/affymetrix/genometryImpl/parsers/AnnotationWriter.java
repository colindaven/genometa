package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import java.io.*;
import java.util.*;


public interface AnnotationWriter {

	public boolean writeAnnotations(Collection<? extends SeqSymmetry> syms, BioSeq seq,
			String type, OutputStream outstream) throws IOException;

	public String getMimeType();
}
