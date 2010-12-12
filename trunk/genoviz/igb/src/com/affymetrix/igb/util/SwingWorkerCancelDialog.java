package com.affymetrix.igb.util;

import java.awt.Dialog.ModalityType;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 *
 * @author stefan
 */
public class SwingWorkerCancelDialog {

	SwingWorker swingWorker = null;
	JFrame jFrame = null;
	Thread dialogThread = null;

	public SwingWorkerCancelDialog(JFrame frame, SwingWorker worker) {
		jFrame = frame;
		swingWorker = worker;		
	}

	public void showCancelDialog(final String title, final String message, final String buttonText) {
		this.dialogThread = new Thread(){
			@Override
			public void run() {
				String cancelButtonText = null;
				if(buttonText != null) {
					cancelButtonText = "Cancel work";
				}
				else {
					cancelButtonText = "Cancel work";
				}				
				Object[] options = {cancelButtonText};

				JOptionPane pane = new JOptionPane(message, JOptionPane.INFORMATION_MESSAGE, JOptionPane.DEFAULT_OPTION, null, options);
				JDialog dialog = pane.createDialog(jFrame, title);
				dialog.setModalityType(ModalityType.MODELESS);

				dialog.setVisible(true);

				while(true) {
					Object value = pane.getValue();

					if(value.toString().equals(cancelButtonText)) {
						swingWorker.cancel(true);
						return;
					}
					try {
						sleep(500);
					} catch (InterruptedException ex) {
						// exception is used for "normal" cancelling the thread+dialog
						dialog.setVisible(false);
						return;
					}
				}
			}
		};
		this.dialogThread.start();
	}

	/*
	 * This method forces the Cancel Dialog to stop itself and got destroyed.
	 */
	public void destroyCancelDialog() {
		this.dialogThread.interrupt();
	}

}
