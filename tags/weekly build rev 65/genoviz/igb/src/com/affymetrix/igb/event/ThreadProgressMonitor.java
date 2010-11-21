package com.affymetrix.igb.event;

import java.awt.Component;
import java.beans.PropertyChangeListener;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;

/**
 *  Provides a JDialog that can let the user know about an underlying process
 *  and provides a "Cancel" button to stop it.
 *  This is similar to the javax.swing.ProgressMonitor class, but does not
 *  show a JProgressBar, and has optional "OK" and "Cancel" buttons to dismiss the dialog.
 */
public final class ThreadProgressMonitor {
  private JOptionPane opt_pane;
  private JDialog dialog;
  private final Thread thread;
  private boolean is_closed = false;
  
  /** Creates a JDialog with parent component c.
   *  @param message  A message to display to the user.  Usually a String or String[].
   *  @param t  If non-null, this given Thread will be interrupted when
   *  the user presses the "Cancel" button (if there is a cancel button).
   *  @param can_cancel if true, there will be a "Cancel" button
   *  @param can_dismiss if true, there will be an "OK" button to close the dialog
   *  without interrupting the Thread.  (Can be dangerous)
   */
  public ThreadProgressMonitor(Component c, String title, Object message, 
  Thread t, boolean can_cancel, boolean can_dismiss) {
    String cancel_text = UIManager.getString("OptionPane.cancelButtonText");
    if (cancel_text == null) {cancel_text = "Cancel";}
    String ok_text = UIManager.getString("OptionPane.okButtonText");
    if (ok_text == null) {ok_text = "OK";}
    List<String> button_list = new ArrayList<String>(2);
    if (can_dismiss) {button_list.add(ok_text);}
    if (can_cancel) {button_list.add(cancel_text);}
    String[] buttons = button_list.toArray(new String[button_list.size()]);
    this.thread = t;
    this.opt_pane = new JOptionPane(
      message,
      JOptionPane.INFORMATION_MESSAGE,
      JOptionPane.DEFAULT_OPTION, 
      (Icon) null, 
      buttons
    );
    this.dialog = opt_pane.createDialog(c, title);
    this.dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    opt_pane.addPropertyChangeListener(pcl);
  }

  PropertyChangeListener pcl = new java.beans.PropertyChangeListener() {
    public void propertyChange(java.beans.PropertyChangeEvent evt) {
      String prop = evt.getPropertyName();
      Object value = opt_pane.getValue();
      if ((evt.getSource() == opt_pane)
      && (prop.equals(JOptionPane.VALUE_PROPERTY)) && value != null) {
        if (value.equals(UIManager.getString("OptionPane.cancelButtonText"))) {
          cancelPressed();
        } else if (value.equals(UIManager.getString("OptionPane.okButtonText"))) {
          okPressed();
        }
      }
    }
  };
  
  private void setMessage(Object o) {
    if (is_closed) return;
    opt_pane.setMessage(o);
    dialog.pack();
  }

  public void setMessageEventually(final Object o) {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        setMessage(o);
      }
    });
  }
  
  private void showDialog() {
    if (is_closed) return;
    dialog.pack();
    dialog.setVisible(true);
  }

  public void showDialogEventually() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        showDialog();
      }
    });
  }
  
  /** Permanently closes the dialog.  The dialogs in this method are not intended
   *  to be re-displayed after being closed.  This avoids some thread-related
   *  problems.  For example, if the closeDialog() method is activated before
   *  showDialog() or setMessage(), the dialog will remain closed, and may never
   *  actually have ever been shown.  Nonetheless, the proper thing to do usually
   *  is to call closeDialogEventually().
   */
  public void closeDialog() {
    is_closed = true;
    dialog.setVisible(false);
    opt_pane.removePropertyChangeListener(pcl);
    pcl = null;
    opt_pane = null;
    dialog = null;
  }

  public void closeDialogEventually() {
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        closeDialog();
      }
    });
  }
  
  /** This method is called when the user selects the "Cancel" button.
   *  It simply calls {@link #closeDialog()}, and if a thread was provided
   *  in the constructor it calls interrupt() on it.
   */
  private void cancelPressed() {
    dialog.setEnabled(false);
    closeDialog();
    if (thread != null) {thread.interrupt();}
  }

  /** This method is called when the user selects the "OK" button.
   *  It simply calls {@link #closeDialog()}.
   */
  private void okPressed() {
    closeDialog();
  }
}
