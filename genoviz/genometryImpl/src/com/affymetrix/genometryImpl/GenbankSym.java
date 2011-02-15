package com.affymetrix.genometryImpl;

import cern.colt.list.IntArrayList;
import com.affymetrix.genometryImpl.symmetry.SingletonSeqSymmetry;
import com.affymetrix.genometryImpl.span.SimpleSeqSpan;
import java.util.*;

public final class GenbankSym implements SeqSpan, SupportsCdsSpan, SymWithProps  {
	private final BioSeq seq; // "chrom"
	private final int txMin; // "chromStart"
	private final int txMax; // "chromEnd"
	private String ID; // "name"
	boolean forward; // "strand"
	int cdsMin = Integer.MIN_VALUE;  // "thickStart" (if = Integer.MIN_VALUE then cdsMin not used)
	int cdsMax = Integer.MIN_VALUE;  // "thickEnd" (if = Integer.MIN_VALUE then cdsMin not used)
	IntArrayList blockMins; // "blockStarts" + "txMin"
	IntArrayList blockMaxs; // "blockStarts" + "txMin" + "blockSizes"
	//IntArrayList CDSblockMins; // "blockStarts" + "txMin"
	//IntArrayList CDSblockMaxs; // "blockStarts" + "txMin" + "blockSizes"
	private String type;	// method
	Map<String,Object> props;

	/**
	 *  Constructs a SeqSymmetry optimized for BED-file format.
	 *  This object is optimized for the case where all optional columns in the
	 *  bed file are used.  If you are using only the first few columns, it would
	 *  be more efficient to use a different SeqSymmetry object.
	 *  @param cdsMin the start of the CDS region, "thinEnd", or Integer.MIN_VALUE.
	 *         If cdsMin = Integer.MIN_VALUE or cdsMin = cdsMax, then there is no CDS.
	 *  @param cdsMax the end of the CDS region, "thickEnd", or Integer.MIN_VALUE.
	 *  @param score an optional score, or Float.NEGATIVE_INFINITY to indicate no score.
	 */
	public GenbankSym(String type, BioSeq seq, int min, int max, String ID) {
		this.type = type;
		this.seq = seq;  // replace chrom name-string with reference to chrom BioSeq
		this.ID = ID;
		this.forward = min < max;
		if (forward) {
			this.txMin = min-1;	// interbase
			this.txMax = max;
		} else {
			this.txMax = min;
			this.txMin = max-1;	// interbase
		}
	}

	/**
	 *  Returns true if the cds was specified in the constructor with valid values.
	 *  If cdsMin = cdsMax = Integer.MIN_VALUE, or if cdsMin = cdsMax, then there is no CDS.
	 *
	 */
	public boolean hasCdsSpan() { return cdsMin != Integer.MIN_VALUE; }
	public SeqSpan getCdsSpan() {
		if (! hasCdsSpan()) { return null; }
		if (forward) { return new SimpleSeqSpan(cdsMin, cdsMax, seq); }
		else { return new SimpleSeqSpan(cdsMax, cdsMin, seq); }
	}

	public String getID() { return ID; }
	public void setID(String ID) { this.ID = ID; }


	public SeqSpan getSpan(BioSeq bs) {
		if (bs.equals(this.seq)) { return this; }
		else { return null; }
	}

	public SeqSpan getSpan(int index) {
		if (index == 0) { return this; }
		else { return null; }
	}

	public boolean getSpan(BioSeq bs, MutableSeqSpan span) {
		if (bs.equals(this.seq)) {
			if (forward) {
				span.set(txMin, txMax, seq);
			}
			else {
				span.set(txMax, txMin, seq);
			}
			return true;
		}
		else { return false; }
	}

	public boolean getSpan(int index, MutableSeqSpan span) {
		if (index == 0) {
			if (forward) {
				span.set(txMin, txMax, seq);
			}
			else {
				span.set(txMax, txMin, seq);
			}
			return true;
		}
		else { return false; }
	}

	/** Always returns 1. */
	public int getSpanCount() { return 1; }

	/** Returns null if index is not 1. */
	public BioSeq getSpanSeq(int index) {
		if (index == 0) { return seq; }
		else { return null; }
	}

