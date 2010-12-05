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
package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.SeqSpan;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import java.applet.Applet;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.lang.Math;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.datamodel.Sequence;
import com.affymetrix.genoviz.parser.FastaSequenceParser;
import com.affymetrix.genoviz.widget.NeoSeq;
import com.affymetrix.genoviz.widget.NeoSeqCustomizer;
import com.affymetrix.genoviz.widget.neoseq.AnnotationGlyph;
import java.applet.AppletContext;
import java.awt.BorderLayout;
import java.awt.CheckboxMenuItem;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.MenuShortcut;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class SequenceViewer extends Applet
		implements WindowListener, ActionListener, ItemListener {

	private NeoSeq seqview;
	private NeoSeq seqview1;
	private Frame mapframe;
	private Frame propframe;
	private Sequence seqmodel;
	private Vector<GlyphI> annotations;
	private String seq;
	// Fake Sequences for Testing
	private boolean use_real_seq = false;
	// All the Standard Nucleotide Code Letters
	private String fake_seq = "ACGT UMRWSYK VHDBXN."; // The two gaps make it 0 mod 10 long.
	private boolean showComp = false; // show complementary strand?
	private int pixel_width = 500;
	private int pixel_height = 400;
	private Color text_annot_color = Color.blue;
	private Color back_annot_color = Color.green;
	private Color out_annot_color = Color.white;
	private Image backgroundImage = null;
	private boolean clicking = false;
	private NeoSeqCustomizer customizer;
	private boolean framesShowing = true;
	private boolean going = false;
	private Color nicePaleBlue = new Color(180, 250, 250);
	NeoPanel widg_pan = new NeoPanel();

	public void init(final SeqSpan[] spans1) {

		String param;

		param = getParameter("background");
		if (null != param) {
			backgroundImage = this.getImage(this.getDocumentBase(), param);
		}

		if (null == backgroundImage) {
			Label placeholder =
					new Label("Running genoviz NeoSeq Demo", Label.CENTER);
			this.setLayout(new BorderLayout());
			this.add("Center", placeholder);
			placeholder.setBackground(nicePaleBlue);
		}

		param = getParameter("show");
		if (null != param) {
			if (param.equalsIgnoreCase("onclick")) {
				clicking = true;
				framesShowing = false;
			}
		}

		/** Using an inner class to catch mouseReleased (nee mouseUp) */
		this.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent evt) {
				if (clicking) {
					if (framesShowing) {
						hideFrames();
					} else {
						showFrames(spans1);
					}
					framesShowing = !framesShowing;
				}
			}
		});

		seqview = new NeoSeq();

		seqview.addKeyListener(new KeyAdapter() {
			// Why is this not getting called?

			@Override
			public void keyPressed(KeyEvent evt) {
				System.err.println("NeoSeqDemo saw key pressed.");
			}
		});

