/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import com.affymetrix.igb.IGB;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import javax.swing.JButton;
import javax.swing.JFileChooser;

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
			//Start File Chooser to select Index File(s)
			retVal = readsInputFileChooser.showOpenDialog(this);
			if(retVal == JFileChooser.APPROVE_OPTION){
				readsTF.setText(indexFileChooser.getSelectedFile().getAbsolutePath());
			}
		}else if("samChooser".equals(e.getActionCommand())){
			//Start File Chooser to select Index File(s)
			retVal = samOutputFileChooser.showSaveDialog(this);
			if(retVal == JFileChooser.APPROVE_OPTION){
				samTF.setText(indexFileChooser.getSelectedFile().getAbsolutePath());
			}
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
	}

	@Override
	protected void setFileExtensions() {
		//Bowtie generates multiple Index Files called xx.1.ebwt, ...
		String[] idx = {"ebwt"};
		indexFileExtensions = idx;
		String[] reads = {"fq", "fastq","fa","fasta"};//TODO welche extensions gibt es noch für Fasta, fastq ? GGf. über das Optionsmenü verfügbar machen??
		readsFileExtensions = reads;
		String[] out = {"sam"};
		outputFileExtensions = out;
	}

	private void initDialog() {
		this.setTitle("Configure and run Bowtie Aligner");
		this.setLayout(new GridLayout(0,3));
		this.add(indexLabel);
		indexTF.setEditable(false);//TF is only to display selected Path
		this.add(indexTF);
		this.add(indexChooserOpener);
		this.add(readsLabel);
		readsTF.setEditable(false);//TF is only to display selected Path
		this.add(readsTF);
		this.add(readsChooserOpener);
		this.add(samLabel);
		samTF.setEditable(false);//TF is only to display selected Path
		this.add(samTF);
		this.add(samChooserOpener);
	}
}
