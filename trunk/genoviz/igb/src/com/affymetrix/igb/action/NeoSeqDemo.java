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
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

public class NeoSeqDemo extends Applet
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
//		System.out.println(" " + seq + "  " + seq.length() + " " + spans.length);
//		String[] seqArray = new String[spans.length];
//		String[] intronArray = new String[spans.length - 1];
		seqArray[0] = seq.substring(0, spans[0].getLength());
		if (spans.length > 1) {
			if (spans[0].getStart() < spans[0].getEnd()) {
				intronArray[0] = seq.substring(spans[0].getLength() + 1, spans[i].getStart() - spans[0].getStart() - 1);
			} else {
				intronArray[0] = seq.substring(spans[0].getLength() + 1, Math.abs(spans[i].getStart() - spans[0].getStart() + 1));
			}
		}
		System.out.println(0 + "  length " + spans[0].getLength() + " " + 0 + " " + spans[0].getLength());
		while (i < spans.length) {
			if (spans[i].getStart() < spans[i].getEnd()) {
//			System.out.println(i + "  length " +spans[i].getLength() +" "+ (spans[i].getStart()-spans[0].getStart()) +" "+ (spans[i].getStart()-spans[0].getStart()+spans[i].getLength()));
//				System.out.println("Extron " + i + "  length " + spans[i].getLength() + " " + (Math.abs(spans[i].getStart() - spans[0].getStart())) + " " + (Math.abs(spans[i].getStart() - spans[0].getStart() + spans[i].getLength())));

				seqArray[i] = seq.substring(Math.abs(spans[i].getStart() - spans[0].getStart()), Math.abs(spans[i].getStart() - spans[0].getStart() + spans[i].getLength()));
				if (i < spans.length - 1) {
					intronArray[i] = seq.substring(spans[i].getEnd() - spans[0].getStart() + 1, spans[i + 1].getStart() - spans[0].getStart() - 1);
//					System.out.println("Intron" + i + "  length " + spans[i].getLength() + " " + (spans[i].getEnd() - spans[0].getStart() + 1) + " " + (spans[i + 1].getStart() - spans[0].getStart() - 1));
				}
			} else {
//				System.out.println(i + "  length " + spans[i].getLength() + " " + (Math.abs(spans[i].getStart() - spans[0].getStart())) + " " + (Math.abs(spans[i].getStart() - spans[0].getStart() - spans[i].getLength())));
				seqArray[i] = seq.substring(Math.abs(spans[i].getStart() - spans[0].getStart()), Math.abs(spans[i].getStart() - spans[0].getStart() - spans[i].getLength()));
				if (i < spans.length - 1) {
					intronArray[i] = seq.substring(Math.abs(spans[i].getEnd() - spans[0].getStart() - 1), Math.abs(spans[i + 1].getStart() - spans[0].getStart() + 1));
				}
			}
			i++;
//		System.out.println(i);
		}

		i = 0;

