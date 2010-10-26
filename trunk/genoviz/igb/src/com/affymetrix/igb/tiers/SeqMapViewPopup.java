/**
*   Copyright (c) 2005-2007 Affymetrix, Inc.
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

package com.affymetrix.igb.tiers;

import com.affymetrix.genometryImpl.SeqSymmetry;
import com.affymetrix.genometryImpl.BioSeq;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import com.affymetrix.genometryImpl.util.UniFileChooser;
import com.affymetrix.genometryImpl.SymWithProps;
import com.affymetrix.genometryImpl.GraphSym;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.style.ITrackStyle;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.genometryImpl.parsers.BedParser;
import com.affymetrix.genometryImpl.style.ITrackStyleExtended;
import com.affymetrix.genometryImpl.style.GraphType;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.glyph.GraphGlyph;
import com.affymetrix.igb.prefs.PreferencesPanel;
import com.affymetrix.igb.tiers.AffyTieredMap.ActionToggler;
import com.affymetrix.igb.tiers.TierGlyph.Direction;
import com.affymetrix.igb.view.DependentData;
import com.affymetrix.igb.view.DependentData.DependentType;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.view.TrackView;

public final class SeqMapViewPopup implements TierLabelManager.PopupListener {

  private static final boolean DEBUG = false;
  
  private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
  private final SeqMapView gviewer;
  private final TierLabelManager handler;

  private final JMenu showMenu = new JMenu("Show...");
  private final JMenu changeMenu = new JMenu("Change...");
  private final JMenu strandsMenu = new JMenu("Strands...");
  private final JMenu summaryMenu = new JMenu("Make Annotation Depth Graph");

  private final ActionToggler at1;
  private final ActionToggler at2;
  private final ActionToggler at3;

  private final Action select_all_tiers_action = new AbstractAction("Select All Tracks") {
    public void actionPerformed(ActionEvent e) {
      handler.selectAllTiers();
    }
  };

  private final Action rename_action = new AbstractAction("Change Display Name") {
    public void actionPerformed(ActionEvent e) {
      List current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() != 1) {
        ErrorHandler.errorPanel("Must select only one track");
      }
      TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
      renameTier(current_tier);
    }
  };

  private final Action customize_action = new AbstractAction("Customize") {
    public void actionPerformed(ActionEvent e) {
      showCustomizer();
    }
  };

  private final Action expand_action = new AbstractAction("Expand") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getSelectedTierLabels(), false);
    }
  };

  private final Action expand_all_action = new AbstractAction("Expand All") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getAllTierLabels(), false);
    }
  };

  private final Action collapse_action = new AbstractAction("Collapse") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getSelectedTierLabels(), true);
    }
  };

  private final Action collapse_all_action = new AbstractAction("Collapse All") {
    public void actionPerformed(ActionEvent e) {
      setTiersCollapsed(handler.getAllTierLabels(), true);
    }
  };

  private final Action hide_action = new AbstractAction("Hide") {
    public void actionPerformed(ActionEvent e) {
      hideTiers(handler.getSelectedTierLabels());
    }
  };

  private final Action show_all_action = new AbstractAction("Show All Types") {
    public void actionPerformed(ActionEvent e) {
      showAllTiers();
    }
  };

  private final Action change_color_action = new AbstractAction("Change FG Color") {
    public void actionPerformed(ActionEvent e) {
      changeColor(handler.getSelectedTierLabels(), true);
    }
  };

  private final Action change_bg_color_action = new AbstractAction("Change BG Color") {
    public void actionPerformed(ActionEvent e) {
      changeColor(handler.getSelectedTierLabels(), false);
    }
  };

  private final Action color_by_score_on_action = new AbstractAction("Color By Score ON") {
    public void actionPerformed(ActionEvent e) {
      setColorByScore(handler.getSelectedTierLabels(), true);
    }
  };

  private final Action color_by_score_off_action = new AbstractAction("Color By Score OFF") {
    public void actionPerformed(ActionEvent e) {
      setColorByScore(handler.getSelectedTierLabels(), false);
    }
  };

  private final Action show_two_tiers = new AbstractAction("Show 2 tracks (+) and (-)") {
    public void actionPerformed(ActionEvent e) {
      setTwoTiers(handler.getSelectedTierLabels(), true);
    }
  };

  private final Action show_single_tier = new AbstractAction("Show 1 track (+/-)") {
    public void actionPerformed(ActionEvent e) {
      setTwoTiers(handler.getSelectedTierLabels(), false);
    }
  };
  
  private final Action sym_summarize_single_action = new AbstractAction("") {
    public void actionPerformed(ActionEvent e) {
      List current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() > 1) {
        ErrorHandler.errorPanel("Must select only one track");
      }
      TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
      addSymSummaryTier(current_tier,false);
    }
  };

  private final Action sym_summarize_both_action = new AbstractAction("") {
    public void actionPerformed(ActionEvent e) {
      List current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() > 1) {
        ErrorHandler.errorPanel("Must select only one track");
      }
      TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
      addSymSummaryTier(current_tier,true);
    }
  };

  private final Action coverage_action = new AbstractAction("Make Annotation Coverage Track") {
    public void actionPerformed(ActionEvent e) {
      List current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() > 1) {
        ErrorHandler.errorPanel("Must select only one track");
      }
      TierGlyph current_tier = (TierGlyph) current_tiers.get(0);
      addSymCoverageTier(current_tier);
    }
  };
  private final Action save_bed_action = new AbstractAction("Save track as BED file") {
    public void actionPerformed(ActionEvent e) {
      List<TierGlyph> current_tiers = handler.getSelectedTiers();
      if (current_tiers.size() > 1) {
        ErrorHandler.errorPanel("Must select only one track");
      }
      TierGlyph current_tier = current_tiers.get(0);
      saveAsBedFile(current_tier);
    }
  };

  private final Action change_expand_max_action = new AbstractAction("Adjust Max Expand") {
    public void actionPerformed(ActionEvent e) {
      changeExpandMax(handler.getSelectedTierLabels());
    }
  };

  private final Action change_expand_max_all_action = new AbstractAction("Adjust Max Expand All") {
    public void actionPerformed(ActionEvent e) {
      changeExpandMax(handler.getAllTierLabels());
    }
  };

  
  private final Action delete_action = new AbstractAction("Delete selected tracks") {
    public void actionPerformed(ActionEvent e) {
		BioSeq seq = gmodel.getSelectedSeq();

      if (IGB.confirmPanel("Really remove selected tracks?\n"+
          "Data will be removed from "+ seq.getID() +" on this genome.")) {
        removeTiers(handler.getSelectedTierLabels());
      }
    }
  };

	public SeqMapViewPopup(TierLabelManager handler, SeqMapView smv) {
		this.handler = handler;
		this.gviewer = smv;
		at1 = new ActionToggler(smv.getSeqMap().show_plus_action);
		at2 = new ActionToggler(smv.getSeqMap().show_minus_action);
		at3 = new ActionToggler(smv.getSeqMap().show_mixed_action);
	}

  private void showCustomizer() {
    PreferencesPanel pv = PreferencesPanel.getSingleton();
    pv.setTab(PreferencesPanel.TAB_NUM_TIERS);
    JFrame f = pv.getFrame();
    f.setVisible(true);
  }

  List<ITrackStyle> getStyles(List<TierLabelGlyph> tier_label_glyphs) {
		if (tier_label_glyphs.isEmpty()) {
			return Collections.<ITrackStyle>emptyList();
		}

		// styles is a list of styles with no duplicates, so a Set rather than a List
		// might make sense.  But at the moment it seems faster to use a List
		List<ITrackStyle> styles = new ArrayList<ITrackStyle>(tier_label_glyphs.size());

		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = tlg.getReferenceTier();
			ITrackStyle tps = tier.getAnnotStyle();
			if (tps != null && !styles.contains(tps)) {
				styles.add(tps);
			}
		}
		return styles;
	}

  private void setTiersCollapsed(List<TierLabelGlyph> tier_labels, boolean collapsed) {
    handler.setTiersCollapsed(tier_labels, collapsed);
    refreshMap(true,true);
  }

  public void changeExpandMax(List<TierLabelGlyph> tier_labels) {
    if (tier_labels == null || tier_labels.isEmpty()) {
      ErrorHandler.errorPanel("changeExpandMaxAll called with an empty list");
      return;
    }

    String initial_value = "0";
    if (tier_labels.size() == 1) {
      TierLabelGlyph tlg = tier_labels.get(0);
      TierGlyph tg = (TierGlyph) tlg.getInfo();
      ITrackStyle style = tg.getAnnotStyle();
      if (style != null) { initial_value = "" + style.getMaxDepth(); }
    }

    String input =
      (String)JOptionPane.showInputDialog(null,
					  "Enter new maximum track height, 0 for unlimited",
					  "Change Selected Tracks Max Height", JOptionPane.PLAIN_MESSAGE,
					  null, null, initial_value);

    if ( input == null || input.equals(JOptionPane.UNINITIALIZED_VALUE)) {
      return;
    }

    int newmax;
    try {
      newmax = Integer.parseInt(input);
    }
    catch (NumberFormatException ex) {
      ErrorHandler.errorPanel("Couldn't parse new track max '"+input+"'");
      return;
    }

    changeExpandMax(tier_labels, newmax);
  }

	private void changeExpandMax(List<TierLabelGlyph> tier_label_glyphs, int max) {
		for (TierLabelGlyph tlg : tier_label_glyphs) {
			TierGlyph tier = (TierGlyph) tlg.getInfo();
			ITrackStyle style = tier.getAnnotStyle();
			style.setMaxDepth(max);
			tier.setMaxExpandDepth(max);
		}
		refreshMap(false,true);
	}

  private void setTwoTiers(List<TierLabelGlyph> tier_label_glyphs, boolean b) {
    for (TierLabelGlyph tlg : tier_label_glyphs) {
      TierGlyph tier = (TierGlyph) tlg.getInfo();
      ITrackStyle style = tier.getAnnotStyle();
      if (style instanceof ITrackStyleExtended) {
        ((ITrackStyleExtended) style).setSeparate(b);
      }
    }
    refreshMap(false,true);
    handler.sortTiers();
  }

  void showAllTiers() {
	  List<TierLabelGlyph> tiervec = handler.getAllTierLabels();

	  for (TierLabelGlyph label : tiervec) {
		  TierGlyph tier = (TierGlyph) label.getInfo();
		  ITrackStyle style = tier.getAnnotStyle();
		  if (style != null) {
			  style.setShow(true);
			  tier.setVisibility(true);
		  }
	  }
	  showMenu.removeAll();
	  handler.sortTiers();
	  refreshMap(false,true); // when re-showing all tier, do strech_to_fit in the y-direction
	}

  /** Hides one tier and creates a JMenuItem that can be used to show it again.
   *  Does not re-pack the given tier, or any other tiers.
   */
  private void hideOneTier(final TierGlyph tier) {
    final ITrackStyle style = tier.getAnnotStyle();
	  // if style.getShow() is already false, there is likely a bug somewhere!
	  if (style == null) {
		  return;
	  }
    if (style.getShow()) {
      style.setShow(false);
      final JMenuItem show_tier = new JMenuItem() {
        // override getText() because the HumanName of the style might change
				@Override
        public String getText() {
          String name = style.getHumanName();
          if (name == null) { name = "<unnamed>"; }
          if (name.length() > 30) {
            name = name.substring(0,30) + "...";
          }
          return name;
        }
      };
      show_tier.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          style.setShow(true);
          showMenu.remove(show_tier);
		  handler.sortTiers();
		  handler.repackTheTiers(false, true);
        }
      });
      showMenu.add(show_tier);
    }
	tier.setVisibility(false);
  }

  /** Hides multiple tiers and then repacks.
   *  @param tiers  a List of GlyphI objects for each of which getInfo() returns a TierGlyph.
   */
  void hideTiers(List<TierLabelGlyph> tiers) {
    for (TierLabelGlyph g : tiers) {
      if (g.getInfo() instanceof TierGlyph) {
        TierGlyph tier = (TierGlyph) g.getInfo();
        hideOneTier(tier);
      }
    }

	handler.repackTheTiers(false, true);

	/** Possible bug : When all strands are hidden.
	 * tier label and tier do appear at same position.
	 **/

	// NOTE: Below call to stretchToFit is not redundancy. It is there
	//       to solve above mentioned bug.
	handler.repackTheTiers(false, true);
  }

  private void changeColor(final List<TierLabelGlyph> tier_label_glyphs, final boolean fg) {
    if (tier_label_glyphs.isEmpty()) {
      return;
    }

    final JColorChooser chooser = new JColorChooser();

    TierLabelGlyph tlg_0 = tier_label_glyphs.get(0);
    TierGlyph tier_0 = (TierGlyph) tlg_0.getInfo();
    ITrackStyle style_0 = tier_0.getAnnotStyle();
    if (style_0 != null) {
      if (fg) {
        chooser.setColor(style_0.getColor());
      } else {
        chooser.setColor(style_0.getBackground());
      }
    }

    ActionListener al = new ActionListener() {

		  public void actionPerformed(ActionEvent e) {
			  for (TierLabelGlyph tlg : tier_label_glyphs) {
				  TierGlyph tier = (TierGlyph) tlg.getInfo();
				  ITrackStyle style = tier.getAnnotStyle();

				  if (style != null) {
					  if (fg) {
						  style.setColor(chooser.getColor());
					  } else {
						  style.setBackground(chooser.getColor());
					  }
				  }
				  for (GraphGlyph gg : TierLabelManager.getContainedGraphs(tier_label_glyphs)) {
					  if (fg) {
						  gg.setColor(chooser.getColor());
						  gg.getGraphState().getTierStyle().setColor(chooser.getColor());
					  } else {
						  gg.getGraphState().getTierStyle().setBackground(chooser.getColor());
					  }
				  }
			  }
		  }
    };

    JDialog dialog = JColorChooser.createDialog((java.awt.Component) null, // parent
                                        "Pick a Color",
                                        true,  //modal
                                        chooser,
                                        al,  //OK button handler
                                        null); //no CANCEL button handler
    dialog.setVisible(true);

    refreshMap(false,false);
  }

  public void renameTier(final TierGlyph tier) {
    if (tier == null) {
      return;
    }
    ITrackStyle style = tier.getAnnotStyle();

    String new_label = JOptionPane.showInputDialog("Label: ", style.getHumanName());
    if (new_label != null && new_label.length() > 0) {
      style.setHumanName(new_label);
    }
    refreshMap(false,false);
  }

  private void setColorByScore(List<TierLabelGlyph> tier_labels, boolean b) {
    for (TierLabelGlyph tlg : tier_labels) {
      ITrackStyle style = tlg.getReferenceTier().getAnnotStyle();
      if (style instanceof ITrackStyleExtended) {
        ITrackStyleExtended astyle = (ITrackStyleExtended) style;
        astyle.setColorByScore(b);
      }
    }

    refreshMap(false,false);
  }



  private static void saveAsBedFile(TierGlyph atier) {
		int childcount = atier.getChildCount();
		List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(childcount);
		for (int i = 0; i < childcount; i++) {
			GlyphI child = atier.getChild(i);
			if (child.getInfo() instanceof SeqSymmetry) {
				syms.add((SeqSymmetry)child.getInfo());
			}
		}
		System.out.println("Saving symmetries as BED file: " + syms.size());

		JFileChooser chooser = UniFileChooser.getFileChooser("Bed file", "bed");
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());

		int option = chooser.showSaveDialog(null);
		if (option == JFileChooser.APPROVE_OPTION) {
			FileTracker.DATA_DIR_TRACKER.setFile(chooser.getCurrentDirectory());
			BioSeq aseq = gmodel.getSelectedSeq();
			DataOutputStream dos = null;
			try {
				File fil = chooser.getSelectedFile();
				dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(fil)));
				BedParser.writeBedFormat(dos, syms, aseq);
			} catch (Exception ex) {
				ErrorHandler.errorPanel("Problem saving file", ex);
			} finally {
				GeneralUtils.safeClose(dos);
			}
		}
	}

  private static void collectSyms(GlyphI gl, List<SeqSymmetry> syms) {
		Object info = gl.getInfo();
		if ((info != null) && (info instanceof SeqSymmetry)) {
			syms.add((SeqSymmetry) info);
		} else if (gl.getChildCount() > 0) {
			// if no SeqSymmetry associated with glyph, descend and try children
			int child_count = gl.getChildCount();
			for (int i = 0; i < child_count; i++) {
				collectSyms(gl.getChild(i), syms);
			}
		}
	}

  private void addSymCoverageTier(TierGlyph atier) {
    BioSeq aseq = gmodel.getSelectedSeq();
    //int child_count = atier.getChildCount();
    //List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>(child_count);
    //collectSyms(atier, syms);

//	  TODO: If tierglyph is empty then it is never displayed. So check when below mentioned condition is met.
    //if (child_count == 0 || syms.size() == 0) {
    //  ErrorHandler.errorPanel("Empty Track",
    //    "The selected track is empty.  Can not make a coverage track for an empty track.");
    //  return;
    //}

	String human_name = "coverage: " + atier.getLabel();
	String unique_name = TrackStyle.getUniqueName(human_name);
	String method = atier.getAnnotStyle().getMethodName();
	DependentData dd = new DependentData(unique_name,DependentType.COVERAGE,method);
	SymWithProps wrapperSym = TrackView.addToDependentList(dd);

	if (wrapperSym == null) {
		ErrorHandler.errorPanel("Empty Track",
			"The selected track is empty.  Can not make a coverage track for an empty track.");
		return;
    }
	
    // Generate a non-persistent style.
    // Factory will be CoverageSummarizerFactory because name starts with "coverage:"

    TrackStyle style = TrackStyle.getInstance(unique_name, false);
    style.setHumanName(human_name);
    style.setGlyphDepth(1);
    style.setSeparate(false); // there are not separate (+) and (-) strands
    style.setExpandable(false); // cannot expand and collapse
    style.setCustomizable(false); // the user can change the color, but not much else is meaningful

    gviewer.setAnnotatedSeq(aseq, true, true);
  }


  private void addSymSummaryTier(TierGlyph atier, boolean bothDirection) {
    // not sure best way to collect syms from tier, but for now,
    //   just recursively descend through child glyphs of the tier, and if
    //   childA.getInfo() is a SeqSymmetry, add to symmetry list and prune recursion
    //   (don't descend into childA's children)


//    List<SeqSymmetry> syms = new ArrayList<SeqSymmetry>();
//    collectSyms(atier, syms);

//	  TODO: If tierglyph is empty then it is never displayed. So check when below mentioned condition is met.
    //if (syms.size() == 0) {
    //ErrorHandler.errorPanel("Nothing to Summarize",
    //    "The selected track is empty. It contains nothing to summarize");
    //return;
    //}

    BioSeq aseq = gmodel.getSelectedSeq();
	String id = null;
	DependentData dd = null;
	String method = atier.getAnnotStyle().getMethodName();
	if(bothDirection){
		id = atier.getLabel() + getSymbol(Direction.BOTH);
		dd = new DependentData(id,DependentType.SUMMARY,method,Direction.BOTH);
	}else{
		id = atier.getLabel() + getSymbol(atier.getDirection());
		dd = new DependentData(id,DependentType.SUMMARY,method,atier.getDirection());
	}
	
	GraphSym gsym = (GraphSym) TrackView.addToDependentList(dd);

	if (gsym == null) {
		ErrorHandler.errorPanel("Nothing to Summarize",
			"The selected track is empty. It contains nothing to summarize");
		return;
    }

	gsym.setGraphName("depth: " + id);
    gviewer.setAnnotatedSeq(aseq, true, true);
    GraphGlyph gl = (GraphGlyph)gviewer.getSeqMap().getItem(gsym);
    gl.setGraphStyle(GraphType.STAIRSTEP_GRAPH);
    gl.setColor(atier.getForegroundColor());
  }

  void refreshMap(boolean stretch_vertically, boolean stretch_horizonatally) {
    if (gviewer != null) {
      // if an AnnotatedSeqViewer is being used, ask it to update itself.
      // later this can be made more specific to just update the tiers that changed
      boolean preserve_view_x = ! stretch_vertically;
	  boolean preserve_view_y = ! stretch_horizonatally;
      gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq(), true, preserve_view_x, preserve_view_y);
    } else {
      // if no AnnotatedSeqViewer (as in simple test programs), update the tiermap itself.
      handler.repackTheTiers(false, stretch_vertically);
    }
  }

  public void popupNotify(javax.swing.JPopupMenu popup, TierLabelManager handler) {
    List<TierLabelGlyph> labels = handler.getSelectedTierLabels();
    int num_selections = labels.size();
    boolean not_empty = ! handler.getAllTierLabels().isEmpty();

    boolean any_are_collapsed = false;
    boolean any_are_expanded = false;
    boolean any_are_color_on = false; // whether any allow setColorByScore()
    boolean any_are_color_off = false; // whether any allow setColorByScore()
    boolean any_are_separate_tiers = false;
    boolean any_are_single_tier = false;

	for (TierLabelGlyph label : labels) {
      TierGlyph glyph = label.getReferenceTier();
      ITrackStyle style = glyph.getAnnotStyle();
      if (style instanceof ITrackStyleExtended) {
        ITrackStyleExtended astyle = (ITrackStyleExtended) style;
        any_are_color_on = any_are_color_on || astyle.getColorByScore();
        any_are_color_off = any_are_color_off || (! astyle.getColorByScore());
        any_are_separate_tiers = any_are_separate_tiers || astyle.getSeparate();
        any_are_single_tier = any_are_single_tier || (! astyle.getSeparate());
      }
      if (style.getExpandable()) {
        any_are_collapsed = any_are_collapsed || style.getCollapsed();
        any_are_expanded = any_are_expanded || ! style.getCollapsed();
      }
    }

    select_all_tiers_action.setEnabled(true);
    customize_action.setEnabled(true);

    hide_action.setEnabled(num_selections > 0);
	delete_action.setEnabled(num_selections > 0);
    show_all_action.setEnabled(not_empty);

    change_color_action.setEnabled(num_selections > 0);
    change_bg_color_action.setEnabled(num_selections > 0);
    rename_action.setEnabled(num_selections == 1);

    color_by_score_on_action.setEnabled(any_are_color_off);
    color_by_score_off_action.setEnabled(any_are_color_on);

    collapse_action.setEnabled(any_are_expanded);
    expand_action.setEnabled(any_are_collapsed);
    change_expand_max_action.setEnabled(any_are_expanded);
	show_single_tier.setEnabled(any_are_separate_tiers);
    show_two_tiers.setEnabled(any_are_single_tier);
    collapse_all_action.setEnabled(not_empty);
    expand_all_action.setEnabled(not_empty);
    change_expand_max_all_action.setEnabled(not_empty);
    showMenu.setEnabled(showMenu.getMenuComponentCount() > 0);

    JMenu save_menu = new JMenu("Save Annotations");

    if (num_selections == 1) {
      // Check whether this selection is a graph or an annotation
      TierLabelGlyph label = labels.get(0);
      TierGlyph glyph = (TierGlyph) label.getInfo();
      ITrackStyle style = glyph.getAnnotStyle();
      boolean is_annotation_type = ! style.isGraphTier();
	  summaryMenu.setEnabled(is_annotation_type);
	  sym_summarize_single_action.putValue(sym_summarize_single_action.NAME, glyph.getLabel() + getSymbol(glyph.getDirection()));
	  sym_summarize_both_action.putValue(sym_summarize_both_action.NAME, glyph.getLabel() + getSymbol(Direction.BOTH));
      //sym_summarize_single_action.setEnabled(is_annotation_type);
      coverage_action.setEnabled(is_annotation_type);
      save_menu.setEnabled(is_annotation_type);
      save_bed_action.setEnabled(is_annotation_type);
    } else {
	  summaryMenu.setEnabled(false);
      //sym_summarize_single_action.setEnabled(false);
      coverage_action.setEnabled(false);
      save_menu.setEnabled(false);
      save_bed_action.setEnabled(false);
    }

    changeMenu.removeAll();
    changeMenu.add(change_color_action);
    changeMenu.add(change_bg_color_action);
    changeMenu.add(rename_action);
    changeMenu.add(change_expand_max_action);
    changeMenu.add(new JSeparator());
    changeMenu.add(show_two_tiers);
    changeMenu.add(show_single_tier);
    changeMenu.add(new JSeparator());
    changeMenu.add(color_by_score_on_action);
    changeMenu.add(color_by_score_off_action);

    popup.add(customize_action);
    popup.add(new JSeparator());
    popup.add(hide_action);
	popup.add(delete_action);
    popup.add(showMenu);
    popup.add(show_all_action);

	strandsMenu.removeAll();
	strandsMenu.add(at1);
	strandsMenu.add(at2);
	strandsMenu.add(at3);
	popup.add(strandsMenu);
    popup.add(new JSeparator());
    popup.add(select_all_tiers_action);
    popup.add(changeMenu);
    popup.add(new JSeparator());
    popup.add(collapse_action);
    popup.add(expand_action);
    popup.add(change_expand_max_action);
    popup.add(new JSeparator());

    popup.add(save_menu);
    save_menu.add(save_bed_action);

    popup.add(new JSeparator());
	summaryMenu.removeAll();
	summaryMenu.add(sym_summarize_single_action);
	if(!show_two_tiers.isEnabled())					// If showing both track then give a option to create
		summaryMenu.add(sym_summarize_both_action); // depth graph in both direction.
	
    popup.add(summaryMenu);
    popup.add(coverage_action);

    if (DEBUG) {
      popup.add(new AbstractAction("DEBUG") {
        public void actionPerformed(ActionEvent e) {
          doDebugAction();
        }
      });
    }
  }

  private void removeTiers(List<TierLabelGlyph> tiers) {
	  for (TierLabelGlyph tlg: tiers) {
		  TrackView.deleteTrack(tlg.getReferenceTier());
	  }
	  gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq());	// refresh
  }
  
  // purely for debugging
	private void doDebugAction() {
		for (TierGlyph tg : handler.getSelectedTiers()) {
			ITrackStyle style = tg.getAnnotStyle();
			System.out.println("Track: " + tg);
			System.out.println("Style: " + style);
		}
	}

	static private String getSymbol(Direction direction){
		return TierLabelGlyph.getDirectionSymbol(direction);
	}

	SeqMapView getSeqMapView() {
		return gviewer;
	}
}
