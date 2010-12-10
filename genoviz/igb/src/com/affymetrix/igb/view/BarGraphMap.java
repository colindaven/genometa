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
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.Adjustable;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.geom.Point2D;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class BarGraphMap extends JPanel {

	// Java GUI Components
	NeoMap map;
	AdjustableJSlider xzoomer;
	AdjustableJSlider yzoomer;
	// Glyph Lists
	Vector<SeqBarGlyph> selected = new Vector<SeqBarGlyph>();
	Hashtable<SeqBarGlyph, SeqReads> _bars = new Hashtable<SeqBarGlyph, SeqReads>();
	// Hairline
	private com.affymetrix.igb.glyph.fhh.UnibrowHairline hairline = null;
	//
	private int _maxValue = 10;
	private int _seqCount = 0;
	private int _barWidth = 10;
	private int _barMargin = 1;
	private float _verticalInitialZoom = 1.1f;// percantage (a value greater then 1.0 zooms out!)
	private int _achsisOffset = 50;
	private int _barOffset = _achsisOffset + 1;
	private boolean _initialized = false;
	private AnnotatedSeqGroup _currentSeqGroup = null;
	private ArrayList<SeqReads>  _currentStatistics = null;
	private HashMap<AnnotatedSeqGroup, ArrayList<SeqReads>> _groups = null;
	private final static GenometryModel gmodel = GenometryModel.getGenometryModel();
	private final Color _barBGColor = new Color(0x66, 0x99, 0xff);
	private final Color _barSelectedBGColor = new Color(0x66, 0x00, 0xff);
	private final Color _graphBGColor = new Color(0xcc, 0xcc, 0xc8);



	private final SeqReads TEST_SEQ = new SeqReads(null, 0);

	public BarGraphMap() {
		_groups = new HashMap<AnnotatedSeqGroup, ArrayList<SeqReads>>();

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
		if (_currentSeqGroup == seqGroup && seqGroup != null) {
			return;
		}

		_initialized = false;


		map.clearWidget();
		_bars.clear();
		selected.clear();
		_maxValue = 10;
		_seqCount = 0;
		_currentSeqGroup = seqGroup;


		SwingUtilities.invokeLater(new Runnable() {

			public void run() {

				if (_currentSeqGroup == null) {
					loadTestData();
				} else {
					loadData();
				}




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

				_initialized = true;
			}
		});
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
		map.addComponentListener(new ComponentAdapter() {

			@Override
			public void componentResized(ComponentEvent e) {
				SwingUtilities.invokeLater(new Runnable() {

					public void run() {
						if (!_initialized) {
							return;
						}
						map.scroll(NeoMap.Y, -((float) _maxValue * _verticalInitialZoom));
						map.updateWidget();
						// scroll by default in at the minimum coordinate of X-Achsis
						hairline.setSpot(getXZoomPoint(_barOffset));
						// scroll by default in at the minimum coordinate of Y-Achsis
						map.setZoomBehavior(NeoMap.Y, NeoMap.CONSTRAIN_COORD, 0);
					}
				});
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

		// DESELECT THE OLD GLYPHS
		Iterator<SeqBarGlyph> it = selected.iterator();
		while (it.hasNext()) {
			it.next().setBackgroundColor(_barBGColor);
		}
		selected.clear();

		Iterator<GlyphI> it2 = hits.iterator();
		// SELECT THE NEW GLYPHS

		while (it2.hasNext()) {
			GlyphI g = it2.next();
			System.out.println(g.getCoordBox().getY());
			if (g instanceof SeqBarGlyph) {
				selected.add((SeqBarGlyph) g);
				((SeqBarGlyph) g).setBackgroundColor(_barSelectedBGColor);
				break;// no multiselection
			}
		}

		// select the sequence in the seq group and jump to the
		// seq map view
		if( selected.size() > 0 ){
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
		map.addItem(g);
	}

	private void loadData() {
		if (_currentSeqGroup == null) {
			throw new RuntimeException("Sequence Group is null!");
		}


		if (_groups.containsKey(_currentSeqGroup)) {
			_currentStatistics = _groups.get(_currentSeqGroup);
		} else {
			// create new read statistics list
			//_currentStatistics = new TreeSet<SeqReads>(new SeqReadsComparator());
			_currentStatistics = new ArrayList<SeqReads>();

			//int c  = 0;
			for (BioSeq bs : _currentSeqGroup.getSeqList()) {
				SeqReads tmpSeqRead = new SeqReads(bs, GeneralLoadUtils.getNumberOfSymmetriesForSeq(bs));
				_currentStatistics.add(tmpSeqRead);
				//if( ++c == 300) break;
			}

			Collections.sort(_currentStatistics, new SeqReadsComparator());

			_groups.put(_currentSeqGroup, _currentStatistics);
		}

		_seqCount = _currentStatistics.size();

		int c = 0;
		for (SeqReads sr : _currentStatistics) {
			c++;
			int reads = sr.getReads().intValue();
            String id = sr.getSeqID();
            String name = sr.getSeqName();

			//String barText = "[" + c + "][" + reads + "] " + id;
			String barText1 = "[ " + name + " ]";
			String barText2 = "[ " + reads + " ] [ " + id + " ]";

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

	public class SeqReadsComparator implements Comparator<SeqReads> {
		public int compare(SeqReads o1, SeqReads o2) {
			return o2.getReads().intValue() - o1.getReads().intValue();
		}
	}

	public class SeqReads {

		private BioSeq _seq = null;
		private Integer _reads = null;
        private String _seqName = null;
        private String _seqID = null;

        public SeqReads(BioSeq seq, int reads) {
            _seq = seq;
            _reads = new Integer(reads);

            if( seq != null ){
				_seqID = seq.getID();
				_seqName = SynonymLookup.getDefaultLookup().getGenomeNameFromRefSeq(_seqID);
            }else{
                _seqName = "NC_TESTTESTTEST123456";
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

         public String getSeqName(){
             return _seqName;
         }

         public String getSeqID(){
             return _seqID;
         }
	}
}
