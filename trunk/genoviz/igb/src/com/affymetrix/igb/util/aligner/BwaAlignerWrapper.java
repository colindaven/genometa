/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 *
 * @author Elmo
 */
public class BwaAlignerWrapper extends AlignerWrapper {

	public static final String BWA_LOCATION_PREF = "BWA_EXECUTABLE_LOCATION";
	public static String bwa_executable_location = PreferenceUtils.getTopNode()
			.get(BwaAlignerWrapper.BWA_LOCATION_PREF, "");
	private String alignmentTempFile = "tmpAln";

	@Override
	public void runAligner(final JComponent componentToUpdate, final Object[] dataForComponentUpdate,
			final int updateInterval) throws IOException {
		try{
				executionProcess = Runtime.getRuntime().exec(generateExecutionParameters(0));
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
						label.setIcon(AlignerExecutor.getNoActivityImageIcon());
					} catch (IOException ex) {

						System.err.println("Failed loading status images");
					}
				}
				});
				updateComponent.start();
				executionProcess.waitFor();//Waits for the task to complete the first step of bwa (alignment)
				
				executionProcess = Runtime.getRuntime().exec(generateExecutionParameters(1));
				stdin = executionProcess.getOutputStream();
				stdout = executionProcess.getInputStream();
				stderr = executionProcess.getErrorStream();
				//Create a thread for Reading stdout of external process
				readStdout = new Thread(new Runnable() {
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
				readStderr = new Thread(new Runnable() {
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
//				updateComponent.start();//Should be enough, since this is exactly the same behaviour like in the first execution
				executionProcess.waitFor();//Waits for the task to complete the second step of bwa (generating sam)
			}catch(Exception ioe){
				ioe.printStackTrace();
			}
	}

	@Override
	public String[] generateExecutionParameters(int paramIdx) {
		String[] shell = this.determineExecutionProcess();
		Vector<String> executionParameters = new Vector<String>();
		for (int i = 0; i < shell.length; i++) {
			if(shell[i] != null || shell[i] != "")
				executionParameters.add(shell[i]);
		}
		String alignerExecutionString = "";
		if(paramIdx == 0){
			alignerExecutionString = BwaAlignerWrapper.getBwaExecutablePath()+ " aln -f " +
				alignmentTempFile + " -t 5 "+ indexLocation + " " + readInputFile;
		}else if(paramIdx == 1){
			alignerExecutionString = BwaAlignerWrapper.getBwaExecutablePath()+ " samse -f "+
				outputFilePath + " " + indexLocation + " " + alignmentTempFile + " " + readInputFile;
		}
		executionParameters.add(alignerExecutionString);

		String[] returnArray = new String[executionParameters.size()];
		executionParameters.copyInto(returnArray);
		return returnArray;
	}

	/**
	 * Method to set the Path to the Executable of the bwa aligner. Will automaticlally set this path
	 *  to the Preference BWA_EXECUTABLE_LOCATION
	 * @param path The Path to the bwa aligner
	 */
	public static void setBwaExecutablePath(String path){
		bwa_executable_location = path;
		PreferenceUtils.getTopNode().put(BwaAlignerWrapper.BWA_LOCATION_PREF, bwa_executable_location);
	}

	public static String getBwaExecutablePath(){
		return bwa_executable_location;
	}

	/**
	 * Sets the index. Corrects the path to the last dot. index.ann => index
	 * @param idxLoc the Path to the index
	 */
	protected void setIndexLocation(String idxLoc){
		indexLocation = idxLoc.substring(0, idxLoc.lastIndexOf("."));
	}


}
