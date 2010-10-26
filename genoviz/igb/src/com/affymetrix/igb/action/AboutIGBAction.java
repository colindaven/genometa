/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.util.GeneralUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import com.affymetrix.igb.IGB;
import com.affymetrix.genometryImpl.util.MenuUtil;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.MessageFormat;
import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import static com.affymetrix.igb.IGBConstants.APP_NAME;
import static com.affymetrix.igb.IGBConstants.APP_VERSION_FULL;
import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 * Open a window showing information about Integrated Genome Browser.
 * @author sgblanch
 */
public class AboutIGBAction extends AbstractAction {
	private static final long serialVersionUID = 1l;

	public AboutIGBAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					MessageFormat.format(
						BUNDLE.getString("about"),
						APP_NAME)),
				MenuUtil.getIcon("toolbarButtonGraphics/general/About16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_A);
	}

	public void actionPerformed(ActionEvent e) {
		JPanel message_pane = new JPanel();
		message_pane.setLayout(new BoxLayout(message_pane, BoxLayout.Y_AXIS));
		JTextArea about_text = new JTextArea();
		about_text.setEditable(false);
		String text = APP_NAME + ", version: " + APP_VERSION_FULL + "\n\n" +
						"IGB (pronounced ig-bee) is a product of the open source Genoviz project,\n" +
						"which develops interactive visualization software for genomics.\n\n" +
						"If you use IGB to create images for publication, please cite the IGB\n" +
						"Applications Note:\n\n" +
						"Nicol JW, Helt GA, Blanchard SG Jr, Raja A, Loraine AE.\n" +
						"The Integrated Genome Browser: free software for distribution and exploration of\n" +
						"genome-scale datasets.\n"+
						"Bioinformatics. 2009 Oct 15;25(20):2730-1.\n\n"+

						"For more details, including license information, see:\n" +
						"\thttp://www.bioviz.org/igb\n" +
						"\thttp://genoviz.sourceforge.net\n\n" ;
		about_text.append(text);
		String cache_root = com.affymetrix.genometryImpl.util.LocalUrlCacher.getCacheRoot();
		File cache_file = new File(cache_root);
		if (cache_file.exists()) {
			about_text.append("\nCached data stored in: \n");
			about_text.append("  " + cache_file.getAbsolutePath() + "\n");
		}
		String data_dir = PreferenceUtils.getAppDataDirectory();
		if (data_dir != null) {
			File data_dir_f = new File(data_dir);
			about_text.append("\nApplication data stored in: \n  " +
							data_dir_f.getAbsolutePath() + "\n");
		}

		message_pane.add(new JScrollPane(about_text));
		JButton igb_paper = new JButton("View IGB Paper");
		JButton bioviz_org = new JButton("Visit Bioviz.org");
		// vikram JButton request_feature = new JButton("Request a Feature");
		// vikram JButton report_bug = new JButton("Report a Bug");
		igb_paper.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				GeneralUtils.browse("http://www.ncbi.nlm.nih.gov/pmc/articles/PMC2759552/?tool=pubmed");
			}
		});
		bioviz_org.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent evt) {
				GeneralUtils.browse("http://www.bioviz.org");
			}
		});
			
		JPanel buttonP = new JPanel(new GridLayout(1, 2));
		buttonP.add(igb_paper);
		buttonP.add(bioviz_org);
	
		message_pane.add(buttonP);

		final JOptionPane pane = new JOptionPane(message_pane, JOptionPane.INFORMATION_MESSAGE,
						JOptionPane.DEFAULT_OPTION);
		final JDialog dialog = pane.createDialog(IGB.getSingleton().getFrame(), MessageFormat.format(BUNDLE.getString("about"), APP_NAME));
		dialog.setVisible(true);
	}
}
