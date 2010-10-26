package com.affymetrix.igb.view.external;

import java.awt.Image;
import java.awt.image.BufferedImage;

/**
 *
 * Simple wrapper around Image and a String error message
 */
public class ImageError {
	final BufferedImage image;
	final String error;

	public ImageError(BufferedImage image, String error){
		this.image = image;
		this.error = error;
	}
}
