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
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;
import java.net.URL;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.JScrollBar;

import com.affymetrix.genoviz.bioviews.ResiduePainter;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.datamodel.Mapping;
import com.affymetrix.genoviz.datamodel.SequenceI;
import com.affymetrix.genoviz.datamodel.Span;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.widget.NeoAssembler;
import com.affymetrix.genoviz.widget.NeoAssemblerCustomizer;
import com.affymetrix.genoviz.util.GeneralUtils;
import com.affymetrix.genoviz.util.DNAUtils;
import com.affymetrix.genoviz.glyph.AlignmentGlyph;
import com.affymetrix.genoviz.glyph.AlignedResiduesGlyph;

import genoviz.demo.datamodel.Assembly;
import genoviz.demo.parser.AlignmentParser;
import genoviz.demo.parser.SequenceParser;
import java.awt.Container;
import java.net.MalformedURLException;
import java.util.Hashtable;
import java.util.Iterator;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.WindowConstants;


/**
 *  A demo of the NeoAssembler widget.  Uses "low-level" calls to
 *  NeoAssembler to construct visual representation of assembly (instead
 *  of a data adapter, which is used in NeoAssemblerDemo2)
 */
public class NeoAssemblerDemo extends JApplet
	implements NeoRangeListener, WindowListener,
			   MouseListener, ActionListener
{
	// try prebuilding "ungapped" single alignment string
	// for each aligned sequence?
	public boolean tryUniString = false;

	// simulate trimmed edges? (only if tryUniString == false)
	public boolean simulateTrimmedEdges = true;

	// skip reverse strand alignments?
	public boolean no_reverse = false;

	NeoAssembler map;
	JScrollBar hzoom;
	Frame mapframe, propframe, zoomframe;
	//  boolean tryUniChild = false;

	Menu editMenu = new Menu("Edit");
	MenuItem propertiesMenuItem, addConsMenuItem, addNextMenuItem,
			 addAlignMenuItem, addGapsMenuItem, addBasesMenuItem, addNegMenuItem,
			 addAllAligns, internalSortItem, externalSortItem,
			 switchFontTypeItem, switchFontStyleItem, switchFontSizeItem,
			 externalColorRectItem, externalColorFontItem;

	int label_width = 120;
	int pixel_width = 500;
	int pixel_height = 200;

	boolean use_label_arrows = true;  // set to false to turn label arrows off
	boolean use_internal_zoomer = true;  // set to false to test external zoomer
	boolean show_half_first = true;  // load consensus and half of assembler first
	// set to true to use ResiduePainter for residue background rect color-coding
	boolean external_rect_coloring = false;
	// set to true to use ResiduePainter for residue character color-coding
	boolean external_font_coloring = false;

	boolean optimize_scrolling = false;
	boolean optimize_damage = false;
	boolean isTimed = false;
	boolean isMemed = false;
	boolean consensus_added = false;

	// For debugging
	//Memer memcheck;
	//Timer tm;

	// Start add sequences to the assembly view with second sequence in
	//    assembly (first is consensus);
	int currentSeq = 1;

	Object firstSeq;
	Vector selectedDataModels = new Vector();
	int negstart = -100;
	int negend = 50;
	int ref_start, ref_end;
	public ResiduePainter bg_painter = new StartCodonPainter();
	public ResiduePainter fg_painter = new ResiduePainterExample();

	// data models used by applet to represent assembly
	Assembly assem;  // the whole assembly
	Mapping consensus;  // the mapping of consensus to reference coordinates
	// (to allow for a consensus with gaps)
	//Vector aligns;  // a Vector of Mappings, one for each sequence in the alignment

	Image backgroundImage = null;
	boolean clicking = false;
	NeoAssemblerCustomizer customizer;
	boolean framesShowing = true;
	boolean going = false;
	Color nicePaleBlue = new Color(180, 250, 250);
	AlignmentGlyph consglyph;


		public void NeoAssemblerDemo() {

			String param;

			param = getParameter("background");
			if (null != param) {
				backgroundImage = this.getImage(this.getDocumentBase(), param);
			}

			if (null == backgroundImage) {
				Label placeholder =
					new Label("Running genoviz NeoAssembler Demo", Label.CENTER);
				this.setLayout(new BorderLayout());
				this.add("North", placeholder);
				placeholder.setBackground(nicePaleBlue);
			}

			param = getParameter("show");
			if (null != param) {
				if (param.equalsIgnoreCase("onclick")) {
					clicking = true;
					framesShowing = false;
				}
			}

		}

	protected void getGoing() {

		going = true;

		//if (isMemed)  { memcheck = new Memer(); }
		//if (isTimed)  { tm = new Timer(); }

		map = new NeoAssembler();
		if (!use_internal_zoomer) {
			hzoom = new JScrollBar(JScrollBar.HORIZONTAL);
			map.setZoomer(NeoAssembler.X, hzoom);
			zoomFrameSetup();
		}


		map.setBackground(nicePaleBlue);
		map.setLabelsBackground(nicePaleBlue);
		map.setDamageOptimized(optimize_damage);
		map.setAutoSort(false);
		map.setLabelWidth( 200 );
		map.setSize(840, 200);
		// Use the NeoAssembler's built-in selection methods.
		map.setSelectionEvent(NeoAssembler.ON_MOUSE_DOWN);
		//    map.setSelectionBehavior(map.SELECT_RESIDUES);
		map.setSelectionBehavior(NeoAssembler.SELECT_RESIDUES);

		assem = loadData();  // parses in data and populates assembly data model
		map.setRange(0, assem.getLength()+1);
		consensus = assem.getConsensus();
		List aligns = assem.getAlignments();

		//if (isMemed)  { memcheck.printMemory(); }

		//----------- setting up alignment map info ------------
		addConsensus();
		if (show_half_first) {
			//if (isTimed)  { tm.start(); }
			int half = aligns.size()/2;
			for (int i=1; i<2*half-1; i++) {
				addNext();
			}
			//if (isTimed)  { tm.print(); }
		}

		//if (isMemed)  { memcheck.printMemory(); }

//		mapframe = new Frame("Genoviz NeoAssembler Demo");
//		setupMenus(mapframe);
//		mapframe.setLayout(new BorderLayout());

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

		Container cpane = this.getContentPane();
		cpane.setLayout(new BorderLayout());
		cpane.add("Center", this.map);
		//cpane.add("North", xzoomer);
		//cpane.add("West", yzoomer);
		/*Panel map_pan;
		map_pan = new NeoPanel();
		map_pan.setLayout(new BorderLayout());
		map_pan.add("Center", map);*/
//		mapframe.add(map);

//		mapframe.setSize(pixel_width, pixel_height);
//		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
//		mapframe.setLocation((screen_size.width-pixel_width)/2,
//				(screen_size.height-pixel_height)/2);
//		mapframe.setVisible(true);//mapframe.show();
//
//		mapframe.addWindowListener(this);
		map.addMouseListener(this);
		map.addRangeListener(this);
		cpane.setVisible(true);
	}


	public Object addBases(Mapping align) {
		GlyphI seqtag = map.<GlyphI>getItem(align);
		map.setResidues(seqtag, align.getSequence().getResidues());
		return seqtag;
	}

	public Object addGaps(Mapping align) {
		// this assumes only one glyph returned by NeoAssembler.getItems()!
		GlyphI seqtag = map.<GlyphI>getItem(align);
		List<Span> spans = align.getSpans();
		Span sp;
		for (int j=0; j<spans.size(); j++) {
			sp = spans.get(j);
			// simulating unaligned edges
			// by declaring the first and last spans unaligned
			if (simulateTrimmedEdges && ((j==0) || (j == spans.size()-1))) {
				map.addUnalignedSpan(seqtag,
						sp.seq_start, sp.seq_end,
						sp.ref_start, sp.ref_end);
			}
			else  {
				map.addAlignedSpan(seqtag, sp.seq_start, sp.seq_end,
						sp.ref_start, sp.ref_end);
			}
		}
		return seqtag;
	}

	/**
	 *  Trying to get improved speed and memory usage.
	 *  Takes the aligned sequence and its mapping to the reference and
	 *  builds a single "ungapped" alignment string, with spaces substituted
	 *  for any gaps.  This is then added to the NeoAssembler as a sequence
	 *  with a single span.
	 */
	public GlyphI addUniAlignment(Mapping align) {
		AlignmentGlyph ag = (AlignmentGlyph)addAlign(align);
		List spans = align.getSpans();
		Span sp;
		int align_start, align_end, align_length;

		align_start = ((Span)spans.get(0)).ref_start;
		align_end = ((Span)spans.get(spans.size()-1)).ref_end;
		align_length = Math.abs(align_end-align_start)+1;

		String seq_string = align.getSequence().getResidues();
		StringBuffer sb = new StringBuffer(align_length);
		for (int i=0; i<align_length; i++) {
			sb.append(' ');
		}
		int align_position;

		for (int j=0; j<spans.size(); j++) {
			sp = (Span)spans.get(j);
			if (align.isForward()) {
				align_position = sp.ref_start - align_start;
				for (int seq_position=sp.seq_start; seq_position<=sp.seq_end;
						seq_position++) {
					// testing to avoid ArrayIndexOutOfBounds exceptions
					//   for sequences with bad alignment data
					if (seq_position < seq_string.length()) {
						sb.setCharAt(align_position, seq_string.charAt(seq_position));
					}
					align_position++;
						}
			}
			else {
				align_position = sp.ref_start - align_start;
				for (int seq_position=sp.seq_start; seq_position>=sp.seq_end;
						seq_position--) {
					// testing to avoid ArrayIndexOutOfBounds exceptions
					//   for sequences with bad alignment data
					if (seq_position < seq_string.length()) {
						sb.setCharAt(align_position, seq_string.charAt(seq_position));
					}
					align_position++;
						}
			}
		}
		AlignedResiduesGlyph arg;
		String str = sb.toString();
		if (align.isForward()) {
			arg = (AlignedResiduesGlyph)map.addAlignedSpan(ag, 0, sb.length()-1,
					align_start, align_end);
			ag.setResidues(str);
		}
		else {
			arg = (AlignedResiduesGlyph)map.addAlignedSpan(ag, sb.length()-1, 0,
					align_start, align_end);
			ag.setResidues(DNAUtils.reverse(str));
		}
		return ag;
	}

	public Object addAlign(Mapping align) {
		String name;
		List spans;
		Span sp;
		int start, end;
		GlyphI seqtag, labeltag;

		spans = align.getSpans();
		start = ((Span)spans.get(0)).ref_start;
		end = ((Span)spans.get(spans.size()-1)).ref_end;
		if (!align.isForward()) {
			int temp = start;
			start = end;
			end = temp;
		}
		name = align.getSequence().getName();
		seqtag = map.addSequence(start, end);

		// color coding particular alignments (based on source, for example)
		if (name.startsWith("GEO")) {
			map.setLabelColor(Color.blue);
			seqtag.setColor(Color.blue);
		}
		else {
			map.setLabelColor(Color.black);
		}
		labeltag = map.setLabel(seqtag, name);

		map.setDataModel(seqtag, align);
		return seqtag;
	}


	public void showProperties() {
		if (null == propframe) {
			propframe = new Frame("NeoAssembler Properties");
			customizer = new NeoAssemblerCustomizer();
			customizer.setObject(this.map);
			propframe.add("Center", customizer);
			propframe.pack();
			propframe.addWindowListener(this);
		}
		propframe.setBounds(200, 200, 500, 300);
		propframe.setVisible(true);//propframe.show();
	}

	// Disable Add Alignment menus.  Done when all of the alignments have been added.
	public void turnOffAddAlignMenus() {
		addAllAligns.setEnabled(false); // Only do this once.
		addNextMenuItem.setEnabled(false); // Only do this once.
		addAlignMenuItem.setEnabled(false); // Only do this once.
	}

	public void addAlignment() {
		List aligns;
		aligns = assem.getAlignments();
		if (currentSeq >= aligns.size()) {
			showStatus("No more sequences to add.");
		}
		else {
			addAlign((Mapping)aligns.get(currentSeq));
			currentSeq++;
			map.updateWidget();
			if (currentSeq == aligns.size()) {
				turnOffAddAlignMenus();
			}
		}
	}

	public void removeSeq() {
		if (selectedDataModels.size() < 1) {
			showStatus("Can't remove sequence. No sequence selected.");
			return;
		}
		Enumeration enm = selectedDataModels.elements();
		while (enm.hasMoreElements()) {
			Mapping align = (Mapping)enm.nextElement();
			List<GlyphI> seq_glyphs = map.<GlyphI>getItems(align);
			map.deselect(seq_glyphs);
			map.removeItem(seq_glyphs);
		}
		map.pack();
		map.updateWidget();
	}

	public void addGaps() {
		Enumeration enm = selectedDataModels.elements();
		while (enm.hasMoreElements()) {
			Mapping align = (Mapping)enm.nextElement();
			addGaps(align);
			// need to make sure gapped sequences are selected too --
			// should probably push this down into glyph's addChild method...
			map.select(map.getItems(align));
			map.updateWidget();
		}
		if (selectedDataModels.size() < 1) {
			showStatus("Can't add gaps. No alignment selected.");
		}
	}

	public void addBases() {
		Enumeration enm = selectedDataModels.elements();
		while (enm.hasMoreElements()) {
			Mapping align = (Mapping)enm.nextElement();
			addBases(align);
			// need to make sure gapped sequences are selected too --
			// should probably push this down into glyph's addChild method...
			map.select(map.getItems(align));
			map.updateWidget();
		}
		if (selectedDataModels.size() < 1) {
			showStatus("Can't add bases. No alignment selected.");
		}
	}

	public void addNext() {
		List aligns = assem.getAlignments();
		if (currentSeq >= aligns.size()) {
			System.err.println("No more sequences to add.");
			showStatus("No more sequences to add.");
		}
		else {
			Mapping align = (Mapping)aligns.get(currentSeq);
			if (no_reverse) {
				if (!align.isForward()) {
					currentSeq++;
					if (currentSeq == aligns.size()) {
						turnOffAddAlignMenus();
					}
					return;
				}
			}
			if (tryUniString) {
				addUniAlignment(align);
			}
			else {
				addAlign(align);
				addBases(align);
				addGaps(align);
			}
			currentSeq++;
			map.updateWidget();
			if (currentSeq == aligns.size()) {
				turnOffAddAlignMenus();
			}
		}
	}

	public void addTheRest() {
		//System.err.println("adding all sequences");
		List aligns = assem.getAlignments();
		while (currentSeq < aligns.size()) {
			addNext();
		}
		map.updateWidget();
	}

	public void addNeg() {
		map.addSequence(negstart, negend);
		negstart -= 100;
		map.updateWidget();
	}


	public Assembly loadData() {
		URL seq_URL = null, align_URL = null;
		try {
			seq_URL = new URL(this.getDocumentBase(), getParameter("seq_file"));
			System.out.println(seq_URL);
			align_URL = new URL(this.getDocumentBase(), getParameter("map_file"));
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		return loadData(seq_URL, align_URL);
	}

	public Assembly loadData(URL seq_URL, URL align_URL) {
		Vector seqs = null;
		Vector aligns = null;
		//Hashtable seqhash = new Hashtable();
		seqs = SequenceParser.getSequences(seq_URL);
		aligns = AlignmentParser.getAlignments(align_URL);
		System.out.println("seq_URL" +  seq_URL);
		Mapping consmap = (Mapping)aligns.elementAt(0);
		Assembly model = new Assembly(consmap, aligns, seqs);
		return model;
	}

	public void addConsensus() {
		if (consensus_added) { return; }
		List spans = consensus.getSpans();
		int start = ((Span)spans.get(0)).ref_start;
		int end = ((Span)spans.get(spans.size()-1)).ref_end;
		consglyph = (AlignmentGlyph)map.setConsensus(start, end,
				consensus.getSequence().getResidues());
		Span sp;
		map.setDataModel(consglyph, consensus);
		//int cycle = 0;
		for (int j=0; j<spans.size(); j++) {
			sp = (Span)spans.get(j);
			map.addAlignedSpan(consglyph, sp.seq_start, sp.seq_end,
					sp.ref_start, sp.ref_end);
		}
		consensus_added = true;
		map.stretchToFit(true, false);
		map.updateWidget();
	}

	static char[] chars_to_check = { 'A', 'C', 'G', 'T', 'N', ' ' };
	static String[] font_names = { "Dialog", "Courier", "Helvetica",
		"DialogInput", "TimesRoman" };
	int font_name_index = 1;
	// rotates through different fonts:
	//    TimesRoman, Helvetica, Courier, Dialog, DialogInput
	public void switchFontType() {
		font_name_index++;
		if (font_name_index >= font_names.length) {
			font_name_index = 0;
		}
		String name = font_names[font_name_index];
		int style = map.getResidueFont().getStyle();
		int size = map.getResidueFont().getSize();
		Font fnt = new Font(name, style, size);
		map.setResidueFont(fnt);
		map.updateWidget();
		System.out.println(map.getResidueFont() + ", monospaced = " +
				GeneralUtils.isReallyMonospaced(fnt,
					DNAUtils.getAllowedDNACharacters()));
	}

	int[] font_styles = { Font.PLAIN, Font.BOLD, Font.ITALIC,
		Font.BOLD+Font.ITALIC };
	int font_style_index = 1;
	// rotates through PLAIN, BOLD, ITALIC, and BOLD+ITALIC
	public void switchFontStyle() {
		font_style_index++;
		if (font_style_index >= font_styles.length) {
			font_style_index = 0;
		}
		String name = map.getResidueFont().getName();
		int style = font_styles[font_style_index];
		int size = map.getResidueFont().getSize();
		Font fnt = new Font(name, style, size);
		map.setResidueFont(fnt);
		map.updateWidget();
		System.out.println(map.getResidueFont() + ", monospaced = " +
				GeneralUtils.isReallyMonospaced(fnt, chars_to_check));
	}

	// rotates through font sizes from 8 to 18
	public void switchFontSize() {
		Font current_font = map.getResidueFont();
		String name = current_font.getName();
		int style = current_font.getStyle();
		int size = current_font.getSize();
		if (size < 18) { size += 2; }
		else { size = 8; }
		Font fnt = new Font(name, style, size);
		map.setResidueFont(fnt);
		map.updateWidget();
		System.out.println(map.getResidueFont() + ", monospaced = " +
				GeneralUtils.isReallyMonospaced(fnt, chars_to_check));
	}

	//public void addAll() {
	//  addConsensus();
	//  Vector aligns;
	//  aligns = assem.getAlignments();
	//  for (int i=0; i<aligns.size(); i++) {
	//    addNext();
	//  }
	//}

	public void zoomFrameSetup() {
		zoomframe = new Frame();
		zoomframe.setBackground(Color.white);
		zoomframe.setLayout(new BorderLayout());
		zoomframe.add("North", hzoom);
		zoomframe.pack();
		zoomframe.setSize(200, zoomframe.getSize().height);
		zoomframe.setVisible(true);
		//zoomframe.show();
	}

	/**
	 *  An example of arbitrary sorting of alignments -- in this case,
	 *  just reverse the order in which they are currently sorted
	 *  General method for arbitrary sorting is:
	 *    call NeoAssembler.getAlignmentGlyphs() to get vector of all
	 *         alignment glyphs, ordered from top to bottom of NeoAssembler
	 *    rearrange ordering in vector
	 *    call NeoAssembler.pack() to repack from top to bottom as ordered
	 *         in the vector
	 */
	@SuppressWarnings("unchecked")
	public void doExternalSort() {
		List align_glyphs = map.getAlignmentGlyphs();
		Object temp1, temp2;
		int j;
		for (int i = 0; i < align_glyphs.size()/2; i++) {
			j = align_glyphs.size() - i - 1;
			temp1 = align_glyphs.get(i);
			temp2 = align_glyphs.get(j);
			align_glyphs.set(i, temp2);
			align_glyphs.set(j, temp1);
		}
		map.pack();
		map.updateWidget();
	}

	public void toggleExternalFontColoring() {
		external_font_coloring = !external_font_coloring;
		List aligns = map.getAlignmentGlyphs();
		List<AlignedResiduesGlyph> spans;
		AlignmentGlyph ag;
		for (int i=0; i<aligns.size(); i++) {
			ag = (AlignmentGlyph)aligns.get(i);
			spans = ag.getAlignedSpans();
			for (AlignedResiduesGlyph sg : spans) {
				if (external_font_coloring) {
					sg.setForegroundPainter(fg_painter);
					sg.setForegroundColorStrategy(AlignedResiduesGlyph.CALCULATED);
				}
				else {
					sg.setForegroundColorStrategy(AlignedResiduesGlyph.FIXED_COLOR);
				}
			}
		}
		// need to call updateWidget(true) rather than just updateWidget(), in
		//   order to force redrawing of entire widget if optimizations
		//   are turned on (optimizations are currently not aware of changes
		//   to residue glyphs' color strategies)
		map.updateWidget(true);
	}

	public void toggleExternalRectColoring() {
		external_rect_coloring = !external_rect_coloring;
		List aligns = map.getAlignmentGlyphs();
		List<AlignedResiduesGlyph> spans;
		AlignmentGlyph ag;
		for (int i=0; i<aligns.size(); i++) {
			ag = (AlignmentGlyph)aligns.get(i);
			spans = ag.getAlignedSpans();
			for (AlignedResiduesGlyph sg : spans) {
				if (external_rect_coloring) {
					sg.setBackgroundPainter(bg_painter);
					sg.setBackgroundColorStrategy(AlignedResiduesGlyph.CALCULATED);
				}
				else {
					sg.setBackgroundColorStrategy(AlignedResiduesGlyph.ALIGNMENT_BASED);
				}
			}
		}
		// need to call updateWidget(true) rather than just updateWidget(), in
		//   order to force redrawing of entire widget if optimizations
		//   are turned on (optimizations are currently not aware of changes
		//   to residue glyphs' color strategies)
		map.updateWidget(true);
	}

	@Override
		public String getAppletInfo() {
			return ("Demonstration of genoviz Software's Assembler Widget");
		}

	private void showFrames() {
		if (!going) {
			getGoing();
		}
		if (mapframe != null) {
			mapframe.setVisible(true);//mapframe.show();
		}
		if (zoomframe != null) {
			zoomframe.setVisible(true);//zoomframe.show();
		}
	}

	private void hideFrames() {
		if (null != propframe)
			propframe.setVisible(false);
		if (null != zoomframe)
			zoomframe.setVisible(false);
		if (null != mapframe)
			mapframe.setVisible(false);
	}

	@Override
		public void start() {
			if (framesShowing) {
				showFrames();
			}
		}

	@Override
		public void stop() {
			hideFrames();
		}


	@Override
		public void destroy() {
			if ( this.mapframe != null )  {
				this.mapframe.setVisible( false );
				this.mapframe.dispose();
				this.mapframe = null;
			}
			if ( this.zoomframe != null ) {
				this.zoomframe.setVisible( false );
				this.zoomframe.dispose();
				this.zoomframe = null;
			}
			if ( this.propframe != null ) {
				this.propframe.setVisible( false );
				this.propframe.dispose();
				this.propframe = null;
			}
			super.destroy();
		}

	@Override
		public void paint(Graphics g) {
			if (null == this.backgroundImage) {
				super.paint(g);
			}
			else {
				g.drawImage(this.backgroundImage, 0, 0, this.getSize().width, this.getSize().height, this);
			}
		}

	/* MENU SETUP */

	public void setupMenus(Frame dock) {

		editMenu = new Menu("Edit");
		propertiesMenuItem = new MenuItem("Properties...");
		addConsMenuItem = new MenuItem("Add consensus");
		addNextMenuItem = new MenuItem("Add align (range, gaps, bases)");
		addAlignMenuItem = new MenuItem("Add align (just range)");
		addGapsMenuItem = new MenuItem("Add gaps to selected align");
		addBasesMenuItem = new MenuItem("Add bases to selected align");
		addNegMenuItem = new MenuItem("Negative align test");
		addAllAligns = new MenuItem("Add rest of aligns");
		internalSortItem = new MenuItem("Sort");
		switchFontSizeItem = new MenuItem("Switch Font Size");
		switchFontTypeItem = new MenuItem("Switch Font Type");
		switchFontStyleItem = new MenuItem("Switch Font Style");
		externalSortItem = new MenuItem("Toggle external sorting");
		externalColorRectItem = new MenuItem("Toggle external rect coloring");
		externalColorFontItem = new MenuItem("Toggle external font coloring");

		editMenu.add(addConsMenuItem);
		editMenu.add(addNextMenuItem);
		editMenu.add(addAlignMenuItem);
		editMenu.add(addGapsMenuItem);
		editMenu.add(addBasesMenuItem);
		editMenu.add(addAllAligns);

		editMenu.addSeparator();
		editMenu.add(switchFontSizeItem);
		editMenu.add(switchFontTypeItem);
		editMenu.add(switchFontStyleItem);
		editMenu.add(internalSortItem);
		editMenu.add(externalSortItem);
		editMenu.add(externalColorRectItem);
		editMenu.add(externalColorFontItem);
		editMenu.addSeparator();
		editMenu.add(propertiesMenuItem);

		addConsMenuItem.addActionListener(this);
		addNextMenuItem.addActionListener(this);
		addAlignMenuItem.addActionListener(this);
		addGapsMenuItem.addActionListener(this);
		addBasesMenuItem.addActionListener(this);
		addAllAligns.addActionListener(this);
		switchFontSizeItem.addActionListener(this);
		switchFontTypeItem.addActionListener(this);
		switchFontStyleItem.addActionListener(this);
		internalSortItem.addActionListener(this);
		externalSortItem.addActionListener(this);
		externalColorRectItem.addActionListener(this);
		externalColorFontItem.addActionListener(this);
		propertiesMenuItem.addActionListener(this);

		MenuBar bar = dock.getMenuBar();
		if (null == bar) {
			bar = new MenuBar();
			dock.setMenuBar(bar);
		}
		bar.add(editMenu);
	}


	/* EVENT HANDLING */

	/** ActionListener Implementation */

	public void actionPerformed(ActionEvent evt) {
		Object theItem = evt.getSource();
		if (theItem == propertiesMenuItem) {
			showProperties();
		}
		else if (theItem == addAlignMenuItem) {
			addAlignment();
			//addAlignMenuItem.setEnabled(0 < assem.getAlignments().size());
		}
		else if (theItem == addGapsMenuItem) {
			try {
				addGaps();
			}
			catch (NullPointerException ex) {
				showStatus(ex.toString());
			}
		}
		else if (theItem == addBasesMenuItem) {
			try {
				addBases();
			}
			catch (NullPointerException ex) {
				showStatus(ex.toString());
			}
		}
		else if (theItem == addNextMenuItem) {
			//System.err.println("adding next");
			addNext();
		}
		else if (theItem == addNegMenuItem) {
			addNeg();
		}
		else if (theItem == addConsMenuItem) {
			addConsensus();
			addConsMenuItem.setEnabled(false); // Only do this once.
		}
		else if (theItem == addAllAligns) {
			addTheRest();
		}
		else if (theItem == internalSortItem) {
			map.setAutoSort(true);  // trigger a sort
			map.setAutoSort(false);  // then turn sorting off for sequences added later
			map.updateWidget();
		}
		else if (theItem == externalSortItem) {
			doExternalSort();
		}
		else if (theItem == externalColorRectItem) {
			toggleExternalRectColoring();
		}
		else if (theItem == externalColorFontItem) {
			toggleExternalFontColoring();
		}
		else if (theItem == switchFontSizeItem) {
			switchFontSize();
		}
		else if (theItem == switchFontTypeItem) {
			switchFontType();
		}
		else if (theItem == switchFontStyleItem) {
			switchFontStyle();
		}
	}

	/** WindowListener Implementation */

	public void windowActivated(WindowEvent e) {}
	public void windowDeactivated(WindowEvent e) {}
	public void windowIconified(WindowEvent e) {}
	public void windowDeiconified(WindowEvent e) {}
	public void windowOpened(WindowEvent e) {}
	public void windowClosed(WindowEvent e) {}
	public void windowClosing(WindowEvent e) {
		if(e.getSource() == mapframe) {
			this.stop();
		}
		else {
			((Window)e.getSource()).setVisible(false);
		}
	}


	/** MouseListener implelementation and collecting mouse events */

	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) {
		if (e.getSource() == map) {
			reportSelectedResidues();
		}
		else {
			if (clicking) {
				if (framesShowing) {
					hideFrames();
				}
				else {
					showFrames();
				}
				framesShowing = !framesShowing;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public void mousePressed(MouseEvent e) {
		if (e.getSource() == map) {
			selectedDataModels.removeAllElements();
			List<GlyphI> items = map.getSelected();
			for (Iterator<GlyphI> it = items.iterator(); it.hasNext();) {
				GlyphI gl = it.next();
				if (map.getDataModel(gl) != null) {
					selectedDataModels.addElement(map.getDataModel(gl));
				}
			}
		}
	}

	/** RangeListener implelementation */

	public void rangeChanged(NeoRangeEvent evt) {
	}

	public void reportSelectedResidues() {

		AlignmentGlyph aglyph = getSelectedGlyph ( map );
		if (null !=aglyph) {

			int[] theRange = getSelectedRange ( aglyph );
			System.out.println( "from " + theRange[0] + " to " + theRange[1] );
			System.out.println( getSelectedResidues ( aglyph ) );
			//Vector aspans = aglyph.getAlignedSpans();

			//AlignedResiduesGlyph aspan;
		}
	}

	public String getSelectedResidues ( AlignmentGlyph theGlyph ) {
		if ( ! (theGlyph.isSelected() ) ) return( "" );
		Rectangle2D.Double selectedBox = theGlyph.getSelectedRegion();
		Mapping glyphMap = theGlyph.getMapping();
		SequenceI glyphSeq = theGlyph.getSequence();
		int begSeq = (int)selectedBox.x;
		int endSeq = (int) ( selectedBox.x + selectedBox.width );
		int seqPos = 0;
		StringBuffer retSeq = new StringBuffer( "" );
		for ( int i = begSeq; i < endSeq; i++ ) {
			seqPos = glyphMap.mapToMapped(i); //seqPos = glyphMap.mapToSequence( i );
			char c = glyphSeq.getResidue( seqPos );
			if ( ' ' < c ) {
				retSeq.append ( glyphSeq.getResidue ( seqPos ) );
			}
		}
		return ( retSeq.toString() );
	}

	public int[] getSelectedRange (AlignmentGlyph theGlyph ) {
		int[] theRange = new int[2];
		if (null != theGlyph) {
			if ( ! (theGlyph.isSelected() ) ) {
				theRange[0] = 0;
				theRange[1] = 0;
			}
			else {
				Rectangle2D.Double selectedBox = theGlyph.getSelectedRegion();
				theRange[0] = (int)(selectedBox.x);
				theRange[1] = (int)(selectedBox.x + selectedBox.width - 1);
			}
		}
		return theRange;
	}

	protected AlignmentGlyph getSelectedGlyph ( NeoAssembler theAssembler ) {
		AlignmentGlyph oneGlyph;
		oneGlyph = (AlignmentGlyph)theAssembler.getConsensusGlyph();
		if (oneGlyph.isSelected() ) return oneGlyph;
		else {
			List<GlyphI> theGlyphs = theAssembler.getAlignmentGlyphs();

			for ( int i  = 0; i < theGlyphs.size(); i++ ) {
				oneGlyph = (AlignmentGlyph)theGlyphs.get ( i );
				if (oneGlyph.isSelected() ) return oneGlyph;
			}
		}
		return ( null );
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
		NeoAssemblerDemo me = new NeoAssemblerDemo();
		parameters = new Hashtable<String, String>();
		parameters.put("seq_file","data/test-sequence.data");
		parameters.put("map_file","data/test-assembly.data");
		me.init();
		me.start();
		JFrame frm = new JFrame("GenoViz NeoAssembler Demo");
		frm.getContentPane().add("Center", me);
		frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frm.pack();
		frm.setBounds(20, 40, 900, 400);
		frm.setVisible(true);
	}
}