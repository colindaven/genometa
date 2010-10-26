/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
*    
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*   Distributions from Affymetrix, Inc., place this in the
*   IGB_LICENSE.html file.  
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/

package com.affymetrix.igb.util;

import com.affymetrix.genoviz.util.ErrorHandler;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.StringTokenizer;
import javax.swing.*;
import javax.swing.event.*;

/**
 *  An adapter to add Excel-like cut and paste facilities to JTable.
 *  Started with ExcelAdapter code at 
 *
 *     http://www.javaworld.com/javaworld/javatips/jw-javatip77.html
 *     Copyright (c) 2003 JavaWorld.com, an IDG company 
 *
 *  From documentation of ExcelAdapter:
 *     ExcelAdapter enables Copy-Paste Clipboard functionality on JTables.
 *     The clipboard data format used by the adapter is compatible with
 *     the clipboard format used by Excel. This provides for clipboard
 *     interoperability between enabled JTables and Excel.
 *
 */

public final class JTableCutPasteAdapter {

  private final JTable jTable1;

  /**
   * An object that enables Copy-Paste on the given JTable.
   */
  public JTableCutPasteAdapter(JTable myJTable, boolean registerDefaultKeyStrokes) {
    jTable1 = myJTable;
    if (registerDefaultKeyStrokes) {
      registerKeyStrokes();
    }
    
    myJTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        copyAction.setEnabled(isValidSelectionForCopy());
        pasteAction.setEnabled(true);
      }
    });
  }
  
  /** Registers the default keyboard actions for Copy (control C) and Paste (control V). */
  private void registerKeyStrokes() {
    KeyStroke copyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_C, ActionEvent.CTRL_MASK, false);
    KeyStroke pasteStroke = KeyStroke.getKeyStroke(KeyEvent.VK_V, ActionEvent.CTRL_MASK, false);

    jTable1.registerKeyboardAction(copyAction , (String) copyAction.getValue(Action.NAME), 
        copyStroke, JComponent.WHEN_FOCUSED);
    jTable1.registerKeyboardAction(pasteAction, (String) pasteAction.getValue(Action.NAME), 
        pasteStroke, JComponent.WHEN_FOCUSED);
  }

 /**
  * Tests to see if the current selection area is suitable for copy action.
  * Selections comprising non-adjacent cells result in invalid selection and
  * then copy action cannot be performed.
  * Paste can be done as long as there is something selected.
  */
  private boolean isValidSelectionForCopy() {
    int numcols=jTable1.getSelectedColumnCount();
    int numrows=jTable1.getSelectedRowCount();
    int[] rowsselected=jTable1.getSelectedRows();
    int[] colsselected=jTable1.getSelectedColumns();
    if (numrows==0 || numcols==0) {
      return false;
    }
    boolean isValid = ((numrows-1==rowsselected[rowsselected.length-1]-rowsselected[0] &&
        numrows==rowsselected.length) &&
        (numcols-1==colsselected[colsselected.length-1]-colsselected[0] &&
        numcols==colsselected.length));
    return isValid;
  }
  
  private static void showInvalidSelectionMessage() {
    String msg = "Invalid selection";
    JOptionPane.showMessageDialog(null, msg, msg, JOptionPane.ERROR_MESSAGE);
  }
  
  public Action copyAction = new AbstractAction("Copy") {
    public void actionPerformed(ActionEvent e) {
      doCopy();
    }
  };
  
  public Action pasteAction = new AbstractAction("Paste") {
    public void actionPerformed(ActionEvent e) {
      try {
        doPaste();
      } catch (Exception ex){
        ErrorHandler.errorPanel("ERROR", ex);
      }
    }
  };
  
  private void doCopy() {
    StringBuffer sbf=new StringBuffer();
    // Check to ensure we have selected only a contiguous block of cells
    int numcols=jTable1.getSelectedColumnCount();
    int numrows=jTable1.getSelectedRowCount();
    int[] rowsselected=jTable1.getSelectedRows();
    int[] colsselected=jTable1.getSelectedColumns();
    if ( ! isValidSelectionForCopy()) {
      showInvalidSelectionMessage();
      return;
    }
    for (int i=0;i<numrows;i++) {
      for (int j=0;j<numcols;j++) {
        sbf.append(jTable1.getValueAt(rowsselected[i],colsselected[j]));
        if (j<numcols-1) sbf.append("\t");
      }
      sbf.append("\n");
    }

    StringSelection stsel  = new StringSelection(sbf.toString());
    Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
    system.setContents(stsel,stsel);
  }

  /**
   * Paste is done by aligning the upper left corner of the selection with the
   * First element in the current selection of the JTable.
   */
  private void doPaste() throws UnsupportedFlavorException, IOException {
    
    int startRow=(jTable1.getSelectedRows())[0];
    int startCol=(jTable1.getSelectedColumns())[0];
    Clipboard system = Toolkit.getDefaultToolkit().getSystemClipboard();
    String trstring= (String)(system.getContents(this).getTransferData(DataFlavor.stringFlavor));
        
    String rowstring,value;
    StringTokenizer st1=new StringTokenizer(trstring,"\n");
    for(int i=0;st1.hasMoreTokens();i++) {
      rowstring=st1.nextToken();
      StringTokenizer st2=new StringTokenizer(rowstring,"\t");
      for(int j=0;st2.hasMoreTokens();j++) {
        value=st2.nextToken();
        if (startRow+i< jTable1.getRowCount()  &&  startCol+j< jTable1.getColumnCount()) {
          jTable1.setValueAt(value,startRow+i,startCol+j);
        }
      }
    }
  }

}

