
package com.affymetrix.igb.action;

import com.affymetrix.igb.view.load.GeneralLoadView;

import java.text.MessageFormat;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author hiralv
 */
public class LoadSequence extends AbstractAction {
	private static final long serialVersionUID = 1l;

	private static final String LOAD = BUNDLE.getString("load");
	private static final String PARTIAL = MessageFormat.format(LOAD,BUNDLE.getString("sequenceInViewCap"));
	private static final String WHOLE = MessageFormat.format(LOAD,BUNDLE.getString("allSequenceCap"));

	private static final LoadSequence partial = new LoadSequence(PARTIAL);
	private static final LoadSequence whole = new LoadSequence(WHOLE);

	private LoadSequence(String command){
		super(command);
	}

	public static AbstractAction getPartialAction(){
		return partial;
	}

	public static AbstractAction getWholeAction(){
		return whole;
	}

	public void actionPerformed(ActionEvent e) {
		GeneralLoadView.getLoadView().loadResidues(e.getActionCommand());
	}
}
