package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: ToggleHairlineLabelAction.java 5958 2010-05-18 16:00:59Z jnicol $
 */
public class ToggleHairlineLabelAction extends AbstractAction {
	private static final long serialVersionUID = 1;
	private static final ToggleHairlineLabelAction ACTION = new ToggleHairlineLabelAction();

	private ToggleHairlineLabelAction() {
		super(BUNDLE.getString("toggleHairlineLabel"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_H);
		/* TODO: This is only correct for English Locale" */
		this.putValue(DISPLAYED_MNEMONIC_INDEX_KEY, 5);

		SeqMapView map_view = IGB.getSingleton().getMapView();
		boolean use_hairline_label = PreferenceUtils.getTopNode().getBoolean(SeqMapView.PREF_HAIRLINE_LABELED, true);
		if (map_view.isHairlineLabeled() != use_hairline_label) {
			map_view.toggleHairlineLabel();
		}
		this.putValue(SELECTED_KEY, use_hairline_label);
	}

	public static ToggleHairlineLabelAction getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		PreferenceUtils.getTopNode().putBoolean(
				SeqMapView.PREF_HAIRLINE_LABELED,
				IGB.getSingleton().getMapView().toggleHairlineLabel());
	}

}
