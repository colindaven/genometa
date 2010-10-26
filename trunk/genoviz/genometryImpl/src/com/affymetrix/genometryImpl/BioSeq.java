package com.affymetrix.genometryImpl;

import com.affymetrix.genometryImpl.general.SymLoader;
import com.affymetrix.genometryImpl.span.SimpleMutableSeqSpan;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import com.affymetrix.genometryImpl.symmetry.SimpleMutableSeqSymmetry;
import com.affymetrix.genometryImpl.util.DNAUtils;
import com.affymetrix.genometryImpl.util.SeqUtils;
import com.affymetrix.genometryImpl.util.IndexingUtils.IndexedSyms;
import com.affymetrix.genometryImpl.util.SearchableCharIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author $Id: BioSeq.java 6568 2010-08-02 15:16:14Z jnicol $
 */
public final class BioSeq implements SearchableCharIterator {
	private Map<String, SymWithProps> type_id2sym = null;   // lazy instantiation of type ids to container annotations
	private Map<String, IndexedSyms> type_id2indexedsym = null;
	private Map<String, SymLoader> type_id2symloader = null;
	private AnnotatedSeqGroup seq_group;
	private List<SeqSymmetry> annots;
	private String version;
	private SearchableCharIterator residues_provider;
	// GAH 8-14-2002: need a residues field in case residues need to be cached
	// (rather than derived from composition), or if we choose to store residues here
	// instead of in composition seqs in case we actually want to compose/cache
	// all residues...
	private String residues;

	/** The index of the first residue of the sequence. */
	private int start;
	/** The index of the last residue of the sequence. */
	private int end;
	/**
	 * SeqSymmetry to store the sequence in.
	 */
	private SeqSymmetry compose;

	/**
	 * Length of the sequence, stored as a double.  The value is always an
	 * integer and much of the functionality of this class and its sub-classes
	 * is lost if the length is greater than Integer.INT_MAX.
	 */
	private double length = 0;
	/**
	 * String identifier for the sequence.  This is not guaranteed to be unique.
	 */
	private final String id;


	public BioSeq(String seqid, String seqversion, int length) {
		this.id = seqid;
		this.length = length;
		start = 0;
		end = length;
		this.version = seqversion;
	}

	public String getID() {
		return id;
	}

	public AnnotatedSeqGroup getSeqGroup() {
		return seq_group;
	}
	public void setSeqGroup(AnnotatedSeqGroup group) {
		seq_group = group;
	}

	public SeqSymmetry getComposition() {
		return compose;
	}

	public void setComposition(SeqSymmetry compose) {
		this.compose = compose;
	}
	/**
	 *  returns Set of type ids
	 */
	public Set<String> getTypeList() {
		if (type_id2sym == null) {
			return Collections.<String>emptySet();
		}
		return type_id2sym.keySet();
	}

	public String getVersion() { return version; }
	public void setVersion(String str) { this.version = str; }

	/**
	 * Returns the number of residues in the sequence as a double.
	 *
	 * @return the number of residues in the sequence as a double
	 */
	public double getLengthDouble() {
		return length;
	}


	public int getLength() {
		if (length > Integer.MAX_VALUE) {
			return Integer.MAX_VALUE - 1;
		} else {
			return (int) length;
		}
	}

	public void setLength(int length) {
		setBounds(0, length);  // sets start, end, bounds

		// if length does not agree with length of residues, null out residues
		if ((residues != null) && (residues.length() != length)) {
			System.out.println("*** WARNING!!! lengths disagree: residues = " + residues.length() +
					", seq = " + this.length);
		}
	}

		/**
	 * Sets the start and end of the sequence as double values.
	 * <p />
	 * <em>WARNING:</em> min and max are stored internally using integers.  If
	 * min or max are outside of the range Integer.MIN_VALUE and
	 * Integer.MAX_VALUE, the values will not be stored properly.  The length
	 * (min - max) is computed and stored as a double before min and max are
	 * downcast to int.
	 *
	 * @param min the index of the first residue of the sequence, as a double.
	 * @param max the index of the last residue of the sequence, as a double.
	 */
	public void setBoundsDouble(double min, double max) {
		length = max - min;
		if (min < Integer.MIN_VALUE) { start = Integer.MIN_VALUE + 1; }
		else { start = (int)min; }
		if (max > Integer.MAX_VALUE) { end = Integer.MAX_VALUE - 1; }
		else { end = (int)max; }
	}

