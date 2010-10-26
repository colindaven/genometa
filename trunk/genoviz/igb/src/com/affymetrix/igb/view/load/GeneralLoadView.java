package com.affymetrix.igb.view.load;

import com.affymetrix.genometryImpl.event.GenericServerInitEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometryImpl.SeqSpan;

import com.affymetrix.genometryImpl.util.LoadUtils.LoadStrategy;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.comparator.StringVersionDateComparator;
import com.affymetrix.genometryImpl.event.GenericServerInitListener;
import com.affymetrix.genometryImpl.event.GroupSelectionEvent;
import com.affymetrix.genometryImpl.event.GroupSelectionListener;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;

import com.affymetrix.genometryImpl.general.GenericFeature;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.general.GenericVersion;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerStatus;
import com.affymetrix.genometryImpl.util.SpeciesLookup;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.general.Persistence;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.util.ThreadUtils;
import com.affymetrix.igb.view.SeqMapView;
import com.affymetrix.igb.IGBConstants;
import com.affymetrix.igb.action.RefreshDataAction;
import com.affymetrix.igb.util.JComboBoxToolTipRenderer;
import com.affymetrix.igb.util.JComboBoxWithSingleListener;
import com.affymetrix.igb.util.ScriptFileLoader;
import java.text.MessageFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;
import javax.swing.table.TableColumn;

public final class GeneralLoadView extends JComponent
				implements ItemListener, ActionListener, GroupSelectionListener, SeqSelectionListener, GenericServerInitListener {

	private static final boolean DEBUG_EVENTS = false;
	private static final GenometryModel gmodel = GenometryModel.getGenometryModel();
	private static final String SELECT_SPECIES = IGBConstants.BUNDLE.getString("speciesCap");
	private static final String SELECT_GENOME = IGBConstants.BUNDLE.getString("genomeVersionCap");
	private static final String CHOOSE = "Choose";
	private static final String LOAD = IGBConstants.BUNDLE.getString("load");
	private AnnotatedSeqGroup curGroup = null;
	private final JComboBox versionCB;
	private final JComboBoxToolTipRenderer versionCBRenderer;
	private final JComboBox speciesCB;
	private final JComboBoxToolTipRenderer speciesCBRenderer;
	private final JButton all_residuesB;
	private final JButton partial_residuesB;
	private final RefreshDataAction refreshDataAction;
	private static final SeqMapView gviewer = Application.getSingleton().getMapView();
	private static JTableX feature_table;
	public static FeaturesTableModel feature_model;
	JScrollPane featuresTableScrollPane;
	private final FeatureTreeView feature_tree_view;
	//private TrackInfoView track_info_view;
	private volatile boolean lookForPersistentGenome = true;	// Once this is set to false, don't invoke persistent genome code

	private static GeneralLoadView singleton;

	private GeneralLoadView() {
		this.setLayout(new BorderLayout());

		JPanel choicePanel = new JPanel();
		choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.X_AXIS));
		choicePanel.setBorder(BorderFactory.createEmptyBorder(2, 4, 4, 4));

		speciesCB = new JComboBoxWithSingleListener();
		speciesCB.addItem(SELECT_SPECIES);
		speciesCB.setMaximumSize(new Dimension(speciesCB.getPreferredSize().width*4,speciesCB.getPreferredSize().height));
		speciesCB.setEnabled(false);
		speciesCB.setEditable(false);
		speciesCB.setToolTipText(CHOOSE + " " + SELECT_SPECIES);

		speciesCBRenderer = new JComboBoxToolTipRenderer();
		speciesCB.setRenderer(speciesCBRenderer);
		speciesCBRenderer.setToolTipEntry(SELECT_SPECIES, CHOOSE + " " + SELECT_SPECIES);
		
		choicePanel.add(new JLabel(CHOOSE + ":"));
		choicePanel.add(Box.createHorizontalStrut(5));
		choicePanel.add(speciesCB);
		choicePanel.add(Box.createHorizontalStrut(50));

		versionCB = new JComboBoxWithSingleListener();
		versionCB.addItem(SELECT_GENOME);
		versionCB.setMaximumSize(new Dimension(versionCB.getPreferredSize().width*4, versionCB.getPreferredSize().height));
		versionCB.setEnabled(false);
		versionCB.setEditable(false);
		versionCB.setToolTipText(CHOOSE + " " + SELECT_GENOME);

		versionCBRenderer = new JComboBoxToolTipRenderer();
		versionCB.setRenderer(versionCBRenderer);
		versionCBRenderer.setToolTipEntry(SELECT_GENOME, CHOOSE + " " + SELECT_GENOME);

		choicePanel.add(versionCB);


		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new GridLayout(1, 3));

		all_residuesB = new JButton(MessageFormat.format(LOAD,IGBConstants.BUNDLE.getString("allSequenceCap")));
		all_residuesB.setToolTipText(MessageFormat.format(LOAD,IGBConstants.BUNDLE.getString("nucleotideSequence")));
		all_residuesB.setMaximumSize(all_residuesB.getPreferredSize());
		all_residuesB.setEnabled(false);
		all_residuesB.addActionListener(this);
		buttonPanel.add(all_residuesB);
		partial_residuesB = new JButton(MessageFormat.format(LOAD,IGBConstants.BUNDLE.getString("sequenceInViewCap")));
		partial_residuesB.setToolTipText(MessageFormat.format(LOAD,IGBConstants.BUNDLE.getString("partialNucleotideSequence")));
		partial_residuesB.setMaximumSize(partial_residuesB.getPreferredSize());
		partial_residuesB.setEnabled(false);
		
		partial_residuesB.addActionListener(this);
		buttonPanel.add(partial_residuesB);
		this.refreshDataAction = RefreshDataAction.getAction();
		JButton refresh_dataB = new JButton(refreshDataAction);
		refresh_dataB.setIcon(null);
		refresh_dataB.setMaximumSize(refresh_dataB.getPreferredSize());
		refreshDataAction.setEnabled(false);
		buttonPanel.add(refresh_dataB);
		this.add("South", buttonPanel);

		feature_model = new FeaturesTableModel(this);
		feature_table = new JTableX(feature_model);
		feature_table.setModel(feature_model);
		feature_table.setRowHeight(20);    // TODO: better than the default value of 16, but still not perfect.
		// Handle sizing of the columns
		feature_table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);   // Allow columns to be resized
		
		featuresTableScrollPane = new JScrollPane(GeneralLoadView.feature_table);
		featuresTableScrollPane.setViewportView(feature_table);

		JPanel featuresPanel = new JPanel();
		featuresPanel.setLayout(new BoxLayout(featuresPanel, BoxLayout.Y_AXIS));
		featuresPanel.add(new JLabel(IGBConstants.BUNDLE.getString("chooseLoadMode")));
		featuresPanel.add(featuresTableScrollPane);


		this.add("North", choicePanel);

		/* COMMENTED OUT.  The Track Info table makes the data load view
		 *                 too busy, so for now, the code is commented out
		 */
