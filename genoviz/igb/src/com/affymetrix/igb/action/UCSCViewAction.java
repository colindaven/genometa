/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.igb.general.ServerList;
import com.affymetrix.genometryImpl.general.GenericServer;
import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.SynonymLookup;
import com.affymetrix.genoviz.util.ErrorHandler;
import com.affymetrix.igb.IGB;
import com.affymetrix.genometryImpl.das.DasServerInfo;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractAction;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: UCSCViewAction.java 6324 2010-07-01 20:11:30Z hiralv $
 */
public class UCSCViewAction extends AbstractAction implements SeqSelectionListener {
	private static final long serialVersionUID = 1l;
	private static final SeqMapView SEQ_MAP = IGB.getSingleton().getMapView();
	private static final String UCSC_DAS_URL = "http://genome.cse.ucsc.edu/cgi-bin/das/dsn";
	private static final String UCSC_URL = "http://genome.ucsc.edu/cgi-bin/hgTracks?";
	private static final SynonymLookup LOOKUP = SynonymLookup.getDefaultLookup();
	private static final Set<String> UCSCSources = Collections.<String>synchronizedSet(new HashSet<String>());

	public UCSCViewAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("viewRegionInUCSCBrowser")),
				MenuUtil.getIcon("toolbarButtonGraphics/development/WebComponent16.gif"));

		GenometryModel model = GenometryModel.getGenometryModel();
		model.addSeqSelectionListener(this);
		this.seqSelectionChanged(new SeqSelectionEvent(this, Collections.<BioSeq>singletonList(model.getSelectedSeq())));
	}

	public void actionPerformed(ActionEvent ae) {
		String query = getUCSCQuery();

		if (!query.isEmpty()) {
			GeneralUtils.browse(UCSC_URL + query);
		} else {
			ErrorHandler.errorPanel("Unable to map genome '" + SEQ_MAP.getAnnotatedSeq().getVersion() + "' to a UCSC genome.");
		}
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		boolean enableThis = evt.getSelectedSeq() != null;	
		// don't do the enabling tests, because it will contact the UCSC server when it's not truly necessary.
		this.setEnabled(enableThis);
	}

	/** Returns the genome UcscVersion in UCSC two-letter plus number format, like "hg17". */
	private static String getUcscGenomeVersion(String version) {
		initUCSCSources();
		String ucsc_version = LOOKUP.findMatchingSynonym(UCSCSources, version);
		return UCSCSources.contains(ucsc_version) ? ucsc_version : "";
	}

	private static void initUCSCSources() {
		synchronized(UCSCSources) {
			if (UCSCSources.isEmpty()) {
				// Get the sources from the UCSC server.  If the server has already been initialized, get from there.
				// This is done to avoid additional slow DAS queries.
				DasServerInfo ucsc = null;
				GenericServer server = null;
				if ((server = ServerList.getServer(UCSC_DAS_URL)) != null) {
					// UCSC server already exists!
					ucsc = (DasServerInfo)server.serverObj;
				} else {
					ucsc = new DasServerInfo(UCSC_DAS_URL);
				}
				UCSCSources.addAll(ucsc.getDataSources().keySet());
			}
		}
	}

	/**
	 * generates part of UCSC query url for current genome coordinates.
	 * @return query URL for current view. "" on error.
	 */
	public static String getUCSCQuery(){
		BioSeq aseq = SEQ_MAP.getAnnotatedSeq();

		if (aseq == null) { return ""; }

        String UcscVersion = getUcscGenomeVersion(aseq.getVersion());
        if(!UcscVersion.isEmpty()){
            return "db=" + UcscVersion + "&position=" + getRegionString();
        }

        return "";
    }

	/**
	 *  Returns the current position in the format used by the UCSC browser.
	 *  This format is also understood by GBrowse and the MapRangeBox of IGB.
	 *  @return a String such as "chr22:15916196-31832390", or null.
	 */
	private static String getRegionString() {
		Rectangle2D.Double vbox = SEQ_MAP.getSeqMap().getView().getCoordBox();

		return SEQ_MAP.getAnnotatedSeq().getID() + ":" + (int)vbox.x + "-" + (int)(vbox.x + vbox.width);
	}
}
