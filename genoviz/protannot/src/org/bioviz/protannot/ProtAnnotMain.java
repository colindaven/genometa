/**
 * Main class. This builds the GUI components and establishes their
 * behavior.
 */
package org.bioviz.protannot;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.ConsoleView;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.genoviz.swing.ColorTableCellEditor;
import com.affymetrix.genoviz.swing.ColorTableCellRenderer;
import com.affymetrix.genoviz.util.ComponentPagePrinter;
import com.affymetrix.genoviz.util.FileDropHandler;

import org.freehep.util.export.ExportDialog;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Toolkit;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.prefs.Preferences;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JTextField;
import javax.swing.TransferHandler;

/**
 * @see     com.affymetrix.genometryImpl.BioSeq
 * @see     com.affymetrix.genoviz.util.ComponentPagePrinter
 */

final public class ProtAnnotMain implements WindowListener {
	public static final ResourceBundle BUNDLE = ResourceBundle.getBundle("protannot");
	public static final String APP_NAME = BUNDLE.getString("appName");
	public static final String APP_NAME_SHORT = BUNDLE.getString("appNameShort");
	public static final String APP_VERSION      = BUNDLE.getString("appVersion");
	public static final String APP_VERSION_FULL = MessageFormat.format(
			BUNDLE.getString("appVersionFull"),
			APP_VERSION);
	public static final String USER_AGENT = MessageFormat.format(
			BUNDLE.getString("userAgent"),
			APP_NAME_SHORT,
			APP_VERSION_FULL,
			System.getProperty("os.name"),
			System.getProperty("os.version"),
			System.getProperty("os.arch"),
			Locale.getDefault().toString());

    // where the application is first invoked
    private static String user_dir = System.getProperty("user.dir");
    // used for choosing new files to load
    private JFileChooser chooser = null;
    // for printing
    private ComponentPagePrinter print_panel = null;
    // for choosing sample path from server
    private JFrame sampleChooser = null;
    // for choosing colors
    private JFrame colorChooser = null;
	// for adding server
	private JFrame addServer = null;
    // the JFrame containing all the widgets
    final private JFrame frm;
	// for show/hide hairline.
	private JCheckBox showhairline;
    // has NeoMaps and PropertySheet (JTable)
    private GenomeView gview;
    // is populated from prefs_file
    private Map<String,Color> prefs_hash;
    // for storing user prefrences
    private Preferences prefs;
    // width of the user's screen
    private Dimension screen;
		
	//To enforce singleton pattern
	private static ProtAnnotMain singleton;

	AbstractAction server_load_action = Actions.getLoadFromServerAction();
	
    private final static boolean testmode = false;
	private static final boolean DEBUG = false;
	private static final Image imageIcon = getIcon();
	private final TransferHandler fdh = new FileDropHandler(){

		@Override
		public void openFileAction(File f) {
			load(f);
		}

		@Override
		public void openURLAction(String url) {
			load(url);
		}
	};

    private enum Arguments {
        SERVER,
        FILENAME;

        public static Arguments getValue(String s)
        {
           if(s.equalsIgnoreCase("-s"))
               return SERVER;
           else if(s.equalsIgnoreCase("-f"))
               return FILENAME;

           return null;
        }

    };
    private final Map<Arguments,String> ArgumentValues = new HashMap<Arguments,String>();

    public static void main(String[] args) {
        singleton = ProtAnnotMain.getInstance();
		ConsoleView.init(APP_NAME);

		System.out.println("Starting \"" + APP_NAME + " " + APP_VERSION_FULL + "\"");
		System.out.println("UserAgent: " + USER_AGENT);
		System.out.println("Java version: " + System.getProperty("java.version") + " from " + System.getProperty("java.vendor"));
		Runtime runtime = Runtime.getRuntime();
		System.out.println("Locale: " + Locale.getDefault());
		System.out.println("System memory: " + runtime.maxMemory() / 1024);
		if (args != null && args.length > 0) {
			System.out.print("arguments: ");
			for (String arg : args) {
				System.out.print(" " + arg);
			}
			System.out.println();
		}else
			System.out.print("No startup arguments");

		System.out.println();

        singleton.parseArguments(args);
        singleton.loadPrefs();
        singleton.start();
    }

