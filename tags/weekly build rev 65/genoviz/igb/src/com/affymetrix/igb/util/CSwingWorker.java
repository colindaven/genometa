package com.affymetrix.igb.util;

import com.affymetrix.igb.Application;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.swing.SwingWorker;

/**
 *
 * @author hiralv
 */
public abstract class CSwingWorker<T,V> extends SwingWorker<T,V> implements ActionListener{

	private final static Set<CSwingWorker> cancellables = new LinkedHashSet<CSwingWorker>();

	protected String statusMessage = "";

	public CSwingWorker(String message){
		statusMessage = message;
	}
	
	public void setStatusMessage(String message){
		statusMessage = message;
	}

	public String getStatusMessage(){
		return statusMessage;
	}


	public void cancel(){
		this.cancel(true);
		setStatusMessage("Cancelling " + statusMessage);
		Application.getSingleton().updatePopup();
	}

	public void actionPerformed(ActionEvent e){
		cancel();
	}


	public static Set<CSwingWorker> getWorkers(){
		return Collections.unmodifiableSet(cancellables);
	}


	public void updateStatusMessage(String message){
		removeStatus();
		setStatusMessage(message);
		showStatus();
		Application.getSingleton().updatePopup();
	}


	private void showStatus(){
		Application.getSingleton().addNotLockedUpMsg(statusMessage);
	}

	private void removeStatus(){
		Application.getSingleton().removeNotLockedUpMsg(statusMessage);
	}


	public void execute(Object key){
		cancellables.add(this);
		ThreadUtils.getPrimaryExecutor(key).execute(this);
		showStatus();
		Application.getSingleton().updatePopup();
	}

	protected void finished(){
		cancellables.remove(this);
		removeStatus();
		Application.getSingleton().updatePopup();
	}

}
