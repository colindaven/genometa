package com.affymetrix.genometryImpl;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.concurrent.CopyOnWriteArraySet;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.genometryImpl.util.SynonymLookup;
/**
 *
 * @version $Id: AnnotatedSeqGroup.java 7029 2010-10-14 21:00:29Z hiralv $
 */
public class AnnotatedSeqGroup {

	private final String UNKNOWN_ID = "UNKNOWN_SYM_";
	private int unknown_id_no = 1;
	final private String id;
	private String organism;
	private String description;
	final private Set<GenericVersion> gVersions = new CopyOnWriteArraySet<GenericVersion>();	// list of (visible) GenericVersions associated with this group
	private boolean use_synonyms;
	final private Map<String, BioSeq> id2seq;
	private List<BioSeq> seqlist; //lazy copy of id2seq.values()
	private boolean id2seq_dirty_bit; // used to keep the lazy copy
	final private TreeMap<String,Set<SeqSymmetry>> id2sym_hash;	// list of names -> sym
	final private TreeMap<String,Set<String>> symid2id_hash;	// main sym id -> list of other names
	private HashMap<String, Integer> type_id2annot_id = new HashMap<String, Integer>();
	
	private final static SynonymLookup chrLookup = SynonymLookup.getChromosomeLookup();
	private final static SynonymLookup groupLookup = SynonymLookup.getDefaultLookup();

	public AnnotatedSeqGroup(String gid) {
		id = gid;
		use_synonyms = true;
		id2seq = Collections.<String, BioSeq>synchronizedMap(new LinkedHashMap<String, BioSeq>());
		id2seq_dirty_bit = false;
		seqlist = new ArrayList<BioSeq>();
		id2sym_hash = new TreeMap<String,Set<SeqSymmetry>>();
		symid2id_hash = new TreeMap<String,Set<String>>();
	}

	final public String getID() {
		return id;
	}

	final public void setDescription(String str) {
		description = str;
	}

	final public String getDescription() {
		return description;
	}

	/** may want to move set/getOrganism() to an AnnotatedGenome subclass */
	final public void setOrganism(String org) {
		organism = org;
	}

	final public String getOrganism() {
		if(organism != null && !("".equals(organism)))
			return organism;

		String org = SpeciesLookup.getSpeciesName(id);

		if(org != null && !("".equals(org)))
			this.organism = org;

		return organism;
	}

	final public void addVersion(GenericVersion gVersion) {
		this.gVersions.add(gVersion);
	}

	/**
	 * Only return versions that should be visible.
	 * @return
	 */
	final public Set<GenericVersion> getEnabledVersions() {
		Set<GenericVersion> versions = new CopyOnWriteArraySet<GenericVersion>();
		for (GenericVersion v : gVersions) {
			if (v.gServer.isEnabled()) {
				versions.add(v);
			}
		}
		return versions;
	}

	final public GenericVersion getVersionOfServer(GenericServer gServer){
		for(GenericVersion v : gVersions){
			if(v.gServer.equals(gServer))
				return v;
		}
		return null; // No Associated version with provided server.
	}
	
	/**
	 * Return all versions.
	 * @return
	 */
	final public Set<GenericVersion> getAllVersions() {
		return Collections.unmodifiableSet(gVersions);
	}
	

	final public void addType(String type, Integer annot_id) {
		type_id2annot_id.put(type, annot_id);
	}
	
	final public void removeType(String type) {
		type_id2annot_id.remove(type);
	}
	
	public final Set<String> getTypeList() {
		return type_id2annot_id.keySet();
	}

	public final Integer getAnnotationId(String type) {
		return type_id2annot_id.get(type);
	}
	
	/**
	 *  Returns a List of BioSeq objects.
	 *  Will not return null.  The list is in the same order as in
	 *  {@link #getSeq(int)}.
	 */
	public List<BioSeq> getSeqList() {
		if (id2seq_dirty_bit) {
			// lazily keep the seqlist up-to-date
			seqlist = new ArrayList<BioSeq>(id2seq.values());
			id2seq_dirty_bit = false;
		}
		return Collections.<BioSeq>unmodifiableList(seqlist);
	}

	/**
	 *  Returns the sequence at the given position in the sequence list.
	 */
	public BioSeq getSeq(int index) {
		final List<BioSeq> seq_list = getSeqList();
		if (index < seq_list.size()) {
			return seq_list.get(index);
		}
		return null;
	}

	/** Returns the number of sequences in the group. */
	public int getSeqCount() {
		return id2seq.size();
	}

	/**
	 *  Sets whether or not to use the SynonymLookup class to search for synonymous
	 *  BioSeqs when using the getSeq(String) method.
	 *  If you set this to false and then add new sequences, you should probably
	 *  NOT later set it back to true unless you are sure you did not add
	 *  some synonymous sequences.
	 */
	public final void setUseSynonyms(boolean b) {
		use_synonyms = b;
	}

