package com.affymetrix.igb.action;

import com.affymetrix.genometryImpl.BioSeq;
import com.affymetrix.genometryImpl.GenometryModel;
import com.affymetrix.genometryImpl.event.SeqSelectionEvent;
import com.affymetrix.genometryImpl.event.SeqSelectionListener;
import com.affymetrix.genoviz.widget.NeoAbstractWidget;
import com.affymetrix.igb.IGB;
import com.affymetrix.genometryImpl.util.MenuUtil;
import com.affymetrix.igb.view.SeqMapView;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.text.MessageFormat;
import java.util.Collections;
import javax.swing.AbstractAction;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Timer;

import static com.affymetrix.igb.IGBConstants.BUNDLE;

/**
 *
 * @author sgblanch
 * @version $Id: AutoScrollAction.java 6324 2010-07-01 20:11:30Z hiralv $
 */
public class AutoScrollAction extends AbstractAction implements SeqSelectionListener {
	private static final long serialVersionUID = 1l;
	/*
	 *  units to scroll are either in pixels or bases
	 */
	private Timer swing_timer = null;
	private int as_bases_per_pix = 75;
	private int as_pix_to_scroll = 4;
	private int as_time_interval = 20;
	private int as_start_pos = 0;
	private int as_end_pos;
	public ActionListener map_auto_scroller = null;

	private static final AutoScrollAction action = new AutoScrollAction();

	private AutoScrollAction() {
		super(MessageFormat.format(
					BUNDLE.getString("menuItemHasDialog"),
					BUNDLE.getString("autoScroll")),
				MenuUtil.getIcon("toolbarButtonGraphics/media/Movie16.gif"));
		this.putValue(MNEMONIC_KEY, KeyEvent.VK_A);

		GenometryModel model = GenometryModel.getGenometryModel();
		model.addSeqSelectionListener(this);
		this.seqSelectionChanged(new SeqSelectionEvent(this, Collections.<BioSeq>singletonList(model.getSelectedSeq())));
	}

	public static AutoScrollAction getAction() { return action; }

	public void actionPerformed(ActionEvent ae) {
		this.toggleAutoScroll();
	}

	public void seqSelectionChanged(SeqSelectionEvent evt) {
		this.setEnabled(evt.getSelectedSeq() != null);
	}

	public final void toggleAutoScroll() {
		this.toggleAutoScroll(IGB.getSingleton().getMapView());
	}

