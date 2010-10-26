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

package com.affymetrix.igb.prefs;

import com.affymetrix.genoviz.util.ErrorHandler;
import java.awt.event.*;
import java.util.prefs.Preferences;
import javax.swing.*;
import javax.swing.border.*;

public final class KeyStrokeEditPanel extends JPanel {
  private static final boolean DEBUG = false;
  
  private final JTextField key_field = new JTextField(20);
  private final JLabel key_label = new JLabel("Type a shortcut: " );
  private final JButton apply_button = new JButton("Apply this shortcut");
  private final JButton clear_button = new JButton("Clear this shortcut");
  
  private int key_code;
  private int modifiers;

	private Preferences the_node = null;
  private String the_key = null;
  
  /** Creates a new instance of KeyStrokesView */
  public KeyStrokeEditPanel() {
    this.setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    this.setBorder(new EmptyBorder(new java.awt.Insets(8, 8, 8, 8)));
        
    Box box1 = new Box(BoxLayout.Y_AXIS);
    box1.add(key_label);
    box1.add(key_field);    
    
    Box buttons = new Box(BoxLayout.X_AXIS);
    buttons.add(Box.createHorizontalGlue());
    buttons.add(apply_button);
    buttons.add(Box.createHorizontalStrut(5));
    buttons.add(clear_button);
    buttons.add(Box.createHorizontalGlue());
    
    this.add(Box.createVerticalGlue());
    this.add(box1);
    this.add(Box.createVerticalStrut(5));
    this.add(buttons);
    this.add(Box.createVerticalGlue());
    
    
    key_field.addKeyListener(new KeyListener() {
      public void keyPressed(KeyEvent evt) {
        evt.consume();
        key_code = evt.getKeyCode();
        modifiers = evt.getModifiers();
        KeyStroke ks = getStroke();
        key_field.setText(keyStroke2String(ks));
      }
      public void keyReleased(KeyEvent evt) {
        evt.consume();
      }
      public void keyTyped(KeyEvent evt) {
        evt.consume();
      }
    }
    );
    
    apply_button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        applyAction();
      }
    }
    );
    
    clear_button.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent ae) {
        clearAction();
      }
    }
    );
    
    setEnabled(false);
  }
  

  
  void setPreferenceKey(Preferences node, String key, String def_value) {
    this.the_node = node;
    this.the_key = key;
    if (this.the_node == null || this.the_key == null) {
      key_label.setText("Make a selection");
      key_field.setText("");
      setEnabled(false);
    } else {
      key_label.setText("Type a shortcut for \""+ this.the_key+ "\":" );
      String value = this.the_node.get(key, def_value);
      key_field.setText(value);
      setEnabled(true);
    }
    key_field.getToolTipText();
  }
  
	@Override
  public void setEnabled(boolean b) {
    apply_button.setEnabled(b);
    clear_button.setEnabled(b);
    key_field.setEnabled(b);
  }
  
  private void applyAction() {
    if (the_node == null || the_key == null) {
      // shouldn't happen
      ErrorHandler.errorPanel("ERROR", "No shortcut command selected");
      setEnabled(false);
      return;
    }
    String str = key_field.getText().trim();
    KeyStroke ks = KeyStroke.getKeyStroke(str);
    if (str.length() == 0) {
      this.the_node.put(this.the_key, "");
    }
    else if (ks == null) {
      ErrorHandler.errorPanel("Unknown Key", "Unknown key code: \""+ str +"\"");
      key_field.setText("");
    }
    else if (isModifierKey(ks) || (str.indexOf("unknown") >= 0)) {
      ErrorHandler.errorPanel("Bad Keystroke", "Illegal shortcut: \""+ str +"\"");
      key_field.setText("");
    } else if (str.indexOf(' ') <= 0 || str.startsWith("shift ")) {
      // Checking that the string contains a space and does NOT start with "shift "
      // is equivalent to checking that there is some other modifier key present.
      // (such as "ctrl " or "alt ").
      // I hate making this restriction, but without it, bad things can happen.
      // For instance, if the user wants to use "Z" too mean "zoom" in the SeqMapView,
      // then any time the letter "Z" is pressed in any input box, a zoom will happen,
      // and the user won't be able to type things like "zebra genome".
      ErrorHandler.errorPanel("Bad Keystroke", "Illegal shortcut: \""+ str +"\"\n" +
        "Must contain a modifier key (ctrl, alt, ...)");
      key_field.setText("");
    } else {
      if (DEBUG) System.out.println("Changing pref: "+this.the_node
        +": "+ this.the_key + "  -->  " + str);
      this.the_node.put(this.the_key, str);      
    }
  }
  
  private void clearAction() {
    if (the_node == null || the_key == null) {
      // shouldn't happen
      ErrorHandler.errorPanel("ERROR", "No shortcut command selected");
      setEnabled(false);
      return;
    }
    key_field.setText("");
    this.the_node.put(this.the_key, "");
  }
  
  /** Returns true if the primary key code is control, or alt, etc. */
  private static boolean isModifierKey(KeyStroke key) {
    int key_code = key.getKeyCode();
    return (
      (key_code == KeyEvent.VK_ALT) || 
      (key_code == KeyEvent.VK_ALT_GRAPH) || 
      (key_code == KeyEvent.BUTTON1_DOWN_MASK) ||
      (key_code == KeyEvent.BUTTON2_DOWN_MASK) ||
      (key_code == KeyEvent.BUTTON3_DOWN_MASK) ||
      (key_code == KeyEvent.VK_CONTROL) || 
      (key_code == KeyEvent.VK_META) || 
      (key_code == KeyEvent.VK_SHIFT)
    );
  }
      
  private KeyStroke getStroke() {
    return KeyStroke.getKeyStroke(key_code, modifiers);
  }
  
  /**
   *  Convert a KeyStroke to a String in the same format used
   *  by KeyStroke.getKeyStroke(String).
   *  Modified, originally from the Java Developer's Almanac 1.4.
   *  http://javaalmanac.com/egs/javax.swing/Key2Str.html
   */
  public static String keyStroke2String(KeyStroke key) {
    StringBuffer s = new StringBuffer(50);
    int m = key.getModifiers();
    
    if ((m & (InputEvent.CTRL_DOWN_MASK)) != 0) {
      s.append("ctrl ");
    }
    if ((m & (InputEvent.META_DOWN_MASK)) != 0) {
      s.append("meta ");
    }
    if ((m & (InputEvent.ALT_DOWN_MASK)) != 0) {
      s.append("alt ");
    }
    if ((m & (InputEvent.BUTTON1_DOWN_MASK)) != 0) {
      s.append("button1 ");
    }
    if ((m & (InputEvent.BUTTON2_DOWN_MASK)) != 0) {
      s.append("button2 ");
    }
    if ((m & (InputEvent.BUTTON3_DOWN_MASK)) != 0) {
      s.append("button3 ");
    }
    if ((m & (InputEvent.SHIFT_DOWN_MASK)) != 0) {
      // It is important that the shift key be appended after the others, so that
      // above I can easily check that there is at least one modifier other
      // than "shift" in the keystroke.
      s.append("shift ");
    }
    
    s.append(getKeyText(key.getKeyCode()));
    
    return s.toString();
  }
  
  /**
   *  From the Java Developer's Almanac 1.4.
   *  http://javaalmanac.com/egs/javax.swing/Key2Str.html
   */
  private static String getKeyText(int keyCode) {
    if (keyCode >= KeyEvent.VK_0 && keyCode <= KeyEvent.VK_9 ||
    keyCode >= KeyEvent.VK_A && keyCode <= KeyEvent.VK_Z) {
      return String.valueOf((char)keyCode);
    }
    
    switch(keyCode) {
      case KeyEvent.VK_COMMA: return "COMMA";
      case KeyEvent.VK_PERIOD: return "PERIOD";
      case KeyEvent.VK_SLASH: return "SLASH";
      case KeyEvent.VK_SEMICOLON: return "SEMICOLON";
      case KeyEvent.VK_EQUALS: return "EQUALS";
      case KeyEvent.VK_OPEN_BRACKET: return "OPEN_BRACKET";
      case KeyEvent.VK_BACK_SLASH: return "BACK_SLASH";
      case KeyEvent.VK_CLOSE_BRACKET: return "CLOSE_BRACKET";
      
      case KeyEvent.VK_ENTER: return "ENTER";
      case KeyEvent.VK_BACK_SPACE: return "BACK_SPACE";
      case KeyEvent.VK_TAB: return "TAB";
      case KeyEvent.VK_CANCEL: return "CANCEL";
      case KeyEvent.VK_CLEAR: return "CLEAR";
      case KeyEvent.VK_SHIFT: return "SHIFT";
      case KeyEvent.VK_CONTROL: return "CONTROL";
      case KeyEvent.VK_ALT: return "ALT";
      case KeyEvent.VK_PAUSE: return "PAUSE";
      case KeyEvent.VK_CAPS_LOCK: return "CAPS_LOCK";
      case KeyEvent.VK_ESCAPE: return "ESCAPE";
      case KeyEvent.VK_SPACE: return "SPACE";
      case KeyEvent.VK_PAGE_UP: return "PAGE_UP";
      case KeyEvent.VK_PAGE_DOWN: return "PAGE_DOWN";
      case KeyEvent.VK_END: return "END";
      case KeyEvent.VK_HOME: return "HOME";
      case KeyEvent.VK_LEFT: return "LEFT";
      case KeyEvent.VK_UP: return "UP";
      case KeyEvent.VK_RIGHT: return "RIGHT";
      case KeyEvent.VK_DOWN: return "DOWN";
      
      // numpad numeric keys handled below
      case KeyEvent.VK_MULTIPLY: return "MULTIPLY";
      case KeyEvent.VK_ADD: return "ADD";
      case KeyEvent.VK_SEPARATOR: return "SEPARATOR";
      case KeyEvent.VK_SUBTRACT: return "SUBTRACT";
      case KeyEvent.VK_DECIMAL: return "DECIMAL";
      case KeyEvent.VK_DIVIDE: return "DIVIDE";
      case KeyEvent.VK_DELETE: return "DELETE";
      case KeyEvent.VK_NUM_LOCK: return "NUM_LOCK";
      case KeyEvent.VK_SCROLL_LOCK: return "SCROLL_LOCK";
      
      case KeyEvent.VK_F1: return "F1";
      case KeyEvent.VK_F2: return "F2";
      case KeyEvent.VK_F3: return "F3";
      case KeyEvent.VK_F4: return "F4";
      case KeyEvent.VK_F5: return "F5";
      case KeyEvent.VK_F6: return "F6";
      case KeyEvent.VK_F7: return "F7";
      case KeyEvent.VK_F8: return "F8";
      case KeyEvent.VK_F9: return "F9";
      case KeyEvent.VK_F10: return "F10";
      case KeyEvent.VK_F11: return "F11";
      case KeyEvent.VK_F12: return "F12";
      case KeyEvent.VK_F13: return "F13";
      case KeyEvent.VK_F14: return "F14";
      case KeyEvent.VK_F15: return "F15";
      case KeyEvent.VK_F16: return "F16";
      case KeyEvent.VK_F17: return "F17";
      case KeyEvent.VK_F18: return "F18";
      case KeyEvent.VK_F19: return "F19";
      case KeyEvent.VK_F20: return "F20";
      case KeyEvent.VK_F21: return "F21";
      case KeyEvent.VK_F22: return "F22";
      case KeyEvent.VK_F23: return "F23";
      case KeyEvent.VK_F24: return "F24";
      
      case KeyEvent.VK_PRINTSCREEN: return "PRINTSCREEN";
      case KeyEvent.VK_INSERT: return "INSERT";
      case KeyEvent.VK_HELP: return "HELP";
      case KeyEvent.VK_META: return "META";
      case KeyEvent.VK_BACK_QUOTE: return "BACK_QUOTE";
      case KeyEvent.VK_QUOTE: return "QUOTE";
      
      case KeyEvent.VK_KP_UP: return "KP_UP";
      case KeyEvent.VK_KP_DOWN: return "KP_DOWN";
      case KeyEvent.VK_KP_LEFT: return "KP_LEFT";
      case KeyEvent.VK_KP_RIGHT: return "KP_RIGHT";
      
      case KeyEvent.VK_DEAD_GRAVE: return "DEAD_GRAVE";
      case KeyEvent.VK_DEAD_ACUTE: return "DEAD_ACUTE";
      case KeyEvent.VK_DEAD_CIRCUMFLEX: return "DEAD_CIRCUMFLEX";
      case KeyEvent.VK_DEAD_TILDE: return "DEAD_TILDE";
      case KeyEvent.VK_DEAD_MACRON: return "DEAD_MACRON";
      case KeyEvent.VK_DEAD_BREVE: return "DEAD_BREVE";
      case KeyEvent.VK_DEAD_ABOVEDOT: return "DEAD_ABOVEDOT";
      case KeyEvent.VK_DEAD_DIAERESIS: return "DEAD_DIAERESIS";
      case KeyEvent.VK_DEAD_ABOVERING: return "DEAD_ABOVERING";
      case KeyEvent.VK_DEAD_DOUBLEACUTE: return "DEAD_DOUBLEACUTE";
      case KeyEvent.VK_DEAD_CARON: return "DEAD_CARON";
      case KeyEvent.VK_DEAD_CEDILLA: return "DEAD_CEDILLA";
      case KeyEvent.VK_DEAD_OGONEK: return "DEAD_OGONEK";
      case KeyEvent.VK_DEAD_IOTA: return "DEAD_IOTA";
      case KeyEvent.VK_DEAD_VOICED_SOUND: return "DEAD_VOICED_SOUND";
      case KeyEvent.VK_DEAD_SEMIVOICED_SOUND: return "DEAD_SEMIVOICED_SOUND";
      
      case KeyEvent.VK_AMPERSAND: return "AMPERSAND";
      case KeyEvent.VK_ASTERISK: return "ASTERISK";
      case KeyEvent.VK_QUOTEDBL: return "QUOTEDBL";
      case KeyEvent.VK_LESS: return "LESS";
      case KeyEvent.VK_GREATER: return "GREATER";
      case KeyEvent.VK_BRACELEFT: return "BRACELEFT";
      case KeyEvent.VK_BRACERIGHT: return "BRACERIGHT";
      case KeyEvent.VK_AT: return "AT";
      case KeyEvent.VK_COLON: return "COLON";
      case KeyEvent.VK_CIRCUMFLEX: return "CIRCUMFLEX";
      case KeyEvent.VK_DOLLAR: return "DOLLAR";
      case KeyEvent.VK_EURO_SIGN: return "EURO_SIGN";
      case KeyEvent.VK_EXCLAMATION_MARK: return "EXCLAMATION_MARK";
      case KeyEvent.VK_INVERTED_EXCLAMATION_MARK:
        return "INVERTED_EXCLAMATION_MARK";
      case KeyEvent.VK_LEFT_PARENTHESIS: return "LEFT_PARENTHESIS";
      case KeyEvent.VK_NUMBER_SIGN: return "NUMBER_SIGN";
      case KeyEvent.VK_MINUS: return "MINUS";
      case KeyEvent.VK_PLUS: return "PLUS";
      case KeyEvent.VK_RIGHT_PARENTHESIS: return "RIGHT_PARENTHESIS";
      case KeyEvent.VK_UNDERSCORE: return "UNDERSCORE";
      
      case KeyEvent.VK_FINAL: return "FINAL";
      case KeyEvent.VK_CONVERT: return "CONVERT";
      case KeyEvent.VK_NONCONVERT: return "NONCONVERT";
      case KeyEvent.VK_ACCEPT: return "ACCEPT";
      case KeyEvent.VK_MODECHANGE: return "MODECHANGE";
      case KeyEvent.VK_KANA: return "KANA";
      case KeyEvent.VK_KANJI: return "KANJI";
      case KeyEvent.VK_ALPHANUMERIC: return "ALPHANUMERIC";
      case KeyEvent.VK_KATAKANA: return "KATAKANA";
      case KeyEvent.VK_HIRAGANA: return "HIRAGANA";
      case KeyEvent.VK_FULL_WIDTH: return "FULL_WIDTH";
      case KeyEvent.VK_HALF_WIDTH: return "HALF_WIDTH";
      case KeyEvent.VK_ROMAN_CHARACTERS: return "ROMAN_CHARACTERS";
      case KeyEvent.VK_ALL_CANDIDATES: return "ALL_CANDIDATES";
      case KeyEvent.VK_PREVIOUS_CANDIDATE: return "PREVIOUS_CANDIDATE";
      case KeyEvent.VK_CODE_INPUT: return "CODE_INPUT";
      case KeyEvent.VK_JAPANESE_KATAKANA: return "JAPANESE_KATAKANA";
      case KeyEvent.VK_JAPANESE_HIRAGANA: return "JAPANESE_HIRAGANA";
      case KeyEvent.VK_JAPANESE_ROMAN: return "JAPANESE_ROMAN";
      case KeyEvent.VK_KANA_LOCK: return "KANA_LOCK";
      case KeyEvent.VK_INPUT_METHOD_ON_OFF: return "INPUT_METHOD_ON_OFF";
      
      case KeyEvent.VK_AGAIN: return "AGAIN";
      case KeyEvent.VK_UNDO: return "UNDO";
      case KeyEvent.VK_COPY: return "COPY";
      case KeyEvent.VK_PASTE: return "PASTE";
      case KeyEvent.VK_CUT: return "CUT";
      case KeyEvent.VK_FIND: return "FIND";
      case KeyEvent.VK_PROPS: return "PROPS";
      case KeyEvent.VK_STOP: return "STOP";
      
      case KeyEvent.VK_COMPOSE: return "COMPOSE";
      case KeyEvent.VK_ALT_GRAPH: return "ALT_GRAPH";
    }
    
    if (keyCode >= KeyEvent.VK_NUMPAD0 && keyCode <= KeyEvent.VK_NUMPAD9) {
      char c = (char)(keyCode - KeyEvent.VK_NUMPAD0 + '0');
      return "NUMPAD"+c;
    }
    
    return "unknown(0x" + Integer.toString(keyCode, 16) + ")";
  }
}
