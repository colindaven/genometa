package com.affymetrix.igb.bookmarks;

import com.affymetrix.genoviz.util.ErrorHandler;
import java.awt.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.table.TableCellEditor;

/**
 * A JTable-based GUI editor for a Bookmark (or any URL).
 * This just has some fields for seeing and setting various parameters,
 * but does not have a "submit" button.  You need to supply the
 * "submit" button, etc.
 * Call {@link #setGUIFromBookmark(Bookmark)} to update the display.
 * Call {@link #setBookmarkFromGUI(Bookmark)} when a "submit" button is pressed.
 * @author  ed
 */
public final class BookmarkTableComponent {
  private static final boolean DEBUG = true;

  private final JPanel main_box = new JPanel();
  
  private final BookmarkTableModel data_model;
  private final JTable table;
  private JTextField url_field = new JTextField(30);
  private final JLabel url_label = new JLabel("URL:");
  
  public BookmarkTableComponent() {
    main_box.setLayout(new BorderLayout());

    data_model = new BookmarkTableModel();
    table = new JTable(data_model);

    JScrollPane scrollpane = new JScrollPane(table);

    scrollpane.setMinimumSize(new java.awt.Dimension(700, 300));
    
    Box url_box = new Box(BoxLayout.X_AXIS);
    url_box.add(url_label);
    url_box.add(Box.createHorizontalStrut(5));
    url_box.add(url_field);
    url_box.add(Box.createHorizontalGlue());
    url_label.setLabelFor(url_field);

    main_box.add(url_box, BorderLayout.NORTH);
    main_box.add(scrollpane, BorderLayout.CENTER);
  }
  
  public Component getComponent() {
    return main_box;
  }
    
  public void stopEditing() {
    TableCellEditor ed = table.getCellEditor();
    if (ed != null) {
      ed.stopCellEditing();
    }
  }

  public void cancelEditing() {
    TableCellEditor ed = table.getCellEditor();
    if (ed != null) {
      ed.cancelCellEditing();
    }
  }
  
  /** Sets data in the given bookmark from the data in the GUI.
   *  @return true if sucessful, false if there was an error.
   */
  public boolean setBookmarkFromGUI(Bookmark the_bm) {
    boolean ok = true;
    if (DEBUG) {
		System.out.println("Before: "+the_bm.getURL().toExternalForm());
	}
    
    stopEditing();

    String str = Bookmark.constructURL(url_field.getText().trim(), data_model.getValuesAsMap());
    try {
      URL url = new URL(str);
      the_bm.setURL(url);
    } catch (MalformedURLException e) {
      JFrame frame = (JFrame) SwingUtilities.getAncestorOfClass(JFrame.class, this.main_box);
      ErrorHandler.errorPanel(frame, "Error", "Cannot construct bookmark: " + e.getMessage(), null);
      ok = false;
    }
    if (DEBUG) {
		System.out.println("After: "+the_bm.getURL().toExternalForm());
	}
    
    return ok;
  }

  public void setGUIFromBookmark(Bookmark bm) {
    cancelEditing(); // just in case!
    if (bm == null) {
      url_field.setText("");
      data_model.setValuesFromMap(Collections.<String,String[]>emptyMap());
    } else {
      URL url = bm.getURL();
      String url_base = bm.getURL().toExternalForm();
      int index = url_base.indexOf('?');
      if (index > 0) {
        url_base = url_base.substring(0, index);
      }
      url_field.setText(url_base);

      data_model.setValuesFromMap(Bookmark.parseParameters(url));
    }
  }  
}
 
