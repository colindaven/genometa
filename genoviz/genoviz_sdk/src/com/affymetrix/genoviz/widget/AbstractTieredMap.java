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

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.awt.Dimension;

import com.affymetrix.genoviz.event.TierEvent;
import com.affymetrix.genoviz.event.TierEventListener;
import com.affymetrix.genoviz.event.TierStateChangeEvent;
import com.affymetrix.genoviz.event.TierStateChangeListener;
import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;
import java.awt.geom.Rectangle2D;

/**
 * Basis for maps that contain horizontal "tiers" of glyphs.
 * The tiers can be manipulated separately.
 */
public abstract class AbstractTieredMap
	extends NeoMap
	implements TierEventListener, TierStateChangeListener {

	/**
	 *  GAH 10-6-99 trying to allow both types of notification for the moment.
	 *  (since ScaffoldAssemblyView is relying on TieredNeoMap/TieredLabelMap combo, which
	 *      needs to supress pack notification except on repack(), and
	 *      ScaffoldAnnotationView is relying on TieredNeoMap/LegendMap combo, which needs
	 *      to be notified of every call to packTiers() )
	 */
	boolean notifyOnPackTiers = false;

	public boolean debug_events = false; // for debugging only
	public String name; // for debugging only

	protected List<MapTierGlyph> tiers = new ArrayList<MapTierGlyph>();
	private final Set<TierEventListener> tierEventListeners = new CopyOnWriteArraySet<TierEventListener>();
	private boolean notifyingListeners = false;

	/**
	 * If anchored then tiers are anchored at a specific location.
	 * First tier upper edge (y)  is at anchor_location
	 */
	private Dimension preferred_size = new Dimension ( 20, 20 );

	protected boolean anchored= true;

	/**
	 * anchor location (in map coords)
	 * for first tier. (only used if anchor == true)
	 */
	protected double anchor_location = 0;

	public AbstractTieredMap (boolean hscroll_show, boolean vscroll_show) {
		super (hscroll_show, vscroll_show);
	}

	public AbstractTieredMap () {
		super();
	}

	/**
	 * adds to method in NeoWidget, destroying references to assorted stuff to
	 * facilitate gc'ing.
	 */
	public void destroy() {
		super.destroy();
		clearWidget();
		viewbox_listeners.clear();
	}

	public void setNotifyOnPackTiers(boolean b) {
		notifyOnPackTiers = b;
	}

	/**
	 * Add a listener to the audience.
	 */
	public synchronized void addTierEventListener(TierEventListener tel) {
		tierEventListeners.add(tel);
	}

	/**
	 * Remove a listener from the audience.
	 */
	public synchronized void removeTierEventListener(TierEventListener tel) {
		tierEventListeners.remove(tel);
	}

	/**
	 * Tell all listeners of an event.
	 */
	public synchronized void notifyTierEventListeners(TierEvent evt) {
		if (debug_events) {
			if (evt.getTier() == null) {
				System.out.println(name + " notifying listeners of event: " +
						evt.getTypeString() + ", tier = null");
			}
			else {
				System.out.println(name + " notifying listeners of event: " +
						evt.getTypeString() + ", tier = " +
						evt.getTier().getLabel() + ", state = " +
						MapTierGlyph.getStateString(evt.getTier().getState()));
			}
		}
		if ( notifyingListeners ) return;
		notifyingListeners = true;
		for (TierEventListener l : tierEventListeners) {
			l.heardTierEvent(evt);
		}
		notifyingListeners = false;
	}

	/* TierStateChangeListener implementation */

	public void heardTierStateChangeEvent (TierStateChangeEvent evt) {
		if ( notifyingListeners ) return;

		// Distill the relevant tier from the event

		MapTierGlyph tier = (MapTierGlyph) evt.getSource();

		if (tier == null)
			return;

		// And tell the label map's listeners about it

		TierEvent te = new TierEvent (this, TierEvent.STATE_CHANGED, tier);
		notifyTierEventListeners (te);
	}



	public boolean isAnchored() {
		return anchored;
	}

	public void setAnchored(boolean anchored) {
		this.anchored = anchored;
	}

	public void setAnchorLocation(double loc) {
		anchor_location = loc;
	}

	public double getAnchorLocation() {
		return anchor_location;
	}

	/**
	 * overriding NeoMap.repack() to pack tiers -- generates a TierEvent.
	 */
	public void repack() {
		// _ONLY_ tiers and glyphs placed in tiers will be repacked --
		// anything added directly to map other than tiers will need to
		// be dealt with manually
		if (debug_events) {
			System.out.println(name + " repacking tiers");
		}
		packTiers (true, true);

		if (! notifyOnPackTiers) {
			notifyTierEventListeners  (new TierEvent (this, TierEvent.REPACK, null));
		}
	}

	/**
	 * pack tiers in order.
	 * @param full_repack if true, packs the contents of the tiers as well
	 * as the tiers with respect to each other.  tiers will always be packed
	 * with respect to each other no matter what.
	 * @param stretch_map reshapes the map to fit all of the tiers.
	 */
	public void packTiers(boolean full_repack, boolean stretch_map)  {
		if (full_repack) {
			for (int i=0; i<tiers.size(); i++) {
				MapTierGlyph mtg = tiers.get(i);
				mtg.pack(getView());
			}
		}

		Rectangle2D.Double mbox = getCoordBounds();

		double offset;
		double height = mbox.height;
		MapTierGlyph mtg;

		if (anchored) {
			offset = anchor_location;
		}
		else {
			offset = mbox.y;
		}
		for (int i=0; i<tiers.size(); i++) {
			mtg = tiers.get(i);

			// don't make room if tier isn't visible, or if it's hidden

			if (!mtg.isVisible() || (mtg.getState() == MapTierGlyph.HIDDEN))
				continue;

			height = mtg.getCoordBox().height;

			// need to call moveAbsolute to trigger recursive move of
			//   all children

			mtg.moveAbsolute(mbox.x, offset);
			mtg.setCoords(mbox.x, offset, mbox.width, height);
			offset = offset + height;
		}

		if (stretch_map) {
			if (tiers.size() <= 0) { return; }
			Rectangle2D.Double pbox = getCoordBounds();
			Rectangle2D.Double newbox = null;
			mtg = null;

			for (int i=0; i<tiers.size(); i++) {
				mtg = tiers.get(i);
				if ( mtg.getState() == MapTierGlyph.HIDDEN ) continue;
				else if ( newbox == null ) {
					newbox = new Rectangle2D.Double();
					newbox.setRect(pbox.x, mtg.getCoordBox().y,
							pbox.width, mtg.getCoordBox().height);
				}
				else {
					Rectangle2D.union(newbox, mtg.getCoordBox(), newbox);
				}
			}

			if ( newbox != null )
				setMapOffset((int) newbox.y, (int) (newbox.y + newbox.height));

			updateWidget();
		}

		if (notifyOnPackTiers) notifyTierEventListeners(new TierEvent(
					this, TierEvent.REPACK, null, full_repack, stretch_map));
	}


	/**
	 * Get the location of the given tier in the map.
	 * @return 0..n, the location counting top-down,
	 * or -1 if the given tier is not on the map.
	 */
	public int indexOf (MapTierGlyph mtg) {
		return tiers.indexOf(mtg);
	}

	/**
	 * Get the tier at the given location, or null.
	 * @param i the index, counting top-down, beginning with 0.
	 */
	public MapTierGlyph getTierAt(int i) {
		if (i>=0 && i<tiers.size()) {
			return tiers.get(i);
		}
		else return null;
	}

	/**
	 * Get all the tiers for the TieredNeoMap.
	 * @return vector of tiers.
	 */
	public List getAllTiers( ) {
		return tiers;
	}

	public int getTierCount() {
		return tiers.size();
	}

	public void clearWidget() {
		for ( int i = 0; i < tiers.size(); i++ ) {
			MapTierGlyph m = tiers.get(i);
			m.removeAllTierStateChangeListeners();
			m.removeChildren();
		}
		super.clearWidget();
		tiers = new ArrayList<MapTierGlyph>();
	}

	/**
	 * making sure the tiers always stretch the full length of the map.
	 */
	public void setBounds(int axis, int start, int end) {
		super.setBounds(axis, start, end);
		Rectangle2D.Double mbox = getScene().getGlyph().getCoordBox();

		if ((axis != X) || (tiers == null))
			return;

		for (int i=0; i<tiers.size(); i++) {
			MapTierGlyph tier = tiers.get(i);
			Rectangle2D.Double tbox = tier.getCoordBox();
			tier.setCoords(mbox.x, tbox.y, mbox.width, tbox.height);
		}
	}

	/**
	 * Given a MapTierGlyph, remove it from ourself.
	 */
	public void removeTier (MapTierGlyph toRemove) {

		// First, see if we've got such a tier

		if (!tiers.contains(toRemove))
			return;

		// Then tell our listeners to remove the corresponding tier

		TierEvent te = new TierEvent (this, TierEvent.REMOVE, toRemove);
		notifyTierEventListeners (te);

		// Remove the tier's children

		toRemove.removeChildren();

		// Finally, remove our own

		tiers.remove (toRemove);

		repack ();
		updateWidget();

		// remove ourself from the tier's audience

		toRemove.removeTierStateChangeListener (this);
	}


	/**
	 * moves a tier to a new position.
	 *
	 * @param from index of the tier's current position.
	 * @param to index of the tier's desired position.
	 */
	public void moveTier( int from, int to ) {
		from = Math.max( from, 0 );
		to = Math.min( to, tiers.size()-1 );
		if ( from == to ) { // null operation
			return;
		}
		MapTierGlyph mtg = tiers.get( from );
		tiers.remove( from );
		tiers.add( to, mtg );

		repack();
		updateWidget();


	}


	/**
	 * moves a tier to a new position,
	 * both in this map and another.
	 *
	 * @param otherMap map with corresponding tiers.
	 * @param tierLocs an array of two index values.
	 * tierLocs[0] is the old location.
	 * tierLocs[1] is the new location.
	 */
	protected void moveTiers (AbstractTieredMap otherMap, int[] tierLocs) {

		int from,to;
		// First do ourself:
		from = Math.max( tierLocs[0], 0 );
		to = Math.min( tierLocs[0], tiers.size()-1 );
		if ( from == to ) { // null operation
			return;
		}
		MapTierGlyph mtg = tiers.get( from );
		tiers.remove( from );
		tiers.add( to, mtg );

		// Then do the other map:
		mtg = otherMap.tiers.get(tierLocs[0]);
		if (mtg == null)
			return; // Why isn't an exception thrown?
		otherMap.tiers.remove(from);
		otherMap.tiers.add(to, mtg);

	}


	/**
	 * Given another AbstractTieredMap and a tier it contains,
	 * remove our corresponding tier.
	 */
	protected void removeCorrTier (AbstractTieredMap otherMap, MapTierGlyph otherTier) {

		// Figure out which one of our tiers corresponds,

		int loc = otherMap.indexOf (otherTier);

		if ((loc < 0) || loc > tiers.size())
			return;

		// Remove the tier's children

		MapTierGlyph toRemove = tiers.get(loc);

		toRemove.removeChildren();

		// Then the tier itself!

		tiers.remove(toRemove);

		packTiers(true,true);
		updateWidget();

		// remove ourself from the tier's audience

		toRemove.removeTierStateChangeListener (this);

	}


	/** Whether or not this map will allow packToMatch() to have any effect */
	private boolean ableToPackToMatch = true;

	/** Returns ableToPackToMatch. */
	public boolean isAbleToPackToMatch() {return ableToPackToMatch;}

	/** Sets ableToPackToMatch=b. */
	public void setAbleToPackToMatch(boolean b) {ableToPackToMatch=b;}

	/**
	 * Given another AbstractTieredMap,
	 * pack ourself to match, unless ableToPackToMatch is false.
	 * (Setting ableToPackToMatch=false is useful when the tiers in
	 * some other map which might call packToMatch are known to be
	 * temporarily out-of-order with respect to this map.)
	 * @see com.affymetrix.genoviz.widget.TierMapRubberBand#swap(int,int)
	 */
	protected void packToMatch (AbstractTieredMap otherMap, boolean full_repack, boolean stretch_map ) {
		MapTierGlyph ourTier, otherTier;

		if (!ableToPackToMatch) {return;}

		for (int i=0; i < tiers.size(); i++) {

			ourTier   = getTierAt (i);
			otherTier = otherMap.getTierAt (i);

			if (otherTier == null || ourTier == null) return;

			// Deal with the geometries --

			Rectangle2D.Double otherCoords = otherTier.getCoordBox();
			Rectangle2D.Double ourCoords   = ourTier.getCoordBox();

			ourTier.moveAbsolute(ourCoords.x, otherCoords.y);
			ourTier.setCoords (ourCoords.x, otherCoords.y,
					ourCoords.width, otherCoords.height);

			ourCoords = ourTier.getCoordBox();
		}
		/*
		 * It was my original intention that this would read "packTiers ( full_repack, stretch_map )", but
		 * I found that the maps got out of sync anyway -- one with slightly greater height than the other, usually --
		 * and I don't think the resize takes all that long, so I'm making this permanently true for now.  At least
		 * full repacks aren't happening all the time.  If someone feels really ambitious, they can see if they can reduce
		 * the number of map stretchings by putting "stretch_map" in for "true", but you're going to have some dubugging to
		 * do. -JMM 8/3/00
		 */
		packTiers( full_repack, true );
		scrollOffset ( otherMap.getVisibleOffset()[0] );
		updateWidget();
	}

	/**
	 * Given a map and a tier it contains,
	 * adjust the state of our corresponding tier appropriately.
	 */
	protected MapTierGlyph adjustState (AbstractTieredMap otherMap,
			MapTierGlyph otherTier) {

		// Figure out which one of our tiers corresponds,

		int loc = otherMap.indexOf (otherTier);

		if ((loc < 0) || loc > tiers.size())
			return null;

		// And set the state of our corresponding tier appropriately.

		MapTierGlyph ourTier = tiers.get(loc);

		// Access the properties directly, rather than via access
		// methods -- avoid ping-pongs.

		ourTier.setState(otherTier.getState());
		if (debug_events) {
			System.out.println(name + " setting state of tier " +
					ourTier.getLabel() + " to " +
					MapTierGlyph.getStateString(ourTier.getState()));
		}
		return ourTier;
	}

	public void setPreferredSize ( Dimension d ) {
		preferred_size = d;
	}

	public Dimension getPreferredSize ( ) {
		return preferred_size;
	}

}
