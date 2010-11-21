/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genometryImpl;

import java.util.*;

import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;

public class SimpleSymWithProps extends SimpleMutableSeqSymmetry
	implements SymWithProps {

	/** Set this property to Boolean.TRUE to indicate that the Symmetry is being
	 *  used simply to group other Symmetry's together, and that this Symmetry
	 *  does not represent any biological feature and should typically not be drawn
	 *  as a glyph.
	 */
	public static final String CONTAINER_PROP = "container sym";

	protected Map<String,Object> props;

	public SimpleSymWithProps() {
		super();
	}

	public SimpleSymWithProps(int estimated_child_count) {
		this();
		children = new ArrayList<SeqSymmetry>(estimated_child_count);
	}

	/** Returns the properties map, or null. */
	public Map<String,Object> getProperties() {
		return props;
	}

	/**
	 *  Creates a clone of the properties Map.
	 *  Uses the same type of Map class (HashMap, TreeMap, etc.)
	 *  as the original.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Object> cloneProperties() {
		if (props == null) {
			return null;
		}
		// quick check for efficient Hashtable cloning
		if (props instanceof Hashtable) {
			return (Map<String, Object>) ((Hashtable<String, Object>) props).clone();
		}
		// quick check for efficient HashMap cloning
		if (props instanceof HashMap) {
			return (Map<String, Object>) ((HashMap<String, Object>) props).clone();
		}
		// quick check for efficient TreeMap cloning
		if (props instanceof TreeMap) {
			return (Map<String, Object>) ((TreeMap<String, Object>) props).clone();
		}
		try {
			Map<String, Object> newprops = (Map<String, Object>) props.getClass().newInstance();
			newprops.putAll(props);
			return newprops;
		} catch (Exception ex) {
			System.out.println("problem trying to clone SymWithProps properties, "
					+ "returning null instead");
			return null;
		}
	}

	/** Sets the properties to the given Map.
	 *  This does not copy the properties, but rather maintains a reference
	 *  to the actual Map passed-in.
	 *  @param propmap  a Map of Strings to Strings.  This class will not throw exceptions
	 *  if the map is null.
	 */
	public boolean setProperties(Map<String,Object> propmap) {
		this.props = propmap;
		return true;
	}

	/** Retrieves the property called "id". */
	@Override
	public String getID() { return (String)getProperty("id"); }
	
	public void setID(String id) { setProperty("id", id); }

	public boolean setProperty(String name, Object val) {
		if (name == null)  { return false; }
		if (props == null) {
			props = new HashMap<String,Object>();
		}
		props.put(name, val);
		return true;
	}

	public Object getProperty(String name) {
		if (props == null) { return null; }
		return props.get(name);
	}

}
