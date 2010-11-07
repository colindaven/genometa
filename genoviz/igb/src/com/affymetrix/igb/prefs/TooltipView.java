package com.affymetrix.igb.prefs;

import java.awt.*;
import javax.swing.*;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genometryImpl.util.TooltipUtils;

/**
 *  A panel that shows the preferences for graph properties.
 */
public final class TooltipView extends IPrefEditorComponent  {
    
  public TooltipView() {
    super();
    this.setName("Tooltip");
	this.setToolTipText("Edit Default Tooltip");
    this.setLayout(new BorderLayout());

    JPanel main_box = new JPanel();
    main_box.setBorder(new javax.swing.border.EmptyBorder(5,5,5,5));
    
    JScrollPane scroll_pane = new JScrollPane(main_box);
    this.add(scroll_pane);
    
    JPanel graphs_box = new JPanel();
    graphs_box.setLayout(new BoxLayout(graphs_box, BoxLayout.Y_AXIS));
    graphs_box.setAlignmentX(0.0f);
    main_box.add(graphs_box);

	
	String name;
	boolean value;
	for(int i = 0; i < TooltipUtils.countTooltips(); i++) {
		name = TooltipUtils.getTooltipName(i);
		value = TooltipUtils.isTooltipShow(i);
		graphs_box.add(PreferenceUtils.createCheckBox(
				name.substring(0, 1).toUpperCase() + name.substring(1),
				PreferenceUtils.getTooltipPrefsNode(), name, value));

		graphs_box.add(Box.createRigidArea(new Dimension(0,5)));
	}
        
    validate();
  }

  public void refresh() {
  }
  
}
