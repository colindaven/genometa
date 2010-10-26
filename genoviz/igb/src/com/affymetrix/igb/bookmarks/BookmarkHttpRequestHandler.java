/**
*   Copyright (c) 2007 Affymetrix, Inc.
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

package com.affymetrix.igb.bookmarks;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.util.ScriptFileLoader;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

class BookmarkHttpRequestHandler implements Runnable {

  private final Socket socket;
  private final Application app;
  private static final String NO_CONTENT = "HTTP/1.x 204 No Content\r\n\r\n";

  public BookmarkHttpRequestHandler(Application app, Socket socket) {
    this.socket = socket;
    this.app = app;
  }


  public void run() {
    try {
      processRequest();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

 private void processRequest() throws IOException {
		BufferedReader reader = null;
		OutputStream output = null;
		
		String line;
		try {
			reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			output = socket.getOutputStream();
			while ((line = reader.readLine()) != null) {
				// we need to process only the GET header line of the input, which will
				// look something like this:
				// 'GET /IGBControl?version=hg18&seqid=chr17&start=43966897&end=44063310 HTTP/1.1'

				String command = null;
				if (line.length() >= 4 && line.substring(0, 4).toUpperCase().equals("GET ")) {
					String[] getCommand = line.substring(4).split(" ");
					if (getCommand.length > 0) {
						command = getCommand[0];
					}
				}


				if (command != null) {
					parseAndGoToBookmark(command);
				} else {
					ScriptFileLoader.doSingleAction(line);
				}

				output.write(SimpleBookmarkServer.prompt);
			}
		} finally {

			GeneralUtils.safeClose(output);
			GeneralUtils.safeClose(reader);
			try {
				socket.close();
			} catch (Exception e) {
				// do nothing
			}
		}

	}

	private void parseAndGoToBookmark(String command) throws NumberFormatException {
		Logger.getLogger(BookmarkHttpRequestHandler.class.getName()).log(Level.FINE,
				"Command = " + command);
		// at this point, the command will look something like this:
		// '/IGBControl?version=hg18&seqid=chr17&start=43966897&end=44063310'
		//TODO: We could check to see that the command is "IGBControl" or "UnibrowControl",
		// but since that is the only command we ever expect, we can just assume for now.
		String params = null;
		int index = command.indexOf('?');
		if (index >= 0 && index < command.length()) {
			params = command.substring(index + 1);
			Map<String, String[]> paramMap = new HashMap<String, String[]>();
			Bookmark.parseParametersFromQuery(paramMap, params, true);
			UnibrowControlServlet.goToBookmark(app, paramMap);
		}
	}
}
