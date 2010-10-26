/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package com.affymetrix.genoviz.datamodel;

import java.util.ArrayList;
import java.util.List;

/**
 * A model for the base calls from a trace.
 * Functionality is being moved over from TraceI,
 * to allow for traces with multiple non-agreeing calls
 *
 * Each base has 2 important features,
 * the called neucleotide and the position of the call in sample space.
 *
 * Note: In Trace
 * the terms Sample Index, Sample Point and Trace Point are identical.
 * The term "trace point" is perfered.
 */

public class BaseCalls {

	private List<BaseCall> baseVector;  // vector of BaseCalls
	private TraceI trace;

	/** optional datamodel for aligning bases. */
	private Mapping aligner;
	public Mapping getAligner() {
		return aligner;
	}
	public void setAligner( Mapping aligner ) {
		this.aligner = aligner;
	}


	public BaseCalls() {
		baseVector = new ArrayList<BaseCall>();
	}

	public BaseCalls( BaseCall[] base_calls ) {
		this();
		setBaseCalls( base_calls );
	}

	public void setTrace( TraceI trace ) {
		this.trace = trace;
	}

	public TraceI getTrace() {
		return this.trace;
	}

	public void setBaseCalls( BaseCall[] theCalls ){
		int iBaseCount = this.baseVector.size();
		baseVector = new ArrayList<BaseCall>(iBaseCount);
		for ( int i = 0; i < theCalls.length; i++ ) {
			addBase( theCalls[i] );
		}
	}

	public BaseCall[] getBaseCalls () {
		BaseCall[] cba = new BaseCall[baseVector.size()];
		this.baseVector.toArray(cba);
		return cba;
	}

	public BaseCall getBaseCall( int index ) {
		return baseVector.get(index);
	}


	public int getBaseCount(){
		return baseVector.size();
	}

	public String getBaseString(){
		StringBuffer residue = new StringBuffer();
		for (BaseCall cb : this.baseVector) {
			char base = cb.getBase();
			residue.append( base );
		}
		return residue.toString();
	}

	/**
	 * constructs the reverse complement of this set of base calls.
	 *
	 * @return a new set of base calls.
	 */
	public BaseCalls reverseComplement() {
		int traceLength = 0;
		if ( null != this.trace ) {
			traceLength = this.trace.getTraceLength();
		}
		BaseCalls revBaseCalls = new BaseCalls();
		revBaseCalls.setTrace( this.getTrace() );
		int iCount = getBaseCount();
		for (int i = iCount - 1; i >= 0; i--) {
			BaseCall base = baseVector.get(i).reverseComplement( traceLength );
			revBaseCalls.addBase(base);
		}
		return revBaseCalls;
	}

	/**
	 * adds a single called base.
	 * Bases should be added in order from 5 prime to 3 prime.
	 *
	 * @param base the one to add.
	 */
	public void addBase(BaseCall base) {
		baseVector.add(base);
	}

	public BaseCall getBaseCallAtTracePoint( int tracePoint ){
		return getBaseCall( getBaseIndexAtTracePoint( tracePoint ) );
	}

	public int getBaseIndexAtTracePoint( int tracePoint ) {
		// use binary search to converge towards base call index
		int low = 0;
		int high = getBaseCount() - 1;

		while (low <= high) {
			int mid =(low + high)/2;
			BaseCall midBase = getBaseCall( mid );
			int midVal = midBase.getTracePoint();

			if (midVal < tracePoint)
				low = mid + 1;
			else if (midVal > tracePoint)
				high = mid - 1;
			else
				return mid; // hit it exactly
		}

		// by this time, high <= low
		//    System.err.println( "Finding base for point: " + tracePoint +" high= " + high +" low= " + low );
		// test for boundry conditions
		if( high < 0 ) {
			return low;
		}
		if( low >= getBaseCount() ) {
			return high;
		}

		int lowPoint = getBaseCall( low ).getTracePoint();
		int highPoint = getBaseCall( high ).getTracePoint();
		// BasePoint[high] < tracePoint < BasePoint[low].
		// Which one is closer?  Test for proximity
		if( tracePoint < ( highPoint + lowPoint ) / 2 ) {
			return high;
		}
		else {
			return low;
		}
	}


}