	static ProtAnnotMain getInstance(){
		if(singleton == null){
			singleton = new ProtAnnotMain();
		}
		return singleton;
	}
	
	/** Returns the icon stored in the jar path.
	 *  It is expected to be at com.affymetrix.igb.igb.gif.
	 *  @return null if the image path is not found or can't be opened.
	 */
	private static Image getIcon() {
		Image icon = null;
		try {
			URL url = ProtAnnotMain.class.getResource("protannot.gif");
			if (url != null) {
				icon = Toolkit.getDefaultToolkit().getImage(url);
			}
		} catch (Exception e) {
			e.printStackTrace();
			// It isn't a big deal if we can't find the icon, just return null
		}
		return icon;
	}
    /**
     * Unloads everything from GnomeView if unable to read the selected path.
     */
    private void no_data() {
        frm.setTitle(" ProtAnnot");
        gview.setTitle("");
        gview.no_data();
		showhairline.setEnabled(false);
    }

    /**
     * Loads preferences from the path.
     *
     * @return Returns a Map with name as key and Color as a value.
     */
    private Map<String,Color> loadPrefs() {
        Map<String,Color> phash = new HashMap<String,Color>();

        prefs = Preferences.userNodeForPackage(ProtAnnotMain.class);

        try
        {
			for(Entry<String,Color> color_pref : GenomeView.COLORS.defaultColorList().entrySet()){
				phash.put(color_pref.getKey(), new Color(prefs.getInt(color_pref.getKey(), color_pref.getValue().getRGB())));
			}
            updatePrefs(phash);
        } catch (Exception ex) {
            Logger.getLogger(ProtAnnotMain.class.getName()).log(Level.SEVERE, null, ex);
        }

        prefs_hash = phash;
        return prefs_hash;
    }

	private ProtAnnotMain(){
		frm = new JFrame(APP_NAME);
	}
    /**
     * Setup the outer frame.
     * @param   args    - optional path name as a parameter.
     */
    private void start() {
		if ("Mac OS X".equals(System.getProperty("os.name"))) {
			MacIntegration mi = MacIntegration.getInstance();
			if (imageIcon != null) {
				mi.setDockIconImage(imageIcon);
			}
		}
		frm.setTransferHandler(fdh);
		frm.setIconImage(imageIcon);
        screen = frm.getToolkit().getScreenSize();
        int frm_width = (int) (screen.width * .8f);
        int frm_height = (int) (screen.height * .8f);
        frm.setSize(frm_width, frm_height);
        frm.setLocation((int) (screen.width * .1f), (int) (screen.height * .05f));
        setUpPanels();
        setUpMenus();
        setupColorChooser();
		setupAddServer();
        if(getArgumentValue(Arguments.SERVER)!=null)
            setupSamplesFromServer();

        frm.addWindowListener(this);
        frm.setVisible(true);
		String file = getArgumentValue(Arguments.FILENAME);
        if(file != null)
		{
			if(isServer(file))
				load(file);
			else if(getArgumentValue(Arguments.SERVER)!=null)
				load(getArgumentValue(Arguments.SERVER) + file);
			else
				load(new File(file));
		}
    }

    /**
     * One of part in setting up the outer frame. Sets up panels.
     */
    private void setUpPanels() {
        Container cpane = frm.getContentPane();
        cpane.setLayout(new BorderLayout());
        gview = new GenomeView(prefs_hash);
        cpane.add("Center", gview);
        print_panel = new ComponentPagePrinter(gview);
    }

    /**
     * One of part in setting up the outer frame. Sets up menus.
     */
    private void setUpMenus() {
        JMenuBar mbar = MenuUtil.getMainMenuBar();
        JMenu menu = MenuUtil.getMenu(BUNDLE.getString("fileMenu"));
		menu.setMnemonic(BUNDLE.getString("fileMenuMnemonic").charAt(0));
        addFileActions(menu);

		menu = MenuUtil.getMenu(BUNDLE.getString("editMenu"));
		menu.setMnemonic(BUNDLE.getString("editMenuMnemonic").charAt(0));
        addEditActions(menu);

        menu = MenuUtil.getMenu(BUNDLE.getString("viewMenu"));
		menu.setMnemonic(BUNDLE.getString("viewMenuMnemonic").charAt(0));
        addViewActions(menu);

		menu = MenuUtil.getMenu(BUNDLE.getString("helpMenu"));
		menu.setMnemonic(BUNDLE.getString("helpMenuMnemonic").charAt(0));
        addHelpActions(menu);

        frm.setJMenuBar(mbar);
		
        if(testmode)
        {
            addQuickLaunch(mbar);
        }
    }

