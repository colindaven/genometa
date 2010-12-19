/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author paetow
 */
public abstract class AlignerExecutor extends JDialog implements ActionListener{

	//TODO ggf noch nen hilfebutton?? mit ref auf die Aligner manual Page

	protected JLabel indexLabel = new JLabel("Index location:");
	//Textfield to print the actual selected path
	protected JTextField indexTF = new JTextField(30);
	//Button to call the filechooser
	protected JButton indexChooserOpener;
	//Filechooser to locate the index file
	protected JFileChooser indexFileChooser;
	//File Extensions allowed to select an Index
	protected String[] indexFileExtensions;

	protected JLabel readsLabel = new JLabel("Read input file location:");
	protected JTextField readsTF = new JTextField(30);
	protected JButton readsChooserOpener;
	protected JFileChooser readsInputFileChooser;
	protected String[] readsFileExtensions;

	protected JLabel samLabel = new JLabel("Alignment output location:");
	protected JTextField samTF = new JTextField(30);
	protected JButton samChooserOpener;
	protected JFileChooser samOutputFileChooser;
	protected String[] outputFileExtensions;//need not to be .sam for all Aligners

	protected GridBagConstraints gbc;
	protected JButton okayButton;
	protected JButton cancelButton;

	/**
	 * Set the allowed Fileextensions for Indexes, Readfiles and Output files
	 */
	protected abstract void setFileExtensions();

	/**
	 * File Filter for all Index Extensions allowed for that specific Aligner
	 */
	final class IndexFileFilter extends FileFilter{
		@Override
		public boolean accept(File f) {
			if(f != null){
				String fileExt = f.getName().substring(f.getName().lastIndexOf(".")+1);
				for (int i = 0; i < indexFileExtensions.length; i++) {
					String string = indexFileExtensions[i];
					if(string.equals(fileExt))
						return true;
				}
			}
			return false;
		}
		@Override
		public String getDescription() {
			String supportedIndexes = "";
			for (int i = 0; i < indexFileExtensions.length-1; i++) {
				supportedIndexes += "*."+indexFileExtensions[i]+ ", ";
			}
			supportedIndexes += "*."+indexFileExtensions[indexFileExtensions.length-1];
			return supportedIndexes;
		}
	}

	/**
	 * File Filter for all Read Input Files for that specific Aligner
	 */
	final class ReadFileFilter extends FileFilter{
		@Override
		public boolean accept(File f) {
			if(f != null){
				String fileExt = f.getName().substring(f.getName().lastIndexOf(".")+1);
				for (int i = 0; i < readsFileExtensions.length; i++) {
					String string = readsFileExtensions[i];
					if(string.equals(fileExt))
						return true;
				}
			}
			return false;
		}
		@Override
		public String getDescription() {
			String supportedIndexes = "";
			for (int i = 0; i < readsFileExtensions.length-1; i++) {
				supportedIndexes += "*."+readsFileExtensions[i]+ ", ";
			}
			supportedIndexes += "*."+readsFileExtensions[readsFileExtensions.length-1];
			return supportedIndexes;
		}
	}

	/**
	 * File Filter for all Outputfiles for that specific Aligner
	 */
	final class OutputFileFilter extends FileFilter{
		@Override
		public boolean accept(File f) {
			if(f != null){
				String fileExt = f.getName().substring(f.getName().lastIndexOf(".")+1);
				for (int i = 0; i < outputFileExtensions.length; i++) {
					String string = outputFileExtensions[i];
					if(string.equals(fileExt))
						return true;
				}
			}
			return false;
		}

		@Override
		public String getDescription() {
			String supportedIndexes = "";
			for (int i = 0; i < outputFileExtensions.length-1; i++) {
				supportedIndexes += "*."+ outputFileExtensions[i]+ ", ";
			}
			supportedIndexes += "*."+outputFileExtensions[outputFileExtensions.length-1];
			return supportedIndexes;
		}
	}
}
