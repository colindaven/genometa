package com.affymetrix.genometryImpl;

import java.util.Map;

/** A SeqSymmetry with Properties. */
public interface SymWithProps extends SeqSymmetry {
	public Map<String,Object> getProperties();
	public Map<String,Object> cloneProperties();
	public Object getProperty(String key);
	public boolean setProperty(String key, Object val);
}
