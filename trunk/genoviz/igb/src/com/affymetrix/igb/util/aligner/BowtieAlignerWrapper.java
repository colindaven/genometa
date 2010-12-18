/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Vector;

/**
 *
 * @author pool336pc02
 */
public class BowtieAlignerWrapper extends AlignerWrapper {

	@Override
	public void runAligner() throws IOException {
		try{
				executionProcess = Runtime.getRuntime().exec(generateExecutionParameters());
				stdin = executionProcess.getOutputStream();
				stdout = executionProcess.getInputStream();
				stderr = executionProcess.getErrorStream();
				String actLine = "";
				BufferedReader brCleanUp =
					new BufferedReader (new InputStreamReader(stdout));
				while((actLine = brCleanUp.readLine())!= null){
					System.out.println(actLine);
				}
				brCleanUp.close();
				brCleanUp = new BufferedReader (new InputStreamReader(stderr));
				while((actLine = brCleanUp.readLine())!= null){
					System.err.println(actLine);
				}
				brCleanUp.close();
			}catch(Exception ioe){
				System.out.println(ioe);
			}
	}

	@Override
	public String[] generateExecutionParameters() {
		String shell = this.determineExecutionProcess();
		Vector<String> executionParameters = new Vector<String>();
		executionParameters.add(shell);
		if(shell.contains("bin")){//wir sind unter linux und benötigen ein '-c'
			executionParameters.add("-c"); //Die Bash benötigt ein -c zum starten eines Befehls beim Aufruf
		}
		String alignerExecutionString = findBowtieExecutionPath()+" -t " + indexLocation
				+ " -e 100 --sam -3 4 -p 7 -q " + readInputFile + " " + outputFilePath;
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
