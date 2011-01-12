/**
 *   Copyright (c) 2001-2007 Affymetrix, Inc.
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
package com.affymetrix.igb;

import com.affymetrix.genometryImpl.util.ConsoleView;
import com.affymetrix.genometryImpl.util.MenuUtil;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

import java.io.*;
import java.net.*;
import java.util.*;

import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.DisplayUtils;

import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.style.DefaultStateProvider;
import com.affymetrix.genometryImpl.style.StateProvider;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.bookmarks.Bookmark;
import com.affymetrix.igb.bookmarks.BookmarkController;
import com.affymetrix.igb.menuitem.*;
import com.affymetrix.igb.view.*;
import com.affymetrix.igb.parsers.XmlPrefsParser;
import com.affymetrix.igb.prefs.*;
import com.affymetrix.igb.bookmarks.SimpleBookmarkServer;
import com.affymetrix.igb.general.Persistence;
import com.affymetrix.igb.tiers.AffyTieredMap.ActionToggler;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.tiers.IGBStateProvider;
import com.affymetrix.igb.util.IGBAuthenticator;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.action.*;
import com.affymetrix.igb.util.ScriptFileLoader;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.util.aligner.AlignerOutputView;
import com.affymetrix.igb.view.external.ExternalViewer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.APP_VERSION;
import static com.affymetrix.igb.IGBConstants.APP_VERSION_FULL;
import static com.affymetrix.igb.IGBConstants.USER_AGENT;

/**
 *  Main class for the Integrated Genome Browser (IGB, pronounced ig-bee).
 *
 * @version $Id: IGB.java 7054 2010-10-20 14:14:09Z hiralv $
 */
