/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.glyph;

import java.awt.Image;
import java.util.HashMap;

/**
 *
 * @author Elmo
 */
public class TextureCache {
	private HashMap<Integer,HashMap<Integer, Image>> cache;
	private static TextureCache instance;

	private TextureCache() {
		cache = new HashMap<Integer, HashMap<Integer, Image>>();
	}

	public static TextureCache getInstance(){
		if(instance == null)
			instance = new TextureCache();
		return instance;
	}

	public Image getImage(int width, int height){
		//Try finding an image in the Cache
		if(cache.containsKey(height)){
			if(cache.get(height).containsKey(width)){
				return cache.get(height).get(width);
			} else {
				Image i = GenericAnnotGlyphFactory.getTexture().getScaledInstance(width, height, Image.SCALE_FAST);
				cache.get(height).put(width, i);
				return i;
			}
		}else{
			HashMap<Integer, Image> hm =  new HashMap<Integer, Image>();
			Image i = GenericAnnotGlyphFactory.getTexture().getScaledInstance(width, height, Image.SCALE_FAST);
			hm.put(width, i);
			cache.put(height,hm);
			return i;
		}
	}


}
