/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.util.ThreadUtils;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;

/**
 * Class to open a dialog that gathers all data needed to call the Bowtie Aligner
 * @author paetow
 */
public final class BowtieAlignerExecutor extends AlignerExecutor{

	boolean isFastQ=false;

	public BowtieAlignerExecutor() {
		setFileExtensions();
		initButtonsAndChoosers();
		initDialog();
		this.pack();
		this.setVisible(true);
	}

	public void actionPerformed(ActionEvent e) {
		int retVal;
		if("indexChooser".equals(e.getActionCommand())){
			//Start File Chooser to select Index File(s)
			retVal = indexFileChooser.showOpenDialog(this);
			if(retVal == JFileChooser.APPROVE_OPTION){
				indexTF.setText(BowtieAlignerWrapper.correctIndexString(
						indexFileChooser.getSelectedFile().getAbsolutePath()));
			}
			PreferenceUtils.getTopNode().put(BowtieAlignerWrapper.INDEX_LOCATION_PREF,
					BowtieAlignerWrapper.correctIndexString(
						indexFileChooser.getSelectedFile().getAbsolutePath()));//Set last index choosed
			this.updateUserDefTF();
		}else if("readsChooser".equals(e.getActionCommand())){
			//Start File Chooser to select Reads File
			retVal = readsInputFileChooser.showOpenDialog(this);
			if( readsInputFileChooser.getSelectedFile() != null){
			String readsFileExtension = "";
			if(readsInputFileChooser.getSelectedFile().getName().contains(".")){
				readsFileExtension = readsInputFileChooser.getSelectedFile().getName().substring(
						readsInputFileChooser.getSelectedFile().getName().lastIndexOf("."));
				isFastQ = (readsFileExtension.contains("q") ? true : false) ;
			}
		}
			if(retVal == JFileChooser.APPROVE_OPTION){
				readsTF.setText(readsInputFileChooser.getSelectedFile().getAbsolutePath());
			}
			PreferenceUtils.getTopNode().put(BowtieAlignerWrapper.READ_LOCATION_PREF,
					readsInputFileChooser.getSelectedFile().getAbsolutePath());//Set last read choosed
			this.updateUserDefTF();
		}else if("reads2Chooser".equals(e.getActionCommand())){
			//Start File Chooser to select Reads File
			retVal = reads2InputFileChooser.showOpenDialog(this);
			if(retVal == JFileChooser.APPROVE_OPTION){
				reads2TF.setText(reads2InputFileChooser.getSelectedFile().getAbsolutePath());
			}
			PreferenceUtils.getTopNode().put(BowtieAlignerWrapper.READ2_LOCATION_PREF,
					reads2InputFileChooser.getSelectedFile().getAbsolutePath());//Set last read2 choosed
			this.updateUserDefTF();
		}else if("samChooser".equals(e.getActionCommand())){
			//Start File Chooser to select Output location
			retVal = samOutputFileChooser.showSaveDialog(this);
			if(retVal == JFileChooser.APPROVE_OPTION){
				samTF.setText(samOutputFileChooser.getSelectedFile().getAbsolutePath());
			}
			PreferenceUtils.getTopNode().put(BowtieAlignerWrapper.OUTPUT_LOCATION_PREF,
					samOutputFileChooser.getSelectedFile().getAbsolutePath());//Set last output choosed
			this.updateUserDefTF();
		}else if("useReads2".equals(e.getActionCommand())){
			//set fields for second reads visible
			if(useReads2.isSelected()){
				useUserDefCmd.setSelected(true);
				useUserDefCmdFields(useUserDefCmd.isSelected());
			}
			useReads2Fields(useReads2.isSelected());
		}else if("useUserDefCmd".equals(e.getActionCommand())){
			//set fields for user definded command string visible
			useUserDefCmdFields(useUserDefCmd.isSelected());
		}else if("okayAction".equals(e.getActionCommand())){
				//TODO set userdefined command if selected
				final BowtieAlignerWrapper bowtie = new BowtieAlignerWrapper();
				bowtie.setIndexLocation(indexTF.getText());
				bowtie.setReadInputFile(readsTF.getText());
				bowtie.setOutputFilePath(samTF.getText());
			SwingWorker<Integer, Integer> worker = new SwingWorker<Integer, Integer>() {
				@Override
				public Integer doInBackground() {
					try {
						bowtie.runAligner(Application.getSingleton().getStatusBar().getAlignerStatusLabel(),
								AlignerExecutor.getProgressImageIcons(), 150, userDefCmdTF.getText());
					} catch (IOException ex) {
						System.err.println("Failed loading status images");
					}
					return 0;
				}
			};
			ThreadUtils.getPrimaryExecutor(new Object()).execute(worker);
			this.dispose();
		}else if("cancelAction".equals(e.getActionCommand())){
			//Cancel pressed
			this.dispose();
		}
	}

