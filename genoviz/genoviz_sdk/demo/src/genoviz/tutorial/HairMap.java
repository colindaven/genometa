package genoviz.tutorial;

import com.affymetrix.genoviz.event.NeoMouseEvent;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.widget.Shadow;
import com.affymetrix.genoviz.widget.VisibleRange;
import java.awt.*;
import java.awt.event.*;

public class HairMap extends SimpleMap3 {
	private VisibleRange zoomPoint = new VisibleRange();

	public HairMap() {
		NeoRangeListener zoomAdjuster = new NeoRangeListener() {
			public void rangeChanged( NeoRangeEvent e ) {
				double midPoint = ( e.getVisibleEnd() + e.getVisibleStart() ) / 2.0f;

				map.setZoomBehavior( NeoMap.X, NeoMap.CONSTRAIN_COORD, midPoint );
				map.updateWidget();
			}

		};

		this.zoomPoint.addListener( zoomAdjuster );

		Shadow hairline = new Shadow( this.map );

		hairline.setSelectable( false );

		MouseListener zoomPointAdjuster = new MouseAdapter() {
			@Override
			public void mouseReleased( MouseEvent e ) {
				double focus = ( ( NeoMouseEvent ) e ).getCoordX();
				zoomPoint.setSpot( focus );
			}

		};

		this.map.addMouseListener( zoomPointAdjuster );
		this.zoomPoint.addListener( hairline );
	}

	public static void main( String argv[] ) {
		HairMap me = new HairMap();
		Frame f = new Frame( "GenoViz" );
		f.add( me, BorderLayout.CENTER );
		f.addWindowListener( new WindowAdapter() {
			@Override
			public void windowClosing( WindowEvent e ) {
				Window w = ( Window ) e.getSource();
				w.dispose();
			}

			@Override
			public void windowClosed( WindowEvent e ) {
				System.exit( 0 );
			}

		} );
		f.pack();
		f.setBounds( 20, 40, 900, 400 );
		f.setVisible(true);
	}

}
