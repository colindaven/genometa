/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

/**
 *
 * @author Elmo
 */
public class AlignerOutputView extends JComponent implements ActionListener{

	public static final String name = "Aligner Output";
	private final String[] availableAligner = {"bowtie", /*"bwa"*/};

	//Panel to hold the control buttons for the Aligners
	private JPanel controlPanel;
	private JComboBox alignerSelection;
	private JButton alignerExecution;
	//Panel to hold the outputs
	private JPanel outputPanel;
	private static JTextPane outputText;
	private static StyledDocument doc;//Holds the text


	public AlignerOutputView() {
		this.setLayout(new BorderLayout());

		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(2, 2, 2, 2);
		gbc.anchor = GridBagConstraints.LINE_START;
		gbc.fill = GridBagConstraints.BOTH;
		controlPanel = new JPanel();
		controlPanel.setLayout(new GridBagLayout());
		gbc.gridx = 0; gbc.gridy = 0;
		controlPanel.add(new JLabel("Aligner:"), gbc);
		alignerSelection = new JComboBox(availableAligner);
		gbc.gridx = 1; gbc.gridy = 0;
		controlPanel.add(alignerSelection, gbc);
		alignerExecution = new JButton("execute");
		alignerExecution.addActionListener(this);
		alignerExecution.setActionCommand("executeAligner");
		gbc.gridx = 2; gbc.gridy = 0;
		controlPanel.add(alignerExecution, gbc);


		outputPanel = new JPanel();
//		outputPanel.setBorder(new javax.swing.border.TitledBorder("Outputs & Errors"));
		outputPanel.setLayout(new GridLayout(1,1));
		outputText = new JTextPane();
		outputText.setEditable(false);
		JScrollPane outputScroll = new JScrollPane(outputText);
		outputPanel.add(outputScroll);


		this.add(controlPanel, BorderLayout.NORTH);
		this.add(outputPanel, BorderLayout.CENTER);
	}

	public void actionPerformed(ActionEvent e) {
		if(e.getActionCommand().equals("executeAligner")){
			if(alignerSelection.getSelectedIndex() == 0){//bowtie
				new BowtieAlignerExecutor();
			}else if(alignerSelection.getSelectedIndex() == 1){//bwa
				new BwaAlignerExecutor();
			}
		}
	}

	public static void appandOutputText(String s){
		doc = outputText.getStyledDocument();
		SimpleAttributeSet set = new SimpleAttributeSet();
		StyleConstants.setForeground(set, Color.BLACK);
		try {
			doc.insertString(doc.getLength(), s+"\n", set);
			outputText.setCaretPosition(outputText.getDocument().getLength());
		} catch (BadLocationException ex) {
			Logger.getLogger(AlignerOutputView.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	public static void appandErrorText(String s){
		doc = outputText.getStyledDocument();
		SimpleAttributeSet set = new SimpleAttributeSet();
		StyleConstants.setForeground(set, Color.red);
		try {
			doc.insertString(doc.getLength(), s+"\n", set);
			outputText.setCaretPosition(outputText.getDocument().getLength());
		} catch (BadLocationException ex) {
			Logger.getLogger(AlignerOutputView.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

}
