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

import com.affymetrix.genometryImpl.util.DisplayUtils;
import com.affymetrix.genometryImpl.util.PreferenceUtils;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.tree.*;

public final class BookmarkListEditor {
  
  /** The name of the JFrame. */
  public static final String TITLE = "Bookmark Editor";

  private final JPanel central_component = new JPanel();

  private final JLabel type_label = new JLabel("Type:");
  private final JLabel type_label_2 = new JLabel("");

  private final JPanel main_box = new JPanel();
  
  private final JLabel name_label = new JLabel("Name:", JLabel.TRAILING);
  private final JTextField name = new JTextField(30);

  private final BookmarkTableComponent ucb_editor = new BookmarkTableComponent();
  
  private final JButton submit_button;
  private final JButton cancel_button;
  
  private final JFrame frame = new JFrame(TITLE);

  private BookmarkList the_bookmark_list = null;
  
  private DefaultTreeModel tree_model = null;

   /** Creates a new instance of BookmarkListEditor. */
  public BookmarkListEditor(DefaultTreeModel t) {
    this.tree_model = t;
    
    name_label.setLabelFor(name);

    Box type_box = new Box(BoxLayout.X_AXIS);
    type_box.add(type_label);
    type_box.add(Box.createHorizontalStrut(5));
    type_box.add(type_label_2);
    type_box.add(Box.createHorizontalGlue());

    Box line1 = new Box(BoxLayout.X_AXIS);
    line1.add(name_label);
    line1.add(Box.createHorizontalStrut(5));
    line1.add(name);
        
    // Setting the layout to BorderLayout allows the JTable in the JScrollPane to
    // re-size itself dynamically correctly.
    central_component.setLayout(new BorderLayout());

    Action cancel_action = new AbstractAction("Cancel") {
      public void actionPerformed(ActionEvent e) {
        frame.setVisible(false);
        saveWindowLocation();
      }
    };

    Action submit_action = new AbstractAction("Apply Changes") {
      public void actionPerformed(ActionEvent e) {
        boolean success = BookmarkListEditor.this.applyChanges();
        if (success) {
          frame.setVisible(false);
          saveWindowLocation();
        }
      }
    };
    cancel_button = new JButton(cancel_action);
    submit_button = new JButton(submit_action);
    
    Box line3 = new Box(BoxLayout.X_AXIS);
    line3.add(Box.createHorizontalGlue());
    line3.add(submit_button);
    line3.add(Box.createHorizontalStrut(5));
    line3.add(cancel_button);
    line3.add(Box.createHorizontalGlue());

    Box top_box = new Box(BoxLayout.Y_AXIS);
    top_box.add(type_box);
    top_box.add(line1);
    main_box.setLayout(new BorderLayout());
    main_box.add(top_box, BorderLayout.NORTH);
    main_box.add(central_component, BorderLayout.CENTER);
    main_box.add(line3, BorderLayout.SOUTH);

    setEnabled(false);
    frame.getContentPane().add(main_box);
    
    Rectangle pos = PreferenceUtils.retrieveWindowLocation(TITLE, new Rectangle(400, 400));
    if (pos != null) {
      PreferenceUtils.setWindowSize(frame, pos);
    }

   frame.addWindowListener( new WindowAdapter() {
			@Override
      public void windowClosing(WindowEvent evt) {
        saveWindowLocation();
      }
   });
 }
  
  // Writes the window location to the persistent preferences.
  private void saveWindowLocation() {
    PreferenceUtils.saveWindowLocation(frame, TITLE);
  }
  
  
  public void openDialog(BookmarkList bl) {
    this.setBookmarkList(bl);
    frame.doLayout();
    frame.repaint();
    
    DisplayUtils.bringFrameToFront(frame);
  }

  private void setBookmarkList(BookmarkList bookmark_list) {
    this.the_bookmark_list = bookmark_list;
    Object o = null;
    central_component.removeAll();
    if (bookmark_list != null) {o = bookmark_list.getUserObject();}
    if (o instanceof Bookmark) {
      Bookmark bm = (Bookmark) o;
      name.setText(bm.getName());
      central_component.add(ucb_editor.getComponent());
      
      if (bm.isUnibrowControl()) {
        type_label_2.setText("Internal Bookmark");
      } else {
        type_label_2.setText("External Bookmark");
      }
      
      this.setBookmark(bm);
      this.setEnabled(true);
    } else if (o instanceof String) {
      name.setText((String) o);
      type_label_2.setText("Bookmark List");
      this.setBookmark(null);
      this.setEnabled(true);
    } else if (o instanceof Separator) {
      name.setText("Separator");
      type_label_2.setText("");
      this.setBookmark(null);
      this.setEnabled(false);
    } else {
      name.setText("");
      type_label_2.setText("");
      this.setBookmark(null);
      this.setEnabled(false);
    }
  }

  /** Tries to reset the bookmark from the GUI.
   *  @return true for sucess, false otherwise.
   */
  private boolean applyChanges() {
    if (the_bookmark_list == null) {
      return false;
    }
    Object o = the_bookmark_list.getUserObject();
    if (o instanceof Bookmark) {
      Bookmark the_bm = (Bookmark) o;
      the_bm.setName(name.getText());
      boolean ok = ucb_editor.setBookmarkFromGUI(the_bm);
      if (ok) {
        //the_bookmark_list.setUserObject(the_bm);
        this.setBookmarkList(the_bookmark_list);
      } else {
        return false;
      }
    } else if (o instanceof String) {
      the_bookmark_list.setUserObject(name.getText());
    }
    if (tree_model != null) {
      tree_model.nodeChanged(the_bookmark_list);
    }
    return true;
  }

  private void setEnabled(boolean b) {
    submit_button.setEnabled(b);
    name.setEnabled(b);
  }

  private void setBookmark(Bookmark bm) {
    ucb_editor.setGUIFromBookmark(bm);
  }
  
  public void setIconImage(Image image) {
    if (image != null) {frame.setIconImage(image);}
  }
}
