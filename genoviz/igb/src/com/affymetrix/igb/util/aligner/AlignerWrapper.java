/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.util.aligner;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
	public String determineExecutionProcess(){
		String os = (String) System.getProperties().get("os.name");
		String shell;
		if(os.contains("win")){
			shell = "cmd.exe";
		}else if(os.contains("nix")|| os.contains("nux")){
			shell = "/bin/bash";
		}else if(os.contains("mac")){
			shell = ""; //TODO welche shell bei MacOS ??
		}else{
			shell = "";
		}
		return shell;
	}

	/**
	 * Wird aufgerufen um einen Aligner auszuführen
	 */
	public abstract void runAligner() throws IOException;

	/**
	 * Erzeugt alle
	 * @return
	 */
	public abstract String[] generateExecutionParameters();

}