public final class IGB extends Application
				implements ActionListener, GroupSelectionListener, SeqSelectionListener {

	static IGB singleton_igb;
	private static final String TABBED_PANES_TITLE = "Tabbed Panes";
	private static final Map<Component, Frame> comp2window = new HashMap<Component, Frame>();
	private final Map<Component, PluginInfo> comp2plugin = new HashMap<Component, PluginInfo>();
	private final Map<Component, JCheckBoxMenuItem> comp2menu_item = new HashMap<Component, JCheckBoxMenuItem>();
	private JFrame frm;
	private JMenuBar mbar;
	private JMenu file_menu;
	private JMenu export_to_file_menu;
	private JMenu view_menu;
	private JMenu edit_menu;
	private JMenu bookmark_menu;
	private JMenu tools_menu;
	private JMenu help_menu;
	private JTabbedPane tab_pane;
	private JTabbedPane left_tab_pane;
	private JSplitPane splitpane;
	private JSplitPane left_splitpane;
//	private JList metagenomics_list; //BFTAG changed
	private SeqInfoView seqInfo;
	private DefaultListModel metagenomics_list_model;
	public BookMarkAction bmark_action; // needs to be public for the BookmarkManagerView plugin
	private JCheckBoxMenuItem toggle_edge_matching_item;
	private JMenuItem move_tab_to_window_item;
	private JMenuItem move_tabbed_panel_to_window_item;
	private SeqMapView map_view;
	public DataLoadView data_load_view = null;
	private final List<PluginInfo> plugins_info = new ArrayList<PluginInfo>(16);
	private final List<Object> plugins = new ArrayList<Object>(16);
	private FileTracker load_directory = FileTracker.DATA_DIR_TRACKER;
	private FileTracker genome_directory = FileTracker.GENOME_DIR_TRACKER;
	private AnnotatedSeqGroup prev_selected_group = null;
	private BioSeq prev_selected_seq = null;
	public static volatile String commandLineBatchFileStr = null;	// Used to run batch file actions if passed via command-line
	private JTabbedPane neoMapPane;
	private BarGraphMap barGraph;
	private JToggleButton fast_dir_swap_menu;//MPTAG added
	public static boolean dir_swap_state = false;
	
	/**
	 * Start the program.
	 */
	public static void main(String[] args) {
		try {

			// Configure HTTP User agent
			System.setProperty("http.agent", USER_AGENT);

			// Turn on anti-aliased fonts. (Ignored prior to JDK1.5)
			System.setProperty("swing.aatext", "true");

			// Letting the look-and-feel determine the window decorations would
			// allow exporting the whole frame, including decorations, to an eps file.
			// But it also may take away some things, like resizing buttons, that the
			// user is used to in their operating system, so leave as false.
			JFrame.setDefaultLookAndFeelDecorated(false);
			


			// if this is != null, then the user-requested l-and-f has already been applied
			if (System.getProperty("swing.defaultlaf") == null) {
				//String os = System.getProperty("os.name");
				//if (os != null && os.toLowerCase().contains("windows")) {
					try {
						// It this is Windows, then use the Windows look and feel.
						Class<?> cl = Class.forName("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
						LookAndFeel look_and_feel = (LookAndFeel) cl.newInstance();

						if (look_and_feel.isSupportedLookAndFeel()) {
							UIManager.setLookAndFeel(look_and_feel);
						}
					} catch (Exception ulfe) {
						// Windows look and feel is only supported on Windows, and only in
						// some version of the jre.  That is perfectly ok.
					}
				//}
			}
			


			// Initialize the ConsoleView right off, so that ALL output will
			// be captured there.
			ConsoleView.init(APP_NAME);

			System.out.println("Starting \"" + APP_NAME + " " + APP_VERSION_FULL + "\"");
			System.out.println("UserAgent: " + USER_AGENT);
			System.out.println("Java version: " + System.getProperty("java.version") + " from " + System.getProperty("java.vendor"));
			Runtime runtime = Runtime.getRuntime();
			System.out.println("Locale: " + Locale.getDefault());
			System.out.println("System memory: " + runtime.maxMemory() / 1024);
			if (args != null) {
				System.out.print("arguments: ");
				for (String arg : args) {
					System.out.print(" " + arg);
				}
				System.out.println();
			}

			System.out.println();

			String offline = get_arg("-offline", args);
			if (offline != null) {
				LocalUrlCacher.setOffLine("true".equals(offline));
			}

			singleton_igb = new IGB();

			PrefsLoader.loadIGBPrefs(args); // force loading of prefs

			singleton_igb.init(args);

			commandLineBatchFileStr = ScriptFileLoader.getScriptFileStr(args);	// potentially used in GeneralLoadView

			goToBookmark(args);

		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	private static void goToBookmark(String[] args) {
		// If the command line contains a parameter "-href http://..." where
		// the URL is a valid IGB control bookmark, then go to that bookmark.
		final String url = get_arg("-href", args);
		if (url != null && url.length() > 0) {
			try {
				final Bookmark bm = new Bookmark(null, url);
				if (bm.isUnibrowControl()) {
					SwingUtilities.invokeLater(new Runnable() {

						public void run() {
							System.out.println("Loading bookmark: " + url);
							BookmarkController.viewBookmark(singleton_igb, bm);
						}
					});
				} else {
					System.out.println("ERROR: URL given with -href argument is not a valid bookmark: \n" + url);
				}
			} catch (MalformedURLException mue) {
				mue.printStackTrace(System.err);
			}
		}
	}

	public SeqMapView getMapView() {
		return map_view;
	}

	public JFrame getFrame() {
		return frm;
	}

	private void startControlServer() {
		// Use the Swing Thread to start a non-Swing thread
		// that will start the control server.
		// Thus the control server will be started only after current GUI stuff is finished,
		// but starting it won't cause the GUI to hang.

		Runnable r = new Runnable() {

			public void run() {
				new SimpleBookmarkServer(IGB.this);
			}
		};

		final Thread t = new Thread(r);

		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				t.start();
			}
		});
	}

	/**
	 * Returns the value of the argument indicated by label.
	 * If arguments are
	 *   "-flag_2 -foo bar", then get_arg("foo", args)
	 * returns "bar", get_arg("flag_2") returns a non-null string,
	 * and get_arg("flag_5") returns null.
	 */
	public static String get_arg(String label, String[] args) {
		String to_return = null;
		boolean got_it = false;
		if (label != null && args != null) {
			for (String item : args) {
				if (got_it) {
					to_return = item;
					break;
				}
				if (item.equals(label)) {
					got_it = true;
				}
			}
		}
		if (got_it && to_return == null) {
			to_return = "true";
		}
		return to_return;
	}

	private static void loadSynonyms(String file, SynonymLookup lookup) {
		InputStream istr = null;
		try {
			istr = IGB.class.getResourceAsStream(file);
			lookup.loadSynonyms(IGB.class.getResourceAsStream(file), true);
		} catch (IOException ex) {
			Logger.getLogger(IGB.class.getName()).log(Level.FINE, "Problem loading default synonyms file " + file, ex);
		} finally {
			GeneralUtils.safeClose(istr);
		}
	}

	private void init(String[] args) {
		loadSynonyms("/synonyms.txt", SynonymLookup.getDefaultLookup());
		loadSynonyms("/chromosomes.txt", SynonymLookup.getChromosomeLookup());

		if ("Mac OS X".equals(System.getProperty("os.name"))) {
			MacIntegration mi = MacIntegration.getInstance();
			if (this.getIcon() != null) {
				mi.setDockIconImage(this.getIcon());
			}
		}

		frm = new JFrame(APP_NAME + " " + APP_VERSION);

		// when HTTP authentication is needed, getPasswordAuthentication will
		//    be called on the authenticator set as the default
		Authenticator.setDefault(new IGBAuthenticator(frm));
		
		
		// force loading of prefs if hasn't happened yet
		// usually since IGB.main() is called first, prefs will have already been loaded
		//   via loadIGBPrefs() call in main().  But if for some reason an IGB instance
		//   is created without call to main(), will force loading of prefs here...
		PrefsLoader.loadIGBPrefs(args);

		StateProvider stateProvider = new IGBStateProvider();
		DefaultStateProvider.setGlobalStateProvider(stateProvider);


		frm.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		Image icon = getIcon();
		if (icon != null) {
			frm.setIconImage(icon);
		}

		mbar = MenuUtil.getMainMenuBar();
		frm.setJMenuBar(mbar);

		GenometryModel gmodel = GenometryModel.getGenometryModel();
		gmodel.addGroupSelectionListener(this);
		gmodel.addSeqSelectionListener(this);
		// WARNING!!  IGB _MUST_ be added as group and seq selection listener to model _BEFORE_ map_view is,
		//    otherwise assumptions for persisting group / seq / span prefs are not valid!

		map_view = new SeqMapView(true);
		gmodel.addSeqSelectionListener(map_view);
		gmodel.addGroupSelectionListener(map_view);
		gmodel.addSymSelectionListener(map_view);

		file_menu = MenuUtil.getMenu(BUNDLE.getString("fileMenu"));
		file_menu.setMnemonic(BUNDLE.getString("fileMenuMnemonic").charAt(0));

		edit_menu = MenuUtil.getMenu(BUNDLE.getString("editMenu"));
		edit_menu.setMnemonic(BUNDLE.getString("editMenuMnemonic").charAt(0));

		view_menu = MenuUtil.getMenu(BUNDLE.getString("viewMenu"));
		view_menu.setMnemonic(BUNDLE.getString("viewMenuMnemonic").charAt(0));

		bookmark_menu = MenuUtil.getMenu(BUNDLE.getString("bookmarksMenu"));
		bookmark_menu.setMnemonic(BUNDLE.getString("bookmarksMenuMnemonic").charAt(0));

		tools_menu = MenuUtil.getMenu(BUNDLE.getString("toolsMenu"));
		tools_menu.setMnemonic(BUNDLE.getString("toolsMenuMnemonic").charAt(0));

		help_menu = MenuUtil.getMenu(BUNDLE.getString("helpMenu"));
		help_menu.setMnemonic(BUNDLE.getString("helpMenuMnemonic").charAt(0));

		bmark_action = new BookMarkAction(this, map_view, bookmark_menu);

		export_to_file_menu = new JMenu(BUNDLE.getString("export"));
		export_to_file_menu.setMnemonic('T');

		toggle_edge_matching_item = new JCheckBoxMenuItem(BUNDLE.getString("toggleEdgeMatching"));
		toggle_edge_matching_item.setMnemonic(KeyEvent.VK_M);
		toggle_edge_matching_item.setState(map_view.getEdgeMatching());
		move_tab_to_window_item = new JMenuItem(BUNDLE.getString("openCurrentTabInNewWindow"), KeyEvent.VK_O);
		move_tabbed_panel_to_window_item = new JMenuItem(BUNDLE.getString("openTabbedPanesInNewWindow"), KeyEvent.VK_P);

		//MPTAG added
		mbar.add(new JLabel("                   "));
		int iconWidth = 70; int iconHeight = 30;
		ImageIcon io = new ImageIcon("./igb/resources/direction_not_selected.gif");
		io.setImage(io.getImage().getScaledInstance(iconWidth,iconHeight , Image.SCALE_SMOOTH));
		fast_dir_swap_menu = new JToggleButton(io, dir_swap_state);
		io = new ImageIcon("./igb/resources/direction_selected.gif");
		io.setImage(io.getImage().getScaledInstance(iconWidth, iconHeight, Image.SCALE_SMOOTH));
		fast_dir_swap_menu.setSelectedIcon(io);
		mbar.add(fast_dir_swap_menu);
		fast_dir_swap_menu.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				dir_swap_state = !dir_swap_state;
				fast_dir_swap_menu.setSelected(dir_swap_state);
				Application.getSingleton().getMapView().repaint();
			}
		});//MPTAG end

		fileMenu();

		editMenu();
		viewMenu();

				//at firtst only for tests

		neoMapPane = new JTabbedPane();
		neoMapPane.insertTab("Sequence Viewer", null, map_view, "Sequence Viewer", 0);

		neoMapPane.insertTab("Overview", null, BarGraphMap.getInstance(), "Overview", 1);
		GenometryModel.getGenometryModel().addSeqSelectionListener(BarGraphMap.getInstance());

		MenuUtil.addToMenu(tools_menu, new JMenuItem(WebLinksManagerView.getShowFrameAction()));
		MenuUtil.addToMenu(tools_menu, new JMenuItem(new ExportOverviewDiagramToCsvAction(frm, barGraph)));

		MenuUtil.addToMenu(help_menu, new JMenuItem(new AboutIGBAction()));
		MenuUtil.addToMenu(help_menu, new JMenuItem(new ForumHelpAction()));
		MenuUtil.addToMenu(help_menu, new JMenuItem(new ReportBugAction()));
		MenuUtil.addToMenu(help_menu, new JMenuItem(new RequestFeatureAction()));
		MenuUtil.addToMenu(help_menu, new JMenuItem(new DocumentationAction()));
		MenuUtil.addToMenu(help_menu, new JMenuItem(new ShowConsoleAction()));

		toggle_edge_matching_item.addActionListener(this);

		move_tab_to_window_item.addActionListener(this);
		move_tabbed_panel_to_window_item.addActionListener(this);

		Container cpane = frm.getContentPane();
		int table_height = 250;
		int fudge = 55;

		Rectangle frame_bounds = PreferenceUtils.retrieveWindowLocation("main window",
						new Rectangle(0, 0, 950, 600)); // 1.58 ratio -- near golden ratio and 1920/1200, which is native ratio for large widescreen LCDs.
		PreferenceUtils.setWindowSize(frm, frame_bounds);

		tab_pane = new JTabbedPane();

		cpane.setLayout(new BorderLayout());
		splitpane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		splitpane.setOneTouchExpandable(true);
		splitpane.setDividerSize(8);
		splitpane.setDividerLocation(frm.getHeight() - (table_height + fudge));

		splitpane.setTopComponent(neoMapPane);

		boolean tab_panel_in_a_window = (PreferenceUtils.getComponentState(TABBED_PANES_TITLE).equals(PreferenceUtils.COMPONENT_STATE_WINDOW));
		if (tab_panel_in_a_window) {
			openTabbedPanelInNewWindow(tab_pane);
		} else {
			splitpane.setBottomComponent(tab_pane);
		}

		// - this is example code for a vertical plugin sidebar -

		// generate tabbed pane for plugins
		left_tab_pane = new JTabbedPane();

		//BFTAG added
		seqInfo = new SeqInfoView();
