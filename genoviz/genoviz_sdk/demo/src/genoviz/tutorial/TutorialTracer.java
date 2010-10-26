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
import com.affymetrix.genoviz.parser.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.widget.neotracer.TraceGlyph;

/**
 *
 * @version $Id: TutorialTracer.java 4917 2010-01-04 22:37:23Z hiralv $
 */
public class TutorialTracer extends Applet implements ActionListener {
	TraceI trace;
	NeoTracer widget;
	Adjustable xzoomer, yzoomer;
	Frame zoomframe = new Frame("Zoom controls");
	NeoPanel pan1, pan2;

	boolean optScrolling = false, optDamage = false;

	int pixel_width = 500;
	int pixel_height = 250;

	int scroll_value = 100;
	float xzoom_value = 2.0f;
	float yzoom_value = 0.1f;

	Button scrollLeftB, scrollRightB, optDamageB, optScrollingB;
	Button toggleTraceB, toggleRevCompB;
	Button xzoomB, yzoomB;

	TextField posText;
	TextField strText;
	Label posLabel;
	Label strLabel;
	Label descLabel;
	Choice searchChoice;
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
				if (null != widget) {
					widget.updateWidget();
				}
			}
		}

	};

	public TutorialTracer() {
		widget = new NeoTracer();
		descLabel = new Label("");

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
		return ("Demonstration of genoviz Software's Trace Viewing Widget");
	}

	@Override
	public void init() {

		editMenu.addSeparator();
		editMenu.add(propertiesMenuItem);
		propertiesMenuItem.addActionListener(this);

		xzoomB = new Button("XZoom");
		xzoomB.addActionListener(this);
		yzoomB = new Button("YZoom");
		yzoomB.addActionListener(this);

		scrollLeftB = new Button("Scroll Left");
		scrollLeftB.addActionListener(this);
		scrollRightB = new Button("Scroll Right");
		scrollRightB.addActionListener(this);
		optScrollingB = new Button("No Scroll Opt");
		optScrollingB.addActionListener(this);
		optDamageB = new Button("No Damage Opt");
		optDamageB.addActionListener(this);
		toggleTraceB = new Button("Toggle G");
		toggleTraceB.addActionListener(this);
		toggleRevCompB = new Button("Toggle RevComp");
		toggleRevCompB.addActionListener(this);

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

		controlPanel = new Panel();

		controlPanel.add(descLabel);
		controlPanel.add(strLabel);
		controlPanel.add(strText);
		controlPanel.add(searchChoice);

		controlPanel.add(toggleRevCompB);
		controlPanel.add(toggleTraceB);

		controlPanel.add(optScrollingB);
		controlPanel.add(optDamageB);

		widget.setSize(pixel_width, pixel_height);

		widget.addMouseListener(this.mouser);

		this.setLayout(new BorderLayout());
		NeoPanel p = new NeoPanel((Component)widget);
		add("Center", p);
		add("North", controlPanel);

		String filestr = new String();
		try {
			String scff = getParameter("scf_file");
			if (scff != null) {
				URL scfURL = new URL(this.getDocumentBase(), scff);
				if ( null != scfURL ) {
					SCFTraceParser scfp = new SCFTraceParser();
					Trace t = (Trace) scfp.importContent(scfURL.openStream());
					setTrace(t);
					filestr = scfURL.getFile();
				}
			}
			else {
				String abif = getParameter("abi_file");
				if (null != abif) {
					URL abiURL = new URL(this.getDocumentBase(), abif);
					if ( null != abiURL ) {
						ABITraceParser abip = new ABITraceParser();
						Trace t = (Trace) abip.importContent(abiURL.openStream());
						setTrace(t);
						filestr = abiURL.getFile();
					}
				}
			}
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}

		if (null != trace) {

			int tempint = filestr.lastIndexOf('/');
			if (tempint != -1) {
				filestr = filestr.substring(tempint+1);
			}

			int trace_length = trace.getTraceLength();
			widget.setLeftTrim(152);
			widget.setRightTrim(trace_length-100);

			setTraceLabel(filestr + ": " + trace.getBaseCount() + " bases");

		}
	}

	public void setTrace(TraceI trace) {
		if (trace == null) {
			System.err.println("no trace!");
			return;
		}
		this.trace = trace;
		widget.setTrace(trace);
	}

	public void setTraceLabel(String label) {
		descLabel.setText(label);
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
			((Frame)parent).addWindowListener(this.hider);
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
		NeoTracer nt = widget;

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
			((Frame)parent).removeWindowListener(this.hider);
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

	public void centerAtBase(int baseNum) {
		widget.centerAtBase(baseNum);
		widget.updateWidget();
	}

	int prevSearchPosition = -1;

	public void actionPerformed(ActionEvent evt)  {
		Object evtSource = evt.getSource();
		if (evtSource == propertiesMenuItem) {
			if (null == propFrame) {
				propFrame = new Frame("NeoTracer Properties");
				NeoTracerCustomizer customizer = new NeoTracerCustomizer();
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
			String traceString = this.trace.getActiveBaseCalls().getBaseString();
			String searchOption = searchChoice.getSelectedItem();
			int basenum = -1;
			if (searchOption == "First") {
				basenum = traceString.indexOf(searchString);
			}
			else if (searchOption == "Last") {
				basenum = traceString.lastIndexOf(searchString);
			}
			else if (searchOption == "Next") {
				basenum = traceString.indexOf(searchString, prevSearchPosition+1);
			}
			else if (searchOption == "Prev") {
				basenum = traceString.lastIndexOf(searchString, prevSearchPosition-1);
			}
			if (basenum == -1) {
				System.out.println("Sequence not found");
			}
			else {
				centerAtBase(basenum);
				prevSearchPosition = basenum;
			}
		}
		else if (evtSource == xzoomB) {
			xzoom_value *= 1.1;
			widget.zoom(NeoTracer.X, xzoom_value);
			widget.updateWidget();
		}
		else if (evtSource == yzoomB) {
			yzoom_value *= 1.1;
			widget.zoom(NeoTracer.Y, yzoom_value);
			widget.updateWidget(true);
		}
		else if (evtSource == scrollLeftB) {
			scroll_value -= 5;
			widget.scroll(NeoTracer.X, scroll_value);
			widget.updateWidget();
		}
		else if (evtSource == scrollRightB) {
			scroll_value += 5;
			widget.scroll(NeoTracer.X, scroll_value);
			widget.updateWidget();
		}
		else if (evtSource == optScrollingB) {
			optScrolling = !optScrolling;
			widget.setScrollingOptimized(optScrolling);
			widget.updateWidget();
			if (optScrolling) {
				optScrollingB.setLabel("Opt Scrolling");
			}
			else {
				optScrollingB.setLabel("No Opt Scrolling");
			}
		}
		else if (evtSource == optDamageB) {
			optDamage = !optDamage;
			widget.setDamageOptimized(optDamage);
			widget.updateWidget();
			if (optDamage) {
				optDamageB.setLabel("Opt Damage");
			}
			else {
				optDamageB.setLabel("No Opt Damage");
			}
		}
		else if (evtSource == toggleTraceB) {
			widget.setVisibility(TraceGlyph.G, !widget.getVisibility(TraceGlyph.G));
			widget.updateWidget();
		}
		else if (evtSource == toggleRevCompB) {
			if (NeoTracer.FORWARD == widget.getDirection()) {
				widget.setDirection(NeoTracer.REVERSE_COMPLEMENT);
			}
			else {
				widget.setDirection(NeoTracer.FORWARD);
			}
			widget.updateWidget();
		}

	}


}
