/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util;

import java.awt.GridLayout;
import java.awt.event.MouseEvent;
import java.io.IOException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.MouseInputListener;

/**
 *
 * @author pool336pc02
 */
public class CommandLineWindow extends JFrame implements MouseInputListener{

	private JButton exec;
	private JTextField indexLoc;
	private JTextField readsFileLoc;
	private JTextField outputFileLocation;

	public CommandLineWindow() {
		this.setTitle("Bowtie Aligner ausführen");
		this.setLayout(new GridLayout(0,2));
		this.add(new JLabel("IndexOrdner: "));
		this.indexLoc = new JTextField();
		this.add(indexLoc);
		this.add(new JLabel("RFeads File Location: "));
		this.readsFileLoc = new JTextField();
		this.add(readsFileLoc);
		this.add(new JLabel("OutputFile"));
		this.outputFileLocation = new JTextField();
		this.add(outputFileLocation);

		exec = new JButton("Execute");
		exec.addMouseListener(this);
		this.add(exec);
		this.pack();
		this.setVisible(true);
	}





	public void mouseClicked(MouseEvent me) {
		if(me.getSource() == exec){
			System.out.println("Klicky Klicky");
			BowtieAlignerWrapper aligner = new BowtieAlignerWrapper();
			aligner.indexLocation = indexLoc.getText();
			aligner.readInputFile = readsFileLoc.getText();
			aligner.outputFilePath = outputFileLocation.getText();
			try{
				aligner.runAligner();
			}catch(IOException ioe){
				ioe.printStackTrace();
			}
		}
	}

	public void mousePressed(MouseEvent me) {
	}

	public void mouseReleased(MouseEvent me) {
	}

	public void mouseEntered(MouseEvent me) {
	}

	public void mouseExited(MouseEvent me) {
	}

	public void mouseDragged(MouseEvent me) {
	}

	public void mouseMoved(MouseEvent me) {
	}



	
/*	private Process process;
	private OutputStream stdin;
	private InputStream stdout;
	private InputStream stderr;

	private JTextArea output_area;
	private JScrollPane output_scroll;
	private JTextField input_field;

	private int lineCnt = 1;

	public CommandLineWindow() {
		this.setTitle("Commandline test");
		this.setLayout(new BorderLayout());
		output_area = new JTextArea(50, 80);
		output_area.setEditable(false);
		output_scroll = new JScrollPane(output_area);
		input_field = new JTextField(80);
		input_field.addKeyListener(this);
		this.add(output_scroll, BorderLayout.CENTER);
		this.add(input_field, BorderLayout.SOUTH);
		this.init();

		this.pack();
		this.setVisible(true);
	}

	private void init(){
		try{
			process = Runtime.getRuntime().exec("/bin/sh");
			stdin = process.getOutputStream();
			stdout = process.getInputStream();
			stderr = process.getErrorStream();
		}catch(IOException ioe){
			System.out.println(ioe);
		}
	}

	private void sendStringToConsole(String s){
		if(s.length() > 0){
			String[] commands;
			int cmdCnt = 0;
			String os = (String) System.getProperties().get("os.name");
			if(os.contains("win")){
				commands = new String[2];
				commands[cmdCnt++] = "cmd.exe";
			}else if(os.contains("nix")|| os.contains("nux")){
				commands = new String[3];
				commands[cmdCnt++] = "/bin/bash";
				commands[cmdCnt++] = "-c";
			}else if(os.contains("mac")){
				commands = new String[2];
				commands[cmdCnt++] = ""; //TODO welche shell bei MacOS ??
			}else{
				commands = new String[1];
			}
			commands[cmdCnt++] = s+"\n";
			try{
				process = Runtime.getRuntime().exec(commands);
				stdin = process.getOutputStream();
				stdout = process.getInputStream();
				stderr = process.getErrorStream();
				String actLine = "";
				BufferedReader brCleanUp =
					new BufferedReader (new InputStreamReader(stdout));
				while((actLine = brCleanUp.readLine())!= null){
					output_area.append(actLine + "\n");
				}
			}catch(Exception ioe){
				System.out.println(ioe);
			}
		}
	}


	public void keyTyped(KeyEvent ke) {
	}

	public void keyPressed(KeyEvent ke) {

		if(ke.getKeyChar() == KeyEvent.VK_ENTER){
			this.sendStringToConsole(input_field.getText());
			input_field.setText("");
		}
	}

	public void keyReleased(KeyEvent ke) {
	}*/

}