//		String ref = getParameter("seq_file");
//		this.use_real_seq = ( null != ref );
//		if ( this.use_real_seq ) {
//			seqmodel = loadSequence( ref );
//		}

	}

	private void customFormatting(String[] seqArray, String[] intronArray, SeqSpan[] spans, String seq) {
		int i = 1;
		seqArray[0] = seq.substring(0, spans[0].getLength());
		if (spans.length > 1) {
			if (spans[0].getStart() < spans[0].getEnd()) {
				intronArray[0] = seq.substring(spans[0].getLength(), spans[i].getStart() - spans[0].getStart());
			} else {
				intronArray[0] = seq.substring(spans[0].getLength(), Math.abs(spans[i].getStart() - spans[0].getStart()));
			}
		}
		while (i < spans.length) {
			if (spans[i].getStart() < spans[i].getEnd()) {
				seqArray[i] = seq.substring(Math.abs(spans[i].getStart() - spans[0].getStart()), Math.abs(spans[i].getEnd() - spans[0].getStart()));
				if (i < spans.length - 1) {
					intronArray[i] = seq.substring(spans[i].getEnd() - spans[0].getStart(), spans[i + 1].getStart() - spans[0].getStart());
				}
			} else {
				seqArray[i] = seq.substring(Math.abs(spans[i].getStart() - spans[0].getStart()), Math.abs(spans[i].getEnd() - spans[0].getStart()));
				if (i < spans.length - 1) {
					intronArray[i] = seq.substring(Math.abs(spans[i].getEnd() - spans[0].getStart()), Math.abs(spans[i + 1].getStart() - spans[0].getStart()));
				}
			}
			i++;
		}

		i = 0;

		return;
	}

	protected void getGoing(SeqSpan[] spans1) {

		going = true;

		seqview.enableDragScrolling(true);

		annotations = new Vector<GlyphI>();

		seq = getClipboard();
		if (null == spans1) {
			seqview.setResidues(seq);
			seqview.setResidueFontColor(Color.YELLOW);
		} else {
			String[] seqArray = new String[spans1.length];
			String[] intronArray = new String[spans1.length - 1];
			customFormatting(seqArray, intronArray, spans1, seq);
			//Below is done because NeoSeq has first character as white space
			seqview.setResidues("");
			int count = 0;
			for (int j = 0, k = 0, l = 0; j < (2 * spans1.length) - 1; j++) {
				if ((j % 2) == 0) {
					seqview.appendResidues(seqArray[k]);
					seqview.addTextColorAnnotation(count, (count + seqArray[k].length()) - 1, Color.YELLOW);
					count += seqArray[k].length();
					k++;

				} else {
					seqview.appendResidues(intronArray[l]);
					seqview.addTextColorAnnotation(count, (count + intronArray[l].length()) - 1, Color.WHITE);
					count += intronArray[l].length();
					l++;

				}
			}

		}

		seqview.setShow(NeoSeq.COMPLEMENT, showComp);

		// Set the color scheme.
		Color[] okayColors = {
			Color.black,
			Color.black,};
		seqview.setStripeColors(okayColors);
		seqview.setFont(new Font("Arial", Font.BOLD, 14));

		seqview.setNumberFontColor(Color.black);
		seqview.setSpacing(20);

		mapframe = new Frame("Sequence Viewer");
		mapframe.setLayout(new BorderLayout());
		setupMenus(mapframe);

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
		
		widg_pan.setLayout(new BorderLayout());
		widg_pan.add("Center", seqview);


		mapframe.setLayout(new BorderLayout());
		mapframe.add("Center", widg_pan);

		// get the size NeoSeq prefers to be in order to display
		//    15 lines at 50 bases per line
		//    Dimension prefsize = seqview.getPreferredSize(50, 15);
		//    seqview.resize(prefsize.width, prefsize.height);
		//    mapframe.pack();  pack and menus don't mix well on some platforms...
		Dimension prefsize = seqview.getPreferredSize(50, 15);
		mapframe.setMinimumSize(prefsize);
//		mapframe.setSize(pixel_width+230, pixel_height);
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		mapframe.setLocation((screen_size.width - pixel_width) / 2,
				(screen_size.height - pixel_height) / 2);
		mapframe.setVisible(true);
		final Applet app = this;
		mapframe.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent e) {
				if (e.getSource() == mapframe) {
					app.stop();
				} else {
					((Window) e.getSource()).setVisible(false);
				}
			}
		});
	}

	@Override
	public String getAppletInfo() {
		return ("Demonstration of genoviz Software's Sequence Viewing Widget");
	}

	private void showFrames(SeqSpan[] spans1) {
		if (!going) {
			getGoing(spans1);
		}
		mapframe.setVisible(true);
	}

	private void hideFrames() {
		if (null != this.propframe) {
			this.propframe.setVisible(false);
		}
		if (null != mapframe) {
			mapframe.setVisible(false);
		}
	}

	public void start(SeqSpan[] spans1) {
		if (framesShowing) {
			showFrames(spans1);
		}
	}

	@Override
	public void stop() {
		hideFrames();
	}

	@Override
	public void destroy() {
		if (this.mapframe != null) {
			this.mapframe.setVisible(false);
			this.mapframe.dispose();
			this.mapframe = null;
		}
		if (this.propframe != null) {
			this.propframe.setVisible(false);
			this.propframe.dispose();
			this.propframe = null;
		}
		super.destroy();
	}

	public static String getClipboard() {
		Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
		try {
			if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
				String text = (String) t.getTransferData(DataFlavor.stringFlavor);

				return text.trim();
			}
		} catch (Exception e) {
		}
		return "";
	}

	public void exportSequenceFasta() {
		FileDialog fd = new FileDialog(mapframe, "Save As", FileDialog.SAVE);
		fd.setVisible(true);
		String fileName = fd.getFile();

		if (null != fileName) {
			try {
				FileWriter fw = new FileWriter(fileName);
				String r = seqview.getResidues();
				fw.write(">" + fileName);
				fw.write('\n');
				int i;
				for (i = 0; i < r.length() - 50; i += 50) {
					fw.write(r, i, 50);
					fw.write('\n');
				}
				if (i < r.length()) {
					fw.write(r.substring(i) + '\n');
				}
				fw.flush();
				fw.close();
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	/* Edit Menu */
	Menu editMenu = new Menu("Edit");
	MenuItem copyMenuItem = new MenuItem("Copy selected sequence to clipboard",
			new MenuShortcut(KeyEvent.VK_C));

	/* File Menu */
	Menu fileMenu = new Menu("File");
	MenuItem saveAsMenuItem = new MenuItem("save As",
			new MenuShortcut(KeyEvent.VK_A));
	MenuItem exitMenuItem = new MenuItem("eXit",
			new MenuShortcut(KeyEvent.VK_X));
	Menu showMenu = new Menu("Show");
	CheckboxMenuItem compCBMenuItem = new CheckboxMenuItem("Reverse Complement");
	CheckboxMenuItem transOneCBMenuItem = new CheckboxMenuItem(" +1 Translation");
	CheckboxMenuItem transTwoCBMenuItem = new CheckboxMenuItem(" +2 Translation");
	CheckboxMenuItem transThreeCBMenuItem = new CheckboxMenuItem(" +3 Translation");
	CheckboxMenuItem transNegOneCBMenuItem = new CheckboxMenuItem(" -1 Translation");
	CheckboxMenuItem transNegTwoCBMenuItem = new CheckboxMenuItem(" -2 Translation");
	CheckboxMenuItem transNegThreeCBMenuItem = new CheckboxMenuItem(" -3 Translation");

	public void setupMenus(Frame dock) {

		/* Edit Menu */

		editMenu.add(copyMenuItem);


		copyMenuItem.addActionListener(this);

		// file menu
		fileMenu.add(saveAsMenuItem);
		fileMenu.add(exitMenuItem);
		saveAsMenuItem.addActionListener(this);
		exitMenuItem.addActionListener(this);
		showMenu.add(compCBMenuItem);
		showMenu.add(transOneCBMenuItem);
		showMenu.add(transTwoCBMenuItem);
		showMenu.add(transThreeCBMenuItem);
		showMenu.add(transNegOneCBMenuItem);
		showMenu.add(transNegTwoCBMenuItem);
		showMenu.add(transNegThreeCBMenuItem);

		compCBMenuItem.addItemListener(this);
		transOneCBMenuItem.addItemListener(this);
		transTwoCBMenuItem.addItemListener(this);
		transThreeCBMenuItem.addItemListener(this);
		transNegOneCBMenuItem.addItemListener(this);
		transNegTwoCBMenuItem.addItemListener(this);
		transNegThreeCBMenuItem.addItemListener(this);

		// add the menus to the menubar
		MenuBar bar = dock.getMenuBar();
		if (null == bar) {
			bar = new MenuBar();
			dock.setMenuBar(bar);
		}

		bar.add(fileMenu);
		bar.add(editMenu);
		bar.add(showMenu);
	}

	/* EVENT HANDLING */
	/** ActionListener Implementation */
	public void actionPerformed(ActionEvent e) {
		Object theItem = e.getSource();

		if (theItem == copyMenuItem) {
			String selectedSeq = seqview.getSelectedResidues();
			if (selectedSeq != null) {
				Clipboard clipboard = this.getToolkit().getSystemClipboard();
				StringBuffer hackbuf = new StringBuffer(selectedSeq);
				String hackstr = new String(hackbuf);
				StringSelection data = new StringSelection(hackstr);
				clipboard.setContents(data, null);
			} else {
				ErrorHandler.errorPanel("Missing Sequence Residues",
						"Don't have all the needed residues, can't copy to clipboard.\n"
						+ "Please load sequence residues for this region.");
			}

		} else if (theItem == saveAsMenuItem) {
			exportSequenceFasta();
		} else if (theItem == exitMenuItem) {
			mapframe.dispose();
			this.destroy();
		}

	}

	private void setMenuItemState(Menu theMenu, CheckboxMenuItem theItem) {
		for (int i = theMenu.getItemCount() - 1; 0 <= i; i--) {
			MenuItem item = theMenu.getItem(i);
			if (item instanceof CheckboxMenuItem) {
				((CheckboxMenuItem) item).setState(item == theItem);
			}
		}
	}

	/** ItemListener Implementation */
	public void itemStateChanged(ItemEvent e) {
		Object theItem = e.getSource();

		if (theItem == compCBMenuItem) {
			CheckboxMenuItem mi = (CheckboxMenuItem) theItem;
			boolean showRevComp = mi.getState();
			seqview.setRevShow(NeoSeq.COMPLEMENT, showRevComp);
			seqview.setRevShow(NeoSeq.NUCLEOTIDES, !showRevComp);
			seqview.updateWidget();
		} else if (theItem == transOneCBMenuItem) {
			CheckboxMenuItem mi = (CheckboxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_ONE, mi.getState());
			seqview.updateWidget();
		} else if (theItem == transTwoCBMenuItem) {
			CheckboxMenuItem mi = (CheckboxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_TWO, mi.getState());
			seqview.updateWidget();
		} else if (theItem == transThreeCBMenuItem) {
			CheckboxMenuItem mi = (CheckboxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_THREE, mi.getState());
			seqview.updateWidget();
		} else if (theItem == transNegOneCBMenuItem) {
			CheckboxMenuItem mi = (CheckboxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_NEG_ONE, mi.getState());
			seqview.updateWidget();
		} else if (theItem == transNegTwoCBMenuItem) {
			CheckboxMenuItem mi = (CheckboxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_NEG_TWO, mi.getState());
			seqview.updateWidget();
		} else if (theItem == transNegThreeCBMenuItem) {
			CheckboxMenuItem mi = (CheckboxMenuItem) theItem;
			seqview.setShow(NeoSeq.FRAME_NEG_THREE, mi.getState());
			seqview.updateWidget();
		} 

	}

	/** WindowListener Implementation */
	public void windowActivated(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowIconified(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e) {
	}

	public void windowOpened(WindowEvent e) {
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
		((Window) e.getSource()).setVisible(false);
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
		if (isApplication) {
			return null;
		}
		return super.getAppletContext();
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
	static Boolean isApplication = false;
	static Hashtable<String, String> parameters;

	public void tempChange(SeqSpan[] spans) {
		isApplication = true;
		SequenceViewer me = new SequenceViewer();
		parameters = new Hashtable<String, String>();
		parameters.put("seq_file", "data/test.fst");
		me.init(spans);
		me.start(spans);
	}


}
