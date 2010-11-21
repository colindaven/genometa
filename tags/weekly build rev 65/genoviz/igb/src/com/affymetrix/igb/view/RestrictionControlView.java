/**
 *   Copyright (c) 2001-2004 Affymetrix, Inc.
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

import java.awt.event.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.util.DNAUtils;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genoviz.bioviews.GlyphI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genoviz.widget.NeoMap;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.tiers.AffyTieredMap;
import com.affymetrix.igb.tiers.TransformTierGlyph;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.regex.Pattern;

public final class RestrictionControlView extends JComponent
				implements ListSelectionListener, ActionListener {

	private final SeqMapView gviewer;
	private final Map<String,String> site_hash = new HashMap<String,String>();
	private JList siteList;
	private JPanel labelP;
	private final List<String> sites = new ArrayList<String>();
	private static Color colors[] = {
		Color.magenta,
		new Color(0x00cd00),
		Color.orange,
		new Color(0x00d7d7),
		new Color(0xb50000),
		Color.blue,
		Color.gray,
		Color.pink};//Distinct Colors for View/Print Ease
	private JLabel labels[];
	private JButton actionB;
	private JButton clearB;

	/**
	 *  keep track of added glyphs
	 */
	private final List<GlyphI> glyphs = new ArrayList<GlyphI>();

	public RestrictionControlView() {
		super();
		this.gviewer = Application.getSingleton().getMapView();
		boolean load_success = true;

		String rest_file = "/rest_enzymes";
		InputStream file_input_str =
						Application.class.getResourceAsStream(rest_file);
		
		if (file_input_str == null) {
			ErrorHandler.errorPanel("Cannot open restriction enzyme file",
							"Cannot find restriction enzyme file '" + rest_file + "'.\n" +
							"Restriction mapping will not be available.");
		}

		BufferedReader d = null;

		if (file_input_str == null) {
			load_success = false;
		} else {
			try {
				//Loading the name of all the restriction sites to GUI
				d = new BufferedReader(new InputStreamReader(file_input_str));
				StringTokenizer string_toks;
				String site_name, site_dna;
				String reply_string;
				//    String reply_string = distr.readLine();
				//int rcount = 0;
				while ((reply_string = d.readLine()) != null) {
					//	System.out.println(reply_string);
					string_toks = new StringTokenizer(reply_string);
					site_name = string_toks.nextToken();
					site_dna = string_toks.nextToken();
					site_hash.put(site_name, site_dna);
					sites.add(site_name);
					//rcount++;
				}
			} catch (Exception ex) {
				load_success = false;
				ErrorHandler.errorPanel("Problem loading restriction site file, aborting load\n" +
								ex.toString());
			} finally {
				GeneralUtils.safeClose(d);
				GeneralUtils.safeClose(file_input_str);
			}
		}

		if (load_success) {
			siteList = new JList(sites.toArray());
			JScrollPane scrollPane = new JScrollPane(siteList);
			labelP = new JPanel();
			labelP.setBackground(Color.white);
			labelP.setLayout(new GridLayout(sites.size(), 1));

			labels = new JLabel[sites.size()];
			JLabel label;
			for (int i = 0; i < labels.length; i++) {//Make a label for the selected pane for each restriction enzyme
				label = new JLabel();
				label.setForeground(colors[i%colors.length]);//We're repeating the colors..deal with it, users.
				label.setText("           ");
				labelP.add(label);
				labels[i] = label;
			}

			this.setLayout(new BorderLayout());
			scrollPane.setPreferredSize(new Dimension(100, 100));

			this.add("West", scrollPane);
			actionB = new JButton("Map Selected Restriction Sites");
			clearB = new JButton("Clear");
			this.add("Center", new JScrollPane(labelP));

			Container button_container = new JPanel();
			button_container.setLayout(new GridLayout(5, 1));
			button_container.add(actionB);
			button_container.add(clearB);
			this.add("East", button_container);

			siteList.addListSelectionListener(this);
			actionB.addActionListener(this);
			clearB.addActionListener(this);
		} else {
			this.setLayout(new BorderLayout());
			JLabel lab = new JLabel("Restriction site mapping not available.");
			this.add("North", lab);
		}
	}

	public void valueChanged(ListSelectionEvent evt) {
		Object src = evt.getSource();
		if (src == siteList) {
			Object[] selected_names = siteList.getSelectedValues();
			for (int i = 0; i < labels.length; i++) {
				if (i < selected_names.length) {
					labels[i].setText((String) (selected_names[i]));
				} else {
					labels[i].setText("");
				}
			}
		}
	}

	private void clearAll() {
		clearGlyphs();
		siteList.clearSelection();
	}

	private void clearGlyphs() {
		AffyTieredMap map = gviewer.getSeqMap();
		map.removeItem(glyphs);
		glyphs.clear();
	}

	public void actionPerformed(ActionEvent evt) {
		if (evt.getSource() == clearB) {
			clearAll();
			return;
		}

		clearGlyphs();

		BioSeq vseq = gviewer.getViewSeq();
		if (vseq == null || !vseq.isComplete()) {
			ErrorHandler.errorPanel("Residues for seq not available, search aborted.");
			return;
		}
		
		new Thread(new GlyphifyMatchesThread()).start();
	}

	

	private class GlyphifyMatchesThread implements Runnable
	{
		public void run()
		{
			Application.getSingleton().addNotLockedUpMsg("Finding Restriction Sites... ");
			try{
				BioSeq vseq = gviewer.getViewSeq();
				if (vseq == null || !vseq.isComplete()) {
					ErrorHandler.errorPanel("Residues for seq not available, search aborted.");
					return;
				}
				NeoMap map = gviewer.getSeqMap();
				TransformTierGlyph axis_tier = gviewer.getAxisTier();
				int residue_offset = vseq.getMin();
				String residues = vseq.getResidues();
				// Search for reverse complement of query string
				String rev_searchstring = DNAUtils.reverseComplement(residues);

				for (int i = 0; i < labels.length; i++) {
					String site_name = labels[i].getText();
					// done when hit first non-labelled JLabel
					if (site_name == null || site_name.equals("")) {
						break;
					}
					String site_residues = site_hash.get(site_name);
					if (site_residues == null) {
						continue;
					}
					Pattern regex = null;
					try {
						regex = Pattern.compile(site_residues, Pattern.CASE_INSENSITIVE);
					} catch (Exception ex) {
						ex.printStackTrace();
						continue;
					}

					System.out.println("searching for occurrences of \"" + site_residues + "\" in sequence");

					residue_offset = vseq.getMin();
					int hit_count1 = SearchView.searchForRegexInResidues(
							true, regex, residues, residue_offset, axis_tier, glyphs, colors[i % colors.length]);

					// Search for reverse complement of query string
					//   flip searchstring around, and redo nibseq search...
					residue_offset = vseq.getMax();
					int hit_count2 = SearchView.searchForRegexInResidues(
							false, regex, rev_searchstring, residue_offset, axis_tier, glyphs, colors[i % colors.length]);

					System.out.println(site_residues + ": " + hit_count1 + " forward strand hits and " + hit_count2 + " reverse strand hits");
					map.updateWidget();
				}
			}finally{
				Application.getSingleton().removeNotLockedUpMsg("Finding Restriction Sites... ");
			}
		}
	}
}
