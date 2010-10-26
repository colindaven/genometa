package com.affymetrix.igb.view.external;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.igb.Application;
import com.affymetrix.igb.action.UCSCViewAction;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.BorderLayout;
import java.awt.Dialog.ModalityType;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 * Base Browser View Component.
 * Shows the view of the current region in other browsers
 * 
 * 
 * @author Ido M. Tamir
 */
public abstract class BrowserView extends JPanel {
	private SwingWorker<Image, Void> worker = null;
	private final Map<String, String> cookieMap = new HashMap<String, String>();
	private final JButton update_button = new JButton("update");
	private final JButton settingsButton = new JButton("settings");
	private BrowserImage browserImage = new BrowserImage();
	private final JScrollPane scroll = new JScrollPane();


	public abstract JDialog getViewHelper(Window window);

	public abstract void initializeCookies();

	public abstract Image getImage(Loc loc, int pixWidth);

	public abstract String getViewName();

	public String getCookie(String key) {
		String value = cookieMap.get(key);
		if (value != null) {
			return value;
		}
		return "";
	}

	public void setCookie(String key, String value) {
		cookieMap.put(key, value);
	}

	public BrowserView(JComboBox selector) {
		super();
		initializeCookies();
		this.setLayout(new BorderLayout());
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

		scroll.setViewportView(browserImage);
		scroll.getVerticalScrollBar().setEnabled(true);
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		this.add("Center", scroll);
		this.add("South", buttonPanel);

		buttonPanel.add(settingsButton);
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(selector);
		buttonPanel.add(Box.createHorizontalStrut(15));
		buttonPanel.add(update_button);
		
		

		update_button.setToolTipText("update the view");
		update_button.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (worker != null) {
					worker.cancel(true);
				}
				final String msg = "Updating " + getViewName() + " View...";
				Application.getSingleton().addNotLockedUpMsg(msg);
				final int pixWidth = scroll.getViewport().getWidth();
				worker = new SwingWorker<Image, Void>() {

					@Override
					public Image doInBackground() {
						String ucscQuery = UCSCViewAction.getUCSCQuery();
						Loc loc = Loc.fromUCSCQuery(ucscQuery);
						if(ucscQuery.equals("") || loc.db.equals("")){
							return BrowserLoader.createErrorImage("could not resolve url for genome", pixWidth);
						}
						return getImage(loc, pixWidth);
					}

					@Override
					public void done() {
						try {
							Image image = get();
							browserImage = new BrowserImage();
							browserImage.setImage(image);
							scroll.setViewportView(browserImage);
						} catch (InterruptedException ignore) {
						} catch (CancellationException ignore) {
						} catch (java.util.concurrent.ExecutionException e) {
							String why = null;
							Throwable cause = e.getCause();
							if (cause != null) {
								why = cause.getMessage();
							} else {
								why = e.getMessage();
							}
							Logger.getLogger(BrowserView.class.getName()).log(Level.FINE, why);
						} finally {
							Application.getSingleton().removeNotLockedUpMsg(msg);
						}
					}
				};
				worker.execute();
			}
		});

		settingsButton.setToolTipText("personalize view");
		settingsButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				final Window window = SwingUtilities.getWindowAncestor(BrowserView.this);
				final JDialog helper = getViewHelper(window);
				helper.setSize(500, 400);
				helper.setModalityType(ModalityType.DOCUMENT_MODAL);

				helper.setVisible(true);
			}
		});
	}
}