	private void initButtonsAndChoosers() {
		//Init buttons an set Action Commands
		indexChooserOpener = new JButton("Select index");
		indexChooserOpener.addActionListener(this);
		indexChooserOpener.setActionCommand("indexChooser");
		readsChooserOpener = new JButton("Select reads");
		readsChooserOpener.addActionListener(this);
		readsChooserOpener.setActionCommand("readsChooser");

		reads2ChooserOpener = new JButton("Select reads");
		reads2ChooserOpener.addActionListener(this);
		reads2ChooserOpener.setActionCommand("reads2Chooser");
		
		samChooserOpener = new JButton("Select output");
		samChooserOpener.addActionListener(this);
		samChooserOpener.setActionCommand("samChooser");

		//init Filechooser and set File extensions
		indexFileChooser = new JFileChooser();
		indexFileChooser.setDialogTitle("Select Index File");
		indexFileChooser.setFileFilter(new IndexFileFilter());//see Super Class
		indexFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		indexFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		readsInputFileChooser = new JFileChooser();
		readsInputFileChooser.setDialogTitle("Select a File containing Reads");
		readsInputFileChooser.setFileFilter(new ReadFileFilter());//see Super Class
		readsInputFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		readsInputFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		//Init reads2 invisible
		reads2InputFileChooser = new JFileChooser();
		reads2InputFileChooser.setDialogTitle("Select a File containing Reads");
		reads2InputFileChooser.setFileFilter(new ReadFileFilter());//see Super Class
		reads2InputFileChooser.setDialogType(JFileChooser.OPEN_DIALOG);
		reads2InputFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		samOutputFileChooser = new JFileChooser();
		samOutputFileChooser.setDialogTitle("Select outputfile");
		samOutputFileChooser.setFileFilter(new OutputFileFilter());//see Super Class
		samOutputFileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
		samOutputFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);//TODO ask for override somewhere

		okayButton = new JButton("Okay");
		okayButton.addActionListener(this);
		okayButton.setActionCommand("okayAction");

		cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(this);
		cancelButton.setActionCommand("cancelAction");

		useUserDefCmd = new JCheckBox("Use userdefined command string");
		useUserDefCmd.setSelected(false);
		useUserDefCmd.addActionListener(this);
		useUserDefCmd.setActionCommand("useUserDefCmd");

