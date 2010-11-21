package com.affymetrix.genometry.servlets;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import org.mortbay.http.HttpContext;
import org.mortbay.http.HttpServer;
import org.mortbay.http.SocketListener;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;


/**
 *  Pure java web server wrapper around GenometryDas2Servlet
 */
public final class GenometryDas2Server {

	//  static int default_server_port = 9092;
	static boolean SHOW_GUI = false;

	public static void main (String[] args) throws Exception {
		final HttpServer server=new HttpServer();
		//    int server_port = default_server_port;
		if (args.length < 3) {
			System.out.println("Usage: ... GenometryDas2Server port data_path admin_email xml_base");
			System.exit(0);
		}
		int server_port = Integer.parseInt(args[0]);
		String data_path = args[1];
		String admin_email = args[2];
		String xml_base = args[3];
		System.setProperty("das2_genometry_server_dir", data_path);
		System.setProperty("das2_maintainer_email", admin_email);
		System.setProperty("das2_xml_base", xml_base);

		if (SHOW_GUI) {
			final JFrame frm = new JFrame("Genometry Server");
			java.awt.Container cpane = frm.getContentPane();
			cpane.setLayout(new BorderLayout());
			JButton exitB = new JButton("Quit Server");
			exitB.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
					try {
						server.stop(); // should call servlet.destroy() for every servlet that's been init()ed
					}
					catch (Exception ex) { ex.printStackTrace(); }
					frm.setVisible(false);
					System.exit(0);
				}
			}
			);
			cpane.add("Center", exitB);
			frm.setSize(300, 80);
			//      frm.pack();
			frm.setVisible(true);
		}

		// Create a port listener
		SocketListener listener = new SocketListener();
		listener.setPort(server_port);
		server.addListener(listener);

		// Create a context
		HttpContext context = new HttpContext();
		//    context.setContextPath("/mystuff/*");
		context.setContextPath("/");

		// Create a servlet container
		ServletHandler servlets = new ServletHandler();
		context.addHandler(servlets);

		// Map a servlet onto the container
		ServletHolder das_holder =
			servlets.addServlet("GenometryDas2Servlet", "/das2/*",
					"com.affymetrix.genometry.servlets.GenometryDas2Servlet");
		das_holder.setInitOrder(1);  // ensure servlet init() is called on startup

		// Serve static content from the context
		//    String home = System.getProperty("jetty.home",".");
		//    context.setResourceBase(home+"/demo/webapps/jetty/tut/");
		//    context.setResourceBase("C:/JavaExtras/jetty/Jetty-4.1.0/demo/webapps/jetty/tut/");
		//    context.addHandler(new ResourceHandler());
		server.addContext(context);

		// Start the http server
		server.start();

		GenometryDas2Servlet das_servlet = (GenometryDas2Servlet)das_holder.getServlet();
		//    das_servlet.addCommandPlugin("psd_query", "com.affymetrix.genometry.servlets.ProbeSetDisplayPlugin");
		//    das_servlet.addCommandPlugin("proximity", "com.affymetrix.genometry.servlets.ProximityQueryPlugin");
		das_servlet.setXmlBase(xml_base);
	}
}


