package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.MenuUtil;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
/**
 *
 * @author hiralv
 */
public class FeatureInfoAction extends AbstractAction {
	final String url ;

	public FeatureInfoAction(String url){
		super(BUNDLE.getString("trackInfo"), MenuUtil.getIcon("toolbarButtonGraphics/general/Information16.gif"));
		this.url = url;
	}

	public void actionPerformed(ActionEvent e) {
		GeneralUtils.browse(url);
	}

}