//		while (spans.length > i) {
//			System.out.println(seqArray[i] + " length " + seqArray[i].length());
//			if (i < spans.length - 1) {
//				System.out.println(intronArray[i] + " length " + intronArray[i].length());
//			}
//			i++;
//		}
		return;
	}

	protected void getGoing(SeqSpan[] spans1) {

		going = true;

		seqview.enableDragScrolling(true);

		annotations = new Vector<GlyphI>();

		String[] seqArray = new String[spans1.length];
		String[] intronArray = new String[spans1.length - 1];

		// need a Sequence data model since using FastaSequenceParser,
		// which parses from fasta file into a Sequence
		if (false) {
			seq = seqmodel.getResidues();
			seqview.setSequence(seqmodel);
		} else {
//			seq = fake_seq;
			seq = getClipboard();
//			System.out.println(spans1[0] + "" + seq);
			customFormatting(seqArray, intronArray, spans1, seq);
//			seqview.setResidues(seq);
			int count =0;
			for(int j=0,k=0,l=0;j<(2*spans1.length)-1;j++){
				if((j%2) == 0){
					seqview.appendResidues(seqArray[k]);
					seqview.addTextColorAnnotation(count, (count+seqArray[k].length())-1, Color.lightGray);
//					System.out.println(seqArray[k]);
//					System.out.println(seqview.toString().substring(count, (count+seqArray[k].length())-1));
//					System.out.println("extron "+seqArray[k].length());
					count += seqArray[k].length();k++;
					
				}
				else{
					seqview.appendResidues(intronArray[l]);
					seqview.addTextColorAnnotation(count, (count+intronArray[l].length())-1, Color.darkGray);
//					System.out.println(intronArray[l]);
//					System.out.println(seqview.toString().substring(count, (count+intronArray[l].length())-1));
//					System.out.println("intron" +intronArray[l].length());
					count += intronArray[l].length();l++;
					
				}
				System.out.println("count"+count+ "j "+j+"k "+k+"l "+l+" spans length"+spans1.length+"extron length "+seqArray.length+"intron length "+intronArray.length);
			}
		}

		seqview.setShow(NeoSeq.COMPLEMENT, showComp);

		// Set the color scheme.
		Color[] okayColors = {
			Color.black,
			Color.black,};
		seqview.setStripeColors(okayColors);
		seqview.setFont(new Font("Courier", Font.BOLD, 14));
//		seqview.setResidueFontColor(Color.yellow);
		seqview.setNumberFontColor(Color.black);
		seqview.setSpacing(20);
//		for(int j=0;j<spans1.length;j++){
////			seqview.addTextColorAnnotation(spans1[j].getStart(), spans1[j].getEnd(), Color.blue);
//			seqview.addAnnotation(spans1[j].getStart(), spans1[j].getEnd(), Color.blue);
//			System.out.println("j"+j+"spans length"+spans1.length+"spans start"+spans1[j].getStart());
//			if((j%2)==0){
//
//
//			}
//		}
//		seqview1.setResidues(seq);
//		seqview1.setResidueFontColor(Color.red);
//		seqview.appendResidues(seq)

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
		NeoPanel widg_pan = new NeoPanel();
		widg_pan.setLayout(new BorderLayout());
		widg_pan.add("Center", seqview);


		mapframe.setLayout(new BorderLayout());
		mapframe.add("Center", widg_pan);

		// get the size NeoSeq prefers to be in order to display
		//    15 lines at 50 bases per line
		//    Dimension prefsize = seqview.getPreferredSize(50, 15);
		//    seqview.resize(prefsize.width, prefsize.height);
		//    mapframe.pack();  pack and menus don't mix well on some platforms...
		mapframe.setSize(pixel_width, pixel_height);
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

	private void showEnds(String theSequence) {
		String message;
//		System.out.println(theSequence.length());
		if (40 < theSequence.length()) {
			message = theSequence.substring(0, 20) + " ... "
					+ theSequence.substring(theSequence.length() - 20);
		} else {
			message = theSequence;
		}
		try {
			showStatus(message);
		} catch (Exception e) {
//			System.out.println(message);
		}
	}

	public void addOutlineAnnotation() {
		if (seqview.residuesSelected()) {
			GlyphI annot = seqview.addOutlineAnnotation(seqview.getSelectedStart(),
					seqview.getSelectedEnd(),
					out_annot_color);
			annotations.addElement(annot);
			seqview.deselect();
			seqview.updateWidget();
		}
	}

	public void addBackgroundAnnotation() {
		if (seqview.residuesSelected()) {
			GlyphI annot = seqview.addAnnotation(seqview.getSelectedStart(),
					seqview.getSelectedEnd(),
					back_annot_color);
			annotations.addElement(annot);
			seqview.deselect();
			seqview.updateWidget();
		}
	}

	private void addTextAnnotation() {
		if (seqview.residuesSelected()) {
			GlyphI annot =
					seqview.addTextColorAnnotation(seqview.getSelectedStart(),
					seqview.getSelectedEnd(),
					text_annot_color);
			annotations.addElement(annot);
			Iterator<GlyphI> it1 = (seqview.getAnnotationItems(seqview.getSequence().getStart(), seqview.getSequence().getLength())).iterator();
//			System.out.println(annot);
			Iterator<GlyphI> it = annotations.iterator();
			GlyphI g;
//			while(it1.hasNext()){
//				int i=0;
//				g=it1.next();
//				System.out.println("i "+ i+" "+g.getCoordBox()+" color"+g.getColor());
//				i++;
//			}
			printSelectionOverlappers();
			seqview.deselect();
			seqview.updateWidget();
//			Iterator<GlyphI> e = (seqview.getAnnotationItems(0, 2988)).iterator();
//			g=it.next();
//			while(e.hasNext()){
//			System.out.println(seqview.getAnnotationRange(g)+" "+g.getColor());
//			}
			
		}
	}

	private void removeAnnotation() {
		if (annotations.size() > 0) {
			GlyphI annot = annotations.lastElement();
			seqview.removeAnnotation(annot);
			annotations.removeElement(annot);
			seqview.updateWidget();
		}
	}

	public void selectAnnotation() {
		if (annotations.size() > 0) {
			GlyphI annot = annotations.lastElement();
			seqview.deselect(seqview.getSelected());
			seqview.select(annot);
			seqview.updateWidget();
		}
	}

	public void toFrontSelectionOverlappers() {
		List<GlyphI> overlappers =
				seqview.getAnnotationItems(seqview.getSelectedStart(),
				seqview.getSelectedEnd());
		for (GlyphI gl : overlappers) {
			seqview.toFrontOfSiblings(gl);
		}
		seqview.updateWidget();
	}

	public void printSelectionOverlappers() {
		List<GlyphI> overlappers = seqview.getAnnotationItems(0,
				seq.length()-1);
		System.out.println("start "+seqview.getSelectedStart()+" end "+(seqview.toString().length()-1));
//				seqview.getAnnotationItems(seqview.getSelectedStart(),
//				seqview.getSelectedEnd());
		AnnotationGlyph gl;
		if (overlappers.size() > 0) {
			System.out.print("Annotations overlapping selection");
			for (int i = 0; i < overlappers.size(); i++) {
				gl = (AnnotationGlyph) overlappers.get(i);
				System.out.print("   from " + gl.getStart() + " to " + gl.getEnd());
			}
		} else {
			System.out.println("No annotations overlapping selection");
		}
		seqview.updateWidget();
	}

	public void setAnnotColors(Color col) {

		text_annot_color = col;
		back_annot_color = col;
		out_annot_color = col;
	}

	public void testSelectedVisibility() {
		List<GlyphI> overlappers =
				seqview.getAnnotationItems(seqview.getSelectedStart(),
				seqview.getSelectedEnd());
		if (overlappers.size() <= 0) {
			System.out.println("Must highlight over an annotation");
			return;
		} else if (overlappers.size() > 1) {
			System.out.println("Must highlight over only one annotation");
			return;
		}

		GlyphI gl = overlappers.get(0);
		if (seqview.isUnObscured(gl)) {
			System.out.println("selected glyph is fully visible");
		} else if (seqview.isFullyWithinView(gl)) {
			System.out.println("selected glyph is fully within view, but "
					+ "at least partially obscured by other glyphs");
		} else if (seqview.isPartiallyWithinView(gl)) {
			System.out.println("selected glyph only partially within view, may "
					+ "also be obscured by other glyphs");
		} else {
			System.out.println("selected glyph is not within view");
		}
	}

	/**
	 * loads a sequence from over the network.
	 *
	 * @param theRef is a URL pointing to a FASTA file containing the sequence.
	 */
	public Sequence loadSequence(String theRef) {
		Sequence model = null;
		try {
			URL seq_URL = new URL(this.getDocumentBase(), theRef);
			System.out.println("getting sequence from " + seq_URL);
			FastaSequenceParser parser = new FastaSequenceParser();
			InputStream istream = seq_URL.openStream();
			BufferedInputStream bistream = new BufferedInputStream(istream);
			model = (Sequence) parser.importContent(istream);
			if (null == model.getID() || model.getID().equals("")) {
				model.setID("NeoSeqDemo");
			}
			bistream.close();
			istream.close();
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		System.out.println("got " + model);
		return model;
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
				System.out.println(seqview.getResidueColor());
				fw.write(">" + fileName);
				fw.write('\n');
				int i;
				for (i = 0; i < r.length() - 60; i += 60) {
					fw.write(r, i, 60);
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
	public void exporthtml() {
		FileDialog fd = new FileDialog(mapframe, "Save As", FileDialog.SAVE);
		fd.setVisible(true);
		String fileName = fd.getFile();

		if (null != fileName) {
			try {
				FileWriter fw = new FileWriter(fileName);
				String r = seqview.getResidues();
				System.out.println(seqview.getResidueColor());
				fw.write("<html><head><title>Sequence Viewer</title></head><body>");


				int i;
				for (i = 0; i < r.length() - 50; i += 50) {
					fw.write(r, i, 60);
					fw.write("<br>");
				}
				if (i < r.length()) {
					fw.write(r.substring(i) + "<br></body></html>");
				}
				fw.flush();
				fw.close();
			} catch (IOException ex) {
				System.out.println(ex.getMessage());
				ex.printStackTrace();
			}
		}
	}

	@Override
	public void paint(Graphics g) {
		if (null == this.backgroundImage) {
			super.paint(g);
		} else {
			g.drawImage(this.backgroundImage, 0, 0,
					this.getSize().width, this.getSize().height, this);
		}
	}
	// MENU DECLARATIONS ******************************************************

	/* Color Menu & Items */
	Menu colorMenu = new Menu("Annotation Color");
	MenuItem blueMenuItem = new MenuItem("blue");
	MenuItem greenMenuItem = new MenuItem("green");
	MenuItem magentaMenuItem = new MenuItem("magenta");
	MenuItem whiteMenuItem = new MenuItem("white");

	/* Edit Menu */
	Menu editMenu = new Menu("Edit");
	MenuItem seqToggleMenuItem = new MenuItem("Change Sequence");
	MenuItem clearWidgetMenuItem = new MenuItem("Clear Sequence");
	MenuItem undoMenuItem = new MenuItem("Undo Annotation");
	MenuItem selectMenuItem = new MenuItem("Select Last Annotation");
	//  MenuItem annotMenuItem = new MenuItem("Add Residue Annotation");
	MenuItem fontAnnotMenuItem = new MenuItem("Add Residue Annotation");
	MenuItem backAnnotMenuItem = new MenuItem("Add Background Annotation");
	MenuItem outAnnotMenuItem = new MenuItem("Add Outline Annotation");
	// these are slight misnomers, since the annotation glyphs that
	//   are affected aren't actually selected, rather they overlap
	//   the highlight glyph
	MenuItem printAnnotMenuItem = new MenuItem("Print Selected Annots");
	MenuItem toFrontMenuItem = new MenuItem("Selected Annots to Front");
	MenuItem visTestMenuItem = new MenuItem("Test Selected Annot Visibility");
	MenuItem showSelectedEndsMenuItem = new MenuItem("Show Ends of Selection");
	MenuItem showVisibleEndsMenuItem = new MenuItem("Show Ends of Visible Portion");
	MenuItem propertiesMenuItem = new MenuItem("Properties...");

	/* Show Menu */
	Menu showMenu = new Menu("Show");
	CheckboxMenuItem compCBMenuItem = new CheckboxMenuItem("Complement");
	CheckboxMenuItem transOneCBMenuItem = new CheckboxMenuItem(" +1 Translation");
	CheckboxMenuItem transTwoCBMenuItem = new CheckboxMenuItem(" +2 Translation");
	CheckboxMenuItem transThreeCBMenuItem = new CheckboxMenuItem(" +3 Translation");
	CheckboxMenuItem transNegOneCBMenuItem = new CheckboxMenuItem(" -1 Translation");
	CheckboxMenuItem transNegTwoCBMenuItem = new CheckboxMenuItem(" -2 Translation");
	CheckboxMenuItem transNegThreeCBMenuItem = new CheckboxMenuItem(" -3 Translation");

	/* Font Menu */
	Menu fontMenu = new Menu("Font");
	CheckboxMenuItem defaultFontCBMenuItem = new CheckboxMenuItem("Default");
	CheckboxMenuItem serifFontCBMenuItem = new CheckboxMenuItem("TimesRoman");
	CheckboxMenuItem sansSerifFontCBMenuItem = new CheckboxMenuItem("Helvetica");
	CheckboxMenuItem monospacedFontCBMenuItem = new CheckboxMenuItem("Courier");
	CheckboxMenuItem dialogFontCBMenuItem = new CheckboxMenuItem("Dialog");
	CheckboxMenuItem dialogInputFontCBMenuItem = new CheckboxMenuItem("DialogInput");

	/* FontSizes Menu */
	Menu fontSizesMenu = new Menu("Font Size");
	CheckboxMenuItem smallFontCBMenuItem = new CheckboxMenuItem("Small");
	CheckboxMenuItem mediumFontCBMenuItem = new CheckboxMenuItem("Medium");
	CheckboxMenuItem largeFontCBMenuItem = new CheckboxMenuItem("Large");

	/* Stripes Menu */
	Menu stripesMenu = new Menu("Stripes");
	CheckboxMenuItem vertStripesCBMenuItem = new CheckboxMenuItem("Veritcal");
	CheckboxMenuItem horizStripesCBMenuItem = new CheckboxMenuItem("Horizontal");
	CheckboxMenuItem noStripesCBMenuItem = new CheckboxMenuItem("None");

	/* StripeSizes Menu */
	Menu stripeSizesMenu = new Menu("Stripe Size");
	CheckboxMenuItem smallStripesCBMenuItem = new CheckboxMenuItem("Small");
	CheckboxMenuItem mediumStripesCBMenuItem = new CheckboxMenuItem("Medium");
	CheckboxMenuItem largeStripesCBMenuItem = new CheckboxMenuItem("Large");

	/* LineSpacing Menu */
	Menu lineSpacingMenu = new Menu("Line Spacing");
	CheckboxMenuItem smallLineSpacingCBMenuItem = new CheckboxMenuItem("Narrow");
	CheckboxMenuItem mediumLineSpacingCBMenuItem = new CheckboxMenuItem("Medium");
	CheckboxMenuItem largeLineSpacingCBMenuItem = new CheckboxMenuItem("Wide");

	/* Translation Menu */
	Menu transFormatMenu = new Menu("Translation");
	CheckboxMenuItem oneLetterCBMenuItem = new CheckboxMenuItem("One Letter");
	CheckboxMenuItem threeLetterCBMenuItem = new CheckboxMenuItem("Three Letter");

	/* File Menu */
	Menu fileMenu = new Menu("File");
	MenuItem saveAsMenuItem = new MenuItem("save As",
			new MenuShortcut(KeyEvent.VK_A));
	MenuItem exitMenuItem = new MenuItem("eXit",
			new MenuShortcut(KeyEvent.VK_X));

	public void setupMenus(Frame dock) {

		/* Color Menu */

		colorMenu.add(blueMenuItem);
		colorMenu.add(greenMenuItem);
		colorMenu.add(magentaMenuItem);
		colorMenu.add(whiteMenuItem);

		blueMenuItem.addActionListener(this);
		greenMenuItem.addActionListener(this);
		magentaMenuItem.addActionListener(this);
		whiteMenuItem.addActionListener(this);

		/* Edit Menu */

		editMenu.add(seqToggleMenuItem);
		editMenu.add(clearWidgetMenuItem);
		editMenu.addSeparator();
		editMenu.add(undoMenuItem);
		editMenu.add(selectMenuItem);
		editMenu.add(backAnnotMenuItem);
		editMenu.add(fontAnnotMenuItem);
		editMenu.add(outAnnotMenuItem);
		editMenu.add(colorMenu);
		editMenu.addSeparator();
		editMenu.add(showSelectedEndsMenuItem);
		editMenu.add(showVisibleEndsMenuItem);
		editMenu.addSeparator();
		editMenu.add(propertiesMenuItem);

		seqToggleMenuItem.addActionListener(this);
		clearWidgetMenuItem.addActionListener(this);
		undoMenuItem.addActionListener(this);
		selectMenuItem.addActionListener(this);
		backAnnotMenuItem.addActionListener(this);
		fontAnnotMenuItem.addActionListener(this);
		outAnnotMenuItem.addActionListener(this);
		showSelectedEndsMenuItem.addActionListener(this);
		showVisibleEndsMenuItem.addActionListener(this);
		propertiesMenuItem.addActionListener(this);


		/* Show Menu */

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

		/* Font Menu */


		Font f = defaultFontCBMenuItem.getFont();
		if (null != f) {
			String fontName = f.getName();
			if (null == fontName) {
				fontName = "";
			}
			int fontSize = defaultFontCBMenuItem.getFont().getSize();
			f = new Font(fontName, Font.ITALIC, fontSize);
			defaultFontCBMenuItem.setFont(f);
		}

		fontMenu.add(defaultFontCBMenuItem); // This is the Java default font.
		fontMenu.add(monospacedFontCBMenuItem); // This is the NeoSeq default font.
		fontMenu.add(dialogFontCBMenuItem);
		fontMenu.add(dialogInputFontCBMenuItem);
		fontMenu.add(sansSerifFontCBMenuItem);
		fontMenu.add(serifFontCBMenuItem);

		defaultFontCBMenuItem.addItemListener(this);
		monospacedFontCBMenuItem.addItemListener(this);
		dialogFontCBMenuItem.addItemListener(this);
		dialogInputFontCBMenuItem.addItemListener(this);
		sansSerifFontCBMenuItem.addItemListener(this);
		serifFontCBMenuItem.addItemListener(this);

		setMenuItemState(fontMenu, defaultFontCBMenuItem);

		/* FontSizes Menu */

		fontSizesMenu.add(smallFontCBMenuItem);
		fontSizesMenu.add(mediumFontCBMenuItem);
		fontSizesMenu.add(largeFontCBMenuItem);

		smallFontCBMenuItem.addItemListener(this);
		mediumFontCBMenuItem.addItemListener(this);
		largeFontCBMenuItem.addItemListener(this);

		setMenuItemState(fontSizesMenu, mediumFontCBMenuItem);

		/* Stripes Menu */

		stripesMenu.add(vertStripesCBMenuItem);
		stripesMenu.add(horizStripesCBMenuItem);
		stripesMenu.add(noStripesCBMenuItem);

		vertStripesCBMenuItem.addItemListener(this);
		horizStripesCBMenuItem.addItemListener(this);
		noStripesCBMenuItem.addItemListener(this);

		setMenuItemState(stripesMenu, vertStripesCBMenuItem);

		/* StripeSizes Menu */

		stripeSizesMenu.add(smallStripesCBMenuItem);
		stripeSizesMenu.add(mediumStripesCBMenuItem);
		stripeSizesMenu.add(largeStripesCBMenuItem);

		smallStripesCBMenuItem.addItemListener(this);
		mediumStripesCBMenuItem.addItemListener(this);
		largeStripesCBMenuItem.addItemListener(this);

		setMenuItemState(stripeSizesMenu, mediumStripesCBMenuItem);

		/* Line Spacing Menu */

		lineSpacingMenu.add(smallLineSpacingCBMenuItem);
		lineSpacingMenu.add(mediumLineSpacingCBMenuItem);
		lineSpacingMenu.add(largeLineSpacingCBMenuItem);

		smallLineSpacingCBMenuItem.addItemListener(this);
		mediumLineSpacingCBMenuItem.addItemListener(this);
		largeLineSpacingCBMenuItem.addItemListener(this);

		setMenuItemState(lineSpacingMenu, largeLineSpacingCBMenuItem);

		/* Translation Menu */

		transFormatMenu.add(oneLetterCBMenuItem);
		transFormatMenu.add(threeLetterCBMenuItem);

		oneLetterCBMenuItem.addItemListener(this);
		threeLetterCBMenuItem.addItemListener(this);

		/* Format Menu */

		Menu formatMenu = new Menu("Format");
		formatMenu.add(transFormatMenu);
		formatMenu.add(fontMenu);
		formatMenu.add(fontSizesMenu);
		formatMenu.addSeparator();
		formatMenu.add(stripesMenu);
		formatMenu.add(stripeSizesMenu);
		formatMenu.addSeparator();
		formatMenu.add(lineSpacingMenu);

		// file menu
		fileMenu.add(saveAsMenuItem);
		fileMenu.add(exitMenuItem);
		saveAsMenuItem.addActionListener(this);
		exitMenuItem.addActionListener(this);

		// add the menus to the menubar
		MenuBar bar = dock.getMenuBar();
		if (null == bar) {
			bar = new MenuBar();
			dock.setMenuBar(bar);
		}

		bar.add(fileMenu);
		bar.add(showMenu);
		bar.add(editMenu);
		bar.add(formatMenu);

	}

	// Done setting up menus ************************************************
	public void changeSequence() {
		use_real_seq = !use_real_seq;
		if (use_real_seq) {
			seq = seqmodel.getResidues();
		} else {
			seq = fake_seq;
		}
		seqview.clearAnnotations();
		seqview.setResidues(seq);
		seqview.updateWidget();
	}


	/* EVENT HANDLING */
	/** ActionListener Implementation */
	public void actionPerformed(ActionEvent e) {
		Object theItem = e.getSource();
		if (theItem == backAnnotMenuItem) {
			addBackgroundAnnotation();
		} else if (theItem == fontAnnotMenuItem) {
			addTextAnnotation();
		} else if (theItem == outAnnotMenuItem) {
			addOutlineAnnotation();
		} else if (theItem == undoMenuItem) {
			removeAnnotation();
		} else if (theItem == selectMenuItem) {
			selectAnnotation();
		} else if (theItem == seqToggleMenuItem) {
			changeSequence();
		} else if (theItem == clearWidgetMenuItem) {
			seqview.clearWidget();
			seqview.updateWidget();
		} else if (theItem == showVisibleEndsMenuItem) {
			String str = seqview.getVisibleResidues();
			showEnds(str);
		} else if (theItem == propertiesMenuItem) {
			if (null == propframe) {
				propframe = new Frame("NeoSeq Properties");
				customizer = new NeoSeqCustomizer();
				customizer.setObject(this.seqview);
				propframe.add("Center", customizer);
				propframe.pack();
				propframe.addWindowListener(this);
			}
			propframe.setBounds(200, 200, 500, 300);
			propframe.setVisible(true);
		} else if (theItem == showSelectedEndsMenuItem) {
			String str = seqview.getSelectedResidues();
			showEnds(str);
		} else if (theItem == blueMenuItem) {
			setAnnotColors(Color.blue);
		} else if (theItem == greenMenuItem) {
			setAnnotColors(Color.green);
		} else if (theItem == magentaMenuItem) {
			setAnnotColors(Color.magenta);
		} else if (theItem == whiteMenuItem) {
			setAnnotColors(Color.white);
		} else if (theItem == printAnnotMenuItem) {
			printSelectionOverlappers();
		} else if (theItem == toFrontMenuItem) {
			toFrontSelectionOverlappers();
		} else if (theItem == visTestMenuItem) {
			testSelectedVisibility();
		} else if (theItem == saveAsMenuItem) {
			exporthtml();
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

		if (theItem == oneLetterCBMenuItem) {
			seqview.setTranslationStyle(NeoSeq.ONE_LETTER_CODE);
			setMenuItemState(transFormatMenu, (CheckboxMenuItem) theItem);
			seqview.updateWidget();
		} else if (theItem == threeLetterCBMenuItem) {
			seqview.setTranslationStyle(NeoSeq.THREE_LETTER_CODE);
			setMenuItemState(transFormatMenu, (CheckboxMenuItem) theItem);
			seqview.updateWidget();
		} else if (theItem == compCBMenuItem) {
			CheckboxMenuItem mi = (CheckboxMenuItem) theItem;
			boolean showComp = mi.getState();
			seqview.setShow(NeoSeq.COMPLEMENT, showComp);
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
		} else if (theItem == dialogFontCBMenuItem
				|| theItem == dialogInputFontCBMenuItem
				|| theItem == monospacedFontCBMenuItem
				|| theItem == serifFontCBMenuItem
				|| theItem == sansSerifFontCBMenuItem) {
			seqview.setFontName(((MenuItem) theItem).getLabel());
			setMenuItemState(fontMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == defaultFontCBMenuItem) {
			seqview.setFontName(""); // NeoSeq will use the Java default font.
			seqview.setFontName("Courier"); // This is the NeoSeq default font.
			setMenuItemState(fontMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == smallFontCBMenuItem) {
			seqview.setFontSize(12);
			setMenuItemState(fontSizesMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == mediumFontCBMenuItem) {
			seqview.setFontSize(14);
			setMenuItemState(fontSizesMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == largeFontCBMenuItem) {
			seqview.setFontSize(16);
			setMenuItemState(fontSizesMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == vertStripesCBMenuItem) {
			seqview.setStripeOrientation(NeoSeq.VERTICAL_STRIPES);
			setMenuItemState(stripesMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == horizStripesCBMenuItem) {
			seqview.setStripeOrientation(NeoSeq.HORIZONTAL_STRIPES);
			setMenuItemState(stripesMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == noStripesCBMenuItem) {
			seqview.setStripeOrientation(NeoSeq.NO_STRIPES);
			setMenuItemState(stripesMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == smallStripesCBMenuItem) {
			seqview.setStripeWidth(5);
			setMenuItemState(stripeSizesMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == mediumStripesCBMenuItem) {
			seqview.setStripeWidth(10);
			setMenuItemState(stripeSizesMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == largeStripesCBMenuItem) {
			seqview.setStripeWidth(20);
			setMenuItemState(stripeSizesMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == smallLineSpacingCBMenuItem) {
			seqview.setSpacing(0);
			setMenuItemState(lineSpacingMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == mediumLineSpacingCBMenuItem) {
			seqview.setSpacing(10);
			setMenuItemState(lineSpacingMenu, (CheckboxMenuItem) theItem);
		} else if (theItem == largeLineSpacingCBMenuItem) {
			seqview.setSpacing(20);
			setMenuItemState(lineSpacingMenu, (CheckboxMenuItem) theItem);
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
		NeoSeqDemo me = new NeoSeqDemo();
		parameters = new Hashtable<String, String>();
		parameters.put("seq_file", "data/test.fst");
		int i = 0;
//		this.spans=spans1;
		while (spans.length > i) {
			System.out.println("start " + spans[i].getStart() + " end   " + spans[i].getEnd());
			i++;
		}

		me.init(spans);
		me.start(spans);
//		JFrame frm = new JFrame("Genoviz NeoSeq Demo");
//		//frm.getContentPane().add("Center", me);
//		frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
//		frm.pack();
		//frm.setBounds(20, 40, 900, 400);
//		me.setVisible(true);
	}
}
