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

package com.affymetrix.genoviz.event;

import com.affymetrix.genoviz.widget.AbstractTieredMap;
import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;
import java.util.EventObject;

public class TierEvent extends EventObject {

	/** A valid event type -- recipients are to repack */
	public static final int REPACK = 1111;

	/** A valid event type -- recipients are to add a tier */
	public static final int ADD = 2222;
	public static final int ADD_BOTTOM = 2223;
	public static final int ADD_TOP = 2224;

	/** A valid event type -- recipients are to remove a tier */
	public static final int REMOVE = 3333;

	/** A valid event type -- recipients are to reorder a tier */
	public static final int REORDER = 4444;

	/** A valid event type -- recipients are to recognize a state change */
	public static final int STATE_CHANGED = 5555;

	protected int type;
	protected AbstractTieredMap source;
	protected MapTierGlyph tier;
	protected int[] moveLocs;
	protected boolean full_repack;
	protected boolean stretch_map;

	/**
	 * Make a new tier event for the given parameters.
	 * @param full_repack In the event of a REPACK event type, intended to indicate the type of repack.  Should be meaningless otherwise.
	 * @param stretch_map In the event of a REPACK event type, intended to indicate the type of repack.  Should be meaningless otherwise.
	 */
	public TierEvent (AbstractTieredMap src, int evtType, MapTierGlyph evtTier, boolean full_repack, boolean stretch_map) {
		super(src);
		source = src;
		type = evtType;
		tier = evtTier;
		this.full_repack = full_repack;
		this.stretch_map = stretch_map;
	}

	public TierEvent (AbstractTieredMap src, int evtType, MapTierGlyph evtTier ) {
		this ( src, evtType, evtTier, true, true );
	}

	/**
	 * Get the MapTierGlyph which this event represents
	 */
	public MapTierGlyph getTier() {
		return tier;
	}

	/**
	 * Get the type which this event represents.
	 * @return should be one of:
	 * {@link #REPACK},
	 * {@link #ADD},
	 * {@link #REMOVE},
	 * {@link #REORDER}, or
	 * {@link #STATE_CHANGED}.
	 */
	public int getType() {
		return type;
	}

	/**
	 * When the event is of type REORDER, set this property with an
	 * array of size 2 -- [start location, end location].
	 * Locations are top-down, zero-based.
	 */
	public void setMoveLocs (int[] moveLocs) {
		if (moveLocs == null)
			return;
		this.moveLocs = new int[2];
		this.moveLocs[0] = moveLocs[0];
		this.moveLocs[1] = moveLocs[1];
	}

	/**
	 * When the event is of type REORDER, get this property, an
	 * array of size 2 -- [start location, end location].
	 * Locations are top-down, zero-based.
	 */
	public int[] getMoveLocs () {
		return moveLocs;
	}

	public String getTypeString() {
		if (type == REPACK) { return "REPACK"; }
		else if (type == ADD) { return "ADD"; }
		else if (type == REMOVE) { return "REMOVE"; }
		else if (type == REORDER) { return "REORDER"; }
		else if (type == STATE_CHANGED) { return "STATE_CHANGED"; }
		else { return "UNKNOWN"; }
	}

	/**
	 * Since type REPACK calls can be made by calls to AbstractTieredMap.packTiers ( boolean full_repack, boolean stretch_map ),
	 * the state of those two booleans should be passed on to TierMap listeners so that they can pack the same way.  Previous to
	 * 10/3/00, this information was not here, and AbstractTieredMap was doing a packTiers ( true, true ) in reponse to all
	 * TierEvents of type REPACK.  Defaults to true if the specific constructor is not used.
	 */
	public boolean getFullRepack() {
		return full_repack;
	}

	/**
	 * @see TierEvent#getTypeString()
	 */
	public boolean getStretchMap() {
		return stretch_map;
	}

}
