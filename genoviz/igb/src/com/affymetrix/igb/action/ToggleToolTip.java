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
 * @author hiralv
 */
public class ToggleToolTip extends AbstractAction {
	private static final long serialVersionUID = 1;
	private static final ToggleToolTip ACTION = new ToggleToolTip();

	private ToggleToolTip() {
		super(BUNDLE.getString("togglePropertiesTooltip"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_H);
		/* TODO: This is only correct for English Locale" */
		this.putValue(DISPLAYED_MNEMONIC_INDEX_KEY, 5);

		SeqMapView map_view = IGB.getSingleton().getMapView();
		boolean show_prop_tooltip = PreferenceUtils.getTopNode().getBoolean(SeqMapView.PREF_SHOW_TOOLTIP, true);
		if (map_view.shouldShowPropTooltip()!= show_prop_tooltip) {
			map_view.togglePropertiesTooltip();
		}
		this.putValue(SELECTED_KEY, show_prop_tooltip);
	}

	public static ToggleToolTip getAction() {
		return ACTION;
	}

	public void actionPerformed(ActionEvent e) {
		PreferenceUtils.getTopNode().putBoolean(
				SeqMapView.PREF_SHOW_TOOLTIP,
				IGB.getSingleton().getMapView().togglePropertiesTooltip());
	}

}
