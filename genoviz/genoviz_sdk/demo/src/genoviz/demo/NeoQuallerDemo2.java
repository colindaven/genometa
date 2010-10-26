package genoviz.demo;

import com.affymetrix.genoviz.awt.NeoPanel;
import com.affymetrix.genoviz.datamodel.ReadConfidence;
import com.affymetrix.genoviz.event.NeoRangeEvent;
import com.affymetrix.genoviz.event.NeoRangeListener;
import com.affymetrix.genoviz.parser.FASTQParser;
import com.affymetrix.genoviz.widget.NeoQualler;
import com.affymetrix.genoviz.widget.NeoQuallerCustomizer;

import java.applet.Applet;
import java.applet.AppletContext;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 *
 * @version $Id: NeoQuallerDemo2.java 6074 2010-06-04 18:49:01Z vbishnoi $
 */
public final class NeoQuallerDemo2 extends JApplet
				implements ActionListener, NeoRangeListener {

	JPanel widg_pan;
	ReadConfidence read_conf;
	NeoQualler widget;
	NeoQualler oneClone;
	int pixel_width = 500;
	int pixel_height = 250;
	TextField posText;
	TextField strText;
	Label posLabel;
	Label strLabel;
	Label descLabel;
	Choice searchChoice;
	Panel controlPanel;
	Menu editMenu = new Menu("Edit");
	MenuItem propertiesMenuItem = new MenuItem("Properties...");
	Frame propFrame; // For Properties
	private boolean propFrameShowing = false;

	public NeoQuallerDemo2() {
		editMenu.addSeparator();
		editMenu.add(propertiesMenuItem);
		propertiesMenuItem.addActionListener(this);
		controlPanel = constructControlPanel();
		widget = new NeoQualler();
		widget.addRangeListener(this);
	}

	@Override
	public String getAppletInfo() {
		return ("Demonstration of FASTQ Quality Scores Viewing Widget");
	}

	@Override
	public void init() {

		String seq = getParameter("FASTQFile");
		URL seqURL = null;

		try {
			seqURL = new URL(getDocumentBase(), seq);
			if (seqURL == null) {
				System.out.println("Couldn't find document base... exiting.");
				return;
			}
			init(seqURL);
			widget.setBasesTrimmedLeft(-10);
			widget.setBasesTrimmedRight(1);
		} catch (MalformedURLException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private void init(URL seqURL) {
		read_conf = FASTQParser.parseFiles(seqURL);
		String filestr = seqURL.getFile();
		int tempint = filestr.lastIndexOf('/');
		if (tempint != -1) {
			filestr = filestr.substring(tempint + 1);
		}
		init(filestr, read_conf);
	}

	private void init(String name, ReadConfidence read_conf) {
		this.read_conf = read_conf;

		descLabel.setText(name + ": " + read_conf.getReadLength() + " bases");

		((Component) widget).setSize(pixel_width, pixel_height);

		widget.setReadConfidence(read_conf);

		widget.updateWidget();

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
		widg_pan = new JPanel();
		widg_pan.setLayout(new BorderLayout());
		widg_pan.add("Center", (Component) widget);

		this.setLayout(new BorderLayout());
		add("Center", widg_pan);


		String param;
		param = getParameter("noControlPanel");
		if (null == param) {
			add("North", controlPanel);
		}

	}

	private Panel constructControlPanel() {
		descLabel = new Label();
		posText = new TextField(3);
		posText.addActionListener(this);
		posLabel = new Label("Center At Loc:");
		strText = new TextField(10);
		strText.addActionListener(this);
		strLabel = new Label("Center At String:");
		searchChoice = new Choice();
		searchChoice.addItem("Next");
		searchChoice.addItem("Prev");
		searchChoice.addItem("First");
		searchChoice.addItem("Last");
		Panel cp = new Panel();
		cp.setBackground(Color.white);
		cp.add(descLabel);
		cp.add(posLabel);
		cp.add(posText);
		cp.add(strLabel);
		cp.add(strText);
		cp.add(searchChoice);
		return cp;
	}

	@Override
	public void start() {
		if (null != propFrame && propFrameShowing) {
			propFrame.setVisible(true);
		}
		super.start();

		widget.zoomRange(1.0f);
		widget.scrollRange(0.0f);
	}

	@Override
	public void stop() {
		Container parent;
		parent = this.getParent();
		while (null != parent && !(parent instanceof Frame)) {
			parent = parent.getParent();
		}
		if (null != propFrame) {
			propFrameShowing = propFrame.isVisible();
			propFrame.setVisible(false);
		}
		super.stop();
	}

	public void showProperties() {
		if (null == propFrame) {
			propFrame = new Frame("NeoQualler Properties");
			NeoQuallerCustomizer customizer = new NeoQuallerCustomizer();
			customizer.setObject(this.widget);
			propFrame.add("Center", customizer);
			propFrame.pack();
			propFrame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent e) {
					propFrameShowing = false;
					propFrame.setVisible(false);
				}
			});
		}
		propFrame.setBounds(200, 200, 150, 150);
		propFrame.setVisible(true);
	}

	public void centerAtBase(int baseNum) {
		widget.centerAtBase(baseNum);
	}

	public void clearSelection() {
		widget.clearSelection();
	}
	int prevSearchPosition = -1;

	public void actionPerformed(ActionEvent evt) {
		Object evtSource = evt.getSource();
		if (evtSource == posText) {
			try {
				int basenum = Integer.parseInt(posText.getText());
				centerAtBase(basenum);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else if (evtSource == strText) {
			String searchString = strText.getText();
			String seqString = read_conf.getBaseString();
			String searchOption = searchChoice.getSelectedItem();
			int basenum = -1;
			if (searchOption.equals("First")) {
				basenum = seqString.indexOf(searchString);
			} else if (searchOption.equals("Last")) {
				basenum = seqString.lastIndexOf(searchString);
			} else if (searchOption.equals("Next")) {
				basenum = seqString.indexOf(searchString, prevSearchPosition + 1);
			} else if (searchOption.equals("Prev")) {
				basenum = seqString.lastIndexOf(searchString, prevSearchPosition - 1);
			}
			if (basenum == -1) {
				System.out.println("Sequence not found");
			} else {
				System.out.println("Centering at " + basenum);
				centerAtBase(basenum);
				prevSearchPosition = basenum;
			}
		} else if (evtSource == propertiesMenuItem) {
			showProperties();
		}
	}


	public void rangeChanged(NeoRangeEvent evt) {
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
		NeoQuallerDemo2 me = new NeoQuallerDemo2();
		parameters = new Hashtable<String, String>();
		parameters.put("FASTQFile","data/qualtest.fastq");
		me.init();
		me.start();
		JFrame frm = new JFrame("Genoviz NeoQualler2 Demo");
		frm.getContentPane().add("Center", me);
		JButton properties = new JButton("Properties");
		frm.getContentPane().add("South", properties);
		frm.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frm.pack();
		//frm.setBounds(20, 40, 900, 400);
		frm.setVisible(true);
	}

}
