package genoviz.tutorial;

import java.applet.Applet;
import java.awt.*;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.bioviews.GlyphI;
import javax.swing.JScrollBar;

public class TutorialGlyphDemo2 extends Applet {

	@Override
	public void init() {
		NeoMap map = new NeoMap(true, false);
		map.setSelectionEvent(NeoMap.ON_MOUSE_DOWN);
		map.setMapRange(0, 10000);
		map.addAxis(30);
		GlyphI tglyph;

		tglyph = new ScaleText();
		tglyph.setCoords(5000, 50, 1500, 15);
		tglyph.setColor(Color.orange);
		map.addItem(tglyph);

		tglyph = new ScaleText();
		tglyph.setCoords(3000, 70, 1000, 15);
		tglyph.setColor(Color.blue);
		map.addItem(tglyph);

		JScrollBar xzoomer = new JScrollBar(JScrollBar.VERTICAL);
		map.setZoomer(JScrollBar.HORIZONTAL, xzoomer);

		NeoPanel pan = new NeoPanel();
		pan.setLayout(new BorderLayout());
		pan.add("Center", map);
		pan.add("West", xzoomer);
		this.setLayout(new BorderLayout());
		this.add("Center", pan);

	}

}
