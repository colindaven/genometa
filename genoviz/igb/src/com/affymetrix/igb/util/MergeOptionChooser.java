package com.affymetrix.igb.util;

import com.affymetrix.igb.menuitem.LoadFileAction;
import com.affymetrix.igb.view.load.GeneralLoadView;
import com.affymetrix.genometryImpl.AnnotatedSeqGroup;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.*;
import javax.swing.*;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/** A JFileChooser that has a checkbox for whether you want to merge annotations.
 *  Note that an alternative way of adding a checkbox to a JFileChooser
 *  is to use JFileChooser.setAccessory().  The only advantage to this
 *  subclass is more control of where the JCheckBox is placed inside the
 *  dialog.
 */
public final class MergeOptionChooser extends JFileChooser implements ActionListener{

	private static final String SELECT_SPECIES = BUNDLE.getString("speciesCap");
	private static final String CHOOSE = "Choose";
	public final Box box;
	public final JComboBox speciesCB = new JComboBox();
	public final JComboBox versionCB = new JComboBox();

	public MergeOptionChooser() {
		super();
		
		speciesCB.addActionListener(this);
		versionCB.addActionListener(this);
		
		box = new Box(BoxLayout.X_AXIS);
		box.setBorder(BorderFactory.createEmptyBorder(5, 5, 8, 5));

		box.add(new JLabel(CHOOSE + ":"));
		box.add(Box.createHorizontalStrut(5));
		box.add(speciesCB);

		box.add(Box.createHorizontalStrut(5));
		box.add(versionCB);
		
	}

	@Override
	protected JDialog createDialog(Component parent) throws HeadlessException {
		JDialog dialog = super.createDialog(parent);

		refreshSpeciesList();
		dialog.getContentPane().add(box, BorderLayout.SOUTH);
		return dialog;
	}

	public void refreshSpeciesList(){
		speciesCB.removeAllItems();
		speciesCB.addItem(LoadFileAction.UNKNOWN_SPECIES_PREFIX + " " + LoadFileAction.unknown_group_count);
		for(String species : GeneralLoadUtils.getSpeciesList()){
			speciesCB.addItem(species);
		}

		String speciesName = GeneralLoadView.getLoadView().getSelectedSpecies();

		if(!SELECT_SPECIES.equals(speciesName))
			speciesCB.setSelectedItem(speciesName);
		else
			speciesCB.setSelectedIndex(0);

		AnnotatedSeqGroup group = GenometryModel.getGenometryModel().getSelectedSeqGroup();
		if (group != null) {
			versionCB.setSelectedItem(group.getID());
		} else {
			versionCB.setSelectedIndex(0);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if(e == null)
			return;

		if(e.getSource() == speciesCB){
			
			populateVersionCB();

			if(speciesCB.getSelectedIndex() == 0)
				speciesCB.setEditable(true);
			else
				speciesCB.setEditable(false);

			versionCB.setSelectedIndex(0);
		}

		if (e.getSource() == versionCB) {
			if (versionCB.getSelectedIndex() == 0) {
				versionCB.setEditable(true);
			} else {
				versionCB.setEditable(false);
			}
		}
	}

	private void populateVersionCB(){
		String speciesName = (String) speciesCB.getSelectedItem();
		versionCB.removeAllItems();
		versionCB.addItem(LoadFileAction.UNKNOWN_GENOME_PREFIX + " " + LoadFileAction.unknown_group_count);
		for(String version : GeneralLoadUtils.getGenericVersions(speciesName)){
			versionCB.addItem(version);
		}
	}

}
