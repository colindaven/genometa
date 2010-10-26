/**
 *   Copyright (c) 1998-2005 Affymetrix, Inc.
 *
 *   Licensed under the Common Public License, Version 1.0 (the "License").
 *   A copy of the license must be included with any distribution of
 *   this source code.
 *   Distributions from Affymetrix, Inc., place this in the
 *   IGB_LICENSE.html file.
 *
 *   The license is also available at
 *   http://www.opensource.org/licenses/cpl.php
 */

package com.affymetrix.genoviz.bioviews;

import java.awt.event.*;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import com.affymetrix.genoviz.awt.NeoCanvas;
import com.affymetrix.genoviz.event.NeoDragEvent;
import com.affymetrix.genoviz.event.NeoDragListener;
import com.affymetrix.genoviz.util.NeoConstants;
import java.awt.Dimension;
import javax.swing.Timer;

@SuppressWarnings(value="deprecation")
public class DragMonitor
	implements NeoConstants, MouseListener, MouseMotionListener, ActionListener {

	private final NeoCanvas can;
	final Set<NeoDragListener> listeners = new CopyOnWriteArraySet<NeoDragListener>();
	boolean already_dragging_outside = false;

	private Timer timer = null;
	protected int initial_delay = 250;
	protected int timer_interval = 100;

  public DragMonitor(NeoCanvas can) {
		this.can = can;
		can.addMouseListener(this);
		can.addMouseMotionListener(this);
	}

	/** implementing MouseListener interface */
	public void mouseClicked(MouseEvent e) { }
	public void mouseEntered(MouseEvent e) { }
	public void mouseExited(MouseEvent e) { }
	public void mousePressed(MouseEvent e) { }
	public void mouseReleased(MouseEvent e) {
		if (timer != null) {
			timer.stop();
			timer = null;
		}
		already_dragging_outside = false;
	}

	/** implementing MouseMotionListener interface */
	public void mouseMoved(MouseEvent e) { }
	public void mouseDragged(MouseEvent evt) {
		Dimension dim = can.getSize();
		int x = evt.getX();
		int y = evt.getY();
		if ((!already_dragging_outside)
				&& (x < 0 || x > dim.width
				|| y < 0 || y > dim.height)) {
			if (timer != null) {
				timer.stop();
			}
			already_dragging_outside = true;

			int direction;
			// direction constants are from com.affymetrix.genoviz.util.NeoConstants
			if (x < 0) {
				direction = WEST;
			} else if (y < 0) {
				direction = NORTH;
			} else if (x > dim.width) {
				direction = EAST;
			} else if (y > dim.height) {
				direction = SOUTH;
			} else {
				direction = NONE;
			}
			Integer dirobj = new Integer(direction);
			timer = new Timer(timer_interval, this);
			timer.setInitialDelay(initial_delay);
			timer.setActionCommand(dirobj.toString());
			timer.start();
		} else if (already_dragging_outside
				&& (x > 0 && x < dim.width
				&& y > 0 && y < dim.height)) {
			already_dragging_outside = false;
			if (timer != null) {
				timer.stop();
				timer = null;
			}
		}
	}

	public void addDragListener(NeoDragListener listener) {
		listeners.add(listener);
	}

	public void removeDragListener(NeoDragListener listener) {
		listeners.remove(listener);
	}

	public Set<NeoDragListener> getDragListeners() {
		return listeners;
	}

	public void actionPerformed(ActionEvent e) {
		String arg = e.getActionCommand();
		int direction = Integer.valueOf(arg);
		NeoDragEvent new_event = new NeoDragEvent(this, direction);
		for (NeoDragListener l : listeners) {
			l.heardDragEvent(new_event);
		}
	}

}
