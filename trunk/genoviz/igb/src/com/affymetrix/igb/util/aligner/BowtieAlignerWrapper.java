/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import java.awt.Image;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 *
 * @author pool336pc02
 */
public class BowtieAlignerWrapper extends AlignerWrapper {


	@Override
	public void runAligner(final JComponent componentToUpdate, final Object[] dataForComponentUpdate,
			final int updateInterval) throws IOException {
		try{//TODO ggf das ganze in einen eigenen Thread auslagern, damit man weiterarbeiten kann
				executionProcess = Runtime.getRuntime().exec(generateExecutionParameters());
				stdin = executionProcess.getOutputStream();
				stdout = executionProcess.getInputStream();
				stderr = executionProcess.getErrorStream();
				//Create a thread for Reading stdout of external process
				Thread readStdout = new Thread(new Runnable() {
				public void run() {
					try{
						String actLine = "";
						BufferedReader brCleanUp =
							new BufferedReader (new InputStreamReader(stdout));
						while((actLine = brCleanUp.readLine())!= null){
							System.out.println(actLine);
						}
						brCleanUp.close();
					}catch (IOException ioe){
						ioe.printStackTrace();
					}
				}
				});
				readStdout.start();
				//Create a thread for reading stderr of external process
				Thread readStderr = new Thread(new Runnable() {
				public void run() {
					try{
						String actLine = "";
						BufferedReader brCleanUp =
							new BufferedReader (new InputStreamReader(stderr));
						while((actLine = brCleanUp.readLine())!= null){
							System.out.println(actLine);
						}
						brCleanUp.close();
					}catch (IOException ioe){
						ioe.printStackTrace();
					}
				}
				});
				readStderr.start();
				//Create a thread to update the Component
				Thread updateComponent = new Thread(new Runnable() {
				public void run() {
					try {
						JLabel label = (JLabel) componentToUpdate;
						ImageIcon[] images = (ImageIcon[]) dataForComponentUpdate;
						int idx = 0;
						while (alignerIsStillRunning()) {
							try {
								label.setIcon(images[idx++]);
								if (idx == images.length) {
									idx = 0;
								}
								Thread.sleep(updateInterval);
							} catch (InterruptedException ex) {
								ex.printStackTrace();
							}
						}
						label.setIcon(BowtieAlignerExecutor.getNoActivityImageIcon());
					} catch (IOException ex) {
						ex.printStackTrace();
					}
				}
				});
				updateComponent.start();
				executionProcess.waitFor();//Waits for the task to complete
			}catch(Exception ioe){
				System.out.println(ioe);
			}
	}

	@Override
	public String[] generateExecutionParameters() {
		String[] shell = this.determineExecutionProcess();
		Vector<String> executionParameters = new Vector<String>();
		for (int i = 0; i < shell.length; i++) {
			if(shell[i] != null | shell[i] != "")
				executionParameters.add(shell[i]);
		}
//		String alignerExecutionString = findBowtieExecutionPath()+" -t " + indexLocation
//				+ " -e 100 --sam -3 4 -p 7 -q " + readInputFile + " " + outputFilePath;
		String alignerExecutionString = "igb\\resources\\ConsoleApplication1.exe 1000";
		executionParameters.add(alignerExecutionString);

		String[] returnArray = new String[executionParameters.size()];
		executionParameters.copyInto(returnArray);
		return returnArray;
	}

	/**
	 * Try to find the location of Bowtie
	 * @return the Path to bowtie;
	 */
	private String findBowtieExecutionPath(){
		return "/home/pool336pc02/Downloads/bowtie-0.12.7/bowtie";
	}

}