	public int getChildCount() {
		if (blockMins == null)  { return 0; }
		else  { return blockMins.size(); }
	}

	public SeqSymmetry getChild(int index) {
		if (blockMins == null || (blockMins.size() <= index)) { return null; }
		// convert to interbase
		if (forward) {
			// blockMins are in seq coordinates, NOT relative to txMin
			// convert to interbase
			return new GenbankChildSingletonSeqSym(blockMins.get(index)-1, blockMaxs.get(index), seq);
		}
		// convert to interbase
		return new GenbankChildSingletonSeqSym(blockMaxs.get(index)-1, blockMins.get(index), seq);
	}

	public void addBlock(int start, int end) {
		if (blockMins == null) {
			blockMins = new IntArrayList();
			blockMaxs = new IntArrayList();
		}
		blockMins.add(start);
		blockMaxs.add(end);
	}

	public void addCDSBlock(int start, int end) {
		// TODO: allow list of CDS blocks, rather than just start and end.
		
		/*if (CDSblockMins == null) {
			CDSblockMins = new IntArrayList();
			CDSblockMaxs = new IntArrayList();
		}*/
		if (forward) {
			//if (CDSblockMins.isEmpty()) {
			if (cdsMin == Integer.MIN_VALUE) {
				cdsMin = start-1;	//interbase
				cdsMax = end;
			}
			//CDSblockMins.add(start-1);	//interbase
			//CDSblockMaxs.add(end);
			cdsMin = Math.min(cdsMin, start-1);	//interbase
			cdsMax = Math.max(cdsMax, end);

		} else {
			//if (CDSblockMins.isEmpty()) {
			if (cdsMin == Integer.MIN_VALUE) {
				cdsMin = end-1;	//interbase
				cdsMax = start;
			}
			//CDSblockMaxs.add(start);
			//CDSblockMins.add(end);	// interbase
			cdsMin = Math.min(cdsMin, end-1);	//interbase
			cdsMax = Math.max(cdsMax, start);
		}
	}

	class GenbankChildSingletonSeqSym extends SingletonSeqSymmetry implements SymWithProps {
		public GenbankChildSingletonSeqSym(int start, int end, BioSeq seq) {
			super(start, end, seq);
		}

		// For the web links to be constructed properly, this class must implement getID(),
		// or must NOT implement SymWithProps.
		public String getID() {return GenbankSym.this.getID();}
		public Map<String,Object> getProperties() {return GenbankSym.this.getProperties();}
		public Map<String,Object> cloneProperties() {return GenbankSym.this.cloneProperties();}
		public Object getProperty(String key) {return GenbankSym.this.getProperty(key);}
		public boolean setProperty(String key, Object val) {return GenbankSym.this.setProperty(key, val);}
	}

	// SeqSpan implementation
	public int getStart() { return (forward ? txMin : txMax); }
	public int getEnd() { return (forward ? txMax : txMin); }
	public int getMin() { return txMin; }
	public int getMax() { return txMax; }
	public int getLength() { return (txMax - txMin); }
	public boolean isForward() { return forward; }
	public BioSeq getBioSeq() { return seq; }
	public double getStartDouble() { return (double)getStart(); }
	public double getEndDouble() { return (double)getEnd(); }
	public double getMaxDouble() { return (double)getMax(); }
	public double getMinDouble() { return (double)getMin(); }
	public double getLengthDouble() { return (double)getLength(); }
	public boolean isIntegral() { return true; }

	public Map<String,Object> getProperties() {
		return cloneProperties();
	}

	public Map<String,Object> cloneProperties() {
		HashMap<String,Object> tprops = new HashMap<String,Object>();
		tprops.put("id", ID);
		tprops.put("type", type);
		if (props != null) {
			tprops.putAll(props);
		}
		return tprops;
	}

	public Object getProperty(String key) {
		// test for standard gene sym  props
		if (key.equals("id")) { return ID; }
		else if (key.equals("type")) { return type; }
		else if (props != null)  {
			return props.get(key);
		}
		else  { return null; }
	}

	public boolean setProperty(String name, Object val) {
		if (props == null) {
			props = new Hashtable<String,Object>();
		}
		props.put(name, val);
		return true;
	}

	public void setStart(int start) {	}

	public void setEnd(int end) {	}

}