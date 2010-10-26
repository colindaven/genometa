/**
 *   Copyright (c) 2006 Affymetrix, Inc.
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

import com.affymetrix.igb.action.AutoLoadAction;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.util.LoadUtils.ServerType;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.LoadUtils;
import com.affymetrix.igb.menuitem.FileTracker;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.igb.general.ServerList;
import com.affymetrix.igb.prefs.IPrefEditorComponent;
import com.affymetrix.igb.prefs.SourceTableModel;
import com.affymetrix.igb.prefs.SourceTableModel.SourceColumn;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.genoviz.swing.BooleanTableCellRenderer;
import com.affymetrix.igb.util.IGBAuthenticator;
import com.affymetrix.genometryImpl.util.LocalUrlCacher;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.*;
import java.io.*;
import java.net.Authenticator;
import java.net.Authenticator.RequestorType;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import static com.affymetrix.genometryImpl.util.LocalUrlCacher.CacheUsage;
import static javax.swing.GroupLayout.Alignment.BASELINE;
import static javax.swing.GroupLayout.Alignment.LEADING;
import static javax.swing.GroupLayout.Alignment.TRAILING;
import static javax.swing.JFileChooser.APPROVE_OPTION;
import static javax.swing.JFileChooser.DIRECTORIES_ONLY;
import static javax.swing.JFileChooser.FILES_AND_DIRECTORIES;
import static javax.swing.JOptionPane.OK_CANCEL_OPTION;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.PLAIN_MESSAGE;

/**
 *
 * @author sgblanch
 * @version $Id: DataLoadPrefsView.java 7028 2010-10-14 20:04:07Z jnicol $
 */
public final class DataLoadPrefsView extends IPrefEditorComponent {
	private static final long serialVersionUID = 2l;

	private static final String PREF_SYN_FILE_URL = "Synonyms File URL";

	private static final String[] OPTIONS = new String[]{"Add Server", "Cancel"};

