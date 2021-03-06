/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.Application;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

/**
 *
 * @author Malte Paetow (malte.paetow@gmx.de)
 */
public class BowtieAlignerWrapper extends AlignerWrapper {

	public static final String BOWTIE_LOCATION_PREF = "BOWTIE_EXECUTABLE_LOCATION";
	public static final String INDEX_LOCATION_PREF = "INDEX_LOCATION_PREF";
	public static final String READ_LOCATION_PREF = "READ_LOCATION_PREF";
	public static final String READ2_LOCATION_PREF = "READ2_LOCATION_PREF";
	public static final String OUTPUT_LOCATION_PREF = "OUTPUT_LOCATION_PREF";
	public static String bowtie_executable_location = PreferenceUtils.getTopNode()
			.get(BowtieAlignerWrapper.BOWTIE_LOCATION_PREF, "");
	public static String bowtie_index_location = PreferenceUtils.getTopNode()
			.get(BowtieAlignerWrapper.INDEX_LOCATION_PREF, "");
	public static String bowtie_read_location = PreferenceUtils.getTopNode()
			.get(BowtieAlignerWrapper.READ_LOCATION_PREF, "");
	public static String bowtie_read2_location = PreferenceUtils.getTopNode()
			.get(BowtieAlignerWrapper.READ2_LOCATION_PREF, "");
	public static String bowtie_output_location = PreferenceUtils.getTopNode()
			.get(BowtieAlignerWrapper.OUTPUT_LOCATION_PREF, "");
	public static final String defaultAlignerParameters = " --sam -p "+
			//Make shure that there is also one processor if there is only one installed
			(Runtime.getRuntime().availableProcessors()-1 > 0 ? 
				Runtime.getRuntime().availableProcessors()-1 : 1) +" ";


	@Override
	public void runAligner(final JComponent componentToUpdate, final Object[] dataForComponentUpdate,
			final int updateInterval, String executionString) throws IOException {
		try{
				executionProcess = Runtime.getRuntime().exec(generateExecutionParameters(0, executionString));
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
				//Create a Linebreak after each parameter
				updateComponent.start();
				Pattern p = Pattern.compile("-.{1} ");
				Matcher m = p.matcher(executionString);
				StringBuffer sb = new StringBuffer();
				while (m.find()) {
				 m.appendReplacement(sb, "\n$0");
				}
				m.appendTail(sb);
				JOptionPane.showMessageDialog(Application.getSingleton().getFrame(), "Alignment started with this command:\n\n"
						+  sb.toString()+ "\n\n"
						+ "Do not shut down genometa as long as the icon in the lower right corner is moving."
						, "Alignment started", JOptionPane.INFORMATION_MESSAGE);
				executionProcess.waitFor();//Waits for the task to complete
				AlignerOutputView.appandOutputText("##############\n"
						+ "Job completed\n"
						+ "Please navigate to "+this.getOutputFilePath()+" to see your file.\n"
						+ "##############");
				JOptionPane.showMessageDialog(Application.getSingleton().getFrame(),
						"Alignment completed. You can now open the file located at \n"+this.getOutputFilePath());
			}catch(Exception ioe){
				ioe.printStackTrace();
			}
	}

	@Override
	public String[] generateExecutionParameters(int paramIdx, String execCmd) {
		String[] shell = this.determineExecutionProcess();
		Vector<String> executionParameters = new Vector<String>();
		for (int i = 0; i < shell.length; i++) {
			if(shell[i] != null || shell[i] != "")
				executionParameters.add(shell[i]);
		}
//		//determine if input file is fasta or fastq
//		String[] readType = {"-f"/*fasta*/, "-q"/*fastq*/};
//		int readTypeIdx = 0;
//		String readsPathFileType = "";
//		try{
//			readsPathFileType = readInputFile.substring(readInputFile.lastIndexOf(".")+1, readInputFile.length());
//		}catch(StringIndexOutOfBoundsException sioe){
//			System.err.println("no reads file");
//		}
//		String[] readFilePossibleExt = {"fq", "fastq"/*Fastq*/,"fa", "fna", "fas","fasta"/*Fasta*/};
//		if(readsPathFileType.equalsIgnoreCase(readFilePossibleExt[0]) ||
//				readsPathFileType.equalsIgnoreCase(readFilePossibleExt[1])){
//			//Its Fastq format
//			readTypeIdx = 1;
//		}else if(readsPathFileType.equalsIgnoreCase(readFilePossibleExt[2]) ||
//				readsPathFileType.equalsIgnoreCase(readFilePossibleExt[3]) ||
//				readsPathFileType.equalsIgnoreCase(readFilePossibleExt[4]) ||
//				readsPathFileType.equalsIgnoreCase(readFilePossibleExt[5])){
//			//its Fasta format
//			readTypeIdx = 0;
//		}
//		String alignerExecutionString = "";
//		if(paramIdx == 0){
//			alignerExecutionString = BowtieAlignerWrapper.getBowtieExecutablePath()+" -t " + indexLocation
//					+ BowtieAlignerWrapper.defaultAlignerParameters
//					+ readType[readTypeIdx] + " " + readInputFile + " " + outputFilePath;
//		}
//		executionParameters.add(alignerExecutionString);
		executionParameters.add(execCmd);

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
		PreferenceUtils.getTopNode().put(BowtieAlignerWrapper.BOWTIE_LOCATION_PREF, bowtie_executable_location);
	}

	public static String getBowtieExecutablePath(){
		return bowtie_executable_location;
	}

	/**
	 * Corrects the path to the index. Remoces everything after the prelast dot. index.1.ebwt => index
	 * @param idxLoc the Path to one of the index files
	 * @return the corrected Index Path
	 */
	protected static String correctIndexString(String idxLoc){
		String ret = "";
		try{
			String s = idxLoc.substring(0, idxLoc.lastIndexOf("."));
			ret = s.substring(0, s.lastIndexOf("."));
		}catch(StringIndexOutOfBoundsException sioe){
			System.err.println("no Index location specified");
		}
		return ret;
	}

}
