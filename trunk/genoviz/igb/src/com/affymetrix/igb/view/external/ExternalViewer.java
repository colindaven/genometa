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
public class ExternalViewer extends JComponent {

	private static final String[] names = {UCSCView.viewName};
	final JComboBox ucscBox;

	public ExternalViewer() {
		this.setLayout(new CardLayout());
		ucscBox = createBox();

		final UCSCView ucsc = new UCSCView(ucscBox);

		add(ucsc, ucsc.getViewName());
	}

	private JComboBox createBox() {
		JComboBox box = new JComboBox(names);
		box.setPrototypeDisplayValue("ENSEMBL");
		box.setMaximumSize(box.getPreferredSize());
		box.setEditable(false);
		return box;
	}

	
}
