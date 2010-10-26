/**
*   Copyright (c) 2001-2007 Affymetrix, Inc.
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

package com.affymetrix.genometryImpl.util;

import java.awt.MediaTracker;
import javax.swing.*;
import java.awt.event.ActionListener;

import com.affymetrix.genometryImpl.util.PreferenceUtils;

public abstract class MenuUtil {

  private static JMenuBar main_menu_bar = new JMenuBar();
    
  /** Sets the accelerator for the given JMenuItem based on
   *  the preference associated with the action command.
   *  The action command Strings should be unique across the whole application.
   */
  private static final void addAccelerator(JMenuItem item, String command) {
    item.setAccelerator(PreferenceUtils.getAccelerator(command));
  }
  
  /** Sets up an association such that the accelerator given in
   *  the user prefs for the action_command will cause the given
   *  action_command to be sent to the given ActionListener
   *  when that accelerator key is pressed and the given component
   *  is in the window that has keyboard focus.
   *  If there was no user preference given for the action command,
   *  this routine does nothing.
   *  @return a KeyStroke, from {@link PreferenceUtils#getAccelerator(String)}
   *  is returned as a convenience
   *  @see PreferenceUtils#getAccelerator(String)
   */
  public static final KeyStroke addAccelerator(JComponent comp, ActionListener al,
    String action_command) {
    KeyStroke ks = PreferenceUtils.getAccelerator(action_command);
    if (ks != null) {
      comp.registerKeyboardAction(al, action_command, ks,
       JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
    return ks;
  }

  public static final JMenuBar getMainMenuBar() {
    return main_menu_bar;
  }
  
  public static final JMenu getMenu(String name) {
    int num_menus = main_menu_bar.getMenuCount();
    for (int i=0; i<num_menus; i++) {
      JMenu menu_i = main_menu_bar.getMenu(i);
      if (name.equals(menu_i.getText())) {
        menu_i.getName();
        return menu_i;
      }
    }
    JMenu new_menu = new JMenu(name);
    new_menu.setName(name); // JMenu.getName() and JMenu.getText() aren't automatically equal
    
    // Add the new menu, but keep the "Help" menu in last place
    if (num_menus > 0 && "Help".equals(main_menu_bar.getMenu(num_menus-1).getName())) {
      main_menu_bar.add(new_menu, num_menus-1);
    } else {
      main_menu_bar.add(new_menu);
    }
    return new_menu;
  }
  
  /**
   *  Calls {@link #addToMenu(JMenu, JMenuItem)}
   *  with command set to null.
   */
  public static final JMenuItem addToMenu(JMenu menu, JMenuItem item) {
    String command = item.getText();
    if (command != null) { addAccelerator(item, command); }
    return menu.add(item);
  }

  /**
   *  Loads an ImageIcon from the specified system resource.
   *  The system resource should be in the classpath, for example,
   *  it could be in the jlfgr-1_0.jar file.  If the resource is
   *  absent or can't be found, this routine will not throw an exception,
   *  but will return null.
   *  For example: "toolbarButtonGraphics/general/About16.gif".
   *  @return An ImageIcon or null if the one specified could not be found.
   */
  public static ImageIcon getIcon(String resource_name) {
    ImageIcon icon = null;
    try {
      // Note: MenuUtil.class.getResource(resource_name) does not work;
      // ClassLoader.getSystemResource(resource_name) works locally, but not with WebStart;
      //
      // Both of these work locally and with WebStart:
      //  MenuUtil.class.getClassLoader().getResource(resource_name)
      //  Thread.currentThread().getContextClassLoader().getResource(resource_name)
      java.net.URL url = Thread.currentThread().getContextClassLoader().getResource(resource_name);      
      if (url != null) {
        icon = new ImageIcon(url);
      }
    } catch (Exception e) {
      // It isn't a big deal if we can't find the icon, just return null
    }
    if (icon == null || icon.getImageLoadStatus() == MediaTracker.ABORTED ||
        icon.getIconHeight() <= 0 || icon.getIconWidth() <= 0) {
      icon = null;
    }
    
    return icon;    
  }
}
