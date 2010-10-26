/**
 *   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.parsers.gchp;

import cern.colt.list.ByteArrayList;
import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import cern.colt.list.ShortArrayList;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.SeqSymmetry;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

final class AffySingleChromData {

	private final int start;
	private final int rowCount;
	String displayName; // AND by a display name.

	private List<AffyChpColumnData> columns = new ArrayList<AffyChpColumnData>();
	private final AffyGenericChpFile chpFile;
	AffyDataSet dataSet;

	/** Creates a new instance of SingleChromosomeData */
	public AffySingleChromData(AffyGenericChpFile chpFile, AffyDataSet dataSet, 
			int chromNum, String chromDisplayName, int start, int count,
			List<AffyChpColumnData> columns) {
		this.chpFile = chpFile;
		this.dataSet = dataSet;
		this.displayName = chromDisplayName;
		this.start = start;
		this.rowCount = count;
		this.columns = columns;
	}

	@Override
	public String toString() {
		return this.getClass().getName() + " [displayName=" + displayName + ", start=" + start + ", count=" + rowCount + ", columns=" + columns.size() + "]";
	}

	private void parse(DataInputStream dis) throws IOException {
		Logger.getLogger(AffySingleChromData.class.getName()).log(
				Level.FINE, "Parsing chromData: {0}, {1}", new Object[]{this.displayName, this.rowCount});
		for (int row=0; row < rowCount; row++) {
			for (AffyChpColumnData col : columns) {
				col.addData(dis);
			}
		}
	}

	private void skip(DataInputStream dis) throws IOException {
		int rowSize = totalRowSize(columns);
		long skipSize = (long)rowCount * rowSize;	// cast to long before multiplying to avoid possible overflow

		while (skipSize > 0) {
			long skipped = dis.skip(skipSize);
			skipSize -= skipped;
		}     
	}

	private static int totalRowSize(List<AffyChpColumnData> columns) {
		int rowSize = 0;
		for (AffyChpColumnData col : columns) {
			rowSize += col.getByteLength();
		}
		return rowSize;
	}

	void parseOrSkip(DataInputStream dis) throws IOException {
		if (this.chpFile.getLoadPolicy().shouldLoadChrom(displayName)) {
			parse(dis);
		} else {
			skip(dis);
		}
	}

	/** Creates GraphSyms that can be added as annotations to the BioSeq. */
	public List<SeqSymmetry> makeGraphs(BioSeq seq) throws IOException {
		List<SeqSymmetry> results = new ArrayList<SeqSymmetry>(columns.size());

		ArrayList probeSetNames = (ArrayList) columns.get(0).getData();
		probeSetNames.trimToSize();

		// column 2 contains chromosome number, but we already know that information so ignore it.

		IntArrayList positions = (IntArrayList) columns.get(2).getData();
		positions.trimToSize();

		if (positions.size() <= -1) {
			return results;
		}

		// add a graph even if the data is of length 0
		// because we want something to be visible in the display, even if it is
		// simply a graph handle and axis with no graph data to draw.

		// In a cnchp file, the first three columns contain non-graph data
		// so skip them and make graphs from all the other columns
		//TODO: maybe make this more generic for all "generic" chp files
		for (AffyChpColumnData colData : columns.subList(3, columns.size())) {
			String graphId = colData.name;
			float[] y;
			if (colData.getData() instanceof FloatArrayList) {
				List<Object> trimmedXandY = trimNaN(positions, (FloatArrayList) colData.getData());
				IntArrayList xlist = (IntArrayList) trimmedXandY.get(0);
				FloatArrayList flist = (FloatArrayList) trimmedXandY.get(1);

				xlist.trimToSize();
				flist.trimToSize();
				GraphSym gsym = new GraphSym(xlist.elements(), flist.elements(), graphId, seq);
				results.add(gsym);
			} else if (colData.getData() instanceof IntArrayList) {
				IntArrayList ilist = (IntArrayList) colData.getData();
				ilist.trimToSize();
				y = new float[ilist.size()];
				for (int i=0;i<ilist.size();i++) {
					y[i] = ilist.get(i);
				}
				GraphSym gsym = new GraphSym(positions.elements(), y, graphId, seq);
				results.add(gsym);
			} else if (colData.getData() instanceof ShortArrayList) {
				GraphSym gsym;
				if (colData.name.startsWith("CNState")) {
					// In the "CNStateMin" and "CNStateMax" graphs, the number "255"
					// is used to represent "unknown".  These x,y pairs should be discarded.
					List<Object> trimmedXandY = trim255(positions, (ShortArrayList) colData.getData());
					IntArrayList xlist = (IntArrayList) trimmedXandY.get(0);
					ShortArrayList ilist = (ShortArrayList) trimmedXandY.get(1);

					xlist.trimToSize();
					ilist.trimToSize();
					y = new float[ilist.size()];
					for (int i = 0; i < ilist.size(); i++) {
						y[i] = ilist.get(i);
					}
					gsym = new GraphSym(xlist.elements(), y, graphId, seq);
				} else {
					ShortArrayList ilist = (ShortArrayList) colData.getData();
					ilist.trimToSize();
					y = new float[ilist.size()];
					for (int i = 0; i < ilist.size(); i++) {
						y[i] = ilist.get(i);
					}
					gsym = new GraphSym(positions.elements(), y, graphId, seq);
				}
				results.add(gsym);
			} else if (colData.getData() instanceof ByteArrayList) {
				ByteArrayList ilist = (ByteArrayList) colData.getData();
				ilist.trimToSize();
				y = new float[ilist.size()];
				for (int i = 0; i < ilist.size(); i++) {
					y[i] = ilist.get(i);
				}
				GraphSym gsym = new GraphSym(positions.elements(), y, graphId, seq);
				results.add(gsym);
			} else {
				Logger.getLogger(
						AffySingleChromData.class.getName()).log(
						Level.SEVERE, "Don''t know how to make a graph for data of type: {0}", colData.type);
			}
		}

		return results;
	}

	/** Removes x,y pairs where the y-value is invalid (NaN or Infinite).
	 *  Returns a List containing one IntList and one FloatList. 
	 *  If there were no invalid values of y, the output IntList and FloatList
	 *  will be the same objects as the input, otherwise they will both be
	 *  new objects.  If the given FloatList contains ONLY invalid values,
	 *  then the returned IntList and FloatList will both be empty.
	 */
	private List<Object> trimNaN(IntArrayList x, FloatArrayList y) {
		if (x.size() != y.size()) {
			throw new IllegalArgumentException("Lists must be the same size " + x.size() + " != " + y.size());
		}

		boolean had_bad_values = false;
		IntArrayList x_out = new IntArrayList(x.size());
		FloatArrayList y_out = new FloatArrayList(y.size());

		for (int i=0; i<x.size(); i++) {
			float f = y.get(i);
			if (Float.isNaN(f) || Float.isInfinite(f)) {
				had_bad_values = true;
			} else {
				x_out.add(x.get(i));
				y_out.add(f);
			}
		}

		if (had_bad_values) {
			return Arrays.<Object>asList(x_out, y_out);
		} else {
			return Arrays.<Object>asList(x, y);
		}
	}

	/** Removes x,y pairs where the y-value is invalid (byte = 255).
	 *  Returns a List containing one IntList and one ShortList. 
	 *  If there were no invalid values of y, the output IntList and ShortList
	 *  will be the same objects as the input, otherwise they will both be
	 *  new objects.  If the given ShortList contains ONLY invalid values,
	 *  then the returned IntList and 3hortList will both be empty.
	 */
	private List<Object> trim255(IntArrayList x, ShortArrayList y) {
		if (x.size() != y.size()) {
			throw new IllegalArgumentException("Lists must be the same size " + x.size() + " != " + y.size());
		}

		boolean had_bad_values = false;
		IntArrayList x_out = new IntArrayList(x.size());
		ShortArrayList y_out = new ShortArrayList(y.size());

		for (int i=0; i<x.size(); i++) {
			short f = y.get(i);
			if (f == 255) {
				had_bad_values = true;
			} else {
				x_out.add(x.get(i));
				y_out.add(f);
			}
		}

		if (had_bad_values) {
			return Arrays.<Object>asList(x_out, y_out);
		} else {
			return Arrays.<Object>asList(x, y);
		}
	}
}
