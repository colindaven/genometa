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

package com.affymetrix.genoviz.widget;

import com.affymetrix.genoviz.event.NeoWidgetEvent;
import com.affymetrix.genoviz.event.NeoWidgetListener;
import java.awt.AWTEventMulticaster;
import java.util.EventListener;

/**
 * a convenience for implementing notification of a set of listeners.
 */
public class NeoEventMulticaster extends AWTEventMulticaster  implements NeoWidgetListener {

	protected NeoEventMulticaster( EventListener a, EventListener b ) {
		super( a, b );
	}

	public static NeoWidgetListener add( NeoWidgetListener listener, NeoWidgetListener l ) {
		EventListener el = addInternal( listener, l );
		return ( NeoWidgetListener ) el;
	}

	public static NeoWidgetListener remove( NeoWidgetListener l, NeoWidgetListener oldl ) {
		EventListener el = removeInternal( l, oldl );
		return ( NeoWidgetListener ) el;
	}

	/**
	 * notifies all the listeners that a widget has been cleared.
	 */
	public void widgetCleared( NeoWidgetEvent e ) {
		( ( NeoWidgetListener ) this.a ).widgetCleared( e );
		( ( NeoWidgetListener ) this.b ).widgetCleared( e );
	}

}
