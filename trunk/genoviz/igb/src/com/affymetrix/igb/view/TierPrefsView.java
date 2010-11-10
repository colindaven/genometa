/**
*   Copyright (c) 2005-2006 Affymetrix, Inc.
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

import com.affymetrix.igb.Application;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.table.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.affymetrix.igb.prefs.IPrefEditorComponent;
import com.affymetrix.igb.tiers.TrackStyle;
import com.affymetrix.igb.tiers.TierGlyph;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genoviz.swing.BooleanTableCellRenderer;
import com.affymetrix.genoviz.swing.ColorTableCellEditor;
import com.affymetrix.genoviz.swing.ColorTableCellRenderer;


/**
 *  A panel for choosing tier properties for the {@link SeqMapView}.
 */
public final class TierPrefsView extends IPrefEditorComponent implements ListSelectionListener, WindowListener  {
	public static final long serialVersionUID = 1l;

  private final JTable table = new JTable();

  private static final String TIER_NAME = "Tier ID";
  private static final String COLOR = "Color";
  private static final String SEPARATE = "2 Tiers";
  private static final String COLLAPSED = "Collapsed";
  private static final String MAX_DEPTH = "Max Depth";
  private static final String BACKGROUND = "Background";
  private static final String GLYPH_DEPTH = "Connected";
  private static final String LABEL_FIELD = "Label Field";
  private static final String HUMAN_NAME = "Display Name";
  private static final String FORWARD_COLOR = "Forward Color"; //MPTAG added
  private static final String REVERSE_COLOR = "Reverse Color"; //MPTAG added

  private final static String[] col_headings = {
    HUMAN_NAME,
    COLOR, BACKGROUND,
    SEPARATE, COLLAPSED,
    MAX_DEPTH, GLYPH_DEPTH, LABEL_FIELD, TIER_NAME,
	FORWARD_COLOR, REVERSE_COLOR,  //MPTAG added
    //    GRAPH_TIER,
  };

  private static final int COL_HUMAN_NAME = 0;
  private static final int COL_COLOR = 1;
  private static final int COL_BACKGROUND = 2;
  private static final int COL_SEPARATE = 3;
  private static final int COL_COLLAPSED = 4;
  private static final int COL_MAX_DEPTH = 5;
  private static final int COL_GLYPH_DEPTH = 6;
  private static final int COL_LABEL_FIELD = 7;
  private static final int COL_TIER_NAME = 8;
  private static final int COL_FORWARD_COLOR = 9;
  private static final int COL_REVERSE_COLOR = 10;

  private final TierPrefsTableModel model;
  private final ListSelectionModel lsm;

  private static final String PREF_AUTO_REFRESH = "Auto-Apply Tier Customizer Changes";
  private static final boolean default_auto_refresh = true;
  private JCheckBox auto_refresh_CB;

  private static final String REFRESH_LIST = "Refresh List";
  private final JButton refresh_list_B = new JButton(REFRESH_LIST);

  private static final String REFRESH_MAP = "Refresh Map";
  private final JButton refresh_map_B = new JButton(REFRESH_MAP);

  private static final String AUTO_REFRESH = "Auto Refresh";
  private static final String APPLY_DEFAULT_BG = "Apply Default Background";

  private SeqMapView smv;

  private static TrackStyle default_annot_style = TrackStyle.getDefaultInstance(); // make sure at least the default instance exists;

  public TierPrefsView() {
    this(false, true);
  }

