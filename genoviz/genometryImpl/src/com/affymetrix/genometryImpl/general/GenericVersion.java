package com.affymetrix.genometryImpl.general;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.comparator.StringVersionDateComparator;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * A class that's useful for visualizing a generic version.
 * A generic version is a genome version found on a specific server.
 * Thus, GenericVersion has a many-to-one map to AnnotatedSeqGroup, and a many-to-one map to GenericServer.
 *
 * @author GenericVersion.java 4632 2009-11-04 15:19:16Z jnicol $
 */
public final class GenericVersion implements Comparable<GenericVersion> {

	public final AnnotatedSeqGroup group;
	/** Display name of this version */
	public final String versionName;          // name of the other.
	/** ID of this version on this server */
	public final String versionID;
	public final GenericServer gServer; // generic Server object.
	public final Object versionSourceObj;     // Das2VersionedSource, DasVersionedSource, ..., QuickLoad?
	private final Set<GenericFeature> features = new CopyOnWriteArraySet<GenericFeature>();	// features associated with this version
	private boolean isInitialized = false;	// is this version initialized?

	/**
	 * @param versionID 
	 * @param versionName
	 * @param gServer -- not null
	 * @param versionSourceObj
	 */
	public GenericVersion(AnnotatedSeqGroup group, String versionID, String versionName, GenericServer gServer, Object versionSourceObj) {
		this.group = group;
		this.versionID = versionID;
		this.versionName = versionName;
		this.gServer = gServer;
		this.versionSourceObj = versionSourceObj;
	}

	public void addFeature(GenericFeature f) {
		features.add(f);
	}

	public void removeFeature(GenericFeature f){
		features.remove(f);
	}

	public void setInitialized() {
		this.isInitialized = true;
	}
	public boolean isInitialized() {
		return this.isInitialized;
	}

	/**
	 * Return versions, but don't allow them to be modified.
	 * @return
	 */
	public Set<GenericFeature> getFeatures() {
		return Collections.<GenericFeature>unmodifiableSet(features);
	}

	@Override
	public String toString() {
		return this.versionName;
	}

	public int compareTo(GenericVersion other) {
		return new StringVersionDateComparator().compare(this.versionName, other.versionName);
	}
}
