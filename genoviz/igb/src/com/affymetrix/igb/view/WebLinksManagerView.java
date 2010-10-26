package com.affymetrix.igb.view;

import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.prefs.WebLink;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.DisplayUtils;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.text.MessageFormat;
import javax.swing.*;
import javax.swing.event.*;

/**
 *  A panel for viewing and editing weblinks.
 */
public final class WebLinksManagerView extends JPanel {
  private JList webLinks;
  private JScrollPane scroll_pane;

  private final Action import_action;
  private final Action export_action;
  private final Action delete_action;
  private final Action edit_action;
  private final Action add_action;
  
  private final WebLinkEditorPanel edit_panel;
  private static JFrame static_frame = null;

  private static JFileChooser static_chooser = null;

  // initialize the static_panel early, because this will cause the accelerator
  // key-strokes to be configured early through the PreferenceUtils and thus
  // for them to be visible in the KeyStrokesView
  private static WebLinksManagerView static_panel = new WebLinksManagerView();

  /** Creates a new instance of Class */
  private WebLinksManagerView() {
    super();

    webLinks = createJList();

    scroll_pane = new JScrollPane(webLinks);

    this.setLayout(new BorderLayout());
    scroll_pane.setMinimumSize(new Dimension(50,50));
    this.add(scroll_pane, BorderLayout.CENTER);

    export_action = makeExportAction();
    import_action = makeImportAction();

    delete_action = makeDeleteAction();
    add_action = makeAddAction();
    edit_action = makeEditAction();

    setUpMenuBar();
    setUpButtons();
    setUpPopupMenu();
        
    enableActions();
    this.validate();

    edit_panel = new WebLinkEditorPanel();
  }


  private final ListCellRenderer list_renderer = new DefaultListCellRenderer() {
    @Override
    public Component getListCellRendererComponent(JList list,
        Object value, int index, boolean isSelected, boolean cellHasFocus) {
      WebLink wl = (WebLink) value;
      String name = wl.getName();
      String regex = "All Tiers";
      if (wl.getRegex() != null) {
        regex = wl.getRegex();
        if (regex.startsWith("(?i)")) {
          regex = regex.substring(4);
        }
      }
      String msg = "<html><b>'" + name
        + "'</b>:&nbsp;&nbsp;&nbsp;&nbsp;<font color=red>" + regex + "</font>";

      return super.getListCellRendererComponent(list, msg, index,isSelected,cellHasFocus);
    }
  };

