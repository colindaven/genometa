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

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.datamodel.*;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.glyph.AlignmentGlyph;
import com.affymetrix.genoviz.widget.*;

import genoviz.demo.adapter.AssemblyAdapter;
import genoviz.demo.datamodel.Assembly;
import genoviz.demo.parser.AlignmentParser;
import genoviz.demo.parser.SequenceParser;

import java.applet.*;
import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Panel;
import java.awt.event.*;
import java.awt.geom.Rectangle2D;
import java.net.*;
import java.util.*;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

/**
 * This is a reworking of the old NeoAssemblerDemo
 *
 * It adds a button to clear and reload the NeoAssembler.
 * This to check a bug reported by Genentech.
 *
 * @version $Id: NeoAssemblerDemo2.java 6217 2010-06-22 14:54:18Z vbishnoi $
 */
public class NeoAssemblerDemo2 extends JApplet
		implements NeoRangeListener,
			   MouseListener{

	Assembly assem; // the data model

	NeoAssembler map; // the widget

	public NeoAssemblerDemo2() {

		// setting up buttons to demo layout configurations
		Panel buttonpanel = buttonSetup();

		map = new NeoAssembler();

		/**
		 *  Assembler inherits from Panel, and thus all of the methods of
		 *  java.awt.Component, Container, and Panel are available,
		 *  such as setting the background color, and resizing
		 */
		((Component)map).setBackground(new Color(180, 250, 250));
		((Component)map).setSize( 840, 200);

		// Let's give the labels lots of room.
		map.setLabelWidth( 200 );

		// Use the NeoAssembler's built-in selection methods.
		map.setSelectionEvent(NeoAssembler.ON_MOUSE_DOWN);
		map.setSelectionBehavior(NeoAssembler.SELECT_RESIDUES);
		map.addMouseListener(this);
		map.addRangeListener(this);
		/**
		  In order for the assembly map to automatically respond to resize events
		  by filling the available space, it is highly recommended a
		  BorderLayout (or similarly flexible layout manager) be used for the
		  Container that holds the BasicMap, and add the BasicMap in the center.
		  */
		Container cpane = this.getContentPane();
		cpane.setLayout(new BorderLayout());

		cpane.add("North", buttonpanel);

//		NeoPanel widg_pan = new NeoPanel();
//		widg_pan.setLayout(new BorderLayout());
		cpane.add("Center", (Component)this.map);
		//add("Center", widg_pan);

	}


	URL seq_URL = null, align_URL = null;
	@Override
	public void init() {
		int alignwidth = Integer.parseInt(getParameter("alignwidth"));
		try {
			seq_URL = new URL(this.getDocumentBase(), getParameter("seq_file"));
			System.out.println(seq_URL);
			align_URL = new URL(this.getDocumentBase(), getParameter("map_file"));
		}
		catch(Exception ex) {
			System.out.println(ex.getMessage());
			ex.printStackTrace();
		}
		assem = loadData(seq_URL, align_URL);

		//---- set range of assembly map ----
		// this is required due to current bug in Assembler
		map.setRange(0, assem.getLength()+1);

		AssemblyAdapter adapter = new AssemblyAdapter(map);
		map.addDataAdapter(adapter);
		map.addData(this.assem);
		map.getSelectedObjects();
		map.addItemListener( new ItemListener() {
			public void itemStateChanged( ItemEvent e ) {
				System.out.println(e.getItem().toString());

				
			}
		} );

	}


	/**
	 * puts buttons on a panel so the user can instigate actions.
	 */
	public Panel buttonSetup() {
		Panel bp = new Panel();

		Button labelB = new Button("Labels Left");
		bp.add(labelB);
		labelB.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				Button b = (Button) e.getSource();
				int id = map.getPlacement(NeoAssembler.LABELS);
				if (id == NeoAssembler.PLACEMENT_LEFT) {
					map.configureLayout( NeoAssembler.LABELS, NeoAssembler.PLACEMENT_RIGHT );
					b.setLabel( "Labels Right" );
				}
				else if ( id == NeoAssembler.PLACEMENT_RIGHT ) {
					map.configureLayout( NeoAssembler.LABELS, NeoAssembler.PLACEMENT_LEFT );
					b.setLabel( "Labels Left" );
				}
			}
		});

		Button consensusB = new Button("Consensus Top");
		bp.add(consensusB);
		consensusB.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				Button b = (Button) e.getSource();
				int id = map.getPlacement(NeoAssembler.CONSENSUS);
				if (id == NeoAssembler.PLACEMENT_BOTTOM) {
					map.configureLayout(NeoAssembler.CONSENSUS, NeoAssembler.PLACEMENT_TOP);
					b.setLabel("Consensus Top");
				}
				else if (id == NeoAssembler.PLACEMENT_TOP) {
					map.configureLayout(NeoAssembler.CONSENSUS, NeoAssembler.PLACEMENT_BOTTOM);
					b.setLabel("Consensus Bottom");
				}
			}
		} );

		Button axisB = new Button("Axis Scroller Bottom");
		bp.add(axisB);
		axisB.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				Button b = (Button) e.getSource();
				int id = map.getPlacement(NeoAssembler.AXIS_SCROLLER);
				if (id == NeoAssembler.PLACEMENT_BOTTOM) {
					map.configureLayout(NeoAssembler.AXIS_SCROLLER, NeoAssembler.PLACEMENT_TOP);
					b.setLabel("Axis Scroller Top");
				}
				else if (id == NeoAssembler.PLACEMENT_TOP) {
					map.configureLayout(NeoAssembler.AXIS_SCROLLER, NeoAssembler.PLACEMENT_BOTTOM);
					b.setLabel("Axis Scroller Bottom");
				}
			}
		} );

		Button offsetB = new Button("Offset Scroller Left");
		bp.add(offsetB);
		offsetB.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				Button b = (Button) e.getSource();
				int id = map.getPlacement(NeoAssembler.OFFSET_SCROLLER);
				if (id == NeoAssembler.PLACEMENT_LEFT) {
					map.configureLayout(NeoAssembler.OFFSET_SCROLLER, NeoAssembler.PLACEMENT_RIGHT);
					b.setLabel("Offset Scroller Right");
				}
				else if (id == NeoAssembler.PLACEMENT_RIGHT) {
					map.configureLayout(NeoAssembler.OFFSET_SCROLLER, NeoAssembler.PLACEMENT_LEFT);
					b.setLabel("Offset Scroller Left");
				}
			}
		} );

		Button resetB = new Button( "Clear" );
		bp.add( resetB );
		resetB.addActionListener( new ActionListener() {
			public void actionPerformed( ActionEvent e ) {
				Button b = (Button) e.getSource();
				if ( b.getLabel().equals( "Clear" ) ) {
					map.clearWidget();
					b.setLabel( "Reload" );
				}
				else {
					NeoAbstractWidget aw = map.getWidget( NeoAssembler.ALIGNMENTS );
					NeoAbstractWidget lw = map.getWidget( NeoAssembler.LABELS );
					((NeoMap)aw).setMapOffset( 0, 0 );
					((NeoMap)lw).setMapOffset( 0, 0 );
					assem = loadData(seq_URL, align_URL);
					map.addData(assem);
					map.setRange(0, assem.getLength()+1);
					map.zoom( NeoAssembler.X, map.getMaxZoom( NeoAssembler.X ) );
					map.scroll( NeoAssembler.Y, 1 );
					map.scroll( NeoAssembler.Y, -1 );
					validate();
					b.setLabel( "Clear" );
				}
		map.updateWidget();
			}
		} );

		return bp;
	}

	public Assembly loadData(URL seq_URL, URL align_URL) {
		Vector seqs = null;
		Vector aligns = null;
		Hashtable seqhash = new Hashtable();
		System.out.println(align_URL);
		seqs = SequenceParser.getSequences(seq_URL);

		Iterator i = seqs.iterator();
		while(i.hasNext()){
			System.out.print(i.next());
		}
		aligns = AlignmentParser.getAlignments(align_URL);
		System.out.println("");
		i = aligns.iterator();
		while(i.hasNext()){
			System.out.print(i.next());
		}
		// This assumes consensus is FIRST in alignment/mapping input
		Mapping consmap = (Mapping)aligns.elementAt(0);
		Assembly model = new Assembly(consmap, aligns, seqs);
		return model;
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
		NeoAssemblerDemo2 me = new NeoAssemblerDemo2();
		parameters = new Hashtable<String, String>();
		parameters.put("seq_file","data/test-sequence.data");
		parameters.put("map_file","data/test-assembly.data");
		parameters.put("alignwidth","14");
		me.init();
		me.start();
		JFrame frm = new JFrame("GenoViz NeoAssembler-2 Demo");
		frm.getContentPane().add("Center", me);
		frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frm.pack();
		frm.setBounds(20, 40, 900, 400);
		frm.setVisible(true);
	}
	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) {
		if (e.getSource() == map) {
			reportSelectedResidues();
		}
		/*else {
			if (clicking) {
				if (framesShowing) {
					hideFrames();
				}
				else {
					showFrames();
				}
				framesShowing = !framesShowing;
			}*/
		}


	@SuppressWarnings("unchecked")
	public void mousePressed(MouseEvent e) {
		if (e.getSource() == map) {
			//selectedDataModels.removeAllElements();
			List<GlyphI> items = map.getSelected();
			for (Iterator<GlyphI> it = items.iterator(); it.hasNext();) {
				GlyphI gl = it.next();
				if (map.getDataModel(gl) != null) {
				//	selectedDataModels.addElement(map.getDataModel(gl));
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


}
