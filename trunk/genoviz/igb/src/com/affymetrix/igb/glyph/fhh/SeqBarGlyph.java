/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.affymetrix.igb.glyph.fhh;

import com.affymetrix.genoviz.bioviews.ViewI;
import com.affymetrix.genoviz.glyph.FillRectGlyph;
import com.affymetrix.genoviz.glyph.SolidGlyph;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.Rectangle2D.Double;

/**
 *
 * @author Burim
 */
public class SeqBarGlyph extends SolidGlyph{


	/**
	 * Rotation Pitch for the text in radians.
	 */
	private double _rotPitch = 0.0f;

	private String text;

	private int _yOffset = -10;

	private int _pixelOffset = 0;

	@Override
	public void draw(ViewI view) {
		calcPixels(view);
//pixelbox.x += _pixelOffset;

		Graphics2D g = view.getGraphics();
		g.setColor(getBackgroundColor());

		// temp fix for AWT drawing bug when rect gets too big -- GAH 2/6/98
		Rectangle compbox = view.getComponentSizeRect();
		pixelbox = pixelbox.intersection(compbox);

		// If the coordbox was specified with negative width or height,
		// convert pixelbox to equivalent one with positive width and height.
		// Constrain abs(width) or abs(height) by min_pixels.
		// Here I'm relying on the fact that min_pixels is positive.
		if (coordbox.width < 0) {
			pixelbox.width = -Math.min(pixelbox.width, -min_pixels_width);
			pixelbox.x -= pixelbox.width;
		}
		else pixelbox.width = Math.max ( pixelbox.width, min_pixels_width );
		if (coordbox.height < 0) {
			pixelbox.height = -Math.min(pixelbox.height, -min_pixels_height);
			pixelbox.y -= pixelbox.height;
		}
		else pixelbox.height = Math.max ( pixelbox.height, min_pixels_height );

		// draw the box
		g.fillRect(pixelbox.x, pixelbox.y, pixelbox.width, pixelbox.height);

		//super.draw( view );



		if( getText() != null ) {

			Font savefont = g.getFont();
			Font f2 = this.getFont();
			if (f2 != savefont) {
				g.setFont(f2);
			} else {
				// If they are equal, there's no need to restore the font
				// down below.
				savefont = null;
			}
			FontMetrics fm = g.getFontMetrics();

			int text_width = fm.stringWidth(this.text);
			int text_height = fm.getHeight();


			double text_width_rot = Math.abs(Math.cos(_rotPitch)*text_width)+Math.abs(Math.cos(_rotPitch+Math.PI/2.0)*text_height);
			double text_height_rot = Math.abs(Math.sin(_rotPitch)*text_width)+Math.abs(Math.sin(_rotPitch+Math.PI/2.0)*text_height);

			if(text_width_rot <= pixelbox.width ) {
				g.setColor(this.getForegroundColor());
				//int text_pos_x = (pixelbox.x + (int)(pixelbox.width/2.0)) - ((int)(text_width_rot/2.0) + (int)Math.cos(_rotPitch+Math.PI/2.0)*text_height);
				//int text_pos_x = (pixelbox.x + (int)(pixelbox.width/2.0));
				int text_pos_x = (pixelbox.x + ((int)text_width_rot)) - 1;
				int text_pos_y = (pixelbox.y + pixelbox.height )+_yOffset;

				g.translate(text_pos_x, text_pos_y);
				g.rotate( getRotPitch() );

				g.drawString(this.getText(), 0, 0 );

				g.rotate(-getRotPitch());
				g.translate(-text_pos_x, -text_pos_y);
			}
			if (null != savefont) {
				g.setFont(savefont);
			}
		}
	}

	

