/**
*   Copyright (c) 2001-2004 Affymetrix, Inc.
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

package com.affymetrix.igb.bookmarks;

import java.awt.*;
import javax.swing.*;

/** An Icon to represent a bookmark.  There is one visual form
 *  for a UnibrowControl bookmark that opens in Unibrow and another
 *  form for one that opens in an external browser.
 *  (This is currently just a test class.  Not ready for production use.)
 */
final class BookmarkIcon implements Icon {
    private final int width;
    private final int height;

    private final int[] xPoints;
    private final int[] yPoints;

    private BookmarkIcon(int width, int height, int[] xPoints, int[] yPoints) {
      if (xPoints.length != yPoints.length || xPoints.length==0) {
        throw new IllegalArgumentException();
      }
      this.width = width;
      this.height = height;
      this.xPoints = xPoints;
      this.yPoints = yPoints;
    }

    static final BookmarkIcon UNIBROW_CONTROL_ICON = getRectangleIcon(6,6);
    static final BookmarkIcon EXTERNAL_ICON = getDiamondIcon(6,6);
    
    private static BookmarkIcon getRectangleIcon(int width, int height) {
      int[] x = {0, width, width, 0};
      int[] y = {0, 0, height, height};
      return new BookmarkIcon(width, height, x, y);
    }
    
    private static BookmarkIcon getDiamondIcon(int width, int height) {
      int[] x = {0, width/2, width, width/2};
      int[] y = {height/2, height, height/2, 0};
      return new BookmarkIcon(width, height, x, y);
    }
    
    public int getIconHeight() {
        return height;
    }

    public int getIconWidth() {
        return width;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
        if (c.isEnabled()) {
            g.setColor(c.getForeground());
        } else {
            g.setColor(Color.gray);
        }

        g.translate(x, y);
        g.fillPolygon(xPoints, yPoints, xPoints.length);
        g.translate(-x, -y);   //Restore graphics object
    }
}
