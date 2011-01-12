/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
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

	public static final String BOWTIE_LOCATION_PREF = "BOWTIE_EXECUTABLE_LOCATION";
	public static String bowtie_executable_location = PreferenceUtils.getTopNode()
			.get(BowtieAlignerWrapper.BOWTIE_LOCATION_PREF, "");


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
							AlignerOutputView.appandOutputText(actLine);
						}
						brCleanUp.close();
					}catch (IOException ioe){
						System.err.println("IOException thrown while reading external threads stdout");
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
							AlignerOutputView.appandErrorText(actLine);
						}
						brCleanUp.close();
					}catch (IOException ioe){
						System.err.println("IOException thrown while reading external threads stderr");
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
								System.err.println("InterrupedException thrown while updating 'background work' images");
								ex.printStackTrace();
							}
						}
						label.setIcon(BowtieAlignerExecutor.getNoActivityImageIcon());
					} catch (IOException ex) {

						System.err.println("Failed loading status images");
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
		String alignerExecutionString = BowtieAlignerWrapper.getBowtieExecutablePath()+" -t " + indexLocation
				+ " -e 100 --sam -3 4 -p 7 -q " + readInputFile + " " + outputFilePath;
//		String alignerExecutionString = "igb\\resources\\ConsoleApplication1.exe 1000";
		executionParameters.add(alignerExecutionString);

		String[] returnArray = new String[executionParameters.size()];
		executionParameters.copyInto(returnArray);
		return returnArray;
	}

	/**
	 * Method to set the Path to the Executable of the bowtie aligner. Will automaticlally set this path
	 *  to the Preference BOWTIE_EXECUTABLE_LOCATION
	 * @param path The Path to the Bowtie aligner
	 */
	public static void setBowtieExecutablePath(String path){
		bowtie_executable_location = path;
		PreferenceUtils.getTopNode().put(BowtieAlignerWrapper.BOWTIE_LOCATION_PREF, path);
	}

	public static String getBowtieExecutablePath(){
		return bowtie_executable_location;
	}

}
