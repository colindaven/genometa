/**
*   Copyright (c) 2005-2006 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.util;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import javax.swing.*;

import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @version $Id: ConsoleView.java 6337 2010-07-02 18:24:53Z hiralv $
 */
public final class ConsoleView {
	private static final String encoding;

	static {
		String enc = System.getProperty("file.encoding");
		encoding = enc == null || enc.isEmpty() ? "UTF-8" : enc;
	}
   
  private static JFrame frame;
    
  /**
   *  Call this to create and initialize the singleton JFrame and
   *  to start the redirection of standard out and err streams into it.
   *  Call {@link #showConsole()} or {@link #getFrame()} when you are
   *  ready to display the frame.
   */
  public static void init(String APP_NAME) {
    getFrame(APP_NAME);
  }
  
   /**
   *  Displays the console and brings it to the front.
   *  If necessary, it will be de-iconified.
   *  This will call {@link #init()} if necessary, but
   *  it is better for you to call init() at the time you want
   *  the console to begin working. 
   */
  public static void showConsole(String APP_NAME) {
    if (frame == null) {
      init(APP_NAME);
    }
    frame.doLayout();
    frame.repaint();
    
    DisplayUtils.bringFrameToFront(frame);
  }
  
  /**
   *  Returns the JFrame that holds the console, creating it if necessary,
   *  but not displaying it if it isn't already displayed.
   *  If you want to display the frame, call {@link #showConsole()} instead.
   */
  private static JFrame getFrame(String APP_NAME) {
	  String TITLE =  TITLE = APP_NAME + " Console";
    if (frame == null) {
      frame = createFrame(TITLE);
      Container cpane = frame.getContentPane();
      cpane.setLayout(new BorderLayout());

      JScrollPane outPane = createOutPane();

      cpane.add(outPane, BorderLayout.CENTER);
      frame.pack(); // set to default size based on contents

      // then try to get size from preferences
      Rectangle pos = PreferenceUtils.retrieveWindowLocation(TITLE, frame.getBounds());
      if (pos != null) {
        PreferenceUtils.setWindowSize(frame, pos);
      }
    }
    return frame;
  }
  
  private static JScrollPane createOutPane() {
    // if it is ever necessary to make a public getOutPane() method,
    // we'll need to make sure that createOutPane() is only called once.

    JTextArea outArea = new JTextArea(20, 50);
    outArea.setEditable(false);
    
    JScrollPane outPane = new JScrollPane(outArea,
      ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
      ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
        
    try {
      // Send err to same text area as out
      // (But we could send err to a separate text area.)
      System.setOut(new PrintStream(new JTextAreaOutputStream(outArea, System.out), false, encoding));
      System.setErr(new PrintStream(new JTextAreaOutputStream(outArea, System.err), false, encoding));
	} catch (UnsupportedEncodingException ex) {
		Logger.getLogger(ConsoleView.class.getName()).log(Level.SEVERE, null, ex);
	} catch (SecurityException se) {
      // This exception should not occur with WebStart, but I'm handling it anyway.
      
      String str = "The application may not have permission to re-direct output "
       + "to this view on your system.  "
       + "\n"
       + "You should be able to view output in the Java console, WebStart console, "
       + "or wherever you normally would view program output.  "
       + "\n\n";
      outArea.append(str);
    }

    return outPane;
  }
 
  /**
   *  Creates a JFrame to hold the console.
   */
  private static JFrame createFrame(final String TITLE) {
    final JFrame frame = new JFrame(TITLE);

    ImageIcon icon = MenuUtil.getIcon("toolbarButtonGraphics/development/Host16.gif");
    if (icon != null) { frame.setIconImage(icon.getImage()); }

    frame.addWindowListener( new WindowAdapter() {
      public void windowClosing(WindowEvent evt) {
        PreferenceUtils.saveWindowLocation(frame, TITLE);
      }
    });
    
    return frame;
  }


  /**
   *  A class to help send a PrintStream, such as System.out,
   *  to a  JTextArea.  Adapted from code in the book "Swing Hacks"
   *  by Joshua Marinacci and Chris Adamson.  This is from hack #95.
   *  This sort of use of the code is allowed (even without attribution).
   *  See the preface of their book for details.
   */
  private static final class JTextAreaOutputStream extends OutputStream {
	  private static final Charset charset = Charset.forName(ConsoleView.encoding);
    JTextArea ta;
    PrintStream original;
    
    /**
     *  Creates an OutputStream that writes to the given JTextArea.
     *  @param echo  Can be null, or a PrintStream to which a copy of all output
     *    will also by written.  Thus you can send System.out to a text area
     *    and also still send an echo to the original System.out.
     */
    public JTextAreaOutputStream(JTextArea t, PrintStream echo) {
      this.ta = t;
      this.original = echo;
    }
    
    public void write(int b) throws IOException{
		write(new byte[] {(byte)b}, 0, 1);
    }

	@Override
	public void write(byte b[]) throws IOException {
		write(b, 0, b.length);
	}

	@Override
	public void write(byte b[], int off, int len) throws IOException {
		ta.append(new String(b, off, len, charset));
		if (original != null) { original.write(b, off, len); }
	}
  }
  
}
