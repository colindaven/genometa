package com.affymetrix.igb.util;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;

/**
 * This class extends JFileChooser and overrides the method approveSelection in
 * order to show up an OptionDialog if the selected file already exists.
 * 
 * @author stefan
 */
public class JFileChooserWithOverwriteWarning extends JFileChooser {
	
	@Override
	public void approveSelection() {
		File selectedFile = getSelectedFile();
		if (getDialogType() == SAVE_DIALOG && selectedFile != null && selectedFile.exists()) {
			int result = JOptionPane.showOptionDialog(null, "File " + selectedFile.getName() + " already exists. Do you really want to overwrite?",
													"Overwrite Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);

			if (result == JOptionPane.NO_OPTION) { return; }
		}
		super.approveSelection();
	}
}