  private final ListSelectionListener list_listener = new ListSelectionListener() {
    public void valueChanged(ListSelectionEvent e) {
      if (! e.getValueIsAdjusting()) {
        enableActions();
      }
    }
  };
  
  
  public static Action getShowFrameAction() {
    Action a = new AbstractAction(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("configureWebLinks"))) {
		private static final long serialVersionUID = 1l;

      public void actionPerformed(ActionEvent evt) {
        showManager();
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/general/Search16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Manage Web Links");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_W));
    setAccelerator(a);
    return a;
  }

  private JList createJList() {
    JList j_list = new JList(WebLink.getWebLinkListModel());
    
    j_list.setCellRenderer(list_renderer);    
    j_list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    j_list.addListSelectionListener(list_listener);
    
    return j_list;
  }

  private void enableActions() {
    int num_selections = webLinks.getSelectedValues().length;
    
    import_action.setEnabled(true);
    export_action.setEnabled(webLinks.getModel().getSize() > 0);

    delete_action.setEnabled(num_selections > 0);
    edit_action.setEnabled(num_selections == 1);
    add_action.setEnabled(true);
  }
  
  private static void setAccelerator(Action a) {
    KeyStroke ks = PreferenceUtils.getAccelerator("Web Links Manager / "+a.getValue(Action.NAME));
    a.putValue(Action.ACCELERATOR_KEY, ks);
  }

  private void setUpMenuBar() {
    JMenuBar menu_bar = new JMenuBar();
    JMenu links_menu = new JMenu("Web Links") {      
      @Override
      public JMenuItem add(Action a) {
        JMenuItem menu_item = super.add(a);
        menu_item.setToolTipText(null);
        return menu_item;
      }
    };
    links_menu.setMnemonic('L');

    links_menu.add(edit_action);
    links_menu.add(add_action);
    links_menu.add(delete_action);
    links_menu.addSeparator();
    links_menu.add(import_action);
    links_menu.add(export_action);

    menu_bar.add(links_menu);
    this.add(menu_bar, BorderLayout.NORTH);
  }

  private void setUpPopupMenu() {
    final JPopupMenu popup = new JPopupMenu() {      
      @Override
      public JMenuItem add(Action a) {
        JMenuItem menu_item = super.add(a);
        menu_item.setToolTipText(null);
        return menu_item;
      }
    };
    popup.add(edit_action);
    popup.add(add_action);
    popup.add(delete_action);
    popup.addSeparator();
    popup.add(import_action);
    popup.add(export_action);
    MouseAdapter mouse_adapter = new MouseAdapter() {
      @Override
      public void mousePressed(MouseEvent e) {
        if (popup.isPopupTrigger(e)) {
          popup.show(webLinks, e.getX(), e.getY());
        }
      }
      @Override
      public void mouseReleased(MouseEvent e) {
        if (popup.isPopupTrigger(e)) {
          popup.show(webLinks, e.getX(), e.getY());
        }
      }
    };
    webLinks.addMouseListener(mouse_adapter);
  }

  private void setUpButtons() {
    JToolBar tool_bar = new JToolBar(JToolBar.HORIZONTAL);
    tool_bar.setFloatable(false);

    tool_bar.add(new JButton(edit_action));
    tool_bar.addSeparator();
    tool_bar.add(new JButton(add_action));
    tool_bar.addSeparator();
    tool_bar.add(new JButton(delete_action));
    this.add(tool_bar, BorderLayout.SOUTH);
  }

  private Action makeImportAction() {
    Action a = new AbstractAction("Import ...") {
      public void actionPerformed(ActionEvent ae) {
        importWebLinks();
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/general/Import16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Import Web Links");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_I));
    setAccelerator(a);
    return a;
  }

  private Action makeExportAction() {
    Action a = new AbstractAction("Export ...") {
      public void actionPerformed(ActionEvent ae) {
        exportWebLinks();
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/general/Export16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Export Web Links");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
    setAccelerator(a);
    return a;
  }
  

  private Action makeDeleteAction() {
    Action a = new AbstractAction("Delete ...") {
      public void actionPerformed(ActionEvent ae) {
        if (localDelete()) {
			return;
		}
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/general/Delete16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Delete Selected Link(s)");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_D));
    setAccelerator(a);
    return a;
  }

 private boolean localDelete() throws HeadlessException {
		Object[] selections = webLinks.getSelectedValues();
		Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, webLinks);
		if (selections.length == 0) {
			this.setEnabled(false);
			return true;
		}
		int yes = JOptionPane.showConfirmDialog(frame, "Delete these " + selections.length + " selected link(s)?", "Delete?", JOptionPane.YES_NO_OPTION);
		if (yes == JOptionPane.YES_OPTION) {
			for (int i = 0; i < selections.length; i++) {
				WebLink.removeWebLink((WebLink) selections[i]);
			}
		}
		return false;
	}

  private Action makeAddAction() {
    Action a = new AbstractAction("Add...") {
      public void actionPerformed(ActionEvent ae) {
				localAdd();
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/development/WebComponentAdd16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Add New Web Link");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_A));
    setAccelerator(a);
    return a;
  }

  private void localAdd() {
		WebLink link = new WebLink();
		edit_panel.setWebLink(link);
		boolean ok = edit_panel.showDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, webLinks));
		if (ok) {
			edit_panel.setLinkPropertiesFromGUI();
			WebLink.addWebLink(link);
		}
	}
  
  private Action makeEditAction() {
    Action a = new AbstractAction("Edit...") {
      public void actionPerformed(ActionEvent ae) {
        WebLink link = (WebLink) webLinks.getSelectedValue();
        edit_panel.setWebLink(link);
        boolean ok = edit_panel.showDialog((JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, webLinks));
        if (ok) {
          edit_panel.setLinkPropertiesFromGUI();
          webLinks.invalidate();
          webLinks.repaint();
        }        
      }
    };
    a.putValue(Action.SMALL_ICON, MenuUtil.getIcon("toolbarButtonGraphics/development/WebComponent16.gif"));
    a.putValue(Action.SHORT_DESCRIPTION, "Edit Selected Web Link");
    a.putValue(Action.MNEMONIC_KEY, new Integer(KeyEvent.VK_E));
    setAccelerator(a);
    return a;
  }
  
  /** Gets a static re-usable file chooser that prefers "html" files. */
  private static JFileChooser getJFileChooser() {
    if (static_chooser == null) {
      static_chooser = UniFileChooser.getFileChooser("XML file", "xml");
      static_chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
    }
    static_chooser.rescanCurrentDirectory();
    return static_chooser;
  }

  /**
   *  Tries to import weblinks.
   */
  private void importWebLinks() {
    JFileChooser chooser = getJFileChooser();
    chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
    Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, webLinks);
    int option = chooser.showOpenDialog(frame);
    if (option == JFileChooser.APPROVE_OPTION) {
      FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
      File fil = chooser.getSelectedFile();
      try {
        WebLink.importWebLinks(fil);
      }
      catch (FileNotFoundException fe) {
        ErrorHandler.errorPanel("Error", "Error importing web links: File Not Found " + 
           fil.getAbsolutePath(), webLinks, fe);
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel("Error", "Error importing web links", webLinks, ex);
      }
    }
    enableActions();
  }

  private void exportWebLinks() {
    Container frame = SwingUtilities.getAncestorOfClass(JFrame.class, webLinks);
    if (webLinks.getModel().getSize() == 0) {
      ErrorHandler.errorPanel("Error", "No web links to save", frame);
      return;
    }
    JFileChooser chooser = getJFileChooser();
    chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
    int option = chooser.showSaveDialog(frame);
    if (option == JFileChooser.APPROVE_OPTION) {
      try {
        FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
        File fil = chooser.getSelectedFile();
        String full_path = fil.getCanonicalPath();

        if (! full_path.endsWith(".xml")) {
          fil = new File(full_path + ".xml");
        }
        WebLink.exportWebLinks(fil, false);
      }
      catch (Exception ex) {
        ErrorHandler.errorPanel("Error", "Error exporting web links", frame, ex);
      }
    }
  }

  
  private static synchronized WebLinksManagerView getManager() {
    if (static_panel == null) {
      static_panel = new WebLinksManagerView();
    }
    return static_panel;
  }

  
  private static synchronized JFrame showManager() {
    if (static_frame == null) {
      static_frame = PreferenceUtils.createFrame("Web Links", getManager());
      ImageIcon icon = MenuUtil.getIcon("toolbarButtonGraphics/general/Search16.gif");
      if (icon != null) { static_frame.setIconImage(icon.getImage()); }
    }
    DisplayUtils.bringFrameToFront(static_frame);
    return static_frame;
  }

}
