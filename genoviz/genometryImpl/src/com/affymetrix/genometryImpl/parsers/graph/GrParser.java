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
package com.affymetrix.genometryImpl.parsers.graph;

import cern.colt.GenericSorting;
import cern.colt.Swapper;
import cern.colt.function.IntComparator;
import cern.colt.list.FloatArrayList;
import cern.colt.list.IntArrayList;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.*;
import java.util.*;


public final class GrParser {

	public static boolean writeGrFormat(GraphSym graf, OutputStream ostr) throws IOException {
		BufferedOutputStream bos = null;
		DataOutputStream dos = null;
		try {
			bos = new BufferedOutputStream(ostr);
			dos = new DataOutputStream(bos);
			writeGraphPoints(graf, dos);
		} finally {
			GeneralUtils.safeClose(dos);
		}
		return true;
	}

	private static void writeGraphPoints(GraphSym graf, DataOutputStream dos) throws IOException {
		int total_points = graf.getPointCount();
		for (int i = 0; i < total_points; i++) {
			dos.writeBytes("" + graf.getGraphXCoord(i) + "\t" +
					graf.getGraphYCoordString(i) + "\n");
		}
	}

	public static GraphSym parse(InputStream istr, BioSeq aseq, String name) throws IOException {
		return parse(istr, aseq, name, true);
	}
	public static GraphSym parse(InputStream istr, BioSeq aseq, String name, boolean ensure_unique_id)
		throws IOException {
		GraphSym graf = null;
		String line = null;
		String headerstr = null;
		boolean hasHeader = false;
		int count = 0;

		IntArrayList xlist = new IntArrayList();
		FloatArrayList ylist = new FloatArrayList();

		InputStreamReader isr = new InputStreamReader(istr);
		BufferedReader br = new BufferedReader(isr);
		// check first line, may be a header for column labels...
		line = br.readLine();
		if (line == null) {
			System.out.println("can't find any data in file!");
			return null;
		}

		try {
			int firstx;
			float firsty;
			if (line.indexOf(' ') > 0) {
				firstx = Integer.parseInt(line.substring(0, line.indexOf(' ')));
				firsty = Float.parseFloat(line.substring(line.indexOf(' ') + 1));
			}
			else if (line.indexOf('\t') > 0) {
				firstx = Integer.parseInt(line.substring(0, line.indexOf('\t')));
				firsty = Float.parseFloat(line.substring(line.indexOf('\t') + 1));
			}
			else {
				System.out.println("format not recognized");
				return null;
			}
			xlist.add(firstx);
			ylist.add(firsty);
			count++;  // first line parses as numbers, so is not a header, increment count
		}
		catch (Exception ex) {
			// if first line does not parse as numbers, must be a header...
			// set header flag, don't count as a line...
			headerstr = line;
			System.out.println("Found header on graph file: " + line);
			hasHeader = true;
		}
		int x = 0;
		float y = 0;
		int xprev = Integer.MIN_VALUE;
		boolean sorted = true;
		while ((line = br.readLine()) != null) {
			if (line.indexOf(' ') > 0) {
				x = Integer.parseInt(line.substring(0, line.indexOf(' ')));
				y = Float.parseFloat(line.substring(line.indexOf(' ') + 1));
			}
			else if (line.indexOf('\t') > 0) {
				x = Integer.parseInt(line.substring(0, line.indexOf('\t')));
				y = Float.parseFloat(line.substring(line.indexOf('\t') + 1));
			}
			xlist.add(x);
			ylist.add(y);
			count++;
			// checking on whether graph is sorted...
			if (xprev > x) { sorted = false; }
			xprev = x;
		}
		if (name == null && hasHeader) {
			name = headerstr;
		}
		int xcoords[] = Arrays.copyOf(xlist.elements(), xlist.size());
		xlist = null;
		float ycoords[] = Arrays.copyOf(ylist.elements(), ylist.size());
		ylist = null;

		if (! sorted) {
			System.err.println("input graph not sorted, sorting by base coord");
			sortXYDataOnX(xcoords,ycoords);
		}
		if (ensure_unique_id)  { name = AnnotatedSeqGroup.getUniqueGraphID(name, aseq); }
		graf = new GraphSym(xcoords, ycoords, name, aseq);
		System.out.println("loaded graph data, total points = " + count);
		return graf;
	}

	/**
	 * Sort xList, yList, and wList based upon xList
	 * @param xList
	 * @param yList
	 * @param wList
	 */
	public static void sortXYDataOnX(final int[] xList, final float[] yList) {
		Swapper swapper = new Swapper() {

			public void swap(int a, int b) {
				int swapInt = xList[a];
				xList[a] = xList[b];
				xList[b] = swapInt;

				float swapFloat = yList[a];
				yList[a] = yList[b];
				yList[b] = swapFloat;
			}
		};
		IntComparator comp = new IntComparator() {
			public int compare(int a, int b) {
				return ((Integer) xList[a]).compareTo(xList[b]);
			}
		};
		GenericSorting.quickSort(0, xList.length, comp, swapper);
	}
}
