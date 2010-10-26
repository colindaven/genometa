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
import java.util.*;

import com.affymetrix.genoviz.bioviews.*;
import com.affymetrix.genoviz.widget.*;
import com.affymetrix.genoviz.widget.tieredmap.*;
import com.affymetrix.genoviz.awt.*;
import com.affymetrix.genoviz.event.*;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Label;
import java.awt.Menu;
import java.awt.MenuBar;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Toolkit;
import javax.swing.JScrollBar;

/**
  This applet, as its name implies, demonstrates some of the capabilities of
  the TieredNeoMap widget.  The map is divided into three sections, or tiers,
  each impletmented by a MapTierGlyph that packs its children and packs away
  from other tiers.   On the right hand side of the frame there is a
  TieredLabelMap, which, when registered with the TieredNeoMap, creates labels
  for each tier as they arrive.
 *
 * @version $Id: TieredMapDemo.java 4946 2010-01-07 22:56:59Z hiralv $
  */

public class TieredMapDemo extends Applet
	implements ActionListener, MouseListener {

	final int pixel_width = 500;
	final int pixel_height = 300;
	final int seq_start = -100;
	final int seq_end = 1050;
	final int offset_start = -20;
	final int offset_end = 100;
	final Color selectionColor = Color.red;

	TieredNeoMap map;
	JScrollBar yscroll;
	MapTierGlyph axisTier, squiggleTier, fillrectTier, roundrectTier, selectedTier;   // The tiers being used
	// Factories that maintain the type of glyph that is necessary for making glyphs.
	MapGlyphFactory squiggleFactory, fillRectFactory, roundRectFactory;
	TieredLabelMap labelmap;
	Frame frame;
	PopupMenu popup;
	MenuItem exit, showall;
	Image backgroundImage = null;

	public TieredMapDemo() {
	}

	@Override
	public void init() {
		String param;

		param = getParameter("background");
		if (null != param) {
			backgroundImage = this.getImage(this.getDocumentBase(), param);
		}

		if (null == backgroundImage) {
			Label placeholder =
				new Label("Running genoviz NeoMap Demo", Label.CENTER);
			this.setLayout(new BorderLayout());
			this.add("Center", placeholder);
			placeholder.setBackground(Color.black);
		}
	}

	@Override
	public void start() {

		/*
		   Set up basic characteristics of the map, many of which are inherited from NeoMap and do
		   not change significantly.  Setting the reshape behavior to NONE is fairly important, since
		   this allows for the TieredNeoMap to expand tiers gracefully as more glyphs arrive.
		   */

		map = new TieredNeoMap(false, false);
		map.setMapColor ( Color.black );
		map.setRubberBandBehavior ( false );
		map.setSelectionEvent ( NeoMap.ON_MOUSE_DOWN );
		map.setSelectionAppearance ( SceneI.SELECT_NONE );
		map.setSelectionColor ( selectionColor );
		map.setMapOffset ( offset_start, offset_end );
		map.setMapRange  ( seq_start, seq_end );
		map.setReshapeBehavior ( NeoAbstractWidget.Y, NeoConstants.NONE );
		map.addMouseListener ( this );

		NeoPanel map_pan = new NeoPanel();
		map_pan.setLayout ( new BorderLayout() );
		map_pan.add ( map, BorderLayout.CENTER );

		yscroll = new JScrollBar(JScrollBar.VERTICAL);
		map.setScroller ( NeoMap.Y, yscroll );
		map_pan.add ( yscroll, BorderLayout.EAST );

		frame = new Frame("Genoviz TieredNeoMap Demo");
		frame.setSize ( pixel_width, pixel_height );
		Dimension screen_size = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation((screen_size.width-pixel_width)/2,
				((screen_size.height-pixel_height)*3)/4);
		labelmap = new TieredLabelMap(false, false);
		// an exception will be thrown if the reshape behavior of the map and labelMap are not the same.
		labelmap.setReshapeBehavior ( NeoAbstractWidget.Y, NeoConstants.NONE );
		labelmap.setRubberBandBehavior ( false );

		// initializes the popupMenu that will be used to take input for the expansion behavior of the tier.
		initPopup();
		//  the following two lines are necessary for proper automatic labelling of tiers as they are generated.
		labelmap.addTierEventListener ( map );
		map.addTierEventListener ( labelmap );
		labelmap.setPreferredSize ( new Dimension ( 100, 100 ) );
		labelmap.setMapColor ( new Color ( 110, 110, 110 ) );
		labelmap.addMouseListener ( this );
		labelmap.setScroller ( NeoMap.Y, yscroll );

		NeoPanel labelpanel = new NeoPanel();
		labelpanel.setLayout ( new BorderLayout() );
		labelpanel.add ( labelmap, BorderLayout.CENTER );

		frame.add ( labelpanel, BorderLayout.EAST );
		frame.add ( map_pan, BorderLayout.CENTER );

		createTiers ();
		/*  BorderLayout puts the TieredNeoMap at the top at first, but then in the middle after
			it is shown and laid out the first time thereafter.  In order to put the map in one
			place and keep it there, it's necessary to add a ComponentListener that validates the
			view immediately after the first showing.
			*/

		frame.addComponentListener ( new ComponentAdapter() {
			@Override
			public void componentShown ( ComponentEvent ce ) {
				map.repack();
				ce.getComponent().validate();
			}
		} );
		frame.addWindowListener ( new WindowAdapter() {
			@Override
			public void windowClosing ( WindowEvent we ) {
				TieredMapDemo.this.stop();
				System.exit( 0 );
			}
		} );
		createMenuBar ( frame );
		frame.setVisible(true);//frame.show();
	}

	// sets up the popup menu
	void initPopup() {
		popup = new PopupMenu();
		popup.add ( new MenuItem ( "Expanded" ) ).addActionListener ( this );
		popup.add ( new MenuItem ( "Collapsed" ) ).addActionListener ( this );
		popup.add ( new MenuItem ( "Hidden" ) ).addActionListener ( this );
		labelmap.add ( popup );
	}

	/**
	 * Creates the three tiers for the view
	 * and the factories that will populate them with glyphs.
	 */
	@SuppressWarnings("unchecked")
	void createTiers () {
		axisTier = new MapTierGlyph();
		axisTier.setState ( MapTierGlyph.COLLAPSED );
		axisTier.setCoords ( seq_start, 0, seq_end-seq_start, 40 );
		axisTier.setFillColor ( Color.black );
		axisTier.setLabel ("");
		axisTier.setShowLabel ( false );
		GlyphI axis = map.addAxis ( 0 );
		axis.getCoordBox().height = 30;
		axis.setForegroundColor ( Color.white );
		axisTier.setMoreStrings(new Vector() );
		axisTier.addChild ( axis );
		map.addTier ( axisTier );

		squiggleFactory = makeFactory(com.affymetrix.genoviz.glyph.SquiggleGlyph.class, new Color ( 152, 12, 210 ));
		fillRectFactory = makeFactory(com.affymetrix.genoviz.glyph.FillRectGlyph.class, new Color ( 90, 212, 180 ));
		roundRectFactory = makeFactory(com.affymetrix.genoviz.glyph.RoundRectGlyph.class, new Color ( 45, 190, 40 ));

		squiggleTier = makeTier ( MapTierGlyph.EXPANDED, 15, "SquiggleGlyphs", squiggleFactory );
		squiggleTier.setFillColor (  Color.gray );
		fillrectTier = makeTier ( MapTierGlyph.EXPANDED, 15, "FillRectGlyphs", fillRectFactory );
		fillrectTier.setFillColor ( Color.black );
		roundrectTier = makeTier ( MapTierGlyph.EXPANDED, 15, "RoundRectGlyphs", roundRectFactory );
		roundrectTier.setFillColor ( Color.gray );
		map.repack();
		map.packTiers(false, true);
		map.updateWidget();
	}

	void createMenuBar ( Frame frame ) {
		MenuBar menubar = frame.getMenuBar();
		if ( menubar == null ) {
			menubar = new MenuBar();
			frame.setMenuBar ( menubar );
		}
		Menu file = new Menu ( "File" );
		exit = new MenuItem ( "Exit" );
		showall = new MenuItem ( "Show All Tiers" );
		file.add ( showall );
		file.addSeparator();
		file.add ( exit );
		exit.addActionListener ( this );
		menubar.add ( file );
		showall.addActionListener ( this );
	}

	// does all the standard operations for generating a MapGlyphFactory
	MapGlyphFactory makeFactory( Class clazz, Color color) {
		MapGlyphFactory factory = new MapGlyphFactory();
		factory.configure ( "-offset 0 -width 5" );
		factory.setScene(map.getScene() );
		factory.setGlyphtype ( clazz );
		factory.setColor ( color );
		return factory;
	}

	@SuppressWarnings("unchecked")
	MapTierGlyph makeTier( int state, int height, String label, MapGlyphFactory factory ) {
		MapTierGlyph tierglyph = new MapTierGlyph();
		/*
		   mapTierGlyphs are by default not hitable, which excludes them from NeoMouseEvents.
		   I needed to tell which Tier was clicked on in order to know which one to add a glyph
		   to.
		   */
		tierglyph.setHitable ( true );
		tierglyph.setSelectable ( false );
		/*
		   State can be one of four things:
		   MapTierGlyph.EXPANDED - packs children away from each other,
		   increses the size of the tier if necessary to fit all children
		   MapTierGlyph.COLLAPSED - packs to the width of the largest child, and lays glyphs over each other.
		   MapTierGlyph.HIDDEN - tier completely disappears
		   MapTierGlyph.FIXED_SIZE - still not completely stable, fixed size is supposed to act like expanded,
		   but without increasing in size when more children are selected.
		   */
		tierglyph.setState ( state );
		tierglyph.setCoords ( seq_start, 0, seq_end-seq_start, height );
		/*
		   If setShowLabel(true), then the label is printed on the glyph in the upper-left hand corner.  The label
		   is also used by the TieredLabelMap, if one is present.
		   */
		tierglyph.setLabel ( label );
		tierglyph.setShowLabel ( false );
		tierglyph.setMoreStrings ( new Vector() );
		map.addTier ( tierglyph );
		factory.setWidth ( height );
		GlyphI glyph;
		for ( int i = 0; i < 5; i++ ) {
			glyph = factory.makeGlyph ( (float)Math.random() * seq_start, (float)Math.random() * seq_end );
			tierglyph.addChild ( glyph );
		}
		return tierglyph;
	}

	/**
	 * used to add glyphs after initialization
	 * ( immediately above in makeeTier() ),
	 * on mouseClick events.
	 */
	void addGlyph ( MapTierGlyph mtg ) {
		MapGlyphFactory factory;
		if ( mtg.equals ( squiggleTier ) ) factory = squiggleFactory;
		else if ( mtg.equals ( fillrectTier ) ) factory = fillRectFactory;
		else if ( mtg.equals ( roundrectTier) ) factory = roundRectFactory;
		else return;
		int yval = yscroll.getValue();
		int offset = map.getVisibleOffset()[0];
		mtg.addChild ( factory.makeGlyph ( (float)Math.random() * seq_start, (float)Math.random() * seq_end ) );
		map.repack();
		yscroll.setValue ( yval );
		map.scrollOffset ( yval );
		labelmap.scrollOffset ( yval );
		map.updateWidget(true);
	}

	public void actionPerformed ( ActionEvent ae ) {
		Object src = ae.getSource();
		if ( ae.getSource().equals( exit ) ) frame.setVisible ( false );
		else if ( ae.getSource().equals ( showall ) ) {
			squiggleTier.setState ( MapTierGlyph.EXPANDED );
			fillrectTier.setState ( MapTierGlyph.EXPANDED );
			roundrectTier.setState ( MapTierGlyph.EXPANDED );
			map.packTiers ( true, false );
			map.updateWidget();
		}
		if ( selectedTier == null ) return;
		if ( selectedTier.getLabel().equals ( axisTier.getLabel() ) ) return;
		MenuItem source = (MenuItem)ae.getSource();
		/*
		   Changes the state of a tier.
		   Make sure to do a map.repack() or a map.packTiers(boolean, boolean)
		   after this to make it effective.
		   */
		if ( source.getLabel().equals ( "Expanded" ) ) selectedTier.setState ( MapTierGlyph.EXPANDED );
		if ( source.getLabel().equals ( "Collapsed" ) ) selectedTier.setState ( MapTierGlyph.COLLAPSED );
		if ( source.getLabel().equals ( "Hidden" ) ) selectedTier.setState ( MapTierGlyph.HIDDEN );
		map.repack();
		map.updateWidget ( true );
	}

	@Override
	public void stop () {
		frame.setVisible(false);
	}

	public void mouseClicked ( MouseEvent e ) {
		NeoMouseEvent nme = (NeoMouseEvent)e;
		List<GlyphI> glyphs = nme.getItems();
		for ( int i = 0; i < glyphs.size(); i++ )
			// iterate through selected glyphs, looking for a MapTierGlyph
			if ( glyphs.get ( i ) instanceof MapTierGlyph )
				selectedTier = (MapTierGlyph) glyphs.get( i );
			else (glyphs.get ( i )).draw(map.getView());
		if ( selectedTier == null ) return;
		if ( selectedTier.getLabel().equals ( axisTier.getLabel() ) ) return;
		if ( e.getSource().equals(map) )
			if ( (nme.getModifiers() & InputEvent.SHIFT_MASK) !=0 ) {
				for (int i = 0; i < glyphs.size(); i++) {
					if ( ! ( glyphs.get(i) instanceof MapTierGlyph ) ) {
						GlyphI remove_me = glyphs.get(i);
						map.removeItem ( remove_me );
						remove_me.getParent().removeChild(remove_me);
						map.repack();
						map.updateWidget();
						break;
					}
				}
			}
			else addGlyph ( selectedTier );
		else if ( ( ( nme.getModifiers() & InputEvent.BUTTON3_MASK ) != 0 ) ||
				( (nme.getModifiers() & InputEvent.CTRL_MASK ) !=0 ) )
			popup.show ( labelmap, nme.getX(), nme.getY() );
	}
	public void mousePressed ( MouseEvent e ) {
	}
	public void mouseReleased ( MouseEvent e ) {
	}
	public void mouseEntered ( MouseEvent e ) {
	}
	public void mouseExited ( MouseEvent e ) {
	}

	static public void main (String[] args) {
		TieredMapDemo tmdemo = new TieredMapDemo();
		tmdemo.start();
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

}