	/**
	 * Sets the start and end of the sequence
	 *
	 * @param min the index of the first residue of the sequence.
	 * @param max the index of the last residue of the sequence.
	 */
	public void setBounds(int min, int max) {
		start = min;
		end = max;
		//    length = end - start;
		length = (double)end - (double)start;
	}

	/**
	 * Returns the integer index of the first residue of the sequence.  Negative
	 * values are acceptable.  The value returned is undefined if the minimum
	 * value is set using setBoundsDouble(double, double) to something outside
	 * of Integer.MIN_VALUE and Integer.MAX_VALUE.
	 *
	 * @return the integer index of the first residue of the sequence.
	 */
	public int getMin() { return start; }

	/**
	 * Returns the integer index of the last residue of the sequence.  The
	 * maximum value must always be greater than the minimum value.  The value
	 * returned is undefined if the maximum value is set using
	 * setBoundsDouble(double, double) to something outside of Integer.MIN_VALUE
	 * and Integer.MAX_VALUE.
	 *
	 * @return the integer index of the last residue of the sequence.
	 */
	public int getMax() { return end; }

	public int getAnnotationCount() {
		if (null != annots) return annots.size();
		return 0;
	}

	public /*@Nullable*/ SeqSymmetry getAnnotation(int index) {
		if (null != annots && index < annots.size())
			return annots.get(index);
		return null;
	}
	/**
	 *  Returns a top-level symmetry or null.
	 *  Used to return a TypeContainerAnnot, but now returns a SymWithProps which is
	 *     either a TypeContainerAnnot or a GraphSym, or a ScoredContainerSym,
	 *     so GraphSyms can be retrieved with graph id given as type
	 */
	public SymWithProps getAnnotation(String type) {
		if (type_id2sym == null) {
			return null; }
		return type_id2sym.get(type);
	}

	public List<SymWithProps> getAnnotations(Pattern regex) {
		List<SymWithProps> results = new ArrayList<SymWithProps>();
		if (type_id2sym != null)  {
			Matcher match = regex.matcher("");
			for (Map.Entry<String, SymWithProps> entry : type_id2sym.entrySet()) {
				String type = entry.getKey();
				if (match.reset(type).matches()) {
					results.add(entry.getValue());
				}
			}
		}
		return results;
	}


	/**
	 *  Overriding addAnnotation(sym) to try and extract a "method"/"type" property
	 *    from the sym.
	 *  <pre>
	 *    If can be found, then instead of adding annotation directly
	 *    to seq, use addAnnotation(sym, type).  Which ends up adding the annotation
	 *    as a child of a container annotation (generally means two levels of container,
	 *    since parsers call addAnnotation with a container already if indicated).
	 *    So for example for DAS transcript-exon annotation will get a four-level
	 *    hierarchy:
	 *       1. Top-level container annot per seq
	 *       2. 2nd-level container annot per DAS call (actually probably special DasFeatureRequestSym
	 *       3. Transcript syms
	 *       4. Exon syms
	 *
	 *  GraphSym's and ScoredContainerSym's are added directly, not in containers.
	 *  </pre>
	 */
	public synchronized void addAnnotation(SeqSymmetry sym) {
		if (! needsContainer(sym)) {
			if (!(sym instanceof SymWithProps)) {
				throw new RuntimeException("sym must be a SymWithProps");
			}
			String symID = sym.getID();
			if (symID == null) {
				throw new RuntimeException("sym.getID() == null && (! needsContainer(sym)), this should never happen!");
			}
			if (type_id2sym == null) { 
				type_id2sym = new LinkedHashMap<String,SymWithProps>(); 
			} else {
				if (type_id2sym.containsKey(symID) && sym.equals(type_id2sym.get(id))) {
					return;	// sym already in hash (and thus also annots list)
				}
			}
			type_id2sym.put(symID, (SymWithProps) sym);
			if (annots == null) {
				annots = new ArrayList<SeqSymmetry>();
			}
			annots.add(sym);
			return;
		}
		String type = determineMethod(sym);
		if (type != null)  {
			// add as child to the top-level container
			addAnnotation(sym, type); // side-effect calls notifyModified()
			return;
		}
		else  {
			throw new RuntimeException(
					"BioSeq.addAnnotation(sym) will only accept " +
					" SeqSymmetries that are also SymWithProps and " +
					" have a _method_ property");
		}
	}