  public TierPrefsView(boolean add_refresh_list_button, boolean add_refresh_map_button) {
    super();

	this.setName("Tiers");
	this.setToolTipText("Set Tier Colors and Properties");
    this.setLayout(new BorderLayout());

    JScrollPane table_scroll_pane = new JScrollPane(table);

    this.add(table_scroll_pane, BorderLayout.CENTER);
    JPanel button_panel = new JPanel();
    button_panel.setLayout(new BoxLayout(button_panel, BoxLayout.X_AXIS));

    JButton apply_bg_button = new JButton(APPLY_DEFAULT_BG);
    button_panel.add(Box.createHorizontalGlue());
    button_panel.add(apply_bg_button);
    apply_bg_button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        copyDefaultBG();
      }
    });


    Application igb = Application.getSingleton();
    if (igb != null) {
      smv = igb.getMapView();
    }

    // Add a "refresh map" button, iff there is an instance of IGB
    if (smv != null && add_refresh_map_button) {
      refresh_map_B.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          refreshSeqMapView();
        }
      });
      button_panel.add(Box.createHorizontalStrut(10));
      button_panel.add(refresh_map_B);

      auto_refresh_CB = PreferenceUtils.createCheckBox(AUTO_REFRESH,
        PreferenceUtils.getTopNode(), PREF_AUTO_REFRESH, default_auto_refresh);
      auto_refresh_CB.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          if (refresh_map_B != null) {
            refresh_map_B.setEnabled(! auto_refresh_CB.isSelected());
            if (auto_refresh_CB.isSelected()) {
              refreshSeqMapView();
            }
          }
        }
      });
      refresh_map_B.setEnabled(! auto_refresh_CB.isSelected());
      button_panel.add(Box.createHorizontalStrut(10));
      button_panel.add(auto_refresh_CB);
    }

    if (add_refresh_list_button) {
      button_panel.add(Box.createHorizontalStrut(10));
      button_panel.add(refresh_list_B);
      //this.add(refresh_list_B, BorderLayout.SOUTH);
      refresh_list_B.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent evt) {
          refreshList();
        }
      });
    }
    button_panel.add(Box.createHorizontalGlue());
    this.add(button_panel, BorderLayout.SOUTH);

    model = new TierPrefsTableModel();
    model.addTableModelListener(new javax.swing.event.TableModelListener() {
      public void tableChanged(javax.swing.event.TableModelEvent e) {
        // do nothing.
      }
    });

    lsm = table.getSelectionModel();
    lsm.addListSelectionListener(this);
    lsm.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

    table.setModel(model);
	table.setAutoCreateRowSorter(true);
	table.setFillsViewportHeight(true);

    table.getColumnModel().getColumn(COL_TIER_NAME).setPreferredWidth(150);
    table.getColumnModel().getColumn(COL_HUMAN_NAME).setPreferredWidth(150);

    table.setRowSelectionAllowed(true);
    table.setEnabled( true ); // doesn't do anything ?

    table.setDefaultRenderer(Color.class, new ColorTableCellRenderer());
    table.setDefaultEditor(Color.class, new ColorTableCellEditor());//MPTAG der hier überträgt meine änderungen nicht!!
    table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());

    validate();
  }

  public void applyChanges() {
    refreshSeqMapView();
  }

  public void setStyleList(List<TrackStyle> styles) {
    model.setStyles(styles);
    model.fireTableDataChanged();
  }

  /** Called when the user selects a row of the table.
   * @param evt
   */
  public void valueChanged(ListSelectionEvent evt) {
    if (evt.getSource()==lsm && ! evt.getValueIsAdjusting()) {
    }
  }

  public void destroy() {
    removeAll();
    if (lsm != null) {lsm.removeListSelectionListener(this);}
  }

  private void refreshSeqMapView() {
    if (smv != null) {
      smv.setAnnotatedSeq(smv.getAnnotatedSeq(), true, true, true);
    }
  }

  void refreshList() {
    boolean only_displayed_tiers = true;
    boolean include_graph_styles = false;
    List<TrackStyle> styles;
    // if only_displayed_tiers, then only put AnnotStyles in table that are being used in tiers currently displayed in main view
    if (only_displayed_tiers) {
      styles = new ArrayList<TrackStyle>();
      styles.add(default_annot_style);
      if (smv != null) {  
	List<TierGlyph> tiers = smv.getSeqMap().getTiers();
	LinkedHashMap<TrackStyle,TrackStyle> stylemap = new LinkedHashMap<TrackStyle,TrackStyle>();
	Iterator<TierGlyph> titer = tiers.iterator();
	while (titer.hasNext()) {
	  TierGlyph tier = titer.next();
	  ITrackStyle style = tier.getAnnotStyle();
	  if ((style instanceof TrackStyle) &&
	      (style.getShow()) && 
	      (tier.getChildCount() > 0) ) {
	    stylemap.put((TrackStyle)style, (TrackStyle)style);
	  }
	}
	styles.addAll(stylemap.values());
      }
    }
    else { styles = TrackStyle.getAllLoadedInstances(); }
    ArrayList<TrackStyle> customizables = new ArrayList<TrackStyle>(styles.size());
    for (int i=0; i<styles.size(); i++) {
      TrackStyle the_style = styles.get(i);
      if (the_style.getCustomizable()) {
	// if graph tier style then only include if include_graph_styles toggle is set (app is _not_ IGB)
	if ((! the_style.isGraphTier()) || include_graph_styles) {
	  customizables.add(the_style);
	}
      }
    }
    this.setStyleList(customizables);
  }

  // Copy the background color from the default style to all loaded styles.
  void copyDefaultBG() {
    Iterator<TrackStyle> iter = TrackStyle.getAllLoadedInstances().iterator();
    while (iter.hasNext()) {
      TrackStyle as = iter.next();
      as.setBackground(default_annot_style.getBackground());
    }
    table.repaint(); // table needs to redraw itself due to changed values
    if (autoApplyChanges()) {
      refreshSeqMapView();
    }
  }

  /**
   *  Call this whenver this component is removed from the view, due to the
   *  tab pane closing or the window closing.  It will decide whether it is
   *  necessary to update the SeqMapView in response to changes in settings
   *  in this panel.
   */
  public void removedFromView() {
    // if autoApplyChanges(), then the changes were already applied,
    // otherwise apply changes as needed.
    if (! autoApplyChanges()) {
      SwingUtilities.invokeLater( new Runnable() {
        public void run() {
          applyChanges();
        }
      });
    }
  }


  /** Whether or not changes to the table should automatically be
   *  applied to the view.
   */
  boolean autoApplyChanges() {
    boolean auto_apply_changes = true;
    if (auto_refresh_CB == null) {
      auto_apply_changes = true;
    } else {
      auto_apply_changes = PreferenceUtils.getBooleanParam(
        PREF_AUTO_REFRESH, default_auto_refresh);
    }
    return auto_apply_changes;
  }

  class TierPrefsTableModel extends AbstractTableModel {
	  public static final long serialVersionUID = 1l;

    List<TrackStyle> tier_styles;

    TierPrefsTableModel() {
      this.tier_styles = Collections.<TrackStyle>emptyList();
    }

    public void setStyles(List<TrackStyle> tier_styles) {
      this.tier_styles = tier_styles;
    }

    public List<TrackStyle> getStyles() {
      return this.tier_styles;
    }

    // Allow editing most fields in normal rows, but don't allow editing some
    // fields in the "default" style row.
		@Override
    public boolean isCellEditable(int row, int column) {
      if (tier_styles.get(row) == default_annot_style) {
        if (column == COL_COLOR || column == COL_BACKGROUND || column == COL_SEPARATE
            || column == COL_COLLAPSED || column == COL_MAX_DEPTH
			|| column == COL_FORWARD_COLOR || column == COL_REVERSE_COLOR  //MPTAG added
			) {
          return true;
        }
        else {
          return false;
        }
      } else {
        return (column != COL_TIER_NAME);
      }
    }

		@Override
    public Class<?> getColumnClass(int c) {
      Object val = getValueAt(0, c);
      if (val == null) {
        return Object.class;
      } else {
        return val.getClass();
      }
    }

    public int getColumnCount() {
      return col_headings.length;
    }

		@Override
    public String getColumnName(int columnIndex) {
      return col_headings[columnIndex];
    }

    public int getRowCount() {
      return tier_styles.size();
    }

    public Object getValueAt(int row, int column) {
      TrackStyle style = tier_styles.get(row);
      switch (column) {
        case COL_COLOR:
          return style.getColor();
        case COL_SEPARATE:
          return Boolean.valueOf(style.getSeparate());
        case COL_COLLAPSED:
          return Boolean.valueOf(style.getCollapsed());
        case COL_TIER_NAME:
          String name = style.getUniqueName();
	  if (name == null) { name = ""; }
          if (! style.getPersistent()) { name = "<html><i>" + name + "</i></html>"; }
          return name;
        case COL_MAX_DEPTH:
          int md = style.getMaxDepth();
          if (md == 0) { return ""; }
          else { return String.valueOf(md); }
        case COL_BACKGROUND:
          return style.getBackground();
        case COL_GLYPH_DEPTH:
          return (style.getGlyphDepth()==2 ? Boolean.TRUE : Boolean.FALSE);
        case COL_LABEL_FIELD:
          return style.getLabelField();
        case COL_HUMAN_NAME:
          if (style == default_annot_style) { return "* default *"; }
          else { return style.getHumanName(); }
	//        case COL_GRAPH_TIER:
	//	  return style.isGraphTier();
		  //MPTAG added
		case COL_FORWARD_COLOR:
			return style.getForwardColor();
		case COL_REVERSE_COLOR:
			return style.getReverseColor();
        default:
          return null;
      }
    }

		@Override
    public void setValueAt(Object value, int row, int col) {
      try {
      TrackStyle style = tier_styles.get(row);
      switch (col) {
        case COL_COLOR:
          style.setColor((Color) value);
          break;
        case COL_SEPARATE:
          style.setSeparate(((Boolean) value).booleanValue());
          break;
        case COL_COLLAPSED:
          style.setCollapsed(((Boolean) value).booleanValue());
          break;
        case COL_TIER_NAME:
          System.out.println("Tier name is not changeable!");
          break;
        case COL_MAX_DEPTH:
          {
            int i = parseInteger(((String) value), 0, style.getMaxDepth());
            style.setMaxDepth(i);
          }
          break;
        case COL_BACKGROUND:
          style.setBackground((Color) value);
          break;
        case COL_GLYPH_DEPTH:
          if (Boolean.TRUE.equals(value)) {
            style.setGlyphDepth(2);
          } else {
            style.setGlyphDepth(1);
          }
          break;
        case COL_LABEL_FIELD:
          style.setLabelField((String) value);
          break;
        case COL_HUMAN_NAME:
	  if (style != default_annot_style) {
	    style.setHumanName((String) value);
	  }
          break;
		  //MPTAG added
		case COL_FORWARD_COLOR:
			style.setForwardColor((Color) value);
			break;
		case COL_REVERSE_COLOR:
			style.setReverseColor((Color) value);
			break;
        default:
          System.out.println("Unknown column selected: " + col);;
      }
      fireTableCellUpdated(row, col);
      } catch (Exception e) {
        // exceptions should not happen, but must be caught if they do
        System.out.println("Exception in TierPrefsView.setValueAt(): " + e);
      }

      if (autoApplyChanges()) {
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            applyChanges();
          }
        });
      }
    }

    /** Parse an integer, using the given fallback if any exception occurrs.
     *  @param s  The String to parse.
     *  @param empty_string  the value to return if the input is an empty string.
     *  @param fallback  the value to return if the input String is unparseable.
     */
    int parseInteger(String s, int empty_string, int fallback) {
      //System.out.println("Parsing string: '" + s + "'");
      int i = fallback;
      try {
        if ("".equals(s.trim())) {i = empty_string; }
        else { i = Integer.parseInt(s); }
      }
      catch (Exception e) {
        //System.out.println("Exception: " + e);
        // don't report the error, use the fallback value
      }
      return i;
    }

  };


  static JFrame static_frame;
  static TierPrefsView static_instance;

  static final String WINDOW_NAME = "Tier Customizer";

  /**
   *  Gets an instance of TierPrefsView wrapped in a JFrame, useful
   *  as a pop-up dialog for setting annotation styles.
   */
  /*public static JFrame showFrame() {
    if (static_frame == null) {
      static_frame = new JFrame(WINDOW_NAME);
      static_instance = new TierPrefsView(false, false);
      static_frame.getContentPane().add(static_instance);

      static_frame.addWindowListener(new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent e) {
          //static_instance.refreshSeqMapView();
          // save window size
        }
      });
      static_frame.pack();
      // restore saved window size
    }

    static_instance.refreshList();


    static_frame.setVisible(true);
    return static_frame;
  }*/

  /** Used for testing.  Opens a window with the TierPrefsView in it. */
  /*public static void main(String[] args) {

    TierPrefsView t = new TierPrefsView();

    AnnotStyle.getInstance("RefSeq");
    AnnotStyle.getInstance("EnsGene");
    AnnotStyle.getInstance("Contig");
    AnnotStyle.getInstance("KnownGene");
    AnnotStyle.getInstance("TwinScan", false);

    t.setStyleList(AnnotStyle.getAllLoadedInstances());

    JFrame f = new JFrame(WINDOW_NAME);
    f.getContentPane().add(t);

    f.addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent e) {
        System.exit(0);
      }
    });
    f.pack();

    f.setSize(800, 800);
    f.setVisible(true);
  }*/

  // implementation of IPrefEditorComponent
  public void refresh() {
    refreshList();
  }

	private void stopEditing() {
		if (table != null && table.getCellEditor() != null) {
			table.getCellEditor().stopCellEditing();
		}
	}

	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (!visible) {
			stopEditing();
		}
	}

	public void windowClosed(WindowEvent e) {
		stopEditing();
	}

  	public void windowOpened(WindowEvent e) { }

	public void windowClosing(WindowEvent e) { }

	public void windowIconified(WindowEvent e) { }

	public void windowDeiconified(WindowEvent e) { }

	public void windowActivated(WindowEvent e) { }

	public void windowDeactivated(WindowEvent e) { }
}
