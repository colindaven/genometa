package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.view.load.GeneralLoadUtils;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

/**
 *
 * @author hiralv
 */
public class AutoLoadAction extends AbstractAction {

	private static final AutoLoadAction singleton = new AutoLoadAction();
	private final JCheckBox autoload;

	private AutoLoadAction(){
		autoload = PreferenceUtils.createCheckBox(PreferenceUtils.AUTO_LOAD, PreferenceUtils.getTopNode(),
				PreferenceUtils.AUTO_LOAD, PreferenceUtils.default_auto_load);
		autoload.setToolTipText("Automatically load default features when available (e.g., cytoband and refseq)");
		autoload.addActionListener(this);
	}

	public static JCheckBox getAction(){
		return singleton.autoload;
	}
	
	public void actionPerformed(ActionEvent e) {
		GeneralLoadUtils.setFeatureAutoLoad(autoload.isSelected());
	}

}
