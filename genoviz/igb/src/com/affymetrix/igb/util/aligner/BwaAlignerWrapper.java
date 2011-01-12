/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import java.io.IOException;
import javax.swing.JComponent;

/**
 *
 * @author Elmo
 */
public class BwaAlignerWrapper extends AlignerWrapper {

	@Override
	public void runAligner(JComponent componentToUpdate, Object[] dataForComponentUpdate, int updateInterval) throws IOException {
		throw new UnsupportedOperationException("Not supported yet.");
	}

	@Override
	public String[] generateExecutionParameters() {
		throw new UnsupportedOperationException("Not supported yet.");
	}

}
