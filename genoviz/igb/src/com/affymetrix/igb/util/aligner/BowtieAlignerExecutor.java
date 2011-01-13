/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import com.affymetrix.igb.Application;
import com.affymetrix.igb.util.ThreadUtils;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.SwingWorker;

/**
 * Class to open a dialog that gathers all data needed to call the Bowtie Aligner
 * @author paetow
 */
public final class BowtieAlignerExecutor extends AlignerExecutor{


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
				indexTF.setText(indexFileChooser.getSelectedFile().getAbsolutePath());
			}
		}else if("readsChooser".equals(e.getActionCommand())){
			//Start File Chooser to select Reads File
			retVal = readsInputFileChooser.showOpenDialog(this);
			if(retVal == JFileChooser.APPROVE_OPTION){
				readsTF.setText(readsInputFileChooser.getSelectedFile().getAbsolutePath());
			}
		}else if("samChooser".equals(e.getActionCommand())){
			//Start File Chooser to select Output location
			retVal = samOutputFileChooser.showSaveDialog(this);
			if(retVal == JFileChooser.APPROVE_OPTION){
				samTF.setText(samOutputFileChooser.getSelectedFile().getAbsolutePath());
			}
		}else if("okayAction".equals(e.getActionCommand())){
				final BowtieAlignerWrapper bowtie = new BowtieAlignerWrapper();
				bowtie.setIndexLocation(indexTF.getText());
				bowtie.setReadInputFile(readsTF.getText());
				bowtie.setOutputFilePath(samTF.getText());
			SwingWorker<Integer, Integer> worker = new SwingWorker<Integer, Integer>() {
//				BowtieAlignerWrapper bowtie = new BowtieAlignerWrapper();
				@Override
				public Integer doInBackground() {
					try {
						bowtie.runAligner(Application.getSingleton().getStatusBar().getAlignerStatusLabel(),
								AlignerExecutor.getProgressImageIcons(), 150);
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
	}

	@Override
	protected void setFileExtensions() {
		//Bowtie generates multiple Index Files called xx.1.ebwt, ...
		String[] idx = {"ebwt"};
		indexFileExtensions = idx;
		String[] reads = {"fq", "fastq"/*Fastq*/,"fa", "fna", "fas","fasta"/*Fastq*/};//TODO welche extensions gibt es noch für Fasta, fastq ? GGf. über das Optionsmenü verfügbar machen??
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
		gbc.gridx = 0; gbc.gridy = 3;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(readsTF, gbc);
		gbc.gridx = 1; gbc.gridy = 3;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(readsChooserOpener, gbc);
		gbc.gridx = 0; gbc.gridy = 4;
		gbc.gridwidth = 2; gbc.gridheight = 1;
		this.add(samLabel, gbc);
		samTF.setEditable(false);//TF is only to display selected Path
		gbc.gridx = 0; gbc.gridy = 5;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(samTF, gbc);
		gbc.gridx = 1; gbc.gridy = 5;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(samChooserOpener, gbc);

		gbc.gridx = 2; gbc.gridy = 0;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(okayButton, gbc);
		gbc.gridx = 2; gbc.gridy = 1;
		gbc.gridwidth = 1; gbc.gridheight = 1;
		this.add(cancelButton,gbc);

		this.setResizable(false);
	}
}
