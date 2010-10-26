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

import com.affymetrix.genoviz.awt.AdjustableJSlider;
import java.awt.event.*;
import java.util.*;

import genoviz.demo.datamodel.*;
import genoviz.demo.adapter.*;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.MapGlyphFactory;
import com.affymetrix.genoviz.bioviews.SceneI;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRubberBandEvent;
import com.affymetrix.genoviz.event.NeoRubberBandListener;
import com.affymetrix.genoviz.glyph.ArrowGlyph;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.LineContainerGlyph;
import com.affymetrix.genoviz.glyph.OutlineRectGlyph;
import com.affymetrix.genoviz.glyph.SequenceGlyph;
import com.affymetrix.genoviz.glyph.StringGlyph;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.NeoMapCustomizer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Image;
import java.awt.Label;
import java.awt.Rectangle;
import javax.swing.JApplet;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSlider;
import javax.swing.WindowConstants;

/**
 * 
 * @version $Id: NeoMapDemo.java 6074 2010-06-04 18:49:01Z vbishnoi $
 */
public class NeoMapDemo extends JApplet
	implements MouseListener, ActionListener,
			   ItemListener, NeoRubberBandListener
{
  NeoMap map;
  JFrame propframe;

  JMenu fileMenu, editMenu, optionsMenu, selectionMenu, reshapeMenu,
    zoomingMenu, fuzzinessMenu;

  JMenuItem printMenuItem, exitMenuItem, clearMenuItem, deleteMenuItem,
    hideMenuItem, unhideMenuItem, optionsMenuItem;

  JCheckBoxMenuItem noSelection, highlightSelection, outlineSelection,
    selectRed, selectOrange, selectYellow;
  JCheckBoxMenuItem fitHorizontallyMenuItem, fitVerticallyMenuItem;
  JCheckBoxMenuItem zoomTopMenuItem, zoomMiddleMenuItem, zoomBottomMenuItem,
    zoomLeftMenuItem, zoomCenterMenuItem, zoomRightMenuItem;
  JCheckBoxMenuItem sharpPrecisionMenuItem, normalPrecisionMenuItem,
    fuzzyPrecisionMenuItem;

  int pixel_width = 600;
  int pixel_height = 300;
  int seq_start = -200;
  int seq_end = 1050;
  int offset_start = -100;
  int offset_end = 100;

  Vector hidden = new Vector();
  Color selectionColor = Color.red;
  int selectionType = SceneI.SELECT_FILL;

  Image backgroundImage = null;
  boolean clicking = false;
  NeoMapCustomizer customizer;
  boolean framesShowing = true;
  boolean going = false;
  Color nicePaleBlue = new Color(180, 250, 250);

  Label placeholder;

  public static void main(String[] args)  {
    NeoMapDemo me = new NeoMapDemo();
    JFrame frm = new JFrame("GenoViz NeoMap Demo");
    frm.getContentPane().add("Center", me);
    frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frm.pack();
    frm.setBounds( 20, 40, 900, 400 );
    frm.setVisible(true);
  }
    

  public NeoMapDemo()  {

    going = true;
    map = new NeoMap(true, true);
	
    map.setMapColor(nicePaleBlue);
	
    /**
     *  Use the NeoMap's built-in selection behavior.
     */
    map.setSelectionEvent(NeoMap.ON_MOUSE_DOWN);

    /**
     *  Specify selection appearance and color (though map will use defaults if not specified)
     */
    map.setSelectionAppearance(selectionType);
    map.setSelectionColor(selectionColor);

    // setting the coordinates of the linear map (in this case the start and
    //     end of the sequence)
    map.setMapRange(seq_start, seq_end);

    // setting the range of possible offset values (coordinates
    //    perpendicular to the linear map)
    map.setMapOffset(offset_start, offset_end);

    /**
     *  The map widget can be assigned Adjustables to control zooming
     *  along the axis (range) and perpendicular to the axis (offset).
     *  Once these are set up, changes to the Adjustable change the
     *  scale and offset of the map, and calls to the map's zoom
     *  methods change the Adjustables.
     */
    AdjustableJSlider xzoomer = new AdjustableJSlider(JSlider.HORIZONTAL);
    AdjustableJSlider yzoomer = new AdjustableJSlider(JSlider.VERTICAL);

    map.setZoomer(NeoMap.X, xzoomer);
    map.setZoomer(NeoMap.Y, yzoomer);

    // Place an axis along the center of the map.
    map.addAxis(1);
	
    addItemsDirectly();  // examples of adding items directly to map
    addItemsWithFactory();  // examples of adding items using factories
    addItemsWithDataAdapter();  // examples of adding items using data adapters
    addSequence();  // manipulation of a more sophisticated glyph

    setupMenus();
    Container cpane = this.getContentPane();
    cpane.setLayout(new BorderLayout());
    cpane.add("Center", map);
    cpane.add("North", xzoomer);
    cpane.add("West", yzoomer);

    map.addMouseListener(this);
    map.addRubberBandListener(this);
	
  }

  public void addItemsDirectly() {

    /**
     *  --------------- Adding items directly to Map ---------------
     *  Configuration method calls to override the default settings for 
     *    NeoMap's default MapGlyphFactory:
     *
     *    setMapOffset()   specifies the offset perpendicular to the axis
     *                   (in offset coordinates, not pixels)
     *    setBackgroundColor()
     *            specifies a color by name, this name hashes to
     *    setGlyphType()    specifies the class of the visual icon to use to
     *                represent map items
     *    setWidth()    specifies the width of the item perpendicular to the
     *                axis (in offset coordinates, not pixels)
     */
    MapGlyphFactory fac = map.getFactory();
    fac.setOffset(-30);
    fac.setBackgroundColor(Color.red);
    fac.setGlyphtype(com.affymetrix.genoviz.glyph.BoundedPointGlyph.class);
    fac.setWidth(5);

    /**
     *  Add an annotation to the map from base 30 to base 100,
     *  according to how the map's default glyph factory is currently configured
     */
    map.addItem(30, 100);

    fac.setGlyphtype(com.affymetrix.genoviz.glyph.FillOvalGlyph.class);
    map.addItem(400, 450);
    fac.setGlyphtype(com.affymetrix.genoviz.glyph.CenteredCircleGlyph.class);
    map.addItem(400,450);

    fac.setGlyphtype(com.affymetrix.genoviz.glyph.ArrowGlyph.class);
    fac.setOffset(-10);
    fac.setBackgroundColor(Color.green);
    fac.setWidth(10);
    map.addItem(300, 500);
    map.addItem(700, 600);
    map.addItem(800, 900);
    map.addItem(800,900);

    // adding items to other items
    fac.setGlyphtype(LineContainerGlyph.class);
    fac.setOffset(0);
    fac.setWidth(10);
    fac.setBackgroundColor(Color.green);
    GlyphI gene_item = map.addItem(100, 700);
    fac.setGlyphtype(FillRectGlyph.class);
    fac.setBackgroundColor(Color.blue);
    map.addItem(gene_item, map.addItem(100, 200));
    map.addItem(gene_item, map.addItem(350, 375));
    map.addItem(gene_item, map.addItem(600, 700));

	fac.setGlyphtype(StringGlyph.class);
	fac.setOffset(0);
	fac.setWidth(20);
	fac.setBackgroundColor(Color.blue);
	StringGlyph str_glyph = (StringGlyph) map.addItem(200,600);
	str_glyph.setString("Click Edit > Properties to change map behavior");
	str_glyph.setForegroundColor(Color.red);

  }

  /**
   *  ------------------- Factories -------------------
   *  Different configurations for adding items to maps can be stored
   *  in the map as factories.
   *  A map can have any number of factories, each of which
   *  can be configured to produce glyphs on the map with color,
   *  offset perpendicular to the axis, glyph class, etc. set by
   *  configuration methods similar to those of the map itself.
   *  Each factory has a String name, and can be referred to by name.
   */
  protected void addItemsWithFactory () {
    // add a factory named "factory1" to the map
    //    MapGlyphFactory arrow_fac = map.addFactory(
    //					  "-color test1 -offset -50 " + "-width 5");
    MapGlyphFactory arrow_fac = map.addFactory("");
    arrow_fac.setGlyphtype(ArrowGlyph.class);
    arrow_fac.setOffset(-80);
    arrow_fac.setBackgroundColor(Color.magenta);
    MapGlyphFactory outline_fac = map.addFactory("");
    outline_fac.setOffset(50);
    outline_fac.setGlyphtype(OutlineRectGlyph.class);
    outline_fac.setWidth(5);
    outline_fac.setForegroundColor(Color.black);

    // add an item to the map from 500 to 100 using factory arrow_fac
    map.addItem(arrow_fac, 500, 100);

    // Factories can also be called directly via makeItem()
    arrow_fac.setBackgroundColor(Color.pink);
    arrow_fac.makeItem(500, 400);
    outline_fac.makeItem(700, 900);
  }

  /**
   *  ----------------- Data Adapters -----------------
   *  Adding items directly to the map or through the map's
   *  factories by specifying a start and end position works for simple
   *  annotations.  For more complex annotations, classes that implement the
   *  NeoDataAdapterI interface provide a more general mechanism to
   *  represent data models on the map.  However, making a data adapter
   *  class requires knowledge of both the data model one wants to represent
   *  and more of the internals of the map widget.
   *
   *  Data Adapters can be used to automate building a visual
   *  representation of datamodels.  Each data adapter is specific
   *  to a particular class of data model.  Once a data adapter has
   *  been constructed and added to the map, calling Map.addData(model)
   *  will represent the model in the map in a manner specific to the
   *  data adapter and its configuration.
   */
  protected void addItemsWithDataAdapter() {
    

    // Annotation constructor takes as arguments: start, end
    Annotation annots[] = {
      new Annotation(100, 300),
      new Annotation(400, 500)
    };

    // ScoredAnnotation constructor takes as arguments: start, end, score
    ScoredAnnotation scoredannots[] = {
      new ScoredAnnotation(500, 550, 0.2),
      new ScoredAnnotation(600, 650, 0.4),
      new ScoredAnnotation(700, 750, 0.6),
      new ScoredAnnotation(800, 850, 0.8),
      new ScoredAnnotation(900, 950, 1.0),
    };

    /**
     *  ScoredAnnotAdapter uses the score of an annotation to
     *  determine a grayscale value to color the glyph representing it
     *  Unless told otherwise, it assumes a scoring system that
     *  ranges from 0.0 to 1.0, and maps this along a linear color
     *  scale, from 0.0 = black to 1.0 = white [values outside this
     *  range will give unpredictable results]
     */
    ScoredAnnotAdapter adapter1 = new ScoredAnnotAdapter(); // implements NeoDataAdapterI
    adapter1.getGlyphFactory().setOffset(-70);
    map.addDataAdapter(adapter1);

    /**
     *  AnnotationAdapter is a very simple adapter for representing annotations.
     *  Annotations implement the interface AnnotationI,
     *  which has a getStart() and getEnd() method
     */
    AnnotationAdapter adapter2 = new AnnotationAdapter(); // implements NeoDataAdapterI
    MapGlyphFactory afac = adapter2.getGlyphFactory();
    afac.setOffset(20);
    afac.setGlyphtype(com.affymetrix.genoviz.glyph.SquiggleGlyph.class);
    afac.setWidth(10);
    afac.setForegroundColor(Color.blue);
    //    adapter.configure("-offset 20 -glyphtype SquiggleGlyph -width 10");
    map.addDataAdapter(adapter2);

    // add an array of Annotations to the map
    for (int i=0; i<annots.length; i++) {
      map.addData(annots[i]);
    }

    // add an array of ScoredAnnotations to the map
    for (int i=0; i<scoredannots.length; i++) {
      map.addData(scoredannots[i]);
    }


  }

  /**
   *  Creating a sequence item, and adding visible annotations to it
   */
  protected void addSequence () {
    MapGlyphFactory seq_fac = map.addFactory("");
    seq_fac.setOffset(-88);
    seq_fac.setGlyphtype( SequenceGlyph.class );
    seq_fac.setWidth(16);
    
    String theSeq = "ACGTACGTACGTACTGACTGTTTTTAAAAAAATATATATATGAATTCGGG";
    SequenceGlyph sg =(SequenceGlyph)seq_fac.makeItem(500, 500 + theSeq.length()-1);
    sg.setResidues(theSeq);
    sg.setBackgroundColor(Color.yellow);
    sg.setForegroundColor(Color.black);

    // Add some boxes to the sequence glyph
    MapGlyphFactory box_fac = map.addFactory("");
    GlyphI child1 = box_fac.makeItem(501, 505);
    GlyphI child2 = box_fac.makeItem(541, 546);
    child1.setBackgroundColor(Color.green);
    child2.setBackgroundColor(Color.pink);
    map.addItem(sg, child1);
    map.addItem(sg, child2);
  }

	@Override
  public String getAppletInfo() {
    return ("Demonstration of Genoviz NeoMap Widget");
  }

  public void setupMenus()  {

    /*
    fileMenu = new JMenu("File");
    printMenuItem = new JMenuItem("Print...");
    exitMenuItem   = new JMenuItem("Exit");
    fileMenu.add(printMenuItem);
    fileMenu.addSeparator();
    fileMenu.add(exitMenuItem);
    printMenuItem.addActionListener(this);
    exitMenuItem.addActionListener(this);
    */


    editMenu = new JMenu("Edit");
    deleteMenuItem = new JMenuItem("Delete");
    clearMenuItem = new JMenuItem("Delete All");
    hideMenuItem = new JMenuItem("Hide");
    unhideMenuItem = new JMenuItem("Show Hidden");
    optionsMenuItem = new JMenuItem("Properties...");
    editMenu.add(deleteMenuItem);
    editMenu.add(clearMenuItem);
    editMenu.add(hideMenuItem);
    editMenu.add(unhideMenuItem);
    editMenu.addSeparator();
    editMenu.add(optionsMenuItem);
    deleteMenuItem.addActionListener(this);
    clearMenuItem.addActionListener(this);
    hideMenuItem.addActionListener(this);
    unhideMenuItem.addActionListener(this);
    optionsMenuItem.addActionListener(this);

    selectionMenu = new JMenu("Selection");
    noSelection = new JCheckBoxMenuItem("none");
    highlightSelection = new JCheckBoxMenuItem("highlighted");
    outlineSelection = new JCheckBoxMenuItem("outlined");
    selectRed = new JCheckBoxMenuItem("red");
    selectOrange = new JCheckBoxMenuItem("orange");
    selectYellow = new JCheckBoxMenuItem("yellow");
    highlightSelection.setState(true);
    selectRed.setState(true);
    selectionMenu.add(noSelection);
    selectionMenu.add(highlightSelection);
    selectionMenu.add(outlineSelection);
    selectionMenu.addSeparator();
    selectionMenu.add(selectYellow);
    selectionMenu.add(selectOrange);
    selectionMenu.add(selectRed);
    noSelection.addItemListener(this);
    highlightSelection.addItemListener(this);
    outlineSelection.addItemListener(this);
    selectYellow.addItemListener(this);
    selectOrange.addItemListener(this);
    selectRed.addItemListener(this);

    reshapeMenu = new JMenu("Reshaping");
    fitHorizontallyMenuItem = new JCheckBoxMenuItem("Fit Horizontally");
    fitVerticallyMenuItem = new JCheckBoxMenuItem("Fit Vertically");
    fitHorizontallyMenuItem.setState(true);
    fitVerticallyMenuItem.setState(true);
    reshapeMenu.add(fitHorizontallyMenuItem);
    reshapeMenu.add(fitVerticallyMenuItem);
    fitHorizontallyMenuItem.addItemListener(this);
    fitVerticallyMenuItem.addItemListener(this);

    zoomingMenu = new JMenu("Zoom from");
    zoomTopMenuItem = new JCheckBoxMenuItem("Top");
    zoomMiddleMenuItem = new JCheckBoxMenuItem("Middle");
    zoomBottomMenuItem = new JCheckBoxMenuItem("Bottom");
    zoomLeftMenuItem = new JCheckBoxMenuItem("Left");
    zoomCenterMenuItem = new JCheckBoxMenuItem("Center");
    zoomRightMenuItem = new JCheckBoxMenuItem("Right");
    zoomMiddleMenuItem.setState(true);
    zoomCenterMenuItem.setState(true);
    zoomingMenu.add(zoomTopMenuItem);
    zoomingMenu.add(zoomMiddleMenuItem);
    zoomingMenu.add(zoomBottomMenuItem);
    zoomingMenu.addSeparator();
    zoomingMenu.add(zoomLeftMenuItem);
    zoomingMenu.add(zoomCenterMenuItem);
    zoomingMenu.add(zoomRightMenuItem);
    zoomTopMenuItem.addItemListener(this);
    zoomMiddleMenuItem.addItemListener(this);
    zoomBottomMenuItem.addItemListener(this);
    zoomLeftMenuItem.addItemListener(this);
    zoomCenterMenuItem.addItemListener(this);
    zoomRightMenuItem.addItemListener(this);

    fuzzinessMenu = new JMenu("Pointer Precision");
    sharpPrecisionMenuItem = new JCheckBoxMenuItem("Sharp");
    normalPrecisionMenuItem = new JCheckBoxMenuItem("Normal");
    fuzzyPrecisionMenuItem = new JCheckBoxMenuItem("Fuzzy");
    normalPrecisionMenuItem.setState(true);
    fuzzinessMenu.add(sharpPrecisionMenuItem);
    fuzzinessMenu.add(normalPrecisionMenuItem);
    fuzzinessMenu.add(fuzzyPrecisionMenuItem);
    sharpPrecisionMenuItem.addItemListener(this);
    normalPrecisionMenuItem.addItemListener(this);
    fuzzyPrecisionMenuItem.addItemListener(this);

    optionsMenu = new JMenu("Options");
    optionsMenu.add(selectionMenu);
    optionsMenu.add(reshapeMenu);
    optionsMenu.add(zoomingMenu);
    optionsMenu.add(fuzzinessMenu);

    JMenuBar bar = new JMenuBar();
    this.setJMenuBar(bar);
    //    bar.add(fileMenu);
    bar.add(editMenu);
    bar.add(optionsMenu);
  }


  /** EventListener interface implementations: */

  /** MouseListener interface implementation */

  public void mouseClicked(MouseEvent e) { }
  public void mouseEntered(MouseEvent e) { }
  public void mouseExited(MouseEvent e) { }
  public void mouseReleased(MouseEvent e) { }

  public void mousePressed(MouseEvent e) {
    if (!(e instanceof NeoMouseEvent)) { return; }
    NeoMouseEvent nme = (NeoMouseEvent)e;
    Object coord_source = nme.getSource();
    if (coord_source == map) {
      // Make the selected item the center of zooming.
      map.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD, nme.getCoordX());
	  map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_COORD, nme.getCoordY());
    }
  }

  /** NeoRubberBandListener interface implementation */

  public void rubberBandChanged(NeoRubberBandEvent e) {
    int id = e.getID();
    if (id == NeoRubberBandEvent.BAND_END && map.getSelectionEvent() != NeoMap.NO_SELECTION) {
      // Here we add some selection by rubberband.
      Rectangle pixelBox = e.getPixelBox();
      pixelBox.setSize(pixelBox.width+1, pixelBox.height+1);
      int fuzziness = map.getPixelFuzziness();
      if (fuzziness <= pixelBox.height || fuzziness <= pixelBox.width) {
	// Rubberband is non-trivial.
	// Select items within it.
	List<GlyphI> items = map.getItems(pixelBox);
	if (!e.isShiftDown()) {
	  map.deselect(map.getSelected());
	}
	for (GlyphI gl : items) {
	  if (gl.isSelectable()) {
	    map.select(gl);
	  }
	}
	map.updateWidget();
      }
    }
  }

  /** ActionListener interface implementation */
  @SuppressWarnings("unchecked")
  public void actionPerformed(ActionEvent e) {
    Object theItem = e.getSource();
    if (theItem == printMenuItem) {
      printMap();
    }
    else if (theItem == exitMenuItem) {
      this.stop();
    }
    else if (theItem == deleteMenuItem) {
      map.removeItem(map.getSelected());
      map.updateWidget();
    }
    else if (theItem == clearMenuItem) {
      map.clearWidget();
      map.addAxis(0);
      map.updateWidget();
    }
   else if (theItem == hideMenuItem) {
		// should be able to just do setVisibility(selected, false);
		for (Iterator<GlyphI> it = map.getSelected().iterator(); it.hasNext();) {
			GlyphI gl = it.next();
			map.setVisibility(gl, false);
			hidden.addElement(gl);
		}
		map.updateWidget();
	}
    else if (theItem == unhideMenuItem) {
      // should be able to just do setVisibility(selected, true);
      Enumeration enm = hidden.elements();
      while (enm.hasMoreElements()) {
	GlyphI gl = (GlyphI)enm.nextElement();
	map.setVisibility(gl, true);
      }
      hidden.removeAllElements();
      map.updateWidget();
    }
    else if (theItem == optionsMenuItem) {
      if (propframe == null) {
	propframe = new JFrame("NeoMap Properties");
	customizer = new NeoMapCustomizer();
	customizer.setObject(map);
	propframe.getContentPane().add("Center", customizer);
	propframe.pack();
//	propframe.addWindowListener(this);
      }
//      propframe.setBounds(200, 200, 750, 300);
      propframe.setVisible(true);
    }

  }

  /* ItemListener interface implementation */

  public void itemStateChanged(ItemEvent e) {
    Object theItem = e.getSource();
    int state = e.getStateChange();
    if (state == ItemEvent.SELECTED)  {
      if (theItem == noSelection) {
	map.deselect(map.getSelected());
	map.updateWidget();
	map.setSelectionEvent(NeoMap.NO_SELECTION);
	// noSelection.setState(true);
	highlightSelection.setState(false);
	outlineSelection.setState(false);
      }
      else if (theItem == highlightSelection) {
	selectionType = SceneI.SELECT_FILL;
	map.setSelectionAppearance(selectionType);
	map.updateWidget();
	map.setSelectionEvent(NeoMap.ON_MOUSE_DOWN);
	noSelection.setState(false);
	// highlightSelection.setState(true);
	outlineSelection.setState(false);
      }
      else if (theItem == outlineSelection) {
	selectionType = SceneI.SELECT_OUTLINE;
	map.setSelectionAppearance(selectionType);
	map.updateWidget();
	map.setSelectionEvent(NeoMap.ON_MOUSE_DOWN);
	noSelection.setState(false);
	highlightSelection.setState(false);
	// outlineSelection.setState(true);
      }
      else if (theItem == selectRed)  {
	map.setSelectionColor(Color.red);
	selectOrange.setState(false);
	selectYellow.setState(false);
	map.updateWidget();
      }
      else if (theItem == selectOrange)  {
	map.setSelectionColor(Color.orange);
	selectRed.setState(false);
	selectYellow.setState(false);
	map.updateWidget();
      }
      else if (theItem == selectYellow)  {
	map.setSelectionColor(Color.yellow);
	selectRed.setState(false);
	selectOrange.setState(false);
	map.updateWidget();
      }
      else if (theItem == fitHorizontallyMenuItem) {
	if (((JCheckBoxMenuItem)theItem).getState()) {
	  map.setReshapeBehavior(NeoMap.X, NeoMap.FITWIDGET);
	  map.setSize(map.getSize()); // use Component's set/getSize method
	  map.updateWidget();
	}
	else {
	  map.setReshapeBehavior(NeoMap.X, NeoConstants.NONE);
	}
      }
      else if (theItem == fitVerticallyMenuItem) {
	if (((JCheckBoxMenuItem)theItem).getState()) {
	  map.setReshapeBehavior(NeoMap.Y, NeoMap.FITWIDGET);
	  map.setSize(map.getSize()); // use Component's set/getSize method
	  map.updateWidget();
	}
	else {
	  map.setReshapeBehavior(NeoMap.Y, NeoConstants.NONE);
	}
      }
      else if (theItem == zoomTopMenuItem) {
	// zoomTopMenuItem.setState(true);
	map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_START);
	zoomMiddleMenuItem.setState(false);
	zoomBottomMenuItem.setState(false);
      }
      else if (theItem == zoomMiddleMenuItem) {
	zoomTopMenuItem.setState(false);
	// zoomMiddleMenuItem.setState(true);
	map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_MIDDLE);
	zoomBottomMenuItem.setState(false);
      }
      else if (theItem == zoomBottomMenuItem) {
	zoomTopMenuItem.setState(false);
	zoomMiddleMenuItem.setState(false);
	map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_END);
	// zoomBottomMenuItem.setState(true);
      }
      else if (theItem == zoomLeftMenuItem) {
	// zoomLeftMenuItem.setState(true);
	map.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_START);
	zoomCenterMenuItem.setState(false);
	zoomRightMenuItem.setState(false);
      }
      else if (theItem == zoomCenterMenuItem) {
	zoomLeftMenuItem.setState(false);
	map.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_MIDDLE);
	// zoomCenterMenuItem.setState(true);
	zoomRightMenuItem.setState(false);
      }
      else if (theItem == zoomRightMenuItem) {
	zoomLeftMenuItem.setState(false);
	zoomCenterMenuItem.setState(false);
	map.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_END);
	// zoomRightMenuItem.setState(true);
      }
      else if (theItem == sharpPrecisionMenuItem) {
	map.setPixelFuzziness(0);
	// sharpPrecisionMenuItem.setState(true);
	normalPrecisionMenuItem.setState(false);
	fuzzyPrecisionMenuItem.setState(false);
      }
      else if (theItem == normalPrecisionMenuItem) {
	map.setPixelFuzziness(2);
	sharpPrecisionMenuItem.setState(false);
	// normalPrecisionMenuItem.setState(true);
	fuzzyPrecisionMenuItem.setState(false);
      }
      else if (theItem == fuzzyPrecisionMenuItem) {
	map.setPixelFuzziness(5);
	sharpPrecisionMenuItem.setState(false);
	normalPrecisionMenuItem.setState(false);
	// fuzzyPrecisionMenuItem.setState(true);
      }
    }
  }


  /** Printing */
  void printMap () {
    System.err.println("printing not yet reimplemented");
    // Obtain a PrintJob and a Graphics object to use with it
    /*
    Toolkit toolkit = Toolkit.getDefaultToolkit();
    PrintJob job    = toolkit.getPrintJob (this, "Print Map",
					   new Properties());
    if (job == null)
      return;  // i.e. the user clicked Cancel.
    Graphics g = job.getGraphics();
    // Give the output some margins (avoid scrunching in upper left corner)
    g.translate (50,50);
    Dimension size = mapframe.getSize();
    // Set a clipping region
    g.setClip (0, 0, size.width, size.height);
    // Print the mapframe and the components it contains
    mapframe.printAll (g);
    // Finish up.
    g.dispose();
    job.end();
    */
  }


}
