package com.affymetrix.genometryImpl.comparator;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class StringVersionDateComparator implements Comparator<String>, Serializable {
	public static final long serialVersionUID = 1l;

	private static final String[] month_array = {"Jan",
		"Feb",
		"Mar",
		"Apr",
		"May",
		"Jun",
		"Jul",
		"Aug",
		"Sep",
		"Oct",
		"Nov",
		"Dec"};
	private static final List<String> months = Arrays.asList(month_array);

	public int compare(String name1, String name2) {
		String[] parts1 = name1.split("_");
		String[] parts2 = name2.split("_");
		int count1 = parts1.length;
		int count2 = parts2.length;
		String yearA = parts1[count1 - 1];
		String yearB = parts2[count2 - 1];

		int year1 = -1;
		int year2 = -1;
		try {
			year1 = Integer.parseInt(yearA);
		} catch (Exception ex) {
		}
		try {
			year2 = Integer.parseInt(yearB);
		} catch (Exception ex) {
		}
		if (year1 == -1 && year2 == -1) {
			// if neither parses as an integer, then they are considered equal for sorting
			return 0;
		}
		if (year1 == -1) {
			return 1;
		}
		if (year2 == -1) {
			return -1;
		} // want to sort so more recent years are sorted to top
		if (year1 > year2) {
			return -1;
		} // year1 is more recent
		if (year2 > year1) {
			return 1;
		} // year2 is more recent
		// year part is same for both group IDs
		// therefore can't determine order from year part, trying month part
		String monthA = parts1[count1 - 2];
		String monthB = parts2[count2 - 2];

		int month1 = months.indexOf(monthA);
		int month2 = months.indexOf(monthB);
		if (month1 == -1 && month2 == -1) {
			return 0;
		}
		if (month1 == -1) {
			return 1;
		}
		if (month2 == -1) {
			return -1;
		}
		return ((Integer)month2).compareTo(month1);

	}
}
