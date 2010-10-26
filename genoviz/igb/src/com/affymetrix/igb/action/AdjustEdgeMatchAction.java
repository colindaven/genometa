package com.affymetrix.igb.action;

import com.affymetrix.igb.IGB;
import com.affymetrix.igb.glyph.EdgeMatchAdjuster;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: AdjustEdgeMatchAction.java 5816 2010-04-29 14:28:29Z sgblanch $
 */
public class AdjustEdgeMatchAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public AdjustEdgeMatchAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("adjustEdgeMatchFuzziness")));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_F);
	}

	public void actionPerformed(ActionEvent e) {
		SeqMapView map_view = IGB.getSingleton().getMapView();
		EdgeMatchAdjuster.showFramedThresholder(map_view.getEdgeMatcher(), map_view);
	}

}
