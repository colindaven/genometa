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

import java.io.DataInputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AffyDataGroup {

	/** File position of the NEXT data group. */
	long file_pos;

	/** File position of the first data set within the group. */
	long file_first_dataset_pos;

	/** Number of data sets within the data group. */
	int num_datasets;

	/** Name of the data group. */
	String name;

	List<AffyDataSet> dataSets = new ArrayList<AffyDataSet>();

	AffyGenericChpFile chpFile;

	/** Creates a new instance of AffyDataGroup */
	protected AffyDataGroup(AffyGenericChpFile chpFile) {
		this.chpFile = chpFile;
	}

	public AffyDataGroup(int pos, int data_pos, int sets, String name) {
		this.file_first_dataset_pos = data_pos;
		this.file_pos = pos;
		this.num_datasets = sets;
		this.name = name;
	}

	public static AffyDataGroup parse(AffyGenericChpFile chpFile, DataInputStream dis) throws IOException {
		AffyDataGroup group = new AffyDataGroup(chpFile);

		group.file_pos = dis.readInt(); //TODO: should be UINT32, but unlikely to matter
		group.file_first_dataset_pos = dis.readInt(); // UINT32
		group.num_datasets = dis.readInt(); // INT32
		group.name = AffyGenericChpFile.parseWString(dis);

		Logger.getLogger(AffyDataGroup.class.getName()).log(
							Level.FINE, "Parsing group: pos={0}, name={1}, datasets={2}", new Object[]{group.file_pos, group.name, group.num_datasets});

		if (group.num_datasets > 1) {
			//TODO: figure out why there is a bug in parsing multiple datasets.
			// There seems to be some difference between the format specification and
			// the actual file contents.
			//throw new IOException("Cannot parse CHP files with more than one dataset.");
		}

		for (int i=0; i<1 && i<group.num_datasets; i++) {
			AffyDataSet data = new AffyDataSet(chpFile);
			data.parse(chpFile, dis);
			group.dataSets.add(data);
		}

		return group;
	}

	public List<AffyDataSet> getDataSets() {
		return dataSets;
	}

	@Override
		public String toString() {
			return "AffyDataGroup: pos: " + file_pos + ", first_dataset_pos: " + file_first_dataset_pos 
				+ ", datasets: " + num_datasets + ", name: " + name;
		}

}
