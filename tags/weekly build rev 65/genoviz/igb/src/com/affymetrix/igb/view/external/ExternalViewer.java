package com.affymetrix.igb.view.external;

import java.awt.CardLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JComboBox;
import javax.swing.JComponent;

/**
 * Container panel for the external views
 * Shows up as tab in IGB
 * Allows selection of subviews with combobox
 *
 * @author Ido M. Tamir
 */
public class ExternalViewer extends JComponent implements ItemListener {

	private static final String[] names = {UCSCView.viewName, EnsemblView.viewName};
	final JComboBox ucscBox;
	final JComboBox ensemblBox;

	public ExternalViewer() {
		this.setLayout(new CardLayout());
		ucscBox = createBox();
		ensemblBox = createBox();

		final UCSCView ucsc = new UCSCView(ucscBox);
		final EnsemblView ensembl = new EnsemblView(ensemblBox);

		add(ucsc, ucsc.getViewName());
		add(ensembl, ensembl.getViewName());
	}

	private JComboBox createBox() {
		JComboBox box = new JComboBox(names);
		box.setPrototypeDisplayValue("ENSEMBL");
		box.setMaximumSize(box.getPreferredSize());
		box.setEditable(false);
		box.addItemListener(this);
		return box;
	}

	
	public void itemStateChanged(ItemEvent e) {
		if (e.getID() == ItemEvent.ITEM_STATE_CHANGED) {
			CardLayout cl = (CardLayout) (getLayout());
			if (e.getSource() == ucscBox) {
				ensemblBox.setSelectedItem(EnsemblView.viewName);
			}
			if (e.getSource() == ensemblBox) {
				ucscBox.setSelectedItem(UCSCView.viewName);
			}
			cl.show(ExternalViewer.this, (String) e.getItem());
		}
	}
}