	/**
	 *  Adds an annotation as a child of the top-level container sym
	 *     for the given type.  Creates new top-level container
	 *     if doesn't yet exist.
	 */
	private synchronized void addAnnotation(SeqSymmetry sym, String type) {
		if (type_id2sym == null) {
			type_id2sym = new LinkedHashMap<String, SymWithProps>();
		}
		MutableSeqSymmetry container = (MutableSeqSymmetry) type_id2sym.get(type);
		if (container == null) {
			container = new TypeContainerAnnot(type);
			((TypeContainerAnnot) container).setProperty("method", type);
			SeqSpan span = new SimpleSeqSpan(0, this.getLength(), this);
			container.addSpan(span);
			type_id2sym.put(type, (TypeContainerAnnot) container);
			if (annots == null) {
				annots = new ArrayList<SeqSymmetry>();
			}
			annots.add(container);	// Can't be a duplicate; the container object was just created.
		}
		container.addChild(sym);
	}
	
	/*
	 * Remove annotation (clear and unload) from DAS/2 server
	 */
	public synchronized void unloadAnnotation(SeqSymmetry annot) {
		removeAnnotation(annot, true);
	}

	/**
	 * Remove annotation from the BioSeq and its parent AnnotatedSeqGroup.
	 * @param annot
	 */
	public synchronized void removeAnnotation(SeqSymmetry annot) {
		removeAnnotation(annot, false);
	}

	private synchronized void removeAnnotation(SeqSymmetry annot, boolean clearContainer) {
		if (annot != null) {
			this.getSeqGroup().removeSymmetry(annot);
		}
		if (null != annots) {
			annots.remove(annot);
		}

		// If the annotation contains other annotations, remove the container
		String type = determineMethod(annot);
		if (type == null) {
			return;
		}
		SymWithProps sym = getAnnotation(type);
		if (sym != null && sym instanceof MutableSeqSymmetry) {
			MutableSeqSymmetry container = (MutableSeqSymmetry) sym;
			if (container == annot) {
				type_id2sym.remove(type);
			} else {
				container.removeChild(annot);
			}
			if (clearContainer) {
				// Additional clearing done on the DAS/2 server
				container.clear();
			}
		}
	}

	/**
	 * Add an indexed collection to id2indexedsym.
	 * @param type ID string.
	 * @param value indexedSyms to add to the hash.
	 */
	public final void addIndexedSyms(String type, IndexedSyms value) {
		if(type_id2indexedsym == null){
			type_id2indexedsym = new HashMap<String, IndexedSyms>();
		}
		type_id2indexedsym.put(type,value);
	}

	public final Set<String> getIndexedTypeList() {
		if(type_id2indexedsym == null){
			return Collections.<String>emptySet();
		}
		return type_id2indexedsym.keySet();
	}

	public final IndexedSyms getIndexedSym(String type) {
		if(type_id2indexedsym == null){
			return null;
		}
		return type_id2indexedsym.get(type);
	}
	
	public boolean removeIndexedSym(String type) {
		if(type_id2indexedsym == null || !type_id2indexedsym.containsKey(type)) {
			return false;
		}
		type_id2indexedsym.remove(type);
		return true;
	}

	public final void addSymLoader(String type, SymLoader value){
		if(type_id2symloader == null){
			type_id2symloader = new HashMap<String, SymLoader>();
		}
		type_id2symloader.put(type, value);
	}

