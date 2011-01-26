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
package com.affymetrix.igb.view;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genoviz.awt.AdjustableJSlider;
import java.awt.event.MouseEvent;
import java.util.*;

import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.bioviews.LinearTransform;
import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.glyph.fhh.*;
import com.affymetrix.genoviz.util.NeoConstants;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.util.csv.CsvFile;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.geom.Point2D;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class BarGraphMap extends JPanel implements  SeqSelectionListener{

	// Java GUI Components
	NeoMap map;
	AdjustableJSlider xzoomer;
	AdjustableJSlider yzoomer;
	// Glyph Lists
	Vector<SeqBarGlyph> selected = new Vector<SeqBarGlyph>();
	Hashtable<SeqBarGlyph, SeqReads> _bars = new Hashtable<SeqBarGlyph, SeqReads>();
	Hashtable<BioSeq, SeqBarGlyph> _bioSeqToBars = new Hashtable<BioSeq, SeqBarGlyph>();
	// Hairline
	private com.affymetrix.igb.glyph.fhh.UnibrowHairline hairline = null;
	//
	private int _maxValue = 10;
	private int _seqCount = 0;
	private int _barWidth = 10;
	private int _barMargin = 1;
	private float _verticalInitialZoom = 1.1f;// percantage (a value greater then 1.0 zooms out!)
	private int _achsisOffset = 80;
	private int _barOffset = _achsisOffset + 1;
	private boolean _initialized = false;
	private AnnotatedSeqGroup _currentSeqGroup = null;
	private ArrayList<SeqReads>  _currentStatistics = null;
	private HashMap<AnnotatedSeqGroup, ArrayList<SeqReads>> _groups = null;
	private HashMap<AnnotatedSeqGroup, Integer> _groupsHash = null;
	private final static GenometryModel gmodel = GenometryModel.getGenometryModel();
	private final Color _barBGColor = new Color(0x66, 0x99, 0xff);
	private final Color _barSelectedBGColor = new Color(0x66, 0x00, 0xff);
	private final Color _graphBGColor = new Color(0xcc, 0xcc, 0xc8);



	private final SeqReads TEST_SEQ = new SeqReads(null, 0);


	private static BarGraphMap _instance = null;

	public static BarGraphMap getInstance(){
		if( _instance == null )
			_instance = new BarGraphMap();
		return _instance;
	}



	private BarGraphMap() {
		_groups = new HashMap<AnnotatedSeqGroup, ArrayList<SeqReads>>();
		_groupsHash = new HashMap<AnnotatedSeqGroup, Integer>();
		// init NeoMap
		initNeoMap();

		// inits Gui Components
		initGuiComponents();
	}

	/**
	 * Within this method we need a SequenceGroup to init
	 * the BarGraph
	 */
	public void init(AnnotatedSeqGroup seqGroup) {
		boolean hasChanged = false;
		if( _currentSeqGroup != null && _currentSeqGroup.getSeqList() != null && _groupsHash != null ){
			// check for the hasCode of the BioSeq Lists
			// if the hashCodes has changed, then the AnnotedSeqGroup
			// should be reloaded!
			hasChanged =  _currentSeqGroup.getSeqList().hashCode() != _groupsHash.get(_currentSeqGroup).intValue();
		}

		if(	_currentSeqGroup != seqGroup  || hasChanged ){
			_initialized = false;

			map.clearWidget();
			_bars.clear();
			_bioSeqToBars.clear();
			selected.clear();
			_maxValue = 10;
			_seqCount = 0;
			_currentSeqGroup = seqGroup;

			SwingWorker<Void, Void> worker;
			worker = new SwingWorker<Void, Void>(){

				@Override
				protected Void doInBackground(){
					// add notification Message
					Application.getSingleton().addNotLockedUpMsg("Initialize BarMap");

					if (_currentSeqGroup == null) {
						loadTestData();
					} else {
						loadData();
					}

					// check bar with most read-size (is the first cause sorted)
					if( _currentStatistics.get(0).getReads().intValue() < 1000 )
						setAchsisOffset(25);
					else if( _currentStatistics.get(0).getReads().intValue() < 1000000 )
						setAchsisOffset(50);
					else if( _currentStatistics.get(0).getReads().intValue() < 1000000000 )
						setAchsisOffset(80);
					else
						setAchsisOffset(110);


					// X (Vertical) Range
					map.setMapOffset(0, _barOffset + _seqCount * (_barWidth + _barMargin));

					// Y (Horizontal) Range
					map.setMapRange(-(int) ((float) _maxValue * _verticalInitialZoom), 0);



					AxisGlyph axis = new AxisGlyph(NeoConstants.VERTICAL);
					axis.setCoords(_achsisOffset - 10, map.getScene().getCoordBox().y, 20,
							map.getScene().getCoordBox().height);
					axis.setTickPlacement(AxisGlyph.LEFT);
					axis.setCoords(_achsisOffset, 0, 0, -((float) _maxValue * _verticalInitialZoom));
					axis.setLabelFormat(AxisGlyph.COMMA);
					map.addAxis(axis);


					// dreates hairline, to mark selection
					hairline = new com.affymetrix.igb.glyph.fhh.UnibrowHairline(map);
					// scroll by default in at the minimum coordinate of Y-Achsis
					map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_COORD, 0);
					hairline.setKeepHairlineInView(false);
					hairline.setPixelOffset(_barOffset);

					map.stretchToFit();

					map.scroll(NeoMap.Y, -((float) _maxValue * _verticalInitialZoom));


					map.updateWidget();
					// scroll by default in at the minimum coordinate of X-Achsis
					hairline.setSpot(getXZoomPoint(_barOffset));
					// scroll by default in at the minimum coordinate of Y-Achsis
					map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_COORD, 0);

					map.scroll(NeoMap.Y, -((float) _maxValue * _verticalInitialZoom));

					_initialized = true;

					//BioSeq selectedSeq = gmodel.getSelectedSeq();
					SeqBarGlyph g = _bioSeqToBars.get(gmodel.getSelectedSeq());
					if( g != null )
					{
						selectBar(g);
					}

					return null;
				}

				@Override
				protected void done(){
					// remove Message
					Application.getSingleton().removeNotLockedUpMsg("Initialize BarMap");
				}
			};
			// Execute the SwingWorker; the GUI will not freeze
			worker.execute();
		}

		// if the group is allready loaded try to select the
		// bar which represents the selected sequence in the SeqMap
		BioSeq selectedSeq = gmodel.getSelectedSeq();
		if ( selectedSeq != null) {
				SeqBarGlyph g = _bioSeqToBars.get(selectedSeq);
				if( g != null )
				{
					selectBar(g);
					map.updateWidget();
				}
		}
	}

	private void selectBar(SeqBarGlyph g) {
		// DESELECT THE OLD GLYPHS
		Iterator<SeqBarGlyph> it = selected.iterator();
		while (it.hasNext()) {
			it.next().setBackgroundColor(_barBGColor);
		}
		selected.clear();
		selected.add(g);
		g.setBackgroundColor(_barSelectedBGColor);
	}

	private void initNeoMap() {
		// with internal vertical and horizontal scroller
		map = new NeoMap(true, true, NeoConstants.VERTICAL, new LinearTransform());
		// double buffered canvas (no flicker)
		map.getNeoCanvas().setDoubleBuffered(false);
		map.setBackground(NeoMap.MAP, _graphBGColor);
		// add and init zoom abilities
		xzoomer = new AdjustableJSlider(Adjustable.HORIZONTAL);
		yzoomer = new AdjustableJSlider(Adjustable.VERTICAL);
		map.setZoomer(NeoMap.X, xzoomer);
		map.setZoomer(NeoMap.Y, yzoomer);

		// listen if the component will resized
		map.getNeoCanvas().addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				//SwingUtilities.invokeLater(new Runnable() {

					//public void run() {
						if (!_initialized) {
							return;
						}
						map.zoomToPrev(NeoMap.Y);
						map.scrollToPrev(NeoMap.Y);
						map.zoomToPrev(NeoMap.X);
						map.scrollToPrev(NeoMap.X);
						//map.scroll(NeoMap.Y, -((float) _maxValue * _verticalInitialZoom));
						map.updateWidget();
						// scroll by default in at the minimum coordinate of X-Achsis
						hairline.setSpot(getXZoomPoint(_barOffset));
						// scroll by default in at the minimum coordinate of Y-Achsis
						map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_COORD, 0);
						//System.out.println("BarGraphMap::componentResized");
					//}
				//});
			}
		});

		// add mouse listener
		map.addMouseListener(new MouseAdapter() {

			@Override
			public void mouseReleased(MouseEvent e) {
				if (!(e instanceof NeoMouseEvent)) {
					return;
				}
				mouseSelection((NeoMouseEvent) e);
			}
		});
	}

	private void initGuiComponents() {
		// init GUI
		Container cpane = this;
		cpane.setLayout(new BorderLayout());
		cpane.add("Center", map);
		cpane.add("North", xzoomer);
		cpane.add("West", yzoomer);
	}

	private void mouseSelection(NeoMouseEvent nevt) {
		Point2D.Double zoom_point = new Point2D.Double(nevt.getCoordX(), nevt.getCoordY());
		List<GlyphI> hits = nevt.getItems();

		boolean barSelected = false;
		// SELECT THE NEW GLYPHS
		Iterator<GlyphI> it2 = hits.iterator();
		while (it2.hasNext()) {
			GlyphI g = it2.next();
			System.out.println(g.getCoordBox().getY());
			if (g instanceof SeqBarGlyph) {
				selectBar(((SeqBarGlyph) g));
				barSelected = true;
				break;// no multiselection
			}
		}

		// select the sequence in the seq group and jump to the
		// seq map view
		if( barSelected ){
			BioSeq seq = _bars.get(selected.get(0)).getSeq();
			if( seq != null){
				if (seq != gmodel.getSelectedSeq()) {
					gmodel.setSelectedSeq(seq);
					Application.getSingleton().changeMainView(Application.MAIN_VIEW_SEQMAP);
				}
			}
		}else{
			// set spot only if there is no selection!
			if (hairline != null) {
				hairline.setSpot((int) zoom_point.x );
			}
		}
		//map.setZoomBehavior(NeoMap.X, NeoMap.CONSTRAIN_COORD, (int)zoom_point.x);

		map.updateWidget();
	}

	private void setAchsisOffset(int offset)
	{
		_achsisOffset = offset;
		_barOffset = _achsisOffset + 1;
	}

	private void addBar(int reads, String line1, String line2, SeqReads readsAnalysis) {
		SeqBarGlyph g = new SeqBarGlyph();
		g.setColor(_barBGColor);
		g.setFirstLineText(line1);
		g.setSecondLineText(line2);
		g.setRotPitch(Math.toRadians(-90.0));
		g.setPixelOffset( /*_bars.size()*5 +*/_barOffset);
		g.setCoords(_bars.size() * (_barWidth + _barMargin), 0, 10, -reads);
		//g.setCoords(_bars.size()*(_barWidth), 0, 10, -reads);
		_bars.put(g, readsAnalysis);
		_bioSeqToBars.put(readsAnalysis.getSeq(), g);
		map.addItem(g);
	}

	private void loadData() {
		// pruefen ob SeqGruppe null ist
		if (_currentSeqGroup == null) {
			throw new RuntimeException("Sequence Group is null!");
		}


		// falls gruppe bereits im cache, dann lade die benoetigten daten
		if (	_groups.containsKey(_currentSeqGroup) &&
				_groupsHash.get(_currentSeqGroup).intValue() == _currentSeqGroup.getSeqList().hashCode()) {
			_currentStatistics = _groups.get(_currentSeqGroup);
		} else {
			// create new read statistics list
			//_currentStatistics = new TreeSet<SeqReads>(new SeqReadsComparator());
			_currentStatistics = new ArrayList<SeqReads>();

			try{
			//int c  = 0;
			for (BioSeq bs : _currentSeqGroup.getSeqList()) {
				int readsNum = GeneralLoadUtils.getNumberOfSymmetriesForSeq(bs);
				if( readsNum < 0)
					continue;
				SeqReads tmpSeqRead = new SeqReads(bs, readsNum);
				_currentStatistics.add(tmpSeqRead);
				//if( ++c == 300) break;
			}
			}catch( Exception ex){
				_currentSeqGroup = null;
				_currentStatistics.clear();
				_currentStatistics = null;
				return;
			}

			if( _currentStatistics.size() < 1){
				_currentSeqGroup = null;
				_currentStatistics.clear();
				_currentStatistics = null;
				return;
			}

			Collections.sort(_currentStatistics, new SeqReadsComparator());


			_groups.put(_currentSeqGroup, _currentStatistics);
			_groupsHash.put(_currentSeqGroup, new Integer(_currentSeqGroup.getSeqList().hashCode()));
		}

		_seqCount = _currentStatistics.size();

		int c = 0;
		for (SeqReads sr : _currentStatistics) {
			c++;
			int reads = sr.getReads().intValue();

			String barText1 = "[ " + sr.getSeqGenus() + " . " +
					sr.getSeqSpecies() + " . " +
					sr.getSeqStrain() + " . " +
					reads + " ]";
			String barText2 =  "[ " + sr.getSeqID() + " ]";

			//System.out.println("addBar: -= " + barText + " =-");

			addBar(reads, barText1, barText2, sr  );
			if (reads > _maxValue) {
				_maxValue = reads;
			}
		}
	}

	private void loadTestData() {
		_maxValue = 10000;
		_seqCount = 100;
		int divid = (int) (_maxValue / (float) _seqCount);

		Random rand = new Random();
		for (int i = 0; i < _seqCount; i++) {
			int reads = _maxValue - (divid * i + rand.nextInt(divid));
			addBar(reads, "[" + (i + 1) + "] NC " + reads, "Line2-Text" , TEST_SEQ);
		}
	}

	private double getXZoomPoint(int zx) {
		Point pixZoomPoint = new Point(zx, 0);
		Point2D.Double zoom_point = new Point2D.Double();

		if (pixZoomPoint.x < _barOffset) {
			pixZoomPoint.x = _barOffset;
		}
		map.getView().transformToCoords(pixZoomPoint, zoom_point);
		return zoom_point.x;
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		if( GenometryModel.getGenometryModel().getSelectedSeqGroup() != null  ){
			this.init( GenometryModel.getGenometryModel().getSelectedSeqGroup() );
		}else {
			this.init( null );
		}
	}

	public class SeqReadsComparator implements Comparator<SeqReads> {
		public int compare(SeqReads o1, SeqReads o2) {
			return o2.getReads().intValue() - o1.getReads().intValue();
		}
	}

	public class SeqReads {

		private BioSeq _seq = null;
		private Integer _reads = null;
		private String _seqGenus = null;
		private String _seqSpecies = null;
		private String _seqStrain = null;
		private String _seqID = null;

		public SeqReads(BioSeq seq, int reads) {
			_seq = seq;
			_reads = new Integer(reads);

            if( seq != null ){
				_seqID = seq.getID();

				_seqGenus = SynonymLookup.getDefaultLookup().getGenomeFromRefSeq(getRefSeqIDFromBioSeqID(_seqID));
				_seqSpecies = SynonymLookup.getDefaultLookup().getGenomeSpeciesFromRefSeq(getRefSeqIDFromBioSeqID(_seqID));
				_seqStrain = SynonymLookup.getDefaultLookup().getGenomeStrainFromRefSeq(getRefSeqIDFromBioSeqID(_seqID));
            }else{
				_seqGenus = "NC_TESTTESTTEST123456";
			}
		}

		/**
		 * @return the _seq
		 */ public BioSeq getSeq() {
			return _seq;
		}

		/**
		 * @return the _reads
		 */ public Integer getReads() {
			return _reads;
		}

         public String getSeqGenus(){
			return _seqGenus;
		}

		 public String getSeqSpecies(){
			return _seqSpecies;
		}

		 public String getSeqStrain(){
			return _seqStrain;
		}

         public String getSeqID(){
			return _seqID;
		}
	}

	public void writeCsvFile(String file) {
		if (_currentStatistics == null) {
			JOptionPane.showMessageDialog(Application.getSingleton().getFrame(),
					"No data loaded in the overview diagram.\nLoad Data, open the overview diagram and retry.",
					"Error",
					JOptionPane.ERROR_MESSAGE);
			return;
		}
		// add Message
		Application.getSingleton().addNotLockedUpMsg("Writing diagram data to '" + file + "'");

		Object[] line = {"genus", "species", "strain", "refseq id", "read count"};

		// generate the csv writer
		CsvFile writer = new CsvFile(file);

		// write the header
		writer.writeLine(line);

		// write the other lines
		for (SeqReads r : _currentStatistics) {
			line[0] = r.getSeqGenus();
			line[1] = r.getSeqSpecies();
			line[2] = r.getSeqStrain();
			line[3] = r.getSeqID();
			line[4] = r.getReads();
			writer.writeLine(line);
		}

		/* do the work */

		// remove Message
		Application.getSingleton().removeNotLockedUpMsg("Writing diagram data to '" + file + "'");

		Application.getSingleton().setStatus("Wrote CSV file '" + file + "'");
	}

	public static String getRefSeqIDFromBioSeqID(String id) {
		//Get necessary informations from the current line (RefSeq id and genome name)
		String workingString = id.substring(id.indexOf("|NC_") + 1);
		//safe the refseq index and the corresponding chromesome name
		return workingString.substring(0, workingString.indexOf("|"));
	}
}
