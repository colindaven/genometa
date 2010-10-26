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

package genoviz.tutorial;

import java.applet.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.*;

import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.datamodel.*;
import com.affymetrix.genoviz.parser.PhredParser;
import com.affymetrix.genoviz.widget.*;

/**
 *
 * @author TutorialQualler.java 4917 2010-01-04 22:37:23Z hiralv $
 */
public class TutorialQualler extends Applet implements ActionListener {
	boolean framed = false;

	ReadConfidence read_conf;
	NeoQualler widget;
	NeoQualler oneClone;
	NeoPanel pan;

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

	protected WindowListener hider;

	protected MouseListener mouser = new MouseAdapter() {

		@Override
		public void mouseClicked(MouseEvent e) {
			if (widget == e.getSource()) {
				if (null != oneClone) {
					oneClone.updateWidget();
				}
			}
			else {
				if (null != widget) {
					widget.updateWidget();
				}
			}
		}

	};

	public TutorialQualler() {
		editMenu.addSeparator();
		editMenu.add(propertiesMenuItem);
		propertiesMenuItem.addActionListener(this);
		controlPanel = constructControlPanel();
		widget = new NeoQualler();

		this.hider = new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent evt){
				Frame f = (Frame)evt.getSource();
				f.setVisible(false);
				if (f == propFrame) {
					propFrameShowing = false;
				}
			}
		};
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
		}
		catch (MalformedURLException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	public void init(URL seqURL, URL phredURL) {
		read_conf = (new PhredParser()).parseFiles(seqURL, phredURL);
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
		widget.addMouseListener(this.mouser);
		widget.updateWidget();

		pan = new NeoPanel();
		pan.setLayout(new BorderLayout());
		this.setLayout(new BorderLayout());
		if (framed) {
			Frame framer = new Frame("Quality Viewer");
			framer.setLayout(new BorderLayout());
			pan.add("Center", (Component)widget);
			framer.add("Center", pan);
			framer.setSize(400, 200);
			framer.setVisible(true); //framer.show();
			framer.addWindowListener(this.hider);
		}

		else {
			pan.add("Center", (Component)widget);
			add("Center", pan);
		}

		Container parent;
		parent = this.getParent();
		while ( null != parent && ! ( parent instanceof Frame ) ) {
			parent = parent.getParent();
		}
		add("North", controlPanel);

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
		MenuBar bar;
		Container parent;
		parent = this.getParent();
		while (null != parent && ! (parent instanceof Frame)) {
			parent = parent.getParent();
		}
		if (null != parent && parent instanceof Frame) {
			Frame parentFrame = (Frame) parent;
			bar = parentFrame.getMenuBar();
			if (null == bar) {
				bar = new MenuBar();
				parentFrame.setMenuBar(bar);
			}
			bar.add(this.editMenu);
			parentFrame.addWindowListener(this.hider);
		}
		if (null != propFrame && propFrameShowing) {
			propFrame.setVisible(true); //propFrame.show();
		}
		super.start();

		widget.zoomRange(1.0f);
		widget.scrollRange(0.0f);
	}

	@Override
	public void stop() {
		MenuBar bar;
		Container parent;
		parent = this.getParent();
		while (null != parent && ! (parent instanceof Frame)) {
			parent = parent.getParent();
		}
		if (null != parent && parent instanceof Frame) {
			Frame parentFrame = (Frame) parent;
			bar = parentFrame.getMenuBar();
			if (null != bar) {
				bar.remove(this.editMenu);
			}
			parentFrame.removeWindowListener(this.hider);
		}
		if (null != propFrame) {
			propFrameShowing = propFrame.isVisible();
			propFrame.setVisible(false);
		}
		super.stop();
	}

	public void heardEvent(java.awt.Event evt) {
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

		if (evtSource == propertiesMenuItem) {
			if (null == propFrame) {
				propFrame = new Frame("NeoQualler Properties");
				NeoQuallerCustomizer customizer = new NeoQuallerCustomizer();
				customizer.setObject(this.widget);
				propFrame.add("Center", customizer);
				propFrame.pack();
				propFrame.addWindowListener(this.hider);
			}
			propFrame.setVisible(true); //propFrame.show();
		}
		else if (evtSource == posText) {
			try  {
				int basenum = Integer.parseInt(posText.getText());
				centerAtBase(basenum);
			}
			catch(Exception ex) { System.out.println("parse error"); }
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
				basenum = seqString.lastIndexOf(searchString,
						prevSearchPosition-1);
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
			oneClone = new NeoQualler(widget);
			oneClone.addMouseListener(this.mouser);

			remove((Component)widget);
			Panel p = new Panel();
			p.setLayout(new GridLayout(0, 1));
			p.add((Component)widget);
			p.add((Component)oneClone);
			add("Center", p);
			doLayout();
			validate();

			cloneButton.setEnabled(false);
		}
	}

}
