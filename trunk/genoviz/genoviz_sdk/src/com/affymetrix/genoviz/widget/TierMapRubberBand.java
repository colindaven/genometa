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

import com.affymetrix.genoviz.bioviews.RubberBand;
import java.awt.Rectangle;
import java.util.List;
import com.affymetrix.genoviz.widget.tieredmap.MapTierGlyph;
import java.util.ArrayList;


/**
 * Used for moving tiers on an AbstractTieredMap component.
 * Other maps can register as 'listeners' and be notified when tiers are moved.
 * This only really makes sense for label maps
 * that reflect the same data as the main map.
 */
public class TierMapRubberBand extends RubberBand {

	AbstractTieredMap tiermap = null;
	int[] tiers;

	protected int startedTier;
	protected int tier;
	protected MapTierGlyph glyph;
	protected List<AbstractTieredMap> other_maps;
	protected boolean somethingToErase = false;

	/**
	 * Default constructor.
	 * If no map is specified in constructor,
	 * use setMap method to add it later.
	 */
	public TierMapRubberBand(){
		super();
		other_maps = new ArrayList<AbstractTieredMap>();
	}

	public TierMapRubberBand(AbstractTieredMap ntm){
		super((java.awt.Component)ntm);
		tiermap = ntm;
		other_maps = new ArrayList<AbstractTieredMap>();
	}

	/**
	 * Allows the component to be set.
	 * This is similar to the rubberband's setComponent method.
	 */
	public void setMap(AbstractTieredMap map){
		tiermap = map;
	}

	/**
	 * Allows other maps to be notified of tier movement.
	 * So they can act accordingly.
	 */
	public void addMap(AbstractTieredMap ntm){
		other_maps.add(ntm);
	}

	public void removeMap ( AbstractTieredMap atm ) {
		other_maps.remove(atm);
	}

	public void start(int x, int y){
		start(x,y,0,0);
	}

	// start, end and stretch methods overide rubberband methods.

	public void start(int x, int y , int height, int width){
		if (tiermap !=null) bufferTiers();

		if (tiers.length > 1) {
			pixelbox = new Rectangle(x, y,height, width);

			forward = true;
			drawn = true;
			started = true;

			startedTier = 1; // to avoid and index of -1 after decrement below.
			while ( (tiers.length-1) > startedTier && tiers[startedTier] < y ) startedTier++;
			startedTier--;
			current = startedTier;
			tier = startedTier;
			glyph = tiermap.getTierAt(startedTier);

			if (glyph != null) {
				tiermap.repaint();
				glyph.pack(tiermap.getView());
			}
		}

		somethingToErase = false; // We haven't drawn anything yet!
	}

	// start, end and stretch methods overide rubberband methods

	public void end(){
		drawn = false;
		started = false;
		swap(current, tier);
		tiermap.repaint();
	}

	// start, end and stretch methods overide rubberband methods

	public void stretch(int x, int y){
		if (somethingToErase) drawXOR(); // remove what we've drawn before
		else somethingToErase=true; // next time, there will be somethingToErase
		Rectangle b = comp.getBounds();
		pixelbox.x = 0;
		pixelbox.width = b.width;
		pixelbox.y = yValue(y);
		pixelbox.height = 2;
		if (current != tier && false){  // remove this false to see the tiers move in real time (slowly)
			swap( current, tier);
			current = tier;
		}
		drawXOR();
	}

	protected int current = -1;

	protected void swap( int from, int to){
		if ( to > from ) to--;
		if ( to == from ) return;

		AbstractTieredMap atm;
		final int size = other_maps.size();
		int i;
		boolean mystate;
		boolean[] states = new boolean[size];

		// During this method, the tiers in `tiermap' and the tiers in
		// each of the `other_maps' can be out-of-order with respect to
		// one another.  Thus we must disable the ability of tiers in
		// any two maps from re-packing to match the tiers at the
		// same location in each others maps until all the moving
		// has been completed.
		mystate = tiermap.isAbleToPackToMatch();
		tiermap.setAbleToPackToMatch(false);
		for (i=0; i<size; i++){
			atm = other_maps.get(i);
			states[i] = atm.isAbleToPackToMatch();
			atm.setAbleToPackToMatch(false);
		}

		tiermap.moveTier(from, to);

		for (i=0; i<size; i++){
			atm = other_maps.get(i);
			atm.moveTier( from, to );
		}


		tiermap.setAbleToPackToMatch(mystate);
		for (i=0; i<size; i++){
			atm = other_maps.get(i);
			atm.setAbleToPackToMatch(states[i]);
			atm.repack();
		}
		tiermap.repack();
	}

	// called at the beginning of a drag to create local index of snap values for horizontal rubberband.

	protected void bufferTiers(){
		int size = tiermap.getTierCount();

		tiers = new int[size +1];
		tiers[0] = 0;
		for (int i=0; i<size;i++){
			MapTierGlyph glyph = tiermap.getTierAt(i);
			if (glyph != null){  // not sure that i need to check, just in case.
				java.awt.Rectangle rect = glyph.getPixelBox();
				if ( glyph.getState() == MapTierGlyph.HIDDEN){
					tiers[i+1] = tiers[i];
				}
				else {
					tiers[i+1] = rect.y + rect.height;
				}
			}
			else {
				tiers[i] = 0;
			}
		}
	}

	// round to the nearest tier border

	protected int yValue(int y) {
		tier = 0;
		while ( (tiers.length-1) > tier && ((tiers[tier]+tiers[tier+1])/2) < y ) {
			tier++;
		}
		return tiers[tier];
	}

}
