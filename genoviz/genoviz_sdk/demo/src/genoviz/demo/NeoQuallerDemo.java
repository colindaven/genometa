/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
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

package genoviz.demo;

import java.applet.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.*;

import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.datamodel.*;
import com.affymetrix.genoviz.parser.*;
import com.affymetrix.genoviz.widget.*;
import java.util.Hashtable;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 *
 * @author NeoQuallerDemo.java 4946 2010-01-07 22:56:59Z hiralv $
 */
public class NeoQuallerDemo extends Applet
	implements ActionListener, NeoRangeListener {

	NeoPanel widg_pan;
	boolean clone_in_same_frame = true;
	boolean framed = false;

	ReadConfidence read_conf;
	NeoQualler widget;
	NeoQualler oneClone;

	int pixel_width = 500;
	int pixel_height = 250;

	TextField posText;
	TextField strText;
	Label posLabel;
	Label strLabel;
	Label descLabel;
	Choice searchChoice;
	Button cloneButton;
	Panel controlPanel;

	Menu editMenu = new Menu("Edit");
	MenuItem propertiesMenuItem = new MenuItem("Properties...");
	Frame propFrame; // For Properties
	private boolean propFrameShowing = false;

	public NeoQuallerDemo() {
		editMenu.addSeparator();
		editMenu.add(propertiesMenuItem);
		propertiesMenuItem.addActionListener(this);
		controlPanel = constructControlPanel();
		widget = new NeoQualler();
		widget.addRangeListener(this);
	}

	@Override
	public String getAppletInfo() {
		return ("Demonstration of genoviz Software's Quality Scores Viewing Widget");
	}

	@Override
	public void init() {

		String seq = getParameter("seqFile");
		String phred = getParameter("phredFile");
		URL seqURL = null;
		URL phredURL = null;

		try {
			seqURL = new URL(getDocumentBase(),seq);
			phredURL = new URL(getDocumentBase(),phred);
			init(seqURL, phredURL);
			widget.setBasesTrimmedLeft(-10);
			widget.setBasesTrimmedRight(1);
		}
		catch (MalformedURLException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void init(URL seqURL, URL phredURL) {
		PhredParser pp = new PhredParser();
		read_conf = pp.parseFiles( seqURL, phredURL );
		String filestr = seqURL.getFile();
		int tempint = filestr.lastIndexOf('/');
		if (tempint != -1) {
			filestr = filestr.substring(tempint+1);
		}
		init(filestr, read_conf);
	}

	public void init(ReadConfidence read_conf) {
		init("",read_conf);
	}

	public void init (String name, ReadConfidence read_conf) {

		this.read_conf = read_conf;

		descLabel.setText(name + ": " + read_conf.getReadLength() + " bases");

		((Component)widget).setSize(pixel_width, pixel_height);

		widget.setReadConfidence(read_conf);
		widget.updateWidget();

		/**
		 *  All NeoWidgets in this release are lightweight components.
		 *  Placing a lightweight component inside a standard Panel often
		 *  causes flicker in the repainting of the lightweight components.
		 *  Therefore the GenoViz includes the NeoPanel, a special subclass
		 *  of Panel, that is designed to support better repainting of
		 *  NeoWidgets contained withing it.  Note however that if you are
		 *  using the widgets within a lightweight component framework
		 *  (such as Swing), you should _not_ wrap them with a NeoPanel
		 *  (since the NeoPanel is a heavyweight component).
		 */
		widg_pan = new NeoPanel();
		widg_pan.setLayout(new BorderLayout());
		widg_pan.add("Center", (Component)widget);

		if (framed) {
			Frame framer = new Frame("Quality Viewer");
			framer.setLayout(new BorderLayout());
			framer.add("Center", widg_pan);
			framer.setSize(400, 200);
			framer.setVisible(true);//framer.show();
		}

		else {
			this.setLayout(new BorderLayout());
			add("Center", widg_pan);
		}

		String param;
		param = getParameter("noControlPanel");
		if (null == param) {
			add("North", controlPanel);
		}

	}

	protected Panel constructControlPanel() {
		descLabel = new Label();
		posText = new TextField(3);
		posText.addActionListener(this);
		posLabel = new Label("Center At Loc:");
		strText = new TextField(10);
		strText.addActionListener(this);
		strLabel = new Label("Center At String:");
		searchChoice = new Choice();
		searchChoice.addItem("Next");
		searchChoice.addItem("Prev");
		searchChoice.addItem("First");
		searchChoice.addItem("Last");
		cloneButton = new Button("Clone");
		cloneButton.addActionListener(this);
		Panel cp = new Panel();
		cp.setBackground(Color.white);
		cp.add(descLabel);
		cp.add(posLabel);
		cp.add(posText);
		cp.add(strLabel);
		cp.add(strText);
		cp.add(searchChoice);
		cp.add(cloneButton);
		return cp;
	}

	@Override
	public void start() {

		/*
		// Add in this code if you are putting the demo in a frame.
		Container parent;
		parent = this.getParent();
		while (null != parent && ! (parent instanceof Frame)) {
		parent = parent.getParent();
		}
		MenuBar mbar = ((Frame)parent).getMenuBar();
		mbar.add(editMenu);
		/* */

		if (null != propFrame && propFrameShowing) {
			propFrame.setVisible(true);//propFrame.show();
		}
		super.start();

		widget.zoomRange(1.0f);
		widget.scrollRange(0.0f);
	}

	@Override
	public void stop() {
		Container parent;
		parent = this.getParent();
		while (null != parent && ! (parent instanceof Frame)) {
			parent = parent.getParent();
		}
		if (null != propFrame) {
			propFrameShowing = propFrame.isVisible();
			propFrame.setVisible(false);
		}
		super.stop();
	}

	public void showProperties() {
		if (null == propFrame) {
			propFrame = new Frame("NeoQualler Properties");
			NeoQuallerCustomizer customizer = new NeoQuallerCustomizer();
			customizer.setObject(this.widget);
			propFrame.add("Center", customizer);
			propFrame.pack();
			propFrame.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					propFrameShowing = false;
					propFrame.setVisible(false);
				}
			});
		}
		propFrame.setBounds(200, 200, 300, 150);
		propFrame.setVisible(true);//propFrame.show();
	}

	public void centerAtBase(int baseNum) {
		widget.centerAtBase(baseNum);
	}

	public void clearSelection() {
		widget.clearSelection();
	}

	int prevSearchPosition = -1;

	public void actionPerformed(ActionEvent evt)  {
		Object evtSource = evt.getSource();
		if (evtSource == posText) {
			try {
				int basenum = Integer.parseInt(posText.getText());
				centerAtBase(basenum);
			}
			catch(Exception ex) {
				System.out.println("parse error");
			}
		}
		else if (evtSource == strText) {
			String searchString = strText.getText();
			String seqString = read_conf.getBaseString();
			String searchOption = searchChoice.getSelectedItem();
			int basenum = -1;
			if (searchOption == "First") {
				basenum = seqString.indexOf(searchString);
			}
			else if (searchOption == "Last") {
				basenum = seqString.lastIndexOf(searchString);
			}
			else if (searchOption == "Next") {
				basenum = seqString.indexOf(searchString, prevSearchPosition+1);
			}
			else if (searchOption == "Prev") {
				basenum = seqString.lastIndexOf(searchString, prevSearchPosition-1);
			}
			if (basenum == -1) {
				System.out.println("Sequence not found");
			}
			else {
				System.out.println("Centering at " + basenum);
				centerAtBase(basenum);
				prevSearchPosition = basenum;
			}
		}
		else if (evtSource == cloneButton) {
			cloneWidget();
			cloneButton.setEnabled(false);
		}
		else if (evtSource == propertiesMenuItem) {
			showProperties();
		}
	}

	public void cloneWidget() {
		oneClone = new NeoQualler(widget);
		if (clone_in_same_frame) {
			widg_pan.remove((Component)widget);
			widg_pan.setLayout(new GridLayout(0, 1));
			widg_pan.add((Component)widget);
			widg_pan.add((Component)oneClone);
			doLayout();
			validate();
		}
		else {
			Frame cloneFrame = new Frame("NeoTracer clone");
			NeoPanel new_pan = new NeoPanel();
			new_pan.setLayout(new BorderLayout());
			new_pan.add("Center", (Component)oneClone);
			cloneFrame.setLayout(new BorderLayout());
			cloneFrame.add("Center", new_pan);
			cloneFrame.setSize(400, 200);
			cloneFrame.setVisible(true);//cloneFrame.show();
		}
	}

	public void rangeChanged(NeoRangeEvent evt) {
	}

	@Override
	public URL getCodeBase()
	{
		if (isApplication) {
				return this.getClass().getResource("/");
			}
		return super.getCodeBase();
	}


	@Override
	public AppletContext getAppletContext()
	{
		if(isApplication)
			return null;
		return super.getAppletContext();
	}


	@Override
	public URL getDocumentBase()
	{
		if(isApplication)
			return getCodeBase();
		return super.getDocumentBase();
	}

	@Override
	public String getParameter(String name)
	{
		if(isApplication)
			return parameters.get(name);
		return super.getParameter(name);
	}

	static Boolean isApplication = false;
	static Hashtable<String,String> parameters;
	static public void main(String[] args)
	{
		isApplication = true;
		NeoQuallerDemo me = new NeoQuallerDemo();
		parameters = new Hashtable<String, String>();
		parameters.put("seqFile","data/qualtest.seq");
		parameters.put("phredFile","data/qualtest.phred");
		me.init();
		me.start();
		JFrame frm = new JFrame("Genoviz NeoQualler Demo");
		frm.getContentPane().add("Center", me);
		JButton properties = new JButton("Properties");
		frm.getContentPane().add("South", properties);
		frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frm.pack();
		//frm.setBounds(20, 40, 900, 400);
		frm.setVisible(true);
	}

}
