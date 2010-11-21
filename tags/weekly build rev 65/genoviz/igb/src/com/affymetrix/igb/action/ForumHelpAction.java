package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.MenuUtil;
import java.awt.event.ActionEvent;
import java.text.MessageFormat;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;
import static com.affymetrix.igb.IGBConstants.APP_NAME;

/**
 *
 * @author hiralv
 */
public class ForumHelpAction extends AbstractAction{

	public ForumHelpAction(){
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					MessageFormat.format(
						BUNDLE.getString("forumHelp"),
						APP_NAME)),
				MenuUtil.getIcon("toolbarButtonGraphics/general/Information16.gif"));
	}

	public void actionPerformed(ActionEvent e) {
		GeneralUtils.browse("https://sourceforge.net/projects/genoviz/forums/forum/439787");
	}

}
