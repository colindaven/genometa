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
import javax.swing.JScrollPane;
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
	}

}
