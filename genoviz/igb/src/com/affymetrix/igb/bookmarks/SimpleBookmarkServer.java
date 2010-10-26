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

import com.affymetrix.igb.Application;

import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import java.net.*;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *  A very simple servlet that listens for bookmark commands as http GET requests.
 *  The server reads only the single "GET" line from the header,
 *  ignores all other input, returns no output, and closes the connection.
 */
public final class SimpleBookmarkServer {
  private static final int default_server_port = 7085;

  /** The OLD name of the IGB servlet, "UnibrowControl". */
  final static String SERVLET_NAME_OLD = "UnibrowControl";

  /** The current name of the IGB servlet, "IGBControl". Current versions of
   *  IGB will respond to both this and {@link #SERVLET_NAME_OLD}, but versions
   *  up to and including 4.56 will respond ONLY to the old name.
   */
  final static String SERVLET_NAME = "IGBControl";
  private static int ports_to_try = 5;
  private int server_port;
  final static byte[] prompt = "igb >>> ".getBytes();

  /** The basic localhost URL that starts a call to IGB; for backwards-compatibility
   *  with versions of IGB 4.56 and earlier, the old name {@link #SERVLET_NAME_OLD}
   *  is used.
   */
  static final String DEFAULT_SERVLET_URL = "http://localhost:"
      + default_server_port + "/" + SERVLET_NAME_OLD;

  public SimpleBookmarkServer(Application app) {
    try {

      server_port = findAvailablePort();

      if (server_port == -1) {
		  Logger.getLogger(SimpleBookmarkServer.class.getName()).log(Level.SEVERE,
            "Couldn't find an available port for IGB to listen to control requests!\n"
        + "Turning off IGB's URL-based control features");
      }
      else {
        ServerSocket server = new ServerSocket(server_port);


        while (true) {
          Socket socket = server.accept();
		  Logger.getLogger(SimpleBookmarkServer.class.getName()).log(Level.FINE,
			"Connection accepted " +
                  socket.getInetAddress() +
                  ":" + socket.getPort());
		  socket.getOutputStream().write(prompt);
          BookmarkHttpRequestHandler request = new BookmarkHttpRequestHandler(app, socket);
          Thread thread = new Thread(request);
          thread.start();
        }
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  private int findAvailablePort() {
    // find an available port, starting with the default_server_point and
      //   incrementing up from there...
    int ports_tried = 0;
    server_port = default_server_port - 1;
    boolean available_port_found = false;
    while ((!available_port_found) && (ports_tried < ports_to_try)) {
      server_port++;
      URL test_url;
      try {
        test_url = new URL("http://localhost:" + server_port +
                "/" + SERVLET_NAME + "?ping=yes");
      } catch (MalformedURLException mfe) {
        return -1;
      }


      try {
        // try and find an open port...
        URLConnection conn = test_url.openConnection();
				conn.setConnectTimeout(LocalUrlCacher.CONNECT_TIMEOUT);
				conn.setReadTimeout(LocalUrlCacher.READ_TIMEOUT);
        conn.connect();
        // if connection is successful, that means we cannot use that port
        // and must try another one.
        ports_tried++;
      } catch (IOException ex) {
		  Logger.getLogger(SimpleBookmarkServer.class.getName()).log(Level.INFO,
				  "Found available port for bookmark server: " + server_port);
        available_port_found = true;
      }
    }

    if (available_port_found) {
      return server_port;
    } else {
      return -1;
    }
  }
}
