/**
*   Copyright (c) 1998-2008 Affymetrix, Inc.
*
*   Licensed under the Common Public License, Version 1.0 (the "License").
*   A copy of the license must be included with any distribution of
*   this source code.
*
*   The license is also available at
*   http://www.opensource.org/licenses/cpl.php
*/
package com.affymetrix.genoviz.awt;

import com.affymetrix.genoviz.event.NeoPaintEvent;
import com.affymetrix.genoviz.event.NeoPaintListener;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.List;
import java.util.Set;
import javax.swing.JComponent;

/**
 * Extends {@link JComponent}
 * to treat painting as an event
 * that can be listened for (by a {@link NeoPaintListener}).
 */
public class NeoCanvas extends JComponent  {
  private final Set<NeoPaintListener> paintListeners = new CopyOnWriteArraySet<NeoPaintListener>();

  /**
   * Paints the component,
   * notifying listeners afterwards
   * via a {@link NeoPaintEvent}.
   *
   * @param g the specified Graphics object
   * @see #update
   */
  @Override
  public void paintComponent(Graphics g) {
    // Neither paint() nor paintComponent() really does much in NeoCanvas.
    // Instead, it posts a paint event which will be heard by the ViewI,
    // causing the ViewI to "draw" itself.
    //TODO: consider letting paintCompoent just directly paint children.
    if (paintListeners.size() > 0) {
      final Rectangle paintrect = getPaintRect(g);
      postPaintEvent(new NeoPaintEvent(this, paintrect, (Graphics2D) g));
    }
  }

  protected Rectangle getPaintRect(Graphics g) {
    final Rectangle cliprect = g.getClipBounds();
    final Rectangle paintrect;
    // sometimes after a resize the cliprect starts out as null, so
    //    checking to avoid NullPointerExceptions
    if (cliprect != null) {
      paintrect = new Rectangle(cliprect.x, cliprect.y,
        cliprect.width, cliprect.height);
    } else {
      paintrect = new Rectangle(getSize());
    }
    return paintrect;
  }
  
  /**
   * Lets all the listeners know that this has been painted.
   *
   * @param e an event to pass to them all.
   */
  public void postPaintEvent(NeoPaintEvent e) {
    for (NeoPaintListener npl : paintListeners) {
      npl.componentPainted(e);
    }
  }

  /**
   * Adds the specified listener to those receiving notification
   * of painting this NeoCanvas.
   *
   * @param pl the listener
   */
  public void addNeoPaintListener(NeoPaintListener pl) {
    paintListeners.add(pl);
  }

  /**
   * Removes the specified event listener
   * so it no longer receives notification of events
   * from this NeoCanvas.
   *
   * @param pl the listener
   */
  public void removeNeoPaintListener(NeoPaintListener pl) {
    paintListeners.remove(pl);
  }

  /**
   * Gets the objects
   * that are listening for this NeoCanvas being painted.
   *
   * @return a List of all the NeoPaintListeners to this NeoCanvas.
   */
  public Set<NeoPaintListener> getNeoPaintListeners() {
    return paintListeners;
  }
}
