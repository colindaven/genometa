package com.affymetrix.genoviz.swing;

import java.awt.*;
import javax.swing.Icon;

/**
 *  A simple square icon for showing a block of color.
 */
public final class ColorIcon implements Icon {

  private Color the_color = Color.BLACK;
  private int the_size = 11;

  /** A default Black ColorIcon. */
  public ColorIcon() {
  }
  
  public ColorIcon(int size, Color c) {
    this();
    setSize(size);
    setColor(c);
  }

  public void setColor(Color c) {
    the_color = c;
  }
  
  public Color getColor() {
    return the_color;
  }
  
  public void setSize(int size) {
    if (the_size < 5) {
      the_size = 5;
    }
    else {
      the_size = size;
    }
  }

  public int getIconWidth() {
    return the_size;
  }

  public int getIconHeight() {
    return the_size;
  }

  public void paintIcon(Component c, Graphics g, int x, int y) {
    g.setColor(c.getForeground());
    g.fillRect(x, y, getIconWidth(), getIconHeight());
    
    // if the_color is null, draw an "X" in the square
    if (the_color == null) {
      g.setColor(c.getBackground());
      g.fillRect(x+1, y+1, getIconWidth()-2, getIconHeight()-2);
      g.setColor(c.getForeground());
      g.drawLine(x, y, x+getIconWidth()-1, y+getIconHeight()-1);
      g.drawLine(x+getIconWidth()-1, y, x, y+getIconHeight()-1);
    }
    // Otherwise, fill the square with color
    else {
      g.setColor(the_color);
      g.fillRect(x+1, y+1, getIconWidth()-2, getIconHeight()-2);
    }
  }
}