	/** Gets a sequence based on its name, possibly taking synonyms into account.
	 *  See {@link #setUseSynonyms(boolean)}.
	 *
	 * @param synonym the string identifier of the requested BioSeq
	 * @return a BioSeq for the given synonym or null
	 */
	public BioSeq getSeq(String synonym) {
		BioSeq aseq = id2seq.get(synonym.toLowerCase());
		if (use_synonyms && aseq == null) {
			// Try and find a synonym.
			for (String syn : chrLookup.getSynonyms(synonym,false)) {
				aseq = id2seq.get(syn.toLowerCase());
				if (aseq != null) {
					return aseq;
				}
			}
		}
		return aseq;
	}

	/**
	 *  For the given symmetry, tries to find in the group a sequence
	 *    that is pointed to by that symmetry.
	 *  @return the first sequence it finds (by iterating through sym's spans),
	 *    or null if none is found.
	 * PRECONDITION: sym != null.
	 */
	public BioSeq getSeq(SeqSymmetry sym) {
		final int spancount = sym.getSpanCount();
		for (int i = 0; i < spancount; i++) {
			SeqSpan span = sym.getSpan(i);
			BioSeq seq1 = span.getBioSeq();
			String seqid = seq1.getID();
			BioSeq seq2 = id2seq.get(seqid.toLowerCase());
			if ((seq2 != null) && (seq1 == seq2)) {
				return seq2;
			}
		}
		return null;
	}

	public final boolean isSynonymous(String synonym) {
		return id.equals(synonym) || groupLookup.isSynonym(id, synonym);
	}

	/**
	 *  Returns the BioSeq with the given id (or synonym), creating it if necessary,
	 *  and increasing its length to the given sym if necessary.
	 */
	public final BioSeq addSeq(String seqid, int length) {
		if (seqid == null) {
			throw new NullPointerException();
		}

		BioSeq aseq = getSeq(seqid);
		if (aseq != null) {
			if (aseq.getLength() < length) {
				aseq.setLength(length);
			}
		} else {
			aseq = new BioSeq(seqid, this.getID(), length);
			this.addSeq(aseq);
		}
		return aseq;
	}

	/**
	 * Adds the BioSeq to the group.
	 */
	final public void addSeq(BioSeq seq) {
		String seqID = seq.getID();
		final BioSeq oldseq = id2seq.get(seqID.toLowerCase());
		if (oldseq == null) {
			synchronized (this) {
				id2seq_dirty_bit = true;
				id2seq.put(seqID.toLowerCase(), seq);
				seq.setSeqGroup(this);
			}
		} else {
			throw new IllegalStateException("ERROR! tried to add seq: " + seqID + " to AnnotatedSeqGroup: " +
					this.getID() + ", but seq with same id is already in group");
		}
	}

	final public Set<SeqSymmetry> findInSymProp(Pattern regex) {
		final Set<SeqSymmetry> symset = new HashSet<SeqSymmetry>();
		final Matcher matcher = regex.matcher("");
		SymWithProps swp;
		String match;
		for (Map.Entry<String, Set<SeqSymmetry>> ent : id2sym_hash.entrySet()) {
			
			for (SeqSymmetry seq : ent.getValue()) {
				if (seq instanceof SymWithProps) {
					swp = (SymWithProps) seq;

					// Iterate through each properties.
					for (Map.Entry<String, Object> prop : swp.getProperties().entrySet()) {
						if (prop.getValue() != null) {
							match = prop.getValue().toString();
							matcher.reset(match);
							if (matcher.matches()) {
								symset.add(seq);
							}
						}
					}
				}
			}
		}
		
		return symset;
	}

	/**
	 * @return set of SeqSymmetries matching the pattern.
	 */
	final public Set<SeqSymmetry> findSyms(Pattern regex) {
		final Set<SeqSymmetry> symset = new HashSet<SeqSymmetry>();
		final Matcher matcher = regex.matcher("");

		for (Map.Entry<String, Set<SeqSymmetry>> ent : id2sym_hash.entrySet()) {
			String seid = ent.getKey();
			Set<SeqSymmetry> val = ent.getValue();
			if (seid != null && val != null) {
				matcher.reset(seid);
				if (matcher.matches()) {
					symset.addAll(val);
				}
			}
		}
		return symset;
	}

