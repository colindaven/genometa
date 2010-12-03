/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Vector;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 *
 * @author pool336pc02
 */
public class CommandLineWindow extends JFrame implements KeyListener{

	
	private Process process;
	private OutputStream stdin;
	private InputStream stdout;
	private InputStream stderr;

	private JTextArea output_area;
	private JTextField input_field;

	public CommandLineWindow() {
		this.setTitle("Commandline test");
		this.setLayout(new BorderLayout());
		output_area = new JTextArea(50, 80);
		output_area.setEditable(false);
		input_field = new JTextField(80);
		input_field.addKeyListener(this);
		this.add(output_area, BorderLayout.CENTER);
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
		try{
			String cmd = s+"\n";
			stdin.write(s.getBytes());
			stdin.flush();
		}catch (Exception e){
			System.out.println("ex write stdin");
		}
	}
	private void readConsoleOutput(){
		try{
			String[] lines = output_area.getText().split("\n");
			Vector<String> newLines = new Vector<String>();
			for (int i = 0; i < lines.length; i++) {
				newLines.add(lines[i]);
			}
			String actLine = "";
			BufferedReader brCleanUp = 
				new BufferedReader (new InputStreamReader(stdout));
			System.out.println(brCleanUp.ready());
			if(brCleanUp.ready()){
				do{
						actLine = brCleanUp.readLine();
						newLines.add(actLine+ "\n");
				}while(actLine != null);
			}
			int numLines = newLines.size();
			if(numLines > 50) numLines = 50;
			for (int i = 0; i < numLines; i++) {
				actLine += newLines.get(newLines.size()-i-1);
			}
			brCleanUp.close();
		}catch (IOException e){
			System.out.println("ex read stdout");
			e.printStackTrace();
		}
	}

	public void keyTyped(KeyEvent ke) {
	}

	public void keyPressed(KeyEvent ke) {

		if(ke.getKeyChar() == KeyEvent.VK_ENTER){
			this.sendStringToConsole(input_field.getText());
			input_field.setText("");
			this.readConsoleOutput();
		}
	}

	public void keyReleased(KeyEvent ke) {
	}

}