//		seqInfo.refreshTable();
		left_tab_pane.addTab("Metagenomics", seqInfo);

		// add a splitter for the left hand side, choose an appropriate width
		left_splitpane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
		left_splitpane.setOneTouchExpandable(true);
		left_splitpane.setDividerLocation(180);

		// add new tabbed pane on left hand side, keep original contents on right
		left_splitpane.setLeftComponent(left_tab_pane);
		left_splitpane.setRightComponent(splitpane);

		// add the new split pane to the main windows
		cpane.add("Center", left_splitpane);

		// Using JTabbedPane.SCROLL_TAB_LAYOUT makes it impossible to add a
		// pop-up menu (or any other mouse listener) on the tab handles.
		// (A pop-up with "Open tab in a new window" would be nice.)
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4465870
		// http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4499556
		tab_pane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
		tab_pane.setMinimumSize(new Dimension(0, 0));

		cpane.add(status_bar, BorderLayout.SOUTH);

		// Show the frame before loading the plugins.  Thus any error panel
		// that is created by an exception during plugin set-up will appear
		// on top of the main frame, not hidden by it.

		frm.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(WindowEvent evt) {
				JFrame frame = (JFrame) evt.getComponent();
				boolean ask_before_exit = PreferenceUtils.getBooleanParam(PreferenceUtils.ASK_BEFORE_EXITING,
						PreferenceUtils.default_ask_before_exiting);
				String message = "Do you really want to exit?";

				if ((!ask_before_exit) || confirmPanel(message)) {
					if (bmark_action != null) {
						bmark_action.autoSaveBookmarks();
					}
					WebLink.autoSave();
					saveWindowLocations();
					Persistence.saveCurrentView(map_view);
					frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				} else {
					frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
				}
			}
		});
		frm.setVisible(true);

		plugins_info.add(new PluginInfo(DataLoadView.class.getName(), BUNDLE.getString("dataAccessTab"), true));
		plugins_info.add(new PluginInfo(PropertyView.class.getName(), BUNDLE.getString("selectionInfoTab"), true));
		plugins_info.add(new PluginInfo(SearchView.class.getName(), BUNDLE.getString("searchTab"), true));
		plugins_info.add(new PluginInfo(AltSpliceView.class.getName(), BUNDLE.getString("slicedViewTab"), true));
		plugins_info.add(new PluginInfo(SimpleGraphTab.class.getName(), BUNDLE.getString("graphAdjusterTab"), true));
		plugins_info.add(new PluginInfo(RestrictionControlView.class.getName(), BUNDLE.getString("restrictionSitesTab"), true));
		plugins_info.add(new PluginInfo(ExternalViewer.class.getName(), BUNDLE.getString("externalViewTab"), true));
		plugins_info.add(new PluginInfo(AlignerOutputView.class.getName(), AlignerOutputView.name, true));


		plugins_info.addAll(XmlPrefsParser.getPlugins());

		if (plugins_info == null || plugins_info.isEmpty()) {
			System.out.println("There are no plugins specified in preferences.");
		} else {
			ThreadUtils.runOnEventQueue(new Runnable() {

				public void run() {
					for (PluginInfo pi : plugins_info) {
						Object plugin = setUpPlugIn(pi);
						plugins.add(plugin);
					}
				}
			});
		}

		WebLink.autoLoad();

		// Need to let the QuickLoad system get started-up before starting
		//   the control server that listens to ping requests?
		// Therefore start listening for http requests only after all set-up is done.
		startControlServer();
	}

	private void fileMenu() {
		MenuUtil.addToMenu(file_menu, new JMenuItem(new LoadFileAction(frm, load_directory)));
		MenuUtil.addToMenu(file_menu, new JMenuItem(new LoadURLAction(frm)));
		MenuUtil.addToMenu(file_menu, new JMenuItem(new ClearAllAction()));
		MenuUtil.addToMenu(file_menu, new JMenuItem(new ClearGraphsAction()));
		file_menu.addSeparator();
		MenuUtil.addToMenu(file_menu, new JMenuItem(new PrintAction()));
		MenuUtil.addToMenu(file_menu, new JMenuItem(new PrintFrameAction()));
		file_menu.add(export_to_file_menu);
		MenuUtil.addToMenu(export_to_file_menu, new JMenuItem(new ExportMainViewAction()));
		MenuUtil.addToMenu(export_to_file_menu, new JMenuItem(new ExportLabelledMainViewAction()));
		MenuUtil.addToMenu(export_to_file_menu, new JMenuItem(new ExportWholeFrameAction()));
		file_menu.addSeparator();
		MenuUtil.addToMenu(file_menu, new JMenuItem(new CreateSpeciesDir(frm, genome_directory)));
		file_menu.addSeparator();
		MenuUtil.addToMenu(file_menu, new JMenuItem(new PreferencesAction()));
		file_menu.addSeparator();
		MenuUtil.addToMenu(file_menu, new JMenuItem(new ExitAction()));
		
	}

	private void editMenu() {
		MenuUtil.addToMenu(edit_menu, new JMenuItem(new CopyResiduesAction()));
	}

	private void viewMenu() {
		JMenu strands_menu = new JMenu("Strands");
		strands_menu.add(new ActionToggler(getMapView().getSeqMap().show_plus_action));
		strands_menu.add(new ActionToggler(getMapView().getSeqMap().show_minus_action));
		strands_menu.add(new ActionToggler(getMapView().getSeqMap().show_mixed_action));
		view_menu.add(strands_menu);
		MenuUtil.addToMenu(view_menu, new JMenuItem(AutoScrollAction.getAction()));
		MenuUtil.addToMenu(view_menu, new JMenuItem(new UCSCViewAction()));
		MenuUtil.addToMenu(view_menu, toggle_edge_matching_item);
		MenuUtil.addToMenu(view_menu, new JMenuItem(new AdjustEdgeMatchAction()));
		MenuUtil.addToMenu(view_menu, new JMenuItem(new ClampViewAction()));
		MenuUtil.addToMenu(view_menu, new JMenuItem(new UnclampViewAction()));
		MenuUtil.addToMenu(view_menu, new JCheckBoxMenuItem(ShrinkWrapAction.getAction()));
		MenuUtil.addToMenu(view_menu, new JCheckBoxMenuItem(ToggleHairlineLabelAction.getAction()));
		MenuUtil.addToMenu(view_menu, new JCheckBoxMenuItem(ToggleToolTip.getAction()));
		MenuUtil.addToMenu(view_menu, move_tab_to_window_item);
		MenuUtil.addToMenu(view_menu, move_tabbed_panel_to_window_item);
	}

	/**
	 *  Puts the given component either in the tab pane or in its own window,
	 *  depending on saved user preferences.
	 */
	private Object setUpPlugIn(PluginInfo pi) {
		if (!pi.shouldLoad()) {
			return null;
		}

		String class_name = pi.getClassName();
		if (class_name == null || class_name.trim().length() == 0) {
			ErrorHandler.errorPanel("Bad Plugin",
							"Cannot create plugin '" + pi.getPluginName() + "' because it has no class name.",
							this.frm);
			PluginInfo.getNodeForName(pi.getPluginName()).putBoolean("load", false);
			return null;
		}

		Object plugin = null;
		Throwable t = null;
		try {
			plugin = PluginInfo.instantiatePlugin(class_name);
		} catch (InstantiationException e) {
			plugin = null;
			t = e;
		}

		if (plugin == null) {
			ErrorHandler.errorPanel("Bad Plugin",
							"Could not create plugin '" + pi.getPluginName() + "'.",
							this.frm, t);
			PluginInfo.getNodeForName(pi.getPluginName()).putBoolean("load", false);
			return null;
		}

		ImageIcon icon = null;

		if (plugin instanceof IPlugin) {
			IPlugin plugin_view = (IPlugin) plugin;
			this.setPluginInstance(plugin_view.getClass(), plugin_view);
			icon = (ImageIcon) plugin_view.getPluginProperty(IPlugin.TEXT_KEY_ICON);
		}

		if (plugin instanceof JComponent) {
			if (plugin instanceof DataLoadView) {
				data_load_view = (DataLoadView) plugin;
			}
			if (plugin instanceof AltSpliceView) {
				MenuUtil.addToMenu(export_to_file_menu, new JMenuItem(new ExportSlicedViewAction()));
			}

			comp2plugin.put((Component) plugin, pi);
			String title = pi.getDisplayName();
			String tool_tip = ((JComponent) plugin).getToolTipText();
			if (tool_tip == null) {
				tool_tip = title;
			}
			JComponent comp = (JComponent) plugin;
			boolean in_a_window = (PreferenceUtils.getComponentState(title).equals(PreferenceUtils.COMPONENT_STATE_WINDOW));
			addToPopupWindows(comp, title);
			JCheckBoxMenuItem menu_item = comp2menu_item.get(comp);
			menu_item.setSelected(in_a_window);
			if (in_a_window) {
				openCompInWindow(comp, tab_pane);
			} else {
				tab_pane.addTab(title, icon, comp, tool_tip);
			}
		}
		return plugin;
	}

	@Override
	public void setPluginInstance(Class<?> c, IPlugin plugin) {
		super.setPluginInstance(c, plugin);
		if (c.equals(BookmarkManagerView.class)) {
			bmark_action.setBookmarkManager((BookmarkManagerView) plugin);
		}
	}

	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();
		if (src == toggle_edge_matching_item) {
			map_view.setEdgeMatching(!map_view.getEdgeMatching());
			toggle_edge_matching_item.setState(map_view.getEdgeMatching());
		} else if (src == move_tab_to_window_item) {
			openTabInNewWindow(tab_pane);
		} else if (src == move_tabbed_panel_to_window_item) {
			openTabbedPanelInNewWindow(tab_pane);
		}
	}

	/** Returns the icon stored in the jar file.
	 *  It is expected to be at com.affymetrix.igb.igb.gif.
	 *  @return null if the image file is not found or can't be opened.
	 */
	public Image getIcon() {
		Image icon = null;
		try {
			URL url = IGB.class.getResource("igb.gif");
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
	 * Saves information about which plugins are in separate windows and
	 * what their preferred sizes are.
	 */
	private void saveWindowLocations() {
		// Save the main window location
		PreferenceUtils.saveWindowLocation(frm, "main window");

		for (Component comp : comp2plugin.keySet()) {
			Frame f = comp2window.get(comp);
			if (f != null) {
				PluginInfo pi = comp2plugin.get(comp);
				PreferenceUtils.saveWindowLocation(f, pi.getPluginName());
			}
		}
		Frame f = comp2window.get(tab_pane);
		if (f != null) {
			PreferenceUtils.saveWindowLocation(f, TABBED_PANES_TITLE);
		}
	}

	private void openTabInNewWindow(final JTabbedPane tab_pane) {
		Runnable r = new Runnable() {

			public void run() {
				int index = tab_pane.getSelectedIndex();
				if (index < 0) {
					ErrorHandler.errorPanel("No more panes!");
					return;
				}
				final JComponent comp = (JComponent) tab_pane.getComponentAt(index);
				openCompInWindow(comp, tab_pane);
			}
		};
		SwingUtilities.invokeLater(r);
	}

	private void openCompInWindow(final JComponent comp, final JTabbedPane tab_pane) {
		final String title;
		final String display_name;
		final String tool_tip = comp.getToolTipText();

		if (comp2plugin.get(comp) instanceof PluginInfo) {
			PluginInfo pi = comp2plugin.get(comp);
			title = pi.getPluginName();
			display_name = pi.getDisplayName();
		} else {
			title = comp.getName();
			display_name = comp.getName();
		}

		Image temp_icon = null;
		if (comp instanceof IPlugin) {
			IPlugin pv = (IPlugin) comp;
			ImageIcon image_icon = (ImageIcon) pv.getPluginProperty(IPlugin.TEXT_KEY_ICON);
			if (image_icon != null) {
				temp_icon = image_icon.getImage();
			}
		}
		if (temp_icon == null) {
			temp_icon = getIcon();
		}

		// If not already open in a new window, make a new window
		if (comp2window.get(comp) == null) {
			tab_pane.remove(comp);
			tab_pane.validate();

			final JFrame frame = new JFrame(display_name);
			final Image icon = temp_icon;
			if (icon != null) {
				frame.setIconImage(icon);
			}
			final Container cont = frame.getContentPane();
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

			cont.add(comp);
			comp.setVisible(true);
			comp2window.put(comp, frame);
			frame.pack(); // pack() to set frame to its preferred size

			Rectangle pos = PreferenceUtils.retrieveWindowLocation(title, frame.getBounds());
			if (pos != null) {
				PreferenceUtils.setWindowSize(frame, pos);
			}
			frame.setVisible(true);
			frame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent evt) {
					// save the current size into the preferences, so the window
					// will re-open with this size next time
					PreferenceUtils.saveWindowLocation(frame, title);
					comp2window.remove(comp);
					cont.remove(comp);
					cont.validate();
					frame.dispose();
					tab_pane.addTab(display_name, null, comp, (tool_tip == null ? display_name : tool_tip));
					PreferenceUtils.saveComponentState(title, PreferenceUtils.COMPONENT_STATE_TAB);
					JCheckBoxMenuItem menu_item = comp2menu_item.get(comp);
					if (menu_item != null) {
						menu_item.setSelected(false);
					}
				}
			});
		} // extra window already exists, but may not be visible
		else {
			DisplayUtils.bringFrameToFront(comp2window.get(comp));
		}
		PreferenceUtils.saveComponentState(title, PreferenceUtils.COMPONENT_STATE_WINDOW);
	}

	private void openTabbedPanelInNewWindow(final JComponent comp) {

		final String title = TABBED_PANES_TITLE;
		final String display_name = title;

		// If not already open in a new window, make a new window
		if (comp2window.get(comp) == null) {
			splitpane.remove(comp);
			splitpane.validate();

			final JFrame frame = new JFrame(display_name);
			final Image icon = getIcon();
			if (icon != null) {
				frame.setIconImage(icon);
			}
			final Container cont = frame.getContentPane();
			frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

			cont.add(comp);
			comp.setVisible(true);
			comp2window.put(comp, frame);
			frame.pack(); // pack() to set frame to its preferred size

			Rectangle pos = PreferenceUtils.retrieveWindowLocation(title, frame.getBounds());
			if (pos != null) {
				//check that it's not too small, problems with using two screens
				int posW = (int) pos.getWidth();
				if (posW < 650) {
					posW = 650;
				}
				int posH = (int) pos.getHeight();
				if (posH < 300) {
					posH = 300;
				}
				pos.setSize(posW, posH);
				PreferenceUtils.setWindowSize(frame, pos);
			}
			frame.setVisible(true);

			final Runnable return_panes_to_main_window = new Runnable() {

				public void run() {
					// save the current size into the preferences, so the window
					// will re-open with this size next time
					PreferenceUtils.saveWindowLocation(frame, title);
					comp2window.remove(comp);
					cont.remove(comp);
					cont.validate();
					frame.dispose();
					splitpane.setBottomComponent(comp);
					splitpane.setDividerLocation(0.70);
					PreferenceUtils.saveComponentState(title, PreferenceUtils.COMPONENT_STATE_TAB);
					JCheckBoxMenuItem menu_item = comp2menu_item.get(comp);
					if (menu_item != null) {
						menu_item.setSelected(false);
					}
				}
			};

			frame.addWindowListener(new WindowAdapter() {

				@Override
				public void windowClosing(WindowEvent evt) {
					SwingUtilities.invokeLater(return_panes_to_main_window);
				}
			});

			JMenuBar mBar = new JMenuBar();
			frame.setJMenuBar(mBar);
			JMenu menu1 = new JMenu("Windows");
			menu1.setMnemonic('W');
			mBar.add(menu1);

			menu1.add(new AbstractAction("Return Tabbed Panes to Main Window") {

				public void actionPerformed(ActionEvent evt) {
					SwingUtilities.invokeLater(return_panes_to_main_window);
				}
			});
			menu1.add(new AbstractAction("Open Current Tab in New Window") {

				public void actionPerformed(ActionEvent evt) {
					openTabInNewWindow(tab_pane);
				}
			});
		} // extra window already exists, but may not be visible
		else {
			DisplayUtils.bringFrameToFront(comp2window.get(comp));
		}
		PreferenceUtils.saveComponentState(title, PreferenceUtils.COMPONENT_STATE_WINDOW);
	}

	private void addToPopupWindows(final JComponent comp, final String title) {
		JCheckBoxMenuItem popupMI = new JCheckBoxMenuItem(title);
		popupMI.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				JCheckBoxMenuItem src = (JCheckBoxMenuItem) evt.getSource();
				Frame frame = comp2window.get(comp);
				if (frame == null) {
					openCompInWindow(comp, tab_pane);
					src.setSelected(true);
				}
			}
		});
		comp2menu_item.put(comp, popupMI);
	}

	public void groupSelectionChanged(GroupSelectionEvent evt) {
		AnnotatedSeqGroup selected_group = evt.getSelectedGroup();
		if ((prev_selected_group != selected_group) && (prev_selected_seq != null)) {
			Persistence.saveSeqSelection(prev_selected_seq);
			Persistence.saveSeqVisibleSpan(map_view);
		}
		prev_selected_group = selected_group;
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		BioSeq selected_seq = evt.getSelectedSeq();
		if ((prev_selected_seq != null) && (prev_selected_seq != selected_seq)) {
			Persistence.saveSeqVisibleSpan(map_view);
		}
		prev_selected_seq = selected_seq;
	}

	public List<Object> getPlugins() {
		return Collections.<Object>unmodifiableList(plugins);
	}

	public static void MPTAGprintClass(String callLocation, Object o){
		System.out.println("MPTAG <<<>>> DEBUG OUTPUT at "+callLocation);
		System.out.println(o.getClass().getCanonicalName()+ "\n");
		try{
			throw new Exception();
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void changeMainView(int main_view_id) {
		neoMapPane.setSelectedIndex(main_view_id);
	}


}