    /**
     * Action perfomed when a path is seleced in the path browser. Calls up load(name) to load the path.
     */
    void doLoadFile() {
        if (this.chooser == null) {
            this.chooser = new JFileChooser(user_dir);
        }
        int option = this.chooser.showOpenDialog(frm);
        if (option == JFileChooser.APPROVE_OPTION) {
            File cfil = this.chooser.getSelectedFile();
            load(cfil);
        }
    }

    /**
     * Component Listener implementation
     */
    public void windowActivated(WindowEvent evt) {
    }

    /** Component Listener implementation */
    public void windowDeactivated(WindowEvent evt) {
    }

    /** Component Listener implementation */
    public void windowDeiconified(WindowEvent evt) {
    }

    /** Component Listener implementation */
    public void windowIconified(WindowEvent evt) {
    }

    /** Component Listener implementation */
    public void windowOpened(WindowEvent evt) {
    }

    /** Close everything and exit upon closing the window */
    public void windowClosing(WindowEvent evt) {
       if (evt.getSource() == frm) {
			updatePrefs(gview.getColorPrefs());

			JFrame frame = (JFrame) evt.getComponent();

			String message = "Do you really want to exit?";
			if (confirmPanel(message)) {
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			} else {
				frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			}
		}
    }

	public void windowClosed(WindowEvent evt) {
		
	}
    /**
     * Adds view_menu item to View view_menu..
     * @param   view_menu    Menu name to which submenus should be added.
     */
    private void addViewActions(JMenu view_menu) {

		AbstractAction b_action = Actions.getOpenInBrowserAction();
		MenuUtil.addToMenu(view_menu, new JMenuItem(b_action));
        gview.popup.add(b_action);

		AbstractAction z_action = Actions.getZoomToFeatureAction();
		MenuUtil.addToMenu(view_menu, new JMenuItem(z_action));
		gview.popup.add(z_action);

		AbstractAction h_action = Actions.getToggleHairlineAction();
		MenuUtil.addToMenu(view_menu, new JCheckBoxMenuItem(h_action));
		gview.popup.add(new JCheckBoxMenuItem(h_action));

		AbstractAction hl_action = Actions.getToggleHairlineLabelAction();
		MenuUtil.addToMenu(view_menu, new JCheckBoxMenuItem(hl_action));
		gview.popup.add(new JCheckBoxMenuItem(hl_action));

		MenuUtil.addToMenu(view_menu, new JMenuItem(Actions.getOpenInNewWindow()));
    }

    /**
     * Adds menu item to File menu. Adds Load,print and quit to it.
     * @param   file_menu   Menu name to which submenus should be added.
     */
    private void addFileActions(final JMenu file_menu) {
        
        MenuUtil.addToMenu(file_menu, new JMenuItem(Actions.getLoadAction()));
		MenuUtil.addToMenu(file_menu, new JMenuItem(Actions.getAddServerAction()));
		
	
		if(getArgumentValue(Arguments.SERVER)==null)
			server_load_action.setEnabled(false);
		else
			server_load_action.setEnabled(true);
		
		MenuUtil.addToMenu(file_menu, new JMenuItem(server_load_action));

        MenuUtil.addToMenu(file_menu, new JMenuItem(Actions.getPrintAction()));
		MenuUtil.addToMenu(file_menu, new JMenuItem(Actions.getExportAction()));
		MenuUtil.addToMenu(file_menu, new JMenuItem(Actions.getPreferencesAction()));
		MenuUtil.addToMenu(file_menu, new JMenuItem(Actions.getExitAction()));

    }

	private void addEditActions(final JMenu edit_menu){
		AbstractAction c_action = Actions.getCopyAction();
		MenuUtil.addToMenu(edit_menu, new JMenuItem(c_action));
		gview.popup.add(c_action);
	}