	private final void toggleAutoScroll(final SeqMapView seqMapView) {
		if (map_auto_scroller == null) {
			if (seqMapView.getViewSeq() == null) {
				return;
			}

			JPanel pan = new JPanel();

			Rectangle2D.Double cbox = seqMapView.getSeqMap().getViewBounds();
			int bases_in_view = (int) cbox.width;
			as_start_pos = (int) cbox.x;
			as_end_pos = seqMapView.getViewSeq().getLength();
			int pixel_width = seqMapView.getSeqMap().getView().getPixelBox().width;
			as_bases_per_pix = bases_in_view / pixel_width;

			// as_bases_per_pix *should* be a float, or else should simply
			// use the current resolution without asking the user,
			// but since it is an integer, we have to set the minimum value as 1
			if (as_bases_per_pix < 1) {
				as_bases_per_pix = 1;
			}

			final JTextField bases_per_pixTF = new JTextField("" + as_bases_per_pix);
			final JTextField pix_to_scrollTF = new JTextField("" + as_pix_to_scroll);
			final JTextField time_intervalTF = new JTextField("" + as_time_interval);
			final JTextField start_posTF = new JTextField("" + as_start_pos);
			final JTextField end_posTF = new JTextField("" + as_end_pos);

			float bases_per_minute = (float) // 1000 ==> ms/s , 60 ==> s/minute, as_time_interval ==> ms/scroll
							(1.0 * as_bases_per_pix * as_pix_to_scroll * 1000 * 60 / as_time_interval);
			bases_per_minute = Math.abs(bases_per_minute);
			float minutes_per_seq = seqMapView.getViewSeq().getLength() / bases_per_minute;
			final JLabel bases_per_minuteL = new JLabel("" + (bases_per_minute / 1000000));
			final JLabel minutes_per_seqL = new JLabel("" + (minutes_per_seq));

			pan.setLayout(new GridLayout(7, 2));
			pan.add(new JLabel("Resolution (bases per pixel)"));
			pan.add(bases_per_pixTF);
			pan.add(new JLabel("Scroll increment (pixels)"));
			pan.add(pix_to_scrollTF);
			pan.add(new JLabel("Starting base position"));
			pan.add(start_posTF);
			pan.add(new JLabel("Ending base position"));
			pan.add(end_posTF);
			pan.add(new JLabel("Time interval (milliseconds)"));
			pan.add(time_intervalTF);
			pan.add(new JLabel("Megabases per minute:  "));
			pan.add(bases_per_minuteL);
			pan.add(new JLabel("Total minutes for seq:  "));
			pan.add(minutes_per_seqL);

			ActionListener al = new ActionListener() {

				public void actionPerformed(ActionEvent evt) {
					as_bases_per_pix = normalizeTF(bases_per_pixTF, as_bases_per_pix, 1, Integer.MAX_VALUE);
					as_pix_to_scroll = normalizeTF(pix_to_scrollTF, as_pix_to_scroll, -1000, 1000);
					as_time_interval = normalizeTF(time_intervalTF, as_time_interval, 1, 1000);
					as_end_pos = normalizeTF(end_posTF, as_end_pos, 1, seqMapView.getViewSeq().getLength());
					as_start_pos = normalizeTF(start_posTF, as_start_pos, 0, as_end_pos);

					float bases_per_minute = (float) // 1000 ==> ms/s , 60 ==> s/minute, as_time_interval ==> ms/scroll
									(1.0 * as_bases_per_pix * as_pix_to_scroll * 1000 * 60 / as_time_interval);
					bases_per_minute = Math.abs(bases_per_minute);
					float minutes_per_seq = (as_end_pos - as_start_pos) / bases_per_minute;
					bases_per_minuteL.setText("" + (bases_per_minute / 1000000));
					minutes_per_seqL.setText("" + (minutes_per_seq));
				}
			};

			bases_per_pixTF.addActionListener(al);
			pix_to_scrollTF.addActionListener(al);
			time_intervalTF.addActionListener(al);
			start_posTF.addActionListener(al);
			end_posTF.addActionListener(al);

			int val = JOptionPane.showOptionDialog(seqMapView, pan, "AutoScroll Parameters",
							JOptionPane.OK_CANCEL_OPTION,
							JOptionPane.PLAIN_MESSAGE,
							null, null, null);
			if (val == JOptionPane.OK_OPTION) {
				as_bases_per_pix = normalizeTF(bases_per_pixTF, as_bases_per_pix, 1, Integer.MAX_VALUE);
				as_pix_to_scroll = normalizeTF(pix_to_scrollTF, as_pix_to_scroll, -1000, 1000);
				as_time_interval = normalizeTF(time_intervalTF, as_time_interval, 1, 1000);
				toggleAutoScroll(seqMapView, as_bases_per_pix, as_pix_to_scroll, as_time_interval, as_start_pos, as_end_pos, true);
			}
		} else {
			swing_timer.stop();
			swing_timer = null;
			map_auto_scroller = null;
		}
	}

	// Normalize a text field so that it holds an integer, with a fallback value
	// if there is a problem, and a minimum and maximum
	private static int normalizeTF(JTextField tf, int fallback, int min, int max) {
		int result = fallback;
		try {
			result = Integer.parseInt(tf.getText());
		} catch (NumberFormatException nfe) {
			Toolkit.getDefaultToolkit().beep();
			result = fallback;
		}
		if (result < min) {
			result = min;
		} else if (result > max) {
			result = max;
		}
		tf.setText(Integer.toString(result));
		return result;
	}

	private void toggleAutoScroll(final SeqMapView seqMapView, int bases_per_pixel, int pix_to_scroll,
					int timer_interval, final int start_coord, final int end_coord, final boolean cycle) {
		double pix_per_coord = 1.0 / (double) bases_per_pixel;
		final double coords_to_scroll = (double) pix_to_scroll / pix_per_coord;

		seqMapView.getSeqMap().zoom(NeoAbstractWidget.X, pix_per_coord);
		seqMapView.getSeqMap().scroll(NeoAbstractWidget.X, start_coord);

		if (map_auto_scroller == null) {
			map_auto_scroller = new ActionListener() {

				public void actionPerformed(ActionEvent evt) {
					Rectangle2D.Double vbox = seqMapView.getSeqMap().getViewBounds();
					int scrollpos = (int) (vbox.x + coords_to_scroll);
					if ((scrollpos + vbox.width) > end_coord) {
						if (cycle) {
							seqMapView.getSeqMap().scroll(NeoAbstractWidget.X, start_coord);
							seqMapView.getSeqMap().updateWidget();
						} else {
							// end of sequence reached, so stop scrolling
							swing_timer.stop();
							swing_timer = null;
							map_auto_scroller = null;
						}
					} else {
						seqMapView.getSeqMap().scroll(NeoAbstractWidget.X, scrollpos);
						seqMapView.getSeqMap().updateWidget();
					}
				}
			};

			swing_timer = new javax.swing.Timer(timer_interval, map_auto_scroller);
			swing_timer.start();
			// Other options:
			//    java.util.Timer ??
			//    com.affymetrix.genoviz.util.NeoTimerEventClock ??
		} else {
			swing_timer.stop();
			swing_timer = null;
			map_auto_scroller = null;
		}
	}
}
