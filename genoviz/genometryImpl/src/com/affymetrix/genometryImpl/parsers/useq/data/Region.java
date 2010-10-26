package com.affymetrix.genometryImpl.parsers.useq.data;
import java.util.*;
import java.io.*;

/**
 * Simple start stop object. Assumes interbase coordinates. 
 * @author david.nix@hci.utah.edu*/
public class Region implements Comparable<Region>, Serializable {
	//fields
	private static final long serialVersionUID = 1L;
	protected int start;
	protected int stop;

	//constructors
	public Region (int start, int stop){
		this.start = start;
		this.stop = stop;
	}

	public String toString(){
		return start+"\t"+stop;
	}
	/**Assumes coordinates are interbase.*/
	public boolean intersects (int start, int stop){
		if (stop <= this.start || start >= this.stop) return false;
		return true;
	}

	/**Checks to see if each start is <= the stop*/
	public static boolean checkStartStops(Region[] ss){
		for (int i=0; i< ss.length; i++){
			if (ss[i].start> ss[i].stop) return false;
		}
		return true;
	}
	/**Returns a Region[] for regions defined with baseHitCounts !=0.
	 * Coordinates are interbase.*/
	public static Region[] makeStartStops(short[] baseHitCount){
		ArrayList<Region> ss = new ArrayList<Region>();
		//find first non zero base
		int i=0;
		for (; i< baseHitCount.length; i++) if (baseHitCount[i]!=0) break;
		if (i == baseHitCount.length) return null;
		int start = i;
		int val = baseHitCount[start];
		//find different base
		for (; i< baseHitCount.length; i++){
			if (baseHitCount[i]!=val) {
				//make SS
				if (val!=0) ss.add(new Region(start,i));
				start = i;
				val = baseHitCount[start];
			}
		}
		//add last
		if (val!=0) ss.add(new Region(start,i));
		Region[] ssA = new Region[ss.size()];
		ss.toArray(ssA);
		return ssA;
	}

	/**Sorts by start base, then by length, smaller to larger for both.*/
	public int compareTo(Region se){
		if (start<se.start) return -1;
		if (start>se.start) return 1;
		// if same start, sort by length, smaller to larger
		int len = stop-start;
		int otherLen = se.stop-se.start;
		if (len<otherLen) return -1;
		if (len>otherLen) return 1;
		return 0;
	}

	/**Returns the total number of bases, assumes interbase coordinates.*/
	public static int totalBP (Region[] ss){
		int total = 0;
		for (int i=0; i< ss.length; i++) total += ss[i].getLength();
		return total;
	}

	/**Returns the starts in number of bases, not coordinates for the array.*/
	public static int[] startsInBases (Region[] ss){
		int[] indexes = new int[ss.length];
		int index = 0;
		for (int i=0; i< ss.length; i++) {
			index += ss[i].getLength();
			indexes[i] = index;
		}
		return indexes;
	}

	/**Assumes interbase coordinates.*/
	public int getLength(){
		return stop-start;
	}

	public int getStop() {
		return stop;
	}
	public int getStart() {
		return start;
	}
	public int[] getStartStop(){
		return new int[]{start,stop};
	}

	public boolean isContainedBy(int beginningBP, int endingBP) {
		if (start >= beginningBP && stop < endingBP) return true;
		return false;
	}
}