	public final Set<String> getSymloaderList(){
		if(type_id2symloader == null){
			return Collections.<String>emptySet();
		}
		return type_id2symloader.keySet();
	}

	public final SymLoader getSymLoader(String type){
		if(type_id2symloader == null) {
			return null;
		}
		return type_id2symloader.get(type);
	}

	public boolean  removeSymLoader(String type) {
		if(type_id2symloader == null || !type_id2symloader.containsKey(type)) {
			return false;
		}
		
		type_id2symloader.remove(type);
		return true;
	}
	
	/**
	 * Returns true if the sym is of a type needs to be wrapped in a {@link TypeContainerAnnot}.
	 * GraphSym's and ScoredContainerSym's are added directly, not in containers.
	 */
	private static boolean needsContainer(SeqSymmetry sym) {
		if (sym instanceof GraphSym || sym instanceof ScoredContainerSym || sym instanceof TypeContainerAnnot) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 *  Finds the "method" for a SeqSymmetry.
	 *  Looks for the "method" in four places, in order:
	 *   (1) the property "method", (2) the property "meth",
	 *   (3) the property "type", (4) TypedSym.getType().
	 *  If no method is found, returns null.
	 */
	public static String determineMethod(SeqSymmetry sym) {
		String meth = null;
		if (sym instanceof SymWithProps)  {
			SymWithProps psym = (SymWithProps)sym;
			meth = (String)psym.getProperty("method");
			if (meth == null) { meth = (String) psym.getProperty("meth"); }
			if (meth == null) { meth = (String) psym.getProperty("type"); }
		}
		if (meth == null) {
			if (sym instanceof TypedSym) {
				meth = ((TypedSym)sym).getType();
			}
		}
		return meth;
	}

	public SearchableCharIterator getResiduesProvider() {
		return residues_provider;
	}
	public <S extends SearchableCharIterator> void setResiduesProvider(S chariter) {
		if (chariter.getLength() != this.getLength()) {
			System.out.println("WARNING -- in setResidueProvider, lengths don't match");
		}
		residues_provider = chariter;
	}

	/**
	 * Returns all residues on the sequence.
	 *
	 * @return a String containing all residues on the sequence.
	 */
	public String getResidues() {
		return this.getResidues(start, end);
	}

	/**
	 * Returns all residues on the sequence.
	 *
	 * @return a String containing all residues on the sequence
	 */
	public String getResidues(int start, int end) {
		return getResidues(start, end, ' ');
	}
		
	/** Gets residues.
	 *  @param fillchar  Character to use for missing residues;
	 *     warning: this parameter is used only if {@link #getResiduesProvider()} is null.
	 */
	private String getResidues(int start, int end, char fillchar) {
		if (residues_provider == null) {
			return getResiduesNoProvider(start, end, '-');
		}

		if (start <= end) {
			return residues_provider.substring(start, end);
		}

		// start > end -- that means reverse complement.
		return DNAUtils.reverseComplement(residues_provider.substring(end, start));
	}
	private String getResiduesNoProvider(int start, int end, char fillchar) {
		if (this.getLengthDouble() > Integer.MAX_VALUE) {
			Logger.getLogger(BioSeq.class.getName()).fine("Length exceeds integer size");
			return "";
		}
		int residue_length = this.getLength();
		if (start < 0 || residue_length <= 0) {
			Logger.getLogger(BioSeq.class.getName()).fine("Invalid arguments: " + start + "," + end + "," + residue_length);
			return "";
		}

		//TODO: If start is greater than residue_length then
		//			this condition fails and returns unexpected string
		// Sanity checks on argument size.
		start = Math.min(start, residue_length);
		end = Math.min(end, residue_length);
		if (start <= end) {
			end = Math.min(end, start+residue_length);
		}
		else {
			start = Math.min(start, end+residue_length);
		}

		if (residues == null) {
			return getResiduesFromComposition(start, end, fillchar);
		}

		if (start <= end) {
			return residues.substring(start, end);
		}

		// start > end -- that means reverse complement.
		return DNAUtils.reverseComplement(residues.substring(end, start));
	}
	/**
	 * Returns the residues on the sequence between start and end using the
	 * fillchar to fill any gaps in the sequence.  Unknown if this implementation
	 * is inclusive or exclusive on start and end.
	 *
	 * @param  res_start    the start index (inclusive?)
	 * @param  res_end      the end index (exclusive?)
	 * @param  fillchar the character to fill empty residues in the sequence with.
	 * @return          a String containing residues between start and end.
	 */
	private String getResiduesFromComposition(int res_start, int res_end, char fillchar) {
			SeqSpan residue_span = new SimpleSeqSpan(res_start, res_end, this);
			int reslength = Math.abs(res_end - res_start);
			char[] char_array = new char[reslength];
			Arrays.fill(char_array, fillchar);
			SeqSymmetry rootsym = this.getComposition();
			if (rootsym != null) {
				// adjusting index into array to compensate for possible seq start < 0
				//int array_offset = -start;
				getResiduesFromComposition(residue_span, rootsym, char_array);
				// Note that new String(char[]) causes the allocation of a second char array
			}
			return new String(char_array);
		}

	/**
	 * Function for finding residues.  This function is a bit of a mess:
	 * the implementation is more confusing than it needs to be.
	 *
	 * @param this_residue_span the SeqSpan to find residues on
	 * @param sym               the SeqSymmetry to search for residues
	 * @param residues          the character array to be filled with residues
	 */
	private void getResiduesFromComposition(SeqSpan this_residue_span, SeqSymmetry sym, char[] residues) {
		int symCount = sym.getChildCount();
		if (symCount == 0) {
			SeqSpan this_comp_span = sym.getSpan(this);
			if (this_comp_span == null || !SeqUtils.overlap(this_comp_span, this_residue_span)) {
				return;
			}
			BioSeq other_seq = SeqUtils.getOtherSeq(sym, this);
			SeqSpan other_comp_span = sym.getSpan(other_seq);
			MutableSeqSpan ispan = new SimpleMutableSeqSpan();
			SeqUtils.intersection(this_comp_span, this_residue_span, ispan);
			MutableSeqSpan other_residue_span = new SimpleMutableSeqSpan();
			SeqUtils.transformSpan(ispan, other_residue_span, other_seq, sym);
			boolean opposite_strands = this_comp_span.isForward() ^ other_comp_span.isForward();
			boolean resultForward = opposite_strands ^ this_residue_span.isForward();
			String spanResidues;
			if (resultForward) {
				spanResidues = other_seq.getResidues(other_residue_span.getMin(), other_residue_span.getMax());
			} else {
				spanResidues = other_seq.getResidues(other_residue_span.getMax(), other_residue_span.getMin());
			}
			if (spanResidues != null) {
				int offset = ispan.getMin() - this_residue_span.getMin();
				System.arraycopy(
						spanResidues.toCharArray(), 0, residues, offset, spanResidues.length());
			}
		} else {
			// recurse to children
			for (int i = 0; i < symCount; i++) {
				SeqSymmetry childSym = sym.getChild(i);
				getResiduesFromComposition(this_residue_span, childSym, residues);
			}
		}
	}
	public void setResidues(String residues) {
		if (residues.length() != this.length) {
			System.out.println("*** WARNING!!! lengths disagree: residues = " + residues.length() +
					", seq = " + this.length + " ****");
		}
		this.residues = residues;
		this.length = residues.length();
	}

	/**
	 * Returns true if all residues on the sequence are available.
	 *
	 * @return true if all residues on the sequence are available.
	 */
		public boolean isComplete() {
			return isComplete(start, end);
		}
	/**
	 * Returns true if all residues between start and end are available.  Unknown
	 * if implementations of this function are inclusive or exclusive on start
	 * and end.
	 * <p />
	 * <em>WARNING:</em> This implementation is flawed.  It only verifies that
	 * all SeqSymmetrys are complete, not that the SeqSymmetrys completely
	 * cover the range in question.
	 *
	 * @param  start the start index (inclusive?)
	 * @param  end   the end index (exclusive?)
	 * @return       true if all residues betwen start and end are available
	 */
	public boolean isComplete(int start, int end) {
		if (residues_provider != null || residues != null) {
			return true;
		}
			// assuming that if all sequences the composite is composed of are
		//    complete, then composite is also complete
		//    [which is an invalid assumption! Because that further assumes that composed seq
		//     is fully covered by the sequences that it is composed from...]
		SeqSymmetry rootsym = this.getComposition();
		if (rootsym == null) {
			return false;
		}

		int comp_count = rootsym.getChildCount();
		if (comp_count == 0) {
			BioSeq other_seq = SeqUtils.getOtherSeq(rootsym, this);
			return other_seq.isComplete(start, end);
		}

		for (int i = 0; i < comp_count; i++) {
			SeqSymmetry comp_sym = rootsym.getChild(i);
			BioSeq other_seq = SeqUtils.getOtherSeq(comp_sym, this);
			if (!other_seq.isComplete()) {
				return false;
			}
		}
		return true;
	}

	public String substring(int start, int end) {
		if (residues_provider == null) {
			return this.getResidues(start, end);
		}
		return residues_provider.substring(start, end);
	}

	public int indexOf(String str, int fromIndex) {
		if (residues_provider == null) {
			return this.getResidues().indexOf(str, fromIndex);
		}
		return residues_provider.indexOf(str, fromIndex);
	}
	
	@Override
	public String toString() {
		return this.getID();
	}


	/**
	 * Add residues to composition (full sequence loaded).
	 * @param aseq
	 */
	public static void addResiduesToComposition(BioSeq aseq) {
		if (aseq.getResiduesProvider() != null) {
			SeqSpan span = new SimpleSeqSpan(0, aseq.getResiduesProvider().getLength(), aseq);
			BioSeq subseq = new BioSeq(
					aseq.getID() + ":" + span.getMin() + "-" + span.getMax(), aseq.getVersion(), aseq.getResiduesProvider().getLength());
			subseq.setResiduesProvider(aseq.getResiduesProvider());
			addSubseqToComposition(aseq, span, subseq);
			return;
		}
		String residues = aseq.getResidues();
		SeqSpan span = new SimpleSeqSpan(0, residues.length(), aseq);
		addResiduesToComposition(aseq, residues, span);
	}


	/**
	 * Adds the residues to the composite sequence.  This allows merging of subsequences.
	 * @param aseq
	 * @param residues
	 * @param span
	 */
	public static void addResiduesToComposition(BioSeq aseq, String residues, SeqSpan span) {
		BioSeq subseq = new BioSeq(
				aseq.getID() + ":" + span.getMin() + "-" + span.getMax(), aseq.getVersion(), residues.length());
		subseq.setResidues(residues);
		addSubseqToComposition(aseq, span, subseq);
	}

	private static void addSubseqToComposition(BioSeq aseq,SeqSpan span, BioSeq subSeq) {
		SeqSpan subSpan = new SimpleSeqSpan(0, span.getLength(), subSeq);
		SeqSpan mainSpan = span;
		MutableSeqSymmetry subsym = new SimpleMutableSeqSymmetry();
		subsym.addSpan(subSpan);
		subsym.addSpan(mainSpan);
		MutableSeqSymmetry compsym = (MutableSeqSymmetry) aseq.getComposition();
		if (compsym == null) {
			//No children.  Add one.
			compsym = new SimpleMutableSeqSymmetry();
			compsym.addChild(subsym);
			compsym.addSpan(new SimpleSeqSpan(mainSpan.getMin(), mainSpan.getMax(), aseq));
			aseq.setComposition(compsym);
		} else {
			// Merge children that already exist.
			compsym.addChild(subsym);
			SeqSpan compspan = compsym.getSpan(aseq);
			int compmin = Math.min(compspan.getMin(), span.getMin());
			int compmax = Math.max(compspan.getMax(), span.getMax());
			SeqSpan new_compspan = new SimpleSeqSpan(compmin, compmax, aseq);
			compsym.removeSpan(compspan);
			compsym.addSpan(new_compspan);
		}
	}
}