		useReads2 = new JCheckBox("Use paired-end alignment");
		useReads2.setSelected(false);
		useReads2.addActionListener(this);
		useReads2.setActionCommand("useReads2");

	}

	@Override
	protected void setFileExtensions() {
		//Bowtie generates multiple Index Files called xx.1.ebwt, ...
		String[] idx = {"ebwt"};
		indexFileExtensions = idx;
		String[] reads = {"fq", "fastq"/*Fastq*/,"fa", "fna", "fas","fasta"/*Fasta*/};
		readsFileExtensions = reads;
		String[] out = {"sam"};
		outputFileExtensions = out;
	}

	private void initDialog() {
		//configure global parameters of gbc
		gbc = new GridBagConstraints();
		gbc.insets = new Insets(5, 5, 2, 5);
		gbc.gridwidth = 3;
		gbc.gridheight = 6;
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.HORIZONTAL;
		this.setTitle("Configure and run Bowtie Aligner");
		this.setLayout(new GridBagLayout());
		gbc.gridx = 0; gbc.gridy = 0;
		gbc.gridwidth = 2; gbc.gridheight = 1;
		this.add(indexLabel, gbc);
		indexTF.setEditable(false);//TF is only to display selected Path
		indexTF.setText(PreferenceUtils.getTopNode()
			.get(BowtieAlignerWrapper.INDEX_LOCATION_PREF, ""));//Try setting last index choosed
		gbc.gridx = 0; gbc.gridy = 1;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(indexTF, gbc);
		gbc.gridx = 1; gbc.gridy = 1;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(indexChooserOpener, gbc);
		gbc.gridx = 0; gbc.gridy = 2;
		gbc.gridwidth = 2; gbc.gridheight = 1;
		this.add(readsLabel, gbc);
		readsTF.setEditable(false);//TF is only to display selected Path
		readsTF.setText(PreferenceUtils.getTopNode()
			.get(BowtieAlignerWrapper.READ_LOCATION_PREF, ""));//Try setting last Read choosed
		gbc.gridx = 0; gbc.gridy = 3;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(readsTF, gbc);
		gbc.gridx = 1; gbc.gridy = 3;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(readsChooserOpener, gbc);
		gbc.gridx = 0; gbc.gridy = 6;
		gbc.gridwidth = 2; gbc.gridheight = 1;
		this.add(samLabel, gbc);
		samTF.setEditable(false);//TF is only to display selected Path
		samTF.setText(PreferenceUtils.getTopNode()
			.get(BowtieAlignerWrapper.OUTPUT_LOCATION_PREF, ""));//Try setting last output choosed
		gbc.gridx = 0; gbc.gridy = 7;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(samTF, gbc);
		gbc.gridx = 1; gbc.gridy = 7;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(samChooserOpener, gbc);

		gbc.gridx = 2; gbc.gridy = 0;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(okayButton, gbc);
		gbc.gridx = 2; gbc.gridy = 1;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(cancelButton,gbc);
		//Checkbox for userdefined command lines
		gbc.gridx = 2; gbc.gridy = 2;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(useUserDefCmd, gbc);
		//Checkbox for 2 Reads (PairedEnd)
		gbc.gridx = 2; gbc.gridy = 3;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(useReads2, gbc);

		this.setResizable(false);
		this.updateUserDefTF();
	}

	/**
	 * Specifies if the Fields for the second reads file are shown or not
	 * @param b true if used, false if not used
	 */
	private void useReads2Fields(boolean b){
		if(b == true){//show the fields for the second reads file
			gbc.gridx = 0; gbc.gridy = 4;
			gbc.gridwidth = 1; gbc.gridheight = 1;
			this.add(reads2Label, gbc);
			reads2TF.setEditable(false);//TF is only to display selected Path
			reads2TF.setText(PreferenceUtils.getTopNode()
				.get(BowtieAlignerWrapper.READ2_LOCATION_PREF, ""));//Try setting last read2 choosed
			gbc.gridx = 0; gbc.gridy = 5;
			gbc.gridwidth = 1; gbc.gridheight = 1;
			this.add(reads2TF, gbc);
			gbc.gridx = 1; gbc.gridy = 5;
			gbc.gridwidth = 1; gbc.gridheight = 1;
			this.add(reads2ChooserOpener, gbc);
		}else{
			this.remove(reads2Label);
			this.remove(reads2TF);
			this.remove(reads2ChooserOpener);
		}
		this.isUsePairedEnd = b;
		this.pack();
		this.repaint();
	}

	/**
	 * Specifies if the fields for user defindes command strings are enabled or diabled
	 * @param b true if enabled, false if disabled
	 */
	private void useUserDefCmdFields(boolean b){
		if(b == true){
			gbc.gridx = 0; gbc.gridy = 8;
			gbc.gridwidth = 1; gbc.gridheight = 1;
			this.add(userDefCommandLabel, gbc);
			reads2TF.setEditable(false);
			gbc.gridx = 0; gbc.gridy = 9;
			gbc.gridwidth = 3; gbc.gridheight = 1;
			this.add(userDefCmdScrollPanel, gbc);
			userDefCmdTF.setLineWrap(true);
			userDefCmdTF.setText(this.getUserDefindedCommandString());
		}else{
			this.remove(userDefCommandLabel);
			this.remove(userDefCmdScrollPanel);
		}
		this.pack();
		this.repaint();
	}

	@Override
	protected String getUserDefindedCommandString() {
		String ret = BowtieAlignerWrapper.bowtie_executable_location;
		//Get the parameters actually set in the context
		String index = " -t "+indexTF.getText();
		ret += index;
		ret += BowtieAlignerWrapper.defaultAlignerParameters;
		String readsType = (isFastQ?" -q ":" -f ");
		ret += readsType;
		ret +=(this.isUsedPairedEnd() ? "-1 " :"");
		String reads = readsTF.getText();
		ret += reads + " ";
		ret +=(this.isUsedPairedEnd() ? "-2 " :"");
		if(this.isUsedPairedEnd()){
			String reads2 = reads2TF.getText();
			ret += reads2;
		}
		String output = samTF.getText();
		ret += " "+  output;
		return ret;
	}

	private void updateUserDefTF(){
		userDefCmdTF.setText(this.getUserDefindedCommandString());
	}
}
