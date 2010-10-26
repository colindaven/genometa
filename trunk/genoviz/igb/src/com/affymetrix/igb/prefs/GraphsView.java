package com.affymetrix.igb.prefs;

import java.awt.*;
import javax.swing.*;
import com.affymetrix.genometryImpl.style.HeatMap;
import com.affymetrix.genometryImpl.parsers.graph.ScoredIntervalParser;
import com.affymetrix.genometryImpl.util.GraphSymUtils;
import com.affymetrix.igb.util.GraphGlyphUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;

/**
 *  A panel that shows the preferences for graph properties.
 */
public final class GraphsView extends IPrefEditorComponent  {
    
  public GraphsView() {
    super();
    this.setName("Graphs");
	this.setToolTipText("Edit Default Graph Properties");
    this.setLayout(new BorderLayout());

    JPanel main_box = new JPanel();
    main_box.setBorder(new javax.swing.border.EmptyBorder(5,5,5,5));
    
    JScrollPane scroll_pane = new JScrollPane(main_box);
    this.add(scroll_pane);
    
    JPanel graphs_box = new JPanel();
    graphs_box.setLayout(new BoxLayout(graphs_box, BoxLayout.Y_AXIS));
    graphs_box.setAlignmentX(0.0f);
    main_box.add(graphs_box);
    
    graphs_box.add(PreferenceUtils.createCheckBox("Use file URL as graph name", PreferenceUtils.getGraphPrefsNode(),
      GraphSymUtils.PREF_USE_URL_AS_NAME, GraphSymUtils.default_use_url_as_name));

    graphs_box.add(Box.createRigidArea(new Dimension(0,5)));
    
    graphs_box.add(PreferenceUtils.createCheckBox("Make graphs from scored interval ('egr' and 'sin') files",
						 PreferenceUtils.getTopNode(),
						 ScoredIntervalParser.PREF_ATTACH_GRAPHS,
						 ScoredIntervalParser.default_attach_graphs));
    
    JComboBox heat_cb = PreferenceUtils.createComboBox(PreferenceUtils.getTopNode(), HeatMap.PREF_HEATMAP_NAME,
        HeatMap.getStandardNames(), HeatMap.def_heatmap_name.toString());
    Box heat_row = Box.createHorizontalBox();
    heat_row.add(new JLabel("Preferred Heatmap"));
    heat_row.add(Box.createRigidArea(new Dimension(6,0)));
    heat_row.add(heat_cb);
    heat_row.setAlignmentX(0.0f);
    graphs_box.add(heat_row);
    graphs_box.add(Box.createRigidArea(new Dimension(0,5)));
        
    validate();
  }

  public void refresh() {
  }
  
}
