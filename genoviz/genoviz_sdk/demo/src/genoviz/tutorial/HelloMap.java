package genoviz.tutorial;

import java.awt.Container;
import javax.swing.JApplet;
import javax.swing.JFrame;
import javax.swing.WindowConstants;
import com.affymetrix.genoviz.widget.NeoMap;

public class HelloMap extends JApplet {
  NeoMap map;

  public HelloMap() {
    map = new NeoMap();
    Container cpane = this.getContentPane();
    cpane.add("Center", map);
    map.setMapRange( 0, 1000);
    map.setMapOffset( -20, 100 );
    map.addAxis( 0 );
    map.addItem(200, 500);
    map.addItem(300, 700);
  }

  
   public static void main( String argv[] ) {
    JFrame frm = new JFrame( "GenoViz HelloMap" );
    Container cpane = frm.getContentPane();
    cpane.add( "Center", new HelloMap() );
    frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    frm.setBounds( 20, 40, 300, 250 );
    frm.setVisible(true);
  }

}
