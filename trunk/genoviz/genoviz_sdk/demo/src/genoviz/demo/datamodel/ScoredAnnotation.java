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

public class ScoredAnnotation extends Annotation implements ScoredAnnotationI {

	double score;

	public ScoredAnnotation(int start, int end, double score) {
		super(start, end);
		this.score = score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	public double getScore() {
		return score;
	}

	public double getScore(int index) {
		return score;
	}

	@Override
	public String toString() {
		return (super.toString() + " : Score = " + score);
	}

}
