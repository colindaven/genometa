package com.affymetrix.genoviz.glyph;

import com.affymetrix.genoviz.bioviews.ViewI;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * A glyph that draws a solid bar with a triangle at the top or bottom.
 * This might be useful for showing transposon insertions, for example.
 */
public class TriBarGlyph extends DirectedGlyph {

	private int x[];
	private int y[];
	private Color fillColor = Color.gray;
	private final Rectangle bar = new Rectangle();


	/**
	 * constructs a tribar glyph
	 * pointing toward the axis.
	 * The glyph will point away from the axis if end &lt; start.
	 * Initially,
	 * the triangle is filled with gray and outlined in black,
	 * and the bar is black.
	 */
	public TriBarGlyph() {
		this.x = new int[4];
		this.y = new int[4];
	}


	@Override
	public void draw( ViewI view ) {
		calcPixels( view );
		Graphics g = view.getGraphics();
		g.setColor( getBackgroundColor() );
		calcBar();
		g.fillRect( this.bar.x, this.bar.y, this.bar.width, this.bar.height );
		g.setColor( getFillColor() );
		g.fillPolygon( this.x, this.y, 4 );
		g.setColor( getBackgroundColor() );
		g.drawPolygon( this.x, this.y, 4 );
		super.draw( view );
	}


	/**
	 * Get the value of fillColor.
	 * @return Value of fillColor.
	 */
	public final Color getFillColor() {return fillColor;}

	/**
	 * Set the value of fillColor.
	 * This color will be used to fill the triangle.
	 * @param v  Value to assign to fillColor.
	 */
	public void setFillColor(Color  v) {this.fillColor = v;}


	@Override
	public void calcPixels(ViewI view) {
		view.transformToPixels(coordbox, pixelbox);
		int xtricenter;
		int ytricenter;

		switch ( this.getDirection() ) {
			case EAST:
				xtricenter = pixelbox.x + pixelbox.width/2;
				ytricenter = pixelbox.y + 5;
				x[0] = xtricenter - 5;
				y[0] = ytricenter - 5;
				x[1] = xtricenter;
				y[1] = ytricenter + 5;
				x[2] = xtricenter + 5;
				y[2] = ytricenter - 5;
				x[3] = x[0];
				y[3] = y[0];
				break;
			case WEST:
				xtricenter = pixelbox.x + pixelbox.width/2;
				ytricenter = pixelbox.y + pixelbox.height - 5;
				x[0] = xtricenter - 5;
				y[0] = ytricenter + 5;
				x[1] = xtricenter;
				y[1] = ytricenter - 5;
				x[2] = xtricenter + 5;
				y[2] = ytricenter + 5;
				x[3] = x[0];
				y[3] = y[0];
				break;
			case SOUTH:
				ytricenter = pixelbox.y + pixelbox.height/2;
				xtricenter = pixelbox.x + pixelbox.width - 5;
				x[0] = xtricenter + 5;
				y[0] = ytricenter - 5;
				x[1] = xtricenter - 5;
				y[1] = ytricenter;
				x[2] = xtricenter + 5;
				y[2] = ytricenter + 5;
				x[3] = x[0];
				y[3] = y[0];
				break;
			case NORTH:
				ytricenter = pixelbox.y + pixelbox.height/2;
				xtricenter = pixelbox.x + 5;
				x[0] = xtricenter - 5;
				y[0] = ytricenter - 5;
				x[1] = xtricenter + 5;
				y[1] = ytricenter;
				x[2] = xtricenter - 5;
				y[2] = ytricenter + 5;
				x[3] = x[0];
				y[3] = y[0];
				break;
		}
	}

	private void calcBar() {
		switch ( this.getDirection() ) {
			case EAST:
			case WEST:
				this.bar.x = pixelbox.x+pixelbox.width/2-1;
				this.bar.y = pixelbox.y;
				this.bar.width = 3;
				this.bar.height = pixelbox.height;
				break;
			case SOUTH:
			case NORTH:
				this.bar.x = pixelbox.x;
				this.bar.y = pixelbox.y+pixelbox.height/2-1;
				this.bar.width = pixelbox.width;
				this.bar.height = 3;
				break;
			default:
				throw new IllegalStateException
					( "TriBarGlyph has an unknown direction " + this.getDirection() );
		}
	}

}
