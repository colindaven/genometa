package com.affymetrix.genometryImpl.parsers;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;

/**
 * IndexWriter interface.
 * To use this interface, a parser must have:
 * 1.  The ability to write a single symmetry
 * 2.  The ability to sort its symmetries.
 * @author jnicol
 */
public interface IndexWriter {
	/**
	 * Write a single symmetry to the file.
	 * It is assumed that the file only uses one chromosome.
	 * @param sym
	 * @param seq - necessary for backwards compatibility.
	 * @param dos
	 * @throws IOException
	 */
	public void writeSymmetry(SeqSymmetry sym, BioSeq seq, OutputStream dos) throws IOException;

	/**
	 * Parse the given stream, returning a list of SeqSymmetries.
	 * @return list of SeqSymmetries.
	 */
	public List<? extends SeqSymmetry> parse(DataInputStream dis, String annot_type, AnnotatedSeqGroup group);

	/**
	 * Get a comparator for the class.
	 * @return comparator.
	 */
	public Comparator<? extends SeqSymmetry> getComparator(BioSeq seq);

	/**
	 * Get the minimum of a given symmetry.
	 * @param sym
	 * @param seq
	 * @return integer
	 */
	public int getMin(SeqSymmetry sym, BioSeq seq);

	/**
	 * Get the maximum of a given symmetry.
	 * @param sym
	 * @param seq
	 * @return integer
	 */
	public int getMax(SeqSymmetry sym, BioSeq seq);

	/**
	 * Get the preferred formats.
	 * 
	 */
	public List<String> getFormatPrefList();
}
