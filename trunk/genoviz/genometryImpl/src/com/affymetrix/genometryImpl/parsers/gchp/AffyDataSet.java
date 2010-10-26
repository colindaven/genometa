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

import java.io.*;
import java.util.*;
import java.util.logging.Logger;

public final class AffyDataSet {

	private long pos_first_data_element;
	private long pos_next_data_element;
	private String name;
	private int param_count;
	private Map<String,AffyChpParameter> params;
	private int num_columns;
	private List<AffyChpColumnType> columnTypes;
	private long num_rows;

	private final Map<Integer, AffySingleChromData> num2chromData = new LinkedHashMap<Integer,AffySingleChromData>();
	private final List<String> chromosomeNames = new ArrayList<String>();


	private final AffyGenericChpFile chpFile;


	/** Creates a new instance of AffyDataSet */
	protected AffyDataSet(AffyGenericChpFile chpFile) {
		this.chpFile = chpFile;
	}

	public void parse(AffyGenericChpFile chpFile, DataInputStream dis) throws IOException {

		pos_first_data_element = dis.readInt();
		pos_next_data_element = dis.readInt();
		name = AffyGenericChpFile.parseWString(dis);
		param_count = dis.readInt();

		Logger.getLogger(AffyDataSet.class.getName()).fine(
				"Parsing data set: name=" + name);

		params = new LinkedHashMap<String,AffyChpParameter>(param_count);
		for (int i=0; i<param_count; i++) {
			AffyChpParameter param = AffyChpParameter.parse(dis);
			params.put(param.name, param);
		}

		num_columns = dis.readInt();
		columnTypes = new ArrayList<AffyChpColumnType>(num_columns);
		for (int i=0; i<num_columns; i++) {
			AffyChpColumnType col = new AffyChpColumnType(
					AffyGenericChpFile.parseWString(dis), dis.readByte(), dis.readInt());
			columnTypes.add(col);
		}

		num_rows = dis.readInt();

		// look for header lines like "0:start", "1:start", etc., 
		// until there are no more of them.  Sadly, we cannot expect that
		// all numbers in a given range will be present.  The files typically
		// skip number 23.
		for (int chromNum = 0; chromNum < 100; chromNum++) {
			if (params.containsKey(chromNum + ":start")) {
				Integer start = (Integer) params.get(chromNum + ":start").getValue();
				Integer count = (Integer) params.get(chromNum + ":count").getValue();
				String chromName = (String) params.get(chromNum + ":display").getValue();
				chromosomeNames.add(chromName);

				List<AffyChpColumnData> chromDataColumns = new ArrayList<AffyChpColumnData>(columnTypes.size());
				for (AffyChpColumnType setColumn : columnTypes) {
					chromDataColumns.add(new AffyChpColumnData(null, setColumn.name, setColumn.type, setColumn.size));
				}

				AffySingleChromData chromData = new AffySingleChromData(chpFile, this,
						chromNum, chromName, start, count, chromDataColumns);
				Logger.getLogger(AffyDataSet.class.getName()).fine(
						"Made chrom: " + chromData.toString());

				num2chromData.put(chromNum, chromData);
			}
		}

		Logger.getLogger(AffyDataSet.class.getName()).fine(
				"Chromosome Numbers: " + num2chromData.keySet());

		// I am making the assumption that chromosome number n is always stored
		// before chromosome number n+1.  I don't think the documentation makes that
		// specific claim, though.  May be more correct to sort by the ":start" values
		// that were read above.

		for (int chromNum = 0; chromNum < 100; chromNum++) {
			if (num2chromData.containsKey(chromNum)) {
				AffySingleChromData chromData = num2chromData.get(chromNum);
				chromData.parseOrSkip(dis);
			}
		}
	}

	@Override
		public String toString() {
			return "AffyDataSet: first_element: " + pos_first_data_element +
				" next_element: " + pos_next_data_element +
				" name: " + name +
				"\n params: " + params.size() +
				"\n num_columns: " + num_columns +
				"\n num_rows: " + num_rows;
		}

	public void dump(PrintStream str) {
		str.println(this.getClass().getName());
		str.println("  first_element: " + pos_first_data_element);
		str.println("  next_element: " + pos_next_data_element);
		str.println("  name: " + name);
		str.println("  params: " + params.size());
		str.println("  num_columns: " + num_columns);
		str.println("  num_rows: " + num_rows);
		str.println("  Parameters:  ");

		for (AffyChpParameter param : params.values()) {
			param.dump(str);
		}

		str.println("  Column descriptions:  ");
		for (int i=0; i<num_columns; i++) {
			AffyChpColumnType col = columnTypes.get(i);
			col.dump(str);
		}
	}

	List<String> getChromosomeNames() {
		return new ArrayList<String>(chromosomeNames);
	}

	List<AffySingleChromData> getSingleChromData() {
		return new ArrayList<AffySingleChromData>(num2chromData.values());
	}
}
