/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.glyph;

import java.awt.Image;
import java.util.HashMap;
import java.util.TreeMap;

/**
 *
 * @author pool336pc02
 */
public class TextureCache {

	private TreeMap<Integer, HashMap< Integer, Image>> cache;

	private static TextureCache instance;

	private TextureCache(){
		cache = new TreeMap<Integer, HashMap<Integer, Image>>();
	}

	public static TextureCache getInstance(){
		if(instance == null)
			instance = new TextureCache();
		return instance;
	}

	public Image getImage(int width, int height){
		if(cache.containsKey(height)){
			if(cache.containsKey(width)){
				return cache.get(height).get(width);
			}else{
				Image i = GenericAnnotGlyphFactory.getTexture().getScaledInstance(width, height, Image.SCALE_FAST);
				cache.get(height).put(width, i);
				return i;
			}
		}else{
			Image i = GenericAnnotGlyphFactory.getTexture().getScaledInstance(width, height, Image.SCALE_FAST);
			HashMap<Integer, Image> hm = new HashMap<Integer, Image>();
			hm.put(width, i);
			cache.put(height, hm);
			return i;
		}
	}

}