	/**
	 * Adds help menu item to Help help_menu.
	 * @param help_menu Menu name to which submenus should be added.
	 */
	private void addHelpActions(final JMenu help_menu){
        MenuUtil.addToMenu(help_menu, new JMenuItem(Actions.getAboutAction()));
		MenuUtil.addToMenu(help_menu, new JMenuItem(Actions.getReportBugAction()));
		MenuUtil.addToMenu(help_menu, new JMenuItem(Actions.getFeatureAction()));
		MenuUtil.addToMenu(help_menu, new JMenuItem(Actions.getShowConsoleAction()));
	}

	void colorChooser(){
		if(colorChooser == null){
			setupColorChooser();
		}
		colorChooser.setVisible(true);
	}

	void export() {
		final ExportDialog export = new ExportDialog();
		export.setIcon(new ImageIcon(imageIcon));
		try {
			export.showExportDialog(gview, "Export view as ...", gview, "export");
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	void print() {
		try {
			print_panel.print();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	void addServer(){
		if(addServer == null){
			setupAddServer();
		}
		addServer.setVisible(true);
	}

	void loadFromServer(){
		sampleChooser.setVisible(true);
	}
	
	void close() {
		
	}

	GenomeView getGenomeView(){
		return gview;
	}
	
	private void setupAddServer(){
		addServer = new JFrame("Add Server Address ...");
        addServer.setSize(250, 85);
        addServer.setLocation((int) (screen.width * .4f), (int) (screen.height * .15f));
        addServer.setLayout(new BoxLayout(addServer.getContentPane(), BoxLayout.Y_AXIS));

		final JTextField address = new JTextField();
		address.setSize(225, 40);
		JPanel buttonpanel = new JPanel();
        buttonpanel.setLayout(new FlowLayout());
        JButton add    = new JButton(" Add  ");
        JButton cancel = new JButton("Cancel");
        buttonpanel.add(add);
        buttonpanel.add(cancel);

        add.addActionListener(new ActionListener(){

                public void actionPerformed(ActionEvent e) {
					checkArguments("",address.getText());
					setupSamplesFromServer();
					server_load_action.setEnabled(true);
					addServer.setVisible(false);
					loadFromServer();
                }
            });

        cancel.addActionListener(new ActionListener(){

                public void actionPerformed(ActionEvent e) {
					addServer.setVisible(false);
                }
            });
		addServer.add(address);
		addServer.add(buttonpanel);
	}

    /**
     * In testmode, adds 3 buttons in menubar to load files.
     * @param   mbar
     */
    private void addQuickLaunch(JMenuBar mbar) {
        JLabel test = new JLabel("                || Chose Test file :-");
        JButton low = new JButton("OLD");
        JButton med = new JButton("NEW");
        JButton high = new JButton("Negative");
        mbar.add(test);
        mbar.add(low);
        mbar.add(med);
        mbar.add(high);

        low.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    load(new File(user_dir + "/samples/ABCB4.paxml"));
                }
            });

        med.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    load(new File(user_dir + "/pyResample/temp/aafile.paxml"));
                }
            });

        high.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    load(new File(user_dir +"/pyResample/temp/aaafile.paxml"));
                }
            });
    }


    /**
     * Sets up interface to select path from the server.
     */
    private void setupSamplesFromServer()
    {

        sampleChooser = new JFrame("Choose a file ...");
        sampleChooser.setSize(250, 250);
        sampleChooser.setLocation((int) (screen.width * .4f), (int) (screen.height * .15f));
        sampleChooser.setLayout(new BoxLayout(sampleChooser.getContentPane(), BoxLayout.Y_AXIS));

        String files[] = getSamplesFromServer();
        final JList filesList = new JList(files);
        filesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scrollPane = new JScrollPane(filesList);

        sampleChooser.add(scrollPane);

        JPanel buttonpanel = new JPanel();
        buttonpanel.setLayout(new FlowLayout());
        JButton open   = new JButton(" Open ");
        JButton cancel = new JButton("Cancel");
        buttonpanel.add(open);
        buttonpanel.add(cancel);

        open.addActionListener(new ActionListener(){

                public void actionPerformed(ActionEvent e) {
                    load(getArgumentValue(Arguments.SERVER) + filesList.getSelectedValue().toString());
                    sampleChooser.setVisible(false);
                }
            });

        cancel.addActionListener(new ActionListener(){

                public void actionPerformed(ActionEvent e) {
                    sampleChooser.setVisible(false);
                }
            });

        if(files.length <= 0)
            open.setEnabled(false);

        sampleChooser.add(buttonpanel);

    }


	private void load(String path){
		try {
			load(LocalUrlCacher.getInputStream(path), path);
		} catch (IOException e) {
			Reporter.report("Couldn't read file: " + e.getMessage(), e, false, false, true);
		}
	}

	/**
	 * Loads the path selected in the path browser.
	 * @param   seqfile - Name of path to be loaded
	 */
	private void load(File seqfile) {

		FileInputStream fistr = null;
		try {
			fistr = new FileInputStream(seqfile);
			load(fistr, seqfile.getName());
		} catch (Exception e) {
			Reporter.report("Couldn't read file: " + e.getMessage(), e, false, false, true);
		} finally {
			GeneralUtils.safeClose(fistr);
		}
	}

	private void load(InputStream fistr, String filename) {
		BufferedInputStream bistr = null;
		try {
			bistr = new BufferedInputStream(GeneralUtils.unzipStream(fistr, filename, new StringBuffer()));
			NormalizeXmlStrand nxs = new NormalizeXmlStrand(bistr);
			if (DEBUG) {
				NormalizeXmlStrand.outputXMLToScreen(nxs.doc);
			}
			Xml2GenometryParser parser = new Xml2GenometryParser();
			BioSeq genome_seq = parser.parse(nxs.doc);
			gview.setTitle("viewing file: " + filename + "\t genome version: " + genome_seq.getVersion() + "\t sequence: " + genome_seq.getID());
			gview.setBioSeq(genome_seq, true);
			frm.setTitle(" ProtAnnot: " + filename + "\t version: " + genome_seq.getVersion() + "\t id: " + genome_seq.getID());
		} catch (Exception ex) {
			Reporter.report("Couldn't read file: " + filename + "\n"
					+ "Error : " + ex.getMessage(),
					ex, false, false, true);
			no_data();
		} finally {
			GeneralUtils.safeClose(bistr);
		}
	}



    /**
     * Loads all samples if server name is provided.
     */
    private String[] getSamplesFromServer()
    {
        List<String> ret = new ArrayList<String>();
        String page = loadPage();
        Pattern pattern = Pattern.compile("<a.+href=\"((.+paxml)|(.+paxml.*))\"");
        Matcher matcher = pattern.matcher(page);
        while (matcher.find()) {
            ret.add(matcher.group(1));
        }
        return ret.toArray(new String[0]);
    }

	/**
	 * Shows a panel asking for the user to confirm something.
	 *
	 * @param message the message String to display to the user
	 * @return true if the user confirms, else false.
	 */
	public static boolean confirmPanel(String message) {
		ProtAnnotMain pa = getInstance();
		JFrame frame = (pa == null) ? null : pa.getFrame();
		return (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
				frame, message, "Confirm", JOptionPane.YES_NO_OPTION));
	}

    /**
     * Parses command line argument and adds valid arguments to the argument dictionary.
     * @param   args    Command line arguments
     */
    private void parseArguments(String[] args) {

       if(args.length  == 1)
	   {
		   checkArguments("",args[0]);
       }else if(args.length%2 == 0)
       {
		   for(int i=0; i<args.length; i += 2)
			   checkArguments(args[i],args[i+1]);
       }
       else
           outputErrorMessage("Invalid number of arguments");
    }

	/**
	 * Check arguments and add to Dictionary. If arguments are invalid showhairline error message.
	 * @param	arg			Argument type.
	 * @param	argValue	Argument Value.
	 */
	private boolean checkArguments(String arg, String argValue)
	{
		arg = arg.toLowerCase();
		
		//Check if it server's argument.
		if ("-s".equals(arg)) {
			//Check if server name starts with http:/ or https:/
			//eg http:// or https://
			if (isServer(argValue)) {
				//Check if it is server name.
				//eg http://protannot.bioviz.org/samples/
				if(argValue.endsWith("/")) {
					return addToArgumentDictionary(new String[]{"-s", argValue});
				} else {
					//Check if it is path on a server. Then add path name and server name.
					//eg https://protannot.bioviz.org/samples/ABCB1.paxml
					String file = argValue.substring(argValue.lastIndexOf('/') + 1);
					String server = argValue.replace(file, "");

					//Check path name is valid.
					if(file.contains(".")){
						checkArguments("-f", file);
						return addToArgumentDictionary(new String[]{"-s", server});
					}
					//If path name is invalid then should be server name without '/' at the end.
					//eg http://protannot.bioviz.org/samples
					else
						return addToArgumentDictionary(new String[]{"-s", argValue+"/"});
				} 
			} else
				return outputErrorMessage("Invalid server name: Server name should start with http or https. " +
						"\n eg. http://protannot.bioviz.org/samples/");
			
		} else if("-f".equals(arg)){
			if (GeneralUtils.getUnzippedName(argValue).endsWith(".paxml"))
				return addToArgumentDictionary(new String[]{"-f", argValue});
			else
				return outputErrorMessage("Invalid file name: File name should end with .paxml" +
						"\n eg. /user/home/protannot/samples/ABCD.paxml OR " +
						"\n eg. https://protannot.bioviz.org/samples/ABCD.paxml");
			
		} else if(arg.length()==0){
			if(isServer(argValue))
				checkArguments("-s",argValue);
			else
				checkArguments("-f",argValue);
		}
		
		return false;
	}

	/**
	 * Create a dialog box to show hairline error message.
	 * @param	error	Error message to be displayed.
	 */
	private static boolean outputErrorMessage(String error){
		JOptionPane.showMessageDialog(new JFrame(), error, "", JOptionPane.ERROR_MESSAGE);
		return false;
	}
	
    /**
     * Adds argument to dictionary
     * @param   args    Argument pair to be inserted in dictionary.
     */
    private boolean addToArgumentDictionary(String[] args) {

       for(int i=0; i<args.length; i+=2)
       {
            if(Arguments.getValue(args[i])!=null)
                ArgumentValues.put(Arguments.getValue(args[i]), args[i+1]);
       }

	   return true;
    }

    /**
     * Gets value for the given argument.
     * @param   arg     Argument name
     * @return  String  Returns argument value
     */
    private String getArgumentValue(Arguments arg)
    {
        return ArgumentValues.get(arg);
    }

    /**
     * Loads the sample directory from the server
     * @return  String  Returns directory listing in string format.
     */
    private String loadPage() {
		try {
			StringBuilder output = new StringBuilder(2000);
			BufferedReader buff = null;
			try {
				URL url = new URL(getArgumentValue(Arguments.SERVER));
				buff = new BufferedReader(new InputStreamReader(LocalUrlCacher.getInputStream(url)));
				boolean eof = false;
				while (!eof) {
					String line = buff.readLine();
					if (line == null) {
						eof = true;
					} else {
						output.append(line).append("\n");
					}
				}
			} catch (IOException e) {
				System.out.println("Error -- " + e.toString());
			} finally {
				GeneralUtils.safeClose(buff);
			}
			return output.toString();
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
    }

    /**
     * Updates users color preferences
     * @param   hash    Map containing color name and color value pairs.
     */
    private void updatePrefs(Map<String,Color> hash)
    {
        prefs = Preferences.userNodeForPackage(org.bioviz.protannot.ProtAnnotMain.class);
        
        for(Entry<String, Color> entry : hash.entrySet()) {
            prefs.putInt(entry.getKey(), entry.getValue().getRGB());
        }
    }

	/**
	 * 
	 * @return Returns protannot frame.
	 */
	JFrame getFrame(){
		return frm;
	}

    /**
     * Sets up interface to choose color preferences.
     */
    private void setupColorChooser()
    {
        colorChooser = new JFrame("Color Preference");
		colorChooser.setIconImage(new ImageIcon(imageIcon).getImage());
        colorChooser.setSize(375, 175);
        colorChooser.setLocation((int) (screen.width * .4f), (int) (screen.height * .15f));
        colorChooser.setLayout(new BorderLayout());

        final ColorTableModel model = new ColorTableModel();
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setLayout(new BorderLayout());
        table.setDefaultRenderer(Color.class, new ColorTableCellRenderer());
        table.setDefaultEditor(Color.class, new ColorTableCellEditor());
        table.setFillsViewportHeight(true);

        JPanel buttonpanel = new JPanel();
        buttonpanel.setLayout(new GridLayout(1,4));

        JButton defaults = new JButton("Defaults");
        JButton apply  = new JButton("Apply ");
        JButton save   = new JButton(" Save ");
        JButton cancel = new JButton("Cancel");

        buttonpanel.add(defaults);
        buttonpanel.add(apply);
        buttonpanel.add(save);
        buttonpanel.add(cancel);


        apply.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                gview.tempChangePreference(model.colorList());
            }
        });

        save.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                gview.changePreference(model.colorList());
                model.setValues(gview.getColorPrefs());
                colorChooser.setVisible(false);
            }
        });

        cancel.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                gview.cancelChangePrefernce();
                model.setValues(gview.getColorPrefs());
                colorChooser.setVisible(false);
            }
        });

        defaults.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                gview.changePreference(GenomeView.COLORS.defaultColorList());
                model.setValues(gview.getColorPrefs());
                colorChooser.setVisible(false);
            }
        });

        colorChooser.add("Center",table);
        colorChooser.add("South",buttonpanel);

    }

    /**
     * Returns color preferences in two dimensional object.
     * @param prefs_col     Map<String,Color>
     * @return  Object[][]  Returns color preferences in two dimensional object.
     */
    private static Object[][] getData(Map<String, Color> prefs_col) {
		Object[][] colordata = new Object[prefs_col.size()][2];
		int i = 0;
		for (Map.Entry<String,Color> entry: prefs_col.entrySet()) {
			colordata[i++] = new Object[]{entry.getKey(), entry.getValue()};
		}
		return colordata;

	}

	private static boolean isServer(String string){
		return (string.startsWith("http:/") || string.startsWith("https:/"));
	}
	
    /**
     * Table model for color preferences
     */
    private class ColorTableModel extends AbstractTableModel{

            final String[] col_headings = {"Name","Color"};
            Object[][] data;

            /**
             * Initialized data with default color values
             */
            ColorTableModel()
            {
                setValues(gview.getColorPrefs());
            }

            /**
             * Gets number of rows.
             * @return  int     Returns number of rows.
             */
            public int getRowCount() {
                return data.length;
            }

            /**
             * Gets number of columns.
             * @return  int     Returns number of columns.
             */
            public int getColumnCount() {
                return col_headings.length;
            }

            /**
             * Returns value at rowIndex and columnIndex.
             * @param rowIndex      Row number
             * @param columnIndex   Column number
             * @return  Object      Returns value at rowIndex and columnIndex.
             */
            public Object getValueAt(int rowIndex, int columnIndex) {
                return data[rowIndex][columnIndex];
            }

            /**
             * Tells if the cell is editable at row and column.
             * @param row   Row number
             * @param col   Column number
             * @return  boolean     true if cell is editable else false
             */
            @Override
            public boolean isCellEditable(int row, int col) {
				return col > 0;
            }

            /**
             * Set cell value at row and col.
             * @param value     Value to be set
             * @param row       Row number
             * @param col       Column number
             */
            @Override
            public void setValueAt(Object value, int row, int col) {
                data[row][col] = value;
                fireTableCellUpdated(row, col);
            }

            /**
             * Returns Class of given column number c.
             * @param c     Column number
             * @return  Class    Class of column c.
             */
            @Override
            public Class getColumnClass(int c) {
                return getValueAt(0, c).getClass();
            }

            /**
             * Returns table value in form of Map
             * @return  Map<String,Color> Map of values in rows.
             */
            public Map<String,Color> colorList()
            {
                Map<String,Color> colorhash = new HashMap<String,Color>();

                for(int i=0; i<data.length; i++)
                    colorhash.put((String)data[i][0], (Color)data[i][1]);

                return colorhash;
            }

            /**
             * Sets all values of the data
             * @param prefs_cols
             */
            public void setValues(Map<String,Color> prefs_cols)
            {
                data = getData(prefs_cols);
            }

        };

}
