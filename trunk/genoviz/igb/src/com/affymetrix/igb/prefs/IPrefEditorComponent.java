/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.prefs;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 *  An interface that should be implemented by any JComponent that can be used 
 *  to view and/or modify a sub-set of the preferences used by the program.
 *  There is no requirement that the preferences be stored using the
 *  java.util.prefs package, but that is what is generally expected.
 */
public abstract class IPrefEditorComponent extends JPanel {
  /**
   *  Gives help text explaining the function of this preferences editor component.
   *  If no help is available, should return null rather than an empty String.
   *  The help text should describe what effect changes in the preferences
   *  in the panel will have, how to make the changes (if it isn't obvious),
   *  and whether the changes are expected to take effect immediately or only
   *  after a re-start.
   *  @return Text in HTML format, or null
   */
  public String getHelpTextHTML() {
		StringBuilder builder = new StringBuilder();
		char buffer[] = new char[4096];
		InputStream stream = null;
		Reader reader = null;

		try {
			stream = this.getClass().getResourceAsStream("/help/" + this.getClass().getName() + ".html");
			reader = new InputStreamReader(stream, "UTF-8");

			for (int read = reader.read(buffer, 0, buffer.length); read >0; read = reader.read(buffer, 0, buffer.length)) {
				builder.append(buffer);
			}

		} catch (UnsupportedEncodingException ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "UTF-8 is not a supported encoding?!", ex);
		} catch (IOException ex) {
			Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Unable to load help file for " + this.getClass().getName(), ex);
		} finally {
			GeneralUtils.safeClose(reader);
			GeneralUtils.safeClose(stream);
		}

		return builder.toString();
	}

  /**
   *  Causes the JComponent to update its fields
   *  so that they match what is stored in the java preferences.
   *  Some implementations may be listening to preference change events and
   *  generally keep up-to-date without needing this method.  
   *  But this method may be called after large events, such as importing
   *  an xml file containing preferences, or after deleting stored preferences.
   */
  public abstract void refresh();
}
