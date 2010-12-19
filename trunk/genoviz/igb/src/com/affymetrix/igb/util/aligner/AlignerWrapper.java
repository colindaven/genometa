/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import javax.swing.JComponent;

/**
 *	Abstarkte Klasse für das aufrufen von Alignern in IGB
 * @author paetow
 */
public abstract class AlignerWrapper {
	//Commandshell auf der der Aligner aufgerufen werden soll
	protected Process executionProcess;
	//stdIn des über determineExecutionProcess ermittelten Prozess
	protected OutputStream stdin;
	//stdOut des über determineExecutionProcess ermittelten Prozess
	protected InputStream stdout;
	//stderr des über determineExecutionProcess ermittelten Prozess
	protected InputStream stderr;
	//String der ausgeführt werden soll
	protected String CommandString;
	//Ordner in der das indexierte Referenzgenom liegt
	protected String indexLocation;
	//Datei, die die Reads enthält
	protected String readInputFile;
	//Zielposition der Ausgabedatei
	protected String outputFilePath;

	/**
	 * Gibt den String für die Ausführung einer Shell auf dem aktuellen Betriebssystem zurück
	 */
	public String[] determineExecutionProcess(){
		String os = (String) System.getProperties().get("os.name");
		os = os.toLowerCase();
		String[] shell = new String[2];
		if(os.contains("win")){
			shell[0] = "cmd.exe";
			shell[1] = "/c";
		}else if(os.contains("nix")|| os.contains("nux")){
			shell[0] = "/bin/bash";
			shell[1] = "-c";
		}else if(os.contains("mac")){
			shell[0] = ""; //TODO welche shell bei MacOS ??
		}else{
			shell[0] = "";
		}
		return shell;
	}

	/**
	 * Wird aufgerufen um einen Aligner auszuführen
	 * @param componentToUpdate a JComponent that can be put into the GUI and
	 *				will periodically be updated.
	 * @param dataForComponentUpdate The Data, the Component will be updated with
	 * @param updateInterval the Time the Update method will sleep between updates
	 */
	public abstract void runAligner(final JComponent componentToUpdate, final Object[] dataForComponentUpdate,
			final int updateInterval) throws IOException;

	/**
	 * Erzeugt alle
	 * @return
	 */
	public abstract String[] generateExecutionParameters();

	/**
	 * Returns true if the Thread is still running. Knows that by asking for the Theads exit Value
	 * an catches an exception that is thworn when the thread is still running
	 * @return true if the Thread is still running, false if it ended;
	 */
	public boolean alignerIsStillRunning(){
		try{
			executionProcess.exitValue();
			return false;
		}catch (IllegalThreadStateException itse){
			return true;
		}
	}

}