	/** Finds all symmetries with the given case-insensitive ID.
	 *  @return a non-null List, possibly an empty one.
	 */
	final public Set<SeqSymmetry> findSyms(String id) {
		if (id == null) {
			return Collections.<SeqSymmetry>emptySet();
		}
		Set<SeqSymmetry> sym_list = id2sym_hash.get(id.toLowerCase());
		if (sym_list == null) {
			return Collections.<SeqSymmetry>emptySet();
		}
		return Collections.<SeqSymmetry>unmodifiableSet(sym_list);
	}

	public String getUniqueID(){
		return UNKNOWN_ID + unknown_id_no++;
	}
	
	/**
	 *  Associates a symmetry with a case-insensitive ID.  You can later retrieve the
	 *  list of all matching symmetries for a given ID by calling findSyms(String).
	 *  Neither argument should be null.
	 */
	final public void addToIndex(String id, SeqSymmetry sym) {
		if (id == null || sym == null) {
			throw new NullPointerException();
		}
		this.putSeqInList(id.toLowerCase(), sym);
	}

	final public Set<String> getSymmetryIDs(String symID) {
		return this.symid2id_hash.get(symID);
	}

	/**
	 * Get unique id for id/trackName combination.
	 * Note this does not auto-increment, in order for the name to be reproducible if we need to load from the same combination again.
	 * @param id
	 * @param trackName
	 * @return unique but reproducible ID
	 */
	public static String getUniqueGraphTrackID(String id, String trackName) {
		if (trackName == null || trackName.length() == 0) {
			trackName = "_EMPTY";
		}
		return id + "_TRACK_" + trackName;
	}

	/**
	 *  Returns input id if no GraphSyms on any seq in the given seq group
	 *  are already using that id.
	 *  Otherwise uses id to build a new unique id.
	 *  The id returned is unique for GraphSyms on all seqs in the given group.
	 */
	public static String getUniqueGraphID(String id, AnnotatedSeqGroup seq_group) {
		String result = id;
		for (BioSeq seq : seq_group.getSeqList()) {
			result = getUniqueGraphID(result, seq);
		}
		return result;
	}

	/**
	 *  Returns input id if no GraphSyms on seq with given id.
	 *  Otherwise uses id to build a new id that is not used by a GraphSym (or top-level container sym )
	 *     currently on the seq.
	 *  The id returned is only unique for GraphSyms on that seq, may be used for graphs on other seqs.
	 */
	public static String getUniqueGraphID(String id, BioSeq seq) {
		if (id == null) {
			return null;
		}
		if (seq == null) {
			return id;
		}

		int prevcount = 0;
		String newid = id;
		while (seq.getAnnotation(newid) != null) {
			prevcount++;
			newid = id + "." + prevcount;
		}
		return newid;
	}


	/**
	 * Function to add a SeqSymmetry to the id2sym_hash (and symid2id_hash).
	 * @param id ID string (lower-cased).
	 * @param sym SeqSymmetry to add to the hash.
	 */
	private void putSeqInList(String id, SeqSymmetry sym) {
		Set<SeqSymmetry> seq_list = id2sym_hash.get(id);
		if (seq_list == null) {
			seq_list = new LinkedHashSet<SeqSymmetry>();
			id2sym_hash.put(id,seq_list);
		}
		seq_list.add(sym);

		String lcSymID = sym.getID().toLowerCase();
		if (id.equals(lcSymID)) {
			return;
		}
		Set<String> id_list = symid2id_hash.get(lcSymID);
		if (id_list == null) {
			id_list = new HashSet<String>();
			symid2id_hash.put(lcSymID, id_list);
		}
		id_list.add(id);
	}

	/**
	 * Remove symmetry from seq group, if it exists.
	 * @param sym
	 */
	final public void removeSymmetry(SeqSymmetry sym) {
		if (sym == null) {
			return;
		}

		for(int i=0; i<sym.getChildCount(); i++){
			removeSymmetry(sym.getChild(i));
		}
		
		if(sym.getID() == null)
			return;

		String lcSymID = sym.getID().toLowerCase();
		Set<SeqSymmetry> symList = id2sym_hash.get(lcSymID);
		if (symList != null && symList.contains(sym)) {
			symList.remove(sym);
			if (symList.isEmpty()) {
				id2sym_hash.remove(lcSymID);
			}
		}
		symid2id_hash.remove(lcSymID);
	}

	/**
	 * Create a temporary shallow-copy genome, to avoid any side-effects.
	 * @param oldGenome
	 * @return tempGenome
	 */
	public static AnnotatedSeqGroup tempGenome(AnnotatedSeqGroup oldGenome) {
		AnnotatedSeqGroup tempGenome = new AnnotatedSeqGroup(oldGenome.getID());
		tempGenome.setOrganism(oldGenome.getOrganism());
		for (BioSeq seq : oldGenome.getSeqList()) {
			tempGenome.addSeq(seq.getID(), seq.getLength());
		}
		return tempGenome;
	}
}
