/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.genometryImpl.util;

/**
 * Class for informations must be available in the whole programm
 * @author Bjoern
 */
public class GenometaGlobals {
	private static int numOfFirstRead;
	private static int numOfLastRead;
	private static boolean memoryOverflow;

	/**
	 * 
	 * @return
	 */
	public static boolean isMemoryOverflow() {
		return memoryOverflow;
	}

	/**
	 *
	 * @param memoryOverflow
	 */
	public static void setMemoryOverflow(boolean memoryOverflow) {
		GenometaGlobals.memoryOverflow = memoryOverflow;
	}

	/**
	 *
	 * @return
	 */
	public static int getNumOfFirstRead() {
		return numOfFirstRead;
	}

	/**
	 *
	 * @param numOfFirstRead
	 */
	public static void setNumOfFirstRead(int numOfFirstRead) {
		GenometaGlobals.numOfFirstRead = numOfFirstRead;
	}

	/**
	 *
	 * @return
	 */
	public static int getNumOfLastRead() {
		return numOfLastRead;
	}

	/**
	 *
	 * @param numOfLastRead
	 */
	public static void setNumOfLastRead(int numOfLastRead) {
		GenometaGlobals.numOfLastRead = numOfLastRead;
	}


}
