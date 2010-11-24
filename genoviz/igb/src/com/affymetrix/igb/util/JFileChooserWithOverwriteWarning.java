package com.affymetrix.igb.util;

import com.affymetrix.genometryImpl.util.ErrorHandler;
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

	private File sourceFile = null;

	/**
	 * Set a file as Source-File to prevent saving a file to itself.
	 * @param sourceFile
	 */
	public void setSourceFile(File sourceFile) {
		this.sourceFile = sourceFile;
	}

	/**
	 * Overwrite approveSelection-Method to show a warning dialog if the user
	 * tries to overwrite an existing file.
	 */
	@Override
	public void approveSelection() {
		File selectedFile = getSelectedFile();
		if (getDialogType() == SAVE_DIALOG && selectedFile != null) {
			// check if a sourceFile is set
			if(this.sourceFile != null) {
				if(selectedFile.equals(this.sourceFile)) {
					ErrorHandler.errorPanel("Overwrite Warning", "Unable to overwrite Source-File!");
					return;
				}
			}
			// check if file already exists
			if(selectedFile.exists()) {
				int result = JOptionPane.showOptionDialog(null, "File " + selectedFile.getName() + " already exists. Do you really want to overwrite?",
													"Overwrite Warning", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE, null, null, null);

				if (result == JOptionPane.NO_OPTION) { return; }
			}
		}
		super.approveSelection();
	}
}