//		track_info_view = new TrackInfoView();		
//		JSplitPane featurePane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, featuresPanel, track_info_view);		
//		featurePane.setResizeWeight(0.5);		
//		JSplitPane jPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.feature_tree_view, featurePane);

		this.feature_tree_view = new FeatureTreeView();
		JSplitPane jPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, this.feature_tree_view, featuresPanel);
		jPane.setResizeWeight(0.5);		
		this.add("Center", jPane);

		this.setBorder(BorderFactory.createEtchedBorder());

		ServerList.addServerInitListener(this);

		GeneralLoadUtils.loadServerMapping();
		populateSpeciesData();
		addListeners();
	}

	private void addListeners() {
		gmodel.addGroupSelectionListener(this);
		gmodel.addSeqSelectionListener(this);

		speciesCB.setEnabled(true);
		versionCB.setEnabled(true);
		speciesCB.addItemListener(this);
		versionCB.addItemListener(this);

	}

	public static synchronized GeneralLoadView getLoadView() {
		if (singleton == null) {
			singleton = new GeneralLoadView();
		}

		return singleton;
	}

	/**
	 * Discover servers, species, etc., asynchronously.
	 * @param loadGenome parameter to check if genomes should be loaded from
	 * actual server or not.
	 */
	private void populateSpeciesData() {
		for (final GenericServer gServer : ServerList.getEnabledServers()) {
			Executor vexec = Executors.newSingleThreadExecutor();
			SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
				protected Void doInBackground() throws Exception {
					Application.getSingleton().addNotLockedUpMsg("Loading server " + gServer + " (" + gServer.serverType.toString() + ")");
					GeneralLoadUtils.discoverServer(gServer);
					return null;
				}
			};

			vexec.execute(worker);
		}
	}

	public void genericServerInit(GenericServerInitEvent evt) {
		boolean areAllServersInited = ServerList.areAllServersInited();	// do this first to avoid race condition
		GenericServer gServer = (GenericServer)evt.getSource();

		if (gServer.getServerStatus() == ServerStatus.NotResponding){
			refreshTreeView();
			return;
		}
		
		if (gServer.getServerStatus() != ServerStatus.Initialized) {
			return;	// ignore uninitialized servers
		}

		if (gServer.serverType != ServerType.LocalFiles) {
			Application.getSingleton().removeNotLockedUpMsg("Loading server " + gServer + " (" + gServer.serverType.toString() + ")");
		}

		// Need to refresh species names
		boolean speciesListener = this.speciesCB.getItemListeners().length > 0;
		String speciesName = (String)this.speciesCB.getSelectedItem();
		refreshSpeciesCB();
		
		if (speciesName != null && !speciesName.equals(SELECT_SPECIES)) {
			lookForPersistentGenome = false;
			String versionName = (String)this.versionCB.getSelectedItem();

			//refresh version names if a species is selected
			refreshVersionCB(speciesName);

			if (versionName != null && !versionName.equals(SELECT_GENOME)) {
				// refresh this version
				initVersion(versionName);

				// TODO: refresh feature tree view if a version is selected
				refreshTreeView();
			}
		}

		if (speciesListener) {
			this.speciesCB.addItemListener(this);
		}

		if (areAllServersInited) {
			runBatchOrRestore();
		}

	}

	private void runBatchOrRestore() {
		try {
			// Only run batch script or restore persistent genome once all the server responses have come back.
			String batchFile = IGB.commandLineBatchFileStr;
			if (batchFile != null) {
				IGB.commandLineBatchFileStr = null;	// we're not using this again!
				lookForPersistentGenome = false;
				Thread.sleep(1000);	// hack so event queue finishes
				ScriptFileLoader.doActions(batchFile);
			} else {
				if (lookForPersistentGenome) {
					lookForPersistentGenome = false;
					Thread.sleep(1000);	// hack so event queue finishes
					RestorePersistentGenome();
				}
			}
		} catch (Exception ex) {
			Logger.getLogger(GeneralLoadView.class.getName()).log(Level.SEVERE, null, ex);
		}
	}

	/**
	 * bootstrap bookmark from Preferences for last species/versionName/genome / sequence / region
	 */
	private void RestorePersistentGenome() {
		// Get group and seq info from persistent preferences.
		// (Recovering as much data as possible before activating listeners.)
		AnnotatedSeqGroup group = Persistence.restoreGroupSelection();
		if (group == null) {
			return;
		}

		Set<GenericVersion> gVersions = group.getEnabledVersions();
		if (gVersions == null || gVersions.isEmpty()) {
			return;
		}
		String versionName = GeneralLoadUtils.getPreferredVersionName(gVersions);
		if (versionName == null || GeneralLoadUtils.versionName2species.get(versionName) == null || gmodel.getSeqGroup(versionName) != group) {
			return;
		}

		if (gmodel.getSelectedSeqGroup() != null || gmodel.getSelectedSeq() != null) {
			return;
		}

		try {
			Application.getSingleton().addNotLockedUpMsg("Loading previous genome " + versionName +" ...");

			gmodel.addGroupSelectionListener(this);

			initVersion(versionName);

			gmodel.setSelectedSeqGroup(group);

			List<GenericFeature> features = GeneralLoadUtils.getSelectedVersionFeatures();
			if (features == null || features.isEmpty()) {
				return;
			}

			BioSeq seq = Persistence.restoreSeqSelection(group);
			if (seq == null) {
				seq = group.getSeq(0);
				if (seq == null) {
					return;
				}
			}

			gmodel.addSeqSelectionListener(this);
			gmodel.setSelectedSeq(seq);

			// Try/catch may not be needed.
			try {
				Persistence.restoreSeqVisibleSpan(gviewer);
			} catch (Exception e) {
				e.printStackTrace();
			}
		} finally {
			Application.getSingleton().removeNotLockedUpMsg("Loading previous genome " + versionName +" ...");
		}
	}

	/**
	 * Initialize Species combo box.  It is assumed that we have the species data at this point.
	 * If a species was already selected, leave it as the selected species.
	 */
	private void refreshSpeciesCB() {
		speciesCB.removeItemListener(this);
		int speciesListLength = GeneralLoadUtils.species2genericVersionList.keySet().size();
		if (speciesListLength == speciesCB.getItemCount() -1) {
			// No new species.  Don't bother refreshing.
			return;
		}

		final List<String> speciesList = new ArrayList<String>();
		speciesList.addAll(GeneralLoadUtils.species2genericVersionList.keySet());
		Collections.sort(speciesList);

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				String oldSpecies = (String)speciesCB.getSelectedItem();

				speciesCB.removeAllItems();
				speciesCB.addItem(SELECT_SPECIES);
				for (String speciesName : speciesList) {
					speciesCBRenderer.setToolTipEntry(speciesName, SpeciesLookup.getCommonSpeciesName(speciesName));
					speciesCB.addItem(speciesName);
				}
				if (oldSpecies == null) {
					return;
				}

				if (speciesList.contains(oldSpecies)) {
					speciesCB.setSelectedItem(oldSpecies);
				} else {
					// species CB changed
					speciesCBChanged();
				}
			}

		});
	}


	/**
	 * Refresh the genome versions.
	 * @param speciesName
	 */
	private void refreshVersionCB(final String speciesName) {
		final List<GenericVersion> versionList = GeneralLoadUtils.species2genericVersionList.get(speciesName);
		final List<String> versionNames = new ArrayList<String>();
		if (versionList != null) {
			for (GenericVersion gVersion : versionList) {
				// the same versionName name may occur on multiple servers
				String versionName = gVersion.versionName;
				if (!versionNames.contains(versionName)) {
					versionNames.add(versionName);
					versionCBRenderer.setToolTipEntry(versionName, GeneralLoadUtils.listSynonyms(versionName));
				}
			}
			Collections.sort(versionNames, new StringVersionDateComparator());
		}
		
		// Sort the versions (by date)

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				versionCB.removeItemListener(GeneralLoadView.this);
				String oldVersion = (String) versionCB.getSelectedItem();

				if (versionList == null || speciesName.equals(SELECT_SPECIES)) {
					versionCB.setSelectedIndex(0);
					versionCB.setEnabled(false);
					return;
				}

				// Add names to combo boxes.
				versionCB.removeAllItems();
				versionCB.addItem(SELECT_GENOME);
				for (String versionName : versionNames) {
					versionCB.addItem(versionName);
				}
				versionCB.setEnabled(true);
				if (oldVersion != null && !oldVersion.equals(SELECT_GENOME) && GeneralLoadUtils.versionName2species.containsKey(oldVersion)) {
					versionCB.setSelectedItem(oldVersion);
				} else {
					versionCB.setSelectedIndex(0);
				}
				if (versionCB.getItemCount() > 1) {
					versionCB.addItemListener(GeneralLoadView.this);
				}
			}
		});
	}


	public static void initVersion(String versionName) {
		Application.getSingleton().addNotLockedUpMsg("Loading chromosomes for " + versionName);
		try {
			GeneralLoadUtils.initVersionAndSeq(versionName); // Make sure this genome versionName's feature names are initialized.
		} finally {
			Application.getSingleton().removeNotLockedUpMsg("Loading chromosomes for " + versionName);
		}
	}
	
	/**
	 * Handles clicking of partial residue, all residue, and refresh data buttons.
	 * @param evt
	 */
	public void actionPerformed(ActionEvent evt) {
		final Object src = evt.getSource();
		if (src != partial_residuesB && src != all_residuesB) {
			return;
		}

		final String genomeVersionName = (String) versionCB.getSelectedItem();

		final BioSeq curSeq = gmodel.getSelectedSeq();
		// Use a SwingWorker to avoid locking up the GUI.
		Executor vexec = ThreadUtils.getPrimaryExecutor(src);

		SwingWorker<Void, Void> worker = getResidueWorker(genomeVersionName, curSeq, gviewer.getVisibleSpan(), src == partial_residuesB, false);
		
		vexec.execute(worker);
	}

	public static SwingWorker<Void, Void> getResidueWorker(final String genomeVersionName, final BioSeq seq,
			final SeqSpan viewspan, final boolean partial, final boolean tryFull) {

		SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

			public Void doInBackground() {
				try {
					Application.getSingleton().addNotLockedUpMsg("Loading residues for "+seq.getID());

					if (partial) {

						if (!GeneralLoadUtils.loadResidues(genomeVersionName, seq, viewspan.getMin(), viewspan.getMax(), viewspan)) {
							if (!tryFull) {
								ErrorHandler.errorPanel("Couldn't load partial sequence",
										"Couldn't locate the partial sequence.  Try loading the full sequence.");
							} else {
								if (!GeneralLoadUtils.loadResidues(genomeVersionName, seq, 0, seq.getLength(), null)) {
									ErrorHandler.errorPanel("Couldn't load partial or full sequence",
											"Couldn't locate the sequence.");
								}
							}
						}
					} else {
						if (!GeneralLoadUtils.loadResidues(genomeVersionName, seq, 0, seq.getLength(), null)) {
							ErrorHandler.errorPanel("Couldn't load full sequence",
									"Couldn't locate the sequence.");
						}
					}
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				return null;
			}

			@Override
			public void done() {
				Application.getSingleton().removeNotLockedUpMsg("Loading residues for "+seq.getID());
			}
		};

		return worker;
	}

	/**
	 * Load any data that's marked for visible range.
	 */
	public void loadVisibleData() {
		if (DEBUG_EVENTS) {
			SeqSpan request_span = gviewer.getVisibleSpan();
			System.out.println("Visible load request span: " + request_span.getBioSeq() + ":" + request_span.getStart() + "-" + request_span.getEnd());
		}

		// Load any features that have a visible strategy and haven't already been loaded.
		for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
			if (gFeature.loadStrategy != LoadStrategy.VISIBLE && gFeature.loadStrategy != LoadStrategy.CHROMOSOME) {
				continue;
			}

			if (DEBUG_EVENTS) {
				System.out.println("Selected : " + gFeature.featureName);
			}
			GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
		}

	}

	/**
	 * One of the combo boxes changed state.
	 * @param evt
	 */
	public void itemStateChanged(ItemEvent evt) {
		Object src = evt.getSource();
		if (DEBUG_EVENTS) {
			System.out.println("####### GeneralLoadView received itemStateChanged event: " + evt);
		}

		try {
			if ((src == speciesCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
				speciesCBChanged(); // make sure display gets updated
			} else if ((src == versionCB) && (evt.getStateChange() == ItemEvent.SELECTED)) {
				versionCBChanged();
			}
		} catch (Throwable t) {
			// some out-of-memory errors could happen during this code, so
			// this catch block will report that to the user.
			ErrorHandler.errorPanel("Error ", t);
		}
	}

	/**
	 * The species combo box changed.
	 * If the species changes to SELECT, the SelectedSeqGroup is set to null.
	 * If the species changes to a specific organism and there's only one choice for the genome versionName, the SelectedSeqGroup is set to that versionName.
	 * Otherwise, the SelectedSetGroup is set to null.
	 */
	private void speciesCBChanged() {
		String speciesName = (String) speciesCB.getSelectedItem();

		// Populate the versionName CB
		refreshVersionCB(speciesName);

		// Select the null group (and the null seq), if it's not already selected.
		if (curGroup != null) {
			gmodel.setSelectedSeqGroup(null); // This method is being called on purpose to fire group selection event.
			gmodel.setSelectedSeq(null);	  // which in turns calls refreshTreeView method.
		}
	}

	/**
	 * The versionName combo box changed.
	 * This changes the selected group (either to null, or to a valid group).
	 * It is assumed that at this point, the species is valid.
	 */
	private void versionCBChanged() {
		String versionName = (String) versionCB.getSelectedItem();
		if (DEBUG_EVENTS) {
			System.out.println("Selected version: " + versionName);
		}

		if (curGroup != null) {
			gmodel.setSelectedSeqGroup(null);
			gmodel.setSelectedSeq(null);
		}

		if (versionName.equals(SELECT_GENOME)) {
			// Select the null group (and the null seq), if it's not already selected.	
			return;
		}

		AnnotatedSeqGroup group = gmodel.getSeqGroup(versionName);
		if (group == null) {
			System.out.println("Group was null -- trying species instead");
			group = gmodel.getSeqGroup(GeneralLoadUtils.versionName2species.get(versionName));
			if (group == null) {
				return;
			}
		}

		speciesCB.setEnabled(false);
		versionCB.setEnabled(false);

		(new InitVersionWorker(versionName, group)).execute();	
	}

	/**
	 * Run initialization of version on thread, so we don't lock up the GUI.
	 * Merge with initVersion();
	 */
	private class InitVersionWorker extends SwingWorker<Void, Void> {

		private final String versionName;
		private final AnnotatedSeqGroup group;

		InitVersionWorker(String versionName, AnnotatedSeqGroup group) {
			this.versionName = versionName;
			this.group = group;
		}

		@Override
		public Void doInBackground() {
			Application.getSingleton().addNotLockedUpMsg("Loading chromosomes for " + versionName);
			GeneralLoadUtils.initVersionAndSeq(versionName); // Make sure this genome versionName's feature names are initialized.
			return null;
		}

		@Override
		protected void done() {
			Application.getSingleton().removeNotLockedUpMsg("Loading chromosomes for " + versionName);
			speciesCB.setEnabled(true);
			versionCB.setEnabled(true);
			if (curGroup != null || group != null) {
				// avoid calling these a half-dozen times
				gmodel.setSelectedSeqGroup(group);
				// TODO: Need to be certain that the group is selected at this point!
				gmodel.setSelectedSeq(group.getSeq(0));
			}
		}
	}



	/**
	 * This gets called when the genome versionName is changed.
	 * This occurs via the combo boxes, or by an external event like bookmarks, or LoadFileAction
	 * @param evt
	 */
	public void groupSelectionChanged(GroupSelectionEvent evt) {
		AnnotatedSeqGroup group = evt.getSelectedGroup();

		if (DEBUG_EVENTS) {
			System.out.println("GeneralLoadView.groupSelectionChanged() called, group: " + (group == null ? null : group.getID()));
		}
		if (group == null) {
			if (versionCB.getSelectedItem() != SELECT_GENOME) {
				versionCB.removeItemListener(this);
				versionCB.setEnabled(false);
				versionCB.addItemListener(this);
			}
			curGroup = null;
			return;
		}
		if (curGroup == group) {
			if (DEBUG_EVENTS) {
				System.out.println("GeneralLoadView.groupSelectionChanged(): group was same as previous.");
			}
			return;
		}
		curGroup = group;

		Set<GenericVersion> gVersions = group.getEnabledVersions();
		if (gVersions.isEmpty()) {
			createUnknownVersion(group);
			return;
		}
		final String versionName = GeneralLoadUtils.getPreferredVersionName(gVersions);
		if (versionName == null) {
			System.out.println("ERROR -- couldn't find version");
			return;
		}
		final String speciesName = GeneralLoadUtils.versionName2species.get(versionName);
		if (speciesName == null) {
			// Couldn't find species matching this versionName -- we have problems.
			System.out.println("ERROR - Couldn't find species for version " + versionName);
			return;
		}

		if (!speciesName.equals(speciesCB.getSelectedItem())) {
			// Set the selected species (the combo box is already populated)
			ThreadUtils.runOnEventQueue(new Runnable() {
				public void run() {
					speciesCB.removeItemListener(GeneralLoadView.this);
					speciesCB.setSelectedItem(speciesName);
					speciesCB.addItemListener(GeneralLoadView.this);
				}
			});
		}
		if (!versionName.equals(versionCB.getSelectedItem())) {
			refreshVersionCB(speciesName);			// Populate the versionName CB
			ThreadUtils.runOnEventQueue(new Runnable() {

				public void run() {
					versionCB.removeItemListener(GeneralLoadView.this);
					versionCB.setSelectedItem(versionName);
					versionCB.addItemListener(GeneralLoadView.this);
				}
			});
		}

		refreshTreeView();	// Replacing clearFeaturesTable with refreshTreeView.
							// refreshTreeView should only be called if feature table
							// needs to be cleared.

		disableAllButtons();
	}

	/**
	 * Changed the selected chromosome.
	 * @param evt
	 */
	public void seqSelectionChanged(SeqSelectionEvent evt) {
		BioSeq aseq = evt.getSelectedSeq();

		if (DEBUG_EVENTS) {
			System.out.println("GeneralLoadView.seqSelectionChanged() called, aseq: " + (aseq == null ? null : aseq.getID()));
		}

		if (aseq == null) {
			refreshTreeView();	// Replacing clearFeaturesTable with refreshTreeView.
								// refreshTreeView should only be called if feature table
								// needs to be cleared.

			disableAllButtons();
			return;
		}

		// validate that this sequence is in our group.
		AnnotatedSeqGroup group = aseq.getSeqGroup();
		if (group == null) {
			if (DEBUG_EVENTS) {
				System.out.println("sequence was null");
			}
			return;
		}
		Set<GenericVersion> gVersions = group.getEnabledVersions();
		if (gVersions.isEmpty()) {
			createUnknownVersion(group);
			return;
		}

		String speciesName = (String) this.speciesCB.getSelectedItem();
		String versionName = (String) this.versionCB.getSelectedItem();
		if (speciesName == null || versionName == null || speciesName.equals(SELECT_SPECIES) || versionName.equals(SELECT_GENOME)) {
			return;
		}

		if (!(GeneralLoadUtils.getPreferredVersionName(gVersions).equals(versionName))) {
			/*System.out.println("ERROR - versions don't match: " + versionName + "," +
					GeneralLoadUtils.getPreferredVersionName(gVersions));*/
			return;
		}

		try {
			Application.getSingleton().addNotLockedUpMsg("Loading features for " + versionName);
			createFeaturesTable();
			loadWholeRangeFeatures();
		} finally {
			Application.getSingleton().removeNotLockedUpMsg("Loading features for " + versionName);
		}
	}


	/**
	 * group has been created independently of the discovery process (probably by loading a file).
	 * create new "unknown" species/versionName.
	 */
	private void createUnknownVersion(AnnotatedSeqGroup group) {
		gmodel.removeGroupSelectionListener(this);
		gmodel.removeSeqSelectionListener(this);

		speciesCB.removeItemListener(this);
		versionCB.removeItemListener(this);
		GenericVersion gVersion = GeneralLoadUtils.getUnknownVersion(group);
		String species = GeneralLoadUtils.versionName2species.get(gVersion.versionName);
		refreshSpeciesCB();
		if (DEBUG_EVENTS) {
			System.out.println("Species is " + species + ", version is " + gVersion.versionName);
		}

		if (!species.equals(speciesCB.getSelectedItem())) {
			gmodel.removeGroupSelectionListener(this);
			gmodel.removeSeqSelectionListener(this);

			speciesCB.removeItemListener(this);
			versionCB.removeItemListener(this);

			// Set the selected species (the combo box is already populated)
			speciesCB.setSelectedItem(species);
			// populate the versionName combo box.
			refreshVersionCB(species);
		}

		initVersion(gVersion.versionName);

		versionCB.setSelectedItem(gVersion.versionName);
		versionCB.setEnabled(true);
		all_residuesB.setEnabled(false);
		partial_residuesB.setEnabled(false);
		refreshDataAction.setEnabled(false);
		addListeners();
	}

	public void refreshTreeView() {

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				final List<GenericFeature> features = GeneralLoadUtils.getSelectedVersionFeatures();
				if (features == null || features.isEmpty()) {
					feature_model.clearFeatures();
				}
				feature_tree_view.initOrRefreshTree(features);
			}
		});
	}

	/**
	 * Create the table with the list of features and their status.
	 */
	public List<GenericFeature> createFeaturesTable() {
		String versionName = (String) this.versionCB.getSelectedItem();
		final List<GenericFeature> features = GeneralLoadUtils.getSelectedVersionFeatures();
		if (DEBUG_EVENTS) {
			BioSeq curSeq = gmodel.getSelectedSeq();
			System.out.println("Creating new table with chrom " + (curSeq == null ? null : curSeq.getID()));
			System.out.println("features for " + versionName + ": " + features.toString());
		}

		int maxFeatureNameLength = 1;
		for (GenericFeature feature : features) {
			maxFeatureNameLength = Math.max(maxFeatureNameLength, feature.featureName.length());
		}
		final int finalMaxFeatureNameLength = maxFeatureNameLength;	// necessary for threading

		final List<GenericFeature> visibleFeatures = FeaturesTableModel.getVisibleFeatures(features);

		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				feature_model.setFeatures(visibleFeatures);

				// the second column contains the feature names.  Resize it so that feature names are fully displayed.
				TableColumn col = feature_table.getColumnModel().getColumn(FeaturesTableModel.FEATURE_NAME_COLUMN);
				col.setPreferredWidth(finalMaxFeatureNameLength);

				col = feature_table.getColumnModel().getColumn(FeaturesTableModel.DELETE_FEATURE_COLUMN);
				col.setResizable(false);
				col.setMaxWidth(10);

				col = feature_table.getColumnModel().getColumn(FeaturesTableModel.REFRESH_FEATURE_COLUMN);
				col.setResizable(false);
				col.setMaxWidth(10);
				
				// Don't enable combo box for full genome sequence
				// Enabling of combo box for local files with unknown chromosomes happens in setComboBoxEditors()
				TableWithVisibleComboBox.setComboBoxEditors(feature_table, !GeneralLoadView.IsGenomeSequence());
			}
		});

		disableButtonsIfNecessary();
		changeVisibleDataButtonIfNecessary(features);	// might have been disabled when switching to another chromosome or genome.
		return features;
	}

	/**
	 * Load any features that have a whole strategy and haven't already been loaded.
	 * @param versionName
	 */
	private static void loadWholeRangeFeatures() {
		for (GenericFeature gFeature : GeneralLoadUtils.getSelectedVersionFeatures()) {
			if (gFeature.loadStrategy != LoadStrategy.GENOME) {
				continue;
			}

//			if (gFeature.gVersion.gServer.serverType == ServerType.QuickLoad ||
//					gFeature.gVersion.gServer.serverType == ServerType.LocalFiles) {
//				// These have already been loaded(currently loaded for the entire genome at once)
//				continue;
//			}

			if (DEBUG_EVENTS) {
				System.out.println("Selected : " + gFeature.featureName);
			}
			GeneralLoadUtils.loadAndDisplayAnnotations(gFeature);
		}
	}

	/**
	 * Check if it is necessary to disable buttons.
	 * @return
	 */
	public static boolean getIsDisableNecessary(){
		boolean enabled = !IsGenomeSequence();
		if (enabled) {
			BioSeq curSeq = gmodel.getSelectedSeq();
			enabled = curSeq.getSeqGroup() != null;	// Don't allow a null sequence group either.
			if (enabled) {		// Don't allow buttons for an "unknown" versionName
				Set<GenericVersion> gVersions = curSeq.getSeqGroup().getEnabledVersions();
				enabled = (!gVersions.isEmpty());
			}
		}
		return enabled;
	}

	/**
	 * Don't allow buttons to be used if they're not valid.
	 */
	private void disableButtonsIfNecessary() {
		// Don't allow buttons for a full genome sequence
		setAllButtons(getIsDisableNecessary());
	}

	private void disableAllButtons() {
		setAllButtons(false);
	}

	private void setAllButtons(final boolean enabled) {
		ThreadUtils.runOnEventQueue(new Runnable() {

			public void run() {
				all_residuesB.setEnabled(enabled);
				partial_residuesB.setEnabled(enabled);
				refreshDataAction.setEnabled(enabled);
			}
		});
	}

	/**
	 * Accessor method.
	 * See if we need to enable/disable the refresh_dataB button
	 * by looking at the features' load strategies.
	 */
	void changeVisibleDataButtonIfNecessary(List<GenericFeature> features) {
		if (IsGenomeSequence()) {
			return;
		// Currently not enabling this button for the full sequence.
		}
		boolean enabled = false;
		for (GenericFeature gFeature : features) {
			if (gFeature.loadStrategy == LoadStrategy.VISIBLE || gFeature.loadStrategy == LoadStrategy.CHROMOSOME) {
				enabled = true;
				break;
			}
		}

		if (refreshDataAction.isEnabled() != enabled) {
			refreshDataAction.setEnabled(enabled);
		}
	}

	private static boolean IsGenomeSequence() {
		BioSeq curSeq = gmodel.getSelectedSeq();
		final String seqID = curSeq == null ? null : curSeq.getID();
		return (seqID == null || IGBConstants.GENOME_SEQ_ID.equals(seqID));
	}

	public String getSelectedSpecies(){
		return (String) speciesCB.getSelectedItem();
	}

	public static void removeFeature(final GenericFeature feature){
		if(feature == null)
			return;

		SwingWorker<Void, Void> delete = new SwingWorker<Void, Void>() {

			@Override
			protected Void doInBackground(){
				Application.getSingleton().addNotLockedUpMsg("Removing feature  "+feature.featureName);
				
				feature.removeAllSyms();

				// If feature is local then remove it from server.
				GenericVersion version = feature.gVersion;
				if (version.gServer.serverType.equals(ServerType.LocalFiles)) {
					version.removeFeature(feature);
				}
				
				return null;
			}

			@Override
			protected void done() {
				// Refresh
				GeneralLoadView.getLoadView().refreshTreeView();
				GeneralLoadView.getLoadView().createFeaturesTable();
				gviewer.setAnnotatedSeq(gviewer.getAnnotatedSeq());
				Application.getSingleton().removeNotLockedUpMsg("Removing feature  "+feature.featureName);
			}
		};

		ThreadUtils.getPrimaryExecutor(feature).execute(delete);
	}
}

