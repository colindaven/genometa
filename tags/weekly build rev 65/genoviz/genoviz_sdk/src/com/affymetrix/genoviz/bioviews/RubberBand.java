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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.util.concurrent.CopyOnWriteArraySet;
import java.awt.event.*;

import com.affymetrix.genoviz.event.NeoRubberBandEvent;
import com.affymetrix.genoviz.event.NeoRubberBandListener;


public class RubberBand implements MouseListener, MouseMotionListener  {

	private static final boolean debug = false;

	// need to resolve EventSourceI vs. Component !!!
	protected Component comp;
	protected Color color;
	protected CopyOnWriteArraySet<NeoRubberBandListener> listeners;
	protected Rectangle pixelbox;
	protected int xorigin, yorigin;
	protected boolean forward, drawn, started;
	protected int startEventID, stretchEventID, endEventID;
	protected int startEventMask, stretchEventMask, endEventMask;
	protected int startEventKey, stretchEventKey, endEventKey;
	protected int startClickCount;

	/**
	 * constructs a rubber band for the given component.
	 */
	public RubberBand(Component c) {
		this();
		setComponent( c );
	}

	/**
	 * constructs a rubber band independant of a component.
	 */
	public RubberBand() {
		color = Color.black;
		listeners = new CopyOnWriteArraySet<NeoRubberBandListener>();
		drawn = false;
		started = false;
		startEventID = MouseEvent.MOUSE_PRESSED;
		stretchEventID = MouseEvent.MOUSE_DRAGGED;
		endEventID = MouseEvent.MOUSE_RELEASED;

		startEventMask = stretchEventMask = endEventMask = 0;
		startEventKey = stretchEventKey = endEventKey = 0;
		startClickCount = 1;
	}

	/**
	 * assignes a component to this rubber band.
	 *
	 * @param c the component where the rubber band is active.
	 */
	public void setComponent( Component c ) {
		this.comp = c;
	}

	public void start(int x, int y) {
		start(x, y, 0, 0);
	}


	public void start(int x, int y, int width, int height) {
		if (drawn) { drawXOR(); }
		pixelbox = new Rectangle(x, y, width, height);
		if (debug)  { System.out.println("*** Starting rubber band: " + pixelbox);}
		xorigin = x;
		yorigin = y;
		forward = true;
		drawn = true;
		started = true;
		drawXOR();
	}

	public void stretch(int x, int y) {
		drawXOR();
		if (x >= xorigin) {
			pixelbox.x = xorigin;
			pixelbox.width = x - xorigin;
			forward = true;
		}
		else {
			pixelbox.x = x;
			pixelbox.width = xorigin - x;
			forward = false;
		}
		if (y >= yorigin) {
			pixelbox.y = yorigin;
			pixelbox.height = y - yorigin;
		}
		else {
			pixelbox.y = y;
			pixelbox.height = yorigin - y;
		}
		drawXOR();
	}

	public void end() {
		if (debug)  { System.out.println("*** ending rubber band: " + pixelbox); }
		drawXOR();
		if (debug)  { System.out.println(" "); }
		drawn = false;
		started = false;
	}

	public void drawXOR() {
		Graphics g = comp.getGraphics();
		if (debug)  { System.out.println("XOR: " + pixelbox + ", " + color); }
		if (pixelbox.width != 0 && pixelbox.height != 0) {
			g.setXORMode(comp.getBackground());
			g.setColor(color);
			g.drawRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);
			g.setPaintMode();
		}
	}

	public void setStartEvent(int id, int mask, int key, int clicks) {
		startEventID = id;
		startEventMask = mask;
		startEventKey = key;
		startClickCount = clicks;
	}

	public void setStretchEvent(int id, int mask, int key) {
		stretchEventID = id;
		stretchEventMask = mask;
		stretchEventKey = key;
	}

	public void setEndEvent(int id, int mask, int key) {
		endEventID = id;
		endEventMask = mask;
		endEventKey = key;
	}

	// handling events on the rubberband's components
	//  (maybe multiple components??? -- unlikely, it's
	//   a component-specific thing)
	//  public void heardEvent(Event evt) {
	public void heardEvent(MouseEvent evt) {
		int nid;
		Component source = (Component)evt.getSource();
		if (!(source == comp)) { return; }
		int id = evt.getID();
		int modifiers = evt.getModifiers();
		int clickCount = evt.getClickCount();
		int x = evt.getX();
		int y = evt.getY();
		if (id == startEventID && clickCount >= startClickCount && !started) {
			// CLH: 4/6/98 clickCount == startClickCount changed to
			// clickCount >= startClickCount to workaround Mac Navigator bug!

			// && modifiers = startEventMask && evt.key = startEventKey
			start(x, y);
			nid = NeoRubberBandEvent.BAND_START;
		}
		else if (id == stretchEventID && started) {
			// && modifiers = stretchEventMask && evt.key = stretchEventKey
			stretch(x, y);
			nid = NeoRubberBandEvent.BAND_STRETCH;
		}
		else if (id == endEventID && started) {
			// && modifiers = endEventMask && evt.key = endEventKey
			end();
			nid = NeoRubberBandEvent.BAND_END;
		}
		else { return; }

		if (listeners.size() > 0) {
			NeoRubberBandEvent rbevent =
				new NeoRubberBandEvent(source, nid, evt.getWhen(), modifiers,
						x, y, clickCount, evt.isPopupTrigger(), this);
			processEvent(rbevent);
		}
	}

	protected void processEvent(NeoRubberBandEvent evt) {
		for (NeoRubberBandListener listener :listeners)  {
			listener.rubberBandChanged(evt);
		}
	}

	public void addRubberBandListener(NeoRubberBandListener listener)  {
		listeners.add(listener);
	}

	public void removeRubberBandListener(NeoRubberBandListener listener)  {
		listeners.remove(listener);
	}

	public void setColor(Color c) { color = c; }
	public Color getColor() { return color; }
	public Rectangle getBoundingBox() { return pixelbox; }

	public void mouseClicked(MouseEvent e) { heardEvent(e); }
	public void mouseEntered(MouseEvent e) { heardEvent(e); }
	public void mouseExited(MouseEvent e) { heardEvent(e); }
	public void mousePressed(MouseEvent e) { heardEvent(e); }
	public void mouseReleased(MouseEvent e) { heardEvent(e); }
	public void mouseDragged(MouseEvent e) {
		if (e.getID() == stretchEventID) {
			heardEvent(e);
		}
	}
	public void mouseMoved(MouseEvent e) {
		if (e.getID() == stretchEventID) {
			heardEvent(e);
		}
	}

	public void clearRubberBand() {
		drawn = false;
		started = false;
	}

	}