	@Override
	public boolean withinView(ViewI view)
	{
		Rectangle2D.Double rect = new Rectangle2D.Double(
				getPositiveCoordBox().x + getCoordOffset(view),
				getPositiveCoordBox().y,
				getPositiveCoordBox().getWidth(),
				getPositiveCoordBox().getHeight());


		if( rect.height == 0.0 ){
			return view.getCoordBox().intersectsLine(
					new Line2D.Double(rect.x, 0.0, rect.x + rect.width, 0.0) );
		}
		if( rect.width == 0.0 ){
			return view.getCoordBox().intersectsLine(
					new Line2D.Double(0.0, rect.y, 0.0, rect.y + rect.height) );
		}

		return rect.intersects(view.getCoordBox());
	}



	/*
	@Override
	public boolean withinView(ViewI view)
	{
		Rectangle rect = getPixelBox();


		if( rect.height == 0.0 ){
			return view.getPixelBox().intersectsLine(
					new Line2D.Double(rect.x, 0.0, rect.x + rect.width, 0.0) );
		}
		if( rect.width == 0.0 ){
			return view.getPixelBox().intersectsLine(
					new Line2D.Double(0.0, rect.y, 0.0, rect.y + rect.height) );
		}

		return rect.intersects(view.getPixelBox());
	}
	 */

	@Override
	public boolean hit(Rectangle pixel_hitbox, ViewI view) {
		return super.hit(pixel_hitbox, view);
	}



	@Override
	public boolean hit(Double coord_hitbox, ViewI view) {
		Rectangle2D.Double rect = new Rectangle2D.Double(
				getPositiveCoordBox().x + getCoordOffset(view),
				getPositiveCoordBox().y,
				getPositiveCoordBox().getWidth(),
				getPositiveCoordBox().getHeight());

		return isHitable() && isVisible() && coord_hitbox.intersects(rect);
		/*Rectangle pixelb = new Rectangle();
		view.transformToPixels(coord_hitbox, pixelb);
		//pixelb.x += _pixelOffset;
		//return super.hit(coord_hitbox, view);
		return isHitable() && isVisible() && pixelb.intersects(getPixelBox());*/
	}

	@Override
	public boolean intersects(Double rect, ViewI view) {
		/*Rectangle pixelb = new Rectangle();
		view.transformToPixels(rect, pixelb);
		return isVisible && pixelb.intersects(getPixelBox());*/
		Rectangle2D.Double posCordB = getPositiveCoordBox();
		boolean var = isVisible && rect.intersects(
				new Rectangle2D.Double(
					posCordB.x + getCoordOffset(view),
					posCordB.y,
					posCordB.getWidth(),
					posCordB.getHeight()
				)
		);
		return var;
	}

	@Override
	public boolean intersects(Rectangle rect) {
		return super.intersects(rect);
	}


	@Override
	public void calcPixels(ViewI view) {
		pixelbox = view.transformToPixels (coordbox, pixelbox);
		pixelbox.x += _pixelOffset;
	}

	@Override
	public Rectangle getPixelBox(ViewI view) {
		pixelbox = view.transformToPixels (coordbox, pixelbox);
		pixelbox.x += _pixelOffset;
		return pixelbox;
	}




	/**
	 * @return the _rotPitch
	 */
	public double getRotPitch() {
		return _rotPitch;
	}

	/**
	 * @param rotPitch the _rotPitch to set
	 */
	 public void setRotPitch(double rotPitch) {
		this._rotPitch = rotPitch;
	}


	public void setText(String str) {
		this.text = str;
	}
	public String getText() {
		return this.text;
	}

	/**
	 * @return the _pixelOffset
	 */
	public int getPixelOffset() {
		return _pixelOffset;
	}

	/**
	 * @return the _pixelOffset
	 */
	public double getCoordOffset(ViewI view) {
		//Point2D.Double coordOffset = new Point2D.Double();
		double scaleX = view.getTransform().getScaleX();
		return _pixelOffset / scaleX;
		//view.transformToCoords(new Point2D.Double(_pixelOffset, 0.0), coordOffset);
		//return coordOffset.x;
	}

	/**
	 * @param pixelOffset the _pixelOffset to set
	 */
	public void setPixelOffset(int pixelOffset) {
		this._pixelOffset = pixelOffset;
		//this.coordbox.x += this._pixelOffset;
	}

}
