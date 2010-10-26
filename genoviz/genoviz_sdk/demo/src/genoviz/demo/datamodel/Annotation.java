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

package genoviz.demo.datamodel;

public class Annotation implements AnnotationI  {
	protected int start, end;
	/** 
	  forward is defined by (start <= end)
	  so forward field is really just a convenience
	  Also, because of this, setForward() should adjust the 
	  start and end as well
	  */
	protected boolean forward;
	protected String type;

	public Annotation() {
	}

	public Annotation(int start, int end) {
		forward = (start <= end);
		this.start = start;
		this.end = end;
	}

	public int getStart() {
		return start;
	}

	public int getEnd() {
		return end;
	}

	public void setStart(int start) {
		this.start = start;
		forward = (start <= end);
	}

	public void setEnd(int end) {
		this.end = end;
		forward = (start <= end);
	}

	public void setForward(boolean forward) {
		/** 
		  if forward is changed from what it was, the start and end (by definition)
		  must also switch
		  */
		if (forward != this.forward) {
			int temp;
			temp = start; start = end; end = temp;
			this.forward = forward;
		}
	}

	public boolean isForward() {
		return forward;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	} 

	public String toString() {
		return (getType() + "  " + start + " " + end);
	}
}