	public DataLoadPrefsView() {
		final GroupLayout layout = new GroupLayout(this);
		final JPanel sourcePanel = initSourcePanel();
		final JPanel synonymsPanel = initSynonymsPanel(this);
		final JPanel cachePanel = initCachePanel();
	
		this.setName("Data Sources");
		this.setToolTipText("Edit data sources and preferences");

		this.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(sourcePanel)
				.addComponent(synonymsPanel)
				.addComponent(cachePanel));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(sourcePanel)
				.addComponent(synonymsPanel)
				.addComponent(cachePanel));
	}

	private static JPanel initSourcePanel() {
		final JPanel sourcePanel = new JPanel();
		final GroupLayout layout = new GroupLayout(sourcePanel);
		final SourceTableModel sourceTableModel = new SourceTableModel();
		final JTable sourcesTable = createSourcesTable(sourceTableModel);
		final JScrollPane sourcesScrollPane = new JScrollPane(sourcesTable);

		sourcePanel.setLayout(layout);
		sourcePanel.setBorder(new TitledBorder("Data Sources"));
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		final JButton addServerButton = createButton("Add\u2026", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				showAddSourceDialog();
				sourceTableModel.init();
			}
		});

		final JButton removeServerButton = createButton("Remove", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object url = sourcesTable.getModel().getValueAt(
						sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()),
						SourceColumn.URL.ordinal());
				removeDataSource(url.toString());
				sourceTableModel.init();
			}
		});
		removeServerButton.setEnabled(false);

		final JButton editAuthButton = createButton("Authentication\u2026", new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				Object url = sourcesTable.getModel().getValueAt(
						sourcesTable.convertRowIndexToModel(sourcesTable.getSelectedRow()),
						SourceColumn.URL.ordinal());
				try {
					URL u = new URL((String) url);
					IGBAuthenticator.resetAuth((String) url);
					Authenticator.requestPasswordAuthentication(
							u.getHost(),
							null,
							u.getPort(),
							u.getProtocol(),
							"Server Credentials",
							null,
							u,
							RequestorType.SERVER);
				} catch (MalformedURLException ex) {
					Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, ex);
				}
			}
		});
		editAuthButton.setEnabled(false);

		final JCheckBox autoload = AutoLoadAction.getAction();
		
		sourcesTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public void valueChanged(ListSelectionEvent event) {
				int viewRow = sourcesTable.getSelectedRow();
				boolean enable = viewRow >= 0;
				
				removeServerButton.setEnabled(enable);
				editAuthButton.setEnabled(enable);
			}
		});

		layout.setHorizontalGroup(layout.createParallelGroup(TRAILING)
				.addComponent(sourcesScrollPane)
				.addComponent(autoload)
				.addGroup(layout.createSequentialGroup()
					.addComponent(addServerButton)
					.addComponent(editAuthButton)
					.addComponent(removeServerButton)));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(sourcesScrollPane)
				.addComponent(autoload)
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(addServerButton)
					.addComponent(editAuthButton)
					.addComponent(removeServerButton)));

		return sourcePanel;
	}

	private static JPanel initSynonymsPanel(final JPanel parent) {
		final JPanel synonymsPanel = new JPanel();
		final GroupLayout layout = new GroupLayout(synonymsPanel);
		final JLabel synonymsLabel= new JLabel("Synonyms File");
		final JTextField synonymFile = new JTextField(PreferenceUtils.getLocationsNode().get(PREF_SYN_FILE_URL, ""));
		final JButton openFile = new JButton("\u2026");
		final ActionListener listener = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == openFile) {
					File file = fileChooser(FILES_AND_DIRECTORIES, parent);
					try {
						if (file != null) {
							synonymFile.setText(file.getCanonicalPath());
						}
					} catch (IOException ex) {
						Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, ex);
					}
				}

				if (synonymFile.getText().isEmpty() || loadSynonymFile(synonymFile)) {
					PreferenceUtils.getLocationsNode().put(PREF_SYN_FILE_URL, synonymFile.getText());
				} else {
					ErrorHandler.errorPanel(
					"Unable to Load Synonyms",
					"Unable to load personal synonyms from " + synonymFile.getText() + ".");
				}
			}
		};

		openFile.setToolTipText("Open Local Directory");
		openFile.addActionListener(listener);
		synonymFile.addActionListener(listener);

		

		synonymsPanel.setLayout(layout);
		synonymsPanel.setBorder(new TitledBorder("Personal Synonyms"));
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);

		layout.setHorizontalGroup(layout.createSequentialGroup()
				.addComponent(synonymsLabel)
				.addComponent(synonymFile)
				.addComponent(openFile));

		layout.setVerticalGroup(layout.createParallelGroup(BASELINE)
				.addComponent(synonymsLabel)
				.addComponent(synonymFile)
				.addComponent(openFile));

		/* Load the synonym file from preferences on startup */
		loadSynonymFile(synonymFile);

		return synonymsPanel;
	}

	private static JPanel initCachePanel() {
		final JPanel cachePanel = new JPanel();
		final GroupLayout layout = new GroupLayout(cachePanel);
		final JLabel usageLabel = new JLabel("Cache Behavior");
		final JLabel emptyLabel = new JLabel();
		final JComboBox	cacheUsage = new JComboBox(CacheUsage.values());
		final JButton clearCache = new JButton("Empty Cache");
		clearCache.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				clearCache.setEnabled(false);
				try {
					Thread.sleep(300);
				} catch (InterruptedException ex) {
					Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.SEVERE, null, ex);
				}
				LocalUrlCacher.clearCache();
				clearCache.setEnabled(true);
			}
		});

		cacheUsage.setSelectedItem(LocalUrlCacher.getCacheUsage(LocalUrlCacher.getPreferredCacheUsage()));
		cacheUsage.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				LocalUrlCacher.setPreferredCacheUsage(((CacheUsage)cacheUsage.getSelectedItem()).usage);
			}
		});

		cachePanel.setLayout(layout);
		cachePanel.setBorder(new TitledBorder("Cache Settings"));
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.linkSize(usageLabel, emptyLabel);

		layout.setHorizontalGroup(layout.createParallelGroup(LEADING)
				.addGroup(layout.createSequentialGroup()
					.addComponent(usageLabel)
					.addComponent(cacheUsage))
				.addGroup(layout.createSequentialGroup()
					.addComponent(emptyLabel)
					.addComponent(clearCache)));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(usageLabel)
					.addComponent(cacheUsage))
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(emptyLabel)
					.addComponent(clearCache)));

		return cachePanel;
	}

	private static JTable createSourcesTable(SourceTableModel sourceTableModel) {
		final JTable table = new JTable(sourceTableModel);

		table.setFillsViewportHeight(true);
		table.setAutoCreateRowSorter(true);
		table.getRowSorter().setSortKeys(SourceTableModel.SORT_KEYS);
		table.setDefaultRenderer(Boolean.class, new BooleanTableCellRenderer());
		TableCellRenderer renderer = new DefaultTableCellRenderer() {
			private static final long serialVersionUID = -5433598077871623855l;

			@Override
			public Component getTableCellRendererComponent(JTable table, Object value,
					boolean isSelected, boolean hasFocus, int row, int col) {

				int modelRow = table.convertRowIndexToModel(row);
				this.setEnabled((Boolean) table.getModel().getValueAt(modelRow, SourceColumn.Enabled.ordinal()));
				return super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, col);
			}
		};
		table.setDefaultRenderer(String.class,  renderer);
		table.setDefaultRenderer(ServerType.class, renderer);

		for (Enumeration<TableColumn> e = table.getColumnModel().getColumns(); e.hasMoreElements(); ) {
			TableColumn column = e.nextElement();
			SourceColumn current = SourceColumn.valueOf((String)column.getHeaderValue());

			switch (current) {
				case Name:
					column.setPreferredWidth(100);
					break;
				case URL:
					column.setPreferredWidth(300);
					break;
				case Enabled:
					column.setPreferredWidth(30);
					break;
				default:
					column.setPreferredWidth(50);
					break;
			}
		}

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		return table;
	}

	private static void showAddSourceDialog() {
		JTextField name = new JTextField("Your server name");
		JTextField url = new JTextField("http://");

		JComboBox  type = new JComboBox(LoadUtils.ServerType.values());

		type.removeItem(LoadUtils.ServerType.LocalFiles);
		type.setSelectedItem(LoadUtils.ServerType.QuickLoad);	// common default

		int result = JOptionPane.showOptionDialog(
				null,
				createAddSourceDialog(name, url, type),
				"Add Data Source",
				OK_CANCEL_OPTION,
				PLAIN_MESSAGE,
				null,
				OPTIONS,
				OPTIONS[0]);

		if (result == OK_OPTION) {
			addDataSource((ServerType)type.getSelectedItem(), name.getText(), url.getText());
		}
	}

	private static JPanel createAddSourceDialog(final JTextField name, final JTextField url, final JComboBox  type) {
		final JPanel messageContainer = new JPanel();
		final JPanel addServerPanel = new JPanel();
		final JLabel nameLabel = new JLabel("Name");
		final JLabel urlLabel = new JLabel("URL");
		final JLabel typeLabel = new JLabel("Type");
		final JButton openDir = new JButton("\u2026");
		final GroupLayout layout = new GroupLayout(addServerPanel);

		openDir.setToolTipText("Open Local Directory");
		openDir.setEnabled(type.getSelectedItem() == LoadUtils.ServerType.QuickLoad);

		type.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				openDir.setEnabled(type.getSelectedItem() == LoadUtils.ServerType.QuickLoad);
			}
		});

		addServerPanel.setLayout(layout);
		layout.setAutoCreateGaps(true);
		layout.setAutoCreateContainerGaps(true);
		layout.linkSize(nameLabel, urlLabel, typeLabel);
		name.setPreferredSize(new Dimension(300, name.getPreferredSize().height));
		layout.linkSize(name, type);
		layout.linkSize(SwingConstants.VERTICAL, name, type, url);

		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(messageContainer)
				.addGroup(layout.createSequentialGroup()
					.addComponent(nameLabel)
					.addComponent(name))
				.addGroup(layout.createSequentialGroup()
					.addComponent(typeLabel)
					.addComponent(type))
				.addGroup(layout.createSequentialGroup()
					.addComponent(urlLabel)
					.addComponent(url)
					.addComponent(openDir)));

		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(messageContainer)
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(nameLabel)
					.addComponent(name))
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(typeLabel)
					.addComponent(type))
				.addGroup(layout.createParallelGroup(BASELINE)
					.addComponent(urlLabel)
					.addComponent(url)
					.addComponent(openDir)));

		messageContainer.setLayout(new BoxLayout(messageContainer, BoxLayout.Y_AXIS));

		openDir.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				File f = fileChooser(DIRECTORIES_ONLY, addServerPanel);
				if (f != null && f.isDirectory()) {
					try {
						url.setText(f.toURI().toURL().toString());
					} catch (MalformedURLException ex) {
						Logger.getLogger(DataLoadPrefsView.class.getName()).log(Level.WARNING, "Unable to convert File '" + f.getName() + "' to URL", ex);
					}
				}
			}
		});

		return addServerPanel;
	}

	/**
	 * Add the URL/Directory and server name to the preferences.
	 * @param url
	 * @param type
	 * @param name
	 */
	private static void addDataSource(ServerType type, String name, String url) {
		if (url == null || url.isEmpty() || name == null || name.isEmpty()) {
			return;
		}
		
		GenericServer server = GeneralLoadUtils.addServer(type, name, url);

		if (server == null) {
			ErrorHandler.errorPanel(
					"Unable to Load Data Source",
					"Unable to load " + type + " data source '" + url + "'.");
			return;
		}

		//ServerList.addServerToPrefs(server);
	}

	private static void removeDataSource(String url) {
		if (ServerList.getServer(url) == null) {
			Logger.getLogger(DataLoadPrefsView.class.getName()).log(
					Level.SEVERE, "Can not remove Server ''{0}'': it does not exist in ServerList", url);
			return;
		}

		ServerList.removeServer(url);
		ServerList.removeServerFromPrefs(url);	// this is done last; other methods can depend upon the preference node
	}

	private static boolean loadSynonymFile(JTextField synonymFile) {
		File file = new File(synonymFile.getText());

		if (!file.isFile() || !file.canRead()) { return false; }

		FileInputStream fis = null;
		try {
			synonymFile.setText(file.getCanonicalPath());
			fis = new FileInputStream(file);
			SynonymLookup.getDefaultLookup().loadSynonyms(fis);
		} catch (IOException ex) {
			return false;
		} finally {
			GeneralUtils.safeClose(fis);
		}

		return true;
	}
	
	private static JButton createButton(String name, ActionListener listener) {
		final JButton button = new JButton(name);
		button.addActionListener(listener);
		return button;
	}

	private static File fileChooser(int mode, Component parent) throws HeadlessException {
		JFileChooser chooser = new JFileChooser();
		
		chooser.setCurrentDirectory(FileTracker.DATA_DIR_TRACKER.getFile());
		chooser.setFileSelectionMode(mode);
		chooser.setDialogTitle("Choose " + (mode == DIRECTORIES_ONLY ? "Directory" : "File"));
		chooser.setAcceptAllFileFilterUsed(mode != DIRECTORIES_ONLY);
		chooser.rescanCurrentDirectory();
		
		if (chooser.showOpenDialog(parent) != APPROVE_OPTION) { return null; }

		return chooser.getSelectedFile();
	}

	public void refresh() { }
}
