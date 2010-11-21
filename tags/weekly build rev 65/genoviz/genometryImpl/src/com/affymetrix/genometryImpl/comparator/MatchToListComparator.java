package com.affymetrix.genometryImpl.comparator;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 *  Given a list of Strings in a file (one per line),
 *     compares for sorting two input Strings based on where they are in the list
 *  If one of the two input Strings is not in list, should sort to bottom
 *  Any whitespace at end of Strings in file is trimmed off
 */
public final class MatchToListComparator implements Comparator<String> {
	List<String> match_list = null;

	public MatchToListComparator(String filename) {
		BufferedReader br = null;
		try {
			br = new BufferedReader(new FileReader(new File(filename)));
			match_list = new ArrayList<String>();
			String line;
			while ((line = br.readLine()) != null) {
				if (line.length() == 0 || line.startsWith("#"))  { continue; }
				String match_term = line.trim();
				match_list.add(match_term);
			}
		}
		catch (Exception ex) {
			System.out.println("Error initializing MatchToListComparator: ");
			ex.printStackTrace();
			match_list = null;
		}
		finally {
			GeneralUtils.safeClose(br);
		}
	}

	public int compare(String name1, String name2) {
		if (match_list == null) { return 0; }
		int index1 = match_list.indexOf(name1);
		int index2 = match_list.indexOf(name2);
		if (index1 == -1 && index2 == -1) { return 0; }
		if (index1 == -1) { return 1; }
		if (index2 == -1) { return -1; }
		return ((Integer)index1).compareTo(index2);
	}
}
