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
import java.awt.event.*;
import java.net.*;

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.datamodel.BaseCalls;
import com.affymetrix.genoviz.datamodel.Range;
import com.affymetrix.genoviz.datamodel.ReadConfidence;
import com.affymetrix.genoviz.datamodel.Trace;
import com.affymetrix.genoviz.datamodel.TraceI;
import com.affymetrix.genoviz.parser.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.widget.neotracer.TraceGlyph;
import java.util.Hashtable;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import javax.swing.plaf.basic.BasicArrowButton;

/**
 *
 * @version $Id: NeoTracerDemo.java 6332 2010-07-02 15:55:49Z vbishnoi $
 */
public class NeoTracerDemo extends Applet
		implements ActionListener, NeoRangeListener, ComponentListener {
	AppletContext appl;
	Applet app;
	NeoPanel widg_pan;
	boolean clone_in_same_frame = true;
	boolean optScrolling = false, optDamage = false;
	boolean external_zoomers = false;
	TraceI trace;
	public NeoTracer widget;
	NeoTracer oneClone;
	Adjustable xzoomer, yzoomer;
	Frame zoomframe = new Frame("Zoom controls");
	int pixel_width = 500;
	int pixel_height = 250;
	int next_or_prev = 0;
	int scroll_value = 100;
	float xzoom_value = 2.0f;
	float yzoom_value = 0.1f;
	static int cloneFlag = 0;
	Button scrollLeftB, scrollRightB, optDamageB, optScrollingB;
	Button toggleTraceB, toggleRevCompB;
	Button xzoomB, yzoomB;
	Button clipper;
	Button properties;
	TextField posText;
	Button findButton;
	TextField strText;
	Label posLabel;
	Label descLabel;
	Button cloneButton;
	Panel controlPanel;
	Menu editMenu = new Menu("Edit");
	MenuItem propertiesMenuItem = new MenuItem("Properties...");
	Frame propFrame; // For Properties
	private boolean propFrameShowing = false;
	private ActionListener nextAction = new ActionListener() {

		public void actionPerformed(ActionEvent evt) {
			next_or_prev = 1;
			searcher(next_or_prev);
		}
	};
	private ActionListener prevAction = new ActionListener() {

		public void actionPerformed(ActionEvent evt) {
			next_or_prev = 2;
			searcher(next_or_prev);
		}
	};
	private ActionListener propAction = new ActionListener() {

		public void actionPerformed(ActionEvent evt) {
			showProperties();
		}
	};

	private void searcher(int next_or_prev) {

		String searchString = strText.getText().toUpperCase();
		//System.out.println(searchString);
		if (searchString.length() < 1) {
			return;
		}
		String traceString;
		BaseCalls bc = ((Trace) trace).getActiveBaseCalls();
		
		traceString = bc.getBaseString().toUpperCase();
		int basenum = -1;
		if (next_or_prev == 1) {
			basenum = traceString.indexOf(searchString, prevSearchPosition + 1);
		} else if (next_or_prev == 2) {
			basenum = traceString.lastIndexOf(searchString,
					prevSearchPosition - 1);
		}
		if (basenum == -1) {
			showStatus("Could not find \"" + searchString + "\"");
		} else {
			//showStatus( "Found it starting at " + basenum );
			centerAtBase(basenum);
			prevSearchPosition = basenum;
			widget.selectResidues(basenum, basenum + searchString.length() - 1);
		}
	}

	public NeoTracerDemo() {
		if (external_zoomers) {
			widget = new NeoTracer(true, true, true);
			xzoomer = new JScrollBar(JScrollBar.HORIZONTAL);
			yzoomer = new JScrollBar(JScrollBar.HORIZONTAL);
			widget.setZoomer(NeoTracer.X, xzoomer);
			widget.setZoomer(NeoTracer.Y, yzoomer);
		} else {
			widget = new NeoTracer();
			//widget.setRange(1, 500);
		}
		widget.addRangeListener(this);
//		widget.addComponentListener(this);
		descLabel = new Label("");
		this.setLayout(new BorderLayout());
		this.widget.setBackground(Color.black);
		System.out.println(this.widget.getColorName(this.widget.getBackground()));
		// moved from init to constructor -- GAH 3-30-99
		widg_pan = new NeoPanel();
		widg_pan.setLayout(new BorderLayout());
		widg_pan.add("Center", (Component) widget);
		this.setLayout(new BorderLayout());
		this.add("Center", widg_pan);
		this.addComponentListener(this);

	}

	@Override
	public String getAppletInfo() {
		return ("Demonstration of genoviz Software's Trace Viewing Widget");
	}

	@Override
	public void init() {

		editMenu.addSeparator();
		editMenu.add(propertiesMenuItem);


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
		toggleRevCompB = new Button("RevComp");
		toggleRevCompB.addActionListener(this);
		clipper = new Button("Clip");
		clipper.addActionListener(this);
		properties= new Button("Properties");
		properties.addActionListener(propAction);
		posText = new TextField(3);
		posText.addActionListener(this);
		posLabel = new Label("Center At Loc:");
		strText = new TextField(10);
		strText.addTextListener(new TextListener() {

			public void textValueChanged(TextEvent e) {
				findButton.setEnabled(0 < strText.getText().length());
				prevSearchPosition = -1;
			}
		});
		strText.addActionListener(nextAction);
		findButton = new Button("Find");
		findButton.setEnabled(false);
		findButton.addActionListener(nextAction);
		JButton nextButton = new BasicArrowButton(SwingConstants.EAST);
		nextButton.addActionListener(nextAction);
		JButton prevButton = new BasicArrowButton(SwingConstants.WEST);
		prevButton.addActionListener(prevAction);
		cloneButton = new Button("Clone");
		cloneButton.addActionListener(this);

		controlPanel = new Panel();
		//    controlPanel.setBackground(Color.white);
		//    controlPanel.add(scrollLeftB);
		//    controlPanel.add(scrollRightB);

		controlPanel.add(descLabel);
		//    controlPanel.add(posLabel);
		//    controlPanel.add(posText);
		controlPanel.add(prevButton);
		controlPanel.add(strText);
		controlPanel.add(nextButton);
		controlPanel.add(findButton);
		controlPanel.add(cloneButton);

		//    controlPanel.add(xzoomB);
		//    controlPanel.add(yzoomB);

		controlPanel.add(toggleRevCompB);
		controlPanel.add(toggleTraceB);
		controlPanel.add(clipper);
		controlPanel.add(properties);

		//    controlPanel.add(optScrollingB);
		//    controlPanel.add(optDamageB);

		if (external_zoomers) {
			zoomFrameSetup();
		}

		((Component) widget).setSize(pixel_width, pixel_height);

		String param;
		param = getParameter("noControlPanel");
		if (null == param) {
			add("North", controlPanel);
		}

		String filestr = new String();
		try {
			String scff = getParameter("scf_file");
			if (scff != null) {
				URL scfURL = new URL(this.getDocumentBase(), scff);
				if (null != scfURL) {
					SCFTraceParser scfp = new SCFTraceParser();
					Trace t = (Trace) scfp.importContent(scfURL.openStream());
					setTrace(t);
					filestr = scfURL.getFile();
				}
			} else {
				String abif = getParameter("abi_file");
				if (null != abif) {
					URL abiURL = new URL(this.getDocumentBase(), abif);
					if (null != abiURL) {
						ABITraceParser abip = new ABITraceParser();
						Trace t = (Trace) abip.importContent(abiURL.openStream());
						setTrace(t);
						filestr = abiURL.getFile();
					}
				}
			}
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}

		param = getParameter("phd");
		if (null != param) {
			try {
				URL r = new URL(this.getDocumentBase(), param);
				if (null != r) {
					ReadConfidence rc = new ReadConfidence();
					ContentParser parser = new PHDReadConfParser();
					rc = (ReadConfidence) parser.importContent(r.openStream());
					widget.replaceBaseCalls(rc.getBaseCalls());
				}
			} catch (Exception phdex) {
				System.err.println(phdex.getMessage());
				phdex.printStackTrace();
			}
		}

		param = getParameter("addphd");
		if (null != param) {
			try {
				URL r = new URL(this.getDocumentBase(), param);
				if (null != r) {
					ReadConfidence rc = new ReadConfidence();
					ContentParser parser = new PHDReadConfParser();
					rc = (ReadConfidence) parser.importContent(r.openStream());
					widget.addBaseCalls(rc.getBaseCalls(), 0);
				}
			} catch (Exception phdex) {
				System.err.println(phdex.getMessage());
				phdex.printStackTrace();
			}
		}

		if (null != trace) {

			int tempint = filestr.lastIndexOf('/');
			if (tempint != -1) {
				filestr = filestr.substring(tempint + 1);
			}

			int trace_length = trace.getTraceLength();

			setTraceLabel(filestr + ": " + trace.getBaseCount() + " bases");

		}

		widget.setMinZoom(NeoTracer.X, 0.1f);
//		widget.setBasesTrimmedLeft(9);
//		widget.setBasesTrimmedRight(19);

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
		Container parent;
		parent = this.getParent();
		while (null != parent && !(parent instanceof Frame)) {
			parent = parent.getParent();
		}
		if (null != parent) {
			MenuBar mb = ((Frame) parent).getMenuBar();
			if (null != mb) {
				mb.add(editMenu);
			}
		}

		if (external_zoomers) {
			zoomframe.setVisible(true); //zoomframe.show();
		}
		if (null != propFrame && propFrameShowing) {
			propFrame.setVisible(true);//propFrame.show();
		}
		super.start();
		NeoTracer nt = widget;

	}

	@Override
	public void stop() {
		Container parent;
		parent = this.getParent();
		while (null != parent && !(parent instanceof Frame)) {
			parent = parent.getParent();
		}
		if (null != parent) {
			MenuBar mb = ((Frame) parent).getMenuBar();
			if (null != mb) {
				mb.remove(editMenu);
			}
		}
		if (null != propFrame) {
			propFrameShowing = propFrame.isVisible();
			propFrame.setVisible(false);
		}
		super.stop();
	}

	public void showProperties() {
		if (null == propFrame) {
			propFrame = new Frame("NeoTracer Properties");
			NeoTracerCustomizer customizer = new NeoTracerCustomizer();
			customizer.setObject(this.widget);
//			customizer.setSelectedChoiceColor();
			System.out.println(this.widget.getColorName(this.widget.getBackground()));
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
		propFrame.setBounds(200, 200, 500, 300);
		propFrame.setVisible(true);//propFrame.show();
	}

	public void centerAtBase(int baseNum) {
		widget.centerAtBase(baseNum);
		widget.updateWidget();
	}
	int prevSearchPosition = -1;

	public void actionPerformed(ActionEvent evt) {
		Object evtSource = evt.getSource();
		if (evtSource == posText) {
			try {
				int basenum = Integer.parseInt(posText.getText());
				centerAtBase(basenum);
			} catch (Exception ex) {
				System.out.println("parse error");
			}
		} else if (evtSource == xzoomB) {
			xzoom_value *= 1.1;
			widget.zoom(NeoTracer.X, xzoom_value);
			widget.updateWidget();
		} else if (evtSource == yzoomB) {
			yzoom_value *= 1.1;
			widget.zoom(NeoTracer.Y, yzoom_value);
			widget.updateWidget(true);
		} else if (evtSource == scrollLeftB) {
			scroll_value -= 5;
			widget.scroll(NeoTracer.X, scroll_value);
			widget.updateWidget();
		} else if (evtSource == scrollRightB) {
			scroll_value += 5;
			widget.scroll(NeoTracer.X, scroll_value);
			widget.updateWidget();
		} else if (evtSource == optScrollingB) {
			optScrolling = !optScrolling;
			widget.setScrollingOptimized(optScrolling);
			widget.updateWidget();
			if (optScrolling) {
				optScrollingB.setLabel("Opt Scrolling");
			} else {
				optScrollingB.setLabel("No Opt Scrolling");
			}
		} else if (evtSource == optDamageB) {
			optDamage = !optDamage;
			widget.setDamageOptimized(optDamage);
			widget.updateWidget();
			if (optDamage) {
				optDamageB.setLabel("Opt Damage");
			} else {
				optDamageB.setLabel("No Opt Damage");
			}
		} else if (evtSource == toggleTraceB) {
			widget.setVisibility(TraceGlyph.G, !widget.getVisibility(TraceGlyph.G));
			widget.updateWidget();
			if (cloneFlag == 1) {
				oneClone.updateWidget();
			}
		} else if (evtSource == clipper) {
			clipTrace(widget.getSel_range_start(), widget.getSel_range_end());
			//clipTrace(100, 200);
			widget.updateWidget();
		} else if (evtSource == toggleRevCompB) {
			if (NeoTracer.FORWARD == widget.getDirection()) {
				widget.setDirection(NeoTracer.REVERSE_COMPLEMENT);

			} else {
				widget.setDirection(NeoTracer.FORWARD);

			}
			widget.updateWidget();
		} else if (evtSource == cloneButton) {
			cloneFlag = 1;
			cloneWidget();
			cloneButton.setEnabled(false);
		}
	}

	public void cloneWidget() {
		oneClone = new NeoTracer(widget);
		if (clone_in_same_frame) {
			widg_pan.remove((Component) widget);
			widg_pan.setLayout(new GridLayout(0, 1));
			widg_pan.add((Component) widget);
			widg_pan.add((Component) oneClone);
			oneClone.setMinZoom(NeoTracer.X, 0.1f);
			doLayout();
			validate();
		} else {
			Frame cloneFrame = new Frame("NeoTracer clone");
			cloneFrame.setLayout(new BorderLayout());
			cloneFrame.setSize(400, 200);
			Panel new_pan = new NeoPanel();
			new_pan.setLayout(new BorderLayout());
			new_pan.add("Center", (Component) oneClone);
			cloneFrame.add("Center", widg_pan);
			cloneFrame.setVisible(true);//cloneFrame.show();
		}
	}

	/**
	 * Testing external zoom controls
	 */
	public void zoomFrameSetup() {
		zoomframe.setBackground(Color.white);
		zoomframe.setLayout(new BorderLayout());
		zoomframe.add("South", (JScrollBar) xzoomer);
		zoomframe.add("North", (JScrollBar) yzoomer);
		zoomframe.pack();
		zoomframe.setSize(200, zoomframe.getSize().height);
	}

	public void setTraceColors(Color[] colors) {
		widget.setTraceColors(colors);
	}

	public void setBasesBackground(Color col) {
		widget.setBackground(NeoTracer.BASES, col);
	}

	public void setTracesBackground(Color col) {
		widget.setBackground(NeoTracer.TRACES, col);
	}

	public void clipTrace(int theFirstBase, int theLastBase) {
		System.err.println("clipping from base " + theFirstBase + " to " + theLastBase);
		BaseCalls bc = widget.getActiveBaseCalls();
		Range trace_range = widget.baseRange2TraceRange(bc, theFirstBase, theLastBase);
		int theFirstPeak = trace_range.beg;
		int theLastPeak = trace_range.end;
		System.out.println(theFirstPeak + " " + theLastPeak);
		widget.setRange(theFirstPeak, theLastPeak);
		widget.stretchToFit(true, true);
		widget.updateWidget();
	}

	public void rangeChanged(NeoRangeEvent evt) {
	}

	@Override
	public URL getCodeBase() {
		if (isApplication) {
			return this.getClass().getResource("/");
		}
		return super.getCodeBase();
	}

	@Override
	public AppletContext getAppletContext() {
		if(isApplication)
		return null;
		return super.getAppletContext();
	}

	@Override
	public void showStatus(String msg) {
		if (this.getAppletContext() != null) {
			getAppletContext().showStatus(msg);
		} else {
			System.out.println("Reached end, sequence not found");
		}
	}

	@Override
	public URL getDocumentBase() {
		if (isApplication) {
			return getCodeBase();
		}
		return super.getDocumentBase();
	}

	@Override
	public String getParameter(String name) {
		if (isApplication) {
			return parameters.get(name);
		}
		return super.getParameter(name);
	}

	public void componentHidden(ComponentEvent e) {
	}

	public void componentMoved(ComponentEvent e) {
	}

	public void componentResized(ComponentEvent e) {
//	if (e.getSource() == canvas) {
		widget.stretchToFit(true, true);
		widget.updateWidget();
//				}
	}

	public void componentShown(ComponentEvent e) {
	}
	static Boolean isApplication = false;
	static Hashtable<String, String> parameters;

	static public void main(String[] args) {
		isApplication = true;
		NeoTracerDemo me = new NeoTracerDemo();
		parameters = new Hashtable<String, String>();
		parameters.put("abi_file", "data/traceTest/trace.abi");
		parameters.put("phd", "data/traceTest/trace.phd");
		me.init();
		me.start();
		JFrame frm = new JFrame("Genoviz NeoTracer Demo");
		frm.getContentPane().add("Center", me);
		frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frm.pack();
		//frm.setBounds(20, 40, 900, 400);
		frm.setVisible(true);
	}
}
